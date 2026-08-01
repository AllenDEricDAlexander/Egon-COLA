import {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useReducer,
  useRef,
  type PropsWithChildren,
} from 'react'
import { Rbac3RequestError } from '../errors'
import type {
  Rbac3Client,
  RefreshResult,
  ReplaceActiveRolesRequest,
  ReplaceActiveRolesResult,
} from '../types'
import { InMemoryAccessTokenStore } from '../auth/InMemoryAccessTokenStore'
import {
  initialRbac3MachineState,
  transitionRbac3State,
  type Rbac3MachineState,
} from './rbac3StateMachine'

export interface Rbac3SessionContextValue extends Rbac3MachineState {
  readonly replaceActiveRoles: (
    request: ReplaceActiveRolesRequest,
  ) => Promise<ReplaceActiveRolesResult | null>
  readonly refresh: () => Promise<RefreshResult>
  readonly logout: () => Promise<void>
  readonly retry: () => Promise<void>
}

export const Rbac3SessionContext = createContext<Rbac3SessionContextValue | null>(null)

export interface Rbac3ProviderProps extends PropsWithChildren {
  readonly client: Rbac3Client
  readonly accessTokenStore: InMemoryAccessTokenStore
  readonly autoInitialize?: boolean
}

export const Rbac3Provider = ({
  client,
  accessTokenStore,
  autoInitialize = true,
  children,
}: Rbac3ProviderProps) => {
  const [state, dispatch] = useReducer(
    transitionRbac3State,
    initialRbac3MachineState,
  )
  const initializePromise = useRef<Promise<void> | null>(null)
  const refreshPromise = useRef<Promise<RefreshResult> | null>(null)

  const publishRefresh = useCallback(async (result: RefreshResult) => {
    accessTokenStore.set(result.accessToken)
    if (result.roleActivationRequired) {
      const [candidates, activeRoles] = await Promise.all([
        client.getActivationCandidates(),
        client.getActiveRoles(),
      ])
      dispatch({ type: 'ACTIVATION_REQUIRED', candidates, activeRoles })
      return
    }
    const bootstrap = await client.getBootstrap()
    dispatch({ type: 'BOOTSTRAP_SUCCEEDED', bootstrap })
  }, [accessTokenStore, client])

  const handleFailure = useCallback((error: unknown) => {
    const classified = classifyError(error)
    if (classified.status === 401) {
      accessTokenStore.clear()
      dispatch({
        type: 'AUTHENTICATION_REQUIRED',
        errorCode: classified.code,
      })
      return
    }
    if (classified.status === 403) {
      dispatch({ type: 'FORBIDDEN', errorCode: classified.code })
      return
    }
    dispatch({
      type: 'ERROR',
      errorCode: classified.code,
      retryable: classified.retryable,
    })
  }, [accessTokenStore])

  const refresh = useCallback((): Promise<RefreshResult> => {
    if (refreshPromise.current === null) {
      dispatch({ type: 'REFRESH_VERSION' })
      refreshPromise.current = client.refresh()
        .then(async (result) => {
          await publishRefresh(result)
          return result
        })
        .catch((error: unknown) => {
          handleFailure(error)
          throw error
        })
        .finally(() => {
          refreshPromise.current = null
        })
    }
    return refreshPromise.current
  }, [client, handleFailure, publishRefresh])

  const initialize = useCallback((): Promise<void> => {
    if (initializePromise.current === null) {
      dispatch({ type: 'INITIALIZE' })
      initializePromise.current = client.refresh()
        .then(publishRefresh)
        .catch((error: unknown) => {
          handleFailure(error)
        })
        .finally(() => {
          initializePromise.current = null
        })
    }
    return initializePromise.current
  }, [client, handleFailure, publishRefresh])

  useEffect(() => {
    if (autoInitialize) {
      void initialize()
    }
  }, [autoInitialize, initialize])

  const replaceActiveRoles = useCallback(async (
    request: ReplaceActiveRolesRequest,
  ): Promise<ReplaceActiveRolesResult | null> => {
    dispatch({ type: 'REPLACE_ACTIVE_ROLES' })
    try {
      const result = await client.replaceActiveRoles(request)
      accessTokenStore.set(result.accessToken)
      const bootstrap = await client.getBootstrap()
      dispatch({ type: 'BOOTSTRAP_SUCCEEDED', bootstrap })
      return result
    } catch (error) {
      const classified = classifyError(error)
      if (classified.code === 'STEP_UP_REQUIRED') {
        dispatch({ type: 'REPLACE_STEP_UP_REQUIRED' })
        throw error
      }
      if (classified.retryable) {
        try {
          const refreshed = await client.refresh()
          accessTokenStore.set(refreshed.accessToken)
          const activeRoles = await client.getActiveRoles()
          if (refreshed.roleActivationRequired) {
            const candidates = await client.getActivationCandidates()
            dispatch({ type: 'ACTIVATION_REQUIRED', candidates, activeRoles })
          } else {
            const bootstrap = await client.getBootstrap()
            dispatch({ type: 'BOOTSTRAP_SUCCEEDED', bootstrap })
          }
          return null
        } catch (recoveryError) {
          handleFailure(recoveryError)
          throw recoveryError
        }
      }
      dispatch({ type: 'REPLACE_REJECTED', errorCode: classified.code })
      throw error
    }
  }, [accessTokenStore, client, handleFailure])

  const logout = useCallback(async (): Promise<void> => {
    try {
      await client.logout()
    } finally {
      accessTokenStore.clear()
      dispatch({ type: 'LOGOUT' })
    }
  }, [accessTokenStore, client])

  const value = useMemo<Rbac3SessionContextValue>(() => ({
    ...state,
    replaceActiveRoles,
    refresh,
    logout,
    retry: initialize,
  }), [initialize, logout, refresh, replaceActiveRoles, state])

  return (
    <Rbac3SessionContext.Provider value={value}>
      {children}
    </Rbac3SessionContext.Provider>
  )
}

interface ClassifiedError {
  readonly status: number
  readonly code: string
  readonly retryable: boolean
}

const classifyError = (error: unknown): ClassifiedError => {
  if (error instanceof Rbac3RequestError) {
    return error
  }
  if (typeof error === 'object' && error !== null) {
    const value = error as Partial<ClassifiedError>
    return {
      status: typeof value.status === 'number' ? value.status : 0,
      code: typeof value.code === 'string' ? value.code : 'NETWORK_ERROR',
      retryable: value.retryable === true,
    }
  }
  return { status: 0, code: 'NETWORK_ERROR', retryable: true }
}
