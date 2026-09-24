import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {cleanup, fireEvent, render, screen, waitFor, within} from '@testing-library/react'
import {MemoryRouter, Route, Routes} from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {GatewayApiError} from '../../api/client'
import type {KnowledgeBase} from '../../api/knowledge'
import type {WikiGraph, WikiPageView} from '../../api/wiki'
import {CapabilityProvider} from '../../app/capabilities'
import {WikiPage} from './WikiPage'

// 原业务Spec §12.1「Wiki 页面」+ §12.2 + TEST-010 的行为合同：
// DRAFT/PUBLISHED、只读、来源失效、409 保留草稿、DIRECT/NOT_REQUIRED，且没有任何人工审核动作。
// 能力判定走真实 CapabilityProvider（只 mock useAuth），因此本文件同时证明
// 员工知识分支不依赖旧管理根的 yuheng:read（REQ-008）。

const KB_ID = '71001'
const PAGE_ID = '76001'
const HASH = 'a'.repeat(64)

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

const wiki = vi.hoisted(() => ({
  page: vi.fn(),
  pages: vi.fn(),
  graph: vi.fn(),
  replaceDraft: vi.fn(),
  publish: vi.fn(),
  unpublish: vi.fn(),
  createGenerationJob: vi.fn(),
}))

const knowledge = vi.hoisted(() => ({
  knowledgeBase: vi.fn(),
  knowledgeBases: vi.fn(),
  documents: vi.fn(),
  members: vi.fn(),
  replaceMembers: vi.fn(),
  replaceKnowledgeBase: vi.fn(),
  uploadDocument: vi.fn(),
  deleteDocument: vi.fn(),
  createReindexJob: vi.fn(),
  documentRevision: vi.fn(),
  jobs: vi.fn(),
  job: vi.fn(),
  retryJob: vi.fn(),
  createAnswer: vi.fn(),
  createKnowledgeBase: vi.fn(),
}))

vi.mock('../../api/wiki', async () => {
  const actual = await vi.importActual<Record<string, unknown>>('../../api/wiki')
  return {...actual, wikiApi: wiki}
})

vi.mock('../../api/knowledge', async () => {
  const actual = await vi.importActual<Record<string, unknown>>('../../api/knowledge')
  return {...actual, knowledgeApi: knowledge}
})

const base = (overrides: Partial<WikiPageView> = {}): WikiPageView => ({
  id: PAGE_ID,
  kbId: KB_ID,
  slug: 'service-guide',
  title: '服务使用指南',
  draftRevisionId: '77001',
  publishedRevisionId: '77002',
  publicationStatus: 'PUBLISHED',
  reviewStatus: 'NOT_REQUIRED',
  publicationPolicySnapshot: 'DIRECT',
  publicationVersion: 3,
  publicationErrorCode: null,
  contentRevisionId: '77002',
  markdown: '# 服务使用指南\n本地正文按纯文本显示。',
  tags: ['运维'],
  links: ['76002'],
  sources: [{documentRevisionId: '75001', chunkId: '75100', sourceHash: HASH}],
  stale: false,
  revision: 7,
  ...overrides,
})

const graphFixture = (overrides: Partial<WikiGraph> = {}): WikiGraph => ({
  nodes: [
    {id: PAGE_ID, title: '服务使用指南'},
    {id: '76002', title: '告警手册'},
  ],
  edges: [{source: PAGE_ID, target: '76002'}],
  truncated: false,
  ...overrides,
})

const kbFixture = (myRole: KnowledgeBase['myRole'] = 'EDITOR'): KnowledgeBase => ({
  id: KB_ID,
  name: '企业运维知识',
  description: '运维手册',
  ownerActorId: '9001',
  myRole,
  egressPolicy: 'LOCAL_ONLY',
  chatModel: 'chat-main',
  embeddingModel: 'embed-local',
  embeddingSpaceId: 'space-1',
  dimensions: 1024,
  revision: 2,
})

