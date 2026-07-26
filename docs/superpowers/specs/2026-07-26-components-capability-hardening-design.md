# Egon-COLA Components 能力兑现与整改设计

状态：待确认（波次 0 与波次 1 的已复核项已在分支 `worktree-components-capability-hardening` 上实施，见第 18 章）

编写日期：2026-07-26

审计基线：`main@bff002cd4e1d`

实施基线：`main@a58d7645`（`Merge branch 'codex/gateway-ddc-rpc-integration'`）

> 基线变更说明：审计执行期间 `codex/gateway-ddc-rpc-integration` 被合入 main。本文第 4 章原
> 将该分支范围记为"已确认待实施"并排除，该判断在合入后已过期——相关联调闭环工作现已落地。
> 第 17 章的实施均基于合入后的 `a58d7645`，components 类结论不受影响（合入内容集中在
> gateway/DDC/RPC 联调，未触及本文所列的 M1–M4 缺陷点）。
> 姊妹规格 `2026-07-26-architecture-audit-rectification-spec.md` 负责合入后的 gateway 类审计。

审计方式：12 个只读审计 Agent 并行执行（10 个组件各 1 个，跨组件结构 1 个，文档覆盖 1 个），
共 1194 次工具调用。对结论执行对抗式复核（要求复核方尽力证伪）：22 条完成复核，其中
21 条 confirmed、1 条 partial、**0 条 refuted**；其余复核因会话额度中断未执行，本文中以
`[未复核]` 标注。

关联设计（**本设计明确不覆盖其范围**）：

- `2026-07-26-gateway-ddc-rpc-integration-remediation-design.md`（已确认，待实施）
- `2026-07-25-gateway-17-gap-remediation-design.md`（已确认，实施中）
- `2026-07-24-ddc-standalone-rpc-framework-design.md`（待用户确认）
- `2026-07-08-access-guard-component.md`、`2026-07-16-bytecode-agent-executor.md`（已存在计划，与本设计部分任务重叠，见第 11 章）

---

## 1. 背景与结论

### 1.1 结论先行：原命题需要修正

立项时的判断是"这些组件功能很多，但比较空"。审计结论是：

> **代码不空。空的是"承诺面"与"实现面"之间的差额。**

支撑证据：

```text
1. 10 个组件共 1489 个 Java 文件、约 131,600 行；全仓几乎不存在 TODO / FIXME /
   UnsupportedOperationException 型未完成残桩。
2. 成熟度评级：bytecode、transactional-outbox 为 mature；其余 8 个为 functional；
   无一为 skeleton。
3. 测试是真的：transactional-outbox 本地实跑 87/87 + 10/10 通过；gateway 180 个
   @Test；dcc 159 个；dtp 115 个；bytecode 110 个（含 ASM 字节码帧验证）；
   rpc 有真实 loopback TCP 与多 JVM 进程测试。
```

对 gateway 与 rpc 两个组件，审计方主动记录了对原命题的反驳：二者"模块内实现有真实深度，
空脚手架假设不成立"。

### 1.2 但"空"的感受是真实的，它有确切来源

用户感到"空"，不是因为没有代码，而是因为**照着 README 去用，很多东西不生效、不存在或起不来**：

```text
1. 按 README 配 dcc / rpc 的 quickstart，应用起不来（缺 tls.development-plaintext）。
2. 引入 access-guard / dtp starter，应用连不上 Redis 就起不来（Redisson 被强制装配）。
3. 调 access-guard 的 14 个配置项，没有任何效果（属性被绑定但无人读取）。
4. 用 access-guard 的 FailStrategy.LOCAL_FALLBACK，行为比 FAIL_OPEN / FAIL_CLOSED 更差
   （直接抛原始异常）。
5. 用 rule-engine 的 RouteDecision.end(data)，data 永远拿不到。
6. 配 method-extension 的 engine=AGENT 而未引入 agent，静默不拦截任何方法。
```

核心矛盾：**README + `@ConfigurationProperties` + 枚举常量 + SPI 接口共同构成的对外承诺面，
大于真实实现面；且仓库没有任何机制阻止这个差额继续扩大。**

因此整改必须做两件事，缺一不可：

```text
1. 关闭已定位的差额（第 6 - 11 章）。
2. 安装防漂移闸门，使差额不能再无声增长（第 12 章）。
```

第 2 件事是本设计最重要的一条。只做第 1 件，半年后会回到同一位置。

---

## 2. 七类缺陷模式

这是本次审计的主结论。所有单点问题都归入以下 7 类，整改按类推进而非按组件推进。

| 编号 | 模式 | 定义 | 危害 |
|---|---|---|---|
| M1 | 配置面空转 | 属性已声明、已写进 README、Spring 已绑定，但生产代码零读取 | 用户改配置无反应，且无法自查 |
| M2 | 声明能力缺实现 | 枚举值 / 策略维度存在且可选，但运行时不路由到任何实现 | 选中即静默降级，有时比不选更差 |
| M3 | 死脚手架 | 接口 / 枚举 / 异常体系零实现、零引用 | 抬高认知成本，误导扩展者 |
| M4 | starter 依赖污染 | starter 以 compile 作用域强推重依赖，污染业务应用 | 引入即改变业务应用启动行为 |
| M5 | 文档即失败 | README quickstart 照抄会启动失败 | 首次接入即失败，信任成本最高 |
| M6 | 安慰剂测试 | 测试数量可观，但恰好绕开高风险路径 | 制造"已验证"错觉 |
| M7 | 工程规范漂移 | 实际结构与 `egon-cola-components-architecture.md` 不一致 | 规范失去约束力 |

