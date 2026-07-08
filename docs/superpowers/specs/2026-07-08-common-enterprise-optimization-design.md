# Egon COLA Common 企业级优化硬迁移设计

## 1. 背景

本次需求来自 `/Users/mario/Downloads/egon-cola-common-enterprise-optimization.md`。当前 `egon-cola-component-common` 已经完成模块拆分，形成 `common-core`、`common-model`、`common-trace`、`common-result`、`common-id`、`common-crypto`、`common-mask`、`common-structure`、`common-test` 等子模块。现有方向是正确的，但底层公共契约仍需要进一步定死字段、序列化、错误码、异常安全输出和依赖边界。

用户明确要求本次做大规模迁移，不考虑兼容性，并按照优化文档建议执行。因此本设计不保留 JavaBean setter 兼容层，不保留旧错误码语义，不新增过渡 adapter，也不为旧调用方式保留桥接方法。

## 2. 目标

1. 将 `common-result` 和 `common-model` 的核心 DTO/Model/Query 从普通可变类迁移为 record 契约。
2. 固定 JSON 字段名、字段顺序、null 字段输出、分页字段名和 Java 序列化契约。
3. 强化 `common-core` 的错误码、枚举和异常体系，保持 core 零第三方依赖。
4. 修复 `ResultDtos.failure(Throwable)` 对未知异常 message 的泄漏风险。
5. 按编译结果迁移仓库内受影响调用方，不保留兼容 getter/setter。
6. 补齐 JSON、Java serialization、错误码唯一性和模块边界测试。
7. 使用当前 worktree `/Users/mario/SelfProject/Egon-COLA/.worktrees/common-enterprise-optimization` 完成后续实现，不启动项目运行。

## 3. 非目标

1. 不新增 Spring Boot starter、自动配置、Filter、Interceptor 或运行时 Bean。
2. 不引入 `jackson-databind` 作为 `common-result` 或 `common-model` 的生产依赖。
3. 不保留旧 `BUSINESS_ERROR=500001` 语义。
4. 不提供 record 和普通 class 两套同名契约。
5. 不引入迁移 adapter、兼容 getter、兼容 setter 或 deprecated wrapper。
6. 不重写 `common-id`、`common-crypto`、`common-mask`、`common-structure` 的既有职责，除非编译迁移必须调整调用方。

## 4. 方案选择

采用 record-first 硬迁移。

备选方案一是普通类硬迁移。它可以破坏兼容性并收紧构造，但仍然保留可变对象模型，对字段契约稳定和构造后不可变帮助有限。

备选方案二是进一步重命名包结构，例如把 `result.dto.ResultDto` 改成 `result.Result`。这会同时破坏 import 和行为，迁移噪音更大，也会削弱当前已经建立的 DTO/Model 边界。

最终选择 record-first：在现有包名和类名上原地切换为 record，尽量把破坏集中在对象访问方式、构造方式和错误码契约上。设计模式上保留并强化现有 Factory Method：`ResultDtos` 和 `ResultModels` 继续负责集中创建响应对象，调用方不直接拼装结果字段。

## 5. Core 契约

`egon-cola-component-common-core` 保持无 Jackson、无 Lombok、无 Spring、无其他 common 子模块依赖。

`CodeEnum<C>` 保留泛型形式，新增 `IntCodeEnum` 用于统一 int code 场景。`ErrorStatus` 增加默认方法 `isSuccess()`，以 `CommonStatus.SUCCESS.getCode()` 为成功判断依据。

`CommonStatus` 按优化文档的错误码分段重排：

```text
0                  SUCCESS
400000 - 409999    通用客户端错误
401000 - 401999    认证错误
403000 - 403999    授权错误
404000 - 404999    资源不存在
422000 - 422999    参数/校验错误
429000 - 429999    限流错误
500000 - 509999    系统错误
510000 - 519999    远程调用错误
520000 - 529999    中间件错误
600000 - 699999    业务通用错误
```

`BUSINESS_ERROR` 迁移为 `600000`。新增 `VALIDATION_ERROR`、`TOO_MANY_REQUESTS`、`REMOTE_CALL_ERROR`、`MIDDLEWARE_ERROR`、`CONCURRENCY_ERROR` 等通用状态。

异常体系保留当前命名风格并补齐类型：

```text
EgonException
EgonBusinessException
EgonSystemException
EgonIllegalStateException
EgonValidationException
EgonUnauthorizedException
EgonForbiddenException
EgonNotFoundException
EgonRemoteCallException
EgonConcurrencyException
```

`EgonException` 增加 `retryable` 字段和构造支持。远程调用异常可以显式标记 `retryable`，其他异常默认不可重试。

## 6. Result 契约

`ResultDto<T>` 迁移为 record，字段固定为：

```text
success: boolean
code: int
status: String
message: String
data: T
traceId: String
timestamp: Long
```

`PageResultDto<T>` 迁移为 record，字段固定为：

```text
success: boolean
code: int
status: String
message: String
records: List<T>
total: long
pageNo: int
pageSize: int
pages: long
hasNext: boolean
hasPrevious: boolean
traceId: String
timestamp: Long
```

分页字段名保持 `pages`，不改成 `totalPages`。`records` 在 canonical constructor 中做 null 转 empty，并使用防御性不可变拷贝。为避免 `List.copyOf` 禁止 null 元素导致 common 过度强硬，使用 `Collections.unmodifiableList(new ArrayList<>(records))`。

`ResultModel<T>` 和 `PageResultModel<T>` 也迁移为 record，字段与内部模型语义保持一致。内部 model 不携带 `traceId` 和 `timestamp`，外部 DTO 由 `ResultDtos` 自动填充 trace 与时间。

