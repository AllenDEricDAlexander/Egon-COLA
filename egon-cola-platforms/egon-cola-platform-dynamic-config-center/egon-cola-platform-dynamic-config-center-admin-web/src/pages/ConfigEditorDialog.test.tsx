import {fireEvent, screen, waitFor} from '@testing-library/react'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {setDdcUnauthorizedHandler} from '../api/client'
import type {DdcConfig} from '../api/types'
import {renderWithQueryClient} from '../test/renderWithQueryClient'
import ConfigEditorDialog from './ConfigEditorDialog'

vi.mock('../components/scope/ScopeSelects', () => ({
  default: ({ onChange, disabled }: {
    onChange: (scope: { bizCode: string; namespaceCode: string; env: string; appCode: string }) => void
    disabled?: boolean
  }) => (
    <button
      type="button"
      disabled={disabled}
      onClick={() => onChange({ bizCode: 'pay', namespaceCode: 'default', env: 'dev', appCode: 'orders' })}
    >
      选择测试作用域
    </button>
  ),
}))

const record = (data: unknown) => ({
  success: true, code: 0, status: 'SUCCESS', message: '', data, traceId: 't', timestamp: 1,
})

const jsonResponse = (body: unknown) =>
  new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })

const config: DdcConfig = {
  id: 'cfg-1', bizCode: 'pay', appCode: 'orders', env: 'dev', visibleNamespaces: ['default'],
  resourceName: 'application.yml', content: 'feature:\n  enabled: true\n', format: 'YAML',
  currentVersion: 3, description: '业务配置', updatedAt: '2026-08-08T00:00:00Z',
}

describe('ConfigEditorDialog', () => {
  beforeEach(() => {
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('creates one YAML document without legacy key, type, or default fields', async () => {
    const onSaved = vi.fn()
    vi.mocked(fetch).mockImplementation((input) => {
      if (String(input).includes('/namespace-env-app-bindings')) {
        return Promise.resolve(jsonResponse(record([{ enabled: true }])))
      }
      return Promise.resolve(jsonResponse(record(config)))
    })

    renderWithQueryClient(
      <ConfigEditorDialog
        open
        config={null}
        defaultScope={{ bizCode: '', namespaceCode: '', env: '', appCode: '' }}
        onClose={() => {}}
        onSaved={onSaved}
      />,
    )

    expect(await screen.findByText('新建 application.yml')).toBeInTheDocument()
    expect(screen.queryByText('配置 Key')).not.toBeInTheDocument()
    expect(screen.queryByText('值类型')).not.toBeInTheDocument()
    expect(screen.queryByText('默认值')).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '选择测试作用域' }))
    fireEvent.change(screen.getByLabelText('YAML 内容'), { target: { value: 'feature:\n  enabled: false\n' } })
    fireEvent.change(screen.getByLabelText('描述'), { target: { value: '支付业务配置' } })
    fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }))

    await waitFor(() => expect(onSaved).toHaveBeenCalledOnce())
    const create = vi.mocked(fetch).mock.calls.find(([input, init]) =>
      String(input) === '/api/v1/ddc/configs' && init?.method === 'POST')
    expect(create).toBeDefined()
    expect(JSON.parse(String(create?.[1]?.body))).toEqual({
      bizCode: 'pay', namespaceCode: 'default', env: 'dev', appCode: 'orders',
      resourceName: 'application.yml', content: 'feature:\n  enabled: false\n', format: 'YAML',
      description: '支付业务配置',
    })
  })

  it('updates YAML with the optimistic current version', async () => {
    const onSaved = vi.fn()
    vi.mocked(fetch).mockResolvedValue(jsonResponse(record(config)))

    renderWithQueryClient(
      <ConfigEditorDialog
        open
        config={config}
        defaultScope={{ bizCode: '', namespaceCode: '', env: '', appCode: '' }}
        onClose={() => {}}
        onSaved={onSaved}
      />,
    )

    const editor = await screen.findByLabelText('YAML 内容')
    await waitFor(() => expect(editor).toHaveValue(config.content))
    fireEvent.change(editor, { target: { value: 'feature:\n  enabled: false\n' } })
    fireEvent.change(screen.getByLabelText('变更原因'), { target: { value: '关闭功能' } })
    fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }))

    await waitFor(() => expect(onSaved).toHaveBeenCalledOnce())
    const update = vi.mocked(fetch).mock.calls.find(([input, init]) =>
      String(input) === '/api/v1/ddc/configs/cfg-1' && init?.method === 'PUT')
    expect(JSON.parse(String(update?.[1]?.body))).toEqual({
      content: 'feature:\n  enabled: false\n',
      changeReason: '关闭功能',
      currentVersion: 3,
    })
  })

  it('uses a viewport-safe width on narrow screens', async () => {
    renderWithQueryClient(
      <ConfigEditorDialog
        open
        config={config}
        defaultScope={{ bizCode: '', namespaceCode: '', env: '', appCode: '' }}
        onClose={() => {}}
        onSaved={() => {}}
      />,
    )

    await screen.findByText('编辑 application.yml')
    expect(document.querySelector('.ant-modal'))
      .toHaveStyle({ width: 'calc(100vw - 24px)' })
  })
})
