import {apiRequest} from './client'
import {newIdempotencyKey} from './trace'
import type {Page} from './types'

// 原业务Spec §9.2.8–§9.2.22（API-008–022）typed client。
// actorId/tenantId 由服务端从可信身份派生，任何命令体都不携带；ID 一律 string，不转 JS number。

const admin = '/api/v1/yuheng/admin'

export const knowledgeMemberRoleValues = ['READER', 'EDITOR', 'OWNER'] as const
export const knowledgeEgressPolicyValues = ['LOCAL_ONLY', 'CLOUD_ALLOWED'] as const
export const knowledgeJobStatusValues = [
  'QUEUED',
  'RUNNING',
  'RETRY_WAIT',
  'SUCCEEDED',
  'FAILED',
  'STALE',
  'CANCELLED',
] as const
export const knowledgeJobStageValues = [
  'QUEUED',
  'PARSE',
  'EMBED',
  'GENERATE',
  'PUBLISH',
  'DONE',
] as const
export const knowledgeDocumentStatusValues = ['READY', 'PROCESSING', 'FAILED'] as const
// 资料“版本”的状态与“资料”的状态是两套枚举：版本为 STAGING/READY/FAILED（KnowledgeRevisionStatusEnum），
// 不含 PROCESSING；两者不能互相顶替。
export const knowledgeRevisionStatusValues = ['STAGING', 'READY', 'FAILED'] as const
export const knowledgeSourceModeValues = ['DOCUMENTS', 'WIKI', 'BOTH'] as const
export const knowledgeSearchModeValues = ['VECTOR', 'KEYWORD', 'HYBRID'] as const

export type KnowledgeMemberRole = (typeof knowledgeMemberRoleValues)[number]
export type KnowledgeEgressPolicy = (typeof knowledgeEgressPolicyValues)[number]
export type KnowledgeJobStatus = (typeof knowledgeJobStatusValues)[number]
export type KnowledgeJobStage = (typeof knowledgeJobStageValues)[number]
export type KnowledgeDocumentStatus = (typeof knowledgeDocumentStatusValues)[number]
export type KnowledgeRevisionStatus = (typeof knowledgeRevisionStatusValues)[number]
export type KnowledgeSourceMode = (typeof knowledgeSourceModeValues)[number]
export type KnowledgeSearchMode = (typeof knowledgeSearchModeValues)[number]

/** 未终态任务：只有存在这些状态时前端才继续 2 秒轮询。 */
export const activeKnowledgeJobStatuses: readonly KnowledgeJobStatus[] = [
  'QUEUED',
  'RUNNING',
  'RETRY_WAIT',
]

export const isTerminalKnowledgeJob = (status: string): boolean =>
  !activeKnowledgeJobStatuses.includes(status as KnowledgeJobStatus)

export type KnowledgeBase = {
  id: string
  name: string
  description: string
  ownerActorId: string
  myRole: KnowledgeMemberRole | string
  egressPolicy: KnowledgeEgressPolicy | string
  chatModel: string
  embeddingModel: string
  embeddingSpaceId: string | null
  dimensions: number | null
  revision: number
}

export type KnowledgeBaseCommand = {
  name: string
  description: string
  egressPolicy: KnowledgeEgressPolicy | string
  chatModel: string
  embeddingModel: string
}

export type KnowledgeBaseUpdate = KnowledgeBaseCommand & {
  expectedRevision: number
}

export type KnowledgeMember = {
  actorId: string
  role: KnowledgeMemberRole | string
}

export type KnowledgeMembers = {
  members: KnowledgeMember[]
  revision: number
}

export type KnowledgeMembersCommand = {
  members: KnowledgeMember[]
  expectedRevision: number
}

export type KnowledgeDocument = {
  id: string
  kbId: string
  fileName: string
  activeRevisionId: string | null
  latestJobId: string | null
  revision: number
  status: KnowledgeDocumentStatus | string
  createdAt: string
}

export type KnowledgeDocumentRevision = {
  id: string
  documentId: string
  kbId: string
  fileName: string
  mediaType: string
  byteCount: number
  text: string
  hash: string
  status: KnowledgeRevisionStatus | string
  createdAt: string
}

/** API-015 的 202 回执：只代表任务已受理，不代表摄取成功。 */
export type KnowledgeUploadReceipt = {
  documentId: string
  revisionId: string
  jobId: string
  status: KnowledgeJobStatus | string
}

export type KnowledgeJob = {
  id: string
  kbId: string
  type: string
  resourceId: string
  status: KnowledgeJobStatus | string
  stage: KnowledgeJobStage | string
  revision: number
  attempt: number
  errorCode: string | null
  createdAt: string
  updatedAt: string
}

export type KnowledgeReindexCommand = {
  sourceRevisionId: string
  expectedRevision: number
}

export type KnowledgeAnswerCommand = {
  question: string
  sourceMode: KnowledgeSourceMode | string
  topK: number
  searchMode: KnowledgeSearchMode | string
}

export type KnowledgeCitation = {
  documentRevisionId: string
  chunkId: string
  sourceHash: string
  documentId: string
  fileName: string
  pageId: string | null
  excerpt: string
  score: number
}

export type KnowledgeAnswer = {
  outcome: 'ANSWERED' | 'NO_EVIDENCE' | string
  answer: string
  citations: KnowledgeCitation[]
  model: string
}

