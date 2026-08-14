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

interface AuthContextValue {
  readonly loading: boolean
  readonly bootstrap?: AuthorizationBootstrap
    readonly login: (
        tenantId: string,
        username: string,
        password: string,
    ) => Promise<void>
  readonly logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export const AuthProvider = ({ children }: PropsWithChildren) => {
  const [loading, setLoading] = useState(true)
  const [bootstrap, setBootstrap] = useState<AuthorizationBootstrap>()

    const login = useCallback(async (
        tenantId: string,
        username: string,
        password: string,
    ) => {
        setLoading(true)
        await gatewayAuth.login({tenantId, username, password})
        setBootstrap(await gatewayAuth.bootstrap<AuthorizationBootstrap>())
        setLoading(false)
  }, [])

  const logout = useCallback(async () => {
      await gatewayAuth.logout()
    setBootstrap(undefined)
  }, [])

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
