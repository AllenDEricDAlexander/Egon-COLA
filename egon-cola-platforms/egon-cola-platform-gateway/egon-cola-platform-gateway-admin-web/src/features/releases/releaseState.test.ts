import { describe, expect, it } from 'vitest'
import type { GatewayRelease } from '../../api/types'
import { releaseOutcome, shouldPollRelease } from './releaseState'

const release = (status: string, partialApplied = false): GatewayRelease => ({
  id: 'r1',
  gatewayGroupId: 'g1',
  draftRevision: 1,
  status,
  partialApplied,
  validationReport: { valid: true, errors: [], warnings: [] },
  structuredDiff: {},
  changeReason: 'test',
  createdAt: '2026-07-25T00:00:00Z',
  updatedAt: '2026-07-25T00:00:00Z',
})

describe('release evidence state', () => {
  it('never renders partial, failed, timeout, or unknown as success', () => {
    expect(releaseOutcome(release('SUCCEEDED', true))).toBe('DANGER')
    expect(releaseOutcome(release('FAILED'))).toBe('DANGER')
    expect(releaseOutcome(release('TIMEOUT'))).toBe('DANGER')
    expect(releaseOutcome(release('UNKNOWN'))).toBe('DANGER')
    expect(releaseOutcome(release('SUCCEEDED'))).toBe('SUCCESS')
  })

  it('polls only visible non-terminal releases', () => {
    expect(shouldPollRelease(release('PUBLISHING'), true)).toBe(true)
    expect(shouldPollRelease(release('PUBLISHING'), false)).toBe(false)
    expect(shouldPollRelease(release('SUCCEEDED'), true)).toBe(false)
  })
})
