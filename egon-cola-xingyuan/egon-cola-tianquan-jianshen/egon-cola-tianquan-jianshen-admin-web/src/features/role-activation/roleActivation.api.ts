import type {ActiveRoleSetView, RoleActivationCandidateView} from '@egon-cola/tianquan-jianshen-react-sdk'
import type {FeatureApiClient} from '../shared/FeatureApi'

export const roleActivationApi = (client: FeatureApiClient) => ({
  candidates: () => client.request<RoleActivationCandidateView>('/api/tianquan-jianshen/v1/auth/role-activation-candidates'),
  current: () => client.request<ActiveRoleSetView>('/api/tianquan-jianshen/v1/auth/role-activations'),
})
