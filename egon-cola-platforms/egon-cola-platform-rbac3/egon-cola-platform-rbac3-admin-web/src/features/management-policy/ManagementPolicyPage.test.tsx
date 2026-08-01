import { InMemoryAccessTokenStore, Rbac3Provider, type Rbac3Client } from '@egon-cola/rbac3-react-sdk'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import type { PropsWithChildren } from 'react'
import { describe, expect, it } from 'vitest'
import { FeatureApiProvider, type FeatureApiClient } from '../shared/FeatureApi'
import { ManagementPolicyPage } from './ManagementPolicyPage'

const wrapper = ({ children }: PropsWithChildren) => {
  const sdk = {
    refresh: async () => ({ accessToken: 'access', roleActivationRequired: false }),
    getBootstrap: async () => ({
      user: { id: '7', tenantId: '9' },
      permissions: ['system:management-policy:read', 'system:management-policy:manage'],
      fieldPolicies: {}, activeRoleContexts: [], apps: [], menus: [], routes: [], actions: [],
    }),
  } as unknown as Rbac3Client
  const api: FeatureApiClient = { request: async <T,>() => [{
    policyId: '101', policyCode: 'ORG_ADMIN', name: '组织管理员', status: 'ACTIVE',
    validFrom: '2026-08-01T00:00:00Z', validTo: null,
    restrictions: { maximumRiskLevel: 'HIGH', requiredAuthenticationStrength: 'MFA' },
    subjects: [{ type: 'ROLE', id: '1' }], scopes: [{ type: 'ORG', referenceId: '2' }],
    activationRootRoleIds: ['3'], operations: ['ASSIGN', 'REVOKE'], version: 4,
  }] as T }
  return (
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <Rbac3Provider client={sdk} accessTokenStore={new InMemoryAccessTokenStore()}>
        <FeatureApiProvider client={api}>{children}</FeatureApiProvider>
      </Rbac3Provider>
    </QueryClientProvider>
  )
}

describe('management policy page', () => {
  it('shows one complete policy with all four authorization sets', async () => {
    render(<ManagementPolicyPage />, { wrapper })
    await waitFor(() => expect(screen.getByText('ORG_ADMIN')).toBeInTheDocument())
    expect(screen.getByText('ROLE:1')).toBeInTheDocument()
    expect(screen.getByText('ORG:2')).toBeInTheDocument()
    expect(screen.getByText('3')).toBeInTheDocument()
    expect(screen.getByText('ASSIGN, REVOKE')).toBeInTheDocument()
  })
})
