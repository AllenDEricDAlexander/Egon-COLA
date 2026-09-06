import {type Rbac3Client, Rbac3Provider} from '@egon-cola/rbac3-react-sdk'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {cleanup, fireEvent, render, screen, waitFor} from '@testing-library/react'
import type {PropsWithChildren} from 'react'
import {afterEach, describe, expect, it, vi} from 'vitest'
import {type FeatureApiClient, FeatureApiProvider} from '../shared/FeatureApi'
import {AssignmentListPage} from './AssignmentListPage'
import {assignmentApi} from './assignment.api'

describe('assignment pages', () => {
  afterEach(cleanup)

  it('renders assignment eligibility states and idempotent guarded actions', async () => {
    const request: FeatureApiClient['request'] = async <T,>() => [{
      assignmentId: '9007199254740999', roleId: '81', assignmentType: 'DIRECT',
      status: 'ACTIVE', validFrom: '2026-08-01T00:00:00Z', validTo: null,
      sourceType: 'MANUAL', sourceId: '7', version: 3,
    }] as T
    render(<AssignmentListPage userId="42" />, { wrapper: wrapper(request, ['system:role-assignment:manage']) })

    await waitFor(() => expect(screen.getByText('9007199254740999')).toBeInTheDocument())
    expect(screen.getByText(/授权资格/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /暂\s*停/ })).toBeInTheDocument()
    expect(document.body.textContent).not.toMatch(/排班|轮岗|审批/)
  })

  it('hides mutation actions without permission', async () => {
    const request: FeatureApiClient['request'] = async <T,>() => [{
      assignmentId: '1', roleId: '81', assignmentType: 'DIRECT', status: 'ACTIVE',
      validFrom: '2026-08-01T00:00:00Z', validTo: null, sourceType: 'MANUAL', sourceId: '7', version: 1,
    }] as T
    render(<AssignmentListPage userId="42" />, { wrapper: wrapper(request, []) })
    await waitFor(() => expect(screen.getByText('81')).toBeInTheDocument())
    expect(screen.queryByRole('button', { name: '新增任职资格' })).not.toBeInTheDocument()
  })

  it('keeps the role assignment workbench on the existing IAM controller contract', async () => {
    const request = vi.fn().mockResolvedValue([])
    const api = assignmentApi({request})

    await api.list('9007199254740999')
    await api.create('9007199254740999', {
      roleId: '81',
      validFrom: '2026-08-29T00:00:00Z',
      validTo: null,
      assignmentType: 'DIRECT',
      reason: 'manual',
      ticketNo: 'IAM-1',
      expectedUserAuthVersion: 3,
    }, 'idempotency-1')
    await api.change('9007199254740999', '9001', 'revoke', {
      reason: 'manual revoke',
      ticketNo: 'IAM-2',
      expectedAssignmentVersion: 1,
      expectedUserAuthVersion: 4,
    }, 'idempotency-2')

    expect(request).toHaveBeenNthCalledWith(1, '/api/rbac3/v1/users/9007199254740999/role-assignments')
    expect(request).toHaveBeenNthCalledWith(2, '/api/rbac3/v1/users/9007199254740999/role-assignments', expect.objectContaining({
      method: 'POST',
      headers: {'Idempotency-Key': 'idempotency-1'},
    }))
    expect(request).toHaveBeenNthCalledWith(3, '/api/rbac3/v1/users/9007199254740999/role-assignments/9001/revoke', expect.objectContaining({
      method: 'POST',
      headers: {'Idempotency-Key': 'idempotency-2'},
    }))
  })

  it('uses the target user version for creation and refreshes it before the next mutation', async () => {
    let targetAuthVersion = 7
    const request = vi.fn(async (path: string, options?: {method?: string; body?: unknown}) => {
      if (path === '/api/rbac3/v1/iam/users/42') {
        return {userId: '42', identitySub: 'target-user', status: 'ACTIVE', authVersion: targetAuthVersion}
      }
      if (options?.method === 'POST') {
        targetAuthVersion += 1
        return {assignmentId: 'assignment-1'}
      }
      return [{assignmentId: 'assignment-1', roleId: '81', assignmentType: 'DIRECT', status: 'ACTIVE',
        validFrom: '2026-09-06T00:00:00Z', validTo: null, sourceType: 'MANUAL', sourceId: '7', version: 3}]
    })
    render(<AssignmentListPage userId="42" />, {wrapper: wrapper(request as FeatureApiClient['request'], ['system:role-assignment:manage'])})
    await waitFor(() => expect(screen.getByRole('button', {name: '新增任职资格'})).toBeEnabled())
    fireEvent.click(screen.getByRole('button', {name: '新增任职资格'}))
    const version = await screen.findByRole('spinbutton', {name: '目标用户授权版本'})
    expect(version).toHaveValue('7')
    expect(version).toHaveAttribute('readonly')
    fireEvent.change(screen.getByLabelText('Role ID'), {target: {value: '81'}})
    fireEvent.change(screen.getByLabelText('生效时间'), {target: {value: '2026-09-06T13:00'}})
    fireEvent.click(screen.getByRole('button', {name: '保存资格'}))
    await waitFor(() => expect(request).toHaveBeenCalledWith(
      '/api/rbac3/v1/users/42/role-assignments',
      expect.objectContaining({method: 'POST', body: expect.objectContaining({expectedUserAuthVersion: 7})}),
    ))
    await waitFor(() => expect(screen.getByRole('button', {name: /暂\s*停/})).toBeEnabled())
    fireEvent.click(screen.getByRole('button', {name: /暂\s*停/}))
    fireEvent.click(await screen.findByRole('button', {name: 'OK'}))
    await waitFor(() => expect(request).toHaveBeenCalledWith(
      '/api/rbac3/v1/users/42/role-assignments/assignment-1/suspend',
      expect.objectContaining({method: 'POST', body: expect.objectContaining({expectedAssignmentVersion: 3, expectedUserAuthVersion: 8})}),
    ))
  })

  it('keeps assignment mutations disabled when the target user version cannot be read', async () => {
    const request: FeatureApiClient['request'] = async <T,>(path: string) => {
      if (path === '/api/rbac3/v1/iam/users/42') throw new Error('target lookup failed')
      return [] as T
    }
    render(<AssignmentListPage userId="42" />, {wrapper: wrapper(request, ['system:role-assignment:manage'])})
    await screen.findByText('无法读取目标用户授权版本，请刷新后重试')
    expect(screen.getByRole('button', {name: '新增任职资格'})).toBeDisabled()
  })
})

const wrapper = (request: FeatureApiClient['request'], permissions: readonly string[]) => ({ children }: PropsWithChildren) => {
  const sdk = {
    getAbout: async () => ({
        user: {id: '7', tenantId: '9', identitySub: 'assignment-test', status: 'ACTIVE'},
        permissions,
        fieldPolicies: {},
      activeRoleContexts: [], apps: [], menus: [], routes: [], actions: [],
        defaultApplicationCode: null,
        defaultRoute: null,
        authVersion: 1,
        policyVersion: 1,
    }),
  } as unknown as Rbac3Client
  return (
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <Rbac3Provider client={sdk}>
        <FeatureApiProvider client={{ request }}>{children}</FeatureApiProvider>
      </Rbac3Provider>
    </QueryClientProvider>
  )
}
