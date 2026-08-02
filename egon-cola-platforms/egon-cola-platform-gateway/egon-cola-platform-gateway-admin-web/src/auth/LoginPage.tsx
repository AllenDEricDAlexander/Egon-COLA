import { Alert, Button, Card, Form, Input, Typography } from 'antd'
import { useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'

interface LoginForm {
  readonly tenantId: string
}

export const LoginPage = () => {
  const auth = useAuth()
  const location = useLocation()
  const returnTo = (location.state as { from?: string } | undefined)?.from
    ?? '/dashboard'
  return (
    <main className="login-shell">
      <Card className="login-card">
        <Typography.Title level={2}>Gateway Admin</Typography.Title>
        <Typography.Paragraph type="secondary">
          通过统一身份平台登录。Access Token 仅保存在当前页面内存中，刷新凭据由 IdP 的 HttpOnly Cookie 管理。
        </Typography.Paragraph>
        {auth.error && <Alert type="error" showIcon message={auth.error} />}
        <Form<LoginForm>
          layout="vertical"
          initialValues={{ tenantId: import.meta.env.VITE_DEFAULT_TENANT_ID ?? 'default' }}
          onFinish={(values) => void auth.login(values.tenantId, returnTo)}
        >
          <Form.Item name="tenantId" label="租户 ID" rules={[{ required: true }]}>
            <Input autoComplete="organization" />
          </Form.Item>
          <Button block type="primary" htmlType="submit" loading={auth.loading}>
            使用统一身份登录
          </Button>
        </Form>
      </Card>
    </main>
  )
}
