import {PermissionGuard, useRbac3Authorization} from '@egon-cola/rbac3-react-sdk'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {Button, Card, Form, Input, InputNumber, Popconfirm, Space, Table, Tag} from 'antd'
import {useFeatureApi, useFeatureTenantContext} from '../shared/FeatureApi'
import {PageState} from '@egon-cola/admin-web-shared'
import {applicationApi, type TenantApplicationView} from './application.api'

export const ApplicationListPage = () => {
    const {status} = useRbac3Authorization()
  const { effectiveTenantId } = useFeatureTenantContext()
  const api = applicationApi(useFeatureApi())
  const queryKey = ['rbac3', 'tenant-applications', effectiveTenantId ?? 'none']
  const query = useQuery({
    queryKey,
    queryFn: api.tenantApplications,
    enabled: status === 'READY',
  })
  const queryClient = useQueryClient()
  const admit = useMutation({
    mutationFn: (values: { ddcApplicationId: string; displayPriority: number }) => api.admitTenantApplication(values.ddcApplicationId, values.displayPriority),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  })
  const changeStatus = useMutation({
    mutationFn: (application: TenantApplicationView) => api.changeTenantApplicationStatus(
      application.applicationId,
      application.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE',
      application.version,
    ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  })
  const remove = useMutation({
    mutationFn: (application: TenantApplicationView) => api.removeTenantApplication(application.applicationId, application.version),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  })
  return (
    <Card title="租户应用授权">
      <PermissionGuard permission="system:application:manage">
        <Form layout="inline" onFinish={(values) => admit.mutate(values)} style={{ marginBottom: 16 }}>
          <Form.Item name="ddcApplicationId" rules={[{ required: true }]}><Input placeholder="DDC Application ID" /></Form.Item>
          <Form.Item name="displayPriority" initialValue={0}><InputNumber min={0} precision={0} /></Form.Item>
          <Button type="primary" htmlType="submit" loading={admit.isPending}>纳入租户授权</Button>
        </Form>
      </PermissionGuard>
      <PageState loading={query.isPending} error={query.error ?? admit.error ?? changeStatus.error ?? remove.error} empty={query.data?.length === 0}>
        <Table<TenantApplicationView>
          rowKey="applicationId"
          dataSource={query.data ?? []}
          pagination={false}
          columns={[
            { title: '业务编码', dataIndex: 'businessCode' },
            { title: '应用编码', dataIndex: 'applicationCode' },
            { title: '应用名称', dataIndex: 'applicationName' },
            { title: '状态', dataIndex: 'status', render: (status: string) => <Tag>{status}</Tag> },
            { title: '优先级', dataIndex: 'displayPriority' },
            { title: '版本', dataIndex: 'version' },
            {
              title: '操作',
              render: (_value, application) => (
                <PermissionGuard permission="system:application:manage">
                  <Space>
                    <Button size="small" loading={changeStatus.isPending} onClick={() => changeStatus.mutate(application)}>
                      {application.status === 'ACTIVE' ? '暂停' : '启用'}
                    </Button>
                    <Popconfirm title="确认移除租户应用授权？" onConfirm={() => remove.mutate(application)}>
                      <Button size="small" danger loading={remove.isPending}>移除</Button>
                    </Popconfirm>
                  </Space>
                </PermissionGuard>
              ),
            },
          ]}
        />
      </PageState>
    </Card>
  )
}
