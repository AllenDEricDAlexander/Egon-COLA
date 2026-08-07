# egon-cola-component-common

[English](README.md) | [中文](README.zh-CN.md)

## Overview

`egon-cola-component-common` is the common-capability aggregator for the Egon COLA component ecosystem. It provides stable contracts for result records, page metadata, request/query POJOs, enum codes, exceptions, tree construction, converters, structured business logs, trace core and Spring integration, IDs, crypto, response/log desensitization, and source boundary assertions.

This directory is a `pom` aggregator, not a runtime JAR that business applications should depend on directly. Business applications should manage versions through `egon-cola-components-bom` and include only the runtime modules they need. `common-core` owns stable contracts, `common-trace` owns the framework-neutral Trace Context and `CommonLogUtil`, and the Trace Spring Boot Starter owns web and client auto-configuration.

## Module Layout

| Module | Description |
|---|---|
| `egon-cola-component-common-core` | `ResultCode`, common exceptions, converter contracts, POJO records, and tree construction |
| `egon-cola-component-common-trace` | Pure JDK + SLF4J trace core, W3C `traceparent` propagation, and MDC-aware `CommonLogUtil` business logs |
| `egon-cola-component-common-trace-spring-boot-starter` | Spring Boot 3 auto-configuration for Servlet, WebFlux, RestClient, WebClient, and Reactor context projection |
| `egon-cola-component-common-id-starter` | Snowflake interfaces, pure-JDK algorithm, parser, deprecated UUIDv7 compatibility APIs, and Spring Boot auto-configuration; all tests live in this module |
| `egon-cola-component-common-crypto` | SHA-256, HMAC-SHA256, Base64, and Hex utilities |
| `egon-cola-component-common-data-desensitize-spring-boot-starter` | `@Sensitive` metadata, shared masking strategies, Jackson response masking, Logback message conversion, and Spring Boot auto-configuration |
| `egon-cola-component-common-test` | Source dependency boundary test utilities used internally by components |

## Features

### Result Codes and Exceptions

`common-core` uses `ResultCode` as its default result code set. Every code is an `int` and implements `ErrorStatus`, making it suitable for API responses, log searches, and cross-system transport. Applications can use exception types such as `BusinessException`, `ValidationException`, and `RemoteCallException`, or implement `ErrorStatus` to define their own error statuses.

Exception classes in common-core intentionally do not use an `Egon` prefix.

### POJO Records

The main contracts in `common-core` use Java records with stable Jackson field names and field ordering:

| Contract | Purpose |
|---|---|
| `ResultRecord<T>` | Unified single-object response, including `success`, `code`, `status`, `message`, `data`, `traceId`, and `timestamp` |
| `PageResultRecord<T>` | Unified page response, including `records` plus a composed `PageMetaRecord` |
| `PageMetaRecord` | Page metadata: `total`, `pageNo`, `pageSize`, `pages`, `hasNext`, and `hasPrevious` |
| `PageQuery` | Normalizes page number and page size; page numbers start at 1, default page size is 10, and maximum page size is 500 |
| `SortQuery` | Optional sort field and `ASC` / `DESC` direction |
| `BaseRequest` | Request metadata container |
| `OperatorContext` | Operator identity context |
| `PageSlice<T>` | Slice pagination without a total count |
| `TreeBuilder`, `TreeNode`, `TreeOptions` | Flat node to parent-child tree construction |

`ResultRecord` and `PageResultRecord` expose static factory methods directly. There is no separate result factory class.

### Converter Contract

`BaseConverter<S, T>` defines `toTarget`, `toSource`, list conversion, and simple `Date` / `String` mapping defaults. MapStruct and MapStruct Plus examples live in the `common-core` test package so production code exposes only the lightweight contract.

### Structured Business Logs

`common-trace` exposes one top-level logging API, `CommonLogUtil`, with a nested
business-log builder. `bizDebug`, `bizInfo`, `bizWarn`, and `bizError` support
stable business fields, result helpers, elapsed time, and throwable logging.
Every terminal log call captures the complete current MDC and renders it before
the business fields in the final single-line message, so correlation does not
depend on a logging pattern containing `%X`, `%mdc`, or `%kvp`.

```java
CommonLogUtil.bizInfo(LOG)
        .biz("order")
        .scene("create")
        .success("order created");
```

