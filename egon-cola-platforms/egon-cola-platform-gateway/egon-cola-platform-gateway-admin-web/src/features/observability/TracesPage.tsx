import { useQuery } from '@tanstack/react-query'
import { Alert, Button, Card, Descriptions, Drawer, Form, Input, Select, Space, Table, Typography } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { GatewayApiError } from '../../api/client'
import { gatewayApi } from '../../api/gatewayApi'
import type { Scope, TraceDetail, TraceSummary } from '../../api/types'
import { EmptyBlock, LoadingBlock, QueryFailure } from '../../components/QueryState'
import { StatusTag } from '../../components/StatusTag'
import { GatewayScopeFilter } from '../../components/GatewayScopeFilter'
import {
  hasRequiredScopeFields,
  readScopeSearchParams,
  writeScopeSearchParams,
} from '../../hooks/scopeSearchParams'
import { sanitizeForDisplay } from './sanitize'

const TraceDetailContent = ({ detail }: { detail: TraceDetail }) => (
  <Space direction="vertical" className="full-width">
    <Descriptions column={2}>
      <Descriptions.Item label="Trace ID">{detail.traceId}</Descriptions.Item>
      <Descriptions.Item label="开始时间">{detail.startedAt ?? '—'}</Descriptions.Item>
      <Descriptions.Item label="结束时间">{detail.finishedAt ?? '—'}</Descriptions.Item>
      <Descriptions.Item label="Attempt 数量">{detail.attempts.length}</Descriptions.Item>
    </Descriptions>
    <Table
      rowKey="attemptId"
      pagination={false}
      dataSource={detail.attempts}
      columns={[
        { title: 'Attempt ID', dataIndex: 'attemptId' },
        { title: '状态', dataIndex: 'status', render: (value: string) => <StatusTag status={value} /> },
        { title: '耗时(ms)', dataIndex: 'durationMs', render: (value?: number) => value ?? '—' },
        { title: '错误码', dataIndex: 'errorCode', render: (value?: string) => value ?? '—' },
      ]}
    />
    <Card size="small" title="脱敏属性">
      <pre className="json-panel">{JSON.stringify(sanitizeForDisplay(detail.redactedAttributes), null, 2)}</pre>
    </Card>
  </Space>
)

