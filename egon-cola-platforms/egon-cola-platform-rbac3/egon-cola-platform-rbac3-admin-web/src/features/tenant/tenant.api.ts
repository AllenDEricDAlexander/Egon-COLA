import type { FeatureApiClient } from '../shared/FeatureApi'

export interface TenantView {
  readonly tenantId: string
  readonly tenantCode: string
  readonly tenantName: string
  readonly status: string
}

export const tenantApi = (client: FeatureApiClient) => ({
  detail: (tenantId: string) => client.request<TenantView>(
    `/api/rbac3/v1/platform/tenants/${encodeURIComponent(tenantId)}`,
  ),
})
