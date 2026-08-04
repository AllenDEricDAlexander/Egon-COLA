import { Button, Result, Spin } from 'antd'
import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { oauthClient } from './AuthContext'
import { useAuth } from './AuthContext'

export const CallbackPage = () => {
  const auth = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [error, setError] = useState<string>()
  const handled = useRef(false)

  useEffect(() => {
    if (handled.current) return
    handled.current = true

    const process = async () => {
      try {
        const returnTo = await oauthClient.handleCallback(window.location.search)
        // handleCallback stores the token → AuthContext's tokenStore.subscribe picks it up
        // → AuthContext calls bootstrap → auth.bootstrap is set
        // Use replaceState to clean the URL
        window.history.replaceState({}, '', returnTo)
        window.dispatchEvent(new PopStateEvent('popstate'))
      } catch (e) {
        setError(e instanceof Error ? e.message : '统一身份登录失败，请重试')
      }
    }
    void process()
  }, [])

  useEffect(() => {
    if (auth.bootstrap && !auth.loading) {
      navigate('/overview', { replace: true })
    }
  }, [auth.bootstrap, auth.loading, navigate])

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

  return <Spin fullscreen description={auth.bootstrap ? '登录成功，跳转中' : '完成统一身份登录'} />
}
