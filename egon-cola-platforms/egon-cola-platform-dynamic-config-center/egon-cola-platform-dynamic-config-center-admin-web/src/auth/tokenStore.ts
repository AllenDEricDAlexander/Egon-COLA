export const TOKEN_KEY = 'egon.ddc.admin.token'

// 登录验证前先进入内存态（旧 webui 行为：token 验证成功后才持久化）
let currentToken = ''

export const getStoredToken = (): string =>
  currentToken || (sessionStorage.getItem(TOKEN_KEY) ?? '')

export const setSessionToken = (token: string): void => {
  currentToken = token
}

export const saveToken = (token: string): void => {
  currentToken = token
  sessionStorage.setItem(TOKEN_KEY, token)
}

export const clearToken = (): void => {
  currentToken = ''
  sessionStorage.removeItem(TOKEN_KEY)
}
