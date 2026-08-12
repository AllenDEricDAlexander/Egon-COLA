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
  refresh-tokens  Refresh local OAuth tokens and rebuild both tenant snapshots
  issue-user-token  Issue one local USER token from explicit environment inputs
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
  write_pending_service_credential "${secret_dir}/gateway-engine.service.jwt"
  write_pending_service_credential "${secret_dir}/ddc-admin.service.jwt"
  write_pending_service_credential "${secret_dir}/mock-backend.service.jwt"
  write_pending_service_credential "${secret_dir}/mcp-provider.service.jwt"
  printf 'Bearer %s' "$(<"${secret_dir}/idp-admin.service.jwt")" \
    >"${secret_dir}/idp-rbac3.authorization"
  chmod 600 "${secret_dir}/idp-rbac3.authorization"
}

oauth_service_token() {
  local client_id="$1" key_id="$2" key_stem="$3" output="$4"
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
      resource=https://api.egon.internal/local/permission/rbac3 \
    --data-urlencode "tenant_id=${service_tenant_id}" \
    --data-urlencode \
      'scope=service:authorization:decide service:authorization:snapshot service:identity:resolve' \
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
  oauth_service_token gateway-engine-service gateway-engine-local gateway-engine \
    "${secret_dir}/gateway-engine.service.jwt"
  oauth_service_token ddc-service ddc-local ddc \
    "${secret_dir}/ddc-admin.service.jwt"
  oauth_service_token mock-backend-service mock-backend-local mock-backend \
    "${secret_dir}/mock-backend.service.jwt"
  oauth_service_token mcp-provider-service mcp-provider-local mcp-provider \
    "${secret_dir}/mcp-provider.service.jwt"
  printf 'Bearer %s' "$(<"${secret_dir}/idp-admin.service.jwt")" \
    >"${secret_dir}/idp-rbac3.authorization"
  chmod 600 "${secret_dir}/idp-rbac3.authorization"
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
  write_env "${file}" DDC_RBAC3_SERVICE_CREDENTIAL_FILE "${secret_dir}/ddc-admin.service.jwt"
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
  write_env "${file}" IDP_RBAC3_AUTHORIZATION_HEADER_FILE "${secret_dir}/idp-rbac3.authorization"
  write_env "${file}" IDP_RBAC3_SERVICE_CREDENTIAL_FILE "${secret_dir}/idp-admin.service.jwt"
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
  write_env "${file}" RBAC3_ADMIN_SERVICE_CREDENTIAL_FILE "${secret_dir}/rbac3-admin.service.jwt"
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
  write_env "${file}" RBAC3_JWT_PRIVATE_KEY_FILE "${secret_dir}/rbac3-private.pem"
  write_env "${file}" RBAC3_JWT_PUBLIC_KEY_FILE "${secret_dir}/rbac3-public.pem"
  write_env "${file}" RBAC3_JWT_PUBLIC_KEY_LOCATION "file:${secret_dir}/rbac3-public.pem"
  write_env "${file}" RBAC3_JWT_ISSUER "${rbac3_url}"
  write_env "${file}" RBAC3_JWT_AUDIENCES rbac3-local
  write_env "${file}" RBAC3_AUDIT_CURSOR_SECRET_FILE "${secret_dir}/rbac3-audit.secret"
  write_env "${file}" RBAC3_SNOWFLAKE_MACHINE_ID 33
  write_env "${file}" RBAC3_DEVELOPMENT_BOOTSTRAP_ENABLED true
  write_env "${file}" RBAC3_DEVELOPMENT_AUTO_ACTIVATE_LOCAL_ADMIN_ROLES true
  write_env "${file}" RBAC3_DEVELOPMENT_TENANT_CODES default,tenant-b
  write_env "${file}" RBAC3_DEVELOPMENT_USERNAME alice
  write_env "${file}" SPRING_FLYWAY_ENABLED true

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
  write_env "${file}" GATEWAY_RBAC3_SERVICE_CREDENTIAL_FILE "${secret_dir}/gateway-admin.service.jwt"
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

  file="$(new_env_file mock-backend)"
  common_identity_env "${file}"
  write_tenant_aware_rbac3_service_token_env "${file}" \
    mock-backend-service mock-backend-local \
    "${secret_dir}/mock-backend-private.pem"
  write_env "${file}" MOCK_BACKEND_PORT 18160
  write_env "${file}" MOCK_BACKEND_REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
  write_env "${file}" MOCK_BACKEND_REDIS_DATABASE 8
  write_env "${file}" MOCK_BACKEND_RBAC3_SERVICE_CREDENTIAL_FILE "${secret_dir}/mock-backend.service.jwt"
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
  write_env "${file}" GATEWAY_MCP_RBAC3_SERVICE_CREDENTIAL_FILE \
    "${secret_dir}/gateway-engine.service.jwt"
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

oauth_login() {
  local cookie_jar="${runtime_dir}/browser.cookies" origin csrf status
  local csrf_headers login_headers
  origin=http://127.0.0.1:18121
  csrf_headers="$(mktemp "${runtime_dir}/login-csrf.XXXXXX")"
  login_headers="$(mktemp "${runtime_dir}/login.XXXXXX")"
  csrf="$(curl -fsS -D "${csrf_headers}" \
    -c "${cookie_jar}" -b "${cookie_jar}" -H "Origin: ${origin}" \
    "${idp_url}/oauth2/login/csrf" | jq -er '.token')"
  require_browser_cors "${csrf_headers}" "${origin}" "IdP login CSRF"
  status="$(curl -sS -D "${login_headers}" \
    -o "${runtime_dir}/login.response" -w '%{http_code}' \
    -c "${cookie_jar}" -b "${cookie_jar}" \
    -H "Origin: ${origin}" \
    -H 'Content-Type: application/json' -H "X-IDP-CSRF: ${csrf}" \
    -d "$(jq -cn --arg password "$(<"${secret_dir}/idp-admin.password")" \
      '{username:"alice",password:$password}')" \
    "${idp_url}/oauth2/login")"
  [[ "${status}" == "200" ]] || fail \
    "IdP login failed with HTTP ${status}: $(<"${runtime_dir}/login.response")"
  require_browser_cors "${login_headers}" "${origin}" "IdP password login"
  rm -f "${csrf_headers}" "${login_headers}"
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

require_browser_cors() {
  local headers="$1" origin="$2" context="$3"
  local allowed_origin allow_credentials
  allowed_origin="$(response_header "${headers}" Access-Control-Allow-Origin)"
  allow_credentials="$(response_header \
    "${headers}" Access-Control-Allow-Credentials)"
  [[ "${allowed_origin}" == "${origin}" ]] || fail \
    "${context} did not allow browser origin ${origin}"
  [[ "${allow_credentials}" == "true" ]] || fail \
    "${context} did not allow browser credentials for ${origin}"
}

oauth_token() {
  local client_id="$1" tenant="$2" output="$3" resource_override="${4:-}"
  local redirect_uri origin resource verifier challenge state headers code response status tenant_id
  case "${client_id}" in
    idp-admin-web)
      origin=http://127.0.0.1:18121
      redirect_uri=${origin}/oauth/callback
      resource=https://api.egon.internal/local/permission/idp
      ;;
    rbac3-admin-web)
      origin=http://127.0.0.1:18131
      redirect_uri=${origin}/oauth/callback
      resource=https://api.egon.internal/local/permission/rbac3
      ;;
    gateway-admin-web)
      origin=http://127.0.0.1:18141
      redirect_uri=${origin}/oauth/callback
      resource=https://api.egon.internal/local/platform/gateway-admin
      ;;
    ddc-admin-web)
      origin=http://127.0.0.1:18152
      redirect_uri=${origin}/oauth/callback
      resource=https://api.egon.internal/local/platform/ddc
      ;;
    mock-backend)
      origin=
      redirect_uri=http://127.0.0.1:18161/oauth/callback
      resource=https://api.egon.internal/local/identity/mock-backend
      ;;
    *) fail "unknown local OAuth client: ${client_id}" ;;
  esac
  if [[ -n "${resource_override}" ]]; then
    resource="${resource_override}"
  fi
  verifier="$(openssl rand -base64 48 | tr '+/' '-_' | tr -d '=\n')"
  challenge="$(printf '%s' "${verifier}" | openssl dgst -binary -sha256 | base64url)"
  state="$(openssl rand -hex 16)"
  tenant_id="$(rbac3_tenant_id "${tenant}")"
  headers="$(mktemp "${runtime_dir}/authorize.XXXXXX")"
  status="$(curl -sS -D "${headers}" -o "${runtime_dir}/authorize.response" \
    -w '%{http_code}' -c "${runtime_dir}/browser.cookies" \
    -b "${runtime_dir}/browser.cookies" -G "${idp_url}/oauth2/authorize" \
    --data-urlencode response_type=code --data-urlencode "client_id=${client_id}" \
    --data-urlencode "redirect_uri=${redirect_uri}" --data-urlencode "tenant_id=${tenant_id}" \
    --data-urlencode "resource=${resource}" \
    --data-urlencode "state=${state}" --data-urlencode "nonce=${state}" \
    --data-urlencode "code_challenge=${challenge}" \
    --data-urlencode code_challenge_method=S256)"
  [[ "${status}" == "302" ]] || fail \
    "IdP authorization failed with HTTP ${status}: $(<"${runtime_dir}/authorize.response")"
  code="$(sed -n 's/^[Ll]ocation:.*[?&]code=\([^&[:space:]]*\).*/\1/p' "${headers}" | tail -1)"
  rm -f "${headers}"
  [[ -n "${code}" ]] || fail "IdP did not return an authorization code for ${client_id}/${tenant}"
  headers="$(mktemp "${runtime_dir}/token.XXXXXX")"
  local -a token_arguments=(-D "${headers}")
  if [[ -n "${origin}" ]]; then
    token_arguments+=(-H "Origin: ${origin}")
  fi
  status="$(curl "${token_arguments[@]}" \
    -sS -o "${runtime_dir}/token.response" -w '%{http_code}' \
    -c "${runtime_dir}/browser.cookies" -b "${runtime_dir}/browser.cookies" -X POST \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode grant_type=authorization_code \
    --data-urlencode "client_id=${client_id}" --data-urlencode "code=${code}" \
    --data-urlencode "code_verifier=${verifier}" \
    --data-urlencode "resource=${resource}" \
    --data-urlencode "redirect_uri=${redirect_uri}" "${idp_url}/oauth2/token")"
  [[ "${status}" == "200" ]] || fail \
    "IdP token exchange failed with HTTP ${status}: $(<"${runtime_dir}/token.response")"
  if [[ -n "${origin}" ]]; then
    require_browser_cors \
      "${headers}" "${origin}" "IdP token exchange for ${client_id}"
  fi
  rm -f "${headers}"
  response="$(<"${runtime_dir}/token.response")"
  jq -er '.access_token' <<<"${response}" >"${output}"
  chmod 600 "${output}"
}

