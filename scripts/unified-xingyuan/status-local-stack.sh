#!/usr/bin/env bash
set -uo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${script_dir}/lib/common.sh"

print_status() {
  local name="$1" url="$2" pid state status
  if unified_xingyuan_process_running "${name}"; then
    pid="$(unified_xingyuan_pid "${name}")"
    state=running
  else
    pid=-
    state=stopped
  fi
  status="$(unified_xingyuan_http_code "${url}")"
  printf '%-20s pid=%-8s process=%-7s health=%s\n' \
    "${name}" "${pid}" "${state}" "${status:-unreachable}"
}

startup_mode=full
if [[ -s "${unified_xingyuan_runtime_dir}/startup-mode" ]]; then
  startup_mode="$(<"${unified_xingyuan_runtime_dir}/startup-mode")"
fi
advertised_host="${UNIFIED_XINGYUAN_ADVERTISED_HOST:-}"
if [[ -z "${advertised_host}" && -s "${unified_xingyuan_env_dir}/yuheng-biz-gateway.env" ]]; then
  advertised_host="$(awk -F= '$1 == "YUHENG_ENGINE_TIANSHU_ADVERTISED_HOST" { print $2; exit }' \
    "${unified_xingyuan_env_dir}/yuheng-biz-gateway.env")"
fi
advertised_host="${advertised_host:-127.0.0.1}"
printf 'Startup mode: %s\n' "${startup_mode}"
printf 'Portal URL: %s\n' "${PLATFORM_PORTAL_URL}"
printf 'Yuheng public origin: %s\n' "${YUHENG_BASE_URL}"
printf 'Advertised host: %s\n' "${advertised_host}"

print_status tianquan-shoubing "${TIANQUAN_SHOUBING_BASE_URL}/actuator/health/readiness"
print_status tianquan-jianshen "${TIANQUAN_JIANSHEN_BASE_URL}/actuator/health/readiness"
print_status tianshu "${TIANSHU_BASE_URL}/actuator/health/readiness"
print_status yuheng-admin "${YUHENG_ADMIN_BASE_URL}/actuator/health/readiness"
print_status yuheng-biz-gateway "${YUHENG_ENGINE_A_BASE_URL}/actuator/health/readiness"
print_status yuheng-mcp-gateway "${YUHENG_MCP_ENGINE_BASE_URL}/actuator/health/readiness"
if [[ "${startup_mode}" == "full" ]]; then
  print_status yuheng-biz-gateway-b "${YUHENG_ENGINE_B_BASE_URL}/actuator/health/readiness"
  print_status mock-backend "${MOCK_BACKEND_BASE_URL}/actuator/health/readiness"
  print_status mcp-provider "${MCP_PROVIDER_BASE_URL}/actuator/health/readiness"
  print_status mcp-remote "${MCP_REMOTE_BASE_URL}/actuator/health/readiness"
fi
print_status tianquan-shoubing-admin-web "${TIANQUAN_SHOUBING_ADMIN_WEB_URL}/"
print_status tianquan-jianshen-admin-web "${TIANQUAN_JIANSHEN_ADMIN_WEB_URL}/"
print_status yuheng-admin-web "${YUHENG_ADMIN_WEB_URL}/"
print_status tianshu-admin-web "${TIANSHU_ADMIN_WEB_URL}/"
print_status portal-web "${PLATFORM_PORTAL_URL}/"
login_status="$(unified_xingyuan_http_code "${YUHENG_BASE_URL}/oauth2/login/csrf")"
printf 'Yuheng login route: %s/oauth2/login/csrf health=%s\n' \
  "${YUHENG_BASE_URL}" "${login_status:-unreachable}"

printf 'Runtime: %s\n' "${unified_xingyuan_runtime_dir}"
printf 'Logs:    %s\n' "${unified_xingyuan_log_dir}"
