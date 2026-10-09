import {cleanup, fireEvent, render, screen, waitFor} from '@testing-library/react'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {MemoryRouter} from 'react-router-dom'
import {ClientListPage} from './ClientListPage'

const state = vi.hoisted(() => ({
    permissions: [] as string[],
    request: vi.fn(),
}))

vi.mock('../../auth/AuthContext', () => ({
    useAuth: () => ({
        loading: false,
        bootstrap: {
            user: {id: 'admin-1', identitySub: 'admin-sub', tenantId: '1001', status: 'ACTIVE'},
            activeRoleContexts: [],
            permissions: state.permissions,
            apps: [], menus: [], routes: [], actions: [], fieldPolicies: {},
            defaultApplicationCode: null, defaultRoute: null, authVersion: 1, policyVersion: 1,
        },
    }),
    httpClient: {request: state.request},
}))

const client = {
    appId: 'order-service',
    clientId: 'order-service-prod',
    clientName: 'Order Service',
    clientType: 'CONFIDENTIAL',
    purpose: 'SERVICE',
    status: 'ACTIVE',
    pkceRequired: false,
    accessTokenTtlSeconds: 900,
    refreshTokenTtlSeconds: 0,
    redirectUris: [],
    resourceUris: ['https://api.example.com/orders'],
    allowedScopes: ['service:authorization:decide'],
    issuer: 'https://auth.example/tenants/1001',
    secretHint: '••••6789',
    secretStatus: 'ACTIVE',
    version: 3,
    createdAt: '2026-08-22T01:00:00Z',
    updatedAt: '2026-08-22T01:00:00Z',
}

const renderPage = (initialEntry = '/clients') => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}})
    const rendered = render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[initialEntry]}>
                <ClientListPage/>
            </MemoryRouter>
        </QueryClientProvider>,
    )
    return {queryClient, ...rendered}
}

beforeEach(() => {
    state.permissions = [
        'tianquan-shoubing:oauth-client:read',
        'tianquan-shoubing:oauth-client:create',
        'tianquan-shoubing:oauth-client:update',
        'tianquan-shoubing:resource-server:grant',
        'system:platform:admin',
    ]
    state.request.mockReset().mockImplementation((path: string, options?: RequestInit) => {
        if (path.startsWith('/api/v1/tianquan-shoubing/clients?') && !options) {
            return Promise.resolve({content: [client], page: 0, size: 20, totalElements: 1, totalPages: 1})
        }
        if (path === '/api/v1/tianquan-shoubing/clients?realm=tenant%3A1001' && options?.method === 'POST') {
            return Promise.resolve({
                clientId: client.clientId,
                appId: client.appId,
                clientName: client.clientName,
                clientType: client.clientType,
                purpose: client.purpose,
                status: client.status,
                clientSecret: 'one-time-secret-1',
                secretHint: '••••cret-1',
                version: 1,
                createdAt: '2026-08-22T01:00:00Z',
                issuer: client.issuer,
            })
        }
        if (path === `/api/v1/tianquan-shoubing/clients/${client.clientId}/secret-rotations?realm=tenant%3A1001`) {
            return Promise.resolve({
                clientId: client.clientId,
                appId: client.appId,
                clientSecret: 'one-time-secret-2',
                secretHint: '••••cret-2',
                version: 4,
                rotatedAt: '2026-08-22T02:00:00Z',
                issuer: client.issuer,
            })
        }
        if (path === `/api/v1/tianquan-shoubing/clients/${client.clientId}?realm=tenant%3A1001` && options?.method === 'PATCH') {
            return Promise.resolve({...client, clientName: 'Order Service Updated', version: 4})
        }
        return Promise.reject(new Error(`Unexpected request: ${path}`))
    })
})

afterEach(() => {
    cleanup()
    localStorage.clear()
    sessionStorage.clear()
})

