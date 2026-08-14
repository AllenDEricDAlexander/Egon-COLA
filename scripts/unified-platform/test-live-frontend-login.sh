#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${script_dir}/lib/common.sh"

service_token="${unified_platform_secret_dir}/idp-admin.service.jwt"
[[ -s "${service_token}" ]] \
  || unified_platform_fail "missing IdP service token"

default_cookie="${unified_platform_runtime_dir}/browser.default.cookies"
[[ -s "${default_cookie}" ]] \
  || unified_platform_fail "missing default tenant Gateway cookie jar"
userinfo="$(curl --max-time 10 -fsS -b "${default_cookie}" \
  "${GATEWAY_BASE_URL}/oauth2/userinfo")" \
  || unified_platform_fail "default tenant Gateway cookie could not resolve /oauth2/userinfo"
tenant_id="$(jq -er '.tid' <<<"${userinfo}")"
identity_sub="$(jq -er '.sub' <<<"${userinfo}")"
[[ "${tenant_id}" =~ ^[1-9][0-9]*$ ]] \
  || unified_platform_fail "default Gateway userinfo has an invalid tenant ID"

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

for command in curl jq; do
  unified_platform_require_command "${command}"
done

fresh_dir="$(mktemp -d "${unified_platform_runtime_dir}/fresh-admin-login.XXXXXX")"
chmod 700 "${fresh_dir}"
trap 'rm -rf "${fresh_dir}"' EXIT
fresh_cookie="${fresh_dir}/gateway.cookies"

csrf="$(curl --max-time 10 -fsS \
  -c "${fresh_cookie}" -b "${fresh_cookie}" \
  -H "Origin: ${IDP_ADMIN_WEB_URL}" \
  "${GATEWAY_BASE_URL}/oauth2/login/csrf" | jq -er '.token')"
login_code="$(curl --max-time 10 -sS -o "${fresh_dir}/login.json" \
  -w '%{http_code}' -c "${fresh_cookie}" -b "${fresh_cookie}" \
  -H "Origin: ${IDP_ADMIN_WEB_URL}" \
  -H 'Content-Type: application/json' -H "X-IDP-CSRF: ${csrf}" \
  -d "$(jq -cn --arg tenantId "${tenant_id}" \
    --arg password "$(<"${unified_platform_secret_dir}/idp-admin.password")" \
    '{tenantId:$tenantId,username:"alice",password:$password}')" \
  "${GATEWAY_BASE_URL}/oauth2/login")"
[[ "${login_code}" == '200' ]] \
  || unified_platform_fail \
    "fresh Gateway password login returned HTTP ${login_code}"

cp "${fresh_cookie}" "${fresh_dir}/refresh-before.cookies"
refresh_code="$(curl --max-time 10 -sS \
  -o "${fresh_dir}/refresh.json" -w '%{http_code}' \
  -c "${fresh_cookie}" -b "${fresh_cookie}" \
  -H "Origin: ${IDP_ADMIN_WEB_URL}" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode grant_type=refresh_token \
  "${GATEWAY_BASE_URL}/oauth2/token")"
[[ "${refresh_code}" == '200' ]] \
  || unified_platform_fail \
    "Gateway refresh token returned HTTP ${refresh_code}"
refresh_code="$(curl --max-time 10 -sS -o "${fresh_dir}/stable-refresh.json" \
  -w '%{http_code}' -c "${fresh_dir}/refresh-before.cookies" \
  -b "${fresh_dir}/refresh-before.cookies" -X POST \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode grant_type=refresh_token \
  "${GATEWAY_BASE_URL}/oauth2/token")"
[[ "${refresh_code}" == '200' ]] \
  || unified_platform_fail 'stable USER Refresh Token was rejected before logout'

verify_fresh_admin_json() {
  local label="$1" url="$2"
  local output="${fresh_dir}/${label}.json" http_code
  http_code="$(curl --max-time 15 -sS -o "${output}" -w '%{http_code}' \
    -b "${fresh_cookie}" "${url}")"
  [[ "${http_code}" == '200' ]] \
    || unified_platform_fail \
      "fresh Admin endpoint returned HTTP ${http_code}: ${label}"
  jq -e 'type == "object"' "${output}" >/dev/null \
    || unified_platform_fail \
      "fresh Admin endpoint returned invalid JSON: ${label}"
}

verify_fresh_admin_array() {
  local label="$1" url="$2"
  local output="${fresh_dir}/${label}.json" http_code
  http_code="$(curl --max-time 15 -sS -o "${output}" -w '%{http_code}' \
    -b "${fresh_cookie}" "${url}")"
  [[ "${http_code}" == '200' ]] \
    || unified_platform_fail \
      "fresh Admin endpoint returned HTTP ${http_code}: ${label}"
  jq -e 'type == "array"' "${output}" >/dev/null \
    || unified_platform_fail \
      "fresh Admin endpoint returned invalid JSON: ${label}"
}

