import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {type Rbac3Client, Rbac3Provider} from '@egon-cola/rbac3-react-sdk'
import {render, screen, waitFor} from '@testing-library/react'
import type {PropsWithChildren} from 'react'
import {describe, expect, it, vi} from 'vitest'
import {type FeatureApiClient, FeatureApiProvider} from '../shared/FeatureApi'
import {OrgPositionSnapshotPage} from './OrgPositionSnapshotPage'
import {UserDirectoryPage} from './UserDirectoryPage'
import {directoryApi} from './directory.api'

const wrapper = ({ children }: PropsWithChildren) => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const sdk = {
    getAbout: async () => ({
        user: {id: '7', tenantId: '42', identitySub: 'directory-test', status: 'ACTIVE'},
      permissions: ['system:user:read', 'system:directory:sync'],
        fieldPolicies: {}, activeRoleContexts: [], apps: [], menus: [], routes: [], actions: [],
        defaultApplicationCode: null, defaultRoute: null, authVersion: 1, policyVersion: 1,
    }),
  } as unknown as Rbac3Client
  const api: FeatureApiClient = {
    request: async <T,>(path: string) => (path.includes('/users/')
      ? {
          userId: '9007199254740999', username: 'alice', displayName: 'Alice',
          status: 'ACTIVE', authVersion: 3, directorySnapshotVersion: 11,
        }
      : { snapshotId: '500', outcome: 'ACTIVATED', counts: {}, affectedUserCount: 1 }) as T,
  }
  return (
    <QueryClientProvider client={queryClient}>
        <Rbac3Provider client={sdk}>
        <FeatureApiProvider client={api}>{children}</FeatureApiProvider>
      </Rbac3Provider>
    </QueryClientProvider>
  )
}

describe('directory pages', () => {
  it('maps organization and position reads to their IAM controller paths', async () => {
    const request = vi.fn().mockResolvedValue([])
    const api = directoryApi({request})

    await api.organizations('1001')
    await api.positions('1001')
    await api.user('9007199254740999')
    await api.users({query: 'alice', status: 'ACTIVE', page: 0, size: 20})
    await api.createUser({identitySub: 'alice', status: 'ACTIVE'})
    await api.updateUser('9007199254740999', {identitySub: 'alice-2', expectedAuthVersion: 3})
    await api.changeUserStatus('9007199254740999', {status: 'LOCKED', reason: '安全审查', expectedAuthVersion: 4})
    await api.deleteUser('9007199254740999', 5)
    await api.organizationAssignments('9007199254740999')
    await api.positionAssignments('9007199254740999')
    await api.submitSnapshot({
      providerCode: 'directory-provider',
      snapshotVersion: 1,
      checksum: 'checksum-1',
      generatedAt: '2026-08-27T01:00:00Z',
      payload: {organizations: [], positions: [], users: []},
    })

    expect(request).toHaveBeenNthCalledWith(1, '/api/rbac3/v1/iam/organizations', {query: {parentId: '1001'}})
    expect(request).toHaveBeenNthCalledWith(2, '/api/rbac3/v1/iam/positions', {query: {orgUnitId: '1001'}})
    expect(request).toHaveBeenNthCalledWith(3, '/api/rbac3/v1/iam/users/9007199254740999')
    expect(request).toHaveBeenNthCalledWith(4, '/api/rbac3/v1/iam/users', expect.objectContaining({query: {query: 'alice', status: 'ACTIVE', page: 0, size: 20}}))
    expect(request).toHaveBeenNthCalledWith(5, '/api/rbac3/v1/iam/users', expect.objectContaining({method: 'POST'}))
    expect(request).toHaveBeenNthCalledWith(6, '/api/rbac3/v1/iam/users/9007199254740999', expect.objectContaining({method: 'PUT'}))
    expect(request).toHaveBeenNthCalledWith(7, '/api/rbac3/v1/iam/users/9007199254740999/status', expect.objectContaining({method: 'PUT'}))
    expect(request).toHaveBeenNthCalledWith(8, '/api/rbac3/v1/iam/users/9007199254740999', {method: 'DELETE', query: {expectedAuthVersion: 5}})
    expect(request).toHaveBeenNthCalledWith(9, '/api/rbac3/v1/iam/users/9007199254740999/organizations')
    expect(request).toHaveBeenNthCalledWith(10, '/api/rbac3/v1/iam/users/9007199254740999/positions')
    expect(request).toHaveBeenNthCalledWith(11, '/api/rbac3/v1/internal/directory-snapshots', expect.objectContaining({method: 'POST'}))
  })

  it('keeps user ids as strings and shows the source snapshot version', async () => {
    render(<UserDirectoryPage initialUserId="9007199254740999" />, { wrapper })

    await waitFor(() => expect(screen.getAllByText('alice').length).toBeGreaterThan(0))
    expect(screen.getByText('9007199254740999')).toBeInTheDocument()
    expect(screen.getByText('11')).toBeInTheDocument()
  })

  it('states that directory snapshots are immutable versions', async () => {
    render(<OrgPositionSnapshotPage />, { wrapper })

    await waitFor(() => expect(screen.getByText(/不可变/)).toBeInTheDocument())
    expect(screen.getByRole('spinbutton', { name: '快照版本' })).toBeInTheDocument()
  })
})
