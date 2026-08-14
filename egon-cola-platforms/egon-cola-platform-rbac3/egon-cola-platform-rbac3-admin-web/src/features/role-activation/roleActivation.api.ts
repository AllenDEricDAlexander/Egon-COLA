import type {ActiveRoleSetView, RoleActivationCandidateView} from '@egon-cola/rbac3-react-sdk'
import type {FeatureApiClient} from '../shared/FeatureApi'

export const roleActivationApi = (client: FeatureApiClient) => ({
  candidates: () => client.request<RoleActivationCandidateView>('/api/rbac3/v1/auth/role-activation-candidates'),
  current: () => client.request<ActiveRoleSetView>('/api/rbac3/v1/auth/role-activations'),
})
