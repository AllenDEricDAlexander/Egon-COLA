# Egon-COLA Transactional Outbox Component Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a production-oriented Egon-COLA Transactional Outbox Spring Boot Starter that atomically stores business messages in PostgreSQL and delivers them after commit through HTTP, RabbitMQ, or a custom handler with bounded retry and crash recovery.

**Architecture:** The component follows the approved `root + starter + test` shape. `TransactionalOutbox` is the business-facing Facade; PostgreSQL is the durable source of truth; short `FOR UPDATE SKIP LOCKED` transactions claim work; a bounded dispatcher invokes Strategy/Adapter `DeliveryHandler` implementations only after business commit. Spring Event is used only as a low-latency wake-up signal, while polling, leases, owner-conditioned updates, and stable message IDs provide recovery and at-least-once delivery.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Maven 3.9.14+, Spring JDBC/Transactions/AOP, PostgreSQL, Jackson, optional Spring Web `RestClient`, optional Spring AMQP `RabbitTemplate`, optional Micrometer, JUnit 5, Testcontainers, WireMock, Awaitility.

## Global Constraints

- Implement the approved design in `docs/superpowers/specs/2026-07-24-transactional-outbox-component-design.md`; a task may clarify implementation detail but may not weaken a Spec invariant.
- Use project version `5.2.3`, Java 21, Spring Boot 3.5.16, and package root `top.egon.cola.component.outbox`.
- Use configuration prefix `egon.cola.component.transactional-outbox`.
- The component root aggregates exactly `egon-cola-component-transactional-outbox-starter` and `egon-cola-component-transactional-outbox-test`.
- The components BOM exports only `top.egon:egon-cola-component-transactional-outbox-starter`.
- V1 supports PostgreSQL and imperative Spring JDBC transactions only; do not add MySQL, SQLite, JPA persistence, or R2DBC implementations.
- The starter must not transitively provide PostgreSQL Driver, Flyway, Actuator, Redis, Testcontainers, or another MQ client.
- `spring-web`, `spring-rabbit`, and `micrometer-core` are optional starter dependencies and their auto-configurations must be isolated behind classpath conditions.
- Add exactly one new SQL resource at `db/transactional-outbox/postgresql/V1__create_transactional_outbox_schema.sql`; never modify an existing migration.
- The SQL resource is a copy-and-renumber template and must not live under Boot's default `db/migration` location or execute automatically.
- Do not modify any archetype, existing domain-event example, RabbitMQ example, or `verify.groovy`.
- Delivery is at-least-once. Keep `messageId` stable across retries and require downstream idempotency; never describe the component as exactly-once.
- Persist payload only in the dedicated outbox payload column and never log it;
  never persist in an outbox row or log Authorization, Cookie, credentials, HTTP
  response bodies, or full destination URLs.
- Use database time for claim, lease, retry, completion, and cleanup comparisons.
- Do not hold a database row lock or business transaction while making HTTP or RabbitMQ calls.
- All state completion updates must match `id + PROCESSING + locked_by`; a stale owner must not overwrite a reclaimed record.
- HTTP and RabbitMQ handlers remain disabled until explicitly enabled.
- Cleanup remains disabled by default and deletes only expired `SUCCEEDED` rows; `DEAD` rows are retained.
- Keep each task TDD-sized and create exactly one commit at the end of each task.
- Preserve unrelated working-tree changes. Do not start a long-running application, open a browser, or change external infrastructure during verification.

---

## Source Spec and Execution Boundary

Implementation is governed by:

```text
docs/superpowers/specs/2026-07-24-transactional-outbox-component-design.md
```

Before Task 1, create or select an isolated worktree through
`superpowers:using-git-worktrees`. Execute Tasks 1–14 in order. A later task may use only
the interfaces listed in its `Consumes` block; if an earlier task changes one of those
interfaces, update every later call site in the same task before committing.

## Design Pattern Decisions

- **Transactional Outbox:** required because a local database transaction cannot
  atomically commit an HTTP or broker side effect. PostgreSQL persistence plus lease
  recovery turns that gap into explicit at-least-once state.
- **Facade:** `TransactionalOutbox` keeps business code independent from
  serialization, transaction guards, fingerprinting, JDBC, and post-commit wake-up.
- **Strategy + Adapter:** `DeliveryHandler` isolates channel variation; HTTP and
  Rabbit implementations adapt their framework APIs without conditionals in the
  dispatcher.
- **Observer, deliberately limited:** Spring Event wakes the dispatcher only after
  commit. PostgreSQL remains the reliable source of truth.
- **Rejected as unnecessary:** State-class hierarchies, Chain of Responsibility,
  Abstract Factory, and deep Template Method inheritance add indirection without a
  real V1 variation point. Enum state plus owner-conditioned SQL and direct
  dispatcher orchestration are clearer and match the Spec.

## File Structure

Create the component reactor and documentation:

```text
egon-cola-components/egon-cola-component-transactional-outbox/
├── pom.xml
├── README.md
├── README.zh-CN.md
├── egon-cola-component-transactional-outbox-starter/
│   ├── pom.xml
│   └── src/
└── egon-cola-component-transactional-outbox-test/
    ├── pom.xml
    └── src/test/java/top/egon/cola/component/outbox/test/
```

Create these starter production areas:

```text
src/main/java/top/egon/cola/component/outbox/
├── annotation/
├── aop/
├── api/
├── autoconfigure/
├── cleanup/
├── deadletter/
├── delivery/
│   ├── http/
│   └── rabbitmq/
├── dispatch/
├── event/
├── exception/
├── observability/
├── retry/
├── serialization/
├── store/
├── transaction/
└── validation/
```

The complete production file ownership is locked as follows:

```text
api/TransactionalOutbox.java
api/OutboxMessage.java
api/OutboxReceipt.java
api/OutboxIdGenerator.java
api/UuidOutboxIdGenerator.java
annotation/TransactionalMessage.java
aop/TransactionalMessageAop.java
aop/OutboxMessageExpressionResolver.java
aop/TransactionalMessageMethodValidator.java
autoconfigure/TransactionalOutboxAutoConfiguration.java
autoconfigure/OutboxHttpAutoConfiguration.java
autoconfigure/OutboxRabbitAutoConfiguration.java
autoconfigure/OutboxMetricsAutoConfiguration.java
autoconfigure/TransactionalOutboxProperties.java
autoconfigure/OutboxConfigurationValidator.java
autoconfigure/OutboxInfrastructure.java
autoconfigure/OutboxInfrastructureResolver.java
cleanup/OutboxCleanupJob.java
deadletter/OutboxDeadLetterListener.java
deadletter/OutboxDeadLetterNotifier.java
delivery/DeliveryHandler.java
delivery/DeliveryHandlerRegistry.java
delivery/DeliveryContext.java
delivery/DeliveryResult.java
delivery/DeliveryFailureClassifier.java
delivery/DefaultDeliveryFailureClassifier.java
delivery/http/HttpDeliveryHandler.java
delivery/http/HttpDeliveryTarget.java
delivery/http/HttpDestinationResolver.java
delivery/http/PropertiesHttpDestinationResolver.java
delivery/http/HttpCredentialProvider.java
delivery/http/HttpDeliveryClassifier.java
delivery/http/DefaultHttpDeliveryClassifier.java
delivery/rabbitmq/RabbitDeliveryHandler.java
delivery/rabbitmq/RabbitDeliveryTarget.java
delivery/rabbitmq/RabbitDestinationResolver.java
delivery/rabbitmq/PropertiesRabbitDestinationResolver.java
delivery/rabbitmq/RabbitMessagePublisher.java
delivery/rabbitmq/RabbitTemplateMessagePublisher.java
delivery/rabbitmq/RabbitPublishOutcome.java
dispatch/OutboxDispatcher.java
dispatch/OutboxPoller.java
dispatch/OutboxWorkerIdentity.java
event/OutboxCommittedEvent.java
event/OutboxCommittedEventListener.java
event/OutboxDeadLetterEvent.java
observability/OutboxMetrics.java
observability/NoopOutboxMetrics.java
observability/MicrometerOutboxMetrics.java
retry/OutboxRetryPolicy.java
retry/ExponentialJitterRetryPolicy.java
serialization/OutboxMessageSerializer.java
serialization/SerializedOutboxPayload.java
serialization/JacksonOutboxMessageSerializer.java
serialization/OutboxMessageFingerprint.java
store/NewOutboxRecord.java
store/OutboxRecord.java
store/OutboxStatus.java
store/OutboxStore.java
store/PostgresqlJdbcOutboxStore.java
store/OutboxSchemaValidator.java
transaction/DefaultTransactionalOutbox.java
transaction/OutboxTransactionGuard.java
transaction/OutboxAfterCommitBuffer.java
validation/OutboxMessageValidator.java
exception/OutboxException.java
exception/OutboxConfigurationException.java
exception/OutboxValidationException.java
exception/OutboxTransactionRequiredException.java
exception/OutboxTransactionSynchronizationException.java
exception/OutboxTransactionMismatchException.java
exception/OutboxSerializationException.java
exception/OutboxIdempotencyConflictException.java
exception/OutboxMessageResolutionException.java
exception/OutboxStorageException.java
```

Create these resources:

```text
egon-cola-component-transactional-outbox-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
egon-cola-component-transactional-outbox-starter/src/main/resources/db/transactional-outbox/postgresql/V1__create_transactional_outbox_schema.sql
```

Unit tests live beside the starter. PostgreSQL, HTTP, RabbitMQ, auto-configuration,
and two usage samples live in the test module. Each task below names its exact test files.

### Task 1: Register the Maven Reactor and Dependency Boundaries

**Files:**
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/pom.xml`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/pom.xml`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test/pom.xml`
- Modify: `egon-cola-components/pom.xml`
- Modify: `egon-cola-components/egon-cola-components-bom/pom.xml`

**Interfaces:**
- Consumes: repository parent `top.egon:egon-cola-components-parent:5.2.3`.
- Produces: starter artifact `top.egon:egon-cola-component-transactional-outbox-starter:5.2.3` and test artifact `top.egon:egon-cola-component-transactional-outbox-test:5.2.3`.

- [ ] **Step 1: Prove the component is absent before scaffolding**

Run:

```bash
test ! -e egon-cola-components/egon-cola-component-transactional-outbox
```

Expected: exit code 0.

- [ ] **Step 2: Create the component root POM**

Create `egon-cola-components/egon-cola-component-transactional-outbox/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-components-parent</artifactId>
        <version>5.2.3</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>egon-cola-component-transactional-outbox</artifactId>
    <packaging>pom</packaging>
    <name>egon-cola-component-transactional-outbox</name>
    <description>Transactional outbox component for Egon COLA.</description>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>egon-cola-component-transactional-outbox-starter</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <modules>
        <module>egon-cola-component-transactional-outbox-starter</module>
        <module>egon-cola-component-transactional-outbox-test</module>
    </modules>
</project>
```

- [ ] **Step 3: Create the starter POM**

Create `egon-cola-component-transactional-outbox-starter/pom.xml` under the component root:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-transactional-outbox</artifactId>
        <version>5.2.3</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>egon-cola-component-transactional-outbox-starter</artifactId>
    <packaging>jar</packaging>
    <name>egon-cola-component-transactional-outbox-starter</name>
    <description>Spring Boot starter for Egon COLA transactional outbox.</description>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-jdbc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-tx</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-web</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.amqp</groupId>
            <artifactId>spring-rabbit</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-core</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 4: Create the test-module POM**

Create `egon-cola-component-transactional-outbox-test/pom.xml` under the component root:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-transactional-outbox</artifactId>
        <version>5.2.3</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>egon-cola-component-transactional-outbox-test</artifactId>
    <packaging>jar</packaging>
    <name>egon-cola-component-transactional-outbox-test</name>
    <description>Sample and integration tests for Egon COLA transactional outbox.</description>

    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-component-transactional-outbox-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>rabbitmq</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.wiremock</groupId>
            <artifactId>wiremock-standalone</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.awaitility</groupId>
            <artifactId>awaitility</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 5: Register the component root and export only the starter**

Add this module after `egon-cola-component-method-extension` in
`egon-cola-components/pom.xml`:

```xml
<module>egon-cola-component-transactional-outbox</module>
```

Add this dependency after `egon-cola-component-method-extension-starter` in
`egon-cola-components/egon-cola-components-bom/pom.xml`:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-transactional-outbox-starter</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 6: Validate the three-project reactor**

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/egon-cola-component-transactional-outbox/pom.xml validate
```

Expected: `BUILD SUCCESS`; the reactor summary contains exactly the component root,
starter, and test modules.

- [ ] **Step 7: Commit the reactor registration**

```bash
git add egon-cola-components/pom.xml \
  egon-cola-components/egon-cola-components-bom/pom.xml \
  egon-cola-components/egon-cola-component-transactional-outbox
git commit -m "feat: register transactional outbox component"
```

### Task 2: Define the Public Enqueue, Serialization, Validation, and Fingerprint Contracts

