export interface GatewayAuthClientOptions {
    readonly baseUrl?: string
    readonly fetch?: typeof globalThis.fetch
    readonly registrationId?: string
    readonly navigate?: (url: string) => void
}

export interface GatewaySessionResult {
    readonly authenticated: boolean
    readonly tenantId: string
    readonly subject: string
    readonly displayName: string
}

export interface GatewayCsrfResult {
    readonly headerName: string
    readonly token: string
}

export type GatewayLogoutRemoteState = 'CONFIRMED' | 'PENDING' | 'UNKNOWN'

export interface GatewayLogoutResult {
    readonly remoteState: GatewayLogoutRemoteState
}

export type GatewayAuthErrorCode =
    | 'NETWORK_OR_TIMEOUT'
    | 'HTTP_ERROR'
    | 'CSRF_FAILED'
    | 'INVALID_RESPONSE'
    | 'INVALID_INPUT'

export class GatewayAuthError extends Error {
    readonly code: GatewayAuthErrorCode
    readonly path: string
    readonly status?: number

    constructor(
        code: GatewayAuthErrorCode,
        path: string,
        message: string,
        status?: number,
    ) {
        super(message)
        this.name = 'GatewayAuthError'
        this.code = code
        this.path = path
        this.status = status
    }
}

/** Browser authentication uses the Edge OIDC entry and keeps tokens server-side. */
export interface GatewayAuthClient {
    login(): void

    logout(): Promise<GatewayLogoutResult>

    stepUp(): void

    userInfo<T>(): Promise<T>

    bootstrap<T>(path?: string): Promise<T>
}

export const createGatewayAuthClient = (
    options: GatewayAuthClientOptions = {},
): GatewayAuthClient => {
    const baseUrl = normalizeBaseUrl(options.baseUrl ?? '')
    const fetcher = options.fetch ?? globalThis.fetch.bind(globalThis)
    const registrationId = configuredRegistrationId(options.registrationId ?? 'edge-ui')
    const navigate = options.navigate ?? ((url: string) => window.location.assign(url))
    const authorizationPath = '/oauth2/authorization/' + encodeURIComponent(registrationId)

    const request = async <T>(path: string, init: RequestInit = {}): Promise<T> => {
        const headers = new Headers(init.headers)
        headers.set('Accept', 'application/json')
        let response: Response
        try {
            response = await fetcher(baseUrl + path, {
                ...init,
                credentials: 'include',
                headers,
            })
        } catch {
            throw new GatewayAuthError(
                'NETWORK_OR_TIMEOUT',
                path,
                'Yuheng authentication service is unreachable',
            )
        }
        if (!response.ok) {
            throw httpError(path, response.status)
        }
        if (response.status === 204) return undefined as T
        let payload: unknown
        try {
            payload = await response.json() as unknown
        } catch {
            throw new GatewayAuthError(
                'INVALID_RESPONSE',
                path,
                'Yuheng authentication response is invalid',
                response.status,
            )
        }
        if (isEnvelope(payload)) return payload.data as T
        return payload as T
    }

    const csrf = async (): Promise<GatewayCsrfResult> => {
        const path = '/bff/csrf'
        const result = await request<unknown>(path)
        if (!isCsrfResult(result) || result.headerName !== 'X-CSRF-TOKEN') {
            throw new GatewayAuthError(
                'CSRF_FAILED',
                path,
                'Yuheng logout security challenge is invalid',
            )
        }
        return result
    }

    return {
        login: () => navigate(baseUrl + authorizationPath),

        logout: async () => {
            const csrfToken = await csrf()
            let response: Response
            try {
                response = await fetcher(baseUrl + '/bff/logout', {
                    method: 'POST',
                    credentials: 'include',
                    headers: {
                        Accept: 'application/json',
                        [csrfToken.headerName]: csrfToken.token,
                    },
                })
            } catch {
                throw new GatewayAuthError(
                    'NETWORK_OR_TIMEOUT',
                    '/bff/logout',
                    'Yuheng authentication service is unreachable',
                )
            }
            if (response.status === 204) return {remoteState: 'CONFIRMED'}
            if (response.status === 503) {
                const payload: unknown = await response.json().catch(() => undefined)
                if (isLogoutFailure(payload)) {
                    return {remoteState: payload.remoteState}
                }
            }
            if (!response.ok) throw httpError('/bff/logout', response.status)
            throw new GatewayAuthError(
                'INVALID_RESPONSE',
                '/bff/logout',
                'Yuheng logout response is invalid',
                response.status,
            )
        },

        stepUp: () => navigate(baseUrl + authorizationPath + '?stepUp=true'),

        userInfo: <T>() => request<T>('/bff/session'),

        bootstrap: <T>(path = '/api/v1/auth/bootstrap') => request<T>(path),
    }
}

const isEnvelope = (value: unknown): value is { data: unknown; success: boolean } =>
    typeof value === 'object'
    && value !== null
    && 'success' in value
    && 'data' in value

const isCsrfResult = (value: unknown): value is GatewayCsrfResult =>
    typeof value === 'object'
    && value !== null
    && 'headerName' in value
    && typeof value.headerName === 'string'
    && 'token' in value
    && typeof value.token === 'string'
    && value.token.length > 0

const isLogoutFailure = (
    value: unknown,
): value is { code: 'REMOTE_REVOCATION_PENDING'; localLoggedOut: true; remoteState: 'PENDING' | 'UNKNOWN' } =>
    typeof value === 'object'
    && value !== null
    && 'code' in value
    && value.code === 'REMOTE_REVOCATION_PENDING'
    && 'localLoggedOut' in value
    && value.localLoggedOut === true
    && 'remoteState' in value
    && (value.remoteState === 'PENDING' || value.remoteState === 'UNKNOWN')

const httpError = (path: string, status: number): GatewayAuthError =>
    new GatewayAuthError(
        'HTTP_ERROR',
        path,
        'Yuheng authentication request failed (' + status + ')',
        status,
    )

const normalizeBaseUrl = (value: string): string => {
    const trimmed = value.trim()
    return trimmed.endsWith('/') ? trimmed.slice(0, -1) : trimmed
}

const configuredRegistrationId = (value: string): string => {
    const registrationId = value.trim()
    if (!/^[A-Za-z0-9_-]{1,128}$/.test(registrationId)) {
        throw new GatewayAuthError(
            'INVALID_INPUT',
            '/oauth2/authorization',
            'OAuth registration is invalid',
        )
    }
    return registrationId
}
