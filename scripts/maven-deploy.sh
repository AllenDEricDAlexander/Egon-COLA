#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MVNW="${ROOT_DIR}/mvnw"
GENERATOR="${ROOT_DIR}/scripts/generate_archetypes.sh"
CHECK_WRAPPER="${ROOT_DIR}/scripts/check_archetypes.sh"

usage() {
  cat <<'USAGE'
Usage: scripts/maven-deploy.sh [target] [options]

Targets:
  all                               Publish the complete root reactor (default)
  archetypes                        Verify the complete generated archetypes reactor
  list                              Print the supported targets and exit

Options:
  --dry-run                         Run the mandatory preflight only (default)
  --publish                         Run the Maven deploy lifecycle
  --skip-tests                      Add -DskipTests only to the final deploy after preflight
  -h, --help                        Show this help

The script always runs the source install, archetype generation/check, full
archetype integration tests, and a no-signature release-shape preflight before
the optional final deploy. It never starts a business application or executes
database SQL. A real publish is opt-in through --publish; Central production
publishing is documented for the all target only.
USAGE
}

list_targets() {
  printf '%s\n' \
    all \
    archetypes
}

target="all"
mode="verify"
skip_tests=false
target_set=false

for argument in "$@"; do
  case "${argument}" in
    --dry-run)
      mode="verify"
      ;;
    --publish)
      mode="deploy"
      ;;
    --skip-tests)
      skip_tests=true
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    list)
      if [[ "${target_set}" == true ]]; then
        echo "Target 'list' cannot be combined with another target." >&2
        exit 2
      fi
      list_targets
      exit 0
      ;;
    all|archetypes)
      if [[ "${target_set}" == true ]]; then
        echo "Only one Maven publish target may be selected." >&2
        exit 2
      fi
      target="${argument}"
      target_set=true
      ;;
    *)
      echo "Unsupported Maven publish target or option: ${argument}" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if [[ ! -x "${MVNW}" ]]; then
  echo "Maven wrapper is not executable: ${MVNW}" >&2
  exit 1
fi

if [[ ! -x "${GENERATOR}" ]]; then
  echo "Archetype generator is not executable: ${GENERATOR}" >&2
  exit 1
fi

if [[ ! -x "${CHECK_WRAPPER}" ]]; then
  echo "Archetype check wrapper is not executable: ${CHECK_WRAPPER}" >&2
  exit 1
fi

project_args=()
case "${target}" in
  all)
    project_args=(-f pom.xml)
    ;;
  archetypes)
    project_args=(-f egon-cola-archetypes/pom.xml -Pgenerated-archetypes)
    ;;
esac

cd "${ROOT_DIR}"

run_preflight() {
  echo "Running mandatory source-to-archetype preflight..."
  "${MVNW}" -B -ntp -N install
  "${MVNW}" -B -ntp -N -f egon-cola-archetypes/pom.xml install
  "${MVNW}" -B -ntp -f egon-cola-archetypes/source-projects/pom.xml clean install
  "${GENERATOR}" generate
  "${CHECK_WRAPPER}"
  if [[ -n "$(git ls-files -- egon-cola-archetypes/.generated)" ]]; then
    echo "Generated archetype workspace must remain ignored and untracked." >&2
    exit 1
  fi
  "${MVNW}" -B -ntp -f egon-cola-archetypes/pom.xml \
    -Pgenerated-archetypes clean install
  "${MVNW}" -B -ntp -Pgenerated-archetypes -Prelease \
    -Dgpg.skip=true clean verify
  assert_generated_release_shape
}

definition_manifests() {
  find egon-cola-archetypes/definitions -mindepth 2 -maxdepth 2 \
    -type f -name archetype.properties -print | LC_ALL=C sort
}

assert_generated_release_shape() {
  local manifest target module_dir artifact_count=0 actual_targets expected_targets
  expected_targets=$'egon-cola-archetype-agent\negon-cola-archetype-light\negon-cola-archetype-light-open\negon-cola-archetype-service\negon-cola-archetype-service-open\negon-cola-archetype-web\negon-cola-archetype-web-open'
  actual_targets="$(while IFS= read -r manifest; do
    [[ -n "${manifest}" ]] || continue
    sed -n 's/^targetArtifactId=//p' "${manifest}" | tr -d '\r'
  done < <(definition_manifests) | LC_ALL=C sort)"
  [[ "${actual_targets}" == "${expected_targets}" ]] \
    || { echo "Expected exactly seven generated archetype targets, found:\n${actual_targets}" >&2; exit 1; }
  while IFS= read -r manifest; do
    [[ -n "${manifest}" ]] || continue
    target="$(sed -n 's/^targetArtifactId=//p' "${manifest}" | tr -d '\r')"
    [[ "${target}" =~ ^egon-cola-archetype-[A-Za-z0-9-]+$ ]] \
      || { echo "Invalid generated target in ${manifest}: ${target}" >&2; exit 1; }
    module_dir="egon-cola-archetypes/.generated/${target}"
    test -f "${module_dir}/pom.xml"
    grep -Fq "<artifactId>${target}</artifactId>" "${module_dir}/pom.xml"
    test "$(find "${module_dir}/target" -maxdepth 1 -type f \
      -name "${target}-*.jar" ! -name '*-sources.jar' ! -name '*-javadoc.jar' | wc -l | tr -d ' ')" -eq 1
    test "$(find "${module_dir}/target" -maxdepth 1 -type f \
      -name "${target}-*-sources.jar" | wc -l | tr -d ' ')" -eq 1
    test "$(find "${module_dir}/target" -maxdepth 1 -type f \
      -name "${target}-*-javadoc.jar" | wc -l | tr -d ' ')" -eq 1
    artifact_count=$((artifact_count + 1))
  done < <(definition_manifests)
  [[ "${artifact_count}" -eq 7 ]] \
    || { echo "Expected seven generated archetype artifacts, found ${artifact_count}." >&2; exit 1; }
}

echo "Maven target: ${target}"
echo "Maven mode: ${mode}"
if [[ "${mode}" == deploy && "${target}" != all ]]; then
  echo "The --publish option only supports the all target; use archetypes for dry-run verification." >&2
  exit 2
fi

run_preflight

if [[ "${mode}" == deploy ]]; then
  version="$("${MVNW}" -f pom.xml -q -N help:evaluate -Dexpression=project.version -DforceStdout)"
  if [[ -z "${version}" || "${version}" == *-SNAPSHOT ]]; then
    echo "Maven Central publish requires a non-SNAPSHOT project version; resolved '${version}'." >&2
    exit 1
  fi
  maven_args=(-B -ntp -Pgenerated-archetypes -Prelease -DtrimStackTrace=false)
  if [[ "${skip_tests}" == true ]]; then
    maven_args+=(-DskipTests)
  fi
  lifecycle=(clean deploy)
  echo "Maven command: ./mvnw ${project_args[*]} ${maven_args[*]} ${lifecycle[*]}"
  "${MVNW}" "${project_args[@]}" "${maven_args[@]}" "${lifecycle[@]}"
else
  echo "Preflight completed; no Maven deploy was requested."
fi
