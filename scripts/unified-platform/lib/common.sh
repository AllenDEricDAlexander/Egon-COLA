#!/usr/bin/env bash

unified_platform_script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
unified_platform_repo_root="$(cd "${unified_platform_script_dir}/../.." && pwd)"
unified_platform_runtime_dir="${UNIFIED_PLATFORM_RUNTIME_DIR:-${unified_platform_repo_root}/target/local-unified-platform}"

case "${unified_platform_runtime_dir}" in
  "${unified_platform_repo_root}/target/"*) ;;
  *)
    printf 'unified-platform: runtime directory must be below %s/target: %s\n' \
      "${unified_platform_repo_root}" "${unified_platform_runtime_dir}" >&2
    exit 2
    ;;
esac

if [[ "${unified_platform_runtime_dir}" == "${unified_platform_repo_root}/target/" ]]; then
  printf 'unified-platform: runtime directory must not be the project target directory\n' >&2
  exit 2
fi

unified_platform_pid_dir="${unified_platform_runtime_dir}/pids"
unified_platform_log_dir="${unified_platform_runtime_dir}/logs"
unified_platform_secret_dir="${unified_platform_runtime_dir}/secrets"
unified_platform_env_dir="${unified_platform_runtime_dir}/env"
unified_platform_evidence_dir="${unified_platform_runtime_dir}/evidence"

IDP_BASE_URL="${IDP_BASE_URL:-http://127.0.0.1:18120}"
IDP_ADMIN_WEB_URL="${IDP_ADMIN_WEB_URL:-http://127.0.0.1:18121}"
RBAC3_BASE_URL="${RBAC3_BASE_URL:-http://127.0.0.1:18130}"
RBAC3_ADMIN_WEB_URL="${RBAC3_ADMIN_WEB_URL:-http://127.0.0.1:18131}"
GATEWAY_ADMIN_BASE_URL="${GATEWAY_ADMIN_BASE_URL:-http://127.0.0.1:18140}"
GATEWAY_ADMIN_WEB_URL="${GATEWAY_ADMIN_WEB_URL:-http://127.0.0.1:18141}"
DDC_BASE_URL="${DDC_BASE_URL:-http://127.0.0.1:18150}"
DDC_ADMIN_WEB_URL="${DDC_ADMIN_WEB_URL:-http://127.0.0.1:18152}"
MOCK_BACKEND_BASE_URL="${MOCK_BACKEND_BASE_URL:-http://127.0.0.1:18160}"
MCP_PROVIDER_BASE_URL="${MCP_PROVIDER_BASE_URL:-http://127.0.0.1:18161}"
MCP_REMOTE_BASE_URL="${MCP_REMOTE_BASE_URL:-http://127.0.0.1:18151}"
GATEWAY_BASE_URL="${GATEWAY_BASE_URL:-http://127.0.0.1:18180}"
GATEWAY_ENGINE_A_BASE_URL="${GATEWAY_ENGINE_A_BASE_URL:-http://127.0.0.1:18182}"
GATEWAY_ENGINE_B_BASE_URL="${GATEWAY_ENGINE_B_BASE_URL:-http://127.0.0.1:18183}"
GATEWAY_ENGINE_B_PUBLIC_URL="${GATEWAY_ENGINE_B_PUBLIC_URL:-http://127.0.0.1:18184}"

unified_platform_fail() {
  printf 'unified-platform: %s\n' "$*" >&2
  exit 1
}

unified_platform_stage() {
  printf '[unified-platform] %s\n' "$1"
}

unified_platform_initialize_directories() {
  umask 077
  mkdir -p \
    "${unified_platform_pid_dir}" \
    "${unified_platform_log_dir}" \
    "${unified_platform_secret_dir}" \
    "${unified_platform_env_dir}" \
    "${unified_platform_evidence_dir}"
  chmod 700 \
    "${unified_platform_runtime_dir}" \
    "${unified_platform_pid_dir}" \
    "${unified_platform_log_dir}" \
    "${unified_platform_secret_dir}" \
    "${unified_platform_env_dir}" \
    "${unified_platform_evidence_dir}"
}

unified_platform_process_running() {
  local name="$1" pid_file="${unified_platform_pid_dir}/$1.pid" pid
  [[ -s "${pid_file}" ]] || return 1
  pid="$(<"${pid_file}")"
  [[ "${pid}" =~ ^[0-9]+$ ]] && kill -0 "${pid}" 2>/dev/null
}

unified_platform_pid() {
  local name="$1"
  if unified_platform_process_running "${name}"; then
    printf '%s' "$(<"${unified_platform_pid_dir}/${name}.pid")"
  else
    printf '%s' '-'
  fi
}

unified_platform_http_code() {
  curl --max-time 3 -sS -o /dev/null -w '%{http_code}' "$1" 2>/dev/null || true
}

unified_platform_wait_http() {
  local name="$1" url="$2" attempts="${3:-90}" attempt status
  for ((attempt = 1; attempt <= attempts; attempt++)); do
    if ! unified_platform_process_running "${name}"; then
      tail -80 "${unified_platform_log_dir}/${name}.log" >&2 2>/dev/null || true
      unified_platform_fail "${name} exited before becoming ready"
    fi
    status="$(unified_platform_http_code "${url}")"
    if [[ "${status}" == "200" ]]; then
      return
    fi
    sleep 1
  done
  tail -80 "${unified_platform_log_dir}/${name}.log" >&2 2>/dev/null || true
  unified_platform_fail "${name} did not become ready at ${url}"
}

unified_platform_start_jar() {
  local name="$1" env_file="$2" jar="$3"
  shift 3
  if unified_platform_process_running "${name}"; then
    return
  fi
  [[ -s "${env_file}" ]] || unified_platform_fail "missing environment file: ${env_file}"
  [[ -s "${jar}" ]] || unified_platform_fail "missing executable jar: ${jar}"
  (
    set -a
    # shellcheck disable=SC1090
    source "${env_file}"
    set +a
    exec nohup java -jar "${jar}" "$@"
  ) >"${unified_platform_log_dir}/${name}.log" 2>&1 </dev/null &
  printf '%s' "$!" >"${unified_platform_pid_dir}/${name}.pid"
  chmod 600 "${unified_platform_pid_dir}/${name}.pid"
}

unified_platform_stop_process() {
  local name="$1" pid_file="${unified_platform_pid_dir}/$1.pid" pid attempt
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

unified_platform_write_env() {
  local file="$1" key="$2" value="$3"
  printf '%s=%q\n' "${key}" "${value}" >>"${file}"
}

unified_platform_require_command() {
  command -v "$1" >/dev/null 2>&1 \
    || unified_platform_fail "missing prerequisite: $1"
}
