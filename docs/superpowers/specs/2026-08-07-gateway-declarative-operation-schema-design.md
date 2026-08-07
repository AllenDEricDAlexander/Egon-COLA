# Gateway 声明式 Operation Schema 与 MCP 参数装配设计

状态：待用户审核

关联文档：

- `2026-08-06-gateway-annotation-managed-mcp-design.md`
- `2026-07-28-gateway-operation-schema-presentation-design.md`
- `2026-07-25-gateway-starter-interface-reporting-design.md`

## 1. 文档关系与决策边界

本文是 2026-08-06 注解托管 MCP 方案的破坏性增补规范，替换其中以下内容：

- HTTP Tool 输入 Schema 的扁平参数合并规则；
- `inputLocations` 的生成、上报和运行时绑定规则；
- `GatewaySchemaField(path, description)` 的方法级字段说明方式；
- `GatewayOperation.responseSchemaFields` 的响应字段数组；
- Java 与 Protobuf 复杂对象、泛型和响应包装对象的 Schema 生成规则。

2026-08-06 方案中的注解托管 MCP、稳定 Tool ID、严格 Override、Operation 并集、
Remote MCP 边界和手工本地 Tool 入口删除规则继续有效。

本文只定义待实现方案。用户审核通过前，不修改前端或后端代码。

## 2. 目标

1. `GatewayOperation.requestSchemaFields` 继续保留，但升级为完整的请求位置与根类型声明。
2. `GatewaySchemaField` 放在 DTO 字段、Record Component、Getter 或方法参数上，字段说明不再集中写在 `GatewayOperation`。
3. 明确区分 Spring `@RequestBody` 与复杂 Query/`@ModelAttribute`，不能再把未标注的复杂参数默认当作 Body。
4. 正确描述 PATH、QUERY、HEADER、COOKIE、BODY、PART 和 Unary RPC Message。
5. 正确描述 `ResultRecord<T>`、`ResultRecord<List<T>>`、`ResultRecord<Map<String, T>>`、`ResultRecord<基础类型>` 和 `PageResultRecord<T>`。
6. 使用 Jackson `JavaType` 保留嵌套泛型，统一支持对象、数组、Map、枚举、基本类型、递归引用和 Jakarta Validation。
7. RPC 继续以 Protobuf Descriptor 为类型事实来源，并通过自定义 Field Option 补充字段业务说明。
8. HTTP Managed MCP 输入直接采用 `path/query/body` 结构，删除 `inputLocations` 和扁平参数二次绑定。
9. Gateway Admin 后端和 Admin Web 同步升级，Managed Tool Schema 只读展示，不恢复任何手工 Schema 配置。

## 3. 非目标

- 不恢复本地 MCP Tool 的创建、编辑、删除或 Schema 配置入口。
- 不允许模型传入 Authorization、任意 HEADER、COOKIE 或文件内容。
- 不为 Multipart、PART、SSE、流式 HTTP 或 Streaming RPC 生成 Managed Tool。
- 不支持同一 HTTP Operation 多个逻辑 Request Body。
- 不从 Java 字段名或 Proto 字段名猜测业务说明。
- 不修改旧 Definition 或旧 Release Snapshot 以伪装成新 Schema。
- 不提供 v1/v2 双读、双写、灰度兼容或旧注解兼容层。

## 4. 最终注解模型

### 4.1 `GatewayInterfaceGroup`

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GatewayInterfaceGroup {

    String businessDomainCode();

    String businessDomainName();

    String entityDomainCode();

    String entityDomainName();

    String code();

    String name();

    String description() default "";

    String mcpServerCode() default "";
}
```

类或 RPC Contract 接口没有该注解时，不进入 Gateway 接口目录。组内只要存在 `registerMcp = true` 的方法，`mcpServerCode` 就必须非空并能在目标 Gateway Group 内唯一解析；环境改派仍只能通过严格 Managed Override 完成。

### 4.2 `GatewayOperation`

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GatewayOperation {

    String name() default "";

    String summary() default "";

    String description() default "";

    String owner() default "";

    boolean externalAccessible() default false;

    boolean idempotent() default false;

    boolean registerMcp() default false;

    String mcpName() default "";

    String[] mcpRequiredPermissions() default {};

    McpRiskLevel mcpRiskLevel() default McpRiskLevel.LOW;

    String[] tags() default {};

    GatewayRequestSchemaField[] requestSchemaFields() default {};

    GatewayResponseSchema responseSchema()
            default @GatewayResponseSchema;
}
```

破坏性变化：

- `requestSchemaFields` 从 `GatewaySchemaField[]` 改为 `GatewayRequestSchemaField[]`；
- 删除 `responseSchemaFields`；
- 新增单一 `responseSchema`；
- 旧的 `GatewaySchemaField(path = "...", description = "...")` 写法不再编译。

### 4.3 `GatewayRequestSchemaField`

```java
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GatewayRequestSchemaField {

    GatewayRequestLocation location();

    Class<?> schema();

    String name() default "";

    GatewaySchemaShape shape() default GatewaySchemaShape.AUTO;

    boolean expanded() default false;
}
```

字段语义：

| 字段 | 语义 |
| --- | --- |
| `location` | 请求根节点的位置，必须与真实 Spring 参数或 RPC 输入匹配 |
| `schema` | `OBJECT/VALUE` 的实际类型，或 `LIST/MAP` 的元素/Value 类型 |
| `name` | PATH、普通 QUERY、HEADER、COOKIE、PART 的线上名称；BODY、展开 Query 和 RPC Message 留空 |
| `shape` | 根值形态；`AUTO` 从真实泛型类型推导并校验 |
| `expanded` | 只允许用于 `QUERY + OBJECT`，表示 Spring `@ModelAttribute` 风格字段展开 |

`requestSchemaFields` 是完整声明，不是局部覆盖。一个方法一旦填写该数组，所有外部业务参数都必须恰好出现一次；缺失、重复、多写或位置不一致都使 Starter 扫描失败。

对于 `registerMcp = true` 的方法，`requestSchemaFields` 必须完整显式声明。非 MCP Operation 可以继续由框架签名自动发现，但不得再使用旧的字段路径数组。

### 4.4 `GatewayResponseSchema`

```java
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GatewayResponseSchema {

    Class<?> wrapper() default Void.class;

    String payloadField() default "";

    Class<?> schema() default Void.class;

    GatewaySchemaShape shape() default GatewaySchemaShape.AUTO;
}
```

字段语义：

| 字段 | 语义 |
| --- | --- |
| `wrapper` | 响应包装类型，例如 `ResultRecord.class`、`PageResultRecord.class` 或 Proto Result Message |
| `payloadField` | 包装对象中承载业务数据的 JSON/Proto 字段名，例如 `data`、`records` |
| `schema` | `OBJECT/VALUE` 的实际类型，或 `LIST/MAP` 的元素/Value 类型 |
| `shape` | Payload 形态；无响应体显式使用 `VOID` |

规则：

