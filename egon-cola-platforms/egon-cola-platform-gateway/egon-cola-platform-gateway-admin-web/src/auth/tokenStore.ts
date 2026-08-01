export type AuthTokens = {
  accessToken: string
  refreshToken?: string
  expiresAt?: string
}

const localKey = 'egon.gateway.admin.auth'
const sessionKey = `${localKey}.session`

const storage = (persistent: boolean): Storage | undefined => {
  if (typeof window === 'undefined') return undefined
  return persistent ? window.localStorage : window.sessionStorage
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

const read = (): AuthTokens | undefined => {
  for (const [persistent, key] of [[false, sessionKey], [true, localKey]] as const) {
    const value = storage(persistent)?.getItem(key)
    if (!value) continue
    try {
      const parsed = JSON.parse(value) as AuthTokens
      if (parsed.accessToken) return parsed
    } catch {
      storage(persistent)?.removeItem(key)
    }
  }
  return undefined
}

let current = read()
const listeners = new Set<() => void>()

const notify = () => listeners.forEach((listener) => listener())

export const tokenStore = {
  get: () => current,
  set: (tokens: AuthTokens, persistent: boolean) => {
    current = {
      ...tokens,
      accessToken: tokens.accessToken.trim(),
      refreshToken: tokens.refreshToken?.trim() || undefined,
      expiresAt: tokens.expiresAt ?? decodeExpiry(tokens.accessToken),
    }
    storage(!persistent)?.removeItem(!persistent ? localKey : sessionKey)
    storage(persistent)?.setItem(
      persistent ? localKey : sessionKey,
      JSON.stringify(current),
    )
    notify()
  },
  clear: () => {
    current = undefined
    storage(true)?.removeItem(localKey)
    storage(false)?.removeItem(sessionKey)
    notify()
  },
  subscribe: (listener: () => void) => {
    listeners.add(listener)
    return () => {
      listeners.delete(listener)
    }
  },
}

const tokenUrl = import.meta.env.VITE_GATEWAY_ADMIN_TOKEN_URL
const clientId = import.meta.env.VITE_GATEWAY_ADMIN_CLIENT_ID
export const oauthRefreshEnabled = Boolean(tokenUrl && clientId)
let refreshInFlight: Promise<string> | undefined

export const refreshAccessToken = async (): Promise<string> => {
  if (refreshInFlight) return refreshInFlight
  const tokens = tokenStore.get()
  if (!tokens?.refreshToken || !tokenUrl || !clientId) {
    throw new Error('登录已过期，请重新登录')
  }
  refreshInFlight = (async () => {
    const form = new URLSearchParams({
      grant_type: 'refresh_token',
      refresh_token: tokens.refreshToken!,
      client_id: clientId,
    })
    const response = await fetch(tokenUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: form,
    })
    if (!response.ok) throw new Error('刷新登录凭据失败')
    const value = await response.json() as {
      access_token?: string
      refresh_token?: string
      expires_in?: number
    }
    if (!value.access_token) throw new Error('刷新响应缺少 access_token')
    tokenStore.set({
      accessToken: value.access_token,
      refreshToken: value.refresh_token ?? tokens.refreshToken,
      expiresAt: value.expires_in
        ? new Date(Date.now() + value.expires_in * 1_000).toISOString()
        : undefined,
    }, Boolean(window.localStorage.getItem(localKey)))
    return value.access_token
  })().finally(() => {
    refreshInFlight = undefined
  })
  return refreshInFlight
}
