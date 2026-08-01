import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { InMemoryAccessTokenStore, Rbac3Provider, type Rbac3Client } from '@egon-cola/rbac3-react-sdk'
import { render, screen, waitFor } from '@testing-library/react'
import type { PropsWithChildren } from 'react'
import { describe, expect, it } from 'vitest'
import { FeatureApiProvider, type FeatureApiClient } from '../shared/FeatureApi'
import { TenantDetailPage } from './TenantDetailPage'
import { TenantListPage } from './TenantListPage'

const wrapper = ({ children }: PropsWithChildren) => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const sdk = {
    refresh: async () => ({ accessToken: 'access', roleActivationRequired: false }),
    getBootstrap: async () => ({
      user: { id: '7', tenantId: '42', username: 'mario', displayName: 'Mario' },
      permissions: ['system:tenant:read'],
      fieldPolicies: {},
    }),
  } as unknown as Rbac3Client
  const api: FeatureApiClient = {
    request: async <T,>() => ({ tenantId: '42', tenantCode: 'tenant-a', tenantName: '租户 A', status: 'ACTIVE' }) as T,
  }
  return (
    <QueryClientProvider client={queryClient}>
      <Rbac3Provider client={sdk} accessTokenStore={new InMemoryAccessTokenStore()}>
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

  it('loads tenant detail without a cross-tenant browser cache', async () => {
    render(<TenantDetailPage tenantId="42" />, { wrapper })

    await waitFor(() => expect(screen.getByText('租户 A')).toBeInTheDocument())
    expect(screen.getByText('ACTIVE')).toBeInTheDocument()
  })
})
