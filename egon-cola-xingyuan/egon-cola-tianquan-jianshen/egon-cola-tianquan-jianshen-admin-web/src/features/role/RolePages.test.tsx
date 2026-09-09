import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {type Rbac3Client, Rbac3Provider} from '@egon-cola/tianquan-jianshen-react-sdk'
import {fireEvent, render, screen, waitFor} from '@testing-library/react'
import type {PropsWithChildren} from 'react'
import {MemoryRouter} from 'react-router-dom'
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
      permissions: ['system:role:read', 'system:role:create', 'system:role-resource:read'],
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
        ? path.includes('/1/')
          ? {roleId: '1', activationRoots: ['1'], roleFamily: ['1', '2'], effectiveFamilyRisk: 'HIGH', permissionCount: 4, conflicts: []}
          : { roleId: '2', activationRoots: ['1', '3'], roleFamily: ['2'], effectiveFamilyRisk: 'HIGH', permissionCount: 4, conflicts: ['AMBIGUOUS_ROOT'] }
        : [
          { roleId: '1', applicationId: '71', roleCode: 'ROOT', roleName: '根角色', roleType: 'MANAGEMENT', riskLevel: 'LOW', privileged: false, status: 'ACTIVE', version: 1 },
          { roleId: '2', applicationId: '71', roleCode: 'CHILD', roleName: '子角色', roleType: 'POSITION', riskLevel: 'HIGH', privileged: false, status: 'DISABLED', version: 2 },
        ]) as T,
  }
  return (
    <QueryClientProvider client={queryClient}>
        <Rbac3Provider client={sdk}>
        <FeatureApiProvider client={api}>
          <MemoryRouter initialEntries={['/iam/roles']}>{children}</MemoryRouter>
        </FeatureApiProvider>
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
    await api.create({
      applicationId: '71', roleCode: 'AUDITOR', roleName: '审计员', roleType: 'POSITION',
      riskLevel: 'LOW', privileged: false, landingRouteId: null, landingPriority: 0,
      maximumAssignmentDays: null,
    })
    await api.update('2', {
      roleName: '子角色', status: 'ACTIVE', landingRouteId: null, landingPriority: 0,
      maximumAssignmentDays: null, expectedRoleVersion: 2,
    })
    await api.addInheritance('1', { applicationId: '71', juniorRoleId: '2', expectedRoleVersion: 1 })
    await api.removeInheritance('1', '2', { applicationId: '71', expectedRoleVersion: 2 })

    expect(request).toHaveBeenNthCalledWith(1, '/api/tianquan-jianshen/v1/iam/roles', {query: {applicationId: '71'}})
    expect(request).toHaveBeenNthCalledWith(2, '/api/tianquan-jianshen/v1/iam/roles/2/impact-analysis')
    expect(request).toHaveBeenNthCalledWith(3, '/api/tianquan-jianshen/v1/iam/roles', expect.objectContaining({method: 'POST'}))
    expect(request).toHaveBeenNthCalledWith(4, '/api/tianquan-jianshen/v1/iam/roles/2', expect.objectContaining({method: 'PUT'}))
    expect(request).toHaveBeenNthCalledWith(5, '/api/tianquan-jianshen/v1/iam/roles/1/inheritances', expect.objectContaining({method: 'POST'}))
    expect(request).toHaveBeenNthCalledWith(6, '/api/tianquan-jianshen/v1/iam/roles/1/inheritances/2', {
      method: 'DELETE', query: {applicationId: '71', expectedRoleVersion: 2},
    })
  })

  it('distinguishes root child disabled and ambiguous roles', async () => {
    render(<RoleGraphPage applicationId="71" />, { wrapper })
    await waitFor(() => expect(screen.getByText('根角色')).toBeInTheDocument())
    expect(await screen.findByText('Root')).toBeInTheDocument()
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

  it('offers the five backend role types instead of treating hierarchy as a type', async () => {
    render(<RoleGraphPage applicationId="71" />, {wrapper})
    fireEvent.click(await screen.findByRole('button', {name: '新建角色'}))
    fireEvent.mouseDown(screen.getByLabelText('角色类型'))
    for (const label of ['公共角色', '岗位角色', '管理角色', '临时角色', '应急角色']) {
      expect((await screen.findAllByText(label)).length).toBeGreaterThan(0)
    }
    expect(screen.queryByText('激活根角色')).not.toBeInTheDocument()
    expect(screen.queryByText('业务角色')).not.toBeInTheDocument()
  })

  it('exposes resource authorization from every role card', async () => {
    render(<RoleGraphPage applicationId="71" />, {wrapper})

    await waitFor(() => expect(screen.getByText('根角色')).toBeInTheDocument())
    expect(screen.getAllByRole('link', {name: '资源授权'}).length).toBeGreaterThan(0)
    expect(screen.getAllByRole('link', {name: '资源授权'})[0]).toHaveAttribute('href', '/iam/roles/1/resources')
  })
})
