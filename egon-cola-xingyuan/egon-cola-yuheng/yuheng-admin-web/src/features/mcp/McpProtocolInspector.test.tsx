import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { McpProtocolInspector } from './McpProtocolInspector'

const mocks = vi.hoisted(() => ({ inspectMcpProtocol: vi.fn() }))

vi.mock('../../api/gatewayApi', () => ({
  gatewayApi: { inspectMcpProtocol: mocks.inspectMcpProtocol },
}))
vi.mock('../../app/capabilities', () => ({ useCapability: () => true }))

beforeEach(() => {
  vi.stubGlobal('matchMedia', vi.fn().mockImplementation(() => ({
    matches: false,
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
  mocks.inspectMcpProtocol.mockReset().mockResolvedValue({
    path: '/mcp/commerce',
    headers: {
      Authorization: 'Bearer never-render-this-token',
      'Content-Type': 'application/json',
    },
    body: { jsonrpc: '2.0', id: 'inspect-1', method: 'initialize', params: {} },
    releaseCandidate: false,
  })
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('McpProtocolInspector', () => {
  it('uses Stable and RC templates and redacts authorization from inspection output', async () => {
    render(
      <QueryClientProvider client={new QueryClient({
        defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
      })}>
        <McpProtocolInspector
          serverId="server-1"
          dialects={['STABLE_2025_11_25', 'RC_2026_07_28']}
        />
      </QueryClientProvider>,
    )

    expect(screen.getAllByText('Stable 2025-11-25')).not.toHaveLength(0)
    expect(screen.getByText(/RC 2026-07-28/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '生成请求' }))

    expect(await screen.findByText(/\[REDACTED\]/)).toBeInTheDocument()
    expect(screen.queryByText(/never-render-this-token/)).not.toBeInTheDocument()
  })
})
