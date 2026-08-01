import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const auth = vi.hoisted(() => ({
  loading: false,
  session: undefined,
  login: vi.fn(),
  logout: vi.fn(),
  refreshSession: vi.fn(),
}))

vi.mock('./AuthContext', () => ({
  useAuth: () => auth,
}))

const renderLogin = async () => {
  const { LoginPage } = await import('./LoginPage')
  return render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  )
}

beforeEach(() => {
  vi.resetModules()
  vi.stubGlobal('matchMedia', vi.fn().mockImplementation(() => ({
    matches: false,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })))
  vi.stubGlobal('ResizeObserver', class {
    observe() {}
    unobserve() {}
    disconnect() {}
  })
  auth.login.mockReset()
})

afterEach(() => {
  cleanup()
  vi.unstubAllEnvs()
  vi.unstubAllGlobals()
})

describe('gateway admin login', () => {
  it('requires only an access token in local authentication mode', async () => {
    vi.stubEnv('VITE_GATEWAY_ADMIN_TOKEN_URL', '')
    vi.stubEnv('VITE_GATEWAY_ADMIN_CLIENT_ID', '')
    await renderLogin()

    expect(screen.getByText(/本地模式仅需 Access Token/)).toBeInTheDocument()
    expect(screen.queryByLabelText(/Refresh Token/)).not.toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Access Token'), {
      target: { value: 'local-access-token' },
    })
    fireEvent.click(screen.getByRole('button', { name: '登录并校验权限' }))

    await waitFor(() => {
      expect(auth.login).toHaveBeenCalledWith({
        accessToken: 'local-access-token',
      }, undefined)
    })
  })

  it('shows the refresh token field when OAuth refresh is configured', async () => {
    vi.stubEnv('VITE_GATEWAY_ADMIN_TOKEN_URL', 'https://identity.example/token')
    vi.stubEnv('VITE_GATEWAY_ADMIN_CLIENT_ID', 'gateway-admin')
    await renderLogin()

    expect(screen.getByLabelText(/Refresh Token/)).toBeInTheDocument()
    expect(screen.queryByText(/本地模式仅需 Access Token/)).not.toBeInTheDocument()
  })
})