**Files:**
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/api/TransactionalOutbox.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/api/OutboxMessage.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/api/OutboxReceipt.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/api/OutboxIdGenerator.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/api/UuidOutboxIdGenerator.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/serialization/OutboxMessageSerializer.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/serialization/SerializedOutboxPayload.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/serialization/JacksonOutboxMessageSerializer.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/serialization/OutboxMessageFingerprint.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/validation/OutboxMessageValidator.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/exception/OutboxException.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/exception/OutboxValidationException.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/exception/OutboxSerializationException.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/api/OutboxMessageTest.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/serialization/JacksonOutboxMessageSerializerTest.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/serialization/OutboxMessageFingerprintTest.java`

**Interfaces:**
- Consumes: Jackson application `ObjectMapper`.
- Produces: `TransactionalOutbox.enqueue(OutboxMessage)`, immutable `OutboxMessage`, `OutboxReceipt`, serializer, validation, and deterministic SHA-256 fingerprint.

- [ ] **Step 1: Write failing public-contract tests**

Create `OutboxMessageTest.java` with focused tests equivalent to:

```java
package top.egon.cola.component.outbox.api;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.outbox.exception.OutboxValidationException;
import top.egon.cola.component.outbox.validation.OutboxMessageValidator;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxMessageTest {

    private final OutboxMessageValidator validator =
            new OutboxMessageValidator(new ObjectMapper(), 1024 * 1024, 64, 16 * 1024);

    @Test
    void shouldBuildImmutableMessageWithDefaults() {
        OutboxMessage message = OutboxMessage.builder()
                .channel("rabbitmq")
                .destination("order-created")
                .payload(Map.of("orderId", "O-1"))
                .header("X-Tenant", "tenant-a")
                .availableAt(Instant.parse("2026-07-24T00:00:00Z"))
                .build();

        validator.validateEnvelope(message);

        assertThat(message.contentType()).isEqualTo("application/json");
        assertThat(message.headers()).containsEntry("X-Tenant", "tenant-a");
        assertThatThrownBy(() -> message.headers().put("X-Test", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectSensitiveAndTransportHeadersCaseInsensitively() {
        OutboxMessage message = OutboxMessage.builder()
                .channel("http")
                .destination("order-callback")
                .payload("{}")
                .header("authorization", "secret")
                .build();

        assertThatThrownBy(() -> validator.validateEnvelope(message))
                .isInstanceOf(OutboxValidationException.class)
                .hasMessageContaining("authorization");
    }

    @Test
    void shouldRejectMissingChannelDestinationAndPayload() {
        OutboxMessage message = OutboxMessage.builder().build();

        assertThatThrownBy(() -> validator.validateEnvelope(message))
                .isInstanceOf(OutboxValidationException.class);
    }
}
```

Create `JacksonOutboxMessageSerializerTest.java`:

```java
package top.egon.cola.component.outbox.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.outbox.exception.OutboxSerializationException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JacksonOutboxMessageSerializerTest {

    private final JacksonOutboxMessageSerializer serializer =
            new JacksonOutboxMessageSerializer(new ObjectMapper());

    @Test
    void shouldSerializeObjectWithApplicationObjectMapper() {
        SerializedOutboxPayload serialized =
                serializer.serialize(new SamplePayload("O-1"), "application/json");

        assertThat(serialized.text()).isEqualTo("{\"orderId\":\"O-1\"}");
        assertThat(serialized.utf8Bytes())
                .isEqualTo(serialized.text().getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    void shouldPreserveCompatibleStringPayload() {
        assertThat(serializer.serialize("{\"orderId\":\"O-1\"}", "application/json").text())
                .isEqualTo("{\"orderId\":\"O-1\"}");
        assertThat(serializer.serialize("plain", "text/plain").text()).isEqualTo("plain");
    }

    @Test
    void shouldWrapJacksonFailureWithoutIncludingPayload() {
        assertThatThrownBy(() -> serializer.serialize(new SelfReference(), "application/json"))
                .isInstanceOf(OutboxSerializationException.class)
                .hasMessage("Failed to serialize outbox payload");
    }

    record SamplePayload(String orderId) {
    }

    static final class SelfReference {
        public SelfReference getSelf() {
            return this;
        }
    }
}
```

Create `OutboxMessageFingerprintTest.java`:

```java
package top.egon.cola.component.outbox.serialization;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxMessageFingerprintTest {

    @Test
    void shouldIgnoreHeaderOrderAndSchedulingMetadata() {
        Map<String, String> first = new LinkedHashMap<>();
        first.put("X-B", "2");
        first.put("X-A", "1");
        Map<String, String> second = new LinkedHashMap<>();
        second.put("X-A", "1");
        second.put("X-B", "2");

        String left = OutboxMessageFingerprint.sha256(
                "http", "order-callback", "{}", "application/json", "1", first);
        String right = OutboxMessageFingerprint.sha256(
                "http", "order-callback", "{}", "application/json", "1", second);

        assertThat(left).isEqualTo(right).hasSize(64);
    }

    @Test
    void shouldChangeWhenPersistedContentChanges() {
        String left = OutboxMessageFingerprint.sha256(
                "http", "order-callback", "{\"v\":1}", "application/json", "1", Map.of());
        String right = OutboxMessageFingerprint.sha256(
                "http", "order-callback", "{\"v\":2}", "application/json", "1", Map.of());

        assertThat(left).isNotEqualTo(right);
    }
}
```

- [ ] **Step 2: Run the tests and verify the missing contracts fail compilation**

Run:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter \
  -am test \
  -Dtest=OutboxMessageTest,JacksonOutboxMessageSerializerTest,OutboxMessageFingerprintTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: Maven fails because the new API and serialization classes do not exist.

- [ ] **Step 3: Implement the immutable public API**

Create these exact public signatures:

```java
package top.egon.cola.component.outbox.api;

public interface TransactionalOutbox {

    OutboxReceipt enqueue(OutboxMessage message);
}
```

```java
package top.egon.cola.component.outbox.api;

public record OutboxReceipt(
        String messageId,
        String idempotencyKey,
        boolean created
) {
}
```

```java
package top.egon.cola.component.outbox.api;

public interface OutboxIdGenerator {

    String nextId();
}
```

```java
package top.egon.cola.component.outbox.api;

import java.util.UUID;

public class UuidOutboxIdGenerator implements OutboxIdGenerator {

    @Override
    public String nextId() {
        return UUID.randomUUID().toString();
    }
}
```

Implement `OutboxMessage` as a final immutable class with this complete surface:

```java
package top.egon.cola.component.outbox.api;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OutboxMessage {

    private final String messageId;
    private final String idempotencyKey;
    private final String channel;
    private final String destination;
    private final Object payload;
    private final String contentType;
    private final String schemaVersion;
    private final Map<String, String> headers;
    private final Instant availableAt;
    private final String traceId;

    private OutboxMessage(Builder builder) {
        this.messageId = builder.messageId;
        this.idempotencyKey = builder.idempotencyKey;
        this.channel = builder.channel;
        this.destination = builder.destination;
        this.payload = builder.payload;
        this.contentType = builder.contentType;
        this.schemaVersion = builder.schemaVersion;
        this.headers = Map.copyOf(builder.headers);
        this.availableAt = builder.availableAt;
        this.traceId = builder.traceId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String messageId() {
        return messageId;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public String channel() {
        return channel;
    }

    public String destination() {
        return destination;
    }

    public Object payload() {
        return payload;
    }

    public String contentType() {
        return contentType;
    }

    public String schemaVersion() {
        return schemaVersion;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public Instant availableAt() {
        return availableAt;
    }

    public String traceId() {
        return traceId;
    }

    public static final class Builder {

        private String messageId;
        private String idempotencyKey;
        private String channel;
        private String destination;
        private Object payload;
        private String contentType = "application/json";
        private String schemaVersion;
        private final Map<String, String> headers = new LinkedHashMap<>();
        private Instant availableAt;
        private String traceId;

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public Builder channel(String channel) {
            this.channel = channel;
            return this;
        }

        public Builder destination(String destination) {
            this.destination = destination;
            return this;
        }

        public Builder payload(Object payload) {
            this.payload = payload;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder schemaVersion(String schemaVersion) {
            this.schemaVersion = schemaVersion;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers.clear();
            if (headers != null) {
                this.headers.putAll(headers);
            }
            return this;
        }

        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public Builder availableAt(Instant availableAt) {
            this.availableAt = availableAt;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public OutboxMessage build() {
            return new OutboxMessage(this);
        }
    }
}
```

- [ ] **Step 4: Implement validation, serialization, and deterministic fingerprinting**

Create `OutboxException` as a `RuntimeException` with message and message/cause
constructors. Make `OutboxValidationException` and `OutboxSerializationException`
extend it with the same constructors.

Implement `OutboxMessageValidator` with these exact limits and rules:

```java
private static final Set<String> FORBIDDEN_HEADERS = Set.of(
        "authorization", "proxy-authorization", "cookie", "set-cookie",
        "host", "content-length", "transfer-encoding", "connection"
);

public void validateEnvelope(OutboxMessage message);

public void validateSerialized(SerializedOutboxPayload payload, Map<String, String> headers);
```

Construct the validator with the application `ObjectMapper`, maximum payload bytes,
maximum header count, and maximum serialized-header bytes. `validateEnvelope`
rejects null messages; blank or over-limit `messageId` (64),
`idempotencyKey` (256), `channel` (64), `destination` (256), `contentType` (128),
`schemaVersion` (32), and `traceId` (128); null payloads; null header names/values;
forbidden headers case-insensitively; and more than the configured header count.
`validateSerialized` uses the injected mapper on `new TreeMap<>(headers)` and rejects
payload or canonical JSON headers whose UTF-8 byte sizes exceed the configured
limits. Expose `public static boolean isForbiddenHeader(String name)` so property
validation uses the identical denylist. Exception messages identify only the field,
never its value.

Create the serializer contracts:

```java
package top.egon.cola.component.outbox.serialization;

public interface OutboxMessageSerializer {

    SerializedOutboxPayload serialize(Object payload, String contentType);
}
```

```java
package top.egon.cola.component.outbox.serialization;

public record SerializedOutboxPayload(String text, int utf8Bytes) {
}
```

`JacksonOutboxMessageSerializer` must retain the injected application `ObjectMapper`.
It returns a raw `String` for `text/*`, `application/json`, `application/*+json`,
`application/xml`, and `application/*+xml`; all other payloads use
`objectMapper.writeValueAsString`. Calculate byte length with `StandardCharsets.UTF_8`
and wrap `JsonProcessingException` in `OutboxSerializationException` with exactly
`Failed to serialize outbox payload`.

Implement this fingerprint signature:

```java
public final class OutboxMessageFingerprint {

    public static String sha256(
            String channel,
            String destination,
            String payload,
            String contentType,
            String schemaVersion,
            Map<String, String> headers
    );
}
```

Feed every nullable string to SHA-256 as a four-byte length followed by UTF-8 bytes;
feed headers from a `TreeMap` as length-prefixed key/value pairs. Return lowercase hex.
Do not accept or hash `messageId`, `idempotencyKey`, `availableAt`, or `traceId`.

- [ ] **Step 5: Run the focused contract tests**

Run the Step 2 Maven command again.

Expected: `BUILD SUCCESS`; all three test classes pass.

- [ ] **Step 6: Commit the public contracts**

```bash
git add egon-cola-components/egon-cola-component-transactional-outbox
git commit -m "feat: add transactional outbox message contracts"
```

### Task 3: Bind and Validate the Complete Configuration Model

**Files:**
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxProperties.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxConfigurationValidator.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/exception/OutboxConfigurationException.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxPropertiesTest.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/OutboxConfigurationValidatorTest.java`

**Interfaces:**
- Consumes: Spring Boot `Duration`, `DataSize`, and configuration-property binding.
- Produces: the single authoritative properties tree and fail-fast validation used by every later auto-configuration.

- [ ] **Step 1: Write failing default, binding, and invariant tests**

Create a test configuration with
`@EnableConfigurationProperties(TransactionalOutboxProperties.class)` and use
`ApplicationContextRunner` to assert:

```java
@Test
void shouldExposeSafeDefaults() {
    contextRunner.run(context -> {
        TransactionalOutboxProperties properties =
                context.getBean(TransactionalOutboxProperties.class);

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getAnnotation().isEnabled()).isTrue();
        assertThat(properties.getAnnotation().getOrder())
                .isEqualTo(Ordered.HIGHEST_PRECEDENCE + 100);
        assertThat(properties.getPolling().getFixedDelay()).isEqualTo(Duration.ofSeconds(1));
        assertThat(properties.getPolling().getBatchSize()).isEqualTo(100);
        assertThat(properties.getPolling().getConcurrency()).isEqualTo(4);
        assertThat(properties.getDelivery().getTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(properties.getDelivery().getLeaseDuration()).isEqualTo(Duration.ofSeconds(60));
        assertThat(properties.getRetry().getMaxAttempts()).isEqualTo(10);
        assertThat(properties.getHttp().isEnabled()).isFalse();
        assertThat(properties.getRabbitmq().isEnabled()).isFalse();
        assertThat(properties.getCleanup().isEnabled()).isFalse();
    });
}

@Test
void shouldBindNestedDestinationsAndBeanNames() {
    contextRunner.withPropertyValues(
            "egon.cola.component.transactional-outbox.storage.data-source-bean-name=businessDataSource",
            "egon.cola.component.transactional-outbox.storage.transaction-manager-bean-name=businessTx",
            "egon.cola.component.transactional-outbox.http.enabled=true",
            "egon.cola.component.transactional-outbox.http.destinations.order-callback.uri=https://orders.test/callback",
            "egon.cola.component.transactional-outbox.http.destinations.order-callback.method=PUT",
            "egon.cola.component.transactional-outbox.rabbitmq.destinations.order-created.exchange=order.events",
            "egon.cola.component.transactional-outbox.rabbitmq.destinations.order-created.routing-key=order.created"
    ).run(context -> {
        TransactionalOutboxProperties properties =
                context.getBean(TransactionalOutboxProperties.class);

        assertThat(properties.getStorage().getDataSourceBeanName())
                .isEqualTo("businessDataSource");
        assertThat(properties.getHttp().getDestinations())
                .containsKey("order-callback");
        assertThat(properties.getRabbitmq().getDestinations())
                .containsKey("order-created");
    });
}
```

Create `OutboxConfigurationValidatorTest` with direct property objects and these
failure assertions:

```java
@Test
void shouldRejectLeaseThatCannotCoverDeliveryWindow() {
    TransactionalOutboxProperties properties = new TransactionalOutboxProperties();
    properties.getDelivery().setTimeout(Duration.ofSeconds(10));
    properties.getDelivery().setLeaseDuration(Duration.ofSeconds(11));
    properties.getPolling().setFixedDelay(Duration.ofSeconds(1));

    assertThatThrownBy(() -> new OutboxConfigurationValidator().validate(properties))
            .isInstanceOf(OutboxConfigurationException.class)
            .hasMessageContaining("lease-duration");
}

@Test
void shouldRejectInvalidRetryAndCapacityRanges() {
    TransactionalOutboxProperties properties = new TransactionalOutboxProperties();
    properties.getRetry().setJitter(1.1);

    assertThatThrownBy(() -> new OutboxConfigurationValidator().validate(properties))
            .isInstanceOf(OutboxConfigurationException.class)
            .hasMessageContaining("retry.jitter");
}
```

- [ ] **Step 2: Run the tests and verify they fail**

Run:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter \
  -am test \
  -Dtest=TransactionalOutboxPropertiesTest,OutboxConfigurationValidatorTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: compilation fails because the properties and validator do not exist.

- [ ] **Step 3: Implement the exact configuration tree**

Create `TransactionalOutboxProperties` with
`@ConfigurationProperties(prefix = "egon.cola.component.transactional-outbox")`.
Use explicit getters and setters, mutable nested property objects, and these exact
fields/defaults:

```java
private boolean enabled = true;
private String nodeId;
private final Annotation annotation = new Annotation();
private final Storage storage = new Storage();
private final Polling polling = new Polling();
private final Delivery delivery = new Delivery();
private final Retry retry = new Retry();
private final Payload payload = new Payload();
private final Http http = new Http();
private final Rabbitmq rabbitmq = new Rabbitmq();
private final Cleanup cleanup = new Cleanup();
private final Shutdown shutdown = new Shutdown();

public static class Annotation {
    private boolean enabled = true;
    private int order = Ordered.HIGHEST_PRECEDENCE + 100;
}

public static class Storage {
    private String dataSourceBeanName;
    private String transactionManagerBeanName;
    private boolean validateSchema = true;
}

public static class Polling {
    private boolean enabled = true;
    private Duration fixedDelay = Duration.ofSeconds(1);
    private int batchSize = 100;
    private int concurrency = 4;
}

public static class Delivery {
    private Duration timeout = Duration.ofSeconds(10);
    private Duration leaseDuration = Duration.ofSeconds(60);
    private int queueCapacity = 1000;
}

public static class Retry {
    private int maxAttempts = 10;
    private Duration initialDelay = Duration.ofSeconds(1);
    private double multiplier = 2.0;
    private Duration maxDelay = Duration.ofMinutes(5);
    private double jitter = 0.2;
}

public static class Payload {
    private DataSize maxBytes = DataSize.ofMegabytes(1);
    private int maxHeaderCount = 64;
    private DataSize maxHeaderBytes = DataSize.ofKilobytes(16);
}

public static class Http {
    private boolean enabled;
    private final Map<String, HttpDestination> destinations = new LinkedHashMap<>();
}

public static class HttpDestination {
    private URI uri;
    private String method = "POST";
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(10);
    private final Map<String, String> fixedHeaders = new LinkedHashMap<>();
}

public static class Rabbitmq {
    private boolean enabled;
    private Duration confirmTimeout = Duration.ofSeconds(5);
    private final Map<String, RabbitDestination> destinations = new LinkedHashMap<>();
}

public static class RabbitDestination {
    private String exchange;
    private String routingKey;
    private boolean mandatory = true;
    private Duration confirmTimeout;
    private final Map<String, String> fixedHeaders = new LinkedHashMap<>();
}

public static class Cleanup {
    private boolean enabled;
    private Duration successRetention = Duration.ofDays(7);
    private Duration fixedDelay = Duration.ofHours(1);
    private int batchSize = 500;
}

public static class Shutdown {
    private Duration gracePeriod = Duration.ofSeconds(30);
}
```

Keep HTTP method as `String`, not `HttpMethod`, so loading the core properties class
does not require optional `spring-web`. Destination maps must never contain credentials.

- [ ] **Step 4: Implement fail-fast validation**

Make `OutboxConfigurationException` extend `OutboxException`. Implement:

```java
public class OutboxConfigurationValidator {

    private static final Duration LEASE_SAFETY_MARGIN = Duration.ofSeconds(1);

    public void validate(TransactionalOutboxProperties properties);
}
```

`validate` enforces:

- positive polling delay, batch size, concurrency, delivery timeout, queue capacity,
  retry delays, max attempts, payload limits, cleanup limits, and shutdown grace period;
- retry multiplier at least `1.0`, jitter in `[0.0, 1.0]`, and maximum delay no smaller
  than initial delay;
- `leaseDuration > deliveryTimeout + pollingFixedDelay + 1 second`;
- node ID no longer than 80 characters, leaving room for `:<UUID>` inside `locked_by(128)`;
- HTTP destination keys are nonblank, URI scheme is `http` or `https`, URI has no
  user-info, method is a standard HTTP verb, and fixed headers pass the same sensitive
  header denylist through `OutboxMessageValidator.isForbiddenHeader`;
- Rabbit destination keys, exchange, and routing key are nonblank, `mandatory` is true,
  effective confirm timeout is positive, and fixed headers pass
  `OutboxMessageValidator.isForbiddenHeader`;
- destination configuration messages identify only the logical destination key, not
  URI, exchange, routing key, or header values.

- [ ] **Step 5: Run the focused configuration tests**

Run the Step 2 Maven command again.

Expected: `BUILD SUCCESS`; defaults, binding, and failure messages pass.

- [ ] **Step 6: Commit the configuration model**

```bash
git add egon-cola-components/egon-cola-component-transactional-outbox
git commit -m "feat: add transactional outbox configuration"
```

### Task 4: Add the PostgreSQL Schema and Owner-Safe Store

**Files:**
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/resources/db/transactional-outbox/postgresql/V1__create_transactional_outbox_schema.sql`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/OutboxStatus.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/NewOutboxRecord.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/OutboxRecord.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/OutboxStore.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/PostgresqlJdbcOutboxStore.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/OutboxSchemaValidator.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/exception/OutboxIdempotencyConflictException.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/exception/OutboxStorageException.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/store/OutboxMigrationContractTest.java`
- Test support: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test/src/test/java/top/egon/cola/component/outbox/test/PostgresqlOutboxTestSupport.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test/src/test/java/top/egon/cola/component/outbox/test/PostgresqlJdbcOutboxStoreIntegrationTest.java`

**Interfaces:**
- Consumes: `OutboxReceipt`, selected `DataSource`, selected `PlatformTransactionManager`, Jackson `ObjectMapper`.
- Produces: one PostgreSQL table, atomic insert/deduplication, due/ID claim, owner-CAS transitions, cleanup, backlog count, and schema validation.

- [ ] **Step 1: Write the failing migration contract test**

Create `OutboxMigrationContractTest`:

```java
package top.egon.cola.component.outbox.store;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxMigrationContractTest {

    @Test
    void shouldPackageOneNonAutomaticPostgresqlMigrationTemplate() throws Exception {
        ClassPathResource resource = new ClassPathResource(
                "db/transactional-outbox/postgresql/V1__create_transactional_outbox_schema.sql");
        String sql = resource.getContentAsString(StandardCharsets.UTF_8);

        assertThat(resource.exists()).isTrue();
        assertThat(sql)
                .contains("create table egon_cola_outbox_message")
                .contains("uk_outbox_message_id")
                .contains("uk_outbox_idempotency_key")
                .contains("idx_outbox_claim")
                .contains("idx_outbox_reclaim")
                .contains("idx_outbox_cleanup");
        assertThat(new ClassPathResource("db/migration/V1__create_transactional_outbox_schema.sql").exists())
                .isFalse();
    }
}
```

- [ ] **Step 2: Write failing real-PostgreSQL store tests**

Create `PostgresqlOutboxTestSupport` as an abstract `@Testcontainers` class with a
static `postgres:16-alpine` `PostgreSQLContainer`, `PGSimpleDataSource`,
`JdbcTemplate`, `DataSourceTransactionManager`, and `ObjectMapper`. In `@BeforeAll`,
execute the packaged SQL through `ScriptUtils`; in `@BeforeEach`, run:

```sql
truncate table egon_cola_outbox_message restart identity
```

Create `PostgresqlJdbcOutboxStoreIntegrationTest` with these tests:

```java
@Test
void shouldInsertAndReturnExistingRecordForSameFingerprint() {
    OutboxReceipt created = store.enqueue(newRecord("message-1", "order:created:1", "fingerprint-a"));
    OutboxReceipt duplicate = store.enqueue(newRecord("message-2", "order:created:1", "fingerprint-a"));

    assertThat(created).isEqualTo(new OutboxReceipt("message-1", "order:created:1", true));
    assertThat(duplicate).isEqualTo(new OutboxReceipt("message-1", "order:created:1", false));
    assertThat(jdbcTemplate.queryForObject(
            "select count(*) from egon_cola_outbox_message", Long.class)).isEqualTo(1L);
}

@Test
void shouldRejectIdentifierReuseWithDifferentFingerprint() {
    store.enqueue(newRecord("message-1", "order:created:1", "fingerprint-a"));

    assertThatThrownBy(() ->
            store.enqueue(newRecord("message-1", "order:created:1", "fingerprint-b")))
            .isInstanceOf(OutboxIdempotencyConflictException.class);
}

@Test
void shouldClaimWithUniqueOwnerAndRejectStaleCompletion() {
    store.enqueue(newRecord("message-1", null, "fingerprint-a"));

    OutboxRecord first = store.claimDue(1, "node-a:claim-1", Duration.ofSeconds(60)).getFirst();
    jdbcTemplate.update("""
            update egon_cola_outbox_message
            set locked_until = clock_timestamp() - interval '1 second'
            where id = ?
            """, first.id());
    OutboxRecord reclaimed =
            store.claimDue(1, "node-a:claim-2", Duration.ofSeconds(60)).getFirst();

    assertThat(store.markSucceeded(first.id(), "node-a:claim-1")).isFalse();
    assertThat(store.markSucceeded(reclaimed.id(), "node-a:claim-2")).isTrue();
}
```

The helper `newRecord` must build a fully populated `NewOutboxRecord` with HTTP
channel, logical destination, `{}` payload, JSON content type, empty headers JSON,
ten attempts, and no explicit `availableAt`.

- [ ] **Step 3: Run the focused tests and verify they fail**

Run:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter,egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test \
  -am test \
  -Dtest=OutboxMigrationContractTest,PostgresqlJdbcOutboxStoreIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: compilation or resource lookup fails because the schema and store do not exist.

- [ ] **Step 4: Create the single schema resource**

Create the SQL exactly from Spec §12.1:

```sql
create table egon_cola_outbox_message (
    id bigint generated by default as identity primary key,
    message_id varchar(64) not null,
    idempotency_key varchar(256),
    message_fingerprint char(64) not null,
    channel varchar(64) not null,
    destination varchar(256) not null,
    payload text not null,
    content_type varchar(128) not null,
    schema_version varchar(32),
    headers_json text not null default '{}',
    trace_id varchar(128),
    status varchar(32) not null,
    attempt_count integer not null default 0,
    max_attempts integer not null,
    next_attempt_at timestamp with time zone not null,
    locked_by varchar(128),
    locked_until timestamp with time zone,
    last_error_code varchar(64),
    last_error_message text,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    completed_at timestamp with time zone,
    constraint ck_outbox_status check (
        status in ('PENDING', 'PROCESSING', 'RETRY_WAIT', 'SUCCEEDED', 'DEAD')
    ),
    constraint ck_outbox_attempt_count check (attempt_count >= 0),
    constraint ck_outbox_max_attempts check (max_attempts >= 1)
);

create unique index uk_outbox_message_id
    on egon_cola_outbox_message(message_id);

create unique index uk_outbox_idempotency_key
    on egon_cola_outbox_message(idempotency_key)
    where idempotency_key is not null;

create index idx_outbox_claim
    on egon_cola_outbox_message(next_attempt_at, id)
    where status in ('PENDING', 'RETRY_WAIT');

create index idx_outbox_reclaim
    on egon_cola_outbox_message(locked_until, id)
    where status = 'PROCESSING';

create index idx_outbox_cleanup
    on egon_cola_outbox_message(completed_at, id)
    where status = 'SUCCEEDED';
```

- [ ] **Step 5: Define the store records and state operations**

Create:

```java
public enum OutboxStatus {
    PENDING,
    PROCESSING,
    RETRY_WAIT,
    SUCCEEDED,
    DEAD
}
```

```java
public record NewOutboxRecord(
        String messageId,
        String idempotencyKey,
        String messageFingerprint,
        String channel,
        String destination,
        String payload,
        String contentType,
        String schemaVersion,
        String headersJson,
        String traceId,
        Instant availableAt,
        int maxAttempts
) {
}
```

```java
public record OutboxRecord(
        long id,
        String messageId,
        String idempotencyKey,
        String messageFingerprint,
        String channel,
        String destination,
        String payload,
        String contentType,
        String schemaVersion,
        Map<String, String> headers,
        String traceId,
        OutboxStatus status,
        int attemptCount,
        int maxAttempts,
        Instant nextAttemptAt,
        String lockedBy,
        Instant lockedUntil,
        String lastErrorCode,
        String lastErrorMessage,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {
}
```

```java
public interface OutboxStore {

    OutboxReceipt enqueue(NewOutboxRecord record);

    List<OutboxRecord> claimDue(int limit, String leaseOwner, Duration leaseDuration);

    List<OutboxRecord> claimByMessageIds(
            Collection<String> messageIds,
            int limit,
            String leaseOwner,
            Duration leaseDuration
    );

    boolean markSucceeded(long id, String leaseOwner);

    boolean markRetry(
            long id,
            String leaseOwner,
            Duration delay,
            String errorCode,
            String errorMessage
    );

    boolean markDead(long id, String leaseOwner, String errorCode, String errorMessage);

    int deleteSucceeded(Duration retention, int limit);

    long countBacklog();

    void validateSchema();
}
```

- [ ] **Step 6: Implement the PostgreSQL JDBC store**

Construct `PostgresqlJdbcOutboxStore` with `JdbcTemplate`,
`NamedParameterJdbcTemplate`, `ObjectMapper`, and the selected
`PlatformTransactionManager`. Both JDBC templates must use the selected DataSource.
Keep `enqueue` on the caller's transaction.
Wrap every claim, transition, cleanup, and schema operation in a short
`TransactionTemplate` using the selected manager; use `PROPAGATION_REQUIRES_NEW`
for worker operations.

The insert must use:

```sql
insert into egon_cola_outbox_message (
    message_id, idempotency_key, message_fingerprint, channel, destination,
    payload, content_type, schema_version, headers_json, trace_id,
    status, attempt_count, max_attempts, next_attempt_at, created_at, updated_at
) values (
    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
    'PENDING', 0, ?, coalesce(?, clock_timestamp()), clock_timestamp(), clock_timestamp()
)
on conflict do nothing
returning message_id
```

When the insert returns no row, select by `message_id` or non-null
`idempotency_key`. Exactly one existing row with the same fingerprint returns
`created=false`; zero rows, two different rows, or a different fingerprint throws
`OutboxIdempotencyConflictException`. Never update an existing row or its schedule.

Both claim methods use a CTE with `FOR UPDATE SKIP LOCKED`, stable
`next_attempt_at, id` ordering, a caller-provided limit, and:

```sql
set status = 'PROCESSING',
    attempt_count = attempt_count + 1,
    locked_by = ?,
    locked_until = clock_timestamp() + (? * interval '1 millisecond'),
    updated_at = clock_timestamp()
```

`claimDue` accepts due `PENDING`/`RETRY_WAIT` and expired `PROCESSING`.
`claimByMessageIds` uses a `NamedParameterJdbcTemplate` and adds
`message_id in (:messageIds)` while retaining the due/expired condition, so an
after-commit event cannot bypass `availableAt`.

All completion updates include:

```sql
where id = ?
  and status = 'PROCESSING'
  and locked_by = ?
```

`markSucceeded` clears lease/error fields and sets database `completed_at`;
`markRetry` sets `RETRY_WAIT` and `next_attempt_at =
clock_timestamp() + delay`; `markDead` sets `DEAD` and database `completed_at`.
Sanitize control characters, truncate persisted error codes to 64 characters, and
truncate persisted error messages to 2,000 characters.

`deleteSucceeded` selects at most the requested IDs whose `completed_at` is older
than database time minus retention and deletes only rows still in `SUCCEEDED`.
`countBacklog` counts `PENDING`, `PROCESSING`, and `RETRY_WAIT`.

Translate `DataAccessException` to `OutboxStorageException` without SQL values,
payload, headers, or credentials in the public message. Retain the original exception
as cause.

Create `OutboxSchemaValidator` as a small collaborator that calls
`OutboxStore.validateSchema()`. The store query must verify the table, all required
columns, the five status values through the check constraint, and the five named
indexes. Failure throws `OutboxConfigurationException`; it never creates or alters
schema.

- [ ] **Step 7: Run the migration and store tests**

Run the Step 3 Maven command again.

Expected: `BUILD SUCCESS`; PostgreSQL executes the SQL, deduplication works, a new
claim token reclaims expired work, and the old token cannot complete it.

- [ ] **Step 8: Commit the PostgreSQL store**

```bash
git add egon-cola-components/egon-cola-component-transactional-outbox
git commit -m "feat: add PostgreSQL transactional outbox store"
```

### Task 5: Define Delivery Strategies, Registry, Failure Classification, and Retry

**Files:**
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/DeliveryHandler.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/DeliveryContext.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/DeliveryResult.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/DeliveryHandlerRegistry.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/DeliveryFailureClassifier.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/DefaultDeliveryFailureClassifier.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/retry/OutboxRetryPolicy.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/retry/ExponentialJitterRetryPolicy.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/delivery/DeliveryHandlerRegistryTest.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/delivery/DeliveryResultTest.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/retry/ExponentialJitterRetryPolicyTest.java`

**Interfaces:**
- Consumes: validated logical `channel` and `destination`.
- Produces: the Strategy SPI selected by the dispatcher and a deterministic, injectable retry policy.

- [ ] **Step 1: Write failing delivery and retry tests**

Create tests with these core assertions:

```java
@Test
void shouldRejectDuplicateChannelsAtStartup() {
    DeliveryHandler first = handler("http");
    DeliveryHandler second = handler("http");

    assertThatThrownBy(() -> new DeliveryHandlerRegistry(List.of(first, second)))
            .isInstanceOf(OutboxConfigurationException.class)
            .hasMessageContaining("http");
}

@Test
void shouldResolveExactlyOneHandlerAndValidateDestination() {
    DeliveryHandler handler = handler("custom");
    DeliveryHandlerRegistry registry = new DeliveryHandlerRegistry(List.of(handler));

    assertThat(registry.required("custom")).isSameAs(handler);
    assertThatThrownBy(() -> registry.required("missing"))
            .isInstanceOf(OutboxValidationException.class);
}
```

```java
@Test
void shouldExposeTypedDeliveryOutcomes() {
    assertThat(DeliveryResult.success().kind()).isEqualTo(DeliveryResult.Kind.SUCCESS);
    assertThat(DeliveryResult.retryableFailure("HTTP_503", "unavailable").kind())
            .isEqualTo(DeliveryResult.Kind.RETRYABLE_FAILURE);
    assertThat(DeliveryResult.permanentFailure("HTTP_400", "bad request").kind())
            .isEqualTo(DeliveryResult.Kind.PERMANENT_FAILURE);
}
```

```java
@Test
void shouldApplyBoundedExponentialBackoffAndDeterministicJitter() {
    ExponentialJitterRetryPolicy policy = new ExponentialJitterRetryPolicy(
            Duration.ofSeconds(1), 2.0, Duration.ofSeconds(5), 0.2, () -> 0.5);

    assertThat(policy.nextDelay(1)).isEqualTo(Duration.ofSeconds(1));
    assertThat(policy.nextDelay(2)).isEqualTo(Duration.ofSeconds(2));
    assertThat(policy.nextDelay(4)).isEqualTo(Duration.ofSeconds(5));
}

@Test
void shouldClampNegativeAndOverflowingCalculations() {
    ExponentialJitterRetryPolicy low = new ExponentialJitterRetryPolicy(
            Duration.ofSeconds(1), 2.0, Duration.ofMinutes(5), 1.0, () -> 0.0);
    ExponentialJitterRetryPolicy high = new ExponentialJitterRetryPolicy(
            Duration.ofDays(1), Double.MAX_VALUE, Duration.ofDays(7), 1.0, () -> 1.0);

    assertThat(low.nextDelay(1)).isZero();
    assertThat(high.nextDelay(Integer.MAX_VALUE)).isEqualTo(Duration.ofDays(7));
}
```

- [ ] **Step 2: Run the tests and verify the missing SPI fails compilation**

Run:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter \
  -am test \
  -Dtest=DeliveryHandlerRegistryTest,DeliveryResultTest,ExponentialJitterRetryPolicyTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: compilation fails because the delivery and retry contracts do not exist.

- [ ] **Step 3: Implement the delivery contracts**

Create these exact signatures:

```java
public interface DeliveryHandler {

    String channel();

    void validateDestination(String destination);

    DeliveryResult deliver(DeliveryContext context) throws Exception;
}
```

```java
public record DeliveryContext(
        String messageId,
        String channel,
        String destination,
        String payload,
        String contentType,
        String schemaVersion,
        Map<String, String> headers,
        String traceId,
        int attempt,
        int maxAttempts,
        Instant deadline
) {
    public DeliveryContext {
        headers = Map.copyOf(headers);
    }
}
```

```java
public record DeliveryResult(Kind kind, String code, String message) {

    public enum Kind {
        SUCCESS,
        RETRYABLE_FAILURE,
        PERMANENT_FAILURE
    }

    public static DeliveryResult success() {
        return new DeliveryResult(Kind.SUCCESS, null, null);
    }

    public static DeliveryResult retryableFailure(String code, String message) {
        return new DeliveryResult(Kind.RETRYABLE_FAILURE, code, message);
    }

    public static DeliveryResult permanentFailure(String code, String message) {
        return new DeliveryResult(Kind.PERMANENT_FAILURE, code, message);
    }
}
```

`DeliveryHandlerRegistry` copies the handler list into an unmodifiable map. Trim and
validate channel names against `[a-z][a-z0-9._-]{0,63}`. Duplicate channels throw
`OutboxConfigurationException`; `required` for an absent channel throws
`OutboxValidationException`. Do not add aliases or case folding.

- [ ] **Step 4: Implement exception classification and bounded retry**

Create:

```java
public interface DeliveryFailureClassifier {

    DeliveryResult classify(Exception exception);
}
```

The default classifier returns:

```java
DeliveryResult.retryableFailure(
        "OUTBOX_DELIVERY_EXCEPTION",
        exception.getClass().getSimpleName()
);
```

It must not include the exception message because it may contain request or credential
data. The dispatcher, not the classifier, handles `Error` by allowing it to escape.

Create:

```java
public interface OutboxRetryPolicy {

    Duration nextDelay(int attempt);
}
```

`ExponentialJitterRetryPolicy` takes `initialDelay`, `multiplier`, `maxDelay`,
`jitter`, and `DoubleSupplier random`. `attempt` is one-based and represents the
attempt that just failed. Saturate multiplication at `maxDelay`, apply factor
`1 + ((randomValue * 2) - 1) * jitter`, clamp the result to `[Duration.ZERO,
maxDelay]`, and never convert overflowing values through floating-point nanoseconds.

- [ ] **Step 5: Run the focused delivery tests**

Run the Step 2 Maven command again.

Expected: `BUILD SUCCESS`; registry uniqueness and deterministic retry boundaries pass.

- [ ] **Step 6: Commit the delivery core**

```bash
git add egon-cola-components/egon-cola-component-transactional-outbox
git commit -m "feat: add outbox delivery strategy contracts"
```

### Task 6: Enqueue in the Caller Transaction and Publish Only After Commit

**Files:**
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/transaction/OutboxTransactionGuard.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/transaction/OutboxAfterCommitBuffer.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/transaction/DefaultTransactionalOutbox.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/event/OutboxCommittedEvent.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/exception/OutboxTransactionRequiredException.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/exception/OutboxTransactionSynchronizationException.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/exception/OutboxTransactionMismatchException.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/transaction/OutboxTransactionGuardTest.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/transaction/OutboxAfterCommitBufferTest.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/transaction/DefaultTransactionalOutboxTest.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test/src/test/java/top/egon/cola/component/outbox/test/TransactionalOutboxTransactionIntegrationTest.java`

**Interfaces:**
- Consumes: Task 2 API/serializer/validator, Task 4 store, Task 5 registry, selected `DataSource`, retry `maxAttempts`, and application `ApplicationEventPublisher`.
- Produces: the direct API implementation with same-transaction insert, idempotent receipt, and one post-commit event per transaction.

- [ ] **Step 1: Write failing transaction-guard unit tests**

Use a mocked `DataSource`, `ConnectionHolder`, and
`TransactionSynchronizationManager` lifecycle:

```java
@AfterEach
void cleanTransactionState() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.clearSynchronization();
    }
    List.copyOf(TransactionSynchronizationManager.getResourceMap().keySet())
            .forEach(TransactionSynchronizationManager::unbindResourceIfPossible);
    TransactionSynchronizationManager.clear();
}

@Test
void shouldRequireAnActualTransaction() {
    assertThatThrownBy(guard::requireSelectedTransaction)
            .isInstanceOf(OutboxTransactionRequiredException.class);
}

@Test
void shouldRequireTransactionSynchronization() {
    TransactionSynchronizationManager.setActualTransactionActive(true);

    assertThatThrownBy(guard::requireSelectedTransaction)
            .isInstanceOf(OutboxTransactionSynchronizationException.class);
}

@Test
void shouldRejectTransactionBoundToAnotherDataSource() {
    TransactionSynchronizationManager.setActualTransactionActive(true);
    TransactionSynchronizationManager.initSynchronization();
    TransactionSynchronizationManager.bindResource(otherDataSource, connectionHolder);

    assertThatThrownBy(guard::requireSelectedTransaction)
            .isInstanceOf(OutboxTransactionMismatchException.class);
}
```

- [ ] **Step 2: Write failing after-commit buffer tests**

Assert that two new IDs create one synchronization and one ordered event, rollback
creates no event, and publisher failure is swallowed only after commit:

```java
@Test
void shouldPublishOneDeduplicatedEventAfterCommit() {
    TransactionSynchronizationManager.initSynchronization();
    buffer.record("message-1");
    buffer.record("message-2");
    buffer.record("message-1");

    assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
    TransactionSynchronization synchronization =
            TransactionSynchronizationManager.getSynchronizations().getFirst();
    synchronization.afterCommit();
    synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

    verify(publisher).publishEvent(
            new OutboxCommittedEvent(List.of("message-1", "message-2")));
}
```

- [ ] **Step 3: Write failing Facade and PostgreSQL transaction tests**

`DefaultTransactionalOutboxTest` uses mocks to prove validation and serialization
occur before store insert, destination validation occurs before persistence,
generated IDs are stable, a duplicate receipt does not register a second wake-up,
and store exceptions are not swallowed.

`TransactionalOutboxTransactionIntegrationTest` extends
`PostgresqlOutboxTestSupport`, creates a small `outbox_test_order` table for the test,
and manually wires the real store, validator, guard, buffer, serializer, ID generator,
and a `DeliveryHandlerRegistry` containing a no-op HTTP handler. Cover:

```java
@Test
void shouldCommitBusinessRowAndOutboxRowTogether() {
    transactionTemplate.executeWithoutResult(status -> {
        jdbcTemplate.update("insert into outbox_test_order(id) values (?)", "O-1");
        transactionalOutbox.enqueue(message("O-1"));
    });

    assertThat(count("outbox_test_order")).isEqualTo(1);
    assertThat(count("egon_cola_outbox_message")).isEqualTo(1);
    assertThat(committedEvents).hasSize(1);
}

@Test
void shouldRollbackBothRowsWhenBusinessFails() {
    assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
        jdbcTemplate.update("insert into outbox_test_order(id) values (?)", "O-1");
        transactionalOutbox.enqueue(message("O-1"));
        throw new IllegalStateException("business failure");
    })).isInstanceOf(IllegalStateException.class);

    assertThat(count("outbox_test_order")).isZero();
    assertThat(count("egon_cola_outbox_message")).isZero();
    assertThat(committedEvents).isEmpty();
}

@Test
void shouldRollbackBusinessRowWhenOutboxInsertFails() {
    OutboxStore failingStore = mock(OutboxStore.class);
    when(failingStore.enqueue(any(NewOutboxRecord.class)))
            .thenThrow(new OutboxStorageException("Outbox insert failed"));
    TransactionalOutbox failingOutbox = facadeWithStore(failingStore);

    assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
        jdbcTemplate.update("insert into outbox_test_order(id) values (?)", "O-1");
        failingOutbox.enqueue(message("O-1"));
    })).isInstanceOf(OutboxStorageException.class);

    assertThat(count("outbox_test_order")).isZero();
}
```

Also call the Facade outside a transaction and inside a transaction managed by a
second `DataSourceTransactionManager` to prove the required and mismatch exceptions.

- [ ] **Step 4: Run the focused tests and verify they fail**

Run:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter,egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test \
  -am test \
  -Dtest=OutboxTransactionGuardTest,OutboxAfterCommitBufferTest,DefaultTransactionalOutboxTest,TransactionalOutboxTransactionIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: compilation fails because the transaction Facade and events do not exist.

- [ ] **Step 5: Implement the transaction guard**

Create `OutboxTransactionGuard(DataSource selectedDataSource)` with:

```java
public void requireSelectedTransaction() {
    if (!TransactionSynchronizationManager.isActualTransactionActive()) {
        throw new OutboxTransactionRequiredException(
                "Transactional outbox requires an active Spring transaction");
    }
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
        throw new OutboxTransactionSynchronizationException(
                "Transactional outbox requires active transaction synchronization");
    }
    if (!TransactionSynchronizationManager.hasResource(selectedDataSource)) {
        throw new OutboxTransactionMismatchException(
                "The active transaction does not use the configured outbox DataSource");
    }
}
```

Each exception extends `OutboxException`. Do not call `DataSource#getConnection`,
create a connection, commit, rollback, or close a Spring-bound connection.

- [ ] **Step 6: Implement the transaction-scoped event buffer**

Create:

```java
public record OutboxCommittedEvent(List<String> messageIds) {
    public OutboxCommittedEvent {
        messageIds = List.copyOf(messageIds);
    }
}
```

`OutboxAfterCommitBuffer.record(String messageId)` examines
`TransactionSynchronizationManager.getSynchronizations()` for its own private
`OutboxSynchronization` type. Reuse that synchronization or register one new
instance holding a `LinkedHashSet<String>`.

In `afterCommit`, publish one `OutboxCommittedEvent`; catch `RuntimeException` from
the publisher and log only the number of IDs. In `afterCompletion`, clear the set.
Do not use a global/thread-local event queue, `@Async`, or a transaction resource
that can leak across suspended `REQUIRES_NEW` transactions.

- [ ] **Step 7: Implement `DefaultTransactionalOutbox`**

Use constructor injection for:

```text
OutboxMessageValidator
OutboxMessageSerializer
OutboxIdGenerator
ObjectMapper
OutboxTransactionGuard
OutboxStore
DeliveryHandlerRegistry
OutboxAfterCommitBuffer
TransactionalOutboxProperties
```

Implement `enqueue` in this order:

```java
transactionGuard.requireSelectedTransaction();
validator.validateEnvelope(message);
String messageId = hasText(message.messageId())
        ? message.messageId()
        : idGenerator.nextId();
SerializedOutboxPayload serialized =
        serializer.serialize(message.payload(), message.contentType());
validator.validateSerialized(serialized, message.headers());
DeliveryHandler handler = handlerRegistry.required(message.channel());
handler.validateDestination(message.destination());
String headersJson = objectMapper.writeValueAsString(new TreeMap<>(message.headers()));
String fingerprint = OutboxMessageFingerprint.sha256(
        message.channel(),
        message.destination(),
        serialized.text(),
        message.contentType(),
        message.schemaVersion(),
        message.headers()
);
OutboxReceipt receipt = store.enqueue(new NewOutboxRecord(
        messageId,
        message.idempotencyKey(),
        fingerprint,
        message.channel(),
        message.destination(),
        serialized.text(),
        message.contentType(),
        message.schemaVersion(),
        headersJson,
        message.traceId(),
        message.availableAt(),
        properties.getRetry().getMaxAttempts()
));
if (receipt.created()) {
    afterCommitBuffer.record(receipt.messageId());
}
return receipt;
```

Wrap header JSON failures in `OutboxSerializationException`; do not catch store,
validation, destination, or transaction exceptions. Generate an ID only once per
call. The store remains responsible for duplicate identifier comparison.

- [ ] **Step 8: Run the direct API transaction tests**

Run the Step 4 Maven command again.

Expected: `BUILD SUCCESS`; PostgreSQL commit/rollback, wrong-DataSource rejection,
and one after-commit event per transaction pass.

- [ ] **Step 9: Commit the transactional Facade**

```bash
git add egon-cola-components/egon-cola-component-transactional-outbox
git commit -m "feat: enqueue outbox messages with business transactions"
```

### Task 7: Dispatch Claimed Records, Retry Safely, Notify DEAD, and Clean Successes

**Files:**
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/dispatch/OutboxWorkerIdentity.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcher.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/dispatch/OutboxPoller.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/event/OutboxCommittedEventListener.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/event/OutboxDeadLetterEvent.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/deadletter/OutboxDeadLetterListener.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/deadletter/OutboxDeadLetterNotifier.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/cleanup/OutboxCleanupJob.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/observability/OutboxMetrics.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/observability/NoopOutboxMetrics.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcherTest.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/dispatch/OutboxPollerTest.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/deadletter/OutboxDeadLetterNotifierTest.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/cleanup/OutboxCleanupJobTest.java`

**Interfaces:**
- Consumes: `OutboxStore`, `DeliveryHandlerRegistry`, `DeliveryFailureClassifier`, `OutboxRetryPolicy`, a bounded Spring `TaskExecutor`, a component `TaskScheduler`, and Task 3 runtime properties.
- Produces: one shared claim/deliver/transition path for polling and after-commit wake-up, lease recovery, bounded execution, dead notification, and successful-row cleanup.

- [ ] **Step 1: Write failing dispatcher state-machine tests**

Use an in-memory `RecordingOutboxStore`, direct executor
`Runnable::run`, deterministic retry policy, and test handlers:

```java
@Test
void shouldMarkSuccessfulDeliveryWithCurrentOwner() {
    store.add(record("message-1", 1, 10, "node-a:claim-1"));
    handler.result = DeliveryResult.success();

    dispatcher.dispatchDue();

    assertThat(store.succeeded).containsExactly("message-1");
    assertThat(store.retried).isEmpty();
    assertThat(store.dead).isEmpty();
}

@Test
void shouldRetryRetryableFailureWhenAttemptsRemain() {
    store.add(record("message-1", 2, 10, "node-a:claim-1"));
    handler.result = DeliveryResult.retryableFailure("HTTP_503", "unavailable");

    dispatcher.dispatchDue();

    assertThat(store.retried.get("message-1").delay()).isEqualTo(Duration.ofSeconds(2));
}

@Test
void shouldDeadLetterExhaustedAndPermanentFailuresWithoutStoppingBatch() {
    store.add(
            record("poison", 10, 10, "node-a:claim-1"),
            record("healthy", 1, 10, "node-a:claim-1"));
    handler.results.put("poison",
            DeliveryResult.retryableFailure("HTTP_503", "unavailable"));
    handler.results.put("healthy", DeliveryResult.success());

    dispatcher.dispatchDue();

    assertThat(store.dead).containsExactly("poison");
    assertThat(store.succeeded).containsExactly("healthy");
    assertThat(deadEvents).extracting(OutboxDeadLetterEvent::messageId)
            .containsExactly("poison");
}

@Test
void shouldObserveLeaseLossWithoutOverwritingNewOwner() {
    store.add(record("message-1", 1, 10, "node-a:claim-1"));
    store.allowOwnerUpdate = false;
    handler.result = DeliveryResult.success();

    dispatcher.dispatchDue();

    assertThat(metrics.leaseLostCount).isEqualTo(1);
}

@Test
void shouldLeaveRecordProcessingWhenErrorEscapes() {
    store.add(record("message-1", 1, 10, "node-a:claim-1"));
    handler.error = new AssertionError("fatal");

    assertThatThrownBy(dispatcher::dispatchDue).isInstanceOf(AssertionError.class);
    assertThat(store.succeeded).isEmpty();
    assertThat(store.retried).isEmpty();
    assertThat(store.dead).isEmpty();
}
```

Add a test that removes the handler or logical destination after enqueue and expects
`OUTBOX_HANDLER_MISSING` or `OUTBOX_DESTINATION_INVALID` to transition that record
to `DEAD` while another record in the same claim completes.

- [ ] **Step 2: Write failing lifecycle, cleanup, and listener-isolation tests**

`OutboxPollerTest` uses a mocked `TaskScheduler` and asserts start schedules one
poll fixed-delay task plus a cleanup task only when enabled; stop cancels both and
prevents later submissions.

`OutboxCleanupJobTest` verifies it calls:

```java
store.deleteSucceeded(
        properties.getCleanup().getSuccessRetention(),
        properties.getCleanup().getBatchSize()
);
```

only when cleanup is enabled. Store integration tests later prove the SQL never
deletes `DEAD` or pending rows.

`OutboxDeadLetterNotifierTest` registers two listeners where the first throws and
asserts the second still receives one event. Assert the event exposes message ID,
channel, logical destination, attempt, error code, sanitized summary, and trace ID,
but no payload or headers.

- [ ] **Step 3: Run the tests and verify they fail**

Run:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter \
  -am test \
  -Dtest=OutboxDispatcherTest,OutboxPollerTest,OutboxDeadLetterNotifierTest,OutboxCleanupJobTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: compilation fails because the dispatcher and lifecycle collaborators do
not exist.

- [ ] **Step 4: Implement worker identity and observability abstraction**

`OutboxWorkerIdentity` accepts the optional configured node ID. When blank, generate
one process-stable ID from sanitized host name, `ProcessHandle.current().pid()`, and
an eight-character random suffix. Implement:

```java
public String nodeId();

public String nextLeaseOwner() {
    return nodeId + ":" + UUID.randomUUID();
}
```

Every claim call obtains a new token; never reuse a stable node ID as `locked_by`.

Create this classpath-independent metrics interface:

```java
public interface OutboxMetrics {

    void enqueue(boolean created);

    void claimed(int count);

    void delivery(String channel, String result, Duration duration);

    void retry(String channel);

    void dead(String channel);

    void leaseLost();

    void wakeupRejected();

    void updateBacklog(long value);
}
```

`NoopOutboxMetrics` implements all methods with empty bodies. No method accepts
message ID, idempotency key, destination, URL, payload, or headers.

- [ ] **Step 5: Implement DEAD events and isolated notification**

Create:

```java
public record OutboxDeadLetterEvent(
        String messageId,
        String channel,
        String destination,
        int attempt,
        String errorCode,
        String errorMessage,
        String traceId
) {
}
```

```java
public interface OutboxDeadLetterListener {

    void onDead(OutboxDeadLetterEvent event);
}
```

`OutboxDeadLetterNotifier` copies the injected listener list. Its `notifyDead`
method runs only after `markDead` returns true, invokes every listener on the current
delivery worker, catches each listener's `RuntimeException`, and continues. This is
already asynchronous relative to the committed business call and does not need a
second unbounded executor.

- [ ] **Step 6: Implement the dispatcher state machine**

Construct `OutboxDispatcher` with the store, registry, failure classifier, retry
policy, dead notifier, metrics, worker identity, bounded `TaskExecutor`, properties,
and an injectable `Clock`.

Expose:

```java
public void submitDue();

public void submitMessageIds(Collection<String> messageIds);

void dispatchDue();

void dispatchMessageIds(Collection<String> messageIds);
```

`submitDue` and `submitMessageIds` put only a finite coordinator task on the same
bounded executor. Coalesce wake-ups with one `AtomicBoolean coordinatorScheduled`,
one insertion-ordered pending-ID set capped at `batchSize`, and one
`AtomicBoolean fullPollRequested`; once the ID cap is reached, set full-poll requested
and discard only the in-memory hint because every ID remains durable in PostgreSQL;
there must never be an unbounded queue of coordinator Runnables. If scheduling is
rejected, clear the scheduling flag, increment `wakeupRejected`, log without IDs,
and rely on the next poll. `dispatchDue` and `dispatchMessageIds`:

1. reserve up to `batchSize` permits from a `Semaphore` sized
   `max(1, concurrency + queueCapacity - 1)` so the currently running coordinator
   itself is not counted as available delivery capacity;
2. if no permit is available, return before claiming;
3. claim no more records than reserved permits with one new lease owner token;
4. release unused permits when fewer rows are returned;
5. submit one finite delivery task per claimed record;
6. release each permit in the task's `finally` block;
7. if shutdown races with submission, release the permit and leave the claimed row
   for lease recovery rather than marking it successful;
8. clear `coordinatorScheduled` in `finally` and reschedule once when hints arrived
   during the race window.

For each record:

```java
DeliveryContext context = new DeliveryContext(
        record.messageId(),
        record.channel(),
        record.destination(),
        record.payload(),
        record.contentType(),
        record.schemaVersion(),
        record.headers(),
        record.traceId(),
        record.attemptCount(),
        record.maxAttempts(),
        clock.instant().plus(properties.getDelivery().getTimeout())
);
```

Resolve and revalidate the handler/destination on every attempt. Convert missing
handler to permanent `OUTBOX_HANDLER_MISSING`; invalid or removed destination to
permanent `OUTBOX_DESTINATION_INVALID`. Invoke `deliver` outside every database
transaction.

Apply results exactly:

```text
SUCCESS
  -> markSucceeded(id, lockedBy)

RETRYABLE_FAILURE and attemptCount < maxAttempts
  -> markRetry(id, lockedBy, retryPolicy.nextDelay(attemptCount), code, message)

RETRYABLE_FAILURE and attemptCount >= maxAttempts
  -> markDead(id, lockedBy, OUTBOX_RETRY_EXHAUSTED, sanitized original code)

PERMANENT_FAILURE
  -> markDead(id, lockedBy, code, message)
```

Classify `Exception` through `DeliveryFailureClassifier`. Catch `Error` only to log
the safe record identity, then rethrow without a transition so the lease recovers.
When a transition returns false, increment `leaseLost`, log a warning without
payload, and perform no second external call or state update.

After a successful DEAD transition, call the notifier. Update cached backlog after
each finite claim cycle with `store.countBacklog()`; Micrometer later reads only this
cache.

- [ ] **Step 7: Implement commit wake-up, polling, cleanup, and shutdown**

`OutboxCommittedEventListener` implements `ApplicationListener<OutboxCommittedEvent>`
and calls `dispatcher.submitMessageIds(event.messageIds())`. It never delivers the
event payload directly.

`OutboxCleanupJob.runOnce()` returns immediately when cleanup is disabled; otherwise
it calls the bounded store deletion once and catches/logs `RuntimeException` without
affecting dispatch.

`OutboxPoller` implements `SmartLifecycle`. On `start`, use the dedicated
`TaskScheduler` to schedule `dispatcher.submitDue` with configured fixed delay and,
when enabled, `cleanupJob.runOnce` with cleanup fixed delay. On `stop`, mark the
poller stopped and cancel scheduled futures before the executor shutdown phase.
Never use an infinite drain loop, `@EnableScheduling`, or a global application
executor.

- [ ] **Step 8: Run the dispatcher and lifecycle tests**

Run the Step 3 Maven command again.

Expected: `BUILD SUCCESS`; result transitions, retry exhaustion, poison isolation,
lease loss, listener isolation, cleanup gating, and poller lifecycle pass.

- [ ] **Step 9: Commit dispatch and recovery**

```bash
git add egon-cola-components/egon-cola-component-transactional-outbox
git commit -m "feat: dispatch and recover transactional outbox records"
```

### Task 8: Add the Allowlisted HTTP Delivery Adapter

**Files:**
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/http/HttpDeliveryTarget.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/http/HttpDestinationResolver.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/http/PropertiesHttpDestinationResolver.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/http/HttpCredentialProvider.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/http/HttpDeliveryClassifier.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/http/DefaultHttpDeliveryClassifier.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/http/HttpDeliveryHandler.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/delivery/http/PropertiesHttpDestinationResolverTest.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/delivery/http/DefaultHttpDeliveryClassifierTest.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test/src/test/java/top/egon/cola/component/outbox/test/HttpDeliveryHandlerIntegrationTest.java`

**Interfaces:**
- Consumes: `DeliveryHandler`, logical destination properties, `DeliveryContext.deadline`, and in-memory credentials.
- Produces: `channel=http` delivery with no redirects, stable idempotency headers, bounded timeouts, and explicit HTTP result classification.

- [ ] **Step 1: Write failing resolver and status-classifier tests**

Assert a configured logical name resolves to an immutable target and an unknown name
fails during `validateDestination`. Add this classification matrix:

```java
@ParameterizedTest
@CsvSource({
        "200,SUCCESS",
        "204,SUCCESS",
        "302,PERMANENT_FAILURE",
        "400,PERMANENT_FAILURE",
        "404,PERMANENT_FAILURE",
        "408,RETRYABLE_FAILURE",
        "425,RETRYABLE_FAILURE",
        "429,RETRYABLE_FAILURE",
        "500,RETRYABLE_FAILURE",
        "503,RETRYABLE_FAILURE"
})
void shouldClassifyHttpStatus(int status, DeliveryResult.Kind expected) {
    assertThat(classifier.classify(HttpStatusCode.valueOf(status)).kind()).isEqualTo(expected);
}
```

- [ ] **Step 2: Write failing WireMock integration tests**

Start WireMock on a random local port and configure destination `order-callback` to
`/callback`. Cover:

```java
@Test
void shouldDeliverBodyAndStableIdempotencyHeadersOnTwoHundred() {
    wireMock.stubFor(post("/callback").willReturn(noContent()));

    DeliveryResult result = handler.deliver(context("message-1"));

    assertThat(result).isEqualTo(DeliveryResult.success());
    wireMock.verify(postRequestedFor(urlEqualTo("/callback"))
            .withHeader("Idempotency-Key", equalTo("message-1"))
            .withHeader("X-Egon-Cola-Message-Id", equalTo("message-1"))
            .withHeader("Content-Type", containing("application/json"))
            .withRequestBody(equalToJson("{\"orderId\":\"O-1\"}")));
}

@Test
void shouldClassifyThrottleServerAndClientFailures() {
    assertThat(deliverStatus(429).kind()).isEqualTo(DeliveryResult.Kind.RETRYABLE_FAILURE);
    assertThat(deliverStatus(503).kind()).isEqualTo(DeliveryResult.Kind.RETRYABLE_FAILURE);
    assertThat(deliverStatus(400).kind()).isEqualTo(DeliveryResult.Kind.PERMANENT_FAILURE);
}

@Test
void shouldNotFollowRedirects() {
    wireMock.stubFor(post("/callback")
            .willReturn(temporaryRedirect("/credential-capture")));

    DeliveryResult result = handler.deliver(context("message-1"));

    assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.PERMANENT_FAILURE);
    wireMock.verify(0, postRequestedFor(urlEqualTo("/credential-capture")));
}

@Test
void shouldRetryTimeoutWithoutPersistingResponseBody() {
    wireMock.stubFor(post("/callback").willReturn(aResponse()
            .withFixedDelay(500)
            .withStatus(503)
            .withBody("Authorization: secret-value")));

    DeliveryResult result = handlerWithReadTimeout(Duration.ofMillis(50))
            .deliver(context("message-1"));

    assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.RETRYABLE_FAILURE);
    assertThat(result.message()).doesNotContain("secret-value");
}
```

Add one test where `HttpCredentialProvider` supplies Authorization in memory and
WireMock receives it, while the `DeliveryContext` remains unchanged. Add one test
that an already-expired deadline returns `HTTP_DEADLINE_EXCEEDED` without a request.

- [ ] **Step 3: Run the tests and verify they fail**

Run:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter,egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test \
  -am test \
  -Dtest=PropertiesHttpDestinationResolverTest,DefaultHttpDeliveryClassifierTest,HttpDeliveryHandlerIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: compilation fails because the HTTP adapter does not exist.

- [ ] **Step 4: Implement logical HTTP targets and credentials**

Create:

```java
public record HttpDeliveryTarget(
        URI uri,
        String method,
        Duration connectTimeout,
        Duration readTimeout,
        Map<String, String> fixedHeaders
) {
    public HttpDeliveryTarget {
        fixedHeaders = Map.copyOf(fixedHeaders);
    }
}
```

```java
public interface HttpDestinationResolver {

    HttpDeliveryTarget resolve(String destination);
}
```

```java
public interface HttpCredentialProvider {

    Map<String, String> resolveHeaders(String destination);
}
```

`PropertiesHttpDestinationResolver` snapshots Task 3's destination map, returns no
URI or headers from the outbox row, and throws `OutboxValidationException` containing
only the logical key when absent. The default credential provider returns `Map.of()`.

- [ ] **Step 5: Implement HTTP classification**

Create:

```java
public interface HttpDeliveryClassifier {

    DeliveryResult classify(HttpStatusCode status);
}
```

`DefaultHttpDeliveryClassifier` implements the Spec matrix. Codes are stable
(`HTTP_204`, `HTTP_429`, and equivalent); messages contain only the numeric status,
never response headers or body.

- [ ] **Step 6: Implement the `RestClient` adapter**

`HttpDeliveryHandler.channel()` returns `http`. `validateDestination` resolves the
target and therefore fails before enqueue for unknown logical names.

For each attempt:

1. resolve the target again;
2. compute remaining time from `DeliveryContext.deadline`;
3. return retryable `HTTP_DEADLINE_EXCEEDED` if no time remains;
4. build a JDK `HttpClient` with target connect timeout and
   `HttpClient.Redirect.NEVER`;
5. build `JdkClientHttpRequestFactory`, setting read timeout to the smaller of target
   read timeout and remaining time;
6. build a request-scoped `RestClient` from that factory;
7. apply configured fixed non-sensitive headers, then persisted message headers,
   then credential-provider headers in memory;
8. reject Host, Content-Length, Transfer-Encoding, and Connection from every source;
9. overwrite `Content-Type` from `context.contentType()`;
10. overwrite `Idempotency-Key` and `X-Egon-Cola-Message-Id` with
    `context.messageId()`;
11. send `context.payload()` as the body and inspect status through
    `RestClient.exchange` without reading the response body.

Catch `ResourceAccessException`, timeout, and connection exceptions as retryable
stable codes. Let other `Exception` flow to the shared classifier. Never log URI,
credential headers, request body, or response body.

- [ ] **Step 7: Run the HTTP tests**

Run the Step 3 Maven command again.

Expected: `BUILD SUCCESS`; redirects are not followed, credentials remain in memory,
and response bodies never enter `DeliveryResult`.

- [ ] **Step 8: Commit the HTTP adapter**

```bash
git add egon-cola-components/egon-cola-component-transactional-outbox
git commit -m "feat: add transactional outbox HTTP delivery"
```

### Task 9: Add RabbitMQ Delivery with Correlated Confirm and Mandatory Return

**Files:**
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/rabbitmq/RabbitDeliveryTarget.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/rabbitmq/RabbitDestinationResolver.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/rabbitmq/PropertiesRabbitDestinationResolver.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/rabbitmq/RabbitMessagePublisher.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/rabbitmq/RabbitTemplateMessagePublisher.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/rabbitmq/RabbitPublishOutcome.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/rabbitmq/RabbitDeliveryHandler.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/delivery/rabbitmq/PropertiesRabbitDestinationResolverTest.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/delivery/rabbitmq/RabbitDeliveryHandlerTest.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test/src/test/java/top/egon/cola/component/outbox/test/RabbitDeliveryHandlerIntegrationTest.java`

**Interfaces:**
- Consumes: application `RabbitTemplate`, configured logical exchange/routing key, correlated publisher confirms, publisher returns, and `DeliveryContext.deadline`.
- Produces: `channel=rabbitmq` delivery where only ACK without return succeeds.

- [ ] **Step 1: Write failing deterministic Rabbit outcome tests**

Use a fake `RabbitMessagePublisher`:

```java
@ParameterizedTest
@MethodSource("outcomes")
void shouldClassifyBrokerOutcome(
        RabbitPublishOutcome outcome,
        DeliveryResult.Kind expected,
        String code
) throws Exception {
    publisher.outcome = outcome;

    DeliveryResult result = handler.deliver(context("message-1", 1));

    assertThat(result.kind()).isEqualTo(expected);
    assertThat(result.code()).isEqualTo(code);
}

static Stream<Arguments> outcomes() {
    return Stream.of(
            arguments(RabbitPublishOutcome.ack(), DeliveryResult.Kind.SUCCESS, null),
            arguments(RabbitPublishOutcome.nack("broker nack"),
                    DeliveryResult.Kind.RETRYABLE_FAILURE, "RABBIT_NACK"),
            arguments(RabbitPublishOutcome.timeout(),
                    DeliveryResult.Kind.RETRYABLE_FAILURE, "RABBIT_CONFIRM_TIMEOUT"),
            arguments(RabbitPublishOutcome.returned(312, "NO_ROUTE"),
                    DeliveryResult.Kind.PERMANENT_FAILURE, "RABBIT_UNROUTABLE")
    );
}
```

Add tests for unknown destinations, connection exception as retryable, and preserving
the same `messageId` over attempt 1 and attempt 2.

- [ ] **Step 2: Write failing RabbitMQ Testcontainer tests**

Create a `rabbitmq:4-management-alpine` container and a test-owned
`CachingConnectionFactory` configured with:

```java
connectionFactory.setPublisherConfirmType(
        CachingConnectionFactory.ConfirmType.CORRELATED);
connectionFactory.setPublisherReturns(true);
RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
rabbitTemplate.setMandatory(true);
```

Use `RabbitAdmin` only in the test to declare topology. Cover:

- ACK delivery to a declared direct exchange/queue/binding and verify the consumed
  message has persistent delivery mode, stable `messageId`, schema version, and
  attempt header;
- mandatory return from a declared exchange with no matching binding and verify
  permanent `RABBIT_UNROUTABLE`;
- broker NACK by binding a queue configured with `x-max-length=1` and
  `x-overflow=reject-publish`, filling it, then publishing another message and
  expecting retryable `RABBIT_NACK`;
- confirm timeout by establishing the connection, pausing the Rabbit container,
  through `DockerClientFactory.instance().client().pauseContainerCmd(containerId)`,
  delivering with a 100 ms confirm timeout inside `assertTimeoutPreemptively`, and
  calling `unpauseContainerCmd(containerId)` in `finally`;
- two attempts publish the same message ID;
- no queue, exchange, or binding is declared by component code.

- [ ] **Step 3: Run the tests and verify they fail**

Run:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter,egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test \
  -am test \
  -Dtest=PropertiesRabbitDestinationResolverTest,RabbitDeliveryHandlerTest,RabbitDeliveryHandlerIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: compilation fails because the Rabbit adapter does not exist.

- [ ] **Step 4: Implement logical Rabbit targets and broker outcomes**

Create:

```java
public record RabbitDeliveryTarget(
        String exchange,
        String routingKey,
        boolean mandatory,
        Duration confirmTimeout,
        Map<String, String> fixedHeaders
) {
    public RabbitDeliveryTarget {
        fixedHeaders = Map.copyOf(fixedHeaders);
    }
}
```

```java
public interface RabbitDestinationResolver {

    RabbitDeliveryTarget resolve(String destination);
}
```

`PropertiesRabbitDestinationResolver` snapshots configuration and uses the
destination confirm timeout when present, otherwise the Rabbit global timeout.
Unknown keys throw `OutboxValidationException` containing only the logical key.

Create:

```java
public record RabbitPublishOutcome(Kind kind, String reason, Integer replyCode) {

    public enum Kind {
        ACK,
        NACK,
        TIMEOUT,
        RETURNED
    }

    public RabbitPublishOutcome {
        Objects.requireNonNull(kind, "kind");
        reason = sanitize(reason);
    }

    public static RabbitPublishOutcome ack() {
        return new RabbitPublishOutcome(Kind.ACK, null, null);
    }

    public static RabbitPublishOutcome nack(String reason) {
        return new RabbitPublishOutcome(Kind.NACK, reason, null);
    }

    public static RabbitPublishOutcome timeout() {
        return new RabbitPublishOutcome(Kind.TIMEOUT, null, null);
    }

    public static RabbitPublishOutcome returned(int replyCode, String reason) {
        return new RabbitPublishOutcome(Kind.RETURNED, reason, replyCode);
    }

    private static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("[\\r\\n\\t]", " ");
        return normalized.substring(0, Math.min(normalized.length(), 256));
    }
}
```

Reason factories sanitize control characters and cap summaries at 256 characters.

- [ ] **Step 5: Implement correlated publishing**

Create:

```java
public interface RabbitMessagePublisher {

    RabbitPublishOutcome publish(
            RabbitDeliveryTarget target,
            DeliveryContext context
    ) throws Exception;
}
```

`RabbitTemplateMessagePublisher`:

1. builds a Spring AMQP `Message` from UTF-8 payload;
2. sets content type, `messageId`, persistent delivery mode, schema version, and
   integer attempt;
3. merges fixed and persisted non-sensitive headers without allowing credentials or
   transport-control headers;
4. creates unique `CorrelationData` from message ID, attempt, and random suffix;
5. sends to only the resolved exchange/routing key;
6. waits for the smaller of target confirm timeout and remaining delivery deadline;
7. returns `TIMEOUT` when the future misses the bound;
8. returns `RETURNED` when `CorrelationData.getReturned()` is non-null, even if the
   confirm ACK is true;
9. otherwise maps confirm ACK/NACK.

Never install or replace a global `RabbitTemplate` callback and never declare
topology.

- [ ] **Step 6: Implement the Rabbit handler**

`RabbitDeliveryHandler.channel()` returns `rabbitmq`. It resolves destinations on
enqueue and every retry. Map:

```text
ACK      -> DeliveryResult.success()
NACK     -> retryable RABBIT_NACK
TIMEOUT  -> retryable RABBIT_CONFIRM_TIMEOUT
RETURNED -> permanent RABBIT_UNROUTABLE
```

Connection and AMQP exceptions become retryable `RABBIT_CONNECTION_ERROR` with only
the exception class name. Do not include broker reply text beyond the sanitized
summary and never include payload.

- [ ] **Step 7: Run deterministic and real-broker tests**

Run the Step 3 Maven command again.

Expected: `BUILD SUCCESS`; ACK-without-return is the sole success path, real NACK,
timeout, and return are non-success outcomes, and no topology is auto-declared.

- [ ] **Step 8: Commit the Rabbit adapter**

```bash
git add egon-cola-components/egon-cola-component-transactional-outbox
git commit -m "feat: add confirmed RabbitMQ outbox delivery"
```

### Task 10: Add the Optional Transactional Annotation without Weakening Rollback

**Files:**
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/annotation/TransactionalMessage.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/aop/OutboxMessageExpressionResolver.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/aop/TransactionalMessageMethodValidator.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/aop/TransactionalMessageAop.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/exception/OutboxMessageResolutionException.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/annotation/TransactionalMessageAnnotationTest.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/aop/OutboxMessageExpressionResolverTest.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/aop/TransactionalMessageMethodValidatorTest.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/aop/TransactionalMessageAopTest.java`

**Interfaces:**
- Consumes: `TransactionalOutbox`, selected `PlatformTransactionManager`, configured advisor order, source annotation expression.
- Produces: `@TransactionalMessage(message = "#result.outboxMessage()")` for public synchronous Spring proxy calls using an explicit REQUIRED `TransactionTemplate`.

- [ ] **Step 1: Write the failing annotation contract test**

Assert runtime retention, method target, documented status, and the single required
`message` attribute:

```java
@Test
void shouldExposeOneRuntimeMethodExpression() throws Exception {
    Target target = TransactionalMessage.class.getAnnotation(Target.class);
    Retention retention = TransactionalMessage.class.getAnnotation(Retention.class);

    assertThat(target.value()).containsExactly(ElementType.METHOD);
    assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
    assertThat(TransactionalMessage.class.isAnnotationPresent(Documented.class)).isTrue();
    assertThat(TransactionalMessage.class.getDeclaredMethods())
            .extracting(Method::getName)
            .containsExactly("message");
}
```

- [ ] **Step 2: Write failing restricted-expression tests**

Use a request and result that each expose a public zero-argument
`outboxMessage()` method. Cover:

```java
assertThat(resolver.resolve("#p0.outboxMessage()", method, args, null))
        .isEqualTo(requestMessage);
assertThat(resolver.resolve("#a0.outboxMessage()", method, args, null))
        .isEqualTo(requestMessage);
assertThat(resolver.resolve("#request.outboxMessage()", method, args, null))
        .isEqualTo(requestMessage);
assertThat(resolver.resolve("#result.outboxMessage()", method, args, result))
        .isEqualTo(resultMessage);
```

Assert null, wrong type, `@environment`, `T(java.lang.System)`,
`new java.lang.String()`, `#p0.getClass()`, and static method paths all throw
`OutboxMessageResolutionException` without echoing the expression result or message
payload.

- [ ] **Step 3: Write failing method-boundary and AOP tests**

`TransactionalMessageMethodValidatorTest` rejects non-public/static/final methods,
`Future`, `CompletionStage`, any return type implementing
`org.reactivestreams.Publisher`, `@Transactional(readOnly = true)`, non-REQUIRED
propagation, and an explicitly different transaction-manager name.

`TransactionalMessageAopTest` builds a Spring `ProxyFactory` with the advisor and a
recording `PlatformTransactionManager`. Cover:

```java
@Test
void shouldInvokeTargetThenResolveResultAndEnqueueInsideOneRequiredTransaction() {
    CreateOrderResult result = proxy.create(new CreateOrderRequest("O-1"));

    assertThat(result.orderId()).isEqualTo("O-1");
    assertThat(target.calls()).isEqualTo(1);
    verify(outbox).enqueue(result.outboxMessage());
    assertThat(transactionManager.commits()).isEqualTo(1);
}

@Test
void shouldRollBackAndPreserveCheckedBusinessException() {
    assertThatThrownBy(() -> proxy.checkedFailure())
            .isSameAs(target.checkedException());

    verifyNoInteractions(outbox);
    assertThat(transactionManager.rollbacks()).isEqualTo(1);
}

@Test
void shouldRollBackBusinessWorkWhenExpressionOrEnqueueFails() {
    when(outbox.enqueue(any())).thenThrow(new OutboxStorageException("insert failed"));

    assertThatThrownBy(() -> proxy.create(new CreateOrderRequest("O-1")))
            .isInstanceOf(OutboxStorageException.class);
    assertThat(target.calls()).isEqualTo(1);
    assertThat(transactionManager.rollbacks()).isEqualTo(1);
}
```

Also prove a target exception skips expression evaluation, a null result expression
rolls back, and the business method is never executed twice.

- [ ] **Step 4: Run the tests and verify they fail**

Run:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter \
  -am test \
  -Dtest=TransactionalMessageAnnotationTest,OutboxMessageExpressionResolverTest,TransactionalMessageMethodValidatorTest,TransactionalMessageAopTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: compilation fails because annotation/AOP classes do not exist.

- [ ] **Step 5: Implement the annotation and restricted resolver**

Create:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TransactionalMessage {

    String message();
}
```

Make `OutboxMessageResolutionException` extend `OutboxException`.

`OutboxMessageExpressionResolver` caches parsed expressions by source string and
uses `MethodBasedEvaluationContext` with `DefaultParameterNameDiscoverer`. Always
register `#p0`, `#a0`, discoverable parameter names, and `#result`.

Apply these restrictions to every evaluation context:

- no `BeanResolver`;
- a `TypeLocator` that always rejects type lookup;
- an empty constructor-resolver list;
- one custom method resolver that permits only public, non-static, zero-argument
  instance methods whose declaring type is not `Object`, `Class`, `ClassLoader`,
  `Runtime`, `System`, `Thread`, `ProcessBuilder`, or a `java.lang.reflect` type;
- read-only property access only.

Require the final value to be a non-null `OutboxMessage`. Wrap parse/evaluation,
access, null, and type failures in `OutboxMessageResolutionException` with stable
field-oriented messages.

- [ ] **Step 6: Implement method validation**

`TransactionalMessageMethodValidator.validate(Method method, Class<?> targetClass)`
resolves the most-specific bridged method and enforces:

```text
public
non-static
non-final
synchronous return type
Spring-proxy invocation boundary
```

Reject `Future`, `CompletionStage`, and a return type assignable to
`org.reactivestreams.Publisher` when that class is present. Inspect a merged Spring
`@Transactional`; when present, reject `readOnly=true`, propagation other than
`REQUIRED`, and a nonblank qualifier different from the selected component
transaction-manager bean name.

- [ ] **Step 7: Implement the advisor with explicit transaction ownership**

`TransactionalMessageAop` extends `StaticMethodMatcherPointcutAdvisor` and implements
`Ordered`. Match only methods carrying the merged annotation and passing the method
validator. Build one `TransactionTemplate` from the selected manager with
`PROPAGATION_REQUIRED`; do not depend on Spring's transaction advisor order.

The interceptor callback executes in this exact sequence:

```text
invocation.proceed()
resolve annotation expression using arguments and result
transactionalOutbox.enqueue(message)
return original result
```

Because `TransactionCallback` cannot declare checked exceptions, store the original
`Throwable` in a holder, call `status.setRollbackOnly()`, exit the callback, then
rethrow the exact same instance. If transaction completion itself fails, add that
failure as suppressed to the original business throwable and still rethrow the
original. Expression, type, and enqueue runtime failures propagate normally and
cause rollback.

`getOrder()` returns `properties.getAnnotation().getOrder()`.

- [ ] **Step 8: Run the annotation and rollback tests**

Run the Step 4 Maven command again.

Expected: `BUILD SUCCESS`; restricted SpEL, result expressions, checked exception
identity, rollback, and single business invocation pass.

- [ ] **Step 9: Commit the annotation mode**

```bash
git add egon-cola-components/egon-cola-component-transactional-outbox
git commit -m "feat: add transactional message annotation"
```

### Task 11: Wire Core and Optional Spring Boot Auto-Configuration

**Files:**
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxInfrastructure.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxInfrastructureResolver.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfiguration.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxHttpAutoConfiguration.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxRabbitAutoConfiguration.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxMetricsAutoConfiguration.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/observability/MicrometerOutboxMetrics.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/transaction/DefaultTransactionalOutbox.java`
- Modify: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/transaction/OutboxAfterCommitBuffer.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/OutboxInfrastructureResolverTest.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfigurationTest.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/OutboxOptionalAutoConfigurationTest.java`
- Test: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/observability/MicrometerOutboxMetricsTest.java`

**Interfaces:**
- Consumes: all Tasks 2–10 production contracts, application infrastructure Beans, and optional classpaths.
- Produces: Boot-discoverable, override-friendly core/HTTP/Rabbit/Micrometer wiring with dedicated bounded threads and fail-fast configuration.

- [ ] **Step 1: Write failing infrastructure-selection tests**

Use `DefaultListableBeanFactory` and registered singleton Beans:

```java
@Test
void shouldSelectUniqueDataSourceAndTransactionManager() {
    beanFactory.registerSingleton("businessDataSource", dataSource);
    beanFactory.registerSingleton("businessTransactionManager", transactionManager);

    OutboxInfrastructure infrastructure =
            resolver.resolve(beanFactory, new TransactionalOutboxProperties());

    assertThat(infrastructure.dataSource()).isSameAs(dataSource);
    assertThat(infrastructure.transactionManager()).isSameAs(transactionManager);
}

@Test
void shouldRequireNamesForAmbiguousCandidates() {
    beanFactory.registerSingleton("firstDataSource", mock(DataSource.class));
    beanFactory.registerSingleton("secondDataSource", mock(DataSource.class));
    beanFactory.registerSingleton("firstTx", mock(PlatformTransactionManager.class));
    beanFactory.registerSingleton("secondTx", mock(PlatformTransactionManager.class));

    assertThatThrownBy(() ->
            resolver.resolve(beanFactory, new TransactionalOutboxProperties()))
            .isInstanceOf(OutboxConfigurationException.class)
            .hasMessageContaining("data-source-bean-name");
}

@Test
void shouldHonorExplicitBeanNames() {
    properties.getStorage().setDataSourceBeanName("secondDataSource");
    properties.getStorage().setTransactionManagerBeanName("secondTx");

    OutboxInfrastructure infrastructure = resolver.resolve(beanFactory, properties);

    assertThat(infrastructure.dataSourceBeanName()).isEqualTo("secondDataSource");
    assertThat(infrastructure.transactionManagerBeanName()).isEqualTo("secondTx");
}
```

Add a primary-candidate test and an explicit name/type mismatch test.

- [ ] **Step 2: Write failing core auto-configuration tests**

Use `ApplicationContextRunner`,
`AutoConfigurations.of(OutboxMetricsAutoConfiguration.class,
TransactionalOutboxAutoConfiguration.class, OutboxHttpAutoConfiguration.class,
OutboxRabbitAutoConfiguration.class)`, `FilteredClassLoader`,
mock infrastructure, and `storage.validate-schema=false` where a real table is not
needed. Cover:

```java
@Test
void shouldBackOffWithoutDataSource() {
    contextRunner.run(context -> assertThat(context)
            .doesNotHaveBean(TransactionalOutbox.class)
            .doesNotHaveBean(OutboxDispatcher.class));
}

@Test
void shouldCreateOneCoreRuntimeForUniqueInfrastructure() {
    contextRunner
            .withBean(DataSource.class, () -> dataSource)
            .withBean(PlatformTransactionManager.class, () -> transactionManager)
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(OutboxStore.class, () -> store)
            .withPropertyValues(
                    "egon.cola.component.transactional-outbox.storage.validate-schema=false",
                    "egon.cola.component.transactional-outbox.polling.enabled=false")
            .run(context -> assertThat(context)
                    .hasSingleBean(TransactionalOutbox.class)
                    .hasSingleBean(OutboxDispatcher.class)
                    .hasSingleBean(OutboxAfterCommitBuffer.class)
                    .hasSingleBean(TransactionalMessageAop.class));
}

@Test
void shouldDisableEveryRuntimeBean() {
    contextRunner
            .withPropertyValues(
                    "egon.cola.component.transactional-outbox.enabled=false")
            .run(context -> assertThat(context)
                    .doesNotHaveBean(TransactionalOutbox.class)
                    .doesNotHaveBean(OutboxDispatcher.class)
                    .doesNotHaveBean(OutboxPoller.class));
}

@Test
void shouldBackOffAnnotationAdvisorOnly() {
    contextRunner
            .withPropertyValues(
                    "egon.cola.component.transactional-outbox.annotation.enabled=false",
                    "egon.cola.component.transactional-outbox.storage.validate-schema=false")
            .run(context -> assertThat(context)
                    .hasSingleBean(TransactionalOutbox.class)
                    .doesNotHaveBean(TransactionalMessageAop.class));
}
```

Add custom Bean backoff tests for serializer, store, ID generator, retry policy,
failure classifier, scheduler, executor, and `TransactionalOutbox`.

- [ ] **Step 3: Write failing optional-classpath and Rabbit safety tests**

Cover:

- HTTP disabled by default;
- HTTP enabled plus `RestClient` plus destinations creates one HTTP handler;
- a custom `HttpDestinationResolver`, credential provider, classifier, or handler
  backs off only its matching default;
- `FilteredClassLoader("org.springframework.web.client.RestClient")` prevents HTTP
  Beans without preventing core Beans;
- Rabbit disabled by default;
- Rabbit enabled with no `RabbitTemplate` backs off;
- Rabbit enabled with a template whose connection factory lacks correlated confirms
  or publisher returns fails startup with `OutboxConfigurationException`;
- Rabbit enabled with correlated confirms, returns, mandatory publishing, and one
  destination creates one Rabbit handler;
- hidden Rabbit/Micrometer classes do not prevent the core context.

- [ ] **Step 4: Write failing Micrometer metric tests**

Use `SimpleMeterRegistry`:

```java
@Test
void shouldRecordOnlyLowCardinalityTagsAndCachedBacklog() {
    metrics.enqueue(true);
    metrics.delivery("http", "success", Duration.ofMillis(12));
    metrics.updateBacklog(7);

    assertThat(registry.get("egon.cola.outbox.enqueue")
            .tag("result", "created").counter().count()).isEqualTo(1.0);
    assertThat(registry.get("egon.cola.outbox.delivery")
            .tag("channel", "http").tag("result", "success")
            .counter().count()).isEqualTo(1.0);
    assertThat(registry.get("egon.cola.outbox.backlog").gauge().value())
            .isEqualTo(7.0);
    assertThat(registry.getMeters())
            .flatExtracting(meter -> meter.getId().getTags())
            .extracting(Tag::getKey)
            .doesNotContain("messageId", "idempotencyKey", "destination", "url");
}
```

- [ ] **Step 5: Run the tests and verify they fail**

Run:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter \
  -am test \
  -Dtest=OutboxInfrastructureResolverTest,TransactionalOutboxAutoConfigurationTest,OutboxOptionalAutoConfigurationTest,MicrometerOutboxMetricsTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: compilation fails because the auto-configurations are not present.

- [ ] **Step 6: Implement deterministic infrastructure selection**

Create:

```java
public record OutboxInfrastructure(
        DataSource dataSource,
        String dataSourceBeanName,
        PlatformTransactionManager transactionManager,
        String transactionManagerBeanName
) {
}
```

`OutboxInfrastructureResolver.resolve(ConfigurableListableBeanFactory,
TransactionalOutboxProperties)`:

1. resolves an explicit nonblank bean name and verifies its type;
2. otherwise uses the sole candidate or Spring's sole primary candidate;
3. reports all candidate bean names, but no connection details, when ambiguous;
4. applies the same rule to `PlatformTransactionManager`;
5. when the selected manager is a `DataSourceTransactionManager`, verifies its
   DataSource is the selected instance;
6. leaves other manager compatibility to the runtime transaction guard;
7. never silently chooses by map iteration order.

- [ ] **Step 7: Implement the core auto-configuration**

Annotate `TransactionalOutboxAutoConfiguration` with:

```java
@AutoConfiguration(afterName = {
        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration",
        "org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration"
})
@EnableConfigurationProperties(TransactionalOutboxProperties.class)
@ConditionalOnClass({DataSource.class, JdbcTemplate.class})
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(
        prefix = "egon.cola.component.transactional-outbox",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
```

Use `@Bean` methods and `@ConditionalOnMissingBean`; never use component scanning.
Wire:

```text
OutboxConfigurationValidator
OutboxInfrastructureResolver -> OutboxInfrastructure
selected JdbcTemplate
selected NamedParameterJdbcTemplate
OutboxMessageValidator
OutboxMessageSerializer
OutboxIdGenerator
OutboxStore
OutboxTransactionGuard
DeliveryHandlerRegistry
OutboxRetryPolicy
DeliveryFailureClassifier
OutboxDeadLetterNotifier
OutboxAfterCommitBuffer
TransactionalOutbox
OutboxWorkerIdentity
OutboxDispatcher
OutboxCleanupJob
OutboxCommittedEventListener
OutboxPoller
optional TransactionalMessageAop
```

Require exactly one application-managed `ObjectMapper`; absence fails with a clear
`OutboxConfigurationException`.

Create named infrastructure Beans:

```java
@Bean(name = "outboxDeliveryExecutor")
@ConditionalOnMissingBean(name = "outboxDeliveryExecutor")
ThreadPoolTaskExecutor outboxDeliveryExecutor(TransactionalOutboxProperties properties);

@Bean(name = "outboxTaskScheduler")
@ConditionalOnMissingBean(name = "outboxTaskScheduler")
ThreadPoolTaskScheduler outboxTaskScheduler(TransactionalOutboxProperties properties);
```

Set delivery core/max pool sizes to configured concurrency, configured bounded queue,
`egon-cola-outbox-delivery-` thread prefix, wait-for-tasks-on-shutdown, and configured
grace seconds. Set scheduler pool size 1, remove-on-cancel, the
`egon-cola-outbox-scheduler-` prefix, and the same shutdown grace. Do not add
`@EnableAsync` or `@EnableScheduling`.

When `storage.validate-schema=true`, register `OutboxSchemaValidator` with
`initMethod="validate"` so schema failure occurs before `OutboxPoller` starts.
Make the poller Bean resolve the optional validator before construction, guaranteeing
its init method has completed. When validation is false, do not query or mutate
schema.

Create `TransactionalMessageAop` only when annotation mode is enabled. Pass the
resolved transaction-manager name to method validation.

Update `DefaultTransactionalOutbox` to accept `OutboxMetrics` and call
`metrics.enqueue(receipt.created())` after store success. Never record a metric for
a failed insert as if it were accepted.

Update `OutboxAfterCommitBuffer` to accept `OutboxMetrics` and call
`metrics.wakeupRejected()` when publishing the post-commit event throws. The
dispatcher already records executor rejection through the same safe counter.

- [ ] **Step 8: Implement optional HTTP, Rabbit, and Micrometer configurations**

`OutboxHttpAutoConfiguration` is conditional on `RestClient`, HTTP enabled, core
configuration, and an `HttpDestinationResolver`. Register property resolver,
empty credential provider, default classifier, and handler with matching
`@ConditionalOnMissingBean`.

`OutboxRabbitAutoConfiguration` is conditional on `RabbitTemplate`, Rabbit enabled,
core configuration, and a `RabbitDestinationResolver`. Before handler construction:

```text
connectionFactory.isPublisherConfirms() == true
connectionFactory.isPublisherReturns() == true
RabbitTemplate mandatory evaluation for a probe message == true
```

Reject any failed requirement with `OutboxConfigurationException`; never call
`setMandatory`, replace callbacks, or mutate the application's connection factory.
Then register property resolver, `RabbitTemplateMessagePublisher`, and handler with
matching backoff.

`OutboxMetricsAutoConfiguration` runs before the core configuration and, when
`MeterRegistry` exists, creates `MicrometerOutboxMetrics`; otherwise the core creates
`NoopOutboxMetrics`.

`MicrometerOutboxMetrics` owns these exact meters:

```text
egon.cola.outbox.enqueue
egon.cola.outbox.claim
egon.cola.outbox.delivery
egon.cola.outbox.retry
egon.cola.outbox.dead
egon.cola.outbox.lease_lost
egon.cola.outbox.delivery.duration
egon.cola.outbox.backlog
```

Use only `channel`, `result`, and `status` tags. The backlog gauge callback returns
one `AtomicLong` cache and never queries the database. Record
`wakeupRejected()` on `egon.cola.outbox.delivery` with stable
`channel=internal,result=wakeup_rejected` tags rather than introducing an
unbounded identifier.

- [ ] **Step 9: Register Boot auto-configurations**

Create `AutoConfiguration.imports` with:

```text
top.egon.cola.component.outbox.autoconfigure.OutboxMetricsAutoConfiguration
top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxAutoConfiguration
top.egon.cola.component.outbox.autoconfigure.OutboxHttpAutoConfiguration
top.egon.cola.component.outbox.autoconfigure.OutboxRabbitAutoConfiguration
```

Do not add `spring.factories`.

- [ ] **Step 10: Run auto-configuration and metric tests**

Run the Step 5 Maven command again.

Expected: `BUILD SUCCESS`; core backoff, named infrastructure, optional classpaths,
Rabbit safety checks, custom Bean backoff, thread naming, and low-cardinality metrics
pass.

- [ ] **Step 11: Commit Boot wiring**

```bash
git add egon-cola-components/egon-cola-component-transactional-outbox
git commit -m "feat: auto-configure transactional outbox runtime"
```

### Task 12: Prove PostgreSQL Concurrency, Recovery, Retry, Cleanup, and Index Semantics

**Files:**
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test/src/test/java/top/egon/cola/component/outbox/test/PostgresqlOutboxConcurrencyIntegrationTest.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test/src/test/java/top/egon/cola/component/outbox/test/PostgresqlOutboxRecoveryIntegrationTest.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test/src/test/java/top/egon/cola/component/outbox/test/PostgresqlOutboxCleanupIntegrationTest.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test/src/test/java/top/egon/cola/component/outbox/test/PostgresqlOutboxQueryPlanIntegrationTest.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test/src/test/java/top/egon/cola/component/outbox/test/OutboxDataSafetyIntegrationTest.java`
- Modify: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test/src/test/java/top/egon/cola/component/outbox/test/PostgresqlOutboxTestSupport.java`

**Interfaces:**
- Consumes: the complete core runtime from Tasks 4–11 and a real PostgreSQL 16 container.
- Produces: executable acceptance evidence for AC-003 through AC-012, AC-016, AC-018, AC-020, and AC-022.

- [ ] **Step 1: Write concurrent claim and lock-release tests**

Seed 200 due records in one transaction. Use two executor threads, a
`CyclicBarrier(2)`, distinct owner tokens, and concurrent
`claimDue(100, owner, Duration.ofSeconds(60))` calls.
Assert:

```java
assertThat(firstIds).doesNotContainAnyElementsOf(secondIds);
assertThat(Stream.concat(firstIds.stream(), secondIds.stream()).distinct())
        .hasSize(200);
```

Add a test that starts a second database transaction after `claimDue` returns and
successfully executes:

```sql
select id
from egon_cola_outbox_message
where message_id = ?
for update nowait
```

This proves the store committed the short claim transaction before external delivery.
Use bounded futures with a five-second timeout; do not use sleeps as correctness
conditions.

- [ ] **Step 2: Write due-time, lease-recovery, and poller-fallback tests**

Cover:

- a future `availableAt` is not claimed;
- a due `RETRY_WAIT` is claimed;
- a non-expired `PROCESSING` row is skipped;
- an expired `PROCESSING` row is reclaimed with incremented attempt count;
- the same node ID with a new random claim token prevents its old task from updating;
- an `OutboxAfterCommitBuffer` publisher that throws does not roll back the business
  row or outbox row;
- calling `dispatcher.dispatchDue()` later delivers that row and marks `SUCCEEDED`;
- one permanently failing record and one successful record in the same real claim
  finish as `DEAD` and `SUCCEEDED` respectively;
- retryable failure writes `RETRY_WAIT` with a database-derived future
  `next_attempt_at`, then max-attempt exhaustion writes `DEAD`.

Use Awaitility only for bounded asynchronous observation:

```java
await().atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> assertThat(status("message-1"))
                .isEqualTo(OutboxStatus.SUCCEEDED));
```

- [ ] **Step 3: Write cleanup and idempotency-window tests**

Create one old `SUCCEEDED`, one recent `SUCCEEDED`, and one each of `PENDING`,
`PROCESSING`, `RETRY_WAIT`, and `DEAD`. Run `deleteSucceeded(retention, 500)`.
Assert only the old successful row is deleted.

Then enqueue the deleted row's former idempotency key and assert
`OutboxReceipt.created()` is true. Assert the retained rows still reject conflicting
content. This explicitly proves cleanup ends only the component-side enqueue
deduplication window.

- [ ] **Step 4: Write schema and query-plan tests**

Assert both unique constraints with real concurrent inserts. Query PostgreSQL
catalogs for the five named indexes and required columns.

For claim plans, insert enough due/future rows, run `analyze`, locally disable
sequential scan inside the test transaction, and execute:

```sql
explain (format json)
select id
from egon_cola_outbox_message
where status in ('PENDING', 'RETRY_WAIT')
  and next_attempt_at <= clock_timestamp()
order by next_attempt_at, id
limit 100
```

Assert parsed JSON contains `idx_outbox_claim`. Run the equivalent expired
`PROCESSING` query and assert `idx_outbox_reclaim`. Reset the local planner setting
by completing the test transaction.

- [ ] **Step 5: Write persisted-error and secret-boundary tests**

Drive HTTP response body `Authorization: should-not-persist`, a thrown exception
whose message contains `Cookie=should-not-persist`, and a long control-character
error through the dispatcher. Query `last_error_code` and `last_error_message` and
assert:

```java
assertThat(lastErrorMessage)
        .doesNotContain("Authorization", "Cookie", "should-not-persist")
        .doesNotContain("\n", "\r")
        .hasSizeLessThanOrEqualTo(2000);
```

Also assert `headers_json` rejects sensitive message headers before insert, metrics
contain no high-cardinality tags, and dead-letter events expose no payload or header
accessor.

- [ ] **Step 6: Run the PostgreSQL acceptance suite**

Run:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test \
  -am test \
  -Dtest=PostgresqlOutboxConcurrencyIntegrationTest,PostgresqlOutboxRecoveryIntegrationTest,PostgresqlOutboxCleanupIntegrationTest,PostgresqlOutboxQueryPlanIntegrationTest,OutboxDataSafetyIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: `BUILD SUCCESS`; no test is skipped, both concurrent workers finish within
the bound, query plans use the intended indexes, and only eligible successful rows
are deleted.

- [ ] **Step 7: Commit the PostgreSQL acceptance suite**

```bash
git add egon-cola-components/egon-cola-component-transactional-outbox
git commit -m "test: verify transactional outbox PostgreSQL recovery"
```

### Task 13: Add Exactly Two Starter Usage Samples

**Files:**
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test/src/test/java/top/egon/cola/component/outbox/test/TransactionalOutboxDirectApiSampleTest.java`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test/src/test/java/top/egon/cola/component/outbox/test/TransactionalMessageAnnotationSampleTest.java`

**Interfaces:**
- Consumes: the public starter API and auto-configuration exactly as a consumer application does.
- Produces: two executable samples—direct API as the recommended path and annotation mode as the optional path.

- [ ] **Step 1: Write the failing direct API sample**

Create a real PostgreSQL-backed Spring test context with one DataSource, one
`DataSourceTransactionManager`, an application `ObjectMapper`, the packaged schema,
polling disabled, and a custom `DeliveryHandler` for `sample`.

The sample service must be:

```java
static class OrderApplicationService {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionalOutbox transactionalOutbox;

    OrderApplicationService(
            JdbcTemplate jdbcTemplate,
            TransactionalOutbox transactionalOutbox
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionalOutbox = transactionalOutbox;
    }

    @Transactional
    public OutboxReceipt createOrder(String orderId) {
        jdbcTemplate.update(
                "insert into outbox_sample_order(id) values (?)", orderId);
        return transactionalOutbox.enqueue(OutboxMessage.builder()
                .idempotencyKey("order:created:" + orderId)
                .channel("sample")
                .destination("order-created-v1")
                .payload(new OrderCreatedEvent(orderId))
                .schemaVersion("1")
                .build());
    }
}
```

Assert the business row and outbox row commit, receipt is created, message ID is
nonblank, and the persisted destination remains the logical name. Call the method
again with the same content and assert the existing receipt is returned without a
second row.

- [ ] **Step 2: Write the failing annotation sample**

Configure AOP and use:

```java
static class AnnotatedOrderApplicationService {

    private final JdbcTemplate jdbcTemplate;

    AnnotatedOrderApplicationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @TransactionalMessage(message = "#result.outboxMessage()")
    public CreateOrderResult createOrder(String orderId) {
        jdbcTemplate.update(
                "insert into outbox_sample_order(id) values (?)", orderId);
        return new CreateOrderResult(
                orderId,
                OutboxMessage.builder()
                        .idempotencyKey("order:annotated-created:" + orderId)
                        .channel("sample")
                        .destination("order-created-v1")
                        .payload(new OrderCreatedEvent(orderId))
                        .build()
        );
    }
}
```

Assert one proxy call commits both rows. Add one annotated method whose result
expression returns null and assert neither business nor outbox row commits.

- [ ] **Step 3: Run the samples and verify the initial failure**

Run:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test \
  -am test \
  -Dtest=TransactionalOutboxDirectApiSampleTest,TransactionalMessageAnnotationSampleTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected before completing sample configuration: tests fail because consumer Beans,
schema bootstrap, or sample services are absent.

- [ ] **Step 4: Complete only the sample-owned Spring configuration**

Add nested `@Configuration(proxyBeanMethods = false)` classes that expose the
sample services and one custom handler:

```java
@Bean
DeliveryHandler sampleDeliveryHandler() {
    return new DeliveryHandler() {
        @Override
        public String channel() {
            return "sample";
        }

        @Override
        public void validateDestination(String destination) {
            if (!"order-created-v1".equals(destination)) {
                throw new OutboxValidationException("Unknown sample destination");
            }
        }

        @Override
        public DeliveryResult deliver(DeliveryContext context) {
            return DeliveryResult.success();
        }
    };
}
```

Import `AopAutoConfiguration`, Jackson auto-configuration, transaction
auto-configuration, and the outbox auto-configurations. Keep polling disabled so
the sample tests prove enqueue behavior without a long-running service or latch.

- [ ] **Step 5: Run both executable samples**

Run the Step 3 Maven command again.

Expected: `BUILD SUCCESS`; exactly two sample test classes prove direct and annotation
usage.

- [ ] **Step 6: Commit the consumer samples**

```bash
git add egon-cola-components/egon-cola-component-transactional-outbox
git commit -m "test: add transactional outbox starter samples"
```

### Task 14: Document the Contract and Run the Full Repository Verification

**Files:**
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/README.md`
- Create: `egon-cola-components/egon-cola-component-transactional-outbox/README.zh-CN.md`
- Verify: all component files and repository wiring from Tasks 1–13.

**Interfaces:**
- Consumes: the final public API, configuration metadata, SQL template, tests, and repository reactor.
- Produces: consumer documentation and final evidence for all 24 acceptance criteria.

- [ ] **Step 1: Write both READMEs from the implemented API**

The English and Chinese documents must contain matching sections for:

```text
What problem this solves
At-least-once guarantee and duplicate-delivery window
When not to use it
Maven dependency
PostgreSQL migration copy-and-renumber procedure
Direct API example
Annotation example and proxy limitations
Single-DataSource configuration
Multi-DataSource explicit bean names
HTTP destination and credential-provider configuration
Rabbit correlated confirm, returns, and mandatory prerequisites
Custom DeliveryHandler example
Retry, lease, DEAD, and crash recovery
Cleanup and its idempotency-window consequence
Metrics and safe logging
Java 21 / Spring Boot 3.5.x / PostgreSQL / imperative JDBC compatibility
Unsupported reactive, cross-database, distributed-transaction, and exactly-once cases
```

Copy code from the two executable sample tests so examples compile. State explicitly:

- introducing the starter does not create its table;
- consumers copy
  `db/transactional-outbox/postgresql/V1__create_transactional_outbox_schema.sql`
  into their own migration sequence and assign their next local version;
- HTTP/Rabbit are off by default;
- Rabbit success means correlated ACK and no mandatory return;
- a repeated remote side effect can occur after remote success and local crash;
- downstream consumers must deduplicate on stable `messageId`;
- cleanup deletes only `SUCCEEDED`, retains `DEAD`, and ends the outbox-side
  deduplication window for deleted keys;
- the component has no Admin, replay API, inbox, or topology declaration.

Do not include credentials, arbitrary URLs in message examples, infinite waits, or a
`CountDownLatch` used as the success contract.

- [ ] **Step 2: Run the focused starter and test reactor**

Run:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter,egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test \
  -am test
```

Expected: `BUILD SUCCESS`; unit, PostgreSQL, WireMock, RabbitMQ, auto-configuration,
and both sample suites run with zero skipped required integration tests.

- [ ] **Step 3: Run the full components reactor**

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml test
```

Expected: `BUILD SUCCESS` for the complete components reactor.

- [ ] **Step 4: Verify forbidden starter dependencies are absent**

Run:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter \
  -am dependency:tree \
  -Dincludes=org.flywaydb,org.postgresql,org.xerial,org.springframework.boot:spring-boot-starter-actuator,org.redisson,org.testcontainers
```

Expected: `BUILD SUCCESS` and no listed artifact under the transactional-outbox
starter dependency tree. Test-module dependencies are outside this boundary.

- [ ] **Step 5: Verify reactor, BOM, auto-configuration, and migration shape**

Run:

```bash
test "$(rg -c '<module>egon-cola-component-transactional-outbox</module>' egon-cola-components/pom.xml)" -eq 1
test "$(rg -c '<artifactId>egon-cola-component-transactional-outbox-starter</artifactId>' egon-cola-components/egon-cola-components-bom/pom.xml)" -eq 1
test "$(find egon-cola-components/egon-cola-component-transactional-outbox -path '*/src/main/resources/db/*' -type f | wc -l | tr -d ' ')" -eq 1
test ! -e egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/resources/db/migration
test -f egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
test -f egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/target/classes/META-INF/spring-configuration-metadata.json
rg -q 'egon\\.cola\\.component\\.transactional-outbox' egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-starter/target/classes/META-INF/spring-configuration-metadata.json
```

Expected: every command exits 0.

- [ ] **Step 6: Run the root integration contract**

Because the component changes the shared reactor and BOM, run:

```bash
./mvnw -B -ntp clean integration-test
```

Expected: `BUILD SUCCESS`. Do not start an application after Maven completes.

- [ ] **Step 7: Verify scope and diff hygiene**

Run:

```bash
git diff --check
git status --short
git diff --name-only "$(git merge-base HEAD main)"...HEAD -- egon-cola-archetypes
```

Expected:

- `git diff --check` exits 0;
- status lists only the two uncommitted README files at this point;
- the archetype diff command prints nothing;
- no existing migration is modified;
- no third functional Maven module, Admin, UI, or replay endpoint exists.

- [ ] **Step 8: Commit the documentation after verification**

```bash
git add \
  egon-cola-components/egon-cola-component-transactional-outbox/README.md \
  egon-cola-components/egon-cola-component-transactional-outbox/README.zh-CN.md
git commit -m "docs: document transactional outbox component"
```

- [ ] **Step 9: Perform completion verification and review handoff**

Invoke `superpowers:verification-before-completion`, rerun:

```bash
git diff HEAD^ HEAD --check
git status --short --branch
```

Expected: the documentation commit is clean and the worktree has no uncommitted
component changes. Then invoke `superpowers:requesting-code-review`; resolve every
correctness or Spec-coverage issue before offering branch integration through
`superpowers:finishing-a-development-branch`.

## Acceptance-Criteria Coverage

| Acceptance criterion | Primary implementation task | Executed proof |
|---|---:|---|
| AC-001 Modules and BOM | 1 | Task 14 shape checks |
| AC-002 Auto-configuration imports | 11 | `TransactionalOutboxAutoConfigurationTest`, Task 14 resource check |
| AC-003 Same-transaction commit | 6 | `TransactionalOutboxTransactionIntegrationTest` |
| AC-004 Same-transaction rollback | 6 | business and insert-failure rollback tests |
| AC-005 No-transaction failure | 6 | guard unit and PostgreSQL integration tests |
| AC-006 DataSource match | 6, 11 | wrong manager test and infrastructure resolver tests |
| AC-007 No pre-commit delivery | 6, 12 | after-commit buffer and real recovery tests |
| AC-008 Lost wake-up recovery | 7, 12 | throwing publisher plus later poll/dispatch |
| AC-009 Multi-instance claim | 4, 12 | concurrent disjoint claim test |
| AC-010 Crash recovery | 4, 12 | expired lease and stale-owner CAS tests |
| AC-011 Finite retry | 5, 7, 12 | retry policy, dispatcher, and database state tests |
| AC-012 Poison isolation | 7, 12 | unit and real PostgreSQL mixed-batch tests |
| AC-013 HTTP semantics | 8 | WireMock integration matrix |
| AC-014 Rabbit confirm | 9 | RabbitMQ Testcontainer ACK/NACK/timeout/return tests |
| AC-015 Custom channel | 5, 13 | registry and sample custom-handler tests |
| AC-016 Idempotency key | 4, 6, 12 | store conflict and cleanup-window tests |
| AC-017 Annotation safety | 10, 13 | AOP rollback and annotation sample tests |
| AC-018 Security | 2, 8, 9, 12 | header, response-body, persisted-error, metric tests |
| AC-019 Observability | 7, 11 | notifier isolation, metrics, lease-loss tests |
| AC-020 Cleanup boundary | 7, 12 | cleanup unit and real-state matrix tests |
| AC-021 Dependency boundary | 1 | Task 14 dependency tree |
| AC-022 Schema | 4, 12 | packaged SQL execution, catalog, and plan tests |
| AC-023 Compatibility boundary | 1, 14 | POM and bilingual README |
| AC-024 No long-running service | all | Maven/Testcontainers verification only |

## Implementation Completion Checklist

- Every production class has a focused owner task and a test seam.
- Every external call occurs after claim transaction completion.
- Every retry preserves the database `message_id`.
- Every state transition is owner-conditioned.
- Every optional integration is isolated behind classpath and enablement conditions.
- Every database change is represented by the one new non-automatic PostgreSQL SQL
  resource.
- Every task ends in one scoped commit.
- No archetype, existing migration, Admin surface, or replay API is touched.
- Final verification distinguishes unit, PostgreSQL, HTTP, RabbitMQ, auto-config,
  components reactor, root integration, dependency boundary, and diff hygiene.

## Execution Handoff

Choose one execution mode after this Plan is approved:

1. **Subagent-Driven (recommended):** invoke
   `superpowers:subagent-driven-development`; use one fresh implementation agent per
   task and run requirements review plus code-quality review before moving to the
   next task.
2. **Inline Execution:** invoke `superpowers:executing-plans`; execute Tasks 1–14 in
   the isolated worktree in ordered batches with explicit review checkpoints.

Neither mode may skip the per-task failing-test proof, one-commit boundary, final
root `clean integration-test`, or the no-long-running-service constraint.
