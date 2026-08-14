import {type Rbac3Client, Rbac3Provider} from '@egon-cola/rbac3-react-sdk'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {fireEvent, render, screen, waitFor} from '@testing-library/react'
import type {PropsWithChildren} from 'react'
import {describe, expect, it} from 'vitest'
import {type FeatureApiClient, FeatureApiProvider} from '../shared/FeatureApi'
import {AuthorizationSimulationPage} from './AuthorizationSimulationPage'

const wrapper = ({ children }: PropsWithChildren) => {
  const sdk = {
    getBootstrap: async () => ({
        user: {id: '7', tenantId: '9', identitySub: 'simulation-test', status: 'ACTIVE'},
        authVersion: 2,
        policyVersion: 4,
      permissions: ['system:authorization-simulation:execute'], fieldPolicies: {}, activeRoleContexts: [], apps: [], menus: [], routes: [], actions: [],
        defaultApplicationCode: null,
        defaultRoute: null,
    }),
  } as unknown as Rbac3Client
  const api: FeatureApiClient = { request: async <T,>() => ({
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
})
