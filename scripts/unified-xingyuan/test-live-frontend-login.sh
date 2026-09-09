#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${script_dir}/lib/common.sh"

for command in curl jq; do
  unified_xingyuan_require_command "${command}"
done

# Verify a new session using the same default tenant rendered by the login form.
# An old cached cookie must not hide a broken password login or block this probe.
idp_web_dir="${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web"
tenant_id="$(awk -F= '$1 == "VITE_DEFAULT_TENANT_ID" {print $2; exit}' \
  "${idp_web_dir}/.env.local")"
[[ "${tenant_id}" =~ ^[1-9][0-9]*$ ]] \
  || unified_xingyuan_fail "generated login environment has an invalid tenant ID"

frontends=(
  "tianquan-shoubing-admin-web|${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web|${TIANQUAN_SHOUBING_ADMIN_WEB_URL}/src/auth/CentralLoginPage.tsx"
  "tianquan-jianshen-admin-web|${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web|${TIANQUAN_JIANSHEN_ADMIN_WEB_URL}/src/features/auth/LoginPage.tsx"
  "yuheng-admin-web|${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web|${YUHENG_ADMIN_WEB_URL}/src/auth/LoginPage.tsx"
  "tianshu-admin-web|${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web|${TIANSHU_ADMIN_WEB_URL}/src/auth/LoginPage.tsx"
  "portal-web|${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-xingyuan-admin-portal|${PLATFORM_PORTAL_URL}/src/app/PortalLayout.tsx"
)

for frontend in "${frontends[@]}"; do
  IFS='|' read -r client_id web_dir module_url <<<"${frontend}"
  env_file="${web_dir}/.env.local"
  [[ -s "${env_file}" ]] \
    || unified_xingyuan_fail "${client_id} has no generated local login environment"
  configured_tenant="$(awk -F= \
    '$1 == "VITE_DEFAULT_TENANT_ID" {print $2; exit}' "${env_file}")"
  [[ "${configured_tenant}" == "${tenant_id}" ]] \
    || unified_xingyuan_fail "${client_id} default tenant does not match the active membership"

  transformed_module="$(curl --max-time 10 -fsS "${module_url}")"
  grep -Fq "${tenant_id}" <<<"${transformed_module}" \
    || unified_xingyuan_fail "${client_id} running Vite process did not load the default tenant"
  grep -Fq "\"VITE_YUHENG_ORIGIN\": \"${YUHENG_BASE_URL}\"" <<<"${transformed_module}" \
    || unified_xingyuan_fail "${client_id} running Vite process did not load the Yuheng login origin"

done

fresh_dir="$(mktemp -d "${unified_xingyuan_runtime_dir}/fresh-admin-login.XXXXXX")"
chmod 700 "${fresh_dir}"
trap 'rm -rf "${fresh_dir}"' EXIT
fresh_cookie="${fresh_dir}/yuheng.cookies"

# All same-origin auth proxies must return protocol JSON, not Vite's HTTP 200 HTML.
for web_url in "${TIANQUAN_SHOUBING_ADMIN_WEB_URL}" "${TIANQUAN_JIANSHEN_ADMIN_WEB_URL}" \
  "${YUHENG_ADMIN_WEB_URL}" "${TIANSHU_ADMIN_WEB_URL}" "${PLATFORM_PORTAL_URL}"; do
  curl --max-time 10 -fsS "${web_url}/oauth2/login/csrf" \
    | jq -e '.token | type == "string" and length > 0' >/dev/null \
    || unified_xingyuan_fail "frontend auth proxy did not return CSRF JSON: ${web_url}"
  preflight_code="$(curl --max-time 10 -sS -o /dev/null \
    -D "${fresh_dir}/cors.headers" -w '%{http_code}' -X OPTIONS \
    -H "Origin: ${web_url}" \
    -H 'Access-Control-Request-Method: POST' \
    -H 'Access-Control-Request-Headers: content-type,x-tianquan-shoubing-csrf' \
    "${YUHENG_BASE_URL}/oauth2/login")"
  [[ "${preflight_code}" == '200' || "${preflight_code}" == '204' ]] \
    || unified_xingyuan_fail "Yuheng login preflight rejected ${web_url}: HTTP ${preflight_code}"
  tr -d '\r' <"${fresh_dir}/cors.headers" \
    | grep -Fixq "Access-Control-Allow-Origin: ${web_url}" \
    || unified_xingyuan_fail "Yuheng login CORS origin is missing for ${web_url}"
  tr -d '\r' <"${fresh_dir}/cors.headers" \
    | grep -Fixq 'Access-Control-Allow-Credentials: true' \
    || unified_xingyuan_fail "Yuheng login CORS credentials are missing for ${web_url}"
