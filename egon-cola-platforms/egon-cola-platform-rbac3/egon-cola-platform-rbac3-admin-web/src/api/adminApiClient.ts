import {
  InMemoryAccessTokenStore,
  Rbac3ApiClient,
  Rbac3RequestError,
  type ApiEnvelope,
  type LoginRequest,
  type LoginResult,
  type Rbac3ErrorCode,
  type Rbac3ErrorResponse,
} from '@egon-cola/rbac3-react-sdk'
import type { AuthApi } from '../features/auth/auth.api'
import type { FeatureApiClient, FeatureApiRequest } from '../features/shared/FeatureApi'

export interface AdminApiClients {
  readonly accessTokenStore: InMemoryAccessTokenStore
  readonly rbac3Client: Rbac3ApiClient
  readonly featureClient: FeatureApiClient
  readonly authApi: AuthApi
}

export const createAdminApiClients = (
  basePath = '',
  fetcher: typeof globalThis.fetch = globalThis.fetch.bind(globalThis),
): AdminApiClients => {
  const accessTokenStore = new InMemoryAccessTokenStore()
  const rbac3Client = new Rbac3ApiClient({ basePath, fetch: fetcher, accessTokenStore })
  const request = async <T,>(path: string, options: FeatureApiRequest = {}, bearer = true): Promise<T> => {
    let response: Response
    try {
      response = await fetcher(buildUrl(basePath, path, options.query), {
        method: options.method ?? 'GET',
        credentials: 'include',
        headers: requestHeaders(options.headers, bearer ? accessTokenStore.get() : null),
        body: options.body === undefined ? undefined : JSON.stringify(options.body),
      })
    } catch (cause) {
      throw new Rbac3RequestError({ status: 0, code: 'NETWORK_ERROR', message: cause instanceof Error ? cause.message : 'RBAC3 network request failed', retryable: true })
    }
    if (!response.ok) throw await responseError(response)
    const envelope = await readJson<ApiEnvelope<T>>(response)
    if (envelope === null || typeof envelope !== 'object' || !('data' in envelope)) {
      throw new Rbac3RequestError({ status: response.status, code: 'INVALID_RESPONSE', message: 'RBAC3 response envelope is invalid', retryable: false })
    }
    return envelope.data
  }
  const featureClient: FeatureApiClient = { request }
  const authApi: AuthApi = {
    login: async (loginRequest: LoginRequest) => {
      const result = await request<LoginResult>('/api/rbac3/v1/auth/login', { method: 'POST', body: loginRequest }, false)
      accessTokenStore.set(result.accessToken)
      return result
    },
  }
  return { accessTokenStore, rbac3Client, featureClient, authApi }
}

const buildUrl = (basePath: string, path: string, query?: FeatureApiRequest['query']) => {
  const parameters = new URLSearchParams()
  Object.entries(query ?? {}).forEach(([key, value]) => {
    if (value !== null && value !== undefined) parameters.set(key, String(value))
  })
  const suffix = parameters.size === 0 ? '' : `?${parameters.toString()}`
  return `${basePath.replace(/\/$/, '')}${path}${suffix}`
}

const requestHeaders = (source: Readonly<Record<string, string>> | undefined, token: string | null) => {
  const headers = new Headers(source)
  headers.set('Accept', 'application/json')
  headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)
  return headers
}

const readJson = async <T,>(response: Response): Promise<T | null> => {
  try { return await response.json() as T } catch { return null }
}

const responseError = async (response: Response) => {
  const body = await readJson<Rbac3ErrorResponse>(response)
  return new Rbac3RequestError({
    status: response.status,
    code: (body?.error.code as Rbac3ErrorCode | undefined) ?? 'INVALID_RESPONSE',
    message: body?.error.message ?? 'RBAC3 request was rejected',
    retryable: body?.error.retryable ?? (response.status === 429 || response.status >= 500),
    traceId: body?.meta.traceId,
  })
}
