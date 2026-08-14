import {createContext, type ReactNode, useCallback, useContext, useEffect, useMemo, useState} from 'react'
import {createGatewayAuthClient,} from '@egon-cola/admin-web-shared'
import {setDdcUnauthorizedHandler} from '../api/client'
import type {AuthorizationBootstrap} from '../api/types'

const gatewayAuth = createGatewayAuthClient({
    baseUrl: import.meta.env.VITE_GATEWAY_ORIGIN ?? '',
})

export {gatewayAuth}

type AuthContextValue = {
  readonly identity: string
    readonly authorized: boolean
  loading: boolean
  error?: string
    login: (tenantId: string, username: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
    const [bootstrap, setBootstrap] = useState<AuthorizationBootstrap>()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string>()
    const identity = bootstrap?.user.identitySub ?? ''
    const authorized = bootstrap?.permissions.includes('DDC_READ') ?? false

    const clearAuthorization = useCallback(() => {
        setBootstrap(undefined)
    setLoading(false)
  }, [])

    const login = useCallback(async (tenantId: string, username: string, password: string) => {
    setError(undefined)
        setLoading(true)
        try {
            await gatewayAuth.login({tenantId, username, password})
            setBootstrap(await gatewayAuth.bootstrap<AuthorizationBootstrap>())
        } catch (failure) {
            setError(failure instanceof Error ? failure.message : '统一身份登录失败')
            throw failure
        } finally {
            setLoading(false)
        }
  }, [])

  const logout = useCallback(async () => {
      await gatewayAuth.logout()
      clearAuthorization()
  }, [clearAuthorization])

  useEffect(() => {
      setDdcUnauthorizedHandler(clearAuthorization)
  }, [clearAuthorization])

  useEffect(() => {
    let active = true
      void gatewayAuth.bootstrap<AuthorizationBootstrap>()
          .then((value) => {
              if (active) setBootstrap(value)
          })
          .catch(() => {
              if (active) setBootstrap(undefined)
          })
          .finally(() => {
              if (active) setLoading(false)
          })
    return () => { active = false }
  }, [])

  const value = useMemo(
      () => ({identity, authorized, loading, error, login, logout}),
      [authorized, error, identity, loading, login, logout],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = (): AuthContextValue => {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth must be used within AuthProvider')
  return value
}
