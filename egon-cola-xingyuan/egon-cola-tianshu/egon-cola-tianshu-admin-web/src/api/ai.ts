import {createDdcTenantQueryKey} from '../query/queryClient'
import {
  ddcApi,
  ddcPageApi,
  requireDdcExpectedTenantId,
  type DdcRequestOptions,
} from './client'
import type {
  DdcAiCreateCommand,
  DdcAiDraftCommand,
  DdcAiOptimizationPolicyCommand,
  DdcAiOptimizationPolicyResult,
  DdcAiOptimizationRunPageParams,
  DdcAiOptimizationRunResult,
  DdcAiOptimizationTriggerCommand,
  DdcAiPublicationPageParams,
  DdcAiPublicationResult,
  DdcAiPromotionCommand,
  DdcAiPublishCommand,
  DdcAiResourcePageParams,
  DdcAiResourceResult,
  DdcAiResourceSummary,
  DdcAiRollbackCommand,
  DdcAiUsageFeedbackPageParams,
  DdcAiUsageFeedbackResult,
  DdcAiVersionPageParams,
  DdcAiVersionResult,
  DdcAiVersionSummary,
  PageResultRecord,
} from './types'

const AI_RESOURCES_PATH = '/api/v1/tianshu/ai/resources'
const LONG_MAX = '9223372036854775807'
type RequestOptions = Pick<DdcRequestOptions, 'signal'>

const assertLongString = (value: string, name: string, allowZero: boolean): void => {
  const pattern = allowZero ? /^(0|[1-9][0-9]{0,18})$/ : /^[1-9][0-9]{0,18}$/
  if (!pattern.test(value) || value.length > LONG_MAX.length
      || (value.length === LONG_MAX.length && value > LONG_MAX)) {
    throw new Error(`${name} must be a decimal Long string`)
  }
}

const assertRequestId = (requestId: string): void => {
  if (!/^[A-Za-z0-9_-]{16,64}$/.test(requestId)) {
    throw new Error('requestId must be 16..64 URL-safe characters')
  }
}

const queryString = (values: Record<string, string | number | undefined>): string => {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(values)) {
    if (value !== undefined) params.set(key, String(value))
  }
  return params.toString()
}

const resourcePath = (resourceId: string): string => {
  assertLongString(resourceId, 'resourceId', false)
  return `${AI_RESOURCES_PATH}/${encodeURIComponent(resourceId)}`
}

const requireTenant = (): string => requireDdcExpectedTenantId()

const mutationOptions = (
  requestId: string,
  body: unknown,
  options: RequestOptions = {},
): DdcRequestOptions => {
  assertRequestId(requestId)
  const headers = new Headers()
  headers.set('Idempotency-Key', requestId)
  return { ...options, method: 'POST', body, headers }
}

export const ddcAiQueryKeys = {
  all: () => createDdcTenantQueryKey(requireTenant(), 'ai'),
  resources: (params: DdcAiResourcePageParams) =>
    createDdcTenantQueryKey(requireTenant(), 'ai', 'resources', params),
  resource: (resourceId: string) =>
    createDdcTenantQueryKey(requireTenant(), 'ai', 'resource', resourceId),
  versions: (resourceId: string, params: DdcAiVersionPageParams) =>
    createDdcTenantQueryKey(requireTenant(), 'ai', 'versions', resourceId, params),
  version: (resourceId: string, versionNo: string) =>
    createDdcTenantQueryKey(requireTenant(), 'ai', 'version', resourceId, versionNo),
  publications: (resourceId: string, params: DdcAiPublicationPageParams) =>
    createDdcTenantQueryKey(requireTenant(), 'ai', 'publications', resourceId, params),
  optimizationPolicy: (resourceId: string) =>
    createDdcTenantQueryKey(requireTenant(), 'ai', 'optimization-policy', resourceId),
  optimizationRunsAll: (resourceId: string) =>
    createDdcTenantQueryKey(requireTenant(), 'ai', 'optimization-runs', resourceId),
  optimizationRuns: (resourceId: string, params: DdcAiOptimizationRunPageParams) =>
    createDdcTenantQueryKey(requireTenant(), 'ai', 'optimization-runs', resourceId, params),
  optimizationRun: (resourceId: string, runId: string) =>
    createDdcTenantQueryKey(requireTenant(), 'ai', 'optimization-run', resourceId, runId),
  usageFeedback: (resourceId: string, params: DdcAiUsageFeedbackPageParams) =>
    createDdcTenantQueryKey(requireTenant(), 'ai', 'usage-feedback', resourceId, params),
}

