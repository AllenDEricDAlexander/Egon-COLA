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

printf 'direct-run-contract: runtime properties adapter PASS\n'