### HTTP Response and Log Desensitization

The data desensitization Starter auto-registers `SensitiveJacksonModule`. Annotated String
fields and accessor methods are masked during JSON serialization without changing the source
object. `RESPONSE` and `LOG` are enabled by default and can be selected independently through
`Sensitive.scenes`.

A business-defined `SensitiveStrategy` Spring Bean overrides the built-in strategy with the same
`SensitiveType`. The resulting registry is shared with Jackson and the active Logback context.

For Logback, register `SensitiveLogConverter` and replace `%msg` with `%sensitiveMsg`; keeping
both conversion words in one pattern would still emit the raw formatted message.

```xml
<conversionRule conversionWord="sensitiveMsg"
                converterClass="top.egon.cola.component.common.desensitize.logback.SensitiveLogConverter"/>
<property name="CONSOLE_LOG_PATTERN"
          value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger - %sensitiveMsg%n"/>
```

An object argument such as `log.info("user={}", user)` is inspected for `@Sensitive` fields and
accessors. A scalar String no longer carries field metadata, so mask it explicitly:

```java
log.info("mobile={}", SensitiveLogs.of(mobile, SensitiveType.MOBILE));
```

### Async Trace Propagation

`common-trace` provides `TraceRouteRunnable`, `TraceRouteCallable<T>`, and
`TraceRouteSupplier<T>`. Each wrapper captures the current `TraceSnapshot` when
it is created, restores Trace and MDC before execution, and restores the worker
thread's original context after completion or failure.

```java
executor.execute(new TraceRouteRunnable() {
    @Override
    protected void doRun() {
        orderService.refresh();
    }
});
```

Functional code can continue using `TraceContext.snapshot().wrap(task)`; both
entry points use the same `TraceScope` restoration mechanism.

## Dependency Setup

First import the component BOM:

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
```

Then include the specific modules you need:

```xml
<dependencies>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common-core</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common-trace</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common-trace-spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common-id-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common-crypto</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common-data-desensitize-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

## Usage Example

The following example shows a Controller that queries a list of orders. It uses `PageQuery` to normalize pagination, `PageResultRecord` and `ResultRecord` for responses, an injected `LongIdGenerator` for database IDs, `@Sensitive` for serialization-time response masking, and `Hmacs` for signing:

```java
package demo.order;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.crypto.hmac.Hmacs;
import top.egon.cola.component.common.desensitize.annotation.Sensitive;
import top.egon.cola.component.common.desensitize.annotation.SensitiveType;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.pojo.PageQuery;
import top.egon.cola.component.common.pojo.PageResultRecord;
import top.egon.cola.component.common.pojo.ResultRecord;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderQueryService queryService;
    private final LongIdGenerator idGenerator;

    public OrderController(OrderQueryService queryService, LongIdGenerator idGenerator) {
        this.queryService = queryService;
        this.idGenerator = idGenerator;
    }

    @GetMapping
    public PageResultRecord<OrderView> list(OrderListQuery query) {
        PageQuery page = new PageQuery(query.pageNo(), query.pageSize());

        List<OrderView> records = queryService.list(page.offset(), page.pageSize())
                .stream()
                .map(OrderView::from)
                .toList();

        return PageResultRecord.success(records, queryService.count(), page.pageNo(), page.pageSize());
    }

    @GetMapping("/new-id")
    public ResultRecord<NewOrderIdView> newOrderId() {
        long orderId = idGenerator.nextLongId();
        String signature = Hmacs.sha256Hex(Long.toString(orderId), "demo-secret");
        return ResultRecord.success(new NewOrderIdView(orderId, signature));
    }

    public record OrderListQuery(int pageNo, int pageSize) {
    }

    public record OrderView(String orderId, String buyerMobile) {
        @Sensitive(type = SensitiveType.MOBILE)
        public String buyerMobile() {
            return buyerMobile;
        }

        static OrderView from(OrderRecord record) {
            return new OrderView(record.orderId(), record.buyerMobile());
        }
    }

    public record NewOrderIdView(long orderId, String signature) {
    }
}
```

Tree construction example:

