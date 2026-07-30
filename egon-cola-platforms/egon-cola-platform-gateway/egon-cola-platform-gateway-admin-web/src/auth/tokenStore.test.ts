import { afterEach, describe, expect, it } from 'vitest'
import { tokenStore } from './tokenStore'

afterEach(() => {
  tokenStore.clear()
})

describe('admin token store', () => {
  it('persists only in the requested browser storage', () => {
    tokenStore.set({
      accessToken: 'access',
      refreshToken: 'refresh',
    }, true)

    expect(localStorage.getItem('egon.gateway.admin.auth')).toContain('access')
    expect(sessionStorage.getItem('egon.gateway.admin.auth.session')).toBeNull()
  })

  it('clears all token material on logout', () => {
    tokenStore.set({ accessToken: 'access' }, false)

    tokenStore.clear()

    expect(tokenStore.get()).toBeUndefined()
    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
  })
})
