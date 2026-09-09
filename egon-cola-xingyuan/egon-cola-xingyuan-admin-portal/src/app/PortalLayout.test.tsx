import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {fireEvent, render, screen, waitFor, within} from '@testing-library/react'
import {MemoryRouter} from 'react-router-dom'
import {AdminThemeProvider, I18nProvider, initI18n} from '@egon-cola/xingyuan-admin-web-shared'
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest'
import {PortalLayout} from './PortalLayout'
import {PortalRouter} from './router'

const auth = vi.hoisted(() => ({userInfo: vi.fn(), login: vi.fn(), logout: vi.fn()}))
vi.mock('../auth/portalAuth', () => ({portalAuth: auth}))

beforeAll(async () => {await initI18n({lng: 'zh-CN', resources: {'zh-CN': {}}})})
beforeEach(() => {
  auth.userInfo.mockReset().mockResolvedValue(null)
  auth.login.mockReset().mockRejectedValue(new Error('invalid credentials'))
  auth.logout.mockReset().mockRejectedValue(new Error('temporarily unavailable'))
})

const renderPortal = (router = false) => render(
  <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
    <AdminThemeProvider><I18nProvider><MemoryRouter>
      {router ? <PortalRouter /> : <PortalLayout><div>平台内容</div></PortalLayout>}
    </MemoryRouter></I18nProvider></AdminThemeProvider>
  </QueryClientProvider>,
)

describe('Portal shell', () => {
  it('offers Gateway login in the host header and keeps the content frameless', async () => {
    renderPortal()
    await screen.findByText('登录')
    expect(screen.getByRole('img', {name: 'Egon COLA Platform'})).toHaveAttribute('src', '/favicon.png')
    expect(screen.queryByRole('contentinfo')).not.toBeInTheDocument()
    expect(screen.getByText('平台内容').closest('.ant-layout-content')).toHaveStyle({padding: '0px'})
    fireEvent.click(screen.getByRole('button', {name: /用户菜单|User Menu/}))
    fireEvent.click(await screen.findByRole('menuitem', {name: /登录/}))
    const modal = await screen.findByRole('dialog', {name: '登录 Egon COLA Platform'})
    fireEvent.change(within(modal).getByLabelText('租户 ID'), {target: {value: '123'}})
    fireEvent.change(within(modal).getByLabelText('用户名'), {target: {value: 'alice'}})
    fireEvent.change(within(modal).getByLabelText('密码'), {target: {value: 'example-password'}})
    fireEvent.click(within(modal).getByRole('button', {name: /登\s*录/}))
    await waitFor(() => expect(auth.login).toHaveBeenCalledWith({
      tenantId: '123', username: 'alice', password: 'example-password',
    }))
    expect(await screen.findByText('登录失败，请检查租户和账号信息后重试')).toBeInTheDocument()
  })

  it('shows the authenticated subject and provides logout without passing credentials to children', async () => {
    auth.userInfo.mockResolvedValue({sub: '42', tid: '123'})
    renderPortal()
    await screen.findByText('42')
    fireEvent.click(screen.getByRole('button', {name: /用户菜单|User Menu/}))
    fireEvent.click(await screen.findByRole('menuitem', {name: /退出登录/}))
    await waitFor(() => expect(auth.logout).toHaveBeenCalledOnce())
    expect(await screen.findByText('退出登录失败，请重试')).toBeInTheDocument()
  })

  it('opens a child platform directly instead of the introduction dashboard', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({
      key: 'idp', displayName: '身份与安全', url: '/children/idp/', standaloneUrl: '/idp/overview',
      version: '5.3.2', contractVersion: 'platform-1', compatibleHostRange: '>=5.3.2 <6.0.0',
      requiredCapabilities: [],
    }), {status: 200})))
    renderPortal(true)
    expect(await screen.findByTestId('wujie-host-idp')).toBeInTheDocument()
    expect(screen.queryByRole('heading', {name: '平台工作台'})).not.toBeInTheDocument()
    vi.unstubAllGlobals()
  })
})
