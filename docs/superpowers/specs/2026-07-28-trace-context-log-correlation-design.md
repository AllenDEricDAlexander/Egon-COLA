# Egon COLA Trace Context 与日志关联架构改造设计

> 审核状态：已按用户修订意见实施，待最终代码复核。Trace 与 Log 均归属 `egon-cola-component-common` 聚合；不保留顶层 Trace 聚合或旧坐标过渡层。

## 目标

建立统一、轻量、可扩展、与现有 Micrometer Observation/OpenTelemetry 兼容的 Trace Context、结构化业务日志和跨协议传播能力。Trace Core、Trace Spring Boot Starter 与 Common Log 都归入 `egon-cola-component-common` 聚合。改造范围覆盖 DDC、RPC、Gateway、DTP、Bytecode，以及涉及 Spring Boot、Spring MVC、内嵌 Tomcat、WebFlux、Reactor Netty、RestClient、WebClient 和 gRPC 的相关代码。

## 非目标

- 不建设日志采集平台、ELK、Loki、日志文件滚动、日志存储。
- 不建设完整 APM。
- 不建设请求/响应审计平台。
- 不引入另一套独立 OpenTelemetry SDK。
- 不复制 FamilyAiButler 的六个 Starter、超大型 `FamilyLogUtil`、Controller 请求响应全文序列化、业务身份字段默认透传或 WebFlux 只在入口线程写一次 MDC 的实现。
- 不默认传播 `userId`、`accountId`、`sessionId`、`Authorization`、Token、手机号、设备信息等身份或敏感数据。
- 不强制替换业务应用 `logback.xml`。

## 当前审查结论

### common

- `egon-cola-component-common` 当前聚合模块只有 `common-core`、`common-id-starter`、`common-crypto`、`common-mask`、`common-test`。
- `TraceContext` 和 `TraceSnapshot` 当前位于 `egon-cola-component-common-core`，包名为 `top.egon.cola.component.common.trace`。
- 当前 `TraceContext` 只维护 MDC key `traceId`，提供 `getTraceId()`、`setTraceId()`、`clearTraceId()`、`snapshot()`。
- 当前 `TraceSnapshot` 只是一个只读 `traceId` DTO，没有 Scope、任务包装、Executor 装饰或完整 MDC 恢复能力。
- `ResultRecord` 和 `PageResultRecord` 只通过 `TraceContext.getTraceId()` 输出响应 `traceId`，这是需要保留的兼容入口。

### DDC

- `DdcTraceIdFilter` 是 Admin 私有 Servlet Filter，使用 `X-Trace-Id` 和 UUID 字符串生成 trace，结束时只删除 `traceId`。
- `HttpDdcAdminClient` 私有构建 `RestClient.Builder`，全局 `RestClientCustomizer` 不一定能影响 `register`、`heartbeat`、`offline`、`pull`、`reportDefaults`、`ack`。
- `DdcOpenApiServiceRegistryClient` 也私有构建 `RestClient`，注册中心 `register`、`heartbeat`、`deregister`、`getInstances`、`getServiceKeys` 需要同样处理 trace header。
- `DdcRuntimeCoordinator` 用自建 `ScheduledExecutorService` 执行 heartbeat/reconcile，没有 TraceScope。
- `DdcRegistrySubscriptionManager` 和 `DdcRedisChangeSubscription` 使用 Redisson listener 与 scheduler 回调，没有 TraceScope，存在回调线程 MDC 残留风险。
- `DdcAckDelivery` 使用单线程 `ScheduledThreadPoolExecutor` 重试 ACK，没有捕获提交线程 trace snapshot，也没有执行前后恢复工作线程上下文。

### RPC

- `RpcMetadataKeys` 已定义小写 gRPC Metadata key：`traceparent`、`tracestate`、`x-egon-trace-id`、`x-egon-rpc-invocation-id`、sourceApp、sourceInstance 等。
- `RpcConsumerClientInterceptor` 当前在拦截器构造时生成 `invocationId` 和 `traceId`，而不是在真实 `ClientCall.start()` 时基于当前上下文生成，可能与实际调用线程 trace 脱节。
- Consumer 当前只写 `x-egon-trace-id`，未创建 child span，未写 requestId，未区分 traceId/requestId/spanId/invocationId。
- `RpcProviderServerInterceptor` 当前解析 `x-egon-trace-id` 后只写 gRPC `Context`，不写 MDC，不包装 `ServerCall.Listener` 回调，线程切换和 streaming 扩展下日志会丢 trace。

### Gateway

- `GatewayTraceContext` 当前承担 W3C `traceparent` 解析、traceId/spanId 生成、tracestate 限制、header conflict 检测和 Gateway engineSpanId 生成。
- `GatewayTelemetry` 已通过 Micrometer Observation 尝试将 Gateway trace 与 Micrometer span 对齐；当存在有效 tracing span 时会用 Micrometer traceId/spanId 替换 fallback。
- Gateway HTTP/RPC data plane 当前直接使用 `GatewayTraceContext`，下游只稳定写 `traceparent`，部分响应/错误路径仍使用 `x-trace-id`，不是统一的 `x-egon-trace-id`。
- Gateway Engine 是自研 Reactor Netty 数据面，不适用 Spring Cloud Gateway `GlobalFilter`。
- `GatewayCallEventV1.Trace` 当前只有 `traceId`、`engineSpanId`、`sampled`，attempt 里记录 spanId。

### Bytecode 与 DTP

- Bytecode starter 的 `ObservationRuntime` 通过 `MDC.get("traceId")` supplier 获取 traceId，必须继续兼容。
- Bytecode executor propagation 有 `MdcContextCarrier`，它用完整 MDC `clear/setContextMap` 恢复。
- DTP starter 有 `DtpContextSnapshot`、`DtpRunnable`、`DtpCallable`，同样用完整 MDC `clear/setContextMap` 恢复；后续应避免与 `TraceSnapshot.decorate()` 双重包装。

### FamilyAiButler 参考结论

可吸收：

