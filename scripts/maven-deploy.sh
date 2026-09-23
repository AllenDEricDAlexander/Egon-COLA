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
  archetypes                        Verify the complete generated archetypes reactor
  list                              Print the supported targets and exit

Options:
  --dry-run                         Run the mandatory preflight only (default)
  --publish                         Run preflight, then the Maven deploy lifecycle
  --fast                            Publish immediately. Skip tests, dry-run, and
                                    release-shape. Still regenerates archetypes so
                                    published coordinates match this version.

  --skip-tests                      Skip Maven test execution in both preflight
                                    and final deploy

  --skip-deploy-tests               Run the complete preflight with tests, but
                                    skip test execution during the final deploy

  -h, --help                        Show this help

Behavior:

  Default:
    Run the complete mandatory preflight with tests.
    No deploy is performed unless --publish or --fast is specified.

  --fast:
    Regenerate archetypes from the current source, then deploy once.
    Do not run tests, archetype IT, or release-shape.
    A release version check still runs; it is not a dry-run or test.

  --skip-tests:
    Skip Maven test execution throughout the script.
    Archetype generation and release-shape checks still run.

  --skip-deploy-tests:
    Preflight still runs with the complete test suite.
    Tests are skipped only when the final deploy lifecycle runs.

Mandatory preflight:

  1. Install the root parent POM
  2. Install the archetype parent POM
  3. Build the archetype source projects
  4. Reinstall the four peer facades with resolvable consumer POMs
  5. Generate archetypes
  6. Verify the generated set is deterministic
  7. Build the generated archetype reactor, excluding those facades
  8. Run release-shape verification, excluding those facades

The four peer facades form a cross-family cycle: each service reactor consumes the
web facade, and each web reactor consumes the service facade. Their source parent
version is the literal property egon-cola.version, which consumers cannot resolve.
Publish installs a standalone consumer POM for each facade first, then publishes the
remaining reactor without those four coordinates.

The script never starts a business application or executes database SQL.

A real publish is opt-in through --publish or --fast.
Central production publishing is supported for the all target only.
--fast is the one-command publish and does not run the preflight below.
It still publishes the four peer facades before the remaining reactor.
USAGE
}

list_targets() {
  printf '%s\n' \
    all \
    archetypes
}

# ---------------------------------------------------------------------------
# Generated archetype definitions
# ---------------------------------------------------------------------------

definition_manifests() {
  find egon-cola-archetypes/definitions \
    -mindepth 2 \
    -maxdepth 2 \
    -type f \
    -name archetype.properties \
    -print | LC_ALL=C sort
}

# Peer contracts consumed across families. Publish these before the reactor that
# runs archetype integration tests, and do not publish them again afterwards.
peer_facade_poms=(
  egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-facade/pom.xml
  egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-facade/pom.xml
  egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-facade/pom.xml
  egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-facade/pom.xml
)

peer_facade_exclusions='!:egon-cola-source-service-facade,!:egon-cola-source-web-facade,!:egon-cola-source-service-open-facade,!:egon-cola-source-web-open-facade'

run_peer_facades() {
  local pom
  for pom in "${peer_facade_poms[@]}"; do
    echo "Peer facade: ${pom}"
    "${MVNW}" -B -ntp -f "${pom}" "$@"
  done
}

# ---------------------------------------------------------------------------
# Assert generated release artifacts
# ---------------------------------------------------------------------------

