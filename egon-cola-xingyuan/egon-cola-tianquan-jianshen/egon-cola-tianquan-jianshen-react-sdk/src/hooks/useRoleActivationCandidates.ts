import {useRbac3Authorization} from './useRbac3Authorization'

export const useRoleActivationCandidates = () => {
    const {status, candidates} = useRbac3Authorization()
  return { status, candidates }
}
