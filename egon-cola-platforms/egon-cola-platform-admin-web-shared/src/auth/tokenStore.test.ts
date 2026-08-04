import { describe, expect, it, vi } from 'vitest'
import { createTokenStore } from './tokenStore'

describe('createTokenStore', () => {
  it('returns null when empty', () => {
    const store = createTokenStore()
    expect(store.get()).toBeNull()
  })

  it('stores and retrieves tokens', () => {
    const store = createTokenStore()
    store.set({ accessToken: 't', nonce: 'n', expiresAt: new Date().toISOString() })
    expect(store.get()?.accessToken).toBe('t')
  })

  it('clears tokens', () => {
    const store = createTokenStore()
    store.set({ accessToken: 't' })
    store.clear()
    expect(store.get()).toBeNull()
  })

  it('notifies subscribers on set', () => {
    const store = createTokenStore()
    const fn = vi.fn()
    store.subscribe(fn)
    store.set({ accessToken: 'x' })
    expect(fn).toHaveBeenCalledWith({ accessToken: 'x', nonce: undefined, expiresAt: undefined })
  })

  it('notifies subscribers on clear', () => {
    const store = createTokenStore()
    store.set({ accessToken: 'x' })
    const fn = vi.fn()
    store.subscribe(fn)
    store.clear()
    expect(fn).toHaveBeenCalledWith(null)
  })

  it('unsubscribe stops notifications', () => {
    const store = createTokenStore()
    const fn = vi.fn()
    const unsub = store.subscribe(fn)
    unsub()
    store.set({ accessToken: 'x' })
    expect(fn).not.toHaveBeenCalled()
  })

  it('extracts nonce from access token on set', () => {
    const store = createTokenStore()
    const header = btoa(JSON.stringify({ alg: 'RS256' }))
    const body = btoa(JSON.stringify({ nonce: 'test-nonce', sub: 'u1' }))
    store.set({ accessToken: `${header}.${body}.sig` })
    expect(store.get()?.nonce).toBe('test-nonce')
  })
})
