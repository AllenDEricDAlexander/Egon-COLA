#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"
identity_script="${repo_root}/scripts/unified-identity-local.sh"
platform_start_script="${repo_root}/scripts/unified-xingyuan/start-local-stack.sh"
platform_common_script="${repo_root}/scripts/unified-xingyuan/lib/common.sh"
platform_verify_script="${repo_root}/scripts/unified-xingyuan/verify-local-stack.sh"
cleanup_script="${repo_root}/scripts/unified-xingyuan/cleanup-legacy-identity-keys.sh"
release_fixture="${repo_root}/scripts/unified-xingyuan/fixtures/unified-xingyuan-release.json"

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

function_file="${temporary_dir}/command-start.sh"
extract_function command_start "${function_file}"
awk '
  /stage "issuing Tianquan-Shoubing-owned service credentials"/ { issuing = 1 }
  issuing && /refresh_service_tokens/ { refreshed = 1 }
  refreshed && /stop_process tianshu/ { stopped = 1 }
  stopped && /start_process tianshu/ { restarted = 1 }
  /initialize_ddc_topology/ { exit !restarted }
  END { if (!restarted) exit 1 }
' "${function_file}" \
  || fail 'Tianshu must reload initialized OAuth credentials before Admin topology requests'

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
[[ "$(java_property_key EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_RUNTIME_PASSWORD_FILE)" \
    == 'egon.cola.platform.tianquan.jianshen.runtime.password-file' ]] \
  || fail 'Tianquan-Jianshen runtime password file must use its canonical property key'
[[ "$(java_property_key YUHENG_ADMIN_TIANSHU_ENABLED)" \
    == 'yuheng.admin.tianshu.enabled' ]] \
  || fail 'Yuheng Tianshu enablement must use its canonical property key'
[[ "$(java_property_key YUHENG_ADMIN_RELEASE_RECONCILE_ENABLED)" \
    == 'yuheng.admin.release-reconcile-enabled' ]] \
  || fail 'Yuheng release recovery enablement must use its canonical property key'
[[ "$(java_property_key YUHENG_ADMIN_SECRETS_MASTER_KEY_BASE64)" \
    == 'yuheng.admin.secrets.master-key-base64' ]] \
  || fail 'Yuheng secret protection must use its canonical property key'
[[ "$(java_property_key EGON_COLA_COMPONENT_YUHENG_PROVIDER_HTTP_FAIL_FAST)" \
    == 'egon.cola.component.tianshu.registry.http.fail-fast' ]] \
  || fail 'Yuheng Provider fail-fast must use its canonical property key'
[[ "$(java_property_key EGON_COLA_COMPONENT_TIANSHU_CONSISTENCY_FAIL_FAST)" \
    == 'egon.cola.component.tianshu.consistency.fail-fast' ]] \
  || fail 'Tianshu consistency fail-fast must use its canonical property key'
[[ "$(java_property_key EGON_COLA_COMPONENT_YUHENG_ENGINE_HTTP_PUBLIC_PORT)" \
    == 'egon.cola.component.yuheng.engine.http.public-port' ]] \
  || fail 'Yuheng public listener must use its nested canonical property key'
[[ "$(java_property_key EGON_COLA_COMPONENT_YUHENG_ENGINE_HTTP_INTERNAL_PORT)" \
    == 'egon.cola.component.yuheng.engine.http.internal-port' ]] \
  || fail 'Yuheng internal listener must use its nested canonical property key'

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
idp_database=tianquan-shoubing-test
rbac3_database=tianquan-jianshen-test
service_tenant_id=default
database_table_exists() {
  [[ "$1" == 'tianquan-jianshen-test' && "$2" == 'public.rbac3_tenant' ]]
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
service_tenant_id=default
database_table_exists() { [[ "$1" == 'tianquan-shoubing-test' ]]; }
database_row_exists() { return 1; }
resolve_existing_service_tenant_id
[[ "${service_tenant_id}" == 'default' ]] \
  || fail 'an empty Tianquan-Shoubing catalog must wait for local tenant bootstrap'
function_file="${temporary_dir}/tianquan-jianshen-jdbc-url.sh"
extract_function rbac3_jdbc_url "${function_file}"
(
  source "${function_file}"
  postgres_host=127.0.0.1
  postgres_port=5432
  tenant_authority_artifact=
  database_table_exists() { return 1; }
  [[ "$(rbac3_jdbc_url)" == 'jdbc:postgresql://127.0.0.1:5432/tianquan-jianshen-test' ]] \
    || fail 'fresh prepare must not emit a fake numeric tenant or authority gate'
)
unset -f database_table_exists database_row_exists rbac3_tenant_id
unset idp_database

jq -e '
  .server.resourceUri == "https://api.egon.internal/local/identity/yuheng-test-mcp-provider"
  and (.server | has("oauthAudience") | not)
' "${release_fixture}" >/dev/null \
  || fail 'MCP Server fixture must use the exact OAuth Resource URI contract'
assert_contains "${platform_start_script}" 'ensure_mcp_user_delegation' \
  'local MCP startup must explicitly grant its exact Resource to the OAuth client'
assert_contains "${platform_start_script}" \
  'wait_gateway_engine_provider_catalog' \
  'local MCP release must wait for both Yuheng Engine Tianshu registrations'
assert_contains "${platform_start_script}" \
  'publish-yuheng-routes' \
  'platform startup must publish the prepared Yuheng routes after Engine startup'
assert_contains "${platform_start_script}" \
  'yuheng-admin-control-plane.service.jwt' \
  'Yuheng control-plane automation must use the dedicated Tianquan-Shoubing SERVICE token'
assert_contains "${identity_script}" \
  'ensure_gateway_application permission tianquan-shoubing' \
  'Yuheng initialization must register the real Tianquan-Shoubing catalog application'
assert_contains "${identity_script}" \
  'wait_gateway_catalog_for_app' \
  'Yuheng startup must wait for every real provider catalog'
assert_contains "${identity_script}" \
  'wait_gateway_openapi_sync_for_app platform yuheng-admin' \
  'Admin catalog refresh must wait for the current executable build to become valid'
assert_contains "${identity_script}" \
  'publish_gateway_routes' \
  'Yuheng startup must publish operation-scoped routes from real catalogs'
assert_contains "${identity_script}" \
  'YUHENG_ADMIN_YUHENG_REPORTING_ENABLED false' \
  'Yuheng Admin must be available before other providers report catalogs'
assert_contains "${cleanup_script}" '--execute' \
  'legacy-key cleanup must require an explicit execute switch'
assert_contains "${cleanup_script}" '--endpoint host:port/database' \
  'legacy-key cleanup must require an explicit Redis endpoint'
assert_contains "${cleanup_script}" 'identity:v1:sso-session:*' \
  'legacy-key cleanup must name the old Tianquan-Shoubing SSO prefix explicitly'
assert_contains "${cleanup_script}" 'tianquan-jianshen:*:fence:session:*' \
  'legacy-key cleanup must name the old Tianquan-Jianshen fence prefix explicitly'
assert_not_contains "${cleanup_script}" 'FLUSHDB' \
  'legacy-key cleanup must not flush a Redis database'
assert_not_contains "${cleanup_script}" 'FLUSHALL' \
  'legacy-key cleanup must not flush all Redis databases'
assert_contains "${platform_start_script}" 'mcp-user.at' \
  'local MCP startup must issue a token for the exact provider Resource'
assert_contains "${platform_verify_script}" \
  'mcp_token_file="${verification_token_dir}/mcp-user.at"' \
  'MCP verification must not reuse the mock backend Resource token'
assert_contains "${platform_verify_script}" 'run_identity issue-user-token' \
  'MCP verification must refresh its Resource token after identity revocation checks'
assert_contains "${platform_verify_script}" \
  'UNIFIED_IDENTITY_TIANQUAN_SHOUBING_DATABASE="${identity_idp_database}"' \
  'platform verification must use the Tianquan-Shoubing database recorded by the running stack'
assert_contains "${platform_verify_script}" \
  'UNIFIED_IDENTITY_TIANQUAN_JIANSHEN_DATABASE="${identity_rbac3_database}"' \
  'platform verification must use the Tianquan-Jianshen database recorded by the running stack'
assert_contains "${platform_verify_script}" \
  'UNIFIED_IDENTITY_YUHENG_DATABASE="${identity_gateway_database}"' \
  'platform verification must use the Yuheng database recorded by the running stack'
assert_contains "${platform_verify_script}" \
  'UNIFIED_IDENTITY_TIANSHU_DATABASE="${identity_ddc_database}"' \
  'platform verification must use the Tianshu database recorded by the running stack'
assert_not_contains "${platform_verify_script}" \
  'Authorization: Bearer $(<"${tenant_token_file}")' \
  'MCP verification must use the exact provider Resource token for every transport'
assert_contains "${platform_verify_script}" \
  '"local_echo_task","arguments":{"body":{"value":"task"}}' \
  'MCP task verification must preserve the Yuheng Operation body location'

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
  printf '%s' 'test-tianshu-runtime-access-key' >"${secret_dir}/tianshu-runtime.access-key"
  printf '%s' 'test-tianshu-runtime-secret' >"${secret_dir}/tianshu-runtime.secret"
  printf '%s' 'test-tianshu-registry-access-key' >"${secret_dir}/tianshu-registry.access-key"
  printf '%s' 'test-tianshu-registry-secret' >"${secret_dir}/tianshu-registry.secret"
  printf '%s' 'test-tianshu-management-access-key' >"${secret_dir}/tianshu-management.access-key"
  printf '%s' 'test-tianshu-management-secret' >"${secret_dir}/tianshu-management.secret"
  printf '%s' 'test-yuheng-master-key' >"${secret_dir}/yuheng-master-key.base64"
  postgres_password() {
    printf '%s' 'test-postgres-password'
  }
  service_tenant_id=default
  database_table_exists() {
    return 1
  }
  rbac3_jdbc_url() {
    printf '%s' 'jdbc:postgresql://127.0.0.1:5432/tianquan-jianshen-test'
  }
  write_service_env_files
)

