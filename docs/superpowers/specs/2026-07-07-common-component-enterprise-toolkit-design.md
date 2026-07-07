# egon-cola-component-common 企业级工具集设计

## 1. 背景

`egon-cola-component-common` 当前只包含少量 COLA 旧风格基础类，主要集中在 `exception`、`model`、`page`、`result`、`validation`。这些类存在以下问题：

1. 注释仍保留旧 COLA、HSF、历史作者和日期信息，不符合当前项目规范。
2. `validation.Assert` 与项目已有的 `spring-validation-starter` 边界重叠，不应继续放在 common 中。
3. `Response`、`SingleResponse`、`MultiResponse`、`PageResponse`、`Command`、`Query`、`DTO`、`ClientObject` 等命名和语义偏旧，不符合当前企业级 common 工具集定位。
4. 工具能力不足，无法支撑企业项目常见的字符串、集合、日期、JSON、ID、枚举、脱敏、加密、树结构等基础能力。

本次设计目标是将 `egon-cola-component-common` 重塑为企业级纯 Java common 工具集，同时保持底层依赖边界清晰，不引入 Spring 自动装配和业务语义。

## 2. 目标

1. 将 common 定位为跨组件、跨业务模块可复用的企业级基础工具集。
2. 直接替换旧 COLA 风格 API，不保留不符合当前规范的旧类。
3. 通过成熟开源库补齐企业常用工具能力，避免低价值自研。
4. 对 JSON、加密、脱敏、树结构等规范敏感能力提供 common 门面收口。
5. `Result`、`PageResult` 等结果模型自动携带 MDC 中的 `traceId`。
6. 移除 common 内的 validation 能力，将参数校验留给 `spring-validation-starter` 和上层异常映射。
7. 保持 common 无 Spring、无 Jakarta Validation、无业务模块依赖。
8. 对 archetype 只做必要的最小对齐，不展开全面架构重排。

## 3. 非目标

1. 不实现 Spring Boot starter、自动配置、Filter、Interceptor 或 Bean 注入。
2. 不替代 `spring-validation-starter`，不提供 Bean Validation 注解或断言式校验框架。
3. 不引入业务领域模型、业务错误码或业务流程语义。
4. 不全面改造 light、web、service archetype 的业务示例。
5. 不追求替代 Hutool、Apache Commons 或 Jackson 的全集能力。
6. 不提供复杂密钥管理、链路追踪系统接入或分布式 ID 基础设施。

## 4. 模块定位与依赖边界

`egon-cola-component-common` 是企业级纯 Java common 工具集，可被 domain、application、adapter 和其他 components 复用。它位于底层基础能力位置，不反向依赖任何业务层、运行时组件或具体框架。

允许引入的依赖：

1. `org.slf4j:slf4j-api`：用于 MDC 读取和写入 `traceId`。
2. `org.apache.commons:commons-lang3`：字符串、对象等基础工具。
3. `org.apache.commons:commons-collections4`：集合工具。
4. `commons-codec:commons-codec`：摘要、Base64、Hex 等编码能力。
5. Jackson：JSON 序列化和反序列化门面。
6. JDK 标准库。

禁止引入的依赖：

1. Spring Framework 和 Spring Boot。
2. Jakarta Validation、Servlet、Persistence 等 Jakarta 技术栈。
3. MyBatis、Redisson、DTP 组件。
4. archetype 内部包或任何业务模块。
5. 其他会让 common 绑定运行时环境的框架。

依赖暴露策略采用混合方式：稳定基础库可以作为 common 的传递依赖暴露；JSON、crypto、masking、tree 等规范敏感能力通过 common 门面收口，避免业务代码散落调用底层实现。

## 5. 包结构

目标包结构如下：

```text
top.egon.cola.component.common
├── exception
├── model
├── query
├── result
├── trace
└── util
```

调整规则：

