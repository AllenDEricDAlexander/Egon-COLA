import type { PlatformKey } from '../manifest/types'

export type SafeValue =
  | string
  | number
  | boolean
  | null
  | SafeValue[]
  | { readonly [key: string]: SafeValue }

export interface ChildContext {
  readonly platformKey: PlatformKey
  readonly routeIntent?: string
  readonly scopeDisplay?: string
  readonly capabilitySummary?: { readonly [key: string]: SafeValue }
  readonly hostVersion?: string
}

const sensitiveKey = /token|secret|cookie|authorization|password|raw.?body|header/i

const isPlatformKey = (value: unknown): value is PlatformKey => (
  value === 'tianquan-shoubing' || value === 'tianquan-jianshen' || value === 'yuheng' || value === 'tianshu'
)

const sanitizeValue = (value: unknown, key?: string): SafeValue | undefined => {
  if (key && sensitiveKey.test(key)) {
    return undefined
  }
  if (value === null || typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
    return value
  }
  if (Array.isArray(value)) {
    return value
      .map((item) => sanitizeValue(item))
      .filter((item): item is SafeValue => item !== undefined)
  }
  if (typeof value === 'object') {
    const sanitized: Record<string, SafeValue> = {}
    Object.entries(value).forEach(([entryKey, entryValue]) => {
      const safeValue = sanitizeValue(entryValue, entryKey)
      if (safeValue !== undefined) {
        sanitized[entryKey] = safeValue
      }
    })
    return sanitized
  }
  return undefined
}

export const sanitizeChildContext = (source: Record<string, unknown>): ChildContext => {
  if (!isPlatformKey(source.platformKey)) {
    throw new Error('Invalid child xingyuan key')
  }

  const result: ChildContext = { platformKey: source.platformKey }
  const mutable = result as { -readonly [K in keyof ChildContext]?: ChildContext[K] }
  ;(['routeIntent', 'scopeDisplay', 'hostVersion'] as const).forEach((key) => {
    if (typeof source[key] === 'string') {
      mutable[key] = source[key]
    }
  })

  const capabilitySummary = sanitizeValue(source.capabilitySummary)
  if (capabilitySummary && typeof capabilitySummary === 'object' && !Array.isArray(capabilitySummary)) {
    mutable.capabilitySummary = capabilitySummary as { readonly [key: string]: SafeValue }
  }
  return result
}

export const buildChildProps = (source: Record<string, unknown>): ChildContext => sanitizeChildContext(source)

export const buildRouteEvent = (source: Record<string, unknown>): ChildContext => sanitizeChildContext(source)
