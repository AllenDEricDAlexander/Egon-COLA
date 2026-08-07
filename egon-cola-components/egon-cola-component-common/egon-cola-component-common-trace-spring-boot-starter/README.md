# egon-cola-component-common-trace-spring-boot-starter

[English](README.md) | [中文](README.zh-CN.md)

## Overview

`egon-cola-component-common-trace-spring-boot-starter` is the common-aggregated
Spring integration for lightweight log correlation and protocol propagation. It builds on the pure JDK + SLF4J
`common-trace` module and adds Spring Boot auto-configuration for Servlet,
Spring MVC, WebFlux, RestClient, and WebClient.

The component does not provide log collection, log rotation, storage, a full
APM system, or a replacement OpenTelemetry SDK. Its scope is trace context,
MDC projection, and header propagation compatible with Micrometer Observation
and OpenTelemetry.

## Modules

| Module | Responsibility |
|---|---|
| `egon-cola-component-common-trace` | Pure core: one complete `TraceContext`, W3C propagation, MDC capture, and three local-thread task templates |
| `egon-cola-component-common-trace-spring-boot-starter` | Spring Boot 3 auto-configuration for Servlet, WebFlux, RestClient, WebClient, and Reactor context projection |

## Protocol

W3C Trace Context is the primary protocol:

| Direction | Headers |
|---|---|
| Inbound read | `traceparent`, `tracestate`, `x-egon-request-id` |
| Compatibility read | `X-Trace-Id` is read-only by default; valid `traceparent` wins |
| Outbound write | `traceparent`, `tracestate`, `x-egon-request-id` |
| Not propagated | `x-egon-trace-id`, `x-trace-id`, `X-Trace-Id` |

`traceId` identifies the whole trace, `spanId` identifies the current unit of
work, `parentSpanId` identifies the upstream span, and `requestId` is a request
troubleshooting ID. `traceFlags`, `tracestate`, `sourceApp`, and
`sourceInstance` are stored on the same `TraceContext`. Identity or sensitive
fields such as user IDs, account IDs, tokens, phone numbers, and device
details are not part of the core trace context. Future baggage must use an
explicit allowlist.

## Spring Boot Usage

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

Log pattern example:

```text
%d %-5level [%X{traceId:-},%X{spanId:-},%X{requestId:-}] %logger - %msg%n
```

Request bodies, response bodies, full headers, authorization data, tokens, and
identity fields are not logged by default. Applications that extend access
logging should add masking, field allowlists, size limits, and content type
limits.

## Core API

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

`TraceContext` contains the trace fields and one complete MDC snapshot.
`TraceContext.Scope` restores the worker thread's previous MDC when closed.
`TraceRouteRunnable`, `TraceRouteCallable`, and `TraceRouteSupplier` each store
one captured `TraceContext`. Executor-specific adapters are supplied by the
owning component; the DTP starter provides adapters for platform threads,
Spring task executors, and virtual threads. This starter does not replace or
post-process application executor beans.

## Auto-Configuration Boundaries

- Uses `@AutoConfiguration` and
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`;
  no component scan.
- Servlet support uses a high-priority `OncePerRequestFilter`.
- WebFlux treats Reactor Context as the source of truth and MDC as a
  thread-local log projection.
- RestClient and WebClient create child spans and write standard headers.
- The self-built Gateway Reactor Netty data plane uses `common-trace`
  directly instead of Spring Cloud Gateway filters.
- RPC integrates in the existing `rpc-starter` gRPC interceptors; there is no
  separate generic gRPC trace starter.
