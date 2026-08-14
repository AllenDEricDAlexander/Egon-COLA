import type {PageResultRecord, ResultEnvelope} from './types'

type UnauthorizedHandler = () => void

let unauthorizedHandler: UnauthorizedHandler = () => {}

export const setDdcUnauthorizedHandler = (handler: UnauthorizedHandler): void => {
  unauthorizedHandler = handler
}

export class DdcApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
    readonly traceId?: string,
  ) {
    super(message)
    this.name = 'DdcApiError'
  }

  get category():
    | 'UNAUTHENTICATED'
    | 'FORBIDDEN'
    | 'NOT_FOUND'
    | 'CONFLICT'
    | 'VALIDATION'
    | 'SERVER'
    | 'NETWORK'
    | 'UNKNOWN' {
    if (this.status === 0) return 'NETWORK'
    if (this.status === 401) return 'UNAUTHENTICATED'
    if (this.status === 403) return 'FORBIDDEN'
    if (this.status === 404) return 'NOT_FOUND'
    if (this.status === 409) return 'CONFLICT'
    if (this.status === 422) return 'VALIDATION'
    if (this.status >= 500) return 'SERVER'
    return 'UNKNOWN'
  }
}

export type DdcRequestOptions = {
  method?: string
  body?: unknown
  signal?: AbortSignal
}

const requestEnvelope = async (
  path: string,
  options: DdcRequestOptions,
): Promise<ResultEnvelope & Record<string, unknown>> => {
  const headers = new Headers()
  let body: string | undefined
  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json')
    body = JSON.stringify(options.body)
  }
    const request = () => fetch(`${import.meta.env.VITE_GATEWAY_ORIGIN ?? ''}${path}`, {
    method: options.method ?? 'GET',
    headers,
    body,
        credentials: 'include',
    signal: options.signal,
  })

  let response: Response
  try {
    response = await request()
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw error
    }
    throw new DdcApiError(
      0,
      'DDC_ADMIN_WEB_UPSTREAM_UNAVAILABLE',
      '无法连接 DDC 管理端',
    )
  }

  const payload = (await response.json().catch(() => ({}))) as
    Partial<ResultEnvelope> & Record<string, unknown>
  if (response.status === 401) {
    unauthorizedHandler()
    throw new DdcApiError(401, 'UNAUTHORIZED', '统一身份登录已过期，请重新登录', payload.traceId)
  }
  if (!response.ok || payload.success === false) {
    const errorStatus = response.ok ? 500 : response.status
    throw new DdcApiError(
      errorStatus,
      String(payload.code ?? errorStatus),
      payload.message || String(payload.code) || `请求失败 (${response.status})`,
      payload.traceId,
    )
  }
  return payload as ResultEnvelope & Record<string, unknown>
}

export async function ddcApi<T>(
  path: string,
  options: DdcRequestOptions = {},
): Promise<T> {
  const payload = await requestEnvelope(path, options)
  return payload.data as T
}

export async function ddcPageApi<T>(
  path: string,
  options: DdcRequestOptions = {},
): Promise<PageResultRecord<T>> {
  const payload = await requestEnvelope(path, options)
  if (!Array.isArray(payload.records)
      || payload.page === null
      || typeof payload.page !== 'object') {
    throw new DdcApiError(
      500,
      'DDC_INVALID_PAGE_RESPONSE',
      '分页响应格式无效',
      payload.traceId,
    )
  }
  return payload as PageResultRecord<T>
}
