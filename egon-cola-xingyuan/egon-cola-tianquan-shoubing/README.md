# Egon COLA Tianquan-Shoubing (Unified Identity Provider)

The Tianquan-Shoubing is the single authority for workforce identities, credentials, browser
SSO, OAuth clients, administrator-provisioned app IDs and one-time client
Secrets, tenant catalog/memberships, authorization codes, access tokens,
refresh-token families, signing keys, and identity-security audit events.

## Boundaries

- `core` contains stable identity contracts and pure identity, OAuth, and token
  policies behind ports, with no Spring or I/O.
- `rpc-contract` carries the stable Egon RPC contracts (`identity_directory.proto`) for
  internal Tianquan-Shoubing capabilities.
- `starter` verifies USER and SERVICE access tokens and reads Resource/Client state from
  Redis. USER validation is fully stateless and carries no session claims.
- `gateway-adapter` implements identity-only Yuheng security capabilities.
- `admin` owns persistence, OAuth HTTP endpoints, Tianshu/Yuheng integration, and
  the executable Tianquan-Shoubing control plane.
- `admin-web` is the React administration and login/consent application. It is a plain
  Node project and not a Maven child of this reactor.

The Tianquan-Shoubing owns tenant catalog and membership facts. Tianquan-Jianshen owns roles, permissions,
data scopes, field policies, policy snapshots, and authorization versions; it
keeps only external-tenant authorization state and never provides tenant or
membership CRUD. USER access tokens carry stable identity and authentication-context
claims only; SERVICE access tokens additionally carry the target Resource, the
authorization context, and the credential and token-security claims.

For the operator cutover, follow
[`unified-identity-oauth-client-tenant-cutover.md`](../../docs/runbooks/unified-identity-oauth-client-tenant-cutover.md).

See
[`docs/superpowers/specs/2026-08-01-unified-identity-platform-design.md`](../../docs/superpowers/specs/2026-08-01-unified-identity-platform-design.md)
for the approved requirements and architecture.

## OAuth Resource and service-client configuration

One Resource Server is the exact logical triple
`bizCode + appCode + environment`, and the triple maps to exactly one absolute
Resource URI. For example, `permission + tianquan-shoubing + prod` maps to
`https://api.egon.internal/prod/permission/tianquan-shoubing`. Instances are short-lived
runtime facts below that logical Resource; they are never approved one by one.
OAuth authorization requests use the RFC 8707 `resource` parameter and every
access token has exactly one `aud` value equal to that URI.

Provision a Resource and its service clients in this order:

1. Apply Tianquan-Shoubing V6 and the compatible Tianshu/RBAC migrations before deploying code
   that requires the new contracts.
2. Create the Resource Server and its exact business/application/environment
   identity, then enable it.
3. A Tianquan-Shoubing administrator creates each Confidential Client, confirms its `appId`
   and `client_id`, and returns a client Secret once. Store that Secret only in
   the consumer's Secret Manager and rotate it through the Tianquan-Shoubing Admin Web.
4. Add USER grants and the Tianquan-Jianshen application-entry permission. Add SERVICE
   grants for an exact source Client, target Resource, explicit `TENANT` or
   `PLATFORM` context, and allowed-scope set.
5. Configure consumers with Spring Security OAuth2 Client
   `client_credentials`/`client_secret_basic`; Tianshu registration uses a
   Tianshu-audience `PLATFORM` SERVICE token and no second registration credential.

Representative access-token claims, registered claims such as `iss`, `jti`,
`iat`, `nbf`, and `exp` elided, identifiers shortened and all credentials
omitted, are:

```json
{"sub":"user-1","tid":"tenant-1","acr":"MFA","auth_time":1760000000,"principal_type":"USER","aud":["https://api.egon.internal/prod/permission/tianquan-shoubing"]}
{"sub":"service-client-1","tid":"tenant-1","client_id":"service-client-1","app_id":"app-1","scope_context":"TENANT","scope":["tianquan-jianshen:policy:read"],"source_biz":"permission","source_app":"tianquan-shoubing","source_env":"prod","credential_id":"cred-1","resource_version":9,"principal_type":"SERVICE","aud":["https://api.egon.internal/prod/permission/tianquan-jianshen"]}
```

`tid` appears on a SERVICE token only when `scope_context` is `TENANT`; a
`PLATFORM` token omits it. `acr` is one of `PASSWORD`, `MFA`, or `STRONG`.
USER tokens deliberately contain no roles,
permissions, data scopes, field policies, or service scopes, and the starter
verifier rejects session and revocation claims outright (`sid`, `session_id`,
`client_id`, `token_version`, `resource_version`, `nonce`, and the authorization
projections). Tianquan-Jianshen decides whether a user may enter the target
application before issuance and enforces operation/data/field permission in the
downstream service. SERVICE token target, tenant, and scope authorization is
owned entirely by Tianquan-Shoubing Service Grants; token issuance never calls Tianquan-Jianshen and no
refresh token is issued for `client_credentials`.

Disabling a Resource stops new user and service tokens and emits a Tianshu
revocation event for only the matching triple. Recovery requires re-enabling the
Resource and required grants, then allowing instances to obtain a fresh
SERVICE token and lease. Rollback is forward-fix only after the migrations are
applied: old binaries that depend on removed client-key or registration
contracts are not compatible, and existing Flyway files must not be edited or
down-migrated.
