import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {fireEvent, render, screen, waitFor} from '@testing-library/react'
import type {PropsWithChildren, ReactNode} from 'react'
import {MemoryRouter} from 'react-router-dom'
import {type Rbac3Client, Rbac3Provider} from '@egon-cola/rbac3-react-sdk'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {FeatureApiProvider, type FeatureApiClient} from '../shared/FeatureApi'
import {OrganizationPage} from './OrganizationPage'

const state = vi.hoisted(() => ({request: vi.fn()}))

const organization = {
    orgUnitId: '1001',
    snapshotId: null,
    type: 'DEPT',
    code: 'hq',
    name: '总部',
    parentId: null,
    path: 'hq',
    depth: 0,
    status: 'ACTIVE',
    externalId: null,
    validFrom: '2026-08-29T00:00:00Z',
    validTo: null,
    version: 1,
}

const wrapper = ({children}: PropsWithChildren) => createWrapper(children, ['system:organization:read'])

const manageWrapper = ({children}: PropsWithChildren) => createWrapper(children, [
    'system:organization:read',
    'system:organization:manage',
])

const createWrapper = (children: ReactNode, permissions: readonly string[]) => {
    const sdk = {
        getAbout: async () => ({
            user: {id: '7', tenantId: '42', identitySub: 'organization-test', status: 'ACTIVE'},
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
                    <MemoryRouter initialEntries={['/iam/organizations']}>{children}</MemoryRouter>
                </FeatureApiProvider>
            </Rbac3Provider>
        </QueryClientProvider>
    )
}

beforeEach(() => {
    state.request.mockReset().mockImplementation((path: string) => {
        if (path === '/api/rbac3/v1/iam/organizations') return Promise.resolve([organization])
        if (path === '/api/rbac3/v1/iam/organizations/1001') return Promise.resolve(organization)
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

    it('creates and archives a manual organization with the server version contract', async () => {
        render(<OrganizationPage/>, {wrapper: manageWrapper})

        await waitFor(() => expect(screen.getByText('总部')).toBeInTheDocument())
        fireEvent.click(screen.getByRole('button', {name: '新增组织'}))
        fireEvent.change(screen.getByRole('textbox', {name: '组织编码'}), {target: {value: 'east'}})
        fireEvent.change(screen.getByRole('textbox', {name: '组织名称'}), {target: {value: '华东'}})
        fireEvent.change(screen.getByRole('textbox', {name: '生效时间'}), {target: {value: '2026-08-29T00:00:00Z'}})
        fireEvent.click(screen.getByRole('button', {name: /保\s*存/}))

        await waitFor(() => expect(state.request).toHaveBeenCalledWith(
            '/api/rbac3/v1/iam/organizations',
            expect.objectContaining({
                method: 'POST',
                body: expect.objectContaining({
                    code: 'east',
                    name: '华东',
                    validTo: null,
                }),
            }),
        ))

        fireEvent.click(screen.getByRole('button', {name: '查看详情'}))
        fireEvent.click(screen.getByRole('button', {name: /编\s*辑/}))
        fireEvent.click(screen.getByRole('button', {name: /保\s*存/}))
        await waitFor(() => expect(state.request).toHaveBeenCalledWith(
            '/api/rbac3/v1/iam/organizations/1001',
            expect.objectContaining({
                method: 'PUT',
                body: expect.objectContaining({expectedVersion: 1}),
            }),
        ))

        fireEvent.click(screen.getByRole('button', {name: '查看详情'}))
        fireEvent.click(screen.getByRole('button', {name: /归\s*档/}))
        fireEvent.click(screen.getByRole('button', {name: '确认停用组织'}))
        await waitFor(() => expect(state.request).toHaveBeenCalledWith(
            '/api/rbac3/v1/iam/organizations/1001',
            {method: 'DELETE', query: {expectedVersion: 1}},
        ))
    })

    it('does not expose organization mutations without manage permission', async () => {
        render(<OrganizationPage/>, {wrapper})

        await waitFor(() => expect(screen.getByText('总部')).toBeInTheDocument())
        expect(screen.queryByRole('button', {name: '新增组织'})).not.toBeInTheDocument()
        fireEvent.click(screen.getByRole('button', {name: '查看详情'}))
        expect(screen.queryByRole('button', {name: '编辑'})).not.toBeInTheDocument()
        expect(screen.queryByRole('button', {name: '归档'})).not.toBeInTheDocument()
    })
})
