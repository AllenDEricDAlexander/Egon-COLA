import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { GatewayApiError } from '../../api/client'
import { OperationPage } from './OperationPage'

const mocks = vi.hoisted(() => ({
  operation: vi.fn(),
  operationOpenApi: vi.fn(),
  openapiSnapshotDocument: vi.fn(),
  updateOperationMetadata: vi.fn(),
  updateManualDefinition: vi.fn(),
  deprecateOperation: vi.fn(),
}))

vi.mock('../../api/gatewayApi', () => ({
  gatewayApi: {
    operation: mocks.operation,
    operationOpenApi: mocks.operationOpenApi,
    openapiSnapshotDocument: mocks.openapiSnapshotDocument,
    updateOperationMetadata: mocks.updateOperationMetadata,
    updateManualDefinition: mocks.updateManualDefinition,
    deprecateOperation: mocks.deprecateOperation,
  },
}))

vi.mock('../../app/capabilities', () => ({
  useCapability: () => true,
}))

const renderPage = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  })
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/operations/operation-1']}>
        <Routes>
          <Route path="/operations/:operationId" element={<OperationPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

const openApiOperation = {
  id: 'operation-1',
  operationKey: 'orders:http:GET:/orders/{id}',
  protocol: 'HTTP',
  methodIdentity: 'GET /orders/{id}',
  externalAccessible: true,
  lifecycleStatus: 'ACTIVE',
  sourceType: 'OPENAPI31',
  revision: 2,
  applicationId: 'application-1',
  interfaceGroupId: 'group-1',
  providerServiceIdentity: { serviceName: 'orders' },
  currentDefinitionId: 'definition-1',
}

const definition = {
  id: 'definition-1',
  operationId: 'operation-1',
  definitionVersion: 1,
  definitionSha256: 'a'.repeat(64),
  summary: 'Get order',
  tags: ['orders'],
  requestSchema: { type: 'object' },
  responseSchema: { type: 'object' },
  errorSchema: [],
  attributes: {},
  externalAccessible: true,
  createdAt: '2026-08-26T03:00:00Z',
  createdBy: 'admin',
}

beforeEach(() => {
  mocks.operation.mockReset().mockResolvedValue({
    operation: openApiOperation,
    definitions: [definition],
  })
  mocks.operationOpenApi.mockReset().mockResolvedValue({
    operationId: 'operation-1',
    operationKey: 'orders:http:GET:/orders/{id}',
    sourceType: 'OPENAPI31',
    snapshotId: 'snapshot-1',
    openapiVersion: '3.1.0',
    openapiGroup: 'orders',
    path: '/orders/{id}',
    method: 'GET',
    openapiOperationId: 'getOrder',
    requestContentTypes: ['application/json'],
    responseContentTypes: ['application/json'],
    operation: {
      operationId: 'getOrder',
      'x-egon': { permission: 'orders:read' },
    },
    syncedAt: '2026-08-26T03:00:01Z',
  })
  mocks.openapiSnapshotDocument.mockReset().mockResolvedValue({
    snapshotId: 'snapshot-1',
    applicationId: 'application-1',
    buildId: 'build-1',
    openapiVersion: '3.1.0',
    documentSha256: 'b'.repeat(64),
    canonicalSha256: 'a'.repeat(64),
    document: { openapi: '3.1.0' },
    fetchedAt: '2026-08-26T02:59:00Z',
    validatedAt: '2026-08-26T03:00:01Z',
  })
  mocks.updateOperationMetadata.mockReset().mockResolvedValue({ operation: openApiOperation, definitions: [definition] })
  mocks.updateManualDefinition.mockReset().mockResolvedValue({ operation: openApiOperation, definitions: [definition] })
  mocks.deprecateOperation.mockReset().mockResolvedValue({ operation: openApiOperation, definitions: [definition] })
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
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('OperationPage OpenAPI projection', () => {
  it('allows catalog lifecycle actions only for a manual operation', async () => {
    const manual = { ...openApiOperation, sourceType: 'MANUAL' }
    mocks.operation.mockResolvedValue({ operation: manual, definitions: [definition] })
    mocks.updateOperationMetadata.mockResolvedValue({ operation: manual, definitions: [definition] })
    mocks.deprecateOperation.mockResolvedValue({ operation: manual, definitions: [definition] })

    renderPage()

    await screen.findByText('GET /orders/{id}')
    fireEvent.click(screen.getByRole('button', { name: '编辑元数据' }))
    fireEvent.change(screen.getByLabelText('摘要'), { target: { value: 'Updated order summary' } })
    fireEvent.click(screen.getByRole('button', { name: '保存元数据' }))

    await waitFor(() => expect(mocks.updateOperationMetadata).toHaveBeenCalledWith(
      'operation-1',
      expect.objectContaining({ summary: 'Updated order summary', tags: ['orders'] }),
    ))
    fireEvent.click(screen.getByRole('button', { name: '废弃 Operation' }))
    fireEvent.click(screen.getByRole('button', { name: '确认废弃' }))
    await waitFor(() => expect(mocks.deprecateOperation).toHaveBeenCalledWith('operation-1'))
  })

  it('keeps RPC operations read-only and removes secret-like provider fields', async () => {
    mocks.operation.mockResolvedValue({
      operation: {
        ...openApiOperation,
        sourceType: 'RPC_DESCRIPTOR',
        protocol: 'RPC',
        providerServiceIdentity: {
          serviceName: 'orders',
          secret: 'raw-secret-value',
          Authorization: 'raw-authorization-value',
        },
      },
      definitions: [{ ...definition, descriptorSnapshot: { fullMethodName: 'orders.Order/Get' } }],
    })

    renderPage()

    expect(await screen.findByText('RPC_DESCRIPTOR')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '编辑元数据' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '编辑定义' })).not.toBeInTheDocument()
    fireEvent.click(screen.getByText('Provider Service Identity'))
    expect(screen.queryByText('raw-secret-value')).not.toBeInTheDocument()
    expect(screen.queryByText('raw-authorization-value')).not.toBeInTheDocument()
  })

  it('loads the OpenAPI fragment and downloads the immutable snapshot by id', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    vi.stubGlobal('navigator', { clipboard: { writeText } })
    const createObjectURL = vi.fn().mockReturnValue('blob:download')
    const revokeObjectURL = vi.fn()
    vi.stubGlobal('URL', { createObjectURL, revokeObjectURL })

    renderPage()

    await screen.findByText('GET /orders/{id}')
    fireEvent.click(screen.getByRole('tab', { name: 'OpenAPI' }))

    expect(await screen.findByText('OpenAPI Group')).toBeInTheDocument()
    expect(screen.getAllByText('orders').length).toBeGreaterThan(0)
    expect(screen.getAllByText(/getOrder/).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/orders:read/).length).toBeGreaterThan(0)

    fireEvent.click(screen.getByRole('button', { name: '复制 OpenAPI Operation' }))
    await waitFor(() => expect(writeText).toHaveBeenCalledWith(
      expect.stringContaining('"operationId": "getOrder"'),
    ))

    fireEvent.click(screen.getByRole('button', { name: '下载完整 OpenAPI 文档' }))
    await waitFor(() => expect(mocks.openapiSnapshotDocument)
      .toHaveBeenCalledWith('snapshot-1'))
    expect(createObjectURL).toHaveBeenCalled()
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:download')
  })

  it('shows an explicit unavailable state for RPC without querying an OpenAPI fragment', async () => {
    mocks.operation.mockResolvedValue({
      operation: { ...openApiOperation, sourceType: 'RPC_DESCRIPTOR', protocol: 'RPC' },
      definitions: [{ ...definition, descriptorSnapshot: { fullMethodName: 'orders.Order/Get' } }],
    })

    renderPage()

    expect(await screen.findByText('RPC_DESCRIPTOR')).toBeInTheDocument()
    expect(screen.getByText('当前 Operation 没有 OpenAPI source')).toBeInTheDocument()
    expect(mocks.operationOpenApi).not.toHaveBeenCalled()
  })

  it('surfaces a typed OpenAPI fragment failure with an explicit retry', async () => {
    mocks.operationOpenApi.mockRejectedValue(new GatewayApiError(
      409,
      'YUHENG_OPENAPI_SOURCE_NOT_AVAILABLE',
      'current operation has no OpenAPI source',
    ))

    renderPage()

    await screen.findByText('GET /orders/{id}')
    fireEvent.click(screen.getByRole('tab', { name: 'OpenAPI' }))

    expect(await screen.findByText('current operation has no OpenAPI source')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '重试 OpenAPI' })).toBeInTheDocument()
  })
})
