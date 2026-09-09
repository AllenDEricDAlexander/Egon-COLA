import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ClientResourceGrantPage } from './ClientResourceGrantPage'

const state = vi.hoisted(() => ({
  permissions: [] as string[],
  grantsAvailable: false,
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

const grant = {
  clientId: 'client-1',
  resourceServerId: 'orders-api',
  grantType: 'CLIENT_CREDENTIALS',
  tenantId: 'tenant-1',
  allowedScopes: ['orders:read'],
  status: 'ACTIVE',
  version: 2,
}

const renderPage = () => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/clients/client-1/resource-grants']}>
        <Routes>
          <Route path="/clients/:clientId/resource-grants" element={<ClientResourceGrantPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

beforeEach(() => {
  state.permissions = ['tianquan-shoubing:resource-server:grant']
  state.grantsAvailable = false
  state.request.mockReset().mockImplementation((path: string, options?: RequestInit) => {
    if (path === '/api/v1/tianquan-shoubing/resource-servers' && !options) return Promise.resolve([resourceServer])
    if (path === '/api/v1/tianquan-shoubing/clients/client-1/resources' && !options) {
      if (state.grantsAvailable) return Promise.resolve([grant])
      return Promise.reject(Object.assign(new Error('Grant query is not available'), { status: 404 }))
    }
    if (path === '/api/v1/tianquan-shoubing/clients/client-1/resources/orders-api' && options?.method === 'PUT') {
      return Promise.resolve(grant)
    }
    if (path === '/api/v1/tianquan-shoubing/clients/client-1/resources/orders-api' && options?.method === 'DELETE') {
      return Promise.resolve(undefined)
    }
    if (path === '/api/v1/tianquan-shoubing/clients/client-1/resource-grants/actions/batch' && options?.method === 'POST') {
      return Promise.resolve([grant])
    }
    return Promise.reject(new Error(`Unexpected request: ${path}`))
  })
})

afterEach(cleanup)

describe('Client Resource Grant administration', () => {
  it('shows an unavailable read capability instead of a false empty grant table', async () => {
    renderPage()

    expect(await screen.findByText('Orders API')).toBeInTheDocument()
    expect(await screen.findByText(/Grant 读取接口待补齐/)).toBeInTheDocument()
    expect(screen.queryByText('暂无 Resource Server')).not.toBeInTheDocument()
  })

  it('uses the existing PUT grant path and sends only the command fields', async () => {
    renderPage()

    await screen.findByText('Orders API')
    fireEvent.click(screen.getByRole('button', { name: /新建.*Grant/ }))
    fireEvent.click(screen.getByRole('button', { name: /确定|OK/ }))

    await waitFor(() => expect(state.request).toHaveBeenCalledWith(
      '/api/v1/tianquan-shoubing/clients/client-1/resources/orders-api',
      expect.objectContaining({
        method: 'PUT',
        body: JSON.stringify({
          grantType: 'USER_DELEGATION',
          allowedScopes: [],
          expectedResourceVersion: resourceServer.version,
        }),
      }),
    ))
  })

  it('uses the existing DELETE grant path and retains the row after a conflict', async () => {
    state.grantsAvailable = true
    state.request.mockImplementation((path: string, options?: RequestInit) => {
      if (path === '/api/v1/tianquan-shoubing/resource-servers' && !options) return Promise.resolve([resourceServer])
      if (path === '/api/v1/tianquan-shoubing/clients/client-1/resources' && !options) return Promise.resolve([grant])
      if (path === '/api/v1/tianquan-shoubing/clients/client-1/resources/orders-api' && options?.method === 'DELETE') {
        return Promise.reject(Object.assign(new Error('版本冲突'), {status: 409}))
      }
      return Promise.reject(new Error(`Unexpected request: ${path}`))
    })

    renderPage()

    await screen.findByText('Orders API')
    fireEvent.click(await screen.findByRole('button', {name: /删除 Grant/}))
    fireEvent.click(screen.getByRole('button', {name: '确认删除'}))

    await waitFor(() => expect(state.request).toHaveBeenCalledWith(
      '/api/v1/tianquan-shoubing/clients/client-1/resources/orders-api',
      expect.objectContaining({
        method: 'DELETE',
        body: JSON.stringify({
          grantType: grant.grantType,
          tenantId: grant.tenantId,
          expectedResourceVersion: resourceServer.version,
          expectedGrantVersion: grant.version,
        }),
      }),
    ))
    expect(await screen.findByText(/版本冲突/)).toBeInTheDocument()
    expect(screen.getByText('Orders API')).toBeInTheDocument()
  })

  it('uses the existing batch grant path for selected grants', async () => {
    state.grantsAvailable = true

    renderPage()

    await screen.findByText('Orders API')
    const checkboxes = screen.getAllByRole('checkbox')
    fireEvent.click(checkboxes[1])
    fireEvent.click(screen.getByRole('button', {name: '批量删除 Grant'}))
    fireEvent.click(screen.getByRole('button', {name: '确认批量删除'}))

    await waitFor(() => expect(state.request).toHaveBeenCalledWith(
      '/api/v1/tianquan-shoubing/clients/client-1/resource-grants/actions/batch',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          bizCode: resourceServer.bizCode,
          environment: resourceServer.environment,
          appCodes: [resourceServer.appCode],
          action: 'DELETE',
          grantType: grant.grantType,
          tenantId: grant.tenantId,
          allowedScopes: [],
          expectedResourceVersions: {[resourceServer.appCode]: resourceServer.version},
          expectedGrantVersions: {[resourceServer.appCode]: grant.version},
        }),
      }),
    ))
  })
})
