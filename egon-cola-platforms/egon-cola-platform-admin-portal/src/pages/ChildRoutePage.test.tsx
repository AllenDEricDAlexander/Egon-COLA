import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { AdminThemeProvider, I18nProvider, initI18n } from '@egon-cola/admin-web-shared'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { startApp } from 'wujie'
import { ChildRoutePage } from './ChildRoutePage'

const manifest = (overrides: Record<string, unknown> = {}) => ({
  key: 'gateway',
  displayName: 'API 网关',
  url: '/children/gateway/assets/index.js',
  standaloneUrl: '/gateway/dashboard',
  version: '5.3.2',
  contractVersion: 'platform-1',
  compatibleHostRange: '>=5.0.0 <6.0.0',
  requiredCapabilities: ['gateway:read'],
  ...overrides,
})
const response = (body: unknown, status = 200): Response => new Response(
  JSON.stringify(body),
  { status, headers: { 'Content-Type': 'application/json' } },
)

const renderRoute = (initialPath = '/platform/gateway/dashboard') => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminThemeProvider>
        <I18nProvider>
          <MemoryRouter initialEntries={[initialPath]}>
            <Routes>
              <Route path="/platform/:platformKey/*" element={<ChildRoutePage />} />
            </Routes>
          </MemoryRouter>
        </I18nProvider>
      </AdminThemeProvider>
    </QueryClientProvider>,
  )
}

beforeAll(async () => {
  await initI18n({ lng: 'zh-CN', resources: { 'zh-CN': {} } })
})

afterEach(() => {
  vi.unstubAllGlobals()
})

beforeEach(() => {
  const testWindow = window as unknown as Record<string, unknown>
  delete testWindow.__WUJIE_TEST_EVENTS
  delete testWindow.__WUJIE_TEST_TRIGGER_LOAD_ERROR
})

describe('ChildRoutePage', () => {
  it('passes the Portal deep-link suffix to the selected child', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response(manifest({
      url: '/children/gateway/',
    }))))

    renderRoute('/platform/gateway/dashboard')

    await screen.findByTestId('wujie-host-gateway')
    expect(vi.mocked(startApp)).toHaveBeenCalledWith(expect.objectContaining({
      url: expect.stringMatching(/\/dashboard$/),
    }))
  })

  it('does not mount an incompatible child and offers its standalone entry', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response(manifest({ compatibleHostRange: '>=9.0.0' }))))

    renderRoute()

    expect(await screen.findByText(/版本不兼容/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /独立打开/ })).toHaveAttribute('href', '/gateway/dashboard')
    expect(screen.queryByTestId('wujie-child')).not.toBeInTheDocument()
  })

  it('cleans up the child before a failed mount is retried', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response(manifest())))

    renderRoute()

    expect(await screen.findByText('子应用已加载')).toBeInTheDocument()
    const testWindow = window as unknown as {
      __WUJIE_TEST_EVENTS?: string[]
      __WUJIE_TEST_TRIGGER_LOAD_ERROR?: () => void
    }
    testWindow.__WUJIE_TEST_TRIGGER_LOAD_ERROR?.()

    expect(await screen.findByText(/子应用加载失败/)).toBeInTheDocument()
    await screen.findByRole('button', { name: '重新挂载' }).then((button) => button.click())
    expect(await screen.findByText('子应用已加载')).toBeInTheDocument()
    expect(testWindow.__WUJIE_TEST_EVENTS).toEqual([
      'beforeLoad',
      'afterMount',
      'loadError',
      'beforeUnmount',
      'afterUnmount',
      'beforeLoad',
      'afterMount',
    ])
  })
})
