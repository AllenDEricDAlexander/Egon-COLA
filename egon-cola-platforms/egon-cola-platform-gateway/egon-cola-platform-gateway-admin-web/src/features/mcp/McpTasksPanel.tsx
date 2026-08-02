import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Switch,
  Table,
  Tabs,
  Tag,
  Typography,
} from 'antd'
import { useState } from 'react'
import { gatewayApi } from '../../api/gatewayApi'
import type { McpCapabilityDraft, McpTask } from '../../api/types'
import { useCapability } from '../../app/capabilities'
import { QueryFailure } from '../../components/QueryState'
import { useMcpCapabilityCollection } from './useMcpCapabilityCollection'

type TaskPolicyForm = {
  toolName: string
  durable: boolean
  inputAllowed: boolean
  executionTimeoutSeconds: number
  resultTtlSeconds: number
  maxAttempts: number
  enabled: boolean
  changeReason: string
}

export const McpTasksPanel = ({ serverId, gatewayGroupId, draftRevision }: {
  serverId: string
  gatewayGroupId: string
  draftRevision: number
}) => {
  const canWrite = useCapability('gateway:mcp:write')
  const canReadRuntime = useCapability('gateway:mcp:runtime:read')
  const queryClient = useQueryClient()
  const collection = useMcpCapabilityCollection(
    'task-policies',
    serverId,
    gatewayGroupId,
    draftRevision,
  )
  const tools = useQuery({
    queryKey: ['mcp-capabilities', gatewayGroupId, serverId, 'tools'],
    queryFn: ({ signal }) => gatewayApi.mcpCapabilities(
      gatewayGroupId,
      serverId,
      'tools',
      signal,
    ),
  })
  const [tenantId, setTenantId] = useState('')
  const [clientId, setClientId] = useState('')
  const [runtimeFilter, setRuntimeFilter] = useState<{ tenantId: string; clientId?: string }>()
  const tasks = useQuery({
    queryKey: ['mcp-runtime-tasks', runtimeFilter],
    queryFn: ({ signal }) => gatewayApi.mcpTasks(
      runtimeFilter!.tenantId,
      runtimeFilter!.clientId,
      signal,
    ),
    enabled: canReadRuntime && Boolean(runtimeFilter?.tenantId),
    refetchInterval: 5_000,
  })
  const cancel = useMutation({
    mutationFn: (task: McpTask) => gatewayApi.cancelMcpTask(task.id, task.revision),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['mcp-runtime-tasks', runtimeFilter] })
    },
  })
  const [form] = Form.useForm<TaskPolicyForm>()
  const [editing, setEditing] = useState<McpCapabilityDraft>()
  const [open, setOpen] = useState(false)

  const openEditor = (policy?: McpCapabilityDraft) => {
    const content = policy?.content ?? {}
    setEditing(policy)
    form.setFieldsValue(policy ? {
      toolName: policy.name,
      durable: content.durable !== false,
      inputAllowed: content.inputAllowed === true,
      executionTimeoutSeconds: typeof content.executionTimeoutSeconds === 'number'
        ? content.executionTimeoutSeconds
        : 60,
      resultTtlSeconds: typeof content.resultTtlSeconds === 'number'
        ? content.resultTtlSeconds
        : 86_400,
      maxAttempts: typeof content.maxAttempts === 'number' ? content.maxAttempts : 3,
      enabled: policy.enabled,
      changeReason: '',
    } : {
      toolName: '',
      durable: true,
      inputAllowed: false,
      executionTimeoutSeconds: 60,
      resultTtlSeconds: 86_400,
      maxAttempts: 3,
      enabled: true,
      changeReason: '',
    })
    setOpen(true)
  }

  const submit = (values: TaskPolicyForm) => collection.save.mutate({
    editing,
    name: values.toolName,
    enabled: values.enabled,
    changeReason: values.changeReason,
    content: {
      durable: values.durable,
      inputAllowed: values.inputAllowed,
      executionTimeoutSeconds: values.executionTimeoutSeconds,
      resultTtlSeconds: values.resultTtlSeconds,
      maxAttempts: values.maxAttempts,
    },
  }, {
    onSuccess: () => {
      setOpen(false)
      setEditing(undefined)
    },
  })

  return (
    <section>
      <Tabs items={[
        {
          key: 'policies',
          label: 'Task Policies',
          children: (
            <section>
              <Button
                type="primary"
                disabled={!canWrite}
                onClick={() => openEditor()}
                style={{ marginBottom: 16 }}
              >
                新增 Task Policy
              </Button>
              {collection.query.error && <QueryFailure error={collection.query.error} />}
              <Table<McpCapabilityDraft>
                rowKey="id"
                loading={collection.query.isLoading}
                dataSource={collection.query.data ?? []}
                columns={[
                  { title: 'Tool', dataIndex: 'name' },
                  {
                    title: 'Durable',
                    render: (_, row) => String(row.content.durable !== false),
                  },
                  { title: 'Max Attempts', render: (_, row) => String(row.content.maxAttempts ?? 3) },
                  { title: 'Revision', dataIndex: 'revision' },
                  {
                    title: '操作',
                    render: (_, row) => <Space>
                      <Button disabled={!canWrite} onClick={() => openEditor(row)}>编辑</Button>
                      <Popconfirm title="确认删除 Task Policy？" onConfirm={() => collection.remove.mutate(row)}>
                        <Button danger disabled={!canWrite}>删除</Button>
                      </Popconfirm>
                    </Space>,
                  },
                ]}
              />
            </section>
          ),
        },
        {
          key: 'runtime',
          label: 'Runtime Tasks',
          children: (
            <section>
              {!canReadRuntime ? (
                <Typography.Text type="secondary">
                  当前账号缺少 gateway:mcp:runtime:read 能力。
                </Typography.Text>
              ) : (
                <>
                  <Space wrap style={{ marginBottom: 16 }}>
                    <Input
                      aria-label="Tenant ID"
                      placeholder="Tenant ID"
                      value={tenantId}
                      onChange={(event) => setTenantId(event.target.value)}
                    />
                    <Input
                      aria-label="Client ID"
                      placeholder="Client ID（可选）"
                      value={clientId}
                      onChange={(event) => setClientId(event.target.value)}
                    />
                    <Button
                      disabled={!tenantId.trim()}
                      onClick={() => setRuntimeFilter({
                        tenantId: tenantId.trim(),
                        clientId: clientId.trim() || undefined,
                      })}
                    >查询</Button>
                  </Space>
                  {tasks.error && <QueryFailure error={tasks.error} />}
                  <Table<McpTask>
                    rowKey="id"
                    loading={tasks.isLoading}
                    dataSource={tasks.data ?? []}
                    columns={[
                      { title: 'Task ID', dataIndex: 'id' },
                      { title: 'Tool', dataIndex: 'toolName' },
                      { title: 'Tenant', dataIndex: 'tenantId' },
                      { title: 'Client', dataIndex: 'clientId' },
                      {
                        title: '状态',
                        render: (_, task) => <Tag>{task.state}</Tag>,
                      },
                      {
                        title: 'Attempts',
                        render: (_, task) => `${task.attemptCount} / ${task.maxAttempts}`,
                      },
                      { title: 'Deadline', dataIndex: 'executionDeadline' },
                      {
                        title: '操作',
                        render: (_, task) => (
                          <Button
                            danger
                            disabled={!canWrite || !['WORKING', 'INPUT_REQUIRED'].includes(task.state)}
                            loading={cancel.isPending}
                            onClick={() => cancel.mutate(task)}
                          >取消</Button>
                        ),
                      },
                    ]}
                  />
                </>
              )}
            </section>
          ),
        },
      ]} />
      <Modal
        title={editing ? '编辑 Task Policy' : '新增 Task Policy'}
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={collection.save.isPending}
        destroyOnHidden
      >
        {collection.save.error && <QueryFailure error={collection.save.error} />}
        <Form form={form} layout="vertical" onFinish={submit}>
          <Form.Item name="toolName" label="Tool" rules={[{ required: true }]}>
            <Select
              disabled={Boolean(editing)}
              options={(tools.data ?? []).map((tool) => ({ value: tool.name, label: tool.name }))}
            />
          </Form.Item>
          <Form.Item name="durable" label="Durable" valuePropName="checked"><Switch /></Form.Item>
          <Form.Item name="inputAllowed" label="允许 Input Required" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="executionTimeoutSeconds" label="执行超时（秒）" rules={[{ required: true }]}>
            <InputNumber min={1} />
          </Form.Item>
          <Form.Item name="resultTtlSeconds" label="结果 TTL（秒）" rules={[{ required: true }]}>
            <InputNumber min={1} />
          </Form.Item>
          <Form.Item name="maxAttempts" label="最大尝试次数" rules={[{ required: true }]}>
            <InputNumber min={1} max={100} />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked"><Switch /></Form.Item>
          <Form.Item name="changeReason" label="变更原因" rules={[{ required: true }]}>
            <Input.TextArea />
          </Form.Item>
        </Form>
      </Modal>
    </section>
  )
}
