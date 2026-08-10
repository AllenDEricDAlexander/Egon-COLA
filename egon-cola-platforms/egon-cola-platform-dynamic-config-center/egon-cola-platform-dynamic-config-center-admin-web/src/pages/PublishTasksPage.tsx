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
  Descriptions,
  Grid,
  Input,
  Modal,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
} from 'antd'
import type { TableColumnsType } from 'antd'
import { useState } from 'react'
import { ddcApi, ddcPageApi } from '../api/client'
import type { DdcPublishResult, DdcPublishTask } from '../api/types'
import AdminPageHeader from '../components/page/AdminPageHeader'
import ScopeSelects, { type ScopeValue } from '../components/scope/ScopeSelects'
import { usePageState } from '../hooks/usePageState'
import { buildQuery, formatTime } from '../lib/query'

const POLL_INTERVAL_MS = 15_000

type PublishTaskFilter = ScopeValue & {
  status: string
  changeId: string
}

const emptyFilter: PublishTaskFilter = {
  bizCode: '',
  namespaceCode: '',
  env: '',
  appCode: '',
  status: '',
  changeId: '',
}

const statusColor = (status: string): string => {
  if (status === 'SUCCESS') return 'green'
  if (status === 'FAILED' || status === 'TIMEOUT') return 'red'
  if (status === 'PENDING' || status === 'DISPATCHED') return 'orange'
  return 'default'
}

