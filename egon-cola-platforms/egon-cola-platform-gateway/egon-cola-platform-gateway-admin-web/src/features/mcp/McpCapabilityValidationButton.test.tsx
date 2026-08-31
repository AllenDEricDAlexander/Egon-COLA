import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {cleanup, fireEvent, render, screen, waitFor} from '@testing-library/react'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import type {McpCapabilityPlural} from '../../api/types'
import {McpCapabilityValidationButton} from './McpCapabilityValidationButton'

const mocks = vi.hoisted(() => ({validateMcpCapability: vi.fn()}))

vi.mock('../../api/gatewayApi', () => ({
  gatewayApi: {validateMcpCapability: mocks.validateMcpCapability},
}))
vi.mock('../../app/capabilities', () => ({useCapability: () => true}))

beforeEach(() => {
  mocks.validateMcpCapability.mockReset().mockResolvedValue({
    valid: false,
    findings: [{code: 'INVALID', path: '$.name', message: '名称无效'}],
  })
})

afterEach(() => {
  cleanup()
})

describe('McpCapabilityValidationButton', () => {
  it('posts the exact capability validation request and displays findings', async () => {
    render(
      <QueryClientProvider client={new QueryClient({defaultOptions: {mutations: {retry: false}}})}>
        <McpCapabilityValidationButton plural={'prompts' satisfies McpCapabilityPlural} capabilityId="p-1" gatewayGroupId="g-1" />
      </QueryClientProvider>,
    )

    fireEvent.click(screen.getByRole('button', {name: /校\s*验/}))

    await waitFor(() => expect(mocks.validateMcpCapability).toHaveBeenCalledWith('prompts', 'p-1', 'g-1'))
    expect(await screen.findByText(/INVALID.*名称无效/)).toBeInTheDocument()
  })
})
