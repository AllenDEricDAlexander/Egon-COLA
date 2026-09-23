# egon-cola-component-common-trace-spring-boot-starter

[English](README.md) | [中文](README.zh-CN.md)

## Overview

`egon-cola-component-common-trace-spring-boot-starter` is the common-aggregated
Spring integration for lightweight log correlation and protocol propagation. It builds on the pure JDK + SLF4J
`common-trace` module and adds Spring Boot auto-configuration for Servlet,
Spring MVC, WebFlux, RestClient, and WebClient.

The component does not provide log collection, log rotation, storage, a full
APM system, or a replacement OpenTelemetry SDK. Its scope is trace context,
MDC projection, and header propagation: the `traceparent` wire format follows W3C
Trace Context, which is what OpenTelemetry-speaking peers expect, and Reactor
cross-thread replay rides on Micrometer `context-propagation` rather than
Micrometer Observation.

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
| Neither read nor written | `x-egon-trace-id` |

Legacy `X-Trace-Id` and `x-trace-id` are read-only aliases used only when no valid
`traceparent` is present; `x-egon-trace-id` is neither read nor written. A missing
`x-egon-request-id` is replaced by a freshly generated identifier.

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

Log pattern example:

```text
%d %-5level [%X{traceId:-},%X{spanId:-},%X{requestId:-}] %logger - %msg%n
```

Request bodies, response bodies, full headers, authorization data, tokens, and
identity fields are never logged. Applications that extend access
logging should add masking, field allowlists, size limits, and content type
limits.

## Configuration

| Key | Default | Effect |
|---|---|---|
| `enabled` | `true` | Master switch for the whole auto-configuration. |
| `propagation.enabled` | `true` | When `false`, inbound headers are ignored and a root context is created. |
| `propagation.legacy-trace-id-read-only` | `true` | Accept `X-Trace-Id` / `x-trace-id` when no valid `traceparent` is present. |
| `propagation.response-headers` | `true` | Master switch for response headers; the stack switch must also be on. |
| `servlet.enabled` | `true` | Registers the filter, its registration, and the MVC log interceptor; each backs off to an existing bean. |
| `servlet.order` | `Integer.MIN_VALUE + 100` | `FilterRegistrationBean` order. |
| `servlet.response-headers` | `true` | Servlet-side response header switch. |
| `servlet.access-log` | `true` | Emits the `trace_access` line from the filter. |
| `servlet.excluded-paths` | empty | `PatternMatchUtils.simpleMatch` globs against the request URI; a match skips the filter entirely. |
| `servlet.record-query` | `false` | Append the query string to the logged path. |
| `servlet.trusted-proxy-headers` | `false` | Read the client IP from the first `X-Forwarded-For` entry instead of `getRemoteAddr()`. |
| `webflux.*` | as `servlet.*` | `WebFlux` extends `Servlet`, so every servlet key has a WebFlux twin. |
| `rest-client.enabled`, `web-client.enabled` | `true` | Register the matching `RestClient`/`WebClient` customizer. |
| `rest-client.take-over-existing-traceparent`, `web-client.take-over-existing-traceparent` | `false` | Overwrite an outbound `traceparent` the caller already set. |
| `reactor.automatic-context-propagation` | `true` | Register `TraceThreadLocalAccessor` when Micrometer `ContextRegistry` is present. |

`servlet.slow-request-threshold`, `servlet.record-headers`, `servlet.record-request-body`
and `servlet.record-response-body` (plus their `webflux` twins) bind without error but no
code reads them: request and response bodies and full headers are never logged, under any
setting. The MVC interceptor logs `http_access` whenever `servlet.enabled` is on; that line
is not gated by `access-log`.

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
- Servlet support uses a high-priority `OncePerRequestFilter` plus a Spring MVC
  `MyLogInterceptor` registered on `/**`.
- WebFlux treats Reactor Context as the source of truth and MDC as a
  thread-local log projection: the filter writes the context under the
  `TraceContext` key and opens its own scope on the subscribing thread.
  Its `trace_access` line fixes `protocol=HTTP` and leaves `errorCode` empty,
  and with `trusted-proxy-headers` on it reports no client IP when
  `X-Forwarded-For` is absent — unlike the servlet filter, which falls back to
  the socket address.
- RestClient and WebClient create child spans and write standard headers.
- The self-built Yuheng Reactor Netty data plane uses `common-trace`
  directly instead of Spring Cloud Gateway filters.
- RPC integrates in the existing `rpc-starter` gRPC interceptors; there is no
  separate generic gRPC trace starter.
