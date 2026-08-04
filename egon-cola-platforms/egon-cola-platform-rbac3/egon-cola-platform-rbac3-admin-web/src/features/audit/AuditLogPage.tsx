import { useRbac3Session } from '@egon-cola/rbac3-react-sdk'
import { useQuery } from '@tanstack/react-query'
import { Button, Card, Col, Form, Input, Row, Select, Space, Table, Tag, Typography } from 'antd'
import { useState } from 'react'
import { useFeatureApi, useFeatureTenantContext } from '../shared/FeatureApi'
import { PageState } from '@egon-cola/admin-web-shared'
import { AuditDetailDrawer } from './AuditDetailDrawer'
import { auditApi, type AuditFilter, type AuditView } from './audit.api'

const makeInitialFilter = (): AuditFilter => {
  const now = new Date()
  const yesterday = new Date(now.getTime() - 24 * 60 * 60 * 1000)
  return { from: yesterday.toISOString(), to: now.toISOString(), limit: 50 }
}

export const AuditLogPage = () => {
  const { status } = useRbac3Session()
  const { effectiveTenantId } = useFeatureTenantContext()
  const api = auditApi(useFeatureApi())
  const [filter, setFilter] = useState<AuditFilter>(makeInitialFilter)
  const [selected, setSelected] = useState<AuditView | null>(null)
  const query = useQuery({
    queryKey: ['rbac3', 'audit', effectiveTenantId ?? 'none', filter],
    queryFn: () => api.list(filter),
    enabled: status === 'READY',
  })
  return (
    <Card title="授权审计">
      <Typography.Paragraph type="secondary">只使用服务端允许的精确过滤字段和签名游标；详情再次执行防御性脱敏。</Typography.Paragraph>
      <Form<AuditFilter> layout="vertical" initialValues={makeInitialFilter()} onFinish={(values) => setFilter({ ...values, limit: 50 })}>
        <Row gutter={12}>
          <Col span={6}><Form.Item name="from" label="From" rules={[{ required: true }]}><Input /></Form.Item></Col>
          <Col span={6}><Form.Item name="to" label="To" rules={[{ required: true }]}><Input /></Form.Item></Col>
          <Col span={4}><Form.Item name="actorId" label="Actor ID"><Input /></Form.Item></Col>
          <Col span={4}><Form.Item name="outcome" label="Outcome"><Select allowClear options={['ALLOW', 'DENY', 'SUCCESS', 'FAILED'].map((value) => ({ value }))} /></Form.Item></Col>
          <Col span={4}><Form.Item name="reasonCode" label="Reason Code"><Input /></Form.Item></Col>
        </Row>
        <Row gutter={12}>
          <Col span={8}><Form.Item name="eventType" label="Event Type"><Input /></Form.Item></Col>
          <Col span={8}><Form.Item name="traceId" label="Trace ID"><Input /></Form.Item></Col>
          <Col span={8}><Form.Item label="查询"><Button type="primary" htmlType="submit">应用服务端过滤</Button></Form.Item></Col>
        </Row>
      </Form>
      <PageState loading={query.isPending} error={query.error} empty={query.data?.items.length === 0}>
        <Table<AuditView>
          rowKey="id"
          dataSource={query.data?.items ?? []}
          pagination={false}
          columns={[
            { title: '时间', dataIndex: 'createdAt' },
            { title: '事件', dataIndex: 'eventType' },
            { title: 'Outcome', dataIndex: 'outcome', render: (value: string) => <Tag>{value}</Tag> },
            { title: 'Actor', render: (_value, row) => `${row.actorType}:${row.actorId}` },
            { title: 'Target', render: (_value, row) => `${row.targetType}:${row.targetId}` },
            { title: 'Reason', dataIndex: 'reasonCode' },
            { title: 'Trace', dataIndex: 'traceId' },
            { title: '操作', render: (_value, row) => <Button size="small" onClick={() => setSelected(row)}>查看详情</Button> },
          ]}
        />
        {query.data?.nextCursor && (
          <Space style={{ marginTop: 16 }}><Button onClick={() => setFilter((current) => ({ ...current, cursor: query.data?.nextCursor ?? undefined }))}>下一页</Button></Space>
        )}
      </PageState>
      <AuditDetailDrawer audit={selected} onClose={() => setSelected(null)} />
    </Card>
  )
}
