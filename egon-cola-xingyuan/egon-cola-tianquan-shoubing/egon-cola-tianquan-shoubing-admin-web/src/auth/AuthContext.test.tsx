import {cleanup, render, screen, waitFor} from '@testing-library/react'
import {afterEach, describe, expect, it, vi} from 'vitest'
import {AuthProvider, useAuth} from './AuthContext'

const state = vi.hoisted(() => ({
    bootstrap: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
}))

vi.mock('@egon-cola/xingyuan-admin-web-shared', async () => {
    const actual = await vi.importActual<typeof import('@egon-cola/xingyuan-admin-web-shared')>('@egon-cola/xingyuan-admin-web-shared')
  return {
      ...actual,
      createGatewayAuthClient: () => ({
          bootstrap: state.bootstrap,
          login: state.login,
          logout: state.logout,
      }),
      createHttpClient: () => ({request: vi.fn()}),
  }
})

const Probe = () => {
    const auth = useAuth()
    return <div>{auth.loading ? 'loading' : auth.bootstrap?.user.identitySub ?? 'logged-out'}</div>
}

afterEach(() => {
    cleanup()
    vi.clearAllMocks()
})

describe('Tianquan-Shoubing authentication hydration', () => {
    it('hydrates authorization from the Yuheng cookie bootstrap without browser token state', async () => {
        state.bootstrap.mockResolvedValue({
            user: {id: 'user-1', tenantId: 'default', identitySub: 'alice-sub', status: 'ACTIVE'},
            activeRoleContexts: [], permissions: [], apps: [], menus: [], routes: [], actions: [], fieldPolicies: {},
            defaultApplicationCode: null, defaultRoute: null, authVersion: 1, policyVersion: 1,
        })
        render(<AuthProvider><Probe/></AuthProvider>)
        await waitFor(() => expect(screen.getByText('alice-sub')).toBeInTheDocument())
        expect(state.bootstrap).toHaveBeenCalledTimes(1)
  })
})


describe('fixed deployment registration', () => {
  it('passes the configured alias to the installed client without tenant or issuer selectors', async () => {
    const navigate = vi.fn()
    vi.stubEnv('VITE_OAUTH_REGISTRATION_ID', 'edge-deployment-child')
    vi.stubEnv('VITE_YUHENG_ORIGIN', '')
    vi.resetModules()
    vi.doUnmock('./AuthContext')
    vi.doMock('@egon-cola/xingyuan-admin-web-shared', async () => {
      const actual = await vi.importActual<typeof import('@egon-cola/xingyuan-admin-web-shared')>('@egon-cola/xingyuan-admin-web-shared')
      return {...actual, createGatewayAuthClient: (options: Parameters<typeof actual.createGatewayAuthClient>[0]) =>
        actual.createGatewayAuthClient({...options, navigate})}
    })
    try {
      const {gatewayAuth: configured} = await import('./AuthContext')
      configured.login()
      configured.stepUp()
      expect(navigate.mock.calls[0]?.[0]).toBe('/oauth2/authorization/edge-deployment-child')
      expect(String(navigate.mock.calls[1]?.[0]).split('?')[0]).toBe('/oauth2/authorization/edge-deployment-child')
      expect(String(navigate.mock.calls[1]?.[0])).not.toMatch(/tenant|issuer|resource/)
    } finally {
      vi.doUnmock('@egon-cola/xingyuan-admin-web-shared')
      vi.unstubAllEnvs()
    }
  })
})
