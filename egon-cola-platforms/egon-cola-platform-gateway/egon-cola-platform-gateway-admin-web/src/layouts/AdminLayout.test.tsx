import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {cleanup, render, screen} from '@testing-library/react'
import {MemoryRouter, Route, Routes} from 'react-router-dom'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {AdminLayout} from './AdminLayout'

const mocks = vi.hoisted(() => ({
  logout: vi.fn(),
  mcp: true,
}))

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({
      authorization: {user: {identitySub: 'Admin'}},
    logout: mocks.logout,
  }),
}))

vi.mock('../app/capabilities', () => ({
  useCapability: (capability: string) => capability === 'gateway:mcp:read' ? mocks.mcp : true,
}))

const renderLayout = (path = '/dashboard') => render(
  <QueryClientProvider client={new QueryClient()}>
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/" element={<AdminLayout />}>
          <Route path="dashboard" element={<div>Dashboard</div>} />
          <Route path="*" element={<div>Detail</div>} />
        </Route>
      </Routes>
    </MemoryRouter>
  </QueryClientProvider>,
)

beforeEach(() => {
  mocks.logout.mockReset().mockResolvedValue(undefined)
  mocks.mcp = true
  vi.stubGlobal('matchMedia', vi.fn().mockImplementation(() => ({
    matches: true,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })))
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('AdminLayout', () => {
  it('keeps the shared shell while removing global scope selectors', () => {
    renderLayout()
    expect(screen.getByText('Gateway Admin')).toBeInTheDocument()
    expect(screen.getByText('总览')).toBeInTheDocument()
    expect(screen.getByRole('navigation', {name: '主菜单'})).toBeInTheDocument()
    expect(screen.queryByRole('menu', {name: '主导航'})).not.toBeInTheDocument()
    expect(screen.getByText('Admin API')).toBeInTheDocument()
    expect(screen.getByText('Admin')).toBeInTheDocument()
    expect(screen.getByText('版本 v5.2.3')).toBeInTheDocument()
    expect(screen.getByText('Dashboard')).toBeInTheDocument()
    expect(screen.queryByLabelText('业务域')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('命名空间')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('环境')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('应用')).not.toBeInTheDocument()
  })

  it('prunes the MCP tree when the capability is absent', () => {
    mocks.mcp = false
    renderLayout()
    expect(screen.queryByText('MCP')).not.toBeInTheDocument()
    expect(screen.getByText('网关治理')).toBeInTheDocument()
  })

  it('selects the interface catalog for an operation detail path', () => {
    renderLayout('/operations/op-1')
    expect(screen.getByText('接口目录').closest('.ant-menu-item')).toHaveClass('ant-menu-item-selected')
  })
})
