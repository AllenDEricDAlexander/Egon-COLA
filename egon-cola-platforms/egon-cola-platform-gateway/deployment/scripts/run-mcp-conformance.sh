#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
gateway_dir="$(cd "${script_dir}/../.." && pwd)"

stable_url="${1:-http://127.0.0.1:18151/conformance/stable}"
rc_url="${2:-http://127.0.0.1:18151/conformance/rc}"
output_root="${3:-${gateway_dir}/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/target/mcp-conformance}"

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
