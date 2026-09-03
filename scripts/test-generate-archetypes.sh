#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd -P)"
GENERATOR="${SCRIPT_DIR}/generate_archetypes.sh"
CHECK_WRAPPER="${SCRIPT_DIR}/check_archetypes.sh"

fail() {
  printf 'archetype-generation-test: %s\n' "$*" >&2
  exit 1
}

assert_file_contains() {
  local file="$1" expected="$2" context="$3"
  grep -Fq -- "$expected" "$file" || fail "${context}: missing ${expected} in ${file}"
}

assert_file_not_contains() {
  local file="$1" unexpected="$2" context="$3"
  if grep -Fq -- "$unexpected" "$file"; then
    fail "${context}: unexpected ${unexpected} in ${file}"
  fi
}

assert_equal() {
  local expected="$1" actual="$2" context="$3"
  [[ "$expected" == "$actual" ]] || fail "${context}: expected '${expected}', got '${actual}'"
}

assert_command_fails() {
  local expected_text="$1"
  shift
  local output_file="${TEST_ROOT}/command-output.$$.log"
  if "$@" >"$output_file" 2>&1; then
    sed -n '1,160p' "$output_file" >&2
    fail "command unexpectedly succeeded: $*"
  fi
  if ! grep -Fq -- "$expected_text" "$output_file"; then
    sed -n '1,160p' "$output_file" >&2
    fail "expected failure: missing ${expected_text} in ${output_file}"
  fi
}

hash_tree() {
  local tree="$1" file relative mode
  [[ -d "$tree" ]] || fail "hash input does not exist: ${tree}"
  while IFS= read -r file; do
    relative="${file#"$tree"/}"
    mode="$(stat -f '%Lp' "$file" 2>/dev/null || stat -c '%a' "$file")"
    printf '%s  %s  %s\n' "$(shasum -a 256 "$file" | awk '{print $1}')" "$mode" "$relative"
  done < <(find "$tree" -type f -print | LC_ALL=C sort)
}

write_manifest() {
  local manifest="$1" source_path="$2" source_artifact="$3" source_package="$4"
  local target_artifact="$5" topology="$6"
  mkdir -p "$(dirname "$manifest")"
  cat >"$manifest" <<EOF
sourceProject=${source_path}
sourceGroupId=example.source
sourceArtifactId=${source_artifact}
sourceVersion=0.1.0-SNAPSHOT
sourcePackage=${source_package}
targetArtifactId=${target_artifact}
expectedTopology=${topology}
EOF
}

