export const TOKEN_KEY = 'egon.ddc.admin.token'

export const getStoredToken = (): string => sessionStorage.getItem(TOKEN_KEY) ?? ''

export const saveToken = (token: string): void => {
  sessionStorage.setItem(TOKEN_KEY, token)
}

export const clearToken = (): void => {
  sessionStorage.removeItem(TOKEN_KEY)
}
