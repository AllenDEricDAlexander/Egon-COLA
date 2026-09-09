#!/usr/bin/env bash

unified_xingyuan_script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
unified_xingyuan_repo_root="$(cd "${unified_xingyuan_script_dir}/../.." && pwd)"
unified_xingyuan_runtime_dir="${UNIFIED_XINGYUAN_RUNTIME_DIR:-${unified_xingyuan_repo_root}/target/local-unified-xingyuan}"

case "${unified_xingyuan_runtime_dir}" in
  "${unified_xingyuan_repo_root}/target/"*) ;;
  *)
    printf 'unified-xingyuan: runtime directory must be below %s/target: %s\n' \
      "${unified_xingyuan_repo_root}" "${unified_xingyuan_runtime_dir}" >&2
    exit 2
    ;;
esac

if [[ "${unified_xingyuan_runtime_dir}" == "${unified_xingyuan_repo_root}/target/" ]]; then
  printf 'unified-xingyuan: runtime directory must not be the project target directory\n' >&2
  exit 2
fi

unified_xingyuan_pid_dir="${unified_xingyuan_runtime_dir}/pids"
unified_xingyuan_log_dir="${unified_xingyuan_runtime_dir}/logs"
unified_xingyuan_secret_dir="${unified_xingyuan_runtime_dir}/secrets"
unified_xingyuan_env_dir="${unified_xingyuan_runtime_dir}/env"
unified_xingyuan_evidence_dir="${unified_xingyuan_runtime_dir}/evidence"

TIANQUAN_SHOUBING_BASE_URL="${TIANQUAN_SHOUBING_BASE_URL:-http://127.0.0.1:18120}"
TIANQUAN_SHOUBING_RPC_TARGET="${TIANQUAN_SHOUBING_RPC_TARGET:-dns:///127.0.0.1:18122}"
TIANQUAN_SHOUBING_ADMIN_WEB_URL="${TIANQUAN_SHOUBING_ADMIN_WEB_URL:-http://127.0.0.1:18121}"
TIANQUAN_JIANSHEN_BASE_URL="${TIANQUAN_JIANSHEN_BASE_URL:-http://127.0.0.1:18130}"
TIANQUAN_JIANSHEN_ADMIN_WEB_URL="${TIANQUAN_JIANSHEN_ADMIN_WEB_URL:-http://127.0.0.1:18131}"
YUHENG_ADMIN_BASE_URL="${YUHENG_ADMIN_BASE_URL:-http://127.0.0.1:18140}"
YUHENG_ADMIN_WEB_URL="${YUHENG_ADMIN_WEB_URL:-http://127.0.0.1:18141}"
TIANSHU_BASE_URL="${TIANSHU_BASE_URL:-http://127.0.0.1:18150}"
TIANSHU_RPC_TARGET="${TIANSHU_RPC_TARGET:-dns:///127.0.0.1:19080}"
TIANSHU_ADMIN_WEB_URL="${TIANSHU_ADMIN_WEB_URL:-http://127.0.0.1:18152}"
PLATFORM_PORTAL_URL="${PLATFORM_PORTAL_URL:-http://127.0.0.1:18125}"
MOCK_BACKEND_BASE_URL="${MOCK_BACKEND_BASE_URL:-http://127.0.0.1:18160}"
MCP_PROVIDER_BASE_URL="${MCP_PROVIDER_BASE_URL:-http://127.0.0.1:18161}"
MCP_REMOTE_BASE_URL="${MCP_REMOTE_BASE_URL:-http://127.0.0.1:18151}"
YUHENG_BASE_URL="${YUHENG_BASE_URL:-http://127.0.0.1:18180}"
YUHENG_MCP_BASE_URL="${YUHENG_MCP_BASE_URL:-http://127.0.0.1:18185}"
YUHENG_MCP_ENGINE_BASE_URL="${YUHENG_MCP_ENGINE_BASE_URL:-http://127.0.0.1:18186}"
YUHENG_ENGINE_A_BASE_URL="${YUHENG_ENGINE_A_BASE_URL:-http://127.0.0.1:18182}"
YUHENG_ENGINE_B_BASE_URL="${YUHENG_ENGINE_B_BASE_URL:-http://127.0.0.1:18183}"
YUHENG_ENGINE_B_PUBLIC_URL="${YUHENG_ENGINE_B_PUBLIC_URL:-http://127.0.0.1:18184}"

unified_xingyuan_fail() {
  printf 'unified-xingyuan: %s\n' "$*" >&2
  exit 1
}

unified_xingyuan_stage() {
  printf '[unified-xingyuan] %s\n' "$1"
}

unified_xingyuan_initialize_directories() {
  umask 077
  mkdir -p \
    "${unified_xingyuan_pid_dir}" \
    "${unified_xingyuan_log_dir}" \
    "${unified_xingyuan_secret_dir}" \
    "${unified_xingyuan_env_dir}" \
    "${unified_xingyuan_evidence_dir}"
  chmod 700 \
    "${unified_xingyuan_runtime_dir}" \
    "${unified_xingyuan_pid_dir}" \
    "${unified_xingyuan_log_dir}" \
    "${unified_xingyuan_secret_dir}" \
    "${unified_xingyuan_env_dir}" \
    "${unified_xingyuan_evidence_dir}"
}

