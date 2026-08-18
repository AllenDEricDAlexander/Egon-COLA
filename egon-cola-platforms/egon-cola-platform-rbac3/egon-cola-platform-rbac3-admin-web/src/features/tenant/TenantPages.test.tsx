import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {type Rbac3Client, Rbac3Provider} from '@egon-cola/rbac3-react-sdk'
import {render, screen, waitFor} from '@testing-library/react'
import type {PropsWithChildren} from 'react'
import {describe, expect, it} from 'vitest'
import {type FeatureApiClient, FeatureApiProvider} from '../shared/FeatureApi'
import {TenantListPage} from './TenantListPage'

const wrapper = ({ children }: PropsWithChildren) => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const sdk = {
    getAbout: async () => ({
        user: {id: '7', tenantId: '42', identitySub: 'tenant-test', status: 'ACTIVE'},
      permissions: ['system:tenant:read'],
      fieldPolicies: {},
        activeRoleContexts: [], apps: [], menus: [], routes: [], actions: [],
        defaultApplicationCode: null, defaultRoute: null, authVersion: 1, policyVersion: 1,
    }),
  } as unknown as Rbac3Client
  const api: FeatureApiClient = {
    request: async <T,>() => ({ tenantId: '42', tenantCode: 'tenant-a', tenantName: '租户 A', status: 'ACTIVE' }) as T,
  }
  return (
    <QueryClientProvider client={queryClient}>
        <Rbac3Provider client={sdk}>
        <FeatureApiProvider client={api}>{children}</FeatureApiProvider>
      </Rbac3Provider>
    </QueryClientProvider>
  )
}

describe('tenant pages', () => {
  it('keeps the active tenant context as a string bigint id', async () => {
    render(<TenantListPage />, { wrapper })

    await waitFor(() => expect(screen.getByText('42')).toBeInTheDocument())
    expect(screen.getByText('当前登录租户')).toBeInTheDocument()
  })

})
