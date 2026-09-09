#!/usr/bin/env bash

set -euo pipefail
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

usage() {
  cat <<'EOF'
Usage: verify-yuheng-tianshu-topology.sh [--help|--check-config|--verify]

Queries an already-running two-instance Tianquan-Jianshen -> Tianshu -> Yuheng topology. It
proves Definition acknowledgement, two HTTP_PROVIDER leases, the explicit
Release, runtime consistency, and routed failover as separate observations.
The script never starts, stops, or signals a process; failover changes are
operator checkpoints.

Required identity variables:
  TIANQUAN_JIANSHEN_INSTANCE_1_ID, TIANQUAN_JIANSHEN_INSTANCE_2_ID, TIANQUAN_JIANSHEN_INSTANCE_1_PORT,
  TIANQUAN_JIANSHEN_INSTANCE_2_PORT, TIANQUAN_JIANSHEN_MACHINE_ID_1, TIANQUAN_JIANSHEN_MACHINE_ID_2,
  TIANQUAN_JIANSHEN_ARTIFACT_VERSION, TIANQUAN_JIANSHEN_BUILD_ID_1, TIANQUAN_JIANSHEN_BUILD_ID_2,
  TIANQUAN_JIANSHEN_TENANT_ID, TIANSHU_BIZ_CODE, DEPLOYMENT_ENV, DEPLOYMENT_NAMESPACE.

Required endpoints and credentials:
  TIANQUAN_JIANSHEN_ADMIN_1_BASE_URL, TIANQUAN_JIANSHEN_ADMIN_2_BASE_URL,
  TIANQUAN_JIANSHEN_TOPOLOGY_ACCESS_TOKEN_FILE, TIANSHU_ADMIN_BASE_URL,
  TIANSHU_STATUS_ACCESS_TOKEN_FILE, YUHENG_ADMIN_BASE_URL,
  YUHENG_STATUS_OAUTH_TOKEN_FILE, YUHENG_PUBLIC_ROUTE_URL,
  YUHENG_GROUP_ID, YUHENG_RELEASE_ID, YUHENG_FAIL_CLOSED_STATUS.
EOF
}

check_config() {
  rbac3_require_command curl
  rbac3_require_command jq
  local name
  for name in \
      TIANQUAN_JIANSHEN_INSTANCE_1_ID TIANQUAN_JIANSHEN_INSTANCE_2_ID TIANQUAN_JIANSHEN_INSTANCE_1_PORT \
      TIANQUAN_JIANSHEN_INSTANCE_2_PORT TIANQUAN_JIANSHEN_MACHINE_ID_1 TIANQUAN_JIANSHEN_MACHINE_ID_2 \
      TIANQUAN_JIANSHEN_ARTIFACT_VERSION TIANQUAN_JIANSHEN_BUILD_ID_1 TIANQUAN_JIANSHEN_BUILD_ID_2 \
      TIANQUAN_JIANSHEN_TENANT_ID TIANSHU_BIZ_CODE DEPLOYMENT_ENV DEPLOYMENT_NAMESPACE \
      YUHENG_GROUP_ID YUHENG_RELEASE_ID YUHENG_FAIL_CLOSED_STATUS; do
    rbac3_require_env "${name}"
  done
  for name in TIANQUAN_JIANSHEN_INSTANCE_1_PORT TIANQUAN_JIANSHEN_INSTANCE_2_PORT \
      TIANQUAN_JIANSHEN_MACHINE_ID_1 TIANQUAN_JIANSHEN_MACHINE_ID_2 YUHENG_FAIL_CLOSED_STATUS; do
    rbac3_validate_uint "${name}"
  done
  for name in TIANQUAN_JIANSHEN_ADMIN_1_BASE_URL TIANQUAN_JIANSHEN_ADMIN_2_BASE_URL \
      TIANSHU_ADMIN_BASE_URL YUHENG_ADMIN_BASE_URL YUHENG_PUBLIC_ROUTE_URL; do
    rbac3_require_env "${name}"
    rbac3_validate_http_url "${name}"
  done
  rbac3_require_secret_file TIANQUAN_JIANSHEN_TOPOLOGY_ACCESS_TOKEN_FILE
  rbac3_require_secret_file TIANSHU_STATUS_ACCESS_TOKEN_FILE
  rbac3_require_secret_file YUHENG_STATUS_OAUTH_TOKEN_FILE
  [[ "${TIANQUAN_JIANSHEN_INSTANCE_1_ID}" != "${TIANQUAN_JIANSHEN_INSTANCE_2_ID}" ]] \
    || rbac3_die "Admin instance IDs must be distinct"
  [[ "${TIANQUAN_JIANSHEN_INSTANCE_1_PORT}" != "${TIANQUAN_JIANSHEN_INSTANCE_2_PORT}" ]] \
    || rbac3_die "Admin ports must be distinct"
  [[ "${TIANQUAN_JIANSHEN_MACHINE_ID_1}" != "${TIANQUAN_JIANSHEN_MACHINE_ID_2}" ]] \
    || rbac3_die "Snowflake machine IDs must be distinct"
  [[ "${TIANQUAN_JIANSHEN_BUILD_ID_1}" != "${TIANQUAN_JIANSHEN_BUILD_ID_2}" ]] \
    || rbac3_die "build IDs must identify the two process artifacts distinctly"
  [[ "${YUHENG_FAIL_CLOSED_STATUS}" -ge 400 ]] \
    || rbac3_die "fail-closed status must be an error status"
}

