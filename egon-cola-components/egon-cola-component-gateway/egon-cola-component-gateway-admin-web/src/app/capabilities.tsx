import { createContext, useContext, type PropsWithChildren } from 'react'

export type Capability =
  | 'gateway.group.read'
  | 'gateway.draft.write'
  | 'gateway.release.publish'
  | 'gateway.release.rollback'
  | 'gateway.operation.manage'
  | 'gateway.audit.read'

const CapabilityContext = createContext<ReadonlySet<Capability>>(new Set())

const developmentCapabilities = new Set<Capability>([
  'gateway.group.read',
  'gateway.draft.write',
  'gateway.release.publish',
  'gateway.release.rollback',
  'gateway.operation.manage',
  'gateway.audit.read',
])

export const CapabilityProvider = ({ children }: PropsWithChildren) => (
  <CapabilityContext.Provider value={developmentCapabilities}>
    {children}
  </CapabilityContext.Provider>
)

export const useCapability = (capability: Capability): boolean =>
  useContext(CapabilityContext).has(capability)
