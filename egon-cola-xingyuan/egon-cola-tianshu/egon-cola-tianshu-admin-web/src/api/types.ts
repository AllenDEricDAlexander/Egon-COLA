export type ResultEnvelope = {
  success: boolean
  code: number
  status: string
  message: string
  traceId?: string
  timestamp: number
}

export type AuthorizationBootstrap = {
    user: {
        id: string
        tenantId: string
        identitySub: string
        status: string
    }
    activeRoleContexts: readonly unknown[]
    permissions: readonly string[]
    apps: readonly unknown[]
    menus: readonly unknown[]
    routes: readonly unknown[]
    actions: readonly unknown[]
    fieldPolicies: Readonly<Record<string, unknown>>
    defaultApplicationCode: string | null
    defaultRoute: string | null
    authVersion: number
    policyVersion: number
}

export type ResultRecord<T> = ResultEnvelope & { data: T }

export type PageMetaRecord = {
  total: number
  pageNo: number
  pageSize: number
  pages: number
  hasNext: boolean
  hasPrevious: boolean
}

export type PageResultRecord<T> = ResultEnvelope & {
  records: T[]
  page: PageMetaRecord
}

export type RegistryService = {
  bizCode: string
  env: string
  appCode: string
  serviceId: string
  serviceKind: string
  protocol: string
  serviceName: string
  group?: string
  version?: string
  metadata?: Record<string, unknown>
}

export type RegistryInstance = {
  instanceId: string
  leaseId?: string
  host: string
  port: number
  secure: boolean
  status: string
  registeredAt?: string
  lastHeartbeatAt?: string
  expireAt?: string
  metadata?: { buildId?: string }
}

export type DdcConfig = {
  id: string
  bizCode: string
  appCode: string
  env: string
  visibleNamespaces: string[]
  resourceName: 'application.yml' | 'application.yaml'
  content: string
  format: 'YAML'
  currentVersion: number
  description?: string
  createdAt?: string
  updatedAt?: string
}

export type DdcPublishResult = {
  changeId: string
  status: string
  targetCount?: number
  ackCount?: number
  failedCount?: number
  ignoredCount?: number
  timeoutCount?: number
  attemptCount?: number
  targetVersion?: number
  resourceChecksum?: string
  errorMessage?: string
}

export type DdcConfigVersion = {
  id: string
  configId: string
  version: number
  resourceName: 'application.yml' | 'application.yaml'
  oldContent?: string
  newContent?: string
  format: 'YAML'
  changeType?: string
  changeReason?: string
  operator?: string
  createdAt?: string
}

export type DdcBiz = {
  id: string
  bizCode: string
  bizName: string
  description?: string
  enabled: boolean
  createdAt?: string
  updatedAt?: string
}

export type DdcEnv = {
  id: string
  envCode: string
  description?: string
  sortOrder: number
  enabled: boolean
  createdAt?: string
  updatedAt?: string
}

export type DdcApp = {
  id: string
  appCode: string
  bizCode: string
  appName: string
  owner?: string
  description?: string
  enabled: boolean
  createdAt?: string
  updatedAt?: string
}

export type DdcNamespace = {
  id: string
  bizCode: string
  namespaceCode: string
  namespace: string
  description?: string
  enabled: boolean
  createdAt?: string
  updatedAt?: string
}

export type DdcPublishTask = {
  id: string
  changeId: string
  configId?: string
  bizCode?: string
  appCode?: string
  env?: string
  resourceName?: string
  targetVersion?: number
  publishMode?: string
  resourceChecksum?: string
  attemptCount?: number
  dispatchedAt?: string
  completedAt?: string
  failureStage?: string
  status: string
  targetCount?: number
  ackCount?: number
  failedCount?: number
  ignoredCount?: number
  timeoutCount?: number
  timeoutMs?: number
  operator?: string
  errorMessage?: string
  createdAt?: string
  updatedAt?: string
}

