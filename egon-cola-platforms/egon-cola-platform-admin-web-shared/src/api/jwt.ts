const textDecoder = new TextDecoder('utf-8')

export const decodeTokenPayload = (token: string): Record<string, unknown> => {
  const parts = token.split('.')
  if (parts.length !== 3) {
    throw new Error('Invalid JWT format: expected 3 parts')
  }
  const payload = parts[1]!
  const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
  try {
    const binary = Uint8Array.from(atob(normalized), (c) => c.charCodeAt(0))
    const json = textDecoder.decode(binary)
    return JSON.parse(json) as Record<string, unknown>
  } catch (cause) {
    throw new Error('Failed to decode JWT payload', { cause })
  }
}

export const computeExpiresAt = (token: string): Date | null => {
  const claims = decodeTokenPayload(token)
  const exp = claims.exp
  if (exp === undefined) return null
  const seconds = typeof exp === 'string' ? Number(exp) : (exp as number)
  if (!Number.isFinite(seconds) || seconds <= 0) return null
  return new Date(seconds * 1000)
}

export const isTokenExpired = (token: string): boolean => {
  const expiresAt = computeExpiresAt(token)
  if (expiresAt === null) return false
  return Date.now() > expiresAt.getTime()
}
