#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${script_dir}/lib/common.sh"

legacy_script="${unified_platform_repo_root}/scripts/unified-identity-local.sh"
release_fixture="${script_dir}/fixtures/unified-platform-release.json"
gateway_web_dir="${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web"
idp_web_dir="${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web"
rbac3_root_dir="${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-rbac3"
rbac3_web_dir="${rbac3_root_dir}/egon-cola-platform-rbac3-admin-web"
ddc_web_dir="${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web"
gateway_engine_jar="${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/target/egon-cola-platform-gateway-engine-exec.jar"
mcp_provider_jar="${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-mcp-provider/target/gateway-test-mcp-provider-exec.jar"
mcp_remote_jar="${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-mcp-remote/target/gateway-test-mcp-remote-exec.jar"
gateway_admin_token_file="${unified_platform_secret_dir}/gateway-admin.access.jwt"
idp_admin_token_file="${unified_platform_secret_dir}/idp-admin.access.jwt"
gateway_group_file="${unified_platform_runtime_dir}/gateway-group.id"
default_tenant_id=

for command in java curl jq openssl psql redis-cli npm; do
  unified_platform_require_command "${command}"
done

[[ -s "${release_fixture}" ]] \
  || unified_platform_fail "missing release fixture: ${release_fixture}"

unified_platform_initialize_directories
mkdir -p "${unified_platform_runtime_dir}/mcp-artifacts"
chmod 700 "${unified_platform_runtime_dir}/mcp-artifacts"

import_existing_qa_credential() {
  local source_file marker_file target_file
  source_file="${UNIFIED_PLATFORM_EXISTING_IDP_PASSWORD_FILE:-${unified_platform_repo_root}/.runtime/unified-identity/secrets/idp-admin.password}"
  marker_file="${unified_platform_secret_dir}/.idp-password-imported"
  target_file="${unified_platform_secret_dir}/idp-admin.password"
  if [[ -s "${source_file}" && ! -e "${marker_file}" \
      && "${source_file}" != "${target_file}" ]]; then
    cp "${source_file}" "${target_file}"
    chmod 600 "${target_file}"
    printf '%s\n' "${source_file}" >"${marker_file}"
    chmod 600 "${marker_file}"
  fi
}

import_existing_qa_credential

export UNIFIED_IDENTITY_RUNTIME_DIR="${unified_platform_runtime_dir}"
export UNIFIED_IDENTITY_IDP_URL="${IDP_BASE_URL}"
export UNIFIED_IDENTITY_IDP_RPC_TARGET="${IDP_RPC_TARGET}"
export UNIFIED_IDENTITY_RBAC3_URL="${RBAC3_BASE_URL}"
export UNIFIED_IDENTITY_GATEWAY_ADMIN_URL="${GATEWAY_ADMIN_BASE_URL}"
export UNIFIED_IDENTITY_DDC_URL="${DDC_BASE_URL}"
export UNIFIED_IDENTITY_DDC_RPC_TARGET="${DDC_RPC_TARGET}"
export UNIFIED_IDENTITY_MOCK_URL="${MOCK_BACKEND_BASE_URL}"
export UNIFIED_IDENTITY_GATEWAY_URL="${GATEWAY_BASE_URL}"
export UNIFIED_IDENTITY_SKIP_BUILD="${UNIFIED_PLATFORM_SKIP_BUILD:-false}"
export UNIFIED_IDENTITY_DEFER_GATEWAY_RELEASE=true

prepare_admin_web_login_environments() {
  local token_file="${unified_platform_secret_dir}/idp-admin.access.jwt"
  [[ -s "${token_file}" ]] \
    || unified_platform_fail "default tenant access token is unavailable"
  default_tenant_id="$(jq -Rer \
    'split(".")[1] | @base64d | fromjson | .tid' <"${token_file}")"
  [[ "${default_tenant_id}" =~ ^[1-9][0-9]*$ ]] \
    || unified_platform_fail "default tenant access token has an invalid tenant ID"
  unified_platform_write_frontend_login_env \
    "${idp_web_dir}" "${default_tenant_id}"
  unified_platform_write_frontend_login_env \
    "${rbac3_web_dir}" "${default_tenant_id}"
  unified_platform_write_frontend_login_env \
    "${gateway_web_dir}" "${default_tenant_id}"
  unified_platform_write_frontend_login_env \
    "${ddc_web_dir}" "${default_tenant_id}"
}

gateway_api() {
  local method="$1" path="$2" body="${3:-}" idempotency_key="${4:-}"
  local response_file status response
  [[ -s "${gateway_admin_token_file}" ]] \
    || unified_platform_fail "Gateway Admin access token is unavailable"
  response_file="$(mktemp "${unified_platform_runtime_dir}/gateway-api.XXXXXX")"
  local arguments=(--max-time 30 -sS -X "${method}"
    -H "Authorization: Bearer $(<"${gateway_admin_token_file}")"
    -H 'Content-Type: application/json')
  if [[ -n "${idempotency_key}" ]]; then
    arguments+=(-H "Idempotency-Key: ${idempotency_key}")
  fi
  if [[ -n "${body}" ]]; then
    arguments+=(-d "${body}")
  fi
  status="$(curl "${arguments[@]}" -o "${response_file}" -w '%{http_code}' \
    "${GATEWAY_ADMIN_BASE_URL}${path}")"
  response="$(<"${response_file}")"
  rm -f "${response_file}"
  if [[ ! "${status}" =~ ^2[0-9][0-9]$ ]]; then
    unified_platform_fail \
      "Gateway Admin ${method} ${path} failed with HTTP ${status}: ${response}"
  fi
  printf '%s' "${response}"
}

