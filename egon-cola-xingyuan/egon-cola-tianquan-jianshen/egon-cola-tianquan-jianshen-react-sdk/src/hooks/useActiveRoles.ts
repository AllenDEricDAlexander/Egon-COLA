import {useRbac3Authorization} from './useRbac3Authorization'

export const useActiveRoles = () => {
    const {status, activeRoles, replaceActiveRoles} = useRbac3Authorization()
  return { status, activeRoles, replaceActiveRoles }
}
