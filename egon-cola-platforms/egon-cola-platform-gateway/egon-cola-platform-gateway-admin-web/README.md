# Gateway Admin Web

[中文](README.zh-CN.md) | [Gateway overview](../README.md)

Gateway Admin Web is an independent React management console. It talks only to Gateway Admin
and does not call Gateway Engine, DDC Admin, or Provider endpoints directly.

## Authentication

The Admin Web never sends an actor identity header. It stores a verified IAM Bearer Token in
`sessionStorage` by default and loads the actor and capabilities from
`GET /api/v1/gateway/admin/session`. Selecting “persist login” moves the token bundle to
`localStorage`; logout removes both copies.

Optional automatic refresh uses:

```text
VITE_GATEWAY_ADMIN_TOKEN_URL=https://iam.example.com/oauth2/token
VITE_GATEWAY_ADMIN_CLIENT_ID=gateway-admin-web
```

The configured identity provider must allow the browser client and enforce its own CORS/PKCE
policy. No client secret is embedded in the web bundle.

## Development

```bash
npm ci
npm run typecheck
npm test -- --run
npm run lint
npm run build
```

Run `npm run e2e` only with a reachable Gateway Admin and the topology required by the browser
scenarios. The command is not a substitute for the Gateway live Maven suite.

## Runtime configuration

The browser calls Gateway Admin. Set `VITE_GATEWAY_ADMIN_API_BASE_URL` to use a different API
origin; an empty value uses the current origin. The authenticated session endpoint supplies
the actor and capabilities; the browser does not configure a placeholder actor.

The deployment can set the scope selected when the admin page first opens. The configured
values are also added to the scope selectors in the page header:

```text
VITE_GATEWAY_ADMIN_DEFAULT_BIZ_CODE=default
VITE_GATEWAY_ADMIN_DEFAULT_APP_CODE=default-app
VITE_GATEWAY_ADMIN_DEFAULT_ENV=dev
VITE_GATEWAY_ADMIN_DEFAULT_NAMESPACE=default
```

Keep credentials out of committed `.env` files. The identity provider, browser CORS/PKCE setup,
TLS termination, and the Gateway Admin authorization policy remain deployment responsibilities.
