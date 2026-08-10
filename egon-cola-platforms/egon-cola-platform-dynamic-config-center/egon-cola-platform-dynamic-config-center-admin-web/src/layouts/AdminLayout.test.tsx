import { fireEvent, render, screen } from '@testing-library/react'
import { App as AntdApp } from 'antd'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
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

describe('AdminLayout', () => {
  beforeEach(() => {
    logout.mockClear()
  })

  it('uses a drawer navigation on narrow screens', async () => {
    setViewport(600)
    renderLayout()

    expect(screen.queryByLabelText('桌面主导航')).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '打开导航' }))
    expect(await screen.findByLabelText('移动主导航')).toBeInTheDocument()
    expect(screen.getByText('元数据管理')).toBeInTheDocument()
  })

  it('uses grouped, collapsible navigation on desktop', () => {
    setViewport(1280)
    renderLayout()

    const navigation = screen.getByLabelText('桌面主导航')
    expect(navigation).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '折叠导航' }))
      .toBeInTheDocument()
    expect(screen.getByText('Mario')).toBeInTheDocument()
    expect(screen.getByText('DDC 已连接')).toBeInTheDocument()
    expect(navigation.querySelector('.anticon')).toBeInTheDocument()
  })
})
