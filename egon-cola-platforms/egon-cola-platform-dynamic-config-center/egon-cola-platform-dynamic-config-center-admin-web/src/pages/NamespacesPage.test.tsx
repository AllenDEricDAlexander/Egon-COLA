import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../api/client'
import { clearScopeOptionsCache } from '../components/scope/useScopeOptions'
import NamespacesPage from './NamespacesPage'

const record = (data: unknown) => ({
  success: true, code: 0, status: 'SUCCESS', message: '', data, traceId: 't', timestamp: 1,
})

const jsonResponse = (body: unknown) =>
  new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })

describe('NamespacesPage', () => {
  beforeEach(() => {
    clearScopeOptionsCache()
    setDdcTokenProvider(() => 'token')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('renders namespaces and creates one without env', async () => {
    vi.mocked(fetch).mockImplementation((input, init) => {
      const url = String(input)
      if (url.includes('/apps')) {
        return Promise.resolve(jsonResponse(record([
          { id: 'a1', appCode: 'orders', bizCode: 'pay-biz', appName: '', owner: 'ops', description: '', enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z' },
        ])))
      }
      if (url.includes('/namespaces') && init?.method === 'POST') {
        const body = JSON.parse(String(init.body))
        expect(body).toMatchObject({ appCode: 'orders', namespaceCode: 'ns-primary', namespace: 'primary', enabled: true })
        expect(body).not.toHaveProperty('env')
        return Promise.resolve(jsonResponse(record({ id: 'n2', ...body, createdAt: '2026-07-02T00:00:00Z', updatedAt: '2026-07-02T00:00:00Z' })))
      }
      if (url.includes('/namespaces')) {
        return Promise.resolve(jsonResponse(record([{
          id: 'n1', appCode: 'orders', namespaceCode: 'ns-default', namespace: 'default', description: '',
          enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z',
        }])))
      }
      return Promise.resolve(jsonResponse(record(null)))
    })

    render(<NamespacesPage />)
    await waitFor(() => expect(screen.getByText('default')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: /新\s*建\s*命\s*名\s*空\s*间/ }))
    const scopeInputs = () => Array.from(document.querySelectorAll('.ant-modal input.ant-select-input')) as HTMLInputElement[]
    fireEvent.change(scopeInputs()[0], { target: { value: 'orders' } })
    fireEvent.keyDown(scopeInputs()[0], { key: 'Enter', code: 'Enter', keyCode: 13 })
    fireEvent.change(screen.getByLabelText('编码'), { target: { value: 'ns-primary' } })
    fireEvent.change(screen.getByLabelText('名称'), { target: { value: 'primary' } })
    fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }))

    await waitFor(() => {
      const create = vi.mocked(fetch).mock.calls.find(([url, init]) => String(url).includes('/namespaces') && init?.method === 'POST')
      expect(create).toBeDefined()
    })
  })
})