write_packaging_pom() {
  local file="$1" artifact="$2"
  cat >"$file" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>example</groupId>
        <artifactId>fixture-archetypes-parent</artifactId>
        <version>@rootVersion@</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>
    <artifactId>${artifact}</artifactId>
    <packaging>maven-archetype</packaging>
    <name>${artifact}</name>
    <description>${artifact} fixture</description>
    <build>
        <resources>
            <resource>
                <directory>\${project.basedir}/archetype-resources</directory>
                <filtering>false</filtering>
            </resource>
            <resource>
                <directory>\${project.basedir}/src/main/resources</directory>
                <filtering>false</filtering>
                <includes><include>META-INF/**</include></includes>
            </resource>
        </resources>
    </build>
</project>
EOF
}

write_definition_assets() {
  local definition="$1" product="$2"
  mkdir -p \
    "$definition/src/main/resources/META-INF/maven" \
    "$definition/src/test/resources/projects/basic" \
    "$definition/src/main/javadoc" \
    "$definition/architecture-docs"
  cat >"$definition/src/main/resources/META-INF/maven/archetype-metadata.xml" <<EOF
<archetype-descriptor name="fixture-${product}">
  <fileSets><fileSet><directory></directory><includes><include>pom.xml</include></includes></fileSet></fileSets>
</archetype-descriptor>
EOF
  cat >"$definition/src/main/resources/META-INF/archetype-post-generate.groovy" <<'EOF'
println 'fixture post-generate'
EOF
  printf '# Fixture %s\n' "$product" >"$definition/src/main/javadoc/README.md"
  printf 'gitignore=.gitignore\n' >"$definition/src/test/resources/projects/basic/archetype.properties"
  printf 'fixture goal\n' >"$definition/src/test/resources/projects/basic/goal.txt"
  printf 'assert true\n' >"$definition/src/test/resources/projects/basic/verify.groovy"
  printf '# Fixture architecture %s\n' "$product" >"$definition/architecture-docs/architecture.md"
}

write_fake_maven_wrapper() {
  local wrapper="$1"
  cat >"$wrapper" <<'EOF'
#!/usr/bin/env bash

set -Eeuo pipefail

for argument in "$@"; do
  if [[ "$argument" == *help:evaluate* ]]; then
    printf '%s\n' '9.9.9'
    exit 0
  fi
done

output_directory=''
source_pom=''
property_file=''
for ((index = 1; index <= $#; index++)); do
  argument="${!index}"
  case "$argument" in
    -DoutputDirectory=*) output_directory="${argument#-DoutputDirectory=}" ;;
    -DpropertyFile=*) property_file="${argument#-DpropertyFile=}" ;;
    -f)
      index=$((index + 1))
      source_pom="${!index}"
      ;;
  esac
done
[[ -n "$output_directory" && -f "$source_pom" && -f "$property_file" ]] || exit 2

source_artifact="$(sed -n 's:.*<artifactId>\([^<]*\)</artifactId>.*:\1:p' "$source_pom" | head -n 1)"
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

resources="$output_directory/src/main/resources/archetype-resources"
mkdir -p "$resources/src/main/java" "$resources/src/main/resources"
printf '<groupId>${groupId}</groupId>\n<artifactId>${artifactId}</artifactId>\n<version>${version}</version>\n' \
  >"$resources/pom.xml"
printf 'package ${package};\n' >"$resources/src/main/java/Example.java"
printf 'value=${APP_VALUE:default}\n' >"$resources/src/main/resources/application.yml"
printf '## Fixture heading\n' >"$resources/README.md"
printf 'source=%s\n' "$source_artifact" >"$resources/source.txt"

module_suffixes="$(sed -n 's:.*<module>\([^<]*\)</module>.*:\1:p' "$source_pom")"
while IFS= read -r suffix; do
  [[ -n "$suffix" ]] || continue
  module_resources="$resources/${source_artifact}-${suffix}"
  mkdir -p "$module_resources/src/main/java" "$module_resources/src/main/resources"
  printf '<artifactId>${rootArtifactId}-%s</artifactId>\n' "$suffix" >"$module_resources/pom.xml"
  printf 'package ${package};\n' >"$module_resources/src/main/java/Example.java"
done <<<"$module_suffixes"
EOF
  chmod +x "$wrapper"
}

setup_definition_fixture() {
  TEST_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/egon-archetype-generation-test.XXXXXX")"
  trap 'rm -rf "$TEST_ROOT"' EXIT
  fixture_repo="$TEST_ROOT/repo"
  fixture_scripts="$fixture_repo/scripts"
  fixture_archetypes="$fixture_repo/egon-cola-archetypes"
  mkdir -p "$fixture_scripts" "$fixture_archetypes/definitions" "$fixture_archetypes/source-projects"
  cp "$GENERATOR" "$fixture_scripts/generate_archetypes.sh"
  chmod +x "$fixture_scripts/generate_archetypes.sh"
  if [[ -f "$CHECK_WRAPPER" ]]; then
    cp "$CHECK_WRAPPER" "$fixture_scripts/check_archetypes.sh"
    chmod +x "$fixture_scripts/check_archetypes.sh"
  fi
  write_fake_maven_wrapper "$fixture_repo/mvnw"

  cat >"$fixture_archetypes/pom.xml" <<'EOF'
<project>
  <groupId>example</groupId>
  <artifactId>fixture-archetypes-parent</artifactId>
  <version>9.9.9</version>
</project>
EOF

  for product in a b c; do
    definition="$fixture_archetypes/definitions/egon-cola-archetype-${product}"
    source_dir="$fixture_archetypes/source-projects/source-${product}"
    topology=root
    if [[ "$product" == b ]]; then
      topology=common,domain
    fi
    mkdir -p "$source_dir/src/main/java" "$source_dir/src/main/resources/db/migration"
    cat >"$source_dir/pom.xml" <<EOF
<project>
  <groupId>example.source</groupId>
  <artifactId>source-${product}</artifactId>
  <version>0.1.0-SNAPSHOT</version>
EOF
    if [[ "$product" == b ]]; then
      cat >>"$source_dir/pom.xml" <<'EOF'
  <modules>
    <module>common</module>
    <module>domain</module>
  </modules>
EOF
    fi
    printf '%s\n' '</project>' >>"$source_dir/pom.xml"
    printf 'package example.source.%s;\n' "$product" >"$source_dir/src/main/java/Example.java"
    printf 'target/\n' >"$source_dir/.gitignore"
    printf '%s\n' '-- legacy V migration fixture' >"$source_dir/src/main/resources/db/migration/V20260825_001__legacy.sql"
    if [[ "$product" == b ]]; then
      for suffix in common domain; do
        mkdir -p "$source_dir/source-${product}-${suffix}/src/main/java"
        cat >"$source_dir/source-${product}-${suffix}/pom.xml" <<EOF
<project><artifactId>source-${product}-${suffix}</artifactId></project>
EOF
        printf 'package example.source.%s.%s;\n' "$product" "$suffix" \
          >"$source_dir/source-${product}-${suffix}/src/main/java/Example.java"
      done
    fi
    mkdir -p "$definition"
    write_manifest "$definition/archetype.properties" \
      "source-projects/source-${product}" "source-${product}" \
      "example.source.${product}" "package-${product}" "$topology"
    write_packaging_pom "$definition/packaging-pom.xml" "package-${product}"
    write_definition_assets "$definition" "$product"
  done

  legacy_sql="$fixture_archetypes/source-projects/source-a/src/main/resources/db/migration/V20260825_001__legacy.sql"
  legacy_hash_before="$(shasum -a 256 "$legacy_sql" | awk '{print $1}')"
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

test_definition_validation() {
  local manifest="$fixture_archetypes/definitions/egon-cola-archetype-a/archetype.properties"
  local packaging_pom="$fixture_archetypes/definitions/egon-cola-archetype-a/packaging-pom.xml"
  printf 'unexpected=value\n' >>"$manifest"
  assert_command_fails 'unknown manifest field' run_fixture_generator generate
  remove_line '^unexpected=value$' "$manifest"

  replace_line 'sourceProject=.*' 'sourceProject=/tmp/outside' "$manifest"
  assert_command_fails 'absolute sourceProject' run_fixture_generator generate
  replace_line 'sourceProject=/tmp/outside' 'sourceProject=source-projects/source-a' "$manifest"

  replace_line 'targetArtifactId=package-a' 'targetArtifactId=package-b' "$manifest"
  replace_line '<artifactId>package-a</artifactId>' '<artifactId>package-b</artifactId>' "$packaging_pom"
  assert_command_fails 'duplicate targetArtifactId' run_fixture_generator generate
  replace_line '<artifactId>package-b</artifactId>' '<artifactId>package-a</artifactId>' "$packaging_pom"
  replace_line 'targetArtifactId=package-b' 'targetArtifactId=package-a' "$manifest"

  mv "$fixture_archetypes/definitions/egon-cola-archetype-a/src/main/javadoc/README.md" \
    "$TEST_ROOT/fixture-readme"
  assert_command_fails 'curated file is missing' run_fixture_generator generate
  mv "$TEST_ROOT/fixture-readme" \
    "$fixture_archetypes/definitions/egon-cola-archetype-a/src/main/javadoc/README.md"

  mkdir "$fixture_archetypes/definitions/egon-cola-archetype-extra"
  assert_command_fails 'archetype.properties is missing' run_fixture_generator generate
  rmdir "$fixture_archetypes/definitions/egon-cola-archetype-extra"
}

test_complete_child_layout() {
  run_fixture_generator generate
  [[ -f "$fixture_archetypes/.generated/pom.xml" ]] || fail 'generated aggregator is missing'
  for product in a b c; do
    child="$fixture_archetypes/.generated/package-${product}"
    [[ -f "$child/pom.xml" ]] || fail "generated child POM is missing: ${child}"
    assert_file_contains "$child/pom.xml" '<packaging>maven-archetype</packaging>' \
      "${child} packaging"
    assert_file_contains "$child/pom.xml" '<version>9.9.9</version>' "${child} root version"
    [[ -f "$child/archetype-resources/pom.xml" ]] || fail "${child} resources are missing"
    [[ -f "$child/src/main/resources/META-INF/maven/archetype-metadata.xml" ]] \
      || fail "${child} metadata is missing"
    [[ -f "$child/src/main/resources/META-INF/archetype-post-generate.groovy" ]] \
      || fail "${child} post script is missing"
    [[ -f "$child/src/main/javadoc/README.md" ]] || fail "${child} javadoc is missing"
    [[ -f "$child/src/test/resources/projects/basic/verify.groovy" ]] \
      || fail "${child} basic IT verifier is missing"
    [[ -f "$child/architecture-docs/architecture.md" ]] \
      || fail "${child} architecture documentation is missing"
    assert_file_not_contains "$child/pom.xml" '@rootVersion@' "${child} unresolved root token"
  done
  [[ -f "$fixture_archetypes/.generated/package-b/archetype-resources/__rootArtifactId__-common/pom.xml" ]] \
    || fail 'multi-module child common module is missing'
}

test_generated_aggregator() {
  local aggregator="$fixture_archetypes/.generated/pom.xml"
  assert_file_contains "$aggregator" '<artifactId>egon-cola-generated-archetypes-reactor</artifactId>' \
    'generated aggregator identity'
  assert_file_contains "$aggregator" '<skip>true</skip>' 'generated aggregator deploy skip'
  expected_modules=$'package-a\npackage-b\npackage-c'
  actual_modules="$(sed -n 's/^[[:space:]]*<module>\([^<]*\)<\/module>[[:space:]]*$/\1/p' "$aggregator")"
  assert_equal "$expected_modules" "$actual_modules" 'generated aggregator module order'
}

test_check_wrapper() {
  [[ -x "$fixture_scripts/check_archetypes.sh" ]] || fail 'check wrapper is missing'
  before_hash="$(hash_tree "$fixture_archetypes/.generated")"
  "$fixture_scripts/check_archetypes.sh" >/dev/null
  after_hash="$(hash_tree "$fixture_archetypes/.generated")"
  assert_equal "$before_hash" "$after_hash" 'check wrapper must not mutate generated set'
  assert_command_fails 'Usage:' "$fixture_scripts/check_archetypes.sh" unexpected

  printf 'mutation\n' >>"$fixture_archetypes/.generated/package-b/archetype-resources/source.txt"
  assert_command_fails 'generated resources are not deterministic' \
    "$fixture_scripts/check_archetypes.sh"
  run_fixture_generator generate >/dev/null
}

test_determinism() {
  first_hash="$(hash_tree "$fixture_archetypes/.generated")"
  rm -f "$FAKE_COUNTER_FILE"
  run_fixture_generator generate >/dev/null
  second_hash="$(hash_tree "$fixture_archetypes/.generated")"
  assert_equal "$first_hash" "$second_hash" 'repeated full generation must be deterministic'
}

test_atomic_full_reactor_failure() {
  old_hash="$(hash_tree "$fixture_archetypes/.generated")"
  rm -f "$FAKE_COUNTER_FILE"
  if FAKE_FAIL_AT=2 run_fixture_generator generate >"$TEST_ROOT/failure.log" 2>&1; then
    fail 'injected Maven failure unexpectedly succeeded'
  fi
  new_hash="$(hash_tree "$fixture_archetypes/.generated")"
  assert_equal "$old_hash" "$new_hash" 'atomic failure must preserve old complete set'
  [[ ! -d "$fixture_archetypes/.generated.lock" ]] || fail 'lock survived injected failure'
  [[ -z "$(find "$fixture_archetypes" -maxdepth 1 -name '.generated.staging.*' -print -quit)" ]] \
    || fail 'staging survived failure'
  [[ -z "$(find "$fixture_archetypes" -maxdepth 1 -name '.generated.previous.*' -print -quit)" ]] \
    || fail 'backup survived failure'
}

test_legacy_sql_hash_boundary() {
  legacy_hash_after="$(shasum -a 256 "$legacy_sql" | awk '{print $1}')"
  assert_equal "$legacy_hash_before" "$legacy_hash_after" 'legacy V migration must remain byte-identical'
}

test_lock_and_signal_cleanup() {
  mkdir "$fixture_archetypes/.generated.lock"
  assert_command_fails 'generation lock is held' run_fixture_generator generate
  rmdir "$fixture_archetypes/.generated.lock"

  rm -f "$FAKE_COUNTER_FILE"
  FAKE_SLEEP=2 "$fixture_scripts/generate_archetypes.sh" generate >/dev/null 2>&1 &
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
  [[ -z "$(find "$fixture_archetypes" -maxdepth 1 -name '.generated.staging.*' -print -quit)" ]] \
    || fail 'staging survived signal'
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
    assert_file_contains "$module/pom.xml" '.generated/${project.artifactId}' \
      "${module} generated resource wiring"
    [[ -f "$module/src/main/javadoc/README.md" ]] || fail "missing javadoc README in ${module}"
    assert_file_contains "$module/src/test/resources/projects/basic/verify.groovy" \
      'top.egon.internal.archetype.source' "${module} sentinel verifier"
  done < <(find "$REPO_ROOT/egon-cola-archetypes" -maxdepth 1 -type d \
    -name "egon-cola-archetype-${family}*" -print | sort)
}

test_release_wiring_mode() {
  local workflow
  for workflow in "$REPO_ROOT/.github/workflows/ci.yaml" \
      "$REPO_ROOT/.github/workflows/ci_java_compatibility.yaml" \
      "$REPO_ROOT/.github/workflows/publish-maven-central.yml"; do
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
      setup_definition_fixture
      test_definition_validation
      test_complete_child_layout
      test_generated_aggregator
      test_check_wrapper
      test_determinism
      test_atomic_full_reactor_failure
      test_legacy_sql_hash_boundary
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
