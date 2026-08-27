import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { GatewayRelease } from '../../api/types'
import { ReleaseDetailPage } from './ReleaseDetailPage'

const mocks = vi.hoisted(() => ({
  release: vi.fn(),
  draft: vi.fn(),
  retryRelease: vi.fn(),
  rollback: vi.fn(),
}))

vi.mock('../../api/gatewayApi', () => ({
  gatewayApi: {
    release: mocks.release,
    draft: mocks.draft,
    retryRelease: mocks.retryRelease,
    rollback: mocks.rollback,
  },
}))

vi.mock('../../app/capabilities', () => ({
  useCapability: () => true,
}))

const baseRelease: GatewayRelease = {
  id: 'release-1',
  gatewayGroupId: 'group-1',
  draftRevision: 3,
  status: 'SUCCEEDED',
  partialApplied: false,
  validationReport: { valid: true, errors: [], warnings: [] },
  structuredDiff: { routes: { changed: 1 } },
  changeReason: 'publish orders route',
  createdAt: '2026-08-27T03:00:00Z',
  updatedAt: '2026-08-27T03:01:00Z',
  attempts: [{
    attemptNo: 1,
    status: 'SUCCEEDED',
    startedAt: '2026-08-27T03:00:01Z',
    completedAt: '2026-08-27T03:00:02Z',
    targets: [{
      instanceId: 'engine-1',
      leaseId: 'lease-1',
      status: 'APPLIED',
      appliedVersion: 3,
      appliedArtifactSha256: 'a'.repeat(64),
      observedAt: '2026-08-27T03:00:02Z',
    }],
  }],
}

const renderPage = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/gateway-groups/group-1/releases/release-1']}>
        <Routes>
          <Route path="/gateway-groups/:groupId/releases/:releaseId" element={<ReleaseDetailPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

beforeEach(() => {
  mocks.release.mockReset().mockResolvedValue(baseRelease)
  mocks.draft.mockReset().mockResolvedValue({ revision: 4 })
  mocks.retryRelease.mockReset().mockResolvedValue(baseRelease)
  mocks.rollback.mockReset().mockResolvedValue({ ...baseRelease, id: 'release-rollback-1' })
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

describe('ReleaseDetailPage evidence state', () => {
  it('does not render a partially applied release as successful', async () => {
    mocks.release.mockResolvedValue({ ...baseRelease, partialApplied: true })

    renderPage()

    expect(await screen.findByText('该 Release 存在部分生效，不能视为成功。')).toBeInTheDocument()
    expect(screen.getByText('SUCCEEDED（部分生效）')).toBeInTheDocument()
    expect(screen.queryByText('发布成功')).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '使用原 Release 内容和原 Target 重试' }))
    await waitFor(() => expect(mocks.retryRelease).toHaveBeenCalledWith('release-1', expect.anything()))
  })

  it('requires a reason and a fresh draft before creating a rollback release', async () => {
    renderPage()

    await screen.findByText('Release release-1')
    fireEvent.click(screen.getByRole('button', { name: '创建回滚 Release' }))

    expect(await screen.findByPlaceholderText('必填变更原因')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'OK' })).toBeDisabled()

    fireEvent.change(screen.getByPlaceholderText('必填变更原因'), { target: { value: 'rollback unsafe target' } })
    await waitFor(() => expect(screen.getByRole('button', { name: 'OK' })).toBeEnabled())
    fireEvent.click(screen.getByRole('button', { name: 'OK' }))

    await waitFor(() => expect(mocks.rollback).toHaveBeenCalledWith(
      'group-1',
      'release-1',
      4,
      'rollback unsafe target',
      expect.anything(),
    ))
  })

  it.each([
    ['FAILED', 'Release 发布失败，不能视为成功。'],
    ['TIMEOUT', 'Release 已超时，不能视为成功。'],
    ['UNKNOWN', 'Release 状态未知，不能视为成功。'],
  ])('keeps %s as an explicit non-success status', async (status, recoveryMessage) => {
    mocks.release.mockResolvedValue({ ...baseRelease, status })

    renderPage()

    expect(await screen.findByText(status)).toBeInTheDocument()
    expect(await screen.findByText(recoveryMessage)).toBeInTheDocument()
    expect(screen.queryByText('发布成功')).not.toBeInTheDocument()
  })
})
