import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {type Rbac3Client, Rbac3Provider} from '@egon-cola/tianquan-jianshen-react-sdk'
import {render, screen, waitFor} from '@testing-library/react'
import type {PropsWithChildren} from 'react'
import {describe, expect, it, vi} from 'vitest'
import {type FeatureApiClient, FeatureApiProvider} from '../shared/FeatureApi'
import {ConstraintPage, validateDsdRoleSelection} from './ConstraintPage'
import {constraintApi} from './constraint.api'

const wrapper = ({ children }: PropsWithChildren) => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const sdk = {
    getAbout: async () => ({
        user: {id: '7', tenantId: '42', identitySub: 'constraint-test', status: 'ACTIVE'},
      permissions: ['system:authorization-constraint:read'],
        fieldPolicies: {}, activeRoleContexts: [], apps: [], menus: [], routes: [], actions: [],
        defaultApplicationCode: null, defaultRoute: null, authVersion: 1, policyVersion: 1,
    }),
  } as unknown as Rbac3Client
  const api: FeatureApiClient = {
    request: async <T,>() => [{ setId: '9', setCode: 'cashier-maker-checker', constraintType: 'DSD', applicationId: '71', maximumActiveRoles: 1, roleIds: ['1', '2'], status: 'ACTIVE', version: 3 }] as T,
  }
  return (
    <QueryClientProvider client={queryClient}>
        <Rbac3Provider client={sdk}>
        <FeatureApiProvider client={api}>{children}</FeatureApiProvider>
      </Rbac3Provider>
    </QueryClientProvider>
  )
}

describe('constraint page', () => {
  it('uses the IAM policies controller root for every policy collection', async () => {
    const request = vi.fn().mockResolvedValue([])
    const api = constraintApi({request})

    await api.sodSets()
    await api.dataRules()
    await api.fieldRules()
    await api.operationSodRules()
    await api.createSod({
      setCode: 'maker-checker', constraintType: 'SSD', applicationId: '71', maximumActiveRoles: 1,
      memberRoleIds: ['1', '2'], validFrom: '2026-08-29T00:00:00Z', validTo: null, expectedVersion: 0,
    })
    await api.updateSod('9', {
      setCode: 'maker-checker', constraintType: 'DSD', applicationId: '71', maximumActiveRoles: 1,
      memberRoleIds: ['1', '2'], validFrom: '2026-08-29T00:00:00Z', validTo: null, expectedVersion: 3,
    })
    await api.savePrerequisites('2', {groupCode: 'approval', matchMode: 'ALL', prerequisiteRoleIds: ['1'], expectedRoleVersion: 4})
    await api.saveCardinality('2', {scopeType: 'ORG', maximumActive: 2, validFrom: '2026-08-29T00:00:00Z', validTo: null, expectedVersion: 5})
    await api.createDataRule({
      applicationId: '71', roleId: '2', permissionId: '91', scopeType: 'ORG', directorySnapshotVersion: null,
      references: [{referenceType: 'ORG', referenceId: '1001'}], validFrom: '2026-08-29T00:00:00Z', validTo: null, expectedVersion: 0,
    })
    await api.updateDataRule('11', {
      applicationId: '71', roleId: '2', permissionId: '91', scopeType: 'ORG', directorySnapshotVersion: null,
      references: [{referenceType: 'ORG', referenceId: '1001'}], validFrom: '2026-08-29T00:00:00Z', validTo: null, expectedVersion: 2,
    })
    await api.createFieldRule({applicationId: '71', roleId: '2', permissionId: '91', fieldDefinitionId: '81', accessLevel: 'READ', validFrom: '2026-08-29T00:00:00Z', validTo: null, expectedVersion: 0})
    await api.updateFieldRule('12', {applicationId: '71', roleId: '2', permissionId: '91', fieldDefinitionId: '81', accessLevel: 'WRITE', validFrom: '2026-08-29T00:00:00Z', validTo: null, expectedVersion: 2})
    await api.createOperationSodRule({applicationCode: 'orders', businessResource: 'invoice', priorActionCode: 'CREATE', forbiddenLaterActionCode: 'APPROVE', lookbackFrom: null, validFrom: '2026-08-29T00:00:00Z', validTo: null, expectedVersion: 0})
    await api.updateOperationSodRule('13', {applicationCode: 'orders', businessResource: 'invoice', priorActionCode: 'CREATE', forbiddenLaterActionCode: 'APPROVE', lookbackFrom: null, validFrom: '2026-08-29T00:00:00Z', validTo: null, expectedVersion: 2})

    expect(request).toHaveBeenNthCalledWith(1, '/api/rbac3/v1/iam/policies/sod-sets')
    expect(request).toHaveBeenNthCalledWith(2, '/api/rbac3/v1/iam/policies/data-rules')
    expect(request).toHaveBeenNthCalledWith(3, '/api/rbac3/v1/iam/policies/field-rules')
    expect(request).toHaveBeenNthCalledWith(4, '/api/rbac3/v1/iam/policies/operation-sod-rules')
    expect(request).toHaveBeenCalledWith('/api/rbac3/v1/iam/policies/sod-sets', expect.objectContaining({method: 'POST'}))
    expect(request).toHaveBeenCalledWith('/api/rbac3/v1/iam/policies/sod-sets/9', expect.objectContaining({method: 'PUT'}))
    expect(request).toHaveBeenCalledWith('/api/rbac3/v1/iam/policies/roles/2/prerequisite-groups', expect.objectContaining({method: 'POST'}))
    expect(request).toHaveBeenCalledWith('/api/rbac3/v1/iam/policies/roles/2/cardinality', expect.objectContaining({method: 'PUT'}))
    expect(request).toHaveBeenCalledWith('/api/rbac3/v1/iam/policies/data-rules', expect.objectContaining({method: 'POST'}))
    expect(request).toHaveBeenCalledWith('/api/rbac3/v1/iam/policies/data-rules/11', expect.objectContaining({method: 'PUT'}))
    expect(request).toHaveBeenCalledWith('/api/rbac3/v1/iam/policies/field-rules', expect.objectContaining({method: 'POST'}))
    expect(request).toHaveBeenCalledWith('/api/rbac3/v1/iam/policies/field-rules/12', expect.objectContaining({method: 'PUT'}))
    expect(request).toHaveBeenCalledWith('/api/rbac3/v1/iam/policies/operation-sod-rules', expect.objectContaining({method: 'POST'}))
    expect(request).toHaveBeenCalledWith('/api/rbac3/v1/iam/policies/operation-sod-rules/13', expect.objectContaining({method: 'PUT'}))
  })

  it('renders DSD as an activation-time constraint', async () => {
    render(<ConstraintPage />, { wrapper })
    await waitFor(() => expect(screen.getByText('cashier-maker-checker')).toBeInTheDocument())
    expect(screen.getByRole('columnheader', { name: '激活根角色' })).toBeInTheDocument()
  })

  it('accepts only activation roots from one application', () => {
    expect(validateDsdRoleSelection([
      { roleId: '1', applicationId: '71', roleType: 'ACTIVATION_ROOT' },
      { roleId: '2', applicationId: '71', roleType: 'BUSINESS' },
    ])).toBe('DSD_ROLE_MUST_BE_ACTIVATION_ROOT')
    expect(validateDsdRoleSelection([
      { roleId: '1', applicationId: '71', roleType: 'ACTIVATION_ROOT' },
      { roleId: '3', applicationId: '72', roleType: 'ACTIVATION_ROOT' },
    ])).toBe('DSD_ROLES_MUST_SHARE_APPLICATION')
  })
})
