import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen, fireEvent, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { McpCapabilityPreview } from './McpCapabilityPreview'

const mocks = vi.hoisted(() => ({
  preview: vi.fn(), validate: vi.fn(), navigate: vi.fn(), permissions: new Set<string>(),
}))

vi.mock('../../api/gatewayApi', () => ({
  gatewayApi: { previewMcpServer: mocks.preview, validateMcpServer: mocks.validate },
}))
vi.mock('../../app/capabilities', () => ({
  useCapability: (permission: string) => mocks.permissions.has(permission),
}))
vi.mock('react-router-dom', () => ({ useNavigate: () => mocks.navigate }))

beforeEach(() => {
  mocks.permissions.clear()
  mocks.permissions.add('gateway:releases:write')
  mocks.preview.mockReset().mockResolvedValue({ content: {}, validation: { valid: true, findings: [] } })
  mocks.validate.mockReset()
  mocks.navigate.mockReset()
})
afterEach(cleanup)

const renderPreview = () => render(
  <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
    <McpCapabilityPreview serverId="server-1" gatewayGroupId="group-1" />
  </QueryClientProvider>,
)

describe('MCP unified release permission', () => {
  it('uses the backend unified release capability without an invented MCP release permission', async () => {
    renderPreview()
    await screen.findByText('MCP 校验通过')
    expect(screen.getByRole('button', { name: '发 布' })).toBeEnabled()
    fireEvent.click(screen.getByRole('button', { name: '发 布' }))
    expect(mocks.navigate).toHaveBeenCalledWith('/gateway-groups/group-1/releases')
  })

  it('keeps publication disabled without the backend release capability', async () => {
    mocks.permissions.clear()
    mocks.permissions.add('gateway:mcp:write')
    renderPreview()
    await screen.findByText('MCP 校验通过')
    expect(screen.getByRole('button', { name: '发 布' })).toBeDisabled()
  })

  it('keeps publication disabled when the latest validation fails', async () => {
    mocks.validate.mockResolvedValue({ valid: false, findings: [] })
    renderPreview()
    await screen.findByText('MCP 校验通过')
    fireEvent.click(screen.getByRole('button', { name: '重新校验' }))
    await waitFor(() => expect(mocks.validate).toHaveBeenCalledWith('server-1'))
    await screen.findByText('MCP 校验未通过')
    expect(screen.getByRole('button', { name: '发 布' })).toBeDisabled()
  })
})
