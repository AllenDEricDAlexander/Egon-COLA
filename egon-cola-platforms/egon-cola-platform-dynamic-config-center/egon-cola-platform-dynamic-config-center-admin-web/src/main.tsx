import { StrictMode } from 'react'
import { createRoot, type Root } from 'react-dom/client'
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

interface WujieRuntimeWindow extends Window {
  __POWERED_BY_WUJIE__?: boolean
  __WUJIE_MOUNT?: () => void
  __WUJIE_UNMOUNT?: () => void
  __WUJIE?: { mount?: () => void }
}

const runtimeWindow = window as WujieRuntimeWindow
let root: Root | undefined

const mount = (): void => {
  if (root) return
  root = createRoot(document.getElementById('root')!)
  root.render(
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
}

const unmount = (): void => {
  root?.unmount()
  root = undefined
}

if (runtimeWindow.__POWERED_BY_WUJIE__) {
  runtimeWindow.__WUJIE_MOUNT = mount
  runtimeWindow.__WUJIE_UNMOUNT = unmount
  runtimeWindow.__WUJIE?.mount?.()
} else {
  mount()
}
