# Egon COLA Transactional Outbox Starter

[English](README.md) | [中文](README.zh-CN.md)

## What Problem This Solves

`egon-cola-component-transactional-outbox-starter` atomically stores a business change and
an outbound message in the same local PostgreSQL transaction, then delivers the
message asynchronously through HTTP, RabbitMQ, or a custom channel. If the
application stops after the database commit, polling recovers the stored message.

The component has one Maven module:

| Module | Purpose |
|---|---|
| `egon-cola-component-transactional-outbox-starter` | Public CQE outbox API, MP-backed PostgreSQL store, technical and optional business Statemachines, polling, retry, cleanup, delivery adapters, managed DDL, maintenance migration, metrics, and tests |

Unit tests remain in the regular component-package directories under
`src/test/java`; integration tests and executable samples are grouped
under `src/test/java/top/egon/cola/component/outbox/integration` and run
through Maven Failsafe.

The implementation uses the Transactional Outbox pattern. `TransactionalOutbox`
is the application-facing facade, while `DeliveryHandler` is the strategy/adapter
extension point for delivery channels.

## Delivery Guarantee

The guarantee is **at least once**, not exactly once:

1. `enqueue` stores the outbox row in the caller's active business transaction.
2. An after-commit event is only a low-latency wake-up hint.
3. PostgreSQL polling with `FOR UPDATE SKIP LOCKED` claims due or expired rows.
4. Delivery runs outside the database transaction.
5. Retry, lease recovery, and owner-conditioned updates move a row through
   `PENDING`, `PROCESSING`, `RETRY_WAIT`, `SUCCEEDED`, or `DEAD`.

A remote endpoint can complete successfully immediately before this application
crashes and records the local success. The same remote side effect can therefore
be attempted again. Every downstream consumer must deduplicate using the stable
`messageId`. HTTP sends it as `Idempotency-Key` and
`X-Egon-Cola-Message-Id`; RabbitMQ uses it as AMQP `messageId`.

An optional `idempotencyKey` prevents duplicate outbox rows while its row remains
in the table. Reusing the same key with different message content fails instead
of silently accepting a conflict. `availableAt` is intentionally excluded from
that content fingerprint.

## State Machines and CQE

The starter uses two independent Spring Statemachines with different owners:

| Machine | Owner | What it decides | Where the committed fact lives |
|---|---|---|---|
| Outbox lifecycle | This starter, always enabled | Whether an outbox message may be enqueued, claimed, reclaimed, retried, completed, or dead-lettered | The MP-backed `egon_outbox.egon_cola_outbox_message` row remains authoritative |
| Business event | The consuming application, opt-in | Whether an already-occurred business fact may advance one registered aggregate state | The application's business row and durable event receipt, written in one transaction |

The lifecycle machine has `__NEW__`, `PENDING`, `PROCESSING`, `RETRY_WAIT`,
`SUCCEEDED`, and `DEAD` states. It owns transition decisions; the database's
owner/version checks still decide which worker can commit. `DELIVERY_RETRYABLE`
selects `RETRY_WAIT` or `DEAD` from the attempt budget. The Store's explicit
`SCHEDULE_RETRY` operation only schedules a retry after the Dispatcher has chosen
that target.

CQE keeps the application boundary explicit:

| Role | Responsibility |
|---|---|
| `Command` | Validates and authorizes a state change, updates the business aggregate, and enqueues its resulting Event in the same local transaction |
| `Query` | Reads business facts only; it does not enter either state machine or publish an Event |
| `Event` | Records something that has already happened; the outbox reliably delivers it, while `OutboxCommittedEvent` is only a local wake-up hint |

### Optional Business Event Consumer

The `statemachine` delivery channel is disabled by default. Enable it only after
registering application-owned definitions and a persistence implementation for
each business aggregate:

```yaml
egon:
  cola:
    component:
      transactional-outbox:
        state-machine:
          execution-timeout: 100ms
          claim-evaluation-timeout: 1s
          business:
            enabled: true
            timeout: 5s
```

