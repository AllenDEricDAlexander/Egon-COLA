import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {cleanup, fireEvent, render, screen, waitFor, within} from '@testing-library/react'
import {MemoryRouter, Route, Routes} from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {GatewayApiError} from '../../api/client'
import type {KnowledgeBase, KnowledgeDocument, KnowledgeJob} from '../../api/knowledge'
import {CapabilityProvider} from '../../app/capabilities'
import {KnowledgeBasePage} from './KnowledgeBasePage'

// 原业务Spec §12.1「KB 工作区」+ §12.2 + §12.3 的 UI state 表 + TEST-010：
// tabs=documents/wiki/ask/jobs/settings；上传走 FormData；任务轮询终态即停；
// 403 清缓存、409 保留编辑、reader 只读、settings 仅 owner；员工不需要 yuheng:read。

const KB_ID = '71001'
const HASH = 'b'.repeat(64)

// 本机没有 node 可执行文件，vitest 只能跑在 bun 上，而 @testing-library/dom 的 waitFor
// 在这个组合下轮询极慢（已发生的调用要数秒后才被观察到），默认 5s 会把它误判成超时。
// 只放宽用例预算，不放宽任何断言；node/CI 上本文件仍在 1.5s 量级完成。
vi.setConfig({testTimeout: 20_000, hookTimeout: 20_000})

const grants = vi.hoisted(() => ({current: [] as string[]}))

vi.mock('../../auth/AuthContext', () => ({
  useAuth: () => ({
    loading: false,
    error: undefined,
    authorization: {permissions: grants.current},
    login: vi.fn(),
    logout: vi.fn(),
    refreshAuthorization: vi.fn(),
  }),
}))

const knowledge = vi.hoisted(() => ({
  knowledgeBases: vi.fn(),
  knowledgeBase: vi.fn(),
  createKnowledgeBase: vi.fn(),
  replaceKnowledgeBase: vi.fn(),
  members: vi.fn(),
  replaceMembers: vi.fn(),
  documents: vi.fn(),
  uploadDocument: vi.fn(),
  documentRevision: vi.fn(),
  deleteDocument: vi.fn(),
  createReindexJob: vi.fn(),
  job: vi.fn(),
  jobs: vi.fn(),
  retryJob: vi.fn(),
  createAnswer: vi.fn(),
}))

const wiki = vi.hoisted(() => ({
  pages: vi.fn(),
  page: vi.fn(),
  graph: vi.fn(),
  replaceDraft: vi.fn(),
  publish: vi.fn(),
  unpublish: vi.fn(),
  createGenerationJob: vi.fn(),
}))

vi.mock('../../api/knowledge', async () => {
  const actual = await vi.importActual<Record<string, unknown>>('../../api/knowledge')
  return {...actual, knowledgeApi: knowledge}
})

vi.mock('../../api/wiki', async () => {
  const actual = await vi.importActual<Record<string, unknown>>('../../api/wiki')
  return {...actual, wikiApi: wiki}
})

const kbFixture = (overrides: Partial<KnowledgeBase> = {}): KnowledgeBase => ({
  id: KB_ID,
  name: '企业运维知识',
  description: '运维手册集合',
  ownerActorId: '9001',
  myRole: 'EDITOR',
  egressPolicy: 'LOCAL_ONLY',
  chatModel: 'chat-main',
  embeddingModel: 'embed-local',
  embeddingSpaceId: 'space-1',
  dimensions: 1024,
  revision: 2,
  ...overrides,
})

const documentFixture = (overrides: Partial<KnowledgeDocument> = {}): KnowledgeDocument => ({
  id: '74001',
  kbId: KB_ID,
  fileName: '服务手册.md',
  activeRevisionId: '75001',
  latestJobId: '78001',
  revision: 5,
  status: 'READY',
  createdAt: '2026-09-21T08:00:00Z',
  ...overrides,
})

const jobFixture = (overrides: Partial<KnowledgeJob> = {}): KnowledgeJob => ({
  id: '78001',
  kbId: KB_ID,
  type: 'DOCUMENT_INGEST',
  resourceId: '74001',
  status: 'SUCCEEDED',
  stage: 'DONE',
  revision: 3,
  attempt: 1,
  errorCode: null,
  createdAt: '2026-09-21T08:00:00Z',
  updatedAt: '2026-09-21T08:01:00Z',
  ...overrides,
})

