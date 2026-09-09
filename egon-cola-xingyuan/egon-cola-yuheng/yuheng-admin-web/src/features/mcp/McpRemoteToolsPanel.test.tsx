import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { McpRemoteToolsPanel } from './McpRemoteToolsPanel'

const mocks = vi.hoisted(() => ({
  mcpRemoteTools: vi.fn(),
  mcpRemoteMounts: vi.fn(),
}))

vi.mock('../../api/gatewayApi', () => ({
  gatewayApi: {
    mcpRemoteTools: mocks.mcpRemoteTools,
    mcpRemoteMounts: mocks.mcpRemoteMounts,
    createMcpRemoteTool: vi.fn(),
    updateMcpRemoteTool: vi.fn(),
    deleteMcpRemoteTool: vi.fn(),
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
  mocks.mcpRemoteTools.mockReset().mockResolvedValue([])
  mocks.mcpRemoteMounts.mockReset().mockResolvedValue([{
    id: 'mount-1',
    gatewayGroupId: 'group-1',
    serverId: 'server-1',
    namespace: 'inventory',
    capabilityFingerprint: 'abcdef',
    enabled: true,
  }])
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('McpRemoteToolsPanel', () => {
  it('provides a dedicated Remote Tool form without a Local Operation source', async () => {
    render(
      <QueryClientProvider client={new QueryClient({
        defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
      })}>
        <McpRemoteToolsPanel serverId="server-1" gatewayGroupId="group-1" draftRevision={7} />
      </QueryClientProvider>,
    )

    const add = await screen.findByRole('button', { name: '新增 Remote Tool' })
    await waitFor(() => expect(add).toBeEnabled())
    fireEvent.click(add)
    expect(screen.getByLabelText('Remote Mount')).toBeInTheDocument()
    expect(screen.getByLabelText('Input Schema')).toBeInTheDocument()
    expect(screen.queryByLabelText('来源')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Operation')).not.toBeInTheDocument()
    expect(screen.queryByText('Local Operation')).not.toBeInTheDocument()
  })

  it('disables creation when the Server has no reviewed Remote Mount', async () => {
    mocks.mcpRemoteMounts.mockResolvedValueOnce([])
    render(
      <QueryClientProvider client={new QueryClient({
        defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
      })}>
        <McpRemoteToolsPanel serverId="server-1" gatewayGroupId="group-1" draftRevision={7} />
      </QueryClientProvider>,
    )

    const add = await screen.findByRole('button', { name: '新增 Remote Tool' })
    await waitFor(() => expect(screen.getByText('当前 MCP Server 没有可用的 Remote Mount')).toBeVisible())
    expect(add).toBeDisabled()
  })
})
