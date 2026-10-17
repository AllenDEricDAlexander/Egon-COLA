import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {
  createAiResource,
  deleteAiResource,
  ddcAiQueryKeys,
  getAiResource,
  getAiOptimizationPolicy,
  getAiOptimizationRun,
  getAiVersion,
  pageAiOptimizationRuns,
  pageAiPublications,
  pageAiResources,
  pageAiUsageFeedback,
  pageAiVersions,
  promoteAiVersion,
  publishAiDraft,
  replaceAiDraft,
  replaceAiOptimizationPolicy,
  rollbackAiPublication,
  triggerAiOptimizationRun,
} from './ai'
import {beginDdcSession, setDdcExpectedTenantId} from './client'
import type {
  DdcAiContent,
  DdcAiCreateCommand,
  DdcAiDraftCommand,
  DdcAiPromotionCommand,
  DdcAiPublishCommand,
  DdcAiRollbackCommand,
} from './types'

const record = (data: unknown) => ({
  success: true,
  code: 10000,
  status: 'SUCCESS',
  message: 'success',
  data,
  traceId: 'trace-ai',
  timestamp: 1,
})

const pageRecord = (records: unknown[]) => ({
  success: true,
  code: 10000,
  status: 'SUCCESS',
  message: 'success',
  records,
  page: { total: records.length, pageNo: 1, pageSize: 20, pages: 1, hasNext: false, hasPrevious: false },
  traceId: 'trace-ai-page',
  timestamp: 1,
})

const jsonResponse = (body: unknown, status = 200) => new Response(
  JSON.stringify(body),
  {status, headers: {'Content-Type': 'application/json'}},
)

const content: DdcAiContent = {
  promptText: null,
  nodes: [
    {path: '', nodeType: 'DIRECTORY', content: null},
    {path: 'SKILL.md', nodeType: 'FILE', content: 'Review the order.'},
  ],
}

const scope = {bizCode: 'retail', namespaceCode: 'dev-space', env: 'dev', appCode: 'assistant'}

