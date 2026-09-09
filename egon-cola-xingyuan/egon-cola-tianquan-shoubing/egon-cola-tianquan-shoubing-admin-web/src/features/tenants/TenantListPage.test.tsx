import {cleanup, fireEvent, render, screen, waitFor, within} from '@testing-library/react'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {MemoryRouter} from 'react-router-dom'
import {ConfigProvider} from 'antd'
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
            <ConfigProvider theme={{token: {motion: false}}}>
                <MemoryRouter initialEntries={['/tenants']}>
                    <TenantListPage/>
                </MemoryRouter>
            </ConfigProvider>
        </QueryClientProvider>,
    )
}

beforeEach(() => {
    state.permissions = ['tianquan-shoubing:tenant:read', 'tianquan-shoubing:tenant:manage']
    state.request.mockReset().mockImplementation((path: string, options?: RequestInit) => {
        if (path === '/api/v1/tianquan-shoubing/tenants?page=0&size=20' && !options) {
            return Promise.resolve({content: [tenant], page: 0, size: 20, totalElements: 1, totalPages: 1})
        }
        if (path === '/api/v1/tianquan-shoubing/tenants' && options?.method === 'POST') return Promise.resolve(tenant)
        if (path === '/api/v1/tianquan-shoubing/tenants/tenant-1' && options?.method === 'PATCH') {
            return Promise.resolve({...tenant, tenantName: 'Acme Updated', version: 3})
        }
        if (path === '/api/v1/tianquan-shoubing/tenants/tenant-1/members?page=0&size=20') {
            return Promise.resolve({content: [membership], page: 0, size: 20, totalElements: 1, totalPages: 1})
        }
        if (path === '/api/v1/tianquan-shoubing/tenants/tenant-1/members/alice-sub' && options?.method === 'PUT') {
            return Promise.resolve(membership)
        }
        return Promise.reject(new Error(`Unexpected request: ${path}`))
    })
})

afterEach(cleanup)

describe('Tianquan-Shoubing tenant and membership administration', () => {
    it('prefills tenant editing on first open and again after saving', async () => {
        renderPage()
        fireEvent.click(await screen.findByText('Acme'))
        fireEvent.click(await screen.findByRole('button', {name: /编\s*辑/}))
        const nameInput = await screen.findByLabelText('租户名称')
        const form = within(nameInput.closest('form')!)
        expect(nameInput).toHaveValue('Acme')
        expect(form.getByText('ACTIVE')).toBeInTheDocument()
        expect(form.getByLabelText('Settings JSON')).toHaveValue(JSON.stringify(tenant.settings, null, 2))
        fireEvent.change(nameInput, {target: {value: 'Acme Updated'}})
        fireEvent.click(screen.getByRole('button', {name: /确定|OK/}))
        await waitFor(() => expect(state.request).toHaveBeenCalledWith(
            '/api/v1/tianquan-shoubing/tenants/tenant-1',
            expect.objectContaining({
                method: 'PATCH',
                body: JSON.stringify({tenantName: 'Acme Updated', status: 'ACTIVE', settings: {region: 'cn'}, expectedVersion: 2}),
            }),
        ))
        await waitFor(() => expect(nameInput).not.toBeVisible())
        fireEvent.click(await screen.findByRole('button', {name: /编\s*辑/}))
        const reopened = await screen.findByLabelText('租户名称')
        expect(reopened).toHaveValue('Acme Updated')
        expect(within(reopened.closest('form')!).getByText('ACTIVE')).toBeInTheDocument()
    })

    it('prefills the immutable member subject and current version before editing', async () => {
        renderPage()
        fireEvent.click(await screen.findByText('Acme'))
        fireEvent.click(screen.getByRole('button', {name: /成员/}))
        const row = await screen.findByRole('row', {name: /alice-sub/})
        fireEvent.click(within(row).getByRole('button', {name: /编\s*辑/}))
        const subject = await screen.findByLabelText('Identity Sub')
        const form = within(subject.closest('form')!)
        expect(subject).toHaveValue('alice-sub')
        expect(subject).toBeDisabled()
        expect(form.getByLabelText('成员版本（新建留空）')).toHaveValue(4)
        expect(form.getByText('ACTIVE')).toBeInTheDocument()
        fireEvent.click(screen.getByRole('button', {name: /确定|OK/}))
        await waitFor(() => expect(state.request).toHaveBeenCalledWith(
            '/api/v1/tianquan-shoubing/tenants/tenant-1/members/alice-sub',
            expect.objectContaining({method: 'PUT', body: JSON.stringify({status: 'ACTIVE', expectedVersion: 4})}),
        ))
    })

    it('guards management actions and maps tenant/member mutations to Tianquan-Shoubing APIs', async () => {
        renderPage()
        await waitFor(() => expect(screen.getByText('Acme')).toBeInTheDocument())

        fireEvent.click(screen.getByRole('button', {name: /创建租户/}))
        fireEvent.change(screen.getByLabelText('租户编码'), {target: {value: 'new-tenant'}})
        fireEvent.change(screen.getByLabelText('租户名称'), {target: {value: 'New Tenant'}})
        fireEvent.click(screen.getAllByRole('button', {name: /确定|OK/})[0])
        await waitFor(() => expect(state.request).toHaveBeenCalledWith(
            '/api/v1/tianquan-shoubing/tenants',
            expect.objectContaining({method: 'POST'}),
        ))

        fireEvent.click(screen.getByText('Acme'))
        fireEvent.click(screen.getByRole('button', {name: /成员/}))
        await waitFor(() => expect(screen.getByText('Alice')).toBeInTheDocument())
        fireEvent.click(screen.getByRole('button', {name: /新增.*成员/}))
        fireEvent.change(screen.getByLabelText('Identity Sub'), {target: {value: 'alice-sub'}})
        fireEvent.click(screen.getAllByRole('button', {name: /确定|OK/}).at(-1)!)

        await waitFor(() => expect(state.request).toHaveBeenCalledWith(
            '/api/v1/tianquan-shoubing/tenants/tenant-1/members/alice-sub',
            expect.objectContaining({
                method: 'PUT',
                body: JSON.stringify({status: 'ACTIVE'}),
            }),
        ))
        expect(state.request.mock.calls.every(([path]) => !String(path).includes('/tianquan-jianshen/'))).toBe(true)
    })

    it('hides create, update and membership actions without tenant manage permission', async () => {
        state.permissions = ['tianquan-shoubing:tenant:read']
        renderPage()
        await waitFor(() => expect(screen.getByText('Acme')).toBeInTheDocument())
        expect(screen.queryByRole('button', {name: /创建租户/})).not.toBeInTheDocument()
        fireEvent.click(screen.getByText('Acme'))
        expect(screen.queryByRole('button', {name: '编辑'})).not.toBeInTheDocument()
        await waitFor(() => expect(screen.getByRole('button', {name: /成员/})).toBeInTheDocument())
    })
})