assert_generated_release_shape() {
  local manifest
  local target
  local module_dir
  local artifact_count=0
  local actual_targets
  local expected_targets

  expected_targets=$'egon-cola-archetype-agent\negon-cola-archetype-light\negon-cola-archetype-light-open\negon-cola-archetype-service\negon-cola-archetype-service-open\negon-cola-archetype-web\negon-cola-archetype-web-open'

  actual_targets="$(
    while IFS= read -r manifest; do
      [[ -n "${manifest}" ]] || continue

      sed -n 's/^targetArtifactId=//p' "${manifest}" | tr -d '\r'
    done < <(definition_manifests) | LC_ALL=C sort
  )"

  if [[ "${actual_targets}" != "${expected_targets}" ]]; then
    printf 'Expected exactly seven generated archetype targets, found:\n%s\n' \
      "${actual_targets}" >&2
    exit 1
  fi

  while IFS= read -r manifest; do
    [[ -n "${manifest}" ]] || continue

    target="$(
      sed -n 's/^targetArtifactId=//p' "${manifest}" | tr -d '\r'
    )"

    if [[ ! "${target}" =~ ^egon-cola-archetype-[A-Za-z0-9-]+$ ]]; then
      echo "Invalid generated target in ${manifest}: ${target}" >&2
      exit 1
    fi

    module_dir="egon-cola-archetypes/.generated/${target}"

    if [[ ! -f "${module_dir}/pom.xml" ]]; then
      echo "Generated archetype POM does not exist: ${module_dir}/pom.xml" >&2
      exit 1
    fi

    if ! grep -Fq \
      "<artifactId>${target}</artifactId>" \
      "${module_dir}/pom.xml"; then
      echo "Generated archetype POM has unexpected artifactId: ${target}" >&2
      exit 1
    fi

    if [[ "$(
      find "${module_dir}/target" \
        -maxdepth 1 \
        -type f \
        -name "${target}-*.jar" \
        ! -name '*-sources.jar' \
        ! -name '*-javadoc.jar' |
        wc -l |
        tr -d ' '
    )" -ne 1 ]]; then
      echo "Expected exactly one main JAR for ${target}." >&2
      exit 1
    fi

    if [[ "$(
      find "${module_dir}/target" \
        -maxdepth 1 \
        -type f \
        -name "${target}-*-sources.jar" |
        wc -l |
        tr -d ' '
    )" -ne 1 ]]; then
      echo "Expected exactly one sources JAR for ${target}." >&2
      exit 1
    fi

    if [[ "$(
      find "${module_dir}/target" \
        -maxdepth 1 \
        -type f \
        -name "${target}-*-javadoc.jar" |
        wc -l |
        tr -d ' '
    )" -ne 1 ]]; then
      echo "Expected exactly one javadoc JAR for ${target}." >&2
      exit 1
    fi

    artifact_count=$((artifact_count + 1))
  done < <(definition_manifests)

  if [[ "${artifact_count}" -ne 7 ]]; then
    echo \
      "Expected seven generated archetype artifacts, found ${artifact_count}." \
      >&2
    exit 1
  fi
}

# ---------------------------------------------------------------------------
# Arguments
# ---------------------------------------------------------------------------

target="all"
mode="verify"

skip_tests=false
skip_deploy_tests=false
fast=false
dry_run=false

target_set=false

for argument in "$@"; do
  case "${argument}" in

    --dry-run)
      dry_run=true
      mode="verify"
      ;;

    --publish)
      mode="deploy"
      ;;

    --fast)
      fast=true
      mode="deploy"
      skip_tests=true
      ;;

    --skip-tests)
      skip_tests=true
      ;;

    --skip-deploy-tests)
      skip_deploy_tests=true
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
      echo >&2
      usage >&2
      exit 2
      ;;
  esac
done

# ---------------------------------------------------------------------------
# Environment validation
# ---------------------------------------------------------------------------

if [[ ! -x "${MVNW}" ]]; then
  echo "Maven wrapper is not executable: ${MVNW}" >&2
  exit 1
fi

# ---------------------------------------------------------------------------
# Target configuration
# ---------------------------------------------------------------------------

project_args=()

case "${target}" in
  all)
    project_args=(-f pom.xml)
    ;;

  archetypes)
    project_args=(
      -f egon-cola-archetypes/pom.xml
      -Pgenerated-archetypes
    )
    ;;
esac

cd "${ROOT_DIR}"

# ---------------------------------------------------------------------------
# Argument validation
# ---------------------------------------------------------------------------

if [[ "${fast}" == true && "${dry_run}" == true ]]; then
  echo "--fast cannot be combined with --dry-run." >&2
  exit 2
fi

if [[ "${mode}" == deploy && "${target}" != all ]]; then
  echo \
    "The --publish option only supports the all target; --fast has the same limit. Use archetypes for dry-run verification." \
    >&2
  exit 2
fi

if [[ "${skip_deploy_tests}" == true && "${mode}" != deploy ]]; then
  echo \
    "Note: --skip-deploy-tests has no effect without --publish."
fi

