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
        const response = await fetcher(`${baseUrl}${path}`, {
            ...init,
            credentials: 'include',
            headers,
        })
        if (!response.ok) {
            const body = await response.json().catch(() => ({})) as {
                error?: string
                error_description?: string
                message?: string
            }
            throw new Error(
                body.error_description ?? body.message ?? body.error ?? `Gateway authentication failed (${response.status})`,
            )
        }
        if (response.status === 204) return undefined as T
        const payload = await response.json() as unknown
        if (isEnvelope(payload)) return payload.data as T
        return payload as T
    }

    const csrf = async (): Promise<string> => {
        const response = await fetcher(`${baseUrl}/oauth2/login/csrf`, {
            credentials: 'include',
            headers: {Accept: 'application/json'},
        })
        if (!response.ok) throw new Error('Unable to establish a login security transaction')
        const body = await response.json().catch(() => ({})) as { token?: string }
        if (!body.token) throw new Error('Gateway login CSRF response is invalid')
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
    if (!normalized) throw new Error(`${name} is required`)
    return normalized
}
