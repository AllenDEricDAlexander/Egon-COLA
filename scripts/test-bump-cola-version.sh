#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd -P)"
FIXTURE_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/egon-cola-version-test.XXXXXX")"
readonly SCRIPT_DIR PROJECT_ROOT FIXTURE_ROOT

cleanup() {
  rm -rf -- "$FIXTURE_ROOT"
}

trap cleanup EXIT HUP INT TERM

die() {
  printf 'bump-version-test: %s\n' "$*" >&2
  exit 1
}

hash_tree() {
  local tree="$1" file
  find "$tree" -type f -print | LC_ALL=C sort | while IFS= read -r file; do
    shasum -a 256 "$file"
  done | shasum -a 256 | awk '{print $1}'
}

assert_file_contains() {
  local file="$1" pattern="$2"
  grep -Fq -- "$pattern" "$file" || die "$file does not contain $pattern"
}

write_fixture() {
  local root="$FIXTURE_ROOT/repo"
  mkdir -p \
    "$root/scripts" \
    "$root/egon-cola-archetypes/source-projects/egon-cola-source-fixture" \
    "$root/egon-cola-archetypes/.generated/egon-cola-archetype-fixture"

  printf '%s\n' \
    '<project>' \
    '  <modelVersion>4.0.0</modelVersion>' \
    '  <groupId>fixture</groupId>' \
    '  <artifactId>fixture</artifactId>' \
    '  <version>5.3.3</version>' \
    '</project>' >"$root/pom.xml"

  printf '%s\n' \
    '<project>' \
    '  <modelVersion>4.0.0</modelVersion>' \
    '  <groupId>top.egon.internal.archetype.source</groupId>' \
    '  <artifactId>egon-cola-source-fixture</artifactId>' \
    '  <version>0.1.0-SNAPSHOT</version>' \
    '  <properties>' \
    '    <egon-cola.version>5.3.3</egon-cola.version>' \
    '  </properties>' \
    '</project>' \
    >"$root/egon-cola-archetypes/source-projects/egon-cola-source-fixture/pom.xml"

  printf '%s\n' "./mvnw -DarchetypeVersion='5.3.3'" >"$root/README.md"
  printf '%s\n' 'generated sentinel' \
    >"$root/egon-cola-archetypes/.generated/egon-cola-archetype-fixture/sentinel.txt"

  printf '%s\n' \
    '#!/usr/bin/env bash' \
    'set -Eeuo pipefail' \
    'ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"' \
    'if [[ " $* " == *" help:evaluate "* ]]; then' \
    '  sed -n "s:.*<version>\\([^<]*\\)</version>.*:\\1:p" "$ROOT/pom.xml" | tail -n 1' \
    '  exit 0' \
    'fi' \
    'if [[ " $* " == *" org.codehaus.mojo:versions-maven-plugin:2.16.2:set "* ]]; then' \
    '  new_version=""' \
    '  for argument in "$@"; do' \
    '    if [[ "$argument" == -DnewVersion=* ]]; then new_version="${argument#-DnewVersion=}"; fi' \
    '  done' \
    '  [[ -n "$new_version" ]]' \
    '  sed "s|<version>5\\.3\\.3</version>|<version>${new_version}</version>|" "$ROOT/pom.xml" >"$ROOT/pom.xml.tmp"' \
    '  mv -- "$ROOT/pom.xml.tmp" "$ROOT/pom.xml"' \
    '  exit 0' \
    'fi' \
    'if [[ " $* " == *" validate "* && "${FAIL_VALIDATE:-0}" == 1 ]]; then exit 42; fi' \
    'exit 0' >"$root/mvnw"
  chmod +x "$root/mvnw"

  cp -- "$PROJECT_ROOT/scripts/bump_cola_version.sh" "$root/scripts/bump_cola_version.sh"
  chmod +x "$root/scripts/bump_cola_version.sh"
}

write_fixture

ROOT="$FIXTURE_ROOT/repo"
SOURCE_POM="$ROOT/egon-cola-archetypes/source-projects/egon-cola-source-fixture/pom.xml"
README="$ROOT/README.md"
GENERATED="$ROOT/egon-cola-archetypes/.generated"

generated_before="$(hash_tree "$GENERATED")"
"$ROOT/scripts/bump_cola_version.sh" 5.3.4 >/dev/null

assert_file_contains "$ROOT/pom.xml" '<version>5.3.4</version>'
assert_file_contains "$SOURCE_POM" '<egon-cola.version>5.3.4</egon-cola.version>'
assert_file_contains "$SOURCE_POM" '<version>0.1.0-SNAPSHOT</version>'
assert_file_contains "$README" "-DarchetypeVersion='5.3.4'"
[[ "$(hash_tree "$GENERATED")" == "$generated_before" ]] || \
  die 'generated workspace changed during version update'

cp -- "$ROOT/pom.xml" "$FIXTURE_ROOT/pom.success"
cp -- "$SOURCE_POM" "$FIXTURE_ROOT/source.success"
cp -- "$README" "$FIXTURE_ROOT/readme.success"
generated_before_failure="$(hash_tree "$GENERATED")"

set +e
FAIL_VALIDATE=1 "$ROOT/scripts/bump_cola_version.sh" 5.3.5 >"$FIXTURE_ROOT/failure.log" 2>&1
failure_rc=$?
set -e
[[ "$failure_rc" -ne 0 ]] || die 'injected Maven validation failure unexpectedly succeeded'
cmp -s "$ROOT/pom.xml" "$FIXTURE_ROOT/pom.success" || die 'root POM was not rolled back'
cmp -s "$SOURCE_POM" "$FIXTURE_ROOT/source.success" || die 'source POM was not rolled back'
cmp -s "$README" "$FIXTURE_ROOT/readme.success" || die 'README was not rolled back'
[[ "$(hash_tree "$GENERATED")" == "$generated_before_failure" ]] || \
  die 'generated workspace changed during failed version update'

set +e
"$ROOT/scripts/bump_cola_version.sh" 'invalid version' >/dev/null 2>&1
invalid_rc=$?
set -e
[[ "$invalid_rc" -ne 0 ]] || die 'invalid version was accepted'

printf '%s\n' 'bump version fixture: PASS'