if [[ "${skip_tests}" == true && "${skip_deploy_tests}" == true ]]; then
  echo \
    "Note: --skip-tests already skips tests during deploy; --skip-deploy-tests is redundant."
fi

# ---------------------------------------------------------------------------
# Maven test arguments
# ---------------------------------------------------------------------------

#
# --skip-tests:
#
# Skip Maven test execution during the complete preflight.
#
preflight_test_args=()

if [[ "${skip_tests}" == true ]]; then
  preflight_test_args+=(
    -DskipTests=true
  )
fi

#
# Final deploy tests are skipped when either:
#
#   --skip-tests
#
# or:
#
#   --skip-deploy-tests
#
deploy_test_args=()

if [[ "${skip_tests}" == true || "${skip_deploy_tests}" == true ]]; then
  deploy_test_args+=(
    -DskipTests=true
  )
fi

# ---------------------------------------------------------------------------
# Preflight
# ---------------------------------------------------------------------------

run_preflight() {
  if [[ ! -x "${GENERATOR}" ]]; then
    echo "Archetype generator is not executable: ${GENERATOR}" >&2
    exit 1
  fi

  echo
  echo "============================================================"
  echo "Running mandatory source-to-archetype preflight"
  echo "============================================================"

  if [[ "${skip_tests}" == true ]]; then
    echo "Preflight Maven tests: SKIPPED"
  else
    echo "Preflight Maven tests: ENABLED"
  fi

  echo

  # -------------------------------------------------------------------------
  # 1. Root parent
  # -------------------------------------------------------------------------

  echo "[1/7] Installing root parent POM..."

  "${MVNW}" \
    -B \
    -ntp \
    -N \
    "${preflight_test_args[@]+"${preflight_test_args[@]}"}" \
    install

  # -------------------------------------------------------------------------
  # 2. Archetype parent
  # -------------------------------------------------------------------------

  echo
  echo "[2/7] Installing archetype parent POM..."

  "${MVNW}" \
    -B \
    -ntp \
    -N \
    -f egon-cola-archetypes/pom.xml \
    "${preflight_test_args[@]+"${preflight_test_args[@]}"}" \
    install

  # -------------------------------------------------------------------------
  # 3. Source projects
  # -------------------------------------------------------------------------

  echo
  echo "[3/8] Building archetype source projects..."

  "${MVNW}" \
    -B \
    -ntp \
    -f egon-cola-archetypes/source-projects/pom.xml \
    "${preflight_test_args[@]+"${preflight_test_args[@]}"}" \
    clean \
    install

  # The source reactor installs facade POMs whose parent version is still
  # ${egon-cola.version}. Replace those repository POMs before any external build.
  echo
  echo "[4/8] Installing peer facades with resolvable consumer POMs..."

  run_peer_facades \
    -Ppublish-resolved-facade \
    "${preflight_test_args[@]+"${preflight_test_args[@]}"}" \
    install

  # -------------------------------------------------------------------------
  # 5. Generate archetypes
  # -------------------------------------------------------------------------

  echo
  echo "[5/8] Generating archetypes..."

  "${GENERATOR}" generate

  # Generated workspace must not become part of the Git repository.
  if [[ -n "$(git ls-files -- egon-cola-archetypes/.generated)" ]]; then
    echo \
      "Generated archetype workspace must remain ignored and untracked." \
      >&2
    exit 1
  fi

  # -------------------------------------------------------------------------
  # 6. Deterministic generation check
  # -------------------------------------------------------------------------

  echo
  echo "[6/8] Verifying generated archetypes are deterministic..."

  "${GENERATOR}" check

  # -------------------------------------------------------------------------
  # 7. Generated archetype reactor
  # -------------------------------------------------------------------------

  echo
  echo "[7/8] Building generated archetype reactor..."

  "${MVNW}" \
    -B \
    -ntp \
    -f egon-cola-archetypes/pom.xml \
    -Pgenerated-archetypes \
    -pl "${peer_facade_exclusions}" \
    "${preflight_test_args[@]+"${preflight_test_args[@]}"}" \
    clean \
    install

  # -------------------------------------------------------------------------
  # 8. Release-shape verification
  # -------------------------------------------------------------------------

  echo
  echo "[8/8] Running release-shape verification..."

  "${MVNW}" \
    -B \
    -ntp \
    -Pgenerated-archetypes \
    -Prelease \
    -pl "${peer_facade_exclusions}" \
    -Dgpg.skip=true \
    "${preflight_test_args[@]+"${preflight_test_args[@]}"}" \
    clean \
    verify

  echo
  echo "Verifying generated release artifacts..."

  assert_generated_release_shape

  echo
  echo "============================================================"
  echo "Mandatory preflight completed successfully"
  echo "============================================================"
}

