#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"
runtime_dir="${UNIFIED_IDENTITY_RUNTIME_DIR:-${repo_root}/.runtime/unified-identity}"
secret_dir="${runtime_dir}/secrets"
log_dir="${runtime_dir}/logs"
pid_dir="${runtime_dir}/pids"
env_dir="${runtime_dir}/env"

idp_url="${UNIFIED_IDENTITY_IDP_URL:-http://127.0.0.1:18120}"
idp_rpc_target="${UNIFIED_IDENTITY_IDP_RPC_TARGET:-dns:///127.0.0.1:18122}"
rbac3_url="${UNIFIED_IDENTITY_RBAC3_URL:-http://127.0.0.1:18130}"
gateway_admin_url="${UNIFIED_IDENTITY_GATEWAY_ADMIN_URL:-http://127.0.0.1:18140}"
ddc_url="${UNIFIED_IDENTITY_DDC_URL:-http://127.0.0.1:18150}"
ddc_rpc_target="${UNIFIED_IDENTITY_DDC_RPC_TARGET:-dns:///127.0.0.1:19080}"
mock_url="${UNIFIED_IDENTITY_MOCK_URL:-http://127.0.0.1:18160}"
gateway_url="${UNIFIED_IDENTITY_GATEWAY_URL:-http://127.0.0.1:18180}"

postgres_host="${UNIFIED_IDENTITY_POSTGRES_HOST:-127.0.0.1}"
postgres_port="${UNIFIED_IDENTITY_POSTGRES_PORT:-5432}"
postgres_user="${UNIFIED_IDENTITY_POSTGRES_USER:-postgres}"
postgres_database="${UNIFIED_IDENTITY_POSTGRES_MAINTENANCE_DATABASE:-postgres}"
postgres_password_file="${UNIFIED_IDENTITY_POSTGRES_PASSWORD_FILE:-}"

redis_host="${UNIFIED_IDENTITY_REDIS_HOST:-127.0.0.1}"
redis_port="${UNIFIED_IDENTITY_REDIS_PORT:-6379}"
redis_config_file="${UNIFIED_IDENTITY_REDIS_CONFIG_FILE:-/opt/homebrew/etc/redis.conf}"
redis_password_source="${UNIFIED_IDENTITY_REDIS_PASSWORD_FILE:-}"

idp_database="${UNIFIED_IDENTITY_IDP_DATABASE:-egon_identity_local}"
rbac3_database="${UNIFIED_IDENTITY_RBAC3_DATABASE:-egon_rbac3_unified_identity_local}"
gateway_database="${UNIFIED_IDENTITY_GATEWAY_DATABASE:-egon_gateway_local}"
ddc_database="${UNIFIED_IDENTITY_DDC_DATABASE:-egon_ddc_local}"
service_tenant_id="${UNIFIED_IDENTITY_SERVICE_TENANT_ID:-default}"
# USER tokens stay in the Gateway-managed cookie jar for the browser path.  These
# variables are deliberately process-local and are used only while bootstrapping
# or running an explicit command-line verification.
ddc_admin_access_token=""
pre_logout_access_token=""

idp_jar="${repo_root}/egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/target/egon-cola-platform-idp-admin-exec.jar"
rbac3_jar="${repo_root}/egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/target/egon-cola-platform-rbac3-admin-exec.jar"
gateway_admin_jar="${repo_root}/egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/target/egon-cola-platform-gateway-admin-exec.jar"
gateway_engine_jar="${repo_root}/egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/target/egon-cola-platform-gateway-engine-exec.jar"
ddc_jar="${repo_root}/egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/target/egon-cola-platform-dynamic-config-center-admin-exec.jar"
mock_jar="${repo_root}/egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-idp-backend/target/gateway-test-idp-backend-exec.jar"

usage() {
  cat <<'USAGE'
Usage: ./scripts/unified-identity-local.sh <command>

Commands:
  prepare  Check host dependencies, create named databases/secrets, and package jars
  start    Start and bootstrap DDC, IdP, RBAC3, Gateway, and the mock backend
  sync-local-credentials  Refresh local SERVICE credentials and USER cookie snapshots
  issue-user-token  Issue one local USER Access Token from explicit inputs
  verify   Execute the host-local unified identity acceptance checks
  status   Show exact managed process and health status
  stop     Gracefully stop only processes recorded by this harness
USAGE
}

fail() {
  echo "unified-identity-local: $*" >&2
  exit 1
}

stage() {
  printf '[unified-identity] %s\n' "$1"
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "missing prerequisite: $1"
}

local_build_id() {
  local jar="$1" digest
  [[ -s "${jar}" ]] || fail "missing executable jar for build identity: ${jar}"
  digest="$(openssl dgst -sha256 -r "${jar}" | awk '{print $1}')"
  [[ "${digest}" =~ ^[0-9a-f]{64}$ ]] \
    || fail "invalid executable jar digest: ${jar}"
  printf 'local-%s' "${digest:0:16}"
}

initialize_directories() {
  umask 077
  mkdir -p "${secret_dir}" "${log_dir}" "${pid_dir}" "${env_dir}"
  chmod 700 "${runtime_dir}" "${secret_dir}" "${log_dir}" "${pid_dir}" "${env_dir}"
}

random_secret() {
  openssl rand -base64 "$1" | tr -d '\n'
}

ensure_secret() {
  local file="$1" bytes="$2"
  if [[ ! -s "${file}" ]]; then
    random_secret "${bytes}" >"${file}"
  fi
  chmod 600 "${file}"
}

ensure_password() {
  local file="$1"
  if [[ ! -s "${file}" ]]; then
    printf 'Aa1!%s' "$(random_secret 18)" >"${file}"
  fi
  chmod 600 "${file}"
}

ensure_rsa_key_pair() {
  local stem="$1"
  if [[ ! -s "${stem}-private.pem" || ! -s "${stem}-public.pem" ]]; then
    openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 \
      -out "${stem}-private.pem" >/dev/null 2>&1
    openssl pkey -in "${stem}-private.pem" -pubout \
      -out "${stem}-public.pem" >/dev/null 2>&1
  fi
  chmod 600 "${stem}-private.pem" "${stem}-public.pem"
}

resolve_redis_password() {
  local target="${secret_dir}/redis.password" password=""
  if [[ -n "${redis_password_source}" ]]; then
    [[ -s "${redis_password_source}" ]] || fail "Redis password file is unreadable"
    password="$(<"${redis_password_source}")"
  elif [[ -r "${redis_config_file}" ]]; then
    password="$(awk '$1 == "requirepass" {print $2; exit}' "${redis_config_file}")"
    password="${password%\"}"
    password="${password#\"}"
  fi
  [[ -n "${password}" ]] || fail "Redis requires an explicit password file or readable requirepass configuration"
  printf '%s' "${password}" >"${target}"
  chmod 600 "${target}"
  REDISCLI_AUTH="${password}" redis-cli -h "${redis_host}" -p "${redis_port}" ping \
    2>/dev/null | grep -qx PONG || fail "Redis authentication failed"
}

postgres_password() {
  if [[ -n "${postgres_password_file}" ]]; then
    [[ -s "${postgres_password_file}" ]] || fail "PostgreSQL password file is unreadable"
    tr -d '\r\n' <"${postgres_password_file}"
  elif [[ -n "${UNIFIED_IDENTITY_POSTGRES_PASSWORD:-}" ]]; then
    printf '%s' "${UNIFIED_IDENTITY_POSTGRES_PASSWORD}"
  elif [[ -s "${secret_dir}/postgres.password" ]]; then
    tr -d '\r\n' <"${secret_dir}/postgres.password"
  else
    fail "PostgreSQL requires an explicit password or protected runtime secret"
  fi
}

resolve_postgres_password() {
  local target="${secret_dir}/postgres.password" password temporary
  password="$(postgres_password)"
  [[ -n "${password}" ]] \
    || fail "PostgreSQL password must not be empty"
  PGPASSWORD="${password}" psql -X -v ON_ERROR_STOP=1 \
    -h "${postgres_host}" -p "${postgres_port}" -U "${postgres_user}" \
    -d "${postgres_database}" -Atqc 'select 1' >/dev/null 2>&1 \
    || fail "PostgreSQL authentication failed"
  temporary="$(mktemp "${secret_dir}/postgres.password.XXXXXX")"
  chmod 600 "${temporary}"
  printf '%s' "${password}" >"${temporary}"
  mv -f "${temporary}" "${target}"
}

psql_command() {
  PGPASSWORD="$(postgres_password)" psql -X -v ON_ERROR_STOP=1 \
    -h "${postgres_host}" -p "${postgres_port}" -U "${postgres_user}" \
    -d "$1" "${@:2}"
}

validate_database_name() {
  [[ "$1" =~ ^[a-z][a-z0-9_]{0,62}$ ]] || fail "unsafe database name: $1"
}

ensure_database() {
  local database="$1"
  validate_database_name "${database}"
  if [[ "$(psql_command "${postgres_database}" -Atqc \
      "select count(*) from pg_database where datname = '${database}'")" == "0" ]]; then
    PGPASSWORD="$(postgres_password)" createdb \
      -h "${postgres_host}" -p "${postgres_port}" -U "${postgres_user}" \
      "${database}"
  fi
}

database_table_exists() {
  [[ "$(psql_command "$1" -Atqc "select to_regclass('$2') is not null")" == "t" ]]
}

database_row_exists() {
  [[ "$(psql_command "$1" -Atqc "$2")" != "0" ]]
}

base64url() {
  openssl base64 -A | tr '+/' '-_' | tr -d '='
}

write_pending_service_credential() {
  printf 'pending-idp-client-credentials' >"$1"
  chmod 600 "$1"
}

write_runtime_secrets() {
  ensure_password "${secret_dir}/idp-admin.password"
  ensure_secret "${secret_dir}/ddc-runtime.access-key" 18
  ensure_secret "${secret_dir}/ddc-runtime.secret" 32
  ensure_secret "${secret_dir}/ddc-registry.access-key" 18
  ensure_secret "${secret_dir}/ddc-registry.secret" 32
  ensure_secret "${secret_dir}/ddc-management.access-key" 18
  ensure_secret "${secret_dir}/ddc-management.secret" 32
  ensure_secret "${secret_dir}/gateway-master-key.base64" 32
  ensure_secret "${secret_dir}/rbac3-audit.secret" 32
  ensure_rsa_key_pair "${secret_dir}/idp"
  ensure_rsa_key_pair "${secret_dir}/rbac3"
  ensure_rsa_key_pair "${secret_dir}/ddc"
  ensure_rsa_key_pair "${secret_dir}/gateway-admin"
  ensure_rsa_key_pair "${secret_dir}/gateway-engine"
  ensure_rsa_key_pair "${secret_dir}/mock-backend"
  ensure_rsa_key_pair "${secret_dir}/mcp-provider"
  write_pending_service_credential "${secret_dir}/idp-admin.service.jwt"
  write_pending_service_credential "${secret_dir}/rbac3-admin.service.jwt"
  write_pending_service_credential "${secret_dir}/gateway-admin.service.jwt"
  write_pending_service_credential "${secret_dir}/gateway-admin-control-plane.service.jwt"
  write_pending_service_credential "${secret_dir}/gateway-engine.service.jwt"
  write_pending_service_credential "${secret_dir}/ddc-admin.service.jwt"
  write_pending_service_credential "${secret_dir}/mock-backend.service.jwt"
  write_pending_service_credential "${secret_dir}/mcp-provider.service.jwt"
}

oauth_service_token() {
  local client_id="$1" key_id="$2" key_stem="$3" output="$4"
  local resource="${5:-https://api.egon.internal/local/permission/rbac3}"
  local scopes="${6:-service:authorization:decide service:authorization:snapshot service:identity:resolve}"
  local now expires assertion_id header payload unsigned signature assertion
  local response_file status token_endpoint
  token_endpoint="${idp_url}/oauth2/token"
  now="$(date +%s)"
  expires="$((now + 60))"
  assertion_id="$(openssl rand -hex 16)"
  header="$(jq -cn --arg kid "${key_id}" \
    '{alg:"RS256",typ:"JWT",kid:$kid}')"
  payload="$(jq -cn \
    --arg client "${client_id}" \
    --arg audience "${token_endpoint}" \
    --arg assertion_id "${assertion_id}" \
    --argjson issued "${now}" --argjson expires "${expires}" \
    '{iss:$client,sub:$client,aud:[$audience],iat:$issued,nbf:$issued,
      exp:$expires,jti:$assertion_id}')"
  unsigned="$(printf '%s' "${header}" | base64url).$(printf '%s' "${payload}" | base64url)"
  signature="$(printf '%s' "${unsigned}" | openssl dgst -sha256 \
    -sign "${secret_dir}/${key_stem}-private.pem" | base64url)"
  assertion="${unsigned}.${signature}"
  response_file="$(mktemp "${runtime_dir}/service-token.XXXXXX")"
  status="$(curl -sS -o "${response_file}" -w '%{http_code}' -X POST \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode grant_type=client_credentials \
    --data-urlencode "client_id=${client_id}" \
    --data-urlencode \
      client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer \
    --data-urlencode "client_assertion=${assertion}" \
    --data-urlencode \
      "resource=${resource}" \
    --data-urlencode "tenant_id=${service_tenant_id}" \
    --data-urlencode "scope=${scopes}" \
    "${token_endpoint}")"
  [[ "${status}" == "200" ]] || fail \
    "IdP Client Credentials failed for ${client_id} with HTTP ${status}: $(<"${response_file}")"
  jq -er '.access_token' "${response_file}" >"${output}"
  rm -f "${response_file}"
  chmod 600 "${output}"
}

