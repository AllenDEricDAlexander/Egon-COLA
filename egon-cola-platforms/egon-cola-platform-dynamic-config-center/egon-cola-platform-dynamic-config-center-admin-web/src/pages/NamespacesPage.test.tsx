import { fireEvent, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../api/client'
import { renderWithQueryClient } from '../test/renderWithQueryClient'
import NamespacesPage from './NamespacesPage'

const record = (data: unknown) => ({
  success: true,
  code: 0,
  status: 'SUCCESS',
  message: '',
  data,
  traceId: 't',
  timestamp: 1,
})

const namespace = {
  id: 'n1',
  bizCode: 'pay-biz',
  namespaceCode: 'ns-default',
  namespace: '默认命名空间',
  description: '',
  enabled: true,
  createdAt: '2026-07-01T00:00:00Z',
  updatedAt: '2026-07-01T00:00:00Z',
}

const pageRecord = (total = 11) => ({
  success: true,
  code: 0,
  status: 'SUCCESS',
  message: '',
  records: [namespace],
  page: {
    total,
    pageNo: 1,
    pageSize: 10,
    pages: Math.ceil(total / 10),
    hasNext: total > 10,
    hasPrevious: false,
  },
  traceId: 't',
  timestamp: 1,
})

const jsonResponse = (body: unknown) => new Response(
  JSON.stringify(body),
  { status: 200, headers: { 'Content-Type': 'application/json' } },
)

const mockEndpoints = (pageResponse: () => Response): void => {
  vi.mocked(fetch).mockImplementation((input) => {
    const url = String(input)
    if (url.includes('/namespaces/page')) return Promise.resolve(pageResponse())
    if (url.includes('/bizs')) {
      return Promise.resolve(jsonResponse(record([
        { id: 'b1', bizCode: 'pay-biz', bizName: '支付业务域', enabled: true },
      ])))
    }
    if (url.includes('/namespace-env-app-bindings')) {
      return Promise.resolve(jsonResponse(record([])))
    }
    if (url.includes('/envs')) {
      return Promise.resolve(jsonResponse(record([
        { id: 'e1', envCode: 'dev', description: '开发', sortOrder: 10, enabled: true },
      ])))
    }
    if (url.includes('/apps')) {
      return Promise.resolve(jsonResponse(record([
        {
          id: 'a1',
          appCode: 'orders',
          bizCode: 'pay-biz',
          appName: '订单服务',
          enabled: true,
        },
      ])))
    }
    return Promise.resolve(jsonResponse(record([])))
  })
}

describe('NamespacesPage', () => {
  beforeEach(() => {
    setDdcTokenProvider(() => 'token')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('uses page filters and reset', async () => {
    mockEndpoints(() => jsonResponse(pageRecord()))
    renderWithQueryClient(<NamespacesPage />)

    expect(await screen.findByText('默认命名空间')).toBeInTheDocument()
    expect(screen.getByText('共 11 条')).toBeInTheDocument()

    fireEvent.change(screen.getByPlaceholderText('命名空间模糊查询'), {
      target: { value: 'default' },
    })
    fireEvent.click(screen.getByRole('button', { name: /查\s*询/ }))
    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      expect.stringMatching(/\/namespaces\/page\?.*keyword=default.*pageNo=1/),
      expect.anything(),
    ))

    fireEvent.click(screen.getByRole('button', { name: /重\s*置/ }))
    await waitFor(() => {
      const latest = vi.mocked(fetch).mock.calls
        .filter(([input]) => String(input).includes('/namespaces/page'))
        .at(-1)
      expect(String(latest?.[0])).not.toContain('keyword=')
    })
  })

  it('opens a responsive binding drawer with a multiple select', async () => {
    mockEndpoints(() => jsonResponse(pageRecord(1)))
    renderWithQueryClient(<NamespacesPage />)

    fireEvent.click(await screen.findByRole('button', { name: /管\s*理\s*绑\s*定/ }))

    await waitFor(() => expect(document.querySelector('.ant-drawer-open'))
      .toBeInTheDocument())
    expect(document.querySelector('.ant-checkbox-group')).not.toBeInTheDocument()
    expect(document.querySelector('.ant-select-multiple')).toBeInTheDocument()
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/ddc/namespace-env-app-bindings?'),
      expect.anything(),
    )
    expect(document.querySelector('.ant-drawer-content-wrapper'))
      .toHaveStyle({ width: '100%' })
  })

  it('retries a namespace page failure', async () => {
    let attempts = 0
    mockEndpoints(() => {
      attempts += 1
      return attempts === 1
        ? jsonResponse({
          success: false,
          code: 56999,
          status: 'DDC_INTERNAL_FAILURE',
          message: '加载失败',
          data: null,
          timestamp: 1,
        })
        : jsonResponse(pageRecord(1))
    })
    renderWithQueryClient(<NamespacesPage />)

    fireEvent.click(await screen.findByRole('button', { name: /重\s*试/ }))
    expect(await screen.findByText('默认命名空间')).toBeInTheDocument()
  })
})
