import {type Rbac3Client, Rbac3Provider} from '@egon-cola/rbac3-react-sdk'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {fireEvent, render, screen, waitFor} from '@testing-library/react'
import type {PropsWithChildren} from 'react'
import {describe, expect, it} from 'vitest'
import {type FeatureApiClient, FeatureApiProvider} from '../shared/FeatureApi'
import {AuthorizationSimulationPage} from './AuthorizationSimulationPage'

const wrapper = ({ children }: PropsWithChildren) => {
  const sdk = {
        getAbout: async () => ({
        user: {id: '7', tenantId: '9', identitySub: 'simulation-test', status: 'ACTIVE'},
        authVersion: 2,
        policyVersion: 4,
      permissions: ['system:authorization-simulation:execute'], fieldPolicies: {}, activeRoleContexts: [], apps: [], menus: [], routes: [], actions: [],
        defaultApplicationCode: null,
        defaultRoute: null,
    }),
  } as unknown as Rbac3Client
  const api: FeatureApiClient = { request: async <T,>(path: string) => (path.includes('role-change-impact') ? {
    impact: { roleId: '1001', activationRoots: ['root-1'], roleFamily: ['BUSINESS'], effectiveFamilyRisk: 'LOW', permissionCount: 3, conflicts: [] },
    policyVersion: 4, evidenceChecksum: 'impact-checksum', expiresAt: '2026-08-01T01:00:00Z',
  } : {
    current: { functionDecision: { decision: 'DENY', reasonCode: 'PERMISSION_MISSING', evidenceIds: ['1'] } },
    hypothetical: { functionDecision: { decision: 'ALLOW', reasonCode: 'PERMISSION_GRANTED', evidenceIds: ['2'] } },
          authVersion: 2, policyVersion: 4, snapshotChecksum: 'safe-checksum', expiresAt: '2026-08-01T01:00:00Z',
  }) as T }
    return <QueryClientProvider client={new QueryClient()}><Rbac3Provider client={sdk}><FeatureApiProvider
        client={api}>{children}</FeatureApiProvider></Rbac3Provider></QueryClientProvider>
}

describe('authorization simulation', () => {
  it('shows current and hypothetical decisions with consistent versions and no apply action', async () => {
    render(<AuthorizationSimulationPage />, { wrapper })
    await waitFor(() => expect(screen.getByLabelText('Permission Code')).toHaveValue(''))
    fireEvent.change(screen.getByLabelText('Permission Code'), { target: { value: 'orders:read' } })
    fireEvent.change(screen.getByLabelText('Application Code'), { target: { value: 'orders' } })
    fireEvent.change(screen.getByLabelText('Resource Code'), { target: { value: 'orders.list' } })
    fireEvent.click(screen.getByRole('button', { name: '执行无副作用模拟' }))
    await waitFor(() => expect(screen.getByText('PERMISSION_GRANTED')).toBeInTheDocument())
    expect(screen.getByText('safe-checksum')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /应用模拟/ })).not.toBeInTheDocument()
  })

  it('shows the role-change impact result without an apply action', async () => {
    render(<AuthorizationSimulationPage />, { wrapper })
    fireEvent.change(screen.getByLabelText('Role ID'), { target: { value: '1001' } })
    await waitFor(() => expect(screen.getByRole('button', { name: '查询角色变更影响' })).toBeInTheDocument())
    fireEvent.click(screen.getByRole('button', { name: '查询角色变更影响' }))
    await waitFor(() => expect(screen.getByText('impact-checksum')).toBeInTheDocument())
    expect(screen.getByText('LOW')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /应用角色变更/ })).not.toBeInTheDocument()
  })
})
