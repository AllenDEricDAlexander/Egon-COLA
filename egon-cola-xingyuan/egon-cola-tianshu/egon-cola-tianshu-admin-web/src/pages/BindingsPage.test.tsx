import { fireEvent, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderWithQueryClient } from '../test/renderWithQueryClient'
import BindingsPage from './BindingsPage'

const pageResponse = {
  success: true,
  code: 0,
  status: 'SUCCESS',
  message: '',
  records: [{
    id: 'binding-1',
    bizCode: 'pay',
    namespaceId: 'namespace-1',
    namespaceCode: 'default',
    env: 'dev',
    appId: 'app-1',
    appCode: 'orders',
    appName: '订单服务',
    enabled: true,
  }],
  page: {
    total: 1,
    pageNo: 1,
    pageSize: 10,
    pages: 1,
    hasNext: false,
    hasPrevious: false,
  },
  traceId: 'trace-bindings',
  timestamp: 1,
}

const recordResponse = (data: unknown) => ({
  success: true,
  code: 0,
  status: 'SUCCESS',
  message: '',
  data,
  traceId: 'trace-binding-mutation',
  timestamp: 1,
})

const jsonResponse = (body: unknown) => new Response(
  JSON.stringify(body),
  { status: 200, headers: { 'Content-Type': 'application/json' } },
)

describe('BindingsPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn((input, init) => {
      const url = String(input)
      if (url.includes('/namespace-env-app-bindings/page')) {
        return Promise.resolve(jsonResponse(pageResponse))
      }
      if (init?.method === 'POST') {
        return Promise.resolve(jsonResponse(recordResponse({ ...pageResponse.records[0], id: 'binding-2' })))
      }
      if (init?.method === 'DELETE') {
        return Promise.resolve(jsonResponse(recordResponse(null)))
      }
      return Promise.resolve(jsonResponse(recordResponse(null)))
    }))
  })

  it('loads the Controller page and creates a binding through the exact path', async () => {
    renderWithQueryClient(<BindingsPage />)

    expect(await screen.findByText('订单服务')).toBeInTheDocument()
    expect(vi.mocked(fetch).mock.calls[0][0]).toEqual(
      expect.stringContaining('/api/v1/tianshu/namespace-env-app-bindings/page'),
    )

    fireEvent.click(screen.getByRole('button', { name: '新增绑定' }))
    fireEvent.change(document.getElementById('bizCode')!, { target: { value: 'pay' } })
    fireEvent.change(document.getElementById('namespaceCode')!, { target: { value: 'default' } })
    fireEvent.change(document.getElementById('env')!, { target: { value: 'dev' } })
    fireEvent.change(document.getElementById('appCode')!, { target: { value: 'orders' } })
    fireEvent.click(screen.getByRole('button', { name: /保.*存/ }))

    await waitFor(() => expect(vi.mocked(fetch).mock.calls.some(([, init]) => (
      init?.method === 'POST'
    ))).toBe(true))
    const postCall = vi.mocked(fetch).mock.calls.find(([, init]) => init?.method === 'POST')
    expect(postCall?.[0]).toEqual('/api/v1/tianshu/namespace-env-app-bindings')
  })
})
