import {type Rbac3ErrorCode, type Rbac3ErrorResponse, Rbac3RequestError,} from '../errors'
import type {
    ActiveRoleSetView,
    ApiEnvelope,
    BootstrapView,
    Rbac3Client,
    ReplaceActiveRolesRequest,
    ReplaceActiveRolesResult,
    RoleActivationCandidateView,
} from '../types'

export interface Rbac3ApiClientOptions {
  readonly basePath?: string
  readonly fetch?: typeof globalThis.fetch
}

/** Typed authorization HTTP adapter. Identity cookies are owned by Gateway/IdP. */
export class Rbac3ApiClient implements Rbac3Client {
  private readonly basePath: string
  private readonly fetcher: typeof globalThis.fetch

    constructor(options: Rbac3ApiClientOptions = {}) {
    this.basePath = normalizeBasePath(options.basePath ?? '')
    this.fetcher = options.fetch ?? globalThis.fetch.bind(globalThis)
  }

  getActivationCandidates(): Promise<RoleActivationCandidateView> {
    return this.request('/api/rbac3/v1/auth/role-activation-candidates')
  }

  getActiveRoles(): Promise<ActiveRoleSetView> {
    return this.request('/api/rbac3/v1/auth/role-activations')
  }

  replaceActiveRoles(
    request: ReplaceActiveRolesRequest,
  ): Promise<ReplaceActiveRolesResult> {
    return this.request('/api/rbac3/v1/auth/role-activations', {
      method: 'PUT',
      body: JSON.stringify(request),
    })
  }

  getBootstrap(): Promise<BootstrapView> {
      return this.request('/api/v1/auth/bootstrap')
  }

    private async request<T>(path: string, init: RequestInit = {}): Promise<T> {
        const headers = new Headers(init.headers)
        headers.set('Accept', 'application/json')
        if (init.body !== undefined && !headers.has('Content-Type')) {
            headers.set('Content-Type', 'application/json')
        }

    let response: Response
    try {
      response = await this.fetcher(`${this.basePath}${path}`, {
        ...init,
        credentials: 'include',
          headers,
      })
    } catch (cause) {
      throw new Rbac3RequestError({
        status: 0,
        code: 'NETWORK_ERROR',
        message: cause instanceof Error ? cause.message : 'RBAC3 network request failed',
        retryable: true,
      })
    }

        if (!response.ok) throw await toRequestError(response)

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
}

const normalizeBasePath = (value: string): string => {
  const trimmed = value.trim()
  return trimmed.endsWith('/') ? trimmed.slice(0, -1) : trimmed
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
