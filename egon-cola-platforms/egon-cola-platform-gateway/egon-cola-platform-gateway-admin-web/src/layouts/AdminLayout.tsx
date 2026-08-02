import {
  ApiOutlined,
  AppstoreOutlined,
  AuditOutlined,
  DashboardOutlined,
  DeploymentUnitOutlined,
  EyeOutlined,
  KeyOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  RobotOutlined,
} from '@ant-design/icons'
import { Badge, Button, Layout, Menu, Select, Space, Typography } from 'antd'
import { useState } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { useScope } from '../hooks/useScope'
import { optionsFor, type ScopeField } from '../hooks/scopeDefaults'
import { useAuth } from '../auth/AuthContext'
import { useCapability, type Capability } from '../app/capabilities'

const { Header, Sider, Content } = Layout

const selectors: Array<[ScopeField, string]> = [
  ['bizCode', '业务域'],
  ['namespace', '命名空间'],
  ['env', '环境'],
  ['appCode', '应用'],
]

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
    key: '/observability/traces',
    icon: <EyeOutlined />,
    label: '调用观测',
    capability: 'gateway:read',
  },
  { key: '/audit', icon: <AuditOutlined />, label: '审计日志', capability: 'gateway:read' },
]

export const AdminLayout = () => {
  const [collapsed, setCollapsed] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { scope, bindings, changeScope: selectScope } = useScope()
  const auth = useAuth()
  const canRead = useCapability('gateway:read')
  const canReadMcp = useCapability('gateway:mcp:read')
  const items = navigation
    .filter((item) => item.capability === 'gateway:mcp:read' ? canReadMcp : canRead)
    .map((item) => ({
        key: item.key,
        icon: item.icon,
        label: <Link to={item.key}>{item.label}</Link>,
      }))

  const changeScope = (
    field: ScopeField,
    value: string,
  ) => {
    if (!window.confirm('切换作用域会清空当前缓存和未保存表单，是否继续？')) {
      return
    }
    selectScope(field, value)
    queryClient.removeQueries({
      predicate: (query) => query.queryKey[0] !== 'gateway-scopes',
    })
    navigate('/dashboard')
  }

  return (
    <Layout className="app-shell">
      <Sider collapsible collapsed={collapsed} trigger={null} width={236}>
        <div className="brand">{collapsed ? 'GW' : 'Egon Gateway'}</div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[items.find((item) => location.pathname.startsWith(item.key))?.key ?? '']}
          items={items}
        />
      </Sider>
      <Layout>
        <Header className="app-header">
          <Space size="middle">
            <Button
              type="text"
              aria-label={collapsed ? '展开导航' : '收起导航'}
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setCollapsed((value) => !value)}
            />
            {selectors.map(([field, label]) => (
              <Select
                key={field}
                aria-label={label}
                value={scope[field]}
                options={optionsFor(bindings, scope, field)}
                onChange={(value) => changeScope(field, value)}
              />
            ))}
          </Space>
          <Space>
            <Badge status="processing" text="Admin API" />
            <Typography.Text type="secondary">
              {auth.session?.displayName}
            </Typography.Text>
            <Button
              icon={<LogoutOutlined />}
              onClick={() => {
                void auth.logout()
                queryClient.clear()
                navigate('/login', { replace: true })
              }}
            >
              退出
            </Button>
          </Space>
        </Header>
        <Content className="app-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
