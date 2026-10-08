import {useRbac3Authorization} from '@egon-cola/tianquan-jianshen-react-sdk'
import {Alert, Button, Result, Spin} from 'antd'
import type {PropsWithChildren} from 'react'
import {useState} from 'react'
import {RoleActivationPage} from '../role-activation/RoleActivationPage'
import {LoginPage} from './LoginPage'
import {gatewayAuth} from './gatewayAuth'

export type AuthenticationShellProps = PropsWithChildren

export const AuthenticationShell = ({ children }: AuthenticationShellProps) => {
    const authorization = useRbac3Authorization()
    const [logoutNotice, setLogoutNotice] = useState<string>()
    const logout = async () => {
        try {
            const result = await gatewayAuth.logout()
            setLogoutNotice(result.remoteState === 'CONFIRMED'
                ? undefined
                : '本地已退出，远端令牌撤销待恢复')
            await authorization.retry()
        } catch (failure) {
            setLogoutNotice(failure instanceof Error ? failure.message : '统一身份退出失败')
        }
    }
    if (['UNINITIALIZED', 'LOADING_ABOUT'].includes(authorization.status)) {
        return <main className="tianquan-jianshen-centered">
            {logoutNotice && <Alert type="warning" showIcon message={logoutNotice} />}
            <Spin size="large" description="正在加载授权上下文"/>
        </main>
  }
    if (authorization.status === 'AUTHENTICATION_REQUIRED') {
        return <LoginPage notice={logoutNotice}/>
  }
    if (['ACTIVATION_REQUIRED', 'REPLACING_ACTIVE_ROLES'].includes(authorization.status)) {
        return <main className="tianquan-jianshen-activation-page">
            {logoutNotice && <Alert type="warning" showIcon message={logoutNotice} />}
            <RoleActivationPage/><Button style={{marginTop: 16}}
                                                                                    onClick={() => void logout()}>退出登录</Button>
        </main>
    }
    if (authorization.status === 'FORBIDDEN_NO_ROUTE') return <>
        {logoutNotice && <Alert type="warning" showIcon message={logoutNotice} />}
        <Result status="403" title="没有可访问页面" subTitle={authorization.errorCode} extra={<Button
            onClick={() => void logout()}>退出登录</Button>}/>
    </>
    if (authorization.status === 'ERROR_RETRYABLE') return <>
        {logoutNotice && <Alert type="warning" showIcon message={logoutNotice} />}
        <Result status="warning" title="授权状态暂不可用" subTitle={authorization.errorCode} extra={<Button
            onClick={() => void authorization.retry()}>重试</Button>}/>
    </>
    if (authorization.status === 'ERROR_FATAL') return <>
        {logoutNotice && <Alert type="warning" showIcon message={logoutNotice} />}
        <Result status="error" title="授权状态无法恢复" subTitle={authorization.errorCode} extra={<Button
            onClick={() => void logout()}>重新登录</Button>}/>
    </>
  return <>{logoutNotice && <Alert type="warning" showIcon message={logoutNotice} />}{children}</>
}
