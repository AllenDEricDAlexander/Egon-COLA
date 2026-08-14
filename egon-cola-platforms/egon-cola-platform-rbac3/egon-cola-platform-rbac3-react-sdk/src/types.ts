export type BigintId = string
export type InstantString = string

export type Decision = 'ALLOW' | 'DENY' | 'INDETERMINATE'

export type FieldAccessLevel = 'NONE' | 'MASKED_READ' | 'READ' | 'WRITE'

export type Rbac3State =
  | 'UNINITIALIZED'
  | 'LOADING_BOOTSTRAP'
  | 'ACTIVATION_REQUIRED'
  | 'REPLACING_ACTIVE_ROLES'
  | 'READY'
  | 'AUTHENTICATION_REQUIRED'
  | 'FORBIDDEN_NO_ROUTE'
  | 'ERROR_RETRYABLE'
  | 'ERROR_FATAL'

export interface ActivationRoot {
  readonly roleId: BigintId
  readonly applicationId: BigintId
  readonly roleCode: string
}

export interface ApplicationActiveRoles {
  readonly applicationCode: string
  readonly rootRoleIds: readonly BigintId[]
}

export interface ActiveRoleSetView {
  readonly activeRoles: readonly ApplicationActiveRoles[]
  readonly activationRequired: boolean
  readonly authVersion: number
  readonly policyVersion: number
  readonly snapshotChecksum: string
}

export interface ReplaceActiveRolesRequest {
  readonly roleIds: readonly BigintId[]
    readonly expectedAuthVersion: number
}

export interface ReplaceActiveRolesResult {
  readonly activeRoles: readonly ApplicationActiveRoles[]
  readonly changed: boolean
  readonly authVersion: number
  readonly policyVersion: number
    readonly activationRequired: boolean
  readonly snapshotChecksum: string
}

export interface RoleActivationCandidate {
  readonly rootRoleId: BigintId
  readonly rootRoleCode: string
  readonly displayName: string
  readonly sourceRoleIds: readonly BigintId[]
  readonly eligibleAssignmentIds: readonly BigintId[]
  readonly mutexSetIds: readonly BigintId[]
  readonly effectiveFamilyRisk: string
  readonly requiredAuthStrength: string
  readonly landingRouteCode: string | null
}

export interface ApplicationCandidates {
  readonly applicationId: BigintId
  readonly applicationCode: string
  readonly candidates: readonly RoleActivationCandidate[]
}

export interface RoleActivationConfigurationError {
  readonly reasonCode: string
  readonly evidenceIds: readonly BigintId[]
}

export interface RoleActivationCandidateView {
  readonly applications: readonly ApplicationCandidates[]
  readonly basedOnAuthVersion: number
  readonly basedOnPolicyVersion: number
  readonly basedOnDirectorySnapshotVersion: string
  readonly configurationErrors: readonly RoleActivationConfigurationError[]
  readonly calculatedAt: InstantString
}

export interface BootstrapUser {
  readonly id: BigintId
    readonly tenantId: string
    readonly identitySub: string
    readonly status: string
}

export interface BootstrapActiveRoleContext {
  readonly applicationCode: string
  readonly activationRoot: ActivationRoot
  readonly effectiveRoleIds: readonly BigintId[]
  readonly eligibleAssignmentIds: readonly BigintId[]
  readonly landingRoute: string | null
}

export interface BootstrapView {
  readonly user: BootstrapUser
  readonly activeRoleContexts: readonly BootstrapActiveRoleContext[]
  readonly permissions: readonly string[]
  readonly apps: readonly ManifestResource[]
  readonly menus: readonly ManifestResource[]
  readonly routes: readonly ManifestResource[]
  readonly actions: readonly ManifestResource[]
  readonly fieldPolicies: Readonly<Record<string, FieldPolicyDecision>>
  readonly defaultApplicationCode: string | null
  readonly defaultRoute: string | null
  readonly authVersion: number
  readonly policyVersion: number
}

export interface AuthorizationDecision {
  readonly decision: Decision
  readonly reasonCode: string
    readonly tenantId: string
    readonly subjectId: string
  readonly permissionCode: string
  readonly authVersion: number
  readonly policyVersion: number
  readonly evidenceIds: readonly BigintId[]
  readonly decidedAt: InstantString
}

export interface PermissionRequest {
  readonly permissionCode: string
}

export interface DataScopeDecision {
  readonly decision: Decision
  readonly reasonCode: string
    readonly tenantId: string
    readonly subjectId: string
  readonly permissionCode: string
  readonly scopeType: string
  readonly allInTenant: boolean
  readonly allowedOrgIds: readonly BigintId[]
  readonly includeOrgDescendants: boolean
  readonly allowedDeptIds: readonly BigintId[]
  readonly includeDeptDescendants: boolean
  readonly allowedUserIds: readonly BigintId[]
  readonly includeSelf: boolean
  readonly selfUserId: BigintId | null
  readonly directorySnapshotVersion: string
  readonly decisionVersion: number
  readonly authVersion: number
  readonly policyVersion: number
  readonly evidenceIds: readonly BigintId[]
  readonly decidedAt: InstantString
}

export interface FieldAccess {
  readonly level: FieldAccessLevel
  readonly maskingStrategy: string | null
}

export interface FieldPolicyDecision {
  readonly decision: Decision
  readonly reasonCode: string
    readonly tenantId: string
    readonly subjectId: string
  readonly permissionCode: string
  readonly applicationCode: string
  readonly resourceCode: string
  readonly fields: Readonly<Record<string, FieldAccess>>
  readonly authVersion: number
  readonly policyVersion: number
  readonly evidenceIds: readonly BigintId[]
  readonly decidedAt: InstantString
}

