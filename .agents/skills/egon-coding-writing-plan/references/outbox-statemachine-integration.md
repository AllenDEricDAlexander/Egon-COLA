# Transactional Outbox and Spring Statemachine integration

Repository baseline: 2026-09-27, Outbox implementation through `deaac908d`. This is a repository integration contract, not a claim about the newest upstream release. Recheck the affected source and BOM when using it. The Chinese audit mirror is `references/outbox-statemachine-integration.zh-CN.md`.

Use this reference for Outbox adoption, business state-machine Event consumption, or changes to technical delivery/migration. An ordinary Query or a task with no event/state-machine concern does not need this integration. Direct MQ and existing HTTP/RabbitMQ/custom handlers remain valid CQE routes; do not force every Event or Command through the optional business adapter.

## Source map

Resolve these repository-relative locations from the actual checkout, not from the skill directory:

- `O` = `egon-cola-components/egon-cola-component-transactional-outbox-starter`.
- `J` = `O/src/main/java/top/egon/cola/component/outbox`; `T` = `O/src/test/java/top/egon/cola/component/outbox`.
- Dependencies: `O/pom.xml` and `egon-cola-components/egon-cola-components-bom/pom.xml`; Spring Statemachine core is currently BOM-managed at 4.0.2. Reuse the resolved version; this reference does not authorize upgrading it or adding the full Statemachine starter, data-* persistence or Kryo.
- Technical behavior: `J/statemachine/OutboxLifecycleStateMachineFactory.java`, `OutboxLifecycleService.java`, `StateMachineExecutionService.java`; `J/store/MybatisPlusOutboxStore.java`; `O/src/main/resources/mapper/outbox/OutboxMessageMapper.xml`.
- Business behavior: `J/statemachine/BusinessStateMachineService.java`, `BusinessStateMachineEvent.java`, `BusinessStateMachineSnapshotBO.java`, the three business SPI interfaces, and `J/delivery/statemachine/BusinessStateMachineDeliveryHandler.java`.
- Wiring/storage: `J/autoconfigure/OutboxBusinessStateMachineAutoConfiguration.java`, `OutboxStateMachineAutoConfiguration.java`, `OutboxMybatisPlusAutoConfiguration.java`, both state-machine/MP property classes, and `J/migration`.
- Accepted design and correction: `docs/egon/spec/2026-09-24-12-00-outbox-statemachine-cqe-integration.md`, especially DEC-009; execution evidence: `docs/egon/plan/2026-09-24-13-16-outbox-statemachine-mp-implementation.md`, Step 9. Some README wording still describes SKIP LOCKED; inspect the current Mapper and effective Spec instead of copying that wording.

## 1. Design decisions

### 1.1 Scope, ownership and CQE

Retain the existing single Starter. Select State for transition decisions, Strategy for application definitions, and Adapter for the existing DeliveryHandler SPI. These patterns already exist; application adoption normally implements extension points rather than modifying the engine.

| Concern | Owner and contract |
| --- | --- |
| Command | Authorization, validation and request idempotency; persist the business change and `TransactionalOutbox.enqueue(OutboxMessage)` in one compatible local transaction. Outbox deduplication does not make the Command itself idempotent. |
| Query | Read business facts without writes, Event publication or state-machine execution. |
| Event | An occurred fact with stable identity, version and consumer behavior; durable delivery through Outbox or a real MQ producer. `OutboxCommittedEvent` is only a wake-up hint, not reliable delivery. |
| Technical lifecycle | Starter-owned Spring Statemachine; required whenever Outbox is enabled. The MP message row is authoritative. SQL locking/CAS determines who can commit; it does not replace state-machine decisions. |
| Business lifecycle | Explicitly enabled application-owned definitions. The business row and durable consumption receipt are authoritative, not an in-memory Machine or ExtendedState. |

The `statemachine` channel is a local DeliveryHandler in the Outbox worker's application. It does not route automatically to a definition registered in another service. A remote consumer needs an explicit supported transport and its own receiving contract.

For each affected flow, the Spec must identify the Command, Event, destination, business aggregate, tenant authority, transition table (source/signal/guard/target), transaction boundaries, duplicate/ordering rules and failure outcomes. Distinguish “intent enqueued”, “transport delivered” and “business transition committed”. Keep at-least-once delivery; no exactly-once or cross-database atomicity claim.

### 1.2 Technical state model

