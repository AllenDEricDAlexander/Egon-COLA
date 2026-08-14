#!/usr/bin/env bash
set -uo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${script_dir}/lib/common.sh"

legacy_script="${unified_platform_repo_root}/scripts/unified-identity-local.sh"
ddc_jar="${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/target/egon-cola-platform-dynamic-config-center-admin-exec.jar"
mcp_remote_jar="${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-mcp-remote/target/gateway-test-mcp-remote-exec.jar"
gateway_control_plane_service_token_file="${unified_platform_secret_dir}/gateway-admin-control-plane.service.jwt"
verification_token_dir=""
gateway_admin_token_file=""
idp_admin_token_file=""
rbac3_admin_token_file=""
ddc_admin_token_file=""
tenant_token_file=""
mcp_token_file=""
rbac3_token_file=""
gateway_group_file="${unified_platform_runtime_dir}/gateway-group.id"
gateway_application_file="${unified_platform_runtime_dir}/gateway-application.id"

identity_runtime_database() {
  local env_file="$1" key="$2" jdbc_url database
  [[ -s "${env_file}" ]] \
    || unified_platform_fail "missing runtime environment: ${env_file}"
  jdbc_url="$(bash -c '
    set -a
    # shellcheck disable=SC1090
    source "$1"
    printf "%s" "${!2-}"
  ' _ "${env_file}" "${key}")"
  [[ "${jdbc_url}" == jdbc:postgresql://*/* ]] \
    || unified_platform_fail \
      "${key} is not a PostgreSQL JDBC URL in ${env_file}"
  database="${jdbc_url##*/}"
  database="${database%%\?*}"
  [[ "${database}" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] \
    || unified_platform_fail \
      "${key} has an unsafe database name in ${env_file}"
  printf '%s' "${database}"
}

identity_idp_database="$(identity_runtime_database \
  "${unified_platform_env_dir}/idp.env" IDP_POSTGRES_URL)"
identity_rbac3_database="$(identity_runtime_database \
  "${unified_platform_env_dir}/rbac3.env" RBAC3_POSTGRES_URL)"
identity_gateway_database="$(identity_runtime_database \
  "${unified_platform_env_dir}/gateway-admin.env" SPRING_DATASOURCE_URL)"
identity_ddc_database="$(identity_runtime_database \
  "${unified_platform_env_dir}/ddc.env" SPRING_DATASOURCE_URL)"

run_identity() {
  UNIFIED_IDENTITY_RUNTIME_DIR="${unified_platform_runtime_dir}" \
  UNIFIED_IDENTITY_IDP_URL="${IDP_BASE_URL}" \
  UNIFIED_IDENTITY_RBAC3_URL="${RBAC3_BASE_URL}" \
  UNIFIED_IDENTITY_GATEWAY_ADMIN_URL="${GATEWAY_ADMIN_BASE_URL}" \
  UNIFIED_IDENTITY_DDC_URL="${DDC_BASE_URL}" \
  UNIFIED_IDENTITY_MOCK_URL="${MOCK_BACKEND_BASE_URL}" \
  UNIFIED_IDENTITY_GATEWAY_URL="${GATEWAY_BASE_URL}" \
  UNIFIED_IDENTITY_IDP_DATABASE="${identity_idp_database}" \
  UNIFIED_IDENTITY_RBAC3_DATABASE="${identity_rbac3_database}" \
  UNIFIED_IDENTITY_GATEWAY_DATABASE="${identity_gateway_database}" \
  UNIFIED_IDENTITY_DDC_DATABASE="${identity_ddc_database}" \
    "${legacy_script}" "$@"
}

failures=0

verify_process() {
  local name="$1"
  if ! unified_platform_process_running "${name}"; then
    printf 'FAIL missing managed process: %s\n' "${name}" >&2
    failures=$((failures + 1))
  fi
}

verify_http() {
  local name="$1" url="$2" http_code
  http_code="$(unified_platform_http_code "${url}")"
  if [[ "${http_code}" != "200" ]]; then
    printf 'FAIL unhealthy endpoint: %s status=%s url=%s\n' \
      "${name}" "${http_code:-unreachable}" "${url}" >&2
    failures=$((failures + 1))
  fi
}

for name in \
  ddc \
  idp \
  rbac3 \
  gateway-admin \
  mock-backend \
  mcp-provider \
  mcp-remote \
  gateway-engine \
  gateway-engine-b \
  idp-admin-web \
  rbac3-admin-web \
  gateway-admin-web \
  ddc-admin-web; do
  verify_process "${name}"
done

verify_http idp "${IDP_BASE_URL}/actuator/health/readiness"
verify_http rbac3 "${RBAC3_BASE_URL}/actuator/health/readiness"
verify_http ddc "${DDC_BASE_URL}/actuator/health/readiness"
verify_http gateway-admin "${GATEWAY_ADMIN_BASE_URL}/actuator/health/readiness"
verify_http gateway-engine-a "${GATEWAY_ENGINE_A_BASE_URL}/actuator/health/readiness"
verify_http gateway-engine-b "${GATEWAY_ENGINE_B_BASE_URL}/actuator/health/readiness"
verify_http mock-backend "${MOCK_BACKEND_BASE_URL}/actuator/health/readiness"
verify_http mcp-provider "${MCP_PROVIDER_BASE_URL}/actuator/health/readiness"
verify_http mcp-remote "${MCP_REMOTE_BASE_URL}/actuator/health/readiness"
verify_http idp-admin-web "${IDP_ADMIN_WEB_URL}/"
verify_http rbac3-admin-web "${RBAC3_ADMIN_WEB_URL}/"
verify_http gateway-admin-web "${GATEWAY_ADMIN_WEB_URL}/"
verify_http ddc-admin-web "${DDC_ADMIN_WEB_URL}/"

if ((failures > 0)); then
  printf 'Unified platform verification failed with %d problem(s).\n' \
    "${failures}" >&2
  exit 1
fi

tmp_dir="$(mktemp -d "${unified_platform_runtime_dir}/verify.XXXXXX")"
chmod 700 "${tmp_dir}"
verification_token_dir="${tmp_dir}/tokens"
mkdir -p "${verification_token_dir}"
chmod 700 "${verification_token_dir}"
gateway_admin_token_file="${gateway_control_plane_service_token_file}"
idp_admin_token_file="${verification_token_dir}/default.at"
rbac3_admin_token_file="${verification_token_dir}/default.at"
ddc_admin_token_file="${verification_token_dir}/default.at"
tenant_token_file="${verification_token_dir}/tenant-b.at"
mcp_token_file="${verification_token_dir}/mcp-user.at"
rbac3_token_file="${verification_token_dir}/default.at"

"${script_dir}/test-live-frontend-login.sh" \
  || unified_platform_fail "Admin Web login contract verification failed"

set -e
for command in curl jq openssl; do
  unified_platform_require_command "${command}"
done

run_identity sync-local-credentials >"${tmp_dir}/token-sync.log"
UNIFIED_IDENTITY_TENANT=default \
UNIFIED_IDENTITY_ACCESS_TOKEN_FILE="${verification_token_dir}/default.at" \
  run_identity issue-user-token >"${tmp_dir}/default-token.log"
UNIFIED_IDENTITY_TENANT=tenant-b \
UNIFIED_IDENTITY_ACCESS_TOKEN_FILE="${verification_token_dir}/tenant-b.at" \
  run_identity issue-user-token >"${tmp_dir}/tenant-b-token.log"
UNIFIED_IDENTITY_TENANT=tenant-b \
UNIFIED_IDENTITY_ACCESS_TOKEN_FILE="${verification_token_dir}/mcp-user.at" \
  run_identity issue-user-token >"${tmp_dir}/mcp-token.log"
for file in \
  "${gateway_admin_token_file}" \
  "${tenant_token_file}" \
  "${mcp_token_file}" \
  "${rbac3_token_file}" \
  "${gateway_group_file}" \
  "${gateway_application_file}"; do
  [[ -s "${file}" ]] || unified_platform_fail "missing verifier input: ${file}"
done

ddc_interrupted=false
remote_interrupted=false
roles_modified=false

gateway_request() {
  local method="$1" path="$2" body="${3:-}" output="$4"
  local arguments=(--max-time 30 -sS -X "${method}"
    -H "Authorization: Bearer $(<"${gateway_admin_token_file}")"
    -H 'Content-Type: application/json')
  if [[ -n "${body}" ]]; then
    arguments+=(-d "${body}")
  fi
  curl "${arguments[@]}" -o "${output}" -w '%{http_code}' \
    "${GATEWAY_ADMIN_BASE_URL}${path}"
}

role_ids() {
  local mode="$1" candidates="$2"
  if [[ "${mode}" == "all" ]]; then
    jq -c '[.data.applications[].candidates[].rootRoleId] | unique' \
      "${candidates}"
  else
    jq -c '[.data.applications[] as $application
      | $application.candidates[]
      | select($application.applicationCode != "mock-backend"
          or .rootRoleCode == "MOCK_LOCAL_ENTRY")
      | .rootRoleId] | unique' \
      "${candidates}"
  fi
}

put_role_activations() {
  local mode="$1" candidates current roles version request http_code
  candidates="${tmp_dir}/role-candidates.json"
  current="${tmp_dir}/role-current.json"
  curl -fsS -o "${candidates}" \
    -H "Authorization: Bearer $(<"${rbac3_token_file}")" \
    "${RBAC3_BASE_URL}/api/rbac3/v1/auth/role-activation-candidates"
  curl -fsS -o "${current}" \
    -H "Authorization: Bearer $(<"${rbac3_token_file}")" \
    "${RBAC3_BASE_URL}/api/rbac3/v1/auth/role-activations"
  roles="$(role_ids "${mode}" "${candidates}")"
  version="$(jq -er '.data.authVersion' "${current}")"
  request="$(jq -cn --argjson roles "${roles}" --argjson version "${version}" \
    '{roleIds:$roles,expectedAuthVersion:$version}')"
  http_code="$(curl -sS -o "${tmp_dir}/role-update.json" -w '%{http_code}' \
    -X PUT -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $(<"${rbac3_token_file}")" \
    -d "${request}" "${RBAC3_BASE_URL}/api/rbac3/v1/auth/role-activations")"
  [[ "${http_code}" == "200" ]] || unified_platform_fail \
    "RBAC3 role activation update failed with HTTP ${http_code}"
}

recover_interrupted_services() {
  set +e
  if [[ "${roles_modified}" == "true" ]] \
      && unified_platform_process_running rbac3; then
    put_role_activations all >/dev/null 2>&1
    roles_modified=false
  fi
  if [[ "${ddc_interrupted}" == "true" ]] \
      || ! unified_platform_process_running ddc; then
    unified_platform_start_jar ddc \
      "${unified_platform_env_dir}/ddc.env" "${ddc_jar}"
    unified_platform_wait_http ddc \
      "${DDC_BASE_URL}/actuator/health/readiness" 60
    ddc_interrupted=false
  fi
  if [[ "${remote_interrupted}" == "true" ]] \
      || ! unified_platform_process_running mcp-remote; then
    unified_platform_start_jar mcp-remote \
      "${unified_platform_env_dir}/mcp-remote.env" "${mcp_remote_jar}"
    unified_platform_wait_http mcp-remote \
      "${MCP_REMOTE_BASE_URL}/actuator/health/readiness" 60
    remote_interrupted=false
  fi
  rm -rf "${tmp_dir}"
}
trap recover_interrupted_services EXIT

assert_json() {
  local file="$1" expression="$2" message="$3"
  jq -e "${expression}" "${file}" >/dev/null \
    || unified_platform_fail "${message}: $(jq -c '.error // .' "${file}")"
}

response_header() {
  local headers="$1" name="$2"
  awk -v name="${name}" '
    {
      line=$0
      sub(/\r$/, "", line)
      prefix=name ":"
      if (tolower(substr(line, 1, length(prefix))) == tolower(prefix)) {
        value=substr(line, length(prefix) + 1)
        sub(/^[[:space:]]+/, "", value)
      }
    }
    END {print value}
  ' "${headers}"
}

verify_browser_preflight() {
  local label="$1" origin="$2" endpoint="$3" request_headers="$4"
  local headers="${tmp_dir}/cors-${label}.headers" http_code
  local allowed_origin allow_credentials allow_methods
  http_code="$(curl --max-time 10 -sS -D "${headers}" -o /dev/null \
    -w '%{http_code}' -X OPTIONS \
    -H "Origin: ${origin}" \
    -H 'Access-Control-Request-Method: POST' \
    -H "Access-Control-Request-Headers: ${request_headers}" \
    "${GATEWAY_BASE_URL}${endpoint}")"
  [[ "${http_code}" == "200" ]] || unified_platform_fail \
    "${label} browser preflight failed with HTTP ${http_code}"
  allowed_origin="$(response_header \
    "${headers}" Access-Control-Allow-Origin)"
  allow_credentials="$(response_header \
    "${headers}" Access-Control-Allow-Credentials)"
  allow_methods="$(response_header \
    "${headers}" Access-Control-Allow-Methods)"
  [[ "${allowed_origin}" == "${origin}" ]] || unified_platform_fail \
    "${label} browser preflight did not allow ${origin}"
  [[ "${allow_credentials}" == "true" ]] || unified_platform_fail \
    "${label} browser preflight did not allow credentials"
  [[ ",${allow_methods}," == *",POST,"* ]] || unified_platform_fail \
    "${label} browser preflight did not allow POST"
}

verify_unknown_origin_rejected() {
  local headers="${tmp_dir}/cors-unknown.headers" http_code
  http_code="$(curl --max-time 10 -sS -D "${headers}" -o /dev/null \
    -w '%{http_code}' -X OPTIONS \
    -H 'Origin: http://localhost:18152' \
    -H 'Access-Control-Request-Method: POST' \
    -H 'Access-Control-Request-Headers: content-type' \
    "${GATEWAY_BASE_URL}/oauth2/token")"
  [[ "${http_code}" == "403" ]] || unified_platform_fail \
    "unconfigured browser origin was not rejected: HTTP ${http_code}"
  [[ -z "$(response_header "${headers}" Access-Control-Allow-Origin)" ]] \
    || unified_platform_fail \
      "unconfigured browser origin received an allow-origin header"
}

verify_authenticated_json() {
  local label="$1" url="$2" token_file="$3" expression="${4:-.}"
  local response="${tmp_dir}/admin-${label}.json"
  local http_code attempt request_hex request_id
  [[ -s "${token_file}" ]] || unified_platform_fail \
    "missing ${label} Admin token: ${token_file}"

  for ((attempt = 1; attempt <= 20; attempt++)); do
    request_hex="$(openssl rand -hex 16)"
    request_id="${request_hex:0:8}-${request_hex:8:4}-${request_hex:12:4}-"\
"${request_hex:16:4}-${request_hex:20:12}"
    http_code="$(curl --max-time 15 -sS -o "${response}" -w '%{http_code}' \
      -H "Authorization: Bearer $(<"${token_file}")" \
      -H "X-Request-Id: ${request_id}" \
      -H "X-Trace-Id: ${request_id}" "${url}" 2>/dev/null || true)"
    if [[ "${http_code}" == "200" ]]; then
      jq -e "${expression}" "${response}" >/dev/null \
        || unified_platform_fail \
          "${label} Admin feature returned unexpected JSON"
      return
    fi
    [[ "${http_code}" == "000" || "${http_code}" == "401" \
        || "${http_code}" == "403" || "${http_code}" == "502" \
        || "${http_code}" == "503" ]] || break
    sleep 1
  done

  [[ "${http_code}" == "200" ]] || unified_platform_fail \
    "${label} Admin feature failed with HTTP ${http_code} after ${attempt} attempts"
}

mcp_initialize() {
  local endpoint="$1" label="$2" headers response http_code session_id
  headers="${tmp_dir}/${label}.headers"
  response="${tmp_dir}/${label}.json"
  http_code="$(curl -sS -D "${headers}" -o "${response}" -w '%{http_code}' \
    -H "Authorization: Bearer $(<"${mcp_token_file}")" \
    -H 'Accept: application/json, text/event-stream' \
    -H 'Content-Type: application/json' \
    --data '{"jsonrpc":"2.0","id":"initialize","method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{"tasks":{"requests":{"tools":{"call":{}}}}},"clientInfo":{"name":"unified-platform-verifier","version":"1.0.0"}}}' \
    "${endpoint}/mcp/unified-local")"
  [[ "${http_code}" == "200" ]] \
    || unified_platform_fail "Stable MCP initialize failed with HTTP ${http_code}"
  assert_json "${response}" \
    '.result.protocolVersion == "2025-11-25" and .result.server.code == "unified-local"' \
    "Stable MCP initialize response is invalid"
  session_id="$(sed -n 's/^[Mm][Cc][Pp]-[Ss]ession-[Ii]d: *//p' \
    "${headers}" | tr -d '\r')"
  [[ -n "${session_id}" ]] \
    || unified_platform_fail "Stable MCP initialize did not return a session"
  printf '%s' "${session_id}" >"${tmp_dir}/${label}.session"
  printf '%s' "${response}"
}