const renderPage = (entry = `/knowledge/${KB_ID}/wiki/${PAGE_ID}`) => {
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
            <Route path="/knowledge/:kbId/wiki/:pageId" element={<WikiPage />} />
          </Routes>
        </MemoryRouter>
      </CapabilityProvider>
    </QueryClientProvider>,
  )
  return {...result, queryClient}
}

// 页面标题同时出现在主题列表与图谱节点里，因此用页脚版本行作为唯一就绪锚点。
const openWorkspace = async () => {
  await screen.findByText(/^页面版本 /)
}

const grantEmployeeKnowledgeAccess = () => {
  // 刻意不含 yuheng:read：员工知识工作区必须独立于旧管理 guard 放行。
  grants.current = ['yuheng:knowledge:read', 'yuheng:knowledge:write']
}

beforeEach(() => {
  grants.current = []
  Object.values(wiki).forEach((fn) => fn.mockReset())
  Object.values(knowledge).forEach((fn) => fn.mockReset())
  wiki.page.mockResolvedValue(base())
  wiki.pages.mockResolvedValue({items: [base()], page: 1, size: 20, total: 1})
  wiki.graph.mockResolvedValue(graphFixture())
  wiki.replaceDraft.mockResolvedValue(base())
  wiki.publish.mockResolvedValue(base({publicationStatus: 'PUBLISHED'}))
  wiki.unpublish.mockResolvedValue(base({publishedRevisionId: null, publicationStatus: 'ARCHIVED'}))
  knowledge.knowledgeBase.mockResolvedValue(kbFixture('EDITOR'))
  knowledge.documents.mockResolvedValue({items: [], page: 1, size: 20, total: 0})
  knowledge.jobs.mockResolvedValue({items: [], page: 1, size: 20, total: 0})
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

describe('WikiPage publication state machine (TEST-010)', () => {
  it('renders a PUBLISHED page for an employee without the admin read capability', async () => {
    grantEmployeeKnowledgeAccess()
    renderPage()

    const heading = await screen.findByText(/^页面版本 /)
    expect(heading).toBeInTheDocument()
    expect(screen.getAllByText('服务使用指南').length).toBeGreaterThan(0)
    expect(screen.getByText('PUBLISHED')).toBeInTheDocument()
    expect(screen.getByText('审核：NOT_REQUIRED')).toBeInTheDocument()
    expect(screen.getByText('策略：DIRECT')).toBeInTheDocument()
    // 不伪造：正文版本描述 contentRevisionId 所指版本。
    expect(screen.getByText(/正文版本 77002/)).toBeInTheDocument()
    expect(wiki.page).toHaveBeenCalledWith(KB_ID, PAGE_ID, undefined, expect.anything())
  })

  it('exposes no reviewer action UI at all under the DIRECT policy', async () => {
    grantEmployeeKnowledgeAccess()
    renderPage()
    await openWorkspace()

    for (const label of [/通过/, /驳回/, /拒绝/, /提交评审/, /提交审核/, /审批/]) {
      expect(screen.queryByRole('button', {name: label})).toBeNull()
    }
    expect(screen.getByRole('button', {name: '直接发布'})).toBeInTheDocument()
    expect(screen.getByRole('button', {name: '下线内容'})).toBeInTheDocument()
  })

  it('shows DRAFT state and keeps publication unavailable without a draft revision', async () => {
    grantEmployeeKnowledgeAccess()
    wiki.page.mockResolvedValue(base({
      publicationStatus: 'DRAFT',
      publishedRevisionId: null,
      contentRevisionId: null,
      draftRevisionId: null,
      publicationVersion: null,
    }))
    renderPage()

    await screen.findByText('DRAFT')
    expect(screen.getByRole('button', {name: '直接发布'})).toBeDisabled()
    expect(screen.getByRole('button', {name: '下线内容'})).toBeDisabled()
    expect(screen.getByText(/正文版本 —/)).toBeInTheDocument()
  })

  it('publishes the selected draft revision with the page revision as the CAS guard', async () => {
    grantEmployeeKnowledgeAccess()
    renderPage()
    await openWorkspace()

    fireEvent.click(screen.getByRole('button', {name: '直接发布'}))
    fireEvent.click(await screen.findByRole('button', {name: '确认发布'}))

    await waitFor(() => expect(wiki.publish).toHaveBeenCalledWith(KB_ID, PAGE_ID, {
      draftRevisionId: '77001',
      expectedRevision: 7,
    }))
  })

  it('surfaces the publication failure code without overwriting the live content', async () => {
    grantEmployeeKnowledgeAccess()
    wiki.page.mockResolvedValue(base({
      publicationStatus: 'PUBLISHED',
      publicationErrorCode: 'YUHENG_WIKI_PUBLICATION_FAILED',
    }))
    renderPage()

    expect(await screen.findByText('策略：DIRECT')).toBeInTheDocument()
    expect(screen.getByText('失败：YUHENG_WIKI_PUBLICATION_FAILED')).toBeInTheDocument()
  })
})

describe('WikiPage editing, conflict and cache behaviour (TEST-010)', () => {
  it('reads a historical revision as read-only and refuses editing', async () => {
    grantEmployeeKnowledgeAccess()
    renderPage(`/knowledge/${KB_ID}/wiki/${PAGE_ID}?revision=76999`)

    expect(await screen.findByText(/正在查看历史版本 76999/)).toBeInTheDocument()
    expect(screen.getByRole('button', {name: '编辑草稿'})).toBeDisabled()
    expect(wiki.page).toHaveBeenCalledWith(KB_ID, PAGE_ID, '76999', expect.anything())
  })

  it('renders a read-only notice for a READER member and never submits a draft', async () => {
    grants.current = ['yuheng:knowledge:read']
    knowledge.knowledgeBase.mockResolvedValue(kbFixture('READER'))
    renderPage()

    expect(await screen.findByText('只读模式')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', {name: '编辑草稿'}))

    expect(screen.queryByLabelText('Markdown 正文')).toBeNull()
    expect(wiki.replaceDraft).not.toHaveBeenCalled()
    expect(wiki.publish).not.toHaveBeenCalled()
  })

  it('keeps the locally edited markdown and reports the conflict on 409', async () => {
    grantEmployeeKnowledgeAccess()
    wiki.replaceDraft.mockRejectedValue(new GatewayApiError(
      409,
      'YUHENG_ADMIN_REVISION_CONFLICT',
      'wiki page revision conflict',
      undefined,
      9,
    ))
    renderPage()
    await openWorkspace()

    fireEvent.click(screen.getByRole('button', {name: '编辑草稿'}))
    const editor = await screen.findByLabelText('Markdown 正文')
    fireEvent.change(editor, {target: {value: '我的人工修订内容'}})
    fireEvent.click(screen.getByRole('button', {name: '保存草稿'}))

    expect(await screen.findByText('wiki page revision conflict')).toBeInTheDocument()
    expect(screen.getByText(/你的草稿已保留在本地/)).toBeInTheDocument()
    expect(screen.getByRole('button', {name: '刷新比较'})).toBeInTheDocument()
    // 不自动覆盖最新版本，也不静默重试。
    expect(editor).toHaveValue('我的人工修订内容')
    expect(wiki.replaceDraft).toHaveBeenCalledTimes(1)
    expect(wiki.replaceDraft).toHaveBeenCalledWith(KB_ID, PAGE_ID, expect.objectContaining({
      markdown: '我的人工修订内容',
      expectedRevision: 7,
    }))
  })

  it('drops cached source content and the local draft when the server answers 403', async () => {
    grantEmployeeKnowledgeAccess()
    wiki.replaceDraft.mockRejectedValue(new GatewayApiError(
      403,
      'YUHENG_ADMIN_CAPABILITY_REQUIRED',
      'membership revoked',
    ))
    const {queryClient} = renderPage()
    await openWorkspace()

    // 失权前已缓存的派生内容：引用原文正文、资料清单、其它页面投影——都必须立即清除。
    queryClient.setQueryData(['knowledge', KB_ID, 'document-revision', '75001', '75002'], {text: '机密原文'})
    queryClient.setQueryData(['knowledge', KB_ID, 'documents'], {items: [{fileName: '机密.pdf'}]})
    queryClient.setQueryData(['wiki', KB_ID, '76009'], base({id: '76009'}))

    fireEvent.click(screen.getByRole('button', {name: '编辑草稿'}))
    fireEvent.change(await screen.findByLabelText('Markdown 正文'), {target: {value: '将被清除的正文'}})
    fireEvent.click(screen.getByRole('button', {name: '保存草稿'}))

    expect(await screen.findByText('membership revoked')).toBeInTheDocument()
    await waitFor(() => {
      expect(queryClient.getQueryData(['knowledge', KB_ID, 'document-revision', '75001', '75002'])).toBeUndefined()
      expect(queryClient.getQueryData(['knowledge', KB_ID, 'documents'])).toBeUndefined()
      expect(queryClient.getQueryData(['wiki', KB_ID, '76009'])).toBeUndefined()
    })
    // 本地草稿状态一并丢弃，失权后不再留下可提交的编辑面。
    expect(screen.queryByLabelText('Markdown 正文')).toBeNull()
    expect(screen.queryByRole('button', {name: '保存草稿'})).toBeNull()
  })

  it('marks stale sources without hiding the current publication', async () => {
    grantEmployeeKnowledgeAccess()
    wiki.page.mockResolvedValue(base({stale: true}))
    renderPage()

    expect(await screen.findByText('来源已变化')).toBeInTheDocument()
    expect(screen.getByText('PUBLISHED')).toBeInTheDocument()
  })
})

describe('WikiPage sources and graph (TEST-010)', () => {
  it('renders graph edges from the server source/target fields with resolved titles', async () => {
    grantEmployeeKnowledgeAccess()
    renderPage()

    const card = await screen.findByText('关联与来源')
    expect(card).toBeInTheDocument()
    expect(await screen.findByText('服务使用指南 → 告警手册')).toBeInTheDocument()
    // 旧字段名会渲染成 undefined → undefined；这里断言不出现未定义文本。
    expect(screen.queryByText(/undefined/)).toBeNull()
    expect(wiki.graph).toHaveBeenCalledWith(KB_ID, PAGE_ID, 100, expect.anything())
  })

  it('warns when the authorized graph was truncated', async () => {
    grantEmployeeKnowledgeAccess()
    wiki.graph.mockResolvedValue(graphFixture({truncated: true}))
    renderPage()

    expect(await screen.findByText('已达到图输出上限')).toBeInTheDocument()
  })

  it('shows the frozen source revision and hash prefix of the displayed content', async () => {
    grantEmployeeKnowledgeAccess()
    renderPage()

    await openWorkspace()
    const sources = screen.getByText('来源').closest('.ant-card') as HTMLElement
    expect(within(sources).getByText('资料版本 75001')).toBeInTheDocument()
    expect(within(sources).getByText(/分块 75100/)).toBeInTheDocument()
    expect(within(sources).getByText(new RegExp(`hash ${HASH.slice(0, 12)}`))).toBeInTheDocument()
  })

  it('renders the page body as plain text instead of injecting markup', async () => {
    grantEmployeeKnowledgeAccess()
    wiki.page.mockResolvedValue(base({markdown: '<img src=x onerror=alert(1)>', title: 'xss'}))
    const {container} = renderPage()

    await screen.findByText('<img src=x onerror=alert(1)>')
    expect(container.querySelector('img[src="x"]')).toBeNull()
  })

  it('fails the page read through the shared error surface without a fake empty page', async () => {
    grantEmployeeKnowledgeAccess()
    wiki.page.mockRejectedValue(new GatewayApiError(
      404,
      'YUHENG_ADMIN_NOT_FOUND',
      'wiki page not found',
    ))
    renderPage()

    expect(await screen.findByText('wiki page not found')).toBeInTheDocument()
    expect(screen.getByText('YUHENG_ADMIN_NOT_FOUND')).toBeInTheDocument()
    expect(screen.queryByText(/^页面版本 /)).toBeNull()
  })
})
