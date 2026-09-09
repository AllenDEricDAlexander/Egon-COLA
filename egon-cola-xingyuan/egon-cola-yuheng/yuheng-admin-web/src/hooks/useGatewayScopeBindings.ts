import { useQuery } from '@tanstack/react-query'
import { gatewayApi } from '../api/gatewayApi'

export const gatewayScopeBindingsQueryKey = ['gateway-scopes'] as const

export const useGatewayScopeBindings = () => {
  return useQuery({
    queryKey: gatewayScopeBindingsQueryKey,
    queryFn: ({ signal }) => gatewayApi.scopes(signal),
  })
}