refresh_service_tokens() {
  oauth_service_token idp-service idp-local idp \
    "${secret_dir}/idp-admin.service.jwt"
  oauth_service_token rbac3-service rbac3-local rbac3 \
    "${secret_dir}/rbac3-admin.service.jwt"
  oauth_service_token gateway-admin-service gateway-admin-local gateway-admin \
    "${secret_dir}/gateway-admin.service.jwt"
  oauth_service_token gateway-admin-service gateway-admin-local gateway-admin \
    "${secret_dir}/gateway-admin-control-plane.service.jwt" \
    https://api.egon.internal/local/platform/gateway-admin \
    'gateway:read gateway:applications:write gateway:catalog:write gateway:credentials:write gateway:drafts:write gateway:groups:write gateway:mcp:approve gateway:mcp:read gateway:mcp:runtime:read gateway:mcp:test gateway:mcp:write gateway:releases:write'
  oauth_service_token gateway-engine-service gateway-engine-local gateway-engine \
    "${secret_dir}/gateway-engine.service.jwt"
  oauth_service_token ddc-service ddc-local ddc \
    "${secret_dir}/ddc-admin.service.jwt"
  oauth_service_token mock-backend-service mock-backend-local mock-backend \
    "${secret_dir}/mock-backend.service.jwt"
  oauth_service_token mcp-provider-service mcp-provider-local mcp-provider \
    "${secret_dir}/mcp-provider.service.jwt"
}

properties_escape() {
  local value="$1"
  value="${value//\\/\\\\}"
  value="${value//$'\t'/\\t}"
  value="${value//$'\r'/\\r}"
  value="${value//$'\n'/\\n}"
  printf '%s' "${value}"
}

java_property_key() {
  case "$1" in
    SERVER_PORT) printf 'server.port' ;;
    SPRING_PROFILES_ACTIVE) printf 'spring.profiles.active' ;;
    SPRING_DATASOURCE_URL) printf 'spring.datasource.url' ;;
    SPRING_DATASOURCE_USERNAME) printf 'spring.datasource.username' ;;
    SPRING_DATASOURCE_PASSWORD) printf 'spring.datasource.password' ;;
    SPRING_FLYWAY_ENABLED) printf 'spring.flyway.enabled' ;;
    EGON_COLA_COMPONENT_ID_MACHINE_ID)
      printf 'egon.cola.component.id.machine-id'
      ;;
    EGON_COLA_PLATFORM_RBAC3_RUNTIME_PASSWORD_FILE)
      printf 'egon.cola.platform.rbac3.runtime.password-file'
      ;;
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_CACHE_TTL)
      printf 'egon.cola.platform.rbac3.authorization.cache-ttl'
      ;;
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_MAXIMUM_JITTER)
      printf 'egon.cola.platform.rbac3.authorization.maximum-jitter'
      ;;
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_NEAR_CACHE_TTL)
      printf 'egon.cola.platform.rbac3.authorization.near-cache-ttl'
      ;;
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_ENABLED)
      printf 'egon.cola.platform.rbac3.authorization.service-token.enabled'
      ;;
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_TOKEN_ENDPOINT)
      printf 'egon.cola.platform.rbac3.authorization.service-token.token-endpoint'
      ;;
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_CLIENT_ID)
      printf 'egon.cola.platform.rbac3.authorization.service-token.client-id'
      ;;
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_KEY_ID)
      printf 'egon.cola.platform.rbac3.authorization.service-token.key-id'
      ;;
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_PRIVATE_KEY_FILE)
      printf 'egon.cola.platform.rbac3.authorization.service-token.private-key-file'
      ;;
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_RESOURCE_URI)
      printf 'egon.cola.platform.rbac3.authorization.service-token.resource-uri'
      ;;
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_SCOPES)
      printf 'egon.cola.platform.rbac3.authorization.service-token.scopes'
      ;;
    EGON_COLA_COMPONENT_DDC_ADMIN_REDIS_HOST)
      printf 'egon.cola.component.ddc.admin.redis.host'
      ;;
    EGON_COLA_COMPONENT_DDC_ADMIN_REDIS_PORT)
      printf 'egon.cola.component.ddc.admin.redis.port'
      ;;
    EGON_COLA_COMPONENT_DDC_ADMIN_REDIS_PASSWORD)
      printf 'egon.cola.component.ddc.admin.redis.password'
      ;;
    EGON_COLA_COMPONENT_DDC_ADMIN_REDIS_DATABASE)
      printf 'egon.cola.component.ddc.admin.redis.database'
      ;;
    EGON_COLA_COMPONENT_DDC_CONSISTENCY_FAIL_FAST)
      printf 'egon.cola.component.ddc.consistency.fail-fast'
      ;;
    EGON_COLA_COMPONENT_GATEWAY_ENGINE_GATEWAY_GROUP_CODE)
      printf 'egon.cola.component.gateway.engine.gateway-group-code'
      ;;
    EGON_COLA_COMPONENT_GATEWAY_ENGINE_ENV)
      printf 'egon.cola.component.gateway.engine.env'
      ;;
    EGON_COLA_COMPONENT_GATEWAY_ENGINE_NAMESPACE)
      printf 'egon.cola.component.gateway.engine.namespace'
      ;;
    EGON_COLA_COMPONENT_GATEWAY_ENGINE_NODE_ID)
      printf 'egon.cola.component.gateway.engine.node-id'
      ;;
    EGON_COLA_COMPONENT_GATEWAY_ENGINE_INSTANCE_ID)
      printf 'egon.cola.component.gateway.engine.instance-id'
      ;;
    EGON_COLA_COMPONENT_GATEWAY_ENGINE_DATA_DIRECTORY)
      printf 'egon.cola.component.gateway.engine.data-directory'
      ;;
    EGON_COLA_COMPONENT_GATEWAY_ENGINE_HTTP_PUBLIC_PORT)
      printf 'egon.cola.component.gateway.engine.http.public-port'
      ;;
    EGON_COLA_COMPONENT_GATEWAY_ENGINE_HTTP_INTERNAL_PORT)
      printf 'egon.cola.component.gateway.engine.http.internal-port'
      ;;
    EGON_COLA_COMPONENT_GATEWAY_PROVIDER_HTTP_FAIL_FAST)
      printf 'egon.cola.component.ddc.registry.http.fail-fast'
      ;;
    GATEWAY_ADMIN_DDC_ENABLED) printf 'gateway.admin.ddc.enabled' ;;
    GATEWAY_ADMIN_SECRETS_MASTER_KEY_BASE64)
      printf 'gateway.admin.secrets.master-key-base64'
      ;;
    GATEWAY_ADMIN_DEFINITION_RECONCILE_DELAY)
      printf 'gateway.admin.definition-reconcile-delay'
      ;;
    *) printf '%s' "$1" ;;
  esac
}

write_env() {
  local file="$1" key="$2" value="$3" properties_file property_key
  properties_file="${file%.env}.properties"
  property_key="$(java_property_key "${key}")"
  printf '%s=%q\n' "${key}" "${value}" >>"${file}"
  printf '%s=%s\n' "${property_key}" \
    "$(properties_escape "${value}")" >>"${properties_file}"
}

new_env_file() {
  local file="${env_dir}/$1.env" properties_file
  properties_file="${file%.env}.properties"
  : >"${file}"
  : >"${properties_file}"
  chmod 600 "${file}" "${properties_file}"
  printf '%s' "${file}"
}

common_identity_env() {
  local file="$1"
  write_env "${file}" SPRING_PROFILES_ACTIVE local
  write_env "${file}" UNIFIED_IDENTITY_ENABLED true
  write_env "${file}" IDP_ADMISSION_RPC_DEVELOPMENT_PLAINTEXT true
  write_env "${file}" IDP_OAUTH_ISSUER "${idp_url}"
  write_env "${file}" IDP_JWK_SET_URI "${idp_url}/oauth2/jwks"
  write_env "${file}" RBAC3_AUTHORIZATION_ENDPOINT "${rbac3_url}"
  write_env "${file}" EGON_COLA_PLATFORM_RBAC3_RUNTIME_PASSWORD_FILE \
    "${secret_dir}/redis.password"
  write_env "${file}" EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_CACHE_TTL 1s
  write_env "${file}" EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_MAXIMUM_JITTER 0s
  write_env "${file}" EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_NEAR_CACHE_TTL 0s
}

write_tenant_aware_rbac3_service_token_env() {
  local file="$1" client_id="$2" key_id="$3" private_key_file="$4"
  write_env "${file}" \
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_ENABLED true
  write_env "${file}" \
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_TOKEN_ENDPOINT \
    "${idp_url}/oauth2/token"
  write_env "${file}" \
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_CLIENT_ID \
    "${client_id}"
  write_env "${file}" \
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_KEY_ID "${key_id}"
  write_env "${file}" \
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_PRIVATE_KEY_FILE \
    "${private_key_file}"
  write_env "${file}" \
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_RESOURCE_URI \
    https://api.egon.internal/local/permission/rbac3
  write_env "${file}" \
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_SCOPES \
    'service:authorization:decide service:authorization:snapshot service:identity:resolve'
}

