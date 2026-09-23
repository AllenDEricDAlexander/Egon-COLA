# Yuheng Admin Web

[中文](README.zh-CN.md) | [Yuheng overview](../README.md)

Yuheng Admin Web is an independent React management console. It talks only to Yuheng Admin
APIs and does not call Yuheng Engine business routes, Tianshu Admin, or Provider endpoints
directly.

## Authentication

The console holds no credential. `src/auth/AuthContext.tsx` builds its auth client with
`createGatewayAuthClient` from `@egon-cola/xingyuan-admin-web-shared`, so login is the
Tianquan-Shoubing cookie/CSRF flow: `GET /oauth2/login/csrf` for the challenge, then
`POST /oauth2/login` with that token echoed in `X-Tianquan-Shoubing-CSRF` and a
`{tenantId, username, password}` body. Tianquan-Shoubing answers with HttpOnly USER
Access/Refresh cookies, and the browser replays them on every later call because the HTTP
client uses `credentials: 'include'`. There is no `localStorage` or `sessionStorage` token
bundle, no “persist login” switch, and no Authorization Code or PKCE redirect.

The actor and capabilities come from `GET /api/v1/auth/bootstrap`
(`GatewayAuthBootstrapController`, which itself requires the `yuheng:read` permission). The
provider calls it on mount, after each successful login, and again through
`refreshAuthorization()`. `CapabilityProvider` turns the returned `permissions` list into the
capability set, and `hasCapability` also accepts the `*` wildcard. Logout calls
`POST /oauth2/logout` and clears in-memory state only. `RequireAuth` redirects to `/login`
when bootstrap fails, and `RequireCapability` renders 403 from the same list. The Admin Web
never sends an actor identity header.

## Development

```bash
npm ci
npm run typecheck
npm test -- --run
npm run lint
npm run build
```

`npm run dev` binds `127.0.0.1:18141` with `strictPort` and proxies both `/oauth2` and `/api`
to the Yuheng public entry, `http://127.0.0.1:18180` by default. Override the two upstreams
independently with `YUHENG_AUTH_PROXY` and `YUHENG_ADMIN_PROXY`. Login therefore travels the
same route production uses: the Engine data plane accepts the USER cookie (or a bearer for
non-browser clients) and Yuheng Admin then verifies the signed token and applies the
Tianquan-Jianshen decision.

`npm run e2e` runs Playwright against `http://127.0.0.1:4173`, starting
`npm run dev -- --host 127.0.0.1 --port 4173` when nothing is listening. The scenarios cover
the HTTP/RPC and MCP control planes and need a reachable Yuheng Admin plus its topology. The
command is not a substitute for the Yuheng live Maven suite.

## Runtime configuration

`VITE_YUHENG_ORIGIN` is the only origin the bundle reads, and an empty value keeps requests on
the current origin — which is why both the dev proxy and the container proxy work without a
rebuild. `VITE_DEFAULT_TENANT_ID` merely prefills the tenant field of the login form.

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
