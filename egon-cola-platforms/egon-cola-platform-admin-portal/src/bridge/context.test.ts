import { describe, expect, it } from 'vitest'
import { buildChildProps, buildRouteEvent, sanitizeChildContext } from './context'

describe('child context bridge', () => {
  it('removes credentials and raw request material from child props', () => {
    const value = sanitizeChildContext({
      platformKey: 'gateway',
      routeIntent: '/dashboard',
      accessToken: 'x',
      cookie: 'y',
      headers: { Authorization: 'z' },
      body: { secret: 'q' },
    })

    expect(value).toEqual({ platformKey: 'gateway', routeIntent: '/dashboard' })
    expect(JSON.stringify(value)).not.toMatch(/accessToken|cookie|authorization|secret|body/i)
  })

  it('keeps only safe capability values and emits the same allow-list for route events', () => {
    const source = {
      platformKey: 'ddc',
      routeIntent: '/instances',
      scopeDisplay: 'prod',
      capabilitySummary: { canRead: true, canWrite: false, clientSecret: 'hidden' },
      hostVersion: '5.3.2',
      rawBody: { password: 'hidden' },
    }

    expect(buildChildProps(source)).toEqual({
      platformKey: 'ddc',
      routeIntent: '/instances',
      scopeDisplay: 'prod',
      capabilitySummary: { canRead: true, canWrite: false },
      hostVersion: '5.3.2',
    })
    expect(buildRouteEvent(source)).toEqual({
      platformKey: 'ddc',
      routeIntent: '/instances',
      scopeDisplay: 'prod',
      capabilitySummary: { canRead: true, canWrite: false },
      hostVersion: '5.3.2',
    })
  })

  it('rejects an unknown platform key instead of forwarding an untrusted value', () => {
    expect(() => sanitizeChildContext({ platformKey: 'unknown' })).toThrow(/platform key/i)
  })
})