| Configuration key | Default | Meaning |
|---|---:|---|
| `egon.cola.component.transactional-outbox.state-machine.execution-timeout` | `100ms` | Maximum execution time for one in-memory transition |
| `egon.cola.component.transactional-outbox.state-machine.claim-evaluation-timeout` | `1s` | Budget for the technical claim decision |
| `egon.cola.component.transactional-outbox.state-machine.business.enabled` | `false` | Registers the optional business consumer only when true |
| `egon.cola.component.transactional-outbox.state-machine.business.timeout` | `5s` | Upper bound for one business transition/transaction |

The MyBatis-Plus store settings that govern the managed schema and explicit
legacy migration are:

| Configuration key | Default | Meaning |
|---|---:|---|
| `egon.cola.component.transactional-outbox.storage.mp.sql-session-factory-bean-name` | `sqlSessionFactory` | Existing Common MP `SqlSessionFactory` used by the Store |
| `egon.cola.component.transactional-outbox.storage.mp.migration-mode` | `false` | Blocks local enqueue and dispatch while maintenance migration is active |
| `egon.cola.component.transactional-outbox.storage.mp.migration-lock-timeout` | `30s` | Maximum wait for the source-table maintenance lock |
| `egon.cola.component.transactional-outbox.storage.mp.manifest-resource` | `db/egon-outbox-mp/manifest.json` | Managed DDL manifest loaded by Common MP |

With the adapter enabled, register one named
`BusinessStateMachineContextExecutor` bean named
`outboxBusinessStateMachineContextExecutor` and at least one
`BusinessStateMachineDefinitionStrategy`. Every definition provides one unique
destination in the form `machineKey:definitionVersion`, a fresh flat
single-region `StateMachineFactory<String, String>`, and an application-owned
`BusinessStateMachineRepository`. The definition validates its payload before
opening the business transaction. The factory must not return a cached/running
machine, use deferred events, or perform external side effects from state actions.

The repository must lock the active aggregate in the event's tenant, return its
authoritative state/version and receipt fingerprint, and atomically save the
new state/version with a receipt for every consumed `eventId`. Implement
`saveTransition` as a compare-and-set on the loaded version. The starter does
not create a business table or receipt table for the application.

The producer sends an occurred fact through the existing outbox inside its
Command transaction. `eventId` is the business receipt identity;
`OutboxMessage.messageId` is the transport identity and may differ:

```java
BusinessStateMachineEvent event = BusinessStateMachineEvent.builder()
        .eventId(eventId)
        .tenantId(tenantId)
        .machineKey("order-payment")
        .definitionVersion(1)
        .businessId(orderId)
        .eventType("PAYMENT_CONFIRMED")
        .expectedVersion(orderVersion)
        .occurredAt(clock.instant())
        .payload(objectMapper.createObjectNode().put("paymentReference", paymentReference))
        .build();

transactionalOutbox.enqueue(OutboxMessage.builder()
        .idempotencyKey("payment-confirmed:" + eventId)
        .channel("statemachine")
        .destination("order-payment:1")
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

The consumer checks the envelope and tenant before touching the aggregate. A
matching persisted `eventId` and fingerprint returns success before version
checks; the same identity with different content is a permanent conflict. A
future expected version is retryable, while a stale version is permanent. Any
database or transition failure rolls back the business state and receipt, then
the existing outbox retry/dead-letter flow applies. This remains at-least-once;
every external consumer still needs its own idempotency policy.

## When Not to Use It

Do not use this component when you require:

- exactly-once remote side effects without downstream deduplication;
- a distributed transaction across databases or external services;
- reactive/R2DBC transaction participation;
- one business transaction spanning a different database from the outbox table;
- an inbox, Admin UI, replay API, or broker topology declaration.

It targets Java 21, Spring Boot 3.5.x, PostgreSQL, and imperative Spring
transactions with the Egon MP store.
Redis is not required.

## Maven Dependency

Import the component BOM and add only the starter:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-components-bom</artifactId>
            <version>${egon-cola.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-transactional-outbox-starter</artifactId>
    </dependency>
</dependencies>
```

