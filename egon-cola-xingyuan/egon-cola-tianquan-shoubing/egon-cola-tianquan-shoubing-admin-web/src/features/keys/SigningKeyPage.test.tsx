import {cleanup, fireEvent, render, screen, waitFor} from '@testing-library/react'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {MemoryRouter} from 'react-router-dom'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {SigningKeyPage} from './SigningKeyPage'

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

const key = {
    kid: 'tenant-1001-2026-01',
    algorithm: 'RS256',
    publicJwk: '{"kty":"RSA","kid":"tenant-1001-2026-01"}',
    status: 'PUBLISHED',
    runtimeServing: false,
    version: 2,
    createdAt: '2026-08-27T01:00:00Z',
    updatedAt: '2026-08-27T01:00:00Z',
    issuer: 'https://auth.example/tenants/1001',
}

const renderPage = (initialEntry = '/signing-keys') => render(
    <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}, mutations: {retry: false}}})}>
        <MemoryRouter initialEntries={[initialEntry]}>
            <SigningKeyPage/>
        </MemoryRouter>
    </QueryClientProvider>,
)

beforeEach(() => {
    state.permissions = [
        'tianquan-shoubing:signing-key:read',
        'tianquan-shoubing:signing-key:activate',
        'tianquan-shoubing:signing-key:publish',
        'system:platform:admin',
    ]
    state.request.mockReset().mockImplementation((path: string, options?: RequestInit) => {
        if (path === '/api/v1/tianquan-shoubing/signing-keys?realm=tenant%3A1001' && !options) {
            return Promise.resolve([key])
        }
        if (path === '/api/v1/tianquan-shoubing/signing-keys?realm=platform' && !options) {
            return Promise.resolve([key])
        }
        if (path === '/api/v1/tianquan-shoubing/signing-keys/tenant-1001-2026-01/activate?expectedVersion=2&realm=tenant%3A1001') {
            return Promise.resolve({...key, status: 'ACTIVE', runtimeServing: true, version: 3})
        }
        return Promise.reject(new Error('Unexpected request: ' + path))
    })
})

afterEach(cleanup)

describe('Signing key realm management', () => {
    it('loads and displays only the selected tenant issuer without private-key material', async () => {
        renderPage()

        expect(await screen.findByText(key.kid)).toBeInTheDocument()
        expect(screen.getByText(key.issuer)).toBeInTheDocument()
        expect(screen.queryByText(/privateKey|PRIVATE KEY/)).not.toBeInTheDocument()
        expect(state.request).toHaveBeenCalledWith(
            '/api/v1/tianquan-shoubing/signing-keys?realm=tenant%3A1001',
        )
    })

    it('uses platform realm only when the authenticated permission permits selection', async () => {
        renderPage()

        await screen.findByText(key.kid)
        fireEvent.mouseDown(screen.getByRole('combobox', {name: 'Issuer Realm'}))
        fireEvent.click(screen.getByText('平台管理 (platform)'))

        await waitFor(() => expect(state.request).toHaveBeenCalledWith(
            '/api/v1/tianquan-shoubing/signing-keys?realm=platform',
        ))
    })

    it('binds activation to the same issuer realm and expected version', async () => {
        renderPage()

        await screen.findByText(key.kid)
        fireEvent.click(screen.getByRole('button', {name: /激活/}))

        await waitFor(() => expect(state.request).toHaveBeenCalledWith(
            '/api/v1/tianquan-shoubing/signing-keys/tenant-1001-2026-01/activate?expectedVersion=2&realm=tenant%3A1001',
            expect.objectContaining({method: 'POST'}),
        ))
    })
})
