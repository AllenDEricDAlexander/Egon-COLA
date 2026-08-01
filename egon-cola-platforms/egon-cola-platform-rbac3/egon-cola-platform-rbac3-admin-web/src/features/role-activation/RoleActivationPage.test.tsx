import { InMemoryAccessTokenStore, Rbac3Provider, Rbac3RequestError, type Rbac3Client } from '@egon-cola/rbac3-react-sdk'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { PropsWithChildren } from 'react'
import { describe, expect, it, vi } from 'vitest'
import {
  FeatureApiProvider,
  type FeatureApiClient,
  type FeatureApiRequest,
} from '../shared/FeatureApi'
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

  it('re-authenticates and retries the unchanged role set when step-up is required', async () => {
    const stepUp = new Rbac3RequestError({
      status: 401,
      code: 'STEP_UP_REQUIRED',
      message: 'strong authentication required',
      retryable: true,
    })
    const replaceActiveRoles = vi.fn()
      .mockRejectedValueOnce(stepUp)
      .mockResolvedValue(replacementResult())
    const stepUpRequest = vi.fn(async () => ({
      sessionId: '301',
      authStrength: 'STRONG',
      strongAuthenticatedAt: '2026-08-01T15:00:00Z',
    }))
    const setup = stepUpWrapper(replaceActiveRoles, stepUpRequest)
    render(<RoleActivationPage />, { wrapper: setup.wrapper })

    await screen.findByRole('checkbox', { name: '平台管理员' })
    fireEvent.click(screen.getByRole('checkbox', { name: '平台管理员' }))
    fireEvent.click(screen.getByRole('button', { name: '激活所选角色' }))
    await screen.findByText('关键角色需要强认证')
    fireEvent.change(screen.getByLabelText('Current Password'), {
      target: { value: 'current-password' },
    })
    fireEvent.click(screen.getByRole('button', { name: '确认强认证并激活' }))

    await waitFor(() => expect(replaceActiveRoles).toHaveBeenCalledTimes(2))
    expect(replaceActiveRoles).toHaveBeenNthCalledWith(1, {
      roleIds: ['50001'], expectedSessionVersion: 4,
    })
    expect(replaceActiveRoles).toHaveBeenNthCalledWith(2, {
      roleIds: ['50001'], expectedSessionVersion: 4,
    })
    expect(stepUpRequest).toHaveBeenCalledWith(
      '/api/rbac3/v1/auth/step-up',
      { method: 'POST', body: { method: 'PASSWORD', credential: 'current-password' } },
    )
  })

  it('does not refresh the session when step-up credentials are rejected', async () => {
    const stepUp = new Rbac3RequestError({
      status: 401,
      code: 'STEP_UP_REQUIRED',
      message: 'strong authentication required',
      retryable: true,
    })
    const rejected = new Rbac3RequestError({
      status: 401,
      code: 'AUTHENTICATION_FAILED',
      message: 'Authentication failed',
      retryable: false,
    })
    const replaceActiveRoles = vi.fn(async () => { throw stepUp })
    const setup = stepUpWrapper(
      replaceActiveRoles,
      vi.fn(async () => { throw rejected }),
    )
    render(<RoleActivationPage />, { wrapper: setup.wrapper })

    await screen.findByRole('checkbox', { name: '平台管理员' })
    fireEvent.click(screen.getByRole('checkbox', { name: '平台管理员' }))
    fireEvent.click(screen.getByRole('button', { name: '激活所选角色' }))
    await screen.findByText('关键角色需要强认证')
    fireEvent.change(screen.getByLabelText('Current Password'), {
      target: { value: 'wrong-password' },
    })
    fireEvent.click(screen.getByRole('button', { name: '确认强认证并激活' }))

    await screen.findByText('强认证失败')
    expect(setup.refresh).toHaveBeenCalledTimes(1)
  })
})

const replacementResult = () => ({
  activeRoles: [{ applicationCode: 'rbac3-system', rootRoleIds: ['50001'] }],
  changed: true, sessionVersion: 5, authVersion: 2, policyVersion: 3,
  accessToken: 'next', expiresIn: 300, refreshTokenRotated: false,
  bootstrapRequired: true, snapshotChecksum: 'sum',
})

const stepUpWrapper = (
  replaceActiveRoles: Rbac3Client['replaceActiveRoles'],
  stepUpRequest: (path: string, request?: FeatureApiRequest) => Promise<unknown>,
) => {
  const candidates = {
    applications: [{
      applicationId: '71',
      applicationCode: 'rbac3-system',
      candidates: [{
        rootRoleId: '50001', rootRoleCode: 'ROLE_PLATFORM_ADMIN',
        displayName: '平台管理员', sourceRoleIds: ['50001'],
        eligibleAssignmentIds: ['201'], mutexSetIds: [],
        effectiveFamilyRisk: 'CRITICAL', requiredAuthStrength: 'STRONG',
        landingRouteCode: null,
      }],
    }],
    basedOnAuthVersion: 2, basedOnPolicyVersion: 3,
    basedOnDirectorySnapshotVersion: '8', configurationErrors: [],
    calculatedAt: '2026-08-01T00:00:00Z',
  }
  const activeRoles = {
    sessionId: '301', activeRoles: [], activationRequired: true,
    authVersion: 2, sessionVersion: 4, policyVersion: 3,
    snapshotChecksum: 'old',
  }
  const refresh = vi.fn(async () => ({
    accessToken: 'access', roleActivationRequired: true,
  }))
  const sdk = {
    refresh,
    getActivationCandidates: async () => candidates,
    getActiveRoles: async () => activeRoles,
    replaceActiveRoles,
    getBootstrap: async () => ({
      user: { id: '7', tenantId: '9' }, permissions: [], fieldPolicies: {},
      activeRoleContexts: [], apps: [], menus: [], routes: [], actions: [],
    }),
  } as unknown as Rbac3Client
  const api: FeatureApiClient = {
    request: async <T,>(path: string, request?: FeatureApiRequest) => {
      if (path.endsWith('/role-activation-candidates')) return candidates as T
      if (path.endsWith('/role-activations')) return activeRoles as T
      return await stepUpRequest(path, request) as T
    },
  }
  const wrapper = ({ children }: PropsWithChildren) => (
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <Rbac3Provider client={sdk} accessTokenStore={new InMemoryAccessTokenStore()}>
        <FeatureApiProvider client={api}>{children}</FeatureApiProvider>
      </Rbac3Provider>
    </QueryClientProvider>
  )
  return { wrapper, refresh }
}

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
