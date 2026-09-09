import {PermissionGuard, useRbac3Authorization} from '@egon-cola/tianquan-jianshen-react-sdk'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {Alert, Button, Card, Drawer, Form, Input, Popconfirm, Select, Space, Table, Tag} from 'antd'
import {useFeatureApi, useFeatureTenantContext} from '../shared/FeatureApi'
import {PageState} from '@egon-cola/xingyuan-admin-web-shared'
import {applicationApi, type ResourceView} from './application.api'
import {PermissionSelector} from './PermissionSelector'
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
  const [mappingResource, setMappingResource] = useState<ResourceView | null>(null)
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
  const mapping = useQuery({
    queryKey: ['rbac3', 'resource-permission-mapping', effectiveTenantId ?? 'none', mappingResource?.resourceId ?? 'none'],
    queryFn: () => api.permissionMapping(mappingResource!.resourceId),
    enabled: status === 'READY' && mappingResource !== null,
  })
  const updateMapping = useMutation({
    mutationFn: (values: { permissionId: string; reason?: string }) => api.updatePermissionMapping(
      mappingResource!.resourceId,
      {
        ...values,
        expectedResourceVersion: mapping.data?.resourceVersion ?? mappingResource!.version,
      },
    ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({queryKey: ['rbac3', 'resource-permission-mapping', effectiveTenantId ?? 'none', mappingResource?.resourceId ?? 'none']})
      await queryClient.invalidateQueries({queryKey})
      setMappingResource(null)
    },
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
              render: (_value, resource) => (
                <Space>
                  {resource.status === 'STALE' && (
                    <PermissionGuard permission="system:resource:archive">
                      <Popconfirm title="确认归档已失效资源？" onConfirm={() => archive.mutate(resource)}>
                        <Button danger size="small">归档</Button>
                      </Popconfirm>
                    </PermissionGuard>
                  )}
                  <PermissionGuard permission="system:resource-permission:read">
                    <Button size="small" onClick={() => setMappingResource(resource)}>映射权限</Button>
                  </PermissionGuard>
                </Space>
              ),
            },
          ]}
        />
      </PageState>
      <Drawer
        open={mappingResource !== null}
        title="资源实际权限映射"
        width={480}
        onClose={() => setMappingResource(null)}
        destroyOnHidden
      >
        <PageState loading={mapping.isPending} error={mapping.error ?? updateMapping.error} empty={!mapping.data}>
          {mapping.data && mappingResource && (
            <Form
              key={mappingResource.resourceId}
              layout="vertical"
              initialValues={{permissionId: mapping.data.actualPermissionId ?? undefined}}
              onFinish={(values) => updateMapping.mutate(values)}
            >
              <Form.Item label="资源">
                <Input value={`${mapping.data.resourceCode} (${mapping.data.resourceType})`} disabled />
              </Form.Item>
              <Form.Item label="代码建议">
                <Input value={mapping.data.suggestedPermissionCode ?? '未提供'} disabled />
              </Form.Item>
              <Form.Item name="permissionId" label="实际权限字符" rules={[{required: true, message: '请选择实际权限字符'}]}>
                <PermissionSelector applicationId={mapping.data.applicationId} />
              </Form.Item>
              {mapping.data.activeRoleCount > 0 && <Alert type="warning" showIcon message={`当前有 ${mapping.data.activeRoleCount} 个有效角色引用此资源`} />}
              <Form.Item name="reason" label="变更原因">
                <Input.TextArea maxLength={500} />
              </Form.Item>
              <Button type="primary" htmlType="submit" loading={updateMapping.isPending}>保存映射</Button>
            </Form>
          )}
        </PageState>
      </Drawer>
    </Card>
  )
}
