#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MVNW="${ROOT_DIR}/mvnw"
GENERATOR="${ROOT_DIR}/scripts/generate_archetypes.sh"

usage() {
  cat <<'USAGE'
Usage: scripts/maven-deploy.sh [target] [options]

Targets:
  all                               Publish the complete root reactor (default)
  archetypes                        Verify/deploy the complete archetypes reactor
  egon-cola-archetype-light-open    Verify/deploy the Light Open archetype
  egon-cola-archetype-service-open  Verify/deploy the Service Open archetype
  egon-cola-archetype-web-open      Verify/deploy the Web Open archetype
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
    archetypes \
    egon-cola-archetype-light-open \
    egon-cola-archetype-service-open \
    egon-cola-archetype-web-open
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
    all|archetypes|egon-cola-archetype-light-open|egon-cola-archetype-service-open|egon-cola-archetype-web-open)
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

project_args=()
case "${target}" in
  all)
    project_args=(-f pom.xml)
    ;;
  archetypes)
    project_args=(-f egon-cola-archetypes/pom.xml)
    ;;
  egon-cola-archetype-light-open|egon-cola-archetype-service-open|egon-cola-archetype-web-open)
    project_args=(-f egon-cola-archetypes/pom.xml -pl ":${target}" -am)
    ;;
esac

cd "${ROOT_DIR}"

run_preflight() {
  echo "Running mandatory source-to-archetype preflight..."
  "${MVNW}" -B -ntp -N install
  "${MVNW}" -B -ntp -N -f egon-cola-archetypes/pom.xml install
  "${MVNW}" -B -ntp -f egon-cola-archetypes/source-projects/pom.xml clean install
  "${GENERATOR}" generate
  "${GENERATOR}" check
  if [[ -n "$(git ls-files -- egon-cola-archetypes/.generated)" ]]; then
    echo "Generated archetype workspace must remain ignored and untracked." >&2
    exit 1
  fi
  "${MVNW}" -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test
  "${MVNW}" -B -ntp -Prelease -Dgpg.skip=true clean verify
}

echo "Maven target: ${target}"
echo "Maven mode: ${mode}"
run_preflight

if [[ "${mode}" == deploy ]]; then
  version="$("${MVNW}" -f pom.xml -q -N help:evaluate -Dexpression=project.version -DforceStdout)"
  if [[ -z "${version}" || "${version}" == *-SNAPSHOT ]]; then
    echo "Maven Central publish requires a non-SNAPSHOT project version; resolved '${version}'." >&2
    exit 1
  fi
  maven_args=(-B -ntp -Prelease -DtrimStackTrace=false)
  if [[ "${skip_tests}" == true ]]; then
    maven_args+=(-DskipTests)
  fi
  lifecycle=(clean deploy)
  echo "Maven command: ./mvnw ${project_args[*]} ${maven_args[*]} ${lifecycle[*]}"
  "${MVNW}" "${project_args[@]}" "${maven_args[@]}" "${lifecycle[@]}"
else
  echo "Preflight completed; no Maven deploy was requested."
fi
