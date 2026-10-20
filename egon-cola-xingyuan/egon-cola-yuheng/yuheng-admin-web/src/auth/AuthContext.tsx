import {createContext, type PropsWithChildren, useCallback, useContext, useEffect, useMemo, useRef, useState} from 'react'
import {createGatewayAuthClient, createHttpClient} from '@egon-cola/xingyuan-admin-web-shared'
import type {AuthorizationBootstrap} from '../api/types'
import {workspaceBootstrapPath, type WorkspaceResource} from './workspaceResource'

const gatewayOrigin = import.meta.env.VITE_YUHENG_ORIGIN ?? ''
const gatewayAuth = createGatewayAuthClient({
  baseUrl: gatewayOrigin,
  registrationId: import.meta.env.VITE_OAUTH_REGISTRATION_ID ?? 'edge-yuheng',
})
const knowledgeAuth = createGatewayAuthClient({
  baseUrl: gatewayOrigin,
  registrationId: import.meta.env.VITE_YAOGUANG_OAUTH_REGISTRATION_ID ?? 'edge-yaoguang',
})
let authErrorHandler: (() => void) | undefined
const httpClient = createHttpClient({
  baseUrl: gatewayOrigin,
  credentials: 'include',
  onAuthError: () => authErrorHandler?.(),
})

export {gatewayAuth, httpClient}

type AuthState = {
  workspace: WorkspaceResource
  loading: boolean
  authorization?: AuthorizationBootstrap
  error?: string
  login: () => void
  logout: () => Promise<void>
  refreshAuthorization: () => Promise<void>
}

const AuthContext = createContext<AuthState | undefined>(undefined)

export const AuthProvider = ({children, resource = 'yuheng'}: PropsWithChildren<{resource?: WorkspaceResource}>) => {
  const authClient = resource === 'yaoguang' ? knowledgeAuth : gatewayAuth
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
      const value = await authClient.bootstrap<AuthorizationBootstrap>(workspaceBootstrapPath(resource))
      if (requestedGeneration === generation.current) setAuthorization(value)
    } catch (failure) {
      if (requestedGeneration === generation.current) clearAuthorization()
      throw failure
    } finally {
      if (requestedGeneration === generation.current) setLoading(false)
    }
  }, [authClient, resource, clearAuthorization, nextGeneration])

  const login = useCallback(() => {
    clearAuthorization()
    setLoading(true)
    setError(undefined)
    try {
      authClient.login()
    } catch (failure) {
      setLoading(false)
      setError(failure instanceof Error ? failure.message : '登录失败')
    }
  }, [authClient, clearAuthorization])

  const logout = useCallback(async () => {
    const requestedGeneration = clearAuthorization()
    try {
      const result = await authClient.logout()
      if (requestedGeneration !== generation.current) return
      setError(result.remoteState === 'CONFIRMED'
        ? undefined
        : '本地已退出，远端令牌撤销待恢复')
    } catch (failure) {
      if (requestedGeneration === generation.current) {
        setError(failure instanceof Error ? failure.message : '统一身份退出失败')
      }
    }
  }, [authClient, clearAuthorization])

  useEffect(() => {
    let active = true
    const requestedGeneration = generation.current
    authErrorHandler = clearAuthorization
    void authClient.bootstrap<AuthorizationBootstrap>(workspaceBootstrapPath(resource))
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
  }, [authClient, resource, clearAuthorization, nextGeneration])

  const value = useMemo(
    () => ({workspace: resource, loading, authorization, error, login, logout, refreshAuthorization}),
    [resource, authorization, error, loading, login, logout, refreshAuthorization],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = (): AuthState => {
  const value = useContext(AuthContext)
  if (!value) throw new Error('AuthProvider is required')
  return value
}
