import { act, renderHook, waitFor } from '@testing-library/react'
import type { PropsWithChildren } from 'react'
import { describe, expect, it, vi } from 'vitest'
import type {
  ActiveRoleSetView,
  BootstrapView,
  Rbac3Client,
  RefreshResult,
  ReplaceActiveRolesResult,
  RoleActivationCandidateView,
} from '../types'
import { InMemoryAccessTokenStore } from '../auth/InMemoryAccessTokenStore'
import { Rbac3ApiClient } from '../client/Rbac3ApiClient'
import { Rbac3RequestError } from '../errors'
import { Rbac3Provider } from './Rbac3Provider'
import { useRbac3Session } from '../hooks/useRbac3Session'

const refresh = {
  accessToken: 'refresh-token-access',
  roleActivationRequired: false,
} as RefreshResult

const bootstrap = {
  permissions: ['orders:read'],
  defaultRoute: '/orders',
} as unknown as BootstrapView

const activeRoles = {
  activeRoles: [],
  sessionVersion: 2,
} as unknown as ActiveRoleSetView

const candidates = { applications: [] } as unknown as RoleActivationCandidateView

const client = (overrides: Partial<Rbac3Client> = {}): Rbac3Client => ({
  getActivationCandidates: vi.fn(async () => candidates),
  getActiveRoles: vi.fn(async () => activeRoles),
  replaceActiveRoles: vi.fn(async () => ({
    accessToken: 'replacement-access',
    bootstrapRequired: true,
  } as ReplaceActiveRolesResult)),
  getBootstrap: vi.fn(async () => bootstrap),
  refresh: vi.fn(async () => refresh),
  logout: vi.fn(async () => undefined),
  ...overrides,
})

