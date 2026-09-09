import {cleanup, fireEvent, render, screen, waitFor} from '@testing-library/react'
import type {ReactNode} from 'react'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {App} from './App'

const admin = vi.hoisted(() => ({
  request: vi.fn(),
  permissions: [
    'tianquan-shoubing:identity-user:read',
    'tianquan-shoubing:oauth-client:read',
    'tianquan-shoubing:oauth-client:create',
    'tianquan-shoubing:oauth-client:update',
    'tianquan-shoubing:tenant:read',
    'tianquan-shoubing:tenant:manage',
    'tianquan-shoubing:resource-server:read',
    'tianquan-shoubing:resource-server:create',
    'tianquan-shoubing:resource-server:status',
    'tianquan-shoubing:resource-server:grant',
    'tianquan-shoubing:signing-key:read',
    'tianquan-shoubing:audit:read',
  ],
}))

vi.mock('../auth/AuthContext', () => ({
  AuthProvider: ({ children }: { children: ReactNode }) => children,
  useAuth: () => ({
    loading: false,
    bootstrap: {
        user: {id: 'user-1', identitySub: 'alice-sub', tenantId: 'default', status: 'ACTIVE'},
        activeRoleContexts: [],
      permissions: admin.permissions,
        apps: [], menus: [], routes: [], actions: [], fieldPolicies: {},
        defaultApplicationCode: null, defaultRoute: null, authVersion: 1, policyVersion: 1,
    },
  }),
  httpClient: { request: admin.request },
}))

beforeEach(() => {
  admin.permissions = [
    'tianquan-shoubing:identity-user:read', 'tianquan-shoubing:oauth-client:read', 'tianquan-shoubing:oauth-client:create',
    'tianquan-shoubing:oauth-client:update', 'tianquan-shoubing:tenant:read', 'tianquan-shoubing:tenant:manage',
    'tianquan-shoubing:resource-server:read', 'tianquan-shoubing:resource-server:create',
    'tianquan-shoubing:resource-server:status', 'tianquan-shoubing:resource-server:grant',
    'tianquan-shoubing:signing-key:read', 'tianquan-shoubing:audit:read',
  ]
  admin.request.mockReset().mockImplementation((path: string) => {
    if (path === '/api/v1/tianquan-shoubing/users?page=0&size=20') {
      return Promise.resolve({
        content: [{
          subject: 'alice-sub',
          username: 'alice',
          displayName: 'Alice',
          status: 'ACTIVE',
          failedLoginCount: 0,
          version: 1,
        }],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      })
    }
    if (path === '/api/v1/tianquan-shoubing/users') {
      return Promise.resolve([{
        subject: 'alice-sub',
        username: 'alice',
        displayName: 'Alice',
        status: 'ACTIVE',
      }])
    }
    if (path === '/api/v1/tianquan-shoubing/clients?page=0&size=20') {
      return Promise.resolve({
        content: [{
          appId: 'tianquan-shoubing-admin-web',
          clientId: 'tianquan-shoubing-admin-web',
          clientName: 'Tianquan-Shoubing Admin Web',
          clientType: 'PUBLIC',
          status: 'ACTIVE',
          pkceRequired: true,
          accessTokenTtlSeconds: 900,
          refreshTokenTtlSeconds: 604800,
          redirectUris: ['http://127.0.0.1:18121/oauth/callback'],
          resourceUris: ['https://api.egon.internal/local/permission/tianquan-shoubing'],
          version: 1,
          createdAt: '2026-08-06T10:00:00Z',
          updatedAt: '2026-08-06T10:00:00Z',
        }],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      })
    }
    if (path === '/api/v1/tianquan-shoubing/clients') {
      return Promise.resolve([{
        appId: 'tianquan-shoubing-admin-web',
        clientId: 'tianquan-shoubing-admin-web',
        clientName: 'Tianquan-Shoubing Admin Web',
        clientType: 'PUBLIC',
        status: 'ACTIVE',
        pkceRequired: true,
        accessTokenTtlSeconds: 900,
        refreshTokenTtlSeconds: 604800,
        redirectUris: ['http://127.0.0.1:18121/oauth/callback'],
        resourceUris: ['https://api.egon.internal/local/permission/tianquan-shoubing'],
        version: 1,
        createdAt: '2026-08-06T10:00:00Z',
        updatedAt: '2026-08-06T10:00:00Z',
      }])
    }
    if (path === '/api/v1/tianquan-shoubing/clients/client-1/resources') {
      return Promise.reject(Object.assign(new Error('Grant read endpoint is not available'), {status: 404}))
    }
    if (path === '/api/v1/tianquan-shoubing/resource-servers') {
      return Promise.resolve([{
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
        version: 1,
        createdAt: '2026-08-06T10:00:00Z',
        updatedAt: '2026-08-06T10:00:00Z',
      }])
    }
    if (path === '/api/v1/tianquan-shoubing/tenants?page=0&size=20') {
      return Promise.resolve({
        content: [{
          tenantId: 'tenant-1',
          tenantCode: 'acme',
          tenantName: 'Acme',
          status: 'ACTIVE',
          settings: {region: 'cn'},
          version: 1,
          createdAt: '2026-08-06T10:00:00Z',
          updatedAt: '2026-08-06T10:00:00Z',
        }],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      })
    }
    if (path === '/api/v1/tianquan-shoubing/signing-keys') {
      return Promise.resolve([{
        kid: 'kid-1',
        algorithm: 'RS256',
        status: 'ACTIVE',
        runtimeServing: true,
        version: 1,
      }])
    }
    if (path === '/api/v1/tianquan-shoubing/audits?page=0&size=20') {
      return Promise.resolve({
        content: [{
          id: 'audit-1',
          eventType: 'LOGIN_SUCCEEDED',
          actorSub: 'alice-sub',
          targetSub: 'alice-sub',
          result: 'SUCCESS',
          reason: 'password',
          occurredAt: '2026-08-06T10:00:00Z',
        }],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      })
    }
    return Promise.reject(new Error(`Unexpected request: ${path}`))
  })
})

