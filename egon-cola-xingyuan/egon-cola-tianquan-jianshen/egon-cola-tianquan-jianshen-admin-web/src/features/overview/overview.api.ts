import type { FeatureApiClient } from '../shared/FeatureApi'

export const overviewApi = (client: FeatureApiClient) => ({
  runtime: () => client.request<Record<string, unknown>>('/api/tianquan-jianshen/v1/runtime/status'),
})