| Source | Signal | Target / condition |
| --- | --- | --- |
| `__NEW__` (memory only) | `ENQUEUE` | `PENDING` |
| `PENDING`, `RETRY_WAIT` | `CLAIM` | `PROCESSING`, only when due |
| `PROCESSING` | `RECLAIM` | External self-transition, only after lease expiry; new owner and attempt |
| `PROCESSING` | `DELIVERY_SUCCEEDED` | `SUCCEEDED` |
| `PROCESSING` | `DELIVERY_RETRYABLE` | `RETRY_WAIT` below maxAttempts, otherwise `DEAD` |
| `PROCESSING` | `SCHEDULE_RETRY` | Explicit Store `markRetry` operation; preserve this edge instead of duplicating retry-budget policy |
| `PROCESSING` | `DELIVERY_PERMANENT` | `DEAD` |
| `SUCCEEDED`, `DEAD` | — | No replay/reset transition; retention cleanup deletes only expired successes |

Reclaim does not gain a new maxAttempts ceiling. A late owner may still complete after expiry if nobody has reclaimed the row; once ownership changes, the old owner must not overwrite it. Cleanup ends the technical idempotency-key retention window. It must not expire business receipts automatically.

### 1.3 Business fact and durable receipt

The existing `BusinessStateMachineEvent` contract contains `eventId`, positive `tenantId`, `machineKey`, positive `definitionVersion`, `businessId`, `eventType`, nullable nonnegative `expectedVersion`, `Instant occurredAt` and object-valued Jackson `ObjectNode payload`. Inspect its annotations for exact formats and limits. `expectedVersion` refers to the consuming aggregate version, not necessarily the producer's version. Each definition validates its own payload before persistence access.

Keep transport `messageId`, producer request key, Outbox `idempotencyKey` and business `eventId` distinct. The durable consumption identity is `(tenantId, machineKey, definitionVersion, businessId, eventId)`. Store the component-provided fingerprint for that identity; do not compute a competing fingerprint or retain only `lastEventId`. E1 → E2 → duplicate E1 must still short-circuit.

Under the aggregate lock, receipt lookup precedes definition/expected-version checks: matching receipt/fingerprint succeeds; the same identity with different content is a permanent conflict. For new Events, definition mismatch and stale expectedVersion fail permanently; a future expectedVersion is retryable. Missing expectedVersion removes that ordering check, not aggregate locking, CAS or receipt deduplication. Receipt retention must cover the declared redelivery/repair window. The application owns its schema and any new managed DDL; do not invent a component-wide business table or retrofit soft deletion onto permanent technical receipt identity.

Tenant headers/body equality is not authorization. The application context executor validates trusted tenant provenance and binds/restores context synchronously around validation and the business transaction. The technical Outbox tenant is fixed at 0; never copy that into business tenant fields or broaden technical ignored-table configuration to business tables.

### 1.4 Transaction and machine restrictions

Producer business writes plus enqueue use the same local database transaction. Claim/reclaim/mark/cleanup use the existing worker transactions; delivery occurs after claim commits. The optional business handler opens its own `REQUIRES_NEW` / `READ_COMMITTED` transaction using the selected Outbox transaction manager. Its business repository must participate in that transaction: load/lock, state CAS and receipt insert commit together. Do not use another datasource or independent REQUIRES_NEW inside `saveTransition`.

A successful business commit followed by failed technical success-marking permits redelivery; the receipt prevents another business transition. Do not compensate an already committed business change solely because technical marking failed. Commit-unknown and transient storage failures are retried using the same identity.

Each evaluation uses a fresh, stopped, flat, single-region Machine and an explicit, single external transition (including a self-transition). No cached running instances, shared ExtendedState, timers, deferred events, do-actions, hierarchical/orthogonal graphs or automatic event chains in this adapter contract. Guards/Actions perform pure computation on copied Event/facts; no JDBC, HTTP, MQ, asynchronous writes or reliance on thread-local tenant context inside reactive execution. Persist on the transaction thread after the runner returns. This is an application contract requiring review/tests, not a sandbox that can block arbitrary Java I/O.

Reuse `StateMachineExecutionService`: reset → start → sendEvent → consume result completion → verify exactly one matching completed transition and final state → bounded stop. ACCEPTED alone and unchanged state alone do not prove success. Use remaining deadline budgets and preserve primary failures when cleanup fails.

## 2. Implementation details

### 2.1 Application integration order

Use the actual host architecture and generator rules. In the Plan, assign exact files and validation to these dependencies; do not blindly create one class per row:

1. Resolve the existing Starter/BOM, local datasource/transaction manager/SqlSessionFactory and supported transport. The canonical artifact is `top.egon:egon-cola-component-transactional-outbox-starter`; “transcational box” is not another dependency.
2. Define the business graph, Event payload, business-state column/version mapping and durable receipt storage. Reuse suitable existing storage; add managed DDL only for missing application requirements, with old SQL/history immutable.
3. Implement the three existing business SPIs below. Establish compilable contracts before behavior RED tests; do not count compilation failures as behavior RED or commit stubs as finished implementation.
4. Register named Beans, the fresh Machine factory and exact versioned destination; then enable the business adapter. Deploy compatible definitions before producing Events of that version; do not silently upgrade old pending Events to a new graph.
5. In the Command transaction, enqueue the exact envelope below; keep Query paths read-only. Reuse the component's service/handler/runner rather than writing another dispatcher or persistence engine.
6. Run focused real-machine tests and application wiring/transaction tests, then authorized isolated PG tests for changed persistence behavior. Record runtime limits separately from code/test completion.

