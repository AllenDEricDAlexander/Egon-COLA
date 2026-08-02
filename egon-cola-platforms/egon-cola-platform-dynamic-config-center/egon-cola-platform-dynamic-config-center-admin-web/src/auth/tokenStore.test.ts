import { afterEach, describe, expect, it } from 'vitest'
import { clearToken, getStoredToken, saveToken } from './tokenStore'

describe('tokenStore', () => {
  afterEach(() => {
    clearToken()
    sessionStorage.clear()
    localStorage.clear()
  })

  it('keeps the access token in memory only', () => {
    saveToken('token-abc')

    expect(getStoredToken()).toBe('token-abc')
    expect(sessionStorage.length).toBe(0)
    expect(localStorage.length).toBe(0)
  })

  it('clears the in-memory token', () => {
    saveToken('token-abc')
    clearToken()
    expect(getStoredToken()).toBe('')
  })
})
