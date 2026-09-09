# RBAC3 API and Resource Manifest Contract

## 1. Common HTTP contract

- Base path: `/api/rbac3/v1`.
- IDs are decimal strings on JSON/TypeScript boundaries. Clients must not coerce
  them to JavaScript `number`.
- Success envelope: `{ "data": ..., "meta": { "requestId": "...", "traceId": "...", "timestamp": "..." } }`.
- Error envelope contains stable `code`, safe `message`, `traceId`, `timestamp`
  and optional field violations; it never contains a stack trace or secret.
- Tenant comes from the verified principal. Only platform administration routes
  accept `X-RBAC3-Target-Tenant`, and the caller needs the platform permission.
- Mutation commands use `If-Match`/expected version and `Idempotency-Key` where
  declared. A repeated key with a different request hash is rejected.
- Unknown JSON properties and malformed enum values are rejected.

## 2. Authentication boundary and role activation

RBAC3 exposes no login, Refresh, logout, JWKS, authorization-code or personnel
Session endpoint. Browser and API authentication are handled by IdP through the
Gateway. Every RBAC3 request receives an IdP-signed USER Access Token and RBAC3
locally verifies it with the IdP Starter; RBAC3 never reads a Refresh Token.

| Method | Path                                            | Semantics                                                      |
|--------|-------------------------------------------------|----------------------------------------------------------------|
| GET    | `/api/v1/auth/bootstrap`                        | Current identity-facing RBAC3 bootstrap and activation summary |
| GET    | `/api/rbac3/v1/auth/role-activation-candidates` | Assigned canonical roots and explanations                      |
| GET    | `/api/rbac3/v1/auth/role-activations`           | Current complete user-level active-root set                    |
| PUT    | `/api/rbac3/v1/auth/role-activations`           | Atomically replace the complete user-level active-root set     |

Role activation does not accept a login or Session ID. It accepts multiple role IDs, maps
children to roots, expands whole families, and rejects same-APP mutually
exclusive roots before mutation.

## 3. Tenant and directory

| Method | Path | Semantics |
| --- | --- | --- |
| GET/POST | `/platform/tenants` | List or create tenants from platform context |
| GET | `/platform/tenants/{tenantId}` | Platform tenant detail |
| PUT | `/platform/tenants/{tenantId}/status` | Tenant status transition with version check |
| GET | `/users` | Tenant-filtered user list |
| GET | `/users/{userId}` | Tenant user detail |
| PUT | `/users/{userId}/status` | User status transition with version check |
| GET | `/org-units` | Directory organization-unit view |
| GET | `/positions` | Directory position view |
| POST | `/internal/directory-snapshots` | Trusted provider snapshot submission |
| GET | `/directory-snapshots/{snapshotId}` | Immutable snapshot receipt/detail |

Target-tenant headers are never honored on ordinary tenant business routes.

## 4. Application, Manifest, role and constraints

| Area | Routes |
| --- | --- |
| Application/resource | `GET /applications`, `GET /applications/{id}/resources`, `POST /resources/{id}/archive` |
| Manifest | `POST /internal/resource-manifests`, `GET /resource-manifests/{id}`, `GET /resource-manifests/{id}/validation`, `POST /resource-manifests/{id}/impact-analysis`, `POST /resource-manifests/{id}/activate` |
| Role graph/permission | `GET/POST /roles`, `PUT /roles/{id}`, permission batch add/remove, inheritance add/remove, impact analysis |
| Constraints | SOD sets, prerequisite groups, cardinality, Data Rules, Field Rules and Operation-SOD Rules under their `/api/rbac3/v1` collections |

Manifest submission is immutable. Activation checks definition identity,
application/current-manifest versions and idempotency, then atomically changes
the active definition. Missing resources become stale first; archival is an
explicit, audited action.

## 5. Assignment and delegated management

Assignments are qualification records, not active roles. Routes under
`/users/{userId}/role-assignments` support list, create, suspend, resume and
revoke. Mutations require version/idempotency evidence and enforce SSD,
prerequisites, cardinality, target scope and privileged/self-assignment rules.

Management Policy routes are `/management-policies`, detail/update/disable,
`/management-capabilities/me`, `/manageable-users` and `/manageable-roles`.
One complete policy must authorize Subject, target Scope, role Root and
Operation. Fragments from multiple policies are never combined.

## 6. Decision, audit and runtime

| Method | Path                                           | Semantics                                           |
|--------|------------------------------------------------|-----------------------------------------------------|
| POST   | `/internal/authorization/decisions`            | Typed Function/Data/Field decision                  |
| GET    | `/internal/v1/authorization/snapshots/current` | Internal immutable user authorization snapshot      |
| POST   | `/internal/authorization/fences/verify`        | Verify mutation Fence state                         |
| POST   | `/internal/business-participations`            | Append idempotent participation fact                |
| GET    | `/internal/business-participations/conflicts`  | Operation-SOD conflict evidence                     |
| GET    | `/audit-logs`                                  | Redacted, signed-cursor audit query                 |
| POST   | `/simulations/authorization`                   | Consistent-snapshot decision simulation             |
| POST   | `/simulations/role-change-impact`              | Role-change impact simulation                       |
| GET    | `/runtime/status`                              | Runtime subsystem status                            |
| GET    | `/runtime/mutations`                           | Bounded mutation journal query                      |
| POST   | `/runtime/mutations/{id}/retry`                | Retry one stable FAILED mutation ID                 |
| GET    | `/runtime/gateway-ddc-status`                  | Separate Definition, Lease and Release observations |

Internal routes require service identity plus exact tenant/APP boundaries; they
are not alternate public administration routes.

## 7. Resource Manifest v1

Submission body:

```json
{
  "applicationId": "9007199254740993",
  "definitionSetId": "definition-set-20260730",
  "manifest": {
    "schemaVersion": "rbac3-resource-manifest/v1",
    "applicationCode": "orders",
    "applicationName": "Order Management",
    "artifactVersion": "2.4.0",
    "buildId": "git-abcdef12",
    "manifestVersion": 17,
    "generatedAt": "2026-07-30T10:00:00Z",
    "checksum": "sha256:canonical-content",
    "apps": [],
    "menus": [],
    "routes": [],
    "actions": [],
    "apis": [],
    "fieldDefinitions": []
  }
}
```

Resource kinds use stable `code`, optional `parentCode`, display metadata,
route/component information, required permission, and Gateway operation
identity for APIs. Mechanical identity fields cannot change in place.

Field definitions bind `resourceCode + fieldCode` to a JSON path, data type,
sensitivity, default access, optional masking strategy, write/export flags.
Sensitive fields default to no access. Masking is performed server-side; clients
must not receive a raw value and then hide it visually.

The checksum is calculated from a canonical, stable-key serialization excluding
transport-only metadata. A duplicate `definitionSetId` with different content
or checksum is rejected.

## 8. Gateway discovery

Every externally or internally routed controller operation has an explicit,
stable `@GatewayOperation` name. RBAC3 Admin reports the complete definition set
to Gateway Admin and separately registers `HTTP_PROVIDER` in DDC. Gateway Engine
routes the explicit Release using DDC instances. Definition acknowledgement,
provider lease, Release publication and actual routing are separate acceptance
facts and must be recorded separately.
