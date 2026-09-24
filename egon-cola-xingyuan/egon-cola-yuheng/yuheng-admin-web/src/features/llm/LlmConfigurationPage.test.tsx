import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {cleanup, fireEvent, render, screen, waitFor, within} from '@testing-library/react'
import {MemoryRouter, Route, Routes} from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {GatewayApiError} from '../../api/client'
import type {LlmChannel, LlmModel} from '../../api/llm'
import {CapabilityProvider} from '../../app/capabilities'
import {LlmConfigurationPage} from './LlmConfigurationPage'

// 原业务Spec §9.2.4–§9.2.7 + §12.1「模型管理」+ TEST-004 的行为合同：
// 渠道/模型表单、secretRef 只是引用名、EMBEDDING 路由含 CLOUD 时前端直接拒绝（不回退云端）。
// 能力判定走真实 CapabilityProvider（只 mock useAuth），因此本文件同时证明
// 模型分支不依赖旧管理根的 yuheng:read（REQ-008）。

// 本机没有 node 可执行文件，vitest 只能跑在 bun 上，而 @testing-library/dom 的查询与轮询
// 在这个组合下明显偏慢（antd 表格越大 DOM 越慢，单次查询可到 10s 量级），默认 5s 会误判成超时。
// 只放宽用例预算并拆开最重的一条，不放宽任何断言；node/CI 上本文件仍在秒级完成。
vi.setConfig({testTimeout: 40_000, hookTimeout: 40_000})

const SECRET_SENTINEL = 'sk-live-never-rendered'

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

const llm = vi.hoisted(() => ({
  channels: vi.fn(),
  replaceChannel: vi.fn(),
  models: vi.fn(),
  replaceModel: vi.fn(),
}))

vi.mock('../../api/llm', async () => {
  const actual = await vi.importActual<Record<string, unknown>>('../../api/llm')
  return {...actual, llmApi: llm}
})

const localChannel = (overrides: Partial<LlmChannel> = {}): LlmChannel => ({
  key: 'local-main',
  name: '本地主渠道',
  deployment: 'LOCAL',
  protocol: 'OPENAI_CHAT',
  baseUrl: 'http://127.0.0.1:11434/v1',
  secretRef: null,
  enabled: true,
  connectTimeoutMs: 3_000,
  headerTimeoutMs: 30_000,
  idleTimeoutMs: 30_000,
  totalTimeoutMs: 120_000,
  maxConcurrent: 16,
  revision: 3,
  ...overrides,
})

// 额外字段模拟服务端多返回一个密钥字段：页面只渲染合同列，因此引用名可见、密钥值不可见。
const cloudChannel = {
  key: 'cloud-eu',
  name: '欧盟云渠道',
  deployment: 'CLOUD',
  protocol: 'ANTHROPIC_MESSAGES',
  baseUrl: 'https://api.example.eu/v1',
  secretRef: 'llm/cloud-eu-key',
  enabled: true,
  connectTimeoutMs: 3_000,
  headerTimeoutMs: 30_000,
  idleTimeoutMs: 30_000,
  totalTimeoutMs: 120_000,
  maxConcurrent: 8,
  revision: 5,
  apiKey: SECRET_SENTINEL,
} as LlmChannel

const chatModel = (overrides: Partial<LlmModel> = {}): LlmModel => ({
  key: 'chat-main',
  name: '对话主模型',
  kind: 'CHAT',
  protocols: ['OPENAI_CHAT'],
  enabled: true,
  dimensions: null,
  embeddingSpaceId: null,
  allowedSubjects: ['svc-answer'],
  routes: [{
    channelKey: 'local-main',
    upstreamModel: 'qwen2.5-instruct',
    capabilities: ['TEXT'],
    priority: 100,
    weight: 1,
  }],
  revision: 4,
  ...overrides,
})

const embeddingModel = (overrides: Partial<LlmModel> = {}): LlmModel => ({
  key: 'embed-local',
  name: '本地嵌入模型',
  kind: 'EMBEDDING',
  protocols: ['OPENAI_EMBEDDING'],
  enabled: true,
  dimensions: 1024,
  embeddingSpaceId: 'space-default',
  allowedSubjects: ['svc-ingest'],
  routes: [{
    channelKey: 'local-main',
    upstreamModel: 'bge-m3',
    capabilities: ['TEXT'],
    priority: 100,
    weight: 1,
  }],
  revision: 2,
  ...overrides,
})

