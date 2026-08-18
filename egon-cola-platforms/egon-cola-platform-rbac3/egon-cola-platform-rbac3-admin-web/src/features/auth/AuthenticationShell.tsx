import {useRbac3Authorization} from '@egon-cola/rbac3-react-sdk'
import {Button, Result, Spin} from 'antd'
import type {PropsWithChildren} from 'react'
import {RoleActivationPage} from '../role-activation/RoleActivationPage'
import {LoginPage} from './LoginPage'
import {gatewayAuth} from './gatewayAuth'

export type AuthenticationShellProps = PropsWithChildren

export const AuthenticationShell = ({ children }: AuthenticationShellProps) => {
    const authorization = useRbac3Authorization()
    const logout = async () => {
        await gatewayAuth.logout()
        window.location.assign('/')
    }
    if (['UNINITIALIZED', 'LOADING_ABOUT'].includes(authorization.status)) {
        return <main className="rbac3-centered"><Spin size="large" description="正在加载授权上下文"/></main>
  }
    if (authorization.status === 'AUTHENTICATION_REQUIRED') {
        return <LoginPage onSuccess={() => authorization.retry()}/>
  }
    if (['ACTIVATION_REQUIRED', 'REPLACING_ACTIVE_ROLES'].includes(authorization.status)) {
        return <main className="rbac3-activation-page"><RoleActivationPage/><Button style={{marginTop: 16}}
                                                                                    onClick={() => void logout()}>退出登录</Button>
        </main>
    }
    if (authorization.status === 'FORBIDDEN_NO_ROUTE') return <Result status="403" title="没有可访问页面"
                                                                      subTitle={authorization.errorCode} extra={<Button
        onClick={() => void logout()}>退出登录</Button>}/>
    if (authorization.status === 'ERROR_RETRYABLE') return <Result status="warning" title="授权状态暂不可用"
                                                                   subTitle={authorization.errorCode} extra={<Button
        onClick={() => void authorization.retry()}>重试</Button>}/>
    if (authorization.status === 'ERROR_FATAL') return <Result status="error" title="授权状态无法恢复"
                                                               subTitle={authorization.errorCode} extra={<Button
        onClick={() => void logout()}>重新登录</Button>}/>
  return children
}
