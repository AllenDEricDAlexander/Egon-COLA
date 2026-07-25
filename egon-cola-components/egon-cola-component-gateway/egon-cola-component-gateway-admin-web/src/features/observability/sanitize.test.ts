import { describe, expect, it } from 'vitest'
import { sanitizeForDisplay } from './sanitize'

describe('sensitive observability rendering', () => {
  it('removes body, credentials, cookie, token, and authorization recursively', () => {
    expect(
      sanitizeForDisplay({
        routeId: 'route-1',
        requestBody: 'sensitive',
        nested: {
          Authorization: 'Bearer secret',
          cookie: 'sid=secret',
          token: 'secret',
          durationMs: 12,
        },
      }),
    ).toEqual({
      routeId: 'route-1',
      nested: { durationMs: 12 },
    })
  })
})
