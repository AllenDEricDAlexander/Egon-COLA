import { describe, expect, it } from 'vitest'
import {
  policyWarnings,
  transportFormState,
  validatePublicRoute,
} from './routeValidation'

describe('draft safety hints', () => {
  it('blocks PUBLIC routes for internal-only operations', () => {
    expect(validatePublicRoute('PUBLIC', false)).toContain(
      'externalAccessible=false',
    )
    expect(validatePublicRoute(['INTERNAL', 'PUBLIC'], false)).toContain(
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

  it('describes conditional transport controls and safety guidance', () => {
    expect(transportFormState('HTTP', {
      transportProtocol: 'WEBSOCKET',
    })).toMatchObject({
      transportEditable: true,
      bodyModesVisible: false,
    })
    expect(transportFormState('HTTP', {
      responseMode: 'SSE',
    }).transparentResponseNotice).toContain('禁止聚合')
    expect(transportFormState('HTTP', {
      responseMode: 'BINARY_STREAM',
    }).transparentResponseNotice).toContain('禁止聚合')
    expect(transportFormState('HTTP', {
      retryEnabled: true,
    }).retryNotice).toContain('幂等')
    expect(transportFormState('RPC', {})).toMatchObject({
      transportEditable: false,
      bodyModesVisible: false,
    })
  })
})