### 2.1 M1 配置面空转（分布最广）

| 组件 | 空转配置 | 状态 |
|---|---|---|
| access-guard | `storage`、`key-prefix`、`redisson.*`、`dynamic.*`、`local-fallback.*`、`thread-pool.name/max-pool-size/queue-capacity`、全局 `circuit-breaker.*` / `rate-limiter.default-*` / `blacklist.default-*`，约 14 项 | confirmed |
| dynamic-thread-pool | `trace.*`、`virtual.*`、`registry.type` | [未复核] |
| rule-engine | `default-max-steps`、`default-timeout-millis`、`async-core-pool-size` | [未复核] |
| bytecode | `executor.include` / `executor.exclude`（已绑定并校验，无人读取） | confirmed |

access-guard 的情形最典型：自动装配只消费 `corePoolSize`，其余默认值由
`AccessGuardAnnotationResolver.java:154-162` 硬编码（350ms、1/1s、24h），而这些正是配置项
声称控制的值。

### 2.2 M2 声明能力缺实现

| 组件 | 声明项 | 实际行为 | 状态 |
|---|---|---|---|
| access-guard | `FailStrategy.LOCAL_FALLBACK` | `AccessGuardFailureHandler.java:16-26` 只处理 FAIL_OPEN / FAIL_CLOSED，LOCAL_FALLBACK 落到 `throw failure` | confirmed |
| access-guard | `TimeoutExecutorType`（4 个值） | 无消费方；`VirtualThreadTimeoutCircuitBreakerExecutor` 为死代码 | confirmed |
| access-guard | 注解式黑名单累计 | `@AccessGuard` 只有 boolean 开关，`blacklistCount` 恒为 0，静默无操作 | confirmed |
| bytecode | `failure-policy=disable-feature` | `CompositeBytecodeTransformer.transform:54-67` 只特判 MARK_FATAL，disable-feature 走与默认完全相同的分支 | confirmed |
| bytecode | `ObservationEvent.traceId` | `ObservationRuntime.java:132` 硬编码 `""` | confirmed |
| bytecode | `executor.names` 别名重映射 | `ExecutorNameResolver.java:24-33` 先返回原始 bean 名，配置的映射键永不命中 | confirmed |
| rule-engine | `RouteDecision.end(data)` | 树执行器从不返回该 payload | [未复核] |
| rule-engine | 链 / 树最后一个节点的超时 | 超时判定在节点之间，末节点后不再判定 | [未复核] |
| method-extension | `engine=AGENT` | 未引入 bytecode starter 时静默不拦截，README 宣称等价 | [未复核] |
| gateway | 上游 HTTP TLS / mTLS | 仅按 `provider.secure()` 选 scheme，从不配置 SslContext / truststore / 客户端证书 | [未复核] |

### 2.3 M3 死脚手架

| 组件 | 死脚手架 | 状态 |
|---|---|---|
| common | `CodeEnum` / `IntCodeEnum` 全仓零实现；`ErrorResultDto` / `ErrorResultModel` 从未被构造；`common-test` 模块零消费方（其逻辑被内联复制两处）；`TraceSnapshot` 只能捕获不能恢复 | confirmed（4 条独立复核） |
| rule-engine | 整个 8 类异常体系为死脚手架；`RuleRouter` 接口与 `AbstractAsyncRuleNode` 无价值抽象 | [未复核] |
| gateway | 4 个安全 SPI 全仓零实现且无进入独立 Engine 的投递路径；`GatewayFilterStage` 14 个阶段中 10 个零生产引用，链本身对扩展封闭 | [未复核] |
| rpc | `RPC_REGISTRATION_FAILED`、`RPC_GATEWAY_AMBIGUOUS`、`RpcGatewayState.AMBIGUOUS` 零引用 | [未复核] |
| dcc | `DdcRedisConfigRepository` bean 已注册、全仓零使用；App / Namespace 治理端点为装饰性 CRUD，无任何运行时约束 | confirmed |
| method-extension | 事件 / 可观测层只提供 no-op publisher 且无文档 | [未复核] |

### 2.4 M4 starter 依赖污染（对接入方危害最大）

```text
1. access-guard-starter/pom.xml:44-53 以 compile 非 optional 引入
   redisson-spring-boot-starter 3.26.0；其 RedissonAutoConfigurationV2 会主动连接
   redis://127.0.0.1:6379，业务应用无 Redis 即启动失败。
   而 README 宣称"本地实现使 starter 开箱可用"，且 access-guard 自身从不装配任何
   Redisson bean —— 即：强制引入了一个自己不用的重依赖。 [confirmed]
2. dtp-starter 同样强推 redisson-spring-boot-starter，另加 commons-lang 2.6
   （2011 年即 EOL）与 fastjson2。 [未复核]
3. rpc-starter 与 gateway-provider-runtime 以 compile 非 optional 依赖
   dcc-starter，而 dcc-starter 自身非 optional 引入 redisson-spring-boot-starter，
   形成传递污染。 [未复核]
```

