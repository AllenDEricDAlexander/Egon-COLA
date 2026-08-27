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
advertised_host="${UNIFIED_IDENTITY_ADVERTISED_HOST:-127.0.0.1}"
declared_hosts="127.0.0.1"
startup_mode="${UNIFIED_IDENTITY_START_MODE:-platforms}"

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
tenant_authority_artifact="${UNIFIED_IDENTITY_TENANT_AUTHORITY_ARTIFACT:-}"
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
  publish-gateway-routes  Publish the prepared local Gateway routes after Engine startup
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
  local client_id="$1" output="$2"
  local resource="${3:-https://api.egon.internal/local/permission/rbac3}"
  local scopes="${4:-service:authorization:decide service:authorization:snapshot service:identity:resolve}"
  local response_file status token_endpoint secret_file
  token_endpoint="${idp_url}/oauth2/token"
  secret_file="${secret_dir}/${client_id}.secret"
  [[ -s "${secret_file}" ]] \
    || fail "IdP Client Secret is unavailable for ${client_id}"
  response_file="$(mktemp "${runtime_dir}/service-token.XXXXXX")"
  status="$(curl -sS -o "${response_file}" -w '%{http_code}' -X POST \
    --user "${client_id}:$(<"${secret_file}")" \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode grant_type=client_credentials \
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
  oauth_service_token idp-service \
    "${secret_dir}/idp-admin.service.jwt"
  oauth_service_token rbac3-service \
    "${secret_dir}/rbac3-admin.service.jwt"
  oauth_service_token gateway-admin-service \
    "${secret_dir}/gateway-admin.service.jwt"
  oauth_service_token gateway-admin-service \
    "${secret_dir}/gateway-admin-control-plane.service.jwt" \
    https://api.egon.internal/local/platform/gateway-admin \
    'gateway:read gateway:applications:write gateway:catalog:write gateway:credentials:write gateway:drafts:write gateway:groups:write gateway:mcp:approve gateway:mcp:read gateway:mcp:runtime:read gateway:mcp:test gateway:mcp:write gateway:releases:write'
  oauth_service_token gateway-engine-service \
    "${secret_dir}/gateway-engine.service.jwt"
  oauth_service_token ddc-service \
    "${secret_dir}/ddc-admin.service.jwt"
  oauth_service_token mock-backend-service \
    "${secret_dir}/mock-backend.service.jwt"
  oauth_service_token mcp-provider-service \
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
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_EGON_IDP_CLIENT_ID)
      printf 'spring.security.oauth2.client.registration.egon-idp.client-id'
      ;;
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_EGON_IDP_CLIENT_SECRET)
      printf 'spring.security.oauth2.client.registration.egon-idp.client-secret'
      ;;
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_EGON_IDP_AUTHORIZATION_GRANT_TYPE)
      printf 'spring.security.oauth2.client.registration.egon-idp.authorization-grant-type'
      ;;
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_EGON_IDP_CLIENT_AUTHENTICATION_METHOD)
      printf 'spring.security.oauth2.client.registration.egon-idp.client-authentication-method'
      ;;
    SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_EGON_IDP_TOKEN_URI)
      printf 'spring.security.oauth2.client.provider.egon-idp.token-uri'
      ;;
    EGON_COLA_PLATFORM_IDP_SERVICE_CLIENT_APP_ID)
      printf 'egon.cola.platform.idp.service-client.app-id'
      ;;
    EGON_COLA_PLATFORM_IDP_SERVICE_CLIENT_REGISTRATION_ID)
      printf 'egon.cola.platform.idp.service-client.registration-id'
      ;;
    EGON_COLA_COMPONENT_ID_MACHINE_ID)
      printf 'egon.cola.component.id.machine-id'
      ;;
    EGON_COLA_COMPONENT_DDC_REGISTRATION_RESOURCE_URI)
      printf 'egon.cola.component.ddc.registration-resource-uri'
      ;;
    EGON_COLA_COMPONENT_DDC_RPC_MAX_INBOUND_MESSAGE_SIZE)
      printf 'egon.cola.component.ddc.rpc.max-inbound-message-size'
      ;;
    EGON_COLA_COMPONENT_RPC_PROVIDER_MAX_INBOUND_MESSAGE_SIZE)
      printf 'egon.cola.component.rpc.provider.max-inbound-message-size'
      ;;
    IDP_GATEWAY_REPORTING_ENABLED|\
    RBAC3_GATEWAY_REPORTING_ENABLED|\
    GATEWAY_ADMIN_GATEWAY_REPORTING_ENABLED|\
    DDC_GATEWAY_REPORTING_ENABLED|\
    MOCK_BACKEND_GATEWAY_REPORTING_ENABLED)
      printf 'egon.cola.component.gateway.reporting.enabled'
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
    GATEWAY_ADMIN_RELEASE_RECONCILE_ENABLED)
      printf 'gateway.admin.release-reconcile-enabled'
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
  write_env "${file}" UNIFIED_PLATFORM_RUNTIME_DIR "${runtime_dir}"
  write_env "${file}" UNIFIED_IDENTITY_ENABLED true
  write_env "${file}" IDP_ADMISSION_RPC_DEVELOPMENT_PLAINTEXT true
  write_env "${file}" IDP_OAUTH_ISSUER "${idp_url}"
  write_env "${file}" IDP_JWK_SET_URI "${idp_url}/oauth2/jwks"
  write_env "${file}" RBAC3_AUTHORIZATION_ENDPOINT "${rbac3_url}"
  write_env "${file}" EGON_COLA_COMPONENT_DDC_REGISTRATION_RESOURCE_URI \
    https://api.egon.internal/local/platform/ddc
  write_env "${file}" EGON_COLA_COMPONENT_DDC_RPC_MAX_INBOUND_MESSAGE_SIZE \
    67108864
  write_env "${file}" EGON_COLA_COMPONENT_RPC_PROVIDER_MAX_INBOUND_MESSAGE_SIZE \
    67108864
  write_env "${file}" EGON_COLA_PLATFORM_RBAC3_RUNTIME_PASSWORD_FILE \
    "${secret_dir}/redis.password"
  write_env "${file}" EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_CACHE_TTL 1s
  write_env "${file}" EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_MAXIMUM_JITTER 0s
  write_env "${file}" EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_NEAR_CACHE_TTL 0s
}

write_idp_service_client_env() {
  local file="$1" client_id="$2" secret_file secret_value
  secret_file="${secret_dir}/${client_id}.secret"
  if [[ -s "${secret_file}" ]]; then
    secret_value="$(<"${secret_file}")"
  else
    secret_value=local-client-secret-pending
  fi
  write_env "${file}" \
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_EGON_IDP_CLIENT_ID \
    "${client_id}"
  write_env "${file}" \
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_EGON_IDP_CLIENT_SECRET \
    "${secret_value}"
  write_env "${file}" \
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_EGON_IDP_AUTHORIZATION_GRANT_TYPE \
    client_credentials
  write_env "${file}" \
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_EGON_IDP_CLIENT_AUTHENTICATION_METHOD \
    client_secret_basic
  write_env "${file}" \
    SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_EGON_IDP_TOKEN_URI \
    "${idp_url}/oauth2/token"
  write_env "${file}" EGON_COLA_PLATFORM_IDP_SERVICE_CLIENT_APP_ID \
    "${client_id}"
  write_env "${file}" EGON_COLA_PLATFORM_IDP_SERVICE_CLIENT_REGISTRATION_ID \
    egon-idp
}

