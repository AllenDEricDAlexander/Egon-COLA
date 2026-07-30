#!/usr/bin/env bash

set -euo pipefail

PERFORMANCE_DIRECTORY="$(
  cd "$(dirname "${BASH_SOURCE[0]}")"
  pwd
)"
DURATION_SECONDS="${GATEWAY_SOAK_DURATION_SECONDS:-86400}"
OUTPUT_DIRECTORY="${GATEWAY_PERF_OUTPUT_DIRECTORY:-${PERFORMANCE_DIRECTORY}/artifacts}"

if [[ ! "${DURATION_SECONDS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "GATEWAY_SOAK_DURATION_SECONDS must be a positive integer" >&2
  exit 2
fi

mkdir -p "${OUTPUT_DIRECTORY}"
RESOURCE_SAMPLER_PID=""
TOTAL_DURATION_SECONDS="$((DURATION_SECONDS * 2))"

cleanup() {
  if [[ -n "${RESOURCE_SAMPLER_PID}" ]] \
      && kill -0 "${RESOURCE_SAMPLER_PID}" 2>/dev/null; then
    kill "${RESOURCE_SAMPLER_PID}" 2>/dev/null || true
    wait "${RESOURCE_SAMPLER_PID}" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

"${PERFORMANCE_DIRECTORY}/sample-resources.sh" \
  "${TOTAL_DURATION_SECONDS}" \
  "${OUTPUT_DIRECTORY}/resources.csv" &
RESOURCE_SAMPLER_PID="$!"

export GATEWAY_PERF_PROFILE=soak
export GATEWAY_PERF_DURATION="${DURATION_SECONDS}s"

"${PERFORMANCE_DIRECTORY}/run-k6.sh" http
"${PERFORMANCE_DIRECTORY}/run-k6.sh" rpc
