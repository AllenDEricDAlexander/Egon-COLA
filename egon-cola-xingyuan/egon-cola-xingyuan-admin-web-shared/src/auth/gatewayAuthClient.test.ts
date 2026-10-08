import { afterEach, describe, expect, it, vi } from 'vitest'
import {
    createGatewayAuthClient,
    GatewayAuthError,
} from './gatewayAuthClient'

const response = (body: unknown, status = 200): Response => new Response(
    status === 204 ? null : JSON.stringify(body),
    {status, headers: {'Content-Type': 'application/json'}},
)

afterEach(() => {
    vi.restoreAllMocks()
})

describe('createGatewayAuthClient', () => {
    it('navigates to the configured OIDC registration without posting credentials', async () => {
        const fetcher = vi.fn()
        const navigate = vi.fn()
        const client = createGatewayAuthClient({
            baseUrl: 'https://edge.example/',
            registrationId: 'edge-ui',
            fetch: fetcher,
            navigate,
        })

        client.login()

        expect(navigate).toHaveBeenCalledWith(
            'https://edge.example/oauth2/authorization/edge-ui',
        )
        expect(fetcher).not.toHaveBeenCalled()
    })

    it('reads the BFF session and sends its returned CSRF header before logout', async () => {
        const fetcher = vi.fn()
            .mockResolvedValueOnce(response({
                authenticated: true,
                tenantId: 'tenant-a',
                subject: 'alice',
                displayName: 'Alice',
            }))
            .mockResolvedValueOnce(response({
                headerName: 'X-CSRF-TOKEN',
                token: 'csrf-token',
            }))
            .mockResolvedValueOnce(response({
                code: 'REMOTE_REVOCATION_PENDING',
                message: 'local logout completed',
                localLoggedOut: true,
                remoteState: 'PENDING',
            }, 503))
        const client = createGatewayAuthClient({
            baseUrl: 'https://edge.example',
            fetch: fetcher,
        })

        await expect(client.userInfo()).resolves.toEqual({
            authenticated: true,
            tenantId: 'tenant-a',
            subject: 'alice',
            displayName: 'Alice',
        })
        await expect(client.logout()).resolves.toEqual({remoteState: 'PENDING'})

        expect(fetcher).toHaveBeenNthCalledWith(1, 'https://edge.example/bff/session',
            expect.objectContaining({credentials: 'include'}))
        expect(fetcher).toHaveBeenNthCalledWith(2, 'https://edge.example/bff/csrf',
            expect.objectContaining({credentials: 'include'}))
        const logoutCall = fetcher.mock.calls[2]
        expect(logoutCall?.[0]).toBe('https://edge.example/bff/logout')
        const logoutInit = logoutCall?.[1] as RequestInit
        expect(logoutInit.method).toBe('POST')
        expect(logoutInit.credentials).toBe('include')
        expect(new Headers(logoutInit.headers).get('X-CSRF-TOKEN')).toBe('csrf-token')
    })

    it('uses OIDC reauthentication instead of posting a password for step-up', async () => {
        const fetcher = vi.fn()
        const navigate = vi.fn()
        const client = createGatewayAuthClient({
            baseUrl: 'https://edge.example',
            registrationId: 'edge-ui',
            fetch: fetcher,
            navigate,
        })

        client.stepUp()

        expect(navigate).toHaveBeenCalledWith(
            'https://edge.example/oauth2/authorization/edge-ui?stepUp=true',
        )
        expect(fetcher).not.toHaveBeenCalled()
    })

    it('classifies HTTP failures without exposing response data', async () => {
        const fetcher = vi.fn().mockResolvedValue(
            response({error_description: 'password and token details must stay private'}, 401),
        )
        const client = createGatewayAuthClient({fetch: fetcher})

        const error = await client.userInfo().catch((failure) => failure) as GatewayAuthError

        expect(error).toBeInstanceOf(GatewayAuthError)
        expect(error.code).toBe('HTTP_ERROR')
        expect(error.path).toBe('/bff/session')
        expect(error.status).toBe(401)
        expect(error.message).not.toContain('password')
        expect(error.message).not.toContain('token')
    })
})