mcp_engine_env="${generated_runtime}/env/yuheng-mcp-gateway.env"
assert_env_equals "${mcp_engine_env}" TIANSHU_APP_CODE yuheng-mcp-gateway-default \
  'MCP Engine must use its own source-bound Tianshu application'
assert_env_equals "${mcp_engine_env}" YUHENG_MCP_ENGINE_RESOURCE_SERVER_ID \
  identity-yuheng-mcp-gateway-default-local \
  'MCP Engine must use its own OAuth Resource Server'
assert_env_equals "${mcp_engine_env}" YUHENG_MCP_ENGINE_RESOURCE_URI \
  https://api.egon.internal/local/identity/yuheng-mcp-gateway-default \
  'MCP Engine must not borrow the API Resource URI'
assert_env_equals "${mcp_engine_env}" SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_EGON_TIANQUAN_SHOUBING_CLIENT_ID \
  yuheng-mcp-gateway-service \
  'MCP Engine must use its own confidential management Client'
assert_env_equals "${mcp_engine_env}" YUHENG_MCP_ENGINE_PORT 18185 \
  'MCP data-plane listener must not reuse an API Engine listener'
assert_env_equals "${mcp_engine_env}" YUHENG_MCP_ENGINE_MANAGEMENT_PORT 18186 \
  'MCP management listener must be independent'
assert_env_equals "${mcp_engine_env}" YUHENG_MCP_ENGINE_TIANSHU_INSTANCE_ID yuheng-mcp-gateway-local-1 \
  'MCP Engine must publish an independent lease'
assert_env_equals "${mcp_engine_env}" YUHENG_MCP_ENGINE_GROUP_CODE default \
  'both roles must consume the same Yuheng group'
assert_env_equals "${generated_runtime}/env/yuheng-admin.env" YUHENG_ADMIN_TIANSHU_API_RPC_APP_CODE \
  yuheng-biz-gateway-default 'Admin must target the API role scope explicitly'
assert_env_equals "${generated_runtime}/env/yuheng-admin.env" YUHENG_ADMIN_TIANSHU_MCP_APP_CODE \
  yuheng-mcp-gateway-default 'Admin must target the MCP role scope explicitly'
assert_contains "${platform_start_script}" 'unified_xingyuan_start_jar yuheng-mcp-gateway' \
  'core startup must start the independent MCP process before publication'
assert_not_contains "${mcp_engine_env}" 'YUHENG_MCP_TASK_SERVICE_TOKEN_PRIVATE_KEY_FILE=' \
  'MCP must use the standard confidential service Client'

idp_env="${generated_runtime}/env/tianquan-shoubing.env"
assert_env_equals "${idp_env}" TIANQUAN_SHOUBING_TIANSHU_ENABLED true \
  'local Tianquan-Shoubing must start its Tianshu config client'
assert_env_equals "${idp_env}" TIANQUAN_SHOUBING_HTTP_PROVIDER_ENABLED true \
  'local Tianquan-Shoubing must publish its HTTP Provider lease'
assert_env_equals "${idp_env}" TIANQUAN_SHOUBING_YUHENG_REPORTING_ENABLED false \
  'local Tianquan-Shoubing must defer Yuheng catalog reporting until its control plane is ready'
assert_env_equals "${idp_env}" TIANQUAN_SHOUBING_RESOURCE_BIZ_CODE permission \
  'local Tianquan-Shoubing must report under the permission business scope'
assert_env_equals "${idp_env}" TIANQUAN_SHOUBING_RESOURCE_APP_CODE tianquan-shoubing \
  'local Tianquan-Shoubing must report under the tianquan-shoubing application scope'
assert_env_equals "${idp_env}" TIANQUAN_SHOUBING_DECLARED_HOSTS 127.0.0.1 \
  'local Tianquan-Shoubing must report its declared host explicitly'
assert_env_equals "${idp_env}" TIANQUAN_SHOUBING_HTTP_OPENAPI_ENABLED true \
  'fresh Tianquan-Shoubing must publish its HTTP OpenAPI catalog without historical reports'
assert_env_equals "${idp_env}" \
  EGON_COLA_COMPONENT_YUHENG_PROVIDER_HTTP_FAIL_FAST false \
  'local Tianquan-Shoubing must recover until its Tianshu scope binding is initialized'
assert_env_equals "${idp_env}" TIANSHU_BIZ_CODE permission \
  'local Tianquan-Shoubing must use its Resource business scope'
assert_env_equals "${idp_env}" TIANSHU_APP_CODE tianquan-shoubing \
  'local Tianquan-Shoubing must use its Resource application scope'
assert_env_equals "${idp_env}" \
  EGON_COLA_COMPONENT_TIANSHU_CONSISTENCY_FAIL_FAST false \
  'local Tianquan-Shoubing must recover until its Tianshu topology exists'
assert_env_equals "${idp_env}" DEPLOYMENT_ENV local \
  'local Tianquan-Shoubing must register in the local environment'
