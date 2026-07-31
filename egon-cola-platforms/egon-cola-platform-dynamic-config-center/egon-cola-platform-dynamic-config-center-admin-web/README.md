# DDC Admin Web

[中文](README.zh-CN.md) | [DDC overview](../README.md)

DDC Admin Web is the standalone management console for the Egon COLA Dynamic
Config Center. It talks only to DDC Admin and does not call other components
directly.

## Authentication

The console follows the DDC Admin bearer-token model: paste an admin Bearer
Access Token into the login page; the token is kept in `sessionStorage` only
and is never written into the URL or sent to the server side of the browser
session. A 401 response clears the token and returns to the login page.

## Development

```bash
npm ci
npm run typecheck
npm test -- --run
npm run lint
npm run build
```

`npm run dev` proxies `/api` to a local DDC Admin at
`http://127.0.0.1:18080` (override with `DDC_ADMIN_PROXY`).

Run `npm run e2e` only with a reachable DDC Admin and a valid token in
`DDC_E2E_TOKEN` (the upstream URL can be overridden with `DDC_E2E_ADMIN_URL`).
The command is not a substitute for the DDC live Maven suite.

## Runtime configuration

The browser calls DDC Admin through the static server's `/api` reverse proxy.
Set `DDC_ADMIN_API_BASE_URL` to point at the admin backend:

| Variable | Default | Meaning |
|---|---|---|
| `PORT` | `8080` | HTTP port for the static server |
| `DDC_ADMIN_API_BASE_URL` | `http://ddc-admin:18080` | DDC Admin upstream for `/api` |
| `DDC_ADMIN_API_DEVELOPMENT_PLAINTEXT` | `false` | Allow plaintext HTTP upstream; must be explicitly `true` |
| `DDC_ADMIN_API_TLS_CA_PATH` | — | CA file for mTLS upstream (required for `https:` upstream) |
| `DDC_ADMIN_API_TLS_CERTIFICATE_PATH` | — | Client certificate for mTLS upstream |
| `DDC_ADMIN_API_TLS_PRIVATE_KEY_PATH` | — | Client private key for mTLS upstream |

Keep credentials out of committed `.env` files. TLS termination and the DDC
Admin authorization policy remain deployment responsibilities.

## Deployment

```bash
docker build -t egon-cola/ddc-admin-web .
docker run --rm -p 8080:8080 \
  -e DDC_ADMIN_API_BASE_URL=http://ddc-admin:18080 \
  -e DDC_ADMIN_API_DEVELOPMENT_PLAINTEXT=true \
  egon-cola/ddc-admin-web
```

Health check: `GET /healthz` returns `ok`.
