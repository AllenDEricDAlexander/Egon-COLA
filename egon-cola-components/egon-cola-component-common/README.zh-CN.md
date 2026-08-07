# egon-cola-component-common

[English](README.md) | 中文

## 简要介绍

`egon-cola-component-common` 是 Egon COLA 组件体系的通用能力聚合模块，提供结果 record、分页元数据、请求/查询 PO、枚举错误码、异常、树结构构建、转换器契约、Trace 核心与 Spring 自动装配、ID、加密、脱敏和源码边界断言等基础能力。

这个目录本身是 `pom` 聚合模块，不是业务应用应该直接依赖的运行时 Jar。业务侧应通过 `egon-cola-components-bom` 管理版本，然后引入需要的运行时模块。`common-core` 负责稳定通用契约，`common-trace` 负责框架无关的 `TraceContext` 和本地线程任务模板，Trace Spring Boot Starter 负责 Web 与客户端自动装配。

## 模块结构

| Module | 说明 |
|---|---|
| `egon-cola-component-common-core` | `ResultCode`、通用异常、转换器契约、POJO record 和树结构构建 |
| `egon-cola-component-common-trace` | 纯 JDK + SLF4J Trace 核心、W3C `traceparent` 传播、完整 MDC 捕获和本地线程任务模板 |
| `egon-cola-component-common-trace-spring-boot-starter` | Spring Boot 3 自动配置：Servlet、WebFlux、RestClient、WebClient 和 Reactor Context 投影 |
| `egon-cola-component-common-id-starter` | Snowflake 接口、纯 JDK 算法、解析器、已废弃的 UUIDv7 兼容 API 和 Spring Boot 自动配置；全部测试位于本模块 |
| `egon-cola-component-common-crypto` | SHA-256、HMAC-SHA256、Base64、Hex 工具 |
| `egon-cola-component-common-data-desensitize-spring-boot-starter` | `@Sensitive` 元数据、共享脱敏策略、Jackson 响应脱敏、Logback 消息转换和 Spring Boot 自动配置 |
| `egon-cola-component-common-test` | 组件内部使用的源码依赖边界测试工具 |

## 功能说明

### 统一错误码和异常

`common-core` 以 `ResultCode` 作为默认结果码集合。所有结果码都是 `int`，并实现 `ErrorStatus`，适合 API 响应、日志检索和跨系统传递。业务可以直接使用 `BusinessException`、`ValidationException`、`RemoteCallException` 等异常类型，也可以实现 `ErrorStatus` 扩展自己的错误状态。

common-core 的异常类名不再使用 `Egon` 前缀。

### POJO Record

`common-core` 中的主要契约使用 Java record，并保留稳定 Jackson 字段名和字段顺序：

| 契约 | 用途 |
|---|---|
| `ResultRecord<T>` | 单对象统一响应，包含 `success`、`code`、`status`、`message`、`data`、`traceId`、`timestamp` |
| `PageResultRecord<T>` | 分页统一响应，包含 `records`，并组合 `PageMetaRecord` |
| `PageMetaRecord` | 分页元数据：`total`、`pageNo`、`pageSize`、`pages`、`hasNext`、`hasPrevious` |
| `PageQuery` | 归一化页码和页大小，页码从 1 开始，默认页大小 10，最大页大小 500 |
| `SortQuery` | 可选排序字段和 `ASC` / `DESC` 排序方向 |
| `BaseRequest` | 请求元数据容器 |
| `OperatorContext` | 操作人身份上下文 |
| `PageSlice<T>` | 不带总数的切片分页 |
| `TreeBuilder`、`TreeNode`、`TreeOptions` | 平铺节点到父子树结构构建 |

`ResultRecord` 和 `PageResultRecord` 直接提供静态工厂方法，不再额外提供结果工厂类。

### Converter 契约

`BaseConverter<S, T>` 定义 `toTarget`、`toSource`、列表转换，以及简单的 `Date` / `String` 默认转换。MapStruct 和 MapStruct Plus 示例放在 `common-core` 的 test 包下，生产代码只暴露轻量契约。

### HTTP 响应与日志脱敏

数据脱敏 Starter 会自动注册 `SensitiveJacksonModule`。String 字段或 accessor 方法声明
`@Sensitive` 后，JSON 序列化阶段输出脱敏值，但不会修改原业务对象。默认同时作用于
`RESPONSE` 和 `LOG`，也可以通过 `Sensitive.scenes` 分场景启用。

业务声明的 `SensitiveStrategy` Spring Bean 会按相同 `SensitiveType` 覆盖内置策略，
生成的 registry 同时提供给 Jackson 和当前 Logback Context。

Logback 需要注册 `SensitiveLogConverter`，并用 `%sensitiveMsg` 替换 `%msg`；同一个
Pattern 同时保留两者仍会输出未脱敏的原始消息。

```xml
<conversionRule conversionWord="sensitiveMsg"
                converterClass="top.egon.cola.component.common.desensitize.logback.SensitiveLogConverter"/>
<property name="CONSOLE_LOG_PATTERN"
          value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger - %sensitiveMsg%n"/>
```

`log.info("user={}", user)` 这类对象参数会解析字段和 accessor 上的 `@Sensitive`。
单独传入的 String 已经没有字段元数据，需要显式脱敏：

```java
log.info("mobile={}", SensitiveLogs.of(mobile, SensitiveType.MOBILE));
```

### 异步任务 Trace 传播

`common-trace` 提供 `TraceRouteRunnable`、`TraceRouteCallable<T>` 和
`TraceRouteSupplier<T>`。每个模板在创建时只保存一个 `TraceContext`，任务执行前恢复
其中的完整 MDC，执行完成或抛出异常后恢复工作线程原 MDC。

