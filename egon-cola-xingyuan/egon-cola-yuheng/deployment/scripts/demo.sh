#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
deployment_dir="$(cd "${script_dir}/.." && pwd)"
repo_root="$(cd "${deployment_dir}/../../.." && pwd)"
runtime_dir="${YUHENG_DEMO_RUNTIME_DIR:-${deployment_dir}/.demo}"
env_file="${YUHENG_DEMO_ENV_FILE:-${deployment_dir}/.env}"
project_suffix="$(printf '%s' "${USER:-local}" | tr -c 'a-zA-Z0-9_-' '-')"
project_name="${YUHENG_DEMO_PROJECT:-egon-cola-yuheng-demo-${project_suffix}}"
admin_base="${YUHENG_DEMO_ADMIN_BASE:-http://127.0.0.1:18080}"
compose_files=(-f "${deployment_dir}/compose.yml" -f "${deployment_dir}/compose.demo.yml")

usage() {
  cat <<'USAGE'
Usage: ./scripts/demo.sh <command>

Commands:
  doctor       Check local prerequisites and render the Compose model
  build        Package demo applications and build their images
  up-control   Start PostgreSQL, both Redis instances, Kafka, Tianshu, Admin and Engines
  init         Create Admin JWT, applications, reporting credentials and group
  up-providers Start MVC, WebFlux and RPC providers
  publish      Resolve reported operations, publish routes/policy and wait for consistency
  up-consumer  Start the RPC consumer
  verify       Call HTTP/RPC paths and verify Admin runtime consistency
  logs         Collect current Compose logs under .demo/logs
  down         Stop the project while preserving volumes
  purge        Delete volumes only for this locally marked demo project
USAGE
}

require_env_file() {
  if [[ ! -f "${env_file}" ]]; then
    echo "missing ${env_file}; copy .env.example to .env and set local secrets" >&2
    exit 1
  fi
}

compose() {
  require_env_file
  docker compose --project-name "${project_name}" --env-file "${env_file}" "${compose_files[@]}" "$@"
}

load_env() {
  require_env_file
  set -a
  # shellcheck disable=SC1090
  source "${env_file}"
  set +a
}

token() {
  cat "${runtime_dir}/admin.jwt"
}

api() {
  local method="$1" path="$2" body="${3:-}"
  local args=(--fail --silent --show-error -X "${method}" -H "Authorization: Bearer $(token)" -H 'Content-Type: application/json')
  if [[ -n "${body}" ]]; then
    args+=(-d "${body}")
  fi
  curl "${args[@]}" "${admin_base}${path}"
}

write_provider_env() {
  local file="$1" access_key="$2" secret="$3"
  umask 077
  printf 'EGON_COLA_COMPONENT_YUHENG_REPORTING_ACCESS_KEY=%s\nEGON_COLA_COMPONENT_YUHENG_REPORTING_SECRET_KEY=%s\n' "${access_key}" "${secret}" >"${file}"
  chmod 600 "${file}"
}

command_doctor() {
  for command in docker curl jq openssl; do
    command -v "${command}" >/dev/null || { echo "missing prerequisite: ${command}" >&2; exit 1; }
  done
  compose config --quiet
  echo "Yuheng demo prerequisites and Compose model are valid."
}

command_build() {
  "${repo_root}/mvnw" -B -ntp -f "${repo_root}/pom.xml" \
    -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-http-provider,egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-webflux-http-provider,egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-rpc-provider,egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-rpc-consumer \
    -am package -DskipTests
  compose build http-provider-mvc http-provider-webflux rpc-provider rpc-consumer
}

command_up_control() {
  compose up -d postgres tianshu-redis rate-limit-redis kafka tianshu-admin yuheng-admin yuheng-biz-gateway yuheng-biz-gateway-2 yuheng-mcp-gateway yuheng-mcp-gateway-2 yuheng-data-plane-proxy
  "${script_dir}/wait-ready.sh" http://127.0.0.1:18070/actuator/health/readiness
  "${script_dir}/wait-ready.sh" http://127.0.0.1:18080/actuator/health/readiness
}

