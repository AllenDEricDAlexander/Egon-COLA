import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import type { ComponentType, PropsWithChildren } from 'react'
import {
  AdminThemeProvider,
  AppErrorBoundary,
  I18nProvider,
} from '@egon-cola/xingyuan-admin-web-shared'
import { PortalRouter } from './router'

const configuredBase: string = import.meta.env.VITE_BASE_PATH ?? '/'
if (!configuredBase.startsWith('/') || configuredBase.startsWith('//')
  || /[\\?#%]/.test(configuredBase) || Array.from(configuredBase).some((character) => character.charCodeAt(0) < 32 || character.charCodeAt(0) === 127) || /\/{2,}/.test(configuredBase)
  || configuredBase.split('/').some((segment) => segment === '.' || segment === '..')) {
  throw new Error('Invalid VITE_BASE_PATH')
}
const basePath = configuredBase === '/' ? '/' : configuredBase.replace(/\/?$/, '/')

// The linked shared package can resolve a second React type declaration tree during tsc;
// the runtime component remains the shared error boundary, while this cast keeps the
// Portal's public provider graph compatible with the current workspace package layout.
const PortalErrorBoundary = AppErrorBoundary as unknown as ComponentType<PropsWithChildren>

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: false,
      staleTime: 10_000,
    },
    mutations: { retry: false },
  },
})

export const App = () => (
  <QueryClientProvider client={queryClient}>
    <AdminThemeProvider>
      <I18nProvider>
        <BrowserRouter basename={basePath}>
          <PortalErrorBoundary>
            <PortalRouter />
          </PortalErrorBoundary>
        </BrowserRouter>
      </I18nProvider>
    </AdminThemeProvider>
  </QueryClientProvider>
)
