# egon-cola-component-common

[English](README.md) | [中文](README.zh-CN.md)

## Overview

`egon-cola-component-common` is the common-capability aggregator for the Egon COLA component ecosystem. It provides stable common contracts for result records, page metadata, request/query POJOs, enum codes, exceptions, trace context, tree construction, converter contracts, IDs, crypto, masking, and source boundary assertions.

This directory is a `pom` aggregator, not a runtime JAR that business applications should depend on directly. Business applications should manage versions through `egon-cola-components-bom` and include the runtime module they need. `common-core` owns the stable common contracts, while ID, crypto, and masking remain separate utility modules.

## Module Layout

| Module | Description |
|---|---|
| `egon-cola-component-common-core` | `ResultCode`, common exceptions, converter contracts, POJO records, trace context, and tree construction |
| `egon-cola-component-common-id` | UUIDv7 utilities and the `IdGenerator` abstraction |
| `egon-cola-component-common-crypto` | SHA-256, HMAC-SHA256, Base64, and Hex utilities |
| `egon-cola-component-common-mask` | Stable masking rules for mobile numbers, email addresses, and prefix/suffix retention |
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
        <artifactId>egon-cola-component-common-id</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common-crypto</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common-mask</artifactId>
    </dependency>
</dependencies>
```

## Usage Example

The following example shows a Controller that queries a list of orders. It uses `PageQuery` to normalize pagination, `PageResultRecord` and `ResultRecord` for responses, `TraceContext` for response trace IDs, `UuidV7` for business IDs, and `Masking` plus `Hmacs` for display and signing:

```java
package demo.order;

import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.crypto.hmac.Hmacs;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.common.mask.Masking;
import top.egon.cola.component.common.pojo.PageQuery;
import top.egon.cola.component.common.pojo.PageResultRecord;
import top.egon.cola.component.common.pojo.ResultRecord;
import top.egon.cola.component.common.trace.TraceContext;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderQueryService queryService;

    public OrderController(OrderQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public PageResultRecord<OrderView> list(OrderListQuery query) {
        TraceContext.setTraceId(MDC.get("traceId"));
        PageQuery page = new PageQuery(query.pageNo(), query.pageSize());

        List<OrderView> records = queryService.list(page.offset(), page.pageSize())
                .stream()
                .map(OrderView::from)
                .toList();

        return PageResultRecord.success(records, queryService.count(), page.pageNo(), page.pageSize());
    }

    @GetMapping("/new-id")
    public ResultRecord<NewOrderIdView> newOrderId() {
        String orderId = UuidV7.simpleString();
        String signature = Hmacs.sha256Hex(orderId, "demo-secret");
        return ResultRecord.success(new NewOrderIdView(orderId, signature));
    }

    public record OrderListQuery(int pageNo, int pageSize) {
    }

    public record OrderView(String orderId, String buyerMobile) {
        static OrderView from(OrderRecord record) {
            return new OrderView(record.orderId(), Masking.mobile(record.buyerMobile()));
        }
    }

    public record NewOrderIdView(String orderId, String signature) {
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

1. Keep stable common contracts in `common-core` so consumers do not compose several tiny semantic JARs for basic result/page/query/trace/tree usage.
2. Prefer Java records for common PO contracts to preserve immutability, serializability, and stable JSON field ordering.
3. Use direct record factory methods instead of separate `ResultDtos` or `ResultModels` classes.
4. Keep `common-core` free of Spring runtime dependencies; Jackson annotations and SLF4J are explicit lightweight dependencies because core owns JSON contracts and trace context.
5. Expose converter contracts, not generated converter implementations. MapStruct and MapStruct Plus implementations belong in consumers or tests.

## Implementation Details

- `ResultRecord.success` and `PageResultRecord.success` read `TraceContext` and include the current `traceId`.
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
| `top.egon.cola.component.common.util.IdUtils` | `UuidV7` or `UuidV7Generator` |
| `top.egon.cola.component.common.util.CryptoUtils` | `Digests`, `Hmacs`, `Base64s`, `Hexes` |
| `top.egon.cola.component.common.util.MaskingUtils` | `Masking` |

The legacy aggregated `util` package, split `model/result/structure` packages, separate result factories, `BaseEntity`, and `AuditableModel` were intentionally removed.

## Validation Command

```bash
./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml test
```
