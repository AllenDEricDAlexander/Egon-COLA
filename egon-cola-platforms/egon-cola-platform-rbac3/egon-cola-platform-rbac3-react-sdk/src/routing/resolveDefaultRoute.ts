import type { BootstrapView, ManifestResource } from '../types'
import { Rbac3ComponentRegistry } from '../registry/Rbac3ComponentRegistry'

export const resolveDefaultRoute = (
  bootstrap: BootstrapView,
  registry: Rbac3ComponentRegistry,
): ManifestResource | null => {
  const permissions = new Set(bootstrap.permissions)
  const accessible = bootstrap.routes
    .filter((route) => route.hidden !== true)
    .filter((route) => route.path !== null && route.componentKey !== null)
    .filter((route) => registry.has(route.componentKey!))
    .filter((route) => route.requiredPermissionCode === null
      || permissions.has(route.requiredPermissionCode))
    .sort((left, right) => (left.order ?? Number.MAX_SAFE_INTEGER)
      - (right.order ?? Number.MAX_SAFE_INTEGER)
      || left.code.localeCompare(right.code))

  return accessible.find((route) => route.path === bootstrap.defaultRoute)
    ?? accessible[0]
    ?? null
}
