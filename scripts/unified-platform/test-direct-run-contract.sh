#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"
identity_script="${repo_root}/scripts/unified-identity-local.sh"
platform_start_script="${repo_root}/scripts/unified-platform/start-local-stack.sh"
platform_common_script="${repo_root}/scripts/unified-platform/lib/common.sh"
platform_verify_script="${repo_root}/scripts/unified-platform/verify-local-stack.sh"
release_fixture="${repo_root}/scripts/unified-platform/fixtures/unified-platform-release.json"

fail() {
  printf 'direct-run-contract: %s\n' "$*" >&2
  exit 1
}

assert_contains() {
  local file="$1" expected="$2" context="$3"
  grep -Fq -- "${expected}" "${file}" \
    || fail "${context}: missing ${expected}"
}

assert_not_contains() {
  local file="$1" unexpected="$2" context="$3"
  if grep -Fq -- "${unexpected}" "${file}"; then
    fail "${context}: found ${unexpected}"
  fi
}

extract_function() {
  local name="$1" output="$2"
  awk -v signature="${name}()" '
    $0 == signature " {" {copying = 1}
    copying {print}
    copying && $0 == "}" {exit}
  ' "${identity_script}" >"${output}"
  [[ -s "${output}" ]] || fail "${name} function is missing"
}

temporary_dir="$(mktemp -d "${TMPDIR:-/tmp}/egon-direct-run-contract.XXXXXX")"
trap 'rm -rf "${temporary_dir}"' EXIT

function_file="${temporary_dir}/local-build-id.sh"
extract_function local_build_id "${function_file}"
# shellcheck disable=SC1090
source "${function_file}"
printf '%s' 'deterministic-local-build' >"${temporary_dir}/local-build.jar"
expected_build_id="local-$(openssl dgst -sha256 -r \
  "${temporary_dir}/local-build.jar" | awk '{print substr($1, 1, 16)}')"
[[ "$(local_build_id "${temporary_dir}/local-build.jar")" \
    == "${expected_build_id}" ]] \
  || fail 'local build IDs must be derived from executable JAR content'

function_file="${temporary_dir}/properties-escape.sh"
extract_function properties_escape "${function_file}"
# shellcheck disable=SC1090
source "${function_file}"

escaped="$(properties_escape $'a\\b\tc\rd\ne')"
[[ "${escaped}" == 'a\\b\tc\rd\ne' ]] \
  || fail "properties_escape did not encode Java properties control characters"

function_file="${temporary_dir}/java-property-key.sh"
extract_function java_property_key "${function_file}"
# shellcheck disable=SC1090
source "${function_file}"
[[ "$(java_property_key SERVER_PORT)" == 'server.port' ]] \
  || fail 'SERVER_PORT must become a Spring property key'
[[ "$(java_property_key SPRING_DATASOURCE_URL)" == 'spring.datasource.url' ]] \
  || fail 'SPRING_DATASOURCE_URL must become a Spring property key'
[[ "$(java_property_key EGON_COLA_PLATFORM_RBAC3_RUNTIME_PASSWORD_FILE)" \
    == 'egon.cola.platform.rbac3.runtime.password-file' ]] \
  || fail 'RBAC3 runtime password file must use its canonical property key'
[[ "$(java_property_key GATEWAY_ADMIN_DDC_ENABLED)" \
    == 'gateway.admin.ddc.enabled' ]] \
  || fail 'Gateway DDC enablement must use its canonical property key'
[[ "$(java_property_key GATEWAY_ADMIN_SECRETS_MASTER_KEY_BASE64)" \
    == 'gateway.admin.secrets.master-key-base64' ]] \
  || fail 'Gateway secret protection must use its canonical property key'
[[ "$(java_property_key EGON_COLA_COMPONENT_GATEWAY_PROVIDER_HTTP_FAIL_FAST)" \
    == 'egon.cola.component.ddc.registry.http.fail-fast' ]] \
  || fail 'Gateway Provider fail-fast must use its canonical property key'
[[ "$(java_property_key EGON_COLA_COMPONENT_DDC_CONSISTENCY_FAIL_FAST)" \
    == 'egon.cola.component.ddc.consistency.fail-fast' ]] \
  || fail 'DDC consistency fail-fast must use its canonical property key'
[[ "$(java_property_key EGON_COLA_COMPONENT_GATEWAY_ENGINE_HTTP_PUBLIC_PORT)" \
    == 'egon.cola.component.gateway.engine.http.public-port' ]] \
  || fail 'Gateway public listener must use its nested canonical property key'
[[ "$(java_property_key EGON_COLA_COMPONENT_GATEWAY_ENGINE_HTTP_INTERNAL_PORT)" \
    == 'egon.cola.component.gateway.engine.http.internal-port' ]] \
  || fail 'Gateway internal listener must use its nested canonical property key'

function_file="${temporary_dir}/postgres-password.sh"
extract_function postgres_password "${function_file}"
# shellcheck disable=SC1090
source "${function_file}"
secret_dir="${temporary_dir}/secrets"
mkdir -p "${secret_dir}"
postgres_password_file=
unset UNIFIED_IDENTITY_POSTGRES_PASSWORD
if (postgres_password) >/dev/null 2>&1; then
  fail 'PostgreSQL password resolution must not guess a default password'
