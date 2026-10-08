import {createContext, type PropsWithChildren, useCallback, useContext, useEffect, useMemo, useState,} from 'react'
import {createGatewayAuthClient, createHttpClient,} from '@egon-cola/xingyuan-admin-web-shared'
import type {AuthorizationBootstrap} from '../api/types'

const gatewayOrigin = import.meta.env.VITE_YUHENG_ORIGIN ?? ''
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
  login: () => void
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

  const login = useCallback(() => {
    setLoading(true)
    setError(undefined)
    try {
      gatewayAuth.login()
    } catch (failure) {
      setLoading(false)
      setError(failure instanceof Error ? failure.message : '登录失败')
    }
  }, [])

  const logout = useCallback(async () => {
    try {
      const result = await gatewayAuth.logout()
      setError(result.remoteState === 'CONFIRMED'
        ? undefined
        : '本地已退出，远端令牌撤销待恢复')
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : '统一身份退出失败')
    } finally {
      setAuthorization(undefined)
    }
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
