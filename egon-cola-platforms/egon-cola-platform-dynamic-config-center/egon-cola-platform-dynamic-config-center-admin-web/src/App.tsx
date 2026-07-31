import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { RequireAuth } from './auth/RouteGuards'
import AdminLayout from './layouts/AdminLayout'
import RegistryPage from './pages/RegistryPage'
import ConfigsPage from './pages/ConfigsPage'
import BizsPage from './pages/BizsPage'
import EnvPage from './pages/EnvPage'
import AppsPage from './pages/AppsPage'
import NamespacesPage from './pages/NamespacesPage'
import PublishTasksPage from './pages/PublishTasksPage'
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
            <Route path="bizs" element={<BizsPage />} />
            <Route path="envs" element={<EnvPage />} />
            <Route path="apps" element={<AppsPage />} />
            <Route path="namespaces" element={<NamespacesPage />} />
            <Route path="publish-tasks" element={<PublishTasksPage />} />
            <Route path="cache" element={<CachePage />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
