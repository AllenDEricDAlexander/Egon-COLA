import { useRbac3Session } from './useRbac3Session'

export const useRoleActivationCandidates = () => {
  const { status, candidates } = useRbac3Session()
  return { status, candidates }
}
