# egon-cola-component-common-log

[English](README.md) | 中文

`egon-cola-component-common-log` 提供轻量、受控的结构化业务日志 API。生产代码只依赖
`slf4j-api`，不依赖 Spring、Logback、Jackson、日志采集平台或 APM。

## 使用方式

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

固定字段包括 `biz`、`scene`、`step`、`phase`、`bill_type`、`bill_id`、
`biz_id`、`status`、`decision`、`error_code`、`cost_ms` 和 `msg`。API 不提供
任意字段 Map，避免业务身份、Token、手机号和设备信息被无约束写入公共日志结构。

字符串字段会被转换为单行并限制为 1024 个字符。异常通过
`log(String, Throwable)` 传入，保留日志框架原生堆栈。

Trace 字段不由 `common-log` 重复拼接。应用建立 `TraceScope` 后，SLF4J 日志事件会从
MDC 自动获取 `traceId`、`spanId` 和 `requestId`。Logback 可使用以下 Pattern：

```text
%d %-5level [%X{traceId:-},%X{spanId:-},%X{requestId:-}] %logger - %msg %kvp%n
```

`common-log` 不替换业务应用的 `logback.xml`，也不负责日志采集、滚动、存储或审计。