done

csrf="$(curl --max-time 10 -fsS \
  -c "${fresh_cookie}" -b "${fresh_cookie}" \
  -H "Origin: ${PLATFORM_PORTAL_URL}" \
  "${YUHENG_BASE_URL}/oauth2/login/csrf" | jq -er '.token')"
login_code="$(curl --max-time 10 -sS -o "${fresh_dir}/login.json" \
  -w '%{http_code}' -c "${fresh_cookie}" -b "${fresh_cookie}" \
  -H "Origin: ${PLATFORM_PORTAL_URL}" \
  -H 'Content-Type: application/json' -H "X-TIANQUAN-SHOUBING-CSRF: ${csrf}" \
  -d "$(jq -cn --arg tenantId "${tenant_id}" \
    --arg password "$(<"${unified_xingyuan_secret_dir}/tianquan-shoubing-admin.password")" \
    '{tenantId:$tenantId,username:"alice",password:$password}')" \
  "${YUHENG_BASE_URL}/oauth2/login")"
[[ "${login_code}" == '200' ]] \
  || unified_xingyuan_fail \
    "fresh Yuheng password login returned HTTP ${login_code}"

userinfo="$(curl --max-time 10 -fsS -b "${fresh_cookie}" \
  "${YUHENG_BASE_URL}/oauth2/userinfo")"
[[ "$(jq -er '.tid' <<<"${userinfo}")" == "${tenant_id}" ]] \
  || unified_xingyuan_fail "fresh USER session has a different tenant from the login form"
identity_sub="$(jq -er '.sub' <<<"${userinfo}")"
membership_response="$(curl --max-time 15 -sS -w $'\n%{http_code}' \
  -b "${fresh_cookie}" \
  "${YUHENG_BASE_URL}/api/v1/tianquan-shoubing/tenants/${tenant_id}/members?query=${identity_sub}&status=ACTIVE&page=0&size=20")"
membership_http_code="${membership_response##*$'\n'}"
membership_body="${membership_response%$'\n'*}"
[[ "${membership_http_code}" == '200' ]] \
  || unified_xingyuan_fail \
    "Tianquan-Shoubing tenant membership resolution returned HTTP ${membership_http_code}"
jq -e --arg identitySub "${identity_sub}" \
  '.totalElements == 1
    and any(.content[]; .identitySub == $identitySub and .status == "ACTIVE")' \
  <<<"${membership_body}" >/dev/null \
  || unified_xingyuan_fail "Tianquan-Shoubing tenant membership is not active"

cp "${fresh_cookie}" "${fresh_dir}/refresh-before.cookies"
refresh_code="$(curl --max-time 10 -sS \
  -o "${fresh_dir}/refresh.json" -w '%{http_code}' \
  -c "${fresh_cookie}" -b "${fresh_cookie}" \
  -H "Origin: ${TIANQUAN_SHOUBING_ADMIN_WEB_URL}" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode grant_type=refresh_token \
  "${YUHENG_BASE_URL}/oauth2/token")"