assert_env_equals "${idp_env}" DEPLOYMENT_NAMESPACE default \
  'local Tianquan-Shoubing must use the default visibility namespace'
assert_env_equals "${idp_env}" TIANQUAN_SHOUBING_INSTANCE_ID tianquan-shoubing-local-1 \
  'local Tianquan-Shoubing must use a stable lease identity'
assert_env_equals "${idp_env}" TIANQUAN_SHOUBING_RESOURCE_SERVER_ID \
  permission-tianquan-shoubing-local \
  'local Tianquan-Shoubing must validate tokens for its exact Resource Server'
assert_env_equals "${idp_env}" TIANQUAN_SHOUBING_RESOURCE_URI \
  https://api.egon.internal/local/permission/tianquan-shoubing \
  'local Tianquan-Shoubing must validate one exact Resource URI'
assert_env_equals "${idp_env}" TIANQUAN_SHOUBING_RESOURCE_MANAGEMENT_CLIENT_ID \
  tianquan-shoubing-service \
  'local Tianquan-Shoubing must request admission with its service client'
assert_env_equals "${idp_env}" TIANQUAN_SHOUBING_RESOURCE_MANAGEMENT_KEY_ID \
  tianquan-shoubing-local \
  'local Tianquan-Shoubing must identify its admission signing key'
assert_env_equals "${idp_env}" TIANQUAN_SHOUBING_RESOURCE_MANAGEMENT_PRIVATE_KEY_FILE \
  "${generated_runtime}/secrets/tianquan-shoubing-private.pem" \
  'local Tianquan-Shoubing must sign admission assertions with its protected private key'
assert_env_equals "${idp_env}" TIANQUAN_SHOUBING_RESOURCE_ADMISSION_RPC_TARGET \
  dns:///127.0.0.1:18122 \
  'local Tianquan-Shoubing must use the static Tianquan-Shoubing admission RPC target'
assert_env_equals "${idp_env}" TIANQUAN_SHOUBING_RPC_PORT 18122 \
  'local Tianquan-Shoubing must expose the internal admission RPC provider'
assert_env_equals "${idp_env}" TIANQUAN_SHOUBING_TIANQUAN_JIANSHEN_SERVICE_CLIENT_ID tianquan-shoubing-service \
  'local Tianquan-Shoubing must call Tianquan-Jianshen with its confidential service client'
assert_env_equals "${idp_env}" TIANQUAN_SHOUBING_TIANQUAN_JIANSHEN_SERVICE_KEY_ID tianquan-shoubing-local \
  'local Tianquan-Shoubing must identify its service assertion key'
assert_env_equals "${idp_env}" TIANQUAN_SHOUBING_TIANQUAN_JIANSHEN_SERVICE_PRIVATE_KEY_FILE \
  "${generated_runtime}/secrets/tianquan-shoubing-private.pem" \
  'local Tianquan-Shoubing must sign service assertions with its protected private key'
assert_env_equals "${idp_env}" TIANQUAN_SHOUBING_TIANQUAN_JIANSHEN_RESOURCE_URI \
  https://api.egon.internal/local/permission/tianquan-jianshen \
  'local Tianquan-Shoubing must request a token for the exact Tianquan-Jianshen Resource'
assert_env_equals "${idp_env}" TIANQUAN_SHOUBING_TIANQUAN_JIANSHEN_SERVICE_TENANT_ID default \
  'local Tianquan-Shoubing service calls must bind one exact tenant'
assert_env_equals "${idp_env}" TIANQUAN_SHOUBING_TIANQUAN_JIANSHEN_SERVICE_SCOPES \
  'service:authorization:decide service:authorization:snapshot service:identity:resolve' \
  'local Tianquan-Shoubing service calls must request only reviewed Tianquan-Jianshen scopes'
assert_env_equals "${idp_env}" TIANSHU_REGISTRY_REDIS_DATABASE 10 \
  'local Tianquan-Shoubing must use the Tianshu Registry Redis database'
assert_env_equals "${idp_env}" TIANSHU_RPC_TARGET dns:///127.0.0.1:19080 \
  'local Tianquan-Shoubing must bootstrap Tianshu through direct RPC'
assert_env_equals "${idp_env}" TIANSHU_RPC_RUNTIME_ACCESS_KEY \
  test-tianshu-runtime-access-key \
  'local Tianquan-Shoubing must use the runtime Tianshu credential'
assert_env_equals "${idp_env}" TIANSHU_RPC_REGISTRY_ACCESS_KEY \
  test-tianshu-registry-access-key \
  'local Tianquan-Shoubing must use the registry Tianshu credential'

rbac3_env="${generated_runtime}/env/tianquan-jianshen.env"
assert_env_equals "${rbac3_env}" TIANQUAN_JIANSHEN_TIANSHU_ENABLED true \
  'local Tianquan-Jianshen must start its Tianshu config client'
assert_env_equals "${rbac3_env}" TIANQUAN_JIANSHEN_HTTP_PROVIDER_ENABLED true \
  'local Tianquan-Jianshen must publish its HTTP Provider lease'
assert_env_equals "${rbac3_env}" TIANQUAN_JIANSHEN_YUHENG_REPORTING_ENABLED false \
  'local Tianquan-Jianshen must defer Yuheng catalog reporting until its control plane is ready'
assert_env_equals "${rbac3_env}" TIANQUAN_JIANSHEN_RESOURCE_BIZ_CODE permission \
  'local Tianquan-Jianshen must report under the permission business scope'
assert_env_equals "${rbac3_env}" TIANQUAN_JIANSHEN_RESOURCE_APP_CODE tianquan-jianshen \
  'local Tianquan-Jianshen must report under the tianquan-jianshen application scope'
assert_env_equals "${rbac3_env}" TIANQUAN_JIANSHEN_DECLARED_HOSTS 127.0.0.1 \
  'local Tianquan-Jianshen must report its declared host explicitly'
assert_env_equals "${rbac3_env}" \
  EGON_COLA_COMPONENT_YUHENG_PROVIDER_HTTP_FAIL_FAST false \
  'local Tianquan-Jianshen must recover until its Tianshu scope binding is initialized'
assert_env_equals "${rbac3_env}" TIANSHU_BIZ_CODE permission \
  'local Tianquan-Jianshen must use its Resource business scope'
assert_env_equals "${rbac3_env}" TIANSHU_APP_CODE tianquan-jianshen \
  'local Tianquan-Jianshen must use its Resource application scope'
assert_env_equals "${rbac3_env}" \
  EGON_COLA_COMPONENT_TIANSHU_CONSISTENCY_FAIL_FAST false \
  'local Tianquan-Jianshen must recover until its Tianshu topology exists'
assert_env_equals "${rbac3_env}" DEPLOYMENT_ENV local \
  'local Tianquan-Jianshen must register in the local environment'
assert_env_equals "${rbac3_env}" DEPLOYMENT_NAMESPACE default \
  'local Tianquan-Jianshen must use the default visibility namespace'
assert_env_equals "${rbac3_env}" TIANQUAN_JIANSHEN_INSTANCE_ID tianquan-jianshen-local-1 \
  'local Tianquan-Jianshen must use a stable lease identity'
assert_env_equals "${rbac3_env}" TIANQUAN_JIANSHEN_ARTIFACT_VERSION local \
  'local Tianquan-Jianshen service identity must use the local artifact version'
assert_env_equals "${rbac3_env}" \
  EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_AUTHORIZATION_SERVICE_TOKEN_ENABLED true \
  'Tianquan-Jianshen must acquire internal authorization credentials per target tenant'
