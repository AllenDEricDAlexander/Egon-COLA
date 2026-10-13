import {createContext, type PropsWithChildren, useCallback, useContext, useEffect, useMemo, useRef, useState} from 'react'
import {createGatewayAuthClient, createHttpClient} from '@egon-cola/xingyuan-admin-web-shared'
import type {AuthorizationBootstrap} from '../api/types'

const gatewayOrigin = import.meta.env.VITE_YUHENG_ORIGIN ?? ''
const gatewayAuth = createGatewayAuthClient({
  baseUrl: gatewayOrigin,
  registrationId: import.meta.env.VITE_OAUTH_REGISTRATION_ID ?? 'edge-yuheng',
})
let authErrorHandler: (() => void) | undefined
const httpClient = createHttpClient({
  baseUrl: gatewayOrigin,
  credentials: 'include',
  onAuthError: () => authErrorHandler?.(),
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

export const AuthProvider = ({children}: PropsWithChildren) => {
  const [loading, setLoading] = useState(true)
  const [authorization, setAuthorization] = useState<AuthorizationBootstrap>()
  const [error, setError] = useState<string>()
  const generation = useRef(0)

  const nextGeneration = useCallback(() => ++generation.current, [])

  const clearAuthorization = useCallback(() => {
    const clearedGeneration = nextGeneration()
    setAuthorization(undefined)
    setLoading(false)
    return clearedGeneration
  }, [nextGeneration])

  const refreshAuthorization = useCallback(async () => {
    const requestedGeneration = nextGeneration()
    try {
      const value = await gatewayAuth.bootstrap<AuthorizationBootstrap>()
      if (requestedGeneration === generation.current) setAuthorization(value)
    } catch (failure) {
      if (requestedGeneration === generation.current) clearAuthorization()
      throw failure
    } finally {
      if (requestedGeneration === generation.current) setLoading(false)
    }
  }, [clearAuthorization, nextGeneration])

  const login = useCallback(() => {
    clearAuthorization()
    setLoading(true)
    setError(undefined)
    try {
      gatewayAuth.login()
    } catch (failure) {
      setLoading(false)
      setError(failure instanceof Error ? failure.message : '登录失败')
    }
  }, [clearAuthorization])

  const logout = useCallback(async () => {
    const requestedGeneration = clearAuthorization()
    try {
      const result = await gatewayAuth.logout()
      if (requestedGeneration !== generation.current) return
      setError(result.remoteState === 'CONFIRMED'
        ? undefined
        : '本地已退出，远端令牌撤销待恢复')
    } catch (failure) {
      if (requestedGeneration === generation.current) {
        setError(failure instanceof Error ? failure.message : '统一身份退出失败')
      }
    }
  }, [clearAuthorization])

  useEffect(() => {
    let active = true
    const requestedGeneration = generation.current
    authErrorHandler = clearAuthorization
    void gatewayAuth.bootstrap<AuthorizationBootstrap>()
      .then((value) => {
        if (active && requestedGeneration === generation.current) setAuthorization(value)
      })
      .catch(() => {
        if (active && requestedGeneration === generation.current) setAuthorization(undefined)
      })
      .finally(() => {
        if (active && requestedGeneration === generation.current) setLoading(false)
      })
    return () => {
      active = false
      nextGeneration()
      if (authErrorHandler === clearAuthorization) authErrorHandler = undefined
    }
  }, [clearAuthorization, nextGeneration])

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
