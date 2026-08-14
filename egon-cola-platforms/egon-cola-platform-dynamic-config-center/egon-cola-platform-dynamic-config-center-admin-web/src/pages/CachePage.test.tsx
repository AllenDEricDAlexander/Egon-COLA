import {fireEvent, screen, waitFor} from '@testing-library/react'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {setDdcUnauthorizedHandler} from '../api/client'
import {renderWithQueryClient} from '../test/renderWithQueryClient'
import CachePage from './CachePage'

const record = (data: unknown) => ({
  success: true,
  code: 0,
  status: 'SUCCESS',
  message: '',
  data,
  traceId: 't',
  timestamp: 1,
})

const pageRecord = {
  success: true,
  code: 0,
  status: 'SUCCESS',
  message: '',
  records: [{
    resourceName: 'application.yml',
    databaseValue: 'feature: true',
    redisValue: 'feature: true',
    databaseVersion: 4,
    redisVersion: 4,
    matched: true,
  }],
  page: {
    total: 12,
    pageNo: 1,
    pageSize: 10,
    pages: 2,
    hasNext: true,
    hasPrevious: false,
  },
  traceId: 't',
  timestamp: 1,
}

const jsonResponse = (body: unknown) => new Response(
  JSON.stringify(body),
  { status: 200, headers: { 'Content-Type': 'application/json' } },
)

const chooseScopeValue = (input: HTMLElement, value: string) => {
  fireEvent.change(input, { target: { value } })
  fireEvent.keyDown(input, { key: 'Enter', code: 'Enter', keyCode: 13 })
}

describe('CachePage', () => {
  beforeEach(() => {
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn((input, init) => {
      const url = String(input)
      if (url.includes('/cache/check/page')) {
        return Promise.resolve(jsonResponse(pageRecord))
      }
      if (url.includes('/cache/rebuild') && init?.method === 'POST') {
        return Promise.resolve(jsonResponse(record(1)))
      }
      return Promise.resolve(jsonResponse(record([])))
    }))
  })

  it('pages checks, summarizes the current page, and confirms rebuild', async () => {
    renderWithQueryClient(<CachePage />)
    const [bizInput, namespaceInput, envInput, appInput] =
      screen.getAllByRole('combobox')
    chooseScopeValue(bizInput, 'infra')
    chooseScopeValue(namespaceInput, 'default')
    chooseScopeValue(envInput, 'prod')
    chooseScopeValue(appInput, 'gateway')

    fireEvent.click(screen.getByRole('button', { name: /检\s*查\s*缓\s*存/ }))
    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      expect.stringMatching(
        /\/api\/v1\/ddc\/cache\/check\/page\?.*bizCode=infra.*env=prod.*appCode=gateway.*pageNo=1.*pageSize=10/,
      ),
      expect.anything(),
    ))
    expect(await screen.findByText('共 12 条')).toBeInTheDocument()
    expect(screen.getByText('本页一致')).toBeInTheDocument()
    expect(screen.getByText('本页不一致')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /重\s*建\s*缓\s*存/ }))
    expect(await screen.findAllByText('确认重建该作用域下的缓存？'))
      .not.toHaveLength(0)
    expect(vi.mocked(fetch).mock.calls.some(([input, init]) =>
      String(input).includes('/cache/rebuild')
      && init?.method === 'POST')).toBe(false)

    const rebuildButtons = screen.getAllByRole('button', {
      name: /重\s*建\s*缓\s*存/,
    })
    fireEvent.click(rebuildButtons[rebuildButtons.length - 1])
    await waitFor(() => expect(vi.mocked(fetch).mock.calls.some(
      ([input, init]) => String(input).includes('/cache/rebuild')
        && init?.method === 'POST',
    )).toBe(true))
  })
})
