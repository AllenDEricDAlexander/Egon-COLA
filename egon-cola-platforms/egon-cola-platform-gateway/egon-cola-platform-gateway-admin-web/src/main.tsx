import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import 'antd/dist/reset.css'
import { AdminThemeProvider, injectTokens, initI18n, I18nProvider } from '@egon-cola/admin-web-shared'
import { App } from './app/App'
import { canonicalOAuthPageUrl } from './auth/oauthOrigin'

injectTokens()

initI18n({
  defaultNS: 'common',
  resources: { 'zh-CN': {} },
})

const canonicalUrl = canonicalOAuthPageUrl(
  window.location.href,
  import.meta.env.VITE_IDP_REDIRECT_URI,
)

if (canonicalUrl) {
  window.location.replace(canonicalUrl)
} else {
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <I18nProvider>
        <AdminThemeProvider>
          <App />
        </AdminThemeProvider>
      </I18nProvider>
    </StrictMode>,
  )
}
