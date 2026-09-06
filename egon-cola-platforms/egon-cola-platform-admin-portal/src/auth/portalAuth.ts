import {createGatewayAuthClient} from '@egon-cola/admin-web-shared'

// Authentication stays on Gateway; the Portal never reads or passes tokens to children.
export const portalAuth = createGatewayAuthClient({
  baseUrl: import.meta.env.VITE_GATEWAY_ORIGIN ?? '',
  fetch: (...args) => globalThis.fetch(...args),
})

export interface PortalIdentity {
  readonly sub: string
  readonly tid: string
}
