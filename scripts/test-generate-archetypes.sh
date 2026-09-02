#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd -P)"
GENERATOR="${SCRIPT_DIR}/generate_archetypes.sh"

fail() {
  printf 'archetype-generation-test: %s\n' "$*" >&2
  exit 1
}

assert_file_contains() {
  local file="$1" expected="$2" context="$3"
  grep -Fq -- "$expected" "$file" || fail "${context}: missing ${expected} in ${file}"
}

assert_equal() {
  local expected="$1" actual="$2" context="$3"
  [[ "$expected" == "$actual" ]] || fail "${context}: expected '${expected}', got '${actual}'"
}

assert_command_fails() {
  local expected_text="$1"
  shift
  local output_file="$TEST_ROOT/command-output.log"
  if "$@" >"$output_file" 2>&1; then
    fail "command unexpectedly succeeded: $*"
  fi
  if ! grep -Fq -- "$expected_text" "$output_file"; then
    cat "$output_file" >&2
    fail "expected failure: missing ${expected_text} in ${output_file}"
  fi
}

write_manifest() {
  local manifest="$1" source_path="$2" source_artifact="$3" source_package="$4" target_artifact="$5"
  mkdir -p "$(dirname "$manifest")"
  cat >"$manifest" <<EOF
sourceProject=${source_path}
sourceGroupId=example.source
sourceArtifactId=${source_artifact}
sourceVersion=0.1.0-SNAPSHOT
sourcePackage=${source_package}
targetArtifactId=${target_artifact}
expectedTopology=root
EOF
}

write_fake_maven_wrapper() {
  local wrapper="$1"
  cat >"$wrapper" <<'EOF'
#!/usr/bin/env bash
set -Eeuo pipefail

output_directory=''
source_pom=''
for argument in "$@"; do
  case "$argument" in
    -DoutputDirectory=*) output_directory="${argument#-DoutputDirectory=}" ;;
    -f) : ;;
    *.xml) [[ -f "$argument" ]] && source_pom="$argument" ;;
  esac
done
[[ -n "$output_directory" && -n "$source_pom" ]] || exit 2

counter_file="${FAKE_COUNTER_FILE:?FAKE_COUNTER_FILE is required}"
counter=0
if [[ -f "$counter_file" ]]; then
  counter="$(<"$counter_file")"
fi
counter=$((counter + 1))
printf '%s\n' "$counter" >"$counter_file"
if [[ -n "${FAKE_FAIL_AT:-}" && "$counter" == "$FAKE_FAIL_AT" ]]; then
  printf 'fake mvnw: injected failure at invocation %s\n' "$counter" >&2
  exit 42
fi
[[ -z "${FAKE_SLEEP:-}" ]] || sleep "$FAKE_SLEEP"

source_dir="$(cd "$(dirname "$source_pom")" && pwd -P)"
mkdir -p "$output_directory/src/main/resources/archetype-resources/src/main/java"
mkdir -p "$output_directory/src/main/resources/archetype-resources/src/main/resources"
mkdir -p "$source_dir/target"
printf 'maven-build-state-%s\n' "$counter" >"$source_dir/target/generated-by-fake-maven.txt"
printf '<groupId>${groupId}</groupId>\n<artifactId>${artifactId}</artifactId>\n' \
  >"$output_directory/src/main/resources/archetype-resources/pom.xml"
printf 'package ${package};\n' \
  >"$output_directory/src/main/resources/archetype-resources/src/main/java/Example.java"
printf 'value=${APP_VALUE:default}\n' \
  >"$output_directory/src/main/resources/archetype-resources/src/main/resources/application.yml"
printf '## Fixture heading\n' \
  >"$output_directory/src/main/resources/archetype-resources/README.md"
printf 'source=%s\n' "$source_dir" \
  >"$output_directory/src/main/resources/archetype-resources/source.txt"
EOF
  chmod +x "$wrapper"
}

