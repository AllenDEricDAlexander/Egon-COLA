import {MenuOutlined, UserOutlined} from '@ant-design/icons'
import {Avatar, Button, Grid, Layout, Dropdown, Space, theme, Typography} from 'antd'
import type {MenuProps} from 'antd'
import {useMemo} from 'react'
import {useT} from '../i18n'
import type {EnterpriseHeaderConfig} from './types'

export interface EnterpriseHeaderProps extends EnterpriseHeaderConfig {
  readonly mobileNavigationVisible?: boolean
  readonly onOpenNavigation?: () => void
}

/** Unified top Banner: brand, platform state/actions, user area, and mobile tree trigger. */
export const EnterpriseHeader = ({
  platformName,
  logo,
  actions,
  user,
  mobileNavigationVisible = false,
  onOpenNavigation,
}: EnterpriseHeaderProps) => {
  const {token} = theme.useToken()
  const t = useT()
  const screens = Grid.useBreakpoint()
  const full = screens.lg === true

  const userMenuItems = useMemo<MenuProps['items']>(() => {
    if (!user) return undefined
    const items: NonNullable<MenuProps['items']> = []
    if (user.description) items.push({key: 'user-description', label: user.description, disabled: true})
    if (user.menu && user.menu.length > 0) {
      if (items.length > 0) items.push({type: 'divider'})
      items.push(...user.menu)
    }
    return items.length > 0 ? items : undefined
  }, [user])

  const userAvatar = user?.avatar ?? (
    <Avatar size={28} icon={<UserOutlined/>} style={{background: token.colorPrimary}}/>
  )
  const userArea = user && (
    userMenuItems ? (
      <Dropdown menu={{items: userMenuItems}} placement="bottomRight" trigger={['click']}>
        <Button type="text" aria-label={t('layout.userMenu', '用户菜单')} style={{height: 44, paddingInline: 8}}>
          <Space size={8}>{userAvatar}{full && <Typography.Text>{user.name}</Typography.Text>}</Space>
        </Button>
      </Dropdown>
    ) : (
      <Button type="text" aria-label={t('layout.userMenu', '用户菜单')} style={{height: 44, paddingInline: 8}}>
        <Space size={8}>{userAvatar}{full && <Typography.Text>{user.name}</Typography.Text>}</Space>
      </Button>
    )
  )

  return (
    <Layout.Header
      className="egon-enterprise-header"
      style={{
        position: 'sticky', top: 0, zIndex: 100, display: 'flex', alignItems: 'center', gap: 16,
        height: 56, lineHeight: '56px', paddingInline: full ? 24 : 16,
        background: token.colorBgContainer, borderBottom: `1px solid ${token.colorBorderSecondary}`,
      }}
    >
      <Space size={12} style={{flexShrink: 0}}>
        {logo ?? <img src="/favicon.png" alt={platformName} width={28} height={28} style={{display: 'block'}}/>}
        <Typography.Text strong style={{fontSize: 15, whiteSpace: 'nowrap'}}>{platformName}</Typography.Text>
      </Space>
      <Space size={4} style={{marginLeft: 'auto', flexShrink: 0}}>
        {full && actions}
        {userArea}
        {!full && mobileNavigationVisible && (
          <Button
            type="text"
            aria-label={t('layout.openNavigation', '打开导航')}
            icon={<MenuOutlined/>}
            onClick={onOpenNavigation}
          />
        )}
      </Space>
    </Layout.Header>
  )
}
