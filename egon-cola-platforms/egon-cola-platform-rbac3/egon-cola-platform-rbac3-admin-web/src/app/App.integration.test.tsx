import {type Rbac3AboutView, type Rbac3Client, Rbac3Provider} from '@egon-cola/rbac3-react-sdk'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {cleanup, fireEvent, render, screen, waitFor} from '@testing-library/react'
import {MemoryRouter} from 'react-router-dom'
import {describe, expect, it, vi} from 'vitest'
import {type FeatureApiClient, FeatureApiProvider} from '../features/shared/FeatureApi'
import {ApplicationRouter} from './router'
import {applicationRouteDescriptors, localResourceRegistry, resolveApplicationLanding} from './navigation'

const about = (permissions: readonly string[], resourceCodes?: readonly string[]): Rbac3AboutView => ({
  user: { subject: 'mario', tenantId: '9', status: 'ACTIVE' },
  currentApplicationCode: 'rbac3-admin',
  activeRoles: [],
  permissions,
  resourceCodes: resourceCodes ?? localResourceRegistry.definitions
    .filter((definition) => definition.suggestedPermissionCode !== null
      && definition.suggestedPermissionCode !== undefined
      && permissions.includes(definition.suggestedPermissionCode))
    .map((definition) => definition.code),
  fieldPolicies: {},
  landingRouteCode: null,
  authVersion: 1,
  policyVersion: 3,
})

const wrapper = (permissions: readonly string[], path: string, resourceCodes?: readonly string[]) => ({ children }: { readonly children: React.ReactNode }) => {
    const sdk = {getAbout: async () => about(permissions, resourceCodes)} as unknown as Rbac3Client
  const feature: FeatureApiClient = { request: async <T,>(requestPath: string) => {
    if (requestPath === '/api/rbac3/v1/iam/organizations') {
      return [{
        orgUnitId: '1001', snapshotId: null, type: 'DEPARTMENT', code: 'hq', name: '总部',
        parentId: null, path: 'hq', depth: 0, status: 'ACTIVE',
      }] as T
    }
    if (requestPath === '/api/rbac3/v1/iam/positions') {
      return [{
        positionId: '2001', snapshotId: null, code: 'ops-admin', name: '运营主管',
        orgUnitId: '1001', status: 'ACTIVE',
      }] as T
    }
    if (requestPath.endsWith('/resources')) {
      return {
        roleId: '301', applicationId: '71', roleVersion: 1,
        directResourceIds: [], derivedApiResourceIds: [],
        summary: {menuPageCount: 0, actionCount: 0, apiCount: 0, inheritedCount: 0}, nodes: [],
      } as T
    }
    return ({definition: {status: 'ACCEPTED', warnings: []}, providerLease: {state: 'ACTIVE'}, gatewayRelease: {status: 'ROUTABLE'}}) as T
  } }
    return <QueryClientProvider client={new QueryClient()}><Rbac3Provider client={sdk}><FeatureApiProvider
        client={feature}><MemoryRouter
        initialEntries={[path]}>{children}</MemoryRouter></FeatureApiProvider></Rbac3Provider></QueryClientProvider>
}

