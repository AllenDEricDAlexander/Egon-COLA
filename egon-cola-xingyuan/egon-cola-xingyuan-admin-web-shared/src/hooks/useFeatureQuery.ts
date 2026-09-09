import { useQuery, type UseQueryResult } from '@tanstack/react-query'

export interface FeatureQueryDeps {
  readonly status: string
  readonly effectiveTenantId: string | null
  readonly featureApi: {
    request<T>(path: string, req?: {
      method?: string
      query?: Record<string, unknown>
      body?: unknown
    }): Promise<T>
  }
}

export const useFeatureQuery = <T>(
  keys: readonly unknown[],
  queryFn: (api: FeatureQueryDeps['featureApi']) => Promise<T>,
  deps: FeatureQueryDeps,
  options?: { enabled?: boolean },
): UseQueryResult<T> => {
  const tenantId = deps.effectiveTenantId ?? 'none'
  return useQuery({
    queryKey: ['rbac3', ...keys, tenantId],
    queryFn: () => queryFn(deps.featureApi),
    enabled: deps.status === 'READY' && (options?.enabled ?? true),
    retry: false,
    refetchOnWindowFocus: false,
  })
}
