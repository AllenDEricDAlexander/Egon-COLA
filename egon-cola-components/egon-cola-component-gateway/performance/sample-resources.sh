#!/usr/bin/env bash

set -euo pipefail

DURATION_SECONDS="${1:-}"
INTERVAL_SECONDS="${GATEWAY_RESOURCE_SAMPLE_INTERVAL_SECONDS:-10}"
OUTPUT_FILE="${2:-resources.csv}"

if [[ ! "${DURATION_SECONDS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "duration must be a positive number of seconds" >&2
  exit 2
fi
if [[ ! "${INTERVAL_SECONDS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "sample interval must be a positive number of seconds" >&2
  exit 2
fi
if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required for container resource sampling" >&2
  exit 1
fi

mkdir -p "$(dirname "${OUTPUT_FILE}")"
echo "timestamp,name,cpu_percent,memory_usage,network_io,block_io,pids" \
  > "${OUTPUT_FILE}"

STARTED_AT="$(date +%s)"
while (( "$(date +%s)" - STARTED_AT < DURATION_SECONDS )); do
  docker stats --no-stream \
    --format '{{.Name}},{{.CPUPerc}},{{.MemUsage}},{{.NetIO}},{{.BlockIO}},{{.PIDs}}' \
    | while IFS= read -r line; do
      echo "$(date -u +%Y-%m-%dT%H:%M:%SZ),${line}"
    done >> "${OUTPUT_FILE}"
  sleep "${INTERVAL_SECONDS}"
done
