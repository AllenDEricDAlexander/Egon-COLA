import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {ddcApi, DdcApiError, ddcPageApi, setDdcUnauthorizedHandler,} from './client'

const record = (data: unknown) => ({
  success: true, code: 0, status: 'SUCCESS', message: '', data,
  traceId: 'trace-1', timestamp: 1,
})

const pageRecord = <T,>(records: T[], total = records.length) => ({
  success: true,
  code: 0,
  status: 'SUCCESS',
  message: '',
  records,
  page: {
    total,
    pageNo: 2,
    pageSize: 20,
    pages: 2,
    hasNext: false,
    hasPrevious: true,
  },
  traceId: 'trace-page',
  timestamp: 1,
})

describe('ddcApi', () => {
  beforeEach(() => {
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })
  afterEach(() => {
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
  })

  const jsonResponse = (body: unknown, status = 200) =>
    new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

    it('sends cookie credentials and returns data', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(record({ list: [1] })))
    await expect(ddcApi<{ list: number[] }>('/api/v1/ddc/apps')).resolves.toEqual({ list: [1] })
    const [url, init] = vi.mocked(fetch).mock.calls[0]
    expect(url).toBe('/api/v1/ddc/apps')
        expect(init!.credentials).toBe('include')
        expect((init!.headers as Headers).has('Authorization')).toBe(false)
  })

  it('stringifies JSON bodies with content type', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(record(null)))
    await ddcApi('/api/v1/ddc/configs', { method: 'POST', body: { resourceName: 'application.yml' } })
    const [, init] = vi.mocked(fetch).mock.calls[0]
    expect((init!.headers as Headers).get('Content-Type')).toBe('application/json')
    expect(init!.body).toBe(JSON.stringify({ resourceName: 'application.yml' }))
  })

  it('returns page records and forwards AbortSignal', async () => {
    const controller = new AbortController()
    vi.mocked(fetch).mockResolvedValue(
      jsonResponse(pageRecord([{ id: 'b1' }], 21)),
    )

    await expect(ddcPageApi<{ id: string }>(
      '/api/v1/ddc/bizs/page',
      { signal: controller.signal },
    )).resolves.toMatchObject({
      records: [{ id: 'b1' }],
      page: { total: 21, pageNo: 2, pageSize: 20 },
    })

    expect(vi.mocked(fetch).mock.calls[0][1]?.signal)
      .toBe(controller.signal)
  })

  it('accepts ResultRecord failures from the global exception handler', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({
      success: false,
      code: 422,
      status: 'INVALID_REQUEST',
      message: '请求参数无效',
      data: null,
      traceId: 'trace-error',
      timestamp: 1,
    }))

    await expect(ddcPageApi('/api/v1/ddc/bizs/page'))
      .rejects.toMatchObject({ code: '422', traceId: 'trace-error' })
  })

    it('keeps the original AbortSignal after a 401', async () => {
    const controller = new AbortController()
        vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(record(null), 401))

        await expect(ddcApi(
      '/api/v1/ddc/apps',
      { signal: controller.signal },
        )).rejects.toMatchObject({status: 401})

        expect(vi.mocked(fetch).mock.calls[0][1]?.signal)
      .toBe(controller.signal)
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
    expect(error.message).toBe('统一身份登录已过期，请重新登录')
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