mcp_call() {
  local endpoint="$1" session_file="$2" label="$3" method="$4" params="$5"
  local response http_code request
  response="${tmp_dir}/${label}.json"
  request="$(jq -cn --arg id "${label}" --arg method "${method}" \
    --argjson params "${params}" \
    '{jsonrpc:"2.0",id:$id,method:$method,params:$params}')"
  http_code="$(curl -sS -o "${response}" -w '%{http_code}' \
    -H "Authorization: Bearer $(<"${mcp_token_file}")" \
    -H "Mcp-Session-Id: $(<"${session_file}")" \
    -H 'Mcp-Protocol-Version: 2025-11-25' \
    -H 'Accept: application/json, text/event-stream' \
    -H 'Content-Type: application/json' -d "${request}" \
    "${endpoint}/mcp/unified-local")"
  [[ "${http_code}" == "200" || "${http_code}" == "202" ]] \
    || unified_platform_fail "MCP ${method} failed with HTTP ${http_code}"
  printf '%s' "${response}"
}

unified_platform_stage "verifying Admin Web browser CORS boundaries"
verify_browser_preflight idp-login "${IDP_ADMIN_WEB_URL}" \
  /oauth2/login 'content-type,x-idp-csrf'
verify_browser_preflight idp-token "${IDP_ADMIN_WEB_URL}" \
  /oauth2/token content-type