fi
printf '%s' 'local-runtime-password' >"${secret_dir}/postgres.password"
[[ "$(postgres_password)" == 'local-runtime-password' ]] \
  || fail 'PostgreSQL password resolution must reuse the protected runtime secret'

function_file="${temporary_dir}/resolve-postgres-password.sh"
extract_function resolve_postgres_password "${function_file}"
# shellcheck disable=SC1090
source "${function_file}"
printf '%s' 'explicit-runtime-password' \
  >"${temporary_dir}/explicit-postgres.password"
postgres_password_file="${temporary_dir}/explicit-postgres.password"
postgres_host=127.0.0.1
postgres_port=5432
postgres_user=postgres
postgres_database=postgres
psql() {
  [[ "${PGPASSWORD:-}" == 'explicit-runtime-password' ]]
}
resolve_postgres_password
[[ "$(<"${secret_dir}/postgres.password")" \
    == 'explicit-runtime-password' ]] \
  || fail 'explicit PostgreSQL credential must be persisted in the protected runtime'
[[ "$(stat -f '%Lp' "${secret_dir}/postgres.password")" == '600' ]] \
  || fail 'persisted PostgreSQL credential must have mode 600'
printf '%s' 'rejected-runtime-password' \
  >"${temporary_dir}/rejected-postgres.password"
postgres_password_file="${temporary_dir}/rejected-postgres.password"
if (resolve_postgres_password) >/dev/null 2>&1; then
  fail 'invalid PostgreSQL credential must be rejected before persistence'
fi
[[ "$(<"${secret_dir}/postgres.password")" \
    == 'explicit-runtime-password' ]] \
  || fail 'invalid PostgreSQL credential replaced the last known-good runtime secret'
[[ "$(stat -f '%Lp' "${secret_dir}/postgres.password")" == '600' ]] \
  || fail 'rejected PostgreSQL credential changed runtime secret permissions'
unset -f psql

function_file="${temporary_dir}/resolve-existing-service-tenant-id.sh"
extract_function resolve_existing_service_tenant_id "${function_file}"
# shellcheck disable=SC1090
source "${function_file}"
rbac3_database=rbac3-test
service_tenant_id=default
database_table_exists() {
  [[ "$1" == 'rbac3-test' && "$2" == 'public.rbac3_tenant' ]]
}
rbac3_tenant_id() {
  [[ "$1" == 'default' ]] || return 1
  printf '%s' '42001'
}
resolve_existing_service_tenant_id
[[ "${service_tenant_id}" == '42001' ]] \
  || fail 'prepare must restore the numeric ID of an existing service tenant'
service_tenant_id=73001
rbac3_tenant_id() {
  return 1
}
resolve_existing_service_tenant_id
[[ "${service_tenant_id}" == '73001' ]] \
  || fail 'an explicit numeric service tenant ID must be preserved'
unset -f database_table_exists rbac3_tenant_id

jq -e '
  .server.resourceUri == "https://api.egon.internal/local/identity/gateway-test-mcp-provider"
  and (.server | has("oauthAudience") | not)
' "${release_fixture}" >/dev/null \
  || fail 'MCP Server fixture must use the exact OAuth Resource URI contract'
assert_contains "${platform_start_script}" 'ensure_mcp_user_delegation' \
  'local MCP startup must explicitly grant its exact Resource to the OAuth client'
assert_contains "${platform_start_script}" 'mcp-tenant-b.access.jwt' \
  'local MCP startup must issue a token for the exact provider Resource'
assert_contains "${platform_verify_script}" \
  'mcp_token_file="${unified_platform_secret_dir}/mcp-tenant-b.access.jwt"' \
  'MCP verification must not reuse the mock backend Resource token'
assert_contains "${platform_verify_script}" 'run_identity issue-user-token' \
  'MCP verification must refresh its Resource token after identity revocation checks'
assert_contains "${platform_verify_script}" \
  'UNIFIED_IDENTITY_IDP_DATABASE="${identity_idp_database}"' \
  'platform verification must use the IdP database recorded by the running stack'
assert_contains "${platform_verify_script}" \
  'UNIFIED_IDENTITY_RBAC3_DATABASE="${identity_rbac3_database}"' \
  'platform verification must use the RBAC3 database recorded by the running stack'
assert_contains "${platform_verify_script}" \
  'UNIFIED_IDENTITY_GATEWAY_DATABASE="${identity_gateway_database}"' \
  'platform verification must use the Gateway database recorded by the running stack'
assert_contains "${platform_verify_script}" \
  'UNIFIED_IDENTITY_DDC_DATABASE="${identity_ddc_database}"' \
  'platform verification must use the DDC database recorded by the running stack'
assert_not_contains "${platform_verify_script}" \
  'Authorization: Bearer $(<"${tenant_token_file}")' \
  'MCP verification must use the exact provider Resource token for every transport'
assert_contains "${platform_verify_script}" \
  '"local_echo_task","arguments":{"body":{"value":"task"}}' \
  'MCP task verification must preserve the Gateway Operation body location'

