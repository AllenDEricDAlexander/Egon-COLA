#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"
runtime_dir="${UNIFIED_IDENTITY_RUNTIME_DIR:-${repo_root}/.runtime/unified-identity}"
secret_dir="${runtime_dir}/secrets"
log_dir="${runtime_dir}/logs"
pid_dir="${runtime_dir}/pids"
env_dir="${runtime_dir}/env"

idp_url="${UNIFIED_IDENTITY_TIANQUAN_SHOUBING_URL:-http://127.0.0.1:18120}"
idp_rpc_target="${UNIFIED_IDENTITY_TIANQUAN_SHOUBING_RPC_TARGET:-dns:///127.0.0.1:18122}"
rbac3_url="${UNIFIED_IDENTITY_TIANQUAN_JIANSHEN_URL:-http://127.0.0.1:18130}"
gateway_admin_url="${UNIFIED_IDENTITY_YUHENG_ADMIN_URL:-http://127.0.0.1:18140}"
ddc_url="${UNIFIED_IDENTITY_TIANSHU_URL:-http://127.0.0.1:18150}"
ddc_rpc_target="${UNIFIED_IDENTITY_TIANSHU_RPC_TARGET:-dns:///127.0.0.1:19080}"
mock_url="${UNIFIED_IDENTITY_MOCK_URL:-http://127.0.0.1:18160}"
gateway_url="${UNIFIED_IDENTITY_YUHENG_URL:-http://127.0.0.1:18180}"
advertised_host="${UNIFIED_IDENTITY_ADVERTISED_HOST:-127.0.0.1}"
declared_hosts="127.0.0.1"
if [[ "${advertised_host}" != "127.0.0.1" && "${advertised_host}" != "localhost" ]]; then
  declared_hosts+=",${advertised_host}"
fi
startup_mode="${UNIFIED_IDENTITY_START_MODE:-xingyuan}"

postgres_host="${UNIFIED_IDENTITY_POSTGRES_HOST:-127.0.0.1}"
postgres_port="${UNIFIED_IDENTITY_POSTGRES_PORT:-5432}"
postgres_user="${UNIFIED_IDENTITY_POSTGRES_USER:-postgres}"
postgres_database="${UNIFIED_IDENTITY_POSTGRES_MAINTENANCE_DATABASE:-postgres}"
postgres_password_file="${UNIFIED_IDENTITY_POSTGRES_PASSWORD_FILE:-}"

redis_host="${UNIFIED_IDENTITY_REDIS_HOST:-127.0.0.1}"
redis_port="${UNIFIED_IDENTITY_REDIS_PORT:-6379}"
redis_config_file="${UNIFIED_IDENTITY_REDIS_CONFIG_FILE:-/opt/homebrew/etc/redis.conf}"
redis_password_source="${UNIFIED_IDENTITY_REDIS_PASSWORD_FILE:-}"

idp_database="${UNIFIED_IDENTITY_TIANQUAN_SHOUBING_DATABASE:-egon_identity_local}"
rbac3_database="${UNIFIED_IDENTITY_TIANQUAN_JIANSHEN_DATABASE:-egon_rbac3_unified_identity_local}"
gateway_database="${UNIFIED_IDENTITY_YUHENG_DATABASE:-egon_gateway_local}"
ddc_database="${UNIFIED_IDENTITY_TIANSHU_DATABASE:-egon_tianshu_local}"
service_tenant_id="${UNIFIED_IDENTITY_SERVICE_TENANT_ID:-default}"
oauth_registry_file="${UNIFIED_IDENTITY_OAUTH_REGISTRY_FILE:-}"
oauth_registry_json=""
platform_partition_id=""
platform_issuer=""
tenant_b_id=""
# USER authorization comes only from protected operator-provided formal input files.
ddc_admin_access_token=""

idp_jar="${repo_root}/egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/target/egon-cola-tianquan-shoubing-admin-exec.jar"
rbac3_jar="${repo_root}/egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/target/egon-cola-tianquan-jianshen-admin-exec.jar"
gateway_admin_jar="${repo_root}/egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/target/yuheng-admin-exec.jar"
gateway_engine_jar="${repo_root}/egon-cola-xingyuan/egon-cola-yuheng/yuheng-biz-gateway/target/yuheng-biz-gateway-exec.jar"
gateway_mcp_engine_jar="${repo_root}/egon-cola-xingyuan/egon-cola-yuheng/yuheng-mcp-gateway/target/yuheng-mcp-gateway-exec.jar"
ddc_jar="${repo_root}/egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/target/egon-cola-tianshu-admin-exec.jar"
mock_jar="${repo_root}/egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-tianquan-shoubing-backend/target/yuheng-test-tianquan-shoubing-backend-exec.jar"