verify_browser_preflight rbac3-token "${RBAC3_ADMIN_WEB_URL}" \
  /oauth2/token content-type
verify_browser_preflight gateway-token "${GATEWAY_ADMIN_WEB_URL}" \
  /oauth2/token content-type
verify_browser_preflight ddc-token "${DDC_ADMIN_WEB_URL}" \
  /oauth2/token content-type
verify_unknown_origin_rejected

unified_platform_stage "verifying unified identity JWT cookie and refresh-token semantics"
if [[ "${UNIFIED_PLATFORM_SKIP_IDENTITY_VERIFY:-false}" != "true" ]]; then
  run_identity verify >"${tmp_dir}/identity-verification.log"
fi

unified_platform_stage "verifying authenticated Admin feature matrix"
verify_authenticated_json idp-bootstrap \
  "${IDP_ADMIN_WEB_URL}/api/v1/auth/bootstrap" \
  "${idp_admin_token_file}" 'type == "object"'
verify_authenticated_json idp-users \
  "${IDP_ADMIN_WEB_URL}/api/v1/identity/users" \
  "${idp_admin_token_file}" 'type == "array"'
verify_authenticated_json idp-clients \
  "${IDP_ADMIN_WEB_URL}/api/v1/identity/clients" \
  "${idp_admin_token_file}" 'type == "array"'
