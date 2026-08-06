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

for command in curl jq openssl; do
  unified_platform_require_command "${command}"
done

fresh_dir="$(mktemp -d "${unified_platform_runtime_dir}/fresh-admin-login.XXXXXX")"
chmod 700 "${fresh_dir}"
trap 'rm -rf "${fresh_dir}"' EXIT
fresh_cookie="${fresh_dir}/browser.cookies"

csrf="$(curl --max-time 10 -fsS \
  -c "${fresh_cookie}" -b "${fresh_cookie}" \
  -H "Origin: ${IDP_ADMIN_WEB_URL}" \
  "${IDP_BASE_URL}/oauth2/login/csrf" | jq -er '.token')"
login_code="$(curl --max-time 10 -sS -o "${fresh_dir}/login.json" \
  -w '%{http_code}' -c "${fresh_cookie}" -b "${fresh_cookie}" \
  -H "Origin: ${IDP_ADMIN_WEB_URL}" \
  -H 'Content-Type: application/json' -H "X-IDP-CSRF: ${csrf}" \
  -d "$(jq -cn --arg password \
    "$(<"${unified_platform_secret_dir}/idp-admin.password")" \
    '{username:"alice",password:$password}')" \
  "${IDP_BASE_URL}/oauth2/login")"
[[ "${login_code}" == '200' ]] \
  || unified_platform_fail \
    "fresh browser password login returned HTTP ${login_code}"

fresh_oauth_token() {
  local client_id="$1" origin="$2" output="$3"
  local verifier challenge state headers code http_code
  verifier="$(openssl rand -base64 48 | tr '+/' '-_' | tr -d '=\n')"
  challenge="$(printf '%s' "${verifier}" | openssl dgst -binary -sha256 \
    | openssl base64 -A | tr '+/' '-_' | tr -d '=')"
  state="$(openssl rand -hex 16)"
  headers="${fresh_dir}/${client_id}.authorize.headers"
  http_code="$(curl --max-time 10 -sS -D "${headers}" -o /dev/null \
    -w '%{http_code}' -c "${fresh_cookie}" -b "${fresh_cookie}" -G \
    "${IDP_BASE_URL}/oauth2/authorize" \
    --data-urlencode response_type=code \
    --data-urlencode "client_id=${client_id}" \
    --data-urlencode "redirect_uri=${origin}/oauth/callback" \
    --data-urlencode "audience=${client_id}" \
    --data-urlencode "tenant_id=${tenant_id}" \
    --data-urlencode "state=${state}" --data-urlencode "nonce=${state}" \
    --data-urlencode "code_challenge=${challenge}" \
    --data-urlencode code_challenge_method=S256)"
  [[ "${http_code}" == '302' ]] \
    || unified_platform_fail \
      "${client_id} fresh authorization returned HTTP ${http_code}"
  code="$(sed -n \
    's/^[Ll]ocation:.*[?&]code=\([^&[:space:]]*\).*/\1/p' \
    "${headers}" | tail -1)"
  [[ -n "${code}" ]] \
    || unified_platform_fail "${client_id} fresh authorization returned no code"
  http_code="$(curl --max-time 10 -sS -o "${fresh_dir}/${client_id}.token.json" \
    -w '%{http_code}' -c "${fresh_cookie}" -b "${fresh_cookie}" \
    -H "Origin: ${origin}" \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode grant_type=authorization_code \
    --data-urlencode "client_id=${client_id}" \
    --data-urlencode "code=${code}" \
    --data-urlencode "code_verifier=${verifier}" \
    --data-urlencode "redirect_uri=${origin}/oauth/callback" \
    "${IDP_BASE_URL}/oauth2/token")"
  [[ "${http_code}" == '200' ]] \
    || unified_platform_fail \
      "${client_id} fresh token exchange returned HTTP ${http_code}"
  jq -er '.access_token' "${fresh_dir}/${client_id}.token.json" >"${output}"
  chmod 600 "${output}"
}

fresh_oauth_token idp-admin-web "${IDP_ADMIN_WEB_URL}" \
  "${fresh_dir}/idp.access.jwt"
fresh_oauth_token rbac3-admin-web "${RBAC3_ADMIN_WEB_URL}" \
  "${fresh_dir}/rbac3.access.jwt"
fresh_oauth_token gateway-admin-web "${GATEWAY_ADMIN_WEB_URL}" \
  "${fresh_dir}/gateway.access.jwt"
fresh_oauth_token ddc-admin-web "${DDC_ADMIN_WEB_URL}" \
  "${fresh_dir}/ddc.access.jwt"

