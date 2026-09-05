import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {type Rbac3Client, Rbac3Provider} from '@egon-cola/rbac3-react-sdk'
import {fireEvent, render, screen, waitFor} from '@testing-library/react'
import type {PropsWithChildren} from 'react'
import {describe, expect, it, vi} from 'vitest'
import {type FeatureApiClient, type FeatureApiRequest, FeatureApiProvider} from '../shared/FeatureApi'
import {PermissionCatalogPage} from './PermissionCatalogPage'

const application = {
  applicationId: '71',
  ddcBusinessId: '7',
  ddcApplicationId: '71',
  businessCode: 'trade',
  applicationCode: 'orders',
  applicationName: '订单服务',
  status: 'ACTIVE',
  displayPriority: 1,
  version: 2,
}

const permission = {
  id: '91',
  applicationId: '71',
  permissionCode: 'orders.read',
  permissionName: '读取订单',
  riskLevel: 'MEDIUM',
  status: 'ACTIVE',
  sourceType: 'MANUAL',
  sourceBuildId: null,
  sourceChecksum: null,
  version: 1,
}

const wrapper = (request: FeatureApiClient['request']) => ({children}: PropsWithChildren) => {
  const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}})
  const sdk = {
    getAbout: async () => ({
      user: {subject: 'permission-test', tenantId: '42', status: 'ACTIVE'},
      permissions: ['system:permission:read', 'system:permission:manage'],
      fieldPolicies: {}, activeRoles: [], currentApplicationCode: null,
      landingRouteCode: null, authVersion: 1, policyVersion: 1,
    }),
  } as unknown as Rbac3Client
  return (
    <QueryClientProvider client={queryClient}>
      <Rbac3Provider client={sdk}>
        <FeatureApiProvider client={{request}}>{children}</FeatureApiProvider>
      </Rbac3Provider>
    </QueryClientProvider>
  )
}

describe('PermissionCatalogPage', () => {
  it('renders the permission controller and submits create/status paths', async () => {
    const request = vi.fn(async <T,>(path: string, options: FeatureApiRequest = {}): Promise<T> => {
      if (path === '/api/rbac3/v1/iam/tenant-applications') return [{...application, applicationId: '820'}] as T
      if (path === '/api/rbac3/v1/iam/resource-catalog/applications') return [application] as T
      if (path === '/api/rbac3/v1/iam/permissions/91') return permission as T
      if (options?.method === 'POST') return permission as T
      if (path.endsWith('/status')) return {...permission, status: 'DISABLED', version: 2} as T
      return (options.query?.applicationId === '71' ? [permission] : []) as T
    }) as unknown as FeatureApiClient['request']
    render(<PermissionCatalogPage />, {wrapper: wrapper(request)})

    await waitFor(() => expect(screen.getByText('orders.read')).toBeInTheDocument())
    expect(request).toHaveBeenCalledWith('/api/rbac3/v1/iam/permissions', {query: {applicationId: '71', assignable: false}})

    fireEvent.click(screen.getByText('orders.read'))
    await waitFor(() => expect(request).toHaveBeenCalledWith('/api/rbac3/v1/iam/permissions/91', expect.anything()))

    fireEvent.click(screen.getByRole('button', {name: '新建权限'}))
    fireEvent.change(document.getElementById('permissionCode')!, {target: {value: 'orders.write'}})
    fireEvent.change(document.getElementById('permissionName')!, {target: {value: '写入订单'}})
    fireEvent.click(screen.getByRole('button', {name: /保.*存/}))
    await waitFor(() => expect(request).toHaveBeenCalledWith(
      '/api/rbac3/v1/iam/permissions',
      expect.objectContaining({method: 'POST', body: expect.objectContaining({applicationId: '71', riskLevel: 'MEDIUM'})}),
    ))

    fireEvent.click(screen.getByRole('button', {name: /停.*用/}))
    await waitFor(() => expect(request).toHaveBeenCalledWith(
      '/api/rbac3/v1/iam/permissions/91/status',
      {method: 'PUT', body: {status: 'DISABLED', expectedVersion: 1}},
    ))
  })
})
