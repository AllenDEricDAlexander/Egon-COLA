import { describe, expect, it } from 'vitest'
import { policyWarnings, validatePublicRoute } from './routeValidation'

describe('draft safety hints', () => {
  it('blocks PUBLIC routes for internal-only operations', () => {
    expect(validatePublicRoute('PUBLIC', false)).toContain(
      'externalAccessible=false',
    )
    expect(validatePublicRoute('INTERNAL', false)).toBeUndefined()
    expect(validatePublicRoute('PUBLIC', true)).toBeUndefined()
  })

  it('warns about unsafe retry and deadline combinations', () => {
    expect(
      policyWarnings('RETRY', {
        idempotent: false,
        retryBudgetMs: 2000,
        deadlineMs: 1000,
      }),
    ).toHaveLength(2)
  })
})