- Servlet 入口捕获前置 MDC、请求结束后恢复原 MDC。
- WebClient 出站优先从 Reactor Context 读上下文，再降级到 MDC。
- Runnable/Callable/Executor 在提交线程捕获上下文，在执行线程先恢复、执行后恢复工作线程原上下文。
- gRPC Provider 使用 `ForwardingServerCallListener` 包装 `onMessage`、`onHalfClose`、`onCancel`、`onComplete`、`onReady`，每个回调恢复上下文。

明确不吸收：

- 六个独立 Starter 全量复制。
- core 依赖 Spring。
- `MDC.clear()` 作为通用恢复策略。
- 默认传播 account/profile/client/session/device/risk 等身份字段。
- Controller AOP 默认序列化完整请求参数、请求体和响应体。
- WebFlux 只在入口线程写一次 MDC。

## 推荐方案

采用 common 聚合内的分层设计：

1. `common-trace` 作为协议无关、Spring 无关、JDK+slf4j-only 的 Trace 核心，并提供单类 `CommonLogUtil` 业务日志工具。
2. `common-trace-spring-boot-starter` 作为 Spring Boot Web/MVC/WebFlux/RestClient/WebClient 自动装配层。
3. RPC、Gateway、DDC 直接接入 `common-trace`，不再各自维护 traceId 校验和生成逻辑。
4. Gateway 的 Micrometer Observation/OpenTelemetry 仍由 Gateway Engine 自己掌握，`common-trace` 只做轻量 fallback 与 MDC 投影，不创建与 OTel 平行冲突的 span。

备选方案与取舍：

| 方案 | 说明 | 结论 |
|---|---|---|
| 只增强 `common-core` | 改动少，但 core 会继续承担 trace、pojo、exception、converter 等职责 | 不推荐，违背拆出 trace 的目标 |
| 完全复制 FamilyAiButler 六 Starter | 覆盖面广，但依赖污染、实现过重、默认传播敏感身份字段 | 不采用 |
| common 聚合内的 `common-trace` + Trace Spring Starter，RPC/Gateway/DDC 直接接入 | Trace 与日志关联共用 MDC 边界，依赖可控，不为一个工具类单建 artifact | 采用 |

## 模块布局

### common 聚合

修改：

- `egon-cola-components/egon-cola-component-common/pom.xml`
- `egon-cola-components/egon-cola-component-common/README.md`
- `egon-cola-components/egon-cola-component-common/README.zh-CN.md`

新增：

- `egon-cola-components/egon-cola-component-common/egon-cola-component-common-trace/pom.xml`
- `egon-cola-components/egon-cola-component-common/egon-cola-component-common-trace/src/main/java/top/egon/cola/component/common/trace`
- `egon-cola-components/egon-cola-component-common/egon-cola-component-common-trace/src/test/java/top/egon/cola/component/common/trace`
- `egon-cola-components/egon-cola-component-common/egon-cola-component-common-trace-spring-boot-starter`

调整：

- `egon-cola-component-common-core` 保留 pojo/exception/enums/converter，不再包含 trace 源码。
- `egon-cola-component-common-core` 增加对 `egon-cola-component-common-trace` 的依赖，以保证 `ResultRecord`、`PageResultRecord` 的 `TraceContext.getTraceId()` 兼容。
- `common-trace` 包名保持 `top.egon.cola.component.common.trace`，已有 import 尽量不变；依赖由下游 POM 解决，不改 Java import。

`common-trace` 生产依赖限制：

- 允许：JDK、`org.slf4j:slf4j-api`
- 禁止：Spring、Spring Boot、Servlet、WebFlux、Reactor、gRPC、Gateway、Jackson、Logback 实现、业务组件
- 测试允许：JUnit、AssertJ、logback-classic

### Common 内部 Trace 模块

`egon-cola-component-common` 直接聚合并由 BOM 导出：

- `egon-cola-component-common-trace`
- `egon-cola-component-common-trace-spring-boot-starter`

不创建与 common 平级的 `egon-cola-component-trace` 聚合，也不保留旧 artifactId 转发层。

Starter 依赖原则：

- 必须依赖 `egon-cola-component-common-trace`
- 必须使用 Spring Boot 3 `@AutoConfiguration`
- 必须使用 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- 禁止 `@ComponentScan`
- Web/MVC/WebFlux/RestClient/WebClient 相关依赖尽量使用 optional 或 classpath 条件，避免把 WebFlux 带入纯 MVC 应用，避免把 Servlet 带入纯 WebFlux 应用。

## common-trace 核心模型

### TraceKeys

职责：

- 定义 MDC key、HTTP header、gRPC metadata 字段名、Reactor Context key。
- 定义组件拥有的 MDC key 集合。
- 不包含业务身份字段和敏感字段。

MDC 字段至少包括：

| MDC key | 含义 | 默认传播 |
|---|---|---|
| `traceId` | 整条调用链 ID，32 位小写 hex 非全零 | 是 |
| `spanId` | 当前本地 span ID，16 位小写 hex 非全零 | 是，通过 `traceparent` |
| `parentSpanId` | 上游 span ID | 不单独作为 HTTP header 传播 |
| `requestId` | 单次入口请求 ID | 是，通过 `x-egon-request-id` |
| `traceFlags` | W3C flags，2 位小写 hex | 是，通过 `traceparent` |
| `tracestate` | W3C tracestate | 是，通过 `tracestate` |
| `sourceApp` | 调用方应用名 | 仅组件显式设置时传播 |
| `sourceInstance` | 调用方实例 ID | 仅组件显式设置时传播 |

标准传播字段：

- `traceparent`
- `tracestate`
- `x-egon-request-id`

兼容传播字段：

- `X-Trace-Id` 默认只读，绝不出站写入；内部统一规范化为 lower-case header name 处理。
- `x-egon-trace-id` 不读取、不写入、不做过渡期双写。
- gRPC Metadata key 全部小写。

### TraceState

定义为不可变 record：

