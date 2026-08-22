# Egon COLA RBAC3 Permission Platform

RBAC3 is a tenant-isolated permission control plane and runtime enforcement
system. It manages applications, resource manifests, role DAGs, assignments,
constraints, active role sets, authorization snapshots, audit evidence, and
Gateway/DDC publication while retaining only authorization state for each
external tenant ID. IdP owns the tenant catalog and identity-sub memberships;
RBAC3 does not create or administer those facts.

Chinese documentation: [README.zh-CN.md](README.zh-CN.md).

## Product semantics

- Authorization changes do **not** use an approval workflow. Authentication,
  authorization, governance, idempotency, and audit are direct system concerns.
- Duty rotation and shift scheduling are business-domain concepts. RBAC3 does
  not model either workflow; it exposes role activation as a semantic API.
- IdP establishes identity and issues the USER Access/Refresh pair. RBAC3
  never creates or manages a personnel Session and never asks the user to
  choose a role during login.
- A user-level active-role set may contain multiple roles. Selecting any role activates its
  canonical top-level role and the entire descendant family, so authorization
  uses the union of all selected root families.
- Mutually exclusive roots in the same APP cannot be active in one user-level set.
  RBAC3 rejects the whole replacement atomically because an ambiguous APP
  context cannot be authorized safely.
- RBAC3 has no separate test module. Each Maven module owns its tests under
  `src/test`; cross-module verification is kept in the closest owning module.

## Modules

| Module            | Responsibility                                                                         | Dependency boundary                                    |
|-------------------|----------------------------------------------------------------------------------------|--------------------------------------------------------|
| `contract`        | Stable DTOs, enums, manifest and decision contracts                                    | No Spring runtime or persistence                       |
| `core`            | Role graph, activation algebra, constraints and pure policies                          | No I/O, HTTP, Redis, JPA or Admin                      |
| `starter`         | Business-service PEP, reference JWT validation and snapshot reads                      | Never depends on Admin                                 |
| `gateway-adapter` | Gateway hot-path authentication and authorization                                      | Never calls Admin over HTTP and never queries SQL      |
| `admin`           | Authorization control plane, persistence, projection workers, DDC/Gateway registration | Server-only; never imported by Starter                 |
| `react-sdk`       | Typed process-memory auth state and UI integration primitives                          | No browser-persistent credentials                      |
| `admin-web`       | Permission-filtered administration UI                                                  | Static Vite application; local component registry only |

There is no `rbac3-test` artifact and no aggregate runtime library.

## Runtime flow

1. A trusted provider submits an immutable resource Manifest.
2. An operator validates impact and activates the Manifest atomically.
3. Role/permission/constraint mutations append a mutation journal record and
   establish a fail-closed Fence.
4. Projection workers build immutable user authorization snapshots in the
   dedicated runtime Redis client; the Fence opens only after projection.
5. Starter or Gateway verifies the IdP USER JWT, asks the IdP membership
   directory for the exact subject/tenant fact, and applies policy-version,
   Function, Data, Field and Participation rules.
6. Admin reports interface Definitions to Gateway Admin with the exact DDC v3
   `bizCode + appCode` identity, registers its `HTTP_PROVIDER` lease in DDC,
   and observes the explicit Gateway Release.
7. Gateway obtains provider instances from DDC and routes only when Definition,
   Lease, Release, consistency, authorization snapshot and Fence checks all agree.

For the operator migration and restore order, follow the
[unified identity cutover runbook](../../docs/runbooks/unified-identity-oauth-client-tenant-cutover.md).

## OAuth Resource authorization boundary

RBAC3 owns USER authorization only. Before IdP issues or refreshes a USER token
for an exact `bizCode + appCode + environment` Resource, the IdP directory and
RBAC3 application-entry policy confirm the user's tenant membership. After authentication,
the downstream Starter applies the user's interface, data, field, participation,
and active-role policies. USER tokens contain identity claims only, so permission
changes remain effective through RBAC3's snapshot and fence rules.

RBAC3 does not own SERVICE principals, service grants, or service scopes. IdP
authorizes `client_credentials` against an exact source Client, target Resource,
tenant, and scope set. A verified SERVICE request is accepted locally only when
its token contains the operation's required IdP scope; it never enters the RBAC3
user decision path. Thus a user allowed into `permission/idp@prod` cannot obtain
a token for `permission/rbac3@prod` unless that target's separate RBAC3 entry
decision also succeeds, while service-to-service access is governed exclusively
by IdP Service Grants.