对照：transactional-outbox 对 spring-web / spring-rabbit / micrometer 全部使用 optional，
是本仓库的正确范例，应作为统一基准。

### 2.5 M5 文档即失败

```text
1. dcc：README quickstart 缺少必填的 tls.development-plaintext，照抄即启动崩溃。 [confirmed]
2. rpc：RpcTransportSecurity.java:19-25 在 tls.enabled=false 且
   development-plaintext=false 时抛 IllegalArgumentException，而二者默认均为 false
   （EgonRpcProperties.java:239-241），README 未提及。 [未复核]
```

这是两个组件的**首次接入体验**，优先级高于任何内部质量问题。

### 2.6 M6 安慰剂测试

| 组件 | 表面 | 实际 |
|---|---|---|
| gateway | admin 持久层约 2700 行手写 JDBC（含 594 行 `JdbcGatewayCatalogStore`） | 仅 2 个测试，均 `mock(JdbcTemplate)` 并断言 SQL 字符串片段；从未对任何真实数据库执行 |
| gateway | 12 个已声明 live 场景 | 场景目录测试只断言"静态清单等于硬编码清单"，实际映射到 2 个从未执行的测试方法 |
| transactional-outbox | `RabbitDeliveryHandlerIntegrationTest` | 第 34 行 `mock(RabbitTemplate.class)`，名为集成测试实为单测；ACK 语义零真实 broker 验证 |
| access-guard | -test 模块声称"集成验证" | 单个测试类 + 手工构造的 `ProceedingJoinPoint` 假件；无真实 Spring AOP 代理、无 Redis |
| rpc | `EgonRpcAutoConfig` | 零测试覆盖；53 个测试全部手工装配 bean，条件装配矩阵完全未验证 |
| dcc | test 模块声称跨模块验证 | 跑在测试替身上，而非发布的类 |

### 2.7 M7 工程规范漂移

```text
1. CHANGELOG.md：架构规范第 4.1 / 15 节要求每组件提供，实际 0/10。
2. docs/ 目录：规范要求 architecture.md + usage.md，实际仅 2/10 组件有。
3. 版本漂移：架构文档正文写父 POM 5.1.1、BOM 5.2.1，实际所有 POM 为 5.2.3。
4. 结构漂移：规范只认可 starter/test/admin 三件套 + common 例外，实际 bytecode 为 9
   模块、gateway 为 7 模块（含 contract/core/engine/provider-runtime），规范未追认。
5. UI 越界：gateway 下有 58 个纳入 git 的 React/Vite/TS 文件（admin-web），
   且未注册进 gateway 的 pom modules；架构规范第 10 / 13.4 节明确要求 UI 独立工程。
6. 发布面失控：release 走 central-publishing-maven-plugin，全仓零处设置
   skipPublishing —— admin / test / engine / benchmark 模块会被一并发布。
7. 无用依赖管理：父 POM 管理 com.alibaba:fastjson 1.2.83，全仓零模块声明、零 import。
```

### 2.8 跨组件重复

```text
1. 三份近乎相同的 admin trace-id servlet filter（DdcTraceIdFilter / DtpTraceIdFilter /
   gateway-admin），且 dtp 绕开 common-trace 直写裸 MDC key。
2. Redis key 构造各写各的：AccessGuardRedisKeys、DtpRedisKeys、dcc 内联拼接，
   无共享命名空间约定。
3. 分页计算块在 common 的三个模块中复制粘贴，且 pageSize 归一化行为不一致。
4. transactional-outbox 的 sanitize() 在 store 与 dispatcher 中逐行重复，
   且截断循环在超预算后继续扫描，导致宽字符被跳过而后续窄字符被保留。
5. gateway-contract 中存在两个同名不同构的 GatewayDefinitionIdentity record。
6. 4 个组件聚合 POM 各自复制粘贴相同的 common-* dependencyManagement 块。
```

---

## 3. 目标

### 3.1 功能目标

```text
1. 对外承诺面与实现面一致：README、配置项、枚举、SPI 所声明的每一项，要么有真实实现，
   要么被删除，不允许静默无效。
2. starter 恢复"最小依赖面"：引入任一 starter 不改变业务应用的启动前置条件。
3. quickstart 可直接跑通：每个组件 README 的首个示例可原样复制并成功启动。
4. 高风险路径具备真实基础设施验证：数据库、消息中间件、Spring 代理、条件装配。
```

### 3.2 质量目标

```text
1. 安装防漂移闸门，使 M1 / M2 / M3 / M4 四类模式在 CI 中可被自动拦截。
2. 消除跨组件重复实现，共享语义下沉到 common。
3. 组件工程结构与架构规范文档双向对齐（改代码或改规范，二选一，不留悬空）。
```

---

## 4. 明确边界

本设计**不覆盖**以下内容，避免与在途工作冲突：

