import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { useState } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../../api/client'
import { clearScopeOptionsCache } from './useScopeOptions'
import ScopeSelects, { type ScopeValue } from './ScopeSelects'

// 说明：jsdom + React 19 下 antd 下拉 portal 的选项点击事件无法送达 React（环境缺口，
// 真实浏览器无此问题）。交互测试统一走"输入 + Enter"路径（即可输入新值兜底的用户路径），
// 下拉选项只做渲染断言。

const record = (data: unknown) => ({
  success: true, code: 0, status: 'SUCCESS', message: '', data, traceId: 't', timestamp: 1,
})

const jsonResponse = (body: unknown) =>
  new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })

const appPayload = (code: string) => ({
  id: code, appCode: code, appName: '', owner: 'ops', description: '', enabled: true,
  createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z',
})

// ScopeSelects 渲染顺序固定：业务域(0) → 应用(1) → 环境(2)
const scopeInputs = (): HTMLInputElement[] =>
  Array.from(document.querySelectorAll('input.ant-select-input')) as HTMLInputElement[]

const typeAndEnter = (input: HTMLInputElement, value: string): void => {
  fireEvent.change(input, { target: { value } })
  fireEvent.keyDown(input, { key: 'Enter', code: 'Enter', keyCode: 13 })
}

describe('ScopeSelects', () => {
  beforeEach(() => {
    clearScopeOptionsCache()
    setDdcTokenProvider(() => 'token')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('renders domain options and propagates typed values with cascade', async () => {
    vi.mocked(fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/namespaces/domains')) {
        return Promise.resolve(jsonResponse(record(['orders'])))
      }
      if (url.includes('/apps')) {
        return Promise.resolve(jsonResponse(record([appPayload('orders-app')])))
      }
      return Promise.resolve(jsonResponse(record(null)))
    })

    const value: ScopeValue = { appCode: '', env: '', namespace: '' }
    const onChange = vi.fn((next: ScopeValue) => Object.assign(value, next))

    render(<ScopeSelects value={value} onChange={onChange} />)
    await waitFor(() => expect(screen.getAllByText('请选择或输入业务域').length).toBeGreaterThan(0))

    // 打开业务域下拉：选项渲染断言
    fireEvent.mouseDown(screen.getAllByText('请选择或输入业务域')[0])
    await waitFor(() => expect(screen.getByRole('option', { name: 'orders' })).toBeInTheDocument())
    fireEvent.keyDown(scopeInputs()[0], { key: 'Escape', code: 'Escape' })

    // 输入新业务域：onChange 携带 namespace，并清空 appCode
    typeAndEnter(scopeInputs()[0], 'new-domain')
    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ namespace: 'new-domain', appCode: '' }))

    // 输入新应用：onChange 携带 appCode
    typeAndEnter(scopeInputs()[1], 'new-app')
    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ appCode: 'new-app' }))

    // 输入新环境：onChange 携带 env
    typeAndEnter(scopeInputs()[2], 'prod')
    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ env: 'prod' }))
  })

  it('loads apps without namespace filter when no domain selected', async () => {
    vi.mocked(fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/namespaces/domains')) {
        return Promise.resolve(jsonResponse(record([])))
      }
      if (url.includes('/apps')) {
        return Promise.resolve(jsonResponse(record([appPayload('standalone')])))
      }
      return Promise.resolve(jsonResponse(record(null)))
    })

    const value: ScopeValue = { appCode: '', env: '', namespace: '' }
    const onChange = vi.fn((next: ScopeValue) => Object.assign(value, next))

    render(<ScopeSelects value={value} onChange={onChange} />)
    await waitFor(() => {
      const appsCall = vi.mocked(fetch).mock.calls.find(([url]) => String(url).includes('/apps'))
      expect(appsCall).toBeDefined()
      expect(String(appsCall![0])).not.toContain('namespace=')
    })
  })

  it('clears the app when the domain changes', async () => {
    vi.mocked(fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/namespaces/domains')) {
        return Promise.resolve(jsonResponse(record(['orders', 'billing'])))
      }
      if (url.includes('/apps')) {
        return Promise.resolve(jsonResponse(record([appPayload('orders-app')])))
      }
      return Promise.resolve(jsonResponse(record(null)))
    })

    const onChange = vi.fn()
    const Harness = () => {
      const [value, setValue] = useState<ScopeValue>({ appCode: 'orders-app', env: 'dev', namespace: 'orders' })
      return (
        <ScopeSelects
          value={value}
          onChange={(next) => {
            setValue(next)
            onChange(next)
          }}
        />
      )
    }

    render(<Harness />)
    await waitFor(() => expect(screen.getAllByText(/orders/).length).toBeGreaterThan(0))

    // 换业务域：maxCount=1 时需先移除已选值，再输入新值；appCode 必须被清空
    const removeButton = document.querySelector('.ant-select-selection-item-remove') as HTMLElement
    fireEvent.click(removeButton)
    typeAndEnter(scopeInputs()[0], 'billing')
    expect(onChange).toHaveBeenLastCalledWith(expect.objectContaining({ namespace: 'billing', appCode: '' }))
  })
})
