import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { McpServersPage } from './McpServersPage'

const scope = {
  bizCode: 'retail',
  namespace: 'ops',
  env: 'local',
  appCode: 'order',
}

const mocks = vi.hoisted(() => ({
  groups: vi.fn(),
  draft: vi.fn(),
  mcpServers: vi.fn(),
  createMcpServer: vi.fn(),
  navigate: vi.fn(),
}))

vi.mock('../../api/gatewayApi', () => ({
  gatewayApi: {
    groups: mocks.groups,
    draft: mocks.draft,
    mcpServers: mocks.mcpServers,
    createMcpServer: mocks.createMcpServer,
  },
}))

vi.mock('../../hooks/useScope', () => ({ useScope: () => ({ scope }) }))
vi.mock('../../app/capabilities', () => ({ useCapability: () => true }))
vi.mock('react-router-dom', () => ({ useNavigate: () => mocks.navigate }))

vi.mock('antd', async (importOriginal) => {
  const actual = await importOriginal<typeof import('antd')>()
  return {
    ...actual,
    Modal: ({ children, onOk, open, title }: {
      children?: ReactNode
      onOk?: () => void
      open?: boolean
      title?: ReactNode
    }) => open ? (
      <section aria-label={String(title)}>
        <h2>{title}</h2>
        {children}
        <button type="button" onClick={onOk}>OK</button>
      </section>
    ) : null,
    message: { success: vi.fn() },
  }
})

const renderPage = () => render(
  <QueryClientProvider client={new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })}>
    <McpServersPage />
  </QueryClientProvider>,
)

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
  mocks.groups.mockReset().mockResolvedValue([{
    ...scope,
    id: 'group-1',
    gatewayGroupCode: 'retail-local',
    displayName: 'Retail Local',
    enabled: true,
    revision: 1,
  }])
  mocks.draft.mockReset().mockResolvedValue({
    gatewayGroupId: 'group-1',
    revision: 7,
    status: 'DRAFT',
    routes: [],
    policies: [],
    updatedAt: '2026-08-03T00:00:00Z',
  })
  mocks.mcpServers.mockReset().mockResolvedValue([])
  mocks.createMcpServer.mockReset().mockResolvedValue({
    draftRevision: 8,
    resourceId: 'server-1',
    resourceRevision: 0,
    replayed: false,
  })
  mocks.navigate.mockReset()
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('McpServersPage', () => {
  it('creates a Server against the selected Gateway draft revision', async () => {
    renderPage()
    await screen.findByText('MCP Servers')
    const add = screen.getByRole('button', { name: '新增 Server' })
    await waitFor(() => expect(add).toBeEnabled())
    fireEvent.click(add)
    fireEvent.change(screen.getByLabelText('Server Code'), { target: { value: 'commerce' } })
    fireEvent.change(screen.getByLabelText('显示名称'), { target: { value: 'Commerce MCP' } })
    fireEvent.change(screen.getByLabelText('Resource URI'), {
      target: { value: 'https://resource.egon.top/gateway-mcp-commerce' },
    })
    fireEvent.change(screen.getByLabelText('变更原因'), { target: { value: 'initial server' } })
    fireEvent.click(screen.getByRole('button', { name: 'OK' }))

    await waitFor(() => expect(mocks.createMcpServer).toHaveBeenCalledWith(
      expect.objectContaining({
        gatewayGroupId: 'group-1',
        serverCode: 'commerce',
        dialects: ['STABLE_2025_11_25'],
        expectedRevision: 0,
        expectedDraftRevision: 7,
      }),
    ))
    expect(mocks.navigate).toHaveBeenCalledWith('/mcp/servers/server-1')
  })
})
