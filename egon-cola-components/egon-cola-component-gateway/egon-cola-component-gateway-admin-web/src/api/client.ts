import type { AdminErrorBody } from './types'
import { createLogicalTrace, traceHeaders, type LogicalTrace } from './trace'
import { refreshAccessToken, tokenStore } from '../auth/tokenStore'

const API_BASE_URL = import.meta.env.VITE_GATEWAY_ADMIN_API_BASE_URL ?? ''
const CONTRACT_VERSION = 'v1'

export type ApiRequest = Omit<RequestInit, 'body'> & {
  body?: unknown
  trace?: LogicalTrace
  idempotencyKey?: string
}

export class GatewayApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
    readonly traceId?: string,
    readonly currentRevision?: number,
    readonly fields: AdminErrorBody['errors'] = [],
  ) {
    super(message)
    this.name = 'GatewayApiError'
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

const decodeError = async (response: Response): Promise<GatewayApiError> => {
  let body: AdminErrorBody
  try {
    body = (await response.json()) as AdminErrorBody
  } catch {
    body = {}
  }
  return new GatewayApiError(
    response.status,
    body.code ?? `HTTP_${response.status}`,
    body.message ?? `Gateway Admin 请求失败（${response.status}）`,
    body.traceId ?? response.headers.get('X-Trace-Id') ?? undefined,
    body.currentRevision,
    body.errors,
  )
}

export const apiRequest = async <T>(
  path: string,
  request: ApiRequest = {},
): Promise<T> => {
  const trace = request.trace ?? createLogicalTrace()
  const headers = new Headers(request.headers)
  headers.set('Accept', 'application/json')
  headers.set('X-Gateway-Contract-Version', CONTRACT_VERSION)
  const accessToken = tokenStore.get()?.accessToken
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
  Object.entries(traceHeaders(trace)).forEach(([name, value]) => headers.set(name, value))
  if (request.idempotencyKey) {
    headers.set('Idempotency-Key', request.idempotencyKey)
  }
  let body: BodyInit | undefined
  if (request.body !== undefined) {
    headers.set('Content-Type', 'application/json')
    body = JSON.stringify(request.body)
  }
  try {
    let response = await fetch(`${API_BASE_URL}${path}`, {
      ...request,
      body,
      headers,
    })
    if (response.status === 401 && accessToken) {
      try {
        headers.set('Authorization', `Bearer ${await refreshAccessToken()}`)
        response = await fetch(`${API_BASE_URL}${path}`, {
          ...request,
          body,
          headers,
        })
      } catch {
        tokenStore.clear()
      }
    }
    if (!response.ok) {
      throw await decodeError(response)
    }
    if (response.status === 204) {
      return undefined as T
    }
    const responseContract = response.headers.get('X-Gateway-Contract-Version')
    if (responseContract && responseContract !== CONTRACT_VERSION && request.method !== 'GET') {
      throw new GatewayApiError(
        409,
        'GATEWAY_ADMIN_CONTRACT_MISMATCH',
        `服务端契约 ${responseContract} 与前端 ${CONTRACT_VERSION} 不兼容`,
        trace?.traceId,
      )
    }
    return (await response.json()) as T
  } catch (error) {
    if (error instanceof GatewayApiError || error instanceof DOMException) {
      throw error
    }
    throw new GatewayApiError(
      0,
      'GATEWAY_ADMIN_NETWORK_ERROR',
      error instanceof Error ? error.message : 'Gateway Admin 网络请求失败',
      trace?.traceId,
    )
  }
}
