#!/usr/bin/env bash

set -euo pipefail
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

usage() {
  cat <<'EOF'
Usage: verify-static.sh [--help|--check-config|--verify]

Runs read-only Tianquan-Jianshen repository structure and security-boundary checks.
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
  [[ -f "${TIANQUAN_JIANSHEN_MODULE_ROOT}/pom.xml" ]] || rbac3_die "Tianquan-Jianshen module root is invalid"
  [[ -f "${TIANQUAN_JIANSHEN_REPOSITORY_ROOT}/mvnw" ]] || rbac3_die "repository root is invalid"
}

verify() {
  check_config
  local migrations
  local admin_root="${TIANQUAN_JIANSHEN_MODULE_ROOT}/egon-cola-tianquan-jianshen-admin"
  local production_yaml="${admin_root}/src/main/resources/application.yml"
  local declarations="${admin_root}/src/main/java/top/egon/cola/platform/tianquan-jianshen/admin/config/tianshu/Rbac3DdcValueDeclarations.java"
  local metrics="${admin_root}/src/main/java/top/egon/cola/platform/tianquan-jianshen/admin/config/tianshu/Rbac3IntegrationMetrics.java"
  local integration_config="${admin_root}/src/main/java/top/egon/cola/platform/tianquan-jianshen/admin/config/runtime/Rbac3PlatformIntegrationConfiguration.java"
  local catalog_contract="${admin_root}/src/test/resources/contracts/tianquan-jianshen-yuheng-catalog-semantic-baseline.json"
  migrations="$(find "${TIANQUAN_JIANSHEN_MODULE_ROOT}" -path '*/src/main/resources/db/migration/V*__*.sql' -type f | wc -l | tr -d ' ')"
  [[ "${migrations}" == '5' ]] || rbac3_die "expected the Tianquan-Jianshen V1 through V5 migration chain, found ${migrations}"
  [[ -f "${TIANQUAN_JIANSHEN_MODULE_ROOT}/egon-cola-tianquan-jianshen-admin/src/main/resources/db/migration/V1__create_rbac3_schema.sql" ]]
  [[ -f "${TIANQUAN_JIANSHEN_MODULE_ROOT}/egon-cola-tianquan-jianshen-admin/src/main/resources/db/migration/V2__add_session_strong_authentication_time.sql" ]]
  [[ -f "${TIANQUAN_JIANSHEN_MODULE_ROOT}/egon-cola-tianquan-jianshen-admin/src/main/resources/db/migration/V5__remove_sessions_and_minimize_authorization_user.sql" ]]
  [[ ! -e "${TIANQUAN_JIANSHEN_MODULE_ROOT}/egon-cola-tianquan-jianshen-test" ]] \
    || rbac3_die "an independent Tianquan-Jianshen test module is forbidden"

  rg -q 'flyway_schema_history_rbac3' "${TIANQUAN_JIANSHEN_MODULE_ROOT}/egon-cola-tianquan-jianshen-admin/src/main"
  rg -q 'flyway_schema_history_outbox' "${TIANQUAN_JIANSHEN_MODULE_ROOT}/egon-cola-tianquan-jianshen-admin/src/main"
  rg -q 'ddcRedissonClient' "${TIANQUAN_JIANSHEN_MODULE_ROOT}"
  rg -q 'gatewayRateLimitRedissonClient' "${TIANQUAN_JIANSHEN_MODULE_ROOT}"
  rg -q 'rbac3RuntimeRedissonClient' "${TIANQUAN_JIANSHEN_MODULE_ROOT}"

  rg -Uq 'tianshu:\n[[:space:]]+enabled: true' "${production_yaml}" \
    || rbac3_die "production application.yml must enable the Tianshu config client"
  rg -Uq 'registry:\n[[:space:]]+enabled: true' "${production_yaml}" \
    || rbac3_die "production application.yml must enable Tianshu service registration"
  rg -q 'ddcHttpRegistrationServerReadyListener' "${integration_config}" \
    || rbac3_die "Tianquan-Jianshen must own the Tianshu-gated HTTP provider ready listener"

  local config_key
  for config_key in tianquan-jianshen.maximum-active-roots; do
    local declaration_count
    declaration_count="$(rg -F -o "value = \"\${${config_key}:" "${declarations}" | wc -l | tr -d ' ')"
    [[ "${declaration_count}" == '1' ]] \
      || rbac3_die "${config_key} must have exactly one Tianshu declaration"
  done
  [[ "$(rg -o 'refreshable = false' "${declarations}" | wc -l | tr -d ' ')" == '1' ]] \
    || rbac3_die "the Tianquan-Jianshen Tianshu declaration must disable reflective refresh"

  local metric_name
  for metric_name in \
      tianquan_jianshen_tianshu_config_apply_total \
      tianquan_jianshen_tianshu_config_snapshot_version \
      tianquan_jianshen_tianshu_config_ready \
      rbac3_gateway_definition_operation_count; do
    rg -q "${metric_name}" "${metrics}" \
      || rbac3_die "required bounded metric is missing: ${metric_name}"
  done
  rg -Fq 'APPLY_STATUSES = Set.of("success", "failed")' "${metrics}" \
    || rbac3_die "Tianshu apply metric status labels must use the fixed whitelist"
  rg -Fq 'AtomicRbac3RuntimePolicy.CONFIG_KEYS.contains(key)' "${metrics}" \
    || rbac3_die "Tianshu apply metric keys must use the fixed whitelist"
  [[ -f "${catalog_contract}" ]] \
    || rbac3_die "Yuheng document catalog contract baseline is missing"

  if rg -ni 'localhost|127\.0\.0\.1' "${production_yaml}"; then
    rbac3_die "production application.yml must not contain a local endpoint fallback"
  fi
  if rg -ni '(password|secret|access-key|token-file|private-key-file):[[:space:]]+\$\{[^}]+:[^}]+\}' \
      "${production_yaml}"; then
    rbac3_die "production secrets must not have default values"
  fi
  if rg -ni 'swagger|springdoc' --glob 'pom.xml' "${TIANQUAN_JIANSHEN_MODULE_ROOT}"; then
    rbac3_die "Yuheng Interface Catalog is the only API document center"
  fi

  if rg -ni 'approval(Status|Request|Policy)|requiredApprovals|approver(User|Role)Id|roleRotation|rotationId|shiftSchedule|轮岗流程|排班流程|审批流程' \
      --glob 'src/main/**' "${TIANQUAN_JIANSHEN_MODULE_ROOT}"; then
    rbac3_die "forbidden workflow semantics were found in production code"
  fi
  # Admin hosts the control-plane PEP for its own management endpoints, so it
  # intentionally consumes the Tianquan-Jianshen Starter. The runtime libraries still
  # must not depend back on the Admin application below.
  if rg -n 'egon-cola-tianquan-jianshen-admin' \
      "${TIANQUAN_JIANSHEN_MODULE_ROOT}/egon-cola-tianquan-jianshen-starter/pom.xml" \
      "${TIANQUAN_JIANSHEN_MODULE_ROOT}/egon-cola-tianquan-jianshen-gateway-adapter/pom.xml"; then
    rbac3_die "runtime libraries must not depend on Admin"
  fi

  rg -q 'Configuration scope' "${TIANQUAN_JIANSHEN_MODULE_ROOT}/README.md" \
    || rbac3_die "README.md must distinguish Tianshu configuration scope from service scope"
  rg -q '配置 scope' "${TIANQUAN_JIANSHEN_MODULE_ROOT}/README.zh-CN.md" \
    || rbac3_die "README.zh-CN.md must distinguish Tianshu configuration scope from service scope"
  rg -Uq 'CONFIG_CLIENT.*HTTP_PROVIDER|HTTP_PROVIDER.*CONFIG_CLIENT' \
    "${TIANQUAN_JIANSHEN_MODULE_ROOT}/docs/architecture.md" \
    || rbac3_die "architecture.md must document the two independent Tianshu leases"
  rg -q 'tianquan-jianshen.maximum-active-roots' \
    "${TIANQUAN_JIANSHEN_MODULE_ROOT}/docs/operations-runbook.md" \
    || rbac3_die "operations-runbook.md must document the Tianshu policy key"
  if rg -n 'TIANQUAN_JIANSHEN_JWT_|access-token-ttl-seconds|refresh-token-ttl-seconds|session-idle-timeout-seconds|session-absolute-timeout-seconds' \
      "${admin_root}/src/main/java" "${admin_root}/src/main/resources/application.yml" \
      "${admin_root}/src/main/resources/application-local.yml"; then
    rbac3_die "Tianquan-Jianshen runtime must not retain personnel JWT or Session timeout configuration"
  fi
  rg -q 'Tianshu Config Client' \
    "${TIANQUAN_JIANSHEN_MODULE_ROOT}/docs/verification-evidence-template.md" \
    || rbac3_die "verification template must record the Tianshu Config Client fact"
  rbac3_note "static verification passed"
}

case "${1:---help}" in
  --help) usage ;;
  --check-config) check_config; rbac3_note "static configuration is valid" ;;
  --verify) verify ;;
  *) usage >&2; exit 2 ;;
esac