```text
1. Gateway / DDC / RPC 三方联调闭环合同问题 —— 由 2026-07-26 联调闭环设计及其 7 份
   integration 计划负责。该工作已于 a58d7645 合入 main，合入后的复核由姊妹规格
   2026-07-26-architecture-audit-rectification-spec.md 负责。
2. Gateway 17 项差距修复 —— 由 2026-07-25 对应设计负责，状态为实施中。
3. Gateway 16 份子 Spec 的功能范围 —— 状态为"已实现，待用户验收"，本设计只针对其中
   审计新发现的实现缺口（上游 TLS、安全 SPI 投递、admin 持久层验证）。
4. archetypes 与 cola-samples —— 不属于 Components 范围。
5. 不新增任何组件，不改变现有组件的能力定位。
6. 不引入新的框架级抽象（Saga / 通用策略框架 / 通用插件容器）。
```

---

## 5. 整改总原则

### 5.1 "实现或删除"二选一，不留第三态

对 M1 / M2 / M3 三类的每一个条目，只允许两种处置：

```text
A. 实现：补齐真实逻辑 + 行为测试 + README 说明。
B. 删除：同时删除代码声明、配置属性、README 条目、设计文档中的对应描述。
```

**禁止第三种处置**：保留声明并加注释说明"暂未实现"。这正是当前状态的成因。

### 5.2 处置判据

```text
选 A（实现）当且仅当：该能力有明确使用场景，且缺失它会让组件定位不完整。
其余一律选 B（删除）。删除是默认选项。
```

### 5.3 优先级排序原则

按"谁先受伤"排序，而非按技术难度：

```text
波次 0：接入方引入即失败              —— 最高
波次 1：接入方用了但行为错误 / 有害
波次 2：接入方配了但无效（"空"的主要来源）
波次 3：扩展者被死脚手架误导
波次 4：维护者无法证明正确性
波次 5：工程规范与重复
```

---

## 6. 波次 0：接入面阻断修复（P0）

目标：让业务应用引入 starter、照抄 quickstart 就能起来。

### 6.1 W0-01 access-guard 解除 Redisson 强制装配

```text
1. pom 中 redisson-spring-boot-starter 改为 optional，或替换为
   org.redisson:redisson 裸依赖（不带 spring-boot 自动装配）。
2. 新增 @ConditionalOnClass(RedissonClient) + @ConditionalOnBean 的自动装配，
   真正装配 RedissonRateLimiterExecutor / RedissonBlacklistService /
   RedissonWhiteListRepository。
3. 使 storage、key-prefix、redisson.client-bean-name、redisson.auto-create-client
   四项配置真实生效（当前全部零读取）。
4. 验证：无 Redis 环境下的 ApplicationContextRunner 启动测试必须通过。
```

规模 M。注：`docs/superpowers/plans/2026-07-08-access-guard-component.md` 已覆盖部分内容，
实施前需先比对该计划的执行状态。

### 6.2 W0-02 dynamic-thread-pool starter 依赖瘦身

```text
1. redisson-spring-boot-starter 替换为 org.redisson:redisson 裸依赖（starter 与 admin 同步）。
2. 移除 commons-lang 2.6（EOL 2011）与 fastjson2；如确需 JSON，统一用组件内已有的
   Jackson 编解码。
3. 移除 starter pom 中零使用的 4 个依赖。
```

规模 S–M。

### 6.3 W0-03 dcc / rpc quickstart 契约修复

```text
1. 二选一：(a) 在 README quickstart 中显式给出 tls.development-plaintext=true 及其
   安全告警；(b) 将开发态默认值改为可直接启动，并在非 dev profile 下强制校验。
2. 推荐 (a)：默认值不应让"不配 TLS"变成静默可用。
3. 同步修正启动期异常信息，使其直接给出应设置的配置项全名。
```

规模 S。

### 6.4 W0-04 发布面收口

```text
1. 为所有 admin / test / engine / benchmark 模块设置 skipPublishing。
2. 校验 BOM 导出集：补 gateway-contract；为 bytecode maven 插件提供版本通道。
3. 移除父 POM 中零使用的 com.alibaba:fastjson 1.2.83 依赖管理。
```

规模 S。

### 6.5 W0-05 rpc / gateway-provider-runtime 依赖解耦

```text
1. 评估 dcc-starter 是否必须为 compile 非 optional；若接入方可选择非 DDC 注册中心，
   应改为 optional + 条件装配。
2. 若必须强依赖，则 dcc-starter 自身的 redisson 依赖必须先 optional 化（依赖 W0-01 同类改造）。
```

规模 M。[未复核] 项，实施前需先确认结论。

---

## 7. 波次 1：语义正确性（P0 / P1）

目标：消除"用了比不用更糟"的行为。

### 7.1 W1-01 common 脱敏边界泄漏（提级为 P0）

审计原评 P1，本设计**提级为 P0**。理由：这是一个名为 mask 的安全工具，在边界输入下
静默返回未脱敏原文，调用方无从察觉。

```text
1. Masking.mobile()（第 12-19 行）对任意长度 >= 7 的输入套用 first3 + "****" + last4。
   输入 "1234567"（7 位）返回 "123****4567" —— 原文 7 位数字全部暴露，且长度被放大。
   8-10 位输入同样只遮蔽极少字符。
2. keepAround()（第 36-42 行）在长度不足时原样返回输入。
3. 修复：长度不足以安全脱敏时必须走全遮蔽分支，绝不返回原文；补边界参数化测试
   （长度 0..12 全覆盖）。
```

规模 S。confirmed。

### 7.2 W1-02 common TreeBuilder 环路防护

