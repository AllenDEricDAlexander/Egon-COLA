import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Outlet, Route, Routes } from 'react-router-dom'
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { initI18n } from '../i18n'
import { EnterpriseLayout } from './EnterpriseLayout'
import type { EnterpriseLayoutConfig } from './types'

const config: EnterpriseLayoutConfig = {
  platformName: 'DDC Admin',
  navigation: [
    { key: 'registry', label: '服务注册', path: '/registry', group: '运行状态' },
    { key: 'configs', label: '配置资源', path: '/configs', group: '配置管理' },
  ],
  user: { name: 'Mario', menu: [{ key: 'logout', label: '退出登录' }] },
  footer: { version: '5.3.2' },
}

const setViewport = (wide: boolean) => {
  vi.stubGlobal('matchMedia', (query: string) => ({
    matches: wide,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  }))
}

const renderLayout = (initialPath = '/registry') => render(
  <MemoryRouter initialEntries={[initialPath]}>
    <Routes>
      <Route
        path="/"
        element={(
          <EnterpriseLayout config={config}>
            <Outlet />
          </EnterpriseLayout>
        )}
      >
        <Route path="registry" element={<div>注册页内容</div>} />
        <Route path="configs" element={<div>配置页内容</div>} />
      </Route>
    </Routes>
  </MemoryRouter>,
)

beforeAll(async () => {
  await initI18n({ lng: 'zh-CN', resources: { 'zh-CN': {} } })
})

beforeEach(() => {
  setViewport(true)
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('EnterpriseLayout', () => {
  it('renders platform name, navigation, user and footer on desktop', () => {
    renderLayout()

    expect(screen.getByText('DDC Admin')).toBeInTheDocument()
    expect(screen.getByRole('menu', { name: '主导航' })).toBeInTheDocument()
    expect(screen.getByText('服务注册')).toBeInTheDocument()
    expect(screen.getByText('配置资源')).toBeInTheDocument()
    expect(screen.getByText('Mario')).toBeInTheDocument()
    expect(screen.getByText('版本 v5.3.2')).toBeInTheDocument()
    expect(screen.getByText(`© ${new Date().getFullYear()} Egon COLA · DDC Admin`))
      .toBeInTheDocument()
  })

  it('highlights the navigation item matching the current route', () => {
    const { unmount } = renderLayout('/configs')

    const selected = screen.getByText('配置资源').closest('.ant-menu-item')
    expect(selected).toHaveClass('ant-menu-item-selected')
    unmount()

    renderLayout('/registry')
    expect(screen.getByText('服务注册').closest('.ant-menu-item'))
      .toHaveClass('ant-menu-item-selected')
  })

  it('navigates to the target route when a navigation item is clicked', async () => {
    renderLayout()

    fireEvent.click(screen.getByText('配置资源'))
    expect(await screen.findByText('配置页内容')).toBeInTheDocument()
  })

  it('collapses navigation into a grouped drawer on narrow screens', async () => {
    setViewport(false)
    renderLayout()

    expect(screen.queryByRole('menu', { name: '主导航' })).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '打开导航' }))

    const drawer = await screen.findByRole('dialog')
    expect(drawer).toBeInTheDocument()
    expect(screen.getByText('运行状态')).toBeInTheDocument()
    expect(screen.getByText('配置管理')).toBeInTheDocument()
  })
})
