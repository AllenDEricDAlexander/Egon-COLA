import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {fireEvent, render, screen, waitFor} from '@testing-library/react'
import type {PropsWithChildren} from 'react'
import {MemoryRouter} from 'react-router-dom'
import {type Rbac3Client, Rbac3Provider} from '@egon-cola/tianquan-jianshen-react-sdk'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {FeatureApiProvider, type FeatureApiClient} from '../shared/FeatureApi'
import {UserRelationsPanel} from './UserRelationsPanel'

const state = vi.hoisted(() => ({request: vi.fn()}))

const organizationAssignment = {
  assignmentId: 'org-assignment-1',
  userId: '7',
  orgUnitId: '1001',
  status: 'ACTIVE',
  validFrom: '2026-08-29T00:00:00Z',
  validTo: null,
  sourceType: 'MANUAL',
  sourceId: 'MANUAL:org-assignment-1',
  reason: 'onboarding',
  ticketNo: 'IAM-ORG-1',
  version: 4,
}

const positionAssignment = {
  ...organizationAssignment,
  assignmentId: 'position-assignment-1',
  positionId: '2001',
  primaryAssignment: true,
  sourceId: 'MANUAL:position-assignment-1',
  version: 5,
}

const wrapper = ({children}: PropsWithChildren) => {
  const sdk = {
    getAbout: async () => ({
      user: {id: '7', tenantId: '42', identitySub: 'relations-test', status: 'ACTIVE'},
      permissions: [
        'system:user-organization:read',
        'system:user-organization:manage',
        'system:user-position:read',
        'system:user-position:manage',
        'system:role-assignment:read',
      ],
      fieldPolicies: {}, activeRoleContexts: [], apps: [], menus: [], routes: [], actions: [],
      defaultApplicationCode: null, defaultRoute: null, authVersion: 3, policyVersion: 1,
    }),
  } as unknown as Rbac3Client
  const api: FeatureApiClient = {request: state.request}
  return (
    <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
      <Rbac3Provider client={sdk}>
        <FeatureApiProvider client={api}>
          <MemoryRouter initialEntries={['/iam/users/7']}>{children}</MemoryRouter>
        </FeatureApiProvider>
      </Rbac3Provider>
    </QueryClientProvider>
  )
}

beforeEach(() => {
  state.request.mockReset().mockImplementation((path: string, request?: {method?: string}) => {
    if (path === '/api/tianquan-jianshen/v1/iam/users/7/organizations' && !request?.method) return Promise.resolve([organizationAssignment])
    if (path === '/api/tianquan-jianshen/v1/iam/users/7/positions' && !request?.method) return Promise.resolve([positionAssignment])
    if (request?.method === 'POST' || request?.method === 'DELETE') return Promise.resolve(null)
    return Promise.reject(new Error(`Unexpected request: ${path}`))
  })
})

afterEach(() => vi.clearAllMocks())

describe('user relation panel', () => {
  it('loads organization and position relations, supports revoke, and links to role assignments', async () => {
    render(<UserRelationsPanel userId="7" authVersion={3}/>, {wrapper})

    await waitFor(() => expect(screen.getByText('1001')).toBeInTheDocument())
    expect(state.request).toHaveBeenCalledWith('/api/tianquan-jianshen/v1/iam/users/7/positions', {})
  })

  it('maps trimmed relation form values and empty validTo to null', async () => {
    render(<UserRelationsPanel userId="7" authVersion={3}/>, {wrapper})

    await waitFor(() => expect(screen.getByText('1001')).toBeInTheDocument())
    fireEvent.change(screen.getByRole('textbox', {name: '组织 ID'}), {target: {value: ' 1002 '}})
    fireEvent.change(screen.getByRole('textbox', {name: '生效时间'}), {target: {value: '2026-08-30T00:00:00Z'}})
    fireEvent.change(screen.getByRole('textbox', {name: '原因'}), {target: {value: '  transfer '}})
    fireEvent.change(screen.getByRole('textbox', {name: '外部工单号'}), {target: {value: ' IAM-3 '}})
    fireEvent.click(screen.getByRole('button', {name: '保存组织关系'}))

    await waitFor(() => expect(state.request).toHaveBeenCalledWith(
      '/api/tianquan-jianshen/v1/iam/users/7/organizations',
      expect.objectContaining({
        method: 'POST',
        body: {
          orgUnitId: '1002',
          validFrom: '2026-08-30T00:00:00Z',
          validTo: null,
          reason: 'transfer',
          ticketNo: 'IAM-3',
        },
      }),
    ))
  })

  it('keeps revoke on the versioned relation endpoint', async () => {
    const request = vi.fn().mockResolvedValue(null)
    const api = (await import('./directory.api')).directoryApi({request})

    await api.revokeOrganization('7', 'org-assignment-1', 4)
    await api.revokePosition('7', 'position-assignment-1', 5)

    expect(request).toHaveBeenNthCalledWith(1, '/api/tianquan-jianshen/v1/iam/users/7/organizations/org-assignment-1', {
      method: 'DELETE', query: {expectedVersion: 4},
    })
    expect(request).toHaveBeenNthCalledWith(2, '/api/tianquan-jianshen/v1/iam/users/7/positions/position-assignment-1', {
      method: 'DELETE', query: {expectedVersion: 5},
    })
  })
})