setup_fixture() {
  TEST_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/egon-archetype-generation-test.XXXXXX")"
  trap 'rm -rf "$TEST_ROOT"' EXIT
  fixture_repo="$TEST_ROOT/repo"
  fixture_scripts="$fixture_repo/scripts"
  fixture_archetypes="$fixture_repo/egon-cola-archetypes"
  mkdir -p "$fixture_scripts" "$fixture_archetypes"
  cp "$GENERATOR" "$fixture_scripts/generate_archetypes.sh"
  chmod +x "$fixture_scripts/generate_archetypes.sh"
  write_fake_maven_wrapper "$fixture_repo/mvnw"

  cat >"$fixture_archetypes/pom.xml" <<'EOF'
<project>
  <modules>
    <module>package-a</module>
    <module>package-b</module>
    <module>package-c</module>
  </modules>
</project>
EOF

  for product in a b c; do
    package_dir="$fixture_archetypes/package-${product}"
    source_dir="$fixture_archetypes/source-projects/source-${product}"
    mkdir -p "$package_dir/src/main/archetype" "$package_dir/src/main/resources/META-INF/maven" "$source_dir/src/main/java"
    cat >"$package_dir/pom.xml" <<EOF
<project>
  <artifactId>package-${product}</artifactId>
  <packaging>maven-archetype</packaging>
</project>
EOF
    cat >"$source_dir/pom.xml" <<EOF
<project>
  <groupId>example.source</groupId>
  <artifactId>source-${product}</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</project>
EOF
    printf 'package example.source.%s;\n' "$product" >"$source_dir/src/main/java/Example.java"
    printf 'target/\n' >"$source_dir/.gitignore"
    cat >"$package_dir/src/main/resources/META-INF/maven/archetype-metadata.xml" <<EOF
<archetype-descriptor name="package-${product}">
  <fileSets><fileSet><directory></directory><includes><include>pom.xml</include></includes></fileSet></fileSets>
</archetype-descriptor>
EOF
    write_manifest "$package_dir/src/main/archetype/archetype.properties" \
      "../source-projects/source-${product}" "source-${product}" "example.source.${product}" "package-${product}"
  done

  export FAKE_COUNTER_FILE="$TEST_ROOT/fake-maven-counter"
  export FAKE_FAIL_AT=''
  export FAKE_SLEEP=''
}

run_fixture_generator() {
  "$fixture_scripts/generate_archetypes.sh" "$@"
}

remove_line() {
  local pattern="$1" file="$2"
  sed -i '' "/${pattern}/d" "$file" 2>/dev/null || sed -i "/${pattern}/d" "$file"
}

replace_line() {
  local pattern="$1" replacement="$2" file="$3"
  sed -i '' "s#${pattern}#${replacement}#" "$file" 2>/dev/null \
    || sed -i "s#${pattern}#${replacement}#" "$file"
}

test_usage_and_manifest_validation() {
  assert_command_fails 'Usage:' run_fixture_generator unknown

  printf 'unexpected=value\n' >>"$fixture_archetypes/package-a/src/main/archetype/archetype.properties"
  assert_command_fails 'unknown manifest field' run_fixture_generator generate
  remove_line '^unexpected=value$' "$fixture_archetypes/package-a/src/main/archetype/archetype.properties"

  replace_line 'sourceProject=.*' 'sourceProject=/tmp/outside' "$fixture_archetypes/package-a/src/main/archetype/archetype.properties"
  assert_command_fails 'absolute sourceProject' run_fixture_generator generate
  replace_line 'sourceProject=/tmp/outside' 'sourceProject=../source-projects/source-a' "$fixture_archetypes/package-a/src/main/archetype/archetype.properties"

  printf 'targetArtifactId=package-b\n' >>"$fixture_archetypes/package-a/src/main/archetype/archetype.properties"
  assert_command_fails 'duplicate manifest field' run_fixture_generator generate
  remove_line '^targetArtifactId=package-b$' "$fixture_archetypes/package-a/src/main/archetype/archetype.properties"
}

test_atomic_failure_and_cleanup() {
  run_fixture_generator generate
  old_hash="$(shasum -a 256 "$fixture_archetypes/.generated/package-a/generation-manifest.sha256" | awk '{print $1}')"
  rm -f "$FAKE_COUNTER_FILE"
  FAKE_FAIL_AT=2 run_fixture_generator generate && fail 'injected Maven failure unexpectedly succeeded'
  new_hash="$(shasum -a 256 "$fixture_archetypes/.generated/package-a/generation-manifest.sha256" | awk '{print $1}')"
  assert_equal "$old_hash" "$new_hash" 'atomic failure must preserve old complete set'
  [[ ! -d "$fixture_archetypes/.generated.lock" ]] || fail 'lock survived injected failure'
  [[ -z "$(find "$fixture_archetypes" -maxdepth 1 -name '.generated.staging.*' -print -quit)" ]] || fail 'staging survived failure'
}

