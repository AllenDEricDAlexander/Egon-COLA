import {createGatewayAuthClient} from '@egon-cola/admin-web-shared'

/** Browser authentication stays in Gateway-owned HttpOnly cookies. */
export const gatewayAuth = createGatewayAuthClient({
    baseUrl: import.meta.env.VITE_GATEWAY_ORIGIN
        ?? import.meta.env.VITE_RBAC3_API_BASE
        ?? '',
})
