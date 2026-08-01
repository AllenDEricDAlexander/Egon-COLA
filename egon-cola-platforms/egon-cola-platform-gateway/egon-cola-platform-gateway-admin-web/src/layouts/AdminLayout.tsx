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
} from '@ant-design/icons'
import { Badge, Button, Layout, Menu, Select, Space, Typography } from 'antd'
import { useState } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { useScope } from '../hooks/useScope'
import { scopeOptions } from '../hooks/scopeDefaults'
import { useAuth } from '../auth/AuthContext'
import { useCapability, type Capability } from '../app/capabilities'

const { Header, Sider, Content } = Layout

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
  const { scope, setScope } = useScope()
  const auth = useAuth()
  const canRead = useCapability('gateway:read')
  const items = canRead
    ? navigation.map((item) => ({
        key: item.key,
        icon: item.icon,
        label: <Link to={item.key}>{item.label}</Link>,
      }))
    : []

  const changeScope = (
    field: 'bizCode' | 'appCode' | 'env' | 'namespace',
    value: string,
  ) => {
    if (!window.confirm('切换作用域会清空当前缓存和未保存表单，是否继续？')) {
      return
    }
    setScope({ ...scope, [field]: value })
    queryClient.clear()
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
            <Select
              aria-label="业务域"
              value={scope.bizCode}
              options={scopeOptions(scope.bizCode, ['default'], 'Biz')}
              onChange={(value) => changeScope('bizCode', value)}
            />
            <Select
              aria-label="应用"
              value={scope.appCode}
              options={scopeOptions(scope.appCode, ['default-app'], 'App')}
              onChange={(value) => changeScope('appCode', value)}
            />
            <Select
              aria-label="环境"
              value={scope.env}
              options={scopeOptions(
                scope.env,
                ['dev', 'test', 'staging', 'prod'],
                'Env',
              )}
              onChange={(value) => changeScope('env', value)}
            />
            <Select
              aria-label="命名空间"
              value={scope.namespace}
              options={scopeOptions(
                scope.namespace,
                ['default', 'public', 'internal'],
              )}
              onChange={(value) => changeScope('namespace', value)}
            />
          </Space>
          <Space>
            <Badge status="processing" text="Admin API" />
            <Typography.Text type="secondary">
              {auth.session?.displayName}
            </Typography.Text>
            <Button
              icon={<LogoutOutlined />}
              onClick={() => {
                auth.logout()
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
