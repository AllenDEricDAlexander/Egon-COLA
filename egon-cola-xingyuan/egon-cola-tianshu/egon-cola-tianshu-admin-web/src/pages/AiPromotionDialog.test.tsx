import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClientProvider } from '@tanstack/react-query'
import { App as AntdApp } from 'antd'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DdcApiError, setDdcExpectedTenantId } from '../api/client'
import type {
  DdcAiPublicationResult,
  DdcAiResourceResult,
  DdcAiResourceSummary,
  DdcAiScope,
  DdcAiVersionResult,
  DdcAiVersionSummary,
  PageResultRecord,
} from '../api/types'
import { createDdcQueryClient } from '../query/queryClient'
import AiPromotionDialog from './AiPromotionDialog'

const aiApi = vi.hoisted(() => ({
  pageAiResources: vi.fn(),
  getAiResource: vi.fn(),
  getAiVersion: vi.fn(),
  promoteAiVersion: vi.fn(),
  pageAiPublications: vi.fn(),
  ddcAiQueryKeys: {
    all: () => ['tianshu', 'tenant-1', 'ai'],
    resources: (params: unknown) => ['tianshu', 'tenant-1', 'ai', 'resources', params],
    resource: (id: string) => ['tianshu', 'tenant-1', 'ai', 'resource', id],
    version: (id: string, versionNo: string) => ['tianshu', 'tenant-1', 'ai', 'version', id, versionNo],
    publications: (id: string, params: unknown) => ['tianshu', 'tenant-1', 'ai', 'publications', id, params],
  },
}))

const requestIds = vi.hoisted(() => ({ next: 'promote_order_0001' }))
vi.mock('../api/ai', () => aiApi)
vi.mock('../lib/uuid', () => ({ uuidV7: () => requestIds.next }))
vi.mock('../components/scope/ScopeSelects', () => ({
  default: ({
    value,
    onChange,
  }: {
    value: DdcAiScope
    onChange: (next: DdcAiScope) => void
  }) => (
    <div>
      {(['bizCode', 'namespaceCode', 'env', 'appCode'] as const).map((key) => (
        <label key={key}>
          {key}
          <input
            aria-label={key}
            value={value[key]}
            onChange={(event) => onChange({ ...value, [key]: event.target.value })}
          />
        </label>
      ))}
    </div>
  ),
}))

const sourceResource: DdcAiResourceSummary = {
  id: '810001',
  scope: { bizCode: 'retail', namespaceCode: 'dev-space', env: 'dev', appCode: 'assistant-app' },
  kind: 'PROMPT',
  resourceKey: 'review-order',
  displayName: '订单审核',
  description: 'source description',
  revision: '5',
  lastVersionNo: '3',
  currentVersionId: '9003',
  currentVersionNo: '3',
  updatedAt: '2026-09-21T10:00:00Z',
}

const sourceVersion: DdcAiVersionSummary = {
  id: '9002',
  resourceId: '810001',
  versionNo: '2',
  kind: 'PROMPT',
  displayName: '订单审核稳定版',
  description: 'verified source',
  contentSha256: 'sha-source',
  contentBytes: 20,
  nodeCount: 0,
  sourceVersionId: null,
  changeNote: 'verified',
  createdAt: '2026-09-21T10:00:00Z',
  createdBy: 'admin',
}

const targetScope: DdcAiScope = {
  bizCode: 'retail',
  namespaceCode: 'prod-space',
  env: 'prod',
  appCode: 'assistant-app',
}

const page = <T,>(records: T[]): PageResultRecord<T> => ({
  success: true,
  code: 10000,
  status: 'SUCCESS',
  message: 'success',
  records,
  page: { total: records.length, pageNo: 1, pageSize: 20, pages: records.length ? 1 : 0, hasNext: false, hasPrevious: false },
  timestamp: 1,
})

const targetResource: DdcAiResourceSummary = {
  ...sourceResource,
  id: '810002',
  scope: targetScope,
  revision: '9',
  lastVersionNo: '3',
  currentVersionId: '9009',
  currentVersionNo: '3',
}

const targetResult: DdcAiResourceResult = {
  resource: targetResource,
  draft: { promptText: 'target draft only', nodes: [] },
}

const targetVersion: DdcAiVersionSummary = {
  ...sourceVersion,
  id: '9009',
  resourceId: '810002',
  versionNo: '3',
  displayName: targetResource.displayName,
  description: targetResource.description,
}

const targetVersionResult: DdcAiVersionResult = {
  version: targetVersion,
  content: { promptText: 'published target content', nodes: [] },
}

const promotionReceipt: DdcAiPublicationResult = {
  id: '95001',
  resourceId: '810002',
  operation: 'PROMOTE',
  previousVersionId: '9009',
  targetVersionId: '9010',
  targetVersionNo: '4',
  sourceVersionId: '9002',
  resourceRevision: '10',
  targetCreated: false,
  createdAt: '2026-09-22T10:00:00Z',
}

