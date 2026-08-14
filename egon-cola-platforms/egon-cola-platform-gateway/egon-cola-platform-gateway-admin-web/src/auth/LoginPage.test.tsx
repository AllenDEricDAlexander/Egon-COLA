import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const auth = vi.hoisted(() => ({
  loading: false,
    authorization: undefined,
  error: undefined,
  login: vi.fn(),
  logout: vi.fn(),
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
    it('uses the Gateway cookie login flow and never renders token inputs', async () => {
    const { LoginPage } = await import('./LoginPage')
    render(<MemoryRouter><LoginPage /></MemoryRouter>)

    expect(screen.queryByLabelText(/Access Token/)).not.toBeInTheDocument()
    expect(screen.queryByLabelText(/Refresh Token/)).not.toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('租户 ID'), {
      target: { value: 'tenant-a' },
    })
        fireEvent.change(screen.getByLabelText('用户名'), {
            target: {value: 'admin'},
        })
        fireEvent.change(screen.getByLabelText('密码'), {
            target: {value: 'secret'},
        })
        fireEvent.click(screen.getByRole('button', {name: /登\s*录/}))

    await waitFor(() => expect(auth.login).toHaveBeenCalledWith(
      'tenant-a',
        'admin',
        'secret',
    ))
  })
})
