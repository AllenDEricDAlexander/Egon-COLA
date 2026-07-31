import { useCallback, useEffect, useState } from 'react'
import { message } from 'antd'
import { ddcApi } from '../../api/client'
import type { DdcApp } from '../../api/types'

export const ENV_OPTIONS = ['dev', 'test', 'sit', 'gray', 'prod']

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
      const app = item as Partial<DdcApp>
      const name = app.appName?.trim()
      return { value: String(app.appCode), label: name ? `${app.appCode}（${name}）` : String(app.appCode) }
    })
  })
  cache.set(path, promise)
  promise.catch(() => {
    cache.delete(path)
  })
  return promise
}

const appsPath = (namespace: string): string => {
  const trimmed = namespace.trim()
  return trimmed === '' ? '/api/v1/ddc/apps' : `/api/v1/ddc/apps?namespace=${encodeURIComponent(trimmed)}`
}

const messageError = (error: unknown): void => {
  message.error(error instanceof Error ? error.message : String(error))
}

export function useScopeOptions(namespace: string): {
  apps: ScopeOption[]
  namespaces: ScopeOption[]
  loading: boolean
  reload: () => void
} {
  const [apps, setApps] = useState<ScopeOption[]>([])
  const [namespaces, setNamespaces] = useState<ScopeOption[]>([])
  const [loading, setLoading] = useState(false)

  const loadNamespaces = useCallback(async () => {
    const options = await fetchOptions('/api/v1/ddc/namespaces/domains')
    setNamespaces(options)
  }, [])

  const loadApps = useCallback(async () => {
    setLoading(true)
    try {
      const options = await fetchOptions(appsPath(namespace))
      setApps(options)
    } finally {
      setLoading(false)
    }
  }, [namespace])

  useEffect(() => {
    loadNamespaces().catch(messageError)
  }, [loadNamespaces])

  useEffect(() => {
    loadApps().catch(messageError)
  }, [loadApps])

  const reload = useCallback(() => {
    cache.delete('/api/v1/ddc/namespaces/domains')
    cache.delete(appsPath(namespace))
    void loadNamespaces().catch(messageError)
    void loadApps().catch(messageError)
  }, [namespace, loadNamespaces, loadApps])

  return { apps, namespaces, loading, reload }
}