describe('Tianshu AI REST client', () => {
  beforeEach(() => {
    beginDdcSession()
    setDdcExpectedTenantId('1001')
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    setDdcExpectedTenantId(undefined)
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
  })

  it('pages resources with the complete scope, kind, literal prefix and tenant header', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(pageRecord([{id: '810001'}])))

    await pageAiResources({...scope, kind: 'SKILL', resourceKeyPrefix: 'review%', pageNo: 2, pageSize: 40})

    const [url, init] = vi.mocked(fetch).mock.calls[0]
    const requestUrl = new URL(String(url), 'https://tianshu.example')
    expect(requestUrl.pathname).toBe('/api/v1/tianshu/ai/resources')
    expect(requestUrl.searchParams.get('bizCode')).toBe('retail')
    expect(requestUrl.searchParams.get('namespaceCode')).toBe('dev-space')
    expect(requestUrl.searchParams.get('env')).toBe('dev')
    expect(requestUrl.searchParams.get('appCode')).toBe('assistant')
    expect(requestUrl.searchParams.get('kind')).toBe('SKILL')
    expect(requestUrl.searchParams.get('resourceKeyPrefix')).toBe('review%')
    expect(requestUrl.searchParams.get('pageNo')).toBe('2')
    expect(requestUrl.searchParams.get('pageSize')).toBe('40')
    expect((init?.headers as Headers).get('X-Egon-Tianshu-Tenant')).toBe('1001')
  })

  it('creates a draft without accepting tenant identity in the request body', async () => {
    const command: DdcAiCreateCommand = {
      scope,
      kind: 'SKILL',
      resourceKey: 'review-order',
      displayName: 'Order review',
      description: '',
      draft: content,
    }
    vi.mocked(fetch).mockResolvedValue(jsonResponse(record({resource: {id: '810001'}, draft: content}), 201))

    await createAiResource(command)

    const [url, init] = vi.mocked(fetch).mock.calls[0]
    expect(String(url)).toBe('/api/v1/tianshu/ai/resources')
    expect(init?.method).toBe('POST')
    expect(JSON.parse(String(init?.body))).toEqual(command)
    expect(JSON.parse(String(init?.body))).not.toHaveProperty('tenantId')
  })

  it('reads a resource using an exact Long string path value', async () => {
    const id = '9223372036854775807'
    vi.mocked(fetch).mockResolvedValue(jsonResponse(record({resource: {id}, draft: content})))

    await getAiResource(id)

    expect(String(vi.mocked(fetch).mock.calls[0][0]))
      .toBe(`/api/v1/tianshu/ai/resources/${id}`)
  })

  it('replaces the complete draft with a string revision', async () => {
    const command: DdcAiDraftCommand = {
      expectedRevision: '9223372036854775807',
      displayName: 'Order review updated',
      description: '',
      draft: content,
    }
    vi.mocked(fetch).mockResolvedValue(jsonResponse(record({resource: {id: '810001'}, draft: content})))

    await replaceAiDraft('810001', command)

    const [url, init] = vi.mocked(fetch).mock.calls[0]
    expect(String(url)).toBe('/api/v1/tianshu/ai/resources/810001/draft')
    expect(init?.method).toBe('PUT')
    expect(JSON.parse(String(init?.body))).toEqual(command)
    expect(JSON.parse(String(init?.body)).expectedRevision).toBe('9223372036854775807')
  })

  it('deletes with expectedRevision in the query and accepts an empty 204 body', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response(null, {status: 204}))

    await expect(deleteAiResource('810001', '4')).resolves.toBeUndefined()

    const [url, init] = vi.mocked(fetch).mock.calls[0]
    expect(new URL(String(url), 'https://tianshu.example').searchParams.get('expectedRevision')).toBe('4')
    expect(init?.method).toBe('DELETE')
    expect(init?.body).toBeUndefined()
  })

  it('pages immutable versions using numeric page controls and string resource identity', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(pageRecord([{id: '820001', versionNo: '1'}])))

    await pageAiVersions('810001', {pageNo: 2, pageSize: 10})

    const requestUrl = new URL(String(vi.mocked(fetch).mock.calls[0][0]), 'https://tianshu.example')
    expect(requestUrl.pathname).toBe('/api/v1/tianshu/ai/resources/810001/versions')
    expect(requestUrl.searchParams.get('pageNo')).toBe('2')
    expect(requestUrl.searchParams.get('pageSize')).toBe('10')
  })

  it('reads one immutable version without converting its Long string', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(record({version: {versionNo: '9007199254740993'}, content})))

    await getAiVersion('810001', '9007199254740993')

    expect(String(vi.mocked(fetch).mock.calls[0][0]))
      .toBe('/api/v1/tianshu/ai/resources/810001/versions/9007199254740993')
  })

  it('reuses the supplied Idempotency-Key for publish retries', async () => {
    const command: DdcAiPublishCommand = {expectedRevision: '7', changeNote: 'release'}
    vi.mocked(fetch).mockResolvedValue(jsonResponse(record({id: '830001'}), 201))

    await publishAiDraft('810001', 'publish_order_0001', command)
    await publishAiDraft('810001', 'publish_order_0001', command)

    const calls = vi.mocked(fetch).mock.calls
    expect(calls).toHaveLength(2)
    expect(calls.map(([, init]) => (init?.headers as Headers).get('Idempotency-Key')))
      .toEqual(['publish_order_0001', 'publish_order_0001'])
    expect(calls[0][0]).toBe('/api/v1/tianshu/ai/resources/810001/publications')
  })

  it('rolls back using the selected version and an idempotency key', async () => {
    const command: DdcAiRollbackCommand = {
      expectedRevision: '8',
      versionNo: '3',
      changeNote: 'restore version 3',
    }
    vi.mocked(fetch).mockResolvedValue(jsonResponse(record({id: '830002'})))

    await rollbackAiPublication('810001', 'rollback_order_0001', command)

    const [url, init] = vi.mocked(fetch).mock.calls[0]
    expect(url).toBe('/api/v1/tianshu/ai/resources/810001/rollbacks')
    expect((init?.headers as Headers).get('Idempotency-Key')).toBe('rollback_order_0001')
    expect(JSON.parse(String(init?.body))).toEqual(command)
  })

  it('promotes a fixed source version and preserves a null target revision', async () => {
    const command: DdcAiPromotionCommand = {
      sourceVersionNo: '3',
      targetScope: {...scope, namespaceCode: 'test-space'},
      targetResourceKey: 'review-order-test',
      expectedTargetRevision: null,
      changeNote: 'promote tested skill',
    }
    vi.mocked(fetch).mockResolvedValue(jsonResponse(record({id: '830003'}), 201))

    await promoteAiVersion('810001', 'promote_order_0001', command)

    const [url, init] = vi.mocked(fetch).mock.calls[0]
    expect(url).toBe('/api/v1/tianshu/ai/resources/810001/promotions')
    expect((init?.headers as Headers).get('Idempotency-Key')).toBe('promote_order_0001')
    expect(JSON.parse(String(init?.body))).toEqual(command)
    expect(JSON.parse(String(init?.body))).toHaveProperty('expectedTargetRevision', null)
  })

  it('pages publication audit with an optional exact requestId filter', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(pageRecord([{id: '830001'}])))

    await pageAiPublications('810001', {requestId: 'publish_order_0001', pageNo: 1, pageSize: 20})

    const requestUrl = new URL(String(vi.mocked(fetch).mock.calls[0][0]), 'https://tianshu.example')
    expect(requestUrl.pathname).toBe('/api/v1/tianshu/ai/resources/810001/publications')
    expect(requestUrl.searchParams.get('requestId')).toBe('publish_order_0001')
  })

  it('keys AI cache entries by the verified tenant', () => {
    expect(ddcAiQueryKeys.resource('810001'))
      .toEqual(['tianshu', '1001', 'ai', 'resource', '810001'])
  })

  it('refuses to call a new AI endpoint before tenant bootstrap completes', async () => {
    setDdcExpectedTenantId(undefined)
    expect(() => getAiResource('810001')).toThrow('verified Tianshu tenant')
    expect(vi.mocked(fetch)).not.toHaveBeenCalled()
  })
  it('reads and replaces the optimization policy with a PUT and revision CAS', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(record({resourceId: '810001', revision: '1'})))

    await getAiOptimizationPolicy('810001')
    await replaceAiOptimizationPolicy('810001', {
      expectedRevision: '0', rearm: false, enabled: true, scoreThreshold: 80, minSamples: 20,
      evaluationWindow: 'PT24H', maxIterations: 5, cooldown: 'PT1H', autoPublish: false, judgeEnabled: true,
      optimizerModelAlias: 'gpt-opt', judgeModelAlias: 'gpt-judge', optimizerInstruction: '',
    })

    const [getUrl] = vi.mocked(fetch).mock.calls[0]
    const [putUrl, putInit] = vi.mocked(fetch).mock.calls[1]
    expect(getUrl).toBe('/api/v1/tianshu/ai/resources/810001/optimization-policy')
    expect(putUrl).toBe('/api/v1/tianshu/ai/resources/810001/optimization-policy')
    expect(putInit?.method).toBe('PUT')
    expect(JSON.parse(String(putInit?.body))).toMatchObject({expectedRevision: '0', evaluationWindow: 'PT24H'})
    expect(() => replaceAiOptimizationPolicy('810001', {
      expectedRevision: '-1', rearm: false, enabled: true, scoreThreshold: 80, minSamples: 20,
      evaluationWindow: 'PT24H', maxIterations: 5, cooldown: 'PT1H', autoPublish: false, judgeEnabled: true,
      optimizerModelAlias: 'gpt-opt', judgeModelAlias: 'gpt-judge', optimizerInstruction: '',
    })).toThrow('expectedRevision')
  })

  it('triggers a run with an Idempotency-Key and reads runs and feedback by exact paths', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(jsonResponse(record({id: '88', state: 'PENDING'}), 202))
      .mockResolvedValueOnce(jsonResponse(pageRecord([])))
      .mockResolvedValueOnce(jsonResponse(record({id: '88', state: 'RUNNING'})))
      .mockResolvedValueOnce(jsonResponse(pageRecord([])))

    await triggerAiOptimizationRun('810001', 'opt_request_000001', {note: 'now'})
    await pageAiOptimizationRuns('810001', {state: 'FAILED', pageNo: 2, pageSize: 10})
    await getAiOptimizationRun('810001', '88')
    await pageAiUsageFeedback('810001', {versionNo: '3', outcome: 'FAILURE', scoreSource: 'JUDGE', maxScore: 60})

    const calls = vi.mocked(fetch).mock.calls
    expect(calls[0][0]).toBe('/api/v1/tianshu/ai/resources/810001/optimization-runs')
    expect(calls[0][1]?.method).toBe('POST')
    expect(new Headers(calls[0][1]?.headers).get('Idempotency-Key')).toBe('opt_request_000001')
    expect(calls[1][0]).toBe('/api/v1/tianshu/ai/resources/810001/optimization-runs?state=FAILED&pageNo=2&pageSize=10')
    expect(calls[2][0]).toBe('/api/v1/tianshu/ai/resources/810001/optimization-runs/88')
    expect(calls[3][0]).toBe('/api/v1/tianshu/ai/resources/810001/usage-feedback?versionNo=3&outcome=FAILURE'
      + '&scoreSource=JUDGE&maxScore=60&pageNo=1&pageSize=20')
    expect(() => triggerAiOptimizationRun('810001', 'short', {note: ''})).toThrow('requestId')
  })
})
