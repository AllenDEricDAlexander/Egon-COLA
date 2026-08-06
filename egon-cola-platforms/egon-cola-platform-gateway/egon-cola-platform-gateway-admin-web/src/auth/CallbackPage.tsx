import { Button, Result, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { oauthClient, useAuth } from './AuthContext'

export const CallbackPage = () => {
  const auth = useAuth()
  const navigate = useNavigate()
  const [returnTo, setReturnTo] = useState<string>()
  const [callbackError, setCallbackError] = useState<string>()

  useEffect(() => {
    let active = true
    void oauthClient.handleCallback(window.location.search)
      .then((target) => {
        if (active) setReturnTo(target)
      })
      .catch((failure) => {
        if (active) {
          setCallbackError(failure instanceof Error ? failure.message : '统一身份登录失败')
        }
      })
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (returnTo && auth.session && !auth.loading) {
      navigate(returnTo, { replace: true })
    }
  }, [auth.loading, auth.session, navigate, returnTo])

  const error = callbackError ?? auth.error
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

  return <Spin fullscreen description={auth.session && returnTo ? '登录成功，跳转中' : '完成统一身份登录'} />
}
