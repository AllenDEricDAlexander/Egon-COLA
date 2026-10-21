# Egon COLA Dynamic Thread Pool Component

[English](README.md) | 中文

## 简要介绍

`egon-cola-component-dynamic-thread-pool` 是 Egon COLA 的动态线程池治理组件，面向 Spring Boot 业务应用提供线程池注册、运行时快照上报、Redis 配置变更监听、动态容量调整、虚拟线程并发限制、common-trace 集成、审计事件和 Micrometer 指标绑定能力。

组件由业务侧 starter 和独立 admin 服务组成。starter 接入业务应用并托管应用内的执行器，admin 通过 REST API 查询 Redis 中的应用/实例/执行器快照，并发布容量调整消息。组件目录不包含 UI，前端可通过 manifest 接口做外部模块发现。

## 模块结构

| Module | 说明 |
|---|---|
| `egon-cola-component-dynamic-thread-pool-starter` | 业务应用 starter，负责执行器发现、快照采集、Redis 注册、配置变更监听、平台线程与虚拟线程 Trace 传播、审计事件、Micrometer 指标 |
| `egon-cola-component-dynamic-thread-pool-admin` | 独立 Spring Boot Admin 服务，提供管理 REST API、manifest、Redis 查询和配置变更发布 |
| `egon-cola-component-dynamic-thread-pool-test` | 组件样例和集成验证模块 |

## 契约约定

- `ExecutorKind` 与 `RegistryEnumVO` 实现公共 `EgonEnum` 契约，使用固定整型编码（`PLATFORM_THREAD_POOL=0`、`SPRING_THREAD_POOL_TASK_EXECUTOR=1`、`VIRTUAL_THREAD_PER_TASK=2`、`UNKNOWN=3`；registry key 为 `0..2`）。编码是字面声明，绝不由 `ordinal()` 推导；`name()` 与 `RegistryEnumVO.getKey()` 保持与变更前完全一致的 Redis 线值。
- `Response.Code` 实现 `ErrorStatus`。`getCode()` 是契约整型编码，对外的历史 String 线值（`0000`、`0001`、`0002`）由 `getStatus()` 暴露并写入 `Response.code` 信封字段，`getMessage()` 携带原始标签。
- 组件不声明自定义异常类型。starter 和 admin 直接抛出 JDK 异常（非法参数或未知执行器用 `IllegalArgumentException`，受限虚拟线程执行器饱和时用 `RejectedExecutionException`，不支持的能力用 `UnsupportedOperationException`），Admin REST 层则通过 `Response` 信封返回失败，而不是依赖异常继承体系。
- starter 与 admin 模块中的 `RemainingComponentContractTest` 钉住了上述约定，可用 `-Dtest=RemainingComponentContractTest -Dsurefire.failIfNoSpecifiedTests=false` 运行。

## 功能说明

### 执行器托管

starter 会收集 Spring 容器中的以下 Bean，并统一适配成 `ManagedExecutor`：

| 执行器类型 | 适配器 | 支持能力 |
|---|---|---|
| `ThreadPoolExecutor` | `ThreadPoolExecutorManagedExecutor` | Trace 上下文提交、快照、核心线程数/最大线程数/keepAlive 调整、队列指标 |
| `ThreadPoolTaskExecutor` | `ThreadPoolTaskExecutorManagedExecutor` | Trace 上下文提交、快照、核心线程数/最大线程数/keepAlive 调整、队列指标 |
| `BoundedVirtualThreadExecutor` | `BoundedVirtualThreadManagedExecutor` | Trace 上下文提交、快照、虚拟线程并发上限调整、运行/提交/失败/拒绝指标 |

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

### Trace 上下文传播

`common-trace` 负责完整 `TraceContext` 和三个本地线程模板。DTP 保留现有执行器适配：`DtpRunnable`、`DtpCallable`、`DtpSupplier` 继承这三个模板；`DtpContextAwareExecutorService`、`DtpTaskDecorator`、`DtpThreads` 在对应提交边界应用它们。`ManagedExecutor` 本身是 `ExecutorService`，三个治理 adapter 都是上下文感知的任务提交入口；普通线程、Spring 平台线程和虚拟线程执行相同的 Trace 与 MDC 捕获、恢复和清理语义。

