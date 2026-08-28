#!/usr/bin/env bash

set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
platform_root="$root/egon-cola-platforms"

declare -a platforms=(
  "IDP|$platform_root/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java|$platform_root/egon-cola-platform-idp/egon-cola-platform-idp-admin-web"
  "RBAC3|$platform_root/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java|$platform_root/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web"
  "Gateway|$platform_root/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java|$platform_root/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web"
  "DDC|$platform_root/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java|$platform_root/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web"
)

for entry in "${platforms[@]}"; do
  IFS='|' read -r name controller_root web_root <<< "$entry"
  test -d "$controller_root"
  test -d "$web_root/src"

  external_files="$(rg -l 'Exposure\.EXTERNAL' "$controller_root" -g '*Controller.java' | wc -l | tr -d ' ')"
  mapping_lines="$(rg -n '@(Get|Post|Put|Patch|Delete)Mapping' "$controller_root" -g '*Controller.java' | rg -v 'internal|Internal|rpc|Rpc' | wc -l | tr -d ' ')"
  browser_api_lines="$(rg -n '/api/' "$web_root/src" -g '*.{ts,tsx}' | wc -l | tr -d ' ')"

  printf '%s\n' "$name"
  printf '%s\n' "  external controller files: $external_files"
  printf '%s\n' "  external mapping annotation lines (static): $mapping_lines"
  printf '%s\n' "  browser Admin API references (static): $browser_api_lines"
done

assert_contains() {
  local file="$1"
  local expected="$2"
  if ! rg -q --fixed-strings "$expected" "$file"; then
    printf '%s\n' "ASSERTION FAILED: $expected ($file)" >&2
    exit 1
  fi
}

assert_contains "$platform_root/egon-cola-platform-admin-portal/src/lifecycle/WujieChild.tsx" 'startApp'
assert_contains "$platform_root/egon-cola-platform-admin-web-shared/src/layout/EnterpriseSidebar.tsx" "typeof candidate !== 'string'"
assert_contains "$platform_root/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/security/management/DdcAdminSecurityConfiguration.java" '/api/v1/ddc/configs/page'
assert_contains "$platform_root/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/App.tsx" 'bindings'
assert_contains "$platform_root/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/resourceDefinitions.json" '"code": "iam.permissions"'
assert_contains "$platform_root/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/application/application.api.ts" '/api/rbac3/v1/iam/permissions'
assert_contains "$platform_root/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/layouts/AdminLayout.tsx" '/openapi-sync'
assert_contains "$platform_root/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/openapi/OpenApiSyncPage.tsx" 'openapiSnapshotDocument'
assert_contains "$root/scripts/unified-platform/start-local-stack.sh" 'UNIFIED_PLATFORM_ADVERTISED_HOST:-127.0.0.1'

printf '%s\n' 'Covered key repairs: Portal Wujie Core mount, shared path guard, DDC page authorization, DDC binding route, RBAC3 permission route, Gateway OpenAPI synchronization route.'
printf '%s\n' 'Intentional exclusions: protocol endpoints (/oauth2, /me, metadata, token), internal/RPC controllers, database/Redis/engine direct access, and active Gateway release publication.'
printf '%s\n' 'Evidence boundary: this is a static source inventory; it does not prove a running process has reloaded the new Web build or that an active Gateway release contains draft changes.'
