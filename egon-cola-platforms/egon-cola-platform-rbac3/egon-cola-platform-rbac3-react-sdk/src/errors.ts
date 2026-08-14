export const RBAC3_ERROR_DEFINITIONS = {
  REQUEST_INVALID: { status: 400, retryable: false },
  AUTHENTICATION_REQUIRED: { status: 401, retryable: false },
  AUTHENTICATION_FAILED: { status: 401, retryable: false },
  TOKEN_INVALID: { status: 401, retryable: false },
  AUTH_VERSION_MISMATCH: { status: 401, retryable: false },
  POLICY_VERSION_MISMATCH: { status: 401, retryable: false },
  PERMISSION_DENIED: { status: 403, retryable: false },
  MANAGEMENT_POLICY_DENIED: { status: 403, retryable: false },
  MANAGED_USER_SCOPE_DENIED: { status: 403, retryable: false },
  MANAGED_ROLE_SCOPE_DENIED: { status: 403, retryable: false },
  MANAGEMENT_OPERATION_DENIED: { status: 403, retryable: false },
  PRIVILEGED_ROLE_MANAGEMENT_DENIED: { status: 403, retryable: false },
  SELF_PRIVILEGE_ESCALATION_DENIED: { status: 403, retryable: false },
  ROLE_ACTIVATION_NOT_ELIGIBLE: { status: 403, retryable: false },
  SSD_CONSTRAINT_VIOLATION: { status: 403, retryable: false },
  OPERATION_SOD_VIOLATION: { status: 403, retryable: false },
  DATA_SCOPE_DENIED: { status: 403, retryable: false },
  FIELD_ACCESS_DENIED: { status: 403, retryable: false },
  RESOURCE_MANIFEST_CONFLICT: { status: 409, retryable: false },
  ROLE_PREREQUISITE_NOT_MET: { status: 409, retryable: false },
  ROLE_CARDINALITY_EXCEEDED: { status: 409, retryable: false },
  ROLE_INHERITANCE_CYCLE: { status: 409, retryable: false },
  ASSIGNMENT_TIME_OVERLAP: { status: 409, retryable: false },
  ROLE_ACTIVATION_REQUIRED: { status: 409, retryable: false },
  ROLE_ACTIVATION_ROOT_AMBIGUOUS: { status: 409, retryable: false },
  APP_ROLE_ACTIVATION_MUTEX_VIOLATION: { status: 409, retryable: false },
  ROLE_ACTIVATION_VERSION_CONFLICT: { status: 409, retryable: true },
  IDEMPOTENCY_CONFLICT: { status: 409, retryable: false },
  ROLE_ACTIVATION_SET_INVALID: { status: 422, retryable: false },
  ROLE_FAMILY_SIZE_LIMIT_EXCEEDED: { status: 422, retryable: false },
  AUTH_RUNTIME_UNAVAILABLE: { status: 503, retryable: true },
  AUTH_PROPAGATION_PENDING: { status: 503, retryable: true },
  TENANT_CONTEXT_INVALID: { status: 400, retryable: false },
  DIRECTORY_SNAPSHOT_INVALID: { status: 400, retryable: false },
    USER_LOCKED: {status: 401, retryable: false},
    STEP_UP_REQUIRED: {status: 401, retryable: false},
  SERVICE_IDENTITY_DENIED: { status: 403, retryable: false },
  APPLICATION_BINDING_DENIED: { status: 403, retryable: false },
  RESOURCE_NOT_FOUND: { status: 404, retryable: false },
  RESOURCE_VERSION_CONFLICT: { status: 409, retryable: true },
  DIRECTORY_SNAPSHOT_CONFLICT: { status: 409, retryable: false },
  DIRECTORY_SNAPSHOT_STALE: { status: 409, retryable: false },
  ROLE_DISABLED: { status: 409, retryable: true },
  AUTH_MUTATION_CONFLICT: { status: 409, retryable: true },
  INVALID_STATE_TRANSITION: { status: 409, retryable: true },
  ROLE_APPLICATION_MISMATCH: { status: 409, retryable: false },
  MANAGEMENT_POLICY_INCOMPLETE: { status: 422, retryable: false },
  RESOURCE_MANIFEST_INVALID: { status: 422, retryable: false },
  RATE_LIMITED: { status: 429, retryable: true },
  AUTH_SNAPSHOT_NOT_READY: { status: 503, retryable: true },
  SIGNING_KEY_UNAVAILABLE: { status: 503, retryable: true },
  DIRECTORY_RUNTIME_UNAVAILABLE: { status: 503, retryable: true },
} as const

export type Rbac3ErrorCode = keyof typeof RBAC3_ERROR_DEFINITIONS

export type Rbac3ErrorStatus =
  (typeof RBAC3_ERROR_DEFINITIONS)[Rbac3ErrorCode]['status']

type Rbac3ErrorCodeForStatus<Status extends Rbac3ErrorStatus> = {
  [Code in Rbac3ErrorCode]:
    (typeof RBAC3_ERROR_DEFINITIONS)[Code]['status'] extends Status
      ? Code
      : never
}[Rbac3ErrorCode]

export interface Rbac3ErrorDetail {
  readonly field: string
  readonly reasonCode: string
  readonly evidenceId: string
}

interface Rbac3ApiErrorBase {
  readonly message: string
  readonly retryable: boolean
  readonly traceId: string
  readonly requestId?: string
  readonly timestamp?: string
  readonly details?: readonly Rbac3ErrorDetail[]
  readonly retryAfterSeconds?: number
}

type Rbac3ApiErrorForStatus<Status extends Rbac3ErrorStatus> =
  Rbac3ApiErrorBase & {
    readonly status: Status
    readonly code: Rbac3ErrorCodeForStatus<Status>
  }

export type Rbac3ApiError = {
  [Status in Rbac3ErrorStatus]: Rbac3ApiErrorForStatus<Status>
}[Rbac3ErrorStatus]

export interface Rbac3ErrorResponse {
  readonly error: {
    readonly code: Rbac3ErrorCode
    readonly message: string
    readonly retryable: boolean
    readonly details: readonly Rbac3ErrorDetail[]
  }
  readonly meta: {
    readonly requestId: string
    readonly traceId: string
    readonly timestamp: string
  }
}

export const getRbac3ErrorDefinition = (code: Rbac3ErrorCode) =>
  RBAC3_ERROR_DEFINITIONS[code]

export class Rbac3RequestError extends Error {
  readonly status: number
  readonly code: Rbac3ErrorCode | 'NETWORK_ERROR' | 'INVALID_RESPONSE'
  readonly retryable: boolean
  readonly traceId: string | null

  constructor(options: {
    readonly status: number
    readonly code: Rbac3ErrorCode | 'NETWORK_ERROR' | 'INVALID_RESPONSE'
    readonly message: string
    readonly retryable: boolean
    readonly traceId?: string | null
  }) {
    super(options.message)
    this.name = 'Rbac3RequestError'
    this.status = options.status
    this.code = options.code
    this.retryable = options.retryable
    this.traceId = options.traceId ?? null
  }
}
