import type { FeatureRouteDescriptor } from './shared/RouteDescriptor'
import { AuditLogPage } from './audit/AuditLogPage'
import { RuntimeStatusPage } from './runtime/RuntimeStatusPage'
import { AuthorizationSimulationPage } from './simulation/AuthorizationSimulationPage'

export const runtimeRouteDescriptors: readonly FeatureRouteDescriptor[] = [
  { key: 'simulation', path: '/diagnostics/simulation', title: '授权模拟', permission: 'system:authorization-simulation:execute', componentKey: 'rbac3-simulation', component: AuthorizationSimulationPage, navigationOrder: 80 },
  { key: 'audit', path: '/diagnostics/audit', title: '授权审计', permission: 'system:audit:read', componentKey: 'rbac3-audit', component: AuditLogPage, navigationOrder: 81 },
  { key: 'runtime', path: '/diagnostics/runtime', title: '运行状态', permission: 'system:authorization-runtime:read', componentKey: 'rbac3-runtime', component: RuntimeStatusPage, navigationOrder: 82 },
] as const
