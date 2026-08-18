import {createContext, type PropsWithChildren, useCallback, useEffect, useMemo, useReducer, useRef,} from 'react'
import {Rbac3RequestError} from '../errors'
import type {Rbac3Client, ReplaceActiveRolesRequest, ReplaceActiveRolesResult,} from '../types'
import {initialRbac3MachineState, type Rbac3MachineState, transitionRbac3State,} from './rbac3StateMachine'

export interface Rbac3AuthorizationContextValue extends Rbac3MachineState {
    readonly replaceActiveRoles: (
        request: ReplaceActiveRolesRequest,
    ) => Promise<ReplaceActiveRolesResult | null>
    readonly retry: () => Promise<void>
}

export const Rbac3AuthorizationContext = createContext<Rbac3AuthorizationContextValue | null>(null)

export interface Rbac3ProviderProps extends PropsWithChildren {
    readonly client: Rbac3Client
    readonly autoInitialize?: boolean
}

export const Rbac3Provider = ({
                                  client,
                                  autoInitialize = true,
                                  children,
                              }: Rbac3ProviderProps) => {
    const [state, dispatch] = useReducer(
        transitionRbac3State,
        initialRbac3MachineState,
    )
    const initializePromise = useRef<Promise<void> | null>(null)

    const loadActivation = useCallback(async () => {
        const [candidates, activeRoles] = await Promise.all([
            client.getActivationCandidates(),
            client.getActiveRoles(),
        ])
        dispatch({type: 'ACTIVATION_REQUIRED', candidates, activeRoles})
    }, [client])

    const handleFailure = useCallback((error: unknown) => {
        const classified = classifyError(error)
        if (classified.status === 401) {
            dispatch({type: 'AUTHENTICATION_REQUIRED', errorCode: classified.code})
            return
        }
        if (classified.status === 403) {
            dispatch({type: 'FORBIDDEN', errorCode: classified.code})
            return
        }
        dispatch({type: 'ERROR', errorCode: classified.code, retryable: classified.retryable})
    }, [])

    const initialize = useCallback((): Promise<void> => {
        if (initializePromise.current === null) {
            dispatch({type: 'INITIALIZE'})
            initializePromise.current = client.getAbout()
                .then((about) => dispatch({type: 'ABOUT_SUCCEEDED', about}))
                .catch(async (error: unknown) => {
                    const classified = classifyError(error)
                    if (classified.code === 'ROLE_ACTIVATION_REQUIRED') {
                        try {
                            await loadActivation()
                            return
                        } catch (activationError) {
                            handleFailure(activationError)
                            return
                        }
                    }
                    handleFailure(error)
                })
                .finally(() => {
                    initializePromise.current = null
                })
        }
        return initializePromise.current
    }, [client, handleFailure, loadActivation])

    useEffect(() => {
        if (autoInitialize) void initialize()
    }, [autoInitialize, initialize])

    const replaceActiveRoles = useCallback(async (
        request: ReplaceActiveRolesRequest,
    ): Promise<ReplaceActiveRolesResult | null> => {
        dispatch({type: 'REPLACE_ACTIVE_ROLES'})
        try {
            const result = await client.replaceActiveRoles(request)
            if (result.activationRequired) {
                await loadActivation()
                return result
            }
            const about = await client.getAbout()
            dispatch({type: 'ABOUT_SUCCEEDED', about})
            return result
        } catch (error) {
            const classified = classifyError(error)
            if (classified.code === 'STEP_UP_REQUIRED') {
                dispatch({type: 'REPLACE_STEP_UP_REQUIRED'})
                throw error
            }
            if (classified.code === 'ROLE_ACTIVATION_VERSION_CONFLICT'
                || classified.code === 'AUTH_MUTATION_CONFLICT') {
                try {
                    await loadActivation()
                    return null
                } catch (recoveryError) {
                    handleFailure(recoveryError)
                    throw recoveryError
                }
            }
            dispatch({type: 'REPLACE_REJECTED', errorCode: classified.code})
            throw error
        }
    }, [client, handleFailure, loadActivation])

    const value = useMemo<Rbac3AuthorizationContextValue>(() => ({
        ...state,
        replaceActiveRoles,
        retry: initialize,
    }), [initialize, replaceActiveRoles, state])

    return (
        <Rbac3AuthorizationContext.Provider value={value}>
            {children}
        </Rbac3AuthorizationContext.Provider>
    )
}

interface ClassifiedError {
    readonly status: number
    readonly code: string
    readonly retryable: boolean
}

const classifyError = (error: unknown): ClassifiedError => {
    if (error instanceof Rbac3RequestError) return error
    if (typeof error === 'object' && error !== null) {
        const value = error as Partial<ClassifiedError>
        return {
            status: typeof value.status === 'number' ? value.status : 0,
            code: typeof value.code === 'string' ? value.code : 'NETWORK_ERROR',
            retryable: value.retryable === true,
        }
    }
    return {status: 0, code: 'NETWORK_ERROR', retryable: true}
}
