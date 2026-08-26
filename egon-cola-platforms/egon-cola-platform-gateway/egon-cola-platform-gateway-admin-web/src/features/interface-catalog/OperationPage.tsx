import { useQuery } from '@tanstack/react-query'
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Space,
  Tabs,
  Tag,
  Timeline,
  Typography,
} from 'antd'
import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { gatewayApi } from '../../api/gatewayApi'
import { GatewayApiError } from '../../api/client'
import type { GatewayOperationOpenApi } from '../../api/types'
import { JsonPanel } from '../../components/JsonPanel'
import { LoadingBlock, QueryFailure } from '../../components/QueryState'
import { StatusTag } from '../../components/StatusTag'
import { SchemaPanel } from './SchemaPanel'

const OPENAPI_SOURCE = 'OPENAPI31'

const openApiJson = (fragment: GatewayOperationOpenApi): string =>
  JSON.stringify(fragment.operation, null, 2)

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
  const [activeTab, setActiveTab] = useState('schema')
  const [downloadPending, setDownloadPending] = useState(false)
  const [actionError, setActionError] = useState<string>()
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
  if (query.isLoading) return <LoadingBlock />
  if (query.error || !query.data) return <QueryFailure error={query.error} />
  const { operation, definitions } = query.data
  const current = definitions.find((definition) => definition.id === operation.currentDefinitionId)
    ?? definitions[0]
  const copyOpenApi = async (fragment: GatewayOperationOpenApi) => {
    setActionError(undefined)
    try {
      const value = openApiJson(fragment)
      if (!navigator.clipboard?.writeText) {
        throw new Error('当前浏览器不支持剪贴板')
      }
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
      const blob = new Blob([JSON.stringify(document.document, null, 2)], {
        type: 'application/json',
      })
      const url = URL.createObjectURL(blob)
      const anchor = window.document.createElement('a')
      anchor.href = url
      anchor.download = `${fragment.openapiGroup}-${fragment.snapshotId}.json`
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
          action={(
            <Button onClick={() => void openApiQuery.refetch()}>
              重试 OpenAPI
            </Button>
          )}
        />
      )
        : openApiQuery.data ? (
          <Space direction="vertical" className="full-width">
            <Descriptions column={2}>
              <Descriptions.Item label="Source">
                <Tag>{operation.sourceType}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="OpenAPI Group">
                {openApiQuery.data.openapiGroup}
              </Descriptions.Item>
              <Descriptions.Item label="Path">
                {openApiQuery.data.method} {openApiQuery.data.path}
              </Descriptions.Item>
              <Descriptions.Item label="Operation ID">
                {openApiQuery.data.openapiOperationId}
              </Descriptions.Item>
              <Descriptions.Item label="Snapshot">
                {openApiQuery.data.snapshotId}
              </Descriptions.Item>
              <Descriptions.Item label="同步时间">
                {openApiQuery.data.syncedAt}
              </Descriptions.Item>
              <Descriptions.Item label="Request Content-Type">
                {openApiQuery.data.requestContentTypes.join(', ') || '—'}
              </Descriptions.Item>
              <Descriptions.Item label="Response Content-Type">
                {openApiQuery.data.responseContentTypes.join(', ') || '—'}
              </Descriptions.Item>
            </Descriptions>
            <Space>
              <Button
                onClick={() => void copyOpenApi(openApiQuery.data!)}
              >
                复制 OpenAPI Operation
              </Button>
              <Button
                onClick={() => void openApiQuery.refetch()}
              >
                刷新 OpenAPI
              </Button>
              <Button
                loading={downloadPending}
                onClick={() => void downloadOpenApi(openApiQuery.data!)}
              >
                下载完整 OpenAPI 文档
              </Button>
            </Space>
            {actionError && (
              <Alert showIcon type="warning" message={actionError} />
            )}
            <Card size="small" title="OpenAPI Operation JSON">
              <pre aria-label="OpenAPI Operation JSON" className="json-panel">
                {openApiJson(openApiQuery.data)}
              </pre>
            </Card>
          </Space>
        ) : (
          <Alert showIcon type="info" message="暂无 OpenAPI Fragment" />
        )
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
                  <JsonPanel title="Provider Service Identity" value={operation.providerServiceIdentity} />
                  <SchemaPanel title="Request Schema" schema={current.requestSchema} />
                  <SchemaPanel title="Response Schema" schema={current.responseSchema} />
                  <JsonPanel title="Error Schema" value={current.errorSchema} />
                  {current.descriptorSnapshot && (
                    <JsonPanel title="RPC Descriptor Snapshot" value={{
                      ...current.descriptorSnapshot,
                      base64DescriptorSet: '[按需下载，页面不展开]',
                    }} />
                  )}
                </Space>
              ),
            },
            ...(hasOpenApiSource ? [{
              key: 'openapi',
              label: 'OpenAPI',
              children: openApiPanel,
            }] : []),
          ]}
        />
      )}
      <Card title="Definition History" className="section-row">
        <Timeline
          items={definitions.map((definition) => ({
            children: `v${definition.definitionVersion} · ${definition.definitionSha256} · ${definition.createdAt}`,
          }))}
        />
      </Card>
    </section>
  )
}
