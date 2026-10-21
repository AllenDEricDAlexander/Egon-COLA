# Egon COLA Dynamic Thread Pool Component

[English](README.md) | [中文](README.zh-CN.md)

## Overview

`egon-cola-component-dynamic-thread-pool` is the Egon COLA dynamic thread-pool governance component. It provides Spring Boot business applications with executor registration, runtime snapshot reporting, Redis configuration change listening, dynamic capacity adjustment, virtual-thread concurrency limiting, common-trace integration, audit events, and Micrometer metric binding.

The component consists of a business-side starter and a standalone admin service. The starter integrates with a business application and manages executors within that application. The admin queries application, instance, and executor snapshots from Redis through REST APIs and publishes capacity adjustment messages. The component directory does not include a UI; an external frontend can discover the module through the manifest endpoint.

## Module Layout

| Module | Description |
|---|---|
| `egon-cola-component-dynamic-thread-pool-starter` | Business application starter responsible for executor discovery, snapshot collection, Redis registration, configuration change listening, platform- and virtual-thread Trace propagation, audit events, and Micrometer metrics |
| `egon-cola-component-dynamic-thread-pool-admin` | Standalone Spring Boot Admin service that provides management REST APIs, a manifest, Redis queries, and configuration change publication |
| `egon-cola-component-dynamic-thread-pool-test` | Component sample and integration verification module |

## Contract Conventions

- `ExecutorKind` and `RegistryEnumVO` implement the public `EgonEnum` contract with fixed integer codes (`PLATFORM_THREAD_POOL=0`, `SPRING_THREAD_POOL_TASK_EXECUTOR=1`, `VIRTUAL_THREAD_PER_TASK=2`, `UNKNOWN=3`; registry keys `0..2`). Codes are literal declarations and are never derived from `ordinal()`; `name()` and `RegistryEnumVO.getKey()` keep exactly the Redis wire values they had before.
- `Response.Code` implements `ErrorStatus`. `getCode()` is the integer contract code, while the published legacy String wire codes (`0000`, `0001`, `0002`) are exposed through `getStatus()` and written into the `Response.code` envelope field. `getMessage()` carries the original label.
- The component declares no custom exception type. The starter and admin throw JDK exceptions directly (`IllegalArgumentException` for invalid sizes and unknown executors, `RejectedExecutionException` for a saturated governed virtual-thread executor, `UnsupportedOperationException` for an unsupported capability), while the Admin REST layer answers failures through the `Response` envelope instead of an exception handler hierarchy.
- `RemainingComponentContractTest` in the starter and admin modules pins the tables above; run it with `-Dtest=RemainingComponentContractTest -Dsurefire.failIfNoSpecifiedTests=false`.

## Features

### Managed Executors

The starter collects the following Beans from the Spring container and adapts them to `ManagedExecutor`:

| Executor Type | Adapter | Supported Capabilities |
|---|---|---|
| `ThreadPoolExecutor` | `ThreadPoolExecutorManagedExecutor` | Trace-aware submission, snapshots, core/maximum thread count and keepAlive adjustment, queue metrics |
| `ThreadPoolTaskExecutor` | `ThreadPoolTaskExecutorManagedExecutor` | Trace-aware submission, snapshots, core/maximum thread count and keepAlive adjustment, queue metrics |
| `BoundedVirtualThreadExecutor` | `BoundedVirtualThreadManagedExecutor` | Trace-aware submission, snapshots, virtual-thread concurrency limit adjustment, running/submitted/failed/rejected metrics |

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

### Trace Context Propagation

`common-trace` owns the complete `TraceContext` and the three local-thread templates. DTP keeps its existing executor adapters: `DtpRunnable`, `DtpCallable`, and `DtpSupplier` extend those templates; `DtpContextAwareExecutorService`, `DtpTaskDecorator`, and `DtpThreads` apply them at the relevant submission boundary. `ManagedExecutor` is itself an `ExecutorService`, and all three governance adapters are context-aware submission boundaries. Platform, Spring platform, and virtual threads therefore share the same Trace and MDC capture, restoration, and cleanup semantics.

