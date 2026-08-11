import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { StrictMode } from 'react'
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'

const auth = vi.hoisted(() => {
  type Tokens = { accessToken: string }
  const listeners = new Set<(tokens: Tokens | null) => void>()
  let tokens: Tokens | null = null
  const tokenStore = {
    get: () => tokens,
    set: (value: Tokens) => {
      tokens = value
      listeners.forEach((listener) => listener(tokens))
    },
    clear: () => {
      tokens = null
      listeners.forEach((listener) => listener(null))
    },
    subscribe: (listener: (value: Tokens | null) => void) => {
      listeners.add(listener)
      return () => { listeners.delete(listener) }
    },
  }
  return {
    beginAuthorization: vi.fn(),
    refresh: vi.fn(),
    revoke: vi.fn(),
    request: vi.fn(),
    reset: () => {
      tokens = null
      listeners.clear()
    },
    tokenStore,
  }
})

vi.mock('@egon-cola/admin-web-shared', () => ({
  createTokenStore: () => auth.tokenStore,
  createOAuthClient: () => ({
    beginAuthorization: auth.beginAuthorization,
    refresh: auth.refresh,
    revoke: auth.revoke,
  }),
  createHttpClient: () => ({ request: auth.request }),
}))

let AuthProvider: typeof import('./AuthContext').AuthProvider
let useAuth: typeof import('./AuthContext').useAuth

const AuthState = () => {
  const state = useAuth()
  return <div>{state.loading ? 'loading' : state.bootstrap?.identitySub ?? 'logged-out'}</div>
}

beforeAll(async () => {
  vi.stubEnv('VITE_IDP_ISSUER', 'http://127.0.0.1:18120')
  vi.stubEnv('VITE_IDP_CLIENT_ID', 'idp-admin-web')
  vi.stubEnv('VITE_IDP_RESOURCE', 'https://api.egon.internal/local/permission/idp')
  ;({ AuthProvider, useAuth } = await import('./AuthContext'))
})

beforeEach(() => {
  window.history.replaceState({}, '', '/overview')
  auth.reset()
  auth.beginAuthorization.mockReset()
  auth.refresh.mockReset().mockImplementation(async () => {
    auth.tokenStore.set({ accessToken: 'restored-jwt' })
    return 'restored-jwt'
  })
  auth.revoke.mockReset()
  auth.request.mockReset().mockResolvedValue({ identitySub: 'alice' })
})

afterEach(cleanup)
afterAll(() => vi.unstubAllEnvs())

describe('IdP authentication hydration', () => {
  it('restores the in-memory JWT from the refresh cookie after a page reload', async () => {
    render(<StrictMode><AuthProvider><AuthState /></AuthProvider></StrictMode>)

    await waitFor(() => expect(screen.getByText('alice')).toBeInTheDocument())

    expect(auth.refresh).toHaveBeenCalledTimes(1)
    expect(auth.request).toHaveBeenCalledWith('/api/v1/auth/bootstrap')
  })

  it('finishes logged out when the refresh cookie is unavailable', async () => {
    auth.refresh.mockRejectedValue(new Error('invalid_grant'))

    render(<StrictMode><AuthProvider><AuthState /></AuthProvider></StrictMode>)

    await waitFor(() => expect(screen.getByText('logged-out')).toBeInTheDocument())
    expect(auth.refresh).toHaveBeenCalledTimes(1)
    expect(auth.request).not.toHaveBeenCalled()
  })
})