verify_authenticated_json idp-signing-keys \
  "${IDP_ADMIN_WEB_URL}/api/v1/identity/signing-keys" \
  "${idp_admin_token_file}" 'type == "array"'
verify_authenticated_json idp-audits \
  "${IDP_ADMIN_WEB_URL}/api/v1/identity/audits?page=0&size=20" \
  "${idp_admin_token_file}" 'type == "object"'

verify_authenticated_json rbac3-bootstrap \
  "${RBAC3_ADMIN_WEB_URL}/api/v1/auth/bootstrap" \
  "${rbac3_admin_token_file}" 'type == "object"'
verify_authenticated_json rbac3-runtime \
  "${RBAC3_ADMIN_WEB_URL}/api/rbac3/v1/runtime/status" \
  "${rbac3_admin_token_file}" '.data != null'
verify_authenticated_json rbac3-mutations \
  "${RBAC3_ADMIN_WEB_URL}/api/rbac3/v1/runtime/mutations?limit=20" \
  "${rbac3_admin_token_file}" '.data != null'
verify_authenticated_json rbac3-tenants \
  "${RBAC3_ADMIN_WEB_URL}/api/rbac3/v1/platform/tenants?page=0&size=20" \
  "${rbac3_admin_token_file}" '.data != null'
verify_authenticated_json rbac3-users \
  "${RBAC3_ADMIN_WEB_URL}/api/rbac3/v1/users?page=0&size=20" \
  "${rbac3_admin_token_file}" '.data != null'
verify_authenticated_json rbac3-org-units \
  "${RBAC3_ADMIN_WEB_URL}/api/rbac3/v1/org-units?page=0&size=20" \
  "${rbac3_admin_token_file}" '.data != null'
verify_authenticated_json rbac3-positions \
  "${RBAC3_ADMIN_WEB_URL}/api/rbac3/v1/positions?page=0&size=20" \
  "${rbac3_admin_token_file}" '.data != null'
verify_authenticated_json rbac3-applications \
  "${RBAC3_ADMIN_WEB_URL}/api/rbac3/v1/applications" \
  "${rbac3_admin_token_file}" '.data != null'
verify_authenticated_json rbac3-roles \
  "${RBAC3_ADMIN_WEB_URL}/api/rbac3/v1/roles" \
  "${rbac3_admin_token_file}" '.data != null'
verify_authenticated_json rbac3-management-policies \
  "${RBAC3_ADMIN_WEB_URL}/api/rbac3/v1/management-policies" \
  "${rbac3_admin_token_file}" '.data != null'
verify_authenticated_json rbac3-sod-sets \
  "${RBAC3_ADMIN_WEB_URL}/api/rbac3/v1/sod-sets" \
  "${rbac3_admin_token_file}" '.data != null'
