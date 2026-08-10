import { fireEvent, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../api/client'
import { renderWithQueryClient } from '../test/renderWithQueryClient'
import RegistryPage from './RegistryPage'

const record = (data: unknown) => ({
  success: true,
  code: 0,
  status: 'SUCCESS',
  message: '',
  data,
  traceId: 't',
  timestamp: 1,
})

const pageRecord = <T,>(records: T[], total: number) => ({
  success: true,
  code: 0,
  status: 'SUCCESS',
  message: '',
  records,
  page: {
    total,
    pageNo: 1,
    pageSize: 10,
    pages: Math.ceil(total / 10),
    hasNext: total > 10,
    hasPrevious: false,
  },
  traceId: 't',
  timestamp: 1,
})

const jsonResponse = (body: unknown) => new Response(
  JSON.stringify(body),
  { status: 200, headers: { 'Content-Type': 'application/json' } },
)

const service = {
  bizCode: 'pay-biz',
  env: 'dev',
  appCode: 'orders',
  serviceId: 'service-order-http',
  serviceKind: 'RPC_PROVIDER',
  protocol: 'grpc',
  serviceName: 'orders.OrderService',
  group: 'default',
  version: '1.0.0',
}

const instance = {
  instanceId: 'i-1',
  host: '10.0.0.1',
  port: 8080,
  secure: false,
  status: 'ONLINE',
  lastHeartbeatAt: '2026-07-31T10:00:00Z',
  expireAt: '2026-07-31T11:00:00Z',
  metadata: { buildId: 'b-1' },
}

describe('RegistryPage', () => {
  beforeEach(() => {
    window.matchMedia = vi.fn().mockImplementation((query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    }))
    setDdcTokenProvider(() => 'token')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn((input) => {
      const url = String(input)
      if (url.includes('/registry/services/page')) {
        return Promise.resolve(jsonResponse(pageRecord([service], 12)))
      }
      if (url.includes('/registry/instances/page')) {
        return Promise.resolve(jsonResponse(pageRecord([instance], 1)))
      }
      return Promise.resolve(jsonResponse(record([])))
    }))
  })

  it('pages services and lazily pages one selected service instances', async () => {
    renderWithQueryClient(<RegistryPage />)

    expect(await screen.findByText('orders.OrderService')).toBeInTheDocument()
    expect(screen.getByText('共 12 条')).toBeInTheDocument()
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining(
        '/api/v1/ddc/registry/services/page?pageNo=1&pageSize=10',
      ),
      expect.anything(),
    )
    expect(vi.mocked(fetch).mock.calls.some(([input]) =>
      String(input).includes('/registry/instances/page'))).toBe(false)

    fireEvent.click(screen.getByRole('button', { name: /查\s*看\s*实\s*例/ }))
    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      expect.stringMatching(
        /registry\/instances\/page\?.*bizCode=pay-biz.*env=dev.*appCode=orders.*serviceName=orders\.OrderService.*pageNo=1.*pageSize=10/,
      ),
      expect.anything(),
    ))
    expect(await screen.findByText('i-1')).toBeInTheDocument()
    const instanceCalls = vi.mocked(fetch).mock.calls.filter(([input]) =>
      String(input).includes('/registry/instances/page'))
    expect(instanceCalls).toHaveLength(1)
    expect(document.querySelector('.ant-drawer-content-wrapper'))
      .toHaveStyle({ width: '100%' })
  })
})
