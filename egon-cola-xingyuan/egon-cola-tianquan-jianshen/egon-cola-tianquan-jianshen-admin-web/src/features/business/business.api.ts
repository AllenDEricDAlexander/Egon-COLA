import type {FeatureApiClient} from '../shared/FeatureApi'

export interface BusinessCatalogView {
  readonly ddcBusinessId: string
  readonly bizCode: string
  readonly bizName: string
  readonly enabled: boolean
}

export interface ApplicationCatalogView {
  readonly ddcApplicationId: string
  readonly ddcBusinessId: string
  readonly bizCode: string
  readonly appCode: string
  readonly appName: string
  readonly applicationEnabled: boolean
  readonly businessEnabled: boolean
}

export const businessApi = (client: FeatureApiClient) => ({
  businesses: (keyword?: string) => client.request<readonly BusinessCatalogView[]>(
    '/api/tianquan-jianshen/v1/iam/catalog/businesses',
    {query: {keyword: keyword?.trim() || undefined}},
  ),
  applications: (ddcBusinessId: string, keyword?: string) => client.request<readonly ApplicationCatalogView[]>(
    `/api/tianquan-jianshen/v1/iam/catalog/businesses/${encodeURIComponent(ddcBusinessId)}/applications`,
    {query: {keyword: keyword?.trim() || undefined}},
  ),
})
