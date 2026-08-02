import { tokenStore } from './tokenStore'

export interface OAuthClientConfiguration {
  readonly issuer: string
  readonly clientId: string
  readonly audience: string
  readonly redirectUri: string
}

interface OAuthTransaction {
  readonly state: string
  readonly nonce: string
  readonly verifier: string
  readonly returnTo: string
  readonly createdAt: number
}

interface OAuthRuntime {
  readonly fetch: typeof globalThis.fetch
  readonly storage: Storage
  readonly randomValues: (
    target: Uint8Array<ArrayBuffer>
  ) => Uint8Array<ArrayBuffer>
  readonly digest: (value: Uint8Array<ArrayBuffer>) => Promise<ArrayBuffer>
  readonly navigate: (url: string) => void
  readonly now: () => number
}

interface TokenResponse {
  readonly access_token?: string
  readonly token_type?: string
  readonly expires_in?: number
}

const transactionKey = 'egon.gateway.admin.oauth.transaction'

export const createBrowserOAuthClient = (
  configuration: OAuthClientConfiguration,
  runtime: OAuthRuntime,
) => {
  const issuer = configuration.issuer.replace(/\/$/, '')
  let refreshInFlight: Promise<string> | undefined

  const storeToken = (response: TokenResponse, expectedNonce?: string): string => {
    if (!response.access_token || response.token_type?.toLowerCase() !== 'bearer') {
      throw new Error('统一身份响应缺少有效的 access_token')
    }
    const claims = decodeClaims(response.access_token)
    if (expectedNonce && claims.nonce !== expectedNonce) {
      throw new Error('统一身份登录 nonce 校验失败')
    }
    tokenStore.set({
      accessToken: response.access_token,
      nonce: typeof claims.nonce === 'string' ? claims.nonce : undefined,
      expiresAt: response.expires_in
        ? new Date(runtime.now() + response.expires_in * 1_000).toISOString()
        : undefined,
    })
    return response.access_token
  }

  const requestToken = async (form: URLSearchParams): Promise<TokenResponse> => {
    const response = await runtime.fetch(`${issuer}/oauth2/token`, {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: form,
    })
    if (!response.ok) throw new Error('统一身份凭据交换失败')
    return await response.json() as TokenResponse
  }

  return {
    beginAuthorization: async (tenantId: string, returnTo = '/') => {
      const transaction: OAuthTransaction = {
        state: randomToken(runtime),
        nonce: randomToken(runtime),
        verifier: randomToken(runtime),
        returnTo: safeReturnTo(returnTo),
        createdAt: runtime.now(),
      }
      runtime.storage.setItem(transactionKey, JSON.stringify(transaction))
      const challenge = base64Url(new Uint8Array(
        await runtime.digest(new TextEncoder().encode(transaction.verifier)),
      ))
      const parameters = new URLSearchParams({
        response_type: 'code',
        client_id: configuration.clientId,
        redirect_uri: configuration.redirectUri,
        audience: configuration.audience,
        tenant_id: required(tenantId, 'tenantId'),
        state: transaction.state,
        nonce: transaction.nonce,
        code_challenge: challenge,
        code_challenge_method: 'S256',
      })
      runtime.navigate(`${issuer}/oauth2/authorize?${parameters.toString()}`)
    },
    handleCallback: async (search: string): Promise<string> => {
      const encoded = runtime.storage.getItem(transactionKey)
      runtime.storage.removeItem(transactionKey)
      if (!encoded) throw new Error('统一身份登录事务不存在或已失效')
      const transaction = JSON.parse(encoded) as OAuthTransaction
      const age = runtime.now() - transaction.createdAt
      if (!Number.isFinite(age) || age < 0 || age > 10 * 60 * 1_000) {
        throw new Error('统一身份登录事务不存在或已失效')
      }
      const parameters = new URLSearchParams(search)
      if (parameters.get('error')) throw new Error('统一身份授权被拒绝')
      if (parameters.get('state') !== transaction.state) {
        throw new Error('统一身份登录 state 校验失败')
      }
      const response = await requestToken(new URLSearchParams({
        grant_type: 'authorization_code',
        client_id: configuration.clientId,
        code: required(parameters.get('code'), 'code'),
        code_verifier: transaction.verifier,
        redirect_uri: configuration.redirectUri,
      }))
      storeToken(response, transaction.nonce)
      return safeReturnTo(transaction.returnTo)
    },
    refresh: (): Promise<string> => {
      if (!refreshInFlight) {
        refreshInFlight = requestToken(new URLSearchParams({
          grant_type: 'refresh_token',
          client_id: configuration.clientId,
        }))
          .then((response) => storeToken(response, tokenStore.get()?.nonce))
          .finally(() => { refreshInFlight = undefined })
      }
      return refreshInFlight
    },
    revoke: async (): Promise<void> => {
      try {
        await runtime.fetch(`${issuer}/oauth2/revoke`, {
          method: 'POST',
          credentials: 'include',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: new URLSearchParams({ client_id: configuration.clientId }),
        })
      } finally {
        tokenStore.clear()
      }
    },
  }
}

const browserRuntime = (): OAuthRuntime => ({
  fetch: globalThis.fetch.bind(globalThis),
  storage: window.sessionStorage,
  randomValues: (target) => crypto.getRandomValues(target),
  digest: (value) => crypto.subtle.digest('SHA-256', value),
  navigate: (url) => window.location.assign(url),
  now: () => Date.now(),
})

export const gatewayOAuth = createBrowserOAuthClient({
  issuer: import.meta.env.VITE_IDP_ISSUER ?? 'http://127.0.0.1:18120',
  clientId: import.meta.env.VITE_IDP_CLIENT_ID ?? 'gateway-admin-web',
  audience: import.meta.env.VITE_IDP_AUDIENCE ?? 'gateway-admin-web',
  redirectUri: import.meta.env.VITE_IDP_REDIRECT_URI
    ?? `${window.location.origin}/oauth/callback`,
}, browserRuntime())

const randomToken = (runtime: OAuthRuntime): string => {
  const bytes = new Uint8Array(new ArrayBuffer(32))
  runtime.randomValues(bytes)
  return base64Url(bytes)
}

const base64Url = (bytes: Uint8Array): string => {
  let value = ''
  bytes.forEach((byte) => { value += String.fromCharCode(byte) })
  return btoa(value).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

const decodeClaims = (token: string): Record<string, unknown> => {
  try {
    const payload = token.split('.')[1]
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(atob(normalized)) as Record<string, unknown>
  } catch {
    throw new Error('统一身份 access_token 格式无效')
  }
}

const required = (value: string | null, name: string): string => {
  const normalized = value?.trim()
  if (!normalized) throw new Error(`${name} is required`)
  return normalized
}

const safeReturnTo = (value: string): string =>
  value.startsWith('/') && !value.startsWith('//') ? value : '/'
