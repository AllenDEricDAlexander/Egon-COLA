import { useQuery } from '@tanstack/react-query'
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
} from 'antd'
import { useState } from 'react'
import { gatewayApi } from '../../api/gatewayApi'
import type { McpCapabilityDraft, McpCapabilityPlural } from '../../api/types'
import { useCapability } from '../../app/capabilities'
import { QueryFailure } from '../../components/QueryState'
import { useScope } from '../../hooks/useScope'
import { parseStringList, validateResourceTemplate, validateResourceUri } from './mcpValidation'
import { useMcpCapabilityCollection } from './useMcpCapabilityCollection'

type ResourceForm = {
  kind: 'resource' | 'template'
  name: string
  uri?: string
  uriTemplate?: string
  description?: string
  mimeType: string
  driverType: string
  operationId?: string
  remoteMountId?: string
  driverValue?: string
  requiredPermissions?: string
  maxBytes: number
  enabled: boolean
  changeReason: string
}

const text = (value: unknown): string => typeof value === 'string' ? value : ''

const configurationValue = (content: Record<string, unknown>): string => {
  const configuration = content.configuration
  if (!configuration || typeof configuration !== 'object' || Array.isArray(configuration)) return ''
  const values = configuration as Record<string, unknown>
  return text(values.content ?? values.base64 ?? values.allowedSchemas)
}

const binding = (values: ResourceForm) => ({
  operationId: values.driverType === 'LOCAL_OPERATION' ? values.operationId : undefined,
  remoteMountId: values.driverType === 'REMOTE_MCP' ? values.remoteMountId : undefined,
})

const configuration = (values: ResourceForm): Record<string, string> => {
  if (values.driverType === 'STATIC_TEXT') return { content: values.driverValue ?? '' }
  if (values.driverType === 'STATIC_BLOB') return { base64: values.driverValue ?? '' }
  if (values.driverType === 'DATABASE_SCHEMA') return { allowedSchemas: values.driverValue ?? '' }
  return {}
}

