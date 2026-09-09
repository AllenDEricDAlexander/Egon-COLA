import {
    Rbac3ApiClient,
    type Rbac3ErrorResponse,
} from '@egon-cola/tianquan-jianshen-react-sdk'
import {ApiError} from '@egon-cola/xingyuan-admin-web-shared'
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
      throw new ApiError(
        cause instanceof Error ? cause.message : 'RBAC3 network request failed',
        0, 'NETWORK_ERROR', {retryable: true},
      )
    }
    if (!response.ok) throw await responseError(response)
    const envelope = await readJson<ResultRecord<T> | LegacyDataEnvelope<T>>(response)
    if (isLegacyDataEnvelope(envelope)) return envelope.data
    if (envelope === null || typeof envelope !== 'object' || !('data' in envelope) || envelope.success !== true) {
      throw new ApiError(
        envelope?.message ?? 'RBAC3 response envelope is invalid',
        response.status,
        envelope?.status ?? (envelope?.code === 401 ? 'AUTHENTICATION_REQUIRED' : 'INVALID_RESPONSE'),
        {retryable: false},
      )
    }
    return envelope.data as T
  }

    return {
        rbac3Client: new Rbac3ApiClient({basePath: normalizedBasePath, fetch: fetcher}),
        featureClient: {request},
    }
}

interface ResultRecord<T> {
  readonly success: boolean
  readonly code: number
  readonly message: string
  readonly data: T | null
  readonly status?: string
  readonly traceId?: string | null
}

interface LegacyDataEnvelope<T> {
  readonly data: T
  readonly meta: Readonly<Record<string, unknown>>
}

const isLegacyDataEnvelope = <T,>(value: ResultRecord<T> | LegacyDataEnvelope<T> | null): value is LegacyDataEnvelope<T> =>
  value !== null && typeof value === 'object' && 'data' in value && 'meta' in value && !('success' in value)

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

const responseError = async (response: Response): Promise<ApiError> => {
  const body = await readJson<Rbac3ErrorResponse & Partial<ResultRecord<unknown>>>(response)
  return new ApiError(
    body?.error?.message ?? body?.message ?? 'RBAC3 request was rejected',
    response.status,
    body?.error?.code ?? body?.status ?? 'INVALID_RESPONSE',
    {
      retryable: body?.error?.retryable ?? (response.status === 429 || response.status >= 500),
      requestId: body?.meta?.traceId ?? body?.traceId ?? undefined,
    },
  )
}