- `wrapper = Void.class` 表示直接响应，且 `payloadField` 必须为空；
- 有 Wrapper 时 `payloadField` 必须存在于真实返回类型或 Proto 输出 Descriptor；
- `LIST` 的 `schema` 是元素类型，不是 `List.class`；
- `MAP` 的 `schema` 是 Value 类型，不是 `Map.class`；
- `VALUE` 只允许字符串、数字、布尔、枚举及受支持的格式化标量；
- `VOID` 要求 `wrapper = Void.class`、`schema = Void.class`，并且真实方法无响应体；
- 对 `registerMcp = true` 的方法，非 Void 响应必须显式声明 `responseSchema`。

### 4.5 `GatewaySchemaField`

```java
@Target({
        ElementType.FIELD,
        ElementType.RECORD_COMPONENT,
        ElementType.METHOD,
        ElementType.PARAMETER
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GatewaySchemaField {

    String description() default "";

    GatewaySchemaType type() default GatewaySchemaType.AUTO;

    String format() default "";

    GatewaySchemaRequired required()
            default GatewaySchemaRequired.AUTO;

    String example() default "";

    Class<?> implementation() default Void.class;
}
```

字段语义：

| 字段 | 语义 |
| --- | --- |
| `description` | 业务字段说明；空值表示未提供，不允许自动猜测 |
| `type` | JSON Schema 类型覆盖；默认从 `JavaType`/Descriptor 推导 |
| `format` | `date`、`date-time`、`uuid`、`decimal` 等格式 |
| `required` | 三态必填声明；`AUTO` 读取 Jackson、构造器和 Validation 元数据 |
| `example` | 标量直接写文本；对象、数组和 Map 必须写合法 JSON |
| `implementation` | 为接口、抽象类、`Object`、通配符或擦除泛型补充实际类型 |

`GatewaySchemaField` 不再有 `path`。嵌套字段说明放在嵌套 DTO 自己的字段上，DTO 被多个 Operation 复用时只维护一份说明。

`implementation` 只补足无法从真实 `JavaType` 得到的类型信息。若 Java 泛型已明确，例如 `List<OrderLineView>` 或 `Map<String, MoneyView>`，不得重复声明。显式类型与真实类型不可赋值、容器形态冲突或覆盖 Bean Validation 时，扫描失败。

共享的 `ResultRecord`、`PageResultRecord` 位于 Common Core，不反向依赖 Gateway Starter 注解。其包装字段由响应 Wrapper Adapter 和 Jackson 属性模型生成，业务 Payload DTO 仍使用 `GatewaySchemaField`。

Record Component 上的注解可能同时传播到生成的 Field、Accessor 和构造器参数。Schema Mapper 必须按同一 Record Component 去重；传播出的相同元数据不是重复声明，只有不同来源给出互相冲突的值才失败。

### 4.6 枚举

```java
public enum GatewayRequestLocation {
    PATH,
    QUERY,
    HEADER,
    COOKIE,
    BODY,
    PART,
    RPC_MESSAGE
}

public enum GatewaySchemaShape {
    AUTO,
    VALUE,
    OBJECT,
    LIST,
    MAP,
    VOID
}

public enum GatewaySchemaType {
    AUTO,
    STRING,
    INTEGER,
    NUMBER,
    BOOLEAN,
    OBJECT,
    ARRAY,
    MAP
}

public enum GatewaySchemaRequired {
    AUTO,
    REQUIRED,
    OPTIONAL
}

public enum McpRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
```

`GatewaySchemaType.MAP` 最终输出为 JSON Schema `type=object + additionalProperties`，不是自造的 JSON Schema 类型。

## 5. Java DTO 字段映射规范

### 5.1 完整复杂对象示例

```java
public record UpdateOrderCommand(
        @GatewaySchemaField(
                description = "修改原因",
                required = GatewaySchemaRequired.REQUIRED,
                example = "客户变更收货信息"
        )
        @NotBlank
        String reason,

        @GatewaySchemaField(
                description = "订单行修改列表",
                required = GatewaySchemaRequired.REQUIRED
        )
        @NotEmpty
        List<@Valid UpdateOrderLineCommand> lines,

        @GatewaySchemaField(
                description = "扩展业务属性，Key 为属性编码"
        )
        Map<String, String> attributes,

        @GatewaySchemaField(
                description = "新收货地址"
        )
        @Valid
        DeliveryAddressCommand deliveryAddress,

        @GatewaySchemaField(
                description = "是否强制覆盖并发版本",
                example = "false"
        )
        boolean force
) {
}

public record UpdateOrderLineCommand(
        @GatewaySchemaField(
                description = "订单行 ID",
                required = GatewaySchemaRequired.REQUIRED,
                example = "OL-10001"
        )
        @NotBlank
        String lineId,

        @GatewaySchemaField(
                description = "新数量",
                required = GatewaySchemaRequired.REQUIRED,
                example = "2"
        )
        @Min(1)
        int quantity
) {
}

public record DeliveryAddressCommand(
        @GatewaySchemaField(description = "省份", example = "浙江省")
        @NotBlank
        String province,

        @GatewaySchemaField(description = "城市", example = "杭州市")
        @NotBlank
        String city,

        @GatewaySchemaField(description = "详细地址")
        @Size(max = 200)
        String detail
) {
}
```

```java
public record OrderAggregateView(
        @GatewaySchemaField(
                description = "订单 ID",
                required = GatewaySchemaRequired.REQUIRED,
                example = "O-20260807-0001"
        )
        String orderId,

        @GatewaySchemaField(description = "订单状态")
        OrderStatus status,

        @GatewaySchemaField(description = "订单行")
        List<OrderLineView> lines,

        @GatewaySchemaField(
                description = "按币种汇总的金额，Key 为 ISO 4217 币种编码"
        )
        Map<String, MoneyView> amountByCurrency,

        @GatewaySchemaField(description = "商品总件数", example = "3")
        int itemCount,

        @GatewaySchemaField(description = "当前调用方是否可编辑")
        boolean editable
) {
}

public record MoneyView(
        @GatewaySchemaField(
                description = "金额",
                format = "decimal",
                example = "128.50"
        )
        BigDecimal amount,

        @GatewaySchemaField(description = "币种", example = "CNY")
        String currency
) {
}
```

`OrderAggregateView` 的 `data` 内同时包含 LIST、MAP、枚举、整数、布尔和嵌套对象，Schema 必须完整保留这些结构。

### 5.2 类型推导与显式覆盖

以下示例展示 `GatewaySchemaField` 的全部字段。只有真实类型不完整时才使用 `type` 和 `implementation`：

```java
public record ExternalRequest(
        @GatewaySchemaField(
                description = "外部请求 ID",
                type = GatewaySchemaType.STRING,
                format = "uuid",
                required = GatewaySchemaRequired.REQUIRED,
                example = "3d594650-3436-4d8d-8bd4-22d6f65f58a2",
                implementation = UUID.class
        )
        Object requestId
) {
}
```

这里 `Object` 是上游既有的不完整签名，`implementation = UUID.class` 提供实际类型，`type = STRING` 与 UUID 的 JSON 表达一致。若字段本来就是 `UUID requestId`，应删除 `type` 和 `implementation`，保留 `description/format/required/example` 即可。