verify_authenticated_json rbac3-data-rules \
  "${RBAC3_ADMIN_WEB_URL}/api/rbac3/v1/data-rules" \
  "${rbac3_admin_token_file}" '.data != null'
verify_authenticated_json rbac3-field-rules \
  "${RBAC3_ADMIN_WEB_URL}/api/rbac3/v1/field-rules" \
  "${rbac3_admin_token_file}" '.data != null'
verify_authenticated_json rbac3-operation-sod-rules \
  "${RBAC3_ADMIN_WEB_URL}/api/rbac3/v1/operation-sod-rules" \
  "${rbac3_admin_token_file}" '.data != null'
verify_authenticated_json rbac3-authorization-bootstrap \
  "${RBAC3_ADMIN_WEB_URL}/api/v1/auth/bootstrap" \
  "${rbac3_admin_token_file}" 'type == "object"'
verify_authenticated_json rbac3-role-candidates \
  "${RBAC3_ADMIN_WEB_URL}/api/rbac3/v1/auth/role-activation-candidates" \
  "${rbac3_admin_token_file}" '.data != null'
verify_authenticated_json rbac3-role-activations \
  "${RBAC3_ADMIN_WEB_URL}/api/rbac3/v1/auth/role-activations" \
  "${rbac3_admin_token_file}" '.data != null'
rbac3_audit_from="$(jq -nr 'now - 86400 | todateiso8601 | @uri')"
rbac3_audit_to="$(jq -nr 'now | todateiso8601 | @uri')"
verify_authenticated_json rbac3-audit \
  "${RBAC3_ADMIN_WEB_URL}/api/rbac3/v1/audit-logs?from=${rbac3_audit_from}&to=${rbac3_audit_to}&limit=20" \
  "${rbac3_admin_token_file}" '.data != null'

gateway_group_id="$(<"${gateway_group_file}")"
gateway_application_id="$(<"${gateway_application_file}")"
gateway_admin_path="${GATEWAY_ADMIN_WEB_URL}/api/v1/gateway/admin"
gateway_scope='bizCode=identity&appCode=mock-backend&env=local&namespace=default'
verify_authenticated_json gateway-authorization-bootstrap \
  "${GATEWAY_ADMIN_WEB_URL}/api/v1/auth/bootstrap" "${gateway_admin_token_file}"
verify_authenticated_json gateway-scopes \
  "${gateway_admin_path}/scopes" "${gateway_admin_token_file}" \
  'type == "array"'
verify_authenticated_json gateway-dashboard \
  "${gateway_admin_path}/dashboard?${gateway_scope}" \
  "${gateway_admin_token_file}"
verify_authenticated_json gateway-groups \
  "${gateway_admin_path}/gateway-groups?${gateway_scope}" \
  "${gateway_admin_token_file}" 'type == "array"'
verify_authenticated_json gateway-group \
  "${gateway_admin_path}/gateway-groups/${gateway_group_id}" \
  "${gateway_admin_token_file}"
verify_authenticated_json gateway-applications \
  "${gateway_admin_path}/applications?${gateway_scope}" \
  "${gateway_admin_token_file}" 'type == "array"'
verify_authenticated_json gateway-credentials \
  "${gateway_admin_path}/applications/${gateway_application_id}/credentials" \
  "${gateway_admin_token_file}" 'type == "array"'
verify_authenticated_json gateway-catalog \
  "${gateway_admin_path}/applications/${gateway_application_id}/catalog" \
  "${gateway_admin_token_file}"
verify_authenticated_json gateway-draft \
  "${gateway_admin_path}/gateway-groups/${gateway_group_id}/draft" \
  "${gateway_admin_token_file}"
verify_authenticated_json gateway-draft-diff \
  "${gateway_admin_path}/gateway-groups/${gateway_group_id}/draft/diff" \
  "${gateway_admin_token_file}"
verify_authenticated_json gateway-releases \
  "${gateway_admin_path}/gateway-groups/${gateway_group_id}/releases" \
  "${gateway_admin_token_file}" 'type == "array"'
verify_authenticated_json gateway-engine-nodes \
  "${gateway_admin_path}/gateway-groups/${gateway_group_id}/engine-nodes" \
  "${gateway_admin_token_file}" '.value != null'
verify_authenticated_json gateway-runtime-consistency \
  "${gateway_admin_path}/gateway-groups/${gateway_group_id}/runtime-consistency" \
  "${gateway_admin_token_file}"
verify_authenticated_json gateway-provider-services \
  "${gateway_admin_path}/providers/services?bizCode=identity&appCode=gateway-test-mcp-provider&env=local&namespace=default" \
  "${gateway_admin_token_file}" '.value != null'
verify_authenticated_json gateway-provider-instances \
  "${gateway_admin_path}/providers/instances?${gateway_scope}" \
  "${gateway_admin_token_file}" '.value != null'
verify_authenticated_json gateway-traces \
  "${gateway_admin_path}/observability/traces?${gateway_scope}" \
  "${gateway_admin_token_file}"
verify_authenticated_json gateway-audit \
  "${gateway_admin_path}/audit?${gateway_scope}" \
  "${gateway_admin_token_file}"
verify_authenticated_json gateway-mcp-servers \
  "${gateway_admin_path}/mcp/servers?gatewayGroupId=${gateway_group_id}" \
  "${gateway_admin_token_file}" 'type == "array"'
verify_authenticated_json gateway-mcp-providers \
  "${gateway_admin_path}/mcp/remote/providers?gatewayGroupId=${gateway_group_id}" \
  "${gateway_admin_token_file}" 'type == "array"'
