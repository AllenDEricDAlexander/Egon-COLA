import {createContext, type ReactNode, useCallback, useContext, useEffect, useMemo, useState} from 'react'
import {useQueryClient} from '@tanstack/react-query'
import {createGatewayAuthClient} from '@egon-cola/xingyuan-admin-web-shared'
import {
  beginDdcSession,
  isCurrentDdcSession,
  setDdcExpectedTenantId,
  setDdcTenantMismatchHandler,
  setDdcUnauthorizedHandler,
} from '../api/client'
import type {AuthorizationBootstrap} from '../api/types'
import {cancelAndClearDdcQueryClient} from '../query/queryClient'

const gatewayAuth = createGatewayAuthClient({
  baseUrl: import.meta.env.VITE_YUHENG_ORIGIN ?? '',
})

export {gatewayAuth}

type AuthContextValue = {
  readonly identity: string
  readonly tenantId: string
  readonly sessionGeneration: number
  readonly authorized: boolean
  loading: boolean
  error?: string
  login: (tenantId: string, username: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({children}: {children: ReactNode}) {
  const queryClient = useQueryClient()
  const [bootstrap, setBootstrap] = useState<AuthorizationBootstrap>()
  const [sessionGeneration, setSessionGeneration] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string>()
  const identity = bootstrap?.user.identitySub ?? ''
  const tenantId = bootstrap?.user.tenantId ?? ''
  const authorized = bootstrap?.permissions.includes('TIANSHU_READ') ?? false

  const beginSession = useCallback((): number => {
    const generation = beginDdcSession()
    setSessionGeneration(generation)
    setDdcExpectedTenantId(undefined)
    setBootstrap(undefined)
    void cancelAndClearDdcQueryClient(queryClient)
    return generation
  }, [queryClient])

  const loadBootstrap = useCallback(async (generation: number): Promise<void> => {
    const next = await gatewayAuth.bootstrap<AuthorizationBootstrap>(
      '/api/v1/tianshu/auth/bootstrap',
    )
    if (!isCurrentDdcSession(generation)) return
    setDdcExpectedTenantId(next.user.tenantId)
    setBootstrap(next)
  }, [])

  const clearAuthorization = useCallback(() => {
    beginSession()
    setError(undefined)
    setLoading(false)
  }, [beginSession])

  const refreshAfterTenantMismatch = useCallback(() => {
    const generation = beginSession()
    setError(undefined)
    setLoading(true)
    void loadBootstrap(generation)
      .catch((failure: unknown) => {
        if (isCurrentDdcSession(generation)) {
          setError(failure instanceof Error ? failure.message : '无法刷新当前租户身份')
        }
      })
      .finally(() => {
        if (isCurrentDdcSession(generation)) setLoading(false)
      })
  }, [beginSession, loadBootstrap])

  const login = useCallback(async (selectedTenantId: string, username: string, password: string) => {
    const generation = beginSession()
    setError(undefined)
    setLoading(true)
    try {
      await gatewayAuth.login({tenantId: selectedTenantId, username, password})
      await loadBootstrap(generation)
    } catch (failure) {
      if (isCurrentDdcSession(generation)) {
        setBootstrap(undefined)
        setError(failure instanceof Error ? failure.message : '统一身份登录失败')
      }
      throw failure
    } finally {
      if (isCurrentDdcSession(generation)) setLoading(false)
    }
  }, [beginSession, loadBootstrap])

  const logout = useCallback(async () => {
    const generation = beginSession()
    setError(undefined)
    setLoading(true)
    try {
      await gatewayAuth.logout()
    } catch (failure) {
      if (isCurrentDdcSession(generation)) {
        setError(failure instanceof Error ? failure.message : '统一身份退出失败')
      }
      throw failure
    } finally {
      if (isCurrentDdcSession(generation)) setLoading(false)
    }
  }, [beginSession])

  useEffect(() => {
    setDdcUnauthorizedHandler(clearAuthorization)
    setDdcTenantMismatchHandler(refreshAfterTenantMismatch)
    return () => {
      setDdcUnauthorizedHandler(() => {})
      setDdcTenantMismatchHandler(() => {})
    }
  }, [clearAuthorization, refreshAfterTenantMismatch])

  useEffect(() => {
    const generation = beginSession()
    let active = true
    setLoading(true)
    void loadBootstrap(generation)
      .catch(() => {
        if (active && isCurrentDdcSession(generation)) setBootstrap(undefined)
      })
      .finally(() => {
        if (active && isCurrentDdcSession(generation)) setLoading(false)
      })
    return () => {
      active = false
      if (isCurrentDdcSession(generation)) {
        beginDdcSession()
        setDdcExpectedTenantId(undefined)
        void cancelAndClearDdcQueryClient(queryClient)
      }
    }
  }, [beginSession, loadBootstrap, queryClient])

  const value = useMemo(
    () => ({identity, tenantId, sessionGeneration, authorized, loading, error, login, logout}),
    [authorized, error, identity, loading, login, logout, sessionGeneration, tenantId],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = (): AuthContextValue => {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth must be used within AuthProvider')
  return value
}
