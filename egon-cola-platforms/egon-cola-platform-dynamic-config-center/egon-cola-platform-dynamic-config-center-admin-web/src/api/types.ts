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
  serviceKind: string
  protocol: string
  serviceName: string
  group?: string
  version?: string
  metadata?: Record<string, unknown>
}

export type RegistryInstance = {
  instanceId: string
  host: string
  port: number
  secure: boolean
  status: string
  lastHeartbeatAt?: string
  expireAt?: string
  metadata?: { buildId?: string }
}

export type DdcConfig = {
  id: string
  appCode: string
  env: string
  namespace: string
  configKey: string
  configValue: string
  defaultValue?: string
  valueType: string
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

export type DdcApp = {
  id: string
  appCode: string
  appName: string
  owner?: string
  description?: string
  enabled: boolean
  createdAt?: string
  updatedAt?: string
}

export type DdcNamespace = {
  id: string
  appCode: string
  env: string
  namespace: string
  description?: string
  enabled: boolean
  createdAt?: string
  updatedAt?: string
}
