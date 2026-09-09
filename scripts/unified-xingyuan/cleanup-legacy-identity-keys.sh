#!/usr/bin/env bash
set -euo pipefail

# This script owns only identity-runtime keys from the pre-stateless layout.
# It deliberately never scans or flushes an entire Redis database.

execute=false
endpoint=

usage() {
  cat <<'USAGE'
Usage: cleanup-legacy-identity-keys.sh [--endpoint host:port/database] [--execute]

The default is a dry run. --execute is destructive and requires an explicit
--endpoint. Redis authentication is taken from REDISCLI_AUTH, as supported by
redis-cli; no password is accepted on the command line.
USAGE
}

fail() {
  printf 'legacy-identity-key-cleanup: %s\n' "$*" >&2
  exit 2
}

while (($# > 0)); do
  case "$1" in
    --endpoint)
      (($# >= 2)) || fail '--endpoint requires host:port/database'
      endpoint="$2"
      shift 2
      ;;
    --execute)
      execute=true
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      fail "unknown argument: $1"
      ;;
  esac
done

[[ -n "${endpoint}" ]] || {
  if [[ "${execute}" == true ]]; then
    fail '--execute requires an explicit --endpoint host:port/database'
  fi
  fail 'an explicit --endpoint host:port/database is required for a dry run'
}

[[ "${endpoint}" != *'*'* && "${endpoint}" != *'?'* ]] \
  || fail 'wildcard Redis endpoints are not allowed'

endpoint_host_port="${endpoint%%/*}"
database="${endpoint##*/}"
host="${endpoint_host_port%:*}"
port="${endpoint_host_port##*:}"
[[ "${endpoint}" == */* && -n "${host}" && -n "${port}" && -n "${database}" ]] \
  || fail 'endpoint must have the form host:port/database'
[[ "${port}" =~ ^[0-9]{1,5}$ && "${database}" =~ ^[0-9]+$ ]] \
  || fail 'endpoint port and database must be numeric'
((port >= 1 && port <= 65535)) \
  || fail 'endpoint port must be between 1 and 65535'

command -v redis-cli >/dev/null 2>&1 \
  || fail 'redis-cli is required'

# These are intentionally narrow. The rbac3 snapshot cache is derived state,
# so clearing the old/new snapshot entries is safe; durable RBAC3 data is not
# addressed by this script. No DDC, Gateway, or business prefix is included.
legacy_patterns=(
  'identity:v1:sso-session:*'
  'identity:v1:auth-code:*'
  'identity:v1:refresh-family:*'
  'identity:v1:refresh:*'
  'identity:v1:refresh-index:user:*'
  'identity:v1:user:*'
  'rbac3:*:session:*'
  'rbac3:*:snapshot:*'
  'rbac3:*:fence:session:*'
  'rbac3:*:key-ring'
)

temporary_dir="$(mktemp -d "${TMPDIR:-/tmp}/egon-legacy-identity-keys.XXXXXX")"
trap 'rm -rf "${temporary_dir}"' EXIT
key_file="${temporary_dir}/keys"
: >"${key_file}"

redis_args=(--raw -h "${host}" -p "${port}" -n "${database}")
declare -a pattern_files=()
for pattern in "${legacy_patterns[@]}"; do
  pattern_file="${temporary_dir}/pattern-${#pattern_files[@]}"
  redis-cli "${redis_args[@]}" --scan --pattern "${pattern}" \
    | sort -u >"${pattern_file}"
  pattern_files+=("${pattern_file}")
  cat "${pattern_file}" >>"${key_file}"
done

sort -u -o "${key_file}" "${key_file}"
printf 'legacy-identity-key-cleanup: mode=%s endpoint=%s\n' \
  "$([[ "${execute}" == true ]] && printf EXECUTE || printf DRY-RUN)" \
  "${endpoint}"

for index in "${!legacy_patterns[@]}"; do
  pattern="${legacy_patterns[${index}]}"
  count="$(wc -l <"${pattern_files[${index}]}")"
  count="${count//[[:space:]]/}"
  printf 'prefix=%s count=%s\n' "${pattern}" "${count}"
done
total="$(wc -l <"${key_file}")"
total="${total//[[:space:]]/}"

if [[ "${execute}" == true ]]; then
  while IFS= read -r key; do
    [[ -n "${key}" ]] || continue
    redis-cli "${redis_args[@]}" UNLINK "${key}" >/dev/null
  done <"${key_file}"
  printf 'removed=%s\n' "${total}"
else
  while IFS= read -r key; do
    [[ -n "${key}" ]] || continue
    printf 'key=%s\n' "${key}"
  done <"${key_file}"
  printf 'would-remove=%s\n' "${total}"
fi