assert_env_equals() {
  local file="$1" key="$2" expected="$3" context="$4" actual
  actual="$(bash -c '
    set -a
    # shellcheck disable=SC1090
    source "$1"
    printf "%s" "${!2-}"
  ' _ "${file}" "${key}")"
  [[ "${actual}" == "${expected}" ]] \
    || fail "${context}: expected ${key}=${expected}, got ${actual:-<unset>}"
}

generated_runtime="${temporary_dir}/generated-runtime"
(
  export UNIFIED_IDENTITY_RUNTIME_DIR="${generated_runtime}"
  # shellcheck disable=SC1090
  source "${identity_script}" help >/dev/null
  initialize_directories
  printf '%s' 'test-redis-password' >"${secret_dir}/redis.password"
  printf '%s' 'test-ddc-runtime-access-key' >"${secret_dir}/ddc-runtime.access-key"
  printf '%s' 'test-ddc-runtime-secret' >"${secret_dir}/ddc-runtime.secret"
  printf '%s' 'test-ddc-registry-access-key' >"${secret_dir}/ddc-registry.access-key"
  printf '%s' 'test-ddc-registry-secret' >"${secret_dir}/ddc-registry.secret"
  printf '%s' 'test-ddc-management-access-key' >"${secret_dir}/ddc-management.access-key"
  printf '%s' 'test-ddc-management-secret' >"${secret_dir}/ddc-management.secret"
  printf '%s' 'test-gateway-master-key' >"${secret_dir}/gateway-master-key.base64"
  postgres_password() {
    printf '%s' 'test-postgres-password'
  }
  write_service_env_files
)

idp_env="${generated_runtime}/env/idp.env"
assert_env_equals "${idp_env}" IDP_DDC_ENABLED true \
  'local IdP must start its DDC config client'
assert_env_equals "${idp_env}" IDP_HTTP_PROVIDER_ENABLED true \
  'local IdP must publish its HTTP Provider lease'
assert_env_equals "${idp_env}" \
  EGON_COLA_COMPONENT_GATEWAY_PROVIDER_HTTP_FAIL_FAST false \
  'local IdP must recover until its DDC scope binding is initialized'
assert_env_equals "${idp_env}" DDC_BIZ_CODE permission \
  'local IdP must use its Resource business scope'
assert_env_equals "${idp_env}" DDC_APP_CODE idp \
  'local IdP must use its Resource application scope'
assert_env_equals "${idp_env}" \
  EGON_COLA_COMPONENT_DDC_CONSISTENCY_FAIL_FAST false \
  'local IdP must recover until its DDC topology exists'
assert_env_equals "${idp_env}" DEPLOYMENT_ENV local \
  'local IdP must register in the local environment'
assert_env_equals "${idp_env}" DEPLOYMENT_NAMESPACE default \
  'local IdP must use the default visibility namespace'
assert_env_equals "${idp_env}" IDP_INSTANCE_ID idp-local-1 \
  'local IdP must use a stable lease identity'
assert_env_equals "${idp_env}" IDP_RESOURCE_SERVER_ID \
  permission-idp-local \
  'local IdP must validate tokens for its exact Resource Server'
assert_env_equals "${idp_env}" IDP_RESOURCE_URI \
  https://api.egon.internal/local/permission/idp \
  'local IdP must validate one exact Resource URI'
assert_env_equals "${idp_env}" IDP_RESOURCE_MANAGEMENT_CLIENT_ID \
  idp-service \
  'local IdP must request admission with its service client'
assert_env_equals "${idp_env}" IDP_RESOURCE_MANAGEMENT_KEY_ID \
  idp-local \
  'local IdP must identify its admission signing key'
assert_env_equals "${idp_env}" IDP_RESOURCE_MANAGEMENT_PRIVATE_KEY_FILE \
  "${generated_runtime}/secrets/idp-private.pem" \
  'local IdP must sign admission assertions with its protected private key'
assert_env_equals "${idp_env}" IDP_RESOURCE_ADMISSION_RPC_TARGET \
  dns:///127.0.0.1:18122 \
  'local IdP must use the static IdP admission RPC target'
assert_env_equals "${idp_env}" IDP_RPC_PORT 18122 \
  'local IdP must expose the internal admission RPC provider'
assert_env_equals "${idp_env}" IDP_RBAC3_SERVICE_CLIENT_ID idp-service \
  'local IdP must call RBAC3 with its confidential service client'
assert_env_equals "${idp_env}" IDP_RBAC3_SERVICE_KEY_ID idp-local \
  'local IdP must identify its service assertion key'
assert_env_equals "${idp_env}" IDP_RBAC3_SERVICE_PRIVATE_KEY_FILE \
  "${generated_runtime}/secrets/idp-private.pem" \
  'local IdP must sign service assertions with its protected private key'
assert_env_equals "${idp_env}" IDP_RBAC3_RESOURCE_URI \
  https://api.egon.internal/local/permission/rbac3 \
  'local IdP must request a token for the exact RBAC3 Resource'
