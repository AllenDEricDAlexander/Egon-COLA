export type LogicalTrace = {
  traceId: string
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
  return traceId ? { traceId } : undefined
}

export const traceHeaders = (
  logicalTrace: LogicalTrace | undefined,
): Record<string, string> => {
  if (!logicalTrace) {
    return {}
  }
  const spanId = randomHex(8)
  if (!spanId) {
    return { 'X-Trace-Id': logicalTrace.traceId }
  }
  return {
    'X-Trace-Id': logicalTrace.traceId,
    traceparent: `00-${logicalTrace.traceId}-${spanId}-01`,
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
