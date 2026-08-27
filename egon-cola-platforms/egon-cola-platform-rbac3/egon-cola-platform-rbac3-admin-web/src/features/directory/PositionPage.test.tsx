import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {render, screen, waitFor} from '@testing-library/react'
import type {PropsWithChildren} from 'react'
import {MemoryRouter} from 'react-router-dom'
import {type Rbac3Client, Rbac3Provider} from '@egon-cola/rbac3-react-sdk'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {FeatureApiProvider, type FeatureApiClient} from '../shared/FeatureApi'
import {PositionPage} from './PositionPage'

const state = vi.hoisted(() => ({request: vi.fn()}))

const position = {
    positionId: '2001',
    snapshotId: null,
    code: 'ops-admin',
    name: '运营主管',
    orgUnitId: '1001',
    status: 'ACTIVE',
}

const wrapper = ({children}: PropsWithChildren) => {
    const sdk = {
        getAbout: async () => ({
            user: {id: '7', tenantId: '42', identitySub: 'position-test', status: 'ACTIVE'},
            permissions: ['system:position:read'],
            fieldPolicies: {}, activeRoleContexts: [], apps: [], menus: [], routes: [], actions: [],
            defaultApplicationCode: null, defaultRoute: null, authVersion: 1, policyVersion: 1,
        }),
    } as unknown as Rbac3Client
    const api: FeatureApiClient = {request: state.request}
    return (
        <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
            <Rbac3Provider client={sdk}>
                <FeatureApiProvider client={api}>
                    <MemoryRouter initialEntries={['/iam/positions?orgUnitId=1001']}>
                        {children}
                    </MemoryRouter>
                </FeatureApiProvider>
            </Rbac3Provider>
        </QueryClientProvider>
    )
}

beforeEach(() => {
    state.request.mockReset().mockImplementation((path: string, request?: {query?: Readonly<Record<string, unknown>>}) => {
        if (path === '/api/rbac3/v1/iam/positions') {
            expect(request?.query).toEqual({orgUnitId: '1001'})
            return Promise.resolve([position])
        }
        return Promise.reject(new Error(`Unexpected request: ${path}`))
    })
})

afterEach(() => vi.clearAllMocks())

describe('position administration page', () => {
    it('requests positions through the position endpoint with organization scope', async () => {
        render(<PositionPage/>, {wrapper})

        await waitFor(() => expect(screen.getByText('运营主管')).toBeInTheDocument())
        expect(screen.getByText('岗位')).toBeInTheDocument()
        expect(state.request).toHaveBeenCalledWith(
            '/api/rbac3/v1/iam/positions',
            expect.objectContaining({query: {orgUnitId: '1001'}}),
        )
        expect(state.request.mock.calls.some(([path]) => String(path).includes('/directory/users/'))).toBe(false)
    })
})
