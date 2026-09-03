#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd -P)"
readonly REPO_ROOT

fail() {
  printf 'spring-dependency-management-test: %s\n' "$*" >&2
  exit 1
}

assert_contains() {
  local file="$1" expected="$2" context="$3"
  grep -Fq -- "$expected" "$file" || fail "${context}: missing '${expected}' in ${file}"
}

assert_not_contains() {
  local file="$1" unexpected="$2" context="$3"
  if grep -Fq -- "$unexpected" "$file"; then
    fail "${context}: unexpected '${unexpected}' in ${file}"
  fi
}

count_occurrences() {
  local file="$1" needle="$2"
  awk -v needle="$needle" 'index($0, needle) > 0 { count++ } END { print count + 0 }' "$file"
}

assert_count() {
  local file="$1" needle="$2" expected="$3" context="$4" actual
  actual="$(count_occurrences "$file" "$needle")"
  [[ "$actual" == "$expected" ]] || fail "${context}: expected ${expected}, got ${actual} in ${file}"
}

assert_parent_boot_only() {
  local file="$1"
  assert_contains "$file" '<artifactId>spring-boot-starter-parent</artifactId>' 'source root Boot Parent'
  assert_contains "$file" '<version>3.5.16</version>' 'source root Boot version'
  assert_count "$file" '<artifactId>spring-boot-dependencies</artifactId>' 0 'source root duplicate Boot BOM'
}

assert_springdoc_dependencies_are_managed() {
  local file="$1" local_managed_dependencies
  assert_contains "$file" '<artifactId>springdoc-openapi-bom</artifactId>' 'Springdoc BOM'
  assert_contains "$file" '<springdoc.version>2.8.17</springdoc.version>' 'Springdoc version property'
  assert_contains "$file" '<version>${springdoc.version}</version>' 'Springdoc BOM version'
  local_managed_dependencies="$(awk '
    /<dependencyManagement>/ { in_dependency_management=1 }
    /<\/dependencyManagement>/ { in_dependency_management=0 }
    in_dependency_management && /<artifactId>springdoc-openapi-starter-/ { print; found=1 }
    END { exit found ? 0 : 1 }
  ' "$file" || true)"
  [[ -z "$local_managed_dependencies" ]] || fail "Springdoc starter must not be locally overridden: ${file}"
}

ROOT_POM="${REPO_ROOT}/pom.xml"
assert_contains "$ROOT_POM" '<spring.boot.version>3.5.16</spring.boot.version>' 'root Spring Boot property'
assert_count "$ROOT_POM" '<artifactId>spring-boot-dependencies</artifactId>' 1 'root Boot BOM ownership'

for child_pom in \
  "${REPO_ROOT}/egon-cola-components/pom.xml" \
  "${REPO_ROOT}/egon-cola-platforms/pom.xml" \
  "${REPO_ROOT}/egon-cola-archetypes/pom.xml"; do
  assert_contains "$child_pom" '<artifactId>egon-cola-aggregation-parent</artifactId>' 'child root Parent'
  assert_not_contains "$child_pom" '<artifactId>spring-boot-dependencies</artifactId>' 'child duplicate Boot BOM'
  assert_not_contains "$child_pom" '<spring.boot.version>' 'child duplicate Boot property'
done

for source_pom in \
  "${REPO_ROOT}/egon-cola-archetypes/source-projects/egon-cola-source-light/pom.xml" \
  "${REPO_ROOT}/egon-cola-archetypes/source-projects/egon-cola-source-light-open/pom.xml" \
  "${REPO_ROOT}/egon-cola-archetypes/source-projects/egon-cola-source-service/pom.xml" \
  "${REPO_ROOT}/egon-cola-archetypes/source-projects/egon-cola-source-service-open/pom.xml" \
  "${REPO_ROOT}/egon-cola-archetypes/source-projects/egon-cola-source-web/pom.xml" \
  "${REPO_ROOT}/egon-cola-archetypes/source-projects/egon-cola-source-web-open/pom.xml"; do
  assert_parent_boot_only "$source_pom"
done

for springdoc_pom in \
  "${REPO_ROOT}/egon-cola-archetypes/source-projects/egon-cola-source-light/pom.xml" \
  "${REPO_ROOT}/egon-cola-archetypes/source-projects/egon-cola-source-light-open/pom.xml" \
  "${REPO_ROOT}/egon-cola-archetypes/source-projects/egon-cola-source-web/pom.xml" \
  "${REPO_ROOT}/egon-cola-archetypes/source-projects/egon-cola-source-web-open/pom.xml"; do
  assert_springdoc_dependencies_are_managed "$springdoc_pom"
done

assert_contains "$ROOT_POM" '<flyway.version>11.15.0</flyway.version>' 'Flyway exception ledger'
assert_contains "$ROOT_POM" '<postgresql.version>42.7.8</postgresql.version>' 'PostgreSQL exception ledger'

for isolated_pom in \
  "${REPO_ROOT}/egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/method-extension-spring/pom.xml" \
  "${REPO_ROOT}/egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/observation-spring-proxies/pom.xml"; do
  [[ -f "$isolated_pom" ]] || fail "isolated Spring IT POM is missing: ${isolated_pom}"
  assert_contains "$isolated_pom" '<version>3.5.16</version>' 'isolated Spring IT baseline'
done

printf '%s\n' 'spring-dependency-management-test: PASS'
