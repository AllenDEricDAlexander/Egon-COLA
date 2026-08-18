import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {type Rbac3Client, Rbac3Provider} from '@egon-cola/rbac3-react-sdk'
import {render, screen, waitFor} from '@testing-library/react'
import type {PropsWithChildren} from 'react'
import {describe, expect, it} from 'vitest'
import {type FeatureApiClient, FeatureApiProvider} from '../shared/FeatureApi'
import {ApplicationListPage} from './ApplicationListPage'
import {FieldDefinitionPage} from './FieldDefinitionPage'
import {PermissionPage} from './PermissionPage'
import {ResourceCatalogPage} from './ResourceCatalogPage'

const wrapper = ({ children }: PropsWithChildren) => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    const sdk = {
    getAbout: async () => ({
        user: {subject: 'application-test', tenantId: '42', status: 'ACTIVE'},
      permissions: ['system:application:read', 'system:resource:read', 'system:field-definition:read', 'system:permission:read'],
        fieldPolicies: {}, activeRoles: [], currentApplicationCode: null, landingRouteCode: null, authVersion: 1, policyVersion: 1,
    }),
  } as unknown as Rbac3Client
  const api: FeatureApiClient = {
    request: async <T,>(path: string) => {
      if (path === '/api/rbac3/v1/iam/tenant-applications') {
        return [{ applicationId: '71', ddcBusinessId: '7', ddcApplicationId: '71', businessCode: 'trade', applicationCode: 'orders', applicationName: '订单', status: 'ACTIVE', displayPriority: 1, version: 2 }] as T
      }
      if (path.includes('/resources')) {
        return [{ resourceId: '81', applicationId: '71', resourceType: 'ROUTE', resourceCode: 'orders.list', resourceName: '订单列表', status: 'STALE', version: 4 }] as T
      }
      if (path.includes('/fields')) {
        return [{ id: '91', applicationId: '71', resourceId: '81', fieldCode: 'email', jsonPath: 'email', dataType: 'STRING', sensitivity: 'NORMAL', defaultAccess: 'READ', maskingStrategy: null, writable: false, exportable: true, status: 'ACTIVE', version: 1 }] as T
      }
      if (path.includes('/permissions')) {
        return [{ id: '91', applicationId: '71', permissionCode: 'orders:read', permissionName: '读取订单', riskLevel: 'NORMAL', status: 'ACTIVE', sourceType: 'MANUAL', version: 1 }] as T
      }
      return [{ applicationId: '71', applicationCode: 'orders', applicationName: '订单', status: 'ACTIVE', version: 2 }] as T
    },
  }
  return (
    <QueryClientProvider client={queryClient}>
        <Rbac3Provider client={sdk}>
        <FeatureApiProvider client={api}>{children}</FeatureApiProvider>
      </Rbac3Provider>
    </QueryClientProvider>
  )
}

describe('application pages', () => {
  it('lists tenant applications in the active tenant', async () => {
    render(<ApplicationListPage />, { wrapper })
    await waitFor(() => expect(screen.getByText('订单')).toBeInTheDocument())
    expect(screen.getByText('orders')).toBeInTheDocument()
  })

  it('lists field definitions without a legacy lifecycle', async () => {
    render(<FieldDefinitionPage />, { wrapper })
    await waitFor(() => expect(screen.getAllByText('email').length).toBeGreaterThan(0))
    expect(screen.queryByText(/legacy lifecycle/i)).not.toBeInTheDocument()
  })

  it('lists global permission characters', async () => {
    render(<PermissionPage />, { wrapper })
    await waitFor(() => expect(screen.getByText('orders:read')).toBeInTheDocument())
  })

  it('shows stale resources and hides archive without permission', async () => {
    render(<ResourceCatalogPage applicationId="71" />, { wrapper })
    await waitFor(() => expect(screen.getByText('STALE')).toBeInTheDocument())
    expect(screen.queryByRole('button', { name: '归档' })).not.toBeInTheDocument()
  })
})