The starter depends on the Common MP extension and Spring Statemachine core.
The application must configure Common MP with one native PostgreSQL `PRIMARY`,
an existing MyBatis-Plus `SqlSessionFactory`, and a matching
`DataSourceTransactionManager`. Business writes and outbox enqueue must use that
same local database transaction. Add `spring-web` only for HTTP delivery and
`spring-rabbit` only for RabbitMQ delivery.

Component failures are raised from `top.egon.cola.component.outbox.common.exception`
and are rooted on the common `CommonException`, so `getCode()`, `getStatus()` and
`isRetryable()` stay uniform across starters. The starter publishes the canonical
`egonColaValidationUtils` bean only when the application does not already have it,
and injects it into its validators, which extend `BaseValidator` and keep their own
protocol checks.

## Managed MP Schema and Legacy Data Migration

The runtime store uses the Egon COLA MyBatis-Plus extension. The starter brings
that component transitively; it does not retain a JDBC fallback. Before first
startup, create the dedicated, empty PostgreSQL schema and grant the application
the required DDL permissions:

```sql
CREATE SCHEMA egon_outbox;
```

On startup, the starter's logical-datasource hook invokes the shared
`EgonColaPostgreDdlRunner` for the unique physical `PRIMARY` before the logical
datasource is created. The only managed outbox script is
`db/egon-outbox-mp/V20260924_001__initialize_outbox_mp_schema.sql`, checked by
`db/egon-outbox-mp/manifest.json` (`family: component-outbox`). The runner
creates/validates the message table and its `ddl_history`; it does not discover
or modify the legacy table. A non-empty schema without a valid managed history
fails closed. Do not copy this script to an application migration directory,
edit the packaged SQL, or repair a checksum by hand.

Configure Common MP for one native PostgreSQL `PRIMARY`, `LOCAL` transactions,
and two `SINGLE` routes to the managed tables. The physical datasource and
transaction-manager names below must refer to the same datasource used by the
business Command. Preserve the host's existing `ignored-tables` entries and
mapper locations when merging these settings:

```yaml
egon:
  cola:
    component:
      transactional-outbox:
        enabled: true
        storage:
          data-source-bean-name: orderDataSource
          transaction-manager-bean-name: orderTransactionManager
          validate-schema: true
          mp:
            sql-session-factory-bean-name: sqlSessionFactory
            migration-mode: false
            migration-lock-timeout: 30s
            manifest-resource: db/egon-outbox-mp/manifest.json
      mybatis-plus:
        enabled: true
        tenant-id:
          ignored-tables:
            - egon_cola_outbox_message
        ddl:
          enabled: true
        sharding:
          enabled: true
          mode: SHARDING
          config-style: NATIVE
          transaction-default-type: LOCAL
          native-rules-resource: classpath:sharding/outbox-native.yml
      cache:
        enabled: false # Only for a new host that has no cache requirement.
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
```

Keep physical datasource credentials in the host's secret configuration. The
unique `PRIMARY` name is host-defined; the native rules must route exactly these
two names to that same source and schema:

```yaml
databaseName: egon
rules:
  - !SINGLE
    tables:
      - primary.egon_outbox.egon_cola_outbox_message
      - primary.egon_outbox.ddl_history
transaction:
  defaultType: LOCAL
```

Replace the example `primary` with the actual configured `PRIMARY` name. Keep
the application's business-table rules, do not use `*.*.*`, and do not route
the legacy source schema through the runtime mapper. The example
`cache.enabled: false` applies only when the new host has no cache use; retain an
existing host's cache configuration. Keep the `mapper-locations` pattern broad
enough to include `mapper/outbox/OutboxMessageMapper.xml` or startup validation
fails.

If legacy rows exist, do not start cutover by copying the old `V1` file. Leave
`db/transactional-outbox/postgresql/V1__create_transactional_outbox_schema.sql`
unchanged and keep the old table in its original schema. The old schema is an
explicit, read-only migration source; it is not used by the runtime Store.