verify_authenticated_json gateway-mcp-mounts \
  "${gateway_admin_path}/mcp/remote/mounts?gatewayGroupId=${gateway_group_id}" \
  "${gateway_admin_token_file}" 'type == "array"'
verify_authenticated_json gateway-mcp-artifacts \
  "${gateway_admin_path}/mcp/apps/artifacts?gatewayGroupId=${gateway_group_id}" \
  "${gateway_admin_token_file}" 'type == "array"'

ddc_admin_path="${DDC_ADMIN_WEB_URL}/api/v1/ddc"
verify_authenticated_json ddc-bootstrap \
  "${DDC_ADMIN_WEB_URL}/api/v1/auth/bootstrap" \
  "${ddc_admin_token_file}" 'type == "object"'
verify_authenticated_json ddc-bizs \
  "${ddc_admin_path}/bizs" "${ddc_admin_token_file}" \
  '.success == true and (.data | type) == "array"'
verify_authenticated_json ddc-envs \
  "${ddc_admin_path}/envs" "${ddc_admin_token_file}" \
  '.success == true and (.data | type) == "array"'
verify_authenticated_json ddc-apps \
  "${ddc_admin_path}/apps?bizCode=identity" "${ddc_admin_token_file}" \
  '.success == true and (.data | type) == "array"'
verify_authenticated_json ddc-namespaces \
  "${ddc_admin_path}/namespaces?bizCode=identity" \
  "${ddc_admin_token_file}" \
  '.success == true and (.data | type) == "array"'
verify_authenticated_json ddc-bindings \
  "${ddc_admin_path}/namespace-env-app-bindings?bizCode=identity&namespaceCode=default&env=local" \
  "${ddc_admin_token_file}" \
  '.success == true and (.data | type) == "array"'
verify_authenticated_json ddc-publish-tasks \
  "${ddc_admin_path}/publish-tasks" "${ddc_admin_token_file}" \
  '.success == true and (.data | type) == "array"'
verify_authenticated_json ddc-registry-services \
  "${ddc_admin_path}/registry/services?bizCode=identity&env=local&appCode=gateway-engine-default&namespace=default" \
  "${ddc_admin_token_file}" \
  '.success == true and (.data.services | type) == "array"'
verify_authenticated_json ddc-instances \
  "${ddc_admin_path}/instances?bizCode=identity&env=local&appCode=gateway-engine-default" \
  "${ddc_admin_token_file}" \
  '.success == true and (.data | type) == "array"'
verify_authenticated_json ddc-configs \
  "${ddc_admin_path}/configs?bizCode=identity&namespaceCode=default&env=local&appCode=gateway-engine-default&includeDeleted=false" \
  "${ddc_admin_token_file}" \
  '.success == true and (.data | type) == "array"'
verify_authenticated_json ddc-cache \
  "${ddc_admin_path}/cache/check?bizCode=identity&env=local&appCode=gateway-engine-default" \
  "${ddc_admin_token_file}" \
  '.success == true and (.data | type) == "array"'

unified_platform_stage "verifying Stable primitives and cross-engine MCP protocol session"
mcp_initialize "${GATEWAY_BASE_URL}" stable-a >/dev/null
stable_session="${tmp_dir}/stable-a.session"
response="$(mcp_call "${GATEWAY_ENGINE_B_PUBLIC_URL}" "${stable_session}" \
  cross-node-ping ping '{}')"
assert_json "${response}" '.result == {}' \
  "cross-engine Stable session did not preserve the ping response"

response="$(mcp_call "${GATEWAY_BASE_URL}" "${stable_session}" \
  tools-list tools/list '{}')"
assert_json "${response}" \
  '([.result.tools[].name] | sort) == ["high_risk_action","local_echo_task","local_query","rc.remote_echo","stable.remote_echo"]' \
  "MCP tool list is incomplete"
response="$(mcp_call "${GATEWAY_BASE_URL}" "${stable_session}" \
  local-tool tools/call '{"name":"local_query","arguments":{"query":{"prefix":"qa"}}}')"
assert_json "${response}" \
  '.result.isError == false and .result.structuredContent.items == ["qa-1","qa-2"]' \
  "local Gateway Operation tool failed"
response="$(mcp_call "${GATEWAY_BASE_URL}" "${stable_session}" \
  stable-tool tools/call '{"name":"stable.remote_echo","arguments":{"value":"stable"}}')"
assert_json "${response}" '.result.structuredContent.value == "stable"' \
  "Stable Remote MCP tool failed"
response="$(mcp_call "${GATEWAY_BASE_URL}" "${stable_session}" \
  rc-tool tools/call '{"name":"rc.remote_echo","arguments":{"value":"rc"}}')"
assert_json "${response}" '.result.structuredContent.value == "rc"' \
  "RC Remote MCP tool failed"

response="$(mcp_call "${GATEWAY_BASE_URL}" "${stable_session}" \
  resources-list resources/list '{}')"
assert_json "${response}" \
  '([.result.resources[].name] | sort) == ["local_status","qa_dashboard","stable.remote_text"]' \
  "MCP resource list is incomplete"
response="$(mcp_call "${GATEWAY_BASE_URL}" "${stable_session}" \
  resource-static resources/read '{"uri":"egon://unified-local/status"}')"
assert_json "${response}" \
  '.result.contents[0].text == "unified-platform-ready"' \
  "static MCP resource failed"
response="$(mcp_call "${GATEWAY_BASE_URL}" "${stable_session}" \
  resource-template resources/read '{"uri":"egon://unified-local/items/order-7"}')"
assert_json "${response}" \
  '.result.contents[0].text == "{\"source\":\"local-template\"}"' \
  "MCP resource template failed"
