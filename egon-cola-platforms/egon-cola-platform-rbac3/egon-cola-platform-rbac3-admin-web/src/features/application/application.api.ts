import type { FeatureApiClient } from '../shared/FeatureApi'

export interface TenantApplicationView {
  readonly applicationId: string
  readonly ddcBusinessId: string
  readonly ddcApplicationId: string
  readonly businessCode: string
  readonly applicationCode: string
  readonly applicationName: string
  readonly status: string
  readonly displayPriority: number
  readonly version: number
}

export interface ResourceView {
  readonly resourceId: string
  readonly applicationId: string
  readonly resourceType: string
  readonly resourceCode: string
  readonly resourceName: string
  readonly parentResourceId?: string | null
  readonly requiredPermissionId?: string | null
  readonly status: string
  readonly version: number
}

export interface FieldDefinitionView {
  readonly id: string
  readonly applicationId: string
  readonly resourceId: string
  readonly fieldCode: string
  readonly jsonPath: string
  readonly dataType: string
  readonly sensitivity: string
  readonly defaultAccess: string
  readonly maskingStrategy?: string | null
  readonly writable: boolean
  readonly exportable: boolean
  readonly status: string
  readonly version: number
}

export interface PermissionView {
  readonly id: string
  readonly applicationId: string
  readonly permissionCode: string
  readonly permissionName: string
  readonly riskLevel: string
  readonly status: string
  readonly sourceType: string
  readonly sourceBuildId?: string | null
  readonly sourceChecksum?: string | null
  readonly version: number
}

export const applicationApi = (client: FeatureApiClient) => ({
  tenantApplications: () => client.request<readonly TenantApplicationView[]>('/api/rbac3/v1/iam/tenant-applications'),
  admitTenantApplication: (ddcApplicationId: string, displayPriority: number) => client.request<TenantApplicationView>(
    '/api/rbac3/v1/iam/tenant-applications',
    { method: 'POST', body: { ddcApplicationId, displayPriority } },
  ),
  changeTenantApplicationStatus: (applicationId: string, status: string, expectedVersion: number) => client.request<TenantApplicationView>(
    `/api/rbac3/v1/iam/tenant-applications/${encodeURIComponent(applicationId)}/status`,
    { method: 'PUT', body: { status, expectedVersion } },
  ),
  removeTenantApplication: (applicationId: string, expectedVersion: number) => client.request<null>(
    `/api/rbac3/v1/iam/tenant-applications/${encodeURIComponent(applicationId)}`,
    { method: 'DELETE', query: { expectedVersion } },
  ),
  applications: () => client.request<readonly { applicationId: string; applicationCode: string; applicationName: string; status: string; version: number }[]>(
    '/api/rbac3/v1/iam/resource-catalog/applications',
  ),
  resources: (applicationId: string) => client.request<readonly ResourceView[]>(
    `/api/rbac3/v1/iam/resource-catalog/applications/${encodeURIComponent(applicationId)}/resources`,
  ),
  fields: (applicationId: string, resourceId?: string) => client.request<readonly FieldDefinitionView[]>(
    resourceId
      ? `/api/rbac3/v1/iam/resource-catalog/resources/${encodeURIComponent(resourceId)}/fields`
      : `/api/rbac3/v1/iam/resource-catalog/applications/${encodeURIComponent(applicationId)}/fields`,
    resourceId ? { query: { applicationId } } : undefined,
  ),
  createField: (command: Omit<FieldDefinitionView, 'id' | 'status' | 'version'>) => client.request<FieldDefinitionView>(
    '/api/rbac3/v1/iam/resource-catalog/fields',
    { method: 'POST', body: command },
  ),
  changeFieldStatus: (id: string, status: string, expectedVersion: number) => client.request<FieldDefinitionView>(
    `/api/rbac3/v1/iam/resource-catalog/fields/${encodeURIComponent(id)}/status`,
    { method: 'PUT', body: { status, expectedVersion } },
  ),
  archive: (resource: ResourceView) => client.request<{ readonly status: string }>(
    `/api/rbac3/v1/iam/resource-catalog/resources/${encodeURIComponent(resource.resourceId)}/archive`,
    { method: 'POST', body: { expectedVersion: resource.version } },
  ),
  permissions: (applicationId: string, assignable = false) => client.request<readonly PermissionView[]>(
    '/api/rbac3/v1/iam/permissions',
    { query: { applicationId, assignable } },
  ),
  createPermission: (command: { applicationId: string; permissionCode: string; permissionName: string; riskLevel: string; description?: string }) => client.request<PermissionView>(
    '/api/rbac3/v1/iam/permissions',
    { method: 'POST', body: command },
  ),
  changePermissionStatus: (id: string, status: string, expectedVersion: number) => client.request<PermissionView>(
    `/api/rbac3/v1/iam/permissions/${encodeURIComponent(id)}/status`,
    { method: 'PUT', body: { status, expectedVersion } },
  ),
})
