import {Card, Col, Descriptions, Row, Space, Tag, Typography} from 'antd'
import {ClockCircleOutlined, SafetyOutlined, TeamOutlined, UserOutlined} from '@ant-design/icons'
import {useQuery} from '@tanstack/react-query'
import {PageState, usePermission} from '@egon-cola/admin-web-shared'
import {httpClient, useAuth} from '../../auth/AuthContext'
import type {IdentityProfileView} from '../../api/types'

export const OverviewPage = () => {
  const auth = useAuth()
    const {has} = usePermission(auth.bootstrap?.permissions ?? [])
    const b = auth.bootstrap
    const profile = useQuery({
        queryKey: ['idp', 'identity-profile'],
        queryFn: () => httpClient.request<IdentityProfileView>('/api/v1/identity/me'),
        enabled: Boolean(b),
        retry: false,
    })
    if (!b) return null
    const activeRoleIds = b.activeRoleContexts.flatMap((context) => context.effectiveRoleIds)

    const permissionModules = [
        {key: 'users', label: '全局用户', perm: 'idp:identity-user:read'},
        {key: 'clients', label: 'OAuth 客户端', perm: 'idp:oauth-client:read'},
        {key: 'tenants', label: '租户目录', perm: 'idp:tenant:read'},
        {key: 'resource-servers', label: 'Resource Server', perm: 'idp:resource-server:read'},
        {key: 'keys', label: '签名密钥', perm: 'idp:signing-key:read'},
        {key: 'audits', label: '安全审计', perm: 'idp:audit:read'},
    ]

  return (
      <Row gutter={[16, 16]}>
          <Col xs={24}>
              <Card title="当前统一身份">
                  <PageState
                      loading={profile.isPending}
                      error={profile.error}
                      empty={!profile.data}
                      emptyDescription="当前统一身份不可用"
                      onRetry={() => {void profile.refetch()}}
                  >
                      {profile.data && (
                          <Descriptions column={2} bordered size="small">
                              <Descriptions.Item label="主体">{profile.data.subject}</Descriptions.Item>
                              <Descriptions.Item label="租户">{profile.data.tenantId}</Descriptions.Item>
                              <Descriptions.Item label="签发时间">{profile.data.issuedAt ?? '-'}</Descriptions.Item>
                              <Descriptions.Item label="过期时间">{profile.data.expiresAt ?? '-'}</Descriptions.Item>
                          </Descriptions>
                      )}
                  </PageState>
              </Card>
          </Col>
          <Col xs={24} lg={14}>
              <Card title={<><UserOutlined/> 当前授权上下文</>}>
                  <Descriptions column={2} bordered size="small">
                      <Descriptions.Item label="全局身份">{b.user.identitySub}</Descriptions.Item>
                      <Descriptions.Item label="租户">{b.user.tenantId}</Descriptions.Item>
                      <Descriptions.Item label="RBAC3 用户 ID">{b.user.id}</Descriptions.Item>
                      <Descriptions.Item label="身份状态">{b.user.status}</Descriptions.Item>
                      <Descriptions.Item label="策略版本">{b.policyVersion}</Descriptions.Item>
                      <Descriptions.Item label="认证版本">{b.authVersion}</Descriptions.Item>
                  </Descriptions>
              </Card>
          </Col>
          <Col xs={24} lg={10}>
              <Card title={<><TeamOutlined/> 活跃角色</>}>
                  {activeRoleIds.length === 0
                      ? <Typography.Text type="secondary">无活跃角色</Typography.Text>
                      : <Space wrap>{activeRoleIds.map((r) => <Tag key={r} color="purple">{r}</Tag>)}</Space>
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
                      <Descriptions.Item label="授权版本">{b.authVersion}</Descriptions.Item>
                      <Descriptions.Item label="策略版本">{b.policyVersion}</Descriptions.Item>
                  </Descriptions>
              </Card>
          </Col>
      </Row>
  )
}
