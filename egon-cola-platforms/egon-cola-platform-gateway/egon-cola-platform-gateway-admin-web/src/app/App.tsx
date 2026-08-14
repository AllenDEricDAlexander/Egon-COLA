import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {ConfigProvider, Result} from 'antd'
import zhCN from 'antd/locale/zh_CN'
import {lazy, Suspense} from 'react'
import {createBrowserRouter, Navigate, RouterProvider} from 'react-router-dom'
import {CapabilityProvider} from './capabilities'
import {AuthProvider} from '../auth/AuthContext'
import {LoginPage} from '../auth/LoginPage'
import {RequireAuth, RequireCapability} from '../auth/RouteGuards'
import {LoadingBlock} from '../components/QueryState'
import {AdminLayout} from '../layouts/AdminLayout'

const DashboardPage = lazy(() =>
  import('../features/dashboard/DashboardPage').then((module) => ({
    default: module.DashboardPage,
  })),
)
const GatewayGroupsPage = lazy(() =>
  import('../features/gateway-groups/GatewayGroupsPage').then((module) => ({
    default: module.GatewayGroupsPage,
  })),
)
const GatewayGroupDetailPage = lazy(() =>
  import('../features/gateway-groups/GatewayGroupDetailPage').then((module) => ({
    default: module.GatewayGroupDetailPage,
  })),
)
const CatalogPage = lazy(() =>
  import('../features/interface-catalog/CatalogPage').then((module) => ({
    default: module.CatalogPage,
  })),
)
const OperationPage = lazy(() =>
  import('../features/interface-catalog/OperationPage').then((module) => ({
    default: module.OperationPage,
  })),
)
const DraftPage = lazy(() =>
  import('../features/draft/DraftPage').then((module) => ({
    default: module.DraftPage,
  })),
)
const ReleasesPage = lazy(() =>
  import('../features/releases/ReleasesPage').then((module) => ({
    default: module.ReleasesPage,
  })),
)
const ReleaseDetailPage = lazy(() =>
  import('../features/releases/ReleaseDetailPage').then((module) => ({
    default: module.ReleaseDetailPage,
  })),
)
const ProvidersPage = lazy(() =>
  import('../features/providers/ProvidersPage').then((module) => ({
    default: module.ProvidersPage,
  })),
)
const TracesPage = lazy(() =>
  import('../features/observability/TracesPage').then((module) => ({
    default: module.TracesPage,
  })),
)
const AuditPage = lazy(() =>
  import('../features/audit/AuditPage').then((module) => ({
    default: module.AuditPage,
  })),
)
const ApplicationsPage = lazy(() =>
  import('../features/applications/ApplicationsPage').then((module) => ({
    default: module.ApplicationsPage,
  })),
)
const McpServersPage = lazy(() =>
  import('../features/mcp/McpServersPage').then((module) => ({
    default: module.McpServersPage,
  })),
)
const McpServerWorkbenchPage = lazy(() =>
  import('../features/mcp/McpServerWorkbenchPage').then((module) => ({
    default: module.McpServerWorkbenchPage,
  })),
)
const McpRemoteProvidersPage = lazy(() =>
  import('../features/mcp/McpRemoteProvidersPage').then((module) => ({
    default: module.McpRemoteProvidersPage,
  })),
)

const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    path: '/',
    element: (
      <RequireAuth>
        <RequireCapability capability="gateway:read">
          <AdminLayout />
        </RequireCapability>
      </RequireAuth>
    ),
    errorElement: <Result status="error" title="页面加载失败" subTitle="请返回上一页或刷新。" />,
    children: [
      { index: true, element: <Navigate replace to="/dashboard" /> },
      { path: 'dashboard', element: <DashboardPage /> },
      { path: 'gateway-groups', element: <GatewayGroupsPage /> },
      {
        path: 'applications',
        element: <RequireCapability capability="gateway:read"><ApplicationsPage /></RequireCapability>,
      },
      { path: 'gateway-groups/:groupId/overview', element: <GatewayGroupDetailPage /> },
      { path: 'gateway-groups/:groupId/draft/routes', element: <DraftPage /> },
      { path: 'gateway-groups/:groupId/draft/policies', element: <DraftPage /> },
      { path: 'gateway-groups/:groupId/releases', element: <ReleasesPage /> },
      {
        path: 'gateway-groups/:groupId/releases/:releaseId',
        element: <ReleaseDetailPage />,
      },
      { path: 'interface-catalog', element: <CatalogPage /> },
      { path: 'applications/:applicationId/catalog', element: <CatalogPage /> },
      { path: 'operations/:operationId', element: <OperationPage /> },
      { path: 'providers', element: <ProvidersPage /> },
      {
        path: 'mcp/servers',
        element: (
          <RequireCapability capability="gateway:mcp:read">
            <McpServersPage />
          </RequireCapability>
        ),
      },
      {
        path: 'mcp/servers/:serverId',
        element: (
          <RequireCapability capability="gateway:mcp:read">
            <McpServerWorkbenchPage />
          </RequireCapability>
        ),
      },
      {
        path: 'mcp/remote-providers',
        element: (
          <RequireCapability capability="gateway:mcp:read">
            <McpRemoteProvidersPage />
          </RequireCapability>
        ),
      },
      { path: 'observability/traces', element: <TracesPage /> },
      {
        path: 'audit',
        element: <RequireCapability capability="gateway:read"><AuditPage /></RequireCapability>,
      },
      { path: '*', element: <Result status="404" title="404" subTitle="页面不存在" /> },
    ],
  },
])

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => {
        const status = (error as { status?: number }).status
        return failureCount < 2 && (status === 429 || status === undefined || status >= 500)
      },
      staleTime: 5_000,
      refetchOnWindowFocus: false,
    },
    mutations: { retry: false },
  },
})

export const App = () => (
  <ConfigProvider
    locale={zhCN}
    theme={{
      token: {
        colorPrimary: '#3157d5',
        borderRadius: 8,
        colorBgLayout: '#f4f6fa',
      },
    }}
  >
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <CapabilityProvider>
          <Suspense fallback={<LoadingBlock />}>
            <RouterProvider router={router} />
          </Suspense>
        </CapabilityProvider>
      </AuthProvider>
    </QueryClientProvider>
  </ConfigProvider>
)
