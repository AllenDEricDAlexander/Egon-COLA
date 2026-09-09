import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Descriptions,
  Form,
  Input,
  Modal,
  Popconfirm,
  Space,
  Tabs,
  Tag,
  Timeline,
  Typography,
  message,
} from 'antd'
import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { gatewayApi } from '../../api/gatewayApi'
import { GatewayApiError } from '../../api/client'
import type { GatewayOperationOpenApi, OperationDefinition } from '../../api/types'
import { useCapability } from '../../app/capabilities'
import { JsonPanel } from '../../components/JsonPanel'
import { LoadingBlock, QueryFailure } from '../../components/QueryState'
import { StatusTag } from '../../components/StatusTag'
import { sanitizeForDisplay } from '../observability/sanitize'
import { SchemaPanel } from './SchemaPanel'

const OPENAPI_SOURCE = 'OPENAPI31'

type MetadataForm = {
  summary?: string
  tags: string
  owner?: string
}

type DefinitionForm = {
  summary?: string
  tags: string
  requestSchema: string
  responseSchema: string
  errorSchema: string
  descriptorSnapshot: string
  attributes: string
  externalAccessible: boolean
}

const openApiJson = (fragment: GatewayOperationOpenApi): string =>
  JSON.stringify(fragment.operation, null, 2)

const splitTags = (value?: string): string[] =>
  value?.split(/[,\s]+/).map((tag) => tag.trim()).filter(Boolean) ?? []

const parseJson = (value: string, field: string): unknown => {
  try {
    return JSON.parse(value)
  } catch {
    throw new Error(field + ' JSON 无效')
  }
}

const parseOptionalJson = (value: string, field: string): unknown | undefined =>
  value.trim() ? parseJson(value, field) : undefined

const jsonText = (value: unknown): string => JSON.stringify(value ?? {}, null, 2)

const OpenApiUnavailable = () => (
  <Alert
    showIcon
    type="info"
    message="当前 Operation 没有 OpenAPI source"
    description="MANUAL 或 RPC_DESCRIPTOR Operation 不生成伪 OpenAPI Fragment。"
  />
)

