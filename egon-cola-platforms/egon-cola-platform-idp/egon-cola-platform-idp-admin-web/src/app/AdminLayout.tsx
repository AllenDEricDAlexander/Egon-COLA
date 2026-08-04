import { Button, Layout, Menu, Space, Tag, Typography } from 'antd'
import { type PropsWithChildren } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { usePermission } from '@egon-cola/admin-web-shared'

export const AdminLayout = ({ children }: PropsWithChildren) => {
  const auth = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const { has } = usePermission(auth.bootstrap?.permissions ?? [])

  const items = [
    { key: 'overview', label: '身份概览', path: '/overview' },
    has('idp:identity-user:read') ? { key: 'users', label: '全局用户', path: '/users' } : null,
    has('idp:oauth-client:read') ? { key: 'clients', label: 'OAuth 客户端', path: '/clients' } : null,
    has('idp:signing-key:read') ? { key: 'keys', label: '签名密钥', path: '/keys' } : null,
    has('idp:audit:read') ? { key: 'audits', label: '安全审计', path: '/audits' } : null,
  ].filter(Boolean) as { key: string; label: string; path: string }[]

  const currentPath = location.pathname
  const selectedKey = items.find((item) => currentPath === item.path)?.key ?? 'overview'

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Layout.Sider width={240} theme="light">
        <Typography.Title level={4} style={{ padding: '20px 22px 8px' }}>统一身份平台</Typography.Title>
        <Menu
          mode="inline"
          selectedKeys={[selectedKey]}
          items={items.map((item) => ({ key: item.key, label: item.label }))}
          onClick={({ key }) => {
            const target = items.find((i) => i.key === key)
            if (target) navigate(target.path)
          }}
        />
      </Layout.Sider>
      <Layout>
        <Layout.Header style={{ background: '#fff', display: 'flex', justifyContent: 'flex-end', alignItems: 'center', borderBottom: '1px solid var(--egon-color-border)' }}>
          <Space>
            <Typography.Text>{auth.bootstrap?.identitySub}</Typography.Text>
            <Tag>{auth.bootstrap?.tenantId}</Tag>
            <Button onClick={() => { void auth.logout() }}>退出当前系统</Button>
          </Space>
        </Layout.Header>
        <Layout.Content style={{ padding: 24 }}>
          {children}
        </Layout.Content>
      </Layout>
    </Layout>
  )
}
