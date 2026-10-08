# Yuheng Admin Web

[中文](README.zh-CN.md) | [Yuheng overview](../README.md)

Yuheng Admin Web is an independent React management console. It talks only to Yuheng Admin
APIs and does not call Yuheng Engine business routes, Tianshu Admin, or Provider endpoints
directly.

## Authentication

The console holds no credential or token. `src/auth/AuthContext.tsx` builds its auth client
with `createGatewayAuthClient` from `@egon-cola/xingyuan-admin-web-shared`; login navigates to
the configured Edge OIDC registration (`/oauth2/authorization/edge-ui`). Edge keeps OAuth
tokens server-side and gives the browser an HttpOnly session cookie. The browser reads the
Edge BFF session, uses its CSRF response for logout, and sends later API calls through the
Edge session. It never submits a tenant, username or password and never stores AT/RT.

The actor and capabilities come from `GET /api/v1/auth/bootstrap`
(`GatewayAuthBootstrapController`, which itself requires the `yuheng:read` permission). The
provider calls it on mount and through `refreshAuthorization()`. `CapabilityProvider` turns
the returned `permissions` list into the capability set, and `hasCapability` also accepts the
`*` wildcard. Logout clears local authorization state and calls the Edge BFF with its CSRF
header. `RequireAuth` redirects to `/login` when bootstrap fails, and `RequireCapability`
renders 403 from the same list. The Admin Web never sends an actor identity header.

## Development

```bash
npm ci
npm run typecheck
npm test -- --run
npm run lint
npm run build
```

`npm run dev` binds `127.0.0.1:18141` with `strictPort` and proxies `/oauth2`, `/bff`, and the
`/login/oauth2` callback to the Edge public entry, and `/api` to the Yuheng Admin entry. Override
the upstreams with
`YUHENG_AUTH_PROXY` and `YUHENG_ADMIN_PROXY`; configure the auth proxy to the Edge instance.
Yuheng Admin still verifies the signed token and applies the Tianquan-Jianshen decision.

`npm run e2e` runs Playwright against `http://127.0.0.1:4173`, starting
`npm run dev -- --host 127.0.0.1 --port 4173` when nothing is listening. Its fixtures mock
the OIDC entry and Admin responses, so they check browser wiring and do not prove a live Edge,
authorization server, Redis session or Yuheng topology.

## Runtime configuration

`VITE_YUHENG_ORIGIN` is the only origin the bundle reads, and an empty value keeps requests on
the current origin — which is why both the dev proxy and the container proxy work without a
rebuild. The login flow does not take a tenant identifier from the browser.

In the image, `static-server.mjs` serves `/app/dist`, answers `/healthz`, and reverse-proxies
`/api/*` to `YUHENG_ADMIN_API_BASE_URL` (default `http://yuheng-admin:18080`). An `http:`
upstream additionally requires `YUHENG_ADMIN_API_DEVELOPMENT_PLAINTEXT=true`; an `https:`
upstream requires `YUHENG_ADMIN_API_TLS_CA_PATH`, `YUHENG_ADMIN_API_TLS_CERTIFICATE_PATH`, and
`YUHENG_ADMIN_API_TLS_PRIVATE_KEY_PATH`. `PORT` defaults to `8080`.

Scope is a page-local query condition. Each page persists its own selected `bizCode`,
`namespace`, `env`, and `appCode` values in the URL through `hooks/scopeSearchParams.ts`, so
navigating between pages does not silently hide data from another scope. Cross-scope pages load
the complete authorized result set and filter it locally. The scope binding catalog is read from
`GET /api/v1/yuheng/admin/scopes` only to populate page controls and creation forms; it is not
a global header context.

Dashboard and provider pages require all four fields. Trace and audit pages require `env` and
`namespace`; gateway-group, MCP server, and remote-provider pages use optional `env` and
`namespace` filters. Interface Catalog, Applications, OpenAPI Sync, and MCP resource/prompt
pages use the full optional scope filter.

Keep credentials out of committed `.env` files. TLS termination, the Engine's trusted-origin
allow-list for cookie-bearing unsafe methods, and the Yuheng Admin authorization policy remain
deployment responsibilities.
