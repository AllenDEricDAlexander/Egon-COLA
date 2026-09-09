import { describe, expect, it } from 'vitest'
import { hasCapability } from './capabilities'

describe('server capabilities', () => {
  it('does not grant a capability that the session did not return', () => {
    expect(hasCapability(
      new Set(['yuheng:read']),
      'yuheng:groups:write',
    )).toBe(false)
  })

  it('honors the backend wildcard capability', () => {
    expect(hasCapability(
      new Set(['*']),
      'yuheng:credentials:write',
    )).toBe(true)
  })
})
