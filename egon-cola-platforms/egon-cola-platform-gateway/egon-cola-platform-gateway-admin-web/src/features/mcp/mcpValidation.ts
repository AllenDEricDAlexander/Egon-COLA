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
): string => {
  // Mirror StrictPromptTemplate's one-pass literal syntax; this preview never evaluates expressions.
  if (template.length > 256 * 1024) throw new Error('Prompt 模板过长')
  const identifier = /^[A-Za-z][A-Za-z0-9_]{0,63}$/
  const declared = new Set(argumentsList)
  if (declared.size !== argumentsList.length || argumentsList.some((name) => !identifier.test(name))) {
    throw new Error('Prompt 参数声明无效或重复')
  }
  let result = ''
  let cursor = 0
  while (cursor < template.length) {
    const start = template.indexOf('${', cursor)
    if (start < 0) return result + template.slice(cursor)
    result += template.slice(cursor, start)
    const end = template.indexOf('}', start + 2)
    if (end < 0) throw new Error('Prompt 参数表达式未闭合')
    const name = template.slice(start + 2, end)
    if (!identifier.test(name) || !declared.has(name)) throw new Error(`未声明或无效的 Prompt 参数：${name}`)
    result += `[${name}]`
    cursor = end + 1
  }
  return result
}
