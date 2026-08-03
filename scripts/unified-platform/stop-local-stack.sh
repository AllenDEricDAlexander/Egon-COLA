#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${script_dir}/lib/common.sh"

export UNIFIED_IDENTITY_RUNTIME_DIR="${unified_platform_runtime_dir}"

for name in \
  idp-admin-web \
  rbac3-admin-web \
  gateway-admin-web \
  ddc-admin-web \
  gateway-engine-b \
  mcp-provider \
  mcp-remote; do
  unified_platform_stop_process "${name}"
done

"${unified_platform_repo_root}/scripts/unified-identity-local.sh" stop
printf 'Unified platform managed processes stopped. Databases, secrets and evidence were preserved.\n'