export type DdcInstance = {
  id: string
  instanceId: string
  bizCode: string
  appCode: string
  env: string
  host: string
  port: number
  pid?: string
  sdkVersion?: string
  leaseId?: string
  leaseExpireAt?: string
  status: string
  lastHeartbeatAt?: string
  createdAt?: string
  updatedAt?: string
  runtimeMetadata?: Record<string, string>
}

export type DdcInstancePage = PageResultRecord<DdcInstance>

export type DdcCapabilityState =
  | 'READY'
  | 'NOT_CONFIGURED'
  | 'FORBIDDEN'
  | 'UNAVAILABLE'

export type DdcCapabilityView = {
  state: DdcCapabilityState
  message: string
  traceId?: string
}

export type DdcCacheCheckRow = {
  resourceName: string
  databaseValue?: string
  redisValue?: string
  databaseVersion?: number
  redisVersion?: number
  matched: boolean
}

export type DdcNamespaceEnvAppBinding = {
  id: string
  bizCode: string
  namespaceId: string
  namespaceCode: string
  env: string
  appId: string
  appCode: string
  appName: string
  enabled: boolean
}

export type DdcNamespaceEnvAppBindingRequest = {
  bizCode: string
  namespaceCode: string
  env: string
  appCode: string
  enabled: boolean
}

export type DdcAiKind = 'PROMPT' | 'SKILL'

export type DdcAiNodeType = 'DIRECTORY' | 'FILE'

export type DdcAiScope = {
  bizCode: string
  namespaceCode: string
  env: string
  appCode: string
}

export type DdcAiNode = {
  path: string
  nodeType: DdcAiNodeType
  content: string | null
}

export type DdcAiContent = {
  promptText: string | null
  nodes: DdcAiNode[]
}

export type DdcAiResourceSummary = {
  id: string
  scope: DdcAiScope
  kind: DdcAiKind
  resourceKey: string
  displayName: string
  description: string
  revision: string
  lastVersionNo: string
  currentVersionId: string | null
  currentVersionNo: string | null
  updatedAt: string
}

export type DdcAiResourceResult = {
  resource: DdcAiResourceSummary
  draft: DdcAiContent
}

export type DdcAiVersionSummary = {
  id: string
  resourceId: string
  versionNo: string
  kind: DdcAiKind
  displayName: string
  description: string
  contentSha256: string
  contentBytes: number
  nodeCount: number
  sourceVersionId: string | null
  changeNote: string
  createdAt: string
  createdBy: string
}

export type DdcAiVersionResult = {
  version: DdcAiVersionSummary
  content: DdcAiContent
}

export type DdcAiPublicationResult = {
  id: string
  resourceId: string
  operation: 'PUBLISH' | 'ROLLBACK' | 'PROMOTE'
  previousVersionId: string | null
  targetVersionId: string
  targetVersionNo: string
  sourceVersionId: string | null
  resourceRevision: string
  targetCreated: boolean
  createdAt: string
}

export type DdcAiCreateCommand = {
  scope: DdcAiScope
  kind: DdcAiKind
  resourceKey: string
  displayName: string
  description: string
  draft: DdcAiContent
}

export type DdcAiDraftCommand = {
  expectedRevision: string
  displayName: string
  description: string
  draft: DdcAiContent
}

export type DdcAiPublishCommand = {
  expectedRevision: string
  changeNote: string
}

export type DdcAiRollbackCommand = {
  expectedRevision: string
  versionNo: string
  changeNote: string
}

export type DdcAiPromotionCommand = {
  sourceVersionNo: string
  targetScope: DdcAiScope
  targetResourceKey: string
  expectedTargetRevision: string | null
  changeNote: string
}

export type DdcAiResourcePageParams = DdcAiScope & {
  kind: DdcAiKind
  resourceKeyPrefix?: string
  pageNo?: number
  pageSize?: number
}

export type DdcAiVersionPageParams = {
  pageNo?: number
  pageSize?: number
}

export type DdcAiPublicationPageParams = DdcAiVersionPageParams & {
  requestId?: string
}
