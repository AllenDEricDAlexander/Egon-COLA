# RBAC3 Architecture, Algorithms, and Design Patterns

## 1. Architectural view

RBAC3 separates control-plane writes from runtime enforcement:

```text
Admin HTTP/API
  -> application facades and domain services
  -> PostgreSQL facts + mutation journal + outbox
  -> projection workers
  -> immutable Redis authorization snapshots
  -> Starter PEP / Gateway Adapter PEP
```

The Contract module owns wire compatibility. Core owns deterministic policy and
graph algorithms. Admin owns I/O and orchestration. Starter and Gateway Adapter
are consumers of immutable runtime state, not clients of Admin implementation.

The integration control plane is deliberately decomposed:

```text
Definition report receipt  -- proves Gateway knows the interface definition
DDC HTTP_PROVIDER lease    -- proves at least one live provider registration
Gateway Release            -- proves a specific rule set was published
Runtime consistency        -- proves engines observed the explicit release
Routed request             -- proves the complete route works at observation time
```

No single health flag substitutes for these observations.

### 1.1 DDC configuration scope and service scope

DDC configuration and service discovery use separate identities and separate
leases. Configuration scope is:

```text
bizCode + appCode + env + resourceName
```

Namespace bindings control configuration visibility but are not part of the
resource identity. The DDC runtime owns one `CONFIG_CLIENT` lease for this scope.
Service scope is:

```text
bizCode + appCode + env + namespace + serviceKind + protocol
  + serviceName + group + version
```

The Gateway provider runtime owns a different `HTTP_PROVIDER` lease for that
scope. `CONFIG_CLIENT` and `HTTP_PROVIDER` may use the same process instance ID,
but their lease IDs, roles, lifecycle, readiness and recovery state are never
derived from one another. Gateway resolves providers only from the service scope;
it does not use the configuration-client lease as a provider registration.

Startup uses a small state gate rather than another registry implementation:

```text
CONFIG_CLIENT register -> pull/apply snapshots -> DDC READY
  + root WebServerInitializedEvent port
  + ApplicationReadyEvent
  -> publish the existing HTTP_PROVIDER runtime exactly once
```

The gate ignores management web-server events, rejects an explicit advertised
port that differs from the root server port, and fails closed when DDC is not
`READY` or no `CONFIG_CLIENT` lease exists. It delegates registration,
heartbeat, recovery and offline behavior to the existing Gateway provider
runtime.

### 1.2 Atomic runtime policy and document catalog

The exact DDC applier adapts the RBAC3 policy leaf into one immutable policy
snapshot. Each apply is serialized, builds and validates a complete candidate,
then atomically swaps the reference. Readers take one snapshot per command, so
they cannot observe mixed fields. A runtime publication replaces the complete
YAML resource; if any consumer rejects it, DDC rolls back the property source and
already-applied leaves.

The current RBAC3 policy is `rbac3.maximum-active-roots` plus its fixed range.
An invalid candidate records only key, target version and bounded error code.
The active snapshot and DDC repository version/checksum remain at the
last-known-good state. A later higher valid version recovers the key. RBAC3 does
not issue tokens or own personnel sessions; the value governs new role-activation
commands only and never rewrites committed activation facts.

Spring MVC remains the mechanical source for HTTP method, path, media types and
parameters. Existing Gateway annotations add business grouping, stable operation
name, summary, tags, accessibility and schema descriptions. The resulting
Gateway Interface Catalog is the sole API document center. RBAC3 reports a
Definition but never auto-publishes a Gateway Release.

Routeability is evaluated from five facts without collapsing them: DDC Config
Client, accepted Gateway Definition, unexpired HTTP Provider lease, explicit
Release/engine consistency, and routed-request evidence. The first four are
available from the runtime status API; the fifth must come from an actual routed
request observation.

## 2. Role graph model

Roles form a tenant- and APP-scoped directed acyclic graph (DAG). An edge points
from senior role to junior role. A role may have multiple parents only when all
paths resolve to one canonical root; multiple distinct roots are ambiguous and
are rejected for activation.

### 2.1 Graph validation

For a proposed graph mutation:

1. Lock the affected APP graph using the PostgreSQL adapter.
2. Apply the proposed edge to an in-memory candidate graph.
3. Run a color-marked depth-first traversal to reject back edges (cycles).
4. Track path depth and reject paths beyond the configured hard limit.
5. Rebuild closure rows atomically from the validated graph.
6. Increment the role/policy version and append the mutation journal record.

For `V` roles and `E` inheritance edges, cycle/depth validation is `O(V + E)`
with `O(V)` working memory. Closure rebuild materializes reachability for reads;
the implementation sets hard graph limits so a tenant cannot turn validation
into an unbounded traversal.

### 2.2 Canonical root and family expansion

Activation accepts role IDs but authorizes root families:

1. Traverse parent edges from each selected role and collect roots.
2. Require exactly one root for each selected role.
3. Deduplicate roots using stable IDs.
4. Traverse descendant edges from every root, including the root itself.
5. Sort roots and family members by stable ID before checksum generation.

The traversal is `O(Vr + Er)` for the reachable subgraph. Seeded metamorphic
tests permute input and edge order to prove that roots, families, and checksums
do not depend on iteration order.

## 3. Atomic role activation

Role activation is a full-set replacement command:

