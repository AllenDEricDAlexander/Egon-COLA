#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${script_dir}/lib/common.sh"

export UNIFIED_IDENTITY_RUNTIME_DIR="${unified_xingyuan_runtime_dir}"

for name in \
  portal-web \
  tianquan-shoubing-admin-web \
  tianquan-jianshen-admin-web \
  yuheng-admin-web \
  tianshu-admin-web \
  yuheng-biz-gateway-b \
  mcp-provider \
  mcp-remote; do
  unified_xingyuan_stop_process "${name}"
done

"${unified_xingyuan_repo_root}/scripts/unified-identity-local.sh" stop
printf 'Unified Xingyuan managed processes stopped. Databases, secrets and evidence were preserved.\n'
