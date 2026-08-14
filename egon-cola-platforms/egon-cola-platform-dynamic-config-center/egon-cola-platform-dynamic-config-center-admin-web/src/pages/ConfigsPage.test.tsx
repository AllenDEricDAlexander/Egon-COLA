import {fireEvent, screen, waitFor} from '@testing-library/react'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {setDdcUnauthorizedHandler} from '../api/client'
import {renderWithQueryClient} from '../test/renderWithQueryClient'
import ConfigsPage from './ConfigsPage'

const record = (data: unknown) => ({
  success: true,
  code: 0,
  status: 'SUCCESS',
  message: '',
  data,
  traceId: 't',
  timestamp: 1,
})

const pageRecord = <T,>(records: T[], total: number) => ({
  success: true,
  code: 0,
  status: 'SUCCESS',
  message: '',
  records,
  page: {
    total,
    pageNo: 1,
    pageSize: 10,
    pages: Math.ceil(total / 10),
    hasNext: total > 10,
    hasPrevious: false,
  },
  traceId: 't',
  timestamp: 1,
})

const jsonResponse = (body: unknown) => new Response(
  JSON.stringify(body),
  { status: 200, headers: { 'Content-Type': 'application/json' } },
)

const configRow = {
  id: 'cfg-1',
  bizCode: 'pay-biz',
  appCode: 'orders',
  env: 'dev',
  visibleNamespaces: ['default', 'ops'],
  resourceName: 'application.yml' as const,
  content: 'feature:\n  enabled: true\n',
  format: 'YAML' as const,
  currentVersion: 3,
  description: '业务配置',
  createdAt: '2026-07-01T00:00:00Z',
  updatedAt: '2026-07-02T00:00:00Z',
}

const versionRow = {
  id: 'v3',
  configId: 'cfg-1',
  version: 3,
  resourceName: 'application.yml' as const,
  newContent: 'feature:\n  enabled: true\n',
  format: 'YAML' as const,
  changeType: 'UPDATE',
  changeReason: 'enable feature',
  operator: 'mario',
  createdAt: '2026-07-02T00:00:00Z',
}

const mockConfigEndpoints = (): void => {
  vi.mocked(fetch).mockImplementation((input, init) => {
    const url = String(input)
    if (url.includes('/configs/cfg-1/versions/page')) {
      return Promise.resolve(jsonResponse(pageRecord([versionRow], 11)))
    }
    if (url.includes('/configs/page')) {
      return Promise.resolve(jsonResponse(pageRecord([configRow], 13)))
    }
    if (url.includes('/bizs') || url.includes('/namespaces')
        || url.includes('/envs') || url.includes('/apps')) {
      return Promise.resolve(jsonResponse(record([])))
    }
    if (url.includes('/publish') && init?.method === 'POST') {
      return Promise.resolve(jsonResponse(record({
        changeId: 'change-1',
        status: 'SUCCESS',
      })))
    }
    return Promise.resolve(jsonResponse(record(null)))
  })
}

const clickLastButton = (name: RegExp): void => {
  const buttons = screen.getAllByRole('button', { name })
  fireEvent.click(buttons[buttons.length - 1])
}

describe('ConfigsPage', () => {
  beforeEach(() => {
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('pages configs and lazily pages version history', async () => {
    mockConfigEndpoints()
    renderWithQueryClient(<ConfigsPage />)

    expect(await screen.findByText('业务配置')).toBeInTheDocument()
    expect(screen.getByText('共 13 条')).toBeInTheDocument()
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining(
        '/api/v1/ddc/configs/page?includeDeleted=false&pageNo=1&pageSize=10',
      ),
      expect.anything(),
    )
    expect(vi.mocked(fetch).mock.calls.some(([input]) =>
      String(input).includes('/versions/page'))).toBe(false)

    fireEvent.click(screen.getByRole('button', { name: /更\s*多\s*操\s*作/ }))
    fireEvent.click(await screen.findByRole('menuitem', { name: /查\s*看\s*版\s*本/ }))

    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining(
        '/api/v1/ddc/configs/cfg-1/versions/page?pageNo=1&pageSize=10',
      ),
      expect.anything(),
    ))
    expect(await screen.findByText('enable feature')).toBeInTheDocument()
    expect(screen.getByText('共 11 条')).toBeInTheDocument()
  })

  it('publishes only after the Ant Design confirmation', async () => {
    mockConfigEndpoints()
    renderWithQueryClient(<ConfigsPage />)
    await screen.findByText('业务配置')

    fireEvent.click(screen.getByRole('button', { name: /发\s*布/ }))
    expect(await screen.findAllByText('确认发布 application.yml 当前版本？'))
      .not.toHaveLength(0)
    expect(vi.mocked(fetch).mock.calls.some(([input, init]) =>
      String(input).includes('/publish') && init?.method === 'POST')).toBe(false)

    clickLastButton(/发\s*布/)
    await waitFor(() => expect(vi.mocked(fetch).mock.calls.some(([input, init]) =>
      String(input).includes('/publish') && init?.method === 'POST')).toBe(true))
  })

  it('deletes and rolls back only after confirmation', async () => {
    mockConfigEndpoints()
    renderWithQueryClient(<ConfigsPage />)
    await screen.findByText('业务配置')

    fireEvent.click(screen.getByRole('button', { name: /更\s*多\s*操\s*作/ }))
    fireEvent.click(await screen.findByRole('menuitem', { name: /删\s*除/ }))
    expect(await screen.findAllByText('确认删除 application.yml？'))
      .not.toHaveLength(0)
    expect(vi.mocked(fetch).mock.calls.some(([input, init]) =>
      String(input) === '/api/v1/ddc/configs/cfg-1'
      && init?.method === 'DELETE')).toBe(false)
    clickLastButton(/删\s*除/)
    await waitFor(() => expect(vi.mocked(fetch).mock.calls.some(([input, init]) =>
      String(input) === '/api/v1/ddc/configs/cfg-1'
      && init?.method === 'DELETE')).toBe(true))

    fireEvent.click(screen.getByRole('button', { name: /更\s*多\s*操\s*作/ }))
    fireEvent.click(await screen.findByRole('menuitem', { name: /查\s*看\s*版\s*本/ }))
    await screen.findByText('enable feature')
    fireEvent.click(screen.getByRole('button', { name: /回\s*滚/ }))
    expect(await screen.findAllByText('确认回滚到版本 3？'))
      .not.toHaveLength(0)
    expect(vi.mocked(fetch).mock.calls.some(([input, init]) =>
      String(input).includes('/rollback') && init?.method === 'POST')).toBe(false)
    clickLastButton(/回\s*滚/)
    await waitFor(() => expect(vi.mocked(fetch).mock.calls.some(([input, init]) =>
      String(input).includes('/rollback') && init?.method === 'POST')).toBe(true))
  })
})
