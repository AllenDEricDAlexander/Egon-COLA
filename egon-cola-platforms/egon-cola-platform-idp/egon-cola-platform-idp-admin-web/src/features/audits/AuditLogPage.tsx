import {Card, Table, Tag} from 'antd'
import {useState} from 'react'
import {useQuery} from '@tanstack/react-query'
import {httpClient} from '../../auth/AuthContext'
import {PageState} from '@egon-cola/admin-web-shared'
import type {AuditPageVO, AuditVO} from '../../api/types'

const PAGE_SIZE = 20

export const AuditLogPage = () => {
  const [page, setPage] = useState(0)

  const query = useQuery({
    queryKey: ['idp', 'audits', page],
      queryFn: () => httpClient.request<AuditPageVO>(`/api/v1/identity/audits?page=${page}&size=${PAGE_SIZE}`),
  })

  return (
    <Card title={`安全审计（${query.data?.totalElements ?? 0}）`}>
      <PageState loading={query.isPending} error={query.error} empty={query.data?.content.length === 0} onRetry={() => { void query.refetch() }}>
          <Table<AuditVO>
          rowKey="id"
          dataSource={query.data?.content ?? []}
          pagination={{
            current: page + 1,
            pageSize: PAGE_SIZE,
            total: query.data?.totalElements ?? 0,
            onChange: (p) => setPage(p - 1),
          }}
          columns={[
            { title: '时间', dataIndex: 'occurredAt' },
            { title: '事件', dataIndex: 'eventType' },
            { title: '操作者', dataIndex: 'actorSub' },
            { title: '目标', dataIndex: 'targetSub' },
            { title: '结果', dataIndex: 'result', render: (v: string) => <Tag>{v}</Tag> },
            { title: '原因', dataIndex: 'reason' },
          ]}
        />
      </PageState>
    </Card>
  )
}
