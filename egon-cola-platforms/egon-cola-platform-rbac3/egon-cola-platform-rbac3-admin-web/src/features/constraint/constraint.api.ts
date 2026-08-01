import type { FeatureApiClient } from '../shared/FeatureApi'

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
  sodSets: () => client.request<readonly SodSetView[]>('/api/rbac3/v1/sod-sets'),
  dataRules: () => client.request<readonly DataRuleView[]>('/api/rbac3/v1/data-rules'),
  fieldRules: () => client.request<readonly FieldRuleView[]>('/api/rbac3/v1/field-rules'),
  operationSodRules: () => client.request<readonly OperationSodRuleView[]>('/api/rbac3/v1/operation-sod-rules'),
})