1. 删除 `validation` 包，不再提供 `Assert`。
2. 删除旧 `page` 包，将分页请求放到 `query`，分页结果放到 `result`。
3. 保留 `result` 包名，但替换为新的 `Result`、`PageResult` 契约。
4. 保留 `model` 包名，但替换旧 `Command`、`Query`、`DTO`、`ClientObject`。
5. 保留 `exception` 包名，但替换旧 `BaseException`、`BizException`、`SysException`。
6. 新增 `trace` 和 `util` 包。

## 6. 核心契约设计

### 6.1 Result

新增 `top.egon.cola.component.common.result.Result<T>`。

字段：

```text
success: boolean
code: String
message: String
data: T
traceId: String
```

设计要求：

1. 成功结果使用 `success/code/message/data/traceId` 结构。
2. 失败结果同样携带 `traceId`，便于问题定位。
3. 静态工厂方法统一从 `TraceContext` 读取 MDC 中的 `traceId`。
4. 成功码和默认失败码来自 common 默认错误码常量。
5. 不再提供 `Response`、`SingleResponse`、`MultiResponse`。

建议工厂方法：

```text
Result.success()
Result.success(data)
Result.failure(code, message)
Result.failure(Throwable)
```

### 6.2 PageResult

新增 `top.egon.cola.component.common.result.PageResult<T>`。

字段：

```text
success: boolean
code: String
message: String
records: List<T>
total: long
pageNo: int
pageSize: int
pages: long
hasNext: boolean
hasPrevious: boolean
traceId: String
```

设计要求：

1. 用于分页查询返回，不再使用旧 `PageResponse`。
2. `records` 默认返回空列表，避免调用方处理 null。
3. `pages` 根据 `total/pageSize` 计算。
4. `hasNext`、`hasPrevious` 根据页码和总页数计算。
5. 工厂方法自动填充 `traceId`。

### 6.3 PageQuery

新增 `top.egon.cola.component.common.query.PageQuery`。

字段：

```text
pageNo: int
pageSize: int
orderBy: String
orderDirection: String
```

设计要求：

1. 默认 `pageNo = 1`。
2. 默认 `pageSize = 10`。
3. 小于最小值时进行归一化。
4. 提供 `offset()` 或 `getOffset()`。
5. 排序方向只接受 `ASC` 和 `DESC`，默认 `DESC`。
6. 不使用 Bean Validation 注解。

### 6.4 Exception

替换旧异常体系，新增：

1. `BusinessException`：业务可预期异常。
2. `SystemException`：系统不可预期异常。
3. `ErrorCodes`：common 默认错误码常量。

异常字段：

```text
code: String
message: String
cause: Throwable
```

设计要求：

1. 异常只持有错误码和错误消息，不强绑定 `ErrorCode` 接口或枚举。
2. 支持通过 `code/message` 和 `code/message/cause` 构造。
3. 默认业务错误码为 `BUSINESS_ERROR`。
4. 默认系统错误码为 `SYSTEM_ERROR`。
5. 不内置大量细分异常类型，避免 common 过早绑定业务场景。

### 6.5 Model

替换旧 `Command`、`Query`、`DTO`、`ClientObject`。

新增基础模型：

1. `BaseModel`：基础序列化模型，提供扩展字段能力。
2. `BaseRequest`：请求对象基础类。
3. `BaseQuery`：查询对象基础类。
4. `BaseEntity<ID>`：可选实体基础类，提供 `id`。
5. `AuditableModel`：可选审计模型，提供审计字段。

`AuditableModel` 字段建议：

```text
createdAt: LocalDateTime
createdBy: String
updatedAt: LocalDateTime
updatedBy: String
deleted: Boolean
```

设计要求：

1. 基础模型只提供企业通用字段，不绑定业务语义。
2. 审计模型是可选父类，不强迫所有业务模型继承。
3. 不再使用 COLA 旧命名 `Command`、`Query`、`DTO`。
4. 扩展字段能力保留，但命名和注释按当前项目规范重写。

## 7. Trace 设计

