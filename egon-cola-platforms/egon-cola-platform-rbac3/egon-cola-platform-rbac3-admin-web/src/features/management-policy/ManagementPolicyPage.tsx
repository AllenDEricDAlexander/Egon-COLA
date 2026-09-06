import {PermissionGuard, useRbac3Authorization} from '@egon-cola/rbac3-react-sdk'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {Alert, Button, Card, Descriptions, Drawer, Popconfirm, Space, Table, Tag, Typography} from 'antd'
import {useState} from 'react'
import {useFeatureApi, useFeatureTenantContext} from '../shared/FeatureApi'
import {PageState} from '@egon-cola/admin-web-shared'
import {ManagementPolicyEditor} from './ManagementPolicyEditor'
import {managementPolicyApi, type ManagementPolicyView, type SaveManagementPolicyCommand} from './managementPolicy.api'

export const ManagementPolicyPage = () => {
  const {status} = useRbac3Authorization()
  const {effectiveTenantId} = useFeatureTenantContext()
  const api = managementPolicyApi(useFeatureApi())
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const [selected, setSelected] = useState<ManagementPolicyView | null>(null)
  const queryKey = ['rbac3', 'management-policies', effectiveTenantId ?? 'none']
  const query = useQuery({ queryKey, queryFn: api.list, enabled: status === 'READY' })
  const capabilities = useQuery({
    queryKey: ['rbac3', 'management-capabilities', effectiveTenantId ?? 'none'],
    queryFn: api.capabilities,
    enabled: status === 'READY',
  })
  const detail = useQuery({
    queryKey: ['rbac3', 'management-policy', effectiveTenantId ?? 'none', selected?.policyId ?? 'none'],
    queryFn: () => api.get(selected!.policyId),
    enabled: status === 'READY' && selected !== null,
  })
  const manageableUsers = useQuery({
    queryKey: ['rbac3', 'manageable-users', effectiveTenantId ?? 'none'],
    queryFn: () => api.manageableUsers(),
    enabled: status === 'READY' && open,
  })
  const manageableRoles = useQuery({
    queryKey: ['rbac3', 'manageable-roles', effectiveTenantId ?? 'none'],
    queryFn: () => api.manageableRoles(),
    enabled: status === 'READY' && open,
  })
  const refreshPolicyViews = () => Promise.all([
    'management-policies', 'management-capabilities', 'manageable-users',
    'manageable-roles', 'management-policy',
  ].map((key) => queryClient.invalidateQueries({
    queryKey: ['rbac3', key, effectiveTenantId ?? 'none'],
  })))
  const save = useMutation({
    mutationFn: (command: SaveManagementPolicyCommand) => selected
      ? api.update(selected, command, crypto.randomUUID())
      : api.create(command, crypto.randomUUID()),
    onSuccess: async () => { await refreshPolicyViews(); setOpen(false); setSelected(null) },
  })
  const disable = useMutation({
    mutationFn: (policy: ManagementPolicyView) => api.disable(policy, crypto.randomUUID()),
    onSuccess: refreshPolicyViews,
  })
  const edit = (policy: ManagementPolicyView | null) => { setSelected(policy); setOpen(true) }
  const mutationError = save.error ?? disable.error
  return (
    <Card
      title="委托管理策略"
      extra={<PermissionGuard permission="system:management-policy:manage"><Button type="primary" onClick={() => edit(null)}>新增完整策略</Button></PermissionGuard>}
    >
      <Typography.Paragraph type="secondary">
        每次授权必须由同一条策略同时满足 Subject、Scope、激活根角色与 Operation；平台不会拼接多条策略的局部授权。
      </Typography.Paragraph>
      {capabilities.error && <Alert type="warning" showIcon title="当前操作者委托能力读取失败" style={{marginBottom: 16}} />}
      {capabilities.data && (
        <Card size="small" title="当前操作者能力" style={{marginBottom: 16}}>
          <Space wrap>
            <Tag color="blue">可用策略 {capabilities.data.policyIds?.length ?? 0}</Tag>
            <Tag color="blue">可用操作 {capabilities.data.operations?.length ?? 0}</Tag>
            <Tag color="blue">激活根角色 {capabilities.data.activationRootRoleIds?.length ?? 0}</Tag>
            {(capabilities.data.operations ?? []).map((operation) => <Tag key={operation}>{operation}</Tag>)}
          </Space>
        </Card>
      )}
      {mutationError && (
        <Alert
          type="error"
          showIcon
          title={mutationError instanceof Error ? mutationError.message : '委托策略操作失败'}
          style={{marginBottom: 16}}
        />
      )}
      <PageState loading={query.isPending} error={query.error} empty={query.data?.length === 0} onRetry={() => {void query.refetch()}}>
        <Table<ManagementPolicyView>
          rowKey="policyId"
          dataSource={query.data ?? []}
          pagination={false}
          scroll={{x: 'max-content'}}
          columns={[
            { title: '编码', dataIndex: 'policyCode' },
            { title: '名称', dataIndex: 'name' },
            { title: 'Subject', render: (_value, row) => row.subjects.map((item) => `${item.type}:${item.id}`).join(', ') },
            { title: 'Scope', render: (_value, row) => row.scopes.map((item) => `${item.type}:${item.referenceId ?? ''}`).join(', ') },
            { title: '激活根角色', render: (_value, row) => row.activationRootRoleIds.join(', ') },
            { title: 'Operation', render: (_value, row) => row.operations.join(', ') },
            { title: '状态', dataIndex: 'status', render: (value: string) => <Tag>{value}</Tag> },
            {
              title: '操作', render: (_value, row) => (
                <Space>
                  <Button size="small" onClick={() => setSelected(row)}>查看详情</Button>
                  <PermissionGuard permission="system:management-policy:manage">
                    <Button size="small" onClick={() => edit(row)}>编辑</Button>
                    {row.status === 'ACTIVE' && <Popconfirm title="确认禁用并保留历史？" onConfirm={() => disable.mutate(row)}><Button danger size="small">禁用</Button></Popconfirm>}
                  </PermissionGuard>
                </Space>
              ),
            },
          ]}
        />
      </PageState>
      {open && (
        <Alert
          type="info"
          showIcon
          title={`可选管理对象：用户 ${manageableUsers.data?.length ?? 0} 个，角色 ${manageableRoles.data?.length ?? 0} 个`}
          description={manageableUsers.error || manageableRoles.error ? '管理对象目录读取失败，仍可按后端合同填写 Subject 和 Scope；保存时服务端会再次校验范围。' : undefined}
          style={{marginTop: 16}}
        />
      )}
      <ManagementPolicyEditor open={open} policy={selected} saving={save.isPending} onCancel={() => { setOpen(false); setSelected(null) }} onSave={save.mutate} />
      <Drawer
        title={selected ? `委托策略：${selected.policyCode}` : ''}
        open={selected !== null && !open}
        onClose={() => setSelected(null)}
        size="large"
      >
        <PageState
          loading={detail.isPending}
          error={detail.error}
          empty={!detail.data}
          emptyDescription="委托策略详情不存在"
          onRetry={() => {void detail.refetch()}}
        >
          {detail.data && (
            <Descriptions bordered column={1} size="small">
              <Descriptions.Item label="策略 ID">{detail.data.policyId}</Descriptions.Item>
              <Descriptions.Item label="策略编码">{detail.data.policyCode}</Descriptions.Item>
              <Descriptions.Item label="名称">{detail.data.name}</Descriptions.Item>
              <Descriptions.Item label="状态">{detail.data.status}</Descriptions.Item>
              <Descriptions.Item label="Subject">{detail.data.subjects.map((item) => `${item.type}:${item.id}`).join(', ')}</Descriptions.Item>
              <Descriptions.Item label="Scope">{detail.data.scopes.map((item) => `${item.type}:${item.referenceId ?? ''}`).join(', ')}</Descriptions.Item>
              <Descriptions.Item label="激活根角色">{detail.data.activationRootRoleIds.join(', ')}</Descriptions.Item>
              <Descriptions.Item label="Operation">{detail.data.operations.join(', ')}</Descriptions.Item>
              <Descriptions.Item label="版本">{detail.data.version}</Descriptions.Item>
            </Descriptions>
          )}
        </PageState>
      </Drawer>
    </Card>
  )
}
