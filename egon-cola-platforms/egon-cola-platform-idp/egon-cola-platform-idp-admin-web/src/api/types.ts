// ─── Auth Bootstrap ───────────────────────────────────────────────
export interface AuthorizationBootstrap {
    readonly user: {
        readonly id: string
        readonly tenantId: string
        readonly identitySub: string
        readonly status: string
    }
    readonly activeRoleContexts: readonly {
        readonly applicationCode: string
        readonly activationRoot: { readonly roleId: string }
        readonly effectiveRoleIds: readonly string[]
    }[]
  readonly permissions: readonly string[]
    readonly apps: readonly unknown[]
    readonly menus: readonly unknown[]
    readonly routes: readonly unknown[]
    readonly actions: readonly unknown[]
    readonly fieldPolicies: Readonly<Record<string, unknown>>
    readonly defaultApplicationCode: string | null
    readonly defaultRoute: string | null
  readonly authVersion: number
  readonly policyVersion: number
}

// ─── Identity Users ──────────────────────────────────────────────
export interface IdentityUserVO {
  readonly subject: string
  readonly username: string
  readonly displayName: string
  readonly status: string
  readonly failedLoginCount: number
  readonly lockedUntil?: string
  readonly lastLoginAt?: string
  readonly version: number
}

export interface CreatedIdentityUserVO extends IdentityUserVO {
    readonly oneTimePassword: string
}

export interface ResetPasswordVO {
    readonly subject: string
  readonly oneTimePassword: string
    readonly mustChangePassword: boolean
}

export interface CreateIdentityUserDTO {
    username: string
    displayName: string
}

export interface UpdateIdentityUserDTO {
    displayName: string
    status: string
    expectedVersion: number
}

// ─── OAuth Clients ───────────────────────────────────────────────
export interface OAuthClientVO {
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
    readonly createdAt: string
    readonly updatedAt: string
}

export interface CreateOAuthClientDTO {
    clientId: string
    clientName: string
    clientType?: string
    accessTokenTtlSeconds: number
    refreshTokenTtlSeconds: number
    redirectUris: string[]
    resourceUris: string[]
}

export interface UpdateOAuthClientDTO {
    clientName: string
    status: string
    accessTokenTtlSeconds: number
    refreshTokenTtlSeconds: number
    expectedVersion: number
}

export interface OAuthValueDTO {
    value: string
}

// ─── Resource Servers ────────────────────────────────────────────
export interface ClientJwkVO {
    readonly kid: string
    readonly algorithm: string
    readonly status: string
    readonly validFrom: string
    readonly validTo: string
    readonly lastUsedAt?: string
    readonly version: number
}

export interface ResourceServerVO {
    readonly resourceServerId: string
    readonly resourceUri: string
    readonly bizCode: string
    readonly appCode: string
    readonly environment: string
    readonly displayName: string
    readonly managementClientId: string
    readonly rbacApplicationCode: string
    readonly entryPermissionCode: string
    readonly admissionTicketTtlSeconds: number
    readonly status: string
    readonly version: number
    readonly keys: readonly ClientJwkVO[]
    readonly createdAt: string
    readonly updatedAt: string
}

export interface CreateResourceServerDTO {
    resourceServerId: string
    resourceUri: string
    bizCode: string
    appCode: string
    environment: string
    displayName: string
    managementClientId: string
    rbacApplicationCode: string
    entryPermissionCode: string
    admissionTicketTtlSeconds: number
    key: CreateClientJwkDTO
}

export interface CreateClientJwkDTO {
    kid: string
    algorithm: string
    publicJwk: string
    validFrom: string
    validTo: string
    expectedResourceVersion: number
}

export interface ResourceVersionDTO {
    expectedVersion: number
}

export interface BatchResourceServerActionDTO {
    bizCode: string
    environment: string
    appCodes: string[]
    action: 'ENABLE' | 'DISABLE'
    expectedVersions: Record<string, number>
}

// ─── Client Resource Grants ──────────────────────────────────────
export type ResourceGrantType = 'USER_DELEGATION' | 'SERVICE_ACCESS'

export interface ClientResourceGrantVO {
    readonly clientId: string
    readonly resourceServerId: string
    readonly grantType: ResourceGrantType
    readonly tenantId: string
    readonly allowedScopes: readonly string[]
    readonly status: string
    readonly version: number
}

export interface UpsertClientResourceGrantDTO {
    grantType: ResourceGrantType
    tenantId?: string
    allowedScopes: string[]
    expectedResourceVersion: number
    expectedGrantVersion?: number
}

export interface DeleteClientResourceGrantDTO {
    grantType: ResourceGrantType
    tenantId?: string
    expectedResourceVersion: number
    expectedGrantVersion: number
}

export interface BatchClientResourceGrantDTO {
    bizCode: string
    environment: string
    appCodes: string[]
    action: 'UPSERT' | 'DELETE'
    grantType: ResourceGrantType
    tenantId?: string
    allowedScopes: string[]
    expectedResourceVersions: Record<string, number>
    expectedGrantVersions: Record<string, number>
}

// ─── Signing Keys ────────────────────────────────────────────────
export interface SigningKeyVO {
  readonly kid: string
  readonly algorithm: string
    readonly publicJwk: string
  readonly status: string
  readonly runtimeServing: boolean
  readonly activatedAt?: string
  readonly retiredAt?: string
  readonly version: number
    readonly createdAt: string
    readonly updatedAt: string
}

export interface PublishSigningKeyDTO {
    kid: string
    encryptedPrivateKey: string
    publicJwk: string
}

// ─── Audits ──────────────────────────────────────────────────────
export interface AuditVO {
  readonly id: string
  readonly eventType: string
  readonly actorSub: string
  readonly targetSub: string
  readonly result: string
  readonly reason: string
  readonly occurredAt: string
}

export interface AuditPageVO {
    readonly content: readonly AuditVO[]
  readonly totalElements: number
}
