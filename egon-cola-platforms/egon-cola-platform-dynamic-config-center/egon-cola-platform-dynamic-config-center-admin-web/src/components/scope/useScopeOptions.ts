import { useQuery } from '@tanstack/react-query'
import { ddcApi } from '../../api/client'

export type ScopeOption = { value: string; label: string }

export const scopeOptionQueryKey = ['ddc', 'scope-options'] as const

export const scopeOptionKey = (path: string) => [
  ...scopeOptionQueryKey,
  path,
] as const

export const withParams = (
  path: string,
  values: Record<string, string>,
): string => {
  const params = new URLSearchParams()
  Object.entries(values).forEach(([key, value]) => {
    const trimmed = value.trim()
    if (trimmed !== '') params.set(key, trimmed)
  })
  const query = params.toString()
  return query === '' ? path : `${path}?${query}`
}

const toOptions = (data: unknown): ScopeOption[] => {
  if (!Array.isArray(data)) return []
  return data.map((item) => {
    if (typeof item === 'string') return { value: item, label: item }
    const record = item as Record<string, unknown>
    const name = String(
      record.bizName
        ?? record.appName
        ?? record.namespace
        ?? record.description
        ?? '',
    ).trim()
    const code = String(
      record.appCode
        ?? record.bizCode
        ?? record.namespaceCode
        ?? record.envCode
        ?? '',
    )
    return { value: code, label: name ? `${code}（${name}）` : code }
  })
}

export function useScopeOption(path: string) {
  return useQuery({
    queryKey: scopeOptionKey(path),
    queryFn: ({ signal }) => ddcApi<unknown>(path, { signal }).then(toOptions),
  })
}
