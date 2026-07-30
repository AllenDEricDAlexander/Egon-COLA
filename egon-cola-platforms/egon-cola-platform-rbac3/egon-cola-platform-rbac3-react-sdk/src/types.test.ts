import { describe, expect, expectTypeOf, it } from 'vitest'
import type {
  ActivationRoot,
  ReplaceActiveRolesRequest,
  Rbac3TokenClaims,
} from './types'
import {
  getRbac3ErrorDefinition,
  RBAC3_ERROR_DEFINITIONS,
  type Rbac3ApiError,
  type Rbac3ErrorResponse,
} from './errors'

describe('RBAC3 TypeScript contracts', () => {
  it('models role activation as whole-set replacement', () => {
    const request: ReplaceActiveRolesRequest = {
      roleIds: ['50001', '51001'],
      expectedSessionVersion: 2,
    }

    expect(request.roleIds).toHaveLength(2)
    expect(request.expectedSessionVersion).toBe(2)
  })

  it('keeps bigint ids as strings', () => {
    const root: ActivationRoot = {
      roleId: '50001',
      applicationId: '71001',
      roleCode: 'ROLE_CASHIER_ROOT',
    }

    expect(typeof root.roleId).toBe('string')
    expect(typeof root.applicationId).toBe('string')
  })

  it('keeps token claims free of authorization collections', () => {
    const claims: Rbac3TokenClaims = {
      iss: 'https://identity.example.test',
      aud: ['egon-cola'],
      sub: '10001',
      tid: '20001',
      sid: '40001',
      av: 43,
      sv: 2,
      pv: 18,
      jti: 'token-01',
      iat: '2026-07-30T08:00:00Z',
      nbf: '2026-07-30T08:00:00Z',
      exp: '2026-07-30T08:15:00Z',
      kid: 'signing-key-01',
    }

    expect(Object.keys(claims)).not.toEqual(
      expect.arrayContaining(['roles', 'permissions', 'dataScopes', 'fieldPolicies']),
    )
  })

  const representativeErrors: readonly Rbac3ApiError[] = [
    { status: 401, code: 'AUTHENTICATION_REQUIRED', message: 'Safe', retryable: false, traceId: 'trace-01' },
    { status: 403, code: 'PERMISSION_DENIED', message: 'Safe', retryable: false, traceId: 'trace-01' },
    { status: 409, code: 'ROLE_ACTIVATION_VERSION_CONFLICT', message: 'Safe', retryable: true, traceId: 'trace-01' },
    { status: 422, code: 'ROLE_ACTIVATION_SET_INVALID', message: 'Safe', retryable: false, traceId: 'trace-01' },
    { status: 429, code: 'RATE_LIMITED', message: 'Safe', retryable: true, traceId: 'trace-01' },
    { status: 503, code: 'AUTH_RUNTIME_UNAVAILABLE', message: 'Safe', retryable: true, traceId: 'trace-01' },
  ]

  it.each(representativeErrors)(
    'models HTTP $status with stable code, retryability, and trace identity',
    (error) => {
      expect(error.status).toBeGreaterThanOrEqual(401)
      expect(error.code).not.toBe('Safe user-facing message')
      expect(error.traceId).toBe('trace-01')
      expectTypeOf(error.retryable).toBeBoolean()
    },
  )

  it('classifies errors by stable code rather than display message', () => {
    const conflict = getRbac3ErrorDefinition(
      'ROLE_ACTIVATION_VERSION_CONFLICT',
    )
    const propagation = getRbac3ErrorDefinition('AUTH_PROPAGATION_PENDING')

    expect(conflict).toEqual({ status: 409, retryable: true })
    expect(propagation).toEqual({ status: 503, retryable: true })
    expect(
      Object.values(RBAC3_ERROR_DEFINITIONS).map(({ status }) => status),
    ).toEqual(expect.arrayContaining([401, 403, 409, 422, 429, 503]))
  })

  it('models the fixed safe error response envelope', () => {
    const response: Rbac3ErrorResponse = {
      error: {
        code: 'SSD_CONSTRAINT_VIOLATION',
        message: 'Target assignment violates separation of duties',
        retryable: false,
        details: [
          {
            field: 'roleId',
            reasonCode: 'SSD_SET_LIMIT_EXCEEDED',
            evidenceId: 'sod-set-9001',
          },
        ],
      },
      meta: {
        requestId: 'req-01',
        traceId: 'trace-01',
        timestamp: '2026-07-30T08:00:00Z',
      },
    }

    expect(response.error.details[0]?.evidenceId).toBe('sod-set-9001')
    expect(response.meta.traceId).toBe('trace-01')
  })
})
