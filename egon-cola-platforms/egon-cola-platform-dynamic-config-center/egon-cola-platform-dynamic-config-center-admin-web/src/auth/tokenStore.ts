export const TOKEN_KEY = 'egon.ddc.admin.token'

sessionStorage.removeItem(TOKEN_KEY)
localStorage.removeItem(TOKEN_KEY)

let currentToken = ''
const listeners = new Set<() => void>()

export const getStoredToken = (): string => currentToken

export const saveToken = (token: string): void => {
  const normalized = token.trim()
  if (!normalized) throw new Error('access token must not be blank')
  currentToken = normalized
  listeners.forEach((listener) => listener())
}

export const clearToken = (): void => {
  currentToken = ''
  listeners.forEach((listener) => listener())
}

export const subscribeToken = (listener: () => void): (() => void) => {
  listeners.add(listener)
  return () => { listeners.delete(listener) }
}