assert_env_equals "${rbac3_env}" \
  SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_EGON_TIANQUAN_SHOUBING_CLIENT_ID \
  tianquan-jianshen-service \
  'Tianquan-Jianshen tenant-aware credentials must use the approved service Client'
assert_env_equals "${rbac3_env}" TIANQUAN_JIANSHEN_RESOURCE_SERVER_ID \
  permission-tianquan-jianshen-local \
  'local Tianquan-Jianshen must validate tokens for its exact Resource Server'
assert_env_equals "${rbac3_env}" TIANQUAN_JIANSHEN_RESOURCE_URI \
  https://api.egon.internal/local/permission/tianquan-jianshen \
  'local Tianquan-Jianshen must validate one exact Resource URI'
assert_env_equals "${rbac3_env}" TIANQUAN_JIANSHEN_RESOURCE_MANAGEMENT_CLIENT_ID \
  tianquan-jianshen-service \
  'local Tianquan-Jianshen must request admission with its service client'
assert_env_equals "${rbac3_env}" TIANQUAN_JIANSHEN_RESOURCE_MANAGEMENT_KEY_ID \
  tianquan-jianshen-local \
  'local Tianquan-Jianshen must identify its admission signing key'
assert_env_equals "${rbac3_env}" \
  TIANQUAN_JIANSHEN_RESOURCE_MANAGEMENT_PRIVATE_KEY_FILE \
  "${generated_runtime}/secrets/tianquan-jianshen-private.pem" \
  'local Tianquan-Jianshen must sign admission with its protected private key'
assert_env_equals "${rbac3_env}" TIANQUAN_JIANSHEN_RESOURCE_ADMISSION_RPC_TARGET \
  dns:///127.0.0.1:18122 \
  'local Tianquan-Jianshen must use the static Tianquan-Shoubing admission RPC target'

gateway_admin_env="${generated_runtime}/env/yuheng-admin.env"
assert_env_equals "${gateway_admin_env}" TIANSHU_RPC_TARGET \
  dns:///127.0.0.1:19080 \
  'local Yuheng Admin must bootstrap Tianshu through direct RPC'
assert_env_equals "${gateway_admin_env}" TIANSHU_RPC_MANAGEMENT_ACCESS_KEY \
  test-tianshu-management-access-key \
  'local Yuheng Admin must use the management Tianshu credential'
assert_env_equals "${gateway_admin_env}" YUHENG_ADMIN_RESOURCE_SERVER_ID \
  platform-yuheng-admin-local \
  'local Yuheng Admin must validate its exact Resource Server'
assert_env_equals "${gateway_admin_env}" YUHENG_ADMIN_RESOURCE_URI \
  https://api.egon.internal/local/platform/yuheng-admin \
  'local Yuheng Admin must validate one exact Resource URI'
assert_env_equals "${gateway_admin_env}" \
  YUHENG_ADMIN_RESOURCE_MANAGEMENT_CLIENT_ID yuheng-admin-service \
  'local Yuheng Admin must request admission with its service client'
assert_env_equals "${gateway_admin_env}" \
  YUHENG_ADMIN_RESOURCE_MANAGEMENT_KEY_ID yuheng-admin-local \
  'local Yuheng Admin must identify its admission signing key'
assert_env_equals "${gateway_admin_env}" \
  YUHENG_ADMIN_RESOURCE_MANAGEMENT_PRIVATE_KEY_FILE \
  "${generated_runtime}/secrets/yuheng-admin-private.pem" \
  'local Yuheng Admin must sign admission with its protected private key'
assert_env_equals "${gateway_admin_env}" \
  YUHENG_ADMIN_RESOURCE_ADMISSION_RPC_TARGET \
  dns:///127.0.0.1:18122 \
  'local Yuheng Admin must use the static Tianquan-Shoubing admission RPC target'
assert_env_equals "${gateway_admin_env}" \
  YUHENG_ADMIN_YUHENG_REPORTING_ENABLED false \
  'Yuheng Admin must defer self-reporting until other catalogs are published'
assert_env_equals "${gateway_admin_env}" \
  YUHENG_ADMIN_RELEASE_RECONCILE_ENABLED false \
  'platform startup must defer historical Yuheng release recovery until the current route release completes'
assert_env_equals "${gateway_admin_env}" YUHENG_ADMIN_RESOURCE_BIZ_CODE xingyuan \
  'Yuheng Admin must report under the Xingyuan business scope'
assert_env_equals "${gateway_admin_env}" YUHENG_ADMIN_RESOURCE_APP_CODE yuheng-admin \
  'Yuheng Admin must report under the yuheng-admin application scope'
assert_env_equals "${gateway_admin_env}" YUHENG_ADMIN_DECLARED_HOSTS 127.0.0.1 \
  'Yuheng Admin must report its declared host explicitly'
assert_contains \
  'egon-cola-xingyuan/egon-cola-yuheng/yuheng-biz-gateway/src/main/resources/application.yml' \
  '          id: ${YUHENG_ENGINE_TIANSHU_INSTANCE_ID:}' \
  'Yuheng Engine Tianshu client and admission ticket must share one instance id'
assert_contains \
  'egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-tianquan-shoubing-backend/src/main/resources/application.yml' \
  '          id: ${MOCK_BACKEND_INSTANCE_ID:mock-backend-local-1}' \
  'mock backend Tianshu client and admission ticket must share one instance id'
[[ "$(grep -Fc 'service_tenant_id="$(rbac3_tenant_id default)"' \
  "${identity_script}")" == "2" ]] || fail \
  'start and credential synchronization must both resolve the exact Tianquan-Jianshen tenant id'
assert_contains "${identity_script}" \
  'resolve_existing_service_tenant_id' \
  'prepare must restore an existing exact Tianquan-Jianshen tenant id before rewriting env files'
assert_contains "${identity_script}" \
  'tenant_b_id="$(rbac3_tenant_id tenant-b)"' \
  'start must resolve the exact secondary Tianquan-Jianshen tenant id'
assert_contains "${identity_script}" \
  'TIANQUAN_SHOUBING_DEVELOPMENT_TIANQUAN_JIANSHEN_SERVICE_TENANT_IDS' \
  'Tianquan-Shoubing bootstrap must receive every exact local Tianquan-Jianshen service tenant'
assert_contains "${identity_script}" 'MOCK_LOCAL_ENTRY' \
  'the no-admin verification state must preserve mock Resource entry permission'
assert_contains "${identity_script}" \
  'user_access_token_for_tenant "${tenant}"' \
  'USER token issuance must reuse the Yuheng cookie and never create a server session'
assert_not_contains "${identity_script}" '.access.jwt' \
  'identity bootstrap must not persist per-client USER Access Token files'
legacy_service_file_key='TIANQUAN_JIANSHEN_SERVICE_CREDENTIAL_'
assert_not_contains "${identity_script}" "${legacy_service_file_key}FILE" \
  'identity services must use Tianquan-Shoubing Client Assertion instead of static Tianquan-Jianshen bearer files'
assert_not_contains "${platform_start_script}" '.access.jwt' \
  'platform startup must not persist per-client USER Access Token files'
assert_not_contains "${platform_verify_script}" '.access.jwt' \
  'platform verification must not persist per-client USER Access Token files'

gateway_engine_env="${generated_runtime}/env/yuheng-biz-gateway.env"
assert_env_equals "${gateway_engine_env}" \
  YUHENG_ENGINE_RESOURCE_ADMISSION_RPC_TARGET \
  dns:///127.0.0.1:18122 \
  'Yuheng Engine must use the static Tianquan-Shoubing admission RPC target'
