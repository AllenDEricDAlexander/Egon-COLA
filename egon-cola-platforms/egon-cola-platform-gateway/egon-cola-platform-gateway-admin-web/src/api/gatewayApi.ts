import { apiRequest } from './client'
import { createLogicalTrace, newIdempotencyKey, type LogicalTrace } from './trace'
import type {
  Application,
  AdminSession,
  AuditEntry,
  CatalogTree,
  Credential,
  DashboardSummary,
  DraftMutationResult,
  EngineNode,
  GatewayDraft,
  GatewayGroup,
  GatewayRelease,
  GatewayScopeBinding,
  IssuedCredential,
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
  new URLSearchParams({
    bizCode: scope.bizCode,
    appCode: scope.appCode,
    env: scope.env,
    namespace: scope.namespace,
  }).toString()

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
  session: (signal?: AbortSignal) =>
    apiRequest<AdminSession>(`${admin}/session`, { signal }),
  scopes: (signal?: AbortSignal) =>
    apiRequest<GatewayScopeBinding[]>(`${admin}/scopes`, { signal }),
  dashboard: (scope: Scope, signal?: AbortSignal) =>
    apiRequest<DashboardSummary>(`${admin}/dashboard?${query(scope)}`, { signal }),
  groups: (scope: Scope, signal?: AbortSignal) =>
    apiRequest<GatewayGroup[]>(`${admin}/gateway-groups?${query(scope)}`, { signal }),
  createGroup: (
    group: {
      gatewayGroupCode: string
      displayName: string
      env: string
      namespace: string
      description?: string
    },
    trace = createLogicalTrace(),
  ) => apiRequest<GatewayGroup>(`${admin}/gateway-groups`, {
    method: 'POST',
    body: group,
    trace,
    idempotencyKey: newIdempotencyKey(),
  }),
  updateGroup: (
    groupId: string,
    group: { displayName: string; description?: string; expectedRevision: number },
    trace = createLogicalTrace(),
  ) => apiRequest<GatewayGroup>(`${admin}/gateway-groups/${groupId}`, {
    method: 'PUT',
    body: group,
    trace,
    idempotencyKey: newIdempotencyKey(),
  }),
  setGroupEnabled: (
    groupId: string,
    enabled: boolean,
    trace = createLogicalTrace(),
  ) => apiRequest<GatewayGroup>(
    `${admin}/gateway-groups/${groupId}/${enabled ? 'enable' : 'disable'}`,
    { method: 'POST', trace, idempotencyKey: newIdempotencyKey() },
  ),
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
  createApplication: (
    application: {
      applicationCode: string
      displayName: string
      env: string
      namespace: string
      description?: string
    },
  ) => apiRequest<Application>(`${admin}/applications`, {
    method: 'POST',
    body: application,
    idempotencyKey: newIdempotencyKey(),
  }),
  updateApplication: (
    applicationId: string,
    application: {
      displayName: string
      description?: string
      expectedRevision: number
    },
  ) => apiRequest<Application>(`${admin}/applications/${applicationId}`, {
    method: 'PUT',
    body: application,
    idempotencyKey: newIdempotencyKey(),
  }),
  credentials: (applicationId: string, signal?: AbortSignal) =>
    apiRequest<Credential[]>(`${admin}/applications/${applicationId}/credentials`, { signal }),
  createCredential: (applicationId: string) =>
    apiRequest<IssuedCredential>(`${admin}/applications/${applicationId}/credentials`, {
      method: 'POST',
      idempotencyKey: newIdempotencyKey(),
    }),
  rotateCredential: (applicationId: string, keyId: string, overlapMinutes: number) =>
    apiRequest<IssuedCredential>(
      `${admin}/applications/${applicationId}/credentials/${keyId}/rotate`,
      {
        method: 'POST',
        body: { overlapMinutes },
        idempotencyKey: newIdempotencyKey(),
      },
    ),
  revokeCredential: (applicationId: string, keyId: string) =>
    apiRequest<Credential>(
      `${admin}/applications/${applicationId}/credentials/${keyId}/revoke`,
      { method: 'POST', idempotencyKey: newIdempotencyKey() },
    ),
  catalog: (applicationId: string, signal?: AbortSignal) =>
    apiRequest<CatalogTree>(`${admin}/applications/${applicationId}/catalog`, { signal }),
  createManualInterfaceGroup: (
    applicationId: string,
    hierarchy: {
      businessCode: string
      businessName: string
      entityCode: string
      entityName: string
      interfaceGroupCode: string
      interfaceGroupName: string
      className?: string
      description?: string
    },
  ) => apiRequest<{ id: string }>(
    `${admin}/applications/${applicationId}/manual-interface-groups`,
    {
      method: 'POST',
      body: hierarchy,
      idempotencyKey: newIdempotencyKey(),
    },
  ),
  createManualOperation: (
    interfaceGroupId: string,
    operation: Record<string, unknown>,
  ) => apiRequest<OperationDetail>(
    `${admin}/interface-groups/${interfaceGroupId}/manual-operations`,
    {
      method: 'POST',
      body: operation,
      idempotencyKey: newIdempotencyKey(),
    },
  ),
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
  deleteRoute: (
    groupId: string,
    routeId: string,
    revision: number,
    changeReason: string,
  ) => {
    const idempotencyKey = newIdempotencyKey()
    return apiRequest<DraftMutationResult>(
      `${admin}/gateway-groups/${groupId}/draft/routes/${routeId}`,
      {
      method: 'DELETE',
      body: { expectedRevision: revision, changeReason, idempotencyKey },
      idempotencyKey,
      },
    )
  },
  deletePolicy: (
    groupId: string,
    policyId: string,
    revision: number,
    changeReason: string,
  ) => {
    const idempotencyKey = newIdempotencyKey()
    return apiRequest<DraftMutationResult>(
      `${admin}/gateway-groups/${groupId}/draft/policies/${policyId}`,
      {
      method: 'DELETE',
      body: { expectedRevision: revision, changeReason, idempotencyKey },
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