const query = (values: Record<string, string | number | boolean | undefined>): string => {
  const params = new URLSearchParams()
  Object.entries(values).forEach(([name, value]) => {
    if (value !== undefined && `${value}` !== '') params.set(name, `${value}`)
  })
  const serialized = params.toString()
  return serialized ? `?${serialized}` : ''
}

const kbPath = (kbId: string) => `${admin}/knowledge-bases/${encodeURIComponent(kbId)}`

export const knowledgeApi = {
  /** API-008 GET /knowledge-bases：只返回当前主体作为成员的 KB。 */
  knowledgeBases: (
    filter: { name?: string; page?: number; size?: number } = {},
    signal?: AbortSignal,
  ) => apiRequest<Page<KnowledgeBase>>(`${admin}/knowledge-bases${query({
    name: filter.name,
    page: filter.page ?? 1,
    size: filter.size ?? 20,
  })}`, { signal }),
  /** API-009 POST /knowledge-bases（Idempotency-Key 必填）。 */
  createKnowledgeBase: (command: KnowledgeBaseCommand) =>
    apiRequest<KnowledgeBase>(`${admin}/knowledge-bases`, {
      method: 'POST',
      body: command,
      idempotencyKey: newIdempotencyKey(),
    }),
  /** API-010 GET /knowledge-bases/{kbId} */
  knowledgeBase: (kbId: string, signal?: AbortSignal) =>
    apiRequest<KnowledgeBase>(kbPath(kbId), { signal }),
  /** API-011 PUT /knowledge-bases/{kbId}：owner 完整替换设置。 */
  replaceKnowledgeBase: (kbId: string, command: KnowledgeBaseUpdate) =>
    apiRequest<KnowledgeBase>(kbPath(kbId), { method: 'PUT', body: command }),
  /** API-012 GET /knowledge-bases/{kbId}/members */
  members: (kbId: string, signal?: AbortSignal) =>
    apiRequest<KnowledgeMembers>(`${kbPath(kbId)}/members`, { signal }),
  /** API-013 PUT /knowledge-bases/{kbId}/members */
  replaceMembers: (kbId: string, command: KnowledgeMembersCommand) =>
    apiRequest<KnowledgeMembers>(`${kbPath(kbId)}/members`, { method: 'PUT', body: command }),
  /** API-014 GET /knowledge-bases/{kbId}/documents */
  documents: (kbId: string, page = 1, size = 20, signal?: AbortSignal) =>
    apiRequest<Page<KnowledgeDocument>>(`${kbPath(kbId)}/documents${query({page, size})}`, {
      signal,
    }),
  /** API-015 POST /knowledge-bases/{kbId}/documents：multipart FormData。 */
  uploadDocument: (kbId: string, form: FormData) =>
    apiRequest<KnowledgeUploadReceipt>(`${kbPath(kbId)}/documents`, {
      method: 'POST',
      body: form,
      idempotencyKey: newIdempotencyKey(),
    }),
  /** API-016 GET .../documents/{documentId}/revisions/{revisionId}：授权原文版本。 */
  documentRevision: (kbId: string, documentId: string, revisionId: string, signal?: AbortSignal) =>
    apiRequest<KnowledgeDocumentRevision>(
      `${kbPath(kbId)}/documents/${encodeURIComponent(documentId)}/revisions/${encodeURIComponent(revisionId)}`,
      { signal },
    ),
  /** API-017 DELETE .../documents/{documentId}?expectedRevision= */
  deleteDocument: (kbId: string, documentId: string, expectedRevision: number) =>
    apiRequest<void>(
      `${kbPath(kbId)}/documents/${encodeURIComponent(documentId)}${query({expectedRevision})}`,
      { method: 'DELETE' },
    ),
  /** API-018 POST .../documents/{documentId}/reindex-jobs */
  createReindexJob: (kbId: string, documentId: string, command: KnowledgeReindexCommand) =>
    apiRequest<KnowledgeJob>(
      `${kbPath(kbId)}/documents/${encodeURIComponent(documentId)}/reindex-jobs`,
      { method: 'POST', body: command, idempotencyKey: newIdempotencyKey() },
    ),
  /** API-019 GET /knowledge-jobs/{jobId} */
  job: (jobId: string, signal?: AbortSignal) =>
    apiRequest<KnowledgeJob>(`${admin}/knowledge-jobs/${encodeURIComponent(jobId)}`, { signal }),
  /** API-020 GET /knowledge-bases/{kbId}/jobs */
  jobs: (
    kbId: string,
    filter: { status?: string; page?: number; size?: number } = {},
    signal?: AbortSignal,
  ) => apiRequest<Page<KnowledgeJob>>(`${kbPath(kbId)}/jobs${query({
    status: filter.status,
    page: filter.page ?? 1,
    size: filter.size ?? 20,
  })}`, { signal }),
  /** API-021 POST /knowledge-jobs/{jobId}/retries */
  retryJob: (jobId: string, expectedRevision: number) =>
    apiRequest<KnowledgeJob>(`${admin}/knowledge-jobs/${encodeURIComponent(jobId)}/retries`, {
      method: 'POST',
      body: { expectedRevision },
      idempotencyKey: newIdempotencyKey(),
    }),
  /** API-022 POST /knowledge-bases/{kbId}/answers：无幂等承诺，不自动重放。 */
  createAnswer: (kbId: string, command: KnowledgeAnswerCommand) =>
    apiRequest<KnowledgeAnswer>(`${kbPath(kbId)}/answers`, { method: 'POST', body: command }),
}
