import { useCallback, useEffect, useState } from 'react'
import { message } from 'antd'
import { ddcApi } from '../../api/client'
import type { DdcApp, DdcBiz, DdcEnv, DdcNamespace } from '../../api/types'

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
      const code = String(record.bizCode ?? record.appCode ?? record.namespace ?? record.envCode ?? '')
      return { value: code, label: name ? `${code}（${name}）` : code }
    })
  })
  cache.set(path, promise)
  promise.catch(() => {
    cache.delete(path)
  })
  return promise
}

const withParam = (path: string, key: string, value: string): string => {
  const trimmed = value.trim()
  return trimmed === '' ? path : `${path}?${key}=${encodeURIComponent(trimmed)}`
}

const messageError = (error: unknown): void => {
  message.error(error instanceof Error ? error.message : String(error))
}

/**
 * 作用域选项加载：业务域 → 应用（按 biz 过滤）→ 命名空间（按 app 过滤），
 * 环境独立从后端实体拉取。级联变化时清空下级并重载。
 */
export function useScopeOptions(bizCode: string, appCode: string): {
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

  const loadEnvs = useCallback(async () => {
    const options = await fetchOptions('/api/v1/ddc/envs')
    setEnvs(options)
  }, [])

  const loadApps = useCallback(async () => {
    setLoading(true)
    try {
      const options = await fetchOptions(withParam('/api/v1/ddc/apps', 'biz', bizCode))
      setApps(options)
    } finally {
      setLoading(false)
    }
  }, [bizCode])

  const loadNamespaces = useCallback(async () => {
    const options = await fetchOptions(withParam('/api/v1/ddc/namespaces', 'appCode', appCode))
    setNamespaces(options)
  }, [appCode])

  useEffect(() => {
    loadBizs().catch(messageError)
  }, [loadBizs])

  useEffect(() => {
    loadEnvs().catch(messageError)
  }, [loadEnvs])

  useEffect(() => {
    loadApps().catch(messageError)
  }, [loadApps])

  useEffect(() => {
    loadNamespaces().catch(messageError)
  }, [loadNamespaces])

  const reload = useCallback(() => {
    cache.delete('/api/v1/ddc/bizs')
    cache.delete('/api/v1/ddc/envs')
    cache.delete(withParam('/api/v1/ddc/apps', 'biz', bizCode))
    cache.delete(withParam('/api/v1/ddc/namespaces', 'appCode', appCode))
    void loadBizs().catch(messageError)
    void loadEnvs().catch(messageError)
    void loadApps().catch(messageError)
    void loadNamespaces().catch(messageError)
  }, [bizCode, appCode, loadBizs, loadEnvs, loadApps, loadNamespaces])

  return { bizs, apps, namespaces, envs, loading, reload }
}
