import {Breadcrumb, Button, Layout as AntLayout, Menu, Space, Tag, Typography} from 'antd'
import type {ItemType} from 'antd/es/breadcrumb/Breadcrumb'
import {HomeOutlined} from '@ant-design/icons'
import {type PropsWithChildren, useMemo} from 'react'
import {useLocation, useNavigate} from 'react-router-dom'
import {useAuth} from '../auth/AuthContext'
import {usePermission} from '@egon-cola/admin-web-shared'

interface NavItem {
    key: string
    label: string
    path: string
    permission: string
}

const ALL_NAV_ITEMS: NavItem[] = [
    {key: 'overview', label: '身份概览', path: '/overview', permission: ''},
    {key: 'users', label: '全局用户', path: '/users', permission: 'idp:identity-user:read'},
    {key: 'clients', label: 'OAuth 客户端', path: '/clients', permission: 'idp:oauth-client:read'},
    {
        key: 'resource-servers',
        label: 'Resource Server',
        path: '/resource-servers',
        permission: 'idp:resource-server:read'
    },
    {key: 'keys', label: '签名密钥', path: '/keys', permission: 'idp:signing-key:read'},
    {key: 'audits', label: '安全审计', path: '/audits', permission: 'idp:audit:read'},
]

const PATH_LABELS: Record<string, string> = {
    '/overview': '身份概览',
    '/users': '全局用户',
    '/clients': 'OAuth 客户端',
    '/resource-servers': 'Resource Server',
    '/keys': '签名密钥',
    '/audits': '安全审计',
}

export const AdminLayout = ({ children }: PropsWithChildren) => {
  const auth = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const { has } = usePermission(auth.bootstrap?.permissions ?? [])

    const navItems = useMemo(
        () => ALL_NAV_ITEMS.filter((item) => !item.permission || has(item.permission)),
        [has],
    )

  const currentPath = location.pathname
    const selectedKey = navItems.find((item) => currentPath.startsWith(item.path) && item.path !== '/overview')
        ? navItems.find((item) => currentPath.startsWith(item.path) && item.path !== '/overview')!.key
        : (navItems.find((item) => item.path === currentPath)?.key ?? 'overview')

    const breadcrumbItems = useMemo((): ItemType[] => {
        const items: ItemType[] = [{title: <><HomeOutlined/> 首页</>}]
        const label = PATH_LABELS[currentPath]
        if (label && currentPath !== '/overview') {
            items.push({title: label})
        }
        if (currentPath.includes('/resource-grants')) {
            items.push({title: 'Resource Grant'})
        }
        return items
    }, [currentPath])

  return (
      <AntLayout style={{minHeight: '100vh'}}>
          <AntLayout.Sider width={240} theme="light" style={{borderRight: '1px solid #f0f0f0'}}>
              <div style={{padding: '20px 22px 12px'}}>
                  <Typography.Title level={4} style={{margin: 0}}>统一身份平台</Typography.Title>
              </div>
        <Menu
          mode="inline"
          selectedKeys={[selectedKey]}
          items={navItems.map((item) => ({key: item.key, label: item.label}))}
          onClick={({ key }) => {
              const target = navItems.find((i) => i.key === key)
            if (target) navigate(target.path)
          }}
          style={{borderInlineEnd: 'none'}}
        />
          </AntLayout.Sider>
          <AntLayout>
              <AntLayout.Header style={{
                  background: '#fff',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  borderBottom: '1px solid #f0f0f0',
                  padding: '0 24px',
                  height: 48,
              }}>
                  <Breadcrumb items={breadcrumbItems}/>
                  <Space size="middle">
                      <Typography.Text type="secondary"
                                       style={{fontSize: 13}}>{auth.bootstrap?.identitySub}</Typography.Text>
                      <Tag color="blue">{auth.bootstrap?.tenantId}</Tag>
                      <Button size="small" onClick={() => {
                          void auth.logout()
                      }}>退出</Button>
          </Space>
              </AntLayout.Header>
              <AntLayout.Content style={{padding: 24, background: '#f5f5f5'}}>
          {children}
              </AntLayout.Content>
          </AntLayout>
      </AntLayout>
  )
}
