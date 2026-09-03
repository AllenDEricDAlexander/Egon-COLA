#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd -P)"
DEPLOY_SCRIPT="${REPO_ROOT}/scripts/maven-deploy.sh"
TEST_ROOT=''
FIXTURE_REPO=''
FIXTURE_DEPLOY=''
FAKE_LOG=''

fail() {
  printf 'archetype-release-test: %s\n' "$*" >&2
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
  grep -Fq -- "$expected_text" "$output_file" || {
    sed -n '1,160p' "$output_file" >&2
    fail "expected failure: missing ${expected_text}"
  }
}

write_fake_maven_wrapper() {
  local wrapper="$1"
  cat >"$wrapper" <<'EOF'
#!/usr/bin/env bash

set -Eeuo pipefail

log="${FAKE_LOG:?FAKE_LOG is required}"
joined=" $* "
label=other
if [[ "$joined" == *"help:evaluate"* ]]; then
  label=version
elif [[ "$joined" == *"-f egon-cola-archetypes/source-projects/pom.xml"* ]]; then
  label=source-install
elif [[ "$joined" == *"-f egon-cola-archetypes/pom.xml"* && "$joined" == *"-Pgenerated-archetypes"* ]]; then
  label=generated-it
elif [[ "$joined" == *"-f egon-cola-archetypes/pom.xml"* && "$joined" == *" install"* ]]; then
  label=archetypes-bootstrap
elif [[ "$joined" == *"clean deploy"* ]]; then
  label=deploy
elif [[ "$joined" == *"clean verify"* ]]; then
  label=release-shape
elif [[ "$joined" == *"-N install"* ]]; then
  label=root-bootstrap
fi
printf 'mvnw:%s:%s\n' "$label" "$*" >>"$log"
if [[ "${FAKE_FAIL_LABEL:-}" == "$label" ]]; then
  printf 'fake mvnw: injected failure at %s\n' "$label" >&2
  exit 42
fi
if [[ "$label" == version ]]; then
  printf '9.9.9\n'
fi
EOF
  chmod +x "$wrapper"
}

write_fake_stage() {
  local script="$1" label="$2"
  cat >"$script" <<EOF
#!/usr/bin/env bash
set -Eeuo pipefail
printf 'stage:${label}\\n' "" >>"\${FAKE_LOG:?FAKE_LOG is required}"
if [[ "\${FAKE_FAIL_LABEL:-}" == "${label}" ]]; then
  printf 'fake stage: injected failure at ${label}\\n' >&2
  exit 43
fi
EOF
  chmod +x "$script"
}

