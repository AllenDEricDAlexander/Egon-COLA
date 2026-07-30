export const validatePublicRoute = (
  accessZone: string,
  operationExternalAccessible: boolean,
): string | undefined =>
  accessZone === 'PUBLIC' && !operationExternalAccessible
    ? 'PUBLIC Route 不能引用 externalAccessible=false 的内部接口'
    : undefined

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
