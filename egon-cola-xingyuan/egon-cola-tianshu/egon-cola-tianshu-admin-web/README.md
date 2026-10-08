# Tianshu Admin Web

[中文](README.zh-CN.md) | [Tianshu overview](../README.md)

Tianshu Admin Web is the standalone management console for the Egon COLA Dynamic
Config Center. It talks only to Tianshu Admin and does not call other components
directly.

## Authentication

The console signs in through the platform unified identity using
`createGatewayAuthClient` from `@egon-cola/xingyuan-admin-web-shared`. Login navigates to the
configured Edge OIDC registration (`/oauth2/authorization/edge-ui`). Edge keeps OAuth tokens
server-side and gives the browser an HttpOnly session cookie; the browser never submits a
tenant ID, username or password and never stores AT/RT.

The console therefore holds no access token and sends no `Authorization` header; API calls
use the Edge session and its BFF token relay. Identity and permissions come from
`GET /api/v1/tianshu/auth/bootstrap`, and the console is authorized only when the
response grants `TIANSHU_READ`. A 401 clears the local bootstrap state and returns to
the login page.

## Development

```bash
npm ci
npm run typecheck
npm test -- --run
npm run lint
npm run build
```

`npm run dev` binds `http://127.0.0.1:18152` (`strictPort`) and proxies `/oauth2`, `/bff`, and
`/login/oauth2` to the Edge gateway, and `/api` to the gateway that fronts Tianshu Admin. Override the
upstreams independently with `TIANSHU_AUTH_PROXY` and `TIANSHU_ADMIN_PROXY`; configure the
auth proxy to the Edge instance. `npm run preview` reuses the same proxy table.

The registry page sends an exact four-part scope on its first request. Set its
build-time defaults when the local scope differs from `default / default-app /
dev / default`:

```bash
VITE_TIANSHU_ADMIN_DEFAULT_BIZ_CODE=retail \
VITE_TIANSHU_ADMIN_DEFAULT_APP_CODE=orders \
VITE_TIANSHU_ADMIN_DEFAULT_ENV=local \
VITE_TIANSHU_ADMIN_DEFAULT_NAMESPACE=default \
npm run dev
```

`npm run e2e` builds the console and serves `npm run preview` on `http://127.0.0.1:4173`.
The current auth scenario mocks the OIDC entry and does not prove a live Edge, authorization
server, Redis session or Tianshu Admin runtime.

## Runtime configuration

The browser calls Tianshu Admin through the static server's `/api` reverse proxy.
Set `TIANSHU_ADMIN_API_BASE_URL` to point at the admin backend:

| Variable | Default | Meaning |
|---|---|---|
| `PORT` | `8080` | HTTP port for the static server |
| `TIANSHU_ADMIN_API_BASE_URL` | `http://tianshu-admin:18080` | Tianshu Admin upstream for `/api` |
| `TIANSHU_ADMIN_API_DEVELOPMENT_PLAINTEXT` | `false` | Allow plaintext HTTP upstream; must be explicitly `true` |
| `TIANSHU_ADMIN_API_TLS_CA_PATH` | — | CA file for mTLS upstream (required for `https:` upstream) |
| `TIANSHU_ADMIN_API_TLS_CERTIFICATE_PATH` | — | Client certificate for mTLS upstream |
| `TIANSHU_ADMIN_API_TLS_PRIVATE_KEY_PATH` | — | Client private key for mTLS upstream |

The static server itself fails fast on an `http:` upstream unless the plaintext switch is
`true`, but the shipped `Dockerfile` already bakes
`TIANSHU_ADMIN_API_DEVELOPMENT_PLAINTEXT=true` into the image for local use; unset or
override it in any environment with real TLS.

Keep credentials out of committed `.env` files. TLS termination and the Tianshu
Admin authorization policy remain deployment responsibilities.

## Scope model

The scope hierarchy is biz (business domain) → app → namespace → env. The
registry identity is always biz-ns-env-app; business domains, applications,
namespaces and environments are managed entities with their own pages, and
disabling any of them rejects new registrations and configuration pulls for
that scope (`TIANSHU_SCOPE_DISABLED`).

The scope filters are selectable dropdowns loaded from the backend: the
business domain list comes from `/bizs`, the application list is filtered by
the selected domain, the namespace list by the selected application, and the
environment list comes from the managed `/envs` entity. Every select also
accepts typed values for new entries. Registry queries always use a complete
four-part scope; the build-time defaults above initialize the first query.

## Server-side pagination

Management tables call the additive `/page` endpoints and read
`PageResultRecord.records` plus `PageResultRecord.page`. Page numbers start at
1; the UI offers 10, 20, or 50 rows per page. Scope selectors and namespace
binding editors intentionally keep using the legacy list endpoints because
they require complete option sets.

Registry tables page service keys and lazily page instances for one selected
service. Tianshu Starter and RPC clients continue to consume complete catalogs and
snapshots; Admin Web pagination is not part of the machine RPC contract.

## Configuration resource contract

The configuration page manages complete YAML resources instead of independent
key-value items. Create requests submit `resourceName=application.yml`,
`format=YAML`, and the complete `content`. The backend also accepts
`application.yaml`, and the list renders the actual resource name and format from
the response. Each `bizCode + env + appCode` owns at most one YAML resource;
namespace bindings control visibility only.

This is a breaking contract. The console no longer sends or reads legacy fields
such as `configKey`, `configValue`, `valueType`, or `contentChecksum`.

## Deployment

```bash
docker build -t egon-cola/tianshu-admin-web .
docker run --rm -p 8080:8080 \
  -e TIANSHU_ADMIN_API_BASE_URL=http://tianshu-admin:18080 \
  -e TIANSHU_ADMIN_API_DEVELOPMENT_PLAINTEXT=true \
  egon-cola/tianshu-admin-web
```

Health check: `GET /healthz` returns `ok`.