expected_role_pairs="$(jq -cn '[
  {applicationCode:"ddc-admin",rootRoleCode:"DDC_LOCAL_ADMIN"},
  {applicationCode:"gateway-admin",rootRoleCode:"GATEWAY_LOCAL_ADMIN"},
  {applicationCode:"idp-admin",rootRoleCode:"IDP_LOCAL_ADMIN"},
  {applicationCode:"mock-backend",rootRoleCode:"MOCK_LOCAL_ADMIN"},
  {applicationCode:"mock-backend",rootRoleCode:"MOCK_LOCAL_ENTRY"},
  {applicationCode:"rbac3-admin",rootRoleCode:"RBAC3_LOCAL_ADMIN"}
]')"
verify_fresh_admin_json role-candidates \
  "${RBAC3_ADMIN_WEB_URL}/api/rbac3/v1/auth/role-activation-candidates"
expected_active_roles="$(jq -cer --argjson expected "${expected_role_pairs}" '
  [
    .data.applications[] as $application
    | $application.candidates[]
    | {
        applicationCode: $application.applicationCode,
        rootRoleCode: .rootRoleCode,
        rootRoleId: .rootRoleId
      }
    | select(. as $candidate | $expected | any(
        .applicationCode == $candidate.applicationCode
        and .rootRoleCode == $candidate.rootRoleCode))
  ]
  | sort_by(.applicationCode, .rootRoleCode) as $matched
  | ($expected | sort_by(.applicationCode, .rootRoleCode)) as $expectedPairs
  | if ($matched | map({applicationCode, rootRoleCode})) == $expectedPairs
    then $matched
      | group_by(.applicationCode)
      | map({
          applicationCode: .[0].applicationCode,
          rootRoleIds: (map(.rootRoleId) | sort)
        })
    else error("generated local administrator role candidates are incomplete")
    end' "${fresh_dir}/role-candidates.json")"

verify_fresh_admin_json idp-bootstrap \
  "${IDP_ADMIN_WEB_URL}/api/v1/auth/bootstrap"
verify_fresh_admin_array idp-users \
  "${IDP_ADMIN_WEB_URL}/api/v1/identity/users"
verify_fresh_admin_array idp-clients \
  "${IDP_ADMIN_WEB_URL}/api/v1/identity/clients"
verify_fresh_admin_array idp-signing-keys \
  "${IDP_ADMIN_WEB_URL}/api/v1/identity/signing-keys"
verify_fresh_admin_json idp-audits \
  "${IDP_ADMIN_WEB_URL}/api/v1/identity/audits?page=0&size=20"

curl --max-time 15 -fsS -o "${fresh_dir}/active-roles.json" \
  -b "${fresh_cookie}" \
  "${RBAC3_ADMIN_WEB_URL}/api/rbac3/v1/auth/role-activations"
jq -e \
  --argjson expected "${expected_active_roles}" \
  '.data.activationRequired == false
    and ([.data.activeRoles[]
      | {applicationCode, rootRoleIds: (.rootRoleIds | sort)}]
      | sort_by(.applicationCode)) == $expected' \
  "${fresh_dir}/active-roles.json" >/dev/null \
  || unified_platform_fail \
    "fresh Gateway JWT login did not activate the generated local administrator roles"

verify_fresh_admin_json rbac3-bootstrap \
  "${RBAC3_ADMIN_WEB_URL}/api/v1/auth/bootstrap"
verify_fresh_admin_json gateway-bootstrap \
  "${GATEWAY_ADMIN_WEB_URL}/api/v1/auth/bootstrap"
verify_fresh_admin_json ddc-bootstrap \
  "${DDC_ADMIN_WEB_URL}/api/v1/auth/bootstrap"

cp "${fresh_cookie}" "${fresh_dir}/pre-logout.cookies"
logout_code="$(curl --max-time 10 -sS -o "${fresh_dir}/logout.json" \
  -w '%{http_code}' -c "${fresh_cookie}" -b "${fresh_cookie}" -X POST \
  "${GATEWAY_BASE_URL}/oauth2/logout")"
[[ "${logout_code}" == '204' ]] \
  || unified_platform_fail "Gateway logout returned HTTP ${logout_code}"
refresh_code="$(curl --max-time 10 -sS -o "${fresh_dir}/refresh-after-logout.json" \
  -w '%{http_code}' -c "${fresh_dir}/refresh-before.cookies" \
  -b "${fresh_dir}/refresh-before.cookies" -X POST \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode grant_type=refresh_token \
  "${GATEWAY_BASE_URL}/oauth2/token")"
[[ "${refresh_code}" != '200' ]] \
  || unified_platform_fail 'Gateway logout did not revoke the USER Refresh Token'
fresh_cookie="${fresh_dir}/pre-logout.cookies"
verify_fresh_admin_json idp-bootstrap-after-logout \
  "${IDP_ADMIN_WEB_URL}/api/v1/auth/bootstrap"

printf '%s\n' \
  'live-frontend-login: memberships, refresh, IdP pages, and four Admin endpoints PASS'
