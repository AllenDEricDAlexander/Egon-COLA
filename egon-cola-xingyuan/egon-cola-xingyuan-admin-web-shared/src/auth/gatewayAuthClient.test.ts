import { afterEach, describe, expect, it, vi } from 'vitest'
import {
    createGatewayAuthClient,
    GatewayAuthError,
} from './gatewayAuthClient'

const response = (body: unknown, status = 200): Response => new Response(
    JSON.stringify(body),
    {status, headers: {'Content-Type': 'application/json'}},
)

afterEach(() => {
    vi.restoreAllMocks()
})

describe('createGatewayAuthClient', () => {
    it('normalizes the Yuheng origin and sends the transient CSRF header with cookies', async () => {
        const fetcher = vi.fn()
            .mockResolvedValueOnce(response({token: 'csrf-token'}))
            .mockResolvedValueOnce(response({
                data: {identitySub: 'alice', displayName: 'Alice', mustChangePassword: false},
                success: true,
            }))
        const client = createGatewayAuthClient({
            baseUrl: 'http://127.0.0.1:18180/',
            fetch: fetcher,
        })

        await expect(client.login({
            tenantId: '1',
            username: 'alice',
            password: 'password',
        })).resolves.toEqual({
            identitySub: 'alice',
            displayName: 'Alice',
            mustChangePassword: false,
        })

        expect(fetcher).toHaveBeenNthCalledWith(1, 'http://127.0.0.1:18180/oauth2/login/csrf', {
            credentials: 'include',
            headers: {Accept: 'application/json'},
        })
        const loginInit = fetcher.mock.calls[1]?.[1] as RequestInit
        const loginHeaders = new Headers(loginInit.headers)
        expect(loginInit.credentials).toBe('include')
        expect(loginHeaders.get('X-Tianquan-Shoubing-CSRF')).toBe('csrf-token')
        expect(JSON.parse(String(loginInit.body))).toEqual({
            tenantId: '1',
            username: 'alice',
            password: 'password',
        })
    })

    it('classifies HTTP failures without exposing the response body', async () => {
        const fetcher = vi.fn().mockResolvedValue(
            response({error_description: 'password and token details must stay private'}, 401),
        )
        const client = createGatewayAuthClient({fetch: fetcher})

        const error = await client.userInfo().catch((failure) => failure) as GatewayAuthError

        expect(error).toBeInstanceOf(GatewayAuthError)
        expect(error.code).toBe('HTTP_ERROR')
        expect(error.path).toBe('/oauth2/userinfo')
        expect(error.status).toBe(401)
        expect(error.message).toBe('Yuheng authentication request failed (401)')
        expect(error.message).not.toContain('password')
        expect(error.message).not.toContain('token')
    })

    it('separates CSRF failures and network failures from normal HTTP errors', async () => {
        const csrfFailure = vi.fn().mockResolvedValue(response({message: 'stale challenge'}, 503))
        const csrfClient = createGatewayAuthClient({fetch: csrfFailure})
        const csrfError = await csrfClient.login({tenantId: '1', username: 'alice', password: 'password'})
            .catch((failure) => failure) as GatewayAuthError

        expect(csrfError).toMatchObject({
            code: 'CSRF_FAILED',
            path: '/oauth2/login/csrf',
            status: 503,
        })

        const networkFailure = vi.fn().mockRejectedValue(new TypeError('Failed to fetch'))
        const networkClient = createGatewayAuthClient({fetch: networkFailure})
        const networkError = await networkClient.bootstrap().catch((failure) => failure) as GatewayAuthError

        expect(networkError).toMatchObject({
            code: 'NETWORK_OR_TIMEOUT',
            path: '/api/v1/auth/bootstrap',
        })
        expect(networkError.message).toBe('Yuheng authentication service is unreachable')
    })
})
