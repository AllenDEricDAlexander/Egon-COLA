import { useQuery } from '@tanstack/react-query'
import {
  Alert,
  Button,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Switch,
  Table,
} from 'antd'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { gatewayApi } from '../../api/gatewayApi'
import type { McpCapabilityDraft } from '../../api/types'
import { useCapability } from '../../app/capabilities'
import { QueryFailure } from '../../components/QueryState'
import { GatewayScopeFilter } from '../../components/GatewayScopeFilter'
import { readScopeSearchParams, writeScopeSearchParams } from '../../hooks/scopeSearchParams'
import { parseStringList, renderPromptTemplate } from './mcpValidation'
import { useMcpCapabilityCollection } from './useMcpCapabilityCollection'

type PromptForm = {
  name: string
  description?: string
  sourceType: string
  template?: string
  operationId?: string
  remoteMountId?: string
  arguments?: string
  requiredPermissions?: string
  enabled: boolean
  changeReason: string
}

const stringValue = (value: unknown): string => typeof value === 'string' ? value : ''

export const McpPromptsPanel = ({ serverId, gatewayGroupId, draftRevision }: {
  serverId: string
  gatewayGroupId: string
  draftRevision: number
}) => {
  const [searchParams, setSearchParams] = useSearchParams()
  const scope = readScopeSearchParams(searchParams, ['bizCode', 'namespace', 'env', 'appCode'], 'prompt')
  const canWrite = useCapability('gateway:mcp:write')
  const collection = useMcpCapabilityCollection('prompts', serverId, gatewayGroupId, draftRevision)
  const applications = useQuery({
    queryKey: ['mcp-applications', scope],
    queryFn: ({ signal }) => gatewayApi.applications(scope, signal),
  })
  const selectedApplicationId = searchParams.get('promptApplicationId') ?? ''
  const operations = useQuery({
    queryKey: ['mcp-operation-options', selectedApplicationId],
    queryFn: ({ signal }) => gatewayApi.mcpOperationOptions(selectedApplicationId, signal),
    enabled: Boolean(selectedApplicationId),
  })
  const mounts = useQuery({
    queryKey: ['mcp-remote-mounts', gatewayGroupId],
    queryFn: ({ signal }) => gatewayApi.mcpRemoteMounts(gatewayGroupId, signal),
  })
  const [form] = Form.useForm<PromptForm>()
  const sourceType = Form.useWatch('sourceType', form) ?? 'STATIC_TEMPLATE'
  const [editing, setEditing] = useState<McpCapabilityDraft>()
  const [open, setOpen] = useState(false)
  const [rendered, setRendered] = useState<string>()
  const [renderError, setRenderError] = useState<string>()

  const openEditor = (prompt?: McpCapabilityDraft) => {
    const content = prompt?.content ?? {}
    setEditing(prompt)
    setRendered(undefined)
    setRenderError(undefined)
    form.setFieldsValue(prompt ? {
      name: prompt.name,
      description: stringValue(content.description),
      sourceType: stringValue(content.sourceType) || 'STATIC_TEMPLATE',
      template: stringValue(content.template),
      operationId: stringValue(content.operationId) || undefined,
      remoteMountId: stringValue(content.remoteMountId) || undefined,
      arguments: Array.isArray(content.arguments) ? content.arguments.join(', ') : '',
      requiredPermissions: Array.isArray(content.requiredPermissions)
        ? content.requiredPermissions.join(', ')
        : '',
      enabled: prompt.enabled,
      changeReason: '',
    } : {
      name: '',
      sourceType: 'STATIC_TEMPLATE',
      template: '',
      enabled: true,
      changeReason: '',
    })
    setOpen(true)
  }

  const submit = (values: PromptForm) => {
    collection.save.mutate({
      editing,
      name: values.name,
      enabled: values.enabled,
      changeReason: values.changeReason,
      content: {
        description: values.description,
        sourceType: values.sourceType,
        template: ['STATIC_TEMPLATE', 'LOCAL_TEMPLATE', 'STRICT_TEMPLATE'].includes(values.sourceType)
          ? values.template
          : undefined,
        operationId: values.sourceType === 'LOCAL_OPERATION' ? values.operationId : undefined,
        remoteMountId: values.sourceType === 'REMOTE_MCP' ? values.remoteMountId : undefined,
        arguments: parseStringList(values.arguments),
        requiredPermissions: parseStringList(values.requiredPermissions),
      },
    }, {
      onSuccess: () => {
        setOpen(false)
        setEditing(undefined)
      },
    })
  }

  const testRender = () => {
    const values = form.getFieldsValue()
    try {
      setRendered(renderPromptTemplate(values.template ?? '', parseStringList(values.arguments)))
      setRenderError(undefined)
    } catch (error) {
      setRendered(undefined)
      setRenderError(error instanceof Error ? error.message : 'Prompt 渲染失败')
    }
  }

  return (
    <section>
      <GatewayScopeFilter
        fields={['bizCode', 'namespace', 'env', 'appCode']}
        value={scope}
        onChange={(value) => {
          const next = writeScopeSearchParams(searchParams, value, ['bizCode', 'namespace', 'env', 'appCode'], 'prompt')
          next.delete('promptApplicationId')
          next.delete('promptOperationId')
          setSearchParams(next)
        }}
      />
      <Select
        aria-label="Prompt Application"
        style={{ width: 360, marginBottom: 16 }}
        placeholder="选择 Application 后加载 Operation"
        value={selectedApplicationId || undefined}
        loading={applications.isLoading}
        options={(applications.data ?? []).map((application) => ({
          value: application.id,
          label: `${application.bizCode} / ${application.applicationCode} / ${application.env} / ${application.namespace} · ${application.displayName}`,
        }))}
        onChange={(value) => {
          const next = new URLSearchParams(searchParams)
          next.set('promptApplicationId', value)
          next.delete('promptOperationId')
          setSearchParams(next)
        }}
      />
      <Button type="primary" disabled={!canWrite} onClick={() => openEditor()} style={{ marginBottom: 16 }}>
        新增 Prompt
      </Button>
      {collection.query.error && <QueryFailure error={collection.query.error} />}
      <Table<McpCapabilityDraft>
        rowKey="id"
        loading={collection.query.isLoading}
        dataSource={collection.query.data ?? []}
        columns={[
          { title: 'Prompt Name', dataIndex: 'name' },
          { title: 'Source', render: (_, row) => stringValue(row.content.sourceType) },
          {
            title: 'Arguments',
            render: (_, row) => Array.isArray(row.content.arguments)
              ? row.content.arguments.join(', ')
              : '-',
          },
          { title: 'Revision', dataIndex: 'revision' },
          {
            title: '操作',
            render: (_, row) => <Space>
              <Button disabled={!canWrite} onClick={() => openEditor(row)}>编辑</Button>
              <Popconfirm title="确认删除 Prompt？" onConfirm={() => collection.remove.mutate(row)}>
                <Button danger disabled={!canWrite}>删除</Button>
              </Popconfirm>
            </Space>,
          },
        ]}
      />
      <Modal
        title={editing ? '编辑 Prompt' : '新增 Prompt'}
        open={open}
        width={760}
        onCancel={() => setOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={collection.save.isPending}
        destroyOnHidden
      >
        {collection.save.error && <QueryFailure error={collection.save.error} />}
        <Form form={form} layout="vertical" onFinish={submit}>
          <Form.Item name="name" label="Prompt Name" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="description" label="描述"><Input.TextArea /></Form.Item>
          <Form.Item name="sourceType" label="来源" rules={[{ required: true }]}>
            <Select options={[
              { value: 'STATIC_TEMPLATE', label: 'Static Template' },
              { value: 'STRICT_TEMPLATE', label: 'Strict Template' },
              { value: 'LOCAL_OPERATION', label: 'Local Operation' },
              { value: 'REMOTE_MCP', label: 'Remote MCP Mount' },
            ]} />
          </Form.Item>
          {['STATIC_TEMPLATE', 'LOCAL_TEMPLATE', 'STRICT_TEMPLATE'].includes(sourceType) && (
            <Form.Item name="template" label="Prompt Template" rules={[{ required: true }]}>
              <Input.TextArea rows={8} placeholder="请处理订单 {{orderId}}" />
            </Form.Item>
          )}
          {sourceType === 'LOCAL_OPERATION' && (
            <Form.Item name="operationId" label="Operation" rules={[{ required: true }]}>
              <Select showSearch optionFilterProp="label" options={operations.data ?? []} />
            </Form.Item>
          )}
          {sourceType === 'REMOTE_MCP' && (
            <Form.Item name="remoteMountId" label="Remote Mount" rules={[{ required: true }]}>
              <Select options={(mounts.data ?? []).map((mount) => ({
                value: mount.id,
                label: mount.namespace,
              }))} />
            </Form.Item>
          )}
          <Form.Item name="arguments" label="Arguments（逗号分隔）"><Input /></Form.Item>
          <Form.Item name="requiredPermissions" label="RBAC3 Permissions（逗号分隔）">
            <Input />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked"><Switch /></Form.Item>
          <Form.Item name="changeReason" label="变更原因" rules={[{ required: true }]}>
            <Input.TextArea />
          </Form.Item>
          {['STATIC_TEMPLATE', 'LOCAL_TEMPLATE', 'STRICT_TEMPLATE'].includes(sourceType) && (
            <Button onClick={testRender}>测试渲染</Button>
          )}
          {renderError && <Alert type="error" showIcon title={renderError} style={{ marginTop: 12 }} />}
          {rendered !== undefined && (
            <Alert type="success" title="Prompt Render Preview" description={rendered} style={{ marginTop: 12 }} />
          )}
        </Form>
      </Modal>
    </section>
  )
}
