import {createContext, type PropsWithChildren, useContext, useMemo} from 'react'
import {useAuth} from '../auth/AuthContext'

export type Capability =
  | 'gateway:read'
  | 'gateway:groups:write'
  | 'gateway:applications:write'
  | 'gateway:credentials:write'
  | 'gateway:catalog:write'
  | 'gateway:drafts:write'
  | 'gateway:releases:write'
  | 'gateway:mcp:read'
  | 'gateway:mcp:write'
  | 'gateway:mcp:test'
  | 'gateway:mcp:release'
  | 'gateway:mcp:approve'
  | 'gateway:mcp:runtime:read'

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
