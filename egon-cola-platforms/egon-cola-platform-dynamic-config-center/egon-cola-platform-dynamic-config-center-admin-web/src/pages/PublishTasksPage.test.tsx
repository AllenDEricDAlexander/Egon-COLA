import {act, fireEvent, screen, waitFor} from '@testing-library/react'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {setDdcUnauthorizedHandler} from '../api/client'
import {renderWithQueryClient} from '../test/renderWithQueryClient'
import PublishTasksPage from './PublishTasksPage'

const record = (data: unknown) => ({
  success: true,
  code: 0,
  status: 'SUCCESS',
  message: '',
  data,
  traceId: 't',
  timestamp: 1,
})

const pageRecord = (total = 12) => ({
  success: true,
  code: 0,
  status: 'SUCCESS',
  message: '',
  records: [task],
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

const failure = {
  success: false,
  code: 56999,
  status: 'DDC_INTERNAL_FAILURE',
  message: '发布任务加载失败',
  data: null,
  timestamp: 1,
}

const jsonResponse = (body: unknown) => new Response(
  JSON.stringify(body),
  { status: 200, headers: { 'Content-Type': 'application/json' } },
)

const task = {
  id: 't-1',
  changeId: 'change-9',
  configId: 'cfg-1',
  bizCode: 'pay-biz',
  appCode: 'orders',
  env: 'dev',
  resourceName: 'application.yml',
  targetVersion: 4,
  publishMode: 'SYNC',
  resourceChecksum: 'abc',
  attemptCount: 1,
  dispatchedAt: '2026-07-31T10:00:00Z',
  completedAt: '2026-07-31T10:00:05Z',
  failureStage: null,
  status: 'FAILED',
  targetCount: 3,
  ackCount: 2,
  failedCount: 1,
  ignoredCount: 0,
  timeoutCount: 0,
  timeoutMs: 30000,
  operator: 'local-admin',
  errorMessage: 'one failure',
  createdAt: '2026-07-31T10:00:00Z',
  updatedAt: '2026-07-31T10:00:05Z',
}

const mockEndpoints = (): void => {
  vi.mocked(fetch).mockImplementation((input, init) => {
    const url = String(input)
    if (url.includes('/publish-tasks/page')) {
      return Promise.resolve(jsonResponse(pageRecord()))
    }
    if (url.endsWith('/publish-tasks/change-9')) {
      return Promise.resolve(jsonResponse(record(task)))
    }
    if (url.includes('/retry') && init?.method === 'POST') {
      return Promise.resolve(jsonResponse(record({
        changeId: 'change-9',
        status: 'SUCCESS',
      })))
    }
    if (url.includes('/bizs') || url.includes('/namespaces')
        || url.includes('/envs') || url.includes('/apps')) {
      return Promise.resolve(jsonResponse(record([])))
    }
    return Promise.resolve(jsonResponse(record(null)))
  })
}

describe('PublishTasksPage', () => {
  beforeEach(() => {
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('pages filtered tasks and opens a viewport-safe detail modal', async () => {
    mockEndpoints()
    renderWithQueryClient(<PublishTasksPage />)

    expect(await screen.findByText('change-9')).toBeInTheDocument()
    expect(screen.getByText('共 12 条')).toBeInTheDocument()
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining(
        '/api/v1/ddc/publish-tasks/page?pageNo=1&pageSize=10',
      ),
      expect.anything(),
    )

    fireEvent.mouseDown(screen.getByLabelText('状态'))
    const failedOptions = await screen.findAllByText('FAILED')
    fireEvent.click(failedOptions[failedOptions.length - 1])
    fireEvent.click(screen.getByRole('button', { name: /查\s*询/ }))
    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      expect.stringMatching(/publish-tasks\/page\?.*status=FAILED/),
      expect.anything(),
    ))

    fireEvent.click(screen.getByRole('button', { name: 'change-9' }))
    await screen.findByText('one failure')
    expect(document.querySelector('.ant-modal'))
      .toHaveStyle({ width: 'calc(100vw - 24px)' })
  })

  it('keeps the current page visible when a background poll fails', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    let pageAttempts = 0
    vi.mocked(fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/publish-tasks/page')) {
        pageAttempts += 1
        return Promise.resolve(jsonResponse(
          pageAttempts === 1 ? pageRecord(1) : failure,
        ))
      }
      return Promise.resolve(jsonResponse(record([])))
    })
    renderWithQueryClient(<PublishTasksPage />)
    expect(await screen.findByText('change-9')).toBeInTheDocument()

    await act(async () => {
      await vi.advanceTimersByTimeAsync(15_000)
    })

    await waitFor(() => expect(pageAttempts).toBeGreaterThanOrEqual(2))
    expect(screen.getByText('change-9')).toBeInTheDocument()
    expect(screen.getAllByRole('alert')).toHaveLength(1)
  })

  it('retries only after the Ant Design confirmation', async () => {
    mockEndpoints()
    renderWithQueryClient(<PublishTasksPage />)
    await screen.findByText('change-9')

    fireEvent.click(screen.getByRole('button', { name: /重\s*试/ }))
    expect(await screen.findAllByText('确认重试任务 change-9？'))
      .not.toHaveLength(0)
    expect(vi.mocked(fetch).mock.calls.some(([input, init]) =>
      String(input).includes('/retry') && init?.method === 'POST')).toBe(false)

    const retryButtons = screen.getAllByRole('button', { name: /重\s*试/ })
    fireEvent.click(retryButtons[retryButtons.length - 1])
    await waitFor(() => expect(vi.mocked(fetch).mock.calls.some(([input, init]) =>
      String(input).includes('/retry') && init?.method === 'POST')).toBe(true))
  })
})
