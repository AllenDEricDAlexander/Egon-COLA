import {Alert, Button, Card, Typography} from 'antd'
import {useState} from 'react'
import {useAuth} from './AuthContext'

export const CentralLoginPage = () => {
  const auth = useAuth()
  const [error, setError] = useState<string>()
  const login = () => {
    setError(undefined)
    try {
      auth.login()
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : '登录失败')
    }
  }
  return (
    <main className="login-page">
      <Card className="login-card">
        <Typography.Title level={2}>Egon 统一身份平台</Typography.Title>
        <Typography.Paragraph type="secondary">
          通过统一身份平台完成 OIDC 登录；浏览器只持有 Edge 的 HttpOnly 会话 Cookie。
        </Typography.Paragraph>
        {(error || auth.error) && (
          <Alert type="error" showIcon message={error ?? auth.error} />
        )}
        <Button block type="primary" loading={auth.loading} onClick={login}>
          使用统一身份登录
        </Button>
      </Card>
    </main>
  )
}
