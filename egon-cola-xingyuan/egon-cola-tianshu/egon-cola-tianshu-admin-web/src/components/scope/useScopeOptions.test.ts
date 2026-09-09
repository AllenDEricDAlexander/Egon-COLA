import {QueryClientProvider} from '@tanstack/react-query'
import {createElement, type PropsWithChildren} from 'react'
import {renderHook, waitFor} from '@testing-library/react'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {setDdcUnauthorizedHandler} from '../../api/client'
import {createDdcQueryClient} from '../../query/queryClient'
import {scopeOptionKey, useScopeOption} from './useScopeOptions'

const record = (data: unknown) => ({
  success: true,
  code: 0,
  status: 'SUCCESS',
  message: '',
  data,
  traceId: 't',
  timestamp: 1,
})

const jsonResponse = (body: unknown) => new Response(
  JSON.stringify(body),
  { status: 200, headers: { 'Content-Type': 'application/json' } },
)

describe('useScopeOption', () => {
  beforeEach(() => {
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('loads and maps only the requested scope path', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(record([
      {
        id: 'b1',
        bizCode: 'pay-biz',
        bizName: '支付业务域',
        enabled: true,
      },
    ])))
    const client = createDdcQueryClient()
    const wrapper = ({ children }: PropsWithChildren) => createElement(
      QueryClientProvider,
      { client },
      children,
    )

    const { result } = renderHook(
      () => useScopeOption('/api/v1/ddc/bizs'),
      { wrapper },
    )

    await waitFor(() => expect(result.current.data).toEqual([
      { value: 'pay-biz', label: 'pay-biz（支付业务域）' },
    ]))
    expect(fetch).toHaveBeenCalledTimes(1)
    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/ddc/bizs',
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    )
    expect(scopeOptionKey('/api/v1/ddc/bizs'))
      .toEqual(['ddc', 'scope-options', '/api/v1/ddc/bizs'])
  })

  it('shares cached options and refetches when the cascade path changes', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(record([])))
    const client = createDdcQueryClient()
    const wrapper = ({ children }: PropsWithChildren) => createElement(
      QueryClientProvider,
      { client },
      children,
    )
    const { rerender } = renderHook(
      ({ path }: { path: string }) => useScopeOption(path),
      { initialProps: { path: '/api/v1/ddc/apps' }, wrapper },
    )
    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1))

    rerender({ path: '/api/v1/ddc/apps' })
    expect(fetch).toHaveBeenCalledTimes(1)

    rerender({ path: '/api/v1/ddc/apps?bizCode=pay-biz' })
    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(2))
    expect(String(vi.mocked(fetch).mock.calls[1][0]))
      .toContain('bizCode=pay-biz')
  })
})