issue_mcp_user_token() {
  UNIFIED_IDENTITY_OAUTH_CLIENT_ID=mock-backend \
  UNIFIED_IDENTITY_OAUTH_TENANT=tenant-b \
  UNIFIED_IDENTITY_OAUTH_RESOURCE_URI=https://api.egon.internal/local/identity/gateway-test-mcp-provider \
  UNIFIED_IDENTITY_OAUTH_TOKEN_FILE="${unified_platform_secret_dir}/mcp-tenant-b.access.jwt" \
    "${legacy_script}" issue-user-token
}

ensure_mcp_user_delegation() {
  local resource_id=identity-gateway-test-mcp-provider-local
  local resource_response resource_version response_file status body
  if issue_mcp_user_token >/dev/null 2>&1; then
    return
  fi
  [[ -s "${idp_admin_token_file}" ]] \
    || unified_platform_fail "IdP Admin access token is unavailable"
  response_file="$(mktemp "${unified_platform_runtime_dir}/idp-api.XXXXXX")"
  resource_response="$(curl --max-time 30 -fsS \
    -H "Authorization: Bearer $(<"${idp_admin_token_file}")" \
    "${IDP_BASE_URL}/api/v1/identity/resource-servers/${resource_id}")" \
    || unified_platform_fail "MCP provider Resource is unavailable"
  resource_version="$(jq -er '.version' <<<"${resource_response}")"
  body="$(jq -cn --argjson version "${resource_version}" \
    '{grantType:"USER_DELEGATION",tenantId:null,allowedScopes:[],expectedResourceVersion:$version,expectedGrantVersion:null}')"
  status="$(curl --max-time 30 -sS -o "${response_file}" -w '%{http_code}' \
    -X PUT -H "Authorization: Bearer $(<"${idp_admin_token_file}")" \
    -H 'Content-Type: application/json' -d "${body}" \
    "${IDP_BASE_URL}/api/v1/identity/clients/mock-backend/resources/${resource_id}")"
  if [[ ! "${status}" =~ ^2[0-9][0-9]$ ]]; then
    body="$(<"${response_file}")"
    rm -f "${response_file}"
    unified_platform_fail \
      "MCP USER_DELEGATION grant failed with HTTP ${status}: ${body}"
  fi
  rm -f "${response_file}"
  issue_mcp_user_token \
    || unified_platform_fail "MCP provider Resource token issuance failed"
}

draft_revision() {
  gateway_api GET "/api/v1/gateway/admin/gateway-groups/$1/draft" \
    | jq -er '.revision'
}

package_mcp_fixtures() {
  if [[ "${UNIFIED_PLATFORM_SKIP_BUILD:-false}" == "true" \
      && -s "${mcp_provider_jar}" && -s "${mcp_remote_jar}" ]]; then
    return
  fi
  "${unified_platform_repo_root}/mvnw" -B -ntp \
    -f "${unified_platform_repo_root}/pom.xml" \
    -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-mcp-provider,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-mcp-remote \
    -am package -DskipTests
}

