#!/usr/bin/env bash

set -euo pipefail
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

usage() {
  cat <<'EOF'
Usage: prepare-rbac3-fixture.sh [--help|--check-config|--prepare]

Creates only a dedicated PostgreSQL schema and one exact Redis marker key, then
records them in RBAC3_IT_STATE_FILE. It never creates or starts infrastructure.

Required: RBAC3_IT_RUN_ID, RBAC3_IT_SCHEMA, RBAC3_IT_REDIS_PREFIX,
RBAC3_IT_TENANT_ID, RBAC3_IT_STATE_FILE, RBAC3_IT_POSTGRES_*,
RBAC3_RUNTIME_REDIS_*. --prepare additionally requires
RBAC3_FIXTURE_CONFIRM=prepare.
EOF
}

check_config() {
  rbac3_require_command jq
  rbac3_require_command psql
  rbac3_require_command redis-cli
  rbac3_require_env RBAC3_IT_RUN_ID
  rbac3_require_env RBAC3_IT_SCHEMA
  rbac3_require_env RBAC3_IT_REDIS_PREFIX
  rbac3_require_env RBAC3_IT_TENANT_ID
  rbac3_require_env RBAC3_IT_STATE_FILE
  rbac3_validate_run_id "${RBAC3_IT_RUN_ID}"
  rbac3_validate_schema "${RBAC3_IT_RUN_ID}" "${RBAC3_IT_SCHEMA}"
  rbac3_validate_redis_prefix "${RBAC3_IT_RUN_ID}" "${RBAC3_IT_REDIS_PREFIX}"
  [[ -n "${RBAC3_IT_TENANT_ID}" ]] || rbac3_die "dedicated tenant ID is required"
  [[ ! -e "${RBAC3_IT_STATE_FILE}" ]] || rbac3_die "fixture state file already exists"
  [[ ! -L "${RBAC3_IT_STATE_FILE}" ]] || rbac3_die "fixture state path must not be a symbolic link"
  rbac3_postgres_args
  rbac3_redis_args RBAC3_RUNTIME_REDIS
  unset PGPASSWORD REDISCLI_AUTH || true
}

prepare() {
  check_config
  [[ "${RBAC3_FIXTURE_CONFIRM:-}" == 'prepare' ]] \
    || rbac3_die "set RBAC3_FIXTURE_CONFIRM=prepare for this opt-in mutation"
  local marker_key="${RBAC3_IT_REDIS_PREFIX}fixture:state"
  local state_parent temporary
  state_parent="$(dirname "${RBAC3_IT_STATE_FILE}")"
  mkdir -p "${state_parent}"
  temporary="${RBAC3_IT_STATE_FILE}.tmp"
  [[ ! -e "${temporary}" ]] || rbac3_die "temporary state path already exists"

  jq -n \
    --arg runId "${RBAC3_IT_RUN_ID}" \
    --arg schema "${RBAC3_IT_SCHEMA}" \
    --arg redisPrefix "${RBAC3_IT_REDIS_PREFIX}" \
    --arg redisKey "${marker_key}" \
    --arg tenantId "${RBAC3_IT_TENANT_ID}" \
    --arg preparedAt "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" \
    '{schemaVersion:1, status:"PREPARING", runId:$runId,
      postgresSchema:$schema, redisPrefix:$redisPrefix,
      redisKeys:[$redisKey], tenantId:$tenantId, preparedAt:$preparedAt}' \
    > "${temporary}"
  chmod 600 "${temporary}"
  mv "${temporary}" "${RBAC3_IT_STATE_FILE}"

  rbac3_postgres_args
  psql "${RBAC3_PSQL_ARGS[@]}" --command "CREATE SCHEMA \"${RBAC3_IT_SCHEMA}\""
  unset PGPASSWORD || true

  rbac3_redis_args RBAC3_RUNTIME_REDIS
  [[ "$(redis-cli "${RBAC3_REDIS_ARGS[@]}" -n "${RBAC3_REDIS_DATABASE}" \
    set "${marker_key}" "${RBAC3_IT_TENANT_ID}" NX)" == 'OK' ]] \
    || rbac3_die "fixture marker key already exists"
  unset REDISCLI_AUTH || true

  jq '.status = "PREPARED"' "${RBAC3_IT_STATE_FILE}" > "${temporary}"
  chmod 600 "${temporary}"
  mv "${temporary}" "${RBAC3_IT_STATE_FILE}"
  rbac3_note "fixture prepared; state recorded at ${RBAC3_IT_STATE_FILE}"
}

case "${1:---help}" in
  --help) usage ;;
  --check-config) check_config; rbac3_note "fixture configuration is valid" ;;
  --prepare) prepare ;;
  *) usage >&2; exit 2 ;;
esac
