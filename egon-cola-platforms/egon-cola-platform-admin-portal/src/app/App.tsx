import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import type { ComponentType, PropsWithChildren } from 'react'
import {
  AdminThemeProvider,
  AppErrorBoundary,
  I18nProvider,
} from '@egon-cola/admin-web-shared'
import { PortalRouter } from './router'

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
        <BrowserRouter>
          <PortalErrorBoundary>
            <PortalRouter />
          </PortalErrorBoundary>
        </BrowserRouter>
      </I18nProvider>
    </AdminThemeProvider>
  </QueryClientProvider>
)
