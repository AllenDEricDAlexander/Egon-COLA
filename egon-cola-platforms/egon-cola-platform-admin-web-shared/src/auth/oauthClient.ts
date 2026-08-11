import type { TokenStore } from './tokenStore'

export interface OAuthClientConfiguration {
  readonly issuer: string
  readonly clientId: string
  readonly resource: string
  readonly redirectUri: string
  readonly tokenStore: TokenStore
}

interface OAuthTransaction {
  readonly state: string
  readonly nonce: string
  readonly verifier: string
  readonly returnTo: string
  readonly createdAt: number
}

export interface OAuthRuntime {
  readonly fetch: typeof globalThis.fetch
  readonly storage: Storage
  readonly randomValues: (target: Uint8Array<ArrayBuffer>) => Uint8Array<ArrayBuffer>
  readonly digest: (value: Uint8Array<ArrayBuffer>) => Promise<ArrayBuffer>
  readonly navigate: (url: string) => void
  readonly now: () => number
}

export interface OAuthClient {
  beginAuthorization(tenantId: string, returnTo?: string): Promise<void>
  handleCallback(search: string): Promise<string>
  refresh(): Promise<string>
  revoke(): Promise<void>
}

const TRANSACTION_KEY = 'egon.admin.oauth.transaction'
const textDecoder = new TextDecoder('utf-8')
const textEncoder = new TextEncoder()

const decodeTokenPayloadRaw = (token: string): Record<string, unknown> => {
  const parts = token.split('.')
  if (parts.length !== 3) throw new Error('Invalid JWT format')
  const normalized = parts[1]!.replace(/-/g, '+').replace(/_/g, '/')
  const binary = Uint8Array.from(atob(normalized), (c) => c.charCodeAt(0))
  return JSON.parse(textDecoder.decode(binary)) as Record<string, unknown>
}

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

const safeReturnTo = (value: string): string =>
  value.startsWith('/') && !value.startsWith('//') ? value : '/'

interface TokenResponse {
  readonly access_token?: string
  readonly token_type?: string
  readonly expires_in?: number
}

export const createOAuthClient = (
  configuration: OAuthClientConfiguration,
  runtime: OAuthRuntime,
): OAuthClient => {
  const issuer = configuration.issuer.replace(/\/$/, '')
  let refreshInFlight: Promise<string> | undefined
  let callbackInFlight: Promise<string> | undefined

  const storeToken = (response: TokenResponse, expectedNonce?: string): string => {
    if (!response.access_token || response.token_type?.toLowerCase() !== 'bearer') {
      throw new Error('Invalid token response: missing access_token or wrong token_type')
    }
    // FIX: nonce validation only for authorization_code grant (when expectedNonce is provided)
    if (expectedNonce) {
      const claims = decodeTokenPayloadRaw(response.access_token)
      if (claims.nonce !== expectedNonce) {
        throw new Error('Token nonce validation failed')
      }
    }
    configuration.tokenStore.set({ accessToken: response.access_token })
    return response.access_token
  }

  const requestToken = async (form: URLSearchParams): Promise<TokenResponse> => {
    const response = await runtime.fetch(`${issuer}/oauth2/token`, {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: form,
    })
    if (!response.ok) {
      const body = await response.json().catch(() => ({})) as { error_description?: string; error?: string }
      throw new Error(body.error_description ?? body.error ?? 'Token exchange failed')
    }
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
      runtime.storage.setItem(TRANSACTION_KEY, JSON.stringify(transaction))
      const challenge = base64Url(new Uint8Array(
        await runtime.digest(textEncoder.encode(transaction.verifier)),
      ))
      const trimmedTenant = tenantId.trim()
      if (!trimmedTenant) throw new Error('tenantId is required')
      const params = new URLSearchParams({
        response_type: 'code',
        client_id: configuration.clientId,
        redirect_uri: configuration.redirectUri,
        resource: configuration.resource,
        tenant_id: trimmedTenant,
        state: transaction.state,
        nonce: transaction.nonce,
        code_challenge: challenge,
        code_challenge_method: 'S256',
      })
      runtime.navigate(`${issuer}/oauth2/authorize?${params.toString()}`)
    },

    handleCallback: (search: string): Promise<string> => {
      if (!callbackInFlight) {
        callbackInFlight = Promise.resolve().then(async () => {
          try {
            const encoded = runtime.storage.getItem(TRANSACTION_KEY)
            if (!encoded) throw new Error('OAuth transaction not found or expired')
            const transaction = JSON.parse(encoded) as OAuthTransaction
            const age = runtime.now() - transaction.createdAt
            if (!Number.isFinite(age) || age < 0 || age > 10 * 60 * 1000) {
              throw new Error('OAuth transaction expired')
            }
            const params = new URLSearchParams(search)
            const errorDesc = params.get('error_description')
            if (params.get('error')) {
              throw new Error(errorDesc ?? 'Authorization was denied')
            }
            // FIX: validate state BEFORE removing transaction (allows retry on failure)
            if (params.get('state') !== transaction.state) {
              throw new Error('OAuth state validation failed')
            }
            const code = params.get('code')
            if (!code) throw new Error('code is required')
            const response = await requestToken(new URLSearchParams({
              grant_type: 'authorization_code',
              client_id: configuration.clientId,
              code,
              code_verifier: transaction.verifier,
              redirect_uri: configuration.redirectUri,
              resource: configuration.resource,
            }))
            storeToken(response, transaction.nonce)
            runtime.storage.removeItem(TRANSACTION_KEY)
            return safeReturnTo(transaction.returnTo)
          } finally {
            callbackInFlight = undefined
          }
        })
      }
      return callbackInFlight
    },

    refresh: (): Promise<string> => {
      if (!refreshInFlight) {
        refreshInFlight = requestToken(new URLSearchParams({
          grant_type: 'refresh_token',
          client_id: configuration.clientId,
          resource: configuration.resource,
        }))
          .then((response) => storeToken(response))  // FIX: NO nonce validation on refresh
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
        configuration.tokenStore.clear()
      }
    },
  }
}