DTP 不替换或代理业务定义的原始执行器 Bean。直接调用原始 `ThreadPoolExecutor` 不会经过 adapter；业务应从 `ManagedExecutorRegistry` 获取同名 `ManagedExecutor` 提交任务，或者显式使用 `DtpContextAwareExecutorService`。直接使用 `ThreadPoolTaskExecutor` 时仍可配置 `DtpTaskDecorator`。

Admin 显式使用 common Trace Spring Boot Starter 处理 W3C HTTP 传播。Redis 变更消息只携带 `traceparent`、`tracestate` 和 `x-egon-request-id`，不会传输完整 MDC 快照。

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
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common-trace-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

Trace Spring Boot Starter 必须显式引入。DTP Starter 只依赖 common Trace Core，不会向业务应用强制传递 HTTP Filter 或 Spring Trace 自动配置。

### Trace 集成 API

| 提交边界 | DTP API |
|---|---|
| DTP 托管的平台、Spring 或虚拟线程执行器 | `ManagedExecutorRegistry` 中的 `ManagedExecutor` |
| `ExecutorService` | `DtpContextAwareExecutorService` |
| Spring `ThreadPoolTaskExecutor` | `DtpTaskDecorator` |
| 直接包装 `Runnable`、`Callable` 或 `Supplier` | `DtpRunnable`、`DtpCallable`、`DtpSupplier` |
| 独立平台线程或虚拟线程 | `DtpThreads` |
| 受治理的虚拟线程执行器 | `BoundedVirtualThreadExecutor` |

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

### 2. 提交任务时传播 Trace 上下文

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
5. `common-trace` 负责上下文捕获与恢复；DTP 负责执行器治理，以及在各提交边界应用上下文的适配器。

### 实现细节

- `DynamicThreadPoolAutoConfig` 通过 `AutoConfiguration.imports` 注册，配置前缀为 `egon.cola.component.dtp`，`enabled` 缺省为 `true`。
- `resolveAppName` 优先使用 `egon.cola.component.dtp.app-name`，其次使用 `spring.application.name`，最后降级为 `default-app`。
- `resolveInstanceId` 优先使用显式配置，其次使用 `{appName}-{server.port}`，最后使用 `{appName}-{JVM runtime name}`。
- `ManagedExecutorRegistry` 保存所有托管执行器；`ManagedExecutor` 同时负责上下文感知任务提交，`DynamicThreadPoolService` 负责查询快照和执行更新。
- `ThreadPoolConfigAdjustListener` 订阅 `DTP:CHANGE_TOPIC:{appName}`，收到 `DtpConfigChangeMessage` 后调用 `IDynamicThreadPoolService.updateExecutor`。
- `BoundedVirtualThreadExecutor` 使用 `Semaphore` 控制并发上限，并通过 `DtpContextAwareExecutorService` 在每次提交时捕获一个 `TraceContext`。它内置 submitted/running/completed/failed/rejected 指标，`updateConcurrencyLimit` 可以运行时调整许可数量。
- `ThreadPoolDataReportJob` 周期性写入 apps 集合、instances 集合和每个执行器的 snapshot；审计事件不在该任务内写入，而是在配置变更被执行时记录。report 可以通过 `egon.cola.component.dtp.report.enabled=false` 关闭。
- Admin 的 `/resize` 只允许 `PLATFORM_THREAD_POOL` 和 `SPRING_THREAD_POOL_TASK_EXECUTOR`，`/virtual-limit` 只发布 `VIRTUAL_THREAD_PER_TASK` 更新命令。

## 边界和注意事项