export interface OperationSodDecision {
  readonly decision: Decision
  readonly reasonCode: string
    readonly tenantId: string
    readonly subjectId: string
  readonly permissionCode: string
  readonly applicationCode: string
  readonly businessResource: string
  readonly businessId: string
  readonly actionCode: string
  readonly conflictingActionCodes: readonly string[]
  readonly authVersion: number
  readonly policyVersion: number
  readonly evidenceIds: readonly BigintId[]
  readonly decidedAt: InstantString
}

export interface AuthorizationFenceDecision {
  readonly decision: Decision
  readonly reasonCode: string
    readonly tenantId: string
    readonly subjectId: string
  readonly permissionCode: string
  readonly snapshotChecksum: string
  readonly businessResource: string
  readonly businessId: string
  readonly traceId: string
  readonly authVersion: number
  readonly policyVersion: number
  readonly evidenceIds: readonly BigintId[]
  readonly decidedAt: InstantString
  readonly verifiedAt: InstantString
}

export interface AppAuthorizationContext {
  readonly applicationId: BigintId
  readonly applicationCode: string
  readonly activationRootRoleIds: readonly BigintId[]
  readonly eligibleAssignmentIds: readonly BigintId[]
  readonly effectiveRoleIds: readonly BigintId[]
  readonly permissions: readonly string[]
  readonly dataScopes: Readonly<Record<string, DataScopeDecision>>
  readonly fieldPolicies: Readonly<Record<string, FieldPolicyDecision>>
  readonly resources: readonly ManifestResource[]
  readonly landingRouteCode: string | null
}

export interface UserAuthorizationSnapshot {
    readonly systemCode: string
    readonly tenantId: string
    readonly identitySub: string
    readonly rbacUserId: string
  readonly authVersion: number
  readonly policyVersion: number
  readonly appContexts: readonly AppAuthorizationContext[]
  readonly checksum: string
  readonly generatedAt: InstantString
    readonly expiresAt: InstantString
}

export interface ManifestResource {
  readonly code: string
  readonly parentCode: string | null
  readonly name: string | null
  readonly order: number | null
  readonly path: string | null
  readonly componentKey: string | null
  readonly requiredPermissionCode: string | null
  readonly redirect: string | null
  readonly hidden: boolean | null
  readonly keepAlive: boolean | null
  readonly routeCode: string | null
  readonly gatewayOperationId: string | null
  readonly httpMethod: string | null
  readonly pathPattern: string | null
  readonly externalAccessible: boolean | null
  readonly metadata: Readonly<Record<string, string>>
}

export interface ResourceFieldDefinition {
  readonly resourceCode: string
  readonly fieldCode: string
  readonly jsonPath: string
  readonly dataType: string
  readonly sensitivity: string
  readonly defaultAccess: string
  readonly maskingStrategy: string | null
  readonly writable: boolean
  readonly exportable: boolean
}

export interface ResourceManifest {
  readonly schemaVersion: string
  readonly applicationCode: string
  readonly applicationName: string
  readonly artifactVersion: string
  readonly buildId: string
  readonly manifestVersion: number
  readonly generatedAt: InstantString
  readonly checksum: string
  readonly apps: readonly ManifestResource[]
  readonly menus: readonly ManifestResource[]
  readonly routes: readonly ManifestResource[]
  readonly actions: readonly ManifestResource[]
  readonly apis: readonly ManifestResource[]
  readonly fieldDefinitions: readonly ResourceFieldDefinition[]
}

export interface AssignmentCommand {
  readonly targetUserId: BigintId
  readonly roleId: BigintId
  readonly validFrom: InstantString
  readonly validTo: InstantString | null
  readonly assignmentType: string
  readonly reason: string
  readonly ticketNo: string | null
  readonly expectedUserAuthVersion: number
}

export interface ManagementPolicySubject {
  readonly type: string
  readonly id: BigintId
}

export interface ManagementPolicyScope {
  readonly type: string
  readonly refId: BigintId | null
}

export interface ManagementPolicyRestrictions {
  readonly maxAssignmentDays: number | null
  readonly maxRiskLevel: string
  readonly requiredAuthStrength: string
  readonly requireReason: boolean
  readonly requireTicket: boolean
  readonly includeInheritedSubjectRoles: boolean
  readonly requireAllAffiliationsInScope: boolean
  readonly allowedRoleTypes: readonly string[]
}

export interface ManagementPolicyView {
  readonly policyId: BigintId
  readonly policyCode: string
  readonly name: string
  readonly validFrom: InstantString
  readonly validTo: InstantString | null
  readonly subjects: readonly ManagementPolicySubject[]
  readonly scopes: readonly ManagementPolicyScope[]
  readonly activationRootRoleIds: readonly BigintId[]
  readonly operations: readonly string[]
  readonly restrictions: ManagementPolicyRestrictions
  readonly version: number
  readonly policyVersion: number
}

export interface BusinessParticipationCommand {
  readonly applicationCode: string
  readonly businessResource: string
  readonly businessId: string
  readonly actorUserId: BigintId
  readonly actionCode: string
  readonly businessEventId: string
  readonly occurredAt: InstantString
  readonly traceId: string
}

export interface Rbac3Client {
  getActivationCandidates(): Promise<RoleActivationCandidateView>
  getActiveRoles(): Promise<ActiveRoleSetView>
  replaceActiveRoles(
    request: ReplaceActiveRolesRequest,
  ): Promise<ReplaceActiveRolesResult>
  getBootstrap(): Promise<BootstrapView>
}

export interface ApiEnvelope<T> {
  readonly data: T
  readonly meta: {
    readonly requestId: string
    readonly traceId: string
    readonly timestamp: InstantString
  }
}