response="$(mcp_call "${GATEWAY_BASE_URL}" "${stable_session}" \
  resource-remote resources/read '{"uri":"egon://unified-local/remote/text"}')"
assert_json "${response}" \
  '.result.contents[0].text == "remote fixture text"' \
  "Remote MCP resource failed"
response="$(mcp_call "${GATEWAY_BASE_URL}" "${stable_session}" \
  resource-app resources/read '{"uri":"ui://unified-local/unified-local-dashboard/1.0.0"}')"
assert_json "${response}" \
  '.result.contents[0].mimeType == "text/html;profile=mcp-app" and (.result.contents[0].text | contains("Unified Local MCP"))' \
  "MCP App resource failed"
response="$(mcp_call "${GATEWAY_BASE_URL}" "${stable_session}" \
  prompt-local prompts/get '{"name":"review_item","arguments":{"id":"order-7"}}')"
assert_json "${response}" \
  '.result.messages[0].content.text == "Review item order-7"' \
  "local MCP prompt failed"
response="$(mcp_call "${GATEWAY_BASE_URL}" "${stable_session}" \
  prompt-remote prompts/get '{"name":"rc.remote_summary","arguments":{"topic":"orders"}}')"
assert_json "${response}" \
  '.result.messages[0].content.text == "Summarize orders"' \
  "Remote MCP prompt failed"
response="$(mcp_call "${GATEWAY_BASE_URL}" "${stable_session}" completion \
  completion/complete '{"ref":{"type":"ref/prompt","name":"rc.remote_summary"},"argument":{"name":"topic","value":"ord"}}')"
assert_json "${response}" \
  '.result.completion.total == 2 and .result.completion.values == ["order-1","order-2"]' \
  "MCP completion failed"
response="$(mcp_call "${GATEWAY_BASE_URL}" "${stable_session}" subscribe \
  resources/subscribe '{"uri":"egon://unified-local/status"}')"
assert_json "${response}" '.result.subscriptionId != null' \
  "MCP resource subscription failed"

stable_stream="$(curl --max-time 3 -sSN \
  -H "Authorization: Bearer $(<"${mcp_token_file}")" \
  -H "Mcp-Session-Id: $(<"${stable_session}")" \
  -H 'Accept: text/event-stream' \
  "${GATEWAY_BASE_URL}/mcp/unified-local" || true)"
grep -Fq '"id":"cross-node-ping"' <<<"${stable_stream}" \
  || unified_platform_fail "cross-engine Stable SSE stream missed the node B event"

unified_platform_stage "verifying durable task creation on A and read on B"
response="$(mcp_call "${GATEWAY_BASE_URL}" "${stable_session}" task-create \
  tools/call '{"name":"local_echo_task","arguments":{"body":{"value":"task"}}}')"
assert_json "${response}" '.result.task.status == "working"' \
  "durable MCP task was not created"
task_id="$(jq -er '.result.task.taskId' "${response}")"
for ((attempt = 1; attempt <= 20; attempt++)); do
  response="$(mcp_call "${GATEWAY_ENGINE_B_PUBLIC_URL}" "${stable_session}" \
    task-get-b tasks/get "$(jq -cn --arg task "${task_id}" '{taskId:$task}')")"
  task_state="$(jq -r '.result.status // "unknown"' "${response}")"
  [[ "${task_state}" == "completed" ]] && break
  [[ "${task_state}" == "failed" || "${task_state}" == "cancelled" ]] && break
  sleep 1
done
assert_json "${response}" \
  '.result.status == "completed" and .result.result != null' \
  "Engine B did not read the completed task created through Engine A"

unified_platform_stage "verifying stateless RC and Legacy SSE transports"
rc_response="${tmp_dir}/rc-discover.json"
rc_http_code="$(curl -sS -o "${rc_response}" -w '%{http_code}' \
  -H "Authorization: Bearer $(<"${mcp_token_file}")" \
  -H 'Content-Type: application/json' \
  -H 'Mcp-Protocol-Version: 2026-07-28' \
  -H 'Mcp-Method: server/discover' \
  --data '{"jsonrpc":"2.0","id":"rc-discover","method":"server/discover","params":{"_meta":{"client":"unified-platform-verifier"}}}' \
  "${GATEWAY_BASE_URL}/mcp/unified-local")"
[[ "${rc_http_code}" == "200" ]] \
  || unified_platform_fail "RC MCP discovery failed with HTTP ${rc_http_code}"
assert_json "${rc_response}" '.result.protocolVersion == "2026-07-28"' \
  "RC MCP discovery response is invalid"

legacy_headers="${tmp_dir}/legacy.headers"
curl --max-time 2 -sSN -D "${legacy_headers}" \
  -o "${tmp_dir}/legacy-stream.txt" \
  -H "Authorization: Bearer $(<"${mcp_token_file}")" \
  -H 'Accept: text/event-stream' \
  "${GATEWAY_BASE_URL}/legacy/mcp/unified-local" || true
grep -Eq '^HTTP/1\.[01] 200' "${legacy_headers}" \
  || unified_platform_fail "Legacy MCP SSE endpoint did not return HTTP 200"
grep -Fq 'event:endpoint' "${tmp_dir}/legacy-stream.txt" \
  || unified_platform_fail "Legacy MCP SSE endpoint event is missing"
legacy_path="$(sed -n 's/^data://p' "${tmp_dir}/legacy-stream.txt" | head -1)"
[[ "${legacy_path}" == /legacy/mcp/unified-local?sessionId=* ]] \
  || unified_platform_fail "Legacy MCP message endpoint is invalid"
