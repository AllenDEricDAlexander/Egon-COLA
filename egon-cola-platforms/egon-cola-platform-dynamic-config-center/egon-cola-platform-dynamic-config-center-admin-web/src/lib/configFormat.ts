const extensionFormats = new Map([
  ['yaml', 'YAML'],
  ['yml', 'YAML'],
  ['toml', 'TOML'],
  ['json', 'JSON'],
  ['txt', 'TXT'],
  ['text', 'TXT'],
  ['conf', 'TXT'],
  ['properties', 'TXT'],
])

const parseJson = (value: unknown): unknown | null => {
  if (typeof value !== 'string' || value.trim() === '') return null
  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}

const jsonValue = (value: string): unknown => {
  try {
    return JSON.parse(value)
  } catch (error) {
    throw new Error('JSON 配置格式无效，请检查括号、逗号和引号', { cause: error })
  }
}

const isObject = (value: unknown): value is Record<string, unknown> =>
  value !== null && typeof value === 'object' && !Array.isArray(value)

export type GatewayInlineRule = {
  activation: {
    activationSchemaVersion: string
    releaseId: string
    mode: string
    ruleSchemaVersion: string
    ruleContentSha256: string
    artifactSha256: string
    inlineSnapshot: string
    totalSize: number
    chunks: unknown[]
  }
  snapshot: {
    ruleSchemaVersion: string
    releaseId: string
    generatedAt: string
    ruleContentSha256: string
    artifactSha256: string
    content: Record<string, unknown>
  }
}

const gatewayInlineRule = (value: string): GatewayInlineRule | null => {
  const activation = parseJson(value)
  if (!isObject(activation)
      || activation.mode !== 'INLINE'
      || typeof activation.activationSchemaVersion !== 'string'
      || typeof activation.releaseId !== 'string'
      || typeof activation.ruleSchemaVersion !== 'string'
      || typeof activation.ruleContentSha256 !== 'string'
      || typeof activation.artifactSha256 !== 'string'
      || typeof activation.totalSize !== 'number'
      || typeof activation.inlineSnapshot !== 'string'
      || !Array.isArray(activation.chunks)
      || activation.chunks.length !== 0) {
    return null
  }
  const snapshot = parseJson(activation.inlineSnapshot)
  if (!isObject(snapshot)
      || !Object.hasOwn(snapshot, 'content')
      || !isObject(snapshot.content)
      || typeof snapshot.releaseId !== 'string'
      || typeof snapshot.generatedAt !== 'string'
      || typeof snapshot.ruleSchemaVersion !== 'string'
      || typeof snapshot.ruleContentSha256 !== 'string'
      || typeof snapshot.artifactSha256 !== 'string'
      || snapshot.releaseId !== activation.releaseId
      || snapshot.ruleSchemaVersion !== activation.ruleSchemaVersion
      || snapshot.ruleContentSha256 !== activation.ruleContentSha256
      || snapshot.artifactSha256 !== activation.artifactSha256) {
    return null
  }
  return { activation, snapshot } as unknown as GatewayInlineRule
}

const extensionFormat = (key: string | undefined): string | undefined => {
  const match = String(key ?? '').trim().toLowerCase().match(/\.([a-z0-9]+)$/)
  return match ? extensionFormats.get(match[1]) : undefined
}

const looksLikeToml = (value: string): boolean => /^(?:\s*\[[^[\]\r\n]+]\s*|\s*[A-Za-z0-9_.-]+\s*=.+)$/m.test(value)

const looksLikeYaml = (value: string): boolean => /^(?:\s*---\s*|\s*[A-Za-z0-9_.-]+\s*:\s*.*)$/m.test(value)

export type ConfigFormat = 'JSON' | 'YAML' | 'TOML' | 'TXT'

export const detectConfigFormat = (config: { configKey?: string; configValue?: string; valueType?: string } = {}): ConfigFormat => {
  const byExtension = extensionFormat(config.configKey)
  if (byExtension) return byExtension as ConfigFormat
  const value = String(config.configValue ?? '')
  if (gatewayInlineRule(value) || String(config.valueType ?? '').toUpperCase() === 'JSON') {
    return 'JSON'
  }
  const parsed = parseJson(value)
  if (isObject(parsed) || Array.isArray(parsed)) return 'JSON'
  if (looksLikeToml(value)) return 'TOML'
  if (looksLikeYaml(value)) return 'YAML'
  return 'TXT'
}