afterEach(cleanup)

describe('Tianquan-Shoubing Admin application providers', () => {
  it('renders the Tianquan-Shoubing domain navigation when embedded', async () => {
    const originalMatchMedia = window.matchMedia
    Object.defineProperty(window, 'matchMedia', {
      configurable: true,
      writable: true,
      value: (query: string) => ({
        matches: query.includes('min-width'),
        media: query,
        onchange: null,
        addListener: () => undefined,
        removeListener: () => undefined,
        addEventListener: () => undefined,
        removeEventListener: () => undefined,
        dispatchEvent: () => false,
      }),
    })
    try {
      window.history.replaceState({}, '', '/overview')
      render(<App embedded />)

      await waitFor(() => expect(screen.getByText('当前授权上下文')).toBeInTheDocument())
      await waitFor(() => expect(screen.getByRole('navigation', {name: '主菜单'})).toBeInTheDocument())
      expect(screen.getByText('身份目录')).toBeInTheDocument()
      expect(screen.queryByText('统一身份平台')).not.toBeInTheDocument()
    } finally {
      Object.defineProperty(window, 'matchMedia', {
        configurable: true,
        writable: true,
        value: originalMatchMedia,
      })
    }
  })

  it.each([
    ['/users', '/api/v1/tianquan-shoubing/users', 'alice'],
    ['/clients', '/api/v1/tianquan-shoubing/clients', 'Tianquan-Shoubing Admin Web'],
    ['/tenants', '/api/v1/tianquan-shoubing/tenants?page=0&size=20', 'Acme'],
    ['/keys', '/api/v1/tianquan-shoubing/signing-keys', 'kid-1'],
    ['/audits', '/api/v1/tianquan-shoubing/audits?page=0&size=20', 'LOGIN_SUCCEEDED'],
  ])('renders the data page at %s', async (route, requestPath, expectedText) => {
    window.history.replaceState({}, '', route)

    render(<App />)

    await waitFor(() => expect(screen.getByText(expectedText)).toBeInTheDocument())
    expect(admin.request).toHaveBeenCalledWith(requestPath)
  })

  it('restores an explicit paged identity route with the page wrapper', async () => {
    window.history.replaceState({}, '', '/users?page=0&size=20')

    render(<App />)

    await waitFor(() => expect(screen.getByText('alice')).toBeInTheDocument())
    expect(admin.request).toHaveBeenCalledWith('/api/v1/tianquan-shoubing/users?page=0&size=20')
  })

  it('keeps Resource Server management free of retired Client JWK and Admission controls', async () => {
    window.history.replaceState({}, '', '/resource-servers')

    render(<App />)

    await waitFor(() => expect(screen.getByText('Orders API')).toBeInTheDocument())
    expect(screen.queryByText(/JWK|准入|Admission/)).not.toBeInTheDocument()
    expect(admin.request).toHaveBeenCalledWith('/api/v1/tianquan-shoubing/resource-servers')
  })

  it('recursively prunes unauthorized navigation groups', async () => {
    admin.permissions = ['tianquan-shoubing:identity-user:read']
    window.history.replaceState({}, '', '/users')

    render(<App />)

    await waitFor(() => expect(screen.getByText('alice')).toBeInTheDocument())
    fireEvent.click(screen.getByRole('button', {name: '打开导航'}))
    fireEvent.click(screen.getByText('身份目录'))
    expect(screen.getByText('身份目录')).toBeInTheDocument()
    expect(screen.getAllByText('全局用户').length).toBeGreaterThan(0)
    expect(screen.queryByText('OAuth 与资源')).not.toBeInTheDocument()
    expect(screen.queryByText('安全治理')).not.toBeInTheDocument()
  })

  it('selects OAuth clients for a resource-grant deep link', async () => {
    window.history.replaceState({}, '', '/clients/client-1/resource-grants')
    render(<App />)
    await waitFor(() => expect(screen.getByRole('button', {name: '打开导航'})).toBeInTheDocument())
    fireEvent.click(screen.getByRole('button', {name: '打开导航'}))
    fireEvent.click(screen.getByText('OAuth 与资源'))
    expect(screen.getByText('OAuth 客户端').closest('.ant-menu-item')).toHaveClass('ant-menu-item-selected')
    expect(await screen.findByText(/Grant 读取接口待补齐/)).toBeInTheDocument()
  })
})
