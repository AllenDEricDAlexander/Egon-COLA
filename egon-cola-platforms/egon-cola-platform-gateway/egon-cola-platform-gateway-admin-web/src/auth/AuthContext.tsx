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
import { refreshAccessToken, tokenStore, type AuthTokens } from './tokenStore'

type AuthState = {
  loading: boolean
  session?: AdminSession
  login: (tokens: AuthTokens, persistent: boolean) => Promise<void>
  logout: () => void
  refreshSession: () => Promise<void>
}

const AuthContext = createContext<AuthState | undefined>(undefined)

export const AuthProvider = ({ children }: PropsWithChildren) => {
  const [loading, setLoading] = useState(Boolean(tokenStore.get()))
  const [session, setSession] = useState<AdminSession>()

  const logout = useCallback(() => {
    tokenStore.clear()
    setSession(undefined)
    setLoading(false)
  }, [])

  const refreshSession = useCallback(async () => {
    const value = await gatewayApi.session()
    setSession(value)
  }, [])

  const login = useCallback(async (tokens: AuthTokens, persistent: boolean) => {
    tokenStore.set(tokens, persistent)
    setLoading(true)
    try {
      await refreshSession()
    } catch (error) {
      logout()
      throw error
    } finally {
      setLoading(false)
    }
  }, [logout, refreshSession])

  useEffect(() => {
    if (!tokenStore.get()) return
    let active = true
    void gatewayApi.session()
      .then((value) => {
        if (active) setSession(value)
      })
      .catch(() => {
        if (active) logout()
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [logout])

  useEffect(() => tokenStore.subscribe(() => {
    if (!tokenStore.get()) {
      setSession(undefined)
      setLoading(false)
    }
  }), [])

  useEffect(() => {
    const expiresAt = tokenStore.get()?.expiresAt
    if (!expiresAt || !session) return
    const delay = Math.max(1_000, Date.parse(expiresAt) - Date.now() - 60_000)
    const timer = window.setTimeout(() => {
      void refreshAccessToken()
        .then(refreshSession)
        .catch(logout)
    }, delay)
    return () => window.clearTimeout(timer)
  }, [logout, refreshSession, session])

  const value = useMemo(
    () => ({ loading, session, login, logout, refreshSession }),
    [loading, login, logout, refreshSession, session],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = (): AuthState => {
  const value = useContext(AuthContext)
  if (!value) throw new Error('AuthProvider is required')
  return value
}
