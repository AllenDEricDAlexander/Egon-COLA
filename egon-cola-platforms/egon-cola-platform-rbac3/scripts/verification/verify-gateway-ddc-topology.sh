#!/usr/bin/env bash

set -euo pipefail
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

usage() {
  cat <<'EOF'
Usage: verify-gateway-ddc-topology.sh [--help|--check-config|--verify]

Queries an already-running two-instance RBAC3 -> DDC -> Gateway topology. It
proves Definition acknowledgement, two HTTP_PROVIDER leases, the explicit
Release, runtime consistency, and routed failover as separate observations.
The script never starts, stops, or signals a process; failover changes are
operator checkpoints.

Required identity variables:
  RBAC3_INSTANCE_1_ID, RBAC3_INSTANCE_2_ID, RBAC3_INSTANCE_1_PORT,
  RBAC3_INSTANCE_2_PORT, RBAC3_MACHINE_ID_1, RBAC3_MACHINE_ID_2,
  RBAC3_ARTIFACT_VERSION, RBAC3_BUILD_ID_1, RBAC3_BUILD_ID_2,
  RBAC3_TENANT_ID, DEPLOYMENT_ENV, DEPLOYMENT_NAMESPACE.

Required endpoints and credentials:
  RBAC3_ADMIN_1_BASE_URL, RBAC3_ADMIN_2_BASE_URL,
  RBAC3_TOPOLOGY_ACCESS_TOKEN_FILE, DDC_ADMIN_BASE_URL,
  DDC_STATUS_ACCESS_TOKEN_FILE, GATEWAY_ADMIN_BASE_URL,
  GATEWAY_STATUS_OAUTH_TOKEN_FILE, GATEWAY_PUBLIC_ROUTE_URL,
  GATEWAY_GROUP_ID, GATEWAY_RELEASE_ID, GATEWAY_FAIL_CLOSED_STATUS.
EOF
}

check_config() {
  rbac3_require_command curl
  rbac3_require_command jq
  local name
  for name in \
      RBAC3_INSTANCE_1_ID RBAC3_INSTANCE_2_ID RBAC3_INSTANCE_1_PORT \
      RBAC3_INSTANCE_2_PORT RBAC3_MACHINE_ID_1 RBAC3_MACHINE_ID_2 \
      RBAC3_ARTIFACT_VERSION RBAC3_BUILD_ID_1 RBAC3_BUILD_ID_2 \
      RBAC3_TENANT_ID DEPLOYMENT_ENV DEPLOYMENT_NAMESPACE \
      GATEWAY_GROUP_ID GATEWAY_RELEASE_ID GATEWAY_FAIL_CLOSED_STATUS; do
    rbac3_require_env "${name}"
  done
  for name in RBAC3_INSTANCE_1_PORT RBAC3_INSTANCE_2_PORT \
      RBAC3_MACHINE_ID_1 RBAC3_MACHINE_ID_2 GATEWAY_FAIL_CLOSED_STATUS; do
    rbac3_validate_uint "${name}"
  done
  for name in RBAC3_ADMIN_1_BASE_URL RBAC3_ADMIN_2_BASE_URL \
      DDC_ADMIN_BASE_URL GATEWAY_ADMIN_BASE_URL GATEWAY_PUBLIC_ROUTE_URL; do
    rbac3_require_env "${name}"
    rbac3_validate_http_url "${name}"
  done
  rbac3_require_secret_file RBAC3_TOPOLOGY_ACCESS_TOKEN_FILE
  rbac3_require_secret_file DDC_STATUS_ACCESS_TOKEN_FILE
  rbac3_require_secret_file GATEWAY_STATUS_OAUTH_TOKEN_FILE
  [[ "${RBAC3_INSTANCE_1_ID}" != "${RBAC3_INSTANCE_2_ID}" ]] \
    || rbac3_die "Admin instance IDs must be distinct"
  [[ "${RBAC3_INSTANCE_1_PORT}" != "${RBAC3_INSTANCE_2_PORT}" ]] \
    || rbac3_die "Admin ports must be distinct"
  [[ "${RBAC3_MACHINE_ID_1}" != "${RBAC3_MACHINE_ID_2}" ]] \
    || rbac3_die "Snowflake machine IDs must be distinct"
  [[ "${RBAC3_BUILD_ID_1}" != "${RBAC3_BUILD_ID_2}" ]] \
    || rbac3_die "build IDs must identify the two process artifacts distinctly"
  [[ "${GATEWAY_FAIL_CLOSED_STATUS}" -ge 400 ]] \
    || rbac3_die "fail-closed status must be an error status"
}

encoded_query() {
  jq -rn --arg value "$1" '$value|@uri'
}

