import { describe, expect, it } from 'vitest'
import { canonicalOAuthPageUrl } from './oauthOrigin'

describe('Gateway OAuth origin', () => {
  it('moves localhost pages to the configured callback origin', () => {
    expect(canonicalOAuthPageUrl(
      'http://localhost:18141/login?from=%2Fdashboard#form',
      'http://127.0.0.1:18141/oauth/callback',
    )).toBe('http://127.0.0.1:18141/login?from=%2Fdashboard#form')
  })

  it('keeps pages already using the callback origin', () => {
    expect(canonicalOAuthPageUrl(
      'http://127.0.0.1:18141/login',
      'http://127.0.0.1:18141/oauth/callback',
    )).toBeUndefined()
  })
})