assert_env_equals "${gateway_engine_env}" \
  EGON_COLA_COMPONENT_TIANSHU_RPC_MAX_INBOUND_MESSAGE_SIZE 67108864 \
  'Yuheng Engine must accept the complete Tianshu Yuheng rule document'
assert_env_equals "${gateway_engine_env}" \
  YUHENG_MCP_TASK_SERVICE_TOKEN_ENABLED true \
  'Yuheng Engine must use a SERVICE identity for durable MCP execution'
assert_env_equals "${gateway_engine_env}" \
  YUHENG_MCP_TASK_SERVICE_TOKEN_CLIENT_ID yuheng-biz-gateway-service \
  'durable MCP execution must use the approved Yuheng Engine Client'
assert_env_equals "${gateway_engine_env}" \
  YUHENG_MCP_TASK_SERVICE_TOKEN_PRIVATE_KEY_FILE \
  "${generated_runtime}/secrets/yuheng-biz-gateway-private.pem" \
  'durable MCP execution must use the protected Yuheng Engine key'
assert_env_equals "${gateway_engine_env}" \
  YUHENG_MCP_TASK_SERVICE_TOKEN_SCOPES mcp:operation:invoke \
  'durable MCP execution must request only the Tianquan-Shoubing-approved Provider scope'

mock_backend_env="${generated_runtime}/env/mock-backend.env"
assert_env_equals "${mock_backend_env}" \
  MOCK_BACKEND_RESOURCE_ADMISSION_RPC_TARGET \
  dns:///127.0.0.1:18122 \
  'mock backend must use the static Tianquan-Shoubing admission RPC target'
assert_not_contains "${identity_script}" RESOURCE_ADMISSION_ENDPOINT \
  'local environment generation must not retain HTTP admission endpoints'

ddc_env="${generated_runtime}/env/tianshu.env"
assert_env_equals "${ddc_env}" TIANSHU_RPC_PORT 19080 \
  'local Tianshu Admin must expose the direct RPC provider'
assert_env_equals "${ddc_env}" TIANSHU_RPC_REGISTRY_ACCESS_KEY \
  test-tianshu-registry-access-key \
  'local Tianshu Admin must configure the registry credential profile'
assert_env_equals "${ddc_env}" TIANSHU_RESOURCE_SERVER_ID \
  platform-tianshu-local \
  'local Tianshu Admin must validate tokens for its exact Resource Server'
assert_env_equals "${ddc_env}" TIANSHU_RESOURCE_URI \
  https://api.egon.internal/local/platform/tianshu \
  'local Tianshu Admin must validate one exact Resource URI'
assert_env_equals "${ddc_env}" TIANSHU_ADMIN_JWT_AUDIENCE \
  https://api.egon.internal/local/platform/tianshu \
  'local Tianshu Admin security chain must use the Resource URI as audience'
assert_env_equals "${ddc_env}" TIANSHU_YUHENG_REPORTING_ENABLED false \
  'Tianshu HTTP catalog must use OpenAPI instead of legacy Yuheng reporting'
assert_env_equals "${ddc_env}" TIANSHU_HTTP_OPENAPI_ENABLED true \
  'fresh Tianshu must publish its HTTP OpenAPI catalog without historical reports'
assert_env_equals "${ddc_env}" TIANSHU_RESOURCE_BIZ_CODE xingyuan \
  'Tianshu must report under the Xingyuan business scope'
assert_env_equals "${ddc_env}" TIANSHU_RESOURCE_APP_CODE tianshu \
  'Tianshu must report under the tianshu application scope'
assert_env_equals "${ddc_env}" TIANSHU_DECLARED_HOSTS 127.0.0.1 \
  'Tianshu must report its declared host explicitly'
assert_env_equals "${rbac3_env}" TIANSHU_REGISTRY_REDIS_DATABASE 10 \
  'local Tianquan-Jianshen must use the Tianshu Registry Redis database'

while IFS='|' read -r service_env client_id _ _; do
  assert_env_equals "${generated_runtime}/env/${service_env}.env" \
    EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_AUTHORIZATION_SERVICE_TOKEN_ENABLED true \
    "${service_env} must acquire Tianquan-Jianshen credentials for the exact USER tenant"
  assert_env_equals "${generated_runtime}/env/${service_env}.env" \
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_EGON_TIANQUAN_SHOUBING_CLIENT_ID \
    "${client_id}" \
    "${service_env} must use its own approved OAuth service Client"
done <<'SERVICE_TOKENS'
tianquan-shoubing|tianquan-shoubing-service|tianquan-shoubing-local|tianquan-shoubing
yuheng-admin|yuheng-admin-service|yuheng-admin-local|yuheng-admin
tianshu|tianshu-service|tianshu-local|tianshu
mock-backend|mock-backend-service|mock-backend-local|mock-backend
yuheng-biz-gateway|yuheng-biz-gateway-service|yuheng-biz-gateway-local|yuheng-biz-gateway
yuheng-mcp-gateway|yuheng-mcp-gateway-service|yuheng-mcp-gateway-local|yuheng-mcp-gateway
SERVICE_TOKENS

function_file="${temporary_dir}/initialize-tianshu-topology.sh"
extract_function initialize_ddc_topology "${function_file}"
# shellcheck disable=SC1090
source "${function_file}"
ddc_topology_calls="${temporary_dir}/tianshu-topology-calls.jsonl"
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
initialize_ddc_topology test-user-token
while read -r provider_biz provider_app; do
  jq -e --arg app "${provider_app}" --arg biz "${provider_biz}" '
    select(
      .method == "POST"
      and .path == "/api/v1/tianshu/apps"
      and .body.bizCode == $biz
      and .body.appCode == $app
      and .body.enabled == true
    )
  ' "${ddc_topology_calls}" >/dev/null \
    || fail "Tianshu topology must create the ${provider_app} application"
  jq -e --arg app "${provider_app}" --arg biz "${provider_biz}" '
    select(
      .method == "POST"
      and .path == "/api/v1/tianshu/namespace-env-app-bindings"
      and .body.bizCode == $biz
      and .body.namespaceCode == "default"
      and .body.env == "local"
      and .body.appCode == $app
      and .body.enabled == true
    )
  ' "${ddc_topology_calls}" >/dev/null \
    || fail "Tianshu topology must enable the ${provider_app} scope binding"
done <<'PROVIDERS'
permission tianquan-shoubing
permission tianquan-jianshen
PROVIDERS
unset -f ddc_api initialize_ddc_topology

function_file="${temporary_dir}/wait-tianshu-provider-registration.sh"
extract_function wait_ddc_provider_registration "${function_file}"
# shellcheck disable=SC1090
source "${function_file}"
ddc_registry_queries="${temporary_dir}/tianshu-registry-queries.txt"
ddc_api() {
  local method="$1" path="$2" attempt
  [[ "${method}" == "GET" ]] \
    || fail 'provider registration wait must only read Tianshu state'
  printf '%s\n' "${path}" >>"${ddc_registry_queries}"
  attempt="$(wc -l <"${ddc_registry_queries}" | tr -d ' ')"
  if [[ "${attempt}" -eq 1 ]]; then
    printf '%s' '{"success":true,"data":{"services":[]}}'
  else
    printf '%s' \
      '{"success":true,"data":{"services":[{"appCode":"tianquan-shoubing","serviceKind":"HTTP_PROVIDER","protocol":"http","serviceName":"tianquan-shoubing-admin","group":"default","version":"5.3.2"}]}}'
  fi
}
sleep() {
  :
}
wait_ddc_provider_registration permission tianquan-shoubing tianquan-shoubing-admin
[[ "$(wc -l <"${ddc_registry_queries}" | tr -d ' ')" -eq 2 ]] \
  || fail 'provider registration wait must poll until the lease is online'