write_service_env_files() {
  local redis_password postgres_password_value file
  redis_password="$(<"${secret_dir}/redis.password")"
  postgres_password_value="$(postgres_password)"

  file="$(new_env_file ddc)"
  common_identity_env "${file}"
  write_tenant_aware_rbac3_service_token_env "${file}" \
    ddc-service ddc-local "${secret_dir}/ddc-private.pem"
  write_env "${file}" SERVER_PORT 18150
  write_env "${file}" SPRING_DATASOURCE_URL "jdbc:postgresql://${postgres_host}:${postgres_port}/${ddc_database}"
  write_env "${file}" SPRING_DATASOURCE_USERNAME "${postgres_user}"
  write_env "${file}" SPRING_DATASOURCE_PASSWORD "${postgres_password_value}"
  write_env "${file}" EGON_COLA_COMPONENT_ID_MACHINE_ID 31
  write_env "${file}" EGON_COLA_COMPONENT_DDC_ADMIN_REDIS_HOST "${redis_host}"
  write_env "${file}" EGON_COLA_COMPONENT_DDC_ADMIN_REDIS_PORT "${redis_port}"
  write_env "${file}" EGON_COLA_COMPONENT_DDC_ADMIN_REDIS_PASSWORD "${redis_password}"
  write_env "${file}" EGON_COLA_COMPONENT_DDC_ADMIN_REDIS_DATABASE 10
  write_env "${file}" DDC_AUTHORIZATION_REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
  write_env "${file}" DDC_AUTHORIZATION_REDIS_DATABASE 8
  write_env "${file}" DDC_ADMIN_JWT_ISSUER "${idp_url}"
  write_env "${file}" DDC_RESOURCE_SERVER_ID platform-ddc-local
  write_env "${file}" DDC_RESOURCE_URI \
    https://api.egon.internal/local/platform/ddc
  write_env "${file}" DDC_RESOURCE_ADMISSION_RPC_TARGET \
    "${idp_rpc_target}"
  write_env "${file}" DDC_ADMIN_JWT_AUDIENCE \
    https://api.egon.internal/local/platform/ddc
  write_env "${file}" DDC_ADMIN_JWT_JWK_SET_URI "${idp_url}/oauth2/jwks"
  write_env "${file}" DDC_RPC_PORT 19080
  write_env "${file}" DDC_RPC_DEVELOPMENT_PLAINTEXT true
  write_env "${file}" DDC_RPC_RUNTIME_ACCESS_KEY "$(<"${secret_dir}/ddc-runtime.access-key")"
  write_env "${file}" DDC_RPC_RUNTIME_SECRET_KEY "$(<"${secret_dir}/ddc-runtime.secret")"
  write_env "${file}" DDC_RPC_REGISTRY_ACCESS_KEY "$(<"${secret_dir}/ddc-registry.access-key")"
  write_env "${file}" DDC_RPC_REGISTRY_SECRET_KEY "$(<"${secret_dir}/ddc-registry.secret")"
  write_env "${file}" DDC_RPC_MANAGEMENT_ACCESS_KEY "$(<"${secret_dir}/ddc-management.access-key")"
  write_env "${file}" DDC_RPC_MANAGEMENT_SECRET_KEY "$(<"${secret_dir}/ddc-management.secret")"
  write_env "${file}" DDC_GATEWAY_REPORTING_ENABLED false
  write_env "${file}" GATEWAY_ADMIN_BASE_URL "${gateway_admin_url}"
  write_env "${file}" DDC_RESOURCE_BIZ_CODE platform
  write_env "${file}" DDC_RESOURCE_APP_CODE ddc
  write_env "${file}" DDC_DECLARED_HOSTS 127.0.0.1
  write_env "${file}" GATEWAY_REPORT_STATE_FILE "${runtime_dir}/ddc-gateway-report.json"

  file="$(new_env_file idp)"
  common_identity_env "${file}"
  write_tenant_aware_rbac3_service_token_env "${file}" \
    idp-service idp-local "${secret_dir}/idp-private.pem"
  write_env "${file}" IDP_POSTGRES_URL "jdbc:postgresql://${postgres_host}:${postgres_port}/${idp_database}"
  write_env "${file}" IDP_POSTGRES_USER "${postgres_user}"
  write_env "${file}" IDP_POSTGRES_PASSWORD "${postgres_password_value}"
  write_env "${file}" IDP_REDIS_HOST "${redis_host}"
  write_env "${file}" IDP_REDIS_PORT "${redis_port}"
  write_env "${file}" IDP_REDIS_PASSWORD "${redis_password}"
  write_env "${file}" IDP_REDIS_DATABASE 8
  write_env "${file}" IDP_AUTHORIZATION_REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
  write_env "${file}" IDP_AUTHORIZATION_REDIS_DATABASE 8
  write_env "${file}" IDP_ADVERTISED_PORT 18120
  write_env "${file}" IDP_OAUTH_LOGIN_URI http://127.0.0.1:18121/login
  write_env "${file}" IDP_REFRESH_COOKIE_SECURE false
  write_env "${file}" IDP_SIGNING_KEY_KID idp-local
  write_env "${file}" IDP_SIGNING_PRIVATE_KEY_FILE "${secret_dir}/idp-private.pem"
  write_env "${file}" IDP_SIGNING_PUBLIC_KEY_FILE "${secret_dir}/idp-public.pem"
  write_env "${file}" IDP_RBAC3_BASE_URL "${rbac3_url}"
  write_tenant_aware_rbac3_service_token_env "${file}" \
    idp-service idp-local "${secret_dir}/idp-private.pem"
  write_env "${file}" IDP_RBAC3_SERVICE_CLIENT_ID idp-service
  write_env "${file}" IDP_RBAC3_SERVICE_KEY_ID idp-local
  write_env "${file}" IDP_RBAC3_SERVICE_PRIVATE_KEY_FILE \
    "${secret_dir}/idp-private.pem"
  write_env "${file}" IDP_RBAC3_RESOURCE_URI \
    https://api.egon.internal/local/permission/rbac3
  write_env "${file}" IDP_RBAC3_SERVICE_TENANT_ID "${service_tenant_id}"
  write_env "${file}" IDP_DEVELOPMENT_RBAC3_SERVICE_TENANT_ID \
    "${service_tenant_id}"
  write_env "${file}" IDP_DEVELOPMENT_RBAC3_SERVICE_TENANT_IDS \
    "${service_tenant_id}"
  write_env "${file}" IDP_RBAC3_SERVICE_SCOPES \
    'service:authorization:decide service:authorization:snapshot service:identity:resolve'
  write_env "${file}" IDP_SNOWFLAKE_MACHINE_ID 32
  write_env "${file}" IDP_DEVELOPMENT_BOOTSTRAP_ENABLED true
  write_env "${file}" IDP_DEVELOPMENT_BOOTSTRAP_KEY_DIRECTORY \
    "${secret_dir}"
  write_env "${file}" IDP_BOOTSTRAP_PASSWORD_FILE "${secret_dir}/idp-admin.password"
  write_env "${file}" IDP_DDC_ENABLED true
  write_env "${file}" EGON_COLA_COMPONENT_DDC_CONSISTENCY_FAIL_FAST false
  write_env "${file}" IDP_HTTP_PROVIDER_ENABLED true
  write_env "${file}" IDP_RPC_PORT 18122
  write_env "${file}" IDP_RPC_DEVELOPMENT_PLAINTEXT true
  write_env "${file}" IDP_RESOURCE_SERVER_ID permission-idp-local
  write_env "${file}" IDP_RESOURCE_URI \
    https://api.egon.internal/local/permission/idp
  write_env "${file}" IDP_RESOURCE_MANAGEMENT_CLIENT_ID idp-service
  write_env "${file}" IDP_RESOURCE_MANAGEMENT_KEY_ID idp-local
  write_env "${file}" IDP_RESOURCE_MANAGEMENT_PRIVATE_KEY_FILE \
    "${secret_dir}/idp-private.pem"
  write_env "${file}" IDP_RESOURCE_ADMISSION_RPC_TARGET \
    "${idp_rpc_target}"
  write_env "${file}" \
    EGON_COLA_COMPONENT_GATEWAY_PROVIDER_HTTP_FAIL_FAST false
  write_env "${file}" IDP_INSTANCE_ID idp-local-1
  write_env "${file}" IDP_ADVERTISED_HOST 127.0.0.1
  write_env "${file}" DDC_BIZ_CODE permission
  write_env "${file}" DDC_APP_CODE idp
  write_env "${file}" DEPLOYMENT_ENV local
  write_env "${file}" DEPLOYMENT_NAMESPACE default
  write_env "${file}" DDC_RPC_TARGET "${ddc_rpc_target}"
  write_env "${file}" DDC_RPC_DEVELOPMENT_PLAINTEXT true
  write_env "${file}" DDC_RPC_RUNTIME_ACCESS_KEY "$(<"${secret_dir}/ddc-runtime.access-key")"
  write_env "${file}" DDC_RPC_RUNTIME_SECRET_KEY "$(<"${secret_dir}/ddc-runtime.secret")"
  write_env "${file}" DDC_RPC_REGISTRY_ACCESS_KEY "$(<"${secret_dir}/ddc-registry.access-key")"
  write_env "${file}" DDC_RPC_REGISTRY_SECRET_KEY "$(<"${secret_dir}/ddc-registry.secret")"
  write_env "${file}" DDC_REGISTRY_REDIS_HOST "${redis_host}"
  write_env "${file}" DDC_REGISTRY_REDIS_PORT "${redis_port}"
  write_env "${file}" DDC_REGISTRY_REDIS_PASSWORD "${redis_password}"
  write_env "${file}" DDC_REGISTRY_REDIS_DATABASE 10
  write_env "${file}" IDP_GATEWAY_REPORTING_ENABLED false
  write_env "${file}" GATEWAY_ADMIN_BASE_URL "${gateway_admin_url}"
  write_env "${file}" IDP_RESOURCE_BIZ_CODE permission
  write_env "${file}" IDP_RESOURCE_APP_CODE idp
  write_env "${file}" IDP_DECLARED_HOSTS 127.0.0.1
  write_env "${file}" GATEWAY_REPORT_STATE_FILE "${runtime_dir}/idp-gateway-report.json"

  file="$(new_env_file rbac3)"
  common_identity_env "${file}"
  write_env "${file}" RBAC3_POSTGRES_URL "jdbc:postgresql://${postgres_host}:${postgres_port}/${rbac3_database}"
  write_env "${file}" RBAC3_POSTGRES_USER "${postgres_user}"
  write_env "${file}" RBAC3_POSTGRES_PASSWORD "${postgres_password_value}"
  write_env "${file}" RBAC3_ADVERTISED_PORT 18130
  write_env "${file}" RBAC3_ADVERTISED_HOST 127.0.0.1
  write_env "${file}" RBAC3_INSTANCE_ID rbac3-local-1
  write_env "${file}" RBAC3_ARTIFACT_VERSION local
  write_env "${file}" RBAC3_DDC_ENABLED true
  write_env "${file}" EGON_COLA_COMPONENT_DDC_CONSISTENCY_FAIL_FAST false
  write_env "${file}" RBAC3_HTTP_PROVIDER_ENABLED true
  write_env "${file}" RBAC3_RESOURCE_SERVER_ID permission-rbac3-local
  write_env "${file}" RBAC3_RESOURCE_URI \
    https://api.egon.internal/local/permission/rbac3
  write_env "${file}" RBAC3_RESOURCE_MANAGEMENT_CLIENT_ID rbac3-service
  write_env "${file}" RBAC3_RESOURCE_MANAGEMENT_KEY_ID rbac3-local
  write_env "${file}" RBAC3_RESOURCE_MANAGEMENT_PRIVATE_KEY_FILE \
    "${secret_dir}/rbac3-private.pem"
  write_env "${file}" RBAC3_RESOURCE_ADMISSION_RPC_TARGET \
    "${idp_rpc_target}"
  write_env "${file}" \
    EGON_COLA_COMPONENT_GATEWAY_PROVIDER_HTTP_FAIL_FAST false
  write_env "${file}" DDC_BIZ_CODE permission
  write_env "${file}" DDC_APP_CODE rbac3
  write_env "${file}" DEPLOYMENT_ENV local
  write_env "${file}" DEPLOYMENT_NAMESPACE default
  write_env "${file}" DDC_RPC_TARGET "${ddc_rpc_target}"
  write_env "${file}" DDC_RPC_DEVELOPMENT_PLAINTEXT true
  write_env "${file}" DDC_RPC_RUNTIME_ACCESS_KEY "$(<"${secret_dir}/ddc-runtime.access-key")"
  write_env "${file}" DDC_RPC_RUNTIME_SECRET_KEY "$(<"${secret_dir}/ddc-runtime.secret")"
  write_env "${file}" DDC_RPC_REGISTRY_ACCESS_KEY "$(<"${secret_dir}/ddc-registry.access-key")"
  write_env "${file}" DDC_RPC_REGISTRY_SECRET_KEY "$(<"${secret_dir}/ddc-registry.secret")"
  write_env "${file}" DDC_REGISTRY_REDIS_HOST "${redis_host}"
  write_env "${file}" DDC_REGISTRY_REDIS_PORT "${redis_port}"
  write_env "${file}" DDC_REGISTRY_REDIS_PASSWORD "${redis_password}"
  write_env "${file}" DDC_REGISTRY_REDIS_DATABASE 10
  write_env "${file}" RBAC3_AUTHORIZATION_REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
  write_env "${file}" RBAC3_AUTHORIZATION_REDIS_DATABASE 8
  write_env "${file}" RBAC3_RUNTIME_REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
  write_env "${file}" RBAC3_RUNTIME_REDIS_DATABASE 8
  write_env "${file}" RBAC3_RUNTIME_REDIS_PASSWORD_FILE "${secret_dir}/redis.password"
  write_env "${file}" RBAC3_AUTHORIZATION_SERVICE_TOKEN_ENABLED true
  write_env "${file}" RBAC3_AUTHORIZATION_SERVICE_TOKEN_TOKEN_ENDPOINT \
    "${idp_url}/oauth2/token"
  write_env "${file}" RBAC3_AUTHORIZATION_SERVICE_TOKEN_CLIENT_ID \
    rbac3-service
  write_env "${file}" RBAC3_AUTHORIZATION_SERVICE_TOKEN_KEY_ID rbac3-local
  write_env "${file}" RBAC3_AUTHORIZATION_SERVICE_TOKEN_PRIVATE_KEY_FILE \
    "${secret_dir}/rbac3-private.pem"
  write_env "${file}" RBAC3_AUTHORIZATION_SERVICE_TOKEN_RESOURCE_URI \
    https://api.egon.internal/local/permission/rbac3
  write_env "${file}" RBAC3_AUTHORIZATION_SERVICE_TOKEN_SCOPES \
    'service:authorization:decide service:authorization:snapshot service:identity:resolve'
  write_env "${file}" RBAC3_AUDIT_CURSOR_SECRET_FILE "${secret_dir}/rbac3-audit.secret"
  write_env "${file}" RBAC3_SNOWFLAKE_MACHINE_ID 33
  write_env "${file}" RBAC3_DEVELOPMENT_BOOTSTRAP_ENABLED true
  write_env "${file}" RBAC3_DEVELOPMENT_AUTO_ACTIVATE_LOCAL_ADMIN_ROLES true
  write_env "${file}" RBAC3_DEVELOPMENT_TENANT_CODES default,tenant-b
  write_env "${file}" RBAC3_DEVELOPMENT_USERNAME alice
  write_env "${file}" SPRING_FLYWAY_ENABLED true
  write_env "${file}" RBAC3_GATEWAY_REPORTING_ENABLED false
  write_env "${file}" GATEWAY_ADMIN_BASE_URL "${gateway_admin_url}"
  write_env "${file}" RBAC3_RESOURCE_BIZ_CODE permission
  write_env "${file}" RBAC3_RESOURCE_APP_CODE rbac3
  write_env "${file}" RBAC3_DECLARED_HOSTS 127.0.0.1
  write_env "${file}" GATEWAY_REPORT_STATE_FILE "${runtime_dir}/rbac3-gateway-report.json"

  file="$(new_env_file gateway-admin)"
  common_identity_env "${file}"
  write_tenant_aware_rbac3_service_token_env "${file}" \
    gateway-admin-service gateway-admin-local \
    "${secret_dir}/gateway-admin-private.pem"
  write_env "${file}" SERVER_PORT 18140
  write_env "${file}" SPRING_DATASOURCE_URL "jdbc:postgresql://${postgres_host}:${postgres_port}/${gateway_database}"
  write_env "${file}" SPRING_DATASOURCE_USERNAME "${postgres_user}"
  write_env "${file}" SPRING_DATASOURCE_PASSWORD "${postgres_password_value}"
  write_env "${file}" EGON_COLA_COMPONENT_ID_MACHINE_ID 34
  write_env "${file}" GATEWAY_AUTHORIZATION_REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
  write_env "${file}" GATEWAY_AUTHORIZATION_REDIS_DATABASE 8
  write_env "${file}" GATEWAY_ADMIN_RESOURCE_SERVER_ID \
    platform-gateway-admin-local
  write_env "${file}" GATEWAY_ADMIN_RESOURCE_URI \
    https://api.egon.internal/local/platform/gateway-admin
  write_env "${file}" GATEWAY_ADMIN_RESOURCE_MANAGEMENT_CLIENT_ID \
    gateway-admin-service
  write_env "${file}" GATEWAY_ADMIN_RESOURCE_MANAGEMENT_KEY_ID \
    gateway-admin-local
  write_env "${file}" GATEWAY_ADMIN_RESOURCE_MANAGEMENT_PRIVATE_KEY_FILE \
    "${secret_dir}/gateway-admin-private.pem"
  write_env "${file}" GATEWAY_ADMIN_RESOURCE_ADMISSION_RPC_TARGET \
    "${idp_rpc_target}"
  write_env "${file}" GATEWAY_ADMIN_INSTANCE_ID gateway-admin-local-1
  write_env "${file}" GATEWAY_ADMIN_SECRETS_MASTER_KEY_BASE64 "$(<"${secret_dir}/gateway-master-key.base64")"
  write_env "${file}" GATEWAY_MCP_ARTIFACT_ROOT "${runtime_dir}/mcp-artifacts"
  write_env "${file}" GATEWAY_ADMIN_DDC_ENABLED true
  write_env "${file}" DDC_RPC_TARGET "${ddc_rpc_target}"
  write_env "${file}" DDC_RPC_DEVELOPMENT_PLAINTEXT true
  write_env "${file}" DDC_RPC_MANAGEMENT_ACCESS_KEY "$(<"${secret_dir}/ddc-management.access-key")"
  write_env "${file}" DDC_RPC_MANAGEMENT_SECRET_KEY "$(<"${secret_dir}/ddc-management.secret")"
  write_env "${file}" GATEWAY_ADMIN_DDC_TARGET_BIZ_CODE identity
  write_env "${file}" GATEWAY_ADMIN_DDC_TARGET_APP_CODE gateway-engine-default
  write_env "${file}" GATEWAY_ADMIN_DEFINITION_RECONCILE_DELAY 1000
  write_env "${file}" GATEWAY_ADMIN_GATEWAY_REPORTING_ENABLED false
  write_env "${file}" GATEWAY_ADMIN_BASE_URL "${gateway_admin_url}"
  write_env "${file}" GATEWAY_ADMIN_RESOURCE_BIZ_CODE platform
  write_env "${file}" GATEWAY_ADMIN_RESOURCE_APP_CODE gateway-admin
  write_env "${file}" GATEWAY_ADMIN_DECLARED_HOSTS 127.0.0.1
  write_env "${file}" GATEWAY_REPORT_STATE_FILE "${runtime_dir}/gateway-admin-gateway-report.json"

  file="$(new_env_file mock-backend)"
  common_identity_env "${file}"
  write_tenant_aware_rbac3_service_token_env "${file}" \
    mock-backend-service mock-backend-local \
    "${secret_dir}/mock-backend-private.pem"
  write_env "${file}" MOCK_BACKEND_PORT 18160
  write_env "${file}" MOCK_BACKEND_REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
  write_env "${file}" MOCK_BACKEND_REDIS_DATABASE 8
  write_env "${file}" MOCK_BACKEND_RESOURCE_MANAGEMENT_PRIVATE_KEY_FILE \
    "${secret_dir}/mock-backend-private.pem"
  write_env "${file}" MOCK_BACKEND_RESOURCE_ADMISSION_RPC_TARGET \
    "${idp_rpc_target}"
  write_env "${file}" MOCK_BACKEND_DDC_ENABLED true
  write_env "${file}" DDC_BIZ_CODE identity
  write_env "${file}" DDC_RPC_TARGET "${ddc_rpc_target}"
  write_env "${file}" DDC_RPC_DEVELOPMENT_PLAINTEXT true
  write_env "${file}" DDC_RPC_RUNTIME_ACCESS_KEY "$(<"${secret_dir}/ddc-runtime.access-key")"
  write_env "${file}" DDC_RPC_RUNTIME_SECRET_KEY "$(<"${secret_dir}/ddc-runtime.secret")"
  write_env "${file}" DDC_RPC_REGISTRY_ACCESS_KEY "$(<"${secret_dir}/ddc-registry.access-key")"
  write_env "${file}" DDC_RPC_REGISTRY_SECRET_KEY "$(<"${secret_dir}/ddc-registry.secret")"
  write_env "${file}" DDC_REGISTRY_REDIS_HOST "${redis_host}"
  write_env "${file}" DDC_REGISTRY_REDIS_PORT "${redis_port}"
  write_env "${file}" DDC_REGISTRY_REDIS_PASSWORD "${redis_password}"
  write_env "${file}" DDC_REGISTRY_REDIS_DATABASE 10
  write_env "${file}" MOCK_BACKEND_GATEWAY_REPORTING_ENABLED true
  write_env "${file}" GATEWAY_ADMIN_BASE_URL "${gateway_admin_url}"
  write_env "${file}" GATEWAY_REPORT_STATE_FILE "${runtime_dir}/mock-backend-gateway-report.json"

  file="$(new_env_file gateway-engine)"
  write_tenant_aware_rbac3_service_token_env "${file}" \
    gateway-engine-service gateway-engine-local \
    "${secret_dir}/gateway-engine-private.pem"
  write_env "${file}" SERVER_PORT 18182
  write_env "${file}" IDP_OAUTH_ISSUER "${idp_url}"
  write_env "${file}" IDP_JWK_SET_URI "${idp_url}/oauth2/jwks"
  write_env "${file}" GATEWAY_ENGINE_RESOURCE_SERVER_ID \
    identity-gateway-engine-default-local
  write_env "${file}" GATEWAY_ENGINE_RESOURCE_URI \
    https://api.egon.internal/local/identity/gateway-engine-default
  write_env "${file}" GATEWAY_ENGINE_RESOURCE_MANAGEMENT_CLIENT_ID \
    gateway-engine-service
  write_env "${file}" GATEWAY_ENGINE_RESOURCE_MANAGEMENT_KEY_ID \
    gateway-engine-local
  write_env "${file}" GATEWAY_ENGINE_RESOURCE_MANAGEMENT_PRIVATE_KEY_FILE \
    "${secret_dir}/gateway-engine-private.pem"
  write_env "${file}" GATEWAY_ENGINE_RESOURCE_ADMISSION_RPC_TARGET \
    "${idp_rpc_target}"
  write_env "${file}" IDP_ADMISSION_RPC_DEVELOPMENT_PLAINTEXT true
  write_env "${file}" GATEWAY_MCP_TASK_SERVICE_TOKEN_ENABLED true
  write_env "${file}" GATEWAY_MCP_TASK_SERVICE_TOKEN_ENDPOINT \
    "${idp_url}/oauth2/token"
  write_env "${file}" GATEWAY_MCP_TASK_SERVICE_TOKEN_CLIENT_ID \
    gateway-engine-service
  write_env "${file}" GATEWAY_MCP_TASK_SERVICE_TOKEN_KEY_ID \
    gateway-engine-local
  write_env "${file}" GATEWAY_MCP_TASK_SERVICE_TOKEN_PRIVATE_KEY_FILE \
    "${secret_dir}/gateway-engine-private.pem"
  write_env "${file}" GATEWAY_MCP_TASK_SERVICE_TOKEN_SCOPES \
    mcp:operation:invoke
  write_env "${file}" IDP_REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
  write_env "${file}" IDP_REDIS_DATABASE 8
  write_env "${file}" IDP_REDIS_PASSWORD_FILE "${secret_dir}/redis.password"
  write_env "${file}" GATEWAY_POSTGRES_URL "jdbc:postgresql://${postgres_host}:${postgres_port}/${gateway_database}"
  write_env "${file}" GATEWAY_POSTGRES_USER "${postgres_user}"
  write_env "${file}" GATEWAY_POSTGRES_PASSWORD "${postgres_password_value}"
  write_env "${file}" GATEWAY_MCP_ARTIFACT_ROOT "${runtime_dir}/mcp-artifacts"
  write_env "${file}" GATEWAY_MCP_REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
  write_env "${file}" GATEWAY_MCP_REDIS_DATABASE 8
  write_env "${file}" GATEWAY_MCP_REDIS_PASSWORD "${redis_password}"
  write_env "${file}" GATEWAY_MCP_RBAC3_ENABLED true
  write_env "${file}" GATEWAY_MCP_RBAC3_SYSTEM_CODE mock-backend
  write_env "${file}" GATEWAY_MCP_RBAC3_REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
  write_env "${file}" GATEWAY_MCP_RBAC3_REDIS_DATABASE 8
  write_env "${file}" GATEWAY_MCP_RBAC3_REDIS_PASSWORD_FILE "${secret_dir}/redis.password"
  write_env "${file}" GATEWAY_MCP_RBAC3_AUTHORIZATION_ENDPOINT "${rbac3_url}"
  write_env "${file}" EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_CACHE_TTL 1s
  write_env "${file}" EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_MAXIMUM_JITTER 0s
  write_env "${file}" EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_NEAR_CACHE_TTL 0s
  write_env "${file}" EGON_COLA_COMPONENT_ID_MACHINE_ID 35
  write_env "${file}" DDC_ENABLED true
  write_env "${file}" DDC_BIZ_CODE identity
  write_env "${file}" DDC_APP_CODE gateway-engine-default
  write_env "${file}" DDC_ENV local
  write_env "${file}" DDC_RPC_TARGET "${ddc_rpc_target}"
  write_env "${file}" DDC_RPC_DEVELOPMENT_PLAINTEXT true
  write_env "${file}" DDC_RPC_RUNTIME_ACCESS_KEY "$(<"${secret_dir}/ddc-runtime.access-key")"
  write_env "${file}" DDC_RPC_RUNTIME_SECRET_KEY "$(<"${secret_dir}/ddc-runtime.secret")"
  write_env "${file}" DDC_RPC_REGISTRY_ACCESS_KEY "$(<"${secret_dir}/ddc-registry.access-key")"
  write_env "${file}" DDC_RPC_REGISTRY_SECRET_KEY "$(<"${secret_dir}/ddc-registry.secret")"
  write_env "${file}" DDC_REDIS_HOST "${redis_host}"
  write_env "${file}" DDC_REDIS_PORT "${redis_port}"
  write_env "${file}" DDC_REDIS_PASSWORD "${redis_password}"
  write_env "${file}" DDC_REDIS_DATABASE 10
  write_env "${file}" EGON_COLA_COMPONENT_DDC_CONSISTENCY_FAIL_FAST false
  write_env "${file}" EGON_COLA_COMPONENT_GATEWAY_ENGINE_GATEWAY_GROUP_CODE default
  write_env "${file}" EGON_COLA_COMPONENT_GATEWAY_ENGINE_ENV local
  write_env "${file}" EGON_COLA_COMPONENT_GATEWAY_ENGINE_NAMESPACE default
  write_env "${file}" EGON_COLA_COMPONENT_GATEWAY_ENGINE_NODE_ID gateway-engine-local
  write_env "${file}" EGON_COLA_COMPONENT_GATEWAY_ENGINE_INSTANCE_ID gateway-engine-local-1
  write_env "${file}" EGON_COLA_COMPONENT_GATEWAY_ENGINE_DATA_DIRECTORY "${runtime_dir}/gateway-engine-data"
  write_env "${file}" EGON_COLA_COMPONENT_GATEWAY_ENGINE_HTTP_PUBLIC_PORT 18180
  write_env "${file}" EGON_COLA_COMPONENT_GATEWAY_ENGINE_HTTP_INTERNAL_PORT 18181
  write_env "${file}" EGON_COLA_COMPONENT_GATEWAY_PROVIDER_HTTP_FAIL_FAST false
  write_env "${file}" GATEWAY_ENGINE_DDC_INSTANCE_ID gateway-engine-local-1
  write_env "${file}" GATEWAY_ENGINE_DDC_ADVERTISED_PORT 18180
  write_env "${file}" GATEWAY_MCP_REMOTE_CIRCUIT_OPEN_DURATION PT3S
  write_env "${file}" GATEWAY_MCP_REMOTE_FAILURE_THRESHOLD 2
  write_env "${file}" GATEWAY_MCP_TASK_POLL_INTERVAL PT1S
}

