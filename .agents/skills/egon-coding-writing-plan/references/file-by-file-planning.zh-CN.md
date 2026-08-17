# 详细逐文件实施规划

> 本文件是 `references/file-by-file-planning.md` 的全中文审核镜像。编写第 4 章、第 5 章或任一实施 Step 前必须读取。只有实施者无需再做架构或业务决策，就能依次执行文件顺序、伪代码、中间状态、命令和提交范围时，Plan 才真正可执行。

## 目录

- [可执行性标准](#可执行性标准)
- [Plan 前 Spec 健康审核](#plan-前-spec-健康审核)
- [Step 边界设计](#step-边界设计)
- [依赖与文件顺序算法](#依赖与文件顺序算法)
- [逐文件必需设计](#逐文件必需设计)
- [伪代码质量标准](#伪代码质量标准)
- [后端完整 Step 示例](#后端完整-step-示例)
- [前端完整 Step 示例](#前端完整-step-示例)
- [数据库 Migration 完整 Step 示例](#数据库-migration-完整-step-示例)
- [验证阶梯](#验证阶梯)
- [提交与交接契约](#提交与交接契约)
- [最终详细度门禁](#最终详细度门禁)

## 可执行性标准

Step 不是字多就详细，而是以下内容没有歧义：

- 准确可观察结果和覆盖需求；
- 修改前仓库状态和已经提交的依赖；
- Test-first 是否适用以及预期 RED 原因；
- 精确有序文件、操作、符号及其顺序原因；
- 每个修改符号和风格选择的当前仓库证据；
- 签名、字段、映射、分支、错误、事务、副作用和消费者；
- 每个文件完成后的中间状态，包括是否故意不编译或测试为 RED；
- 准确验证工作目录、命令、预期退出/结果和失败回退点；
- 准确 Commit Paths、信息、回滚/Forward Fix 和 Step 结束状态。

拒绝“实现 Service”“更新 API”“增加前端”“编写测试”或复制 Spec 原文。Plan 必须说明仓库如何变化，而不是重复功能做什么。

## Plan 前 Spec 健康审核

把 Spec 元素转换成文件前，先确认有效 Spec 的设计必要且内部可实施。

| Spec 元素 | Spec 必要性结论/证据 | 当前仓库证据 | 直接/复用方案 | Plan 决策 |
| --- | --- | --- | --- | --- |
| `<API/类/表/缓存/Job/页面/依赖>` | `<Spec 章节>` | `<路径/符号/消费者>` | `<复用/派生/合并>` | Implement / Already exists / Return to Spec |

存在以下情况时返回 `REVISE` 或 `BLOCKED` 给 Spec/用户：

- 新接口只获取被原样复制到另一请求的参数；
- 目标后端可从现有上下文派生身份、租户、应用、来源或所有权；
- 新增类/分层/表/缓存/模式没有当前变化点或需求；
- Spec 的“直接方案”缺失、只是陪跑，或实际能用更少移动部件满足同一需求；
- 拟议消费者、路径、符号、查询、Migration、页面或验证命令不存在，且 Spec 未批准创建；
- 实施需要自行发明契约、状态、错误、字段映射、事务或兼容规则。

不能在 Plan 中修复架构。记录证据和需要修订的准确 Spec 章节。详细实施说明不能把无依据设计伪装成可实施方案。

## Step 边界设计

一个 Step 交付一个可独立验证的语义结果，通常对应一个提交。多个文件组成不可分割的 RED/GREEN 切片时，可以放在同一 Step。

合理 Step 边界：

- 一个契约行为从聚焦 RED 测试到实现与接线；
- 一个 Schema Migration 及验证它所需的最小 Mapping/Repository 兼容；
- 一个前端用户动作从组件测试到 Client/状态/渲染；
- 每条路径完全机械相同且一个验证可证明的仓库级迁移。

不合理 Step 边界：

- 包含多个无关行为的“Backend”“Frontend”或“Tests”；
- 每文件一个 Step，导致中间提交不能编译或证明结果；
- 整个功能一个 Step，但需求和测试可分离；
- 为后续未批准工作提前创建架构；
- 文件与另一 Step 重叠，却不说明后续准确原因和所有权。

每个 Step 必须说明：

| 关注点 | 必需内容 |
| --- | --- |
| 基线状态 | 修改前存在且通过什么；依赖哪些前置 Step Commit |
| 可观察结果 | 一个外部或技术可验证结果 |
| 结束状态 | 现在可用的契约/文件/测试，以及刻意未做内容 |
| Test-first 门禁 | `Required` 和 RED 原因，或 `Not applicable` 与仓库/技术证据 |
| 提交范围 | Step 所有的准确路径，不包含无关文件 |

## 依赖与文件顺序算法

必须推导顺序，不能套固定分层列表。

1. **识别证明点**——选择证明缺失行为的聚焦单元/契约/组件/Migration 测试。
2. **识别编译前置**——测试编译所需 Type、生成契约、Fixture、Schema 或共享常量。必须位于 RED 前时解释原因。
3. **放置 RED 文件**——在行为实现前新增/修改聚焦测试，并说明预期失败。
4. **增加最小实现路径**——按真实编译/运行依赖排列 Type、Mapping、持久化、业务逻辑、入口和消费者。
5. **增加接线与注册**——实现存在后才加入 Config、Registry、Route、DI 或 Export。
6. **增加兼容/运维文件**——在依赖有效的准确位置加入 Migration、Config、权限清单、指标、文档或发布脚本。
7. **用验证闭环**——先聚焦 GREEN，再执行 Step 所需最小模块/跨模块门禁。

常见顺序只是指导，不是规则：

```text
Java 行为：
聚焦测试 -> 契约/数据 Type -> DAO/Mapper -> service.impl -> Controller/接线 -> 聚焦+模块测试

前端行为：
组件/Hook 测试 -> API/Type 契约 -> Client/Query Hook -> Component/Page -> Route/接线 -> 聚焦+Typecheck

数据库变更：
Migration 契约测试 -> 新 Migration -> PO/Mapper -> DAO Query -> Service 兼容 -> Migration+集成测试

生成 RPC：
Proto/IDL 契约测试 -> IDL -> 生成命令/产物 -> Provider -> Client Adapter -> Consumer Test
```

编译要求契约先于测试时，说明 File 1 只建立编译前置，File 2 才是 RED 行为门禁。不能假装契约文件本身已经证明 RED。

## 逐文件必需设计

每个 `#### File N` 必须包含：

| 字段 | 必需详细度 |
| --- | --- |
| Purpose | 当前 Step 中的一个职责，不是整个功能 |
| Symbols | 准确 Class/Method/Function/Type/Field/Config Key/Test Case |
| Repository evidence | 证明放置和风格的现有路径/符号/模式 |
| Dependencies and consumers | Caller、Callee、Import/Module、运行/编译消费者 |
| Why now | 该文件处于准确顺序位置的依赖理由 |
| Contract/signature changes | 准确 Before/After 或新签名/字段/注解/Schema |
| Input/output and state mapping | 逐字段来源/目标、默认、空值、状态/持久化影响 |
| Error and edge behavior | 校验、权限、不存在、重复、并发、依赖、回滚分支 |
| Implementation pseudocode | 使用真实符号的仓库语言算法 |
| Verification contribution | 哪个测试/门禁观察该文件行为 |
| After this file | Compile/RED/GREEN/已接线状态和剩余缺口 |

`DELETE` 必须说明消费者已删除、替代物、搜索证据、生成引用、兼容和如何验证不存在。`RENAME` 写明两个路径和全部引用。`GENERATED` 写明 Source of Truth 和准确生成命令，不能手改生成产物。

## 伪代码质量标准

伪代码应让实施者只选择语法细节，而不再选择行为或架构。

按需包含：

- 准确注解、可见性、Class/Interface、Method/Function 签名、参数和返回类型；
- 校验顺序和准确错误/异常/结果映射；
- 权威上下文派生，而不是调用方提供安全事实；
- 协作者调用顺序和事务边界；
- Query 条件、Join、排序、分页、影响行、锁和索引；
- 字段映射、空/默认/枚举/时间/小数转换和敏感数据；
- 幂等、重复、并发、重试、超时、回滚、缓存失效和事件；
- 前端状态转换、Hook、Query Key、Loading/Empty/Error/Disabled 和用户动作；
- 测试 Fixture、动作、断言、Mock/Fake、持久化/发布结果和否定断言。

伪代码不能：

- 发明 Spec 和仓库都不存在的符号；
- 只写 `validate/process/save/handle error` 而不写条件和结果；
- 写成应在实施阶段完成的生产级完整方法体；
- 用“按需”“适当”“等等”隐藏未决选择；
- 未经 Spec 必要性结论就引入 Helper、Mapper、Factory、Strategy、Endpoint 或 Cache。

## 后端完整 Step 示例

以下只说明结构；所有符号、路径和命令必须替换为仓库证据。

### Step — 原子拒绝重复订单创建

- Requirements: `REQ-007`、`REQ-008`
- Dependencies: Migration Step 已提交
- Baseline state: `OrderServiceImpl#create` 写订单/明细，但没有租户范围幂等查询；普通创建测试通过。
- Observable outcome: 同租户/Key/载荷返回第一次结果；不同载荷冲突；不创建重复订单。
- End state: Service 与 DAO 实现已批准幂等契约；Controller 契约不变；并发集成由本 Step 后续文件覆盖。
- Test-first gate: `Required`——当前聚焦重复测试会创建两笔订单或找不到已保存结果。
- Ordered files:

#### File 1 — `MODIFY src/test/java/.../OrderServiceImplTest.java`

- Purpose: 实现前定义顺序重放和 Key/Hash 冲突。
- Symbols: `returnsStoredResultForSameKey`、`rejectsSameKeyForDifferentPayload`
- Repository evidence: 当前测试类中的 Create Fixture 和 Repository Fake。
- Dependencies and consumers: 调用公共 `OrderService#create`；观察 Fake DAO 写入和 `OrderResult`。
- Why now: 不改变生产行为，先建立 RED 契约。
- Contract/signature changes: 复用现有创建签名；只有已在 Spec 中规定时才给 Command 增加幂等 Key。
- Input/output and state mapping: 租户来自测试安全上下文；Canonical Request -> Hash；已保存结果 -> 返回结果。
- Error and edge behavior: 同 Hash 返回第一次结果；不同 Hash 抛 `IdempotencyConflictException`；都断言只写一笔订单。
- Implementation pseudocode:

```java
@Test same_key_same_payload_returns_first_result() {
    arrange tenantContext(TENANT_A), command(KEY_1, PAYLOAD_A)
    first = service.create(command)
    replay = service.create(command)
    assertThat(replay).isEqualTo(first)
    verify(orderDao, times(1)).insert(any())
}

@Test same_key_different_payload_conflicts_without_second_write() {
    service.create(command(KEY_1, PAYLOAD_A))
    assertThatThrownBy(() -> service.create(command(KEY_1, PAYLOAD_B)))
        .isInstanceOf(IdempotencyConflictException.class)
    verify(orderDao, times(1)).insert(any())
}
```

- Verification contribution: RED 命令只运行这两个测试，且必须因缺失重放/冲突行为失败。
- After this file: 测试基于已批准契约编译，并因目标行为缺失失败，不是 Fixture 错误。

#### File 2 — `MODIFY src/main/java/.../service/impl/OrderServiceImpl.java`

- Purpose: 承担 Canonical Hash 比较和事务编排。
- Symbols: `create(CreateOrderCommand)`；只有 Spec 已批准时才组合 `OrderIdempotencyService/Dao`。
- Repository evidence: 现有 `@Transactional` 创建方法和异常映射惯例。
- Dependencies and consumers: Controller 依赖 `OrderService`；实现调用幂等/订单/明细 DAO。
- Why now: File 1 固定行为，这是最小 GREEN 编排。
- Contract/signature changes: Controller Route 不变；使用已批准 Command/Key 字段。
- Input/output and state mapping: 从可信上下文派生 Tenant；Canonical 业务字段 -> Request Hash；持久化第一次结果 -> Response。
- Error and edge behavior: 同 Hash 重放，不同 Hash 冲突；唯一竞争后重读 Winner；事务失败不写部分订单/结果。
- Implementation pseudocode:

```java
@Transactional
OrderResult create(CreateOrderCommand command) {
    tenantId = currentTenant.requireId()
    hash = requestCanonicalizer.sha256(command.businessFields())
    existing = idempotencyDao.find(tenantId, command.key())
    if (existing != null) {
        if (!existing.requestHash().equals(hash)) throw new IdempotencyConflictException()
        return existing.toOrderResult()
    }
    validated = orderValidator.validateAndPrice(tenantId, command)
    order = orderDao.insert(validated.order())
    itemDao.batchInsert(order.id(), validated.items())
    idempotencyDao.insert(tenantId, command.key(), hash, order.result())
    return order.result()
}
```

- Verification contribution: 顺序单测 GREEN；事务集成观察竞争/回滚。
- After this file: 聚焦单元行为 GREEN；数据库唯一竞争仍由 File 3 集成覆盖。

- Validation working directory: 仓库模块根目录
- Verification command: `mvn -pl order-module -Dtest=OrderServiceImplTest,OrderIdempotencyIT test`
- Expected result: 指定测试通过；顺序/并发重复都只存在一笔订单/结果。
- Failure returns to: 断言/Fixture 不一致回 File 1，编排失败回 File 2，约束/竞争失败回 Migration Step。
- Completion criteria: 两项需求都有单元/集成证据，且未增加公共 Route 或无关类。
- Rollback: 只回退本 Step 三个路径；Additive Migration 按已批准 Forward Fix 边界处理。
- Commit paths: 本 Step 声明的准确测试、实现和集成测试路径。
- Commit: `feat(order): make creation idempotent`

## 前端完整 Step 示例

使用相同逐文件字段。典型顺序：

1. 命名 Loading、Success、Validation、Forbidden、Retry 和 Cache 断言的 Component/Hook Test；
2. 已批准且无法复用时新增准确 API Request/Response Type；
3. Client/Query Hook，包含 Route/Query Key、请求映射、错误映射和 Invalidation；
4. Component/Page 渲染分支和事件；
5. 只有尚未注册时才增加 Route/Menu/Export 接线。

伪代码示例：

```typescript
const mutation = useMutation({
  mutationFn: (form: OrderFormState) => orderClient.create(mapFormToRequest(form)),
  onSuccess: ({data}) => {
    queryClient.invalidateQueries({queryKey: orderKeys.list()});
    navigate(`/orders/${data.orderId}`);
  },
  onError: (error) => setFieldErrors(mapValidationErrors(error)),
});

if (query.isPending) return <OrderSkeleton />;
if (query.isError) return <RetryPanel onRetry={query.refetch} />;
if (query.data.items.length === 0) return <EmptyOrders />;
return <OrderTable rows={query.data.items} />;
```

说明租户/身份来自哪里。目标 API 可派生或页面已经持有选中资源时，不能规划参数查询。写明准确测试命令、Typecheck/Build 门禁、预期结果和只能人工验证的边界。

## 数据库 Migration 完整 Step 示例

文件顺序通常包括：

1. 断言缺失字段/约束/索引或当前失败的 Migration/Schema 契约测试；
2. 正好一个新的下一版本 Flyway 文件；
3. 能兼容已批准窗口的 PO/Entity/Mapper 修改；
4. DAO/Repository Query、影响行和锁行为；
5. Migration、历史行、约束和 Query 集成测试。

SQL 伪代码必须写执行顺序和 Guard：

```sql
-- 按已批准规则分析/保护重复 Key
ALTER TABLE orders ADD COLUMN idempotency_key varchar(64);
-- 只有 Spec 定义确定来源时才回填，否则保持可空兼容
CREATE UNIQUE INDEX ... ON orders(tenant_id, idempotency_key) WHERE idempotency_key IS NOT NULL;
-- 旧 Writer 退出且已批准阶段到达后才强制 NOT NULL
```

说明方言、准确 Migration 路径/版本、锁/构建风险、新旧应用矩阵、批次/重启所有者、验证 SQL 与预期数量、应用回滚限制和 Forward Fix。绝不能规划修改旧 Migration。

## 验证阶梯

每条命令行必须包含：

| 顺序 | 工作目录 | 准确命令/方法 | 范围 | 预期结果 | 失败回到 | 运行时边界 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 RED | `<cwd>` | `<command>` | `<test>` | 因准确缺失行为失败 | File N | Static/Module |
| 2 GREEN | `<cwd>` | `<command>` | `<test/module>` | Exit 0；指定测试通过 | 所属文件 | Static/Module |
| 3 Regression | `<cwd>` | `<command>` | `<module/cross-module>` | Exit 0；无仓库策略警告 | 所属 Step | Static/Module |
| 4 Manual/runtime | `<cwd/system>` | `<steps>` | `<live behavior>` | `<可观察结果>` | `<Step/后续>` | 用户控制 |

不能只写 `mvn test`、`npm test` 或“运行构建”，必须写仓库根/模块、Selector、所需环境变量、预期测试/结果、Timeout/运行时依赖，以及 Skip/Warning 解释。

Plan 中的验证命令是未来指令，不是验证已通过的证据。

## 提交与交接契约

每个 Step：

- 列出准确 `Commit paths`，必须等于 Step 声明写范围；
- 使用与可观察结果一致的一个语义消息；
- 写明基线和结束状态，让下一 Step 知道哪些契约/测试已经存在；
- 无关 Dirty/Staged/Untracked 路径不能进入暂存或提交；
- 根据数据/兼容风险，把回滚定义为路径限定源码回退、应用回滚边界或 Forward Fix；
- 不能用一个提交包含多个独立结果，也不能为空实现创建提交。

同一文件必须在多个 Step 修改时，说明每个 Step 所有的准确符号/章节、为什么一个原子 Step 更差，并保证实施过程不会提交已知不完整的公共契约。

## 最终详细度门禁

存在以下任一情况返回 `REVISE`：

- Step 或文件描述可以原样粘贴到无关仓库；
- 实施者仍需选择签名、字段来源、分支、错误、事务、Query、页面状态、测试断言或文件顺序；
- 伪代码比其声称定义的行为更短，或只用通用动词而没有真实符号；
- 缺少当前仓库证据、依赖/消费者、映射、错误、验证贡献或 After-file 状态；
- 验证缺少工作目录、准确命令/方法、预期结果和失败回退点；
- Commit Paths 不准确或包含 Step 外文件；
- Step 实施的 Spec 元素未通过简洁性/必要性审核；
- Plan 只在阶段级详细，而不是文件和符号级。
