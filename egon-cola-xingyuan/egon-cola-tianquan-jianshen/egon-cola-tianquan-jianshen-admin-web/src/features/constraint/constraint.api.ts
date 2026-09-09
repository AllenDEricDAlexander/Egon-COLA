import type { FeatureApiClient } from '../shared/FeatureApi'

export interface MutationResult {
  readonly resourceId: string
  readonly policyVersion: number
  readonly propagationId: string
  readonly propagationPending: boolean
}

export interface SodSetCommand {
  readonly setCode: string
  readonly constraintType: string
  readonly applicationId: string | null
  readonly maximumActiveRoles: number
  readonly memberRoleIds: readonly string[]
  readonly validFrom: string
  readonly validTo: string | null
  readonly expectedVersion: number
}

export interface PrerequisiteGroupCommand {
  readonly groupCode: string
  readonly matchMode: string
  readonly prerequisiteRoleIds: readonly string[]
  readonly expectedRoleVersion: number
}

export interface CardinalityCommand {
  readonly scopeType: string
  readonly maximumActive: number
  readonly validFrom: string
  readonly validTo: string | null
  readonly expectedVersion: number
}

export interface RuleReference {
  readonly referenceType: string
  readonly referenceId: string
}

export interface DataRuleCommand {
  readonly applicationId: string
  readonly roleId: string
  readonly permissionId: string
  readonly scopeType: string
  readonly directorySnapshotVersion: number | null
  readonly references: readonly RuleReference[]
  readonly validFrom: string
  readonly validTo: string | null
  readonly expectedVersion: number
}

export interface FieldRuleCommand {
  readonly applicationId: string
  readonly roleId: string
  readonly permissionId: string
  readonly fieldDefinitionId: string
  readonly accessLevel: string
  readonly validFrom: string
  readonly validTo: string | null
  readonly expectedVersion: number
}

export interface OperationSodRuleCommand {
  readonly applicationCode: string
  readonly businessResource: string
  readonly priorActionCode: string
  readonly forbiddenLaterActionCode: string
  readonly lookbackFrom: string | null
  readonly validFrom: string
  readonly validTo: string | null
  readonly expectedVersion: number
}

export interface SodSetView {
  readonly setId: string
  readonly setCode: string
  readonly constraintType: 'SSD' | 'DSD'
  readonly applicationId: string | null
  readonly maximumActiveRoles: number
  readonly roleIds: readonly string[]
  readonly status: string
  readonly version: number
}

export interface DataRuleView {
  readonly ruleId: string
  readonly applicationId: string
  readonly roleId: string
  readonly permissionId: string
  readonly scopeType: string
  readonly references: readonly RuleReference[]
  readonly status: string
  readonly version: number
}

export interface FieldRuleView {
  readonly ruleId: string
  readonly applicationId: string
  readonly roleId: string
  readonly permissionId: string
  readonly fieldDefinitionId: string
  readonly accessLevel: string
  readonly status: string
  readonly version: number
}

export interface OperationSodRuleView {
  readonly ruleId: string
  readonly applicationCode: string
  readonly businessResource: string
  readonly priorActionCode: string
  readonly forbiddenLaterActionCode: string
  readonly status: string
  readonly version: number
}

export const constraintApi = (client: FeatureApiClient) => ({
  sodSets: () => client.request<readonly SodSetView[]>('/api/tianquan-jianshen/v1/iam/policies/sod-sets'),
  createSod: (command: SodSetCommand) => client.request<MutationResult>(
    '/api/tianquan-jianshen/v1/iam/policies/sod-sets',
    {method: 'POST', body: command},
  ),
  updateSod: (setId: string, command: SodSetCommand) => client.request<MutationResult>(
    `/api/tianquan-jianshen/v1/iam/policies/sod-sets/${encodeURIComponent(setId)}`,
    {method: 'PUT', body: command},
  ),
  savePrerequisites: (roleId: string, command: PrerequisiteGroupCommand) => client.request<MutationResult>(
    `/api/tianquan-jianshen/v1/iam/policies/roles/${encodeURIComponent(roleId)}/prerequisite-groups`,
    {method: 'POST', body: command},
  ),
  saveCardinality: (roleId: string, command: CardinalityCommand) => client.request<MutationResult>(
    `/api/tianquan-jianshen/v1/iam/policies/roles/${encodeURIComponent(roleId)}/cardinality`,
    {method: 'PUT', body: command},
  ),
  dataRules: () => client.request<readonly DataRuleView[]>('/api/tianquan-jianshen/v1/iam/policies/data-rules'),
  createDataRule: (command: DataRuleCommand) => client.request<MutationResult>(
    '/api/tianquan-jianshen/v1/iam/policies/data-rules',
    {method: 'POST', body: command},
  ),
  updateDataRule: (ruleId: string, command: DataRuleCommand) => client.request<MutationResult>(
    `/api/tianquan-jianshen/v1/iam/policies/data-rules/${encodeURIComponent(ruleId)}`,
    {method: 'PUT', body: command},
  ),
  fieldRules: () => client.request<readonly FieldRuleView[]>('/api/tianquan-jianshen/v1/iam/policies/field-rules'),
  createFieldRule: (command: FieldRuleCommand) => client.request<MutationResult>(
    '/api/tianquan-jianshen/v1/iam/policies/field-rules',
    {method: 'POST', body: command},
  ),
  updateFieldRule: (ruleId: string, command: FieldRuleCommand) => client.request<MutationResult>(
    `/api/tianquan-jianshen/v1/iam/policies/field-rules/${encodeURIComponent(ruleId)}`,
    {method: 'PUT', body: command},
  ),
  operationSodRules: () => client.request<readonly OperationSodRuleView[]>('/api/tianquan-jianshen/v1/iam/policies/operation-sod-rules'),
  createOperationSodRule: (command: OperationSodRuleCommand) => client.request<MutationResult>(
    '/api/tianquan-jianshen/v1/iam/policies/operation-sod-rules',
    {method: 'POST', body: command},
  ),
  updateOperationSodRule: (ruleId: string, command: OperationSodRuleCommand) => client.request<MutationResult>(
    `/api/tianquan-jianshen/v1/iam/policies/operation-sod-rules/${encodeURIComponent(ruleId)}`,
    {method: 'PUT', body: command},
  ),
})
