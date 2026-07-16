# Egon COLA Dynamic Thread Pool Component

[English](README.md) | [中文](README.zh-CN.md)

## Overview

`egon-cola-component-dynamic-thread-pool` is the Egon COLA dynamic thread-pool governance component. It provides Spring Boot business applications with executor registration, runtime snapshot reporting, Redis configuration change listening, dynamic capacity adjustment, virtual-thread concurrency limiting, MDC context propagation, audit events, and Micrometer metric binding.

The component consists of a business-side starter and a standalone admin service. The starter integrates with a business application and manages executors within that application. The admin queries application, instance, and executor snapshots from Redis through REST APIs and publishes capacity adjustment messages. The component directory does not include a UI; an external frontend can discover the module through the manifest endpoint.

## Module Layout

| Module | Description |
|---|---|
| `egon-cola-component-dynamic-thread-pool-starter` | Business application starter responsible for executor discovery, snapshot collection, Redis registration, configuration change listening, MDC propagation, audit events, and Micrometer metrics |
| `egon-cola-component-dynamic-thread-pool-admin` | Standalone Spring Boot Admin service that provides management REST APIs, a manifest, Redis queries, and configuration change publication |
| `egon-cola-component-dynamic-thread-pool-test` | Component sample and integration verification module |

## Features

### Managed Executors

The starter collects the following Beans from the Spring container and adapts them to `ManagedExecutor`:

| Executor Type | Adapter | Supported Capabilities |
|---|---|---|
| `ThreadPoolExecutor` | `ThreadPoolExecutorManagedExecutor` | Snapshots, core/maximum thread count and keepAlive adjustment, queue metrics |
| `ThreadPoolTaskExecutor` | `ThreadPoolTaskExecutorManagedExecutor` | Snapshots, core/maximum thread count and keepAlive adjustment, queue metrics |
| `BoundedVirtualThreadExecutor` | `BoundedVirtualThreadManagedExecutor` | Snapshots, virtual-thread concurrency limit adjustment, running/submitted/failed/rejected metrics |

### Redis Registration and Change Listening

The starter creates a Redisson client named `dynamicThreadRedissonClient` and writes runtime data through `RedisRegistry`:

| Key / Topic | Description |
|---|---|
| `DTP:APPS` | Registered application set |
| `DTP:APP:{appName}:INSTANCES` | Application instance set |
| `DTP:SNAPSHOT:{appName}:{instanceId}:{executorName}` | Executor snapshot |
| `DTP:CHANGE_TOPIC:{appName}` | Topic used by the admin to publish capacity adjustment messages |
| `DTP:EVENT:{appName}:{yyyyMMdd}` | Adjustment audit events |

### Snapshot Reporting and Metrics

`ThreadPoolDataReportJob` reports snapshots at the interval configured by `egon.cola.component.dtp.report.interval`. If a `MeterRegistry` exists in the business application, `DtpMeterBinder` binds executor metrics to Micrometer.

### MDC Context Propagation

`DtpRunnable`, `DtpCallable`, `DtpSupplier`, `DtpContextAwareExecutorService`, and `DtpThreads` capture the MDC when a task is submitted and restore it while the task runs, preventing asynchronous threads from losing `traceId` / `requestId`.

### Admin REST API

The Admin API base path is `/api/v1/dtp`:

| API | Description |
|---|---|
| `GET /api/v1/dtp/manifest` | Manifest for management UI discovery |
| `GET /api/v1/dtp/apps` | Query applications |
| `GET /api/v1/dtp/apps/{appName}/instances` | Query application instances |
| `GET /api/v1/dtp/apps/{appName}/instances/{instanceId}/executors` | Query all executor snapshots for an instance |
| `GET /api/v1/dtp/apps/{appName}/instances/{instanceId}/executors/{executorName}` | Query a single executor snapshot |
| `POST /api/v1/dtp/apps/{appName}/instances/{instanceId}/executors/{executorName}/resize` | Adjust the capacity of a platform thread pool or Spring thread pool |
| `POST /api/v1/dtp/apps/{appName}/instances/{instanceId}/executors/{executorName}/virtual-limit` | Adjust the concurrency limit of a bounded virtual-thread executor |
| `GET /api/v1/dtp/events?appName={appName}&date={yyyyMMdd}` | Query audit events |

## Dependency Setup

Include the starter in a business application:

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
        <artifactId>egon-cola-component-dynamic-thread-pool-starter</artifactId>
    </dependency>
</dependencies>
```

Build and deploy the Admin service as a standalone application within the component:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin -am package -DskipTests
```

## Configuration

Business application configuration:

```yaml
spring:
  application:
    name: order-service

server:
  port: 8081

egon:
  cola:
    component:
      dtp:
        enabled: true
        app-name: ${spring.application.name}
        instance-id: ${spring.application.name}-${server.port}
        registry:
          type: redis
          redis:
            host: 127.0.0.1
            port: 6379
            password:
            database: 0
            pool-size: 64
            min-idle-size: 10
            idle-timeout: 10000
            connect-timeout: 10000
            retry-attempts: 3
            retry-interval: 1000
            ping-interval: 0
            keep-alive: true
        report:
          enabled: true
          interval: 20s
        trace:
          enabled: true
          mdc-enabled: true
          trace-id-key: traceId
          request-id-key: requestId
        virtual:
          enabled: true
          default-concurrency-limit: 500
```

The admin activates the `dev` profile by default. Redis points to the local machine in `application-dev.yml`:

```yaml
server:
  port: 8089

egon:
  cola:
    component:
      dtp:
        registry:
          redis:
            host: 127.0.0.1
            port: 6379
            password: ${EGON_COLA_DTP_REDIS_PASSWORD:}
```

## Complete Usage Example