usage() {
  cat <<'USAGE'
Usage: ./scripts/unified-identity-local.sh <command>

Commands:
  prepare  Check host dependencies, create named databases/secrets, and package jars
  start    Start and bootstrap Tianshu, Tianquan-Shoubing, Tianquan-Jianshen, Yuheng, and the mock backend
  publish-yuheng-routes  Publish the prepared local Yuheng routes after Engine startup
  refresh-yuheng-admin-catalog  Refresh the running Admin's own interface catalog
  sync-local-credentials  Refresh SERVICE credentials and verify protected formal USER inputs
  issue-user-token  Copy one verified operator-provided formal USER input to a protected output
  oauth-context  Print the verified operator tenant/subject/issuer context without token data
  write-service-registration  Append one explicit registered client to a managed env/properties file
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

protected_oauth_file() {
  local file="$1" maximum="${2:-131072}" metadata owner mode bytes
  [[ "${file}" == /* && -f "${file}" && -s "${file}" && ! -L "${file}" ]] \
    || fail "OAuth material must be an existing absolute regular owner-only file"
  if [[ "$(uname -s)" == Darwin ]]; then
    metadata="$(stat -f '%u:%Lp:%z' "${file}")"
  else
    metadata="$(stat -c '%u:%a:%s' "${file}")"
  fi
  IFS=: read -r owner mode bytes <<<"${metadata}"
  [[ "${owner}" == "$(id -u)" && ( "${mode}" == 400 || "${mode}" == 600 ) \
    && "${bytes}" =~ ^[0-9]+$ && "${bytes}" -le "${maximum}" ]] \
    || fail "OAuth material has unsafe ownership, permissions, or size"
}

require_oauth_decimal() {
  local value="$1" allow_zero="${2:-false}"
  [[ "${value}" =~ ^(0|[1-9][0-9]{0,18})$ \
    && ( "${#value}" -lt 19 || "${value}" < 9223372036854775808 ) \
    && ( "${allow_zero}" == true || "${value}" != 0 ) ]] \
    || fail "OAuth registry contains an invalid decimal Long"
}

require_oauth_origin() {
  local origin="$1"
  [[ "${origin}" =~ ^https://([A-Za-z0-9][A-Za-z0-9.-]*|\[[0-9A-Fa-f:]+\])(:[0-9]{1,5})?$ \
    || "${origin}" =~ ^http://(127\.0\.0\.1|localhost|\[::1\])(:[0-9]{1,5})?$ ]] \
    || fail "OAuth public origin must be canonical HTTPS or exact loopback HTTP"
  [[ "${origin}" != *..* && "${#origin}" -le 2048 ]] \
    || fail "OAuth public origin is invalid"
  if [[ "${origin}" =~ :([0-9]{1,5})$ ]]; then
    local port=$((10#${BASH_REMATCH[1]}))
    [[ "${port}" -ge 1 && "${port}" -le 65535 ]] || fail "OAuth public-origin port is invalid"
  fi
}

local_oauth_registration() {
  local client_id="$1" tenant_id="${2:-PLATFORM}"
  [[ -n "${oauth_registry_json:-}" ]] || load_local_oauth_registry
  jq -ce --arg client "${client_id}" --arg tenant "${tenant_id}" '
    .clients[$client] | if $tenant == "PLATFORM" then .platform else .tenants[$tenant] end
    | select(type == "object")' <<<"${oauth_registry_json}" \
    || fail "OAuth client has no explicit registration for the requested realm"
}

local_oauth_issuer() {
  local ref="$1"
  jq -er --arg ref "${ref}" '.issuers[$ref].issuer' <<<"${oauth_registry_json}" \
    || fail "OAuth issuer reference is missing"
}

load_local_oauth_registry() {
  [[ -n "${oauth_registry_json:-}" ]] && return
  [[ -n "${oauth_registry_file:-}" ]] \
    || fail "UNIFIED_IDENTITY_OAUTH_REGISTRY_FILE is required; provision/import the current OAuth facts first"
  protected_oauth_file "${oauth_registry_file}"
  oauth_registry_json="$(<"${oauth_registry_file}")"
  jq -e '
    def text: type == "string" and length > 0 and length <= 4096;
    def decimal: type == "string" and test("^(0|[1-9][0-9]{0,18})$");
    def positive: decimal and . != "0";
    def registration:
      type == "object" and (keys | sort) == ["credentialVersion","issuerRef","secretFile"]
      and (.issuerRef | text) and (.credentialVersion | decimal) and (.secretFile | text);
    type == "object"
    and (keys | sort) == ["auditHmacKeyFile","clients","identitySub","issuers","kekFile","kekId",
      "platformManagementTenantId","platformStoragePartitionId","publicOrigin","revocationEpoch","schemaVersion","tenants"]
    and .schemaVersion == 1 and (.publicOrigin | text)
    and (.platformStoragePartitionId | positive) and (.platformManagementTenantId | positive)
    and (.identitySub | type == "string" and test("^[A-Za-z0-9._~-]{1,64}$"))
    and (.revocationEpoch | type == "string" and test("^[A-Za-z0-9._-]{1,128}$"))
    and (.kekId | type == "string" and test("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$"))
    and (.kekFile | text) and (.auditHmacKeyFile | text)
    and (.issuers | type == "object" and length >= 1 and length <= 16)
    and all(.issuers | to_entries[];
      (.key | test("^[a-z][a-z0-9-]{0,31}$")) and (.value | type == "object")
      and ((.value | keys | sort) == ["issuer","realmKey","realmType","trustVersion"]
        or (.value | keys | sort) == ["issuer","realmKey","realmType","tenantId","trustVersion"])
      and (.value.issuer | text) and (.value.trustVersion | decimal)
      and (.value.realmKey | type == "string" and test("^(platform|tenant:[1-9][0-9]{0,18})$"))
      and ((.value.realmType == "PLATFORM" and .value.tenantId == null)
        or (.value.realmType == "TENANT" and (.value.tenantId | positive))))
    and ([.issuers[].issuer] | length == (unique | length))
    and ([.issuers[] | select(.realmType == "PLATFORM")] | length == 1)
    and (.tenants | type == "object" and length >= 2 and length <= 8 and has("default") and has("tenant-b"))
    and all(.tenants | to_entries[];
      (.key | test("^[a-z][a-z0-9-]{2,63}$")) and (.value | type == "object")
      and ((.value | keys | sort) == ["accessTokenFile","issuerRef","tenantId"]
        or (.value | keys | sort) == ["accessTokenFile","issuerRef","revokedAccessTokenFile","tenantId"])
      and (.value.tenantId | positive) and (.value.issuerRef | text) and (.value.accessTokenFile | text)
      and (.value.revokedAccessTokenFile == null or (.value.revokedAccessTokenFile | text)))
    and ([.tenants[].tenantId] | length == (unique | length))
    and (.clients | type == "object" and length >= 1 and length <= 32)
    and all(.clients | to_entries[];
      (.key | test("^[a-z][a-z0-9-]{2,127}$")) and (.value | type == "object")
      and (.value | (keys | sort) == ["appId","platform","tenants"])
      and (.value.appId | type == "string" and test("^[a-z][a-z0-9-]{2,127}$"))
      and (.value.platform | registration)
      and (.value.tenants | type == "object" and length >= 2 and length <= 8)
      and all(.value.tenants | to_entries[]; (.key | positive) and (.value | registration)))
  ' <<<"${oauth_registry_json}" >/dev/null \
    || fail "OAuth registry schema is invalid or contains unknown fields"
  require_oauth_origin "$(jq -er '.publicOrigin' <<<"${oauth_registry_json}")"
  [[ "$(jq -er '.publicOrigin' <<<"${oauth_registry_json}")" == "${idp_url}" ]] \
    || fail "OAuth registry publicOrigin must equal the explicit local IdP origin"
  platform_partition_id="$(jq -er '.platformStoragePartitionId' <<<"${oauth_registry_json}")"
  require_oauth_decimal "${platform_partition_id}"
  require_oauth_decimal "$(jq -er '.platformManagementTenantId' <<<"${oauth_registry_json}")"
  protected_oauth_file "$(jq -er '.kekFile' <<<"${oauth_registry_json}")" 1024
  protected_oauth_file "$(jq -er '.auditHmacKeyFile' <<<"${oauth_registry_json}")" 1024
  local ref issuer type tenant_id realm_key version expected registration client_id tenant_code secret_file secret
  while IFS= read -r ref; do
    issuer="$(local_oauth_issuer "${ref}")"
    type="$(jq -er --arg ref "${ref}" '.issuers[$ref].realmType' <<<"${oauth_registry_json}")"
    realm_key="$(jq -er --arg ref "${ref}" '.issuers[$ref].realmKey' <<<"${oauth_registry_json}")"
    version="$(jq -er --arg ref "${ref}" '.issuers[$ref].trustVersion' <<<"${oauth_registry_json}")"
    require_oauth_decimal "${version}" true
    if [[ "${type}" == PLATFORM ]]; then
      expected="${idp_url}/platform"
      [[ "${realm_key}" == platform ]] || fail "PLATFORM registry realmKey is invalid"
      platform_issuer="${issuer}"
    else
      tenant_id="$(jq -er --arg ref "${ref}" '.issuers[$ref].tenantId' <<<"${oauth_registry_json}")"
      require_oauth_decimal "${tenant_id}"
      [[ "${tenant_id}" != "${platform_partition_id}" && "${realm_key}" == "tenant:${tenant_id}" ]] \
        || fail "TENANT registry partition or realmKey is invalid"
      expected="${idp_url}/tenants/${tenant_id}"
    fi
    [[ "${issuer}" == "${expected}" ]] || fail "OAuth issuer must equal its canonical configured realm URI"
  done < <(jq -r '.issuers | keys[]' <<<"${oauth_registry_json}")
  while IFS= read -r tenant_code; do
    tenant_id="$(jq -er --arg tenant "${tenant_code}" '.tenants[$tenant].tenantId' <<<"${oauth_registry_json}")"
    ref="$(jq -er --arg tenant "${tenant_code}" '.tenants[$tenant].issuerRef' <<<"${oauth_registry_json}")"
    require_oauth_decimal "${tenant_id}"
    jq -e --arg ref "${ref}" --arg tenant "${tenant_id}" \
      '.issuers[$ref].realmType == "TENANT" and .issuers[$ref].tenantId == $tenant' \
      <<<"${oauth_registry_json}" >/dev/null || fail "Local tenant does not match its trusted issuer"
    protected_oauth_file "$(jq -er --arg tenant "${tenant_code}" '.tenants[$tenant].accessTokenFile' <<<"${oauth_registry_json}")" 16384
    local_user_input_metadata "${tenant_code}" >/dev/null
  done < <(jq -r '.tenants | keys[]' <<<"${oauth_registry_json}")
  for client_id in tianquan-shoubing-service tianquan-jianshen-service yuheng-admin-service \
      yuheng-biz-gateway-service yuheng-mcp-gateway-service tianshu-service mock-backend-service mcp-provider-service; do
    jq -e --arg client "${client_id}" '.clients | has($client)' <<<"${oauth_registry_json}" >/dev/null \
      || fail "A required local SERVICE client is absent from the explicit registry"
  done
  while IFS= read -r client_id; do
    jq -e --arg client "${client_id}" \
      '(.clients[$client].tenants | keys | sort) == ([.tenants[].tenantId] | sort)' \
      <<<"${oauth_registry_json}" >/dev/null || fail "Local client must explicitly register every configured tenant"
    while IFS= read -r tenant_id; do
      registration="$(local_oauth_registration "${client_id}" "${tenant_id}")"
      ref="$(jq -er '.issuerRef' <<<"${registration}")"
      version="$(jq -er '.credentialVersion' <<<"${registration}")"
      require_oauth_decimal "${version}" true
      if [[ "${tenant_id}" == PLATFORM ]]; then
        jq -e --arg ref "${ref}" '.issuers[$ref].realmType == "PLATFORM"' <<<"${oauth_registry_json}" >/dev/null \
          || fail "Local PLATFORM client points to a tenant issuer"
      else
        jq -e --arg ref "${ref}" --arg tenant "${tenant_id}" \
          '.issuers[$ref].realmType == "TENANT" and .issuers[$ref].tenantId == $tenant' \
          <<<"${oauth_registry_json}" >/dev/null || fail "Local SERVICE client issuer is outside its configured tenant"
      fi
      secret_file="$(jq -er '.secretFile' <<<"${registration}")"
      protected_oauth_file "${secret_file}" 512
      secret="$(<"${secret_file}")"
      [[ "${#secret}" -ge 32 && "${#secret}" -le 256 && "${secret}" =~ ^[-A-Za-z0-9_]+$ ]] || fail "Local SERVICE secret has an invalid one-time format"
    done < <(jq -r --arg client "${client_id}" \
      '"PLATFORM", (.clients[$client].tenants | keys[])' <<<"${oauth_registry_json}")
  done < <(jq -r '.clients | keys[]' <<<"${oauth_registry_json}")
  service_tenant_id="$(jq -er '.tenants.default.tenantId' <<<"${oauth_registry_json}")"
  tenant_b_id="$(jq -er '.tenants["tenant-b"].tenantId' <<<"${oauth_registry_json}")"
}

write_oauth_property() {
  local file="$1" key="$2" value="$3"
  printf '%s=%s\n' "${key}" "$(properties_escape "${value}")" >>"${file%.env}.properties"
}

write_local_oauth_trust_env() {
  local file="$1" ref issuer prefix type tenant_id
  [[ -n "${oauth_registry_json:-}" ]] || load_local_oauth_registry
  write_env "${file}" SPRING_CONFIG_ADDITIONAL_LOCATION "file:${file%.env}.properties"
  write_env "${file}" TIANQUAN_SHOUBING_OAUTH_ISSUER "${platform_issuer}"
  write_env "${file}" TIANQUAN_SHOUBING_JWK_SET_URI "${platform_issuer}/.well-known/jwks.json"
  while IFS= read -r ref; do
    issuer="$(local_oauth_issuer "${ref}")"
    prefix="egon.cola.platform.tianquan.shoubing.trusted-issuers.[${ref}]"
    type="$(jq -er --arg ref "${ref}" '.issuers[$ref].realmType' <<<"${oauth_registry_json}")"
    write_oauth_property "${file}" "${prefix}.issuer" "${issuer}"
    write_oauth_property "${file}" "${prefix}.jwks-uri" "${issuer}/.well-known/jwks.json"
    write_oauth_property "${file}" "${prefix}.realm-key" \
      "$(jq -er --arg ref "${ref}" '.issuers[$ref].realmKey' <<<"${oauth_registry_json}")"
    write_oauth_property "${file}" "${prefix}.realm-type" "${type}"
    write_oauth_property "${file}" "${prefix}.trust-version" \
      "$(jq -er --arg ref "${ref}" '.issuers[$ref].trustVersion' <<<"${oauth_registry_json}")"
    if [[ "${type}" == TENANT ]]; then
      tenant_id="$(jq -er --arg ref "${ref}" '.issuers[$ref].tenantId' <<<"${oauth_registry_json}")"
      write_oauth_property "${file}" "${prefix}.tenant-id" "${tenant_id}"
    fi
  done < <(jq -r '.issuers | keys[]' <<<"${oauth_registry_json}")
  write_oauth_property "${file}" egon.cola.platform.tianquan.shoubing.revocation.primary-endpoint \
    "redis://${redis_host}:${redis_port}"
  write_oauth_property "${file}" egon.cola.platform.tianquan.shoubing.revocation.epoch \
    "$(jq -er '.revocationEpoch' <<<"${oauth_registry_json}")"
  write_oauth_property "${file}" egon.cola.platform.tianquan.shoubing.revocation.mode STRICT_SHARED
}

write_local_oauth_registration_env() {
  local file="$1" client_id="$2" tenant_id registration ref issuer secret_file registration_id prefix
  write_local_oauth_trust_env "${file}"
  while IFS= read -r tenant_id; do
    registration="$(local_oauth_registration "${client_id}" "${tenant_id}")"
    ref="$(jq -er '.issuerRef' <<<"${registration}")"
    issuer="$(local_oauth_issuer "${ref}")"
    secret_file="$(jq -er '.secretFile' <<<"${registration}")"
    registration_id=egon-tianquan-shoubing
    if [[ "${tenant_id}" != PLATFORM ]]; then
      registration_id="egon-tianquan-shoubing-tenant-${tenant_id}"
      write_oauth_property "${file}" \
        "egon.cola.platform.tianquan.shoubing.service-client.tenant-registration-ids.[${tenant_id}]" \
        "${registration_id}"
    fi
    prefix="spring.security.oauth2.client.registration.${registration_id}"
    write_oauth_property "${file}" "${prefix}.client-id" "${client_id}"
    write_oauth_property "${file}" "${prefix}.client-secret" "$(<"${secret_file}")"
    write_oauth_property "${file}" "${prefix}.authorization-grant-type" client_credentials
    write_oauth_property "${file}" "${prefix}.client-authentication-method" client_secret_basic
    write_oauth_property "${file}" "${prefix}.provider" "${registration_id}"
    prefix="spring.security.oauth2.client.provider.${registration_id}"
    write_oauth_property "${file}" "${prefix}.issuer-uri" "${issuer}"
    write_oauth_property "${file}" "${prefix}.token-uri" "${issuer}/oauth2/token"
    write_oauth_property "${file}" "${prefix}.jwk-set-uri" "${issuer}/.well-known/jwks.json"
    write_oauth_property "${file}" \
      "egon.cola.platform.tianquan.shoubing.service-client.credential-versions.[${registration_id}]" \
      "$(jq -er '.credentialVersion' <<<"${registration}")"
  done < <(jq -r --arg client "${client_id}" \
    '"PLATFORM", (.clients[$client].tenants | keys[])' <<<"${oauth_registry_json}")
  write_oauth_property "${file}" egon.cola.platform.tianquan.shoubing.service-client.app-id \
    "$(jq -er --arg client "${client_id}" '.clients[$client].appId' <<<"${oauth_registry_json}")"
  write_oauth_property "${file}" egon.cola.platform.tianquan.shoubing.service-client.registration-id egon-tianquan-shoubing
}

validate_current_oauth_directory() {
  local subject ref issuer type partition version count client_id tenant_id registration app_id
  subject="$(jq -er '.identitySub' <<<"${oauth_registry_json}")"
  count="$(psql_command "${idp_database}" -Atqc \
    "select count(*) from tianquan_oauth.oauth_identity_user where tenant_id = ${platform_partition_id}
      and identity_sub = '${subject}' and status = 'ACTIVE' and deleted_at is null")"
  [[ "${count}" == 1 ]] || fail "Current MP operator identity is missing or inactive; complete explicit import/provisioning first"
  while IFS= read -r ref; do
    issuer="$(local_oauth_issuer "${ref}")"
    type="$(jq -er --arg ref "${ref}" '.issuers[$ref].realmType' <<<"${oauth_registry_json}")"
    partition="${platform_partition_id}"
    if [[ "${type}" == TENANT ]]; then
      partition="$(jq -er --arg ref "${ref}" '.issuers[$ref].tenantId' <<<"${oauth_registry_json}")"
    fi
    version="$(jq -er --arg ref "${ref}" '.issuers[$ref].trustVersion' <<<"${oauth_registry_json}")"
    count="$(psql_command "${idp_database}" -Atqc \
      "select count(*) from tianquan_oauth.oauth_realm r join tianquan_oauth.oauth_signing_key k
        on k.tenant_id = r.tenant_id and k.kid = r.active_kid and k.status = 'ACTIVE' and k.deleted_at is null
        where r.tenant_id = ${partition} and r.issuer = '${issuer}' and r.realm_type = '${type}'
          and r.trust_version = ${version} and r.status = 'ACTIVE' and r.deleted_at is null")"
    [[ "${count}" == 1 ]] || fail "Current OAuth realm/key is not ready or differs from the approved registry"
  done < <(jq -r '.issuers | keys[]' <<<"${oauth_registry_json}")
  while IFS= read -r tenant_id; do
    count="$(psql_command "${idp_database}" -Atqc \
      "select count(*) from tianquan_oauth.oauth_identity_tenant t join tianquan_oauth.oauth_identity_membership m
        on m.tenant_id = t.tenant_id and m.identity_tenant_id = t.identity_tenant_id
          and m.identity_sub = '${subject}' and m.status = 'ACTIVE' and m.deleted_at is null
        where t.tenant_id = ${platform_partition_id} and t.identity_tenant_id = '${tenant_id}'
          and t.status = 'ACTIVE' and t.deleted_at is null")"
    [[ "${count}" == 1 ]] || fail "Current MP tenant/membership is missing or inactive"
  done < <(jq -r '.tenants[].tenantId' <<<"${oauth_registry_json}")
  while IFS= read -r client_id; do
    app_id="$(jq -er --arg client "${client_id}" '.clients[$client].appId' <<<"${oauth_registry_json}")"
    while IFS= read -r tenant_id; do
      registration="$(local_oauth_registration "${client_id}" "${tenant_id}")"
      partition="${tenant_id}"
      [[ "${tenant_id}" == PLATFORM ]] && partition="${platform_partition_id}"
      version="$(jq -er '.credentialVersion' <<<"${registration}")"
      count="$(psql_command "${idp_database}" -Atqc \
        "select count(*) from tianquan_oauth.oauth_client c where c.tenant_id = ${partition}
          and c.client_id = '${client_id}' and c.app_id = '${app_id}' and c.credential_version = ${version}
          and c.purpose = 'SERVICE' and c.status = 'ACTIVE' and c.deleted_at is null
          and exists (select 1 from tianquan_oauth.oauth_grant g join tianquan_oauth.oauth_resource r
            on r.tenant_id = ${platform_partition_id} and r.resource_server_id = g.resource_server_id
              and r.version = g.resource_version and r.status = 'ACTIVE' and r.deleted_at is null
            where g.tenant_id = c.tenant_id and g.client_pk = c.id and g.grant_type = 'CLIENT_CREDENTIALS'
              and g.status = 'ACTIVE' and g.deleted_at is null)")"
      [[ "${count}" == 1 ]] || fail "Current SERVICE client/credential/Resource/Grant facts are not provisioned"
    done < <(jq -r --arg client "${client_id}" \
      '"PLATFORM", (.clients[$client].tenants | keys[])' <<<"${oauth_registry_json}")
  done < <(jq -r '.clients | keys[]' <<<"${oauth_registry_json}")
}

local_user_input_metadata() {
  local tenant="$1" file ref issuer tenant_id token claims subject userinfo
  [[ -n "${oauth_registry_json:-}" ]] || load_local_oauth_registry
  file="${2:-$(jq -er --arg tenant "${tenant}" '.tenants[$tenant].accessTokenFile' <<<"${oauth_registry_json}")}" \
    || fail "A protected formal USER authorization input is required for this tenant"
  protected_oauth_file "${file}" 16384
  token="$(<"${file}")"
  [[ "${token}" =~ ^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$ ]] \
    || fail "Formal USER input has an invalid token format"
  issuer="$(local_oauth_issuer "$(jq -er --arg tenant "${tenant}" '.tenants[$tenant].issuerRef' <<<"${oauth_registry_json}")")"
  tenant_id="$(jq -er --arg tenant "${tenant}" '.tenants[$tenant].tenantId' <<<"${oauth_registry_json}")"
  subject="$(jq -er '.identitySub' <<<"${oauth_registry_json}")"
  claims="$(jq -Rer 'split(".")[1] | gsub("-";"+") | gsub("_";"/") | @base64d | fromjson' <<<"${token}" 2>/dev/null)" \
    || fail "Formal USER input payload is invalid"
  jq -e --arg issuer "${issuer}" --arg tenant "${tenant_id}" --arg subject "${subject}" '
    .iss == $issuer and (.tenant_id | tostring) == $tenant and .sub == $subject
    and .principal_type == "USER" and (.exp | type == "number" and . > now)
    and (.aud | type == "array" and length > 0)
  ' <<<"${claims}" >/dev/null || fail "Formal USER input is expired or outside its configured issuer/tenant/identity"
  printf '%s' "${token}"
}

user_access_token_for_tenant() {
  local tenant="$1" token issuer subject userinfo
  token="$(local_user_input_metadata "${tenant}")"
  issuer="$(local_oauth_issuer "$(jq -er --arg tenant "${tenant}" '.tenants[$tenant].issuerRef' <<<"${oauth_registry_json}")")"
  subject="$(jq -er '.identitySub' <<<"${oauth_registry_json}")"
  # Metadata parsing is only an early guard. The formal issuer verifies the active signed token.
  userinfo="$(curl --max-time 10 -fsS -H "Authorization: Bearer ${token}" "${issuer}/userinfo" 2>/dev/null)" \
    || fail "Formal USER authorization is not currently valid; complete operator OIDC authentication"
  jq -e --arg subject "${subject}" '.sub == $subject' <<<"${userinfo}" >/dev/null \
    || fail "Formal issuer UserInfo does not match the configured operator identity"
  printf '%s' "${token}"
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
  [[ ! -L "$1" ]] || fail "SERVICE credential output must not be a symlink"
  if [[ ! -e "$1" ]]; then
    printf 'pending-tianquan-shoubing-client-credentials' >"$1"
    chmod 600 "$1"
  fi
}

write_runtime_secrets() {
  ensure_password "${secret_dir}/tianquan-shoubing-admin.password"
  ensure_secret "${secret_dir}/tianshu-runtime.access-key" 18
  ensure_secret "${secret_dir}/tianshu-runtime.secret" 32
  ensure_secret "${secret_dir}/tianshu-registry.access-key" 18
  ensure_secret "${secret_dir}/tianshu-registry.secret" 32
  ensure_secret "${secret_dir}/tianshu-management.access-key" 18
  ensure_secret "${secret_dir}/tianshu-management.secret" 32
  ensure_secret "${secret_dir}/yuheng-master-key.base64" 32
  ensure_secret "${secret_dir}/tianquan-jianshen-audit.secret" 32
  ensure_rsa_key_pair "${secret_dir}/tianquan-shoubing"
  ensure_rsa_key_pair "${secret_dir}/tianquan-jianshen"
  ensure_rsa_key_pair "${secret_dir}/tianshu"
  ensure_rsa_key_pair "${secret_dir}/yuheng-admin"
  ensure_rsa_key_pair "${secret_dir}/yuheng-biz-gateway"
  ensure_rsa_key_pair "${secret_dir}/mock-backend"
  ensure_rsa_key_pair "${secret_dir}/mcp-provider"
  write_pending_service_credential "${secret_dir}/tianquan-shoubing-admin.service.jwt"
  write_pending_service_credential "${secret_dir}/tianquan-jianshen-admin.service.jwt"
  write_pending_service_credential "${secret_dir}/yuheng-admin.service.jwt"
  write_pending_service_credential "${secret_dir}/yuheng-admin-control-plane.service.jwt"
  write_pending_service_credential "${secret_dir}/yuheng-biz-gateway.service.jwt"
  write_pending_service_credential "${secret_dir}/yuheng-mcp-gateway.service.jwt"
  write_pending_service_credential "${secret_dir}/tianshu-admin.service.jwt"
  write_pending_service_credential "${secret_dir}/mock-backend.service.jwt"
  write_pending_service_credential "${secret_dir}/mcp-provider.service.jwt"
}

oauth_service_token() {
  local client_id="$1" output="$2"
  local resource="${3:-https://api.egon.internal/local/permission/tianquan-jianshen}"
  local scopes="${4:-service:authorization:decide service:authorization:snapshot service:identity:resolve}"
  local tenant_id="${5:-PLATFORM}" registration issuer secret_file response_file status token_endpoint token
  registration="$(local_oauth_registration "${client_id}" "${tenant_id}")"
  issuer="$(local_oauth_issuer "$(jq -er '.issuerRef' <<<"${registration}")")"
  token_endpoint="${issuer}/oauth2/token"
  secret_file="$(jq -er '.secretFile' <<<"${registration}")"
  protected_oauth_file "${secret_file}" 512
  [[ ! -L "${output}" ]] || fail "SERVICE output must not be a symlink"
  response_file="$(mktemp "${runtime_dir}/service-token.XXXXXX")"
  chmod 600 "${response_file}"
  # Configuration on stdin keeps client credentials out of process arguments.
  status="$(curl --max-time 10 --config - -sS -o "${response_file}" -w '%{http_code}' -X POST \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode grant_type=client_credentials --data-urlencode "resource=${resource}" \
    --data-urlencode "scope=${scopes}" "${token_endpoint}" <<CURL_CONFIG
user = "${client_id}:$(<"${secret_file}")"
CURL_CONFIG
)" || { rm -f "${response_file}"; fail "Formal SERVICE token request failed"; }
  if [[ "${status}" != 200 ]]; then
    rm -f "${response_file}"
    fail "Formal SERVICE token request returned HTTP ${status}"
  fi
  token="$(jq -er '.access_token | select(type == "string" and length <= 16384)' "${response_file}")" \
    || { rm -f "${response_file}"; fail "Formal SERVICE response did not contain a bounded access token"; }
  rm -f "${response_file}"
  printf '%s' "${token}" >"${output}"
  chmod 600 "${output}"
}

refresh_service_tokens() {
  oauth_service_token tianquan-shoubing-service \
    "${secret_dir}/tianquan-shoubing-admin.service.jwt"
  oauth_service_token tianquan-jianshen-service \
    "${secret_dir}/tianquan-jianshen-admin.service.jwt"
  oauth_service_token yuheng-admin-service \
    "${secret_dir}/yuheng-admin.service.jwt"
  oauth_service_token yuheng-admin-service \
    "${secret_dir}/yuheng-admin-control-plane.service.jwt" \
    https://api.egon.internal/local/platform/yuheng-admin \
    'yuheng:read yuheng:applications:write yuheng:catalog:write yuheng:credentials:write yuheng:drafts:write yuheng:groups:write yuheng:mcp:approve yuheng:mcp:read yuheng:mcp:runtime:read yuheng:mcp:test yuheng:mcp:write yuheng:releases:write'
  oauth_service_token yuheng-biz-gateway-service \
    "${secret_dir}/yuheng-biz-gateway.service.jwt"
  oauth_service_token yuheng-mcp-gateway-service \
    "${secret_dir}/yuheng-mcp-gateway.service.jwt"
  oauth_service_token tianshu-service \
    "${secret_dir}/tianshu-admin.service.jwt"
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
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_EGON_TIANQUAN_SHOUBING_CLIENT_ID)
      printf 'spring.security.oauth2.client.registration.egon-tianquan-shoubing.client-id'
      ;;
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_EGON_TIANQUAN_SHOUBING_CLIENT_SECRET)
      printf 'spring.security.oauth2.client.registration.egon-tianquan-shoubing.client-secret'
      ;;
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_EGON_TIANQUAN_SHOUBING_AUTHORIZATION_GRANT_TYPE)
      printf 'spring.security.oauth2.client.registration.egon-tianquan-shoubing.authorization-grant-type'
      ;;
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_EGON_TIANQUAN_SHOUBING_CLIENT_AUTHENTICATION_METHOD)
      printf 'spring.security.oauth2.client.registration.egon-tianquan-shoubing.client-authentication-method'
      ;;
    SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_EGON_TIANQUAN_SHOUBING_TOKEN_URI)
      printf 'spring.security.oauth2.client.provider.egon-tianquan-shoubing.token-uri'
      ;;
    EGON_COLA_PLATFORM_TIANQUAN_SHOUBING_SERVICE_CLIENT_APP_ID)
      printf 'egon.cola.platform.tianquan.shoubing.service-client.app-id'
      ;;
    EGON_COLA_PLATFORM_TIANQUAN_SHOUBING_SERVICE_CLIENT_REGISTRATION_ID)
      printf 'egon.cola.platform.tianquan.shoubing.service-client.registration-id'
      ;;
    EGON_COLA_COMPONENT_ID_MACHINE_ID)
      printf 'egon.cola.component.id.machine-id'
      ;;
    EGON_COLA_COMPONENT_TIANSHU_REGISTRATION_RESOURCE_URI)
      printf 'egon.cola.component.tianshu.registration-resource-uri'
      ;;
    EGON_COLA_COMPONENT_TIANSHU_RPC_MAX_INBOUND_MESSAGE_SIZE)
      printf 'egon.cola.component.tianshu.rpc.max-inbound-message-size'
      ;;
    EGON_COLA_COMPONENT_RPC_PROVIDER_MAX_INBOUND_MESSAGE_SIZE)
      printf 'egon.cola.component.rpc.provider.max-inbound-message-size'
      ;;
    TIANQUAN_SHOUBING_YUHENG_REPORTING_ENABLED|\
    TIANQUAN_JIANSHEN_YUHENG_REPORTING_ENABLED|\
    YUHENG_ADMIN_YUHENG_REPORTING_ENABLED|\
    TIANSHU_YUHENG_REPORTING_ENABLED|\
    MOCK_BACKEND_YUHENG_REPORTING_ENABLED)
      printf 'egon.cola.component.yuheng.reporting.enabled'
      ;;
    EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_RUNTIME_PASSWORD_FILE)
      printf 'egon.cola.platform.tianquan.jianshen.runtime.password-file'
      ;;
    EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_AUTHORIZATION_CACHE_TTL)
      printf 'egon.cola.platform.tianquan.jianshen.authorization.cache-ttl'
      ;;
    EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_AUTHORIZATION_MAXIMUM_JITTER)
      printf 'egon.cola.platform.tianquan.jianshen.authorization.maximum-jitter'
      ;;
    EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_AUTHORIZATION_NEAR_CACHE_TTL)
      printf 'egon.cola.platform.tianquan.jianshen.authorization.near-cache-ttl'
      ;;
    EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_AUTHORIZATION_SERVICE_TOKEN_ENABLED)
      printf 'egon.cola.platform.tianquan.jianshen.authorization.service-token.enabled'
      ;;
    EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_AUTHORIZATION_SERVICE_TOKEN_TOKEN_ENDPOINT)
      printf 'egon.cola.platform.tianquan.jianshen.authorization.service-token.token-endpoint'
      ;;
    EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_AUTHORIZATION_SERVICE_TOKEN_CLIENT_ID)
      printf 'egon.cola.platform.tianquan.jianshen.authorization.service-token.client-id'
      ;;
    EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_AUTHORIZATION_SERVICE_TOKEN_KEY_ID)
      printf 'egon.cola.platform.tianquan.jianshen.authorization.service-token.key-id'
      ;;
    EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_AUTHORIZATION_SERVICE_TOKEN_PRIVATE_KEY_FILE)
      printf 'egon.cola.platform.tianquan.jianshen.authorization.service-token.private-key-file'
      ;;
    EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_AUTHORIZATION_SERVICE_TOKEN_RESOURCE_URI)
      printf 'egon.cola.platform.tianquan.jianshen.authorization.service-token.resource-uri'
      ;;
    EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_AUTHORIZATION_SERVICE_TOKEN_SCOPES)
      printf 'egon.cola.platform.tianquan.jianshen.authorization.service-token.scopes'
      ;;
    EGON_COLA_COMPONENT_TIANSHU_ADMIN_REDIS_HOST)
      printf 'egon.cola.component.tianshu.admin.redis.host'
      ;;
    EGON_COLA_COMPONENT_TIANSHU_ADMIN_REDIS_PORT)
      printf 'egon.cola.component.tianshu.admin.redis.port'
      ;;
    EGON_COLA_COMPONENT_TIANSHU_ADMIN_REDIS_PASSWORD)
      printf 'egon.cola.component.tianshu.admin.redis.password'
      ;;
    EGON_COLA_COMPONENT_TIANSHU_ADMIN_REDIS_DATABASE)
      printf 'egon.cola.component.tianshu.admin.redis.database'
      ;;
    EGON_COLA_COMPONENT_TIANSHU_ENABLED)
      printf 'egon.cola.component.tianshu.enabled'
      ;;
    EGON_COLA_COMPONENT_TIANSHU_REDIS_ENABLED)
      printf 'egon.cola.component.tianshu.redis.enabled'
      ;;
    EGON_COLA_COMPONENT_TIANSHU_CONSISTENCY_FAIL_FAST)
      printf 'egon.cola.component.tianshu.consistency.fail-fast'
      ;;
    EGON_COLA_COMPONENT_YUHENG_ENGINE_YUHENG_GROUP_CODE)
      printf 'egon.cola.component.yuheng.engine.yuheng-group-code'
      ;;
    EGON_COLA_COMPONENT_YUHENG_ENGINE_ENV)
      printf 'egon.cola.component.yuheng.engine.env'
      ;;
    EGON_COLA_COMPONENT_YUHENG_ENGINE_NAMESPACE)
      printf 'egon.cola.component.yuheng.engine.namespace'
      ;;
    EGON_COLA_COMPONENT_YUHENG_ENGINE_NODE_ID)
      printf 'egon.cola.component.yuheng.engine.node-id'
      ;;
    EGON_COLA_COMPONENT_YUHENG_ENGINE_INSTANCE_ID)
      printf 'egon.cola.component.yuheng.engine.instance-id'
      ;;
    EGON_COLA_COMPONENT_YUHENG_ENGINE_DATA_DIRECTORY)
      printf 'egon.cola.component.yuheng.engine.data-directory'
      ;;
    EGON_COLA_COMPONENT_YUHENG_ENGINE_HTTP_PUBLIC_PORT)
      printf 'egon.cola.component.yuheng.engine.http.public-port'
      ;;
    EGON_COLA_COMPONENT_YUHENG_ENGINE_HTTP_INTERNAL_PORT)
      printf 'egon.cola.component.yuheng.engine.http.internal-port'
      ;;
    EGON_COLA_COMPONENT_YUHENG_PROVIDER_HTTP_FAIL_FAST)
      printf 'egon.cola.component.tianshu.registry.http.fail-fast'
      ;;
    YUHENG_ADMIN_TIANSHU_ENABLED) printf 'yuheng.admin.tianshu.enabled' ;;
    YUHENG_ADMIN_SECRETS_MASTER_KEY_BASE64)
      printf 'yuheng.admin.secrets.master-key-base64'
      ;;
    YUHENG_ADMIN_DEFINITION_RECONCILE_DELAY)
      printf 'yuheng.admin.definition-reconcile-delay'
      ;;
    YUHENG_ADMIN_RELEASE_RECONCILE_ENABLED)
      printf 'yuheng.admin.release-reconcile-enabled'
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

write_tianshu_client_identity_env() {
  local file="$1" tenant_id="$2" redis_username="$3"
  [[ "${tenant_id}" =~ ^[1-9][0-9]*$ ]] \
    || fail "Tianshu client tenant ID must be a positive integer"
  [[ -n "${redis_username}" ]] \
    || fail "Tianshu Redis ACL username must not be empty"
  write_env "${file}" TIANSHU_TENANT_ID "${tenant_id}"
  write_env "${file}" TIANSHU_REDIS_USERNAME "${redis_username}"
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
  write_env "${file}" UNIFIED_XINGYUAN_RUNTIME_DIR "${runtime_dir}"
  write_env "${file}" UNIFIED_IDENTITY_ENABLED true
  write_env "${file}" TIANQUAN_SHOUBING_ADMISSION_RPC_DEVELOPMENT_PLAINTEXT true
  write_local_oauth_trust_env "${file}"
  write_env "${file}" TIANQUAN_JIANSHEN_AUTHORIZATION_ENDPOINT "${rbac3_url}"
  write_env "${file}" EGON_COLA_COMPONENT_TIANSHU_REGISTRATION_RESOURCE_URI \
    https://api.egon.internal/local/platform/tianshu
  write_env "${file}" EGON_COLA_COMPONENT_TIANSHU_RPC_MAX_INBOUND_MESSAGE_SIZE \
    67108864
  write_env "${file}" EGON_COLA_COMPONENT_RPC_PROVIDER_MAX_INBOUND_MESSAGE_SIZE \
    67108864
  write_env "${file}" EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_RUNTIME_PASSWORD_FILE \
    "${secret_dir}/redis.password"
  write_env "${file}" EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_AUTHORIZATION_CACHE_TTL 1s
  write_env "${file}" EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_AUTHORIZATION_MAXIMUM_JITTER 0s
  write_env "${file}" EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_AUTHORIZATION_NEAR_CACHE_TTL 0s
}

write_idp_service_client_env() {
  write_local_oauth_registration_env "$1" "$2"
}

write_tenant_aware_rbac3_service_token_env() {
  local file="$1" client_id="$2"
  write_idp_service_client_env "${file}" "${client_id}"
  write_env "${file}" \
    EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_AUTHORIZATION_SERVICE_TOKEN_ENABLED true
  write_env "${file}" \
    EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_AUTHORIZATION_SERVICE_TOKEN_RESOURCE_URI \
    https://api.egon.internal/local/permission/tianquan-jianshen
  write_env "${file}" \
    EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_AUTHORIZATION_SERVICE_TOKEN_SCOPES \
    'service:authorization:decide service:authorization:snapshot service:identity:resolve'
}

write_service_env_files() {
  local redis_password postgres_password_value redis_username file
  redis_password="$(<"${secret_dir}/redis.password")"
  redis_username="${TIANSHU_REDIS_USERNAME:-default}"
  postgres_password_value="$(postgres_password)"

  file="$(new_env_file tianshu)"
  common_identity_env "${file}"
  write_tenant_aware_rbac3_service_token_env "${file}" \
    tianshu-service tianshu-local "${secret_dir}/tianshu-private.pem"
  write_env "${file}" SERVER_PORT 18150
  write_env "${file}" SPRING_DATASOURCE_URL "jdbc:postgresql://${postgres_host}:${postgres_port}/${ddc_database}"
  write_env "${file}" SPRING_DATASOURCE_USERNAME "${postgres_user}"
  write_env "${file}" SPRING_DATASOURCE_PASSWORD "${postgres_password_value}"
  write_env "${file}" EGON_COLA_COMPONENT_ID_MACHINE_ID 31
  write_env "${file}" EGON_COLA_COMPONENT_TIANSHU_ADMIN_REDIS_HOST "${redis_host}"
  write_env "${file}" EGON_COLA_COMPONENT_TIANSHU_ADMIN_REDIS_PORT "${redis_port}"
  write_env "${file}" EGON_COLA_COMPONENT_TIANSHU_ADMIN_REDIS_PASSWORD "${redis_password}"
  write_env "${file}" EGON_COLA_COMPONENT_TIANSHU_ADMIN_REDIS_DATABASE 10
  write_env "${file}" EGON_COLA_COMPONENT_TIANSHU_ENABLED false
  write_env "${file}" EGON_COLA_COMPONENT_TIANSHU_REDIS_ENABLED false
  write_env "${file}" TIANSHU_AUTHORIZATION_REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
  write_env "${file}" TIANSHU_AUTHORIZATION_REDIS_DATABASE 8
  write_env "${file}" TIANSHU_ADMIN_JWT_ISSUER "${platform_issuer}"
  write_env "${file}" TIANSHU_RESOURCE_SERVER_ID platform-tianshu-local
  write_env "${file}" TIANSHU_RESOURCE_URI \
    https://api.egon.internal/local/platform/tianshu
  write_env "${file}" TIANSHU_RESOURCE_ADMISSION_RPC_TARGET \
    "${idp_rpc_target}"
  write_env "${file}" TIANSHU_ADMIN_JWT_AUDIENCE \
    https://api.egon.internal/local/platform/tianshu
  write_env "${file}" TIANSHU_ADMIN_JWT_JWK_SET_URI "${platform_issuer}/.well-known/jwks.json"
  write_env "${file}" TIANSHU_RPC_PORT 19080
  write_env "${file}" TIANSHU_RPC_TARGET "${ddc_rpc_target}"
  write_env "${file}" TIANSHU_RPC_DEVELOPMENT_PLAINTEXT true
  write_env "${file}" TIANSHU_RPC_RUNTIME_ACCESS_KEY "$(<"${secret_dir}/tianshu-runtime.access-key")"
  write_env "${file}" TIANSHU_RPC_RUNTIME_SECRET_KEY "$(<"${secret_dir}/tianshu-runtime.secret")"
  write_env "${file}" TIANSHU_RPC_REGISTRY_ACCESS_KEY "$(<"${secret_dir}/tianshu-registry.access-key")"
  write_env "${file}" TIANSHU_RPC_REGISTRY_SECRET_KEY "$(<"${secret_dir}/tianshu-registry.secret")"
  write_env "${file}" TIANSHU_RPC_MANAGEMENT_ACCESS_KEY "$(<"${secret_dir}/tianshu-management.access-key")"
  write_env "${file}" TIANSHU_RPC_MANAGEMENT_SECRET_KEY "$(<"${secret_dir}/tianshu-management.secret")"
  # Tianshu must start once before Tianquan-Shoubing exists. Self-registration is enabled only
  # after Tianquan-Shoubing can issue the required PLATFORM SERVICE token.
  write_env "${file}" TIANSHU_SELF_REGISTRATION_ENABLED false
  write_env "${file}" TIANSHU_SELF_REGISTRATION_FAIL_FAST false
  write_env "${file}" TIANSHU_INSTANCE_ID tianshu-admin-local-1
  write_env "${file}" TIANSHU_ADVERTISED_HOST "${advertised_host}"
  write_env "${file}" TIANSHU_ADVERTISED_PORT 18150
  write_env "${file}" TIANSHU_ARTIFACT_VERSION local
  write_env "${file}" TIANSHU_MAX_CONFIG_BYTES 67108864
  write_env "${file}" TIANSHU_REDIS_HOST "${redis_host}"
  write_env "${file}" TIANSHU_REDIS_PORT "${redis_port}"
  write_env "${file}" TIANSHU_REDIS_PASSWORD "${redis_password}"
  write_env "${file}" TIANSHU_REDIS_DATABASE 10
  write_env "${file}" TIANSHU_YUHENG_REPORTING_ENABLED false
  write_env "${file}" TIANSHU_HTTP_OPENAPI_ENABLED true
  write_env "${file}" YUHENG_ADMIN_BASE_URL "${gateway_admin_url}"
  write_env "${file}" TIANSHU_RESOURCE_BIZ_CODE xingyuan
  write_env "${file}" TIANSHU_RESOURCE_APP_CODE tianshu
  write_env "${file}" TIANSHU_DECLARED_HOSTS "${declared_hosts}"
  write_env "${file}" YUHENG_REPORT_STATE_FILE "${runtime_dir}/tianshu-yuheng-report.json"

  file="$(new_env_file tianquan-shoubing)"
  write_tianshu_client_identity_env "${file}" "${service_tenant_id}" "${redis_username}"
  common_identity_env "${file}"
  write_tenant_aware_rbac3_service_token_env "${file}" \
    tianquan-shoubing-service tianquan-shoubing-local "${secret_dir}/tianquan-shoubing-private.pem"
  write_env "${file}" EGON_TIANQUAN_SHOUBING_PLATFORM_STORAGE_PARTITION_ID "${platform_partition_id}"
  write_env "${file}" EGON_TIANQUAN_SHOUBING_OAUTH_PUBLIC_ORIGIN "${idp_url}"
  write_env "${file}" EGON_TIANQUAN_SHOUBING_PLATFORM_MANAGEMENT_TENANT_ID \
    "$(jq -er '.platformManagementTenantId' <<<"${oauth_registry_json}")"
  write_env "${file}" EGON_TIANQUAN_SHOUBING_OAUTH_KEK_ID "$(jq -er '.kekId' <<<"${oauth_registry_json}")"
  write_env "${file}" EGON_TIANQUAN_SHOUBING_OAUTH_KEK_FILE "$(jq -er '.kekFile' <<<"${oauth_registry_json}")"
  write_env "${file}" EGON_TIANQUAN_SHOUBING_REVOCATION_EPOCH "$(jq -er '.revocationEpoch' <<<"${oauth_registry_json}")"
  write_env "${file}" EGON_TIANQUAN_SHOUBING_OAUTH_AUDIT_HMAC_KEY_FILE \
    "$(jq -er '.auditHmacKeyFile' <<<"${oauth_registry_json}")"
  mkdir -p "${runtime_dir}/oauth-audit-spool"
  chmod 700 "${runtime_dir}/oauth-audit-spool"
  write_env "${file}" EGON_TIANQUAN_SHOUBING_OAUTH_AUDIT_SPOOL_DIRECTORY "${runtime_dir}/oauth-audit-spool"
  write_oauth_property "${file}" egon.tianquan-shoubing.oauth.local-explicit-service-registrations true
  write_env "${file}" TIANQUAN_SHOUBING_POSTGRES_URL "jdbc:postgresql://${postgres_host}:${postgres_port}/${idp_database}"
  write_env "${file}" TIANQUAN_SHOUBING_POSTGRES_USER "${postgres_user}"
  write_env "${file}" TIANQUAN_SHOUBING_POSTGRES_PASSWORD "${postgres_password_value}"
  write_env "${file}" TIANQUAN_SHOUBING_REDIS_HOST "${redis_host}"
  write_env "${file}" TIANQUAN_SHOUBING_REDIS_PORT "${redis_port}"
  write_env "${file}" TIANQUAN_SHOUBING_REDIS_PASSWORD "${redis_password}"
  write_env "${file}" TIANQUAN_SHOUBING_REDIS_DATABASE 8
  write_env "${file}" TIANQUAN_SHOUBING_AUTHORIZATION_REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
  write_env "${file}" TIANQUAN_SHOUBING_AUTHORIZATION_REDIS_DATABASE 8
  write_env "${file}" TIANQUAN_SHOUBING_ADVERTISED_PORT 18120
  write_env "${file}" TIANQUAN_SHOUBING_OAUTH_ALLOWED_ORIGINS \
    http://127.0.0.1:18121,http://127.0.0.1:18125,http://127.0.0.1:18131,http://127.0.0.1:18141,http://127.0.0.1:18152
  write_env "${file}" TIANQUAN_SHOUBING_TIANQUAN_JIANSHEN_BASE_URL "${rbac3_url}"
  write_tenant_aware_rbac3_service_token_env "${file}" \
    tianquan-shoubing-service tianquan-shoubing-local "${secret_dir}/tianquan-shoubing-private.pem"
  write_env "${file}" TIANQUAN_SHOUBING_TIANQUAN_JIANSHEN_SERVICE_CLIENT_ID tianquan-shoubing-service
  write_env "${file}" TIANQUAN_SHOUBING_TIANQUAN_JIANSHEN_SERVICE_KEY_ID tianquan-shoubing-local
  write_env "${file}" TIANQUAN_SHOUBING_TIANQUAN_JIANSHEN_SERVICE_PRIVATE_KEY_FILE \
    "${secret_dir}/tianquan-shoubing-private.pem"
  write_env "${file}" TIANQUAN_SHOUBING_TIANQUAN_JIANSHEN_RESOURCE_URI \
    https://api.egon.internal/local/permission/tianquan-jianshen
  write_env "${file}" TIANQUAN_SHOUBING_TIANQUAN_JIANSHEN_SERVICE_TENANT_ID "${service_tenant_id}"
  write_env "${file}" TIANQUAN_SHOUBING_DEVELOPMENT_TIANQUAN_JIANSHEN_SERVICE_TENANT_ID \
    "${service_tenant_id}"
  write_env "${file}" TIANQUAN_SHOUBING_DEVELOPMENT_TIANQUAN_JIANSHEN_SERVICE_TENANT_IDS \
    "${service_tenant_id}"
  write_env "${file}" TIANQUAN_SHOUBING_TIANQUAN_JIANSHEN_SERVICE_SCOPES \
    'service:authorization:decide service:authorization:snapshot service:identity:resolve'
  write_env "${file}" TIANQUAN_SHOUBING_SNOWFLAKE_MACHINE_ID 32
  write_env "${file}" TIANQUAN_SHOUBING_DEVELOPMENT_BOOTSTRAP_ENABLED false
  write_env "${file}" TIANQUAN_SHOUBING_DEVELOPMENT_BOOTSTRAP_KEY_DIRECTORY \
    "${secret_dir}"
  write_env "${file}" TIANQUAN_SHOUBING_BOOTSTRAP_PASSWORD_FILE "${secret_dir}/tianquan-shoubing-admin.password"
  write_env "${file}" TIANQUAN_SHOUBING_TIANSHU_ENABLED true
  write_env "${file}" EGON_COLA_COMPONENT_TIANSHU_CONSISTENCY_FAIL_FAST false
  write_env "${file}" TIANQUAN_SHOUBING_HTTP_PROVIDER_ENABLED true
  write_env "${file}" TIANQUAN_SHOUBING_RPC_PORT 18122
  write_env "${file}" TIANQUAN_SHOUBING_RPC_PROVIDER_REGISTRATION_MODE DISABLED
  write_env "${file}" TIANQUAN_SHOUBING_RPC_DEVELOPMENT_PLAINTEXT true
  write_env "${file}" TIANQUAN_SHOUBING_RESOURCE_SERVER_ID permission-tianquan-shoubing-local
  write_env "${file}" TIANQUAN_SHOUBING_RESOURCE_URI \
    https://api.egon.internal/local/permission/tianquan-shoubing
  write_env "${file}" TIANQUAN_SHOUBING_RESOURCE_MANAGEMENT_CLIENT_ID tianquan-shoubing-service
  write_env "${file}" TIANQUAN_SHOUBING_RESOURCE_MANAGEMENT_KEY_ID tianquan-shoubing-local
  write_env "${file}" TIANQUAN_SHOUBING_RESOURCE_MANAGEMENT_PRIVATE_KEY_FILE \
    "${secret_dir}/tianquan-shoubing-private.pem"
  write_env "${file}" TIANQUAN_SHOUBING_RESOURCE_ADMISSION_RPC_TARGET \
    "${idp_rpc_target}"
  write_env "${file}" \
    EGON_COLA_COMPONENT_YUHENG_PROVIDER_HTTP_FAIL_FAST false
  write_env "${file}" TIANQUAN_SHOUBING_INSTANCE_ID tianquan-shoubing-local-1
  write_env "${file}" TIANQUAN_SHOUBING_ARTIFACT_VERSION local
  write_env "${file}" TIANQUAN_SHOUBING_ADVERTISED_HOST "${advertised_host}"
  write_env "${file}" TIANSHU_BIZ_CODE permission
  write_env "${file}" TIANSHU_APP_CODE tianquan-shoubing
  write_env "${file}" DEPLOYMENT_ENV local
  write_env "${file}" DEPLOYMENT_NAMESPACE default
  write_env "${file}" TIANSHU_RPC_TARGET "${ddc_rpc_target}"
  write_env "${file}" TIANSHU_RPC_DEVELOPMENT_PLAINTEXT true
  write_env "${file}" TIANSHU_RPC_RUNTIME_ACCESS_KEY "$(<"${secret_dir}/tianshu-runtime.access-key")"
  write_env "${file}" TIANSHU_RPC_RUNTIME_SECRET_KEY "$(<"${secret_dir}/tianshu-runtime.secret")"
  write_env "${file}" TIANSHU_RPC_REGISTRY_ACCESS_KEY "$(<"${secret_dir}/tianshu-registry.access-key")"
  write_env "${file}" TIANSHU_RPC_REGISTRY_SECRET_KEY "$(<"${secret_dir}/tianshu-registry.secret")"
  write_env "${file}" TIANSHU_RPC_MANAGEMENT_ACCESS_KEY "$(<"${secret_dir}/tianshu-management.access-key")"
  write_env "${file}" TIANSHU_RPC_MANAGEMENT_SECRET_KEY "$(<"${secret_dir}/tianshu-management.secret")"
  write_env "${file}" TIANSHU_REGISTRY_REDIS_HOST "${redis_host}"
  write_env "${file}" TIANSHU_REGISTRY_REDIS_PORT "${redis_port}"
  write_env "${file}" TIANSHU_REGISTRY_REDIS_PASSWORD "${redis_password}"
  write_env "${file}" TIANSHU_REGISTRY_REDIS_DATABASE 10
  write_env "${file}" TIANQUAN_SHOUBING_YUHENG_REPORTING_ENABLED false
  write_env "${file}" TIANQUAN_SHOUBING_HTTP_OPENAPI_ENABLED true
  write_env "${file}" YUHENG_ADMIN_BASE_URL "${gateway_admin_url}"
  write_env "${file}" TIANQUAN_SHOUBING_RESOURCE_BIZ_CODE permission
  write_env "${file}" TIANQUAN_SHOUBING_RESOURCE_APP_CODE tianquan-shoubing
  write_env "${file}" TIANQUAN_SHOUBING_DECLARED_HOSTS "${declared_hosts}"
  write_env "${file}" YUHENG_REPORT_STATE_FILE "${runtime_dir}/tianquan-shoubing-yuheng-report.json"

  file="$(new_env_file tianquan-jianshen)"
  write_tianshu_client_identity_env "${file}" "${service_tenant_id}" "${redis_username}"
  common_identity_env "${file}"
  write_tenant_aware_rbac3_service_token_env "${file}" tianquan-jianshen-service
  write_env "${file}" TIANQUAN_JIANSHEN_POSTGRES_URL "$(rbac3_jdbc_url)"
  write_env "${file}" TIANQUAN_JIANSHEN_POSTGRES_USER "${postgres_user}"
  write_env "${file}" TIANQUAN_JIANSHEN_POSTGRES_PASSWORD "${postgres_password_value}"
  write_env "${file}" TIANQUAN_JIANSHEN_ADVERTISED_PORT 18130
  write_env "${file}" TIANQUAN_JIANSHEN_ADVERTISED_HOST "${advertised_host}"
  write_env "${file}" TIANQUAN_JIANSHEN_INSTANCE_ID tianquan-jianshen-local-1
  write_env "${file}" TIANQUAN_JIANSHEN_ARTIFACT_VERSION local
  # The Tianquan-Jianshen identity-directory references are direct RPC references. Keep the
  # consumer disabled until Tianshu has admitted the Tianquan-Shoubing RPC Provider lease.
  write_env "${file}" TIANQUAN_JIANSHEN_RPC_ENABLED false
  write_env "${file}" TIANQUAN_JIANSHEN_RPC_CONSUMER_ENABLED false
  write_env "${file}" TIANQUAN_JIANSHEN_TIANSHU_ENABLED true
  write_env "${file}" EGON_COLA_COMPONENT_TIANSHU_CONSISTENCY_FAIL_FAST false
  write_env "${file}" TIANQUAN_JIANSHEN_HTTP_PROVIDER_ENABLED true
  write_env "${file}" TIANQUAN_JIANSHEN_RESOURCE_SERVER_ID permission-tianquan-jianshen-local
  write_env "${file}" TIANQUAN_JIANSHEN_RESOURCE_URI \
    https://api.egon.internal/local/permission/tianquan-jianshen
  write_env "${file}" TIANQUAN_JIANSHEN_RESOURCE_MANAGEMENT_CLIENT_ID tianquan-jianshen-service
  write_env "${file}" TIANQUAN_JIANSHEN_RESOURCE_MANAGEMENT_KEY_ID tianquan-jianshen-local
  write_env "${file}" TIANQUAN_JIANSHEN_RESOURCE_MANAGEMENT_PRIVATE_KEY_FILE \
    "${secret_dir}/tianquan-jianshen-private.pem"
  write_env "${file}" TIANQUAN_JIANSHEN_RESOURCE_ADMISSION_RPC_TARGET \
    "${idp_rpc_target}"
  write_env "${file}" \
    EGON_COLA_COMPONENT_YUHENG_PROVIDER_HTTP_FAIL_FAST false
  write_env "${file}" TIANSHU_BIZ_CODE permission
  write_env "${file}" TIANSHU_APP_CODE tianquan-jianshen
  write_env "${file}" DEPLOYMENT_ENV local
  write_env "${file}" DEPLOYMENT_NAMESPACE default
  write_env "${file}" TIANSHU_RPC_TARGET "${ddc_rpc_target}"
  write_env "${file}" TIANSHU_RPC_DEVELOPMENT_PLAINTEXT true
  write_env "${file}" TIANSHU_RPC_RUNTIME_ACCESS_KEY "$(<"${secret_dir}/tianshu-runtime.access-key")"
  write_env "${file}" TIANSHU_RPC_RUNTIME_SECRET_KEY "$(<"${secret_dir}/tianshu-runtime.secret")"
  write_env "${file}" TIANSHU_RPC_REGISTRY_ACCESS_KEY "$(<"${secret_dir}/tianshu-registry.access-key")"
  write_env "${file}" TIANSHU_RPC_REGISTRY_SECRET_KEY "$(<"${secret_dir}/tianshu-registry.secret")"
  write_env "${file}" TIANSHU_RPC_MANAGEMENT_ACCESS_KEY "$(<"${secret_dir}/tianshu-management.access-key")"
  write_env "${file}" TIANSHU_RPC_MANAGEMENT_SECRET_KEY "$(<"${secret_dir}/tianshu-management.secret")"
  write_env "${file}" TIANSHU_REGISTRY_REDIS_HOST "${redis_host}"
  write_env "${file}" TIANSHU_REGISTRY_REDIS_PORT "${redis_port}"
  write_env "${file}" TIANSHU_REGISTRY_REDIS_PASSWORD "${redis_password}"
  write_env "${file}" TIANSHU_REGISTRY_REDIS_DATABASE 10
  write_env "${file}" TIANQUAN_JIANSHEN_AUTHORIZATION_REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
  write_env "${file}" TIANQUAN_JIANSHEN_AUTHORIZATION_REDIS_DATABASE 8
  write_env "${file}" TIANQUAN_JIANSHEN_RUNTIME_REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
  write_env "${file}" TIANQUAN_JIANSHEN_RUNTIME_REDIS_DATABASE 8
  write_env "${file}" TIANQUAN_JIANSHEN_RUNTIME_REDIS_PASSWORD_FILE "${secret_dir}/redis.password"
  write_env "${file}" TIANQUAN_JIANSHEN_AUDIT_CURSOR_SECRET_FILE "${secret_dir}/tianquan-jianshen-audit.secret"
  write_env "${file}" TIANQUAN_JIANSHEN_SNOWFLAKE_MACHINE_ID 33
  write_env "${file}" TIANQUAN_JIANSHEN_DEVELOPMENT_BOOTSTRAP_ENABLED false
  write_env "${file}" TIANQUAN_JIANSHEN_DEVELOPMENT_AUTO_ACTIVATE_LOCAL_ADMIN_ROLES true
  write_env "${file}" TIANQUAN_JIANSHEN_DEVELOPMENT_TENANT_IDS "${service_tenant_id}"
  write_env "${file}" SPRING_FLYWAY_ENABLED true
  write_env "${file}" TIANQUAN_JIANSHEN_YUHENG_REPORTING_ENABLED false
  write_env "${file}" YUHENG_ADMIN_BASE_URL "${gateway_admin_url}"
  write_env "${file}" TIANQUAN_JIANSHEN_RESOURCE_BIZ_CODE permission
  write_env "${file}" TIANQUAN_JIANSHEN_RESOURCE_APP_CODE tianquan-jianshen
  write_env "${file}" TIANQUAN_JIANSHEN_DECLARED_HOSTS "${declared_hosts}"
  write_env "${file}" YUHENG_REPORT_STATE_FILE "${runtime_dir}/tianquan-jianshen-yuheng-report.json"

  file="$(new_env_file yuheng-admin)"
  write_tianshu_client_identity_env "${file}" "${service_tenant_id}" "${redis_username}"
  common_identity_env "${file}"
  write_env "${file}" DEPLOYMENT_ENV local
  write_tenant_aware_rbac3_service_token_env "${file}" \
    yuheng-admin-service yuheng-admin-local \
    "${secret_dir}/yuheng-admin-private.pem"
  write_env "${file}" SERVER_PORT 18140
  write_env "${file}" SPRING_DATASOURCE_URL "jdbc:postgresql://${postgres_host}:${postgres_port}/${gateway_database}"
  write_env "${file}" SPRING_DATASOURCE_USERNAME "${postgres_user}"
  write_env "${file}" SPRING_DATASOURCE_PASSWORD "${postgres_password_value}"
  write_env "${file}" EGON_COLA_COMPONENT_ID_MACHINE_ID 34
  write_env "${file}" YUHENG_AUTHORIZATION_REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
  write_env "${file}" YUHENG_AUTHORIZATION_REDIS_DATABASE 8
  write_env "${file}" YUHENG_ADMIN_RESOURCE_SERVER_ID \
    platform-yuheng-admin-local
  write_env "${file}" YUHENG_ADMIN_RESOURCE_URI \
    https://api.egon.internal/local/platform/yuheng-admin
  write_env "${file}" YUHENG_ADMIN_RESOURCE_MANAGEMENT_CLIENT_ID \
    yuheng-admin-service
  write_env "${file}" YUHENG_ADMIN_RESOURCE_MANAGEMENT_KEY_ID \
    yuheng-admin-local
  write_env "${file}" YUHENG_ADMIN_RESOURCE_MANAGEMENT_PRIVATE_KEY_FILE \
    "${secret_dir}/yuheng-admin-private.pem"
  write_env "${file}" YUHENG_ADMIN_RESOURCE_ADMISSION_RPC_TARGET \
    "${idp_rpc_target}"
  write_env "${file}" YUHENG_ADMIN_INSTANCE_ID yuheng-admin-local-1
  write_env "${file}" YUHENG_ADMIN_SECRETS_MASTER_KEY_BASE64 "$(<"${secret_dir}/yuheng-master-key.base64")"
  write_env "${file}" YUHENG_MCP_ARTIFACT_ROOT "${runtime_dir}/mcp-artifacts"
  write_env "${file}" YUHENG_ADMIN_TIANSHU_ENABLED true
  # Local OpenAPI ingestion is explicitly restricted to the detected provider
  # host. Production keeps the HTTPS-only default from application.yml.
  write_env "${file}" YUHENG_ADMIN_OPENAPI_ENABLED true
  write_env "${file}" YUHENG_ADMIN_HTTP_OPENAPI_ENABLED true
  write_env "${file}" YUHENG_ADMIN_OPENAPI_ALLOW_DEVELOPMENT_HTTP true
  write_env "${file}" YUHENG_ADMIN_OPENAPI_ALLOWED_CIDR \
    "${advertised_host}/32"
  write_env "${file}" YUHENG_ADMIN_OPENAPI_RECONCILE_DELAY PT1S
  write_env "${file}" TIANSHU_MAX_CONFIG_BYTES 67108864
  write_env "${file}" EGON_COLA_COMPONENT_TIANSHU_RPC_DEFAULT_TIMEOUT 300s
  write_env "${file}" YUHENG_ADMIN_RULE_CHUNK_RETENTION 24h
  write_env "${file}" YUHENG_ADMIN_RULE_CHUNK_CLEANUP_DELAY 1h
  write_env "${file}" TIANSHU_ENABLED true
  write_env "${file}" TIANSHU_REGISTRY_ENABLED true
  write_env "${file}" YUHENG_ADMIN_TIANSHU_REGISTRATION_ENABLED true
  write_env "${file}" TIANSHU_RPC_TARGET "${ddc_rpc_target}"
  write_env "${file}" TIANSHU_RPC_DEVELOPMENT_PLAINTEXT true
  write_env "${file}" TIANSHU_RPC_RUNTIME_ACCESS_KEY "$(<"${secret_dir}/tianshu-runtime.access-key")"
  write_env "${file}" TIANSHU_RPC_RUNTIME_SECRET_KEY "$(<"${secret_dir}/tianshu-runtime.secret")"
  write_env "${file}" TIANSHU_RPC_REGISTRY_ACCESS_KEY "$(<"${secret_dir}/tianshu-registry.access-key")"
  write_env "${file}" TIANSHU_RPC_REGISTRY_SECRET_KEY "$(<"${secret_dir}/tianshu-registry.secret")"
  write_env "${file}" TIANSHU_RPC_MANAGEMENT_ACCESS_KEY "$(<"${secret_dir}/tianshu-management.access-key")"
  write_env "${file}" TIANSHU_RPC_MANAGEMENT_SECRET_KEY "$(<"${secret_dir}/tianshu-management.secret")"
  write_env "${file}" TIANSHU_REDIS_HOST "${redis_host}"
  write_env "${file}" TIANSHU_REDIS_PORT "${redis_port}"
  write_env "${file}" TIANSHU_REDIS_PASSWORD "${redis_password}"
  write_env "${file}" TIANSHU_REDIS_DATABASE 10
  write_env "${file}" YUHENG_ADMIN_TIANSHU_ADVERTISED_HOST "${advertised_host}"
  write_env "${file}" YUHENG_ADMIN_TIANSHU_ADVERTISED_PORT 18140
  write_env "${file}" YUHENG_ADMIN_VERSION local
  write_env "${file}" YUHENG_ADMIN_TIANSHU_API_RPC_BIZ_CODE identity
  write_env "${file}" YUHENG_ADMIN_TIANSHU_API_RPC_APP_CODE yuheng-biz-gateway-default
  write_env "${file}" YUHENG_ADMIN_TIANSHU_MCP_BIZ_CODE identity
  write_env "${file}" YUHENG_ADMIN_TIANSHU_MCP_APP_CODE yuheng-mcp-gateway-default
  write_env "${file}" YUHENG_ADMIN_DEFINITION_RECONCILE_DELAY 1000
  if [[ "${startup_mode}" == "xingyuan" ]]; then
    write_env "${file}" YUHENG_ADMIN_RELEASE_RECONCILE_ENABLED false
  fi
  write_env "${file}" YUHENG_ADMIN_YUHENG_REPORTING_ENABLED false
  write_env "${file}" YUHENG_ADMIN_BASE_URL "${gateway_admin_url}"
  write_env "${file}" YUHENG_ADMIN_RESOURCE_BIZ_CODE xingyuan
  write_env "${file}" YUHENG_ADMIN_RESOURCE_APP_CODE yuheng-admin
  write_env "${file}" YUHENG_ADMIN_DECLARED_HOSTS "${declared_hosts}"
  write_env "${file}" YUHENG_REPORT_STATE_FILE "${runtime_dir}/yuheng-admin-yuheng-report.json"

  file="$(new_env_file mock-backend)"
  write_tianshu_client_identity_env "${file}" "${service_tenant_id}" "${redis_username}"
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
  write_env "${file}" MOCK_BACKEND_TIANSHU_ENABLED true
  write_env "${file}" MOCK_BACKEND_ADVERTISED_HOST "${advertised_host}"
  write_env "${file}" TIANSHU_BIZ_CODE identity
  write_env "${file}" TIANSHU_RPC_TARGET "${ddc_rpc_target}"
  write_env "${file}" TIANSHU_RPC_DEVELOPMENT_PLAINTEXT true
  write_env "${file}" TIANSHU_RPC_RUNTIME_ACCESS_KEY "$(<"${secret_dir}/tianshu-runtime.access-key")"
  write_env "${file}" TIANSHU_RPC_RUNTIME_SECRET_KEY "$(<"${secret_dir}/tianshu-runtime.secret")"
  write_env "${file}" TIANSHU_RPC_REGISTRY_ACCESS_KEY "$(<"${secret_dir}/tianshu-registry.access-key")"
  write_env "${file}" TIANSHU_RPC_REGISTRY_SECRET_KEY "$(<"${secret_dir}/tianshu-registry.secret")"
  write_env "${file}" TIANSHU_REGISTRY_REDIS_HOST "${redis_host}"
  write_env "${file}" TIANSHU_REGISTRY_REDIS_PORT "${redis_port}"
  write_env "${file}" TIANSHU_REGISTRY_REDIS_PASSWORD "${redis_password}"
  write_env "${file}" TIANSHU_REGISTRY_REDIS_DATABASE 10
  write_env "${file}" MOCK_BACKEND_YUHENG_REPORTING_ENABLED true
  write_env "${file}" YUHENG_ADMIN_BASE_URL "${gateway_admin_url}"
  write_env "${file}" YUHENG_REPORT_STATE_FILE "${runtime_dir}/mock-backend-yuheng-report.json"

  file="$(new_env_file yuheng-biz-gateway)"
  write_tianshu_client_identity_env "${file}" "${service_tenant_id}" "${redis_username}"
  write_tenant_aware_rbac3_service_token_env "${file}" \
    yuheng-biz-gateway-service yuheng-biz-gateway-local \
    "${secret_dir}/yuheng-biz-gateway-private.pem"
  write_env "${file}" EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_SYSTEM_CODE mock-backend
  write_env "${file}" EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_AUTHORIZATION_ENDPOINT \
    "${rbac3_url}"
  write_env "${file}" SERVER_PORT 18182
  write_local_oauth_trust_env "${file}"
  write_env "${file}" EGON_COLA_COMPONENT_TIANSHU_REGISTRATION_RESOURCE_URI \
    https://api.egon.internal/local/platform/tianshu
  write_env "${file}" YUHENG_ENGINE_RESOURCE_SERVER_ID \
    identity-yuheng-biz-gateway-default-local
  write_env "${file}" YUHENG_ENGINE_RESOURCE_URI \
    https://api.egon.internal/local/identity/yuheng-biz-gateway-default
  write_env "${file}" YUHENG_ENGINE_RESOURCE_MANAGEMENT_CLIENT_ID \
    yuheng-biz-gateway-service
  write_env "${file}" YUHENG_ENGINE_RESOURCE_MANAGEMENT_KEY_ID \
    yuheng-biz-gateway-local
  write_env "${file}" YUHENG_ENGINE_RESOURCE_MANAGEMENT_PRIVATE_KEY_FILE \
    "${secret_dir}/yuheng-biz-gateway-private.pem"
  write_env "${file}" YUHENG_ENGINE_RESOURCE_ADMISSION_RPC_TARGET \
    "${idp_rpc_target}"
  write_env "${file}" TIANQUAN_SHOUBING_ADMISSION_RPC_DEVELOPMENT_PLAINTEXT true
  write_env "${file}" YUHENG_MCP_TASK_SERVICE_TOKEN_ENABLED true
  write_env "${file}" YUHENG_MCP_TASK_SERVICE_TOKEN_CLIENT_ID \
    yuheng-biz-gateway-service
  write_env "${file}" YUHENG_MCP_TASK_SERVICE_TOKEN_KEY_ID \
    yuheng-biz-gateway-local
  write_env "${file}" YUHENG_MCP_TASK_SERVICE_TOKEN_PRIVATE_KEY_FILE \
    "${secret_dir}/yuheng-biz-gateway-private.pem"
  write_env "${file}" YUHENG_MCP_TASK_SERVICE_TOKEN_SCOPES \
    mcp:operation:invoke
  write_env "${file}" TIANQUAN_SHOUBING_REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
  write_env "${file}" TIANQUAN_SHOUBING_REDIS_DATABASE 8
  write_env "${file}" TIANQUAN_SHOUBING_REDIS_PASSWORD_FILE "${secret_dir}/redis.password"
  write_env "${file}" TIANQUAN_SHOUBING_REFRESH_STATUS_RESOURCE_URI \
    https://api.egon.internal/local/permission/tianquan-shoubing
  write_env "${file}" TIANQUAN_SHOUBING_REFRESH_STATUS_SCOPES tianquan-shoubing:refresh-token:validate
  write_env "${file}" TIANQUAN_SHOUBING_YUHENG_TRUSTED_ORIGINS \
    http://127.0.0.1:18121,http://127.0.0.1:18125,http://127.0.0.1:18131,http://127.0.0.1:18141,http://127.0.0.1:18152
  write_env "${file}" YUHENG_TIANQUAN_JIANSHEN_SCOPE_ENABLED true
  write_env "${file}" YUHENG_TIANQUAN_JIANSHEN_SCOPE_REDIS_ADDRESS \
    "redis://${redis_host}:${redis_port}"
  write_env "${file}" YUHENG_TIANQUAN_JIANSHEN_SCOPE_REDIS_DATABASE 8
  write_env "${file}" YUHENG_TIANQUAN_JIANSHEN_SCOPE_REDIS_PASSWORD_FILE \
    "${secret_dir}/redis.password"
  write_env "${file}" YUHENG_TIANQUAN_JIANSHEN_SCOPE_REDIS_TIMEOUT 2s
  write_env "${file}" YUHENG_POSTGRES_URL "jdbc:postgresql://${postgres_host}:${postgres_port}/${gateway_database}"
  write_env "${file}" YUHENG_POSTGRES_USER "${postgres_user}"
  write_env "${file}" YUHENG_POSTGRES_PASSWORD "${postgres_password_value}"
  write_env "${file}" YUHENG_MCP_ARTIFACT_ROOT "${runtime_dir}/mcp-artifacts"
  write_env "${file}" YUHENG_MCP_REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
  write_env "${file}" YUHENG_MCP_REDIS_DATABASE 8
  write_env "${file}" YUHENG_MCP_REDIS_PASSWORD "${redis_password}"
  write_env "${file}" YUHENG_MCP_TIANQUAN_JIANSHEN_ENABLED true
  write_env "${file}" YUHENG_MCP_TIANQUAN_JIANSHEN_SYSTEM_CODE mock-backend
  write_env "${file}" YUHENG_MCP_TIANQUAN_JIANSHEN_REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
  write_env "${file}" YUHENG_MCP_TIANQUAN_JIANSHEN_REDIS_DATABASE 8
  write_env "${file}" YUHENG_MCP_TIANQUAN_JIANSHEN_REDIS_PASSWORD_FILE "${secret_dir}/redis.password"
  write_env "${file}" YUHENG_MCP_TIANQUAN_JIANSHEN_AUTHORIZATION_ENDPOINT "${rbac3_url}"
  write_env "${file}" EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_AUTHORIZATION_CACHE_TTL 1s
  write_env "${file}" EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_AUTHORIZATION_MAXIMUM_JITTER 0s
  write_env "${file}" EGON_COLA_PLATFORM_TIANQUAN_JIANSHEN_AUTHORIZATION_NEAR_CACHE_TTL 0s
  write_env "${file}" EGON_COLA_COMPONENT_ID_MACHINE_ID 35
  write_env "${file}" TIANSHU_ENABLED true
  write_env "${file}" TIANSHU_MAX_CONFIG_BYTES 67108864
  write_env "${file}" EGON_COLA_COMPONENT_TIANSHU_RPC_MAX_INBOUND_MESSAGE_SIZE \
    67108864
  write_env "${file}" TIANSHU_BIZ_CODE identity
  write_env "${file}" TIANSHU_APP_CODE yuheng-biz-gateway-default
  write_env "${file}" TIANSHU_ENV local
  write_env "${file}" TIANSHU_RPC_TARGET "${ddc_rpc_target}"
  write_env "${file}" TIANSHU_RPC_DEVELOPMENT_PLAINTEXT true
  write_env "${file}" TIANSHU_RPC_RUNTIME_ACCESS_KEY "$(<"${secret_dir}/tianshu-runtime.access-key")"
  write_env "${file}" TIANSHU_RPC_RUNTIME_SECRET_KEY "$(<"${secret_dir}/tianshu-runtime.secret")"
  write_env "${file}" TIANSHU_RPC_REGISTRY_ACCESS_KEY "$(<"${secret_dir}/tianshu-registry.access-key")"
  write_env "${file}" TIANSHU_RPC_REGISTRY_SECRET_KEY "$(<"${secret_dir}/tianshu-registry.secret")"
  write_env "${file}" TIANSHU_REDIS_HOST "${redis_host}"
  write_env "${file}" TIANSHU_REDIS_PORT "${redis_port}"
  write_env "${file}" TIANSHU_REDIS_PASSWORD "${redis_password}"
  write_env "${file}" TIANSHU_REDIS_DATABASE 10
  write_env "${file}" EGON_COLA_COMPONENT_TIANSHU_CONSISTENCY_FAIL_FAST false
  write_env "${file}" EGON_COLA_COMPONENT_YUHENG_ENGINE_YUHENG_GROUP_CODE default
  write_env "${file}" EGON_COLA_COMPONENT_YUHENG_ENGINE_ENV local
  write_env "${file}" EGON_COLA_COMPONENT_YUHENG_ENGINE_NAMESPACE default
  write_env "${file}" EGON_COLA_COMPONENT_YUHENG_ENGINE_NODE_ID yuheng-biz-gateway-local
  write_env "${file}" EGON_COLA_COMPONENT_YUHENG_ENGINE_INSTANCE_ID yuheng-biz-gateway-local-1
  write_env "${file}" EGON_COLA_COMPONENT_YUHENG_ENGINE_DATA_DIRECTORY "${runtime_dir}/yuheng-biz-gateway-data"
  write_env "${file}" EGON_COLA_COMPONENT_YUHENG_ENGINE_HTTP_PUBLIC_PORT 18180
  write_env "${file}" EGON_COLA_COMPONENT_YUHENG_ENGINE_HTTP_INTERNAL_PORT 18181
  write_env "${file}" EGON_COLA_COMPONENT_YUHENG_PROVIDER_HTTP_FAIL_FAST false
  write_env "${file}" YUHENG_ENGINE_TIANSHU_INSTANCE_ID yuheng-biz-gateway-local-1
  write_env "${file}" YUHENG_ENGINE_TIANSHU_ADVERTISED_HOST "${advertised_host}"
  write_env "${file}" YUHENG_ENGINE_TIANSHU_ADVERTISED_PORT 18180
  write_env "${file}" YUHENG_MCP_REMOTE_CIRCUIT_OPEN_DURATION PT3S
  write_env "${file}" YUHENG_MCP_REMOTE_FAILURE_THRESHOLD 2
  write_env "${file}" YUHENG_MCP_TASK_POLL_INTERVAL PT1S
  write_mcp_engine_env_file
}

# The MCP process has its own OAuth resource/client and Tianshu source scope.
# Do not copy the API Engine environment: its identity and ports are not aliases.
write_mcp_engine_env_file() {
  local file redis_password postgres_password_value redis_username
  file="$(new_env_file yuheng-mcp-gateway)"
  redis_password="$(<"${secret_dir}/redis.password")"
  redis_username="${TIANSHU_REDIS_USERNAME:-default}"
  postgres_password_value="$(postgres_password)"
  write_tianshu_client_identity_env "${file}" "${service_tenant_id}" "${redis_username}"
  common_identity_env "${file}"
  write_tenant_aware_rbac3_service_token_env "${file}" yuheng-mcp-gateway-service
  write_env "${file}" SERVER_PORT 18186
  write_env "${file}" YUHENG_MCP_ENGINE_MANAGEMENT_PORT 18186
  write_env "${file}" YUHENG_MCP_ENGINE_PORT 18185
  write_env "${file}" YUHENG_MCP_ENGINE_RESOURCE_SERVER_ID identity-yuheng-mcp-gateway-default-local
  write_env "${file}" YUHENG_MCP_ENGINE_RESOURCE_URI \
    https://api.egon.internal/local/identity/yuheng-mcp-gateway-default
  write_env "${file}" TIANQUAN_SHOUBING_REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
  write_env "${file}" TIANQUAN_SHOUBING_REDIS_DATABASE 8
  write_env "${file}" TIANQUAN_SHOUBING_REDIS_PASSWORD_FILE "${secret_dir}/redis.password"
  write_env "${file}" TIANQUAN_SHOUBING_REFRESH_STATUS_RESOURCE_URI https://api.egon.internal/local/permission/tianquan-shoubing
  write_env "${file}" TIANQUAN_SHOUBING_REFRESH_STATUS_SCOPES tianquan-shoubing:refresh-token:validate
  write_env "${file}" TIANQUAN_SHOUBING_YUHENG_TRUSTED_ORIGINS \
    http://127.0.0.1:18121,http://127.0.0.1:18125,http://127.0.0.1:18131,http://127.0.0.1:18141,http://127.0.0.1:18152
  write_env "${file}" YUHENG_MCP_TIANQUAN_JIANSHEN_ENABLED true
  write_env "${file}" YUHENG_MCP_TIANQUAN_JIANSHEN_SYSTEM_CODE mock-backend
  write_env "${file}" YUHENG_MCP_TIANQUAN_JIANSHEN_REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
  write_env "${file}" YUHENG_MCP_TIANQUAN_JIANSHEN_REDIS_DATABASE 8
  write_env "${file}" YUHENG_MCP_TIANQUAN_JIANSHEN_REDIS_PASSWORD_FILE "${secret_dir}/redis.password"
  write_env "${file}" YUHENG_MCP_TIANQUAN_JIANSHEN_AUTHORIZATION_ENDPOINT "${rbac3_url}"
  write_env "${file}" YUHENG_MCP_TIANQUAN_JIANSHEN_SCOPE_ENABLED true
  write_env "${file}" YUHENG_MCP_TIANQUAN_JIANSHEN_SCOPE_REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
  write_env "${file}" YUHENG_MCP_TIANQUAN_JIANSHEN_SCOPE_REDIS_DATABASE 8
  write_env "${file}" YUHENG_MCP_TIANQUAN_JIANSHEN_SCOPE_REDIS_PASSWORD_FILE "${secret_dir}/redis.password"
  write_env "${file}" YUHENG_MCP_POSTGRES_URL "jdbc:postgresql://${postgres_host}:${postgres_port}/${gateway_database}"
  write_env "${file}" YUHENG_MCP_POSTGRES_USER "${postgres_user}"
  write_env "${file}" YUHENG_MCP_POSTGRES_PASSWORD "${postgres_password_value}"
  write_env "${file}" YUHENG_MCP_REDIS_ADDRESS "redis://${redis_host}:${redis_port}"
  write_env "${file}" YUHENG_MCP_REDIS_DATABASE 8
  write_env "${file}" YUHENG_MCP_REDIS_PASSWORD "${redis_password}"
  write_env "${file}" YUHENG_MCP_ARTIFACT_ROOT "${runtime_dir}/mcp-artifacts"
  write_env "${file}" EGON_COLA_COMPONENT_ID_MACHINE_ID 36
  write_env "${file}" TIANSHU_ENABLED true
  write_env "${file}" TIANSHU_MAX_CONFIG_BYTES 67108864
  write_env "${file}" TIANSHU_BIZ_CODE identity
  write_env "${file}" TIANSHU_APP_CODE yuheng-mcp-gateway-default
  write_env "${file}" TIANSHU_ENV local
  write_env "${file}" TIANSHU_RPC_TARGET "${ddc_rpc_target}"
  write_env "${file}" TIANSHU_RPC_DEVELOPMENT_PLAINTEXT true
  write_env "${file}" YUHENG_MCP_TIANSHU_RPC_RUNTIME_ACCESS_KEY "$(<"${secret_dir}/tianshu-runtime.access-key")"
  write_env "${file}" YUHENG_MCP_TIANSHU_RPC_RUNTIME_SECRET_KEY "$(<"${secret_dir}/tianshu-runtime.secret")"
  write_env "${file}" YUHENG_MCP_TIANSHU_RPC_REGISTRY_ACCESS_KEY "$(<"${secret_dir}/tianshu-registry.access-key")"
  write_env "${file}" YUHENG_MCP_TIANSHU_RPC_REGISTRY_SECRET_KEY "$(<"${secret_dir}/tianshu-registry.secret")"
  write_env "${file}" TIANSHU_REDIS_HOST "${redis_host}"
  write_env "${file}" TIANSHU_REDIS_PORT "${redis_port}"
  write_env "${file}" TIANSHU_REDIS_PASSWORD "${redis_password}"
  write_env "${file}" TIANSHU_REDIS_DATABASE 10
  write_env "${file}" EGON_COLA_COMPONENT_TIANSHU_CONSISTENCY_FAIL_FAST false
  write_env "${file}" YUHENG_MCP_ENGINE_GROUP_CODE default
  write_env "${file}" YUHENG_MCP_ENGINE_ENV local
  write_env "${file}" YUHENG_MCP_ENGINE_NAMESPACE default
  write_env "${file}" YUHENG_MCP_ENGINE_NODE_ID yuheng-mcp-gateway-local
  write_env "${file}" YUHENG_MCP_ENGINE_TIANSHU_INSTANCE_ID yuheng-mcp-gateway-local-1
  write_env "${file}" YUHENG_MCP_ENGINE_DATA_DIRECTORY "${runtime_dir}/yuheng-mcp-gateway-data"
  write_env "${file}" YUHENG_MCP_ENGINE_TIANSHU_ADVERTISED_HOST "${advertised_host}"
  write_env "${file}" YUHENG_MCP_ENGINE_TIANSHU_ADVERTISED_PORT 18185
  write_env "${file}" YUHENG_MCP_ENGINE_VERSION local
  write_env "${file}" YUHENG_MCP_ENGINE_DEVELOPMENT_PLAINTEXT true
  write_env "${file}" YUHENG_MCP_ENGINE_OUTBOUND_RPC_DEVELOPMENT_PLAINTEXT true
  write_env "${file}" YUHENG_MCP_TASK_POLL_INTERVAL PT1S
}

package_applications() {
  if [[ "${UNIFIED_IDENTITY_SKIP_BUILD:-false}" == "true" ]] \
      && [[ -s "${idp_jar}" && -s "${rbac3_jar}" && -s "${gateway_admin_jar}" \
      && -s "${gateway_engine_jar}" && -s "${gateway_mcp_engine_jar}" && -s "${ddc_jar}" && -s "${mock_jar}" ]]; then
    return
  fi
  "${repo_root}/mvnw" -B -ntp -f "${repo_root}/pom.xml" \
    -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin,egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin,egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin,egon-cola-xingyuan/egon-cola-yuheng/yuheng-biz-gateway,egon-cola-xingyuan/egon-cola-yuheng/yuheng-mcp-gateway,egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-tianquan-shoubing-backend \
    -am package -DskipTests
}

write_application_build_ids() {
  write_env "${env_dir}/tianquan-shoubing.env" TIANQUAN_SHOUBING_BUILD_ID "$(local_build_id "${idp_jar}")"
  write_env "${env_dir}/tianquan-jianshen.env" TIANQUAN_JIANSHEN_BUILD_ID "$(local_build_id "${rbac3_jar}")"
  write_env "${env_dir}/yuheng-admin.env" YUHENG_ADMIN_BUILD_ID "$(local_build_id "${gateway_admin_jar}")"
  write_env "${env_dir}/tianshu.env" TIANSHU_BUILD_ID "$(local_build_id "${ddc_jar}")"
  write_env "${env_dir}/mock-backend.env" \
    MOCK_BACKEND_BUILD_ID "$(local_build_id "${mock_jar}")"
}

command_prepare() {
  load_local_oauth_registry
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
    || fail "invalid Tianshu RPC target: ${ddc_rpc_target}"
  for ((attempt = 1; attempt <= 90; attempt++)); do
    if nc -z -w 1 "${host}" "${port}" >/dev/null 2>&1; then
      return
    fi
    sleep 1
  done
  fail "Tianshu RPC did not become ready at ${ddc_rpc_target}"
}



identity_subject() {
  local subject count
  subject="$(jq -er '.identitySub' <<<"${oauth_registry_json}")"
  count="$(psql_command "${idp_database}" -Atqc \
    "select count(*) from tianquan_oauth.oauth_identity_user where tenant_id = ${platform_partition_id}
      and identity_sub = '${subject}' and status = 'ACTIVE' and deleted_at is null")"
  [[ "${count}" == 1 ]] || fail "Current MP operator identity is not available"
  printf '%s' "${subject}"
}

rbac3_tenant_id() {
  local tenant_code="$1" tenant_id count
  [[ "${tenant_code}" =~ ^[a-z][a-z0-9-]{2,63}$ ]] || fail "unsafe tenant code"
  [[ -n "${oauth_registry_json:-}" ]] || load_local_oauth_registry
  tenant_id="$(jq -er --arg code "${tenant_code}" '.tenants[$code].tenantId' <<<"${oauth_registry_json}")" \
    || fail "Local tenant is not explicitly configured"
  count="$(psql_command "${idp_database}" -Atqc \
    "select count(*) from tianquan_oauth.oauth_identity_tenant where tenant_id = ${platform_partition_id}
      and identity_tenant_id = '${tenant_id}' and tenant_code = '${tenant_code}'
      and status = 'ACTIVE' and deleted_at is null")"
  [[ "${count}" == 1 ]] || fail "Current MP tenant does not match the operator registry"
  printf '%s' "${tenant_id}"
}

resolve_existing_service_tenant_id() {
  [[ -n "${oauth_registry_json:-}" ]] || load_local_oauth_registry
  service_tenant_id="$(jq -er '.tenants.default.tenantId' <<<"${oauth_registry_json}")"
  require_oauth_decimal "${service_tenant_id}"
}

rbac3_jdbc_url() {
  database_table_exists "${rbac3_database}" public.rbac3_tenant_authorization_state \
    || fail "Complete the explicit Tianquan-Jianshen tenant-authority migration before local startup"
  printf 'jdbc:postgresql://%s:%s/%s' "${postgres_host}" "${postgres_port}" "${rbac3_database}"
}



reconcile_local_tianquan_jianshen_tianshu_catalog() {
  local definition application_code ddc_app_code ddc_business_code
  local catalog_ids ddc_application_id ddc_business_id application_count access_count
  local definitions=(
    'tianquan-jianshen-admin|tianquan-jianshen|permission'
    'tianquan-shoubing-admin|tianquan-shoubing|permission'
    'yuheng-admin|yuheng-admin|platform'
    'tianshu-admin|tianshu|platform'
    'mock-backend|mock-backend|identity'
  )
  for definition in "${definitions[@]}"; do
    IFS='|' read -r application_code ddc_app_code ddc_business_code \
      <<<"${definition}"
    [[ "${application_code}" =~ ^[a-z0-9-]{1,64}$ \
        && "${ddc_app_code}" =~ ^[a-z0-9-]{1,64}$ \
        && "${ddc_business_code}" =~ ^[a-z0-9-]{1,64}$ ]] \
      || fail "unsafe local Tianshu catalog mapping"
    catalog_ids="$(psql_command "${ddc_database}" -AtF '|' -c \
      "select application.id, business.id
         from ddc_app application
         join ddc_biz business on business.biz_code = application.biz_code
        where application.app_code = '${ddc_app_code}'
          and business.biz_code = '${ddc_business_code}'
          and application.enabled
          and business.enabled")"
    [[ "${catalog_ids}" =~ ^[A-Za-z0-9_-]{1,64}\|[A-Za-z0-9_-]{1,64}$ ]] \
      || fail "Tianshu catalog mapping is unavailable for ${application_code}"
    ddc_application_id="${catalog_ids%%|*}"
    ddc_business_id="${catalog_ids#*|}"
    psql_command "${rbac3_database}" -qc \
      "update rbac3_application
          set ddc_application_id = '${ddc_application_id}',
              ddc_business_id = '${ddc_business_id}',
              updated_at = current_timestamp,
              updated_by = 'local-tianshu-reconciliation'
        where application_code = '${application_code}'
          and created_by = 'flyway-v10';
       update rbac3_user_business_access
          set ddc_business_id = '${ddc_business_id}',
              updated_at = current_timestamp,
              updated_by = 'local-tianshu-reconciliation'
        where source_type = 'SYSTEM'
          and source_id = 'flyway-v11:${ddc_business_code}';"
    application_count="$(psql_command "${rbac3_database}" -Atqc \
      "select count(*) from rbac3_application
        where application_code = '${application_code}'
          and ddc_application_id = '${ddc_application_id}'
          and ddc_business_id = '${ddc_business_id}'")"
    [[ "${application_count}" == "1" ]] \
      || fail "Tianquan-Jianshen application mapping failed for ${application_code}"
    access_count="$(psql_command "${rbac3_database}" -Atqc \
      "select count(*) from rbac3_user_business_access
        where source_type = 'SYSTEM'
          and source_id = 'flyway-v11:${ddc_business_code}'
          and ddc_business_id = '${ddc_business_id}'")"
    [[ "${access_count}" =~ ^[1-9][0-9]*$ ]] \
      || fail "Tianquan-Jianshen Business access mapping failed for ${ddc_business_code}"
  done
}


















clear_local_rbac3_snapshots() {
  local password pattern key tenant
  password="$(<"${secret_dir}/redis.password")"
  for tenant in "${service_tenant_id}" "${tenant_b_id}"; do
    [[ "${tenant}" =~ ^[1-9][0-9]*$ ]] || continue
    pattern="tianquan-jianshen:{${tenant}}:snapshot:*"
    while IFS= read -r key; do
      [[ -n "${key}" ]] || continue
      REDISCLI_AUTH="${password}" redis-cli -h "${redis_host}" \
        -p "${redis_port}" -n 8 UNLINK "${key}" >/dev/null
    done < <(REDISCLI_AUTH="${password}" redis-cli -h "${redis_host}" \
      -p "${redis_port}" -n 8 --scan --pattern "${pattern}")
  done
}

command_issue_user_token() {
  local tenant="${UNIFIED_IDENTITY_TENANT:-}" output="${UNIFIED_IDENTITY_ACCESS_TOKEN_FILE:-}" token temporary directory metadata
  [[ -n "${tenant}" && "${output}" == /* && ! -L "${output}" ]] \
    || fail "An explicit tenant and safe absolute protected output are required"
  directory="$(dirname "${output}")"
  [[ -d "${directory}" && ! -L "${directory}" ]] || fail "USER output requires an existing private directory"
  if [[ "$(uname -s)" == Darwin ]]; then
    metadata="$(stat -f '%u:%Lp' "${directory}")"
  else
    metadata="$(stat -c '%u:%a' "${directory}")"
  fi
  [[ "${metadata}" == "$(id -u):700" ]] || fail "USER output directory must be owner-only"
  if [[ -e "${output}" ]]; then
    protected_oauth_file "${output}" 16384
  fi
  token="$(user_access_token_for_tenant "${tenant}")"
  [[ ! "${output}" -ef "$(jq -er --arg tenant "${tenant}" '.tenants[$tenant].accessTokenFile' <<<"${oauth_registry_json}")" ]] \
    || fail "Formal USER source material must not be overwritten"
  temporary="$(mktemp "$(dirname "${output}")/user-input.XXXXXX")"
  chmod 600 "${temporary}"
  printf '%s' "${token}" >"${temporary}"
  mv -f "${temporary}" "${output}"
}

command_write_service_registration() {
  local file="${UNIFIED_IDENTITY_REGISTRATION_ENV_FILE:-}" client_id="${UNIFIED_IDENTITY_REGISTRATION_CLIENT_ID:-}"
  [[ "${file}" == "${env_dir}/"*.env && -n "${client_id}" ]] \
    || fail "An explicit managed env file and SERVICE client are required"
  [[ "$(cd -P "$(dirname "${file}")" && pwd)" == "$(cd -P "${env_dir}" && pwd)" ]] \
    || fail "SERVICE env output must stay in the managed env directory"
  protected_oauth_file "${file}" 131072
  [[ ! -L "${file%.env}.properties" ]] || fail "SERVICE properties output must not be a symlink"
  if [[ -e "${file%.env}.properties" ]]; then
    protected_oauth_file "${file%.env}.properties" 131072
  fi
  write_local_oauth_registration_env "${file}" "${client_id}"
  chmod 600 "${file%.env}.properties"
}

command_oauth_context() {
  local tenant="${UNIFIED_IDENTITY_TENANT:-default}"
  user_access_token_for_tenant "${tenant}" >/dev/null
  jq -c --arg tenant "${tenant}" \
    '{tenantId:.tenants[$tenant].tenantId,subject:.identitySub,
      issuer:.issuers[.tenants[$tenant].issuerRef].issuer}' <<<"${oauth_registry_json}"
}

activate_roles() {
  local access_token="$1" include_mock="$2" candidates current role_ids version request status
  [[ -n "${access_token}" ]] || fail "USER Access Token is required for role activation"
  status="$(curl -sS -o "${runtime_dir}/activation-candidates.response" \
    -w '%{http_code}' -H "Authorization: Bearer ${access_token}" \
    "${rbac3_url}/api/tianquan-jianshen/v1/auth/role-activation-candidates")"
  [[ "${status}" == "200" ]] || fail \
    "Tianquan-Jianshen activation candidates failed with HTTP ${status}: $(<"${runtime_dir}/activation-candidates.response")"
  candidates="$(<"${runtime_dir}/activation-candidates.response")"
  status="$(curl -sS -o "${runtime_dir}/role-activations.response" \
    -w '%{http_code}' -H "Authorization: Bearer ${access_token}" \
    "${rbac3_url}/api/tianquan-jianshen/v1/auth/role-activations")"
  [[ "${status}" == "200" ]] || fail \
    "Tianquan-Jianshen current activation failed with HTTP ${status}: $(<"${runtime_dir}/role-activations.response")"
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
    fail "Tianquan-Jianshen returned no activation candidates or active roles"
  fi
  version="$(jq -er '.data.authVersion' <<<"${current}")"
  request="$(jq -cn --argjson roles "${role_ids}" --argjson version "${version}" \
    '{roleIds:$roles,expectedAuthVersion:$version}')"
  status="$(curl -sS -o "${runtime_dir}/role-activation-update.response" \
    -w '%{http_code}' -X PUT -H 'Content-Type: application/json' \
    -H "Authorization: Bearer ${access_token}" -d "${request}" \
    "${rbac3_url}/api/tianquan-jianshen/v1/auth/role-activations")"
  [[ "${status}" == "200" ]] || fail \
    "Tianquan-Jianshen role activation failed with HTTP ${status}: $(<"${runtime_dir}/role-activation-update.response")"
}

gateway_api() {
  local method="$1" path="$2" body="${3:-}" idempotency_key="${4:-}"
  local response_file status response
  local arguments=(-sS -X "${method}" \
    -H "Authorization: Bearer $(<"${secret_dir}/yuheng-admin-control-plane.service.jwt")" \
    -H 'Content-Type: application/json')
  if [[ -n "${idempotency_key}" ]]; then
    arguments+=(-H "Idempotency-Key: ${idempotency_key}")
  fi
  if [[ -n "${body}" ]]; then
    arguments+=(-d "${body}")
  fi
  response_file="$(mktemp "${runtime_dir}/yuheng-api.XXXXXX")"
  status="$(curl "${arguments[@]}" -o "${response_file}" -w '%{http_code}' \
    "${gateway_admin_url}${path}")"
  response="$(<"${response_file}")"
  rm -f "${response_file}"
  [[ "${status}" =~ ^2[0-9][0-9]$ ]] || fail \
    "Yuheng Admin ${method} ${path} failed with HTTP ${status}: ${response}"
  printf '%s' "${response}"
}

ddc_api() {
  local method="$1" path="$2" body="${3:-}"
  local response_file status response
  [[ -n "${ddc_admin_access_token}" ]] \
    || fail "USER Access Token is required for Tianshu Admin bootstrap"
  local arguments=(-sS -X "${method}" \
    -H "Authorization: Bearer ${ddc_admin_access_token}" \
    -H 'Content-Type: application/json')
  if [[ -n "${body}" ]]; then
    arguments+=(-d "${body}")
  fi
  response_file="$(mktemp "${runtime_dir}/tianshu-api.XXXXXX")"
  status="$(curl "${arguments[@]}" -o "${response_file}" -w '%{http_code}' \
    "${ddc_url}${path}")"
  response="$(<"${response_file}")"
  rm -f "${response_file}"
  [[ "${status}" =~ ^2[0-9][0-9]$ ]] || fail \
    "Tianshu Admin ${method} ${path} failed with HTTP ${status}: ${response}"
  printf '%s' "${response}"
}

initialize_ddc_topology() {
  local access_token="$1" response biz_code biz_name app_code
  ddc_admin_access_token="${access_token}"
  response="$(ddc_api GET '/api/v1/tianshu/envs?keyword=local')"
  if ! jq -e '.data[] | select(.envCode == "local")' \
      <<<"${response}" >/dev/null; then
    ddc_api POST /api/v1/tianshu/envs \
      '{"envCode":"local","description":"Host-local development","sortOrder":0,"enabled":true}' \
      >/dev/null
  fi

  while read -r biz_code biz_name; do
    response="$(ddc_api GET "/api/v1/tianshu/bizs?keyword=${biz_code}")"
    if ! jq -e --arg biz "${biz_code}" \
        '.data[] | select(.bizCode == $biz)' <<<"${response}" >/dev/null; then
      ddc_api POST /api/v1/tianshu/bizs \
        "$(jq -cn --arg biz "${biz_code}" --arg name "${biz_name}" \
          '{bizCode:$biz,bizName:$name,description:"Host-local OAuth2 resource topology",enabled:true}')" \
        >/dev/null
    fi
    response="$(ddc_api GET "/api/v1/tianshu/namespaces?bizCode=${biz_code}&keyword=default")"
    if ! jq -e --arg biz "${biz_code}" \
        '.data[] | select(.bizCode == $biz and .namespaceCode == "default")' \
        <<<"${response}" >/dev/null; then
      ddc_api POST /api/v1/tianshu/namespaces \
        "$(jq -cn --arg biz "${biz_code}" \
          '{bizCode:$biz,namespaceCode:"default",namespace:"Default",description:"Host-local OAuth2 resource topology",enabled:true}')" \
        >/dev/null
    fi
  done <<'BUSINESSES'
permission Permission
xingyuan Xingyuan
identity Identity
BUSINESSES

  while read -r biz_code app_code; do
    response="$(ddc_api GET "/api/v1/tianshu/apps?bizCode=${biz_code}&keyword=${app_code}")"
    if ! jq -e --arg app "${app_code}" \
        --arg biz "${biz_code}" \
        '.data[] | select(.bizCode == $biz and .appCode == $app)' \
        <<<"${response}" >/dev/null; then
      ddc_api POST /api/v1/tianshu/apps \
        "$(jq -cn --arg biz "${biz_code}" --arg app "${app_code}" \
          '{bizCode:$biz,appCode:$app,appName:$app,owner:"xingyuan",description:"Host-local OAuth2 resource topology",enabled:true}')" \
        >/dev/null
    fi
    response="$(ddc_api GET "/api/v1/tianshu/namespace-env-app-bindings?bizCode=${biz_code}&namespaceCode=default&env=local&appCode=${app_code}")"
    if ! jq -e '.data[] | select(.enabled == true)' \
        <<<"${response}" >/dev/null; then
      ddc_api POST /api/v1/tianshu/namespace-env-app-bindings \
        "$(jq -cn --arg biz "${biz_code}" --arg app "${app_code}" \
          '{bizCode:$biz,namespaceCode:"default",env:"local",appCode:$app,enabled:true}')" \
        >/dev/null
    fi
  done <<'APPLICATIONS'
permission tianquan-shoubing
permission tianquan-jianshen
xingyuan tianshu
xingyuan yuheng-admin
identity mock-backend
identity yuheng-biz-gateway-default
identity yuheng-mcp-gateway-default
identity yuheng-test-mcp-provider
APPLICATIONS
}

wait_ddc_provider_registration() {
  local biz_code="$1" app_code="$2" service_name="$3" response
  for ((attempt = 1; attempt <= 30; attempt++)); do
    response="$(ddc_api GET \
      "/api/v1/tianshu/registry/services?bizCode=${biz_code}&namespaceCode=default&env=local&appCode=${app_code}&serviceKind=HTTP_PROVIDER&protocol=http&serviceName=${service_name}&group=default")"
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
  fail "${biz_code}/${app_code} did not register an online Tianshu HTTP Provider lease"
}

wait_ddc_rpc_provider_registration() {
  local biz_code="$1" app_code="$2" service_name="$3" group="$4" version="$5" response
  for ((attempt = 1; attempt <= 30; attempt++)); do
    response="$(ddc_api GET \
      "/api/v1/tianshu/registry/services?bizCode=${biz_code}&namespaceCode=default&env=local&appCode=${app_code}&serviceKind=RPC_PROVIDER&protocol=grpc&serviceName=${service_name}&group=${group}&version=${version}")"
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
  fail "${biz_code}/${app_code}/${service_name} did not register an online Tianshu RPC Provider lease"
}

gateway_application_id_file() {
  case "$1" in
    tianquan-shoubing|tianquan-jianshen|yuheng-admin|tianshu|mock-backend) ;;
    *) fail "unsupported Yuheng reporting application: $1" ;;
  esac
  printf '%s/yuheng-application.%s.id' "${runtime_dir}" "$1"
}

gateway_report_access_key_file() {
  printf '%s/yuheng-report-%s.access-key' "${secret_dir}" "$1"
}

gateway_report_secret_file() {
  printf '%s/yuheng-report-%s.secret' "${secret_dir}" "$1"
}

gateway_report_master_key_fingerprint_file() {
  printf '%s/.yuheng-report-%s.master-key.sha256' "${secret_dir}" "$1"
}

gateway_master_key_fingerprint() {
  openssl dgst -sha256 -r "${secret_dir}/yuheng-master-key.base64" \
    | awk '{print $1}'
}

gateway_reporting_credential_is_active() {
  local app_id="$1" access_file="$2" access_key credentials
  [[ -s "${access_file}" ]] || return 1
  access_key="$(<"${access_file}")"
  credentials="$(gateway_api GET \
    "/api/v1/yuheng/admin/applications/${app_id}/credentials")"
  jq -e --arg access_key "${access_key}" \
    'any(.[]; .accessKey == $access_key and .status == "ACTIVE")' \
    <<<"${credentials}" >/dev/null
}

configure_gateway_reporter() {
  local app_code="$1" access_file secret_file env_file enabled_key
  access_file="$(gateway_report_access_key_file "${app_code}")"
  secret_file="$(gateway_report_secret_file "${app_code}")"
  case "${app_code}" in
    tianquan-shoubing) env_file="${env_dir}/tianquan-shoubing.env"; enabled_key=TIANQUAN_SHOUBING_YUHENG_REPORTING_ENABLED ;;
    tianquan-jianshen) env_file="${env_dir}/tianquan-jianshen.env"; enabled_key=TIANQUAN_JIANSHEN_YUHENG_REPORTING_ENABLED ;;
    yuheng-admin) env_file="${env_dir}/yuheng-admin.env"; enabled_key=YUHENG_ADMIN_YUHENG_REPORTING_ENABLED ;;
    tianshu) env_file="${env_dir}/tianshu.env"; enabled_key=TIANSHU_YUHENG_REPORTING_ENABLED ;;
    mock-backend) env_file="${env_dir}/mock-backend.env"; enabled_key=MOCK_BACKEND_YUHENG_REPORTING_ENABLED ;;
    *) fail "unsupported Yuheng reporting application: ${app_code}" ;;
  esac
  write_env "${env_file}" YUHENG_REPORT_ACCESS_KEY "$(<"${access_file}")"
  write_env "${env_file}" YUHENG_REPORT_SECRET_KEY "$(<"${secret_file}")"
  write_env "${env_file}" "${enabled_key}" true
}

ensure_gateway_reporting_application() {
  local biz_code="$1" app_code="$2" display_name="$3"
  local app_id credential access_file secret_file fingerprint marker_file
  ensure_gateway_application "${biz_code}" "${app_code}" "${display_name}"
  app_id="$(<"$(gateway_application_id_file "${app_code}")")"
  access_file="$(gateway_report_access_key_file "${app_code}")"
  secret_file="$(gateway_report_secret_file "${app_code}")"
  marker_file="$(gateway_report_master_key_fingerprint_file "${app_code}")"
  fingerprint="$(gateway_master_key_fingerprint)"
  if [[ ! -s "${access_file}" || ! -s "${secret_file}" \
      || ! -s "${marker_file}" \
      || "$(<"${marker_file}")" != "${fingerprint}" ]] \
      || ! gateway_reporting_credential_is_active "${app_id}" "${access_file}"; then
    credential="$(gateway_api POST "/api/v1/yuheng/admin/applications/${app_id}/credentials" '{}')"
    jq -er '.accessKey' <<<"${credential}" >"${access_file}"
    jq -er '.secret' <<<"${credential}" >"${secret_file}"
    printf '%s' "${fingerprint}" >"${marker_file}"
    chmod 600 "${access_file}" "${secret_file}" "${marker_file}"
  fi
  printf '%s' "${app_id}" >"$(gateway_application_id_file "${app_code}")"
  chmod 600 "$(gateway_application_id_file "${app_code}")"
  configure_gateway_reporter "${app_code}"
}

ensure_gateway_application() {
  local biz_code="$1" app_code="$2" display_name="$3"
  local applications application app_id
  applications="$(gateway_api GET "/api/v1/yuheng/admin/applications?bizCode=${biz_code}&namespace=default&env=local&appCode=${app_code}")"
  app_id="$(jq -r --arg app "${app_code}" \
    '.[] | select(.applicationCode == $app) | .id' <<<"${applications}" | head -1)"
  if [[ -z "${app_id}" ]]; then
    application="$(gateway_api POST /api/v1/yuheng/admin/applications \
      "$(jq -cn --arg biz "${biz_code}" --arg app "${app_code}" \
        --arg display "${display_name}" \
        '{bizCode:$biz,applicationCode:$app,displayName:$display,env:"local",namespace:"default",description:"Host-local unified identity Yuheng catalog provider"}')")"
    app_id="$(jq -er '.id' <<<"${application}")"
  fi
  printf '%s' "${app_id}" >"$(gateway_application_id_file "${app_code}")"
  chmod 600 "$(gateway_application_id_file "${app_code}")"
}

initialize_gateway_control_plane() {
  local groups group group_id
  ensure_gateway_application permission tianquan-shoubing "Tianquan-Shoubing Identity Admin"
  ensure_gateway_application permission tianquan-jianshen "Tianquan-Jianshen Permission Admin"
  ensure_gateway_application xingyuan yuheng-admin "Yuheng Admin"
  ensure_gateway_application xingyuan tianshu "Tianshu Admin"
  if [[ "${startup_mode}" == "full" ]]; then
    ensure_gateway_reporting_application identity mock-backend "Unified Identity Mock Backend"
  else
    ensure_gateway_application identity mock-backend "Unified Identity Mock Backend"
  fi

  groups="$(gateway_api GET '/api/v1/yuheng/admin/yuheng-groups?env=local&namespace=default')"
  group_id="$(jq -r '.[] | select(.gatewayGroupCode == "default") | .id' <<<"${groups}" | head -1)"
  if [[ -z "${group_id}" ]]; then
    group="$(gateway_api POST /api/v1/yuheng/admin/yuheng-groups \
      '{"gatewayGroupCode":"default","displayName":"Unified Identity Local Yuheng","env":"local","namespace":"default","description":"Host-local unified identity Yuheng route group"}')"
    group_id="$(jq -er '.id' <<<"${group}")"
  fi
  printf '%s' "${group_id}" >"${runtime_dir}/yuheng-group.id"
  printf '%s' "$(<"$(gateway_application_id_file mock-backend)")" \
    >"${runtime_dir}/yuheng-application.id"
  chmod 600 "${runtime_dir}/yuheng-group.id" "${runtime_dir}/yuheng-application.id"
}

wait_gateway_catalog_for_app() {
  local app_code="$1" app_id response
  app_id="$(<"$(gateway_application_id_file "${app_code}")")"
  for ((attempt = 1; attempt <= 60; attempt++)); do
    response="$(gateway_api GET \
      "/api/v1/yuheng/admin/applications/${app_id}/catalog" || true)"
    if jq -e '.. | objects | select(.protocol? == "HTTP" and .lifecycleStatus? == "ACTIVE")' \
        <<<"${response}" >/dev/null 2>&1; then
      if [[ "${app_code}" == "yuheng-admin" ]] && ! jq -e '
        [.. | objects | select(.protocol? == "HTTP" and .lifecycleStatus? == "ACTIVE")
          | .methodIdentity] as $methods
        | ["GET /api/v1/yuheng/admin/openapi/sync-states",
           "GET /api/v1/yuheng/admin/operations/{operationId}/openapi",
           "GET /api/v1/yuheng/admin/openapi/snapshots/{snapshotId}/document"]
        | all(. as $method | $methods | index($method) != null)
      ' <<<"${response}" >/dev/null 2>&1; then
        sleep 1
        continue
      fi
      return
    fi
    sleep 1
  done
  fail "${app_code} Yuheng catalog did not become active"
}

wait_gateway_catalog() {
  local app_code
  for app_code in tianquan-shoubing tianquan-jianshen yuheng-admin tianshu mock-backend; do
    wait_gateway_catalog_for_app "${app_code}"
  done
}

wait_gateway_engine_provider_registration() {
  local response
  for ((attempt = 1; attempt <= 60; attempt++)); do
    response="$(gateway_api GET \
      '/api/v1/yuheng/admin/providers/instances?bizCode=identity&appCode=yuheng-biz-gateway-default&env=local&namespace=default' \
      || true)"
    if jq -e '
        .value[]?
        | select(.status == "ONLINE" and .instanceId == "yuheng-biz-gateway-local-1")
      ' <<<"${response}" >/dev/null 2>&1; then
      return
    fi
    sleep 1
  done
  fail "Yuheng Engine did not publish an online Tianshu provider registration: ${response}"
}

wait_gateway_openapi_sync_for_app() {
  local biz_code="$1" app_code="$2" response build_id
  case "${app_code}" in
    tianquan-shoubing) build_id="$(local_build_id "${idp_jar}")" ;;
    tianquan-jianshen) build_id="$(local_build_id "${rbac3_jar}")" ;;
    tianshu) build_id="$(local_build_id "${ddc_jar}")" ;;
    yuheng-admin) build_id="$(local_build_id "${gateway_admin_jar}")" ;;
    *) fail "unknown local OpenAPI provider: ${app_code}" ;;
  esac
  for ((attempt = 1; attempt <= 60; attempt++)); do
    response="$(gateway_api GET \
      "/api/v1/yuheng/admin/openapi/sync-states?bizCode=${biz_code}&namespace=default&env=local&appCode=${app_code}" \
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
  fail "${biz_code}/${app_code} Yuheng OpenAPI sync did not become valid: ${response}"
}

gateway_catalog_operations() {
  local app_code app_id catalog part_file combined_file
  combined_file="${runtime_dir}/yuheng-catalog-operations.jsonl"
  : >"${combined_file}"
  for app_code in tianquan-shoubing tianquan-jianshen yuheng-admin tianshu mock-backend; do
    app_id="$(<"$(gateway_application_id_file "${app_code}")")"
    catalog="$(gateway_api GET \
      "/api/v1/yuheng/admin/applications/${app_id}/catalog")"
    part_file="$(mktemp "${runtime_dir}/yuheng-catalog.XXXXXX")"
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
    "/api/v1/yuheng/admin/mcp/servers?gatewayGroupId=${group_id}")"
  while IFS= read -r server_id; do
    [[ -n "${server_id}" ]] || continue
    managed_tools="$(gateway_api GET \
      "/api/v1/yuheng/admin/mcp/groups/${group_id}/managed-tools?serverId=${server_id}")"
    valid_tools="$(jq -c '[.[] | select(.enabled == true) | .name] | unique' \
      <<<"${managed_tools}")"

    task_policies="$(gateway_api GET \
      "/api/v1/yuheng/admin/mcp/servers/${server_id}/task-policies?gatewayGroupId=${group_id}")"
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
        "/api/v1/yuheng/admin/yuheng-groups/${group_id}/draft" \
        | jq -er '.revision')"
      response="$(gateway_api DELETE \
        "/api/v1/yuheng/admin/mcp/task-policies/${policy_id}" \
        "$(jq -cn --arg group "${group_id}" --argjson expected "${policy_revision}" \
          --argjson draft "${revision}" \
          '{gatewayGroupId:$group,expectedRevision:$expected,expectedDraftRevision:$draft,changeReason:"Remove platform-mode MCP task policy without a valid Tool"}')" \
        "unified-xingyuan-remove-mcp-task-${policy_id}")"
      jq -e '.resourceId != null' <<<"${response}" >/dev/null
    done < <(jq -c '.[]' <<<"${task_policies}")

    app_bindings="$(gateway_api GET \
      "/api/v1/yuheng/admin/mcp/servers/${server_id}/app-bindings?gatewayGroupId=${group_id}")"
    while IFS= read -r binding; do
      binding_id="$(jq -er '.id' <<<"${binding}")"
      binding_valid="$(jq -e --argjson tools "${valid_tools}" '
        ((.content.allowedTools // []) - $tools | length) == 0
      ' <<<"${binding}" >/dev/null 2>&1; echo $?)"
      [[ "${binding_valid}" == "0" ]] && continue
      binding_revision="$(jq -er '.revision' <<<"${binding}")"
      revision="$(gateway_api GET \
        "/api/v1/yuheng/admin/yuheng-groups/${group_id}/draft" \
        | jq -er '.revision')"
      response="$(gateway_api DELETE \
        "/api/v1/yuheng/admin/mcp/app-bindings/${binding_id}" \
        "$(jq -cn --arg group "${group_id}" --argjson expected "${binding_revision}" \
          --argjson draft "${revision}" \
          '{gatewayGroupId:$group,expectedRevision:$expected,expectedDraftRevision:$draft,changeReason:"Remove platform-mode MCP app binding with invalid Tool references"}')" \
        "unified-xingyuan-remove-mcp-app-${binding_id}")"
      jq -e '.resourceId != null' <<<"${response}" >/dev/null
    done < <(jq -c '.[]' <<<"${app_bindings}")
  done < <(jq -r '.[].id' <<<"${servers}")
}

route_id_for_operation() {
  printf '%s' "$1" | openssl dgst -sha256 -r \
    | awk '{print "unified-" substr($1, 1, 32)}'
}

select_gateway_catalog_operations() {
  jq '
    map(select(.protocol == "HTTP" and .externalAccessible == true
      and .lifecycleStatus == "ACTIVE"))
    | map(select(.methodIdentity != "GET /api/v1/auth/bootstrap"
      or .reportedApplication == "yuheng-admin"))
    | group_by([.reportedApplication, (.methodIdentity | split("/")
        | map(if startswith("{") and endswith("}") then "{}" else . end)
        | join("/"))])
    | map(if any(.[]; .sourceType == "OPENAPI31")
        then map(select(.sourceType != "STARTER")) else . end)
    | (add // [])
    | map(select((.methodIdentity | split(" ")[1]
        | test("^/(oauth2/|\\.well-known/|tenants/|platform/|internal/v1/oauth2/refresh-token/)")) | not))
    | map(. + {securityType:
      (if .reportedApplication == "tianquan-jianshen" and (
        .methodIdentity == "GET /api/v1/auth/about"
        or .methodIdentity == "GET /api/tianquan-jianshen/v1/auth/role-activation-candidates"
        or .methodIdentity == "GET /api/tianquan-jianshen/v1/auth/role-activations"
        or .methodIdentity == "PUT /api/tianquan-jianshen/v1/auth/role-activations")
       then "IDENTITY_PROTECTED"
       else "BUSINESS_PROTECTED" end)})'
}

publish_gateway_routes() {
  local defer_release="${1:-false}"
  local group_id operations draft revision response validation release
  local security ids policy_id route_type auth_mode forward recovery
  local extractors auth_providers authz_providers operation_id method_identity app_code
  local method path route_id legacy_route_id stale_route_id route_content desired_policy
  local managed_operation_ids cors_policy_id cors_origins cors_methods
  local cors_headers cors_exposed security_type route_transport_policy
  group_id="$(<"${runtime_dir}/yuheng-group.id")"
  operations="$(gateway_catalog_operations | select_gateway_catalog_operations)"
  printf '%s' "${operations}" >"${runtime_dir}/yuheng-operations.json"
  draft="$(gateway_api GET "/api/v1/yuheng/admin/yuheng-groups/${group_id}/draft")"
  revision="$(jq -er '.revision' <<<"${draft}")"
  managed_operation_ids="$(jq '[.[].id]' <<<"${operations}")"

  while IFS= read -r stale_route_id; do
    [[ -n "${stale_route_id}" ]] || continue
    response="$(gateway_api DELETE \
      "/api/v1/yuheng/admin/yuheng-groups/${group_id}/draft/routes/${stale_route_id}" \
      "$(jq -cn --argjson revision "${revision}" \
        --arg route "${stale_route_id}" \
        '{expectedRevision:$revision,idempotencyKey:("unified-remove-stale-route-" + $route + "-" + ($revision | tostring)),changeReason:"Remove a deterministic route whose operation is no longer selected for the unified local topology"}')")"
    revision="$(jq -er '.revision' <<<"${response}")"
    draft="$(gateway_api GET "/api/v1/yuheng/admin/yuheng-groups/${group_id}/draft")"
  done < <(jq -r --argjson operations "${managed_operation_ids}" '
    .routes[]?
    | select(([.operationId] - $operations | length) > 0
      and ((.routeId | startswith("unified-")) or .enabled != true))
    | .routeId' <<<"${draft}")

  if jq -e '.policies[]? | select(.policyId == "identity-basic" and .policyScope == "GLOBAL")' \
      <<<"${draft}" >/dev/null; then
    response="$(gateway_api DELETE \
      "/api/v1/yuheng/admin/yuheng-groups/${group_id}/draft/policies/identity-basic" \
      "$(jq -cn --argjson revision "${revision}" \
        '{expectedRevision:$revision,idempotencyKey:("unified-remove-identity-basic-" + ($revision | tostring)),changeReason:"Replace legacy global policy with operation-scoped stateless policies"}')")"
    revision="$(jq -er '.revision' <<<"${response}")"
    draft="$(gateway_api GET "/api/v1/yuheng/admin/yuheng-groups/${group_id}/draft")"
  fi

  cors_policy_id=unified-local-cors
  cors_origins='["http://127.0.0.1:18121","http://127.0.0.1:18125","http://127.0.0.1:18131","http://127.0.0.1:18141","http://127.0.0.1:18152"]'
  cors_methods='["GET","POST","PUT","PATCH","DELETE","OPTIONS"]'
  cors_headers='["Authorization","Content-Type","X-TIANQUAN-SHOUBING-CSRF","X-CSRF-TOKEN","Idempotency-Key","X-Yuheng-Contract-Version","traceparent","x-egon-request-id"]'
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
      "/api/v1/yuheng/admin/yuheng-groups/${group_id}/draft/policies/${cors_policy_id}" \
      "${desired_policy}")"
    revision="$(jq -er '.revision' <<<"${response}")"
    draft="$(gateway_api GET "/api/v1/yuheng/admin/yuheng-groups/${group_id}/draft")"
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
        route_type=IDENTITY_PROTECTED; auth_mode=REQUIRED; forward=ORIGINAL_BEARER; recovery='"tianquan-shoubing-user-refresh"'
        extractors='["tianquan-shoubing-user-cookie"]'; auth_providers='["tianquan-shoubing-jwt"]'; authz_providers='[]' ;;
      BUSINESS_PROTECTED)
        policy_id=unified-business-protected
        route_type=BUSINESS_PROTECTED; auth_mode=REQUIRED; forward=ORIGINAL_BEARER; recovery='"tianquan-shoubing-user-refresh"'
        extractors='["tianquan-shoubing-user-cookie"]'; auth_providers='["tianquan-shoubing-jwt"]'; authz_providers='["tianquan-jianshen-biz-app-scope"]' ;;
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
        '{policyType:"SECURITY",policyScope:"OPERATION",content:{operationIds:$ids,routeSecurityType:$routeType,authenticationMode:$authMode,credentialExtractorIds:$extractors,authenticationProviderIds:$authProviders,authorizationProviderIds:$authzProviders,credentialRecoveryProviderId:$recovery,decisionMode:"ALL_ALLOW",providerTimeoutMs:1000,failureMode:"FAIL_CLOSED",credentialForwardingMode:$forward},enabled:true,expectedRevision:$revision,idempotencyKey:("unified-policy-" + $policy + "-" + ($revision | tostring)),changeReason:"Publish operation-scoped stateless identity and Tianquan-Jianshen Yuheng policies"}')"
      response="$(gateway_api PUT \
        "/api/v1/yuheng/admin/yuheng-groups/${group_id}/draft/policies/${policy_id}" \
        "${desired_policy}")"
      revision="$(jq -er '.revision' <<<"${response}")"
      draft="$(gateway_api GET "/api/v1/yuheng/admin/yuheng-groups/${group_id}/draft")"
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
        "/api/v1/yuheng/admin/yuheng-groups/${group_id}/draft/routes/${legacy_route_id}" \
        "$(jq -cn --argjson revision "${revision}" \
          --arg route "${legacy_route_id}" \
          '{expectedRevision:$revision,idempotencyKey:("unified-remove-legacy-route-" + $route + "-" + ($revision | tostring)),changeReason:"Remove a legacy route duplicated by the deterministic reported-operation route"}')")"
      revision="$(jq -er '.revision' <<<"${response}")"
      draft="$(gateway_api GET "/api/v1/yuheng/admin/yuheng-groups/${group_id}/draft")"
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
      "/api/v1/yuheng/admin/yuheng-groups/${group_id}/draft/routes/${route_id}" \
      "$(jq -cn --argjson route "${route_content}" --argjson revision "${revision}" \
        --arg operation "${operation_id}" \
        '$route + {expectedRevision:$revision,idempotencyKey:("unified-route-" + $operation + "-" + ($revision | tostring)),changeReason:"Publish reported HTTP operation from the real provider catalog"}')")"
    revision="$(jq -er '.revision' <<<"${response}")"
    draft="$(gateway_api GET "/api/v1/yuheng/admin/yuheng-groups/${group_id}/draft")"
  done < <(jq -r '.[] | [.id,.methodIdentity,.reportedApplication,.securityType] | @tsv' \
    <<<"${operations}")

  if [[ "${defer_release}" == "true" ]]; then
    return
  fi
  validation="$(gateway_api POST "/api/v1/yuheng/admin/yuheng-groups/${group_id}/draft/validate" '{}')"
  jq -e '.valid == true' <<<"${validation}" >/dev/null \
    || fail "Yuheng draft validation failed: ${validation}"
  release="$(gateway_api POST "/api/v1/yuheng/admin/yuheng-groups/${group_id}/releases" \
    "$(jq -cn --argjson revision "${revision}" \
      '{expectedDraftRevision:$revision,changeReason:"Unified identity real catalog route release"}')")"
  jq -e '.status == "SUCCESS"' <<<"${release}" >/dev/null \
    || fail "Yuheng release did not succeed: ${release}"
  jq -er '.releaseId' <<<"${release}" >"${runtime_dir}/yuheng-release.id"
  chmod 600 "${runtime_dir}/yuheng-release.id"
}

wait_gateway_route() {
  local status route="${gateway_url}/api/v1/tianquan-shoubing/me"
  [[ "${startup_mode}" != xingyuan ]] && route="${gateway_url}/api/mock/read"
  for ((attempt = 1; attempt <= 30; attempt++)); do
    status="$(curl --max-time 3 -sS -o /dev/null -w '%{http_code}' "${route}")"
    [[ "${status}" == 401 ]] && return
    sleep 1
  done
  fail "Yuheng Engine did not expose its current protected routes"
}

command_start() {
  load_local_oauth_registry
  command_prepare
  validate_current_oauth_directory
  case "${startup_mode}" in
    xingyuan|full) ;;
    *) fail "unsupported UNIFIED_IDENTITY_START_MODE: ${startup_mode} (use xingyuan or full)" ;;
  esac
  local subject rbac3_access_token ddc_access_token
  stage "starting the provisioned local authorization server without Tianshu publication"
  start_process tianquan-shoubing "${env_dir}/tianquan-shoubing.env" "${idp_jar}" \
    --egon.cola.component.tianshu.enabled=false \
    --egon.cola.component.tianshu.registry.enabled=false \
    --egon.cola.component.tianshu.registry.http.enabled=false
  wait_http tianquan-shoubing "${idp_url}/actuator/health/readiness"
  stage "starting Tianshu with the provisioned PLATFORM registration"
  start_process tianshu "${env_dir}/tianshu.env" "${ddc_jar}"
  wait_http tianshu "${ddc_url}/actuator/health/readiness"
  wait_ddc_rpc
  resolve_existing_service_tenant_id
  write_service_env_files
  write_application_build_ids
  stage "issuing Tianquan-Shoubing-owned service credentials"
  refresh_service_tokens
  # Tianshu starts before Tianquan-Shoubing can create/rotate machine credentials. Reload the
  # regenerated properties before its Admin API requests an Tianquan-Jianshen service token.
  stage "reloading Tianshu with initialized Tianquan-Shoubing client credentials"
  stop_process tianshu
  start_process tianshu "${env_dir}/tianshu.env" "${ddc_jar}"
  wait_http tianshu "${ddc_url}/actuator/health/readiness"
  wait_ddc_rpc
  subject="$(identity_subject)"
  [[ -n "${subject}" ]] || fail "Tianquan-Shoubing bootstrap subject is missing"

  stage "starting Tianquan-Jianshen bootstrap phase without Tianshu publication"
  write_env "${env_dir}/tianquan-jianshen.env" TIANQUAN_JIANSHEN_DEVELOPMENT_IDENTITY_SUB "${subject}"
  start_process tianquan-jianshen "${env_dir}/tianquan-jianshen.env" "${rbac3_jar}" \
    --egon.cola.component.tianshu.enabled=true \
    --egon.cola.component.tianshu.registry.enabled=false \
    --egon.cola.component.tianshu.registry.http.enabled=false \
    --egon.tianquan-jianshen.development-bootstrap.enabled=false
  wait_http tianquan-jianshen "${rbac3_url}/actuator/health/readiness"

  service_tenant_id="$(rbac3_tenant_id default)"
  tenant_b_id="$(rbac3_tenant_id tenant-b)"
  write_env "${env_dir}/tianquan-shoubing.env" TIANQUAN_SHOUBING_TIANQUAN_JIANSHEN_SERVICE_TENANT_ID \
    "${service_tenant_id}"
  write_env "${env_dir}/tianquan-shoubing.env" TIANQUAN_SHOUBING_DEVELOPMENT_TIANQUAN_JIANSHEN_SERVICE_TENANT_ID \
    "${service_tenant_id}"
  write_env "${env_dir}/tianquan-shoubing.env" TIANQUAN_SHOUBING_DEVELOPMENT_TIANQUAN_JIANSHEN_SERVICE_TENANT_IDS \
    "${service_tenant_id},${tenant_b_id}"
  write_env "${env_dir}/tianquan-jianshen.env" TIANQUAN_JIANSHEN_DEVELOPMENT_TENANT_IDS \
    "${service_tenant_id},${tenant_b_id}"
  stage "binding Tianquan-Shoubing service credentials to the Tianquan-Jianshen tenant"
  stop_process tianquan-shoubing
  start_process tianquan-shoubing "${env_dir}/tianquan-shoubing.env" "${idp_jar}" \
    --egon.cola.component.tianshu.enabled=false \
    --egon.cola.component.tianshu.registry.enabled=false \
    --egon.cola.component.tianshu.registry.http.enabled=false
  wait_http tianquan-shoubing "${idp_url}/actuator/health/readiness"
  refresh_service_tokens

  stage "clearing stale local Tianquan-Jianshen authorization snapshots"
  clear_local_rbac3_snapshots
  stage "validating the protected formal default-tenant USER input"
  stage "validating current USER authorization at its formal issuer"
  rbac3_access_token="$(user_access_token_for_tenant default)"
  stage "initializing Tianshu unified identity topology"
  ddc_access_token="$(user_access_token_for_tenant default)"
  initialize_ddc_topology "${ddc_access_token}"
  stage "reconciling SQL-seeded Tianquan-Jianshen applications with Tianshu catalog IDs"
  reconcile_local_tianquan_jianshen_tianshu_catalog
  stage "activating non-mock roles"
  activate_roles "${rbac3_access_token}" false

  stage "restarting Tianquan-Shoubing and Tianquan-Jianshen with admitted Tianshu publication"
  stop_process tianquan-jianshen
  stop_process tianquan-shoubing
  # Tianshu's runtime registration needs an online Tianquan-Shoubing. Restore Tianquan-Shoubing first without
  # requiring Tianquan-Jianshen publication, then enable Tianshu RPC and complete the chain.
  write_env "${env_dir}/tianquan-shoubing.env" TIANQUAN_SHOUBING_RPC_PROVIDER_REGISTRATION_MODE DISABLED
  start_process tianquan-shoubing "${env_dir}/tianquan-shoubing.env" "${idp_jar}" \
    --egon.cola.component.tianshu.enabled=false \
    --egon.cola.component.tianshu.registry.enabled=false \
    --egon.cola.component.tianshu.registry.http.enabled=false
  wait_http tianquan-shoubing "${idp_url}/actuator/health/readiness"
  refresh_service_tokens

  write_env "${env_dir}/tianshu.env" TIANSHU_SELF_REGISTRATION_ENABLED true
  write_env "${env_dir}/tianshu.env" EGON_COLA_COMPONENT_TIANSHU_ENABLED true
  write_env "${env_dir}/tianshu.env" EGON_COLA_COMPONENT_TIANSHU_REDIS_ENABLED true
  stop_process tianshu
  start_process tianshu "${env_dir}/tianshu.env" "${ddc_jar}"
  wait_http tianshu "${ddc_url}/actuator/health/readiness"
  wait_ddc_rpc
  write_env "${env_dir}/tianquan-jianshen.env" TIANQUAN_JIANSHEN_DEVELOPMENT_BOOTSTRAP_ENABLED true
  write_env "${env_dir}/tianquan-jianshen.env" TIANQUAN_JIANSHEN_RPC_ENABLED true
  write_env "${env_dir}/tianquan-jianshen.env" TIANQUAN_JIANSHEN_RPC_CONSUMER_ENABLED true
  write_env "${env_dir}/tianquan-shoubing.env" TIANQUAN_SHOUBING_RPC_PROVIDER_REGISTRATION_MODE REQUIRED
  stop_process tianquan-shoubing
  start_process tianquan-shoubing "${env_dir}/tianquan-shoubing.env" "${idp_jar}"
  wait_http tianquan-shoubing "${idp_url}/actuator/health/readiness"
  refresh_service_tokens
  stage "waiting for Tianquan-Shoubing identity RPC publication"
  start_process tianquan-jianshen "${env_dir}/tianquan-jianshen.env" "${rbac3_jar}" \
    --egon.tianquan-jianshen.development-bootstrap.enabled=false
  wait_http tianquan-jianshen "${rbac3_url}/actuator/health/readiness"
  stage "refreshing the USER token for Tianshu RPC registration polling"
  ddc_admin_access_token="$(user_access_token_for_tenant default)"
  wait_ddc_rpc_provider_registration permission tianquan-shoubing egon.tianquan.shoubing.v1.IdentityDirectoryService tianquan-shoubing 1.0.0
  stage "starting Tianquan-Jianshen topology bootstrap after Tianquan-Shoubing RPC publication"
  stop_process tianquan-jianshen
  start_process tianquan-jianshen "${env_dir}/tianquan-jianshen.env" "${rbac3_jar}"
  wait_http tianquan-jianshen "${rbac3_url}/actuator/health/readiness"

  stage "validating the operator USER authorization and activation context"
  rbac3_access_token="$(user_access_token_for_tenant default)"
  activate_roles "${rbac3_access_token}" false
  wait_ddc_provider_registration permission tianquan-shoubing tianquan-shoubing-admin
  wait_ddc_provider_registration permission tianquan-jianshen tianquan-jianshen-admin

  stage "starting Yuheng Admin"
  start_process yuheng-admin "${env_dir}/yuheng-admin.env" "${gateway_admin_jar}"
  wait_http yuheng-admin "${gateway_admin_url}/actuator/health/readiness"
  initialize_gateway_control_plane

  if [[ "${startup_mode}" == "xingyuan" ]]; then
    stage "waiting for Yuheng Admin OpenAPI catalog ingestion"
    wait_gateway_catalog_for_app yuheng-admin
    stage "waiting for Tianquan-Jianshen OpenAPI group ingestion"
    wait_gateway_openapi_sync_for_app permission tianquan-jianshen
    stage "waiting for Tianquan-Shoubing and Tianshu OpenAPI group ingestion"
    wait_gateway_openapi_sync_for_app permission tianquan-shoubing
    wait_gateway_openapi_sync_for_app xingyuan tianshu
    stage "reconciling orphaned local MCP draft capabilities"
    reconcile_platform_mcp_draft "$(<"${runtime_dir}/yuheng-group.id")"
    stage "preparing the current local Yuheng HTTP catalog draft"
    publish_gateway_routes true
    echo "Unified identity platform backends are running with a prepared Yuheng OpenAPI catalog draft."
    echo "Start the Yuheng Engine, publish the draft, and start the Admin Web and Portal applications with scripts/unified-xingyuan/start-local-stack.sh."
    return
  fi

  stage "restarting providers with real Yuheng catalog reporting"
  # Yuheng Admin is the reporting control plane, so it first remains available with its own
  # reporting disabled while Tianshu, Tianquan-Shoubing, and Tianquan-Jianshen publish their real HTTP catalogs.
  write_env "${env_dir}/yuheng-admin.env" \
    YUHENG_ADMIN_YUHENG_REPORTING_ENABLED false
  stop_process yuheng-admin
  stop_process tianquan-jianshen
  start_process yuheng-admin "${env_dir}/yuheng-admin.env" "${gateway_admin_jar}"
  wait_http yuheng-admin "${gateway_admin_url}/actuator/health/readiness"
  stop_process tianshu
  start_process tianshu "${env_dir}/tianshu.env" "${ddc_jar}"
  wait_http tianshu "${ddc_url}/actuator/health/readiness"
  wait_ddc_rpc
  stop_process tianquan-shoubing
  start_process tianquan-shoubing "${env_dir}/tianquan-shoubing.env" "${idp_jar}"
  wait_http tianquan-shoubing "${idp_url}/actuator/health/readiness"
  refresh_service_tokens
  start_process tianquan-jianshen "${env_dir}/tianquan-jianshen.env" "${rbac3_jar}"
  wait_http tianquan-jianshen "${rbac3_url}/actuator/health/readiness"
  write_env "${env_dir}/yuheng-admin.env" \
    YUHENG_ADMIN_YUHENG_REPORTING_ENABLED false
  stop_process yuheng-admin
  start_process yuheng-admin "${env_dir}/yuheng-admin.env" "${gateway_admin_jar}"
  wait_http yuheng-admin "${gateway_admin_url}/actuator/health/readiness"

  stage "waiting for final Tianshu provider registrations"
  wait_ddc_provider_registration permission tianquan-shoubing tianquan-shoubing-admin
  wait_ddc_provider_registration permission tianquan-jianshen tianquan-jianshen-admin
  stage "starting Yuheng Engine after Tianshu control plane is ready"
  start_process yuheng-biz-gateway "${env_dir}/yuheng-biz-gateway.env" "${gateway_engine_jar}"
  wait_http yuheng-biz-gateway http://127.0.0.1:18182/actuator/health/readiness
  stage "starting independent Yuheng MCP Engine"
  start_process yuheng-mcp-gateway "${env_dir}/yuheng-mcp-gateway.env" "${gateway_mcp_engine_jar}"
  wait_http yuheng-mcp-gateway http://127.0.0.1:18186/actuator/health/readiness

  stage "starting mock backend"
  start_process mock-backend "${env_dir}/mock-backend.env" "${mock_jar}"
  wait_http mock-backend "${mock_url}/actuator/health/readiness"
  wait_gateway_catalog
  if [[ "${UNIFIED_IDENTITY_DEFER_YUHENG_RELEASE:-false}" == "true" ]]; then
    stage "preparing Yuheng routes for the unified platform publisher"
    publish_gateway_routes true
  else
    publish_gateway_routes
    wait_gateway_route
  fi
  echo "Unified identity backends are running. Run verify for live acceptance checks."
}

command_refresh_tokens() {
  load_local_oauth_registry
  local default_access_token tenant_b_access_token
  process_running tianquan-shoubing || fail "tianquan-shoubing is not running"
  process_running tianquan-jianshen || fail "tianquan-jianshen is not running"
  validate_current_oauth_directory
  refresh_service_tokens
  default_access_token="$(user_access_token_for_tenant default)"
  activate_roles "${default_access_token}" true
  tenant_b_access_token="$(user_access_token_for_tenant tenant-b)"
  activate_roles "${tenant_b_access_token}" true
  stage "SERVICE credentials refreshed; operator USER inputs verified without token or browser refresh"
}

http_status() {
  curl --max-time 10 -sS -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $2" "$1"
}



command_verify() {
  load_local_oauth_registry
  local status subject_before subject_tenant_b token_claims revoked_file
  local rbac3_access_token default_access_token tenant_b_access_token
  local verify_token_dir mvn_status
  for name in tianshu tianquan-shoubing tianquan-jianshen yuheng-admin mock-backend yuheng-biz-gateway yuheng-mcp-gateway; do
    process_running "${name}" || fail "${name} is not running; run start first"
  done
  rbac3_access_token="$(user_access_token_for_tenant default)"
  activate_roles "${rbac3_access_token}" false
  default_access_token="$(user_access_token_for_tenant default)"
  status="$(http_status "${gateway_url}/api/mock/admin" \
    "${default_access_token}")"
  [[ "${status}" == "403" ]] || fail \
    "downstream permission denial must be 403 before mock role activation; got ${status}"
  activate_roles "${rbac3_access_token}" true
  for ((attempt = 1; attempt <= 20; attempt++)); do
    if [[ "$(http_status "${gateway_url}/api/mock/admin" \
      "${default_access_token}")" == "200" ]]; then
      break
    fi
    sleep 1
  done
  [[ "$(http_status "${gateway_url}/api/mock/admin" \
    "${default_access_token}")" == "200" ]] \
    || fail "role activation did not authorize the unchanged access token"

  tenant_b_access_token="$(user_access_token_for_tenant tenant-b)"
  activate_roles "${tenant_b_access_token}" true
  [[ "$(http_status "${gateway_url}/api/mock/read" \
    "${tenant_b_access_token}")" == "200" ]] \
    || fail "tenant-b token did not reach the backend"
  subject_before="$(jq -Rer 'split(".")[1] | gsub("-";"+") | gsub("_";"/") | @base64d | fromjson | .sub' \
    <<<"${default_access_token}")"
  subject_tenant_b="$(jq -Rer 'split(".")[1] | gsub("-";"+") | gsub("_";"/") | @base64d | fromjson | .sub' \
    <<<"${tenant_b_access_token}")"
  [[ "${subject_before}" == "${subject_tenant_b}" ]] \
    || fail "tenant switch changed the USER identity subject"
  for token_claims in "${default_access_token}" "${tenant_b_access_token}"; do
    jq -Rre 'split(".")[1] | gsub("-";"+") | gsub("_";"/") | @base64d | fromjson
      | ((.principal_type // "USER") == "USER"
        and (has("sid") | not)
        and (has("client_id"))
        and (has("token_version") | not))' <<<"${token_claims}" >/dev/null 2>&1 \
      || fail "USER Access Token contains a session or client lifecycle claim"
  done

  revoked_file="$(jq -er '.tenants.default.revokedAccessTokenFile' <<<"${oauth_registry_json}")" \
    || fail "Provide protected revoked USER evidence for backend revocation acceptance; BFF rotation/logout is operator-run"
  protected_oauth_file "${revoked_file}" 16384
  local_user_input_metadata default "${revoked_file}" >/dev/null
  [[ "$(http_status "${gateway_url}/api/mock/read" "$(<"${revoked_file}")")" == 401 ]] \
    || fail "The supplied revoked USER evidence is still accepted"

  verify_token_dir="$(mktemp -d "${runtime_dir}/verify-tokens.XXXXXX")"
  chmod 700 "${verify_token_dir}"
  printf '%s' "${default_access_token}" >"${verify_token_dir}/default.at"
  printf '%s' "${tenant_b_access_token}" >"${verify_token_dir}/tenant-b.at"
  printf '%s' "$(<"${revoked_file}")" >"${verify_token_dir}/pre-logout.at"
  chmod 600 "${verify_token_dir}"/*.at
  UNIFIED_IDENTITY_LIVE=true \
  UNIFIED_IDENTITY_YUHENG_URL="${gateway_url}" \
  UNIFIED_IDENTITY_MOCK_URL="${mock_url}" \
  UNIFIED_IDENTITY_DEFAULT_TOKEN_FILE="${verify_token_dir}/default.at" \
  UNIFIED_IDENTITY_TENANT_B_TOKEN_FILE="${verify_token_dir}/tenant-b.at" \
  UNIFIED_IDENTITY_PRE_LOGOUT_TOKEN_FILE="${verify_token_dir}/pre-logout.at" \
    "${repo_root}/mvnw" -B -ntp -f "${repo_root}/pom.xml" \
      -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite \
      -am -Dtest=UnifiedIdentityTopologyIT,UnifiedIdentityRevocationIT,UnifiedIdentityTenantSwitchIT \
      -Dsurefire.failIfNoSpecifiedTests=false test || mvn_status=$?
  if [[ -n "${mvn_status:-}" ]]; then
    rm -rf "${verify_token_dir}"
    return "${mvn_status}"
  fi
  rm -rf "${verify_token_dir}"
  echo "Backend permission, tenant and supplied revocation checks passed; real BFF rotation/logout remains separate operator acceptance."
}

command_status() {
  local name pid state url status
  for name in tianshu tianquan-shoubing tianquan-jianshen yuheng-admin mock-backend yuheng-biz-gateway yuheng-mcp-gateway; do
    if process_running "${name}"; then
      pid="$(<"${pid_dir}/${name}.pid")"
      state=running
    else
      pid=-
      state=stopped
    fi
    case "${name}" in
      tianshu) url="${ddc_url}/actuator/health/readiness" ;;
      tianquan-shoubing) url="${idp_url}/actuator/health/readiness" ;;
      tianquan-jianshen) url="${rbac3_url}/actuator/health/readiness" ;;
      yuheng-admin) url="${gateway_admin_url}/actuator/health/readiness" ;;
      mock-backend) url="${mock_url}/actuator/health/readiness" ;;
      yuheng-biz-gateway) url=http://127.0.0.1:18182/actuator/health/readiness ;;
      yuheng-mcp-gateway) url=http://127.0.0.1:18186/actuator/health/readiness ;;
    esac
    status="$(curl -sS -o /dev/null -w '%{http_code}' "${url}" 2>/dev/null || true)"
    printf '%-16s pid=%-8s process=%-7s health=%s\n' \
      "${name}" "${pid}" "${state}" "${status:-unreachable}"
  done
}

command_refresh_gateway_admin_catalog() {
  load_local_oauth_registry
  for command in java curl jq openssl psql; do
    require_command "${command}"
  done
  initialize_directories
  process_running yuheng-admin || fail "yuheng-admin is not running"
  resolve_existing_service_tenant_id
  refresh_service_tokens
  ensure_gateway_application xingyuan yuheng-admin "Yuheng Admin"
  write_env "${env_dir}/yuheng-admin.env" YUHENG_ADMIN_YUHENG_REPORTING_ENABLED false
  write_env "${env_dir}/yuheng-admin.env" YUHENG_ADMIN_HTTP_OPENAPI_ENABLED true
  write_env "${env_dir}/yuheng-admin.env" YUHENG_ADMIN_BUILD_ID \
    "$(local_build_id "${gateway_admin_jar}")"
  stop_process yuheng-admin
  start_process yuheng-admin "${env_dir}/yuheng-admin.env" "${gateway_admin_jar}"
  wait_http yuheng-admin "${gateway_admin_url}/actuator/health/readiness"
  wait_gateway_catalog_for_app yuheng-admin
  wait_gateway_openapi_sync_for_app xingyuan yuheng-admin
  echo "Yuheng Admin current OpenAPI query interfaces are available in its catalog."
}

command_publish_gateway_routes() {
  load_local_oauth_registry
  local command
  for command in curl jq openssl; do
    require_command "${command}"
  done
  initialize_directories
  process_running yuheng-admin \
    || fail "yuheng-admin is not running; run start first"
  process_running yuheng-biz-gateway \
    || fail "yuheng-biz-gateway is not running; start the platform stack first"
  [[ -s "${secret_dir}/yuheng-admin-control-plane.service.jwt" ]] \
    || fail "Yuheng control-plane SERVICE token is unavailable"
  [[ -s "${runtime_dir}/yuheng-group.id" ]] \
    || fail "local Yuheng group is unavailable; run start first"
  wait_http yuheng-biz-gateway http://127.0.0.1:18182/actuator/health/readiness
  stage "waiting for Yuheng Engine Tianshu provider registration"
  wait_gateway_engine_provider_registration
  stage "publishing the prepared local Yuheng HTTP catalog"
  publish_gateway_routes
  wait_gateway_route
  echo "Unified identity Yuheng HTTP catalog release is active."
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
  for name in yuheng-mcp-gateway yuheng-biz-gateway mock-backend yuheng-admin tianquan-jianshen tianquan-shoubing tianshu; do
    stop_process "${name}"
  done
  echo "Unified identity managed processes stopped; databases and secrets were preserved."
}

case "${1:---help}" in
  --help|-h|help) usage ;;
  prepare) command_prepare ;;
  start) command_start ;;
  publish-yuheng-routes) command_publish_gateway_routes ;;
  refresh-yuheng-admin-catalog) command_refresh_gateway_admin_catalog ;;
  sync-local-credentials) command_refresh_tokens ;;
  issue-user-token) load_local_oauth_registry; command_issue_user_token ;;
  oauth-context) load_local_oauth_registry; command_oauth_context ;;
  write-service-registration) load_local_oauth_registry; command_write_service_registration ;;
  verify) command_verify ;;
  status) command_status ;;
  stop) command_stop ;;
  *) usage >&2; exit 2 ;;
esac
