#!/usr/bin/env bash

set -euo pipefail
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

usage() {
  cat <<'EOF'
Usage: cleanup-rbac3-fixture.sh [--help|--check-config|--clean]

Removes only the exact schema and Redis keys recorded by the preparation script.
The state file is validated before any mutation and retained with a .cleaned
suffix as evidence. --clean requires RBAC3_FIXTURE_CONFIRM=cleanup.
EOF
}

load_state() {
  rbac3_require_command jq
  rbac3_require_env RBAC3_IT_STATE_FILE
  [[ -f "${RBAC3_IT_STATE_FILE}" && ! -L "${RBAC3_IT_STATE_FILE}" ]] \
    || rbac3_die "fixture state must be a regular non-symbolic-link file"
  STATE_RUN_ID="$(jq -er '.runId' "${RBAC3_IT_STATE_FILE}")"
  STATE_SCHEMA="$(jq -er '.postgresSchema' "${RBAC3_IT_STATE_FILE}")"
  STATE_REDIS_PREFIX="$(jq -er '.redisPrefix' "${RBAC3_IT_STATE_FILE}")"
  rbac3_validate_run_id "${STATE_RUN_ID}"
  rbac3_validate_schema "${STATE_RUN_ID}" "${STATE_SCHEMA}"
  rbac3_validate_redis_prefix "${STATE_RUN_ID}" "${STATE_REDIS_PREFIX}"
  STATE_REDIS_KEYS=()
  while IFS= read -r key; do
    STATE_REDIS_KEYS+=("${key}")
  done < <(jq -er '.redisKeys[]' "${RBAC3_IT_STATE_FILE}")
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
  rbac3_redis_args RBAC3_RUNTIME_REDIS
  unset PGPASSWORD REDISCLI_AUTH || true
}

clean() {
  check_config
  [[ "${RBAC3_FIXTURE_CONFIRM:-}" == 'cleanup' ]] \
    || rbac3_die "set RBAC3_FIXTURE_CONFIRM=cleanup for this opt-in mutation"

  rbac3_postgres_args
  psql "${RBAC3_PSQL_ARGS[@]}" --command "DROP SCHEMA IF EXISTS \"${STATE_SCHEMA}\" CASCADE"
  unset PGPASSWORD || true

  rbac3_redis_args RBAC3_RUNTIME_REDIS
  local key
  for key in "${STATE_REDIS_KEYS[@]}"; do
    redis-cli "${RBAC3_REDIS_ARGS[@]}" -n "${RBAC3_REDIS_DATABASE}" del "${key}" >/dev/null
  done
  unset REDISCLI_AUTH || true

  local evidence="${RBAC3_IT_STATE_FILE}.cleaned.$(date -u '+%Y%m%dT%H%M%SZ')"
  mv "${RBAC3_IT_STATE_FILE}" "${evidence}"
  rbac3_note "fixture cleanup complete; evidence retained at ${evidence}"
}

case "${1:---help}" in
  --help) usage ;;
  --check-config) check_config; rbac3_note "cleanup configuration and state are valid" ;;
  --clean) clean ;;
  *) usage >&2; exit 2 ;;
esac
