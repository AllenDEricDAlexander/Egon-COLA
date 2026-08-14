import {describe, expect, expectTypeOf, it} from 'vitest'
import type {ActivationRoot, ReplaceActiveRolesRequest, ResourceFieldDefinition,} from './types'
import {getRbac3ErrorDefinition, RBAC3_ERROR_DEFINITIONS, type Rbac3ApiError, type Rbac3ErrorResponse,} from './errors'

describe('RBAC3 TypeScript contracts', () => {
    it('models role activation with the user authorization version', () => {
    const request: ReplaceActiveRolesRequest = {
      roleIds: ['50001', '51001'],
        expectedAuthVersion: 2,
    }
        expect(request.expectedAuthVersion).toBe(2)
  })

  it('keeps bigint ids as strings', () => {
      const root: ActivationRoot = {roleId: '50001', applicationId: '71001', roleCode: 'ROLE_CASHIER_ROOT'}
    expect(typeof root.roleId).toBe('string')
    expect(typeof root.applicationId).toBe('string')
  })

  it('keeps manifest field definitions aligned with the Java contract', () => {
    const field: ResourceFieldDefinition = {
        resourceCode: 'payment-detail', fieldCode: 'bank-account-no', jsonPath: '$.bankAccountNo',
        dataType: 'STRING', sensitivity: 'HIGH', defaultAccess: 'MASKED_READ',
        maskingStrategy: 'BANK_ACCOUNT', writable: false, exportable: false,
    }
    expect(field.jsonPath).toBe('$.bankAccountNo')
  })

  const representativeErrors: readonly Rbac3ApiError[] = [
    { status: 401, code: 'AUTHENTICATION_REQUIRED', message: 'Safe', retryable: false, traceId: 'trace-01' },
    { status: 403, code: 'PERMISSION_DENIED', message: 'Safe', retryable: false, traceId: 'trace-01' },
    { status: 409, code: 'ROLE_ACTIVATION_VERSION_CONFLICT', message: 'Safe', retryable: true, traceId: 'trace-01' },
    { status: 422, code: 'ROLE_ACTIVATION_SET_INVALID', message: 'Safe', retryable: false, traceId: 'trace-01' },
    { status: 429, code: 'RATE_LIMITED', message: 'Safe', retryable: true, traceId: 'trace-01' },
    { status: 503, code: 'AUTH_RUNTIME_UNAVAILABLE', message: 'Safe', retryable: true, traceId: 'trace-01' },
  ]

    it.each(representativeErrors)('models HTTP $status with stable error metadata', (error) => {
        expect(error.status).toBeGreaterThanOrEqual(401)
        expect(error.traceId).toBe('trace-01')
        expectTypeOf(error.retryable).toBeBoolean()
    })

    it('classifies errors by stable code', () => {
        expect(getRbac3ErrorDefinition('ROLE_ACTIVATION_VERSION_CONFLICT'))
            .toEqual({status: 409, retryable: true})
        expect(Object.values(RBAC3_ERROR_DEFINITIONS).map(({status}) => status))
            .toEqual(expect.arrayContaining([401, 403, 409, 422, 429, 503]))
  })

  it('models the fixed safe error response envelope', () => {
    const response: Rbac3ErrorResponse = {
        error: {
            code: 'SSD_CONSTRAINT_VIOLATION',
            message: 'Target assignment violates separation of duties',
            retryable: false,
            details: []
        },
        meta: {requestId: 'req-01', traceId: 'trace-01', timestamp: '2026-07-30T08:00:00Z'},
    }
    expect(response.meta.traceId).toBe('trace-01')
  })
})
