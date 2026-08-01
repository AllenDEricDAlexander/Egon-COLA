import { InMemoryAccessTokenStore, Rbac3Provider, type Rbac3Client } from '@egon-cola/rbac3-react-sdk'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { PropsWithChildren } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { FeatureApiProvider, type FeatureApiClient } from '../shared/FeatureApi'
import { RoleActivationPage } from './RoleActivationPage'

describe('role activation page', () => {
  it('groups candidates by APP, explains normalized roots, and replaces the complete set', async () => {
    const replaceActiveRoles = vi.fn(async () => ({
      activeRoles: [{ applicationCode: 'orders', rootRoleIds: ['11', '12'] }],
      changed: true, sessionVersion: 5, authVersion: 2, policyVersion: 3,
      accessToken: 'next', expiresIn: 300, refreshTokenRotated: false,
      bootstrapRequired: true, snapshotChecksum: 'sum',
    }))
    render(<RoleActivationPage />, { wrapper: wrapper(replaceActiveRoles) })

    await waitFor(() => expect(screen.getByText('订单管理员')).toBeInTheDocument())
    expect(screen.getByText(/子角色 91.*根角色 11/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('checkbox', { name: /订单复核员/ }))
    fireEvent.click(screen.getByRole('button', { name: '激活所选角色' }))
    await waitFor(() => expect(replaceActiveRoles).toHaveBeenCalledWith({
      roleIds: ['11', '12'], expectedSessionVersion: 4,
    }))
  })
})

const wrapper = (replaceActiveRoles: Rbac3Client['replaceActiveRoles']) => ({ children }: PropsWithChildren) => {
  const candidates = {
    applications: [{ applicationId: '71', applicationCode: 'orders', candidates: [
      { rootRoleId: '11', rootRoleCode: 'ORDER_ADMIN', displayName: '订单管理员', sourceRoleIds: ['91'], eligibleAssignmentIds: ['201'], mutexSetIds: [], effectiveFamilyRisk: 'HIGH', requiredAuthStrength: 'MFA', landingRouteCode: 'orders' },
      { rootRoleId: '12', rootRoleCode: 'ORDER_REVIEWER', displayName: '订单复核员', sourceRoleIds: ['92'], eligibleAssignmentIds: ['202'], mutexSetIds: [], effectiveFamilyRisk: 'LOW', requiredAuthStrength: 'PASSWORD', landingRouteCode: 'orders' },
    ] }], basedOnAuthVersion: 2, basedOnPolicyVersion: 3, basedOnDirectorySnapshotVersion: '8', configurationErrors: [], calculatedAt: '2026-08-01T00:00:00Z',
  }
  const activeRoles = { sessionId: '301', activeRoles: [{ applicationCode: 'orders', rootRoleIds: ['11'] }], activationRequired: true, authVersion: 2, sessionVersion: 4, policyVersion: 3, snapshotChecksum: 'old' }
  const sdk = {
    refresh: async () => ({ accessToken: 'access', roleActivationRequired: true }),
    getActivationCandidates: async () => candidates,
    getActiveRoles: async () => activeRoles,
    replaceActiveRoles,
    getBootstrap: async () => ({ user: { id: '7', tenantId: '9' }, permissions: [], fieldPolicies: {}, activeRoleContexts: [], apps: [], menus: [], routes: [], actions: [] }),
  } as unknown as Rbac3Client
  const api: FeatureApiClient = {
    request: async <T,>(path: string) => (
      path.endsWith('/role-activation-candidates') ? candidates : activeRoles
    ) as T,
  }
  return (
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <Rbac3Provider client={sdk} accessTokenStore={new InMemoryAccessTokenStore()}>
        <FeatureApiProvider client={api}>{children}</FeatureApiProvider>
      </Rbac3Provider>
    </QueryClientProvider>
  )
}