For a controlled migration, stop **all** old and new producers and workers and
wait for in-flight transactions to finish. Then set `migration-mode: true` on
every new instance. That local flag refuses new `enqueue`, `submitDue`, and
`submitMessageIds` calls; it cannot stop another replica or an older process.
After managed DDL is ready, invoke the local `OutboxLegacyMigrationService`
maintenance Bean with the explicit legacy source schema:

```java
OutboxMigrationResult result = outboxLegacyMigrationService.migrate(
        "legacy_outbox", 100, false);
if (!result.isVerified()) {
    throw new IllegalStateException("Do not switch traffic before verifying every outbox row");
}
```

`verifyOnly: true` performs a read-only comparison. Copy mode preserves all 22
legacy message columns, including raw payload/header strings, identity,
fingerprint, five-state value, attempts, owner, and timestamps. The target adds
technical tenant `0`, migration audit identity, `deleted_at = NULL`, and
`version = 0`. Each batch commits independently. An interrupted or unknown
commit can be retried explicitly: exact matching identities are skipped, while
different IDs, message IDs, keys, or contents stop the migration without
overwriting data. `verified` means a complete source/target comparison found no
missing, extra, or different row; a count alone is not sufficient. The service
never runs on startup, clears the source, or changes `migration-mode` for you.

Only after the report is verified and all new consumers/definitions are ready
should you deploy with `migration-mode: false` and enable producers. Keep the old
table and SQL history. Before the new table receives writes, a rollback can use
the previous application version and old table; after new writes begin, the old
table is stale and cannot be selected by changing configuration back. Stop
writes and reconcile forward before any later rollback.

## Direct API Example

The recommended API is explicit and keeps the business write and enqueue in one
imperative Spring transaction:

```java
@Service
public class OrderApplicationService {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionalOutbox transactionalOutbox;

    public OrderApplicationService(
            JdbcTemplate jdbcTemplate,
            TransactionalOutbox transactionalOutbox
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionalOutbox = transactionalOutbox;
    }

    @Transactional
    public OutboxReceipt createOrder(String orderId) {
        jdbcTemplate.update(
                "insert into business_order(id) values (?) on conflict do nothing",
                orderId
        );
        return transactionalOutbox.enqueue(OutboxMessage.builder()
                .idempotencyKey("order:created:" + orderId)
                .channel("order-events")
                .destination("order-created-v1")
                .payload(new OrderCreatedEvent(orderId))
                .schemaVersion("1")
                .build());
    }

    public record OrderCreatedEvent(String orderId) {
    }
}
```

`enqueue` requires an active, synchronization-enabled Spring transaction bound to
the configured outbox `DataSource`. It fails before insertion when no compatible
transaction is active.

## Annotation Example

`@TransactionalMessage` can derive one `OutboxMessage` from a synchronous method
result. Its advisor starts or joins a `REQUIRED` transaction with the configured
transaction manager:

```java
@Service
public class AnnotatedOrderApplicationService {

    private final JdbcTemplate jdbcTemplate;

    public AnnotatedOrderApplicationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @TransactionalMessage(message = "#result.outboxMessage()")
    public CreateOrderResult createOrder(String orderId) {
        jdbcTemplate.update("insert into business_order(id) values (?)", orderId);
        return new CreateOrderResult(
                orderId,
                OutboxMessage.builder()
                        .idempotencyKey("order:annotated-created:" + orderId)
                        .channel("order-events")
                        .destination("order-created-v1")
                        .payload(new OrderCreatedEvent(orderId))
                        .build()
        );
    }

    public record CreateOrderResult(String orderId, OutboxMessage outboxMessage) {
    }

    public record OrderCreatedEvent(String orderId) {
    }
}
```

The expression must resolve to a non-null `OutboxMessage`; otherwise the entire
transaction rolls back. The method must be a proxied Spring Bean method that is
public, non-static, non-final, and synchronous. `Future`, `CompletionStage`, and
reactive return types are rejected. If `@Transactional` is also present, it must
use `REQUIRED`, be writable, and select the same transaction manager. Standard
Spring proxy limitations such as self-invocation still apply.

