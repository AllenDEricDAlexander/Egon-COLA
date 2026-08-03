#!/usr/bin/env bash
set -uo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${script_dir}/lib/common.sh"

legacy_script="${unified_platform_repo_root}/scripts/unified-identity-local.sh"
ddc_jar="${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/target/egon-cola-platform-dynamic-config-center-admin-exec.jar"
mcp_remote_jar="${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-mcp-remote/target/gateway-test-mcp-remote-exec.jar"
gateway_admin_token_file="${unified_platform_secret_dir}/gateway-admin.access.jwt"
tenant_token_file="${unified_platform_secret_dir}/tenant-b.access.jwt"
rbac3_token_file="${unified_platform_secret_dir}/rbac3-tenant-b.access.jwt"
gateway_group_file="${unified_platform_runtime_dir}/gateway-group.id"

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

set -e
for command in curl jq openssl; do
  unified_platform_require_command "${command}"
done
for file in \
  "${gateway_admin_token_file}" \
  "${tenant_token_file}" \
  "${rbac3_token_file}" \
  "${gateway_group_file}"; do
  [[ -s "${file}" ]] || unified_platform_fail "missing verifier input: ${file}"
done

tmp_dir="$(mktemp -d "${unified_platform_runtime_dir}/verify.XXXXXX")"
chmod 700 "${tmp_dir}"
ddc_interrupted=false
remote_interrupted=false
route_removed=false
roles_modified=false
route_restore_body=""

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

restore_route() {
  [[ "${route_removed}" == "true" && -n "${route_restore_body}" ]] || return
  gateway_request PUT \
    "/api/v1/gateway/admin/gateway-groups/$(<"${gateway_group_file}")/draft/routes/mcp-local-query" \
    "${route_restore_body}" "${tmp_dir}/route-restore.json" >/dev/null || true
  route_removed=false
}

role_ids() {
  local mode="$1" candidates="$2"
  if [[ "${mode}" == "all" ]]; then
    jq -c '[.data.applications[].candidates[].rootRoleId] | unique' \
      "${candidates}"
  else
    jq -c '[.data.applications[] | select(.applicationCode != "mock-backend") | .candidates[].rootRoleId] | unique' \
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
  version="$(jq -er '.data.sessionVersion' "${current}")"
  request="$(jq -cn --argjson roles "${roles}" --argjson version "${version}" \
    '{roleIds:$roles,expectedContextVersion:$version}')"
  http_code="$(curl -sS -o "${tmp_dir}/role-update.json" -w '%{http_code}' \
    -X PUT -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $(<"${rbac3_token_file}")" \
    -d "${request}" "${RBAC3_BASE_URL}/api/rbac3/v1/auth/role-activations")"
  [[ "${http_code}" == "200" ]] || unified_platform_fail \
    "RBAC3 role activation update failed with HTTP ${http_code}"
}

recover_interrupted_services() {
  set +e
  restore_route
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

mcp_initialize() {
  local endpoint="$1" label="$2" headers response http_code session_id
  headers="${tmp_dir}/${label}.headers"
  response="${tmp_dir}/${label}.json"
  http_code="$(curl -sS -D "${headers}" -o "${response}" -w '%{http_code}' \
    -H "Authorization: Bearer $(<"${tenant_token_file}")" \
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
    -H "Authorization: Bearer $(<"${tenant_token_file}")" \
    -H "Mcp-Session-Id: $(<"${session_file}")" \
    -H 'Mcp-Protocol-Version: 2025-11-25' \
    -H 'Accept: application/json, text/event-stream' \
    -H 'Content-Type: application/json' -d "${request}" \
    "${endpoint}/mcp/unified-local")"
  [[ "${http_code}" == "200" || "${http_code}" == "202" ]] \
    || unified_platform_fail "MCP ${method} failed with HTTP ${http_code}"
  printf '%s' "${response}"
}

