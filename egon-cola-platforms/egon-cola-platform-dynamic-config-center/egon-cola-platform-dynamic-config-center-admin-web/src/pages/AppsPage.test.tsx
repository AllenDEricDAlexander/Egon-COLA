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

  it('renders apps with biz and creates a new one', async () => {
    vi.mocked(fetch).mockImplementation((input, init) => {
      const url = String(input)
      if (url.includes('/bizs')) {
        return Promise.resolve(jsonResponse(record([
          { id: 'b1', bizCode: 'pay-biz', bizName: '支付业务域', description: '', enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z' },
        ])))
      }
      if (url.includes('/apps') && init?.method === 'POST') {
        const body = JSON.parse(String(init.body))
        expect(body).toMatchObject({ appCode: 'billing', bizCode: 'pay-biz', appName: 'billing', enabled: true })
        return Promise.resolve(jsonResponse(record({ id: 'a2', ...body, createdAt: '2026-07-02T00:00:00Z', updatedAt: '2026-07-02T00:00:00Z' })))
      }
      if (url.includes('/apps')) {
        return Promise.resolve(jsonResponse(record([{
          id: 'a1', appCode: 'orders', bizCode: 'pay-biz', appName: '订单服务', owner: 'ops', description: '',
          enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z',
        }])))
      }
      return Promise.resolve(jsonResponse(record(null)))
    })

    render(<AppsPage />)
    await waitFor(() => expect(screen.getByText('订单服务')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: /新\s*建\s*应\s*用/ }))
    // 业务域下拉：输入路径（jsdom 环境限制）
    const scopeInputs = () => Array.from(document.querySelectorAll('.ant-modal input.ant-select-input')) as HTMLInputElement[]
    fireEvent.change(scopeInputs()[0], { target: { value: 'pay-biz' } })
    fireEvent.keyDown(scopeInputs()[0], { key: 'Enter', code: 'Enter', keyCode: 13 })
    fireEvent.change(screen.getByLabelText('应用编码'), { target: { value: 'billing' } })
    fireEvent.change(screen.getByLabelText('应用名称'), { target: { value: 'billing' } })
    fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }))

    await waitFor(() => {
      const create = vi.mocked(fetch).mock.calls.find(([url, init]) => String(url).includes('/apps') && init?.method === 'POST')
      expect(create).toBeDefined()
    })
  })
})
