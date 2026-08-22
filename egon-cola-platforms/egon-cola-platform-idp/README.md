# Egon COLA Unified Identity Provider

The IdP is the single authority for workforce identities, credentials, browser
SSO, OAuth clients, administrator-provisioned app IDs and one-time client
Secrets, tenant catalog/memberships, authorization codes, access tokens,
refresh-token families, signing keys, and identity-security audit events.

## Boundaries

- `core` contains stable identity contracts and pure identity, OAuth, and token
  policies behind ports, with no Spring or I/O.
- `starter` verifies IdP tokens and global user state in downstream services.
- `gateway-adapter` implements identity-only Gateway security capabilities.
- `admin` owns persistence, OAuth HTTP endpoints, DDC/Gateway integration, and
  the executable IdP control plane.
- `admin-web` is the React administration and login/consent application.

The IdP owns tenant catalog and membership facts. RBAC3 owns roles, permissions,
data scopes, field policies, policy snapshots, and authorization versions; it
keeps only external-tenant authorization state and never provides tenant or
membership CRUD. Access tokens contain stable identity, target, context, and
token-security claims.

For the operator cutover, follow
[`unified-identity-oauth-client-tenant-cutover.md`](../../docs/runbooks/unified-identity-oauth-client-tenant-cutover.md).

See
[`docs/superpowers/specs/2026-08-01-unified-identity-platform-design.md`](../../docs/superpowers/specs/2026-08-01-unified-identity-platform-design.md)
for the approved requirements and architecture.

## OAuth Resource and service-client configuration

One Resource Server is the exact logical triple
`bizCode + appCode + environment`, and the triple maps to exactly one absolute
Resource URI. For example, `permission + idp + prod` maps to
`https://api.egon.internal/prod/permission/idp`. Instances are short-lived
runtime facts below that logical Resource; they are never approved one by one.
OAuth authorization requests use the RFC 8707 `resource` parameter and every
access token has exactly one `aud` value equal to that URI.

Provision a Resource and its service clients in this order:

1. Apply IdP V5 and the compatible DDC/RBAC migrations before deploying code
   that requires the new contracts.
2. Create the Resource Server and its exact business/application/environment
   identity, then enable it.
3. An IdP administrator creates each Confidential Client, confirms its `appId`
   and `client_id`, and returns a client Secret once. Store that Secret only in
   the consumer's Secret Manager and rotate it through the IdP Admin Web.
4. Add USER grants and the RBAC3 application-entry permission. Add SERVICE
   grants for an exact source Client, target Resource, explicit `TENANT` or
   `PLATFORM` context, and allowed-scope set.
5. Configure consumers with Spring Security OAuth2 Client
   `client_credentials`/`client_secret_basic`; DDC registration uses a
   DDC-audience `PLATFORM` SERVICE token and no second registration credential.

Representative access-token claims, with identifiers shortened and all
credentials omitted, are:

```json
{"sub":"user-1","tid":"tenant-1","sid":"session-1","client_id":"web-1","principal_type":"USER","token_version":7,"resource_version":9,"aud":["https://api.egon.internal/prod/permission/idp"]}
{"sub":"service-client-1","tid":"tenant-1","client_id":"service-client-1","principal_type":"SERVICE","scope":["rbac3:policy:read"],"source_biz":"permission","source_app":"idp","source_env":"prod","resource_version":9,"aud":["https://api.egon.internal/prod/permission/rbac3"]}
```

USER tokens deliberately contain no roles, permissions, data scopes, field
policies, or service scopes. RBAC3 decides whether a user may enter the target
application before issuance and enforces operation/data/field permission in the
downstream service. SERVICE token target, tenant, and scope authorization is
owned entirely by IdP Service Grants; token issuance never calls RBAC3 and no
refresh token is issued for `client_credentials`.

Disabling a Resource stops new user and service tokens and emits a DDC
revocation event for only the matching triple. Recovery requires re-enabling the
Resource and required grants, then allowing instances to obtain a fresh
SERVICE token and lease. Rollback is forward-fix only after the migrations are
applied: old binaries that depend on removed client-key or registration
contracts are not compatible, and existing Flyway files must not be edited or
down-migrated.
