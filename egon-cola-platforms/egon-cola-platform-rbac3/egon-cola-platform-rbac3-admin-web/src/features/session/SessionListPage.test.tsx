import { InMemoryAccessTokenStore, Rbac3Provider, type Rbac3Client } from '@egon-cola/rbac3-react-sdk'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import type { PropsWithChildren } from 'react'
import { describe, expect, it } from 'vitest'
import { FeatureApiProvider, type FeatureApiClient } from '../shared/FeatureApi'
import { SessionListPage } from './SessionListPage'

const wrapper = ({ children }: PropsWithChildren) => {
  const sdk = {
    refresh: async () => ({ accessToken: 'access', roleActivationRequired: false }),
    getBootstrap: async () => ({
      user: { id: '7', tenantId: '9' }, permissions: ['system:session:revoke'],
      fieldPolicies: {}, activeRoleContexts: [], apps: [], menus: [], routes: [], actions: [],
    }),
  } as unknown as Rbac3Client
  const api: FeatureApiClient = { request: async <T,>() => [{
    sessionId: '501', status: 'ACTIVE', sessionVersion: 9,
    authenticatedAt: '2026-08-01T00:00:00Z', lastSeenAt: '2026-08-01T01:00:00Z',
    absoluteExpiresAt: '2026-08-02T00:00:00Z',
  }] as T }
  return (
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <Rbac3Provider client={sdk} accessTokenStore={new InMemoryAccessTokenStore()}>
        <FeatureApiProvider client={api}>{children}</FeatureApiProvider>
      </Rbac3Provider>
    </QueryClientProvider>
  )
}

describe('session page', () => {
  it('renders revocable session metadata without token or snapshot material', async () => {
    render(<SessionListPage />, { wrapper })
    await waitFor(() => expect(screen.getByText('501')).toBeInTheDocument())
    expect(screen.getByText('9')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '撤销会话' })).toBeInTheDocument()
    expect(document.body.textContent).not.toMatch(/Refresh Hash|Access Token|Snapshot Checksum/)
  })
})