package_applications() {
  if [[ "${UNIFIED_IDENTITY_SKIP_BUILD:-false}" == "true" ]] \
      && [[ -s "${idp_jar}" && -s "${rbac3_jar}" && -s "${gateway_admin_jar}" \
      && -s "${gateway_engine_jar}" && -s "${ddc_jar}" && -s "${mock_jar}" ]]; then
    return
  fi
  "${repo_root}/mvnw" -B -ntp -f "${repo_root}/pom.xml" \
    -pl egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin,egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin,egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-idp-backend \
    -am package -DskipTests
}

write_application_build_ids() {
  write_env "${env_dir}/idp.env" IDP_BUILD_ID "$(local_build_id "${idp_jar}")"
  write_env "${env_dir}/rbac3.env" RBAC3_BUILD_ID "$(local_build_id "${rbac3_jar}")"
  write_env "${env_dir}/gateway-admin.env" GATEWAY_ADMIN_BUILD_ID "$(local_build_id "${gateway_admin_jar}")"
  write_env "${env_dir}/ddc.env" DDC_BUILD_ID "$(local_build_id "${ddc_jar}")"
  write_env "${env_dir}/mock-backend.env" \
    MOCK_BACKEND_BUILD_ID "$(local_build_id "${mock_jar}")"
}

