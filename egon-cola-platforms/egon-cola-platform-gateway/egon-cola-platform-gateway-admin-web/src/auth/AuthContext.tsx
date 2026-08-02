import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from 'react'
import { gatewayApi } from '../api/gatewayApi'
import type { AdminSession } from '../api/types'
import { gatewayOAuth } from './oauthClient'
import { tokenStore } from './tokenStore'

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

  const clearSession = useCallback(() => {
    tokenStore.clear()
    setSession(undefined)
    setLoading(false)
  }, [])

  const logout = useCallback(async () => {
    await gatewayOAuth.revoke()
    setSession(undefined)
    setLoading(false)
  }, [])

  const refreshSession = useCallback(async () => {
    setSession(await gatewayApi.session())
  }, [])

  const login = useCallback(async (tenantId: string, returnTo = '/dashboard') => {
    setError(undefined)
    await gatewayOAuth.beginAuthorization(tenantId, returnTo)
  }, [])

  useEffect(() => {
    let active = true
    const initialize = async () => {
      try {
        let returnTo: string | undefined
        if (window.location.pathname === '/oauth/callback') {
          returnTo = await gatewayOAuth.handleCallback(window.location.search)
        } else if (!tokenStore.get()) {
          await gatewayOAuth.refresh()
        }
        const value = await gatewayApi.session()
        if (!active) return
        setSession(value)
        if (returnTo) {
          window.history.replaceState({}, '', returnTo)
          window.dispatchEvent(new PopStateEvent('popstate'))
        }
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
  }, [])

  useEffect(() => tokenStore.subscribe(() => {
    if (!tokenStore.get()) setSession(undefined)
  }), [])

  useEffect(() => {
    const expiresAt = tokenStore.get()?.expiresAt
    if (!expiresAt || !session) return
    const delay = Math.max(1_000, Date.parse(expiresAt) - Date.now() - 60_000)
    const timer = window.setTimeout(() => {
      void gatewayOAuth.refresh()
        .then(refreshSession)
        .catch(clearSession)
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
