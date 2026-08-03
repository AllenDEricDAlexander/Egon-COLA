#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${script_dir}/lib/common.sh"

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
  "${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web" \
  "${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/node_modules/.bin/vite" \
  'IdP Admin Web'
ensure_frontend_dependencies \
  "${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-rbac3" \
  "${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-rbac3/node_modules/.bin/vite" \
  'RBAC3 workspace'
ensure_frontend_dependencies \
  "${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web" \
  "${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/node_modules/.bin/vite" \
  'Gateway Admin Web'
ensure_frontend_dependencies \
  "${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web" \
  "${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/node_modules/.bin/vite" \
  'DDC Admin Web'

cleanup_required=true
cleanup() {
  local status=$?
  if [[ "${cleanup_required}" == "true" ]]; then
    "${script_dir}/stop-local-stack.sh" >/dev/null 2>&1 || true
  fi
  exit "${status}"
}
trap cleanup EXIT

unified_platform_stage 'initializing databases, SSO, authorization, DDC, and Gateway topology'
"${script_dir}/start-local-stack.sh"
"${script_dir}/stop-local-stack.sh"
cleanup_required=false
trap - EXIT

jar_paths=(
  'egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/target/egon-cola-platform-idp-admin-exec.jar'
  'egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/target/egon-cola-platform-rbac3-admin-exec.jar'
  'egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/target/egon-cola-platform-gateway-admin-exec.jar'
  'egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/target/egon-cola-platform-gateway-engine-exec.jar'
  'egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/target/egon-cola-platform-dynamic-config-center-admin-exec.jar'
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
  "${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web" \
  "${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web" \
  "${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web" \
  "${unified_platform_repo_root}/egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web"; do
  [[ -s "${web_dir}/.env.local" ]] \
    || unified_platform_fail "missing generated frontend login environment: ${web_dir}"
done

printf 'Direct-run artifacts are ready. Runtime configuration: %s\n' \
  "${unified_platform_env_dir}"
printf 'Run the documented java -jar and npm run dev commands from %s.\n' \
  "${unified_platform_repo_root}"
