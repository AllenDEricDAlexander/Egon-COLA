import {cleanup, fireEvent, render, screen, waitFor} from '@testing-library/react'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {MemoryRouter} from 'react-router-dom'
import {TenantListPage} from './TenantListPage'

const state = vi.hoisted(() => ({
    permissions: [] as string[],
    request: vi.fn(),
}))

vi.mock('../../auth/AuthContext', () => ({
    useAuth: () => ({
        loading: false,
        bootstrap: {
            user: {id: 'admin-1', identitySub: 'admin-sub', tenantId: 'default', status: 'ACTIVE'},
            activeRoleContexts: [],
            permissions: state.permissions,
            apps: [], menus: [], routes: [], actions: [], fieldPolicies: {},
            defaultApplicationCode: null, defaultRoute: null, authVersion: 1, policyVersion: 1,
        },
    }),
    httpClient: {request: state.request},
}))

const tenant = {
    tenantId: 'tenant-1',
    tenantCode: 'acme',
    tenantName: 'Acme',
    status: 'ACTIVE',
    settings: {region: 'cn'},
    version: 2,
    createdAt: '2026-08-22T01:00:00Z',
    updatedAt: '2026-08-22T02:00:00Z',
}

const membership = {
    tenantId: 'tenant-1',
    identitySub: 'alice-sub',
    displayName: 'Alice',
    status: 'ACTIVE',
    version: 4,
    updatedAt: '2026-08-22T02:00:00Z',
}

const renderPage = () => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}})
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/tenants']}>
                <TenantListPage/>
            </MemoryRouter>
        </QueryClientProvider>,
    )
}

beforeEach(() => {
    state.permissions = ['idp:tenant:read', 'idp:tenant:manage']
    state.request.mockReset().mockImplementation((path: string, options?: RequestInit) => {
        if (path === '/api/v1/identity/tenants?page=0&size=20' && !options) {
            return Promise.resolve({content: [tenant], page: 0, size: 20, totalElements: 1, totalPages: 1})
        }
        if (path === '/api/v1/identity/tenants' && options?.method === 'POST') return Promise.resolve(tenant)
        if (path === '/api/v1/identity/tenants/tenant-1' && options?.method === 'PATCH') {
            return Promise.resolve({...tenant, tenantName: 'Acme Updated', version: 3})
        }
        if (path === '/api/v1/identity/tenants/tenant-1/members?page=0&size=20') {
            return Promise.resolve({content: [membership], page: 0, size: 20, totalElements: 1, totalPages: 1})
        }
        if (path === '/api/v1/identity/tenants/tenant-1/members/alice-sub' && options?.method === 'PUT') {
            return Promise.resolve(membership)
        }
        return Promise.reject(new Error(`Unexpected request: ${path}`))
    })
})

afterEach(cleanup)

describe('IdP tenant and membership administration', () => {
    it('guards management actions and maps tenant/member mutations to IdP APIs', async () => {
        renderPage()
        await waitFor(() => expect(screen.getByText('Acme')).toBeInTheDocument())

        fireEvent.click(screen.getByRole('button', {name: /创建租户/}))
        fireEvent.change(screen.getByLabelText('租户编码'), {target: {value: 'new-tenant'}})
        fireEvent.change(screen.getByLabelText('租户名称'), {target: {value: 'New Tenant'}})
        fireEvent.click(screen.getAllByRole('button', {name: /确定|OK/})[0])
        await waitFor(() => expect(state.request).toHaveBeenCalledWith(
            '/api/v1/identity/tenants',
            expect.objectContaining({method: 'POST'}),
        ))

        fireEvent.click(screen.getByText('Acme'))
        fireEvent.click(screen.getByRole('button', {name: /成员/}))
        await waitFor(() => expect(screen.getByText('Alice')).toBeInTheDocument())
        fireEvent.click(screen.getByRole('button', {name: /新增.*成员/}))
        fireEvent.change(screen.getByLabelText('Identity Sub'), {target: {value: 'alice-sub'}})
        fireEvent.click(screen.getAllByRole('button', {name: /确定|OK/}).at(-1)!)

        await waitFor(() => expect(state.request).toHaveBeenCalledWith(
            '/api/v1/identity/tenants/tenant-1/members/alice-sub',
            expect.objectContaining({
                method: 'PUT',
                body: JSON.stringify({status: 'ACTIVE'}),
            }),
        ))
        expect(state.request.mock.calls.every(([path]) => !String(path).includes('/rbac3/'))).toBe(true)
    })

    it('hides create, update and membership actions without tenant manage permission', async () => {
        state.permissions = ['idp:tenant:read']
        renderPage()
        await waitFor(() => expect(screen.getByText('Acme')).toBeInTheDocument())
        expect(screen.queryByRole('button', {name: /创建租户/})).not.toBeInTheDocument()
        fireEvent.click(screen.getByText('Acme'))
        expect(screen.queryByRole('button', {name: '编辑'})).not.toBeInTheDocument()
        await waitFor(() => expect(screen.getByRole('button', {name: /成员/})).toBeInTheDocument())
    })
})
