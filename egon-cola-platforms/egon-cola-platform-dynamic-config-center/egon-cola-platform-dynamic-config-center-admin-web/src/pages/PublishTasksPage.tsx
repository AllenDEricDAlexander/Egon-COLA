import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Descriptions, Modal, Table, Tag, Typography, message } from 'antd'
import { ddcApi } from '../api/client'
import type { DdcPublishResult, DdcPublishTask } from '../api/types'
import { formatTime } from '../lib/query'

const POLL_INTERVAL_MS = 15000

const statusColor = (status: string): string => {
  if (status === 'SUCCESS') return 'green'
  if (status === 'FAILED' || status === 'TIMEOUT') return 'red'
  if (status === 'PENDING' || status === 'DISPATCHED') return 'orange'
  return 'default'
}

export default function PublishTasksPage() {
  const [tasks, setTasks] = useState<DdcPublishTask[]>([])
  const [loading, setLoading] = useState(false)
  const [detail, setDetail] = useState<DdcPublishTask | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)

  const loadTasks = useCallback(async () => {
    const data = await ddcApi<DdcPublishTask[]>('/api/v1/ddc/publish-tasks')
    setTasks(data ?? [])
  }, [])

  useEffect(() => {
    loadTasks().catch((error) => {
      message.error(error instanceof Error ? error.message : String(error))
    })
  }, [loadTasks])

  useEffect(() => {
    const timer = setInterval(() => {
      loadTasks().catch((error) => {
        message.error(error instanceof Error ? error.message : String(error))
      })
    }, POLL_INTERVAL_MS)
    return () => clearInterval(timer)
  }, [loadTasks])

  const refresh = async () => {
    setLoading(true)
    try {
      await loadTasks()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    } finally {
      setLoading(false)
    }
  }

  const openDetail = async (task: DdcPublishTask) => {
    setDetail(task)
    setDetailLoading(true)
    try {
      const data = await ddcApi<DdcPublishTask>(`/api/v1/ddc/publish-tasks/${encodeURIComponent(task.changeId)}`)
      setDetail(data)
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    } finally {
      setDetailLoading(false)
    }
  }

  const retry = async (task: DdcPublishTask) => {
    if (!window.confirm(`确认重试任务 ${task.changeId}？`)) return
    try {
      const result = await ddcApi<DdcPublishResult>(`/api/v1/ddc/publish-tasks/${encodeURIComponent(task.changeId)}/retry`, {
        method: 'POST',
      })
      message.success(`重试任务 ${result.changeId}：${result.status}`)
      await refresh()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    }
  }

  const columns = [
    {
      title: 'Change ID',
      dataIndex: 'changeId',
      key: 'changeId',
      render: (value: string, row: DdcPublishTask) => (
        <Button type="link" size="small" onClick={() => void openDetail(row)}>
          <Typography.Text code>{value}</Typography.Text>
        </Button>
      ),
    },
    {
      title: '作用域',
      key: 'scope',
      render: (_: unknown, row: DdcPublishTask) =>
        [row.appCode, row.env, row.namespace].filter(Boolean).join(' / ') || '—',
    },
    { title: '配置 Key', dataIndex: 'configKey', key: 'configKey' },
    { title: '目标版本', dataIndex: 'targetVersion', key: 'targetVersion' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => <Tag color={statusColor(status)}>{status}</Tag>,
    },
    { title: '尝试次数', dataIndex: 'attemptCount', key: 'attemptCount' },
    {
      title: 'ack/失败/超时',
      key: 'counts',
      render: (_: unknown, row: DdcPublishTask) =>
        `${row.ackCount ?? 0}/${row.failedCount ?? 0}/${row.timeoutCount ?? 0}`,
    },
    { title: '操作人', dataIndex: 'operator', key: 'operator' },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', render: formatTime },
    { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', render: formatTime },
    {
      title: '操作',
      key: 'actions',
      render: (_: unknown, row: DdcPublishTask) => (
        <Button size="small" type="primary" onClick={() => void retry(row)}>重试</Button>
      ),
    },
  ]

  const detailItems = detail
    ? [
        ['Change ID', detail.changeId],
        ['作用域', [detail.appCode, detail.env, detail.namespace].filter(Boolean).join(' / ')],
        ['配置 Key', detail.configKey ?? '—'],
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
        ['内容校验和', detail.contentChecksum ?? '—'],
        ['调度时间', formatTime(detail.dispatchedAt)],
        ['完成时间', formatTime(detail.completedAt)],
        ['创建时间', formatTime(detail.createdAt)],
      ].map(([label, value]) => ({ key: label, label, children: value }))
    : []

  return (
    <div>
      <Typography.Title level={3}>发布任务</Typography.Title>
      <Card
        size="small"
        title={`任务（${tasks.length}）`}
        extra={<Button type="primary" onClick={() => void refresh()}>刷新</Button>}
      >
        <Table<DdcPublishTask>
          rowKey={(row) => row.id}
          columns={columns}
          dataSource={tasks}
          loading={loading}
          size="small"
          pagination={{ pageSize: 10, size: 'small' }}
        />
      </Card>
      <Modal
        open={detail !== null}
        title={`任务详情：${detail?.changeId ?? ''}`}
        onCancel={() => setDetail(null)}
        footer={null}
        width={720}
      >
        <Descriptions bordered size="small" column={2} items={detailItems} />
      </Modal>
    </div>
  )
}