command_prepare() {
  for command in java curl jq openssl psql createdb redis-cli awk; do
    require_command "${command}"
  done
  initialize_directories
  resolve_postgres_password
  psql_command "${postgres_database}" -Atqc 'select 1' >/dev/null
  resolve_redis_password
  for database in "${idp_database}" "${rbac3_database}" \
      "${gateway_database}" "${ddc_database}"; do
    ensure_database "${database}"
  done
  resolve_existing_service_tenant_id
  write_runtime_secrets
  write_service_env_files
  package_applications
  write_application_build_ids
  echo "Host-local unified identity prerequisites are prepared in ${runtime_dir}."
}

process_running() {
  local file="${pid_dir}/$1.pid" pid
  [[ -s "${file}" ]] || return 1
  pid="$(<"${file}")"
  [[ "${pid}" =~ ^[0-9]+$ ]] && kill -0 "${pid}" 2>/dev/null
}

start_process() {
  local name="$1" env_file="$2" jar="$3"
  shift 3
  if process_running "${name}"; then
    return
  fi
  (
    set -a
    # shellcheck disable=SC1090
    source "${env_file}"
    set +a
    exec nohup java -jar "${jar}" "$@"
  ) >"${log_dir}/${name}.log" 2>&1 </dev/null &
  printf '%s' "$!" >"${pid_dir}/${name}.pid"
  chmod 600 "${pid_dir}/${name}.pid"
}

wait_http() {
  local name="$1" url="$2" attempts="${3:-90}" status
  for ((attempt = 1; attempt <= attempts; attempt++)); do
    if ! process_running "${name}"; then
      tail -80 "${log_dir}/${name}.log" >&2 || true
      fail "${name} exited before becoming ready"
    fi
    status="$(curl -s -o /dev/null -w '%{http_code}' "${url}" || true)"
    if [[ "${status}" == "200" ]]; then
      return
    fi
    sleep 1
  done
  tail -80 "${log_dir}/${name}.log" >&2 || true
  fail "${name} did not become ready at ${url}"
}

bootstrap_idp_argument() {
  if database_table_exists "${idp_database}" public.identity_user \
      && database_row_exists "${idp_database}" 'select count(*) from identity_user'; then
    return
  fi
  printf '%s' '--idp-bootstrap-admin=alice'
}

identity_subject() {
  psql_command "${idp_database}" -Atqc \
    "select id from identity_user where username_normalized = 'alice'"
}

rbac3_tenant_id() {
  local tenant_code="$1"
  [[ "${tenant_code}" =~ ^[a-z0-9][a-z0-9-]{0,62}$ ]] \
    || fail "unsafe tenant code: ${tenant_code}"
  local tenant_id
  tenant_id="$(psql_command "${rbac3_database}" -Atqc \
    "select id from rbac3_tenant where lower(code) = '${tenant_code}'")"
  [[ -n "${tenant_id}" ]] || fail "RBAC3 tenant does not exist: ${tenant_code}"
  printf '%s' "${tenant_id}"
}

resolve_existing_service_tenant_id() {
  if [[ "${service_tenant_id}" =~ ^[1-9][0-9]*$ ]]; then
    return
  fi
  if database_table_exists "${rbac3_database}" public.rbac3_tenant; then
    service_tenant_id="$(rbac3_tenant_id "${service_tenant_id}")"
  fi
}

cookie_jar_for_tenant() {
  local tenant="$1"
  [[ "${tenant}" =~ ^[a-z0-9][a-z0-9-]{0,62}$ ]] \
    || fail "unsafe tenant code: ${tenant}"
  printf '%s/browser.%s.cookies' "${runtime_dir}" "${tenant}"
}

access_token_from_cookie() {
  local cookie_jar="$1" token
  [[ -s "${cookie_jar}" ]] || fail "missing USER cookie jar: ${cookie_jar}"
  token="$(awk '$0 !~ /^#/ && ($6 == "__Host-egon_user_at" || $6 == "egon_user_at_local") { value=$7 } END { print value }' "${cookie_jar}")"
  [[ "${token}" =~ ^[^.[:space:]]+\.[^.[:space:]]+\.[^.[:space:]]+$ ]] \
    || fail "USER Access Token cookie is missing from ${cookie_jar}"
  printf '%s' "${token}"
}

idp_bootstrap_login() {
  local tenant="$1" cookie_jar csrf status
  cookie_jar="$(cookie_jar_for_tenant "${tenant}")"
  csrf="$(curl -fsS -c "${cookie_jar}" -b "${cookie_jar}" \
    "${idp_url}/oauth2/login/csrf" | jq -er '.token')"
  status="$(curl -sS -o "${runtime_dir}/login.response" -w '%{http_code}' \
    -c "${cookie_jar}" -b "${cookie_jar}" \
    -H 'Content-Type: application/json' -H "X-IDP-CSRF: ${csrf}" \
    -d "$(jq -cn --arg tenantId "$(rbac3_tenant_id "${tenant}")" \
      --arg password "$(<"${secret_dir}/idp-admin.password")" \
      '{tenantId:$tenantId,username:"alice",password:$password}')" \
    "${idp_url}/oauth2/login")"
  [[ "${status}" == "200" ]] || fail \
    "IdP bootstrap login failed with HTTP ${status}: $(<"${runtime_dir}/login.response")"
}

gateway_login() {
  local tenant="$1" cookie_jar csrf status
  cookie_jar="$(cookie_jar_for_tenant "${tenant}")"
  csrf="$(curl -fsS -c "${cookie_jar}" -b "${cookie_jar}" \
    "${gateway_url}/oauth2/login/csrf" | jq -er '.token')"
  status="$(curl -sS -o "${runtime_dir}/login.response" -w '%{http_code}' \
    -c "${cookie_jar}" -b "${cookie_jar}" \
    -H 'Content-Type: application/json' -H "X-IDP-CSRF: ${csrf}" \
    -d "$(jq -cn --arg tenantId "$(rbac3_tenant_id "${tenant}")" \
      --arg password "$(<"${secret_dir}/idp-admin.password")" \
      '{tenantId:$tenantId,username:"alice",password:$password}')" \
    "${gateway_url}/oauth2/login")"
  [[ "${status}" == "200" ]] || fail \
    "Gateway login failed with HTTP ${status}: $(<"${runtime_dir}/login.response")"
}

platform_user_login() {
  local tenant="${1:-default}"
  if process_running gateway-engine; then
    gateway_login "${tenant}"
  else
    idp_bootstrap_login "${tenant}"
  fi
}

gateway_refresh() {
  local tenant="$1" cookie_jar="${2:-}" status
  [[ -n "${cookie_jar}" ]] || cookie_jar="$(cookie_jar_for_tenant "${tenant}")"
  status="$(curl -sS -o "${runtime_dir}/refresh.response" -w '%{http_code}' \
    -c "${cookie_jar}" -b "${cookie_jar}" -X POST \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode grant_type=refresh_token "${gateway_url}/oauth2/token")"
  [[ "${status}" == "200" ]] || fail \
    "Gateway USER refresh failed with HTTP ${status}: $(<"${runtime_dir}/refresh.response")"
}

gateway_logout() {
  local tenant="$1" cookie_jar="${2:-}" status
  [[ -n "${cookie_jar}" ]] || cookie_jar="$(cookie_jar_for_tenant "${tenant}")"
  status="$(curl -sS -o "${runtime_dir}/logout.response" -w '%{http_code}' \
    -c "${cookie_jar}" -b "${cookie_jar}" -X POST \
    "${gateway_url}/oauth2/logout")"
  [[ "${status}" == "204" ]] || fail \
    "Gateway logout failed with HTTP ${status}: $(<"${runtime_dir}/logout.response")"
}

user_access_token_for_tenant() {
  local tenant="$1" cookie_jar token
  cookie_jar="$(cookie_jar_for_tenant "${tenant}")"
  if ! token="$(access_token_from_cookie "${cookie_jar}" 2>/dev/null)"; then
    platform_user_login "${tenant}"
    token="$(access_token_from_cookie "${cookie_jar}")"
  fi
  printf '%s' "${token}"
}

command_issue_user_token() {
  local tenant="${UNIFIED_IDENTITY_TENANT:-}"
  local output="${UNIFIED_IDENTITY_ACCESS_TOKEN_FILE:-}" token
  process_running idp || fail "idp is not running; run start first"
  process_running rbac3 || fail "rbac3 is not running; run start first"
  [[ -n "${tenant}" ]] || fail "UNIFIED_IDENTITY_TENANT is required"
  [[ -n "${output}" ]] || fail "UNIFIED_IDENTITY_ACCESS_TOKEN_FILE is required"
  token="$(user_access_token_for_tenant "${tenant}")"
  printf '%s' "${token}" >"${output}"
  chmod 600 "${output}"
}

activate_roles() {
  local access_token="$1" include_mock="$2" candidates current role_ids version request status
  [[ -n "${access_token}" ]] || fail "USER Access Token is required for role activation"
  status="$(curl -sS -o "${runtime_dir}/activation-candidates.response" \
    -w '%{http_code}' -H "Authorization: Bearer ${access_token}" \
    "${rbac3_url}/api/rbac3/v1/auth/role-activation-candidates")"
  [[ "${status}" == "200" ]] || fail \
    "RBAC3 activation candidates failed with HTTP ${status}: $(<"${runtime_dir}/activation-candidates.response")"
  candidates="$(<"${runtime_dir}/activation-candidates.response")"
  status="$(curl -sS -o "${runtime_dir}/role-activations.response" \
    -w '%{http_code}' -H "Authorization: Bearer ${access_token}" \
    "${rbac3_url}/api/rbac3/v1/auth/role-activations")"
  [[ "${status}" == "200" ]] || fail \
    "RBAC3 current activation failed with HTTP ${status}: $(<"${runtime_dir}/role-activations.response")"
  current="$(<"${runtime_dir}/role-activations.response")"
  if [[ "${include_mock}" == "true" ]]; then
    role_ids="$(jq -c '[.data.applications[].candidates[].rootRoleId] | unique' <<<"${candidates}")"
  else
    role_ids="$(jq -c '[.data.applications[] as $application
      | $application.candidates[]
      | select($application.applicationCode != "mock-backend"
          or .rootRoleCode == "MOCK_LOCAL_ENTRY")
      | .rootRoleId] | unique' <<<"${candidates}")"
  fi
  [[ "$(jq 'length' <<<"${role_ids}")" -gt 0 ]] || fail "RBAC3 returned no activation candidates"
  version="$(jq -er '.data.authVersion' <<<"${current}")"
  request="$(jq -cn --argjson roles "${role_ids}" --argjson version "${version}" \
    '{roleIds:$roles,expectedAuthVersion:$version}')"
  status="$(curl -sS -o "${runtime_dir}/role-activation-update.response" \
    -w '%{http_code}' -X PUT -H 'Content-Type: application/json' \
    -H "Authorization: Bearer ${access_token}" -d "${request}" \
    "${rbac3_url}/api/rbac3/v1/auth/role-activations")"
  [[ "${status}" == "200" ]] || fail \
    "RBAC3 role activation failed with HTTP ${status}: $(<"${runtime_dir}/role-activation-update.response")"
}

