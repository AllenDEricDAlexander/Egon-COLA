import { useQuery } from '@tanstack/react-query'
import { Alert, Button, Card, Input, InputNumber, Radio, Space, Table, Tag, Typography } from 'antd'
import type { TableColumnsType } from 'antd'
import { useMemo, useState } from 'react'
import { DdcApiError } from '../api/client'
import { ddcAiQueryKeys, pageAiUsageFeedback } from '../api/ai'
import type {
  DdcAiOutcome,
  DdcAiScoreSource,
  DdcAiUsageFeedbackPageParams,
  DdcAiUsageFeedbackResult,
} from '../api/types'
import { formatTime } from '../lib/query'

type Props = {
  resourceId: string
}

type Filters = {
  versionNo: string
  outcome: DdcAiOutcome | 'ALL'
  scoreSource: DdcAiScoreSource | 'ALL'
  maxScore: number | null
}

const PAGE_SIZE = 20
const EMPTY_FILTERS: Filters = { versionNo: '', outcome: 'ALL', scoreSource: 'ALL', maxScore: null }
const OUTCOME_COLORS: Record<DdcAiOutcome, string> = { SUCCESS: 'success', FAILURE: 'error', UNKNOWN: 'default' }

const failureText = (error: unknown): string => {
  if (error instanceof DdcApiError && error.status === 403) return '当前身份无权查看使用反馈'
  return error instanceof Error ? error.message : String(error)
}

const toParams = (filters: Filters, pageNo: number): DdcAiUsageFeedbackPageParams => ({
  versionNo: /^[1-9][0-9]{0,18}$/.test(filters.versionNo.trim()) ? filters.versionNo.trim() : undefined,
  outcome: filters.outcome === 'ALL' ? undefined : filters.outcome,
  scoreSource: filters.scoreSource === 'ALL' ? undefined : filters.scoreSource,
  maxScore: filters.maxScore ?? undefined,
  pageNo,
  pageSize: PAGE_SIZE,
})

const excerpt = (label: string, value: string | null) => (
  <div>
    <Typography.Text strong>{label}</Typography.Text>
    {value === null
      ? <Typography.Paragraph type="secondary">（未上报）</Typography.Paragraph>
      : <pre className="tianshu-ai-excerpt" aria-label={label} style={{ whiteSpace: 'pre-wrap', maxHeight: 240, overflow: 'auto' }}>{value}</pre>}
  </div>
)

export default function AiFeedbackPanel({ resourceId }: Props) {
  const [draft, setDraft] = useState<Filters>(EMPTY_FILTERS)
  const [applied, setApplied] = useState<Filters>(EMPTY_FILTERS)
  const [pageNo, setPageNo] = useState(1)
  const params = useMemo(() => toParams(applied, pageNo), [applied, pageNo])
  const feedbackQuery = useQuery({
    queryKey: ddcAiQueryKeys.usageFeedback(resourceId, params),
    queryFn: ({ signal }) => pageAiUsageFeedback(resourceId, params, { signal }),
  })

  const columns: TableColumnsType<DdcAiUsageFeedbackResult> = [
    {
      title: '发生时间',
      dataIndex: 'occurredAt',
      key: 'occurredAt',
      render: (value: string) => <span title={`UTC ${value}`}>{formatTime(value)}</span>,
    },
    { title: '版本', dataIndex: 'versionNo', key: 'versionNo', render: (value: string | null) => value ?? '—' },
    { title: '结果', dataIndex: 'outcome', key: 'outcome', render: (value: DdcAiOutcome) => <Tag color={OUTCOME_COLORS[value]}>{value}</Tag> },
    { title: '分数', dataIndex: 'effectiveScore', key: 'effectiveScore', render: (value: number | null) => value ?? '—' },
    { title: '分数来源', dataIndex: 'scoreSource', key: 'scoreSource' },
    { title: '评分状态', dataIndex: 'judgeState', key: 'judgeState', render: (value: string, row) =>
      row.judgeError ? <span title={row.judgeError}>{value}</span> : value },
    { title: '耗时(ms)', dataIndex: 'latencyMs', key: 'latencyMs', render: (value: number | null) => value ?? '—' },
    { title: '调用方', key: 'caller', render: (_, row) => `${row.appCode} · ${row.env}` },
    { title: 'usageId', dataIndex: 'usageId', key: 'usageId', render: (value: string) => <Typography.Text code>{value}</Typography.Text> },
  ]

  return (
    <Card title="使用反馈">
      <Space orientation="vertical" style={{ width: '100%' }}>
        <Space wrap>
          <Input aria-label="版本号" style={{ width: 100 }} placeholder="版本号" value={draft.versionNo}
            onChange={(event) => setDraft({ ...draft, versionNo: event.target.value })} />
          <Radio.Group
            optionType="button"
            aria-label="结果过滤"
            value={draft.outcome}
            onChange={(event) => setDraft({ ...draft, outcome: event.target.value })}
            options={[
              { label: '全部结果', value: 'ALL' },
              { label: '成功', value: 'SUCCESS' },
              { label: '失败', value: 'FAILURE' },
              { label: '未知', value: 'UNKNOWN' },
            ]}
          />
          <Radio.Group
            optionType="button"
            aria-label="分数来源过滤"
            value={draft.scoreSource}
            onChange={(event) => setDraft({ ...draft, scoreSource: event.target.value })}
            options={[
              { label: '全部来源', value: 'ALL' },
              { label: '显式评分', value: 'EXPLICIT' },
              { label: 'LLM 评分', value: 'JUDGE' },
              { label: '无分数', value: 'NONE' },
            ]}
          />
          <InputNumber aria-label="最高分数" min={0} max={100} placeholder="分数 ≤" value={draft.maxScore}
            onChange={(value) => setDraft({ ...draft, maxScore: value === null ? null : Number(value) })} />
          <Button type="primary" onClick={() => { setApplied(draft); setPageNo(1) }}>筛选</Button>
          <Button onClick={() => { setDraft(EMPTY_FILTERS); setApplied(EMPTY_FILTERS); setPageNo(1) }}>重置</Button>
        </Space>
        {feedbackQuery.isError && <Alert type="error" showIcon title={failureText(feedbackQuery.error)} />}
        <Table<DdcAiUsageFeedbackResult>
          rowKey="id"
          size="small"
          loading={feedbackQuery.isPending}
          columns={columns}
          dataSource={feedbackQuery.data?.records ?? []}
          expandable={{
            expandedRowRender: (row) => (
              <Space orientation="vertical" style={{ width: '100%' }}>
                {excerpt('输入摘录', row.inputExcerpt)}
                {excerpt('输出摘录', row.outputExcerpt)}
                <Typography.Text type="secondary">traceId：{row.traceId ?? '—'}；评分时间：{formatTime(row.judgedAt ?? undefined)}</Typography.Text>
              </Space>
            ),
          }}
          pagination={{
            current: pageNo,
            pageSize: PAGE_SIZE,
            total: feedbackQuery.data?.page.total ?? 0,
            onChange: setPageNo,
          }}
        />
      </Space>
    </Card>
  )
}
