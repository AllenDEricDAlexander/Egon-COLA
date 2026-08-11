import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import {
  createOAuthClient,
  createTokenStore,
  decodeTokenPayload,
  type OAuthClient,
} from '@egon-cola/admin-web-shared'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../api/client'

const requiredEnv = (key: string): string => {
  const value = (import.meta.env as Record<string, string>)[key]
  if (!value) throw new Error(`${key} is required`)
  return value
}

const tokenStore = createTokenStore()

const oauthClient: OAuthClient = createOAuthClient({
  issuer: requiredEnv('VITE_IDP_ISSUER'),
  clientId: requiredEnv('VITE_IDP_CLIENT_ID'),
  resource: requiredEnv('VITE_IDP_RESOURCE'),
  redirectUri: (import.meta.env as Record<string, string>).VITE_IDP_REDIRECT_URI
    ?? `${window.location.origin}/oauth/callback`,
  tokenStore,
}, {
  fetch: globalThis.fetch.bind(globalThis),
  storage: window.sessionStorage,
  randomValues: (target: Uint8Array<ArrayBuffer>) => crypto.getRandomValues(target),
  digest: (value: Uint8Array<ArrayBuffer>) => crypto.subtle.digest('SHA-256', value),
  navigate: (url: string) => { window.location.assign(url) },
  now: () => Date.now(),
})

export { oauthClient, tokenStore }

// Adapt shared tokenStore to DDC's string-based token provider pattern
const getStoredToken = (): string => tokenStore.get()?.accessToken ?? ''
const subscribeToken = (fn: () => void): (() => void) => tokenStore.subscribe(() => fn())

type AuthContextValue = {
  token: string
  readonly identity: string
  loading: boolean
  error?: string
  login: (tenantId: string, returnTo?: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export const identityFromToken = (token: string): string => {
  if (!token) return ''
  try {
    const claims = decodeTokenPayload(token)
    return [
      claims.displayName,
      claims.name,
      claims.preferred_username,
      claims.sub,
    ].find((value): value is string =>
      typeof value === 'string' && value.trim() !== '') ?? ''
  } catch {
    return ''
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string>()
  const identity = useMemo(() => identityFromToken(token), [token])

  const clearSession = useCallback(() => {
    tokenStore.clear()
    setToken('')
    setLoading(false)
  }, [])

  const login = useCallback(async (tenantId: string, returnTo = '/') => {
    setError(undefined)
    await oauthClient.beginAuthorization(tenantId, returnTo)
  }, [])

  const logout = useCallback(async () => {
    await oauthClient.revoke()
    clearSession()
  }, [clearSession])

  useEffect(() => {
    setDdcTokenProvider(getStoredToken)
    setDdcUnauthorizedHandler(clearSession)
    return subscribeToken(() => setToken(getStoredToken()))
  }, [clearSession])

  useEffect(() => {
    let active = true
    const initialize = async () => {
      try {
        let returnTo: string | undefined
        if (window.location.pathname === '/oauth/callback') {
          returnTo = await oauthClient.handleCallback(window.location.search)
        } else {
          await oauthClient.refresh()
        }
        if (!active) return
        const accessToken = getStoredToken()
        setToken(accessToken)
        if (returnTo) {
          window.history.replaceState({}, '', returnTo)
          window.dispatchEvent(new PopStateEvent('popstate'))
        }
      } catch (failure) {
        if (!active) return
        tokenStore.clear()
        setToken('')
        if (window.location.pathname === '/oauth/callback') {
          setError(failure instanceof Error ? failure.message : '统一身份登录失败')
        }
      } finally {
        if (active) setLoading(false)
      }
    }
    void initialize()
    return () => { active = false }
  }, [])

  const value = useMemo(
    () => ({ token, identity, loading, error, login, logout }),
    [error, identity, loading, login, logout, token],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = (): AuthContextValue => {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth must be used within AuthProvider')
  return value
}
