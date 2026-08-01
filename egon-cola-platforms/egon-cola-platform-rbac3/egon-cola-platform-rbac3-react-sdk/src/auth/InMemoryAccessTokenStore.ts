export type AccessTokenListener = () => void

/** Keeps the short-lived Bearer credential only in the current JS process. */
export class InMemoryAccessTokenStore {
  private accessToken: string | null
  private readonly listeners = new Set<AccessTokenListener>()

  constructor(initialAccessToken: string | null = null) {
    this.accessToken = normalize(initialAccessToken)
  }

  get(): string | null {
    return this.accessToken
  }

  set(accessToken: string): void {
    const normalized = normalize(accessToken)
    if (normalized === null) {
      throw new Error('access token must not be blank')
    }
    if (normalized === this.accessToken) {
      return
    }
    this.accessToken = normalized
    this.notify()
  }

  clear(): void {
    if (this.accessToken === null) {
      return
    }
    this.accessToken = null
    this.notify()
  }

  subscribe(listener: AccessTokenListener): () => void {
    this.listeners.add(listener)
    return () => this.listeners.delete(listener)
  }

  private notify(): void {
    this.listeners.forEach((listener) => listener())
  }
}

const normalize = (value: string | null): string | null => {
  const normalized = value?.trim()
  return normalized ? normalized : null
}
