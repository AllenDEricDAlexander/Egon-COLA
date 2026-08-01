import { useCallback, useEffect, useState } from 'react'
import { message } from 'antd'
import { ddcApi } from '../../api/client'

export type ScopeOption = { value: string; label: string }

const cache = new Map<string, Promise<ScopeOption[]>>()

/** 测试用：清空会话缓存 */
export const clearScopeOptionsCache = (): void => {
  cache.clear()
}

const fetchOptions = (path: string): Promise<ScopeOption[]> => {
  const cached = cache.get(path)
  if (cached) return cached
  const promise = ddcApi<unknown>(path).then((data) => {
    if (!Array.isArray(data)) return []
    return data.map((item) => {
      if (typeof item === 'string') return { value: item, label: item }
      const record = item as Record<string, unknown>
      const name = String(record.bizName ?? record.appName ?? record.namespace ?? record.description ?? '').trim()
      const code = String(record.appCode ?? record.bizCode ?? record.namespaceCode ?? record.envCode ?? '')
      return { value: code, label: name ? `${code}（${name}）` : code }
    })
  })
  cache.set(path, promise)
  promise.catch(() => {
    cache.delete(path)
  })
  return promise
}

const withParams = (path: string, values: Record<string, string>): string => {
  const params = new URLSearchParams()
  Object.entries(values).forEach(([key, value]) => {
    const trimmed = value.trim()
    if (trimmed !== '') params.set(key, trimmed)
  })
  const query = params.toString()
  return query === '' ? path : `${path}?${query}`
}

const messageError = (error: unknown): void => {
  message.error(error instanceof Error ? error.message : String(error))
}

/**
 * 作用域选项加载：业务域 → 命名空间 → 环境 → 应用。
 * 每一级都使用已选择的上级作用域过滤；空值保留为可选筛选。
 */
export function useScopeOptions(
  bizCode: string,
  namespaceCode: string,
  env: string,
): {
  bizs: ScopeOption[]
  apps: ScopeOption[]
  namespaces: ScopeOption[]
  envs: ScopeOption[]
  loading: boolean
  reload: () => void
} {
  const [bizs, setBizs] = useState<ScopeOption[]>([])
  const [apps, setApps] = useState<ScopeOption[]>([])
  const [namespaces, setNamespaces] = useState<ScopeOption[]>([])
  const [envs, setEnvs] = useState<ScopeOption[]>([])
  const [loading, setLoading] = useState(false)

  const loadBizs = useCallback(async () => {
    const options = await fetchOptions('/api/v1/ddc/bizs')
    setBizs(options)
  }, [])

  const namespacePath = withParams('/api/v1/ddc/namespaces', { bizCode })
  const envPath = withParams('/api/v1/ddc/envs', { bizCode, namespaceCode })
  const appPath = withParams('/api/v1/ddc/apps', { bizCode, namespaceCode, env })

  const loadNamespaces = useCallback(async () => {
    const options = await fetchOptions(namespacePath)
    setNamespaces(options)
  }, [namespacePath])

  const loadEnvs = useCallback(async () => {
    const options = await fetchOptions(envPath)
    setEnvs(options)
  }, [envPath])

  const loadApps = useCallback(async () => {
    setLoading(true)
    try {
      const options = await fetchOptions(appPath)
      setApps(options)
    } finally {
      setLoading(false)
    }
  }, [appPath])

  useEffect(() => {
    void Promise.resolve().then(loadBizs).catch(messageError)
  }, [loadBizs])

  useEffect(() => {
    void Promise.resolve().then(loadEnvs).catch(messageError)
  }, [loadEnvs])

  useEffect(() => {
    void Promise.resolve().then(loadApps).catch(messageError)
  }, [loadApps])

  useEffect(() => {
    void Promise.resolve().then(loadNamespaces).catch(messageError)
  }, [loadNamespaces])

  const reload = useCallback(() => {
    cache.delete('/api/v1/ddc/bizs')
    cache.delete(namespacePath)
    cache.delete(envPath)
    cache.delete(appPath)
    void loadBizs().catch(messageError)
    void loadEnvs().catch(messageError)
    void loadApps().catch(messageError)
    void loadNamespaces().catch(messageError)
  }, [namespacePath, envPath, appPath, loadBizs, loadEnvs, loadApps, loadNamespaces])

  return { bizs, apps, namespaces, envs, loading, reload }
}
