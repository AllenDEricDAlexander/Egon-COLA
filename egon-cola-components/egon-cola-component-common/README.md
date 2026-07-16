# egon-cola-component-common

[English](README.md) | [中文](README.zh-CN.md)

## Overview

`egon-cola-component-common` is the enterprise-grade common-capability aggregator for the Egon COLA component ecosystem. It provides foundational capabilities including error codes, exceptions, request models, pagination models, response models, trace context, IDs, cryptographic encoding, data masking, tree construction, and test boundary assertions.

This directory is a `pom` aggregator, not a runtime JAR that business applications should depend on directly. Business applications should manage versions through `egon-cola-components-bom` and include only the specific submodules they need, avoiding unnecessary foundational capabilities in the application.

## Module Layout

| Module | Description |
|---|---|
| `egon-cola-component-common-core` | Common `int code` error statuses, exception base classes, business/system/validation/authorization exception types, and enum contracts |
| `egon-cola-component-common-model` | Request and pagination models such as `PageQuery`, `SortQuery`, `TimeRangeQuery`, `BaseRequest`, `OperatorContext`, `PageModel`, and `PageSlice` |
| `egon-cola-component-common-trace` | SLF4J MDC-based `traceId` context and snapshots |
| `egon-cola-component-common-result` | External response DTOs, internal service result Models, and their factory methods |
| `egon-cola-component-common-id` | UUIDv7 utilities and the `IdGenerator` abstraction |
| `egon-cola-component-common-crypto` | SHA-256, HMAC-SHA256, Base64, and Hex utilities |
| `egon-cola-component-common-mask` | Stable masking rules for mobile numbers, email addresses, and prefix/suffix retention |
| `egon-cola-component-common-structure` | General-purpose parent-child tree builder |
| `egon-cola-component-common-test` | Source dependency boundary test utilities used internally by components |

## Features

### Unified Error Statuses and Exceptions

`common-core` uses `CommonStatus` as its default error status set. Every status uses an `int code`, making it suitable for API responses, log searches, and cross-system transport. Applications can use exception types such as `EgonBusinessException`, `EgonValidationException`, and `EgonRemoteCallException` directly, or implement `ErrorStatus` to define their own error statuses.

### Request, Query, and Pagination Models

The primary contracts in `common-model` use Java records with stable JSON field names and ordering:

| Contract | Purpose |
|---|---|
| `PageQuery` | Normalizes page number and page size; page numbers start at 1, the default page size is 10, and the maximum page size is 500 |
| `SortQuery` | Optional sort field and `ASC` / `DESC` direction |
| `TimeRangeQuery` | Optional start/end time range |
| `BaseRequest` | Request metadata container |
| `OperatorContext` | Operator identity context |
| `PageModel` / `PageSlice` | Internal pagination structures whose record collections are defensively copied into immutable lists |

### Separate External DTOs and Internal Models

`common-result` distinguishes external API responses from internal application/service results:

| Scenario | Types |
|---|---|
| Controller responses | `ResultDto`, `PageResultDto`, `ErrorResultDto` |
| Internal application/domain service results | `ResultModel`, `PageResultModel`, `ErrorResultModel` |
| External response factory | `ResultDtos` |
| Internal result factory | `ResultModels` |

`ResultDtos` reads `TraceContext.getTraceId()` and writes the current MDC `traceId` into the response, linking API results to logs.

### Foundational Utilities

`common-id` provides UUIDv7 for generating roughly time-ordered business IDs. `common-crypto` provides stable UTF-8 encoding and digest methods. `common-mask` handles common field masking. `TreeBuilder` in `common-structure` converts flat nodes into parent-child trees.

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
        <artifactId>egon-cola-component-common-model</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common-result</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common-trace</artifactId>
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
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common-structure</artifactId>
    </dependency>
</dependencies>
```

## Complete Usage Example

The following example shows a Controller that queries a list of orders. It uses `PageQuery` to normalize pagination parameters, `ResultDtos` for external responses, `TraceContext` to include a trace ID in the response, `UuidV7` to generate a business ID, and `Masking` plus `Hmacs` for display and signing:

```java
package demo.order;

