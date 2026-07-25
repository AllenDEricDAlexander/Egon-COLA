import { apiRequest } from './client'
import { createLogicalTrace, newIdempotencyKey, type LogicalTrace } from './trace'
import type {
  Application,
  AuditEntry,
  CatalogTree,
  DashboardSummary,
  EngineNode,
  GatewayDraft,
  GatewayGroup,
  GatewayRelease,
  OperationDetail,
  Page,
  ProviderInstance,
  RuntimeConsistency,
  Scope,
  TraceSummary,
  ValidationReport,
} from './types'

const admin = '/api/v1/gateway/admin'
const query = (scope: Scope): string =>
  new URLSearchParams({ env: scope.env, namespace: scope.namespace }).toString()

export const gatewayApi = {
  dashboard: (scope: Scope, signal?: AbortSignal) =>
    apiRequest<DashboardSummary>(`${admin}/dashboard?${query(scope)}`, { signal }),
  groups: (scope: Scope, signal?: AbortSignal) =>
    apiRequest<GatewayGroup[]>(`${admin}/gateway-groups?${query(scope)}`, { signal }),
  group: (groupId: string, signal?: AbortSignal) =>
    apiRequest<GatewayGroup>(`${admin}/gateway-groups/${groupId}`, { signal }),
  engineNodes: (groupId: string, signal?: AbortSignal) =>
    apiRequest<EngineNode[]>(`${admin}/gateway-groups/${groupId}/engine-nodes`, { signal }),
  consistency: (groupId: string, signal?: AbortSignal) =>
    apiRequest<RuntimeConsistency>(
      `${admin}/gateway-groups/${groupId}/runtime-consistency`,
      { signal },
    ),
  applications: (scope: Scope, signal?: AbortSignal) =>
    apiRequest<Application[]>(`${admin}/applications?${query(scope)}`, { signal }),
  catalog: (applicationId: string, signal?: AbortSignal) =>
    apiRequest<CatalogTree>(`${admin}/applications/${applicationId}/catalog`, { signal }),
  operation: (operationId: string, signal?: AbortSignal) =>
    apiRequest<OperationDetail>(`${admin}/operations/${operationId}`, { signal }),
  draft: (groupId: string, signal?: AbortSignal) =>
    apiRequest<GatewayDraft>(`${admin}/gateway-groups/${groupId}/draft`, { signal }),
  saveRoute: (
    groupId: string,
    routeId: string,
    route: {
      operationId: string
      content: Record<string, unknown>
      enabled: boolean
      changeReason: string
    },
    revision: number,
    trace = createLogicalTrace(),
  ) => {
    const idempotencyKey = newIdempotencyKey()
    return apiRequest<GatewayDraft>(
      `${admin}/gateway-groups/${groupId}/draft/routes/${routeId}`,
      {
      method: 'PUT',
      body: { ...route, expectedRevision: revision, idempotencyKey },
      trace,
      idempotencyKey,
      },
    )
  },
  savePolicy: (
    groupId: string,
    policyId: string,
    policy: {
      policyType: string
      policyScope: string
      content: Record<string, unknown>
      enabled: boolean
      changeReason: string
    },
    revision: number,
    trace = createLogicalTrace(),
  ) => {
    const idempotencyKey = newIdempotencyKey()
    return apiRequest<GatewayDraft>(
      `${admin}/gateway-groups/${groupId}/draft/policies/${policyId}`,
      {
        method: 'PUT',
        body: { ...policy, expectedRevision: revision, idempotencyKey },
        trace,
        idempotencyKey,
      },
    )
  },
  validateDraft: (groupId: string, trace: LogicalTrace | undefined) =>
    apiRequest<ValidationReport>(`${admin}/gateway-groups/${groupId}/draft/validate`, {
      method: 'POST',
      trace,
    }),
  draftDiff: (groupId: string, signal?: AbortSignal) =>
    apiRequest<Record<string, unknown>>(`${admin}/gateway-groups/${groupId}/draft/diff`, {
      signal,
    }),
  releases: (groupId: string, signal?: AbortSignal) =>
    apiRequest<GatewayRelease[]>(`${admin}/gateway-groups/${groupId}/releases`, { signal }),
  release: (
    releaseId: string,
    signal?: AbortSignal,
    trace?: LogicalTrace,
  ) =>
    apiRequest<GatewayRelease>(`${admin}/releases/${releaseId}`, {
      signal,
      trace,
    }),
  publish: (
    groupId: string,
    draftRevision: number,
    changeReason: string,
    trace = createLogicalTrace(),
  ) =>
    apiRequest<GatewayRelease>(`${admin}/gateway-groups/${groupId}/releases`, {
      method: 'POST',
      body: { expectedDraftRevision: draftRevision, changeReason },
      trace,
      idempotencyKey: newIdempotencyKey(),
    }),
  retryRelease: (releaseId: string, trace = createLogicalTrace()) =>
    apiRequest<GatewayRelease>(`${admin}/releases/${releaseId}/retry`, {
      method: 'POST',
      body: { reason: 'Retry from Gateway Admin Web' },
      trace,
      idempotencyKey: newIdempotencyKey(),
    }),
  rollback: (
    groupId: string,
    releaseId: string,
    expectedDraftRevision: number,
    reason: string,
    trace = createLogicalTrace(),
  ) =>
    apiRequest<GatewayRelease>(`${admin}/gateway-groups/${groupId}/rollback`, {
      method: 'POST',
      body: {
        sourceReleaseId: releaseId,
        expectedDraftRevision,
        changeReason: reason,
      },
      trace,
      idempotencyKey: newIdempotencyKey(),
    }),
  providers: (scope: Scope, signal?: AbortSignal) =>
    apiRequest<ProviderInstance[]>(`${admin}/providers/instances?${query(scope)}`, { signal }),
  traces: (scope: Scope, filters: URLSearchParams, signal?: AbortSignal) =>
    apiRequest<Page<TraceSummary>>(
      `${admin}/observability/traces?${query(scope)}&${filters.toString()}`,
      { signal },
    ),
  audits: (scope: Scope, filters: URLSearchParams, signal?: AbortSignal) =>
    apiRequest<Page<AuditEntry>>(`${admin}/audit?${query(scope)}&${filters.toString()}`, {
      signal,
    }),
}
