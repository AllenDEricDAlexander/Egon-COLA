import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  App,
  Button,
  Card,
  Descriptions,
  Input,
  InputNumber,
  Radio,
  Space,
  Spin,
  Switch,
  Table,
  Tag,
  Typography,
} from 'antd'
import type { TableColumnsType } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { DdcApiError } from '../api/client'
import {
  ddcAiQueryKeys,
  getAiOptimizationPolicy,
  getAiOptimizationRun,
  pageAiOptimizationRuns,
  replaceAiOptimizationPolicy,
  triggerAiOptimizationRun,
} from '../api/ai'
import type {
  DdcAiOptimizationPolicyFields,
  DdcAiOptimizationPolicyResult,
  DdcAiOptimizationRunResult,
  DdcAiPolicyState,
  DdcAiRunState,
} from '../api/types'
import { formatTime } from '../lib/query'
import { uuidV7 } from '../lib/uuid'

type Props = {
  resourceId: string
  pollIntervalMs?: number
}

const TERMINAL_STATES: readonly DdcAiRunState[] = ['SUCCEEDED', 'FAILED', 'SKIPPED']
const RUN_PAGE_SIZE = 10
const ALIAS_PATTERN = /^[a-z0-9][a-z0-9-]{0,63}$/
const DURATION_PATTERN = /^P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?)?$/

const POLICY_STATE_LABELS: Record<DdcAiPolicyState, string> = {
  ARMED: '待评估',
  AWAITING_PUBLISH: '待人工发布',
  CONVERGED: '已收敛',
  EXHAUSTED: '已达迭代上限',
  PAUSED: '已暂停',
}

const RUN_STATE_LABELS: Record<DdcAiRunState, string> = {
  PENDING: '待执行',
  RUNNING: '执行中',
  SUCCEEDED: '成功',
  FAILED: '失败',
  SKIPPED: '已跳过',
}

const RUN_STATE_COLORS: Record<DdcAiRunState, string> = {
  PENDING: 'default',
  RUNNING: 'processing',
  SUCCEEDED: 'success',
  FAILED: 'error',
  SKIPPED: 'warning',
}

/** ISO-8601 时长（仅 D/H/M/S）换算为秒；无效返回 null。 / ISO-8601 duration (D/H/M/S only) in seconds, or null. */
export const durationSeconds = (value: string): number | null => {
  const match = DURATION_PATTERN.exec(value.trim())
  if (!match || value.trim() === 'P' || value.trim().endsWith('T')) return null
  const [, days, hours, minutes, seconds] = match
  return Number(days ?? 0) * 86_400 + Number(hours ?? 0) * 3_600 + Number(minutes ?? 0) * 60 + Number(seconds ?? 0)
}

const policyFields = (policy: DdcAiOptimizationPolicyResult): DdcAiOptimizationPolicyFields => ({
  enabled: policy.enabled,
  scoreThreshold: policy.scoreThreshold,
  minSamples: policy.minSamples,
  evaluationWindow: policy.evaluationWindow,
  maxIterations: policy.maxIterations,
  cooldown: policy.cooldown,
  autoPublish: policy.autoPublish,
  judgeEnabled: policy.judgeEnabled,
  optimizerModelAlias: policy.optimizerModelAlias,
  judgeModelAlias: policy.judgeModelAlias,
  optimizerInstruction: policy.optimizerInstruction,
})

const validationErrors = (fields: DdcAiOptimizationPolicyFields): string[] => {
  const errors: string[] = []
  const window = durationSeconds(fields.evaluationWindow)
  const cooldown = durationSeconds(fields.cooldown)
  if (window === null || window < 300 || window > 2_592_000) errors.push('评估窗口须为 PT5M 到 P30D 之间的 ISO-8601 时长')
  if (cooldown === null || cooldown < 60 || cooldown > 604_800) errors.push('冷却时间须为 PT1M 到 P7D 之间的 ISO-8601 时长')
  if (!ALIAS_PATTERN.test(fields.optimizerModelAlias)) errors.push('优化模型别名格式无效')
  if (!ALIAS_PATTERN.test(fields.judgeModelAlias)) errors.push('评分模型别名格式无效')
  if (fields.optimizerInstruction.length > 4096) errors.push('优化指令最多 4096 个字符')
  return errors
}

const errorCode = (error: unknown): string | undefined => error instanceof DdcApiError ? error.code : undefined

