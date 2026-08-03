#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${script_dir}/lib/common.sh"

default_token="${unified_platform_secret_dir}/idp-admin.access.jwt"
service_token="${unified_platform_secret_dir}/idp-admin.service.jwt"
[[ -s "${default_token}" ]] \
  || unified_platform_fail "missing default tenant access token"
[[ -s "${service_token}" ]] \
  || unified_platform_fail "missing IdP service token"

tenant_id="$(jq -Rer 'split(".")[1] | @base64d | fromjson | .tid' \
  <"${default_token}")"
identity_sub="$(jq -Rer 'split(".")[1] | @base64d | fromjson | .sub' \
  <"${default_token}")"
[[ "${tenant_id}" =~ ^[1-9][0-9]*$ ]] \
  || unified_platform_fail "default access token has an invalid tenant ID"

frontends=(
  "idp-admin-web|${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web|${IDP_ADMIN_WEB_URL}/src/auth/CentralLoginPage.tsx"
  "rbac3-admin-web|${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web|${RBAC3_ADMIN_WEB_URL}/src/features/auth/LoginPage.tsx"
  "gateway-admin-web|${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web|${GATEWAY_ADMIN_WEB_URL}/src/auth/LoginPage.tsx"
  "ddc-admin-web|${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web|${DDC_ADMIN_WEB_URL}/src/auth/LoginPage.tsx"
)

for frontend in "${frontends[@]}"; do
  IFS='|' read -r client_id web_dir module_url <<<"${frontend}"
  env_file="${web_dir}/.env.local"
  [[ -s "${env_file}" ]] \
    || unified_platform_fail "${client_id} has no generated local login environment"
  configured_tenant="$(awk -F= \
    '$1 == "VITE_DEFAULT_TENANT_ID" {print $2; exit}' "${env_file}")"
  [[ "${configured_tenant}" == "${tenant_id}" ]] \
    || unified_platform_fail "${client_id} default tenant does not match the active membership"

  transformed_module="$(curl --max-time 10 -fsS "${module_url}")"
  grep -Fq "${tenant_id}" <<<"${transformed_module}" \
    || unified_platform_fail "${client_id} running Vite process did not load the default tenant"

  response="$(curl --max-time 10 -sS -w $'\n%{http_code}' \
    -H "Authorization: Bearer $(<"${service_token}")" \
    -H 'Content-Type: application/json' \
    -d "$(jq -cn \
      --arg identitySub "${identity_sub}" \
      --arg tenantId "${configured_tenant}" \
      --arg clientId "${client_id}" \
      '{identitySub:$identitySub,tenantId:$tenantId,clientId:$clientId}')" \
    "${RBAC3_BASE_URL}/internal/v1/identity/resolve")"
  http_code="${response##*$'\n'}"
  body="${response%$'\n'*}"
  [[ "${http_code}" == '200' ]] \
    || unified_platform_fail "${client_id} membership resolution returned HTTP ${http_code}"
  jq -e --arg tenantId "${tenant_id}" \
    '.data.status == "ACTIVE" and .data.tenantId == $tenantId' \
    <<<"${body}" >/dev/null \
    || unified_platform_fail "${client_id} membership is not active"
done

printf 'live-frontend-login: all four frontend defaults resolve to ACTIVE memberships\n'