export const pageAiResources = (
  query: DdcAiResourcePageParams,
  options: RequestOptions = {},
): Promise<PageResultRecord<DdcAiResourceSummary>> => {
  requireTenant()
  const search = queryString({
    bizCode: query.bizCode,
    namespaceCode: query.namespaceCode,
    env: query.env,
    appCode: query.appCode,
    kind: query.kind,
    resourceKeyPrefix: query.resourceKeyPrefix,
    pageNo: query.pageNo ?? 1,
    pageSize: query.pageSize ?? 20,
  })
  return ddcPageApi<DdcAiResourceSummary>(`${AI_RESOURCES_PATH}?${search}`, options)
}

export const createAiResource = (
  command: DdcAiCreateCommand,
  options: RequestOptions = {},
): Promise<DdcAiResourceResult> => {
  requireTenant()
  return ddcApi<DdcAiResourceResult>(AI_RESOURCES_PATH, { ...options, method: 'POST', body: command })
}

export const getAiResource = (
  resourceId: string,
  options: RequestOptions = {},
): Promise<DdcAiResourceResult> => {
  requireTenant()
  return ddcApi<DdcAiResourceResult>(resourcePath(resourceId), options)
}

export const replaceAiDraft = (
  resourceId: string,
  command: DdcAiDraftCommand,
  options: RequestOptions = {},
): Promise<DdcAiResourceResult> => {
  requireTenant()
  assertLongString(command.expectedRevision, 'expectedRevision', true)
  return ddcApi<DdcAiResourceResult>(`${resourcePath(resourceId)}/draft`, { ...options, method: 'PUT', body: command })
}

export const deleteAiResource = (
  resourceId: string,
  expectedRevision: string,
  options: RequestOptions = {},
): Promise<void> => {
  requireTenant()
  assertLongString(expectedRevision, 'expectedRevision', true)
  const search = queryString({ expectedRevision })
  return ddcApi<void>(`${resourcePath(resourceId)}?${search}`, { ...options, method: 'DELETE' })
}

export const pageAiVersions = (
  resourceId: string,
  query: DdcAiVersionPageParams = {},
  options: RequestOptions = {},
): Promise<PageResultRecord<DdcAiVersionSummary>> => {
  requireTenant()
  const search = queryString({ pageNo: query.pageNo ?? 1, pageSize: query.pageSize ?? 20 })
  return ddcPageApi<DdcAiVersionSummary>(`${resourcePath(resourceId)}/versions?${search}`, options)
}

export const getAiVersion = (
  resourceId: string,
  versionNo: string,
  options: RequestOptions = {},
): Promise<DdcAiVersionResult> => {
  requireTenant()
  assertLongString(versionNo, 'versionNo', false)
  return ddcApi<DdcAiVersionResult>(`${resourcePath(resourceId)}/versions/${encodeURIComponent(versionNo)}`, options)
}

export const publishAiDraft = (
  resourceId: string,
  requestId: string,
  command: DdcAiPublishCommand,
  options: RequestOptions = {},
): Promise<DdcAiPublicationResult> => {
  requireTenant()
  assertLongString(command.expectedRevision, 'expectedRevision', true)
  return ddcApi<DdcAiPublicationResult>(`${resourcePath(resourceId)}/publications`, mutationOptions(requestId, command, options))
}

export const rollbackAiPublication = (
  resourceId: string,
  requestId: string,
  command: DdcAiRollbackCommand,
  options: RequestOptions = {},
): Promise<DdcAiPublicationResult> => {
  requireTenant()
  assertLongString(command.expectedRevision, 'expectedRevision', true)
  assertLongString(command.versionNo, 'versionNo', false)
  return ddcApi<DdcAiPublicationResult>(`${resourcePath(resourceId)}/rollbacks`, mutationOptions(requestId, command, options))
}

