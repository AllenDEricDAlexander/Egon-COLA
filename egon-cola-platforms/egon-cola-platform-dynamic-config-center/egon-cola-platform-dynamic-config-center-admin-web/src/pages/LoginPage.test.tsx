import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../api/client'
import { clearToken } from '../auth/tokenStore'
import LoginPage from '../auth/LoginPage'
import { AuthProvider } from '../auth/AuthContext'
import type { ReactNode } from 'react'

const record = (data: unknown) => ({
  success: true, code: 0, status: 'SUCCESS', message: '', data, traceId: 't', timestamp: 1,
})

const jsonResponse = (body: unknown) =>
  new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })

const Harness = ({ children }: { children: ReactNode }) => (
  <AuthProvider>{children}</AuthProvider>
)

describe('LoginPage', () => {
  beforeEach(() => {
    clearToken()
    setDdcTokenProvider(() => '')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('sends the candidate token on the validation request and persists after success', async () => {
    vi.mocked(fetch).mockImplementation((input, init) => {
      const url = String(input)
      if (url.includes('/apps')) {
        const auth = (init?.headers as Headers | undefined)?.get('Authorization') ?? ''
        expect(auth).toBe('Bearer candidate-token')
        return Promise.resolve(jsonResponse(record([])))
      }
      return Promise.resolve(jsonResponse(record(null)))
    })

    render(
      <Harness>
        <LoginPage />
      </Harness>,
    )
    fireEvent.change(screen.getByPlaceholderText('粘贴 admin.token 内容'), {
      target: { value: 'candidate-token' },
    })
    fireEvent.click(screen.getByRole('button', { name: /登\s*录\s*并\s*加\s*载/ }))

    await waitFor(() => {
      expect(sessionStorage.getItem('egon.ddc.admin.token')).toBe('candidate-token')
    })
  })

  it('does not persist an invalid candidate when validation fails', async () => {
    vi.mocked(fetch).mockResolvedValue(
      new Response(JSON.stringify({ success: false, code: 401, status: 'UNAUTHORIZED', message: 'bad', data: null, traceId: 't', timestamp: 1 }), {
        status: 401,
        headers: { 'Content-Type': 'application/json' },
      }),
    )

    render(
      <Harness>
        <LoginPage />
      </Harness>,
    )
    fireEvent.change(screen.getByPlaceholderText('粘贴 admin.token 内容'), {
      target: { value: 'bad-token' },
    })
    fireEvent.click(screen.getByRole('button', { name: /登\s*录\s*并\s*加\s*载/ }))

    await waitFor(() => {
      expect(sessionStorage.getItem('egon.ddc.admin.token')).toBeNull()
    })
  })
})
