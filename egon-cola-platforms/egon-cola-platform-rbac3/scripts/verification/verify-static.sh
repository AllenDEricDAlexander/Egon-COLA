#!/usr/bin/env bash

set -euo pipefail
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

usage() {
  cat <<'EOF'
Usage: verify-static.sh [--help|--check-config|--verify]

Runs read-only RBAC3 repository structure and security-boundary checks.
It does not access the network, databases, Redis, or start any process.

  --help          Show this help.
  --check-config  Confirm required local tools and repository paths.
  --verify        Execute all static checks.
EOF
}

check_config() {
  rbac3_require_command bash
  rbac3_require_command find
  rbac3_require_command rg
  [[ -f "${RBAC3_MODULE_ROOT}/pom.xml" ]] || rbac3_die "RBAC3 module root is invalid"
  [[ -f "${RBAC3_REPOSITORY_ROOT}/mvnw" ]] || rbac3_die "repository root is invalid"
}

verify() {
  check_config
  local migrations
  migrations="$(find "${RBAC3_MODULE_ROOT}" -path '*/src/main/resources/db/migration/V*__*.sql' -type f | wc -l | tr -d ' ')"
  [[ "${migrations}" == '2' ]] || rbac3_die "expected the immutable V1 and additive V2 RBAC3 migrations, found ${migrations}"
  [[ -f "${RBAC3_MODULE_ROOT}/egon-cola-platform-rbac3-admin/src/main/resources/db/migration/V1__create_rbac3_schema.sql" ]]
  [[ -f "${RBAC3_MODULE_ROOT}/egon-cola-platform-rbac3-admin/src/main/resources/db/migration/V2__add_session_strong_authentication_time.sql" ]]
  [[ ! -e "${RBAC3_MODULE_ROOT}/egon-cola-platform-rbac3-test" ]] \
    || rbac3_die "an independent RBAC3 test module is forbidden"

  rg -q 'flyway_schema_history_rbac3' "${RBAC3_MODULE_ROOT}/egon-cola-platform-rbac3-admin/src/main"
  rg -q 'flyway_schema_history_outbox' "${RBAC3_MODULE_ROOT}/egon-cola-platform-rbac3-admin/src/main"
  rg -q 'ddcRegistryRedissonClient' "${RBAC3_MODULE_ROOT}"
  rg -q 'gatewayRateLimitRedissonClient' "${RBAC3_MODULE_ROOT}"
  rg -q 'rbac3RuntimeRedissonClient' "${RBAC3_MODULE_ROOT}"

  if rg -ni 'approval(Status|Request|Policy)|requiredApprovals|approver(User|Role)Id|roleRotation|rotationId|shiftSchedule|轮岗流程|排班流程|审批流程' \
      --glob 'src/main/**' "${RBAC3_MODULE_ROOT}"; then
    rbac3_die "forbidden workflow semantics were found in production code"
  fi
  if rg -n 'egon-cola-platform-rbac3-starter' \
      "${RBAC3_MODULE_ROOT}/egon-cola-platform-rbac3-admin/pom.xml"; then
    rbac3_die "Admin must not depend on Starter"
  fi
  if rg -n 'egon-cola-platform-rbac3-admin' \
      "${RBAC3_MODULE_ROOT}/egon-cola-platform-rbac3-starter/pom.xml" \
      "${RBAC3_MODULE_ROOT}/egon-cola-platform-rbac3-gateway-adapter/pom.xml"; then
    rbac3_die "runtime libraries must not depend on Admin"
  fi
  rbac3_note "static verification passed"
}

case "${1:---help}" in
  --help) usage ;;
  --check-config) check_config; rbac3_note "static configuration is valid" ;;
  --verify) verify ;;
  *) usage >&2; exit 2 ;;
esac
