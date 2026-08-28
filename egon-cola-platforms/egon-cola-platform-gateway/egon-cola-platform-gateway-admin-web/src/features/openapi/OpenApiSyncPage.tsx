import {useQuery} from '@tanstack/react-query'
import {Alert, Button, Card, Descriptions, Drawer, Space, Table, Tag, Typography} from 'antd'
import {useState} from 'react'
import {useSearchParams} from 'react-router-dom'
import {gatewayApi} from '../../api/gatewayApi'
import type {GatewayOpenApiSyncState, Scope} from '../../api/types'
import {GatewayScopeFilter} from '../../components/GatewayScopeFilter'
import {EmptyBlock, LoadingBlock, QueryFailure} from '../../components/QueryState'
import {readScopeSearchParams, writeScopeSearchParams} from '../../hooks/scopeSearchParams'
import {JsonPanel} from '../../components/JsonPanel'

const statusColor = (status: string): string => {
  if (status === 'VALID') return 'success'
  if (['INVALID', 'INCONSISTENT_BUILD', 'INGEST_FAILED', 'FETCH_FAILED'].includes(status)) return 'error'
  if (['STALE', 'FETCHING', 'VALIDATING', 'INGESTING'].includes(status)) return 'processing'
  return 'default'
}

export const OpenApiSyncPage = () => {
  const [searchParams, setSearchParams] = useSearchParams()
  const [snapshotId, setSnapshotId] = useState<string | null>(null)
  const scope = readScopeSearchParams(searchParams, ['bizCode', 'namespace', 'env', 'appCode'])
  const states = useQuery({
    queryKey: ['openapi-sync-page', scope],
    queryFn: ({signal}) => gatewayApi.openapiSyncStates(scope, signal),
  })
  const document = useQuery({
    queryKey: ['openapi-snapshot-document', snapshotId],
    queryFn: ({signal}) => gatewayApi.openapiSnapshotDocument(snapshotId!, signal),
    enabled: snapshotId !== null,
  })

  const updateScope = (value: Partial<Scope>) => {
    setSearchParams(writeScopeSearchParams(searchParams, value, ['bizCode', 'namespace', 'env', 'appCode']))
  }

  return (
    <section>
      <Typography.Title level={2}>OpenAPI 同步</Typography.Title>
      <Typography.Paragraph type="secondary">
        查看各应用构建、OpenAPI Group 与快照的一致性；状态异常时保留最后一次错误和重试时间，文档仅通过受控快照接口查看。
      </Typography.Paragraph>
      <GatewayScopeFilter
        fields={['bizCode', 'namespace', 'env', 'appCode']}
        value={scope}
        onChange={updateScope}
      />
      <Card className="section-row" title="同步状态">
        {states.isLoading ? <LoadingBlock />
          : states.error ? <QueryFailure error={states.error} retry={() => void states.refetch()} />
            : states.data?.length ? (
              <Table<GatewayOpenApiSyncState>
                rowKey="id"
                dataSource={states.data}
                scroll={{x: 1200}}
                pagination={{pageSize: 20, showSizeChanger: true}}
                columns={[
                  {title: 'Application', dataIndex: 'applicationId'},
                  {title: 'Build', dataIndex: 'buildId'},
                  {title: 'Artifact', dataIndex: 'artifactVersion'},
                  {title: 'OpenAPI Group', dataIndex: 'openapiGroup'},
                  {title: 'Source', dataIndex: 'sourceType'},
                  {title: '状态', dataIndex: 'status', render: (value: string) => <Tag color={statusColor(value)}>{value}</Tag>},
                  {title: 'Operations', dataIndex: 'operationCount', render: (value: number | null) => value ?? '—'},
                  {title: 'Schemas', dataIndex: 'schemaCount', render: (value: number | null) => value ?? '—'},
                  {title: '最后成功', dataIndex: 'lastSuccessAt', render: (value: string | null) => value ?? '—'},
                  {title: '下次重试', dataIndex: 'nextRetryAt', render: (value: string | null) => value ?? '—'},
                  {
                    title: '快照',
                    render: (_value: unknown, state: GatewayOpenApiSyncState) => state.snapshotId
                      ? <Button size="small" onClick={() => setSnapshotId(state.snapshotId)}>查看文档</Button>
                      : <Typography.Text type="secondary">未生成</Typography.Text>,
                  },
                ]}
              />
            ) : <EmptyBlock description="当前 Scope 没有 OpenAPI 同步记录" />}
      </Card>
      {states.data?.some((state) => state.lastErrorCode) && (
        <Alert
          className="section-row"
          type="warning"
          showIcon
          message="存在同步失败记录"
          description="请结合错误码、构建号和快照状态处理，不要将未完成的 Group 发布为聚合 Definition Set。"
        />
      )}
      <Drawer
        title={document.data ? `OpenAPI 快照 · ${document.data.snapshotId}` : 'OpenAPI 快照文档'}
        open={snapshotId !== null}
        onClose={() => setSnapshotId(null)}
        size="large"
      >
        {document.isLoading ? <LoadingBlock />
          : document.error ? <QueryFailure error={document.error} retry={() => void document.refetch()} />
            : document.data ? (
              <Space direction="vertical" className="full-width">
                <Descriptions bordered column={2} size="small">
                  <Descriptions.Item label="Application">{document.data.applicationId}</Descriptions.Item>
                  <Descriptions.Item label="Build">{document.data.buildId}</Descriptions.Item>
                  <Descriptions.Item label="OpenAPI 版本">{document.data.openapiVersion}</Descriptions.Item>
                  <Descriptions.Item label="文档 SHA-256">{document.data.documentSha256}</Descriptions.Item>
                  <Descriptions.Item label="Canonical SHA-256">{document.data.canonicalSha256}</Descriptions.Item>
                  <Descriptions.Item label="校验时间">{document.data.validatedAt}</Descriptions.Item>
                </Descriptions>
                <JsonPanel title="OpenAPI Document" value={document.data.document} />
              </Space>
            ) : <EmptyBlock description="快照文档不存在" />}
      </Drawer>
    </section>
  )
}
