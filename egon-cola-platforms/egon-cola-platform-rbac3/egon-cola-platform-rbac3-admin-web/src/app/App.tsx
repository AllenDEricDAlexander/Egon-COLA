import { Rbac3Provider } from '@egon-cola/rbac3-react-sdk'
import { QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { createAdminApiClients } from '../api/adminApiClient'
import { AuthenticationShell } from '../features/auth/AuthenticationShell'
import { FeatureApiProvider } from '../features/shared/FeatureApi'
import { AppErrorBoundary } from './AppErrorBoundary'
import { createAdminQueryClient } from './queryClient'
import { ApplicationRouter } from './router'

const queryClient = createAdminQueryClient()
const clients = createAdminApiClients(import.meta.env.VITE_RBAC3_API_BASE ?? '')

export const App = () => (
  <QueryClientProvider client={queryClient}>
    <Rbac3Provider client={clients.rbac3Client} accessTokenStore={clients.accessTokenStore}>
      <FeatureApiProvider client={clients.featureClient}>
        <BrowserRouter>
          <AppErrorBoundary>
            <AuthenticationShell authApi={clients.authApi}>
              <ApplicationRouter />
            </AuthenticationShell>
          </AppErrorBoundary>
        </BrowserRouter>
      </FeatureApiProvider>
    </Rbac3Provider>
  </QueryClientProvider>
)