import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.crypto.hmac.Hmacs;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.common.mask.Masking;
import top.egon.cola.component.common.model.query.PageQuery;
import top.egon.cola.component.common.result.dto.PageResultDto;
import top.egon.cola.component.common.result.dto.ResultDto;
import top.egon.cola.component.common.result.factory.ResultDtos;
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
    public PageResultDto<OrderView> list(OrderListQuery query) {
        TraceContext.setTraceId(MDC.get("traceId"));
        PageQuery page = new PageQuery(query.pageNo(), query.pageSize());

        List<OrderView> records = queryService.list(page.offset(), page.pageSize())
                .stream()
                .map(OrderView::from)
                .toList();

        return ResultDtos.page(records, queryService.count(), page.pageNo(), page.pageSize());
    }

    @GetMapping("/new-id")
    public ResultDto<NewOrderIdView> newOrderId() {
        String orderId = UuidV7.simpleString();
        String signature = Hmacs.sha256Hex(orderId, "demo-secret");
        return ResultDtos.success(new NewOrderIdView(orderId, signature));
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

The associated service can return an internal model, allowing the Controller to decide whether to convert it into a DTO:

```java
package demo.order;

import top.egon.cola.component.common.result.factory.ResultModels;
import top.egon.cola.component.common.result.model.PageResultModel;

import java.util.List;

public class OrderQueryService {

    public List<OrderRecord> list(int offset, int pageSize) {
        return List.of(new OrderRecord("O-1001", "13800138000"));
    }

    public long count() {
        return 1L;
    }

    public PageResultModel<OrderRecord> page(int pageNo, int pageSize) {
        List<OrderRecord> records = list(Math.max(pageNo - 1, 0) * pageSize, pageSize);
        return ResultModels.page(records, count(), pageNo, pageSize);
    }
}

record OrderRecord(String orderId, String buyerMobile) {
}
```

Tree construction example:

```java
import top.egon.cola.component.common.structure.tree.TreeBuilder;
import top.egon.cola.component.common.structure.tree.TreeNode;

import java.util.List;

List<TreeNode<Long, String>> nodes = List.of(
        new TreeNode<>(1L, null, "Headquarters"),
        new TreeNode<>(2L, 1L, "East China"),
        new TreeNode<>(3L, 2L, "Shanghai")
);

List<TreeNode<Long, String>> roots = TreeBuilder.build(nodes);
```

## Design Principles and Implementation Details

### Design Principles

1. Split by capability instead of creating an all-inclusive common JAR. Applications include only the modules they need.
2. Separate external DTOs from internal Models so that Controller response formats do not constrain internal call results.
3. Prefer Java records for common contracts to preserve immutability, serializability, and stable field ordering.
4. Keep `common-core` free of Spring and Jackson dependencies to reduce the cost of sharing foundational error codes and exceptions.
5. Retain only stable, explicit, low-intrusion utility functions without introducing business semantics.

### Implementation Details

- `PageQuery` normalizes page number and page size during construction, and `offset()` calculates a database offset from the normalized values.
- `PageModel` copies the supplied records and wraps them in an immutable list so callers cannot modify the pagination result.
- `ResultDtos.success` and `ResultDtos.page` read `TraceContext` and include the MDC `traceId` in the response.
- `UuidV7` is based on a time-ordered epoch UUID and suits IDs that benefit from log ordering and better index locality.
- `Masking.mobile` keeps the first three and last four digits of a standard mobile number; short strings use the prefix/suffix retention rule.
- `TreeBuilder` uses `LinkedHashMap` to preserve input order and, by default, retains orphan nodes as roots.
- `SourceBoundaryAssert` is located in `common-test`. It is intended for component-internal source boundary tests and should not be used as a business runtime dependency.

## Migration Notes

| Old API | New API |
|---|---|
| `top.egon.cola.component.common.result.Result` | `ResultDto` or `ResultModel` |
| `top.egon.cola.component.common.result.PageResult` | `PageResultDto` or `PageResultModel` |
| `top.egon.cola.component.common.exception.BusinessException` | `EgonBusinessException` |
| `top.egon.cola.component.common.exception.SystemException` | `EgonSystemException` |
| `top.egon.cola.component.common.exception.ErrorCodes` | `CommonStatus` |
| `top.egon.cola.component.common.util.IdUtils` | `UuidV7` or `UuidV7Generator` |
| `top.egon.cola.component.common.util.CryptoUtils` | `Digests`, `Hmacs`, `Base64s`, `Hexes` |
| `top.egon.cola.component.common.util.MaskingUtils` | `Masking` |
| `top.egon.cola.component.common.util.TreeUtils` | `TreeBuilder` |

The legacy aggregated `util` package, JavaBean-style common contracts, `BaseEntity`, and `AuditableModel` were intentionally removed.

## Validation Command

```bash
./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml test
```
