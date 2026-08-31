import {Breadcrumb, Tag} from 'antd'
import type {ItemType} from 'antd/es/breadcrumb/Breadcrumb'
import {HomeOutlined, LogoutOutlined} from '@ant-design/icons'
import {
    EnterpriseLayout,
    type EnterpriseLayoutConfig,
    type EnterpriseNavigationItem,
    usePermission,
} from '@egon-cola/admin-web-shared'
import {type PropsWithChildren, useMemo} from 'react'
import {useLocation, useNavigate} from 'react-router-dom'
import {useAuth} from '../auth/AuthContext'
import {version} from '../../package.json'

interface NavItem {
    key: string
    label: string
    path?: string
    permission: string
    activePathPrefixes?: readonly string[]
    children?: readonly NavItem[]
}

// 平台自己的导航树；按 bootstrap 权限递归剪枝后交给 shared 左侧树。
const ALL_NAV_ITEMS: NavItem[] = [
    {key: 'overview', label: '身份概览', path: '/overview', permission: ''},
    {key: 'identity', label: '身份目录', permission: '', children: [
        {key: 'users', label: '全局用户', path: '/users', permission: 'idp:identity-user:read'},
        {key: 'tenants', label: '租户目录', path: '/tenants', permission: 'idp:tenant:read'},
    ]},
    {key: 'oauth', label: 'OAuth 与资源', permission: '', children: [
        {key: 'clients', label: 'OAuth 客户端', path: '/clients', activePathPrefixes: ['/clients'], permission: 'idp:oauth-client:read'},
        {key: 'resource-servers', label: 'Resource Server', path: '/resource-servers', permission: 'idp:resource-server:read'},
    ]},
    {key: 'security', label: '安全治理', permission: '', children: [
        {key: 'keys', label: '签名密钥', path: '/keys', permission: 'idp:signing-key:read'},
        {key: 'audits', label: '安全审计', path: '/audits', permission: 'idp:audit:read'},
    ]},
]

const PATH_LABELS: Record<string, string> = {
    '/overview': '身份概览',
    '/users': '全局用户',
    '/clients': 'OAuth 客户端',
    '/tenants': '租户目录',
    '/resource-servers': 'Resource Server',
    '/keys': '签名密钥',
    '/audits': '安全审计',
}

export const AdminLayout = ({ children, embedded = false }: PropsWithChildren<{ readonly embedded?: boolean }>) => {
    const auth = useAuth()
    const navigate = useNavigate()
    const location = useLocation()
    const { has } = usePermission(auth.bootstrap?.permissions ?? [])

    const navigation: EnterpriseNavigationItem[] = useMemo(
        () => filterNavigation(ALL_NAV_ITEMS, has),
        [has],
    )

    const currentPath = location.pathname
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

    const config: EnterpriseLayoutConfig = {
        platformName: '统一身份平台',
        navigation,
        actions: auth.bootstrap
            ? <Tag color="blue">{auth.bootstrap.user.tenantId}</Tag>
            : undefined,
        user: auth.bootstrap
            ? {
                name: auth.bootstrap.user.identitySub,
                description: `Tenant: ${auth.bootstrap.user.tenantId}`,
                menu: [
                    {
                        key: 'logout',
                        label: '退出登录',
                        icon: <LogoutOutlined/>,
                        onClick: () => {
                            void auth.logout()
                            navigate('/login', { replace: true })
                        },
                    },
                ],
            }
            : undefined,
        footer: { version },
    }

    const layoutConfig: EnterpriseLayoutConfig = embedded
        ? {...config, hideHeader: true, hideFooter: true}
        : config

    return (
        <EnterpriseLayout config={layoutConfig}>
            <Breadcrumb items={breadcrumbItems} style={{ marginBottom: 16 }} />
            {children}
        </EnterpriseLayout>
    )
}

const filterNavigation = (
    items: readonly NavItem[],
    has: (permission: string) => boolean,
): EnterpriseNavigationItem[] => items.flatMap((item): EnterpriseNavigationItem[] => {
    const children = item.children ? filterNavigation(item.children, has) : []
    const allowed = !item.permission || has(item.permission)
    if (item.children) {
        return children.length > 0 ? [{key: item.key, label: item.label, children}] : []
    }
    return allowed ? [{
        key: item.key,
        label: item.label,
        path: item.path,
        activePathPrefixes: item.activePathPrefixes,
    }] : []
})