const noticeFor = (format: ConfigFormat): string => {
  if (format === 'JSON') return 'JSON 已自动缩进；保存前会校验格式，业务字段会完整保留。'
  if (format === 'YAML') return 'YAML 按原始文本编辑和保存。'
  if (format === 'TOML') return 'TOML 按原始文本编辑和保存。'
  return 'TXT 按原始文本编辑和保存。'
}

export type ConfigEditor = {
  format: ConfigFormat
  content: string
  adapter: 'PLAIN' | 'GATEWAY_INLINE_RULE'
  originalValue: string
  gateway: GatewayInlineRule | null
  notice: string
}

export const prepareConfigEditor = (config: { configKey?: string; configValue?: string; valueType?: string } = {}): ConfigEditor => {
  const originalValue = String(config.configValue ?? '')
  const gateway = gatewayInlineRule(originalValue)
  if (gateway) {
    return {
      format: 'JSON',
      content: JSON.stringify(gateway.snapshot.content, null, 2),
      adapter: 'GATEWAY_INLINE_RULE',
      originalValue,
      gateway,
      notice: '已隐藏 Gateway 发布版本、校验和与时间等系统元数据；保存时自动重建。',
    }
  }
  const format = detectConfigFormat(config)
  const parsed = format === 'JSON' ? parseJson(originalValue) : null
  return {
    format,
    content: parsed === null ? originalValue : JSON.stringify(parsed, null, 2),
    adapter: 'PLAIN',
    originalValue,
    gateway: null,
    notice: noticeFor(format),
  }
}

const canonicalValue = (value: unknown): unknown => {
  if (Array.isArray(value)) return value.map(canonicalValue)
  if (!isObject(value)) return value
  return Object.fromEntries(Object.keys(value)
    .filter((key) => value[key] !== null && value[key] !== undefined)
    .filter((key) => !(key === 'parameters'
      && Array.isArray(value[key])
      && value[key].length === 0
      && (Object.hasOwn(value, 'operationId') || Object.hasOwn(value, 'operationKey'))))
    .sort()
    .map((key) => [key, canonicalValue(value[key])]))
}

const canonicalJson = (value: unknown): string => JSON.stringify(canonicalValue(value))

const sha256 = async (value: string): Promise<string> => {
  if (!globalThis.crypto?.subtle) {
    throw new Error('当前环境不支持 SHA-256，无法安全保存 Gateway 规则')
  }
  const digest = await globalThis.crypto.subtle.digest('SHA-256', new TextEncoder().encode(value))
  return [...new Uint8Array(digest)].map((byte) => byte.toString(16).padStart(2, '0')).join('')
}

export const serializeConfigEditor = async (editor: ConfigEditor, content: string): Promise<string> => {
  if (editor.format !== 'JSON') return content
  const parsedContent = jsonValue(content)
  if (editor.adapter !== 'GATEWAY_INLINE_RULE') return content
  if (!isObject(parsedContent)) {
    throw new Error('Gateway 规则内容必须是 JSON 对象')
  }

  // adapter 为 GATEWAY_INLINE_RULE 时 prepareConfigEditor 必已设置 gateway
  const originalSnapshot = editor.gateway!.snapshot
  const contentJson = canonicalJson(parsedContent)
  const ruleContentSha256 = await sha256(contentJson)
  const material = {
    content: parsedContent,
    generatedAt: originalSnapshot.generatedAt,
    releaseId: originalSnapshot.releaseId,
    ruleContentSha256,
    ruleSchemaVersion: originalSnapshot.ruleSchemaVersion,
  }
  const artifactSha256 = await sha256(canonicalJson(material))
  const snapshot = {
    ...originalSnapshot,
    content: parsedContent,
    ruleContentSha256,
    artifactSha256,
  }
  const inlineSnapshot = canonicalJson(snapshot)
  const activation = {
    ...editor.gateway!.activation,
    ruleContentSha256,
    artifactSha256,
    inlineSnapshot,
    totalSize: new TextEncoder().encode(inlineSnapshot).length,
  }
  return canonicalJson(activation)
}
