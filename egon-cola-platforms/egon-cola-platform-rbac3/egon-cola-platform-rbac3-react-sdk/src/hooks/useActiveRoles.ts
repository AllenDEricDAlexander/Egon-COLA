import { useRbac3Session } from './useRbac3Session'

export const useActiveRoles = () => {
  const { status, activeRoles, replaceActiveRoles } = useRbac3Session()
  return { status, activeRoles, replaceActiveRoles }
}
