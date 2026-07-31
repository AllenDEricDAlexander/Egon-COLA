import { renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../../api/client'
import { ENV_OPTIONS, clearScopeOptionsCache, useScopeOptions } from './useScopeOptions'

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

  it('exposes the fixed env enum', () => {
    expect(ENV_OPTIONS).toEqual(['dev', 'test', 'sit', 'gray', 'prod'])
  })

  it('loads domains and all apps without namespace filter', async () => {
    vi.mocked(fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/namespaces/domains')) {
        return Promise.resolve(jsonResponse(record(['orders', 'billing'])))
      }
      if (url.includes('/apps')) {
        return Promise.resolve(jsonResponse(record([
          { id: 'a1', appCode: 'orders', appName: '订单服务', owner: 'ops', description: '', enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z' },
          { id: 'a2', appCode: 'billing', appName: '', owner: 'ops', description: '', enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z' },
        ])))
      }
      return Promise.resolve(jsonResponse(record(null)))
    })

    const { result } = renderHook(() => useScopeOptions(''))
    await waitFor(() => expect(result.current.namespaces.length).toBe(2))
    await waitFor(() => expect(result.current.apps.length).toBe(2))
    expect(result.current.apps[0]).toEqual({ value: 'orders', label: 'orders（订单服务）' })
    expect(result.current.apps[1]).toEqual({ value: 'billing', label: 'billing' })
    const appsCall = vi.mocked(fetch).mock.calls.find(([url]) => String(url).includes('/apps'))
    expect(String(appsCall![0])).not.toContain('namespace=')
  })

  it('reloads apps filtered by namespace when it changes', async () => {
    vi.mocked(fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/namespaces/domains')) {
        return Promise.resolve(jsonResponse(record(['orders'])))
      }
      if (url.includes('/apps')) {
        return Promise.resolve(jsonResponse(record([
          { id: 'a1', appCode: 'orders', appName: '', owner: 'ops', description: '', enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z' },
        ])))
      }
      return Promise.resolve(jsonResponse(record(null)))
    })

    const { result, rerender } = renderHook(({ ns }: { ns: string }) => useScopeOptions(ns), { initialProps: { ns: '' } })
    await waitFor(() => expect(result.current.apps.length).toBe(1))
    const beforeCalls = vi.mocked(fetch).mock.calls.length

    rerender({ ns: 'orders' })
    await waitFor(() => {
      const filtered = vi.mocked(fetch).mock.calls
        .slice(beforeCalls)
        .find(([url]) => String(url).includes('/apps'))
      expect(filtered).toBeDefined()
      expect(String(filtered![0])).toContain('namespace=orders')
    })
  })
})
