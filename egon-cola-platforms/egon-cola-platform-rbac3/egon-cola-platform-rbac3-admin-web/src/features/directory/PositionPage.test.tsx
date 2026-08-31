import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {fireEvent, render, screen, waitFor} from '@testing-library/react'
import type {PropsWithChildren, ReactNode} from 'react'
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
    externalId: null,
    validFrom: '2026-08-29T00:00:00Z',
    validTo: null,
    version: 1,
}

const wrapper = ({children}: PropsWithChildren) => createWrapper(children, ['system:position:read'])

const manageWrapper = ({children}: PropsWithChildren) => createWrapper(children, [
    'system:position:read',
    'system:position:manage',
])

const createWrapper = (children: ReactNode, permissions: readonly string[]) => {
    const sdk = {
        getAbout: async () => ({
            user: {id: '7', tenantId: '42', identitySub: 'position-test', status: 'ACTIVE'},
            permissions,
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

    it('requires an organization and sends the row version for position mutations', async () => {
        render(<PositionPage/>, {wrapper: manageWrapper})

        await waitFor(() => expect(screen.getByText('运营主管')).toBeInTheDocument())
        fireEvent.click(screen.getByRole('button', {name: '新增岗位'}))
        fireEvent.change(screen.getByRole('textbox', {name: '岗位编码'}), {target: {value: 'new-position'}})
        fireEvent.change(screen.getByRole('textbox', {name: '岗位名称'}), {target: {value: '新岗位'}})
        fireEvent.change(screen.getByRole('textbox', {name: '岗位归属组织 ID'}), {target: {value: '1001'}})
        fireEvent.change(screen.getByRole('textbox', {name: '生效时间'}), {target: {value: '2026-08-29T00:00:00Z'}})
        fireEvent.click(screen.getByRole('button', {name: /保\s*存/}))

        await waitFor(() => expect(state.request).toHaveBeenCalledWith(
            '/api/rbac3/v1/iam/positions',
            expect.objectContaining({
                method: 'POST',
                body: expect.objectContaining({
                    name: '新岗位',
                    orgUnitId: '1001',
                    validTo: null,
                }),
            }),
        ))

        fireEvent.click(screen.getByRole('button', {name: '查看详情'}))
        fireEvent.click(screen.getByRole('button', {name: /编\s*辑/}))
        fireEvent.click(screen.getByRole('button', {name: /保\s*存/}))
        await waitFor(() => expect(state.request).toHaveBeenCalledWith(
            '/api/rbac3/v1/iam/positions/2001',
            expect.objectContaining({
                method: 'PUT',
                body: expect.objectContaining({expectedVersion: 1}),
            }),
        ))

        fireEvent.click(screen.getByRole('button', {name: '查看详情'}))
        fireEvent.click(screen.getByRole('button', {name: /归\s*档/}))
        fireEvent.click(screen.getByRole('button', {name: '确认停用岗位'}))
        await waitFor(() => expect(state.request).toHaveBeenCalledWith(
            '/api/rbac3/v1/iam/positions/2001',
            {method: 'DELETE', query: {expectedVersion: 1}},
        ))
    })

    it('does not expose position mutations without manage permission', async () => {
        render(<PositionPage/>, {wrapper})

        await waitFor(() => expect(screen.getByText('运营主管')).toBeInTheDocument())
        expect(screen.queryByRole('button', {name: '新增岗位'})).not.toBeInTheDocument()
        fireEvent.click(screen.getByRole('button', {name: '查看详情'}))
        expect(screen.queryByRole('button', {name: '编辑'})).not.toBeInTheDocument()
        expect(screen.queryByRole('button', {name: '归档'})).not.toBeInTheDocument()
    })
})
