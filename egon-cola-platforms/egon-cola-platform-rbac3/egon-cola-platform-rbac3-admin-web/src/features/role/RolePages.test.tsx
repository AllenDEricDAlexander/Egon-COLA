import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {type Rbac3Client, Rbac3Provider} from '@egon-cola/rbac3-react-sdk'
import {render, screen, waitFor} from '@testing-library/react'
import type {PropsWithChildren} from 'react'
import {describe, expect, it} from 'vitest'
import {type FeatureApiClient, FeatureApiProvider} from '../shared/FeatureApi'
import {RoleGraphPage} from './RoleGraphPage'
import {RolePermissionPage} from './RolePermissionPage'

const wrapper = ({ children }: PropsWithChildren) => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const sdk = {
    getBootstrap: async () => ({
        user: {id: '7', tenantId: '42', identitySub: 'role-test', status: 'ACTIVE'},
      permissions: ['system:role:read'],
        fieldPolicies: {}, activeRoleContexts: [], apps: [], menus: [], routes: [], actions: [],
        defaultApplicationCode: null, defaultRoute: null, authVersion: 1, policyVersion: 1,
    }),
  } as unknown as Rbac3Client
  const api: FeatureApiClient = {
    request: async <T,>(path: string) => (path.includes('impact-analysis')
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
  it('distinguishes root child disabled and ambiguous roles', async () => {
    render(<RoleGraphPage applicationId="71" />, { wrapper })
    await waitFor(() => expect(screen.getByText('根角色')).toBeInTheDocument())
    expect(screen.getByText('Root')).toBeInTheDocument()
    expect(screen.getByText('Child')).toBeInTheDocument()
    expect(screen.getByText('Disabled')).toBeInTheDocument()
    expect(screen.getByText('Ambiguous')).toBeInTheDocument()
  })

  it('shows role impact before permission changes', async () => {
    render(<RolePermissionPage roleId="2" />, { wrapper })
    await waitFor(() => expect(screen.getByText('4')).toBeInTheDocument())
    expect(screen.getAllByText('HIGH').length).toBeGreaterThan(0)
  })
})
