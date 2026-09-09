import type { FeatureApiClient } from '../shared/FeatureApi'

export interface RoleView {
  readonly roleId: string
  readonly applicationId: string
  readonly roleCode: string
  readonly roleName: string
  readonly roleType: string
  readonly riskLevel: string
  readonly privileged: boolean
  readonly status: string
  readonly version: number
}

export interface RoleImpactView {
  readonly roleId: string
  readonly activationRoots: readonly string[]
  readonly roleFamily: readonly string[]
  readonly effectiveFamilyRisk: string
  readonly permissionCount: number
  readonly conflicts: readonly string[]
}

export interface RoleMutationResult {
  readonly resourceId: string
  readonly policyVersion: number
  readonly propagationId: string
  readonly propagationPending: boolean
}

export interface RoleResourceGrantNode {
  readonly resourceId: string
  readonly resourceCode: string
  readonly name: string
  readonly category: string
  readonly technicalType: string
  readonly parentCode: string | null
  readonly status: string
  readonly mappingStatus: string
  readonly grantState: string
  readonly grantable: boolean
  readonly disabledReason: string | null
  readonly linkedApis: readonly RoleResourceLinkedApi[]
  readonly children: readonly RoleResourceGrantNode[]
}

export interface RoleResourceLinkedApi {
  readonly resourceId: string
  readonly resourceCode: string
  readonly name: string
  readonly method: string | null
  readonly path: string | null
}

export interface RoleResourceGrantTreeView {
  readonly roleId: string
  readonly applicationId: string
  readonly roleVersion: number
  readonly directResourceIds: readonly string[]
  readonly derivedApiResourceIds: readonly string[]
  readonly summary: {
    readonly menuPageCount: number
    readonly actionCount: number
    readonly apiCount: number
    readonly inheritedCount: number
  }
  readonly nodes: readonly RoleResourceGrantNode[]
}

export interface ReplaceRoleResourcesCommand {
  readonly resourceIds: readonly string[]
  readonly validFrom: string
  readonly validTo: string | null
  readonly expectedRoleVersion: number
}

export interface CreateRoleCommand {
  readonly applicationId: string
  readonly roleCode: string
  readonly roleName: string
  readonly roleType: string
  readonly riskLevel: string
  readonly privileged: boolean
  readonly landingRouteId: string | null
  readonly landingPriority: number
  readonly maximumAssignmentDays: number | null
}

export interface UpdateRoleCommand {
  readonly roleName: string
  readonly status: string
  readonly landingRouteId: string | null
  readonly landingPriority: number
  readonly maximumAssignmentDays: number | null
  readonly expectedRoleVersion: number
}

export interface InheritanceCommand {
  readonly applicationId: string
  readonly juniorRoleId: string
  readonly expectedRoleVersion: number
}

export const roleApi = (client: FeatureApiClient) => ({
  roles: (applicationId?: string) => client.request<readonly RoleView[]>(
    '/api/rbac3/v1/iam/roles',
    { query: { applicationId } },
  ),
  create: (command: CreateRoleCommand) => client.request<RoleMutationResult>(
    '/api/rbac3/v1/iam/roles',
    { method: 'POST', body: command },
  ),
  update: (roleId: string, command: UpdateRoleCommand) => client.request<RoleMutationResult>(
    `/api/rbac3/v1/iam/roles/${encodeURIComponent(roleId)}`,
    { method: 'PUT', body: command },
  ),
  addInheritance: (roleId: string, command: InheritanceCommand) => client.request<RoleImpactView>(
    `/api/rbac3/v1/iam/roles/${encodeURIComponent(roleId)}/inheritances`,
    { method: 'POST', body: command },
  ),
  removeInheritance: (
    roleId: string,
    juniorRoleId: string,
    query: { applicationId: string; expectedRoleVersion: number },
  ) => client.request<RoleImpactView>(
    `/api/rbac3/v1/iam/roles/${encodeURIComponent(roleId)}/inheritances/${encodeURIComponent(juniorRoleId)}`,
    { method: 'DELETE', query },
  ),
  impact: (roleId: string) => client.request<RoleImpactView>(
    `/api/rbac3/v1/iam/roles/${encodeURIComponent(roleId)}/impact-analysis`,
  ),
  resources: (roleId: string) => client.request<RoleResourceGrantTreeView>(
    `/api/rbac3/v1/iam/roles/${encodeURIComponent(roleId)}/resources`,
  ),
  replaceResources: (roleId: string, command: ReplaceRoleResourcesCommand) => client.request(
    `/api/rbac3/v1/iam/roles/${encodeURIComponent(roleId)}/resources`,
    { method: 'PUT', body: command },
  ),
})
