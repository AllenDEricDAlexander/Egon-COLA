import {createGatewayAuthClient} from '@egon-cola/xingyuan-admin-web-shared'

/** Browser authentication stays in Yuheng-owned HttpOnly cookies. */
export const gatewayAuth = createGatewayAuthClient({
  registrationId: import.meta.env.VITE_OAUTH_REGISTRATION_ID ?? 'edge-jianshen',
    baseUrl: import.meta.env.VITE_YUHENG_ORIGIN
        ?? import.meta.env.VITE_TIANQUAN_JIANSHEN_API_BASE
        ?? '',
})
