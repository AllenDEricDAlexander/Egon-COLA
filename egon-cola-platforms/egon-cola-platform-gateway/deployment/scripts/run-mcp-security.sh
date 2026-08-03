#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd "${script_dir}/../../../.." && pwd)"
suite="egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite"

cd "${repository_root}"
./mvnw \
  -pl "${suite}" \
  -am \
  -DskipITs=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest='McpSecurityIT,McpHaRecoveryIT,McpCompleteReleaseIT' \
  test