write_fixture_definitions() {
  local family target definition module
  for family in light light-open service service-open web web-open; do
    target="egon-cola-archetype-${family}"
    definition="${FIXTURE_REPO}/egon-cola-archetypes/definitions/${target}"
    mkdir -p "${definition}" \
      "${FIXTURE_REPO}/egon-cola-archetypes/.generated/${target}/target"
    cat >"${definition}/archetype.properties" <<EOF
sourceProject=source-projects/source-${family}
sourceGroupId=top.egon.source
sourceArtifactId=source-${family}
sourceVersion=0.1.0-SNAPSHOT
sourcePackage=top.egon.source.${family//-/.}
targetArtifactId=${target}
expectedTopology=root
EOF
    module="${FIXTURE_REPO}/egon-cola-archetypes/.generated/${target}"
    printf '<artifactId>%s</artifactId>\n' "$target" >"${module}/pom.xml"
    touch "${module}/target/${target}-9.9.9.jar" \
      "${module}/target/${target}-9.9.9-sources.jar" \
      "${module}/target/${target}-9.9.9-javadoc.jar"
  done
}

setup_fixture() {
  TEST_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/egon-archetype-release-test.XXXXXX")"
  trap 'rm -rf "$TEST_ROOT"' EXIT
  FIXTURE_REPO="${TEST_ROOT}/repo"
  mkdir -p "${FIXTURE_REPO}/scripts" \
    "${FIXTURE_REPO}/egon-cola-archetypes/source-projects"
  cp "$DEPLOY_SCRIPT" "${FIXTURE_REPO}/scripts/maven-deploy.sh"
  chmod +x "${FIXTURE_REPO}/scripts/maven-deploy.sh"
  write_fake_maven_wrapper "${FIXTURE_REPO}/mvnw"
  write_fake_stage "${FIXTURE_REPO}/scripts/generate_archetypes.sh" generate
  write_fake_stage "${FIXTURE_REPO}/scripts/check_archetypes.sh" check
  write_fixture_definitions
  git -C "$FIXTURE_REPO" init -q
  FAKE_LOG="${TEST_ROOT}/fake-call.log"
  : >"$FAKE_LOG"
  export FAKE_LOG
}

fixture_deploy() {
  "${FIXTURE_REPO}/scripts/maven-deploy.sh" "$@"
}

event_labels() {
  sed -n \
    -e 's/^mvnw:\([^:]*\):.*/\1/p' \
    -e 's/^stage:\(.*\)$/\1/p' \
    "$FAKE_LOG"
}

assert_order() {
  local expected actual item index=0 expected_count=0
  local -a expected_items=()
  expected="$1"
  actual="$2"
  while IFS= read -r item; do
    [[ -n "$item" ]] || continue
    expected_items[$expected_count]="$item"
    expected_count=$((expected_count + 1))
  done <<<"$expected"
  while IFS= read -r item; do
    [[ -n "$item" ]] || continue
    if ((index < expected_count)) && [[ "$item" == "${expected_items[$index]}" ]]; then
      index=$((index + 1))
      ((index == expected_count)) && break
    fi
  done <<<"$actual"
  ((index == expected_count)) || {
    printf 'Observed release events:\n%s\n' "$actual" >&2
    fail 'mandatory release stages are out of order or missing'
  }
}

test_supported_targets() {
  local expected actual
  expected=$'all\narchetypes'
  actual="$(fixture_deploy list)"
  assert_equal "$expected" "$actual" 'supported target list'
  assert_command_fails 'Unsupported Maven publish target' \
    fixture_deploy egon-cola-archetype-web-open --dry-run
  assert_command_fails 'only supports the all target' \
    fixture_deploy archetypes --publish
}

test_mandatory_preflight_order() {
  local expected actual
  : >"$FAKE_LOG"
  fixture_deploy all --dry-run >/dev/null
  expected=$'root-bootstrap\narchetypes-bootstrap\nsource-install\ngenerate\ncheck\ngenerated-it\nrelease-shape'
  actual="$(event_labels)"
  assert_order "$expected" "$actual"
  if grep -q '^mvnw:deploy:' "$FAKE_LOG"; then
    fail 'dry-run reached Maven deploy'
  fi
}

test_single_publish_deploy() {
  local actual deploy_count
  : >"$FAKE_LOG"
  fixture_deploy all --publish >/dev/null
  actual="$(event_labels)"
  assert_order $'root-bootstrap\narchetypes-bootstrap\nsource-install\ngenerate\ncheck\ngenerated-it\nrelease-shape\nversion\ndeploy' "$actual"
  deploy_count="$(grep -c '^mvnw:deploy:' "$FAKE_LOG" || true)"
  assert_equal '1' "$deploy_count" 'single root deploy'
  grep -Fq -- '-Pgenerated-archetypes' "$FAKE_LOG" || fail 'deploy omitted generated profile'
  grep -Fq -- '-Prelease' "$FAKE_LOG" || fail 'deploy omitted release profile'
}

test_failure_never_reaches_deploy() {
  local failure deploy_count
  for failure in root-bootstrap archetypes-bootstrap source-install generate check generated-it release-shape; do
    : >"$FAKE_LOG"
    if FAKE_FAIL_LABEL="$failure" fixture_deploy all --publish >/dev/null 2>&1; then
      fail "failure injection unexpectedly succeeded: ${failure}"
    fi
    deploy_count="$(grep -c '^mvnw:deploy:' "$FAKE_LOG" || true)"
    assert_equal '0' "$deploy_count" "deploy after ${failure} failure"
  done
}

test_definition_inventory() {
  local expected=6 actual manifest target targets
  actual="$(find "$REPO_ROOT/egon-cola-archetypes/definitions" -mindepth 2 -maxdepth 2 \
    -type f -name archetype.properties -print | wc -l | tr -d ' ')"
  assert_equal "$expected" "$actual" 'definition manifest count'
  targets="$(find "$REPO_ROOT/egon-cola-archetypes/definitions" -mindepth 2 -maxdepth 2 \
    -type f -name archetype.properties -print | sort | \
    while IFS= read -r manifest; do sed -n 's/^targetArtifactId=//p' "$manifest"; done)"
  actual="$(printf '%s\n' "$targets" | sort -u | sed '/^$/d' | wc -l | tr -d ' ')"
  assert_equal "$expected" "$actual" 'unique definition target count'
  while IFS= read -r manifest; do
    target="$(sed -n 's/^targetArtifactId=//p' "$manifest")"
    [[ "$target" == egon-cola-archetype-* ]] || fail "invalid definition target: ${manifest}"
    assert_file_contains "$manifest" 'sourceProject=source-projects/' 'definition source ownership'
  done < <(find "$REPO_ROOT/egon-cola-archetypes/definitions" -mindepth 2 -maxdepth 2 \
    -type f -name archetype.properties -print | sort)
}

test_generated_profile_and_paths() {
  local workflow
  assert_file_contains "$DEPLOY_SCRIPT" '-Pgenerated-archetypes' 'deploy generated profile'
  assert_file_contains "$DEPLOY_SCRIPT" 'egon-cola-archetypes/.generated' 'deploy generated artifact root'
  assert_file_not_contains "$DEPLOY_SCRIPT" 'egon-cola-archetype-light-open' 'individual Open target'
  for workflow in "$REPO_ROOT/.github/workflows/ci.yaml" \
      "$REPO_ROOT/.github/workflows/ci_java_compatibility.yaml" \
      "$REPO_ROOT/.github/workflows/publish-maven-central.yml"; do
    assert_file_contains "$workflow" 'egon-cola-archetypes/definitions' \
      "${workflow} definitions inventory"
    assert_file_contains "$workflow" '.generated/' "${workflow} generated artifact root"
    assert_file_contains "$workflow" '-Pgenerated-archetypes' "${workflow} generated profile"
    assert_file_contains "$workflow" 'scripts/check_archetypes.sh' \
      "${workflow} generated drift check"
    assert_file_not_contains "$workflow" 'src/main/archetype/archetype.properties' \
      "${workflow} legacy manifest path"
    assert_file_not_contains "$workflow" 'egon-cola-archetypes/${ARCHETYPE_ARTIFACT_ID}/pom.xml' \
      "${workflow} legacy package path"
  done
}

main() {
  setup_fixture
  test_supported_targets
  test_mandatory_preflight_order
  test_single_publish_deploy
  test_failure_never_reaches_deploy
  test_definition_inventory
  test_generated_profile_and_paths
  printf 'archetype-release-test: all release contract tests passed\n'
}

main "$@"
