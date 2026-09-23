# egon-cola-component-common-trace-spring-boot-starter

[English](README.md) | 中文

## 简要介绍

`egon-cola-component-common-trace-spring-boot-starter` 是 common 聚合内的日志关联和跨协议 Trace 传播 Starter。它在
`common-trace` 的纯 JDK + SLF4J 核心之上提供 Spring Boot 自动配置，覆盖
Servlet/Spring MVC、WebFlux、RestClient 和 WebClient。

该组件不建设日志采集平台、日志文件滚动、APM，也不替代 Micrometer Observation 或
OpenTelemetry SDK。它只负责统一 Trace Context、MDC 投影和跨协议 Header 传播：`traceparent`
线格式遵循 W3C Trace Context，因此可与 OpenTelemetry 语境的对接方互认；Reactor 跨线程回放
依赖 Micrometer `context-propagation`，而非 Micrometer Observation。

## 模块

| 模块 | 职责 |
|---|---|
| `egon-cola-component-common-trace` | 纯核心：一个完整 `TraceContext`、W3C 传播、MDC 捕获和三个本地线程任务模板 |
| `egon-cola-component-common-trace-spring-boot-starter` | Spring Boot 3 自动配置：Servlet、WebFlux、RestClient、WebClient 和 Reactor Context 投影 |

## 协议和字段

主传播协议是 W3C Trace Context：

| 方向 | Header |
|---|---|
| 入站读取 | `traceparent`、`tracestate`、`x-egon-request-id` |
| 默认兼容读取 | `X-Trace-Id` 只读；合法 `traceparent` 优先 |
| 出站写入 | `traceparent`、`tracestate`、`x-egon-request-id` |
| 不读取也不写入 | `x-egon-trace-id` |

遗留的 `X-Trace-Id` 与 `x-trace-id` 是只读别名，仅在不存在合法 `traceparent` 时被采用；`x-egon-trace-id` 既不读取也不写入。缺失的 `x-egon-request-id` 由新生成的标识补齐。

`traceId` 是一次链路的全局 ID，`spanId` 是当前处理单元 ID，
`parentSpanId` 是上游 span，`requestId` 是请求级业务排错 ID；`traceFlags`、
`tracestate`、`sourceApp`、`sourceInstance` 也都保存在同一个 `TraceContext` 中。不要把 `userId`、`accountId`、Token、手机号、
设备信息等身份或敏感数据放进核心 Trace Context；未来 baggage 必须通过显式
allowlist。

## Spring Boot 使用

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-common-trace-spring-boot-starter</artifactId>
</dependency>
```

```yaml
egon:
  cola:
    component:
      trace:
        enabled: true
        propagation:
          enabled: true
          legacy-trace-id-read-only: true
          response-headers: true
        servlet:
          enabled: true
          access-log: true
          record-query: false
          trusted-proxy-headers: false
        webflux:
          enabled: true
        rest-client:
          enabled: true
          take-over-existing-traceparent: false
        web-client:
          enabled: true
        reactor:
          automatic-context-propagation: true
```

日志 Pattern 示例：

```text
%d %-5level [%X{traceId:-},%X{spanId:-},%X{requestId:-}] %logger - %msg%n
```

默认不会记录请求体、响应体、全量 Header、Authorization、Token 或身份字段，且任何开关都不会打开这些输出。需要扩展
访问日志时，应在业务侧提供脱敏、字段 allowlist、大小限制和 Content-Type 限制。

## 配置

| 配置项 | 默认值 | 作用 |
|---|---|---|
| `enabled` | `true` | 整组自动配置总开关。 |
| `propagation.enabled` | `true` | 为 `false` 时忽略入站 Header 并新建 root 上下文。 |
| `propagation.legacy-trace-id-read-only` | `true` | 无合法 `traceparent` 时接受 `X-Trace-Id` / `x-trace-id`。 |
| `propagation.response-headers` | `true` | 响应 Header 总开关，需与对应技术栈开关同时为真。 |
| `servlet.enabled` | `true` | 注册过滤器、过滤器注册 Bean 与 MVC 日志拦截器；各自存在同类型 Bean 时退让。 |
| `servlet.order` | `Integer.MIN_VALUE + 100` | `FilterRegistrationBean` 顺序。 |
| `servlet.response-headers` | `true` | Servlet 侧响应 Header 开关。 |
| `servlet.access-log` | `true` | 由过滤器输出 `trace_access` 行。 |
| `servlet.excluded-paths` | 空 | 以 `PatternMatchUtils.simpleMatch` 匹配请求 URI；命中即整体跳过过滤器。 |
| `servlet.record-query` | `false` | 在日志路径后附加查询串。 |
| `servlet.trusted-proxy-headers` | `false` | 客户端 IP 取 `X-Forwarded-For` 首项，否则取 `getRemoteAddr()`。 |
| `webflux.*` | 同 `servlet.*` | `WebFlux` 继承 `Servlet`，每个 servlet 键都有 webflux 同名键。 |
| `rest-client.enabled`、`web-client.enabled` | `true` | 注册对应的 RestClient/WebClient customizer。 |
| `rest-client.take-over-existing-traceparent`、`web-client.take-over-existing-traceparent` | `false` | 覆盖调用方已设置的出站 `traceparent`。 |
| `reactor.automatic-context-propagation` | `true` | 存在 Micrometer `ContextRegistry` 时注册 `TraceThreadLocalAccessor`。 |

`servlet.slow-request-threshold`、`servlet.record-headers`、`servlet.record-request-body`、
`servlet.record-response-body`（含 `webflux` 同名项）可以正常绑定，但没有任何代码读取它们：
无论怎么配置，请求体、响应体和全量 Header 都不会被记录。MVC 拦截器的 `http_access`
只受 `servlet.enabled` 控制，不受 `access-log` 控制。

## 核心 API

```java
TraceContext child = TraceContext.currentOrCreate().child();
try (TraceContext.Scope ignored = child.open()) {
    log.info("calling downstream");
}

executor.execute(new TraceRouteRunnable() {
    @Override
    protected void doRun() {
        task.run();
    }
});
```

`TraceContext` 同时保存 Trace 字段和一份完整 MDC 快照，`TraceContext.Scope` 关闭时恢复
工作线程原 MDC。`TraceRouteRunnable`、`TraceRouteCallable`、`TraceRouteSupplier`
各自只保存一个捕获的 `TraceContext`。执行器适配由所属组件提供；DTP Starter 提供平台
线程、Spring 任务执行器和虚拟线程适配。当前 Starter 不会替换或后处理业务执行器 Bean。

## 自动配置边界

- 使用 `@AutoConfiguration` 和
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，
  不使用 `@ComponentScan`。
- Servlet 使用高优先级 `OncePerRequestFilter`，并在 `/**` 上注册 Spring MVC 的 `MyLogInterceptor`。
- WebFlux 以 Reactor Context 为真实上下文来源，MDC 只是当前线程日志投影：过滤器把上下文写入
  `TraceContext` 键，并在订阅线程上开启自身作用域。其 `trace_access` 行固定 `protocol=HTTP`
  且 `errorCode` 为空；开启 `trusted-proxy-headers` 后若没有 `X-Forwarded-For`，客户端 IP 为空，
  而 Servlet 过滤器会退回 socket 地址。
- RestClient 和 WebClient 出站创建 child span 并写入标准 Header。
- Yuheng 自研 Reactor Netty 数据面直接使用 `common-trace`，不套用 Spring Cloud
  Gateway Filter。
- RPC 直接在现有 `rpc-starter` 的 gRPC Interceptor 中接入，不另建通用 gRPC Trace
  Starter。
