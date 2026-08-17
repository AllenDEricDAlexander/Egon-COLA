# Per-table and Per-index Database Design

Read this reference whenever Chapter 11 affects persisted data or relies on existing tables for correctness. Use the repository's actual database dialect, migration framework, naming conventions, and access technology.

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

## Database review failures

Return `REVISE` when any applies:

- a table is listed but not expanded, or a column lacks type/null/default/meaning;
- an interface/model field cannot be mapped to a column or intentional non-persistent source;
- an index lacks a real query and column-order rationale;
- uniqueness, soft-delete, tenant, time, money, or `NULL` semantics are ambiguous;
- migration/backfill/compatibility/locking/rollback behavior is missing;
- an existing immutable migration is proposed for modification;
- source inspection is presented as proof of a live schema or query plan.
