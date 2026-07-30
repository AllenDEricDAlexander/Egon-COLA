import { describe, expect, it } from 'vitest'
import { hasCapability } from './capabilities'

describe('server capabilities', () => {
  it('does not grant a capability that the session did not return', () => {
    expect(hasCapability(
      new Set(['gateway:read']),
      'gateway:groups:write',
    )).toBe(false)
  })

  it('honors the backend wildcard capability', () => {
    expect(hasCapability(
      new Set(['*']),
      'gateway:credentials:write',
    )).toBe(true)
  })
})
