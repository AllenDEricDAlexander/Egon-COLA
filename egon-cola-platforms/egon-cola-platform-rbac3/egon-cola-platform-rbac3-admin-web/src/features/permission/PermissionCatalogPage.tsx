import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {PermissionGuard, useRbac3Authorization} from '@egon-cola/rbac3-react-sdk'
import {PageState} from '@egon-cola/admin-web-shared'
import {Alert, Button, Card, Descriptions, Drawer, Form, Input, Modal, Select, Space, Table, Tag} from 'antd'
import {useState} from 'react'
import {useFeatureApi, useFeatureTenantContext} from '../shared/FeatureApi'
import {applicationApi, type PermissionView} from '../application/application.api'

type PermissionFormValues = {
  readonly permissionCode: string
  readonly permissionName: string
  readonly riskLevel: string
  readonly description?: string
}

const riskLevels = [
  {value: 'LOW', label: '低风险'},
  {value: 'MEDIUM', label: '中风险'},
  {value: 'HIGH', label: '高风险'},
  {value: 'CRITICAL', label: '关键风险'},
]

export const PermissionCatalogPage = () => {
  const {status} = useRbac3Authorization()
  const {effectiveTenantId} = useFeatureTenantContext()
  const api = applicationApi(useFeatureApi())
  const queryClient = useQueryClient()
  const [applicationId, setApplicationId] = useState('')
  const [permissionModalOpen, setPermissionModalOpen] = useState(false)
  const [selectedPermission, setSelectedPermission] = useState<PermissionView | null>(null)
  const [permissionForm] = Form.useForm<PermissionFormValues>()
  const tenant = effectiveTenantId ?? 'none'
  const applications = useQuery({
    queryKey: ['rbac3', 'resource-applications', tenant],
    queryFn: api.applications,
    enabled: status === 'READY',
  })

  const resolvedApplicationId = applicationId || applications.data?.[0]?.applicationId || ''
  const permissions = useQuery({
    queryKey: ['rbac3', 'permissions', tenant, resolvedApplicationId],
    queryFn: () => api.permissions(resolvedApplicationId),
    enabled: status === 'READY' && resolvedApplicationId.length > 0,
  })
  const permissionDetail = useQuery({
    queryKey: ['rbac3', 'permission', tenant, selectedPermission?.id ?? 'none'],
    queryFn: () => api.permission(selectedPermission!.id),
    enabled: status === 'READY' && selectedPermission !== null,
  })
  const queryKey = ['rbac3', 'permissions', tenant, resolvedApplicationId]
  const create = useMutation({
    mutationFn: (values: PermissionFormValues) => api.createPermission({
      applicationId: resolvedApplicationId,
      permissionCode: values.permissionCode.trim(),
      permissionName: values.permissionName.trim(),
      riskLevel: values.riskLevel,
      description: values.description?.trim() || undefined,
    }),
    onSuccess: async () => {
      setPermissionModalOpen(false)
      permissionForm.resetFields()
      await queryClient.invalidateQueries({queryKey})
    },
  })
  const changeStatus = useMutation({
    mutationFn: (permission: PermissionView) => api.changePermissionStatus(
      permission.id,
      permission.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE',
      permission.version,
    ),
    onSuccess: () => queryClient.invalidateQueries({queryKey}),
  })

  const submitCreate = async () => {
    try {
      create.mutate(await permissionForm.validateFields())
    } catch {
      return
    }
  }

  const mutationError = create.error ?? changeStatus.error

  return (
    <Card
      title="权限目录"
      extra={(
        <PermissionGuard permission="system:permission:manage">
          <Button type="primary" disabled={!resolvedApplicationId} onClick={() => setPermissionModalOpen(true)}>新建权限</Button>
        </PermissionGuard>
      )}
    >
      <Space wrap style={{marginBottom: 16}}>
        <Select
          aria-label="应用"
          placeholder="选择应用"
          style={{minWidth: 260}}
          value={resolvedApplicationId || undefined}
          loading={applications.isPending}
          options={(applications.data ?? []).map((application) => ({
            value: application.applicationId,
            label: `${application.applicationName} (${application.applicationCode})`,
          }))}
          onChange={setApplicationId}
        />
      </Space>
      {mutationError && (
        <Alert
          type="error"
          showIcon
          closable
          title={mutationError instanceof Error ? mutationError.message : String(mutationError)}
          style={{marginBottom: 16}}
        />
      )}
      <PageState
        loading={applications.isPending || permissions.isPending}
        error={applications.error ?? permissions.error}
        empty={Boolean(resolvedApplicationId) && permissions.data?.length === 0}
        emptyDescription="当前应用暂无权限字符"
        onRetry={() => { void permissions.refetch() }}
      >
        <Table<PermissionView>
          rowKey="id"
          dataSource={permissions.data ?? []}
          pagination={false}
          onRow={(permission) => ({onClick: () => setSelectedPermission(permission), style: {cursor: 'pointer'}})}
          columns={[
            {title: '权限字符', dataIndex: 'permissionCode', render: (value: string) => <Tag color="blue">{value}</Tag>},
            {title: '权限名称', dataIndex: 'permissionName'},
            {title: '风险等级', dataIndex: 'riskLevel'},
            {title: '来源', dataIndex: 'sourceType'},
            {title: '状态', dataIndex: 'status', render: (value: string) => <Tag>{value}</Tag>},
            {title: '版本', dataIndex: 'version'},
            {
              title: '操作',
              render: (_value: unknown, permission: PermissionView) => (
                <PermissionGuard permission="system:permission:manage">
                  <Button
                    size="small"
                    loading={changeStatus.isPending && changeStatus.variables?.id === permission.id}
                    onClick={(event) => { event.stopPropagation(); changeStatus.mutate(permission) }}
                  >
                    {permission.status === 'ACTIVE' ? '停用' : '启用'}
                  </Button>
                </PermissionGuard>
              ),
            },
          ]}
        />
      </PageState>

      <Drawer
        title={selectedPermission ? `权限：${selectedPermission.permissionCode}` : ''}
        open={selectedPermission !== null}
        onClose={() => setSelectedPermission(null)}
        size="large"
      >
        <PageState
          loading={permissionDetail.isPending}
          error={permissionDetail.error}
          empty={!permissionDetail.data}
          emptyDescription="权限详情不存在"
          onRetry={() => { void permissionDetail.refetch() }}
        >
          {permissionDetail.data && (
            <Descriptions bordered column={1} size="small">
              <Descriptions.Item label="权限 ID">{permissionDetail.data.id}</Descriptions.Item>
              <Descriptions.Item label="权限字符">{permissionDetail.data.permissionCode}</Descriptions.Item>
              <Descriptions.Item label="名称">{permissionDetail.data.permissionName}</Descriptions.Item>
              <Descriptions.Item label="风险等级">{permissionDetail.data.riskLevel}</Descriptions.Item>
              <Descriptions.Item label="来源">{permissionDetail.data.sourceType}</Descriptions.Item>
              <Descriptions.Item label="构建版本">{permissionDetail.data.sourceBuildId ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="校验和">{permissionDetail.data.sourceChecksum ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="状态">{permissionDetail.data.status}</Descriptions.Item>
              <Descriptions.Item label="版本">{permissionDetail.data.version}</Descriptions.Item>
            </Descriptions>
          )}
        </PageState>
      </Drawer>

      <Modal
        open={permissionModalOpen}
        title="新建权限"
        onCancel={() => setPermissionModalOpen(false)}
        onOk={() => { void submitCreate() }}
        okText="保存"
        confirmLoading={create.isPending}
        destroyOnHidden
      >
        <Form form={permissionForm} layout="vertical">
          <Form.Item name="permissionCode" label="权限字符" rules={[{required: true}]}><Input /></Form.Item>
          <Form.Item name="permissionName" label="权限名称" rules={[{required: true}]}><Input /></Form.Item>
          <Form.Item name="riskLevel" label="风险等级" initialValue="MEDIUM" rules={[{required: true}]}>
            <Select options={riskLevels} />
          </Form.Item>
          <Form.Item name="description" label="说明"><Input.TextArea rows={3} /></Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}
