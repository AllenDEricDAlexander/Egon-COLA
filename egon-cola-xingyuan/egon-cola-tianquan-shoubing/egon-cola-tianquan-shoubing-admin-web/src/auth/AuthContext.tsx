import {createContext, type PropsWithChildren, useCallback, useContext, useEffect, useMemo, useState,} from 'react'
import {createGatewayAuthClient, createHttpClient,} from '@egon-cola/xingyuan-admin-web-shared'
import type {AuthorizationBootstrap} from '../api/types'

const gatewayOrigin = import.meta.env.VITE_YUHENG_ORIGIN ?? ''

const gatewayAuth = createGatewayAuthClient({
  registrationId: import.meta.env.VITE_OAUTH_REGISTRATION_ID ?? 'edge-shoubing',baseUrl: gatewayOrigin})
const httpClient = createHttpClient({
    baseUrl: gatewayOrigin,
  credentials: 'include',
    onAuthError: () => undefined,
})

export {gatewayAuth, httpClient}

interface AuthContextValue {
  readonly loading: boolean
  readonly bootstrap?: AuthorizationBootstrap
  readonly error?: string
  readonly login: () => void
  readonly logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export const AuthProvider = ({ children }: PropsWithChildren) => {
  const [loading, setLoading] = useState(true)
  const [bootstrap, setBootstrap] = useState<AuthorizationBootstrap>()
  const [error, setError] = useState<string>()

  const login = useCallback(() => {
    setError(undefined)
    setLoading(true)
    try {
      gatewayAuth.login()
    } catch (failure) {
      setLoading(false)
      setError(failure instanceof Error ? failure.message : '统一身份登录失败')
    }
  }, [])

  const logout = useCallback(async () => {
    try {
      const result = await gatewayAuth.logout()
      setBootstrap(undefined)
      setError(result.remoteState === 'CONFIRMED'
        ? undefined
        : '本地已退出，远端令牌撤销待恢复')
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : '统一身份退出失败')
    }
  }, [])

  useEffect(() => {
    let active = true
      void gatewayAuth.bootstrap<AuthorizationBootstrap>(
          '/api/v1/tianquan-shoubing/auth/bootstrap',
      )
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
    () => ({ loading, bootstrap, error, login, logout }),
    [loading, bootstrap, error, login, logout],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = (): AuthContextValue => {
  const value = useContext(AuthContext)
  if (!value) throw new Error('AuthProvider is required')
  return value
}
