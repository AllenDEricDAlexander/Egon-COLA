import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { McpToolsPanel } from './McpToolsPanel'

const mocks = vi.hoisted(() => ({
  mcpManagedTools: vi.fn(),
  mcpServers: vi.fn(),
}))

vi.mock('../../api/gatewayApi', () => ({
  gatewayApi: {
    mcpManagedTools: mocks.mcpManagedTools,
    mcpServers: mocks.mcpServers,
    updateMcpManagedToolOverride: vi.fn(),
    deleteMcpManagedToolOverride: vi.fn(),
  },
}))
vi.mock('../../app/capabilities', () => ({ useCapability: () => true }))
vi.mock('antd', async (importOriginal) => {
  const actual = await importOriginal<typeof import('antd')>()
  return {
    ...actual,
    Modal: ({ children, open, title }: { children?: ReactNode; open?: boolean; title?: ReactNode }) =>
      open ? <section aria-label={String(title)}><h2>{title}</h2>{children}</section> : null,
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
  mocks.mcpManagedTools.mockReset().mockResolvedValue([{
    toolId: 'tool-1',
    gatewayGroupId: 'group-1',
    operationId: 'operation-1',
    operationKey: 'order.query',
    name: 'orders.query',
    description: 'Query an order',
    operationProtocol: 'HTTP',
    inputSchema: { type: 'object' },
    outputSchema: { type: 'object' },
    inputLocations: {},
    codeServerId: 'server-1',
    codeServerCode: 'commerce',
    serverId: 'server-1',
    serverCode: 'commerce',
    codePermissions: ['order:read'],
    additionalPermissions: [],
    effectivePermissions: ['order:read'],
    codeRiskLevel: 'LOW',
    minimumRiskLevel: 'LOW',
    effectiveRiskLevel: 'LOW',
    idempotent: true,
    enabled: true,
    overrideRevision: 0,
  }])
  mocks.mcpServers.mockReset().mockResolvedValue([{
    id: 'server-1',
    displayName: 'Commerce MCP',
    serverCode: 'commerce',
  }])
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('McpToolsPanel', () => {
  it('shows annotation-projected Managed Tools without any manual Local Tool entry', async () => {
    render(
      <QueryClientProvider client={new QueryClient({
        defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
      })}>
        <McpToolsPanel serverId="server-1" gatewayGroupId="group-1" draftRevision={7} />
      </QueryClientProvider>,
    )

    expect(await screen.findByText('orders.query')).toBeVisible()
    expect(screen.getAllByText('order:read')).toHaveLength(2)
    expect(screen.queryByRole('button', { name: /新增.*Tool/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '删除' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: '恢复默认' })).toBeDisabled()

    fireEvent.click(screen.getByRole('button', { name: '严格 Override' }))
    expect(screen.getByRole('heading', { name: 'Managed Tool 严格 Override' })).toBeVisible()
    expect(screen.getByLabelText('MCP Server')).toBeInTheDocument()
    expect(screen.getByLabelText('追加权限')).toBeInTheDocument()
    expect(screen.getByLabelText('最低风险')).toBeInTheDocument()
    expect(screen.queryByLabelText('Operation')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Input Schema')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Output Schema')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('幂等')).not.toBeInTheDocument()
  })
})
