import {
    type ApiEnvelope,
    Rbac3ApiClient,
    type Rbac3ErrorCode,
    type Rbac3ErrorResponse,
    Rbac3RequestError,
} from '@egon-cola/rbac3-react-sdk'
import type {FeatureApiClient, FeatureApiRequest} from '../features/shared/FeatureApi'

export interface AdminApiClients {
  readonly rbac3Client: Rbac3ApiClient
  readonly featureClient: FeatureApiClient
}

export const createAdminApiClients = (
  basePath = '',
  fetcher: typeof globalThis.fetch = globalThis.fetch.bind(globalThis),
): AdminApiClients => {
  const normalizedBasePath = basePath.replace(/\/$/, '')
  const request = async <T,>(
    path: string,
    options: FeatureApiRequest = {},
  ): Promise<T> => {
    let response: Response
      const headers = new Headers(options.headers)
      headers.set('Accept', 'application/json')
      if (options.body !== undefined && !headers.has('Content-Type')) {
          headers.set('Content-Type', 'application/json')
      }
    try {
      response = await fetcher(buildUrl(normalizedBasePath, path, options.query), {
        method: options.method ?? 'GET',
        credentials: 'include',
          headers,
        body: options.body === undefined ? undefined : JSON.stringify(options.body),
      })
    } catch (cause) {
      throw new Rbac3RequestError({
        status: 0,
        code: 'NETWORK_ERROR',
        message: cause instanceof Error ? cause.message : 'RBAC3 network request failed',
        retryable: true,
      })
    }
    if (!response.ok) throw await responseError(response)
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

    return {
        rbac3Client: new Rbac3ApiClient({basePath: normalizedBasePath, fetch: fetcher}),
        featureClient: {request},
    }
}

const buildUrl = (
    basePath: string,
    path: string,
    query?: FeatureApiRequest['query'],
) => {
  const parameters = new URLSearchParams()
  Object.entries(query ?? {}).forEach(([key, value]) => {
    if (value !== null && value !== undefined) parameters.set(key, String(value))
  })
  const suffix = parameters.size === 0 ? '' : `?${parameters.toString()}`
  return `${basePath}${path}${suffix}`
}

const readJson = async <T,>(response: Response): Promise<T | null> => {
  try { return await response.json() as T } catch { return null }
}

const responseError = async (response: Response): Promise<Rbac3RequestError> => {
  const body = await readJson<Rbac3ErrorResponse>(response)
  return new Rbac3RequestError({
    status: response.status,
    code: (body?.error?.code as Rbac3ErrorCode | undefined) ?? 'INVALID_RESPONSE',
    message: body?.error?.message ?? 'RBAC3 request was rejected',
    retryable: body?.error?.retryable ?? (response.status === 429 || response.status >= 500),
    traceId: body?.meta?.traceId,
  })
}
