import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { App as AntdApp } from 'antd'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import AdminLayout from './AdminLayout'

const logout = vi.fn()

type WujieTestWindow = Window & { $wujie?: { props?: { embedded?: boolean } } }

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({
    token: 'token',
    identity: 'Mario',
    loading: false,
    login: vi.fn(),
    logout,
  }),
}))

const setViewport = (width: number) => {
  window.matchMedia = vi.fn().mockImplementation((query: string) => {
    const minWidth = Number(query.match(/min-width:\s*(\d+)px/)?.[1] ?? 0)
    return {
      matches: minWidth > 0 && width >= minWidth,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    }
  })
}

const renderLayout = (path = '/registry', embedded = false) => {
  ;(window as WujieTestWindow).$wujie = embedded ? {props: {embedded: true}} : undefined
  return render(
    <AntdApp>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/" element={<AdminLayout />}>
            <Route path="registry" element={<div>注册页内容</div>} />
            <Route path="instances" element={<div>实例页内容</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    </AntdApp>,
  )
}

beforeEach(() => {
  logout.mockClear()
})

afterEach(() => {
  cleanup()
  delete (window as WujieTestWindow).$wujie
})

describe('AdminLayout', () => {
  it('keeps Tianshu runtime and metadata navigation when embedded', () => {
    setViewport(1280)
    renderLayout('/registry', true)

    expect(screen.getByText('运行状态')).toBeInTheDocument()
    expect(screen.getByText('元数据管理')).toBeInTheDocument()
    expect(screen.queryByText('Tianshu Admin')).not.toBeInTheDocument()
  })

  it('renders the unified Banner and grouped left navigation on desktop', () => {
    setViewport(1280)
    renderLayout()

    expect(screen.getByText('Tianshu Admin')).toBeInTheDocument()
    expect(screen.getByRole('navigation', {name: '主菜单'})).toBeInTheDocument()
    expect(screen.queryByRole('menu', {name: '主导航'})).not.toBeInTheDocument()
    expect(screen.getByText('服务注册')).toBeInTheDocument()
    expect(screen.getByText('Tianshu 已连接')).toBeInTheDocument()
    expect(screen.getByText('Mario')).toBeInTheDocument()
    expect(screen.getByText('注册页内容')).toBeInTheDocument()
    expect(screen.getByText('版本 v5.3.2')).toBeInTheDocument()
  })

  it('highlights the navigation item matching the current route', () => {
    setViewport(1280)
    renderLayout()

    expect(screen.getByText('服务注册').closest('.ant-menu-item'))
      .toHaveClass('ant-menu-item-selected')
  })

  it('shows configuration client instances under runtime and highlights it', () => {
    setViewport(1280)
    renderLayout('/instances')

    expect(screen.getByText('配置客户端实例')).toBeInTheDocument()
    expect(screen.getByText('配置客户端实例').closest('.ant-menu-item'))
      .toHaveClass('ant-menu-item-selected')
  })

  it('offers logout from the user dropdown', async () => {
    setViewport(1280)
    renderLayout()

    fireEvent.click(screen.getByRole('button', { name: '用户菜单' }))
    fireEvent.click(await screen.findByText('退出登录'))
    expect(logout).toHaveBeenCalled()
  })

  it('uses the same tree in a left drawer on narrow screens', async () => {
    setViewport(600)
    renderLayout()

    expect(screen.queryByRole('navigation', {name: '主菜单'})).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '打开导航' }))
    expect(await screen.findByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText('元数据管理')).toBeInTheDocument()
  })
})
