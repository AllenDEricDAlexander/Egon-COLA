import { useQuery } from '@tanstack/react-query'
import { Alert, Card, Descriptions, Table, Tag } from 'antd'
import { gatewayApi } from '../../api/gatewayApi'
import type { EngineNode } from '../../api/types'
import { LoadingBlock, QueryFailure } from '../../components/QueryState'

export const McpRuntimeStatus = ({ gatewayGroupId }: { gatewayGroupId: string }) => {
  const consistency = useQuery({
    queryKey: ['runtime-consistency', gatewayGroupId],
    queryFn: ({ signal }) => gatewayApi.consistency(gatewayGroupId, signal),
  })
  const nodes = useQuery({
    queryKey: ['engine-nodes', gatewayGroupId],
    queryFn: ({ signal }) => gatewayApi.engineNodes(gatewayGroupId, signal),
  })
  if (consistency.isLoading || nodes.isLoading) return <LoadingBlock />
  if (consistency.error || nodes.error) {
    return <QueryFailure error={consistency.error ?? nodes.error} />
  }
  return (
    <section>
      {consistency.data?.stale && (
        <Alert type="warning" showIcon title="DDC 运行态投影已过期，当前显示最后已知状态。" />
      )}
      <Card>
        <Descriptions>
          <Descriptions.Item label="统一 Release">{consistency.data?.targetReleaseId ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="Ready Engines">
            {consistency.data?.readyNodes} / {consistency.data?.totalNodes}
          </Descriptions.Item>
          <Descriptions.Item label="一致性">
            <Tag color={consistency.data?.consistent ? 'green' : 'red'}>
              {consistency.data?.consistent ? 'CONSISTENT' : 'INCONSISTENT'}
            </Tag>
          </Descriptions.Item>
        </Descriptions>
      </Card>
      <Table<EngineNode>
        rowKey={(node) => `${node.instanceId}:${node.leaseId}`}
        dataSource={nodes.data ?? []}
        columns={[
          { title: 'Engine', dataIndex: 'instanceId' },
          { title: 'Lease', dataIndex: 'leaseId' },
          { title: '状态', dataIndex: 'status' },
          { title: '最后心跳', dataIndex: 'lastHeartbeatAt' },
          { title: '来源', render: () => consistency.data?.source },
        ]}
      />
    </section>
  )
}
