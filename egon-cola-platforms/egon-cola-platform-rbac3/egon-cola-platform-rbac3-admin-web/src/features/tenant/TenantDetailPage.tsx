import { useQuery } from '@tanstack/react-query'
import { Card, Descriptions, Tag } from 'antd'
import { useRbac3Session } from '@egon-cola/rbac3-react-sdk'
import { useFeatureApi, useFeatureTenantContext } from '../shared/FeatureApi'
import { PageState } from '../shared/PageState'
import { tenantApi } from './tenant.api'

export interface TenantDetailPageProps {
  readonly tenantId: string
}

export const TenantDetailPage = ({ tenantId }: TenantDetailPageProps) => {
  const { status } = useRbac3Session()
  const { effectiveTenantId } = useFeatureTenantContext()
  const api = tenantApi(useFeatureApi())
  const query = useQuery({
    queryKey: ['rbac3', 'tenant', effectiveTenantId ?? 'none', tenantId],
    queryFn: () => api.detail(tenantId),
    enabled: status === 'READY',
  })
  return (
    <Card title="租户详情">
      <PageState loading={query.isPending} error={query.error} empty={!query.data}>
        {query.data && (
          <Descriptions bordered column={1}>
            <Descriptions.Item label="编码">{query.data.tenantCode}</Descriptions.Item>
            <Descriptions.Item label="名称">{query.data.tenantName}</Descriptions.Item>
            <Descriptions.Item label="状态"><Tag>{query.data.status}</Tag></Descriptions.Item>
          </Descriptions>
        )}
      </PageState>
    </Card>
  )
}
