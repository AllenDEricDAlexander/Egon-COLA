import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { QueryClientProvider } from '@tanstack/react-query'
import { App as AntdApp } from 'antd'
import { createMemoryRouter, Link, Outlet, RouterProvider } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DdcApiError, setDdcExpectedTenantId } from '../api/client'
import type { DdcAiContent, DdcAiResourceResult } from '../api/types'
import { createDdcQueryClient } from '../query/queryClient'
import AdminLayout from '../layouts/AdminLayout'
import AiResourceEditorPage from './AiResourceEditorPage'

const aiApi = vi.hoisted(() => ({
  getAiResource: vi.fn(),
  replaceAiDraft: vi.fn(),
  deleteAiResource: vi.fn(),
  ddcAiQueryKeys: {
    all: () => ['tianshu', 'tenant-1', 'ai'],
    resource: (id: string) => ['tianshu', 'tenant-1', 'ai', 'resource', id],
  },
}))
const authContext = vi.hoisted(() => {
  const guards = new Set<() => Promise<boolean>>()
  const logoutCompletion = vi.fn()
  const registerUnsavedChangesGuard = vi.fn((guard: () => Promise<boolean>) => {
    guards.add(guard)
    return () => {
      guards.delete(guard)
    }
  })
  const logout = vi.fn(async () => {
    for (const guard of guards) {
      if (!(await guard())) return
    }
    logoutCompletion()
  })
  return {guards, logout, logoutCompletion, registerUnsavedChangesGuard}
})

vi.mock('../api/ai', () => aiApi)
vi.mock('./AiOptimizationPanel', () => ({
  default: ({ resourceId }: { resourceId: string }) => <div data-testid="optimization-panel">{resourceId}</div>,
}))
vi.mock('./AiFeedbackPanel', () => ({
  default: ({ resourceId }: { resourceId: string }) => <div data-testid="feedback-panel">{resourceId}</div>,
}))
vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({
    identity: 'admin',
    logout: authContext.logout,
    registerUnsavedChangesGuard: authContext.registerUnsavedChangesGuard,
  }),
}))

const promptResult = (revision: string, promptText: string): DdcAiResourceResult => ({
  resource: {
    id: '810001',
    scope: { bizCode: 'retail', namespaceCode: 'dev-space', env: 'dev', appCode: 'assistant-app' },
    kind: 'PROMPT',
    resourceKey: 'review-order',
    displayName: '订单审核',
    description: '提示词',
    revision,
    lastVersionNo: '0',
    currentVersionId: null,
    currentVersionNo: null,
    updatedAt: '2026-09-21T10:00:00Z',
  },
  draft: { promptText, nodes: [] },
})

const skillResult = (nodes: DdcAiContent['nodes']): DdcAiResourceResult => ({
  resource: {
    ...promptResult('2', '').resource,
    kind: 'SKILL',
    resourceKey: 'order-review',
    displayName: '订单审核 Skill',
  },
  draft: { promptText: null, nodes },
})

const renderEditor = (id = '810001') => {
  const router = createMemoryRouter([
    {
      path: '/',
      element: <OutletHarness />,
      children: [
        { path: 'ai/resources/:id', element: <AiResourceEditorPage /> },
        { path: 'away', element: <div>离开目标页面</div> },
      ],
    },
  ], { initialEntries: [`/ai/resources/${id}`] })
  const queryClient = createDdcQueryClient()
  const view = render(
    <AntdApp>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </AntdApp>,
  )
  return { router, queryClient, ...view }
}

const renderEditorWithLayout = () => {
  const router = createMemoryRouter([{
    path: '/',
    element: <AdminLayout />,
    children: [
      { path: 'ai/resources/:id', element: <AiResourceEditorPage /> },
    ],
  }], { initialEntries: ['/ai/resources/810001'] })
  const queryClient = createDdcQueryClient()
  return render(
    <AntdApp>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </AntdApp>,
  )
}

const OutletHarness = () => {
  return <><div><Link to="/away">离开到另一页</Link></div><Outlet /></>
}