RBAC3 Admin obtains its DDC-audience PLATFORM SERVICE token through Spring
OAuth2 Client using the IdP-administered app ID and one-time Secret. DDC binds
the token audience, scope, source, instance, replay state, and lease expiry; no
second registration credential is used. Apply IdP V5 and the compatible DDC/RBAC
release together. Roll back with a coordinated forward fix because old
service-permission and unauthenticated-registration paths are intentionally
removed.

See [architecture.md](docs/architecture.md) for algorithms and design patterns,
[api-and-manifest.md](docs/api-and-manifest.md) for contracts,
[security-boundaries.md](docs/security-boundaries.md) for trust boundaries, and
[operations-runbook.md](docs/operations-runbook.md) for deployment.

## DDC configuration and Gateway service integration

Configuration scope and service scope are different identities:

- Configuration resource identity is `bizCode + appCode + env + resourceName`;
  namespace bindings control visibility but are not part of that identity.
  DDC owns the `CONFIG_CLIENT` lease, while RBAC3 consumes the YAML policy
  document and
  accepts only validated monotonically versioned snapshots.
- Service scope is `bizCode + appCode + env + namespace + serviceKind + protocol
  + serviceName + group + version`. RBAC3 uses a separate `HTTP_PROVIDER` lease;
  the provider obtains it from DDC with a PLATFORM SERVICE token and Gateway
  obtains unexpired instances from this scope.

The two leases may share an instance ID but never a lease credential or state.
At startup, the configuration client must hold a `CONFIG_CLIENT` lease and be
`READY` before the root HTTP server is published as an `HTTP_PROVIDER`. Interface
Definition reporting is independent of both leases. Spring MVC mappings plus the
existing Gateway annotations feed the Gateway Interface Catalog, which is the
only API document center. A Gateway Release is always an explicit operator action;
RBAC3 never auto-publishes one.

| DDC key | Default | Accepted range |
| --- | ---: | ---: |
| `rbac3.maximum-active-roots` | 16 | 1..32 |

The key controls only the maximum number of active role roots. IdP owns the
five-minute USER Access Token and stable Refresh Token lifecycles. There are no
RBAC3 token/session timeout keys and no cross-key timeout publication.

An accepted update atomically replaces one complete in-memory policy snapshot.
It affects only newly executed role-activation commands. IdP token issuance and
Refresh Token revocation remain outside RBAC3; already committed active-role
sets are not rewritten.
Invalid values produce a failed ACK while the last-known-good policy and DDC
repository metadata remain active; recovery requires a higher valid version.

Operational routeability is five independent facts: DDC Config Client,
Gateway Definition, unexpired DDC HTTP Provider lease, explicit Gateway
Release/engine consistency, and an observed routed request. Status and metrics
expose bounded versions, state, fingerprints, and error codes only—never raw
configuration values, lease credentials, passwords, tokens, private keys, hashes,
or bootstrap administrator secrets.

## Build and test

Requirements: Java 21, the Maven Wrapper, and Node 24 from `.node-version`.

```bash
./mvnw -B -ntp \
  -pl :egon-cola-platform-rbac3-contract,:egon-cola-platform-rbac3-core,:egon-cola-platform-rbac3-starter,:egon-cola-platform-rbac3-gateway-adapter,:egon-cola-platform-rbac3-admin,:egon-cola-platform-gateway-engine \
  -am clean verify

cd egon-cola-platforms/egon-cola-platform-rbac3
npm ci
npm run typecheck
npm test -- --run
npm run lint
npm run build
npm run e2e --workspace @egon-cola/rbac3-admin-web -- --list
```

The E2E listing command discovers scenarios without opening a browser. The
repository does not automatically start RBAC3, Gateway, DDC, PostgreSQL, Redis,
or the Admin Web.

## Verification tooling

All scripts are opt-in. `--help` and `--check-config` are read-only and do not
contact external services.

```bash
scripts/verification/verify-static.sh --verify
scripts/verification/verify-local-dependencies.sh --check-config
scripts/verification/prepare-rbac3-fixture.sh --check-config
scripts/verification/verify-gateway-ddc-topology.sh --check-config
scripts/verification/cleanup-rbac3-fixture.sh --check-config
```

Live topology verification requires two operator-started Admin instances with
distinct ports, instance IDs, build IDs, and Snowflake machine IDs. It pauses
for the operator to perform stop/restore actions and never changes process
state itself. External topology evidence is deliberately distinct from unit,
module-integration, static, and host-local dependency evidence.

## Delivery boundary

CI proves compilation, tests, frontend checks, packaging, and static boundaries.
It does not claim that a particular external PostgreSQL/Redis/DDC/Gateway
deployment is healthy. Record live evidence using
[verification-evidence-template.md](docs/verification-evidence-template.md).
