import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { CallbackPage } from './CallbackPage'

const callback = vi.hoisted(() => ({
  auth: { loading: false, bootstrap: undefined as object | undefined },
  handle: vi.fn(),
}))

vi.mock('./AuthContext', () => ({
  oauthClient: { handleCallback: callback.handle },
  useAuth: () => callback.auth,
}))

const TestRouter = () => (
  <MemoryRouter initialEntries={['/oauth/callback']}>
    <Routes>
      <Route path="/oauth/callback" element={<CallbackPage />} />
      <Route path="/users" element={<div>用户管理</div>} />
    </Routes>
  </MemoryRouter>
)

beforeEach(() => {
  callback.auth.loading = false
  callback.auth.bootstrap = undefined
  callback.handle.mockReset().mockResolvedValue('/users')
})

afterEach(cleanup)

describe('IdP OAuth callback', () => {
  it('waits for authorization bootstrap before entering a protected route', async () => {
    const view = render(<TestRouter />)

    await waitFor(() => expect(callback.handle).toHaveBeenCalled())
    expect(screen.queryByText('用户管理')).not.toBeInTheDocument()

    callback.auth.bootstrap = {}
    view.rerender(<TestRouter />)

    await waitFor(() => expect(screen.getByText('用户管理')).toBeInTheDocument())
  })
})