- 组件目录不包含 UI，UI 应通过 `/api/v1/dtp/manifest` 和 REST API 在外部系统中集成。
- starter 默认创建自己的 Redisson 客户端 `dynamicThreadRedissonClient`，业务应用需要保证 Redis 可用。
- 执行器 Bean 名是治理标识，重命名 Bean 会影响 admin 调整路径。
- 对虚拟线程执行器只能调整并发上限，不能调整平台线程池参数。
- Admin 在 `/resize` 上要求：`executorKind` 可调整、`corePoolSize` 与 `maximumPoolSize` 均为正数且 `corePoolSize <= maximumPoolSize`；当 `allowCoreThreadTimeOut` 为 true 时 `keepAliveSeconds` 还必须为正数。业务应用侧的 `ThreadPoolResizeSupport` 会再次校验正数和同一大小关系，并按不会产生中间态 `core > max` 的顺序写入两个值。

## 验证命令

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test -am -Dsurefire.failIfNoSpecifiedTests=false test
```

## ThreadManage 统一管理（兼容增强）

推荐为新池使用 `ThreadManage`；已有原始 Bean、`ManagedExecutorRegistry.get/list`、Runnable/Callable/Supplier、Dtp 包装、DtpThreads 和 TraceRoute 模板均保留。管理器复用同一注册表，不建立静态全局池。初始化后发现原始 Bean，不替换类型或覆盖用户 TaskDecorator/拒绝策略。

```java
@Bean("emailExecutor")
public ThreadPoolTaskExecutor emailExecutor(@Qualifier("threadManage") ThreadManage threadManage) {
    return threadManage.newThreadPool(new ThreadPoolCreateCommand()
            .setExecutorName("emailExecutor")
            .setCorePoolSize(4)
            .setMaximumPoolSize(16)
            .setQueueCapacity(1000));
}
```

类型分别为 `top.egon.cola.component.dtp.executor.ThreadManage`、`top.egon.cola.component.dtp.domain.model.entity.ThreadPoolCreateCommand`；返回值仍是 Spring `ThreadPoolTaskExecutor`，支持按原 Bean 名注入和 `@Async("emailExecutor")`。Bean 名与命令名称应一致；别名指向同一原对象。相同名称/创建定义复用原池，不同对象或定义冲突则拒绝，不会覆盖已有任务；动态调参不会重置创建定义。

```java
ManagedExecutor executor = threadManage.executor("emailExecutor");
executor.execute(() -> sendEmail());
Future<String> result = executor.submit(() -> loadMessage());
ExecutorSnapshot snapshot = executor.snapshot();
```

原生 `Runnable`/`Callable` 可以直接提交。新池在每次提交时捕获 MDC，有 traceId 时继续传递，没有时为普通后台任务建立根上下文。旧 `DtpRunnable.wrap`、`DtpCallable.wrap`、`DtpSupplier.wrap` 仍在包装时捕获，包括显式空上下文；退出时恢复执行线程原来的完整 MDC，CallerRuns 也遵守相同规则。HTTP Trace Filter 仍需业务显式引入 Trace Spring Boot Starter，DTP 只传递 Trace Core。

### 任务监控与可观测边界

保留原 `dtp.executor.*` Gauge 和 Redis snapshot，新增下列指标：

| 指标 | 含义 |
| --- | --- |
| `dtp.task.submitted` | 已进入观察边界的提交次数，包含拒绝尝试 |
| `dtp.task.succeeded` / `failed` / `cancelled` | 被真实委托或受控 Future 确认的结果；取消不算失败 |
| `dtp.task.rejected` | 已观测的拒绝处理事件；CallerRuns 后仍可能成功 |
| `dtp.task.queue.wait` / `execution` | 排队与执行耗时 Timer；默认不开直方图 |
| `dtp.executor.queue.capacity` / `queue.resizable` | 实际队列容量与是否支持动态改容 |
| `dtp.executor.observation.complete` | 完整观测能力标识；0 表示禁用或存在无法完整观察的路径 |

任务指标使用 appName、instanceId、executorName、executorKind、virtual；traceId 不作为标签。异常/拒绝/慢任务日志带池身份与 Trace，按池限频，不输出任务对象、参数、完整 MDC 或密码。正常成功不逐条打 INFO。原 completedTaskCount 保持原有执行器口径，不能与新业务终态 Counter 混为一谈。

完整观察适用于新池的 execute/submit、受控 invokeAll/invokeAny。已有外接池通过 adapter 获得上下文与执行观察，但不修改其拒绝处理器；自定义静默丢弃无法完整推断，能力标识为0。直接调用未增强的原 Bean 或从 getThreadPoolExecutor() 取得底层对象会绕过相应提交边界。

`CompletableFuture.runAsync/supplyAsync` 必须显式传入治理 executor 才传播 MDC。JDK 内部 AsynchronousCompletionTask 不公开 stage 结果，因此仅记录提交/耗时并将完整能力标识置0，不把其 Runnable 返回当作业务成功；stage 异常仍通过原 CompletableFuture 读取。需要完整终态统计时使用 `ThreadManage.runAsync/supplyAsync` 受控根阶段或 `ExecutorService.submit`。这项增强不改变异步 stage、Future.cancel 或原拒绝策略的行为。

### 动态调整与生命周期

原 resize 接口增加可选 `queueCapacity`；缺省或 null 表示不改队列。core/max、keepAliveSeconds、allowCoreThreadTimeOut 与虚拟 concurrencyLimit 继续支持。参数和目标能力全部验证后原地更新，按实际执行器串行，Spring getter 与底层线程参数保持一致。

新池使用可调容量 FIFO 队列，扩容唤醒阻塞生产者；缩容低于应用时积压量时整次拒绝，不清理、不丢任务、不重建执行器。capacity=0 创建 SynchronousQueue，运行中不切换队列类型。原固定队列仍能调线程数，改容量会明确失败。虚拟线程只调并发上限，缩容不打断正在执行的任务。

`ThreadManage.changeThreadPool(ExecutorUpdateCommand)` 返回当前进程真实 UpdateResult；Admin 原 Boolean 仍只表示发布成功，通过原快照/审计确认生效。Redis 命令先过滤目标、校验 envelope/payload，再以目标身份和 messageId 去重（10分钟/最多10000条）；同 ID 不同内容拒绝。审计写失败不回滚已生效配置，并继续尝试报告实际快照。不保证持久投递、跨重启去重、全局排序或 version CAS；重启仍以创建配置为基线。

工厂池转为 Spring Bean 后交由 Spring 关闭；非 Bean 工厂池由 ThreadManage 关闭，外部池沿用原生命周期。新池优雅 shutdown，最多等待30秒后记录未终止状态，不自动强杀任务。先升级目标 starter，再在 Admin 使用新队列字段。组件样例保留原离线注册配置；上述新工厂 Bean 路径由无 Redis 的 Context 回归测试覆盖。

```yaml
egon:
  cola:
    component:
      dtp:
        observation:
          enabled: true
          slow-task-threshold: 30s
          queue-wait-threshold: 5s
          alarm:
            enabled: false
            queue-usage-threshold: 0.8
            consecutive-samples: 3
            cooldown: 60s
