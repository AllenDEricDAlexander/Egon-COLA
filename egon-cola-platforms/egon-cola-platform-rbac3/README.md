# Egon COLA RBAC3 Permission Platform

RBAC3 is a tenant-isolated permission control plane and runtime enforcement
system. It manages applications, resource manifests, role DAGs, assignments,
constraints, active role sets, authorization snapshots, audit evidence, and
Gateway/DDC publication. The implementation lives under `egon-cola-platforms`
and is delivered as one system, not as staged feature subsets.

Chinese documentation: [README.zh-CN.md](README.zh-CN.md).

## Product semantics

- Authorization changes do **not** use an approval workflow. Authentication,
  authorization, governance, idempotency, and audit are direct system concerns.
- Duty rotation and shift scheduling are business-domain concepts. RBAC3 does
  not model either workflow; it exposes role activation as a semantic API.
- Login establishes identity and creates a Session with no selected role. It
  never asks the user to choose a role.
- A Session may activate multiple roles. Selecting any role activates its
  canonical top-level role and the entire descendant family, so authorization
  uses the union of all selected root families.
- Mutually exclusive roots in the same APP cannot be active in one Session.
  RBAC3 rejects the whole replacement atomically because an ambiguous APP
  context cannot be authorized safely.
- RBAC3 has no separate test module. Each Maven module owns its tests under
  `src/test`; cross-module verification is kept in the closest owning module.

## Modules

| Module | Responsibility | Dependency boundary |
| --- | --- | --- |
| `contract` | Stable DTOs, enums, manifest and decision contracts | No Spring runtime or persistence |
| `core` | Role graph, activation algebra, constraints and pure policies | No I/O, HTTP, Redis, JPA or Admin |
| `starter` | Business-service PEP, reference JWT validation and snapshot reads | Never depends on Admin |
| `gateway-adapter` | Gateway hot-path authentication and authorization | Never calls Admin over HTTP and never queries SQL |
| `admin` | Control plane, authentication, persistence, projection workers, DDC/Gateway registration | Server-only; never imported by Starter |
| `react-sdk` | Typed process-memory auth state and UI integration primitives | No browser-persistent credentials |
| `admin-web` | Permission-filtered administration UI | Static Vite application; local component registry only |

There is no `rbac3-test` artifact and no aggregate runtime library.

## Runtime flow

1. A trusted provider submits an immutable resource Manifest.
2. An operator validates impact and activates the Manifest atomically.
3. Role/permission/constraint mutations append a mutation journal record and
   establish a fail-closed Fence.
4. Projection workers build immutable Session authorization snapshots in the
   dedicated runtime Redis client; the Fence opens only after projection.
5. Starter or Gateway verifies the reference JWT, exact Session/User/Tenant and
   policy versions, then applies Function, Data, Field and Participation rules.
6. Admin reports interface Definitions to Gateway Admin, registers its
   `HTTP_PROVIDER` lease in DDC, and observes the explicit Gateway Release.
7. Gateway obtains provider instances from DDC and routes only when Definition,
   Lease, Release, consistency, Session snapshot and Fence checks all agree.

See [architecture.md](docs/architecture.md) for algorithms and design patterns,
[api-and-manifest.md](docs/api-and-manifest.md) for contracts,
[security-boundaries.md](docs/security-boundaries.md) for trust boundaries, and
[operations-runbook.md](docs/operations-runbook.md) for deployment.

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