encoded_query() {
  jq -rn --arg value "$1" '$value|@uri'
}

assert_admin_status() {
  local base_url="$1"
  local expected_instance="$2"
  local response
  response="$(rbac3_bearer_get "${TIANQUAN_JIANSHEN_TOPOLOGY_ACCESS_TOKEN_FILE}" \
    "${base_url%/}/api/tianquan-jianshen/v1/runtime/yuheng-tianshu-status")"
  jq -e --arg instance "${expected_instance}" --arg release "${YUHENG_RELEASE_ID}" '
    (.data | type) == "object"
    and (.data.definition.status == "ACCEPTED" or .data.definition.status == "ACCEPTED_WITH_WARNINGS")
    and .data.definition.definitionSetId != null
    and .data.providerLease.state == "REGISTERED"
    and .data.providerLease.instanceId == $instance
    and .data.gatewayRelease.releaseId == $release
    and .data.gatewayRelease.status == "ROUTABLE"
  ' <<< "${response}" >/dev/null
}

ddc_instances() {
  local biz_code app_code env namespace
  biz_code="$(encoded_query "${TIANSHU_BIZ_CODE}")"
  app_code="$(encoded_query 'tianquan-jianshen-admin')"
  env="$(encoded_query "${DEPLOYMENT_ENV}")"
  namespace="$(encoded_query "${DEPLOYMENT_NAMESPACE}")"
  rbac3_bearer_get "${TIANSHU_STATUS_ACCESS_TOKEN_FILE}" \
    "${TIANSHU_ADMIN_BASE_URL%/}/api/v1/tianshu/registry/instances?bizCode=${biz_code}&appCode=${app_code}&env=${env}&namespace=${namespace}&serviceKind=HTTP_PROVIDER&protocol=http&serviceName=tianquan-jianshen-admin&group=default&version=$(encoded_query "${TIANQUAN_JIANSHEN_ARTIFACT_VERSION}")"
}

assert_ddc_count() {
  local expected="$1"
  local response
  response="$(ddc_instances)"
  jq -e --argjson expected "${expected}" '
    [.data.instances[]? | select(.status == "UP" or .status == "ONLINE" or .status == "ACTIVE")] | length == $expected
  ' <<< "${response}" >/dev/null
}