assert_env_equals "${idp_env}" IDP_RBAC3_SERVICE_TENANT_ID default \
  'local IdP service calls must bind one exact tenant'
assert_env_equals "${idp_env}" IDP_RBAC3_SERVICE_SCOPES \
  'service:authorization:decide service:authorization:snapshot service:identity:resolve' \
  'local IdP service calls must request only reviewed RBAC3 scopes'
assert_env_equals "${idp_env}" DDC_REGISTRY_REDIS_DATABASE 10 \
  'local IdP must use the DDC Registry Redis database'
assert_env_equals "${idp_env}" DDC_RPC_TARGET dns:///127.0.0.1:19080 \
  'local IdP must bootstrap DDC through direct RPC'
assert_env_equals "${idp_env}" DDC_RPC_RUNTIME_ACCESS_KEY \
  test-ddc-runtime-access-key \
  'local IdP must use the runtime DDC credential'
assert_env_equals "${idp_env}" DDC_RPC_REGISTRY_ACCESS_KEY \
  test-ddc-registry-access-key \
  'local IdP must use the registry DDC credential'

rbac3_env="${generated_runtime}/env/rbac3.env"
assert_env_equals "${rbac3_env}" RBAC3_DDC_ENABLED true \
  'local RBAC3 must start its DDC config client'
assert_env_equals "${rbac3_env}" RBAC3_HTTP_PROVIDER_ENABLED true \
  'local RBAC3 must publish its HTTP Provider lease'
assert_env_equals "${rbac3_env}" \
  EGON_COLA_COMPONENT_GATEWAY_PROVIDER_HTTP_FAIL_FAST false \
  'local RBAC3 must recover until its DDC scope binding is initialized'
assert_env_equals "${rbac3_env}" DDC_BIZ_CODE permission \
  'local RBAC3 must use its Resource business scope'
assert_env_equals "${rbac3_env}" DDC_APP_CODE rbac3 \
  'local RBAC3 must use its Resource application scope'
assert_env_equals "${rbac3_env}" \
  EGON_COLA_COMPONENT_DDC_CONSISTENCY_FAIL_FAST false \
  'local RBAC3 must recover until its DDC topology exists'
assert_env_equals "${rbac3_env}" DEPLOYMENT_ENV local \
  'local RBAC3 must register in the local environment'
assert_env_equals "${rbac3_env}" DEPLOYMENT_NAMESPACE default \
  'local RBAC3 must use the default visibility namespace'
assert_env_equals "${rbac3_env}" RBAC3_INSTANCE_ID rbac3-local-1 \
  'local RBAC3 must use a stable lease identity'
assert_env_equals "${rbac3_env}" RBAC3_ARTIFACT_VERSION local \
  'local RBAC3 service identity must use the local artifact version'
assert_env_equals "${rbac3_env}" \
  RBAC3_AUTHORIZATION_SERVICE_TOKEN_ENABLED true \
  'RBAC3 must acquire internal authorization credentials per target tenant'
assert_env_equals "${rbac3_env}" \
  RBAC3_AUTHORIZATION_SERVICE_TOKEN_CLIENT_ID rbac3-service \
  'RBAC3 tenant-aware credentials must use the approved service Client'
assert_env_equals "${rbac3_env}" RBAC3_RESOURCE_SERVER_ID \
  permission-rbac3-local \
  'local RBAC3 must validate tokens for its exact Resource Server'
assert_env_equals "${rbac3_env}" RBAC3_RESOURCE_URI \
  https://api.egon.internal/local/permission/rbac3 \
  'local RBAC3 must validate one exact Resource URI'
assert_env_equals "${rbac3_env}" RBAC3_RESOURCE_MANAGEMENT_CLIENT_ID \
  rbac3-service \
  'local RBAC3 must request admission with its service client'
assert_env_equals "${rbac3_env}" RBAC3_RESOURCE_MANAGEMENT_KEY_ID \
  rbac3-local \
  'local RBAC3 must identify its admission signing key'
assert_env_equals "${rbac3_env}" \
  RBAC3_RESOURCE_MANAGEMENT_PRIVATE_KEY_FILE \
  "${generated_runtime}/secrets/rbac3-private.pem" \
  'local RBAC3 must sign admission with its protected private key'
assert_env_equals "${rbac3_env}" RBAC3_RESOURCE_ADMISSION_RPC_TARGET \
  dns:///127.0.0.1:18122 \
  'local RBAC3 must use the static IdP admission RPC target'

gateway_admin_env="${generated_runtime}/env/gateway-admin.env"
assert_env_equals "${gateway_admin_env}" DDC_RPC_TARGET \
  dns:///127.0.0.1:19080 \
  'local Gateway Admin must bootstrap DDC through direct RPC'
assert_env_equals "${gateway_admin_env}" DDC_RPC_MANAGEMENT_ACCESS_KEY \
  test-ddc-management-access-key \
  'local Gateway Admin must use the management DDC credential'
assert_env_equals "${gateway_admin_env}" GATEWAY_ADMIN_RESOURCE_SERVER_ID \
  platform-gateway-admin-local \
  'local Gateway Admin must validate its exact Resource Server'
