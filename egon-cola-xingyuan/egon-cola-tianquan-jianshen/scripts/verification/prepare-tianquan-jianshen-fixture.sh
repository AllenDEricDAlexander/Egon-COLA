#!/usr/bin/env bash

set -euo pipefail
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

usage() {
  cat <<'EOF'
Usage: prepare-tianquan-jianshen-fixture.sh [--help|--check-config|--prepare]

Creates only a dedicated PostgreSQL schema and one exact Redis marker key, then
records them in TIANQUAN_JIANSHEN_IT_STATE_FILE. It never creates or starts infrastructure.

Required: TIANQUAN_JIANSHEN_IT_RUN_ID, TIANQUAN_JIANSHEN_IT_SCHEMA, TIANQUAN_JIANSHEN_IT_REDIS_PREFIX,
TIANQUAN_JIANSHEN_IT_TENANT_ID, TIANQUAN_JIANSHEN_IT_STATE_FILE, TIANQUAN_JIANSHEN_IT_POSTGRES_*,
TIANQUAN_JIANSHEN_RUNTIME_REDIS_*. --prepare additionally requires
TIANQUAN_JIANSHEN_FIXTURE_CONFIRM=prepare.
EOF
}

check_config() {
  rbac3_require_command jq
  rbac3_require_command psql
  rbac3_require_command redis-cli
  rbac3_require_env TIANQUAN_JIANSHEN_IT_RUN_ID
  rbac3_require_env TIANQUAN_JIANSHEN_IT_SCHEMA
  rbac3_require_env TIANQUAN_JIANSHEN_IT_REDIS_PREFIX
  rbac3_require_env TIANQUAN_JIANSHEN_IT_TENANT_ID
  rbac3_require_env TIANQUAN_JIANSHEN_IT_STATE_FILE
  rbac3_validate_run_id "${TIANQUAN_JIANSHEN_IT_RUN_ID}"
  rbac3_validate_schema "${TIANQUAN_JIANSHEN_IT_RUN_ID}" "${TIANQUAN_JIANSHEN_IT_SCHEMA}"
  rbac3_validate_redis_prefix "${TIANQUAN_JIANSHEN_IT_RUN_ID}" "${TIANQUAN_JIANSHEN_IT_REDIS_PREFIX}"
  [[ -n "${TIANQUAN_JIANSHEN_IT_TENANT_ID}" ]] || rbac3_die "dedicated tenant ID is required"
  [[ ! -e "${TIANQUAN_JIANSHEN_IT_STATE_FILE}" ]] || rbac3_die "fixture state file already exists"
  [[ ! -L "${TIANQUAN_JIANSHEN_IT_STATE_FILE}" ]] || rbac3_die "fixture state path must not be a symbolic link"
  rbac3_postgres_args
  rbac3_redis_args TIANQUAN_JIANSHEN_RUNTIME_REDIS
  unset PGPASSWORD REDISCLI_AUTH || true
}

prepare() {
  check_config
  [[ "${TIANQUAN_JIANSHEN_FIXTURE_CONFIRM:-}" == 'prepare' ]] \
    || rbac3_die "set TIANQUAN_JIANSHEN_FIXTURE_CONFIRM=prepare for this opt-in mutation"
  local marker_key="${TIANQUAN_JIANSHEN_IT_REDIS_PREFIX}fixture:state"
  local state_parent temporary
  state_parent="$(dirname "${TIANQUAN_JIANSHEN_IT_STATE_FILE}")"
  mkdir -p "${state_parent}"
  temporary="${TIANQUAN_JIANSHEN_IT_STATE_FILE}.tmp"
  [[ ! -e "${temporary}" ]] || rbac3_die "temporary state path already exists"

  jq -n \
    --arg runId "${TIANQUAN_JIANSHEN_IT_RUN_ID}" \
    --arg schema "${TIANQUAN_JIANSHEN_IT_SCHEMA}" \
    --arg redisPrefix "${TIANQUAN_JIANSHEN_IT_REDIS_PREFIX}" \
    --arg redisKey "${marker_key}" \
    --arg tenantId "${TIANQUAN_JIANSHEN_IT_TENANT_ID}" \
    --arg preparedAt "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" \
    '{schemaVersion:1, status:"PREPARING", runId:$runId,
      postgresSchema:$schema, redisPrefix:$redisPrefix,
      redisKeys:[$redisKey], tenantId:$tenantId, preparedAt:$preparedAt}' \
    > "${temporary}"
  chmod 600 "${temporary}"
  mv "${temporary}" "${TIANQUAN_JIANSHEN_IT_STATE_FILE}"

  rbac3_postgres_args
  psql "${TIANQUAN_JIANSHEN_PSQL_ARGS[@]}" --command "CREATE SCHEMA \"${TIANQUAN_JIANSHEN_IT_SCHEMA}\""
  unset PGPASSWORD || true

  rbac3_redis_args TIANQUAN_JIANSHEN_RUNTIME_REDIS
  [[ "$(redis-cli "${TIANQUAN_JIANSHEN_REDIS_ARGS[@]}" -n "${TIANQUAN_JIANSHEN_REDIS_DATABASE}" \
    set "${marker_key}" "${TIANQUAN_JIANSHEN_IT_TENANT_ID}" NX)" == 'OK' ]] \
    || rbac3_die "fixture marker key already exists"
  unset REDISCLI_AUTH || true

  jq '.status = "PREPARED"' "${TIANQUAN_JIANSHEN_IT_STATE_FILE}" > "${temporary}"
  chmod 600 "${temporary}"
  mv "${temporary}" "${TIANQUAN_JIANSHEN_IT_STATE_FILE}"
  rbac3_note "fixture prepared; state recorded at ${TIANQUAN_JIANSHEN_IT_STATE_FILE}"
}

case "${1:---help}" in
  --help) usage ;;
  --check-config) check_config; rbac3_note "fixture configuration is valid" ;;
  --prepare) prepare ;;
  *) usage >&2; exit 2 ;;
esac
