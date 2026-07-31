import { afterEach, describe, expect, it } from 'vitest'
import { TOKEN_KEY, clearToken, getStoredToken, saveToken, setSessionToken } from './tokenStore'

describe('tokenStore', () => {
  afterEach(() => {
    sessionStorage.clear()
    clearToken()
  })

  it('persists and reads the token from sessionStorage', () => {
    expect(getStoredToken()).toBe('')
    saveToken('token-abc')
    expect(sessionStorage.getItem(TOKEN_KEY)).toBe('token-abc')
    expect(getStoredToken()).toBe('token-abc')
  })

  it('clears the token', () => {
    saveToken('token-abc')
    clearToken()
    expect(getStoredToken()).toBe('')
    expect(sessionStorage.getItem(TOKEN_KEY)).toBeNull()
  })

  it('setSessionToken exposes the candidate in memory without persisting', () => {
    setSessionToken('candidate-token')
    expect(getStoredToken()).toBe('candidate-token')
    expect(sessionStorage.getItem(TOKEN_KEY)).toBeNull()
    clearToken()
    expect(getStoredToken()).toBe('')
  })
})
