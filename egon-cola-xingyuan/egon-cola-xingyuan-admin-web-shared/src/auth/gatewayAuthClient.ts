export interface GatewayAuthClientOptions {
    readonly baseUrl?: string
    readonly fetch?: typeof globalThis.fetch
}

export interface GatewayLoginRequest {
    readonly tenantId: string
    readonly username: string
    readonly password: string
}

export interface GatewayLoginResult {
    readonly identitySub: string
    readonly displayName: string
    readonly mustChangePassword: boolean
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

/**
 * Browser authentication transport for the public Gateway identity routes.
 * USER access and refresh tokens stay in HttpOnly cookies; this client never
 * reads, stores, parses, or returns either token.
 */
export interface GatewayAuthClient {
    login(request: GatewayLoginRequest): Promise<GatewayLoginResult>

    logout(): Promise<void>

    stepUp(password: string): Promise<void>

    userInfo<T>(): Promise<T>

    bootstrap<T>(path?: string): Promise<T>
}

export const createGatewayAuthClient = (
    options: GatewayAuthClientOptions = {},
): GatewayAuthClient => {
    const baseUrl = normalizeBaseUrl(options.baseUrl ?? '')
    const fetcher = options.fetch ?? globalThis.fetch.bind(globalThis)

    const request = async <T>(path: string, init: RequestInit = {}): Promise<T> => {
        const headers = new Headers(init.headers)
        headers.set('Accept', 'application/json')
        let response: Response
        try {
            response = await fetcher(`${baseUrl}${path}`, {
                ...init,
                credentials: 'include',
                headers,
            })
        } catch {
            throw new GatewayAuthError(
                'NETWORK_OR_TIMEOUT',
                path,
                'Gateway authentication service is unreachable',
            )
        }
        if (!response.ok) {
            throw new GatewayAuthError(
                'HTTP_ERROR',
                path,
                `Gateway authentication request failed (${response.status})`,
                response.status,
            )
        }
        if (response.status === 204) return undefined as T
        let payload: unknown
        try {
            payload = await response.json() as unknown
        } catch {
            throw new GatewayAuthError(
                'INVALID_RESPONSE',
                path,
                'Gateway authentication response is invalid',
                response.status,
            )
        }
        if (isEnvelope(payload)) return payload.data as T
        return payload as T
    }

    const csrf = async (): Promise<string> => {
        const path = '/oauth2/login/csrf'
        let response: Response
        try {
            response = await fetcher(`${baseUrl}${path}`, {
                credentials: 'include',
                headers: {Accept: 'application/json'},
            })
        } catch {
            throw new GatewayAuthError(
                'NETWORK_OR_TIMEOUT',
                path,
                'Gateway authentication service is unreachable',
            )
        }
        if (!response.ok) {
            throw new GatewayAuthError(
                'CSRF_FAILED',
                path,
                `Gateway login security challenge failed (${response.status})`,
                response.status,
            )
        }
        let body: { token?: string }
        try {
            body = await response.json() as { token?: string }
        } catch {
            throw new GatewayAuthError(
                'INVALID_RESPONSE',
                path,
                'Gateway login CSRF response is invalid',
                response.status,
            )
        }
        if (!body.token) {
            throw new GatewayAuthError(
                'CSRF_FAILED',
                path,
                'Gateway login CSRF response is invalid',
                response.status,
            )
        }
        return body.token
    }

    return {
        login: async (credentials) => {
            const csrfToken = await csrf()
            return request<GatewayLoginResult>('/oauth2/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-IDP-CSRF': csrfToken,
                },
                body: JSON.stringify({
                    tenantId: required(credentials.tenantId, 'tenantId'),
                    username: required(credentials.username, 'username'),
                    password: required(credentials.password, 'password'),
                }),
            })
        },

        logout: async () => {
            await request<void>('/oauth2/logout', {method: 'POST'})
        },

        stepUp: async (password) => {
            await request<void>('/oauth2/step-up', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({password: required(password, 'password')}),
            })
        },

        userInfo: <T>() => request<T>('/oauth2/userinfo'),

        bootstrap: <T>(path = '/api/v1/auth/bootstrap') => request<T>(path),
    }
}

const isEnvelope = (value: unknown): value is { data: unknown; success: boolean } =>
    typeof value === 'object'
    && value !== null
    && 'success' in value
    && 'data' in value

const normalizeBaseUrl = (value: string): string => {
    const trimmed = value.trim()
    return trimmed.endsWith('/') ? trimmed.slice(0, -1) : trimmed
}

const required = (value: string, name: string): string => {
    const normalized = value.trim()
    if (!normalized) {
        throw new GatewayAuthError('INVALID_INPUT', '/oauth2/login', `${name} is required`)
    }
    return normalized
}
