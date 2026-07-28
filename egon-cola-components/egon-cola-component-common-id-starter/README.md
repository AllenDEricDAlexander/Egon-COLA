# egon-cola-component-common-id-starter

[English](README.md) | [中文](README.zh-CN.md)

## Overview

This Starter provides the Spring Boot 3 entry point for Egon COLA's stateful, pure-JDK Snowflake ID generator. The algorithm remains in `egon-cola-component-common-id` without Spring dependencies; this module only binds configuration and creates the default bean.

Use `LongIdGenerator.nextLongId()` for database `BIGINT` primary keys. The inherited `IdGenerator.nextId()` method returns the same value as a decimal string for compatibility.

## Maven Dependency

Import `egon-cola-components-bom`, then add the Starter without a version:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-common-id-starter</artifactId>
</dependency>
```

For a non-Spring application, depend directly on the pure-Java module instead:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-common-id</artifactId>
</dependency>
```

## Configuration

`machine-id` is mandatory when the Starter is enabled. It is never inferred from an IP address, MAC address, hostname, port, process ID, random value, or hash.

```yaml
egon:
  cola:
    component:
      id:
        enabled: true
        machine-id: 17
        max-clock-backward: 5ms
```

| Property | Type | Default | Description |
|---|---|---|---|
| `egon.cola.component.id.enabled` | `boolean` | `true` | Enables the default Snowflake bean. |
| `egon.cola.component.id.machine-id` | `long` | none | Required explicit node ID from `0` to `1023`. |
| `egon.cola.component.id.max-clock-backward` | `Duration` | `5ms` | Largest rollback that the process may briefly wait out. |

Missing or out-of-range `machine-id` values fail during application context startup. Setting `enabled=false` creates no generator. A custom `IdGenerator` or `LongIdGenerator` bean makes the default auto-configuration back off.

## Spring Usage

```java
import org.springframework.stereotype.Service;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

@Service
public class OrderService {

    private final LongIdGenerator idGenerator;

    public OrderService(LongIdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    public long createOrder() {
        long orderId = idGenerator.nextLongId();
        // Persist orderId into a BIGINT column.
        return orderId;
    }
}
```

The same bean is also injectable as `IdGenerator`; `nextId()` returns the decimal form of `nextLongId()`.

## Non-Spring Usage

Create one long-lived generator instance per process and supply a deployment-assigned machine ID:

```java
import java.time.Duration;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

LongIdGenerator idGenerator = new SnowflakeIdGenerator(17, Duration.ofMillis(5));
long id = idGenerator.nextLongId();
String text = idGenerator.nextId();
```

Do not create a new generator for each request. Its in-memory timestamp and sequence state is what provides strict monotonicity within that instance.

## Database `BIGINT`

Use a signed 64-bit column and bind the value as a Java `long`:

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    created_at TIMESTAMP NOT NULL
);
```

```java
preparedStatement.setLong(1, idGenerator.nextLongId());
```

The generated value is positive. When exposing it to JavaScript clients, consider serializing it as a string because JavaScript numbers cannot exactly represent every 64-bit integer.

## Fixed ID Layout

The layout and epoch are protocol constants and cannot be configured:

```text
0 | 41-bit elapsed milliseconds | 10-bit machine ID | 12-bit sequence
```

- Sign bit: always `0`, producing a positive `long`.
- Epoch: `2026-01-01T00:00:00Z`.
- Timestamp: 41 elapsed-millisecond bits, approximately 69.7 years.
- Machine ID: 10 bits, up to 1,024 nodes (`0..1023`).
- Sequence: 12 bits, up to 4,096 IDs per millisecond per node (`0..4095`).

The all-zero encoding is reserved so the generator never returns `0`. Consequently,
only machine `0` at the exact Epoch millisecond starts at sequence `1`; every normal
operating millisecond retains the full 4,096-ID sequence capacity.

One generator instance is thread-safe, duplicate-free, and strictly increasing at its successful CAS linearization point. Correctly configured nodes with normal clocks produce globally unique IDs that are ordered by time trend. Without central coordination, IDs from different nodes do not guarantee the strict global order of real business events.

## Parsing

```java
import top.egon.cola.component.common.id.snowflake.SnowflakeId;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdParser;

SnowflakeId decoded = SnowflakeIdParser.parse(id);
decoded.generatedAt();
decoded.machineId();
decoded.sequence();
```

The parser rejects negative values and always uses the same fixed layout and epoch as the generator.

## Clock Rollback Policy

- A rollback not larger than `max-clock-backward` is waited out using short parks and a bounded monotonic-time deadline.
- Waiting is interrupt-aware; interruption stops generation and preserves the thread's interrupt flag.
- A larger rollback, or a small rollback that does not recover within the deadline, immediately raises `ClockMovedBackwardException`. Its diagnostics include the current time, last used time, rollback distance, and machine ID.
- The implementation does not continue on an invented logical timestamp after a serious rollback. That would be unsafe after a process restart because the in-memory watermark is lost.
- Timestamp exhaustion raises `SnowflakeTimestampOutOfRangeException` instead of wrapping the 41-bit field.

Run reliable NTP on every node and monitor time synchronization. A pure in-memory generator cannot unconditionally guarantee no duplicates across a serious clock rollback combined with process restart.

## Machine ID Allocation

Every simultaneously active generator must have a unique `machine-id`. Reusing one ID on two nodes can produce collisions.

For a Kubernetes StatefulSet, a controlled mapping from the stable ordinal is suitable when the replica count stays within 1,024:

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: order-service
spec:
  serviceName: order-service
  replicas: 3
  template:
    spec:
      containers:
        - name: app
          env:
            - name: EGON_COLA_COMPONENT_ID_MACHINE_ID
              valueFrom:
                fieldRef:
                  fieldPath: metadata.labels['apps.kubernetes.io/pod-index']
```

Confirm that the cluster supplies the pod-index label, that ordinals are not reused by concurrently active pods during rollout, and that no other workload uses the same allocation range. An ordinary Deployment's random Pod name is not a stable machine ID and must not be hashed or parsed as one.

## UUIDv7 Compatibility and Boundaries

`UuidV7` and `UuidV7Generator` remain temporarily available in `egon-cola-component-common-id` as pure-JDK RFC 9562 compatibility APIs. They are deprecated for removal and no longer depend on `uuid-creator`. The Starter never auto-configures UUIDv7 and Snowflake is the default database-key strategy.

Do not mechanically replace UUID values that are part of a UUIDv7 wire contract, a `VARCHAR(36)` schema, or UUID-specific sharding/validation. Migrate those consumers only with an explicit contract and data migration.

This component intentionally does not provide automatic node discovery, Redis leases, database segments, batch prefetch, persistent watermarks, or network coordination.

## Validation

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-common-id-starter -am clean test
```
