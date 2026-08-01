import { Navigate, useParams } from 'react-router-dom'
import type { FeatureRouteDescriptor } from './shared/RouteDescriptor'
import { OverviewPage } from './overview/OverviewPage'
import { TenantListPage } from './tenant/TenantListPage'
import { UserDirectoryPage } from './directory/UserDirectoryPage'
import { OrgPositionSnapshotPage } from './directory/OrgPositionSnapshotPage'
import { ApplicationListPage } from './application/ApplicationListPage'
import { ManifestDetailPage } from './application/ManifestDetailPage'
import { ResourceCatalogPage } from './application/ResourceCatalogPage'
import { RoleGraphPage } from './role/RoleGraphPage'
import { RolePermissionPage } from './role/RolePermissionPage'
import { ConstraintPage } from './constraint/ConstraintPage'

const ManifestRoute = () => {
  const { manifestId } = useParams()
  return manifestId ? <ManifestDetailPage manifestId={manifestId} /> : <Navigate to="/applications" replace />
}

const ResourceRoute = () => {
  const { applicationId } = useParams()
  return applicationId ? <ResourceCatalogPage applicationId={applicationId} /> : <Navigate to="/applications" replace />
}

const RolePermissionRoute = () => {
  const { roleId } = useParams()
  return roleId ? <RolePermissionPage roleId={roleId} /> : <Navigate to="/roles" replace />
}

export const governanceRouteDescriptors: readonly FeatureRouteDescriptor[] = [
  { key: 'overview', path: '/', title: '治理概览', permission: 'system:runtime:read', componentKey: 'rbac3-overview', component: OverviewPage, navigationOrder: 10 },
  { key: 'tenants', path: '/tenants', title: '租户上下文', permission: 'system:tenant:read', componentKey: 'rbac3-tenants', component: TenantListPage, navigationOrder: 20 },
  { key: 'directory-users', path: '/directory/users', title: '用户目录', permission: 'system:user:read', componentKey: 'rbac3-directory-users', component: UserDirectoryPage, navigationOrder: 30 },
  { key: 'directory-snapshot', path: '/directory/snapshots', title: '目录快照', permission: 'system:directory:sync', componentKey: 'rbac3-directory-snapshot', component: OrgPositionSnapshotPage, navigationOrder: 31 },
  { key: 'applications', path: '/applications', title: '应用管理', permission: 'system:application:read', componentKey: 'rbac3-applications', component: ApplicationListPage, navigationOrder: 40 },
  { key: 'manifest-detail', path: '/manifests/:manifestId', title: 'Manifest 详情', permission: 'system:resource-manifest:read', componentKey: 'rbac3-manifest-detail', component: ManifestRoute, navigationOrder: 41 },
  { key: 'resources', path: '/applications/:applicationId/resources', title: '资源目录', permission: 'system:resource:read', componentKey: 'rbac3-resources', component: ResourceRoute, navigationOrder: 42 },
  { key: 'roles', path: '/roles', title: '角色图谱', permission: 'system:role:read', componentKey: 'rbac3-role-graph', component: RoleGraphPage, navigationOrder: 50 },
  { key: 'role-permissions', path: '/roles/:roleId/permissions', title: '角色权限', permission: 'system:role:read', componentKey: 'rbac3-role-permissions', component: RolePermissionRoute, navigationOrder: 51 },
  { key: 'constraints', path: '/constraints', title: '授权约束', permission: 'system:authorization-constraint:read', componentKey: 'rbac3-constraints', component: ConstraintPage, navigationOrder: 60 },
] as const