## DataSource and MyBatis-Plus Selection

The outbox MP `SqlSessionFactory`, selected DataSource, and selected transaction
manager must all refer to the same native `PRIMARY`. The component rejects
multiple primaries, a mismatched factory DataSource, a non-Spring-managed MP
transaction factory, or an outbox table that is missing from the explicit
`SINGLE` routes. A single candidate can use the defaults; with multiple Spring
beans, name both DataSource and transaction manager explicitly:

```yaml
egon:
  cola:
    component:
      transactional-outbox:
        storage:
          data-source-bean-name: orderDataSource
          transaction-manager-bean-name: orderTransactionManager
```

Startup validation checks that the selected `DataSourceTransactionManager` owns
the selected Common MP DataSource and that the named `SqlSessionFactory` uses
that same datasource with `SpringManagedTransactionFactory`. The business write
and outbox write must use the same local PostgreSQL transaction; one logical
datasource name alone does not prove cross-database atomicity.

## Core Runtime Configuration

Defaults are intentionally finite and bounded:

```yaml
egon:
  cola:
    component:
      transactional-outbox:
        enabled: true
        polling:
          enabled: true
          fixed-delay: 1s
          batch-size: 100
          concurrency: 4
        delivery:
          timeout: 10s
          lease-duration: 60s
          queue-capacity: 1000
        retry:
          max-attempts: 10
          initial-delay: 1s
          multiplier: 2.0
          max-delay: 5m
          jitter: 0.2
        payload:
          max-bytes: 1MB
          max-header-count: 64
          max-header-bytes: 16KB
        shutdown:
          grace-period: 30s
```

Database time determines due rows and lease expiry. Every claim uses a unique
owner token containing the node identity and claim UUID; completion, retry, and
dead-letter transitions are compare-and-set operations for that owner.

## HTTP Delivery

HTTP is off by default. Messages use a logical destination, never an arbitrary
URL from the outbox row:

```yaml
egon:
  cola:
    component:
      transactional-outbox:
        http:
          enabled: true
          destinations:
            order-api:
              uri: https://service.example.internal/events/orders
              method: POST
              connect-timeout: 2s
              read-timeout: 10s
              fixed-headers:
                X-Event-Source: order-service
```

Enqueue with `channel("http")` and `destination("order-api")`. Redirects are not
followed. Use an `HttpCredentialProvider` Bean for credentials instead of persisting
headers in `OutboxMessage`: `Authorization`, `Proxy-Authorization`, `Cookie`,
`Set-Cookie`, `Host`, `Content-Length`, `Transfer-Encoding`, and `Connection` are
rejected during enqueue validation and are stripped again from outbound headers:

```java
@Bean
HttpCredentialProvider outboxHttpCredentials(TokenSupplier tokenSupplier) {
    return destination -> Map.of(
            HttpHeaders.AUTHORIZATION,
            "Bearer " + tokenSupplier.currentToken()
    );
}
```

The provider is called at delivery time. Payloads, credentials, full URLs, and
response bodies are not written to component logs.

## RabbitMQ Delivery

RabbitMQ is off by default and does not declare exchanges, queues, or bindings.
The consuming application must configure topology separately and enable
correlated publisher confirms, publisher returns, and mandatory publishing:

```yaml
spring:
  rabbitmq:
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true

egon:
  cola:
    component:
      transactional-outbox:
        rabbitmq:
          enabled: true
          confirm-timeout: 5s
          destinations:
            order-events:
              exchange: business.events
              routing-key: order.created.v1
              mandatory: true
```

Enqueue with `channel("rabbitmq")` and `destination("order-events")`. Rabbit
delivery succeeds only when the correlated publisher confirm is ACK and no
mandatory return was received. NACK, return, timeout, or connection failure is
classified for retry or DEAD handling. The stable `messageId` is retained across
attempts.

## Custom DeliveryHandler

Register one handler per custom channel. Destination validation happens during
enqueue, before the row is inserted:

