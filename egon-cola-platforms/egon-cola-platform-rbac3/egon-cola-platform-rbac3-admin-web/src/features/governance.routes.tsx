import { Navigate, useParams } from 'react-router-dom'
import type { FeatureRouteDescriptor } from './shared/RouteDescriptor'
import { OverviewPage } from './overview/OverviewPage'
import { UserDirectoryPage } from './directory/UserDirectoryPage'
import { ApplicationListPage } from './application/ApplicationListPage'
import { ResourceCatalogPage } from './application/ResourceCatalogPage'
import { PermissionPage } from './application/PermissionPage'
import { FieldDefinitionPage } from './application/FieldDefinitionPage'
import { RoleGraphPage } from './role/RoleGraphPage'
import { RolePermissionPage } from './role/RolePermissionPage'
import { ConstraintPage } from './constraint/ConstraintPage'

const RolePermissionRoute = () => {
  const { roleId } = useParams()
  return roleId ? <RolePermissionPage roleId={roleId} /> : <Navigate to="/roles" replace />
}

export const governanceRouteDescriptors: readonly FeatureRouteDescriptor[] = [
  { key: 'overview', path: '/iam/overview', title: '治理概览', permission: 'system:runtime:read', componentKey: 'rbac3-overview', component: OverviewPage, navigationOrder: 10 },
  { key: 'directory-users', path: '/iam/users', title: '用户目录', permission: 'system:user:read', componentKey: 'rbac3-users', component: UserDirectoryPage, navigationOrder: 30 },
  { key: 'directory-organizations', path: '/iam/organizations', title: '组织', permission: 'system:organization:read', componentKey: 'rbac3-organizations', component: UserDirectoryPage, navigationOrder: 31 },
  { key: 'directory-positions', path: '/iam/positions', title: '岗位', permission: 'system:position:read', componentKey: 'rbac3-positions', component: UserDirectoryPage, navigationOrder: 32 },
  { key: 'tenant-applications', path: '/iam/tenant-applications', title: '租户应用', permission: 'system:application:read', componentKey: 'rbac3-tenant-applications', component: ApplicationListPage, navigationOrder: 40 },
  { key: 'resources', path: '/iam/resources', title: '资源目录', permission: 'system:resource:read', componentKey: 'rbac3-resources', component: ResourceCatalogPage, navigationOrder: 41 },
  { key: 'fields', path: '/iam/fields', title: '字段定义', permission: 'system:field-definition:read', componentKey: 'rbac3-fields', component: FieldDefinitionPage, navigationOrder: 42 },
  { key: 'permissions', path: '/iam/permissions', title: '权限字符', permission: 'system:permission:read', componentKey: 'rbac3-permissions', component: PermissionPage, navigationOrder: 43 },
  { key: 'roles', path: '/iam/roles', title: '角色图谱', permission: 'system:role:read', componentKey: 'rbac3-role-graph', component: RoleGraphPage, navigationOrder: 50 },
  { key: 'role-permissions', path: '/iam/roles/:roleId/permissions', title: '角色权限', permission: 'system:role:read', componentKey: 'rbac3-role-permissions', component: RolePermissionRoute, navigationOrder: 51, hideFromNav: true },
  { key: 'constraints', path: '/iam/policies', title: '授权约束', permission: 'system:authorization-constraint:read', componentKey: 'rbac3-constraints', component: ConstraintPage, navigationOrder: 60 },
] as const
