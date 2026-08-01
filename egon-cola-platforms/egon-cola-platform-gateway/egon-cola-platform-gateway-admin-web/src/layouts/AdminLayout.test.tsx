import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { GatewayScopeBinding, Scope } from '../api/types'
import { AdminLayout } from './AdminLayout'

const defaultScope: Scope = {
  bizCode: 'retail',
  namespace: 'default',
  env: 'local',
  appCode: 'order',
}

const bindings: GatewayScopeBinding[] = [
  {
    ...defaultScope,
    bindingId: 'binding-default',
    appName: 'Order',
    connected: true,
  },
  {
    ...defaultScope,
    namespace: 'ops',
    bindingId: 'binding-ops',
    appName: 'Order',
    connected: true,
  },
]

const mocks = vi.hoisted(() => ({
  changeScope: vi.fn(),
  logout: vi.fn(),
}))

vi.mock('../hooks/useScope', () => ({
  useScope: () => ({
    scope: defaultScope,
    bindings,
    changeScope: mocks.changeScope,
  }),
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

vi.mock('antd', async (importOriginal) => {
  const actual = await importOriginal<typeof import('antd')>()
  return {
    ...actual,
    Select: ({
      'aria-label': ariaLabel,
      onChange,
      options,
      value,
    }: {
      'aria-label': string
      onChange: (value: string) => void
      options: Array<{ label: string, value: string }>
      value: string
    }) => (
      <select
        aria-label={ariaLabel}
        value={value}
        onChange={(event) => onChange(event.target.value)}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    ),
  }
})

const renderLayout = () => {
  const queryClient = new QueryClient()
  queryClient.setQueryData(['gateway-scopes'], bindings)
  queryClient.setQueryData(['applications', defaultScope], [{ id: 'app' }])
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/dashboard']}>
        <Routes>
          <Route path="/" element={<AdminLayout />}>
            <Route path="dashboard" element={<div>Dashboard</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
  return queryClient
}

beforeEach(() => {
  mocks.changeScope.mockReset()
  vi.stubGlobal('confirm', vi.fn(() => true))
  vi.stubGlobal('matchMedia', vi.fn().mockImplementation(() => ({
    matches: false,
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

describe('AdminLayout DDC selectors', () => {
  it('renders biz namespace env app in DDC hierarchy order', () => {
    renderLayout()

    expect(screen.getAllByRole('combobox')
      .map((item) => item.getAttribute('aria-label'))).toEqual([
      '业务域',
      '命名空间',
      '环境',
      '应用',
    ])
    expect(screen.getByLabelText('命名空间')).toHaveTextContent('ops')
    expect(screen.getByLabelText('应用')).not.toHaveTextContent('default-app')
  })

  it('uses the DDC cascade and preserves the scope catalog cache', () => {
    const queryClient = renderLayout()

    fireEvent.change(screen.getByLabelText('命名空间'), {
      target: { value: 'ops' },
    })

    expect(mocks.changeScope).toHaveBeenCalledWith('namespace', 'ops')
    expect(queryClient.getQueryData(['gateway-scopes'])).toEqual(bindings)
    expect(queryClient.getQueryData(['applications', defaultScope]))
      .toBeUndefined()
  })
})