command_issue_user_token() {
  local client_id="${UNIFIED_IDENTITY_OAUTH_CLIENT_ID:-}"
  local tenant="${UNIFIED_IDENTITY_OAUTH_TENANT:-}"
  local resource="${UNIFIED_IDENTITY_OAUTH_RESOURCE_URI:-}"
  local output="${UNIFIED_IDENTITY_OAUTH_TOKEN_FILE:-}"
  process_running idp || fail "idp is not running; run start first"
  process_running rbac3 || fail "rbac3 is not running; run start first"
  [[ -n "${client_id}" ]] || fail \
    "UNIFIED_IDENTITY_OAUTH_CLIENT_ID is required"
  [[ -n "${tenant}" ]] || fail \
    "UNIFIED_IDENTITY_OAUTH_TENANT is required"
  [[ "${resource}" =~ ^https?:// ]] || fail \
    "UNIFIED_IDENTITY_OAUTH_RESOURCE_URI must be an absolute HTTP URI"
  [[ -n "${output}" ]] || fail \
    "UNIFIED_IDENTITY_OAUTH_TOKEN_FILE is required"
  [[ -s "${runtime_dir}/browser.cookies" ]] || oauth_login
  oauth_token "${client_id}" "${tenant}" "${output}" "${resource}"
}

activate_roles() {
  local token_file="$1" include_mock="$2" candidates current role_ids version request status
  status="$(curl -sS -o "${runtime_dir}/activation-candidates.response" \
    -w '%{http_code}' -H "Authorization: Bearer $(<"${token_file}")" \
    "${rbac3_url}/api/rbac3/v1/auth/role-activation-candidates")"
  [[ "${status}" == "200" ]] || fail \
    "RBAC3 activation candidates failed with HTTP ${status}: $(<"${runtime_dir}/activation-candidates.response")"
  candidates="$(<"${runtime_dir}/activation-candidates.response")"
  status="$(curl -sS -o "${runtime_dir}/role-activations.response" \
    -w '%{http_code}' -H "Authorization: Bearer $(<"${token_file}")" \
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
  version="$(jq -er '.data.sessionVersion' <<<"${current}")"
  request="$(jq -cn --argjson roles "${role_ids}" --argjson version "${version}" \
    '{roleIds:$roles,expectedContextVersion:$version}')"
  status="$(curl -sS -o "${runtime_dir}/role-activation-update.response" \
    -w '%{http_code}' -X PUT -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $(<"${token_file}")" -d "${request}" \
    "${rbac3_url}/api/rbac3/v1/auth/role-activations")"
  [[ "${status}" == "200" ]] || fail \
    "RBAC3 role activation failed with HTTP ${status}: $(<"${runtime_dir}/role-activation-update.response")"
}

gateway_api() {
  local method="$1" path="$2" body="${3:-}"
  local response_file status response
  local arguments=(-sS -X "${method}" \
    -H "Authorization: Bearer $(<"${secret_dir}/gateway-admin.access.jwt")" \
    -H 'Content-Type: application/json')
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
  local arguments=(-sS -X "${method}" \
    -H "Authorization: Bearer $(<"${secret_dir}/ddc-admin.access.jwt")" \
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
  local response biz_code biz_name app_code
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

initialize_gateway_control_plane() {
  local applications application app_id credential groups group group_id
  applications="$(gateway_api GET '/api/v1/gateway/admin/applications?bizCode=identity&namespace=default&env=local&appCode=mock-backend')"
  app_id="$(jq -r '.[0].id // empty' <<<"${applications}")"
  if [[ -z "${app_id}" ]]; then
    application="$(gateway_api POST /api/v1/gateway/admin/applications \
      '{"bizCode":"identity","applicationCode":"mock-backend","displayName":"Unified Identity Mock Backend","env":"local","namespace":"default","description":"Host-local unified identity verification"}')"
    app_id="$(jq -er '.id' <<<"${application}")"
  fi
  credential="$(gateway_api POST "/api/v1/gateway/admin/applications/${app_id}/credentials" '{}')"
  jq -er '.accessKey' <<<"${credential}" >"${secret_dir}/gateway-report.access-key"
  jq -er '.secret' <<<"${credential}" >"${secret_dir}/gateway-report.secret"
  printf '%s' "${app_id}" >"${runtime_dir}/gateway-application.id"

  groups="$(gateway_api GET '/api/v1/gateway/admin/gateway-groups?env=local&namespace=default')"
  group_id="$(jq -r '.[] | select(.gatewayGroupCode == "default") | .id' <<<"${groups}" | head -1)"
  if [[ -z "${group_id}" ]]; then
    group="$(gateway_api POST /api/v1/gateway/admin/gateway-groups \
      '{"gatewayGroupCode":"default","displayName":"Unified Identity Local Gateway","env":"local","namespace":"default","description":"Host-local unified identity verification"}')"
    group_id="$(jq -er '.id' <<<"${group}")"
  fi
  printf '%s' "${group_id}" >"${runtime_dir}/gateway-group.id"
  chmod 600 "${secret_dir}/gateway-report.access-key" \
    "${secret_dir}/gateway-report.secret" "${runtime_dir}/gateway-application.id" \
    "${runtime_dir}/gateway-group.id"
  write_env "${env_dir}/mock-backend.env" GATEWAY_REPORT_ACCESS_KEY \
    "$(<"${secret_dir}/gateway-report.access-key")"
  write_env "${env_dir}/mock-backend.env" GATEWAY_REPORT_SECRET_KEY \
    "$(<"${secret_dir}/gateway-report.secret")"
}

wait_gateway_catalog() {
  local app_id response
  app_id="$(<"${runtime_dir}/gateway-application.id")"
  for ((attempt = 1; attempt <= 60; attempt++)); do
    response="$(gateway_api GET "/api/v1/gateway/admin/applications/${app_id}/catalog" || true)"
    if jq -e '.. | objects | select(.methodIdentity? == "GET /api/mock/read" and .lifecycleStatus? == "ACTIVE")' \
        <<<"${response}" >/dev/null 2>&1; then
      return
    fi
    sleep 1
  done
  fail "mock backend Gateway catalog did not become active"
}

publish_gateway_routes() {
  local defer_release="${1:-false}"
  local app_id group_id catalog draft revision route operation_id response validation release
  app_id="$(<"${runtime_dir}/gateway-application.id")"
  group_id="$(<"${runtime_dir}/gateway-group.id")"
  catalog="$(gateway_api GET "/api/v1/gateway/admin/applications/${app_id}/catalog")"
  draft="$(gateway_api GET "/api/v1/gateway/admin/gateway-groups/${group_id}/draft")"
  revision="$(jq -er '.revision' <<<"${draft}")"
  if ! jq -e '
      .policies[]
      | select(
          .policyId == "identity-basic"
          and .policyType == "SECURITY"
          and .policyScope == "GLOBAL"
          and .enabled == true
          and .content.authenticationMode == "REQUIRED"
          and .content.credentialExtractorIds == ["idp-bearer"]
          and .content.authenticationProviderIds == ["idp-jwt"]
          and .content.authorizationProviderIds == []
          and .content.decisionMode == "ALL_ALLOW"
          and .content.identityMapperId == null
          and .content.providerTimeoutMs == 1000
          and .content.failureMode == "FAIL_CLOSED"
          and .content.credentialForwardingMode == "ORIGINAL_BEARER"
        )' <<<"${draft}" >/dev/null; then
    response="$(gateway_api PUT "/api/v1/gateway/admin/gateway-groups/${group_id}/draft/policies/identity-basic" \
      "$(jq -cn --argjson revision "${revision}" \
        '{policyType:"SECURITY",policyScope:"GLOBAL",content:{authenticationMode:"REQUIRED",credentialExtractorIds:["idp-bearer"],authenticationProviderIds:["idp-jwt"],authorizationProviderIds:[],decisionMode:"ALL_ALLOW",providerTimeoutMs:1000,failureMode:"FAIL_CLOSED",credentialForwardingMode:"ORIGINAL_BEARER"},enabled:true,expectedRevision:$revision,idempotencyKey:("unified-identity-security-" + ($revision | tostring)),changeReason:"Gateway validates identity and forwards the authenticated bearer for downstream authorization"}')")"
    revision="$(jq -er '.revision' <<<"${response}")"
  fi
  for route in read admin; do
    operation_id="$(jq -r --arg method "GET /api/mock/${route}" \
      '.. | objects | select(.methodIdentity? == $method) | .id' \
      <<<"${catalog}" | head -1)"
    [[ -n "${operation_id}" ]] || fail "missing reported mock ${route} operation"
    if jq -e --arg route "mock-${route}" --arg operation "${operation_id}" \
        '.routes[] | select(.routeId == $route and .operationId == $operation and .enabled == true and .content.host == "*" and .content.httpMethod == "GET" and .content.pathPattern == ("/api/mock/" + ($route | sub("^mock-"; ""))))' \
        <<<"${draft}" >/dev/null; then
      continue
    fi
    response="$(gateway_api PUT "/api/v1/gateway/admin/gateway-groups/${group_id}/draft/routes/mock-${route}" \
      "$(jq -cn --arg operation "${operation_id}" --arg route "${route}" \
        --argjson revision "${revision}" \
        '{operationId:$operation,content:{host:"*",httpMethod:"GET",pathPattern:("/api/mock/" + $route),accessZones:["PUBLIC"],priority:100},enabled:true,expectedRevision:$revision,idempotencyKey:("unified-identity-" + $route + "-" + ($revision | tostring)),changeReason:"Unified identity local route"}')")"
    revision="$(jq -er '.revision' <<<"${response}")"
  done
  if [[ "${defer_release}" == "true" ]]; then
    return
  fi
  validation="$(gateway_api POST "/api/v1/gateway/admin/gateway-groups/${group_id}/draft/validate" '{}')"
  jq -e '.valid == true' <<<"${validation}" >/dev/null \
    || fail "Gateway draft validation failed: ${validation}"
  release="$(gateway_api POST "/api/v1/gateway/admin/gateway-groups/${group_id}/releases" \
    "$(jq -cn --argjson revision "${revision}" \
      '{expectedDraftRevision:$revision,changeReason:"Unified identity local release"}')")"
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
  local idp_argument subject tenant_b_id
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

  stage "establishing IdP SSO session"
  oauth_login
  stage "issuing RBAC3 admin token"
  oauth_token rbac3-admin-web default "${secret_dir}/rbac3-default.access.jwt"
  stage "activating non-mock roles"
  activate_roles "${secret_dir}/rbac3-default.access.jwt" false
  stage "initializing DDC unified identity topology"
  oauth_token ddc-admin-web default "${secret_dir}/ddc-admin.access.jwt"
  initialize_ddc_topology

  stage "restarting IdP and RBAC3 with admitted DDC publication"
  stop_process rbac3
  stop_process idp
  start_process idp "${env_dir}/idp.env" "${idp_jar}"
  wait_http idp "${idp_url}/actuator/health/readiness"
  refresh_service_tokens
  start_process rbac3 "${env_dir}/rbac3.env" "${rbac3_jar}"
  wait_http rbac3 "${rbac3_url}/actuator/health/readiness"

  stage "restoring the local SSO and RBAC3 activation context"
  oauth_login
  oauth_token rbac3-admin-web default "${secret_dir}/rbac3-default.access.jwt"
  activate_roles "${secret_dir}/rbac3-default.access.jwt" false
  oauth_token ddc-admin-web default "${secret_dir}/ddc-admin.access.jwt"
  wait_ddc_provider_registration permission idp idp-admin
  wait_ddc_provider_registration permission rbac3 rbac3-admin
  stage "starting Gateway Engine DDC client"
  start_process gateway-engine "${env_dir}/gateway-engine.env" "${gateway_engine_jar}"
  wait_http gateway-engine http://127.0.0.1:18182/actuator/health/readiness
  stage "issuing Gateway admin token"
  oauth_token gateway-admin-web default "${secret_dir}/gateway-admin.access.jwt"

  stage "starting Gateway Admin"
  start_process gateway-admin "${env_dir}/gateway-admin.env" "${gateway_admin_jar}"
  wait_http gateway-admin "${gateway_admin_url}/actuator/health/readiness"
  initialize_gateway_control_plane

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
  process_running idp || fail "idp is not running; run start first"
  process_running rbac3 || fail "rbac3 is not running; run start first"
  service_tenant_id="$(rbac3_tenant_id default)"

  stage "refreshing IdP-owned service credentials"
  refresh_service_tokens
  stage "refreshing local OAuth SSO session"
  oauth_login
  oauth_token idp-admin-web default "${secret_dir}/idp-admin.access.jwt"
  oauth_token rbac3-admin-web default "${secret_dir}/rbac3-default.access.jwt"
  activate_roles "${secret_dir}/rbac3-default.access.jwt" true
  oauth_token gateway-admin-web default "${secret_dir}/gateway-admin.access.jwt"
  oauth_token ddc-admin-web default "${secret_dir}/ddc-admin.access.jwt"
  oauth_token mock-backend default "${secret_dir}/default.access.jwt"

  oauth_token rbac3-admin-web tenant-b \
    "${secret_dir}/rbac3-tenant-b.access.jwt"
  activate_roles "${secret_dir}/rbac3-tenant-b.access.jwt" true
  oauth_token mock-backend tenant-b "${secret_dir}/tenant-b.access.jwt"
  stage "local OAuth tokens and authorization snapshots are current"
}

http_status() {
  local url="$1" token_file="$2"
  curl -sS -o /dev/null -w '%{http_code}' \
    -H "Authorization: Bearer $(<"${token_file}")" "${url}"
}

refresh_replay_check() {
  local old_cookie="${runtime_dir}/refresh-replay.cookies"
  local first_response="${runtime_dir}/refresh-first.response"
  local first_status replay_status
  cp "${runtime_dir}/browser.cookies" "${old_cookie}"
  first_status="$(curl -sS -o "${first_response}" -w '%{http_code}' \
    -c "${runtime_dir}/browser.cookies" \
    -b "${runtime_dir}/browser.cookies" -X POST \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode grant_type=refresh_token --data-urlencode client_id=mock-backend \
    --data-urlencode \
      resource=https://api.egon.internal/local/identity/mock-backend \
    "${idp_url}/oauth2/token")"
  [[ "${first_status}" == "200" ]] || fail \
    "initial refresh failed with HTTP ${first_status}: $(<"${first_response}")"
  jq -er '.access_token' "${first_response}" >"${secret_dir}/pre-replay.access.jwt"
  replay_status="$(curl -sS -o "${runtime_dir}/refresh-replay.response" -w '%{http_code}' \
    -b "${old_cookie}" -X POST -H 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode grant_type=refresh_token --data-urlencode client_id=mock-backend \
    --data-urlencode \
      resource=https://api.egon.internal/local/identity/mock-backend \
    "${idp_url}/oauth2/token")"
  [[ "${replay_status}" == "400" ]] || fail "refresh replay was not rejected"
  cp "${secret_dir}/pre-replay.access.jwt" "${secret_dir}/revoked.access.jwt"
  for ((attempt = 1; attempt <= 20; attempt++)); do
    if [[ "$(http_status "${gateway_url}/api/mock/read" "${secret_dir}/revoked.access.jwt")" == "401" ]]; then
      return
    fi
    sleep 1
  done
  fail "refresh replay did not revoke the old access token at Gateway"
}

disable_identity_check() {
  local subject record display_name version request status
  subject="$(identity_subject)"
  [[ "${subject}" =~ ^[A-Za-z0-9._~-]{1,64}$ ]] \
    || fail "IdP bootstrap subject is unsafe"
  record="$(psql_command "${idp_database}" -AtF $'\t' -c \
    "select display_name, version from identity_user where id = '${subject}'")"
  IFS=$'\t' read -r display_name version <<<"${record}"
  [[ -n "${display_name}" && "${version}" =~ ^[0-9]+$ ]] \
    || fail "IdP bootstrap identity cannot be loaded"
  oauth_token idp-admin-web default "${secret_dir}/idp-admin.access.jwt"
  request="$(jq -cn --arg displayName "${display_name}" \
    --argjson expectedVersion "${version}" \
    '{displayName:$displayName,status:"DISABLED",expectedVersion:$expectedVersion}')"
  status="$(curl -sS -o "${runtime_dir}/identity-disable.response" \
    -w '%{http_code}' -X PATCH -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $(<"${secret_dir}/idp-admin.access.jwt")" \
    -d "${request}" "${idp_url}/api/v1/identity/users/${subject}")"
  [[ "${status}" == "200" ]] || fail \
    "IdP identity disable failed with HTTP ${status}: $(<"${runtime_dir}/identity-disable.response")"
  for ((attempt = 1; attempt <= 20; attempt++)); do
    if [[ "$(http_status "${gateway_url}/api/mock/read" \
        "${secret_dir}/tenant-b.access.jwt")" == "401" ]]; then
      break
    fi
    sleep 1
  done
  [[ "$(http_status "${gateway_url}/api/mock/read" \
      "${secret_dir}/tenant-b.access.jwt")" == "401" ]] \
    || fail "disabled identity access token was not rejected at Gateway"

  psql_command "${idp_database}" -c \
    "update identity_user set status = 'ACTIVE', version = version + 1, updated_at = now() where id = '${subject}' and status = 'DISABLED'" \
    >/dev/null
  [[ "$(psql_command "${idp_database}" -Atqc \
      "select status from identity_user where id = '${subject}'")" == "ACTIVE" ]] \
    || fail "development identity could not be restored after disable check"
  stop_process idp
  start_process idp "${env_dir}/idp.env" "${idp_jar}"
  wait_http idp "${idp_url}/actuator/health/readiness"
}

command_verify() {
  local status
  for name in ddc idp rbac3 gateway-admin mock-backend gateway-engine; do
    process_running "${name}" || fail "${name} is not running; run start first"
  done
  oauth_login
  oauth_token rbac3-admin-web default \
    "${secret_dir}/rbac3-default.access.jwt"
  activate_roles "${secret_dir}/rbac3-default.access.jwt" false
  oauth_token mock-backend default "${secret_dir}/mock-before-activation.access.jwt"
  status="$(http_status "${gateway_url}/api/mock/admin" \
    "${secret_dir}/mock-before-activation.access.jwt")"
  [[ "${status}" == "403" ]] || fail \
    "downstream permission denial must be 403 before mock role activation; got ${status}"
  activate_roles "${secret_dir}/rbac3-default.access.jwt" true
  for ((attempt = 1; attempt <= 20; attempt++)); do
    if [[ "$(http_status "${gateway_url}/api/mock/admin" "${secret_dir}/mock-before-activation.access.jwt")" == "200" ]]; then
      break
    fi
    sleep 1
  done
  [[ "$(http_status "${gateway_url}/api/mock/admin" "${secret_dir}/mock-before-activation.access.jwt")" == "200" ]] \
    || fail "role activation did not authorize the unchanged access token"

  oauth_token rbac3-admin-web tenant-b "${secret_dir}/rbac3-tenant-b.access.jwt"
  activate_roles "${secret_dir}/rbac3-tenant-b.access.jwt" true
  oauth_token mock-backend tenant-b "${secret_dir}/tenant-b.access.jwt"
  [[ "$(http_status "${gateway_url}/api/mock/read" "${secret_dir}/tenant-b.access.jwt")" == "200" ]] \
    || fail "tenant-b token did not reach the backend"
  [[ "$(jq -Rr 'split(".")[1] | @base64d | fromjson | .sid' \
      <"${secret_dir}/mock-before-activation.access.jwt")" == \
      "$(jq -Rr 'split(".")[1] | @base64d | fromjson | .sid' \
      <"${secret_dir}/tenant-b.access.jwt")" ]] \
    || fail "tenant switch changed the stable SSO session"

  disable_identity_check
  oauth_token mock-backend default "${secret_dir}/default.access.jwt"
  refresh_replay_check
  oauth_token mock-backend default "${secret_dir}/default.access.jwt"
  oauth_token mock-backend tenant-b "${secret_dir}/tenant-b.access.jwt"
  [[ "$(http_status "${gateway_url}/api/mock/read" "${secret_dir}/default.access.jwt")" == "200" ]] \
    || fail "fresh access token failed after replay revocation"
  [[ "$(http_status "${gateway_url}/api/mock/read" "${secret_dir}/tenant-b.access.jwt")" == "200" ]] \
    || fail "fresh tenant-b token failed after replay revocation"

  UNIFIED_IDENTITY_LIVE=true \
  UNIFIED_IDENTITY_GATEWAY_URL="${gateway_url}" \
  UNIFIED_IDENTITY_MOCK_URL="${mock_url}" \
  UNIFIED_IDENTITY_DEFAULT_TOKEN_FILE="${secret_dir}/default.access.jwt" \
  UNIFIED_IDENTITY_TENANT_B_TOKEN_FILE="${secret_dir}/tenant-b.access.jwt" \
  UNIFIED_IDENTITY_REVOKED_TOKEN_FILE="${secret_dir}/revoked.access.jwt" \
    "${repo_root}/mvnw" -B -ntp -f "${repo_root}/pom.xml" \
      -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite \
      -am -Dtest=UnifiedIdentityTopologyIT,UnifiedIdentityRevocationIT,UnifiedIdentityTenantSwitchIT \
      -Dsurefire.failIfNoSpecifiedTests=false test
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
  refresh-tokens) command_refresh_tokens ;;
  issue-user-token) command_issue_user_token ;;
  verify) command_verify ;;
  status) command_status ;;
  stop) command_stop ;;
  *) usage >&2; exit 2 ;;
esac
