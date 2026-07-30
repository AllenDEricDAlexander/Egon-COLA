import { render, screen } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { SchemaPanel } from './SchemaPanel'

beforeEach(() => {
  vi.stubGlobal('matchMedia', vi.fn().mockImplementation(() => ({
    matches: false,
    media: '',
    onchange: null,
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
})

afterEach(() => {
  vi.unstubAllGlobals()
})

it('renders an expanded field table with types and descriptions', () => {
  render(
    <SchemaPanel
      title="Request Schema"
      schema={{
        type: 'object',
        properties: {
          customerId: {
            type: 'string',
            protobufType: 'STRING',
            protobufName: 'customer_id',
            fieldNumber: 1,
            description: '客户编号',
          },
          deliveryAddress: {
            type: 'object',
            messageType: 'shop.v1.Address',
            properties: {
              province: {
                type: 'string',
                protobufType: 'STRING',
              },
            },
          },
        },
        required: ['customerId'],
      }}
    />,
  )

  expect(screen.getByText('Request Schema')).toBeInTheDocument()
  expect(screen.getByRole('columnheader', { name: '字段 / 路径' })).toBeInTheDocument()
  expect(screen.getByRole('columnheader', { name: '技术类型' })).toBeInTheDocument()
  expect(screen.getByText('customerId')).toBeInTheDocument()
  expect(screen.getByText('customer_id')).toBeInTheDocument()
  expect(screen.getByText('客户编号')).toBeInTheDocument()
  expect(screen.getByText('province')).toBeInTheDocument()
  expect(screen.getAllByText('暂无字段说明').length).toBeGreaterThan(0)
  expect(screen.getByText('原始 Schema JSON')).toBeInTheDocument()
})

it('renders an explicit empty state for a missing schema', () => {
  render(<SchemaPanel title="Response Schema" schema={{}} />)

  expect(screen.getByText('暂无 Schema 字段')).toBeInTheDocument()
})
