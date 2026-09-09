#!/usr/bin/env bash

set -euo pipefail

PERFORMANCE_DIRECTORY="$(
  cd "$(dirname "${BASH_SOURCE[0]}")"
  pwd
)"
SCENARIO="${1:-}"
OUTPUT_DIRECTORY="${YUHENG_PERF_OUTPUT_DIRECTORY:-${PERFORMANCE_DIRECTORY}/artifacts}"

case "${SCENARIO}" in
  http|rpc)
    ;;
  *)
    echo "usage: $0 <http|rpc>" >&2
    exit 2
    ;;
esac

mkdir -p "${OUTPUT_DIRECTORY}"
RESULT_FILE="${OUTPUT_DIRECTORY}/${SCENARIO}-summary.json"

if command -v k6 >/dev/null 2>&1; then
  exec k6 run \
    --summary-export "${RESULT_FILE}" \
    "${PERFORMANCE_DIRECTORY}/${SCENARIO}.js"
fi

if command -v docker >/dev/null 2>&1; then
  exec docker run --rm \
    --network host \
    --env YUHENG_PERF_PROFILE \
    --env YUHENG_PERF_DURATION \
    --env YUHENG_PERF_RATE \
    --env YUHENG_PERF_PRE_ALLOCATED_VUS \
    --env YUHENG_PERF_MAX_VUS \
    --env YUHENG_PUBLIC_BASE_URL \
    --env YUHENG_PUBLIC_HOST \
    --env YUHENG_INTERNAL_BASE_URL \
    --env YUHENG_INTERNAL_HOST \
    --env YUHENG_RPC_CONSUMER_BASE_URL \
    --env YUHENG_RPC_HOST \
    --volume "${PERFORMANCE_DIRECTORY}:/performance:ro" \
    --volume "${OUTPUT_DIRECTORY}:/artifacts" \
    grafana/k6:0.57.0 run \
    --summary-export "/artifacts/${SCENARIO}-summary.json" \
    "/performance/${SCENARIO}.js"
fi

echo "k6 or Docker is required" >&2
exit 1
