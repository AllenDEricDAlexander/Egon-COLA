import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { App } from 'antd'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../api/client'
import ConfigsPage from './ConfigsPage'

const record = (data: unknown) => ({
  success: true, code: 0, status: 'SUCCESS', message: '', data, traceId: 't', timestamp: 1,
})

const jsonResponse = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

const configRow = {
  id: 'cfg-1', bizCode: 'pay-biz', appCode: 'orders', env: 'dev',
  visibleNamespaces: ['default', 'ops'],
  resourceName: 'application.yml' as const, content: 'feature:\n  enabled: true\n',
  format: 'YAML' as const, currentVersion: 3, description: '业务配置',
  createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-02T00:00:00Z',
}

describe('ConfigsPage', () => {
  beforeEach(() => {
    setDdcTokenProvider(() => 'token')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('renders the YAML document once with actions', async () => {
    vi.mocked(fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/bizs') || url.includes('/namespaces')
        || url.includes('/envs') || url.includes('/apps')) {
        return Promise.resolve(jsonResponse(record([])))
      }
      if (url.includes('/configs')) return Promise.resolve(jsonResponse(record([configRow, configRow])))
      return Promise.resolve(jsonResponse(record(null)))
    })

    render(<App><ConfigsPage /></App>)
    await waitFor(() => expect(screen.getByText('application.yml')).toBeInTheDocument())
    expect(screen.getByText('YAML')).toBeInTheDocument()
    expect(screen.getByText('业务配置')).toBeInTheDocument()
    expect(screen.getByText('default')).toBeInTheDocument()
    expect(screen.getByText('ops')).toBeInTheDocument()
    expect(screen.getAllByText('application.yml')).toHaveLength(1)
    const listRequest = vi.mocked(fetch).mock.calls
      .map(([input]) => String(input))
      .find((url) => url.includes('/api/v1/ddc/configs?'))
    expect(listRequest).toContain('includeDeleted=false')
    expect(listRequest).not.toContain('bizCode=')
    expect(listRequest).not.toContain('namespaceCode=')
    expect(listRequest).not.toContain('resourceName=')
  })

  it('publishes with uuid changeId and refreshes', async () => {
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(fetch).mockImplementation((input, init) => {
      const url = String(input)
      if (url.includes('/bizs') || url.includes('/namespaces')
        || url.includes('/envs') || url.includes('/apps')) {
        return Promise.resolve(jsonResponse(record([])))
      }
      if (url.includes('/publish')) {
        const body = JSON.parse(String(init?.body))
        expect(body.changeId).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/)
        expect(body.expectedVersion).toBe(3)
        return Promise.resolve(jsonResponse(record({
          changeId: 'change-1', status: 'SUCCESS', targetCount: 2, ackCount: 2, failedCount: 0,
          ignoredCount: 0, timeoutCount: 0, attemptCount: 1, targetVersion: 3,
          resourceChecksum: 'abc', errorMessage: null,
        })))
      }
      if (url.includes('/configs')) return Promise.resolve(jsonResponse(record([configRow])))
      return Promise.resolve(jsonResponse(record(null)))
    })

    render(<App><ConfigsPage /></App>)
    await waitFor(() => expect(screen.getByText('application.yml')).toBeInTheDocument())
    fireEvent.click(screen.getByRole('button', { name: /发\s*布/ }))
    await waitFor(() => expect(screen.getByText(/发布任务 change-1/)).toBeInTheDocument())
    confirm.mockRestore()
  })
})
