import assert from 'node:assert/strict'
import test from 'node:test'

import { uuidV7 } from '../../../main/resources/static/ddc-admin/uuid.mjs'

test('uuidV7 encodes the timestamp and RFC variant as UUID version 7', () => {
  const value = uuidV7(
    0x0123456789ab,
    Uint8Array.from([0, 1, 2, 3, 4, 5, 6, 7, 8, 9]),
  )

  assert.equal(value, '01234567-89ab-7001-8203-040506070809')
})