assert_env_equals "${gateway_admin_env}" GATEWAY_ADMIN_RESOURCE_URI \
  https://api.egon.internal/local/platform/gateway-admin \
  'local Gateway Admin must validate one exact Resource URI'
assert_env_equals "${gateway_admin_env}" \
  GATEWAY_ADMIN_RESOURCE_MANAGEMENT_CLIENT_ID gateway-admin-service \
  'local Gateway Admin must request admission with its service client'
assert_env_equals "${gateway_admin_env}" \
  GATEWAY_ADMIN_RESOURCE_MANAGEMENT_KEY_ID gateway-admin-local \
  'local Gateway Admin must identify its admission signing key'
assert_env_equals "${gateway_admin_env}" \
  GATEWAY_ADMIN_RESOURCE_MANAGEMENT_PRIVATE_KEY_FILE \
  "${generated_runtime}/secrets/gateway-admin-private.pem" \
  'local Gateway Admin must sign admission with its protected private key'
assert_env_equals "${gateway_admin_env}" \
  GATEWAY_ADMIN_RESOURCE_ADMISSION_RPC_TARGET \
  dns:///127.0.0.1:18122 \
  'local Gateway Admin must use the static IdP admission RPC target'
assert_contains \
  'egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/resources/application.yml' \
  '          id: ${GATEWAY_ENGINE_DDC_INSTANCE_ID:}' \
  'Gateway Engine DDC client and admission ticket must share one instance id'
assert_contains \
  'egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-idp-backend/src/main/resources/application.yml' \
  '          id: ${MOCK_BACKEND_INSTANCE_ID:mock-backend-local-1}' \
  'mock backend DDC client and admission ticket must share one instance id'
[[ "$(grep -Fc 'service_tenant_id="$(rbac3_tenant_id default)"' \
  "${identity_script}")" == "2" ]] || fail \
  'start and refresh-tokens must both resolve the exact RBAC3 tenant id'
assert_contains "${identity_script}" \
  'resolve_existing_service_tenant_id' \
  'prepare must restore an existing exact RBAC3 tenant id before rewriting env files'
assert_contains "${identity_script}" \
  'tenant_b_id="$(rbac3_tenant_id tenant-b)"' \
  'start must resolve the exact secondary RBAC3 tenant id'
assert_contains "${identity_script}" \
  'IDP_DEVELOPMENT_RBAC3_SERVICE_TENANT_IDS' \
  'IdP bootstrap must receive every exact local RBAC3 service tenant'
assert_contains "${identity_script}" 'MOCK_LOCAL_ENTRY' \
  'the no-admin verification state must preserve mock Resource entry permission'
assert_contains "${identity_script}" \
  '[[ -s "${runtime_dir}/browser.cookies" ]] || oauth_login' \
  'issuing another Resource token must reuse the current browser SSO session'

gateway_engine_env="${generated_runtime}/env/gateway-engine.env"
assert_env_equals "${gateway_engine_env}" \
  GATEWAY_ENGINE_RESOURCE_ADMISSION_RPC_TARGET \
  dns:///127.0.0.1:18122 \
  'Gateway Engine must use the static IdP admission RPC target'
assert_env_equals "${gateway_engine_env}" \
  GATEWAY_MCP_TASK_SERVICE_TOKEN_ENABLED true \
  'Gateway Engine must use a SERVICE identity for durable MCP execution'
assert_env_equals "${gateway_engine_env}" \
  GATEWAY_MCP_TASK_SERVICE_TOKEN_CLIENT_ID gateway-engine-service \
  'durable MCP execution must use the approved Gateway Engine Client'
assert_env_equals "${gateway_engine_env}" \
  GATEWAY_MCP_TASK_SERVICE_TOKEN_PRIVATE_KEY_FILE \
  "${generated_runtime}/secrets/gateway-engine-private.pem" \
  'durable MCP execution must use the protected Gateway Engine key'
assert_env_equals "${gateway_engine_env}" \
  GATEWAY_MCP_TASK_SERVICE_TOKEN_SCOPES mcp:operation:invoke \
  'durable MCP execution must request only the IdP-approved Provider scope'

mock_backend_env="${generated_runtime}/env/mock-backend.env"
assert_env_equals "${mock_backend_env}" \
  MOCK_BACKEND_RESOURCE_ADMISSION_RPC_TARGET \
  dns:///127.0.0.1:18122 \
  'mock backend must use the static IdP admission RPC target'
assert_not_contains "${identity_script}" RESOURCE_ADMISSION_ENDPOINT \
  'local environment generation must not retain HTTP admission endpoints'

ddc_env="${generated_runtime}/env/ddc.env"
assert_env_equals "${ddc_env}" DDC_RPC_PORT 19080 \
  'local DDC Admin must expose the direct RPC provider'
assert_env_equals "${ddc_env}" DDC_RPC_REGISTRY_ACCESS_KEY \
  test-ddc-registry-access-key \
  'local DDC Admin must configure the registry credential profile'
assert_env_equals "${ddc_env}" DDC_RESOURCE_SERVER_ID \
  platform-ddc-local \
  'local DDC Admin must validate tokens for its exact Resource Server'
