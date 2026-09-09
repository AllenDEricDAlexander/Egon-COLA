import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {cleanup, render, screen} from '@testing-library/react'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {McpTasksPanel} from './McpTasksPanel'

vi.mock('../../app/capabilities', () => ({useCapability: () => false}))
vi.mock('../../api/gatewayApi', () => ({gatewayApi: {mcpToolReferences: async () => []}}))
vi.mock('./useMcpCapabilityCollection', () => ({useMcpCapabilityCollection: () => ({
  query: {data: [], isLoading: false}, save: {isPending: false}, remove: {},
})}))

beforeEach(() => {
  vi.stubGlobal('matchMedia', vi.fn().mockImplementation(() => ({
    matches: false, addListener: vi.fn(), removeListener: vi.fn(),
    addEventListener: vi.fn(), removeEventListener: vi.fn(), dispatchEvent: vi.fn(),
  })))
  vi.stubGlobal('ResizeObserver', class {
    observe() {}
    unobserve() {}
    disconnect() {}
  })
})

afterEach(() => { cleanup(); vi.unstubAllGlobals() })

describe('Tasks extension disclosure', () => {
  it('does not present the existing durable task workflow as standard MCP Tasks', async () => {
    render(<QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
      <McpTasksPanel serverId="qa-server" gatewayGroupId="qa-group" draftRevision={1} />
    </QueryClientProvider>)
    expect(await screen.findByText('Egon Tasks 扩展（非标准 MCP Tasks）')).toBeVisible()
    expect(screen.getByText(/暂不支持标准任务协商、tasks\/list 或 tasks\/result/)).toBeVisible()
  })
})
