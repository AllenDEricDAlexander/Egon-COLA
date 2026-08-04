import { Button, Result, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from './AuthContext'

export const CallbackPage = () => {
  const auth = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [error, setError] = useState<string>()

  useEffect(() => {
    let active = true
    if (auth.bootstrap && !auth.loading) {
      navigate('/overview', { replace: true })
      return
    }
    if (!auth.loading && !auth.bootstrap) {
      const errorParam = searchParams.get('error_description') ?? searchParams.get('error')
      if (active) setError(errorParam ?? '统一身份登录失败，请重试')
    }
    return () => { active = false }
  }, [auth.bootstrap, auth.loading, navigate, searchParams])

  if (auth.loading || auth.bootstrap) {
    return <Spin fullscreen description={auth.bootstrap ? '登录成功，跳转中' : '完成统一身份登录'} />
  }

  if (error) {
    return (
      <Result
        status="error"
        title="登录失败"
        subTitle={error}
        extra={<Button type="primary" onClick={() => navigate('/login', { replace: true })}>返回登录</Button>}
      />
    )
  }

  return <Spin fullscreen description="处理中" />
}
