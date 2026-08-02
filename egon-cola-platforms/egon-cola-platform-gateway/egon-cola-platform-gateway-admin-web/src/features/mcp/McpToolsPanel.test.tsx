import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { McpToolsPanel } from './McpToolsPanel'

const mocks = vi.hoisted(() => ({
  mcpCapabilities: vi.fn(),
  mcpOperationOptions: vi.fn(),
}))

vi.mock('../../api/gatewayApi', () => ({
  gatewayApi: {
    mcpCapabilities: mocks.mcpCapabilities,
    mcpOperationOptions: mocks.mcpOperationOptions,
    createMcpCapability: vi.fn(),
    updateMcpCapability: vi.fn(),
    deleteMcpCapability: vi.fn(),
  },
}))
vi.mock('../../app/capabilities', () => ({ useCapability: () => true }))
vi.mock('../../hooks/useScope', () => ({
  useScope: () => ({
    scope: { bizCode: 'retail', namespace: 'ops', env: 'local', appCode: 'order' },
  }),
}))
vi.mock('antd', async (importOriginal) => {
  const actual = await importOriginal<typeof import('antd')>()
  return {
    ...actual,
    Modal: ({ children, open, title }: { children?: ReactNode; open?: boolean; title?: ReactNode }) =>
      open ? <section aria-label={String(title)}><h2>{title}</h2>{children}</section> : null,
    message: { success: vi.fn() },
  }
})

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
  mocks.mcpCapabilities.mockReset().mockResolvedValue([])
  mocks.mcpOperationOptions.mockReset().mockResolvedValue([{
    value: 'operation-1',
    label: 'order.query · HTTP GET /orders/{id}',
  }])
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('McpToolsPanel', () => {
  it('selects an Operation from catalog and never exposes a provider URL field', async () => {
    render(
      <QueryClientProvider client={new QueryClient({
        defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
      })}>
        <McpToolsPanel serverId="server-1" gatewayGroupId="group-1" draftRevision={7} />
      </QueryClientProvider>,
    )

    fireEvent.click(await screen.findByRole('button', { name: '新增 Tool' }))
    expect(screen.getByLabelText('Operation')).toBeInTheDocument()
    expect(screen.queryByLabelText('Provider URL')).not.toBeInTheDocument()
  })
})
