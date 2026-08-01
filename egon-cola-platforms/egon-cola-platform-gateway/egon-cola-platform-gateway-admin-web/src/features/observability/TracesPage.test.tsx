import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { gatewayApi } from '../../api/gatewayApi'
import type { Page, TraceSummary } from '../../api/types'
import { TracesPage } from './TracesPage'

vi.mock('../../api/gatewayApi', () => ({
  gatewayApi: {
    traces: vi.fn(),
  },
}))

vi.mock('../../hooks/useScope', () => ({
  useScope: () => ({
    scope: {
      bizCode: 'retail',
      namespace: 'default',
      env: 'local',
      appCode: 'order',
    },
  }),
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
    engineInstanceId: 'gateway-engine-local-1',
    providerService: 'gateway-test-http-provider',
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
  vi.useRealTimers()
  vi.unstubAllGlobals()
  vi.clearAllMocks()
})

it('shows a gateway call that arrives after the page was opened', async () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  render(
    <QueryClientProvider client={queryClient}>
      <TracesPage />
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
