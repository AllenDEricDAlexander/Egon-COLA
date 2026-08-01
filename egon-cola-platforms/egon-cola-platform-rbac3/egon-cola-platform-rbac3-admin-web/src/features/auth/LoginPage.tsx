import type { LoginRequest } from '@egon-cola/rbac3-react-sdk'
import { Alert, Button, Card, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import type { AuthApi } from './auth.api'

interface LoginForm { readonly tenantCode: string; readonly username: string; readonly password: string }

export interface LoginPageProps {
  readonly authApi: AuthApi
  readonly onAuthenticated: () => Promise<void>
}

export const LoginPage = ({ authApi, onAuthenticated }: LoginPageProps) => {
  const [form] = Form.useForm<LoginForm>()
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const submit = async (values: LoginForm) => {
    setSubmitting(true)
    setError(null)
    const request: LoginRequest = {
      tenantCode: values.tenantCode.trim(),
      username: values.username.trim(),
      password: values.password,
      device: { deviceId: crypto.randomUUID(), deviceName: 'RBAC3 Admin Web' },
    }
    try {
      await authApi.login(request)
      form.setFieldValue('password', '')
      await onAuthenticated()
    } catch (cause) {
      form.setFieldValue('password', '')
      setError(cause)
    } finally {
      setSubmitting(false)
    }
  }
  return (
    <main className="rbac3-auth-page">
      <Card title="RBAC3 管理端登录" className="rbac3-login-card">
        <Typography.Paragraph type="secondary">登录只验证用户身份；角色在登录后按当前 Session 单独激活。</Typography.Paragraph>
        {error !== null && <Alert type="error" showIcon message="登录失败" description={error instanceof Error ? error.message : 'AUTHENTICATION_FAILED'} />}
        <Form<LoginForm> form={form} layout="vertical" onFinish={(values) => void submit(values)}>
          <Form.Item name="tenantCode" label="Tenant Code" rules={[{ required: true }]}><Input aria-label="Tenant Code" autoComplete="organization" /></Form.Item>
          <Form.Item name="username" label="Username" rules={[{ required: true }]}><Input aria-label="Username" autoComplete="username" /></Form.Item>
          <Form.Item name="password" label="Password" rules={[{ required: true }]}><Input.Password aria-label="Password" autoComplete="current-password" /></Form.Item>
          <Button type="primary" htmlType="submit" block loading={submitting}>登录</Button>
        </Form>
      </Card>
    </main>
  )
}
