import {
  ApartmentOutlined,
  AppstoreOutlined,
  CloudOutlined,
  ClusterOutlined,
  DatabaseOutlined,
  DeploymentUnitOutlined,
  FileTextOutlined,
  MenuFoldOutlined,
  MenuOutlined,
  MenuUnfoldOutlined,
  PartitionOutlined,
} from '@ant-design/icons'
import {
  Badge,
  Button,
  Drawer,
  Grid,
  Layout,
  Menu,
  Space,
  theme,
  Typography,
} from 'antd'
import type { MenuProps } from 'antd'
import { useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const navigation = [
  { key: 'registry', label: '服务注册', icon: <ClusterOutlined /> },
  {
    key: 'publish-tasks',
    label: '发布任务',
    icon: <DeploymentUnitOutlined />,
  },
  { key: 'cache', label: '缓存', icon: <DatabaseOutlined /> },
  { key: 'configs', label: '配置资源', icon: <FileTextOutlined /> },
  { key: 'bizs', label: '业务域', icon: <PartitionOutlined /> },
  { key: 'envs', label: '环境', icon: <CloudOutlined /> },
  { key: 'apps', label: '应用', icon: <AppstoreOutlined /> },
  {
    key: 'namespaces',
    label: '命名空间',
    icon: <ApartmentOutlined />,
  },
]

const menuItems: MenuProps['items'] = [
  {
    type: 'group',
    label: '运行状态',
    children: navigation.slice(0, 3),
  },
  {
    type: 'group',
    label: '配置管理',
    children: navigation.slice(3, 4),
  },
  {
    type: 'group',
    label: '元数据管理',
    children: navigation.slice(4),
  },
]

export default function AdminLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const { identity, logout } = useAuth()
  const screens = Grid.useBreakpoint()
  const { token } = theme.useToken()
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const desktop = screens.md === true
  const selected = navigation.find((item) =>
    location.pathname.startsWith(`/${item.key}`))?.key ?? 'registry'
  const currentLabel = navigation.find((item) => item.key === selected)?.label
    ?? 'DDC Admin'

  const navigateTo = (key: string) => {
    navigate(`/${key}`)
    setMobileOpen(false)
  }

  const menu = (ariaLabel: string) => (
    <Menu
      aria-label={ariaLabel}
      mode="inline"
      selectedKeys={[selected]}
      items={menuItems}
      onClick={({ key }) => navigateTo(key)}
    />
  )

  return (
    <Layout className="ddc-admin-layout" style={{ minHeight: '100vh' }}>
      {desktop && (
        <Layout.Sider
          aria-label="桌面主导航"
          theme="light"
          width={224}
          collapsedWidth={64}
          collapsed={collapsed}
        >
          <div style={{ padding: collapsed ? '18px 12px' : '18px 20px' }}>
            <Typography.Text strong>{collapsed ? 'DDC' : 'DDC Admin'}</Typography.Text>
          </div>
          {menu('桌面导航菜单')}
        </Layout.Sider>
      )}
      <Layout className="ddc-admin-main">
        <Layout.Header
          className="ddc-admin-header"
          style={{
            background: token.colorBgContainer,
            borderBottom: `1px solid ${token.colorBorderSecondary}`,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            paddingInline: desktop ? 24 : 12,
          }}
        >
          <Space>
            {desktop ? (
              <Button
                type="text"
                aria-label="折叠导航"
                icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
                onClick={() => setCollapsed((value) => !value)}
              />
            ) : (
              <Button
                type="text"
                aria-label="打开导航"
                icon={<MenuOutlined />}
                onClick={() => setMobileOpen(true)}
              />
            )}
            <Typography.Text strong>{currentLabel}</Typography.Text>
          </Space>
          <Space>
            <Badge status="success" text="DDC 已连接" />
            {identity && <Typography.Text>{identity}</Typography.Text>}
            <Button onClick={() => { void logout() }}>退出</Button>
          </Space>
        </Layout.Header>
        <Layout.Content className="ddc-admin-content">
          <Outlet />
        </Layout.Content>
      </Layout>
      <Drawer
        open={!desktop && mobileOpen}
        placement="left"
        title="DDC Admin"
        size="min(86vw, 320px)"
        onClose={() => setMobileOpen(false)}
      >
        {menu('移动主导航')}
      </Drawer>
    </Layout>
  )
}
