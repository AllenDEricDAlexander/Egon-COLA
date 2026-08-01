import { InMemoryAccessTokenStore, Rbac3Provider, type Rbac3Client } from '@egon-cola/rbac3-react-sdk'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import type { PropsWithChildren } from 'react'
import { describe, expect, it } from 'vitest'
import { FeatureApiProvider, type FeatureApiClient } from '../shared/FeatureApi'
import { RuntimeStatusPage } from './RuntimeStatusPage'

const wrapper = ({ children }: PropsWithChildren) => {
  const sdk = { refresh: async () => ({ accessToken: 'access', roleActivationRequired: false }), getBootstrap: async () => ({ user: { id: '7', tenantId: '9' }, permissions: ['system:authorization-runtime:read', 'system:authorization-runtime:operate'], fieldPolicies: {}, activeRoleContexts: [], apps: [], menus: [], routes: [], actions: [] }) } as unknown as Rbac3Client
  const status = {
    ddcConfigClient: { state: 'READY', instanceId: 'rbac3-1', leaseIdFingerprint: 'af0130b1190e', leaseExpireAt: '2026-08-01T00:00:30Z', configVersions: { 'rbac3.maximum-active-roots': 7 }, lastApplyFailureKey: null, lastApplyFailureVersion: null, lastApplyFailureCode: null },
    definition: { status: 'ACCEPTED', definitionSetId: 'definition-7', warnings: [] },
    providerLease: { state: 'RECOVERING', instanceId: 'rbac3-1', leaseExpireAt: null },
    gatewayRelease: { releaseId: null, status: 'MISSING', observedByEngineVersion: null }, checkedAt: '2026-08-01T00:00:00Z',
    flyway: { rbac3History: 'UP_TO_DATE', outboxHistory: 'UP_TO_DATE' }, redisProjection: { state: 'DEGRADED', checkpointLag: 3 },
    fence: { state: 'STALE', oldestAgeSeconds: 90 }, outbox: { state: 'LAGGING', pendingCount: 12, oldestAgeSeconds: 30 },
  }
  const api: FeatureApiClient = { request: async <T,>(path: string) => (path.includes('/mutations') ? { items: [{ mutationId: '701', scopeType: 'USER', scopeId: '7', commandId: 'cmd-1', status: 'FAILED', attempt: 2, lastErrorCode: 'REDIS_UNAVAILABLE', updatedAt: '2026-08-01T00:00:00Z' }], nextCursor: null } : status) as T }
  return <QueryClientProvider client={new QueryClient()}><Rbac3Provider client={sdk} accessTokenStore={new InMemoryAccessTokenStore()}><FeatureApiProvider client={api}>{children}</FeatureApiProvider></Rbac3Provider></QueryClientProvider>
}

describe('runtime status page', () => {
  it('keeps definition lease release and recovery subsystems independent', async () => {
    render(<RuntimeStatusPage />, { wrapper })
    await waitFor(() => expect(screen.getByText('ACCEPTED')).toBeInTheDocument())
    expect(screen.getByText('DDC Config Client')).toBeInTheDocument()
    expect(screen.getByText('READY')).toBeInTheDocument()
    expect(screen.getByText(/af0130b1190e/)).toBeInTheDocument()
    expect(screen.getByText('RECOVERING')).toBeInTheDocument()
    expect(screen.getByText('MISSING')).toBeInTheDocument()
    expect(screen.getByText('LAGGING')).toBeInTheDocument()
    expect(screen.getByText('STALE')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '重试 701' })).toBeInTheDocument()
    expect(document.body.textContent).not.toMatch(/config-client-lease-secret-value|invalid-secret-like-value|internal storage locator|database statement|endpoint secret/i)
  })
})