unified_xingyuan_process_running() {
  local name="$1" pid_file="${unified_xingyuan_pid_dir}/$1.pid" pid
  [[ -s "${pid_file}" ]] || return 1
  pid="$(<"${pid_file}")"
  [[ "${pid}" =~ ^[0-9]+$ ]] && kill -0 "${pid}" 2>/dev/null
}

unified_xingyuan_pid() {
  local name="$1"
  if unified_xingyuan_process_running "${name}"; then
    printf '%s' "$(<"${unified_xingyuan_pid_dir}/${name}.pid")"
  else
    printf '%s' '-'
  fi
}

unified_xingyuan_http_code() {
  curl --max-time 3 -sS -o /dev/null -w '%{http_code}' "$1" 2>/dev/null || true
}

unified_xingyuan_wait_http() {
  local name="$1" url="$2" attempts="${3:-90}" attempt status
  for ((attempt = 1; attempt <= attempts; attempt++)); do
    if ! unified_xingyuan_process_running "${name}"; then
      tail -80 "${unified_xingyuan_log_dir}/${name}.log" >&2 2>/dev/null || true
      unified_xingyuan_fail "${name} exited before becoming ready"
    fi
    status="$(unified_xingyuan_http_code "${url}")"
    if [[ "${status}" == "200" ]]; then
      return
    fi
    sleep 1
  done
  tail -80 "${unified_xingyuan_log_dir}/${name}.log" >&2 2>/dev/null || true
  unified_xingyuan_fail "${name} did not become ready at ${url}"
}

unified_xingyuan_start_jar() {
  local name="$1" env_file="$2" jar="$3"
  shift 3
  if unified_xingyuan_process_running "${name}"; then
    return
  fi
  [[ -s "${env_file}" ]] || unified_xingyuan_fail "missing environment file: ${env_file}"
  [[ -s "${jar}" ]] || unified_xingyuan_fail "missing executable jar: ${jar}"
  (
    set -a
    # shellcheck disable=SC1090
    source "${env_file}"
    set +a
    exec nohup java -jar "${jar}" "$@"
  ) >"${unified_xingyuan_log_dir}/${name}.log" 2>&1 </dev/null &
  printf '%s' "$!" >"${unified_xingyuan_pid_dir}/${name}.pid"
  chmod 600 "${unified_xingyuan_pid_dir}/${name}.pid"
}

unified_xingyuan_stop_process() {
  local name="$1" pid_file="${unified_xingyuan_pid_dir}/$1.pid" pid attempt
  [[ -s "${pid_file}" ]] || return 0
  pid="$(<"${pid_file}")"
  if [[ "${pid}" =~ ^[0-9]+$ ]] && kill -0 "${pid}" 2>/dev/null; then
    kill "${pid}"
    for ((attempt = 1; attempt <= 30; attempt++)); do
      kill -0 "${pid}" 2>/dev/null || break
      sleep 1
    done
    if kill -0 "${pid}" 2>/dev/null; then
      kill -KILL "${pid}"
    fi
  fi
  rm -f "${pid_file}"
}

unified_xingyuan_write_env() {
  local file="$1" key="$2" value="$3"
  printf '%s=%q\n' "${key}" "${value}" >>"${file}"
}

unified_xingyuan_write_property() {
  local file="$1" key="$2" value="$3"
  value="${value//\\/\\\\}"
  value="${value//$'\t'/\\t}"
  value="${value//$'\r'/\\r}"
  value="${value//$'\n'/\\n}"
  printf '%s=%s\n' "${key}" "${value}" >>"${file}"
}

unified_xingyuan_local_build_id() {
  local jar="$1" digest
  [[ -s "${jar}" ]] \
    || unified_xingyuan_fail "missing executable jar for build identity: ${jar}"
  digest="$(openssl dgst -sha256 -r "${jar}" | awk '{print $1}')"
  [[ "${digest}" =~ ^[0-9a-f]{64}$ ]] \
    || unified_xingyuan_fail "invalid executable jar digest: ${jar}"
  printf 'local-%s' "${digest:0:16}"
}

unified_xingyuan_write_frontend_login_env() {
  local web_dir="$1" tenant_id="$2"
  local file="${web_dir}/.env.local"
  local marker='# Managed by scripts/unified-xingyuan for Yuheng USER cookies.'
  local legacy_marker='# Managed by scripts/unified-xingyuan for local SSO.'
  [[ "${tenant_id}" =~ ^[1-9][0-9]*$ ]] \
    || unified_xingyuan_fail "default tenant ID must be a positive integer"
  [[ -d "${web_dir}" ]] \
    || unified_xingyuan_fail "frontend directory is missing: ${web_dir}"
  if [[ -e "${file}" ]] \
      && [[ "$(sed -n '1p' "${file}")" != "${marker}" ]] \
      && [[ "$(sed -n '1p' "${file}")" != "${legacy_marker}" ]]; then
    unified_xingyuan_fail \
      "refusing to overwrite unmanaged frontend environment: ${file}"
  fi
  # Vite only inherits shell exports for that process. Persist both values so
  # a later plain npm run dev uses the same authenticated Yuheng entry point.
  printf '%s\nVITE_DEFAULT_TENANT_ID=%s\nVITE_YUHENG_ORIGIN=%s\n' \
    "${marker}" "${tenant_id}" "${YUHENG_BASE_URL}" >"${file}"
  chmod 600 "${file}"
}

unified_xingyuan_require_command() {
  command -v "$1" >/dev/null 2>&1 \
    || unified_xingyuan_fail "missing prerequisite: $1"
}
