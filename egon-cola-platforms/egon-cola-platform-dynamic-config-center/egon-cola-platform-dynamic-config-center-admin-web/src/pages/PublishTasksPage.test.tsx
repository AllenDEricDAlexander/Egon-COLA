import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../api/client'
import PublishTasksPage from './PublishTasksPage'

const record = (data: unknown) => ({
  success: true, code: 0, status: 'SUCCESS', message: '', data, traceId: 't', timestamp: 1,
})

const jsonResponse = (body: unknown) =>
  new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })

const task = {
  id: 't-1', changeId: 'change-9', configId: 'cfg-1', appCode: 'orders', env: 'dev',
  namespace: 'default', configKey: 'application.yml', targetVersion: 4, publishMode: 'SYNC',
  contentChecksum: 'abc', attemptCount: 1, dispatchedAt: '2026-07-31T10:00:00Z',
  completedAt: '2026-07-31T10:00:05Z', failureStage: null, status: 'SUCCESS',
  targetCount: 3, ackCount: 3, failedCount: 0, ignoredCount: 0, timeoutCount: 0,
  timeoutMs: 30000, operator: 'local-admin', errorMessage: null,
  createdAt: '2026-07-31T10:00:00Z', updatedAt: '2026-07-31T10:00:05Z',
}

describe('PublishTasksPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    setDdcTokenProvider(() => 'token')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('renders publish tasks and retries a failed one', async () => {
    vi.mocked(fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/retry')) {
        return Promise.resolve(jsonResponse(record({
          changeId: 'change-9', status: 'SUCCESS', targetCount: 3, ackCount: 3, failedCount: 0,
          ignoredCount: 0, timeoutCount: 0, attemptCount: 2, targetVersion: 4,
          contentChecksum: 'abc', errorMessage: null,
        })))
      }
      return Promise.resolve(jsonResponse(record([task])))
    })
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true)

    render(<PublishTasksPage />)
    await waitFor(() => expect(screen.getByText('change-9')).toBeInTheDocument())
    expect(screen.getByText('application.yml')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: /重\s*试/ }))
    await waitFor(() => expect(screen.getByText(/重试任务 change-9/)).toBeInTheDocument())
    confirm.mockRestore()
  })
})