gateway_api() {
  local method="$1" path="$2" body="${3:-}" idempotency_key="${4:-}"
  local response_file status response
  local arguments=(-sS -X "${method}" \
    -H "Authorization: Bearer $(<"${secret_dir}/gateway-admin-control-plane.service.jwt")" \
    -H 'Content-Type: application/json')
  if [[ -n "${idempotency_key}" ]]; then
    arguments+=(-H "Idempotency-Key: ${idempotency_key}")
  fi
  if [[ -n "${body}" ]]; then
    arguments+=(-d "${body}")
  fi
  response_file="$(mktemp "${runtime_dir}/gateway-api.XXXXXX")"
  status="$(curl "${arguments[@]}" -o "${response_file}" -w '%{http_code}' \
    "${gateway_admin_url}${path}")"
  response="$(<"${response_file}")"
  rm -f "${response_file}"
  [[ "${status}" =~ ^2[0-9][0-9]$ ]] || fail \
    "Gateway Admin ${method} ${path} failed with HTTP ${status}: ${response}"
  printf '%s' "${response}"
}

ddc_api() {
  local method="$1" path="$2" body="${3:-}"
  local response_file status response
  [[ -n "${ddc_admin_access_token}" ]] \
    || fail "USER Access Token is required for DDC Admin bootstrap"
  local arguments=(-sS -X "${method}" \
    -H "Authorization: Bearer ${ddc_admin_access_token}" \
    -H 'Content-Type: application/json')
  if [[ -n "${body}" ]]; then
    arguments+=(-d "${body}")
  fi
  response_file="$(mktemp "${runtime_dir}/ddc-api.XXXXXX")"
  status="$(curl "${arguments[@]}" -o "${response_file}" -w '%{http_code}' \
    "${ddc_url}${path}")"
  response="$(<"${response_file}")"
  rm -f "${response_file}"
  [[ "${status}" =~ ^2[0-9][0-9]$ ]] || fail \
    "DDC Admin ${method} ${path} failed with HTTP ${status}: ${response}"
  printf '%s' "${response}"
}

initialize_ddc_topology() {
  local access_token="$1" response biz_code biz_name app_code
  ddc_admin_access_token="${access_token}"
  response="$(ddc_api GET '/api/v1/ddc/envs?keyword=local')"
  if ! jq -e '.data[] | select(.envCode == "local")' \
      <<<"${response}" >/dev/null; then
    ddc_api POST /api/v1/ddc/envs \
      '{"envCode":"local","description":"Host-local development","sortOrder":0,"enabled":true}' \
      >/dev/null
  fi

  while read -r biz_code biz_name; do
    response="$(ddc_api GET "/api/v1/ddc/bizs?keyword=${biz_code}")"
    if ! jq -e --arg biz "${biz_code}" \
        '.data[] | select(.bizCode == $biz)' <<<"${response}" >/dev/null; then
      ddc_api POST /api/v1/ddc/bizs \
        "$(jq -cn --arg biz "${biz_code}" --arg name "${biz_name}" \
          '{bizCode:$biz,bizName:$name,description:"Host-local OAuth2 resource topology",enabled:true}')" \
        >/dev/null
    fi
    response="$(ddc_api GET "/api/v1/ddc/namespaces?bizCode=${biz_code}&keyword=default")"
    if ! jq -e --arg biz "${biz_code}" \
        '.data[] | select(.bizCode == $biz and .namespaceCode == "default")' \
        <<<"${response}" >/dev/null; then
      ddc_api POST /api/v1/ddc/namespaces \
        "$(jq -cn --arg biz "${biz_code}" \
          '{bizCode:$biz,namespaceCode:"default",namespace:"Default",description:"Host-local OAuth2 resource topology",enabled:true}')" \
        >/dev/null
    fi
  done <<'BUSINESSES'
permission Permission
platform Platform
identity Identity
BUSINESSES

  while read -r biz_code app_code; do
    response="$(ddc_api GET "/api/v1/ddc/apps?bizCode=${biz_code}&keyword=${app_code}")"
    if ! jq -e --arg app "${app_code}" \
        --arg biz "${biz_code}" \
        '.data[] | select(.bizCode == $biz and .appCode == $app)' \
        <<<"${response}" >/dev/null; then
      ddc_api POST /api/v1/ddc/apps \
        "$(jq -cn --arg biz "${biz_code}" --arg app "${app_code}" \
          '{bizCode:$biz,appCode:$app,appName:$app,owner:"platform",description:"Host-local OAuth2 resource topology",enabled:true}')" \
        >/dev/null
    fi
    response="$(ddc_api GET "/api/v1/ddc/namespace-env-app-bindings?bizCode=${biz_code}&namespaceCode=default&env=local&appCode=${app_code}")"
    if ! jq -e '.data[] | select(.enabled == true)' \
        <<<"${response}" >/dev/null; then
      ddc_api POST /api/v1/ddc/namespace-env-app-bindings \
        "$(jq -cn --arg biz "${biz_code}" --arg app "${app_code}" \
          '{bizCode:$biz,namespaceCode:"default",env:"local",appCode:$app,enabled:true}')" \
        >/dev/null
    fi
  done <<'APPLICATIONS'
permission idp
permission rbac3
platform ddc
platform gateway-admin
identity mock-backend
identity gateway-engine-default
identity gateway-test-mcp-provider
APPLICATIONS
}

wait_ddc_provider_registration() {
  local biz_code="$1" app_code="$2" service_name="$3" response
  for ((attempt = 1; attempt <= 30; attempt++)); do
    response="$(ddc_api GET \
      "/api/v1/ddc/registry/services?bizCode=${biz_code}&namespaceCode=default&env=local&appCode=${app_code}&serviceKind=HTTP_PROVIDER&protocol=http&serviceName=${service_name}&group=default")"
    if jq -e --arg app "${app_code}" --arg service "${service_name}" '
        .data.services[]
        | select(
            .appCode == $app
            and .serviceKind == "HTTP_PROVIDER"
            and .protocol == "http"
            and .serviceName == $service
            and .group == "default"
          )
      ' <<<"${response}" >/dev/null; then
      return
    fi
    sleep 1
  done
  fail "${biz_code}/${app_code} did not register an online DDC HTTP Provider lease"
}

gateway_application_id_file() {
  case "$1" in
    idp|rbac3|gateway-admin|ddc|mock-backend) ;;
    *) fail "unsupported Gateway reporting application: $1" ;;
  esac
  printf '%s/gateway-application.%s.id' "${runtime_dir}" "$1"
}

gateway_report_access_key_file() {
  printf '%s/gateway-report-%s.access-key' "${secret_dir}" "$1"
}

gateway_report_secret_file() {
  printf '%s/gateway-report-%s.secret' "${secret_dir}" "$1"
}

configure_gateway_reporter() {
  local app_code="$1" access_file secret_file env_file enabled_key
  access_file="$(gateway_report_access_key_file "${app_code}")"
  secret_file="$(gateway_report_secret_file "${app_code}")"
  case "${app_code}" in
    idp) env_file="${env_dir}/idp.env"; enabled_key=IDP_GATEWAY_REPORTING_ENABLED ;;
    rbac3) env_file="${env_dir}/rbac3.env"; enabled_key=RBAC3_GATEWAY_REPORTING_ENABLED ;;
    gateway-admin) env_file="${env_dir}/gateway-admin.env"; enabled_key=GATEWAY_ADMIN_GATEWAY_REPORTING_ENABLED ;;
    ddc) env_file="${env_dir}/ddc.env"; enabled_key=DDC_GATEWAY_REPORTING_ENABLED ;;
    mock-backend) env_file="${env_dir}/mock-backend.env"; enabled_key=MOCK_BACKEND_GATEWAY_REPORTING_ENABLED ;;
    *) fail "unsupported Gateway reporting application: ${app_code}" ;;
  esac
  write_env "${env_file}" GATEWAY_REPORT_ACCESS_KEY "$(<"${access_file}")"
  write_env "${env_file}" GATEWAY_REPORT_SECRET_KEY "$(<"${secret_file}")"
  write_env "${env_file}" "${enabled_key}" true
}

ensure_gateway_reporting_application() {
  local biz_code="$1" app_code="$2" display_name="$3"
  local applications application app_id credential access_file secret_file
  applications="$(gateway_api GET "/api/v1/gateway/admin/applications?bizCode=${biz_code}&namespace=default&env=local&appCode=${app_code}")"
  app_id="$(jq -r --arg app "${app_code}" \
    '.[] | select(.applicationCode == $app) | .id' <<<"${applications}" | head -1)"
  if [[ -z "${app_id}" ]]; then
    application="$(gateway_api POST /api/v1/gateway/admin/applications \
      "$(jq -cn --arg biz "${biz_code}" --arg app "${app_code}" \
        --arg display "${display_name}" \
        '{bizCode:$biz,applicationCode:$app,displayName:$display,env:"local",namespace:"default",description:"Host-local unified identity Gateway catalog provider"}')")"
    app_id="$(jq -er '.id' <<<"${application}")"
  fi
  access_file="$(gateway_report_access_key_file "${app_code}")"
  secret_file="$(gateway_report_secret_file "${app_code}")"
  if [[ ! -s "${access_file}" || ! -s "${secret_file}" ]]; then
    credential="$(gateway_api POST "/api/v1/gateway/admin/applications/${app_id}/credentials" '{}')"
    jq -er '.accessKey' <<<"${credential}" >"${access_file}"
    jq -er '.secret' <<<"${credential}" >"${secret_file}"
    chmod 600 "${access_file}" "${secret_file}"
  fi
  printf '%s' "${app_id}" >"$(gateway_application_id_file "${app_code}")"
  chmod 600 "$(gateway_application_id_file "${app_code}")"
  configure_gateway_reporter "${app_code}"
}

initialize_gateway_control_plane() {
  local groups group group_id
  ensure_gateway_reporting_application permission idp "IdP Identity Admin"
  ensure_gateway_reporting_application permission rbac3 "RBAC3 Permission Admin"
  ensure_gateway_reporting_application platform gateway-admin "Gateway Admin"
  ensure_gateway_reporting_application platform ddc "Dynamic Config Center Admin"
  ensure_gateway_reporting_application identity mock-backend "Unified Identity Mock Backend"

  groups="$(gateway_api GET '/api/v1/gateway/admin/gateway-groups?env=local&namespace=default')"
  group_id="$(jq -r '.[] | select(.gatewayGroupCode == "default") | .id' <<<"${groups}" | head -1)"
  if [[ -z "${group_id}" ]]; then
    group="$(gateway_api POST /api/v1/gateway/admin/gateway-groups \
      '{"gatewayGroupCode":"default","displayName":"Unified Identity Local Gateway","env":"local","namespace":"default","description":"Host-local unified identity Gateway route group"}')"
    group_id="$(jq -er '.id' <<<"${group}")"
  fi
  printf '%s' "${group_id}" >"${runtime_dir}/gateway-group.id"
  printf '%s' "$(<"$(gateway_application_id_file mock-backend)")" \
    >"${runtime_dir}/gateway-application.id"
  chmod 600 "${runtime_dir}/gateway-group.id" "${runtime_dir}/gateway-application.id"
}

wait_gateway_catalog_for_app() {
  local app_code="$1" app_id response
  app_id="$(<"$(gateway_application_id_file "${app_code}")")"
  for ((attempt = 1; attempt <= 60; attempt++)); do
    response="$(gateway_api GET \
      "/api/v1/gateway/admin/applications/${app_id}/catalog" || true)"
    if jq -e '.. | objects | select(.protocol? == "HTTP" and .lifecycleStatus? == "ACTIVE")' \
        <<<"${response}" >/dev/null 2>&1; then
      return
    fi
    sleep 1
  done
  fail "${app_code} Gateway catalog did not become active"
}

wait_gateway_catalog() {
  local app_code
  for app_code in idp rbac3 gateway-admin ddc mock-backend; do
    wait_gateway_catalog_for_app "${app_code}"
  done
}

