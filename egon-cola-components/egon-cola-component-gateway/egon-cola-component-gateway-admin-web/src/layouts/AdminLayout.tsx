import {
  ApiOutlined,
  AppstoreOutlined,
  AuditOutlined,
  DashboardOutlined,
  DeploymentUnitOutlined,
  EyeOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from '@ant-design/icons'
import { Badge, Button, Layout, Menu, Select, Space, Typography } from 'antd'
import { useState } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { useScope } from '../hooks/useScope'

const { Header, Sider, Content } = Layout

const items = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: <Link to="/dashboard">总览</Link> },
  {
    key: '/gateway-groups',
    icon: <DeploymentUnitOutlined />,
    label: <Link to="/gateway-groups">Gateway Group</Link>,
  },
  {
    key: '/interface-catalog',
    icon: <AppstoreOutlined />,
    label: <Link to="/interface-catalog">接口目录</Link>,
  },
  { key: '/providers', icon: <ApiOutlined />, label: <Link to="/providers">Provider</Link> },
  {
    key: '/observability/traces',
    icon: <EyeOutlined />,
    label: <Link to="/observability/traces">调用观测</Link>,
  },
  { key: '/audit', icon: <AuditOutlined />, label: <Link to="/audit">审计日志</Link> },
]

export const AdminLayout = () => {
  const [collapsed, setCollapsed] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { scope, setScope } = useScope()

  const changeScope = (field: 'env' | 'namespace', value: string) => {
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
              aria-label="环境"
              value={scope.env}
              options={['dev', 'test', 'staging', 'prod'].map((value) => ({
                value,
                label: `Env: ${value}`,
              }))}
              onChange={(value) => changeScope('env', value)}
            />
            <Select
              aria-label="命名空间"
              value={scope.namespace}
              options={['default', 'public', 'internal'].map((value) => ({
                value,
                label: `Namespace: ${value}`,
              }))}
              onChange={(value) => changeScope('namespace', value)}
            />
          </Space>
          <Space>
            <Badge status="processing" text="Admin API" />
            <Typography.Text type="secondary">gateway-admin-web</Typography.Text>
          </Space>
        </Header>
        <Content className="app-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
