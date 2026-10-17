import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DdcApiError, setDdcExpectedTenantId } from '../api/client'
import type {
  DdcAiOptimizationPolicyResult,
  DdcAiOptimizationRunResult,
  PageResultRecord,
} from '../api/types'
import { renderWithQueryClient } from '../test/renderWithQueryClient'
import AiOptimizationPanel from './AiOptimizationPanel'

const aiApi = vi.hoisted(() => ({
  getAiOptimizationPolicy: vi.fn(),
  replaceAiOptimizationPolicy: vi.fn(),
  triggerAiOptimizationRun: vi.fn(),
  pageAiOptimizationRuns: vi.fn(),
  getAiOptimizationRun: vi.fn(),
  ddcAiQueryKeys: {
    optimizationPolicy: (id: string) => ['tianshu', 'tenant-1', 'ai', 'optimization-policy', id],
    optimizationRunsAll: (id: string) => ['tianshu', 'tenant-1', 'ai', 'optimization-runs', id],
    optimizationRuns: (id: string, params: unknown) => ['tianshu', 'tenant-1', 'ai', 'optimization-runs', id, params],
    optimizationRun: (id: string, runId: string) => ['tianshu', 'tenant-1', 'ai', 'optimization-run', id, runId],
  },
}))
vi.mock('../api/ai', () => aiApi)
vi.mock('../lib/uuid', () => ({ uuidV7: () => '0199f0a0-0000-7000-8000-000000000001' }))

const policy = (overrides: Partial<DdcAiOptimizationPolicyResult> = {}): DdcAiOptimizationPolicyResult => ({
  resourceId: '810001',
  persisted: false,
  revision: '0',
  enabled: false,
  scoreThreshold: 80,
  minSamples: 20,
  evaluationWindow: 'PT24H',
  maxIterations: 5,
  cooldown: 'PT1H',
  autoPublish: false,
  judgeEnabled: true,
  optimizerModelAlias: 'gpt-opt',
  judgeModelAlias: 'gpt-judge',
  optimizerInstruction: '',
  state: 'ARMED',
  iterationsDone: 0,
  consecutiveFailures: 0,
  activeRunId: null,
  convergedAt: null,
  evaluation: null,
  ...overrides,
})

const run = (overrides: Partial<DdcAiOptimizationRunResult> = {}): DdcAiOptimizationRunResult => ({
  id: '88',
  resourceId: '810001',
  policyId: '3',
  runNo: '4',
  iterationNo: 1,
  trigger: 'MANUAL',
  state: 'PENDING',
  baseVersionId: '9',
  baseVersionNo: '3',
  sampleCount: 24,
  averageScore: '55.25',
  thresholdSnapshot: 80,
  windowStart: '2026-10-16T06:00:00Z',
  windowEnd: '2026-10-17T06:00:00Z',
  modelAlias: 'gpt-opt',
  promptTokens: null,
  completionTokens: null,
  producedVersionId: null,
  producedVersionNo: null,
  publicationId: null,
  autoPublished: false,
  errorCode: null,
  errorMessage: null,
  requestId: '0199f0a0-0000-7000-8000-000000000001',
  note: '',
  startedAt: null,
  finishedAt: null,
  createdAt: '2026-10-17T06:00:00Z',
  createdBy: 'user-1',
  ...overrides,
})

const page = (records: DdcAiOptimizationRunResult[]): PageResultRecord<DdcAiOptimizationRunResult> => ({
  success: true,
  code: 10000,
  status: 'SUCCESS',
  message: 'success',
  records,
  page: { total: records.length, pageNo: 1, pageSize: 10, pages: 1, hasNext: false, hasPrevious: false },
  traceId: 'trace',
  timestamp: 1,
} as PageResultRecord<DdcAiOptimizationRunResult>)

const renderPanel = () => renderWithQueryClient(<AiOptimizationPanel resourceId="810001" pollIntervalMs={20} />)