const renderPage = (entry = `/knowledge/${KB_ID}`) => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {retry: false},
      mutations: {retry: false},
    },
  })
  const result = render(
    <QueryClientProvider client={queryClient}>
      <CapabilityProvider>
        <MemoryRouter initialEntries={[entry]}>
          <Routes>
            <Route path="/knowledge/:kbId" element={<KnowledgeBasePage />} />
          </Routes>
        </MemoryRouter>
      </CapabilityProvider>
    </QueryClientProvider>,
  )
  return {...result, queryClient}
}

const openWorkspace = async () => {
  await screen.findByText('我的角色：EDITOR')
}

const grantEmployee = (...roles: string[]) => {
  // 刻意不含 yuheng:read：知识工作区不得复用旧管理 guard 的能力。
  grants.current = ['yuheng:knowledge:read', ...roles]
}

const openTab = async (label: string) => {
  fireEvent.click(await screen.findByRole('tab', {name: label}))
}

beforeEach(() => {
  grants.current = []
  Object.values(knowledge).forEach((fn) => fn.mockReset())
  Object.values(wiki).forEach((fn) => fn.mockReset())
  knowledge.knowledgeBase.mockResolvedValue(kbFixture())
  knowledge.replaceKnowledgeBase.mockResolvedValue(kbFixture({revision: 3}))
  knowledge.members.mockResolvedValue({members: [{actorId: '9002', role: 'READER'}], revision: 4})
  knowledge.replaceMembers.mockResolvedValue({
    members: [{actorId: '9002', role: 'READER'}],
    revision: 5,
  })
  knowledge.documents.mockResolvedValue({
    items: [documentFixture()],
    page: 1,
    size: 20,
    total: 1,
  })
  knowledge.uploadDocument.mockResolvedValue({
    documentId: '74001',
    revisionId: '75002',
    jobId: '78002',
    status: 'QUEUED',
  })
  knowledge.deleteDocument.mockResolvedValue(undefined)
  knowledge.createReindexJob.mockResolvedValue(jobFixture({status: 'QUEUED'}))
  knowledge.jobs.mockResolvedValue({items: [jobFixture()], page: 1, size: 20, total: 1})
  knowledge.job.mockResolvedValue(jobFixture())
  knowledge.retryJob.mockResolvedValue(jobFixture({status: 'RUNNING'}))
  knowledge.createAnswer.mockResolvedValue({
    outcome: 'ANSWERED',
    answer: '按手册第 3 步执行。',
    citations: [{
      documentRevisionId: '75001',
      chunkId: '75100',
      sourceHash: HASH,
      documentId: '74001',
      fileName: '服务手册.md',
      pageId: null,
      excerpt: '第 3 步：重启网关。',
      score: 0.82,
    }],
    model: 'chat-main',
  })
  knowledge.documentRevision.mockResolvedValue({
    id: '75001',
    documentId: '74001',
    kbId: KB_ID,
    fileName: '服务手册.md',
    mediaType: 'text/markdown',
    byteCount: 12,
    text: '第 3 步：重启网关。',
    hash: HASH,
    status: 'READY',
    createdAt: '2026-09-21T08:00:00Z',
  })
  wiki.pages.mockResolvedValue({items: [], page: 1, size: 20, total: 0})
  wiki.createGenerationJob.mockResolvedValue(jobFixture({type: 'WIKI_GENERATE', status: 'QUEUED'}))
  vi.stubGlobal('matchMedia', vi.fn().mockImplementation(() => ({
    matches: false,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })))
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('KnowledgeBasePage workspace shell (TEST-010)', () => {
  it('opens the KB workspace for an employee who has no admin read capability', async () => {
    grantEmployee('yuheng:knowledge:write')
    renderPage()

    expect(await screen.findByText('企业运维知识')).toBeInTheDocument()
    expect(screen.getByText('我的角色：EDITOR')).toBeInTheDocument()
    expect(screen.getByText('出域策略：LOCAL_ONLY')).toBeInTheDocument()
    expect(screen.getByText('嵌入空间：space-1 · 1024 维')).toBeInTheDocument()
    expect(knowledge.knowledgeBase).toHaveBeenCalledWith(KB_ID, expect.anything())
  })

  it('offers exactly the five §12.1 tabs and defers their data', async () => {
    grantEmployee('yuheng:knowledge:write')
    renderPage()
    await openWorkspace()

    for (const label of ['资料列表', 'Wiki 主题', '知识问答', '处理任务', '知识库设置']) {
      expect(screen.getByRole('tab', {name: label})).toBeInTheDocument()
    }
    // 未打开的页签不预取：任务与文档原文都不该在资料页时被打扰。
    expect(knowledge.jobs).not.toHaveBeenCalled()
    expect(knowledge.documentRevision).not.toHaveBeenCalled()
  })

  it('fails closed on the KB read and evicts cached content when access is revoked', async () => {
    grantEmployee('yuheng:knowledge:write')
    knowledge.knowledgeBase.mockRejectedValue(new GatewayApiError(
      403,
      'YUHENG_ADMIN_CAPABILITY_REQUIRED',
      'membership revoked',
    ))
    const {queryClient} = renderPage()

    expect(await screen.findByText('membership revoked')).toBeInTheDocument()
    expect(screen.getByText('YUHENG_ADMIN_CAPABILITY_REQUIRED')).toBeInTheDocument()
    expect(screen.queryByText('服务手册.md')).toBeNull()

    queryClient.setQueryData(['knowledge', KB_ID, 'documents'], {items: [documentFixture()]})
    // antd 在两个汉字的按钮名里插入空格，实际可访问名是「重 试」。
    fireEvent.click(screen.getByRole('button', {name: /重\s*试/}))
    await waitFor(() => {
      expect(queryClient.getQueryData(['knowledge', KB_ID, 'documents'])).toBeUndefined()
    })
  })

  it('renders the empty document list without pretending there is content', async () => {
    grantEmployee('yuheng:knowledge:write')
    knowledge.documents.mockResolvedValue({items: [], page: 1, size: 20, total: 0})
    renderPage()
    await openWorkspace()

    expect(screen.getByText('暂无资料')).toBeInTheDocument()
  })
})

describe('KnowledgeBasePage documents and upload (TEST-010)', () => {
  it('lists documents with their active revision, latest job and status', async () => {
    grantEmployee('yuheng:knowledge:write')
    renderPage()
    await openWorkspace()

    expect(screen.getByText('服务手册.md')).toBeInTheDocument()
    expect(screen.getByText('75001')).toBeInTheDocument()
    expect(screen.getByText('78001')).toBeInTheDocument()
    expect(screen.getByText('READY')).toBeInTheDocument()
  })

  it('uploads the selected file as multipart FormData without self-reported identity', async () => {
    grantEmployee('yuheng:knowledge:write')
    renderPage()
    await openWorkspace()
    const file = new File(['# 手册'], '服务手册.md', {type: 'text/markdown'})

    fireEvent.click(screen.getByRole('button', {name: '上传资料'}))
    fireEvent.change(await screen.findByLabelText('资料文件'), {target: {files: [file]}})
    fireEvent.click(screen.getByRole('button', {name: '提交上传'}))

    await waitFor(() => expect(knowledge.uploadDocument).toHaveBeenCalledTimes(1))
    const [kbId, form] = knowledge.uploadDocument.mock.calls[0] as [string, FormData]
    expect(kbId).toBe(KB_ID)
    expect(form).toBeInstanceOf(FormData)
    expect(form.get('file')).toBe(file)
    // 202 只代表受理：回执里的任务被单独失效，命令体不带 actorId/tenantId。
    expect(form.has('actorId')).toBe(false)
    expect(form.has('tenantId')).toBe(false)
    expect(knowledge.uploadDocument.mock.calls[0][1]).not.toHaveProperty('actorId')
  })

  it('keeps the reader in a disabled upload path and never sends a command', async () => {
    grants.current = ['yuheng:knowledge:read']
    knowledge.knowledgeBase.mockResolvedValue(kbFixture({myRole: 'READER'}))
    renderPage()
    await waitFor(() => expect(screen.queryByText('我的角色：EDITOR')).toBeNull())
    await screen.findByText('企业运维知识')

    const upload = screen.getByRole('button', {name: '上传资料'})
    expect(upload).toBeDisabled()
    expect(upload).toHaveAttribute('title', '当前角色不能上传资料')
    fireEvent.click(upload)
    expect(knowledge.uploadDocument).not.toHaveBeenCalled()
  })

  it('reports a 409 document delete conflict and keeps the row visible', async () => {
    grantEmployee('yuheng:knowledge:write')
    knowledge.deleteDocument.mockRejectedValue(new GatewayApiError(
      409,
      'YUHENG_ADMIN_REVISION_CONFLICT',
      'document revision conflict',
      undefined,
      6,
    ))
    renderPage()
    await openWorkspace()

    fireEvent.click(screen.getByRole('button', {name: '删除资料'}))
    // Popconfirm 渲染在 portal 里，且 mutation 后的重渲染在本机运行器上明显偏慢。
    fireEvent.click(await screen.findByRole('button', {name: '确认删除'}, {timeout: 8_000}))

    expect(await screen.findByText('document revision conflict', {}, {timeout: 8_000}))
      .toBeInTheDocument()
    expect(screen.getByText('版本已变化，请刷新后确认。')).toBeInTheDocument()
    expect(knowledge.deleteDocument).toHaveBeenCalledWith(KB_ID, '74001', 5)
    expect(screen.getByText('服务手册.md')).toBeInTheDocument()
  })

  it('reindexes from the active revision with the document revision as the CAS guard', async () => {
    grantEmployee('yuheng:knowledge:write')
    renderPage()
    await openWorkspace()

    fireEvent.click(screen.getByRole('button', {name: '重建索引'}))

    await waitFor(() => expect(knowledge.createReindexJob).toHaveBeenCalledWith(KB_ID, '74001', {
      sourceRevisionId: '75001',
      expectedRevision: 5,
    }), {timeout: 8_000})
  })
})

describe('KnowledgeBasePage membership-scoped settings (TEST-010)', () => {
  it('keeps settings read-only for a non-owner even with the admin capability', async () => {
    grantEmployee('yuheng:knowledge:write', 'yuheng:knowledge:admin')
    renderPage()
    await openWorkspace()
    await openTab('知识库设置')

    expect(await screen.findByText('只有知识库所有者可修改设置与成员')).toBeInTheDocument()
    expect(screen.getByText(/当前角色：EDITOR/)).toBeInTheDocument()
    expect(knowledge.members).not.toHaveBeenCalled()
    expect(screen.queryByText('成员授权')).toBeNull()
  })

  it('lets the owner replace settings and members with exact typed payloads', async () => {
    grantEmployee('yuheng:knowledge:write', 'yuheng:knowledge:admin')
    knowledge.knowledgeBase.mockResolvedValue(kbFixture({myRole: 'OWNER'}))
    renderPage()
    await waitFor(() => expect(screen.getByText('我的角色：OWNER')).toBeInTheDocument())
    await openTab('知识库设置')

    const name = await screen.findByLabelText('名称')
    await waitFor(() => expect(name).toHaveValue('企业运维知识'))
    fireEvent.change(name, {target: {value: '企业运维知识集'}})
    fireEvent.click(screen.getByRole('button', {name: '保存设置'}))

    await waitFor(() => expect(knowledge.replaceKnowledgeBase).toHaveBeenCalledWith(KB_ID, {
      name: '企业运维知识集',
      description: '运维手册集合',
      egressPolicy: 'LOCAL_ONLY',
      chatModel: 'chat-main',
      embeddingModel: 'embed-local',
      expectedRevision: 2,
    }))
    expect(knowledge.replaceKnowledgeBase.mock.calls[0][1]).not.toHaveProperty('actorId')
    expect(knowledge.replaceKnowledgeBase.mock.calls[0][1]).not.toHaveProperty('tenantId')

    await waitFor(() => expect(knowledge.members).toHaveBeenCalledWith(KB_ID, expect.anything()))
    fireEvent.click(screen.getByRole('button', {name: '保存成员'}))
    await waitFor(() => expect(knowledge.replaceMembers).toHaveBeenCalledWith(KB_ID, {
      members: [{actorId: '9002', role: 'READER'}],
      expectedRevision: 4,
    }))
  })

  it('preserves the unsaved settings input on a 409 without auto-retrying', async () => {
    grantEmployee('yuheng:knowledge:write', 'yuheng:knowledge:admin')
    // 设置表单只对 OWNER 开放（见上面的只读用例），不设为 OWNER 就根本发不出命令。
    knowledge.knowledgeBase.mockResolvedValue(kbFixture({myRole: 'OWNER'}))
    knowledge.replaceKnowledgeBase.mockRejectedValue(new GatewayApiError(
      409,
      'YUHENG_ADMIN_REVISION_CONFLICT',
      'knowledge base revision conflict',
      undefined,
      9,
    ))
    renderPage()
    await waitFor(() => expect(screen.getByText('我的角色：OWNER')).toBeInTheDocument(), {
      timeout: 8_000,
    })
    await openTab('知识库设置')

    const name = await screen.findByLabelText('名称')
    await waitFor(() => expect(name).toHaveValue('企业运维知识'))
    fireEvent.change(name, {target: {value: '我的新名称'}})
    fireEvent.click(screen.getByRole('button', {name: '保存设置'}))

    expect(await screen.findByText('knowledge base revision conflict', {}, {timeout: 8_000}))
      .toBeInTheDocument()
    expect(screen.getByText('版本已被他人推进，请刷新后再提交。')).toBeInTheDocument()
    expect(name).toHaveValue('我的新名称')
    expect(knowledge.replaceKnowledgeBase).toHaveBeenCalledTimes(1)
  })
})

describe('KnowledgeBasePage answer panel (TEST-010)', () => {
  it('asks over DOCUMENTS/WIKI/BOTH and renders traceable citations', async () => {
    grantEmployee('yuheng:knowledge:write')
    renderPage()
    await openWorkspace()
    await openTab('知识问答')

    fireEvent.change(await screen.findByLabelText('问题'), {target: {value: '如何重启网关？'}})
    fireEvent.click(screen.getByRole('button', {name: '提交问题'}))

    await waitFor(() => expect(knowledge.createAnswer).toHaveBeenCalledWith(KB_ID, {
      question: '如何重启网关？',
      sourceMode: 'BOTH',
      topK: 8,
      searchMode: 'VECTOR',
    }))
    expect(await screen.findByText('按手册第 3 步执行。')).toBeInTheDocument()
    expect(screen.getByText('版本 75001')).toBeInTheDocument()
    expect(screen.getByText('分块 75100')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', {name: '查看原文版本'}))
    expect(await screen.findByText('第 3 步：重启网关。')).toBeInTheDocument()
    expect(knowledge.documentRevision).toHaveBeenCalledWith(KB_ID, '74001', '75001', expect.anything())
  })

  it('treats NO_EVIDENCE as an honest empty answer, not a failure', async () => {
    grantEmployee('yuheng:knowledge:write')
    knowledge.createAnswer.mockResolvedValue({
      outcome: 'NO_EVIDENCE',
      answer: '',
      citations: [],
      model: 'chat-main',
    })
    renderPage()
    await openWorkspace()
    await openTab('知识问答')

    fireEvent.change(await screen.findByLabelText('问题'), {target: {value: '无关问题'}})
    fireEvent.click(screen.getByRole('button', {name: '提交问题'}))

    expect(await screen.findByText('空结果不是故障：本轮不生成任何引用，也不编造回答。')).toBeInTheDocument()
    expect(screen.getByText('引用（0）')).toBeInTheDocument()
    expect(screen.getByText('无引用')).toBeInTheDocument()
  })

  it('warns when the frozen citation hash no longer matches the revision', async () => {
    grantEmployee('yuheng:knowledge:write')
    knowledge.documentRevision.mockResolvedValue({
      id: '75001',
      documentId: '74001',
      kbId: KB_ID,
      fileName: '服务手册.md',
      mediaType: 'text/markdown',
      byteCount: 12,
      text: '已改写的原文',
      hash: 'c'.repeat(64),
      status: 'READY',
      createdAt: '2026-09-21T08:00:00Z',
    })
    renderPage()
    await openWorkspace()
    await openTab('知识问答')

    fireEvent.change(await screen.findByLabelText('问题'), {target: {value: '如何重启网关？'}})
    fireEvent.click(screen.getByRole('button', {name: '提交问题'}))
    await screen.findByText('按手册第 3 步执行。')
    fireEvent.click(screen.getByRole('button', {name: '查看原文版本'}))

    expect(await screen.findByText('来源已变化')).toBeInTheDocument()
  })
})

describe('KnowledgeBasePage job polling (TEST-010)', () => {
  it('stops polling once every visible job is terminal', async () => {
    grantEmployee('yuheng:knowledge:write')
    knowledge.jobs.mockResolvedValue({
      items: [jobFixture({status: 'SUCCEEDED', stage: 'DONE'})],
      page: 1,
      size: 20,
      total: 1,
    })
    renderPage()
    await openWorkspace()
    await openTab('处理任务')

    await screen.findByText('服务手册.md')
    const initialCalls = knowledge.jobs.mock.calls.length
    expect(screen.getByText('轮询开启')).toBeInTheDocument()

    await new Promise((resolve) => setTimeout(resolve, 2_400))
    expect(knowledge.jobs.mock.calls.length).toBe(initialCalls)
  })

  it('keeps polling while an active job is visible and shows its stage', async () => {
    grantEmployee('yuheng:knowledge:write')
    knowledge.jobs.mockResolvedValue({
      items: [jobFixture({status: 'RUNNING', stage: 'EMBED'})],
      page: 1,
      size: 20,
      total: 1,
    })
    renderPage()
    await openWorkspace()
    await openTab('处理任务')

    await screen.findByText('EMBED')
    const initialCalls = knowledge.jobs.mock.calls.length
    await waitFor(() => expect(knowledge.jobs.mock.calls.length).toBeGreaterThan(initialCalls), {
      timeout: 4_000,
    })
  })

  it('restricts manual retry to failed jobs and to writers', async () => {
    grantEmployee('yuheng:knowledge:write')
    knowledge.jobs.mockResolvedValue({
      items: [
        jobFixture({id: '78001', status: 'FAILED', stage: 'EMBED', errorCode: 'YUHENG_KB_EMBED_FAILED'}),
      ],
      page: 1,
      size: 20,
      total: 1,
    })
    renderPage()
    await openWorkspace()
    await openTab('处理任务')

    const row = (await screen.findByText('YUHENG_KB_EMBED_FAILED')).closest('tr') as HTMLElement
    fireEvent.click(within(row).getByRole('button', {name: '重试任务'}))

    await waitFor(() => expect(knowledge.retryJob).toHaveBeenCalledWith('78001', 3))
  })
})

describe('KnowledgeBasePage wiki generation (TEST-010)', () => {
  it('submits only active revision ids of this KB for generation', async () => {
    grantEmployee('yuheng:knowledge:write')
    renderPage()
    await openWorkspace()
    await openTab('Wiki 主题')

    fireEvent.click(await screen.findByRole('button', {name: '生成页面'}))
    const source = await screen.findByLabelText('来源版本', {}, {timeout: 8_000})
    // 走鼠标路径：tags 模式的键盘提交依赖下拉高亮与动画完成，这里只验证提交的载荷形状。
    fireEvent.mouseDown(source)
    fireEvent.click(await screen.findByText('服务手册.md · 75001', {}, {timeout: 8_000}))
    fireEvent.click(screen.getByRole('button', {name: '提交生成'}))

    await waitFor(() => expect(wiki.createGenerationJob).toHaveBeenCalledWith(KB_ID, {
      sourceRevisionIds: ['75001'],
      pageId: null,
      expectedRevision: 0,
    }), {timeout: 8_000})
  })

  it('renders the wiki topic table with the snapshot columns the server owns', async () => {
    grantEmployee('yuheng:knowledge:write')
    wiki.pages.mockResolvedValue({
      items: [{
        id: '76001',
        kbId: KB_ID,
        slug: 'guide',
        title: '服务使用指南',
        publicationStatus: 'PUBLISHED',
        reviewStatus: 'NOT_REQUIRED',
        publicationPolicySnapshot: 'DIRECT',
        stale: true,
      }],
      page: 1,
      size: 20,
      total: 1,
    })
    renderPage()
    await openWorkspace()
    await openTab('Wiki 主题')

    expect(await screen.findByText('服务使用指南')).toBeInTheDocument()
    expect(screen.getByText('PUBLISHED')).toBeInTheDocument()
    expect(screen.getByText('NOT_REQUIRED')).toBeInTheDocument()
    expect(screen.getByText('DIRECT')).toBeInTheDocument()
    expect(screen.getByText('来源已变化')).toBeInTheDocument()
  })
})
