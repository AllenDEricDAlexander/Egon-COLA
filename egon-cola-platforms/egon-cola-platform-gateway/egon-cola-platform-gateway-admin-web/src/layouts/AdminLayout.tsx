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

// 平台自己的导航数据，由 capability 过滤后交给统一 Header。
const navigation: Array<{
  key: string
  icon: React.ReactNode
  label: string
  capability: Capability
}> = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: '总览', capability: 'gateway:read' },
  {
    key: '/gateway-groups',
    icon: <DeploymentUnitOutlined />,
    label: 'Gateway Group',
    capability: 'gateway:read',
  },
  {
    key: '/applications',
    icon: <KeyOutlined />,
    label: 'Application / Credential',
    capability: 'gateway:read',
  },
  {
    key: '/interface-catalog',
    icon: <AppstoreOutlined />,
    label: '接口目录',
    capability: 'gateway:read',
  },
  { key: '/providers', icon: <ApiOutlined />, label: 'Provider', capability: 'gateway:read' },
  {
    key: '/mcp/servers',
    icon: <RobotOutlined />,
    label: 'MCP Control Plane',
    capability: 'gateway:mcp:read',
  },
  {
    key: '/mcp/remote-providers',
    icon: <ShareAltOutlined />,
    label: 'Remote MCP',
    capability: 'gateway:mcp:read',
  },
  {
    key: '/observability/traces',
    icon: <EyeOutlined />,
    label: '调用观测',
    capability: 'gateway:read',
  },
  { key: '/audit', icon: <AuditOutlined />, label: '审计日志', capability: 'gateway:read' },
]

export const AdminLayout = () => {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const auth = useAuth()
  const canRead = useCapability('gateway:read')
  const canReadMcp = useCapability('gateway:mcp:read')

  const items: EnterpriseNavigationItem[] = navigation
    .filter((item) => item.capability === 'gateway:mcp:read' ? canReadMcp : canRead)
    .map((item) => ({
      key: item.key,
      path: item.key,
      icon: item.icon,
      label: item.label,
    }))

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