export const McpResourcesPanel = ({ serverId, gatewayGroupId, draftRevision }: {
  serverId: string
  gatewayGroupId: string
  draftRevision: number
}) => {
  const { scope } = useScope()
  const canWrite = useCapability('gateway:mcp:write')
  const resources = useMcpCapabilityCollection('resources', serverId, gatewayGroupId, draftRevision)
  const templates = useMcpCapabilityCollection(
    'resource-templates',
    serverId,
    gatewayGroupId,
    draftRevision,
  )
  const operations = useQuery({
    queryKey: ['mcp-operation-options', scope],
    queryFn: ({ signal }) => gatewayApi.mcpOperationOptions(scope, signal),
  })
  const mounts = useQuery({
    queryKey: ['mcp-remote-mounts', gatewayGroupId],
    queryFn: ({ signal }) => gatewayApi.mcpRemoteMounts(gatewayGroupId, signal),
  })
  const [form] = Form.useForm<ResourceForm>()
  const kind = Form.useWatch('kind', form) ?? 'resource'
  const driverType = Form.useWatch('driverType', form) ?? 'STATIC_TEXT'
  const [editing, setEditing] = useState<McpCapabilityDraft>()
  const [open, setOpen] = useState(false)

  const state = kind === 'template' ? templates : resources
  const openEditor = (nextKind: ResourceForm['kind'], item?: McpCapabilityDraft) => {
    const content = item?.content ?? {}
    setEditing(item)
    form.setFieldsValue(item ? {
      kind: nextKind,
      name: item.name,
      uri: text(content.uri),
      uriTemplate: text(content.uriTemplate),
      description: text(content.description),
      mimeType: text(content.mimeType) || 'application/json',
      driverType: text(content.driverType) || 'STATIC_TEXT',
      operationId: text(content.operationId) || undefined,
      remoteMountId: text(content.remoteMountId) || undefined,
      driverValue: configurationValue(content),
      requiredPermissions: Array.isArray(content.requiredPermissions)
        ? content.requiredPermissions.join(', ')
        : '',
      maxBytes: typeof content.maxBytes === 'number' ? content.maxBytes : 1_048_576,
      enabled: item.enabled,
      changeReason: '',
    } : {
      kind: nextKind,
      name: '',
      mimeType: 'application/json',
      driverType: 'STATIC_TEXT',
      maxBytes: 1_048_576,
      enabled: true,
      changeReason: '',
    })
    setOpen(true)
  }

  const submit = (values: ResourceForm) => {
    const target = values.kind === 'template' ? templates : resources
    const uri = values.kind === 'resource'
      ? validateResourceUri(values.uri ?? '')
      : undefined
    const uriTemplate = values.kind === 'template'
      ? validateResourceTemplate(values.uriTemplate ?? '')
      : undefined
    target.save.mutate({
      editing,
      name: values.name,
      enabled: values.enabled,
      changeReason: values.changeReason,
      content: {
        uri,
        uriTemplate,
        description: values.description,
        mimeType: values.mimeType,
        driverType: values.driverType,
        ...binding(values),
        configuration: configuration(values),
        requiredPermissions: parseStringList(values.requiredPermissions),
        maxBytes: values.maxBytes,
      },
    }, {
      onSuccess: () => {
        setOpen(false)
        setEditing(undefined)
      },
    })
  }

  const table = (plural: McpCapabilityPlural, data: McpCapabilityDraft[] | undefined) => {
    const isTemplate = plural === 'resource-templates'
    const collection = isTemplate ? templates : resources
    return (
      <section>
        <Button
          type="primary"
          disabled={!canWrite}
          onClick={() => openEditor(isTemplate ? 'template' : 'resource')}
          style={{ marginBottom: 16 }}
        >
          {isTemplate ? '新增 Resource Template' : '新增 Resource'}
        </Button>
        {collection.query.error && <QueryFailure error={collection.query.error} />}
        <Table<McpCapabilityDraft>
          rowKey="id"
          loading={collection.query.isLoading}
          dataSource={data ?? []}
          columns={[
            { title: 'Name', dataIndex: 'name' },
            {
              title: isTemplate ? 'URI Template' : 'URI',
              render: (_, row) => text(row.content[isTemplate ? 'uriTemplate' : 'uri']),
            },
            { title: 'Driver', render: (_, row) => text(row.content.driverType) },
            { title: 'Revision', dataIndex: 'revision' },
            {
              title: '操作',
              render: (_, row) => <Space>
                <Button
                  disabled={!canWrite}
                  onClick={() => openEditor(isTemplate ? 'template' : 'resource', row)}
                >编辑</Button>
                <Popconfirm title="确认删除？" onConfirm={() => collection.remove.mutate(row)}>
                  <Button danger disabled={!canWrite}>删除</Button>
                </Popconfirm>
              </Space>,
            },
          ]}
        />
      </section>
    )
  }

  return (
    <section>
      <Tabs items={[
        { key: 'resources', label: 'Resources', children: table('resources', resources.query.data) },
        {
          key: 'resource-templates',
          label: 'Resource Templates',
          children: table('resource-templates', templates.query.data),
        },
      ]} />
      <Modal
        title={editing ? '编辑 Resource Capability' : '新增 Resource Capability'}
        open={open}
        width={760}
        onCancel={() => setOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={state.save.isPending}
        destroyOnHidden
      >
        {state.save.error && <QueryFailure error={state.save.error} />}
        <Form form={form} layout="vertical" onFinish={submit}>
          <Form.Item name="kind" hidden><Input /></Form.Item>
          <Form.Item name="name" label="Name" rules={[{ required: true }]}><Input /></Form.Item>
          {kind === 'template' ? (
            <Form.Item name="uriTemplate" label="URI Template" rules={[{ required: true }]}>
              <Input placeholder="egon://orders/{id}" />
            </Form.Item>
          ) : (
            <Form.Item name="uri" label="Resource URI" rules={[{ required: true }]}>
              <Input placeholder="egon://orders/schema" />
            </Form.Item>
          )}
          <Form.Item name="description" label="描述"><Input.TextArea /></Form.Item>
          <Form.Item name="mimeType" label="MIME Type" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="driverType" label="Driver" rules={[{ required: true }]}>
            <Select options={[
              { value: 'STATIC_TEXT', label: 'Static Text' },
              { value: 'STATIC_BLOB', label: 'Static Blob (Base64)' },
              { value: 'DATABASE_SCHEMA', label: 'Database Schema (Allowlist)' },
              { value: 'LOCAL_OPERATION', label: 'Local Operation' },
              { value: 'REMOTE_MCP', label: 'Remote MCP Mount' },
            ]} />
          </Form.Item>
          {driverType === 'LOCAL_OPERATION' && (
            <Form.Item name="operationId" label="Operation" rules={[{ required: true }]}>
              <Select showSearch optionFilterProp="label" options={operations.data ?? []} />
            </Form.Item>
          )}
          {driverType === 'REMOTE_MCP' && (
            <Form.Item name="remoteMountId" label="Remote Mount" rules={[{ required: true }]}>
              <Select options={(mounts.data ?? []).map((mount) => ({
                value: mount.id,
                label: mount.namespace,
              }))} />
            </Form.Item>
          )}
          {['STATIC_TEXT', 'STATIC_BLOB', 'DATABASE_SCHEMA'].includes(driverType) && (
            <Form.Item
              name="driverValue"
              label={driverType === 'STATIC_TEXT'
                ? 'Text Content'
                : driverType === 'STATIC_BLOB'
                  ? 'Base64 Content'
                  : 'Allowed Schemas（逗号分隔）'}
              rules={[{ required: true }]}
            >
              <Input.TextArea rows={5} />
            </Form.Item>
          )}
          <Form.Item name="requiredPermissions" label="RBAC3 Permissions（逗号分隔）">
            <Input />
          </Form.Item>
          <Form.Item name="maxBytes" label="最大响应字节" rules={[{ required: true }]}>
            <InputNumber min={1} max={67_108_864} />
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
