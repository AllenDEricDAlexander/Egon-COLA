import { useRbac3Session } from '@egon-cola/rbac3-react-sdk'
import { Button, Layout, Menu, Result, Space, Typography } from 'antd'
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import type { PropsWithChildren } from 'react'
import { applicationRouteDescriptors, isRouteAllowed, resolveApplicationLanding, visibleNavigation } from './navigation'
import type { FeatureRouteDescriptor } from '../features/shared/RouteDescriptor'

export const ApplicationRouter = () => {
  const { bootstrap } = useRbac3Session()
  if (!bootstrap) return null
  const landing = resolveApplicationLanding(bootstrap)
  const fallback = landing === null
    ? <Result status="403" title="没有可访问页面" subTitle="当前激活角色没有可用的本地路由。" />
    : <Navigate to={landing} replace />
  return (
    <AdminLayout>
      <Routes>
        {applicationRouteDescriptors.map((route) => (
          <Route key={route.key} path={route.path} element={<RouteAccessGuard route={route}><route.component /></RouteAccessGuard>} />
        ))}
        <Route path="*" element={fallback} />
      </Routes>
    </AdminLayout>
  )
}

const RouteAccessGuard = ({ route, children }: PropsWithChildren<{ readonly route: FeatureRouteDescriptor }>) => {
  const { bootstrap } = useRbac3Session()
  if (!bootstrap || !isRouteAllowed(bootstrap, route)) return <Result status="403" title="无权访问此页面" subTitle="路由已在客户端隐藏，服务端仍会独立执行授权校验。" />
  return children
}

const AdminLayout = ({ children }: PropsWithChildren) => {
  const { bootstrap, logout } = useRbac3Session()
  const navigate = useNavigate()
  const location = useLocation()
  if (!bootstrap) return null
  const navigation = visibleNavigation(bootstrap)
  return (
    <Layout className="rbac3-app-layout">
      <Layout.Sider width={248} theme="light" className="rbac3-sidebar">
        <Typography.Title level={4} className="rbac3-brand">RBAC3 权限平台</Typography.Title>
        <Menu
          mode="inline"
          selectedKeys={[navigation.find((item) => location.pathname === item.path)?.key ?? '']}
          items={navigation.map((item) => ({ key: item.key, label: item.title, onClick: () => navigate(item.path) }))}
        />
      </Layout.Sider>
      <Layout>
        <Layout.Header className="rbac3-header">
          <Space>
            <Typography.Text>{bootstrap.user.displayName || bootstrap.user.username}</Typography.Text>
            <Button onClick={() => void logout()}>退出登录</Button>
          </Space>
        </Layout.Header>
        <Layout.Content className="rbac3-content">{children}</Layout.Content>
      </Layout>
    </Layout>
  )
}
