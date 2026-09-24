import {apiRequest} from './client'
import type {Page} from './types'

// 原业务Spec §9.2.4–§9.2.7（API-004–007）typed client：字段与 wire shape 逐项一致，
// ID/key 保持 string，secretRef 只是部署引用名，解析值永不出现在本模块的任何类型里。

const admin = '/api/v1/yuheng/admin'

export const llmDeploymentValues = ['LOCAL', 'CLOUD'] as const
export const llmProtocolValues = [
  'OPENAI_CHAT',
  'OPENAI_EMBEDDING',
  'OPENAI_RESPONSES',
  'ANTHROPIC_MESSAGES',
] as const
export const llmModelKindValues = ['CHAT', 'EMBEDDING'] as const
export const llmCapabilityValues = [
  'TEXT',
  'FUNCTION_TOOLS',
  'STRUCTURED_OUTPUT',
  'VISION',
  'REASONING',
] as const

export type LlmDeployment = (typeof llmDeploymentValues)[number]
export type LlmProtocol = (typeof llmProtocolValues)[number]
export type LlmModelKind = (typeof llmModelKindValues)[number]
export type LlmRouteCapability = (typeof llmCapabilityValues)[number]

export type LlmChannel = {
  key: string
  name: string
  deployment: LlmDeployment | string
  protocol: LlmProtocol | string
  baseUrl: string
  secretRef: string | null
  enabled: boolean
  connectTimeoutMs: number
  headerTimeoutMs: number
  idleTimeoutMs: number
  totalTimeoutMs: number
  maxConcurrent: number
  revision: number
}

export type LlmChannelCommand = Omit<LlmChannel, 'revision'> & {
  expectedRevision: number
}

export type LlmModelRoute = {
  channelKey: string
  upstreamModel: string
  capabilities: LlmRouteCapability[]
  priority: number
  weight: number
}

export type LlmModel = {
  key: string
  name: string
  kind: LlmModelKind | string
  protocols: LlmProtocol[]
  enabled: boolean
  dimensions: number | null
  embeddingSpaceId: string | null
  allowedSubjects: string[]
  routes: LlmModelRoute[]
  revision: number
}

export type LlmModelCommand = Omit<LlmModel, 'revision'> & {
  expectedRevision: number
}

const pageQuery = (page: number, size: number, extra: Record<string, string | undefined> = {}) => {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  Object.entries(extra).forEach(([name, value]) => {
    if (value !== undefined && value !== '') params.set(name, value)
  })
  return `?${params}`
}

export const llmApi = {
  /** API-004 GET /api/v1/yuheng/admin/llm/channels */
  channels: (page = 1, size = 20, signal?: AbortSignal) =>
    apiRequest<Page<LlmChannel>>(`${admin}/llm/channels${pageQuery(page, size)}`, { signal }),
  /** API-005 PUT /api/v1/yuheng/admin/llm/channels/{channelKey}；并发只由 expectedRevision 控制 */
  replaceChannel: (channelKey: string, command: LlmChannelCommand) =>
    apiRequest<LlmChannel>(`${admin}/llm/channels/${encodeURIComponent(channelKey)}`, {
      method: 'PUT',
      body: command,
    }),
  /** API-006 GET /api/v1/yuheng/admin/llm/models */
  models: (page = 1, size = 20, signal?: AbortSignal) =>
    apiRequest<Page<LlmModel>>(`${admin}/llm/models${pageQuery(page, size)}`, { signal }),
  /** API-007 PUT /api/v1/yuheng/admin/llm/models/{modelKey}；PUT 只用 expectedRevision 防并发 */
  replaceModel: (modelKey: string, command: LlmModelCommand) =>
    apiRequest<LlmModel>(`${admin}/llm/models/${encodeURIComponent(modelKey)}`, {
      method: 'PUT',
      body: command,
    }),
}

/** EMBEDDING 只能落在 LOCAL 渠道：出域策略由服务端复核，前端只做提前拒绝，不声称已授权。 */
export const embeddingRoutesRejected = (
  kind: LlmModelKind | string,
  routes: readonly Pick<LlmModelRoute, 'channelKey'>[],
  channels: readonly Pick<LlmChannel, 'key' | 'deployment'>[],
): boolean => {
  if (kind !== 'EMBEDDING') return false
  const deploymentByKey = new Map(channels.map((channel) => [channel.key, channel.deployment]))
  return routes.some((route) => deploymentByKey.get(route.channelKey) !== 'LOCAL')
}
