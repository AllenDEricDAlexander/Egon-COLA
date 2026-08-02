import { createContext, useCallback, useContext, useEffect, useMemo, useState, type PropsWithChildren } from 'react'
import { idpApi } from '../api/idpApi'
import type { AuthorizationBootstrap } from '../api/types'
import { idpOAuth } from './oauthClient'
import { tokenStore } from './tokenStore'

interface AuthContextValue {
  readonly loading: boolean
  readonly bootstrap?: AuthorizationBootstrap
  readonly error?: string
  readonly login: (tenantId: string, returnTo?: string) => Promise<void>
  readonly logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export const AuthProvider = ({ children }: PropsWithChildren) => {
  const [loading, setLoading] = useState(true)
  const [bootstrap, setBootstrap] = useState<AuthorizationBootstrap>()
  const [error, setError] = useState<string>()

  const login = useCallback(async (tenantId: string, returnTo = '/') => {
    setError(undefined)
    await idpOAuth.beginAuthorization(tenantId, returnTo)
  }, [])

  const logout = useCallback(async () => {
    await idpOAuth.revoke()
    setBootstrap(undefined)
  }, [])

  useEffect(() => {
    let active = true
    const initialize = async () => {
      if (window.location.pathname === '/login') {
        if (active) setLoading(false)
        return
      }
      try {
        let returnTo: string | undefined
        if (window.location.pathname === '/oauth/callback') {
          returnTo = await idpOAuth.handleCallback(window.location.search)
        } else if (!tokenStore.get()) {
          await idpOAuth.refresh()
        }
        const value = await idpApi<AuthorizationBootstrap>('/api/v1/auth/bootstrap')
        if (!active) return
        setBootstrap(value)
        if (returnTo) {
          window.history.replaceState({}, '', returnTo)
          window.dispatchEvent(new PopStateEvent('popstate'))
        }
      } catch (failure) {
        if (!active) return
        tokenStore.clear()
        setBootstrap(undefined)
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
    () => ({ loading, bootstrap, error, login, logout }),
    [bootstrap, error, loading, login, logout],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = (): AuthContextValue => {
  const value = useContext(AuthContext)
  if (!value) throw new Error('AuthProvider is required')
  return value
}
