import type { ReactNode } from 'react'
import { useAuth } from './AuthContext'
import LoginPage from './LoginPage'

export function RequireAuth({ children }: { children: ReactNode }) {
  const { token } = useAuth()
  if (token === '') return <LoginPage />
  return <>{children}</>
}