```java
public record TraceState(
        String traceId,
        String spanId,
        String parentSpanId,
        String requestId,
        String traceFlags,
        String tracestate,
        String sourceApp,
        String sourceInstance
) implements Serializable
```

约束：

- 构造时校验 traceId、spanId、parentSpanId、traceFlags、tracestate。
- traceId 必须是 32 位小写十六进制非全零。
- spanId 必须是 16 位小写十六进制非全零。
- parentSpanId 可空；非空时必须是合法 spanId。
- requestId 可空；入口/出站创建时应补齐。
- traceFlags 为空时默认 `00`；非空必须是 2 位小写十六进制。
- tracestate 可空；非空时最大 512 字符，拒绝 CR/LF。
- 不包含 `userId`、`accountId`、`sessionId`、Authorization、Token、手机号、设备信息。

建议方法：

- `TraceState.root()`
- `TraceState.root(String requestId)`
- `TraceState.child()`
- `TraceState.withRequestId(String requestId)`
- `TraceState.withSource(String sourceApp, String sourceInstance)`
- `TraceState.toMdcMap()`
- `TraceState.fromMdc(Map<String, String>)`
- `String traceparent()`

### TraceIds

职责：

- 使用 JDK `SecureRandom` 生成 W3C traceId/spanId。
- 拒绝全零值。
- 提供格式校验与规范化。

约束：

- traceId 不使用 Snowflake、数据库 ID、UUID 字符串直接充当。
- spanId 不使用 UUID 或业务 ID 截断充当。
- 输出全部小写 hex。

建议方法：

- `newTraceId()`
- `newSpanId()`
- `isValidTraceId(String)`
- `isValidSpanId(String)`
- `normalizeTraceId(String)`
- `normalizeSpanId(String)`

### TraceParent

职责：

- 解析和编码 W3C `traceparent`。

解析规则：

- 必须拒绝 CR/LF。
- 必须校验长度和四段结构。
- 版本 `ff` 必须拒绝。
- 第一版实现只接受版本 `00` 的固定长度格式：`00-<32hex traceId>-<16hex spanId>-<2hex flags>`。
- traceId/spanId 全零拒绝。
- flags 必须 2 位 hex。
- 非法字符拒绝。
- 解析失败不能抛给业务路径，应返回 invalid result，让传播层继续走兼容 header 或生成新 trace。

### TraceStateHeader

职责：

- 校验和规范化 W3C `tracestate`。

规则：

- 空白视为不存在。
- 最大长度 512。
- 拒绝 `\r`、`\n`。
- 第一版不强制完整 vendor grammar，但必须保证不会造成 header 注入。

### TraceCarrierReader / TraceCarrierWriter

协议无关接口：

```java
@FunctionalInterface
public interface TraceCarrierReader<C> {
    String get(C carrier, String name);
}

@FunctionalInterface
public interface TraceCarrierWriter<C> {
    void set(C carrier, String name, String value);
}
```

使用方式：

- HTTP Servlet：`HttpServletRequest::getHeader`，`HttpServletResponse::setHeader`
- Spring `HttpHeaders`：`headers::getFirst`，`headers::set`
- WebClient `ClientRequest.Builder`：通过 headers consumer 写入
- Reactor Netty headers：大小写无关读取，lower-case 写入
- gRPC `Metadata`：用小写 `Metadata.Key<String>`

### TracePropagation

职责：

- 按统一优先级从 carrier 提取 TraceState。
- 向 carrier 注入标准和兼容 header。
- 暴露 header 冲突标识和来源。

提取优先级：

1. 合法 `traceparent`
2. 配置开启时合法 `X-Trace-Id` 只读兼容值
3. 全部无效则生成新 TraceState

冲突检测：

- 如果 `traceparent` 合法且兼容 trace header 合法，但 traceId 不一致，则 `headerConflict=true`。
- 冲突不改变主协议优先级，仍以 `traceparent` 为准。
- 冲突信息通过 `Extraction` 返回，供 Gateway 安全日志和指标使用。

建议模型：

```java
public record TraceExtraction(
        TraceState state,
        TraceSource source,
        boolean generated,
        boolean headerConflict,
        String conflictingTraceId
)
```

注入规则：

- 默认写入：
  - `traceparent`
  - `tracestate`，仅非空时
  - `x-egon-request-id`
- 永不写入 `x-egon-trace-id`、`x-trace-id` 或 `X-Trace-Id`。
- 不覆盖业务方已明确设置且合法的 `traceparent`，除非配置 `takeoverExistingTraceHeaders=true`。
- 如果已有非法 trace header，组件可覆盖为合法值。
- sourceApp/sourceInstance 只在组件显式提供时写入对应协议字段，不从身份 header 推断。

### TraceContext

职责：

- 作为兼容静态入口，仍基于 MDC 投影当前 TraceState。
- 提供打开/关闭 Scope、当前上下文读取、懒生成、兼容 traceId 方法。

兼容保留：

- `getTraceId()`
- `setTraceId(String traceId)`
- `clearTraceId()`
- `snapshot()`

新增建议 API：

- `Optional<TraceState> current()`
- `TraceState currentOrCreate()`
- `TraceState currentOrCreate(String requestId)`
- `TraceScope open(TraceState state)`
- `TraceScope open(TraceState state, Map<String, String> additionalMdc)`
- `TraceSnapshot snapshot()`
- `void clearOwnedKeys()`
- `String putIfAbsent(String key, String value)`

语义：

- `current()` 从 MDC owned keys 读取并校验；无法组成合法 TraceState 时返回 empty。
- `currentOrCreate()` 优先返回当前合法 TraceState，否则生成 root TraceState 并打开轻量当前上下文。
- `setTraceId()` 为兼容方法，只设置 `traceId`；如果当前没有 spanId，不自动补齐 span，避免老调用方无意创建完整链路。
- 新入口代码应优先使用 `TraceContext.open(TraceState)`。

### TraceScope

职责：

- 实现 `AutoCloseable`。
- 打开时将 TraceState 投影到 MDC。
- 关闭时恢复进入 Scope 前的状态。

