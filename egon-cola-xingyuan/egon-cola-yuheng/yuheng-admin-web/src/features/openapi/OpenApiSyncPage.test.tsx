import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {cleanup, fireEvent, render, screen, waitFor} from '@testing-library/react'
import {MemoryRouter} from 'react-router-dom'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {gatewayApi} from '../../api/gatewayApi'
import {OpenApiSyncPage} from './OpenApiSyncPage'

vi.mock('../../api/gatewayApi', () => ({
  gatewayApi: {
    openapiSyncStates: vi.fn(),
    openapiSnapshotDocument: vi.fn(),
  },
}))

vi.mock('../../hooks/useGatewayScopeBindings', () => ({
  useGatewayScopeBindings: () => ({
    data: [{
      bindingId: 'binding-1', bizCode: 'retail', namespace: 'yuheng', env: 'test', appCode: 'orders',
      appName: 'Orders', connected: true,
    }],
    isLoading: false,
    error: null,
    refetch: vi.fn(),
  }),
}))

const state = {
  id: 'sync-1',
  applicationId: 'application-1',
  buildId: 'build-1',
  artifactVersion: '1.0.0',
  openapiGroup: 'orders',
  sourceType: 'OPENAPI31',
  status: 'VALID',
  snapshotId: 'snapshot-1',
  definitionSetId: 'set-1',
  operationCount: 3,
  schemaCount: 2,
  canonicalSha256: 'a'.repeat(64),
  lastErrorCode: null,
  lastErrorMessage: null,
  lastAttemptAt: null,
  lastSuccessAt: '2026-08-26T03:00:00Z',
  nextRetryAt: null,
}

describe('OpenApiSyncPage', () => {
  afterEach(cleanup)
  beforeEach(() => {
    vi.stubGlobal('matchMedia', vi.fn().mockImplementation(() => ({
      matches: false,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })))
    vi.mocked(gatewayApi.openapiSyncStates).mockReset().mockResolvedValue([state])
    vi.mocked(gatewayApi.openapiSnapshotDocument).mockReset().mockResolvedValue({
      snapshotId: 'snapshot-1',
      applicationId: 'application-1',
      buildId: 'build-1',
      openapiVersion: '3.1.0',
      documentSha256: 'b'.repeat(64),
      canonicalSha256: 'a'.repeat(64),
      document: {openapi: '3.1.0', paths: {}},
      fetchedAt: '2026-08-26T02:59:00Z',
      validatedAt: '2026-08-26T03:00:00Z',
    })
  })

  it('shows sync state and opens the authoritative snapshot document', async () => {
    render(
      <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
        <MemoryRouter initialEntries={['/openapi-sync']}>
          <OpenApiSyncPage />
        </MemoryRouter>
      </QueryClientProvider>,
    )

    await waitFor(() => expect(screen.getByText('build-1')).toBeInTheDocument())
    expect(gatewayApi.openapiSyncStates).toHaveBeenCalledWith({}, expect.anything())
    fireEvent.click(screen.getByRole('button', {name: '查看文档'}))
    await waitFor(() => expect(screen.getByText('3.1.0')).toBeInTheDocument())
    expect(gatewayApi.openapiSnapshotDocument).toHaveBeenCalledWith('snapshot-1', expect.anything())
  })

  it.each([['STALE', false], ['FETCH_FAILED', true]] as const)(
    'reports errors for %s without treating retired builds as failures', async (status, warns) => {
      vi.mocked(gatewayApi.openapiSyncStates).mockResolvedValue([
        state,
        {...state, id: 'old-sync', buildId: 'old-build', status, lastErrorCode: 'PROVIDER_ERROR'},
      ])
      render(
        <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
          <MemoryRouter><OpenApiSyncPage /></MemoryRouter>
        </QueryClientProvider>,
      )
      await screen.findByText(status)
      expect(screen.getByText('PROVIDER_ERROR')).toBeInTheDocument()
      await waitFor(() => expect(screen.queryByText('存在同步失败记录') !== null).toBe(warns))
    },
  )
})
