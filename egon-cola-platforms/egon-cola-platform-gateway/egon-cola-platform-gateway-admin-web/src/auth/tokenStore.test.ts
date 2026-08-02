import { afterEach, describe, expect, it } from 'vitest'
import { tokenStore } from './tokenStore'

afterEach(() => {
  tokenStore.clear()
  localStorage.clear()
  sessionStorage.clear()
})

describe('admin token store', () => {
  it('keeps the access token in memory only', () => {
    tokenStore.set({ accessToken: 'access' })

    expect(tokenStore.get()?.accessToken).toBe('access')
    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
  })

  it('clears all in-memory token material on logout', () => {
    tokenStore.set({ accessToken: 'access', nonce: 'nonce' })

    tokenStore.clear()

    expect(tokenStore.get()).toBeUndefined()
  })
})
