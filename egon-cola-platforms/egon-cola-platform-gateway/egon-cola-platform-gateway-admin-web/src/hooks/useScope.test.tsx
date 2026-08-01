import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { GatewayScopeBinding } from '../api/types'
import { ScopeProvider, useScope } from './useScope'

const mocks = vi.hoisted(() => ({
  scopes: vi.fn(),
  session: {
    actorId: 'admin',
    displayName: 'Admin',
    actorType: 'USER',
    capabilities: ['gateway:read'],
    roles: ['gateway-admin'],
  },
}))

vi.mock('../api/gatewayApi', () => ({
  gatewayApi: { scopes: mocks.scopes },
}))

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ session: mocks.session }),
}))

const scopeBinding = (
  namespace: string,
  connected: boolean,
): GatewayScopeBinding => ({
  bindingId: `binding-${namespace}`,
  bizCode: 'retail',
  namespace,
  env: 'local',
  appCode: 'order',
  appName: 'Order',
  connected,
})

const ScopeConsumer = () => {
  const { scope, changeScope } = useScope()
  return (
    <div data-testid="scoped-child">
      <span>
        {scope.bizCode}/{scope.namespace}/{scope.env}/{scope.appCode}
      </span>
      <button
        type="button"
        onClick={() => changeScope('namespace', 'ops')}
      >
        switch
      </button>
    </div>
  )
}

const renderScopeConsumer = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <ScopeProvider>
        <ScopeConsumer />
      </ScopeProvider>
    </QueryClientProvider>,
  )
}

beforeEach(() => {
  localStorage.clear()
  mocks.scopes.mockReset()
})

afterEach(() => {
  cleanup()
})

describe('ScopeProvider', () => {
  it('loads scopes after login and exposes the first connected binding', async () => {
    mocks.scopes.mockResolvedValue([
      scopeBinding('ops', false),
      scopeBinding('default', true),
    ])

    renderScopeConsumer()

    expect(await screen.findByText('retail/default/local/order'))
      .toBeInTheDocument()
  })

  it('persists only a valid cascaded scope', async () => {
    mocks.scopes.mockResolvedValue([
      scopeBinding('default', true),
      scopeBinding('ops', true),
    ])
    renderScopeConsumer()
    await screen.findByText('retail/default/local/order')

    fireEvent.click(screen.getByRole('button', { name: 'switch' }))

    expect(await screen.findByText('retail/ops/local/order'))
      .toBeInTheDocument()
    await waitFor(() => expect(JSON.parse(
      localStorage.getItem('egon.gateway.admin.scope.v1') ?? '{}',
    )).toEqual({
      bizCode: 'retail',
      namespace: 'ops',
      env: 'local',
      appCode: 'order',
    }))
  })

  it('does not render scoped children when no DDC binding exists', async () => {
    mocks.scopes.mockResolvedValue([])
    renderScopeConsumer()

    expect(await screen.findByText(
      'DDC 暂无已启用的 namespace-env-app 绑定',
    )).toBeInTheDocument()
    expect(screen.queryByTestId('scoped-child')).not.toBeInTheDocument()
  })

  it('does not render scoped children when DDC scope loading fails', async () => {
    mocks.scopes.mockRejectedValue(new Error('offline'))
    renderScopeConsumer()

    expect(await screen.findByText('DDC 作用域加载失败'))
      .toBeInTheDocument()
    expect(screen.queryByTestId('scoped-child')).not.toBeInTheDocument()
  })
})
