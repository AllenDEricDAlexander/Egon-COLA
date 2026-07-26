#!/usr/bin/env bash
set -euo pipefail

url="${1:?readiness URL is required}"
timeout_seconds="${2:-120}"
deadline="$((SECONDS + timeout_seconds))"

until curl --fail --silent --show-error --max-time 3 "${url}" >/dev/null; do
  if ((SECONDS >= deadline)); then
    echo "timed out waiting for ${url}" >&2
    exit 1
  fi
  sleep 1
done
