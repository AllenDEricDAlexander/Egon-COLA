import { Alert, Button, Card, Typography } from 'antd'
import { useAuth } from './AuthContext'

export default function LoginPage() {
  const auth = useAuth()
  return (
    <div style={{ maxWidth: 480, margin: '96px auto', padding: '0 16px' }}>
      <Card>
        <Typography.Title level={4}>Tianshu 管理端</Typography.Title>
        <Typography.Paragraph type="secondary">
          跳转到统一身份平台完成 OIDC 登录；浏览器只持有 Edge 的 HttpOnly 会话 Cookie。
        </Typography.Paragraph>
        {auth.error && <Alert type="warning" showIcon message={auth.error} />}
        <Button type="primary" block loading={auth.loading} onClick={() => void auth.login()}>
          使用统一身份登录
        </Button>
      </Card>
    </div>
  )
}
