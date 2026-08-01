#!/usr/bin/env bash

set -euo pipefail
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

usage() {
  cat <<'EOF'
Usage: verify-local-dependencies.sh [--help|--check-config|--verify]

Checks explicitly configured host-local PostgreSQL and the three independent
Redis roles used by RBAC3/DDC/Gateway. Existing services are queried only; no
service or application is started.

Required PostgreSQL variables:
  RBAC3_IT_POSTGRES_URL, RBAC3_IT_POSTGRES_USER,
  RBAC3_IT_POSTGRES_PASSWORD_FILE

Required Redis prefixes (each needs _HOST, _PORT, _DATABASE; _PASSWORD_FILE is
optional): DDC_REGISTRY_REDIS, GATEWAY_RATE_LIMIT_REDIS, RBAC3_RUNTIME_REDIS.
EOF
}

check_config() {
  rbac3_require_command psql
  rbac3_require_command redis-cli
  rbac3_postgres_args
  local prefix
  for prefix in DDC_REGISTRY_REDIS GATEWAY_RATE_LIMIT_REDIS RBAC3_RUNTIME_REDIS; do
    rbac3_redis_args "${prefix}"
  done
  unset PGPASSWORD REDISCLI_AUTH || true
}

verify() {
  check_config
  rbac3_postgres_args
  [[ "$(psql "${RBAC3_PSQL_ARGS[@]}" --tuples-only --no-align --command 'SELECT 1')" == '1' ]] \
    || rbac3_die "PostgreSQL readiness query failed"
  unset PGPASSWORD || true

  local prefix
  for prefix in DDC_REGISTRY_REDIS GATEWAY_RATE_LIMIT_REDIS RBAC3_RUNTIME_REDIS; do
    rbac3_redis_ping "${prefix}" || rbac3_die "Redis readiness failed for ${prefix}"
    unset REDISCLI_AUTH || true
  done
  rbac3_note "all explicitly configured local dependencies responded"
}

case "${1:---help}" in
  --help) usage ;;
  --check-config) check_config; rbac3_note "local dependency configuration is valid" ;;
  --verify) verify ;;
  *) usage >&2; exit 2 ;;
esac
