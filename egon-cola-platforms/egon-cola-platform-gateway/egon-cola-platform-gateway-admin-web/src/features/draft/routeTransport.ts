import type {
  GatewayRouteContent,
  GatewayRouteProfile,
  GatewayRouteTransportPolicy,
} from '../../api/types'

export type RouteFormValues = {
  host?: string
  httpMethod?: string
  pathPattern?: string
  accessZones?: string[]
  priority?: number
  operationProtocol?: string
  transportPolicy?: GatewayRouteTransportPolicy
  advancedContent: string
  legacyHostMissing?: boolean
}

export type RouteTransportValidationIssue = {
  path: string
  code: string
  message: string
}

export type TransportPolicyField =
  | 'transportProtocol'
  | 'requestBodyMode'
  | 'responseMode'
  | 'maxRequestBodyBytes'
  | 'connectTimeoutMs'
  | 'responseHeaderTimeoutMs'
  | 'streamIdleTimeoutMs'
  | 'totalTimeoutMs'
  | 'websocketIdleTimeoutMs'
  | 'websocketMaxFrameBytes'
  | 'bodyLogEnabled'
  | 'retryEnabled'

const ROUTE_KEYS = new Set([
  'host',
  'httpMethod',
  'pathPattern',
  'accessZones',
  'priority',
  'transportPolicy',
  'listener',
  'method',
  'path',
  'protocol',
  'fullMethodName',
  'providerServiceName',
  'operationExternalAccessible',
])

const PROFILE_DEFAULTS: Record<GatewayRouteProfile, GatewayRouteTransportPolicy> = {
  DEFAULT: {
    transportProtocol: 'HTTP',
    requestBodyMode: 'AGGREGATED',
    responseMode: 'STANDARD',
    maxRequestBodyBytes: 2_097_152,
    connectTimeoutMs: 30_000,
    responseHeaderTimeoutMs: 5_000,
    streamIdleTimeoutMs: 5_000,
    bodyLogEnabled: false,
  },
  OPENAI_HTTP: {
    transportProtocol: 'HTTP',
    requestBodyMode: 'STREAMING',
    responseMode: 'AUTO_STREAM',
    maxRequestBodyBytes: 536_870_912,
    connectTimeoutMs: 10_000,
    responseHeaderTimeoutMs: 120_000,
    streamIdleTimeoutMs: 90_000,
    totalTimeoutMs: 1_800_000,
    websocketIdleTimeoutMs: 300_000,
    websocketMaxFrameBytes: 16_777_216,
    bodyLogEnabled: false,
    retryEnabled: false,
  },
}

const NUMERIC_RANGES = {
  maxRequestBodyBytes: [1, 1_073_741_824],
  connectTimeoutMs: [100, 60_000],
  responseHeaderTimeoutMs: [1_000, 600_000],
  streamIdleTimeoutMs: [1_000, 1_800_000],
  totalTimeoutMs: [1_000, 7_200_000],
  websocketIdleTimeoutMs: [1_000, 7_200_000],
  websocketMaxFrameBytes: [1_024, 67_108_864],
} as const

const isRecord = (value: unknown): value is Record<string, unknown> =>
  Boolean(value) && !Array.isArray(value) && typeof value === 'object'

const optionalText = (value: unknown): string | undefined =>
  typeof value === 'string' && value.trim() ? value.trim() : undefined

const optionalNumber = (value: unknown): number | undefined =>
  typeof value === 'number' && Number.isFinite(value) ? value : undefined

const accessZones = (content: Record<string, unknown>): string[] | undefined => {
  if (Array.isArray(content.accessZones)) {
    const zones = content.accessZones
      .filter((value): value is string => typeof value === 'string' && Boolean(value.trim()))
      .map((value) => value.trim().toUpperCase())
    return zones.length ? [...new Set(zones)] : undefined
  }
  const listener = optionalText(content.listener)
  return listener ? [listener.toUpperCase()] : undefined
}

const parseAdvancedContent = (value: string): Record<string, unknown> => {
  const parsed = JSON.parse(value || '{}') as unknown
  if (!isRecord(parsed)) {
    throw new Error('高级扩展内容必须是 JSON Object')
  }
  return parsed
}

const removeUndefined = (value: unknown): unknown => {
  if (Array.isArray(value)) {
    return value
      .map(removeUndefined)
      .filter((item) => item !== undefined)
  }
  if (isRecord(value)) {
    return Object.fromEntries(
      Object.entries(value)
        .map(([key, item]) => [key, removeUndefined(item)] as const)
        .filter(([, item]) => item !== undefined),
    )
  }
  return value
}

const normalizedPolicy = (
  policy: GatewayRouteTransportPolicy | undefined,
): GatewayRouteTransportPolicy | undefined => {
  if (!policy) return undefined
  const normalized = removeUndefined({ ...policy }) as GatewayRouteTransportPolicy
  if (normalized.transportProtocol === 'WEBSOCKET') {
    delete normalized.requestBodyMode
    delete normalized.responseMode
  }
  return Object.keys(normalized).length ? normalized : undefined
}

export const readRouteForm = (content: Record<string, unknown>): RouteFormValues => {
  const advanced = Object.fromEntries(
    Object.entries(content).filter(([key]) => !ROUTE_KEYS.has(key)),
  )
  const host = optionalText(content.host)
  const hasLegacyKeys = ['listener', 'method', 'path'].some((key) => key in content)
  return {
    host,
    httpMethod: optionalText(content.httpMethod) ?? optionalText(content.method),
    pathPattern: optionalText(content.pathPattern) ?? optionalText(content.path),
    accessZones: accessZones(content),
    priority: optionalNumber(content.priority) ?? 0,
    transportPolicy: isRecord(content.transportPolicy)
      ? { ...content.transportPolicy }
      : undefined,
    advancedContent: JSON.stringify(advanced, null, 2),
    legacyHostMissing: hasLegacyKeys && !host,
  }
}

