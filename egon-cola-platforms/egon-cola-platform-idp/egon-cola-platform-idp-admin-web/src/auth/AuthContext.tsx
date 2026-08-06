import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
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
import type { AuthorizationBootstrap } from '../api/types'

const requiredEnv = (key: string): string => {
  const value = (import.meta.env as Record<string, string>)[key]
  if (!value) throw new Error(`${key} is required`)
  return value
}

const tokenStore = createTokenStore()

const oauthClient: OAuthClient = createOAuthClient({
  issuer: requiredEnv('VITE_IDP_ISSUER'),
  clientId: requiredEnv('VITE_IDP_CLIENT_ID'),
  audience: requiredEnv('VITE_IDP_AUDIENCE'),
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
  baseUrl: (import.meta.env as Record<string, string>).VITE_IDP_API_BASE_URL ?? '',
  credentials: 'include',
  getAccessToken: () => tokenStore.get()?.accessToken ?? null,
  onAuthError: () => oauthClient.refresh(),
  onFatalAuthError: () => {
    tokenStore.clear()
    window.location.assign('/login')
  },
})

export { oauthClient, httpClient }

interface AuthContextValue {
  readonly loading: boolean
  readonly bootstrap?: AuthorizationBootstrap
  readonly login: (tenantId: string, returnTo?: string) => Promise<void>
  readonly logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export const AuthProvider = ({ children }: PropsWithChildren) => {
  const [loading, setLoading] = useState(true)
  const [bootstrap, setBootstrap] = useState<AuthorizationBootstrap>()
  const [tokens, setTokens] = useState<AuthTokens | null>(tokenStore.get())
  const refreshAttempted = useRef(false)
  const refreshInFlight = useRef<Promise<string> | undefined>(undefined)

  const login = useCallback(async (tenantId: string, returnTo = '/') => {
    await oauthClient.beginAuthorization(tenantId, returnTo)
  }, [])

  const logout = useCallback(async () => {
    await oauthClient.revoke()
    setBootstrap(undefined)
  }, [])

  useEffect(() => tokenStore.subscribe(setTokens), [])

  useEffect(() => {
    let active = true
    const initialize = async () => {
      if (!tokens) {
        if (active) setBootstrap(undefined)
        if (window.location.pathname === '/oauth/callback') {
          if (active) setLoading(false)
          return
        }
        if (!refreshAttempted.current) {
          refreshAttempted.current = true
          refreshInFlight.current = oauthClient.refresh()
        }
        const refresh = refreshInFlight.current
        if (!refresh) {
          if (active) setLoading(false)
          return
        }
        if (active) setLoading(true)
        try {
          await refresh
          if (active && !tokenStore.get()) setLoading(false)
        } catch {
          if (active) setLoading(false)
        } finally {
          if (refreshInFlight.current === refresh) refreshInFlight.current = undefined
        }
        return
      }
      if (active) setLoading(true)
      try {
        const value = await httpClient.request<AuthorizationBootstrap>('/api/v1/auth/bootstrap')
        if (active) setBootstrap(value)
      } catch {
        if (!active) return
        tokenStore.clear()
        setBootstrap(undefined)
        if (window.location.pathname !== '/login') {
          window.history.replaceState({}, '', '/login')
          window.dispatchEvent(new PopStateEvent('popstate'))
        }
      } finally {
        if (active) setLoading(false)
      }
    }
    void initialize()
    return () => { active = false }
  }, [tokens])

  const value = useMemo(
    () => ({ loading, bootstrap, login, logout }),
    [loading, bootstrap, login, logout],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = (): AuthContextValue => {
  const value = useContext(AuthContext)
  if (!value) throw new Error('AuthProvider is required')
  return value
}
