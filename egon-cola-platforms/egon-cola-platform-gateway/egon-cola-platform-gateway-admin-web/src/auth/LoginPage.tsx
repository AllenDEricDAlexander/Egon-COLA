import {Alert, Button, Card, Form, Input, Typography} from 'antd'
import {useAuth} from './AuthContext'

interface LoginForm {
  readonly tenantId: string
    readonly username: string
    readonly password: string
}

export const LoginPage = () => {
  const auth = useAuth()
  return (
    <main className="login-shell">
      <Card className="login-card">
        <Typography.Title level={2}>Gateway Admin</Typography.Title>
        <Typography.Paragraph type="secondary">
            通过 Gateway 的公开身份接口登录；USER AT/RT 仅由 IdP 写入 HttpOnly Cookie。
        </Typography.Paragraph>
        {auth.error && <Alert type="error" showIcon message={auth.error} />}
        <Form<LoginForm>
          layout="vertical"
          initialValues={{ tenantId: import.meta.env.VITE_DEFAULT_TENANT_ID ?? 'default' }}
          onFinish={(values) => void auth.login(values.tenantId, values.username, values.password)}
        >
          <Form.Item name="tenantId" label="租户 ID" rules={[{ required: true }]}>
            <Input autoComplete="organization" />
          </Form.Item>
            <Form.Item name="username" label="用户名" rules={[{required: true}]}>
                <Input autoComplete="username"/>
            </Form.Item>
            <Form.Item name="password" label="密码" rules={[{required: true}]}>
                <Input.Password autoComplete="current-password"/>
            </Form.Item>
          <Button block type="primary" htmlType="submit" loading={auth.loading}>
              登录
          </Button>
        </Form>
      </Card>
    </main>
  )
}
