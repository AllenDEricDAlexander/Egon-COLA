import { MenuOutlined, UserOutlined } from '@ant-design/icons'
import { Avatar, Button, Drawer, Dropdown, Grid, Layout, Menu, Space, theme, Typography } from 'antd'
import type { MenuProps } from 'antd'
import { useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useT } from '../i18n'
import type { EnterpriseHeaderConfig, EnterpriseNavigationItem } from './types'

export type EnterpriseHeaderProps = EnterpriseHeaderConfig

const toItem = (item: EnterpriseNavigationItem): NonNullable<MenuProps['items']>[number] => ({
  key: item.key,
  icon: item.icon,
  label: item.label,
  children: item.children?.map(toItem),
})

const flattenLeaves = (items: readonly EnterpriseNavigationItem[]): EnterpriseNavigationItem[] =>
  items.flatMap((item) => item.children?.length ? flattenLeaves(item.children) : [item])

/** 统一企业级顶部 Header：品牌区 + 平台名称 + 一级导航 + 全局操作区 + 用户区。 */
export const EnterpriseHeader = ({
  platformName,
  logo,
  navigation = [],
  actions,
  user,
  onNavigate,
}: EnterpriseHeaderProps) => {
  const { token } = theme.useToken()
  const t = useT()
  const navigate = useNavigate()
  const location = useLocation()
  const screens = Grid.useBreakpoint()
  const [drawerOpen, setDrawerOpen] = useState(false)

  // 窄屏下收起顶部导航，改用抽屉展示（< lg 断点）。
  const full = screens.lg === true

  // 路由高亮：与当前路径做最长前缀匹配（带路径边界），避免顺序敏感与误匹配。
  const selectedKey = useMemo(() => {
    let best: EnterpriseNavigationItem | undefined
    for (const item of flattenLeaves(navigation)) {
      if (!item.path) continue
      const matches = item.path === '/'
        ? location.pathname === item.path
        : location.pathname === item.path
          || location.pathname.startsWith(item.path.endsWith('/') ? item.path : `${item.path}/`)
      if (matches && (!best || item.path.length > (best.path?.length ?? -1))) {
        best = item
      }
    }
    return best?.key
  }, [location.pathname, navigation])

  const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
    const item = flattenLeaves(navigation).find((entry) => entry.key === key)
    if (!item || !item.path) return
    setDrawerOpen(false)
    if (onNavigate) {
      onNavigate(item)
    } else {
      navigate(item.path)
    }
  }

  // 抽屉导航按 group 分组展示；无分组的项排在最前，其余按出现顺序分组。
  const drawerItems = useMemo<MenuProps['items']>(() => {
    const items: NonNullable<MenuProps['items']> = navigation
      .filter((item) => !item.group)
      .map(toItem)
    const groups: string[] = []
    for (const item of navigation) {
      if (item.group && !groups.includes(item.group)) groups.push(item.group)
    }
    for (const group of groups) {
      items.push({
        type: 'group',
        label: group,
        children: navigation.filter((item) => item.group === group).map(toItem),
      })
    }
    return items
  }, [navigation])

  const userMenuItems = useMemo<MenuProps['items']>(() => {
    if (!user) return undefined
    const items: NonNullable<MenuProps['items']> = []
    if (user.description) {
      items.push({ key: 'user-description', label: user.description, disabled: true })
    }
    if (user.menu && user.menu.length > 0) {
      if (items.length > 0) items.push({ type: 'divider' })
      items.push(...user.menu)
    }
    return items.length > 0 ? items : undefined
  }, [user])

  const userAvatar = user?.avatar ?? (
    <Avatar size={28} icon={<UserOutlined />} style={{ background: token.colorPrimary }} />
  )

  const userArea = user && (
    userMenuItems ? (
      <Dropdown menu={{ items: userMenuItems }} placement="bottomRight" trigger={['click']}>
        <Button
          type="text"
          aria-label={t('layout.userMenu', '用户菜单')}
          style={{ height: 44, paddingInline: 8 }}
        >
          <Space size={8}>
            {userAvatar}
            {full && <Typography.Text>{user.name}</Typography.Text>}
          </Space>
        </Button>
      </Dropdown>
    ) : (
      <Button type="text" aria-label={t('layout.userMenu', '用户菜单')} style={{ height: 44, paddingInline: 8 }}>
        <Space size={8}>
          {userAvatar}
          {full && <Typography.Text>{user.name}</Typography.Text>}
        </Space>
      </Button>
    )
  )

  return (
    <>
      <Layout.Header
        className="egon-enterprise-header"
        style={{
          position: 'sticky',
          top: 0,
          zIndex: 100,
          display: 'flex',
          alignItems: 'center',
          gap: 16,
          height: 56,
          lineHeight: '56px',
          paddingInline: full ? 24 : 16,
          background: token.colorBgContainer,
          borderBottom: `1px solid ${token.colorBorderSecondary}`,
        }}
      >
        <Space size={12} style={{ flexShrink: 0 }}>
          {logo ?? (
            <img
              src="/favicon.png"
              alt={platformName}
              width={28}
              height={28}
              style={{ display: 'block' }}
            />
          )}
          <Typography.Text strong style={{ fontSize: 15, whiteSpace: 'nowrap' }}>
            {platformName}
          </Typography.Text>
        </Space>
        {full && navigation.length > 0 && (
          <Menu
            aria-label={t('layout.navigation', '主导航')}
            mode="horizontal"
            selectedKeys={selectedKey ? [selectedKey] : []}
            items={navigation.map(toItem)}
            onClick={handleMenuClick}
            style={{
              flex: 1,
              minWidth: 0,
              background: 'transparent',
              borderBottom: 'none',
            }}
          />
        )}
        <Space size={4} style={{ marginLeft: 'auto', flexShrink: 0 }}>
          {full && actions}
          {userArea}
          {!full && navigation.length > 0 && (
            <Button
              type="text"
              aria-label={t('layout.openNavigation', '打开导航')}
              icon={<MenuOutlined />}
              onClick={() => setDrawerOpen(true)}
            />
          )}
        </Space>
      </Layout.Header>
      <Drawer
        open={!full && drawerOpen}
        placement="left"
        title={platformName}
        size="min(86vw, 320px)"
        styles={{ body: { padding: 0 } }}
        onClose={() => setDrawerOpen(false)}
      >
        {actions && (
          <div
            style={{
              padding: '12px 16px',
              borderBottom: `1px solid ${token.colorBorderSecondary}`,
            }}
          >
            <Space direction="vertical" size={8} style={{ width: '100%' }}>
              {actions}
            </Space>
          </div>
        )}
        <Menu
          aria-label={t('layout.navigation', '主导航')}
          mode="inline"
          selectedKeys={selectedKey ? [selectedKey] : []}
          items={drawerItems}
          onClick={handleMenuClick}
          style={{ borderInlineEnd: 'none' }}
        />
      </Drawer>
    </>
  )
}
