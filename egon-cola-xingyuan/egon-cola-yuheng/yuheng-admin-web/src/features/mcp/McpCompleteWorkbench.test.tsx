import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { McpCapabilityPreview } from './McpCapabilityPreview'

const mocks = vi.hoisted(() => ({ previewMcpServer: vi.fn(), navigate: vi.fn() }))

vi.mock('../../api/gatewayApi', () => ({
  gatewayApi: {
    previewMcpServer: mocks.previewMcpServer,
    validateMcpServer: vi.fn(),
  },
}))
vi.mock('../../app/capabilities', () => ({ useCapability: () => true }))
vi.mock('react-router-dom', () => ({ useNavigate: () => mocks.navigate }))

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
  mocks.previewMcpServer.mockReset().mockResolvedValue({
    content: { remoteMounts: [{ namespace: 'billing' }] },
    validation: {
      valid: false,
      findings: [{
        path: 'remoteMounts.mount-1.namespace',
        code: 'CAPABILITY_NAME_CONFLICT',
        message: 'remote capability name conflicts with a local Tool',
      }],
    },
  })
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('complete MCP workbench', () => {
  it('shows remote capability conflicts and blocks release until resolved', async () => {
    render(
      <QueryClientProvider client={new QueryClient({
        defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
      })}>
        <McpCapabilityPreview serverId="server-1" gatewayGroupId="group-1" />
      </QueryClientProvider>,
    )

    expect(await screen.findByText('CAPABILITY_NAME_CONFLICT')).toBeVisible()
    expect(screen.getByRole('button', { name: /发\s*布/ })).toBeDisabled()
  })
})