gateway_catalog_operations() {
  local app_code app_id catalog part_file combined_file
  combined_file="${runtime_dir}/gateway-catalog-operations.jsonl"
  : >"${combined_file}"
  for app_code in idp rbac3 gateway-admin ddc mock-backend; do
    app_id="$(<"$(gateway_application_id_file "${app_code}")")"
    catalog="$(gateway_api GET \
      "/api/v1/gateway/admin/applications/${app_id}/catalog")"
    part_file="$(mktemp "${runtime_dir}/gateway-catalog.XXXXXX")"
    jq -c --arg app "${app_code}" \
      '[.. | objects | select(.id? and .methodIdentity? and .protocol?)
       | . + {reportedApplication:$app}]' <<<"${catalog}" >"${part_file}"
    jq -c '.' "${part_file}" >>"${combined_file}"
    rm -f "${part_file}"
  done
  jq -s 'map(.[]) | unique_by(.id)' "${combined_file}"
}

route_id_for_operation() {
  printf '%s' "$1" | openssl dgst -sha256 -r \
    | awk '{print "unified-" substr($1, 1, 32)}'
}

publish_gateway_routes() {
  local defer_release="${1:-false}"
  local group_id operations draft revision response validation release
  local security ids policy_id route_type auth_mode forward recovery
  local extractors auth_providers authz_providers operation_id method_identity app_code
  local method path route_id route_content desired_policy
  group_id="$(<"${runtime_dir}/gateway-group.id")"
  operations="$(gateway_catalog_operations | jq '
    map(select(.protocol == "HTTP" and .externalAccessible == true
      and .lifecycleStatus == "ACTIVE"))
    | map(. + {securityType:
      (if .reportedApplication == "idp" and (
        .methodIdentity == "GET /oauth2/login/csrf"
        or .methodIdentity == "POST /oauth2/login"
        or .methodIdentity == "POST /oauth2/token"
        or .methodIdentity == "POST /oauth2/revoke"
        or .methodIdentity == "POST /oauth2/logout"
        or .methodIdentity == "GET /.well-known/oauth-authorization-server"
        or .methodIdentity == "GET /oauth2/jwks")
       then "PUBLIC_PROTOCOL"
       elif .reportedApplication == "idp" and (
        .methodIdentity == "GET /oauth2/userinfo"
        or .methodIdentity == "POST /oauth2/step-up")
       then "IDENTITY_PROTECTED"
       else "BUSINESS_PROTECTED" end)})')"
  printf '%s' "${operations}" >"${runtime_dir}/gateway-operations.json"
  draft="$(gateway_api GET "/api/v1/gateway/admin/gateway-groups/${group_id}/draft")"
  revision="$(jq -er '.revision' <<<"${draft}")"

  if jq -e '.policies[]? | select(.policyId == "identity-basic" and .policyScope == "GLOBAL")' \
      <<<"${draft}" >/dev/null; then
    response="$(gateway_api DELETE \
      "/api/v1/gateway/admin/gateway-groups/${group_id}/draft/policies/identity-basic" \
      "$(jq -cn --argjson revision "${revision}" \
        '{expectedRevision:$revision,idempotencyKey:("unified-remove-identity-basic-" + ($revision | tostring)),changeReason:"Replace legacy global policy with operation-scoped stateless policies"}')")"
    revision="$(jq -er '.revision' <<<"${response}")"
    draft="$(gateway_api GET "/api/v1/gateway/admin/gateway-groups/${group_id}/draft")"
  fi

  for security in PUBLIC_PROTOCOL IDENTITY_PROTECTED BUSINESS_PROTECTED; do
    ids="$(jq -c --arg security "${security}" \
      '[.[] | select(.securityType == $security) | .id] | sort' \
      <<<"${operations}")"
    [[ "$(jq 'length' <<<"${ids}")" -gt 0 ]] || continue
    case "${security}" in
      PUBLIC_PROTOCOL)
        policy_id=unified-public-protocol
        route_type=PUBLIC_PROTOCOL; auth_mode=NONE; forward=NONE; recovery=null
        extractors='[]'; auth_providers='[]'; authz_providers='[]' ;;
      IDENTITY_PROTECTED)
        policy_id=unified-identity-protected
        route_type=IDENTITY_PROTECTED; auth_mode=REQUIRED; forward=ORIGINAL_BEARER; recovery='"idp-user-refresh"'
        extractors='["idp-user-cookie"]'; auth_providers='["idp-jwt"]'; authz_providers='[]' ;;
      BUSINESS_PROTECTED)
        policy_id=unified-business-protected
        route_type=BUSINESS_PROTECTED; auth_mode=REQUIRED; forward=ORIGINAL_BEARER; recovery='"idp-user-refresh"'
        extractors='["idp-user-cookie"]'; auth_providers='["idp-jwt"]'; authz_providers='["rbac3-permission"]' ;;
    esac
    if ! jq -e --arg policy "${policy_id}" --arg routeType "${route_type}" \
        --arg authMode "${auth_mode}" --arg forward "${forward}" \
        --argjson ids "${ids}" --argjson extractors "${extractors}" \
        --argjson authProviders "${auth_providers}" \
        --argjson authzProviders "${authz_providers}" \
        --argjson recovery "${recovery}" '
        .policies[]? | select(
          .policyId == $policy and .policyScope == "OPERATION" and .enabled == true
          and .content.routeSecurityType == $routeType
          and .content.authenticationMode == $authMode
          and .content.credentialExtractorIds == $extractors
          and .content.authenticationProviderIds == $authProviders
          and .content.authorizationProviderIds == $authzProviders
          and .content.operationIds == $ids
          and .content.credentialRecoveryProviderId == $recovery
          and .content.decisionMode == "ALL_ALLOW"
          and .content.providerTimeoutMs == 1000
          and .content.failureMode == "FAIL_CLOSED"
          and .content.credentialForwardingMode == $forward
        )' <<<"${draft}" >/dev/null; then
      desired_policy="$(jq -cn --argjson ids "${ids}" \
        --arg routeType "${route_type}" --arg authMode "${auth_mode}" \
        --arg forward "${forward}" --argjson extractors "${extractors}" \
        --argjson authProviders "${auth_providers}" \
        --argjson authzProviders "${authz_providers}" \
        --argjson recovery "${recovery}" --argjson revision "${revision}" \
        --arg policy "${policy_id}" \
        '{policyType:"SECURITY",policyScope:"OPERATION",content:{operationIds:$ids,routeSecurityType:$routeType,authenticationMode:$authMode,credentialExtractorIds:$extractors,authenticationProviderIds:$authProviders,authorizationProviderIds:$authzProviders,credentialRecoveryProviderId:$recovery,decisionMode:"ALL_ALLOW",providerTimeoutMs:1000,failureMode:"FAIL_CLOSED",credentialForwardingMode:$forward},enabled:true,expectedRevision:$revision,idempotencyKey:("unified-policy-" + $policy + "-" + ($revision | tostring)),changeReason:"Publish operation-scoped stateless identity and RBAC3 Gateway policies"}')"
      response="$(gateway_api PUT \
        "/api/v1/gateway/admin/gateway-groups/${group_id}/draft/policies/${policy_id}" \
        "${desired_policy}")"
      revision="$(jq -er '.revision' <<<"${response}")"
      draft="$(gateway_api GET "/api/v1/gateway/admin/gateway-groups/${group_id}/draft")"
    fi
  done

  while IFS=$'\t' read -r operation_id method_identity app_code; do
    [[ -n "${operation_id}" && -n "${method_identity}" ]] || continue
    method="${method_identity%% *}"
    path="${method_identity#* }"
    route_id="$(route_id_for_operation "${operation_id}")"
    if jq -e --arg route "${route_id}" --arg operation "${operation_id}" \
        --arg method "${method}" --arg path "${path}" '
        .routes[]? | select(.routeId == $route and .operationId == $operation
          and .enabled == true and .content.host == "*"
          and .content.httpMethod == $method
          and .content.pathPattern == $path
          and .content.accessZones == ["PUBLIC"])' <<<"${draft}" >/dev/null; then
      continue
    fi
    route_content="$(jq -cn --arg operation "${operation_id}" \
      --arg method "${method}" --arg path "${path}" \
      '{operationId:$operation,content:{host:"*",httpMethod:$method,pathPattern:$path,accessZones:["PUBLIC"],priority:100},enabled:true}')"
    response="$(gateway_api PUT \
      "/api/v1/gateway/admin/gateway-groups/${group_id}/draft/routes/${route_id}" \
      "$(jq -cn --argjson route "${route_content}" --argjson revision "${revision}" \
        --arg operation "${operation_id}" \
        '$route + {expectedRevision:$revision,idempotencyKey:("unified-route-" + $operation + "-" + ($revision | tostring)),changeReason:"Publish reported HTTP operation from the real provider catalog"}')")"
    revision="$(jq -er '.revision' <<<"${response}")"
    draft="$(gateway_api GET "/api/v1/gateway/admin/gateway-groups/${group_id}/draft")"
  done < <(jq -r '.[] | [.id,.methodIdentity,.reportedApplication] | @tsv' \
    <<<"${operations}")

  if [[ "${defer_release}" == "true" ]]; then
    return
  fi
  validation="$(gateway_api POST "/api/v1/gateway/admin/gateway-groups/${group_id}/draft/validate" '{}')"
  jq -e '.valid == true' <<<"${validation}" >/dev/null \
    || fail "Gateway draft validation failed: ${validation}"
  release="$(gateway_api POST "/api/v1/gateway/admin/gateway-groups/${group_id}/releases" \
    "$(jq -cn --argjson revision "${revision}" \
      '{expectedDraftRevision:$revision,changeReason:"Unified identity real catalog route release"}')")"
  jq -e '.status == "SUCCESS"' <<<"${release}" >/dev/null \
    || fail "Gateway release did not succeed: ${release}"
  jq -er '.releaseId' <<<"${release}" >"${runtime_dir}/gateway-release.id"
  chmod 600 "${runtime_dir}/gateway-release.id"
}

wait_gateway_route() {
  local status
  for ((attempt = 1; attempt <= 30; attempt++)); do
    status="$(curl -sS -o /dev/null -w '%{http_code}' \
      "${gateway_url}/api/mock/read")"
    if [[ "${status}" == "401" ]]; then
      return
    fi
    sleep 1
  done
  fail "Gateway Engine did not load the unified identity routes"
}