describe('Rbac3Provider', () => {
  it('initializes once when multiple hooks consume one provider', async () => {
    const sdk = client()
    const store = new InMemoryAccessTokenStore()
    const wrapper = ({ children }: PropsWithChildren) => (
      <Rbac3Provider client={sdk} accessTokenStore={store}>{children}</Rbac3Provider>
    )
    const session = renderHook(() => [useRbac3Session(), useRbac3Session()] as const, {
      wrapper,
    })

    await waitFor(() => expect(session.result.current[0].status).toBe('READY'))
    expect(session.result.current[1].status).toBe('READY')

    expect(sdk.refresh).toHaveBeenCalledTimes(1)
    expect(store.get()).toBe('refresh-token-access')
  })

  it('replaces the access token before publishing the new bootstrap', async () => {
    const sdk = client()
    const store = new InMemoryAccessTokenStore('old-access')
    const observed: string[] = []
    store.subscribe(() => observed.push(store.get() ?? '<empty>'))
    const wrapper = ({ children }: PropsWithChildren) => (
      <Rbac3Provider client={sdk} accessTokenStore={store}>{children}</Rbac3Provider>
    )
    const { result } = renderHook(() => useRbac3Session(), { wrapper })
    await waitFor(() => expect(result.current.status).toBe('READY'))

    await act(async () => {
      await result.current.replaceActiveRoles({
        roleIds: ['50001'],
        expectedSessionVersion: 1,
      })
    })

    expect(store.get()).toBe('replacement-access')
    expect(observed).toContain('replacement-access')
    expect(result.current.status).toBe('READY')
  })

  it('recovers an uncertain replace response through refresh plus current GET', async () => {
    const lost = Object.assign(new Error('response lost'), { retryable: true })
    const sdk = client({
      replaceActiveRoles: vi.fn(async () => { throw lost }),
    })
    const wrapper = ({ children }: PropsWithChildren) => (
      <Rbac3Provider client={sdk} accessTokenStore={new InMemoryAccessTokenStore()}>
        {children}
      </Rbac3Provider>
    )
    const { result } = renderHook(() => useRbac3Session(), { wrapper })
    await waitFor(() => expect(result.current.status).toBe('READY'))

    await act(async () => {
      await result.current.replaceActiveRoles({
        roleIds: ['50001'],
        expectedSessionVersion: 1,
      })
    })

    expect(sdk.refresh).toHaveBeenCalledTimes(2)
    expect(sdk.getActiveRoles).toHaveBeenCalledTimes(1)
    expect(result.current.status).toBe('READY')
  })

  it('single-flights concurrent HTTP 401 refresh and retries with the new token', async () => {
    const store = new InMemoryAccessTokenStore('old-access')
    let refreshCalls = 0
    const fetcher = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const path = String(input)
      if (path.endsWith('/auth/refresh')) {
        refreshCalls += 1
        return jsonResponse({
          data: { ...refresh, accessToken: 'new-access' },
        })
      }
      const authorization = new Headers(init?.headers).get('Authorization')
      return authorization === 'Bearer new-access'
        ? jsonResponse({ data: bootstrap })
        : jsonResponse({
            error: {
              code: 'AUTH_VERSION_MISMATCH',
              message: 'version changed',
              retryable: false,
              details: [],
            },
            meta: { traceId: 'trace-1' },
          }, 401)
    })
    const sdk = new Rbac3ApiClient({
      accessTokenStore: store,
      fetch: fetcher as typeof fetch,
    })

    await Promise.all([sdk.getBootstrap(), sdk.getBootstrap()])

    expect(refreshCalls).toBe(1)
    expect(store.get()).toBe('new-access')
    expect(fetcher.mock.calls.every(([, init]) => init?.credentials === 'include'))
      .toBe(true)
  })

  it('does not refresh a forbidden response', async () => {
    const fetcher = vi.fn(async () => jsonResponse({
      error: {
        code: 'PERMISSION_DENIED',
        message: 'forbidden',
        retryable: false,
        details: [],
      },
      meta: { traceId: 'trace-2' },
    }, 403))
    const sdk = new Rbac3ApiClient({
      accessTokenStore: new InMemoryAccessTokenStore('access'),
      fetch: fetcher as typeof fetch,
    })

    await expect(sdk.getBootstrap()).rejects.toMatchObject({
      status: 403,
      code: 'PERMISSION_DENIED',
    })
    expect(fetcher).toHaveBeenCalledTimes(1)
  })

  it('does not refresh a step-up challenge', async () => {
    const store = new InMemoryAccessTokenStore('access')
    const fetcher = vi.fn(async () => jsonResponse({
      error: {
        code: 'STEP_UP_REQUIRED',
        message: 'strong authentication required',
        retryable: true,
        details: [],
      },
      meta: { traceId: 'trace-step-up' },
    }, 401))
    const sdk = new Rbac3ApiClient({
      accessTokenStore: store,
      fetch: fetcher as typeof fetch,
    })

    await expect(sdk.replaceActiveRoles({
      roleIds: ['50001'],
      expectedSessionVersion: 1,
    })).rejects.toMatchObject({
      status: 401,
      code: 'STEP_UP_REQUIRED',
    })

    expect(store.get()).toBe('access')
    expect(fetcher).toHaveBeenCalledTimes(1)
  })

  it('keeps role activation recoverable while step-up is required', async () => {
    const stepUp = new Rbac3RequestError({
      status: 401,
      code: 'STEP_UP_REQUIRED',
      message: 'strong authentication required',
      retryable: true,
    })
    const sdk = client({
      refresh: vi.fn(async () => ({ ...refresh, roleActivationRequired: true })),
      replaceActiveRoles: vi.fn(async () => { throw stepUp }),
    })
    const wrapper = ({ children }: PropsWithChildren) => (
      <Rbac3Provider client={sdk} accessTokenStore={new InMemoryAccessTokenStore()}>
        {children}
      </Rbac3Provider>
    )
    const { result } = renderHook(() => useRbac3Session(), { wrapper })
    await waitFor(() => expect(result.current.status).toBe('ACTIVATION_REQUIRED'))

    await act(async () => {
      await expect(result.current.replaceActiveRoles({
        roleIds: ['50001'],
        expectedSessionVersion: 1,
      })).rejects.toMatchObject({ code: 'STEP_UP_REQUIRED' })
    })

    expect(result.current.status).toBe('ACTIVATION_REQUIRED')
    expect(result.current.errorCode).toBe('STEP_UP_REQUIRED')
    expect(sdk.refresh).toHaveBeenCalledTimes(1)
  })

  it('treats an unauthorized logout as an already closed session', async () => {
    const store = new InMemoryAccessTokenStore('expired-access')
    const fetcher = vi.fn(async () => jsonResponse({
      error: {
        code: 'AUTHENTICATION_REQUIRED',
        message: 'login required',
        retryable: false,
        details: [],
      },
      meta: { traceId: 'trace-logout' },
    }, 401))
    const sdk = new Rbac3ApiClient({
      accessTokenStore: store,
      fetch: fetcher as typeof fetch,
    })

    await expect(sdk.logout()).resolves.toBeUndefined()

    expect(store.get()).toBeNull()
    expect(fetcher).toHaveBeenCalledTimes(1)
  })
})

const jsonResponse = (body: unknown, status = 200) => new Response(
  JSON.stringify(body),
  {
    status,
    headers: { 'Content-Type': 'application/json' },
  },
)
