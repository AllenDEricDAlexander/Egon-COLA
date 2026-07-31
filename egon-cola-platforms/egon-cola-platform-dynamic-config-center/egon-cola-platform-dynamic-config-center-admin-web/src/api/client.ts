import type { ResultRecord } from './types'

type TokenProvider = () => string
type UnauthorizedHandler = () => void

let tokenProvider: TokenProvider = () => ''
let unauthorizedHandler: UnauthorizedHandler = () => {}

export const setDdcTokenProvider = (provider: TokenProvider): void => {
  tokenProvider = provider
}

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

export type DdcRequestOptions = { method?: string; body?: unknown }

export async function ddcApi<T>(path: string, options: DdcRequestOptions = {}): Promise<T> {
  const headers = new Headers()
  headers.set('Authorization', `Bearer ${tokenProvider()}`)
  let body: string | undefined
  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json')
    body = JSON.stringify(options.body)
  }
  let response: Response
  try {
    response = await fetch(path, { method: options.method ?? 'GET', headers, body })
  } catch {
    throw new DdcApiError(0, 'DDC_ADMIN_WEB_UPSTREAM_UNAVAILABLE', '无法连接 DDC 管理端')
  }
  const payload = (await response.json().catch(() => ({}))) as Partial<ResultRecord<unknown>>
  if (response.status === 401) {
    unauthorizedHandler()
    throw new DdcApiError(401, 'UNAUTHORIZED', '登录已过期，请重新粘贴 Access Token', payload.traceId)
  }
  if (!response.ok || payload.success === false) {
    // 后端业务失败为 HTTP 2xx + success=false，归类为 SERVER
    const errorStatus = response.ok ? 500 : response.status
    throw new DdcApiError(
      errorStatus,
      String(payload.code ?? errorStatus),
      payload.message || String(payload.code) || `请求失败 (${response.status})`,
      payload.traceId,
    )
  }
  return payload.data as T
}
