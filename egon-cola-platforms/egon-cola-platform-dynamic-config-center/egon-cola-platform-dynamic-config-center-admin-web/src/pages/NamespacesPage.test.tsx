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
      if (url.includes('/namespaces/domains')) {
        return Promise.resolve(jsonResponse(record([])))
      }
      if (url.includes('/namespaces')) {
        return Promise.resolve(jsonResponse(record([{
          id: 'n1', appCode: 'orders', env: 'dev', namespace: 'default', description: '',
          enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z',
        }])))
      }
      if (url.includes('/apps')) {
        return Promise.resolve(jsonResponse(record([])))
      }
      return Promise.resolve(jsonResponse(record([])))
    })

    render(<NamespacesPage />)
    const scopeInputs = () =>
      Array.from(document.querySelectorAll('input.ant-select-input')) as HTMLInputElement[]
    const typeAndEnter = (input: HTMLInputElement, value: string) => {
      fireEvent.change(input, { target: { value } })
      fireEvent.keyDown(input, { key: 'Enter', code: 'Enter', keyCode: 13 })
    }
    typeAndEnter(scopeInputs()[0], 'orders')
    typeAndEnter(scopeInputs()[1], 'dev')
    fireEvent.click(screen.getByRole('button', { name: /查\s*询/ }))
    await waitFor(() => expect(screen.getByText('default')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: /新\s*建\s*命\s*名\s*空\s*间/ }))
    // 新建对话框：应用编码/环境为可选下拉（输入路径），命名空间手输
    const modalInputs = () =>
      Array.from(document.querySelectorAll('.ant-modal input.ant-select-input')) as HTMLInputElement[]
    typeAndEnter(modalInputs()[0], 'billing')
    typeAndEnter(modalInputs()[1], 'prod')
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
