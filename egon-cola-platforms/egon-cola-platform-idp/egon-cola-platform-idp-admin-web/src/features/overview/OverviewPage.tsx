import { Card, Descriptions } from 'antd'
import { useAuth } from '../../auth/AuthContext'

export const OverviewPage = () => {
  const auth = useAuth()
  if (!auth.bootstrap) return null
  return (
    <Card title="当前授权上下文">
      <Descriptions column={2} bordered>
        <Descriptions.Item label="全局身份">{auth.bootstrap.identitySub}</Descriptions.Item>
        <Descriptions.Item label="租户">{auth.bootstrap.tenantId}</Descriptions.Item>
        <Descriptions.Item label="RBAC3 用户">{auth.bootstrap.rbac3UserId}</Descriptions.Item>
        <Descriptions.Item label="系统">{auth.bootstrap.systemCode}</Descriptions.Item>
        <Descriptions.Item label="权限数">{auth.bootstrap.permissions.length}</Descriptions.Item>
        <Descriptions.Item label="策略版本">{auth.bootstrap.policyVersion}</Descriptions.Item>
      </Descriptions>
    </Card>
  )
}
