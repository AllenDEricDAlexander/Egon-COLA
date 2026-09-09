import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, useLocation } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { UserListPage } from './UserListPage'

const state = vi.hoisted(() => ({
  permissions: [] as string[],
  request: vi.fn(),
}))

vi.mock('../../auth/AuthContext', () => ({
  useAuth: () => ({
    loading: false,
    bootstrap: {
      user: { id: 'admin-1', identitySub: 'admin-sub', tenantId: 'default', status: 'ACTIVE' },
      activeRoleContexts: [],
      permissions: state.permissions,
      apps: [], menus: [], routes: [], actions: [], fieldPolicies: {},
      defaultApplicationCode: null, defaultRoute: null, authVersion: 1, policyVersion: 1,
    },
  }),
  httpClient: { request: state.request },
}))

const user = {
  subject: 'alice-sub',
  username: 'alice',
  displayName: 'Alice',
  status: 'ACTIVE',
  failedLoginCount: 0,
  version: 1,
  lastLoginAt: '2026-08-27T01:00:00Z',
}

const renderPage = (initialEntry = '/users?page=2&size=20') => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <UserListPage />
        <LocationProbe />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

const LocationProbe = () => <output data-testid="location-search">{useLocation().search}</output>

beforeEach(() => {
  state.permissions = ['tianquan-shoubing:identity-user:read', 'tianquan-shoubing:identity-user:create']
  state.request.mockReset().mockImplementation((path: string, options?: RequestInit) => {
    if (path.includes('/api/v1/tianquan-shoubing/users?')) {
      return Promise.resolve({ content: [user], page: 2, size: 20, totalElements: 41, totalPages: 3 })
    }
    if (path === '/api/v1/tianquan-shoubing/users' && options?.method === 'POST') {
      return Promise.resolve({ ...user, subject: 'new-sub', oneTimePassword: 'one-time-password' })
    }
    if (path === '/api/v1/tianquan-shoubing/users') return Promise.resolve([user])
    return Promise.reject(new Error(`Unexpected request: ${path}`))
  })
})

afterEach(cleanup)

describe('Identity user administration', () => {
  it('renders server total and resets page when the filter changes', async () => {
    renderPage()

    expect(await screen.findByText('alice')).toBeInTheDocument()
    expect(screen.getByText('共 41 条')).toBeInTheDocument()
    fireEvent.change(screen.getByRole('textbox', { name: '用户' }), { target: { value: 'alice' } })
    fireEvent.click(screen.getByRole('button', { name: /查\s*询/ }))

    await waitFor(() => expect(screen.getByTestId('location-search')).toHaveTextContent('page=0'))
    expect(state.request).toHaveBeenCalledWith(expect.stringContaining('query=alice'))
  })

  it('keeps one-time password out of the URL', async () => {
    renderPage('/users')

    await screen.findByText('alice')
    fireEvent.click(screen.getByRole('button', { name: /创建用户/ }))
    fireEvent.change(screen.getByLabelText('用户名'), { target: { value: 'new-user' } })
    fireEvent.change(screen.getByLabelText('显示名'), { target: { value: 'New User' } })
    fireEvent.click(screen.getAllByRole('button', { name: /确定|OK/ }).at(-1)!)

    await waitFor(() => expect(screen.getByText('one-time-password')).toBeInTheDocument())
    expect(window.location.href).not.toContain('one-time-password')
  })
})
