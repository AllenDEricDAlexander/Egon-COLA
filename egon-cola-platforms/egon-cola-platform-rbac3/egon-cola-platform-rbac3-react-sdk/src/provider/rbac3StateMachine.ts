import type {ActiveRoleSetView, BootstrapView, Rbac3State, RoleActivationCandidateView,} from '../types'

export interface Rbac3MachineState {
  readonly status: Rbac3State
  readonly bootstrap: BootstrapView | null
  readonly candidates: RoleActivationCandidateView | null
  readonly activeRoles: ActiveRoleSetView | null
  readonly errorCode: string | null
}

export type Rbac3MachineEvent =
  | { readonly type: 'INITIALIZE' }
  | { readonly type: 'BOOTSTRAP_SUCCEEDED'; readonly bootstrap: BootstrapView }
  | {
      readonly type: 'ACTIVATION_REQUIRED'
      readonly candidates: RoleActivationCandidateView | null
      readonly activeRoles?: ActiveRoleSetView | null
    }
  | { readonly type: 'REPLACE_ACTIVE_ROLES' }
  | { readonly type: 'REPLACE_STEP_UP_REQUIRED' }
  | { readonly type: 'REPLACE_REJECTED'; readonly errorCode: string }
  | { readonly type: 'AUTHENTICATION_REQUIRED'; readonly errorCode?: string }
  | { readonly type: 'FORBIDDEN'; readonly errorCode: string }
  | { readonly type: 'ERROR'; readonly errorCode: string; readonly retryable: boolean }

export const initialRbac3MachineState: Rbac3MachineState = Object.freeze({
  status: 'UNINITIALIZED',
  bootstrap: null,
  candidates: null,
  activeRoles: null,
  errorCode: null,
})

export const transitionRbac3State = (
  state: Rbac3MachineState,
  event: Rbac3MachineEvent,
): Rbac3MachineState => {
  switch (event.type) {
    case 'INITIALIZE':
      return { ...state, status: 'LOADING_BOOTSTRAP', errorCode: null }
    case 'BOOTSTRAP_SUCCEEDED':
      return {
        status: 'READY',
        bootstrap: event.bootstrap,
        candidates: null,
        activeRoles: state.activeRoles,
        errorCode: null,
      }
    case 'ACTIVATION_REQUIRED':
      return {
        status: 'ACTIVATION_REQUIRED',
        bootstrap: null,
        candidates: event.candidates,
        activeRoles: event.activeRoles ?? null,
        errorCode: null,
      }
    case 'REPLACE_ACTIVE_ROLES':
      return { ...state, status: 'REPLACING_ACTIVE_ROLES', errorCode: null }
    case 'REPLACE_STEP_UP_REQUIRED':
      return { ...state, status: 'ACTIVATION_REQUIRED', errorCode: 'STEP_UP_REQUIRED' }
    case 'REPLACE_REJECTED':
      return {
        ...state,
        status: state.bootstrap === null ? 'ERROR_FATAL' : 'READY',
        errorCode: event.errorCode,
      }
    case 'AUTHENTICATION_REQUIRED':
      return {
        ...initialRbac3MachineState,
        status: 'AUTHENTICATION_REQUIRED',
        errorCode: event.errorCode ?? 'AUTHENTICATION_REQUIRED',
      }
    case 'FORBIDDEN':
      return { ...state, status: 'FORBIDDEN_NO_ROUTE', errorCode: event.errorCode }
    case 'ERROR':
      return {
        ...state,
        status: event.retryable ? 'ERROR_RETRYABLE' : 'ERROR_FATAL',
        errorCode: event.errorCode,
      }
  }
}
