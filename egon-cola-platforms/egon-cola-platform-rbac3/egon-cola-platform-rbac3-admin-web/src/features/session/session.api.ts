import type { FeatureApiClient } from '../shared/FeatureApi'

export interface SessionView {
  readonly sessionId: string
  readonly status: string
  readonly sessionVersion: number
  readonly authenticatedAt: string
  readonly lastSeenAt: string
  readonly absoluteExpiresAt: string
}

export const sessionApi = (client: FeatureApiClient) => ({
  mine: () => client.request<readonly SessionView[]>('/api/rbac3/v1/sessions/mine'),
  revoke: (sessionId: string) => client.request<{ readonly success: boolean; readonly stateChanged: boolean }>(
    `/api/rbac3/v1/sessions/${encodeURIComponent(sessionId)}`,
    { method: 'DELETE' },
  ),
})
