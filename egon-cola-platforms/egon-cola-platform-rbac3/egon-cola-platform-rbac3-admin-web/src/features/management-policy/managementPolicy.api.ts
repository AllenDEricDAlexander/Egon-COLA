import type { FeatureApiClient } from '../shared/FeatureApi'

export interface PolicySubject { readonly type: string; readonly id: string }
export interface PolicyScope { readonly type: string; readonly referenceId: string | null }
export interface PolicyRestrictions {
  readonly maximumAssignmentDays: number | null
  readonly maximumRiskLevel: string
  readonly requiredAuthenticationStrength: string
  readonly requireReason: boolean
  readonly requireTicket: boolean
  readonly includeInheritedSubjectRoles: boolean
  readonly requireAllAffiliationsInScope: boolean
}
export interface ManagementPolicyView {
  readonly policyId: string
  readonly policyCode: string
  readonly name: string
  readonly status: string
  readonly validFrom: string
  readonly validTo: string | null
  readonly restrictions: PolicyRestrictions
  readonly subjects: readonly PolicySubject[]
  readonly scopes: readonly PolicyScope[]
  readonly activationRootRoleIds: readonly string[]
  readonly operations: readonly string[]
  readonly version: number
}
export type SaveManagementPolicyCommand = Omit<ManagementPolicyView, 'policyId' | 'status' | 'version'>

export const managementPolicyApi = (client: FeatureApiClient) => ({
  list: () => client.request<readonly ManagementPolicyView[]>('/api/rbac3/v1/management-policies'),
  create: (command: SaveManagementPolicyCommand, idempotencyKey: string) => client.request<ManagementPolicyView>(
    '/api/rbac3/v1/management-policies',
    { method: 'POST', body: command, headers: { 'Idempotency-Key': idempotencyKey } },
  ),
  update: (policy: ManagementPolicyView, command: SaveManagementPolicyCommand, idempotencyKey: string) => client.request<ManagementPolicyView>(
    `/api/rbac3/v1/management-policies/${encodeURIComponent(policy.policyId)}`,
    { method: 'PUT', body: command, headers: { 'If-Match': String(policy.version), 'Idempotency-Key': idempotencyKey } },
  ),
  disable: (policy: ManagementPolicyView, idempotencyKey: string) => client.request<ManagementPolicyView>(
    `/api/rbac3/v1/management-policies/${encodeURIComponent(policy.policyId)}/disable`,
    { method: 'POST', headers: { 'If-Match': String(policy.version), 'Idempotency-Key': idempotencyKey } },
  ),
})
