import { idpOAuth } from '../auth/oauthClient'
import { tokenStore } from '../auth/tokenStore'

const baseUrl = import.meta.env.VITE_IDP_API_BASE_URL ?? ''

export const idpApi = async <T,>(path: string, init: RequestInit = {}): Promise<T> => {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const accessToken = tokenStore.get()?.accessToken
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
  let response = await fetch(`${baseUrl}${path}`, { ...init, headers })
  if (response.status === 401 && accessToken) {
    headers.set('Authorization', `Bearer ${await idpOAuth.refresh()}`)
    response = await fetch(`${baseUrl}${path}`, { ...init, headers })
  }
  if (!response.ok) {
    const body = await response.json().catch(() => ({})) as { message?: string }
    throw new Error(body.message ?? `统一身份平台请求失败 (${response.status})`)
  }
  if (response.status === 204) return undefined as T
  return await response.json() as T
}
