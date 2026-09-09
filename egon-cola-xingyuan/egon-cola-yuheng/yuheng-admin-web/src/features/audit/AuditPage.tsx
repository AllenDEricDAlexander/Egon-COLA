import { useQuery } from '@tanstack/react-query'
import { Card, Drawer, Form, Input, Select, Table, Typography } from 'antd'
import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { gatewayApi } from '../../api/gatewayApi'
import type { AuditEntry, Scope } from '../../api/types'
import { EmptyBlock, LoadingBlock, QueryFailure } from '../../components/QueryState'
import { StatusTag } from '../../components/StatusTag'
import { GatewayScopeFilter } from '../../components/GatewayScopeFilter'
import {
  hasRequiredScopeFields,
  readScopeSearchParams,
  writeScopeSearchParams,
} from '../../hooks/scopeSearchParams'
import { useCapability } from '../../app/capabilities'
import { JsonPanel } from '../../components/JsonPanel'
import { sanitizeForDisplay } from '../observability/sanitize'

export const AuditPage = () => {
  const [searchParams, setSearchParams] = useSearchParams()
  const scope = readScopeSearchParams(searchParams, ['env', 'namespace'])
  const requestScope = hasRequiredScopeFields(scope, ['env', 'namespace'])
    ? scope as Pick<Scope, 'env' | 'namespace'>
    : undefined
  const canRead = useCapability('gateway:read')
  const [selected, setSelected] = useState<AuditEntry>()
  const search = useMemo(() => {
    const next = new URLSearchParams(searchParams)
    next.delete('env')
    next.delete('namespace')
    return next
  }, [searchParams])
  const query = useQuery({
    queryKey: ['audit', requestScope, search.toString()],
    queryFn: ({ signal }) => gatewayApi.audits(requestScope!, search, signal),
    enabled: canRead && Boolean(requestScope),
  })
  if (!canRead) return <QueryFailure error={new Error('没有审计读取能力')} />
  const updateScope = (value: Partial<Scope>) => {
    const next = writeScopeSearchParams(searchParams, value, ['env', 'namespace'])
    next.set('page', '1')
    setSearchParams(next)
  }
  const submit = (values: Record<string, unknown>) => {
    const next = new URLSearchParams(searchParams)
    ;['actorId', 'resourceId', 'traceId', 'successful'].forEach((key) => {
      const value = values[key]
      if (value === undefined || value === '') next.delete(key)
      else next.set(key, String(value))
    })
    next.set('page', '1')
    setSearchParams(next)
  }
  return (
    <section>
      <Typography.Title level={2}>审计日志</Typography.Title>
      <GatewayScopeFilter fields={['env', 'namespace']} value={scope} required onChange={updateScope} />
      <Card>
        <Form layout="inline" initialValues={Object.fromEntries(search)} onFinish={submit}>
          <Form.Item name="actorId" label="Actor"><Input allowClear /></Form.Item>
          <Form.Item name="resourceId" label="Resource ID"><Input allowClear /></Form.Item>
          <Form.Item name="traceId" label="Trace ID"><Input allowClear /></Form.Item>
          <Form.Item name="successful" label="结果">
            <Select allowClear style={{ width: 120 }} options={[{ value: 'true', label: '成功' }, { value: 'false', label: '失败' }]} />
          </Form.Item>
          <Form.Item><button type="submit" className="ant-btn ant-btn-primary">查询</button></Form.Item>
        </Form>
      </Card>
      {!requestScope ? <EmptyBlock description="请选择 Env 和 Namespace 查询范围" /> : (
        query.isLoading ? <LoadingBlock /> : query.error ? <QueryFailure error={query.error} /> : (
          <Table<AuditEntry>
            className="section-row"
            rowKey="id"
            dataSource={query.data?.items ?? []}
            onRow={(row) => ({ onClick: () => setSelected(row) })}
            onChange={(pagination) => {
              const next = new URLSearchParams(searchParams)
              next.set('page', String(pagination.current ?? 1))
              setSearchParams(next)
            }}
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
        )
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
