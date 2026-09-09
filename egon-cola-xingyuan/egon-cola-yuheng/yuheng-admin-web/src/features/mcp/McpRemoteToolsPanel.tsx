import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Button,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import { useState } from 'react'
import { gatewayApi } from '../../api/gatewayApi'
import type { McpRemoteTool, McpToolRiskLevel } from '../../api/types'
import { useCapability } from '../../app/capabilities'
import { QueryFailure } from '../../components/QueryState'
import { formatJson, parseJsonObject, parseStringList, validateJsonSchema } from './mcpValidation'

type RemoteToolForm = {
  name: string
  description?: string
  remoteMountId: string
  inputSchema: string
  outputSchema: string
  annotations: string
  requiredPermissions?: string
  riskLevel: McpToolRiskLevel
  idempotent: boolean
  enabled: boolean
  changeReason: string
}

const riskLevels: McpToolRiskLevel[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']

export const McpRemoteToolsPanel = ({ serverId, gatewayGroupId, draftRevision }: {
  serverId: string
  gatewayGroupId: string
  draftRevision: number
}) => {
  const canWrite = useCapability('gateway:mcp:write')
  const queryClient = useQueryClient()
  const [messageApi, messageContext] = message.useMessage()
  const [form] = Form.useForm<RemoteToolForm>()
  const [editing, setEditing] = useState<McpRemoteTool>()
  const [modalOpen, setModalOpen] = useState(false)
  const tools = useQuery({
    queryKey: ['mcp-remote-tools', gatewayGroupId, serverId],
    queryFn: ({ signal }) => gatewayApi.mcpRemoteTools(gatewayGroupId, serverId, signal),
  })
  const mounts = useQuery({
    queryKey: ['mcp-remote-mounts', gatewayGroupId],
    queryFn: ({ signal }) => gatewayApi.mcpRemoteMounts(gatewayGroupId, signal),
  })

  const invalidateTools = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['mcp-remote-tools', gatewayGroupId] }),
      queryClient.invalidateQueries({ queryKey: ['mcp-tool-references', gatewayGroupId] }),
      queryClient.invalidateQueries({ queryKey: ['gateway-draft', gatewayGroupId] }),
      queryClient.invalidateQueries({ queryKey: ['mcp-capability-preview', serverId] }),
    ])
  }
  const save = useMutation({
    mutationFn: (values: RemoteToolForm) => {
      const tool = {
        gatewayGroupId,
        serverId,
        name: values.name.trim(),
        description: values.description?.trim() || undefined,
        remoteMountId: values.remoteMountId,
        inputSchema: validateJsonSchema(values.inputSchema, 'Input Schema'),
        outputSchema: validateJsonSchema(values.outputSchema, 'Output Schema'),
        annotations: parseJsonObject(values.annotations, 'Annotations'),
        requiredPermissions: parseStringList(values.requiredPermissions),
        riskLevel: values.riskLevel,
        idempotent: values.idempotent,
        enabled: values.enabled,
        expectedRevision: editing?.revision ?? 0,
        expectedDraftRevision: draftRevision,
        changeReason: values.changeReason,
      }
      return editing
        ? gatewayApi.updateMcpRemoteTool(editing.id, tool)
        : gatewayApi.createMcpRemoteTool(tool)
    },
    onSuccess: async () => {
      setModalOpen(false)
      setEditing(undefined)
      form.resetFields()
      await invalidateTools()
      void messageApi.success('Remote MCP Tool 已保存')
    },
  })
  const remove = useMutation({
    mutationFn: (tool: McpRemoteTool) => gatewayApi.deleteMcpRemoteTool(tool.id, {
      gatewayGroupId,
      expectedRevision: tool.revision,
      expectedDraftRevision: draftRevision,
      changeReason: 'Delete Remote MCP Tool from Admin Web',
    }),
    onSuccess: async () => {
      await invalidateTools()
      void messageApi.success('Remote MCP Tool 已删除')
    },
  })

  const openEditor = (tool?: McpRemoteTool) => {
    save.reset()
    setEditing(tool)
    form.setFieldsValue(tool ? {
      name: tool.name,
      description: tool.description,
      remoteMountId: tool.remoteMountId,
      inputSchema: formatJson(tool.inputSchema),
      outputSchema: formatJson(tool.outputSchema),
      annotations: formatJson(tool.annotations),
      requiredPermissions: tool.requiredPermissions.join(', '),
      riskLevel: tool.riskLevel,
      idempotent: tool.idempotent,
      enabled: tool.enabled,
      changeReason: '',
    } : {
      name: '',
      description: '',
      remoteMountId: undefined,
      inputSchema: formatJson({ type: 'object', additionalProperties: false }),
      outputSchema: formatJson({ type: 'object' }),
      annotations: formatJson({}),
      requiredPermissions: '',
      riskLevel: 'LOW',
      idempotent: false,
      enabled: true,
      changeReason: '',
    })
    setModalOpen(true)
  }

  const availableMounts = (mounts.data ?? []).filter(
    (mount) => mount.serverId === serverId && (mount.enabled || mount.id === editing?.remoteMountId),
  )

  return (
    <section>
      {messageContext}
      <Button
        type="primary"
        disabled={!canWrite || availableMounts.length === 0}
        onClick={() => openEditor()}
        style={{ marginBottom: 16 }}
      >
        新增 Remote Tool
      </Button>
      {availableMounts.length === 0 && !mounts.isLoading && (
        <Typography.Text type="secondary" style={{ marginLeft: 12 }}>
          当前 MCP Server 没有可用的 Remote Mount
        </Typography.Text>
      )}
      {tools.error && <QueryFailure error={tools.error} retry={() => void tools.refetch()} />}
      {remove.error && <QueryFailure error={remove.error} />}
      <Table<McpRemoteTool>
        rowKey="id"
        loading={tools.isLoading}
        dataSource={tools.data ?? []}
        scroll={{ x: 980 }}
        columns={[
          {
            title: 'Tool',
            width: 220,
            fixed: 'start',
            render: (_, tool) => (
              <Space orientation="vertical" size={0}>
                <Typography.Text strong>{tool.name}</Typography.Text>
                <Typography.Text type="secondary">{tool.description || '无描述'}</Typography.Text>
              </Space>
            ),
          },
          { title: 'Remote Mount', dataIndex: 'remoteMountId', width: 180 },
          {
            title: 'Permissions',
            width: 260,
            render: (_, tool) => tool.requiredPermissions.length > 0
              ? <Space wrap size={[0, 4]}>{tool.requiredPermissions.map((item) => <Tag key={item}>{item}</Tag>)}</Space>
              : <Typography.Text type="secondary">无</Typography.Text>,
          },
          { title: 'Risk', dataIndex: 'riskLevel', width: 100 },
          {
            title: 'Runtime',
            width: 150,
            render: (_, tool) => (
              <Space orientation="vertical" size={2}>
                <Tag color={tool.enabled ? 'green' : 'default'}>
                  {tool.enabled ? 'ENABLED' : 'DISABLED'}
                </Tag>
                <Typography.Text type="secondary">
                  {tool.idempotent ? 'Idempotent' : 'Non-idempotent'}
                </Typography.Text>
              </Space>
            ),
          },
          { title: 'Revision', dataIndex: 'revision', width: 90 },
          {
            title: '操作',
            width: 150,
            fixed: 'end',
            render: (_, tool) => (
              <Space>
                <Button disabled={!canWrite} onClick={() => openEditor(tool)}>编辑</Button>
                <Popconfirm title="确认删除 Remote Tool？" onConfirm={() => remove.mutate(tool)}>
                  <Button danger disabled={!canWrite}>删除</Button>
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />
      <Modal
        title={editing ? '编辑 Remote Tool' : '新增 Remote Tool'}
        open={modalOpen}
        width={760}
        onCancel={() => setModalOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={save.isPending}
        destroyOnHidden
      >
        {save.error && <QueryFailure error={save.error} />}
        <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)}>
          <Form.Item name="name" label="Tool Name" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="description" label="描述"><Input.TextArea /></Form.Item>
          <Form.Item name="remoteMountId" label="Remote Mount" rules={[{ required: true }]}>
            <Select options={availableMounts.map((mount) => ({
              value: mount.id,
              label: `${mount.namespace} · ${mount.capabilityFingerprint}`,
            }))} />
          </Form.Item>
          <Form.Item name="inputSchema" label="Input Schema" rules={[{ required: true }]}>
            <Input.TextArea rows={6} />
          </Form.Item>
          <Form.Item name="outputSchema" label="Output Schema" rules={[{ required: true }]}>
            <Input.TextArea rows={6} />
          </Form.Item>
          <Form.Item name="annotations" label="Remote Annotations JSON" rules={[{ required: true }]}>
            <Input.TextArea rows={4} />
          </Form.Item>
          <Form.Item name="requiredPermissions" label="RBAC3 Permissions（逗号分隔）">
            <Input />
          </Form.Item>
          <Form.Item name="riskLevel" label="风险级别" rules={[{ required: true }]}>
            <Select options={riskLevels.map((value) => ({ value, label: value }))} />
          </Form.Item>
          <Form.Item name="idempotent" label="幂等" valuePropName="checked"><Switch /></Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked"><Switch /></Form.Item>
          <Form.Item name="changeReason" label="变更原因" rules={[{ required: true }]}>
            <Input.TextArea />
          </Form.Item>
        </Form>
      </Modal>
    </section>
  )
}
