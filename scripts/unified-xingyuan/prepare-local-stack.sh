#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${script_dir}/lib/common.sh"

if [[ "${1:-}" == '--static-only' ]]; then
  exec "${script_dir}/verify-local-stack.sh" --static-only
fi

for command in java npm curl jq openssl psql createdb redis-cli awk; do
  unified_platform_require_command "${command}"
done

ensure_frontend_dependencies() {
  local install_dir="$1" vite="$2" label="$3"
  if [[ -x "${vite}" ]]; then
    return
  fi
  unified_platform_stage "installing locked ${label} dependencies"
  (
    cd "${install_dir}"
    npm ci
  )
}

ensure_frontend_dependencies \
  "${unified_platform_repo_root}/egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web" \
  "${unified_platform_repo_root}/egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/node_modules/.bin/vite" \
  'IdP Admin Web'
ensure_frontend_dependencies \
  "${unified_platform_repo_root}/egon-cola-xingyuan/egon-cola-tianquan-jianshen" \
  "${unified_platform_repo_root}/egon-cola-xingyuan/egon-cola-tianquan-jianshen/node_modules/.bin/vite" \
  'RBAC3 workspace'
ensure_frontend_dependencies \
  "${unified_platform_repo_root}/egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web" \
  "${unified_platform_repo_root}/egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/node_modules/.bin/vite" \
  'Gateway Admin Web'
ensure_frontend_dependencies \
  "${unified_platform_repo_root}/egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web" \
  "${unified_platform_repo_root}/egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/node_modules/.bin/vite" \
  'DDC Admin Web'
ensure_frontend_dependencies \
  "${unified_platform_repo_root}/egon-cola-xingyuan/egon-cola-xingyuan-admin-portal" \
  "${unified_platform_repo_root}/egon-cola-xingyuan/egon-cola-xingyuan-admin-portal/node_modules/.bin/vite" \
  'Platform Admin Portal'

cleanup_required=true
cleanup() {
  local status=$?
  if [[ "${cleanup_required}" == "true" ]]; then
    "${script_dir}/stop-local-stack.sh" >/dev/null 2>&1 || true
  fi
  exit "${status}"
}
trap cleanup EXIT

unified_platform_stage 'initializing databases, JWT authorization, DDC, and Gateway topology'
"${script_dir}/start-local-stack.sh"
"${script_dir}/stop-local-stack.sh"
cleanup_required=false
trap - EXIT

jar_paths=(
  'egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/target/egon-cola-tianquan-shoubing-admin-exec.jar'
  'egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/target/egon-cola-tianquan-jianshen-admin-exec.jar'
  'egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/target/yuheng-admin-exec.jar'
  'egon-cola-xingyuan/egon-cola-yuheng/yuheng-biz-gateway/target/yuheng-biz-gateway-exec.jar'
  'egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/target/egon-cola-tianshu-admin-exec.jar'
)
for relative_path in "${jar_paths[@]}"; do
  [[ -s "${unified_platform_repo_root}/${relative_path}" ]] \
    || unified_platform_fail "missing executable JAR: ${relative_path}"
done

for service in idp rbac3 gateway-admin gateway-engine ddc; do
  properties_file="${unified_platform_env_dir}/${service}.properties"
  [[ -s "${properties_file}" ]] \
    || unified_platform_fail "missing runtime properties: ${properties_file}"
  [[ "$(stat -f '%Lp' "${properties_file}")" == '600' ]] \
    || unified_platform_fail "runtime properties must have mode 600: ${properties_file}"
done

grep -q '^RBAC3_DEVELOPMENT_IDENTITY_SUB=.' \
  "${unified_platform_env_dir}/rbac3.properties" \
  || unified_platform_fail 'RBAC3 direct-run identity binding was not initialized'

for web_dir in \
  "${unified_platform_repo_root}/egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web" \
  "${unified_platform_repo_root}/egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web" \
  "${unified_platform_repo_root}/egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web" \
  "${unified_platform_repo_root}/egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web"; do
  [[ -s "${web_dir}/.env.local" ]] \
    || unified_platform_fail "missing generated frontend login environment: ${web_dir}"
done

printf 'Direct-run artifacts are ready. Runtime configuration: %s\n' \
  "${unified_platform_env_dir}"
printf 'Run the documented java -jar and npm run dev commands from %s.\n' \
  "${unified_platform_repo_root}"