新增 `top.egon.cola.component.common.trace.TraceContext`。

设计要求：

1. MDC key 统一为 `traceId`。
2. `TraceContext.getTraceId()` 只读取 `MDC.get("traceId")`。
3. 不兼容 `trace_id`、`requestId`、`X-Trace-Id` 等别名。
4. 提供 `setTraceId(String)` 和 `clearTraceId()`。
5. `Result` 和 `PageResult` 的工厂方法自动读取 `TraceContext.getTraceId()`。
6. common 只负责读取和写入 MDC，不负责生成 HTTP 链路 trace，也不负责自动传播。

## 8. 工具能力设计

### 8.1 Strings

新增 `top.egon.cola.component.common.util.Strings`。

能力范围：

1. blank 判断。
2. default 值处理。
3. trim 和 normalize。
4. truncate。
5. equals/equalsIgnoreCase。

实现策略：基于 `commons-lang3` 做薄门面。

### 8.2 Collections2

新增 `top.egon.cola.component.common.util.Collections2`。

能力范围：

1. null-safe empty 判断。
2. null-safe size。
3. first/last。
4. empty list/set/map。
5. 简单 map/filter 门面。

实现策略：基于 `commons-collections4` 和 JDK Stream 做薄门面。

### 8.3 Dates

新增 `top.egon.cola.component.common.util.Dates`。

能力范围：

1. 当前时间。
2. format。
3. parse。
4. startOfDay/endOfDay。
5. epoch millis 与 `LocalDateTime` 转换。

实现策略：基于 `java.time`，不引入额外日期库。

### 8.4 Jsons

新增 `top.egon.cola.component.common.util.Jsons`。

能力范围：

1. `toJson(Object)`。
2. `fromJson(String, Class<T>)`。
3. `fromJsonList(String, Class<T>)`。
4. `toMap(String)`。
5. `convert(Object, Class<T>)`。

实现策略：基于 Jackson，异常统一包装为 `SystemException`。

### 8.5 Ids

新增 `top.egon.cola.component.common.util.Ids`。

能力范围：

1. 标准 UUID。
2. 无横线 UUID。
3. 简短随机 ID。

实现策略：首批基于 JDK UUID，不引入额外分布式 ID 依赖。

### 8.6 Enums

新增 `top.egon.cola.component.common.util.Enums`。

能力范围：

1. 按 enum name 查找。
2. 按 code 查找。
3. 判断枚举值是否存在。

可选约定：提供轻量 `CodeEnum` 接口，业务枚举可选择实现，但 common 不强迫所有枚举继承。

### 8.7 Masking

新增 `top.egon.cola.component.common.util.Masking`。

能力范围：

1. 手机号脱敏。
2. 邮箱脱敏。
3. 身份证脱敏。
4. 银行卡脱敏。
5. 姓名脱敏。
6. 通用区间脱敏。

实现策略：基于字符串处理和 `commons-lang3`，不绑定具体业务字段。

### 8.8 Crypto

新增 `top.egon.cola.component.common.util.Crypto`。

能力范围：

1. MD5。
2. SHA-256。
3. HMAC-SHA256。
4. Base64 encode/decode。
5. Hex encode/decode。

实现策略：基于 JDK `MessageDigest`、`Mac` 和 `commons-codec`，不提供密钥管理。

### 8.9 Trees

新增 `top.egon.cola.component.common.util.Trees`。

能力范围：

1. 扁平列表转树。
2. 指定 id、parentId、children 访问器。
3. 支持根节点 parentId 判定。

实现策略：使用轻量函数式接口，不绑定业务模型和持久化模型。

## 9. 注释规范

1. 原有英文 COLA 搬运注释全部改为当前项目风格的简洁中文说明。
2. 类注释说明职责、适用边界和不做什么。
3. 方法注释只加在公共复杂方法上，不给显而易见的 getter/setter 添加噪音。
4. 不保留原作者、历史日期、HSF、COLA 旧描述等与当前项目无关的信息。
5. 工具类注释强调薄门面和规范收口，避免误导为完整框架。

