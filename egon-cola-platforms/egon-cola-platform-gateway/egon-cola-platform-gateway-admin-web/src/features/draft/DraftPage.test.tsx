import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { gatewayApi } from '../../api/gatewayApi'
import type {
  DraftMutationResult,
  GatewayDraft,
  GatewayRouteTransportPolicy,
  OperationDetail,
} from '../../api/types'
import { DraftPage } from './DraftPage'

vi.mock('antd', async (importOriginal) => {
  const actual = await importOriginal<typeof import('antd')>()
  return {
    ...actual,
    Modal: ({
      children,
      confirmLoading,
      okButtonProps,
      onCancel,
      onOk,
      open,
      title,
    }: {
      children?: ReactNode
      confirmLoading?: boolean
      okButtonProps?: { disabled?: boolean }
      onCancel?: () => void
      onOk?: () => void
      open?: boolean
      title?: ReactNode
    }) => open ? (
      <section aria-label={String(title)}>
        <h2>{title}</h2>
        {children}
        <button type="button" onClick={onCancel}>Cancel</button>
        <button
          type="button"
          disabled={Boolean(confirmLoading || okButtonProps?.disabled)}
          onClick={onOk}
        >
          OK
        </button>
      </section>
    ) : null,
  }
})

vi.mock('../../api/gatewayApi', () => ({
  gatewayApi: {
    draft: vi.fn(),
    draftDiff: vi.fn(),
    operation: vi.fn(),
    saveRoute: vi.fn(),
    savePolicy: vi.fn(),
    deleteRoute: vi.fn(),
    deletePolicy: vi.fn(),
  },
}))

vi.mock('../../app/capabilities', () => ({
  useCapability: vi.fn(() => true),
}))

const originalTransportPolicy: GatewayRouteTransportPolicy = {
  profile: 'OPENAI_HTTP',
  transportProtocol: 'HTTP',
  requestBodyMode: 'STREAMING',
  responseMode: 'AUTO_STREAM',
  maxRequestBodyBytes: 536_870_912,
  connectTimeoutMs: 10_000,
  responseHeaderTimeoutMs: 120_000,
  streamIdleTimeoutMs: 90_000,
  totalTimeoutMs: 1_800_000,
  websocketIdleTimeoutMs: 300_000,
  websocketMaxFrameBytes: 16_777_216,
  bodyLogEnabled: false,
  retryEnabled: false,
  futureOption: false,
}

const draft: GatewayDraft = {
  gatewayGroupId: 'group-1',
  revision: 7,
  status: 'EDITING',
  routes: [
    {
      routeId: 'route-a',
      operationId: 'operation-a',
      enabled: true,
      routeContent: {
        host: 'a.example.com',
        httpMethod: 'POST',
        pathPattern: '/v1/a/**',
        accessZones: ['PUBLIC'],
        priority: 0,
        transportPolicy: originalTransportPolicy,
      },
    },
    {
      routeId: 'route-b',
      operationId: 'operation-b',
      enabled: true,
      routeContent: {
        host: 'b.example.com',
        httpMethod: 'GET',
        pathPattern: '/v1/b/**',
        accessZones: ['INTERNAL'],
        priority: 1,
      },
    },
  ],
  policies: [],
  updatedAt: '2026-07-30T06:00:00Z',
}

const operationDetail = (
  operationId: string,
  protocol: 'HTTP' | 'RPC' = 'HTTP',
): OperationDetail => ({
  operation: {
    id: operationId,
    applicationId: 'application-1',
    interfaceGroupId: 'interface-group-1',
    operationKey: `POST /${operationId}`,
    protocol,
    methodIdentity: `POST /${operationId}`,
    providerServiceIdentity: {
      env: 'test',
      namespace: 'gateway',
      protocol,
      serviceName: 'openai-compatible-provider',
      group: 'default',
      version: '1.0.0',
      transport: 'HTTP',
    },
    externalAccessible: true,
    lifecycleStatus: 'ACTIVE',
    sourceType: 'STARTER',
    revision: 3,
  },
  definitions: [],
})

const mutationResult = (routeId: string): DraftMutationResult => ({
  revision: 8,
  resourceId: routeId,
  replayed: false,
})

const deferred = <T,>() => {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })
  return { promise, reject, resolve }
}

const renderDraftPage = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      mutations: { retry: false },
      queries: { retry: false },
    },
  })
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/gateway-groups/group-1/draft/routes']}>
        <Routes>
          <Route path="/gateway-groups/:groupId/draft/routes" element={<DraftPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

const openRoute = async (index: number) => {
  await screen.findByText(index === 0 ? 'route-a' : 'route-b')
  const editButtons = await screen.findAllByText(/编\s*辑/)
  fireEvent.click(editButtons[index])
  await screen.findByText('Route Transport')
}

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
  vi.mocked(gatewayApi.draft).mockResolvedValue(draft)
  vi.mocked(gatewayApi.draftDiff).mockResolvedValue({})
  vi.mocked(gatewayApi.saveRoute).mockResolvedValue(mutationResult('route-a'))
})

