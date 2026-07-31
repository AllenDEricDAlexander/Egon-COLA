import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { clearToken, getStoredToken, saveToken } from './tokenStore'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../api/client'

type AuthContextValue = {
  token: string
  setToken: (token: string) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setTokenState] = useState<string>(() => getStoredToken())

  const setToken = useCallback((next: string) => {
    saveToken(next)
    setTokenState(next)
  }, [])

  const logout = useCallback(() => {
    clearToken()
    setTokenState('')
  }, [])

  useEffect(() => {
    setDdcTokenProvider(getStoredToken)
    setDdcUnauthorizedHandler(logout)
  }, [logout])

  const value = useMemo(() => ({ token, setToken, logout }), [token, setToken, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = (): AuthContextValue => {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth must be used within AuthProvider')
  return value
}
