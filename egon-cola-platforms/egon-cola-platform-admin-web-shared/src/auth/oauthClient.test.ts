import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createOAuthClient, type OAuthRuntime } from './oauthClient'
import { createTokenStore } from './tokenStore'

const accessToken = (nonce: string): string => {
  const payload = btoa(JSON.stringify({ nonce }))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
  return `e30.${payload}.signature`
}

const createClient = () => {
  const navigate = vi.fn()
  const fetch = vi.fn<typeof globalThis.fetch>()
  const runtime: OAuthRuntime = {
    fetch,
    storage: window.sessionStorage,
    randomValues: (target) => target.fill(7),
    digest: async () => new Uint8Array(32).buffer,
    navigate,
    now: () => 1_000,
  }
  const client = createOAuthClient({
    issuer: 'http://127.0.0.1:18120',
    clientId: 'gateway-admin-web',
    audience: 'gateway-admin-web',
    redirectUri: 'http://127.0.0.1:18141/oauth/callback',
    tokenStore: createTokenStore(),
  }, runtime)
  return { client, fetch, navigate }
}

beforeEach(() => window.sessionStorage.clear())

describe('OAuth callback transaction', () => {
  it('keeps the transaction when token exchange fails', async () => {
    const { client, fetch, navigate } = createClient()
    await client.beginAuthorization('default', '/dashboard')
    const authorizationUrl = new URL(navigate.mock.calls[0]![0] as string)
    fetch.mockRejectedValueOnce(new Error('network unavailable'))

    await expect(client.handleCallback(`?code=code-1&state=${authorizationUrl.searchParams.get('state')}`))
      .rejects.toThrow('network unavailable')

    expect(window.sessionStorage.length).toBe(1)
  })

  it('accepts a new transaction after a missing-transaction failure', async () => {
    const { client, fetch, navigate } = createClient()
    await expect(client.handleCallback('?code=stale')).rejects
      .toThrow('OAuth transaction not found or expired')

    await client.beginAuthorization('default', '/dashboard')
    const authorizationUrl = new URL(navigate.mock.calls[0]![0] as string)
    const transaction = JSON.parse(window.sessionStorage.getItem(
      window.sessionStorage.key(0)!,
    )!) as { nonce: string }
    fetch.mockResolvedValueOnce(new Response(JSON.stringify({
      access_token: accessToken(transaction.nonce),
      token_type: 'Bearer',
    }), { status: 200, headers: { 'Content-Type': 'application/json' } }))

    await expect(client.handleCallback(
      `?code=code-2&state=${authorizationUrl.searchParams.get('state')}`,
    )).resolves.toBe('/dashboard')
    expect(window.sessionStorage.length).toBe(0)
  })
})
