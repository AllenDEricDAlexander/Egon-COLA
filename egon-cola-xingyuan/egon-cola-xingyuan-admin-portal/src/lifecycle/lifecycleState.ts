import type { PlatformKey } from '../manifest/types'

export type LifecycleStatus =
  | 'IDLE'
  | 'LOADING'
  | 'MOUNTED'
  | 'MOUNT_FAILED'
  | 'UNMOUNTING'
  | 'CRASHED'

export interface LifecycleState {
  readonly platformKey: PlatformKey
  readonly status: LifecycleStatus
  readonly retryCount: number
  readonly errorCode?: string
  readonly errorMessage?: string
  readonly cleanupToken?: string
}
export type LifecycleEvent =
  | { readonly type: 'LOAD' }
  | { readonly type: 'MOUNTED'; readonly cleanupToken: string }
  | { readonly type: 'MOUNT_FAILED'; readonly code: string; readonly message: string }
  | { readonly type: 'CHILD_CRASHED'; readonly message: string }
  | { readonly type: 'UNMOUNT' }
  | { readonly type: 'CLEANED' }
  | { readonly type: 'RETRY' }

export const initialLifecycleState = (platformKey: PlatformKey): LifecycleState => ({
  platformKey,
  status: 'IDLE',
  retryCount: 0,
})

const clearFailure = (state: LifecycleState): LifecycleState => ({
  ...state,
  errorCode: undefined,
  errorMessage: undefined,
  cleanupToken: undefined,
})

export const reduceLifecycle = (
  state: LifecycleState,
  event: LifecycleEvent,
): LifecycleState => {
  switch (event.type) {
    case 'LOAD':
      if (state.status === 'UNMOUNTING' || state.status === 'LOADING' || state.status === 'MOUNTED') {
        return state
      }
      return { ...clearFailure(state), status: 'LOADING' }
    case 'MOUNTED':
      if (state.status === 'UNMOUNTING') {
        return state
      }
      return {
        ...clearFailure(state),
        status: 'MOUNTED',
        cleanupToken: event.cleanupToken,
      }
    case 'MOUNT_FAILED':
      if (state.status === 'UNMOUNTING') {
        return state
      }
      return {
        ...state,
        status: 'MOUNT_FAILED',
        errorCode: event.code,
        errorMessage: event.message,
        cleanupToken: undefined,
      }
    case 'CHILD_CRASHED':
      if (state.status === 'IDLE' || state.status === 'UNMOUNTING') {
        return state
      }
      return {
        ...state,
        status: 'CRASHED',
        errorCode: 'CHILD_RUNTIME_ERROR',
        errorMessage: event.message,
      }
    case 'UNMOUNT':
      if (state.status === 'IDLE' || state.status === 'UNMOUNTING') {
        return state
      }
      return { ...state, status: 'UNMOUNTING' }
    case 'CLEANED':
      if (state.status !== 'UNMOUNTING') {
        return state
      }
      return { ...clearFailure(state), status: 'IDLE' }
    case 'RETRY':
      if (state.status !== 'MOUNT_FAILED' && state.status !== 'CRASHED' && state.status !== 'IDLE') {
        return state
      }
      return {
        ...clearFailure(state),
        status: 'LOADING',
        retryCount: state.retryCount + 1,
      }
  }
}
