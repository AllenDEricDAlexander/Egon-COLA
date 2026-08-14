import {Alert, Button, Card, Form, Input, Typography} from 'antd'
import {useState} from 'react'
import {useAuth} from './AuthContext'

interface LoginForm {
  readonly tenantId: string
  readonly username: string
  readonly password: string
}

export const CentralLoginPage = () => {
    const auth = useAuth()
  const [error, setError] = useState<string>()
  const submit = async (values: LoginForm) => {
    setError(undefined)
    try {
        await auth.login(values.tenantId, values.username, values.password)
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : '登录失败')
    }
  }
  return (
    <main className="login-page">
      <Card className="login-card">
        <Typography.Title level={2}>Egon 统一身份平台</Typography.Title>
        <Typography.Paragraph type="secondary">
            登录凭据只提交到 Gateway 的公开身份接口，USER AT/RT 由 IdP 写入 HttpOnly Cookie。
        </Typography.Paragraph>
          {(error || auth.bootstrap === undefined && !auth.loading) && (
              <Alert type="error" showIcon message={error ?? '登录失败'}/>
          )}
        <Form<LoginForm>
          layout="vertical"
          initialValues={{ tenantId: import.meta.env.VITE_DEFAULT_TENANT_ID ?? 'default' }}
          onFinish={(values) => void submit(values)}
        >
            <Form.Item name="tenantId" label="租户 ID" rules={[{required: true}]}>
                <Input autoComplete="organization"/>
            </Form.Item>
          <Form.Item name="username" label="用户名" rules={[{ required: true }]}>
            <Input autoComplete="username" />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true }]}>
            <Input.Password autoComplete="current-password" />
          </Form.Item>
            <Button block type="primary" htmlType="submit" loading={auth.loading}>
            登录
          </Button>
        </Form>
      </Card>
    </main>
  )
}
