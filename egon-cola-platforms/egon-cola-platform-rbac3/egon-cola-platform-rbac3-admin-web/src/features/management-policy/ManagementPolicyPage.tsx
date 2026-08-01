import { PermissionGuard, useRbac3Session } from '@egon-cola/rbac3-react-sdk'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button, Card, Popconfirm, Space, Table, Tag, Typography } from 'antd'
import { useState } from 'react'
import { useFeatureApi, useFeatureTenantContext } from '../shared/FeatureApi'
import { PageState } from '../shared/PageState'
import { ManagementPolicyEditor } from './ManagementPolicyEditor'
import { managementPolicyApi, type ManagementPolicyView, type SaveManagementPolicyCommand } from './managementPolicy.api'

export const ManagementPolicyPage = () => {
  const { status } = useRbac3Session()
  const { effectiveTenantId } = useFeatureTenantContext()
  const api = managementPolicyApi(useFeatureApi())
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const [selected, setSelected] = useState<ManagementPolicyView | null>(null)
  const queryKey = ['rbac3', 'management-policies', effectiveTenantId ?? 'none']
  const query = useQuery({ queryKey, queryFn: api.list, enabled: status === 'READY' })
  const save = useMutation({
    mutationFn: (command: SaveManagementPolicyCommand) => selected
      ? api.update(selected, command, crypto.randomUUID())
      : api.create(command, crypto.randomUUID()),
    onSuccess: async () => { setOpen(false); setSelected(null); await queryClient.invalidateQueries({ queryKey }) },
  })
  const disable = useMutation({
    mutationFn: (policy: ManagementPolicyView) => api.disable(policy, crypto.randomUUID()),
    onSuccess: async () => queryClient.invalidateQueries({ queryKey }),
  })
  const edit = (policy: ManagementPolicyView | null) => { setSelected(policy); setOpen(true) }
  return (
    <Card
      title="委托管理策略"
      extra={<PermissionGuard permission="system:management-policy:manage"><Button type="primary" onClick={() => edit(null)}>新增完整策略</Button></PermissionGuard>}
    >
      <Typography.Paragraph type="secondary">
        每次授权必须由同一条策略同时满足 Subject、Scope、激活根角色与 Operation；平台不会拼接多条策略的局部授权。
      </Typography.Paragraph>
      <PageState loading={query.isPending} error={query.error ?? save.error ?? disable.error} empty={query.data?.length === 0}>
        <Table<ManagementPolicyView>
          rowKey="policyId"
          dataSource={query.data ?? []}
          pagination={false}
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
                <PermissionGuard permission="system:management-policy:manage">
                  <Space>
                    <Button size="small" onClick={() => edit(row)}>编辑</Button>
                    {row.status === 'ACTIVE' && <Popconfirm title="确认禁用并保留历史？" onConfirm={() => disable.mutate(row)}><Button danger size="small">禁用</Button></Popconfirm>}
                  </Space>
                </PermissionGuard>
              ),
            },
          ]}
        />
      </PageState>
      <ManagementPolicyEditor open={open} policy={selected} saving={save.isPending} onCancel={() => { setOpen(false); setSelected(null) }} onSave={save.mutate} />
    </Card>
  )
}
