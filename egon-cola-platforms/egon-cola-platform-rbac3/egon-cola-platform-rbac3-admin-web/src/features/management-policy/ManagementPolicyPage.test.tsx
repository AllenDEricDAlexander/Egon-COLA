import {type Rbac3Client, Rbac3Provider} from '@egon-cola/rbac3-react-sdk'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {render, screen, waitFor} from '@testing-library/react'
import type {PropsWithChildren} from 'react'
import {describe, expect, it, vi} from 'vitest'
import {type FeatureApiClient, FeatureApiProvider} from '../shared/FeatureApi'
import {ManagementPolicyPage} from './ManagementPolicyPage'
import {managementPolicyApi} from './managementPolicy.api'

const wrapper = ({ children }: PropsWithChildren) => {
  const sdk = {
    getAbout: async () => ({
        user: {id: '7', tenantId: '9', identitySub: 'policy-test', status: 'ACTIVE'},
      permissions: ['system:management-policy:read', 'system:management-policy:manage'],
      fieldPolicies: {}, activeRoleContexts: [], apps: [], menus: [], routes: [], actions: [],
        defaultApplicationCode: null, defaultRoute: null, authVersion: 1, policyVersion: 1,
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
        <Rbac3Provider client={sdk}>
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

  it('maps policy detail and capability target reads to their controller paths', async () => {
    const request = vi.fn().mockResolvedValue({})
    const api = managementPolicyApi({request})

    await api.get('101')
    await api.capabilities()
    await api.manageableUsers()
    await api.manageableRoles()

    expect(request).toHaveBeenNthCalledWith(1, '/api/rbac3/v1/management-policies/101')
    expect(request).toHaveBeenNthCalledWith(2, '/api/rbac3/v1/management-capabilities/me')
    expect(request).toHaveBeenNthCalledWith(3, '/api/rbac3/v1/manageable-users')
    expect(request).toHaveBeenNthCalledWith(4, '/api/rbac3/v1/manageable-roles')
  })
})
