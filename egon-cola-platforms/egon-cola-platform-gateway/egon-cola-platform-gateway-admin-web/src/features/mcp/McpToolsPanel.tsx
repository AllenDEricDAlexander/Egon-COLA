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
  message,
} from 'antd'
import { useState } from 'react'
import { gatewayApi } from '../../api/gatewayApi'
import type { McpCapabilityDraft, McpCapabilityMutation } from '../../api/types'
import { useCapability } from '../../app/capabilities'
import { QueryFailure } from '../../components/QueryState'
import { useScope } from '../../hooks/useScope'
import { formatJson, parseStringList, validateJsonSchema } from './mcpValidation'

type ToolForm = {
  name: string
  description?: string
  sourceType: 'LOCAL_OPERATION' | 'REMOTE_MCP'
  operationId?: string
  remoteMountId?: string
  inputSchema: string
  outputSchema: string
  requiredPermissions?: string
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH'
  idempotent: boolean
  enabled: boolean
  changeReason: string
}

export const McpToolsPanel = ({ serverId, gatewayGroupId, draftRevision }: {
  serverId: string
  gatewayGroupId: string
  draftRevision: number
}) => {
  const { scope } = useScope()
  const canWrite = useCapability('gateway:mcp:write')
  const queryClient = useQueryClient()
  const [form] = Form.useForm<ToolForm>()
  const sourceType = Form.useWatch('sourceType', form)
  const [editing, setEditing] = useState<McpCapabilityDraft>()
  const [modalOpen, setModalOpen] = useState(false)
  const tools = useQuery({
    queryKey: ['mcp-capabilities', gatewayGroupId, serverId, 'tools'],
    queryFn: ({ signal }) => gatewayApi.mcpCapabilities(
      gatewayGroupId,
      serverId,
      'tools',
      signal,
    ),
  })
  const operations = useQuery({
    queryKey: ['mcp-operation-options', scope],
    queryFn: ({ signal }) => gatewayApi.mcpOperationOptions(scope, signal),
  })
  const save = useMutation({
    mutationFn: (values: ToolForm) => {
      const capability: McpCapabilityMutation = {
        gatewayGroupId,
        serverId,
        name: values.name,
        enabled: values.enabled,
        expectedRevision: editing?.revision ?? 0,
        expectedDraftRevision: draftRevision,
        changeReason: values.changeReason,
        content: {
          description: values.description,
          sourceType: values.sourceType,
          operationId: values.sourceType === 'LOCAL_OPERATION' ? values.operationId : undefined,
          remoteMountId: values.sourceType === 'REMOTE_MCP' ? values.remoteMountId : undefined,
          inputSchema: validateJsonSchema(values.inputSchema, 'Input Schema'),
          outputSchema: validateJsonSchema(values.outputSchema, 'Output Schema'),
          argumentBindings: {},
          resultBindings: {},
          annotations: {},
          requiredPermissions: parseStringList(values.requiredPermissions),
          riskLevel: values.riskLevel,
          idempotent: values.idempotent,
        },
      }
      return editing
        ? gatewayApi.updateMcpCapability('tools', editing.id, capability)
        : gatewayApi.createMcpCapability('tools', capability)
    },
    onSuccess: async () => {
      setModalOpen(false)
      setEditing(undefined)
      form.resetFields()
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: ['mcp-capabilities', gatewayGroupId, serverId, 'tools'],
        }),
        queryClient.invalidateQueries({ queryKey: ['gateway-draft', gatewayGroupId] }),
      ])
      void message.success('MCP Tool 已保存')
    },
  })
  const remove = useMutation({
    mutationFn: (tool: McpCapabilityDraft) => gatewayApi.deleteMcpCapability(
      'tools',
      tool.id,
      {
        gatewayGroupId,
        expectedRevision: tool.revision,
        expectedDraftRevision: draftRevision,
        changeReason: 'Delete MCP Tool from Admin Web',
      },
    ),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: ['mcp-capabilities', gatewayGroupId, serverId, 'tools'],
        }),
        queryClient.invalidateQueries({ queryKey: ['gateway-draft', gatewayGroupId] }),
      ])
      void message.success('MCP Tool 已删除')
    },
  })

  const openEditor = (tool?: McpCapabilityDraft) => {
    const content = tool?.content ?? {}
    setEditing(tool)
    save.reset()
    form.setFieldsValue(tool ? {
      name: tool.name,
      description: String(content.description ?? ''),
      sourceType: content.sourceType === 'REMOTE_MCP' ? 'REMOTE_MCP' : 'LOCAL_OPERATION',
      operationId: typeof content.operationId === 'string' ? content.operationId : undefined,
      remoteMountId: typeof content.remoteMountId === 'string' ? content.remoteMountId : undefined,
      inputSchema: formatJson(content.inputSchema),
      outputSchema: formatJson(content.outputSchema),
      requiredPermissions: Array.isArray(content.requiredPermissions)
        ? content.requiredPermissions.join(', ')
        : '',
      riskLevel: content.riskLevel === 'HIGH' || content.riskLevel === 'MEDIUM'
        ? content.riskLevel
        : 'LOW',
      idempotent: content.idempotent === true,
      enabled: tool.enabled,
      changeReason: '',
    } : {
      name: '',
      sourceType: 'LOCAL_OPERATION',
      inputSchema: formatJson({ type: 'object', additionalProperties: false }),
      outputSchema: formatJson({ type: 'object' }),
      riskLevel: 'LOW',
      idempotent: false,
      enabled: true,
      changeReason: '',
    })
    setModalOpen(true)
  }

  return (
    <section>
      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" disabled={!canWrite} onClick={() => openEditor()}>
          新增 Tool
        </Button>
      </Space>
      {tools.error && <QueryFailure error={tools.error} retry={() => void tools.refetch()} />}
      <Table<McpCapabilityDraft>
        rowKey="id"
        loading={tools.isLoading}
        dataSource={tools.data ?? []}
        columns={[
          { title: 'Tool Name', dataIndex: 'name' },
          {
            title: 'Source',
            render: (_, tool) => <Tag>{String(tool.content.sourceType ?? '-')}</Tag>,
          },
          {
            title: 'Operation / Mount',
            render: (_, tool) => String(tool.content.operationId ?? tool.content.remoteMountId ?? '-'),
          },
          { title: 'Revision', dataIndex: 'revision' },
          {
            title: '操作',
            render: (_, tool) => (
              <Space>
                <Button disabled={!canWrite} onClick={() => openEditor(tool)}>编辑</Button>
                <Popconfirm title="确认删除 Tool？" onConfirm={() => remove.mutate(tool)}>
                  <Button danger disabled={!canWrite}>删除</Button>
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />
      <Modal
        title={editing ? '编辑 Tool' : '新增 Tool'}
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
          <Form.Item name="sourceType" label="来源" rules={[{ required: true }]}>
            <Select options={[
              { value: 'LOCAL_OPERATION', label: 'Local Operation' },
              { value: 'REMOTE_MCP', label: 'Remote MCP Mount' },
            ]} />
          </Form.Item>
          {sourceType === 'REMOTE_MCP' ? (
            <Form.Item name="remoteMountId" label="Remote Mount" rules={[{ required: true }]}>
              <Input />
            </Form.Item>
          ) : (
            <Form.Item name="operationId" label="Operation" rules={[{ required: true }]}>
              <Select
                showSearch
                loading={operations.isLoading}
                options={operations.data ?? []}
                optionFilterProp="label"
              />
            </Form.Item>
          )}
          <Form.Item name="inputSchema" label="Input Schema" rules={[{ required: true }]}>
            <Input.TextArea rows={6} />
          </Form.Item>
          <Form.Item name="outputSchema" label="Output Schema" rules={[{ required: true }]}>
            <Input.TextArea rows={6} />
          </Form.Item>
          <Form.Item name="requiredPermissions" label="RBAC3 Permissions（逗号分隔）">
            <Input />
          </Form.Item>
          <Form.Item name="riskLevel" label="风险级别" rules={[{ required: true }]}>
            <Select options={['LOW', 'MEDIUM', 'HIGH'].map((value) => ({ value, label: value }))} />
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
