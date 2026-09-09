import {StrictMode} from 'react'
import {createRoot, type Root} from 'react-dom/client'
import 'antd/dist/reset.css'
import {AdminThemeProvider, I18nProvider, initI18n, injectTokens} from '@egon-cola/xingyuan-admin-web-shared'
import {App} from './app/App'

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
    $wujie?: { props?: { embedded?: boolean } }
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
                    <App/>
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
