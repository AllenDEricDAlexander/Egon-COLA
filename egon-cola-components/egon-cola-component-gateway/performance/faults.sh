#!/usr/bin/env bash

set -euo pipefail

PERFORMANCE_DIRECTORY="$(
  cd "$(dirname "${BASH_SOURCE[0]}")"
  pwd
)"
DEPLOYMENT_DIRECTORY="${PERFORMANCE_DIRECTORY}/../deployment"
TARGET="${1:-}"
DURATION_SECONDS="${2:-30}"
OUTPUT_DIRECTORY="${GATEWAY_PERF_OUTPUT_DIRECTORY:-${PERFORMANCE_DIRECTORY}/artifacts}"
PAUSED_TARGET=""
TARGET_KIND=""

if [[ ! "${DURATION_SECONDS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "fault duration must be a positive number of seconds" >&2
  exit 2
fi

case "${TARGET}" in
  kafka)
    PAUSED_TARGET="kafka"
    TARGET_KIND="compose"
    ;;
  redis)
    PAUSED_TARGET="${GATEWAY_FAULT_REDIS_SERVICE:-rate-limit-redis}"
    if [[ "${PAUSED_TARGET}" != "rate-limit-redis" \
        && "${PAUSED_TARGET}" != "ddc-redis" ]]; then
      echo "GATEWAY_FAULT_REDIS_SERVICE must be rate-limit-redis or ddc-redis" >&2
      exit 2
    fi
    TARGET_KIND="compose"
    ;;
  postgres)
    PAUSED_TARGET="postgres"
    TARGET_KIND="compose"
    ;;
  provider)
    PAUSED_TARGET="${GATEWAY_PROVIDER_CONTAINER:-}"
    if [[ -z "${PAUSED_TARGET}" ]]; then
      echo "GATEWAY_PROVIDER_CONTAINER is required for provider faults" >&2
      exit 2
    fi
    TARGET_KIND="container"
    ;;
  *)
    echo "usage: $0 <kafka|redis|postgres|provider> [duration-seconds]" >&2
    exit 2
    ;;
esac

mkdir -p "${OUTPUT_DIRECTORY}"
COMPOSE=(
  docker compose
  --env-file "${DEPLOYMENT_DIRECTORY}/.env"
  --file "${DEPLOYMENT_DIRECTORY}/compose.yml"
)

restore_target() {
  if [[ "${TARGET_KIND}" == "compose" ]]; then
    "${COMPOSE[@]}" unpause "${PAUSED_TARGET}" >/dev/null 2>&1 || true
  elif [[ "${TARGET_KIND}" == "container" ]]; then
    docker unpause "${PAUSED_TARGET}" >/dev/null 2>&1 || true
  fi
}
trap restore_target EXIT INT TERM

date -u +%Y-%m-%dT%H:%M:%SZ \
  > "${OUTPUT_DIRECTORY}/fault-${TARGET}-started.txt"
if [[ "${TARGET_KIND}" == "compose" ]]; then
  "${COMPOSE[@]}" ps > "${OUTPUT_DIRECTORY}/fault-${TARGET}-before.txt"
  "${COMPOSE[@]}" pause "${PAUSED_TARGET}"
else
  docker inspect "${PAUSED_TARGET}" \
    > "${OUTPUT_DIRECTORY}/fault-${TARGET}-before.json"
  docker pause "${PAUSED_TARGET}"
fi

sleep "${DURATION_SECONDS}"
restore_target
trap - EXIT INT TERM

date -u +%Y-%m-%dT%H:%M:%SZ \
  > "${OUTPUT_DIRECTORY}/fault-${TARGET}-recovered.txt"
if [[ "${TARGET_KIND}" == "compose" ]]; then
  "${COMPOSE[@]}" ps > "${OUTPUT_DIRECTORY}/fault-${TARGET}-after.txt"
  "${COMPOSE[@]}" logs --no-color --tail 500 "${PAUSED_TARGET}" \
    > "${OUTPUT_DIRECTORY}/fault-${TARGET}.log"
else
  docker inspect "${PAUSED_TARGET}" \
    > "${OUTPUT_DIRECTORY}/fault-${TARGET}-after.json"
  docker logs --tail 500 "${PAUSED_TARGET}" \
    > "${OUTPUT_DIRECTORY}/fault-${TARGET}.log" 2>&1
fi
