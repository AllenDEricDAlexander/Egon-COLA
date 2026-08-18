import type { Rbac3AboutView } from '../types'
import type { FrontendResourceDefinition } from '../registry/FrontendResourceRegistry'
import { FrontendResourceRegistry } from '../registry/FrontendResourceRegistry'

export const resolveDefaultRoute = (
  about: Rbac3AboutView,
  definitions: FrontendResourceRegistry | readonly FrontendResourceDefinition[],
): FrontendResourceDefinition | null => {
  const registry = definitions instanceof FrontendResourceRegistry
    ? definitions : new FrontendResourceRegistry(definitions)
  const accessible = registry.definitions
    .filter((route) => route.kind === 'ROUTE')
    .filter((route) => route.hidden !== true)
    .filter((route) => about.permissions.includes(route.permission))
    .sort((left, right) => (left.order ?? Number.MAX_SAFE_INTEGER)
      - (right.order ?? Number.MAX_SAFE_INTEGER)
      || left.code.localeCompare(right.code))

  return accessible.find((route) => route.code === about.landingRouteCode)
    ?? accessible[0]
    ?? null
}
