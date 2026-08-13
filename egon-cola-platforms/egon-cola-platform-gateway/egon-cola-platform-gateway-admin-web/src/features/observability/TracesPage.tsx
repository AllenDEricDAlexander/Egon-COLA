import { useQuery } from '@tanstack/react-query'
import { Card, Form, Input, Select, Table, Typography } from 'antd'
import { useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import { gatewayApi } from '../../api/gatewayApi'
import type { Scope, TraceSummary } from '../../api/types'
import { EmptyBlock, LoadingBlock, QueryFailure } from '../../components/QueryState'
import { StatusTag } from '../../components/StatusTag'
import { GatewayScopeFilter } from '../../components/GatewayScopeFilter'
import {
  hasRequiredScopeFields,
  readScopeSearchParams,
  writeScopeSearchParams,
} from '../../hooks/scopeSearchParams'

export const TracesPage = () => {
  const [searchParams, setSearchParams] = useSearchParams()
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
  const query = useQuery({
    queryKey: ['traces', requestScope, search.toString()],
    queryFn: ({ signal }) => gatewayApi.traces(requestScope!, search, signal),
    enabled: Boolean(requestScope),
    refetchInterval: requestScope ? 5_000 : false,
    refetchIntervalInBackground: true,
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
        query.isLoading ? <LoadingBlock /> : query.error ? <QueryFailure error={query.error} /> : (
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
        )
      )}
      <Typography.Paragraph type="secondary">
        页面只查询受控聚合与脱敏明细，不直接消费 Kafka，也不展示 Body、Credential、Cookie 或原始 Header。
      </Typography.Paragraph>
    </section>
  )
}
