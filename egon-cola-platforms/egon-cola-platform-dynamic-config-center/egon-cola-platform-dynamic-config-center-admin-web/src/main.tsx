import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import 'antd/dist/reset.css'
import { AdminThemeProvider, injectTokens, initI18n, I18nProvider } from '@egon-cola/admin-web-shared'
import App from './App'

injectTokens()

initI18n({
  defaultNS: 'common',
  resources: { 'zh-CN': {} },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <I18nProvider>
      <AdminThemeProvider>
        <App />
      </AdminThemeProvider>
    </I18nProvider>
  </StrictMode>,
)
