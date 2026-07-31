import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../api/client'
import AppsPage from './AppsPage'

const record = (data: unknown) => ({
  success: true, code: 0, status: 'SUCCESS', message: '', data, traceId: 't', timestamp: 1,
})

const jsonResponse = (body: unknown) =>
  new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })

describe('AppsPage', () => {
  beforeEach(() => {
    setDdcTokenProvider(() => 'token')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('renders apps and creates a new one', async () => {
    vi.mocked(fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/apps') && !url.includes('/namespaces')) {
        return Promise.resolve(jsonResponse(record([{
          id: 'a1', appCode: 'orders', appName: '订单服务', owner: 'ops', description: '',
          enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z',
        }])))
      }
      return Promise.resolve(jsonResponse(record([])))
    })

    render(<AppsPage />)
    await waitFor(() => expect(screen.getByText('订单服务')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: /新\s*建\s*应\s*用/ }))
    fireEvent.change(screen.getByLabelText('应用编码'), { target: { value: 'billing' } })
    fireEvent.change(screen.getByLabelText('应用名称'), { target: { value: 'billing' } })
    fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }))

    await waitFor(() => {
      const calls = vi.mocked(fetch).mock.calls
      const create = calls.find(([url, init]) => String(url).includes('/apps') && init?.method === 'POST')
      expect(create).toBeDefined()
      const body = JSON.parse(String(create![1]?.body))
      expect(body).toMatchObject({ appCode: 'billing', appName: 'billing', owner: 'local-admin', enabled: true })
    })
  })
})
