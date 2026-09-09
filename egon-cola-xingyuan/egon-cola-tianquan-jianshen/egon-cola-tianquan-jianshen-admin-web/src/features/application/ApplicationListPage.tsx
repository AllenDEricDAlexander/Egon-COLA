import {PermissionGuard, useRbac3Authorization} from '@egon-cola/tianquan-jianshen-react-sdk'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {Alert, Button, Card, Descriptions, Drawer, Form, Input, InputNumber, Popconfirm, Space, Table, Tag} from 'antd'
import {useState} from 'react'
import {useFeatureApi, useFeatureTenantContext} from '../shared/FeatureApi'
import {PageState} from '@egon-cola/xingyuan-admin-web-shared'
import {applicationApi, type TenantApplicationView} from './application.api'

export const ApplicationListPage = () => {
  const {status} = useRbac3Authorization()
  const {effectiveTenantId} = useFeatureTenantContext()
  const api = applicationApi(useFeatureApi())
  const queryClient = useQueryClient()
  const [selectedApplication, setSelectedApplication] = useState<TenantApplicationView | null>(null)
  const queryKey = ['rbac3', 'tenant-applications', effectiveTenantId ?? 'none']
  const detailKey = ['rbac3', 'tenant-application', effectiveTenantId ?? 'none', selectedApplication?.applicationId ?? 'none']
  const query = useQuery({
    queryKey,
    queryFn: api.tenantApplications,
    enabled: status === 'READY',
  })
  const detail = useQuery({
    queryKey: detailKey,
    queryFn: () => api.application(selectedApplication!.applicationId),
    enabled: status === 'READY' && selectedApplication !== null,
  })
  const refresh = async () => {
    await Promise.all([
      queryClient.invalidateQueries({queryKey}),
      queryClient.invalidateQueries({queryKey: ['rbac3', 'tenant-application']}),
    ])
  }
  const admit = useMutation({
    mutationFn: (values: {ddcApplicationId: string; displayPriority: number}) => api.admitTenantApplication(values.ddcApplicationId.trim(), values.displayPriority),
    onSuccess: refresh,
  })
  const changeStatus = useMutation({
    mutationFn: (application: TenantApplicationView) => api.changeTenantApplicationStatus(
      application.applicationId,
      application.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE',
      application.version,
    ),
    onSuccess: refresh,
  })
  const remove = useMutation({
    mutationFn: (application: TenantApplicationView) => api.removeTenantApplication(application.applicationId, application.version),
    onSuccess: async () => {
      setSelectedApplication(null)
      await refresh()
    },
  })
  const mutationError = admit.error ?? changeStatus.error ?? remove.error

  return (
    <Card title="租户应用授权">
      <PermissionGuard permission="system:application:manage">
        <Form layout="inline" onFinish={(values) => admit.mutate(values)} style={{marginBottom: 16}}>
          <Form.Item name="ddcApplicationId" rules={[{required: true, whitespace: true}]}>
            <Input placeholder="DDC Application ID" />
          </Form.Item>
          <Form.Item name="displayPriority" initialValue={0}>
            <InputNumber min={0} precision={0} />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={admit.isPending}>纳入租户授权</Button>
        </Form>
      </PermissionGuard>
      {mutationError && (
        <Alert
          type="error"
          showIcon
          title={mutationError instanceof Error ? mutationError.message : '租户应用操作失败'}
          action={<Button size="small" onClick={() => {void query.refetch()}}>刷新</Button>}
          style={{marginBottom: 16}}
        />
      )}
      <PageState loading={query.isPending} error={query.error} empty={query.data?.length === 0}>
        <Table<TenantApplicationView>
          rowKey="applicationId"
          dataSource={query.data ?? []}
          pagination={false}
          scroll={{x: 'max-content'}}
          columns={[
            {title: '业务编码', dataIndex: 'businessCode'},
            {title: '应用编码', dataIndex: 'applicationCode'},
            {title: '应用名称', dataIndex: 'applicationName'},
            {title: '状态', dataIndex: 'status', render: (value: string) => <Tag>{value}</Tag>},
            {title: '优先级', dataIndex: 'displayPriority'},
            {title: '版本', dataIndex: 'version'},
            {
              title: '操作',
              render: (_value: unknown, application: TenantApplicationView) => (
                <Space>
                  <Button size="small" onClick={() => setSelectedApplication(application)}>查看详情</Button>
                  <PermissionGuard permission="system:application:manage">
                    <Button size="small" loading={changeStatus.isPending} onClick={() => changeStatus.mutate(application)}>
                      {application.status === 'ACTIVE' ? '暂停' : '启用'}
                    </Button>
                    <Popconfirm title="确认移除租户应用授权？" okText="确认移除" cancelText="取消" onConfirm={() => remove.mutate(application)}>
                      <Button size="small" danger loading={remove.isPending}>移除</Button>
                    </Popconfirm>
                  </PermissionGuard>
                </Space>
              ),
            },
          ]}
        />
      </PageState>
      <Drawer
        title={selectedApplication ? `租户应用：${selectedApplication.applicationName}` : ''}
        open={selectedApplication !== null}
        onClose={() => setSelectedApplication(null)}
        size="large"
      >
        <PageState
          loading={detail.isPending}
          error={detail.error}
          empty={!detail.data}
          emptyDescription="租户应用详情不存在"
          onRetry={() => {void detail.refetch()}}
        >
          {detail.data && (
            <Descriptions bordered column={1} size="small">
              <Descriptions.Item label="应用 ID">{detail.data.applicationId}</Descriptions.Item>
              <Descriptions.Item label="DDC 业务 ID">{detail.data.ddcBusinessId}</Descriptions.Item>
              <Descriptions.Item label="DDC 应用 ID">{detail.data.ddcApplicationId}</Descriptions.Item>
              <Descriptions.Item label="业务编码">{detail.data.businessCode}</Descriptions.Item>
              <Descriptions.Item label="应用编码">{detail.data.applicationCode}</Descriptions.Item>
              <Descriptions.Item label="应用名称">{detail.data.applicationName}</Descriptions.Item>
              <Descriptions.Item label="状态">{detail.data.status}</Descriptions.Item>
              <Descriptions.Item label="显示优先级">{detail.data.displayPriority}</Descriptions.Item>
              <Descriptions.Item label="版本">{detail.data.version}</Descriptions.Item>
            </Descriptions>
          )}
        </PageState>
      </Drawer>
    </Card>
  )
}