write_extra_service_env_files() {
  local file redis_password
  redis_password="$(<"${unified_platform_secret_dir}/redis.password")"

  file="${unified_platform_env_dir}/mcp-remote.env"
  : >"${file}"
  chmod 600 "${file}"
  unified_platform_write_env "${file}" MCP_TEST_REMOTE_PORT 18151

  file="${unified_platform_env_dir}/gateway-engine-b.env"
  cp "${unified_platform_env_dir}/gateway-engine.env" "${file}"
  chmod 600 "${file}"
  unified_platform_write_env "${file}" SERVER_PORT 18183
  unified_platform_write_env "${file}" EGON_COLA_COMPONENT_ID_MACHINE_ID 36
  unified_platform_write_env "${file}" EGON_COLA_COMPONENT_GATEWAY_ENGINE_NODE_ID gateway-engine-local-b
  unified_platform_write_env "${file}" EGON_COLA_COMPONENT_GATEWAY_ENGINE_INSTANCE_ID gateway-engine-local-2
  unified_platform_write_env "${file}" EGON_COLA_COMPONENT_GATEWAY_ENGINE_DATA_DIRECTORY "${unified_platform_runtime_dir}/gateway-engine-b-data"
  unified_platform_write_env "${file}" EGON_COLA_COMPONENT_GATEWAY_ENGINE_HTTP_PUBLIC_PORT 18184
  unified_platform_write_env "${file}" EGON_COLA_COMPONENT_GATEWAY_ENGINE_HTTP_INTERNAL_PORT 18185
  unified_platform_write_env "${file}" EGON_COLA_COMPONENT_GATEWAY_ENGINE_RPC_PORT 19190
  unified_platform_write_env "${file}" GATEWAY_ENGINE_DDC_INSTANCE_ID gateway-engine-local-2
  unified_platform_write_env "${file}" GATEWAY_ENGINE_DDC_ADVERTISED_PORT 18184
  unified_platform_write_env "${file}" GATEWAY_MCP_REMOTE_CIRCUIT_OPEN_DURATION PT3S
  unified_platform_write_env "${file}" GATEWAY_MCP_REMOTE_FAILURE_THRESHOLD 2
  unified_platform_write_env "${file}" GATEWAY_MCP_TASK_POLL_INTERVAL PT1S

  file="${unified_platform_env_dir}/mcp-provider.env"
  : >"${file}"
  chmod 600 "${file}"
  unified_platform_write_env "${file}" MCP_TEST_PROVIDER_PORT 18161
  unified_platform_write_env "${file}" MCP_TEST_PROVIDER_HOST 127.0.0.1
  unified_platform_write_env "${file}" MCP_TEST_PROVIDER_INSTANCE_ID mcp-provider-local-1
  unified_platform_write_env "${file}" MCP_TEST_PROVIDER_BUILD_ID \
    "$(unified_platform_local_build_id "${mcp_provider_jar}")"
  unified_platform_write_env "${file}" IDP_OAUTH_ISSUER "${IDP_BASE_URL}"
  unified_platform_write_env "${file}" IDP_JWK_SET_URI \
    "${IDP_BASE_URL}/oauth2/jwks"
  unified_platform_write_env "${file}" MCP_PROVIDER_RESOURCE_SERVER_ID \
    identity-gateway-test-mcp-provider-local
  unified_platform_write_env "${file}" MCP_PROVIDER_RESOURCE_URI \
    https://api.egon.internal/local/identity/gateway-test-mcp-provider
  unified_platform_write_env "${file}" \
    MCP_PROVIDER_RESOURCE_MANAGEMENT_CLIENT_ID mcp-provider-service
  unified_platform_write_env "${file}" \
    MCP_PROVIDER_RESOURCE_MANAGEMENT_KEY_ID mcp-provider-local
  unified_platform_write_env "${file}" \
    MCP_PROVIDER_RESOURCE_MANAGEMENT_PRIVATE_KEY_FILE \
    "${unified_platform_secret_dir}/mcp-provider-private.pem"
  unified_platform_write_env "${file}" \
    MCP_PROVIDER_RESOURCE_ADMISSION_RPC_TARGET "${IDP_RPC_TARGET}"
  unified_platform_write_env "${file}" \
    IDP_ADMISSION_RPC_DEVELOPMENT_PLAINTEXT true
  unified_platform_write_env "${file}" MCP_PROVIDER_REDIS_ADDRESS \
    redis://127.0.0.1:6379
  unified_platform_write_env "${file}" MCP_PROVIDER_REDIS_DATABASE 8
  unified_platform_write_env "${file}" MCP_PROVIDER_REDIS_PASSWORD_FILE \
    "${unified_platform_secret_dir}/redis.password"
  unified_platform_write_env "${file}" RBAC3_AUTHORIZATION_ENDPOINT \
    "${RBAC3_BASE_URL}"
  unified_platform_write_env "${file}" \
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_ENABLED true
  unified_platform_write_env "${file}" \
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_TOKEN_ENDPOINT \
    "${IDP_BASE_URL}/oauth2/token"
  unified_platform_write_env "${file}" \
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_CLIENT_ID \
    mcp-provider-service
  unified_platform_write_env "${file}" \
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_KEY_ID \
    mcp-provider-local
  unified_platform_write_env "${file}" \
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_PRIVATE_KEY_FILE \
    "${unified_platform_secret_dir}/mcp-provider-private.pem"
  unified_platform_write_env "${file}" \
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_RESOURCE_URI \
    https://api.egon.internal/local/permission/rbac3
  unified_platform_write_env "${file}" \
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_SCOPES \
    "service:authorization:decide service:authorization:snapshot service:identity:resolve"
  unified_platform_write_env "${file}" DDC_ENABLED true
  unified_platform_write_env "${file}" DDC_BIZ_CODE identity
  unified_platform_write_env "${file}" DDC_APP_CODE gateway-test-mcp-provider
  unified_platform_write_env "${file}" DDC_ENV local
  unified_platform_write_env "${file}" DDC_NAMESPACE default
  unified_platform_write_env "${file}" DDC_RPC_TARGET "${DDC_RPC_TARGET}"
  unified_platform_write_env "${file}" DDC_RPC_DEVELOPMENT_PLAINTEXT true
  unified_platform_write_env "${file}" DDC_RPC_RUNTIME_ACCESS_KEY "$(<"${unified_platform_secret_dir}/ddc-runtime.access-key")"
  unified_platform_write_env "${file}" DDC_RPC_RUNTIME_SECRET_KEY "$(<"${unified_platform_secret_dir}/ddc-runtime.secret")"
  unified_platform_write_env "${file}" DDC_RPC_REGISTRY_ACCESS_KEY "$(<"${unified_platform_secret_dir}/ddc-registry.access-key")"
  unified_platform_write_env "${file}" DDC_RPC_REGISTRY_SECRET_KEY "$(<"${unified_platform_secret_dir}/ddc-registry.secret")"
  unified_platform_write_env "${file}" DDC_REGISTRY_ENABLED true
  unified_platform_write_env "${file}" DDC_REGISTRY_REDIS_HOST 127.0.0.1
  unified_platform_write_env "${file}" DDC_REGISTRY_REDIS_PORT 6379
  unified_platform_write_env "${file}" DDC_REGISTRY_REDIS_PASSWORD "${redis_password}"
  unified_platform_write_env "${file}" DDC_REGISTRY_REDIS_DATABASE 10
  unified_platform_write_env "${file}" GATEWAY_REPORTING_ENABLED true
  unified_platform_write_env "${file}" GATEWAY_ADMIN_BASE_URL "${GATEWAY_ADMIN_BASE_URL}"
  unified_platform_write_env "${file}" GATEWAY_REPORT_STATE_FILE "${unified_platform_runtime_dir}/mcp-provider-gateway-report.json"
}

