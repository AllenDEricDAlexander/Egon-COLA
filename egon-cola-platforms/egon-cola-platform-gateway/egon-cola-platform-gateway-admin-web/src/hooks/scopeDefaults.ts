import type { GatewayScopeBinding, Scope } from '../api/types'

const fieldOrder = ['bizCode', 'namespace', 'env', 'appCode'] as const

export type ScopeField = typeof fieldOrder[number]

const fieldLabel: Record<ScopeField, string> = {
  bizCode: 'Biz',
  namespace: 'Namespace',
  env: 'Env',
  appCode: 'App',
}

const scopeOf = (binding?: GatewayScopeBinding): Scope | undefined =>
  binding
    ? {
        bizCode: binding.bizCode,
        namespace: binding.namespace,
        env: binding.env,
        appCode: binding.appCode,
      }
    : undefined

const sameScope = (left: Scope, right: Scope) =>
  fieldOrder.every((field) => left[field] === right[field])

export const resolveInitialScope = (
  bindings: GatewayScopeBinding[],
  stored?: Scope,
  configured?: Scope,
): Scope | undefined => {
  const valid = (candidate?: Scope): Scope | undefined =>
    candidate && bindings.some((binding) => sameScope(binding, candidate))
      ? candidate
      : undefined
  return valid(stored)
    ?? valid(configured)
    ?? scopeOf(bindings.find((binding) => binding.connected))
    ?? scopeOf(bindings[0])
}

export const changeScope = (
  bindings: GatewayScopeBinding[],
  current: Scope,
  field: ScopeField,
  value: string,
): Scope => {
  const index = fieldOrder.indexOf(field)
  const prefix = { ...current, [field]: value }
  const matchesPrefix = (binding: GatewayScopeBinding) =>
    fieldOrder.slice(0, index + 1)
      .every((name) => binding[name] === prefix[name])
  const retained = bindings.find((binding) =>
    matchesPrefix(binding)
      && fieldOrder.slice(index + 1)
        .every((name) => binding[name] === current[name]))
  const selected = retained ?? bindings.find(matchesPrefix)
  if (!selected) throw new Error(`No DDC scope for ${field}=${value}`)
  return scopeOf(selected)!
}

export const optionsFor = (
  bindings: GatewayScopeBinding[],
  scope: Scope,
  field: ScopeField,
) => {
  const index = fieldOrder.indexOf(field)
  const values = bindings
    .filter((binding) => fieldOrder.slice(0, index)
      .every((name) => binding[name] === scope[name]))
    .map((binding) => binding[field])
  return [...new Set(values)].map((value) => ({
    value,
    label: `${fieldLabel[field]}: ${value}`,
  }))
}

const configuredScope = (
  bizCode?: string,
  namespace?: string,
  env?: string,
  appCode?: string,
): Scope | undefined => {
  const values = [bizCode, namespace, env, appCode]
    .map((value) => value?.trim())
  return values.every(Boolean)
    ? {
        bizCode: values[0]!,
        namespace: values[1]!,
        env: values[2]!,
        appCode: values[3]!,
      }
    : undefined
}

export const configuredInitialScope = configuredScope(
  import.meta.env.VITE_GATEWAY_ADMIN_DEFAULT_BIZ_CODE,
  import.meta.env.VITE_GATEWAY_ADMIN_DEFAULT_NAMESPACE,
  import.meta.env.VITE_GATEWAY_ADMIN_DEFAULT_ENV,
  import.meta.env.VITE_GATEWAY_ADMIN_DEFAULT_APP_CODE,
)
