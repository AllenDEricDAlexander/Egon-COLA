import {cleanup, render, screen, waitFor} from '@testing-library/react'
import {afterEach, describe, expect, it, vi} from 'vitest'
import {AuthProvider, useAuth} from './AuthContext'

const state = vi.hoisted(() => ({
    bootstrap: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
}))

vi.mock('@egon-cola/admin-web-shared', async () => {
    const actual = await vi.importActual<typeof import('@egon-cola/admin-web-shared')>('@egon-cola/admin-web-shared')
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

describe('IdP authentication hydration', () => {
    it('hydrates authorization from the Gateway cookie bootstrap without browser token state', async () => {
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
