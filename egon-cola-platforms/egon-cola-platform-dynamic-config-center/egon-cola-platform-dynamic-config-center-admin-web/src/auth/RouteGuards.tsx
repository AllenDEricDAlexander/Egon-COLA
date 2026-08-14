import type {ReactNode} from 'react'
import {Spin} from 'antd'
import {useAuth} from './AuthContext'
import LoginPage from './LoginPage'

export function RequireAuth({ children }: { children: ReactNode }) {
    const {authorized, loading} = useAuth()
  if (loading) return <Spin fullscreen tip="校验统一登录态" />
    if (!authorized) return <LoginPage/>
  return <>{children}</>
}