assert_env_equals "${ddc_env}" DDC_RESOURCE_URI \
  https://api.egon.internal/local/platform/ddc \
  'local DDC Admin must validate one exact Resource URI'
assert_env_equals "${ddc_env}" DDC_ADMIN_JWT_AUDIENCE \
  https://api.egon.internal/local/platform/ddc \
  'local DDC Admin security chain must use the Resource URI as audience'
assert_env_equals "${rbac3_env}" DDC_REGISTRY_REDIS_DATABASE 10 \
  'local RBAC3 must use the DDC Registry Redis database'

while IFS='|' read -r service_env client_id key_id private_key; do
  assert_env_equals "${generated_runtime}/env/${service_env}.env" \
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_ENABLED true \
    "${service_env} must acquire RBAC3 credentials for the exact USER tenant"
  assert_env_equals "${generated_runtime}/env/${service_env}.env" \
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_CLIENT_ID \
    "${client_id}" \
    "${service_env} must use its own approved OAuth service Client"
  assert_env_equals "${generated_runtime}/env/${service_env}.env" \
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_KEY_ID \
    "${key_id}" \
    "${service_env} must identify its own private_key_jwt key"
  assert_env_equals "${generated_runtime}/env/${service_env}.env" \
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_PRIVATE_KEY_FILE \
    "${generated_runtime}/secrets/${private_key}-private.pem" \
    "${service_env} must use its owner-only private key"
done <<'SERVICE_TOKENS'
idp|idp-service|idp-local|idp
gateway-admin|gateway-admin-service|gateway-admin-local|gateway-admin
ddc|ddc-service|ddc-local|ddc
mock-backend|mock-backend-service|mock-backend-local|mock-backend
gateway-engine|gateway-engine-service|gateway-engine-local|gateway-engine
SERVICE_TOKENS

function_file="${temporary_dir}/initialize-ddc-topology.sh"
extract_function initialize_ddc_topology "${function_file}"
# shellcheck disable=SC1090
source "${function_file}"
ddc_topology_calls="${temporary_dir}/ddc-topology-calls.jsonl"
ddc_api() {
  local method="$1" path="$2" body="${3:-null}"
  if [[ "${method}" == "GET" ]]; then
    printf '%s' '{"success":true,"data":[]}'
    return
  fi
  jq -cn --arg method "${method}" --arg path "${path}" \
    --argjson body "${body}" \
    '{method:$method,path:$path,body:$body}' >>"${ddc_topology_calls}"
  printf '%s' '{"success":true,"data":{}}'
}
initialize_ddc_topology
while read -r provider_biz provider_app; do
  jq -e --arg app "${provider_app}" --arg biz "${provider_biz}" '
    select(
      .method == "POST"
      and .path == "/api/v1/ddc/apps"
      and .body.bizCode == $biz
      and .body.appCode == $app
      and .body.enabled == true
    )
  ' "${ddc_topology_calls}" >/dev/null \
    || fail "DDC topology must create the ${provider_app} application"
  jq -e --arg app "${provider_app}" --arg biz "${provider_biz}" '
    select(
      .method == "POST"
      and .path == "/api/v1/ddc/namespace-env-app-bindings"
      and .body.bizCode == $biz
      and .body.namespaceCode == "default"
      and .body.env == "local"
      and .body.appCode == $app
      and .body.enabled == true
    )
  ' "${ddc_topology_calls}" >/dev/null \
    || fail "DDC topology must enable the ${provider_app} scope binding"
done <<'PROVIDERS'
permission idp
permission rbac3
PROVIDERS
unset -f ddc_api initialize_ddc_topology

function_file="${temporary_dir}/wait-ddc-provider-registration.sh"
extract_function wait_ddc_provider_registration "${function_file}"
# shellcheck disable=SC1090
source "${function_file}"
ddc_registry_queries="${temporary_dir}/ddc-registry-queries.txt"
ddc_api() {
  local method="$1" path="$2" attempt
  [[ "${method}" == "GET" ]] \
    || fail 'provider registration wait must only read DDC state'
  printf '%s\n' "${path}" >>"${ddc_registry_queries}"
  attempt="$(wc -l <"${ddc_registry_queries}" | tr -d ' ')"
  if [[ "${attempt}" -eq 1 ]]; then
    printf '%s' '{"success":true,"data":{"services":[]}}'
  else
    printf '%s' \
      '{"success":true,"data":{"services":[{"appCode":"idp","serviceKind":"HTTP_PROVIDER","protocol":"http","serviceName":"idp-admin","group":"default","version":"5.3.2"}]}}'
  fi
}
sleep() {
  :
}
wait_ddc_provider_registration permission idp idp-admin
[[ "$(wc -l <"${ddc_registry_queries}" | tr -d ' ')" -eq 2 ]] \
  || fail 'provider registration wait must poll until the lease is online'
grep -Fq \
  'registry/services?bizCode=permission&namespaceCode=default&env=local&appCode=idp&serviceKind=HTTP_PROVIDER&protocol=http&serviceName=idp-admin&group=default' \
  "${ddc_registry_queries}" \
  || fail 'provider registration wait must query the exact IdP service key'