export const writeCanonicalRoute = (values: RouteFormValues): GatewayRouteContent => {
  const advanced = parseAdvancedContent(values.advancedContent)
  ROUTE_KEYS.forEach((key) => delete advanced[key])
  return removeUndefined({
    ...advanced,
    host: optionalText(values.host),
    httpMethod: optionalText(values.httpMethod)?.toUpperCase(),
    pathPattern: optionalText(values.pathPattern),
    accessZones: values.accessZones
      ?.filter((value) => Boolean(value?.trim()))
      .map((value) => value.trim().toUpperCase()),
    priority: Number(values.priority ?? 0),
    transportPolicy: normalizedPolicy(values.transportPolicy),
  }) as GatewayRouteContent
}

export const transportFieldPresentation = (
  policy: GatewayRouteTransportPolicy | undefined,
  field: TransportPolicyField,
): { value: unknown; source: 'PROFILE_DEFAULT' | 'ROUTE_OVERRIDE' } => {
  if (policy?.[field] !== undefined) {
    return { value: policy[field], source: 'ROUTE_OVERRIDE' }
  }
  const profile = policy?.profile === 'OPENAI_HTTP' ? 'OPENAI_HTTP' : 'DEFAULT'
  return { value: PROFILE_DEFAULTS[profile][field], source: 'PROFILE_DEFAULT' }
}

const issue = (
  path: string,
  code: string,
  message: string,
): RouteTransportValidationIssue => ({ path, code, message })

export const validateTransportRoute = (
  values: RouteFormValues,
): RouteTransportValidationIssue[] => {
  const issues: RouteTransportValidationIssue[] = []
  if (!optionalText(values.host)) {
    issues.push(issue(
      'host',
      'ROUTE_HOST_REQUIRED',
      values.legacyHostMissing ? '历史草稿缺少 Host，请补录' : 'Host 不能为空',
    ))
  }
  if (!optionalText(values.httpMethod)) {
    issues.push(issue('httpMethod', 'ROUTE_METHOD_REQUIRED', 'HTTP Method 不能为空'))
  }
  if (!optionalText(values.pathPattern)?.startsWith('/')) {
    issues.push(issue('pathPattern', 'ROUTE_PATH_INVALID', 'Path Pattern 必须以 / 开头'))
  }
  if (!values.accessZones?.length) {
    issues.push(issue('accessZones', 'ROUTE_ACCESS_ZONE_REQUIRED', '至少选择一个 Access Zone'))
  }

  const policy = values.transportPolicy
  const enumFields = [
    ['profile', ['DEFAULT', 'OPENAI_HTTP']],
    ['transportProtocol', ['HTTP', 'WEBSOCKET']],
    ['requestBodyMode', ['AGGREGATED', 'STREAMING']],
    ['responseMode', ['STANDARD', 'AUTO_STREAM', 'SSE', 'BINARY_STREAM']],
  ] as const
  enumFields.forEach(([field, allowed]) => {
    const value = policy?.[field]
    if (value !== undefined && !allowed.some((candidate) => candidate === value)) {
      issues.push(issue(
        `transportPolicy.${field}`,
        'TRANSPORT_ENUM_UNKNOWN',
        `${field} 包含未知值`,
      ))
    }
  })
  Object.entries(NUMERIC_RANGES).forEach(([field, [minimum, maximum]]) => {
    const value = policy?.[field]
    if (value === undefined) return
    if (
      typeof value !== 'number'
      || !Number.isInteger(value)
      || value < minimum
      || value > maximum
    ) {
      issues.push(issue(
        `transportPolicy.${field}`,
        'TRANSPORT_VALUE_OUT_OF_RANGE',
        `${field} 必须是 ${minimum} 到 ${maximum} 之间的整数`,
      ))
    }
  })

  if (policy?.transportProtocol === 'WEBSOCKET' && values.httpMethod !== 'GET') {
    issues.push(issue(
      'httpMethod',
      'WEBSOCKET_GET_REQUIRED',
      'WebSocket Route 必须使用 GET Method',
    ))
  }
  if (values.operationProtocol === 'RPC') {
    if (policy?.profile === 'OPENAI_HTTP') {
      issues.push(issue(
        'transportPolicy.profile',
        'RPC_TRANSPORT_UNSUPPORTED',
        'RPC Operation 不能使用 OPENAI_HTTP Profile',
      ))
    }
    if (policy?.transportProtocol === 'WEBSOCKET') {
      issues.push(issue(
        'transportPolicy.transportProtocol',
        'RPC_TRANSPORT_UNSUPPORTED',
        'RPC Operation 不能选择 WebSocket Transport',
      ))
    }
    if (policy?.requestBodyMode === 'STREAMING') {
      issues.push(issue(
        'transportPolicy.requestBodyMode',
        'RPC_TRANSPORT_UNSUPPORTED',
        'RPC Operation 不能选择 Streaming Request Body',
      ))
    }
    if (policy?.responseMode && policy.responseMode !== 'STANDARD') {
      issues.push(issue(
        'transportPolicy.responseMode',
        'RPC_TRANSPORT_UNSUPPORTED',
        'RPC Operation 不能选择 Streaming Response Mode',
      ))
    }
  }
  return issues
}
