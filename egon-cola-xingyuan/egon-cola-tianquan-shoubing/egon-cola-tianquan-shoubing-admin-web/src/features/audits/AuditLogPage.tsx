import {Button, Card, Form, Input, Select, Space, Table, Tag} from 'antd'
import {ReloadOutlined} from '@ant-design/icons'
import {useSearchParams} from 'react-router-dom'
import {useQuery} from '@tanstack/react-query'
import {httpClient} from '../../auth/AuthContext'
import {PageState} from '@egon-cola/xingyuan-admin-web-shared'
import type {AuditFilter, AuditPageVO, AuditVO} from '../../api/types'

const PAGE_SIZE = 20

type AuditFilterForm = Omit<AuditFilter, 'page' | 'size'>

const readAuditFilter = (searchParams: URLSearchParams): AuditFilter => {
  const pageValue = Number.parseInt(searchParams.get('page') ?? '0', 10)
  const sizeValue = Number.parseInt(searchParams.get('size') ?? String(PAGE_SIZE), 10)
  const read = (name: string) => searchParams.get(name)?.trim() || undefined

  return {
    page: Number.isInteger(pageValue) && pageValue >= 0 ? pageValue : 0,
    size: Number.isInteger(sizeValue) && sizeValue > 0 ? sizeValue : PAGE_SIZE,
    actorSub: read('actorSub'),
    eventType: read('eventType'),
    result: read('result'),
    from: read('from'),
    to: read('to'),
    traceId: read('traceId'),
  }
}

const toAuditSearchParams = (filter: AuditFilter): URLSearchParams => {
  const searchParams = new URLSearchParams({page: String(filter.page), size: String(filter.size)})
  if (filter.actorSub) searchParams.set('actorSub', filter.actorSub.trim())
  if (filter.eventType) searchParams.set('eventType', filter.eventType.trim())
  if (filter.result) searchParams.set('result', filter.result)
  if (filter.from) searchParams.set('from', filter.from)
  if (filter.to) searchParams.set('to', filter.to)
  if (filter.traceId) searchParams.set('traceId', filter.traceId.trim())
  return searchParams
}

const buildAuditQuery = (filter: AuditFilter): string => toAuditSearchParams(filter).toString()

// Date controls use local wall time; URL/API filters retain absolute instants.
const localDateTime = (value?: string): string | undefined => {
  if (!value) return undefined
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return undefined
  return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, -1)
}

export const AuditLogPage = () => {
  const [searchParams, setSearchParams] = useSearchParams()
  const submitted = readAuditFilter(searchParams)
  const requestQuery = buildAuditQuery(submitted)

  const query = useQuery({
    queryKey: ['tianquan-shoubing', 'audits', requestQuery],
    queryFn: () => httpClient.request<AuditPageVO>(`/api/v1/tianquan-shoubing/audits?${requestQuery}`),
  })

  return (
    <Card
      title={`安全审计（${query.data?.totalElements ?? 0}）`}
      extra={<Button icon={<ReloadOutlined/>} onClick={() => { void query.refetch() }}>刷新</Button>}
    >
      <Form<AuditFilterForm>
        key={searchParams.toString()}
        layout="inline"
        initialValues={{
          actorSub: submitted.actorSub,
          eventType: submitted.eventType,
          result: submitted.result,
          from: localDateTime(submitted.from),
          to: localDateTime(submitted.to),
          traceId: submitted.traceId,
        }}
        onFinish={(values) => {
          setSearchParams(toAuditSearchParams({
            page: 0, size: PAGE_SIZE, ...values,
            from: values.from ? new Date(values.from).toISOString() : undefined,
            to: values.to ? new Date(values.to).toISOString() : undefined,
          }))
        }}
        style={{marginBottom: 16}}
      >
        <Form.Item name="actorSub" label="操作者">
          <Input allowClear placeholder="identitySub"/>
        </Form.Item>
        <Form.Item name="eventType" label="事件">
          <Input allowClear placeholder="LOGIN_SUCCEEDED"/>
        </Form.Item>
        <Form.Item name="result" label="结果">
          <Select
            allowClear
            placeholder="全部"
            options={['SUCCESS', 'FAILURE'].map((value) => ({label: value, value}))}
            style={{width: 140}}
          />
        </Form.Item>
        <Form.Item name="from" label="开始时间">
          <Input type="datetime-local" step="0.001"/>
        </Form.Item>
        <Form.Item name="to" label="结束时间">
          <Input type="datetime-local" step="0.001"/>
        </Form.Item>
        <Form.Item name="traceId" label="Trace ID">
          <Input allowClear placeholder="trace-id"/>
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit">查询</Button>
            <Button onClick={() => setSearchParams(toAuditSearchParams({page: 0, size: PAGE_SIZE}))}>重置</Button>
          </Space>
        </Form.Item>
      </Form>
      <PageState
        loading={query.isPending}
        error={query.error}
        empty={query.data?.content.length === 0}
        emptyDescription="暂无审计记录"
        onRetry={() => { void query.refetch() }}
      >
        <Table<AuditVO>
          rowKey="id"
          dataSource={query.data?.content ?? []}
          pagination={{
            current: (query.data?.page ?? submitted.page) + 1,
            pageSize: query.data?.size ?? submitted.size,
            total: query.data?.totalElements ?? 0,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, size) => {
              setSearchParams(toAuditSearchParams({...submitted, page: page - 1, size}))
            },
          }}
          columns={[
            {title: '时间', dataIndex: 'occurredAt'},
            {title: '事件', dataIndex: 'eventType'},
            {title: '操作者', dataIndex: 'actorSub'},
            {title: '目标', dataIndex: 'targetSub'},
            {title: '结果', dataIndex: 'result', render: (value: string) => <Tag>{value}</Tag>},
            {title: '原因', dataIndex: 'reason'},
          ]}
        />
      </PageState>
    </Card>
  )
}
