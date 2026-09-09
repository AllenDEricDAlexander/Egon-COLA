import type { FeatureApiClient } from '../shared/FeatureApi'

export interface AuditView {
  readonly id: string
  readonly tenantId: string
  readonly eventType: string
  readonly outcome: string
  readonly severity: string
  readonly actorType: string
  readonly actorId: string
  readonly targetType: string
  readonly targetId: string
  readonly managementPolicyId?: string | null
  readonly reasonCode: string
  readonly requestId: string
  readonly traceId: string
  readonly beforeSnapshot: Readonly<Record<string, unknown>>
  readonly afterSnapshot: Readonly<Record<string, unknown>>
  readonly payloadChecksum: string
  readonly createdAt: string
}
export interface AuditPage { readonly items: readonly AuditView[]; readonly nextCursor: string | null }
export interface AuditFilter {
  readonly from: string
  readonly to: string
  readonly actorId?: string
  readonly eventType?: string
  readonly outcome?: string
  readonly reasonCode?: string
  readonly traceId?: string
  readonly cursor?: string
  readonly limit: number
}

export const auditApi = (client: FeatureApiClient) => ({
  list: (filter: AuditFilter) => {
    const requestId = crypto.randomUUID()
    return client.request<AuditPage>('/api/rbac3/v1/audit-logs', {
      query: {
        from: filter.from,
        to: filter.to,
        actorId: filter.actorId,
        eventType: filter.eventType,
        outcome: filter.outcome,
        reasonCode: filter.reasonCode,
        traceId: filter.traceId,
        cursor: filter.cursor,
        limit: filter.limit,
      },
      headers: { 'X-Request-Id': requestId, 'X-Trace-Id': requestId },
    })
  },
})
