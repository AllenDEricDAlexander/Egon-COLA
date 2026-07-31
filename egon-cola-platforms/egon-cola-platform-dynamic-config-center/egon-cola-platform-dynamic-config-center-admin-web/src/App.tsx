import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { RequireAuth } from './auth/RouteGuards'
import AdminLayout from './layouts/AdminLayout'
import RegistryPage from './pages/RegistryPage'
import ConfigsPage from './pages/ConfigsPage'
import AppsPage from './pages/AppsPage'
import NamespacesPage from './pages/NamespacesPage'
import PublishTasksPage from './pages/PublishTasksPage'
import InstancesPage from './pages/InstancesPage'
import CachePage from './pages/CachePage'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route
            path="/"
            element={
              <RequireAuth>
                <AdminLayout />
              </RequireAuth>
            }
          >
            <Route index element={<Navigate to="/registry" replace />} />
            <Route path="registry" element={<RegistryPage />} />
            <Route path="configs" element={<ConfigsPage />} />
            <Route path="apps" element={<AppsPage />} />
            <Route path="namespaces" element={<NamespacesPage />} />
            <Route path="publish-tasks" element={<PublishTasksPage />} />
            <Route path="instances" element={<InstancesPage />} />
            <Route path="cache" element={<CachePage />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
