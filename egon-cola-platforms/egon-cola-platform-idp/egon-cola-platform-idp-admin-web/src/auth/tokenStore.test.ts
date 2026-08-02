import { afterEach, describe, expect, it } from 'vitest'
import { tokenStore } from './tokenStore'

afterEach(() => {
  tokenStore.clear()
  localStorage.clear()
  sessionStorage.clear()
})

describe('IdP access token store', () => {
  it('never persists access credentials', () => {
    tokenStore.set({ accessToken: 'access-value' })

    expect(tokenStore.get()?.accessToken).toBe('access-value')
    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
  })
})
