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
