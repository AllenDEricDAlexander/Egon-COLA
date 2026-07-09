# Egon COLA Dynamic Thread Pool Component

## 简要介绍

`egon-cola-component-dynamic-thread-pool` 是 Egon COLA 的动态线程池治理组件，面向 Spring Boot 业务应用提供线程池注册、运行时快照上报、Redis 配置变更监听、动态容量调整、虚拟线程并发限制、MDC 上下文传播、审计事件和 Micrometer 指标绑定能力。

组件由业务侧 starter 和独立 admin 服务组成。starter 接入业务应用并托管应用内的执行器，admin 通过 REST API 查询 Redis 中的应用/实例/执行器快照，并发布容量调整消息。组件目录不包含 UI，前端可通过 manifest 接口做外部模块发现。

## 模块结构

| Module | 说明 |
|---|---|
| `egon-cola-component-dynamic-thread-pool-starter` | 业务应用 starter，负责执行器发现、快照采集、Redis 注册、配置变更监听、MDC 传播、审计事件、Micrometer 指标 |
| `egon-cola-component-dynamic-thread-pool-admin` | 独立 Spring Boot Admin 服务，提供管理 REST API、manifest、Redis 查询和配置变更发布 |
| `egon-cola-component-dynamic-thread-pool-test` | 组件样例和集成验证模块 |

## 功能说明

### 执行器托管

starter 会收集 Spring 容器中的以下 Bean，并统一适配成 `ManagedExecutor`：

| 执行器类型 | 适配器 | 支持能力 |
|---|---|---|
| `ThreadPoolExecutor` | `ThreadPoolExecutorManagedExecutor` | 快照、核心线程数/最大线程数/keepAlive 调整、队列指标 |
| `ThreadPoolTaskExecutor` | `ThreadPoolTaskExecutorManagedExecutor` | 快照、核心线程数/最大线程数/keepAlive 调整、队列指标 |
| `BoundedVirtualThreadExecutor` | `BoundedVirtualThreadManagedExecutor` | 快照、虚拟线程并发上限调整、运行/提交/失败/拒绝指标 |

### Redis 注册和变更监听

starter 会创建名为 `dynamicThreadRedissonClient` 的 Redisson 客户端，并通过 `RedisRegistry` 写入运行时数据：

| Key / Topic | 说明 |
|---|---|
| `DTP:APPS` | 已注册应用集合 |
| `DTP:APP:{appName}:INSTANCES` | 应用实例集合 |
| `DTP:SNAPSHOT:{appName}:{instanceId}:{executorName}` | 执行器快照 |
| `DTP:CHANGE_TOPIC:{appName}` | admin 发布容量调整消息的 Topic |
| `DTP:EVENT:{appName}:{yyyyMMdd}` | 调整审计事件 |

### 快照上报和指标

`ThreadPoolDataReportJob` 按 `egon.cola.component.dtp.report.interval` 周期上报快照。若业务应用存在 `MeterRegistry`，`DtpMeterBinder` 会绑定执行器指标到 Micrometer。

### MDC 上下文传播

`DtpRunnable`、`DtpCallable`、`DtpSupplier`、`DtpContextAwareExecutorService` 和 `DtpThreads` 会捕获提交任务时的 MDC，并在任务执行时恢复，避免异步线程丢失 `traceId` / `requestId`。

### Admin REST API

Admin API 基础路径为 `/api/v1/dtp`：

| API | 说明 |
|---|---|
| `GET /api/v1/dtp/manifest` | 管理 UI 发现用 manifest |
| `GET /api/v1/dtp/apps` | 查询应用列表 |
| `GET /api/v1/dtp/apps/{appName}/instances` | 查询应用实例 |
| `GET /api/v1/dtp/apps/{appName}/instances/{instanceId}/executors` | 查询实例下所有执行器快照 |
| `GET /api/v1/dtp/apps/{appName}/instances/{instanceId}/executors/{executorName}` | 查询单个执行器快照 |
| `POST /api/v1/dtp/apps/{appName}/instances/{instanceId}/executors/{executorName}/resize` | 调整平台线程池或 Spring 线程池容量 |
| `POST /api/v1/dtp/apps/{appName}/instances/{instanceId}/executors/{executorName}/virtual-limit` | 调整有界虚拟线程执行器并发上限 |
| `GET /api/v1/dtp/events?appName={appName}&date={yyyyMMdd}` | 查询审计事件 |

## 依赖方式

