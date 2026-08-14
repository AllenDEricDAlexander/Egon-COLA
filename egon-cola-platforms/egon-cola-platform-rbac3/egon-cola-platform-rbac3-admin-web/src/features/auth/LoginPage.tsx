import {Button, Card, Form, Input, Typography} from 'antd'
import {gatewayAuth} from './gatewayAuth'

export const LoginPage = ({onSuccess}: { readonly onSuccess: () => Promise<void> | void }) => {
  return (
    <main className="rbac3-auth-page">
      <Card title="RBAC3 权限平台" className="rbac3-login-card">
        <Typography.Paragraph type="secondary">
            通过 Gateway 统一身份入口登录。浏览器只使用 HttpOnly Cookie，RBAC3 只负责授权。
        </Typography.Paragraph>
        <Form
          layout="vertical"
          initialValues={{ tenantId: import.meta.env.VITE_DEFAULT_TENANT_ID ?? 'default' }}
          onFinish={async (values: { tenantId: string; username: string; password: string }) => {
              await gatewayAuth.login(values)
              await onSuccess()
          }}
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
          <Button type="primary" htmlType="submit" block>
            使用统一身份登录
          </Button>
        </Form>
      </Card>
    </main>
  )
}
