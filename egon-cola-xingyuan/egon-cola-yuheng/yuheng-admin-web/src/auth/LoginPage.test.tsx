import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const auth = vi.hoisted(() => ({
  loading: false,
    authorization: undefined as unknown,
  error: undefined,
  login: vi.fn(),
  logout: vi.fn(),
}))

vi.mock('./AuthContext', () => ({ useAuth: () => auth }))

beforeEach(() => {
  auth.authorization = undefined
  auth.loading = false
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

describe('yuheng admin login', () => {
  it.each([
    ['/catalog', '接口目录'],
    [undefined, '运行总览'],
    ['/login', '运行总览'],
    ['//example.invalid', '运行总览'],
    ['https://example.invalid', '运行总览'],
  ])('redirects an authenticated user from %s to a local page', async (from, expected) => {
    auth.authorization = {user: {id: 'admin'}}
    const {LoginPage} = await import('./LoginPage')
    render(
      <MemoryRouter initialEntries={[{pathname: '/login', state: {from}}]}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/dashboard" element={<div>运行总览</div>} />
          <Route path="/catalog" element={<div>接口目录</div>} />
        </Routes>
      </MemoryRouter>,
    )
    expect(await screen.findByText(expected!)).toBeInTheDocument()
  })
  it('starts passwordless OIDC login and never renders tenant, credential or token inputs', async () => {
    const { LoginPage } = await import('./LoginPage')
    render(<MemoryRouter><LoginPage /></MemoryRouter>)

    expect(screen.queryByLabelText(/租户 ID|用户名|密码/)).not.toBeInTheDocument()
    expect(screen.queryByLabelText(/Access Token/)).not.toBeInTheDocument()
    expect(screen.queryByLabelText(/Refresh Token/)).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', {name: '使用统一身份登录'}))

    await waitFor(() => expect(auth.login).toHaveBeenCalledOnce())
  })
})


describe('fixed deployment registration', () => {
  it('passes the configured alias to the installed client without tenant or issuer selectors', async () => {
    const navigate = vi.fn()
    vi.stubEnv('VITE_OAUTH_REGISTRATION_ID', 'edge-deployment-child')
    vi.stubEnv('VITE_YUHENG_ORIGIN', '')
    vi.resetModules()
    vi.doUnmock('./AuthContext')
    vi.doMock('@egon-cola/xingyuan-admin-web-shared', async () => {
      const actual = await vi.importActual<typeof import('@egon-cola/xingyuan-admin-web-shared')>('@egon-cola/xingyuan-admin-web-shared')
      return {...actual, createGatewayAuthClient: (options: Parameters<typeof actual.createGatewayAuthClient>[0]) =>
        actual.createGatewayAuthClient({...options, navigate})}
    })
    try {
      const {gatewayAuth: configured} = await import('./AuthContext')
      configured.login()
      configured.stepUp()
      expect(navigate.mock.calls[0]?.[0]).toBe('/oauth2/authorization/edge-deployment-child')
      expect(String(navigate.mock.calls[1]?.[0]).split('?')[0]).toBe('/oauth2/authorization/edge-deployment-child')
      expect(String(navigate.mock.calls[1]?.[0])).not.toMatch(/tenant|issuer|resource/)
    } finally {
      vi.doUnmock('@egon-cola/xingyuan-admin-web-shared')
      vi.unstubAllEnvs()
    }
  })
})


describe('Yuheng authorization lifetime', () => {
  it('does not restore authorization when a bootstrap completes after local logout', async () => {
    vi.resetModules()
    vi.doUnmock('./AuthContext')
    const {AuthProvider, gatewayAuth, useAuth} = await import('./AuthContext')
    let complete: ((value: unknown) => void) | undefined
    vi.spyOn(gatewayAuth, 'bootstrap').mockImplementation(() => new Promise((resolve) => { complete = resolve }))
    vi.spyOn(gatewayAuth, 'logout').mockResolvedValue({remoteState: 'PENDING'})
    const Probe = () => {
      const auth = useAuth()
      return <div><span data-testid="authorization">{auth.authorization ? 'authorized' : 'empty'}</span>
        <button onClick={() => void auth.logout()}>local-logout</button></div>
    }
    const rendered = render(<AuthProvider><Probe /></AuthProvider>)
    await waitFor(() => expect(complete).toBeTypeOf('function'))
    fireEvent.click(screen.getByText('local-logout'))
    await waitFor(() => expect(gatewayAuth.logout).toHaveBeenCalledOnce())
    await act(async () => { complete?.({user: {id: 'late-user', tenantId: '2002'}, permissions: ['OPS']}) })
    expect(screen.getByTestId('authorization')).toHaveTextContent('empty')
    rendered.unmount()
  })
})


describe('Yuheng provider-owned recovery', () => {
  it('clears authorization on a verified 401 and refuses a late explicit refresh', async () => {
    vi.resetModules()
    vi.doUnmock('./AuthContext')
    const {AuthProvider, gatewayAuth, httpClient, useAuth} = await import('./AuthContext')
    const bootstrap = vi.spyOn(gatewayAuth, 'bootstrap').mockResolvedValue({user: {id: 'user-A'}, permissions: ['OPS']})
    let complete: ((value: unknown) => void) | undefined
    const Probe = () => {
      const auth = useAuth()
      return <div><span data-testid="current-permissions">{auth.authorization ? 'authorized' : 'empty'}</span>
        <button onClick={() => void auth.refreshAuthorization()}>refresh-authorization</button></div>
    }
    const rendered = render(<AuthProvider><Probe /></AuthProvider>)
    await waitFor(() => expect(screen.getByTestId('current-permissions')).toHaveTextContent('authorized'))
    bootstrap.mockImplementation(() => new Promise((resolve) => { complete = resolve }))
    fireEvent.click(screen.getByText('refresh-authorization'))
    await waitFor(() => expect(complete).toBeTypeOf('function'))
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{}', {status: 401})))
    await act(async () => { await expect(httpClient.request('/api/v1/protected')).rejects.toBeDefined() })
    expect(screen.getByTestId('current-permissions')).toHaveTextContent('empty')
    await act(async () => { complete?.({user: {id: 'late-A'}, permissions: ['OPS']}) })
    expect(screen.getByTestId('current-permissions')).toHaveTextContent('empty')
    rendered.unmount()
  })
})