业务应用引入 starter：

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

Admin 服务作为组件内独立应用构建和部署：

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin -am package -DskipTests
```

## 配置说明

业务应用配置：

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

Admin 默认激活 `dev` profile，`application-dev.yml` 中 Redis 默认指向本机：

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

## 完整的使用示例

### 1. 在业务应用中声明需要治理的执行器

starter 会自动发现容器中的执行器 Bean。Bean 名就是 admin API 中使用的 `executorName`。

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

### 2. 提交任务时传播 MDC

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
            // 这里可以继续写日志、调用下游或执行业务逻辑，traceId 与提交线程一致。
            process(orderId, traceId);
        });
    }

    private void process(String orderId, String traceId) {
        // business logic
    }
}
```

### 3. 查询快照并调整线程池

查询应用和执行器：

```bash
curl http://localhost:8089/api/v1/dtp/apps
curl http://localhost:8089/api/v1/dtp/apps/order-service/instances
curl http://localhost:8089/api/v1/dtp/apps/order-service/instances/order-service-8081/executors
```

调整平台线程池或 Spring 线程池容量：

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

调整有界虚拟线程并发上限：

```bash
curl -X POST \
  http://localhost:8089/api/v1/dtp/apps/order-service/instances/order-service-8081/executors/orderVirtualExecutor/virtual-limit \
  -H 'Content-Type: application/json' \
  -d '{
    "concurrencyLimit": 300,
    "operator": "ops"
  }'
```

查询审计事件：

```bash
curl 'http://localhost:8089/api/v1/dtp/events?appName=order-service&date=20260709'
```

## 设计思想和实现细节

### 设计思想

1. 业务应用只感知 starter，不直接暴露管理接口。
2. Admin 不直连业务应用，所有状态读取和变更通知都通过 Redis 完成，降低运行时耦合。
3. 执行器能力通过 `ManagedExecutor` 适配，平台线程池、Spring 线程池、虚拟线程执行器共享一套快照和更新模型。
4. 调整命令携带 `appName`、`instanceId`、`executorName`、`executorKind`，starter 端先校验身份再更新，避免错误实例消费错误命令。
5. MDC 传播独立为包装器，不强制改写业务线程池使用方式。

### 实现细节

- `DynamicThreadPoolAutoConfig` 通过 `AutoConfiguration.imports` 注册，配置前缀为 `egon.cola.component.dtp`，`enabled` 缺省为 `true`。
- `resolveAppName` 优先使用 `egon.cola.component.dtp.app-name`，其次使用 `spring.application.name`，最后降级为 `default-app`。
- `resolveInstanceId` 优先使用显式配置，其次使用 `{appName}-{server.port}`，最后使用 JVM runtime name。
- `ManagedExecutorRegistry` 保存所有托管执行器，`DynamicThreadPoolService` 负责查询快照和执行更新。
- `ThreadPoolConfigAdjustListener` 订阅 `DTP:CHANGE_TOPIC:{appName}`，收到 `DtpConfigChangeMessage` 后调用 `IDynamicThreadPoolService.updateExecutor`。
- `BoundedVirtualThreadExecutor` 使用 `Semaphore` 控制并发上限，内置 submitted/running/completed/failed/rejected 指标，`updateConcurrencyLimit` 可以运行时调整许可数量。
- `ThreadPoolDataReportJob` 周期性写入 snapshot、apps、instances 和审计数据；report 可以通过 `egon.cola.component.dtp.report.enabled=false` 关闭。
- Admin 的 `/resize` 只允许 `PLATFORM_THREAD_POOL` 和 `SPRING_THREAD_POOL_TASK_EXECUTOR`，`/virtual-limit` 只发布 `VIRTUAL_THREAD_PER_TASK` 更新命令。

## 边界和注意事项

- 组件目录不包含 UI，UI 应通过 `/api/v1/dtp/manifest` 和 REST API 在外部系统中集成。
- starter 默认创建自己的 Redisson 客户端 `dynamicThreadRedissonClient`，业务应用需要保证 Redis 可用。
- 执行器 Bean 名是治理标识，重命名 Bean 会影响 admin 调整路径。
- 对虚拟线程执行器只能调整并发上限，不能调整平台线程池参数。
- 对平台线程池或 Spring 线程池的调整会校验 `corePoolSize <= maximumPoolSize`。

## 验证命令

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test -am -Dsurefire.failIfNoSpecifiedTests=false test
```
