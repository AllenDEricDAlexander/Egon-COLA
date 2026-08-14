import {createContext, type PropsWithChildren, useCallback, useContext, useEffect, useMemo, useState,} from 'react'
import {createGatewayAuthClient, createHttpClient,} from '@egon-cola/admin-web-shared'
import type {AuthorizationBootstrap} from '../api/types'

const gatewayOrigin = import.meta.env.VITE_GATEWAY_ORIGIN ?? ''
const gatewayAuth = createGatewayAuthClient({baseUrl: gatewayOrigin})
const httpClient = createHttpClient({
    baseUrl: gatewayOrigin,
  credentials: 'include',
    onAuthError: () => undefined,
})

export {gatewayAuth, httpClient}

type AuthState = {
  loading: boolean
    authorization?: AuthorizationBootstrap
  error?: string
    login: (tenantId: string, username: string, password: string) => Promise<void>
  logout: () => Promise<void>
    refreshAuthorization: () => Promise<void>
}

const AuthContext = createContext<AuthState | undefined>(undefined)

export const AuthProvider = ({ children }: PropsWithChildren) => {
  const [loading, setLoading] = useState(true)
    const [authorization, setAuthorization] = useState<AuthorizationBootstrap>()
  const [error, setError] = useState<string>()

    const refreshAuthorization = useCallback(async () => {
        const value = await gatewayAuth.bootstrap<AuthorizationBootstrap>()
        setAuthorization(value)
  }, [])

    const login = useCallback(async (
        tenantId: string,
        username: string,
        password: string,
    ) => {
        setLoading(true)
        setError(undefined)
        try {
            await gatewayAuth.login({tenantId, username, password})
            await refreshAuthorization()
        } catch (failure) {
            setError(failure instanceof Error ? failure.message : '登录失败')
            throw failure
        } finally {
            setLoading(false)
        }
    }, [refreshAuthorization])

    const logout = useCallback(async () => {
        await gatewayAuth.logout()
        setAuthorization(undefined)
  }, [])

  useEffect(() => {
    let active = true
      void gatewayAuth.bootstrap<AuthorizationBootstrap>()
          .then((value) => {
              if (active) setAuthorization(value)
          })
          .catch(() => {
              if (active) setAuthorization(undefined)
          })
          .finally(() => {
        if (active) setLoading(false)
          })
    return () => { active = false }
  }, [])

  const value = useMemo(
      () => ({loading, authorization, error, login, logout, refreshAuthorization}),
      [authorization, error, loading, login, logout, refreshAuthorization],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = (): AuthState => {
  const value = useContext(AuthContext)
  if (!value) throw new Error('AuthProvider is required')
  return value
}
