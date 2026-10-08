import {createGatewayAuthClient, type GatewaySessionResult} from '@egon-cola/xingyuan-admin-web-shared'

// Authentication stays on Yuheng; the Portal never reads or passes tokens to children.
export const portalAuth = createGatewayAuthClient({
  baseUrl: import.meta.env.VITE_YUHENG_ORIGIN ?? '',
  fetch: (...args) => globalThis.fetch(...args),
})

export type PortalIdentity = GatewaySessionResult
