import {LoginOutlined, LogoutOutlined} from '@ant-design/icons'
import {EnterpriseLayout} from '@egon-cola/xingyuan-admin-web-shared'
import {useMutation, useQuery} from '@tanstack/react-query'
import {Alert} from 'antd'
import type {PropsWithChildren} from 'react'
import {useState} from 'react'
import {portalAuth, type PortalIdentity} from '../auth/portalAuth'
import {navigationFor} from '../pages/PortalHomePage'
import './portal.css'

const REMOTE_LOGOUT_NOTICE_KEY = 'egon.portal.logout.remote-state'

/** Host-owned brand and sign-in controls; each child still owns its authorization. */
export const PortalLayout = ({children}: PropsWithChildren) => {
  const [remoteLogoutNotice] = useState(() => {
    const state = window.sessionStorage.getItem(REMOTE_LOGOUT_NOTICE_KEY)
    window.sessionStorage.removeItem(REMOTE_LOGOUT_NOTICE_KEY)
    if (state === 'PENDING') return '本地已退出，远端令牌撤销待恢复。'
    if (state === 'UNKNOWN') return '本地已退出，远端令牌撤销状态未知。'
    return undefined
  })
  const identity = useQuery({
    queryKey: ['portal-identity'],
    queryFn: () => portalAuth.userInfo<PortalIdentity>(),
    retry: false,
    refetchOnWindowFocus: true,
  })
  const logout = useMutation({
    mutationFn: () => portalAuth.logout(),
    onSuccess: (result) => {
      if (result.remoteState !== 'CONFIRMED') {
        window.sessionStorage.setItem(REMOTE_LOGOUT_NOTICE_KEY, result.remoteState)
      }
      // Reloading clears the child application's in-memory authorization state too.
      window.location.reload()
    },
  })
  const user = identity.data?.authenticated ? identity.data : undefined

  return (
    <div className="portal-layout">
      <EnterpriseLayout config={{
        platformName: 'Egon COLA Xingyuan',
        logo: <img src="/favicon.png" alt="Egon COLA Xingyuan" width={28} height={28}
          style={{display: 'block', objectFit: 'contain'}} />,
        navigation: navigationFor(),
        hideFooter: true,
        contentStyle: {padding: 0, minHeight: 0, flex: '1 1 0', display: 'flex', overflow: 'hidden'},
        user: user ? {
          name: user.subject,
          description: `租户 ${user.tenantId}`,
          menu: [{key: 'logout', label: logout.isPending ? '正在退出…' : '退出登录',
            icon: <LogoutOutlined />, disabled: logout.isPending, onClick: () => logout.mutate()}],
        } : {
          name: identity.isPending ? '检查登录…' : '登录',
          avatar: <LoginOutlined />,
          menu: [{key: 'login', label: '登录', icon: <LoginOutlined />,
            onClick: () => portalAuth.login()}],
        },
      }}>
        <div className="portal-content">
          {remoteLogoutNotice && <Alert type="warning" showIcon title={remoteLogoutNotice} />}
          {logout.error && <Alert type="error" showIcon title="退出登录失败，请重试" />}
          {children}
        </div>
      </EnterpriseLayout>
    </div>
  )
}