```text
expected authVersion
  -> user authorization row lock
  -> canonical roots
  -> assignment evidence
  -> disabled/expiry/prerequisite checks
  -> same-APP DSD/mutex check
  -> root count limit
  -> permission/scope/field merge
  -> immutable snapshot
  -> authVersion CAS
  -> Redis projection
```

The same normalized root set is idempotent. A different request using a stale
`authVersion` fails with a conflict. No partial root activation is committed.
If the client loses the response, it reads the current set and compares the
server result before deciding whether another request is necessary.

## 4. Authorization algebra

### 4.1 Function permissions

Function permissions are a set union across all active root families. Union is
commutative, associative, and idempotent, which guarantees stable decisions for
equivalent active sets.

### 4.2 Data scopes

Data scopes are merged by the typed scope merger registered for the resource
kind. A merger returns a canonical scope expression and evidence. It may widen
only according to an explicit rule; an unknown scope type fails closed.

### 4.3 Field access

Field rules use a stable resource/field key. Denial and masking constraints are
preserved during merge; sensitive fields default to `NONE`. Output is sorted by
stable field key so serialization and checksums are deterministic.

### 4.4 Operation SOD and participation

Operation SOD is checked against the effective operation and business object.
Participation facts are append-only, tenant-scoped, serialized by business
event identity, and queried without exposing unrelated object history.

## 5. Runtime consistency and failure behavior

Reference JWTs contain identity claims, not an embedded permission list. The PEP validates:

1. one syntactically valid Bearer credential;
2. trusted RS256 issuer, audience, `kid`, signature and time claims;
3. exact Tenant, IdP subject, authentication context and policy versions;
4. an unexpired immutable authorization snapshot;
5. no closed mutation Fence for the protected scope;
6. typed Function/Data/Field/Participation decision.

Missing state, Redis errors, untrusted keys, version drift, unknown mappings and
rule errors deny access. A bounded last-known-good public-key cache may cover a
temporary key-source failure, but never version or authorization state drift.

The Gateway Adapter performs no blocking Admin HTTP or PostgreSQL call on the
request path. Original credentials are forwarded only after a complete allow,
and never for anonymous, denied, errored, RPC or multiple-credential requests.

## 6. Persistence and delivery

RBAC3 and Transactional Outbox use one PostgreSQL DataSource but separate
Flyway locations and history tables. RBAC3 has one immutable V1 migration; new
schema changes require the next migration and must never rewrite V1.

Authorization mutations and outbox records are written in the business
transaction. Workers claim bounded batches with PostgreSQL locking semantics,
publish deterministic Redis snapshots, persist checkpoints, and open Fences
only after the projection succeeds. Retry is bounded and mutation-addressable;
the UI cannot issue a broad “retry everything” command.

## 7. Selected design patterns

| Pattern            | Where used                                                                  | Why it fits                                                                                                       |
|--------------------|-----------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| Specification      | SSD/DSD, prerequisite, cardinality, self-assignment and operation-SOD rules | Rules are independently testable predicates with explicit evidence and denial codes                               |
| Strategy           | Password authentication, Data Scope merger, Field Rule merger               | These are known variation points; direct conditional chains would couple unrelated algorithms                     |
| Domain Service     | Graph validation, activation resolution, authorization decision             | The operation spans multiple domain facts but does not belong to one entity                                       |
| Facade             | Assignment, Manifest, Management Policy, activation, simulation             | Keeps HTTP orchestration out of domain algorithms and provides one transaction boundary                           |
| Ports and Adapters | Database clock, locks, query stores, Redis projection, DDC/Gateway status   | Core/application code depends on capabilities, while infrastructure details remain replaceable in tests           |
| State              | Mutation/fence status and runtime publication status                        | Transitions and illegal moves are explicit rather than scattered boolean combinations                             |
| Adapter            | Starter PEP, Gateway Adapter and the exact DDC policy applier               | Translates host/configuration contracts into typed policy behavior without duplicating their lifecycle algorithms |
| Observer           | Optional Micrometer apply observer                                          | Adds fixed-cardinality success/failure evidence without making configuration delivery depend on metrics           |
| Immutable Snapshot | Runtime policy and authorization snapshot publication                       | One reference swap gives command-scoped consistency and preserves a last-known-good value on validation failure   |

A general-purpose rule engine, abstract factory hierarchy, event-sourced aggregate,
and chain class per authorization step were rejected. The rule set is typed and
known, transactions are relational, and direct ordered orchestration is clearer
and safer than runtime-configurable indirection.

## 8. Complexity and capacity controls

- Role graph traversal: `O(V + E)` under configured role/edge/depth limits.
- Permission merge: `O(P log P)` from canonical sorting; membership is set based.
- Snapshot lookup: bounded Redis reads, no SQL or Admin HTTP on Gateway hot path.
- Assignment/participation capacity: serialized by stable PostgreSQL lock keys.
- Admin list/audit APIs: bounded cursor/page sizes and query-count tests.
- Workers: bounded batch size and concurrency, SKIP LOCKED ownership, retry state.
- Active roles: maximum active roots and assignment/activation constraints.

Wall-clock assertions are opt-in (`rbac3.performance.enforce`) because shared CI
latency is not a stable performance oracle. Default tests assert deterministic
operation/query budgets; controlled environments may add calibrated time limits.