```text
1. TreeBuilder（第 30-41 行）遇自引用父节点时执行 node.addChild(node) 且该节点永不进入
   roots；A-B 互为父子时同样静默丢节点。
2. 修复：显式检测自引用、环路与重复 id，按既定策略处理（抛出或降级为根节点），
   不允许静默丢弃。
```

规模 S。confirmed。

### 7.3 W1-03 access-guard 拒绝路径不得执行业务方法

```text
1. AccessGuardExecutionService.java:178-185 在 WHITELIST_REJECTED / BLACKLIST_HIT /
   RATE_LIMITED 判定成立后，若 rejectResponseInvoker 抛 RuntimeException，
   FAIL_OPEN 语义会让业务方法照常执行。
2. 这是治理组件的语义反转：已判定拒绝的调用，因为"生成拒绝响应失败"而被放行。
3. 修复：拒绝判定一旦成立即为终态；拒绝响应生成失败应降级为固定拒绝响应，
   不得回退到执行业务方法。
```

规模 S。confirmed。**这是本次审计中语义最危险的单点。**

### 7.4 W1-04 access-guard FailStrategy.LOCAL_FALLBACK

```text
实现或删除。当前 LOCAL_FALLBACK 落入 throw failure，比 FAIL_OPEN 与 FAIL_CLOSED 都差。
A. 实现：按 rule + hash 建本地决策缓存，遵循 local-fallback.expire-after-write，
   由 AccessGuardFailureHandler 消费。
B. 删除：同时移除枚举值、local-fallback.* 属性、README 对应行。
建议 A（本地兜底对治理组件有真实价值）。
```

规模 M。confirmed。

### 7.5 W1-05 bytecode failure-policy=disable-feature

```text
实现或删除。当前该值与默认策略行为完全相同。
建议实现：失败时关闭对应增强特性并保持进程存活，这正是该策略存在的理由。
```

规模 M。confirmed。

### 7.6 W1-06 bytecode executor.names 与 ObservationEvent.traceId

```text
1. ExecutorNameResolver 调整解析顺序，使 configuredNames 的重映射实际生效。
2. traceId：接入 common-trace 填充真实值，或删除该字段与 README 描述。
   建议填充（可观测组件的 traceId 恒为空是硬伤）。
```

规模 S–M。confirmed。

### 7.7 W1-07 rule-engine 语义修复 [未复核]

```text
1. 树执行器在 end() 决策时返回 endData。
2. 补齐末节点后的超时判定。
3. 停止静默覆盖调用方设置的 RuleContext maxSteps / timeout。
4. 修正 async 线程池语义：async-core-pool-size 当前为 no-op，实际为固定池 + 无界队列，
   与文档描述不符。
```

规模 M。实施前需先复核。

### 7.8 W1-08 method-extension engine=AGENT 快速失败 [未复核]

```text
配置 engine=AGENT 但 agent runtime 不在场时必须启动期失败并给出明确指引，
不得静默不拦截。同时将 @MethodExtension 声明校验（handler 缺失 / 歧义、
returnJson 非法、ObjectMapper 缺失）从惰性改为启动期。
```

规模 M。

### 7.9 W1-09 gateway 上游 TLS / mTLS [未复核]

```text
当前仅按 provider.secure() 选择 https scheme，从不配置 SslContext、truststore 或客户端
证书，实际得不到 TLS 保证。需实现真实信任材料装配，并保证失败时 fail-closed。
```

规模 L。实施前需先复核，并与 gateway 在途设计对齐边界。

---

## 8. 波次 2：配置面归一（"空"的主要来源）

对第 2.1 节列出的每一项配置执行"实现或删除"，逐组件收口：

```text
1. access-guard：约 14 项。重点是命名有界线程池（当前 newFixedThreadPool 无界队列）、
   全局默认值注入 RuleSpec、dynamic.enabled 真正门控 configProvider 调用。
2. dynamic-thread-pool：trace.*、virtual.*、registry.type。
3. rule-engine：default-max-steps、default-timeout-millis 接入执行器。
4. bytecode：executor.include / exclude 实现或删除。
```

交付要求：每一项都必须落到"有生产读取方 + 有断言其生效的测试"，或从属性类与 README 中消失。

规模 L（横跨 4 组件）。

---

## 9. 波次 3：死脚手架清理

对第 2.3 节条目执行"实现或删除"，默认删除。

```text
1. common：CodeEnum / IntCodeEnum、ErrorResultDto / ErrorResultModel、common-test 模块
   （或反向：让两处内联复制改为依赖它）、TraceSnapshot 恢复路径（建议补齐 restore，
   使 capture/restore 成对）。
2. common 异常体系：9 类中 8 类未使用且无自定义消息构造器 —— 建议收敛为
   EgonException + ErrorStatus + 自定义消息构造器。
3. rule-engine：8 类异常体系、RuleRouter、AbstractAsyncRuleNode。
4. rpc：3 个零引用常量。
5. dcc：DdcRedisConfigRepository bean；App / Namespace 治理端点需明确"要么施加运行时约束，
   要么下线"。
6. gateway：安全 SPI 需给出投递路径 + 至少 1 个参考实现，否则删除；
   GatewayFilterStage 14 个阶段收敛为实际使用的 4 个，或开放链的扩展点。
7. method-extension：事件层提供真实 publisher 并补文档，或删除。
```

