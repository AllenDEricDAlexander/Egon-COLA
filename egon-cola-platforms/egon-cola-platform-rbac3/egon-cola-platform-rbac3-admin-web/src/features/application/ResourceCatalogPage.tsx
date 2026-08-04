import { PermissionGuard, useRbac3Session } from '@egon-cola/rbac3-react-sdk'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button, Card, Popconfirm, Table, Tag } from 'antd'
import { useFeatureApi, useFeatureTenantContext } from '../shared/FeatureApi'
import { PageState } from '@egon-cola/admin-web-shared'
import { applicationApi, type ResourceView } from './application.api'

export interface ResourceCatalogPageProps {
  readonly applicationId: string
}

export const ResourceCatalogPage = ({ applicationId }: ResourceCatalogPageProps) => {
  const { status } = useRbac3Session()
  const { effectiveTenantId } = useFeatureTenantContext()
  const featureClient = useFeatureApi()
  const api = applicationApi(featureClient)
  const queryClient = useQueryClient()
  const queryKey = ['rbac3', 'resources', effectiveTenantId ?? 'none', applicationId]
  const query = useQuery({
    queryKey,
    queryFn: () => api.resources(applicationId),
    enabled: status === 'READY',
  })
  const archive = useMutation({
    mutationFn: api.archive,
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  })
  return (
    <Card title="资源目录">
      <PageState loading={query.isPending} error={query.error ?? archive.error} empty={query.data?.length === 0}>
        <Table<ResourceView>
          rowKey="resourceId"
          dataSource={query.data ?? []}
          pagination={false}
          columns={[
            { title: '类型', dataIndex: 'resourceType' },
            { title: '资源编码', dataIndex: 'resourceCode' },
            { title: '名称', dataIndex: 'resourceName' },
            { title: '状态', dataIndex: 'status', render: (status: string) => <Tag color={status === 'STALE' ? 'orange' : undefined}>{status}</Tag> },
            {
              title: '操作',
              render: (_value, resource) => resource.status === 'STALE' && (
                <PermissionGuard permission="system:resource:archive">
                  <Popconfirm title="确认归档已失效资源？" onConfirm={() => archive.mutate(resource)}>
                    <Button danger size="small">归档</Button>
                  </Popconfirm>
                </PermissionGuard>
              ),
            },
          ]}
        />
      </PageState>
    </Card>
  )
}