DTP does not replace or proxy executor Beans defined by the business application. Direct calls to a raw `ThreadPoolExecutor` bypass the adapter; submit through the same-name `ManagedExecutor` obtained from `ManagedExecutorRegistry`, or explicitly use `DtpContextAwareExecutorService`. Direct `ThreadPoolTaskExecutor` usage can still install `DtpTaskDecorator`.

The Admin explicitly uses the common Trace Spring Boot starter for W3C HTTP propagation. Redis change messages carry only `traceparent`, `tracestate`, and `x-egon-request-id`; they never transport a complete MDC snapshot.

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
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common-trace-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

The Trace Spring Boot starter is explicit. The DTP starter depends only on the common Trace core and does not force HTTP filters or Spring Trace auto-configuration into business applications.

### Trace Integration APIs

| Submission Boundary | DTP API |
|---|---|
| DTP-managed platform, Spring, or virtual executor | `ManagedExecutor` from `ManagedExecutorRegistry` |
| `ExecutorService` | `DtpContextAwareExecutorService` |
| Spring `ThreadPoolTaskExecutor` | `DtpTaskDecorator` |
| Direct `Runnable`, `Callable`, or `Supplier` wrapping | `DtpRunnable`, `DtpCallable`, `DtpSupplier` |
| Standalone platform or virtual thread | `DtpThreads` |
| Governed virtual-thread executor | `BoundedVirtualThreadExecutor` |

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
import top.egon.cola.component.dtp.context.DtpTaskDecorator;
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
        executor.setTaskDecorator(new DtpTaskDecorator());
        executor.initialize();
        return executor;
    }

    @Bean("orderVirtualExecutor")
    public BoundedVirtualThreadExecutor orderVirtualExecutor() {
        return new BoundedVirtualThreadExecutor("order-virtual", 200);
    }
}
```

### 2. Propagate Trace Context When Submitting Tasks

```java
package demo.order;

import org.springframework.stereotype.Service;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.dtp.executor.ManagedExecutor;
import top.egon.cola.component.dtp.executor.ManagedExecutorRegistry;

@Service
public class OrderAsyncService {

    private final ManagedExecutor executor;

    public OrderAsyncService(ManagedExecutorRegistry registry) {
        this.executor = registry.get("orderPlatformExecutor")
                .orElseThrow(() -> new IllegalStateException("orderPlatformExecutor is not managed"));
    }

