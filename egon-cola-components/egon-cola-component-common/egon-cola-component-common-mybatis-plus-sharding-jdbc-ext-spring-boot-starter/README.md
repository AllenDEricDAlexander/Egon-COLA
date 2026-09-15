# MyBatis-Plus + ShardingSphere-JDBC Starter

Consuming this starter requires PostgreSQL + MyBatis-Plus + ShardingSphere-JDBC. Excluding `shardingsphere-jdbc` is unsupported and must fail at compile or startup. The starter publishes the logical `@Primary DataSource` from one YAML file under `egon.cola.component.mybatis-plus.sharding`. `config-style: STRATEGY` and `config-style: NATIVE` are mutually exclusive.

Recommended STRATEGY yaml uses `tenant_id` first, then a business root such as `order_id` for orders/order_items (`COMPLEX_TENANT_THEN_BUSINESS`). Default transaction type is LOCAL; XA remains on the classpath and can be selected with `transaction-default-type: XA`.

```yaml
egon:
  cola:
    component:
      mybatis-plus:
        sharding:
          enabled: true
          mode: SHARDING
          config-style: STRATEGY
          transaction-default-type: LOCAL
          data-sources:
            - name: master_data
              logical-name: master_data
              role: PRIMARY
              driver-class-name: org.postgresql.Driver
              jdbc-url: jdbc:postgresql://localhost:5432/master_data
              username: postgres
              password: postgres
            - name: shard_0
              logical-name: shard_0
              role: PRIMARY
              driver-class-name: org.postgresql.Driver
              jdbc-url: jdbc:postgresql://localhost:5432/shard_0
              username: postgres
              password: postgres
            - name: shard_1
              logical-name: shard_1
              role: PRIMARY
              driver-class-name: org.postgresql.Driver
              jdbc-url: jdbc:postgresql://localhost:5432/shard_1
              username: postgres
              password: postgres
          tables:
            users:
              type: SINGLE
              data-source: master_data
            dict_region:
              type: BROADCAST
            orders:
              type: COMPLEX_TENANT_THEN_BUSINESS
              sharding-column: tenant_id
              table-columns: [tenant_id, order_id]
              root-key-name: order_id
            order_items:
              type: COMPLEX_TENANT_THEN_BUSINESS
              sharding-column: tenant_id
              table-columns: [tenant_id, order_id]
              root-key-name: order_id
```

NATIVE escape hatch: point `native-rules-resource` at one ShardingSphere rules file. Do not declare STRATEGY `tables` in the same YAML.

```yaml
egon:
  cola:
    component:
      mybatis-plus:
        sharding:
          enabled: true
          mode: SHARDING
          config-style: NATIVE
          transaction-default-type: LOCAL
          native-rules-resource: classpath:egon-ss-native.yml
          data-sources:
            - name: shard_0
              logical-name: shard_0
              role: PRIMARY
              driver-class-name: org.postgresql.Driver
              jdbc-url: jdbc:postgresql://localhost:5432/shard_0
              username: postgres
              password: postgres
            - name: shard_1
              logical-name: shard_1
              role: PRIMARY
              driver-class-name: org.postgresql.Driver
              jdbc-url: jdbc:postgresql://localhost:5432/shard_1
              username: postgres
              password: postgres
```

```yaml
# classpath:egon-ss-native.yml
databaseName: egon
rules:
  - !SHARDING
    tables:
      orders:
        actualDataNodes: shard_0.orders,shard_1.orders
        databaseStrategy:
          standard:
            shardingColumn: tenant_id
            shardingAlgorithmName: tenant_db
    shardingAlgorithms:
      tenant_db:
        type: CLASS_BASED
        props:
          strategy: STANDARD
          algorithmClassName: top.egon.cola.component.common.mybatis.sharding.algorithm.EgonColaLongTenantShardingAlgorithm
  - !SINGLE
    tables:
      - master_data.users
```

MyBatis-Plus 3.5.16 integration for PostgreSQL: common models, guarded Repository commands, explicit query SQL, MybatisBatch, tenancy, optimistic locking, ShardingSphere topology and managed DDL. Keep `Controller → Service → Repository → Mapper`; COLA Application/Domain services retain business ownership and repositories live in infrastructure.

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

Do not register DDL target records as default MP IDdl beans or combine this runner with DdlApplicationRunner. This starter owns pool creation, topology validation, TableInfo schema maintenance, DDL scripts, and logical datasource construction. Applications must not rebuild local `ShardingDataSourceBootstrapper` copies.

Six business archetypes use this runner and retain old B/V/manual SQL unchanged as archives. Agent keeps Flyway and adds one empty-knowledge-table correction; Outbox/vector ownership is unchanged.

## Configuration and proof

Settings live under `egon.cola.component.mybatis-plus`; source profiles provide complete examples. Common defaults DDL to disabled, the six archetypes enable it, and Agent disables it. ID settings use `egon.cola.component.id`.

Tests exercise actual Mapper/plugin paths following the official MP test style. CPU/Mock/H2 results do not prove PostgreSQL DDL, replication, physical placement or query performance. Explicit PG tests use dedicated databases and `egon.pg.routing=true` / `egon.pg.readwrite=true`; run them manually. Validate SQL plans with EXPLAIN on realistic data.
