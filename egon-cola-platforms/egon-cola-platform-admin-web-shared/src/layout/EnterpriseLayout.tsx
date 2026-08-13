import { Grid, Layout } from 'antd'
import type { ReactNode } from 'react'
import { EnterpriseFooter } from './EnterpriseFooter'
import { EnterpriseHeader } from './EnterpriseHeader'
import type { EnterpriseLayoutConfig } from './types'

export interface EnterpriseLayoutProps {
  readonly config: EnterpriseLayoutConfig
  readonly children: ReactNode
}

/**
 * 统一企业级页面骨架：Header（sticky 顶部导航）+ 可伸缩内容区 + 贴底 Footer。
 * 通过 flex 布局保证最小高度占满视口，内容较少时 Footer 不会悬浮在页面中间。
 */
export const EnterpriseLayout = ({ config, children }: EnterpriseLayoutProps) => {
  const screens = Grid.useBreakpoint()
  const {
    platformName,
    logo,
    navigation,
    user,
    actions,
    onNavigate,
    footer,
    contentStyle,
  } = config

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <EnterpriseHeader
        platformName={platformName}
        logo={logo}
        navigation={navigation}
        user={user}
        actions={actions}
        onNavigate={onNavigate}
      />
      <Layout.Content
        style={{
          flex: '1 0 auto',
          minWidth: 0,
          overflowX: 'hidden',
          padding: screens.md ? 24 : 12,
          ...contentStyle,
        }}
      >
        {children}
      </Layout.Content>
      <EnterpriseFooter platformName={platformName} {...footer} />
    </Layout>
  )
}
