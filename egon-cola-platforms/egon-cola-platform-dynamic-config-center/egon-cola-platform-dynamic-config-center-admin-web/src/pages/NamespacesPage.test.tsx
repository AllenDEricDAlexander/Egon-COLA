import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../api/client'
import NamespacesPage from './NamespacesPage'

const record = (data: unknown) => ({
  success: true, code: 0, status: 'SUCCESS', message: '', data, traceId: 't', timestamp: 1,
})

const jsonResponse = (body: unknown) =>
  new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })

describe('NamespacesPage', () => {
  beforeEach(() => {
    setDdcTokenProvider(() => 'token')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('renders namespaces and creates a new one', async () => {
    vi.mocked(fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/namespaces')) {
        return Promise.resolve(jsonResponse(record([{
          id: 'n1', appCode: 'orders', env: 'dev', namespace: 'default', description: '',
          enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z',
        }])))
      }
      return Promise.resolve(jsonResponse(record([])))
    })

    render(<NamespacesPage />)
    fireEvent.change(screen.getByPlaceholderText('appCode'), { target: { value: 'orders' } })
    fireEvent.change(screen.getByPlaceholderText('env'), { target: { value: 'dev' } })
    fireEvent.click(screen.getByRole('button', { name: /查\s*询/ }))
    await waitFor(() => expect(screen.getByText('orders')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: /新\s*建\s*命\s*名\s*空\s*间/ }))
    fireEvent.change(screen.getByLabelText('应用编码'), { target: { value: 'billing' } })
    fireEvent.change(screen.getByLabelText('环境'), { target: { value: 'prod' } })
    fireEvent.change(screen.getByLabelText('命名空间'), { target: { value: 'primary' } })
    fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }))

    await waitFor(() => {
      const calls = vi.mocked(fetch).mock.calls
      const create = calls.find(([url, init]) => String(url).includes('/namespaces') && init?.method === 'POST')
      expect(create).toBeDefined()
      const body = JSON.parse(String(create![1]?.body))
      expect(body).toMatchObject({ appCode: 'billing', env: 'prod', namespace: 'primary', enabled: true })
    })
  })
})