    public void submitOrderTask(String orderId) {
        executor.submit(() -> {
            String traceId = TraceContext.currentOrCreate().traceId();
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
5. `common-trace` owns Trace/MDC snapshots; DTP coordinates declared additional accessors and executor governance at each submission boundary.

### Implementation Details

- `DynamicThreadPoolAutoConfig` is registered through `AutoConfiguration.imports`. Its configuration prefix is `egon.cola.component.dtp`, and `enabled` defaults to `true`.
- `resolveAppName` first uses `egon.cola.component.dtp.app-name`, then `spring.application.name`, and finally falls back to `default-app`.
- `resolveInstanceId` first uses the explicit configuration, then `{appName}-{server.port}`, and finally `{appName}-{JVM runtime name}`.
- `ManagedExecutorRegistry` stores all managed executors; `ManagedExecutor` also provides context-aware task submission, and `DynamicThreadPoolService` queries snapshots and performs updates.
- `ThreadPoolConfigAdjustListener` subscribes to `DTP:CHANGE_TOPIC:{appName}`. After receiving a `DtpConfigChangeMessage`, it calls `IDynamicThreadPoolService.updateExecutor`.
- `BoundedVirtualThreadExecutor` uses `Semaphore` to control its concurrency limit and `DtpContextAwareExecutorService` to capture one `TraceContext` per submission. It includes submitted/running/completed/failed/rejected metrics and supports runtime permit adjustment through `updateConcurrencyLimit`.
- `ThreadPoolDataReportJob` periodically writes the apps set, the instance set, and one snapshot key per executor. Audit events are not part of that job; they are recorded when a configuration change is applied. Reporting can be disabled with `egon.cola.component.dtp.report.enabled=false`.
- Admin `/resize` accepts only `PLATFORM_THREAD_POOL` and `SPRING_THREAD_POOL_TASK_EXECUTOR`; `/virtual-limit` publishes only `VIRTUAL_THREAD_PER_TASK` update commands.

## Boundaries and Operational Notes

- The component directory does not include a UI. Integrate a UI through `/api/v1/dtp/manifest` and the REST APIs in an external system.
- By default, the starter creates its own Redisson client named `dynamicThreadRedissonClient`; the business application must ensure Redis is available.
- Executor Bean names are governance identifiers. Renaming a Bean changes the path used by the admin for adjustments.
- Virtual-thread executors support only concurrency-limit adjustments, not platform thread-pool parameters.
- The Admin rejects a `/resize` request unless `executorKind` is resizable, `corePoolSize` and `maximumPoolSize` are both positive, and `corePoolSize <= maximumPoolSize`; `keepAliveSeconds` must additionally be positive whenever `allowCoreThreadTimeOut` is true. `ThreadPoolResizeSupport` re-checks positivity and the same ordering on the business-application side, then applies the two values in the order that avoids a transient `core > max` state.

## Validation Commands

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test -am -Dsurefire.failIfNoSpecifiedTests=false test
```

## Unified ThreadManage management (compatible extension)

Use the Spring Bean `threadManage` for new pools. Existing executor Beans, `ManagedExecutorRegistry.get/list`, Runnable/Callable/Supplier, all Dtp wrappers, DtpThreads, and TraceRoute templates remain available. The existing registry is the only catalog; initialized Beans are registered without replacing their types or user decorators/rejection handlers.

```java
@Bean("emailExecutor")
public ThreadPoolTaskExecutor emailExecutor(@Qualifier("threadManage") ThreadManage threadManage) {
    return threadManage.newThreadPool(new ThreadPoolCreateCommand()
            .setExecutorName("emailExecutor")
            .setCorePoolSize(4).setMaximumPoolSize(16).setQueueCapacity(1000));
}

ManagedExecutor executor = threadManage.executor("emailExecutor");
executor.execute(() -> sendEmail());
Future<String> message = executor.submit(() -> loadMessage());
```

Import `top.egon.cola.component.dtp.executor.ThreadManage` and `top.egon.cola.component.dtp.domain.model.entity.ThreadPoolCreateCommand`. The factory returns a Spring ThreadPoolTaskExecutor, retaining named injection and @Async compatibility. Use the same Bean and pool name; aliases share the original object. Concurrent identical definitions return one pool; conflicting objects/definitions are rejected. Runtime resizing does not reset the creation definition.

New pools capture MDC at submission and create a root context for ordinary background tasks without a traceId. Explicit legacy wrappers keep their original capture time, including empty context. Execution, failure, nesting and CallerRuns restore the previous MDC. HTTP Trace auto-configuration still requires the explicit Trace Spring Boot Starter; DTP depends only on Trace Core.

### Metrics and observation boundaries

Existing `dtp.executor.*` gauges and Redis snapshots remain. New function counters are `dtp.task.submitted`, `succeeded`, `failed`, `cancelled`, `rejected`; timers are `dtp.task.queue.wait` and `dtp.task.execution`. Gauges `dtp.executor.queue.capacity`, `queue.resizable`, and `observation.complete` describe capacity and supported observation. A rejection event can be followed by successful CallerRuns execution. Cancellation is separate from failure. Existing completedTaskCount retains its original executor-specific meaning.

Tags remain appName, instanceId, executorName, executorKind and virtual. traceId belongs in diagnostic logs, never metric labels. Failure, rejection and completed slow-task diagnostics are rate limited per pool and omit task payloads, full MDC and passwords. Histograms and pressure alarms are disabled by default.

New pools observe execute/submit and controlled invokeAll/invokeAny. Adopted pools gain adapter context/execution observation without modifying their rejection handlers, so silent custom drops cannot be inferred completely; observation.complete is 0. Raw Bean calls or direct getThreadPoolExecutor() access can bypass instrumentation.

Pass the managed executor explicitly to CompletableFuture.runAsync/supplyAsync for MDC propagation. JDK AsynchronousCompletionTask hides the stage result: record submission/timing and mark observation incomplete rather than count its Runnable return as business success. Read errors from the original CompletableFuture; use ThreadManage.runAsync/supplyAsync for controlled roots or ExecutorService.submit for complete terminal counters. Legacy Future.cancel and rejection-policy behavior is preserved.

### Resizing, delivery and lifecycle

The existing resize endpoint accepts optional nullable queueCapacity. New FIFO queues support live capacity changes; expansion wakes producers, and shrinking below the live backlog rejects the whole command without changing thread parameters, dropping tasks or replacing the executor. Zero capacity selects SynchronousQueue only at creation. Fixed existing queues retain thread-size updates and explicitly reject queue resizing. Core/max/keepAlive/core timeout changes are serialized per actual executor and keep Spring metadata in sync. Virtual executors retain concurrency-limit adjustment without interrupting running tasks.

ThreadManage.changeThreadPool returns the local UpdateResult. The Admin Boolean still confirms Redis publication only; use existing snapshots/audits to confirm application. Validate matching envelope/payload identity, filter non-target instances and deduplicate by target plus messageId for 10 minutes / at most 10000 entries. Reject conflicting payloads under one ID. Audit failure never pretends the applied configuration rolled back, and reporting the actual snapshot is attempted independently. No durable delivery, restart deduplication, global ordering or version CAS is implied. Upgrade target starters before sending the new field.

Spring owns factory pools exposed as Beans. ThreadManage closes non-Bean factory pools, while external pools retain their original lifecycle. New pools shut down gracefully and wait at most 30 seconds without forcibly killing tasks. Existing offline sample configuration is retained; the new factory Bean path has isolated Context coverage.

Configuration keys below `egon.cola.component.dtp.observation`: enabled=true, slow-task-threshold=30s, queue-wait-threshold=5s; alarm.enabled=false, queue-usage-threshold=0.8, consecutive-samples=3, cooldown=60s. Pressure checks reuse the reporting interval; zero-buffer queues have no queue-use ratio. Alarms never resize pools automatically. Slow execution is diagnosed after completion, without interrupting tasks.

Admin generates OpenAPI 3 at /v3/api-docs, enabled in dev/test and disabled by default in base/prod. No Swagger UI, database, authentication mechanism or additional management service is introduced.

## Task context switching and unified exceptions

Managed entries capture each ordinary submission, back up the executing thread, install the snapshot and restore its previous state in `finally`. This covers worker reuse, missing values, nesting, CallerRuns and virtual threads. Cancellation never closes another thread's Scope.

Runnable/Callable/Supplier remain available. Use these helpers for observable root stages:

```java
CompletableFuture<Void> sent = threadManage.runAsync("emailExecutor", () -> sendEmail());
CompletableFuture<String> loaded = threadManage.supplyAsync("emailExecutor", () -> loadMessage());
```

The returned object is the actual observed root. Failed context installation also completes that root exceptionally. Real failures are reported even without business `get()`. Cancellation, rejection and caller waiting timeouts remain distinct; the framework does not retry, compensate or return a replacement success.

### Register an additional ThreadLocal once

Trace/full MDC propagation is built in. Adapt an existing holder using `io.micrometer.context.ThreadLocalAccessor` and qualifier `dtpContextAccessor`, for example:

```java
@Bean("businessContextAccessor")
@Qualifier("dtpContextAccessor")
ThreadLocalAccessor<String> businessContextAccessor() {
    return new ThreadLocalAccessor<>() {
        public Object key() { return "business.context"; }
        public String getValue() { return BusinessContext.get(); }
        public void setValue(String value) { BusinessContext.set(value); }
        public void setValue() { BusinessContext.remove(); }
    };
}
```

Each holder needs a distinct stable key. Only explicitly qualified Beans are selected; there is no global ThreadLocal scan. Getters must be side-effect free and return immutable values or defensive copies. Missing values are removed during the task, then previous values restored. Transactions, connections, sessions, servlet objects, credentials and automatic SecurityContext copying are outside this contract.

Legacy Dtp/TraceRoute wrappers retain construction-time Trace/MDC capture; additional holders are captured at managed submission. The no-argument DtpTaskDecorator and static DtpThreads retain Trace-only behavior. Opaque custom legacy wrappers preserve execution behavior and report unknown Trace rather than blame the submitting request.

### CompletableFuture continuations and Spring Async

An ordinary executor captures at its actual execute call, which may occur on another completion thread. Bind once in the intended request/task scope for continuations:

```java
Executor flow = threadManage.contextualExecutor("emailExecutor");
CompletableFuture<String> converted = upstream.thenApplyAsync(this::convert, flow);
```

Pass the view to subsequent Async methods and keep it request/chain local. Each dispatch has a separate task token. Non-Async stages follow JDK rules. Binding supplies context, not hidden stage results: root helpers observe their roots; retain and handle the final business stage normally.

Boot's default builder receives the decorator when no user TaskDecorator exists. Existing decorators remain intact; compose once with Spring CompositeTaskDecorator when needed. Six maintained source configurations optionally delegate their existing AsyncConfigurer to `dtpAsyncUncaughtExceptionHandler`. Existing generated/custom configurations need that one-time delegation. No second user configurer is installed; executor selection, including Boot force mode, stays intact.

The default `dtpTaskExceptionHandler` reuses Common ExceptionLogger levels, classification and redacted stacks. Supply a Bean with that exact name to replace it. Handler notifications and real counters are independent of default log limiting; system, business and slow diagnostics have separate budgets. Keep handlers quick/thread safe and avoid synchronous resubmission to the failing pool. Component-owned factories compose uncaught handling while preserving original factory attributes and handlers.

Full terminal semantics belong to controlled pools/entries. A foreign Spring Future reachable only through TaskDecorator is cancelled if installation fails before business execution, because its exception setter is inaccessible; a technical diagnostic is recorded. Owned virtual futures are completed with the original technical cause and always release their permit. Arbitrary hidden third-party stages remain outside complete observation.

### Settings and technical metrics

```yaml
egon:
  cola:
    component:
      dtp:
        context-propagation:
          custom-thread-locals-enabled: true
        exception-handling:
          enabled: true
          log-interval: 1s
          uncaught-enabled: true
          async-void-enabled: true
```

Disabling extra holders retains Trace/MDC. Observation statistics and exception reporting are independent. Disabling void reports still avoids counting an observed void failure as success.

Additional counters: `dtp.context.propagation.failures` (fixed CAPTURE/BACKUP/INSTALL/RESTORE phases), `dtp.task.exception.reporter.failures`, `dtp.executor.uncaught`. Existing pool dimensions are retained; Trace/user/accessor values never become metric tags. Unknown-pool diagnostics invent no pool counters. Existing queue/thread/virtual-limit resizing and Redis contracts remain unchanged.

The starter directly declares the existing BOM-managed `io.micrometer:context-propagation` library; metrics remain optional. Tests/builds do not start applications or external infrastructure.
