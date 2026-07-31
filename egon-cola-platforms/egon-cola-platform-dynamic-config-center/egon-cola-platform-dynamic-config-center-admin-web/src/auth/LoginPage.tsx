import { useState } from 'react'
import { Button, Card, Input, Typography, message } from 'antd'
import { ddcApi } from '../api/client'
import { useAuth } from './AuthContext'

export default function LoginPage() {
  const { setToken } = useAuth()
  const [accessToken, setAccessToken] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async () => {
    setLoading(true)
    try {
      await ddcApi('/api/v1/ddc/apps')
      setToken(accessToken.trim())
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ maxWidth: 480, margin: '96px auto', padding: '0 16px' }}>
      <Card>
        <Typography.Title level={4}>连接本机 DDC 管理端</Typography.Title>
        <Typography.Paragraph type="secondary">
          Token 仅保存在当前浏览器会话，不会写入 URL 或服务端。
        </Typography.Paragraph>
        <Input.TextArea
          value={accessToken}
          onChange={(event) => setAccessToken(event.target.value)}
          rows={4}
          placeholder="粘贴 admin.token 内容"
          autoComplete="off"
        />
        <Button
          type="primary"
          block
          style={{ marginTop: 16 }}
          loading={loading}
          disabled={accessToken.trim() === ''}
          onClick={() => void submit()}
        >
          登录并加载
        </Button>
      </Card>
    </div>
  )
}