- Java `byte/short/int/long/BigInteger` 映射为 `integer`，并尽可能带 `int32/int64` format；
- `float/double/BigDecimal` 映射为 `number`；
- `String/char/Character/UUID/LocalDate/Instant` 映射为带相应 format 的 `string`；
- Enum 映射为 `string + enum`；
- Array/Collection 映射为 `array + items`；
- `Map<String, T>` 映射为 `object + additionalProperties`；
- 普通 Bean/Record 映射为 `object + properties`；
- 普通请求对象默认 `additionalProperties=false`；只有显式 Map、`@JsonAnySetter` 或等价扩展属性允许动态 Key；
- Managed MCP 使用的 Map Key 必须可稳定序列化为字符串；不满足时发布失败。

显式 `type` 必须与真实类型兼容。它用于补充不完整模型，不用于把字符串伪装成对象或绕过真实方法签名。

### 5.3 Required 与 Validation

优先级和冲突规则：

1. `@NotNull/@NotBlank/@NotEmpty`、Jackson Required Creator Property 和 Proto Field Option 形成真实 Required 约束；
2. `required = REQUIRED` 可以收紧未声明的字段；
3. `required = OPTIONAL` 只能明确一个本来可选的字段，不能弱化 Validation 或构造器约束；
4. `required = AUTO` 使用真实类型、Jackson 和 Validation 推导；
5. 任意冲突都在 Starter 扫描时失败，不静默选择一方。

标准映射至少包括：

| Validation | JSON Schema |
| --- | --- |
| `@NotNull/@NotBlank/@NotEmpty` | 父对象 `required`，并设置相应长度约束 |
| `@Size` | `minLength/maxLength` 或 `minItems/maxItems` |
| `@Min/@Max` | `minimum/maximum` |
| `@DecimalMin/@DecimalMax` | `minimum/maximum` 及 exclusive 语义 |
| `@Pattern` | `pattern` |
| `@Positive/@PositiveOrZero` | `exclusiveMinimum/minimum` |
| `@Email` | `format=email` |

## 6. HTTP 请求组合规则

### 6.1 Request Body 与复杂 Query 严格分离

`BODY` 只允许匹配带 Spring `@RequestBody` 的参数。没有 `@RequestBody` 的复杂对象不能再按 Body 推断。

复杂 Query 必须使用：

```java
@GatewayRequestSchemaField(
        location = GatewayRequestLocation.QUERY,
        schema = OrderQuery.class,
        shape = GatewaySchemaShape.OBJECT,
        expanded = true
)
```

并匹配真实的 `@ModelAttribute OrderQuery query`，或 Spring 明确按 ModelAttribute 解析的同等签名。DTO 字段展开到 `query.properties`，不是把整个对象编码成一个 JSON Query 参数。

普通 `@RequestParam` 使用 `expanded = false` 和明确 `name`。复杂 Query 与显式 Query 参数展开后若发生字段名冲突，扫描失败。

### 6.2 完整 PATH + QUERY + HEADER + Request Body 示例

```java
@GatewayInterfaceGroup(
        businessDomainCode = "trade",
        businessDomainName = "交易域",
        entityDomainCode = "order",
        entityDomainName = "订单",
        code = "order-service",
        name = "订单服务",
        description = "订单相关业务接口",
        mcpServerCode = "trade-mcp"
)
@RestController
@RequestMapping("/orders")
public class OrderController {

    @PutMapping("/{orderId}")
    @GatewayOperation(
            name = "修改订单",
            summary = "修改订单行、地址和扩展属性",
            description = "返回修改后的订单聚合对象",
            owner = "order-team",
            externalAccessible = false,
            idempotent = true,
            registerMcp = true,
            mcpName = "order_update",
            mcpRequiredPermissions = {"order:write"},
            mcpRiskLevel = McpRiskLevel.MEDIUM,
            tags = {"order", "command"},
            requestSchemaFields = {
                    @GatewayRequestSchemaField(
                            location = GatewayRequestLocation.PATH,
                            name = "orderId",
                            schema = String.class,
                            shape = GatewaySchemaShape.VALUE
                    ),
                    @GatewayRequestSchemaField(
                            location = GatewayRequestLocation.QUERY,
                            name = "notify",
                            schema = Boolean.class,
                            shape = GatewaySchemaShape.VALUE
                    ),
                    @GatewayRequestSchemaField(
                            location = GatewayRequestLocation.HEADER,
                            name = "Authorization",
                            schema = String.class,
                            shape = GatewaySchemaShape.VALUE
                    ),
                    @GatewayRequestSchemaField(
                            location = GatewayRequestLocation.BODY,
                            schema = UpdateOrderCommand.class,
                            shape = GatewaySchemaShape.OBJECT
                    )
            },
            responseSchema = @GatewayResponseSchema(
                    wrapper = ResultRecord.class,
                    payloadField = "data",
                    schema = OrderAggregateView.class,
                    shape = GatewaySchemaShape.OBJECT
            )
    )
    public ResultRecord<OrderAggregateView> update(
            @PathVariable("orderId")
            @GatewaySchemaField(
                    description = "订单 ID",
                    required = GatewaySchemaRequired.REQUIRED,
                    example = "O-20260807-0001"
            )
            String orderId,
            @RequestParam(name = "notify", defaultValue = "false")
            @GatewaySchemaField(
                    description = "是否发送订单变更通知",
                    example = "false"
            )
            boolean notify,
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody
            @GatewaySchemaField(description = "订单修改内容")
            UpdateOrderCommand command) {
        return ResultRecord.success(orderService.update(
                orderId,
                notify,
                command
        ));
    }
}
```

Authorization 只进入接口目录，不进入 MCP inputSchema。它由 Gateway 身份上下文注入，模型不能提供或覆盖。

### 6.3 复杂 Query + `ResultRecord<List<T>>` 示例

```java
public record OrderQuery(
        @GatewaySchemaField(description = "订单状态")
        OrderStatus status,

        @GatewaySchemaField(description = "客户 ID")
        String customerId,

        @GatewaySchemaField(description = "业务标签")
        List<String> tags
) {
}

@GetMapping
@GatewayOperation(
        name = "查询订单列表",
        summary = "按组合条件查询订单",
        owner = "order-team",
        idempotent = true,
        registerMcp = true,
        mcpName = "order_list",
        mcpRequiredPermissions = {"order:read"},
        mcpRiskLevel = McpRiskLevel.LOW,
        tags = {"order", "query"},
        requestSchemaFields = @GatewayRequestSchemaField(
                location = GatewayRequestLocation.QUERY,
                schema = OrderQuery.class,
                shape = GatewaySchemaShape.OBJECT,
                expanded = true
        ),
        responseSchema = @GatewayResponseSchema(
                wrapper = ResultRecord.class,
                payloadField = "data",
                schema = OrderSummaryView.class,
                shape = GatewaySchemaShape.LIST
        )
)
public ResultRecord<List<OrderSummaryView>> list(
        @Valid @ModelAttribute OrderQuery query) {
    return ResultRecord.success(orderService.list(query));
}
```

### 6.4 `PageResultRecord<T>` 示例