const failureText = (error: unknown): string => {
  if (error instanceof DdcApiError && error.status === 403) return '当前身份无权执行此操作'
  return error instanceof Error ? error.message : String(error)
}

const utcTime = (value: string | null | undefined) => value
  ? <span title={`UTC ${value}`}>{formatTime(value)}</span>
  : '—'

export default function AiOptimizationPanel({ resourceId, pollIntervalMs = 2000 }: Props) {
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const [edits, setEdits] = useState<Partial<DdcAiOptimizationPolicyFields>>({})
  const [conflict, setConflict] = useState(false)
  const [channelDisabled, setChannelDisabled] = useState(false)
  const [failure, setFailure] = useState<string>()
  const [note, setNote] = useState('')
  const [triggeredRunId, setTriggeredRunId] = useState<string | null>(null)
  const [runStateFilter, setRunStateFilter] = useState<DdcAiRunState | 'ALL'>('ALL')
  const [runPageNo, setRunPageNo] = useState(1)

  const policyKey = ddcAiQueryKeys.optimizationPolicy(resourceId)
  const policyQuery = useQuery({
    queryKey: policyKey,
    queryFn: ({ signal }) => getAiOptimizationPolicy(resourceId, { signal }),
  })
  const policy = policyQuery.data
  const runParams = useMemo(() => ({
    state: runStateFilter === 'ALL' ? undefined : runStateFilter,
    pageNo: runPageNo,
    pageSize: RUN_PAGE_SIZE,
  }), [runStateFilter, runPageNo])
  const runsQuery = useQuery({
    queryKey: ddcAiQueryKeys.optimizationRuns(resourceId, runParams),
    queryFn: ({ signal }) => pageAiOptimizationRuns(resourceId, runParams, { signal }),
  })
  const trackedRunId = triggeredRunId ?? policy?.activeRunId ?? null
  const runQuery = useQuery({
    queryKey: ddcAiQueryKeys.optimizationRun(resourceId, trackedRunId ?? 'none'),
    queryFn: ({ signal }) => getAiOptimizationRun(resourceId, trackedRunId!, { signal }),
    enabled: trackedRunId !== null,
    staleTime: 0,
    refetchInterval: (query) => {
      const state = query.state.data?.state
      return state && TERMINAL_STATES.includes(state) ? false : pollIntervalMs
    },
  })
  const trackedRun = runQuery.data
  const runActive = trackedRun !== undefined && !TERMINAL_STATES.includes(trackedRun.state)
  const trackedTerminalState = trackedRun && TERMINAL_STATES.includes(trackedRun.state) ? trackedRun.state : null

  useEffect(() => {
    if (trackedTerminalState === null) return
    void queryClient.invalidateQueries({ queryKey: ddcAiQueryKeys.optimizationPolicy(resourceId) })
    void queryClient.invalidateQueries({ queryKey: ddcAiQueryKeys.optimizationRunsAll(resourceId) })
  }, [trackedRunId, trackedTerminalState, queryClient, resourceId])

  const fields = policy ? { ...policyFields(policy), ...edits } : null
  const errors = fields ? validationErrors(fields) : []

  const saveMutation = useMutation({
    mutationFn: (rearm: boolean) => replaceAiOptimizationPolicy(resourceId, {
      ...fields!,
      expectedRevision: policy!.revision,
      rearm,
    }),
    onSuccess: (saved) => {
      queryClient.setQueryData(policyKey, saved)
      setEdits({})
      setConflict(false)
      setFailure(undefined)
      message.success('优化策略已保存')
    },
    onError: (error) => {
      const code = errorCode(error)
      if (code === '56303') setConflict(true)
      else if (code === '56323') message.warning('存在进行中的优化运行，结束后才能重新启用')
      else if (code === '56322') setChannelDisabled(true)
      else setFailure(failureText(error))
    },
  })

  const triggerMutation = useMutation({
    mutationFn: () => triggerAiOptimizationRun(resourceId, uuidV7(), { note }),
    onSuccess: (run) => {
      queryClient.setQueryData(ddcAiQueryKeys.optimizationRun(resourceId, run.id), run)
      setTriggeredRunId(run.id)
      setNote('')
      void queryClient.invalidateQueries({ queryKey: ddcAiQueryKeys.optimizationRunsAll(resourceId) })
    },
    onError: (error) => {
      const code = errorCode(error)
      if (code === '56322') setChannelDisabled(true)
      else if (code === '56323') message.warning('已有优化运行在进行中')
      else if (code === '56321') message.warning('资源尚未发布，无法优化')
      else setFailure(failureText(error))
    },
  })

  const updateField = <K extends keyof DdcAiOptimizationPolicyFields>(key: K, value: DdcAiOptimizationPolicyFields[K]) =>
    setEdits((current) => ({ ...current, [key]: value }))

  if (policyQuery.isPending) return <Spin />
  if (policyQuery.isError || !policy || !fields) {
    return <Alert type="error" showIcon title={`优化策略不可用：${failureText(policyQuery.error)}`} />
  }

  const runColumns: TableColumnsType<DdcAiOptimizationRunResult> = [
    { title: '运行号', dataIndex: 'runNo', key: 'runNo' },
    { title: '触发', dataIndex: 'trigger', key: 'trigger', render: (value: string) => value === 'MANUAL' ? '手动' : '定时' },
    { title: '状态', dataIndex: 'state', key: 'state', render: (value: DdcAiRunState) => <Tag color={RUN_STATE_COLORS[value]}>{value}</Tag> },
    { title: '基线版本', dataIndex: 'baseVersionNo', key: 'baseVersionNo', render: (value: string | null) => value ?? '—' },
    { title: '样本 / 均分', key: 'score', render: (_, run) => `${run.sampleCount} / ${run.averageScore ?? '—'}` },
    { title: '产出版本', dataIndex: 'producedVersionNo', key: 'producedVersionNo', render: (value: string | null, run) =>
      value ? `${value}${run.autoPublished ? '（已自动发布）' : '（草稿）'}` : '—' },
    { title: '错误', dataIndex: 'errorCode', key: 'errorCode', render: (value: string | null, run) =>
      value ? <span title={run.errorMessage ?? undefined}>{value}</span> : '—' },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', render: (value: string) => utcTime(value) },
  ]

  return (
    <Space orientation="vertical" size="large" className="tianshu-ai-page">
      {channelDisabled && (
        <Alert type="warning" showIcon title="优化通道未启用：管理端未开启 egon.cola.component.tianshu.admin.ai.optimization.enabled" />
      )}
      {conflict && (
        <Alert
          type="warning"
          showIcon
          title="策略已被他人修改；已保留你的输入，请刷新修订后重新保存"
          action={<Button size="small" onClick={() => { setConflict(false); void policyQuery.refetch() }}>刷新修订</Button>}
        />
      )}
      {failure && <Alert type="error" showIcon closable title={failure} onClose={() => setFailure(undefined)} />}

      <Card title="评估快照">
        <Descriptions
          size="small"
          column={3}
          items={[
            { key: 'state', label: '策略状态', children: <Tag>{POLICY_STATE_LABELS[policy.state]}</Tag> },
            { key: 'iterations', label: '迭代', children: `${policy.iterationsDone} / ${policy.maxIterations}` },
            { key: 'failures', label: '连续失败', children: policy.consecutiveFailures },
            { key: 'evaluatedAt', label: '最近评估', children: utcTime(policy.evaluation?.evaluatedAt) },
            { key: 'version', label: '评估版本', children: policy.evaluation?.versionNo ?? '—' },
            { key: 'samples', label: '样本 / 待评分', children:
              `${policy.evaluation?.sampleCount ?? 0} / ${policy.evaluation?.pendingJudgeCount ?? 0}` },
            { key: 'average', label: '平均分', children: policy.evaluation?.averageScore ?? '—' },
            { key: 'revision', label: '策略修订', children: policy.persisted ? policy.revision : '未保存（默认值）' },
            { key: 'converged', label: '收敛时间', children: utcTime(policy.convergedAt) },
          ]}
        />
      </Card>

      <Card title="优化策略">
        <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
          <Space wrap>
            <Switch aria-label="启用自动优化" checked={fields.enabled} onChange={(value) => updateField('enabled', value)} />
            <span>启用自动优化</span>
            <Switch aria-label="启用 LLM 评分" checked={fields.judgeEnabled} onChange={(value) => updateField('judgeEnabled', value)} />
            <span>启用 LLM 评分</span>
            <Switch aria-label="自动发布优化结果" checked={fields.autoPublish} onChange={(value) => updateField('autoPublish', value)} />
            <span>自动发布优化结果</span>
          </Space>
          <Space wrap>
            <label>分数阈值 <InputNumber aria-label="分数阈值" min={1} max={100} value={fields.scoreThreshold}
              onChange={(value) => updateField('scoreThreshold', Number(value ?? 1))} /></label>
            <label>最少样本 <InputNumber aria-label="最少样本" min={1} max={100000} value={fields.minSamples}
              onChange={(value) => updateField('minSamples', Number(value ?? 1))} /></label>
            <label>最多迭代 <InputNumber aria-label="最多迭代" min={1} max={50} value={fields.maxIterations}
              onChange={(value) => updateField('maxIterations', Number(value ?? 1))} /></label>
          </Space>
          <Space wrap>
            <label>评估窗口 <Input aria-label="评估窗口" style={{ width: 120 }} value={fields.evaluationWindow}
              onChange={(event) => updateField('evaluationWindow', event.target.value)} /></label>
            <label>冷却时间 <Input aria-label="冷却时间" style={{ width: 120 }} value={fields.cooldown}
              onChange={(event) => updateField('cooldown', event.target.value)} /></label>
            <label>优化模型 <Input aria-label="优化模型别名" style={{ width: 180 }} value={fields.optimizerModelAlias}
              onChange={(event) => updateField('optimizerModelAlias', event.target.value)} /></label>
            <label>评分模型 <Input aria-label="评分模型别名" style={{ width: 180 }} value={fields.judgeModelAlias}
              onChange={(event) => updateField('judgeModelAlias', event.target.value)} /></label>
          </Space>
          <Input.TextArea aria-label="优化指令" rows={3} maxLength={4096} value={fields.optimizerInstruction}
            placeholder="附加给优化模型的约束（可选）"
            onChange={(event) => updateField('optimizerInstruction', event.target.value)} />
          {errors.map((error) => <Typography.Text key={error} type="danger">{error}</Typography.Text>)}
          <Space wrap>
            <Button type="primary" loading={saveMutation.isPending} disabled={errors.length > 0}
              onClick={() => saveMutation.mutate(false)}>保存策略</Button>
            <Button loading={saveMutation.isPending} disabled={errors.length > 0 || runActive || !policy.persisted
              || policy.activeRunId !== null || policy.state === 'ARMED'}
              onClick={() => saveMutation.mutate(true)}>保存并重新启用</Button>
          </Space>
        </Space>
      </Card>

      <Card title="手动优化">
        <Space orientation="vertical" style={{ width: '100%' }}>
          <Space wrap>
            <Input aria-label="运行备注" style={{ width: 320 }} maxLength={512} value={note} placeholder="备注（可选）"
              onChange={(event) => setNote(event.target.value)} />
            <Button type="primary" loading={triggerMutation.isPending} disabled={runActive || !policy.persisted}
              onClick={() => triggerMutation.mutate()}>立即优化一次</Button>
          </Space>
          {trackedRun && (
            <Descriptions
              size="small"
              column={4}
              items={[
                { key: 'run', label: '当前运行', children: trackedRun.runNo },
                { key: 'state', label: '状态', children: <Tag color={RUN_STATE_COLORS[trackedRun.state]}>{trackedRun.state}</Tag> },
                { key: 'produced', label: '产出版本', children: trackedRun.producedVersionNo ?? '—' },
                { key: 'error', label: '错误', children: trackedRun.errorCode ?? '—' },
              ]}
            />
          )}
        </Space>
      </Card>

      <Card title="优化运行">
        <Space orientation="vertical" style={{ width: '100%' }}>
          <Radio.Group
            optionType="button"
            value={runStateFilter}
            onChange={(event) => { setRunStateFilter(event.target.value); setRunPageNo(1) }}
            options={[
              { label: '全部', value: 'ALL' },
              ...(['PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'SKIPPED'] as const)
                .map((state) => ({ label: RUN_STATE_LABELS[state], value: state })),
            ]}
          />
          <Table<DdcAiOptimizationRunResult>
            rowKey="id"
            size="small"
            loading={runsQuery.isPending}
            columns={runColumns}
            dataSource={runsQuery.data?.records ?? []}
            pagination={{
              current: runPageNo,
              pageSize: RUN_PAGE_SIZE,
              total: runsQuery.data?.page.total ?? 0,
              onChange: setRunPageNo,
            }}
          />
        </Space>
      </Card>
    </Space>
  )
}
