import {
  Rbac3ApiClient,
  Rbac3RequestError,
  type ApiEnvelope,
  type BootstrapView,
  type Rbac3ErrorCode,
  type Rbac3ErrorResponse,
  type RefreshResult,
} from '@egon-cola/rbac3-react-sdk'
import { rbac3AccessTokenStore, rbac3OAuth } from '../features/auth/oauthClient'
import type { FeatureApiClient, FeatureApiRequest } from '../features/shared/FeatureApi'

export interface AdminApiClients {
  readonly accessTokenStore: typeof rbac3AccessTokenStore
  readonly rbac3Client: Rbac3ApiClient
  readonly featureClient: FeatureApiClient
}

interface UnifiedBootstrapView {
  readonly identitySub: string
  readonly tenantId: string
  readonly sessionId: string
  readonly rbac3UserId: string
  readonly systemCode: string
  readonly permissions: readonly string[]
  readonly activeRoleIds: readonly string[]
  readonly authVersion: number
  readonly contextVersion: number
  readonly policyVersion: number
}

export const createAdminApiClients = (
  basePath = '',
  fetcher: typeof globalThis.fetch = globalThis.fetch.bind(globalThis),
): AdminApiClients => {
  const normalizedBasePath = basePath.replace(/\/$/, '')
  const request = async <T,>(
    path: string,
    options: FeatureApiRequest = {},
    allowRefresh = true,
  ): Promise<T> => {
    let response: Response
    try {
      response = await fetcher(buildUrl(normalizedBasePath, path, options.query), {
        method: options.method ?? 'GET',
        credentials: 'include',
        headers: requestHeaders(options.headers, rbac3AccessTokenStore.get()),
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
    if (response.status === 401 && allowRefresh) {
      await rbac3OAuth.refresh()
      return request(path, options, false)
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
  const featureClient: FeatureApiClient = { request }
  const rbac3Client = new UnifiedRbac3ApiClient(
    normalizedBasePath,
    fetcher,
  )
  return {
    accessTokenStore: rbac3AccessTokenStore,
    rbac3Client,
    featureClient,
  }
}

class UnifiedRbac3ApiClient extends Rbac3ApiClient {
  constructor(
    private readonly unifiedBasePath: string,
    private readonly unifiedFetcher: typeof globalThis.fetch,
  ) {
    super({
      basePath: unifiedBasePath,
      fetch: unifiedFetcher,
      accessTokenStore: rbac3AccessTokenStore,
    })
  }

  override async refresh(): Promise<RefreshResult> {
    const accessToken = await rbac3OAuth.refresh()
    const claims = tokenClaims(accessToken)
    return {
      tokenType: 'Bearer',
      accessToken,
      expiresIn: expiresIn(claims.exp),
      refreshToken: null,
      refreshExpiresIn: 0,
      sessionId: stringClaim(claims.sid),
      authVersion: 0,
      sessionVersion: 0,
      policyVersion: 0,
      roleActivationRequired: false,
      activationReasonCode: null,
      bootstrapRequired: true,
    }
  }

  override async getBootstrap(): Promise<BootstrapView> {
    const response = await this.unifiedFetcher(
      `${this.unifiedBasePath}/api/v1/auth/bootstrap`,
      {
        credentials: 'include',
        headers: requestHeaders(undefined, rbac3AccessTokenStore.get()),
      },
    )
    if (!response.ok) throw await responseError(response)
    const body = await readJson<UnifiedBootstrapView | ApiEnvelope<UnifiedBootstrapView>>(
      response,
    )
    const bootstrap = body && 'data' in body ? body.data : body
    if (!bootstrap || !Array.isArray(bootstrap.permissions)) {
      throw new Rbac3RequestError({
        status: response.status,
        code: 'INVALID_RESPONSE',
        message: 'Unified authorization bootstrap is invalid',
        retryable: false,
      })
    }
    return {
      user: {
        id: bootstrap.rbac3UserId,
        tenantId: bootstrap.tenantId,
        username: bootstrap.identitySub,
        displayName: bootstrap.identitySub,
      },
      activeRoleContexts: [],
      permissions: bootstrap.permissions,
      apps: [],
      menus: [],
      routes: [],
      actions: [],
      fieldPolicies: {},
      defaultApplicationCode: null,
      defaultRoute: null,
      sessionId: bootstrap.sessionId,
      authVersion: bootstrap.authVersion,
      sessionVersion: bootstrap.contextVersion,
      policyVersion: bootstrap.policyVersion,
    }
  }

  override async logout(): Promise<void> {
    await rbac3OAuth.revoke()
  }
}

const buildUrl = (basePath: string, path: string, query?: FeatureApiRequest['query']) => {
  const parameters = new URLSearchParams()
  Object.entries(query ?? {}).forEach(([key, value]) => {
    if (value !== null && value !== undefined) parameters.set(key, String(value))
  })
  const suffix = parameters.size === 0 ? '' : `?${parameters.toString()}`
  return `${basePath}${path}${suffix}`
}

const requestHeaders = (
  source: Readonly<Record<string, string>> | undefined,
  token: string | null,
) => {
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
    code: (body?.error?.code as Rbac3ErrorCode | undefined) ?? 'INVALID_RESPONSE',
    message: body?.error?.message ?? 'RBAC3 request was rejected',
    retryable: body?.error?.retryable ?? (response.status === 429 || response.status >= 500),
    traceId: body?.meta?.traceId,
  })
}

const tokenClaims = (token: string): Record<string, unknown> => {
  const payload = token.split('.')[1]
  const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
  return JSON.parse(atob(normalized)) as Record<string, unknown>
}

const stringClaim = (value: unknown): string =>
  typeof value === 'string' && value ? value : 'unknown'

const expiresIn = (value: unknown): number =>
  typeof value === 'number'
    ? Math.max(0, Math.floor(value - Date.now() / 1_000))
    : 0
