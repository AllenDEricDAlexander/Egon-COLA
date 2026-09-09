import { useQuery } from '@tanstack/react-query'
import { Alert, Card, Descriptions, Space, Table, Typography } from 'antd'
import { gatewayApi } from '../../api/gatewayApi'
import { gatewayEngineRoleOf, type EngineNode } from '../../api/types'
import { LoadingBlock, QueryFailure } from '../../components/QueryState'
import { StatusTag } from '../../components/StatusTag'

export const McpRuntimeStatus = ({ gatewayGroupId }: { gatewayGroupId: string }) => {
  const consistency = useQuery({
    queryKey: ['runtime-consistency', gatewayGroupId],
    queryFn: ({ signal }) => gatewayApi.consistency(gatewayGroupId, signal),
    enabled: Boolean(gatewayGroupId),
  })
  const nodes = useQuery({
    queryKey: ['engine-nodes', gatewayGroupId],
    queryFn: ({ signal }) => gatewayApi.engineNodes(gatewayGroupId, signal),
    enabled: Boolean(gatewayGroupId),
  })
  if (consistency.isLoading || nodes.isLoading) return <LoadingBlock />
  if (consistency.error || nodes.error) {
    return <QueryFailure error={consistency.error ?? nodes.error} />
  }
  const replicas = (nodes.data ?? []).filter(node => gatewayEngineRoleOf(node.metadata) === 'MCP')
  const stateFor = (node: EngineNode) => consistency.data?.nodes?.find(state =>
    state.instanceId === node.instanceId && state.leaseId === node.leaseId)
  const online = replicas.filter(node => stateFor(node))
  const ready = online.filter(node => stateFor(node)?.status === 'CONSISTENT').length
  const stale = Boolean(consistency.data?.stale || nodes.data?.some(node => node.stale))
  const status = stale ? 'STALE' : !online.length ? 'NOT_READY'
    : ready === online.length ? 'CONSISTENT' : 'INCONSISTENT'

  return (
    <Space direction="vertical" size="large" className="full-width">
      <Typography.Title level={3}>MCP Engine</Typography.Title>
      {stale && <Alert type="warning" showIcon title="Tianshu 运行态投影已过期，当前显示最后已知状态。" />}
      {!online.length && <Alert type="warning" showIcon title="MCP Engine 不可用" />}
      <Card>
        <Descriptions>
          <Descriptions.Item label="统一 Release">{consistency.data?.targetReleaseId ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="MCP Ready Engines">{ready} / {online.length}</Descriptions.Item>
          <Descriptions.Item label="MCP 状态"><StatusTag status={status} /></Descriptions.Item>
          <Descriptions.Item label="统一一致性">
            <StatusTag status={stale ? 'STALE' : online.length && consistency.data?.consistent ? 'CONSISTENT' : 'INCONSISTENT'} />
          </Descriptions.Item>
        </Descriptions>
      </Card>
      <Table<EngineNode>
        rowKey={node => `${node.instanceId}:${node.leaseId}`}
        dataSource={replicas}
        pagination={false}
        scroll={{ x: 1000 }}
        columns={[
          { title: 'Engine', dataIndex: 'instanceId' },
          { title: 'Lease', dataIndex: 'leaseId' },
          { title: '状态', render: (_, node) => <StatusTag status={stale ? 'STALE' : stateFor(node)?.status ?? 'NOT_READY'} /> },
          { title: 'Release', render: (_, node) => stateFor(node)?.activeReleaseId ?? '-' },
          { title: 'Version', render: (_, node) => stateFor(node)?.activeRuleVersion ?? '-' },
          { title: 'Checksum', render: (_, node) => stateFor(node)?.activeRuleChecksum ?? '-' },
          { title: 'ACK', render: (_, node) => stateFor(node)?.lastApplyStatus ?? '-' },
          { title: 'Last ACK', render: (_, node) => stateFor(node)?.lastAckAt ?? '-' },
          { title: '原因', render: (_, node) => stateFor(node)?.reason ?? (stateFor(node) ? '-' : '未确认在线租约') },
          { title: '最后心跳', dataIndex: 'lastHeartbeatAt' },
          { title: '来源', render: () => consistency.data?.source },
        ]}
      />
    </Space>
  )
}