describe('OAuth client secret administration', () => {
    it('renders page metadata and opens a safe detail drawer', async () => {
        renderPage('/clients?page=0&size=20')
        await waitFor(() => expect(screen.getByText('Order Service')).toBeInTheDocument())
        expect(screen.getByText('共 1 条')).toBeInTheDocument()
        expect(state.request).toHaveBeenCalledWith('/api/v1/tianquan-shoubing/clients?page=0&size=20&realm=tenant%3A1001')

        fireEvent.click(screen.getByText('Order Service'))
        expect(screen.getByRole('dialog')).toHaveTextContent('order-service-prod')
        expect(screen.queryByText('raw-secret')).not.toBeInTheDocument()
    })

    it('invalidates the client list after an update', async () => {
        const {queryClient} = renderPage()
        const invalidateQueries = vi.spyOn(queryClient, 'invalidateQueries')
        await waitFor(() => expect(screen.getByText('Order Service')).toBeInTheDocument())

        fireEvent.click(screen.getByText('Order Service'))
        fireEvent.click(screen.getByRole('button', {name: /编辑/}))
        fireEvent.change(screen.getByLabelText('名称'), {target: {value: 'Order Service Updated'}})
        fireEvent.change(screen.getByLabelText('Allowed Scopes（逗号分隔）'), {
            target: {value: 'orders:read, orders:write'},
        })
        fireEvent.click(screen.getByRole('button', {name: /确定|OK/}))

        await waitFor(() => expect(state.request).toHaveBeenCalledWith(
            `/api/v1/tianquan-shoubing/clients/${client.clientId}?realm=tenant%3A1001`,
            expect.objectContaining({
                method: 'PATCH',
                body: expect.stringContaining('"allowedScopes":["orders:read","orders:write"]'),
            }),
        ))
        expect(invalidateQueries).toHaveBeenCalledWith(expect.objectContaining({queryKey: ['tianquan-shoubing', 'clients']}))
    })

    it('uses the selected platform realm for an authorized operator and displays the issuer', async () => {
        renderPage()
        await waitFor(() => expect(state.request).toHaveBeenCalledWith(
            '/api/v1/tianquan-shoubing/clients?realm=tenant%3A1001',
        ))

        fireEvent.mouseDown(screen.getByRole('combobox', {name: 'Issuer Realm'}))
        fireEvent.click(screen.getByText('平台管理 (platform)'))
        fireEvent.click(screen.getByRole('button', {name: /查\s*询/}))

        await waitFor(() => expect(state.request).toHaveBeenCalledWith(
            '/api/v1/tianquan-shoubing/clients?page=0&size=20&realm=platform',
        ))
        fireEvent.click(await screen.findByText('Order Service'))
        expect(screen.getAllByText(client.issuer).length).toBeGreaterThan(0)
    })

    it('does not honor a platform realm in the URL without platform permission', async () => {
        state.permissions = ['tianquan-shoubing:oauth-client:read']
        renderPage('/clients?realm=platform')

        await waitFor(() => expect(state.request).toHaveBeenCalledWith(
            '/api/v1/tianquan-shoubing/clients?realm=tenant%3A1001',
        ))
        expect(state.request).not.toHaveBeenCalledWith('/api/v1/tianquan-shoubing/clients?realm=platform')
    })

    it('shows App ID and one-time create credentials without persisting the secret', async () => {
        renderPage()
        await waitFor(() => expect(screen.getByText('Order Service')).toBeInTheDocument())

        fireEvent.click(screen.getByRole('button', {name: /创建客户端/}))
        expect(screen.getByLabelText('App ID')).toBeInTheDocument()
        fireEvent.change(screen.getByLabelText('App ID'), {target: {value: 'order-service'}})
        fireEvent.change(screen.getByPlaceholderText('唯一标识，如 my-app'), {target: {value: 'order-service-prod'}})
        fireEvent.change(screen.getByPlaceholderText('展示名称'), {target: {value: 'Order Service'}})
        fireEvent.mouseDown(screen.getByRole('combobox', {name: '类型'}))
        fireEvent.click(screen.getByText('CONFIDENTIAL (机器客户端)'))
        fireEvent.mouseDown(screen.getByRole('combobox', {name: '客户端用途'}))
        fireEvent.click(screen.getByText('SERVICE (服务调用)'))
        fireEvent.change(screen.getByPlaceholderText('orders:read, orders:write'), {
            target: {value: 'service:authorization:decide'},
        })
        fireEvent.click(screen.getByRole('button', {name: /确定|OK/}))

        await waitFor(() => expect(screen.getByText('one-time-secret-1')).toBeInTheDocument())
        expect(state.request).toHaveBeenCalledWith(
            '/api/v1/tianquan-shoubing/clients?realm=tenant%3A1001',
            expect.objectContaining({
                method: 'POST',
                body: expect.stringContaining('"purpose":"SERVICE"'),
            }),
        )
        expect(JSON.parse(String(state.request.mock.calls.find((call) => call[1]?.method === 'POST')?.[1]?.body)))
            .toMatchObject({allowedScopes: ['service:authorization:decide']})
        expect(screen.getAllByText('order-service').length).toBeGreaterThan(0)
        expect(screen.getAllByText('order-service-prod').length).toBeGreaterThan(0)
        expect(localStorage.length).toBe(0)
        expect(sessionStorage.length).toBe(0)
        expect(window.location.href).not.toContain('one-time-secret-1')
        fireEvent.click(screen.getByRole('button', {name: /关.*闭/}))
        await waitFor(() => expect(screen.queryByText('one-time-secret-1')).not.toBeInTheDocument())
    })

    it('rotates with the displayed version and replaces the old hint only after success', async () => {
        renderPage()
        await waitFor(() => expect(screen.getByText('Order Service')).toBeInTheDocument())
        fireEvent.click(screen.getByText('Order Service'))
        fireEvent.click(screen.getByRole('button', {name: '轮换 Secret'}))
        fireEvent.click(screen.getByRole('button', {name: /确定|OK/}))

        await waitFor(() => expect(screen.getByText('one-time-secret-2')).toBeInTheDocument())
        expect(state.request).toHaveBeenCalledWith(
            `/api/v1/tianquan-shoubing/clients/${client.clientId}/secret-rotations?realm=tenant%3A1001`,
            expect.objectContaining({
                method: 'POST',
                body: JSON.stringify({expectedVersion: client.version}),
            }),
        )
    })

    it('does not render management actions without the matching permission', async () => {
        state.permissions = ['tianquan-shoubing:oauth-client:read']
        renderPage()
        await waitFor(() => expect(screen.getByText('Order Service')).toBeInTheDocument())
        expect(screen.queryByRole('button', {name: '创建客户端'})).not.toBeInTheDocument()
        fireEvent.click(screen.getByText('Order Service'))
        expect(screen.queryByRole('button', {name: '轮换 Secret'})).not.toBeInTheDocument()
    })
})
