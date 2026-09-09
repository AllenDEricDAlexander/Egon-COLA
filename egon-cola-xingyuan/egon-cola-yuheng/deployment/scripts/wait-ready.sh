#!/usr/bin/env bash
set -euo pipefail

if [[ "${1:-}" == "--engines" ]]; then
  for port in 18083 18183 18085 18185; do
    "${BASH_SOURCE[0]}" "http://127.0.0.1:${port}/actuator/health/readiness" "${2:-120}"
  done
  exit 0
fi

url="${1:?readiness URL is required}"
timeout_seconds="${2:-120}"
[[ "${url}" == http://* || "${url}" == https://* ]] || { echo "expected HTTP(S) readiness URL" >&2; exit 2; }
[[ "${timeout_seconds}" =~ ^[1-9][0-9]*$ ]] || { echo "timeout must be a positive integer" >&2; exit 2; }
args=(--fail --silent --show-error --max-time 3)
[[ -z "${YUHENG_HEALTH_CA:-}" ]] || args+=(--cacert "${YUHENG_HEALTH_CA}")
if [[ -n "${YUHENG_HEALTH_CERT:-}" || -n "${YUHENG_HEALTH_KEY:-}" ]]; then
  args+=(--cert "${YUHENG_HEALTH_CERT:?both health certificate and key are required}" --key "${YUHENG_HEALTH_KEY:?both health certificate and key are required}")
fi
deadline="$((SECONDS + timeout_seconds))"
until curl "${args[@]}" "${url}" >/dev/null; do
  if ((SECONDS >= deadline)); then
    echo "timed out waiting for ${url}" >&2
    exit 1
  fi
  sleep 1
done
