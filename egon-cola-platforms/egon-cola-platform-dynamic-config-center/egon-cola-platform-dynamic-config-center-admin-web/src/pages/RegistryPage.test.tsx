import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../api/client'
import RegistryPage from './RegistryPage'

const record = (data: unknown) => ({
  success: true, code: 0, status: 'SUCCESS', message: '', data, traceId: 't', timestamp: 1,
})

const jsonResponse = (body: unknown) =>
  new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })

describe('RegistryPage', () => {
  beforeEach(() => {
    setDdcTokenProvider(() => 'token')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('loads and renders the four service kinds with dedup', async () => {
    vi.mocked(fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/namespaces/domains')) {
        return Promise.resolve(jsonResponse(record([])))
      }
      if (url.includes('/apps')) {
        return Promise.resolve(jsonResponse(record([])))
      }
      if (url.includes('/registry/services')) {
        return Promise.resolve(jsonResponse(record({ services: [
          { serviceKind: 'HTTP_PROVIDER', protocol: 'http', serviceName: 'orders', group: 'g', version: 'v1' },
          { serviceKind: 'HTTP_PROVIDER', protocol: 'http', serviceName: 'orders', group: 'g', version: 'v1' },
        ] })))
      }
      return Promise.resolve(jsonResponse(record({ services: [] })))
    })

    render(<RegistryPage />)
    await waitFor(() => expect(screen.getByText('orders')).toBeInTheDocument())
    expect(screen.getAllByText('orders')).toHaveLength(1)
    expect(screen.getAllByText('HTTP Provider').length).toBeGreaterThan(0)
  })

  it('loads instances when a service row is selected', async () => {
    vi.mocked(fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/namespaces/domains')) {
        return Promise.resolve(jsonResponse(record([])))
      }
      if (url.includes('/apps')) {
        return Promise.resolve(jsonResponse(record([])))
      }
      if (url.includes('/registry/instances')) {
        return Promise.resolve(jsonResponse(record({ instances: [
          { instanceId: 'i-1', host: '10.0.0.1', port: 8080, secure: false, status: 'ONLINE', lastHeartbeatAt: '2026-07-31T10:00:00Z', expireAt: '2026-07-31T11:00:00Z', metadata: { buildId: 'b-1' } },
        ] })))
      }
      return Promise.resolve(jsonResponse(record({ services: [
        { serviceKind: 'RPC_PROVIDER', protocol: 'grpc', serviceName: 'checkout', group: '', version: '' },
      ] })))
    })

    render(<RegistryPage />)
    await waitFor(() => expect(screen.getByText('checkout')).toBeInTheDocument())
    fireEvent.click(screen.getByText('checkout'))
    await waitFor(() => expect(screen.getByText('10.0.0.1:8080')).toBeInTheDocument())
    expect(screen.getByText('ONLINE')).toBeInTheDocument()
  })
})
