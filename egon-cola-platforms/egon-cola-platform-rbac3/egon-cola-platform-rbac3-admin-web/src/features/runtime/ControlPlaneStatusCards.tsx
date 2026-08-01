import { Card, Col, Descriptions, Row, Space, Tag, Typography } from 'antd'
import type { ControlPlaneRuntimeStatus } from './runtime.api'

const statusColor = (status: string) => ['ACCEPTED', 'ACTIVE', 'ROUTABLE', 'UP_TO_DATE', 'HEALTHY'].includes(status) ? 'green' : ['MISSING', 'FAILED', 'STALE', 'LAGGING', 'DEGRADED'].includes(status) ? 'red' : 'orange'

export const ControlPlaneStatusCards = ({ status }: { readonly status: ControlPlaneRuntimeStatus }) => (
  <Space direction="vertical" size="middle" style={{ width: '100%' }}>
    <Row gutter={16}>
      <Col span={8}><StatusCard title="Gateway Definition" status={status.definition.status} detail={status.definition.definitionSetId ?? '-'} /></Col>
      <Col span={8}><StatusCard title="DDC HTTP Provider Lease" status={status.providerLease.state} detail={`${status.providerLease.instanceId ?? '-'} · ${status.providerLease.leaseExpireAt ?? '无有效租约'}`} /></Col>
      <Col span={8}><StatusCard title="Gateway Release" status={status.gatewayRelease.status} detail={`${status.gatewayRelease.releaseId ?? '-'} · ${status.gatewayRelease.observedByEngineVersion ?? '未观测'}`} /></Col>
    </Row>
    <Row gutter={16}>
      <Col span={6}><StatusCard title="RBAC3 Flyway History" status={status.flyway?.rbac3History ?? 'UNKNOWN'} /></Col>
      <Col span={6}><StatusCard title="Outbox Flyway History" status={status.flyway?.outboxHistory ?? 'UNKNOWN'} /></Col>
      <Col span={6}><StatusCard title="Redis Projection" status={status.redisProjection?.state ?? 'UNKNOWN'} detail={`Checkpoint Lag ${status.redisProjection?.checkpointLag ?? '-'}`} /></Col>
      <Col span={6}><StatusCard title="Outbox" status={status.outbox?.state ?? 'UNKNOWN'} detail={`Pending ${status.outbox?.pendingCount ?? '-'} · Oldest ${status.outbox?.oldestAgeSeconds ?? '-'}s`} /></Col>
    </Row>
    <Descriptions bordered size="small" column={2}>
      <Descriptions.Item label="Fence / Mutation"><Tag color={statusColor(status.fence?.state ?? 'UNKNOWN')}>{status.fence?.state ?? 'UNKNOWN'}</Tag></Descriptions.Item>
      <Descriptions.Item label="Oldest Fence Age">{status.fence?.oldestAgeSeconds ?? '-'}s</Descriptions.Item>
      <Descriptions.Item label="Checked At" span={2}>{status.checkedAt}</Descriptions.Item>
    </Descriptions>
  </Space>
)

const StatusCard = ({ title, status, detail }: { readonly title: string; readonly status: string; readonly detail?: string }) => (
  <Card size="small" title={title}>
    <Space direction="vertical">
      <Tag color={statusColor(status)}>{status}</Tag>
      {detail && <Typography.Text type="secondary">{detail}</Typography.Text>}
    </Space>
  </Card>
)
