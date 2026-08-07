# egon-cola-component-common-trace-spring-boot-starter

[English](README.md) | 中文

## 简要介绍

`egon-cola-component-common-trace-spring-boot-starter` 是 common 聚合内的日志关联和跨协议 Trace 传播 Starter。它在
`common-trace` 的纯 JDK + SLF4J 核心之上提供 Spring Boot 自动配置，覆盖
Servlet/Spring MVC、WebFlux、RestClient 和 WebClient。

该组件不建设日志采集平台、日志文件滚动、APM，也不替代 Micrometer Observation 或
OpenTelemetry SDK。它只负责统一 Trace Context、MDC 投影和跨协议 Header 传播。

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
| 不再传播 | `x-egon-trace-id`、`x-trace-id`、`X-Trace-Id` |

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
          record-request-body: false
          record-response-body: false
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

默认不会记录请求体、响应体、全量 Header、Authorization、Token 或身份字段。需要扩展
访问日志时，应在业务侧提供脱敏、字段 allowlist、大小限制和 Content-Type 限制。

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
- Servlet 使用高优先级 `OncePerRequestFilter`。
- WebFlux 以 Reactor Context 为真实上下文来源，MDC 只是当前线程日志投影。
- RestClient 和 WebClient 出站创建 child span 并写入标准 Header。
- Gateway 自研 Reactor Netty 数据面直接使用 `common-trace`，不套用 Spring Cloud
  Gateway Filter。
- RPC 直接在现有 `rpc-starter` 的 gRPC Interceptor 中接入，不另建通用 gRPC Trace
  Starter。