```java
import top.egon.cola.component.common.pojo.TreeBuilder;
import top.egon.cola.component.common.pojo.TreeNode;

import java.util.List;

List<TreeNode<Long, String>> nodes = List.of(
        new TreeNode<>(1L, null, "Headquarters"),
        new TreeNode<>(2L, 1L, "East China"),
        new TreeNode<>(3L, 2L, "Shanghai")
);

List<TreeNode<Long, String>> roots = TreeBuilder.build(nodes);
```

## Design Principles

1. Keep stable common contracts in `common-core` so consumers do not compose several tiny semantic JARs for basic result/page/query/tree usage.
2. Prefer Java records for common PO contracts to preserve immutability, serializability, and stable JSON field ordering.
3. Use direct record factory methods instead of separate `ResultDtos` or `ResultModels` classes.
4. Keep `common-core` free of Spring runtime dependencies; Jackson annotations are explicit lightweight dependencies because core owns JSON contracts.
5. Keep `common-trace` limited to the JDK and `slf4j-api`; Trace propagation and `CommonLogUtil` do not depend on Spring, Servlet, WebFlux, Reactor, gRPC, Gateway, Jackson, or a Logback implementation.
6. Render MDC directly in `CommonLogUtil` messages so business-log correlation does not depend on backend-specific patterns.
7. Keep the Trace Spring Boot Starter in the same common aggregator without leaking Spring dependencies into `common-trace`.
8. Expose converter contracts, not generated converter implementations. MapStruct and MapStruct Plus implementations belong in consumers or tests.

## Implementation Details

- `ResultRecord.success` and `PageResultRecord.success` read `TraceContext` and include the current `traceId`; trace context is provided by `common-trace`.
- `ResultRecord` and `PageResultRecord` retain the stable `status` field while using `ResultCode` as the default source of `code`, `status`, and `message`.
- `PageResultRecord` composes `PageMetaRecord`; page metadata is not flattened into the result record.
- `PageResultRecord` and `PageSlice` defensively copy supplied records and expose immutable lists.
- `PageQuery` normalizes page number and page size during construction, and `offset()` calculates a database offset from the normalized values.
- `TreeBuilder` uses `LinkedHashMap` to preserve input order and, by default, retains orphan nodes as roots.
- `SourceBoundaryAssert` is located in `common-test`. It is intended for component-internal source boundary tests and should not be used as a business runtime dependency.

## Migration Notes

| Old API | New API |
|---|---|
| `CommonStatus` | `ResultCode` |
| Old prefixed common exceptions | `BusinessException`, `ValidationException`, `RemoteCallException` |
| `ResultDto`, `ResultModel` | `ResultRecord` |
| `PageResultDto`, `PageResultModel` | `PageResultRecord` |
| `ResultDtos`, `ResultModels` | Static factory methods on `ResultRecord` and `PageResultRecord` |
| `PageMeta` | `PageMetaRecord` |
| `PageModel` | `PageResultRecord` for response pages, or `PageSlice` for slice-only data |
| `top.egon.cola.component.common.model.*` | `top.egon.cola.component.common.pojo.*` |
| `top.egon.cola.component.common.result.*` | `top.egon.cola.component.common.pojo.*` |
| `top.egon.cola.component.common.structure.tree.*` | `top.egon.cola.component.common.pojo.*` |
| `top.egon.cola.component.common.util.IdUtils` | `LongIdGenerator` / `SnowflakeIdGenerator`; use deprecated `UuidV7` only for UUID compatibility contracts |
| `top.egon.cola.component.common.util.CryptoUtils` | `Digests`, `Hmacs`, `Base64s`, `Hexes` |
| `egon-cola-component-common-mask` | `egon-cola-component-common-data-desensitize-spring-boot-starter` |
| `top.egon.cola.component.common.util.MaskingUtils`, `top.egon.cola.component.common.mask.Masking` | `@Sensitive`, `SensitiveStrategyRegistry`, or `SensitiveLogs.of` for scalar log arguments |

The legacy aggregated `util` package, split `model/result/structure` packages, separate result factories, `BaseEntity`, and `AuditableModel` were intentionally removed.

For Snowflake layout, configuration, rollback behavior, Kubernetes machine-ID allocation, and UUIDv7 migration boundaries, see the [common ID Starter README](egon-cola-component-common-id-starter/README.md).

## Validation Command

```bash
./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml test
```
