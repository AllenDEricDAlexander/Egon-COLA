import type { Scope } from '../api/types'

const valueOr = (value: string | undefined, fallback: string) => {
  const normalized = value?.trim()
  return normalized ? normalized : fallback
}

export const resolveInitialScope = (
  configuredEnv?: string,
  configuredNamespace?: string,
): Scope => ({
  env: valueOr(configuredEnv, 'dev'),
  namespace: valueOr(configuredNamespace, 'default'),
})

export const configuredInitialScope = resolveInitialScope(
  import.meta.env.VITE_GATEWAY_ADMIN_DEFAULT_ENV,
  import.meta.env.VITE_GATEWAY_ADMIN_DEFAULT_NAMESPACE,
)

export const scopeOptions = (
  current: string,
  defaults: string[],
  label = 'Namespace',
) => [...new Set([...defaults, current].map((value) => value.trim()).filter(Boolean))]
  .map((value) => ({ value, label: `${label}: ${value}` }))
