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

startup_mode=full
if [[ -s "${unified_platform_runtime_dir}/startup-mode" ]]; then
  startup_mode="$(<"${unified_platform_runtime_dir}/startup-mode")"
fi
advertised_host="${UNIFIED_PLATFORM_ADVERTISED_HOST:-}"
if [[ -z "${advertised_host}" && -s "${unified_platform_env_dir}/gateway-engine.env" ]]; then
  advertised_host="$(awk -F= '$1 == "GATEWAY_ENGINE_DDC_ADVERTISED_HOST" { print $2; exit }' \
    "${unified_platform_env_dir}/gateway-engine.env")"
fi
advertised_host="${advertised_host:-127.0.0.1}"
printf 'Startup mode: %s\n' "${startup_mode}"
printf 'Portal URL: %s\n' "${PLATFORM_PORTAL_URL}"
printf 'Gateway public origin: %s\n' "${GATEWAY_BASE_URL}"
printf 'Advertised host: %s\n' "${advertised_host}"

print_status idp "${IDP_BASE_URL}/actuator/health/readiness"
print_status rbac3 "${RBAC3_BASE_URL}/actuator/health/readiness"
print_status ddc "${DDC_BASE_URL}/actuator/health/readiness"
print_status gateway-admin "${GATEWAY_ADMIN_BASE_URL}/actuator/health/readiness"
print_status gateway-engine "${GATEWAY_ENGINE_A_BASE_URL}/actuator/health/readiness"
print_status gateway-mcp-engine "${GATEWAY_MCP_ENGINE_BASE_URL}/actuator/health/readiness"
if [[ "${startup_mode}" == "full" ]]; then
  print_status gateway-engine-b "${GATEWAY_ENGINE_B_BASE_URL}/actuator/health/readiness"
  print_status mock-backend "${MOCK_BACKEND_BASE_URL}/actuator/health/readiness"
  print_status mcp-provider "${MCP_PROVIDER_BASE_URL}/actuator/health/readiness"
  print_status mcp-remote "${MCP_REMOTE_BASE_URL}/actuator/health/readiness"
fi
print_status idp-admin-web "${IDP_ADMIN_WEB_URL}/"
print_status rbac3-admin-web "${RBAC3_ADMIN_WEB_URL}/"
print_status gateway-admin-web "${GATEWAY_ADMIN_WEB_URL}/"
print_status ddc-admin-web "${DDC_ADMIN_WEB_URL}/"
print_status portal-web "${PLATFORM_PORTAL_URL}/"
login_status="$(unified_platform_http_code "${GATEWAY_BASE_URL}/oauth2/login/csrf")"
printf 'Gateway login route: %s/oauth2/login/csrf health=%s\n' \
  "${GATEWAY_BASE_URL}" "${login_status:-unreachable}"

printf 'Runtime: %s\n' "${unified_platform_runtime_dir}"
printf 'Logs:    %s\n' "${unified_platform_log_dir}"