恢复策略：

- Scope 捕获进入前完整 MDC 快照用于安全回滚。
- Scope 只声明拥有 `TraceKeys.ownedMdcKeys()` 和打开时显式写入的 additional keys。
- 普通 `TraceScope.close()` 默认只恢复 owned keys，保留作用域内业务新增的非 owned MDC 字段，避免破坏其他框架。
- `TraceSnapshot.open()` 用于线程池任务恢复时，可选择“任务级完整恢复”：执行前保存工作线程完整 MDC，执行后完整恢复工作线程原 MDC，防止线程池泄漏。
- 支持嵌套 Scope：内层关闭恢复到外层状态，外层关闭恢复到进入外层前状态。
- 支持异常路径。
- 重复关闭安全：第二次 close 无副作用。
- 禁止简单 `MDC.clear()` 作为普通 Scope 收尾。

### TraceSnapshot

职责：

- 保存提交线程的完整 TraceState 与必要 MDC 快照。
- 提供跨线程任务传播工具。

建议字段：

- `TraceState state`
- `Map<String, String> mdcSnapshot`

建议 API：

- `String getTraceId()`：兼容旧 API。
- `TraceState state()`
- `TraceScope open()`
- `Runnable wrap(Runnable runnable)`
- `<T> Callable<T> wrap(Callable<T> callable)`
- `<T> Supplier<T> wrap(Supplier<T> supplier)`
- `Executor decorate(Executor executor)`

线程池语义：

- 包装发生在提交线程，捕获提交线程 snapshot。
- 执行前在工作线程捕获原 MDC。
- 执行中恢复提交线程 TraceSnapshot。
- 执行后完整恢复工作线程原 MDC。
- 异常原样抛出。
- 不泄漏到线程池复用线程。

## Spring Trace Starter

### 模块与包

模块：

- `egon-cola-component-common-trace-spring-boot-starter`

包：

- `top.egon.cola.component.common.trace.autoconfigure`

AutoConfiguration imports：

```text
top.egon.cola.component.common.trace.autoconfigure.TraceAutoConfiguration
```

### TraceProperties

配置前缀：

```properties
egon.cola.component.trace
```

配置清单：

| 配置项 | 默认值 | 运行时消费点 |
|---|---:|---|
| `enabled` | `true` | 所有自动配置总开关 |
| `propagation.enabled` | `true` | 入站提取、出站注入 |
| `propagation.legacy-trace-id-read-only` | `true` | 是否只读兼容 `X-Trace-Id` |
| `propagation.response-headers` | `true` | 是否允许响应写回标准 Header |
| `propagation.source-headers` | `false` | 是否显式接收 sourceApp/sourceInstance |
| `servlet.enabled` | `true` | Servlet Filter |
| `servlet.order` | `Integer.MIN_VALUE + 100` | FilterRegistrationBean order |
| `servlet.response-headers` | `true` | 响应写 `traceparent` 与 `x-egon-request-id` |
| `servlet.access-log` | `true` | 有限结构化完成日志 |
| `servlet.excluded-paths` | empty | 访问日志排除路径 |
| `servlet.slow-request-threshold` | `1s` | 慢请求阈值 |
| `servlet.record-query` | `false` | 是否记录 query |
| `servlet.record-headers` | `false` | 是否记录 header |
| `servlet.record-request-body` | `false` | 是否记录请求体 |
| `servlet.record-response-body` | `false` | 是否记录响应体 |
| `servlet.trusted-proxy-headers` | `false` | 是否信任代理来源 Header |
| `webflux.enabled` | `true` | WebFilter |
| `rest-client.enabled` | `true` | RestClientCustomizer |
| `rest-client.take-over-existing-traceparent` | `false` | 是否覆盖合法 traceparent |
| `web-client.enabled` | `true` | WebClientCustomizer |
| `web-client.take-over-existing-traceparent` | `false` | 是否覆盖合法 traceparent |
| `reactor.automatic-context-propagation` | `true` | ThreadLocalAccessor/ContextRegistry |

要求：

- 每个配置项必须有生产代码消费点和自动配置测试。
- 禁止存在“配置项已定义但运行时未读取”的空转项。

### Servlet / Spring MVC / Tomcat 入站

组件：

- `TraceServletFilter extends OncePerRequestFilter`
- `TraceAccessLogger`
- `ClientIpResolver`

自动装配：

- `@ConditionalOnWebApplication(type = SERVLET)`
- `@ConditionalOnClass({OncePerRequestFilter.class, HttpServletRequest.class})`
- `@ConditionalOnProperty(prefix = "egon.cola.component.trace.servlet", name = "enabled", havingValue = "true", matchIfMissing = true)`
- `@ConditionalOnMissingBean(name = "egonTraceServletFilterRegistration")`

行为：

1. 在高优先级 Filter 中读取请求 header。
2. `TracePropagation.extract()` 提取或生成 TraceState。
3. 入口服务创建当前服务 spanId，parentSpanId 指向上游 spanId。
4. requestId 优先来自 `x-egon-request-id`，没有则生成。
5. 使用 `TraceContext.open(state, additionalMdc)` 写入 MDC。
6. 通过配置写回响应 `traceparent`、`x-egon-request-id`，不写任何兼容 traceId Header。
7. 请求结束、异常或 async dispatch 完成后恢复上下文。
8. 默认输出一条结构化完成日志。

访问日志字段：

- `protocol`
- `method`
- `path` 或 route template
- `status`
- `cost_ms`
- `traceId`
- `spanId`
- `requestId`
- `clientIp`
- `errorCode`
- `responseBytes`
- `slow`

安全限制：

- 不默认记录请求体、响应体、完整 query、完整 header。
- 不序列化文件、Multipart、ServletRequest、ServletResponse、Flux、Mono、SSE、StreamingResponseBody 或大对象。
- `X-Forwarded-For` 只有在 `trusted-proxies-enabled=true` 且远端地址匹配可信代理时使用。

