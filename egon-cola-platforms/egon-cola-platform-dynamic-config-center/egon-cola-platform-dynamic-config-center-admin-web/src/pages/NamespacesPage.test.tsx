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

  it('creates a biz namespace and manages its env-app bindings', async () => {
    vi.mocked(fetch).mockImplementation((input, init) => {
      const url = String(input)
      if (url.includes('/bizs')) {
        return Promise.resolve(jsonResponse(record([
          { id: 'b1', bizCode: 'pay-biz', bizName: '支付业务域', description: '', enabled: true },
        ])))
      }
      if (url.includes('/envs')) {
        return Promise.resolve(jsonResponse(record([
          { id: 'e1', envCode: 'dev', description: '开发', sortOrder: 10, enabled: true },
        ])))
      }
      if (url.includes('/apps')) {
        return Promise.resolve(jsonResponse(record([
          { id: 'a1', appCode: 'orders', bizCode: 'pay-biz', appName: '订单服务', owner: 'ops', description: '', enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z' },
        ])))
      }
      if (url.includes('/namespace-env-app-bindings') && init?.method === 'POST') {
        const body = JSON.parse(String(init.body))
        expect(body).toMatchObject({
          bizCode: 'pay-biz', namespaceCode: 'ns-default', env: 'dev', appCode: 'orders', enabled: true,
        })
        return Promise.resolve(jsonResponse(record({ id: 'binding-1', ...body })))
      }
      if (url.includes('/namespace-env-app-bindings')) {
        return Promise.resolve(jsonResponse(record([])))
      }
      if (url.includes('/namespaces') && init?.method === 'POST') {
        const body = JSON.parse(String(init.body))
        expect(body).toMatchObject({ bizCode: 'pay-biz', namespaceCode: 'ns-primary', namespace: 'primary', enabled: true })
        expect(body).not.toHaveProperty('appCode')
        expect(body).not.toHaveProperty('env')
        return Promise.resolve(jsonResponse(record({ id: 'n2', ...body, createdAt: '2026-07-02T00:00:00Z', updatedAt: '2026-07-02T00:00:00Z' })))
      }
      if (url.includes('/namespaces')) {
        return Promise.resolve(jsonResponse(record([{
          id: 'n1', bizCode: 'pay-biz', namespaceCode: 'ns-default', namespace: 'default', description: '',
          enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z',
        }])))
      }
      return Promise.resolve(jsonResponse(record(null)))
    })

    render(<NamespacesPage />)
    await waitFor(() => expect(screen.getByText('default')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: /新\s*建\s*命\s*名\s*空\s*间/ }))
    const scopeInputs = () => Array.from(document.querySelectorAll('.ant-modal input.ant-select-input')) as HTMLInputElement[]
    fireEvent.change(scopeInputs()[0], { target: { value: 'pay-biz' } })
    fireEvent.keyDown(scopeInputs()[0], { key: 'Enter', code: 'Enter', keyCode: 13 })
    fireEvent.change(screen.getByLabelText('编码'), { target: { value: 'ns-primary' } })
    fireEvent.change(screen.getByLabelText('名称'), { target: { value: 'primary' } })
    fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }))

    await waitFor(() => {
      const create = vi.mocked(fetch).mock.calls.find(([url, init]) => String(url).includes('/namespaces') && init?.method === 'POST')
      expect(create).toBeDefined()
    })

    fireEvent.click(screen.getByRole('button', { name: /管\s*理\s*绑\s*定/ }))
    await waitFor(() => expect(screen.getByLabelText('orders（订单服务）')).toBeInTheDocument())
    fireEvent.click(screen.getByLabelText('orders（订单服务）'))
    fireEvent.click(screen.getByRole('button', { name: /保\s*存\s*绑\s*定/ }))
    await waitFor(() => {
      const createBinding = vi.mocked(fetch).mock.calls.find(([url, init]) =>
        String(url).includes('/namespace-env-app-bindings') && init?.method === 'POST')
      expect(createBinding).toBeDefined()
    })
  }, 15_000)
})
