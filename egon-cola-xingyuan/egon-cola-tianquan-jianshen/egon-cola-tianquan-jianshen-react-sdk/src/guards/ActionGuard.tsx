import type {ReactNode} from 'react'
import {useRbac3Authorization} from '../hooks/useRbac3Authorization'

export interface ActionGuardProps {
  readonly resourceCode: string
  readonly children: ReactNode
  readonly fallback?: ReactNode
}

/** Controls an ACTION by its resource code; permission characters remain an internal server key. */
export const ActionGuard = ({resourceCode, children, fallback = null}: ActionGuardProps) => {
  const {status, about} = useRbac3Authorization()
  return status === 'READY' && about !== null && about.resourceCodes.includes(resourceCode)
    ? children
    : fallback
}
