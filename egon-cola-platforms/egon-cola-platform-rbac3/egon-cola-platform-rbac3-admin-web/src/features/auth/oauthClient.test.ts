import { afterEach, describe, expect, it, vi } from 'vitest'
import { createBrowserOAuthClient } from './oauthClient'
import { rbac3AccessTokenStore } from './oauthClient'

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
  clientId: 'rbac3-admin-web',
  resource: 'https://api.egon.internal/local/permission/rbac3',
  redirectUri: 'https://gateway.example.test/oauth/callback',
}

afterEach(() => {
  rbac3AccessTokenStore.clear()
  sessionStorage.clear()
})

describe('OAuth PKCE client', () => {
  it('sends the configured resource during authorization code exchange', async () => {
    const navigate = vi.fn()
    const fetcher = vi.fn<typeof fetch>()
    const client = createBrowserOAuthClient(configuration, runtime(fetcher, navigate))
    await client.beginAuthorization('tenant-a', '/dashboard')
    const authorizationUrl = new URL(navigate.mock.calls[0][0])
    const state = authorizationUrl.searchParams.get('state')!
    const nonce = authorizationUrl.searchParams.get('nonce')!
    fetcher.mockResolvedValueOnce(new Response(JSON.stringify({
      access_token: jwt({ nonce }),
      token_type: 'Bearer',
      expires_in: 900,
    }), { status: 200, headers: { 'Content-Type': 'application/json' } }))

    await client.handleCallback(`?code=one-time&state=${state}`)

    const request = fetcher.mock.calls[0][1] as RequestInit
    const form = new URLSearchParams(String(request.body))
    expect(form.get('resource')).toBe(
      'https://api.egon.internal/local/permission/rbac3',
    )
  })

  it('uses S256, validates state and nonce, then removes the transaction', async () => {
    const navigate = vi.fn()
    const fetcher = vi.fn<typeof fetch>()
    const client = createBrowserOAuthClient(configuration, runtime(fetcher, navigate))

    await client.beginAuthorization('tenant-a', '/dashboard')

    const authorizationUrl = new URL(navigate.mock.calls[0][0])
    expect(authorizationUrl.searchParams.get('code_challenge_method')).toBe('S256')
    expect(authorizationUrl.searchParams.get('tenant_id')).toBe('tenant-a')
    expect(authorizationUrl.searchParams.get('resource')).toBe(
      'https://api.egon.internal/local/permission/rbac3',
    )
    expect(authorizationUrl.searchParams.get('audience')).toBeNull()
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
    expect(rbac3AccessTokenStore.get()).toContain('.')
    expect(sessionStorage.length).toBe(0)
    const request = fetcher.mock.calls[0][1] as RequestInit
    expect(request.credentials).toBe('include')
    expect(String(request.body)).toContain('code_verifier=')
  })

  it('deduplicates concurrent callback handling from StrictMode effects', async () => {
    let release: ((value: Response) => void) | undefined
    const navigate = vi.fn()
    const fetcher = vi.fn<typeof fetch>(() => new Promise<Response>(
      (resolve) => { release = resolve },
    ))
    const client = createBrowserOAuthClient(configuration, runtime(fetcher, navigate))
    await client.beginAuthorization('tenant-a', '/dashboard')
    const authorizationUrl = new URL(navigate.mock.calls[0][0])
    const state = authorizationUrl.searchParams.get('state')!
    const nonce = authorizationUrl.searchParams.get('nonce')!

    const callbacks = Promise.all([
      client.handleCallback(`?code=one-time&state=${state}`),
      client.handleCallback(`?code=one-time&state=${state}`),
    ])

    expect(fetcher).toHaveBeenCalledTimes(1)
    release!(new Response(JSON.stringify({
      access_token: jwt({ nonce }),
      token_type: 'Bearer',
      expires_in: 900,
    }), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    await expect(callbacks).resolves.toEqual(['/dashboard', '/dashboard'])
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

  it('deduplicates concurrent cookie refresh with resource and no refresh token body', async () => {
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
    const form = new URLSearchParams(String(request.body))
    expect(form.get('grant_type')).toBe('refresh_token')
    expect(form.get('client_id')).toBe('rbac3-admin-web')
    expect(form.get('resource')).toBe(
      'https://api.egon.internal/local/permission/rbac3',
    )
    expect(form.has('refresh_token')).toBe(false)
  })
})
