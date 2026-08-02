import { Button, Card, Form, Input, Typography } from 'antd'
import { useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'

export default function LoginPage() {
  const auth = useAuth()
  const location = useLocation()
  const returnTo = (location.state as { from?: string } | undefined)?.from ?? '/'
  return (
    <div style={{ maxWidth: 480, margin: '96px auto', padding: '0 16px' }}>
      <Card>
        <Typography.Title level={4}>DDC 管理端</Typography.Title>
        <Typography.Paragraph type="secondary">
          使用统一身份平台登录；页面不接收、不持久化任何手工 Token。
        </Typography.Paragraph>
        <Form
          layout="vertical"
          initialValues={{ tenantId: import.meta.env.VITE_DEFAULT_TENANT_ID ?? 'default' }}
          onFinish={(values: { tenantId: string }) => void auth.login(values.tenantId, returnTo)}
        >
          <Form.Item name="tenantId" label="租户 ID" rules={[{ required: true }]}>
            <Input autoComplete="organization" />
          </Form.Item>
          <Button type="primary" block htmlType="submit" loading={auth.loading}>
            使用统一身份登录
          </Button>
        </Form>
      </Card>
    </div>
  )
}