assert_gateway_control_plane() {
  local token release providers consistency biz_code app_code env namespace
  token="$(rbac3_read_secret "${YUHENG_STATUS_OAUTH_TOKEN_FILE}")"
  release="$(curl --fail-with-body --silent --show-error --connect-timeout 3 --max-time 10 \
    --header "Authorization: Bearer ${token}" --header 'Accept: application/json' \
    "${YUHENG_ADMIN_BASE_URL%/}/api/v1/yuheng/admin/releases/$(encoded_query "${YUHENG_RELEASE_ID}")")"
  jq -e --arg release "${YUHENG_RELEASE_ID}" '
    (.releaseId // .id) == $release and .status == "SUCCESS"
  ' <<< "${release}" >/dev/null

  biz_code="$(encoded_query "${TIANSHU_BIZ_CODE}")"
  app_code="$(encoded_query 'tianquan-jianshen-admin')"
  env="$(encoded_query "${DEPLOYMENT_ENV}")"
  namespace="$(encoded_query "${DEPLOYMENT_NAMESPACE}")"
  providers="$(curl --fail-with-body --silent --show-error --connect-timeout 3 --max-time 10 \
    --header "Authorization: Bearer ${token}" --header 'Accept: application/json' \
    "${YUHENG_ADMIN_BASE_URL%/}/api/v1/yuheng/admin/providers/instances?bizCode=${biz_code}&appCode=${app_code}&env=${env}&namespace=${namespace}&serviceKind=HTTP_PROVIDER&protocol=http&serviceName=tianquan-jianshen-admin&group=default&version=$(encoded_query "${TIANQUAN_JIANSHEN_ARTIFACT_VERSION}")")"
  jq -e '[.. | objects | select(has("instanceId"))] | length >= 2' <<< "${providers}" >/dev/null

  consistency="$(curl --fail-with-body --silent --show-error --connect-timeout 3 --max-time 10 \
    --header "Authorization: Bearer ${token}" --header 'Accept: application/json' \
    "${YUHENG_ADMIN_BASE_URL%/}/api/v1/yuheng/admin/yuheng-groups/$(encoded_query "${YUHENG_GROUP_ID}")/runtime-consistency")"
  jq -e --arg release "${YUHENG_RELEASE_ID}" '
    .consistent == true and .releaseId == $release and (.releaseStatus // .status) == "SUCCESS"
  ' <<< "${consistency}" >/dev/null
}

route_status() {
  curl --silent --show-error --output /dev/null --write-out '%{http_code}' \
    --connect-timeout 3 --max-time 10 \
    --header "X-Tianquan-Jianshen-Test-Tenant: ${TIANQUAN_JIANSHEN_TENANT_ID}" \
    "${YUHENG_PUBLIC_ROUTE_URL}"
}

assert_route_success() {
  local attempt status
  for attempt in 1 2 3 4 5; do
    status="$(route_status)"
    [[ "${status}" -ge 200 && "${status}" -lt 400 ]] \
      || rbac3_die "Yuheng route attempt ${attempt} returned ${status}"
  done
}

verify() {
  check_config
  rbac3_note "checkpoint 1/4: validating two acknowledged Definitions and routeability"
  assert_admin_status "${TIANQUAN_JIANSHEN_ADMIN_1_BASE_URL}" "${TIANQUAN_JIANSHEN_INSTANCE_1_ID}"
  assert_admin_status "${TIANQUAN_JIANSHEN_ADMIN_2_BASE_URL}" "${TIANQUAN_JIANSHEN_INSTANCE_2_ID}"
  rbac3_note "artifact ${TIANQUAN_JIANSHEN_ARTIFACT_VERSION}; builds ${TIANQUAN_JIANSHEN_BUILD_ID_1}, ${TIANQUAN_JIANSHEN_BUILD_ID_2}"
  assert_ddc_count 2
  assert_gateway_control_plane
  assert_route_success

  rbac3_pause "Stop Admin instance 1 (${TIANQUAN_JIANSHEN_INSTANCE_1_ID}) using your deployment tooling. Do not stop instance 2."
  rbac3_note "checkpoint 2/4: validating single-instance lease and routed failover"
  assert_ddc_count 1
  assert_route_success

  rbac3_pause "Stop Admin instance 2 (${TIANQUAN_JIANSHEN_INSTANCE_2_ID}) using your deployment tooling. Both Admin instances must now be unavailable."
  rbac3_note "checkpoint 3/4: validating fail-closed routing with no providers"
  assert_ddc_count 0
  local status
  status="$(route_status)"
  [[ "${status}" == "${YUHENG_FAIL_CLOSED_STATUS}" ]] \
    || rbac3_die "expected fail-closed ${YUHENG_FAIL_CLOSED_STATUS}, received ${status}"

  rbac3_pause "Restore both Admin instances using your deployment tooling, then wait for Definition acknowledgement and Tianshu leases."
  rbac3_note "checkpoint 4/4: validating recovery"
  assert_admin_status "${TIANQUAN_JIANSHEN_ADMIN_1_BASE_URL}" "${TIANQUAN_JIANSHEN_INSTANCE_1_ID}"
  assert_admin_status "${TIANQUAN_JIANSHEN_ADMIN_2_BASE_URL}" "${TIANQUAN_JIANSHEN_INSTANCE_2_ID}"
  assert_ddc_count 2
  assert_gateway_control_plane
  assert_route_success
  rbac3_note "live topology verification passed"
}

case "${1:---help}" in
  --help) usage ;;
  --check-config) check_config; rbac3_note "topology configuration is valid and no endpoint was contacted" ;;
  --verify) verify ;;
  *) usage >&2; exit 2 ;;
esac