```java
@GetMapping("/page")
@GatewayOperation(
        name = "分页查询订单",
        summary = "按组合条件分页查询订单",
        owner = "order-team",
        idempotent = true,
        registerMcp = true,
        mcpName = "order_page",
        mcpRequiredPermissions = {"order:read"},
        mcpRiskLevel = McpRiskLevel.LOW,
        tags = {"order", "query", "page"},
        requestSchemaFields = @GatewayRequestSchemaField(
                location = GatewayRequestLocation.QUERY,
                schema = OrderPageQuery.class,
                shape = GatewaySchemaShape.OBJECT,
                expanded = true
        ),
        responseSchema = @GatewayResponseSchema(
                wrapper = PageResultRecord.class,
                payloadField = "records",
                schema = OrderSummaryView.class,
                shape = GatewaySchemaShape.LIST
        )
)
public PageResultRecord<OrderSummaryView> page(
        @Valid @ModelAttribute OrderPageQuery query) {
    return orderService.page(query);
}
```

`PageResultRecord<T>` 的完整输出包含 `success/code/status/message/records/page/traceId/timestamp`。`records` 是 `array<OrderSummaryView>`，`page` 是复杂对象，不能只输出 records 的局部 Schema。

### 6.5 Body 和 Result 的形态矩阵

| Java 签名 | 注解声明 |
| --- | --- |
| `@RequestBody UpdateOrderCommand` | `BODY, schema=UpdateOrderCommand.class, shape=OBJECT` |
| `@RequestBody List<UpdateOrderCommand>` | `BODY, schema=UpdateOrderCommand.class, shape=LIST` |
| `@RequestBody Map<String, UpdateOrderCommand>` | `BODY, schema=UpdateOrderCommand.class, shape=MAP` |
| `@RequestBody String` | `BODY, schema=String.class, shape=VALUE` |
| `ResultRecord<OrderAggregateView>` | `wrapper=ResultRecord, payloadField=data, schema=OrderAggregateView, shape=OBJECT` |
| `ResultRecord<List<OrderSummaryView>>` | `wrapper=ResultRecord, payloadField=data, schema=OrderSummaryView, shape=LIST` |
| `ResultRecord<Map<String, MoneyView>>` | `wrapper=ResultRecord, payloadField=data, schema=MoneyView, shape=MAP` |
| `ResultRecord<Long>` | `wrapper=ResultRecord, payloadField=data, schema=Long, shape=VALUE` |
| `PageResultRecord<OrderSummaryView>` | `wrapper=PageResultRecord, payloadField=records, schema=OrderSummaryView, shape=LIST` |

## 7. HTTP 签名严格校验

Starter 以 Spring 最终 Handler Method 模型为事实来源，对注解做双向校验：

1. PATH 的 `name` 必须存在于最终 Route Template，并匹配 `@PathVariable`；
2. QUERY 必须匹配 `@RequestParam` 或复杂 Query/ModelAttribute；
3. HEADER、COOKIE、PART 必须匹配同名 Spring 参数；
4. BODY 必须匹配唯一 `@RequestBody`，不能匹配 ModelAttribute；
5. BODY 的 `shape/schema` 必须匹配方法参数的完整泛型 `JavaType`；
6. 每个真实业务参数必须被声明一次，Servlet、Principal、Request/Response 等框架参数除外；
7. `expanded=true` 只允许 `QUERY + OBJECT`，并且展开后的属性名必须唯一；
8. `registerMcp=true` 时存在 PART/Multipart、Streaming 或无法可信注入的 Required HEADER/COOKIE，直接拒绝 MCP 投影；
9. Authorization、Trace 和身份上下文只能由 Gateway 注入，不能进入模型输入；
10. 注解声明与真实签名冲突时启动失败，不输出警告后继续运行。

## 8. Java Schema 生成

### 8.1 `JavaType` 是唯一 Java 类型模型

所有 Java Schema 递归都使用：

```java
JavaType javaType = objectMapper.getTypeFactory().constructType(type);
```

后续递归必须使用属性的完整 `JavaType`，不能再调用 `propertyType.getRawClass()` 丢弃泛型。方法参数和返回值都从 `Method#getGenericParameterTypes()`、`Method#getGenericReturnType()` 构造。

### 8.2 `$defs/$ref` 与递归

- 每个规范化 `JavaType` 只生成一份 `$defs`；
- 同一类型的重复使用通过 `$ref` 引用；
- 自引用、互相引用和递归容器必须使用 `$ref`，不能无限展开；
- 泛型实参不同的同一 Raw Class 视为不同 Schema Definition；
- `$defs` Key 从规范化类型签名稳定生成，不能依赖扫描顺序；
- 超过全局深度、节点数或总字节限制时整份 Operation Definition 失败，不能生成不完整的 MCP Schema。

### 8.3 Jackson 属性语义

Schema 使用最终 JSON 属性名和可见性：

- 尊重 `@JsonProperty`、Property Naming Strategy、`@JsonIgnore`、`@JsonIgnoreProperties`；
- Getter/Field/Record Component 上的 `GatewaySchemaField` 合并到同一 Jackson Property；
- 同一属性多个位置声明冲突时失败；
- `example` 必须按生成出的字段 Schema 校验；
- DTO 中不可见或不存在的注解字段不能被静默忽略。

Nullability 只来自明确的 `@Nullable`、受支持的 Optional 类型或已知 Wrapper 语义，不能把所有 Java 引用类型都猜成可空。`ResultRecord.data` 在失败响应中允许为 null；`PageResultRecord.records` 和 `page` 由其构造器保证非 null。Wrapper Adapter 必须反映这些真实约束。

## 9. RPC Schema 与 Proto Field Option

### 9.1 类型来源

Unary RPC 的请求/响应结构只从 `RpcContractCatalog` 和 Protobuf Descriptor 生成。Java 生成类只用于 `GatewayOperation` 根类型声明的签名校验，不能用 Java 反射代替 Descriptor。

RPC 请求必须只有一个根声明：

```java
@GatewayRequestSchemaField(
        location = GatewayRequestLocation.RPC_MESSAGE,
        schema = UpdateOrderRequest.class,
        shape = GatewaySchemaShape.OBJECT
)
```

Protobuf 生成类不能稳定承载手写 Java 字段注解。字段说明、格式、示例和 Required 语义使用 Gateway 自有 Proto Field Option。

### 9.2 Proto Option 定义

`gateway-contract` 发布 `egon/gateway/schema_options.proto` 及生成的 Java Extension：

```proto
syntax = "proto3";

package egon.gateway.schema.v1;

option java_package =
    "top.egon.cola.component.gateway.contract.schema.proto";
option java_multiple_files = true;

import "google/protobuf/descriptor.proto";

enum GatewayRequiredOption {
  GATEWAY_REQUIRED_AUTO = 0;
  GATEWAY_REQUIRED = 1;
  GATEWAY_OPTIONAL = 2;
}

message GatewaySchemaFieldOption {
  string description = 1;
  string format = 2;
  GatewayRequiredOption required = 3;
  string example = 4;
}

extend google.protobuf.FieldOptions {
  GatewaySchemaFieldOption gateway_schema = 51001;
}
```

