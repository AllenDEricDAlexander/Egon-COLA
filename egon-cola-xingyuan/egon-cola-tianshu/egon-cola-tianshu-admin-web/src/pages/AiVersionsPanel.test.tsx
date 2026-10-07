import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { QueryClientProvider } from '@tanstack/react-query'
import { App as AntdApp } from 'antd'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DdcApiError, setDdcExpectedTenantId } from '../api/client'
import type {
  DdcAiPublicationResult,
  DdcAiResourceResult,
  DdcAiResourceSummary,
  DdcAiVersionResult,
  DdcAiVersionSummary,
  PageResultRecord,
} from '../api/types'
import { createDdcQueryClient } from '../query/queryClient'
import AiResourceEditorPage from './AiResourceEditorPage'
import AiVersionsPanel from './AiVersionsPanel'

const aiApi = vi.hoisted(() => ({
  pageAiVersions: vi.fn(),
  getAiVersion: vi.fn(),
  publishAiDraft: vi.fn(),
  rollbackAiPublication: vi.fn(),
  pageAiPublications: vi.fn(),
  getAiResource: vi.fn(),
  ddcAiQueryKeys: {
    all: () => ['tianshu', 'tenant-1', 'ai'],
    resources: (params: unknown) => ['tianshu', 'tenant-1', 'ai', 'resources', params],
    resource: (id: string) => ['tianshu', 'tenant-1', 'ai', 'resource', id],
    versions: (id: string, params: unknown) => ['tianshu', 'tenant-1', 'ai', 'versions', id, params],
    version: (id: string, versionNo: string) => ['tianshu', 'tenant-1', 'ai', 'version', id, versionNo],
    publications: (id: string, params: unknown) => ['tianshu', 'tenant-1', 'ai', 'publications', id, params],
  },
}))

const requestIds = vi.hoisted(() => ({ next: 'publish_order_0001' }))
vi.mock('../api/ai', () => aiApi)
vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ registerUnsavedChangesGuard: () => () => {}, error: undefined }),
}))
vi.mock('../lib/uuid', () => ({ uuidV7: () => requestIds.next }))
vi.mock('./AiPromotionDialog', () => ({
  default: ({
    open,
    sourceResource,
    sourceVersion,
    onCancel,
    onPublished,
  }: {
    open: boolean
    sourceResource: DdcAiResourceSummary
    sourceVersion: DdcAiVersionSummary
    onCancel: () => void
    onPublished: (result: DdcAiPublicationResult) => void
  }) => open ? (
    <div data-testid="promotion-dialog">
      <span>{sourceResource.id}</span>
      <span>{sourceVersion.versionNo}</span>
      <button onClick={onCancel}>关闭推广</button>
      <button onClick={() => onPublished({
        id: 'promotion-receipt',
        resourceId: '810002',
        operation: 'PROMOTE',
        previousVersionId: null,
        targetVersionId: '9010',
        targetVersionNo: '4',
        sourceVersionId: sourceVersion.id,
        resourceRevision: '1',
        targetCreated: true,
        createdAt: '2026-10-07T00:00:00Z',
      })}>complete-promotion</button>
    </div>
  ) : null,
}))

const resource: DdcAiResourceSummary = {
  id: '810001',
  scope: { bizCode: 'retail', namespaceCode: 'dev-space', env: 'dev', appCode: 'assistant-app' },
  kind: 'PROMPT',
  resourceKey: 'review-order',
  displayName: '订单审核',
  description: '订单说明',
  revision: '4',
  lastVersionNo: '2',
  currentVersionId: '9002',
  currentVersionNo: '2',
  updatedAt: '2026-09-21T10:00:00Z',
}

const version = (versionNo: string, id: string): DdcAiVersionSummary => ({
  id,
  resourceId: resource.id,
  versionNo,
  kind: 'PROMPT',
  displayName: '订单审核',
  description: '订单说明',
  contentSha256: `sha-${versionNo}`,
  contentBytes: 12,
  nodeCount: 0,
  sourceVersionId: versionNo === '2' ? '8001' : null,
  changeNote: `发布${versionNo}`,
  createdAt: `2026-09-2${versionNo}T10:00:00Z`,
  createdBy: 'admin',
})

const page = <T,>(records: T[]): PageResultRecord<T> => ({
  success: true,
  code: 10000,
  status: 'SUCCESS',
  message: 'success',
  records,
  page: {
    total: records.length,
    pageNo: 1,
    pageSize: 20,
    pages: records.length ? 1 : 0,
    hasNext: false,
    hasPrevious: false,
  },
  timestamp: 1,
})

const versionResult = (row: DdcAiVersionSummary): DdcAiVersionResult => ({
  version: row,
  content: { promptText: `历史文本 v${row.versionNo}\n`, nodes: [] },
})

const publication: DdcAiPublicationResult = {
  id: '95001',
  resourceId: '810001',
  operation: 'PUBLISH',
  previousVersionId: '9001',
  targetVersionId: '9002',
  targetVersionNo: '2',
  sourceVersionId: null,
  resourceRevision: '5',
  targetCreated: false,
  createdAt: '2026-09-22T10:00:00Z',
}

