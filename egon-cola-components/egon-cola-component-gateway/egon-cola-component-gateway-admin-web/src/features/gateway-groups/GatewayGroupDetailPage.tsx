import { useQuery } from '@tanstack/react-query'
import { Card, Descriptions, Space, Table, Tabs, Typography } from 'antd'
import { Link, useParams } from 'react-router-dom'
import { gatewayApi } from '../../api/gatewayApi'
import { LoadingBlock, QueryFailure } from '../../components/QueryState'
import { StatusTag } from '../../components/StatusTag'

export const GatewayGroupDetailPage = () => {
  const { groupId = '' } = useParams()
  const group = useQuery({
    queryKey: ['gateway-group', groupId],
    queryFn: ({ signal }) => gatewayApi.group(groupId, signal),
    enabled: Boolean(groupId),
  })
  const nodes = useQuery({
    queryKey: ['engine-nodes', groupId],
    queryFn: ({ signal }) => gatewayApi.engineNodes(groupId, signal),
    enabled: Boolean(groupId),
    refetchInterval: 10_000,
  })
  const consistency = useQuery({
    queryKey: ['runtime-consistency', groupId],
    queryFn: ({ signal }) => gatewayApi.consistency(groupId, signal),
    enabled: Boolean(groupId),
    refetchInterval: 10_000,
  })
  if (group.isLoading) return <LoadingBlock />
  if (group.error || !group.data) return <QueryFailure error={group.error} />
  return (
    <section>
      <Typography.Title level={2}>{group.data.displayName}</Typography.Title>
      <Tabs
        items={[
          {
            key: 'overview',
            label: 'Overview',
            children: (
              <Space direction="vertical" size="large" className="full-width">
                <Card>
                  <Descriptions column={2}>
                    <Descriptions.Item label="Code">{group.data.gatewayGroupCode}</Descriptions.Item>
                    <Descriptions.Item label="作用域">{group.data.env} / {group.data.namespace}</Descriptions.Item>
                    <Descriptions.Item label="状态"><StatusTag status={group.data.enabled ? 'ACTIVE' : 'DISABLED'} /></Descriptions.Item>
                    <Descriptions.Item label="Revision">{group.data.revision}</Descriptions.Item>
                  </Descriptions>
                </Card>
                <Card title="Runtime Consistency">
                  {consistency.error ? <QueryFailure error={consistency.error} /> : consistency.data && (
                    <Descriptions>
                      <Descriptions.Item label="一致性">
                        <StatusTag status={consistency.data.consistent ? 'CONSISTENT' : 'INCONSISTENT'} />
                      </Descriptions.Item>
                      <Descriptions.Item label="Ready">{consistency.data.readyNodes} / {consistency.data.totalNodes}</Descriptions.Item>
                      <Descriptions.Item label="目标 Release">{consistency.data.desiredReleaseId ?? '-'}</Descriptions.Item>
                      <Descriptions.Item label="观测">{consistency.data.observedAt}</Descriptions.Item>
                      <Descriptions.Item label="数据状态"><StatusTag status={consistency.data.stale ? 'STALE' : 'FRESH'} /></Descriptions.Item>
                    </Descriptions>
                  )}
                </Card>
              </Space>
            ),
          },
          {
            key: 'nodes',
            label: 'Engine Nodes',
            children: nodes.error ? <QueryFailure error={nodes.error} /> : (
              <Table
                rowKey={(record) => `${record.instanceId}:${record.leaseId}`}
                dataSource={nodes.data ?? []}
                scroll={{ x: 1200 }}
                columns={[
                  { title: 'Instance ID', dataIndex: 'instanceId' },
                  { title: 'Lease ID', dataIndex: 'leaseId' },
                  { title: 'Observed At', dataIndex: 'observedAt' },
                  { title: 'Stale', render: (_, row) => <StatusTag status={row.stale ? 'STALE' : 'FRESH'} /> },
                  { title: 'Capabilities', render: (_, row) => row.capabilities?.join(', ') ?? '-' },
                  { title: 'Active Release', dataIndex: 'activeReleaseId' },
                  { title: 'Last ACK', dataIndex: 'lastAck' },
                ]}
              />
            ),
          },
          {
            key: 'actions',
            label: '工作台',
            children: (
              <Space>
                <Link to={`/gateway-groups/${groupId}/draft/routes`}>Draft Routes</Link>
                <Link to={`/gateway-groups/${groupId}/draft/policies`}>Draft Policies</Link>
                <Link to={`/gateway-groups/${groupId}/releases`}>Releases</Link>
              </Space>
            ),
          },
        ]}
      />
    </section>
  )
}
