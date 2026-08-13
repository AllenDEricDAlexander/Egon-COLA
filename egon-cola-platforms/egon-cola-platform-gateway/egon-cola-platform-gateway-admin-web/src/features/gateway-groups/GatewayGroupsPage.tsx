import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button, Form, Input, Modal, Popconfirm, Select, Space, Table, Typography, message } from 'antd'
import { useState } from 'react'
import { useMemo } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { gatewayApi } from '../../api/gatewayApi'
import type { GatewayGroup } from '../../api/types'
import { useCapability } from '../../app/capabilities'
import { LoadingBlock, QueryFailure } from '../../components/QueryState'
import { StatusTag } from '../../components/StatusTag'
import { GatewayScopeFilter } from '../../components/GatewayScopeFilter'
import { useGatewayScopeBindings } from '../../hooks/useGatewayScopeBindings'
import { readScopeSearchParams, writeScopeSearchParams } from '../../hooks/scopeSearchParams'

export const GatewayGroupsPage = () => {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const queryClient = useQueryClient()
  const bindings = useGatewayScopeBindings()
  const canWrite = useCapability('gateway:groups:write')
  const [form] = Form.useForm()
  const [editing, setEditing] = useState<GatewayGroup>()
  const filters = readScopeSearchParams(searchParams, ['env', 'namespace'])
  const query = useQuery({
    queryKey: ['gateway-groups'],
    queryFn: ({ signal }) => gatewayApi.groups(signal),
  })
  const groups = useMemo(() => (query.data ?? []).filter((group) =>
    (!filters.env || group.env === filters.env)
      && (!filters.namespace || group.namespace === filters.namespace)), [filters.env, filters.namespace, query.data])
  const save = useMutation({
    mutationFn: (values: any) => editing?.id
      ? gatewayApi.updateGroup(editing.id, {
          displayName: values.displayName,
          description: values.description,
          expectedRevision: editing.revision,
        })
      : (() => {
          const binding = bindings.data?.find((item) => item.bindingId === values.bindingId)
          return gatewayApi.createGroup({
            gatewayGroupCode: values.gatewayGroupCode,
            displayName: values.displayName,
            description: values.description,
            env: binding?.env ?? values.env,
            namespace: binding?.namespace ?? values.namespace,
          })
        })(),
    onSuccess: async () => {
      setEditing(undefined)
      form.resetFields()
      await queryClient.invalidateQueries({ queryKey: ['gateway-groups'] })
      void message.success('Gateway Group 已保存')
    },
  })
  const toggle = useMutation({
    mutationFn: (group: GatewayGroup) =>
      gatewayApi.setGroupEnabled(group.id, !group.enabled),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['gateway-groups'] })
      void message.success('Gateway Group 状态已更新')
    },
  })
  if (query.isLoading) return <LoadingBlock />
  if (query.error) return <QueryFailure error={query.error} retry={() => void query.refetch()} />
  return (
    <section>
      <Space className="page-title">
        <Typography.Title level={2}>Gateway Group</Typography.Title>
        <Button
          type="primary"
          disabled={!canWrite}
          onClick={() => {
            setEditing({} as GatewayGroup)
            form.resetFields()
          }}
        >
          新建 Gateway Group
        </Button>
      </Space>
      <GatewayScopeFilter
        fields={['env', 'namespace']}
        value={filters}
        onChange={(value) => setSearchParams(writeScopeSearchParams(searchParams, value, ['env', 'namespace']))}
      />
      <Table
        rowKey="id"
        dataSource={groups}
        scroll={{ x: 900 }}
        columns={[
          { title: 'Code', dataIndex: 'gatewayGroupCode' },
          { title: '名称', dataIndex: 'displayName' },
          { title: 'Env', dataIndex: 'env' },
          { title: 'Namespace', dataIndex: 'namespace' },
          {
            title: '状态',
            render: (_, record) => <StatusTag status={record.enabled ? 'ACTIVE' : 'DISABLED'} />,
          },
          { title: 'Revision', dataIndex: 'revision' },
          {
            title: '操作',
            render: (_, record) => (
              <Space>
                <Button type="link" onClick={() => navigate(`/gateway-groups/${record.id}/overview`)}>
                  查看
                </Button>
                <Button
                  disabled={!canWrite}
                  onClick={() => {
                    setEditing(record)
                    form.setFieldsValue(record)
                  }}
                >
                  编辑
                </Button>
                <Popconfirm
                  title={record.enabled ? '确认停用 Gateway Group？' : '确认启用 Gateway Group？'}
                  description={record.enabled ? '停用会阻止后续路由发布，请确认影响范围。' : undefined}
                  onConfirm={() => toggle.mutate(record)}
                >
                  <Button danger={record.enabled} disabled={!canWrite}>
                    {record.enabled ? '停用' : '启用'}
                  </Button>
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />
      <Modal
        title={editing?.id ? '编辑 Gateway Group' : '新建 Gateway Group'}
        open={Boolean(editing)}
        onCancel={() => setEditing(undefined)}
        onOk={() => form.submit()}
        confirmLoading={save.isPending}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)}>
          {!editing?.id && (
            <Form.Item name="gatewayGroupCode" label="Group Code" rules={[{ required: true }]}>
              <Input />
            </Form.Item>
          )}
          {!editing?.id && (
            <Form.Item name="bindingId" label="Scope Binding" rules={[{ required: true }]}>
              <Select
                placeholder="选择 Gateway Group Scope"
                options={(bindings.data ?? []).map((binding) => ({
                  value: binding.bindingId,
                  label: `${binding.env} / ${binding.namespace} (${binding.appCode})`,
                }))}
              />
            </Form.Item>
          )}
          <Form.Item name="displayName" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述"><Input.TextArea /></Form.Item>
        </Form>
      </Modal>
    </section>
  )
}