规模 L。

---

## 10. 波次 4：测试真实化

对第 2.6 节的安慰剂测试补真实基础设施验证：

```text
1. gateway admin 持久层：对真实 PostgreSQL（Testcontainers）验证 9 个 Jdbc store，
   替代断言 SQL 字符串的 mock 测试。—— 优先级最高，约 2700 行代码零真实验证。
2. transactional-outbox：真实 RabbitMQ broker 集成套件，验证 ACK 语义与
   mandatory return 行为。
3. access-guard：-test 模块补真实 Spring AOP 代理 + Redis 路径验证。
4. rpc：为 EgonRpcAutoConfig 补 ApplicationContextRunner 条件装配矩阵测试。
5. dcc：补 1 个 DDC-only 真实进程集成测试。
6. gateway：将 12 个场景码绑定到实际执行的 live 场景，或下调声明。
```

统一约定：依赖外部中间件的测试用环境变量 assumption 门控（沿用 outbox 的
`EGON_OUTBOX_TEST_POSTGRES_ENABLED` 模式），由专用 CI job 执行，保证默认构建不依赖 Docker。

规模 L。

---

## 11. 波次 5：工程规范与去重

### 11.1 规范对齐

```text
1. 补齐 10 个组件的 CHANGELOG.md 与 docs/（architecture.md、usage.md）。
2. 架构文档版本号同步为 5.2.3（当前正文写 5.1.1 / 5.2.1）。
3. 架构文档追认 bytecode 9 模块与 gateway 7 模块形态，或反向调整代码结构。
   建议追认并补充"多模块运行时组件"形态说明。
4. gateway admin-web 定位决策：迁出为独立前端工程（符合规范第 10 章），
   或在架构文档中明确记为受控例外。README 当前已按例外描述，但规范正文未追认。
```

### 11.2 跨组件去重

```text
1. trace-id filter：三份合一，统一走 common-trace（dtp 当前直写裸 MDC key）。
2. Redis key：下沉统一命名空间约定到 common。
3. 分页计算：下沉到 common-model，统一 pageSize 归一化行为。
4. outbox sanitize()：抽公共文本清洗器，并修正截断循环的宽字符跳过缺陷。
5. gateway-contract 两个同名 GatewayDefinitionIdentity 合一。
6. 4 个聚合 POM 的重复 dependencyManagement 上提至父 POM。
```

### 11.3 性能与健壮性（P2 集）

```text
1. outbox：HTTP 投递复用 HttpClient / RestClient（当前每次投递新建，含新 TLS 握手）；
   为自定义 handler 加 deadline 看门狗（当前挂起会永久泄漏投递许可）；
   节流 backlog count(*)（当前每批 dispatch 后全表计数，默认 1s 轮询）。
2. rpc：将 channel 连接移出 RpcConsumerGatewayManager 的 monitor
   （当前持锁串行 connect，默认 5000ms 超时期间阻塞全部调用）；补 Micrometer 指标与
   HealthIndicator（当前生命周期关键循环仅 LOGGER.warn）。
3. dtp：实例心跳 TTL + 关闭时注销（当前 DTP:APPS 与实例集合只增不减）；
   变更消息按 instanceId 过滤后再审计（当前每次 resize 广播都会由所有非目标实例
   记录失败审计事件）；starter 与 admin 共用一套加固的 Redis codec
   （当前重复且开启了不安全的 Jackson default typing）；为 admin 写端点加鉴权。
4. dcc：admin 读端点分页 + DTO 映射（当前返回无界原始 JPA 实体、未命中返回 200 + null）；
   @DdcValue 绑定需在 bean 销毁时解绑（当前非单例 bean 泄漏并持续被反射写入）；
   admin 异常映射覆盖 EgonException / DdcAdminException（当前全部塌缩为匿名
   INTERNAL_FAILURE）。
5. method-extension：缓存 handler 与注解解析（当前每次拦截都做全量
   beanFactory.getBeansOfType 扫描）。
6. bytecode：停止在每次类加载时重建并发布完整 agent 状态；加固架构缓存配置摘要
   （当前是对无序 map toString 取 32 位 hashCode）。
7. gateway：拆分 admin-web Dashboard chunk。
```

---

## 12. 防漂移闸门（最重要的一章）

只修不防，半年后回到原点。本章定义 CI 可执行的自动闸门，每一类对应一种缺陷模式。

### 12.1 G1 配置项活性闸门（对应 M1）

```text
规则：每个 @ConfigurationProperties 类的每个字段，必须在生产源码中至少有一个读取方。
例外机制：字段标注 @ReservedConfig("原因 + 计划")，例外清单必须显式列出且数量受限。
实现：测试期反射扫描属性类字段 + 对 getter 的静态引用分析。
失败即构建失败。
```

### 12.2 G2 策略枚举穷尽闸门（对应 M2）

```text
规则：被配置或注解暴露的策略枚举，其每个常量必须在对应的分发点被显式处理。
实现：优先用 Java 21 switch 模式匹配的穷尽性（去掉 default 分支，让编译器强制）；
     无法改造处补测试遍历 values() 断言不落入兜底分支。
```

### 12.3 G3 死脚手架闸门（对应 M3）