### WebFlux 入站

组件：

- `TraceWebFilter implements WebFilter`
- `TraceReactorContextAccessor implements ThreadLocalAccessor<TraceState>`
- `TraceReactorContextKeys`

原则：

- Reactor Context 是响应式链路真实上下文来源。
- MDC 只是当前执行线程的日志投影。
- 不能只在 `WebFilter` 入口 `MDC.put()` 一次。

行为：

1. `TraceWebFilter` 从请求 header 提取或生成 TraceState。
2. 将 TraceState 写入 Reactor Context。
3. 通过 `TraceScope` 在 filter 初始订阅点投影 MDC。
4. WebClient 出站通过 `Mono.deferContextual()` 优先读取 Reactor Context。
5. 通过 Micrometer Context Propagation 的 `ThreadLocalAccessor`/`ContextRegistry` 或 Spring Boot 已启用的自动上下文传播机制，在 `publishOn`、`subscribeOn`、`flatMap`、`boundedElastic`、parallel Scheduler、Reactor Netty EventLoop 和错误恢复路径恢复 MDC。

重复注册控制：

- 使用静态 `AtomicBoolean` 或 bean lifecycle guard，确保 ThreadLocalAccessor 不重复注册。
- 如果检测到同 name accessor 已注册，则跳过。
- 不无条件启用全局 Reactor Hook；只有在配置开启且当前环境没有 Boot/Micrometer 自动传播时才启用必要适配。

### RestClient 出站

组件：

- `TraceRestClientCustomizer implements RestClientCustomizer`
- `TraceClientRequestInterceptor`

行为：

1. 在拦截器执行时读取 `TraceContext.currentOrCreate()`。
2. 每次下游调用创建 child `TraceState`。
3. 注入 `traceparent`、`tracestate`、`x-egon-request-id`。
4. 不覆盖业务方已设置且合法的 `traceparent`，除非 `takeover-existing-trace-headers=true`。
5. 执行请求时用 `TraceScope` 打开 child span，确保出站日志也带 child span。

### WebClient 出站

组件：

- `TraceWebClientCustomizer implements WebClientCustomizer`
- `TraceExchangeFilterFunction`

行为：

1. 使用 `Mono.deferContextual()`。
2. 优先从 Reactor Context 获取 TraceState。
3. Reactor Context 无值时降级到 `TraceContext.current()`。
4. 每次请求创建 child span。
5. 注入标准 header。
6. 保证 WebFlux 线程切换后仍取得正确 trace。

## RPC 改造

### Metadata 语义

| 字段 | 语义 | 生命周期 |
|---|---|---|
| `traceId` | 整条调用链 ID | 跨服务保持不变 |
| `spanId` | 当前 RPC 调用 span | 每次 RPC client start 创建 |
| `parentSpanId` | 调用方当前 span | 来自上游/当前上下文 |
| `requestId` | 入口请求 ID或后台逻辑操作 ID | 跨服务传播 |
| `invocationId` | RPC 逻辑调用 ID | 一次业务 RPC 调用保持不变，重试不变 |

### RpcMetadataKeys

保留现有 key，新增：

- `REQUEST_ID = ascii("x-egon-request-id")`
- 必要时新增 `SPAN_ID` 只用于本地 metadata model，不建议单独作为 wire header；wire span 使用 `traceparent`。

### RpcInvocationMetadata

建议扩展：

```java
public record RpcInvocationMetadata(
        String service,
        String group,
        String version,
        String invocationId,
        String sourceApp,
        String sourceInstance,
        String traceId,
        String spanId,
        String parentSpanId,
        String requestId,
        String traceparent,
        String tracestate,
        TraceState traceState
)
```

保留 `current()`，新增：

- `currentTraceState()`
- `currentOrEmpty()`

### Consumer

改造点：

- `RpcConsumerClientInterceptor` 不在构造函数捕获 trace metadata。
- `RpcConsumerInvocationHandler.invoke()` 为一次逻辑 RPC 调用生成 invocationId。
- 每次 `ClientCall.start()` 基于当前 TraceState 创建 child span。
- 写入：
  - `traceparent`
  - `tracestate`
  - `x-egon-request-id`
  - `x-egon-rpc-invocation-id`
  - `x-egon-rpc-source-app`
  - `x-egon-rpc-source-instance`
- 对 gateway retry，每次实际 `start()` 有独立 child span，invocationId 保持不变。

### Provider

改造点：

- 使用 `TracePropagation.extract()` 从 gRPC Metadata 解析 TraceState。
- 创建 provider 当前 span，parentSpanId 指向 consumer span。
- 将 TraceState 写入 gRPC `Context` 和 MDC。
- 包装 `ServerCall.Listener` 的每个回调：
  - `onMessage`
  - `onHalfClose`
  - `onCancel`
  - `onComplete`
  - `onReady`
- 每个回调开始时 `TraceContext.open(state)`，结束时 close 恢复。
- `next.startCall()` 本身也在 Scope 中执行，执行后恢复原上下文。
- 当前 unary 也按 Listener 模型实现，避免未来 streaming 重做。

## Gateway 改造

### GatewayTraceContext 下沉边界

下沉到 `common-trace`：

- W3C `traceparent` 解析。
- `tracestate` 校验。
- traceId/spanId 生成与校验。
- header 优先级。
- header conflict 检测。
- 协议无关 carrier。

保留在 Gateway：

- `engineSpanId`
- `headerConflict`
- `source`
- provider attempt span
- Gateway 安全/路由/治理相关 trace 投影字段

建议改造后：

```java
public record GatewayTraceContext(
        TraceState state,
        String engineSpanId,
        Source source,
        boolean headerConflict
)
```

保留兼容访问器：

- `traceId()`
- `parentSpanId()`
- `engineSpanId()`
- `traceFlags()`
- `tracestate()`
- `sampled()`
- `engineTraceparent()`
- `childTraceparent(String childSpanId)`

### Reactor Netty 数据面

要求：