command_start() {
  command_prepare
  local idp_argument subject tenant_b_id rbac3_access_token ddc_access_token
  stage "starting DDC"
  start_process ddc "${env_dir}/ddc.env" "${ddc_jar}"
  wait_http ddc "${ddc_url}/actuator/health/readiness"

  stage "starting IdP bootstrap phase without DDC publication"
  idp_argument="$(bootstrap_idp_argument)"
  if [[ -n "${idp_argument}" ]]; then
    start_process idp "${env_dir}/idp.env" "${idp_jar}" \
      --egon.cola.component.ddc.enabled=false \
      --egon.cola.component.ddc.registry.enabled=false \
      --egon.cola.component.ddc.registry.http.enabled=false \
      "${idp_argument}"
  else
    start_process idp "${env_dir}/idp.env" "${idp_jar}" \
      --egon.cola.component.ddc.enabled=false \
      --egon.cola.component.ddc.registry.enabled=false \
      --egon.cola.component.ddc.registry.http.enabled=false
  fi
  wait_http idp "${idp_url}/actuator/health/readiness"
  stage "issuing IdP-owned service credentials"
  refresh_service_tokens
  subject="$(identity_subject)"
  [[ -n "${subject}" ]] || fail "IdP bootstrap subject is missing"

  stage "starting RBAC3 bootstrap phase without DDC publication"
  write_env "${env_dir}/rbac3.env" RBAC3_DEVELOPMENT_IDENTITY_SUB "${subject}"
  start_process rbac3 "${env_dir}/rbac3.env" "${rbac3_jar}" \
    --egon.cola.component.ddc.enabled=false \
    --egon.cola.component.ddc.registry.enabled=false \
    --egon.cola.component.ddc.registry.http.enabled=false
  wait_http rbac3 "${rbac3_url}/actuator/health/readiness"

  service_tenant_id="$(rbac3_tenant_id default)"
  tenant_b_id="$(rbac3_tenant_id tenant-b)"
  write_env "${env_dir}/idp.env" IDP_RBAC3_SERVICE_TENANT_ID \
    "${service_tenant_id}"
  write_env "${env_dir}/idp.env" IDP_DEVELOPMENT_RBAC3_SERVICE_TENANT_ID \
    "${service_tenant_id}"
  write_env "${env_dir}/idp.env" IDP_DEVELOPMENT_RBAC3_SERVICE_TENANT_IDS \
    "${service_tenant_id},${tenant_b_id}"
  stage "binding IdP service credentials to the RBAC3 tenant"
  stop_process idp
  start_process idp "${env_dir}/idp.env" "${idp_jar}" \
    --egon.cola.component.ddc.enabled=false \
    --egon.cola.component.ddc.registry.enabled=false \
    --egon.cola.component.ddc.registry.http.enabled=false
  wait_http idp "${idp_url}/actuator/health/readiness"
  refresh_service_tokens

  stage "establishing the default-tenant USER cookie"
  platform_user_login
  stage "loading the default-tenant USER Access Token from its Gateway cookie"
  rbac3_access_token="$(user_access_token_for_tenant default)"
  stage "activating non-mock roles"
  activate_roles "${rbac3_access_token}" false
  stage "initializing DDC unified identity topology"
  ddc_access_token="$(user_access_token_for_tenant default)"
  initialize_ddc_topology "${ddc_access_token}"

  stage "restarting IdP and RBAC3 with admitted DDC publication"
  stop_process rbac3
  stop_process idp
  start_process idp "${env_dir}/idp.env" "${idp_jar}"
  wait_http idp "${idp_url}/actuator/health/readiness"
  refresh_service_tokens
  start_process rbac3 "${env_dir}/rbac3.env" "${rbac3_jar}"
  wait_http rbac3 "${rbac3_url}/actuator/health/readiness"

  stage "restoring the USER cookie and RBAC3 activation context"
  platform_user_login
  rbac3_access_token="$(user_access_token_for_tenant default)"
  activate_roles "${rbac3_access_token}" false
  wait_ddc_provider_registration permission idp idp-admin
  wait_ddc_provider_registration permission rbac3 rbac3-admin
  stage "starting Gateway Engine DDC client"
  start_process gateway-engine "${env_dir}/gateway-engine.env" "${gateway_engine_jar}"
  wait_http gateway-engine http://127.0.0.1:18182/actuator/health/readiness

  stage "starting Gateway Admin"
  start_process gateway-admin "${env_dir}/gateway-admin.env" "${gateway_admin_jar}"
  wait_http gateway-admin "${gateway_admin_url}/actuator/health/readiness"
  initialize_gateway_control_plane

  stage "restarting providers with real Gateway catalog reporting"
  # Gateway Admin is the reporting control plane, so it first remains available with its own
  # reporting disabled while DDC, IdP, and RBAC3 publish their real HTTP catalogs.
  write_env "${env_dir}/gateway-admin.env" \
    GATEWAY_ADMIN_GATEWAY_REPORTING_ENABLED false
  stop_process gateway-admin
  stop_process rbac3
  stop_process idp
  stop_process ddc
  start_process gateway-admin "${env_dir}/gateway-admin.env" "${gateway_admin_jar}"
  wait_http gateway-admin "${gateway_admin_url}/actuator/health/readiness"
  start_process ddc "${env_dir}/ddc.env" "${ddc_jar}"
  wait_http ddc "${ddc_url}/actuator/health/readiness"
  start_process idp "${env_dir}/idp.env" "${idp_jar}"
  wait_http idp "${idp_url}/actuator/health/readiness"
  refresh_service_tokens
  start_process rbac3 "${env_dir}/rbac3.env" "${rbac3_jar}"
  wait_http rbac3 "${rbac3_url}/actuator/health/readiness"
  write_env "${env_dir}/gateway-admin.env" \
    GATEWAY_ADMIN_GATEWAY_REPORTING_ENABLED true
  stop_process gateway-admin
  start_process gateway-admin "${env_dir}/gateway-admin.env" "${gateway_admin_jar}"
  wait_http gateway-admin "${gateway_admin_url}/actuator/health/readiness"

  stage "starting mock backend"
  start_process mock-backend "${env_dir}/mock-backend.env" "${mock_jar}"
  wait_http mock-backend "${mock_url}/actuator/health/readiness"
  wait_gateway_catalog
  if [[ "${UNIFIED_IDENTITY_DEFER_GATEWAY_RELEASE:-false}" == "true" ]]; then
    stage "preparing Gateway routes for the unified platform publisher"
    publish_gateway_routes true
  else
    publish_gateway_routes
    wait_gateway_route
  fi
  echo "Unified identity backends are running. Run verify for live acceptance checks."
}

command_refresh_tokens() {
  local default_access_token tenant_b_access_token
  process_running idp || fail "idp is not running; run start first"
  process_running rbac3 || fail "rbac3 is not running; run start first"
  service_tenant_id="$(rbac3_tenant_id default)"

  stage "refreshing IdP-owned service credentials"
  refresh_service_tokens
  stage "refreshing local USER cookies and authorization snapshots"
  platform_user_login
  default_access_token="$(user_access_token_for_tenant default)"
  activate_roles "${default_access_token}" true

  platform_user_login tenant-b
  tenant_b_access_token="$(user_access_token_for_tenant tenant-b)"
  activate_roles "${tenant_b_access_token}" true
  stage "local USER Access Token cookies and authorization snapshots are current"
}

http_status() {
  local url="$1" access_token="$2"
  curl -sS -o /dev/null -w '%{http_code}' \
    -H "Authorization: Bearer ${access_token}" "${url}"
}

stable_refresh_check() {
  local tenant=default cookie_jar old_cookie before after revoked_status
  cookie_jar="$(cookie_jar_for_tenant "${tenant}")"
  old_cookie="${runtime_dir}/refresh-stable-before.cookies"
  before="$(access_token_from_cookie "${cookie_jar}")"
  cp "${cookie_jar}" "${old_cookie}"
  gateway_refresh "${tenant}" "${cookie_jar}"
  after="$(access_token_from_cookie "${cookie_jar}")"
  [[ "${before}" != "${after}" ]] \
    || fail "USER refresh did not issue a new five-minute Access Token"

  gateway_refresh "${tenant}" "${old_cookie}"
  [[ "$(access_token_from_cookie "${old_cookie}")" != "${before}" ]] \
    || fail "stable Refresh Token did not remain usable for a second refresh"
  pre_logout_access_token="${after}"
  gateway_logout "${tenant}" "${cookie_jar}"
  revoked_status="$(curl -sS -o "${runtime_dir}/refresh-after-logout.response" \
    -w '%{http_code}' -c "${old_cookie}" -b "${old_cookie}" -X POST \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode grant_type=refresh_token "${gateway_url}/oauth2/token")"
  [[ "${revoked_status}" != "200" ]] \
    || fail "Refresh Token remained usable after Gateway logout"
}

command_verify() {
  local status subject_before subject_tenant_b token_claims
  local rbac3_access_token default_access_token tenant_b_access_token
  local verify_token_dir mvn_status
  for name in ddc idp rbac3 gateway-admin mock-backend gateway-engine; do
    process_running "${name}" || fail "${name} is not running; run start first"
  done
  platform_user_login
  rbac3_access_token="$(user_access_token_for_tenant default)"
  activate_roles "${rbac3_access_token}" false
  default_access_token="$(user_access_token_for_tenant default)"
  status="$(http_status "${gateway_url}/api/mock/admin" \
    "${default_access_token}")"
  [[ "${status}" == "403" ]] || fail \
    "downstream permission denial must be 403 before mock role activation; got ${status}"
  activate_roles "${rbac3_access_token}" true
  for ((attempt = 1; attempt <= 20; attempt++)); do
    if [[ "$(http_status "${gateway_url}/api/mock/admin" "${default_access_token}")" == "200" ]]; then
      break
    fi
    sleep 1
  done
  [[ "$(http_status "${gateway_url}/api/mock/admin" "${default_access_token}")" == "200" ]] \
    || fail "role activation did not authorize the unchanged access token"

  platform_user_login tenant-b
  tenant_b_access_token="$(user_access_token_for_tenant tenant-b)"
  activate_roles "${tenant_b_access_token}" true
  [[ "$(http_status "${gateway_url}/api/mock/read" "${tenant_b_access_token}")" == "200" ]] \
    || fail "tenant-b token did not reach the backend"
  subject_before="$(jq -Rer 'split(".")[1] | @base64d | fromjson | .sub' \
    <<<"${default_access_token}")"
  subject_tenant_b="$(jq -Rer 'split(".")[1] | @base64d | fromjson | .sub' \
    <<<"${tenant_b_access_token}")"
  [[ "${subject_before}" == "${subject_tenant_b}" ]] \
    || fail "tenant switch changed the USER identity subject"
  for token_claims in "${default_access_token}" "${tenant_b_access_token}"; do
    jq -Rre 'split(".")[1] | @base64d | fromjson
      | ((.principal_type // "USER") == "USER"
        and (has("sid") | not)
        and (has("client_id") | not)
        and (has("token_version") | not))' <<<"${token_claims}" >/dev/null 2>&1 \
      || fail "USER Access Token contains a session or client lifecycle claim"
  done

  platform_user_login default
  default_access_token="$(user_access_token_for_tenant default)"
  stable_refresh_check
  platform_user_login default
  default_access_token="$(user_access_token_for_tenant default)"
  platform_user_login tenant-b
  tenant_b_access_token="$(user_access_token_for_tenant tenant-b)"
  [[ "$(http_status "${gateway_url}/api/mock/read" "${default_access_token}")" == "200" ]] \
    || fail "fresh access token failed after stable refresh and logout"
  [[ "$(http_status "${gateway_url}/api/mock/read" "${tenant_b_access_token}")" == "200" ]] \
    || fail "fresh tenant-b token failed after stable refresh and logout"

  verify_token_dir="$(mktemp -d "${runtime_dir}/verify-tokens.XXXXXX")"
  chmod 700 "${verify_token_dir}"
  printf '%s' "${default_access_token}" >"${verify_token_dir}/default.at"
  printf '%s' "${tenant_b_access_token}" >"${verify_token_dir}/tenant-b.at"
  printf '%s' "${pre_logout_access_token}" >"${verify_token_dir}/pre-logout.at"
  chmod 600 "${verify_token_dir}"/*.at
  UNIFIED_IDENTITY_LIVE=true \
  UNIFIED_IDENTITY_GATEWAY_URL="${gateway_url}" \
  UNIFIED_IDENTITY_MOCK_URL="${mock_url}" \
  UNIFIED_IDENTITY_DEFAULT_TOKEN_FILE="${verify_token_dir}/default.at" \
  UNIFIED_IDENTITY_TENANT_B_TOKEN_FILE="${verify_token_dir}/tenant-b.at" \
  UNIFIED_IDENTITY_PRE_LOGOUT_TOKEN_FILE="${verify_token_dir}/pre-logout.at" \
    "${repo_root}/mvnw" -B -ntp -f "${repo_root}/pom.xml" \
      -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite \
      -am -Dtest=UnifiedIdentityTopologyIT,UnifiedIdentityRevocationIT,UnifiedIdentityTenantSwitchIT \
      -Dsurefire.failIfNoSpecifiedTests=false test || mvn_status=$?
  if [[ -n "${mvn_status:-}" ]]; then
    rm -rf "${verify_token_dir}"
    return "${mvn_status}"
  fi
  rm -rf "${verify_token_dir}"
  echo "Unified identity host-local verification passed."
}

command_status() {
  local name pid state url status
  for name in ddc idp rbac3 gateway-admin mock-backend gateway-engine; do
    if process_running "${name}"; then
      pid="$(<"${pid_dir}/${name}.pid")"
      state=running
    else
      pid=-
      state=stopped
    fi
    case "${name}" in
      ddc) url="${ddc_url}/actuator/health/readiness" ;;
      idp) url="${idp_url}/actuator/health/readiness" ;;
      rbac3) url="${rbac3_url}/actuator/health/readiness" ;;
      gateway-admin) url="${gateway_admin_url}/actuator/health/readiness" ;;
      mock-backend) url="${mock_url}/actuator/health/readiness" ;;
      gateway-engine) url=http://127.0.0.1:18182/actuator/health/readiness ;;
    esac
    status="$(curl -sS -o /dev/null -w '%{http_code}' "${url}" 2>/dev/null || true)"
    printf '%-16s pid=%-8s process=%-7s health=%s\n' \
      "${name}" "${pid}" "${state}" "${status:-unreachable}"
  done
}

stop_process() {
  local name="$1" file="${pid_dir}/$1.pid" pid
  [[ -s "${file}" ]] || return 0
  pid="$(<"${file}")"
  if [[ "${pid}" =~ ^[0-9]+$ ]] && kill -0 "${pid}" 2>/dev/null; then
    kill "${pid}"
    for ((attempt = 1; attempt <= 30; attempt++)); do
      kill -0 "${pid}" 2>/dev/null || break
      sleep 1
    done
    if kill -0 "${pid}" 2>/dev/null; then
      kill -KILL "${pid}"
    fi
  fi
  rm -f "${file}"
}

command_stop() {
  for name in gateway-engine mock-backend gateway-admin rbac3 idp ddc; do
    stop_process "${name}"
  done
  echo "Unified identity managed processes stopped; databases and secrets were preserved."
}

case "${1:---help}" in
  --help|-h|help) usage ;;
  prepare) command_prepare ;;
  start) command_start ;;
  sync-local-credentials) command_refresh_tokens ;;
  issue-user-token) command_issue_user_token ;;
  verify) command_verify ;;
  status) command_status ;;
  stop) command_stop ;;
  *) usage >&2; exit 2 ;;
esac
