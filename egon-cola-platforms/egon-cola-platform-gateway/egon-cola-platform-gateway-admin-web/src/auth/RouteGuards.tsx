import {Result, Spin} from 'antd'
import {Navigate, useLocation} from 'react-router-dom'
import type {PropsWithChildren} from 'react'
import {useAuth} from './AuthContext'
import {type Capability, useCapability} from '../app/capabilities'

export const RequireAuth = ({ children }: PropsWithChildren) => {
  const auth = useAuth()
  const location = useLocation()
  if (auth.loading) return <Spin fullscreen tip="校验登录态" />
    if (!auth.authorization) {
    return <Navigate replace to="/login" state={{ from: location.pathname }} />
  }
  return children
}

export const RequireCapability = ({
  capability,
  children,
}: PropsWithChildren<{ capability: Capability }>) => {
  const allowed = useCapability(capability)
  if (!allowed) {
    return (
      <Result
        status="403"
        title="403"
        subTitle={`当前账号缺少 ${capability} 能力`}
      />
    )
  }
  return children
}