类型、数组、Map、Enum 和消息引用继续由 Descriptor 决定，不在 Field Option 中重复声明。`schema_options.proto` 必须进入 Descriptor Snapshot 的依赖闭包。

### 9.3 RPC 复杂对象与 List Result Proto 示例

```proto
syntax = "proto3";

package trade.order.v1;

option java_package = "com.example.trade.order.rpc.proto";
option java_multiple_files = true;

import "egon/gateway/schema_options.proto";

message UpdateOrderLine {
  string line_id = 1 [(egon.gateway.schema.v1.gateway_schema) = {
    description: "订单行 ID"
    required: GATEWAY_REQUIRED
    example: "OL-10001"
  }];
  int32 quantity = 2 [(egon.gateway.schema.v1.gateway_schema) = {
    description: "新数量"
    required: GATEWAY_REQUIRED
    example: "2"
  }];
}

message UpdateOrderRequest {
  string order_id = 1 [(egon.gateway.schema.v1.gateway_schema) = {
    description: "订单 ID"
    required: GATEWAY_REQUIRED
    example: "O-20260807-0001"
  }];
  repeated UpdateOrderLine lines = 2
      [(egon.gateway.schema.v1.gateway_schema) = {
        description: "订单行修改列表"
        required: GATEWAY_REQUIRED
      }];
  map<string, string> attributes = 3
      [(egon.gateway.schema.v1.gateway_schema) = {
        description: "扩展业务属性"
      }];
  bool force = 4 [(egon.gateway.schema.v1.gateway_schema) = {
    description: "是否强制覆盖并发版本"
    example: "false"
  }];
}

message MoneyView {
  string amount = 1 [(egon.gateway.schema.v1.gateway_schema) = {
    description: "金额"
    format: "decimal"
    example: "128.50"
  }];
  string currency = 2 [(egon.gateway.schema.v1.gateway_schema) = {
    description: "币种"
    example: "CNY"
  }];
}

message OrderLineView {
  string line_id = 1;
  int32 quantity = 2;
}

message OrderAggregateView {
  string order_id = 1;
  string status = 2;
  repeated OrderLineView lines = 3;
  map<string, MoneyView> amount_by_currency = 4;
  int32 item_count = 5;
  bool editable = 6;
}

message OrderSummaryView {
  string order_id = 1;
  string status = 2;
}

message UpdateOrderResult {
  bool success = 1;
  int32 code = 2;
  string message = 3;
  OrderAggregateView data = 4
      [(egon.gateway.schema.v1.gateway_schema) = {
        description: "修改后的订单聚合对象"
      }];
}

message OrderListRequest {
  string customer_id = 1;
  repeated string statuses = 2;
}

message OrderListResult {
  bool success = 1;
  int32 code = 2;
  string message = 3;
  repeated OrderSummaryView data = 4
      [(egon.gateway.schema.v1.gateway_schema) = {
        description: "订单列表"
      }];
}

service OrderService {
  rpc UpdateOrder(UpdateOrderRequest) returns (UpdateOrderResult);
  rpc ListOrders(OrderListRequest) returns (OrderListResult);
}
```

### 9.4 RPC Contract 完整示例

```java
@EgonRpcService(
        grpcClass = OrderServiceGrpc.class,
        group = "default",
        version = "1.0.0"
)
@GatewayInterfaceGroup(
        businessDomainCode = "trade",
        businessDomainName = "交易域",
        entityDomainCode = "order",
        entityDomainName = "订单",
        code = "order-rpc",
        name = "订单 RPC 服务",
        description = "订单 Unary RPC 接口",
        mcpServerCode = "trade-mcp"
)
public interface OrderRpc {

    @EgonRpcMethod(name = "UpdateOrder", idempotent = true)
    @GatewayOperation(
            name = "RPC 修改订单",
            summary = "修改订单并返回完整订单聚合对象",
            description = "RPC arguments 是完整 UpdateOrderRequest",
            owner = "order-team",
            externalAccessible = false,
            idempotent = true,
            registerMcp = true,
            mcpName = "rpc_order_update",
            mcpRequiredPermissions = {"order:write"},
            mcpRiskLevel = McpRiskLevel.MEDIUM,
            tags = {"rpc", "order", "command"},
            requestSchemaFields = @GatewayRequestSchemaField(
                    location = GatewayRequestLocation.RPC_MESSAGE,
                    schema = UpdateOrderRequest.class,
                    shape = GatewaySchemaShape.OBJECT
            ),
            responseSchema = @GatewayResponseSchema(
                    wrapper = UpdateOrderResult.class,
                    payloadField = "data",
                    schema = OrderAggregateView.class,
                    shape = GatewaySchemaShape.OBJECT
            )
    )
    UpdateOrderResult updateOrder(UpdateOrderRequest request);

    @EgonRpcMethod(name = "ListOrders", idempotent = true)
    @GatewayOperation(
            name = "RPC 查询订单列表",
            summary = "查询订单列表",
            owner = "order-team",
            externalAccessible = false,
            idempotent = true,
            registerMcp = true,
            mcpName = "rpc_order_list",
            mcpRequiredPermissions = {"order:read"},
            mcpRiskLevel = McpRiskLevel.LOW,
            tags = {"rpc", "order", "query"},
            requestSchemaFields = @GatewayRequestSchemaField(
                    location = GatewayRequestLocation.RPC_MESSAGE,
                    schema = OrderListRequest.class,
                    shape = GatewaySchemaShape.OBJECT
            ),
            responseSchema = @GatewayResponseSchema(
                    wrapper = OrderListResult.class,
                    payloadField = "data",
                    schema = OrderSummaryView.class,
                    shape = GatewaySchemaShape.LIST
            )
    )
    OrderListResult listOrders(OrderListRequest request);
}
```

`@EgonRpcMethod.idempotent` 与 `@GatewayOperation.idempotent` 必须一致，不一致时 Contract 校验失败。

### 9.5 RPC Descriptor 映射

- scalar、enum、repeated、map、nested message、oneof 和 well-known type 都从 Descriptor 映射；
- repeated 映射为 `array + items`；
- map 映射为 `object + additionalProperties`；
- 循环消息使用 `$defs/$ref`；
- JSON 字段名使用 `FieldDescriptor#getJsonName()`，同时保留 proto name、field number 和 protobuf type；
- Field Option 只补充 description、format、required、example；
- Option 指定的 example 必须通过生成后的字段 Schema 校验；
- Unary 以外的 RPC 继续拒绝 Gateway/MCP 投影。

## 10. Operation Definition v2

### 10.1 破坏性协议升级

Starter 上报 `contractVersion` 从 `v1` 升级为 `v2`。Gateway Admin v2 只接收 v2，不保留 v1 解析分支。

`GatewayInterfaceDefinitionReport.Operation` 删除：

```text
parameters: List<Parameter>
```

同时删除 `GatewayInterfaceDefinitionReport.Parameter`。位置、名称、Required、默认值、约束、说明和技术类型全部由分组后的 `requestSchema` 表达，避免 `parameters` 与 Schema 双份数据漂移。