```java
executor.execute(new TraceRouteRunnable() {
    @Override
    protected void doRun() {
        orderService.refresh();
    }
});
```

执行器相关适配保留在各自组件中。例如，动态线程池 Starter 基于这三个模板提供
`DtpRunnable`、`DtpCallable`、`DtpSupplier`、`DtpContextAwareExecutorService`、
`DtpTaskDecorator` 和 `DtpThreads`。

## 依赖方式

先导入组件 BOM：

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

再按需引入具体模块：

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

## 使用示例

下面示例展示一个查询订单列表的 Controller：它使用 `PageQuery` 归一化分页参数，用 `PageResultRecord` 和 `ResultRecord` 输出响应，注入 `LongIdGenerator` 生成数据库 ID，用 `@Sensitive` 在序列化阶段完成响应脱敏，并用 `Hmacs` 处理签名。

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

树结构构建示例：

```java
import top.egon.cola.component.common.pojo.TreeBuilder;
import top.egon.cola.component.common.pojo.TreeNode;

import java.util.List;

List<TreeNode<Long, String>> nodes = List.of(
        new TreeNode<>(1L, null, "总部"),
        new TreeNode<>(2L, 1L, "华东区"),
        new TreeNode<>(3L, 2L, "上海")
);

List<TreeNode<Long, String>> roots = TreeBuilder.build(nodes);
```

## 设计思想

1. 稳定通用契约收敛进 `common-core`，业务方不需要为 result/page/query/tree 这类基础语义组合多个小 Jar。
2. 公共 PO 契约优先使用 Java record，保持不可变、可序列化、JSON 字段顺序稳定。
3. 用 record 自身的静态工厂方法替代独立的 `ResultDtos` 或 `ResultModels` 工厂类。
4. `common-core` 保持无 Spring 运行时依赖；Jackson annotation 是显式轻量依赖，因为 core 负责 JSON 契约。
5. `common-trace` 只依赖 JDK 和 `slf4j-api`；Trace 传播不依赖 Spring、Servlet、WebFlux、Reactor、gRPC、Gateway、Jackson 或 Logback 实现。
6. 执行器适配保留在所属组件内；`common-trace` 只提供 `TraceContext` 和三个本地线程任务模板。
7. Trace Spring Boot Starter 与 Trace Core 同属 common 聚合，但 Spring 依赖不会进入 `common-trace`。
8. 只暴露 converter 契约，不在生产代码里提供生成式 converter 实现。MapStruct 和 MapStruct Plus 实现由业务侧或测试示例承载。

## 实现细节

- `ResultRecord.success` 和 `PageResultRecord.success` 会读取 `TraceContext` 并带上当前 `traceId`；Trace 上下文能力由 `common-trace` 提供。
- `ResultRecord` 和 `PageResultRecord` 保留稳定的 `status` 字段，默认由 `ResultCode` 提供 `code`、`status` 和 `message`。
- `PageResultRecord` 组合 `PageMetaRecord`，分页元数据不再平铺进结果 record。
- `PageResultRecord` 和 `PageSlice` 会防御性复制 records，并暴露不可变列表。
- `PageQuery` 在构造时完成页码和页大小归一化，`offset()` 根据归一化后的值计算数据库偏移量。
- `TreeBuilder` 使用 `LinkedHashMap` 保持输入顺序，默认把孤儿节点作为根节点保留。
- `SourceBoundaryAssert` 位于 `common-test`，用于组件内部测试源码边界，不应作为业务运行时依赖。

## 迁移说明

| 旧 API | 新 API |
|---|---|
| `CommonStatus` | `ResultCode` |
| 旧前缀通用异常 | `BusinessException`、`ValidationException`、`RemoteCallException` |
| `ResultDto`、`ResultModel` | `ResultRecord` |
| `PageResultDto`、`PageResultModel` | `PageResultRecord` |
| `ResultDtos`、`ResultModels` | `ResultRecord` 和 `PageResultRecord` 自身的静态工厂方法 |
| `PageMeta` | `PageMetaRecord` |
| `PageModel` | 响应分页用 `PageResultRecord`，切片数据用 `PageSlice` |
| `top.egon.cola.component.common.model.*` | `top.egon.cola.component.common.pojo.*` |
| `top.egon.cola.component.common.result.*` | `top.egon.cola.component.common.pojo.*` |
| `top.egon.cola.component.common.structure.tree.*` | `top.egon.cola.component.common.pojo.*` |
| `top.egon.cola.component.common.util.IdUtils` | `LongIdGenerator` / `SnowflakeIdGenerator`；仅在 UUID 兼容契约中继续使用已废弃的 `UuidV7` |
| `top.egon.cola.component.common.util.CryptoUtils` | `Digests`、`Hmacs`、`Base64s`、`Hexes` |
| `egon-cola-component-common-mask` | `egon-cola-component-common-data-desensitize-spring-boot-starter` |
| `top.egon.cola.component.common.util.MaskingUtils`、`top.egon.cola.component.common.mask.Masking` | `@Sensitive`、`SensitiveStrategyRegistry`，或日志标量参数使用 `SensitiveLogs.of` |

旧的 `util` 聚合包、拆分的 `model/result/structure` 包、独立结果工厂、`BaseEntity` 和 `AuditableModel` 已被有意移除。

Snowflake 位布局、配置、时钟回拨、Kubernetes 机器 ID 分配和 UUIDv7 迁移边界见 [common ID Starter 中文文档](egon-cola-component-common-id-starter/README.zh-CN.md)。

## 验证命令

```bash
./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml test
```
