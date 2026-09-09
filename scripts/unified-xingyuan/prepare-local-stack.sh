#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${script_dir}/lib/common.sh"

if [[ "${1:-}" == '--static-only' ]]; then
  exec "${script_dir}/verify-local-stack.sh" --static-only
fi

for command in java npm curl jq openssl psql createdb redis-cli awk; do
  unified_xingyuan_require_command "${command}"
done

ensure_frontend_dependencies() {
  local install_dir="$1" vite="$2" label="$3"
  if [[ -x "${vite}" ]]; then
    return
  fi
  unified_xingyuan_stage "installing locked ${label} dependencies"
  (
    cd "${install_dir}"
    npm ci
  )
}

ensure_frontend_dependencies \
  "${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web" \
  "${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/node_modules/.bin/vite" \
  'Tianquan-Shoubing Admin Web'
ensure_frontend_dependencies \
  "${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-tianquan-jianshen" \
  "${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-tianquan-jianshen/node_modules/.bin/vite" \
  'Tianquan-Jianshen workspace'
ensure_frontend_dependencies \
  "${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web" \
  "${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/node_modules/.bin/vite" \
  'Yuheng Admin Web'
ensure_frontend_dependencies \
  "${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web" \
  "${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/node_modules/.bin/vite" \
  'Tianshu Admin Web'
ensure_frontend_dependencies \
  "${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-xingyuan-admin-portal" \
  "${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-xingyuan-admin-portal/node_modules/.bin/vite" \
  'Xingyuan Admin Portal'

cleanup_required=true
cleanup() {
  local status=$?
  if [[ "${cleanup_required}" == "true" ]]; then
    "${script_dir}/stop-local-stack.sh" >/dev/null 2>&1 || true
  fi
  exit "${status}"
}
trap cleanup EXIT

unified_xingyuan_stage 'initializing databases, JWT authorization, Tianshu, and Yuheng topology'
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
  [[ -s "${unified_xingyuan_repo_root}/${relative_path}" ]] \
    || unified_xingyuan_fail "missing executable JAR: ${relative_path}"
done

for service in tianquan-shoubing tianquan-jianshen yuheng-admin yuheng-biz-gateway tianshu; do
  properties_file="${unified_xingyuan_env_dir}/${service}.properties"
  [[ -s "${properties_file}" ]] \
    || unified_xingyuan_fail "missing runtime properties: ${properties_file}"
  [[ "$(stat -f '%Lp' "${properties_file}")" == '600' ]] \
    || unified_xingyuan_fail "runtime properties must have mode 600: ${properties_file}"
done

grep -q '^TIANQUAN_JIANSHEN_DEVELOPMENT_IDENTITY_SUB=.' \
  "${unified_xingyuan_env_dir}/tianquan-jianshen.properties" \
  || unified_xingyuan_fail 'Tianquan-Jianshen direct-run identity binding was not initialized'

for web_dir in \
  "${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web" \
  "${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web" \
  "${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web" \
  "${unified_xingyuan_repo_root}/egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web"; do
  [[ -s "${web_dir}/.env.local" ]] \
    || unified_xingyuan_fail "missing generated frontend login environment: ${web_dir}"
done

printf 'Direct-run artifacts are ready. Runtime configuration: %s\n' \
  "${unified_xingyuan_env_dir}"
printf 'Run the documented java -jar and npm run dev commands from %s.\n' \
  "${unified_xingyuan_repo_root}"
