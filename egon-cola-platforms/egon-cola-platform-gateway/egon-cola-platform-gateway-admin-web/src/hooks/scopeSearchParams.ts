import type { Scope } from '../api/types'

export const scopeFieldOrder = [
  'bizCode',
  'namespace',
  'env',
  'appCode',
] as const

export type ScopeField = typeof scopeFieldOrder[number]

const keyFor = (field: ScopeField, prefix = '') =>
  prefix ? `${prefix}${field.slice(0, 1).toUpperCase()}${field.slice(1)}` : field

export const readScopeSearchParams = (
  params: URLSearchParams,
  fields: readonly ScopeField[],
  paramPrefix = '',
): Partial<Scope> => Object.fromEntries(
  fields.flatMap((field) => {
    const value = params.get(keyFor(field, paramPrefix))?.trim()
    return value ? [[field, value]] : []
  }),
) as Partial<Scope>

export const writeScopeSearchParams = (
  current: URLSearchParams,
  value: Partial<Scope>,
  fields: readonly ScopeField[],
  paramPrefix = '',
): URLSearchParams => {
  const next = new URLSearchParams(current)
  fields.forEach((field) => {
    const key = keyFor(field, paramPrefix)
    const normalized = value[field]?.trim()
    if (normalized) next.set(key, normalized)
    else next.delete(key)
  })
  return next
}

export const hasRequiredScopeFields = (
  value: Partial<Scope>,
  fields: readonly ScopeField[],
): boolean => fields.every((field) => Boolean(value[field]?.trim()))
