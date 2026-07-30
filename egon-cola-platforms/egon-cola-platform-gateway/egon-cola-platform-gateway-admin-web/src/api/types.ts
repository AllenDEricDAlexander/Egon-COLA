export type Scope = {
  env: string
  namespace: string
}

export type AdminSession = {
  actorId: string
  displayName: string
  actorType: string
  capabilities: string[]
  roles: string[]
  expiresAt?: string
}

export type GatewayGroup = Scope & {
  id: string
  gatewayGroupCode: string
  displayName: string
  description?: string
  enabled: boolean
  revision: number
  createdAt?: string
  updatedAt?: string
}

export type Application = Scope & {
  id: string
  applicationCode: string
  displayName: string
  description?: string
  revision: number
}

export type Credential = {
  id: string
  accessKey: string
  status: string
  validFrom: string
  validUntil?: string
}

export type IssuedCredential = Credential & {
  secret: string
}

export type EngineNode = {
  appCode: string
  env: string
  namespace: string
  instanceId: string
  leaseId: string
  host?: string
  port?: number
  leaseRole: string
  status: string
  registeredAt: string
  lastHeartbeatAt: string
  expireAt: string
  observedAt: string
  stale: boolean
}

export type RuntimeConsistency = {
  targetReleaseId?: string
  targetReleaseStatus?: string
  readyNodes: number
  totalNodes: number
  consistent: boolean
  stale: boolean
  source: string
  observedAt: string
}

export type ProviderInstance = {
  serviceKey: string
  protocol: 'HTTP' | 'RPC' | string
  serviceName: string
  group: string
  version: string
  instanceId: string
  leaseId: string
  host: string
  port: number
  region?: string
  zone?: string
  weight?: number
  tags?: Record<string, string>
  definitionSetId?: string
  status: string
  expireAt: string
  observedAt: string
  stale: boolean
}

export type OperationNode = {
  id: string
  operationKey: string
  protocol: 'HTTP' | 'RPC' | string
  methodIdentity: string
  externalAccessible: boolean
  lifecycleStatus: string
  sourceType: 'STARTER' | 'MANUAL' | string
  revision: number
}

export type InterfaceGroupNode = {
  id: string
  code: string
  displayName: string
  sourceType: 'STARTER' | 'MANUAL' | string
  className?: string
  operations: OperationNode[]
}

export type EntityNode = {
  id: string
  code: string
  displayName: string
  interfaceGroups: InterfaceGroupNode[]
}

export type BusinessNode = {
  id: string
  code: string
  displayName: string
  entityDomains: EntityNode[]
}

export type CatalogTree = {
  applicationId: string
  businessDomains: BusinessNode[]
}

export type OperationDefinition = {
  id: string
  operationId: string
  definitionVersion: number
  definitionSha256: string
  summary?: string
  tags: string[]
  requestSchema: Record<string, unknown>
  responseSchema: Record<string, unknown>
  errorSchema: Array<Record<string, unknown>>
  descriptorSnapshot?: Record<string, unknown>
  attributes: Record<string, unknown>
  externalAccessible: boolean
  createdAt: string
  createdBy: string
}

export type OperationDetail = {
  operation: OperationNode & {
    applicationId: string
    interfaceGroupId: string
    providerServiceIdentity: Record<string, unknown>
    currentDefinitionId?: string
  }
  definitions: OperationDefinition[]
}

export type GatewayRouteProfile = 'DEFAULT' | 'OPENAI_HTTP'

export type GatewayTransportProtocol = 'HTTP' | 'WEBSOCKET'

export type GatewayRequestBodyMode = 'AGGREGATED' | 'STREAMING'

export type GatewayTransportResponseMode =
  | 'STANDARD'
  | 'AUTO_STREAM'
  | 'SSE'
  | 'BINARY_STREAM'

export type GatewayRouteTransportPolicy = {
  profile?: GatewayRouteProfile
  transportProtocol?: GatewayTransportProtocol
  requestBodyMode?: GatewayRequestBodyMode
  responseMode?: GatewayTransportResponseMode
  maxRequestBodyBytes?: number
  connectTimeoutMs?: number
  responseHeaderTimeoutMs?: number
  streamIdleTimeoutMs?: number
  totalTimeoutMs?: number
  websocketIdleTimeoutMs?: number
  websocketMaxFrameBytes?: number
  bodyLogEnabled?: boolean
  retryEnabled?: boolean
  [key: string]: unknown
}

export type GatewayRouteContent = {
  host?: string
  httpMethod?: string
  pathPattern?: string
  accessZones?: string[]
  priority?: number
  transportPolicy?: GatewayRouteTransportPolicy
  [key: string]: unknown
}

export type DraftRoute = {
  routeId: string
  operationId: string
  routeContent: GatewayRouteContent
  enabled: boolean
}

export type DraftPolicy = {
  policyId: string
  policyType: string
  policyScope: string
  policyContent: Record<string, any>
  enabled: boolean
}

export type GatewayDraft = {
  gatewayGroupId: string
  revision: number
  basedOnReleaseId?: string
  status: string
  changeSummary?: string
  routes: DraftRoute[]
  policies: DraftPolicy[]
  updatedAt: string
}

export type DraftMutationResult = {
  revision: number
  resourceId: string
  replayed: boolean
}

export type ValidationIssue = {
  path: string
  code: string
  message: string
  severity?: 'ERROR' | 'WARNING'
}

export type ValidationReport = {
  valid: boolean
  errors: ValidationIssue[]
  warnings: ValidationIssue[]
}

export type ReleaseTarget = {
  instanceId: string
  leaseId: string
  status: string
  appliedVersion?: number
  appliedArtifactSha256?: string
  errorCode?: string
  observedAt: string
}

export type ReleaseAttempt = {
  attemptNo: number
  status: string
  changeId?: string
  startedAt: string
  completedAt?: string
  errorCode?: string
  errorMessage?: string
  targets: ReleaseTarget[]
}

export type GatewayRelease = {
  id: string
  gatewayGroupId: string
  draftRevision: number
  basedOnReleaseId?: string
  rollbackOfReleaseId?: string
  status: string
  partialApplied: boolean
  changeId?: string
  validationReport: ValidationReport
  structuredDiff: Record<string, unknown>
  changeReason: string
  createdAt: string
  updatedAt: string
  attempts?: ReleaseAttempt[]
}

export type TraceSummary = {
  traceId: string
  startedAt: string
  durationMs: number
  protocol: string
  gatewayGroupId: string
  operationKey: string
  statusCategory: string
  engineInstanceId: string
  providerService: string
}

export type AuditEntry = {
  id: string
  actorId: string
  actorType: string
  source: string
  traceId?: string
  resourceType: string
  resourceId: string
  action: string
  beforeSummary?: Record<string, unknown>
  afterSummary?: Record<string, unknown>
  draftRevision?: number
  releaseId?: string
  successful: boolean
  errorCode?: string
  occurredAt: string
}

export type DashboardSummary = {
  gatewayGroups: number
  readyEngines: number
  totalEngines: number
  inconsistentGroups: number
  activeProviders: number
  abnormalProviders: number
  releaseSuccessRate: number
  requestSeries: Array<{
    time: string
    requests: number
    errors: number
    p50: number
    p95: number
    p99: number
  }>
  protocolCalls: Array<{ protocol: string; value: number }>
}

export type Page<T> = {
  items: T[]
  page: number
  size: number
  total: number
}

export type AdminErrorBody = {
  code?: string
  message?: string
  currentRevision?: number
  traceId?: string
  errors?: ValidationIssue[]
}
