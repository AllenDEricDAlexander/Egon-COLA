import {act, renderHook, waitFor} from '@testing-library/react'
import type {PropsWithChildren} from 'react'
import {describe, expect, it, vi} from 'vitest'
import type {
    ActiveRoleSetView,
    BootstrapView,
    Rbac3Client,
    ReplaceActiveRolesResult,
    RoleActivationCandidateView,
} from '../types'
import {Rbac3RequestError} from '../errors'
import {useRbac3Authorization} from '../hooks/useRbac3Authorization'
import {Rbac3Provider} from './Rbac3Provider'

const bootstrap = {permissions: ['orders:read'], defaultRoute: '/orders'} as unknown as BootstrapView
const activeRoles = {
    activeRoles: [],
    activationRequired: false,
    authVersion: 1,
    policyVersion: 1,
    snapshotChecksum: 'sum'
} as ActiveRoleSetView
const candidates = { applications: [] } as unknown as RoleActivationCandidateView

const client = (overrides: Partial<Rbac3Client> = {}): Rbac3Client => ({
  getActivationCandidates: vi.fn(async () => candidates),
  getActiveRoles: vi.fn(async () => activeRoles),
  replaceActiveRoles: vi.fn(async () => ({
      activeRoles: [], changed: true, authVersion: 2, policyVersion: 1,
      activationRequired: false, snapshotChecksum: 'next',
  } as ReplaceActiveRolesResult)),
  getBootstrap: vi.fn(async () => bootstrap),
  ...overrides,
})

describe('Rbac3Provider', () => {
    it('initializes through the protected bootstrap endpoint without token state', async () => {
    const sdk = client()
    const wrapper = ({ children }: PropsWithChildren) => (
        <Rbac3Provider client={sdk}>{children}</Rbac3Provider>
    )
        const {result} = renderHook(() => useRbac3Authorization(), {wrapper})

    await waitFor(() => expect(result.current.status).toBe('READY'))
        expect(sdk.getBootstrap).toHaveBeenCalledTimes(1)
        expect(result.current.bootstrap).toBe(bootstrap)
  })

    it('loads role activation candidates when bootstrap reports activation required', async () => {
    const sdk = client({
        getBootstrap: vi.fn(async () => {
            throw new Rbac3RequestError({
                status: 409, code: 'ROLE_ACTIVATION_REQUIRED', message: 'activate a role', retryable: false,
            })
        }),
    })
    const wrapper = ({ children }: PropsWithChildren) => (
        <Rbac3Provider client={sdk}>{children}</Rbac3Provider>
    )
        const {result} = renderHook(() => useRbac3Authorization(), {wrapper})

        await waitFor(() => expect(result.current.status).toBe('ACTIVATION_REQUIRED'))
        expect(sdk.getActivationCandidates).toHaveBeenCalledTimes(1)
        expect(sdk.getActiveRoles).toHaveBeenCalledTimes(1)
    })

    it('publishes the new bootstrap after role replacement', async () => {
        const nextBootstrap = {permissions: ['orders:read', 'orders:write']} as unknown as BootstrapView
        const sdk = client({
            getBootstrap: vi.fn()
                .mockResolvedValueOnce(bootstrap)
                .mockResolvedValueOnce(nextBootstrap)
        })
        const wrapper = ({children}: PropsWithChildren) => (
            <Rbac3Provider client={sdk}>{children}</Rbac3Provider>
        )
        const {result} = renderHook(() => useRbac3Authorization(), {wrapper})
    await waitFor(() => expect(result.current.status).toBe('READY'))

    await act(async () => {
        await result.current.replaceActiveRoles({roleIds: ['50001'], expectedAuthVersion: 1})
    })

        expect(result.current.bootstrap).toBe(nextBootstrap)
    expect(result.current.status).toBe('READY')
  })

    it('keeps activation recoverable when the gateway asks for step-up', async () => {
    const stepUp = new Rbac3RequestError({
        status: 401, code: 'STEP_UP_REQUIRED', message: 'strong authentication required', retryable: false,
    })
        const sdk = client({
            replaceActiveRoles: vi.fn(async () => {
                throw stepUp
            })
        })
    const wrapper = ({ children }: PropsWithChildren) => (
        <Rbac3Provider client={sdk}>{children}</Rbac3Provider>
    )
        const {result} = renderHook(() => useRbac3Authorization(), {wrapper})
        await waitFor(() => expect(result.current.status).toBe('READY'))

    await act(async () => {
        await expect(result.current.replaceActiveRoles({roleIds: ['50001'], expectedAuthVersion: 1}))
            .rejects.toMatchObject({code: 'STEP_UP_REQUIRED'})
    })
    expect(result.current.status).toBe('ACTIVATION_REQUIRED')
    expect(result.current.errorCode).toBe('STEP_UP_REQUIRED')
    })
})
