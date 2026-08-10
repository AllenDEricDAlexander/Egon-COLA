import { fireEvent, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../api/client'
import { renderWithQueryClient } from '../test/renderWithQueryClient'
import EnvPage from './EnvPage'

const pageRecord = (total = 12) => ({
  success: true,
  code: 0,
  status: 'SUCCESS',
  message: '',
  records: [{
    id: 'e1',
    envCode: 'dev',
    description: '开发环境',
    sortOrder: 10,
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

describe('EnvPage', () => {
  beforeEach(() => {
    setDdcTokenProvider(() => 'token')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('submits and resets filters through the page route', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(pageRecord()))
    renderWithQueryClient(<EnvPage />)

    expect(await screen.findByText('开发环境')).toBeInTheDocument()
    expect(screen.getByText('共 12 条')).toBeInTheDocument()

    const keyword = screen.getByPlaceholderText('名称模糊查询')
    fireEvent.change(keyword, { target: { value: 'dev' } })
    fireEvent.click(screen.getByRole('button', { name: /查\s*询/ }))
    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      expect.stringMatching(/\/envs\/page\?.*keyword=dev.*pageNo=1/),
      expect.anything(),
    ))

    fireEvent.click(screen.getByRole('button', { name: /重\s*置/ }))
    await waitFor(() => {
      const latest = vi.mocked(fetch).mock.calls
        .filter(([input]) => String(input).includes('/envs/page'))
        .at(-1)
      expect(String(latest?.[0])).not.toContain('keyword=')
    })
  })

  it('retries the failed page query', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(jsonResponse({
        success: false,
        code: 56999,
        status: 'DDC_INTERNAL_FAILURE',
        message: '加载失败',
        data: null,
        timestamp: 1,
      }))
      .mockResolvedValueOnce(jsonResponse(pageRecord(1)))
    renderWithQueryClient(<EnvPage />)

    fireEvent.click(await screen.findByRole('button', { name: /重\s*试/ }))
    expect(await screen.findByText('开发环境')).toBeInTheDocument()
  })
})
