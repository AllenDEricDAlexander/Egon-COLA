import type { McpProtocolInspection } from '../../api/types'

const sensitiveKey = /^(authorization|proxy-authorization|cookie|set-cookie|password|secret|token)$/i

export const parseJsonObject = (value: string, field: string): Record<string, unknown> => {
  let parsed: unknown
  try {
    parsed = JSON.parse(value)
  } catch {
    throw new Error(`${field} 必须是合法 JSON`)
  }
  if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
    throw new Error(`${field} 必须是 JSON 对象`)
  }
  return parsed as Record<string, unknown>
}

export const formatJson = (value: unknown): string => JSON.stringify(value ?? {}, null, 2)

export const parseStringList = (value?: string): string[] =>
  [...new Set((value ?? '').split(',').map((item) => item.trim()).filter(Boolean))].sort()

const sanitizeValue = (value: unknown): unknown => {
  if (Array.isArray(value)) return value.map(sanitizeValue)
  if (!value || typeof value !== 'object') return value
  return Object.fromEntries(Object.entries(value).map(([key, item]) => [
    key,
    sensitiveKey.test(key) ? '[REDACTED]' : sanitizeValue(item),
  ]))
}

export const sanitizeInspection = (
  inspection: McpProtocolInspection,
): McpProtocolInspection => sanitizeValue(inspection) as McpProtocolInspection