initialize_mcp_provider_application() {
  local applications application_id application credential
  applications="$(gateway_api GET \
    '/api/v1/gateway/admin/applications?bizCode=identity&namespace=default&env=local&appCode=gateway-test-mcp-provider')"
  application_id="$(jq -r '.[0].id // empty' <<<"${applications}")"
  if [[ -z "${application_id}" ]]; then
    application="$(gateway_api POST /api/v1/gateway/admin/applications \
      '{"bizCode":"identity","applicationCode":"gateway-test-mcp-provider","displayName":"Gateway MCP Local Provider","env":"local","namespace":"default","description":"Host-local MCP Operation fixture"}')"
    application_id="$(jq -er '.id' <<<"${application}")"
  fi
  credential="$(gateway_api POST \
    "/api/v1/gateway/admin/applications/${application_id}/credentials" '{}')"
  jq -er '.accessKey' <<<"${credential}" \
    >"${unified_platform_secret_dir}/mcp-provider-report.access-key"
  jq -er '.secret' <<<"${credential}" \
    >"${unified_platform_secret_dir}/mcp-provider-report.secret"
  printf '%s' "${application_id}" \
    >"${unified_platform_runtime_dir}/mcp-provider-application.id"
  chmod 600 \
    "${unified_platform_secret_dir}/mcp-provider-report.access-key" \
    "${unified_platform_secret_dir}/mcp-provider-report.secret" \
    "${unified_platform_runtime_dir}/mcp-provider-application.id"
  unified_platform_write_env "${unified_platform_env_dir}/mcp-provider.env" \
    GATEWAY_REPORT_ACCESS_KEY \
    "$(<"${unified_platform_secret_dir}/mcp-provider-report.access-key")"
  unified_platform_write_env "${unified_platform_env_dir}/mcp-provider.env" \
    GATEWAY_REPORT_SECRET_KEY \
    "$(<"${unified_platform_secret_dir}/mcp-provider-report.secret")"
}

wait_mcp_provider_catalog() {
  local application_id response
  application_id="$(<"${unified_platform_runtime_dir}/mcp-provider-application.id")"
  for ((attempt = 1; attempt <= 60; attempt++)); do
    response="$(gateway_api GET \
      "/api/v1/gateway/admin/applications/${application_id}/catalog" || true)"
    if jq -e '.. | objects | select(.methodIdentity? == "GET /api/mcp-fixtures/query" and .lifecycleStatus? == "ACTIVE")' \
        <<<"${response}" >/dev/null 2>&1; then
      return
    fi
    sleep 1
  done
  unified_platform_fail "MCP Provider Gateway catalog did not become active"
}

ensure_mcp_server() {
  local group_id="$1" servers server server_id revision response body
  local expected_resource
  servers="$(gateway_api GET \
    "/api/v1/gateway/admin/mcp/servers?gatewayGroupId=${group_id}")"
  server="$(jq -c '.[] | select(.serverCode == "unified-local")' \
    <<<"${servers}" | head -1)"
  server_id="$(jq -r '.id // empty' <<<"${server}")"
  expected_resource="$(jq -er '.server.resourceUri' "${release_fixture}")"
  if [[ -z "${server_id}" ]]; then
    revision="$(draft_revision "${group_id}")"
    body="$(jq -c --arg group "${group_id}" --argjson draft "${revision}" \
      '.server + {gatewayGroupId:$group,expectedRevision:0,expectedDraftRevision:$draft,changeReason:"Create host-local unified MCP Server"}' \
      "${release_fixture}")"
    response="$(gateway_api POST /api/v1/gateway/admin/mcp/servers \
      "${body}" unified-local-server-v1)"
    server_id="$(jq -er '.resourceId' <<<"${response}")"
  elif [[ "$(jq -r '.resourceUri' <<<"${server}")" \
      != "${expected_resource}" ]]; then
    revision="$(draft_revision "${group_id}")"
    body="$(jq -c --arg group "${group_id}" \
      --argjson expected "$(jq -r '.revision' <<<"${server}")" \
      --argjson draft "${revision}" \
      '.server + {gatewayGroupId:$group,expectedRevision:$expected,expectedDraftRevision:$draft,changeReason:"Bind host-local MCP Server to its exact provider Resource"}' \
      "${release_fixture}")"
    gateway_api PUT "/api/v1/gateway/admin/mcp/servers/${server_id}" \
      "${body}" unified-local-server-provider-resource-v1 >/dev/null
  fi
  printf '%s' "${server_id}" >"${unified_platform_runtime_dir}/mcp-server.id"
  chmod 600 "${unified_platform_runtime_dir}/mcp-server.id"
}

ensure_remote_providers() {
  local group_id="$1" item code providers provider_id revision response body
  while IFS= read -r item; do
    code="$(jq -er '.providerCode' <<<"${item}")"
    providers="$(gateway_api GET \
      "/api/v1/gateway/admin/mcp/remote/providers?gatewayGroupId=${group_id}")"
    provider_id="$(jq -r --arg code "${code}" \
      '.[] | select(.providerCode == $code) | .id' <<<"${providers}" | head -1)"
    if [[ -z "${provider_id}" ]]; then
      revision="$(draft_revision "${group_id}")"
      body="$(jq -cn --arg group "${group_id}" --argjson item "${item}" \
        --argjson draft "${revision}" \
        '$item + {gatewayGroupId:$group,expectedRevision:0,expectedDraftRevision:$draft,changeReason:("Create host-local Remote Provider " + $item.providerCode)}')"
      response="$(gateway_api POST /api/v1/gateway/admin/mcp/remote/providers \
        "${body}" "unified-local-provider-${code}-v1")"
      provider_id="$(jq -er '.resourceId' <<<"${response}")"
    fi
    printf '%s' "${provider_id}" \
      >"${unified_platform_runtime_dir}/mcp-provider-${code}.id"
    chmod 600 "${unified_platform_runtime_dir}/mcp-provider-${code}.id"
  done < <(jq -c '.remoteProviders[]' "${release_fixture}")
}

