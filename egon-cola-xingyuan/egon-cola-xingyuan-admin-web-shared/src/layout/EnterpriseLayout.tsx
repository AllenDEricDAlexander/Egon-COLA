import {Grid, Layout} from 'antd'
import type {ReactNode} from 'react'
import {useEffect, useMemo, useState} from 'react'
import {useLocation} from 'react-router-dom'
import {EnterpriseFooter} from './EnterpriseFooter'
import {EnterpriseHeader} from './EnterpriseHeader'
import {EnterpriseSidebar, resolveNavigationSelection} from './EnterpriseSidebar'
import type {EnterpriseLayoutConfig} from './types'

export interface EnterpriseLayoutProps {
  readonly config: EnterpriseLayoutConfig
  readonly children: ReactNode
}

/** Banner plus responsive left navigation shell; navigation state stays local to this layout. */
export const EnterpriseLayout = ({config, children}: EnterpriseLayoutProps) => {
  const screens = Grid.useBreakpoint()
  const location = useLocation()
  const [collapsed, setCollapsed] = useState(false)
  const [openKeys, setOpenKeys] = useState<readonly string[]>([])
  const [drawerOpen, setDrawerOpen] = useState(false)
  const navigation = useMemo(() => config.navigation ?? [], [config.navigation])
  const full = screens.lg === true
  const selection = useMemo(
    () => resolveNavigationSelection(navigation, location.pathname),
    [location.pathname, navigation],
  )

  useEffect(() => {
    if (!full) {
      // Breakpoint changes must close the controlled mobile drawer.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setDrawerOpen(false)
    }
  }, [full])

  useEffect(() => {
    if (selection.ancestorKeys.length === 0) return
    // Selected ancestors are synchronized into the controlled menu state.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setOpenKeys((current) => {
      const missing = selection.ancestorKeys.filter((key) => !current.includes(key))
      return missing.length > 0 ? [...current, ...missing] : current
    })
  }, [selection.ancestorKeys])

  const mainColumn = (
    <Layout style={{minWidth: 0, flex: '1 1 auto'}}>
      <Layout.Content
        style={{
          flex: '1 0 auto', minWidth: 0, overflowX: 'hidden', padding: screens.md ? 24 : 12,
          ...config.contentStyle,
        }}
      >
        {children}
      </Layout.Content>
      {config.hideFooter !== true && (
        <EnterpriseFooter platformName={config.platformName} {...config.footer}/>
      )}
    </Layout>
  )

  return (
    <Layout style={{minHeight: '100vh'}}>
      {config.hideHeader !== true && (
        <EnterpriseHeader
          platformName={config.platformName}
          logo={config.logo}
          user={config.user}
          actions={config.actions}
          mobileNavigationVisible={!full && navigation.length > 0}
          onOpenNavigation={() => setDrawerOpen(true)}
        />
      )}
      <Layout hasSider={full && navigation.length > 0}>
        {navigation.length > 0 && (
          <EnterpriseSidebar
            items={navigation}
            collapsed={collapsed}
            onCollapsedChange={setCollapsed}
            openKeys={openKeys}
            onOpenKeysChange={setOpenKeys}
            selectedKey={selection.selectedKey}
            mobile={!full}
            drawerOpen={drawerOpen}
            onDrawerClose={() => setDrawerOpen(false)}
            platformName={config.platformName}
            actions={config.actions}
            onNavigate={config.onNavigate}
          />
        )}
        {mainColumn}
      </Layout>
    </Layout>
  )
}