```

告警复用 report.interval：启用后队列使用率达到阈值并连续采样满足次数，或出现拒绝增量时记录压力告警；SynchronousQueue 不计算队列占比。告警不自动改线程数，不强制中断慢任务；慢任务日志在执行结束时诊断。Admin 使用 OpenAPI 3，文档路径 `/v3/api-docs`；dev/test 可启用，base/prod 默认关闭，不增加 UI 或认证机制。

## 线程切换上下文与统一异常处理

托管入口对普通任务每次提交捕获快照，执行前备份当前线程状态，安装任务上下文，在 `finally` 恢复。覆盖线程复用、空值、嵌套、CallerRuns 和虚拟线程；取消线程不会代为关闭实际执行线程的 Scope。

原 Runnable/Callable/Supplier 继续使用。需要真实 root CompletableFuture 观察时：

```java
CompletableFuture<Void> sent = threadManage.runAsync("emailExecutor", () -> sendEmail());
CompletableFuture<String> loaded = threadManage.supplyAsync("emailExecutor", () -> loadMessage());
```

返回被观察的原 root，上下文安装失败也会异常完成。业务不调用 `get()`，真实失败也进入默认策略。取消、拒绝、调用方等待超时分别处理，不返回伪成功，不自动重试或补偿。

### 自定义 ThreadLocal 一次接入

默认已传递完整 MDC/Trace。通过 `io.micrometer.context.ThreadLocalAccessor` 适配已有 holder，并添加 `@Qualifier("dtpContextAccessor")`，例如：

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

各 holder 使用不同稳定 key；只收集显式限定的 Bean，不扫描全局 ThreadLocal。getter 不应有副作用，返回不可变值或防御副本；任务无值时 remove，结束后恢复旧值。事务、连接、Session、Servlet 对象、凭据和自动 SecurityContext 复制不在传递范围。

旧 Dtp/TraceRoute 包装保留构造时的 Trace/MDC 捕获，新增 holder 按托管提交点捕获；无参 DtpTaskDecorator 和静态 DtpThreads 保留 Trace-only 行为。自定义旧包装私有快照不可见时保持业务执行语义，诊断明确 Trace 未知，避免错误关联提交请求。

### CompletableFuture 后续阶段与 Spring Async

普通 executor 捕获的是实际 execute 时点，可能已经是另一个完成线程。需要固定请求上下文时，在正确请求/任务作用域内绑定一次：

```java
Executor flow = threadManage.contextualExecutor("emailExecutor");
CompletableFuture<String> converted = upstream.thenApplyAsync(this::convert, flow);
```

后续 Async 方法显式使用该视图，并保持请求/链局部；每次调度仍是独立任务。非 Async 节点按 JDK 规则执行。绑定负责上下文，不能读取任意隐藏 stage 结果；root helper 观察其掌握的根阶段，业务继续保留和处理最终返回链。

没有用户 TaskDecorator 时，Boot 默认 builder 自动接入；已有装饰器保留，可一次使用 Spring CompositeTaskDecorator 组合。六种标准源工程通过已有 AsyncConfigurer 可选委托 `dtpAsyncUncaughtExceptionHandler`；旧生成/自定义配置按此方式一次接入。不增加第二个用户配置器，原执行器选择及 Boot force 模式保持。

默认 `dtpTaskExceptionHandler` 复用公共 ExceptionLogger 分类、级别和脱敏堆栈；提供该精确名称的 Bean 可替换默认策略。真实计数和策略通知不因默认日志限频丢失，系统异常、业务异常、慢诊断分别预算。策略应线程安全、快速，不同步回投同一故障池。组件只组合自建工厂的 uncaught 处理，保留原工厂属性/handler。

完整终态以受控池/入口为准。第三方 Spring Future 若只能通过 TaskDecorator 接入，安装失败时取消尚未执行业务的原 Future 并记录技术诊断，其内部异常 setter 不可访问；组件拥有的虚拟 Future 则以原技术原因异常完成并确保释放许可。任意第三方隐藏阶段不在完整观测保证内。

### 配置和技术指标

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

关闭额外 holder 仍保留 Trace/MDC；统计与异常报告分别控制。关闭 void 报告时，已观察的 void 失败仍不会误记成功。

增加 `dtp.context.propagation.failures`（固定 CAPTURE/BACKUP/INSTALL/RESTORE）、`dtp.task.exception.reporter.failures`、`dtp.executor.uncaught`。沿用原池维度，不将 Trace、用户或 holder 值作为标签；未知池只诊断，不编造完整池计数。原线程/队列/虚拟限制调整和 Redis 契约保留。

starter 直接声明仓库已有、BOM 管理的 `io.micrometer:context-propagation`；指标后端仍可选。测试、编译和打包不启动项目或外部服务。
