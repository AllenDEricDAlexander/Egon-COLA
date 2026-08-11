# Egon COLA Unified Identity Provider

The IdP is the single authority for workforce identities, credentials, browser
SSO, OAuth clients, authorization codes, access tokens, refresh-token families,
signing keys, and identity-security audit events.

## Boundaries

- `core` contains stable identity contracts and pure identity, OAuth, and token
  policies behind ports, with no Spring or I/O.
- `starter` verifies IdP tokens and global user state in downstream services.
- `gateway-adapter` implements identity-only Gateway security capabilities.
- `admin` owns persistence, OAuth HTTP endpoints, DDC/Gateway integration, and
  the executable IdP control plane.
- `admin-web` is the React administration and login/consent application.

The IdP does not own tenants, tenant memberships, roles, permissions, data
scopes, field policies, or RBAC versions. RBAC3 remains the authority for those
facts. Access tokens contain only stable identity and token-security claims.

See
[`docs/superpowers/specs/2026-08-01-unified-identity-platform-design.md`](../../docs/superpowers/specs/2026-08-01-unified-identity-platform-design.md)
for the approved requirements and architecture.

## OAuth Resource Server admission

One Resource Server is the exact logical triple
`bizCode + appCode + environment`, and the triple maps to exactly one absolute
Resource URI. For example, `permission + idp + prod` maps to
`https://api.egon.internal/prod/permission/idp`. Instances are short-lived
runtime facts below that logical Resource; they are never approved one by one.
OAuth authorization requests use the RFC 8707 `resource` parameter and every
access token has exactly one `aud` value equal to that URI.

Provision a Resource in this order:

1. Apply the IdP V2, DDC V8, and Gateway V11 migrations before deploying code
   that requires the new contracts.
2. Create the Resource Server and its exact business/application/environment
   identity, then enable it.
3. Generate each Resource owner key pair in the deployment environment. Keep
   the private key in an owner-only absolute-path file and register only the
   public key and `kid` in IdP. During rotation, publish the new public key,
   deploy the new `kid` and private key, verify admission, and only then disable
   the old key.
4. Add USER client-to-Resource grants and configure the RBAC3 application-entry
   permission. Add SERVICE grants for an exact source Client, target Resource,
   tenant, and allowed-scope set.
5. Configure the Resource URI, triple, instance ID, management Client, `kid`,
   private-key path, admission endpoint, and renewal skew locally, then deploy.

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

Disabling a Resource stops new user tokens, service tokens, and admission
tickets and emits a DDC revocation event for only the matching triple. Recovery
requires re-enabling the Resource, restoring at least one valid public key and
the required grants, then allowing instances to obtain fresh tickets and leases.
Rollback is forward-fix only after the migrations are applied: old binaries that
depend on the removed audience table or unauthenticated DDC contracts are not
compatible, and existing Flyway files must not be edited or down-migrated.

The approved end-to-end decisions and swimlanes are in
[`2026-08-10-oauth2-resource-server-admission-design.md`](../../docs/superpowers/specs/2026-08-10-oauth2-resource-server-admission-design.md).