```text
规则：导出包中的接口 / 抽象类，必须存在生产实现或明确标注为用户扩展点
     （@ExtensionPoint + 至少一个参考实现或测试替身）。
实现：接入已有的 egon-cola-component-bytecode-architecture-maven-plugin。
     该插件已提供构建期架构检查能力，本闸门是其自然延伸。
```

### 12.4 G4 starter 依赖面闸门（对应 M4）

```text
规则：starter 的 compile 作用域依赖必须在白名单内；引入任何带 spring-boot 自动装配的
     第三方 starter 一律拒绝。
基准：以 transactional-outbox-starter 的依赖形态为正例。
实现：maven-enforcer bannedDependencies + 每个 starter 一个
     "无外部中间件环境下 ApplicationContextRunner 启动通过"的测试。
```

### 12.5 G5 quickstart 可执行闸门（对应 M5）

```text
规则：README 首个 quickstart 配置块必须以可执行资源形式存在
     （如 src/test/resources/quickstart.yml），并有一个测试用它启动上下文。
效果：README 配置一旦与代码要求脱节，构建立即失败。
```

### 12.6 闸门落地顺序

```text
G4 与 G5 先行（成本最低、收益最直接），G1 次之，G2 / G3 随波次 2 / 3 同步落地。
每个闸门上线时允许携带初始例外清单，但例外清单只能减不能增，并在 CHANGELOG 中可见。
```

---

## 13. 分组件任务索引

| 组件 | 成熟度 | P0 | P1 | 主要工作 |
|---|---|---|---|---|
| common | functional | W1-01 脱敏泄漏 | W1-02 树环路、TraceSnapshot | 死脚手架清理、异常体系收敛、分页去重 |
| access-guard | functional | W0-01 Redisson、W1-03 拒绝路径、W1-04 LOCAL_FALLBACK | 14 项空转配置、TimeoutExecutorType、黑名单注解 | 配置面归一 + 真实集成测试 |
| bytecode | mature | W1-05 disable-feature | executor.names、traceId、endpoint 隐私 | 大部分为 P2 优化 |
| dynamic-config-center | functional | W0-03 quickstart | admin 异常映射 | 治理端点定位、admin 读端点分页 |
| dynamic-thread-pool | functional | W0-02 依赖瘦身 | 实例存活、审计噪声、admin 鉴权 | 配置面归一 + codec 合一 |
| rpc | functional | W0-03 quickstart | 自动装配测试、consumer 锁热点 | W0-05 依赖解耦、可观测性 |
| rule-engine | functional | — | W1-07 语义四项 | 死脚手架清理（占比最高） |
| method-extension | functional | — | W1-08 AGENT 快速失败、启动期校验 | 热路径缓存 |
| transactional-outbox | mature | — | RabbitMQ 真实验证、HTTP 客户端复用、看门狗 | 质量最高，工作量最小 |
| gateway | functional | — | W1-09 TLS、安全 SPI、admin 持久层验证 | 与在途设计对齐边界后再动 |

说明：transactional-outbox 与 bytecode 是本仓库质量基准，其形态应作为其他组件的参照。

---

## 14. 实施与提交顺序

```text
波次 0（接入面）        —— W0-01 .. W0-05，每项独立提交
闸门 G4 / G5 上线        —— 锁定波次 0 成果，防止回退
波次 1（语义正确性）    —— W1-01 .. W1-09，按组件独立提交
闸门 G1 上线
波次 2（配置面归一）    —— 按组件独立提交
闸门 G2 / G3 上线
波次 3（死脚手架清理）  —— 按组件独立提交
波次 4（测试真实化）    —— 按组件独立提交
波次 5（规范与去重）    —— 按主题独立提交
```

约束：

```text
1. 每个任务单独提交，提交信息注明波次与任务号。
2. 每个波次结束执行 ./mvnw -V --no-transfer-progress clean install 全量验证。
3. 删除类改动必须同步删除 README、属性类、设计文档中的对应描述，一次提交内完成。
4. [未复核] 标注的任务，实施前必须先完成证伪式复核，复核结论写回本文档。
```

---

## 15. 完成定义

```text
1. 第 2 章七类模式的每一个列举项，均处于"已实现并有测试"或"已删除且文档同步"状态。
2. 五个闸门 G1-G5 在 CI 中生效，例外清单已固化且有明确理由。
3. 每个组件的 README quickstart 可原样复制并成功启动，且由测试保证。
4. 任一 starter 在无外部中间件环境下可完成 Spring 上下文启动。
5. 10 个组件均具备 README.md、CHANGELOG.md、docs/architecture.md、docs/usage.md。
6. 架构规范文档与实际结构双向一致，无悬空条款。
7. 全量 ./mvnw clean install 通过；真实基础设施测试在专用 CI job 中通过。
```

---

## 16. 风险与取舍

```text
1. 删除优先可能误删有真实规划的能力。
   缓解：第 5.2 节判据 + 波次 3 前逐条确认，删除均可从 git 历史恢复。
2. 波次 2 / 3 横跨多组件，易与 gateway 在途实施冲突。
   缓解：gateway 相关项排在最后，且先与在途设计对齐边界。
3. 闸门可能在存量代码上大面积报红。
   缓解：允许携带初始例外清单上线，只减不增。
4. 真实中间件测试增加 CI 时长。
   缓解：沿用 assumption 门控 + 专用 job，默认构建不受影响。
5. 本文 P1 及以下条目中约半数标注 [未复核]（复核阶段因会话额度中断）。
   缓解：实施前逐条复核。已复核的 22 条中 21 条 confirmed、0 条 refuted，
   说明审计基线可靠，但不能免除对未复核项的验证。
```

