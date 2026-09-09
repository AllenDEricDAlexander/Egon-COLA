import { Descriptions, Drawer, Space, Tag, Typography } from 'antd'
import type { AuditView } from './audit.api'

const sensitiveKey = /password|secret|token|hash|private.?key|credential/i

export const redactAuditSnapshot = (value: unknown, key = ''): unknown => {
  if (sensitiveKey.test(key)) return '[REDACTED]'
  if (Array.isArray(value)) return value.map((item) => redactAuditSnapshot(item))
  if (typeof value === 'object' && value !== null) {
    return Object.fromEntries(Object.entries(value).map(([childKey, child]) => [childKey, redactAuditSnapshot(child, childKey)]))
  }
  return value
}

export interface AuditDetailDrawerProps {
  readonly audit: AuditView | null
  readonly onClose: () => void
}

const Snapshot = ({ title, value }: { readonly title: string; readonly value: Readonly<Record<string, unknown>> }) => (
  <div>
    <Typography.Title level={5}>{title}</Typography.Title>
    <Descriptions bordered size="small" column={1}>
      {Object.entries(redactAuditSnapshot(value) as Record<string, unknown>).map(([key, item]) => (
        <Descriptions.Item key={key} label={key}>{typeof item === 'object' ? JSON.stringify(item) : String(item)}</Descriptions.Item>
      ))}
    </Descriptions>
  </div>
)

export const AuditDetailDrawer = ({ audit, onClose }: AuditDetailDrawerProps) => (
  <Drawer title="审计详情" open={audit !== null} width={720} onClose={onClose} destroyOnHidden>
    {audit && (
      <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
        <Descriptions bordered column={2}>
          <Descriptions.Item label="Event"><Tag>{audit.eventType}</Tag></Descriptions.Item>
          <Descriptions.Item label="Outcome"><Tag>{audit.outcome}</Tag></Descriptions.Item>
          <Descriptions.Item label="Reason">{audit.reasonCode}</Descriptions.Item>
          <Descriptions.Item label="Created At">{audit.createdAt}</Descriptions.Item>
          <Descriptions.Item label="Request ID">{audit.requestId}</Descriptions.Item>
          <Descriptions.Item label="Trace ID">{audit.traceId}</Descriptions.Item>
          <Descriptions.Item label="Payload Checksum" span={2}>{audit.payloadChecksum}</Descriptions.Item>
        </Descriptions>
        <Snapshot title="Before（结构化且脱敏）" value={audit.beforeSnapshot} />
        <Snapshot title="After（结构化且脱敏）" value={audit.afterSnapshot} />
      </Space>
    )}
  </Drawer>
)
