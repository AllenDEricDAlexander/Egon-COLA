import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {Alert, Button, Descriptions, Select, Space, Table, Tag} from 'antd'
import {useEffect, useState} from 'react'
import {
  activeKnowledgeJobStatuses,
  isTerminalKnowledgeJob,
  knowledgeApi,
  knowledgeJobStatusValues,
  type KnowledgeJob,
} from '../../api/knowledge'
import {GatewayApiError} from '../../api/client'
import {useCapability} from '../../app/capabilities'
import {LoadingBlock, QueryFailure} from '../../components/QueryState'

const hasActiveJob = (jobs: readonly KnowledgeJob[] | undefined): boolean =>
  (jobs ?? []).some((job) =>
    activeKnowledgeJobStatuses.some((active) => active === job.status),
  )

// 页签不可见时组件不挂载；页面切到后台时停止轮询；终态任务同样停止（原业务Spec §12.2）。
const useDocumentVisible = (): boolean => {
  const [visible, setVisible] = useState(() => document.visibilityState !== 'hidden')
  useEffect(() => {
    const onChange = () => setVisible(document.visibilityState !== 'hidden')
    document.addEventListener('visibilitychange', onChange)
    return () => document.removeEventListener('visibilitychange', onChange)
  }, [])
  return visible
}

export const KnowledgeJobsPanel = ({ kbId }: { kbId: string }) => {
  const queryClient = useQueryClient()
  const canRetry = useCapability('yuheng:knowledge:write')
  const visible = useDocumentVisible()
  const [status, setStatus] = useState<string>()
  const [selectedJobId, setSelectedJobId] = useState<string>()
  const jobs = useQuery({
    queryKey: ['knowledge', kbId, 'jobs', {status}],
    queryFn: ({signal}) => knowledgeApi.jobs(kbId, {status}, signal),
    refetchInterval: (query) =>
      visible && hasActiveJob(query.state.data?.items) ? 2_000 : false,
  })
  const job = useQuery({
    queryKey: ['job', selectedJobId],
    queryFn: ({signal}) => knowledgeApi.job(selectedJobId!, signal),
    enabled: Boolean(selectedJobId),
    refetchInterval: (query) =>
      visible && query.state.data && !isTerminalKnowledgeJob(query.state.data.status) ? 2_000 : false,
  })
  const retry = useMutation({
    mutationFn: (target: KnowledgeJob) => knowledgeApi.retryJob(target.id, target.revision),
    retry: false,
    onSuccess: async (retried) => {
      await Promise.all([
        queryClient.invalidateQueries({queryKey: ['knowledge', kbId, 'jobs']}),
        queryClient.invalidateQueries({queryKey: ['job', retried.id]}),
      ])
    },
  })

  if (jobs.isLoading) return <LoadingBlock />
  if (jobs.isError) return <QueryFailure error={jobs.error} retry={() => void jobs.refetch()} />

  const rows = jobs.data?.items ?? []
  const retryConflict = retry.error instanceof GatewayApiError && retry.error.status === 409

  return (
    <Space direction="vertical" size="middle" style={{width: '100%'}}>
      <Space wrap>
        <Select
          allowClear
          aria-label="任务状态筛选"
          placeholder="全部状态"
          value={status}
          style={{minWidth: 180}}
          onChange={setStatus}
          options={knowledgeJobStatusValues.map((value) => ({value, label: value}))}
        />
        <Button onClick={() => void jobs.refetch()}>刷新任务</Button>
        <Tag>{visible ? '轮询开启' : '页面不可见，轮询已停止'}</Tag>
      </Space>
      {retry.isError && (
        <Alert
          type="error"
          showIcon
          message={retry.error instanceof Error ? retry.error.message : '重试失败'}
          description={retryConflict ? '任务版本已变化，请刷新后再决定' : undefined}
        />
      )}
      <Table<KnowledgeJob>
        rowKey="id"
        dataSource={rows}
        pagination={{total: jobs.data?.total ?? 0, pageSize: 20}}
        onRow={(record) => ({onClick: () => setSelectedJobId(record.id)})}
        columns={[
          {title: '任务 ID', dataIndex: 'id'},
          {title: '类型', dataIndex: 'type'},
          {title: '关联资源', dataIndex: 'resourceId'},
          {title: '状态', dataIndex: 'status'},
          {title: '阶段', dataIndex: 'stage'},
          {title: '尝试', dataIndex: 'attempt'},
          {
            title: '错误码',
            dataIndex: 'errorCode',
            render: (value: string | null) => value ?? '—',
          },
          {
            title: '操作',
            render: (_, record) => (
              <Button
                size="small"
                disabled={!canRetry || record.status !== 'FAILED' || retry.isPending}
                title={canRetry ? undefined : '当前账号缺少 yuheng:knowledge:write 能力'}
                onClick={() => retry.mutate(record)}
              >
                重试任务
              </Button>
            ),
          },
        ]}
      />
      {selectedJobId && (
        <Descriptions
          title="任务进度"
          bordered
          size="small"
          column={2}
          items={[
            {key: 'id', label: '任务 ID', children: job.data?.id ?? selectedJobId},
            {key: 'status', label: '状态', children: job.data?.status ?? '加载中'},
            {key: 'stage', label: '阶段', children: job.data?.stage ?? '—'},
            {key: 'attempt', label: '尝试次数', children: job.data ? `${job.data.attempt} / 3` : '—'},
            {key: 'error', label: '错误码', children: job.data?.errorCode ?? '—'},
            {key: 'updated', label: '更新时间', children: job.data?.updatedAt ?? '—'},
          ]}
        />
      )}
      {job.isError && <QueryFailure error={job.error} />}
    </Space>
  )
}