---

## 17. 已实施记录

分支：`worktree-components-capability-hardening`。以下任务已实施并通过所在模块测试。
未复核项一律未动。

| 提交 | 任务 | 关键改动 | 验证 |
|---|---|---|---|
| `424edcec` | W1-01 | `keepAround` 窗口覆盖全串时改为全遮蔽；`mobile` 仅在能遮蔽 ≥4 字符时才用 3/4 窗口 | mask 模块 20/20，含长度 1–12 参数化用例 |
| `ff6cbbb6` | W1-02 | 定位每个环的闭合节点并只提升该节点为根，其余成员仍挂接父节点；新增 `TreeOptions.failOnCycle` | structure 模块 6/6，含"挂在环上的节点仍保持挂接" |
| `c8889ebe` | W1-03 | 拒绝判定为终态，拒绝响应生成失败改抛 `AccessGuardRejectedException`，不再 `proceed()` | starter 51/51，新增用例断言 `never()).proceed()` |
| `5d4f86f9` | W1-04 | `LOCAL_FALLBACK` 降级到本地决策路径并遵循 `local-fallback.enabled`；策略 switch 改为穷尽；删除 `expire-after-write` 及两处 README | starter 55/55 |
| `c1594eb7` | W1-05 | `DISABLE_FEATURE` 改为永久停用增强并调用此前零调用方的 `AgentStateStore.disableFeature` | agent 模块 15/15，新增用例证明与 `SKIP_CLASS` 行为分叉 |
| `b80d154e` | W1-06 | `executor.names` 改为按已解析名称优先匹配；`ObservationEvent.traceId` 由 starter 注入 MDC 供给器 | runtime 16/16、starter 29/29，隐私与依赖边界用例仍通过 |
| `c5cce38c` | W0-02 | dtp starter/admin 换成裸 `org.redisson:redisson`；移除 fastjson2 与 commons-lang 2.6 | starter 70/70、admin 27/27，新增依赖边界用例 |
| `41aa9748` | W0-01 | access-guard Redisson 改 optional，新增 `AccessGuardRedissonAutoConfiguration` 真正装配 REDISSON 存储；`Storage` 增加 `LOCAL` 并作为默认；显式声明 jackson-databind | starter 60/60、test 模块 7/7 |

实施过程中发现并修正的两处设计偏差，与本文原建议不同：

```text
1. W1-04 原建议实现"本地决策缓存并遵循 expire-after-write"。实际代码的本地兜底是一个活的
   本地执行器（RedissonRateLimiterExecutor 已按此假设编写），再加缓存等于引入第二套冗余机制。
   因此改为对齐既有机制，并删除无法被任何机制兑现的 expire-after-write。
2. W0-01 移除 redisson-spring-boot-starter 后暴露出 access-guard 的 returnJson 功能一直在
   传递依赖 Redisson starter 携带的 Jackson。已显式声明 jackson-databind。这正是 M4 依赖污染
   会掩盖真实耦合的实例。
```

`Storage` 默认值由 `REDISSON` 改为 `LOCAL` 属于契约修正而非行为变更：原枚举只有 `REDISSON`
一个取值，且没有任何 Redisson Bean 被装配，实际运行行为一直是本地。

## 18. 待确认项

请确认以下决策，确认后本文状态改为"已确认，待实施"，并据此拆分实施计划：

```text
1. 总体方案：认可"承诺面 > 实现面"的问题定性，以及"实现或删除、不留第三态"的处置原则？
2. 波次顺序：认可按"谁先受伤"排序（接入面 -> 语义 -> 配置 -> 脚手架 -> 测试 -> 规范）？
3. 闸门范围：五个闸门 G1-G5 是否全部落地？其中 G3 依赖 bytecode 架构插件，是否接受该耦合？
4. 逐项处置分歧点（实现 or 删除）：
   a. access-guard FailStrategy.LOCAL_FALLBACK      —— 本文建议实现
   b. bytecode failure-policy=disable-feature        —— 本文建议实现
   c. common CodeEnum / IntCodeEnum / ErrorResultDto —— 本文建议删除
   d. common 异常体系 9 类收敛为 1 类 + ErrorStatus  —— 本文建议收敛
   e. rule-engine 8 类异常体系                       —— 本文建议删除
   f. gateway 安全 SPI                               —— 本文建议实现投递路径 + 参考实现
   g. gateway GatewayFilterStage 14 阶段             —— 本文建议收敛为 4 个
   h. dcc App / Namespace 治理端点                   —— 本文建议施加运行时约束或下线
5. gateway admin-web：迁出为独立前端工程，还是在架构规范中追认为受控例外？
6. 架构规范文档：追认 bytecode / gateway 的多模块形态，还是调整代码结构向规范靠拢？
7. 是否需要为本设计拆分逐波次的实施计划文档（放入 docs/superpowers/plans/）？
```
