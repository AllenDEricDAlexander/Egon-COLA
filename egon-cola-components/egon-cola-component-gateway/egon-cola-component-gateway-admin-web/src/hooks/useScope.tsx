import { createContext, useContext, useMemo, useState, type PropsWithChildren } from 'react'
import type { Scope } from '../api/types'
import { configuredInitialScope } from './scopeDefaults'

type ScopeContextValue = {
  scope: Scope
  setScope: (scope: Scope) => void
}

const ScopeContext = createContext<ScopeContextValue | null>(null)

export const ScopeProvider = ({ children }: PropsWithChildren) => {
  const [scope, setScope] = useState<Scope>(() => configuredInitialScope)
  const value = useMemo(() => ({ scope, setScope }), [scope])
  return <ScopeContext.Provider value={value}>{children}</ScopeContext.Provider>
}

export const useScope = (): ScopeContextValue => {
  const value = useContext(ScopeContext)
  if (!value) {
    throw new Error('ScopeProvider is missing')
  }
  return value
}
