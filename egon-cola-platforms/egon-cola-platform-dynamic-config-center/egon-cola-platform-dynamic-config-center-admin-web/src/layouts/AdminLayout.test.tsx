import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { App as AntdApp } from 'antd'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import AdminLayout from './AdminLayout'

const logout = vi.fn()

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

const renderLayout = () => render(
  <AntdApp>
    <MemoryRouter initialEntries={['/registry']}>
      <Routes>
        <Route path="/" element={<AdminLayout />}>
          <Route path="registry" element={<div>注册页内容</div>} />
        </Route>
      </Routes>
    </MemoryRouter>
  </AntdApp>,
)

beforeEach(() => {
  logout.mockClear()
})

afterEach(() => {
  cleanup()
})

describe('AdminLayout', () => {
  it('renders the unified header, navigation, user and footer on desktop', () => {
    setViewport(1280)
    renderLayout()

    expect(screen.getByText('DDC Admin')).toBeInTheDocument()
    expect(screen.getByText('服务注册')).toBeInTheDocument()
    expect(screen.getByText('DDC 已连接')).toBeInTheDocument()
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

  it('offers logout from the user dropdown', async () => {
    setViewport(1280)
    renderLayout()

    fireEvent.click(screen.getByRole('button', { name: '用户菜单' }))
    fireEvent.click(await screen.findByText('退出登录'))
    expect(logout).toHaveBeenCalled()
  })

  it('uses a grouped drawer navigation on narrow screens', async () => {
    setViewport(600)
    renderLayout()

    expect(screen.queryByRole('menu', { name: '主导航' })).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '打开导航' }))
    expect(await screen.findByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText('元数据管理')).toBeInTheDocument()
  })
})
