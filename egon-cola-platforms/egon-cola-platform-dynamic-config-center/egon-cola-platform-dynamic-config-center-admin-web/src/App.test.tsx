import {cleanup, screen} from '@testing-library/react'
import type {ReactNode} from 'react'
import {afterEach, describe, expect, it, vi} from 'vitest'
import {renderWithQueryClient} from './test/renderWithQueryClient'
import App from './App'

const auth = vi.hoisted(() => ({
  authorized: false,
  loading: false,
  identity: 'admin',
  error: undefined,
  login: vi.fn(),
  logout: vi.fn(),
}))

vi.mock('./auth/AuthContext', () => ({
  AuthProvider: ({children}: {children: ReactNode}) => children,
  useAuth: () => auth,
}))

describe('App', () => {
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('renders the unified login page when silent refresh is unavailable', async () => {
    auth.authorized = false
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('no session')))
    renderWithQueryClient(<App />)
    expect(await screen.findByText('DDC 管理端')).toBeInTheDocument()
  })

  it('renders configuration client instances at the dedicated route', async () => {
    auth.authorized = true
    window.history.replaceState({}, '', '/instances')
    const jsonResponse = (body: unknown) => new Response(JSON.stringify(body), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    })
    vi.stubGlobal('fetch', vi.fn((input) => {
      const url = String(input)
      if (url.endsWith('/api/v1/ddc/auth/bootstrap')) {
        return Promise.resolve(jsonResponse({
          user: { identitySub: 'admin' },
          permissions: ['DDC_READ'],
        }))
      }
      if (url.includes('/instances/page')) {
        return Promise.resolve(jsonResponse({
          success: true,
          code: 0,
          status: 'SUCCESS',
          message: '',
          records: [],
          page: { total: 0, pageNo: 1, pageSize: 10, pages: 0, hasNext: false, hasPrevious: false },
          traceId: 'trace-instances',
          timestamp: 1,
        }))
      }
      return Promise.resolve(jsonResponse({
        success: true,
        code: 0,
        status: 'SUCCESS',
        message: '',
        data: [],
        traceId: 'trace-scope',
        timestamp: 1,
      }))
    }))

    renderWithQueryClient(<App />)

    expect(await screen.findByText('配置客户端实例')).toBeInTheDocument()
  })
})
