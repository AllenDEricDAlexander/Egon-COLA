#!/usr/bin/env bash

set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
platform_root="$root/egon-cola-xingyuan"

declare -a platforms=(
  "IDP|$platform_root/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java|$platform_root/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web"
  "RBAC3|$platform_root/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java|$platform_root/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web"
  "Gateway|$platform_root/egon-cola-yuheng/yuheng-admin/src/main/java|$platform_root/egon-cola-yuheng/yuheng-admin-web"
  "DDC|$platform_root/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java|$platform_root/egon-cola-tianshu/egon-cola-tianshu-admin-web"
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

assert_contains "$platform_root/egon-cola-xingyuan-admin-portal/src/lifecycle/WujieChild.tsx" 'startApp'
assert_contains "$platform_root/egon-cola-xingyuan-admin-web-shared/src/layout/EnterpriseSidebar.tsx" "typeof candidate !== 'string'"
assert_contains "$platform_root/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/security/management/DdcAdminSecurityConfiguration.java" '/api/v1/ddc/configs/page'
assert_contains "$platform_root/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/App.tsx" 'bindings'
assert_contains "$platform_root/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/app/resourceDefinitions.json" '"code": "iam.permissions"'
assert_contains "$platform_root/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/application/application.api.ts" '/api/rbac3/v1/iam/permissions'
assert_contains "$platform_root/egon-cola-yuheng/yuheng-admin-web/src/layouts/AdminLayout.tsx" '/openapi-sync'
assert_contains "$platform_root/egon-cola-yuheng/yuheng-admin-web/src/features/openapi/OpenApiSyncPage.tsx" 'openapiSnapshotDocument'
assert_contains "$root/scripts/unified-xingyuan/start-local-stack.sh" 'UNIFIED_PLATFORM_ADVERTISED_HOST:-127.0.0.1'

if ! python3 "$root/scripts/unified-xingyuan/controller_ui_coverage.py" --repo-root "$root" --format text; then
  printf '%s\n' 'ASSERTION FAILED: external Controller/UI coverage has unexplained management methods' >&2
  exit 1
fi

printf '%s\n' 'Covered key repairs: Portal Wujie Core mount, shared path guard, DDC page authorization, DDC binding route, RBAC3 permission route, Gateway OpenAPI synchronization route.'
printf '%s\n' 'Intentional exclusions: protocol endpoints (/oauth2, /me, metadata, token), internal/RPC controllers, database/Redis/engine direct access, and active Gateway release publication.'
printf '%s\n' 'Evidence boundary: this is a static source inventory; it does not prove a running process has reloaded the new Web build or that an active Gateway release contains draft changes.'
