import { Alert, Spin } from 'antd'
import { useEffect, useState, type PropsWithChildren } from 'react'
import { rbac3OAuth } from './oauthClient'

export const UnifiedOAuthGate = ({ children }: PropsWithChildren) => {
  const callback = window.location.pathname === '/oauth/callback'
  const [ready, setReady] = useState(!callback)
  const [error, setError] = useState<string>()

  useEffect(() => {
    if (!callback) return
    let active = true
    void rbac3OAuth.handleCallback(window.location.search)
      .then((returnTo) => {
        if (!active) return
        window.history.replaceState({}, '', returnTo)
        setReady(true)
      })
      .catch((failure) => {
        if (!active) return
        setError(failure instanceof Error ? failure.message : '统一身份登录失败')
      })
    return () => { active = false }
  }, [callback])

  if (error) {
    return <main className="rbac3-centered"><Alert type="error" showIcon message={error} /></main>
  }
  if (!ready) {
    return <main className="rbac3-centered"><Spin size="large" description="完成统一身份登录" /></main>
  }
  return children
}
