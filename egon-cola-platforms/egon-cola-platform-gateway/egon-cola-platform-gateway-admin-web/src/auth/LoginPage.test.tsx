import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const auth = vi.hoisted(() => ({
  loading: false,
  session: undefined,
  error: undefined,
  login: vi.fn(),
  logout: vi.fn(),
  refreshSession: vi.fn(),
}))

vi.mock('./AuthContext', () => ({ useAuth: () => auth }))

beforeEach(() => {
  auth.login.mockReset()
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
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('gateway admin login', () => {
  it('offers only unified SSO and never renders token inputs', async () => {
    const { LoginPage } = await import('./LoginPage')
    render(<MemoryRouter><LoginPage /></MemoryRouter>)

    expect(screen.queryByLabelText(/Access Token/)).not.toBeInTheDocument()
    expect(screen.queryByLabelText(/Refresh Token/)).not.toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('租户 ID'), {
      target: { value: 'tenant-a' },
    })
    fireEvent.click(screen.getByRole('button', { name: '使用统一身份登录' }))

    await waitFor(() => expect(auth.login).toHaveBeenCalledWith(
      'tenant-a',
      '/dashboard',
    ))
  })
})
