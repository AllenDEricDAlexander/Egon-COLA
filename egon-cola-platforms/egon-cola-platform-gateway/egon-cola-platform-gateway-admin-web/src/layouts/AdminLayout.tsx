import {
    ApiOutlined,
    AppstoreOutlined,
    AuditOutlined,
    DashboardOutlined,
    DeploymentUnitOutlined,
    EyeOutlined,
    KeyOutlined,
    LogoutOutlined,
    RobotOutlined,
    ShareAltOutlined,
} from '@ant-design/icons'
import {Badge, Space} from 'antd'
import {useQueryClient} from '@tanstack/react-query'
import {
    EnterpriseLayout,
    type EnterpriseLayoutConfig,
    type EnterpriseNavigationItem,
} from '@egon-cola/admin-web-shared'
import {Outlet, useNavigate} from 'react-router-dom'
import {version} from '../../package.json'
import {useAuth} from '../auth/AuthContext'
import {type Capability, useCapability} from '../app/capabilities'

// 平台自己的导航树，由 capability 递归剪枝后交给 shared 左侧树。
interface GatewayNavItem {
  key: string
  icon: React.ReactNode
  label: string
  capability: Capability
  path?: string
  activePathPrefixes?: readonly string[]
  children?: readonly GatewayNavItem[]
}

interface WujieRuntimeWindow extends Window {
  $wujie?: { props?: { embedded?: boolean } }
}

const navigation: readonly GatewayNavItem[] = [
  { key: '/dashboard', path: '/dashboard', icon: <DashboardOutlined />, label: '总览', capability: 'gateway:read' },
  {
    key: 'gateway-governance',
    icon: <DeploymentUnitOutlined />,
    label: '网关治理',
    capability: 'gateway:read',
    children: [
      {key: '/gateway-groups', path: '/gateway-groups', icon: <DeploymentUnitOutlined />, label: 'Gateway Group', capability: 'gateway:read'},
      {key: '/applications', path: '/applications', icon: <KeyOutlined />, label: 'Application / Credential', capability: 'gateway:read'},
      {key: '/openapi-sync', path: '/openapi-sync', icon: <ApiOutlined />, label: 'OpenAPI 同步', capability: 'gateway:read'},
      {key: '/interface-catalog', path: '/interface-catalog', activePathPrefixes: ['/operations'], icon: <AppstoreOutlined />, label: '接口目录', capability: 'gateway:read'},
      {key: '/providers', path: '/providers', icon: <ApiOutlined />, label: 'Provider', capability: 'gateway:read'},
    ],
  },
  {
    key: 'mcp',
    icon: <RobotOutlined />,
    label: 'MCP',
    capability: 'gateway:mcp:read',
    children: [
      {key: '/mcp/servers', path: '/mcp/servers', icon: <RobotOutlined />, label: 'MCP Control Plane', capability: 'gateway:mcp:read'},
      {key: '/mcp/remote-providers', path: '/mcp/remote-providers', icon: <ShareAltOutlined />, label: 'Remote MCP', capability: 'gateway:mcp:read'},
    ],
  },
  {
    key: 'observability',
    icon: <EyeOutlined />,
    label: '观测与审计',
    capability: 'gateway:read',
    children: [
      {key: '/observability/traces', path: '/observability/traces', icon: <EyeOutlined />, label: '调用观测', capability: 'gateway:read'},
      {key: '/audit', path: '/audit', icon: <AuditOutlined />, label: '审计日志', capability: 'gateway:read'},
    ],
  },
]

export const AdminLayout = () => {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const auth = useAuth()
  const canRead = useCapability('gateway:read')
  const canReadMcp = useCapability('gateway:mcp:read')

  const embedded = (window as WujieRuntimeWindow).$wujie?.props?.embedded === true
  if (embedded) return <Outlet />

  const items: EnterpriseNavigationItem[] = filterNavigation(navigation, canRead, canReadMcp)

  const config: EnterpriseLayoutConfig = {
    platformName: 'Gateway Admin',
    navigation: items,
    actions: (
      <Space size="middle" wrap>
        <Badge status="processing" text="Admin API" />
      </Space>
    ),
      user: auth.authorization
      ? {
              name: auth.authorization.user.identitySub,
          menu: [
            {
              key: 'logout',
              label: '退出登录',
              icon: <LogoutOutlined />,
              onClick: () => {
                void auth.logout()
                queryClient.clear()
                navigate('/login', { replace: true })
              },
            },
          ],
        }
      : undefined,
    footer: { version },
  }

  return (
    <EnterpriseLayout config={config}>
      <Outlet />
    </EnterpriseLayout>
  )
}

const filterNavigation = (
  entries: readonly GatewayNavItem[],
  canRead: boolean,
  canReadMcp: boolean,
): EnterpriseNavigationItem[] => entries.flatMap((item): EnterpriseNavigationItem[] => {
  const permitted = item.capability === 'gateway:mcp:read' ? canReadMcp : canRead
  if (!permitted) return []
  const children = item.children ? filterNavigation(item.children, canRead, canReadMcp) : []
  if (item.children && children.length === 0) return []
  return [{
    key: item.key,
    label: item.label,
    path: item.path,
    icon: item.icon,
    activePathPrefixes: item.activePathPrefixes,
    children: children.length > 0 ? children : undefined,
  }]
})
