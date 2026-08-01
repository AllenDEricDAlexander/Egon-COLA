import type { ReactNode } from 'react'
import { usePermission } from '../hooks/usePermission'

export interface PermissionGuardProps {
  readonly permission: string
  readonly children: ReactNode
  readonly fallback?: ReactNode
}

export const PermissionGuard = ({
  permission,
  children,
  fallback = null,
}: PermissionGuardProps) => (
  usePermission(permission) ? children : fallback
)