idp_refresh_code="$(curl --max-time 10 -sS \
  -o "${fresh_dir}/idp.refresh.json" -w '%{http_code}' \
  -c "${fresh_cookie}" -b "${fresh_cookie}" \
  -H "Origin: ${IDP_ADMIN_WEB_URL}" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode grant_type=refresh_token \
  --data-urlencode client_id=idp-admin-web \
  "${IDP_BASE_URL}/oauth2/token")"
[[ "${idp_refresh_code}" == '200' ]] \
  || unified_platform_fail \
    "idp-admin-web refresh token returned HTTP ${idp_refresh_code}"
jq -er '.access_token' "${fresh_dir}/idp.refresh.json" \
  >"${fresh_dir}/idp.refreshed.access.jwt"
chmod 600 "${fresh_dir}/idp.refreshed.access.jwt"

fresh_sid_count="$(for token_file in \
  "${fresh_dir}/idp.access.jwt" \
  "${fresh_dir}/rbac3.access.jwt" \
  "${fresh_dir}/gateway.access.jwt" \
  "${fresh_dir}/ddc.access.jwt"; do
  jq -Rr 'split(".")[1] | @base64d | fromjson | .sid' <"${token_file}"
done | sort -u | wc -l | tr -d ' ')"
[[ "${fresh_sid_count}" == '1' ]] \
  || unified_platform_fail "fresh Admin tokens do not share one SSO session"

verify_fresh_admin_json() {
  local label="$1" url="$2" token_file="$3"
  local output="${fresh_dir}/${label}.json" http_code
  http_code="$(curl --max-time 15 -sS -o "${output}" -w '%{http_code}' \
    -H "Authorization: Bearer $(<"${token_file}")" "${url}")"
  [[ "${http_code}" == '200' ]] \
    || unified_platform_fail \
      "fresh Admin endpoint returned HTTP ${http_code}: ${label}"
  jq -e 'type == "object"' "${output}" >/dev/null \
    || unified_platform_fail \
      "fresh Admin endpoint returned invalid JSON: ${label}"
}

expected_role_pairs="$(jq -cn '[
  {applicationCode:"ddc-admin",rootRoleCode:"DDC_LOCAL_ADMIN"},
  {applicationCode:"gateway-admin",rootRoleCode:"GATEWAY_LOCAL_ADMIN"},
  {applicationCode:"idp-admin",rootRoleCode:"IDP_LOCAL_ADMIN"},
  {applicationCode:"mock-backend",rootRoleCode:"MOCK_LOCAL_ADMIN"},
  {applicationCode:"rbac3-admin",rootRoleCode:"RBAC3_LOCAL_ADMIN"}
]')"
verify_fresh_admin_json role-candidates \
  "${RBAC3_ADMIN_WEB_URL}/api/rbac3/v1/auth/role-activation-candidates" \
  "${fresh_dir}/rbac3.access.jwt"
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
  | sort_by(.applicationCode) as $matched
  | ($expected | sort_by(.applicationCode)) as $expectedPairs
  | if ($matched | map({applicationCode, rootRoleCode})) == $expectedPairs
    then $matched | map({applicationCode, rootRoleIds: [.rootRoleId]})
    else error("generated local administrator role candidates are incomplete")
    end' "${fresh_dir}/role-candidates.json")"

verify_fresh_admin_json idp-bootstrap \
  "${IDP_ADMIN_WEB_URL}/api/v1/auth/bootstrap" \
  "${fresh_dir}/idp.refreshed.access.jwt"

curl --max-time 15 -fsS -o "${fresh_dir}/active-roles.json" \
  -H "Authorization: Bearer $(<"${fresh_dir}/rbac3.access.jwt")" \
  "${RBAC3_ADMIN_WEB_URL}/api/rbac3/v1/auth/role-activations"
jq -e \
  --argjson expected "${expected_active_roles}" \
  '.data.activationRequired == false
    and ([.data.activeRoles[]
      | {applicationCode, rootRoleIds: (.rootRoleIds | sort)}]
      | sort_by(.applicationCode)) == $expected' \
  "${fresh_dir}/active-roles.json" >/dev/null \
  || unified_platform_fail \
    "fresh SSO session did not activate the generated local administrator roles"

verify_fresh_admin_json rbac3-bootstrap \
  "${RBAC3_ADMIN_WEB_URL}/api/v1/auth/bootstrap" \
  "${fresh_dir}/rbac3.access.jwt"
verify_fresh_admin_json gateway-session \
  "${GATEWAY_ADMIN_WEB_URL}/api/v1/gateway/admin/session" \
  "${fresh_dir}/gateway.access.jwt"
verify_fresh_admin_json ddc-bootstrap \
  "${DDC_ADMIN_WEB_URL}/api/v1/auth/bootstrap" \
  "${fresh_dir}/ddc.access.jwt"

printf '%s\n' \
  'live-frontend-login: memberships, refresh, and four password+PKCE Admin endpoints PASS'
