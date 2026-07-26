# Egon COLA Transactional Outbox

[English](README.md) | [中文](README.zh-CN.md)

## What Problem This Solves

`egon-cola-component-transactional-outbox` atomically stores a business change and
an outbound message in the same local PostgreSQL transaction, then delivers the
message asynchronously through HTTP, RabbitMQ, or a custom channel. If the
application stops after the database commit, polling recovers the stored message.

The component has two Maven modules:

| Module | Purpose |
|---|---|
| `egon-cola-component-transactional-outbox-starter` | Public API, PostgreSQL/JDBC store, polling, retry, cleanup, HTTP/RabbitMQ adapters, auto-configuration, and metrics |
| `egon-cola-component-transactional-outbox-test` | Component integration tests and exactly two executable consumer samples |

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

## When Not to Use It

Do not use this component when you require:

- exactly-once remote side effects without downstream deduplication;
- a distributed transaction across databases or external services;
- reactive/R2DBC transaction participation;
- one business transaction spanning a different database from the outbox table;
- an inbox, Admin UI, replay API, or broker topology declaration.

It targets Java 21, Spring Boot 3.5.x, PostgreSQL, and imperative Spring JDBC.
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

The application must also provide one `ObjectMapper`, a JDBC `DataSource`, and
its matching `PlatformTransactionManager`. Add `spring-web` only for HTTP
delivery and `spring-rabbit` only for RabbitMQ delivery.

## PostgreSQL Migration

Introducing the starter **does not create its table** and does not run Flyway.
Copy:

```text
egon-cola-component-transactional-outbox-starter/src/main/resources/
db/transactional-outbox/postgresql/V1__create_transactional_outbox_schema.sql
```

into the consuming application's own `classpath:db` migration sequence. Rename
the copy to the application's next local Flyway version, for example
`V42__create_transactional_outbox_schema.sql`. Do not place the packaged file in
an auto-discovered migration location without assigning a consumer-owned
version.

The startup schema validator is enabled by default and fails fast if the expected
table shape is absent. It can be disabled only when schema validation is handled
elsewhere:

```yaml
egon:
  cola:
    component:
      transactional-outbox:
        storage:
          validate-schema: false
```

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

## DataSource Selection

With one `DataSource` and one transaction manager, no explicit selection is
needed. If multiple candidates exist, the component chooses a single `@Primary`
candidate; otherwise name both Beans explicitly:

```yaml
egon:
  cola:
    component:
      transactional-outbox:
        storage:
          data-source-bean-name: orderDataSource
          transaction-manager-bean-name: orderTransactionManager
```

For `DataSourceTransactionManager`, startup validation also checks that the
selected manager owns the selected `DataSource`. The business write and outbox
write must use this same database transaction.

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
followed. Use an `HttpCredentialProvider` Bean for credentials rather than
persisting `Authorization` or `Cookie` headers in `OutboxMessage`:

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

If exactly one Micrometer `MeterRegistry` is present, the component records:

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
machine. To verify real PostgreSQL transaction, lease, concurrent recovery,
and query-plan behavior, make Docker available and run:

```bash
EGON_OUTBOX_TEST_POSTGRES_ENABLED=true ./mvnw -B -ntp \
  -pl :egon-cola-component-transactional-outbox-test \
  -am clean verify
```

The suite uses an isolated PostgreSQL 16.6 Testcontainer and does not read local
PostgreSQL usernames or passwords. The GitHub CI `Outbox PostgreSQL` job always
enables this suite; Docker or database startup failures fail the job instead of
silently skipping it.

## Explicit Scope

Supported: Java 21, Spring Boot 3.5.x, PostgreSQL, imperative JDBC transactions,
HTTP delivery, RabbitMQ delivery, and custom synchronous handlers.

Unsupported: reactive/R2DBC transactions, cross-database transactions,
distributed transactions, exactly-once guarantees, automatic Flyway execution,
Admin/UI, replay APIs, inbox processing, and broker topology declaration.
