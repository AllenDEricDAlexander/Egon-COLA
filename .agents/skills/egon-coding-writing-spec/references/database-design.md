# Per-table and Per-index Database Design

Read this reference whenever Chapter 11 affects persisted data or relies on existing tables for correctness. Use the repository's actual database dialect, migration framework, naming conventions, and access technology.

## Contents

- [Database scope and inventory](#database-scope-and-inventory)
- [Required per-table subsection](#required-per-table-subsection)
- [Database drafting sequence](#database-drafting-sequence)
- [Complete per-table worked example](#complete-per-table-worked-example)
- [Migration decision patterns](#migration-decision-patterns)
- [Depth and consistency gate](#depth-and-consistency-gate)
- [Database review failures](#database-review-failures)

## Database scope and inventory

First identify the database/schema, ownership, migration locations, current version sequence, access layer, and whether the evidence is source-only or verified against a live schema.

List every created, altered, read, or written table:

| Table | Existing/new | Purpose and owner | Read/write paths | Change | Migration | Requirements |
| --- | --- | --- | --- | --- | --- | --- |

Every inventory table must have a detailed subsection. If no database is involved, keep Chapter 11 and write evidence-backed `N/A`.

## Required per-table subsection

### 1. Purpose, ownership, and lifecycle

State the exact schema/table name, business purpose, owning module, authoritative writer, readers, creation/update/archive/delete lifecycle, retention, tenant partitioning, expected row count/growth, and sensitive/audit classification.

### 2. Complete column design

| Column | Native type | Length/precision | Null | Default | Generated | PK/FK/unique/check | Meaning | Source/mapping | Example |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |

Describe every existing column affected by the design and every proposed column. Use database-native types rather than only Java types. Define:

- absence semantics: missing vs `NULL` vs empty vs zero;
- enum/state values and invalid states;
- money/quantity precision and rounding;
- date/time format, time zone, and clock source;
- ID generation, immutability, tenant/audit/version fields;
- FK target and update/delete behavior;
- unique/check constraints and the business rule each enforces;
- PO/ORM Entity and interface-field mapping.

### 3. Keys, relationships, and constraints

Explain primary/candidate/business keys, one-to-one/one-to-many relationships, ownership, optionality, cascade/restrict behavior, orphan handling, and whether referential integrity is database-enforced or application-enforced. Do not add an FK or cascade without checking existing repository policy and data quality.

### 4. Index inventory and detailed justification

Document every retained, added, changed, or removed index:

| Index | Type/unique | Ordered columns/expressions | Predicate/include | Query and operation | Cardinality/selectivity | Sort/coverage role | Write/storage cost | Decision |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |

For each index state:

- exact name and database-specific definition;
- query/mapper/repository method it serves, including filters, joins, sort, grouping, and pagination;
- equality columns before range/sort columns and why the order fits real access patterns;
- uniqueness semantics, including `NULL`, tenant scope, soft delete, or partial-index behavior;
- expected selectivity/cardinality and data-volume evidence;
- whether it covers the query and whether an existing prefix index becomes redundant;
- write amplification, storage, lock/build duration, and online/concurrent creation support;
- planned verification such as generated SQL inspection, `EXPLAIN`, migration test, or representative integration test.

Reject speculative indexes that have no identified query. Also reject a critical query whose access path remains unknown.

### 5. Access patterns and SQL shape

List reads/writes with exact caller and transaction owner:

| Operation | Caller | Predicate/join/order | Expected rows | Index/constraint | Lock/isolation | Failure/idempotency |
| --- | --- | --- | --- | --- | --- | --- |

Include pagination stability, batch size, N+1 risk, optimistic/pessimistic locking, upsert behavior, duplicate handling, and affected-row expectations where applicable.

### 6. Migration and historical data

Follow repository policy. When Flyway migration files are immutable, add exactly one new next-version migration for one database change and never edit, rename, reorder, reformat, or repair an existing migration.

Define:

- exact new migration path/version and dialect;
- DDL/data-change pseudocode in execution order;
- preconditions and current-data profiling needed before rollout;
- default/backfill/nullability sequence and batch/restart behavior;
- compatibility window between old/new application versions;
- index-build and lock risk;
- verification queries and expected results;
- rollback feasibility; when destructive rollback is unsafe, define forward-fix and application rollback limits.

### 7. Transaction, consistency, and recovery

Define transaction owner, isolation/locking, concurrent-write behavior, idempotency/deduplication, cache invalidation, outbox/event relationship, partial-failure handling, audit trail, and reconciliation/repair path.

## Database drafting sequence

Complete database design from evidence outward. Do not start by proposing a table or index name.

1. **Establish the persistence baseline** — identify database dialect/version, schema, migration tool/path/version convention, ORM/mapper technology, naming/type conventions, transaction manager, soft-delete/tenant/audit conventions, and whether a live schema was verified.
2. **Trace data ownership** — identify authoritative writer, readers, interface/model fields, lifecycle/state transitions, retention, expected volume/growth, and sensitive/audit classification.
3. **Inspect current DDL and access paths** — read immutable migrations or schema definitions, PO/Entity mappings, Mapper/DAO SQL, generated-query methods, batch jobs, reports, and existing indexes/constraints. Source definitions and a live schema may differ; label the boundary.
4. **Design columns and constraints** — use native types and state exact absence, default, precision, time, enum, tenant, audit, identity, uniqueness, relationship, and compatibility semantics.
5. **Design from queries to indexes** — write the real query shape first, including equality/range/join/order/group/page behavior; only then select or reject an index and explain ordered keys, selectivity, coverage, overlap, and write/build cost.
6. **Design migration and runtime coexistence** — profile existing data, order expand/backfill/validate/contract steps, define old/new application compatibility, lock duration, batching/restart, verification, rollback boundary, and forward fix.
7. **Cross-check and test** — map every interface/model field to a column or intentional derived source; align transactions, errors, cache/events, frontend behavior, tests, and traceability.

Use this evidence ledger before selecting DDL:

| Concern | Repository/live evidence | Confirmed baseline | Design consequence | Verification limit |
| --- | --- | --- | --- | --- |
| Dialect/version | Build/config/container/migration syntax | PostgreSQL `<version>` | Use native timestamp/index syntax | Config is static; live version unverified |
| Table definition | Migration `V...__create_orders.sql` | `orders` exists with named columns | Add a new migration; never edit predecessor | Live drift not checked |
| Write path | `OrderServiceImpl#create` -> `OrderDao#insert` | One local transaction | Service remains transaction owner | Runtime isolation not measured |
| Query path | `OrderDao#findPage` SQL | tenant/status filter + created/id sort | Index must support this order | `EXPLAIN` pending representative data |

## Complete per-table worked example

This example demonstrates documentation depth only. It uses fictitious PostgreSQL-like names and must be replaced with the repository's actual dialect, schema, fields, constraints, queries, and migration policy.

### Example table inventory

| Table | Existing/new | Purpose and owner | Read/write paths | Change | Migration | Requirements |
| --- | --- | --- | --- | --- | --- | --- |
| `biz.orders` | Existing | Authoritative order header owned by Order module | `OrderDao#insert`, `OrderDao#findById`, `OrderDao#findPage` | Add idempotency identity and optimistic version | `classpath:db/V42__extend_orders_idempotency.sql` | `REQ-007`, `REQ-008` |

### `biz.orders`

#### Purpose, ownership, and lifecycle

- **Purpose**: store the authoritative header and stable creation result for one tenant order; line items remain in the separately designed `biz.order_items` table.
- **Writer**: only the Order module through `OrderServiceImpl`; DAO executes persistence but does not own state-transition policy.
- **Readers**: create retry lookup, detail/list queries, fulfillment integration, audit/report jobs explicitly found in the repository.
- **Lifecycle**: created once, state changes only through documented transitions, no physical delete while retention/audit obligations apply; archive behavior must match current repository policy.
- **Tenant/security**: every business key and query is tenant-scoped; cross-tenant IDs return the repository-defined non-disclosing outcome.
- **Capacity**: record current row count if verified, otherwise estimate only from stated business evidence; state expected daily growth, retention, hot-window, and largest tenant. Do not invent figures.
- **Evidence boundary**: source inspection proves intended schema/access; row counts, skew, bloat, and plans require live/profile evidence.

#### Complete column design

| Column | Native type | Length/precision | Null | Default | Generated | PK/FK/unique/check | Meaning | Source/mapping | Example |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `id` | `bigint` | 64-bit | No | None | Repository ID generator | PK | Immutable internal order ID | `OrderPO.id` -> `data.orderId` | `810001` |
| `tenant_id` | `bigint` | 64-bit | No | None | Security context at write | Included in business uniqueness and every access path | Owning tenant; never supplied by request body | `TenantContext` -> `OrderPO.tenantId` | `2001` |
| `order_no` | `varchar(32)` | 32 chars | No | None | Order-number generator | Unique within tenant via `uk_orders_tenant_order_no` | Immutable user-visible order number | `OrderPO.orderNo` -> response | `O202608170001` |
| `customer_id` | `bigint` | 64-bit | No | None | Request after tenant validation | Application-enforced reference unless repository policy uses FK | Tenant-visible customer identity | request -> `OrderPO.customerId` | `12001` |
| `status` | `varchar(24)` | 24 chars | No | `'CREATED'` only if old/new version compatibility proves safe; otherwise no DDL default | Service | Check/enum policy follows repository | Current lifecycle state; list all allowed transitions | `OrderStatus` | `CREATED` |
| `currency` | `char(3)` | ISO-like code | No | None | Validated request | Check only if repository consistently uses checks | Currency for all order amounts | request -> PO -> response | `CNY` |
| `total_amount` | `numeric(19,2)` | precision 19, scale 2 | No | None | Service calculation | Check `>= 0` only if repository policy/data supports it | Authoritative total in currency units | `BigDecimal`; JSON string | `39.80` |
| `idempotency_key` | `varchar(64)` | 64 ASCII chars | Yes during expand/backfill window; target No for new rows | None | Request header | Tenant-scoped uniqueness after data validation | Stable identity of one create intent | Header -> PO; raw logging prohibited | `01J5...K9` |
| `request_hash` | `char(64)` | SHA-256 hex length | Same rollout semantics as key | None | Canonical request hash | None alone | Detects same-key/different-payload reuse; not a security signature | Service-derived | `a4e...` |
| `version` | `bigint` | 64-bit | No | `0` when compatible with current ORM | ORM/Service increment | Optimistic-lock predicate | Prevents silent concurrent state overwrite | `OrderPO.version` | `3` |
| `created_at` | `timestamptz` | microsecond precision per dialect | No | Repository clock policy | Server/application per existing convention | None | Creation instant, stored with unambiguous offset semantics | PO -> ISO response | `2026-08-17T02:15:30Z` |
| `updated_at` | `timestamptz` | same as `created_at` | No | Repository clock policy | Updated on successful mutation | None | Last committed mutation instant | `OrderPO.updatedAt` | `2026-08-17T02:20:00Z` |

Field semantics that must accompany the table:

- missing `idempotency_key` during a compatibility window is distinct from blank; blank is never a valid new value;
- `total_amount` is stored in currency units at scale 2 in this example; if the repository uses cents or variable currency scale, change database/Java/JSON/frontend rules together;
- state values, valid transitions, terminal states, and behavior for unknown legacy values are defined in the model/state section;
- timestamps represent instants; serialization zone and frontend localization are interface concerns, not separate stored local time;
- tenant, audit, and version fields follow one repository convention; do not add duplicate base fields if inherited persistence mapping already supplies them.

#### Keys, relationships, and constraints

| Key/relationship | Definition | Business rule | Delete/update behavior | Enforcement and evidence |
| --- | --- | --- | --- | --- |
| Primary key | `pk_orders(id)` | Stable internal identity | Immutable | Database PK, current repository convention |
| Business key | `(tenant_id, order_no)` | Order number unique per tenant | Immutable | Unique index/constraint; duplicate maps to documented conflict |
| Idempotency key | `(tenant_id, idempotency_key)` for non-null active keys | One create intent per tenant/key | Retention/expiry policy must not permit unsafe replay | Unique partial/full strategy depends on dialect and compatibility data |
| Customer reference | `(tenant_id, customer_id)` logical relationship | Customer must belong to tenant | Order retention must not cascade-delete | Database FK only if repository policy and current data allow it; otherwise application validation plus audit |
| Items | `orders.id` -> `order_items.order_id` one-to-many | Header owns line lifecycle | No orphan line; deletion/archive follows policy | Constraint/cascade choice must match existing schema and migration evidence |

#### Index inventory and per-index justification

| Index | Type/unique | Ordered columns/expressions | Predicate/include | Query and operation | Cardinality/selectivity | Sort/coverage role | Write/storage cost | Decision |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `pk_orders` | btree unique | `(id)` | None | detail/update by ID with tenant guard | ID highly selective | Lookup; tenant guard still checked | Existing mandatory PK | Retain |
| `uk_orders_tenant_order_no` | btree unique | `(tenant_id, order_no)` | None | tenant detail by order number | Composite unique | Full lookup | One uniqueness check per write | Retain/add only from evidence |
| `uk_orders_tenant_idempotency` | btree unique | `(tenant_id, idempotency_key)` | `WHERE idempotency_key IS NOT NULL` if dialect/data window requires | create retry and conflict lookup | Tenant + key expected unique | Full lookup, not sorting | Additional write/check/storage; concurrent build options assessed | Add after duplicate profiling |
| `idx_orders_tenant_status_created` | btree | `(tenant_id, status, created_at DESC, id DESC)` | Optional include only if dialect and measured benefit justify | `findPage`: tenant equality, optional exact status, newest first, stable ID tie-breaker | Tenant/status selectivity must be profiled | Avoid sort and stabilize pagination for matching query variant | Write amplification on every insert/state update | Add, change, or reject after checking optional-status query and existing prefixes |

Per-index reasoning must include the query shape. For example:

```sql
-- Documentation-only query shape; use the actual Mapper/DAO SQL and dialect.
SELECT id, order_no, status, currency, total_amount, created_at
FROM biz.orders
WHERE tenant_id = :tenantId
  AND status = :status
  AND (created_at, id) < (:cursorCreatedAt, :cursorId)
ORDER BY created_at DESC, id DESC
LIMIT :limit;
```

`tenant_id` and `status` are equality keys before the range/order keys. `id` is the deterministic tie-breaker. If `status` is optional and the generated SQL omits it, prove whether the same index remains useful or a second access path/existing prefix is required; do not assume. Check existing indexes for redundant prefixes before adding anything.

#### Access patterns and SQL shape

| Operation | Caller | Predicate/join/order | Expected rows | Index/constraint | Lock/isolation | Failure/idempotency |
| --- | --- | --- | --- | --- | --- | --- |
| Create | `OrderServiceImpl#create` | insert one header plus N items | 1 header | PK, business key, idempotency unique | One local transaction; isolation from repository | Duplicate key mapped by constraint identity; whole write rolls back |
| Retry lookup | `OrderServiceImpl#create` | tenant + idempotency key | 0 or 1 | `uk_orders_tenant_idempotency` | Define race handling around insert | Same hash returns stored outcome; different hash conflicts |
| Detail | `OrderServiceImpl#get` | tenant + ID | 0 or 1 | PK plus tenant predicate | Read policy | Cross-tenant/missing use same documented result if non-disclosure required |
| Page | `OrderServiceImpl#page` | tenant + optional status + stable sort/cursor | 0 to page size | page index or proven alternative | Read consistency documented | Concurrent inserts may appear only on later refreshed traversal per cursor rule |
| State update | `OrderServiceImpl#transition` | ID + tenant + expected version/state | affected rows 1 | PK; version/state predicate | Optimistic lock | 0 affected rows maps to not-found or conflict after safe discrimination |

Address batch size, item insert strategy, N+1 avoidance, count query, cursor/page stability, lock order, transaction duration, and affected-row assertions using actual repository behavior.

#### Migration and historical-data handling

Illustrative expand-first sequence; adapt to repository policy and data profile:

1. confirm the next immutable migration version and create exactly one new file for this database change;
2. profile duplicate/null candidate values and existing index overlap before DDL;
3. add nullable columns or otherwise backward-compatible structures first;
4. deploy code that writes new fields while still reading legacy rows safely;
5. backfill historical rows in bounded, restartable batches with deterministic progress and throttling;
6. validate counts, nulls, duplicates, hashes, constraints, and query results;
7. create/validate indexes using dialect-supported low-lock mechanism when necessary;
8. enforce `NOT NULL` or checks only after old writers are gone and verification proves safety;
9. contract/remove legacy fields only in a later separately approved change when required.

The real Spec must name the exact migration path, SQL/DDL pseudocode, pre/post verification queries and expected values, estimated lock/build risk from evidence, old/new application matrix, batch owner/restart marker, monitoring, rollback point, and forward-fix strategy. Application rollback after new writes may be unsafe even if the DDL remains additive; state this explicitly.

#### Transaction, consistency, and recovery

- `OrderServiceImpl` owns one transaction for idempotency identity, header, and items; DAO methods participate and contain no independent business transaction.
- Concurrent same-key creates rely on the database uniqueness boundary plus deterministic conflict lookup; an in-memory lock is insufficient across processes.
- State updates include expected version/current state so lost updates become a documented conflict rather than silent overwrite.
- Cache, events, search indexes, or downstream projections are either updated after commit through the repository's proven mechanism or explicitly excluded; never claim atomicity across an unverified external boundary.
- Unknown client outcomes are resolved by same-key retry; partial external side effects need an outbox/reconciliation design only when the repository and scope actually include them.
- Audit and repair identify tenant, order, operation, stable correlation/idempotency identity, old/new state, result, and operator action without logging prohibited payload data.

## Migration decision patterns

| Situation | Safe design direction | Required proof | Common unsafe shortcut |
| --- | --- | --- | --- |
| New nullable field read by new code | Expand schema, tolerant read, then write | Old/new version compatibility and null semantics | Marking `NOT NULL` immediately on populated table |
| New required field with derivable history | Nullable/additive -> backfill -> validate -> enforce | Deterministic derivation, batch restart, zero-invalid query | One unbounded update inside migration without lock estimate |
| New unique key | Profile duplicates -> define conflict rule -> remediate -> create unique constraint | Duplicate counts and owner-approved merge/reject rule | Adding uniqueness and hoping production data is clean |
| Large index | Prove query -> inspect overlap -> choose online/concurrent build if supported | Data volume, dialect, lock/build behavior, rollback | Adding every plausible filter combination |
| Destructive type/column change | Expand-and-contract across compatible releases | Conversion correctness, dual read/write or cutover, rollback boundary | Rename/drop in one release while old code runs |
| Existing Flyway migration is wrong | Add the next corrective migration | Current version/checksum and forward transition | Editing or repairing an already-applied migration |

## Depth and consistency gate

Before accepting a table detail, verify:

- all seven required subsections exist in order and name the exact schema/table;
- affected existing and all proposed columns include native type, precision/length, null, default, generation, constraints, meaning, mapping, and example;
- primary/business/foreign relationships state ownership, optionality, tenant scope, update/delete/orphan behavior, and enforcement choice;
- every index names an exact query/caller, ordered keys, selectivity evidence, sort/coverage role, overlap, write/storage/build/lock cost, and verification;
- every access path states predicate/join/order/page, row expectation, index/constraint, lock/isolation, failure, idempotency, and affected-row semantics;
- migration covers data profile, exact new version/path, ordered DDL/backfill, batching/restart, old/new coexistence, locks, validation, rollback limit, and forward fix;
- transaction/recovery aligns with Service orchestration, API errors/retries, cache/events, audit, tests, and rollout;
- static inspection is never presented as a live schema, data-distribution, lock-duration, or query-plan result.

Line count is not a substitute. A read-only lookup against a proven stable table may need less prose; a one-column change on a populated high-volume table may require extensive migration and compatibility design.

## Database review failures

Return `REVISE` when any applies:

- a table is listed but not expanded, or a column lacks type/null/default/meaning;
- an interface/model field cannot be mapped to a column or intentional non-persistent source;
- an index lacks a real query and column-order rationale;
- uniqueness, soft-delete, tenant, time, money, or `NULL` semantics are ambiguous;
- migration/backfill/compatibility/locking/rollback behavior is missing;
- an existing immutable migration is proposed for modification;
- source inspection is presented as proof of a live schema or query plan.
