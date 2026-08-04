import { decodeTokenPayload } from '../api/jwt'

export interface AuthTokens {
  readonly accessToken: string
  readonly nonce?: string
  readonly expiresAt?: string
}

export interface TokenStore {
  get(): AuthTokens | null
  set(tokens: AuthTokens): void
  clear(): void
  subscribe(fn: (tokens: AuthTokens | null) => void): () => void
}

const extractNonce = (accessToken: string): string | undefined => {
  try {
    const claims = decodeTokenPayload(accessToken)
    return typeof claims.nonce === 'string' ? claims.nonce : undefined
  } catch {
    return undefined
  }
}

const extractExpiresAt = (accessToken: string, responseExpiresIn?: number): string | undefined => {
  try {
    const claims = decodeTokenPayload(accessToken)
    const exp = claims.exp
    if (exp !== undefined) {
      const seconds = typeof exp === 'string' ? Number(exp) : (exp as number)
      if (Number.isFinite(seconds)) return new Date(seconds * 1000).toISOString()
    }
  } catch { /* fall through */ }
  if (responseExpiresIn) {
    return new Date(Date.now() + responseExpiresIn * 1000).toISOString()
  }
  return undefined
}

export const createTokenStore = (): TokenStore => {
  let tokens: AuthTokens | null = null
  const listeners = new Set<(tokens: AuthTokens | null) => void>()

  const notify = () => {
    for (const fn of listeners) fn(tokens)
  }

  return {
    get: () => tokens,
    set: (incoming) => {
      tokens = {
        accessToken: incoming.accessToken,
        nonce: incoming.nonce ?? extractNonce(incoming.accessToken),
        expiresAt: incoming.expiresAt ?? extractExpiresAt(incoming.accessToken),
      }
      notify()
    },
    clear: () => {
      tokens = null
      notify()
    },
    subscribe: (fn) => {
      listeners.add(fn)
      return () => { listeners.delete(fn) }
    },
  }
}
