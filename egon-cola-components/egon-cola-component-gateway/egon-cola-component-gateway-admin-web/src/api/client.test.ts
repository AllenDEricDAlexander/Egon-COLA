import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiRequest, GatewayApiError } from './client'
import { createLogicalTrace } from './trace'

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('typed API client', () => {
  it('injects one logical trace and a valid W3C traceparent', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)
    const trace = createLogicalTrace()

    await apiRequest('/api/test', { trace })

    const init = fetchMock.mock.calls[0][1] as RequestInit
    const headers = init.headers as Headers
    expect(headers.get('X-Trace-Id')).toMatch(/^[0-9a-f]{32}$/)
    expect(headers.get('traceparent')).toMatch(
      /^00-[0-9a-f]{32}-[0-9a-f]{16}-01$/,
    )
    expect(headers.get('X-Gateway-Contract-Version')).toBe('v1')
  })

  it('maps revision conflicts without discarding server evidence', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            code: 'GATEWAY_ADMIN_REVISION_CONFLICT',
            message: 'stale draft',
            currentRevision: 9,
            traceId: 'a'.repeat(32),
          }),
          { status: 409, headers: { 'Content-Type': 'application/json' } },
        ),
      ),
    )

    await expect(apiRequest('/api/test')).rejects.toMatchObject({
      category: 'CONFLICT',
      currentRevision: 9,
      traceId: 'a'.repeat(32),
    } satisfies Partial<GatewayApiError>)
  })
})
