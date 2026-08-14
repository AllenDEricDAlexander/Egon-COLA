import '@testing-library/jest-dom/vitest'
import {act, render, screen, waitFor} from '@testing-library/react'
import {describe, expect, it} from 'vitest'
import type {BootstrapView, Rbac3Client} from '../types'
import {Rbac3Provider} from '../provider/Rbac3Provider'
import {ActionGuard} from './ActionGuard'
import {FieldGuard} from './FieldGuard'
import {PermissionGuard} from './PermissionGuard'

const bootstrap = {
  permissions: ['orders:read'],
  fieldPolicies: {
    'orders:detail': {
      decision: 'ALLOW',
      fields: {
        accountNo: { level: 'MASKED_READ', maskingStrategy: 'BANK_ACCOUNT' },
        amount: { level: 'READ', maskingStrategy: null },
        internalNote: { level: 'NONE', maskingStrategy: null },
      },
    },
  },
} as unknown as BootstrapView

const client = {
    getActivationCandidates: async () => ({applications: []}),
    getActiveRoles: async () => ({
        activeRoles: [],
        activationRequired: false,
        authVersion: 1,
        policyVersion: 1,
        snapshotChecksum: 'sum'
    }),
    replaceActiveRoles: async () => ({
        activeRoles: [],
        changed: false,
        authVersion: 1,
        policyVersion: 1,
        activationRequired: false,
        snapshotChecksum: 'sum'
    }),
  getBootstrap: async () => bootstrap,
} as unknown as Rbac3Client

describe('RBAC3 guards', () => {
  it('hides permissions by default until bootstrap is ready', async () => {
    let releaseBootstrap: ((view: BootstrapView) => void) | undefined
    const delayedClient = {
      ...client,
      getBootstrap: () => new Promise<BootstrapView>((resolve) => {
        releaseBootstrap = resolve
      }),
    } as Rbac3Client

    render(
        <Rbac3Provider client={delayedClient}>
        <PermissionGuard permission="orders:read">visible</PermissionGuard>
      </Rbac3Provider>,
    )

    expect(screen.queryByText('visible')).not.toBeInTheDocument()
    await waitFor(() => expect(releaseBootstrap).toBeTypeOf('function'))
    act(() => releaseBootstrap?.(bootstrap))
    await waitFor(() => expect(screen.getByText('visible')).toBeInTheDocument())
  })

  it('does not expose a write escape hatch', async () => {
    render(
        <Rbac3Provider client={client}>
        <ActionGuard permission="orders:write">write</ActionGuard>
      </Rbac3Provider>,
    )

    await waitFor(() => expect(screen.queryByText('write')).not.toBeInTheDocument())
  })

  it('uses server-masked values and never masks a plaintext value in the browser', async () => {
    render(
        <Rbac3Provider client={client}>
        <FieldGuard
          policyKey="orders:detail"
          fieldCode="accountNo"
          value="6222021234567890"
          maskedValue="6222 **** **** 7890"
        />
      </Rbac3Provider>,
    )

    await waitFor(() => expect(screen.getByText('6222 **** **** 7890')).toBeInTheDocument())
    expect(screen.queryByText('6222021234567890')).not.toBeInTheDocument()
  })
})