- 不使用 Spring Cloud Gateway `GlobalFilter`。
- Gateway HTTP Listener 在最早阶段建立 TraceState 并写入 Reactor Context。
- 路由、安全、鉴权、限流、熔断、重试、HTTP Upstream、RPC Upstream、GatewayCallEvent 都使用同一 Trace 来源。
- `TraceScope` 用于日志投影，不把 MDC 当作 Reactor 真实上下文。

### Micrometer / OpenTelemetry 对齐

要求：

- Gateway 已有 Micrometer Tracing Bridge 和 OpenTelemetry，Gateway traceId/spanId 优先与当前 Observation/Tracer Span 对齐。
- 有有效 Observation span 时，由 Gateway adapter 读取 Micrometer `TraceContext` 并生成/替换 Gateway TraceState 的当前 span。
- 没有 Tracer 或 span 为 noop 时，才使用 common-trace 轻量生成逻辑。
- 禁止 common-trace 在 Gateway 中创建与 OTel 不一致的平行 span。
- Gateway 普通日志、OpenTelemetry Trace、`GatewayCallEventV1.Trace`、管理端 Trace 查询、下游 `traceparent` 中的 traceId/spanId 必须一致。

### Attempt span

- 每次 Provider Attempt 创建独立 child spanId。
- 重试不能复用相同 attempt spanId。
- 整个请求 traceId 保持不变。
- `GatewayCallEventV1.Attempt.spanId` 记录 attempt spanId。

### Header 统一

- Gateway 入站只接受 `traceparent`、`tracestate`、`x-egon-request-id`；无兼容期，不读取旧 traceId Header。
- 出站统一写 `traceparent`、`tracestate`、`x-egon-request-id`。
- 响应错误路径写标准 `traceparent` 与 requestId，不写 `x-trace-id` 或 `x-egon-trace-id`。

## DDC 改造

### 内部 Trace 注入器

新增内部组件：

- `DdcTraceSupport`
- `DdcTraceHttpHeadersInjector`
- `DdcTraceOperations`

职责：

- 对 `HttpDdcAdminClient` 和 `DdcOpenApiServiceRegistryClient` 显式注入 trace header。
- 不依赖全局 `RestClientCustomizer` 生效。
- 在 `register`、`heartbeat`、`offline`、`pull`、`reportDefaults`、`ack`、registry register/heartbeat/deregister/query 中传播当前 trace。

规则：

- 业务请求触发 DDC 调用：继承当前 traceId，创建 child span。
- 后台注册、心跳、定时 pull、ACK 重试、Redis Topic 回调、租约恢复：没有上游 trace 时，为每次逻辑操作创建独立 root TraceScope。
- 执行结束后恢复线程原上下文。
- 结构化日志 additional MDC 可包含：
  - `component=ddc`
  - `operation=heartbeat|pull|ack|register|offline|reconcile|redis-event|registry-refresh`
- 不把这些 additional MDC 默认跨服务传播。

### 高频日志

- 成功 heartbeat 默认 DEBUG 或仅指标。
- 失败、重试耗尽、状态变化输出 WARN/ERROR。
- ACK queue saturation、non-retryable、exhausted 保持 WARN，但需带 traceId/requestId。

## Spring MVC Admin 应用接入

DDC Admin、Gateway Admin 等 Spring MVC 应用应直接依赖 `egon-cola-component-common-trace-spring-boot-starter`。

删除或合并：

- `DdcTraceIdFilter`
- `GatewayAdminTraceFilter`
- 后续可覆盖 `DtpTraceIdFilter`

行为：

- Admin 应用不再各自维护 Filter。
- 统一使用 `traceparent` 主协议和 `x-egon-request-id` 请求字段，不携带 Egon traceId 字段。

## Bytecode 与 DTP 兼容策略

Bytecode：

- `ObservationRuntime` 的 traceId supplier 改为 `TraceContext::getTraceId` 或保持 MDC key `traceId`，两者都兼容。
- 不强制引入 `common-trace` 到 bytecode runtime；如在 starter 层接入，只在 starter 层依赖。
- Bytecode `MdcContextCarrier` 可保持完整 MDC 传播，但文档标注与 Trace Snapshot 的关系。

DTP：

- 如果本次范围允许改 DTP，则将 `DtpContextSnapshot` 内部实现委托给 `TraceSnapshot` 或复用 common-trace 的 owned-key 恢复策略。
- 如果本次不改 DTP，则在 Trace Starter 自动包装 Executor 时跳过已由 DTP/Bytecode 包装的任务，避免双重嵌套。
- DTP Admin 后续应删除私有 `DtpTraceIdFilter` 并接入 Trace Spring Starter。

## 业务日志边界

参考 `FamilyLogUtil` 的单类组织方式，在 `common-trace` 中只暴露一个顶层
`CommonLogUtil`；业务日志 Builder 和阶段枚举均为内部类型，不再创建独立
`common-log` artifact。

README 可定义推荐字段：

- `biz`
- `scene`
- `step`
- `phase`
- `bill_type`
- `bill_id`
- `biz_id`
- `status`
- `decision`
- `error_code`
- `cost_ms`
- `msg`

`CommonLogUtil` 复用 `common-trace` 已有的 `slf4j-api` 依赖。每次真正打印时读取
完整 MDC，按照 Trace 字段、其余 MDC 字段、业务字段的顺序，直接渲染为稳定单行
`key=value` 消息，不依赖 `%X`、`%mdc`、`%kvp` 或特定日志实现。字符串值单行化并
限长，集合限制元素数，扩展字段执行基础脱敏，异常保留原生 cause。生产代码仍不依赖
Spring、Logback、Jackson、日志采集平台或 APM。

## 协议传播矩阵

