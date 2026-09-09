import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { GatewayApiError } from '../../api/client'
import { GatewayGroupDetailPage } from './GatewayGroupDetailPage'

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

const renderPage = (cached = false) => {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  if (cached) {
    client.setQueryData(['runtime-consistency', 'group-1'], healthy)
    client.setQueryData(['engine-nodes', 'group-1'], [node('api-1', 'API_RPC'), node('mcp-1', 'MCP')])
  }
  return render(<QueryClientProvider client={client}>
    <MemoryRouter initialEntries={['/gateway-groups/group-1']}>
      <Routes><Route path="/gateway-groups/:groupId" element={<GatewayGroupDetailPage />} /></Routes>
    </MemoryRouter>
  </QueryClientProvider>)
}

describe('Gateway group role projection', () => {
  it('renders both role cards and per-node release evidence', async () => {
    renderPage()
    expect(await screen.findByRole('heading', { name: 'API / RPC Engine' })).toBeVisible()
    expect(screen.getByRole('heading', { name: 'MCP Engine' })).toBeVisible()
    expect(screen.getByText('api-1')).toBeVisible()
    expect(screen.getByText('mcp-1')).toBeVisible()
    expect(screen.getAllByText('artifact-sha')).toHaveLength(2)
  })

  it('keeps a missing MCP role visible when API is ready', async () => {
    mocks.engineNodes.mockResolvedValue([node('api-1', 'API_RPC')])
    mocks.consistency.mockResolvedValue({ ...healthy, totalNodes: 1, readyNodes: 1, consistent: false,
      nodes: [state('api-1')] })
    renderPage()
    expect(await screen.findByText('缺少 MCP Engine 角色')).toBeVisible()
  })

  it('reports unknown metadata and retains every inconsistent replica', async () => {
    mocks.engineNodes.mockResolvedValue([node('api-1', 'API_RPC'), node('mcp-1', 'MCP'),
      node('mcp-2', 'MCP'), node('old', 'COMBINED')])
    mocks.consistency.mockResolvedValue({ ...healthy, consistent: false, totalNodes: 4,
      nodes: [state('api-1'), state('mcp-1'), state('mcp-2', 'VERSION_MISMATCH'), state('old', 'ROLE_UNKNOWN')] })
    renderPage()
    expect(await screen.findByText(/未知 Engine 角色/)).toBeVisible()
    expect(screen.getByText('VERSION_MISMATCH')).toBeVisible()
    expect(screen.getByText('mcp-2')).toBeVisible()
    expect(screen.getByText('13')).toBeVisible()
  })

  it('does not count an offline MCP lease as present', async () => {
    mocks.consistency.mockResolvedValue({ ...healthy, consistent: false, nodes: [state('api-1')],
      totalNodes: 1, readyNodes: 1 })
    renderPage()
    expect(await screen.findByText('缺少 MCP Engine 角色')).toBeVisible()
  })

  it('shows both missing roles for an empty projection', async () => {
    mocks.engineNodes.mockResolvedValue([])
    mocks.consistency.mockResolvedValue({ ...healthy, consistent: false, nodes: [], totalNodes: 0, readyNodes: 0 })
    renderPage()
    expect(await screen.findByText('缺少 API / RPC Engine 角色')).toBeVisible()
    expect(screen.getByText('缺少 MCP Engine 角色')).toBeVisible()
  })

  it('marks cached projection stale instead of healthy', async () => {
    mocks.consistency.mockResolvedValue({ ...healthy, stale: true })
    renderPage()
    await screen.findByRole('heading', { name: 'MCP Engine' })
    expect(screen.queryByText(/^CONSISTENT$/)).not.toBeInTheDocument()
    expect(screen.getAllByText('STALE').length).toBeGreaterThan(0)
  })

  it.each([403, 503])('hides cached healthy cards after HTTP %s', async (status) => {
    mocks.engineNodes.mockRejectedValue(new GatewayApiError(status, `HTTP_${status}`, '节点读取失败'))
    renderPage(true)
    expect(await screen.findByText(`HTTP_${status}`)).toBeVisible()
    expect(screen.queryByRole('heading', { name: 'MCP Engine' })).not.toBeInTheDocument()
    expect(screen.queryByText(/^CONSISTENT$/)).not.toBeInTheDocument()
  })

  it('keeps loading projections from looking like missing or healthy roles', async () => {
    mocks.engineNodes.mockReturnValue(new Promise(() => {}))
    const { container } = renderPage()
    await screen.findByRole('heading', { name: 'Edge' })
    expect(container.querySelector('.ant-skeleton')).not.toBeNull()
    expect(screen.queryByText(/^CONSISTENT$/)).not.toBeInTheDocument()
    expect(screen.queryByText(/缺少 MCP/)).not.toBeInTheDocument()
  })
})