describe('application router', () => {
  it('keeps IAM navigation inside the embedded RBAC3 child', async () => {
    const originalMatchMedia = window.matchMedia
    Object.defineProperty(window, 'matchMedia', {
      configurable: true,
      writable: true,
      value: vi.fn().mockImplementation((query: string) => ({
        matches: query.includes('min-width'),
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    })
    try {
      render(<ApplicationRouter embedded />, {
        wrapper: wrapper(['system:runtime:read', 'system:user:read', 'system:role:read'], '/iam/overview'),
      })
      await waitFor(() => expect(screen.getByText('IAM')).toBeInTheDocument())
      fireEvent.click(screen.getByText('目录'))
      expect(screen.getByText('用户')).toBeInTheDocument()
      expect(screen.getByText('IAM')).toBeInTheDocument()
      expect(screen.queryByText('RBAC3 权限平台')).not.toBeInTheDocument()
    } finally {
      Object.defineProperty(window, 'matchMedia', {
        configurable: true,
        writable: true,
        value: originalMatchMedia,
      })
    }
  })

  it('maps organization and position routes to their own page responsibilities', async () => {
    render(<ApplicationRouter />, {wrapper: wrapper(['system:organization:read'], '/iam/organizations')})
    await waitFor(() => expect(screen.getByText('总部')).toBeInTheDocument())
    expect(screen.queryByLabelText('用户 ID')).not.toBeInTheDocument()
    cleanup()

    render(<ApplicationRouter />, {wrapper: wrapper(['system:position:read'], '/iam/positions')})
    await waitFor(() => expect(screen.getByText('运营主管')).toBeInTheDocument())
    expect(screen.queryByLabelText('用户 ID')).not.toBeInTheDocument()
  })

  it('blocks a manually entered route whose permission is absent', async () => {
    render(<ApplicationRouter />, { wrapper: wrapper([], '/iam/policies') })
    await waitFor(() => expect(screen.getByText('无权访问此页面')).toBeInTheDocument())
  })

  it('chooses the stable first accessible local route when no default is usable', () => {
    expect(resolveApplicationLanding(about(['system:runtime:read']))).toBe('/iam/overview')
  })

  it('removes the RBAC tenant catalog route and browser resource definition', () => {
    expect(applicationRouteDescriptors.some((route) => route.path === '/iam/tenants')).toBe(false)
    expect(localResourceRegistry.definitions.some((definition) => definition.code === 'iam.tenants')).toBe(false)
  })

  it('does not expose a browser resource report or synchronization action', async () => {
    render(<ApplicationRouter />, { wrapper: wrapper(['system:runtime:read'], '/iam/overview') })
    await waitFor(() => expect(screen.getByText('权限治理概览')).toBeInTheDocument())
    expect(screen.queryByRole('button', { name: /sync|report|上报|同步/i })).not.toBeInTheDocument()
  })

  it('does not expose the removed role-permissions route', async () => {
    render(<ApplicationRouter />, { wrapper: wrapper(['system:role:read'], '/iam/roles/2/permissions') })
    await waitFor(() => expect(screen.queryByText('角色权限与影响分析')).not.toBeInTheDocument())
  })

  it('renders the authorized resource tree and selects the role parent for hidden details', async () => {
    render(<ApplicationRouter />, { wrapper: wrapper(['system:role:read'], '/iam/roles') })
    await waitFor(() => expect(screen.getByText('角色图谱')).toBeInTheDocument())
    fireEvent.click(screen.getByRole('button', {name: '打开导航'}))
    fireEvent.click(screen.getByText('IAM'))
    fireEvent.click(screen.getByText('授权'))
    expect(screen.getByText('角色')).toBeInTheDocument()
    cleanup()

    render(<ApplicationRouter />, {
      wrapper: wrapper(['system:role:read', 'system:role-resource:read'], '/iam/roles/301/resources'),
    })
    await waitFor(() => expect(screen.getByRole('button', {name: '打开导航'})).toBeInTheDocument())
    fireEvent.click(screen.getByRole('button', {name: '打开导航'}))
    await waitFor(() => expect(screen.getByText('角色').closest('.ant-menu-item')).toHaveClass('ant-menu-item-selected'))
  })

  it('does not render an empty rail when the user has no visible route', async () => {
    render(<ApplicationRouter />, {wrapper: wrapper([], '/iam/roles', [])})
    await waitFor(() => expect(screen.getByText('无权访问此页面')).toBeInTheDocument())
    expect(screen.queryByRole('button', {name: '打开导航'})).not.toBeInTheDocument()
    expect(screen.queryByRole('navigation', {name: '主菜单'})).not.toBeInTheDocument()
  })
})
