# REST、CQRS、GraphQL 与 OpenAPI 3 API 设计规范

只要外部 HTTP API 被判定为 `Affected`，就必须完整读取本参考。本参考是 `references/interface-contract-design.zh-CN.md` 的专项深化，两份参考同时生效。目标是让第 9 章成为可直接实施的 API 契约，而不是路由或注解清单。

## 1. 权威来源与参考项目吸收原则

按以下优先级使用依据：

1. 用户明确需求与已接受的有效 Spec；
2. 当前仓库真实 Controller、Spring 配置、安全配置、DTO、错误/结果类型、Jackson 规则、GraphQL SDL、客户端、测试和依赖管理；
3. 第 18 节列出的官方协议与框架文档；
4. 外部示例仓库只能作为非规范性设计输入。

用户指定的 [Mshuyan/swagger](https://github.com/Mshuyan/swagger) 仓库可以吸收以下思想：

- 在 API 文档中体现 Bean Validation 约束；
- 记录成功/错误包装、安全方案和授权要求；
- 让可复用 API 契约可审核，并在真实网关确有需要时支持文档聚合；
- 允许配置文档是否暴露。

该仓库不能作为新增设计的实现基线。它包含 Springfox、`Docket`、Swagger 2 注解、较旧的 Spring Boot 示例、混合代际注解和手工聚合方案。新增或现代化 Spring Boot API 必须：

- 使用当前 Spring Boot 兼容、且由仓库或 BOM 管理的 `springdoc-openapi-starter-*`；
- 只使用 `io.swagger.v3.oas.annotations.*`；
- 禁止新增 `io.swagger.annotations.*`、Springfox `Docket`、`@EnableSwagger2`、`@EnableOpenApi`、`ApiImplicitParam`，也禁止为了让 Swagger UI 展示文档而伪造接口；
- 已有网关、Component 或文档目录能够发布服务文档时，不得自行手写网关聚合；
- 不得只为把注解从 Controller 挪走而新增一个纯注解 Java 接口。只有仓库已经存在 Facade/API 契约接口，并且测试证明 Spring 路由、校验和 OpenAPI 注解能够按预期继承时，才可复用该接口。

如果受影响旧模块仍然使用 Springfox，必须明确写成 `Legacy/Compatibility`，不能静默混用两代方案。除非用户已经批准，否则迁移到 springdoc 是另一个受影响变更。

## 2. 先选择协议，再设计端点

每个受影响外部操作必须且只能分类为以下一种：

| API 风格 | CQRS 角色 | 适用场景 | 不能仅因以下理由使用 |
| --- | --- | --- | --- |
| REST 资源查询 | Query | 稳定资源/集合表示符合 HTTP 资源语义 | GET 看起来熟悉 |
| REST 资源命令 | Command | 创建、完整替换、局部更新、删除或资源生命周期动作具有清晰 HTTP 语义 | 每个 Service 方法都需要 URL |
| REST 任务命令 | Command | 业务动作无法诚实表达为普通资源状态替换 | 使用动词方便 |
| GraphQL 查询 | Query | 客户端需要跨关联数据选择图形/投影，并且项目已经支持或明确采用 GraphQL | 返回值是嵌套 JSON |
| GraphQL 变更 | Command | 任务型写操作属于已批准的 GraphQL Schema | 只有一个 `/graphql` URL 看起来简单 |
| GraphQL 订阅 | Subscription/读取流 | 需求已证明需要实时流，并具备传输与生命周期支持 | 觉得轮询不优雅 |

Spec 必须说明所选风格为什么适合消费者目标，以及直接复用已有方案为什么不足。REST 与 GraphQL 只有在职责明确时才能共存：必须写明各自服务哪个消费者/用例、哪个业务能力是权威来源、校验/安全/错误如何保持一致，以及是否共同委托给同一个应用 Service。不得为每种协议复制业务逻辑或形成独立写语义。

OpenAPI 描述 REST HTTP 操作；GraphQL SDL 和操作文档描述 GraphQL 字段。不得用 GraphQL Resolver 上的 Swagger 注解替代 SDL，也不得把通用 `POST /graphql` 传输操作拆成伪 REST 资源。

## 3. 不制造形式主义架构的 CQRS 语义门禁

即使是普通三层项目，每个 API 操作也必须声明 CQRS 角色：

- **Query** 只读取数据，不得创建持久业务状态、发布业务事件或暗中执行 Command。指标等附带可观测性不属于业务写入。
- **Command** 表达业务任务或资源状态变化。当业务转换具有 Guard 或副作用时，应使用 `approveInvoice` 这类任务语言，而不是 `setStatusApproved` 这类字段赋值语言。
- **Subscription** 交付经过批准的数据流，必须定义数据源、授权、顺序、续传/重连、背压和终止行为。

CQRS 分类本身不能证明需要拆库、只读副本、事件溯源、消息总线、Handler 继承树或新增模块。按当前证据选择最小级别：

| 级别 | 设计 | 采用条件 |
| --- | --- | --- |
| L0 | 明确把操作分为 Query 或 Command；继续共享现有 Service/数据模型 | 简单 CRUD 和聚焦改动的默认值 |
| L1 | 分离 `Query`/`Command` 边界对象和应用方法，共享数据库 | 存在不同的校验、权限、投影或事务语义 |
| L2 | 分离读取投影/模型，共享或复制持久化 | 有可测量的查询形状、性能或安全需要 |
| L3 | 分离存储、消息与最终一致性，必要时事件溯源 | 有明确扩展性/可用性/历史要求，并完成同步、延迟、恢复和运维设计 |

选择 L0 以上级别时，必须记录证据、一致性模型、失败方式、额外状态/调用、迁移、可观测性、测试，以及更低级别为什么不足。不得新增一个仅用于取得随后原样传给 Command 参数的 Query 接口；Command 必须自行派生并权威校验服务端拥有的上下文。

## 4. REST 资源与 URI 规则

每个 REST 操作都必须：

- 使用稳定资源标识设计 URI，通常为小写复数名词，例如 `/api/v1/orders/{orderId}`；
- 只有当子资源确实受父资源约束且关系稳定时才嵌套，避免过深路径；
- 当基础设施拥有租户、操作者、权限、服务端时间、派生状态和服务端配置时，不得让调用方在 Path/Body 中控制它们；
- 查询参数用于过滤、搜索、排序、分页、字段投影和可选表示控制，不能暗中写数据；
- 普通 CRUD 禁止 RPC 式动词。当任务命令具有独立业务语义、审计、幂等或生命周期时，可使用 `POST /orders/{orderId}/cancellations` 这类从属动作/资源；
- 不得把包名或类名暴露为公开资源词汇；
- 必须结合类/方法 Mapping、context path、网关前缀和 API 版本验证完整应用路由，不能发明环境 Host。

### 4.1 HTTP 方法语义

| 方法 | 必需语义 | 常见成功状态 | 禁止设计 |
| --- | --- | --- | --- |
| `GET` | 安全、只读，并说明缓存语义 | `200`；有条件请求时 `304`；单资源不存在为 `404` | 请求体、隐藏 Command、服务端状态转换 |
| `POST` | 创建从属资源或执行非幂等/任务 Command | 创建用 `201` + `Location`；或 `200`；真实异步工作用 `202` | 没有 Key/去重契约却宣称幂等 |
| `PUT` | 对已知 URI 做完整替换，幂等 | `200` 或 `204`；支持按 URI 创建时才用 `201` | 把局部更新说成 PUT |
| `PATCH` | 明确局部更新媒体类型与字段语义 | `200` 或 `204` | 缺失/null/重置行为含糊 |
| `DELETE` | 按文档约定删除/停用资源，请求效果幂等 | `204` 或仓库惯例 | 未说明异步删除仍返回完成成功 |
| `HEAD` | 与 GET 元数据一致但无内容 | `200`/`304` | 独立业务逻辑 |

HTTP 方法、状态、Header、缓存和条件请求行为必须一致。避免给 `GET`、`HEAD` 以及通常的 `DELETE` 定义 Body，因为它们缺少可靠互操作语义。

### 4.2 状态码与 Header 契约

逐项记录适用结果，不能只写一个通用 `default`：

- `200 OK`：成功返回表示，或同步 Command 结果；
- `201 Created`：已创建资源，说明 `Location` 和返回表示；
- `202 Accepted`：工作尚未完成，定义状态资源/字段、轮询或回调、过期、终态失败和重试；
- `204 No Content`：没有响应体，生成的 OpenAPI content 也必须为空；
- `400 Bad Request`：语法/类型错误或仓库标准校验映射；
- `401 Unauthorized`：认证缺失/无效，以及适用的 `WWW-Authenticate`；
- `403 Forbidden`：已经认证但无权限，不能泄露跨租户资源是否存在；
- `404 Not Found`：按仓库信息披露策略表示资源不存在；
- `409 Conflict`：当前状态、唯一约束、幂等 Key 复用或业务转换冲突；
- `412 Precondition Failed`：采用 HTTP 乐观并发时 `If-Match` 等前置条件失败；
- 实际适用时还要定义 `415 Unsupported Media Type`、`422 Unprocessable Content`、`429 Too Many Requests` 和 `5xx` 依赖/内部结果。

只有实现与消费者确实支持时，才设计 `Location`、`ETag`、`If-Match`、`Idempotency-Key`、关联/追踪 ID、分页 Link/Cursor、`Retry-After`、缓存 Header 和弃用 Header。Header 名称、所有者、格式、必填性、传播、日志/脱敏和重试影响都属于入参/出参表。

### 4.3 查询、分页和兼容性

列表/搜索操作必须定义：

- 过滤运算、组合、blank/null/unknown 处理以及权限、租户范围；
- 可排序字段、方向、默认值和确定性 Tie-breaker；
- Offset 页码基准/默认/上限/Count 语义，或 Cursor 编码/过期/稳定顺序语义；
- 除非已有契约不同，空结果必须是非 null 空集合；
- 翻页期间并发变化的影响，以及是否承诺快照一致性；
- 最大页大小、字段投影、查询/索引证据和成本控制；
- 新增/删除/改名兼容、未知字段行为、版本/弃用策略和具名消费者。

不得自动新增 API 版本。遵循仓库现有兼容策略，把破坏性公开变更作为重大用户决策。

## 5. 请求、校验与序列化契约

对每个 REST 请求或 GraphQL Input，追踪每个值来自 Path、Query、Header、Cookie、Body、Multipart、GraphQL Variable、认证主体、租户上下文、配置、服务端时钟还是派生数据。

Spec 必须定义：

- JSON/Property 名与 Java 语义类型（`Command`、`Query`、`Request`、DTO、VO 等）；
- 传输类型/格式、必填、缺失/null/blank/empty 行为、默认值、min/max、长度、精度/Scale、Pattern、Enum、集合边界/唯一性、Trim/大小写、时区和未知字段行为；
- 复用输入的嵌套 `@Valid`、方法/类 `@Validated` 和 Validation Group 选择；
- 规范化时点与权威重校验，适用时复用已有 `ValidatorUtils`、Jakarta Validation 和 libphonenumber；
- Jackson 字段名、Include、格式、枚举/日期行为、敏感字段暴露和兼容性；
- 精确的校验错误映射，并保证字段路径可安全暴露给消费者。

OpenAPI 注解描述契约，不能替代运行时 Bean Validation。必须把生成约束与校验注解、跨字段自定义规则进行比对。不能写代码不会执行的文档约束，也不能依靠生成文档保护或校验操作。

## 6. 响应与错误契约

复用仓库真实响应/错误基础设施，不得只为 Swagger 新建第二套 Wrapper。

- 仓库已有稳定 Egon/公共 `Result` 或错误 Envelope 时，记录准确 HTTP 映射、字段、nullability、Code 和 Jackson 形状。
- 新外部 REST API 没有既有应用 Envelope 时，优先使用 Spring 对 RFC 9457 `ProblemDetail`/`ErrorResponse` 的支持，不得再发明错误格式。
- 采用 RFC 9457 会破坏已有消费者时，保持当前 Envelope 并记录兼容决策；没有明确协商/版本策略时，不得在不同 Controller 中混用格式。
- 禁止暴露 Stack Trace、异常类名、SQL、Secret、Token、租户存在性或内部依赖细节。

每个响应都必须给出完整 `jsonc`，每个 Key 都有行尾含义注释；nullability/来源/精度需要更细说明时再增加字段表。每个错误行必须包含条件、HTTP 状态、稳定业务码或 GraphQL `extensions.code`、形状、可重试性、前端处理、日志/Trace 行为和测试。

## 7. OpenAPI 3 文档契约

对每个受影响 REST 操作，第 9 章必须像设计运行时路由一样设计生成的 OpenAPI Operation。

### 7.1 必需 Operation 属性

| OpenAPI 元素 | 规则 |
| --- | --- |
| `tags` | 稳定的资源/能力分组，不能使用包名 |
| `summary` | 简短、面向消费者的动作，一句话 |
| `description` | 业务行为、关键 Guard/副作用、幂等/一致性和消费者说明，不能重复实现代码 |
| `operationId` | 显式、全局唯一、稳定的 lowerCamelCase 标识，不能依赖生成器处理方法重名 |
| `parameters` | 精确 name + `in`、Schema/Format、required/default/example、说明；每个 Path 参数都 required 且与 URI 模板一致 |
| `requestBody` | required、媒体类型、Schema、Example；无 Body 操作不得添加 |
| `responses` | 每个实质成功/错误状态都有说明、媒体类型、Header 和准确 Schema；禁止不解释的 `default` |
| `security` | 精确已注册 Scheme 名和 OAuth Scope/Permission；公开操作显式覆盖全局 Security |
| `deprecated` | 只有同时写明替代和移除策略时才能为 true |

Schema 必须定义稳定名称、说明、示例、required/null 语义、format、enum、range、length、必要的 pattern、readOnly/writeOnly；只有传输契约真实需要时才能使用 discriminator/composition。禁止暴露持久化对象或内部多态结构。

OpenAPI 3.1 或 3.0 必须服从已安装 springdoc/swagger-core 的兼容性和当前消费者。生成器或下游工具只产生/接受 3.0 时，不得要求 3.1 特性。没有检查 Spring Boot、Dependency Management 和仓库兼容性前，不得在 Spec 中硬编码库版本。

### 7.2 单一事实源与生成物

必须声明一种事实源模型：

- **Code-first**：Spring Mapping、Java 边界类型、Bean Validation、Jackson 和 OpenAPI 注解是权威；生成 `/v3/api-docs` 并做差异校验。
- **Contract-first**：已批准且入库的 OpenAPI 文档是权威；校验生成 Server/Client 集成和实现一致性。不得另行维护冲突注解。

Code-first 仓库不得自行提交复制的 JSON/YAML，除非当前构建/发布规则要求。需要提交时，必须定义再生成责任和漂移门禁。

## 8. springdoc 依赖与配置规则

提出依赖前，检查当前 Spring Boot 代际、Web MVC/WebFlux、Dependency Management、安全、Actuator/网关拓扑和现有文档设置。

使用匹配的 Starter：

- MVC + UI：`org.springdoc:springdoc-openapi-starter-webmvc-ui`；
- MVC 仅 API：`org.springdoc:springdoc-openapi-starter-webmvc-api`；
- WebFlux + UI/API：对应 `springdoc-openapi-starter-webflux-*`。

优先使用仓库/BOM 管理版本。新增模块不得同时加入 Springfox 和 springdoc。Starter 已经提供兼容注解依赖时，不得再次添加 Swagger Core Annotations；只有仓库依赖政策要求显式受管声明时例外。

配置设计必须覆盖：

- 按环境启用 API 文档/UI；
- 修改默认值时，写清 `/v3/api-docs`、YAML、Swagger UI、Management Port 和分组文档路径；
- 只有真实存在 public/internal/module Audience，且包含规则互不重叠时才使用 `GroupedOpenApi`；
- 扫描 Package/Path、隐藏内部端点和 Management Endpoint；
- 代理/网关后的 Server URL 处理，禁止硬编码环境 Host；
- Spring Security 对文档/UI 与 OAuth Redirect Path 的访问规则；
- 生产环境的暴露、认证、网络限制或完全禁用；
- 所有 Spring Boot 环境配置具有相同 Key 集合，值可以不同。

文档聚合属于架构功能。网关/目录必须设计发现来源、服务标识、文档 URL、认证传播、超时、部分服务失败、陈旧缓存、版本兼容、Component Name/operationId 冲突和生产访问。不能在没有证明适配当前网关时复制静态 `SwaggerResourcesProvider` 示例。

## 9. OpenAPI 3 注解规范

只允许 `io.swagger.v3.oas.annotations.*`。必须明确处理 Import 冲突，特别是 Spring `org.springframework.web.bind.annotation.RequestBody` 与 OpenAPI `io.swagger.v3.oas.annotations.parameters.RequestBody`。

### 9.1 注解放置矩阵

| 目标 | 注解 | 必需设计内容 |
| --- | --- | --- |
| API 元数据配置 | `@OpenAPIDefinition(info = @Info(...), tags = ...)` | title、version、description；contact/license 只有仓库负责时才写；不得包含 Secret 或环境 Host |
| 安全配置 | `@SecurityScheme` | 来自真实安全配置的精确 Scheme Name/Type、Bearer Format 或 OAuth/OpenID Flow 与 Scope |
| Controller 或已有 API 契约类型 | `@Tag` | 稳定能力名与消费者可读说明 |
| REST 操作 | `@Operation` | `summary`、`description`、显式唯一 `operationId`，以及适用的 tag/security/deprecated |
| Path/Query/Header/Cookie 参数 | `@Parameter` | description、required、example、Schema 约束；Spring 注解能准确推导的结构优先推导，语义缺失才补注解 |
| 已有查询参数聚合对象 | springdoc `@ParameterObject` | 把真实 Flat Query/Form Object 展开为 Parameter；不能用于 JSON Body，也不能只为文档制造 Carrier |
| 请求体 | OpenAPI `@RequestBody` | description、required、`@Content`，只有推导不完整时再显式 Schema/Example |
| 每个结果 | `@ApiResponse` / `@ApiResponses` | 精确 Response Code、Description、Header、`@Content`、Schema/Example |
| Media/Schema | `@Content`、`@Schema`、`@ArraySchema` | 媒体类型和真实 Wrapper/Payload Schema；数组使用 `@ArraySchema`，不能与冲突的 Array/Schema 声明并存 |
| Example | `@ExampleObject` | 有效协议 JSON、具名用途、不含 Secret/真实个人数据；Example 必须与 Validation/Schema 一致 |
| 响应 Header | `io.swagger.v3.oas.annotations.headers.Header` | 对 `Location`、`ETag`、Rate/Retry、Correlation 或 Deprecation Header 定义准确名称、说明、Requiredness/Schema/Example |
| 边界模型/字段 | `@Schema` | 只在表达传输契约需要时提供 name/description/example/format/access/allowable values |
| 安全操作 | `@SecurityRequirement` | 精确已注册 Scheme Name 与 Scope，不能拿描述性 Permission 字符串冒充 Scheme |
| OAuth/OpenID Scheme | `@OAuthFlows`、`@OAuthFlow`、`@OAuthScope`，位于 `@SecurityScheme` 内 | 精确真实 Endpoint、Flow、Scope 名/含义和 Client Audience；禁止 Credential |
| Operation/Server 关系 | `@Link`、`@Callback`、`@Server` | 只有真实 Follow-up/Callback/Server Contract 才使用；避免环境特定 Host 和装饰性 Link |
| 已有 Functional Web Route | springdoc `@RouterOperation` / `@RouterOperations` | 准确 Path、Method、Operation Bean Method、Consumes/Produces、Parameter、Response 与 Security；只有仓库真实使用 RouterFunction 时才使用 |
| 隐藏内部项 | `@Hidden` | 只有端点必须存在但不能进入当前发布 Audience 时使用 |

避免注解噪音。Spring MVC Mapping、Java 类型、Bean Validation 和 Jackson 已提供结构事实；注解用于补充或修正消费者语义，不应逐字重复明显类型。反过来，显式 `operationId`、业务说明、实质 Response/Error、安全、Wrapper Schema、Example 和非显然序列化不能因为 springdoc 可以“推断”就省略。

执行以下放置规则：

- `@Operation` 以及操作专属 `@ApiResponse`/`@SecurityRequirement` 必须放在真实 Mapping Operation，不能放到无关 Helper；
- 非 Body 参数使用 `@Parameter`，Body 使用 OpenAPI `@RequestBody`；不能把 Body 当成 Implicit Parameter；
- springdoc `@ParameterObject` 只能用于字段确实来自 Query/Form 的已有参数聚合对象；必须通过生成文档确认 Nested Object 和 Naming 行为；
- Spring Mapping/Validation Annotation 是运行时权威，OpenAPI Annotation 用于补足文档语义；发生 `RequestBody` Import 冲突时必须显式限定；
- `204` 不得定义 `@Content`；数组使用 `@ArraySchema`；Generic Wrapper 必须具体化，使生成 Component 包含真实 Payload；
- 字段 Requiredness/Nullability 由已安装注解版本、Bean Validation 与 Jackson 共同表达。版本支持时优先当前 `requiredMode` API，不能从旧示例复制已弃用 Attribute；
- 只有值和行为确实一致时，才能把 Metadata、Security Scheme、共享 Problem Response、Header 或 Schema 放入中央 Component；
- `summary` 保持简短，详细 Markdown 行为放入 `description`，Java 重构不能随意改变 `operationId`；
- `@Hidden` 用于从某个发布 Audience 排除真实内部 Operation，不能代替 Security 或 Package Scan 职责。
- Functional Endpoint 必须保持 `RouterFunction`、Handler Method 和 `@RouterOperation` 身份一致，并有生成契约测试；不能再添加一个并行 Annotated Controller。

### 9.2 REST Controller 示例

以下只是形状示例，不是仓库证据。真实 Spec 必须用已经核实的名称、Wrapper、状态、错误、权限和路径替换。

```java
@Tag(name = "Orders", description = "Order query and command operations")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final @Qualifier("orderService") OrderService orderService;

    @Operation(
            summary = "Create an order",
            description = "Validates authoritative references and creates one order idempotently.",
            operationId = "createOrder",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Order created",
                    content = @Content(schema = @Schema(implementation = CreateOrderResultResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed",
                    content = @Content(schema = @Schema(implementation = ValidationProblemResponse.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "Idempotency or state conflict",
                    content = @Content(schema = @Schema(implementation = ConflictProblemResponse.class)))
    })
    @PostMapping
    public ResponseEntity<CreateOrderResultResponse> createOrder(
            @Parameter(description = "Stable key for retries of the same command", required = true)
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @org.springframework.web.bind.annotation.RequestBody CreateOrderCommand command) {
        // 仅为形状示例；生产逻辑必须位于既有应用/Service 边界。
        throw new UnsupportedOperationException("example only");
    }
}
```

真实 Spec 必须说明是否由已有 API/Facade 接口持有注解。不得只为移动注解行而创建 `OrderApi`，也不得拆开运行时 Mapping 和文档导致二者漂移。

### 9.3 Schema 示例

```java
public record CreateOrderCommand(
        @Schema(description = "Tenant-visible customer identifier", example = "12001")
        @Positive Long customerId,
        @Schema(description = "Unique order lines; one through one hundred entries")
        @NotEmpty @Size(max = 100) List<@Valid CreateOrderItemCommand> items) {
}
```

必须比对生成的 Required List、数值/集合约束、JSON 名、Example、Enum/Date Format 和真实运行时校验。泛型响应 Wrapper 必须确认 springdoc 能解析具体 Payload；只有生成结果确实错误时，才使用显式 `@Content(schema = @Schema(implementation = ...))` 或仓库标准 Customizer。不能在没有工具限制证据与批准约定时，为每个端点制造一个假的 Wrapper 子类。

## 10. GraphQL Schema 与操作规范

即使 Resolver 使用注解实现，GraphQL 设计仍以 Schema 为先。Spec 必须写明已入库 SDL 路径、受影响 Schema Type/Field、Resolver Symbol 和消费者 Operation Document。

### 10.1 Schema 规则

- Object/Interface Type 使用名词；Mutation 使用任务型动词。
- Query Field 只读；Mutation Field 是 Command，承担业务校验/事务/副作用。
- Mutation 使用专用 Input Type；不能为了方便暴露持久化 Entity 或把输出类型复用成输入。
- 有意识地决定 Nullability。`!` 是兼容承诺；说明 Nullable Field 和 Partial Data 行为。
- 闭合集合使用 Enum；只有证明需要时才使用 Custom Scalar，并定义精确 Coercion/Serialization；ID 具有稳定、不透明语义。
- 使用 `@deprecated(reason: ...)` 时定义替代字段、消费者迁移和移除策略。
- 禁止无界嵌套集合和递归图扩张。
- 当仓库约定支持且图数据需要稳定演进时，优先 Cursor/Connection 分页，定义 Edge/Node/Cursor/PageInfo、稳定排序、限制和并发变化；简单有界列表没有需求时，不得制造 Relay Wrapper。

### 10.2 操作契约

每个受影响字段都必须提供：

1. SDL Fragment，包含 Description、Input/Output Type、Nullability、Enum/Scalar 和 Deprecation；
2. 具名消费者 Operation Document，包含 Variable 和完整代表性 Selection Set；
3. Variable 表，包含类型、必填/null/default、校验、含义、示例和来源；
4. 完整成功与错误/Partial Data `jsonc` 传输示例；
5. 精确 `Query.field`、`Mutation.field` 或 `Subscription.field` 身份和 Spring Resolver Symbol；
6. HTTP 端点认证与 Field/业务 Service 授权；
7. Mutation 的事务、幂等、并发、副作用和一致性；
8. Query 的 Fetch Plan、Batching、Pagination、Depth/Complexity/Cost 限制和 N+1 防护；
9. 兼容性、消费者文档、Schema Check、Resolver Test 和 Transport Test。

按项目策略，把顶层 GraphQL `errors` 用于 Parse、Validation、Coercion、Authorization/Execution 和意外失败。定义安全 `extensions.code`、Field/Argument Path、可重试性、Partial `data`、脱敏和前端处理。不能把所有领域校验失败一律放进 HTTP `400`；GraphQL 执行可能返回包含 Error 和 Partial Data 的协议响应。不得把业务结果藏进没有稳定 Typed/Error 语义的通用成功 Payload。

### 10.3 Spring for GraphQL 映射

采用 GraphQL 已获批准时，使用 Spring Boot GraphQL Starter 和项目现有配置。标准映射为：

| GraphQL 契约 | Spring 映射 | 必需设计证据 |
| --- | --- | --- |
| `Query.field` | `@QueryMapping` 或准确 `@SchemaMapping(typeName = "Query", field = "...")` | SDL Field、Resolver、Query Object/Argument、只读 Service 调用 |
| `Mutation.field` | `@MutationMapping` | SDL Input/Output、`@Argument @Valid`、复用对象的 `@Validated` Group、Command Service、事务/幂等/错误 |
| 嵌套字段 | `@SchemaMapping` | Parent/Source Type、加载职责、授权、Null/Error 行为 |
| 批量嵌套字段 | `@BatchMapping` 或注册 DataLoader | Key、稳定结果关联、Batch Size、缺失项、顺序、Cache Scope、N+1 测试 |
| Subscription | `@SubscriptionMapping` | Publisher/Source、授权、Transport、背压、重连/续传、清理 |

只保护 `/graphql` 路径不足以完成字段授权，因为所有操作共享一个 URL。必须在 Service Method 等业务边界复用 Spring Security（例如项目既有 `@PreAuthorize` 规则），并说明字段披露规则。授权不能只写在 Description 中。

### 10.4 GraphQL 传输

Spring for GraphQL 与 GraphQL-over-HTTP 草案共同约束传输。必须根据已安装版本记录实际支持的 Method/Media Type。POST 与 JSON Request Envelope 是基线；框架支持时设计并测试 `application/graphql-response+json`。GET、Persisted Query、WebSocket、RSocket、Multipart Upload、Subscription 和 Incremental Delivery 都不是默认能力，必须另有明确支持与安全/缓存分析。

GraphQL-over-HTTP 文档仍处于草案阶段。把其版本作为兼容性输入，记录已安装 Spring 行为，没有测试不得宣称草案特有行为。

### 10.5 GraphQL 示例

```graphql
"Order operations available to the authenticated tenant."
type Query {
  order(id: ID!): Order
}

type Mutation {
  createOrder(input: CreateOrderInput!): CreateOrderPayload!
}

input CreateOrderInput {
  customerId: ID!
  items: [CreateOrderItemInput!]!
  idempotencyKey: String!
}

type CreateOrderPayload {
  order: Order!
  traceId: String!
}
```

```graphql
mutation CreateOrder($input: CreateOrderInput!) {
  createOrder(input: $input) {
    order {
      id
      number
      status
    }
    traceId
  }
}
```

真实 Spec 必须展开所有受影响的引用 Type 和 Variable；这个缩略示例只用于说明 SDL 与消费者 Operation Document 必须分开。

## 11. REST 与 GraphQL 共存规则

两种协议暴露同一能力时：

- 指定一个应用 Service/Command/Query 行为作为业务事实源；
- 禁止协议 Adapter 实现不同的校验、权限、事务、状态或事件规则；
- 从相同 Typed Business Outcome 映射 REST 状态/错误 Envelope 与 GraphQL Error，不能把 Transport Concern 泄露进领域逻辑；
- 定义跨协议的幂等范围，不能让同一 Command 绕过去重保护；
- 定义共享 Rate/Cost Policy、Audit Identity、Tenant Source、Observability Correlation 和敏感字段规则；
- 测试等价业务结果，同时分别测试协议序列化与错误；
- 写明两种协议对具名消费者的独立必要性。没有独立价值时只选一种，不维护两套 Facade。

## 12. 安全与文档暴露

逐操作/字段说明认证、授权、租户隔离、所有权检查、敏感字段、Rate/Cost Limit、审计和错误披露，再映射到运行时与文档：

- `@SecurityScheme` 定义真实机制；`@SecurityRequirement` 引用其精确注册名与 Scope；
- HTTP Security 保护 REST/Docs/GraphQL Path；Method/Service Security 保护业务操作和 GraphQL Field；
- Example 使用合成、非秘密值；
- Swagger UI OAuth 配置不得在源码或公开输出中包含 Client Secret；
- 生产文档/UI 是否暴露必须是显式配置/安全决策，不能依赖偶然默认值；
- GraphQL Introspection/GraphiQL 按环境和 Audience 决定；关闭它们不能代替授权；
- Schema Description 与 Error Detail 不能泄露内部拓扑或跨租户存在性。

## 13. API 文档与代码组织

根据仓库证据选择注解归属：

1. 已由 Controller 持有：继续让 Mapping 和 OpenAPI 注解同处，除非本地风格另有规定。
2. 已有 Facade/API 接口持有：只有它本来就是公开应用契约，且继承行为有测试时才能使用。
3. 中央复用组件：`@OpenAPIDefinition`、`@SecurityScheme`、可复用 Schema/Response 或 Customizer 只承载真正共享事实。

不得仅为缩短注解而新增并行 Endpoint Interface、Response Subclass、Annotation Constant 或 Customizer。只有当复用确实能防止多个操作漂移，且 springdoc 能正确表示 Generic/Wrapper 契约时才选用。自定义 `OpenApiCustomizer`、Model Converter 或 Plugin 必须说明生成器缺口、范围、顺序、兼容性、测试和维护者。

## 14. 生成契约校验

Spec 必须从仓库识别准确可执行命令，不能假设工具存在。Plan 后续需要按顺序落实实现与这些检查。

受影响 REST API 至少设计：

- 编译和聚焦 Controller/Validation/Security/Error 测试；
- 生成/读取真实 `/v3/api-docs`，没有实际运行时不得声称 Live 验证；
- 使用仓库已有 Plugin/Tool 解析并校验 OpenAPI，或说明增加工具的必要性；
- 断言 Path、Method、唯一 `operationId`、Parameter、Required Field、Schema/Format、Status、Header、Error Model、Security 和 Deprecated；
- 仓库有入库 Baseline 或消费者契约时比较差异；
- 校验每个环境按预期暴露或关闭 Swagger UI/文档；
- 校验未泄露内部 Endpoint/Schema 或 Secret。

GraphQL 至少设计：

- 通过已有测试校验 SDL 和应用启动时 Schema Wiring；
- 使用 `GraphQlTester` 与具名 `.graphql`/`.gql` 文档测试 Query、Mutation、Validation、Authorization、Error、Partial Data、Pagination、Batching 和 N+1 敏感路径；
- 测试已安装 Spring 版本支持的 HTTP Media Type/Status；
- 按仓库规则 Diff/检查 Schema 或消费者文档；
- 适用时校验 Complexity/Depth/Cost Limit 与字段安全。

静态源码检查不是实时运行证据。Spec 可以规定后续运行时检查，但最终结论必须区分设计完整性和实现/运行验证。

## 15. 第 9 章阻断型 API 门禁

只要有外部 API 受影响，第 9.4 节必须逐行包含以下全部门禁。每一行都需要根据仓库证据和完整设计人工判断：

| 门禁 ID | 阻断问题 |
| --- | --- |
| `API-GATE-001` | 每个 API 是否必要、原子化、服务独立消费者目标，并且没有 Fetch-then-forward？ |
| `API-GATE-002` | REST/GraphQL 选择和 Query/Command/Subscription 角色是否明确且最小充分？ |
| `API-GATE-003` | 所有受影响 REST 操作是否满足资源、Method、Status、Header、Idempotency、Pagination 和 Compatibility 语义？ |
| `API-GATE-004` | 所有受影响 GraphQL Field 是否具有完整 SDL、Operation、Nullability、Resolver、Batching/Cost、Security 和 Error 语义？ |
| `API-GATE-005` | 运行时校验、Jackson/GraphQL Coercion、Schema、Example 和公开字段名是否一致？ |
| `API-GATE-006` | Auth、Permission、Tenant、敏感数据、错误披露、Rate/Cost 和文档暴露规则是否明确？ |
| `API-GATE-007` | REST 的 springdoc 兼容性、注解归属、OpenAPI 元素和 Legacy Springfox 边界是否明确？ |
| `API-GATE-008` | 生成 OpenAPI 或 GraphQL Schema/Operation 的校验、契约测试和漂移检查是否准确可执行？ |
| `API-GATE-009` | 第 9 章是否逐字段、逐结果与需求、模型、数据库、前端、测试、兼容和发布保持一致？ |

状态只允许 `PASS` 或有证据的 `N/A`。只有对应协议完全不存在时，协议专项行才可 `N/A`。任何 `FAIL`、`BLOCKED`、`UNKNOWN`、缺失行、空证据或未关闭例外都禁止 `PASS — Ready for user review`，并且必须同步反映到第 20 章相关 `MC-*` 和 `MC-BLOCKER-001`。

## 16. 第 20 章必需映射

API 受影响时，通用 Manual Check 必须包含 API 证据：

- `MC-REUSE-001`：已检查 Spring/springdoc/Spring GraphQL/Egon Result、Validation、Security、Converter 和网关能力；
- `MC-DEP-001`：没有重复 Springfox/springdoc Stack，也没有无依据 GraphQL/工具依赖；
- `MC-VALID-001`：覆盖每个 REST/GraphQL 输入边界、Group、Nested Validation、Normalization 和 Error Path；
- `MC-JSON-001`：Jackson/Wire/OpenAPI/GraphQL Response 一致，且未混用 JSON 库；
- `MC-CONFIG-001`：文档/GraphQL 配置 Key 在各环境一致；
- `MC-SCOPE-001`：只完整重设计受影响 Operation/Protocol；
- `MC-TEST-001`：生成契约/Schema、Validation、Security、Error、Compatibility 和 Consumer Test；
- `MC-BLOCKER-001`：所有 `API-GATE-*` 与重大协议/兼容决策已经关闭。

## 17. 审核失败条件

以下情况按性质返回 `REVISE` 或 `BLOCKED`：

- 只有路由表，没有逐操作 REST/CQRS/GraphQL 和文档设计；
- Query 修改业务状态，或具有任务语义的 Command 仍被命名为普通字段更新；
- 用 CQRS 为不需要的 Bus、Event Store、Read Database、Handler Layer 或 Package Structure 辩护；
- REST 普通资源使用动词、Method 语义错误、所有结果都用通用 `200`、异步行为未说明，或 GET 有 Body；
- OpenAPI 依赖推导 `operationId`、遗漏实质 Error/Security、用 DTO 类名代替真实 Wrapper，或与运行时 Validation/Jackson 冲突；
- 新代码使用 Springfox/Swagger 2 注解，或与 OpenAPI 3 注解混用；
- 无架构理由和漂移测试就把文档拆到 Interface/Customizer；
- 为记录认证等非操作关注点而创建伪 Endpoint；
- GraphQL 只记录 `POST /graphql`，或把 Swagger 注解当成 GraphQL Schema；
- GraphQL Query 写数据、Mutation 缺少 Command/Idempotency/Transaction 语义、Nested Field 制造 N+1，或 Null/Error/Authorization 未定义；
- REST 与 GraphQL 复制业务逻辑，或形成冲突 Validation、Permission、State Transition、Idempotency；
- Docs/UI/Introspection 暴露和安全是偶然行为，或含有 Secret；
- 生成文档/Schema 校验只含糊写成“检查 Swagger”或“测试 GraphQL”。

## 18. 规范与调研来源

资料于 2026-09-03 核查。真实 Spec 仍必须根据当前项目验证协议/框架版本。

- [OpenAPI Specification 3.1](https://spec.openapis.org/oas/v3.1.0)
- [RFC 9110：HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110.html)
- [RFC 9457：Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457.html)
- [springdoc-openapi 官方文档](https://springdoc.org/)
- [springdoc-openapi 官方仓库](https://github.com/springdoc/springdoc-openapi)
- [Swagger Core OpenAPI 3 注解指南](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations)
- [OpenAPI 3 Bearer Authentication](https://swagger.io/docs/specification/v3_0/authentication/bearer-authentication/)
- [Microsoft CQRS Pattern 指南](https://learn.microsoft.com/en-us/azure/architecture/patterns/cqrs)
- [GraphQL Specification](https://spec.graphql.org/October2021/)
- [GraphQL over HTTP 草案](https://graphql.github.io/graphql-over-http/draft/)
- [Spring for GraphQL Annotated Controllers](https://docs.spring.io/spring-graphql/reference/controllers.html)
- [Spring for GraphQL Transports](https://docs.spring.io/spring-graphql/reference/transports.html)
- [Spring for GraphQL Security](https://docs.spring.io/spring-graphql/reference/security.html)
- [Spring for GraphQL Testing](https://docs.spring.io/spring-graphql/reference/testing.html)
- [Spring Framework RFC 9457 Error Responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
- [Mshuyan/swagger 示例仓库](https://github.com/Mshuyan/swagger) —— 仅作为非规范性 Legacy/Example 输入
