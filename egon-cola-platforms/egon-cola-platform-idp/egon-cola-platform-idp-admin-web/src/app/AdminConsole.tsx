import { Button, Card, Descriptions, Form, Input, Layout, Menu, Modal, Space, Table, Tag, Typography, message } from 'antd'
import { useCallback, useMemo, useState } from 'react'
import { idpApi } from '../api/idpApi'
import type { AuditPage, IdentityUser, OAuthClientView, SigningKeyView } from '../api/types'
import { useAuth } from '../auth/AuthContext'

type Section = 'overview' | 'users' | 'clients' | 'keys' | 'audits'

export const AdminConsole = () => {
  const auth = useAuth()
  const [section, setSection] = useState<Section>('overview')
  const [users, setUsers] = useState<readonly IdentityUser[]>([])
  const [clients, setClients] = useState<readonly OAuthClientView[]>([])
  const [keys, setKeys] = useState<readonly SigningKeyView[]>([])
  const [audits, setAudits] = useState<AuditPage>({ content: [], totalElements: 0 })
  const [loading, setLoading] = useState(false)
  const [userModal, setUserModal] = useState(false)
  const [clientModal, setClientModal] = useState(false)
  const [userForm] = Form.useForm()
  const [clientForm] = Form.useForm()
  const [messageApi, contextHolder] = message.useMessage()

  const load = useCallback(async (target: Section) => {
    if (target === 'overview') return
    try {
      if (target === 'users') {
        setUsers(await idpApi<IdentityUser[]>('/api/v1/identity/users'))
      } else if (target === 'clients') {
        setClients(await idpApi<OAuthClientView[]>('/api/v1/identity/clients'))
      } else if (target === 'keys') {
        setKeys(await idpApi<SigningKeyView[]>('/api/v1/identity/signing-keys'))
      } else {
        setAudits(await idpApi<AuditPage>('/api/v1/identity/audits?page=0&size=100'))
      }
    } catch (failure) {
      void messageApi.error(failure instanceof Error ? failure.message : '加载失败')
    }
  }, [messageApi])

  const reload = useCallback(async (target: Section) => {
    setLoading(true)
    await load(target)
    setLoading(false)
  }, [load])

  const permissions = useMemo(
    () => new Set(auth.bootstrap?.permissions ?? []),
    [auth.bootstrap?.permissions],
  )
  const items = [
    { key: 'overview', label: '身份概览' },
    permissions.has('idp:identity-user:read') ? { key: 'users', label: '全局用户' } : null,
    permissions.has('idp:oauth-client:read') ? { key: 'clients', label: 'OAuth 客户端' } : null,
    permissions.has('idp:signing-key:read') ? { key: 'keys', label: '签名密钥' } : null,
    permissions.has('idp:audit:read') ? { key: 'audits', label: '安全审计' } : null,
  ].filter((item): item is { key: string; label: string } => item !== null)

  const createUser = async () => {
    const values = await userForm.validateFields() as { username: string; displayName: string }
    const created = await idpApi<{ oneTimePassword: string }>('/api/v1/identity/users', {
      method: 'POST',
      body: JSON.stringify(values),
    })
    setUserModal(false)
    userForm.resetFields()
    Modal.success({
      title: '用户已创建',
      content: `一次性密码：${created.oneTimePassword}（关闭后不再显示）`,
    })
    await reload('users')
  }

  const createClient = async () => {
    const values = await clientForm.validateFields() as {
      clientId: string
      clientName: string
      redirectUri: string
      audience: string
    }
    await idpApi('/api/v1/identity/clients', {
      method: 'POST',
      body: JSON.stringify({
        clientId: values.clientId,
        clientName: values.clientName,
        accessTokenTtlSeconds: 900,
        refreshTokenTtlSeconds: 604800,
        redirectUris: [values.redirectUri],
        audiences: [values.audience],
      }),
    })
    setClientModal(false)
    clientForm.resetFields()
    await reload('clients')
  }

  const resetPassword = async (subject: string) => {
    const result = await idpApi<{ oneTimePassword: string }>(
      `/api/v1/identity/users/${encodeURIComponent(subject)}/password-reset`,
      { method: 'POST' },
    )
    Modal.success({ title: '密码已重置', content: `一次性密码：${result.oneTimePassword}` })
  }

  const revokeAll = async (subject: string) => {
    await idpApi(`/api/v1/identity/users/${encodeURIComponent(subject)}/revoke-all`, {
      method: 'POST',
    })
    void messageApi.success('该用户的全部刷新会话已撤销')
    await reload('users')
  }

  return (
    <Layout className="app-layout">
      {contextHolder}
      <Layout.Sider width={240} theme="light">
        <Typography.Title level={4} className="brand">统一身份平台</Typography.Title>
        <Menu
          mode="inline"
          selectedKeys={[section]}
          items={items}
          onClick={({ key }) => {
            const target = key as Section
            setSection(target)
            void reload(target)
          }}
        />
      </Layout.Sider>
      <Layout>
        <Layout.Header className="app-header">
          <Space>
            <Typography.Text>{auth.bootstrap?.identitySub}</Typography.Text>
            <Tag>{auth.bootstrap?.tenantId}</Tag>
            <Button onClick={() => void auth.logout()}>退出当前系统</Button>
          </Space>
        </Layout.Header>
        <Layout.Content className="app-content">
          {section === 'overview' && auth.bootstrap && (
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
          )}
          {section === 'users' && (
            <Card title="全局身份用户" extra={permissions.has('idp:identity-user:create') && <Button type="primary" onClick={() => setUserModal(true)}>创建用户</Button>}>
              <Table loading={loading} rowKey="subject" dataSource={[...users]} columns={[
                { title: '用户名', dataIndex: 'username' },
                { title: '显示名', dataIndex: 'displayName' },
                { title: '状态', dataIndex: 'status', render: (value) => <Tag>{value}</Tag> },
                { title: 'Token 版本', dataIndex: 'tokenVersion' },
                { title: '操作', render: (_, row) => <Space>
                  {permissions.has('idp:identity-user:password-reset') && <Button size="small" onClick={() => void resetPassword(row.subject)}>重置密码</Button>}
                  {permissions.has('idp:identity-user:revoke-all') && <Button size="small" danger onClick={() => void revokeAll(row.subject)}>撤销会话</Button>}
                </Space> },
              ]} />
            </Card>
          )}
          {section === 'clients' && (
            <Card title="OAuth 公共客户端" extra={permissions.has('idp:oauth-client:create') && <Button type="primary" onClick={() => setClientModal(true)}>创建客户端</Button>}>
              <Table loading={loading} rowKey="clientId" dataSource={[...clients]} columns={[
                { title: 'Client ID', dataIndex: 'clientId' },
                { title: '名称', dataIndex: 'clientName' },
                { title: '状态', dataIndex: 'status' },
                { title: 'PKCE', dataIndex: 'pkceRequired', render: (value) => value ? 'S256' : '否' },
                { title: '回调地址', dataIndex: 'redirectUris', render: (values: string[]) => values.join(', ') },
                { title: 'Audience', dataIndex: 'audiences', render: (values: string[]) => values.join(', ') },
              ]} />
            </Card>
          )}
          {section === 'keys' && (
            <Card title="签名密钥（私钥永不返回浏览器）">
              <Table loading={loading} rowKey="kid" dataSource={[...keys]} columns={[
                { title: 'KID', dataIndex: 'kid' },
                { title: '算法', dataIndex: 'algorithm' },
                { title: '状态', dataIndex: 'status' },
                { title: '当前服务', dataIndex: 'runtimeServing', render: (value) => value ? '是' : '否' },
                { title: '版本', dataIndex: 'version' },
              ]} />
            </Card>
          )}
          {section === 'audits' && (
            <Card title={`安全审计（${audits.totalElements}）`}>
              <Table loading={loading} rowKey="id" dataSource={[...audits.content]} columns={[
                { title: '时间', dataIndex: 'occurredAt' },
                { title: '事件', dataIndex: 'eventType' },
                { title: '操作者', dataIndex: 'actorSub' },
                { title: '目标', dataIndex: 'targetSub' },
                { title: '结果', dataIndex: 'result' },
                { title: '原因', dataIndex: 'reason' },
              ]} />
            </Card>
          )}
        </Layout.Content>
      </Layout>
      <Modal title="创建全局身份用户" open={userModal} onCancel={() => setUserModal(false)} onOk={() => void createUser()}>
        <Form form={userForm} layout="vertical">
          <Form.Item name="username" label="用户名" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="displayName" label="显示名" rules={[{ required: true }]}><Input /></Form.Item>
        </Form>
      </Modal>
      <Modal title="创建 OAuth 公共客户端" open={clientModal} onCancel={() => setClientModal(false)} onOk={() => void createClient()}>
        <Form form={clientForm} layout="vertical">
          <Form.Item name="clientId" label="Client ID" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="clientName" label="名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="redirectUri" label="精确回调地址" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="audience" label="Audience" rules={[{ required: true }]}><Input /></Form.Item>
        </Form>
      </Modal>
    </Layout>
  )
}
