import {StrictMode} from 'react'
import {createRoot} from 'react-dom/client'
import 'antd/dist/reset.css'
import {AdminThemeProvider, I18nProvider, initI18n, injectTokens} from '@egon-cola/admin-web-shared'
import {App} from './app/App'

injectTokens()

initI18n({
  defaultNS: 'common',
  resources: { 'zh-CN': {} },
})

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <I18nProvider>
            <AdminThemeProvider>
                <App/>
            </AdminThemeProvider>
        </I18nProvider>
    </StrictMode>,
)