unified_platform_stage "verifying unified identity SSO and revocation semantics"
if [[ "${UNIFIED_PLATFORM_SKIP_IDENTITY_VERIFY:-false}" != "true" ]]; then
  UNIFIED_IDENTITY_RUNTIME_DIR="${unified_platform_runtime_dir}" \
  UNIFIED_IDENTITY_IDP_URL="${IDP_BASE_URL}" \
  UNIFIED_IDENTITY_RBAC3_URL="${RBAC3_BASE_URL}" \
  UNIFIED_IDENTITY_GATEWAY_ADMIN_URL="${GATEWAY_ADMIN_BASE_URL}" \
  UNIFIED_IDENTITY_DDC_URL="${DDC_BASE_URL}" \
  UNIFIED_IDENTITY_MOCK_URL="${MOCK_BACKEND_BASE_URL}" \
  UNIFIED_IDENTITY_GATEWAY_URL="${GATEWAY_BASE_URL}" \
    "${legacy_script}" verify >"${tmp_dir}/identity-verification.log"
fi
UNIFIED_IDENTITY_RUNTIME_DIR="${unified_platform_runtime_dir}" \
UNIFIED_IDENTITY_IDP_URL="${IDP_BASE_URL}" \
UNIFIED_IDENTITY_RBAC3_URL="${RBAC3_BASE_URL}" \
  "${legacy_script}" refresh-tokens >"${tmp_dir}/token-refresh.log"

unified_platform_stage "verifying Stable primitives and cross-engine Redis session"
mcp_initialize "${GATEWAY_BASE_URL}" stable-a >/dev/null
stable_session="${tmp_dir}/stable-a.session"
response="$(mcp_call "${GATEWAY_ENGINE_B_PUBLIC_URL}" "${stable_session}" \
  cross-node-ping ping '{}')"
assert_json "${response}" '.result == {}' \
  "cross-engine Stable session did not preserve the ping response"

response="$(mcp_call "${GATEWAY_BASE_URL}" "${stable_session}" \
  tools-list tools/list '{}')"
assert_json "${response}" \
  '([.result.tools[].name] | sort) == ["high_risk_query","local_query","local_query_task","rc.remote_echo","stable.remote_echo"]' \
  "MCP tool list is incomplete"
response="$(mcp_call "${GATEWAY_BASE_URL}" "${stable_session}" \
  local-tool tools/call '{"name":"local_query","arguments":{"prefix":"qa"}}')"
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
  -H "Authorization: Bearer $(<"${tenant_token_file}")" \
  -H "Mcp-Session-Id: $(<"${stable_session}")" \
  -H 'Accept: text/event-stream' \
  "${GATEWAY_BASE_URL}/mcp/unified-local" || true)"
grep -Fq '"id":"cross-node-ping"' <<<"${stable_stream}" \
  || unified_platform_fail "cross-engine Stable SSE stream missed the node B event"

unified_platform_stage "verifying durable task creation on A and read on B"
response="$(mcp_call "${GATEWAY_BASE_URL}" "${stable_session}" task-create \
  tools/call '{"name":"local_query_task","arguments":{"prefix":"task"}}')"
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
  -H "Authorization: Bearer $(<"${tenant_token_file}")" \
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
  -H "Authorization: Bearer $(<"${tenant_token_file}")" \
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
  -H "Authorization: Bearer $(<"${tenant_token_file}")" \
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
  tools/call '{"name":"local_query","arguments":{"prefix":"lkg"}}')"
assert_json "${response}" '.result.isError == false' \
  "Gateway lost the last-known-good release during DDC interruption"
unified_platform_start_jar ddc \
  "${unified_platform_env_dir}/ddc.env" "${ddc_jar}"
unified_platform_wait_http ddc "${DDC_BASE_URL}/actuator/health/readiness" 60
ddc_interrupted=false

unified_platform_stage "verifying invalid release rejection keeps the active release"
group_id="$(<"${gateway_group_file}")"
history_file="${tmp_dir}/release-history-before.json"
[[ "$(gateway_request GET \
  "/api/v1/gateway/admin/gateway-groups/${group_id}/releases" '' \
  "${history_file}")" == "200" ]] \
  || unified_platform_fail "Gateway release history is unavailable"
active_release="$(jq -er '[.[] | select(.status == "SUCCESS")][0].releaseId' \
  "${history_file}")"
assert_json "${history_file}" \
  '([.[] | select(.status == "SUCCESS")][0].attempts[0].targets | length) >= 2' \
  "latest release was not applied to both Gateway engines"
