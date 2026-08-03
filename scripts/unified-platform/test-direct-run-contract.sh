#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"
identity_script="${repo_root}/scripts/unified-identity-local.sh"

fail() {
  printf 'direct-run-contract: %s\n' "$*" >&2
  exit 1
}

assert_contains() {
  local file="$1" expected="$2" context="$3"
  grep -Fq -- "${expected}" "${file}" \
    || fail "${context}: missing ${expected}"
}

extract_function() {
  local name="$1" output="$2"
  awk -v signature="${name}()" '
    $0 == signature " {" {copying = 1}
    copying {print}
    copying && $0 == "}" {exit}
  ' "${identity_script}" >"${output}"
  [[ -s "${output}" ]] || fail "${name} function is missing"
}

temporary_dir="$(mktemp -d "${TMPDIR:-/tmp}/egon-direct-run-contract.XXXXXX")"
trap 'rm -rf "${temporary_dir}"' EXIT

function_file="${temporary_dir}/properties-escape.sh"
extract_function properties_escape "${function_file}"
# shellcheck disable=SC1090
source "${function_file}"

escaped="$(properties_escape $'a\\b\tc\rd\ne')"
[[ "${escaped}" == 'a\\b\tc\rd\ne' ]] \
  || fail "properties_escape did not encode Java properties control characters"

assert_contains "${identity_script}" '${file%.env}.properties' \
  'write_env must target the sibling Java properties file'
assert_contains "${identity_script}" 'properties_escape "${value}"' \
  'write_env must encode the Java properties value'
assert_contains "${identity_script}" 'chmod 600 "${file}" "${properties_file}"' \
  'new_env_file must protect both runtime configuration files'

assert_service_config() {
  local relative_file="$1" service="$2" file="${repo_root}/$1"
  assert_contains "${file}" 'default: local' \
    "${service} must use the local profile when no profile is supplied"
  assert_contains "${file}" \
    "optional:file:\${UNIFIED_PLATFORM_RUNTIME_DIR:target/local-unified-platform}/env/${service}.properties" \
    "${service} must import its generated runtime properties"
}

assert_service_config \
  'egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/resources/application.yml' \
  idp
assert_service_config \
  'egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/application.yml' \
  rbac3
assert_service_config \
  'egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/resources/application.yml' \
  gateway-admin
assert_service_config \
  'egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/resources/application.yml' \
  gateway-engine
ddc_config='egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/resources/application.yml'
assert_service_config "${ddc_config}" ddc
assert_contains "${repo_root}/${ddc_config}" \
  'classpath:META-INF/egon-cola-ddc.properties' \
  'DDC must preserve its starter defaults import'

assert_vite_proxy() {
  local relative_file="$1" platform="$2" port="$3"
  assert_contains "${repo_root}/${relative_file}" "http://127.0.0.1:${port}" \
    "${platform} plain npm run dev must proxy to its local backend"
}

assert_vite_proxy \
  'egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/vite.config.ts' \
  IdP 18120
assert_vite_proxy \
  'egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/vite.config.ts' \
  RBAC3 18130
assert_vite_proxy \
  'egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/vite.config.ts' \
  Gateway 18140
assert_vite_proxy \
  'egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/vite.config.ts' \
  DDC 18150

printf 'direct-run-contract: runtime properties adapter PASS\n'
