import { Button, Layout, Menu, Space, Typography } from 'antd'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const menuItems = [
  { key: 'registry', label: '服务注册' },
  { key: 'configs', label: '配置管理' },
  { key: 'bizs', label: '业务域' },
  { key: 'envs', label: '环境' },
  { key: 'apps', label: '应用' },
  { key: 'namespaces', label: '命名空间' },
  { key: 'publish-tasks', label: '发布任务' },
  { key: 'cache', label: '缓存' },
]

export default function AdminLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const { logout } = useAuth()
  const selected = menuItems.find((item) => location.pathname.startsWith(`/${item.key}`))?.key ?? 'registry'

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Layout.Sider theme="light" width={200}>
        <div style={{ padding: 16 }}>
          <Typography.Text strong>DDC Admin</Typography.Text>
        </div>
        <Menu
          mode="inline"
          selectedKeys={[selected]}
          items={menuItems}
          onClick={({ key }) => navigate(`/${key}`)}
        />
      </Layout.Sider>
      <Layout>
        <Layout.Header style={{ background: '#fff', display: 'flex', justifyContent: 'flex-end', alignItems: 'center' }}>
          <Space>
            <Typography.Text>DDC 已连接</Typography.Text>
            <Button onClick={logout}>退出</Button>
          </Space>
        </Layout.Header>
        <Layout.Content style={{ padding: 24 }}>
          <Outlet />
        </Layout.Content>
      </Layout>
    </Layout>
  )
}
