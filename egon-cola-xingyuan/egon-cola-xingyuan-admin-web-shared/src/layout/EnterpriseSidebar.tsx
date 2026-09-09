import {Layout, Menu, theme, Drawer} from 'antd'
import type {MenuProps} from 'antd'
import {useNavigate} from 'react-router-dom'
import type {EnterpriseNavigationItem} from './types'
import type {ReactNode} from 'react'

export interface EnterpriseSidebarProps {
  readonly items: readonly EnterpriseNavigationItem[]
  readonly collapsed: boolean
  readonly onCollapsedChange: (collapsed: boolean) => void
  readonly openKeys: readonly string[]
  readonly onOpenKeysChange: (keys: readonly string[]) => void
  readonly selectedKey?: string
  readonly mobile: boolean
  readonly drawerOpen: boolean
  readonly onDrawerClose: () => void
  readonly platformName: string
  readonly actions?: ReactNode
  readonly onNavigate?: (item: EnterpriseNavigationItem) => void
}

export const EnterpriseSidebar = ({
  items,
  collapsed,
  onCollapsedChange,
  openKeys,
  onOpenKeysChange,
  selectedKey,
  mobile,
  drawerOpen,
  onDrawerClose,
  platformName,
  actions,
  onNavigate,
}: EnterpriseSidebarProps) => {
  const {token} = theme.useToken()
  const navigate = useNavigate()
  const handleClick: MenuProps['onClick'] = ({key}) => {
    const item = findItem(items, String(key))
    if (!item?.path) return
    if (onNavigate) onNavigate(item)
    else navigate(item.path)
    if (mobile) onDrawerClose()
  }
  const menu = (
    <nav aria-label="主菜单">
      <Menu
        aria-label="主菜单"
        mode="inline"
        items={items.map((item) => toMenuItem(item, collapsed))}
        selectedKeys={selectedKey ? [selectedKey] : []}
        openKeys={[...openKeys]}
        onOpenChange={(keys) => onOpenKeysChange(keys as string[])}
        onClick={handleClick}
        style={{borderInlineEnd: 'none'}}
      />
    </nav>
  )

  if (mobile) {
    return (
      <Drawer
        open={drawerOpen}
        placement="left"
        title={platformName}
        size="min(86vw, 320px)"
        styles={{body: {padding: 0}}}
        onClose={onDrawerClose}
      >
        {actions && (
          <div style={{padding: '12px 16px', borderBottom: `1px solid ${token.colorBorderSecondary}`}}>
            {actions}
          </div>
        )}
        {menu}
      </Drawer>
    )
  }

  return (
    <Layout.Sider
      collapsible
      collapsed={collapsed}
      onCollapse={onCollapsedChange}
      width={240}
      collapsedWidth={72}
      theme="light"
      style={{borderInlineEnd: `1px solid ${token.colorBorderSecondary}`}}
    >
      {menu}
    </Layout.Sider>
  )
}

export interface NavigationSelection {
  readonly selectedKey?: string
  readonly ancestorKeys: readonly string[]
}

export const resolveNavigationSelection = (
  items: readonly EnterpriseNavigationItem[],
  pathname: string,
): NavigationSelection => {
  const candidates: Array<{item: EnterpriseNavigationItem; ancestors: string[]; matchLength: number}> = []
  const visit = (entries: readonly EnterpriseNavigationItem[], ancestors: string[]) => {
    for (const item of entries) {
      const nextAncestors = [...ancestors, item.key]
      const paths = [item.path, ...(item.activePathPrefixes ?? [])].filter(
        (value): value is string => Boolean(value),
      )
      for (const path of paths) {
        if (matchesPath(path, pathname)) {
          candidates.push({item, ancestors, matchLength: path.length})
        }
      }
      if (item.children) visit(item.children, nextAncestors)
    }
  }
  visit(items, [])
  candidates.sort((left, right) => right.matchLength - left.matchLength || left.item.key.localeCompare(right.item.key))
  const winner = candidates[0]
  return winner
    ? {selectedKey: winner.item.key, ancestorKeys: winner.ancestors}
    : {selectedKey: undefined, ancestorKeys: []}
}

const matchesPath = (candidate: unknown, pathname: string): boolean => {
  if (typeof candidate !== 'string' || !candidate.startsWith('/')) return false
  if (candidate === '/') return pathname === '/'
  return pathname === candidate || pathname.startsWith(candidate.endsWith('/') ? candidate : `${candidate}/`)
}

const toMenuItem = (item: EnterpriseNavigationItem, collapsed: boolean): NonNullable<MenuProps['items']>[number] => ({
  key: item.key,
  icon: item.icon,
  label: item.label,
  title: collapsed ? item.label : undefined,
  children: item.children?.map((child) => toMenuItem(child, collapsed)),
})

const findItem = (
  items: readonly EnterpriseNavigationItem[],
  key: string,
): EnterpriseNavigationItem | undefined => {
  for (const item of items) {
    if (item.key === key) return item
    const child = item.children && findItem(item.children, key)
    if (child) return child
  }
  return undefined
}