```java
@Bean
DeliveryHandler orderEventsDeliveryHandler(OrderEventClient client) {
    return new DeliveryHandler() {
        @Override
        public String channel() {
            return "order-events";
        }

        @Override
        public void validateDestination(String destination) {
            if (!"order-created-v1".equals(destination)) {
                throw new OutboxValidationException("Unknown order destination");
            }
        }

        @Override
        public DeliveryResult deliver(DeliveryContext context) {
            return client.send(context.messageId(), context.payload())
                    ? DeliveryResult.success()
                    : DeliveryResult.retryableFailure(
                            "ORDER_DELIVERY_UNAVAILABLE",
                            "ORDER_DELIVERY_UNAVAILABLE"
                    );
        }
    };
}
```

The handler runs outside the business/database transaction. It must honor
`context.deadline()`, avoid unbounded waits, and return a permanent failure for
non-retryable input.

## Retry, Lease, DEAD, and Crash Recovery

Retry uses bounded exponential backoff with jitter. A retryable result moves the
row to `RETRY_WAIT`; exhausting `max-attempts`, a permanent failure, or an
unknown channel moves it to `DEAD`. `OutboxDeadLetterListener` Beans may observe
the transition, but listener failures do not undo the stored state.

If a worker crashes in `PROCESSING`, another worker can reclaim the row after
`locked_until`. A stale worker cannot overwrite a new owner because all terminal
updates compare the owner token. One poison message does not stop the rest of a
claimed batch.

## Cleanup and the Idempotency Window

Cleanup is disabled by default:

```yaml
egon:
  cola:
    component:
      transactional-outbox:
        cleanup:
          enabled: true
          success-retention: 7d
          fixed-delay: 1h
          batch-size: 500
```

Only old `SUCCEEDED` rows are deleted. `DEAD` rows are retained. Deleting a
successful row also ends the outbox-side deduplication window for its
`idempotencyKey`; downstream deduplication policy must be at least as long as
the business requires.

## Metrics and Safe Logging

If a Micrometer `MeterRegistry` and a `DataSource` bean are both present and the
application defines no `OutboxMetrics` bean of its own, the component records:

- `egon.cola.outbox.backlog`
- `egon.cola.outbox.enqueue`
- `egon.cola.outbox.claim`
- `egon.cola.outbox.delivery`
- `egon.cola.outbox.delivery.duration`
- `egon.cola.outbox.retry`
- `egon.cola.outbox.dead`
- `egon.cola.outbox.lease_lost`

Metric tags have bounded values such as channel and result; they do not use
message IDs, destinations, or error text. Logs and stored error summaries omit
payloads, credentials, `Authorization`, `Cookie`, response bodies, and full
URLs.

## PostgreSQL Integration Tests

The default Maven reactor does not connect to PostgreSQL on the developer
machine. To verify real PostgreSQL transaction, lease, migration copy/resume,
concurrent recovery, and query-plan behavior, make Docker available and run:

```bash
EGON_OUTBOX_TEST_POSTGRES_ENABLED=true ./mvnw -B -ntp \
  -pl :egon-cola-component-transactional-outbox-starter \
  -am clean verify
```

The suite uses an isolated PostgreSQL 16.6 Testcontainer and does not read local
PostgreSQL usernames or passwords. `OutboxMpMigrationIntegrationTest` is also
behind this gate: it is compiled by `test-compile` but requires this explicit
environment switch to execute. The GitHub CI `CI Backend` job sets
`EGON_OUTBOX_TEST_POSTGRES_ENABLED=true` for the whole reactor, so this suite always
runs there and never assume-skips; Docker or database startup failures fail the job
instead of silently skipping it.

## Explicit Scope

Supported: Java 21, Spring Boot 3.5.x, PostgreSQL, imperative Spring transactions
with the Egon MP store, technical lifecycle and opt-in business Statemachines,
HTTP delivery, RabbitMQ delivery, and custom synchronous handlers.

Unsupported: reactive/R2DBC transactions, cross-database transactions,
distributed transactions, exactly-once guarantees, automatic legacy data copy
or destructive rollback, Admin/UI, replay APIs, inbox processing, and broker
topology declaration.