test_determinism_and_check() {
  rm -f "$FAKE_COUNTER_FILE"
  run_fixture_generator generate
  assert_file_contains "$fixture_archetypes/.generated/package-b/archetype-resources/src/main/resources/application.yml" \
    '${symbol_dollar}{APP_VALUE:default}' 'Velocity dollar escaping'
  assert_file_contains "$fixture_archetypes/.generated/package-b/archetype-resources/README.md" \
    '${symbol_pound}${symbol_pound} Fixture heading' 'Velocity pound escaping'
  first_hash="$(shasum -a 256 "$fixture_archetypes/.generated/package-b/archetype-resources/source.txt" | awk '{print $1}')"
  current_manifest="$(shasum -a 256 "$fixture_archetypes/.generated/package-b/generation-manifest.sha256" | awk '{print $1}')"
  run_fixture_generator check
  second_hash="$(shasum -a 256 "$fixture_archetypes/.generated/package-b/archetype-resources/source.txt" | awk '{print $1}')"
  after_check_manifest="$(shasum -a 256 "$fixture_archetypes/.generated/package-b/generation-manifest.sha256" | awk '{print $1}')"
  assert_equal "$first_hash" "$second_hash" 'repeated generation resource hash'
  assert_equal "$current_manifest" "$after_check_manifest" 'check must not mutate generated set'
}

test_lock_and_signal_cleanup() {
  mkdir "$fixture_archetypes/.generated.lock"
  assert_command_fails 'generation lock is held' run_fixture_generator generate
  rmdir "$fixture_archetypes/.generated.lock"

  rm -f "$FAKE_COUNTER_FILE"
  FAKE_SLEEP=2 "$fixture_scripts/generate_archetypes.sh" generate &
  first_pid=$!
  for _ in 1 2 3 4 5 6 7 8 9 10; do
    [[ -d "$fixture_archetypes/.generated.lock" ]] && break
    sleep 0.1
  done
  [[ -d "$fixture_archetypes/.generated.lock" ]] || fail 'generator did not acquire lock'
  assert_command_fails 'generation lock is held' run_fixture_generator generate
  kill -TERM "$first_pid"
  wait "$first_pid" 2>/dev/null || true
  [[ ! -d "$fixture_archetypes/.generated.lock" ]] || fail 'lock survived signal cleanup'
  [[ -z "$(find "$fixture_archetypes" -maxdepth 1 -name '.generated.staging.*' -print -quit)" ]] || fail 'staging survived signal'
}

test_generation_mode() {
  [[ -x "$GENERATOR" ]] || fail "production generator is required for generation mode"
  "$GENERATOR" generate >/dev/null 2>&1 || fail 'real generation mode is not ready yet'
  "$GENERATOR" check >/dev/null 2>&1 || fail 'real generation check is not ready yet'
}

test_package_mode() {
  local family="$1" module
  [[ "$family" =~ ^(light|service|web)$ ]] || fail "unsupported package family: ${family}"
  while IFS= read -r module; do
    [[ -n "$module" ]] || continue
    assert_file_contains "$module/pom.xml" '.generated/${project.artifactId}' "${module} generated resource wiring"
    [[ -f "$module/src/main/javadoc/README.md" ]] || fail "missing javadoc README in ${module}"
    assert_file_contains "$module/src/test/resources/projects/basic/verify.groovy" 'top.egon.internal.archetype.source' "${module} sentinel verifier"
  done < <(find "$REPO_ROOT/egon-cola-archetypes" -maxdepth 1 -type d -name "egon-cola-archetype-${family}*" -print | sort)
}

test_release_wiring_mode() {
  local workflow
  for workflow in "$REPO_ROOT/.github/workflows/ci.yaml" "$REPO_ROOT/.github/workflows/ci_java_compatibility.yaml" "$REPO_ROOT/.github/workflows/publish-maven-central.yml"; do
    [[ -f "$workflow" ]] || fail "missing workflow: ${workflow}"
    assert_file_contains "$workflow" 'generate_archetypes.sh' "${workflow} generation gate"
    assert_file_contains "$workflow" 'source-projects' "${workflow} source gate"
  done
}

main() {
  local mode="${1:-unit}"
  case "$mode" in
    unit)
      [[ -x "$GENERATOR" ]] || fail "generator is missing: ${GENERATOR}"
      setup_fixture
      test_usage_and_manifest_validation
      test_atomic_failure_and_cleanup
      test_determinism_and_check
      test_lock_and_signal_cleanup
      printf '%s\n' 'archetype-generation-test: unit safety tests passed'
      ;;
    generation)
      test_generation_mode
      ;;
    package)
      [[ $# -eq 2 ]] || fail 'package mode requires light, service or web'
      test_package_mode "$2"
      ;;
    release)
      test_release_wiring_mode
      ;;
    *)
      fail "unsupported test mode: ${mode}"
      ;;
  esac
}

main "$@"
