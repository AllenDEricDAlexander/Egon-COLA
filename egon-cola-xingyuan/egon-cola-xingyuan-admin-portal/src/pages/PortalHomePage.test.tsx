import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeAll, describe, expect, it } from 'vitest'
import {
  AdminThemeProvider,
  I18nProvider,
  initI18n,
} from '@egon-cola/xingyuan-admin-web-shared'
import { PORTAL_PLATFORM_ITEMS, PortalHomePage } from './PortalHomePage'
import type { PlatformKey } from '../manifest/types'
import type { SummaryAdapters } from '../summary/platformSummaryFacade'
import { resolveChildUrl } from '../lifecycle/WujieChild'
import jianshenResources from '../../../egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/app/resourceDefinitions.json'

const renderHome = (
  adapters: SummaryAdapters,
  allowedPlatformKeys?: readonly PlatformKey[],
) => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminThemeProvider>
        <I18nProvider>
          <MemoryRouter>
            <PortalHomePage adapters={adapters} allowedPlatformKeys={allowedPlatformKeys} />
          </MemoryRouter>
        </I18nProvider>
      </AdminThemeProvider>
    </QueryClientProvider>,
  )
}

beforeAll(async () => {
  await initI18n({ lng: 'zh-CN', resources: { 'zh-CN': {} } })
})

describe('PortalHomePage', () => {
  it('renders the static xingyuan menu and keeps partial summary cards visible', async () => {
    renderHome({
        'tianquan-shoubing': async () => ({status: 'READY', summary: '12 个身份'}),
      yuheng: async () => ({ status: 'TIMEOUT' }),
      tianshu: async () => ({ status: 'READY', summary: '8 个配置' }),
    })

    expect(await screen.findByRole('heading', { name: '平台工作台' })).toBeInTheDocument()
    expect(await screen.findByText('身份与安全')).toBeInTheDocument()
    expect(screen.getAllByText('权限治理').length).toBeGreaterThan(0)
    expect(screen.getAllByText('配置中心').length).toBeGreaterThan(0)
    expect(screen.getAllByText(/请求超时/).length).toBeGreaterThan(0)
    expect(screen.getByRole('link', { name: /独立打开 API 网关/ })).toHaveAttribute('href', '/yuheng/dashboard')
  })

  it('hides a xingyuan without the host-level capability', async () => {
    renderHome({}, ['tianquan-shoubing'])

    expect(await screen.findByText('身份与安全')).toBeInTheDocument()
    expect(screen.queryByText('配置中心')).not.toBeInTheDocument()
    expect(screen.queryByText('API 网关')).not.toBeInTheDocument()
  })

  it('opens the declared Jianshen role route through both Portal entries', async () => {
    renderHome({})
    const roleRoute = jianshenResources.find((resource) => resource.code === 'iam.roles')?.path
    expect(roleRoute).toBeDefined()
    const entry = PORTAL_PLATFORM_ITEMS.find((item) => item.key === 'tianquan-jianshen')!

    const standalone = await screen.findByRole('link', { name: '独立打开 权限治理' })
    const card = standalone.closest('.ant-card') as HTMLElement
    expect(within(card).getByRole('link', { name: '进入平台' }))
      .toHaveAttribute('href', `/xingyuan/tianquan-jianshen${roleRoute}`)
    expect(standalone)
      .toHaveAttribute('href', `/tianquan-jianshen${roleRoute}`)
    expect(resolveChildUrl('/apps/tianquan-jianshen/', entry.key, entry.path))
      .toBe(new URL(`/apps/tianquan-jianshen${roleRoute}`, window.location.href).toString())
  })
})