数据库继续复用 Operation Definition 的 `request_schema JSONB`、`response_schema JSONB` 和 `attributes JSONB`，不需要 Flyway 迁移。旧 Definition 保留为不可变历史，但不能用于生成 v2 Managed Tool 或新 Release。

### 10.2 HTTP Request Schema

HTTP requestSchema 固定为位置分组对象。只输出当前 Operation 实际存在的位置；PART 只允许进入接口目录，不能进入 Managed MCP。

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "x-egon-schema-model": "gateway-operation-request/v2",
  "type": "object",
  "properties": {
    "path": {
      "type": "object",
      "properties": {
        "orderId": {
          "type": "string",
          "description": "订单 ID"
        }
      },
      "required": ["orderId"],
      "additionalProperties": false
    },
    "query": {
      "type": "object",
      "properties": {
        "notify": {
          "type": "boolean",
          "default": false
        }
      },
      "additionalProperties": false
    },
    "header": {
      "type": "object",
      "properties": {
        "Authorization": {"type": "string"}
      },
      "required": ["Authorization"],
      "additionalProperties": false
    },
    "body": {
      "$ref": "#/$defs/UpdateOrderCommand"
    }
  },
  "required": ["path", "header", "body"],
  "additionalProperties": false,
  "$defs": {
    "UpdateOrderCommand": {
      "type": "object",
      "properties": {}
    }
  }
}
```

可用根节点为 `path/query/header/cookie/body/part`。BODY 直接放请求体 Schema，不额外套方法参数名。

### 10.3 Response Schema

responseSchema 始终描述线上完整响应，不只描述 Payload。`ResultRecord<List<OrderSummaryView>>` 的核心形态为：

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "x-egon-schema-model": "gateway-operation-response/v2",
  "type": "object",
  "properties": {
    "success": {"type": "boolean"},
    "code": {"type": "integer", "format": "int32"},
    "status": {"type": "string"},
    "message": {"type": "string"},
    "data": {
      "anyOf": [
        {
          "type": "array",
          "items": {"$ref": "#/$defs/OrderSummaryView"}
        },
        {"type": "null"}
      ]
    },
    "traceId": {"type": "string"},
    "timestamp": {"type": "integer", "format": "int64"}
  },
  "required": [
    "success",
    "code",
    "status",
    "message",
    "data",
    "traceId",
    "timestamp"
  ],
  "$defs": {
    "OrderSummaryView": {
      "type": "object",
      "properties": {}
    }
  }
}
```

Wrapper Adapter 负责将真实返回泛型、`wrapper/payloadField/schema/shape` 和 Jackson/Descriptor 属性模型交叉校验。注解不是替代真实方法签名的第二套类型系统。

## 11. Managed MCP 发布投影

### 11.1 HTTP Tool Input

HTTP Managed Tool 从 Operation v2 requestSchema 投影，只保留实际存在的：

```text
path
query
body
```

HEADER、COOKIE 和 PART 不复制到 MCP inputSchema。投影后清理不可达 `$defs`，保留所有可达引用并再次执行 JSON Schema 校验。

示例：

```json
{
  "type": "object",
  "properties": {
    "path": {
      "type": "object",
      "properties": {
        "orderId": {"type": "string"}
      },
      "required": ["orderId"],
      "additionalProperties": false
    },
    "query": {
      "type": "object",
      "properties": {
        "notify": {"type": "boolean", "default": false}
      },
      "additionalProperties": false
    },
    "body": {"$ref": "#/$defs/UpdateOrderCommand"}
  },
  "required": ["path", "body"],
  "additionalProperties": false,
  "$defs": {
    "UpdateOrderCommand": {
      "type": "object",
      "properties": {}
    }
  }
}
```

Tool outputSchema 直接复用完整 Operation responseSchema，包括 `ResultRecord` 或 `PageResultRecord` Wrapper。

### 11.2 RPC Tool Input

RPC Managed Tool inputSchema 直接使用完整 Protobuf Input Message Schema，不增加 `rpcMessage` 或 `body` 外壳。MCP `arguments` 就是完整请求消息对象。

RPC outputSchema 直接使用完整 Protobuf Output Message Schema，包括 Result Message Wrapper。

### 11.3 稳定身份和严格校验

Tool ID 继续使用：

```text
lowercaseHex(SHA-256(serverCode + "\0" + operationKey))
```

发布必须拒绝：

- v1 或缺少 Schema Model Version 的 Operation；
- HTTP Schema 根位置非法或结构不完整；
- RPC Message 与 Descriptor 不一致；
- 无法解析的 `$ref`、非法 example、非法 Map Key；
- Required HEADER/COOKIE 无可信 Gateway 注入来源；
- PART/Multipart/Streaming；
- 响应 Wrapper、Payload Field、Shape 与真实 Schema 不一致。

## 12. Runtime 调用契约

`GatewayOperationCall` 保留：

```java
public record GatewayOperationCall(
        String operationId,
        Map<String, Object> pathArguments,
        Map<String, Object> queryArguments,
        Object body
) {
}
```

HTTP 调用规则：

```text
arguments.path  -> pathArguments
arguments.query -> queryArguments
arguments.body  -> body
```

缺少可选 `path/query/body` 时分别使用空 Map 或 null。根节点出现 `header/cookie/part` 或未知字段时调用失败。HTTP Provider Invoker 按位置编码 Path、重复 Query 值和 JSON Body，不再依据 HTTP Method 猜测“GET 放 Query、其他全放 Body”。

RPC 调用规则：

```text
arguments -> body
pathArguments = {}
queryArguments = {}
```

### 12.1 删除 `inputLocations`

从以下所有契约删除 `inputLocations`：

- `McpRuntimeTool`；
- Admin Managed Tool DTO/API；
- Admin Web `McpManagedTool` TypeScript 类型；
- `McpReleaseContentFactory.ToolInput`；
- Rule JSON、Canonicalizer、Codec 和 Fixtures；
- `McpToolsCallHandler` 的扁平字段位置遍历逻辑。

不保留 deprecated Getter、不从旧 Release 推导、不接受旧 JSON 字段。

## 13. 后端模块改动范围

### 13.1 Gateway Starter

- 替换 `GatewayOperation`、`GatewaySchemaField`，新增请求/响应注解和枚举；
- 新增基于 `JavaType` 的 Java Schema Adapter；
- 升级 Protobuf Schema Adapter 并读取自定义 Field Option；
- 增加 Result/PageResult/通用 Wrapper Adapter；
- HTTP Mapper 输出位置分组 Schema；
- RPC Contributor 校验唯一 `RPC_MESSAGE` 和响应 Envelope；
- Definition Report 升级为 v2；
- `McpExposureMapper` 继续只写 MCP Exposure，不复制 Schema。

### 13.2 Gateway Contract

- 修改 Reporting v2 DTO，删除 Parameter；
- 修改 `McpRuntimeTool`，删除 `inputLocations`；
- 发布 `schema_options.proto` 及 Java Extension；
- 保持 `GatewayOperationCall` 的位置感知契约。

### 13.3 Gateway Admin

