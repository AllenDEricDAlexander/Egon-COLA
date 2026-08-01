import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { InMemoryAccessTokenStore, Rbac3Provider, type Rbac3Client } from '@egon-cola/rbac3-react-sdk'
import { render, screen, waitFor } from '@testing-library/react'
import type { PropsWithChildren } from 'react'
import { describe, expect, it } from 'vitest'
import { FeatureApiProvider, type FeatureApiClient } from '../shared/FeatureApi'
import { ApplicationListPage } from './ApplicationListPage'
import { ManifestDetailPage } from './ManifestDetailPage'
import { ResourceCatalogPage } from './ResourceCatalogPage'

const wrapper = ({ children }: PropsWithChildren) => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const sdk = {
    refresh: async () => ({ accessToken: 'access', roleActivationRequired: false }),
    getBootstrap: async () => ({
      user: { id: '7', tenantId: '42' },
      permissions: ['system:application:read', 'system:resource:read'],
      fieldPolicies: {},
    }),
  } as unknown as Rbac3Client
  const api: FeatureApiClient = {
    request: async <T,>(path: string) => {
      if (path === '/api/rbac3/v1/applications') {
        return [{ applicationId: '71', applicationCode: 'orders', applicationName: '订单', status: 'ACTIVE', version: 2 }] as T
      }
      if (path.includes('/resources')) {
        return [{ resourceId: '81', applicationId: '71', resourceType: 'ROUTE', resourceCode: 'orders.list', resourceName: '订单列表', status: 'STALE', version: 4 }] as T
      }
      if (path.endsWith('/validation')) {
        return { manifestId: '91', valid: true, errors: [], warnings: [] } as T
      }
      if (path.endsWith('/impact-analysis')) {
        return { manifestId: '91', resourcesAdded: 2, resourcesChanged: 1, resourcesStale: 0, affectedRoleCount: 3, conflicts: [] } as T
      }
      return { manifestId: '91', applicationId: '71', status: 'VALIDATED', checksum: 'sha256:abc', manifestVersion: 8 } as T
    },
  }
  return (
    <QueryClientProvider client={queryClient}>
      <Rbac3Provider client={sdk} accessTokenStore={new InMemoryAccessTokenStore()}>
        <FeatureApiProvider client={api}>{children}</FeatureApiProvider>
      </Rbac3Provider>
    </QueryClientProvider>
  )
}

describe('application pages', () => {
  it('lists applications in the active tenant', async () => {
    render(<ApplicationListPage />, { wrapper })
    await waitFor(() => expect(screen.getByText('订单')).toBeInTheDocument())
    expect(screen.getByText('orders')).toBeInTheDocument()
  })

  it('shows immutable manifest checksum and version', async () => {
    render(<ManifestDetailPage manifestId="91" />, { wrapper })
    await waitFor(() => expect(screen.getByText('sha256:abc')).toBeInTheDocument())
    expect(screen.getByText('8')).toBeInTheDocument()
  })

  it('shows stale resources and hides archive without permission', async () => {
    render(<ResourceCatalogPage applicationId="71" />, { wrapper })
    await waitFor(() => expect(screen.getByText('STALE')).toBeInTheDocument())
    expect(screen.queryByRole('button', { name: '归档' })).not.toBeInTheDocument()
  })
})
