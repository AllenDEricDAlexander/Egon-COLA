import { useQuery } from '@tanstack/react-query'
import { Alert, Card, Descriptions, Space, Tag, Timeline, Typography } from 'antd'
import { useParams } from 'react-router-dom'
import { gatewayApi } from '../../api/gatewayApi'
import { JsonPanel } from '../../components/JsonPanel'
import { LoadingBlock, QueryFailure } from '../../components/QueryState'
import { StatusTag } from '../../components/StatusTag'

export const OperationPage = () => {
  const { operationId = '' } = useParams()
  const query = useQuery({
    queryKey: ['operation', operationId],
    queryFn: ({ signal }) => gatewayApi.operation(operationId, signal),
    enabled: Boolean(operationId),
  })
  if (query.isLoading) return <LoadingBlock />
  if (query.error || !query.data) return <QueryFailure error={query.error} />
  const { operation, definitions } = query.data
  const current = definitions.find((definition) => definition.id === operation.currentDefinitionId)
    ?? definitions[0]
  return (
    <section>
      <Typography.Title level={2}>{operation.methodIdentity}</Typography.Title>
      {operation.sourceType === 'STARTER' && (
        <Alert
          showIcon
          type="info"
          message="STARTER 原始定义只读；管理元数据与 Route 独立维护。"
        />
      )}
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
        <Space direction="vertical" className="full-width section-row">
          <JsonPanel title="Provider Service Identity" value={operation.providerServiceIdentity} />
          <JsonPanel title="Request Schema" value={current.requestSchema} />
          <JsonPanel title="Response Schema" value={current.responseSchema} />
          <JsonPanel title="Error Schema" value={current.errorSchema} />
          {current.descriptorSnapshot && (
            <JsonPanel title="RPC Descriptor Snapshot" value={{
              ...current.descriptorSnapshot,
              base64DescriptorSet: '[按需下载，页面不展开]',
            }} />
          )}
        </Space>
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
