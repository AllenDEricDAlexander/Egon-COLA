import type { FeatureApiClient } from '../shared/FeatureApi'

export const overviewApi = (client: FeatureApiClient) => ({
  runtime: () => client.request<Record<string, unknown>>('/api/rbac3/v1/runtime/status'),
})
