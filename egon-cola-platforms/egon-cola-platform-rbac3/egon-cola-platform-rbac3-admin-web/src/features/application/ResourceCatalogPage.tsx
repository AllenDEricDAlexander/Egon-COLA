import {PermissionGuard, useRbac3Authorization} from '@egon-cola/rbac3-react-sdk'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {Button, Card, Select, Popconfirm, Space, Table, Tag} from 'antd'
import {useFeatureApi, useFeatureTenantContext} from '../shared/FeatureApi'
import {PageState} from '@egon-cola/admin-web-shared'
import {applicationApi, type ResourceView} from './application.api'
import {useState} from 'react'

export interface ResourceCatalogPageProps {
  readonly applicationId?: string
}

export const ResourceCatalogPage = ({ applicationId: initialApplicationId }: ResourceCatalogPageProps) => {
    const {status} = useRbac3Authorization()
  const { effectiveTenantId } = useFeatureTenantContext()
  const featureClient = useFeatureApi()
  const api = applicationApi(featureClient)
  const queryClient = useQueryClient()
  const [selectedApplicationId, setSelectedApplicationId] = useState(initialApplicationId ?? '')
  const applications = useQuery({
    queryKey: ['rbac3', 'catalog-applications', effectiveTenantId ?? 'none'],
    queryFn: api.applications,
    enabled: status === 'READY',
  })
  const applicationId = selectedApplicationId || applications.data?.[0]?.applicationId || ''
  const queryKey = ['rbac3', 'resources', effectiveTenantId ?? 'none', applicationId]
  const query = useQuery({
    queryKey,
    queryFn: () => api.resources(applicationId),
    enabled: status === 'READY' && applicationId.length > 0,
  })
  const archive = useMutation({
    mutationFn: api.archive,
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  })
  return (
    <Card title="资源目录">
      <Space style={{ marginBottom: 16 }}>
        <span>应用</span>
        <Select
          style={{ minWidth: 220 }}
          value={applicationId || undefined}
          loading={applications.isPending}
          options={(applications.data ?? []).map((application) => ({ label: `${application.applicationName} (${application.applicationCode})`, value: application.applicationId }))}
          onChange={setSelectedApplicationId}
        />
      </Space>
      <PageState loading={applications.isPending || query.isPending} error={applications.error ?? query.error ?? archive.error} empty={query.data?.length === 0}>
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