ensure_remote_mounts() {
  local group_id="$1" server_id="$2" item code namespace provider_id mounts
  local mount_id revision response body
  while IFS= read -r item; do
    code="$(jq -er '.providerCode' <<<"${item}")"
    namespace="$(jq -er '.namespace' <<<"${item}")"
    provider_id="$(<"${unified_platform_runtime_dir}/mcp-provider-${code}.id")"
    mounts="$(gateway_api GET \
      "/api/v1/gateway/admin/mcp/remote/mounts?gatewayGroupId=${group_id}")"
    mount_id="$(jq -r --arg namespace "${namespace}" --arg server "${server_id}" \
      '.[] | select(.namespace == $namespace and .serverId == $server) | .id' \
      <<<"${mounts}" | head -1)"
    if [[ -z "${mount_id}" ]]; then
      revision="$(draft_revision "${group_id}")"
      body="$(jq -cn --arg group "${group_id}" --arg server "${server_id}" \
        --arg provider "${provider_id}" --argjson item "${item}" \
        --argjson draft "${revision}" \
        '$item | del(.providerCode) | . + {gatewayGroupId:$group,serverId:$server,providerId:$provider,expectedRevision:0,expectedDraftRevision:$draft,changeReason:("Mount host-local Remote namespace " + .namespace)}')"
      response="$(gateway_api POST /api/v1/gateway/admin/mcp/remote/mounts \
        "${body}" "unified-local-mount-${namespace}-v1")"
      mount_id="$(jq -er '.resourceId' <<<"${response}")"
    fi
    printf '%s' "${mount_id}" \
      >"${unified_platform_runtime_dir}/mcp-mount-${namespace}.id"
    chmod 600 "${unified_platform_runtime_dir}/mcp-mount-${namespace}.id"
  done < <(jq -c '.remoteMounts[]' "${release_fixture}")
}

ensure_app_artifact() {
  local group_id="$1" artifacts artifact artifact_id revision artifact_file
  local response_file status
  artifacts="$(gateway_api GET \
    "/api/v1/gateway/admin/mcp/apps/artifacts?gatewayGroupId=${group_id}")"
  artifact="$(jq -c --arg code "$(jq -r '.artifact.appCode' "${release_fixture}")" \
    --arg version "$(jq -r '.artifact.version' "${release_fixture}")" \
    '.[] | select(.appCode == $code and .version == $version)' \
    <<<"${artifacts}" | head -1)"
  artifact_id=""
  if [[ -n "${artifact}" ]]; then
    artifact_id="$(jq -r '.id // empty' <<<"${artifact}")"
  fi
  artifact_file="${unified_platform_runtime_dir}/unified-local-dashboard.html"
  jq -r '.artifact.content' "${release_fixture}" >"${artifact_file}"
  chmod 600 "${artifact_file}"
  if [[ -z "${artifact_id}" ]]; then
    revision="$(draft_revision "${group_id}")"
    response_file="$(mktemp "${unified_platform_runtime_dir}/artifact-api.XXXXXX")"
    status="$(curl --max-time 30 -sS -o "${response_file}" -w '%{http_code}' \
      -H "Authorization: Bearer $(<"${gateway_admin_token_file}")" \
      -H 'Idempotency-Key: unified-local-artifact-v1' \
      --form-string "gatewayGroupId=${group_id}" \
      --form-string "appCode=$(jq -r '.artifact.appCode' "${release_fixture}")" \
      --form-string "version=$(jq -r '.artifact.version' "${release_fixture}")" \
      --form-string "displayName=$(jq -r '.artifact.displayName' "${release_fixture}")" \
      --form-string "resourceUri=$(jq -r '.artifact.resourceUri' "${release_fixture}")" \
      --form-string "mimeType=$(jq -r '.artifact.mimeType' "${release_fixture}")" \
      --form-string "contentSecurityPolicy=$(jq -r '.artifact.contentSecurityPolicy' "${release_fixture}")" \
      --form-string "permissions=$(jq -r '.artifact.permissions[0]' "${release_fixture}")" \
      --form-string 'expectedRevision=0' \
      --form-string "expectedDraftRevision=${revision}" \
      --form-string 'changeReason=Upload host-local MCP App artifact' \
      -F "artifact=@${artifact_file};type=text/html" \
      "${GATEWAY_ADMIN_BASE_URL}/api/v1/gateway/admin/mcp/apps/artifacts/upload")"
    if [[ ! "${status}" =~ ^2[0-9][0-9]$ ]]; then
      unified_platform_fail \
        "MCP App artifact upload failed with HTTP ${status}: $(<"${response_file}")"
    fi
    artifact_id="$(jq -er '.resourceId' "${response_file}")"
    rm -f "${response_file}"
  else
    restore_app_artifact "${artifact}" "${artifact_file}"
  fi
  printf '%s' "${artifact_id}" >"${unified_platform_runtime_dir}/mcp-artifact.id"
  chmod 600 "${unified_platform_runtime_dir}/mcp-artifact.id"
}

