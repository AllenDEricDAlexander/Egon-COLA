import {type Rbac3Client, Rbac3Provider} from '@egon-cola/rbac3-react-sdk'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {fireEvent, render, screen, waitFor} from '@testing-library/react'
import type {PropsWithChildren} from 'react'
import {describe, expect, it} from 'vitest'
import {type FeatureApiClient, FeatureApiProvider} from '../shared/FeatureApi'
import {AuditLogPage} from './AuditLogPage'

const wrapper = ({ children }: PropsWithChildren) => {
    const sdk = {
        getBootstrap: async () => ({
            user: {
                id: '7',
                tenantId: '9',
                identitySub: 'audit-test',
                status: 'ACTIVE'
            },
            permissions: ['system:audit:read'],
            fieldPolicies: {},
            activeRoleContexts: [],
            apps: [],
            menus: [],
            routes: [],
            actions: [],
            defaultApplicationCode: null,
            defaultRoute: null,
            authVersion: 1,
            policyVersion: 1
        })
    } as unknown as Rbac3Client
  const api: FeatureApiClient = { request: async <T,>() => ({ items: [{
    id: '601', tenantId: '9', eventType: 'AUTHORIZATION_DENIED', outcome: 'DENY', severity: 'WARN', actorType: 'USER', actorId: '7',
    targetType: 'RESOURCE', targetId: 'orders.list', reasonCode: 'PERMISSION_MISSING', requestId: 'req-1', traceId: 'trace-1',
    beforeSnapshot: { password: 'should-not-render', status: 'ACTIVE' }, afterSnapshot: { token: 'should-not-render', status: 'DENIED' },
    payloadChecksum: 'checksum', createdAt: '2026-08-01T00:00:00Z',
  }], nextCursor: null }) as T }
    return <QueryClientProvider client={new QueryClient()}><Rbac3Provider client={sdk}><FeatureApiProvider
        client={api}>{children}</FeatureApiProvider></Rbac3Provider></QueryClientProvider>
}

describe('audit log page', () => {
  it('uses server filters and redacts sensitive structured values in detail', async () => {
    render(<AuditLogPage />, { wrapper })
    await waitFor(() => expect(screen.getByText('AUTHORIZATION_DENIED')).toBeInTheDocument())
    fireEvent.click(screen.getByRole('button', { name: '查看详情' }))
    expect(await screen.findAllByText('[REDACTED]')).toHaveLength(2)
    expect(document.body.textContent).not.toContain('should-not-render')
    expect(screen.getAllByText('trace-1').length).toBeGreaterThan(0)
  })
})
