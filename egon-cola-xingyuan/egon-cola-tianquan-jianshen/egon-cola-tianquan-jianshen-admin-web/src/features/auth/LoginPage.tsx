import {Alert, Button, Card, Typography} from 'antd'
import {useState} from 'react'
import {gatewayAuth} from './gatewayAuth'

export const LoginPage = ({notice}: {readonly notice?: string}) => {
  const [error, setError] = useState<string>()
  const login = () => {
    setError(undefined)
    try {
      gatewayAuth.login()
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : '统一身份登录失败')
    }
  }

  return (
    <main className="tianquan-jianshen-auth-page">
      <Card title="Tianquan-Jianshen 权限平台" className="tianquan-jianshen-login-card">
        <Typography.Paragraph type="secondary">
          跳转到统一身份平台完成 OIDC 登录；鉴神只使用经验证的 RBAC 授权上下文。
        </Typography.Paragraph>
        {notice && <Alert type="warning" showIcon message={notice} style={{marginBottom: 16}} />}
        {error && <Alert type="error" showIcon message={error} style={{marginBottom: 16}} />}
        <Button type="primary" block onClick={login}>
          使用统一身份登录
        </Button>
      </Card>
    </main>
  )
}
