import { InMemoryAccessTokenStore, Rbac3Provider, type Rbac3Client } from '@egon-cola/rbac3-react-sdk'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import type { PropsWithChildren } from 'react'
import { describe, expect, it } from 'vitest'
import { FeatureApiProvider, type FeatureApiClient } from '../shared/FeatureApi'
import { AssignmentListPage } from './AssignmentListPage'

describe('assignment pages', () => {
  it('renders assignment eligibility states and idempotent guarded actions', async () => {
    const request: FeatureApiClient['request'] = async <T,>() => [{
      assignmentId: '9007199254740999', roleId: '81', assignmentType: 'DIRECT',
      status: 'ACTIVE', validFrom: '2026-08-01T00:00:00Z', validTo: null,
      sourceType: 'MANUAL', sourceId: '7', version: 3,
    }] as T
    render(<AssignmentListPage userId="42" />, { wrapper: wrapper(request, ['system:role-assignment:manage']) })

    await waitFor(() => expect(screen.getByText('9007199254740999')).toBeInTheDocument())
    expect(screen.getByText(/授权资格/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /暂\s*停/ })).toBeInTheDocument()
    expect(document.body.textContent).not.toMatch(/排班|轮岗|审批/)
  })

  it('hides mutation actions without permission', async () => {
    const request: FeatureApiClient['request'] = async <T,>() => [{
      assignmentId: '1', roleId: '81', assignmentType: 'DIRECT', status: 'ACTIVE',
      validFrom: '2026-08-01T00:00:00Z', validTo: null, sourceType: 'MANUAL', sourceId: '7', version: 1,
    }] as T
    render(<AssignmentListPage userId="42" />, { wrapper: wrapper(request, []) })
    await waitFor(() => expect(screen.getByText('81')).toBeInTheDocument())
    expect(screen.queryByRole('button', { name: '新增任职资格' })).not.toBeInTheDocument()
  })
})

const wrapper = (request: FeatureApiClient['request'], permissions: readonly string[]) => ({ children }: PropsWithChildren) => {
  const sdk = {
    refresh: async () => ({ accessToken: 'access', roleActivationRequired: false }),
    getBootstrap: async () => ({
      user: { id: '7', tenantId: '9' }, permissions, fieldPolicies: {},
      activeRoleContexts: [], apps: [], menus: [], routes: [], actions: [],
    }),
  } as unknown as Rbac3Client
  return (
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <Rbac3Provider client={sdk} accessTokenStore={new InMemoryAccessTokenStore()}>
        <FeatureApiProvider client={{ request }}>{children}</FeatureApiProvider>
      </Rbac3Provider>
    </QueryClientProvider>
  )
}
