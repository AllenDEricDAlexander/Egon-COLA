import { Rbac3ComponentRegistry, resolveDefaultRoute, type BootstrapView } from '@egon-cola/rbac3-react-sdk'
import { authorizationRouteDescriptors } from '../features/authorization.routes'
import { governanceRouteDescriptors } from '../features/governance.routes'
import { runtimeRouteDescriptors } from '../features/runtime.routes'
import type { FeatureRouteDescriptor } from '../features/shared/RouteDescriptor'

export const applicationRouteDescriptors: readonly FeatureRouteDescriptor[] = [
  ...governanceRouteDescriptors,
  ...authorizationRouteDescriptors,
  ...runtimeRouteDescriptors,
].sort((left, right) => left.navigationOrder - right.navigationOrder || left.key.localeCompare(right.key))

export const applicationComponentRegistry = new Rbac3ComponentRegistry(
  applicationRouteDescriptors.map((route) => [route.componentKey, route.component] as const),
)

export const isRouteAllowed = (bootstrap: BootstrapView, route: FeatureRouteDescriptor) => bootstrap.permissions.includes(route.permission)

export const visibleNavigation = (bootstrap: BootstrapView) => applicationRouteDescriptors
  .filter((route) => !route.path.includes(':'))
  .filter((route) => isRouteAllowed(bootstrap, route))

export const resolveApplicationLanding = (bootstrap: BootstrapView): string | null => {
  const manifestRoute = resolveDefaultRoute(bootstrap, applicationComponentRegistry)
  if (manifestRoute?.path) {
    const descriptor = applicationRouteDescriptors.find((route) => route.componentKey === manifestRoute.componentKey && route.path === manifestRoute.path)
    if (descriptor && isRouteAllowed(bootstrap, descriptor)) return descriptor.path
  }
  return visibleNavigation(bootstrap)[0]?.path ?? null
}