write_tenant_aware_rbac3_service_token_env() {
  local file="$1" client_id="$2"
  write_idp_service_client_env "${file}" "${client_id}"
  write_env "${file}" \
    EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_SERVICE_TOKEN_ENABLED true
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
  write_env "${file}" DDC_RPC_TARGET "${ddc_rpc_target}"
  write_env "${file}" DDC_RPC_DEVELOPMENT_PLAINTEXT true
  write_env "${file}" DDC_RPC_RUNTIME_ACCESS_KEY "$(<"${secret_dir}/ddc-runtime.access-key")"
  write_env "${file}" DDC_RPC_RUNTIME_SECRET_KEY "$(<"${secret_dir}/ddc-runtime.secret")"
  write_env "${file}" DDC_RPC_REGISTRY_ACCESS_KEY "$(<"${secret_dir}/ddc-registry.access-key")"
  write_env "${file}" DDC_RPC_REGISTRY_SECRET_KEY "$(<"${secret_dir}/ddc-registry.secret")"
  write_env "${file}" DDC_RPC_MANAGEMENT_ACCESS_KEY "$(<"${secret_dir}/ddc-management.access-key")"
  write_env "${file}" DDC_RPC_MANAGEMENT_SECRET_KEY "$(<"${secret_dir}/ddc-management.secret")"
  # DDC must start once before IdP exists. Self-registration is enabled only
  # after IdP can issue the required PLATFORM SERVICE token.
  write_env "${file}" DDC_SELF_REGISTRATION_ENABLED false
  write_env "${file}" DDC_SELF_REGISTRATION_FAIL_FAST false
  write_env "${file}" DDC_INSTANCE_ID ddc-admin-local-1
  write_env "${file}" DDC_ADVERTISED_HOST "${advertised_host}"
  write_env "${file}" DDC_ADVERTISED_PORT 18150
  write_env "${file}" DDC_ARTIFACT_VERSION local
  write_env "${file}" DDC_MAX_CONFIG_BYTES 67108864
  write_env "${file}" DDC_REDIS_HOST "${redis_host}"
  write_env "${file}" DDC_REDIS_PORT "${redis_port}"
  write_env "${file}" DDC_REDIS_PASSWORD "${redis_password}"
  write_env "${file}" DDC_REDIS_DATABASE 10
  write_env "${file}" DDC_GATEWAY_REPORTING_ENABLED false
  write_env "${file}" GATEWAY_ADMIN_BASE_URL "${gateway_admin_url}"
  write_env "${file}" DDC_RESOURCE_BIZ_CODE platform
  write_env "${file}" DDC_RESOURCE_APP_CODE ddc
  write_env "${file}" DDC_DECLARED_HOSTS "${declared_hosts}"
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
  write_env "${file}" IDP_RPC_PROVIDER_REGISTRATION_MODE DISABLED
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
  write_env "${file}" IDP_ARTIFACT_VERSION local
  write_env "${file}" IDP_ADVERTISED_HOST "${advertised_host}"
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
  write_env "${file}" DDC_RPC_MANAGEMENT_ACCESS_KEY "$(<"${secret_dir}/ddc-management.access-key")"
  write_env "${file}" DDC_RPC_MANAGEMENT_SECRET_KEY "$(<"${secret_dir}/ddc-management.secret")"
  write_env "${file}" DDC_REGISTRY_REDIS_HOST "${redis_host}"
  write_env "${file}" DDC_REGISTRY_REDIS_PORT "${redis_port}"
  write_env "${file}" DDC_REGISTRY_REDIS_PASSWORD "${redis_password}"
  write_env "${file}" DDC_REGISTRY_REDIS_DATABASE 10
  write_env "${file}" IDP_GATEWAY_REPORTING_ENABLED false
  write_env "${file}" GATEWAY_ADMIN_BASE_URL "${gateway_admin_url}"
  write_env "${file}" IDP_RESOURCE_BIZ_CODE permission
  write_env "${file}" IDP_RESOURCE_APP_CODE idp
  write_env "${file}" IDP_DECLARED_HOSTS "${declared_hosts}"
  write_env "${file}" GATEWAY_REPORT_STATE_FILE "${runtime_dir}/idp-gateway-report.json"

  file="$(new_env_file rbac3)"
  common_identity_env "${file}"
  write_tenant_aware_rbac3_service_token_env "${file}" rbac3-service
  write_env "${file}" RBAC3_POSTGRES_URL "$(rbac3_jdbc_url)"
  write_env "${file}" RBAC3_POSTGRES_USER "${postgres_user}"
  write_env "${file}" RBAC3_POSTGRES_PASSWORD "${postgres_password_value}"
  write_env "${file}" RBAC3_ADVERTISED_PORT 18130
  write_env "${file}" RBAC3_ADVERTISED_HOST "${advertised_host}"
  write_env "${file}" RBAC3_INSTANCE_ID rbac3-local-1
  write_env "${file}" RBAC3_ARTIFACT_VERSION local
  # The RBAC3 identity-directory references are direct RPC references. Keep the
  # consumer disabled until DDC has admitted the IdP RPC Provider lease.
  write_env "${file}" RBAC3_RPC_ENABLED false
  write_env "${file}" RBAC3_RPC_CONSUMER_ENABLED false
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
  write_env "${file}" DDC_RPC_MANAGEMENT_ACCESS_KEY "$(<"${secret_dir}/ddc-management.access-key")"
  write_env "${file}" DDC_RPC_MANAGEMENT_SECRET_KEY "$(<"${secret_dir}/ddc-management.secret")"
  write_env "${file}" DDC_REGISTRY_REDIS_HOST "${redis_host}"
  write_env "${file}" DDC_REGISTRY_REDIS_PORT "${redis_port}"
  write_env "${file}" DDC_REGISTRY_REDIS_PASSWORD "${redis_password}"
  write_env "${file}" DDC_REGISTRY_REDIS_DATABASE 10
  write_env "${file}" RBAC3_AUTHORIZATION_REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
  write_env "${file}" RBAC3_AUTHORIZATION_REDIS_DATABASE 8
  write_env "${file}" RBAC3_RUNTIME_REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
  write_env "${file}" RBAC3_RUNTIME_REDIS_DATABASE 8
  write_env "${file}" RBAC3_RUNTIME_REDIS_PASSWORD_FILE "${secret_dir}/redis.password"
  write_env "${file}" RBAC3_AUDIT_CURSOR_SECRET_FILE "${secret_dir}/rbac3-audit.secret"
  write_env "${file}" RBAC3_SNOWFLAKE_MACHINE_ID 33
  write_env "${file}" RBAC3_DEVELOPMENT_BOOTSTRAP_ENABLED false
  write_env "${file}" RBAC3_DEVELOPMENT_AUTO_ACTIVATE_LOCAL_ADMIN_ROLES true
  write_env "${file}" RBAC3_DEVELOPMENT_TENANT_IDS "${service_tenant_id}"
  write_env "${file}" SPRING_FLYWAY_ENABLED true
  write_env "${file}" RBAC3_GATEWAY_REPORTING_ENABLED false
  write_env "${file}" GATEWAY_ADMIN_BASE_URL "${gateway_admin_url}"
  write_env "${file}" RBAC3_RESOURCE_BIZ_CODE permission
  write_env "${file}" RBAC3_RESOURCE_APP_CODE rbac3
  write_env "${file}" RBAC3_DECLARED_HOSTS "${declared_hosts}"
  write_env "${file}" GATEWAY_REPORT_STATE_FILE "${runtime_dir}/rbac3-gateway-report.json"

  file="$(new_env_file gateway-admin)"
  common_identity_env "${file}"
  write_env "${file}" DEPLOYMENT_ENV local
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
  # Local OpenAPI ingestion is explicitly restricted to the detected provider
  # host. Production keeps the HTTPS-only default from application.yml.
  write_env "${file}" GATEWAY_ADMIN_OPENAPI_ENABLED true
  write_env "${file}" GATEWAY_ADMIN_OPENAPI_ALLOW_DEVELOPMENT_HTTP true
  write_env "${file}" GATEWAY_ADMIN_OPENAPI_ALLOWED_CIDR \
    "${advertised_host}/32"
  write_env "${file}" GATEWAY_ADMIN_OPENAPI_RECONCILE_DELAY PT1S
  write_env "${file}" DDC_MAX_CONFIG_BYTES 67108864
  write_env "${file}" EGON_COLA_COMPONENT_DDC_RPC_DEFAULT_TIMEOUT 300s
  write_env "${file}" GATEWAY_ADMIN_RULE_CHUNK_RETENTION 1s
  write_env "${file}" GATEWAY_ADMIN_RULE_CHUNK_CLEANUP_DELAY 1s
  write_env "${file}" DDC_ENABLED true
  write_env "${file}" DDC_REGISTRY_ENABLED true
  write_env "${file}" GATEWAY_ADMIN_DDC_REGISTRATION_ENABLED true
  write_env "${file}" DDC_RPC_TARGET "${ddc_rpc_target}"
  write_env "${file}" DDC_RPC_DEVELOPMENT_PLAINTEXT true
  write_env "${file}" DDC_RPC_RUNTIME_ACCESS_KEY "$(<"${secret_dir}/ddc-runtime.access-key")"
  write_env "${file}" DDC_RPC_RUNTIME_SECRET_KEY "$(<"${secret_dir}/ddc-runtime.secret")"
  write_env "${file}" DDC_RPC_REGISTRY_ACCESS_KEY "$(<"${secret_dir}/ddc-registry.access-key")"
  write_env "${file}" DDC_RPC_REGISTRY_SECRET_KEY "$(<"${secret_dir}/ddc-registry.secret")"
  write_env "${file}" DDC_RPC_MANAGEMENT_ACCESS_KEY "$(<"${secret_dir}/ddc-management.access-key")"
  write_env "${file}" DDC_RPC_MANAGEMENT_SECRET_KEY "$(<"${secret_dir}/ddc-management.secret")"
  write_env "${file}" DDC_REDIS_HOST "${redis_host}"
  write_env "${file}" DDC_REDIS_PORT "${redis_port}"
  write_env "${file}" DDC_REDIS_PASSWORD "${redis_password}"
  write_env "${file}" DDC_REDIS_DATABASE 10
  write_env "${file}" GATEWAY_ADMIN_DDC_ADVERTISED_HOST "${advertised_host}"
  write_env "${file}" GATEWAY_ADMIN_DDC_ADVERTISED_PORT 18140
  write_env "${file}" GATEWAY_ADMIN_VERSION local
  write_env "${file}" GATEWAY_ADMIN_DDC_TARGET_BIZ_CODE identity
  write_env "${file}" GATEWAY_ADMIN_DDC_TARGET_APP_CODE gateway-engine-default
  write_env "${file}" GATEWAY_ADMIN_DEFINITION_RECONCILE_DELAY 1000
  if [[ "${startup_mode}" == "platforms" ]]; then
    write_env "${file}" GATEWAY_ADMIN_RELEASE_RECONCILE_ENABLED false
  fi
  write_env "${file}" GATEWAY_ADMIN_GATEWAY_REPORTING_ENABLED false
  write_env "${file}" GATEWAY_ADMIN_BASE_URL "${gateway_admin_url}"
  write_env "${file}" GATEWAY_ADMIN_RESOURCE_BIZ_CODE platform
  write_env "${file}" GATEWAY_ADMIN_RESOURCE_APP_CODE gateway-admin
  write_env "${file}" GATEWAY_ADMIN_DECLARED_HOSTS "${declared_hosts}"
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
  write_env "${file}" MOCK_BACKEND_ADVERTISED_HOST "${advertised_host}"
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
  write_env "${file}" EGON_COLA_PLATFORM_RBAC3_SYSTEM_CODE mock-backend
  write_env "${file}" EGON_COLA_PLATFORM_RBAC3_AUTHORIZATION_ENDPOINT \
    "${rbac3_url}"
  write_env "${file}" SERVER_PORT 18182
  write_env "${file}" IDP_OAUTH_ISSUER "${idp_url}"
  write_env "${file}" IDP_JWK_SET_URI "${idp_url}/oauth2/jwks"
  write_env "${file}" EGON_COLA_COMPONENT_DDC_REGISTRATION_RESOURCE_URI \
    https://api.egon.internal/local/platform/ddc
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
  write_env "${file}" IDP_REFRESH_URI "${idp_url}/oauth2/token"
  write_env "${file}" IDP_ACCESS_TOKEN_COOKIE_NAME egon_user_at_local
  write_env "${file}" IDP_REFRESH_TOKEN_COOKIE_NAME egon_user_rt_local
  write_env "${file}" IDP_REFRESH_STATUS_RESOURCE_URI \
    https://api.egon.internal/local/permission/idp
  write_env "${file}" IDP_REFRESH_STATUS_SCOPES idp:refresh-token:validate
  write_env "${file}" IDP_GATEWAY_TRUSTED_ORIGINS \
    http://127.0.0.1:18121,http://127.0.0.1:18131,http://127.0.0.1:18141,http://127.0.0.1:18152
  write_env "${file}" GATEWAY_RBAC3_SCOPE_ENABLED true
  write_env "${file}" GATEWAY_RBAC3_SCOPE_REDIS_ADDRESS \
    "redis://${redis_host}:${redis_port}"
  write_env "${file}" GATEWAY_RBAC3_SCOPE_REDIS_DATABASE 8
  write_env "${file}" GATEWAY_RBAC3_SCOPE_REDIS_PASSWORD_FILE \
    "${secret_dir}/redis.password"
  write_env "${file}" GATEWAY_RBAC3_SCOPE_REDIS_TIMEOUT 2s
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
  write_env "${file}" DDC_MAX_CONFIG_BYTES 67108864
  write_env "${file}" EGON_COLA_COMPONENT_DDC_RPC_MAX_INBOUND_MESSAGE_SIZE \
    67108864
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
  write_env "${file}" GATEWAY_ENGINE_DDC_ADVERTISED_HOST "${advertised_host}"
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
  for command in java curl jq openssl psql createdb redis-cli awk nc; do
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

wait_ddc_rpc() {
  local target="${ddc_rpc_target##*/}" host port
  host="${target%:*}"
  port="${target##*:}"
  [[ -n "${host}" && "${port}" =~ ^[0-9]+$ ]] \
    || fail "invalid DDC RPC target: ${ddc_rpc_target}"
  for ((attempt = 1; attempt <= 90; attempt++)); do
    if nc -z -w 1 "${host}" "${port}" >/dev/null 2>&1; then
      return
    fi
    sleep 1
  done
  fail "DDC RPC did not become ready at ${ddc_rpc_target}"
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
  if database_table_exists "${idp_database}" public.identity_tenant; then
    tenant_id="$(psql_command "${idp_database}" -Atqc \
      "select id from identity_tenant where lower(tenant_code) = '${tenant_code}'")"
  else
    tenant_id="$(psql_command "${rbac3_database}" -Atqc \
      "select id from rbac3_tenant where lower(code) = '${tenant_code}'")"
  fi
  [[ -n "${tenant_id}" ]] || fail "IdP tenant does not exist: ${tenant_code}"
  printf '%s' "${tenant_id}"
}

resolve_existing_service_tenant_id() {
  if [[ "${service_tenant_id}" =~ ^[1-9][0-9]*$ ]]; then
    return
  fi
  if database_table_exists "${idp_database}" public.identity_tenant \
      || database_table_exists "${rbac3_database}" public.rbac3_tenant; then
    service_tenant_id="$(rbac3_tenant_id "${service_tenant_id}")"
  fi
}

rbac3_jdbc_url() {
  local base source_count bootstrap_tenant_ids bootstrap_identity_sub
  base="jdbc:postgresql://${postgres_host}:${postgres_port}/${rbac3_database}"
  if database_table_exists "${rbac3_database}" \
      public.rbac3_tenant_authorization_state; then
    printf '%s' "${base}"
    return
  fi
  source_count=0
  if database_table_exists "${rbac3_database}" public.rbac3_tenant; then
    source_count="$(psql_command "${rbac3_database}" -Atqc \
      'select count(*) from rbac3_tenant')"
  fi
  bootstrap_tenant_ids="${service_tenant_id}"
  if [[ -n "${tenant_authority_artifact}" \
      && -s "${tenant_authority_artifact}" ]]; then
    bootstrap_tenant_ids="$(jq -er \
      '[.tenants[].id] | unique | join(",")' \
      "${tenant_authority_artifact}")"
  fi
  [[ "${bootstrap_tenant_ids}" =~ ^[1-9][0-9]{0,18}(,[1-9][0-9]{0,18})*$ ]] \
    || fail "RBAC3 bootstrap tenant IDs are invalid"
  bootstrap_identity_sub=""
  if database_table_exists "${idp_database}" public.identity_user; then
    bootstrap_identity_sub="$(identity_subject)"
  fi
  [[ -z "${bootstrap_identity_sub}" \
      || "${bootstrap_identity_sub}" =~ ^[A-Za-z0-9._~-]{1,200}$ ]] \
    || fail "RBAC3 bootstrap identity subject is invalid"
  printf '%s?options=-c%%20rbac3.tenant_authority.gate_id=VERIFIED%%20-c%%20rbac3.tenant_authority.gate_checksum=local-bootstrap%%20-c%%20rbac3.tenant_authority.source_count=%s%%20-c%%20rbac3.tenant_authority.orphan_count=0%%20-c%%20rbac3.tenant_authority.duplicate_count=0%%20-c%%20rbac3.tenant_authority.placeholder_count=0%%20-c%%20rbac3.bootstrap.tenant_ids=%s%%20-c%%20rbac3.bootstrap.identity_sub=%s' \
    "${base}" "${source_count}" "${bootstrap_tenant_ids}" \
    "${bootstrap_identity_sub}"
}

adopt_local_idp_authority() {
  if [[ -n "${tenant_authority_artifact}" ]]; then
    [[ -s "${tenant_authority_artifact}" ]] \
      || fail "tenant authority artifact is unreadable"
    PGPASSWORD="$(postgres_password)" \
      "${repo_root}/scripts/unified-platform/migrate-tenant-authority.sh" \
      import-idp \
      --db-url "postgresql://${postgres_user}@${postgres_host}:${postgres_port}/${idp_database}" \
      --freeze-marker "$(dirname "${tenant_authority_artifact}")/write-freeze.marker" \
      --artifact "${tenant_authority_artifact}"
    PGPASSWORD="$(postgres_password)" \
      "${repo_root}/scripts/unified-platform/migrate-tenant-authority.sh" \
      verify-idp \
      --artifact "${tenant_authority_artifact}" \
      --db-url "postgresql://${postgres_user}@${postgres_host}:${postgres_port}/${idp_database}"
  fi
  psql_command "${idp_database}" -qc \
    "update identity_client set app_id = client_id where client_type = 'CONFIDENTIAL' and app_id is null"
}

reconcile_local_rbac3_ddc_catalog() {
  local definition application_code ddc_app_code ddc_business_code
  local catalog_ids ddc_application_id ddc_business_id application_count access_count
  local definitions=(
    'rbac3-admin|rbac3|permission'
    'idp-admin|idp|permission'
    'gateway-admin|gateway-admin|platform'
    'ddc-admin|ddc|platform'
    'mock-backend|mock-backend|identity'
  )
  for definition in "${definitions[@]}"; do
    IFS='|' read -r application_code ddc_app_code ddc_business_code \
      <<<"${definition}"
    [[ "${application_code}" =~ ^[a-z0-9-]{1,64}$ \
        && "${ddc_app_code}" =~ ^[a-z0-9-]{1,64}$ \
        && "${ddc_business_code}" =~ ^[a-z0-9-]{1,64}$ ]] \
      || fail "unsafe local DDC catalog mapping"
    catalog_ids="$(psql_command "${ddc_database}" -AtF '|' -c \
      "select application.id, business.id
         from ddc_app application
         join ddc_biz business on business.biz_code = application.biz_code
        where application.app_code = '${ddc_app_code}'
          and business.biz_code = '${ddc_business_code}'
          and application.enabled
          and business.enabled")"
    [[ "${catalog_ids}" =~ ^[A-Za-z0-9_-]{1,64}\|[A-Za-z0-9_-]{1,64}$ ]] \
      || fail "DDC catalog mapping is unavailable for ${application_code}"
    ddc_application_id="${catalog_ids%%|*}"
    ddc_business_id="${catalog_ids#*|}"
    psql_command "${rbac3_database}" -qc \
      "update rbac3_application
          set ddc_application_id = '${ddc_application_id}',
              ddc_business_id = '${ddc_business_id}',
              updated_at = current_timestamp,
              updated_by = 'local-ddc-reconciliation'
        where application_code = '${application_code}'
          and created_by = 'flyway-v10';
       update rbac3_user_business_access
          set ddc_business_id = '${ddc_business_id}',
              updated_at = current_timestamp,
              updated_by = 'local-ddc-reconciliation'
        where source_type = 'SYSTEM'
          and source_id = 'flyway-v11:${ddc_business_code}';"
    application_count="$(psql_command "${rbac3_database}" -Atqc \
      "select count(*) from rbac3_application
        where application_code = '${application_code}'
          and ddc_application_id = '${ddc_application_id}'
          and ddc_business_id = '${ddc_business_id}'")"
    [[ "${application_count}" == "1" ]] \
      || fail "RBAC3 application mapping failed for ${application_code}"
    access_count="$(psql_command "${rbac3_database}" -Atqc \
      "select count(*) from rbac3_user_business_access
        where source_type = 'SYSTEM'
          and source_id = 'flyway-v11:${ddc_business_code}'
          and ddc_business_id = '${ddc_business_id}'")"
    [[ "${access_count}" =~ ^[1-9][0-9]*$ ]] \
      || fail "RBAC3 Business access mapping failed for ${ddc_business_code}"
  done
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
  token="$(awk 'BEGIN { FS="\t" } { sub(/^#HttpOnly_/, "", $1) } $0 !~ /^#/ && ($6 == "__Host-egon_user_at" || $6 == "egon_user_at_local") { value=$7 } END { print value }' "${cookie_jar}")"
  [[ "${token}" =~ ^[^.[:space:]]+\.[^.[:space:]]+\.[^.[:space:]]+$ ]] \
    || fail "USER Access Token cookie is missing from ${cookie_jar}"
  printf '%s' "${token}"
}

user_access_token_is_active() {
  local access_token="$1" http_code
  http_code="$(curl -sS -o /dev/null -w '%{http_code}' \
    -H "Authorization: Bearer ${access_token}" \
    "${idp_url}/oauth2/userinfo" 2>/dev/null || true)"
  [[ "${http_code}" == "200" ]]
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
  if [[ "${UNIFIED_IDENTITY_DEFER_GATEWAY_RELEASE:-false}" != "true" ]] \
      && process_running gateway-engine; then
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
  if ! token="$(access_token_from_cookie "${cookie_jar}" 2>/dev/null)" \
      || ! user_access_token_is_active "${token}"; then
    platform_user_login "${tenant}"
    token="$(access_token_from_cookie "${cookie_jar}")"
  fi
  printf '%s' "${token}"
}

clear_local_rbac3_snapshots() {
  local password pattern key tenant
  password="$(<"${secret_dir}/redis.password")"
  for tenant in "${service_tenant_id}" "${tenant_b_id}"; do
    [[ "${tenant}" =~ ^[1-9][0-9]*$ ]] || continue
    pattern="rbac3:{${tenant}}:snapshot:*"
    while IFS= read -r key; do
      [[ -n "${key}" ]] || continue
      REDISCLI_AUTH="${password}" redis-cli -h "${redis_host}" \
        -p "${redis_port}" -n 8 UNLINK "${key}" >/dev/null
    done < <(REDISCLI_AUTH="${password}" redis-cli -h "${redis_host}" \
      -p "${redis_port}" -n 8 --scan --pattern "${pattern}")
  done
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
  if [[ "$(jq 'length' <<<"${role_ids}")" -eq 0 ]]; then
    if jq -e '.data.activationRequired == false
        and (.data.activeRoles | length) > 0' \
        <<<"${current}" >/dev/null; then
      return
    fi
    fail "RBAC3 returned no activation candidates or active roles"
  fi
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

wait_ddc_rpc_provider_registration() {
  local biz_code="$1" app_code="$2" service_name="$3" group="$4" version="$5" response
  for ((attempt = 1; attempt <= 30; attempt++)); do
    response="$(ddc_api GET \
      "/api/v1/ddc/registry/services?bizCode=${biz_code}&namespaceCode=default&env=local&appCode=${app_code}&serviceKind=RPC_PROVIDER&protocol=grpc&serviceName=${service_name}&group=${group}&version=${version}")"
    if jq -e --arg app "${app_code}" --arg service "${service_name}" \
        --arg group "${group}" --arg version "${version}" '
        .data.services[]
        | select(
            .appCode == $app
            and .serviceKind == "RPC_PROVIDER"
            and .protocol == "grpc"
            and .serviceName == $service
            and .group == $group
            and .version == $version
          )
      ' <<<"${response}" >/dev/null; then
      return
    fi
    sleep 1
  done
  fail "${biz_code}/${app_code}/${service_name} did not register an online DDC RPC Provider lease"
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
  local app_id credential access_file secret_file
  ensure_gateway_application "${biz_code}" "${app_code}" "${display_name}"
  app_id="$(<"$(gateway_application_id_file "${app_code}")")"
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

ensure_gateway_application() {
  local biz_code="$1" app_code="$2" display_name="$3"
  local applications application app_id
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
  printf '%s' "${app_id}" >"$(gateway_application_id_file "${app_code}")"
  chmod 600 "$(gateway_application_id_file "${app_code}")"
}

initialize_gateway_control_plane() {
  local groups group group_id
  if [[ "${startup_mode}" == "full" ]]; then
    ensure_gateway_reporting_application permission idp "IdP Identity Admin"
    ensure_gateway_reporting_application permission rbac3 "RBAC3 Permission Admin"
    ensure_gateway_reporting_application platform gateway-admin "Gateway Admin"
    ensure_gateway_reporting_application platform ddc "Dynamic Config Center Admin"
    ensure_gateway_reporting_application identity mock-backend "Unified Identity Mock Backend"
  else
    ensure_gateway_application permission idp "IdP Identity Admin"
    ensure_gateway_application permission rbac3 "RBAC3 Permission Admin"
    ensure_gateway_application platform gateway-admin "Gateway Admin"
    ensure_gateway_application platform ddc "Dynamic Config Center Admin"
    ensure_gateway_application identity mock-backend "Unified Identity Mock Backend"
  fi

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

wait_gateway_engine_provider_registration() {
  local response
  for ((attempt = 1; attempt <= 60; attempt++)); do
    response="$(gateway_api GET \
      '/api/v1/gateway/admin/providers/instances?bizCode=identity&appCode=gateway-engine-default&env=local&namespace=default' \
      || true)"
    if jq -e '
        .value[]?
        | select(.status == "ONLINE" and .instanceId == "gateway-engine-local-1")
      ' <<<"${response}" >/dev/null 2>&1; then
      return
    fi
    sleep 1
  done
  fail "Gateway Engine did not publish an online DDC provider registration: ${response}"
}

wait_gateway_openapi_sync_for_app() {
  local biz_code="$1" app_code="$2" response build_id
  build_id="$(local_build_id "${rbac3_jar}")"
  for ((attempt = 1; attempt <= 60; attempt++)); do
    response="$(gateway_api GET \
      "/api/v1/gateway/admin/openapi/sync-states?bizCode=${biz_code}&namespace=default&env=local&appCode=${app_code}" \
      || true)"
    if jq -e --arg build "${build_id}" '
        [ .[] | select(.buildId == $build) ] as $current
        | ($current | length) > 0
        and all($current[]; .status == "VALID" and .definitionSetId != null)
      ' <<<"${response}" >/dev/null 2>&1; then
      return
    fi
    sleep 1
  done
  fail "${biz_code}/${app_code} Gateway OpenAPI sync did not become valid: ${response}"
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

reconcile_platform_mcp_draft() {
  local group_id="$1" servers server_id managed_tools valid_tools
  local task_policies policy_id policy_name policy_revision response revision
  local app_bindings binding_id binding_revision binding_valid
  servers="$(gateway_api GET \
    "/api/v1/gateway/admin/mcp/servers?gatewayGroupId=${group_id}")"
  while IFS= read -r server_id; do
    [[ -n "${server_id}" ]] || continue
    managed_tools="$(gateway_api GET \
      "/api/v1/gateway/admin/mcp/groups/${group_id}/managed-tools?serverId=${server_id}")"
    valid_tools="$(jq -c '[.[] | select(.enabled == true) | .name] | unique' \
      <<<"${managed_tools}")"

    task_policies="$(gateway_api GET \
      "/api/v1/gateway/admin/mcp/servers/${server_id}/task-policies?gatewayGroupId=${group_id}")"
    while IFS= read -r policy; do
      policy_id="$(jq -er '.id' <<<"${policy}")"
      policy_name="$(jq -er '.name' <<<"${policy}")"
      if jq -e --arg name "${policy_name}" \
          --argjson tools "${valid_tools}" \
          '$tools | index($name) != null' <<<"null" \
          >/dev/null 2>&1; then
        continue
      fi
      policy_revision="$(jq -er '.revision' <<<"${policy}")"
      revision="$(gateway_api GET \
        "/api/v1/gateway/admin/gateway-groups/${group_id}/draft" \
        | jq -er '.revision')"
      response="$(gateway_api DELETE \
        "/api/v1/gateway/admin/mcp/task-policies/${policy_id}" \
        "$(jq -cn --arg group "${group_id}" --argjson expected "${policy_revision}" \
          --argjson draft "${revision}" \
          '{gatewayGroupId:$group,expectedRevision:$expected,expectedDraftRevision:$draft,changeReason:"Remove platform-mode MCP task policy without a valid Tool"}')" \
        "unified-platform-remove-mcp-task-${policy_id}")"
      jq -e '.resourceId != null' <<<"${response}" >/dev/null
    done < <(jq -c '.[]' <<<"${task_policies}")

    app_bindings="$(gateway_api GET \
      "/api/v1/gateway/admin/mcp/servers/${server_id}/app-bindings?gatewayGroupId=${group_id}")"
    while IFS= read -r binding; do
      binding_id="$(jq -er '.id' <<<"${binding}")"
      binding_valid="$(jq -e --argjson tools "${valid_tools}" '
        ((.content.allowedTools // []) - $tools | length) == 0
      ' <<<"${binding}" >/dev/null 2>&1; echo $?)"
      [[ "${binding_valid}" == "0" ]] && continue
      binding_revision="$(jq -er '.revision' <<<"${binding}")"
      revision="$(gateway_api GET \
        "/api/v1/gateway/admin/gateway-groups/${group_id}/draft" \
        | jq -er '.revision')"
      response="$(gateway_api DELETE \
        "/api/v1/gateway/admin/mcp/app-bindings/${binding_id}" \
        "$(jq -cn --arg group "${group_id}" --argjson expected "${binding_revision}" \
          --argjson draft "${revision}" \
          '{gatewayGroupId:$group,expectedRevision:$expected,expectedDraftRevision:$draft,changeReason:"Remove platform-mode MCP app binding with invalid Tool references"}')" \
        "unified-platform-remove-mcp-app-${binding_id}")"
      jq -e '.resourceId != null' <<<"${response}" >/dev/null
    done < <(jq -c '.[]' <<<"${app_bindings}")
  done < <(jq -r '.[].id' <<<"${servers}")
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
  local method path route_id legacy_route_id stale_route_id route_content desired_policy
  local managed_operation_ids cors_policy_id cors_origins cors_methods
  local cors_headers cors_exposed security_type route_transport_policy
  group_id="$(<"${runtime_dir}/gateway-group.id")"
  operations="$(gateway_catalog_operations | jq '
    map(select(.protocol == "HTTP" and .externalAccessible == true
      and .lifecycleStatus == "ACTIVE"))
    | map(select(.methodIdentity != "GET /api/v1/auth/bootstrap"
      or .reportedApplication == "gateway-admin"))
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
  managed_operation_ids="$(jq '[.[].id]' <<<"${operations}")"

  while IFS= read -r stale_route_id; do
    [[ -n "${stale_route_id}" ]] || continue
    response="$(gateway_api DELETE \
      "/api/v1/gateway/admin/gateway-groups/${group_id}/draft/routes/${stale_route_id}" \
      "$(jq -cn --argjson revision "${revision}" \
        --arg route "${stale_route_id}" \
        '{expectedRevision:$revision,idempotencyKey:("unified-remove-stale-route-" + $route + "-" + ($revision | tostring)),changeReason:"Remove a deterministic route whose operation is no longer selected for the unified local topology"}')")"
    revision="$(jq -er '.revision' <<<"${response}")"
    draft="$(gateway_api GET "/api/v1/gateway/admin/gateway-groups/${group_id}/draft")"
  done < <(jq -r --argjson operations "${managed_operation_ids}" '
    .routes[]?
    | select(([.operationId] - $operations | length) > 0
      and ((.routeId | startswith("unified-")) or .enabled != true))
    | .routeId' <<<"${draft}")

  if jq -e '.policies[]? | select(.policyId == "identity-basic" and .policyScope == "GLOBAL")' \
      <<<"${draft}" >/dev/null; then
    response="$(gateway_api DELETE \
      "/api/v1/gateway/admin/gateway-groups/${group_id}/draft/policies/identity-basic" \
      "$(jq -cn --argjson revision "${revision}" \
        '{expectedRevision:$revision,idempotencyKey:("unified-remove-identity-basic-" + ($revision | tostring)),changeReason:"Replace legacy global policy with operation-scoped stateless policies"}')")"
    revision="$(jq -er '.revision' <<<"${response}")"
    draft="$(gateway_api GET "/api/v1/gateway/admin/gateway-groups/${group_id}/draft")"
  fi

  cors_policy_id=unified-local-cors
  cors_origins='["http://127.0.0.1:18121","http://127.0.0.1:18131","http://127.0.0.1:18141","http://127.0.0.1:18152"]'
  cors_methods='["GET","POST","PUT","PATCH","DELETE","OPTIONS"]'
  cors_headers='["Authorization","Content-Type","X-IDP-CSRF","X-CSRF-TOKEN","Idempotency-Key"]'
  cors_exposed='["traceparent","x-egon-request-id"]'
  ids="$(jq -c '[.[].id] | sort' <<<"${operations}")"
  if ! jq -e --arg policy "${cors_policy_id}" --argjson ids "${ids}" \
      --argjson origins "${cors_origins}" --argjson methods "${cors_methods}" \
      --argjson headers "${cors_headers}" --argjson exposed "${cors_exposed}" '
      .policies[]? | select(
        .policyId == $policy and .policyType == "CORS"
        and .policyScope == "OPERATION" and .enabled == true
        and .content.operationIds == $ids
        and .content.allowedOrigins == $origins
        and .content.allowedMethods == $methods
        and .content.allowedHeaders == $headers
        and .content.exposedHeaders == $exposed
        and .content.allowCredentials == true
        and .content.maxAgeSeconds == 600
      )' <<<"${draft}" >/dev/null; then
    desired_policy="$(jq -cn --argjson ids "${ids}" \
      --argjson origins "${cors_origins}" --argjson methods "${cors_methods}" \
      --argjson headers "${cors_headers}" --argjson exposed "${cors_exposed}" \
      --argjson revision "${revision}" --arg policy "${cors_policy_id}" \
      '{policyType:"CORS",policyScope:"OPERATION",content:{operationIds:$ids,allowedOrigins:$origins,allowedMethods:$methods,allowedHeaders:$headers,exposedHeaders:$exposed,allowCredentials:true,maxAgeSeconds:600,enabled:true},enabled:true,expectedRevision:$revision,idempotencyKey:("unified-policy-" + $policy + "-" + ($revision | tostring)),changeReason:"Publish local frontend CORS policy for the unified platform"}')"
    response="$(gateway_api PUT \
      "/api/v1/gateway/admin/gateway-groups/${group_id}/draft/policies/${cors_policy_id}" \
      "${desired_policy}")"
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
        extractors='["idp-user-cookie"]'; auth_providers='["idp-jwt"]'; authz_providers='["rbac3-biz-app-scope"]' ;;
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

  while IFS=$'\t' read -r operation_id method_identity app_code security_type; do
    [[ -n "${operation_id}" && -n "${method_identity}" ]] || continue
    method="${method_identity%% *}"
    path="${method_identity#* }"
    route_id="$(route_id_for_operation "${operation_id}")"
    if [[ "${security_type}" == "PUBLIC_PROTOCOL" ]]; then
      route_transport_policy=null
    else
      route_transport_policy='{"profile":"OPENAI_HTTP","transportProtocol":"HTTP","requestBodyMode":"AGGREGATED","responseMode":"STANDARD","maxRequestBodyBytes":16777216,"connectTimeoutMs":30000,"responseHeaderTimeoutMs":120000,"streamIdleTimeoutMs":120000,"totalTimeoutMs":120000,"bodyLogEnabled":false,"retryEnabled":false}'
    fi
    while IFS= read -r legacy_route_id; do
      [[ -n "${legacy_route_id}" ]] || continue
      response="$(gateway_api DELETE \
        "/api/v1/gateway/admin/gateway-groups/${group_id}/draft/routes/${legacy_route_id}" \
        "$(jq -cn --argjson revision "${revision}" \
          --arg route "${legacy_route_id}" \
          '{expectedRevision:$revision,idempotencyKey:("unified-remove-legacy-route-" + $route + "-" + ($revision | tostring)),changeReason:"Remove a legacy route duplicated by the deterministic reported-operation route"}')")"
      revision="$(jq -er '.revision' <<<"${response}")"
      draft="$(gateway_api GET "/api/v1/gateway/admin/gateway-groups/${group_id}/draft")"
    done < <(jq -r --arg route "${route_id}" \
      --arg operation "${operation_id}" --arg method "${method}" \
      --arg path "${path}" '
      .routes[]?
      | select(.routeId != $route and .operationId == $operation
        and .enabled == true and .content.host == "*"
        and .content.httpMethod == $method
        and .content.pathPattern == $path
        and .content.accessZones == ["PUBLIC"])
      | .routeId' <<<"${draft}")
    if jq -e --arg route "${route_id}" --arg operation "${operation_id}" \
        --arg method "${method}" --arg path "${path}" \
        --argjson transport "${route_transport_policy}" '
        .routes[]? | select(.routeId == $route and .operationId == $operation
          and .enabled == true and .content.host == "*"
          and .content.httpMethod == $method
          and .content.pathPattern == $path
          and .content.accessZones == ["PUBLIC"]
          and (.content.transportPolicy // null) == $transport)' <<<"${draft}" >/dev/null; then
      continue
    fi
    route_content="$(jq -cn --arg operation "${operation_id}" \
      --arg method "${method}" --arg path "${path}" \
      --argjson transport "${route_transport_policy}" \
      '{operationId:$operation,content:{host:"*",httpMethod:$method,pathPattern:$path,accessZones:["PUBLIC"],priority:100},enabled:true}
       | if $transport == null then . else .content.transportPolicy = $transport end')"
    response="$(gateway_api PUT \
      "/api/v1/gateway/admin/gateway-groups/${group_id}/draft/routes/${route_id}" \
      "$(jq -cn --argjson route "${route_content}" --argjson revision "${revision}" \
        --arg operation "${operation_id}" \
        '$route + {expectedRevision:$revision,idempotencyKey:("unified-route-" + $operation + "-" + ($revision | tostring)),changeReason:"Publish reported HTTP operation from the real provider catalog"}')")"
    revision="$(jq -er '.revision' <<<"${response}")"
    draft="$(gateway_api GET "/api/v1/gateway/admin/gateway-groups/${group_id}/draft")"
  done < <(jq -r '.[] | [.id,.methodIdentity,.reportedApplication,.securityType] | @tsv' \
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
  case "${startup_mode}" in
    platforms|full) ;;
    *) fail "unsupported UNIFIED_IDENTITY_START_MODE: ${startup_mode} (use platforms or full)" ;;
  esac
  local idp_argument subject tenant_b_id rbac3_access_token ddc_access_token
  stage "starting DDC"
  start_process ddc "${env_dir}/ddc.env" "${ddc_jar}"
  wait_http ddc "${ddc_url}/actuator/health/readiness"
  wait_ddc_rpc

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
  stage "adopting IdP tenant authority and local confidential app IDs"
  adopt_local_idp_authority
  write_service_env_files
  write_application_build_ids
  stage "issuing IdP-owned service credentials"
  refresh_service_tokens
  subject="$(identity_subject)"
  [[ -n "${subject}" ]] || fail "IdP bootstrap subject is missing"

  stage "starting RBAC3 bootstrap phase without DDC publication"
  write_env "${env_dir}/rbac3.env" RBAC3_DEVELOPMENT_IDENTITY_SUB "${subject}"
  start_process rbac3 "${env_dir}/rbac3.env" "${rbac3_jar}" \
    --egon.cola.component.ddc.enabled=true \
    --egon.cola.component.ddc.registry.enabled=false \
    --egon.cola.component.ddc.registry.http.enabled=false \
    --egon.rbac3.development-bootstrap.enabled=false
  wait_http rbac3 "${rbac3_url}/actuator/health/readiness"

  service_tenant_id="$(rbac3_tenant_id default)"
  tenant_b_id="$(rbac3_tenant_id tenant-b)"
  write_env "${env_dir}/idp.env" IDP_RBAC3_SERVICE_TENANT_ID \
    "${service_tenant_id}"
  write_env "${env_dir}/idp.env" IDP_DEVELOPMENT_RBAC3_SERVICE_TENANT_ID \
    "${service_tenant_id}"
  write_env "${env_dir}/idp.env" IDP_DEVELOPMENT_RBAC3_SERVICE_TENANT_IDS \
    "${service_tenant_id},${tenant_b_id}"
  write_env "${env_dir}/rbac3.env" RBAC3_DEVELOPMENT_TENANT_IDS \
    "${service_tenant_id},${tenant_b_id}"
  stage "binding IdP service credentials to the RBAC3 tenant"
  stop_process idp
  start_process idp "${env_dir}/idp.env" "${idp_jar}" \
    --egon.cola.component.ddc.enabled=false \
    --egon.cola.component.ddc.registry.enabled=false \
    --egon.cola.component.ddc.registry.http.enabled=false
  wait_http idp "${idp_url}/actuator/health/readiness"
  refresh_service_tokens

  stage "clearing stale local RBAC3 authorization snapshots"
  clear_local_rbac3_snapshots
  stage "establishing the default-tenant USER cookie"
  idp_bootstrap_login default
  stage "loading the default-tenant USER Access Token from its Gateway cookie"
  rbac3_access_token="$(user_access_token_for_tenant default)"
  stage "initializing DDC unified identity topology"
  ddc_access_token="$(user_access_token_for_tenant default)"
  initialize_ddc_topology "${ddc_access_token}"
  stage "reconciling SQL-seeded RBAC3 applications with DDC catalog IDs"
  reconcile_local_rbac3_ddc_catalog
  stage "activating non-mock roles"
  activate_roles "${rbac3_access_token}" false

  stage "restarting IdP and RBAC3 with admitted DDC publication"
  stop_process rbac3
  stop_process idp
  write_env "${env_dir}/ddc.env" DDC_SELF_REGISTRATION_ENABLED true
  stop_process ddc
  start_process ddc "${env_dir}/ddc.env" "${ddc_jar}"
  wait_http ddc "${ddc_url}/actuator/health/readiness"
  wait_ddc_rpc
  write_env "${env_dir}/rbac3.env" RBAC3_DEVELOPMENT_BOOTSTRAP_ENABLED true
  write_env "${env_dir}/rbac3.env" RBAC3_RPC_ENABLED true
  write_env "${env_dir}/rbac3.env" RBAC3_RPC_CONSUMER_ENABLED true
  write_env "${env_dir}/idp.env" IDP_RPC_PROVIDER_REGISTRATION_MODE REQUIRED
  start_process idp "${env_dir}/idp.env" "${idp_jar}"
  wait_http idp "${idp_url}/actuator/health/readiness"
  refresh_service_tokens
  stage "waiting for IdP identity RPC publication"
  start_process rbac3 "${env_dir}/rbac3.env" "${rbac3_jar}" \
    --egon.rbac3.development-bootstrap.enabled=false
  wait_http rbac3 "${rbac3_url}/actuator/health/readiness"
  stage "refreshing the USER token for DDC RPC registration polling"
  idp_bootstrap_login default
  ddc_admin_access_token="$(user_access_token_for_tenant default)"
  wait_ddc_rpc_provider_registration permission idp egon.idp.v1.IdentityDirectoryService idp 1.0.0
  stage "starting RBAC3 topology bootstrap after IdP RPC publication"
  stop_process rbac3
  start_process rbac3 "${env_dir}/rbac3.env" "${rbac3_jar}"
  wait_http rbac3 "${rbac3_url}/actuator/health/readiness"

  stage "restoring the USER cookie and RBAC3 activation context"
  idp_bootstrap_login default
  rbac3_access_token="$(user_access_token_for_tenant default)"
  activate_roles "${rbac3_access_token}" false
  wait_ddc_provider_registration permission idp idp-admin
  wait_ddc_provider_registration permission rbac3 rbac3-admin

  stage "starting Gateway Admin"
  start_process gateway-admin "${env_dir}/gateway-admin.env" "${gateway_admin_jar}"
  wait_http gateway-admin "${gateway_admin_url}/actuator/health/readiness"
  initialize_gateway_control_plane

  if [[ "${startup_mode}" == "platforms" ]]; then
    stage "waiting for RBAC3 OpenAPI group ingestion"
    wait_gateway_openapi_sync_for_app permission rbac3
    stage "reconciling orphaned local MCP draft capabilities"
    reconcile_platform_mcp_draft "$(<"${runtime_dir}/gateway-group.id")"
    stage "preparing the current local Gateway HTTP catalog draft"
    publish_gateway_routes true
    echo "Unified identity platform backends are running with a prepared Gateway OpenAPI catalog draft."
    echo "Start the Gateway Engine, publish the draft, and start the Admin Web and Portal applications with scripts/unified-platform/start-local-stack.sh."
    return
  fi

  stage "restarting providers with real Gateway catalog reporting"
  # Gateway Admin is the reporting control plane, so it first remains available with its own
  # reporting disabled while DDC, IdP, and RBAC3 publish their real HTTP catalogs.
  write_env "${env_dir}/gateway-admin.env" \
    GATEWAY_ADMIN_GATEWAY_REPORTING_ENABLED false
  stop_process gateway-admin
  stop_process rbac3
  start_process gateway-admin "${env_dir}/gateway-admin.env" "${gateway_admin_jar}"
  wait_http gateway-admin "${gateway_admin_url}/actuator/health/readiness"
  stop_process ddc
  start_process ddc "${env_dir}/ddc.env" "${ddc_jar}"
  wait_http ddc "${ddc_url}/actuator/health/readiness"
  wait_ddc_rpc
  stop_process idp
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

  stage "waiting for final DDC provider registrations"
  wait_ddc_provider_registration permission idp idp-admin
  wait_ddc_provider_registration permission rbac3 rbac3-admin
  stage "starting Gateway Engine after DDC control plane is ready"
  start_process gateway-engine "${env_dir}/gateway-engine.env" "${gateway_engine_jar}"
  wait_http gateway-engine http://127.0.0.1:18182/actuator/health/readiness

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
  local url="$1" access_token="$2" cookie_jar="${3:-}"
  local arguments=(-sS -o /dev/null -w '%{http_code}'
    -H "Authorization: Bearer ${access_token}")
  if [[ -n "${cookie_jar}" ]]; then
    arguments+=(-b "${cookie_jar}")
  fi
  curl "${arguments[@]}" "${url}"
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
    "${default_access_token}" "$(cookie_jar_for_tenant default)")"
  [[ "${status}" == "403" ]] || fail \
    "downstream permission denial must be 403 before mock role activation; got ${status}"
  activate_roles "${rbac3_access_token}" true
  for ((attempt = 1; attempt <= 20; attempt++)); do
    if [[ "$(http_status "${gateway_url}/api/mock/admin" \
      "${default_access_token}" "$(cookie_jar_for_tenant default)")" == "200" ]]; then
      break
    fi
    sleep 1
  done
  [[ "$(http_status "${gateway_url}/api/mock/admin" \
    "${default_access_token}" "$(cookie_jar_for_tenant default)")" == "200" ]] \
    || fail "role activation did not authorize the unchanged access token"

  platform_user_login tenant-b
  tenant_b_access_token="$(user_access_token_for_tenant tenant-b)"
  activate_roles "${tenant_b_access_token}" true
  [[ "$(http_status "${gateway_url}/api/mock/read" \
    "${tenant_b_access_token}" "$(cookie_jar_for_tenant tenant-b)")" == "200" ]] \
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
  [[ "$(http_status "${gateway_url}/api/mock/read" \
    "${default_access_token}" "$(cookie_jar_for_tenant default)")" == "200" ]] \
    || fail "fresh access token failed after stable refresh and logout"
  [[ "$(http_status "${gateway_url}/api/mock/read" \
    "${tenant_b_access_token}" "$(cookie_jar_for_tenant tenant-b)")" == "200" ]] \
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

command_publish_gateway_routes() {
  local command
  for command in curl jq openssl; do
    require_command "${command}"
  done
  initialize_directories
  process_running gateway-admin \
    || fail "gateway-admin is not running; run start first"
  process_running gateway-engine \
    || fail "gateway-engine is not running; start the platform stack first"
  [[ -s "${secret_dir}/gateway-admin-control-plane.service.jwt" ]] \
    || fail "Gateway control-plane SERVICE token is unavailable"
  [[ -s "${runtime_dir}/gateway-group.id" ]] \
    || fail "local Gateway group is unavailable; run start first"
  wait_http gateway-engine http://127.0.0.1:18182/actuator/health/readiness
  stage "waiting for Gateway Engine DDC provider registration"
  wait_gateway_engine_provider_registration
  stage "publishing the prepared local Gateway HTTP catalog"
  publish_gateway_routes
  wait_gateway_route
  echo "Unified identity Gateway HTTP catalog release is active."
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
  publish-gateway-routes) command_publish_gateway_routes ;;
  sync-local-credentials) command_refresh_tokens ;;
  issue-user-token) command_issue_user_token ;;
  verify) command_verify ;;
  status) command_status ;;
  stop) command_stop ;;
  *) usage >&2; exit 2 ;;
esac
