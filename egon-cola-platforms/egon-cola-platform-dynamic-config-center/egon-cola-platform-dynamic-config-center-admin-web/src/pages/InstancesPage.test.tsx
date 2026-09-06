import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcUnauthorizedHandler } from '../api/client'
import { renderWithQueryClient } from '../test/renderWithQueryClient'
import InstancesPage from './InstancesPage'

const record = (data: unknown) => ({
  success: true,
  code: 0,
  status: 'SUCCESS',
  message: '',
  data,
  traceId: 'trace-scope',
  timestamp: 1,
})

const pageRecord = (records: unknown[], total: number, pageNo = 1) => ({
  success: true,
  code: 0,
  status: 'SUCCESS',
  message: '',
  records,
  page: {
    total,
    pageNo,
    pageSize: 10,
    pages: Math.ceil(total / 10),
    hasNext: pageNo < Math.ceil(total / 10),
    hasPrevious: pageNo > 1,
  },
  traceId: 'trace-instances',
  timestamp: 1,
})

const jsonResponse = (body: unknown, status = 200): Response => new Response(
  JSON.stringify(body),
  { status, headers: { 'Content-Type': 'application/json' } },
)

const instance = {
  id: 'db-instance-1',
  instanceId: 'instance-1',
  bizCode: 'pay-biz',
  appCode: 'orders',
  env: 'prod',
  host: '10.0.0.8',
  port: 8080,
  pid: '1008',
  sdkVersion: '5.3.2',
  leaseId: 'lease-1',
  leaseExpireAt: '2026-08-27T04:00:00Z',
  status: 'ONLINE',
  lastHeartbeatAt: '2026-08-27T03:59:00Z',
  createdAt: '2026-08-27T03:00:00Z',
  updatedAt: '2026-08-27T03:59:00Z',
  runtimeMetadata: { zone: 'az-a' },
}

const scopeInputs = (): HTMLInputElement[] =>
  Array.from(document.querySelectorAll('input.ant-select-input')) as HTMLInputElement[]

const typeAndEnter = (input: HTMLInputElement, value: string): void => {
  fireEvent.change(input, { target: { value } })
  fireEvent.keyDown(input, { key: 'Enter', code: 'Enter', keyCode: 13 })
}

const fillCompleteScope = async (): Promise<void> => {
  await waitFor(() => expect(scopeInputs()).toHaveLength(4))
  typeAndEnter(scopeInputs()[0], 'pay-biz')
  typeAndEnter(scopeInputs()[1], 'default')
  typeAndEnter(scopeInputs()[2], 'prod')
  typeAndEnter(scopeInputs()[3], 'orders')
}

const mockScopeEndpoints = (
  instances: (url: string) => Response | Promise<Response>,
): void => {
  vi.mocked(fetch).mockImplementation((input) => {
    const url = String(input)
    if (url.includes('/instances/page')) return Promise.resolve(instances(url))
    if (url.includes('/bizs')) return Promise.resolve(jsonResponse(record([
      { bizCode: 'pay-biz', bizName: '支付域' },
    ])))
    if (url.includes('/namespaces')) return Promise.resolve(jsonResponse(record([
      { namespaceCode: 'default', namespace: '默认', bizCode: 'pay-biz' },
    ])))
    if (url.includes('/envs')) return Promise.resolve(jsonResponse(record([
      { envCode: 'prod', description: '生产环境' },
    ])))
    if (url.includes('/apps')) return Promise.resolve(jsonResponse(record([
      { appCode: 'orders', appName: '订单应用', bizCode: 'pay-biz' },
    ])))
    return Promise.resolve(jsonResponse(record(null)))
  })
}

beforeEach(() => {
  setDdcUnauthorizedHandler(() => {})
  vi.stubGlobal('fetch', vi.fn())
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('InstancesPage', () => {
  it.each([null, undefined])('shows the host when a config client does not report a port (%s)', async (port) => {
    mockScopeEndpoints(() => jsonResponse(pageRecord([{ ...instance, port }], 1)))
    renderWithQueryClient(<InstancesPage />)
    await fillCompleteScope()
    fireEvent.click(screen.getByRole('button', { name: /查\s*询/ }))
    expect(await screen.findByText('10.0.0.8')).toBeInTheDocument()
    expect(screen.queryByText('10.0.0.8:null')).not.toBeInTheDocument()
    expect(screen.queryByText('10.0.0.8:undefined')).not.toBeInTheDocument()
  })

  it('requests instances with submitted scope and preserves server page metadata', async () => {
    mockScopeEndpoints(() => jsonResponse(pageRecord([instance], 21)))
    renderWithQueryClient(<InstancesPage />)

    await fillCompleteScope()
    fireEvent.click(screen.getByRole('button', { name: /查\s*询/ }))

    expect(await screen.findByText('instance-1')).toBeInTheDocument()
    expect(screen.getByText('共 21 条')).toBeInTheDocument()
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining(
        '/api/v1/ddc/instances/page?bizCode=pay-biz&env=prod&appCode=orders&pageNo=1&pageSize=10',
      ),
      expect.anything(),
    )
    expect(screen.getByText('10.0.0.8:8080')).toBeInTheDocument()
  })

  it('resets to the first server page when the scope is submitted again', async () => {
    mockScopeEndpoints((url) => {
      const pageNo = Number(new URL(url, 'http://ddc.test').searchParams.get('pageNo') ?? '1')
      return jsonResponse(pageRecord([instance], 21, pageNo))
    })
    renderWithQueryClient(<InstancesPage />)

    await fillCompleteScope()
    fireEvent.click(screen.getByRole('button', { name: /查\s*询/ }))
    await screen.findByText('instance-1')

    fireEvent.click(screen.getByTitle('2'))
    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('pageNo=2&pageSize=10'),
      expect.anything(),
    ))

    fireEvent.click(screen.getByRole('button', { name: /查\s*询/ }))
    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('pageNo=1&pageSize=10'),
      expect.anything(),
    ))
  })

  it('shows a retryable error without dropping the submitted scope', async () => {
    let attempts = 0
    mockScopeEndpoints(() => {
      attempts += 1
      if (attempts === 1) {
        return jsonResponse({
          success: false,
          code: 500,
          status: 'DDC_INTERNAL_FAILURE',
          message: '实例列表加载失败',
          data: null,
          traceId: 'trace-instances-error',
          timestamp: 1,
        }, 500)
      }
      return jsonResponse(pageRecord([instance], 1))
    })
    renderWithQueryClient(<InstancesPage />)

    await fillCompleteScope()
    fireEvent.click(screen.getByRole('button', { name: /查\s*询/ }))
    fireEvent.click(await screen.findByRole('button', { name: /重\s*试/ }))

    expect(await screen.findByText('instance-1')).toBeInTheDocument()
    expect(attempts).toBe(2)
  })
})
