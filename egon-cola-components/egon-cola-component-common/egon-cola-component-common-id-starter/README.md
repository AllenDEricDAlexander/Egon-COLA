# egon-cola-component-common-id-starter

[English](README.md) | [中文](README.zh-CN.md)

## Overview

This is the single Egon COLA ID module. It contains the stateful, pure-JDK Snowflake interfaces and algorithm together with the Spring Boot 3 configuration binding. The core algorithm packages do not import Spring APIs, while applications consume one Starter artifact.

Use `SnowflakeIdGenerator.nextLongId()` for database `BIGINT` primary keys. `SnowflakeIdGenerator.nextId()` returns the same value as a decimal string for compatibility. Both are static entry points on one process-wide engine; `SnowflakeIdGenerator` cannot be constructed and has no reset seam. `LongIdGenerator` remains the named strategy type and declares both operations abstractly.

## Maven Dependency

Import `egon-cola-components-bom`, then add the Starter without a version:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-common-id-starter</artifactId>
</dependency>
```

Non-Spring applications use the same artifact and call `SnowflakeIdGenerator.initialize(machineId, maxClockBackward)` once during startup; Spring auto-configuration performs that binding for you and is only activated by a Spring Boot application context.

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
| `egon.cola.component.id.enabled` | `boolean` | `true` | Enables the default Snowflake binding. |
| `egon.cola.component.id.machine-id` | `long` | none | Required explicit node ID from `0` to `1023`. |
| `egon.cola.component.id.max-clock-backward` | `Duration` | `5ms` | Largest rollback that the process may briefly wait out. |

Missing or out-of-range `machine-id` values fail during application context startup. Setting `enabled=false` binds no engine. A custom `IdGenerator` or `LongIdGenerator` bean makes the default auto-configuration back off.

## Spring Usage

```java
import org.springframework.stereotype.Service;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

@Service
public class OrderService {

    public long createOrder() {
        long orderId = SnowflakeIdGenerator.nextLongId();
        // Persist orderId into a BIGINT column.
        return orderId;
    }
}
```

No generator bean is published, so nothing is injected: auto-configuration binds the engine during the configuration phase and business code calls the static entry. `nextId()` returns the decimal form of `nextLongId()`.

## Non-Spring Usage

Bind once per process with a deployment-assigned machine ID, then call the static entries:

```java
import java.time.Duration;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

SnowflakeIdGenerator.initialize(17L, Duration.ofMillis(5));
long id = SnowflakeIdGenerator.nextLongId();
String text = SnowflakeIdGenerator.nextId();
```

Repeated `initialize` calls with an identical configuration reuse the bound engine; a different configuration is rejected instead of reseeding a live sequence. `SnowflakeLongIdGenerator` is the named engine behind the facade and is constructed directly only by test fixtures and benchmarks that need an isolated clock.

Do not rebind per request. The in-memory timestamp and sequence state of the bound engine is what provides strict monotonicity within the process.

## Database `BIGINT`

Use a signed 64-bit column and bind the value as a Java `long`:

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    created_at TIMESTAMP NOT NULL
);
```

```java
preparedStatement.setLong(1, SnowflakeIdGenerator.nextLongId());
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

The bound engine is thread-safe, duplicate-free, and strictly increasing at its successful CAS linearization point. Correctly configured nodes with normal clocks produce globally unique IDs that are ordered by time trend. Without central coordination, IDs from different nodes do not guarantee the strict global order of real business events.

## Clock Rollback Policy

- A rollback not larger than `max-clock-backward` is waited out using short parks and a bounded monotonic-time deadline.
- Waiting is interrupt-aware: a thread that is already interrupted, or interrupted while waiting, stops generation with `IdGenerationInterruptedException` and keeps its interrupt flag set.
- A larger rollback, or a small rollback that does not recover within the deadline, immediately raises `ClockMovedBackwardException`. Its diagnostics include the current time, last used time, rollback distance, and machine ID.
- The implementation does not continue on an invented logical timestamp after a serious rollback. That would be unsafe after a process restart because the in-memory watermark is lost.
- A wall clock outside the representable window raises `SnowflakeTimestampOutOfRangeException` instead of wrapping the 41-bit field. The window ends when the 41 elapsed-millisecond bits are exhausted, and it also excludes any instant before the Epoch.

Run reliable NTP on every node and monitor time synchronization. A pure in-memory generator cannot unconditionally guarantee no duplicates across a serious clock rollback combined with process restart.

Configuration failures and these generation failures all throw the common-core `CommonException` with `ResultCode.SYSTEM_ERROR`, so each carries an integer `getCode()`, a String `getStatus()`, and a message containing the generation diagnostics.

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

## Boundaries

This Starter only generates Snowflake IDs via the static `SnowflakeIdGenerator` entries. It does not provide UUIDv7, automatic node discovery, Redis leases, database segments, batch prefetch, persistent watermarks, or network coordination.

## Validation

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl egon-cola-component-common/egon-cola-component-common-id-starter -am clean test
```
