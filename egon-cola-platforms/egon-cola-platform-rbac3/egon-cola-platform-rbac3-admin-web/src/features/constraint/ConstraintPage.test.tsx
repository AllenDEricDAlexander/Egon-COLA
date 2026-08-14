import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {type Rbac3Client, Rbac3Provider} from '@egon-cola/rbac3-react-sdk'
import {render, screen, waitFor} from '@testing-library/react'
import type {PropsWithChildren} from 'react'
import {describe, expect, it} from 'vitest'
import {type FeatureApiClient, FeatureApiProvider} from '../shared/FeatureApi'
import {ConstraintPage, validateDsdRoleSelection} from './ConstraintPage'

const wrapper = ({ children }: PropsWithChildren) => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const sdk = {
    getBootstrap: async () => ({
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
