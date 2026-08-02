import { Button, Card, Form, Input, Typography } from 'antd'
import { useLocation } from 'react-router-dom'
import { rbac3OAuth } from './oauthClient'

export const LoginPage = () => {
  const location = useLocation()
  return (
    <main className="rbac3-auth-page">
      <Card title="RBAC3 权限平台" className="rbac3-login-card">
        <Typography.Paragraph type="secondary">
          通过统一身份平台登录。RBAC3 只负责当前租户、当前系统的授权，不再签发登录 Token。
        </Typography.Paragraph>
        <Form
          layout="vertical"
          initialValues={{ tenantId: import.meta.env.VITE_DEFAULT_TENANT_ID ?? 'default' }}
          onFinish={(values: { tenantId: string }) => void rbac3OAuth.beginAuthorization(
            values.tenantId,
            location.pathname === '/' ? '/' : location.pathname,
          )}
        >
          <Form.Item name="tenantId" label="租户 ID" rules={[{ required: true }]}>
            <Input autoComplete="organization" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block>
            使用统一身份登录
          </Button>
        </Form>
      </Card>
    </main>
  )
}
