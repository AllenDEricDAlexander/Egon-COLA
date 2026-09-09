#!/usr/bin/env bash

set -euo pipefail

TIANQUAN_JIANSHEN_VERIFICATION_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TIANQUAN_JIANSHEN_MODULE_ROOT="$(cd "${TIANQUAN_JIANSHEN_VERIFICATION_DIR}/../.." && pwd)"
TIANQUAN_JIANSHEN_REPOSITORY_ROOT="$(cd "${TIANQUAN_JIANSHEN_MODULE_ROOT}/../.." && pwd)"

rbac3_note() {
  printf '[tianquan-jianshen-verification] %s\n' "$*"
}

rbac3_die() {
  printf '[tianquan-jianshen-verification] ERROR: %s\n' "$*" >&2
  exit 1
}

rbac3_require_command() {
  command -v "$1" >/dev/null 2>&1 || rbac3_die "required command is unavailable: $1"
}

rbac3_require_env() {
  local name="$1"
  [[ -n "${!name:-}" ]] || rbac3_die "required environment variable is missing: ${name}"
}

rbac3_require_secret_file() {
  local name="$1"
  rbac3_require_env "${name}"
  local path="${!name}"
  [[ -f "${path}" && ! -L "${path}" && -r "${path}" ]] \
    || rbac3_die "${name} must point to a readable regular file"
  [[ -s "${path}" ]] || rbac3_die "${name} must not be empty"
}

rbac3_read_secret() {
  local path="$1"
  local value
  value="$(tr -d '\r\n' < "${path}")"
  [[ -n "${value}" ]] || rbac3_die "secret file is empty"
  printf '%s' "${value}"
}

rbac3_validate_run_id() {
  local run_id="$1"
  [[ "${run_id}" =~ ^[a-z0-9]+$ ]] \
    || rbac3_die "runId must match [a-z0-9]+"
}

rbac3_validate_schema() {
  local run_id="$1"
  local schema="$2"
  [[ "${schema}" =~ ^rbac3_it_[a-z0-9]+$ ]] \
    || rbac3_die "fixture schema is outside the rbac3_it_ namespace"
  [[ "${schema}" == "rbac3_it_${run_id}" ]] \
    || rbac3_die "fixture schema must equal rbac3_it_<runId>"
}

rbac3_validate_redis_prefix() {
  local run_id="$1"
  local prefix="$2"
  [[ "${prefix}" == "tianquan-jianshen:it:${run_id}:" ]] \
    || rbac3_die "fixture Redis prefix must equal tianquan-jianshen:it:<runId>:"
}

rbac3_validate_http_url() {
  local name="$1"
  local value="${!name:-}"
  [[ "${value}" =~ ^https?://[^[:space:]]+$ ]] \
    || rbac3_die "${name} must be an explicit HTTP(S) URL"
}

rbac3_validate_uint() {
  local name="$1"
  local value="${!name:-}"
  [[ "${value}" =~ ^[0-9]+$ ]] || rbac3_die "${name} must be an unsigned integer"
}

rbac3_bearer_get() {
  local token_file="$1"
  local url="$2"
  local token
  token="$(rbac3_read_secret "${token_file}")"
  curl --fail-with-body --silent --show-error \
    --connect-timeout 3 --max-time 10 \
    --header "Authorization: Bearer ${token}" \
    --header 'Accept: application/json' \
    "${url}"
}

rbac3_redis_args() {
  local prefix="$1"
  local host_name="${prefix}_HOST"
  local port_name="${prefix}_PORT"
  local database_name="${prefix}_DATABASE"
  rbac3_require_env "${host_name}"
  rbac3_require_env "${port_name}"
  rbac3_require_env "${database_name}"
  rbac3_validate_uint "${port_name}"
  rbac3_validate_uint "${database_name}"
  TIANQUAN_JIANSHEN_REDIS_ARGS=(
    --host "${!host_name}"
    --port "${!port_name}"
    --raw
    --no-auth-warning
  )
  TIANQUAN_JIANSHEN_REDIS_DATABASE="${!database_name}"
  local password_name="${prefix}_PASSWORD_FILE"
  if [[ -n "${!password_name:-}" ]]; then
    rbac3_require_secret_file "${password_name}"
    REDISCLI_AUTH="$(rbac3_read_secret "${!password_name}")"
    export REDISCLI_AUTH
  else
    unset REDISCLI_AUTH || true
  fi
}

rbac3_redis_ping() {
  local prefix="$1"
  rbac3_redis_args "${prefix}"
  redis-cli "${TIANQUAN_JIANSHEN_REDIS_ARGS[@]}" -n "${TIANQUAN_JIANSHEN_REDIS_DATABASE}" ping \
    | grep -Fxq 'PONG'
}

rbac3_postgres_args() {
  rbac3_require_env TIANQUAN_JIANSHEN_IT_POSTGRES_URL
  rbac3_require_env TIANQUAN_JIANSHEN_IT_POSTGRES_USER
  rbac3_require_secret_file TIANQUAN_JIANSHEN_IT_POSTGRES_PASSWORD_FILE
  PGPASSWORD="$(rbac3_read_secret "${TIANQUAN_JIANSHEN_IT_POSTGRES_PASSWORD_FILE}")"
  export PGPASSWORD
  TIANQUAN_JIANSHEN_PSQL_ARGS=(
    "${TIANQUAN_JIANSHEN_IT_POSTGRES_URL}"
    --username "${TIANQUAN_JIANSHEN_IT_POSTGRES_USER}"
    --no-password
    --set ON_ERROR_STOP=1
  )
}

rbac3_pause() {
  local message="$1"
  [[ -t 0 ]] || rbac3_die "topology failover checkpoints require an interactive terminal"
  printf '\n%s\n' "${message}"
  read -r -p 'Press Enter only after the external action is complete: ' _
}
