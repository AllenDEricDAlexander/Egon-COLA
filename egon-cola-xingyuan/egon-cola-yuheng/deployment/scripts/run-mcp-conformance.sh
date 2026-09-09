#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
gateway_dir="$(cd "${script_dir}/../.." && pwd)"

# Target a published MCP Server on the MCP role; fixture URLs remain explicit overrides.
mcp_base="${MCP_CONFORMANCE_BASE_URL:-http://127.0.0.1:18084}"
stable_url="${1:-${mcp_base}/mcp/commerce}"
rc_url="${2:-${mcp_base}/mcp/commerce}"
output_root="${3:-${gateway_dir}/yuheng-test/yuheng-test-suite/target/mcp-conformance}"

mkdir -p "${output_root}/stable" "${output_root}/rc"

npx --yes @modelcontextprotocol/conformance@0.1.16 server \
  --url "${stable_url}" \
  --suite active \
  --spec-version 2025-11-25 \
  --output-dir "${output_root}/stable"

npx --yes @modelcontextprotocol/conformance@0.2.0-alpha.10 server \
  --url "${rc_url}" \
  --suite draft \
  --spec-version 2026-07-28 \
  --output-dir "${output_root}/rc"
