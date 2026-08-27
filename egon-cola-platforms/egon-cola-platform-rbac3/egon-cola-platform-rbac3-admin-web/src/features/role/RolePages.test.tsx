import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {type Rbac3Client, Rbac3Provider} from '@egon-cola/rbac3-react-sdk'
import {render, screen, waitFor} from '@testing-library/react'
import type {PropsWithChildren} from 'react'
import {describe, expect, it, vi} from 'vitest'
import {type FeatureApiClient, FeatureApiProvider} from '../shared/FeatureApi'
import {RoleGraphPage} from './RoleGraphPage'
import {RoleResourceGrantPage} from './RoleResourceGrantPage'
import {roleApi} from './role.api'

const wrapper = ({ children }: PropsWithChildren) => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const sdk = {
    getAbout: async () => ({
        user: {id: '7', tenantId: '42', identitySub: 'role-test', status: 'ACTIVE'},
      permissions: ['system:role:read'],
        fieldPolicies: {}, activeRoleContexts: [], apps: [], menus: [], routes: [], actions: [],
        defaultApplicationCode: null, defaultRoute: null, authVersion: 1, policyVersion: 1,
    }),
  } as unknown as Rbac3Client
  const api: FeatureApiClient = {
    request: async <T,>(path: string) => (path.endsWith('/resources')
      ? {
          roleId: '2', applicationId: '71', roleVersion: 4,
          directResourceIds: ['501'], derivedApiResourceIds: ['701'],
          summary: { menuPageCount: 1, actionCount: 1, apiCount: 1, inheritedCount: 0 },
          nodes: [
            { resourceId: '500', resourceCode: 'iam', name: '权限平台', category: 'MENU_PAGE', technicalType: 'MENU', parentCode: null, status: 'ACTIVE', mappingStatus: 'CONFIGURED', grantState: 'NONE', grantable: false, disabledReason: 'RESOURCE_NOT_GRANTABLE', linkedApis: [], children: [
              { resourceId: '501', resourceCode: 'iam.users', name: '用户管理', category: 'MENU_PAGE', technicalType: 'ROUTE', parentCode: 'iam', status: 'ACTIVE', mappingStatus: 'CONFIGURED', grantState: 'DIRECT', grantable: true, disabledReason: null, linkedApis: [{ resourceId: '701', resourceCode: 'iam.api.users.list', name: '用户列表', method: 'GET', path: '/users' }], children: [] },
              { resourceId: '502', resourceCode: 'iam.users.disable', name: '禁用用户', category: 'ACTION', technicalType: 'ACTION', parentCode: 'iam.users', status: 'ACTIVE', mappingStatus: 'PENDING_VALIDATION', grantState: 'NONE', grantable: false, disabledReason: 'PERMISSION_MAPPING_REQUIRED', linkedApis: [], children: [] },
            ] },
          ],
        }
      : path.includes('impact-analysis')
        ? { roleId: '2', activationRoots: ['1', '3'], roleFamily: ['2'], effectiveFamilyRisk: 'HIGH', permissionCount: 4, conflicts: ['AMBIGUOUS_ROOT'] }
        : [
          { roleId: '1', applicationId: '71', roleCode: 'ROOT', roleName: '根角色', roleType: 'ACTIVATION_ROOT', riskLevel: 'LOW', privileged: false, status: 'ACTIVE', version: 1 },
          { roleId: '2', applicationId: '71', roleCode: 'CHILD', roleName: '子角色', roleType: 'BUSINESS', riskLevel: 'HIGH', privileged: false, status: 'DISABLED', version: 2 },
        ]) as T,
  }
  return (
    <QueryClientProvider client={queryClient}>
        <Rbac3Provider client={sdk}>
        <FeatureApiProvider client={api}>{children}</FeatureApiProvider>
      </Rbac3Provider>
    </QueryClientProvider>
  )
}

describe('role pages', () => {
  it('uses the IAM controller paths for role list and impact analysis', async () => {
    const request = vi.fn().mockResolvedValue([])
    const api = roleApi({request})

    await api.roles('71')
    await api.impact('2')

    expect(request).toHaveBeenNthCalledWith(1, '/api/rbac3/v1/iam/roles', {query: {applicationId: '71'}})
    expect(request).toHaveBeenNthCalledWith(2, '/api/rbac3/v1/iam/roles/2/impact-analysis')
  })

  it('distinguishes root child disabled and ambiguous roles', async () => {
    render(<RoleGraphPage applicationId="71" />, { wrapper })
    await waitFor(() => expect(screen.getByText('根角色')).toBeInTheDocument())
    expect(screen.getByText('Root')).toBeInTheDocument()
    expect(screen.getByText('Child')).toBeInTheDocument()
    expect(screen.getByText('Disabled')).toBeInTheDocument()
    expect(screen.getByText('Ambiguous')).toBeInTheDocument()
  })

  it('configures readable resource roots without exposing permission characters', async () => {
    render(<RoleResourceGrantPage roleId="2" />, { wrapper })
    await waitFor(() => expect(screen.getByText('用户管理')).toBeInTheDocument())
    expect(screen.getByText(/用户列表/)).toBeInTheDocument()
    expect(screen.queryByText(/system:|permissionId/i)).not.toBeInTheDocument()
  })
})
