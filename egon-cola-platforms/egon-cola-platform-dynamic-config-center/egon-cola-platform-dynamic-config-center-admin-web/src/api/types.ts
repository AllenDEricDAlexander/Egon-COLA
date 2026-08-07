export type ResultRecord<T> = {
  success: boolean
  code: number
  status: string
  message: string
  data: T
  traceId: string
  timestamp: number
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
  configKey: 'application.yml'
  configValue: string
  valueType: 'YAML'
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
  contentChecksum?: string
  errorMessage?: string
}

export type DdcConfigVersion = {
  id: string
  configId: string
  version: number
  oldValue?: string
  newValue?: string
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
  configKey?: string
  targetVersion?: number
  publishMode?: string
  contentChecksum?: string
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

export type DdcCacheCheckRow = {
  configKey: string
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
