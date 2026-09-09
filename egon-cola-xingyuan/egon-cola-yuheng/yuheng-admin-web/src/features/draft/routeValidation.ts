import type { GatewayRouteTransportPolicy } from '../../api/types'

export const validatePublicRoute = (
  accessZone: string | string[] | undefined,
  operationExternalAccessible: boolean,
): string | undefined => {
  const zones = Array.isArray(accessZone) ? accessZone : [accessZone]
  return zones.includes('PUBLIC') && !operationExternalAccessible
    ? 'PUBLIC Route 不能引用 externalAccessible=false 的内部接口'
    : undefined
}

export type TransportFormState = {
  transportEditable: boolean
  bodyModesVisible: boolean
  transparentResponseNotice?: string
  retryNotice?: string
}

export const transportFormState = (
  operationProtocol: string | undefined,
  policy: GatewayRouteTransportPolicy | undefined,
): TransportFormState => ({
  transportEditable: operationProtocol === 'HTTP',
  bodyModesVisible:
    operationProtocol === 'HTTP' && policy?.transportProtocol !== 'WEBSOCKET',
  transparentResponseNotice:
    policy?.responseMode === 'SSE' || policy?.responseMode === 'BINARY_STREAM'
      ? '透明响应，禁止聚合、缓存或内容转换。'
      : undefined,
  retryNotice: policy?.retryEnabled === true
    ? '允许重试仍受 Operation 幂等、请求体可重放和响应提交点限制。'
    : undefined,
})

export const policyWarnings = (
  policyType: string,
  content: Record<string, unknown>,
): string[] => {
  const warnings: string[] = []
  if (policyType === 'RETRY' && content.idempotent === false) {
    warnings.push('非幂等 Operation 开启自动重试可能产生重复副作用')
  }
  if (policyType === 'RATE_LIMIT' && content.distributed === true && content.failureMode === 'OPEN') {
    warnings.push('分布式限流 Fail Open 会在 Redis 故障时放行请求')
  }
  if (
    typeof content.retryBudgetMs === 'number'
    && typeof content.deadlineMs === 'number'
    && content.retryBudgetMs > content.deadlineMs
  ) {
    warnings.push('Retry 总预算不能超过 Deadline')
  }
  return warnings
}
