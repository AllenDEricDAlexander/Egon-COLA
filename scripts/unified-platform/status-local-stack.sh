#!/usr/bin/env bash
set -uo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${script_dir}/lib/common.sh"

print_status() {
  local name="$1" url="$2" pid state status
  if unified_platform_process_running "${name}"; then
    pid="$(unified_platform_pid "${name}")"
    state=running
  else
    pid=-
    state=stopped
  fi
  status="$(unified_platform_http_code "${url}")"
  printf '%-20s pid=%-8s process=%-7s health=%s\n' \
    "${name}" "${pid}" "${state}" "${status:-unreachable}"
}

print_status idp "${IDP_BASE_URL}/actuator/health/readiness"
print_status rbac3 "${RBAC3_BASE_URL}/actuator/health/readiness"
print_status ddc "${DDC_BASE_URL}/actuator/health/readiness"
print_status gateway-admin "${GATEWAY_ADMIN_BASE_URL}/actuator/health/readiness"
print_status gateway-engine "${GATEWAY_ENGINE_A_BASE_URL}/actuator/health/readiness"
print_status gateway-engine-b "${GATEWAY_ENGINE_B_BASE_URL}/actuator/health/readiness"
print_status mock-backend "${MOCK_BACKEND_BASE_URL}/actuator/health/readiness"
print_status mcp-provider "${MCP_PROVIDER_BASE_URL}/actuator/health/readiness"
print_status mcp-remote "${MCP_REMOTE_BASE_URL}/actuator/health/readiness"
print_status gateway-admin-web "${GATEWAY_ADMIN_WEB_URL}/"

printf 'Runtime: %s\n' "${unified_platform_runtime_dir}"
printf 'Logs:    %s\n' "${unified_platform_log_dir}"
