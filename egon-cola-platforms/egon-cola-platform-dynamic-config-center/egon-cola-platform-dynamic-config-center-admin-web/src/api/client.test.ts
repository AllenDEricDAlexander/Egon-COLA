import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DdcApiError, ddcApi, setDdcTokenProvider, setDdcUnauthorizedHandler } from './client'

const record = (data: unknown) => ({
  success: true, code: 0, status: 'SUCCESS', message: '', data,
  traceId: 'trace-1', timestamp: 1,
})

describe('ddcApi', () => {
  beforeEach(() => {
    setDdcTokenProvider(() => 'token-1')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })
  afterEach(() => vi.unstubAllGlobals())

  const jsonResponse = (body: unknown, status = 200) =>
    new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

  it('sends bearer token and returns data', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(record({ list: [1] })))
    await expect(ddcApi<{ list: number[] }>('/api/v1/ddc/apps')).resolves.toEqual({ list: [1] })
    const [url, init] = vi.mocked(fetch).mock.calls[0]
    expect(url).toBe('/api/v1/ddc/apps')
    expect((init!.headers as Headers).get('Authorization')).toBe('Bearer token-1')
  })

  it('stringifies JSON bodies with content type', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(record(null)))
    await ddcApi('/api/v1/ddc/configs', { method: 'POST', body: { configKey: 'a' } })
    const [, init] = vi.mocked(fetch).mock.calls[0]
    expect((init!.headers as Headers).get('Content-Type')).toBe('application/json')
    expect(init!.body).toBe(JSON.stringify({ configKey: 'a' }))
  })

  it('throws DdcApiError with backend message on success=false', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ success: false, code: 500, status: 'FAIL', message: '配置格式无效', data: null, traceId: 't', timestamp: 1 }, 200))
    const error = await ddcApi('/api/v1/ddc/apps').catch((e) => e as DdcApiError) as DdcApiError
    expect(error).toBeInstanceOf(DdcApiError)
    expect(error.message).toBe('配置格式无效')
    expect(error.category).toBe('SERVER')
  })

  it('on 401 calls the unauthorized handler and throws', async () => {
    const handler = vi.fn()
    setDdcUnauthorizedHandler(handler)
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ success: false, code: 401, status: 'UNAUTHORIZED', message: 'jwt expired', data: null, traceId: 't', timestamp: 1 }, 401))
    const error = await ddcApi('/api/v1/ddc/apps').catch((e) => e as DdcApiError) as DdcApiError
    expect(handler).toHaveBeenCalledTimes(1)
    expect(error.message).toBe('登录已过期，请重新粘贴 Access Token')
    expect(error.category).toBe('UNAUTHENTICATED')
  })

  it('maps network failures to NETWORK category', async () => {
    vi.mocked(fetch).mockRejectedValue(new TypeError('fetch failed'))
    const error = await ddcApi('/api/v1/ddc/apps').catch((e) => e as DdcApiError) as DdcApiError
    expect(error.status).toBe(0)
    expect(error.category).toBe('NETWORK')
    expect(error.code).toBe('DDC_ADMIN_WEB_UPSTREAM_UNAVAILABLE')
  })
})
