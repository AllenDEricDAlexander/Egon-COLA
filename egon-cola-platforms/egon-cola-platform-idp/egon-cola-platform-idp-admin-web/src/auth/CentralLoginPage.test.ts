import { describe, expect, it, vi } from 'vitest'
import { establishSso, safeAuthorizationReturnTo } from './CentralLoginPage'

describe('central login transport', () => {
  it('accepts only the configured issuer authorization endpoint as return target', () => {
    const safe = new URLSearchParams({
      return_to: 'http://127.0.0.1:18120/oauth2/authorize?client_id=gateway-admin-web',
    })
    const unsafe = new URLSearchParams({
      return_to: 'https://attacker.example.test/oauth2/authorize',
    })

    expect(safeAuthorizationReturnTo(`?${safe}`)).toContain('/oauth2/authorize')
    expect(safeAuthorizationReturnTo(`?${unsafe}`)).toBeUndefined()
  })

  it('uses a fresh double-submit token and credentialed requests', async () => {
    const fetcher = vi.fn<typeof fetch>()
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: 'csrf-value' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }))
      .mockResolvedValueOnce(new Response(JSON.stringify({
        identitySub: 'alice-sub',
        displayName: 'Alice',
        mustChangePassword: false,
      }), { status: 200, headers: { 'Content-Type': 'application/json' } }))

    await expect(establishSso('alice', 'secret', fetcher))
      .resolves.toMatchObject({ identitySub: 'alice-sub' })

    expect((fetcher.mock.calls[0][1] as RequestInit).credentials).toBe('include')
    const login = fetcher.mock.calls[1][1] as RequestInit
    expect(login.credentials).toBe('include')
    expect((login.headers as Record<string, string>)['X-IDP-CSRF']).toBe('csrf-value')
    expect(login.body).toBe(JSON.stringify({ username: 'alice', password: 'secret' }))
  })
})
