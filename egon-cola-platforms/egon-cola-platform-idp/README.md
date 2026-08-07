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