draft_file="${tmp_dir}/draft-before-invalid.json"
[[ "$(gateway_request GET \
  "/api/v1/gateway/admin/gateway-groups/${group_id}/draft" '' \
  "${draft_file}")" == "200" ]] \
  || unified_platform_fail "Gateway draft is unavailable"
route="$(jq -cer '.routes[] | select(.routeId == "mcp-local-query")' \
  "${draft_file}")"
draft_revision="$(jq -er '.revision' "${draft_file}")"
delete_body="$(jq -cn --argjson revision "${draft_revision}" \
  '{expectedRevision:$revision,idempotencyKey:("unified-verify-remove-operation-" + ($revision|tostring)),changeReason:"Verify invalid release protection"}')"
delete_code="$(gateway_request DELETE \
  "/api/v1/gateway/admin/gateway-groups/${group_id}/draft/routes/mcp-local-query" \
  "${delete_body}" "${tmp_dir}/route-delete.json")"
[[ "${delete_code}" == "200" ]] \
  || unified_platform_fail "could not prepare the invalid Gateway draft"
route_removed=true
deleted_revision="$(jq -er '.revision' "${tmp_dir}/route-delete.json")"
route_restore_body="$(jq -cn --argjson route "${route}" \
  --argjson revision "${deleted_revision}" \
  '{operationId:$route.operationId,content:$route.content,enabled:$route.enabled,expectedRevision:$revision,idempotencyKey:("unified-verify-restore-operation-" + ($revision|tostring)),changeReason:"Restore MCP Operation anchor after invalid release test"}')"
invalid_release_body="$(jq -cn --argjson revision "${deleted_revision}" \
  '{expectedDraftRevision:$revision,changeReason:"This release must be rejected because its MCP operation is missing"}')"
invalid_code="$(gateway_request POST \
  "/api/v1/gateway/admin/gateway-groups/${group_id}/releases" \
  "${invalid_release_body}" "${tmp_dir}/invalid-release.json")"
[[ ! "${invalid_code}" =~ ^2[0-9][0-9]$ ]] \
  || unified_platform_fail "invalid Gateway release unexpectedly succeeded"
history_after="${tmp_dir}/release-history-after.json"
[[ "$(gateway_request GET \
  "/api/v1/gateway/admin/gateway-groups/${group_id}/releases" '' \
  "${history_after}")" == "200" ]] \
  || unified_platform_fail "Gateway release history is unavailable after rejection"
[[ "$(jq -er '[.[] | select(.status == "SUCCESS")][0].releaseId' \
  "${history_after}")" == "${active_release}" ]] \
  || unified_platform_fail "invalid release changed the active release"
restore_route
response="$(mcp_call "${GATEWAY_BASE_URL}" "${stable_session}" release-lkg \
  tools/call '{"name":"local_query","arguments":{"prefix":"release-lkg"}}')"
assert_json "${response}" '.result.isError == false' \
  "active MCP release stopped serving after invalid release rejection"

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
    '{"name":"local_query","arguments":{"prefix":"denied"}}')"
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
    '{"name":"local_query","arguments":{"prefix":"restored"}}')"
  if jq -e '.result.isError == false' "${response}" >/dev/null; then
    break
  fi
  sleep 1
done
assert_json "${response}" '.result.isError == false' \
  "RBAC3 permission restoration did not authorize the unchanged token"

unified_platform_stage "recording sanitized verification evidence"
jq -n --arg verifiedAt "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  '{verifiedAt:$verifiedAt,status:"PASS",checks:["process-health","identity-sso-tokenVersion-refresh-replay","rbac3-snapshot-revocation","ddc-registration-and-lkg","gateway-invalid-release-protection","mcp-stable-rc-legacy","mcp-local-remote-primitives","mcp-app","mcp-cross-engine-session-and-task","remote-circuit-recovery","admin-webs"]}' \
  >"${unified_platform_evidence_dir}/verification-summary.json"
chmod 600 "${unified_platform_evidence_dir}/verification-summary.json"

printf 'Unified platform deep verification passed. Sanitized evidence: %s\n' \
  "${unified_platform_evidence_dir}/verification-summary.json"
