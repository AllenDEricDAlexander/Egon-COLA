# egon-cola-component-common

## 简要介绍

`egon-cola-component-common` 是 Egon COLA 组件体系的企业级通用能力聚合模块，提供错误码、异常、请求模型、分页模型、响应模型、链路追踪、ID、加密编码、数据脱敏、树结构构建和测试边界断言等基础能力。

这个目录本身是 `pom` 聚合模块，不是业务应用应该直接依赖的运行时 Jar。业务侧应通过 `egon-cola-components-bom` 管理版本，然后按需引入具体子模块，避免把不需要的基础能力一起带入业务工程。

## 模块结构

| Module | 说明 |
|---|---|
| `egon-cola-component-common-core` | 通用 `int code` 错误状态、异常基类、业务/系统/校验/权限等异常类型、枚举契约 |
| `egon-cola-component-common-model` | `PageQuery`、`SortQuery`、`TimeRangeQuery`、`BaseRequest`、`OperatorContext`、`PageModel`、`PageSlice` 等请求和分页模型 |
| `egon-cola-component-common-trace` | 基于 SLF4J MDC 的 `traceId` 上下文与快照 |
| `egon-cola-component-common-result` | 对外响应 DTO 与内部服务返回 Model，以及对应工厂方法 |
| `egon-cola-component-common-id` | UUIDv7 生成工具与 `IdGenerator` 抽象 |
| `egon-cola-component-common-crypto` | SHA-256、HMAC-SHA256、Base64、Hex 工具 |
| `egon-cola-component-common-mask` | 手机号、邮箱、首尾保留等稳定脱敏规则 |
| `egon-cola-component-common-structure` | 通用父子节点树构建器 |
| `egon-cola-component-common-test` | 组件内部使用的源码依赖边界测试工具 |

## 功能说明

### 统一错误状态和异常

`common-core` 以 `CommonStatus` 作为默认错误状态集合，所有状态使用 `int code`，适合 API 响应、日志检索和跨系统传递。业务可以直接使用 `EgonBusinessException`、`EgonValidationException`、`EgonRemoteCallException` 等异常类型，也可以实现 `ErrorStatus` 扩展自己的错误状态。

### 请求、查询和分页模型

`common-model` 的主要契约使用 Java record，并带有稳定 JSON 字段名和字段顺序：

| 契约 | 用途 |
|---|---|
| `PageQuery` | 归一化页码和页大小，页码从 1 开始，默认页大小 10，最大页大小 500 |
| `SortQuery` | 可选排序字段和 `ASC` / `DESC` 排序方向 |
| `TimeRangeQuery` | 可选开始/结束时间范围 |
| `BaseRequest` | 请求元数据容器 |
| `OperatorContext` | 操作人身份上下文 |
| `PageModel` / `PageSlice` | 内部分页数据结构，记录集合会被防御性复制为不可变列表 |

### 对外 DTO 与内部 Model 分离

`common-result` 区分对外 API 响应和内部应用/服务结果：

| 场景 | 类型 |
|---|---|
| Controller 对外返回 | `ResultDto`、`PageResultDto`、`ErrorResultDto` |
| 应用服务/领域服务内部返回 | `ResultModel`、`PageResultModel`、`ErrorResultModel` |
| 对外响应工厂 | `ResultDtos` |
| 内部结果工厂 | `ResultModels` |

`ResultDtos` 会读取 `TraceContext.getTraceId()`，把当前 MDC 中的 `traceId` 写入响应，便于日志和接口结果关联。

### 基础工具能力

`common-id` 提供 UUIDv7，适合生成趋势递增的业务 ID。`common-crypto` 提供稳定的 UTF-8 编码/摘要方法。`common-mask` 负责常用字段脱敏。`common-structure` 的 `TreeBuilder` 可以把平铺节点构造成父子树。

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

## 完整的使用示例

下面示例展示一个查询订单列表的 Controller：它使用 `PageQuery` 归一化分页参数，用 `ResultDtos` 输出对外响应，用 `TraceContext` 注入响应链路 ID，用 `UuidV7` 生成业务 ID，用 `Masking` 和 `Hmacs` 处理展示和签名。

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

配套服务可以返回内部模型，Controller 再决定是否转换为 DTO：

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

树结构构建示例：

```java
import top.egon.cola.component.common.structure.tree.TreeBuilder;
import top.egon.cola.component.common.structure.tree.TreeNode;

import java.util.List;

List<TreeNode<Long, String>> nodes = List.of(
        new TreeNode<>(1L, null, "总部"),
        new TreeNode<>(2L, 1L, "华东区"),
        new TreeNode<>(3L, 2L, "上海")
);

List<TreeNode<Long, String>> roots = TreeBuilder.build(nodes);
```

## 设计思想和实现细节

### 设计思想

1. 按能力拆分，不做大而全的 common Jar。业务方只引入自己需要的模块。
2. 对外 DTO 与内部 Model 分离，避免内部调用结果被 Controller 响应格式绑死。
3. 公共契约优先使用 Java record，保持不可变、可序列化、字段顺序稳定。
4. `common-core` 保持无 Spring、无 Jackson 依赖，减少基础错误码和异常的传递成本。
5. 工具能力只保留稳定、明确、低侵入的函数，不引入业务语义。

### 实现细节

- `PageQuery` 在构造时完成页码和页大小归一化，`offset()` 根据归一化后的值计算数据库偏移量。
- `PageModel` 会复制传入 records 并包装为不可变列表，避免分页结果被外部修改。
- `ResultDtos.success`、`ResultDtos.page` 会读取 `TraceContext`，把 MDC `traceId` 写入响应。
- `UuidV7` 基于 time-ordered epoch UUID，适合日志排序和索引局部性更好的 ID 场景。
- `Masking.mobile` 对标准手机号保留前三后四，短字符串走首尾保留规则。
- `TreeBuilder` 使用 `LinkedHashMap` 保持输入顺序，默认把孤儿节点作为根节点保留。
- `SourceBoundaryAssert` 位于 `common-test`，用于组件内部测试源码边界，不应作为业务运行时依赖。

## 迁移说明

| 旧 API | 新 API |
|---|---|
| `top.egon.cola.component.common.result.Result` | `ResultDto` 或 `ResultModel` |
| `top.egon.cola.component.common.result.PageResult` | `PageResultDto` 或 `PageResultModel` |
| `top.egon.cola.component.common.exception.BusinessException` | `EgonBusinessException` |
| `top.egon.cola.component.common.exception.SystemException` | `EgonSystemException` |
| `top.egon.cola.component.common.exception.ErrorCodes` | `CommonStatus` |
| `top.egon.cola.component.common.util.IdUtils` | `UuidV7` 或 `UuidV7Generator` |
| `top.egon.cola.component.common.util.CryptoUtils` | `Digests`、`Hmacs`、`Base64s`、`Hexes` |
| `top.egon.cola.component.common.util.MaskingUtils` | `Masking` |
| `top.egon.cola.component.common.util.TreeUtils` | `TreeBuilder` |

旧的 `util` 聚合包、JavaBean 风格通用契约、`BaseEntity` 和 `AuditableModel` 已被有意移除。

## 验证命令

```bash
./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml test
```
