import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiRequest, GatewayApiError } from './client'
import { createLogicalTrace } from './trace'
import { tokenStore } from '../auth/tokenStore'

afterEach(() => {
  tokenStore.clear()
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
    expect(headers.has('X-Trace-Id')).toBe(false)
    expect(headers.get('traceparent')).toMatch(
      /^00-[0-9a-f]{32}-[0-9a-f]{16}-01$/,
    )
    expect(headers.get('x-egon-request-id')).toMatch(/^[0-9a-f]{32}$/)
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

  it('uses the persisted bearer token and never trusts an actor header', async () => {
    tokenStore.set({ accessToken: 'signed-jwt' }, false)
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await apiRequest('/api/test')

    const headers = (fetchMock.mock.calls[0][1] as RequestInit).headers as Headers
    expect(headers.get('Authorization')).toBe('Bearer signed-jwt')
    expect(headers.has('X-Admin-Actor-Id')).toBe(false)
  })
})