export const OperationPage = () => {
  const { operationId = '' } = useParams()
  const queryClient = useQueryClient()
  const canWrite = useCapability('yuheng:catalog:write')
  const [activeTab, setActiveTab] = useState('schema')
  const [downloadPending, setDownloadPending] = useState(false)
  const [actionError, setActionError] = useState<string>()
  const [metadataOpen, setMetadataOpen] = useState(false)
  const [definitionOpen, setDefinitionOpen] = useState(false)
  const [metadataForm] = Form.useForm<MetadataForm>()
  const [definitionForm] = Form.useForm<DefinitionForm>()

  const query = useQuery({
    queryKey: ['operation', operationId],
    queryFn: ({ signal }) => gatewayApi.operation(operationId, signal),
    enabled: Boolean(operationId),
  })
  const hasOpenApiSource = query.data?.operation.sourceType === OPENAPI_SOURCE
  const openApiQuery = useQuery({
    queryKey: ['operation-openapi', operationId, query.data?.operation.currentDefinitionId],
    queryFn: ({ signal }) => gatewayApi.operationOpenApi(operationId, signal),
    enabled: hasOpenApiSource && activeTab === 'openapi',
  })
  const metadataMutation = useMutation({
    mutationFn: (values: MetadataForm) => gatewayApi.updateOperationMetadata(operationId, {
      summary: values.summary?.trim() || undefined,
      tags: splitTags(values.tags),
      owner: values.owner?.trim() || undefined,
    }),
    onSuccess: async () => {
      setMetadataOpen(false)
      await queryClient.invalidateQueries({ queryKey: ['operation', operationId] })
      void message.success('Operation 元数据已更新')
    },
    onError: (error) => setActionError(error instanceof Error ? error.message : '元数据更新失败'),
  })
  const definitionMutation = useMutation({
    mutationFn: (values: DefinitionForm) => gatewayApi.updateManualDefinition(operationId, {
      summary: values.summary?.trim() || undefined,
      tags: splitTags(values.tags),
      requestSchema: parseJson(values.requestSchema, 'Request Schema'),
      responseSchema: parseJson(values.responseSchema, 'Response Schema'),
      errorSchema: parseJson(values.errorSchema, 'Error Schema'),
      descriptorSnapshot: parseOptionalJson(values.descriptorSnapshot, 'Descriptor Snapshot'),
      attributes: parseJson(values.attributes, 'Attributes'),
      externalAccessible: values.externalAccessible,
    }),
    onSuccess: async () => {
      setDefinitionOpen(false)
      await queryClient.invalidateQueries({ queryKey: ['operation', operationId] })
      void message.success('Operation 定义已更新')
    },
    onError: (error) => setActionError(error instanceof Error ? error.message : '定义更新失败'),
  })
  const deprecateMutation = useMutation({
    mutationFn: () => gatewayApi.deprecateOperation(operationId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['operation', operationId] })
      void message.success('Operation 已提交废弃')
    },
    onError: (error) => setActionError(error instanceof Error ? error.message : '废弃操作失败'),
  })

  if (query.isLoading) return <LoadingBlock />
  if (query.error || !query.data) return <QueryFailure error={query.error} />

  const { operation, definitions } = query.data
  const current = definitions.find((definition) => definition.id === operation.currentDefinitionId)
    ?? definitions[0]
  const isManual = operation.sourceType === 'MANUAL'
  const openMetadataEditor = () => {
    metadataForm.setFieldsValue({
      summary: current?.summary,
      tags: current?.tags.join(', ') ?? '',
      owner: typeof current?.attributes.owner === 'string'
        ? current.attributes.owner
        : '',
    })
    setActionError(undefined)
    setMetadataOpen(true)
  }
  const openDefinitionEditor = () => {
    if (!current) return
    definitionForm.setFieldsValue({
      summary: current.summary,
      tags: current.tags.join(', '),
      requestSchema: jsonText(current.requestSchema),
      responseSchema: jsonText(current.responseSchema),
      errorSchema: jsonText(current.errorSchema),
      descriptorSnapshot: current.descriptorSnapshot ? jsonText(current.descriptorSnapshot) : '',
      attributes: jsonText(current.attributes),
      externalAccessible: current.externalAccessible,
    })
    setActionError(undefined)
    setDefinitionOpen(true)
  }
  const copyOpenApi = async (fragment: GatewayOperationOpenApi) => {
    setActionError(undefined)
    try {
      const value = openApiJson(fragment)
      if (!navigator.clipboard?.writeText) throw new Error('当前浏览器不支持剪贴板')
      await navigator.clipboard.writeText(value)
    } catch (error) {
      setActionError(error instanceof Error ? error.message : '复制 OpenAPI Operation 失败')
    }
  }
  const downloadOpenApi = async (fragment: GatewayOperationOpenApi) => {
    setActionError(undefined)
    setDownloadPending(true)
    try {
      const document = await gatewayApi.openapiSnapshotDocument(fragment.snapshotId)
      const blob = new Blob([JSON.stringify(document.document, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const anchor = window.document.createElement('a')
      anchor.href = url
      anchor.download = fragment.openapiGroup + '-' + fragment.snapshotId + '.json'
      anchor.click()
      URL.revokeObjectURL(url)
    } catch (error) {
      setActionError(error instanceof Error ? error.message : '下载 OpenAPI 文档失败')
    } finally {
      setDownloadPending(false)
    }
  }
  const openApiPanel = hasOpenApiSource ? (
    openApiQuery.isLoading ? <LoadingBlock />
      : openApiQuery.error ? (
        <Alert
          showIcon
          type="error"
          message={openApiQuery.error instanceof GatewayApiError
            ? openApiQuery.error.message
            : 'OpenAPI Fragment 加载失败'}
          description={openApiQuery.error instanceof GatewayApiError
            ? openApiQuery.error.code
            : undefined}
          action={<Button onClick={() => void openApiQuery.refetch()}>重试 OpenAPI</Button>}
        />
      )
        : openApiQuery.data ? (
          <Space direction="vertical" className="full-width">
            <Descriptions column={2}>
              <Descriptions.Item label="Source"><Tag>{operation.sourceType}</Tag></Descriptions.Item>
              <Descriptions.Item label="OpenAPI Group">{openApiQuery.data.openapiGroup}</Descriptions.Item>
              <Descriptions.Item label="Path">{openApiQuery.data.method} {openApiQuery.data.path}</Descriptions.Item>
              <Descriptions.Item label="Operation ID">{openApiQuery.data.openapiOperationId}</Descriptions.Item>
              <Descriptions.Item label="Snapshot">{openApiQuery.data.snapshotId}</Descriptions.Item>
              <Descriptions.Item label="同步时间">{openApiQuery.data.syncedAt}</Descriptions.Item>
              <Descriptions.Item label="Request Content-Type">{openApiQuery.data.requestContentTypes.join(', ') || '—'}</Descriptions.Item>
              <Descriptions.Item label="Response Content-Type">{openApiQuery.data.responseContentTypes.join(', ') || '—'}</Descriptions.Item>
            </Descriptions>
            <Space>
              <Button onClick={() => void copyOpenApi(openApiQuery.data!)}>复制 OpenAPI Operation</Button>
              <Button onClick={() => void openApiQuery.refetch()}>刷新 OpenAPI</Button>
              <Button loading={downloadPending} onClick={() => void downloadOpenApi(openApiQuery.data!)}>
                下载完整 OpenAPI 文档
              </Button>
            </Space>
            {actionError && <Alert showIcon type="warning" message={actionError} />}
            <Card size="small" title="OpenAPI Operation JSON">
              <pre aria-label="OpenAPI Operation JSON" className="json-panel">{openApiJson(openApiQuery.data)}</pre>
            </Card>
          </Space>
        ) : <Alert showIcon type="info" message="暂无 OpenAPI Fragment" />
  ) : <OpenApiUnavailable />

  return (
    <section>
      <Typography.Title level={2}>{operation.methodIdentity}</Typography.Title>
      {!hasOpenApiSource && <OpenApiUnavailable />}
      <Card className="section-row">
        <Descriptions column={2}>
          <Descriptions.Item label="Operation Key">{operation.operationKey}</Descriptions.Item>
          <Descriptions.Item label="Protocol"><Tag>{operation.protocol}</Tag></Descriptions.Item>
          <Descriptions.Item label="Source"><Tag>{operation.sourceType}</Tag></Descriptions.Item>
          <Descriptions.Item label="External Accessible">
            <StatusTag status={operation.externalAccessible ? 'ALLOWED' : 'INTERNAL_ONLY'} />
          </Descriptions.Item>
          <Descriptions.Item label="Lifecycle"><StatusTag status={operation.lifecycleStatus} /></Descriptions.Item>
          <Descriptions.Item label="Revision">{operation.revision}</Descriptions.Item>
        </Descriptions>
        {isManual && canWrite && (
          <Space wrap style={{ marginTop: 16 }}>
            <Button onClick={openMetadataEditor}>编辑元数据</Button>
            <Button onClick={openDefinitionEditor} disabled={!current}>编辑定义</Button>
            <Popconfirm
              title="确认废弃此 Operation？"
              description="废弃操作将由服务端记录审计，不能通过此页面直接恢复。"
              okText="确认废弃"
              cancelText="取消"
              onConfirm={() => deprecateMutation.mutate()}
            >
              <Button danger loading={deprecateMutation.isPending}>废弃 Operation</Button>
            </Popconfirm>
          </Space>
        )}
        {actionError && <Alert showIcon type="warning" message={actionError} style={{ marginTop: 16 }} />}
      </Card>
      {current && (
        <Tabs
          className="section-row"
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: 'schema',
              label: 'Schema',
              children: (
                <Space direction="vertical" className="full-width">
                  <JsonPanel title="Provider Service Identity" value={sanitizeForDisplay(operation.providerServiceIdentity)} />
                  <SchemaPanel title="Request Schema" schema={current.requestSchema} />
                  <SchemaPanel title="Response Schema" schema={current.responseSchema} />
                  <JsonPanel title="Error Schema" value={sanitizeForDisplay(current.errorSchema)} />
                  {current.descriptorSnapshot && (
                    <JsonPanel title="RPC Descriptor Snapshot" value={{
                      ...sanitizeForDisplay(current.descriptorSnapshot) as Record<string, unknown>,
                      base64DescriptorSet: '[按需下载，页面不展开]',
                    }} />
                  )}
                </Space>
              ),
            },
            ...(hasOpenApiSource ? [{ key: 'openapi', label: 'OpenAPI', children: openApiPanel }] : []),
          ]}
        />
      )}
      <Card title="Definition History" className="section-row">
        <Timeline items={definitions.map((definition: OperationDefinition) => ({
          children: 'v' + definition.definitionVersion + ' · ' + definition.definitionSha256 + ' · ' + definition.createdAt,
        }))} />
      </Card>
      <Modal
        title="编辑 Operation 元数据"
        open={metadataOpen}
        onCancel={() => setMetadataOpen(false)}
        onOk={() => metadataForm.submit()}
        okText="保存元数据"
        cancelText="取消"
        confirmLoading={metadataMutation.isPending}
        destroyOnHidden
      >
        <Form form={metadataForm} layout="vertical" onFinish={(values) => metadataMutation.mutate(values)}>
          <Form.Item name="summary" label="摘要"><Input /></Form.Item>
          <Form.Item name="tags" label="标签（逗号或空格分隔）" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="owner" label="Owner"><Input /></Form.Item>
        </Form>
      </Modal>
      <Modal
        title="编辑 Manual Operation 定义"
        open={definitionOpen}
        width={720}
        onCancel={() => setDefinitionOpen(false)}
        onOk={() => definitionForm.submit()}
        okText="保存定义"
        cancelText="取消"
        confirmLoading={definitionMutation.isPending}
        destroyOnHidden
      >
        <Form form={definitionForm} layout="vertical" onFinish={(values) => definitionMutation.mutate(values)}>
          <Form.Item name="summary" label="摘要"><Input /></Form.Item>
          <Form.Item name="tags" label="标签（逗号或空格分隔）" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="requestSchema" label="Request Schema JSON" rules={[{ required: true }]}><Input.TextArea rows={6} /></Form.Item>
          <Form.Item name="responseSchema" label="Response Schema JSON" rules={[{ required: true }]}><Input.TextArea rows={6} /></Form.Item>
          <Form.Item name="errorSchema" label="Error Schema JSON" rules={[{ required: true }]}><Input.TextArea rows={4} /></Form.Item>
          <Form.Item name="descriptorSnapshot" label="Descriptor Snapshot JSON"><Input.TextArea rows={4} /></Form.Item>
          <Form.Item name="attributes" label="Attributes JSON" rules={[{ required: true }]}><Input.TextArea rows={4} /></Form.Item>
          <Form.Item name="externalAccessible" valuePropName="checked"><Checkbox>允许外部调用</Checkbox></Form.Item>
        </Form>
      </Modal>
    </section>
  )
}
