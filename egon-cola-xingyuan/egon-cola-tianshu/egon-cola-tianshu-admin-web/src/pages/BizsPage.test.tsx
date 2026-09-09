import {fireEvent, screen, waitFor} from '@testing-library/react'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {setDdcUnauthorizedHandler} from '../api/client'
import {renderWithQueryClient} from '../test/renderWithQueryClient'
import BizsPage from './BizsPage'

const pageRecord = <T,>(records: T[], total = records.length) => ({
  success: true,
  code: 0,
  status: 'SUCCESS',
  message: '',
  records,
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

const biz = (name = '支付业务域') => ({
  id: 'b1',
  bizCode: 'pay-biz',
  bizName: name,
  description: '',
  enabled: true,
  createdAt: '2026-07-01T00:00:00Z',
  updatedAt: '2026-07-01T00:00:00Z',
})

const jsonResponse = (body: unknown) => new Response(
  JSON.stringify(body),
  { status: 200, headers: { 'Content-Type': 'application/json' } },
)

describe('BizsPage', () => {
  beforeEach(() => {
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('uses server pagination and requests the selected page', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(pageRecord([biz()], 21)))

    renderWithQueryClient(<BizsPage />)

    expect(await screen.findByText('支付业务域')).toBeInTheDocument()
    expect(screen.getByText('共 21 条')).toBeInTheDocument()
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/ddc/bizs/page?pageNo=1&pageSize=10'),
      expect.anything(),
    )

    fireEvent.click(screen.getByTitle('2'))
    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/ddc/bizs/page?pageNo=2&pageSize=10'),
      expect.anything(),
    ))
  })

  it('aborts an obsolete filtered query and renders the latest result', async () => {
    let staleSignal: AbortSignal | undefined
    vi.mocked(fetch).mockImplementation((input, init) => {
      const url = String(input)
      if (url.includes('keyword=first')) {
        staleSignal = init?.signal ?? undefined
        return new Promise((_resolve, reject) => {
          staleSignal?.addEventListener('abort', () => reject(
            new DOMException('aborted', 'AbortError'),
          ))
        })
      }
      if (url.includes('keyword=second')) {
        return Promise.resolve(jsonResponse(pageRecord([biz('第二次结果')], 1)))
      }
      return Promise.resolve(jsonResponse(pageRecord([biz()], 21)))
    })
    renderWithQueryClient(<BizsPage />)
    await screen.findByText('支付业务域')

    const keyword = screen.getByPlaceholderText('名称模糊查询')
    fireEvent.change(keyword, { target: { value: 'first' } })
    fireEvent.click(screen.getByRole('button', { name: /查\s*询/ }))
    await waitFor(() => expect(staleSignal).toBeDefined())

    fireEvent.change(keyword, { target: { value: 'second' } })
    fireEvent.click(screen.getByRole('button', { name: /查\s*询/ }))

    await waitFor(() => expect(staleSignal?.aborted).toBe(true))
    expect(await screen.findByText('第二次结果')).toBeInTheDocument()
  })

  it('shows a retry action for page failures', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(jsonResponse({
        success: false,
        code: 56999,
        status: 'DDC_INTERNAL_FAILURE',
        message: '加载失败',
        data: null,
        traceId: 'error-trace',
        timestamp: 1,
      }))
      .mockResolvedValueOnce(jsonResponse(pageRecord([biz()], 1)))

    renderWithQueryClient(<BizsPage />)

    fireEvent.click(await screen.findByRole('button', { name: /重\s*试/ }))
    expect(await screen.findByText('支付业务域')).toBeInTheDocument()
    expect(fetch).toHaveBeenCalledTimes(2)
  })
})
