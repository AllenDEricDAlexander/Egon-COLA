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

export interface BindRolePermissionsCommand {
  readonly applicationId: string
  readonly permissionIds: readonly string[]
  readonly validFrom: string
  readonly validTo: string | null
  readonly expectedRoleVersion: number
}

export const roleApi = (client: FeatureApiClient) => ({
  roles: (applicationId?: string) => client.request<readonly RoleView[]>(
    '/api/rbac3/v1/roles',
    { query: { applicationId } },
  ),
  impact: (roleId: string) => client.request<RoleImpactView>(
    `/api/rbac3/v1/roles/${encodeURIComponent(roleId)}/impact-analysis`,
  ),
  bindPermissions: (roleId: string, command: BindRolePermissionsCommand) => client.request(
    `/api/rbac3/v1/roles/${encodeURIComponent(roleId)}/permissions`,
    { method: 'POST', body: command },
  ),
})
