import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AdminLayout } from './AdminLayout'

const mocks = vi.hoisted(() => ({
  logout: vi.fn(),
}))

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({
    session: { displayName: 'Admin' },
    logout: mocks.logout,
  }),
}))

vi.mock('../app/capabilities', () => ({
  useCapability: () => true,
}))

const renderLayout = () => render(
  <QueryClientProvider client={new QueryClient()}>
    <MemoryRouter initialEntries={['/dashboard']}>
      <Routes>
        <Route path="/" element={<AdminLayout />}>
          <Route path="dashboard" element={<div>Dashboard</div>} />
        </Route>
      </Routes>
    </MemoryRouter>
  </QueryClientProvider>,
)

beforeEach(() => {
  mocks.logout.mockReset().mockResolvedValue(undefined)
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
    expect(screen.getByText('Admin API')).toBeInTheDocument()
    expect(screen.getByText('Admin')).toBeInTheDocument()
    expect(screen.getByText('版本 v5.2.3')).toBeInTheDocument()
    expect(screen.getByText('Dashboard')).toBeInTheDocument()
    expect(screen.queryByLabelText('业务域')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('命名空间')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('环境')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('应用')).not.toBeInTheDocument()
  })
})
