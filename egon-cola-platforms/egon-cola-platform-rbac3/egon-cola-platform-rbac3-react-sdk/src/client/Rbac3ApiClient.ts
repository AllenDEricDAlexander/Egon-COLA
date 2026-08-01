import {
  Rbac3RequestError,
  type Rbac3ErrorCode,
  type Rbac3ErrorResponse,
} from '../errors'
import type {
  ActiveRoleSetView,
  ApiEnvelope,
  BootstrapView,
  Rbac3Client,
  RefreshResult,
  ReplaceActiveRolesRequest,
  ReplaceActiveRolesResult,
  RoleActivationCandidateView,
} from '../types'
import { InMemoryAccessTokenStore } from '../auth/InMemoryAccessTokenStore'

export interface Rbac3ApiClientOptions {
  readonly basePath?: string
  readonly fetch?: typeof globalThis.fetch
  readonly accessTokenStore: InMemoryAccessTokenStore
  readonly idempotencyKey?: () => string
}

/** Typed HTTP adapter with one refresh retry and cookie-based refresh transport. */
export class Rbac3ApiClient implements Rbac3Client {
  private readonly basePath: string
  private readonly fetcher: typeof globalThis.fetch
  private readonly tokenStore: InMemoryAccessTokenStore
  private readonly idempotencyKey: () => string
  private refreshPromise: Promise<RefreshResult> | null = null

  constructor(options: Rbac3ApiClientOptions) {
    this.basePath = normalizeBasePath(options.basePath ?? '')
    this.fetcher = options.fetch ?? globalThis.fetch.bind(globalThis)
    this.tokenStore = options.accessTokenStore
    this.idempotencyKey = options.idempotencyKey ?? defaultIdempotencyKey
  }

  getActivationCandidates(): Promise<RoleActivationCandidateView> {
    return this.request('/api/rbac3/v1/role-activation/candidates')
  }

  getActiveRoles(): Promise<ActiveRoleSetView> {
    return this.request('/api/rbac3/v1/role-activation/current')
  }

  replaceActiveRoles(
    request: ReplaceActiveRolesRequest,
  ): Promise<ReplaceActiveRolesResult> {
    return this.request('/api/rbac3/v1/role-activation/current', {
      method: 'PUT',
      headers: { 'Idempotency-Key': this.idempotencyKey() },
      body: JSON.stringify(request),
    })
  }

  getBootstrap(): Promise<BootstrapView> {
    return this.request('/api/rbac3/v1/auth/bootstrap')
  }

  refresh(): Promise<RefreshResult> {
    if (this.refreshPromise === null) {
      this.refreshPromise = this.refreshDirect()
        .finally(() => {
          this.refreshPromise = null
        })
    }
    return this.refreshPromise
  }

  async logout(): Promise<void> {
    try {
      await this.request<unknown>('/api/rbac3/v1/auth/logout', {
        method: 'POST',
      }, false)
    } finally {
      this.tokenStore.clear()
    }
  }

  private async refreshDirect(): Promise<RefreshResult> {
    const result = await this.request<RefreshResult>(
      '/api/rbac3/v1/auth/refresh',
      { method: 'POST', body: '{}' },
      false,
      false,
    )
    this.tokenStore.set(result.accessToken)
    return result
  }

  private async request<T>(
    path: string,
    init: RequestInit = {},
    allowRefresh = true,
    withBearer = true,
  ): Promise<T> {
    let response: Response
    try {
      response = await this.fetcher(`${this.basePath}${path}`, {
        ...init,
        credentials: 'include',
        headers: this.headers(init.headers, withBearer),
      })
    } catch (cause) {
      throw new Rbac3RequestError({
        status: 0,
        code: 'NETWORK_ERROR',
        message: cause instanceof Error ? cause.message : 'RBAC3 network request failed',
        retryable: true,
      })
    }

    if (response.status === 401 && allowRefresh) {
      await this.refresh()
      return this.request(path, init, false, withBearer)
    }
    if (!response.ok) {
      throw await toRequestError(response)
    }

    const envelope = await readJson<ApiEnvelope<T>>(response)
    if (envelope === null || typeof envelope !== 'object' || !('data' in envelope)) {
      throw new Rbac3RequestError({
        status: response.status,
        code: 'INVALID_RESPONSE',
        message: 'RBAC3 response envelope is invalid',
        retryable: false,
      })
    }
    return envelope.data
  }

  private headers(source: HeadersInit | undefined, withBearer: boolean): Headers {
    const headers = new Headers(source)
    headers.set('Accept', 'application/json')
    if (!headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json')
    }
    const accessToken = withBearer ? this.tokenStore.get() : null
    if (accessToken !== null) {
      headers.set('Authorization', `Bearer ${accessToken}`)
    } else {
      headers.delete('Authorization')
    }
    return headers
  }
}

const normalizeBasePath = (value: string): string => {
  const trimmed = value.trim()
  return trimmed.endsWith('/') ? trimmed.slice(0, -1) : trimmed
}

const defaultIdempotencyKey = (): string => {
  if (typeof globalThis.crypto?.randomUUID === 'function') {
    return globalThis.crypto.randomUUID()
  }
  throw new Error('crypto.randomUUID is required for RBAC3 mutation commands')
}

const readJson = async <T>(response: Response): Promise<T | null> => {
  try {
    return await response.json() as T
  } catch {
    return null
  }
}

const toRequestError = async (response: Response): Promise<Rbac3RequestError> => {
  const body = await readJson<Rbac3ErrorResponse>(response)
  const code = body?.error?.code as Rbac3ErrorCode | undefined
  return new Rbac3RequestError({
    status: response.status,
    code: code ?? 'INVALID_RESPONSE',
    message: body?.error?.message ?? 'RBAC3 request was rejected',
    retryable: body?.error?.retryable ?? response.status >= 500,
    traceId: body?.meta?.traceId,
  })
}
