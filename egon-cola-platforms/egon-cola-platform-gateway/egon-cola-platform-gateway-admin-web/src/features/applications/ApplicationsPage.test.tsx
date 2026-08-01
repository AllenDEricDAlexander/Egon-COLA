import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { GatewayApiError } from '../../api/client'
import { ApplicationsPage } from './ApplicationsPage'

const scope = {
  bizCode: 'retail',
  namespace: 'ops',
  env: 'local',
  appCode: 'order',
}

const mocks = vi.hoisted(() => ({
  applications: vi.fn(),
  createApplication: vi.fn(),
}))

vi.mock('../../api/gatewayApi', () => ({
  gatewayApi: {
    applications: mocks.applications,
    createApplication: mocks.createApplication,
    updateApplication: vi.fn(),
    credentials: vi.fn(),
    createCredential: vi.fn(),
    rotateCredential: vi.fn(),
    revokeCredential: vi.fn(),
  },
}))

vi.mock('../../hooks/useScope', () => ({
  useScope: () => ({ scope }),
}))

vi.mock('../../app/capabilities', () => ({
  useCapability: () => true,
}))

vi.mock('antd', async (importOriginal) => {
  const actual = await importOriginal<typeof import('antd')>()
  return {
    ...actual,
    Modal: ({
      children,
      onCancel,
      onOk,
      open,
      title,
    }: {
      children?: ReactNode
      onCancel?: () => void
      onOk?: () => void
      open?: boolean
      title?: ReactNode
    }) => open ? (
      <section aria-label={String(title)}>
        <h2>{title}</h2>
        {children}
        <button type="button" onClick={onCancel}>Cancel</button>
        <button type="button" onClick={onOk}>OK</button>
      </section>
    ) : null,
    message: { success: vi.fn() },
  }
})

const renderPage = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      mutations: { retry: false },
      queries: { retry: false },
    },
  })
  render(
    <QueryClientProvider client={queryClient}>
      <ApplicationsPage />
    </QueryClientProvider>,
  )
  return queryClient
}

beforeEach(() => {
  mocks.applications.mockReset().mockResolvedValue([])
  mocks.createApplication.mockReset().mockResolvedValue({
    id: 'application-order',
    applicationCode: 'order',
    displayName: 'Order Gateway',
    ...scope,
    ddcMatched: true,
    revision: 0,
  })
  vi.stubGlobal('matchMedia', vi.fn().mockImplementation(() => ({
    matches: false,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })))
  vi.stubGlobal('ResizeObserver', class {
    observe() {}
    unobserve() {}
    disconnect() {}
  })
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('ApplicationsPage DDC identity', () => {
  it('creates an application from the selected read-only DDC scope', async () => {
    renderPage()
    await screen.findByText('Application / Credential')

    fireEvent.click(screen.getByRole('button', { name: '新建 Application' }))

    for (const value of ['retail', 'ops', 'local', 'order']) {
      expect(screen.getByDisplayValue(value)).toBeDisabled()
    }
    expect(screen.queryByLabelText('Application Code')).not.toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('名称'), {
      target: { value: 'Order Gateway' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'OK' }))

    await waitFor(() => expect(mocks.createApplication).toHaveBeenCalledWith({
      bizCode: 'retail',
      namespace: 'ops',
      env: 'local',
      applicationCode: 'order',
      displayName: 'Order Gateway',
      description: undefined,
    }))
  })

  it('shows duplicate physical application errors in the modal', async () => {
    mocks.createApplication.mockRejectedValue(new GatewayApiError(
      409,
      'GATEWAY_ADMIN_APPLICATION_ALREADY_EXISTS',
      'gateway application already exists: application-order',
    ))
    renderPage()
    await screen.findByText('Application / Credential')
    fireEvent.click(screen.getByRole('button', { name: '新建 Application' }))
    fireEvent.change(screen.getByLabelText('名称'), {
      target: { value: 'Order Gateway' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'OK' }))

    expect(await screen.findByText(
      'gateway application already exists: application-order',
    )).toBeInTheDocument()
  })
})
