import type { GatewayRelease } from '../../api/types'

const terminal = new Set(['SUCCEEDED', 'FAILED', 'TIMEOUT', 'UNKNOWN', 'ROLLED_BACK'])

export const shouldPollRelease = (
  release: GatewayRelease | undefined,
  visible: boolean,
): boolean => Boolean(visible && release && !terminal.has(release.status.toUpperCase()))

export const releaseOutcome = (
  release: Pick<GatewayRelease, 'status' | 'partialApplied'>,
): 'SUCCESS' | 'DANGER' | 'PROGRESS' => {
  if (release.partialApplied) return 'DANGER'
  if (release.status.toUpperCase() === 'SUCCEEDED') return 'SUCCESS'
  if (['FAILED', 'TIMEOUT', 'UNKNOWN'].includes(release.status.toUpperCase())) return 'DANGER'
  return 'PROGRESS'
}
