import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from 'react'
import {
  createOAuthClient,
  createTokenStore,
  createHttpClient,
  type OAuthClient,
  type AuthTokens,
} from '@egon-cola/admin-web-shared'
import type { AdminSession } from '../api/types'

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

const httpClient = createHttpClient({
  baseUrl: (import.meta.env as Record<string, string>).VITE_GATEWAY_ADMIN_API_BASE_URL ?? '',
  credentials: 'include',
  getAccessToken: () => tokenStore.get()?.accessToken ?? null,
  onAuthError: () => oauthClient.refresh(),
  onFatalAuthError: () => {
    tokenStore.clear()
    window.location.assign('/login')
  },
})

export { oauthClient, httpClient, tokenStore }

type AuthState = {
  loading: boolean
  session?: AdminSession
  error?: string
  login: (tenantId: string, returnTo?: string) => Promise<void>
  logout: () => Promise<void>
  refreshSession: () => Promise<void>
}

const AuthContext = createContext<AuthState | undefined>(undefined)

export const AuthProvider = ({ children }: PropsWithChildren) => {
  const [loading, setLoading] = useState(true)
  const [session, setSession] = useState<AdminSession>()
  const [error, setError] = useState<string>()
  const [tokens, setTokens] = useState<AuthTokens | null>(tokenStore.get())

  const clearSession = useCallback(() => {
    tokenStore.clear()
    setSession(undefined)
    setLoading(false)
  }, [])

  const logout = useCallback(async () => {
    await oauthClient.revoke()
    setSession(undefined)
    setLoading(false)
  }, [])

  const refreshSession = useCallback(async () => {
    setSession(await httpClient.request<AdminSession>('/api/v1/gateway/admin/session'))
  }, [])

  const login = useCallback(async (tenantId: string, returnTo = '/dashboard') => {
    setError(undefined)
    await oauthClient.beginAuthorization(tenantId, returnTo)
  }, [])

  useEffect(() => tokenStore.subscribe(setTokens), [])

  useEffect(() => {
    let active = true
    const initialize = async () => {
      if (!tokens) {
        if (active) { setSession(undefined); setLoading(false) }
        return
      }
      try {
        const value = await httpClient.request<AdminSession>('/api/v1/gateway/admin/session')
        if (active) setSession(value)
      } catch (failure) {
        if (!active) return
        tokenStore.clear()
        setSession(undefined)
        if (window.location.pathname === '/oauth/callback') {
          setError(failure instanceof Error ? failure.message : '统一身份登录失败')
        }
      } finally {
        if (active) setLoading(false)
      }
    }
    void initialize()
    return () => { active = false }
  }, [tokens])

  // Proactive token refresh
  useEffect(() => {
    const stored = tokenStore.get()
    if (!stored?.expiresAt || !session) return
    const delay = Math.max(1_000, Date.parse(stored.expiresAt) - Date.now() - 60_000)
    const timer = window.setTimeout(() => {
      void oauthClient.refresh().then(refreshSession).catch(clearSession)
    }, delay)
    return () => window.clearTimeout(timer)
  }, [clearSession, refreshSession, session])

  const value = useMemo(
    () => ({ loading, session, error, login, logout, refreshSession }),
    [error, loading, login, logout, refreshSession, session],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = (): AuthState => {
  const value = useContext(AuthContext)
  if (!value) throw new Error('AuthProvider is required')
  return value
}
