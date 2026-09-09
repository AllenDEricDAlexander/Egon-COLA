import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { GatewayApiError } from '../../api/client'
import { McpRuntimeStatus } from './McpRuntimeStatus'

const mocks = vi.hoisted(() => ({ group: vi.fn(), engineNodes: vi.fn(), consistency: vi.fn() }))
vi.mock('../../api/gatewayApi', () => ({ gatewayApi: mocks }))

const node = (id: string, role: string) => ({
  appCode: 'ge', env: 'test', namespace: 'default', instanceId: id, leaseId: `lease-${id}`,
  leaseRole: 'CONFIG_CLIENT', status: 'ONLINE', registeredAt: '2026-09-05T00:00:00Z',
  lastHeartbeatAt: '2026-09-05T00:00:01Z', expireAt: '2026-09-05T00:01:00Z',
  observedAt: '2026-09-05T00:00:02Z', stale: false,
  metadata: { 'gateway.engine.role': role },
})
const state = (id: string, reason?: string) => ({
  instanceId: id, leaseId: `lease-${id}`, leaseStatus: 'ONLINE',
  status: reason ? 'INCONSISTENT' : 'CONSISTENT', reason,
  activeReleaseId: 'release-1', activeRuleVersion: reason === 'VERSION_MISMATCH' ? 13 : 12,
  activeRuleChecksum: reason === 'CHECKSUM_MISMATCH' ? 'different-sha' : 'artifact-sha',
  lastApplyStatus: 'ACK_SUCCESS', lastAckAt: '2026-09-05T00:00:01Z',
})
const healthy = {
  targetReleaseId: 'release-1', targetReleaseStatus: 'SUCCESS', readyNodes: 2, totalNodes: 2,
  consistent: true, stale: false, source: 'DDC_CONFIG_CLIENT', observedAt: '2026-09-05T00:00:02Z',
  nodes: [state('api-1'), state('mcp-1')],
}

beforeEach(() => {
  mocks.group.mockReset().mockResolvedValue({ id: 'group-1', displayName: 'Edge', gatewayGroupCode: 'edge',
    env: 'test', namespace: 'default', enabled: true, revision: 1 })
  mocks.engineNodes.mockReset().mockResolvedValue([node('api-1', 'API_RPC'), node('mcp-1', 'MCP')])
  mocks.consistency.mockReset().mockResolvedValue(healthy)
  vi.stubGlobal('matchMedia', vi.fn().mockImplementation(() => ({
    matches: false, addListener: vi.fn(), removeListener: vi.fn(),
    addEventListener: vi.fn(), removeEventListener: vi.fn(), dispatchEvent: vi.fn(),
  })))
})
afterEach(() => { cleanup(); vi.unstubAllGlobals() })

const renderStatus = () => render(<QueryClientProvider client={new QueryClient({
  defaultOptions: { queries: { retry: false } },
})}><McpRuntimeStatus gatewayGroupId="group-1" /></QueryClientProvider>)

describe('MCP runtime role isolation', () => {
  it('shows MCP evidence without displaying API node rows', async () => {
    renderStatus()
    expect(await screen.findByRole('heading', { name: 'MCP Engine' })).toBeVisible()
    expect(screen.getByText('artifact-sha')).toBeVisible()
    expect(screen.queryByText('api-1')).not.toBeInTheDocument()
  })

  it.each(['API_RPC', 'COMBINED', 'mcp', ''])('does not use %s as an available MCP role', async (role) => {
    mocks.engineNodes.mockResolvedValue([node('not-mcp', role)])
    mocks.consistency.mockResolvedValue({ ...healthy, consistent: false, nodes: [state('not-mcp')] })
    renderStatus()
    expect(await screen.findByText('MCP Engine 不可用')).toBeVisible()
    expect(screen.queryByText(/^CONSISTENT$/)).not.toBeInTheDocument()
  })

  it('keeps a skewed MCP replica visible even when another replica is ready', async () => {
    mocks.engineNodes.mockResolvedValue([node('mcp-1', 'MCP'), node('mcp-2', 'MCP')])
    mocks.consistency.mockResolvedValue({ ...healthy, consistent: false,
      nodes: [state('mcp-1'), state('mcp-2', 'CHECKSUM_MISMATCH')] })
    renderStatus()
    expect(await screen.findByText('CHECKSUM_MISMATCH')).toBeVisible()
    expect(screen.getByText('different-sha')).toBeVisible()
  })

  it('does not mark a stale projection healthy', async () => {
    mocks.consistency.mockResolvedValue({ ...healthy, stale: true })
    renderStatus()
    await screen.findByRole('heading', { name: 'MCP Engine' })
    expect(screen.queryByText(/^CONSISTENT$/)).not.toBeInTheDocument()
    expect(screen.getAllByText('STALE').length).toBeGreaterThan(0)
  })

  it.each(['ACK_MISSING', 'APPLY_NOT_ACKED', 'RELEASE_MISMATCH'])('shows %s for MCP without API health fallback', async (reason) => {
    mocks.consistency.mockResolvedValue({ ...healthy, consistent: false,
      nodes: [state('api-1'), state('mcp-1', reason)] })
    renderStatus()
    expect(await screen.findByText(reason)).toBeVisible()
    expect(screen.queryByText(/^CONSISTENT$/)).not.toBeInTheDocument()
  })

  it('does not count an old MCP lease that the backend no longer considers online', async () => {
    mocks.consistency.mockResolvedValue({ ...healthy, consistent: false, nodes: [state('api-1')] })
    renderStatus()
    expect(await screen.findByText('MCP Engine 不可用')).toBeVisible()
    expect(screen.getByText('未确认在线租约')).toBeVisible()
    expect(screen.queryByText(/^CONSISTENT$/)).not.toBeInTheDocument()
  })

  it.each([403, 503])('surfaces HTTP %s without synthesizing role health', async (status) => {
    mocks.consistency.mockRejectedValue(new GatewayApiError(status, `HTTP_${status}`, '运行态读取失败'))
    renderStatus()
    expect(await screen.findByText(`HTTP_${status}`)).toBeVisible()
    expect(screen.queryByText(/^CONSISTENT$/)).not.toBeInTheDocument()
  })

  it('renders loading without a false missing-role message', () => {
    mocks.consistency.mockReturnValue(new Promise(() => {}))
    const { container } = renderStatus()
    expect(container.querySelector('.ant-skeleton')).not.toBeNull()
    expect(screen.queryByText('MCP Engine 不可用')).not.toBeInTheDocument()
  })
})
