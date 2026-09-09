import {cleanup, fireEvent, render, screen, waitFor} from '@testing-library/react'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {MemoryRouter, useLocation} from 'react-router-dom'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {ApiError} from '@egon-cola/xingyuan-admin-web-shared'
import {AuditLogPage} from './AuditLogPage'

const state = vi.hoisted(() => ({
    forbidden: false,
    request: vi.fn(),
}))

vi.mock('../../auth/AuthContext', () => ({
    httpClient: {request: state.request},
}))

const audit = {
    id: 'audit-1',
    eventType: 'LOGIN_SUCCEEDED',
    actorSub: 'alice-sub',
    targetSub: 'alice-sub',
    result: 'SUCCESS',
    reason: 'password',
    occurredAt: '2026-08-27T01:00:00Z',
}

const LocationProbe = () => <output data-testid="location-search">{useLocation().search}</output>

const renderPage = (initialEntry = '/audits?page=3&size=20') => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}})
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[initialEntry]}>
                <AuditLogPage/>
                <LocationProbe/>
            </MemoryRouter>
        </QueryClientProvider>,
    )
}

beforeEach(() => {
    state.forbidden = false
    state.request.mockReset().mockImplementation((path: string) => {
        if (state.forbidden) return Promise.reject(new ApiError('Forbidden', 403, 'FORBIDDEN'))
        if (path.startsWith('/api/v1/tianquan-shoubing/audits?')) {
            return Promise.resolve({content: [audit], totalElements: 1})
        }
        return Promise.reject(new Error(`Unexpected request: ${path}`))
    })
})

afterEach(cleanup)

describe('Identity audit administration', () => {
    it('renders persisted ISO filters in local time without losing precision', async () => {
        const from = new Date(2026, 8, 6, 15, 30, 45, 123).toISOString()
        renderPage(`/audits?from=${encodeURIComponent(from)}`)
        await screen.findByText('LOGIN_SUCCEEDED')
        expect(screen.getByLabelText('开始时间')).toHaveValue('2026-09-06T15:30:45.123')
        expect(screen.getByLabelText('开始时间')).toHaveAttribute('step', '0.001')
    })

    it('submits local date controls as absolute ISO instants', async () => {
        renderPage('/audits')
        await screen.findByText('LOGIN_SUCCEEDED')
        fireEvent.change(screen.getByLabelText('开始时间'), {target: {value: '2026-09-06T15:30'}})
        fireEvent.change(screen.getByLabelText('结束时间'), {target: {value: '2026-09-06T16:30'}})
        fireEvent.click(screen.getByRole('button', {name: /查\s*询/}))
        const from = encodeURIComponent(new Date(2026, 8, 6, 15, 30).toISOString())
        const to = encodeURIComponent(new Date(2026, 8, 6, 16, 30).toISOString())
        await waitFor(() => expect(state.request).toHaveBeenLastCalledWith(
            `/api/v1/tianquan-shoubing/audits?page=0&size=20&from=${from}&to=${to}`,
        ))
        expect(screen.getByLabelText('开始时间')).toHaveValue('2026-09-06T15:30')
    })

    it('serializes submitted filters and resets to the first page', async () => {
        renderPage()

        expect(await screen.findByText('LOGIN_SUCCEEDED')).toBeInTheDocument()
        expect(screen.getByText('共 1 条')).toBeInTheDocument()
        fireEvent.change(screen.getByRole('textbox', {name: '操作者'}), {target: {value: 'alice'}})
        fireEvent.change(screen.getByRole('textbox', {name: 'Trace ID'}), {target: {value: 'trace-1'}})
        fireEvent.click(screen.getByRole('button', {name: /查\s*询/}))

        await waitFor(() => expect(screen.getByTestId('location-search')).toHaveTextContent('page=0'))
        expect(screen.getByTestId('location-search')).toHaveTextContent('actorSub=alice')
        expect(screen.getByTestId('location-search')).toHaveTextContent('traceId=trace-1')
        expect(state.request).toHaveBeenCalledWith(
            '/api/v1/tianquan-shoubing/audits?page=0&size=20&actorSub=alice&traceId=trace-1',
        )
    })

    it('shows permission denial instead of a successful empty audit page', async () => {
        state.forbidden = true
        renderPage('/audits')

        expect(await screen.findByText('无权访问')).toBeInTheDocument()
        expect(screen.queryByText('暂无数据')).not.toBeInTheDocument()
    })
})
