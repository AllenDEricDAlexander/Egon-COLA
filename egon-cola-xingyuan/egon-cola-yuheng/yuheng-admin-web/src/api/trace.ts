export type LogicalTrace = {
  traceId: string
  requestId: string
}

const randomHex = (bytes: number): string | undefined => {
  if (!globalThis.crypto?.getRandomValues) {
    return undefined
  }
  const values = new Uint8Array(bytes)
  globalThis.crypto.getRandomValues(values)
  return Array.from(values, (value) => value.toString(16).padStart(2, '0')).join('')
}

export const createLogicalTrace = (): LogicalTrace | undefined => {
  const traceId = randomHex(16)
  const requestId = randomHex(16)
  return traceId && requestId ? { traceId, requestId } : undefined
}

export const traceHeaders = (
  logicalTrace: LogicalTrace | undefined,
): Record<string, string> => {
  if (!logicalTrace) {
    return {}
  }
  const spanId = randomHex(8)
  if (!spanId) {
    return { 'x-egon-request-id': logicalTrace.requestId }
  }
  return {
    traceparent: `00-${logicalTrace.traceId}-${spanId}-01`,
    'x-egon-request-id': logicalTrace.requestId,
  }
}

export const newIdempotencyKey = (): string => {
  if (globalThis.crypto?.randomUUID) {
    return globalThis.crypto.randomUUID()
  }
  const generated = randomHex(16)
  if (!generated) {
    throw new Error('安全随机数不可用，无法执行写操作')
  }
  return generated
}
