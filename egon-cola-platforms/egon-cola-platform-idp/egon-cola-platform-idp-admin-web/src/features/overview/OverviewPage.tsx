import {Card, Col, Descriptions, Row, Space, Tag, Typography} from 'antd'
import {ClockCircleOutlined, SafetyOutlined, TeamOutlined, UserOutlined} from '@ant-design/icons'
import {useAuth} from '../../auth/AuthContext'
import {usePermission} from '@egon-cola/admin-web-shared'

export const OverviewPage = () => {
  const auth = useAuth()
    const {has} = usePermission(auth.bootstrap?.permissions ?? [])
    const b = auth.bootstrap
    if (!b) return null

    const permissionModules = [
        {key: 'users', label: '全局用户', perm: 'idp:identity-user:read'},
        {key: 'clients', label: 'OAuth 客户端', perm: 'idp:oauth-client:read'},
        {key: 'resource-servers', label: 'Resource Server', perm: 'idp:resource-server:read'},
        {key: 'keys', label: '签名密钥', perm: 'idp:signing-key:read'},
        {key: 'audits', label: '安全审计', perm: 'idp:audit:read'},
    ]

  return (
      <Row gutter={[16, 16]}>
          <Col xs={24} lg={14}>
              <Card title={<><UserOutlined/> 当前授权上下文</>}>
                  <Descriptions column={2} bordered size="small">
                      <Descriptions.Item label="全局身份">{b.identitySub}</Descriptions.Item>
                      <Descriptions.Item label="租户">{b.tenantId}</Descriptions.Item>
                      <Descriptions.Item label="会话 ID">{b.sessionId}</Descriptions.Item>
                      <Descriptions.Item label="RBAC3 用户 ID">{b.rbac3UserId}</Descriptions.Item>
                      <Descriptions.Item label="当前系统">{b.systemCode}</Descriptions.Item>
                      <Descriptions.Item label="策略版本">{b.policyVersion}</Descriptions.Item>
                      <Descriptions.Item label="认证版本">{b.authVersion}</Descriptions.Item>
                      <Descriptions.Item label="上下文版本">{b.contextVersion}</Descriptions.Item>
                  </Descriptions>
              </Card>
          </Col>
          <Col xs={24} lg={10}>
              <Card title={<><TeamOutlined/> 活跃角色</>}>
                  {b.activeRoleIds.length === 0
                      ? <Typography.Text type="secondary">无活跃角色</Typography.Text>
                      : <Space wrap>{b.activeRoleIds.map((r) => <Tag key={r} color="purple">{r}</Tag>)}</Space>
                  }
              </Card>
              <Card title={<><SafetyOutlined/> 权限模块访问</>} style={{marginTop: 16}}>
                  {permissionModules.map((m) => (
                      <Tag key={m.key} color={has(m.perm) ? 'green' : 'default'} style={{marginBottom: 8}}>
                          {m.label}{has(m.perm) ? ' ✓' : ' ✗'}
                      </Tag>
                  ))}
              </Card>
              <Card title={<><ClockCircleOutlined/> 授权时间</>} style={{marginTop: 16}}>
                  <Descriptions column={1} bordered size="small">
                      <Descriptions.Item label="签发时间">{b.generatedAt}</Descriptions.Item>
                      <Descriptions.Item label="过期时间">{b.expiresAt}</Descriptions.Item>
                  </Descriptions>
              </Card>
          </Col>
      </Row>
  )
}