export const promoteAiVersion = (
  resourceId: string,
  requestId: string,
  command: DdcAiPromotionCommand,
  options: RequestOptions = {},
): Promise<DdcAiPublicationResult> => {
  requireTenant()
  assertLongString(command.sourceVersionNo, 'sourceVersionNo', false)
  if (command.expectedTargetRevision !== null) {
    assertLongString(command.expectedTargetRevision, 'expectedTargetRevision', true)
  }
  return ddcApi<DdcAiPublicationResult>(`${resourcePath(resourceId)}/promotions`, mutationOptions(requestId, command, options))
}

export const pageAiPublications = (
  resourceId: string,
  query: DdcAiPublicationPageParams = {},
  options: RequestOptions = {},
): Promise<PageResultRecord<DdcAiPublicationResult>> => {
  requireTenant()
  const search = queryString({
    requestId: query.requestId,
    pageNo: query.pageNo ?? 1,
    pageSize: query.pageSize ?? 20,
  })
  return ddcPageApi<DdcAiPublicationResult>(`${resourcePath(resourceId)}/publications?${search}`, options)
}

export const getAiOptimizationPolicy = (
  resourceId: string,
  options: RequestOptions = {},
): Promise<DdcAiOptimizationPolicyResult> => {
  requireTenant()
  return ddcApi<DdcAiOptimizationPolicyResult>(`${resourcePath(resourceId)}/optimization-policy`, options)
}

export const replaceAiOptimizationPolicy = (
  resourceId: string,
  command: DdcAiOptimizationPolicyCommand,
  options: RequestOptions = {},
): Promise<DdcAiOptimizationPolicyResult> => {
  requireTenant()
  assertLongString(command.expectedRevision, 'expectedRevision', true)
  return ddcApi<DdcAiOptimizationPolicyResult>(`${resourcePath(resourceId)}/optimization-policy`,
    { ...options, method: 'PUT', body: command })
}

export const triggerAiOptimizationRun = (
  resourceId: string,
  requestId: string,
  command: DdcAiOptimizationTriggerCommand,
  options: RequestOptions = {},
): Promise<DdcAiOptimizationRunResult> => {
  requireTenant()
  return ddcApi<DdcAiOptimizationRunResult>(`${resourcePath(resourceId)}/optimization-runs`,
    mutationOptions(requestId, command, options))
}

export const pageAiOptimizationRuns = (
  resourceId: string,
  query: DdcAiOptimizationRunPageParams = {},
  options: RequestOptions = {},
): Promise<PageResultRecord<DdcAiOptimizationRunResult>> => {
  requireTenant()
  const search = queryString({ state: query.state, pageNo: query.pageNo ?? 1, pageSize: query.pageSize ?? 20 })
  return ddcPageApi<DdcAiOptimizationRunResult>(`${resourcePath(resourceId)}/optimization-runs?${search}`, options)
}

export const getAiOptimizationRun = (
  resourceId: string,
  runId: string,
  options: RequestOptions = {},
): Promise<DdcAiOptimizationRunResult> => {
  requireTenant()
  assertLongString(runId, 'runId', false)
  return ddcApi<DdcAiOptimizationRunResult>(
    `${resourcePath(resourceId)}/optimization-runs/${encodeURIComponent(runId)}`, options)
}

export const pageAiUsageFeedback = (
  resourceId: string,
  query: DdcAiUsageFeedbackPageParams = {},
  options: RequestOptions = {},
): Promise<PageResultRecord<DdcAiUsageFeedbackResult>> => {
  requireTenant()
  if (query.versionNo !== undefined) assertLongString(query.versionNo, 'versionNo', false)
  const search = queryString({
    versionNo: query.versionNo,
    outcome: query.outcome,
    scoreSource: query.scoreSource,
    maxScore: query.maxScore,
    pageNo: query.pageNo ?? 1,
    pageSize: query.pageSize ?? 20,
  })
  return ddcPageApi<DdcAiUsageFeedbackResult>(`${resourcePath(resourceId)}/usage-feedback?${search}`, options)
}
