import { useQuery } from '@tanstack/react-query'
import { Alert, Card, Descriptions, Space, Table, Tabs, Typography } from 'antd'
import { Link, useParams } from 'react-router-dom'
import { gatewayApi } from '../../api/gatewayApi'
import { gatewayEngineRoleOf, type EngineNode, type GatewayEngineRole } from '../../api/types'
import { LoadingBlock, QueryFailure } from '../../components/QueryState'
import { StatusTag } from '../../components/StatusTag'

const roles: readonly { role: GatewayEngineRole; label: string }[] = [
  { role: 'API_RPC', label: 'API / RPC Engine' },
  { role: 'MCP', label: 'MCP Engine' },
]

export const GatewayGroupDetailPage = () => {
  const { groupId = '' } = useParams()
  const group = useQuery({
    queryKey: ['yuheng-group', groupId],
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

  const stateFor = (node: EngineNode) => consistency.data?.nodes?.find(state =>
    state.instanceId === node.instanceId && state.leaseId === node.leaseId)
  const stale = Boolean(consistency.data?.stale || nodes.data?.some(node => node.stale))
  const unknown = (nodes.data ?? []).filter(node => !gatewayEngineRoleOf(node.metadata))
  const rolesComplete = roles.every(({ role }) => nodes.data?.some(node =>
    gatewayEngineRoleOf(node.metadata) === role && stateFor(node)))
  const runtimeError = nodes.error ?? consistency.error
  const runtimeLoading = nodes.isLoading || consistency.isLoading

  const roleCard = ({ role, label }: typeof roles[number]) => {
    const replicas = (nodes.data ?? []).filter(node => gatewayEngineRoleOf(node.metadata) === role)
    // Only the backend projection decides which exact lease is online.
    const online = replicas.filter(node => stateFor(node))
    const ready = online.filter(node => stateFor(node)?.status === 'CONSISTENT').length
    const status = stale ? 'STALE' : !online.length ? 'NOT_READY'
      : ready === online.length ? 'CONSISTENT' : 'INCONSISTENT'
    return (
      <Card key={role} title={<Typography.Title level={3}>{label}</Typography.Title>}>
        <Space direction="vertical" className="full-width">
          {!online.length && <Alert type="warning" showIcon title={`缺少 ${label} 角色`} />}
          <Space><StatusTag status={status} /><Typography.Text>Ready {ready} / {online.length} · 全部副本 {replicas.length}</Typography.Text></Space>
          <Table<EngineNode>
            size="small"
            rowKey={node => `${node.instanceId}:${node.leaseId}`}
            dataSource={replicas}
            pagination={false}
            scroll={{ x: 1000 }}
            columns={[
              { title: 'Instance', dataIndex: 'instanceId' },
              { title: 'Lease', dataIndex: 'leaseId' },
              { title: '状态', render: (_, node) => <StatusTag status={stale ? 'STALE' : stateFor(node)?.status ?? 'NOT_READY'} /> },
              { title: 'Release', render: (_, node) => stateFor(node)?.activeReleaseId ?? '-' },
              { title: 'Version', render: (_, node) => stateFor(node)?.activeRuleVersion ?? '-' },
              { title: 'Checksum', render: (_, node) => stateFor(node)?.activeRuleChecksum ?? '-' },
              { title: 'ACK', render: (_, node) => stateFor(node)?.lastApplyStatus ?? '-' },
              { title: 'Last ACK', render: (_, node) => stateFor(node)?.lastAckAt ?? '-' },
              { title: '原因', render: (_, node) => stateFor(node)?.reason ?? (stateFor(node) ? '-' : '未确认在线租约') },
            ]}
          />
        </Space>
      </Card>
    )
  }

  return (
    <section>
      <Typography.Title level={2}>{group.data.displayName}</Typography.Title>
      <Tabs items={[
        {
          key: 'overview', label: 'Overview', children: (
            <Space direction="vertical" size="large" className="full-width">
              <Card>
                <Descriptions column={2}>
                  <Descriptions.Item label="Code">{group.data.gatewayGroupCode}</Descriptions.Item>
                  <Descriptions.Item label="作用域">{group.data.env} / {group.data.namespace}</Descriptions.Item>
                  <Descriptions.Item label="状态"><StatusTag status={group.data.enabled ? 'ACTIVE' : 'DISABLED'} /></Descriptions.Item>
                  <Descriptions.Item label="Revision">{group.data.revision}</Descriptions.Item>
                </Descriptions>
              </Card>
              {runtimeError ? <QueryFailure error={runtimeError} /> : runtimeLoading ? <LoadingBlock /> : (
                <>
                  <Card title="Runtime Consistency">
                    <Descriptions>
                      <Descriptions.Item label="一致性">
                        <StatusTag status={stale ? 'STALE' : rolesComplete && consistency.data?.consistent ? 'CONSISTENT' : 'INCONSISTENT'} />
                      </Descriptions.Item>
                      <Descriptions.Item label="Ready">{consistency.data?.readyNodes ?? 0} / {consistency.data?.totalNodes ?? 0}</Descriptions.Item>
                      <Descriptions.Item label="目标 Release">{consistency.data?.targetReleaseId ?? '-'}</Descriptions.Item>
                      <Descriptions.Item label="Release 状态">{consistency.data?.targetReleaseStatus ?? '-'}</Descriptions.Item>
                      <Descriptions.Item label="观测">{consistency.data?.observedAt ?? '-'}</Descriptions.Item>
                      <Descriptions.Item label="数据状态"><StatusTag status={stale ? 'STALE' : 'FRESH'} /></Descriptions.Item>
                    </Descriptions>
                  </Card>
                  {stale && <Alert type="warning" showIcon title="Tianshu 运行态投影已过期，当前显示最后已知状态。" />}
                  {unknown.length > 0 && <Alert type="warning" showIcon title={`未知 Engine 角色（${unknown.length}）`}
                    description={unknown.map(node => `${node.instanceId}: ${node.metadata?.['yuheng.engine.role'] || 'ROLE_MISSING'}`).join('；')} />}
                  {roles.map(roleCard)}
                </>
              )}
            </Space>
          ),
        },
        {
          key: 'nodes', label: 'Engine Nodes', children: nodes.error ? <QueryFailure error={nodes.error} /> : nodes.isLoading ? <LoadingBlock /> : (
            <Table<EngineNode>
              rowKey={node => `${node.instanceId}:${node.leaseId}`}
              dataSource={nodes.data ?? []} scroll={{ x: 1200 }}
              columns={[
                { title: 'Instance ID', dataIndex: 'instanceId' },
                { title: 'Lease ID', dataIndex: 'leaseId' },
                { title: 'Engine Role', render: (_, node) => gatewayEngineRoleOf(node.metadata) ?? 'UNKNOWN' },
                { title: 'Lease Role', dataIndex: 'leaseRole' },
                { title: 'Observed At', dataIndex: 'observedAt' },
                { title: 'Stale', render: (_, node) => <StatusTag status={node.stale ? 'STALE' : 'FRESH'} /> },
                { title: 'Status', render: (_, node) => <StatusTag status={node.status} /> },
                { title: 'Host:Port', render: (_, node) => `${node.host ?? '-'}:${node.port ?? '-'}` },
                { title: 'Last Heartbeat', dataIndex: 'lastHeartbeatAt' },
                { title: 'Expire At', dataIndex: 'expireAt' },
              ]}
            />
          ),
        },
        {
          key: 'actions', label: '工作台', children: (
            <Space>
              <Link to={`/yuheng-groups/${groupId}/draft/routes`}>Draft Routes</Link>
              <Link to={`/yuheng-groups/${groupId}/draft/policies`}>Draft Policies</Link>
              <Link to={`/yuheng-groups/${groupId}/releases`}>Releases</Link>
            </Space>
          ),
        },
      ]} />
    </section>
  )
}
