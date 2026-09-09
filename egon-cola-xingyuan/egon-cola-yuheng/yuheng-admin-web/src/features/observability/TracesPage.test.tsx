import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { GatewayApiError } from '../../api/client'
import { gatewayApi } from '../../api/gatewayApi'
import type { Page, TraceSummary } from '../../api/types'
import { TracesPage } from './TracesPage'

vi.mock('../../api/gatewayApi', () => ({
  gatewayApi: {
    traces: vi.fn(),
    scopes: vi.fn().mockResolvedValue([]),
  },
}))

vi.mock('../../hooks/useGatewayScopeBindings', () => ({
  useGatewayScopeBindings: () => ({ data: [], isLoading: false, error: null, refetch: vi.fn() }),
}))

const emptyPage: Page<TraceSummary> = {
  items: [],
  page: 1,
  size: 20,
  total: 0,
}

const pageWithNewTrace: Page<TraceSummary> = {
  items: [{
    traceId: '1234567890abcdef1234567890abcdef',
    startedAt: '2026-07-28T05:00:00Z',
    durationMs: 12,
    protocol: 'HTTP',
    gatewayGroupId: 'group-local',
    operationKey: 'GET /api/orders/{id}',
    statusCategory: 'SUCCESS',
    engineInstanceId: 'yuheng-biz-gateway-local-1',
    providerService: 'yuheng-test-http-provider',
  }],
  page: 1,
  size: 20,
  total: 1,
}

beforeEach(() => {
  vi.useFakeTimers()
  vi.stubGlobal('matchMedia', vi.fn().mockImplementation(() => ({
    matches: false,
    media: '',
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })))
  vi.stubGlobal('ResizeObserver', class {
    observe() {}

    unobserve() {}

    disconnect() {}
  })
  vi.mocked(gatewayApi.traces)
    .mockResolvedValueOnce(emptyPage)
    .mockResolvedValue(pageWithNewTrace)
})

afterEach(() => {
  cleanup()
  vi.useRealTimers()
  vi.unstubAllGlobals()
  vi.clearAllMocks()
})

it('shows a yuheng call that arrives after the page was opened', async () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/observability/traces?env=local&namespace=default']}>
        <TracesPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )

  await act(async () => {
    await vi.waitFor(() => {
      expect(gatewayApi.traces).toHaveBeenCalledTimes(1)
    })
  })
  expect(screen.queryByText(pageWithNewTrace.items[0].traceId)).not.toBeInTheDocument()

  await act(async () => {
    await vi.advanceTimersByTimeAsync(5_000)
    await vi.waitFor(() => {
      expect(gatewayApi.traces).toHaveBeenCalledTimes(2)
    })
  })

  expect(screen.getByText(pageWithNewTrace.items[0].traceId)).toBeInTheDocument()
})

it('serializes submitted filters and resets the server page to one', async () => {
  vi.mocked(gatewayApi.traces).mockReset().mockResolvedValue(pageWithNewTrace)
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter initialEntries={['/observability/traces?env=local&namespace=default&page=4']}>
        <TracesPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )

  await act(async () => {
    await vi.waitFor(() => expect(screen.getByText(pageWithNewTrace.items[0].traceId)).toBeInTheDocument())
  })
  fireEvent.change(screen.getByLabelText('Trace ID'), { target: { value: '1234' } })
  await act(async () => {
    fireEvent.click(screen.getByRole('button', { name: '查询' }))
    await Promise.resolve()
  })

  await act(async () => {
    await vi.waitFor(() => expect(gatewayApi.traces).toHaveBeenCalledTimes(2))
  })
  const filters = vi.mocked(gatewayApi.traces).mock.calls[1][1]
  expect(filters.get('traceId')).toBe('1234')
  expect(filters.get('page')).toBe('1')
})

it('keeps traces summary-only when the backend has no detail mapping', async () => {
  vi.mocked(gatewayApi.traces).mockReset().mockResolvedValue(pageWithNewTrace)
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter initialEntries={['/observability/traces?env=local&namespace=default']}>
        <TracesPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )

  await act(async () => {
    await vi.waitFor(() => expect(screen.getByText(pageWithNewTrace.items[0].traceId)).toBeInTheDocument())
  })
  expect(screen.queryByRole('button', {name: '查看详情'})).not.toBeInTheDocument()
})

it('keeps existing rows visible when a background refresh fails', async () => {
  vi.mocked(gatewayApi.traces).mockReset()
    .mockResolvedValueOnce(pageWithNewTrace)
    .mockRejectedValueOnce(new GatewayApiError(500, 'TRACE_REFRESH_FAILED', 'refresh failed'))
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter initialEntries={['/observability/traces?env=local&namespace=default']}>
        <TracesPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )

  await act(async () => {
    await vi.waitFor(() => expect(screen.getByText(pageWithNewTrace.items[0].traceId)).toBeInTheDocument())
  })
  await act(async () => {
    await vi.advanceTimersByTimeAsync(5_000)
    await vi.waitFor(() => expect(gatewayApi.traces).toHaveBeenCalledTimes(2))
  })

  expect(screen.getByText(pageWithNewTrace.items[0].traceId)).toBeInTheDocument()
  expect(screen.getByText('刷新失败，当前显示最近一次成功结果')).toBeInTheDocument()
})
