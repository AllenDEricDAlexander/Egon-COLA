import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DdcApiError, setDdcExpectedTenantId } from '../api/client'
import type { DdcAiUsageFeedbackResult, PageResultRecord } from '../api/types'
import { renderWithQueryClient } from '../test/renderWithQueryClient'
import AiFeedbackPanel from './AiFeedbackPanel'

const aiApi = vi.hoisted(() => ({
  pageAiUsageFeedback: vi.fn(),
  ddcAiQueryKeys: {
    usageFeedback: (id: string, params: unknown) => ['tianshu', 'tenant-1', 'ai', 'usage-feedback', id, params],
  },
}))
vi.mock('../api/ai', () => aiApi)

const sample = (overrides: Partial<DdcAiUsageFeedbackResult> = {}): DdcAiUsageFeedbackResult => ({
  id: '5',
  resourceId: '810001',
  versionId: '9',
  versionNo: '3',
  usageId: '01JAX0M7Q9K2R5T8V1W4X7Y0ZB',
  reporterCredentialId: 'sdk-order',
  bizCode: 'retail',
  env: 'dev',
  appCode: 'order',
  outcome: 'FAILURE',
  explicitScore: null,
  judgeScore: 40,
  effectiveScore: 40,
  scoreSource: 'JUDGE',
  judgeState: 'DONE',
  judgeAttempts: 1,
  judgeError: null,
  inputExcerpt: 'refund order 42',
  outputExcerpt: '<b>not html</b>',
  latencyMs: 812,
  occurredAt: '2026-10-17T05:00:00Z',
  traceId: 'trace-1',
  judgedAt: '2026-10-17T05:01:00Z',
  createdAt: '2026-10-17T05:00:01Z',
  ...overrides,
})

const page = (records: DdcAiUsageFeedbackResult[]): PageResultRecord<DdcAiUsageFeedbackResult> => ({
  success: true,
  code: 10000,
  status: 'SUCCESS',
  message: 'success',
  records,
  page: { total: records.length, pageNo: 1, pageSize: 20, pages: 1, hasNext: false, hasPrevious: false },
  traceId: 'trace',
  timestamp: 1,
} as PageResultRecord<DdcAiUsageFeedbackResult>)

describe('AiFeedbackPanel', () => {
  beforeEach(() => setDdcExpectedTenantId('tenant-1'))

  afterEach(() => {
    cleanup()
    vi.clearAllMocks()
    setDdcExpectedTenantId(undefined)
  })

  it('filters by version, outcome, score source and maximum score', async () => {
    aiApi.pageAiUsageFeedback.mockResolvedValue(page([sample()]))
    renderWithQueryClient(<AiFeedbackPanel resourceId="810001" />)

    expect(await screen.findByText('01JAX0M7Q9K2R5T8V1W4X7Y0ZB')).toBeInTheDocument()
    expect(aiApi.pageAiUsageFeedback.mock.calls[0][1]).toEqual({ pageNo: 1, pageSize: 20 })

    fireEvent.change(screen.getByRole('textbox', { name: '版本号' }), { target: { value: '3' } })
    fireEvent.click(screen.getByRole('radio', { name: '失败' }))
    fireEvent.click(screen.getByRole('radio', { name: 'LLM 评分' }))
    fireEvent.change(screen.getByRole('spinbutton', { name: '最高分数' }), { target: { value: '60' } })
    fireEvent.click(screen.getByRole('button', { name: /筛\s*选/ }))

    await waitFor(() => expect(aiApi.pageAiUsageFeedback).toHaveBeenLastCalledWith('810001', {
      versionNo: '3', outcome: 'FAILURE', scoreSource: 'JUDGE', maxScore: 60, pageNo: 1, pageSize: 20,
    }, expect.anything()))
  })

  it('renders excerpts read-only and marks UTC instants', async () => {
    aiApi.pageAiUsageFeedback.mockResolvedValue(page([sample()]))
    const { container } = renderWithQueryClient(<AiFeedbackPanel resourceId="810001" />)

    await screen.findByText('01JAX0M7Q9K2R5T8V1W4X7Y0ZB')
    expect(container.querySelector('[title="UTC 2026-10-17T05:00:00Z"]')).not.toBeNull()
    fireEvent.click(screen.getByRole('button', { name: /expand row|展开行/i }))

    expect(await screen.findByLabelText('输出摘录')).toHaveTextContent('<b>not html</b>')
    expect(screen.getByLabelText('输入摘录')).toHaveTextContent('refund order 42')
    expect(container.querySelector('b')).toBeNull()
  })

  it('shows a permission message on 403', async () => {
    aiApi.pageAiUsageFeedback.mockRejectedValue(new DdcApiError(403, '56307', 'forbidden'))
    renderWithQueryClient(<AiFeedbackPanel resourceId="810001" />)

    expect(await screen.findByText('当前身份无权查看使用反馈')).toBeInTheDocument()
  })
})
