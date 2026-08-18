import {type Rbac3AboutView, type Rbac3Client, Rbac3Provider} from '@egon-cola/rbac3-react-sdk'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {render, screen, waitFor} from '@testing-library/react'
import {MemoryRouter} from 'react-router-dom'
import {describe, expect, it} from 'vitest'
import {type FeatureApiClient, FeatureApiProvider} from '../features/shared/FeatureApi'
import {ApplicationRouter} from './router'
import {resolveApplicationLanding} from './navigation'

const about = (permissions: readonly string[]): Rbac3AboutView => ({
  user: { subject: 'mario', tenantId: '9', status: 'ACTIVE' },
  currentApplicationCode: 'rbac3-admin',
  activeRoles: [],
  permissions,
  fieldPolicies: {},
  landingRouteCode: null,
  authVersion: 1,
  policyVersion: 3,
})

const wrapper = (permissions: readonly string[], path: string) => ({ children }: { readonly children: React.ReactNode }) => {
    const sdk = {getAbout: async () => about(permissions)} as unknown as Rbac3Client
  const feature: FeatureApiClient = { request: async <T,>() => ({ definition: { status: 'ACCEPTED', warnings: [] }, providerLease: { state: 'ACTIVE' }, gatewayRelease: { status: 'ROUTABLE' } }) as T }
    return <QueryClientProvider client={new QueryClient()}><Rbac3Provider client={sdk}><FeatureApiProvider
        client={feature}><MemoryRouter
        initialEntries={[path]}>{children}</MemoryRouter></FeatureApiProvider></Rbac3Provider></QueryClientProvider>
}

describe('application router', () => {
  it('blocks a manually entered route whose permission is absent', async () => {
    render(<ApplicationRouter />, { wrapper: wrapper([], '/iam/policies') })
    await waitFor(() => expect(screen.getByText('无权访问此页面')).toBeInTheDocument())
  })

  it('chooses the stable first accessible local route when no default is usable', () => {
    expect(resolveApplicationLanding(about(['system:tenant:read']))).toBe('/iam/tenants')
  })

  it('does not expose a browser resource report or synchronization action', async () => {
    render(<ApplicationRouter />, { wrapper: wrapper(['system:runtime:read'], '/iam/overview') })
    await waitFor(() => expect(screen.getByText('权限治理概览')).toBeInTheDocument())
    expect(screen.queryByRole('button', { name: /sync|report|上报|同步/i })).not.toBeInTheDocument()
  })
})
