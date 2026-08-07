export type Scope = {
  bizCode: string
  appCode: string
  env: string
  namespace: string
}

export type GatewayScopeBinding = Scope & {
  bindingId: string
  appName: string
  connected: boolean
  gatewayApplicationId?: string
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

export type Application = Omit<Scope, 'appCode'> & {
  id: string
  applicationCode: string
  displayName: string
  description?: string
  ddcMatched: boolean
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

export type McpProtocolDialect =
  | 'STABLE_2025_11_25'
  | 'RC_2026_07_28'
  | 'LEGACY_2024_SSE'

export type McpServer = {
  id: string
  gatewayGroupId: string
  serverCode: string
  displayName: string
  description?: string
  instructions?: string
  dialects: McpProtocolDialect[]
  oauthAudience: string
  listCacheTtlSeconds: number
  enabled: boolean
  revision: number
  createdAt?: string
  updatedAt?: string
}

export type McpServerMutation = Omit<
  McpServer,
  'id' | 'revision' | 'createdAt' | 'updatedAt'
> & {
  expectedRevision: number
  expectedDraftRevision: number
  changeReason: string
}

export type McpCapabilityPlural =
  | 'resources'
  | 'resource-templates'
  | 'prompts'
  | 'task-policies'
  | 'app-bindings'

export type McpCapabilityKind =
  | 'RESOURCE'
  | 'RESOURCE_TEMPLATE'
  | 'PROMPT'
  | 'TASK_POLICY'
  | 'APP_BINDING'

export type McpCapabilityDraft = {
  kind: McpCapabilityKind
  id: string
  gatewayGroupId: string
  serverId: string
  name: string
  content: Record<string, unknown>
  enabled: boolean
  revision: number
}

export type McpCapabilityMutation = {
  gatewayGroupId: string
  serverId: string
  name: string
  content: Record<string, unknown>
  enabled: boolean
  expectedRevision: number
  expectedDraftRevision: number
  changeReason: string
}

export type McpMutationResult = {
  draftRevision: number
  resourceId: string
  resourceRevision: number
  replayed: boolean
}

export type McpValidationReport = {
  valid: boolean
  findings: Array<{
    path: string
    code: string
    message: string
  }>
}

export type McpCapabilityPreview = {
  content: Record<string, unknown>
  validation: McpValidationReport
}

export type McpProtocolInspection = {
  path: string
  headers: Record<string, string>
  body: Record<string, unknown>
  releaseCandidate: boolean
}

export type McpOperationOption = {
  value: string
  label: string
}

export type McpToolRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export type McpManagedTool = {
  toolId: string
  gatewayGroupId: string
  operationId: string
  operationKey: string
  name: string
  description?: string
  operationProtocol: string
  inputSchema: Record<string, unknown>
  outputSchema: Record<string, unknown>
  codeServerId: string
  codeServerCode: string
  serverId: string
  serverCode: string
  codePermissions: string[]
  additionalPermissions: string[]
  effectivePermissions: string[]
  codeRiskLevel: McpToolRiskLevel
  minimumRiskLevel?: McpToolRiskLevel
  effectiveRiskLevel: McpToolRiskLevel
  idempotent: boolean
  enabled: boolean
  overrideRevision: number
}

export type McpManagedToolOverrideMutation = {
  gatewayGroupId: string
  serverId?: string
  additionalPermissions: string[]
  minimumRiskLevel?: McpToolRiskLevel
  enabled?: false
  expectedRevision: number
  expectedDraftRevision: number
  changeReason: string
}

export type McpRemoteTool = {
  id: string
  gatewayGroupId: string
  serverId: string
  serverCode: string
  name: string
  description?: string
  remoteMountId: string
  inputSchema: Record<string, unknown>
  outputSchema: Record<string, unknown>
  annotations: Record<string, unknown>
  requiredPermissions: string[]
  riskLevel: McpToolRiskLevel
  idempotent: boolean
  enabled: boolean
  revision: number
}

export type McpRemoteToolMutation = Omit<McpRemoteTool, 'id' | 'revision' | 'serverCode'> & {
  expectedRevision: number
  expectedDraftRevision: number
  changeReason: string
}

export type McpToolReference = {
  name: string
  riskLevel: McpToolRiskLevel
  enabled: boolean
}

export type McpRemoteProvider = {
  id: string
  gatewayGroupId: string
  providerCode: string
  content: Record<string, unknown>
  enabled: boolean
  revision: number
}

export type McpRemoteProviderMutation = Omit<
  McpRemoteProvider,
  'id' | 'revision'
> & {
  expectedRevision: number
  expectedDraftRevision: number
  changeReason: string
}

export type McpRemoteCapability = {
  id: string
  providerId: string
  primitiveType: string
  remoteName: string
  descriptor: Record<string, unknown>
  capabilityFingerprint: string
  syncedAt: string
}

export type McpRemoteMount = {
  id: string
  gatewayGroupId: string
  serverId: string
  providerId: string
  namespace: string
  capabilityFingerprint: string
  content: Record<string, unknown>
  enabled: boolean
  revision: number
}

export type McpRemoteMountMutation = Omit<
  McpRemoteMount,
  'id' | 'revision'
> & {
  expectedRevision: number
  expectedDraftRevision: number
  changeReason: string
}

export type McpAppArtifact = {
  id: string
  gatewayGroupId: string
  appCode: string
  version: string
  displayName: string
  resourceUri: string
  artifactReference: string
  sha256: string
  sizeBytes: number
  mimeType: string
  contentSecurityPolicy: string
  permissions: string[]
  allowedOrigins: string[]
  createdBy: string
  createdAt: string
}

export type McpTask = {
  id: string
  principalFingerprint: string
  subjectId: string
  tenantId: string
  clientId: string
  serverCode: string
  toolName: string
  requestDigest: string
  state: string
  inputPayload?: Record<string, unknown>
  resultPayload?: Record<string, unknown>
  errorPayload?: Record<string, unknown>
  workerOwner?: string
  leaseUntil?: string
  executionDeadline: string
  expiresAt: string
  attemptCount: number
  maxAttempts: number
  revision: number
  createdAt: string
  updatedAt: string
}

export type McpApproval = {
  approvalId: string
  approvalToken: string
  expiresAt: string
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
