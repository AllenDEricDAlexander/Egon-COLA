import type { GatewayRelease } from '../../api/types'

const terminal = new Set(['SUCCESS', 'FAILED', 'TIMEOUT', 'UNKNOWN', 'SUPERSEDED'])

export const shouldPollRelease = (
  release: GatewayRelease | undefined,
  visible: boolean,
): boolean => Boolean(visible && release && !terminal.has(release.status.toUpperCase()))

export const releaseOutcome = (
  release: Pick<GatewayRelease, 'status' | 'partialApplied'>,
): 'SUCCESS' | 'DANGER' | 'PROGRESS' => {
  if (release.partialApplied) return 'DANGER'
  if (release.status.toUpperCase() === 'SUCCESS') return 'SUCCESS'
  if (['FAILED', 'TIMEOUT', 'UNKNOWN'].includes(release.status.toUpperCase())) return 'DANGER'
  return 'PROGRESS'
}
