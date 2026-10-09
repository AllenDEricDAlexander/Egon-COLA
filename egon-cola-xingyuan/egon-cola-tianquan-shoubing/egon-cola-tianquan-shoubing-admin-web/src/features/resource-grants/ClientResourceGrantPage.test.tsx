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
      user: { id: 'admin-1', identitySub: 'admin-sub', tenantId: '1001', status: 'ACTIVE' },
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
      <MemoryRouter initialEntries={['/clients/client-1/resource-grants?realm=tenant%3A1001']}>
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
    if (path === '/api/v1/tianquan-shoubing/resource-servers?realm=platform' && !options) return Promise.resolve([resourceServer])
    if (path === '/api/v1/tianquan-shoubing/clients/client-1/resources?realm=tenant%3A1001' && !options) {
      if (state.grantsAvailable) return Promise.resolve([grant])
      return Promise.reject(Object.assign(new Error('Grant query is not available'), { status: 404 }))
    }
    if (path === '/api/v1/tianquan-shoubing/clients/client-1/resources/orders-api?realm=tenant%3A1001' && options?.method === 'PUT') {
      return Promise.resolve(grant)
    }
    if (path === '/api/v1/tianquan-shoubing/clients/client-1/resources/orders-api?realm=tenant%3A1001' && options?.method === 'DELETE') {
      return Promise.resolve(undefined)
    }
    if (path === '/api/v1/tianquan-shoubing/clients/client-1/resource-grants/actions/batch?realm=tenant%3A1001' && options?.method === 'POST') {
      return Promise.resolve([grant])
    }
    return Promise.reject(new Error(`Unexpected request: ${path}`))
  })
})

afterEach(cleanup)

describe('Client Resource Grant administration', () => {
  it('keeps a missing client response distinct from an empty grant list', async () => {
    renderPage()

    expect(await screen.findByText('Orders API')).toBeInTheDocument()
    expect(await screen.findByText(/Client 或 Grant 不存在/)).toBeInTheDocument()
    expect(screen.queryByText('暂无 Resource Server')).not.toBeInTheDocument()
  })

  it('reads existing grants with the verified tenant realm', async () => {
    state.grantsAvailable = true
    renderPage()

    expect(await screen.findByText('Orders API')).toBeInTheDocument()
    expect(await screen.findByText(/orders:read/)).toBeInTheDocument()
    expect(state.request).toHaveBeenCalledWith(
      '/api/v1/tianquan-shoubing/clients/client-1/resources?realm=tenant%3A1001',
    )
  })

  it('uses the existing PUT grant path and sends only the command fields', async () => {
    renderPage()

    await screen.findByText('Orders API')
    fireEvent.click(screen.getByRole('button', { name: /新建.*Grant/ }))
    fireEvent.click(screen.getByRole('button', { name: /确定|OK/ }))

    await waitFor(() => expect(state.request).toHaveBeenCalledWith(
      '/api/v1/tianquan-shoubing/clients/client-1/resources/orders-api?realm=tenant%3A1001',
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

  it('sends the exact explicit business scopes for a USER delegation grant', async () => {
    renderPage()
    await screen.findByText('Orders API')
    fireEvent.click(screen.getByRole('button', { name: /新建.*Grant/ }))
    fireEvent.change(screen.getByLabelText('允许的 Scope（空格分隔）'), { target: { value: 'orders:read orders:write' } })
    fireEvent.click(screen.getByRole('button', { name: /确定|OK/ }))
    await waitFor(() => expect(state.request).toHaveBeenCalledWith(
      '/api/v1/tianquan-shoubing/clients/client-1/resources/orders-api?realm=tenant%3A1001',
      expect.objectContaining({ method: 'PUT', body: JSON.stringify({
        grantType: 'USER_DELEGATION', allowedScopes: ['orders:read', 'orders:write'],
        expectedResourceVersion: resourceServer.version,
      }) }),
    ))
  })

  it.each(['openid', 'orders/read', 'x'.repeat(129),
    Array.from({ length: 65 }, (_, i) => `orders:${i}`).join(' ')])(
    'rejects invalid USER scope input before PUT: %s', async (value) => {
      renderPage()
      await screen.findByText('Orders API')
      fireEvent.click(screen.getByRole('button', { name: /新建.*Grant/ }))
      fireEvent.change(screen.getByLabelText('允许的 Scope（空格分隔）'), { target: { value } })
      fireEvent.click(screen.getByRole('button', { name: /确定|OK/ }))
      expect(await screen.findByText('USER Grant 仅允许最多 64 个业务 Scope，每项最多 128 个字符')).toBeInTheDocument()
      expect(state.request.mock.calls.some(([, options]) => options?.method === 'PUT')).toBe(false)
    },
  )

  it('restores explicit USER scope and grant version when editing', async () => {
    state.request.mockImplementation((path: string, options?: RequestInit) => {
      if (path === '/api/v1/tianquan-shoubing/resource-servers?realm=platform' && !options) return Promise.resolve([resourceServer])
      if (path === '/api/v1/tianquan-shoubing/clients/client-1/resources?realm=tenant%3A1001' && !options) {
        return Promise.resolve([{ ...grant, grantType: 'USER_DELEGATION', tenantId: undefined }])
      }
      return Promise.resolve(grant)
    })
    renderPage()
    await screen.findByText('Orders API')
    fireEvent.click(await screen.findByRole('button', { name: /新建.*更新.*Grant/ }))
    expect(screen.getByLabelText('允许的 Scope（空格分隔）')).toHaveValue('orders:read')
    fireEvent.click(screen.getByRole('button', { name: /确定|OK/ }))
    await waitFor(() => expect(state.request).toHaveBeenCalledWith(
      '/api/v1/tianquan-shoubing/clients/client-1/resources/orders-api?realm=tenant%3A1001',
      expect.objectContaining({ method: 'PUT', body: JSON.stringify({
        grantType: 'USER_DELEGATION', allowedScopes: ['orders:read'],
        expectedResourceVersion: resourceServer.version, expectedGrantVersion: grant.version,
      }) }),
    ))
  })

  it('uses the existing DELETE grant path and retains the row after a conflict', async () => {
    state.grantsAvailable = true
    state.request.mockImplementation((path: string, options?: RequestInit) => {
      if (path === '/api/v1/tianquan-shoubing/resource-servers?realm=platform' && !options) return Promise.resolve([resourceServer])
      if (path === '/api/v1/tianquan-shoubing/clients/client-1/resources?realm=tenant%3A1001' && !options) return Promise.resolve([grant])
      if (path === '/api/v1/tianquan-shoubing/clients/client-1/resources/orders-api?realm=tenant%3A1001' && options?.method === 'DELETE') {
        return Promise.reject(Object.assign(new Error('版本冲突'), {status: 409}))
      }
      return Promise.reject(new Error(`Unexpected request: ${path}`))
    })

    renderPage()

    await screen.findByText('Orders API')
    fireEvent.click(await screen.findByRole('button', {name: /删除 Grant/}))
    fireEvent.click(screen.getByRole('button', {name: '确认删除'}))

    await waitFor(() => expect(state.request).toHaveBeenCalledWith(
      '/api/v1/tianquan-shoubing/clients/client-1/resources/orders-api?realm=tenant%3A1001',
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
      '/api/v1/tianquan-shoubing/clients/client-1/resource-grants/actions/batch?realm=tenant%3A1001',
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
