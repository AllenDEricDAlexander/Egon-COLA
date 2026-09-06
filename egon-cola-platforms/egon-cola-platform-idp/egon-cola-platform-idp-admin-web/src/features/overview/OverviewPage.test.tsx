import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {cleanup, render, screen, waitFor} from '@testing-library/react'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {OverviewPage} from './OverviewPage'

const mocks = vi.hoisted(() => ({
  request: vi.fn(),
  activeRoleContexts: [] as {roleId: string; roleCode: string; applicationCode: string}[],
}))

vi.mock('../../auth/AuthContext', () => ({
  httpClient: {request: mocks.request},
  useAuth: () => ({
    bootstrap: {
      user: {id: 'rbac-user-1', tenantId: 'tenant-a', identitySub: 'admin-sub', status: 'ACTIVE'},
      activeRoleContexts: mocks.activeRoleContexts,
      permissions: [],
      apps: [], menus: [], routes: [], actions: [], fieldPolicies: {},
      defaultApplicationCode: 'idp-admin', defaultRoute: null, authVersion: 2, policyVersion: 3,
    },
  }),
}))

vi.mock('@egon-cola/admin-web-shared', async (importOriginal) => ({
  ...await importOriginal<typeof import('@egon-cola/admin-web-shared')>(),
  usePermission: () => ({has: () => false}),
}))

describe('IDP overview', () => {
  afterEach(cleanup)
  beforeEach(() => { mocks.activeRoleContexts = [] })

  it('renders the flat role descriptors returned by the bootstrap contract', async () => {
    mocks.activeRoleContexts = [{roleId: 'role-1', roleCode: 'IDP_ADMIN', applicationCode: 'idp-admin'}]
    mocks.request.mockResolvedValue({subject: 'admin-sub', tenantId: 'tenant-a'})
    render(
      <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
        <OverviewPage />
      </QueryClientProvider>,
    )
    expect(await screen.findByText('role-1')).toBeInTheDocument()
    expect(screen.queryByText('当前上下文未返回角色明细')).not.toBeInTheDocument()
  })

  it('shows safe identity profile fields without token identifiers', async () => {
    mocks.request.mockResolvedValue({
      subject: 'admin-sub',
      tenantId: 'tenant-a',
      tokenId: 'token-a',
      audience: ['private-token-audience'],
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
    expect(screen.queryByText('private-token-audience')).not.toBeInTheDocument()
    expect(screen.queryByText('RBAC3 用户 ID')).not.toBeInTheDocument()
    expect(screen.queryByText('rbac-user-1')).not.toBeInTheDocument()
    expect(screen.getByText('idp-admin')).toBeInTheDocument()
    expect(screen.getByText('当前上下文未返回角色明细')).toBeInTheDocument()
  })
})