command_init() {
  load_env
  mkdir -p "${runtime_dir}"
  umask 077
  "${script_dir}/demo-token.sh" "${YUHENG_ADMIN_JWT_HMAC_SECRET_BASE64:?missing JWT secret}" >"${runtime_dir}/admin.jwt"
  chmod 600 "${runtime_dir}/admin.jwt"
  : >"${runtime_dir}/applications.tsv"
  while IFS= read -r application; do
    request="$(jq --arg bizCode "${YUHENG_BIZ_CODE:-default}" --arg env "${YUHENG_ENV:-local}" --arg namespace "${YUHENG_NAMESPACE:-default}" '. + {bizCode:$bizCode,env:$env,namespace:$namespace}' <<<"${application}")"
    created="$(api POST /api/v1/yuheng/admin/applications "${request}")"
    app_id="$(jq -er '.id' <<<"${created}")"
    app_code="$(jq -er '.applicationCode' <<<"${created}")"
    credential="$(api POST "/api/v1/yuheng/admin/applications/${app_id}/credentials" '{}')"
    printf '%s\t%s\n' "${app_code}" "${app_id}" >>"${runtime_dir}/applications.tsv"
    case "${app_code}" in
      yuheng-test-http-provider) write_provider_env "${runtime_dir}/http-provider.env" "$(jq -er '.accessKey' <<<"${credential}")" "$(jq -er '.secret' <<<"${credential}")" ;;
      yuheng-test-rpc-provider) write_provider_env "${runtime_dir}/rpc-provider.env" "$(jq -er '.accessKey' <<<"${credential}")" "$(jq -er '.secret' <<<"${credential}")" ;;
    esac
  done < <(jq -c '.[]' "${deployment_dir}/demo/applications.json")
  group="$(api POST /api/v1/yuheng/admin/yuheng-groups "$(jq -n --arg env "${YUHENG_ENV:-local}" --arg namespace "${YUHENG_NAMESPACE:-default}" '{gatewayGroupCode:"default",displayName:"Yuheng Demo",env:$env,namespace:$namespace,description:"Yuheng Tianshu RPC integration demo"}')")"
  jq -er '.id' <<<"${group}" >"${runtime_dir}/group.id"
  touch "${runtime_dir}/.local-demo-marker"
  chmod 600 "${runtime_dir}"/*
  echo "Yuheng demo control-plane objects initialized."
}

command_up_providers() {
  [[ -s "${runtime_dir}/http-provider.env" && -s "${runtime_dir}/rpc-provider.env" ]] || { echo "run init first" >&2; exit 1; }
  compose up -d http-provider-mvc http-provider-webflux rpc-provider
  "${script_dir}/wait-ready.sh" http://127.0.0.1:18094/actuator/health/readiness
  "${script_dir}/wait-ready.sh" http://127.0.0.1:18095/actuator/health/readiness
  "${script_dir}/wait-ready.sh" http://127.0.0.1:18086/actuator/health/readiness
}

application_id() {
  awk -F '\t' -v code="$1" '$1 == code {print $2}' "${runtime_dir}/applications.tsv"
}

operation_id() {
  local app_code="$1" method_identity="$2"
  api GET "/api/v1/yuheng/admin/applications/$(application_id "${app_code}")/catalog" | jq -er --arg method "${method_identity}" '.. | objects | select(.methodIdentity? == $method) | .id' | head -n 1
}

command_publish() {
  group_id="$(cat "${runtime_dir}/group.id")"
  revision=0
  while IFS= read -r route; do
    route_id="$(jq -er '.routeId' <<<"${route}")"
    op_id="$(operation_id "$(jq -er '.applicationCode' <<<"${route}")" "$(jq -er '.methodIdentity' <<<"${route}")")"
    request="$(jq --arg operationId "${op_id}" --argjson revision "${revision}" '{operationId:$operationId,content:{host:.host,httpMethod:.httpMethod,pathPattern:.pathPattern,accessZones:.accessZones,priority:.priority},enabled:true,expectedRevision:$revision,idempotencyKey:("demo-route-" + .routeId),changeReason:"Yuheng demo publish"}' <<<"${route}")"
    result="$(api PUT "/api/v1/yuheng/admin/yuheng-groups/${group_id}/draft/routes/${route_id}" "${request}")"
    revision="$(jq -er '.revision' <<<"${result}")"
  done < <(jq -c '.[]' "${deployment_dir}/demo/routes.json")
  while IFS= read -r policy; do
    policy_id="$(jq -er '.policyId' <<<"${policy}")"
    op_id="$(operation_id yuheng-test-http-provider "$(jq -er '.operationMethodIdentity' <<<"${policy}")")"
    request="$(jq --arg operationId "${op_id}" --argjson revision "${revision}" '{policyType:.policyType,policyScope:.policyScope,content:{operationIds:[$operationId],keyExpression:.keyExpression,capacity:.capacity,initialTokens:.initialTokens,refillTokens:.refillTokens,refillPeriod:.refillPeriod,mode:.mode},enabled:true,expectedRevision:$revision,idempotencyKey:("demo-policy-" + .policyId),changeReason:"Yuheng demo publish"}' <<<"${policy}")"
    result="$(api PUT "/api/v1/yuheng/admin/yuheng-groups/${group_id}/draft/policies/${policy_id}" "${request}")"
    revision="$(jq -er '.revision' <<<"${result}")"
  done < <(jq -c '.[]' "${deployment_dir}/demo/policies.json")
  api POST "/api/v1/yuheng/admin/yuheng-groups/${group_id}/draft/validate" '{}' | jq -e '.valid == true' >/dev/null
  release="$(api POST "/api/v1/yuheng/admin/yuheng-groups/${group_id}/releases" "$(jq -n --argjson revision "${revision}" '{expectedDraftRevision:$revision,changeReason:"Yuheng demo release"}')")"
  jq -e '.status == "SUCCEEDED"' <<<"${release}" >/dev/null
  "${script_dir}/wait-ready.sh" --engines
  deadline="$((SECONDS + 120))"
  until api GET "/api/v1/yuheng/admin/yuheng-groups/${group_id}/runtime-consistency" | jq -e '.consistent == true and .readyEngineNodeCount == 4' >/dev/null; do
    ((SECONDS < deadline)) || { echo "API_RPC/MCP release ACK convergence timed out" >&2; exit 1; }
    sleep 2
  done
  echo "Yuheng demo rules published: $(jq -r '.releaseId' <<<"${release}")"
}

command_up_consumer() {
  compose up -d rpc-consumer
  "${script_dir}/wait-ready.sh" http://127.0.0.1:18087/actuator/health/readiness
}

command_verify() {
  group_id="$(cat "${runtime_dir}/group.id")"
  curl --fail --silent --show-error -H 'Host: providers.yuheng.demo' http://127.0.0.1:18081/api/providers/demo-http | jq -e '.framework == "mvc" or .framework == "webflux"' >/dev/null
  curl --fail --silent --show-error 'http://127.0.0.1:18087/test/rpc/echo?message=demo-rpc' | jq -e '.message == "demo-rpc"' >/dev/null
  api GET "/api/v1/yuheng/admin/yuheng-groups/${group_id}/runtime-consistency" | jq -e '.consistent == true and .readyEngineNodeCount == 4' >/dev/null
  echo "Yuheng demo verification passed."
}

command_logs() {
  mkdir -p "${runtime_dir}/logs"
  compose logs --no-color >"${runtime_dir}/logs/compose.log"
  echo "Logs written to ${runtime_dir}/logs/compose.log"
}

command_down() {
  compose down --remove-orphans
}

command_purge() {
  marker="${runtime_dir}/.local-demo-marker"
  [[ -f "${marker}" ]] || { echo "refusing purge: local demo marker is missing" >&2; exit 1; }
  [[ "${project_name}" == egon-cola-yuheng-demo-* ]] || { echo "refusing purge for a non-demo Compose project" >&2; exit 1; }
  compose down --volumes --remove-orphans
}

case "${1:---help}" in
  --help|-h|help) usage ;;
  doctor) command_doctor ;;
  build) command_build ;;
  up-control) command_up_control ;;
  init) command_init ;;
  up-providers) command_up_providers ;;
  publish) command_publish ;;
  up-consumer) command_up_consumer ;;
  verify) command_verify ;;
  logs) command_logs ;;
  down) command_down ;;
  purge) command_purge ;;
  *) usage >&2; exit 2 ;;
esac
