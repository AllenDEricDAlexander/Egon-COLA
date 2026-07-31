import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../api/client'
import ConfigsPage from './ConfigsPage'

const record = (data: unknown) => ({
  success: true, code: 0, status: 'SUCCESS', message: '', data, traceId: 't', timestamp: 1,
})

const jsonResponse = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

const configRow = {
  id: 'cfg-1', appCode: 'orders', env: 'dev', namespace: 'default',
  configKey: 'feature.flags', configValue: '{"enabled":true}', defaultValue: '',
  valueType: 'JSON', currentVersion: 3, description: '功能开关',
  createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-02T00:00:00Z',
}

describe('ConfigsPage', () => {
  beforeEach(() => {
    setDdcTokenProvider(() => 'token')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('renders config rows with format badge and actions', async () => {
    vi.mocked(fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/namespaces/domains')) {
        return Promise.resolve(jsonResponse(record([])))
      }
      if (url.includes('/apps')) {
        return Promise.resolve(jsonResponse(record([])))
      }
      if (url.includes('/configs')) return Promise.resolve(jsonResponse(record([configRow])))
      return Promise.resolve(jsonResponse(record(null)))
    })

    render(<ConfigsPage />)
    await waitFor(() => expect(screen.getByText('feature.flags')).toBeInTheDocument())
    expect(screen.getAllByText('JSON').length).toBeGreaterThan(0)
    expect(screen.getByText('功能开关')).toBeInTheDocument()
  })

  it('publishes with uuid changeId and refreshes', async () => {
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(fetch).mockImplementation((input, init) => {
      const url = String(input)
      if (url.includes('/namespaces/domains')) {
        return Promise.resolve(jsonResponse(record([])))
      }
      if (url.includes('/apps')) {
        return Promise.resolve(jsonResponse(record([])))
      }
      if (url.includes('/publish')) {
        const body = JSON.parse(String(init?.body))
        expect(body.changeId).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/)
        expect(body.expectedVersion).toBe(3)
        return Promise.resolve(jsonResponse(record({
          changeId: 'change-1', status: 'SUCCESS', targetCount: 2, ackCount: 2, failedCount: 0,
          ignoredCount: 0, timeoutCount: 0, attemptCount: 1, targetVersion: 3,
          contentChecksum: 'abc', errorMessage: null,
        })))
      }
      if (url.includes('/configs')) return Promise.resolve(jsonResponse(record([configRow])))
      return Promise.resolve(jsonResponse(record(null)))
    })

    render(<ConfigsPage />)
    await waitFor(() => expect(screen.getByText('feature.flags')).toBeInTheDocument())
    fireEvent.click(screen.getByRole('button', { name: /发\s*布/ }))
    await waitFor(() => expect(screen.getByText(/发布任务 change-1/)).toBeInTheDocument())
    confirm.mockRestore()
  })
})
