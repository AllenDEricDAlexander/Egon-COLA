import { PermissionGuard, useRbac3Authorization } from '@egon-cola/rbac3-react-sdk'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button, Card, Form, Input, Select, Space, Table, Tag } from 'antd'
import { useState } from 'react'
import { PageState } from '@egon-cola/admin-web-shared'
import { useFeatureApi, useFeatureTenantContext } from '../shared/FeatureApi'
import { applicationApi, type PermissionView } from './application.api'

export const PermissionPage = () => {
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
  const queryKey = ['rbac3', 'permissions', effectiveTenantId ?? 'none', resolvedApplicationId]
  const permissions = useQuery({
    queryKey,
    queryFn: () => api.permissions(resolvedApplicationId),
    enabled: status === 'READY' && resolvedApplicationId.length > 0,
  })
  const create = useMutation({
    mutationFn: (values: { permissionCode: string; permissionName: string; riskLevel: string }) => api.createPermission({
      applicationId: resolvedApplicationId,
      ...values,
    }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  })
  const changeStatus = useMutation({
    mutationFn: (permission: PermissionView) => api.changePermissionStatus(
      permission.id,
      permission.status === 'ACTIVE' ? 'ARCHIVED' : 'ACTIVE',
      permission.version,
    ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  })
  return (
    <Card title="权限字符">
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
      <PermissionGuard permission="system:permission:manage">
        <Form layout="inline" onFinish={(values) => create.mutate(values)} style={{ marginBottom: 16 }}>
          <Form.Item name="permissionCode" rules={[{ required: true }]}><Input placeholder="权限字符" /></Form.Item>
          <Form.Item name="permissionName" rules={[{ required: true }]}><Input placeholder="权限名称" /></Form.Item>
          <Form.Item name="riskLevel" initialValue="NORMAL"><Input placeholder="风险等级" /></Form.Item>
          <Button type="primary" htmlType="submit" loading={create.isPending} disabled={!resolvedApplicationId}>新增权限</Button>
        </Form>
      </PermissionGuard>
      <PageState loading={applications.isPending || permissions.isPending} error={applications.error ?? permissions.error ?? create.error ?? changeStatus.error} empty={permissions.data?.length === 0}>
        <Table<PermissionView>
          rowKey="id"
          dataSource={permissions.data ?? []}
          pagination={false}
          columns={[
            { title: '权限字符', dataIndex: 'permissionCode' },
            { title: '名称', dataIndex: 'permissionName' },
            { title: '风险', dataIndex: 'riskLevel' },
            { title: '来源', dataIndex: 'sourceType' },
            { title: '状态', dataIndex: 'status', render: (value: string) => <Tag>{value}</Tag> },
            {
              title: '操作',
              render: (_value, permission) => (
                <PermissionGuard permission="system:permission:manage">
                  <Button size="small" onClick={() => changeStatus.mutate(permission)}>
                    {permission.status === 'ACTIVE' ? '归档' : '激活'}
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