# ---------------------------------------------------------------------------
# Execution summary
# ---------------------------------------------------------------------------

echo "Maven target: ${target}"
echo "Maven mode: ${mode}"
echo "Fast publish: ${fast}"
echo "Skip all Maven tests: ${skip_tests}"
echo "Skip final deploy tests: ${skip_deploy_tests}"

# ---------------------------------------------------------------------------
# Run preflight
# ---------------------------------------------------------------------------

if [[ "${fast}" == true ]]; then
  echo
  echo "Fast publish: skipping preflight and Maven tests."
else
  run_preflight
fi

# ---------------------------------------------------------------------------
# Deploy
# ---------------------------------------------------------------------------

if [[ "${mode}" == deploy ]]; then

  echo
  echo "Resolving project version..."

  version="$(
    "${MVNW}" \
      -f pom.xml \
      -q \
      -N \
      help:evaluate \
      -Dexpression=project.version \
      -DforceStdout
  )"

  if [[ -z "${version}" || "${version}" == *-SNAPSHOT ]]; then
    echo \
      "Maven Central publish requires a non-SNAPSHOT project version; resolved '${version}'." \
      >&2
    exit 1
  fi

  echo "Release version: ${version}"

  if [[ "${fast}" == true ]]; then
    echo
    echo "Fast publish: installing parents and regenerating archetypes..."
    "${MVNW}" -B -ntp -N -DskipTests install
    "${MVNW}" -B -ntp -N -f egon-cola-components/egon-cola-components-bom/pom.xml -DskipTests install
    "${MVNW}" -B -ntp -N -f egon-cola-archetypes/pom.xml -DskipTests install
    "${GENERATOR}" generate
  fi

  echo
  echo "Publishing the four peer facades before the remaining reactor..."

  run_peer_facades \
    -Prelease \
    -Ppublish-resolved-facade \
    -DtrimStackTrace=false \
    "${deploy_test_args[@]+"${deploy_test_args[@]}"}" \
    clean \
    deploy

  # -------------------------------------------------------------------------
  # Final Maven deploy arguments
  # -------------------------------------------------------------------------

  maven_args=(
    -B
    -ntp
    -Pgenerated-archetypes
    -Prelease
    -pl "${peer_facade_exclusions}"
    -DtrimStackTrace=false
  )

  maven_args+=(
    "${deploy_test_args[@]+"${deploy_test_args[@]}"}"
  )

  lifecycle=(
    clean
    deploy
  )

  echo
  echo "============================================================"
  echo "Starting Maven deploy"
  echo "============================================================"

  if [[ "${fast}" == true ]]; then
    echo "Preflight:             SKIPPED"
    echo "Final deploy tests:    SKIPPED"

  elif [[ "${skip_tests}" == true ]]; then
    echo "Preflight Maven tests: SKIPPED"
    echo "Final deploy tests:    SKIPPED"

  elif [[ "${skip_deploy_tests}" == true ]]; then
    echo "Preflight Maven tests: PASSED"
    echo "Final deploy tests:    SKIPPED"

  else
    echo "Preflight Maven tests: PASSED"
    echo "Final deploy tests:    ENABLED"
  fi

  echo
  printf 'Maven command: ./mvnw'

  printf ' %q' "${project_args[@]}"
  printf ' %q' "${maven_args[@]}"
  printf ' %q' "${lifecycle[@]}"

  printf '\n\n'

  "${MVNW}" \
    "${project_args[@]}" \
    "${maven_args[@]}" \
    "${lifecycle[@]}"

  echo
  echo "============================================================"
  echo "Maven deploy completed successfully"
  echo "============================================================"

else

  echo
  echo "Preflight completed; no Maven deploy was requested."

fi
