import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import LoginPage from '../auth/LoginPage'

const auth = vi.hoisted(() => ({
  loading: false,
  error: undefined,
  login: vi.fn(),
  logout: vi.fn(),
}))

vi.mock('../auth/AuthContext', () => ({ useAuth: () => auth }))

describe('LoginPage', () => {
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
  })

  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('starts OIDC login without tenant, username, password, or token inputs', async () => {
    render(<MemoryRouter><LoginPage /></MemoryRouter>)

    expect(screen.queryByPlaceholderText(/Token/)).not.toBeInTheDocument()
    expect(screen.queryByLabelText('租户 ID')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('用户名')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('密码')).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', {name: /使用统一身份登录/}))

    await waitFor(() => expect(auth.login).toHaveBeenCalledWith())
  })
})
