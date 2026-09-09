import {act, renderHook, waitFor} from '@testing-library/react'
import type {PropsWithChildren} from 'react'
import {describe, expect, it, vi} from 'vitest'
import type {
    ActiveRoleSetView,
    Rbac3AboutView,
    Rbac3Client,
    ReplaceActiveRolesResult,
    RoleActivationCandidateView,
} from '../types'
import {Rbac3RequestError} from '../errors'
import {useRbac3Authorization} from '../hooks/useRbac3Authorization'
import {Rbac3Provider} from './Rbac3Provider'

const about = {permissions: ['orders:read'], resourceCodes: ['orders.read'], currentApplicationCode: 'orders'} as unknown as Rbac3AboutView
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
  getAbout: vi.fn(async () => about),
  ...overrides,
})

describe('Rbac3Provider', () => {
    it('opens role selection when about returns the restricted initial RBAC context', async () => {
        const sdk = client({getAbout: vi.fn(async () => ({
            ...about, currentApplicationCode: 'rbac3-admin', activeRoles: [], resourceCodes: [],
            permissions: ['system:about:read', 'system:role-activation:read', 'system:role-activation:use'],
        }))})
        const wrapper = ({children}: PropsWithChildren) => <Rbac3Provider client={sdk}>{children}</Rbac3Provider>
        const {result} = renderHook(() => useRbac3Authorization(), {wrapper})
        await waitFor(() => expect(result.current.status).toBe('ACTIVATION_REQUIRED'))
        expect(sdk.getActivationCandidates).toHaveBeenCalledTimes(1)
        expect(sdk.getActiveRoles).toHaveBeenCalledTimes(1)
        expect(sdk.replaceActiveRoles).not.toHaveBeenCalled()
    })

    it('keeps a user with selected roles ready even when role activation is permitted', async () => {
        const sdk = client({getAbout: vi.fn(async () => ({
            ...about, currentApplicationCode: 'rbac3-admin',
            activeRoles: [{applicationCode: 'rbac3-admin', roleId: '10', roleCode: 'ROOT'}],
            permissions: ['system:role-activation:use'],
        }))})
        const wrapper = ({children}: PropsWithChildren) => <Rbac3Provider client={sdk}>{children}</Rbac3Provider>
        const {result} = renderHook(() => useRbac3Authorization(), {wrapper})
        await waitFor(() => expect(result.current.status).toBe('READY'))
        expect(sdk.getActivationCandidates).not.toHaveBeenCalled()
    })

    it('initializes through the protected about endpoint without token state', async () => {
    const sdk = client()
    const wrapper = ({ children }: PropsWithChildren) => (
        <Rbac3Provider client={sdk}>{children}</Rbac3Provider>
    )
        const {result} = renderHook(() => useRbac3Authorization(), {wrapper})

    await waitFor(() => expect(result.current.status).toBe('READY'))
        expect(sdk.getAbout).toHaveBeenCalledTimes(1)
        expect(result.current.about).toBe(about)
  })

    it('loads role activation candidates when about reports activation required', async () => {
    const sdk = client({
        getAbout: vi.fn(async () => {
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

    it('publishes the new about after role replacement', async () => {
        const nextAbout = {permissions: ['orders:read', 'orders:write'], resourceCodes: ['orders.read', 'orders.write']} as unknown as Rbac3AboutView
        const sdk = client({
            getAbout: vi.fn()
                .mockResolvedValueOnce(about)
                .mockResolvedValueOnce(nextAbout)
        })
        const wrapper = ({children}: PropsWithChildren) => (
            <Rbac3Provider client={sdk}>{children}</Rbac3Provider>
        )
        const {result} = renderHook(() => useRbac3Authorization(), {wrapper})
    await waitFor(() => expect(result.current.status).toBe('READY'))

    await act(async () => {
        await result.current.replaceActiveRoles({roleIds: ['50001'], expectedAuthVersion: 1})
    })

        expect(result.current.about).toBe(nextAbout)
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