const onCancel = vi.fn()
const onPublished = vi.fn()

const renderDialog = () => {
  const queryClient = createDdcQueryClient()
  return {
    queryClient,
    ...render(
      <AntdApp>
        <QueryClientProvider client={queryClient}>
          <AiPromotionDialog
            open
            sourceResource={sourceResource}
            sourceVersion={sourceVersion}
            onCancel={onCancel}
            onPublished={onPublished}
          />
        </QueryClientProvider>
      </AntdApp>,
    ),
  }
}

const fillTarget = () => {
  fireEvent.change(screen.getByLabelText('bizCode'), { target: { value: targetScope.bizCode } })
  fireEvent.change(screen.getByLabelText('namespaceCode'), { target: { value: targetScope.namespaceCode } })
  fireEvent.change(screen.getByLabelText('env'), { target: { value: targetScope.env } })
  fireEvent.change(screen.getByLabelText('appCode'), { target: { value: targetScope.appCode } })
  fireEvent.change(screen.getByLabelText('目标资源键'), { target: { value: 'review-order' } })
  fireEvent.change(screen.getByLabelText('变更说明'), { target: { value: '推广验证版本' } })
}

describe('AiPromotionDialog', () => {
  beforeEach(() => {
    setDdcExpectedTenantId('tenant-1')
    requestIds.next = 'promote_order_0001'
    onCancel.mockReset()
    onPublished.mockReset()
    aiApi.pageAiResources.mockReset().mockResolvedValue(page([]))
    aiApi.getAiResource.mockReset().mockResolvedValue(targetResult)
    aiApi.getAiVersion.mockReset().mockResolvedValue(targetVersionResult)
    aiApi.promoteAiVersion.mockReset().mockResolvedValue(promotionReceipt)
    aiApi.pageAiPublications.mockReset().mockResolvedValue(page([]))
  })

  afterEach(() => {
    cleanup()
    setDdcExpectedTenantId(undefined)
  })

  it('publishes the exact source version to a new four-dimensional target without sending source content', async () => {
    renderDialog()
    fillTarget()
    expect(await screen.findByText(/目标资源不存在/)).toBeInTheDocument()
    expect(await screen.findByText(/固定来源版本 2/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '复制并发布' }))

    await waitFor(() => expect(aiApi.promoteAiVersion).toHaveBeenCalledWith(
      '810001',
      'promote_order_0001',
      {
        sourceVersionNo: '2',
        targetScope,
        targetResourceKey: 'review-order',
        expectedTargetRevision: null,
        changeNote: '推广验证版本',
      },
    ))
    expect(JSON.stringify(aiApi.promoteAiVersion.mock.calls[0][2])).not.toContain('source description')
    expect(onPublished).toHaveBeenCalledWith(promotionReceipt)
  })

  it('previews an existing target revision and preserves its unpublished draft', async () => {
    aiApi.pageAiResources.mockResolvedValue(page([targetResource]))
    renderDialog()
    fillTarget()

    expect(await screen.findByText(/目标revision 9/)).toBeInTheDocument()
    expect(screen.getByText(/目标存在未发布草稿/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '复制并发布' }))

    await waitFor(() => expect(aiApi.promoteAiVersion).toHaveBeenCalledWith(
      '810001',
      'promote_order_0001',
      expect.objectContaining({ expectedTargetRevision: '9', targetResourceKey: 'review-order' }),
    ))
  })

  it('keeps target input and disables submit when the target scope cannot be read', async () => {
    aiApi.pageAiResources.mockRejectedValueOnce(new DdcApiError(403, 'TIANSHU_AI_SCOPE_FORBIDDEN', 'forbidden'))
    renderDialog()
    fillTarget()

    expect(await screen.findByText('无法读取目标资源状态')).toBeInTheDocument()
    expect(screen.getByLabelText('目标资源键')).toHaveValue('review-order')
    expect(screen.getByRole('button', { name: '复制并发布' })).toBeDisabled()
    expect(aiApi.promoteAiVersion).not.toHaveBeenCalled()
  })

  it('keeps the original idempotency key and payload after an unknown result', async () => {
    aiApi.promoteAiVersion.mockRejectedValueOnce(new DdcApiError(0, 'NETWORK', 'network lost'))
    renderDialog()
    fillTarget()
    await screen.findByText(/目标资源不存在/)
    fireEvent.click(screen.getByRole('button', { name: '复制并发布' }))

    expect(await screen.findByText(/结果未知/)).toBeInTheDocument()
    expect(aiApi.promoteAiVersion).toHaveBeenCalledTimes(1)
    fireEvent.click(screen.getByRole('button', { name: '核验原请求' }))
    expect(await screen.findByText(/未找到发布回执或目标资源/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: /使用原请求号重试/ }))

    await waitFor(() => expect(aiApi.promoteAiVersion).toHaveBeenCalledTimes(2))
    expect(aiApi.promoteAiVersion.mock.calls[0]).toEqual(aiApi.promoteAiVersion.mock.calls[1])
  })
})
