import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import 'antd/dist/reset.css'
import { initI18n, injectTokens } from '@egon-cola/admin-web-shared'
import { App } from './app/App'

injectTokens()

void initI18n({
  defaultNS: 'common',
  resources: { 'zh-CN': {} },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
