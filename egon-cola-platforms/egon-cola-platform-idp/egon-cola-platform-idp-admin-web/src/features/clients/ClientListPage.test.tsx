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
            user: {id: 'admin-1', identitySub: 'admin-sub', tenantId: 'default', status: 'ACTIVE'},
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
    status: 'ACTIVE',
    pkceRequired: false,
    accessTokenTtlSeconds: 900,
    refreshTokenTtlSeconds: 0,
    redirectUris: [],
    resourceUris: ['https://api.example.com/orders'],
    secretHint: '••••6789',
    secretStatus: 'ACTIVE',
    version: 3,
    createdAt: '2026-08-22T01:00:00Z',
    updatedAt: '2026-08-22T01:00:00Z',
}

const renderPage = () => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}})
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/clients']}>
                <ClientListPage/>
            </MemoryRouter>
        </QueryClientProvider>,
    )
}

beforeEach(() => {
    state.permissions = [
        'idp:oauth-client:read',
        'idp:oauth-client:create',
        'idp:oauth-client:update',
        'idp:resource-server:grant',
    ]
    state.request.mockReset().mockImplementation((path: string, options?: RequestInit) => {
        if (path === '/api/v1/identity/clients' && !options) return Promise.resolve([client])
        if (path === '/api/v1/identity/clients' && options?.method === 'POST') {
            return Promise.resolve({
                clientId: client.clientId,
                appId: client.appId,
                clientName: client.clientName,
                clientType: client.clientType,
                status: client.status,
                clientSecret: 'one-time-secret-1',
                secretHint: '••••cret-1',
                version: 1,
                createdAt: '2026-08-22T01:00:00Z',
            })
        }
        if (path === `/api/v1/identity/clients/${client.clientId}/secret-rotations`) {
            return Promise.resolve({
                clientId: client.clientId,
                appId: client.appId,
                clientSecret: 'one-time-secret-2',
                secretHint: '••••cret-2',
                version: 4,
                rotatedAt: '2026-08-22T02:00:00Z',
            })
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
        fireEvent.click(screen.getByRole('button', {name: /确定|OK/}))

        await waitFor(() => expect(screen.getByText('one-time-secret-1')).toBeInTheDocument())
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
            `/api/v1/identity/clients/${client.clientId}/secret-rotations`,
            expect.objectContaining({
                method: 'POST',
                body: JSON.stringify({expectedVersion: client.version}),
            }),
        )
    })

    it('does not render management actions without the matching permission', async () => {
        state.permissions = ['idp:oauth-client:read']
        renderPage()
        await waitFor(() => expect(screen.getByText('Order Service')).toBeInTheDocument())
        expect(screen.queryByRole('button', {name: '创建客户端'})).not.toBeInTheDocument()
        fireEvent.click(screen.getByText('Order Service'))
        expect(screen.queryByRole('button', {name: '轮换 Secret'})).not.toBeInTheDocument()
    })
})