- v2 Report 校验和持久化；
- Catalog 不再把 `parameters` 写入 Operation attributes；
- `McpReleaseContentFactory` 从 v2 requestSchema 投影 HTTP `path/query/body`；
- RPC 直接复用 Message Schema；
- Managed Tool View 删除 `inputLocations`，继续返回只读 input/output Schema；
- Release 和 Activation 拒绝 v1 Operation；
- Remote MCP Tool 契约和手工 Remote Schema 不变。

### 13.4 Gateway Engine 与 MCP Runtime

- Rule Codec 与 Runtime Tool 删除 `inputLocations`；
- `McpToolsCallHandler` 直接解析结构化 arguments；
- HTTP/RPC Operation Invoker 分别消费位置参数和完整 Protobuf Message；
- Task 输入快照继续保存 `pathArguments/queryArguments/body`，不保存 Token、Cookie 或 Authorization。

## 14. Admin Web 同步改造

### 14.1 Operation 详情

Request Schema 主视图先展示位置分组：

```text
PATH
QUERY
HEADER
COOKIE
BODY
PART（仅接口目录）
```

每个位置下继续展示递归字段树。`schemaRows` 必须：

- 解析本地 `$defs/$ref`；
- 防止递归引用造成无限渲染；
- 展示 array item、Map `{value}`、基础值和对象；
- 展示 nullable、`oneOf/anyOf/allOf` 等组合类型；
- 正确继承父对象的 `required`；
- 展示 description、format、example、enum 和 Validation 约束；
- 不把 `$defs` 当作业务字段渲染；
- 原始 JSON 继续放在折叠区。

### 14.2 Managed Tool

Managed Tool 列表增加只读 Schema 预览：

- Input Schema 显示 HTTP `path/query/body` 或 RPC Message；
- Output Schema 显示完整 Wrapper；
- 保留 Operation、Server、权限、风险、幂等和启停状态；
- 删除 `inputLocations` 展示和类型；
- 不提供 Schema、Payload Field、类型、参数位置或绑定编辑控件。

Managed Override 仍只允许改 Server、追加权限、提高风险和禁用。Remote Tool 页面不受本次 Schema 注解限制。

### 14.3 前端 API 类型

`McpManagedTool` 调整为：

```ts
export type McpManagedTool = {
  toolId: string
  gatewayGroupId: string
  operationId: string
  operationKey: string
  name: string
  description?: string
  operationProtocol: 'HTTP' | 'RPC'
  inputSchema: Record<string, unknown>
  outputSchema: Record<string, unknown>
  codeServerId: string
  codeServerCode: string
  serverId: string
  serverCode: string
  codePermissions: string[]
  additionalPermissions: string[]
  effectivePermissions: string[]
  codeRiskLevel: McpToolRiskLevel
  minimumRiskLevel?: McpToolRiskLevel
  effectiveRiskLevel: McpToolRiskLevel
  idempotent: boolean
  enabled: boolean
  overrideRevision: number
}
```

## 15. 需要删除的旧代码

实现完成后必须删除，而不是保留旁路：

### 15.1 Starter

- 旧 `GatewaySchemaField.path`；
- `GatewayOperation.responseSchemaFields`；
- 方法注解内按字符串 Path 匹配字段说明的 `GatewaySchemaDescriptions` 实现；
- HTTP `bodySchema(parameters)` 的“有 Body 就丢弃 PATH/QUERY”逻辑；
- 未标注复杂参数默认归类 BODY 的逻辑；
- Java Schema 递归中退化为 Raw Class、固定深度截断的旧实现；
- Protobuf `GatewaySchemaField[]` Path Documentation Index；
- 所有旧注解测试夹具和示例。

### 15.2 Reporting 与 Admin

- `GatewayInterfaceDefinitionReport.Parameter`；
- Operation Report 的 `parameters` 字段；
- `attributes.parameters` 写入、读取和校验；
- Managed MCP 从扁平 Parameters 合成 Schema 的逻辑；
- `ToolInput.locations` 和所有 `inputLocations` DTO 映射；
- v1 Report 接收和 v1 Managed Tool 投影分支。

### 15.3 Contract、Engine 与 Runtime

- `McpRuntimeTool.inputLocations`；
- Rule JSON 中 `inputLocations` 的兼容处理；
- Runtime 的扁平参数名到 PATH/QUERY/BODY 的遍历绑定；
- 旧 Release Fixture 中 `inputLocations`；
- 任何根据 HTTP Method 推断参数位置的 MCP 调用代码。

### 15.4 Admin Web

- `McpManagedTool.inputLocations`；
- 仅支持内联递归、不解析 `$ref` 的旧 Schema Tree 分支；
- 任何 Managed Tool Schema 或参数绑定编辑入口；
- 旧注解示例和文档。

仓库最终执行全局搜索，除迁移说明和历史 Spec 外，生产代码与测试中不得残留 `responseSchemaFields`、旧 `GatewaySchemaField(path=...)` 或 `inputLocations`。

## 16. 迁移方案

### 16.1 代码迁移

1. 发布新的 Gateway Contract/Starter 编译依赖；
2. 所有业务 DTO 把字段说明迁移到 `GatewaySchemaField`；
3. 所有 Managed MCP HTTP 方法补齐完整 `requestSchemaFields` 和 `responseSchema`；
4. 所有 Managed MCP RPC Contract 使用唯一 `RPC_MESSAGE` 根声明；
5. RPC `.proto` 导入 `schema_options.proto` 并迁移字段说明；
6. 删除所有旧 Path 字段数组；
7. 每个 Provider 使用全新 `buildId` 构建并上报 v2 Definition；
8. 只有当前 Definition 为 v2 的 Operation 才允许进入新 Release。

### 16.2 数据迁移

本次不修改数据库表结构，因此不新增 Flyway 文件。

- 历史 v1 Definition 保持只读历史；
- 现有 Managed Tool Override 继续按稳定 Tool ID 生效；
- v1 Release Snapshot 保留审计价值，但新 Admin 禁止重新激活；
- 不把 v1 Schema 自动转换并写回数据库；
- 不迁移或恢复任何旧手工 Local Tool 配置。

### 16.3 部署顺序

本次不支持新旧版本混跑，必须使用维护窗口：

1. 备份数据库，冻结 Gateway Draft 写入和 Release 发布；
2. 构建已完成注解/Proto 迁移的 Provider 新版本；
3. 停止旧 Gateway Admin 和 Engine；
4. 同步部署 Gateway Contract 消费方、Admin 后端、Admin Web、Engine 和 MCP Runtime v2；
5. 部署 Provider 新版本并使用新 `buildId` 上报；
6. 验证 Catalog 中 HTTP/RPC 当前 Definition 均为 v2；
7. 创建全新 Gateway Release，不复用旧 Snapshot；
8. 验证无 Route 的 Managed Tool 仍带入 Operation；
9. 完成 HTTP 对象/List/Page/Map/Value 与 Unary RPC 对象/List 调用验收；
10. 恢复流量、Draft 写入和 Release 发布。

回滚只能整体回退 Admin、Web、Engine、Runtime、Starter 和 Provider，并恢复维护窗口前的发布状态。不能依赖 v2 代码读取 v1 Tool 参数契约。

## 17. 设计模式判断

采用 Adapter/Mapper，但不引入 Strategy 或继承层级：

