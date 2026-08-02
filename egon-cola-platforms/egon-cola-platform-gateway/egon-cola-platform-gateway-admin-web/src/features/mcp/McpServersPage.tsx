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
  Tag,
  Typography,
  message,
} from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { gatewayApi } from '../../api/gatewayApi'
import type { McpProtocolDialect, McpServer, McpServerMutation } from '../../api/types'
import { useCapability } from '../../app/capabilities'
import { LoadingBlock, QueryFailure } from '../../components/QueryState'
import { useScope } from '../../hooks/useScope'

type ServerForm = Omit<
  McpServerMutation,
  'gatewayGroupId' | 'expectedRevision' | 'expectedDraftRevision'
>

const dialectOptions: Array<{ label: string; value: McpProtocolDialect }> = [
  { value: 'STABLE_2025_11_25', label: 'Stable 2025-11-25' },
  { value: 'RC_2026_07_28', label: 'RC 2026-07-28' },
  { value: 'LEGACY_2024_SSE', label: 'Legacy 2024 SSE' },
]

export const McpServersPage = () => {
  const { scope } = useScope()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const canWrite = useCapability('gateway:mcp:write')
  const [selectedGroupId, setSelectedGroupId] = useState('')
  const [editing, setEditing] = useState<McpServer>()
  const [modalOpen, setModalOpen] = useState(false)
  const [form] = Form.useForm<ServerForm>()
  const groups = useQuery({
    queryKey: ['gateway-groups', scope],
    queryFn: ({ signal }) => gatewayApi.groups(scope, signal),
  })
  const gatewayGroupId = selectedGroupId || groups.data?.[0]?.id || ''
  const draft = useQuery({
    queryKey: ['gateway-draft', gatewayGroupId],
    queryFn: ({ signal }) => gatewayApi.draft(gatewayGroupId, signal),
    enabled: Boolean(gatewayGroupId),
  })
  const servers = useQuery({
    queryKey: ['mcp-servers', gatewayGroupId],
    queryFn: ({ signal }) => gatewayApi.mcpServers(gatewayGroupId, signal),
    enabled: Boolean(gatewayGroupId),
  })
  const save = useMutation({
    mutationFn: (values: ServerForm) => {
      const request: McpServerMutation = {
        ...values,
        gatewayGroupId,
        expectedRevision: editing?.revision ?? 0,
        expectedDraftRevision: draft.data?.revision ?? 0,
      }
      return editing
        ? gatewayApi.updateMcpServer(editing.id, request)
        : gatewayApi.createMcpServer(request)
    },
    onSuccess: async (result) => {
      setModalOpen(false)
      setEditing(undefined)
      form.resetFields()
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['mcp-servers', gatewayGroupId] }),
        queryClient.invalidateQueries({ queryKey: ['gateway-draft', gatewayGroupId] }),
      ])
      void message.success('MCP Server 已保存')
      if (!editing) navigate(`/mcp/servers/${result.resourceId}`)
    },
  })
  const remove = useMutation({
    mutationFn: (server: McpServer) => gatewayApi.deleteMcpServer(server.id, {
      gatewayGroupId,
      expectedRevision: server.revision,
      expectedDraftRevision: draft.data?.revision ?? 0,
      changeReason: 'Delete MCP Server from Admin Web',
    }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['mcp-servers', gatewayGroupId] }),
        queryClient.invalidateQueries({ queryKey: ['gateway-draft', gatewayGroupId] }),
      ])
      void message.success('MCP Server 已删除')
    },
  })

  const openEditor = (server?: McpServer) => {
    save.reset()
    setEditing(server)
    form.setFieldsValue(server ? {
      serverCode: server.serverCode,
      displayName: server.displayName,
      description: server.description,
      instructions: server.instructions,
      dialects: server.dialects,
      oauthAudience: server.oauthAudience,
      listCacheTtlSeconds: server.listCacheTtlSeconds,
      enabled: server.enabled,
      changeReason: '',
    } : {
      serverCode: '',
      displayName: '',
      dialects: ['STABLE_2025_11_25'],
      oauthAudience: '',
      listCacheTtlSeconds: 30,
      enabled: true,
      changeReason: '',
    })
    setModalOpen(true)
  }

  if (groups.isLoading) return <LoadingBlock />
  if (groups.error) return <QueryFailure error={groups.error} retry={() => void groups.refetch()} />

  return (
    <section>
      <Space className="page-title" align="center" wrap>
        <Typography.Title level={2}>MCP Servers</Typography.Title>
        <Select
          aria-label="Gateway Group"
          value={gatewayGroupId || undefined}
          placeholder="选择 Gateway Group"
          style={{ minWidth: 240 }}
          options={(groups.data ?? []).map((group) => ({
            value: group.id,
            label: `${group.displayName} (${group.gatewayGroupCode})`,
          }))}
          onChange={setSelectedGroupId}
        />
        <Button
          type="primary"
          disabled={!canWrite || !gatewayGroupId || !draft.data}
          onClick={() => openEditor()}
        >
          新增 Server
        </Button>
      </Space>
      {!gatewayGroupId ? (
        <Typography.Text type="secondary">当前作用域尚未配置 Gateway Group。</Typography.Text>
      ) : servers.error ? (
        <QueryFailure error={servers.error} retry={() => void servers.refetch()} />
      ) : (
        <Table<McpServer>
          rowKey="id"
          loading={servers.isLoading}
          dataSource={servers.data ?? []}
          columns={[
            { title: 'Server Code', dataIndex: 'serverCode' },
            { title: '名称', dataIndex: 'displayName' },
            { title: 'OAuth Audience', dataIndex: 'oauthAudience' },
            {
              title: '协议',
              render: (_, server) => (
                <Space wrap>{server.dialects.map((dialect) => <Tag key={dialect}>{dialect}</Tag>)}</Space>
              ),
            },
            { title: 'Revision', dataIndex: 'revision' },
            {
              title: '状态',
              render: (_, server) => <Tag color={server.enabled ? 'green' : 'default'}>
                {server.enabled ? 'ENABLED' : 'DISABLED'}
              </Tag>,
            },
            {
              title: '操作',
              render: (_, server) => (
                <Space>
                  <Button type="link" onClick={() => navigate(`/mcp/servers/${server.id}`)}>
                    工作台
                  </Button>
                  <Button disabled={!canWrite} onClick={() => openEditor(server)}>编辑</Button>
                  <Popconfirm
                    title="确认删除 MCP Server？"
                    description="其下能力草稿也将一并删除。"
                    onConfirm={() => remove.mutate(server)}
                  >
                    <Button danger disabled={!canWrite}>删除</Button>
                  </Popconfirm>
                </Space>
              ),
            },
          ]}
        />
      )}
      <Modal
        title={editing ? '编辑 MCP Server' : '新增 MCP Server'}
        open={modalOpen}
        width={720}
        onCancel={() => setModalOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={save.isPending}
        destroyOnHidden
      >
        {save.error && <QueryFailure error={save.error} />}
        <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)}>
          <Form.Item name="serverCode" label="Server Code" rules={[{ required: true }]}>
            <Input disabled={Boolean(editing)} />
          </Form.Item>
          <Form.Item name="displayName" label="显示名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述"><Input.TextArea /></Form.Item>
          <Form.Item name="instructions" label="Instructions"><Input.TextArea rows={3} /></Form.Item>
          <Form.Item name="dialects" label="协议版本" rules={[{ required: true }]}>
            <Select mode="multiple" options={dialectOptions} />
          </Form.Item>
          <Form.Item name="oauthAudience" label="OAuth Audience" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="listCacheTtlSeconds" label="List Cache TTL（秒）" rules={[{ required: true }]}>
            <InputNumber min={0} />
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
