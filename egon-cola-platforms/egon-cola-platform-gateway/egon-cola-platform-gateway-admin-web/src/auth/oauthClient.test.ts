import { afterEach, describe, expect, it, vi } from 'vitest'
import { createBrowserOAuthClient } from './oauthClient'
import { tokenStore } from './tokenStore'

const jwt = (claims: Record<string, unknown>) => {
  const encode = (value: unknown) => btoa(JSON.stringify(value))
    .replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
  return `${encode({ alg: 'RS256' })}.${encode(claims)}.signature`
}

const runtime = (fetcher: typeof fetch, navigate = vi.fn()) => ({
  fetch: fetcher,
  storage: sessionStorage,
  randomValues: (target: Uint8Array<ArrayBuffer>) => {
    target.fill(7)
    return target
  },
  digest: async () => new Uint8Array(32).fill(9).buffer,
  navigate,
  now: () => Date.parse('2026-08-02T00:00:00Z'),
})

const configuration = {
  issuer: 'https://idp.example.test',
  clientId: 'gateway-admin-web',
  audience: 'gateway-admin-web',
  redirectUri: 'https://gateway.example.test/oauth/callback',
}

afterEach(() => {
  tokenStore.clear()
  sessionStorage.clear()
})

describe('OAuth PKCE client', () => {
  it('uses S256, validates state and nonce, then removes the transaction', async () => {
    const navigate = vi.fn()
    const fetcher = vi.fn<typeof fetch>()
    const client = createBrowserOAuthClient(configuration, runtime(fetcher, navigate))

    await client.beginAuthorization('tenant-a', '/dashboard')

    const authorizationUrl = new URL(navigate.mock.calls[0][0])
    expect(authorizationUrl.searchParams.get('code_challenge_method')).toBe('S256')
    expect(authorizationUrl.searchParams.get('tenant_id')).toBe('tenant-a')
    expect(authorizationUrl.searchParams.get('code_verifier')).toBeNull()
    const state = authorizationUrl.searchParams.get('state')!
    const nonce = authorizationUrl.searchParams.get('nonce')!
    fetcher.mockResolvedValue(new Response(JSON.stringify({
      access_token: jwt({ nonce }),
      token_type: 'Bearer',
      expires_in: 900,
    }), { status: 200, headers: { 'Content-Type': 'application/json' } }))

    await expect(client.handleCallback(`?code=one-time&state=${state}`))
      .resolves.toBe('/dashboard')
    expect(tokenStore.get()?.accessToken).toContain('.')
    expect(sessionStorage.length).toBe(0)
    const request = fetcher.mock.calls[0][1] as RequestInit
    expect(request.credentials).toBe('include')
    expect(String(request.body)).toContain('code_verifier=')
  })

  it('rejects an authorization transaction older than ten minutes', async () => {
    const navigate = vi.fn()
    const fetcher = vi.fn<typeof fetch>()
    const client = createBrowserOAuthClient(configuration, runtime(fetcher, navigate))
    await client.beginAuthorization('tenant-a')
    const key = sessionStorage.key(0)!
    const transaction = JSON.parse(sessionStorage.getItem(key)!) as Record<string, unknown>
    transaction.createdAt = Date.parse('2026-08-01T23:49:59Z')
    sessionStorage.setItem(key, JSON.stringify(transaction))
    const state = new URL(navigate.mock.calls[0][0]).searchParams.get('state')

    await expect(client.handleCallback(`?code=expired&state=${state}`))
      .rejects.toThrow('统一身份登录事务不存在或已失效')
    expect(fetcher).not.toHaveBeenCalled()
    expect(sessionStorage.length).toBe(0)
  })

  it('deduplicates concurrent cookie refresh without sending a refresh token body', async () => {
    let release: ((value: Response) => void) | undefined
    const fetcher = vi.fn<typeof fetch>(() => new Promise<Response>(
      (resolve) => { release = resolve },
    ))
    const client = createBrowserOAuthClient(configuration, runtime(fetcher))

    const first = client.refresh()
    const second = client.refresh()
    expect(fetcher).toHaveBeenCalledTimes(1)
    release!(new Response(JSON.stringify({
      access_token: jwt({ nonce: 'n' }),
      token_type: 'Bearer',
      expires_in: 900,
    }), { status: 200, headers: { 'Content-Type': 'application/json' } }))

    await expect(Promise.all([first, second])).resolves.toHaveLength(2)
    const request = fetcher.mock.calls[0][1] as RequestInit
    expect(String(request.body)).toBe(
      'grant_type=refresh_token&client_id=gateway-admin-web',
    )
  })
})
