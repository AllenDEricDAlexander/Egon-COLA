import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { PageState } from '@egon-cola/admin-web-shared'
import { Alert, Button, Card, Space, Table, Tag, Typography } from 'antd'
import type { TableColumnsType } from 'antd'
import { useState } from 'react'
import { ddcPageApi } from '../api/client'
import type { DdcInstance } from '../api/types'
import AdminPageHeader from '../components/page/AdminPageHeader'
import ScopeSelects, { type ScopeValue } from '../components/scope/ScopeSelects'
import { usePageState } from '../hooks/usePageState'
import { buildQuery, formatTime } from '../lib/query'

const emptyScope: ScopeValue = {
  bizCode: '',
  namespaceCode: '',
  env: '',
  appCode: '',
}

const scopeReady = (scope: ScopeValue): boolean =>
  scope.bizCode.trim() !== ''
  && scope.env.trim() !== ''
  && scope.appCode.trim() !== ''

export default function InstancesPage() {
  const pageState = usePageState()
  const [draft, setDraft] = useState<ScopeValue>({ ...emptyScope })
  const [submitted, setSubmitted] = useState<ScopeValue | null>(null)
  const [scopeError, setScopeError] = useState(false)

  const instancesQuery = useQuery({
    enabled: submitted !== null,
    queryKey: ['ddc', 'instances', submitted, pageState.page],
    queryFn: ({ signal }) => ddcPageApi<DdcInstance>(
      `/api/v1/ddc/instances/page?${buildQuery({
        bizCode: submitted!.bizCode,
        env: submitted!.env,
        appCode: submitted!.appCode,
        pageNo: pageState.page.pageNo,
        pageSize: pageState.page.pageSize,
      })}`,
      { signal },
    ),
    placeholderData: keepPreviousData,
    staleTime: 0,
  })

  const applyFilter = () => {
    const next = { ...draft }
    if (!scopeReady(next)) {
      setScopeError(true)
      return
    }
    setScopeError(false)
    setSubmitted(next)
    pageState.resetPage()
  }

  const resetFilter = () => {
    setDraft({ ...emptyScope })
    setSubmitted(null)
    setScopeError(false)
    pageState.resetPage()
  }

  const rows = instancesQuery.data?.records ?? []
  const columns: TableColumnsType<DdcInstance> = [
    {
      title: '实例 ID',
      dataIndex: 'instanceId',
      key: 'instanceId',
      render: (value: string) => <Typography.Text code copyable={{ text: value }}>{value}</Typography.Text>,
    },
    {
      title: '作用域',
      key: 'scope',
      render: (_: unknown, row) => `${row.bizCode} / ${row.env} / ${row.appCode}`,
    },
    {
      title: '地址',
      key: 'address',
      render: (_: unknown, row) => <Typography.Text code>{`${row.host}:${row.port}`}</Typography.Text>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (value: string) => <Tag color={value === 'ONLINE' ? 'green' : 'default'}>{value || 'UNKNOWN'}</Tag>,
    },
    { title: 'Lease ID', dataIndex: 'leaseId', key: 'leaseId', render: (value?: string) => value ?? '—' },
    { title: 'Lease 过期时间', dataIndex: 'leaseExpireAt', key: 'leaseExpireAt', render: formatTime },
    { title: '最近心跳', dataIndex: 'lastHeartbeatAt', key: 'lastHeartbeatAt', render: formatTime },
    { title: 'SDK 版本', dataIndex: 'sdkVersion', key: 'sdkVersion', render: (value?: string) => value ?? '—' },
    { title: 'PID', dataIndex: 'pid', key: 'pid', render: (value?: string) => value ?? '—' },
  ]

  return (
    <div>
      <AdminPageHeader
        title="配置客户端实例"
        description="按物理作用域分页查看配置客户端租约、状态和最近心跳。"
        extra={(
          <Button disabled={submitted === null} loading={instancesQuery.isFetching} onClick={() => { void instancesQuery.refetch() }}>
            刷新当前页
          </Button>
        )}
      />
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <ScopeSelects
            value={draft}
            onChange={(scope) => {
              setScopeError(false)
              setDraft(scope)
            }}
          />
          <Button type="primary" onClick={applyFilter}>查询</Button>
          <Button onClick={resetFilter}>重置</Button>
        </Space>
        {scopeError && (
          <Alert
            className="section-row"
            type="warning"
            showIcon
            message="请填写 bizCode、env 和 appCode 后再查询"
          />
        )}
      </Card>
      <Card size="small" title="客户端实例列表">
        {submitted === null ? (
          <Typography.Text type="secondary">请选择完整作用域并执行查询。</Typography.Text>
        ) : (
          <PageState
            loading={instancesQuery.isPending && !instancesQuery.error}
            error={instancesQuery.error}
            empty={rows.length === 0}
            emptyDescription="当前作用域暂无配置客户端实例"
            showPartial={instancesQuery.data !== undefined}
            onRetry={() => { void instancesQuery.refetch() }}
          >
            <Table<DdcInstance>
              rowKey={(row) => row.instanceId}
              columns={columns}
              dataSource={rows}
              loading={instancesQuery.isFetching}
              size="small"
              scroll={{ x: 'max-content' }}
              pagination={{
                current: instancesQuery.data?.page.pageNo ?? pageState.page.pageNo,
                pageSize: instancesQuery.data?.page.pageSize ?? pageState.page.pageSize,
                total: instancesQuery.data?.page.total ?? 0,
                showSizeChanger: true,
                pageSizeOptions: [10, 20, 50],
                showTotal: (total) => `共 ${total} 条`,
                onChange: pageState.onTableChange,
              }}
            />
          </PageState>
        )}
      </Card>
    </div>
  )
}
