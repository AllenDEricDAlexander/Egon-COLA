import { useQuery } from '@tanstack/react-query'
import { Result, Spin } from 'antd'
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from 'react'
import { gatewayApi } from '../api/gatewayApi'
import type { GatewayScopeBinding, Scope } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import {
  changeScope as cascadeScope,
  configuredInitialScope,
  resolveInitialScope,
  type ScopeField,
} from './scopeDefaults'

const STORAGE_KEY = 'egon.gateway.admin.scope.v1'

type ScopeContextValue = {
  scope: Scope
  bindings: GatewayScopeBinding[]
  changeScope: (field: ScopeField, value: string) => void
}

const ScopeContext = createContext<ScopeContextValue | null>(null)

const storedScope = (): Scope | undefined => {
  try {
    const value = localStorage.getItem(STORAGE_KEY)
    return value ? JSON.parse(value) as Scope : undefined
  } catch {
    return undefined
  }
}

const sameScope = (left: Scope, right: Scope) =>
  left.bizCode === right.bizCode
  && left.namespace === right.namespace
  && left.env === right.env
  && left.appCode === right.appCode

export const ScopeProvider = ({ children }: PropsWithChildren) => {
  const auth = useAuth()
  const [selectedScope, setSelectedScope] = useState<Scope | undefined>(
    storedScope,
  )
  const scopes = useQuery({
    queryKey: ['gateway-scopes'],
    queryFn: ({ signal }) => gatewayApi.scopes(signal),
    enabled: Boolean(auth.session),
  })

  const scope = useMemo(() => scopes.data
    ? resolveInitialScope(
        scopes.data,
        selectedScope,
        configuredInitialScope,
      )
    : undefined, [scopes.data, selectedScope])

  useEffect(() => {
    if (!scope || !scopes.data?.some((binding) => sameScope(binding, scope))) {
      return
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(scope))
  }, [scope, scopes.data])

  const changeScope = useCallback((field: ScopeField, value: string) => {
    if (scope && scopes.data) {
      setSelectedScope(cascadeScope(scopes.data, scope, field, value))
    }
  }, [scope, scopes.data])

  const contextValue = useMemo(() => scope && scopes.data
    ? { scope, bindings: scopes.data, changeScope }
    : null, [changeScope, scope, scopes.data])

  if (!auth.session) return children
  if (scopes.isLoading || (scopes.data?.length && !scope)) {
    return <Spin fullscreen tip="加载 DDC 作用域" />
  }
  if (scopes.error) {
    return <Result status="error" title="DDC 作用域加载失败" />
  }
  if (!scopes.data?.length) {
    return (
      <Result
        status="info"
        title="DDC 暂无已启用的 namespace-env-app 绑定"
      />
    )
  }
  if (!contextValue) {
    return <Result status="error" title="DDC 作用域解析失败" />
  }
  return (
    <ScopeContext.Provider value={contextValue}>
      {children}
    </ScopeContext.Provider>
  )
}

export const useScope = (): ScopeContextValue => {
  const value = useContext(ScopeContext)
  if (!value) {
    throw new Error('ScopeProvider is missing')
  }
  return value
}
