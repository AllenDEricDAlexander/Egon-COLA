import {Rbac3Provider} from '@egon-cola/tianquan-jianshen-react-sdk'
import {QueryClientProvider} from '@tanstack/react-query'
import {BrowserRouter} from 'react-router-dom'
import {AppErrorBoundary} from '@egon-cola/xingyuan-admin-web-shared'
import {createAdminApiClients} from '../api/adminApiClient'
import {AuthenticationShell} from '../features/auth/AuthenticationShell'
import {FeatureApiProvider} from '../features/shared/FeatureApi'
import {createAdminQueryClient} from './queryClient'
import {ApplicationRouter} from './router'

const queryClient = createAdminQueryClient()
const clients = createAdminApiClients(import.meta.env.VITE_TIANQUAN_JIANSHEN_API_BASE ?? '')

export const App = ({ embedded = false }: { readonly embedded?: boolean }) => (
    <QueryClientProvider client={queryClient}>
        <Rbac3Provider client={clients.rbac3Client}>
            <FeatureApiProvider client={clients.featureClient}>
                <BrowserRouter>
                    <AppErrorBoundary onError={(error, info) => {
                        console.error('[Tianquan-Jianshen] Unhandled error:', error, info.componentStack)
                    }}>
                        <AuthenticationShell>
                            <ApplicationRouter embedded={embedded}/>
                        </AuthenticationShell>
                    </AppErrorBoundary>
                </BrowserRouter>
            </FeatureApiProvider>
        </Rbac3Provider>
    </QueryClientProvider>
)