| Existing SPI | Host implementation obligations |
| --- | --- |
| `BusinessStateMachineDefinitionStrategy` | `destination()`, `stateMachineFactory()`, `repository()`, `validateEvent(event)`; destination is unique `machineKey:definitionVersion`; payload validation is definition-specific. |
| `BusinessStateMachineRepository` | `lockAndLoad(event,fingerprint)` returns `BusinessStateMachineSnapshotBO(currentState,version,definitionVersion,appliedFingerprint,facts)` under a tenant/active/business-identity aggregate lock, including a receipt lookup for this Event identity. `saveTransition(event,snapshot,targetState,fingerprint)` returns void: guard by tenant/identity/active/version, require a successful CAS, insert receipt in the same transaction, throw on failure so partial writes roll back. Map retryable concurrency failures to exceptions recognized by the existing service. |
| `BusinessStateMachineContextExecutor` | `<T> T execute(event,Supplier<T> action)`; exactly one Bean named `outboxBusinessStateMachineContextExecutor`, trusted tenant check, synchronous execution and finally restoration. |

Use the project's named Beans, qualified Lombok constructor injection, annotated/grouped validation and MapStruct/Egon converters for real model boundaries. Keep overriding-method constraints compatible with the interfaces; do not strengthen parameter constraints in overrides and trigger Bean Validation contract errors.

### 2.2 Producer envelope

The following is a method-body fragment inside an authorized Command transaction. `event` is a validated, fully populated `BusinessStateMachineEvent`; `transportKey` is stable for this fact and destination. It is not a complete application class or a substitute for business request idempotency.

```java
transactionalOutbox.enqueue(OutboxMessage.builder()
        .idempotencyKey(transportKey)
        .channel("statemachine")
        .destination(event.getMachineKey() + ":" + event.getDefinitionVersion())
        .contentType("application/json")
        .schemaVersion("1")
        .headers(Map.of(
                "egon-event-id", event.getEventId(),
                "egon-tenant-id", event.getTenantId().toString(),
                "egon-machine-key", event.getMachineKey(),
                "egon-definition-version", event.getDefinitionVersion().toString()))
        .payload(event)
        .build());
```

The handler rejects unknown top-level fields and envelope/header/body mismatches. Business definitionVersion and transport schemaVersion are separate. Do not use serialized strings in place of the required Event object or place credentials in headers/payload. Application payload validation is still required; top-level Jackson validation cannot establish arbitrary domain rules.

### 2.3 Configuration and persistence wiring

Prefixes below are under `egon.cola.component.transactional-outbox`:

| Suffix | Current default / requirement |
| --- | --- |
| `state-machine.execution-timeout` | `100ms`; actual technical runner evaluation budget |
| `state-machine.claim-evaluation-timeout` | `1s`; startup validates executionTimeout ≤ this value ≤ leaseDuration. Current lifecycle execution uses executionTimeout; do not claim a separate whole-batch deadline without inspecting/enforcing it. |
| `state-machine.business.enabled` | `false`; when true require the context executor and at least one valid unique definition/factory/repository |
| `state-machine.business.timeout` | `5s`, strictly below delivery.timeout; processing uses the earlier of business and delivery deadlines |
| `storage.mp.sql-session-factory-bean-name` | `sqlSessionFactory` |
| `storage.mp.migration-mode` | `false`; local enqueue/dispatch maintenance gate, not a fleet-wide writer fence |
| `storage.mp.migration-lock-timeout` | `30s` |
| `storage.mp.manifest-resource` | `db/egon-outbox-mp/manifest.json` |

The current Store requires Common MP SHARDING/NATIVE/LOCAL with one PostgreSQL PRIMARY and two SINGLE routes for the technical message table and `ddl_history` in schema `egon_outbox`. The chosen `DataSourceTransactionManager`, logical datasource and SqlSessionFactory must refer to the same datasource; the factory uses `SpringManagedTransactionFactory`. Preserve existing mapper locations/ignored tables and add only the exact technical table `egon_cola_outbox_message` where required. Outbox does not require Redis; do not disable an existing host cache or start Redis merely because MP has a cache dependency.

