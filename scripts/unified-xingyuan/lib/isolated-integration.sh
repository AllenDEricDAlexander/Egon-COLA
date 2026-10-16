#!/usr/bin/env bash

# Keep the established shell API; protocol and ownership checks live in the private stdlib module.
isolated_integration_validate_manifest() {
  python3 "$isolated_integration_script_dir/lib/isolated_integration.py" validate \
    "$1" "$isolated_integration_repo_root" "$2" "$3" "$4"
}

isolated_integration_provision_identity() {
  python3 "$isolated_integration_script_dir/lib/isolated_integration.py" provision-identity \
    "$1" "$2" "$3" "$isolated_integration_repo_root"
}

isolated_integration_prepare() {
  python3 "$isolated_integration_script_dir/lib/isolated_integration.py" prepare "$1" "$2" "$3"
}

isolated_integration_process_state() {
  python3 "$isolated_integration_script_dir/lib/isolated_integration.py" process-state "$1" "$2" "$3"
}

isolated_integration_authorize_cli() {
  python3 "$isolated_integration_script_dir/lib/isolated_integration.py" authorize-cli \
    "$1" "$2" "$3" "$4" "$5" "$6" "$isolated_integration_repo_root"
}

isolated_integration_provision_authorization() {
  case "$3" in
    platform)
      python3 "$isolated_integration_script_dir/lib/isolated_integration.py" authorization-platform \
        "$1" "$2" "$4" "$isolated_integration_repo_root"
      ;;
    native)
      python3 "$isolated_integration_script_dir/lib/isolated_integration.py" authorization-native \
        "$1" "$2" "$4" "$5" "$isolated_integration_repo_root"
      ;;
    *) return 2 ;;
  esac
}

isolated_integration_start_control_plane() {
  python3 "$isolated_integration_script_dir/lib/isolated_integration.py" start-control-plane "$1" "$2"
}

isolated_integration_start_consumers() {
  python3 "$isolated_integration_script_dir/lib/isolated_integration.py" start-consumers "$1" "$2" "$3"
}

isolated_integration_verify() {
  python3 "$isolated_integration_script_dir/lib/isolated_integration.py" verify "$1" "$2" "$3" "$4"
}
