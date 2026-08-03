import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../api/client'
import { ddcOAuth } from './oauthClient'
import { clearToken, getStoredToken, subscribeToken } from './tokenStore'

type AuthContextValue = {
  token: string
  loading: boolean
  error?: string
  login: (tenantId: string, returnTo?: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string>()

  const clearSession = useCallback(() => {
    clearToken()
    setToken('')
    setLoading(false)
  }, [])

  const login = useCallback(async (tenantId: string, returnTo = '/') => {
    setError(undefined)
    await ddcOAuth.beginAuthorization(tenantId, returnTo)
  }, [])

  const logout = useCallback(async () => {
    await ddcOAuth.revoke()
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
          returnTo = await ddcOAuth.handleCallback(window.location.search)
        } else {
          await ddcOAuth.refresh()
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
        clearToken()
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
    () => ({ token, loading, error, login, logout }),
    [error, loading, login, logout, token],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = (): AuthContextValue => {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth must be used within AuthProvider')
  return value
}
