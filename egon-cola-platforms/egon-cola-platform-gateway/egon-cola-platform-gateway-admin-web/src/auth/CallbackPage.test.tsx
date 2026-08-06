import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { CallbackPage } from './CallbackPage'

const callback = vi.hoisted(() => ({
  auth: {
    loading: false,
    session: undefined as object | undefined,
    error: undefined as string | undefined,
  },
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
      <Route path="/dashboard" element={<div>Gateway 控制台</div>} />
    </Routes>
  </MemoryRouter>
)

beforeEach(() => {
  callback.auth.loading = false
  callback.auth.session = undefined
  callback.auth.error = undefined
  callback.handle.mockReset().mockResolvedValue('/dashboard')
})

afterEach(cleanup)

describe('Gateway OAuth callback', () => {
  it('exchanges the authorization code and waits for the Admin session', async () => {
    const view = render(<TestRouter />)

    await waitFor(() => expect(callback.handle).toHaveBeenCalled())
    expect(screen.queryByText('Gateway 控制台')).not.toBeInTheDocument()

    callback.auth.session = {}
    view.rerender(<TestRouter />)

    await waitFor(() => expect(screen.getByText('Gateway 控制台')).toBeInTheDocument())
  })
})