unset -f ddc_api sleep wait_ddc_provider_registration

# shellcheck source=lib/common.sh
source "${repo_root}/scripts/unified-platform/lib/common.sh"
declare -F unified_platform_write_frontend_login_env >/dev/null \
  || fail 'frontend login environment writer is missing'
frontend_dir="${temporary_dir}/admin-web"
mkdir -p "${frontend_dir}"
unified_platform_write_frontend_login_env \
  "${frontend_dir}" '77351065313480704'
frontend_env="${frontend_dir}/.env.local"
[[ "$(stat -f '%Lp' "${frontend_env}")" == '600' ]] \
  || fail 'generated frontend login environment must have mode 600'
# shellcheck disable=SC1090
source "${frontend_env}"
[[ "${VITE_DEFAULT_TENANT_ID}" == '77351065313480704' ]] \
  || fail 'plain npm run dev must receive the resolvable default tenant ID'
printf '%s\n' 'VITE_CUSTOM_SETTING=preserve-me' >"${frontend_env}"
if (unified_platform_write_frontend_login_env \
    "${frontend_dir}" '77351065313480704') >/dev/null 2>&1; then
  fail 'frontend login environment writer must not overwrite an unmanaged file'
fi
[[ "$(<"${frontend_env}")" == 'VITE_CUSTOM_SETTING=preserve-me' ]] \
  || fail 'unmanaged frontend login environment was modified'

assert_contains "${identity_script}" '${file%.env}.properties' \
  'write_env must target the sibling Java properties file'
assert_contains "${identity_script}" 'properties_escape "${value}"' \
  'write_env must encode the Java properties value'
assert_contains "${identity_script}" 'java_property_key "${key}"' \
  'write_env must translate environment names for Java property sources'
assert_contains "${identity_script}" 'chmod 600 "${file}" "${properties_file}"' \
  'new_env_file must protect both runtime configuration files'
assert_contains "${identity_script}" \
  'write_env "${file}" EGON_COLA_COMPONENT_GATEWAY_PROVIDER_HTTP_FAIL_FAST false' \
  'direct Gateway Engine startup must recover when DDC is still starting'
assert_contains "${identity_script}" \
  'write_env "${file}" EGON_COLA_COMPONENT_DDC_CONSISTENCY_FAIL_FAST false' \
  'direct DDC client startup must reconcile when DDC is still starting'
assert_contains "${identity_script}" \
  'write_env "${file}" RBAC3_DEVELOPMENT_AUTO_ACTIVATE_LOCAL_ADMIN_ROLES true' \
  'local RBAC3 startup must activate the generated local administrator roles'
assert_contains "${identity_script}" '[[ -s "${file}" ]] || return 0' \
  'identity shutdown must tolerate an already stopped process'
assert_contains "${repo_root}/scripts/unified-platform/lib/common.sh" \
  '[[ -s "${pid_file}" ]] || return 0' \
  'platform shutdown must tolerate an already stopped process'

assert_service_config() {
  local relative_file="$1" service="$2" file="${repo_root}/$1"
  assert_contains "${file}" 'default: local' \
    "${service} must use the local profile when no profile is supplied"
  assert_contains "${file}" \
    "optional:file:\${UNIFIED_PLATFORM_RUNTIME_DIR:target/local-unified-platform}/env/${service}.properties" \
    "${service} must import its generated runtime properties"
}

assert_service_config \
  'egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/resources/application.yml' \
  idp
assert_service_config \
  'egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/application.yml' \
  rbac3
assert_service_config \
  'egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/resources/application.yml' \
  gateway-admin
assert_service_config \
  'egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/resources/application.yml' \
  gateway-engine
ddc_config='egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/resources/application.yml'
assert_service_config "${ddc_config}" ddc
assert_contains "${repo_root}/${ddc_config}" \
  'classpath:META-INF/egon-cola-ddc.properties' \
  'DDC must preserve its starter defaults import'

assert_vite_proxy() {
  local relative_file="$1" platform="$2" port="$3"
  assert_contains "${repo_root}/${relative_file}" "http://127.0.0.1:${port}" \
    "${platform} plain npm run dev must proxy to its local backend"
}

assert_vite_proxy \
  'egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/vite.config.ts' \
  IdP 18120
assert_vite_proxy \
  'egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/vite.config.ts' \
  RBAC3 18130
assert_vite_proxy \
  'egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/vite.config.ts' \
  Gateway 18140
assert_vite_proxy \
  'egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/vite.config.ts' \
  DDC 18150

prepare_script="${repo_root}/scripts/unified-platform/prepare-local-stack.sh"
start_script="${repo_root}/scripts/unified-platform/start-local-stack.sh"
live_login_test="${repo_root}/scripts/unified-platform/test-live-frontend-login.sh"
[[ -x "${prepare_script}" ]] \
  || fail 'prepare-local-stack.sh must exist and be executable'
[[ -x "${live_login_test}" ]] \
  || fail 'live frontend login contract must exist and be executable'
assert_contains "${prepare_script}" 'start-local-stack.sh' \
  'preparation must initialize the full local topology'
