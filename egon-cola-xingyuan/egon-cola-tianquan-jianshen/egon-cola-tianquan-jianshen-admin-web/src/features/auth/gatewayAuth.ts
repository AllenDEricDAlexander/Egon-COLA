import {createGatewayAuthClient} from '@egon-cola/xingyuan-admin-web-shared'

/** Browser authentication stays in Yuheng-owned HttpOnly cookies. */
export const gatewayAuth = createGatewayAuthClient({
    baseUrl: import.meta.env.VITE_YUHENG_ORIGIN
        ?? import.meta.env.VITE_TIANQUAN_JIANSHEN_API_BASE
        ?? '',
})
