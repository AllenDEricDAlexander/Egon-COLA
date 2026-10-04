import type {PageResultRecord, ResultEnvelope} from './types'

type UnauthorizedHandler = () => void
type TenantMismatchHandler = () => void

export type DdcRequestContext = Readonly<{
  tenantId?: string
  sessionGeneration: number
}>

const TENANT_HEADER = 'X-Egon-Tianshu-Tenant'

let unauthorizedHandler: UnauthorizedHandler = () => {}
let tenantMismatchHandler: TenantMismatchHandler = () => {}
let requestContext: DdcRequestContext = { sessionGeneration: 0 }

export const setDdcUnauthorizedHandler = (handler: UnauthorizedHandler): void => {
  unauthorizedHandler = handler
}

export const setDdcTenantMismatchHandler = (handler: TenantMismatchHandler): void => {
  tenantMismatchHandler = handler
}

export const beginDdcSession = (): number => {
  requestContext = { sessionGeneration: requestContext.sessionGeneration + 1 }
  return requestContext.sessionGeneration
}

export const setDdcExpectedTenantId = (tenantId?: string): void => {
  requestContext = {
    ...requestContext,
    tenantId: tenantId || undefined,
  }
}

export const getDdcRequestContext = (): DdcRequestContext => ({ ...requestContext })

export const isCurrentDdcSession = (sessionGeneration: number): boolean =>
  requestContext.sessionGeneration === sessionGeneration

export const requireDdcExpectedTenantId = (): string => {
  if (!requestContext.tenantId) {
    throw new Error('A verified Tianshu tenant is required before calling an AI endpoint')
  }
  return requestContext.tenantId
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
  headers?: HeadersInit
}

const requestEnvelope = async (
  path: string,
  options: DdcRequestOptions,
): Promise<ResultEnvelope & Record<string, unknown>> => {
  const requestContextAtStart = getDdcRequestContext()
  const headers = new Headers(options.headers)
  let body: string | undefined
  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json')
    body = JSON.stringify(options.body)
  }
  headers.delete(TENANT_HEADER)
  if (requestContextAtStart.tenantId) {
    headers.set(TENANT_HEADER, encodeURIComponent(requestContextAtStart.tenantId))
  }
  const request = () => fetch(`${import.meta.env.VITE_YUHENG_ORIGIN ?? ''}${path}`, {
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
      'TIANSHU_ADMIN_WEB_UPSTREAM_UNAVAILABLE',
      '无法连接 Tianshu 管理端',
    )
  }

  if (!isCurrentDdcSession(requestContextAtStart.sessionGeneration)) {
    throw new DOMException('Response belongs to a previous Tianshu session', 'AbortError')
  }

  const payload = (await response.json().catch(() => ({}))) as
    Partial<ResultEnvelope> & Record<string, unknown>
  if (!isCurrentDdcSession(requestContextAtStart.sessionGeneration)) {
    throw new DOMException('Response belongs to a previous Tianshu session', 'AbortError')
  }
  if (response.status === 401) {
    unauthorizedHandler()
    throw new DdcApiError(401, 'UNAUTHORIZED', '统一身份登录已过期，请重新登录', payload.traceId)
  }
  if (response.status === 403 && String(payload.code) === 'TIANSHU_ADMIN_TENANT_MISMATCH') {
    tenantMismatchHandler()
    throw new DdcApiError(
      403,
      'TIANSHU_ADMIN_TENANT_MISMATCH',
      String(payload.message ?? '当前身份租户已变化，请重新加载页面'),
      payload.traceId,
    )
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
      'TIANSHU_INVALID_PAGE_RESPONSE',
      '分页响应格式无效',
      payload.traceId,
    )
  }
  return payload as PageResultRecord<T>
}
