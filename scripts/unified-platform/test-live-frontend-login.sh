#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${script_dir}/lib/common.sh"

for command in curl jq; do
  unified_platform_require_command "${command}"
done

# Verify a new session using the same default tenant rendered by the login form.
# An old cached cookie must not hide a broken password login or block this probe.
idp_web_dir="${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web"
tenant_id="$(awk -F= '$1 == "VITE_DEFAULT_TENANT_ID" {print $2; exit}' \
  "${idp_web_dir}/.env.local")"
[[ "${tenant_id}" =~ ^[1-9][0-9]*$ ]] \
  || unified_platform_fail "generated login environment has an invalid tenant ID"

frontends=(
  "idp-admin-web|${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web|${IDP_ADMIN_WEB_URL}/src/auth/CentralLoginPage.tsx"
  "rbac3-admin-web|${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web|${RBAC3_ADMIN_WEB_URL}/src/features/auth/LoginPage.tsx"
  "gateway-admin-web|${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web|${GATEWAY_ADMIN_WEB_URL}/src/auth/LoginPage.tsx"
  "ddc-admin-web|${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web|${DDC_ADMIN_WEB_URL}/src/auth/LoginPage.tsx"
  "portal-web|${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-admin-portal|${PLATFORM_PORTAL_URL}/src/app/PortalLayout.tsx"
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
  grep -Fq "\"VITE_GATEWAY_ORIGIN\": \"${GATEWAY_BASE_URL}\"" <<<"${transformed_module}" \
    || unified_platform_fail "${client_id} running Vite process did not load the Gateway login origin"

done

fresh_dir="$(mktemp -d "${unified_platform_runtime_dir}/fresh-admin-login.XXXXXX")"
chmod 700 "${fresh_dir}"
trap 'rm -rf "${fresh_dir}"' EXIT
fresh_cookie="${fresh_dir}/gateway.cookies"

# All same-origin auth proxies must return protocol JSON, not Vite's HTTP 200 HTML.
for web_url in "${IDP_ADMIN_WEB_URL}" "${RBAC3_ADMIN_WEB_URL}" \
  "${GATEWAY_ADMIN_WEB_URL}" "${DDC_ADMIN_WEB_URL}" "${PLATFORM_PORTAL_URL}"; do
  curl --max-time 10 -fsS "${web_url}/oauth2/login/csrf" \
    | jq -e '.token | type == "string" and length > 0' >/dev/null \
    || unified_platform_fail "frontend auth proxy did not return CSRF JSON: ${web_url}"
  preflight_code="$(curl --max-time 10 -sS -o /dev/null \
    -D "${fresh_dir}/cors.headers" -w '%{http_code}' -X OPTIONS \
    -H "Origin: ${web_url}" \
    -H 'Access-Control-Request-Method: POST' \
    -H 'Access-Control-Request-Headers: content-type,x-idp-csrf' \
    "${GATEWAY_BASE_URL}/oauth2/login")"
  [[ "${preflight_code}" == '200' || "${preflight_code}" == '204' ]] \
    || unified_platform_fail "Gateway login preflight rejected ${web_url}: HTTP ${preflight_code}"
  tr -d '\r' <"${fresh_dir}/cors.headers" \
    | grep -Fixq "Access-Control-Allow-Origin: ${web_url}" \
    || unified_platform_fail "Gateway login CORS origin is missing for ${web_url}"
  tr -d '\r' <"${fresh_dir}/cors.headers" \
    | grep -Fixq 'Access-Control-Allow-Credentials: true' \
    || unified_platform_fail "Gateway login CORS credentials are missing for ${web_url}"
done

csrf="$(curl --max-time 10 -fsS \
  -c "${fresh_cookie}" -b "${fresh_cookie}" \
  -H "Origin: ${PLATFORM_PORTAL_URL}" \
  "${GATEWAY_BASE_URL}/oauth2/login/csrf" | jq -er '.token')"
login_code="$(curl --max-time 10 -sS -o "${fresh_dir}/login.json" \
  -w '%{http_code}' -c "${fresh_cookie}" -b "${fresh_cookie}" \
  -H "Origin: ${PLATFORM_PORTAL_URL}" \
  -H 'Content-Type: application/json' -H "X-IDP-CSRF: ${csrf}" \
  -d "$(jq -cn --arg tenantId "${tenant_id}" \
    --arg password "$(<"${unified_platform_secret_dir}/idp-admin.password")" \
    '{tenantId:$tenantId,username:"alice",password:$password}')" \
  "${GATEWAY_BASE_URL}/oauth2/login")"
[[ "${login_code}" == '200' ]] \
  || unified_platform_fail \
    "fresh Gateway password login returned HTTP ${login_code}"

userinfo="$(curl --max-time 10 -fsS -b "${fresh_cookie}" \
  "${GATEWAY_BASE_URL}/oauth2/userinfo")"
[[ "$(jq -er '.tid' <<<"${userinfo}")" == "${tenant_id}" ]] \
  || unified_platform_fail "fresh USER session has a different tenant from the login form"
identity_sub="$(jq -er '.sub' <<<"${userinfo}")"
membership_response="$(curl --max-time 15 -sS -w $'\n%{http_code}' \
  -b "${fresh_cookie}" \
  "${GATEWAY_BASE_URL}/api/v1/identity/tenants/${tenant_id}/members?query=${identity_sub}&status=ACTIVE&page=0&size=20")"
membership_http_code="${membership_response##*$'\n'}"
membership_body="${membership_response%$'\n'*}"
[[ "${membership_http_code}" == '200' ]] \
  || unified_platform_fail \
    "IdP tenant membership resolution returned HTTP ${membership_http_code}"
jq -e --arg identitySub "${identity_sub}" \
  '.totalElements == 1
    and any(.content[]; .identitySub == $identitySub and .status == "ACTIVE")' \
  <<<"${membership_body}" >/dev/null \
  || unified_platform_fail "IdP tenant membership is not active"

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
    then ($matched
      | map(select(.applicationCode != "mock-backend"
          or .rootRoleCode == "MOCK_LOCAL_ENTRY")))
      | group_by(.applicationCode)
      | map({
          applicationCode: .[0].applicationCode,
          rootRoleIds: (map(.rootRoleId) | sort)
        })
    else error("generated local administrator role candidates are incomplete")
    end' "${fresh_dir}/role-candidates.json")"

verify_fresh_admin_json idp-bootstrap \
  "${IDP_ADMIN_WEB_URL}/api/v1/identity/auth/bootstrap"
jq -e '(.data // .).permissions | index("idp:bootstrap:read") != null' \
  "${fresh_dir}/idp-bootstrap.json" >/dev/null \
  || unified_platform_fail 'IdP Admin bootstrap did not return IdP permissions'
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
    and (([.data.activeRoles[]
      | {applicationCode, rootRoleIds: (.rootRoleIds | sort)}]
      | sort_by(.applicationCode)) as $actual
      | $actual | contains($expected))' \
  "${fresh_dir}/active-roles.json" >/dev/null \
  || unified_platform_fail \
    "fresh Gateway JWT login did not activate the configured local roles"

verify_fresh_admin_json rbac3-about \
  "${RBAC3_ADMIN_WEB_URL}/api/v1/auth/about"
jq -e '(.data // .).permissions | index("system:about:read") != null' \
  "${fresh_dir}/rbac3-about.json" >/dev/null \
  || unified_platform_fail 'RBAC3 About did not return RBAC3 permissions'
verify_fresh_admin_json gateway-bootstrap \
  "${GATEWAY_ADMIN_WEB_URL}/api/v1/auth/bootstrap"
verify_fresh_admin_json ddc-bootstrap \
  "${DDC_ADMIN_WEB_URL}/api/v1/ddc/auth/bootstrap"
jq -e '(.data // .).permissions | index("DDC_READ") != null' \
  "${fresh_dir}/ddc-bootstrap.json" >/dev/null \
  || unified_platform_fail 'DDC bootstrap did not return DDC permissions'

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
after_logout_code="$(curl --max-time 15 -sS \
  -o "${fresh_dir}/idp-bootstrap-after-logout.json" -w '%{http_code}' \
  -b "${fresh_dir}/pre-logout.cookies" \
  "${IDP_ADMIN_WEB_URL}/api/v1/identity/auth/bootstrap")"
[[ "${after_logout_code}" == '401' ]] \
  || unified_platform_fail \
    "logout did not invalidate the old Gateway cookie; bootstrap returned HTTP ${after_logout_code}"

printf '%s\n' \
  'live-frontend-login: memberships, refresh, IdP pages, and four Admin endpoints PASS'
