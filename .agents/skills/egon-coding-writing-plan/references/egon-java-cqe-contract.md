# Egon Java / CQE effective contract · Java 与 CQE 生效规范

Updated: 2026-09-22. Read this reference for every Java Spec, Plan and execution audit. It supersedes field-count-based records, unconditional builder choices, utility-only validation and CQRS naming. Apply only to the requested change surface; a documentation update does not authorize rewriting existing Java or SQL.

## POJO, value object and enums · 对象与枚举

Ordinary POJOs, persistence entities and mutable business carriers use `class`, regardless of field count. The common class annotations and an inheritance example are shown below; select the builder using the table, not by field count:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
```

| Construction target | Selection | Required check |
| --- | --- | --- |
| Ordinary class without inherited builder state | `@Builder` | Verify the targeted constructor/factory and its parameters |
| Existing constructor or factory method | `@Builder` on that constructor/method | Preserve initialization/invariant logic and visibility |
| Class hierarchy whose builder includes superclass fields, e.g. `EgonModel` PO | `@SuperBuilder` | All participating superclasses use compatible `@SuperBuilder`; no mixed `@Builder` chain |
| Immutable value-object record | Record construction | No mutable POJO annotation requirement |

Class-level `@Builder` relies on an all-arguments construction target; with explicit constructors prefer annotating the intended constructor. With Lombok-generated no/all-args constructors, verify the generated target using the project's Lombok version. `toBuilder=true` on a SuperBuilder hierarchy requires the parent chain to support it. Do not change dependencies/import packages merely because current online docs differ from the pinned version.

Sources: [Lombok Builder](https://projectlombok.org/features/Builder), [Lombok SuperBuilder](https://projectlombok.org/features/experimental/SuperBuilder), [Lombok EqualsAndHashCode](https://projectlombok.org/features/EqualsAndHashCode).

- Use `callSuper = true` for a meaningful superclass such as `EgonModel`. A class extending only `Object` must omit it (or use `callSuper = false`); do not invent a parent solely to satisfy an annotation. `@SuperBuilder` must be compatible through the actual inheritance chain. Do not mix it with `@Builder`.
- Do not apply data-class `@RequiredArgsConstructor`; retain it for business Bean dependency injection. For a zero-field class where no/all-args constructors collide, retain only the nonduplicating constructor and record the reason. Framework or public-API conflicts beyond these mechanical exceptions need an explicit design decision.
- Only a value object representing immutable value semantics may use `record`; PO/entity identity, lifecycle and update behavior require `class`. DTO/Query/Command/Event names alone do not establish value semantics; a presentation VO suffix does not mean DDD Value Object. A `record` is exempt from the mutable POJO annotation set. Components containing collections or mutable objects require defensive copies and immutable element semantics; compact constructors may normalize and enforce invariants. Do not call every short DTO a value object.
- Persistence enums use `@EnumValue` on the stable database code field. Frontend JSON enums use Jackson `@JsonValue` on the intended external scalar field/accessor. If the same enum serves both boundaries, use both with an explicit value mapping. Follow the existing `EgonEnum` contract where applicable. Never persist `ordinal()` or expose accidental `name()` values. Specify unknown/null input and compatible deserialization; `@JsonValue` alone is not an input-validation policy.

普通实体一律使用 class；只有语义上不可变的值对象可用 record，豁免可变 POJO 注解。通用注解加按场景选择的 Builder 如上；直接继承 Object 时不能强行 callSuper=true，零字段构造器不能重复生成，这些是明确的编译适配，不应新建无意义父类。枚举持久化用 @EnumValue，前端输出用 @JsonValue，分别核对输入、输出与存储编码。

## Component reuse and MP · 组件与持久化

Mandatory capability discovery starts in `egon-cola-components`: common contracts/validation/conversion, ID, MP, cache, RPC, observability, security, thread pools and transactional outbox. Record exact module/API, fit, chosen reuse and any concrete gap. Reuse a matching Egon component before recreating its capability or wiring a parallel generic starter. Reuse does not mean importing every component or adding infrastructure to a pure contract module.

“Egon-Cola-MP-starter” resolves to the current Maven artifact `top.egon:egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter`. Persistence implementations must consume this starter; domain/facade contracts must not gain technical CRUD inheritance just to make every POM list it. Use the repository/BOM version; do not invent an alias artifact or pin a second MP version.

- `EgonModel<T>` owns ID, tenant, audit, nullable `LocalDateTime deletedAt` and optimistic `version`.
- Infrastructure repositories extend `EgonColaRepository<M,T>`; Mapper/DAO extends `EgonColaMapper<T>`. Domain service ports expose business semantics, not PO-generic CRUD.
- Commands use guarded/versioned writes and check results. Queries use named explicit Mapper XML with tenant, active-row and routing predicates. No ActiveRecord, QueryChain or generic wrapper bypass.
- Reuse MapStruct/MapStructPlus and common `BaseConverter<S,T>` / `BaseForwardConverter<S,T>`. Cache annotations belong to concrete repository methods; ordinary CRUD does not imply cache invalidation.

项目必须尽量复用 egon-cola-components 的已有能力；持久化实现必须用上述准确 MP Starter。保持模块边界，不能把 MP 依赖和 PO 泛型扩散到纯领域端口或 facade。

## Validation · 注解驱动校验

Define validation through Jakarta constraints, composed/custom annotations with `@Constraint` and `ConstraintValidator`, `@Valid` cascades, Spring `@Validated`, validation groups and group conversion where needed. Reuse native constraints for generic rules; express domain-specific reusable constraints as custom annotations/extensions. Verify proxy entry, self-invocation, nested objects, group selection, error mapping and non-HTTP entry paths.

`ValidationUtils` is a generic explicit-validation helper for paths without automatic interception. It may invoke the same constraint metadata; it is not the business-rule registry, a replacement for annotations, or a requirement to call it at every proxy-validated boundary. Cross-field rules belong in class-level constraints; stateful business invariants remain in the owning domain/service and transaction. Telephone constraints may delegate to libphonenumber; do not reimplement parsing in a utility.

不能完全依赖 ValidationUtils。每个边界明确原生/自定义注解、@Valid、@Validated、分组和扩展；工具仅补充手工触发。检查非代理、自调用、消息入口、嵌套对象及错误映射。

## Soft-delete business uniqueness · 软删除业务唯一键

The user's `deleteAt` means the existing Java `deletedAt` / SQL `deleted_at`; do not add or rename a column to the misspelling. Every soft-deletable business unique key must combine the business columns (and tenant scope where applicable) with `deleted_at`. Primary identity and deliberately permanent technical deduplication keys are separate semantics and must be identified explicitly.

Because current MP uses NULL for active rows, plain `UNIQUE (tenant_id, business_code, deleted_at)` does not by itself enforce active-row uniqueness on PostgreSQL's ordinary NULL semantics. The design must specify both the composite lifecycle key and the effective active-row guard. Preserve the composite requirement; a partial index alone must not silently replace it.

For example, after verifying the deployed PostgreSQL capability, use either NULLS NOT DISTINCT composite uniqueness, or a composite unique key plus `UNIQUE (tenant_id, business_code) WHERE deleted_at IS NULL`. Do not retain an old unique constraint on the business columns alone that still prevents recreate-after-delete. Verify repeated deletes with equal timestamp precision, undelete conflicts, tenant isolation, concurrent creates, nullable business fields and shard-local versus global enforcement. If repeated historical timestamps can collide, resolve the lifecycle-key design explicitly rather than claiming the extra column prevents every error.

Use the distributed managed-DDL contract below for corrective schema work. Existing SQL/history stays immutable; legacy db/manual and B/V files are archival.

联合唯一键必须包含业务列与 deleted_at，并验证 NULL；仅加可空列无法证明有效行唯一。检查旧业务列单独唯一约束、重复删除、恢复、租户及分片语义。文档更新不等于已修正现有 DDL。

## Distributed managed DDL · 分布式受管 DDL

All project database design uses the existing MP-SDJ extension starter; Flyway is no longer a supported project option. The user shorthand `egon-mp-sdj-ext-starter` resolves to `top.egon:egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` in the current repository. Do not introduce an alias dependency or a second migration framework.

- Reuse `EgonColaPostgreDdlRunner` and the starter's datasource/topology lifecycle; applications must not copy a local `ShardingDataSourceBootstrapper`. Inspect `EgonColaShardingDataSourceBootstrapper` and the actual auto-configuration wiring before claiming startup execution.
- Inputs are explicit physical PRIMARY/schema/role targets and ordered scripts with SHA-256 manifests. Current Light/Service/Web resources are `db/egon-mp` and `repository-manifest.json`; derive the actual configured resource path for the affected module.
- PostgreSQL schema advisory transaction locks serialize competing instances. Each SQL version and its `ddl_history` row commit on the same connection/transaction. Multiple physical targets do not form one global transaction; a later target failure does not undo earlier committed targets.
- Unknown commit outcomes are verified on a new connection before retry. Check lock/statement/topology-ready timeouts, target identity, script prefix, checksum and route fingerprint. Nonempty unmanaged schemas or drift require explicit recovery; never auto-DROP, repair, adopt history or blindly replay.
- One logical schema change adds one next SQL version and its manifest checksum entry. Never edit, reorder, rename or rehash already-applied SQL/history to force a pass. Test partial-target progress, rerun/idempotency, contention and commit-unknown recovery.
- Do not combine the runner with MP default `IDdl` beans/`DdlApplicationRunner`. Configuration lives under `egon.cola.component.mybatis-plus.ddl`; match existing profile key structure and deployment permissions.

统一采用该组件分布式 DDL，不再规划 Flyway。锁与 SQL/history 原子性按物理目标和脚本版本保证，不是跨库全局原子事务。新增版本与 Manifest，保留已应用历史，失败必须可判定、可续跑，不能自动重建或修 checksum。

Source drift must be reported honestly: the checked-in Agent POM/config still contains old migration dependencies and disables Common DDL. This is a legacy implementation gap against the current user requirement, not a retained architecture option; documentation work alone does not complete its runtime migration.

## CQE · Command / Query / Event

- **Command** expresses state-changing intent, owns authorization, validation, idempotency, transaction and failure behavior.
- **Query** is read-only business retrieval; no hidden writes or business event publication.
- **Event** is an occurred fact, delivered through `egon-cola-component-transactional-outbox-starter` or an actual MQ middleware producer. Local `ApplicationEventPublisher`, `@Async`, in-memory listeners or after-commit logging alone do not satisfy delivery.
- With the outbox, use `TransactionalOutbox.enqueue(OutboxMessage)` within the business transaction and verify the datasource/transaction-manager boundary and supported transport. The user shorthand `egon-cola-transcational-box` refers to this existing artifact, not a new dependency.
- Direct MQ is allowed, but must document publish confirmation, rollback/publish ordering, retry/recovery and the DB/MQ dual-write failure window. When atomic DB-plus-event intent is required, use the outbox or a verified broker transaction design. Do not call after-commit RabbitMQ publication atomic or exactly once.
- Every event contract records event ID, business identity, schema version, destination/channel, payload, consumer validation/idempotency, retry/backoff, dead-letter handling and observability. A test-local publisher is only a test double.

CQE replaces the project's CQRS naming while preserving command/query separation. It does not require a third Maven module, event sourcing, independent databases or an event for every query/CRUD operation. If an operation publishes no event, record that explicitly. Existing API/validator filenames and symbols are not proof of messaging guarantees.

## Architecture and phase gates · 架构与分阶段检查

Ground DDD in `egon-cola-archetypes/source-projects` POMs, Java and architecture tests plus `definitions` packaging contracts. Light is one module with domain-first packages and `start`; Service/Web have common, facade, domain, application, infrastructure, adapter, starter; Agent has six modules without facade. Preserve the selected native/Open variant, actual protocol dependencies and port ownership. Domain owns business ports; Infrastructure implements them. Service has RPC/MQ entrypoints; Web/Light have HTTP/GraphQL as present in source. Do not copy a generic DDD tree.

Traditional three-layer architecture retains its current package tree and Controller -> Service -> Repository -> Mapper boundaries. CQE and annotation rules do not authorize adding DDD layers.

In Spec design, Plan file blocks, and execution/final review, record these six checks separately under the existing Manual Check IDs:

| Concern | Existing gate | Required evidence |
| --- | --- | --- |
| POJO / record / enum | MC-MODEL-001, MC-JSON-001 | Type semantics, annotation set, inheritance, immutable components, DB/JSON code mapping |
| Component / MP reuse | MC-REUSE-001, MC-DEP-001 | Actual artifacts/APIs, module owner, fit or gap |
| Validation | MC-VALID-001 | Custom/native constraint metadata, cascade/group/proxy and negative tests |
| Business uniqueness / DDL | MC-MODEL-001, MC-TEST-001 | Business + deleted_at key, active-row enforcement, one SQL version + manifest, per-target transaction/lock/recovery |
| CQE delivery | MC-ARCH-001, MC-TEST-001 | C/Q/E ownership, real outbox/MQ route and failure/idempotency proof |
| Exact architecture | MC-ARCH-001 | Source tree, POM edges, ports and selected variant; unchanged three-layer structure |

Each applicable concern requires a decision and evidence; prose claims or passing unrelated tests are insufficient. Source inspection and isolated tests do not prove live PostgreSQL, broker or cross-process delivery.
