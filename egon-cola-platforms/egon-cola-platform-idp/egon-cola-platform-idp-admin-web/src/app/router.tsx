import {Spin} from 'antd'
import {lazy, Suspense} from 'react'
import {Navigate, Outlet, Route, Routes} from 'react-router-dom'
import {AppErrorBoundary} from '@egon-cola/admin-web-shared'
import {AuthProvider, useAuth} from '../auth/AuthContext'
import {AdminLayout} from './AdminLayout'

const CentralLoginPage = lazy(() => import('../auth/CentralLoginPage').then(m => ({ default: m.CentralLoginPage })))
const OverviewPage = lazy(() => import('../features/overview/OverviewPage').then(m => ({ default: m.OverviewPage })))
const UserListPage = lazy(() => import('../features/users/UserListPage').then(m => ({ default: m.UserListPage })))
const ClientListPage = lazy(() => import('../features/clients/ClientListPage').then(m => ({ default: m.ClientListPage })))
const ResourceServerListPage = lazy(() => import('../features/resource-servers/ResourceServerListPage').then(m => ({default: m.ResourceServerListPage})))
const ClientResourceGrantPage = lazy(() => import('../features/resource-grants/ClientResourceGrantPage').then(m => ({default: m.ClientResourceGrantPage})))
const SigningKeyPage = lazy(() => import('../features/keys/SigningKeyPage').then(m => ({ default: m.SigningKeyPage })))
const AuditLogPage = lazy(() => import('../features/audits/AuditLogPage').then(m => ({ default: m.AuditLogPage })))

const PageFallback = () => <Spin style={{ display: 'grid', placeItems: 'center', minHeight: 200 }} />

const LoginRoute = () => {
  const auth = useAuth()
  if (auth.loading) return <Spin fullscreen description="校验统一登录态" />
  if (auth.bootstrap) return <Navigate to="/overview" replace />
  return <CentralLoginPage />
}

const ConsoleGuard = () => {
  const auth = useAuth()
  if (auth.loading) return <Spin fullscreen description="校验统一登录态" />
  if (!auth.bootstrap) return <Navigate to="/login" replace />
  return (
    <AdminLayout>
      <Outlet />
    </AdminLayout>
  )
}

export const AppRouter = () => (
  <AppErrorBoundary>
    <AuthProvider>
      <Suspense fallback={<PageFallback />}>
        <Routes>
          <Route path="/login" element={<LoginRoute />} />
          <Route element={<ConsoleGuard />}>
            <Route index element={<Navigate to="/overview" replace />} />
            <Route path="/overview" element={<OverviewPage />} />
            <Route path="/users" element={<UserListPage />} />
            <Route path="/clients" element={<ClientListPage />} />
              <Route path="/clients/:clientId/resource-grants" element={<ClientResourceGrantPage/>}/>
              <Route path="/resource-servers" element={<ResourceServerListPage/>}/>
            <Route path="/keys" element={<SigningKeyPage />} />
            <Route path="/audits" element={<AuditLogPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/overview" replace />} />
        </Routes>
      </Suspense>
    </AuthProvider>
  </AppErrorBoundary>
)
