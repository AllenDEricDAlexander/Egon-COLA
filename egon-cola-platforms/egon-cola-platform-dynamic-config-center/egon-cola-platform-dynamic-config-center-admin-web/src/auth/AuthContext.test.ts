import { describe, expect, it } from 'vitest'
import { identityFromToken } from './AuthContext'

const jwt = (payload: Record<string, unknown>): string => [
  btoa(JSON.stringify({ alg: 'none', typ: 'JWT' })),
  btoa(JSON.stringify(payload)),
  'signature',
].join('.')

describe('identityFromToken', () => {
  it('prefers displayName from the existing access token', () => {
    expect(identityFromToken(jwt({
      displayName: 'Mario',
      name: 'Fallback Name',
      sub: 'user-1',
    }))).toBe('Mario')
  })

  it('returns an empty identity for a malformed token', () => {
    expect(identityFromToken('not.a.token')).toBe('')
  })
})