restore_app_artifact() {
  local artifact="$1" source_file="$2" reference expected_reference
  local expected_sha expected_size actual_sha actual_size target directory temporary
  local app_code version
  app_code="$(jq -r '.artifact.appCode' "${release_fixture}")"
  version="$(jq -r '.artifact.version' "${release_fixture}")"
  [[ "${app_code}" =~ ^[A-Za-z0-9._-]+$ && "${version}" =~ ^[A-Za-z0-9._-]+$ ]] \
    || unified_platform_fail "MCP App fixture contains an unsafe artifact path"
  expected_reference="apps/${app_code}/${version}/index.html"
  reference="$(jq -r '.artifactReference // empty' <<<"${artifact}")"
  expected_sha="$(jq -r '.sha256 // empty' <<<"${artifact}")"
  expected_size="$(jq -r '.sizeBytes // empty' <<<"${artifact}")"
  actual_sha="$(openssl dgst -sha256 -r "${source_file}" | awk '{print $1}')"
  actual_size="$(wc -c <"${source_file}" | tr -d '[:space:]')"
  [[ "${reference}" == "${expected_reference}" \
      && "${expected_sha}" == "${actual_sha}" \
      && "${expected_size}" == "${actual_size}" ]] \
    || unified_platform_fail \
      "stored MCP App metadata does not match the host-local fixture"

  directory="${unified_platform_runtime_dir}/mcp-artifacts/apps/${app_code}/${version}"
  target="${directory}/index.html"
  [[ ! -L "${unified_platform_runtime_dir}/mcp-artifacts" \
      && ! -L "${unified_platform_runtime_dir}/mcp-artifacts/apps" \
      && ! -L "${unified_platform_runtime_dir}/mcp-artifacts/apps/${app_code}" \
      && ! -L "${directory}" \
      && ! -L "${target}" ]] \
    || unified_platform_fail "MCP App artifact path contains a symlink"
  mkdir -p "${directory}"
  chmod 700 \
    "${unified_platform_runtime_dir}/mcp-artifacts/apps" \
    "${unified_platform_runtime_dir}/mcp-artifacts/apps/${app_code}" \
    "${directory}"
  if [[ -f "${target}" ]]; then
    actual_sha="$(openssl dgst -sha256 -r "${target}" | awk '{print $1}')"
    actual_size="$(wc -c <"${target}" | tr -d '[:space:]')"
    [[ "${actual_sha}" == "${expected_sha}" \
        && "${actual_size}" == "${expected_size}" ]] \
      || unified_platform_fail "stored MCP App artifact is immutable"
    return
  fi
  temporary="$(mktemp "${directory}/.index.html.XXXXXX")"
  cp "${source_file}" "${temporary}"
  chmod 600 "${temporary}"
  mv "${temporary}" "${target}"
}

resolved_capability_content() {
  local item="$1" content namespace artifact_code resolved
  content="$(jq -c '.content' <<<"${item}")"
  namespace="$(jq -r '.remoteMountNamespace // empty' <<<"${content}")"
  if [[ -n "${namespace}" ]]; then
    resolved="$(<"${unified_platform_runtime_dir}/mcp-mount-${namespace}.id")"
    content="$(jq -c --arg id "${resolved}" \
      'del(.remoteMountNamespace) | .remoteMountId = $id' <<<"${content}")"
  fi
  artifact_code="$(jq -r '.appArtifactCode // empty' <<<"${content}")"
  if [[ -n "${artifact_code}" ]]; then
    resolved="$(<"${unified_platform_runtime_dir}/mcp-artifact.id")"
    content="$(jq -c --arg id "${resolved}" \
      'del(.appArtifactCode) | .appArtifactId = $id' <<<"${content}")"
  fi
  printf '%s' "${content}"
}

