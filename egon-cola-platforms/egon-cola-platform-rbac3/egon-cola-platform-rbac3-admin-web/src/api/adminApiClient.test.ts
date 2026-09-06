import {describe, expect, it, vi} from 'vitest'
import {ApiError, classifyApiError} from '@egon-cola/admin-web-shared'
import {createAdminApiClients} from './adminApiClient'

describe('RBAC admin error envelopes', () => {
  it.each([
    [400, {success: false, code: 400, status: 'REQUEST_INVALID', message: 'Request payload is invalid', traceId: 'trace-1', data: null}, 'REQUEST_INVALID', 'Request payload is invalid', 'server'],
    [403, {success: false, code: 403, status: 'MANAGEMENT_POLICY_DENIED', message: 'Request rejected by authorization policy', traceId: 'trace-1', data: null}, 'MANAGEMENT_POLICY_DENIED', 'Request rejected by authorization policy', 'permission'],
    [409, {error: {code: 'AUTH_MUTATION_CONFLICT', message: 'Conflict', retryable: false}, meta: {traceId: 'trace-1'}}, 'AUTH_MUTATION_CONFLICT', 'Conflict', 'validation'],
  ])('preserves the error code and message for HTTP %s', async (status, body, code, message, type) => {
    const fetcher = vi.fn().mockResolvedValue(new Response(JSON.stringify(body), {status: status as number}))
    const {featureClient} = createAdminApiClients('', fetcher)
    const error = await featureClient.request('/api/rbac3/v1/users/42/role-assignments').catch(value => value)
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({status, code, message, requestId: 'trace-1'})
    expect(classifyApiError(error).type).toBe(type)
    expect(classifyApiError(error).title).not.toBe('未知错误')
  })
})
