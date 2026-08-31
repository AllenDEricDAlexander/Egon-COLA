import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {render, screen, waitFor} from '@testing-library/react'
import {describe, expect, it, vi} from 'vitest'
import {OverviewPage} from './OverviewPage'

const mocks = vi.hoisted(() => ({request: vi.fn()}))

vi.mock('../../auth/AuthContext', () => ({
  httpClient: {request: mocks.request},
  useAuth: () => ({
    bootstrap: {
      user: {id: 'rbac-user-1', tenantId: 'tenant-a', identitySub: 'admin-sub', status: 'ACTIVE'},
      activeRoleContexts: [],
      permissions: [],
      apps: [], menus: [], routes: [], actions: [], fieldPolicies: {},
      defaultApplicationCode: null, defaultRoute: null, authVersion: 2, policyVersion: 3,
    },
  }),
}))

vi.mock('@egon-cola/admin-web-shared', async (importOriginal) => ({
  ...await importOriginal<typeof import('@egon-cola/admin-web-shared')>(),
  usePermission: () => ({has: () => false}),
}))

describe('IDP overview', () => {
  it('shows safe identity profile fields without token identifiers', async () => {
    mocks.request.mockResolvedValue({
      subject: 'admin-sub',
      tenantId: 'tenant-a',
      tokenId: 'token-a',
      audience: ['idp-admin'],
      issuedAt: '2026-08-31T00:00:00Z',
      expiresAt: '2026-09-01T00:00:00Z',
    })
    render(
      <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
        <OverviewPage />
      </QueryClientProvider>,
    )

    expect(await screen.findByText('当前统一身份')).toBeInTheDocument()
    await waitFor(() => expect(mocks.request).toHaveBeenCalledWith('/api/v1/identity/me'))
    expect(screen.getAllByText('admin-sub').length).toBeGreaterThan(0)
    expect(screen.getAllByText('tenant-a').length).toBeGreaterThan(0)
    expect(screen.queryByText('token-a')).not.toBeInTheDocument()
    expect(screen.queryByText('idp-admin')).not.toBeInTheDocument()
  })
})
