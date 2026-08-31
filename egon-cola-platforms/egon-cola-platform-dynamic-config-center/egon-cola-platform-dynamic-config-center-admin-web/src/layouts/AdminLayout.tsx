import {
  ApartmentOutlined,
  AppstoreOutlined,
  CloudOutlined,
  ClusterOutlined,
  DatabaseOutlined,
  DeploymentUnitOutlined,
  FileTextOutlined,
  LinkOutlined,
  LogoutOutlined,
  PartitionOutlined,
} from '@ant-design/icons'
import { Badge } from 'antd'
import {
  EnterpriseLayout,
  type EnterpriseLayoutConfig,
  type EnterpriseNavigationItem,
} from '@egon-cola/admin-web-shared'
import { Outlet } from 'react-router-dom'
import { version } from '../../package.json'
import { useAuth } from '../auth/AuthContext'

interface WujieRuntimeWindow extends Window {
  $wujie?: { props?: { embedded?: boolean } }
}

// 平台自己的导航树；shared 只负责左侧递归树的渲染与高亮。
const navigation: readonly EnterpriseNavigationItem[] = [
  { key: 'runtime', label: '运行状态', icon: <ClusterOutlined />, children: [
    { key: 'registry', label: '服务注册', path: '/registry', icon: <ClusterOutlined /> },
    { key: 'instances', label: '配置客户端实例', path: '/instances', icon: <ClusterOutlined /> },
    { key: 'publish-tasks', label: '发布任务', path: '/publish-tasks', icon: <DeploymentUnitOutlined /> },
    { key: 'cache', label: '缓存', path: '/cache', icon: <DatabaseOutlined /> },
  ] },
  { key: 'configuration', label: '配置管理', icon: <FileTextOutlined />, children: [
    { key: 'configs', label: '配置资源', path: '/configs', icon: <FileTextOutlined /> },
  ] },
  { key: 'metadata', label: '元数据管理', icon: <PartitionOutlined />, children: [
    { key: 'bizs', label: '业务域', path: '/bizs', icon: <PartitionOutlined /> },
    { key: 'envs', label: '环境', path: '/envs', icon: <CloudOutlined /> },
    { key: 'apps', label: '应用', path: '/apps', icon: <AppstoreOutlined /> },
    { key: 'namespaces', label: '命名空间', path: '/namespaces', icon: <ApartmentOutlined /> },
    { key: 'bindings', label: '作用域绑定', path: '/bindings', icon: <LinkOutlined /> },
  ] },
]

export default function AdminLayout() {
  const { identity, logout } = useAuth()
  const embedded = (window as WujieRuntimeWindow).$wujie?.props?.embedded === true

  const config: EnterpriseLayoutConfig = {
    platformName: 'DDC Admin',
    navigation,
    actions: <Badge status="success" text="DDC 已连接" />,
    user: {
      name: identity,
      menu: [
        {
          key: 'logout',
          label: '退出登录',
          icon: <LogoutOutlined />,
          onClick: () => {
            void logout()
          },
        },
      ],
    },
    footer: { version },
  }
  const layoutConfig: EnterpriseLayoutConfig = embedded
    ? {...config, hideHeader: true, hideFooter: true}
    : config

  return (
    <EnterpriseLayout config={layoutConfig}>
      <Outlet />
    </EnterpriseLayout>
  )
}
