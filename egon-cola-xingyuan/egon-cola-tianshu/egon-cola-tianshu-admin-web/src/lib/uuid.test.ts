import { describe, expect, it } from 'vitest'
import { uuidV7 } from './uuid'

describe('uuidV7', () => {
  it('encodes the timestamp and RFC variant as UUID version 7', () => {
    const value = uuidV7(
      0x0123456789ab,
      Uint8Array.from([0, 1, 2, 3, 4, 5, 6, 7, 8, 9]),
    )
    expect(value).toBe('01234567-89ab-7001-8203-040506070809')
  })
})
