import {useRbac3Authorization} from '@egon-cola/rbac3-react-sdk'
import {Result} from 'antd'
import {EnterpriseLayout, type EnterpriseLayoutConfig,} from '@egon-cola/admin-web-shared'
import {Navigate, Route, Routes} from 'react-router-dom'
import type {PropsWithChildren} from 'react'
import {version} from '../../package.json'
import {applicationRouteDescriptors, isRouteAllowed, resolveApplicationLanding, visibleNavigation} from './navigation'
import type {FeatureRouteDescriptor} from '../features/shared/RouteDescriptor'

export const ApplicationRouter = () => {
    const {about} = useRbac3Authorization()
  if (!about) return null
  const landing = resolveApplicationLanding(about)
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
    const {about} = useRbac3Authorization()
  if (!about || !isRouteAllowed(about, route)) return <Result status="403" title="无权访问此页面" subTitle="路由已在客户端隐藏，服务端仍会独立执行授权校验。" />
  return children
}

const AdminLayout = ({ children }: PropsWithChildren) => {
    const {about} = useRbac3Authorization()
  if (!about) return null
  // 导航由 SDK 的 visibleNavigation 提供（含权限过滤），shared 只负责渲染与高亮。
  const config: EnterpriseLayoutConfig = {
    platformName: 'RBAC3 权限平台',
    navigation: visibleNavigation(about),
    user: {
        name: about.user.subject,
        menu: [],
    },
    footer: { version },
  }
  return <EnterpriseLayout config={config}>{children}</EnterpriseLayout>
}
