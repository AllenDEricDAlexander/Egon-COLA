import { useRbac3Session } from './useRbac3Session'

export const usePermission = (permission: string): boolean => {
  const { status, bootstrap } = useRbac3Session()
  return status === 'READY'
    && bootstrap !== null
    && bootstrap.permissions.includes(permission)
}
