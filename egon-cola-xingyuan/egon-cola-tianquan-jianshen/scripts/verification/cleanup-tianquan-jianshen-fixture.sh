#!/usr/bin/env bash

set -euo pipefail
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

usage() {
  cat <<'EOF'
Usage: cleanup-tianquan-jianshen-fixture.sh [--help|--check-config|--clean]

Removes only the exact schema and Redis keys recorded by the preparation script.
The state file is validated before any mutation and retained with a .cleaned
suffix as evidence. --clean requires TIANQUAN_JIANSHEN_FIXTURE_CONFIRM=cleanup.
EOF
}

load_state() {
  rbac3_require_command jq
  rbac3_require_env TIANQUAN_JIANSHEN_IT_STATE_FILE
  [[ -f "${TIANQUAN_JIANSHEN_IT_STATE_FILE}" && ! -L "${TIANQUAN_JIANSHEN_IT_STATE_FILE}" ]] \
    || rbac3_die "fixture state must be a regular non-symbolic-link file"
  STATE_RUN_ID="$(jq -er '.runId' "${TIANQUAN_JIANSHEN_IT_STATE_FILE}")"
  STATE_SCHEMA="$(jq -er '.postgresSchema' "${TIANQUAN_JIANSHEN_IT_STATE_FILE}")"
  STATE_REDIS_PREFIX="$(jq -er '.redisPrefix' "${TIANQUAN_JIANSHEN_IT_STATE_FILE}")"
  rbac3_validate_run_id "${STATE_RUN_ID}"
  rbac3_validate_schema "${STATE_RUN_ID}" "${STATE_SCHEMA}"
  rbac3_validate_redis_prefix "${STATE_RUN_ID}" "${STATE_REDIS_PREFIX}"
  STATE_REDIS_KEYS=()
  while IFS= read -r key; do
    STATE_REDIS_KEYS+=("${key}")
  done < <(jq -er '.redisKeys[]' "${TIANQUAN_JIANSHEN_IT_STATE_FILE}")
  [[ "${#STATE_REDIS_KEYS[@]}" -gt 0 ]] || rbac3_die "state contains no Redis keys"
  local key
  for key in "${STATE_REDIS_KEYS[@]}"; do
    [[ "${key}" == "${STATE_REDIS_PREFIX}"* ]] \
      || rbac3_die "state contains a Redis key outside the fixture prefix"
    [[ "${key}" =~ ^[A-Za-z0-9:_-]+$ ]] \
      || rbac3_die "state contains an unsafe Redis key"
  done
}

check_config() {
  rbac3_require_command psql
  rbac3_require_command redis-cli
  load_state
  rbac3_postgres_args
  rbac3_redis_args TIANQUAN_JIANSHEN_RUNTIME_REDIS
  unset PGPASSWORD REDISCLI_AUTH || true
}

clean() {
  check_config
  [[ "${TIANQUAN_JIANSHEN_FIXTURE_CONFIRM:-}" == 'cleanup' ]] \
    || rbac3_die "set TIANQUAN_JIANSHEN_FIXTURE_CONFIRM=cleanup for this opt-in mutation"

  rbac3_postgres_args
  psql "${TIANQUAN_JIANSHEN_PSQL_ARGS[@]}" --command "DROP SCHEMA IF EXISTS \"${STATE_SCHEMA}\" CASCADE"
  unset PGPASSWORD || true

  rbac3_redis_args TIANQUAN_JIANSHEN_RUNTIME_REDIS
  local key
  for key in "${STATE_REDIS_KEYS[@]}"; do
    redis-cli "${TIANQUAN_JIANSHEN_REDIS_ARGS[@]}" -n "${TIANQUAN_JIANSHEN_REDIS_DATABASE}" del "${key}" >/dev/null
  done
  unset REDISCLI_AUTH || true

  local evidence="${TIANQUAN_JIANSHEN_IT_STATE_FILE}.cleaned.$(date -u '+%Y%m%dT%H%M%SZ')"
  mv "${TIANQUAN_JIANSHEN_IT_STATE_FILE}" "${evidence}"
  rbac3_note "fixture cleanup complete; evidence retained at ${evidence}"
}

case "${1:---help}" in
  --help) usage ;;
  --check-config) check_config; rbac3_note "cleanup configuration and state are valid" ;;
  --clean) clean ;;
  *) usage >&2; exit 2 ;;
esac