Reuse `OutboxManagedDdlInitializer` / `OutboxLogicalDataSourceFactory` and `EgonColaPostgreDdlRunner` before logical datasource creation. The Starter owns `db/egon-outbox-mp/manifest.json` (`component-outbox`) and its SQL; applications must not copy/rewrite that script into their own migrations. A custom logical factory must explicitly preserve the initialization hook. Creating the empty schema/permissions and running a production migration require the task's operational authorization.

### 2.4 Component maintenance and legacy migration

Apply these details only when the task actually modifies Store internals or migrates an old installation:

- Claim/cleanup read non-locking candidates bounded by `min(10000, requestedLimit*4)`, with public request limit 1..10000. For each candidate, acquire `pg_try_advisory_xact_lock(id)` through a SELECT from the target table with id/tenant0/active predicates, on the same worker transaction/PRIMARY as the CAS. A bare function SELECT loses the required SQL-shape/table-route evidence. Do not restore unsupported `FOR UPDATE SKIP LOCKED` for the current ShardingSphere 5.5.3 path.
- Claim invokes the existing SSM and then id/version/source/tenant/active/due-or-expired CAS; cleanup uses id/version/SUCCEEDED/retention CAS delete. False or absent lock result / CAS 0 skips the candidate; genuine engine/database errors roll back the batch. Return at most the requested count; contention may produce a short batch. Advisory locks are released at transaction end, before delivery. Existing owner-completion row-lock/CAS semantics remain in effect.
- Nullable identity lookups must bind on actual PostgreSQL: `idempotency_key = ?` already leaves NULL unmatched; an untyped `? IS NOT NULL` can fail under ShardingSphere with unknown parameter type. Check null/non-null keys and migration reruns, not just XML parsing.
- `OutboxLegacyMigrationService.migrate(sourceSchema,batchSize,verifyOnly)` requires explicit maintenance mode and all old/new writers and workers stopped by the operator. Source and target must use the supported same PRIMARY topology. Reuse bounded source SHARE locking, keyset batches, atomic target writes, exact legacy-field/fingerprint comparison, rerun and final verification. Do not replay SSM transitions during copying or equate DDL history with completed data migration.
- Keep old tables/SQL/history; do not auto-copy on startup, repair checksums or revert to the old table after new writes. Production cutover requires verified data plus ready definitions/consumers. A skill or test command does not authorize live cutover.

### 2.5 Phase evidence and meaningful validation

| Phase | Required deliverable for the affected surface |
| --- | --- |
| Spec design | Ownership/transition and CQE contracts; tenant/transaction/receipt/version rules; schema and error/rollout decisions; explicit optional-adapter choice. |
| Plan | Exact host SPI/Bean/config/producer/storage files, dependency order, behavior RED/GREEN, SQL/manifest ownership, negative and concurrency tests, commit scope. Do not plan recreation of existing component classes. |
| Execution/final review | Verify actual Bean wiring, runtime transaction/receipt ordering, constraints, mapper SQL and reports against the Spec. Inspect source drift rather than reporting every configured property or graph restriction as enforced. |

Use existing Manual Checks: `MC-ARCH-001` / `MC-REUSE-001` for ownership and extension points; `MC-PATTERN-001` for real State/Strategy/Adapter behavior; `MC-VALID-001` / `MC-BEAN-001` / `MC-CONFIG-001` for contracts/wiring; `MC-MODEL-001` / `MC-TEST-001` for durable receipts, CAS, DDL and failure evidence; `MC-SCOPE-001` for component versus application changes.

Select relevant tests: genuine transition completion and Guard rejection/self-transition; disabled/enabled/missing/duplicate definitions; malformed envelope, tenant rejection and context restoration; equal duplicate versus changed fingerprint; E1/E2/E1; stale/future versions; state+receipt rollback; business commit followed by technical-mark failure; claim/reclaim/old-owner/cleanup contention; migration null keys, copy/rerun/conflicts/unknown-commit.

Current component regression anchors include `T/statemachine/*Test.java`, `T/autoconfigure/OutboxBusinessStateMachineAutoConfigurationTest.java`, and `T/integration/BusinessStateMachineIntegrationTest.java`, `PostgresqlOutboxConcurrencyIntegrationTest.java`, `PostgresqlOutboxCleanupIntegrationTest.java`, `OutboxMpMigrationIntegrationTest.java`, `OutboxManagedDdlIntegrationTest.java`. Inspect actual Maven discovery. Failsafe PostgreSQL tests require `EGON_OUTBOX_TEST_POSTGRES_ENABLED=true` and Docker; default skips are not passing runtime proof. Prefer named Outbox Failsafe classes in an authorized isolated run so `-am verify` does not accidentally start unrelated dependency-module integrations. Source/compile/test evidence never authorizes or proves production migration.
