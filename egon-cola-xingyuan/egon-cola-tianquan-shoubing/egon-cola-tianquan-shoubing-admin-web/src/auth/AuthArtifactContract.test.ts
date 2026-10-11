import {describe, expect, it, vi} from 'vitest'
import * as installedShared from '@egon-cola/xingyuan-admin-web-shared'

// Resolve the installed registry artifact; this suite deliberately has no shared-SDK mock.
describe('installed gateway authentication artifact', () => {
    it('navigates login and step-up without accepting credentials or fetching tokens', () => {
        const fetcher = vi.fn<typeof fetch>()
        const navigate = vi.fn()
        const client = installedShared.createGatewayAuthClient({baseUrl: 'https://edge.example', fetch: fetcher, navigate})
        client.login()
        expect(navigate).toHaveBeenNthCalledWith(1, 'https://edge.example/oauth2/authorization/edge-ui')
        client.stepUp()
        expect(navigate).toHaveBeenNthCalledWith(2, 'https://edge.example/oauth2/authorization/edge-ui?stepUp=true')
        expect(fetcher).not.toHaveBeenCalled()
        expect(installedShared).not.toHaveProperty('createOAuthClient')
        expect(installedShared).not.toHaveProperty('createTokenStore')
        expect(installedShared).not.toHaveProperty('decodeTokenPayload')
    })

    it('gets the CSRF challenge before POST logout and confirms the remote result', async () => {
        const fetcher = vi.fn<typeof fetch>()
            .mockResolvedValueOnce(new Response(JSON.stringify({headerName: 'X-CSRF-TOKEN', token: 'csrf-challenge'}), {status: 200}))
            .mockResolvedValueOnce(new Response(null, {status: 204}))
        const client = installedShared.createGatewayAuthClient({baseUrl: 'https://edge.example', fetch: fetcher, navigate: vi.fn()})
        await expect(client.logout()).resolves.toEqual({remoteState: 'CONFIRMED'})
        expect(fetcher).toHaveBeenNthCalledWith(1, 'https://edge.example/bff/csrf', expect.objectContaining({credentials: 'include'}))
        expect(fetcher).toHaveBeenNthCalledWith(2, 'https://edge.example/bff/logout', expect.objectContaining({
            method: 'POST', credentials: 'include', headers: {Accept: 'application/json', 'X-CSRF-TOKEN': 'csrf-challenge'},
        }))
        expect(fetcher.mock.calls[1][1]?.body).toBeUndefined()
    })

    it.each(['PENDING', 'UNKNOWN'] as const)('keeps the 503 remote logout state %s', async (remoteState) => {
        const fetcher = vi.fn<typeof fetch>()
            .mockResolvedValueOnce(new Response(JSON.stringify({headerName: 'X-CSRF-TOKEN', token: 'csrf-challenge'}), {status: 200}))
            .mockResolvedValueOnce(new Response(JSON.stringify({code: 'REMOTE_REVOCATION_PENDING', localLoggedOut: true, remoteState}), {status: 503}))
        const client = installedShared.createGatewayAuthClient({fetch: fetcher, navigate: vi.fn()})
        await expect(client.logout()).resolves.toEqual({remoteState})
        expect(fetcher).toHaveBeenCalledTimes(2)
        expect(fetcher.mock.calls[1][1]?.body).toBeUndefined()
    })

    it('does not send logout after an invalid CSRF response', async () => {
        const fetcher = vi.fn<typeof fetch>().mockResolvedValue(new Response(JSON.stringify({headerName: 'unsafe', token: ''}), {status: 200}))
        const client = installedShared.createGatewayAuthClient({fetch: fetcher, navigate: vi.fn()})
        await expect(client.logout()).rejects.toMatchObject({code: 'CSRF_FAILED'})
        expect(fetcher).toHaveBeenCalledTimes(1)
    })
})