[[ "${refresh_code}" == '200' ]] \
  || unified_xingyuan_fail \
    "Yuheng refresh token returned HTTP ${refresh_code}"
refresh_code="$(curl --max-time 10 -sS -o "${fresh_dir}/stable-refresh.json" \
  -w '%{http_code}' -c "${fresh_dir}/refresh-before.cookies" \
  -b "${fresh_dir}/refresh-before.cookies" -X POST \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode grant_type=refresh_token \
  "${YUHENG_BASE_URL}/oauth2/token")"
[[ "${refresh_code}" == '200' ]] \
  || unified_xingyuan_fail 'stable USER Refresh Token was rejected before logout'

verify_fresh_admin_json() {
  local label="$1" url="$2"
  local output="${fresh_dir}/${label}.json" http_code
  http_code="$(curl --max-time 15 -sS -o "${output}" -w '%{http_code}' \
    -b "${fresh_cookie}" "${url}")"
  [[ "${http_code}" == '200' ]] \
    || unified_xingyuan_fail \
      "fresh Admin endpoint returned HTTP ${http_code}: ${label}"
  jq -e 'type == "object"' "${output}" >/dev/null \
    || unified_xingyuan_fail \
      "fresh Admin endpoint returned invalid JSON: ${label}"
}

verify_fresh_admin_array() {
  local label="$1" url="$2"
  local output="${fresh_dir}/${label}.json" http_code
  http_code="$(curl --max-time 15 -sS -o "${output}" -w '%{http_code}' \
    -b "${fresh_cookie}" "${url}")"
  [[ "${http_code}" == '200' ]] \
    || unified_xingyuan_fail \
      "fresh Admin endpoint returned HTTP ${http_code}: ${label}"
  jq -e 'type == "array"' "${output}" >/dev/null \
    || unified_xingyuan_fail \
      "fresh Admin endpoint returned invalid JSON: ${label}"
}

expected_role_pairs="$(jq -cn '[
  {applicationCode:"tianshu-admin",rootRoleCode:"TIANSHU_LOCAL_ADMIN"},
  {applicationCode:"yuheng-admin",rootRoleCode:"YUHENG_LOCAL_ADMIN"},
  {applicationCode:"tianquan-shoubing-admin",rootRoleCode:"TIANQUAN_SHOUBING_LOCAL_ADMIN"},
  {applicationCode:"mock-backend",rootRoleCode:"MOCK_LOCAL_ADMIN"},
  {applicationCode:"mock-backend",rootRoleCode:"MOCK_LOCAL_ENTRY"},
  {applicationCode:"tianquan-jianshen-admin",rootRoleCode:"TIANQUAN_JIANSHEN_LOCAL_ADMIN"}
]')"
verify_fresh_admin_json role-candidates \
  "${TIANQUAN_JIANSHEN_ADMIN_WEB_URL}/api/tianquan-jianshen/v1/auth/role-activation-candidates"
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

verify_fresh_admin_json tianquan-shoubing-bootstrap \
  "${TIANQUAN_SHOUBING_ADMIN_WEB_URL}/api/v1/tianquan-shoubing/auth/bootstrap"
jq -e '(.data // .).permissions | index("tianquan-shoubing:bootstrap:read") != null' \
  "${fresh_dir}/tianquan-shoubing-bootstrap.json" >/dev/null \
  || unified_xingyuan_fail 'Tianquan-Shoubing Admin bootstrap did not return Tianquan-Shoubing permissions'
verify_fresh_admin_array tianquan-shoubing-users \
  "${TIANQUAN_SHOUBING_ADMIN_WEB_URL}/api/v1/tianquan-shoubing/users"
verify_fresh_admin_array tianquan-shoubing-clients \
  "${TIANQUAN_SHOUBING_ADMIN_WEB_URL}/api/v1/tianquan-shoubing/clients"
