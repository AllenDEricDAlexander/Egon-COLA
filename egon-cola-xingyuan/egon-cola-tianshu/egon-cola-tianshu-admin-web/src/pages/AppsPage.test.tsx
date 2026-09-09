import {fireEvent, screen, waitFor} from '@testing-library/react'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {setDdcUnauthorizedHandler} from '../api/client'
import {renderWithQueryClient} from '../test/renderWithQueryClient'
import AppsPage from './AppsPage'

const record = (data: unknown) => ({
  success: true,
  code: 0,
  status: 'SUCCESS',
  message: '',
  data,
  traceId: 't',
  timestamp: 1,
})

const pageRecord = (total = 14) => ({
  success: true,
  code: 0,
  status: 'SUCCESS',
  message: '',
  records: [{
    id: 'a1',
    appCode: 'orders',
    bizCode: 'pay-biz',
    appName: '订单服务',
    owner: 'ops',
    description: '',
    enabled: true,
    createdAt: '2026-07-01T00:00:00Z',
    updatedAt: '2026-07-01T00:00:00Z',
  }],
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

const mockOptionsAndPage = (pageResponse: () => Response): void => {
  vi.mocked(fetch).mockImplementation((input) => {
    const url = String(input)
    if (url.includes('/apps/page')) return Promise.resolve(pageResponse())
    if (url.includes('/bizs') || url.includes('/namespaces') || url.includes('/envs')) {
      return Promise.resolve(jsonResponse(record([])))
    }
    return Promise.resolve(jsonResponse(record([])))
  })
}

describe('AppsPage', () => {
  beforeEach(() => {
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('uses the page endpoint and resets filters', async () => {
    mockOptionsAndPage(() => jsonResponse(pageRecord()))
    renderWithQueryClient(<AppsPage />)

    expect(await screen.findByText('订单服务')).toBeInTheDocument()
    expect(screen.getByText('共 14 条')).toBeInTheDocument()

    fireEvent.change(screen.getByPlaceholderText('appCode / 名称模糊查询'), {
      target: { value: 'orders' },
    })
    fireEvent.click(screen.getByRole('button', { name: /查\s*询/ }))
    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      expect.stringMatching(/\/apps\/page\?.*keyword=orders.*pageNo=1/),
      expect.anything(),
    ))

    fireEvent.click(screen.getByRole('button', { name: /重\s*置/ }))
    await waitFor(() => {
      const latest = vi.mocked(fetch).mock.calls
        .filter(([input]) => String(input).includes('/apps/page'))
        .at(-1)
      expect(String(latest?.[0])).not.toContain('keyword=')
    })
  })

  it('retries an app page failure', async () => {
    let attempts = 0
    mockOptionsAndPage(() => {
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
    renderWithQueryClient(<AppsPage />)

    fireEvent.click(await screen.findByRole('button', { name: /重\s*试/ }))
    expect(await screen.findByText('订单服务')).toBeInTheDocument()
  })
})