const renderPage = (entry = '/llm/configuration') => {
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
            <Route path="/llm/configuration" element={<LlmConfigurationPage />} />
          </Routes>
        </MemoryRouter>
      </CapabilityProvider>
    </QueryClientProvider>,
  )
  return {...result, queryClient}
}

// 行定位不依赖渲染顺序，避免表格列调整后用例互相误伤。
const rowElement = (key: string) => {
  const row = screen.getByText(key).closest('tr')
  if (!row) throw new Error(`行 ${key} 未渲染`)
  return row as HTMLElement
}
const channelRow = (key: string) => within(rowElement(key))
const modelRow = (key: string) => within(rowElement(key))

const commandOf = (call: number) => llm.replaceChannel.mock.calls[call][1]
const modelCommandOf = (call: number) => llm.replaceModel.mock.calls[call][1]

// antd 6 的整表 DOM 让 getByRole({name}) 的可访问名计算在本机运行器上极慢（单次约 10s）。
// 这里按文本定位按钮再取最近的可禁用宿主，断言对象与 getByRole 完全相同。
const buttonIn = (scope: HTMLElement, label: string) => {
  const button = within(scope).getByText(label).closest('button')
  if (!button) throw new Error(`${label} 按钮未渲染`)
  return button
}

beforeEach(() => {
  grants.current = []
  Object.values(llm).forEach((fn) => fn.mockReset())
  llm.channels.mockResolvedValue({items: [localChannel(), cloudChannel], page: 1, size: 20, total: 2})
  llm.models.mockResolvedValue({
    items: [chatModel(), embeddingModel(), embeddingModel({
      key: 'embed-cloud',
      name: '越界嵌入模型',
      embeddingSpaceId: 'space-cloud',
      routes: [{
        channelKey: 'cloud-eu',
        upstreamModel: 'text-embedding-3',
        capabilities: ['TEXT'],
        priority: 100,
        weight: 1,
      }],
      revision: 6,
    })],
    page: 1,
    size: 20,
    total: 3,
  })
  llm.replaceChannel.mockImplementation(async (key: string) => localChannel({key}))
  llm.replaceModel.mockImplementation(async (key: string) => (
    key === 'embed-cloud'
      ? embeddingModel({key, routes: [{
        channelKey: 'cloud-eu',
        upstreamModel: 'text-embedding-3',
        capabilities: ['TEXT'],
        priority: 100,
        weight: 1,
      }], revision: 7})
      : embeddingModel({key})
  ))
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

describe('LlmConfigurationPage dictionaries (TEST-004)', () => {
  it('renders channels for an llm-only grant and never surfaces secret values', async () => {
    // 刻意不含 yuheng:read：模型网关工作区必须独立于旧管理 guard 放行。
    grants.current = ['yuheng:llm:read']
    renderPage()

    await screen.findByText('模型网关配置')
    expect(screen.getByText('local-main')).toBeInTheDocument()
    expect(screen.getByText('无认证')).toBeInTheDocument()
    // 只显示部署引用名。
    expect(screen.getByText('llm/cloud-eu-key')).toBeInTheDocument()
    expect(document.body.textContent).not.toContain(SECRET_SENTINEL)
    expect(llm.channels).toHaveBeenCalledTimes(1)
    expect(llm.models).toHaveBeenCalledTimes(1)
    // 没有额外的租户/预取请求，也没有任何写请求。
    expect(llm.replaceChannel).not.toHaveBeenCalled()
    expect(llm.replaceModel).not.toHaveBeenCalled()
  })

  it('keeps every channel write action disabled without llm:write', async () => {
    grants.current = ['yuheng:llm:read']
    renderPage()
    await screen.findByText('local-main')

    expect(buttonIn(document.body, '新建渠道')).toBeDisabled()
    expect(buttonIn(rowElement('cloud-eu'), '编辑渠道')).toBeDisabled()
    expect(buttonIn(rowElement('local-main'), '停用渠道')).toBeDisabled()
  })

  it('opens the model tab from the URL and keeps writes disabled without llm:write', async () => {
    grants.current = ['yuheng:llm:read']
    renderPage('/llm/configuration?tab=models')

    await screen.findByText('embed-local')
    expect(screen.getByRole('tab', {name: '模型映射', selected: true})).toBeInTheDocument()
    expect(screen.getByText('space-default · 1024 维（不可原地更换）')).toBeInTheDocument()
    expect(screen.getByText('embed-cloud')).toBeInTheDocument()
    expect(modelRow('embed-local').getByRole('button', {name: '编辑模型'})).toBeDisabled()
    expect(screen.getByRole('button', {name: '新建模型 alias'})).toBeDisabled()
  })

  it('reports a read failure and drops the cached projection when retrying a 403', async () => {
    grants.current = ['yuheng:llm:read', 'yuheng:llm:write']
    llm.channels.mockRejectedValue(new GatewayApiError(
      403,
      'YUHENG_ADMIN_CAPABILITY_REQUIRED',
      'llm configuration read revoked',
    ))
    const {queryClient} = renderPage()
    const removeQueries = vi.spyOn(queryClient, 'removeQueries')

    await screen.findByText('llm configuration read revoked')
    // antd 在两个汉字的按钮名里插入空格，实际可访问名是「重 试」。
    fireEvent.click(screen.getByRole('button', {name: /重\s*试/}))
    await waitFor(() => expect(removeQueries).toHaveBeenCalledWith({queryKey: ['llm']}), {timeout: 8_000})
    expect(document.body.textContent).not.toContain(SECRET_SENTINEL)
  })

  it('submits a blank secret reference as null with revision 0 for a new LOCAL channel', async () => {
    grants.current = ['yuheng:llm:read', 'yuheng:llm:write']
    renderPage()
    await screen.findByText('local-main')

    fireEvent.click(screen.getByRole('button', {name: '新建渠道'}))
    fireEvent.change(screen.getByLabelText('渠道键输入'), {target: {value: 'local-backup'}})
    // 展示名允许带空白：命令侧必须按 trim 后的值提交，渠道键则由 pattern 直接拒绝空白。
    fireEvent.change(screen.getByLabelText('渠道名称输入'), {target: {value: ' 本地备用渠道 '}})
    fireEvent.change(screen.getByLabelText('渠道根地址输入'), {target: {value: 'http://10.0.0.9:8000/v1'}})
    fireEvent.click(screen.getByRole('button', {name: '保存渠道'}))

    await waitFor(() => expect(llm.replaceChannel).toHaveBeenCalledWith('local-backup', {
      key: 'local-backup',
      name: '本地备用渠道',
      deployment: 'LOCAL',
      protocol: 'OPENAI_CHAT',
      baseUrl: 'http://10.0.0.9:8000/v1',
      secretRef: null,
      enabled: true,
      connectTimeoutMs: 3_000,
      headerTimeoutMs: 30_000,
      idleTimeoutMs: 30_000,
      totalTimeoutMs: 120_000,
      maxConcurrent: 16,
      expectedRevision: 0,
    }), {timeout: 8_000})
    // 命令按合同白名单构造，不透传服务端多返回的字段。
    expect(commandOf(0)).not.toHaveProperty('apiKey')
    expect(JSON.stringify(commandOf(0))).not.toContain(SECRET_SENTINEL)
  })

  it('keeps the channel key immutable and requires a secret reference for CLOUD', async () => {
    grants.current = ['yuheng:llm:read', 'yuheng:llm:write']
    renderPage()
    await screen.findByText('local-main')

    fireEvent.click(channelRow('local-main').getByRole('button', {name: '编辑渠道'}))
    expect(screen.getByLabelText('渠道键输入')).toBeDisabled()
    expect(screen.getByLabelText('渠道键输入')).toHaveValue('local-main')
    expect(screen.getByText('当前版本')).toBeInTheDocument()

    // rc-select 的键盘提交依赖下拉动画，这里走鼠标路径。
    fireEvent.mouseDown(screen.getByRole('combobox', {name: '部署选择'}))
    fireEvent.click(await screen.findByText('CLOUD（可能出域）', {}, {timeout: 8_000}))
    fireEvent.click(screen.getByRole('button', {name: '保存渠道'}))

    expect(await screen.findByText('CLOUD 渠道必须提供密钥引用名')).toBeInTheDocument()
    expect(llm.replaceChannel).not.toHaveBeenCalled()
  })

  it('toggles a channel using the loaded revision as the CAS token', async () => {
    grants.current = ['yuheng:llm:read', 'yuheng:llm:write']
    renderPage()
    await screen.findByText('local-main')

    fireEvent.click(channelRow('local-main').getByRole('button', {name: '停用渠道'}))
    await waitFor(() => expect(llm.replaceChannel).toHaveBeenCalledWith('local-main', expect.objectContaining({
      enabled: false,
      secretRef: null,
      expectedRevision: 3,
    })), {timeout: 8_000})
    expect(commandOf(0)).not.toHaveProperty('revision')
  })

  it('keeps the typed draft when the channel save loses the revision race', async () => {
    grants.current = ['yuheng:llm:read', 'yuheng:llm:write']
    llm.replaceChannel.mockRejectedValue(new GatewayApiError(
      409,
      'YUHENG_ADMIN_REVISION_CONFLICT',
      'channel revision conflict',
      undefined,
      9,
    ))
    renderPage()
    await screen.findByText('local-main')

    fireEvent.click(screen.getByRole('button', {name: '新建渠道'}))
    fireEvent.change(screen.getByLabelText('渠道键输入'), {target: {value: 'local-draft'}})
    fireEvent.change(screen.getByLabelText('渠道名称输入'), {target: {value: '并发失败的草稿'}})
    fireEvent.change(screen.getByLabelText('渠道根地址输入'), {target: {value: 'http://10.0.0.10:8000/v1'}})
    fireEvent.click(screen.getByRole('button', {name: '保存渠道'}))

    expect(await screen.findByText('channel revision conflict', {}, {timeout: 8_000})).toBeInTheDocument()
    expect(screen.getByText('渠道版本已被他人推进：你的输入已保留，请刷新比较后再提交。')).toBeInTheDocument()
    // 409 不丢用户输入：抽屉仍在，键值仍可读。
    expect(screen.getByLabelText('渠道名称输入')).toHaveValue('并发失败的草稿')
  })

  it('refuses an embedding alias routed to a CLOUD channel and sends no request', async () => {
    grants.current = ['yuheng:llm:read', 'yuheng:llm:write']
    renderPage('/llm/configuration?tab=models')
    await screen.findByText('embed-cloud')

    fireEvent.click(modelRow('embed-cloud').getByRole('button', {name: '编辑模型'}))
    // 已绑定嵌入空间的 alias 不可原地更换空间与维度。
    expect(screen.getByLabelText('嵌入空间输入')).toBeDisabled()
    fireEvent.click(screen.getByRole('button', {name: '保存模型'}))

    expect(await screen.findByText('嵌入模型的路由包含 CLOUD 渠道', {}, {timeout: 8_000})).toBeInTheDocument()
    expect(screen.getByText('嵌入必须全部落在 LOCAL 渠道；已拒绝提交，请改用本地渠道，系统不会回退到云端嵌入。')).toBeInTheDocument()
    // 前端先拒绝，服务端才会权威复核：这里绝不发出创建云嵌入的命令。
    expect(llm.replaceModel).not.toHaveBeenCalled()
    expect(modelRow('embed-cloud').getByRole('button', {name: '编辑模型'})).toBeInTheDocument()
  })

  it('submits a LOCAL-only embedding alias with its space and dimensions', async () => {
    grants.current = ['yuheng:llm:read', 'yuheng:llm:write']
    renderPage('/llm/configuration?tab=models')
    await screen.findByText('embed-local')

    fireEvent.click(modelRow('embed-local').getByRole('button', {name: '编辑模型'}))
    fireEvent.click(screen.getByRole('button', {name: '保存模型'}))

    await waitFor(() => expect(llm.replaceModel).toHaveBeenCalledWith('embed-local', {
      key: 'embed-local',
      name: '本地嵌入模型',
      kind: 'EMBEDDING',
      protocols: ['OPENAI_EMBEDDING'],
      enabled: true,
      dimensions: 1024,
      embeddingSpaceId: 'space-default',
      allowedSubjects: ['svc-ingest'],
      routes: [{
        channelKey: 'local-main',
        upstreamModel: 'bge-m3',
        capabilities: ['TEXT'],
        priority: 100,
        weight: 1,
      }],
      expectedRevision: 2,
    }), {timeout: 8_000})
    expect(JSON.stringify(modelCommandOf(0))).not.toContain(SECRET_SENTINEL)
  })

  it('sends null dimensions and embedding space when saving a CHAT alias', async () => {
    grants.current = ['yuheng:llm:read', 'yuheng:llm:write']
    renderPage('/llm/configuration?tab=models')
    await screen.findByText('chat-main')

    fireEvent.click(modelRow('chat-main').getByRole('button', {name: '编辑模型'}))
    expect(screen.getByLabelText('模型别名输入')).toBeDisabled()
    fireEvent.click(screen.getByRole('button', {name: '保存模型'}))

    await waitFor(() => expect(llm.replaceModel).toHaveBeenCalledWith('chat-main', expect.objectContaining({
      kind: 'CHAT',
      dimensions: null,
      embeddingSpaceId: null,
      expectedRevision: 4,
    })), {timeout: 8_000})
    // 前端不发送残留值，避免「看起来还能改」的错觉。
    expect(modelCommandOf(0)).not.toHaveProperty('revision')
  })
})
