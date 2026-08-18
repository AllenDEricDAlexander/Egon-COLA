import { PermissionGuard, useRbac3Authorization } from '@egon-cola/rbac3-react-sdk'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button, Card, Checkbox, Form, Input, Select, Space, Table, Tag } from 'antd'
import { useState } from 'react'
import { PageState } from '@egon-cola/admin-web-shared'
import { useFeatureApi, useFeatureTenantContext } from '../shared/FeatureApi'
import { applicationApi, type FieldDefinitionView } from './application.api'

export const FieldDefinitionPage = () => {
  const { status } = useRbac3Authorization()
  const { effectiveTenantId } = useFeatureTenantContext()
  const api = applicationApi(useFeatureApi())
  const queryClient = useQueryClient()
  const [applicationId, setApplicationId] = useState('')
  const applications = useQuery({
    queryKey: ['rbac3', 'catalog-applications', effectiveTenantId ?? 'none'],
    queryFn: api.applications,
    enabled: status === 'READY',
  })
  const resolvedApplicationId = applicationId || applications.data?.[0]?.applicationId || ''
  const queryKey = ['rbac3', 'fields', effectiveTenantId ?? 'none', resolvedApplicationId]
  const fields = useQuery({
    queryKey,
    queryFn: () => api.fields(resolvedApplicationId),
    enabled: status === 'READY' && resolvedApplicationId.length > 0,
  })
  const create = useMutation({
    mutationFn: (values: Omit<FieldDefinitionView, 'id' | 'status' | 'version' | 'applicationId'>) => api.createField({
      ...values,
      applicationId: resolvedApplicationId,
    }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  })
  const changeStatus = useMutation({
    mutationFn: (field: FieldDefinitionView) => api.changeFieldStatus(
      field.id,
      field.status === 'ACTIVE' ? 'ARCHIVED' : 'ACTIVE',
      field.version,
    ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  })
  return (
    <Card title="字段定义与字段权限">
      <Space style={{ marginBottom: 16 }}>
        <span>应用</span>
        <Select
          style={{ minWidth: 240 }}
          value={resolvedApplicationId || undefined}
          loading={applications.isPending}
          options={(applications.data ?? []).map((application) => ({ label: `${application.applicationName} (${application.applicationCode})`, value: application.applicationId }))}
          onChange={setApplicationId}
        />
      </Space>
      <PermissionGuard permission="system:field-definition:manage">
        <Form layout="inline" onFinish={(values) => create.mutate(values)} style={{ marginBottom: 16 }}>
          <Form.Item name="resourceId" rules={[{ required: true }]}><Input placeholder="资源 ID" /></Form.Item>
          <Form.Item name="fieldCode" rules={[{ required: true }]}><Input placeholder="字段编码" /></Form.Item>
          <Form.Item name="jsonPath" rules={[{ required: true }]}><Input placeholder="JSON Path" /></Form.Item>
          <Form.Item name="dataType" initialValue="STRING"><Input placeholder="数据类型" /></Form.Item>
          <Form.Item name="sensitivity" initialValue="NORMAL"><Input placeholder="敏感级别" /></Form.Item>
          <Form.Item name="defaultAccess" initialValue="NONE"><Input placeholder="默认访问级别" /></Form.Item>
          <Form.Item name="maskingStrategy"><Input placeholder="脱敏策略（可选）" /></Form.Item>
          <Form.Item name="writable" valuePropName="checked" initialValue={false}><Checkbox>可写</Checkbox></Form.Item>
          <Form.Item name="exportable" valuePropName="checked" initialValue={false}><Checkbox>可导出</Checkbox></Form.Item>
          <Button type="primary" htmlType="submit" loading={create.isPending} disabled={!resolvedApplicationId}>新增字段</Button>
        </Form>
      </PermissionGuard>
      <PageState loading={applications.isPending || fields.isPending} error={applications.error ?? fields.error ?? create.error ?? changeStatus.error} empty={fields.data?.length === 0}>
        <Table<FieldDefinitionView>
          rowKey="id"
          dataSource={fields.data ?? []}
          pagination={false}
          columns={[
            { title: '字段编码', dataIndex: 'fieldCode' },
            { title: 'JSON Path', dataIndex: 'jsonPath' },
            { title: '敏感级别', dataIndex: 'sensitivity' },
            { title: '默认访问', dataIndex: 'defaultAccess' },
            { title: '脱敏策略', dataIndex: 'maskingStrategy' },
            { title: '状态', dataIndex: 'status', render: (value: string) => <Tag>{value}</Tag> },
            {
              title: '操作',
              render: (_value, field) => (
                <PermissionGuard permission="system:field-definition:manage">
                  <Button size="small" onClick={() => changeStatus.mutate(field)}>
                    {field.status === 'ACTIVE' ? '归档' : '激活'}
                  </Button>
                </PermissionGuard>
              ),
            },
          ]}
        />
      </PageState>
    </Card>
  )
}
