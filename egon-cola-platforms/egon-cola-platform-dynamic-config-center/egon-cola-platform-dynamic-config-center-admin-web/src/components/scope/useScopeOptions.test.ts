import { renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../../api/client'
import { clearScopeOptionsCache, useScopeOptions } from './useScopeOptions'

const record = (data: unknown) => ({
  success: true, code: 0, status: 'SUCCESS', message: '', data, traceId: 't', timestamp: 1,
})

const jsonResponse = (body: unknown) =>
  new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })

describe('useScopeOptions', () => {
  beforeEach(() => {
    clearScopeOptionsCache()
    setDdcTokenProvider(() => 'token')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('loads bizs, envs and all apps without filters', async () => {
    vi.mocked(fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/bizs')) {
        return Promise.resolve(jsonResponse(record([
          { id: 'b1', bizCode: 'pay-biz', bizName: '支付业务域', description: '', enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z' },
        ])))
      }
      if (url.includes('/envs')) {
        return Promise.resolve(jsonResponse(record([
          { id: 'e1', envCode: 'dev', description: '开发环境', sortOrder: 10, enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z' },
        ])))
      }
      if (url.includes('/apps')) {
        return Promise.resolve(jsonResponse(record([
          { id: 'a1', appCode: 'orders', bizCode: 'pay-biz', appName: '订单服务', owner: 'ops', description: '', enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z' },
        ])))
      }
      if (url.includes('/namespaces')) {
        return Promise.resolve(jsonResponse(record([
          { id: 'n1', appCode: 'orders', namespace: 'default', description: '', enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z' },
        ])))
      }
      return Promise.resolve(jsonResponse(record(null)))
    })

    const { result } = renderHook(() => useScopeOptions('', ''))
    await waitFor(() => expect(result.current.bizs.length).toBe(1))
    await waitFor(() => expect(result.current.envs.length).toBe(1))
    await waitFor(() => expect(result.current.apps.length).toBe(1))
    await waitFor(() => expect(result.current.namespaces.length).toBe(1))
    expect(result.current.bizs[0]).toEqual({ value: 'pay-biz', label: 'pay-biz（支付业务域）' })
    expect(result.current.envs[0]).toEqual({ value: 'dev', label: 'dev（开发环境）' })
    expect(result.current.apps[0]).toEqual({ value: 'orders', label: 'orders（订单服务）' })
    const appsCall = vi.mocked(fetch).mock.calls.find(([url]) => String(url).includes('/apps'))
    expect(String(appsCall![0])).not.toContain('biz=')
  })

  it('reloads apps by biz and namespaces by app when cascade changes', async () => {
    vi.mocked(fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/bizs')) return Promise.resolve(jsonResponse(record([])))
      if (url.includes('/envs')) return Promise.resolve(jsonResponse(record([])))
      if (url.includes('/apps')) {
        return Promise.resolve(jsonResponse(record([
          { id: 'a1', appCode: 'orders', bizCode: 'pay-biz', appName: '', owner: 'ops', description: '', enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z' },
        ])))
      }
      if (url.includes('/namespaces')) {
        return Promise.resolve(jsonResponse(record([
          { id: 'n1', appCode: 'orders', namespace: 'default', description: '', enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z' },
        ])))
      }
      return Promise.resolve(jsonResponse(record(null)))
    })

    const { rerender } = renderHook(
      ({ biz, app }: { biz: string; app: string }) => useScopeOptions(biz, app),
      { initialProps: { biz: '', app: '' } },
    )
    await waitFor(() => {
      const appsCall = vi.mocked(fetch).mock.calls.find(([url]) => String(url).includes('/apps'))
      expect(appsCall).toBeDefined()
    })
    const beforeCalls = vi.mocked(fetch).mock.calls.length

    rerender({ biz: 'pay-biz', app: 'orders' })
    await waitFor(() => {
      const filtered = vi.mocked(fetch).mock.calls
        .slice(beforeCalls)
        .find(([url]) => String(url).includes('/apps'))
      expect(filtered).toBeDefined()
      expect(String(filtered![0])).toContain('biz=pay-biz')
    })
    await waitFor(() => {
      const nsCall = vi.mocked(fetch).mock.calls
        .slice(beforeCalls)
        .find(([url]) => String(url).includes('/namespaces'))
      expect(nsCall).toBeDefined()
      expect(String(nsCall![0])).toContain('appCode=orders')
    })
  })
})