const resourceResult: DdcAiResourceResult = {
  resource: { ...resource, revision: '5', currentVersionId: '9002', currentVersionNo: '2' },
  draft: { promptText: '草稿内容', nodes: [] },
}

const renderPanel = (
  draftDirty = false,
  onResourceChanged = vi.fn(),
  seedQueries = false,
  onPendingOperationChange = vi.fn(),
  resourceUnavailable = false,
) => {
  const router = createMemoryRouter([
    { path: '/ai/resources/:id', element: <AiVersionsPanel resource={resource} draftDirty={draftDirty} resourceUnavailable={resourceUnavailable} onResourceChanged={onResourceChanged} onPendingOperationChange={onPendingOperationChange} /> },
  ], { initialEntries: ['/ai/resources/810001'] })
  const queryClient = createDdcQueryClient()
  if (seedQueries) {
    queryClient.setQueryData(
      aiApi.ddcAiQueryKeys.versions('810001', { pageNo: 1, pageSize: 20 }),
      page([version('2', '9002'), version('1', '9001')]),
    )
    queryClient.setQueryData(
      aiApi.ddcAiQueryKeys.publications('810001', { requestId: undefined, pageNo: 1, pageSize: 20 }),
      page([publication]),
    )
  }
  return {
    router,
    queryClient,
    onResourceChanged,
    onPendingOperationChange,
    ...render(
      <AntdApp>
        <QueryClientProvider client={queryClient}><RouterProvider router={router} /></QueryClientProvider>
      </AntdApp>,
    ),
  }
}

const renderEditor = () => {
  const router = createMemoryRouter([
    { path: '/ai/resources/:id', element: <AiResourceEditorPage /> },
    { path: '/away', element: <div>离开目标页面</div> },
  ], { initialEntries: ['/ai/resources/810001'] })
  const queryClient = createDdcQueryClient()
  return {
    router,
    ...render(
      <AntdApp>
        <QueryClientProvider client={queryClient}><RouterProvider router={router} /></QueryClientProvider>
      </AntdApp>,
    ),
  }
}

