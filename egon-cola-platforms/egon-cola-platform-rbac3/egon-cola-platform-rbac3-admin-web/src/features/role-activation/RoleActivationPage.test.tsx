import {type Rbac3Client, Rbac3Provider} from '@egon-cola/rbac3-react-sdk'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {fireEvent, render, screen, waitFor} from '@testing-library/react'
import type {PropsWithChildren} from 'react'
import {describe, expect, it, vi} from 'vitest'
import {type FeatureApiClient, FeatureApiProvider} from '../shared/FeatureApi'
import {RoleActivationPage} from './RoleActivationPage'

describe('role activation page', () => {
    it('submits a complete role set with the current authorization version', async () => {
        const replaceActiveRoles = vi.fn<Rbac3Client['replaceActiveRoles']>(async () => ({
      activeRoles: [{ applicationCode: 'orders', rootRoleIds: ['11', '12'] }],
            changed: true,
            authVersion: 3,
            policyVersion: 3,
            activationRequired: false,
            snapshotChecksum: 'sum',
    }))
    render(<RoleActivationPage />, { wrapper: wrapper(replaceActiveRoles) })

    await waitFor(() => expect(screen.getByText('订单管理员')).toBeInTheDocument())
    fireEvent.click(screen.getByRole('checkbox', { name: /订单复核员/ }))
    fireEvent.click(screen.getByRole('button', { name: '激活所选角色' }))

    await waitFor(() => expect(replaceActiveRoles).toHaveBeenCalledWith({
        roleIds: ['11', '12'], expectedAuthVersion: 2,
    }))
  })
})

const wrapper = (replaceActiveRoles: Rbac3Client['replaceActiveRoles']) => ({children}: PropsWithChildren) => {
  const candidates = {
      applications: [{
          applicationId: '71', applicationCode: 'orders', candidates: [
              {
                  rootRoleId: '11',
                  rootRoleCode: 'ORDER_ADMIN',
                  displayName: '订单管理员',
                  sourceRoleIds: ['91'],
                  eligibleAssignmentIds: ['201'],
                  mutexSetIds: [],
                  effectiveFamilyRisk: 'HIGH',
                  requiredAuthStrength: 'MFA',
                  landingRouteCode: 'orders'
              },
              {
                  rootRoleId: '12',
                  rootRoleCode: 'ORDER_REVIEWER',
                  displayName: '订单复核员',
                  sourceRoleIds: ['92'],
                  eligibleAssignmentIds: ['202'],
                  mutexSetIds: [],
                  effectiveFamilyRisk: 'LOW',
                  requiredAuthStrength: 'PASSWORD',
                  landingRouteCode: 'orders'
              },
          ]
      }],
      basedOnAuthVersion: 2,
      basedOnPolicyVersion: 3,
      basedOnDirectorySnapshotVersion: '8',
      configurationErrors: [],
    calculatedAt: '2026-08-01T00:00:00Z',
  }
  const activeRoles = {
      activeRoles: [{applicationCode: 'orders', rootRoleIds: ['11']}],
      activationRequired: true,
      authVersion: 2,
      policyVersion: 3,
    snapshotChecksum: 'old',
  }
  const sdk = {
    getActivationCandidates: async () => candidates,
    getActiveRoles: async () => activeRoles,
    replaceActiveRoles,
        getAbout: async () => ({
        user: {id: '7', tenantId: '9', identitySub: 'mario', status: 'ACTIVE'},
        permissions: ['system:role-activation:use'],
        fieldPolicies: {}, activeRoleContexts: [], apps: [], menus: [], routes: [], actions: [],
        defaultApplicationCode: null, defaultRoute: null, authVersion: 2, policyVersion: 3,
    }),
  } as unknown as Rbac3Client
  const api: FeatureApiClient = {
    request: async <T,>(path: string) => (
      path.endsWith('/role-activation-candidates') ? candidates : activeRoles
    ) as T,
  }
  return (
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <Rbac3Provider client={sdk}>
        <FeatureApiProvider client={api}>{children}</FeatureApiProvider>
      </Rbac3Provider>
    </QueryClientProvider>
  )
}
