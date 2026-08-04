import { ApiError } from './errors'

export interface HttpClientConfig {
  readonly baseUrl: string
  readonly credentials: RequestCredentials
  readonly onAuthError: () => Promise<string>
  readonly onFatalAuthError: () => void
  readonly getAccessToken?: () => string | null
  readonly timeout?: number
}

export interface HttpClient {
  request<T>(path: string, init?: RequestInit & { signal?: AbortSignal }): Promise<T>
}

export const createHttpClient = (config: HttpClientConfig): HttpClient => {
  const { baseUrl, credentials, onAuthError, onFatalAuthError, getAccessToken, timeout = 30_000 } = config

  const buildHeaders = (init?: RequestInit): Headers => {
    const headers = new Headers(init?.headers)
    headers.set('Accept', 'application/json')
    const token = getAccessToken?.()
    if (token) headers.set('Authorization', `Bearer ${token}`)
    if (shouldSetJsonContentType(init?.body) && !headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json')
    }
    return headers
  }

  const doFetch = (path: string, init?: RequestInit & { signal?: AbortSignal }): Promise<Response> => {
    const controller = new AbortController()
    const timer = setTimeout(() => controller.abort(), timeout)
    const signal = init?.signal ?? controller.signal
    return fetch(`${baseUrl}${path}`, { ...init, credentials, headers: buildHeaders(init), signal })
      .finally(() => clearTimeout(timer))
  }

  const handleResponse = async <T>(response: Response): Promise<T> => {
    if (response.status === 204) return undefined as T
    if (!response.ok) {
      const body = await response.json().catch(() => ({})) as { code?: string; message?: string }
      throw new ApiError(
        body.message ?? `Request failed (${response.status})`,
        response.status,
        body.code ?? 'REQUEST_FAILED',
      )
    }
    return await response.json() as T
  }

  const request = async <T>(path: string, init?: RequestInit & { signal?: AbortSignal }): Promise<T> => {
    const response = await doFetch(path, init)
    if (response.status === 401) {
      try {
        const newToken = await onAuthError()
        const retryHeaders = new Headers(init?.headers)
        retryHeaders.set('Authorization', `Bearer ${newToken}`)
        const retryResponse = await doFetch(path, { ...init, headers: retryHeaders })
        if (retryResponse.status === 401) {
          onFatalAuthError()
          throw new ApiError('Authentication failed after token refresh', 401, 'AUTH_FAILED')
        }
        return handleResponse<T>(retryResponse)
      } catch (error) {
        if (error instanceof ApiError) throw error
        onFatalAuthError()
        throw error
      }
    }
    return handleResponse<T>(response)
  }

  return { request }
}

const shouldSetJsonContentType = (body: unknown): boolean => {
  if (body === null || body === undefined) return false
  if (typeof body === 'string') {
    try { JSON.parse(body); return true } catch { return false }
  }
  if (body instanceof FormData || body instanceof URLSearchParams) return false
  return typeof body === 'object'
}