| 场景 | 入站来源 | 当前上下文来源 | 出站字段 | Span 策略 | 备注 |
|---|---|---|---|---|---|
| Servlet/MVC | HTTP headers | MDC TraceScope | 响应 header | 入口 server span | access log 有限字段 |
| WebFlux | HTTP headers | Reactor Context | 响应 header | 入口 server span | MDC 为线程投影 |
| RestClient | TraceContext/MDC | 当前线程 | HTTP headers | 每次请求 child span | 不覆盖合法 traceparent |
| WebClient | Reactor Context 优先，MDC 降级 | Reactor Context | HTTP headers | 每次请求 child span | 支持线程切换 |
| RPC Consumer | TraceContext/MDC | 调用开始时 | gRPC Metadata | 每次 start child span | invocationId 与 spanId 分离 |
| RPC Provider | gRPC Metadata | gRPC Context + MDC | 业务内当前上下文 | provider server span | Listener 每回调恢复 |
| Gateway HTTP | HTTP headers | Reactor Context + GatewayTraceContext | Upstream HTTP headers | request span + attempt span | 与 OTel span 对齐 |
| Gateway RPC | gRPC Metadata | gRPC Context + GatewayTraceContext | Upstream gRPC Metadata | request span + attempt span | attempt span 不复用 |
| DDC Client | TraceContext/MDC 或 root | TraceScope | HTTP headers | child/root operation span | 私有 RestClient 显式注入 |
| DDC 后台任务 | root 或提交 snapshot | TraceScope | HTTP headers | 每次逻辑操作独立 | 执行后恢复线程 |

## 兼容性影响

保持兼容：

- Java import `top.egon.cola.component.common.trace.TraceContext` 尽量不变。
- `TraceContext.getTraceId()`、`setTraceId()`、`clearTraceId()`、`snapshot()` 保留。
- `TraceSnapshot.getTraceId()` 保留。
- `ResultRecord`、`PageResultRecord` 继续只输出 `traceId`。
- Gateway `GatewayTraceContext.traceId()` 等访问器保留。
- RPC `RpcInvocationMetadata.current()` 保留。

行为变化：

- traceId 统一为 W3C 32 位小写 hex 非全零。
- spanId 统一为 W3C 16 位小写 hex 非全零。
- `traceparent` 成为主协议。
- 不传播 `x-egon-trace-id`、`x-trace-id` 或 `X-Trace-Id`。
- `X-Trace-Id` 仅在非 Gateway 的通用提取器中默认只读兼容；Gateway 无过渡期。
- Admin 私有 Filter 被统一 Starter 替代。
- RPC Provider 日志在 listener 回调中带 trace。
- DDC 后台线程不会残留上一轮 trace。

需要审查的破坏点：

- 外部客户端如果只依赖旧 traceId Header，必须直接迁移到 W3C `traceparent`；不提供出站过渡期双写。
- Gateway 管理端和前端从响应 `traceparent` 读取 traceId，并使用 `x-egon-request-id` 关联请求。
- 依赖树新增 `egon-cola-component-common-trace` 和 `egon-cola-component-common-trace-spring-boot-starter`。

## 测试验收

### common-trace

覆盖：

- traceId/spanId 格式。
- 全零值拒绝。
- `traceparent` 正常解析。
- 非法版本。
- 非法长度。
- 非法字符。
- CRLF 注入拒绝。
- `tracestate` 长度限制和 CRLF 拒绝。
- header 优先级。
- header conflict。
- `TraceScope` 嵌套恢复。
- 异常路径恢复。
- 重复 close 安全。
- 只清理组件 owned MDC 字段。
- `TraceSnapshot.wrap(Runnable)`。
- `TraceSnapshot.wrap(Callable)`。
- `TraceSnapshot.wrap(Supplier)`。
- `TraceSnapshot.decorate(Executor)`。
- 线程池复用不泄漏。

命令：

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-trace test
```

### Trace Spring Starter

覆盖：

- Servlet 入站提取和响应 header。
- Servlet 异常路径恢复。
- async dispatch 完成后恢复。
- access log exclude paths。
- 请求体默认不记录。
- 自定义 Filter bean 时自动配置退让。
- classpath 缺 Servlet 时不创建 Servlet bean。
- 功能关闭时不装配。
- WebFlux `publishOn`。
- WebFlux `subscribeOn`。
- WebFlux `flatMap`。
- WebFlux `parallel`。
- WebFlux `boundedElastic`。
- 并发请求 trace 不丢失不串号。
- WebClient 从 Reactor Context 传播。
- RestClient 从 MDC/TraceContext 传播。
- 自定义 `RestClientCustomizer`/`WebClientCustomizer` 可共存。
- Micrometer Context Propagation accessor 不重复注册。

命令：

```bash
./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml -pl egon-cola-component-common-trace-spring-boot-starter -am test
```

### CommonLogUtil

在 `common-trace` 测试中覆盖完整 MDC 与自定义 MDC、稳定字段顺序、`msg`、异常
cause、敏感字段脱敏、字符串单行限长、集合边界和禁用日志级别短路。生产依赖树仍只
允许 `slf4j-api`。

### RPC

覆盖：

- Consumer 在 `ClientCall.start()` 生成 header。
- Consumer 每次 retry/start 创建不同 child span。
- Provider 提取 `traceparent`。
- Provider 写入 `RpcInvocationMetadata`。
- Provider 每个 Listener 回调恢复 MDC。
- Provider 回调异常后恢复原 MDC。
- 并发调用隔离。
- invalid metadata 生成新 trace。

命令：

```bash
./mvnw -B -ntp -f egon-cola-components/egon-cola-component-rpc/pom.xml test
```

### Gateway

覆盖：

- `GatewayTraceContext` 复用 common-trace parser。
- Gateway 忽略旧 traceId Header，只接受 `traceparent`。
- Gateway OTel Span 与 MDC/CallEvent 对齐。
- Gateway HTTP upstream 传播统一 header。
- Gateway RPC upstream 传播统一 metadata。
- 重试 attempt span 不重复。
- GatewayCallEvent traceId/spanId 一致。
- 错误响应写 `traceparent` 与 `x-egon-request-id`，不写旧 traceId Header。

命令：

```bash
./mvnw -B -ntp -f egon-cola-components/egon-cola-component-gateway/pom.xml test
```

### DDC

覆盖：

- `HttpDdcAdminClient` register/heartbeat/offline/pull/reportDefaults/ack 注入 trace header。
- `DdcOpenApiServiceRegistryClient` register/heartbeat/deregister/query 注入 trace header。
- 业务调用继承当前 trace 并创建 child span。
- 后台 heartbeat 无上游请求时创建并清理 Trace。
- reconcile/pull 创建并清理 Trace。
- Redis listener 回调线程不残留 Trace。
- ACK submit 捕获 snapshot，重试线程不泄漏 MDC。
- 高频成功 heartbeat 不输出 INFO。

命令：

```bash
./mvnw -B -ntp -f egon-cola-components/egon-cola-component-dynamic-config-center/pom.xml test
```

### 依赖和集成检查

命令：

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml -pl \
egon-cola-component-common/egon-cola-component-common-trace,\
egon-cola-component-common/egon-cola-component-common-trace-spring-boot-starter,\
egon-cola-component-rpc/egon-cola-component-rpc-starter,\
egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter,\
egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin,\
egon-cola-component-gateway/egon-cola-component-gateway-contract,\
egon-cola-component-gateway/egon-cola-component-gateway-engine,\
egon-cola-component-gateway/egon-cola-component-gateway-admin \
-am test

./mvnw -B -ntp -f egon-cola-components/pom.xml test

./mvnw -B -ntp -f egon-cola-components/pom.xml package -DskipTests
```

