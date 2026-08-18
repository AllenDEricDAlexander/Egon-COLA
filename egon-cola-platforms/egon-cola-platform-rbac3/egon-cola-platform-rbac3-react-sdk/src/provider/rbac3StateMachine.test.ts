import { describe, expect, it } from 'vitest'
import {
  initialRbac3MachineState,
  transitionRbac3State,
} from './rbac3StateMachine'

describe('RBAC3 state machine', () => {
  it('requires explicit about before anything is ready', () => {
    const loading = transitionRbac3State(initialRbac3MachineState, {
      type: 'INITIALIZE',
    })

    expect(loading.status).toBe('LOADING_ABOUT')
    expect(loading.about).toBeNull()
  })

  it('enters activation required after login without an active role', () => {
    const state = transitionRbac3State(initialRbac3MachineState, {
      type: 'ACTIVATION_REQUIRED',
      candidates: null,
    })

    expect(state.status).toBe('ACTIVATION_REQUIRED')
    expect(state.about).toBeNull()
  })

  it('keeps the previous ready snapshot after a mutex conflict', () => {
    const ready = transitionRbac3State(initialRbac3MachineState, {
      type: 'ABOUT_SUCCEEDED',
      about: {
        permissions: ['orders:read'],
      } as never,
    })
    const replacing = transitionRbac3State(ready, {
      type: 'REPLACE_ACTIVE_ROLES',
    })
    const restored = transitionRbac3State(replacing, {
      type: 'REPLACE_REJECTED',
      errorCode: 'APP_ROLE_ACTIVATION_MUTEX_VIOLATION',
    })

    expect(restored.status).toBe('READY')
    expect(restored.about).toBe(ready.about)
    expect(restored.errorCode).toBe('APP_ROLE_ACTIVATION_MUTEX_VIOLATION')
  })

  it('does not turn a forbidden response into an authentication redirect', () => {
    const state = transitionRbac3State(initialRbac3MachineState, {
      type: 'FORBIDDEN',
      errorCode: 'PERMISSION_DENIED',
    })

    expect(state.status).toBe('FORBIDDEN_NO_ROUTE')
  })
})
