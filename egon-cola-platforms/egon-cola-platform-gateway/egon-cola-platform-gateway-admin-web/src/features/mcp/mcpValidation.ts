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

export const validateResourceUri = (value: string): string => {
  const uri = value.trim()
  if (!/^[a-z][a-z0-9+.-]*:\/\//i.test(uri) || uri.includes('..')) {
    throw new Error('Resource URI 必须是绝对且不包含路径穿越的 URI')
  }
  return uri
}

export const validateResourceTemplate = (value: string): string => {
  const template = value.trim()
  if (template.length > 2048 || template.includes('..')) {
    throw new Error('URI Template 过长或包含路径穿越')
  }
  const sample = template.replace(/\{[A-Za-z][A-Za-z0-9_]*\}/g, 'x')
  if (sample.includes('{') || sample.includes('}')) {
    throw new Error('URI Template 变量名无效')
  }
  validateResourceUri(sample)
  return template
}

export const validateJsonSchema = (value: string, field: string): Record<string, unknown> => {
  const schema = parseJsonObject(value, field)
  const visit = (node: unknown) => {
    if (Array.isArray(node)) return node.forEach(visit)
    if (!node || typeof node !== 'object') return
    Object.entries(node).forEach(([key, item]) => {
      if (key === '$ref' && typeof item === 'string' && !item.startsWith('#')) {
        throw new Error(`${field} 禁止外部 $ref`)
      }
      visit(item)
    })
  }
  visit(schema)
  return schema
}

export const renderPromptTemplate = (
  template: string,
  argumentsList: string[],
): string => template.replace(/\{\{\s*([A-Za-z][A-Za-z0-9_]*)\s*\}\}/g, (_, name: string) => {
  if (!argumentsList.includes(name)) throw new Error(`未声明 Prompt 参数：${name}`)
  return `[${name}]`
})
