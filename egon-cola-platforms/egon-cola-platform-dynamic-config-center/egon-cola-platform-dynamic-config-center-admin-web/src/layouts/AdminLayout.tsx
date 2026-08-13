import {
  ApartmentOutlined,
  AppstoreOutlined,
  CloudOutlined,
  ClusterOutlined,
  DatabaseOutlined,
  DeploymentUnitOutlined,
  FileTextOutlined,
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

// 平台自己的导航数据；分组仅用于窄屏抽屉导航的展示。
const navigation: readonly EnterpriseNavigationItem[] = [
  { key: 'registry', label: '服务注册', path: '/registry', icon: <ClusterOutlined />, group: '运行状态' },
  { key: 'publish-tasks', label: '发布任务', path: '/publish-tasks', icon: <DeploymentUnitOutlined />, group: '运行状态' },
  { key: 'cache', label: '缓存', path: '/cache', icon: <DatabaseOutlined />, group: '运行状态' },
  { key: 'configs', label: '配置资源', path: '/configs', icon: <FileTextOutlined />, group: '配置管理' },
  { key: 'bizs', label: '业务域', path: '/bizs', icon: <PartitionOutlined />, group: '元数据管理' },
  { key: 'envs', label: '环境', path: '/envs', icon: <CloudOutlined />, group: '元数据管理' },
  { key: 'apps', label: '应用', path: '/apps', icon: <AppstoreOutlined />, group: '元数据管理' },
  { key: 'namespaces', label: '命名空间', path: '/namespaces', icon: <ApartmentOutlined />, group: '元数据管理' },
]

export default function AdminLayout() {
  const { identity, logout } = useAuth()

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

  return (
    <EnterpriseLayout config={config}>
      <Outlet />
    </EnterpriseLayout>
  )
}
