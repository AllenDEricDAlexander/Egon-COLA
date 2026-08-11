export interface AuthorizationBootstrap {
  readonly identitySub: string
  readonly tenantId: string
  readonly sessionId: string
  readonly rbac3UserId: string
  readonly systemCode: string
  readonly permissions: readonly string[]
  readonly activeRoleIds: readonly string[]
  readonly authVersion: number
  readonly contextVersion: number
  readonly policyVersion: number
  readonly generatedAt: string
  readonly expiresAt: string
}

export interface IdentityUser {
  readonly subject: string
  readonly username: string
  readonly displayName: string
  readonly status: string
  readonly tokenVersion: number
  readonly failedLoginCount: number
  readonly lockedUntil?: string
  readonly lastLoginAt?: string
  readonly version: number
}

export interface CreatedIdentityUser extends IdentityUser {
  readonly oneTimePassword: string
}

export interface OAuthClientView {
  readonly clientId: string
  readonly clientName: string
  readonly clientType: string
  readonly status: string
  readonly pkceRequired: boolean
  readonly accessTokenTtlSeconds: number
  readonly refreshTokenTtlSeconds: number
  readonly redirectUris: readonly string[]
  readonly resourceUris: readonly string[]
  readonly version: number
}

export interface SigningKeyView {
  readonly kid: string
  readonly algorithm: string
  readonly status: string
  readonly runtimeServing: boolean
  readonly activatedAt?: string
  readonly retiredAt?: string
  readonly version: number
}

export interface AuditView {
  readonly id: string
  readonly eventType: string
  readonly actorSub: string
  readonly targetSub: string
  readonly result: string
  readonly reason: string
  readonly occurredAt: string
}

export interface AuditPage {
  readonly content: readonly AuditView[]
  readonly totalElements: number
}
