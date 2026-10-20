import {Alert, Button, Card, Typography} from 'antd'
import {Navigate, useLocation} from 'react-router-dom'
import {useAuth} from './AuthContext'

export const LoginPage = () => {
  const auth = useAuth()
  const location = useLocation()
  if (auth.authorization && !auth.loading) {
    const from = location.state?.from
    const destination = typeof from === 'string' && from.startsWith('/')
      && !from.startsWith('//') && !from.includes('\\') && from !== '/login'
      ? from : auth.workspace === 'yaoguang' ? '/knowledge' : '/dashboard'
    return <Navigate replace to={destination} />
  }
  return (
    <main className="login-shell">
      <Card className="login-card">
        <Typography.Title level={2}>{auth.workspace === 'yaoguang' ? '知识库与 Wiki' : 'Yuheng Admin'}</Typography.Title>
        <Typography.Paragraph type="secondary">
          跳转到统一身份平台完成 OIDC 登录；浏览器只持有 Edge 的 HttpOnly 会话 Cookie。
        </Typography.Paragraph>
        {auth.error && <Alert type="warning" showIcon title={auth.error} />}
        <Button block type="primary" loading={auth.loading} onClick={auth.login}>
          使用统一身份登录
        </Button>
      </Card>
    </main>
  )
}
