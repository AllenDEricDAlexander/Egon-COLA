#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

isolated_integration_script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
isolated_integration_repo_root="$(cd "$isolated_integration_script_dir/../.." && pwd -P)"
source "$isolated_integration_script_dir/lib/isolated-integration.sh"

[[ $# -ge 1 ]] || exit 2
isolated_integration_operation="$1"
shift
case "$isolated_integration_operation" in
  prepare|provision-identity|start-control-plane|provision-authorization|start-consumers|authorize-cli|verify|status|stop) ;;
  *) exit 2 ;;
esac
isolated_integration_manifest=''
isolated_integration_result=''
isolated_integration_phase=''
isolated_integration_token_index=''
isolated_integration_tenant_code=''
isolated_integration_resource_key=''
isolated_integration_user_key=''
isolated_integration_output_file=''
isolated_integration_case_set=''
isolated_integration_resume=false
isolated_integration_seen=' '
while [[ $# -gt 0 ]]; do
  [[ "${isolated_integration_seen}" != *" $1 "* ]] || exit 2
  isolated_integration_seen+="$1 "
  case "$1" in
    --resume)
      case "$isolated_integration_operation" in prepare|provision-identity|provision-authorization) ;; *) exit 2 ;; esac
      isolated_integration_resume=true
      shift
      ;;
    --manifest|--result|--phase|--token-index|--tenant-code|--resource-key|--user-key|--output-file|--case-set)
      [[ $# -ge 2 && -n "$2" && "$2" != --* ]] || exit 2
      case "$1" in
        --manifest) isolated_integration_manifest="$2" ;;
        --result) isolated_integration_result="$2" ;;
        --phase)
          [[ "$isolated_integration_operation" == provision-authorization && ( "$2" == platform || "$2" == native ) ]] || exit 2
          isolated_integration_phase="$2"
          ;;
        --token-index)
          case "$isolated_integration_operation" in provision-authorization|start-consumers|verify) ;; *) exit 2 ;; esac
          isolated_integration_token_index="$2"
          ;;
        --tenant-code|--resource-key|--user-key|--output-file)
          [[ "$isolated_integration_operation" == authorize-cli ]] || exit 2
          case "$1" in
            --tenant-code) isolated_integration_tenant_code="$2" ;;
            --resource-key) isolated_integration_resource_key="$2" ;;
            --user-key) isolated_integration_user_key="$2" ;;
            --output-file) isolated_integration_output_file="$2" ;;
          esac
          ;;
        --case-set)
          [[ "$isolated_integration_operation" == verify && "$2" == full ]] || exit 2
          isolated_integration_case_set="$2"
          ;;
      esac
      shift 2
      ;;
    *) exit 2 ;;
  esac
done
[[ -n "$isolated_integration_manifest" && -n "$isolated_integration_result" ]] || exit 2
case "$isolated_integration_operation" in
  provision-authorization)
    [[ -n "$isolated_integration_phase" ]] || exit 2
    [[ "$isolated_integration_phase" != native || -n "$isolated_integration_token_index" ]] || exit 2
    ;;
  start-consumers) [[ -n "$isolated_integration_token_index" ]] || exit 2 ;;
  verify) [[ -n "$isolated_integration_case_set" && -n "$isolated_integration_token_index" ]] || exit 2 ;;
  authorize-cli)
    [[ -n "$isolated_integration_tenant_code" && -n "$isolated_integration_resource_key" && -n "$isolated_integration_user_key" && -n "$isolated_integration_output_file" ]] || exit 2
    case "$isolated_integration_tenant_code" in default|tenant-b) ;; *) exit 2 ;; esac
    case "$isolated_integration_user_key" in admin|viewer) ;; *) exit 2 ;; esac
    ;;
esac
if [[ "$isolated_integration_operation" != status && "${XINGYUAN_ISOLATED_INTEGRATION:-}" != true ]]; then
  exit 3
fi
isolated_integration_validate_manifest "$isolated_integration_manifest" "$isolated_integration_result" \
  "$isolated_integration_token_index" "$isolated_integration_output_file"
case "$isolated_integration_operation" in
  prepare)
    isolated_integration_prepare "$isolated_integration_manifest" "$isolated_integration_result" "$isolated_integration_resume"
    ;;
  provision-identity)
    isolated_integration_provision_identity "$isolated_integration_manifest" "$isolated_integration_result" "$isolated_integration_resume"
    ;;
  start-control-plane)
    isolated_integration_start_control_plane "$isolated_integration_manifest" "$isolated_integration_result"
    ;;
  start-consumers)
    isolated_integration_start_consumers "$isolated_integration_manifest" "$isolated_integration_result" "$isolated_integration_token_index"
    ;;
  provision-authorization)
    isolated_integration_provision_authorization "$isolated_integration_manifest" "$isolated_integration_result" \
      "$isolated_integration_phase" "$isolated_integration_resume" "$isolated_integration_token_index"
    ;;
  authorize-cli)
    isolated_integration_authorize_cli "$isolated_integration_manifest" "$isolated_integration_result" \
      "$isolated_integration_tenant_code" "$isolated_integration_resource_key" "$isolated_integration_user_key" "$isolated_integration_output_file"
    ;;
  verify)
    isolated_integration_verify "$isolated_integration_manifest" "$isolated_integration_result" "$isolated_integration_token_index" "$isolated_integration_case_set"
    ;;
  status|stop)
    isolated_integration_process_state "$isolated_integration_operation" "$isolated_integration_manifest" "$isolated_integration_result"
    ;;
  *) exit 4 ;;
esac