describe('AiResourceEditorPage', () => {
  beforeEach(() => {
    setDdcExpectedTenantId('tenant-1')
    authContext.logout.mockClear()
    authContext.logoutCompletion.mockReset()
    authContext.registerUnsavedChangesGuard.mockClear()
    authContext.guards.clear()
    aiApi.getAiResource.mockReset().mockResolvedValue(promptResult('3', '原文\n'))
    aiApi.replaceAiDraft.mockReset().mockImplementation(async (_id: string, command: { expectedRevision: string; draft: DdcAiContent }) => ({
      ...promptResult('4', command.draft.promptText ?? ''),
      resource: { ...promptResult('4', '').resource, revision: '4' },
    }))
    aiApi.deleteAiResource.mockReset().mockResolvedValue(undefined)
  })

  afterEach(() => {
    cleanup()
    setDdcExpectedTenantId(undefined)
  })

  it('shows the optimization tab and keeps the draft buffer when switching back', async () => {
    const { router } = renderEditor()
    const textarea = await screen.findByLabelText('Prompt 正文')
    fireEvent.change(textarea, { target: { value: '未保存的编辑' } })

    fireEvent.click(screen.getByRole('tab', { name: '反馈与优化' }))

    expect(await screen.findByTestId('optimization-panel')).toHaveTextContent('810001')
    expect(screen.getByTestId('feedback-panel')).toHaveTextContent('810001')
    expect(router.state.location.search).not.toContain('versionNo')

    fireEvent.click(screen.getByRole('tab', { name: '草稿编辑' }))
    expect(screen.getByLabelText('Prompt 正文')).toHaveValue('未保存的编辑')
  })

  it('saves the full Prompt draft using the loaded decimal revision and exact text', async () => {
    renderEditor()
    const textarea = await screen.findByLabelText('Prompt 正文')
    fireEvent.change(textarea, { target: { value: '  保留空格\n和换行  ' } })
    fireEvent.click(screen.getByRole('button', { name: '保存草稿' }))

    await waitFor(() => expect(aiApi.replaceAiDraft).toHaveBeenCalledWith('810001', expect.objectContaining({
      expectedRevision: '3',
      displayName: '订单审核',
      description: '提示词',
      draft: { promptText: '  保留空格\n和换行  ', nodes: [] },
    })))
  })

  it('retains local content after 409 and requires explicit acknowledgement before using the refreshed revision', async () => {
    aiApi.replaceAiDraft.mockRejectedValueOnce(new DdcApiError(409, '56303', 'revision conflict'))
    aiApi.getAiResource
      .mockResolvedValueOnce(promptResult('3', '原文\n'))
      .mockResolvedValueOnce(promptResult('4', '服务端新版本\n'))
    renderEditor()
    const textarea = await screen.findByLabelText('Prompt 正文')
    fireEvent.change(textarea, { target: { value: '我的本地修改\n' } })
    fireEvent.click(screen.getByRole('button', { name: '保存草稿' }))

    expect(await screen.findByText(/服务器修订 4 已加载/)).toBeInTheDocument()
    expect(screen.getByLabelText('Prompt 正文')).toHaveValue('我的本地修改\n')
    expect(screen.getByText('服务端新版本')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '我已比较，继续保存本地修改' })).toBeInTheDocument()
    expect(aiApi.replaceAiDraft).toHaveBeenCalledTimes(1)

    fireEvent.click(screen.getByRole('button', { name: '我已比较，继续保存本地修改' }))
    fireEvent.click(screen.getByRole('button', { name: '保存草稿' }))
    await waitFor(() => expect(aiApi.replaceAiDraft).toHaveBeenLastCalledWith('810001', expect.objectContaining({
      expectedRevision: '4',
      draft: { promptText: '我的本地修改\n', nodes: [] },
    })))
  })

  it('keeps the dirty editor visible and disables writes when tenant refresh is declined', async () => {
    const {queryClient} = renderEditor()
    const textarea = await screen.findByLabelText('Prompt 正文')
    fireEvent.change(textarea, { target: { value: '本地未保存内容' } })
    expect(authContext.guards.size).toBe(1)
    aiApi.getAiResource.mockRejectedValueOnce(new DdcApiError(
      403,
      'TIANSHU_ADMIN_TENANT_MISMATCH',
      'TIANSHU_ADMIN_TENANT_MISMATCH',
    ))

    await queryClient.invalidateQueries({
      queryKey: aiApi.ddcAiQueryKeys.resource('810001'),
    })

    expect(await screen.findByText('当前会话无法刷新资源状态；本地编辑仍保留')).toBeInTheDocument()
    expect(screen.getByLabelText('Prompt 正文')).toHaveValue('本地未保存内容')
    expect(screen.getByRole('button', { name: '保存草稿' })).toBeDisabled()
  })

  it('edits Skill files as plain text with tree add, move and rename', async () => {
    aiApi.getAiResource.mockResolvedValueOnce(skillResult([
      { path: '', nodeType: 'DIRECTORY', content: null },
      { path: 'SKILL.md', nodeType: 'FILE', content: '# Skill\n' },
      { path: 'guides', nodeType: 'DIRECTORY', content: null },
      { path: 'guides/old.txt', nodeType: 'FILE', content: 'old' },
    ]))
    const { container } = renderEditor()

    expect(await screen.findByText('SKILL.md')).toBeInTheDocument()
    expect(container.querySelector('svg[data-icon="folder"]')).toBeInTheDocument()
    expect(container.querySelector('svg[data-icon="file-text"]')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '新增目录' }))
    fireEvent.change(screen.getByLabelText('目录路径'), { target: { value: 'docs' } })
    fireEvent.click(screen.getByRole('button', { name: '确认新增' }))
    fireEvent.click(screen.getByRole('button', { name: '新增文件' }))
    fireEvent.change(screen.getByLabelText('文件路径'), { target: { value: 'docs/new.txt' } })
    fireEvent.click(screen.getByRole('button', { name: '确认新增' }))
    const fileArea = await screen.findByLabelText('文件正文: docs/new.txt')
    fireEvent.change(fileArea, { target: { value: '纯文本\n不解析 Markdown' } })

    fireEvent.click(screen.getByRole('button', { name: /移\s*动/ }))
    fireEvent.change(screen.getByLabelText('目标目录路径'), { target: { value: '' } })
    fireEvent.click(screen.getByRole('button', { name: '确认移动' }))
    fireEvent.click(screen.getByRole('button', { name: '重命名' }))
    fireEvent.change(screen.getByLabelText('新路径'), { target: { value: 'guide.txt' } })
    fireEvent.click(screen.getByRole('button', { name: '确认重命名' }))

    fireEvent.click(screen.getByRole('button', { name: '保存草稿' }))
    await waitFor(() => expect(aiApi.replaceAiDraft).toHaveBeenCalledWith('810001', expect.objectContaining({
      expectedRevision: '2',
      draft: {
        promptText: null,
        nodes: expect.arrayContaining([
          { path: '', nodeType: 'DIRECTORY', content: null },
          { path: 'SKILL.md', nodeType: 'FILE', content: '# Skill\n' },
          { path: 'guide.txt', nodeType: 'FILE', content: '纯文本\n不解析 Markdown' },
          { path: 'docs', nodeType: 'DIRECTORY', content: null },
          { path: 'guides', nodeType: 'DIRECTORY', content: null },
          { path: 'guides/old.txt', nodeType: 'FILE', content: 'old' },
        ]),
      },
    })))
  })

  it('confirms recursive deletion of a Skill directory and its descendant nodes', async () => {
    aiApi.getAiResource.mockResolvedValueOnce(skillResult([
      { path: '', nodeType: 'DIRECTORY', content: null },
      { path: 'SKILL.md', nodeType: 'FILE', content: '# Skill\n' },
      { path: 'guides', nodeType: 'DIRECTORY', content: null },
      { path: 'guides/old.txt', nodeType: 'FILE', content: 'old' },
    ]))
    renderEditor()
    await screen.findByText('SKILL.md')
    fireEvent.click(screen.getByText('guides（目录）'))
    fireEvent.click(screen.getByRole('button', { name: '删除节点' }))

    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText(/将删除 2 个节点/)).toBeInTheDocument()
    fireEvent.click(within(dialog).getByRole('button', { name: '确认删除节点' }))
    expect(screen.queryByText('guides（目录）')).not.toBeInTheDocument()
  })

  it('blocks route changes and page exit while the local buffer is dirty', async () => {
    renderEditor()
    fireEvent.change(await screen.findByLabelText('Prompt 正文'), { target: { value: '未保存文字' } })

    const beforeUnload = new Event('beforeunload', { cancelable: true })
    window.dispatchEvent(beforeUnload)
    expect(beforeUnload.defaultPrevented).toBe(true)

    fireEvent.click(screen.getByRole('link', { name: '离开到另一页' }))
    expect(await screen.findByText('离开编辑页？')).toBeInTheDocument()
    expect(screen.getByLabelText('Prompt 正文')).toHaveValue('未保存文字')
    fireEvent.click(screen.getByRole('button', { name: '继续编辑' }))
    expect(screen.queryByText('离开目标页面')).not.toBeInTheDocument()
  })

  it('asks before logout and discards the dirty buffer only after confirmation', async () => {
    window.matchMedia = vi.fn().mockImplementation(() => ({
      matches: true,
      media: '',
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    }))
    renderEditorWithLayout()
    fireEvent.change(await screen.findByLabelText('Prompt 正文'), { target: { value: '未保存内容' } })
    expect(await screen.findByText('未保存')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '用户菜单' }))
    fireEvent.click(await screen.findByText('退出登录'))
    expect(await screen.findByText(/切换或清除当前身份会话/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '继续编辑' }))
    expect(authContext.logoutCompletion).not.toHaveBeenCalled()

    cleanup()
    renderEditorWithLayout()
    fireEvent.change(await screen.findByLabelText('Prompt 正文'), { target: { value: '未保存内容' } })
    await screen.findByText('未保存')
    fireEvent.click(screen.getByRole('button', { name: '用户菜单' }))
    fireEvent.click(await screen.findByText('退出登录'))
    fireEvent.click(await screen.findByRole('button', { name: '放弃修改并继续' }))
    await waitFor(() => expect(authContext.logoutCompletion).toHaveBeenCalledTimes(1))
  })
})
