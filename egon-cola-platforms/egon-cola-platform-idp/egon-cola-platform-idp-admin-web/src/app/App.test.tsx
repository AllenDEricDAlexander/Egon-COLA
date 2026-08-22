import {cleanup, render, screen, waitFor} from '@testing-library/react'
import type {ReactNode} from 'react'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {App} from './App'

const admin = vi.hoisted(() => ({ request: vi.fn() }))

vi.mock('../auth/AuthContext', () => ({
  AuthProvider: ({ children }: { children: ReactNode }) => children,
  useAuth: () => ({
    loading: false,
    bootstrap: {
        user: {id: 'user-1', identitySub: 'alice-sub', tenantId: 'default', status: 'ACTIVE'},
        activeRoleContexts: [],
      permissions: [
          'idp:identity-user:read',
          'idp:oauth-client:read',
          'idp:oauth-client:create',
          'idp:oauth-client:update',
          'idp:resource-server:read',
          'idp:resource-server:create',
          'idp:resource-server:status',
          'idp:resource-server:grant',
          'idp:signing-key:read',
          'idp:audit:read',
      ],
        apps: [], menus: [], routes: [], actions: [], fieldPolicies: {},
        defaultApplicationCode: null, defaultRoute: null, authVersion: 1, policyVersion: 1,
    },
  }),
  httpClient: { request: admin.request },
}))

beforeEach(() => {
  admin.request.mockReset().mockImplementation((path: string) => {
    if (path === '/api/v1/identity/users') {
      return Promise.resolve([{
        subject: 'alice-sub',
        username: 'alice',
        displayName: 'Alice',
        status: 'ACTIVE',
      }])
    }
    if (path === '/api/v1/identity/clients') {
      return Promise.resolve([{
        appId: 'idp-admin-web',
        clientId: 'idp-admin-web',
        clientName: 'IdP Admin Web',
        clientType: 'PUBLIC',
        status: 'ACTIVE',
        pkceRequired: true,
        accessTokenTtlSeconds: 900,
        refreshTokenTtlSeconds: 604800,
        redirectUris: ['http://127.0.0.1:18121/oauth/callback'],
        resourceUris: ['https://api.egon.internal/local/permission/idp'],
        version: 1,
        createdAt: '2026-08-06T10:00:00Z',
        updatedAt: '2026-08-06T10:00:00Z',
      }])
    }
    if (path === '/api/v1/identity/resource-servers') {
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
    if (path === '/api/v1/identity/signing-keys') {
      return Promise.resolve([{
        kid: 'kid-1',
        algorithm: 'RS256',
        status: 'ACTIVE',
        runtimeServing: true,
        version: 1,
      }])
    }
    if (path === '/api/v1/identity/audits?page=0&size=20') {
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
        totalElements: 1,
      })
    }
    return Promise.reject(new Error(`Unexpected request: ${path}`))
  })
})

afterEach(cleanup)

describe('IdP Admin application providers', () => {
  it.each([
    ['/users', '/api/v1/identity/users', 'alice'],
    ['/clients', '/api/v1/identity/clients', 'IdP Admin Web'],
    ['/keys', '/api/v1/identity/signing-keys', 'kid-1'],
    ['/audits', '/api/v1/identity/audits?page=0&size=20', 'LOGIN_SUCCEEDED'],
  ])('renders the data page at %s', async (route, requestPath, expectedText) => {
    window.history.replaceState({}, '', route)

    render(<App />)

    await waitFor(() => expect(screen.getByText(expectedText)).toBeInTheDocument())
    expect(admin.request).toHaveBeenCalledWith(requestPath)
  })

  it('keeps Resource Server management free of retired Client JWK and Admission controls', async () => {
    window.history.replaceState({}, '', '/resource-servers')

    render(<App />)

    await waitFor(() => expect(screen.getByText('Orders API')).toBeInTheDocument())
    expect(screen.queryByText(/JWK|准入|Admission/)).not.toBeInTheDocument()
    expect(admin.request).toHaveBeenCalledWith('/api/v1/identity/resource-servers')
  })
})
