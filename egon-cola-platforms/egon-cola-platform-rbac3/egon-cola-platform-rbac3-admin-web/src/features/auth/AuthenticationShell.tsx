import { useRbac3Session } from '@egon-cola/rbac3-react-sdk'
import { Button, Result, Spin } from 'antd'
import type { PropsWithChildren } from 'react'
import { RoleActivationPage } from '../role-activation/RoleActivationPage'
import { LoginPage } from './LoginPage'

export interface AuthenticationShellProps extends PropsWithChildren {}

export const AuthenticationShell = ({ children }: AuthenticationShellProps) => {
  const session = useRbac3Session()
  if (['UNINITIALIZED', 'LOADING_BOOTSTRAP', 'REFRESHING_VERSION'].includes(session.status)) {
    return <main className="rbac3-centered"><Spin size="large" description="正在重建安全会话" /></main>
  }
  if (session.status === 'AUTHENTICATION_REQUIRED') return <LoginPage />
  if (['ACTIVATION_REQUIRED', 'REPLACING_ACTIVE_ROLES'].includes(session.status)) {
    return <main className="rbac3-activation-page"><RoleActivationPage /><Button style={{ marginTop: 16 }} onClick={() => void session.logout()}>退出登录</Button></main>
  }
  if (session.status === 'FORBIDDEN_NO_ROUTE') return <Result status="403" title="没有可访问页面" subTitle={session.errorCode} extra={<Button onClick={() => void session.logout()}>退出登录</Button>} />
  if (session.status === 'ERROR_RETRYABLE') return <Result status="warning" title="授权状态暂不可用" subTitle={session.errorCode} extra={<Button onClick={() => void session.retry()}>重试</Button>} />
  if (session.status === 'ERROR_FATAL') return <Result status="error" title="授权状态无法恢复" subTitle={session.errorCode} extra={<Button onClick={() => void session.logout()}>重新登录</Button>} />
  return children
}