- Java Schema Adapter：把 Jackson `JavaType` 和 Bean Validation 适配成统一 JSON Schema；
- Protobuf Schema Adapter：把 Descriptor 和 Field Option 适配成相同 JSON Schema；
- Response Wrapper Adapter：把真实返回类型与 Result/PageResult/Proto Wrapper 适配成完整响应 Schema；
- MCP Request Assembler：唯一负责把结构化 arguments 转成 `GatewayOperationCall`。

这些 Adapter 隔离了三种真实差异来源：Java 类型系统、Protobuf Descriptor 和响应包装语义。直接把所有分支继续堆进 HTTP/RPC Contributor 会重复泛型、递归和 Wrapper 规则。

不采用 Strategy/Abstract Factory/Builder/Chain：协议只有 HTTP 和 RPC 两种稳定来源，对象创建也不复杂；增加可插拔层级不会减少当前复杂度。Schema 的 `$defs/$ref` 是数据层面的递归组合，不需要额外 Composite 类体系。

## 18. 测试矩阵

### 18.1 注解与 Java Schema

- 注解 Target、Retention、默认值和全部枚举；
- Record、Bean Field、Getter、方法参数元数据；
- 嵌套对象、List、Map、数组、Enum、基本类型和格式类型；
- 泛型 Wrapper、嵌套泛型、接口 implementation；
- 自引用和互相引用 `$defs/$ref`；
- Jackson Rename/Ignore/Naming Strategy；
- Jakarta Validation 标准约束和冲突失败；
- example 合法/非法校验；
- Schema 深度、节点和字节上限。

### 18.2 HTTP Discovery

- PATH + QUERY + HEADER + `@RequestBody` 同时存在；
- `@RequestBody` OBJECT/LIST/MAP/VALUE；
- `@ModelAttribute` 复杂 Query 展开；
- ModelAttribute 不被识别为 BODY；
- 缺失、重复、错位置、错名称、错 Shape/Schema 失败；
- Required HEADER/COOKIE 注入规则；
- Multipart/PART/Streaming 对 Catalog 可见但 MCP 投影失败；
- `ResultRecord` 的 OBJECT/LIST/MAP/VALUE；
- `PageResultRecord` 的 records 和 page 完整 Schema。

### 18.3 RPC Discovery

- 唯一 RPC_MESSAGE 根声明；
- Descriptor scalar/repeated/map/enum/nested/oneof/well-known type；
- Proto Field Option description/format/required/example；
- Option 依赖进入 Descriptor Snapshot；
- Request/Response 根类与 Descriptor 不一致失败；
- Proto Result 的 OBJECT/LIST Payload；
- RPC 与 Gateway 幂等声明不一致失败；
- Streaming RPC 失败。

### 18.4 Reporting、Catalog 与 Release

- v2 Canonical Fingerprint 可复现；
- Admin 拒绝 v1 Report；
- Definition 不再持久化 `attributes.parameters`；
- HTTP Tool 只包含 path/query/body；
- RPC Tool 使用完整 Message；
- outputSchema 保留完整 Wrapper；
- `$defs` 可达裁剪与非法 `$ref` 失败；
- 无 Route 的 Managed Tool 正确带入 Runtime Operation；
- 现有严格 Override 在 Tool ID 不变时继续生效。

### 18.5 Runtime

- HTTP path/query/body 精确构造；
- Query array 的重复参数编码；
- Body OBJECT/LIST/MAP/VALUE 原样传递；
- 未知根字段和 header/cookie/part 输入失败；
- RPC arguments 整体映射为 Protobuf 请求；
- Task Snapshot 不含 Authorization/Cookie；
- 新 Rule JSON 不存在 `inputLocations`。

### 18.6 Admin Web

- 位置分组 Request Schema；
- `$defs/$ref` 解析和循环保护；
- 对象、数组、Map、基础值、Required、约束和示例展示；
- ResultRecord/PageResultRecord 完整响应展示；
- Managed Tool 只读 input/output Schema 预览；
- TypeScript 类型不存在 `inputLocations`；
- Managed Tool 无 Schema/绑定编辑入口；
- Vitest、TypeScript、ESLint 和生产构建通过。

### 18.7 端到端验收

- HTTP Managed Tool：PATH + QUERY +复杂 Request Body，响应 `ResultRecord<复杂对象>`；
- HTTP Managed Tool：复杂 Query，响应 `ResultRecord<List<T>>`；
- HTTP Managed Tool：分页响应 `PageResultRecord<T>`；
- HTTP Managed Tool：Result Payload 为 Map 和基础类型；
- Unary RPC Managed Tool：复杂 Message 请求，复杂 Result Message 响应；
- Unary RPC Managed Tool：List Result Message 响应；
- 控制面只能禁用、改 Server、追加权限或提高风险；
- Remote MCP、Resource、Prompt、App、Task 和 Approval 无回归。

## 19. 验收标准

1. `GatewaySchemaField` 只在具体 Java 属性/参数上声明，不再使用字符串 Path。
2. `GatewayOperation.requestSchemaFields` 被保留，并能完整表达 PATH、QUERY、HEADER、COOKIE、BODY、PART 和 RPC_MESSAGE。
3. 复杂 `@RequestBody` 与复杂 Query/ModelAttribute 被严格区分并通过签名校验。
4. HTTP/RPC 的对象、List、Map、基础类型和嵌套泛型生成正确 JSON Schema。
5. `ResultRecord`、`PageResultRecord` 和 Proto Result 输出完整 Wrapper Schema，不只输出 Payload。
6. DTO `data` 内同时存在 List、Map、基本类型和复杂对象时，Catalog、Managed Tool 和 Admin Web 展示一致。
7. HTTP MCP arguments 固定为 `path/query/body`，RPC arguments 固定为完整 Message。
8. 生产代码、测试和前端类型中不存在 `inputLocations`。
9. Managed Tool Schema 完全由 Operation v2 投影，控制面没有任何手工编辑入口。
10. v1 Report/Release 不被新版本兼容消费，所有 Provider 使用新 `buildId` 上报。
11. 旧字段数组、扁平参数绑定、Raw Class 泛型丢失和旧 RPC Path Documentation 代码已删除。
12. 前后端、Engine、Starter、Contract 和测试夹具在同一次破坏性版本中完成切换。

## 20. 本轮审核项

1. 是否确认 `requestSchemaFields` 使用完整声明，Managed MCP 方法不允许局部省略？
2. 是否确认 `responseSchemaFields` 删除，统一改为单一 `responseSchema`？
3. 是否确认 `GatewaySchemaField` 删除 `path`，字段说明迁移到 DTO 属性/参数？
4. 是否确认 HTTP requestSchema 使用位置分组，Managed MCP 只保留 `path/query/body`？
5. 是否确认 RPC 字段说明改用 `schema_options.proto` 自定义 Field Option？
6. 是否确认 Reporting 升级到 v2，并删除独立 `Parameter`/`attributes.parameters`？
7. 是否确认 `inputLocations` 从后端、Rule、Runtime、API、前端和测试中彻底删除？
8. 是否确认不做兼容、不自动改写历史 Definition、不允许旧 Release 重新激活？
