import type { FeatureApiClient } from '../shared/FeatureApi'

export interface ControlPlaneRuntimeStatus {
  readonly definition: { readonly status: string; readonly definitionSetId: string | null; readonly warnings: readonly string[] }
  readonly providerLease: { readonly state: string; readonly instanceId: string | null; readonly leaseExpireAt: string | null }
  readonly gatewayRelease: { readonly releaseId: string | null; readonly status: string; readonly observedByEngineVersion: string | null }
  readonly checkedAt: string
  readonly flyway?: { readonly rbac3History: string; readonly outboxHistory: string }
  readonly redisProjection?: { readonly state: string; readonly checkpointLag: number }
  readonly fence?: { readonly state: string; readonly oldestAgeSeconds: number }
  readonly outbox?: { readonly state: string; readonly pendingCount: number; readonly oldestAgeSeconds: number }
}
export interface MutationView {
  readonly mutationId: string
  readonly scopeType: string
  readonly scopeId: string
  readonly commandId: string
  readonly status: string
  readonly attempt: number
  readonly lastErrorCode: string | null
  readonly updatedAt: string
}
export interface MutationPage { readonly items: readonly MutationView[]; readonly nextCursor: string | null }

export const runtimeApi = (client: FeatureApiClient) => ({
  status: () => client.request<ControlPlaneRuntimeStatus>('/api/rbac3/v1/runtime/status'),
  mutations: (status?: string, cursor?: string) => client.request<MutationPage>('/api/rbac3/v1/runtime/mutations', { query: { status, cursor, limit: 50 } }),
  retryMutation: (mutationId: string) => client.request<{ readonly mutationId: string; readonly status: string }>(
    `/api/rbac3/v1/runtime/mutations/${encodeURIComponent(mutationId)}/retry`,
    { method: 'POST' },
  ),
})
