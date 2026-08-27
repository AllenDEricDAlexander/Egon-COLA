import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {render, screen, waitFor} from '@testing-library/react'
import type {PropsWithChildren} from 'react'
import {MemoryRouter} from 'react-router-dom'
import {type Rbac3Client, Rbac3Provider} from '@egon-cola/rbac3-react-sdk'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {FeatureApiProvider, type FeatureApiClient} from '../shared/FeatureApi'
import {OrganizationPage} from './OrganizationPage'

const state = vi.hoisted(() => ({request: vi.fn()}))

const organization = {
    orgUnitId: '1001',
    snapshotId: null,
    type: 'DEPARTMENT',
    code: 'hq',
    name: '总部',
    parentId: null,
    path: 'hq',
    depth: 0,
    status: 'ACTIVE',
}

const wrapper = ({children}: PropsWithChildren) => {
    const sdk = {
        getAbout: async () => ({
            user: {id: '7', tenantId: '42', identitySub: 'organization-test', status: 'ACTIVE'},
            permissions: ['system:organization:read'],
            fieldPolicies: {}, activeRoleContexts: [], apps: [], menus: [], routes: [], actions: [],
            defaultApplicationCode: null, defaultRoute: null, authVersion: 1, policyVersion: 1,
        }),
    } as unknown as Rbac3Client
    const api: FeatureApiClient = {request: state.request}
    return (
        <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
            <Rbac3Provider client={sdk}>
                <FeatureApiProvider client={api}>
                    <MemoryRouter initialEntries={['/iam/organizations']}>
                        {children}
                    </MemoryRouter>
                </FeatureApiProvider>
            </Rbac3Provider>
        </QueryClientProvider>
    )
}

beforeEach(() => {
    state.request.mockReset().mockImplementation((path: string) => {
        if (path === '/api/rbac3/v1/iam/organizations') return Promise.resolve([organization])
        return Promise.reject(new Error(`Unexpected request: ${path}`))
    })
})

afterEach(() => vi.clearAllMocks())

describe('organization administration page', () => {
    it('renders organization data and never falls back to user detail lookup', async () => {
        render(<OrganizationPage/>, {wrapper})

        await waitFor(() => expect(screen.getByText('总部')).toBeInTheDocument())
        expect(screen.getByText('组织')).toBeInTheDocument()
        expect(state.request).toHaveBeenCalledWith(
            '/api/rbac3/v1/iam/organizations',
            expect.anything(),
        )
        expect(state.request.mock.calls.some(([path]) => String(path).includes('/directory/users/'))).toBe(false)
    })
})