grep -Fq \
  'registry/services?bizCode=permission&namespaceCode=default&env=local&appCode=tianquan-shoubing&serviceKind=HTTP_PROVIDER&protocol=http&serviceName=tianquan-shoubing-admin&group=default' \
  "${ddc_registry_queries}" \
  || fail 'provider registration wait must query the exact Tianquan-Shoubing service key'
unset -f ddc_api sleep wait_ddc_provider_registration

function_file="${temporary_dir}/wait-tianshu-rpc-provider-registration.sh"
extract_function wait_ddc_rpc_provider_registration "${function_file}"
# shellcheck disable=SC1090
source "${function_file}"
ddc_rpc_registry_queries="${temporary_dir}/tianshu-rpc-registry-queries.txt"
ddc_api() {
  local method="$1" path="$2" attempt
  [[ "${method}" == "GET" ]] \
    || fail 'RPC provider registration wait must only read Tianshu state'
  printf '%s\n' "${path}" >>"${ddc_rpc_registry_queries}"
  attempt="$(wc -l <"${ddc_rpc_registry_queries}" | tr -d ' ')"
  if [[ "${attempt}" -eq 1 ]]; then
    printf '%s' '{"success":true,"data":{"services":[]}}'
  else
    printf '%s' \
      '{"success":true,"data":{"services":[{"appCode":"tianquan-shoubing","serviceKind":"RPC_PROVIDER","protocol":"grpc","serviceName":"egon.tianquan.shoubing.v1.IdentityDirectoryService","group":"tianquan-shoubing","version":"1.0.0"}]}}'
  fi
}
sleep() {
  :
}
wait_ddc_rpc_provider_registration permission tianquan-shoubing egon.tianquan.shoubing.v1.IdentityDirectoryService tianquan-shoubing 1.0.0
[[ "$(wc -l <"${ddc_rpc_registry_queries}" | tr -d ' ')" -eq 2 ]] \
  || fail 'RPC provider registration wait must poll until the lease is online'
grep -Fq \
  'registry/services?bizCode=permission&namespaceCode=default&env=local&appCode=tianquan-shoubing&serviceKind=RPC_PROVIDER&protocol=grpc&serviceName=egon.tianquan.shoubing.v1.IdentityDirectoryService&group=tianquan-shoubing&version=1.0.0' \
  "${ddc_rpc_registry_queries}" \
  || fail 'RPC provider registration wait must query the exact Tianquan-Shoubing RPC service key'
unset -f ddc_api sleep wait_ddc_rpc_provider_registration

# shellcheck source=lib/common.sh
source "${repo_root}/scripts/unified-xingyuan/lib/common.sh"
declare -F unified_xingyuan_write_frontend_login_env >/dev/null \
  || fail 'frontend login environment writer is missing'
frontend_dir="${temporary_dir}/admin-web"
mkdir -p "${frontend_dir}"
unified_xingyuan_write_frontend_login_env \
  "${frontend_dir}" '77351065313480704'
frontend_env="${frontend_dir}/.env.local"
[[ "$(stat -f '%Lp' "${frontend_env}")" == '600' ]] \
  || fail 'generated frontend login environment must have mode 600'
# shellcheck disable=SC1090
source "${frontend_env}"
[[ "${VITE_DEFAULT_TENANT_ID}" == '77351065313480704' ]] \
  || fail 'plain npm run dev must receive the resolvable default tenant ID'
[[ "${VITE_YUHENG_ORIGIN}" == "${YUHENG_BASE_URL}" ]] \
  || fail 'plain npm run dev must use Yuheng rather than the Vite HTML fallback for login'
assert_contains "${platform_start_script}" '"${script_dir}/test-live-frontend-login.sh"' \
  'startup must verify a fresh login and authorization before reporting success'
