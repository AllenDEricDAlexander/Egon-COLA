import {apiRequest} from './client'
import {newIdempotencyKey} from './trace'
import type {KnowledgeJob} from './knowledge'
import type {Page} from './types'

// 原业务Spec §9.2.23–§9.2.28、§9.2.31（API-023–028/031）typed lifecycle/page/graph client。
// publicationStatus/reviewStatus/publicationPolicySnapshot 等描述 contentRevisionId 所指版本；
// 没有正文版本时为 null，前端不得用 page 状态顶替。

const admin = '/api/v1/yuheng/admin'

export const wikiPublicationStatusValues = [
  'DRAFT',
  'PUBLISHING',
  'PUBLISHED',
  'SUPERSEDED',
  'ARCHIVED',
] as const
export const wikiReviewStatusValues = [
  'NOT_REQUIRED',
  'NOT_SUBMITTED',
  'PENDING',
  'APPROVED',
  'REJECTED',
  'CANCELLED',
] as const
export const wikiPublicationPolicyValues = ['DIRECT', 'REVIEW_REQUIRED'] as const

export type WikiPublicationStatus = (typeof wikiPublicationStatusValues)[number]
export type WikiReviewStatus = (typeof wikiReviewStatusValues)[number]
export type WikiPublicationPolicy = (typeof wikiPublicationPolicyValues)[number]

export type WikiSource = {
  documentRevisionId: string
  chunkId: string
  sourceHash: string
}

export type WikiPageView = {
  id: string
  kbId: string
  slug: string
  title: string
  draftRevisionId: string | null
  publishedRevisionId: string | null
  publicationStatus: WikiPublicationStatus | string | null
  reviewStatus: WikiReviewStatus | string | null
  publicationPolicySnapshot: WikiPublicationPolicy | string | null
  publicationVersion: number | null
  publicationErrorCode: string | null
  contentRevisionId: string | null
  markdown: string
  tags: string[]
  /** 同 KB pageId 数组，最多 100；未解析目标保留告警而不伪造。 */
  links: string[]
  sources: WikiSource[]
  stale: boolean
  revision: number
}

export type WikiDraftCommand = {
  title: string
  markdown: string
  tags: string[]
  links: string[]
  sources: WikiSource[]
  expectedRevision: number
}

export type WikiGenerationCommand = {
  sourceRevisionIds: string[]
  pageId: string | null
  expectedRevision: number
}

export type WikiPublicationCommand = {
  draftRevisionId: string
  expectedRevision: number
}

export type WikiGraphNode = {
  id: string
  title: string
}

export type WikiGraphEdge = {
  /** 起点页面稳定 ID；服务端字段名为 source/target（WikiGraphVO.WikiGraphEdgeVO）。 */
  source: string
  target: string
}

export type WikiGraph = {
  nodes: WikiGraphNode[]
  edges: WikiGraphEdge[]
  truncated: boolean
}

const query = (values: Record<string, string | number | boolean | undefined>): string => {
  const params = new URLSearchParams()
  Object.entries(values).forEach(([name, value]) => {
    if (value !== undefined && `${value}` !== '') params.set(name, `${value}`)
  })
  const serialized = params.toString()
  return serialized ? `?${serialized}` : ''
}

const pagesPath = (kbId: string) =>
  `${admin}/knowledge-bases/${encodeURIComponent(kbId)}/wiki/pages`

export const wikiApi = {
  /** API-023 GET .../wiki/pages：includeDraft=true 需要 EDITOR。 */
  pages: (
    kbId: string,
    filter: {
      includeDraft?: boolean
      search?: string
      tag?: string
      page?: number
      size?: number
    } = {},
    signal?: AbortSignal,
  ) => apiRequest<Page<WikiPageView>>(`${pagesPath(kbId)}${query({
    includeDraft: filter.includeDraft,
    search: filter.search,
    tag: filter.tag,
    page: filter.page ?? 1,
    size: filter.size ?? 20,
  })}`, { signal }),
  /** API-024 POST .../wiki/generation-jobs：202 只代表任务受理。 */
  createGenerationJob: (kbId: string, command: WikiGenerationCommand) =>
    apiRequest<KnowledgeJob>(
      `${admin}/knowledge-bases/${encodeURIComponent(kbId)}/wiki/generation-jobs`,
      { method: 'POST', body: command, idempotencyKey: newIdempotencyKey() },
    ),
  /** API-025 GET .../wiki/pages/{pageId}?revisionId= */
  page: (kbId: string, pageId: string, revisionId?: string, signal?: AbortSignal) =>
    apiRequest<WikiPageView>(
      `${pagesPath(kbId)}/${encodeURIComponent(pageId)}${query({ revisionId })}`,
      { signal },
    ),
  /** API-026 PUT .../wiki/pages/{pageId}/draft：保存草稿不改变已发布内容。 */
  replaceDraft: (kbId: string, pageId: string, command: WikiDraftCommand) =>
    apiRequest<WikiPageView>(`${pagesPath(kbId)}/${encodeURIComponent(pageId)}/draft`, {
      method: 'PUT',
      body: command,
    }),
  /** API-027 POST .../wiki/pages/{pageId}/publications：DIRECT 策略直接发布。 */
  publish: (kbId: string, pageId: string, command: WikiPublicationCommand) =>
    apiRequest<WikiPageView>(
      `${pagesPath(kbId)}/${encodeURIComponent(pageId)}/publications`,
      { method: 'POST', body: command },
    ),
  /** API-031 DELETE .../wiki/pages/{pageId}/publications/current?expectedRevision= */
  unpublish: (kbId: string, pageId: string, expectedRevision: number) =>
    apiRequest<void>(
      `${pagesPath(kbId)}/${encodeURIComponent(pageId)}/publications/current${query({expectedRevision})}`,
      { method: 'DELETE' },
    ),
  /** API-028 GET .../wiki/graph：只返回授权已发布节点。 */
  graph: (kbId: string, pageId?: string, limit = 100, signal?: AbortSignal) =>
    apiRequest<WikiGraph>(
      `${admin}/knowledge-bases/${encodeURIComponent(kbId)}/wiki/graph${query({pageId, limit})}`,
      { signal },
    ),
}
