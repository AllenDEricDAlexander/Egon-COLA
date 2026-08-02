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
import type {
  McpProtocolDialect,
  McpRemoteCapability,
  McpRemoteProvider,
  McpRemoteProviderMutation,
} from '../../api/types'
import { useCapability } from '../../app/capabilities'
import { JsonPanel } from '../../components/JsonPanel'
import { LoadingBlock, QueryFailure } from '../../components/QueryState'
import { useScope } from '../../hooks/useScope'

type ProviderForm = {
  providerCode: string
  displayName: string
  dialect: McpProtocolDialect
  transportType: string
  endpointReference: string
  authProfileReference?: string
  tlsProfileReference?: string
  enabled: boolean
  changeReason: string
}

const value = (content: Record<string, unknown>, name: string): string =>
  typeof content[name] === 'string' ? content[name] as string : ''

export const McpRemoteProvidersPage = () => {
  const { scope } = useScope()
  const canWrite = useCapability('gateway:mcp:write')
  const canTest = useCapability('gateway:mcp:test')
  const queryClient = useQueryClient()
  const [selectedGroupId, setSelectedGroupId] = useState('')
  const [editing, setEditing] = useState<McpRemoteProvider>()
  const [open, setOpen] = useState(false)
  const [discovery, setDiscovery] = useState<{
    provider: McpRemoteProvider
    capabilities: McpRemoteCapability[]
  }>()
  const [form] = Form.useForm<ProviderForm>()
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
  const providers = useQuery({
    queryKey: ['mcp-remote-providers', gatewayGroupId],
    queryFn: ({ signal }) => gatewayApi.mcpRemoteProviders(gatewayGroupId, signal),
    enabled: Boolean(gatewayGroupId),
  })
  const save = useMutation({
    mutationFn: (values: ProviderForm) => {
      const content = editing?.content ?? {}
      const provider: McpRemoteProviderMutation = {
        gatewayGroupId,
        providerCode: values.providerCode,
        enabled: values.enabled,
        expectedRevision: editing?.revision ?? 0,
        expectedDraftRevision: draft.data?.revision ?? 0,
        changeReason: values.changeReason,
        content: {
          displayName: values.displayName,
          dialect: values.dialect,
          transportType: values.transportType,
          endpointReference: values.endpointReference,
          authProfileReference: values.authProfileReference,
          tlsProfileReference: values.tlsProfileReference,
          capabilityFingerprint: content.capabilityFingerprint,
          status: content.status ?? 'CONFIGURED',
        },
      }
      return editing
        ? gatewayApi.updateMcpRemoteProvider(editing.id, provider)
        : gatewayApi.createMcpRemoteProvider(provider)
    },
    onSuccess: async () => {
      setOpen(false)
      setEditing(undefined)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['mcp-remote-providers', gatewayGroupId] }),
        queryClient.invalidateQueries({ queryKey: ['gateway-draft', gatewayGroupId] }),
      ])
      void message.success('Remote MCP Provider 已保存')
    },
  })
  const remove = useMutation({
    mutationFn: (provider: McpRemoteProvider) => gatewayApi.deleteMcpRemoteProvider(provider.id, {
      gatewayGroupId,
      expectedRevision: provider.revision,
      expectedDraftRevision: draft.data?.revision ?? 0,
      changeReason: 'Delete Remote MCP Provider from Admin Web',
    }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['mcp-remote-providers', gatewayGroupId] }),
        queryClient.invalidateQueries({ queryKey: ['gateway-draft', gatewayGroupId] }),
      ])
    },
  })
  const discover = useMutation({
    mutationFn: (provider: McpRemoteProvider) => gatewayApi.discoverMcpRemoteProvider(provider.id)
      .then((capabilities) => ({ provider, capabilities })),
    onSuccess: setDiscovery,
  })

  const openEditor = (provider?: McpRemoteProvider) => {
    const content = provider?.content ?? {}
    setEditing(provider)
    form.setFieldsValue(provider ? {
      providerCode: provider.providerCode,
      displayName: value(content, 'displayName'),
      dialect: value(content, 'dialect') as McpProtocolDialect,
      transportType: value(content, 'transportType'),
      endpointReference: value(content, 'endpointReference'),
      authProfileReference: value(content, 'authProfileReference') || undefined,
      tlsProfileReference: value(content, 'tlsProfileReference') || undefined,
      enabled: provider.enabled,
      changeReason: '',
    } : {
      providerCode: '',
      displayName: '',
      dialect: 'STABLE_2025_11_25',
      transportType: 'STREAMABLE_HTTP',
      endpointReference: '',
      enabled: true,
      changeReason: '',
    })
    setOpen(true)
  }

  if (groups.isLoading) return <LoadingBlock />
  if (groups.error) return <QueryFailure error={groups.error} />
  return (
    <section>
      <Space className="page-title" align="center" wrap>
        <Typography.Title level={2}>Remote MCP Providers</Typography.Title>
        <Select
          aria-label="Gateway Group"
          value={gatewayGroupId || undefined}
          style={{ minWidth: 240 }}
          options={(groups.data ?? []).map((group) => ({
            value: group.id,
            label: `${group.displayName} (${group.gatewayGroupCode})`,
          }))}
          onChange={setSelectedGroupId}
        />
        <Button
          type="primary"
          disabled={!canWrite || !draft.data}
          onClick={() => openEditor()}
        >新增 Provider</Button>
      </Space>
      {providers.error && <QueryFailure error={providers.error} />}
      <Table<McpRemoteProvider>
        rowKey="id"
        loading={providers.isLoading}
        dataSource={providers.data ?? []}
        columns={[
          { title: 'Provider Code', dataIndex: 'providerCode' },
          { title: '名称', render: (_, row) => value(row.content, 'displayName') },
          { title: 'Dialect', render: (_, row) => value(row.content, 'dialect') },
          { title: 'Transport', render: (_, row) => value(row.content, 'transportType') },
          { title: 'Endpoint Reference', render: (_, row) => value(row.content, 'endpointReference') },
          {
            title: '状态',
            render: (_, row) => <Tag>{value(row.content, 'status') || 'CONFIGURED'}</Tag>,
          },
          {
            title: '操作',
            render: (_, row) => <Space>
              <Button disabled={!canTest} onClick={() => discover.mutate(row)}>Discover / Diff</Button>
              <Button disabled={!canWrite} onClick={() => openEditor(row)}>编辑</Button>
              <Popconfirm title="确认删除 Provider？" onConfirm={() => remove.mutate(row)}>
                <Button danger disabled={!canWrite}>删除</Button>
              </Popconfirm>
            </Space>,
          },
        ]}
      />
      <Modal
        title={editing ? '编辑 Remote Provider' : '新增 Remote Provider'}
        open={open}
        width={720}
        onCancel={() => setOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={save.isPending}
        destroyOnHidden
      >
        {save.error && <QueryFailure error={save.error} />}
        <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)}>
          <Form.Item name="providerCode" label="Provider Code" rules={[{ required: true }]}>
            <Input disabled={Boolean(editing)} />
          </Form.Item>
          <Form.Item name="displayName" label="显示名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="dialect" label="Dialect" rules={[{ required: true }]}>
            <Select options={[
              { value: 'STABLE_2025_11_25', label: 'Stable 2025-11-25' },
              { value: 'RC_2026_07_28', label: 'RC 2026-07-28' },
              { value: 'LEGACY_2024_SSE', label: 'Legacy SSE' },
            ]} />
          </Form.Item>
          <Form.Item name="transportType" label="Transport" rules={[{ required: true }]}>
            <Select options={[
              { value: 'STREAMABLE_HTTP', label: 'Streamable HTTP' },
              { value: 'LEGACY_SSE', label: 'Legacy SSE' },
              { value: 'STDIO_MANAGED', label: 'Managed STDIO' },
            ]} />
          </Form.Item>
          <Form.Item
            name="endpointReference"
            label="Endpoint Reference"
            rules={[{ required: true }]}
            extra="只允许由 Gateway 管理、通过服务端安全校验的引用；Tool 表单不会接收 Provider URL。"
          >
            <Input />
          </Form.Item>
          <Form.Item name="authProfileReference" label="Auth Profile Reference"><Input /></Form.Item>
          <Form.Item name="tlsProfileReference" label="TLS Profile Reference"><Input /></Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked"><Switch /></Form.Item>
          <Form.Item name="changeReason" label="变更原因" rules={[{ required: true }]}>
            <Input.TextArea />
          </Form.Item>
        </Form>
      </Modal>
      <Modal
        title={`Remote Discover Diff · ${discovery?.provider.providerCode ?? ''}`}
        open={Boolean(discovery)}
        onCancel={() => setDiscovery(undefined)}
        footer={null}
        width={900}
      >
        {discovery && (
          <>
            <Typography.Paragraph>
              已发现 {discovery.capabilities.length} 个能力。Mount 必须固定本次 capability fingerprint；
              后续远端漂移会阻断统一发布。
            </Typography.Paragraph>
            <JsonPanel title="Reviewed Remote Capabilities" value={discovery.capabilities} />
          </>
        )}
      </Modal>
    </section>
  )
}
