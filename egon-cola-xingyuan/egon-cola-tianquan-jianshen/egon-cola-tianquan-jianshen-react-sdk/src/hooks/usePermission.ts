import {useRbac3Authorization} from './useRbac3Authorization'

export const usePermission = (permission: string): boolean => {
    const {status, about} = useRbac3Authorization()
  return status === 'READY'
    && about !== null
    && about.permissions.includes(permission)
}
