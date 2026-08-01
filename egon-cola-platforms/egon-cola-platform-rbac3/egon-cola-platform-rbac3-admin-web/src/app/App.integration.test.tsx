import { InMemoryAccessTokenStore, Rbac3Provider, type BootstrapView, type Rbac3Client } from '@egon-cola/rbac3-react-sdk'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { FeatureApiProvider, type FeatureApiClient } from '../features/shared/FeatureApi'
import { ApplicationRouter } from './router'
import { resolveApplicationLanding } from './navigation'

const bootstrap = (permissions: readonly string[]): BootstrapView => ({
  user: { id: '7', tenantId: '9', username: 'mario', displayName: 'Mario' }, permissions,
  fieldPolicies: {}, activeRoleContexts: [], apps: [], menus: [], routes: [], actions: [],
  defaultApplicationCode: null, defaultRoute: null, sessionId: '11', authVersion: 1, sessionVersion: 2, policyVersion: 3,
})

const wrapper = (permissions: readonly string[], path: string) => ({ children }: { readonly children: React.ReactNode }) => {
  const sdk = { refresh: async () => ({ accessToken: 'access', roleActivationRequired: false }), getBootstrap: async () => bootstrap(permissions) } as unknown as Rbac3Client
  const feature: FeatureApiClient = { request: async <T,>() => ({ definition: { status: 'ACCEPTED', warnings: [] }, providerLease: { state: 'ACTIVE' }, gatewayRelease: { status: 'ROUTABLE' } }) as T }
  return <QueryClientProvider client={new QueryClient()}><Rbac3Provider client={sdk} accessTokenStore={new InMemoryAccessTokenStore()}><FeatureApiProvider client={feature}><MemoryRouter initialEntries={[path]}>{children}</MemoryRouter></FeatureApiProvider></Rbac3Provider></QueryClientProvider>
}

describe('application router', () => {
  it('blocks a manually entered route whose permission is absent', async () => {
    render(<ApplicationRouter />, { wrapper: wrapper([], '/constraints') })
    await waitFor(() => expect(screen.getByText('无权访问此页面')).toBeInTheDocument())
  })

  it('chooses the stable first accessible local route when no default is usable', () => {
    expect(resolveApplicationLanding(bootstrap(['system:tenant:read']))).toBe('/tenants')
  })
})