检查项：

- BOM 包含新模块。
- 自动配置 imports 存在。
- Starter 不引入不必要运行时依赖。
- Servlet/WebFlux 条件互斥。
- common-trace 依赖树只有 JDK + slf4j-api。
- 没有重复注册 Micrometer Context Propagation accessor。
- Gateway OTel span 与 `GatewayCallEvent` 对齐。
- 无 MDC 泄漏。
- 无敏感数据默认日志或传播。

## 文档更新

更新：

- `egon-cola-components/egon-cola-component-common/README.md`
- `egon-cola-components/egon-cola-component-common/README.zh-CN.md`
- `egon-cola-components/egon-cola-components-bom/README.md`
- `egon-cola-components/egon-cola-components-bom/README.zh-CN.md`
- `egon-cola-components/egon-cola-component-common/egon-cola-component-common-trace-spring-boot-starter/README.md`
- `egon-cola-components/egon-cola-component-common/egon-cola-component-common-trace-spring-boot-starter/README.zh-CN.md`
- `egon-cola-components/egon-cola-component-rpc/README.md`
- `egon-cola-components/egon-cola-component-rpc/README.zh-CN.md`
- `egon-cola-components/egon-cola-component-dynamic-config-center/README.md`
- `egon-cola-components/egon-cola-component-dynamic-config-center/README.zh-CN.md`
- `egon-cola-components/egon-cola-component-gateway/README.md`
- `egon-cola-components/egon-cola-component-gateway/README.zh-CN.md`

必须说明：

- Trace Context 与业务 ID、日志采集、APM 的边界。
- `traceId`、`spanId`、`parentSpanId`、`requestId`、`invocationId` 的差异。
- `traceparent` 是唯一 trace 主协议；Egon 只保留 requestId Header，不携带 Egon traceId Header。
- 请求体和响应体默认不记录的安全原因。
- Spring MVC、WebFlux、RestClient、WebClient、RPC、Gateway、DDC 示例。
- 通过日志 Pattern 输出 `%X{traceId}`、`%X{spanId}`、`%X{requestId}`。
- 不强制替换业务应用 logback 配置。

## 设计模式取舍

采用：

- Facade：`TraceContext` 作为兼容门面，屏蔽 MDC 和 TraceState 细节。
- Scope Guard：`TraceScope` 通过 try-with-resources 保证异常路径恢复。
- Adapter：`TraceCarrierReader`/`TraceCarrierWriter` 适配 HTTP、gRPC、Reactor Netty、Spring headers。
- Strategy：`TracePropagation` 通过配置控制 header 优先级、兼容 header、覆盖策略。
- Decorator：`TraceSnapshot.wrap/decorate` 包装异步任务和 Executor。
- Builder：`CommonLogUtil` 用内部 Builder 组织可选业务字段，扩展字段统一经过规范化、限长和脱敏。

不采用：

- 在日志工具类中重复 Trace 生成、协议传播、线程上下文恢复，以及日志平台封装。
- Controller AOP 全量日志，本次不是审计平台。
- 独立 OTel SDK 封装，Gateway 已由 Micrometer/OTel 负责。

## 实施切分建议

1. common-trace 与 Trace Spring Boot Starter 全部归入 common 聚合，业务日志工具直接放入 common-trace。
2. common-core 依赖 common-trace，并保持 `ResultRecord`/`PageResultRecord` 兼容。
3. Trace Spring Boot Starter：Servlet、RestClient、WebFlux、WebClient 与 Reactor Context。
4. RPC Consumer/Provider metadata 与 Listener Scope。
5. DDC RestClient 显式注入与后台任务 Scope。
6. GatewayTraceContext 下沉、Gateway Engine Reactor Context/MDC/OTel 对齐。
7. Admin 私有 Filter 删除并接入 common Trace Starter。
8. DTP Runnable/Callable/Supplier 与 Bytecode `MdcContextCarrier` 复用 TraceSnapshot。
9. README/BOM/依赖树/整体验证。

每个切分都有聚焦测试；按用户要求 inline 修改，未收到提交指令前不提交。

## 已确认决策

- 不新增 common 外的 Trace 聚合；Log、Trace Core 和 Trace Starter 都在 common 下。
- `X-Trace-Id` 默认只读，不提供出站过渡期。
- Gateway 无过渡期，不读写旧 traceId Header。
- 不携带 `x-egon-trace-id`。
- DTP 不新增 Starter，只增强并复用 Runnable/Callable/Supplier/Executor 上下文包装。
- Bytecode `MdcContextCarrier` 复用 common-trace Snapshot。