verify_fresh_admin_array tianquan-shoubing-signing-keys \
  "${TIANQUAN_SHOUBING_ADMIN_WEB_URL}/api/v1/tianquan-shoubing/signing-keys"
verify_fresh_admin_json tianquan-shoubing-audits \
  "${TIANQUAN_SHOUBING_ADMIN_WEB_URL}/api/v1/tianquan-shoubing/audits?page=0&size=20"

curl --max-time 15 -fsS -o "${fresh_dir}/active-roles.json" \
  -b "${fresh_cookie}" \
  "${TIANQUAN_JIANSHEN_ADMIN_WEB_URL}/api/tianquan-jianshen/v1/auth/role-activations"
jq -e \
  --argjson expected "${expected_active_roles}" \
  '.data.activationRequired == false
    and (([.data.activeRoles[]
      | {applicationCode, rootRoleIds: (.rootRoleIds | sort)}]
      | sort_by(.applicationCode)) as $actual
      | $actual | contains($expected))' \
  "${fresh_dir}/active-roles.json" >/dev/null \
  || unified_xingyuan_fail \
    "fresh Yuheng JWT login did not activate the configured local roles"

verify_fresh_admin_json tianquan-jianshen-about \
  "${TIANQUAN_JIANSHEN_ADMIN_WEB_URL}/api/v1/auth/about"
jq -e '(.data // .).permissions | index("system:about:read") != null' \
  "${fresh_dir}/tianquan-jianshen-about.json" >/dev/null \
  || unified_xingyuan_fail 'Tianquan-Jianshen About did not return Tianquan-Jianshen permissions'
verify_fresh_admin_json yuheng-bootstrap \
  "${YUHENG_ADMIN_WEB_URL}/api/v1/auth/bootstrap"
verify_fresh_admin_json tianshu-bootstrap \
  "${TIANSHU_ADMIN_WEB_URL}/api/v1/tianshu/auth/bootstrap"
jq -e '(.data // .).permissions | index("TIANSHU_READ") != null' \
  "${fresh_dir}/tianshu-bootstrap.json" >/dev/null \
  || unified_xingyuan_fail 'Tianshu bootstrap did not return Tianshu permissions'

cp "${fresh_cookie}" "${fresh_dir}/pre-logout.cookies"
logout_code="$(curl --max-time 10 -sS -o "${fresh_dir}/logout.json" \
  -w '%{http_code}' -c "${fresh_cookie}" -b "${fresh_cookie}" -X POST \
  "${YUHENG_BASE_URL}/oauth2/logout")"
[[ "${logout_code}" == '204' ]] \
  || unified_xingyuan_fail "Yuheng logout returned HTTP ${logout_code}"
refresh_code="$(curl --max-time 10 -sS -o "${fresh_dir}/refresh-after-logout.json" \
  -w '%{http_code}' -c "${fresh_dir}/refresh-before.cookies" \
  -b "${fresh_dir}/refresh-before.cookies" -X POST \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode grant_type=refresh_token \
  "${YUHENG_BASE_URL}/oauth2/token")"
[[ "${refresh_code}" != '200' ]] \
  || unified_xingyuan_fail 'Yuheng logout did not revoke the USER Refresh Token'
after_logout_code="$(curl --max-time 15 -sS \
  -o "${fresh_dir}/tianquan-shoubing-bootstrap-after-logout.json" -w '%{http_code}' \
  -b "${fresh_dir}/pre-logout.cookies" \
  "${TIANQUAN_SHOUBING_ADMIN_WEB_URL}/api/v1/tianquan-shoubing/auth/bootstrap")"
[[ "${after_logout_code}" == '401' ]] \
  || unified_xingyuan_fail \
    "logout did not invalidate the old Yuheng cookie; bootstrap returned HTTP ${after_logout_code}"

printf '%s\n' \
  'live-frontend-login: memberships, refresh, Tianquan-Shoubing pages, and four Admin endpoints PASS'
