import { describe, expect, it } from 'vitest'
import { computeExpiresAt, decodeTokenPayload, isTokenExpired } from './jwt'

const textEncoder = new TextEncoder()

const toBase64Url = (value: string): string => {
  const bytes = textEncoder.encode(value)
  let binary = ''
  bytes.forEach((b) => { binary += String.fromCharCode(b) })
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

const makeToken = (payload: Record<string, unknown>): string => {
  const header = toBase64Url(JSON.stringify({ alg: 'RS256' }))
  const body = toBase64Url(JSON.stringify(payload))
  return `${header}.${body}.signature`
}

describe('decodeTokenPayload', () => {
  it('decodes standard JWT payload', () => {
    const token = makeToken({ sub: 'user-1', exp: 2000000000 })
    const result = decodeTokenPayload(token)
    expect(result.sub).toBe('user-1')
  })

  it('decodes non-ASCII (UTF-8) claims without corruption', () => {
    const token = makeToken({ name: '张三', displayName: '管理员' })
    const result = decodeTokenPayload(token)
    expect(result.name).toBe('张三')
    expect(result.displayName).toBe('管理员')
  })

  it('throws on malformed token', () => {
    expect(() => decodeTokenPayload('not.a.token')).toThrow()
  })

  it('throws on empty token', () => {
    expect(() => decodeTokenPayload('')).toThrow()
  })
})

describe('computeExpiresAt', () => {
  it('returns Date from numeric exp', () => {
    const token = makeToken({ exp: 2000000000 })
    const result = computeExpiresAt(token)
    expect(result).toBeInstanceOf(Date)
    expect(result!.getTime()).toBe(2000000000 * 1000)
  })

  it('returns Date from string exp', () => {
    const token = makeToken({ exp: '2000000000' })
    const result = computeExpiresAt(token)
    expect(result!.getTime()).toBe(2000000000 * 1000)
  })

  it('returns null when no exp claim', () => {
    const token = makeToken({ sub: 'x' })
    expect(computeExpiresAt(token)).toBeNull()
  })
})

describe('isTokenExpired', () => {
  it('returns false for future token', () => {
    const future = Math.floor(Date.now() / 1000) + 3600
    expect(isTokenExpired(makeToken({ exp: future }))).toBe(false)
  })

  it('returns true for past token', () => {
    const past = Math.floor(Date.now() / 1000) - 3600
    expect(isTokenExpired(makeToken({ exp: past }))).toBe(true)
  })

  it('returns false when no exp claim', () => {
    expect(isTokenExpired(makeToken({ sub: 'x' }))).toBe(false)
  })
})
