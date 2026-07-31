import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../api/client'
import EnvPage from './EnvPage'

const record = (data: unknown) => ({
  success: true, code: 0, status: 'SUCCESS', message: '', data, traceId: 't', timestamp: 1,
})

const jsonResponse = (body: unknown) =>
  new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })

describe('EnvPage', () => {
  beforeEach(() => {
    setDdcTokenProvider(() => 'token')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('renders envs and creates a new one', async () => {
    vi.mocked(fetch).mockImplementation((input, init) => {
      const url = String(input)
      if (url.includes('/envs') && init?.method === 'POST') {
        const body = JSON.parse(String(init.body))
        expect(body).toMatchObject({ envCode: 'pre', sortOrder: 60, enabled: true })
        return Promise.resolve(jsonResponse(record({ id: 'e2', ...body, createdAt: '2026-07-02T00:00:00Z', updatedAt: '2026-07-02T00:00:00Z' })))
      }
      if (url.includes('/envs')) {
        return Promise.resolve(jsonResponse(record([{
          id: 'e1', envCode: 'dev', description: '开发环境', sortOrder: 10,
          enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z',
        }])))
      }
      return Promise.resolve(jsonResponse(record(null)))
    })

    render(<EnvPage />)
    await waitFor(() => expect(screen.getByText('开发环境')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: /新\s*建\s*环\s*境/ }))
    fireEvent.change(screen.getByLabelText('环境编码'), { target: { value: 'pre' } })
    fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }))

    await waitFor(() => {
      const create = vi.mocked(fetch).mock.calls.find(([url, init]) => String(url).includes('/envs') && init?.method === 'POST')
      expect(create).toBeDefined()
    })
  })
})
