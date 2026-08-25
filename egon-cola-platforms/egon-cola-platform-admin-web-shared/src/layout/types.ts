import type { MenuProps } from 'antd'
import type { CSSProperties, ReactNode } from 'react'

/**
 * 一级导航项。平台将自己的导航菜单数据传入，shared 只负责渲染与路由高亮，
 * 不在 shared 中定义任何平台菜单。
 *
 * `path` 用于跳转；`activePathPrefixes` 只用于高亮边界匹配，不改变浏览器 URL。
 */
export interface EnterpriseNavigationItem {
  readonly key: string
  readonly label: string
  readonly path?: string
  readonly icon?: ReactNode
  /** Nested MENU/ROUTE children. Parent items without a path only expand. */
  readonly children?: readonly EnterpriseNavigationItem[]
  /** Additional absolute prefixes used to select this item for detail routes. */
  readonly activePathPrefixes?: readonly string[]
}

/**
 * 当前登录用户信息。shared 不定义认证来源，
 * 由平台从各自的 AuthContext / Session 中取值后传入。
 */
export interface EnterpriseUser {
  /** 显示名（用户名 / displayName / identity 等，由平台决定）。 */
  readonly name: string
  /** 次要信息，展示在用户下拉面板中（如租户、subject 等）。 */
  readonly description?: string
  /** 自定义头像节点；缺省时使用统一风格的首字母头像。 */
  readonly avatar?: ReactNode
  /** 用户下拉操作项（如退出登录）。缺省时不渲染下拉菜单。 */
  readonly menu?: MenuProps['items']
}

/**
 * 顶部 Banner 配置。业务导航由 EnterpriseLayout 的左侧树负责。
 */
export interface EnterpriseHeaderConfig {
  readonly platformName: string
  /** 品牌区 Logo 节点；缺省时使用统一 favicon（`/favicon.png`）作为品牌标识。 */
  readonly logo?: ReactNode
  /** 全局操作区，渲染在用户区左侧（连接状态、作用域选择器等平台已有能力）。 */
  readonly actions?: ReactNode
  readonly user?: EnterpriseUser
}

/** Footer 配置。只展示仓库中真实存在的基础信息，不虚构联系信息与备案等。 */
export interface EnterpriseFooterConfig {
  /** 版权主体名，缺省为 `Egon COLA`。 */
  readonly copyrightName?: string
  /** 版本号（如平台 package.json 中的 version）。 */
  readonly version?: string
  /** 附加内容节点（保持克制，仅用于平台真实存在的附加信息）。 */
  readonly extra?: ReactNode
}

/** EnterpriseLayout 总配置。平台只通过该配置注入差异，不复制任何布局代码。 */
export interface EnterpriseLayoutConfig extends EnterpriseHeaderConfig {
  readonly navigation?: readonly EnterpriseNavigationItem[]
  /** Navigation click behavior; defaults to react-router navigate(item.path). */
  readonly onNavigate?: (item: EnterpriseNavigationItem) => void
  readonly footer?: EnterpriseFooterConfig
  /** 内容区自定义样式，会覆盖默认 padding。 */
  readonly contentStyle?: CSSProperties
}
