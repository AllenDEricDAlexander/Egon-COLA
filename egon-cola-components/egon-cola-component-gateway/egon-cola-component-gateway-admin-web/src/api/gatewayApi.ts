import { apiRequest } from './client'
import { createLogicalTrace, newIdempotencyKey, type LogicalTrace } from './trace'
import type {
  Application,
  AuditEntry,
  CatalogTree,
  DashboardSummary,
  DraftMutationResult,
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

type ProjectionEnvelope<T> = {
  value: T
  observedAt: string
  source: string
  stale: boolean
  refreshError?: string
}

type EngineNodeResponse = Omit<EngineNode, 'observedAt' | 'stale'>

type RuntimeConsistencyResponse = {
  targetReleaseId?: string
  targetReleaseStatus?: string
  engineNodeCount: number
  readyEngineNodeCount: number
  consistent: boolean
  observedAt: string
  source: string
  stale: boolean
}

type ProviderInstanceResponse = Omit<ProviderInstance, 'stale'>

type DraftResponse = Omit<GatewayDraft, 'routes' | 'policies'> & {
  routes: Array<Omit<GatewayDraft['routes'][number], 'routeContent'> & {
    content: Record<string, unknown>
  }>
  policies: Array<Omit<GatewayDraft['policies'][number], 'policyContent'> & {
    content: Record<string, unknown>
  }>
}

type ReleaseResponse = Omit<GatewayRelease, 'id'> & {
  releaseId: string
}

const mapDraft = (draft: DraftResponse): GatewayDraft => ({
  ...draft,
  routes: draft.routes.map(({ content, ...route }) => ({
    ...route,
    routeContent: content,
  })),
  policies: draft.policies.map(({ content, ...policy }) => ({
    ...policy,
    policyContent: content,
  })),
})

const mapRelease = ({ releaseId, ...release }: ReleaseResponse): GatewayRelease => ({
  ...release,
  id: releaseId,
})

export const gatewayApi = {
  dashboard: (scope: Scope, signal?: AbortSignal) =>
    apiRequest<DashboardSummary>(`${admin}/dashboard?${query(scope)}`, { signal }),
  groups: (scope: Scope, signal?: AbortSignal) =>
    apiRequest<GatewayGroup[]>(`${admin}/gateway-groups?${query(scope)}`, { signal }),
  group: (groupId: string, signal?: AbortSignal) =>
    apiRequest<GatewayGroup>(`${admin}/gateway-groups/${groupId}`, { signal }),
  engineNodes: async (groupId: string, signal?: AbortSignal) => {
    const projection = await apiRequest<ProjectionEnvelope<EngineNodeResponse[]>>(
      `${admin}/gateway-groups/${groupId}/engine-nodes`,
      { signal },
    )
    return projection.value.map((node) => ({
      ...node,
      observedAt: projection.observedAt,
      stale: projection.stale,
    }))
  },
  consistency: async (groupId: string, signal?: AbortSignal) => {
    const value = await apiRequest<RuntimeConsistencyResponse>(
      `${admin}/gateway-groups/${groupId}/runtime-consistency`,
      { signal },
    )
    return {
      targetReleaseId: value.targetReleaseId,
      targetReleaseStatus: value.targetReleaseStatus,
      readyNodes: value.readyEngineNodeCount,
      totalNodes: value.engineNodeCount,
      consistent: value.consistent,
      stale: value.stale,
      source: value.source,
      observedAt: value.observedAt,
    } satisfies RuntimeConsistency
  },
  applications: (scope: Scope, signal?: AbortSignal) =>
    apiRequest<Application[]>(`${admin}/applications?${query(scope)}`, { signal }),
  catalog: (applicationId: string, signal?: AbortSignal) =>
    apiRequest<CatalogTree>(`${admin}/applications/${applicationId}/catalog`, { signal }),
  operation: (operationId: string, signal?: AbortSignal) =>
    apiRequest<OperationDetail>(`${admin}/operations/${operationId}`, { signal }),
  draft: async (groupId: string, signal?: AbortSignal) =>
    mapDraft(await apiRequest<DraftResponse>(
      `${admin}/gateway-groups/${groupId}/draft`,
      { signal },
    )),
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
    return apiRequest<DraftMutationResult>(
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
    return apiRequest<DraftMutationResult>(
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
  releases: async (groupId: string, signal?: AbortSignal) =>
    (await apiRequest<ReleaseResponse[]>(
      `${admin}/gateway-groups/${groupId}/releases`,
      { signal },
    )).map(mapRelease),
  release: (
    releaseId: string,
    signal?: AbortSignal,
    trace?: LogicalTrace,
  ) =>
    apiRequest<ReleaseResponse>(`${admin}/releases/${releaseId}`, {
      signal,
      trace,
    }).then(mapRelease),
  publish: (
    groupId: string,
    draftRevision: number,
    changeReason: string,
    trace = createLogicalTrace(),
  ) =>
    apiRequest<ReleaseResponse>(`${admin}/gateway-groups/${groupId}/releases`, {
      method: 'POST',
      body: { expectedDraftRevision: draftRevision, changeReason },
      trace,
      idempotencyKey: newIdempotencyKey(),
    }).then(mapRelease),
  retryRelease: (releaseId: string, trace = createLogicalTrace()) =>
    apiRequest<ReleaseResponse>(`${admin}/releases/${releaseId}/retry`, {
      method: 'POST',
      body: { reason: 'Retry from Gateway Admin Web' },
      trace,
      idempotencyKey: newIdempotencyKey(),
    }).then(mapRelease),
  rollback: (
    groupId: string,
    releaseId: string,
    expectedDraftRevision: number,
    reason: string,
    trace = createLogicalTrace(),
  ) =>
    apiRequest<ReleaseResponse>(`${admin}/gateway-groups/${groupId}/rollback`, {
      method: 'POST',
      body: {
        sourceReleaseId: releaseId,
        expectedDraftRevision,
        changeReason: reason,
      },
      trace,
      idempotencyKey: newIdempotencyKey(),
    }).then(mapRelease),
  providers: async (scope: Scope, signal?: AbortSignal) => {
    const projection = await apiRequest<ProjectionEnvelope<ProviderInstanceResponse[]>>(
      `${admin}/providers/instances?${query(scope)}`,
      { signal },
    )
    return projection.value.map((instance) => ({
      ...instance,
      stale: projection.stale,
    }))
  },
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
