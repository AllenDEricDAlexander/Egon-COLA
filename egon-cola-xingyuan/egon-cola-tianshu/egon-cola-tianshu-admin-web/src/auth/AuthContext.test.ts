import { createElement } from 'react'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import * as apiClient from '../api/client'
import {ddcApi} from '../api/client'
import type { AuthorizationBootstrap } from '../api/types'
import { AuthProvider, gatewayAuth, useAuth } from './AuthContext'
import { renderWithQueryClient } from '../test/renderWithQueryClient'

type RequestContext = { tenantId?: string; sessionGeneration: number }

const requestContext = () => {
  const getter = (apiClient as unknown as Record<string, unknown>).getDdcRequestContext
  expect(getter).toBeTypeOf('function')
  if (typeof getter !== 'function') throw new Error('request context getter is unavailable')
  return (getter as () => RequestContext)()
}

const bootstrap = (tenantId: string): AuthorizationBootstrap => ({
  user: { id: `user-${tenantId}`, tenantId, identitySub: `subject-${tenantId}`, status: 'ACTIVE' },
  activeRoleContexts: [],
  permissions: ['TIANSHU_READ'],
  apps: [],
  menus: [],
  routes: [],
  actions: [],
  fieldPolicies: {},
  defaultApplicationCode: null,
  defaultRoute: null,
  authVersion: 1,
  policyVersion: 1,
})

function SessionProbe() {
  const auth = useAuth()
  return createElement(
    'div',
    null,
    createElement('span', { 'data-testid': 'tenant' }, requestContext().tenantId ?? ''),
    createElement('span', { 'data-testid': 'generation' }, String(requestContext().sessionGeneration)),
    createElement('button', { onClick: () => void auth.login('2002', 'operator', 'password') }, 'switch-tenant'),
    createElement('button', { onClick: () => void auth.logout() }, 'logout'),
  )
}

describe('AuthContext tenant session isolation', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
  })

  it('derives the expected request tenant from the verified bootstrap', async () => {
    vi.spyOn(gatewayAuth, 'bootstrap').mockResolvedValue(bootstrap('1001'))

    const { unmount } = renderWithQueryClient(
      createElement(AuthProvider, null, createElement(SessionProbe)),
    )

    await waitFor(() => expect(requestContext().tenantId).toBe('1001'))
    unmount()
  })

  it('clears old query data and advances the session when switching tenants', async () => {
    const bootstraps = [bootstrap('1001'), bootstrap('2002')]
    vi.spyOn(gatewayAuth, 'bootstrap').mockImplementation(async () => bootstraps.shift() as AuthorizationBootstrap)
    vi.spyOn(gatewayAuth, 'login').mockResolvedValue(undefined as never)

    const { queryClient, unmount } = renderWithQueryClient(
      createElement(AuthProvider, null, createElement(SessionProbe)),
    )
    await waitFor(() => expect(requestContext().tenantId).toBe('1001'))
    const firstGeneration = requestContext().sessionGeneration
    const oldTenantQueryKey = ['tianshu', 'tenant', '1001', 'ai', 'resources'] as const
    queryClient.setQueryData(oldTenantQueryKey, { privateResource: 'tenant-1001' })
    let pendingSignal: AbortSignal | undefined
    const pendingQuery = queryClient.fetchQuery({
      queryKey: ['tianshu', 'tenant', '1001', 'ai', 'in-flight'],
      queryFn: ({signal}) => new Promise<never>((_resolve, reject) => {
        pendingSignal = signal
        signal.addEventListener('abort', () => reject(new DOMException('cancelled', 'AbortError')), {once: true})
      }),
    }).catch(() => undefined)
    await waitFor(() => expect(pendingSignal).toBeDefined())

    fireEvent.click(screen.getByText('switch-tenant'))

    await waitFor(() => expect(requestContext().tenantId).toBe('2002'))
    expect(requestContext().sessionGeneration).toBeGreaterThan(firstGeneration)
    expect(pendingSignal?.aborted).toBe(true)
    expect(queryClient.getQueryData(oldTenantQueryKey)).toBeUndefined()
    await pendingQuery
    unmount()
  })

  it('rebootstraps on tenant mismatch without replaying the request', async () => {
    const bootstraps = [bootstrap('1001'), bootstrap('2002')]
    vi.spyOn(gatewayAuth, 'bootstrap').mockImplementation(
      async () => bootstraps.shift() as AuthorizationBootstrap,
    )
    const {queryClient, unmount} = renderWithQueryClient(
      createElement(AuthProvider, null, createElement(SessionProbe)),
    )
    await waitFor(() => expect(requestContext().tenantId).toBe('1001'))
    const firstGeneration = requestContext().sessionGeneration
    const oldTenantKey = ['tianshu', 'tenant', '1001', 'ai', 'resource', '810001'] as const
    queryClient.setQueryData(oldTenantKey, {privateValue: 'tenant-1001'})
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({
      code: 'TIANSHU_ADMIN_TENANT_MISMATCH',
      message: 'TIANSHU_ADMIN_TENANT_MISMATCH',
    }), {status: 403, headers: {'Content-Type': 'application/json'}})))

    await expect(ddcApi('/api/v1/tianshu/ai/resources/810001/draft', {
      method: 'PUT',
      body: {expectedRevision: '1'},
    })).rejects.toMatchObject({status: 403, code: 'TIANSHU_ADMIN_TENANT_MISMATCH'})
    await waitFor(() => expect(requestContext().tenantId).toBe('2002'))

    expect(requestContext().sessionGeneration).toBeGreaterThan(firstGeneration)
    expect(queryClient.getQueryData(oldTenantKey)).toBeUndefined()
    expect(vi.mocked(fetch)).toHaveBeenCalledTimes(1)
    unmount()
  })

  it('clears tenant query state and advances generation on logout', async () => {
    vi.spyOn(gatewayAuth, 'bootstrap').mockResolvedValue(bootstrap('1001'))
    vi.spyOn(gatewayAuth, 'logout').mockResolvedValue(undefined as never)
    const {queryClient, unmount} = renderWithQueryClient(
      createElement(AuthProvider, null, createElement(SessionProbe)),
    )
    await waitFor(() => expect(requestContext().tenantId).toBe('1001'))
    const generation = requestContext().sessionGeneration
    const tenantKey = ['tianshu', 'tenant', '1001', 'ai', 'resources'] as const
    queryClient.setQueryData(tenantKey, [{id: '810001'}])

    fireEvent.click(screen.getByText('logout'))

    await waitFor(() => expect(requestContext().tenantId).toBeUndefined())
    expect(requestContext().sessionGeneration).toBeGreaterThan(generation)
    expect(queryClient.getQueryData(tenantKey)).toBeUndefined()
    expect(gatewayAuth.logout).toHaveBeenCalledTimes(1)
    unmount()
  })
})
