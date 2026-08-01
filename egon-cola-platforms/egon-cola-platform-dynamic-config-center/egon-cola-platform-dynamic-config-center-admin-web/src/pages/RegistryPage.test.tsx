import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../api/client'
import { clearScopeOptionsCache } from '../components/scope/useScopeOptions'
import RegistryPage from './RegistryPage'

const record = (data: unknown) => ({
  success: true, code: 0, status: 'SUCCESS', message: '', data, traceId: 't', timestamp: 1,
})

const jsonResponse = (body: unknown) =>
  new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })

const servicePayload = (appCode: string, serviceName: string) => ({
  bizCode: 'pay-biz', appCode, serviceKind: 'HTTP_PROVIDER', protocol: 'http',
  serviceName, group: 'default', version: '1.0.0',
})

const instancePayload = (instanceId: string) => ({
  instanceId, host: '10.0.0.1', port: 8080, secure: false, status: 'ONLINE',
  lastHeartbeatAt: '2026-07-31T10:00:00Z', expireAt: '2026-07-31T11:00:00Z', metadata: { buildId: 'b-1' },
})

describe('RegistryPage', () => {
  beforeEach(() => {
    clearScopeOptionsCache()
    setDdcTokenProvider(() => 'token')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('aggregates services by app and opens instance drawer', async () => {
    vi.mocked(fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/bizs')) return Promise.resolve(jsonResponse(record([])))
      if (url.includes('/envs')) return Promise.resolve(jsonResponse(record([])))
      if (url.includes('/apps')) return Promise.resolve(jsonResponse(record([])))
      if (url.includes('/namespaces')) return Promise.resolve(jsonResponse(record([])))
      if (url.includes('/registry/instances')) {
        return Promise.resolve(jsonResponse(record({ instances: [instancePayload('i-1')] })))
      }
      if (url.includes('/registry/services')) {
        return Promise.resolve(jsonResponse(record({ services: [
          servicePayload('orders', 'order-http'),
          servicePayload('orders', 'order-http'),
          servicePayload('billing', 'billing-http'),
        ] })))
      }
      return Promise.resolve(jsonResponse(record(null)))
    })

    render(<RegistryPage />)
    await waitFor(() => expect(screen.getByText('orders')).toBeInTheDocument())
    const serviceRequests = vi.mocked(fetch).mock.calls
      .map(([input]) => String(input))
      .filter((url) => url.includes('/registry/services'))
    expect(serviceRequests).toHaveLength(4)
    serviceRequests.forEach((url) => {
      expect(url).toContain('bizCode=default')
      expect(url).toContain('appCode=default-app')
      expect(url).toContain('env=dev')
      expect(url).toContain('namespace=default')
    })
    // 去重：orders 只有一行；billing 一行
    expect(screen.getAllByText('orders')).toHaveLength(1)
    expect(screen.getByText('billing')).toBeInTheDocument()

    // 点击 app 行打开抽屉
    fireEvent.click(screen.getByText('orders'))
    await waitFor(() => expect(screen.getByText(/order-http/)).toBeInTheDocument())
    await waitFor(() => expect(screen.getByText('i-1')).toBeInTheDocument())
    expect(screen.getByText('10.0.0.1:8080')).toBeInTheDocument()
  })
})
