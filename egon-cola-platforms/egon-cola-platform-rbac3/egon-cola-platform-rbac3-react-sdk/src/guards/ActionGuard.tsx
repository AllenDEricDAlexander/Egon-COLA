import { PermissionGuard, type PermissionGuardProps } from './PermissionGuard'

export type ActionGuardProps = PermissionGuardProps

export const ActionGuard = (props: ActionGuardProps) => (
  <PermissionGuard {...props} />
)