## 10. Archetype 最小对齐策略

本次主目标是 `egon-cola-component-common`。因为旧 API 会被直接删除，若仓库内 archetype 或测试引用旧 API 导致编译失败，需要做最小对齐。

对齐规则：

1. 只处理编译失败或明显引用旧 common API 的位置。
2. 不全面改造 light、web、service archetype 的业务示例。
3. 不重排 archetype 分层结构。
4. 不将 common 强行引入 facade 层，避免违反现有 facade 自包含约定。
5. 如果 archetype 内部已有自己的 `common.exceptions.BizException` 或 `facade.dto.PageResponse`，除非编译冲突，否则不强行替换。

## 11. 测试与验证设计

实现阶段需要更新 common 模块测试，优先覆盖以下内容：

1. `CommonComponentBoundaryTest`：验证 common 不依赖 Spring、Jakarta、业务组件、DTP 组件。
2. `Result` traceId 测试：MDC 中存在 `traceId` 时，成功和失败结果自动携带。
3. `PageResult` 测试：总页数、上一页、下一页、空列表默认值。
4. `PageQuery` 测试：页码归一化、页大小归一化、offset、排序方向。
5. `BusinessException` 和 `SystemException` 测试：错误码、错误消息和 cause。
6. `Jsons` 测试：序列化、反序列化、列表反序列化和异常包装。
7. `Masking` 测试：手机号、邮箱、身份证、银行卡、姓名。
8. `Crypto` 测试：摘要、HMAC、Base64、Hex。
9. `Trees` 测试：扁平列表转树。

建议验证命令：

```bash
mvn -pl egon-cola-components/egon-cola-component-common -am test
```

如果实现阶段修改 archetype，再追加对应 archetype 的最小验证命令。

## 12. 设计模式取舍

本设计使用 Facade 思路，但不引入复杂模式体系。

采用 Facade 的位置：

1. `Jsons` 收口 Jackson。
2. `Crypto` 收口 JDK 和 Commons Codec。
3. `Masking` 收口脱敏规则。
4. `Trees` 收口树构造算法。

不采用的模式：

1. Strategy：当前工具能力没有复杂可插拔算法需求。
2. Factory：结果对象和分页对象使用静态工厂方法足够。
3. Template Method：工具类不需要继承式扩展。
4. Chain of Responsibility：错误处理和工具调用不需要责任链。
5. Adapter：当前没有多套外部实现需要统一适配。

选择原因：common 工具集应直接、稳定、低抽象。过早引入策略、工厂、模板等模式会增加使用成本，不符合当前目标。

## 13. 风险与约束

1. 本次是破坏性 API 调整，外部消费者若依赖旧类需要迁移。
2. `Result` 自动携带 `traceId` 依赖调用方提前写入 MDC，common 不负责 trace 生成和传播。
3. 引入 Jackson 后 common 不再是零三方依赖，但仍保持无 Spring 依赖。
4. 工具类只覆盖企业高频能力，不追求完整框架能力。
5. `validation` 删除后，参数校验必须由 `spring-validation-starter` 或上层应用处理。
6. 如果 archetype 对旧 API 有隐性依赖，实现阶段需要以最小改动保持仓库验证可通过。

## 14. 交付范围

实现阶段交付范围：

1. 调整 `egon-cola-component-common/pom.xml` 依赖。
2. 删除旧 COLA 风格类和 validation 包。
3. 新增核心契约类：`Result`、`PageResult`、`PageQuery`、异常、模型、TraceContext。
4. 新增企业常用工具门面：`Strings`、`Collections2`、`Dates`、`Jsons`、`Ids`、`Enums`、`Masking`、`Crypto`、`Trees`。
5. 更新 common 模块测试。
6. 对 archetype 做必要的最小编译对齐。
7. 运行 common 模块验证；如修改 archetype，再运行对应最小验证。