assert_admin_status() {
  local base_url="$1"
  local expected_instance="$2"
  local response
  response="$(rbac3_bearer_get "${RBAC3_TOPOLOGY_ACCESS_TOKEN_FILE}" \
    "${base_url%/}/api/rbac3/v1/runtime/gateway-ddc-status")"
  jq -e --arg instance "${expected_instance}" --arg release "${GATEWAY_RELEASE_ID}" '
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
  local env namespace
  env="$(encoded_query "${DEPLOYMENT_ENV}")"
  namespace="$(encoded_query "${DEPLOYMENT_NAMESPACE}")"
  rbac3_bearer_get "${DDC_STATUS_ACCESS_TOKEN_FILE}" \
    "${DDC_ADMIN_BASE_URL%/}/api/v1/ddc/registry/instances?env=${env}&namespace=${namespace}&serviceKind=HTTP_PROVIDER&protocol=http&serviceName=rbac3-admin&group=default&version=$(encoded_query "${RBAC3_ARTIFACT_VERSION}")"
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
  local token release providers consistency env namespace
  token="$(rbac3_read_secret "${GATEWAY_STATUS_OAUTH_TOKEN_FILE}")"
  release="$(curl --fail-with-body --silent --show-error --connect-timeout 3 --max-time 10 \
    --header "Authorization: Bearer ${token}" --header 'Accept: application/json' \
    "${GATEWAY_ADMIN_BASE_URL%/}/api/v1/gateway/admin/releases/$(encoded_query "${GATEWAY_RELEASE_ID}")")"
  jq -e --arg release "${GATEWAY_RELEASE_ID}" '
    (.releaseId // .id) == $release and .status == "SUCCESS"
  ' <<< "${release}" >/dev/null

  env="$(encoded_query "${DEPLOYMENT_ENV}")"
  namespace="$(encoded_query "${DEPLOYMENT_NAMESPACE}")"
  providers="$(curl --fail-with-body --silent --show-error --connect-timeout 3 --max-time 10 \
    --header "Authorization: Bearer ${token}" --header 'Accept: application/json' \
    "${GATEWAY_ADMIN_BASE_URL%/}/api/v1/gateway/admin/providers/instances?env=${env}&namespace=${namespace}&serviceKind=HTTP_PROVIDER&protocol=http&serviceName=rbac3-admin&group=default&version=$(encoded_query "${RBAC3_ARTIFACT_VERSION}")")"
  jq -e '[.. | objects | select(has("instanceId"))] | length >= 2' <<< "${providers}" >/dev/null

  consistency="$(curl --fail-with-body --silent --show-error --connect-timeout 3 --max-time 10 \
    --header "Authorization: Bearer ${token}" --header 'Accept: application/json' \
    "${GATEWAY_ADMIN_BASE_URL%/}/api/v1/gateway/admin/gateway-groups/$(encoded_query "${GATEWAY_GROUP_ID}")/runtime-consistency")"
  jq -e --arg release "${GATEWAY_RELEASE_ID}" '
    .consistent == true and .releaseId == $release and (.releaseStatus // .status) == "SUCCESS"
  ' <<< "${consistency}" >/dev/null
}

route_status() {
  curl --silent --show-error --output /dev/null --write-out '%{http_code}' \
    --connect-timeout 3 --max-time 10 \
    --header "X-RBAC3-Test-Tenant: ${RBAC3_TENANT_ID}" \
    "${GATEWAY_PUBLIC_ROUTE_URL}"
}

assert_route_success() {
  local attempt status
  for attempt in 1 2 3 4 5; do
    status="$(route_status)"
    [[ "${status}" -ge 200 && "${status}" -lt 400 ]] \
      || rbac3_die "Gateway route attempt ${attempt} returned ${status}"
  done
}

verify() {
  check_config
  rbac3_note "checkpoint 1/4: validating two acknowledged Definitions and routeability"
  assert_admin_status "${RBAC3_ADMIN_1_BASE_URL}" "${RBAC3_INSTANCE_1_ID}"
  assert_admin_status "${RBAC3_ADMIN_2_BASE_URL}" "${RBAC3_INSTANCE_2_ID}"
  rbac3_note "artifact ${RBAC3_ARTIFACT_VERSION}; builds ${RBAC3_BUILD_ID_1}, ${RBAC3_BUILD_ID_2}"
  assert_ddc_count 2
  assert_gateway_control_plane
  assert_route_success

  rbac3_pause "Stop Admin instance 1 (${RBAC3_INSTANCE_1_ID}) using your deployment tooling. Do not stop instance 2."
  rbac3_note "checkpoint 2/4: validating single-instance lease and routed failover"
  assert_ddc_count 1
  assert_route_success

  rbac3_pause "Stop Admin instance 2 (${RBAC3_INSTANCE_2_ID}) using your deployment tooling. Both Admin instances must now be unavailable."
  rbac3_note "checkpoint 3/4: validating fail-closed routing with no providers"
  assert_ddc_count 0
  local status
  status="$(route_status)"
  [[ "${status}" == "${GATEWAY_FAIL_CLOSED_STATUS}" ]] \
    || rbac3_die "expected fail-closed ${GATEWAY_FAIL_CLOSED_STATUS}, received ${status}"

  rbac3_pause "Restore both Admin instances using your deployment tooling, then wait for Definition acknowledgement and DDC leases."
  rbac3_note "checkpoint 4/4: validating recovery"
  assert_admin_status "${RBAC3_ADMIN_1_BASE_URL}" "${RBAC3_INSTANCE_1_ID}"
  assert_admin_status "${RBAC3_ADMIN_2_BASE_URL}" "${RBAC3_INSTANCE_2_ID}"
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
