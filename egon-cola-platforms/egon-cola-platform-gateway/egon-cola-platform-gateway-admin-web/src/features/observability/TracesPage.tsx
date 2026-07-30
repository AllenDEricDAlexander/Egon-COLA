import { useQuery } from '@tanstack/react-query'
import { Card, Form, Input, Select, Table, Typography } from 'antd'
import { useMemo, useState } from 'react'
import { gatewayApi } from '../../api/gatewayApi'
import type { TraceSummary } from '../../api/types'
import { LoadingBlock, QueryFailure } from '../../components/QueryState'
import { StatusTag } from '../../components/StatusTag'
import { useScope } from '../../hooks/useScope'

export const TracesPage = () => {
  const { scope } = useScope()
  const [filters, setFilters] = useState<Record<string, string>>({})
  const search = useMemo(() => new URLSearchParams(filters), [filters])
  const query = useQuery({
    queryKey: ['traces', scope, search.toString()],
    queryFn: ({ signal }) => gatewayApi.traces(scope, search, signal),
    refetchInterval: 5_000,
    refetchIntervalInBackground: true,
  })
  return (
    <section>
      <Typography.Title level={2}>调用观测</Typography.Title>
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
          <Form.Item name="traceId" label="Trace ID"><Input allowClear /></Form.Item>
          <Form.Item name="protocol" label="Protocol"><Select allowClear style={{ width: 130 }} options={['HTTP', 'RPC'].map((value) => ({ value }))} /></Form.Item>
          <Form.Item name="statusCategory" label="状态"><Select allowClear style={{ width: 150 }} options={['SUCCESS', 'CLIENT_ERROR', 'SERVER_ERROR', 'TIMEOUT'].map((value) => ({ value }))} /></Form.Item>
          <Form.Item><button type="submit" className="ant-btn ant-btn-primary">查询</button></Form.Item>
        </Form>
      </Card>
      {query.isLoading ? <LoadingBlock /> : query.error ? <QueryFailure error={query.error} /> : (
        <Table<TraceSummary>
          className="section-row"
          rowKey="traceId"
          dataSource={query.data?.items ?? []}
          pagination={{ current: query.data?.page, pageSize: query.data?.size, total: query.data?.total }}
          scroll={{ x: 1200 }}
          columns={[
            { title: 'Trace ID', dataIndex: 'traceId', render: (value) => <Typography.Text copyable>{value}</Typography.Text> },
            { title: '开始时间', dataIndex: 'startedAt' },
            { title: '耗时(ms)', dataIndex: 'durationMs' },
            { title: 'Protocol', dataIndex: 'protocol' },
            { title: 'Operation', dataIndex: 'operationKey' },
            { title: '状态', dataIndex: 'statusCategory', render: (value) => <StatusTag status={value} /> },
            { title: 'Engine', dataIndex: 'engineInstanceId' },
            { title: 'Provider', dataIndex: 'providerService' },
          ]}
        />
      )}
      <Typography.Paragraph type="secondary">
        页面只查询受控聚合与脱敏明细，不直接消费 Kafka，也不展示 Body、Credential、Cookie 或原始 Header。
      </Typography.Paragraph>
    </section>
  )
}