legacy_http_code="$(curl -sS -o "${tmp_dir}/legacy-post.json" -w '%{http_code}' \
  -H "Authorization: Bearer $(<"${mcp_token_file}")" \
  -H 'Content-Type: application/json' \
  --data '{"jsonrpc":"2.0","id":"legacy-ping","method":"ping","params":{}}' \
  "${GATEWAY_BASE_URL}${legacy_path}")"
[[ "${legacy_http_code}" == "202" ]] \
  || unified_platform_fail "Legacy MCP message POST failed with HTTP ${legacy_http_code}"

unified_platform_stage "verifying DDC last-known-good continuity"
unified_platform_stop_process ddc
ddc_interrupted=true
[[ "$(unified_platform_http_code "${DDC_BASE_URL}/actuator/health/readiness")" != "200" ]] \
  || unified_platform_fail "DDC interruption did not take effect"
response="$(mcp_call "${GATEWAY_BASE_URL}" "${stable_session}" ddc-lkg \
  tools/call '{"name":"local_query","arguments":{"query":{"prefix":"lkg"}}}')"
assert_json "${response}" '.result.isError == false' \
  "Gateway lost the last-known-good release during DDC interruption"
unified_platform_start_jar ddc \
  "${unified_platform_env_dir}/ddc.env" "${ddc_jar}"
unified_platform_wait_http ddc "${DDC_BASE_URL}/actuator/health/readiness" 60
ddc_interrupted=false

unified_platform_stage "verifying the annotation-managed release on both engines"
group_id="$(<"${gateway_group_file}")"
history_file="${tmp_dir}/release-history-before.json"
[[ "$(gateway_request GET \
  "/api/v1/gateway/admin/gateway-groups/${group_id}/releases" '' \
  "${history_file}")" == "200" ]] \
  || unified_platform_fail "Gateway release history is unavailable"
assert_json "${history_file}" \
  '([.[] | select(.status == "SUCCESS")][0].attempts[0].targets | length) >= 2' \
  "latest release was not applied to both Gateway engines"
response="$(mcp_call "${GATEWAY_BASE_URL}" "${stable_session}" release-lkg \
  tools/call '{"name":"local_query","arguments":{"query":{"prefix":"release-lkg"}}}')"
assert_json "${response}" '.result.isError == false' \
  "annotation-managed MCP release is unavailable"

unified_platform_stage "verifying Remote MCP circuit opening and recovery"
unified_platform_stop_process mcp-remote
remote_interrupted=true
for attempt in 1 2; do
  response="$(mcp_call "${GATEWAY_BASE_URL}" "${stable_session}" \
    "remote-outage-${attempt}" tools/call \
    '{"name":"stable.remote_echo","arguments":{"value":"outage"}}')"
  assert_json "${response}" '.error.dataCode == "MCP_REMOTE_UNAVAILABLE"' \
    "Remote MCP outage was not isolated"
done
unified_platform_start_jar mcp-remote \
  "${unified_platform_env_dir}/mcp-remote.env" "${mcp_remote_jar}"
unified_platform_wait_http mcp-remote \
  "${MCP_REMOTE_BASE_URL}/actuator/health/readiness" 60
remote_interrupted=false
sleep 4
response="$(mcp_call "${GATEWAY_BASE_URL}" "${stable_session}" \
  remote-recovered tools/call \
  '{"name":"stable.remote_echo","arguments":{"value":"recovered"}}')"
assert_json "${response}" '.result.structuredContent.value == "recovered"' \
  "Remote MCP circuit did not recover"

unified_platform_stage "verifying RBAC3 revocation and unchanged-token recovery"
put_role_activations non-mock
roles_modified=true
for ((attempt = 1; attempt <= 20; attempt++)); do
  response="$(mcp_call "${GATEWAY_BASE_URL}" "${stable_session}" \
    "rbac-denied-${attempt}" tools/call \
    '{"name":"local_query","arguments":{"query":{"prefix":"denied"}}}')"
  if jq -e '.error.dataCode == "MCP_FORBIDDEN"
      and (.error.data.reasonCode | startswith("RBAC3_"))' \
      "${response}" >/dev/null; then
    break
  fi
  sleep 1
done
assert_json "${response}" \
  '.error.dataCode == "MCP_FORBIDDEN"
    and (.error.data.reasonCode | startswith("RBAC3_"))' \
  "RBAC3 permission revocation did not deny the MCP call"
put_role_activations all
roles_modified=false
for ((attempt = 1; attempt <= 20; attempt++)); do
  response="$(mcp_call "${GATEWAY_BASE_URL}" "${stable_session}" \
    "rbac-restored-${attempt}" tools/call \
    '{"name":"local_query","arguments":{"query":{"prefix":"restored"}}}')"
  if jq -e '.result.isError == false' "${response}" >/dev/null; then
    break
  fi
  sleep 1
done
assert_json "${response}" '.result.isError == false' \
  "RBAC3 permission restoration did not authorize the unchanged token"

unified_platform_stage "recording sanitized verification evidence"
jq -n --arg verifiedAt "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  '{verifiedAt:$verifiedAt,status:"PASS",checks:["process-health","admin-web-default-tenant-membership","admin-web-gateway-cookie-login","admin-feature-matrix","identity-jwt-cookie-stable-refresh-rt-revoke","rbac3-snapshot-revocation","ddc-registration-and-lkg","gateway-annotation-managed-mcp-release","mcp-stable-rc-legacy","mcp-local-remote-primitives","mcp-app","mcp-cross-engine-protocol-session-and-task","remote-circuit-recovery"]}' \
  >"${unified_platform_evidence_dir}/verification-summary.json"
chmod 600 "${unified_platform_evidence_dir}/verification-summary.json"

printf 'Unified platform deep verification passed. Sanitized evidence: %s\n' \
  "${unified_platform_evidence_dir}/verification-summary.json"
