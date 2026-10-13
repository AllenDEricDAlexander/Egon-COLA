import {useEffect, useState} from 'react'
import {createBrowserRouter, Navigate, RouterProvider} from 'react-router-dom'
import {AuthProvider} from './auth/AuthContext'
import {RequireAuth} from './auth/RouteGuards'
import AdminLayout from './layouts/AdminLayout'
import RegistryPage from './pages/RegistryPage'
import ConfigsPage from './pages/ConfigsPage'
import BizsPage from './pages/BizsPage'
import EnvPage from './pages/EnvPage'
import AppsPage from './pages/AppsPage'
import NamespacesPage from './pages/NamespacesPage'
import PublishTasksPage from './pages/PublishTasksPage'
import CachePage from './pages/CachePage'
import InstancesPage from './pages/InstancesPage'
import BindingsPage from './pages/BindingsPage'
import AiResourcesPage from './pages/AiResourcesPage'
import AiResourceEditorPage from './pages/AiResourceEditorPage'

const configuredBase: string = import.meta.env.VITE_BASE_PATH ?? '/'
if (!configuredBase.startsWith('/') || configuredBase.startsWith('//')
  || /[\\?#%]/.test(configuredBase) || Array.from(configuredBase).some((character) => character.charCodeAt(0) < 32 || character.charCodeAt(0) === 127) || /\/{2,}/.test(configuredBase)
  || configuredBase.split('/').some((segment) => segment === '.' || segment === '..')) {
  throw new Error('Invalid VITE_BASE_PATH')
}
const basePath = configuredBase === '/' ? '/' : configuredBase.replace(/\/?$/, '/')

const routes = [{
  path: '/',
  element: (
    <RequireAuth>
      <AdminLayout />
    </RequireAuth>
  ),
  children: [
    { index: true, element: <Navigate to="/registry" replace /> },
    { path: 'registry', element: <RegistryPage /> },
    { path: 'configs', element: <ConfigsPage /> },
    { path: 'bizs', element: <BizsPage /> },
    { path: 'envs', element: <EnvPage /> },
    { path: 'apps', element: <AppsPage /> },
    { path: 'namespaces', element: <NamespacesPage /> },
    { path: 'bindings', element: <BindingsPage /> },
    { path: 'publish-tasks', element: <PublishTasksPage /> },
    { path: 'cache', element: <CachePage /> },
    { path: 'instances', element: <InstancesPage /> },
    { path: 'ai/prompts', element: <AiResourcesPage kind="PROMPT" /> },
    { path: 'ai/skills', element: <AiResourcesPage kind="SKILL" /> },
    { path: 'ai/resources/:id', element: <AiResourceEditorPage /> },
  ],
}]

export default function App() {
  const [router, setRouter] = useState<ReturnType<typeof createBrowserRouter> | null>(null)

  useEffect(() => {
    const currentRouter = createBrowserRouter(routes, {basename: basePath})
    setRouter(currentRouter)
    return () => currentRouter.dispose()
  }, [])

  if (!router) return null
  return (
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>
  )
}
