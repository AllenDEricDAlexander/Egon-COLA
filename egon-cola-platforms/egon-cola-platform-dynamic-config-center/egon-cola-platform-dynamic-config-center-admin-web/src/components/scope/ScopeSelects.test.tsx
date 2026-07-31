import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { useState } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../../api/client'
import { clearScopeOptionsCache } from './useScopeOptions'
import ScopeSelects, { type ScopeValue } from './ScopeSelects'

// 说明：jsdom + React 19 下 antd 下拉 portal 的选项点击事件无法送达 React（环境缺口，
// 真实浏览器无此问题）。交互测试统一走"输入 + Enter"路径，下拉选项只做渲染断言。

const record = (data: unknown) => ({
  success: true, code: 0, status: 'SUCCESS', message: '', data, traceId: 't', timestamp: 1,
})

const jsonResponse = (body: unknown) =>
  new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })

const appPayload = (code: string) => ({
  id: code, appCode: code, bizCode: 'pay-biz', appName: '', owner: 'ops', description: '', enabled: true,
  createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z',
})

// 渲染顺序固定：业务域(0) → 应用(1) → 命名空间(2) → 环境(3)
const scopeInputs = (): HTMLInputElement[] =>
  Array.from(document.querySelectorAll('input.ant-select-input')) as HTMLInputElement[]

const typeAndEnter = (input: HTMLInputElement, value: string): void => {
  fireEvent.change(input, { target: { value } })
  fireEvent.keyDown(input, { key: 'Enter', code: 'Enter', keyCode: 13 })
}

const mockScopeEndpoints = (bizs: string[], apps: string[], nss: string[], envs: string[]): void => {
  vi.mocked(fetch).mockImplementation((input) => {
    const url = String(input)
    if (url.includes('/bizs')) return Promise.resolve(jsonResponse(record(bizs.map((b) => ({ id: b, bizCode: b, bizName: b, description: '', enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z' })))))
    if (url.includes('/envs')) return Promise.resolve(jsonResponse(record(envs.map((e) => ({ id: e, envCode: e, description: e, sortOrder: 10, enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z' })))))
    if (url.includes('/apps')) return Promise.resolve(jsonResponse(record(apps.map(appPayload))))
    if (url.includes('/namespaces')) return Promise.resolve(jsonResponse(record(nss.map((n) => ({ id: n, appCode: 'orders-app', namespace: n, description: '', enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z' })))))
    return Promise.resolve(jsonResponse(record(null)))
  })
}

describe('ScopeSelects', () => {
  beforeEach(() => {
    clearScopeOptionsCache()
    setDdcTokenProvider(() => 'token')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('renders four level selects with cascade clearing', async () => {
    mockScopeEndpoints(['pay-biz'], ['orders-app'], ['default'], ['dev'])

    const value: ScopeValue = { bizCode: '', appCode: '', env: '', namespace: '' }
    const onChange = vi.fn((next: ScopeValue) => Object.assign(value, next))

    render(<ScopeSelects value={value} onChange={onChange} />)
    await waitFor(() => expect(screen.getAllByText('请选择或输入业务域').length).toBeGreaterThan(0))

    // 输入新业务域：清空 app 与 namespace
    typeAndEnter(scopeInputs()[0], 'pay-biz')
    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ bizCode: 'pay-biz', appCode: '', namespace: '' }))

    // 输入新应用：清空 namespace
    typeAndEnter(scopeInputs()[1], 'orders-app')
    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ appCode: 'orders-app', namespace: '' }))

    // 输入命名空间
    typeAndEnter(scopeInputs()[2], 'default')
    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ namespace: 'default' }))

    // 输入环境
    typeAndEnter(scopeInputs()[3], 'dev')
    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ env: 'dev' }))
  })

  it('loads apps filtered by selected biz and namespaces by app', async () => {
    mockScopeEndpoints(['pay-biz'], ['orders-app'], ['default'], ['dev'])

    render(<ScopeSelects value={{ bizCode: 'pay-biz', appCode: 'orders-app', env: '', namespace: '' }} onChange={() => {}} />)
    await waitFor(() => {
      const appsCall = vi.mocked(fetch).mock.calls
        .find(([url]) => String(url).includes('/apps') && String(url).includes('biz=pay-biz'))
      expect(appsCall).toBeDefined()
    })
    await waitFor(() => {
      const nsCall = vi.mocked(fetch).mock.calls
        .find(([url]) => String(url).includes('/namespaces') && String(url).includes('appCode=orders-app'))
      expect(nsCall).toBeDefined()
    })
  })

  it('clears app and namespace when biz changes', async () => {
    mockScopeEndpoints(['pay-biz', 'risk-biz'], ['orders-app'], ['default'], ['dev'])

    const onChange = vi.fn()
    const Harness = () => {
      const [value, setValue] = useState<ScopeValue>({ bizCode: 'pay-biz', appCode: 'orders-app', env: 'dev', namespace: 'default' })
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
    await waitFor(() => expect(screen.getAllByText(/pay-biz/).length).toBeGreaterThan(0))

    // 换业务域（maxCount=1 需先移除 tag 再输入）
    const removeButton = document.querySelector('.ant-select-selection-item-remove') as HTMLElement
    fireEvent.click(removeButton)
    typeAndEnter(scopeInputs()[0], 'risk-biz')
    expect(onChange).toHaveBeenLastCalledWith(expect.objectContaining({ bizCode: 'risk-biz', appCode: '', namespace: '' }))
  })
})