describe('AiOptimizationPanel', () => {
  beforeEach(() => {
    setDdcExpectedTenantId('tenant-1')
    aiApi.pageAiOptimizationRuns.mockResolvedValue(page([]))
  })

  afterEach(() => {
    cleanup()
    vi.clearAllMocks()
    setDdcExpectedTenantId(undefined)
  })

  it('renders default policy and saves with revision 0', async () => {
    aiApi.getAiOptimizationPolicy.mockResolvedValue(policy())
    aiApi.replaceAiOptimizationPolicy.mockResolvedValue(policy({ persisted: true, revision: '1', enabled: true }))
    renderPanel()

    expect(await screen.findByText('未保存（默认值）')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('switch', { name: '启用自动优化' }))
    fireEvent.click(screen.getByRole('button', { name: '保存策略' }))

    await waitFor(() => expect(aiApi.replaceAiOptimizationPolicy).toHaveBeenCalledTimes(1))
    expect(aiApi.replaceAiOptimizationPolicy.mock.calls[0][0]).toBe('810001')
    expect(aiApi.replaceAiOptimizationPolicy.mock.calls[0][1]).toMatchObject({
      expectedRevision: '0', rearm: false, enabled: true, evaluationWindow: 'PT24H', cooldown: 'PT1H',
    })
    expect(await screen.findByText('1')).toBeInTheDocument()
  })

  it('shows 409 conflict and keeps form values', async () => {
    aiApi.getAiOptimizationPolicy.mockResolvedValue(policy({ persisted: true, revision: '3' }))
    aiApi.replaceAiOptimizationPolicy.mockRejectedValue(new DdcApiError(409, '56303', 'stale'))
    renderPanel()

    const threshold = await screen.findByRole('spinbutton', { name: '分数阈值' })
    fireEvent.change(threshold, { target: { value: '70' } })
    fireEvent.click(screen.getByRole('button', { name: '保存策略' }))

    expect(await screen.findByText(/策略已被他人修改/)).toBeInTheDocument()
    expect(screen.getByRole('spinbutton', { name: '分数阈值' })).toHaveValue('70')
    expect(aiApi.replaceAiOptimizationPolicy.mock.calls[0][1]).toMatchObject({ expectedRevision: '3', scoreThreshold: 70 })
  })

  it('blocks invalid durations before calling the server', async () => {
    aiApi.getAiOptimizationPolicy.mockResolvedValue(policy())
    renderPanel()

    fireEvent.change(await screen.findByRole('textbox', { name: '评估窗口' }), { target: { value: 'PT1M' } })

    expect(screen.getByText(/评估窗口须为 PT5M 到 P30D/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '保存策略' })).toBeDisabled()
  })

  it('rearm disabled while run active', async () => {
    aiApi.getAiOptimizationPolicy.mockResolvedValue(policy({
      persisted: true, revision: '5', state: 'EXHAUSTED', activeRunId: '77',
    }))
    aiApi.getAiOptimizationRun.mockResolvedValue(run({ id: '77', state: 'RUNNING' }))
    renderPanel()

    expect(await screen.findByText('RUNNING')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByRole('button', { name: '立即优化一次' })).toBeDisabled())
    expect(screen.getByRole('button', { name: '保存并重新启用' })).toBeDisabled()
  })

  it('trigger posts an Idempotency-Key and polls until terminal state', async () => {
    aiApi.getAiOptimizationPolicy.mockResolvedValue(policy({ persisted: true, revision: '2' }))
    aiApi.triggerAiOptimizationRun.mockResolvedValue(run({ state: 'PENDING' }))
    aiApi.getAiOptimizationRun
      .mockResolvedValueOnce(run({ state: 'RUNNING' }))
      .mockResolvedValue(run({ state: 'SUCCEEDED', producedVersionId: '10', producedVersionNo: '4' }))
    renderPanel()

    fireEvent.change(await screen.findByRole('textbox', { name: '运行备注' }), { target: { value: 'manual' } })
    fireEvent.click(screen.getByRole('button', { name: '立即优化一次' }))

    await waitFor(() => expect(aiApi.triggerAiOptimizationRun).toHaveBeenCalledTimes(1))
    expect(aiApi.triggerAiOptimizationRun.mock.calls[0][1]).toMatch(/^[A-Za-z0-9_-]{16,64}$/)
    expect(aiApi.triggerAiOptimizationRun.mock.calls[0][2]).toEqual({ note: 'manual' })
    expect(await screen.findByText('SUCCEEDED', {}, { timeout: 2000 })).toBeInTheDocument()
    const polls = aiApi.getAiOptimizationRun.mock.calls.length
    await new Promise((resolve) => setTimeout(resolve, 100))
    expect(aiApi.getAiOptimizationRun.mock.calls.length).toBe(polls)
    expect(aiApi.getAiOptimizationPolicy.mock.calls.length).toBeGreaterThan(1)
  })

  it('shows channel disabled banner on 503', async () => {
    aiApi.getAiOptimizationPolicy.mockResolvedValue(policy({ persisted: true, revision: '2' }))
    aiApi.triggerAiOptimizationRun.mockRejectedValue(new DdcApiError(503, '56322', 'disabled'))
    renderPanel()

    fireEvent.click(await screen.findByRole('button', { name: '立即优化一次' }))

    expect(await screen.findByText(/优化通道未启用/)).toBeInTheDocument()
  })

  it('filters runs by state', async () => {
    aiApi.getAiOptimizationPolicy.mockResolvedValue(policy({ persisted: true, revision: '2' }))
    aiApi.pageAiOptimizationRuns.mockResolvedValue(page([run({ state: 'FAILED', errorCode: 'MODEL_TIMEOUT' })]))
    renderPanel()

    fireEvent.click(await screen.findByRole('radio', { name: '失败' }))

    await waitFor(() => expect(aiApi.pageAiOptimizationRuns).toHaveBeenLastCalledWith('810001',
      { state: 'FAILED', pageNo: 1, pageSize: 10 }, expect.anything()))
    expect(await screen.findByText('MODEL_TIMEOUT')).toBeInTheDocument()
  })
})