export const TracesPage = () => {
  const [searchParams, setSearchParams] = useSearchParams()
  const [selectedTraceId, setSelectedTraceId] = useState<string | null>(null)
  const [visible, setVisible] = useState(document.visibilityState === 'visible')
  const scope = readScopeSearchParams(searchParams, ['env', 'namespace'])
  const requestScope = hasRequiredScopeFields(scope, ['env', 'namespace'])
    ? scope as Pick<Scope, 'env' | 'namespace'>
    : undefined
  const search = useMemo(() => {
    const next = new URLSearchParams(searchParams)
    next.delete('env')
    next.delete('namespace')
    return next
  }, [searchParams])
  useEffect(() => {
    const listener = () => setVisible(document.visibilityState === 'visible')
    document.addEventListener('visibilitychange', listener)
    return () => document.removeEventListener('visibilitychange', listener)
  }, [])
  const query = useQuery({
    queryKey: ['traces', requestScope, search.toString()],
    queryFn: ({ signal }) => gatewayApi.traces(requestScope!, search, signal),
    enabled: Boolean(requestScope),
    retry: false,
    refetchInterval: requestScope && visible ? 5_000 : false,
    refetchIntervalInBackground: false,
  })
  const detailQuery = useQuery({
    queryKey: ['trace-detail', selectedTraceId],
    queryFn: ({ signal }) => gatewayApi.traceDetail(selectedTraceId!, signal),
    enabled: selectedTraceId !== null,
    retry: false,
  })
  const updateScope = (value: Partial<Scope>) => {
    const next = writeScopeSearchParams(searchParams, value, ['env', 'namespace'])
    next.set('page', '1')
    setSearchParams(next)
  }
  const submit = (values: Record<string, unknown>) => {
    const next = new URLSearchParams(searchParams)
    ;['traceId', 'protocol', 'statusCategory'].forEach((key) => {
      const value = values[key]
      if (value === undefined || value === '') next.delete(key)
      else next.set(key, String(value))
    })
    next.set('page', '1')
    setSearchParams(next)
  }

  return (
    <section>
      <Typography.Title level={2}>调用观测</Typography.Title>
      <GatewayScopeFilter
        fields={['env', 'namespace']}
        value={scope}
        required
        onChange={updateScope}
      />
      <Card>
        <Form
          layout="inline"
          initialValues={Object.fromEntries(search)}
          onFinish={submit}
        >
          <Form.Item name="traceId" label="Trace ID"><Input allowClear /></Form.Item>
          <Form.Item name="protocol" label="Protocol">
            <Select allowClear style={{ width: 130 }} options={['HTTP', 'RPC'].map((value) => ({ value }))} />
          </Form.Item>
          <Form.Item name="statusCategory" label="状态">
            <Select allowClear style={{ width: 150 }} options={['SUCCESS', 'CLIENT_ERROR', 'SERVER_ERROR', 'TIMEOUT'].map((value) => ({ value }))} />
          </Form.Item>
          <Form.Item><button type="submit" className="ant-btn ant-btn-primary">查询</button></Form.Item>
        </Form>
      </Card>
      {!requestScope ? <EmptyBlock description="请选择 Env 和 Namespace 查询范围" /> : (
        query.isLoading && !query.data ? <LoadingBlock /> : query.error && !query.data ? <QueryFailure error={query.error} retry={() => void query.refetch()} /> : (
          <>
            {query.error && query.data && (
              <Alert
                className="section-row"
                type="warning"
                showIcon
                message="刷新失败，当前显示最近一次成功结果"
                action={<Button onClick={() => void query.refetch()}>重试</Button>}
              />
            )}
            <Table<TraceSummary>
              className="section-row"
              rowKey="traceId"
              dataSource={query.data?.items ?? []}
              pagination={{ current: query.data?.page, pageSize: query.data?.size, total: query.data?.total }}
              onChange={(pagination) => {
                const next = new URLSearchParams(searchParams)
                next.set('page', String(pagination.current ?? 1))
                setSearchParams(next)
              }}
              scroll={{ x: 1280 }}
              columns={[
                { title: 'Trace ID', dataIndex: 'traceId', render: (value) => <Typography.Text copyable>{value}</Typography.Text> },
                { title: '开始时间', dataIndex: 'startedAt' },
                { title: '耗时(ms)', dataIndex: 'durationMs' },
                { title: 'Protocol', dataIndex: 'protocol' },
                { title: 'Operation', dataIndex: 'operationKey' },
                { title: '状态', dataIndex: 'statusCategory', render: (value) => <StatusTag status={value} /> },
                { title: 'Engine', dataIndex: 'engineInstanceId' },
                { title: 'Provider', dataIndex: 'providerService' },
                { title: '操作', render: (_, row) => <Button size="small" onClick={() => setSelectedTraceId(row.traceId)}>查看详情</Button> },
              ]}
            />
          </>
        )
      )}
      <Drawer
        title={selectedTraceId ? `Trace 详情 · ${selectedTraceId}` : 'Trace 详情'}
        open={selectedTraceId !== null}
        onClose={() => setSelectedTraceId(null)}
        size="large"
      >
        {detailQuery.isLoading ? <LoadingBlock />
          : detailQuery.error instanceof GatewayApiError && detailQuery.error.status === 404
            ? <Alert type="info" showIcon message="Trace 详情接口待补齐" description="当前后端仅提供 Trace 汇总查询；页面不会把缺失详情伪装成空数据。" />
            : detailQuery.error ? <QueryFailure error={detailQuery.error} retry={() => void detailQuery.refetch()} />
              : detailQuery.data ? <TraceDetailContent detail={detailQuery.data} />
                : <EmptyBlock description="暂无 Trace 详情" />}
      </Drawer>
      <Typography.Paragraph type="secondary">
        页面只查询受控聚合与脱敏明细，不直接消费 Kafka，也不展示 Body、Credential、Cookie 或原始 Header。
      </Typography.Paragraph>
    </section>
  )
}
