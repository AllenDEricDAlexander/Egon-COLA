import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { QueryClientProvider } from '@tanstack/react-query'
import { App as AntdApp } from 'antd'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DdcApiError, setDdcExpectedTenantId } from '../api/client'
import type { DdcAiResourceResult, DdcAiResourceSummary, PageResultRecord } from '../api/types'
import { createDdcQueryClient } from '../query/queryClient'
import AiResourcesPage from './AiResourcesPage'

const aiApi = vi.hoisted(() => ({
  pageAiResources: vi.fn(),
  createAiResource: vi.fn(),
  deleteAiResource: vi.fn(),
  ddcAiQueryKeys: {
    all: () => ['tianshu', 'tenant-1', 'ai'],
    resources: (params: unknown) => ['tianshu', 'tenant-1', 'ai', 'resources', params],
  },
}))

vi.mock('../api/ai', () => aiApi)
vi.mock('../components/scope/ScopeSelects', () => ({
  default: ({
    value,
    onChange,
  }: {
    value: { bizCode: string; namespaceCode: string; env: string; appCode: string }
    onChange: (next: { bizCode: string; namespaceCode: string; env: string; appCode: string }) => void
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

const scope = {
  bizCode: 'retail',
  namespaceCode: 'dev-space',
  env: 'dev',
  appCode: 'assistant-app',
}

const resource: DdcAiResourceSummary = {
  id: '810001',
  scope,
  kind: 'PROMPT',
  resourceKey: 'review-order',
  displayName: '订单审核',
  description: '文本提示词',
  revision: '0',
  lastVersionNo: '0',
  currentVersionId: null,
  currentVersionNo: null,
  updatedAt: '2026-09-21T10:00:00Z',
}

const page = (records: DdcAiResourceSummary[]): PageResultRecord<DdcAiResourceSummary> => ({
  success: true,
  code: 10000,
  status: 'SUCCESS',
  message: 'success',
  records,
  page: {
    total: records.length,
    pageNo: 1,
    pageSize: 20,
    pages: records.length === 0 ? 0 : 1,
    hasNext: false,
    hasPrevious: false,
  },
  timestamp: 1,
})

const createdResource: DdcAiResourceResult = {
  resource: { ...resource, id: '810002', resourceKey: 'new-prompt' },
  draft: { promptText: '  原文  \n', nodes: [] },
}

const renderPage = (entry = '/ai/prompts') => {
  const router = createMemoryRouter([
    { path: '/ai/prompts', element: <AiResourcesPage kind="PROMPT" /> },
    { path: '/ai/skills', element: <AiResourcesPage kind="SKILL" /> },
    { path: '/ai/resources/:id', element: <div>资源编辑页</div> },
  ], { initialEntries: [entry] })
  const queryClient = createDdcQueryClient()
  return {
    router,
    queryClient,
    ...renderWithProviders(<RouterProvider router={router} />, queryClient),
  }
}

const renderWithProviders = (ui: ReactNode, queryClient: ReturnType<typeof createDdcQueryClient>) =>
  render(<AntdApp><QueryClientProvider client={queryClient}>{ui}</QueryClientProvider></AntdApp>)

describe('AiResourcesPage', () => {
  beforeEach(() => {
    setDdcExpectedTenantId('tenant-1')
    aiApi.pageAiResources.mockReset().mockResolvedValue(page([]))
    aiApi.createAiResource.mockReset().mockResolvedValue(createdResource)
    aiApi.deleteAiResource.mockReset().mockResolvedValue(undefined)
  })

  afterEach(() => {
    cleanup()
    setDdcExpectedTenantId(undefined)
  })

  it('queries a full four-dimensional scope and keeps an empty result distinct from an error', async () => {
    aiApi.pageAiResources.mockResolvedValue(page([resource]))
    renderPage('/ai/prompts?bizCode=retail&namespaceCode=dev-space&env=dev&appCode=assistant-app&resourceKeyPrefix=review')

    expect(await screen.findByText('订单审核')).toBeInTheDocument()
    expect(aiApi.pageAiResources).toHaveBeenCalledWith(
      expect.objectContaining({ ...scope, kind: 'PROMPT', resourceKeyPrefix: 'review', pageNo: 1, pageSize: 20 }),
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    )

    cleanup()
    aiApi.pageAiResources.mockResolvedValue(page([]))
    renderPage('/ai/prompts?bizCode=retail&namespaceCode=dev-space&env=dev&appCode=assistant-app')
    expect(await screen.findByText('当前范围暂无 Prompt 资源')).toBeInTheDocument()
    expect(screen.queryByText(/未发布正文/)).not.toBeInTheDocument()
  })

  it('creates a Prompt draft with the selected scope and preserves text exactly', async () => {
    const { router } = renderPage('/ai/prompts?bizCode=retail&namespaceCode=dev-space&env=dev&appCode=assistant-app')

    await screen.findByText('当前范围暂无 Prompt 资源')
    fireEvent.click(await screen.findByRole('button', { name: '新建 Prompt' }))
    fireEvent.change(screen.getByLabelText('资源键'), { target: { value: 'new-prompt' } })
    fireEvent.change(screen.getByLabelText('显示名称'), { target: { value: 'New prompt' } })
    fireEvent.change(screen.getByLabelText('描述'), { target: { value: 'description' } })
    fireEvent.change(screen.getByLabelText('Prompt 正文'), { target: { value: '  原文  \n' } })
    fireEvent.click(screen.getByRole('button', { name: '创建草稿' }))

    await waitFor(() => expect(aiApi.createAiResource).toHaveBeenCalledWith({
      scope,
      kind: 'PROMPT',
      resourceKey: 'new-prompt',
      displayName: 'New prompt',
      description: 'description',
      draft: { promptText: '  原文  \n', nodes: [] },
    }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/ai/resources/810002'))
  })

  it('does not present a forbidden scope as an empty list or create destination', async () => {
    aiApi.pageAiResources.mockRejectedValue(new DdcApiError(403, 'TIANSHU_AI_SCOPE_FORBIDDEN', 'forbidden'))
    renderPage('/ai/skills?bizCode=retail&namespaceCode=dev-space&env=dev&appCode=assistant-app')

    expect(await screen.findByText('当前身份无权读取此范围')).toBeInTheDocument()
    expect(screen.queryByText('当前范围暂无 Skill 资源')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '新建 Skill' })).not.toBeInTheDocument()
  })

  it('confirms logical deletion and submits the observed revision', async () => {
    aiApi.pageAiResources.mockResolvedValue(page([resource]))
    renderPage('/ai/prompts?bizCode=retail&namespaceCode=dev-space&env=dev&appCode=assistant-app')
    await screen.findByText('订单审核')

    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText(/运行读取将立即不可用，历史版本仍会保留/)).toBeInTheDocument()
    expect(aiApi.deleteAiResource).not.toHaveBeenCalled()
    fireEvent.click(within(dialog).getByRole('button', { name: '确认删除' }))

    await waitFor(() => expect(aiApi.deleteAiResource).toHaveBeenCalledWith('810001', '0'))
  })
})