printf '%s\n' 'VITE_CUSTOM_SETTING=preserve-me' >"${frontend_env}"
if (unified_xingyuan_write_frontend_login_env \
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
  'write_env "${file}" EGON_COLA_COMPONENT_YUHENG_PROVIDER_HTTP_FAIL_FAST false' \
  'direct Yuheng Engine startup must recover when Tianshu is still starting'
assert_contains "${identity_script}" \
  'write_env "${file}" TIANSHU_MAX_CONFIG_BYTES 67108864' \
  'local Yuheng rule documents must fit the complete chunked catalog'
assert_contains "${identity_script}" \
  'EGON_COLA_COMPONENT_TIANSHU_RPC_MAX_INBOUND_MESSAGE_SIZE' \
  'local Tianshu RPC clients must accept the complete Yuheng rule document'
assert_contains "${identity_script}" \
  'EGON_COLA_COMPONENT_RPC_PROVIDER_MAX_INBOUND_MESSAGE_SIZE' \
  'local Tianshu RPC providers must accept the complete Yuheng rule document'
assert_contains "${identity_script}" \
  'write_env "${file}" EGON_COLA_COMPONENT_TIANSHU_CONSISTENCY_FAIL_FAST false' \
  'direct Tianshu client startup must reconcile when Tianshu is still starting'
assert_contains "${identity_script}" 'wait_ddc_rpc' \
  'local startup must wait for the Tianshu RPC listener before starting clients'
assert_contains "${identity_script}" 'YUHENG_ADMIN_RULE_CHUNK_RETENTION 24h' \
  'local startup must retain release chunks through the normal recovery window'
assert_contains "${identity_script}" 'YUHENG_ADMIN_RULE_CHUNK_CLEANUP_DELAY 1h' \
  'local chunk cleanup must not continuously contend with release publication'
assert_contains "${identity_script}" '"X-Yuheng-Contract-Version","traceparent","x-egon-request-id"' \
  'Yuheng frontend contract and trace headers must be allowed by the local CORS policy'
assert_contains "${identity_script}" \
  'starting Yuheng Engine after Tianshu control plane is ready' \
  'Yuheng Engine must start after the final Tianshu provider restart'
assert_contains "${identity_script}" \
  'write_env "${file}" TIANQUAN_JIANSHEN_DEVELOPMENT_BOOTSTRAP_ENABLED false' \
  'the first Tianquan-Jianshen startup must defer topology bootstrap until Tianshu publication'
assert_contains "${identity_script}" \
  'write_env "${file}" TIANQUAN_JIANSHEN_RPC_ENABLED false' \
  'the first Tianquan-Jianshen startup must defer direct RPC runtime creation until Tianquan-Shoubing publication'
assert_contains "${identity_script}" \
  'write_env "${file}" TIANQUAN_JIANSHEN_RPC_CONSUMER_ENABLED false' \
  'the first Tianquan-Jianshen startup must keep direct RPC consumers disabled until Tianquan-Shoubing publication'
assert_contains "${identity_script}" \
  '--egon.tianquan-jianshen.development-bootstrap.enabled=false' \
  'the first Tianquan-Jianshen bootstrap phase must defer topology activation until Tianshu publication'
assert_contains "${identity_script}" \
  'write_env "${env_dir}/tianquan-jianshen.env" TIANQUAN_JIANSHEN_DEVELOPMENT_BOOTSTRAP_ENABLED true' \
  'the final Tianquan-Jianshen startup must enable topology bootstrap after Tianshu publication'
assert_contains "${identity_script}" \
  'write_env "${env_dir}/tianquan-jianshen.env" TIANQUAN_JIANSHEN_RPC_ENABLED true' \
  'the final Tianquan-Jianshen startup must enable direct RPC runtime after Tianquan-Shoubing publication'
assert_contains "${identity_script}" \
  'write_env "${env_dir}/tianquan-jianshen.env" TIANQUAN_JIANSHEN_RPC_CONSUMER_ENABLED true' \
  'the final Tianquan-Jianshen startup must enable direct RPC consumers after Tianquan-Shoubing publication'
assert_contains "${identity_script}" \
  'wait_ddc_rpc_provider_registration permission tianquan-shoubing egon.tianquan.shoubing.v1.IdentityDirectoryService tianquan-shoubing 1.0.0' \
  'Tianquan-Jianshen topology bootstrap must wait for the Tianquan-Shoubing RPC provider publication'
assert_contains "${identity_script}" \
  'stage "starting Tianquan-Jianshen topology bootstrap after Tianquan-Shoubing RPC publication"' \
  'Tianquan-Jianshen topology bootstrap must have an explicit post-publication stage'
assert_contains "${identity_script}" \
  'ddc_admin_access_token="$(user_access_token_for_tenant default)"' \
  'Tianshu registry polling must use a fresh USER Access Token after the Tianquan-Shoubing restart'
assert_contains "${identity_script}" 'clear_local_rbac3_snapshots' \
  'local startup must discard stale derived Tianquan-Jianshen snapshots before rebuilding them'
assert_contains "${identity_script}" \
  'write_env "${file}" TIANQUAN_JIANSHEN_DEVELOPMENT_AUTO_ACTIVATE_LOCAL_ADMIN_ROLES true' \
  'local Tianquan-Jianshen startup must activate the generated local administrator roles'
assert_contains "${identity_script}" \
  'write_env "${file}" TIANQUAN_SHOUBING_RPC_PROVIDER_REGISTRATION_MODE DISABLED' \
  'Tianquan-Shoubing bootstrap startup must keep RPC registration disabled until Tianshu is ready'
assert_contains "${identity_script}" \
  'write_env "${env_dir}/tianquan-shoubing.env" TIANQUAN_SHOUBING_RPC_PROVIDER_REGISTRATION_MODE REQUIRED' \
  'final Tianquan-Shoubing startup must require RPC provider registration in Tianshu'
assert_contains "${identity_script}" '[[ -s "${file}" ]] || return 0' \
  'identity shutdown must tolerate an already stopped process'
assert_contains "${repo_root}/scripts/unified-xingyuan/lib/common.sh" \
  '[[ -s "${pid_file}" ]] || return 0' \
  'platform shutdown must tolerate an already stopped process'

assert_service_config() {
  local relative_file="$1" service="$2" file="${repo_root}/$1"
  assert_contains "${file}" 'default: local' \
    "${service} must use the local profile when no profile is supplied"
  assert_contains "${file}" \
    "optional:file:\${UNIFIED_XINGYUAN_RUNTIME_DIR:target/local-unified-xingyuan}/env/${service}.properties" \
    "${service} must import its generated runtime properties"
}

assert_service_config \
  'egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application.yml' \
  tianquan-shoubing
assert_service_config \
  'egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/resources/application.yml' \
  tianquan-jianshen
assert_service_config \
  'egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/resources/application.yml' \
  yuheng-admin
assert_service_config \
  'egon-cola-xingyuan/egon-cola-yuheng/yuheng-biz-gateway/src/main/resources/application.yml' \
  yuheng-biz-gateway
ddc_config='egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/application.yml'
assert_service_config "${ddc_config}" tianshu
assert_contains "${repo_root}/${ddc_config}" \
  'classpath:META-INF/egon-cola-tianshu.properties' \
  'Tianshu must preserve its starter defaults import'

assert_vite_proxy() {
  local relative_file="$1" platform="$2"
  assert_contains "${repo_root}/${relative_file}" "'/oauth2':" \
    "${platform} auth requests must not fall through to Vite HTML"
  assert_contains "${repo_root}/${relative_file}" "ADMIN_PROXY ?? 'http://127.0.0.1:18180'" \
    "${platform} USER cookies must reach the authenticated Yuheng proxy"
}

assert_vite_proxy \
  'egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/vite.config.ts' \
  Tianquan-Shoubing
assert_vite_proxy \
  'egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/vite.config.ts' \
  Tianquan-Jianshen
assert_vite_proxy \
  'egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/vite.config.ts' \
  Yuheng
assert_vite_proxy \
  'egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/vite.config.ts' \
  Tianshu

prepare_script="${repo_root}/scripts/unified-xingyuan/prepare-local-stack.sh"
start_script="${repo_root}/scripts/unified-xingyuan/start-local-stack.sh"
live_login_test="${repo_root}/scripts/unified-xingyuan/test-live-frontend-login.sh"
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
assert_contains "${start_script}" 'test-live-frontend-login.sh' \
  'stack startup must prove fresh password login and authorization before success'
assert_contains "${identity_script}" 'publish_gateway_routes true' \
  'deferred startup must prepare HTTP routes before the unified MCP release'
assert_contains "${identity_script}" 'publish-yuheng-routes' \
  'platform startup must expose a post-Engine Yuheng route release command'
assert_contains "${identity_script}" 'wait_gateway_engine_provider_registration' \
  'platform Yuheng route release must wait for an online Engine provider lease'
assert_contains "${identity_script}" \
  'MOCK_BACKEND_BUILD_ID "$(local_build_id "${mock_jar}")"' \
  'mock backend reports must use a content-derived local build ID'
assert_contains "${platform_common_script}" 'unified_xingyuan_local_build_id()' \
  'unified platform fixtures must share content-derived local build IDs'
assert_contains "${platform_start_script}" \
  'MCP_TEST_PROVIDER_BUILD_ID' \
  'MCP provider reports must declare a local build ID'
assert_contains "${platform_start_script}" \
  '"$(unified_xingyuan_local_build_id "${mcp_provider_jar}")"' \
  'MCP provider reports must use a content-derived local build ID'
function_file="${temporary_dir}/publish-yuheng-routes.sh"
extract_function publish_gateway_routes "${function_file}"
deferred_return_line="$(grep -nF 'if [[ "${defer_release}" == "true" ]]' \
  "${function_file}" | cut -d: -f1)"
draft_validation_line="$(grep -nF 'validation="$(gateway_api POST' \
  "${function_file}" | cut -d: -f1)"
[[ -n "${deferred_return_line}" && -n "${draft_validation_line}" \
    && "${deferred_return_line}" -lt "${draft_validation_line}" ]] \
  || fail 'deferred startup must postpone full draft validation until MCP providers are online'
assert_contains "${live_login_test}" 'fresh Admin endpoint returned HTTP' \
  'frontend login contract must exercise fresh Yuheng JWT Admin endpoints'
assert_contains "${live_login_test}" 'fresh_cookie="${fresh_dir}/yuheng.cookies"' \
  'frontend login contract must use one Yuheng USER cookie jar'
assert_not_contains "${live_login_test}" 'Authorization: Bearer $(<"${default_token}")' \
  'frontend login contract must not forward the pre-generated USER Access Token'
assert_not_contains "${live_login_test}" 'fresh_dir}/tianquan-shoubing.access.jwt' \
  'frontend login contract must not create per-client USER token files'
assert_not_contains "${live_login_test}" '.access.jwt' \
  'frontend login contract must not persist per-client USER Access Token files'
assert_not_contains "${live_login_test}" '/oauth2/authorize' \
  'frontend login contract must not use Authorization Code flow'
assert_not_contains "${live_login_test}" 'grant_type=authorization_code' \
  'frontend login contract must not exchange Authorization Codes'
for application_code in tianquan-shoubing-admin tianquan-jianshen-admin yuheng-admin tianshu-admin mock-backend; do
  assert_contains "${live_login_test}" "\"${application_code}\"" \
    "frontend login contract must verify the ${application_code} role"
done
for role_code in \
  TIANQUAN_SHOUBING_LOCAL_ADMIN TIANQUAN_JIANSHEN_LOCAL_ADMIN YUHENG_LOCAL_ADMIN TIANSHU_LOCAL_ADMIN \
  MOCK_LOCAL_ADMIN MOCK_LOCAL_ENTRY; do
  assert_contains "${live_login_test}" "\"${role_code}\"" \
    "frontend login contract must verify the ${role_code} role code"
done
last_web_line="$(grep -nF 'start_admin_web tianshu-admin-web' \
  "${start_script}" | tail -1 | cut -d: -f1)"
success_line="$(grep -nF "printf 'Unified Xingyuan local stack is running" \
  "${start_script}" | tail -1 | cut -d: -f1)"
[[ "${last_web_line}" -lt "${success_line}" ]] \
  || fail 'startup success must be reported after all Web apps are running'

identity_runbook="${repo_root}/docs/runbooks/unified-identity-local.md"
operations_runbook="${repo_root}/docs/operations/unified-identity-mcp-local-runbook.md"
for runbook in "${identity_runbook}" "${operations_runbook}"; do
  assert_contains "${runbook}" 'prepare-local-stack.sh' \
    'runbook must document the one-time preparation command'
  assert_contains "${runbook}" 'egon-cola-tianquan-shoubing-admin-exec.jar' \
    'runbook must document direct Tianquan-Shoubing JAR startup'
  assert_contains "${runbook}" 'egon-cola-tianquan-jianshen-admin-exec.jar' \
    'runbook must document direct Tianquan-Jianshen JAR startup'
  assert_contains "${runbook}" 'yuheng-admin-exec.jar' \
    'runbook must document direct Yuheng Admin JAR startup'
  assert_contains "${runbook}" 'yuheng-biz-gateway-exec.jar' \
    'runbook must document direct Yuheng Engine JAR startup'
  assert_contains "${runbook}" 'egon-cola-tianshu-admin-exec.jar' \
    'runbook must document direct Tianshu JAR startup'
  assert_contains "${runbook}" 'npm run dev' \
    'runbook must document plain frontend startup'
done

verifier="${repo_root}/scripts/unified-xingyuan/verify-local-stack.sh"
assert_contains "${verifier}" 'verify_authenticated_json()' \
  'deep verification must provide a reusable authenticated JSON check'
for label in tianquan-shoubing-users tianquan-jianshen-roles yuheng-dashboard tianshu-configs; do
  assert_contains "${verifier}" "${label}" \
    "deep verification must cover ${label}"
done
assert_contains "${verifier}" 'admin-feature-matrix' \
  'sanitized evidence must include the Admin feature matrix'

function_file="${temporary_dir}/wait-admin-catalog.sh"
selector_file="${temporary_dir}/select-catalog.sh"
extract_function select_gateway_catalog_operations "${selector_file}"
source "${selector_file}"
selected_self_operations="$(jq -cn '[
  "GET /api/v1/auth/about",
  "GET /api/tianquan-jianshen/v1/auth/role-activation-candidates",
  "GET /api/tianquan-jianshen/v1/auth/role-activations",
  "PUT /api/tianquan-jianshen/v1/auth/role-activations",
  "POST /api/v1/auth/about",
  "GET /api/tianquan-jianshen/v1/iam/users",
  "GET /api/tianquan-jianshen/v1/auth/role-activations/admin"
] | [ .[] as $method | ["tianquan-jianshen", "yuheng-admin"][] as $app
  | {id:($app + ":" + $method),methodIdentity:$method,reportedApplication:$app,
     protocol:"HTTP",sourceType:"OPENAPI31",externalAccessible:true,lifecycleStatus:"ACTIVE"}]' \
  | select_gateway_catalog_operations)"
jq -e '[.[] | select(.securityType == "IDENTITY_PROTECTED") | .id] | sort == ([
  "tianquan-jianshen:GET /api/v1/auth/about",
  "tianquan-jianshen:GET /api/tianquan-jianshen/v1/auth/role-activation-candidates",
  "tianquan-jianshen:GET /api/tianquan-jianshen/v1/auth/role-activations",
  "tianquan-jianshen:PUT /api/tianquan-jianshen/v1/auth/role-activations"
] | sort)' <<<"${selected_self_operations}" >/dev/null \
  || fail 'only the four RBAC current-user bootstrap operations may use identity-only Yuheng policy'
