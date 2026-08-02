import { ConfigProvider, Spin } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { useEffect, useState } from 'react'
import { AuthProvider, useAuth } from '../auth/AuthContext'
import { CentralLoginPage } from '../auth/CentralLoginPage'
import { AdminConsole } from './AdminConsole'

const ConsoleRoute = () => {
  const auth = useAuth()
  if (auth.loading) return <Spin fullscreen description="校验统一登录态" />
  if (!auth.bootstrap) {
    window.history.replaceState({}, '', '/login')
    window.dispatchEvent(new PopStateEvent('popstate'))
    return null
  }
  return <AdminConsole />
}

const ApplicationRoutes = () => {
  const [pathname, setPathname] = useState(window.location.pathname)
  useEffect(() => {
    const update = () => setPathname(window.location.pathname)
    window.addEventListener('popstate', update)
    return () => window.removeEventListener('popstate', update)
  }, [])
  if (pathname === '/login') return <CentralLoginPage />
  if (pathname === '/oauth/callback') {
    return <Spin fullscreen description="完成统一身份登录" />
  }
  return <ConsoleRoute />
}

export const App = () => (
  <ConfigProvider locale={zhCN} theme={{ token: { colorPrimary: '#2447b8' } }}>
    <AuthProvider><ApplicationRoutes /></AuthProvider>
  </ConfigProvider>
)
