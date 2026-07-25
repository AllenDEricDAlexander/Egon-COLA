import { useQuery } from '@tanstack/react-query'
import { Card, Drawer, Form, Input, Select, Table, Typography } from 'antd'
import { useMemo, useState } from 'react'
import { gatewayApi } from '../../api/gatewayApi'
import type { AuditEntry } from '../../api/types'
import { JsonPanel } from '../../components/JsonPanel'
import { LoadingBlock, QueryFailure } from '../../components/QueryState'
import { StatusTag } from '../../components/StatusTag'
import { useScope } from '../../hooks/useScope'
import { useCapability } from '../../app/capabilities'
import { sanitizeForDisplay } from '../observability/sanitize'

export const AuditPage = () => {
  const { scope } = useScope()
  const canRead = useCapability('gateway:read')
  const [filters, setFilters] = useState<Record<string, string>>({})
  const [selected, setSelected] = useState<AuditEntry>()
  const search = useMemo(() => new URLSearchParams(filters), [filters])
  const query = useQuery({
    queryKey: ['audit', scope, search.toString()],
    queryFn: ({ signal }) => gatewayApi.audits(scope, search, signal),
    enabled: canRead,
  })
  if (!canRead) return <QueryFailure error={new Error('没有审计读取能力')} />
  return (
    <section>
      <Typography.Title level={2}>审计日志</Typography.Title>
      <Card>
        <Form
          layout="inline"
          onFinish={(values) =>
            setFilters(Object.fromEntries(
              Object.entries(values)
                .filter(([, value]) => value !== undefined && value !== '')
                .map(([key, value]) => [key, String(value)]),
            ))
          }
        >
          <Form.Item name="actorId" label="Actor"><Input allowClear /></Form.Item>
          <Form.Item name="resourceId" label="Resource ID"><Input allowClear /></Form.Item>
          <Form.Item name="traceId" label="Trace ID"><Input allowClear /></Form.Item>
          <Form.Item name="successful" label="结果"><Select allowClear style={{ width: 120 }} options={[{ value: 'true', label: '成功' }, { value: 'false', label: '失败' }]} /></Form.Item>
          <Form.Item><button type="submit" className="ant-btn ant-btn-primary">查询</button></Form.Item>
        </Form>
      </Card>
      {query.isLoading ? <LoadingBlock /> : query.error ? <QueryFailure error={query.error} /> : (
        <Table<AuditEntry>
          className="section-row"
          rowKey="id"
          dataSource={query.data?.items ?? []}
          onRow={(row) => ({ onClick: () => setSelected(row) })}
          scroll={{ x: 1100 }}
          columns={[
            { title: '时间', dataIndex: 'occurredAt' },
            { title: 'Actor', dataIndex: 'actorId' },
            { title: 'Action', dataIndex: 'action' },
            { title: 'Resource', render: (_, row) => `${row.resourceType} / ${row.resourceId}` },
            { title: 'Release', dataIndex: 'releaseId' },
            { title: 'Trace', dataIndex: 'traceId' },
            { title: '结果', render: (_, row) => <StatusTag status={row.successful ? 'SUCCESS' : 'FAILED'} /> },
            { title: '错误码', dataIndex: 'errorCode' },
          ]}
        />
      )}
      <Drawer title="审计详情" width={680} open={Boolean(selected)} onClose={() => setSelected(undefined)}>
        {selected && (
          <>
            <Typography.Paragraph>Revision：{selected.draftRevision ?? '-'}</Typography.Paragraph>
            <JsonPanel title="Before Summary" value={sanitizeForDisplay(selected.beforeSummary)} />
            <JsonPanel title="After Summary" value={sanitizeForDisplay(selected.afterSummary)} />
          </>
        )}
      </Drawer>
    </section>
  )
}