jq -e 'all(.[] | select(.securityType != "IDENTITY_PROTECTED"); .securityType == "BUSINESS_PROTECTED")' \
  <<<"${selected_self_operations}" >/dev/null \
  || fail 'management endpoints, other applications, methods and path suffixes must retain business authorization'
selected_operations="$(jq -cn '[
  {id:"old",sourceType:"STARTER",methodIdentity:"GET /mcp/{plural:tools|resources}/{id}"},
  {id:"new",sourceType:"OPENAPI31",methodIdentity:"GET /mcp/{plural}/{id}"},
  {id:"manual",sourceType:"MANUAL",methodIdentity:"GET /custom"},
  {id:"legacy-only",sourceType:"STARTER",methodIdentity:"GET /existing"}
] | map(. + {reportedApplication:"yuheng-admin",protocol:"HTTP",externalAccessible:true,lifecycleStatus:"ACTIVE"})' \
  | select_gateway_catalog_operations)"
jq -e '[.[].id] | sort == ["legacy-only","manual","new"]' \
  <<<"${selected_operations}" >/dev/null \
  || fail 'OpenAPI routes must supersede duplicate legacy starter patterns without dropping manual or unmatched routes'

extract_function wait_gateway_catalog_for_app "${function_file}"
source "${function_file}"
printf '%s' 'admin-application' >"${temporary_dir}/admin-application.id"
gateway_application_id_file() { printf '%s/admin-application.id' "${temporary_dir}"; }
gateway_api() {
  printf 'query\n' >>"${temporary_dir}/admin-catalog-queries"
  if [[ "$(wc -l <"${temporary_dir}/admin-catalog-queries" | tr -d ' ')" == 1 ]]; then
    printf '%s' '{"operations":[{"protocol":"HTTP","lifecycleStatus":"ACTIVE","methodIdentity":"GET /old"}]}'
  else
    jq -cn '["GET /api/v1/yuheng/admin/openapi/sync-states",
      "GET /api/v1/yuheng/admin/operations/{operationId}/openapi",
      "GET /api/v1/yuheng/admin/openapi/snapshots/{snapshotId}/document"]
      | {operations:map({protocol:"HTTP",lifecycleStatus:"ACTIVE",methodIdentity:.})}'
  fi
}
sleep() { :; }
wait_gateway_catalog_for_app yuheng-admin
[[ "$(wc -l <"${temporary_dir}/admin-catalog-queries" | tr -d ' ')" == 2 ]] \
  || fail 'Admin catalog wait must reject a stale catalog without the OpenAPI query endpoints'
unset -f gateway_api gateway_application_id_file sleep wait_gateway_catalog_for_app

printf 'direct-run-contract: runtime properties adapter PASS\n'
