#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${script_dir}/lib/common.sh"

for command in curl jq awk; do
  unified_xingyuan_require_command "${command}"
done
unified_xingyuan_require_edge_origin

frontends=(
  "tianquan-shoubing-admin-web|${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web|${TIANQUAN_SHOUBING_ADMIN_WEB_URL}"
  "tianquan-jianshen-admin-web|${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web|${TIANQUAN_JIANSHEN_ADMIN_WEB_URL}"
  "yuheng-admin-web|${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web|${YUHENG_ADMIN_WEB_URL}"
  "tianshu-admin-web|${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web|${TIANSHU_ADMIN_WEB_URL}"
  "portal-web|${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-xingyuan-admin-portal|${PLATFORM_PORTAL_URL}"
)

for frontend in "${frontends[@]}"; do
  IFS='|' read -r name web_dir web_url <<<"${frontend}"
  env_file="${web_dir}/.env.local"
  [[ -s "${env_file}" ]] \
    || unified_xingyuan_fail "${name} has no generated Edge environment"
  [[ "$(stat -f '%Lp' "${env_file}")" == '600' ]] \
    || unified_xingyuan_fail "${name} Edge environment must have mode 600"
  grep -Fqx "VITE_YUHENG_ORIGIN=${YUHENG_EDGE_BASE_URL}" "${env_file}" \
    || unified_xingyuan_fail "${name} does not use the configured Edge origin"
  if grep -q '^VITE_DEFAULT_TENANT_ID=' "${env_file}"; then
    unified_xingyuan_fail "${name} browser environment must not prefill a tenant"
  fi
done

authorization_response="$(curl --max-time 10 -sS -D - -o /dev/null \
  -w $'\n%{http_code}' "${YUHENG_EDGE_BASE_URL}/oauth2/authorization/edge-ui")" \
  || unified_xingyuan_fail "Edge OIDC authorization route is unreachable"
authorization_status="${authorization_response##*$'\n'}"
authorization_location="$(awk 'BEGIN {IGNORECASE=1} /^Location:/ {sub(/^[^:]*:[[:space:]]*/, ""); gsub(/\r/, ""); print; exit}' \
  <<<"${authorization_response}")"
[[ "${authorization_status}" == '302' || "${authorization_status}" == '303' ]] \
  && [[ -n "${authorization_location}" ]] \
  || unified_xingyuan_fail "Edge OIDC authorization route did not return an authorization redirect"

csrf_response="$(curl --max-time 10 -fsS "${YUHENG_EDGE_BASE_URL}/bff/csrf")" \
  || unified_xingyuan_fail "Edge BFF CSRF route is unavailable"
jq -e '.headerName == "X-CSRF-TOKEN" and (.token | type == "string" and length > 0)' \
  <<<"${csrf_response}" >/dev/null \
  || unified_xingyuan_fail "Edge BFF CSRF response does not match the contract"

temporary_dir="$(mktemp -d "${TMPDIR:-/tmp}/egon-edge-bff-preflight.XXXXXX")"
chmod 700 "${temporary_dir}"
trap 'rm -rf "${temporary_dir}"' EXIT
session_status="$(curl --max-time 10 -sS -o "${temporary_dir}/session.json" -w '%{http_code}' \
  "${YUHENG_EDGE_BASE_URL}/bff/session")" \
  || unified_xingyuan_fail "Edge BFF session route is unreachable"
[[ "${session_status}" == '401' ]] \
  || unified_xingyuan_fail "anonymous Edge BFF session returned HTTP ${session_status}"
jq -e '.error == "invalid_token" and (has("accessToken") | not) and (has("refreshToken") | not)' \
  "${temporary_dir}/session.json" >/dev/null \
  || unified_xingyuan_fail "anonymous Edge BFF session response contains an invalid shape"

response_header() {
  local file="$1" name="$2"
  awk -v wanted="${name}" 'BEGIN {IGNORECASE=1}
    index($0, wanted ":") == 1 {
      sub(/^[^:]*:[[:space:]]*/, "")
      gsub(/\r/, "")
      print
      exit
    }' "${file}"
}

for frontend in "${frontends[@]}"; do
  IFS='|' read -r name _ web_url <<<"${frontend}"
  headers_file="${temporary_dir}/${name}.headers"
  status="$(curl --max-time 10 -sS -D "${headers_file}" -o /dev/null -w '%{http_code}' \
    -X OPTIONS \
    -H "Origin: ${web_url}" \
    -H 'Access-Control-Request-Method: POST' \
    -H 'Access-Control-Request-Headers: content-type,x-csrf-token' \
    "${YUHENG_EDGE_BASE_URL}/bff/logout")" \
    || unified_xingyuan_fail "${name} Edge BFF CORS preflight is unreachable"
  [[ "${status}" == '200' || "${status}" == '204' ]] \
    || unified_xingyuan_fail "${name} Edge BFF CORS preflight returned HTTP ${status}"
  [[ "$(response_header "${headers_file}" Access-Control-Allow-Origin)" == "${web_url}" ]] \
    || unified_xingyuan_fail "${name} Edge BFF CORS preflight did not allow the exact frontend origin"
  [[ "$(response_header "${headers_file}" Access-Control-Allow-Credentials)" == 'true' ]] \
    || unified_xingyuan_fail "${name} Edge BFF CORS preflight did not allow credentials"
  allowed_headers="$(response_header "${headers_file}" Access-Control-Allow-Headers | tr '[:upper:]' '[:lower:]')"
  [[ ",${allowed_headers//[[:space:]]/}," == *',x-csrf-token,'* ]] \
    || unified_xingyuan_fail "${name} Edge BFF CORS preflight did not allow X-CSRF-TOKEN"
done

printf '%s\n' 'live-frontend-login: Edge OIDC, anonymous session, CSRF, and five frontend CORS preflights PASS'
