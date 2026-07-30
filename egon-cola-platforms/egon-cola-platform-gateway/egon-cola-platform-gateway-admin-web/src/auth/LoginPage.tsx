import { Alert, Button, Card, Checkbox, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from './AuthContext'

export const LoginPage = () => {
  const auth = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [error, setError] = useState<string>()
  if (auth.session) return <Navigate replace to="/dashboard" />
  return (
    <main className="login-shell">
      <Card className="login-card">
        <Typography.Title level={2}>Gateway Admin 登录</Typography.Title>
        <Typography.Paragraph type="secondary">
          使用企业身份系统签发的 Bearer Token。Token 只保存在当前会话，除非明确勾选持久化。
        </Typography.Paragraph>
        {error && <Alert type="error" showIcon message={error} />}
        <Form
          layout="vertical"
          onFinish={async (values) => {
            setError(undefined)
            try {
              await auth.login({
                accessToken: values.accessToken,
                refreshToken: values.refreshToken,
              }, values.remember)
              const target = (location.state as { from?: string } | undefined)?.from
              navigate(target ?? '/dashboard', { replace: true })
            } catch (failure) {
              setError(failure instanceof Error ? failure.message : '登录失败')
            }
          }}
        >
          <Form.Item name="accessToken" label="Access Token" rules={[{ required: true }]}>
            <Input.Password autoComplete="off" />
          </Form.Item>
          <Form.Item name="refreshToken" label="Refresh Token（可选）">
            <Input.Password autoComplete="off" />
          </Form.Item>
          <Form.Item name="remember" valuePropName="checked">
            <Checkbox>在此浏览器持久化登录</Checkbox>
          </Form.Item>
          <Button block type="primary" htmlType="submit" loading={auth.loading}>
            登录并校验权限
          </Button>
        </Form>
      </Card>
    </main>
  )
}
