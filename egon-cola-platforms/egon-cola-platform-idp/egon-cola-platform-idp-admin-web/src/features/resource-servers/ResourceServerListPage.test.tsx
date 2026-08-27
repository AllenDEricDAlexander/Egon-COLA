import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ResourceServerListPage } from './ResourceServerListPage'

const state = vi.hoisted(() => ({
  permissions: [] as string[],
  request: vi.fn(),
}))

vi.mock('../../auth/AuthContext', () => ({
  useAuth: () => ({
    loading: false,
    bootstrap: {
      user: { id: 'admin-1', identitySub: 'admin-sub', tenantId: 'default', status: 'ACTIVE' },
      activeRoleContexts: [],
      permissions: state.permissions,
      apps: [], menus: [], routes: [], actions: [], fieldPolicies: {},
      defaultApplicationCode: null, defaultRoute: null, authVersion: 1, policyVersion: 1,
    },
  }),
  httpClient: { request: state.request },
}))

const resourceServer = {
  resourceServerId: 'orders-api',
  resourceUri: 'https://api.example.com/orders',
  bizCode: 'commerce',
  appCode: 'orders',
  environment: 'prod',
  displayName: 'Orders API',
  managementClientId: 'orders-management',
  rbacApplicationCode: 'commerce',
  entryPermissionCode: 'orders:read',
  status: 'ACTIVE',
  version: 4,
  createdAt: '2026-08-27T01:00:00Z',
  updatedAt: '2026-08-27T01:00:00Z',
}

const renderPage = () => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/resource-servers?page=0&size=20']}>
        <ResourceServerListPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

beforeEach(() => {
  state.permissions = ['idp:resource-server:read', 'idp:resource-server:status']
  state.request.mockReset().mockImplementation((path: string, options?: RequestInit) => {
    if (path === '/api/v1/identity/resource-servers?page=0&size=20' && !options) {
      return Promise.resolve({ content: [resourceServer], page: 0, size: 20, totalElements: 21, totalPages: 2 })
    }
    if (path === `/api/v1/identity/resource-servers/${resourceServer.resourceServerId}/disable`) {
      return Promise.reject(new Error('版本冲突'))
    }
    return Promise.reject(new Error(`Unexpected request: ${path}`))
  })
})

afterEach(cleanup)

describe('Resource Server administration', () => {
  it('renders server total and opens a safe detail drawer', async () => {
    renderPage()

    expect(await screen.findByText('Orders API')).toBeInTheDocument()
    expect(screen.getByText('共 21 条')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: /查看详情/ }))
    expect(await screen.findByRole('dialog')).toHaveTextContent('orders-api')
  })

  it('keeps the row and exposes a conflict after a status mutation fails', async () => {
    renderPage()

    await screen.findByText('Orders API')
    fireEvent.click(screen.getByRole('button', { name: /查看详情/ }))
    fireEvent.click(await screen.findByRole('button', { name: /禁用/ }))
    await waitFor(() => expect(screen.getByText('版本冲突')).toBeInTheDocument())
    expect(screen.getAllByText('Orders API').length).toBeGreaterThan(0)
  })
})
