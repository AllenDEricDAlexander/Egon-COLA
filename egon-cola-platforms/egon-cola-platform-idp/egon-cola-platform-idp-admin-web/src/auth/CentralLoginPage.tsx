import { Alert, Button, Card, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { idpOAuth } from './oauthClient'

interface LoginForm {
  readonly tenantId: string
  readonly username: string
  readonly password: string
}

interface LoginResponse {
  readonly identitySub: string
  readonly displayName: string
  readonly mustChangePassword: boolean
}

const issuer = (import.meta.env.VITE_IDP_ISSUER ?? 'http://127.0.0.1:18120')
  .replace(/\/$/, '')

export const safeAuthorizationReturnTo = (search: string): string | undefined => {
  const value = new URLSearchParams(search).get('return_to')
  if (!value) return undefined
  try {
    const uri = new URL(value)
    return uri.origin === new URL(issuer).origin
      && uri.pathname === '/oauth2/authorize'
      ? uri.toString()
      : undefined
  } catch {
    return undefined
  }
}

export const establishSso = async (
  username: string,
  password: string,
  fetcher: typeof globalThis.fetch = globalThis.fetch.bind(globalThis),
): Promise<LoginResponse> => {
  const csrfResponse = await fetcher(`${issuer}/oauth2/login/csrf`, {
    credentials: 'include',
    headers: { 'Accept': 'application/json' },
  })
  if (!csrfResponse.ok) throw new Error('无法建立安全登录事务')
  const csrf = await csrfResponse.json() as { token?: string }
  if (!csrf.token) throw new Error('登录 CSRF 响应无效')
  const response = await fetcher(`${issuer}/oauth2/login`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      'X-IDP-CSRF': csrf.token,
    },
    body: JSON.stringify({ username, password }),
  })
  if (!response.ok) throw new Error('用户名或密码错误')
  return await response.json() as LoginResponse
}

export const CentralLoginPage = () => {
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string>()
  const returnTo = safeAuthorizationReturnTo(window.location.search)
  const submit = async (values: LoginForm) => {
    setSubmitting(true)
    setError(undefined)
    try {
      await establishSso(values.username.trim(), values.password)
      if (returnTo) {
        window.location.assign(returnTo)
      } else {
        await idpOAuth.beginAuthorization(values.tenantId, '/')
      }
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : '登录失败')
    } finally {
      setSubmitting(false)
    }
  }
  return (
    <main className="login-page">
      <Card className="login-card">
        <Typography.Title level={2}>Egon 统一身份平台</Typography.Title>
        <Typography.Paragraph type="secondary">
          一次登录，访问已接入的 Gateway、DDC、RBAC3 与 IdP 管理系统。
        </Typography.Paragraph>
        {error && <Alert type="error" showIcon message={error} />}
        <Form<LoginForm>
          layout="vertical"
          initialValues={{ tenantId: import.meta.env.VITE_DEFAULT_TENANT_ID ?? 'default' }}
          onFinish={(values) => void submit(values)}
        >
          {!returnTo && (
            <Form.Item name="tenantId" label="租户 ID" rules={[{ required: true }]}>
              <Input autoComplete="organization" />
            </Form.Item>
          )}
          <Form.Item name="username" label="用户名" rules={[{ required: true }]}>
            <Input autoComplete="username" />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true }]}>
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          <Button block type="primary" htmlType="submit" loading={submitting}>
            登录
          </Button>
        </Form>
      </Card>
    </main>
  )
}
