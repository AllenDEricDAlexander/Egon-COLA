import { describe, expect, it } from 'vitest'
import {
  initialLifecycleState,
  reduceLifecycle,
  type LifecycleState,
} from './lifecycleState'

describe('reduceLifecycle', () => {
  it('requires cleanup before a remount can start', () => {
    let state = reduceLifecycle(initialLifecycleState('gateway'), { type: 'LOAD' })
    state = reduceLifecycle(state, { type: 'MOUNTED', cleanupToken: 'gateway:1' })
    state = reduceLifecycle(state, { type: 'UNMOUNT' })

    expect(state.status).toBe('UNMOUNTING')
    expect(reduceLifecycle(state, { type: 'LOAD' })).toBe(state)

    state = reduceLifecycle(state, { type: 'CLEANED' })
    expect(state.status).toBe('IDLE')
    expect(reduceLifecycle(state, { type: 'LOAD' }).status).toBe('LOADING')
  })

  it('keeps a child crash local and supports an isolated retry', () => {
    const mounted: LifecycleState = {
      ...initialLifecycleState('ddc'),
      status: 'MOUNTED',
      cleanupToken: 'ddc:1',
    }
    const crashed = reduceLifecycle(mounted, { type: 'CHILD_CRASHED', message: 'render failed' })

    expect(crashed).toMatchObject({
      platformKey: 'ddc',
      status: 'CRASHED',
      errorCode: 'CHILD_RUNTIME_ERROR',
      errorMessage: 'render failed',
    })
    expect(reduceLifecycle(crashed, { type: 'RETRY' })).toMatchObject({
      platformKey: 'ddc',
      status: 'LOADING',
      retryCount: 1,
    })
  })

  it('records a mount failure without changing the selected platform', () => {
    const state = reduceLifecycle(
      reduceLifecycle(initialLifecycleState('idp'), { type: 'LOAD' }),
      { type: 'MOUNT_FAILED', code: 'MANIFEST_VERSION_UNSUPPORTED', message: 'unsupported' },
    )

    expect(state).toMatchObject({
      platformKey: 'idp',
      status: 'MOUNT_FAILED',
      errorCode: 'MANIFEST_VERSION_UNSUPPORTED',
      errorMessage: 'unsupported',
    })
  })
})
