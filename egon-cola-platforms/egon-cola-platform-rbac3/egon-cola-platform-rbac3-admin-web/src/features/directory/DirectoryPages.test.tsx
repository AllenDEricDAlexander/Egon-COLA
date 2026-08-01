import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { InMemoryAccessTokenStore, Rbac3Provider, type Rbac3Client } from '@egon-cola/rbac3-react-sdk'
import { render, screen, waitFor } from '@testing-library/react'
import type { PropsWithChildren } from 'react'
import { describe, expect, it } from 'vitest'
import { FeatureApiProvider, type FeatureApiClient } from '../shared/FeatureApi'
import { OrgPositionSnapshotPage } from './OrgPositionSnapshotPage'
import { UserDirectoryPage } from './UserDirectoryPage'

const wrapper = ({ children }: PropsWithChildren) => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const sdk = {
    refresh: async () => ({ accessToken: 'access', roleActivationRequired: false }),
    getBootstrap: async () => ({
      user: { id: '7', tenantId: '42' },
      permissions: ['system:user:read', 'system:directory:sync'],
      fieldPolicies: {},
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
      <Rbac3Provider client={sdk} accessTokenStore={new InMemoryAccessTokenStore()}>
        <FeatureApiProvider client={api}>{children}</FeatureApiProvider>
      </Rbac3Provider>
    </QueryClientProvider>
  )
}

describe('directory pages', () => {
  it('keeps user ids as strings and shows the source snapshot version', async () => {
    render(<UserDirectoryPage initialUserId="9007199254740999" />, { wrapper })

    await waitFor(() => expect(screen.getByText('alice')).toBeInTheDocument())
    expect(screen.getByText('9007199254740999')).toBeInTheDocument()
    expect(screen.getByText('11')).toBeInTheDocument()
  })

  it('states that directory snapshots are immutable versions', async () => {
    render(<OrgPositionSnapshotPage />, { wrapper })

    await waitFor(() => expect(screen.getByText(/不可变/)).toBeInTheDocument())
    expect(screen.getByRole('spinbutton', { name: '快照版本' })).toBeInTheDocument()
  })
})
