import {
  FrontendResourceRegistry,
  resolveDefaultRoute,
  type FrontendNavigationNode,
  type FrontendResourceDefinition,
  type Rbac3AboutView,
} from '@egon-cola/rbac3-react-sdk'
import resourceDefinitions from './resourceDefinitions.json'
import { authorizationRouteDescriptors } from '../features/authorization.routes'
import { governanceRouteDescriptors } from '../features/governance.routes'
import { runtimeRouteDescriptors } from '../features/runtime.routes'
import type { FeatureRouteDescriptor } from '../features/shared/RouteDescriptor'
import type { EnterpriseNavigationItem } from '@egon-cola/admin-web-shared'

export const localResourceRegistry = new FrontendResourceRegistry(
  resourceDefinitions as readonly FrontendResourceDefinition[],
)

const componentDescriptors: readonly FeatureRouteDescriptor[] = [
  ...governanceRouteDescriptors,
  ...authorizationRouteDescriptors,
  ...runtimeRouteDescriptors,
].sort((left, right) => left.navigationOrder - right.navigationOrder || left.key.localeCompare(right.key))

const componentByKey = new Map(componentDescriptors.map((descriptor) => [descriptor.componentKey, descriptor]))

export const applicationRouteDescriptors: readonly FeatureRouteDescriptor[] = localResourceRegistry.definitions
  .filter((definition) => definition.kind === 'ROUTE' && definition.componentKey)
  .map((definition) => {
    const component = componentByKey.get(definition.componentKey!)
    if (!component) throw new Error(`missing local component binding for route ${definition.code}`)
    return {
      ...component,
      key: definition.code,
      path: definition.path!,
      title: definition.name,
      permission: definition.permission,
      navigationOrder: definition.order ?? component.navigationOrder,
      hideFromNav: definition.hidden,
    }
  })

const definitionForRoute = (route: FeatureRouteDescriptor) => localResourceRegistry.definitions.find(
  (definition) => definition.kind === 'ROUTE' && definition.componentKey === route.componentKey,
)

export const isRouteAllowed = (about: Rbac3AboutView, route: FeatureRouteDescriptor) => {
  const definition = definitionForRoute(route)
  return about.permissions.includes(definition?.permission ?? route.permission)
}

export const visibleNavigation = (about: Rbac3AboutView) => localResourceRegistry
  .navigation(about)
  .map(toNavigationItem)
  .filter((item): item is EnterpriseNavigationItem => item !== null)

export const resolveApplicationLanding = (about: Rbac3AboutView): string | null => {
  const definition = resolveDefaultRoute(about, localResourceRegistry)
  if (definition?.path) return definition.path
  return applicationRouteDescriptors.find((route) => isRouteAllowed(about, route))?.path ?? null
}

const toNavigationItem = (node: FrontendNavigationNode): EnterpriseNavigationItem | null => {
  if (node.kind === 'ROUTE' && node.hidden === true) return null
  const children = node.children
    .map(toNavigationItem)
    .filter((item): item is EnterpriseNavigationItem => item !== null)
  return {
  key: node.code,
  label: node.name,
  path: node.path,
    children: children.length > 0 ? children : undefined,
  }
}