assert_contains "${prepare_script}" 'stop-local-stack.sh' \
  'preparation must leave ports free for direct commands'
assert_contains "${prepare_script}" 'npm ci' \
  'preparation must install missing locked frontend dependencies'
assert_contains "${prepare_script}" '.properties' \
  'preparation must verify generated Java runtime configuration'
assert_not_contains "${start_script}" 'test-live-frontend-login.sh' \
  'stack startup must not execute frontend login regression tests'
assert_contains "${identity_script}" 'publish_gateway_routes true' \
  'deferred startup must prepare HTTP routes before the unified MCP release'
assert_contains "${identity_script}" \
  'MOCK_BACKEND_BUILD_ID "$(local_build_id "${mock_jar}")"' \
  'mock backend reports must use a content-derived local build ID'
assert_contains "${platform_common_script}" 'unified_platform_local_build_id()' \
  'unified platform fixtures must share content-derived local build IDs'
assert_contains "${platform_start_script}" \
  'MCP_TEST_PROVIDER_BUILD_ID' \
  'MCP provider reports must declare a local build ID'
assert_contains "${platform_start_script}" \
  '"$(unified_platform_local_build_id "${mcp_provider_jar}")"' \
  'MCP provider reports must use a content-derived local build ID'
function_file="${temporary_dir}/publish-gateway-routes.sh"
extract_function publish_gateway_routes "${function_file}"
deferred_return_line="$(grep -nF 'if [[ "${defer_release}" == "true" ]]' \
  "${function_file}" | cut -d: -f1)"
draft_validation_line="$(grep -nF 'validation="$(gateway_api POST' \
  "${function_file}" | cut -d: -f1)"
[[ -n "${deferred_return_line}" && -n "${draft_validation_line}" \
    && "${deferred_return_line}" -lt "${draft_validation_line}" ]] \
  || fail 'deferred startup must postpone full draft validation until MCP providers are online'
assert_contains "${live_login_test}" 'fresh Admin endpoint returned HTTP' \
  'frontend login contract must exercise fresh SSO Admin endpoints'
for client_id in idp-admin-web rbac3-admin-web gateway-admin-web ddc-admin-web; do
  assert_contains "${live_login_test}" "fresh_oauth_token ${client_id}" \
    "frontend login contract must issue a fresh ${client_id} token"
done
for application_code in idp-admin rbac3-admin gateway-admin ddc-admin mock-backend; do
  assert_contains "${live_login_test}" "\"${application_code}\"" \
    "frontend login contract must verify the ${application_code} role"
done
for role_code in \
  IDP_LOCAL_ADMIN RBAC3_LOCAL_ADMIN GATEWAY_LOCAL_ADMIN DDC_LOCAL_ADMIN \
  MOCK_LOCAL_ADMIN MOCK_LOCAL_ENTRY; do
  assert_contains "${live_login_test}" "\"${role_code}\"" \
    "frontend login contract must verify the ${role_code} role code"
done
last_web_line="$(grep -nF 'start_admin_web ddc-admin-web' \
  "${start_script}" | tail -1 | cut -d: -f1)"
success_line="$(grep -nF "printf 'Unified platform local stack is running" \
  "${start_script}" | tail -1 | cut -d: -f1)"
[[ "${last_web_line}" -lt "${success_line}" ]] \
  || fail 'startup success must be reported after all Web apps are running'

identity_runbook="${repo_root}/docs/runbooks/unified-identity-local.md"
operations_runbook="${repo_root}/docs/operations/unified-identity-mcp-local-runbook.md"
for runbook in "${identity_runbook}" "${operations_runbook}"; do
  assert_contains "${runbook}" 'prepare-local-stack.sh' \
    'runbook must document the one-time preparation command'
  assert_contains "${runbook}" 'egon-cola-platform-idp-admin-exec.jar' \
    'runbook must document direct IdP JAR startup'
  assert_contains "${runbook}" 'egon-cola-platform-rbac3-admin-exec.jar' \
    'runbook must document direct RBAC3 JAR startup'
  assert_contains "${runbook}" 'egon-cola-platform-gateway-admin-exec.jar' \
    'runbook must document direct Gateway Admin JAR startup'
  assert_contains "${runbook}" 'egon-cola-platform-gateway-engine-exec.jar' \
    'runbook must document direct Gateway Engine JAR startup'
  assert_contains "${runbook}" 'egon-cola-platform-dynamic-config-center-admin-exec.jar' \
    'runbook must document direct DDC JAR startup'
  assert_contains "${runbook}" 'npm run dev' \
    'runbook must document plain frontend startup'
done

verifier="${repo_root}/scripts/unified-platform/verify-local-stack.sh"
assert_contains "${verifier}" 'verify_authenticated_json()' \
  'deep verification must provide a reusable authenticated JSON check'
for label in idp-users rbac3-roles gateway-dashboard ddc-configs; do
  assert_contains "${verifier}" "${label}" \
    "deep verification must cover ${label}"
done
assert_contains "${verifier}" 'admin-feature-matrix' \
  'sanitized evidence must include the Admin feature matrix'

printf 'direct-run-contract: runtime properties adapter PASS\n'
