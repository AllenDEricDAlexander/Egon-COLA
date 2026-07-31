import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../api/client'
import BizsPage from './BizsPage'

const record = (data: unknown) => ({
  success: true, code: 0, status: 'SUCCESS', message: '', data, traceId: 't', timestamp: 1,
})

const jsonResponse = (body: unknown) =>
  new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })

describe('BizsPage', () => {
  beforeEach(() => {
    setDdcTokenProvider(() => 'token')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('renders bizs and creates a new one', async () => {
    vi.mocked(fetch).mockImplementation((input, init) => {
      const url = String(input)
      if (url.includes('/bizs') && init?.method === 'POST') {
        const body = JSON.parse(String(init.body))
        expect(body).toMatchObject({ bizCode: 'risk-biz', bizName: '风控业务域', enabled: true })
        return Promise.resolve(jsonResponse(record({ id: 'b2', ...body, createdAt: '2026-07-02T00:00:00Z', updatedAt: '2026-07-02T00:00:00Z' })))
      }
      if (url.includes('/bizs')) {
        return Promise.resolve(jsonResponse(record([{
          id: 'b1', bizCode: 'pay-biz', bizName: '支付业务域', description: '',
          enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z',
        }])))
      }
      return Promise.resolve(jsonResponse(record(null)))
    })

    render(<BizsPage />)
    await waitFor(() => expect(screen.getByText('支付业务域')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: /新\s*建\s*业\s*务\s*域/ }))
    fireEvent.change(screen.getByLabelText('业务域编码'), { target: { value: 'risk-biz' } })
    fireEvent.change(screen.getByLabelText('名称'), { target: { value: '风控业务域' } })
    fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }))

    await waitFor(() => {
      const create = vi.mocked(fetch).mock.calls.find(([url, init]) => String(url).includes('/bizs') && init?.method === 'POST')
      expect(create).toBeDefined()
    })
  })
})