afterEach(() => {
  cleanup()
  vi.clearAllMocks()
  vi.unstubAllGlobals()
})

describe('DraftPage route editor state integrity', () => {
  it('waits for Operation protocol before saving every existing transport field', async () => {
    const operationLookup = deferred<OperationDetail>()
    vi.mocked(gatewayApi.operation)
      .mockReturnValueOnce(operationLookup.promise)
      .mockResolvedValueOnce(operationDetail('operation-a'))
    renderDraftPage()

    await openRoute(0)
    const confirm = screen.getByText('OK').closest('button')!
    expect(confirm).toBeDisabled()

    await act(async () => {
      operationLookup.resolve(operationDetail('operation-a'))
      await operationLookup.promise
    })
    await screen.findByText('Operation Protocol：HTTP')
    await waitFor(() => expect(confirm).not.toBeDisabled())
    fireEvent.click(confirm)

    await waitFor(() => expect(gatewayApi.saveRoute).toHaveBeenCalledTimes(1))
    expect(vi.mocked(gatewayApi.saveRoute).mock.calls[0][2].content.transportPolicy)
      .toEqual(originalTransportPolicy)
  })

  it('hides HTTP transport controls for an RPC Operation', async () => {
    vi.mocked(gatewayApi.operation).mockResolvedValue(
      operationDetail('operation-a', 'RPC'),
    )
    renderDraftPage()

    await openRoute(0)
    await screen.findByText('Operation Protocol：RPC / gRPC')

    expect(screen.queryByText('Transport Policy')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Transport Protocol')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Request Body Mode')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Response Mode')).not.toBeInTheDocument()
  })

  it('keeps Route B open when Route A save completes after switching editors', async () => {
    const routeASave = deferred<DraftMutationResult>()
    vi.mocked(gatewayApi.operation).mockImplementation((operationId) =>
      Promise.resolve(operationDetail(operationId)))
    vi.mocked(gatewayApi.saveRoute).mockReturnValueOnce(routeASave.promise)
    renderDraftPage()

    await openRoute(0)
    await screen.findByText('Operation Protocol：HTTP')
    fireEvent.click(screen.getByText('OK').closest('button')!)
    await waitFor(() => expect(gatewayApi.saveRoute).toHaveBeenCalledTimes(1))

    fireEvent.click(screen.getByText('Cancel'))
    await waitFor(() => expect(screen.queryByText('Route Transport')).not.toBeInTheDocument())
    await openRoute(1)
    await screen.findByText('Operation Protocol：HTTP')
    const routeId = screen.getByLabelText('Route ID')
    const host = screen.getByLabelText('Host')
    expect(routeId).toHaveValue('route-b')
    fireEvent.change(host, { target: { value: 'b-edited.example.com' } })
    expect(host).toHaveValue('b-edited.example.com')

    await act(async () => {
      routeASave.resolve(mutationResult('route-a'))
      await routeASave.promise
    })

    await waitFor(() => expect(gatewayApi.draft).toHaveBeenCalledTimes(2))
    expect(screen.getByText('Operation Protocol：HTTP')).toBeInTheDocument()
    expect(screen.getByText('Route Transport')).toBeInTheDocument()
    expect(screen.getByLabelText('Route ID')).toHaveValue('route-b')
    expect(screen.getByLabelText('Host')).toHaveValue('b-edited.example.com')
  })

  it('isolates Route B from Route A submit validation rejection', async () => {
    const routeAValidation = deferred<OperationDetail>()
    vi.mocked(gatewayApi.operation)
      .mockResolvedValueOnce(operationDetail('operation-a'))
      .mockReturnValueOnce(routeAValidation.promise)
      .mockResolvedValueOnce(operationDetail('operation-b'))
    renderDraftPage()

    await openRoute(0)
    await screen.findByText('Operation Protocol：HTTP')
    fireEvent.click(screen.getByText('OK'))
    await waitFor(() => expect(gatewayApi.operation).toHaveBeenCalledTimes(2))

    fireEvent.click(screen.getByText('Cancel'))
    await waitFor(() => expect(screen.queryByText('Route Transport')).not.toBeInTheDocument())
    await openRoute(1)
    await screen.findByText('Operation Protocol：HTTP')
    const confirm = screen.getByText('OK').closest('button')!
    const host = screen.getByLabelText('Host')
    fireEvent.change(host, { target: { value: 'b-validation.example.com' } })

    expect(confirm).not.toBeDisabled()
    expect(screen.getByLabelText('Route ID')).toHaveValue('route-b')
    expect(host).toHaveValue('b-validation.example.com')

    await act(async () => {
      routeAValidation.reject(new Error('Route A Operation lookup failed'))
      await routeAValidation.promise.catch(() => undefined)
    })

    await waitFor(() => expect(confirm).not.toBeDisabled())
    expect(screen.getByText('Operation Protocol：HTTP')).toBeInTheDocument()
    expect(screen.queryByText('无法从服务端读取 Operation 协议，请确认 Operation ID。'))
      .not.toBeInTheDocument()
    expect(screen.getByText('Route Transport')).toBeInTheDocument()
    expect(screen.getByLabelText('Route ID')).toHaveValue('route-b')
    expect(screen.getByLabelText('Host')).toHaveValue('b-validation.example.com')
  })

  it('locks only Route A while its submit validation is pending', async () => {
    const routeAValidation = deferred<OperationDetail>()
    vi.mocked(gatewayApi.operation)
      .mockResolvedValueOnce(operationDetail('operation-a'))
      .mockReturnValueOnce(routeAValidation.promise)
      .mockResolvedValueOnce(operationDetail('operation-b'))
    renderDraftPage()

    await openRoute(0)
    await screen.findByText('Operation Protocol：HTTP')
    fireEvent.click(screen.getByText('OK'))
    await waitFor(() => expect(gatewayApi.operation).toHaveBeenCalledTimes(2))

    expect(screen.getByLabelText('Route ID')).toBeDisabled()
    expect(screen.getByLabelText('Operation ID')).toBeDisabled()
    expect(screen.getByLabelText('Host')).toBeDisabled()

    fireEvent.click(screen.getByText('Cancel'))
    await waitFor(() => expect(screen.queryByText('Route Transport')).not.toBeInTheDocument())
    await openRoute(1)
    await screen.findByText('Operation Protocol：HTTP')
    const routeBHost = screen.getByLabelText('Host')

    expect(screen.getByLabelText('Route ID')).not.toBeDisabled()
    expect(screen.getByLabelText('Operation ID')).not.toBeDisabled()
    expect(routeBHost).not.toBeDisabled()
    expect(screen.getByText('OK').closest('button')).not.toBeDisabled()
    fireEvent.change(routeBHost, { target: { value: 'b-validation-pending.example.com' } })
    expect(routeBHost).toHaveValue('b-validation-pending.example.com')

    await act(async () => {
      routeAValidation.reject(new Error('Route A validation cancelled'))
      await routeAValidation.promise.catch(() => undefined)
    })
  })

  it('locks only Route A while its save mutation is pending', async () => {
    const routeASave = deferred<DraftMutationResult>()
    vi.mocked(gatewayApi.operation).mockImplementation((operationId) =>
      Promise.resolve(operationDetail(operationId)))
    vi.mocked(gatewayApi.saveRoute).mockReturnValueOnce(routeASave.promise)
    renderDraftPage()

    await openRoute(0)
    await screen.findByText('Operation Protocol：HTTP')
    fireEvent.click(screen.getByText('OK'))
    await waitFor(() => expect(gatewayApi.saveRoute).toHaveBeenCalledTimes(1))

    expect(screen.getByLabelText('Route ID')).toBeDisabled()
    expect(screen.getByLabelText('Operation ID')).toBeDisabled()
    expect(screen.getByLabelText('Host')).toBeDisabled()

    fireEvent.click(screen.getByText('Cancel'))
    await waitFor(() => expect(screen.queryByText('Route Transport')).not.toBeInTheDocument())
    await openRoute(1)
    await screen.findByText('Operation Protocol：HTTP')
    const routeBHost = screen.getByLabelText('Host')

    expect(screen.getByLabelText('Route ID')).not.toBeDisabled()
    expect(screen.getByLabelText('Operation ID')).not.toBeDisabled()
    expect(routeBHost).not.toBeDisabled()
    expect(screen.getByText('OK').closest('button')).not.toBeDisabled()
    fireEvent.change(routeBHost, { target: { value: 'b-save-pending.example.com' } })
    expect(routeBHost).toHaveValue('b-save-pending.example.com')

    await act(async () => {
      routeASave.resolve(mutationResult('route-a'))
      await routeASave.promise
    })
    await waitFor(() => expect(gatewayApi.draft).toHaveBeenCalledTimes(2))
    expect(screen.getByLabelText('Host')).toHaveValue('b-save-pending.example.com')
  })

  it('does not restart a READY Operation lookup during submit validation', async () => {
    const routeAValidation = deferred<OperationDetail>()
    vi.mocked(gatewayApi.operation)
      .mockResolvedValueOnce(operationDetail('operation-a'))
      .mockReturnValueOnce(routeAValidation.promise)
      .mockResolvedValueOnce(operationDetail('operation-a'))
    renderDraftPage()

    await openRoute(0)
    await screen.findByText('Operation Protocol：HTTP')
    const operationId = screen.getByLabelText('Operation ID')
    fireEvent.click(screen.getByText('OK'))
    await waitFor(() => expect(gatewayApi.operation).toHaveBeenCalledTimes(2))
    await waitFor(() => expect(screen.getByText('OK').closest('button')).toBeDisabled())

    fireEvent.blur(operationId)
    expect(gatewayApi.operation).toHaveBeenCalledTimes(2)

    await act(async () => {
      routeAValidation.resolve(operationDetail('operation-a'))
      await routeAValidation.promise
    })
    await waitFor(() => expect(gatewayApi.saveRoute).toHaveBeenCalledTimes(1))
  })
})
