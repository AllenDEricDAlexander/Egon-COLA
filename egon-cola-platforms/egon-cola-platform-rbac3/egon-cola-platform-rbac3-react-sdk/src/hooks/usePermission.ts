import {useRbac3Authorization} from './useRbac3Authorization'

export const usePermission = (permission: string): boolean => {
    const {status, bootstrap} = useRbac3Authorization()
  return status === 'READY'
    && bootstrap !== null
    && bootstrap.permissions.includes(permission)
}
