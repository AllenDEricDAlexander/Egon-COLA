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
import type {
  McpRemoteCapability,
  McpRemoteMount,
  McpRemoteMountMutation,
} from '../../api/types'
import { useCapability } from '../../app/capabilities'
import { QueryFailure } from '../../components/QueryState'
import { parseJsonObject, parseStringList } from './mcpValidation'

type MountForm = {
  providerId: string
  namespace: string
  capabilityFingerprint: string
  primitiveTypes: string[]
  renameRules: string
  conflictPolicy: string
  requiredPermissions?: string
  enabled: boolean
  changeReason: string
}

export const McpRemoteMountsPanel = ({ serverId, gatewayGroupId, draftRevision }: {
  serverId: string
  gatewayGroupId: string
  draftRevision: number
}) => {
  const canWrite = useCapability('gateway:mcp:write')
  const canTest = useCapability('gateway:mcp:test')
  const queryClient = useQueryClient()
  const [form] = Form.useForm<MountForm>()
  const [editing, setEditing] = useState<McpRemoteMount>()
  const [open, setOpen] = useState(false)
  const [discovered, setDiscovered] = useState<McpRemoteCapability[]>([])
  const providerId = Form.useWatch('providerId', form)
  const providers = useQuery({
    queryKey: ['mcp-remote-providers', gatewayGroupId],
    queryFn: ({ signal }) => gatewayApi.mcpRemoteProviders(gatewayGroupId, signal),
  })
  const mounts = useQuery({
    queryKey: ['mcp-remote-mounts', gatewayGroupId],
    queryFn: ({ signal }) => gatewayApi.mcpRemoteMounts(gatewayGroupId, signal),
  })
  const discover = useMutation({
    mutationFn: () => gatewayApi.discoverMcpRemoteProvider(providerId),
    onSuccess: (capabilities) => {
      setDiscovered(capabilities)
      const fingerprint = capabilities[0]?.capabilityFingerprint
      if (fingerprint) form.setFieldValue('capabilityFingerprint', fingerprint)
    },
  })
  const save = useMutation({
    mutationFn: (values: MountForm) => {
      const renameRules = parseJsonObject(values.renameRules, 'Rename Rules')
      if (Object.values(renameRules).some((item) => typeof item !== 'string')) {
        throw new Error('Rename Rules 的值必须是字符串')
      }
      const mount: McpRemoteMountMutation = {
        gatewayGroupId,
        serverId,
        providerId: values.providerId,
        namespace: values.namespace,
        capabilityFingerprint: values.capabilityFingerprint,
        enabled: values.enabled,
        expectedRevision: editing?.revision ?? 0,
        expectedDraftRevision: draftRevision,
        changeReason: values.changeReason,
        content: {
          primitiveTypes: values.primitiveTypes,
          renameRules,
          conflictPolicy: values.conflictPolicy,
          requiredPermissions: parseStringList(values.requiredPermissions),
        },
      }
      return editing
        ? gatewayApi.updateMcpRemoteMount(editing.id, mount)
        : gatewayApi.createMcpRemoteMount(mount)
    },
    onSuccess: async () => {
      setOpen(false)
      setEditing(undefined)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['mcp-remote-mounts', gatewayGroupId] }),
        queryClient.invalidateQueries({ queryKey: ['gateway-draft', gatewayGroupId] }),
        queryClient.invalidateQueries({ queryKey: ['mcp-capability-preview', serverId] }),
      ])
      void message.success('Remote MCP Mount 已保存')
    },
  })
  const remove = useMutation({
    mutationFn: (mount: McpRemoteMount) => gatewayApi.deleteMcpRemoteMount(mount.id, {
      gatewayGroupId,
      expectedRevision: mount.revision,
      expectedDraftRevision: draftRevision,
      changeReason: 'Delete Remote MCP Mount from Admin Web',
    }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['mcp-remote-mounts', gatewayGroupId] }),
        queryClient.invalidateQueries({ queryKey: ['gateway-draft', gatewayGroupId] }),
      ])
    },
  })

  const openEditor = (mount?: McpRemoteMount) => {
    const content = mount?.content ?? {}
    setEditing(mount)
    setDiscovered([])
    form.setFieldsValue(mount ? {
      providerId: mount.providerId,
      namespace: mount.namespace,
      capabilityFingerprint: mount.capabilityFingerprint,
      primitiveTypes: Array.isArray(content.primitiveTypes)
        ? content.primitiveTypes as string[]
        : ['TOOL', 'RESOURCE', 'RESOURCE_TEMPLATE', 'PROMPT', 'APP'],
      renameRules: JSON.stringify(content.renameRules ?? {}, null, 2),
      conflictPolicy: typeof content.conflictPolicy === 'string' ? content.conflictPolicy : 'REJECT',
      requiredPermissions: Array.isArray(content.requiredPermissions)
        ? content.requiredPermissions.join(', ')
        : '',
      enabled: mount.enabled,
      changeReason: '',
    } : {
      providerId: '',
      namespace: '',
      capabilityFingerprint: '',
      primitiveTypes: ['TOOL', 'RESOURCE', 'RESOURCE_TEMPLATE', 'PROMPT', 'APP'],
      renameRules: '{}',
      conflictPolicy: 'REJECT',
      enabled: true,
      changeReason: '',
    })
    setOpen(true)
  }

  return (
    <section>
      <Button type="primary" disabled={!canWrite} onClick={() => openEditor()} style={{ marginBottom: 16 }}>
        新增 Remote Mount
      </Button>
      {mounts.error && <QueryFailure error={mounts.error} />}
      <Table<McpRemoteMount>
        rowKey="id"
        loading={mounts.isLoading}
        dataSource={(mounts.data ?? []).filter((mount) => mount.serverId === serverId)}
        columns={[
          { title: 'Namespace', dataIndex: 'namespace' },
          {
            title: 'Provider',
            render: (_, row) => providers.data?.find((provider) => provider.id === row.providerId)
              ?.providerCode ?? row.providerId,
          },
          {
            title: 'Fingerprint',
            render: (_, row) => `${row.capabilityFingerprint.slice(0, 12)}…`,
          },
          {
            title: 'Conflict Policy',
            render: (_, row) => <Tag>{String(row.content.conflictPolicy ?? 'REJECT')}</Tag>,
          },
          { title: 'Revision', dataIndex: 'revision' },
          {
            title: '操作',
            render: (_, row) => <Space>
              <Button disabled={!canWrite} onClick={() => openEditor(row)}>编辑</Button>
              <Popconfirm title="确认删除 Mount？" onConfirm={() => remove.mutate(row)}>
                <Button danger disabled={!canWrite}>删除</Button>
              </Popconfirm>
            </Space>,
          },
        ]}
      />
      <Modal
        title={editing ? '编辑 Remote Mount' : '新增 Remote Mount'}
        open={open}
        width={760}
        onCancel={() => setOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={save.isPending}
        destroyOnHidden
      >
        {save.error && <QueryFailure error={save.error} />}
        <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)}>
          <Form.Item name="providerId" label="Remote Provider" rules={[{ required: true }]}>
            <Select
              disabled={Boolean(editing)}
              options={(providers.data ?? []).map((provider) => ({
                value: provider.id,
                label: provider.providerCode,
              }))}
              onChange={() => {
                setDiscovered([])
                form.setFieldValue('capabilityFingerprint', '')
              }}
            />
          </Form.Item>
          <Button
            disabled={!canTest || !providerId}
            loading={discover.isPending}
            onClick={() => discover.mutate()}
            style={{ marginBottom: 16 }}
          >刷新能力指纹</Button>
          {discover.error && <QueryFailure error={discover.error} />}
          <Form.Item name="capabilityFingerprint" label="Capability Fingerprint" rules={[{ required: true }]}>
            <Input readOnly />
          </Form.Item>
          {discovered.length > 0 && (
            <Tag color="green">已审阅 {discovered.length} 个远程能力</Tag>
          )}
          <Form.Item name="namespace" label="Namespace" rules={[{ required: true }]}>
            <Input placeholder="billing" />
          </Form.Item>
          <Form.Item name="primitiveTypes" label="Primitive Types" rules={[{ required: true }]}>
            <Select mode="multiple" options={[
              'TOOL', 'RESOURCE', 'RESOURCE_TEMPLATE', 'PROMPT', 'APP',
            ].map((value) => ({ value, label: value }))} />
          </Form.Item>
          <Form.Item name="renameRules" label="Rename Rules JSON" rules={[{ required: true }]}>
            <Input.TextArea rows={5} />
          </Form.Item>
          <Form.Item name="conflictPolicy" label="Conflict Policy" rules={[{ required: true }]}>
            <Select options={[
              { value: 'REJECT', label: 'Reject（推荐）' },
              { value: 'KEEP_LOCAL', label: 'Keep Local' },
              { value: 'REPLACE', label: 'Replace' },
            ]} />
          </Form.Item>
          <Form.Item name="requiredPermissions" label="RBAC3 Permissions（逗号分隔）">
            <Input />
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
