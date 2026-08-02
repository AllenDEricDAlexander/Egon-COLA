export type AuthTokens = {
  accessToken: string
  expiresAt?: string
  nonce?: string
}

const legacyKeys = [
  'egon.idp.admin.auth',
  'egon.idp.admin.auth.session',
]

if (typeof window !== 'undefined') {
  legacyKeys.forEach((key) => {
    window.localStorage.removeItem(key)
    window.sessionStorage.removeItem(key)
  })
}

const decodeExpiry = (token: string): string | undefined => {
  try {
    const payload = token.split('.')[1]
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const claims = JSON.parse(atob(normalized)) as { exp?: number }
    return claims.exp ? new Date(claims.exp * 1_000).toISOString() : undefined
  } catch {
    return undefined
  }
}

let current: AuthTokens | undefined
const listeners = new Set<() => void>()
const notify = () => listeners.forEach((listener) => listener())

/** Keeps short-lived access credentials only in the current JavaScript process. */
export const tokenStore = {
  get: () => current,
  set: (tokens: AuthTokens) => {
    const accessToken = tokens.accessToken.trim()
    if (!accessToken) throw new Error('access token must not be blank')
    current = {
      accessToken,
      expiresAt: tokens.expiresAt ?? decodeExpiry(accessToken),
      nonce: tokens.nonce,
    }
    notify()
  },
  clear: () => {
    current = undefined
    notify()
  },
  subscribe: (listener: () => void) => {
    listeners.add(listener)
    return () => { listeners.delete(listener) }
  },
}