ensure_remote_tools() {
  local group_id="$1" server_id="$2" item name enabled existing existing_item
  local tool_id tool_revision content desired current revision body response
  existing="$(gateway_api GET \
    "/api/v1/gateway/admin/mcp/remote-tools?gatewayGroupId=${group_id}&serverId=${server_id}")"
  while IFS= read -r item; do
    name="$(jq -er '.name' <<<"${item}")"
    enabled="$(jq -er '.enabled' <<<"${item}")"
    existing_item="$(jq -c --arg name "${name}" \
      '.[] | select(.name == $name)' <<<"${existing}" | head -1)"
    content="$(resolved_capability_content "${item}")"
    desired="$(jq -cn --argjson content "${content}" \
      --argjson enabled "${enabled}" \
      '$content | {
        description:(.description // null),
        remoteMountId,
        inputSchema:(.inputSchema // null),
        outputSchema:(.outputSchema // null),
        annotations:(.annotations // {}),
        requiredPermissions:((.requiredPermissions // []) | sort),
        riskLevel:(.riskLevel // "LOW"),
        idempotent:(.idempotent // false),
        enabled:$enabled
      }')"
    revision="$(draft_revision "${group_id}")"
    if [[ -n "${existing_item}" ]]; then
      current="$(jq -c '{
        description:(.description // null),
        remoteMountId,
        inputSchema:(.inputSchema // null),
        outputSchema:(.outputSchema // null),
        annotations:(.annotations // {}),
        requiredPermissions:((.requiredPermissions // []) | sort),
        riskLevel,
        idempotent,
        enabled
      }' <<<"${existing_item}")"
      if [[ "${current}" == "${desired}" ]]; then
        continue
      fi
      tool_id="$(jq -er '.id' <<<"${existing_item}")"
      tool_revision="$(jq -er '.revision' <<<"${existing_item}")"
      body="$(jq -cn --arg group "${group_id}" --arg server "${server_id}" \
        --arg name "${name}" --argjson content "${content}" \
        --argjson enabled "${enabled}" --argjson expected "${tool_revision}" \
        --argjson draft "${revision}" \
        '$content + {gatewayGroupId:$group,serverId:$server,name:$name,enabled:$enabled,expectedRevision:$expected,expectedDraftRevision:$draft,changeReason:("Reconcile host-local Remote MCP Tool " + $name)}')"
      response="$(gateway_api PUT \
        "/api/v1/gateway/admin/mcp/remote-tools/${tool_id}" \
        "${body}" "unified-local-remote-tool-${name}-update-${revision}")"
      jq -e '.resourceId != null' <<<"${response}" >/dev/null
      continue
    fi
    body="$(jq -cn --arg group "${group_id}" --arg server "${server_id}" \
      --arg name "${name}" --argjson content "${content}" \
      --argjson enabled "${enabled}" --argjson draft "${revision}" \
      '$content + {gatewayGroupId:$group,serverId:$server,name:$name,enabled:$enabled,expectedRevision:0,expectedDraftRevision:$draft,changeReason:("Create host-local Remote MCP Tool " + $name)}')"
    response="$(gateway_api POST /api/v1/gateway/admin/mcp/remote-tools \
      "${body}" "unified-local-remote-tool-${name}-v1")"
    jq -e '.resourceId != null' <<<"${response}" >/dev/null
  done < <(jq -c '.remoteTools[]' "${release_fixture}")
}

ensure_capabilities() {
  local group_id="$1" server_id="$2" item plural name enabled existing
  local existing_item capability_id capability_revision content revision body response
  while IFS= read -r item; do
    plural="$(jq -er '.plural' <<<"${item}")"
    name="$(jq -er '.name' <<<"${item}")"
    enabled="$(jq -er '.enabled' <<<"${item}")"
    existing="$(gateway_api GET \
      "/api/v1/gateway/admin/mcp/servers/${server_id}/${plural}?gatewayGroupId=${group_id}")"
    existing_item="$(jq -c --arg name "${name}" \
      '.[] | select(.name == $name)' <<<"${existing}" | head -1)"
    content="$(resolved_capability_content "${item}")"
    revision="$(draft_revision "${group_id}")"
    if [[ -n "${existing_item}" ]]; then
      if jq -e --argjson content "${content}" --argjson enabled "${enabled}" \
          '.content == $content and .enabled == $enabled' \
          <<<"${existing_item}" >/dev/null; then
        continue
      fi
      capability_id="$(jq -er '.id' <<<"${existing_item}")"
      capability_revision="$(jq -er '.revision' <<<"${existing_item}")"
      body="$(jq -cn --arg group "${group_id}" --arg server "${server_id}" \
        --arg name "${name}" --argjson content "${content}" \
        --argjson enabled "${enabled}" \
        --argjson expected "${capability_revision}" \
        --argjson draft "${revision}" \
        '{gatewayGroupId:$group,serverId:$server,name:$name,content:$content,enabled:$enabled,expectedRevision:$expected,expectedDraftRevision:$draft,changeReason:("Reconcile host-local MCP capability " + $name)}')"
      response="$(gateway_api PUT \
        "/api/v1/gateway/admin/mcp/${plural}/${capability_id}" \
        "${body}" "unified-local-${plural}-${name}-update-${revision}")"
      jq -e '.resourceId != null' <<<"${response}" >/dev/null
      continue
    fi
    body="$(jq -cn --arg group "${group_id}" --arg name "${name}" \
      --argjson content "${content}" --argjson enabled "${enabled}" \
      --argjson draft "${revision}" \
      '{gatewayGroupId:$group,name:$name,content:$content,enabled:$enabled,expectedRevision:0,expectedDraftRevision:$draft,changeReason:("Create host-local MCP capability " + $name)}')"
    response="$(gateway_api POST \
      "/api/v1/gateway/admin/mcp/servers/${server_id}/${plural}" \
      "${body}" "unified-local-${plural}-${name}-v1")"
    jq -e '.resourceId != null' <<<"${response}" >/dev/null
  done < <(jq -c '.capabilities[]' "${release_fixture}")
}

publish_mcp_release() {
  local group_id="$1" server_id="$2" validation revision release
  validation="$(gateway_api POST \
    "/api/v1/gateway/admin/mcp/servers/${server_id}/validate" '{}')"
  jq -e '.valid == true' <<<"${validation}" >/dev/null \
    || unified_platform_fail "MCP draft validation failed: ${validation}"
  revision="$(draft_revision "${group_id}")"
  release="$(gateway_api POST \
    "/api/v1/gateway/admin/gateway-groups/${group_id}/releases" \
    "$(jq -cn --argjson revision "${revision}" \
      '{expectedDraftRevision:$revision,changeReason:"Publish host-local unified identity and MCP release"}')")"
  jq -e '.status == "SUCCESS"' <<<"${release}" >/dev/null \
    || unified_platform_fail "unified MCP release did not succeed: ${release}"
  jq -er '.releaseId' <<<"${release}" \
    >"${unified_platform_runtime_dir}/mcp-release.id"
  chmod 600 "${unified_platform_runtime_dir}/mcp-release.id"
}

wait_mcp_endpoint() {
  local name="$1" url="$2" status
  for ((attempt = 1; attempt <= 60; attempt++)); do
    status="$(curl --max-time 3 -sS -o /dev/null -w '%{http_code}' \
      "${url}/mcp/unified-local" 2>/dev/null || true)"
    if [[ "${status}" == "401" ]]; then
      return
    fi
    sleep 1
  done
  unified_platform_fail "${name} did not activate the unified MCP release"
}

start_admin_web() {
  local name="$1" web_dir="$2" vite="$3" web_url="$4"
  local client_id="$5" proxy_name="$6" proxy_url="$7" port resource
  if unified_platform_process_running "${name}"; then
    return
  fi
  [[ -x "${vite}" ]] \
    || unified_platform_fail \
      "${name} dependencies are missing; run npm install in ${web_dir}"
  port="${web_url##*:}"
  case "${client_id}" in
    idp-admin-web)
      resource=https://api.egon.internal/local/permission/idp
      ;;
    rbac3-admin-web)
      resource=https://api.egon.internal/local/permission/rbac3
      ;;
    gateway-admin-web)
      resource=https://api.egon.internal/local/platform/gateway-admin
      ;;
    ddc-admin-web)
      resource=https://api.egon.internal/local/platform/ddc
      ;;
    *) unified_platform_fail "unknown Admin OAuth client: ${client_id}" ;;
  esac
  (
    cd "${web_dir}"
    export "${proxy_name}=${proxy_url}"
    export VITE_IDP_ISSUER="${IDP_BASE_URL}"
    export VITE_IDP_CLIENT_ID="${client_id}"
    export VITE_IDP_RESOURCE="${resource}"
    export VITE_IDP_REDIRECT_URI="${web_url}/oauth/callback"
    export VITE_DEFAULT_TENANT_ID="${default_tenant_id}"
    exec nohup "${vite}" \
      --config "${web_dir}/vite.config.ts" \
      --host 127.0.0.1 --port "${port}" --strictPort
  ) >"${unified_platform_log_dir}/${name}.log" 2>&1 </dev/null &
  printf '%s' "$!" >"${unified_platform_pid_dir}/${name}.pid"
  chmod 600 "${unified_platform_pid_dir}/${name}.pid"
  unified_platform_wait_http "${name}" "${web_url}/"
}

unified_platform_stage "preparing and starting IdP, RBAC3, DDC, Gateway A and mock backend"
"${legacy_script}" start
"${legacy_script}" refresh-tokens
prepare_admin_web_login_environments

package_mcp_fixtures
write_extra_service_env_files

unified_platform_stage "starting Stable and RC Remote MCP fixture"
unified_platform_start_jar mcp-remote \
  "${unified_platform_env_dir}/mcp-remote.env" "${mcp_remote_jar}"
unified_platform_wait_http mcp-remote \
  "${MCP_REMOTE_BASE_URL}/actuator/health/readiness"

unified_platform_stage "starting Gateway Engine B"
unified_platform_start_jar gateway-engine-b \
  "${unified_platform_env_dir}/gateway-engine-b.env" "${gateway_engine_jar}"
unified_platform_wait_http gateway-engine-b \
  "${GATEWAY_ENGINE_B_BASE_URL}/actuator/health/readiness"

unified_platform_stage "registering and starting local MCP Operation provider"
initialize_mcp_provider_application
unified_platform_start_jar mcp-provider \
  "${unified_platform_env_dir}/mcp-provider.env" "${mcp_provider_jar}"
unified_platform_wait_http mcp-provider \
  "${MCP_PROVIDER_BASE_URL}/actuator/health/readiness"
wait_mcp_provider_catalog
ensure_mcp_user_delegation

group_id="$(<"${gateway_group_file}")"
ensure_mcp_server "${group_id}"
server_id="$(<"${unified_platform_runtime_dir}/mcp-server.id")"
  ensure_remote_providers "${group_id}"
  ensure_remote_mounts "${group_id}" "${server_id}"
  ensure_app_artifact "${group_id}"
  ensure_remote_tools "${group_id}" "${server_id}"
  ensure_capabilities "${group_id}" "${server_id}"

unified_platform_stage "publishing one unified HTTP and MCP release"
publish_mcp_release "${group_id}" "${server_id}"
wait_mcp_endpoint gateway-engine-a "${GATEWAY_BASE_URL}"
wait_mcp_endpoint gateway-engine-b "${GATEWAY_ENGINE_B_PUBLIC_URL}"

unified_platform_stage "starting four Admin Web applications"
start_admin_web idp-admin-web "${idp_web_dir}" \
  "${idp_web_dir}/node_modules/.bin/vite" "${IDP_ADMIN_WEB_URL}" \
  idp-admin-web IDP_ADMIN_PROXY "${IDP_BASE_URL}"
start_admin_web rbac3-admin-web "${rbac3_web_dir}" \
  "${rbac3_root_dir}/node_modules/.bin/vite" "${RBAC3_ADMIN_WEB_URL}" \
  rbac3-admin-web RBAC3_ADMIN_PROXY "${RBAC3_BASE_URL}"
start_admin_web gateway-admin-web "${gateway_web_dir}" \
  "${gateway_web_dir}/node_modules/.bin/vite" "${GATEWAY_ADMIN_WEB_URL}" \
  gateway-admin-web GATEWAY_ADMIN_PROXY "${GATEWAY_ADMIN_BASE_URL}"
start_admin_web ddc-admin-web "${ddc_web_dir}" \
  "${ddc_web_dir}/node_modules/.bin/vite" "${DDC_ADMIN_WEB_URL}" \
  ddc-admin-web DDC_ADMIN_PROXY "${DDC_BASE_URL}"

printf 'Unified platform local stack is running in %s.\n' \
  "${unified_platform_runtime_dir}"
"${script_dir}/status-local-stack.sh"
