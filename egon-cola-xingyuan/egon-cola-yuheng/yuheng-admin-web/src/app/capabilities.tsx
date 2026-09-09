import {createContext, type PropsWithChildren, useContext, useMemo} from 'react'
import {useAuth} from '../auth/AuthContext'

export type Capability =
  | 'yuheng:read'
  | 'yuheng:groups:write'
  | 'yuheng:applications:write'
  | 'yuheng:credentials:write'
  | 'yuheng:catalog:write'
  | 'yuheng:drafts:write'
  | 'yuheng:releases:write'
  | 'yuheng:mcp:read'
  | 'yuheng:mcp:write'
  | 'yuheng:mcp:test'
  | 'yuheng:mcp:approve'
  | 'yuheng:mcp:runtime:read'

const CapabilityContext = createContext<ReadonlySet<string>>(new Set())

export const CapabilityProvider = ({ children }: PropsWithChildren) => {
    const {authorization} = useAuth()
  const capabilities = useMemo(
      () => new Set(authorization?.permissions ?? []),
      [authorization],
  )
  return (
    <CapabilityContext.Provider value={capabilities}>
      {children}
    </CapabilityContext.Provider>
  )
}

export const useCapability = (capability: Capability): boolean => {
  const capabilities = useContext(CapabilityContext)
  return hasCapability(capabilities, capability)
}

export const hasCapability = (
  capabilities: ReadonlySet<string>,
  capability: Capability,
): boolean => capabilities.has(capability) || capabilities.has('*')
