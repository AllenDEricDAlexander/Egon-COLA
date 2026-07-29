# egon-cola-component-common-log

[English](README.md) | [中文](README.zh-CN.md)

`egon-cola-component-common-log` provides a small, controlled structured
business logging API. Production code depends only on `slf4j-api`; it does not
depend on Spring, Logback, Jackson, a log collection platform, or an APM SDK.

## Usage

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-common-log</artifactId>
</dependency>
```

```java
BizLog.info(LOG)
        .biz("order")
        .scene("create")
        .step("persist")
        .phase("END")
        .billType("ORDER")
        .billId(orderId)
        .bizId(orderId)
        .status("SUCCESS")
        .decision("ALLOW")
        .costMs(costMs)
        .log("order created");
```

The fixed schema contains `biz`, `scene`, `step`, `phase`, `bill_type`,
`bill_id`, `biz_id`, `status`, `decision`, `error_code`, `cost_ms`, and `msg`.
There is no arbitrary field Map API, which keeps identity data, tokens, phone
numbers, and device data out of the shared schema by default.

String values are normalized to one line and limited to 1024 characters. Pass
exceptions through `log(String, Throwable)` to preserve the logging backend's
native stack trace handling.

`common-log` does not duplicate trace fields. Once an application opens a
`TraceScope`, SLF4J log events obtain `traceId`, `spanId`, and `requestId` from
MDC. A Logback pattern can render both MDC and SLF4J key-value pairs:

```text
%d %-5level [%X{traceId:-},%X{spanId:-},%X{requestId:-}] %logger - %msg %kvp%n
```

The module does not replace an application's `logback.xml` and does not provide
log collection, rotation, storage, or request auditing.
