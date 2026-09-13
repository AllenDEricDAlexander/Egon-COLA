# MyBatis-Plus Repository Starter

MyBatis-Plus 3.5.16 integration for PostgreSQL: common models, guarded Repository commands, explicit query SQL, MybatisBatch, tenancy, optimistic locking and managed DDL. Keep `Controller → Service → Repository → Mapper`; COLA Application/Domain services retain business ownership and repositories live in infrastructure.

## Model contract

Extend `EgonModel<PO>` and declare `@TableName`. Do not shadow technical fields.

| Java field | SQL column | Contract |
| --- | --- | --- |
| id | id | Long/BIGINT; inherited mandatory `@TableId(type=ASSIGN_ID)` |
| tenantId | tenant_id | Non-null Long/BIGINT |
| createUserId / updateUserId | create_user_id / update_user_id | Current user context |
| createTime / updateTime | create_time / update_time | Instant, UTC microseconds |
| deletedAt | deleted_at | LocalDateTime/timestamp(6); NULL means active |
| version | version | Long/BIGINT; insert 0, increment on update/delete |

`@TableLogic` uses `(CURRENT_TIMESTAMP AT TIME ZONE 'UTC')`. `EgonColaIdentifierGenerator` delegates to the existing named `snowflakeIdGenerator`; configure a unique `EGON_ID_MACHINE_ID` per instance. Counters belong only in isolated tests. `@KeySequence` conflicts with this ASSIGN_ID contract and is rejected.

Common accepts any non-null Long tenant; ShardingSphere hosts require positive Long sharding keys. The mandatory MetaObjectHandler owns technical fields. Extension hooks may fill business fields only.

## Repository and CQRS

`EgonColaIRepository<T>` extends official `IRepository`; `EgonColaRepository<M,T>` supplies guarded operations. Business service ports expose domain types without PO generics or technical CRUD inheritance. Concrete named repositories use qualified Lombok constructor injection and provide mapper/modelValidationUtils/tenantIdProvider/properties getters.

- Commands use save, versioned updateById/removeById and guarded batches; callers check affected rows.
- Queries use named Mapper XML. Every mapper supplies `selectActiveById`, `selectActiveByIds`, `deleteVersionedById`.
- ActiveRecord is unavailable. QueryChain/lambdaQuery and generic Query Wrapper entry points fail fast; do not concatenate `.last()` SQL.
- For custom UPDATE, MP increments the entity version before execution: bind `#{MP_OPTLOCK_VERSION_ORIGINAL}` in WHERE, together with tenant, active and expected business state.
- Preserve id, tenant, creation metadata and version from the caller or a row loaded in the same transaction. Zero affected rows are not successful updates.
- MybatisBatch requires the same DataSource and an actual Spring transaction. Empty input emits no SQL; invalid/duplicate IDs fail early. Default chunk 1000, collection limit 10000; failures mark rollback-only.

No platform SQL Injector was added. Use mapper extensions for concrete non-generic SQL needs, and field handlers for real JSONB/array differences. Agent keeps its field-specific JSONB handler; the global String handler stays standard. Persisted enums require one `@EnumValue` and matching public `@JsonValue`/Jackson semantics, checked at startup.

## SQL guards and transactions

The original SQL guard proves positive ID bounds before execution. Final SQL checks enforce tenancy, active rows, versions and audit fields after TenantLine. Block-attack, optimistic locking, PostgreSQL pagination and LOCAL write-target checks share one interceptor chain. Dynamic table names are disabled by default and require explicit mappings.

LOCAL checks span SqlSessionFactory instances. Multi-table writes within one physical group are allowed; cross-group writes fail and mark rollback-only. XA/BASE are not enabled. Root-key bulk writes require the exact statement ID and column in `local-write-guard.allowed-root-statements`.

Page limit is 500. Data-change recording and IllegalSQL are permitted only in dev. Keep the raw recorder logger `'OFF'`; use the separate safe `top.egon.cola.component.common.mybatis.change-summary` topic. Mixed dev/prod activation is rejected.

## Managed DDL

`EgonColaPostgreDdlRunner` receives explicit physical PRIMARY/schema/role targets and SHA-256 manifests. Schema advisory locks, managed-prefix checks, script SQL and ddl_history run on one transaction connection. Unknown commit outcomes are checked through a new connection before any retry. Non-empty unmanaged schemas, checksum drift and route fingerprint changes require operator action; no automatic DROP, repair or history adoption exists.

Do not register DDL target records as default MP IDdl beans or combine this runner with DdlApplicationRunner. Common has no ShardingSphere dependency. Hosts own pool creation, topology validation, DDL, primary/replica readiness and logical datasource construction.

Six business archetypes use this runner and retain old B/V/manual SQL unchanged as archives. Agent keeps Flyway and adds one empty-knowledge-table correction; Outbox/vector ownership is unchanged.

## Configuration and proof

Settings live under `egon.cola.component.mybatis-plus`; source profiles provide complete examples. Common defaults DDL to disabled, the six archetypes enable it, and Agent disables it. ID settings use `egon.cola.component.id`.

Tests exercise actual Mapper/plugin paths following the official MP test style. CPU/Mock/H2 results do not prove PostgreSQL DDL, replication, physical placement or query performance. Explicit PG tests use dedicated databases and `egon.pg.routing=true` / `egon.pg.readwrite=true`; run them manually. Validate SQL plans with EXPLAIN on realistic data.