describe('AiVersionsPanel', () => {
  beforeEach(() => {
    setDdcExpectedTenantId('tenant-1')
    requestIds.next = 'publish_order_0001'
    aiApi.pageAiVersions.mockReset().mockResolvedValue(page([version('2', '9002'), version('1', '9001')]))
    aiApi.getAiVersion.mockReset().mockImplementation(async (_id: string, versionNo: string) => versionResult(version(versionNo, `900${versionNo}`)))
    aiApi.publishAiDraft.mockReset().mockResolvedValue(publication)
    aiApi.rollbackAiPublication.mockReset().mockResolvedValue({
      ...publication,
      id: '95002',
      operation: 'ROLLBACK',
      previousVersionId: '9002',
      targetVersionId: '9001',
      targetVersionNo: '1',
      resourceRevision: '5',
    })
    aiApi.pageAiPublications.mockReset().mockResolvedValue(page([publication]))
    aiApi.getAiResource.mockReset().mockResolvedValue(resourceResult)
  })

  afterEach(() => {
    cleanup()
    setDdcExpectedTenantId(undefined)
  })

  it('loads version summaries and exact historical content as read-only with versionNo in the URL', async () => {
    const { router } = renderPanel()

    await waitFor(() => expect(aiApi.pageAiVersions).toHaveBeenCalled())
    expect(await screen.findByText('发布2')).toBeInTheDocument()
    expect(screen.getByText('当前发布')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: /读取版本 1/ }))

    const historicalPrompt = await screen.findByLabelText('历史 Prompt 正文 v1')
    expect(historicalPrompt).toHaveValue('历史文本 v1\n')
    expect(historicalPrompt).toBeDisabled()
    await waitFor(() => expect(router.state.location.search).toContain('versionNo=1'))
    expect(aiApi.getAiVersion).toHaveBeenCalledWith('810001', '1', expect.objectContaining({ signal: expect.any(AbortSignal) }))
    expect(await screen.findByText('PUBLISH')).toBeInTheDocument()
  }, 10000)

  it('does not present a denied version history as empty', async () => {
    aiApi.pageAiVersions.mockRejectedValueOnce(new DdcApiError(403, 'TIANSHU_AI_SCOPE_FORBIDDEN', 'forbidden'))
    renderPanel()

    expect(await screen.findAllByText('版本列表读取失败')).toHaveLength(2)
    expect(screen.queryByText('暂无已发布版本')).not.toBeInTheDocument()
  })

  it('keeps publishing disabled for dirty drafts but allows rollback without replacing the draft buffer', async () => {
    requestIds.next = 'rollback_order_0001'
    const {onResourceChanged} = renderPanel(true, vi.fn(), true)
    expect(screen.getByText('发布2')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /发布当前草稿/ })).toBeDisabled()
    expect(aiApi.publishAiDraft).not.toHaveBeenCalled()

    fireEvent.click(screen.getByRole('button', { name: /回滚到版本 1/ }))
    const rollbackDialog = await screen.findByRole('dialog')
    fireEvent.click(within(rollbackDialog).getByRole('button', { name: /确认回滚/ }))
    await waitFor(() => expect(aiApi.rollbackAiPublication).toHaveBeenCalledWith(
      '810001',
      'rollback_order_0001',
      { expectedRevision: '4', versionNo: '1', changeNote: expect.any(String) },
    ))
    expect(onResourceChanged).toHaveBeenCalledWith(resourceResult)
  }, 15000)

  it('disables publish, rollback and promotion when the resource session cannot refresh', async () => {
    const { container } = renderPanel(false, vi.fn(), true, vi.fn(), true)
    expect(container.textContent).toContain('发布2')
    const buttons = [...container.querySelectorAll('button')]
    expect(buttons.find((button) => button.textContent?.includes('发布当前草稿'))).toBeDisabled()
    expect(buttons.find((button) => button.textContent?.includes('回滚到版本 1'))).toBeDisabled()
    expect(buttons.filter((button) => button.textContent?.includes('复制发布')).every((button) => button.disabled)).toBe(true)
  })

  it('publishes a saved draft with the observed decimal revision and an idempotency key', async () => {
    const {onResourceChanged} = renderPanel(false, vi.fn(), true)
    expect(screen.getByText('发布2')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /发布当前草稿/ }))
    expect(await screen.findByText('确认发布当前草稿？')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '确认发布' }))

    await waitFor(() => expect(aiApi.publishAiDraft).toHaveBeenCalledWith(
      '810001',
      'publish_order_0001',
      { expectedRevision: '4', changeNote: expect.any(String) },
    ))
    expect(onResourceChanged).toHaveBeenCalledWith(resourceResult)
  })

  it('keeps and reuses the same publish request id after an unknown result', async () => {
    aiApi.publishAiDraft.mockRejectedValueOnce(new DdcApiError(0, 'NETWORK', 'network lost'))
    aiApi.pageAiPublications.mockResolvedValue(page([]))
    const { onPendingOperationChange } = renderPanel(false, vi.fn(), true)
    expect(screen.getByRole('button', { name: /发布当前草稿/ })).toBeEnabled()
    fireEvent.click(screen.getByRole('button', { name: /发布当前草稿/ }))
    fireEvent.click(await screen.findByRole('button', { name: '确认发布' }))

    expect(await screen.findByText(/原请求号已保留/)).toBeInTheDocument()
    expect(onPendingOperationChange).toHaveBeenLastCalledWith(true)
    fireEvent.click(screen.getByRole('button', { name: /使用原请求号重试/ }))

    await waitFor(() => expect(aiApi.publishAiDraft).toHaveBeenCalledTimes(2))
    expect(aiApi.publishAiDraft.mock.calls[0]).toEqual(aiApi.publishAiDraft.mock.calls[1])
    expect(onPendingOperationChange).toHaveBeenLastCalledWith(false)
  }, 15000)

  it('opens promotion with the exact source resource id and immutable version number', async () => {
    const { router } = renderPanel(false, vi.fn(), true)
    await screen.findByText('发布2')
    fireEvent.click(screen.getAllByRole('button', { name: '复制发布' })[0])

    const dialog = await screen.findByTestId('promotion-dialog')
    expect(within(dialog).getByText('810001')).toBeInTheDocument()
    expect(within(dialog).getByText('2')).toBeInTheDocument()
    fireEvent.click(within(dialog).getByRole('button', { name: 'complete-promotion' }))
    expect(await screen.findByText(/PROMOTE 已提交，目标版本 4/)).toBeInTheDocument()
    expect(screen.getByText('已创建独立目标资源')).toBeInTheDocument()
    expect(screen.getByText(/来源版本ID 9002/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '查看目标版本' }))
    expect(router.state.location.pathname).toBe('/ai/resources/810002')
    expect(router.state.location.search).toBe('?versionNo=4')
  }, 10000)

  it('selects history through versionNo while keeping the local draft buffer intact', async () => {
    const { router } = renderEditor()
    const draft = await screen.findByLabelText('Prompt 正文')
    fireEvent.change(draft, { target: { value: '未保存的编辑缓冲' } })

    fireEvent.click(screen.getByRole('tab', { name: '版本与发布' }))

    expect(await screen.findByLabelText('历史 Prompt 正文 v2')).toBeDisabled()
    expect(screen.getByLabelText('历史 Prompt 正文 v2')).toHaveValue('历史文本 v2\n')
    expect(router.state.location.search).toContain('versionNo=2')
    fireEvent.click(screen.getByRole('tab', { name: '草稿编辑' }))
    expect(screen.getByLabelText('Prompt 正文')).toHaveValue('未保存的编辑缓冲')
  }, 10000)

})
