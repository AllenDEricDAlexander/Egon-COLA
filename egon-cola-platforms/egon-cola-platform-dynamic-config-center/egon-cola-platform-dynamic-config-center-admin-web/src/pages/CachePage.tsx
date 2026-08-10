import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import { PageState } from '@egon-cola/admin-web-shared'
import {
  App,
  Button,
  Card,
  Col,
  Row,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
} from 'antd'
import type { TableColumnsType } from 'antd'
import { useState } from 'react'
import { ddcApi, ddcPageApi } from '../api/client'
import type { DdcCacheCheckRow } from '../api/types'
import AdminPageHeader from '../components/page/AdminPageHeader'
import ScopeSelects, { type ScopeValue } from '../components/scope/ScopeSelects'
import { usePageState } from '../hooks/usePageState'
import { buildQuery } from '../lib/query'

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

export default function CachePage() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const pageState = usePageState()
  const [draft, setDraft] = useState<ScopeValue>({ ...emptyScope })
  const [submitted, setSubmitted] = useState<ScopeValue | null>(null)

  const checkQuery = useQuery({
    enabled: submitted !== null,
    queryKey: ['ddc', 'cache-check', submitted, pageState.page],
    queryFn: ({ signal }) => ddcPageApi<DdcCacheCheckRow>(
      `/api/v1/ddc/cache/check/page?${buildQuery({
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

  const rebuildMutation = useMutation({
    mutationFn: (scope: ScopeValue) => ddcApi<number>(
      `/api/v1/ddc/cache/rebuild?${buildQuery({
        bizCode: scope.bizCode,
        env: scope.env,
        appCode: scope.appCode,
      })}`,
      { method: 'POST' },
    ),
    onSuccess: async (count) => {
      message.success(`已重建 ${count ?? 0} 项缓存`)
      await queryClient.invalidateQueries({ queryKey: ['ddc', 'cache-check'] })
    },
    onError: (error) => message.error(
      error instanceof Error ? error.message : String(error),
    ),
  })

  const check = () => {
    const next = { ...draft }
    if (!scopeReady(next)) {
      message.warning('请填写 bizCode / env / appCode')
      return
    }
    setSubmitted(next)
    pageState.resetPage()
  }

  const confirmRebuild = () => {
    const scope = submitted ?? draft
    if (!scopeReady(scope)) {
      message.warning('请先检查完整作用域')
      return
    }
    modal.confirm({
      title: '确认重建该作用域下的缓存？',
      okText: '重建缓存',
      okButtonProps: { danger: true },
      onOk: () => rebuildMutation.mutateAsync(scope),
    })
  }

  const rows = checkQuery.data?.records ?? []
  const matched = rows.filter((row) => row.matched).length
  const mismatched = rows.length - matched

  const columns: TableColumnsType<DdcCacheCheckRow> = [
    {
      title: '配置资源',
      dataIndex: 'resourceName',
      key: 'resourceName',
      render: (value: string) => (
        <Typography.Text code>{value}</Typography.Text>
      ),
    },
    {
      title: '一致性',
      dataIndex: 'matched',
      key: 'matched',
      render: (value: boolean) => (
        <Tag color={value ? 'green' : 'red'}>{value ? '一致' : '不一致'}</Tag>
      ),
    },
    { title: 'DB 版本', dataIndex: 'databaseVersion', key: 'databaseVersion' },
    { title: 'Redis 版本', dataIndex: 'redisVersion', key: 'redisVersion' },
    {
      title: 'DB 值',
      dataIndex: 'databaseValue',
      key: 'databaseValue',
      render: (value?: string) => (
        <Typography.Text code ellipsis style={{ maxWidth: 240 }}>
          {value ?? '—'}
        </Typography.Text>
      ),
    },
    {
      title: 'Redis 值',
      dataIndex: 'redisValue',
      key: 'redisValue',
      render: (value?: string) => (
        <Typography.Text code ellipsis style={{ maxWidth: 240 }}>
          {value ?? '—'}
        </Typography.Text>
      ),
    },
  ]

  return (
    <div>
      <AdminPageHeader
        title="缓存管理"
        description="按物理作用域分页检查数据库与 Redis 的缓存一致性。"
      />
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <ScopeSelects value={draft} onChange={setDraft} />
          <Button
            type="primary"
            loading={checkQuery.isFetching}
            onClick={check}
          >
            检查缓存
          </Button>
          <Button
            danger
            loading={rebuildMutation.isPending}
            onClick={confirmRebuild}
          >
            重建缓存
          </Button>
        </Space>
      </Card>
      {submitted !== null && (
        <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
          <Col xs={24} sm={12}>
            <Card size="small">
              <Statistic title="本页一致" value={matched} />
            </Card>
          </Col>
          <Col xs={24} sm={12}>
            <Card size="small">
              <Statistic title="本页不一致" value={mismatched} />
            </Card>
          </Col>
        </Row>
      )}
      <Card size="small" title="缓存检查结果">
        {submitted === null ? (
          <Typography.Text type="secondary">
            请选择完整作用域并执行检查。
          </Typography.Text>
        ) : (
          <PageState
            loading={checkQuery.isPending}
            error={checkQuery.error}
            empty={rows.length === 0}
            onRetry={() => { void checkQuery.refetch() }}
          >
            <Table<DdcCacheCheckRow>
              rowKey={(row) => row.resourceName}
              columns={columns}
              dataSource={rows}
              loading={checkQuery.isFetching}
              size="small"
              scroll={{ x: 'max-content' }}
              pagination={{
                current: checkQuery.data?.page.pageNo
                  ?? pageState.page.pageNo,
                pageSize: checkQuery.data?.page.pageSize
                  ?? pageState.page.pageSize,
                total: checkQuery.data?.page.total ?? 0,
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
