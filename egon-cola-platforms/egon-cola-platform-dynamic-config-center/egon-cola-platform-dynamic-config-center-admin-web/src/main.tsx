import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClientProvider } from '@tanstack/react-query'
import { App as AntdApp } from 'antd'
import 'antd/dist/reset.css'
import { AdminThemeProvider, injectTokens, initI18n, I18nProvider } from '@egon-cola/admin-web-shared'
import App from './App'
import { queryClient } from './query/queryClient'
import './styles/admin.css'

injectTokens()

initI18n({
  defaultNS: 'common',
  resources: { 'zh-CN': {} },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <I18nProvider>
      <AdminThemeProvider>
        <QueryClientProvider client={queryClient}>
          <AntdApp>
            <App />
          </AntdApp>
        </QueryClientProvider>
      </AdminThemeProvider>
    </I18nProvider>
  </StrictMode>,
)