所有 result record 必须：

```text
implements Serializable
@Serial + @JsonIgnore serialVersionUID = 1L
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder(...)
每个 record component 加 @JsonProperty
```

`ResultDtos.failure(Throwable)` 规则：

1. 如果是 `EgonException`，使用异常中的 `code/status/message`。
2. 如果是未知异常或 null，返回 `CommonStatus.SYSTEM_ERROR`。
3. 未知异常 message 不允许进入对外响应。

## 7. Model 与 Query 契约

`PageMeta` 迁移为 record，保留 `of(total, pageNo, pageSize)` 工厂方法，并负责归一化页码、页大小、总数和页数。

`PageModel<T>` 迁移为 record，字段为：

```text
records: List<T>
meta: PageMeta
```

它继续提供 `of(records, total, pageNo, pageSize)` 工厂方法，并可保留 `total()`、`pageNo()` 等便利访问方法。由于 record accessor 已经是 `records()` 和 `meta()`，不保留旧 `getRecords()`、`getMeta()`。

`PageSlice<T>` 迁移为 record，字段为：

```text
records: List<T>
hasNext: boolean
```

`PageQuery` 迁移为 record，字段为：

```text
pageNo: int
pageSize: int
```

它提供：

```text
DEFAULT_PAGE_NO = 1
DEFAULT_PAGE_SIZE = 10
MAX_PAGE_SIZE = 500
defaultPage()
offset()
```

canonical constructor 归一化页码和页大小，`offset()` 使用 `@JsonIgnore`。

`SortQuery` 和 `TimeRangeQuery` 也迁移为 record。`SortQuery` 的方向仅允许 `ASC` 或 `DESC`，默认空值保持 null，具体 SQL 字段白名单仍由 application/infrastructure 层完成，common 不拼 SQL。`TimeRangeQuery` 保持 `LocalDateTime`，并按优化文档使用 `yyyy-MM-dd HH:mm:ss` 的 Jackson 格式注解。

`BaseRequest` 和 `OperatorContext` 迁移为 record。`BaseRequest` 只携带 `operator`，不放 `traceId` 或 `requestId`。`OperatorContext` 只保留 `operatorId`、`operatorName`、`tenantId`。

## 8. 依赖边界

`common-result` 依赖：

```text
common-core
common-trace
jackson-annotations
lombok provided
logback-classic test
jackson-databind test
```

`common-model` 依赖：

```text
common-core
jackson-annotations
lombok provided
jackson-databind test
```

`common-core` 不引入 Jackson、Lombok、Spring。生产代码只使用 `jackson-annotations` 锁定 JSON 契约，JSON snapshot 测试使用 test scope 的 `jackson-databind`。

## 9. 调用方迁移

迁移采用编译驱动：

1. 先修改 common record 契约、工厂方法和 common 测试。
2. 运行 common 子 reactor。
3. 搜索并迁移仓库内调用方，重点关注 DDC admin/starter 中 `ResultDto` 的访问方式。
4. 所有 `getXxx()` / `isXxx()` 改为 record accessor，例如 `code()`、`success()`、`data()`。
5. 所有 `new + setter` 改为 record 构造、`of(...)`、`defaultPage()` 或 `ResultDtos` / `ResultModels` 工厂方法。
6. 不新增兼容 getter，不新增兼容 setter，不新增 bridge method。

## 10. 测试设计

新增或改造测试覆盖：

1. JSON 契约测试：null 字段仍输出，字段顺序稳定，`serialVersionUID` 不输出，分页 `records=null` 输出 `[]`，分页字段名保持 `pages`。
2. Java serialization 测试：`ResultDto`、`PageResultDto`、`ResultModel`、`PageResultModel`、`PageModel`、`PageQuery` 可以 ObjectOutputStream 往返。
3. 错误码测试：`CommonStatus` code 唯一，status 唯一，`SUCCESS` code 为 0。
4. 异常测试：新异常类型保留 code/status/message/cause/retryable；未知异常不会暴露原始 message。
5. 边界测试：`common-core` 不依赖 Spring、Jakarta、Jackson、Lombok、其他 common 子模块；`common-result` 生产依赖不包含 `jackson-databind`。

目标验证命令：

```bash
./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml test
```

受影响调用方迁移后，再运行最小相关组件测试。若 DDC 组件受影响，优先运行 DDC admin/starter 的 targeted Maven 测试。不会启动项目。

## 11. 任务提交策略

实现阶段按任务拆分提交：

1. 设计 spec 提交。
2. core 契约提交。
3. result record 迁移提交。
4. model/query/request record 迁移提交。
5. 调用方迁移提交。
6. 测试与文档收口提交。

每个提交前运行对应的最小验证命令。若某阶段验证失败，先定位是否为本阶段改动导致，再继续下一阶段。

## 12. 风险

1. record 迁移会破坏所有 JavaBean getter/setter 调用方，需要编译驱动统一修复。
2. `BUSINESS_ERROR` 改码会改变对外 JSON 契约，必须由本次“不考虑兼容性”的范围承担。
3. Jackson record 反序列化依赖当前 Jackson 版本能力，测试必须覆盖。
4. 如果某些框架强依赖无参构造和 setter，本次不会兼容，需要调用方改造或在上层使用专用请求对象。

## 13. 完成标准

1. common 核心 DTO/Model/Query/Request 契约均迁移为 record。
2. JSON 和 Java serialization 契约测试通过。
3. 未知异常 message 不再对外泄漏。
4. `CommonStatus` 新分段和唯一性测试通过。
5. common 子 reactor 测试通过。
6. 仓库内受影响调用方完成 record accessor 迁移，并通过最小相关 Maven 验证。