export default function PublishTasksPage() {
  const { message, modal } = App.useApp()
  const screens = Grid.useBreakpoint()
  const queryClient = useQueryClient()
  const pageState = usePageState()
  const [draft, setDraft] = useState<PublishTaskFilter>({ ...emptyFilter })
  const [submitted, setSubmitted] = useState<PublishTaskFilter>({
    ...emptyFilter,
  })
  const [detailTask, setDetailTask] = useState<DdcPublishTask | null>(null)

  const tasksQuery = useQuery({
    queryKey: ['ddc', 'publish-tasks', submitted, pageState.page],
    queryFn: ({ signal }) => ddcPageApi<DdcPublishTask>(
      `/api/v1/ddc/publish-tasks/page?${buildQuery({
        ...submitted,
        pageNo: pageState.page.pageNo,
        pageSize: pageState.page.pageSize,
      })}`,
      { signal },
    ),
    placeholderData: keepPreviousData,
    refetchInterval: POLL_INTERVAL_MS,
    refetchIntervalInBackground: true,
    staleTime: 0,
  })

  const detailQuery = useQuery({
    enabled: detailTask !== null,
    queryKey: ['ddc', 'publish-task-detail', detailTask?.changeId],
    queryFn: ({ signal }) => ddcApi<DdcPublishTask>(
      `/api/v1/ddc/publish-tasks/${encodeURIComponent(detailTask!.changeId)}`,
      { signal },
    ),
    staleTime: 0,
  })

  const retryMutation = useMutation({
    mutationFn: (task: DdcPublishTask) => ddcApi<DdcPublishResult>(
      `/api/v1/ddc/publish-tasks/${encodeURIComponent(task.changeId)}/retry`,
      { method: 'POST' },
    ),
    onSuccess: async (result) => {
      message.success(`重试任务 ${result.changeId}：${result.status}`)
      await queryClient.invalidateQueries({
        queryKey: ['ddc', 'publish-tasks'],
      })
    },
    onError: (error) => message.error(
      error instanceof Error ? error.message : String(error),
    ),
  })

  const applyFilter = () => {
    setSubmitted({
      ...draft,
      changeId: draft.changeId.trim(),
    })
    pageState.resetPage()
  }

  const resetFilter = () => {
    setDraft({ ...emptyFilter })
    setSubmitted({ ...emptyFilter })
    pageState.resetPage()
  }

  const confirmRetry = (task: DdcPublishTask) => {
    modal.confirm({
      title: `确认重试任务 ${task.changeId}？`,
      okText: '重试',
      onOk: () => retryMutation.mutateAsync(task),
    })
  }

  const columns: TableColumnsType<DdcPublishTask> = [
    {
      title: 'Change ID',
      dataIndex: 'changeId',
      key: 'changeId',
      render: (value: string, row) => (
        <Button type="link" size="small" onClick={() => setDetailTask(row)}>
          <Typography.Text code>{value}</Typography.Text>
        </Button>
      ),
    },
    {
      title: '作用域',
      key: 'scope',
      render: (_: unknown, row) =>
        [row.bizCode, row.env, row.appCode].filter(Boolean).join(' / ') || '—',
    },
    { title: '配置资源', dataIndex: 'resourceName', key: 'resourceName' },
    { title: '目标版本', dataIndex: 'targetVersion', key: 'targetVersion' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={statusColor(status)}>{status}</Tag>
      ),
    },
    { title: '尝试次数', dataIndex: 'attemptCount', key: 'attemptCount' },
    {
      title: 'ack/失败/超时',
      key: 'counts',
      render: (_: unknown, row) =>
        `${row.ackCount ?? 0}/${row.failedCount ?? 0}/${row.timeoutCount ?? 0}`,
    },
    { title: '操作人', dataIndex: 'operator', key: 'operator' },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: formatTime,
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      render: formatTime,
    },
    {
      title: '操作',
      key: 'actions',
      fixed: 'right',
      render: (_: unknown, row) => (
        <Button
          size="small"
          type="primary"
          loading={retryMutation.isPending
            && retryMutation.variables?.id === row.id}
          onClick={() => confirmRetry(row)}
        >
          重试
        </Button>
      ),
    },
  ]

  const detail = detailQuery.data ?? detailTask
  const detailItems = detail
    ? [
        ['Change ID', detail.changeId],
        [
          '作用域',
          [detail.bizCode, detail.env, detail.appCode]
            .filter(Boolean)
            .join(' / '),
        ],
        ['配置资源', detail.resourceName ?? '—'],
        ['目标版本', String(detail.targetVersion ?? '—')],
        ['发布模式', detail.publishMode ?? '—'],
        ['状态', detail.status],
        ['尝试次数', String(detail.attemptCount ?? '—')],
        ['目标数', String(detail.targetCount ?? '—')],
        ['ack 数', String(detail.ackCount ?? '—')],
        ['失败数', String(detail.failedCount ?? '—')],
        ['忽略数', String(detail.ignoredCount ?? '—')],
        ['超时数', String(detail.timeoutCount ?? '—')],
        ['超时时间(ms)', String(detail.timeoutMs ?? '—')],
        ['失败阶段', detail.failureStage ?? '—'],
        ['错误信息', detail.errorMessage ?? '—'],
        ['资源校验和', detail.resourceChecksum ?? '—'],
        ['调度时间', formatTime(detail.dispatchedAt)],
        ['完成时间', formatTime(detail.completedAt)],
        ['创建时间', formatTime(detail.createdAt)],
      ].map(([label, value]) => ({ key: label, label, children: value }))
    : []

  return (
    <div>
      <AdminPageHeader
        title="发布任务"
        description="分页查看发布结果，并每 15 秒刷新当前筛选页。"
        extra={(
          <Button type="primary" onClick={() => { void tasksQuery.refetch() }}>
            刷新当前页
          </Button>
        )}
      />
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <ScopeSelects
            value={draft}
            onChange={(scope) => setDraft({ ...draft, ...scope })}
          />
          <Select
            aria-label="状态"
            allowClear
            placeholder="状态"
            value={draft.status || undefined}
            options={['PENDING', 'DISPATCHED', 'SUCCESS', 'FAILED', 'TIMEOUT']
              .map((value) => ({ value, label: value }))}
            onChange={(status = '') => setDraft({ ...draft, status })}
            style={{ width: 150 }}
          />
          <Input
            placeholder="Change ID"
            value={draft.changeId}
            onChange={(event) => setDraft({
              ...draft,
              changeId: event.target.value,
            })}
            onPressEnter={applyFilter}
            style={{ width: 220 }}
          />
          <Button type="primary" onClick={applyFilter}>查询</Button>
          <Button onClick={resetFilter}>重置</Button>
        </Space>
      </Card>
      <Card size="small" title="任务列表">
        <PageState
          loading={tasksQuery.isPending}
          error={tasksQuery.error}
          empty={(tasksQuery.data?.records.length ?? 0) === 0}
          showPartial={tasksQuery.data !== undefined}
          onRetry={() => { void tasksQuery.refetch() }}
        >
          <Table<DdcPublishTask>
            rowKey={(row) => row.id}
            columns={columns}
            dataSource={tasksQuery.data?.records ?? []}
            loading={tasksQuery.isFetching}
            size="small"
            scroll={{ x: 'max-content' }}
            pagination={{
              current: tasksQuery.data?.page.pageNo ?? pageState.page.pageNo,
              pageSize: tasksQuery.data?.page.pageSize
                ?? pageState.page.pageSize,
              total: tasksQuery.data?.page.total ?? 0,
              showSizeChanger: true,
              pageSizeOptions: [10, 20, 50],
              showTotal: (total) => `共 ${total} 条`,
              onChange: pageState.onTableChange,
            }}
          />
        </PageState>
      </Card>
      <Modal
        open={detailTask !== null}
        title={`任务详情：${detailTask?.changeId ?? ''}`}
        onCancel={() => setDetailTask(null)}
        footer={null}
        width={screens.md ? 720 : 'calc(100vw - 24px)'}
      >
        <Spin spinning={detailQuery.isFetching}>
          <Descriptions
            bordered
            size="small"
            column={screens.md ? 2 : 1}
            items={detailItems}
          />
        </Spin>
      </Modal>
    </div>
  )
}