### 1. Declare Executors to Govern in the Business Application

The starter automatically discovers executor Beans in the container. The Bean name becomes the `executorName` used by the Admin API.

```java
package demo.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import top.egon.cola.component.dtp.executor.virtual.BoundedVirtualThreadExecutor;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class OrderExecutorConfig {

    @Bean("orderPlatformExecutor")
    public ThreadPoolExecutor orderPlatformExecutor() {
        return new ThreadPoolExecutor(
                8,
                32,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean("orderTaskExecutor")
    public ThreadPoolTaskExecutor orderTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("order-task-");
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(1000);
        executor.initialize();
        return executor;
    }

    @Bean("orderVirtualExecutor")
    public BoundedVirtualThreadExecutor orderVirtualExecutor() {
        return new BoundedVirtualThreadExecutor("order-virtual", 200);
    }
}
```

### 2. Propagate MDC When Submitting Tasks

```java
package demo.order;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import top.egon.cola.component.dtp.context.DtpContextAwareExecutorService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class OrderAsyncService {

    private final ExecutorService executor;

    public OrderAsyncService(ThreadPoolExecutor orderPlatformExecutor) {
        this.executor = new DtpContextAwareExecutorService(orderPlatformExecutor);
    }

    public void submitOrderTask(String orderId) {
        MDC.put("traceId", "trace-" + orderId);
        executor.submit(() -> {
            String traceId = MDC.get("traceId");
            // Logs, downstream calls, and business logic continue with the submitting thread's traceId.
            process(orderId, traceId);
        });
    }

    private void process(String orderId, String traceId) {
        // business logic
    }
}
```

### 3. Query Snapshots and Adjust Thread Pools

Query applications and executors:

```bash
curl http://localhost:8089/api/v1/dtp/apps
curl http://localhost:8089/api/v1/dtp/apps/order-service/instances
curl http://localhost:8089/api/v1/dtp/apps/order-service/instances/order-service-8081/executors
```

Adjust the capacity of a platform thread pool or Spring thread pool:

```bash
curl -X POST \
  http://localhost:8089/api/v1/dtp/apps/order-service/instances/order-service-8081/executors/orderPlatformExecutor/resize \
  -H 'Content-Type: application/json' \
  -d '{
    "executorKind": "PLATFORM_THREAD_POOL",
    "corePoolSize": 12,
    "maximumPoolSize": 48,
    "keepAliveSeconds": 90,
    "allowCoreThreadTimeOut": true,
    "operator": "ops"
  }'
```

Adjust the concurrency limit of a bounded virtual-thread executor:

```bash
curl -X POST \
  http://localhost:8089/api/v1/dtp/apps/order-service/instances/order-service-8081/executors/orderVirtualExecutor/virtual-limit \
  -H 'Content-Type: application/json' \
  -d '{
    "concurrencyLimit": 300,
    "operator": "ops"
  }'
```

Query audit events:

```bash
curl 'http://localhost:8089/api/v1/dtp/events?appName=order-service&date=20260709'
```

## Design Principles and Implementation Details

### Design Principles

1. Business applications interact only with the starter and do not expose management endpoints directly.
2. The admin does not connect directly to business applications. All state reads and change notifications pass through Redis, reducing runtime coupling.
3. `ManagedExecutor` adapts executor capabilities so platform thread pools, Spring thread pools, and virtual-thread executors share one snapshot and update model.
4. Adjustment commands include `appName`, `instanceId`, `executorName`, and `executorKind`. The starter validates the identity before updating, preventing a command from being applied to the wrong instance.
5. MDC propagation is implemented as wrappers and does not require business code to replace its thread-pool usage model.

### Implementation Details

- `DynamicThreadPoolAutoConfig` is registered through `AutoConfiguration.imports`. Its configuration prefix is `egon.cola.component.dtp`, and `enabled` defaults to `true`.
- `resolveAppName` first uses `egon.cola.component.dtp.app-name`, then `spring.application.name`, and finally falls back to `default-app`.
- `resolveInstanceId` first uses the explicit configuration, then `{appName}-{server.port}`, and finally the JVM runtime name.
- `ManagedExecutorRegistry` stores all managed executors, and `DynamicThreadPoolService` queries snapshots and performs updates.
- `ThreadPoolConfigAdjustListener` subscribes to `DTP:CHANGE_TOPIC:{appName}`. After receiving a `DtpConfigChangeMessage`, it calls `IDynamicThreadPoolService.updateExecutor`.
- `BoundedVirtualThreadExecutor` uses `Semaphore` to control its concurrency limit, includes submitted/running/completed/failed/rejected metrics, and supports runtime permit adjustment through `updateConcurrencyLimit`.
- `ThreadPoolDataReportJob` periodically writes snapshot, apps, instances, and audit data. Reporting can be disabled with `egon.cola.component.dtp.report.enabled=false`.
- Admin `/resize` accepts only `PLATFORM_THREAD_POOL` and `SPRING_THREAD_POOL_TASK_EXECUTOR`; `/virtual-limit` publishes only `VIRTUAL_THREAD_PER_TASK` update commands.

## Boundaries and Operational Notes

- The component directory does not include a UI. Integrate a UI through `/api/v1/dtp/manifest` and the REST APIs in an external system.
- By default, the starter creates its own Redisson client named `dynamicThreadRedissonClient`; the business application must ensure Redis is available.
- Executor Bean names are governance identifiers. Renaming a Bean changes the path used by the admin for adjustments.
- Virtual-thread executors support only concurrency-limit adjustments, not platform thread-pool parameters.
- Adjustments to platform thread pools or Spring thread pools validate `corePoolSize <= maximumPoolSize`.

## Validation Commands

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test -am -Dsurefire.failIfNoSpecifiedTests=false test
```
