# RPC 运行时治理演进实施计划

| Field | Value |
| --- | --- |
| Document | `2026-08-21-12-59-rpc-runtime-governance-implementation.md` |
| Template Version | `2` |
| Status | `Review` |
| Created | `2026-08-21 12:59 CST` |
| Updated | `2026-08-21 12:59 CST` |
| Owner | `Egon-COLA platform owner / User` |
| Repository | `/Users/mario/SelfProject/Egon-COLA` |
| Scope | `RPC Starter, RPC DDC Adapter, Access Guard rate-limit runtime, RPC test-contract/process tests, bilingual component documentation` |
| Source Requirement | `2026-08-19 and 2026-08-21 user requirements and confirmations: fixed Gateway/Direct client mode, same-mode reselection, cached DDC discovery, Provider-owned heartbeat, five load-balancing algorithms, graceful lifecycle, Guard-based Provider rate limiting, blocking/CompletionStage/generic unary calls, shared-channel multiplexing, CGLIB proxying, business-owned idempotency, business errors remain terminal, FAIL_OPEN returns null` |
| Baseline Revision | `main@0aa6673d7af572c0a13618716e9a2c76f8d33863; branch ahead of origin/main by 3; unrelated staged GatewayContractVersions.java deletion and unrelated untracked files preserved` |
| Implements Spec | [RPC 运行时治理演进规格](../spec/2026-08-19-15-36-rpc-runtime-governance-evolution.md) |
| Spec Status | `Review` |
| Spec Revision | `Updated 2026-08-21 10:19 CST; commit 0aa6673d` |
| Effective Specs | [RPC 运行时治理演进规格](../spec/2026-08-19-15-36-rpc-runtime-governance-evolution.md); [DDC 单机闭环与轻量 RPC 设计](../../superpowers/specs/2026-07-24-ddc-standalone-rpc-framework-design.md); [Gateway Engine RPC 数据面](../../superpowers/specs/2026-07-25-gateway-engine-rpc-design.md); [DDC/RPC/Gateway 统一服务模型](../../superpowers/specs/2026-07-26-ddc-rpc-gateway-unified-service-model-design.md); [Gateway BIZ/APP 与 DDC 直连 RPC](../spec/2026-08-15-16-57-gateway-biz-app-scope-direct-rpc-design.md) |
| Depends On Plans | `None` |
| Supersedes | `None` |
| Superseded By | `None` |
| Related Plans | `None` |

## 1. Summary

本计划以 [RPC 运行时治理演进规格](../spec/2026-08-19-15-36-rpc-runtime-governance-evolution.md) 为唯一主目标，把实现拆成 12 个按依赖排序、各自测试优先、各自产生一个路径受限提交的 Step。顺序为：先扩展 Access Guard 三种限流算法及 Redis 原子状态，再建立 RPC 调用策略、负载均衡、权重、共享 Channel、固定模式 Reference 与 Consumer 生命周期，随后实现统一 blocking/async 执行器、异步 Contract/Provider、泛化调用和 CGLIB 注入，最后接入 Guard Provider Adapter、Provider 生命周期、真实 TCP/进程测试与双语文档。

完成证据包括 75 个 Spec 测试场景在聚焦单测、组件测试、可选 Redis Testcontainers、RPC test-contract TCP 和用户控制的进程级 DDC/Redis 验证中的映射；每步都定义 RED 原因、最小 GREEN、验证命令、失败回退点和独立 commit。Plan 阶段不修改生产/测试源码、不启动任何服务、不执行迁移；关系数据库和前端均不受影响。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path：[RPC 运行时治理演进规格](../spec/2026-08-19-15-36-rpc-runtime-governance-evolution.md)。
- Status：`Review`；文档自身 verdict 为 `PASS — Ready for user review`，尚未标记 `Accepted`。
- Revision：`Updated 2026-08-21 10:19 CST`；关系与行为决策提交为 `0aa6673d`。
- Approval evidence：用户在关闭 7 个公共行为问题后显式调用 `$egon-coding-writing-plan`，授权在 Review Spec 上继续规划；因此本 Plan 保持 `Review`，不得据此开始实现。

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [RPC 运行时治理演进规格](../spec/2026-08-19-15-36-rpc-runtime-governance-evolution.md) | `Review`; 2026-08-21 10:19 CST; `0aa6673d` | §1–§20 全部 | 23 条要求、14 个已关闭决策、接口/模型/失败/测试/发布的唯一当前权威 |
| Amendment source | [DDC 单机闭环与轻量 RPC 设计](../../superpowers/specs/2026-07-24-ddc-standalone-rpc-framework-design.md) | Gateway Mock 边界修订，待确认 | §7.4、§8、§9、§10、§11.4、§13–§16、§20 | 保留 DDC lease、event+full-pull subscription、Proto unary、metadata/status、Provider heartbeat 基线；其“无 Consumer LB/重试/泛化/限流”和 JDK Proxy 被 Primary 修订 |
| Dependency | [Gateway Engine RPC 数据面](../../superpowers/specs/2026-07-25-gateway-engine-rpc-design.md) | 已实现，待验收 | §5–§13 | 保留 Gateway raw unary、safe metadata、Provider route/retry 独立层和 trailer 重建边界；本计划不修改 Gateway production code |
| Amendment source | [DDC/RPC/Gateway 统一服务模型](../../superpowers/specs/2026-07-26-ddc-rpc-gateway-unified-service-model-design.md) | S1–S4 已实施，S5 勘误 | 勘误 E1/E3、§2、§3.2、§3.4、§4.3–§4.4 | 保留 `gateway.weight` 的 1–10000 类型化事实、field-free Provider marker 和 additive RPC annotations；`retryOnlyIdempotent` 被 Primary 明确修订 |
| Predecessor/partial replacement | [Gateway BIZ/APP 与 DDC 直连 RPC](../spec/2026-08-15-16-57-gateway-biz-app-scope-direct-rpc-design.md) | `Review`; 2026-08-15 16:57 CST | §7.1–§7.2、§9 `META-001`、§15 Security、§16 route retirement | 保留 Gateway/Direct 明示模式、精确 Provider query、credential relay 与业务 Provider 授权；其固定 Round Robin/无 client governance 部分被 Primary supersede |

### 2.3 Superseded or excluded content

- Primary Spec 已修订 2026-07-24 的唯一 Gateway、无 Consumer Provider Directory/LB/retry/limit/generic、JDK Proxy 与 blocking-only 约束。
- Primary Spec 已修订 2026-07-26 的 `retryOnlyIdempotent=true`：RPC Consumer 不读取 `idempotent` 来决定重试，业务以唯一单据号、upsert/覆盖等方式承担重复安全。
- Primary Spec 只 supersede 2026-08-15 Spec 的 RPC Reference/Direct/runtime governance；Gateway BIZ/APP 安全、credential relay、Definition/Release 和 Provider lease 分离仍有效。
- Gateway Engine production implementation、DDC Admin/Starter subscription implementation、IdP/RBAC3、Proto wire、关系数据库/Flyway、前端、streaming、AOT/native-image 均排除在写范围之外。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| `REQ-001` | Primary §4, §9.2.1–§9.2.2 | 两个 Reference 复用同义公共策略字段，Direct 只额外拥有目标身份 | annotation defaults additive；resolver 产出同一 `RpcReferencePolicy` | annotations、reference models/resolver、proxy tests |
| `REQ-002` | Primary §4, §9.2.1 | `@EgonRpcReference` 固定 Gateway primary | 只登记/读取 Gateway demand；既有源码兼容 | Gateway Strategy、BPP、process counter |
| `REQ-003` | Primary §4, §9.2.2 | `@EgonRpcDirectReference` 固定 Direct primary | 只登记/读取 exact `RPC_PROVIDER` query；Gateway calls=0 | Direct Strategy、Provider Manager/DDC adapter |
| `REQ-004` | Primary §4, §7.3.4 | 可用性失败只在同模式换未尝试实例，禁止跨模式 | acquisition/UNAVAILABLE 才重选；候选耗尽报模式错误 | executor、LB、mode strategies |
| `REQ-005` | Primary §4, §7.3.1 | Consumer 缓存目录并复用现有 DDC event+full pull | 每 query 单订阅；调用热路径无 DDC pull | managers、existing DDC subscription boundary |
| `REQ-006` | Primary §4, §7.3.4 | revision 单调、lease 过期摘除、对账恢复 | 旧 revision 丢弃；DDC 失败后本地过期，不无限保留 | managers、DDC adapter regression |
| `REQ-007` | Primary §4, §10.3 | Provider 主动 heartbeat，间隔由 RPC 配置；DDC 不反向探活 | fixed-delay register/heartbeat/recover；interval < lease | Provider lifecycle/lease tests |
| `REQ-008` | Primary §4, §7.3.1 | 支持随机、加权随机、轮询、平滑加权轮询、一致性 Hash，并保留 least-in-flight | enum 全覆盖、算法确定性/统计性/排除行为通过 | LB Strategy/Factory、properties |
| `REQ-009` | Primary §4, §7.3.1, §10 | 权重统一来自 `gateway.weight`；一致性 Hash 使用业务 affinity key | Provider/Gateway default=100；typed resolver/generic key稳定且不泄露 | endpoint/codec、resolver、LB tests |
| `REQ-010` | Primary §4, §10.6 | Provider 优雅启动仅在 Server+lease 后 READY | STARTING/READY/DEGRADED/FAILED 状态可观测 | Provider lifecycle/state |
| `REQ-011` | Primary §4, §10.6 | Provider/Consumer 优雅关闭，拒绝新调用并有界排空 | DRAINING gate；in-flight drain；timeout force close；callback once | lifecycles、pool、server tests |
| `REQ-012` | Primary §4, §9.2.3, §11 | Provider 方法复用 Guard `@RateLimitGuard`，支持 token/leaky/sliding 三算法 | Local/Redisson parity、配置非法 fail-fast、Provider marker不加字段 | Guard runtime/tests、RPC optional dependency |
| `REQ-013` | Primary §4, §9.2.3 | Guard `RATE_LIMITED` 映射 gRPC `UNAVAILABLE`/provider/rate-limit | target bean不执行；Direct pure limit exhaustion=`RPC_RATE_LIMITED`；Gateway保持 provider unavailable | mapper/autoconfig/status/executor |
| `REQ-014` | Primary §4, §9.2.4 | 保留 typed blocking unary | blocking Message→Message 与旧 Echo 合同兼容 | validator/plan/executor/proxy |
| `REQ-015` | Primary §4, §9.2.5/§9.2.7 | 支持 typed/generic `CompletionStage`，native cancellation 和单终态 | 方法立即返回；cancel active ClientCall；错误类型与 blocking parity | validator/server/executor/generic |
| `REQ-016` | Primary §4, §9.2.6–§9.2.7 | 泛化调用复用 canonical `fully.qualified.Service/Method` 与 raw Protobuf bytes | 无 dot alias/任意 metadata；blocking/async parity；bounded target cache | generic API/cache/raw descriptor |
| `REQ-017` | Primary §4, §7.3.1 | 同 transport key 共享 ManagedChannel 多路复用 | concurrent acquire 单建链；并发 unary 共 Channel；ref/in-flight drain | channel pool/managers/component test |
| `REQ-018` | Primary §4, §13 | typed proxy 使用 Spring CGLIB，并在 proxy time 编译 method plan | class为 CGLIB；Object methods本地；调用期 O(1) lookup | proxy factory/interceptor/descriptors |
| `REQ-019` | Primary §4, §7.3.3 | RPC retry 不考虑幂等，业务承担重复安全 | `idempotent=false` 在配置 retries 时仍发送下一 attempt | executor tests/docs |
| `REQ-020` | Primary §4, §7.3.4 | 业务错误是 terminal；只有 acquisition/UNAVAILABLE 可重试/执行 availability fail strategy | non-UNAVAILABLE 一次调用并原样映射；FAIL_OPEN 仅耗尽后返回/完成 null | executor/status/fallback tests |
| `REQ-021` | Primary §4, §16 | 保持 Proto/DDC/Gateway/Provider marker/旧 annotations 兼容 | 无 `.proto`/Gateway/Flyway production diff；legacy APIs compile | compatibility/static gates |
| `REQ-022` | Primary §4, §14–§16 | 双语 README、配置、错误与发布说明同步 | 中英文语义一致；无旧矛盾；聚焦/模块测试通过 | four README files、quality gates |
| `REQ-023` | Primary §4, §10.6 | Consumer 启动先装 discovery/pool 后开放调用；失败/关闭完整清理 | READY/DEGRADED/FAILED/DRAINING/STOPPED 顺序与回调可测 | Consumer coordinator、AutoConfig |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

1. 先在 Access Guard 内把算法枚举、Strategy/Factory、Local state 和 Redisson Lua/state 完成；RPC 只消费 Guard rejection，不复制限流算法或 key authority。
2. 再建立 RPC 客户端的公共注解属性、LB Strategy/Factory、权重投影和共享 Channel pool，使后续 Reference/Executor 有稳定依赖。
3. 将 Gateway/Direct 声明解析成固定模式 Strategy；Manager 继续只持有 DDC snapshot，Channel 所有权转交共享 pool；Consumer coordinator 统一 start/drain gate。
4. 用一个 `RpcInvocationExecutor` 同时实现 blocking/async、deadline、same-mode reselection、business-terminal、availability fail strategy 和 nullable FAIL_OPEN；后续 typed/generic 都只构造 plan。
5. 扩展 Contract/Provider 支持 `CompletionStage<Response>`，再增加 raw generic command/cache，避免两套执行循环。
6. 最后把 typed 注入迁移到 CGLIB method interceptor，统一 AutoConfig；Provider Guard Adapter 和显式生命周期状态在核心调用链稳定后接入。
7. 真实 TCP/进程测试与 README 最后收口，且进程级 DDC/Redis 仍由用户显式开启；每个前置 Step 已有模块内 RED/GREEN，不依赖最终大测试定位基础错误。

### 4.2 Test-first strategy

| Behavior | RED test and expected RED reason | Minimum GREEN | Permitted wiring/refactor |
| --- | --- | --- | --- |
| Guard Local algorithms | enum/factory/local tests因 LEAKY_BUCKET/SLIDING_WINDOW 或策略不存在而 compile/fail | three isolated maps/state machines + exact retryAfter | keep existing backend/failure SPI |
| Guard Redis algorithms | key/script tests因 suffix/script/type contract不存在而 fail | legacy token HASH + leak HASH + window LIST Lua | optional Testcontainers parity |
| RPC policy/LB | properties/LB tests因 enum/SPI/factory缺失而 compile/fail | five algorithms + least-in-flight, resolver validation | no network/channel creation in LB |
| Weight projection | DDC tests因 endpoint无 weight/decoded value ignored而 fail | `ServiceInstanceMeta.weight` → both endpoint records | no DDC wire change |
| Channel pool | pool tests因 pool/key/lease不存在而 compile/fail | single-flight entry, ref/in-flight, drain/force close | existing channel factory builds transport |
| Fixed-mode cache/lifecycle | resolver/strategy/manager/coordinator tests因统一 definition/gate不存在而 fail | one mode strategy + snapshot-only managers + lifecycle state | reuse DDC subscription, no new polling job |
| Invocation semantics | executor tests因 executor/plan不存在而 compile/fail | bounded same-mode loop/state machine; business errors terminal | one blocking/async core |
| Async Provider/Contract | validator/server tests因 CompletionStage 被拒绝/observer不桥接而 fail | exact generic type validation + single-terminal observer | existing unary descriptor unchanged |
| Generic API | generic tests因 API/cache/raw descriptor不存在而 compile/fail | canonical raw unary and bounded cache | same executor/security interceptor factories |
| CGLIB/wiring | proxy/BPP/context tests因 JDK Proxy/dual factories/old handler而 fail | CGLIB interceptor + precompiled map + unified AutoConfig | preserve compatibility constructor for programmatic direct clients |
| Guard RPC + lifecycle | component/mapper/lifecycle tests因 optional adapter/state/weight metadata不存在而 fail | conditional mapper, UNAVAILABLE trailers, explicit states/heartbeat | `@EgonRpcProvider` stays field-free |
| Cross-module acceptance | TCP/process/docs tests因 async/generic/multiplex/no-cross-mode assertions缺失而 fail | real loopback TCP and explicit live profile | no automatic service start |

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | Step 3 | Guard plan/policy/local backend files | Establishes algorithm contract and Local semantics |
| Step 2 | Step 1 | Step 3 | Guard Redisson/key files | Uses Step 1 algorithm-bearing request/Factory |
| Step 3 | None | Steps 1–2 | RPC annotations/config/loadbalance files | Pure consumer contracts/algorithms |
| Step 4 | Step 3 | Step 2 | RPC endpoint/DDC directory files | Adds canonical weights to LB candidates |
| Step 5 | Step 4 | None | RPC channel package | Pool key depends on final endpoint contract |
| Step 6 | Steps 3–5 | None | RPC reference/managers/lifecycle packages | Fixed-mode strategies need LB, weights and pool |
| Step 7 | Step 6 | None | RPC invocation/error/metadata files | Executor needs stable mode/candidate/pool contracts |
| Step 8 | Step 7 | Step 9 only after Step 7 API fixed | RPC contract/provider binding/server files | Async server and descriptors consume invocation mode |
| Step 9 | Steps 6–8 | None | RPC generic package | Generic cache uses fixed-mode strategy and executor |
| Step 10 | Steps 7–9 | None | RPC proxy/core AutoConfig files | Final wiring needs every consumer runtime component |
| Step 11 | Steps 1–2, 8, 10 | None | RPC Guard/lifecycle/lease files | Adapter needs Guard API and provider exception chain |
| Step 12 | Steps 1–11 | None | READMEs and RPC test-contract/process files | Final integration evidence only after contracts stabilize |

### 4.4 Commit boundaries

每个 Step 产生一个 semantic commit，并以 `git commit --only <Step-owned paths>` 提交。Guard Local/Redis 分成两个提交以隔离内存算法正确性和 Redis Lua/兼容 key 风险；RPC contract/runtime/wiring分层提交以保证每个中间 revision 可编译并可用聚焦测试定位。不存在跨 Step 修改同一文件；如果实施中发现某文件必须二次修改，先更新 Plan 的唯一 owner Step，再继续执行，而不是把相同路径散落到多个 commit。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| 两个公开 Reference annotations | Keep/Modify；Primary §7.0, §9.2.1–§9.2.2 | 两个注解和双 BPP 已存在 | 不造第三注解/嵌套 options | additive fields + one resolver | Implement common policy; keep both public shapes |
| `RpcReferencePolicy/Definition` | Create；Primary §7.0, §10.1 | 当前 flat annotation 直接传入两个 proxy factories | 直接 if/else 会复制 override/validation | two immutable records + resolver | Implement |
| Mode Strategy + Factory | Create；Primary §7.0, §13 | current Gateway/Provider channel providers分离但 proxy factories重复 | 单 handler switch会允许误查另一 Directory | 2 strategies, one factory, closeable demand | Implement exact immutable mode |
| DDC Consumer cache/job | Reuse；Primary §7.0 | `DdcManagedRegistrySubscription` 已 listener-before-pull、event coalesce、fixed-delay reconcile、expireLocal | 新 RPC polling/cache 是重复状态 | zero new DDC files/jobs | Already exists; managers consume snapshots only |
| LB Strategy/Factory | Create；Primary §7.3.1 | Manager硬编码 RR；Gateway已有 factory precedent但模块不可依赖 | one switch in manager would duplicate mode code | 5 algorithms + compatibility state/cleanup | Implement in neutral RPC package |
| Hash resolver SPI | Create；Primary §9.2.8 | invocation ID/request全体不能代表业务 affinity | generic显式 key；typed无可信可推导默认 | one named bean lookup/call per logical invocation | Implement only for typed consistent hash |
| Shared Channel pool | Create；Primary §7.0, §10.1 | managers目前各 snapshot eager-create and own channels | raw manager reuse cannot share across queries/modes | entry state/ref/in-flight/drain concurrency | Implement; no new protocol |
| Unified executor | Create；Primary §7.0, §7.3 | current JDK handler owns blocking Gateway-only loop and idempotent gate | separate blocking/async/generic loops would drift | one state machine, cancellation/terminal races | Implement |
| Generic API/cache | Create；Primary §9.2.6–§9.2.7, §10.1 | no string/raw API; dynamic query lifetime could grow unbounded | new Proto envelope or generated interface rejected | 3 public/internal types + bounded closeable cache | Implement raw canonical unary only |
| CGLIB + method plans | Replace；Primary §13 | `Proxy.newProxyInstance` + linear descriptor lookup | keep JDK Proxy violates user decision | one interceptor, one factory; delete duplicates | Implement |
| Guard algorithm Factory | Create；Primary §7.3.1 | Guard only TOKEN_BUCKET in one Local map/one Lua | RPC-local limiter duplicates authority | per-backend 3 Strategy instances, Factory coverage check | Implement in Guard only |
| Provider rate-limit annotation | Reuse；Primary §9.2.3 | `@RateLimitGuard` and most-specific Spring AOP resolver already exist | adding fields to Provider marker duplicates Guard | optional dependency + exception Adapter only | Reuse annotation/AOP; implement Adapter |
| Provider/Consumer state models | Create；Primary §10.6 | Provider/Manager use booleans/partial enums | more booleans cannot express drain/failure ordering | two enums + two coordinators/state transitions | Implement |
| Provider heartbeat scheduler | Reuse/clarify；Primary §10.3 | `RpcProviderLifecycle` already fixed-delay schedules `heartbeatAndRecover` | DDC active probe rejected | add state callbacks/validation, not a second scheduler | Modify existing owner |
| Guard Redis keys | Modify；Primary §11 | token unsuffixed HASH already deployed | one key with changing Redis types risks WRONGTYPE | two safe suffixes + TTL, no migration | Implement lazy state; retain token key |
| Gateway/DDC/Proto/DB/frontend changes | Not applicable/Unchanged；Primary §3, §11, §12, §16 | effective implementations already provide required wire/discovery/security | changing them expands scope without need | high cross-module compatibility cost | No production change |

No fetch-then-forward API is added：generic caller submits a complete invocation command and directly consumes bytes；typed affinity comes from an application resolver and credentials remain framework-owned. No caller-supplied DDC credential, arbitrary Metadata, Provider host, second cache, mapper layer, relational entity, UI state or speculative streaming abstraction enters the Plan。

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| Guard Local algorithms | `REQ-012` | plan/factory/local backend tests | existing Guard backend SPI | algorithm request + three local strategies | Redis strategies/RPC Guard | Step 1 |
| Guard Redis algorithms | `REQ-012`,`REQ-013` | key/Lua/mock/integration tests | Step 1 request/factory | compatible token/leak/window atomic state | Provider distributed rate limit | Step 2 |
| Consumer policy/LB | `REQ-001`,`REQ-008`,`REQ-009`,`REQ-020` | properties/LB tests | existing annotations/properties | expanded enums/properties/LB SPI | fixed-mode strategies/executor | Step 3 |
| Endpoint weights | `REQ-008`,`REQ-009`,`REQ-021` | DDC directory tests | Step 3 algorithms | neutral weighted endpoints | managers/LB | Step 4 |
| Shared Channel ownership | `REQ-011`,`REQ-017` | pool concurrency/drain tests | final endpoint contract | key/lease/pool | executor/lifecycle | Step 5 |
| Reference/cache/lifecycle | `REQ-001`–`REQ-006`,`REQ-011`,`REQ-023` | resolver/manager/coordinator tests | Steps 3–5 | fixed strategy/candidate snapshots/gate | executor/proxy/generic | Step 6 |
| Unified invocation | `REQ-003`,`REQ-004`,`REQ-013`–`REQ-020` | executor/status tests | Step 6 strategy/pool | blocking+async state machine | typed/generic calls | Step 7 |
| Async contract/provider | `REQ-014`,`REQ-015`,`REQ-018`,`REQ-021` | validator/scanner/server tests | Step 7 modes | exact CompletionStage descriptor/binding | CGLIB/test contract | Step 8 |
| Generic API/cache | `REQ-005`,`REQ-009`,`REQ-015`–`REQ-017`,`REQ-020` | generic/cache tests | Steps 6–8 | canonical raw invocation API | AutoConfig/TCP tests | Step 9 |
| CGLIB and consumer wiring | `REQ-001`–`REQ-005`,`REQ-011`,`REQ-014`–`REQ-018`,`REQ-023` | BPP/interceptor/context tests | Steps 6–9 | final typed runtime and beans | Guard/component acceptance | Step 10 |
| Guard Adapter/provider lifecycle | `REQ-007`,`REQ-010`–`REQ-013`,`REQ-020`,`REQ-021` | mapper/AOP/lifecycle/lease tests | Guard Steps, Provider async, AutoConfig | UNAVAILABLE adapter + explicit Provider state | full TCP/process acceptance | Step 11 |
| Cross-module acceptance/docs | `REQ-002`–`REQ-023` | TCP/process/static docs gates | Steps 1–11 | release-ready evidence/documentation | user review/execution approval | Step 12 |

## 5. Change File Tree

```text
egon-cola-components/
├── egon-cola-component-access-guard-starter/
│   ├── README.md                                                        MODIFY [S12]
│   ├── README.zh-CN.md                                                  MODIFY [S12]
│   └── src/
│       ├── main/java/top/egon/cola/component/accessguard/
│       │   ├── core/plan/AdmissionConfig.java                           MODIFY [S1]
│       │   ├── core/plan/GuardPlanValidator.java                        MODIFY [S1]
│       │   ├── policy/ratelimit/RateLimitAlgorithmStrategy.java         CREATE [S1]
│       │   ├── policy/ratelimit/RateLimitAlgorithmStrategyFactory.java  CREATE [S1]
│       │   ├── policy/ratelimit/RateLimitPolicy.java                    MODIFY [S1]
│       │   ├── store/RateLimitRequest.java                              MODIFY [S1]
│       │   ├── store/local/LocalRateLimitBackend.java                   MODIFY [S1]
│       │   └── store/redisson/
│       │       ├── AccessGuardRedisKeyFactory.java                      MODIFY [S2]
│       │       └── RedissonRateLimitBackend.java                        MODIFY [S2]
│       └── test/java/top/egon/cola/component/accessguard/
│           ├── core/plan/GuardPlanValidatorTest.java                    CREATE [S1]
│           ├── policy/ratelimit/RateLimitAlgorithmStrategyFactoryTest.java CREATE [S1]
│           ├── store/local/LocalRateLimitBackendTest.java               MODIFY [S1]
│           └── store/redisson/
│               ├── AccessGuardRedisKeyFactoryTest.java                  MODIFY [S2]
│               ├── RedissonStoreContractTest.java                       MODIFY [S2]
│               └── RedissonStoreIntegrationTest.java                    MODIFY [S2]
├── egon-cola-component-rpc/
│   ├── README.md                                                        MODIFY [S12]
│   ├── README.zh-CN.md                                                  MODIFY [S12]
│   ├── egon-cola-component-rpc-starter/
│   │   ├── pom.xml                                                      MODIFY [S11]
│   │   └── src/
│   │       ├── main/
│   │       │   ├── java/top/egon/cola/component/rpc/
│   │       │   │   ├── annotation/
│   │       │   │   │   ├── EgonRpcReference.java                       MODIFY [S3]
│   │       │   │   │   ├── EgonRpcDirectReference.java                 MODIFY [S3]
│   │       │   │   │   ├── EgonRpcMethod.java                          MODIFY [S8]
│   │       │   │   │   └── LoadBalance.java                            MODIFY [S3]
│   │       │   │   ├── config/
│   │       │   │   │   ├── EgonRpcAutoConfig.java                      MODIFY [S10]
│   │       │   │   │   ├── EgonRpcProperties.java                      MODIFY [S3]
│   │       │   │   │   └── RpcAccessGuardAutoConfiguration.java        CREATE [S11]
│   │       │   │   ├── consumer/
│   │       │   │   │   ├── channel/
│   │       │   │   │   │   ├── RpcEndpoint.java                        MODIFY [S4]
│   │       │   │   │   │   ├── RpcConsumerChannelFactory.java          MODIFY [S5]
│   │       │   │   │   │   ├── RpcConsumerChannelPool.java             CREATE [S5]
│   │       │   │   │   │   ├── RpcChannelKey.java                      CREATE [S5]
│   │       │   │   │   │   └── RpcChannelLease.java                    CREATE [S5]
│   │       │   │   │   ├── gateway/
│   │       │   │   │   │   ├── RpcConsumerGatewayManager.java          MODIFY [S6]
│   │       │   │   │   │   └── RpcGatewayEndpoint.java                 MODIFY [S4]
│   │       │   │   │   ├── provider/
│   │       │   │   │   │   ├── RpcConsumerProviderManager.java         MODIFY [S6]
│   │       │   │   │   │   └── RpcProviderEndpoint.java                MODIFY [S4]
│   │       │   │   │   ├── loadbalance/
│   │       │   │   │   │   ├── package-info.java                       CREATE [S3]
│   │       │   │   │   │   ├── RpcLoadBalancer.java                    CREATE [S3]
│   │       │   │   │   │   ├── RpcLoadBalancers.java                   CREATE [S3]
│   │       │   │   │   │   ├── RpcLoadBalanceContext.java              CREATE [S3]
│   │       │   │   │   │   └── RpcLoadBalanceKeyResolver.java          CREATE [S3]
│   │       │   │   │   ├── reference/
│   │       │   │   │   │   ├── package-info.java                       CREATE [S6]
│   │       │   │   │   │   ├── RpcReferenceMode.java                   CREATE [S6]
│   │       │   │   │   │   ├── RpcReferencePolicy.java                 CREATE [S6]
│   │       │   │   │   │   ├── RpcReferenceDefinition.java             CREATE [S6]
│   │       │   │   │   │   ├── RpcReferenceDefinitionResolver.java     CREATE [S6]
│   │       │   │   │   │   ├── RpcReferenceStrategy.java               CREATE [S6]
│   │       │   │   │   │   ├── RpcReferenceStrategyFactory.java        CREATE [S6]
│   │       │   │   │   │   ├── GatewayRpcReferenceStrategy.java        CREATE [S6]
│   │       │   │   │   │   └── DirectRpcReferenceStrategy.java         CREATE [S6]
│   │       │   │   │   ├── lifecycle/
│   │       │   │   │   │   ├── package-info.java                       CREATE [S6]
│   │       │   │   │   │   ├── RpcConsumerRuntimeState.java            CREATE [S6]
│   │       │   │   │   │   └── RpcConsumerLifecycleCoordinator.java    CREATE [S6]
│   │       │   │   │   ├── invocation/
│   │       │   │   │   │   ├── package-info.java                       CREATE [S7]
│   │       │   │   │   │   ├── RpcInvocationMode.java                  CREATE [S7]
│   │       │   │   │   │   ├── RpcInvocationPlan.java                  CREATE [S7]
│   │       │   │   │   │   ├── RpcInvocationContext.java               CREATE [S7]
│   │       │   │   │   │   └── RpcInvocationExecutor.java              CREATE [S7]
│   │       │   │   │   ├── generic/
│   │       │   │   │   │   ├── package-info.java                       CREATE [S9]
│   │       │   │   │   │   ├── RpcGenericInvocation.java               CREATE [S9]
│   │       │   │   │   │   ├── RpcGenericTargetCache.java              CREATE [S9]
│   │       │   │   │   │   └── RpcGenericInvoker.java                  CREATE [S9]
│   │       │   │   │   ├── interceptor/
│   │       │   │   │   │   ├── RpcClientInvocation.java                 MODIFY [S9]
│   │       │   │   │   │   └── RpcConsumerClientInterceptor.java        MODIFY [S9]
│   │       │   │   │   └── proxy/
│   │       │   │   │       ├── EgonRpcReferenceBeanPostProcessor.java   MODIFY [S10]
│   │       │   │   │       ├── RpcConsumerProxyFactory.java             MODIFY [S10]
│   │       │   │   │       ├── RpcConsumerMethodInterceptor.java        CREATE [S10]
│   │       │   │   │       ├── RpcDirectReferenceProxyFactory.java      DELETE [S10]
│   │       │   │   │       └── RpcConsumerInvocationHandler.java        DELETE [S10]
│   │       │   │   ├── contract/
│   │       │   │   │   ├── descriptor/RpcContractDescriptor.java       MODIFY [S8]
│   │       │   │   │   ├── descriptor/RpcMethodDescriptor.java         MODIFY [S8]
│   │       │   │   │   └── validation/RpcContractValidator.java        MODIFY [S8]
│   │       │   │   ├── context/invocation/RpcMetadataKeys.java         MODIFY [S7]
│   │       │   │   ├── exception/
│   │       │   │   │   ├── EgonRpcErrorCode.java                       MODIFY [S7]
│   │       │   │   │   └── RpcStatusExceptionMapper.java               MODIFY [S7]
│   │       │   │   └── provider/
│   │       │   │       ├── binding/RpcProviderBinding.java             MODIFY [S8]
│   │       │   │       ├── binding/RpcProviderMethodBinding.java       MODIFY [S8]
│   │       │   │       ├── binding/RpcProviderBeanScanner.java         MODIFY [S8]
│   │       │   │       ├── lifecycle/RpcProviderLifecycle.java         MODIFY [S11]
│   │       │   │       ├── lifecycle/RpcProviderRuntimeState.java      CREATE [S11]
│   │       │   │       ├── registration/RpcProviderLeaseManager.java   MODIFY [S11]
│   │       │   │       └── server/
│   │       │   │           ├── RpcServerServiceDefinitionFactory.java  MODIFY [S8]
│   │       │   │           └── RpcAccessGuardExceptionMapper.java      CREATE [S11]
│   │       │   └── resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports MODIFY [S11]
│   │       └── test/java/top/egon/cola/component/rpc/
│   │           ├── config/
│   │           │   ├── EgonRpcPropertiesTest.java                      MODIFY [S3]
│   │           │   ├── EgonRpcAutoConfigTest.java                      MODIFY [S10]
│   │           │   └── RpcAccessGuardAutoConfigurationTest.java       CREATE [S11]
│   │           ├── consumer/
│   │           │   ├── channel/RpcConsumerChannelPoolTest.java        CREATE [S5]
│   │           │   ├── gateway/RpcConsumerGatewayManagerTest.java     MODIFY [S6]
│   │           │   ├── provider/RpcConsumerProviderManagerTest.java   MODIFY [S6]
│   │           │   ├── loadbalance/RpcLoadBalancersTest.java          CREATE [S3]
│   │           │   ├── reference/RpcReferenceDefinitionResolverTest.java CREATE [S6]
│   │           │   ├── reference/RpcReferenceStrategyFactoryTest.java CREATE [S6]
│   │           │   ├── lifecycle/RpcConsumerLifecycleCoordinatorTest.java CREATE [S6]
│   │           │   ├── invocation/RpcInvocationExecutorTest.java      CREATE [S7]
│   │           │   ├── generic/RpcGenericInvokerTest.java             CREATE [S9]
│   │           │   ├── generic/RpcGenericTargetCacheTest.java         CREATE [S9]
│   │           │   ├── interceptor/RpcConsumerClientInterceptorTest.java MODIFY [S9]
│   │           │   └── proxy/
│   │           │       ├── EgonRpcReferenceBeanPostProcessorTest.java MODIFY [S10]
│   │           │       ├── RpcConsumerMethodInterceptorTest.java      CREATE [S10]
│   │           │       └── RpcConsumerInvocationHandlerTest.java      DELETE [S10]
│   │           ├── contract/validation/RpcContractValidatorTest.java  MODIFY [S8]
│   │           ├── exception/RpcStatusExceptionMapperTest.java        MODIFY [S7]
│   │           └── provider/
│   │               ├── binding/RpcProviderBeanScannerTest.java        MODIFY [S8]
│   │               ├── lifecycle/RpcProviderLifecycleTest.java        MODIFY [S11]
│   │               ├── registration/RpcProviderLeaseManagerTest.java  MODIFY [S11]
│   │               └── server/
│   │                   ├── RpcServerServiceDefinitionFactoryTest.java CREATE [S8]
│   │                   ├── RpcAccessGuardExceptionMapperTest.java      CREATE [S11]
│   │                   └── RpcProviderAccessGuardComponentTest.java    CREATE [S11]
│   ├── egon-cola-component-rpc-ddc-adapter/src/
│   │   ├── main/java/top/egon/cola/component/rpc/ddc/registry/
│   │   │   ├── DdcRpcProviderDirectory.java                           MODIFY [S4]
│   │   │   └── DdcRpcGatewayDirectory.java                            MODIFY [S4]
│   │   └── test/java/top/egon/cola/component/rpc/ddc/registry/
│   │       ├── DdcRpcProviderDirectoryTest.java                       MODIFY [S4]
│   │       └── DdcRpcGatewayDirectoryTest.java                        MODIFY [S4]
│   └── egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract/src/
│       ├── main/java/top/egon/cola/component/rpc/test/contract/AsyncEchoRpc.java CREATE [S12]
│       └── test/java/top/egon/cola/component/rpc/test/
│           ├── contract/EchoGeneratedContractTest.java                MODIFY [S12]
│           ├── fixture/consumer/AsyncEchoRpcTestClient.java           CREATE [S12]
│           ├── fixture/directconsumer/DirectEchoRpcTestClient.java     CREATE [S12]
│           ├── fixture/directconsumer/RpcDirectTestConsumerApplication.java CREATE [S12]
│           ├── fixture/provider/AsyncEchoRpcTestProvider.java         CREATE [S12]
│           ├── mockgateway/RpcRuntimeGovernanceTcpTest.java           CREATE [S12]
│           └── process/RpcProcessIT.java                              MODIFY [S12]
```

Inventory ownership is exactly the `[S1]`–`[S12]` marker shown above. The only semantic-preserving addition to the Primary target tree is `GuardPlanValidator.java` and its test because current repository validation lives there; no DDC subscription implementation file is added.

| Operation group | Exact paths represented above | Current evidence/final responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- |
| Guard Local CREATE/MODIFY | `accessguard/core/plan/*`, `policy/ratelimit/*`, `store/RateLimitRequest.java`, `store/local/LocalRateLimitBackend.java` and matching three tests | TOKEN_BUCKET-only enum/map → three Strategy maps with exact validation | Step 1 | `REQ-012` | Guard focused tests |
| Guard Redisson MODIFY | exact two `store/redisson` production files and three named tests | one HASH Lua/key → compatible token plus suffixed leak/window atomic state | Step 2 | `REQ-012`,`REQ-013` | mock script + optional Redis IT |
| RPC policy/LB CREATE/MODIFY | exact annotations/properties/loadbalance files and two tests | unwired annotation fields/hardcoded RR → validated five-algorithm factory and hash SPI | Step 3 | `REQ-001`,`REQ-008`,`REQ-009`,`REQ-020` | properties/LB tests |
| Endpoint/DDC MODIFY | exact endpoint interface/records, two DDC directories and two tests | decoded weight ignored → canonical weight propagated | Step 4 | `REQ-008`,`REQ-009`,`REQ-021` | DDC adapter tests |
| Channel CREATE/MODIFY | exact five channel files and pool test | per-manager channels → shared key/lease/pool | Step 5 | `REQ-011`,`REQ-017` | pool test |
| Reference/cache/lifecycle CREATE/MODIFY | exact nine reference, two manager, three lifecycle and five test files | annotation-specific runtime → fixed strategy and explicit Consumer gate | Step 6 | `REQ-001`–`REQ-006`,`REQ-011`,`REQ-023` | reference/manager/lifecycle tests |
| Invocation CREATE/MODIFY | exact five invocation, metadata/error and two tests | handler-owned loop → unified blocking/async state machine | Step 7 | `REQ-003`,`REQ-004`,`REQ-013`–`REQ-020` | executor/status tests |
| Async Contract/Provider MODIFY | exact annotation/descriptors/validator/binding/server and three tests | Message-only return → exact CompletionStage<Message> parity | Step 8 | `REQ-014`,`REQ-015`,`REQ-018`,`REQ-021` | contract/server tests |
| Generic CREATE/MODIFY | exact four generic files, two interceptor files and three tests | no dynamic API/Message-only interceptor context → bounded canonical raw unary API with the same framework metadata/security path | Step 9 | `REQ-005`,`REQ-009`,`REQ-015`–`REQ-017`,`REQ-020`,`REQ-021` | generic/interceptor tests |
| CGLIB/wiring CREATE/MODIFY/DELETE | exact proxy, AutoConfig and four test files | two JDK factories/handler → one CGLIB plan interceptor and runtime graph | Step 10 | `REQ-001`–`REQ-005`,`REQ-011`,`REQ-014`–`REQ-018`,`REQ-023` | proxy/context tests |
| Guard Adapter/lifecycle CREATE/MODIFY | exact POM, Guard AutoConfig/imports/mapper, Provider lifecycle/state/lease and six tests | no optional Guard mapping/boolean lifecycle → conditional UNAVAILABLE adapter and explicit state | Step 11 | `REQ-007`,`REQ-010`–`REQ-013`,`REQ-020`,`REQ-021` | component/lifecycle tests |
| Acceptance/docs CREATE/MODIFY | exact four README, Async Echo fixtures, TCP test and Process IT | old docs/blocking fixture → complete compatibility/acceptance surface | Step 12 | `REQ-002`–`REQ-023` | static/TCP/live gates |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- Baseline is `main@0aa6673d7af572c0a13618716e9a2c76f8d33863`; branch is three commits ahead of `origin/main`.
- Applicable instructions are the user-provided Main Agent/AGENTS rules plus the named Plan skill; repository search found no additional `AGENTS.md`.
- Existing staged deletion `egon-cola-platforms/.../GatewayContractVersions.java`, untracked `0`, and unrelated Spec/Plan files are user-owned and must remain untouched.
- Every implementation Step begins with `git status --short --branch` and commits only its exact `Commit paths`; never use broad `git add .`, reset, checkout, or modify old Flyway/generated Protobuf Java.
- Generated `target/` output from Maven is ignored build state, not a commit artifact. No application/service is started automatically.

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| Toolchain | `./mvnw -version` from repository root | Maven 3.9.14, Java 21.0.10 (observed 2026-08-21) | build tool only |
| Focused reactor selector | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-access-guard-starter,:egon-cola-component-rpc-starter,:egon-cola-component-rpc-ddc-adapter -am -Dtest=LocalRateLimitBackendTest,RpcConsumerInvocationHandlerTest,DdcRpcProviderDirectoryTest -Dsurefire.failIfNoSpecifiedTests=false test` | observed exit 0; 3 named tests, 9 reactor modules | baseline module proof, not live Redis/DDC/gRPC topology |
| Guard module | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-access-guard-starter -am test` | exit 0 | Local/mock Guard regression |
| Optional Redis IT | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-access-guard-starter -am -Degon.access.guard.redis.it=true -Dtest=RedissonStoreIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` | Docker available and exit 0; otherwise explicitly deferred, not skipped as pass | real disposable Redis only |
| RPC core/adapters | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-rpc-starter,:egon-cola-component-rpc-ddc-adapter -am test` | exit 0 | module tests; no external DDC |
| RPC test contract | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-rpc-test-contract -am test` | exit 0 | generated Proto + real loopback TCP tests |
| Process IT | `DDC_TEST_REDIS_HOST=127.0.0.1 DDC_TEST_REDIS_PORT=6379 ./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-rpc-test-contract -am -Pddc-live-test -Dit.test=RpcProcessIT verify` | user supplies single Redis and explicitly runs; exit 0 | separate JVM/live DDC boundary |
| Static/docs | `git diff --check` plus scoped `rg` commands in Step 12 | no whitespace errors/contradictory legacy text | repository static proof |

### 6.3 Immutable constraints and approved decisions

- Client configuration/annotation fixes mode to GATEWAY or DIRECT. Failure never switches mode; only a different same-mode instance may be selected.
- Business/semantic errors, including not satisfying business conditions, remain visible terminal errors and never activate retry or availability fail strategy.
- Retry classifier ignores `@EgonRpcMethod.idempotent`; business duplicate safety is a deployment prerequisite, not a framework gate.
- `FAIL_OPEN` applies only after availability exhaustion and returns/completes `null` for typed/generic blocking/async.
- `@EgonRpcProvider` remains a field-free discovery marker. Provider limiting uses existing Guard `@RateLimitGuard` on implementation methods; `RATE_LIMITED` transport status is `UNAVAILABLE`.
- Generic identity is exactly `fully.qualified.Service/Method`, not `fullservice.method`, leading-slash aliases, arbitrary Metadata or a new Proto service.
- DDC event is an invalidation hint; full snapshot/revision/lease is authority. Provider heartbeat is RPC-owned and configured by RPC Provider properties; DDC does not call Provider health probes.
- Token Bucket Redis key remains exact legacy unsuffixed HASH. New algorithm state is lazy/TTL-based; no Flyway, backfill or destructive Redis cleanup.
- Gateway production code, Proto files, IdP credential relay and relational/frontend surfaces are unchanged.

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| `PLAN-CLAR-001` | Add `GuardPlanValidator.java` to Step 1 although Primary target tree only labels `AdmissionConfig` for config validation | current algorithm bounds are implemented in `GuardPlanValidator#validate` | Primary already mandates algorithm-specific startup validation; this only locates existing owner | validation could be moved into the record, but would duplicate current style |
| `PLAN-CLAR-002` | Do not modify DDC subscription/coordinator files | `DdcManagedRegistrySubscription#start/onEvent/safeRefresh` already subscribes before full pull, coalesces events, schedules fixed-delay reconciliation and expires local state | exactly implements `REQ-005/006` reuse decision | if live adapter bypasses this coordinator, implementation must return to Spec/Plan rather than add RPC polling silently |
| `PLAN-CLAR-003` | Retain `consumer.direct` programmatic client API and the existing `RpcConsumerProxyFactory` compatibility constructor | IdP Starter, RPC DDC client and DDC Admin IT instantiate `RpcDirectClientFactory` | preserves existing infrastructure callers while typed annotation path migrates to CGLIB/executor | removing it would expand consumers and require a separate compatibility decision |
| `PLAN-CLAR-004` | `RpcLoadBalancers` contains the enum-keyed factory and nested implementations; no separate `RpcLoadBalancerFactory.java` | Primary target tree explicitly names `RpcLoadBalancers.java (nested concrete strategies)` and Gateway has `ProviderLoadBalancers` precedent | satisfies Strategy+Factory without one more public type | if project style requires a separate factory, update Spec and inventory first |
| `PLAN-CLAR-005` | Provider registration derives default `gateway.weight` from each `RpcProviderBinding.contractType` in `RpcProviderLeaseManager#prepare`; explicit configured metadata wins | `EgonRpcService.weight` exists; `RpcProviderLeaseManager#prepare` receives bindings; `RpcProviderMetadataMerger` currently merges process metadata | implements the documented default/override without widening `RpcServiceIdentity` | if one process exports same identity with divergent weights, existing duplicate-provider validation must reject it |
| `PLAN-CLAR-006` | Async fixture uses a second Java contract bound to the existing Echo unary descriptor only in an isolated test Provider context | one Java interface cannot expose the same wire method twice in one Provider registry; no new `.proto` method is allowed | proves async Java shape without changing wire or creating duplicate production bindings | adding both fixtures to one context must remain a negative duplicate-binding case |
| `PLAN-CLAR-007` | Review Spec authorization permits a Review Plan but not execution | explicit user Plan-skill invocation after decisions; Plan skill status rule | changes workflow status only, not runtime behavior | implementation waits for explicit approval/Accepted status |
| `PLAN-CLAR-008` | Step 9 also modifies `RpcClientInvocation` and `RpcConsumerClientInterceptor` with compatibility constructors/factories for raw calls | current types require `RpcContractDescriptor/RpcMethodDescriptor/Message`, so generic bytes cannot reuse the approved interceptor/metadata path without a synthetic contract | Primary requires generic to use the same framework-owned auth/trace metadata; extending the existing context is smaller than a second interceptor chain | preserve the four-argument constructor and accessor methods; converting the internal record to a final class is allowed only to avoid changing those binary method descriptors |

## 7. Ordered File-by-file Implementation Steps

### Step 1 — Establish three validated Local rate-limit strategies behind the existing Guard backend

- Requirements: `REQ-012`
- Dependencies: None
- Baseline state: Guard supports only `TOKEN_BUCKET`; one `LocalRateLimitBackend` map owns token state; shared positive-value validation has no sliding-window rules.
- Observable outcome: Token Bucket, Leaky Bucket and exact Sliding Window have isolated bounded Local state, deterministic admission/retryAfter semantics and complete Strategy Factory coverage.
- End state: Guard public annotation/backend/failure-policy APIs stay stable; `RateLimitRequest` carries the algorithm and Local backend delegates without a switch-heavy combined state record.
- Test-first gate: Required — new enum/factory tests initially do not compile and Local leaky/window tests fail because the strategies/state do not exist.
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/core/plan/GuardPlanValidatorTest.java`

- Purpose: Lock startup validation for the three algorithms.
- Symbols: `GuardPlanValidatorTest`, `rejectsSlidingWindowCostOtherThanOne`, `rejectsOversizedSlidingWindow`, `acceptsAllSupportedAlgorithms`.
- Repository evidence: `GuardPlanValidator#validate` currently owns rate-limit numeric checks; JUnit/AssertJ is the module convention.
- Dependencies and consumers: `GuardPlanValidator`, `GuardPlanSnapshot` fixtures, new enum values.
- Why now: It is the RED contract before changing the configuration model.
- Contract/signature changes: Sliding Window requires `requestedTokens=1` and `capacity<=100000`; all algorithms require positive capacity/refill/cost/period.
- Input/output and state mapping: algorithm-specific `GuardPlanSnapshot` → `validate` → no result or `IllegalArgumentException`; no store state is created.
- Error and edge behavior: unknown binding is rejected by property binding; zero/negative/overflow-prone values remain fail-fast.
- Implementation pseudocode:

```java
for (RateLimitAlgorithm algorithm : RateLimitAlgorithm.values()) validate(plan(algorithm, validValues()));
assertThatThrownBy(() -> validate(plan(SLIDING_WINDOW, requestedTokens(2)))).hasMessageContaining("requestedTokens=1");
assertThatThrownBy(() -> validate(plan(SLIDING_WINDOW, capacity(100_001)))).hasMessageContaining("100000");
```

- Verification contribution: `-Dtest=GuardPlanValidatorTest` proves startup rule selection and bounds.
- After this file: RED is an expected compile failure for missing enum values, not a fixture/environment failure.

#### File 2 — `CREATE egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/policy/ratelimit/RateLimitAlgorithmStrategyFactoryTest.java`

- Purpose: Prove exact enum coverage and duplicate/missing strategy rejection.
- Symbols: `RateLimitAlgorithmStrategyFactoryTest`, three mapping tests, missing/duplicate tests.
- Repository evidence: Guard uses small final classes and constructor validation; Primary §7.3.1 requires one strategy per enum per backend.
- Dependencies and consumers: `RateLimitAlgorithmStrategy`, Factory, synthetic strategies returning tagged decisions.
- Why now: Factory invariants precede backend delegation.
- Contract/signature changes: Factory constructor accepts strategies; `acquire` dispatches strictly by `request.algorithm()`.
- Input/output and state mapping: list of strategies → immutable enum map → matching strategy result; no fallback to token strategy.
- Error and edge behavior: null request, missing enum, duplicate enum and null strategy fail immediately.
- Implementation pseudocode:

```java
var factory = new RateLimitAlgorithmStrategyFactory(List.of(token(), leaky(), sliding()));
assertThat(factory.acquire(request(LEAKY_BUCKET))).isEqualTo(leakyDecision());
assertThatThrownBy(() -> new RateLimitAlgorithmStrategyFactory(List.of(token(), token()))).isInstanceOf(IllegalArgumentException.class);
```

- Verification contribution: focused factory RED/GREEN and enum-exhaustiveness proof.
- After this file: RED identifies absent Strategy/Factory types.

#### File 3 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/store/local/LocalRateLimitBackendTest.java`

- Purpose: Define token compatibility, leak, exact window, concurrency, reset, cleanup and overflow behavior.
- Symbols: existing token tests plus `leaksByFullPeriods`, `countsSameTimestampCalls`, `resetsOnConfigChange`, `neverOverAdmitsConcurrently`.
- Repository evidence: current test uses deterministic ticker and validates burst/refill/eviction.
- Dependencies and consumers: algorithm-bearing request, Local backend, barriers/executor for concurrency.
- Why now: It is the observable RED for the three Local state machines.
- Contract/signature changes: request helper takes algorithm; token results stay byte-for-byte semantically compatible.
- Input/output and state mapping: monotonic nanos + rule/state/hash/config → admit/reject/remaining/retryAfter and isolated map entry.
- Error and edge behavior: boundary `<= now-window` expires; duplicate nanos count separately; saturated arithmetic never wraps; cleanup sums all maps.
- Implementation pseudocode:

```java
advance(window.minusNanos(1)); assertThat(sliding.acquire(request)).isRejected();
advance(Duration.ofNanos(1)); assertThat(sliding.acquire(request)).isAllowed();
runConcurrently(64, () -> backend.acquire(request(LEAKY_BUCKET))); assertThat(allowedCount).isLessThanOrEqualTo(capacity);
```

- Verification contribution: `-Dtest=LocalRateLimitBackendTest` maps Primary `TEST-039`–`043`,`048` Local portions.
- After this file: tests compile only after enum/request changes and fail until strategies are implemented.

#### File 4 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/plan/AdmissionConfig.java`

- Purpose: Publish the additive algorithm enum contract.
- Symbols: `RateLimitAlgorithm.TOKEN_BUCKET`, `LEAKY_BUCKET`, `SLIDING_WINDOW`.
- Repository evidence: `RateLimitConfig` already owns the enum and preserves TOKEN_BUCKET default through properties.
- Dependencies and consumers: properties binder, plan source/validator, RateLimitPolicy/request, tests.
- Why now: All strategy code compiles against one authority.
- Contract/signature changes: two enum constants only; record fields/defaults remain unchanged.
- Input/output and state mapping: configured string → enum → immutable plan; no state is allocated here.
- Error and edge behavior: unknown value remains Spring binding/startup failure; no silent token fallback.
- Implementation pseudocode:

```java
enum RateLimitAlgorithm { TOKEN_BUCKET, LEAKY_BUCKET, SLIDING_WINDOW }
RateLimitConfig { algorithm = requireNonNull(algorithm); refillPeriod = requireNonNull(refillPeriod); }
// Preserve all existing record components and TOKEN_BUCKET property default.
```

- Verification contribution: unblocks all Step 1 RED tests and property compatibility.
- After this file: configuration model recognizes all required algorithms but does not yet execute them.

#### File 5 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/plan/GuardPlanValidator.java`

- Purpose: Enforce common and Sliding-specific limits before runtime.
- Symbols: `validate(GuardPlanSnapshot)` rate-limit branch.
- Repository evidence: this method already validates positive token values and period.
- Dependencies and consumers: `AdmissionConfig.RateLimitConfig`, resolver/startup validator.
- Why now: The new enum must not admit invalid memory-amplifying window plans.
- Contract/signature changes: no signature change; error messages identify algorithm-specific invalid configuration.
- Input/output and state mapping: compiled plan → common validation → switch for sliding constraints → accepted immutable snapshot.
- Error and edge behavior: validate regardless of `enabled` as current code does; protect duration conversion/large capacity.
- Implementation pseudocode:

```java
validatePositive(rate.capacity(), rate.refillTokens(), rate.requestedTokens(), rate.refillPeriod());
if (rate.algorithm() == SLIDING_WINDOW && rate.requestedTokens() != 1) throw invalid("requestedTokens=1");
if (rate.algorithm() == SLIDING_WINDOW && rate.capacity() > 100_000) throw invalid("capacity <= 100000");
```

- Verification contribution: makes File 1 GREEN and preserves existing validation tests.
- After this file: invalid plans fail before any backend lookup/state creation.

#### File 6 — `CREATE egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/ratelimit/RateLimitAlgorithmStrategy.java`

- Purpose: Define the real per-storage algorithm variation point.
- Symbols: `algorithm()`, `acquire(RateLimitRequest)`, `evictExpired()`, `size()`.
- Repository evidence: Primary §7.3.1 requires each Local/Redisson backend to construct its own three strategies; current backend SPI remains unchanged.
- Dependencies and consumers: algorithm enum, request/decision, Factory, nested Local/Redisson strategies.
- Why now: Separates state machines without adding public storage backends.
- Contract/signature changes: package-internal/public-to-module interface; Guard public API unaffected.
- Input/output and state mapping: one algorithm-tagged request → one atomic strategy decision; maintenance methods aggregate bounded state.
- Error and edge behavior: a strategy must reject mismatched algorithm and never reinterpret another algorithm's state.
- Implementation pseudocode:

```java
RateLimitAlgorithm algorithm();
RateLimitDecision acquire(RateLimitRequest request); // require request.algorithm == algorithm
default int evictExpired() { return 0; } default int size() { return 0; }
```

- Verification contribution: enables the factory and backend delegation tests.
- After this file: storage implementations have a minimal, testable algorithm seam.

#### File 7 — `CREATE egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/ratelimit/RateLimitAlgorithmStrategyFactory.java`

- Purpose: Build an immutable exhaustive enum registry and dispatch requests.
- Symbols: constructor, `acquire`, `evictExpired`, `size`.
- Repository evidence: Guard prefers constructor-validated final collaborators; no plugin container is needed.
- Dependencies and consumers: Strategy interface, enum, Local/Redisson backend constructors.
- Why now: Guarantees missing/duplicate algorithms fail at backend creation, not on first request.
- Contract/signature changes: new internal factory only.
- Input/output and state mapping: iterable → `EnumMap` after exact coverage check; request → mapped strategy; maintenance sums with safe integer handling.
- Error and edge behavior: null/duplicate/missing mappings throw stable `IllegalArgumentException`; no default branch.
- Implementation pseudocode:

```java
for (strategy : strategies) requireAbsentThenPut(strategy.algorithm(), strategy);
if (!mappings.keySet().equals(EnumSet.allOf(RateLimitAlgorithm.class))) throw incompleteCoverage();
return mappings.get(request.algorithm()).acquire(request);
```

- Verification contribution: makes File 2 GREEN and statically forces future enum additions to update both backends.
- After this file: Factory behavior is complete independent of storage.

#### File 8 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/ratelimit/RateLimitPolicy.java`

- Purpose: Carry the selected algorithm from Guard plan to backend.
- Symbols: `evaluate` request construction.
- Repository evidence: current policy is the sole mapper from `RateLimitConfig` to `RateLimitRequest`.
- Dependencies and consumers: unchanged `RateLimitBackend`, expanded request.
- Why now: Storage dispatch must use the compiled plan's authority.
- Contract/signature changes: constructor/backend SPI unchanged; request now receives `config.algorithm()`.
- Input/output and state mapping: Guard context + config → algorithm-bearing normalized request → existing PolicyResult mapping.
- Error and edge behavior: disabled policy still returns pass without touching backend; retryAfter/remaining mapping unchanged.
- Implementation pseudocode:

```java
if (!config.enabled()) return PolicyResult.pass();
var request = new RateLimitRequest(context.ruleId(), context.stateVersion(), context.keyHash(), config.algorithm(), config.capacity(), config.refillTokens(), config.refillPeriod(), config.requestedTokens());
return mapDecision(backend.acquire(request));
```

- Verification contribution: policy/engine regressions prove no RPC-specific behavior enters Guard.
- After this file: runtime requests carry the selected algorithm to either backend.

#### File 9 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/RateLimitRequest.java`

- Purpose: Add the required immutable algorithm discriminator and preserve redaction.
- Symbols: `algorithm` record component, compact constructor, `toString`.
- Repository evidence: current request owns all normalized store inputs and redacts keyHash.
- Dependencies and consumers: policy, Factory/strategies, Local/Redisson tests.
- Why now: Backend factory cannot safely infer algorithm from key/config.
- Contract/signature changes: additive constructor component within internal Guard SPI; all call sites updated in this Step/Step 2.
- Input/output and state mapping: normalized plan values → immutable request; raw key remains only its SHA-256 hash.
- Error and edge behavior: null algorithm fails; common bounds remain; `toString` includes algorithm but never raw/hash value.
- Implementation pseudocode:

```java
algorithm = Objects.requireNonNull(algorithm, "algorithm");
validateIdentityAndPositiveLimits(ruleId, stateVersion, keyHash, capacity, refillTokens, refillPeriod, requestedTokens);
return "RateLimitRequest[algorithm=" + algorithm + ", keyHash=<redacted>, ...]";
```

- Verification contribution: compile gate for all callers and redaction tests.
- After this file: algorithm identity is explicit at the backend boundary.

#### File 10 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/local/LocalRateLimitBackend.java`

- Purpose: Delegate to three isolated bounded Local strategies.
- Symbols: constructor, `acquire`, `evictExpired`, `size`, nested `TokenBucketStrategy`, `LeakyBucketStrategy`, `SlidingWindowStrategy` and state records.
- Repository evidence: current token implementation already uses `ConcurrentHashMap.compute`, ticker, maxEntries and idle TTL.
- Dependencies and consumers: Strategy Factory; no AutoConfig constructor change.
- Why now: This is the minimum GREEN implementation for File 3.
- Contract/signature changes: `RateLimitBackend` and public constructor unchanged.
- Input/output and state mapping: token starts full/refills; leak starts empty/leaks full periods; window deque removes timestamps `<= now-window`; each map has same key and independent config reset.
- Error and edge behavior: shared maxEntries applies across strategies; safe multiply/add; concurrent same-key acquire linearizes; eviction decrements one shared count exactly once.
- Implementation pseudocode:

```java
factory = new Factory(List.of(new TokenBucketStrategy(ticker, capacity), new LeakyBucketStrategy(ticker, capacity), new SlidingWindowStrategy(ticker, capacity)));
RateLimitDecision acquire(request) { return factory.acquire(request); }
int evictExpired() { return factory.evictExpired(); } int size() { return sharedEntryCount.get(); }
```

- Verification contribution: File 3 GREEN plus existing Guard engine/store regressions.
- After this file: all three Local algorithms satisfy deterministic, bounded, atomic semantics.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-access-guard-starter -am -Dtest=GuardPlanValidatorTest,RateLimitAlgorithmStrategyFactoryTest,LocalRateLimitBackendTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0; named tests pass; existing TOKEN_BUCKET cases unchanged.
- Failure returns to: File 4/5 for plan validation, File 7 for coverage, File 10 for state/concurrency; enum/API disagreement returns to Primary Spec.
- Completion criteria: all three algorithms pass boundary/concurrency/cleanup tests, token compatibility is retained and module compiles without RPC changes.
- Rollback: path-limited revert of Step 1 restores TOKEN_BUCKET-only request/backend; no persisted Local state survives process restart.
- Commit paths: egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/core/plan/GuardPlanValidatorTest.java egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/policy/ratelimit/RateLimitAlgorithmStrategyFactoryTest.java egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/store/local/LocalRateLimitBackendTest.java egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/plan/AdmissionConfig.java egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/plan/GuardPlanValidator.java egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/ratelimit/RateLimitAlgorithmStrategy.java egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/ratelimit/RateLimitAlgorithmStrategyFactory.java egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/ratelimit/RateLimitPolicy.java egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/RateLimitRequest.java egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/local/LocalRateLimitBackend.java
- Commit: `feat(access-guard): add local rate limit strategies`

### Step 2 — Add compatible Redisson leaky-bucket and sliding-window atomic state

- Requirements: `REQ-012`, `REQ-013`
- Dependencies: Step 1
- Baseline state: Token Bucket uses the deployed unsuffixed HASH/Lua and expanded request/factory types now exist; no other Redis algorithm keys or scripts exist.
- Observable outcome: Redisson uses Redis TIME and one-key atomic scripts for all algorithms, preserves the exact token key/state, isolates leak/window types with safe suffixes and resets incompatible config atomically.
- End state: no migration/backfill/delete is required; Local/Redisson decisions are contract-equivalent and old algorithm keys expire by TTL.
- Test-first gate: Required — key/script tests fail because suffix methods and per-algorithm Lua dispatch are absent; optional IT initially fails for non-token requests.
- Ordered files:

#### File 1 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/store/redisson/AccessGuardRedisKeyFactoryTest.java`

- Purpose: Lock exact legacy and new namespaced key shapes.
- Symbols: existing rate key test plus `keepsTokenBucketKeyUnchanged`, `addsSafeLeakySuffix`, `addsSafeSlidingSuffix`.
- Repository evidence: current test validates safe segments/hash redaction; key factory is single authority.
- Dependencies and consumers: expanded key factory and algorithm enum.
- Why now: Key compatibility must be RED before script code.
- Contract/signature changes: algorithm-aware overload/method returns base for token, suffixes for other algorithms.
- Input/output and state mapping: rule/state/hash/algorithm → one exact Redis key; raw business key never participates.
- Error and edge behavior: unsafe segments/hash fail; caller cannot provide suffix; no double suffix.
- Implementation pseudocode:

```java
assertThat(factory.rateLimit("draw", "v1", HASH, TOKEN_BUCKET)).isEqualTo(legacyBase);
assertThat(factory.rateLimit("draw", "v1", HASH, LEAKY_BUCKET)).isEqualTo(legacyBase + ":leaky-bucket");
assertThat(factory.rateLimit("draw", "v1", HASH, SLIDING_WINDOW)).isEqualTo(legacyBase + ":sliding-window");
```

- Verification contribution: Primary `TEST-074` and rollback compatibility.
- After this file: RED isolates key-shape absence.

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/store/redisson/RedissonStoreContractTest.java`

- Purpose: Assert script selection, Redis TIME, data type operations, config reset, return tuple and wrapped failures.
- Symbols: token test retained; new leaky/window script contract tests; transition/wrong-response tests.
- Repository evidence: existing Mockito RScript captor verifies Lua primitives and response mapping.
- Dependencies and consumers: algorithm request helper, Redisson backend, key factory.
- Why now: It defines RED without requiring Docker.
- Contract/signature changes: helper supplies algorithm; scripts remain private implementation but their atomic command contract is observable.
- Input/output and state mapping: request → captured key/script/ARGV → `{allowed,remaining,retryAfter}` decision.
- Error and edge behavior: every script uses one key/Redis TIME/PEXPIRE; list avoids full LRANGE; invalid response/store failure remains `StoreOperationException`.
- Implementation pseudocode:

```java
backend.acquire(request(LEAKY_BUCKET)); verify(lua).contains("TIME", "HMGET", "HSET", "PEXPIRE");
backend.acquire(request(SLIDING_WINDOW)); verify(lua).contains("LINDEX", "LPOP", "LLEN", "RPUSH", "PEXPIRE");
assertThatThrownBy(() -> brokenScript.acquire(request)).isInstanceOf(StoreOperationException.class);
```

- Verification contribution: deterministic Redis contract for Primary `TEST-073`,`075` without live state.
- After this file: RED points to absent algorithm dispatch/Lua.

#### File 3 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/store/redisson/RedissonStoreIntegrationTest.java`

- Purpose: Prove real Redis atomic parity and concurrency for all algorithms when explicitly enabled.
- Symbols: parameterized/shared-client token/leak/window tests, config/algorithm transition test.
- Repository evidence: test already uses opt-in system property, Testcontainers Redis 7.4 and two clients.
- Dependencies and consumers: Docker availability, Redisson backend/key factory, Local decisions for parity.
- Why now: Lua syntax/type/concurrency needs real Redis evidence separate from default module gate.
- Contract/signature changes: test-only helpers accept algorithm/config; production API unchanged.
- Input/output and state mapping: two clients/same key → shared atomic quota; algorithm switch → distinct keys; config mismatch → reset.
- Error and edge behavior: same-millisecond window calls count separately; no WRONGTYPE/cross-key script; TTL present; outage still follows existing failure policy path.
- Implementation pseudocode:

```java
for (algorithm : values()) runTwoClientBarrier(() -> backend.acquire(request(algorithm)));
assertThat(totalAllowed).isLessThanOrEqualTo(capacity); assertThat(redisKeyTtl).isPositive();
switchAlgorithmSameStateVersion(); assertThat(noWrongTypeAndIndependentQuota()).isTrue();
```

- Verification contribution: optional real Redis proof for Primary `TEST-073`,`075`; not counted if Docker/profile is absent.
- After this file: opt-in test is RED until scripts are implemented.

#### File 4 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/redisson/AccessGuardRedisKeyFactory.java`

- Purpose: Centralize algorithm-safe rate-limit key derivation.
- Symbols: existing `rateLimit` retained; additive algorithm overload/helper.
- Repository evidence: current factory validates every segment and SHA-256 hash and emits the deployed base.
- Dependencies and consumers: Redisson backend only; no RPC dependency.
- Why now: Script implementation must not concatenate caller-controlled suffixes.
- Contract/signature changes: legacy three-argument method continues returning token base; algorithm-aware method selects static suffix.
- Input/output and state mapping: validated base + enum → exact final key; token state remains discoverable by old artifact.
- Error and edge behavior: null algorithm rejected; `switch` exhaustive; only static suffixes allowed.
- Implementation pseudocode:

```java
String base = rateLimit(ruleId, stateVersion, keyHash);
return switch (algorithm) { case TOKEN_BUCKET -> base; case LEAKY_BUCKET -> base + ":leaky-bucket"; case SLIDING_WINDOW -> base + ":sliding-window"; };
// Keep existing segment/hash validation and legacy method unchanged.
```

- Verification contribution: File 1 GREEN and exact rollback key proof.
- After this file: all algorithm keys are safe and deterministic.

#### File 5 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/redisson/RedissonRateLimitBackend.java`

- Purpose: Delegate to three nested Redis strategies with atomic one-key Lua.
- Symbols: constructor, `acquire`, nested token/leaky/sliding strategies and scripts.
- Repository evidence: current token Lua already uses Redis TIME, config fields, PEXPIRE and wrapped errors.
- Dependencies and consumers: Step 1 Factory/interface/request, algorithm-aware key factory, RedissonClient.
- Why now: Minimum GREEN for Files 2–3.
- Contract/signature changes: public constructor and `RateLimitBackend` unchanged.
- Input/output and state mapping: token HASH unchanged; leak HASH stores level/lastLeak/config; window LIST stores `v1|capacity|window|timestamp`, pops expired, resets on oldest config mismatch.
- Error and edge behavior: all arithmetic bounded by validated config; same timestamp entries remain distinct; response tuple exactly three longs; runtime exceptions wrap consistently.
- Implementation pseudocode:

```java
factory = new Factory(List.of(new TokenScript(client,keyFactory,ttl), new LeakyScript(...), new SlidingWindowScript(...)));
RateLimitDecision acquire(request) { try { return factory.acquire(request); } catch (StoreOperationException e) { throw e; } catch (RuntimeException e) { throw wrapped(e); } }
// Each strategy calls one READ_WRITE script with List.of(keyFactory.rateLimit(..., algorithm)).
```

- Verification contribution: mock contracts and optional Redis IT GREEN; token script regression stays exact.
- After this file: distributed Guard rate limiting supports all required algorithms without key migration.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-access-guard-starter -am -Dtest=AccessGuardRedisKeyFactoryTest,RedissonStoreContractTest -Dsurefire.failIfNoSpecifiedTests=false test`; when Docker is explicitly available also run `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-access-guard-starter -am -Degon.access.guard.redis.it=true -Dtest=RedissonStoreIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`.
- Expected result: default command exit 0; optional command exit 0 with all three real Redis algorithms and no skipped tests.
- Failure returns to: File 4 for key mismatch; File 5 for Lua/response/atomicity; Step 1 if request/factory semantics conflict.
- Completion criteria: token legacy key/script works, leak/window use correct types/suffixes/TTL, mock contracts pass, optional live proof is either explicitly passed or reported deferred.
- Rollback: change rules to TOKEN_BUCKET before old artifact deploy; suffixed keys expire naturally; never delete shared Redis keys during rollback.
- Commit paths: egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/store/redisson/AccessGuardRedisKeyFactoryTest.java egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/store/redisson/RedissonStoreContractTest.java egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/store/redisson/RedissonStoreIntegrationTest.java egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/redisson/AccessGuardRedisKeyFactory.java egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/redisson/RedissonRateLimitBackend.java
- Commit: `feat(access-guard): add distributed rate limit strategies`

### Step 3 — Publish validated RPC reference policy fields and load-balancing strategies

- Requirements: `REQ-001`, `REQ-008`, `REQ-009`, `REQ-020`
- Dependencies: None
- Baseline state: Gateway annotation already has most policy fields, Direct has only target/group/version/timeout; enum lacks weighted-random/hash; managers hardcode Round Robin.
- Observable outcome: both annotations expose identical common fields, configuration resolves bounded defaults, and all required algorithms plus least-in-flight are independently selectable/testable without creating Channels.
- End state: annotation/config/LB contracts are available but not yet wired into Reference injection; consistent-hash raw keys are validated/hashed outside logs.
- Test-first gate: Required — properties tests fail for missing fields/bounds and the new LB test does not compile until enum/SPI/factory/context/resolver exist.
- Ordered files:

#### File 1 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/config/EgonRpcPropertiesTest.java`

- Purpose: Lock defaults and bounds for retry/LB/hash/generic cache while retaining old gateway attempts.
- Symbols: new consumer property assertions and invalid-value tests.
- Repository evidence: current test directly validates timeout/TLS/consumer settings; properties use explicit `validateSharedSettings`.
- Dependencies and consumers: `EgonRpcProperties.Consumer`, LoadBalance enum.
- Why now: Configuration is the first RED contract for all subsequent runtime wiring.
- Contract/signature changes: defaults maxRetries=3, ROUND_ROBIN, vnodes=160, generic max=256, idle=600000ms; documented ranges enforced.
- Input/output and state mapping: setters/bound values → validate → normalized runtime configuration.
- Error and edge behavior: negative/too-large retry, INHERIT default algorithm, out-of-range vnode/cache/idle fail with `RPC_INVALID_CONTRACT`.
- Implementation pseudocode:

```java
assertThat(new Consumer().getMaxRetries()).isEqualTo(3); assertThat(defaultLoadBalance).isEqualTo(ROUND_ROBIN);
setConsistentHashVirtualNodes(15); assertThatThrownBy(consumer::validateSharedSettings).hasMessageContaining("virtual nodes");
setGenericCacheMaxEntries(4097); assertThatThrownBy(consumer::validateSharedSettings).isInstanceOf(EgonRpcException.class);
```

- Verification contribution: configuration RED/GREEN and compatibility for `gatewayMaxAttempts=2`.
- After this file: failures are caused by missing properties/validation.

#### File 2 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/loadbalance/RpcLoadBalancersTest.java`

- Purpose: Define deterministic/statistical selection, state cleanup, exclusion and key safety for every algorithm.
- Symbols: `RpcLoadBalancersTest` tests mapped to Primary `TEST-016`–`025`.
- Repository evidence: Gateway `ProviderLoadBalancers` tests provide factory/state precedent; RPC uses AssertJ/JUnit/Mockito.
- Dependencies and consumers: neutral endpoint fixtures, deterministic random seam/ticker, context/hash resolver.
- Why now: Algorithms must be correct before Manager/executor wiring.
- Contract/signature changes: factory resolves every non-INHERIT enum; `select` returns endpoint only and never opens transport.
- Input/output and state mapping: stable candidates/weights/digest/excluded/query identity → selected endpoint; per-query RR/SWRR/LIF state retained and cleaned.
- Error and edge behavior: empty/all-excluded, invalid weight/key, overflow sequence, membership changes and one-ring traversal are asserted.
- Implementation pseudocode:

```java
assertCycle(loadBalancers.create(ROUND_ROBIN), endpoints(3), expectedStableOrder());
assertDistribution(loadBalancers.seeded(WEIGHTED_RANDOM), weights(1,3,6), tolerance(0.03));
assertMinimalRemap(loadBalancers.create(CONSISTENT_HASH), fixedKeys(10_000), beforeMembers, afterMembers);
```

- Verification contribution: complete LB behavior and enum coverage before network integration.
- After this file: RED is missing package/types/enum constants, then behavioral failures.

#### File 3 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/annotation/EgonRpcReference.java`

- Purpose: Add the typed consistent-hash resolver reference while preserving all existing elements/defaults.
- Symbols: `loadBalanceKeyResolver()`.
- Repository evidence: Gateway annotation already carries timeout/retries/LB/group/version/fallback/fail strategy.
- Dependencies and consumers: Definition resolver/plan; named Spring bean resolved later.
- Why now: Common policy contract must exist before resolver models.
- Contract/signature changes: additive `String loadBalanceKeyResolver() default ""` only.
- Input/output and state mapping: annotation bean name → policy; empty allowed unless effective algorithm is CONSISTENT_HASH.
- Error and edge behavior: no bean lookup here; validation belongs to resolver; old compiled source sees default.
- Implementation pseudocode:

```java
/** Named RpcLoadBalanceKeyResolver used only when effective policy is CONSISTENT_HASH. */
String loadBalanceKeyResolver() default "";
// Keep all old element names, types and defaults unchanged for source compatibility.
```

- Verification contribution: reflection/definition tests in Step 6 and README compatibility in Step 12.
- After this file: Gateway declaration can express typed affinity ownership.

#### File 4 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/annotation/EgonRpcDirectReference.java`

- Purpose: Add the same retries/LB/fallback/fail/hash fields as Gateway Reference.
- Symbols: `retries`, `loadBalance`, `fallbackBean`, `failStrategy`, `loadBalanceKeyResolver`.
- Repository evidence: Direct already owns only deployment target plus group/version/timeout; annotations cannot inherit elements.
- Dependencies and consumers: reference resolver/definition.
- Why now: Eliminates duplicate semantics without a third public annotation.
- Contract/signature changes: additive elements with exactly Gateway defaults; no mode/fallback-to-gateway field.
- Input/output and state mapping: target-specific identity plus flat common fields → later immutable definition.
- Error and edge behavior: invalid combinations deferred to resolver; mode remains inherently DIRECT.
- Implementation pseudocode:

```java
int retries() default -1; LoadBalance loadBalance() default INHERIT;
String fallbackBean() default ""; FailStrategy failStrategy() default INHERIT;
String loadBalanceKeyResolver() default ""; // never denotes another transport mode
```

- Verification contribution: common-policy parity tests in Step 6.
- After this file: Direct and Gateway expose the same call-site policy vocabulary.

#### File 5 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/annotation/LoadBalance.java`

- Purpose: Add required weighted-random and consistent-hash constants.
- Symbols: `WEIGHTED_RANDOM`, `CONSISTENT_HASH`; retain `SMOOTH_WEIGHTED_ROUND_ROBIN`, `LEAST_IN_FLIGHT`, `INHERIT`.
- Repository evidence: enum names map by name and existing constants are published API.
- Dependencies and consumers: properties, reference/service annotations, LB registry.
- Why now: Factory tests require one authoritative enum.
- Contract/signature changes: additive enum constants; no removal/rename/reorder contract relied upon.
- Input/output and state mapping: declaration string → enum → factory strategy.
- Error and edge behavior: `INHERIT` must be rejected after policy resolution; unsupported future constant fails factory coverage tests.
- Implementation pseudocode:

```java
enum LoadBalance { INHERIT, ROUND_ROBIN, SMOOTH_WEIGHTED_ROUND_ROBIN, RANDOM,
    WEIGHTED_RANDOM, CONSISTENT_HASH, LEAST_IN_FLIGHT }
// Do not alias weighted RR to weighted random; each maps to a distinct strategy.
```

- Verification contribution: enum loop in File 2 proves complete factory mapping.
- After this file: all required algorithms are representable.

#### File 6 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/config/EgonRpcProperties.java`

- Purpose: Define bounded Consumer defaults and compatibility resolution inputs.
- Symbols: new fields/getters/setters and expanded `validateSharedSettings`.
- Repository evidence: current Consumer owns default timeout, drain timeout and `gatewayMaxAttempts=2` compatibility cap.
- Dependencies and consumers: resolver, LB registry, generic cache, AutoConfig.
- Why now: Runtime models must not invent defaults independently.
- Contract/signature changes: additive properties named by Primary §10.3; existing getters/defaults unchanged.
- Input/output and state mapping: Spring configuration → validated Consumer object → resolver/cache/LB constructor arguments.
- Error and edge behavior: exact numeric ranges; `defaultLoadBalance` cannot null/INHERIT; `gatewayMaxAttempts` remains 1..10.
- Implementation pseudocode:

```java
private int maxRetries=3, consistentHashVirtualNodes=160, genericCacheMaxEntries=256;
private LoadBalance defaultLoadBalance=ROUND_ROBIN; private long genericCacheIdleTimeoutMs=600_000;
validateRangesAndReject(defaultLoadBalance == null || defaultLoadBalance == INHERIT);
```

- Verification contribution: File 1 GREEN; provides stable inputs for Steps 6/9/10.
- After this file: all new consumer configuration is explicit and bounded.

#### File 7 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/loadbalance/package-info.java`

- Purpose: Record the neutral selection-only package boundary.
- Symbols: package documentation.
- Repository evidence: new stable packages in the repository use bilingual `package-info` and declare exclusions.
- Dependencies and consumers: LB types in the same package.
- Why now: Prevents selection code from absorbing discovery/channel lifecycle.
- Contract/signature changes: none.
- Input/output and state mapping: documentation states snapshot/context input and endpoint output, with no I/O ownership.
- Error and edge behavior: explicitly excludes DDC, ManagedChannel creation and mode fallback.
- Implementation pseudocode:

```java
/** Registry-neutral endpoint selection strategies and their bounded per-query state.
 * This package neither discovers services nor creates/closes transport channels.
 * All candidates are immutable snapshots supplied by fixed-mode reference strategies. */
```

- Verification contribution: static package review and architecture boundary.
- After this file: package intent is reviewable before implementations.

#### File 8 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/loadbalance/RpcLoadBalancer.java`

- Purpose: Define selection and state-release contract.
- Symbols: `select(RpcLoadBalanceContext)`, optional `removeQuery/close` semantics.
- Repository evidence: Primary requires algorithms return candidates only; Gateway precedent has a small strategy interface.
- Dependencies and consumers: `RpcLoadBalanceContext`, `RpcEndpoint`.
- Why now: Tests and registry need a minimal real variation point.
- Contract/signature changes: new internal SPI, not a network API.
- Input/output and state mapping: context → one candidate; state keyed by exact service query identity.
- Error and edge behavior: empty/all excluded throws mode-neutral selection exception; no fallback or channel side effect.
- Implementation pseudocode:

```java
RpcEndpoint select(RpcLoadBalanceContext context);
default void remove(String queryIdentity) { /* stateless algorithms do nothing */ }
default void close() { /* stateful registries release query state */ }
```

- Verification contribution: File 2 compiles and treats all algorithms uniformly.
- After this file: algorithm implementations share one focused contract.

#### File 9 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/loadbalance/RpcLoadBalanceContext.java`

- Purpose: Carry sanitized, read-only selection inputs.
- Symbols: record fields for query/service/method/request-or-bytes digest/candidates/excluded/affinity digest/revision.
- Repository evidence: Primary §9.2.8 forbids credentials and raw hash key exposure.
- Dependencies and consumers: executor, typed resolver, generic invoker, LB strategies.
- Why now: Prevents algorithms from reaching proxy/DDC/security internals.
- Contract/signature changes: new internal/SPI context; candidate list defensive copy.
- Input/output and state mapping: invocation plan/candidate snapshot → immutable selection view.
- Error and edge behavior: nonblank identities, nonempty candidates, defensive bytes, no Authorization/raw affinity logging.
- Implementation pseudocode:

```java
record RpcLoadBalanceContext(String queryIdentity, String serviceName, String fullMethodName,
        Object request, List<? extends RpcEndpoint> candidates, Set<String> excluded, byte[] affinityDigest, long revision) {
    candidates=List.copyOf(candidates); excluded=Set.copyOf(excluded); affinityDigest=copyOrNull(affinityDigest); }
```

- Verification contribution: selection tests prove exclusion and redaction inputs.
- After this file: algorithm input is immutable and credential-free.

#### File 10 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/loadbalance/RpcLoadBalanceKeyResolver.java`

- Purpose: Expose the typed business affinity SPI.
- Symbols: `String resolve(RpcLoadBalanceContext context)`.
- Repository evidence: whole request/invocation ID are rejected alternatives; named Spring bean ownership is approved.
- Dependencies and consumers: business applications implement; reference resolver resolves named bean once.
- Why now: Consistent hash must have a trusted typed key source.
- Contract/signature changes: additive Java SPI; resolver must be thread-safe/deterministic.
- Input/output and state mapping: sanitized context → 1–512 UTF-8-byte nonblank key; executor hashes then discards raw value.
- Error and edge behavior: throw/null/blank/oversized becomes nonretryable `RPC_INVALID_REQUEST`; no credentials in context.
- Implementation pseudocode:

```java
@FunctionalInterface public interface RpcLoadBalanceKeyResolver {
    String resolve(RpcLoadBalanceContext context); // evaluated once per logical invocation
    // Implementations must be deterministic, thread-safe and must not log the returned raw key.
}
```

- Verification contribution: resolver validation/single-evaluation tests in Steps 6–7.
- After this file: typed consistent hash has an explicit business-owned seam.

#### File 11 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/loadbalance/RpcLoadBalancers.java`

- Purpose: Implement the enum-keyed Factory and six concrete strategies in one bounded file.
- Symbols: `create`, nested random/weighted/RR/SWRR/hash/LIF strategies, state cleanup.
- Repository evidence: target Spec explicitly selects nested implementations; Gateway `ProviderLoadBalancers` is the style precedent.
- Dependencies and consumers: properties vnode count, context/endpoint weight, SHA-256/JDK concurrency primitives.
- Why now: Minimum GREEN for File 2 without class explosion.
- Contract/signature changes: new factory; deterministic seams package-private for tests.
- Input/output and state mapping: filtered stable candidates → algorithm selection; RR/SWRR/LIF state per query; hash ring per query/revision/membership.
- Error and edge behavior: overflow-safe totals/floorMod, invalid weights excluded/defaulted before call, ring wraps once, raw key never logged.
- Implementation pseudocode:

```java
RpcLoadBalancer create(LoadBalance type) { return requireNonInherit(mappings.get(type)); }
List<RpcEndpoint> eligible = stableSort(context.candidates()).filter(notExcludedAndPositiveWeight);
return strategy(type).select(eligible, queryState(context.queryIdentity()), context.affinityDigest());
```

- Verification contribution: all Primary LB tests GREEN and future enum coverage fail-fast.
- After this file: selection algorithms are complete and transport-neutral.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-rpc-starter -am -Dtest=EgonRpcPropertiesTest,RpcLoadBalancersTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0; property boundary and all algorithm tests pass; no Channel/DDC interaction occurs.
- Failure returns to: Files 3–6 for public/default contracts; Files 8–11 for selection/state; missing affinity semantics returns to Primary Spec.
- Completion criteria: required algorithms + least-in-flight are exhaustive, deterministic/statistically bounded, exclusion-safe, and common Direct fields/defaults are source-compatible.
- Rollback: revert Step 3 paths; no runtime/persistent state migration exists.
- Commit paths: egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/config/EgonRpcPropertiesTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/loadbalance/RpcLoadBalancersTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/annotation/EgonRpcReference.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/annotation/EgonRpcDirectReference.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/annotation/LoadBalance.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/config/EgonRpcProperties.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/loadbalance/package-info.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/loadbalance/RpcLoadBalancer.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/loadbalance/RpcLoadBalanceContext.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/loadbalance/RpcLoadBalanceKeyResolver.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/loadbalance/RpcLoadBalancers.java
- Commit: `feat(rpc): add reference policy load balancing contracts`

### Step 4 — Project canonical DDC weights into neutral RPC endpoints

- Requirements: `REQ-008`, `REQ-009`, `REQ-021`
- Dependencies: Step 3
- Baseline state: `ServiceInstanceMetaCodec.decode` is called then discarded; endpoint records have no weight; `RpcEndpoint` exposes only host/port/secure.
- Observable outcome: Provider and Gateway snapshots carry validated `gateway.weight`, with missing/invalid remote values falling back to 100 and custom endpoint implementations remaining source compatible.
- End state: no DDC wire/key/subscription changes; LB receives the same weight semantics in both modes.
- Test-first gate: Required — DDC directory tests fail because endpoint accessors/default contract do not expose decoded weight.
- Ordered files:

#### File 1 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter/src/test/java/top/egon/cola/component/rpc/ddc/registry/DdcRpcProviderDirectoryTest.java`

- Purpose: Assert exact Provider query plus 80/missing/invalid weight mapping.
- Symbols: existing subscription tests expanded with endpoint weight assertions.
- Repository evidence: current test captures DDC key and snapshot mapping with fake client.
- Dependencies and consumers: `ServiceInstanceMetaCodec`, Provider endpoint.
- Why now: It is the RED proof that decoded metadata is currently ignored.
- Contract/signature changes: endpoint gains `weight()`; query/wire unchanged.
- Input/output and state mapping: DDC instance metadata → decoded meta → Provider endpoint weight.
- Error and edge behavior: missing/invalid remote value maps 100; expired lease behavior unchanged.
- Implementation pseudocode:

```java
publish(instance(metadata("gateway.weight", "80"))); assertThat(snapshot.endpoints().getFirst().weight()).isEqualTo(80);
publish(instance(Map.of())); assertThat(endpoint.weight()).isEqualTo(100);
publish(instance(metadata("gateway.weight", "invalid"))); assertThat(endpoint.weight()).isEqualTo(100);
```

- Verification contribution: Primary `TEST-023` Provider half and exact `RPC_PROVIDER` boundary.
- After this file: RED fails on missing endpoint weight/accessor.

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter/src/test/java/top/egon/cola/component/rpc/ddc/registry/DdcRpcGatewayDirectoryTest.java`

- Purpose: Assert identical weight projection for Gateway snapshots.
- Symbols: weight/default cases added to existing exact-key subscription tests.
- Repository evidence: directory currently mirrors Provider mapping and discards decoded meta.
- Dependencies and consumers: Gateway endpoint, DDC fake client.
- Why now: Weighted Gateway Reference requires parity, not Provider-only weight.
- Contract/signature changes: endpoint gains weight record component.
- Input/output and state mapping: INTERNAL_GATEWAY instance metadata → Gateway endpoint weight.
- Error and edge behavior: invalid/missing becomes 100; biz/app fallback and lease mapping unchanged.
- Implementation pseudocode:

```java
subscribe(gatewayQuery, listener); publishGateway(weightMetadata("80"));
assertThat(lastSnapshot.endpoints()).singleElement().extracting(RpcGatewayEndpoint::weight).isEqualTo(80);
assertDefaultWeightFor(missingMetadata()); assertDefaultWeightFor(invalidMetadata());
```

- Verification contribution: Primary `TEST-023` Gateway half.
- After this file: RED demonstrates both modes lack weight.

#### File 3 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/channel/RpcEndpoint.java`

- Purpose: Add a compatibility-safe neutral weight contract.
- Symbols: default `int weight()`.
- Repository evidence: third-party/custom endpoints currently implement only three transport methods.
- Dependencies and consumers: LB context/strategies and endpoint records.
- Why now: Weighted algorithms must not depend on DDC-specific types.
- Contract/signature changes: default method returns 100, so existing implementations compile unchanged.
- Input/output and state mapping: endpoint implementation → selection weight; transport identity unchanged.
- Error and edge behavior: callers defensively normalize out-of-range values to 100; interface does not throw.
- Implementation pseudocode:

```java
default int weight() { return 100; }
// Weight is selection metadata, never part of RpcChannelKey or transport security identity.
// Custom endpoints compiled before this addition retain effective weight 100.
```

- Verification contribution: compilation compatibility and LB default test.
- After this file: all endpoints have a neutral default weight.

#### File 4 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/provider/RpcProviderEndpoint.java`

- Purpose: Store Provider instance weight alongside lease identity.
- Symbols: `weight` record component/validation and compatibility constructor if required by existing tests.
- Repository evidence: record validates route/port/lease; many current callers construct six components.
- Dependencies and consumers: DDC adapter, manager/LB.
- Why now: Provider snapshot is weight's correct lifecycle owner.
- Contract/signature changes: additive component plus six-arg constructor delegating weight=100 to protect callers.
- Input/output and state mapping: decoded weight → immutable endpoint; identity excludes weight for lease/channel reuse.
- Error and edge behavior: constructor normalizes invalid weight to 100 for untrusted adapters; write side separately rejects invalid configuration.
- Implementation pseudocode:

```java
record RpcProviderEndpoint(..., Instant leaseExpireAt, int weight) implements RpcEndpoint {
    RpcProviderEndpoint(oldSixArgs) { this(oldSixArgs..., 100); }
    compact { validateLeaseAndAddress(); weight = validWeight(weight) ? weight : 100; }
}
```

- Verification contribution: Provider directory and LB tests.
- After this file: Provider candidates carry bounded weights without breaking old constructors.

#### File 5 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/gateway/RpcGatewayEndpoint.java`

- Purpose: Store Gateway instance weight with the same compatibility rules.
- Symbols: weight component and old-shape constructor.
- Repository evidence: Gateway record mirrors Provider endpoint and is constructed throughout tests/adapters.
- Dependencies and consumers: DDC Gateway directory, manager/LB.
- Why now: Gateway annotation exposes LB, so weights cannot be Direct-only.
- Contract/signature changes: additive component; legacy constructor defaults 100.
- Input/output and state mapping: decoded weight → immutable Gateway candidate; channel key remains host/port/secure.
- Error and edge behavior: invalid remote weight becomes 100; lease expiration unchanged.
- Implementation pseudocode:

```java
record RpcGatewayEndpoint(..., Instant leaseExpireAt, int weight) implements RpcEndpoint {
    RpcGatewayEndpoint(oldSixArgs) { this(oldSixArgs..., 100); }
    compact { validateIdentityAddressExpiry(); weight = normalizeWeight(weight); }
}
```

- Verification contribution: Gateway directory and all LB mode parity tests.
- After this file: both endpoint kinds expose identical selection metadata.

#### File 6 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter/src/main/java/top/egon/cola/component/rpc/ddc/registry/DdcRpcProviderDirectory.java`

- Purpose: Map decoded `ServiceInstanceMeta.weight` into Provider endpoints.
- Symbols: snapshot mapping lambda.
- Repository evidence: code currently calls decode then discards its result.
- Dependencies and consumers: DDC codec and new Provider constructor.
- Why now: Completes Provider metadata projection with no new cache.
- Contract/signature changes: neutral snapshot contains weight; subscription API unchanged.
- Input/output and state mapping: DDC metadata map → codec → `meta.weight()` → endpoint.
- Error and edge behavior: rely on codec's default/validation; do not catch/replace snapshot revision/lease fields.
- Implementation pseudocode:

```java
ServiceInstanceMeta meta = ServiceInstanceMetaCodec.decode(instance.metadata());
return new RpcProviderEndpoint(instance.instanceId(), instance.leaseId(), instance.host(), instance.port(),
        instance.secure(), instance.leaseExpireAt(), meta.weight());
```

- Verification contribution: File 1 GREEN and no DDC query change.
- After this file: Provider snapshots carry canonical weights.

#### File 7 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter/src/main/java/top/egon/cola/component/rpc/ddc/registry/DdcRpcGatewayDirectory.java`

- Purpose: Map the same decoded weight into Gateway endpoints.
- Symbols: snapshot mapping lambda.
- Repository evidence: identical discarded decode call exists here.
- Dependencies and consumers: DDC codec and Gateway constructor.
- Why now: Completes both-mode parity.
- Contract/signature changes: subscription/query unchanged.
- Input/output and state mapping: DDC instance metadata → typed meta weight → Gateway endpoint.
- Error and edge behavior: no alternate key, no raw string parser, no snapshot suppression.
- Implementation pseudocode:

```java
ServiceInstanceMeta meta = ServiceInstanceMetaCodec.decode(instance.metadata());
return new RpcGatewayEndpoint(instance.instanceId(), instance.leaseId(), instance.host(), instance.port(),
        instance.secure(), instance.leaseExpireAt(), meta.weight());
```

- Verification contribution: File 2 GREEN and adapter parity.
- After this file: all consumer candidates use one weight authority.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-rpc-ddc-adapter -am -Dtest=DdcRpcProviderDirectoryTest,DdcRpcGatewayDirectoryTest,RpcLoadBalancersTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0; 80/missing/invalid cases map identically; exact DDC query/revision/lease assertions remain green.
- Failure returns to: Files 3–5 for endpoint compatibility; Files 6–7 for codec projection; DDC wire change returns to Spec.
- Completion criteria: both modes deliver 1–10000/default100 weights to neutral LB without new DDC files/jobs/proto.
- Rollback: revert endpoint/adapter paths together; old consumers resume effective weight 100; no data migration.
- Commit paths: egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter/src/test/java/top/egon/cola/component/rpc/ddc/registry/DdcRpcProviderDirectoryTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter/src/test/java/top/egon/cola/component/rpc/ddc/registry/DdcRpcGatewayDirectoryTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/channel/RpcEndpoint.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/provider/RpcProviderEndpoint.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/gateway/RpcGatewayEndpoint.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter/src/main/java/top/egon/cola/component/rpc/ddc/registry/DdcRpcProviderDirectory.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter/src/main/java/top/egon/cola/component/rpc/ddc/registry/DdcRpcGatewayDirectory.java
- Commit: `feat(rpc): project registry endpoint weights`

### Step 5 — Centralize ManagedChannel ownership in a shared multiplexing pool

- Requirements: `REQ-011`, `REQ-017`
- Dependencies: Step 4
- Baseline state: Gateway/Provider Managers eagerly create and own separate ManagedChannels per snapshot/query; Channel factory has no stable key overload.
- Observable outcome: concurrent acquire of the same host/port/secure key creates one Channel, returns independent leases, tracks in-flight use, drains at ref=0 and force closes after timeout.
- End state: pool is independently tested and ready for Manager/Executor wiring; gRPC retry remains disabled and business TLS profile remains process-global.
- Test-first gate: Required — pool test does not compile until key/lease/pool exist and then fails on single-flight/ref/drain races.
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/channel/RpcConsumerChannelPoolTest.java`

- Purpose: Define single-flight creation, lease/in-flight accounting, graceful/forced drain and idempotent close.
- Symbols: `RpcConsumerChannelPoolTest` mapped to Primary `TEST-026`,`028`–`030`.
- Repository evidence: existing manager tests use mock ManagedChannel/latches; JUnit supports concurrent barriers.
- Dependencies and consumers: counting channel factory, fake channel, key/lease/pool.
- Why now: Resource ownership must be RED before managers stop owning channels.
- Contract/signature changes: `acquire(endpoint)` returns lease with `channel`, `beginCall/endCall`, `close`.
- Input/output and state mapping: endpoint key/ref/in-flight → CONNECTING/ACTIVE/DRAINING/CLOSED; factory count observable.
- Error and edge behavior: concurrent factory failure removes entry; acquire while pool draining fails; close/release/endCall are idempotent; timeout calls shutdownNow once.
- Implementation pseudocode:

```java
runConcurrently(32, () -> leases.add(pool.acquire(endpoint))); assertThat(factory.createCount()).isOne();
lease.beginCall(); closeAllReferences(); assertThat(channel.isShutdown()).isFalse(); lease.endCall(); assertGracefulClose();
holdInFlightPast(timeout); pool.close(); assertThat(channel.shutdownNowCount()).isOne();
```

- Verification contribution: exact ownership/concurrency/drain proof before transport integration.
- After this file: RED identifies missing pool behavior only.

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/channel/RpcConsumerChannelFactory.java`

- Purpose: Build Channels from stable pool keys while preserving endpoint overloads.
- Symbols: additive `create(RpcChannelKey)` and existing `create(RpcEndpoint)` delegation.
- Repository evidence: current factory centralizes Netty/mTLS/plaintext/disableRetry and awaitReady.
- Dependencies and consumers: Channel pool, legacy managers/direct callers.
- Why now: Pool must not duplicate transport construction/security checks.
- Contract/signature changes: additive overload only; existing endpoint/Gateway overloads remain.
- Input/output and state mapping: key host/port/secure → NettyChannelBuilder → ManagedChannel.
- Error and edge behavior: same TLS mismatch errors; retry disabled; factory does not cache or await on its own.
- Implementation pseudocode:

```java
ManagedChannel create(RpcEndpoint endpoint) { return create(RpcChannelKey.from(endpoint)); }
ManagedChannel create(RpcChannelKey key) { var builder=forAddress(key.host(),key.port()).disableRetry(); applySecurity(builder,key.secure()); return builder.build(); }
// awaitReady remains a bounded compatibility helper, not pool ownership.
```

- Verification contribution: pool and existing channel factory tests share the same security path.
- After this file: transport creation accepts stable pool identity.

#### File 3 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/channel/RpcChannelKey.java`

- Purpose: Define equality/hash identity for shared business Channels.
- Symbols: record `host`, `port`, `secure`; `from(RpcEndpoint)`.
- Repository evidence: Primary §10 states current business TLS profile is process-global; lease/query/weight must not fragment transport reuse.
- Dependencies and consumers: endpoints, factory, pool.
- Why now: `ManagedChannel` identity cannot use mutable lease objects.
- Contract/signature changes: new internal value type.
- Input/output and state mapping: normalized route address/security → stable map key.
- Error and edge behavior: reject blank/unroutable host and invalid port; exclude weight/instance/lease/service from equality.
- Implementation pseudocode:

```java
record RpcChannelKey(String host, int port, boolean secure) {
    compact { host=normalizeRoutable(host); requirePort(port); }
    static RpcChannelKey from(RpcEndpoint endpoint) { return new RpcChannelKey(endpoint.host(), endpoint.port(), endpoint.secure()); }
}
```

- Verification contribution: same endpoint across queries/modes converges in pool test.
- After this file: Channel identity is explicit and immutable.

#### File 4 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/channel/RpcChannelLease.java`

- Purpose: Separate shared Channel ownership from per-invocation/use lifetime.
- Symbols: `channel`, `beginCall`, `endCall`, `close`, atomic guards.
- Repository evidence: current managers expose raw Channel and cannot release shared refs safely.
- Dependencies and consumers: package-private pool entry callbacks; executor.
- Why now: Multiplexing needs one release per reference and in-flight stream.
- Contract/signature changes: new AutoCloseable handle; raw Channel is borrowed, never directly shut down by callers.
- Input/output and state mapping: acquire increments ref; begin/end modify in-flight; close decrements ref once.
- Error and edge behavior: use after close/draining rejected; duplicate close/end ignored or throws deterministic misuse error per test; no negative counters.
- Implementation pseudocode:

```java
ManagedChannel channel() { requireOpen(); return entry.channel(); }
void beginCall() { requireOpen(); entry.incrementInFlight(); callCount.incrementAndGet(); }
void close() { if (closed.compareAndSet(false,true)) { finishOutstandingCalls(); entry.releaseReference(); } }
```

- Verification contribution: lease accounting and cancellation release assertions.
- After this file: callers can safely share one Channel entry.

#### File 5 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/channel/RpcConsumerChannelPool.java`

- Purpose: Own single-flight Channel entries and pool lifecycle.
- Symbols: states, `acquire`, release/call callbacks, `drain`, `close`, entry map.
- Repository evidence: Primary §10.6 state machine and existing manager drain timeout/property provide required behavior.
- Dependencies and consumers: channel factory/key/lease, Consumer coordinator/executor.
- Why now: Minimum GREEN before Manager migration.
- Contract/signature changes: new SmartLifecycle/AutoCloseable internal runtime; configured drain timeout.
- Input/output and state mapping: key → one entry/channel/ref/in-flight/state; zero refs triggers bounded drain; pool close rejects acquisitions and drains all.
- Error and edge behavior: compute/single-flight failure cleanup, shutdown callback races, timeout force close, no broad synchronized invocation lock.
- Implementation pseudocode:

```java
Entry entry = entries.compute(key, (k,current) -> current == null ? connect(k) : current.retain());
RpcChannelLease acquire(endpoint) { requireAccepting(); return entryFor(RpcChannelKey.from(endpoint)).newLease(); }
onZeroRefs(entry) { entry.markDraining(); shutdown(); closeWhen(inFlight==0 || timeout); entries.remove(key,entry); }
```

- Verification contribution: File 1 GREEN; later component test proves HTTP/2 concurrency.
- After this file: shared Channel ownership/drain is self-contained and thread-safe.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-rpc-starter -am -Dtest=RpcConsumerChannelPoolTest,RpcTransportSecurityTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0; one factory call per key, exact drain/force-close counts, TLS regressions green.
- Failure returns to: File 3 for key convergence, File 4 for accounting, File 5 for races/drain, File 2 for transport security.
- Completion criteria: pool passes concurrency and lifecycle tests without manager/proxy changes or leaked threads/channels.
- Rollback: revert Step 5; no persisted data; existing managers still own their current channels until Step 6.
- Commit paths: egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/channel/RpcConsumerChannelPoolTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/channel/RpcConsumerChannelFactory.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/channel/RpcChannelKey.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/channel/RpcChannelLease.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/channel/RpcConsumerChannelPool.java
- Commit: `feat(rpc): add shared consumer channel pool`

### Step 6 — Compile both annotations into immutable fixed-mode strategies and a graceful Consumer runtime

- Requirements: `REQ-001`, `REQ-002`, `REQ-003`, `REQ-004`, `REQ-005`, `REQ-006`, `REQ-011`, `REQ-023`
- Dependencies: Steps 3–5
- Baseline state: BPP/factories pass annotations directly; Managers own eager channels and partial states; Gateway/Direct selection has no common definition or Consumer-wide invocation gate.
- Observable outcome: one resolver builds common policy + mode target, one Factory creates exactly one closeable mode Strategy, Managers cache immutable endpoint snapshots only, and Consumer lifecycle opens/closes discovery/pool in deterministic order.
- End state: fixed-mode candidate access is ready for executor/proxy; existing Manager constructors/currentChannel compatibility remains temporarily backed by the shared pool so intermediate callers compile without duplicate raw Channel ownership.
- Test-first gate: Required — resolver/factory/coordinator tests do not compile; Manager tests fail when they expect endpoint snapshots/ref-counted subscription and no eager duplicate Channel.
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/reference/RpcReferenceDefinitionResolverTest.java`

- Purpose: Lock common-field precedence, exact Direct identity, hash/fallback validation and immutable mode.
- Symbols: resolver tests mapped to Primary `TEST-001`–`003`,`022`,`054`,`056` declaration portions.
- Repository evidence: `RpcContractValidatorTest` supplies descriptor fixtures; Spring tests use named beans/ApplicationContext.
- Dependencies and consumers: annotations, properties, contract descriptor, resolver/hash/fallback beans.
- Why now: Resolver is the single authority before Strategies or proxies consume declarations.
- Contract/signature changes: `resolve(Field, RpcContractDescriptor)` returns one definition containing an immutable effective policy per Java method; method/reference/service/default precedence is exact.
- Input/output and state mapping: field annotations + each service/method metadata + Consumer defaults → normalized mode target and method-policy map.
- Error and edge behavior: double annotation, invalid target/ranges, missing/wrong hash resolver/fallback, CONSISTENT_HASH blank resolver fail with bean+field and no secrets.
- Implementation pseudocode:

```java
Definition direct = resolver.resolve(field("direct"), validator.validate(EchoContract.class));
assertThat(direct.mode()).isEqualTo(DIRECT); assertThat(direct.policyFor(echoMethod)).usingRecursiveComparison().isEqualTo(expectedEffectivePolicy());
assertThatThrownBy(() -> resolve(consistentHashWithoutBean)).hasMessageContaining("bean").hasMessageNotContaining("authorization");
```

- Verification contribution: exact annotation compatibility/precedence and nonretryable configuration errors.
- After this file: RED identifies missing reference model/resolver.

#### File 2 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/reference/RpcReferenceStrategyFactoryTest.java`

- Purpose: Prove one mode maps to one Strategy and opposite Manager is never consulted.
- Symbols: Gateway/Direct creation, missing Manager, retain/close/ref-count tests.
- Repository evidence: existing BPP tests distinguish two factories; Primary forbids runtime switch API.
- Dependencies and consumers: fake Managers/pool/LB registry, definitions, Strategy Factory.
- Why now: Mode isolation is the central RED boundary.
- Contract/signature changes: `create(definition)` returns closeable Strategy with fixed `mode/candidates/revision/queryIdentity`.
- Input/output and state mapping: definition mode → matching Manager demand → immutable candidate snapshot; close releases only that demand.
- Error and edge behavior: missing required Directory/Manager gives mode-specific contract/startup error; no other Manager call even on empty candidates.
- Implementation pseudocode:

```java
var direct = factory.create(directDefinition); assertThat(direct.mode()).isEqualTo(DIRECT);
direct.candidates(context); verify(providerManager).snapshot(exactQuery); verifyNoInteractions(gatewayManager);
direct.close(); verify(providerManager).release(exactQuery); assertThat(factory.hasSwitchApi()).isFalse();
```

- Verification contribution: Primary `TEST-004`,`007`,`008`,`070` mode/demand portions.
- After this file: RED fails on absent Strategy hierarchy/Factory.

#### File 3 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/gateway/RpcConsumerGatewayManagerTest.java`

- Purpose: Reframe Gateway Manager as revisioned endpoint cache/demand owner with pool-backed compatibility.
- Symbols: initial fail-fast, event snapshots, revision rollback, expiry, retain/release, no eager duplicate Channel tests.
- Repository evidence: current test already covers discovery state, multiple gateways, drains and failures.
- Dependencies and consumers: fake directory/clock/pool, Gateway Manager.
- Why now: Strategy must read cache rather than Manager-owned selection loop.
- Contract/signature changes: adds snapshot/retain/release APIs; existing `currentChannel` remains compatibility only.
- Input/output and state mapping: directory snapshots/demand count → immutable endpoints/revision/state; lease expiry filters candidate.
- Error and edge behavior: old revision ignored; no demand means no subscription; startup pull failure cleanup; empty becomes unavailable, never Provider lookup.
- Implementation pseudocode:

```java
manager.retainDemand(); manager.start(); directory.publish(snapshot(2, endpoints));
assertThat(manager.snapshot().revision()).isEqualTo(2); directory.publish(snapshot(1, stale)); assertRevisionStill(2);
manager.releaseDemand(); assertThat(subscription.closeCount()).isOne(); verifyNoInteractions(providerDirectory);
```

- Verification contribution: Primary `TEST-010`,`014`,`015`,`070`,`071` Gateway portions.
- After this file: RED pinpoints current eager channel/cache coupling.

#### File 4 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/provider/RpcConsumerProviderManagerTest.java`

- Purpose: Lock exact-query shared subscription, immutable snapshot, revision/expiry and demand release.
- Symbols: existing direct manager tests rewritten around candidate snapshots and compatibility lease release.
- Repository evidence: current Manager already deduplicates registrations map and filters revision/lease.
- Dependencies and consumers: fake Provider Directory/clock/pool.
- Why now: Direct Strategy relies on snapshot-only ownership.
- Contract/signature changes: `retain(query)`, `snapshot(query)`, `release(query)`; existing register/currentChannel compatibility preserved.
- Input/output and state mapping: exact query/ref count + DDC snapshot → one cached immutable endpoint list/revision.
- Error and edge behavior: missing query/empty/expired yields Provider unavailable; query removal closes subscription/LB state/pool compatibility refs once.
- Implementation pseudocode:

```java
AutoCloseable first=manager.retain(query), second=manager.retain(query); assertThat(directory.subscribeCount()).isOne();
publish(revision(5), activeAndExpired); assertThat(manager.snapshot(query).endpoints()).containsExactly(active);
first.close(); assertOpen(); second.close(); assertThat(subscription.closeCount()).isOne();
```

- Verification contribution: Primary `TEST-010`,`014`,`015`,`069` discovery ownership portions.
- After this file: RED exposes missing ref-counted cache API.

#### File 5 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/lifecycle/RpcConsumerLifecycleCoordinatorTest.java`

- Purpose: Define STARTING/READY/DEGRADED/FAILED/DRAINING/STOPPED ordering and single callback.
- Symbols: tests mapped to Primary `TEST-070`–`072`.
- Repository evidence: Provider lifecycle/Manager tests use SmartLifecycle and latches; Consumer currently has no coordinator.
- Dependencies and consumers: fake Managers, strategy factory, pool, close hooks, clock/callback.
- Why now: Invocation gate and cleanup order must be fixed before executor.
- Contract/signature changes: coordinator exposes `state`, `requireAccepting`, SmartLifecycle start/stop(callback).
- Input/output and state mapping: primary demands/snapshot readiness → state; stop closes gate→hooks/subscriptions→pool→STOPPED.
- Error and edge behavior: Gateway startup failure closes all opened resources; Direct empty means DEGRADED; repeated start/stop/callback idempotent; new invocation during drain rejected before DDC/pool.
- Implementation pseudocode:

```java
coordinator.start(); assertThat(events).containsExactly("pool.start","gateway.start","provider.start","gate.open");
provider.publishEmpty(); assertThat(coordinator.state()).isEqualTo(DEGRADED); assertThatCode(coordinator::requireAccepting).doesNotThrowAnyException();
coordinator.stop(callback); assertThat(events).endsWith("gate.close","hooks.close","managers.stop","pool.close","callback");
```

- Verification contribution: full Consumer lifecycle acceptance before Spring wiring.
- After this file: RED is missing lifecycle state/coordinator.

#### File 6 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/reference/package-info.java`

- Purpose: Document definition/strategy ownership and fixed-mode invariant.
- Symbols: package documentation.
- Repository evidence: new stable runtime packages require explicit boundaries.
- Dependencies and consumers: reference types.
- Why now: Prevents Reference package from absorbing invocation/channel algorithms.
- Contract/signature changes: none.
- Input/output and state mapping: declaration→definition→one strategy is documented.
- Error and edge behavior: states no cross-mode fallback/switch and no per-call annotation reflection.
- Implementation pseudocode:

```java
/** Compiles field declarations into immutable policy and one fixed transport-mode strategy.
 * Strategies expose cached endpoint snapshots only; invocation, balancing and channel ownership live elsewhere.
 * No API in this package may switch between Gateway and Direct after definition creation. */
```

- Verification contribution: architecture/static review.
- After this file: package invariant is explicit.

#### File 7 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/reference/RpcReferenceMode.java`

- Purpose: Represent the immutable client-selected mode.
- Symbols: `GATEWAY`, `DIRECT`.
- Repository evidence: annotation type, not a runtime AUTO flag, is the approved source.
- Dependencies and consumers: definition/strategy/executor/generic target.
- Why now: One enum replaces scattered `instanceof annotation` branching.
- Contract/signature changes: new internal enum only.
- Input/output and state mapping: annotation kind → enum once; never mutable.
- Error and edge behavior: no AUTO/fallback value; future value forces factory coverage.
- Implementation pseudocode:

```java
public enum RpcReferenceMode { GATEWAY, DIRECT }
// The enum is assigned by RpcReferenceDefinitionResolver from the annotation type.
// Invocation retry may change endpoint identity but must never change this value.
```

- Verification contribution: Factory exhaustiveness/no-AUTO assertions.
- After this file: mode identity is explicit.

#### File 8 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/reference/RpcReferencePolicy.java`

- Purpose: Hold fully resolved call-site policy independent of annotation instance.
- Symbols: immutable record fields timeout/retries/LB/fallback/fail/hash resolver.
- Repository evidence: both annotations now expose same fields; Primary §10.1 rejects hot-path annotation use.
- Dependencies and consumers: resolver, invocation plan/executor.
- Why now: Common semantics need one normalized model.
- Contract/signature changes: internal record; values are effective, so no INHERIT/-1 remains.
- Input/output and state mapping: one method's layered declarations/defaults → validated effective policy and resolved optional bean/function references.
- Error and edge behavior: timeout>0, retries bounded, LB non-INHERIT, fallback/hash consistency enforced in compact constructor/resolver.
- Implementation pseudocode:

```java
record RpcReferencePolicy(long timeoutMs, int retries, LoadBalance loadBalance, FailStrategy failStrategy,
        String fallbackBean, RpcLoadBalanceKeyResolver keyResolver) {
    compact { require(timeoutMs>0 && retries>=0 && loadBalance!=INHERIT); validateStrategyBeans(); }
}
```

- Verification contribution: File 1 recursive parity/precedence assertions.
- After this file: common policy is immutable and hot-path ready.

#### File 9 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/reference/RpcReferenceDefinition.java`

- Purpose: Combine fixed mode, mode-specific target and the immutable per-method effective policy map.
- Symbols: mode, normalized `RpcServiceIdentity`, optional exact Provider query, optional typed `policyFor(Method)` index and generic factory.
- Repository evidence: Direct target lifecycle differs from common policy; composition was selected over annotation inheritance.
- Dependencies and consumers: resolver/strategy factory/proxy plan.
- Why now: Factory input must be complete and immutable.
- Contract/signature changes: internal record with static Gateway/Direct factories if useful.
- Input/output and state mapping: typed contract + annotation → service identity and complete method-policy index; generic command → the same mode/service/query shape with no Java-method index because its policy is stored directly in `RpcInvocationPlan`.
- Error and edge behavior: Direct requires exact query; Gateway forbids Provider query; no opposite-mode target field.
- Implementation pseudocode:

```java
record RpcReferenceDefinition(RpcReferenceMode mode, RpcServiceIdentity serviceIdentity,
        RpcProviderQuery directQuery, Map<Method,RpcReferencePolicy> typedPolicies) {
    compact { validateModeTarget(); typedPolicies=immutableIndex(typedPolicies); }
    static RpcReferenceDefinition generic(mode,serviceIdentity,directQuery) { return new(...,Map.of()); }
}
```

- Verification contribution: definition invariant and no-cross-mode factory tests.
- After this file: all data needed to choose one strategy is explicit.

#### File 10 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/reference/RpcReferenceDefinitionResolver.java`

- Purpose: Resolve annotations/contract/method/config and trusted named beans once.
- Symbols: constructor with properties/ApplicationContext, `resolve(Field, descriptor)`, policy helpers.
- Repository evidence: current BPP passes only timeout and Direct factory rebuilds query; Spring named-bean checks belong at startup.
- Dependencies and consumers: annotations, descriptor, process identity, properties, ApplicationContext.
- Why now: Minimum GREEN for File 1 and one source of precedence.
- Contract/signature changes: new internal resolver; errors use stable RPC codes and bean/field context.
- Input/output and state mapping: Field → annotation kind/target + each method/reference/service/default layer → definition with complete policy index; resolver/fallback beans captured once.
- Error and edge behavior: exact safe segments, timeout/retry bounds, fallback interface, hash key bean type; no credentials/values in messages.
- Implementation pseudocode:

```java
AnnotationChoice choice = requireExactlyOneReference(field); RpcReferenceMode mode = modeOf(choice);
Map<Method,RpcReferencePolicy> policies = contract.methods().stream().collect(uniqueToMap(javaMethod,
        method -> resolveEffectivePolicy(method, contract, choice.commonFields(), properties.getConsumer(), applicationContext)));
return mode==DIRECT ? directDefinition(RpcServiceIdentity.from(contract),exactQuery(choice,contract,processIdentity),policies)
        : gatewayDefinition(RpcServiceIdentity.from(contract),policies);
```

- Verification contribution: File 1 GREEN and BPP later becomes simple orchestration.
- After this file: declarations compile into validated immutable definitions.

#### File 11 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/reference/RpcReferenceStrategy.java`

- Purpose: Expose only fixed-mode cached candidates and demand lifecycle to executor.
- Symbols: `mode`, `queryIdentity`, `revision`, `candidates`, `close`.
- Repository evidence: Primary §7.3.1 says Strategy supplies candidates; LB/pool own later steps.
- Dependencies and consumers: endpoint snapshots, executor/generic cache.
- Why now: Factory/concrete strategies need a small common port.
- Contract/signature changes: new internal AutoCloseable SPI; no `switchMode` or Channel creation.
- Input/output and state mapping: current Manager snapshot → immutable candidates/revision.
- Error and edge behavior: closed strategy rejects access; empty list is returned/classified by executor, never triggers other mode.
- Implementation pseudocode:

```java
RpcReferenceMode mode(); String queryIdentity(); long revision();
List<? extends RpcEndpoint> candidates();
void close(); // releases only this strategy's retained Manager demand
```

- Verification contribution: no-cross-mode and demand-release tests.
- After this file: executor-facing mode port is defined.

#### File 12 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/reference/RpcReferenceStrategyFactory.java`

- Purpose: Select exactly one concrete strategy and track created strategies for lifecycle cleanup.
- Symbols: `create`, registry of mode constructors, `close`.
- Repository evidence: user requested Strategy/Factory; two modes are a real stable variation.
- Dependencies and consumers: optional Managers, definitions, Consumer coordinator/generic cache.
- Why now: Minimum GREEN for File 2.
- Contract/signature changes: new internal factory; missing mode dependency fails before invocation.
- Input/output and state mapping: definition.mode → one strategy retaining only corresponding demand; active strategies tracked idempotently.
- Error and edge behavior: null/unknown/missing Manager fails; close all once; strategy creation failure releases partial demand.
- Implementation pseudocode:

```java
return switch (definition.mode()) {
    case GATEWAY -> track(new GatewayRpcReferenceStrategy(definition, requireGatewayManager()));
    case DIRECT -> track(new DirectRpcReferenceStrategy(definition, requireProviderManager())); };
```

- Verification contribution: File 2 GREEN and coordinator cleanup.
- After this file: mode selection is exhaustive and immutable.

#### File 13 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/reference/GatewayRpcReferenceStrategy.java`

- Purpose: Bind one Gateway definition to the Gateway primary demand/cache only.
- Symbols: constructor/Strategy methods/atomic close.
- Repository evidence: Manager query is process-configured; Gateway Reference never owns Direct identity.
- Dependencies and consumers: Gateway Manager and definition.
- Why now: Concrete mode implementation for factory.
- Contract/signature changes: internal final class.
- Input/output and state mapping: creation retains Gateway demand; candidates/revision delegate to Gateway snapshot; close releases demand.
- Error and edge behavior: unavailable snapshot stays empty/mode-specific; no Provider Manager reference/import.
- Implementation pseudocode:

```java
constructor { require(definition.mode()==GATEWAY); demand=gatewayManager.retainDemand(); }
List<RpcGatewayEndpoint> candidates() { return gatewayManager.snapshot().endpoints(); }
closeOnce(() -> demand.close()); // never constructs or queries RpcProviderQuery
```

- Verification contribution: opposite-manager zero-call assertions.
- After this file: Gateway strategy has no cross-mode path.

#### File 14 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/reference/DirectRpcReferenceStrategy.java`

- Purpose: Bind one Direct definition to its exact Provider query/cache only.
- Symbols: constructor/Strategy methods/atomic close.
- Repository evidence: `RpcProviderQuery` already contains biz/app/env/service/group/version/protocol.
- Dependencies and consumers: Provider Manager and definition query.
- Why now: Concrete Direct implementation for factory.
- Contract/signature changes: internal final class.
- Input/output and state mapping: creation retains exact query; candidates/revision delegate; close releases ref count.
- Error and edge behavior: empty/expired stays Direct-unavailable; no Gateway Manager reference/import.
- Implementation pseudocode:

```java
constructor { require(definition.mode()==DIRECT); query=definition.directQuery(); demand=providerManager.retain(query); }
List<RpcProviderEndpoint> candidates() { return providerManager.snapshot(query).endpoints(); }
closeOnce(() -> demand.close()); // never reads INTERNAL_GATEWAY
```

- Verification contribution: Direct exact-query/no-Gateway tests.
- After this file: Direct strategy is fixed and closeable.

#### File 15 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/gateway/RpcConsumerGatewayManager.java`

- Purpose: Own Gateway demand/subscription/revision/expiry, not primary LB or independent raw Channels.
- Symbols: retain/release handle, snapshot view, lifecycle/state; compatibility currentChannel via injected/shared pool lease.
- Repository evidence: current class already owns subscription/revision/expiry and eager ActiveGateway channels.
- Dependencies and consumers: Gateway Directory, clock/properties, optional pool compatibility, Strategy.
- Why now: Implements File 3 GREEN and single cache per primary query.
- Contract/signature changes: additive snapshot/demand APIs and constructor accepting shared pool; old constructor/methods retained for current tests/callers.
- Input/output and state mapping: directory full snapshots → sorted unexpired endpoint snapshot; demand count controls subscription.
- Error and edge behavior: initial fail-fast rules retained; old revision ignored; stop closes subscription/compat leases; no Provider calls.
- Implementation pseudocode:

```java
Demand retainDemand() { increment; ensureSubscribedWhenRunning(); return closeOnce(this::releaseDemand); }
accept(snapshot) { if (revisionIsNewer) publish(sortedActiveEndpoints(snapshot)); }
currentChannel(excluded) { return compatibilityPoolLease(selectRoundRobin(snapshot,excluded)).channel(); }
```

- Verification contribution: File 3 GREEN and existing Gateway compatibility tests.
- After this file: Gateway Manager is a cache/demand owner with pool-backed legacy access.

#### File 16 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/provider/RpcConsumerProviderManager.java`

- Purpose: Own ref-counted exact Provider caches and subscriptions, not per-query Channel sets.
- Symbols: retain/release, snapshot, lifecycle; compatibility register/currentChannel via pool.
- Repository evidence: registrations map/revision filtering already exist but each registration eagerly creates Channels.
- Dependencies and consumers: Provider Directory, pool compatibility, Direct Strategy.
- Why now: Implements File 4 GREEN and removes duplicated query state.
- Contract/signature changes: additive demand/snapshot APIs; old `register` behavior retained via wrapper.
- Input/output and state mapping: query/ref count + full snapshots → immutable sorted active endpoints/revision.
- Error and edge behavior: release final ref closes subscription/removes state/LB compatibility; expired endpoints excluded; no Gateway calls.
- Implementation pseudocode:

```java
Demand retain(query) { Registration r=registrations.compute(query, newRegistration); r.refs++; subscribeIfRunning(r); return handle; }
Snapshot snapshot(query) { expireLocally(); return immutableCurrentOrEmpty(query); }
release(query) { if (--refs==0) closeSubscriptionAndCompatibilityLeasesThenRemove(query); }
```

- Verification contribution: File 4 GREEN and shared-subscription/memory bounds.
- After this file: Provider Manager is a bounded exact-query cache.

#### File 17 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/lifecycle/package-info.java`

- Purpose: Document Consumer coordinator/state/gate ownership.
- Symbols: package documentation.
- Repository evidence: lifecycle is a new stable package in target tree.
- Dependencies and consumers: coordinator/state.
- Why now: Makes graceful-start/stop boundary explicit.
- Contract/signature changes: none.
- Input/output and state mapping: describes manager/pool/hooks ordering and invocation gate.
- Error and edge behavior: explicitly excludes DDC polling and business retry.
- Implementation pseudocode:

```java
/** Coordinates Consumer discovery, shared transport and the new-invocation gate.
 * It starts infrastructure before publishing READY/DEGRADED and drains in reverse ownership order.
 * It does not select endpoints, execute RPC attempts or create an additional DDC reconciliation job. */
```

- Verification contribution: static architecture review.
- After this file: lifecycle package scope is fixed.

#### File 18 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/lifecycle/RpcConsumerRuntimeState.java`

- Purpose: Represent complete Consumer runtime states.
- Symbols: `NEW`, `STARTING`, `READY`, `DEGRADED`, `DRAINING`, `FAILED`, `STOPPED`.
- Repository evidence: Primary §10.6 requires states unavailable in current booleans.
- Dependencies and consumers: coordinator/executor/observability.
- Why now: Tests need explicit transition assertions.
- Contract/signature changes: new internal enum.
- Input/output and state mapping: lifecycle events/snapshot readiness → one state.
- Error and edge behavior: DRAINING/FAILED are not accepting; STOPPED is idempotent terminal for instance.
- Implementation pseudocode:

```java
enum RpcConsumerRuntimeState { NEW, STARTING, READY, DEGRADED, DRAINING, FAILED, STOPPED }
boolean accepting() { return this==READY || this==DEGRADED; }
boolean terminalOrDraining() { return this==DRAINING || this==FAILED || this==STOPPED; }
```

- Verification contribution: File 5 transition matrix.
- After this file: gate semantics are expressible without booleans.

#### File 19 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/lifecycle/RpcConsumerLifecycleCoordinator.java`

- Purpose: Start discovery/pool before calls and perform reverse-order graceful cleanup.
- Symbols: SmartLifecycle `start`, `stop(Runnable)`, `state`, `requireAccepting`, close-hook registry.
- Repository evidence: current Managers are SmartLifecycle but no single gate/order owner exists.
- Dependencies and consumers: both Managers, pool, Strategy Factory, future generic cache hook, executor/AutoConfig.
- Why now: Minimum GREEN for File 5 and executor prerequisite.
- Contract/signature changes: new lifecycle bean; accepts optional resources without knowing generic implementation class.
- Input/output and state mapping: start resources→READY/DEGRADED; failure→FAILED/cleanup; stop→DRAINING/gate close/hooks/managers/pool/STOPPED/callback.
- Error and edge behavior: callback exactly once, repeated calls idempotent, startup exception suppresses cleanup failures and preserves primary cause.
- Implementation pseudocode:

```java
start() { CAS(NEW,STARTING); pool.start(); managers.start(); state=evaluatePrimaryDemands(); openGateIfReadyOrDegraded(); }
requireAccepting() { if (!state.accepting()) throw modeNeutralUnavailableWithoutDiscoveryWork(); }
stop(callback) { transitionToDRAININGAndCloseGate(); closeHooksReverse(); stopManagers(); pool.close(); state=STOPPED; callbackOnce(); }
```

- Verification contribution: File 5 GREEN and Step 7 new-call gate.
- After this file: Consumer runtime has one deterministic lifecycle owner.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-rpc-starter -am -Dtest=RpcReferenceDefinitionResolverTest,RpcReferenceStrategyFactoryTest,RpcConsumerGatewayManagerTest,RpcConsumerProviderManagerTest,RpcConsumerLifecycleCoordinatorTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0; common policy/mode isolation/cache revision/ref-count/lifecycle order tests pass; existing compatibility methods compile.
- Failure returns to: Files 8–10 for policy resolution, Files 11–16 for strategy/cache, Files 18–19 for state/order; any request for cross-mode recovery returns to Spec.
- Completion criteria: each Reference retains only one mode demand, managers use one immutable cache per exact query, old revisions/expired leases are excluded, and Consumer gate/cleanup is deterministic.
- Rollback: revert Step 6 as one unit; Step 3–5 foundation remains unused but harmless; no external state changes.
- Commit paths: egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/reference/RpcReferenceDefinitionResolverTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/reference/RpcReferenceStrategyFactoryTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/gateway/RpcConsumerGatewayManagerTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/provider/RpcConsumerProviderManagerTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/lifecycle/RpcConsumerLifecycleCoordinatorTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/reference/package-info.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/reference/RpcReferenceMode.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/reference/RpcReferencePolicy.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/reference/RpcReferenceDefinition.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/reference/RpcReferenceDefinitionResolver.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/reference/RpcReferenceStrategy.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/reference/RpcReferenceStrategyFactory.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/reference/GatewayRpcReferenceStrategy.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/reference/DirectRpcReferenceStrategy.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/gateway/RpcConsumerGatewayManager.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/provider/RpcConsumerProviderManager.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/lifecycle/package-info.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/lifecycle/RpcConsumerRuntimeState.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/lifecycle/RpcConsumerLifecycleCoordinator.java
- Commit: `refactor(rpc): unify fixed mode reference lifecycle`

### Step 7 — Implement one blocking/async same-mode invocation state machine and terminal error contract

- Requirements: `REQ-003`, `REQ-004`, `REQ-013`, `REQ-014`, `REQ-015`, `REQ-017`, `REQ-019`, `REQ-020`
- Dependencies: Step 6
- Baseline state: JDK handler owns a blocking-only idempotent/Gateway-stage retry loop; no shared deadline/cancel/terminal context or nullable fail-strategy implementation exists.
- Observable outcome: one executor handles blocking and cancellable async unary attempts, filters only acquisition/UNAVAILABLE for same-mode reselection, preserves one invocation ID/deadline, releases pool leases once, and applies availability fail strategy only after exhaustion.
- End state: executor accepts typed or raw method plans; business errors are terminal; Direct all-rate-limited maps `RPC_RATE_LIMITED`; Gateway or mixed failures remain mode-specific unavailable.
- Test-first gate: Required — executor test does not compile and status test fails for missing shared error-type/rate-limit mapping.
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/invocation/RpcInvocationExecutorTest.java`

- Purpose: Define the full attempt/deadline/cancel/error/fail-strategy matrix.
- Symbols: tests mapped to Primary `TEST-005`,`006`,`049`–`060`,`056`.
- Repository evidence: old handler test uses fake channels/status trailers; pool/LB/strategy now have deterministic seams.
- Dependencies and consumers: fake Strategy/LB/pool/ClientCall, ticker, fallback counter, typed/raw plans.
- Why now: This RED contract owns the most correctness-sensitive behavior before proxy/generic callers.
- Contract/signature changes: executor exposes blocking and cancellable `CompletionStage` entry points; plans are immutable.
- Input/output and state mapping: plan/request → context(invocationId, deadline, attempts, excluded, rate flags, terminal CAS) → result/error/null.
- Error and edge behavior: idempotent=false still retries configured UNAVAILABLE; non-UNAVAILABLE never retries/fallbacks; deadline/cancel stop; candidate once; mixed rate/unavailable classification; release once.
- Implementation pseudocode:

```java
execute(plan(retries(1), idempotent(false)), request, firstUnavailableThenSuccess()); assertThat(attempts).isEqualTo(2);
execute(plan(failOpen()), businessStatus(ALREADY_EXISTS)); assertThatThrownErrorIsOriginal(); assertThat(failStrategyCalls).isZero();
CompletionStage<?> stage=executeAsync(plan, request); cancel(stage); assertThat(activeCall.cancelCount()).isOne(); assertThat(poolReleaseCount).isOne();
```

- Verification contribution: central proof for retry safety, nullable FAIL_OPEN, async race and no-cross-mode behavior.
- After this file: RED is missing executor/plan/context/mode or expected behavior, not network setup.

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/exception/RpcStatusExceptionMapperTest.java`

- Purpose: Lock shared trailer keys and stable rate-limit/unavailable/business mappings.
- Symbols: Direct rate-limit, Gateway stripped trailer, provider/gateway stage, not-found and business status tests.
- Repository evidence: current test covers `RpcFailureStage` and error-type for not-found; mapper duplicates error-type key privately.
- Dependencies and consumers: shared `RpcMetadataKeys.ERROR_TYPE`, new error code.
- Why now: Executor classifies raw status then mapper must produce stable terminal exception.
- Contract/signature changes: `RPC_RATE_LIMITED` is additive; existing mappings remain.
- Input/output and state mapping: gRPC Status+trailers → sanitized `EgonRpcException` code/cause.
- Error and edge behavior: rate-limit requires UNAVAILABLE+provider+error type; absent type stays provider/gateway unavailable; messages redact descriptions.
- Implementation pseudocode:

```java
assertCode(map(unavailable(trailers(PROVIDER,"rate-limit"))), RPC_RATE_LIMITED);
assertCode(map(unavailable(trailers(PROVIDER,null))), RPC_PROVIDER_UNAVAILABLE);
assertSanitized(map(status(PERMISSION_DENIED,"secret sql"))).messageDoesNotContain("secret sql");
```

- Verification contribution: Primary `TEST-045`,`051` mapping portions.
- After this file: RED identifies missing shared key/error code/mapping.

#### File 3 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/invocation/package-info.java`

- Purpose: Document logical-call ownership and exclusions.
- Symbols: package documentation.
- Repository evidence: Primary target tree requires a stable invocation package.
- Dependencies and consumers: plan/context/mode/executor.
- Why now: Prevents retry/deadline logic from returning to proxies/managers.
- Contract/signature changes: none.
- Input/output and state mapping: precompiled plan+request → one logical terminal result.
- Error and edge behavior: states business idempotency/data transactions and mode switching are outside this package.
- Implementation pseudocode:

```java
/** Owns one logical unary invocation across bounded same-mode transport attempts.
 * It enforces one deadline, cancellation and terminal completion while releasing shared channel leases exactly once.
 * It neither changes reference mode nor decides business idempotency or Provider transactions. */
```

- Verification contribution: static architecture review.
- After this file: invocation responsibility is explicit.

#### File 4 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/invocation/RpcInvocationMode.java`

- Purpose: Represent Java completion shape independently of transport mode.
- Symbols: `BLOCKING`, `ASYNC`.
- Repository evidence: Primary distinguishes reference route mode from return mode.
- Dependencies and consumers: descriptors/plans/server/generic.
- Why now: Avoids boolean confusion between Direct/Gateway and sync/async.
- Contract/signature changes: new internal enum.
- Input/output and state mapping: Java method/generic API → mode; wire remains unary.
- Error and edge behavior: no streaming values; unsupported shape rejected by validator.
- Implementation pseudocode:

```java
public enum RpcInvocationMode { BLOCKING, ASYNC }
// Reference mode remains RpcReferenceMode; do not combine the two dimensions.
// Both values execute the same unary MethodDescriptor and retry classifier.
```

- Verification contribution: plan/validator async parity tests.
- After this file: completion shape is explicit.

#### File 5 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/invocation/RpcInvocationPlan.java`

- Purpose: Hold all proxy/generic-time compiled data needed without per-call reflection.
- Symbols: method descriptor, service/method identity, invocation/reference mode, policy, strategy, LB, ordered interceptor factories/process identity, fallback/result type.
- Repository evidence: current handler repeatedly searches descriptor and constructs interceptors; Primary §10.1 requires immutable plan.
- Dependencies and consumers: proxy/generic builder and executor.
- Why now: Executor must consume one stable contract for typed/raw calls.
- Contract/signature changes: new internal generic-neutral record/class; typed casts validated at construction.
- Input/output and state mapping: validated contract/reference/generic command → immutable executable plan.
- Error and edge behavior: null/mismatched marshaller/result mode rejected before network; plan closes/references strategy only through owning cache/factory.
- Implementation pseudocode:

```java
record RpcInvocationPlan(String serviceName, String fullMethodName, MethodDescriptor<Object,Object> method,
        RpcInvocationMode invocationMode, RpcReferenceMode referenceMode, RpcReferencePolicy policy,
        RpcReferenceStrategy strategy, RpcLoadBalancer loadBalancer, List<RpcClientInterceptorFactory> interceptorFactories,
        RpcProcessIdentity processIdentity, Class<?> responseType, Function<Object,Object> fallback) { validateAllAndCopy(); }
```

- Verification contribution: executor plan validation and later O(1) proxy lookup.
- After this file: runtime behavior is fully described without annotations/reflection.

#### File 6 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/invocation/RpcInvocationContext.java`

- Purpose: Own mutable per-logical-call state and single terminal guard.
- Symbols: invocation ID, absolute deadline, attempt count, excluded identities, affinity digest, rate-only flag, active call/lease, terminal CAS.
- Repository evidence: current handler stores only local attempted Channels/deadline and cannot coordinate async races.
- Dependencies and consumers: executor and observability.
- Why now: Shared plans must remain immutable/thread-safe.
- Contract/signature changes: new internal class; no public exposure of raw affinity/request/credentials.
- Input/output and state mapping: start ticker/UUID → remaining budget; failures add endpoint identity/aggregate flags; terminal releases active resources.
- Error and edge behavior: attempt cap, deadline underflow, cancel/response/error races, endpoint identity once, raw key discarded after digest.
- Implementation pseudocode:

```java
boolean beginAttempt(String endpointId) { requireNotTerminal(); return excluded.add(endpointId) && ++attempts<=maxAttempts; }
long remainingNanos() { return max(0, deadlineNanos-ticker.getAsLong()); }
boolean completeTerminal(Runnable release) { if (!terminal.compareAndSet(false,true)) return false; cancelOrReleaseOnce(); release.run(); return true; }
```

- Verification contribution: race/deadline/exclusion assertions in File 1.
- After this file: all per-call mutation has one owner.

#### File 7 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/invocation/RpcInvocationExecutor.java`

- Purpose: Execute blocking/async unary calls with one classifier/state machine.
- Symbols: `executeBlocking`, `executeAsync`, selection/acquire/attempt/classify/fail-strategy helpers.
- Repository evidence: current handler loop, gRPC `ClientCalls`, new Strategy/LB/pool/context provide all collaborators.
- Dependencies and consumers: Consumer lifecycle gate, pool, UUID/ticker, status mapper/interceptors.
- Why now: Minimum GREEN for File 1 and single source for typed/generic.
- Contract/signature changes: new runtime service; no idempotent input in retry decision.
- Input/output and state mapping: plan/request → gate→one invocation ID/one request-aware interceptor list→affinity digest→candidate/LB→lease/call→classify→retry or terminal/fallback.
- Error and edge behavior: only acquisition/UNAVAILABLE retries; one total deadline; caller cancel cancels call; all-rate Direct distinction; fallback once; normal remote null invalid.
- Implementation pseudocode:

```java
List<ClientInterceptor> interceptors=createOnceForLogicalInvocation(plan,request,context.invocationId());
while (context.hasBudgetAndAttempts()) { endpoint=selectSameMode(plan.strategy().candidates(), excluded); lease=pool.acquire(endpoint);
  try { return invokeUnary(plan, lease, interceptors, request, context.remainingNanos()); } catch (StatusRuntimeException e) { if (!isAvailability(e)) throw map(e); context.exclude(endpoint,e); } finally { releaseAttempt(); } }
return applyAvailabilityStrategyOnce(plan, aggregateFailure(context)); // FAIL_OPEN returns null
```

- Verification contribution: File 1 GREEN; later all invocation shapes delegate here.
- After this file: retry/deadline/terminal semantics are centralized.

#### File 8 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/context/invocation/RpcMetadataKeys.java`

- Purpose: Make error-type trailer a shared safe constant.
- Symbols: `ERROR_TYPE`.
- Repository evidence: status mapper currently duplicates literal; Provider Guard Adapter needs the same key.
- Dependencies and consumers: mapper/adapter/Gateway existing allowlist behavior.
- Why now: Prevents divergent trailer spelling/classification.
- Contract/signature changes: additive public constant only.
- Input/output and state mapping: ASCII trailer name → shared Metadata key.
- Error and edge behavior: value authority remains mappers; no arbitrary caller metadata exposure.
- Implementation pseudocode:

```java
public static final Metadata.Key<String> ERROR_TYPE = ascii("x-egon-rpc-error-type");
// Existing SERVICE/GROUP/VERSION/INVOCATION/FAILURE_STAGE keys remain byte-for-byte unchanged.
// Only trusted Provider/Gateway code may emit safe enumerated values.
```

- Verification contribution: mapper/Guard tests use one identity.
- After this file: error-type has one constant owner.

#### File 9 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/exception/EgonRpcErrorCode.java`

- Purpose: Add the Direct-observable rate-limit terminal code.
- Symbols: `RPC_RATE_LIMITED`.
- Repository evidence: existing enum is stable RPC application-code authority; Primary explicitly rejects a generic failover-exhausted code.
- Dependencies and consumers: status mapper/executor/docs.
- Why now: Exhaustion result needs a stable public code.
- Contract/signature changes: one additive enum constant; existing constants untouched.
- Input/output and state mapping: pure provider rate-limit exhaustion → code.
- Error and edge behavior: mixed/Gateway remains existing unavailable; never used for business rejection.
- Implementation pseudocode:

```java
enum EgonRpcErrorCode { /* existing constants */, RPC_RATE_LIMITED, RPC_INTERNAL }
// Mapper selects this only for UNAVAILABLE + provider stage + error-type=rate-limit.
// Executor downgrades mixed availability causes to the existing mode-specific unavailable code.
```

- Verification contribution: File 2/executor code assertions.
- After this file: rate-limit exhaustion is representable.

#### File 10 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/exception/RpcStatusExceptionMapper.java`

- Purpose: Consume shared metadata key and map rate-limit without exposing remote descriptions.
- Symbols: `map`, `unavailableCode`, removal of private key.
- Repository evidence: current mapper already distinguishes failure stage and sanitizes messages.
- Dependencies and consumers: MetadataKeys/ErrorCode/executor/proxy/generic.
- Why now: Makes File 2 GREEN and one stable terminal mapping.
- Contract/signature changes: additive rate code behavior; method signature unchanged.
- Input/output and state mapping: raw status/trailers → stable code + sanitized message + original cause.
- Error and edge behavior: absent/unknown type ignored; rate type on non-UNAVAILABLE does not override business mapping; Gateway stripped trailer stays unavailable.
- Implementation pseudocode:

```java
if (status==UNAVAILABLE && stage==PROVIDER && equalsIgnoreCase(trailers.get(ERROR_TYPE),"rate-limit")) code=RPC_RATE_LIMITED;
else code=existingStatusMapping(status, stage, trailers);
return new EgonRpcException(code, sanitizedMessage(code), originalException);
```

- Verification contribution: File 2 GREEN and executor final errors.
- After this file: status mapping matches the approved mode-specific contract.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-rpc-starter -am -Dtest=RpcInvocationExecutorTest,RpcStatusExceptionMapperTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0; complete retry/business/deadline/cancel/fail strategy matrix and sanitized mapping pass.
- Failure returns to: Files 5–7 for plan/state/loop; Files 8–10 for trailers/codes; business-status reclassification returns to Spec.
- Completion criteria: all invocation modes use one logical deadline, never change reference mode, ignore idempotent for retry, treat business errors terminal and release resources exactly once.
- Rollback: revert Step 7; Step 3–6 infrastructure remains but old proxy loop is still active until Step 10.
- Commit paths: egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/invocation/RpcInvocationExecutorTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/exception/RpcStatusExceptionMapperTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/invocation/package-info.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/invocation/RpcInvocationMode.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/invocation/RpcInvocationPlan.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/invocation/RpcInvocationContext.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/invocation/RpcInvocationExecutor.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/context/invocation/RpcMetadataKeys.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/exception/EgonRpcErrorCode.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/exception/RpcStatusExceptionMapper.java
- Commit: `feat(rpc): centralize invocation governance`

### Step 8 — Support exact typed CompletionStage contracts and Provider completion

- Requirements: `REQ-014`, `REQ-015`, `REQ-018`, `REQ-021`
- Dependencies: Step 7
- Baseline state: validator accepts only Message return; descriptor lookup is linear; Provider observer requires immediate Message response.
- Observable outcome: `Message method(Message)` and `CompletionStage<Response> method(Message)` validate against the same unary Proto descriptor; Provider bridges stage success/error/cancel once through existing mapper chain.
- End state: descriptors expose O(1) Java method lookup and invocation mode/output type; Provider binding remains Spring-proxy compatible.
- Test-first gate: Required — validator/server tests fail because CompletionStage is rejected/not observed; scanner proxy regression defines the Guard-compatible invocation boundary.
- Ordered files:

#### File 1 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/contract/validation/RpcContractValidatorTest.java`

- Purpose: Define accepted/rejected blocking and generic CompletionStage shapes.
- Symbols: async exact response, raw/wildcard/nested/wrong response/null contract cases.
- Repository evidence: current test covers unary/overload/Message descriptor validation.
- Dependencies and consumers: test descriptor fixtures and expanded descriptors.
- Why now: Contract shape is the RED public boundary.
- Contract/signature changes: exact `CompletionStage<GeneratedResponse>` accepted; raw/wildcard/nested rejected.
- Input/output and state mapping: reflected Java Method + Proto descriptor → `RpcMethodDescriptor(invocationMode,responseType)`.
- Error and edge behavior: request remains exactly one Message; streaming/wrong output/duplicate wire method rejected.
- Implementation pseudocode:

```java
assertThat(validate(AsyncEchoContract.class).method(asyncMethod).invocationMode()).isEqualTo(ASYNC);
assertThat(method.responseType()).isEqualTo(EchoResponse.class);
assertThatThrownBy(() -> validate(RawStageContract.class)).hasMessageContaining("CompletionStage");
```

- Verification contribution: Primary `TEST-057`,`058` and invalid-shape coverage.
- After this file: RED proves validator cannot yet describe async.

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/provider/binding/RpcProviderBeanScannerTest.java`

- Purpose: Prove synchronous/async bindings retain the Spring proxy bean and exact interface invocation method.
- Symbols: proxied async provider scan and duplicate/missing contract regressions.
- Repository evidence: scanner uses `AopUtils.getTargetClass` but stores original bean, which is required for Guard advice.
- Dependencies and consumers: Spring ProxyFactory, async contract fixture, method registry.
- Why now: Provider invocation must not unwrap AOP target.
- Contract/signature changes: binding exposes invocation mode/method while bean identity remains proxy.
- Input/output and state mapping: proxied bean/interfaces → bindings keyed by service/full method.
- Error and edge behavior: duplicate wire method rejected; no `@EgonRpcService` remains fail-fast; proxy advice observable.
- Implementation pseudocode:

```java
Object proxy = springProxy(asyncProviderWithAdviceCounter); Registry registry=scanner(proxy).scan();
Binding binding=registry.method(service,"Echo"); assertThat(binding.provider().bean()).isSameAs(proxy);
invoke(binding, request); assertThat(adviceCounter).hasValue(1); assertThat(binding.method().invocationMode()).isEqualTo(ASYNC);
```

- Verification contribution: Provider AOP/async binding regression before Guard integration.
- After this file: RED identifies descriptor/binding gaps, not Spring fixture failure.

#### File 3 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/provider/server/RpcServerServiceDefinitionFactoryTest.java`

- Purpose: Define synchronous and async Provider observer semantics and mapper/cancel races.
- Symbols: sync success, async immediate return/success/error/null stage/null value, cancellation/single terminal tests.
- Repository evidence: server factory lacks direct test; `RpcProviderExceptionMapperTest` supplies mapper fixtures and gRPC observers.
- Dependencies and consumers: fake availability, registry/bindings, controllable futures/observer.
- Why now: Server behavior must be RED before modifying reflection path.
- Contract/signature changes: same generated unary service definition supports both Java return modes.
- Input/output and state mapping: request → proxy method → Message or Stage → observer next/completed/error once.
- Error and edge behavior: availability gate precedes method/advice; null stage/value/internal error; cancellation attempts Future.cancel when supported; mapper order retained.
- Implementation pseudocode:

```java
invoke(asyncBinding, request, observer); assertThat(observer.isTerminal()).isFalse(); future.complete(response); assertSuccessOnce();
future.completeExceptionally(domainError); assertMappedErrorOnce();
cancelGrpcContext(); assertThat(cancellableFuture.isCancelled()).isTrue(); assertThat(observer.terminalCount()).isOne();
```

- Verification contribution: Primary `TEST-047`,`061` server-unit portions.
- After this file: RED fails on Message-only cast/observer path.

#### File 4 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/annotation/EgonRpcMethod.java`

- Purpose: Document that idempotent is metadata only and declare both supported Java returns.
- Symbols: Javadocs for annotation/idempotent/retries.
- Repository evidence: current docs imply retries may be disabled for non-idempotent; user explicitly changed responsibility.
- Dependencies and consumers: business contract authors/docs/static checks.
- Why now: Public contract must match runtime before proxy activation.
- Contract/signature changes: no annotation element changes.
- Input/output and state mapping: documentation maps Java signature to unary descriptor and business duplicate responsibility.
- Error and edge behavior: explicitly rejects null response/stage/streaming and states business mapper cannot use UNAVAILABLE for domain errors.
- Implementation pseudocode:

```java
/** Supported: Response method(Request) or CompletionStage<Response> method(Request), both bound to one unary Proto method.
 * idempotent is descriptive metadata and never gates configured consumer retries.
 * Business code owns duplicate safety; domain errors must use reserved non-UNAVAILABLE statuses. */
```

- Verification contribution: README/static contract gate.
- After this file: annotation documentation no longer contradicts executor.

#### File 5 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/contract/descriptor/RpcContractDescriptor.java`

- Purpose: Replace linear method search with immutable O(1) index.
- Symbols: constructor index, `method(Method)`, optional `methodByFullName`.
- Repository evidence: current `method` streams list on every proxy call.
- Dependencies and consumers: validator/proxy/generic cache.
- Why now: Async descriptor expansion is the right one-time index construction point.
- Contract/signature changes: record may become final class/retain accessors; public accessor behavior remains.
- Input/output and state mapping: validated list → immutable maps by Java Method/fullMethodName.
- Error and edge behavior: duplicate Java/full wire keys fail construction; returned collections immutable.
- Implementation pseudocode:

```java
this.methods=List.copyOf(methods); this.byJavaMethod=uniqueIndex(methods, RpcMethodDescriptor::javaMethod);
this.byFullMethodName=uniqueIndex(methods, RpcMethodDescriptor::fullMethodName);
RpcMethodDescriptor method(Method method) { return requireFound(byJavaMethod.get(method)); }
```

- Verification contribution: O(1) proxy test and duplicate validation.
- After this file: contract lookup is precompiled.

#### File 6 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/contract/descriptor/RpcMethodDescriptor.java`

- Purpose: Record Java invocation mode and exact response type beside generated descriptor.
- Symbols: new `invocationMode`, `requestType`, `responseType`; typed grpc method retained.
- Repository evidence: current descriptor stores Method/id/name/idempotent/grpc/proto only.
- Dependencies and consumers: validator, plan builder, Provider server.
- Why now: Callers/Provider must branch without re-reading generic reflection.
- Contract/signature changes: internal record components additive; construction centralized in validator.
- Input/output and state mapping: validated Java signature/Proto → immutable descriptor.
- Error and edge behavior: response type always generated Message even when Java return is Stage.
- Implementation pseudocode:

```java
record RpcMethodDescriptor(Method javaMethod, String methodName, String fullMethodName, boolean idempotent,
        RpcInvocationMode invocationMode, Class<? extends Message> requestType, Class<? extends Message> responseType,
        MethodDescriptor<Message,Message> grpcMethod, Descriptors.MethodDescriptor protoMethod) {}
```

- Verification contribution: Files 1–3 and CGLIB plan creation.
- After this file: async shape is stored once.

#### File 7 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/contract/validation/RpcContractValidator.java`

- Purpose: Parse exact CompletionStage generic return and detect duplicate wire bindings.
- Symbols: `validateMethod`, generic response resolver, duplicate checks.
- Repository evidence: current code validates direct return Message/default instance and Java overload only.
- Dependencies and consumers: reflection `ParameterizedType`, descriptors/mode enum.
- Why now: Minimum GREEN for File 1.
- Contract/signature changes: accepted Java shape expands additively; Proto/wire unchanged.
- Input/output and state mapping: return Type → BLOCKING response class or ASYNC generic response class → descriptor equality checks.
- Error and edge behavior: raw/wildcard/type variable/nested Stage/wrong descriptor/null types/duplicate proto method rejected with `RPC_INVALID_CONTRACT`.
- Implementation pseudocode:

```java
ReturnShape shape = returnType==MessageSubclass ? blocking(returnType) : exactCompletionStageArgument(genericReturnType);
assertMessageDescriptor(shape.responseType(), generated.protoMethod().getOutputType());
rejectDuplicateFullMethodNameThenCreateDescriptor(shape.mode(), requestType, shape.responseType());
```

- Verification contribution: File 1 GREEN and existing sync contract regressions.
- After this file: typed validation supports both approved unary shapes only.

#### File 8 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/binding/RpcProviderBinding.java`

- Purpose: Preserve proxied bean and expose validated contract invocation ownership.
- Symbols: compact validation/accessor helpers used by server.
- Repository evidence: record already stores original bean + descriptor.
- Dependencies and consumers: scanner/method binding/server.
- Why now: Makes proxy retention an explicit invariant for Guard advice.
- Contract/signature changes: accessors preserved; may add `contractMethod` helper without unwrapping target.
- Input/output and state mapping: Spring bean + contract descriptor → binding; service identity unchanged.
- Error and edge behavior: null/non-assignable bean fails scanner startup; no target extraction for invocation.
- Implementation pseudocode:

```java
compact { requireNonNull(bean); requireNonNull(contract); require(contract.contractType().isInstance(bean)); }
Method invocableMethod(RpcMethodDescriptor method) { return method.javaMethod(); }
RpcServiceIdentity serviceIdentity() { return RpcServiceIdentity.from(contract); }
```

- Verification contribution: File 2 proxy identity assertion.
- After this file: Provider binding explicitly retains AOP proxy.

#### File 9 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/binding/RpcProviderMethodBinding.java`

- Purpose: Expose mode/output and a single proxy-safe invocation helper.
- Symbols: `invoke(Message)`, mode/response delegates as appropriate.
- Repository evidence: current record is only pair; server reflects directly.
- Dependencies and consumers: Provider binding/method descriptor/server.
- Why now: Removes reflection branching from observer factory while keeping advice.
- Contract/signature changes: record components preserved; helper additive.
- Input/output and state mapping: request → interface Method.invoke(original proxy) → Message or CompletionStage.
- Error and edge behavior: unwrap `InvocationTargetException` target for mapper; do not swallow AOP exception.
- Implementation pseudocode:

```java
Object invoke(Message request) throws Throwable {
    try { return method.javaMethod().invoke(provider.bean(), request); }
    catch (InvocationTargetException e) { throw e.getTargetException(); }
}
```

- Verification contribution: scanner/server tests observe advice and original exception.
- After this file: Provider call path is proxy-safe and mode-neutral.

#### File 10 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/binding/RpcProviderBeanScanner.java`

- Purpose: Validate proxied bean assignability and build unique async-capable bindings.
- Symbols: `scanBean`/duplicate registry integration.
- Repository evidence: current scanner finds target interfaces via AopUtils and stores original bean.
- Dependencies and consumers: validator/binding/method registry.
- Why now: File 2 GREEN and explicit proxy regression.
- Contract/signature changes: scan API unchanged.
- Input/output and state mapping: target class identifies contracts; original bean populates binding; registry catches same wire identity.
- Error and edge behavior: JDK/CGLIB proxy interfaces discovered; duplicate service/method fail; no provider contract remains diagnostic.
- Implementation pseudocode:

```java
Class<?> targetClass=AopUtils.getTargetClass(bean); contracts=annotatedInterfaces(targetClass);
for (contract : contracts) { descriptor=validator.validate(contract); require(contract.isInstance(bean)); providers.add(new Binding(bean,descriptor)); }
return registryRejectingDuplicateFullMethods(providers);
```

- Verification contribution: File 2 and existing provider scan tests GREEN.
- After this file: scanner supports async descriptors without bypassing Spring proxy.

#### File 11 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/server/RpcServerServiceDefinitionFactory.java`

- Purpose: Bridge sync/async Provider results to one unary observer terminal path.
- Symbols: `invoke`, async completion callback, terminal guard/cancellation listener, existing `fail` mapper chain.
- Repository evidence: current `ServerCalls.asyncUnaryCall` invokes proxy and requires immediate Message.
- Dependencies and consumers: mode-bearing binding, gRPC Context, exception mappers/availability.
- Why now: Minimum GREEN for File 3.
- Contract/signature changes: generated service/method definitions unchanged; Java Provider return expands.
- Input/output and state mapping: binding mode → Message immediate or Stage callback → exact response/observer terminal.
- Error and edge behavior: null stage/value/internal; completion exception unwrapped; cancel Future when supported; availability gate before advice; terminal CAS prevents double completion.
- Implementation pseudocode:

```java
Object result=binding.invoke(request); if (mode==BLOCKING) completeMessage(requireResponse(result),observer,terminal);
else requireStage(result).whenComplete((value,error) -> { if (error!=null) failOnce(unwrap(error)); else completeMessage(requireResponse(value)); });
Context.current().addListener(ctx -> cancelFutureIfSupported(result), directExecutor());
```

- Verification contribution: File 3 GREEN and preserves ordered mapper/security availability behavior.
- After this file: Provider supports both Java completion shapes over unchanged unary wire.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-rpc-starter -am -Dtest=RpcContractValidatorTest,RpcProviderBeanScannerTest,RpcServerServiceDefinitionFactoryTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0; sync regressions and all exact async/observer/cancel cases pass.
- Failure returns to: Files 5–7 for descriptor/validation; Files 8–11 for proxy-safe Provider completion; wire changes return to Spec.
- Completion criteria: exact CompletionStage response type is validated once, Provider invokes Spring proxy and completes observer once, no `.proto` or streaming support added.
- Rollback: revert Step 8; existing blocking contracts continue under Step 7 executor foundation.
- Commit paths: egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/contract/validation/RpcContractValidatorTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/provider/binding/RpcProviderBeanScannerTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/provider/server/RpcServerServiceDefinitionFactoryTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/annotation/EgonRpcMethod.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/contract/descriptor/RpcContractDescriptor.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/contract/descriptor/RpcMethodDescriptor.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/contract/validation/RpcContractValidator.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/binding/RpcProviderBinding.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/binding/RpcProviderMethodBinding.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/binding/RpcProviderBeanScanner.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/server/RpcServerServiceDefinitionFactory.java
- Commit: `feat(rpc): support completion stage contracts`

### Step 9 — Add bounded canonical raw generic invocation through the same metadata and executor path

- Requirements: `REQ-005`, `REQ-009`, `REQ-015`, `REQ-016`, `REQ-017`, `REQ-020`, `REQ-021`
- Dependencies: Steps 6–8
- Baseline state: no dynamic API; interceptor context requires a typed contract/method/Message; dynamic target subscriptions would otherwise be unbounded.
- Observable outcome: blocking/async generic calls accept exact target, canonical `Service/Method`, raw bytes and optional affinity key, reuse fixed-mode discovery/LB/pool/executor/interceptors, and evict idle/LRU targets with exact close.
- End state: no generic Proto service, JSON/Map serializer, arbitrary metadata or dot alias is introduced; compatibility constructors/accessors for typed interceptor factories remain.
- Test-first gate: Required — generic/cache tests do not compile; interceptor test fails to represent raw invocation while retaining framework metadata/security.
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/generic/RpcGenericInvokerTest.java`

- Purpose: Define raw blocking/async parity, canonical validation, security surface, retry/cancel and nullable fail-open.
- Symbols: tests mapped to Primary `TEST-062`–`064`.
- Repository evidence: raw Gateway marshaller precedent exists; executor/status tests provide fake Strategy/pool.
- Dependencies and consumers: generic invocation/invoker, Echo bytes/descriptor fixtures, interceptor capture.
- Why now: Public generic behavior must be RED before API implementation.
- Contract/signature changes: `invokeBlocking` returns `byte[]`; `invokeAsync` returns `CompletionStage<byte[]>`.
- Input/output and state mapping: immutable command → cached plan/strategy → executor → defensive response bytes/null/error.
- Error and edge behavior: rejects dot/leading slash/mismatched service/null payload/arbitrary headers/host; cancellation and business errors match typed; consistent hash requires affinity key.
- Implementation pseudocode:

```java
byte[] response=invoker.invokeBlocking(command("egon.rpc.test.v1.EchoService/Echo", request.toByteArray()));
assertThat(EchoResponse.parseFrom(response)).isEqualTo(typedResponse); assertThatThrownBy(() -> invoke("fullservice.method")).isInvalidRequest();
CompletableFuture<byte[]> future=asFuture(invoker.invokeAsync(command)); future.cancel(true); assertActiveRawCallCancelledOnce();
```

- Verification contribution: exact generic API/wire/security parity.
- After this file: RED identifies absent API/raw marshaller/plan path.

#### File 2 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/generic/RpcGenericTargetCacheTest.java`

- Purpose: Prove normalized key sharing, max/LRU/idle eviction, concurrent single creation and close once.
- Symbols: tests mapped to Primary `TEST-069`.
- Repository evidence: no current generic cache; properties define exact bounds from Step 3.
- Dependencies and consumers: fake Strategy Factory/clock/plans, cache.
- Why now: Dynamic discovery lifetime must be bounded before public API.
- Contract/signature changes: cache resolves/creates normalized target+method plan and is AutoCloseable.
- Input/output and state mapping: normalized command key → entry(plan,strategy,lastAccess); eviction closes strategy/demand and removes LB state.
- Error and edge behavior: concurrent same key creates once; failed creation not cached; close/evict race closes once; no raw payload/key in cache key.
- Implementation pseudocode:

```java
runConcurrently(20, () -> cache.resolve(sameTarget)); assertThat(factory.createCount()).isOne();
resolve(entries(max+1)); assertThat(oldest.strategyCloseCount()).isOne();
advance(idleTimeout); cache.evictIdle(); assertThat(allIdleClosedExactlyOnce()).isTrue();
```

- Verification contribution: memory/subscription/channel reference bound proof.
- After this file: RED identifies missing bounded cache.

#### File 3 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/interceptor/RpcConsumerClientInterceptorTest.java`

- Purpose: Prove typed and generic targets receive the same framework identity/trace/invocation metadata without arbitrary caller headers.
- Symbols: existing typed tests plus generic invocation context case and shared invocation ID across retries.
- Repository evidence: current interceptor constructs metadata from `RpcContractDescriptor` and generates ID in constructor.
- Dependencies and consumers: expanded invocation context/interceptor factory, fake raw method.
- Why now: Generic must not bypass security/observability to avoid synthetic typed contracts.
- Contract/signature changes: interceptor supports explicit normalized service/group/version/invocation ID constructor/factory; old constructor retained.
- Input/output and state mapping: typed or generic invocation target → same allowlisted Metadata keys; existing valid trace preserved.
- Error and edge behavior: caller cannot inject Authorization via command; invocation ID stable across attempts; malformed trace regenerated.
- Implementation pseudocode:

```java
ClientInterceptor generic=RpcConsumerClientInterceptor.forTarget(target, processIdentity, invocationId);
startTwoAttempts(generic); assertThat(headers1.get(INVOCATION_ID)).isEqualTo(headers2.get(INVOCATION_ID));
assertThat(commandApiFields()).doesNotContain("metadata","authorization","host","port");
```

- Verification contribution: generic security/trace parity and same logical identity.
- After this file: RED shows typed-only interceptor construction.

#### File 4 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/generic/package-info.java`

- Purpose: Document restricted raw unary API and cache ownership.
- Symbols: package documentation.
- Repository evidence: target tree identifies generic as a stable new package.
- Dependencies and consumers: command/cache/invoker.
- Why now: Security and schema responsibility must be visible.
- Contract/signature changes: none.
- Input/output and state mapping: exact target+bytes → existing unary result bytes.
- Error and edge behavior: explicitly excludes JSON/Map, arbitrary metadata, endpoint addresses, streaming and mode fallback.
- Implementation pseudocode:

```java
/** Restricted raw-Protobuf unary invocation for callers that already own descriptors.
 * Targets are canonical Service/Method identities and fixed Gateway/Direct modes; framework metadata remains controlled.
 * Dynamic discovery/plan state is bounded and closed by RpcGenericTargetCache. */
```

- Verification contribution: static API/security review.
- After this file: generic package constraints are explicit.

#### File 5 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/generic/RpcGenericInvocation.java`

- Purpose: Define defensive public command values without credentials or addresses.
- Symbols: fixed mode/Direct identity/service/group/version/fullMethodName/payload/options/affinity key record or builder.
- Repository evidence: Primary §9.2.6 lists complete fields and rejects dot alias/arbitrary metadata.
- Dependencies and consumers: generic invoker/cache/reference resolver equivalents.
- Why now: Public input must be immutable/validated before cache keys.
- Contract/signature changes: additive public API; byte arrays defensive copied.
- Input/output and state mapping: caller-provided logical identity/options/raw bytes → normalized command; Direct requires biz/app/env.
- Error and edge behavior: exact one slash, service segment equality, safe segments, nonnull payload, timeout/retry bounds, no headers/host/credentials.
- Implementation pseudocode:

```java
compact { validateFixedModeAndTarget(); fullMethodName=canonicalServiceSlashMethod(fullMethodName,serviceName);
    requestPayload=Arrays.copyOf(requireNonNull(requestPayload), requestPayload.length); validateOptionsAndAffinity(); }
byte[] requestPayload() { return Arrays.copyOf(requestPayload, requestPayload.length); }
```

- Verification contribution: invalid identity/security reflection cases in File 1.
- After this file: generic request contract is complete and safe.

#### File 6 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/generic/RpcGenericTargetCache.java`

- Purpose: Bound dynamic target method plans and close their discovery demands.
- Symbols: cache key/entry, `resolve`, `evictIdle`, `close`.
- Repository evidence: Primary sets 256/10-minute defaults and requires exact close on eviction.
- Dependencies and consumers: properties, Strategy Factory generic-definition factory, LB factory/raw plan builder, ticker.
- Why now: Minimum GREEN for File 2.
- Contract/signature changes: internal/public runtime bean; no external persistence.
- Input/output and state mapping: normalized target+method+policy → one plan/strategy entry; LRU/access timestamp bounded.
- Error and edge behavior: single-flight creation, failure cleanup, concurrent eviction/use reference guard, close all once.
- Implementation pseudocode:

```java
Entry resolve(command) { Key key=Key.fromNormalized(command); Entry e=singleFlightCompute(key, k -> createEntry(RpcReferenceDefinition.generic(k.mode(),k.serviceIdentity(),k.directQuery()))); e.touch(now); evictOverflow(); return e.retain(); }
evictIdle() { removeIf(entry.lastAccess<=now-idle && entry.notInUse(), Entry::closeOnce); }
close() { accepting=false; entries.values().forEach(Entry::closeOnce); entries.clear(); }
```

- Verification contribution: File 2 GREEN and Consumer lifecycle hook.
- After this file: dynamic state/subscriptions are bounded.

#### File 7 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/generic/RpcGenericInvoker.java`

- Purpose: Validate commands, build raw unary descriptors/plans and delegate both completion shapes.
- Symbols: `invokeBlocking`, `invokeAsync`, raw marshaller and defensive output helpers.
- Repository evidence: Gateway raw byte marshaller uses InputStream bytes; Primary rejects a new wire service.
- Dependencies and consumers: cache/executor/interceptor factories/process identity.
- Why now: Minimum GREEN for File 1.
- Contract/signature changes: new public programmatic API.
- Input/output and state mapping: command → cache entry/plan → executor → defensive bytes/null/exception.
- Error and edge behavior: payload/message size protected by existing gRPC config; malformed Provider bytes/status propagate; async method returns before network response and cancellation reaches executor.
- Implementation pseudocode:

```java
byte[] invokeBlocking(command) { Entry e=cache.resolve(validate(command)); try { return defensive((byte[])executor.executeBlocking(e.plan(), command.requestPayload())); } finally { e.release(); } }
CompletionStage<byte[]> invokeAsync(command) { Entry e=cache.resolve(validate(command)); return bridgeAndRelease(executor.executeAsync(e.plan(),payload),e); }
MethodDescriptor<byte[],byte[]> rawMethod(fullName) { return unary(fullName, byteArrayMarshaller(), byteArrayMarshaller()); }
```

- Verification contribution: File 1 GREEN and typed/raw parity in Step 12.
- After this file: generic API reuses the full invocation core.

#### File 8 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/interceptor/RpcClientInvocation.java`

- Purpose: Represent typed or generic interceptor context without breaking typed factories.
- Symbols: target fields/raw flag or static factory; legacy four-argument constructor/accessors retained.
- Repository evidence: current record requires contract/method/request Message; external IdP factory consumes this API.
- Dependencies and consumers: interceptor factories, proxy plan/generic invoker.
- Why now: Generic security must reuse existing factory chain.
- Contract/signature changes: convert the internal record to a final immutable class only if needed; the old four-argument constructor and `contract/method/request/processIdentity` accessor descriptors remain valid, while additive target/raw accessors support generic calls.
- Input/output and state mapping: typed constructor derives target from descriptors; generic factory supplies normalized target/raw payload metadata without exposing arbitrary headers.
- Error and edge behavior: exactly one typed/raw shape; raw payload defensively copied/redacted; process identity required.
- Implementation pseudocode:

```java
RpcClientInvocation(typedFourArgs) { this(targetFrom(contract,method), contract, method, request, null, processIdentity); }
static RpcClientInvocation generic(GenericInvocation cmd, RpcProcessIdentity id) { return new RpcClientInvocation(targetFrom(cmd), null,null,null,cmd.payloadCopy(),id); }
private constructor { requireExactlyOne(request!=null, rawRequest!=null); validateTarget(); storeDefensiveCopies(); }
```

- Verification contribution: File 3 and existing IdP/RPC interceptor compatibility compile.
- After this file: request-aware factories can inspect a safe common target for both call shapes.

#### File 9 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/interceptor/RpcConsumerClientInterceptor.java`

- Purpose: Build framework metadata from explicit target and stable logical invocation ID.
- Symbols: legacy constructor retained; target constructor/static factory; metadata fields.
- Repository evidence: current constructor generates one ID and reads identity from contract.
- Dependencies and consumers: executor plan/proxy/generic; MetadataKeys.
- Why now: Generic must not fabricate descriptors and retries must share one ID.
- Contract/signature changes: additive construction path; existing behavior unchanged.
- Input/output and state mapping: service/group/version/process/invocation ID → allowlisted headers on every attempt.
- Error and edge behavior: validate target text/ID; do not overwrite valid trace; never accept caller metadata map.
- Implementation pseudocode:

```java
legacyConstructor(contract,id) { this(contract.serviceName(),contract.group(),contract.version(),id,UuidV7.simpleString()); }
forTarget(target,id,invocationId) { validateTargetAndInvocationId(); return new interceptor(...); }
metadataAtStart(existing) { putServiceIdentityAndStableInvocation(); addChildTraceOnlyIfMissingOrInvalid(existing); }
```

- Verification contribution: File 3 GREEN and generic credential/trace parity.
- After this file: typed/generic attempts share the same safe metadata path.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-rpc-starter -am -Dtest=RpcGenericInvokerTest,RpcGenericTargetCacheTest,RpcConsumerClientInterceptorTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0; canonical/raw/security/cache/cancel tests pass and old typed interceptor tests stay green.
- Failure returns to: Files 5–7 for command/cache/invoker; Files 8–9 for interceptor compatibility/security; any arbitrary metadata requirement returns to Spec.
- Completion criteria: generic sync/async calls use one executor/metadata chain, target state is bounded, raw bytes defensive and public API contains no credentials/address/fallback mode.
- Rollback: remove generic beans/API after callers stop using them; core typed runtime and wire remain unchanged; cache is in-memory only.
- Commit paths: egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/generic/RpcGenericInvokerTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/generic/RpcGenericTargetCacheTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/interceptor/RpcConsumerClientInterceptorTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/generic/package-info.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/generic/RpcGenericInvocation.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/generic/RpcGenericTargetCache.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/generic/RpcGenericInvoker.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/interceptor/RpcClientInvocation.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/interceptor/RpcConsumerClientInterceptor.java
- Commit: `feat(rpc): add bounded generic invocation`

### Step 10 — Replace duplicate JDK proxy paths with one CGLIB method-plan runtime and wire the Consumer graph

- Requirements: `REQ-001`, `REQ-002`, `REQ-003`, `REQ-004`, `REQ-005`, `REQ-011`, `REQ-014`, `REQ-015`, `REQ-016`, `REQ-017`, `REQ-018`, `REQ-023`
- Dependencies: Steps 7–9
- Baseline state: BPP chooses two proxy factories, typed proxies use JDK InvocationHandler/linear lookup, and new pool/strategies/executor/generic/coordinator are not Spring-wired.
- Observable outcome: one CGLIB factory builds a method→plan map at injection time, one interceptor dispatches blocking/async, BPP resolves one definition/strategy per field, and AutoConfig creates one shared pool/runtime/generic API/lifecycle.
- End state: duplicate Direct factory and JDK handler/test are deleted; legacy programmatic direct factory continues through compatibility constructor; no opposite-mode demand is installed.
- Test-first gate: Required — BPP/proxy tests fail on JDK class/old factories and AutoConfig tests fail because new runtime beans/order/conditional dependencies are absent.
- Ordered files:

#### File 1 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/proxy/EgonRpcReferenceBeanPostProcessorTest.java`

- Purpose: Define unified field resolution/injection, CGLIB type and per-mode demand isolation.
- Symbols: Gateway/Direct/same-contract/double-annotation/missing dependency/hash/fallback tests.
- Repository evidence: existing test already covers dual annotations and factory availability.
- Dependencies and consumers: real resolver/factory, fake Managers/ApplicationContext, proxy factory.
- Why now: It is the RED user-facing injection contract.
- Contract/signature changes: BPP receives resolver/Strategy Factory/single proxy factory; output remains assignable contract.
- Input/output and state mapping: bean field → definition→strategy→CGLIB proxy; strategy retained for lifecycle cleanup.
- Error and edge behavior: same field double annotation/missing Directory/invalid bean error contains bean+field; opposite manager calls zero.
- Implementation pseudocode:

```java
postProcess(beanWithGatewayAndDirectFields); assertCglib(bean.gateway); assertCglib(bean.direct);
verify(gatewayManager,times(1)).retainDemand(); verify(providerManager,times(1)).retain(exactQuery);
assertThatThrownBy(() -> postProcess(doubleAnnotated)).hasMessageContaining("bean").hasMessageContaining("field");
```

- Verification contribution: Primary `TEST-001`–`009`,`065`,`070` injection portions.
- After this file: RED shows old constructor/factory/JDK behavior.

#### File 2 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/proxy/RpcConsumerMethodInterceptorTest.java`

- Purpose: Prove O(1) plan lookup, local Object methods, blocking/async dispatch and CGLIB classloader behavior.
- Symbols: interceptor/factory tests mapped to Primary `TEST-057`–`060`,`065`.
- Repository evidence: old handler test covers Object methods/request validation/retry; executor now owns retries.
- Dependencies and consumers: fake executor, plan map, Echo contracts, Spring CGLIB.
- Why now: New proxy implementation needs focused RED separate from network.
- Contract/signature changes: CGLIB MethodInterceptor returns exact response or Stage; unsupported method fails contract error.
- Input/output and state mapping: method+args → direct map lookup → executor call; Object methods never enter executor.
- Error and edge behavior: null/wrong args invalid request; classloader/interface preserved; equals identity; no per-call reflection stream.
- Implementation pseudocode:

```java
EchoContract proxy=factory.create(definition,strategy); assertThat(ClassUtils.isCglibProxyClass(proxy.getClass())).isTrue();
proxy.echo(request); verify(executor).executeBlocking(samePlan,request);
proxy.toString(); proxy.hashCode(); proxy.equals(proxy); verifyNoMoreInteractions(executor);
```

- Verification contribution: exact CGLIB/O(1)/dispatch behavior.
- After this file: RED fails until factory/interceptor replace JDK handler.

#### File 3 — `DELETE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/proxy/RpcConsumerInvocationHandlerTest.java`

- Purpose: Remove tests for the superseded JDK handler after behavior is transferred to Files 1–2/executor tests.
- Symbols: delete `RpcConsumerInvocationHandlerTest`.
- Repository evidence: class asserts idempotent/Gateway-stage retry behavior explicitly superseded by Primary and Step 7.
- Dependencies and consumers: replacement coverage in `RpcInvocationExecutorTest` and `RpcConsumerMethodInterceptorTest`.
- Why now: Keeping it would enforce obsolete architecture/behavior.
- Contract/signature changes: test-only deletion; no coverage loss after replacements are GREEN.
- Input/output and state mapping: old assertions map one-to-one to executor/proxy tests before removal.
- Error and edge behavior: deletion occurs only after replacement tests pass in the same Step.
- Implementation pseudocode:

```text
verify every Object/request assertion exists in RpcConsumerMethodInterceptorTest;
verify every retry/status/deadline assertion exists in RpcInvocationExecutorTest;
delete the obsolete JDK InvocationHandler test file only after those selectors are GREEN.
```

- Verification contribution: prevents contradictory idempotent retry expectations.
- After this file: no test names the deleted handler architecture.

#### File 4 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/config/EgonRpcAutoConfigTest.java`

- Purpose: Lock one shared pool/executor/strategy factory/proxy/generic/coordinator and mode-conditional Manager graph.
- Symbols: ApplicationContextRunner scenarios gateway-only/direct-only/both/no reference/generic/lifecycle close.
- Repository evidence: current test validates conditional manager/two proxy factory beans.
- Dependencies and consumers: new runtime types, fake directories, properties.
- Why now: Spring wiring is the final RED consumer entry path.
- Contract/signature changes: one `RpcConsumerProxyFactory`; generic invoker bean when consumer enabled; no Direct proxy factory bean.
- Input/output and state mapping: properties+available Directory beans → runtime graph; reference demands determine Manager startup/DEGRADED.
- Error and edge behavior: no Guard required; missing required mode Directory yields stable injection error; context close drains resources; consumer disabled creates none.
- Implementation pseudocode:

```java
runner.withGatewayDirectory().run(ctx -> assertSingleRuntimeGraph(ctx, GATEWAY));
runner.withProviderDirectory().run(ctx -> assertDirectOnlyWithoutGatewayDemand(ctx));
runner.withBothDirectories().run(ctx -> assertThat(ctx).hasSingleBean(RpcConsumerChannelPool.class).hasSingleBean(RpcGenericInvoker.class));
```

- Verification contribution: AutoConfig topology and lifecycle ordering.
- After this file: RED identifies old bean graph/missing runtime wiring.

#### File 5 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/proxy/EgonRpcReferenceBeanPostProcessor.java`

- Purpose: Orchestrate validation/definition/strategy/single factory injection only.
- Symbols: constructor, `postProcessBeforeInitialization`, unified injection helper.
- Repository evidence: current BPP duplicates Gateway/Direct branches and directly registers Gateway demand.
- Dependencies and consumers: resolver, Strategy Factory, CGLIB factory.
- Why now: Minimum GREEN for File 1 after runtime components exist.
- Contract/signature changes: internal constructor/wiring changes; BeanPostProcessor behavior remains public injection contract.
- Input/output and state mapping: annotated field → validate contract → resolve definition → create fixed strategy → create proxy → set field.
- Error and edge behavior: on proxy creation failure closes strategy; all diagnostics wrap bean/field; reflection only at startup.
- Implementation pseudocode:

```java
forEachAnnotatedField(bean, field -> { descriptor=validator.validate(field.getType()); definition=resolver.resolve(field,descriptor);
  strategy=strategyFactory.create(definition); try { proxy=proxyFactory.create(descriptor,definition,strategy); set(field,bean,proxy); }
  catch (RuntimeException e) { strategy.close(); throw injectionFailure(beanName,field,e); } });
```

- Verification contribution: File 1 GREEN and one mode demand per field.
- After this file: BPP has no mode-specific proxy branch.

#### File 6 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/proxy/RpcConsumerProxyFactory.java`

- Purpose: Generate interface-only CGLIB proxies and immutable method plans.
- Symbols: final runtime constructor, `create(descriptor,definition,strategy)`, legacy constructor/create adapter.
- Repository evidence: current factory uses `Proxy.newProxyInstance`; external programmatic direct factory instantiates old constructor.
- Dependencies and consumers: descriptor/resolver strategy/LB/executor/interceptor factories/process identity.
- Why now: Minimum GREEN for File 2 while preserving current clients.
- Contract/signature changes: main create accepts compiled definition/strategy; old constructor and `create(Class,long)` retained/deprecated if appropriate.
- Input/output and state mapping: descriptor methods → plan map with policy/mode/interceptors/fallback → Enhancer implementing contract.
- Error and edge behavior: enhancer/classloader failure closes strategy at BPP; unsupported return rejected earlier; legacy path wraps one owned channel without cross-mode fallback.
- Implementation pseudocode:

```java
Map<Method,RpcInvocationPlan> plans=compilePlans(descriptor,definition,strategy,loadBalancers,interceptorFactories);
Enhancer enhancer=new Enhancer(); enhancer.setSuperclass(Object.class); enhancer.setInterfaces(new Class[]{descriptor.contractType()});
enhancer.setCallback(new RpcConsumerMethodInterceptor(descriptor,plans,executor)); return descriptor.contractType().cast(enhancer.create());
```

- Verification contribution: File 2/AutoConfig and old direct client tests.
- After this file: typed proxies are CGLIB with precompiled plans.

#### File 7 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/proxy/RpcConsumerMethodInterceptor.java`

- Purpose: Perform constant-time method dispatch and local Object behavior.
- Symbols: `intercept`, Object helpers/request validator.
- Repository evidence: old handler behavior is small; retry/network code moved to executor.
- Dependencies and consumers: descriptor, immutable plan map, executor.
- Why now: CGLIB callback minimum implementation.
- Contract/signature changes: new internal final interceptor; no public API.
- Input/output and state mapping: Method/args → plan → blocking or async executor result.
- Error and edge behavior: exactly one nonnull expected request type; Object methods identity-local; missing plan invalid contract; no fallback/retry logic here.
- Implementation pseudocode:

```java
if (method.getDeclaringClass()==Object.class) return objectMethod(proxy,method,args);
RpcInvocationPlan plan=requirePlan(plans.get(method)); Object request=requireSingleTypedArgument(args,plan);
return plan.invocationMode()==BLOCKING ? executor.executeBlocking(plan,request) : executor.executeAsync(plan,request);
```

- Verification contribution: File 2 GREEN and proves thin proxy layer.
- After this file: hot-path dispatch is one map lookup and executor call.

#### File 8 — `DELETE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/proxy/RpcDirectReferenceProxyFactory.java`

- Purpose: Remove duplicate Direct-only contract/query/proxy construction.
- Symbols: delete class.
- Repository evidence: its responsibilities move to DefinitionResolver, Strategy Factory and unified ProxyFactory.
- Dependencies and consumers: AutoConfig/BPP imports removed in Files 5/10.
- Why now: Final graph no longer needs two factories.
- Contract/signature changes: internal Spring bean/type removed; public annotation remains.
- Input/output and state mapping: query resolution maps to resolver; demand maps to Direct Strategy; proxy maps to unified factory.
- Error and edge behavior: verify repository has no external production consumers before deletion; missing usages block this deletion.
- Implementation pseudocode:

```text
rg all production references to RpcDirectReferenceProxyFactory;
replace AutoConfig/BPP construction with resolver -> DirectRpcReferenceStrategy -> RpcConsumerProxyFactory;
delete only when compilation proves no remaining consumer and Direct tests are GREEN.
```

- Verification contribution: static no-reference check and AutoConfig tests.
- After this file: Direct annotation uses the same proxy/runtime as Gateway.

#### File 9 — `DELETE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/proxy/RpcConsumerInvocationHandler.java`

- Purpose: Remove JDK Proxy and obsolete idempotent/Gateway-only retry loop.
- Symbols: delete class.
- Repository evidence: all behaviors move to MethodInterceptor/Executor; user requires CGLIB.
- Dependencies and consumers: ProxyFactory construction removed in File 6.
- Why now: Avoid two runtime semantics after replacement is test-covered.
- Contract/signature changes: internal class removed; proxies remain typed contract instances.
- Input/output and state mapping: request validation/Object methods → interceptor; retry/deadline/status → executor.
- Error and edge behavior: delete only after replacement selector passes; no compatibility alias keeps the obsolete handler active.
- Implementation pseudocode:

```text
verify RpcConsumerProxyFactory constructs RpcConsumerMethodInterceptor only;
verify Executor tests cover availability retry, deadline, cancellation, business-terminal and FAIL_OPEN null;
delete RpcConsumerInvocationHandler and confirm rg finds no production/test reference.
```

- Verification contribution: CGLIB-only static gate.
- After this file: no JDK dynamic proxy handler remains in annotation path.

#### File 10 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/config/EgonRpcAutoConfig.java`

- Purpose: Wire one Consumer runtime graph in dependency/lifecycle order.
- Symbols: beans for LB registry, pool, managers, resolver/strategy factory, executor, generic cache/invoker, proxy/BPP, coordinator; remove two-factory wiring.
- Repository evidence: current AutoConfig has conditional managers and two proxy beans; all inputs now exist.
- Dependencies and consumers: Spring ObjectProvider directories/interceptors, properties/process identity/ApplicationContext.
- Why now: Minimum GREEN for File 4 and actual feature entry point.
- Contract/signature changes: bean topology changes internally; consumer property prefix/enable flag unchanged.
- Input/output and state mapping: classpath/properties/directories → singleton graph; coordinator owns close hooks (`genericCache`, strategy factory, pool/managers).
- Error and edge behavior: missing optional mode Directory only fails when corresponding annotation creates demand; disabled consumer creates none; no Guard class references here.
- Implementation pseudocode:

```java
@Bean pool(factory,properties); @Bean loadBalancers(properties); @Bean managersIfDirectories(...);
@Bean strategyFactory(optionalManagers); @Bean executor(coordinator,pool,statusMapper); @Bean genericCacheAndInvoker(...);
@Bean proxyFactory(...); @Bean bpp(validator,resolver,strategyFactory,proxyFactory); @Bean coordinator(managers,pool,closeHooks);
```

- Verification contribution: File 4 GREEN and final Consumer component creation.
- After this file: Spring applications receive the new CGLIB/governance runtime.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-rpc-starter -am -Dtest=EgonRpcReferenceBeanPostProcessorTest,RpcConsumerMethodInterceptorTest,EgonRpcAutoConfigTest,RpcDirectClientFactoryTest -Dsurefire.failIfNoSpecifiedTests=false test && ! rg -n "Proxy\.newProxyInstance|RpcConsumerInvocationHandler|RpcDirectReferenceProxyFactory" egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/proxy egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/config/EgonRpcAutoConfig.java`
- Expected result: exit 0; CGLIB/runtime/legacy direct tests pass and static search returns no forbidden old proxy types.
- Failure returns to: Files 5–7 for injection/proxy; Files 8–9 only after replacements pass; File 10 for bean graph/lifecycle.
- Completion criteria: one CGLIB factory/interceptor/runtime graph serves both annotations and generic API, old programmatic direct clients compile, no JDK proxy or duplicate Direct factory remains.
- Rollback: revert Step 10 as one commit to restore old proxy activation while Steps 3–9 remain internal/unwired; no wire/data rollback.
- Commit paths: egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/proxy/EgonRpcReferenceBeanPostProcessorTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/proxy/RpcConsumerMethodInterceptorTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/proxy/RpcConsumerInvocationHandlerTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/config/EgonRpcAutoConfigTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/proxy/EgonRpcReferenceBeanPostProcessor.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/proxy/RpcConsumerProxyFactory.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/proxy/RpcConsumerMethodInterceptor.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/proxy/RpcDirectReferenceProxyFactory.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/proxy/RpcConsumerInvocationHandler.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/config/EgonRpcAutoConfig.java
- Commit: `refactor(rpc): activate cglib invocation runtime`

### Step 11 — Adapt Guard rate limits to Provider UNAVAILABLE and make Provider lifecycle state explicit

- Requirements: `REQ-007`, `REQ-010`, `REQ-011`, `REQ-012`, `REQ-013`, `REQ-020`, `REQ-021`
- Dependencies: Steps 1–2, 8, 10
- Baseline state: RPC has no Guard dependency/mapper; Provider lifecycle uses a boolean, starts Server before registration but exposes no DEGRADED/FAILED state; lease metadata does not default service weight.
- Observable outcome: optional class-safe AutoConfiguration maps only Guard `RATE_LIMITED` to UNAVAILABLE/provider/rate-limit before business invocation; Provider publishes explicit states, RPC-owned heartbeat recovery and graceful drain; registration emits explicit/default `gateway.weight`.
- End state: applications without Guard have no Guard/AOP/Redisson requirement; `@EgonRpcProvider` remains unchanged; all Provider lifecycle callbacks/state transitions are testable.
- Test-first gate: Required — optional context/mapper/AOP/lifecycle/lease tests fail because adapter/state/weight behavior does not exist.
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/config/RpcAccessGuardAutoConfigurationTest.java`

- Purpose: Prove optional class-safe bean creation and mapper ordering.
- Symbols: contexts with/without Guard class/bean, disabled RPC/provider, single mapper tests.
- Repository evidence: AutoConfig tests use ApplicationContextRunner; separate auto-config avoids main class linkage.
- Dependencies and consumers: optional Guard dependency, imports file, mapper.
- Why now: Classpath safety is RED before adding Maven dependency/wiring.
- Contract/signature changes: conditional mapper bean only when Guard API and RPC Provider are enabled.
- Input/output and state mapping: classpath/properties → zero/one `RpcAccessGuardExceptionMapper` exposed as `RpcProviderExceptionMapper`.
- Error and edge behavior: no Guard means startup success/no bean; custom mapper chain retained; no Redisson/AOP forced by RPC alone.
- Implementation pseudocode:

```java
withoutGuard.run(ctx -> assertThat(ctx).doesNotHaveBean(RpcAccessGuardExceptionMapper.class));
withGuardAndProvider.run(ctx -> assertThat(ctx).hasSingleBean(RpcAccessGuardExceptionMapper.class));
withGuardConsumerOnly.run(ctx -> assertThat(ctx).doesNotHaveBean(RpcAccessGuardExceptionMapper.class));
```

- Verification contribution: optional dependency/class-safe contract.
- After this file: RED identifies missing auto config/import.

#### File 2 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/provider/server/RpcAccessGuardExceptionMapperTest.java`

- Purpose: Define exact Guard decision→gRPC status/trailer mapping and delegation.
- Symbols: rate limited, other Guard decision, wrapped/non-Guard cases.
- Repository evidence: Provider mapper SPI returns Optional and existing tests assert ordered delegation.
- Dependencies and consumers: Guard outcomes/rejection exception, shared metadata keys.
- Why now: Adapter public failure behavior is RED.
- Contract/signature changes: only `RATE_LIMITED` maps; others return empty.
- Input/output and state mapping: `AccessGuardRejectedException.outcome.decision` → optional StatusRuntimeException with safe trailers.
- Error and edge behavior: no rule/key/payload/token in description/trailers; cause chain inspected only as approved; non-rate rejection not misclassified.
- Implementation pseudocode:

```java
var mapped=mapper.map(rejected(outcome(RATE_LIMITED))).orElseThrow(); assertThat(mapped.getStatus().getCode()).isEqualTo(UNAVAILABLE);
assertTrailer(mapped, FAILURE_STAGE,"provider"); assertTrailer(mapped, ERROR_TYPE,"rate-limit");
assertThat(mapper.map(rejected(outcome(DENIED)))).isEmpty(); assertThat(mapper.map(new RuntimeException())).isEmpty();
```

- Verification contribution: Primary `TEST-045` Adapter portion.
- After this file: RED fails on missing mapper.

#### File 3 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/provider/server/RpcProviderAccessGuardComponentTest.java`

- Purpose: Prove real Spring-proxied Provider implementation `@RateLimitGuard` rejects before business and maps async/sync identically.
- Symbols: Spring context, guarded Provider, counting target/fake Guard backend, real service definition/observer tests.
- Repository evidence: Guard Advisor resolves most-specific implementation method and supports CompletionStage; scanner retains proxy after Step 8.
- Dependencies and consumers: both auto-configurations, Provider scanner/server, Guard rule fixture.
- Why now: Cross-component AOP invocation is the acceptance RED for `REQ-013`.
- Contract/signature changes: no Provider marker fields; annotation stays on implementation method.
- Input/output and state mapping: RPC request→availability gate→Spring proxy→Guard decision; reject produces status/trailers and target count 0.
- Error and edge behavior: DRAINING gate rejects before Guard/permit; allowed then business error remains non-UNAVAILABLE mapper result; stage terminates once.
- Implementation pseudocode:

```java
invoke(serverDefinition(guardedSpringProxy), request); assertStatus(UNAVAILABLE); assertProviderRateTrailers();
assertThat(providerBusinessCalls).isZero(); assertThat(guardAcquireCalls).isOne();
markUnavailableBeforeInvoke(); assertThat(guardAcquireCalls).isUnchanged(); assertStatus(UNAVAILABLE);
```

- Verification contribution: Primary `TEST-044`,`047` and AOP/lifecycle ordering.
- After this file: RED shows absent adapter or proxy/advice bypass.

#### File 4 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/provider/lifecycle/RpcProviderLifecycleTest.java`

- Purpose: Assert explicit startup/recovery/drain/failure states and callback once.
- Symbols: tests mapped to Primary `TEST-031`–`037`.
- Repository evidence: current test covers bind/register/fail-fast/nonfailfast/stop ordering with fake Server/registry.
- Dependencies and consumers: new state enum, controllable lease manager/server/in-flight call.
- Why now: Lifecycle state is RED before replacing boolean.
- Contract/signature changes: `state()` exposed; SmartLifecycle behavior retained; callback stop supported.
- Input/output and state mapping: start/bind/register/heartbeat/stop events → exact state sequence and availability.
- Error and edge behavior: fail-fast→FAILED cleanup; nonfailfast→DEGRADED→READY; lost one lease→DEGRADED; DRAINING no recovery/new calls; timeout shutdownNow/callback once.
- Implementation pseudocode:

```java
lifecycle.start(); assertTransitions(STARTING,READY); assertReadyOnlyAfterAllLeases();
loseLeaseThenHeartbeat(); assertThat(state).isEqualTo(DEGRADED); recoverAll(); assertThat(state).isEqualTo(READY);
lifecycle.stop(callback); assertTransitions(DRAINING,STOPPED); assertThat(callbackCount).isOne();
```

- Verification contribution: graceful start/stop and heartbeat ownership proof.
- After this file: RED identifies boolean/no-callback/no-degraded behavior.

#### File 5 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/provider/registration/RpcProviderLeaseManagerTest.java`

- Purpose: Lock service weight metadata default/override and lease health summary for lifecycle state.
- Symbols: default annotation weight, configured metadata override, invalid weight, all-leases-active/recovery tests.
- Repository evidence: current test covers register/heartbeat/recovery/deregister and metadata constants.
- Dependencies and consumers: Provider binding annotation, metadata merger, lease manager.
- Why now: `gateway.weight` write side and lifecycle recovery need one tested owner.
- Contract/signature changes: additive `allPreparedLeasesActive`/counts if needed; registration interface unchanged.
- Input/output and state mapping: binding contract weight + configured metadata → registration metadata; leases map → aggregate health.
- Error and edge behavior: explicit 1–10000 wins; missing uses annotation/default100; invalid startup fails; stale heartbeat never marks available.
- Implementation pseudocode:

```java
manager.prepare(binding(serviceWeight(80)),host,port); assertMetadata("gateway.weight","80");
properties.metadata.put("gateway.weight","60"); prepare(); assertMetadata("gateway.weight","60");
heartbeatLoseOneOfTwo(); assertThat(manager.allPreparedLeasesActive()).isFalse(); recover(); assertThat(...).isTrue();
```

- Verification contribution: Primary weight write semantics and lifecycle state input.
- After this file: RED shows missing weight/default/aggregate state.

#### File 6 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/pom.xml`

- Purpose: Add Access Guard Starter as optional integration dependency.
- Symbols: Maven dependency with `${project.version}` and `<optional>true>`.
- Repository evidence: Guard is sibling component with no RPC dependency; RPC currently has no AOP/Redisson dependency.
- Dependencies and consumers: separate Guard AutoConfig/test compile; downstream apps opt in explicitly.
- Why now: Mapper can compile while transitive consumers remain unaffected.
- Contract/signature changes: published optional Maven dependency only.
- Input/output and state mapping: RPC artifact compile classpath includes Guard API; downstream dependency graph omits it unless explicitly requested.
- Error and edge behavior: no version drift/new external library; dependency cycle check via reactor.
- Implementation pseudocode:

```xml
<dependency><groupId>top.egon</groupId><artifactId>egon-cola-component-access-guard-starter</artifactId>
  <version>${project.version}</version><optional>true</optional></dependency>
<!-- Keep RPC main AutoConfig free of Guard method signatures. -->
```

- Verification contribution: dependency tree/context-without-Guard tests.
- After this file: optional adapter classes compile without forcing Guard on applications.

#### File 7 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/config/RpcAccessGuardAutoConfiguration.java`

- Purpose: Conditionally publish the Guard exception Adapter outside main AutoConfig.
- Symbols: AutoConfiguration class/mapper bean.
- Repository evidence: Spring Boot imports file and `@ConditionalOnClass` pattern are used across components.
- Dependencies and consumers: Guard rejection class, RPC provider enabled property, mapper SPI.
- Why now: Minimum GREEN for File 1 without classloading Guard in Guard-less apps.
- Contract/signature changes: new conditional auto-configuration only.
- Input/output and state mapping: Guard class + RPC/provider enabled → one ordered mapper bean.
- Error and edge behavior: missing class/property skips cleanly; custom same bean can override via `@ConditionalOnMissingBean`.
- Implementation pseudocode:

```java
@AutoConfiguration(afterName="top.egon.cola.component.accessguard.autoconfigure.AccessGuardAopAutoConfiguration")
@ConditionalOnClass(name="top.egon.cola.component.accessguard.api.AccessGuardRejectedException")
@ConditionalOnProperty(prefix="egon.cola.component.rpc.provider",name="enabled",havingValue="true")
@Bean @ConditionalOnMissingBean(RpcAccessGuardExceptionMapper.class) mapper() { return new RpcAccessGuardExceptionMapper(); }
```

- Verification contribution: File 1 GREEN and no-Guard startup.
- After this file: Guard integration is classpath/property isolated.

#### File 8 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- Purpose: Register the separate Guard adapter AutoConfiguration.
- Symbols: one class-name line.
- Repository evidence: file currently registers `EgonRpcAutoConfig` and is the Boot discovery authority.
- Dependencies and consumers: Boot auto-configuration import selector.
- Why now: Conditional class must be discoverable in real applications.
- Contract/signature changes: additive import; main config order retained.
- Input/output and state mapping: jar resource → Boot imports → conditional evaluation.
- Error and edge behavior: no duplicate line; class absence handled by condition; deterministic ordering by annotation.
- Implementation pseudocode:

```text
retain the existing EgonRpcAutoConfig import;
append top.egon.cola.component.rpc.config.RpcAccessGuardAutoConfiguration exactly once;
let @ConditionalOnClass/@ConditionalOnProperty decide whether a mapper bean is created.
```

- Verification contribution: ApplicationContextRunner uses actual imports.
- After this file: packaged starter discovers optional integration.

#### File 9 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/server/RpcAccessGuardExceptionMapper.java`

- Purpose: Adapt only Guard rate-limit rejection to safe gRPC status/trailers.
- Symbols: `map(Throwable)`.
- Repository evidence: existing ordered mapper SPI is exact extension point; Guard exception exposes structured outcome.
- Dependencies and consumers: optional Guard API, MetadataKeys/FailureStage.
- Why now: Minimum GREEN for Files 2–3.
- Contract/signature changes: new mapper implementation, no Guard→RPC dependency.
- Input/output and state mapping: rate decision → `Status.UNAVAILABLE` + provider stage + `rate-limit`; return Optional.empty otherwise.
- Error and edge behavior: sanitize description; do not map backend/internal/other decisions unless Guard outcome is exactly RATE_LIMITED.
- Implementation pseudocode:

```java
if (!(throwable instanceof AccessGuardRejectedException rejected) || rejected.outcome().decision()!=RATE_LIMITED) return Optional.empty();
Metadata trailers=new Metadata(); RpcFailureStage.PROVIDER.put(trailers); trailers.put(ERROR_TYPE,"rate-limit");
return Optional.of(Status.UNAVAILABLE.withDescription("RPC provider rate limited").asRuntimeException(trailers));
```

- Verification contribution: Files 2–3 GREEN and executor Direct classification.
- After this file: Provider Guard rejection has the approved transport contract.

#### File 10 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/lifecycle/RpcProviderRuntimeState.java`

- Purpose: Represent full Provider lifecycle states.
- Symbols: `NEW`, `STARTING`, `READY`, `DEGRADED`, `DRAINING`, `FAILED`, `STOPPED`.
- Repository evidence: Primary §10.6 requires states beyond current boolean.
- Dependencies and consumers: Provider lifecycle/tests/logs.
- Why now: Lifecycle transitions need an explicit authority.
- Contract/signature changes: new internal/public-observable enum.
- Input/output and state mapping: Server/lease/stop events → state.
- Error and edge behavior: DRAINING terminal for instance; READY only all required leases active.
- Implementation pseudocode:

```java
public enum RpcProviderRuntimeState { NEW, STARTING, READY, DEGRADED, DRAINING, FAILED, STOPPED }
boolean servingNewCalls() { return this==READY || this==DEGRADED; }
// Per-service availability still gates methods; DEGRADED never means all services are available.
```

- Verification contribution: File 4 exact transitions.
- After this file: Provider state is representable and testable.

#### File 11 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/lifecycle/RpcProviderLifecycle.java`

- Purpose: Enforce explicit start/recovery/drain state and callback semantics around existing scheduler/server.
- Symbols: atomic/volatile state, `start`, heartbeat wrapper, `stop(Runnable)`, `state`.
- Repository evidence: current order binds Server, prepares/registers, starts fixed-delay heartbeat, clears availability/deregisters/drains Server.
- Dependencies and consumers: lease health summary, availability, server/properties.
- Why now: Minimum GREEN for File 4 while reusing existing active heartbeat owner.
- Contract/signature changes: additive state/callback; SmartLifecycle interface preserved.
- Input/output and state mapping: STARTING after validation; READY all leases; DEGRADED nonfailfast/lease loss; FAILED startup cleanup; DRAINING gate then STOPPED.
- Error and edge behavior: interval<lease validation retained; no heartbeat after DRAINING; repeated stop/callback once; accepted calls drain, new calls fail availability before Guard.
- Implementation pseudocode:

```java
start() { transition(NEW_OR_STOPPED,STARTING); bindServer(); prepareLeases(); register(); state=leaseManager.allActive()?READY:DEGRADED; scheduleHeartbeat(this::heartbeatAndRefreshState); }
heartbeatAndRefreshState() { if (state==DRAINING) return; leaseManager.heartbeatAndRecover(); state=leaseManager.allActive()?READY:DEGRADED; }
stop(callback) { state=DRAINING; availability.clear(); disableRecoveryAndDeregister(); drainServerOrForce(); state=STOPPED; callbackOnce(); }
```

- Verification contribution: File 4/component Guard ordering GREEN.
- After this file: Provider graceful lifecycle and heartbeat state are explicit.

#### File 12 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/registration/RpcProviderLeaseManager.java`

- Purpose: Emit canonical weight metadata and expose aggregate prepared/active lease health.
- Symbols: `prepare` per-binding metadata, `allPreparedLeasesActive`, counts/log state.
- Repository evidence: current prepare receives bindings but `registrationMetadata` only receives identity; leases map already authoritative.
- Dependencies and consumers: `EgonRpcService.weight`, metadata merger/properties, Provider lifecycle.
- Why now: Minimum GREEN for File 5 and READY/DEGRADED calculation.
- Contract/signature changes: registry wire unchanged; additive health methods.
- Input/output and state mapping: configured metadata wins; else contract annotation weight → `gateway.weight`; prepared identities vs lease map → aggregate active.
- Error and edge behavior: validate 1–10000 before register; no duplicate metadata conflict; loss/removal immediately false; logs redact credentials.
- Implementation pseudocode:

```java
Map metadata=registrationMetadata(binding); metadata.putIfAbsent("gateway.weight", Integer.toString(binding.contract().contractType().getAnnotation(EgonRpcService.class).weight()));
validateWeight(metadata.get("gateway.weight")); registrations.put(service, registration(metadata));
boolean allPreparedLeasesActive() { return !registrations.isEmpty() && leases.keySet().containsAll(registrations.keySet()); }
```

- Verification contribution: File 5 GREEN, weighted discovery write/read chain and lifecycle recovery.
- After this file: Provider state and DDC weight write authority are complete.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-rpc-starter -am -Dtest=RpcAccessGuardAutoConfigurationTest,RpcAccessGuardExceptionMapperTest,RpcProviderAccessGuardComponentTest,RpcProviderLifecycleTest,RpcProviderLeaseManagerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0; no-Guard/with-Guard, mapper/AOP, Provider state/drain/heartbeat and weight tests pass.
- Failure returns to: Files 6–9 for optional integration; Files 10–12 for lifecycle/weight; Guard algorithm failures return to Steps 1–2.
- Completion criteria: Guard-less apps remain unchanged, limited calls are UNAVAILABLE with safe trailers and target count 0, Provider states/heartbeat/drain are deterministic, and DDC registration weight is valid.
- Rollback: disable/remove Guard annotation/rules before reverting adapter; Provider artifact rollback preserves DDC lease schema; no database/key deletion.
- Commit paths: egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/config/RpcAccessGuardAutoConfigurationTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/provider/server/RpcAccessGuardExceptionMapperTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/provider/server/RpcProviderAccessGuardComponentTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/provider/lifecycle/RpcProviderLifecycleTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/provider/registration/RpcProviderLeaseManagerTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/pom.xml egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/config/RpcAccessGuardAutoConfiguration.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/server/RpcAccessGuardExceptionMapper.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/lifecycle/RpcProviderRuntimeState.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/lifecycle/RpcProviderLifecycle.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/registration/RpcProviderLeaseManager.java
- Commit: `feat(rpc): integrate provider guard lifecycle`

### Step 12 — Close cross-module TCP/process compatibility and bilingual operational documentation

- Requirements: `REQ-002`, `REQ-003`, `REQ-004`, `REQ-005`, `REQ-006`, `REQ-007`, `REQ-008`, `REQ-009`, `REQ-010`, `REQ-011`, `REQ-012`, `REQ-013`, `REQ-014`, `REQ-015`, `REQ-016`, `REQ-017`, `REQ-018`, `REQ-019`, `REQ-020`, `REQ-021`, `REQ-022`, `REQ-023`
- Dependencies: Steps 1–11
- Baseline state: module tests cover units; shared Echo Proto has blocking Java contract only; existing TCP/process tests do not assert async/generic/multiplex/no-cross-mode runtime governance; READMEs describe older limitations.
- Observable outcome: isolated async fixture reuses the Echo unary descriptor, real loopback TCP proves blocking/async/generic/CGLIB/shared-Channel behavior, opt-in process IT proves same-mode isolation/reselection, and both components document exact configuration/failure/rollout/nullability contracts.
- End state: all 75 Primary test IDs map to executable gates or explicit user-controlled live proof; no Proto/Gateway/DDC/Flyway/frontend production file changes.
- Test-first gate: Required — new fixture/TCP/process assertions fail until the implemented runtime is fully wired; documentation static searches initially find obsolete statements.
- Ordered files:

#### File 1 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/README.md`

- Purpose: Document three algorithms, exact properties/state/storage/failure semantics and RPC usage prerequisite in English.
- Symbols: rate-limit configuration tables/examples/Redis key/rollback sections.
- Repository evidence: README already owns Guard rule/storage/failure-policy documentation.
- Dependencies and consumers: Step 1–2 behavior; operators/Provider owners.
- Why now: Documentation follows tested contracts.
- Contract/signature changes: no code; default remains TOKEN_BUCKET.
- Input/output and state mapping: YAML fields → token/leak/window semantics; Local/Redisson scope/TTL/retryAfter described.
- Error and edge behavior: invalid sliding config fail-fast; Redis outage follows configured failure policy; old suffix cleanup/rollback explicit.
- Implementation pseudocode:

```markdown
Add one table mapping TOKEN_BUCKET/LEAKY_BUCKET/SLIDING_WINDOW to capacity/refillTokens/refillPeriod/requestedTokens.
Document Local monotonic time versus Redis TIME, legacy token key and two suffix keys, TTL/config reset and capacity limit.
Show @RateLimitGuard on an implementation method and state that RPC integration requires explicitly adding both starters.
```

- Verification contribution: `REQ-012`,`REQ-022` English static gate.
- After this file: English Guard docs match runtime.

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/README.zh-CN.md`

- Purpose: Provide the exact Chinese mirror of File 1.
- Symbols: Chinese algorithm/config/storage/failure/rollback sections.
- Repository evidence: component maintains bilingual README pair.
- Dependencies and consumers: same implementation; Chinese reviewers/operators.
- Why now: Prevents language-specific contract drift.
- Contract/signature changes: none.
- Input/output and state mapping: same property/default/key mapping as English.
- Error and edge behavior: same fail-fast/outage/TTL limits, with no added semantics.
- Implementation pseudocode:

```markdown
逐项镜像英文三算法参数表与精确准入/重试等待语义；不得翻译成排队执行。
写明 Token Bucket 旧 key 不变、漏桶/滑动窗口 suffix、Redis TIME、TTL 与配置变更重置。
给出 Provider 实现方法 @RateLimitGuard 示例以及显式依赖 Guard Starter 的前提。
```

- Verification contribution: bilingual parity review.
- After this file: Chinese Guard docs are semantically identical.

#### File 3 — `MODIFY egon-cola-components/egon-cola-component-rpc/README.md`

- Purpose: Document fixed modes, discovery cache, LB, lifecycle, Guard mapping, sync/async/generic/multiplex/CGLIB/retry/nullability in English.
- Symbols: annotations/config/API/error/rollout/rollback sections.
- Repository evidence: RPC README currently documents Gateway/Direct/blocking baseline and properties.
- Dependencies and consumers: Steps 3–11 public/runtime contracts.
- Why now: Release documentation must reflect actual tested behavior.
- Contract/signature changes: no code; examples use exact APIs/names.
- Input/output and state mapping: annotation/property/generic command → mode/candidates/attempts/results/errors; Provider config → heartbeat/state.
- Error and edge behavior: business errors terminal, UNAVAILABLE-only reselection, no cross-mode, FAIL_OPEN null, business duplicate safety and optional Guard caveats explicit.
- Implementation pseudocode:

```markdown
Show Gateway and Direct annotations with identical policy fields and state that mode never changes after injection.
List five algorithms plus least-in-flight, weight/hash resolver rules, shared Channel and Consumer/Provider graceful lifecycle properties.
Document CompletionStage and canonical Service/Method byte API, Guard UNAVAILABLE mapping, retry classifier and nullable FAIL_OPEN migration.
```

- Verification contribution: Primary `TEST-009`,`068` English side.
- After this file: English RPC docs contain no superseded limitation/fallback/idempotent-gate text.

#### File 4 — `MODIFY egon-cola-components/egon-cola-component-rpc/README.zh-CN.md`

- Purpose: Mirror File 3 exactly in Chinese.
- Symbols: Chinese annotations/config/API/errors/release sections.
- Repository evidence: bilingual README is repository convention and user reviews in Chinese.
- Dependencies and consumers: same runtime.
- Why now: Completes reviewable operational contract.
- Contract/signature changes: none.
- Input/output and state mapping: same examples/defaults/errors as English.
- Error and edge behavior: explicitly says business condition failures remain errors and rate limit transport is UNAVAILABLE.
- Implementation pseudocode:

```markdown
镜像 Gateway/Direct 固定模式、同模式换节点、DDC event+全量对账、五算法与权重/Hash resolver。
镜像阻塞/CompletionStage/泛化 raw bytes、多路复用/CGLIB、Provider Guard 与优雅启停配置。
突出业务错误不重试不降级、幂等由业务保证、availability FAIL_OPEN 返回 null 的迁移风险。
```

- Verification contribution: Primary `TEST-068` Chinese side.
- After this file: bilingual RPC contract is aligned.

#### File 5 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract/src/main/java/top/egon/cola/component/rpc/test/contract/AsyncEchoRpc.java`

- Purpose: Provide an async Java contract over the existing generated Echo unary descriptor.
- Symbols: `AsyncEchoRpc#echoAsync(EchoRequest): CompletionStage<EchoResponse>`.
- Repository evidence: `EchoRpc` binds `EchoServiceGrpc.getEchoMethod`; no Proto change allowed.
- Dependencies and consumers: generated Echo types and Step 8 validator.
- Why now: Cross-module compile/TCP proof needs a real published test contract.
- Contract/signature changes: additive test artifact type only; wire method unchanged.
- Input/output and state mapping: EchoRequest → existing `Echo` unary → Stage EchoResponse.
- Error and edge behavior: not co-registered with blocking fixture in one Provider context; no second `.proto` method.
- Implementation pseudocode:

```java
@EgonRpcService(grpcClass=EchoServiceGrpc.class, group="default", version="1.0.0")
public interface AsyncEchoRpc { @EgonRpcMethod(name="Echo")
    CompletionStage<EchoResponse> echoAsync(EchoRequest request); }
```

- Verification contribution: generated descriptor/async compatibility tests.
- After this file: test artifact exposes approved async Java shape.

#### File 6 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract/src/test/java/top/egon/cola/component/rpc/test/contract/EchoGeneratedContractTest.java`

- Purpose: Prove blocking/async/generic all reference the same canonical generated method and no Proto additions.
- Symbols: async validation/full-name/descriptor count assertions.
- Repository evidence: current test verifies generated classes/full method.
- Dependencies and consumers: `AsyncEchoRpc`, validator, EchoServiceGrpc descriptor.
- Why now: Contract-level RED protects wire compatibility.
- Contract/signature changes: test-only assertions.
- Input/output and state mapping: both Java interfaces → one generated `egon.rpc.test.v1.EchoService/Echo` descriptor.
- Error and edge behavior: service method count remains one; dot/leading slash not accepted by generic validation.
- Implementation pseudocode:

```java
assertThat(EchoServiceGrpc.getServiceDescriptor().getMethods()).hasSize(1);
assertThat(validator.validate(AsyncEchoRpc.class).methods()).singleElement().extracting(RpcMethodDescriptor::fullMethodName).isEqualTo(canonical);
assertThat(canonical).isEqualTo("egon.rpc.test.v1.EchoService/Echo");
```

- Verification contribution: `REQ-015`,`REQ-016`,`REQ-021` wire proof.
- After this file: async addition cannot silently alter Proto.

#### File 7 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract/src/test/java/top/egon/cola/component/rpc/test/fixture/consumer/AsyncEchoRpcTestClient.java`

- Purpose: Provide an annotated CGLIB async Consumer fixture.
- Symbols: field `@EgonRpcReference AsyncEchoRpc`, `echoAsync`, proxy accessor.
- Repository evidence: blocking `EchoRpcTestClient` is the fixture precedent.
- Dependencies and consumers: async contract/request, TCP test.
- Why now: Component test needs real field injection.
- Contract/signature changes: test-only fixture.
- Input/output and state mapping: String → EchoRequest → Stage response through Gateway mode.
- Error and edge behavior: returned stage is not joined in fixture; cancellation remains caller-controlled.
- Implementation pseudocode:

```java
@EgonRpcReference(timeoutMs=3000,retries=1) private AsyncEchoRpc rpc;
CompletionStage<EchoResponse> echoAsync(String text) { return rpc.echoAsync(EchoRequest.newBuilder().setMessage(text).build()); }
AsyncEchoRpc proxy() { return rpc; }
```

- Verification contribution: CGLIB async TCP assertion.
- After this file: async Consumer can be started in isolated context.

#### File 8 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract/src/test/java/top/egon/cola/component/rpc/test/fixture/provider/AsyncEchoRpcTestProvider.java`

- Purpose: Provide a controllable async Provider fixture in an isolated context.
- Symbols: `@EgonRpcProvider`, future/latch/call count and async response.
- Repository evidence: blocking provider fixture records provider/invocation identity.
- Dependencies and consumers: async contract, invocation metadata, TCP test.
- Why now: Proves server stage completion/cancellation over real TCP.
- Contract/signature changes: test-only Provider.
- Input/output and state mapping: request/invocation context → controllable CompletableFuture response.
- Error and edge behavior: cancellation observable; never returns null; isolated to avoid duplicate same wire service.
- Implementation pseudocode:

```java
@EgonRpcProvider final class AsyncEchoRpcTestProvider implements AsyncEchoRpc {
  CompletionStage<EchoResponse> echoAsync(EchoRequest request) { calls.incrementAndGet(); active=new CompletableFuture<>(); return active; }
  void complete(String providerId) { active.complete(response(providerId)); }
}
```

- Verification contribution: real Provider async/cancel proof.
- After this file: isolated async server fixture is ready.

#### File 9 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract/src/test/java/top/egon/cola/component/rpc/test/mockgateway/RpcRuntimeGovernanceTcpTest.java`

- Purpose: Prove CGLIB blocking/async/generic parity and multiplexing over real loopback TCP.
- Symbols: separate blocking/async contexts, generic invoker, concurrent call and shutdown leak assertions.
- Repository evidence: `RpcTcpCallTest` builds real Netty Server/ManagedChannel and Mock Gateway; no InProcess transport.
- Dependencies and consumers: final AutoConfig/runtime, fixtures, in-memory registry, Mock Gateway.
- Why now: Cross-module acceptance after all unit contracts.
- Contract/signature changes: test only.
- Input/output and state mapping: Provider/Gateway/Consumer contexts + one endpoint key → typed/raw requests → same responses/invocation trace; pool factory count one.
- Error and edge behavior: 100 concurrent unary calls do not serialize; async returns before completion/cancel; all resources close; no direct Provider Channel for Gateway fixture.
- Implementation pseudocode:

```java
startRealTcpProviderGatewayConsumer(); assertCglib(asyncClient.proxy());
run100ConcurrentBlockingAndGenericCalls(); assertThat(sharedPool.channelCountFor(gatewayKey)).isOne(); assertAllResponses();
CompletionStage<?> pending=asyncClient.echoAsync("x"); assertNotDoneThenComplete(); cancelSecondCallAndAssertProviderContextCancellation(); closeAndAssertNoLeaks();
```

- Verification contribution: Primary `TEST-027`,`061`–`065` and multiplex acceptance.
- After this file: local real TCP proves final runtime, not live DDC/Redis.

#### File 10 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract/src/test/java/top/egon/cola/component/rpc/test/fixture/directconsumer/DirectEchoRpcTestClient.java`

- Purpose: Provide a process-test Consumer that has only a Direct Reference demand.
- Symbols: `@EgonRpcDirectReference(bizCode="test-biz",appCode="test-app")`, `echo`, proxy accessor.
- Repository evidence: process scope always uses test-biz/test-app and blank annotation env resolves current process env.
- Dependencies and consumers: EchoRpc/request, Direct Provider Directory AutoConfig, direct process application.
- Why now: Process isolation counters cannot be proven with the existing Gateway-only client.
- Contract/signature changes: test-only fixture; no production annotation change.
- Input/output and state mapping: process env + exact annotation target → `RPC_PROVIDER` query → Echo response.
- Error and edge behavior: contains no Gateway Reference field; retries/LB can be set explicitly for same-mode reselection.
- Implementation pseudocode:

```java
@EgonRpcDirectReference(bizCode="test-biz",appCode="test-app",retries=1,loadBalance=ROUND_ROBIN)
private EchoRpc rpc;
EchoResponse echo(String value) { return rpc.echo(EchoRequest.newBuilder().setMessage(value).build()); }
```

- Verification contribution: Direct-only Process IT and zero Gateway demand proof.
- After this file: a direct-only Consumer fixture exists without changing the Gateway fixture.

#### File 11 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract/src/test/java/top/egon/cola/component/rpc/test/fixture/directconsumer/RpcDirectTestConsumerApplication.java`

- Purpose: Start only the Direct fixture in a separate JVM and print structured one-shot result.
- Symbols: Spring Boot application/main mirroring current process Consumer.
- Repository evidence: `fixture.consumer.RpcTestConsumerApplication` owns the existing Gateway one-shot pattern.
- Dependencies and consumers: Direct client, admission test configuration, Process harness.
- Why now: Package-local component scan prevents accidentally creating the Gateway fixture/demand.
- Contract/signature changes: test-only main class.
- Input/output and state mapping: command-line DDC/RPC properties → context → Direct echo → structured provider/invocation/trace output → close.
- Error and edge behavior: on failure exits nonzero through uncaught startup/invocation exception; finally closes context; no secret output.
- Implementation pseudocode:

```java
@SpringBootApplication @Import(RpcTestAdmissionConfiguration.class) class RpcDirectTestConsumerApplication {
  main(args) { context=SpringApplication.run(...); try { response=context.getBean(DirectEchoRpcTestClient.class).echo(message); printStructured(response); }
  finally { context.close(); } }
}
```

- Verification contribution: real separate-JVM Direct path for Process IT.
- After this file: process harness can launch Direct without any Gateway bean/demand.

#### File 12 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract/src/test/java/top/egon/cola/component/rpc/test/process/RpcProcessIT.java`

- Purpose: Extend explicit live profile to two Providers/two Gateways, same-mode reselection/isolation and graceful drain.
- Symbols: process topology/counters/events for Primary `TEST-066`,`067` and lifecycle checks.
- Repository evidence: current Failsafe IT starts DDC Admin/Provider/Mock Gateway/Consumer JVMs with external Redis and unique namespace.
- Dependencies and consumers: user-supplied Redis, process apps/Mock Gateway instrumentation.
- Why now: Only separate JVM/live registry can prove end-to-end topology; it stays opt-in.
- Contract/signature changes: test harness/events only; production modules unchanged.
- Input/output and state mapping: registry leases/process failure injection → same-mode endpoint selection/counters/result/error/drain.
- Error and edge behavior: Direct failure never increments Gateway; Gateway exhaustion never opens Direct channel; business error one attempt; child stdout retained on failure; namespace-only cleanup.
- Implementation pseudocode:

```java
startDdcAndTwoProvidersAndTwoGateways(uniqueScope); runGatewayConsumer(firstGatewayUnavailable); assertSecondGatewayUsedAndDirectCounterZero();
runDirectConsumer(withArg("--egon.cola.component.rpc.identity.env="+env), firstProviderUnavailable); assertSecondProviderUsedAndGatewayCounterUnchanged();
sendBusinessError(); assertAttemptCount(1); gracefullyStopSelectedNode(); assertLeaseRemovalAndInFlightCompletion();
```

- Verification contribution: user-controlled live proof for no-cross-mode/discovery/heartbeat/drain; never run automatically by agent.
- After this file: process harness covers final topology when profile is explicitly invoked.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: default: `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-access-guard-starter,:egon-cola-component-rpc-starter,:egon-cola-component-rpc-ddc-adapter,:egon-cola-component-rpc-test-contract -am test`; static: `git diff --check && ! rg -n "Consumer only supports blocking|generic invocation is not supported|JDK Dynamic Proxy|retry only.*idempotent|fallbackToGateway|fallback-to-gateway|自动.*降级.*直连|自动.*改走.*Gateway" egon-cola-components/egon-cola-component-rpc/README.md egon-cola-components/egon-cola-component-rpc/README.zh-CN.md`; live only by user: `DDC_TEST_REDIS_HOST=127.0.0.1 DDC_TEST_REDIS_PORT=6379 ./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-rpc-test-contract -am -Pddc-live-test -Dit.test=RpcProcessIT verify`.
- Expected result: default/static exit 0; generated Echo descriptor still one unary method; optional live command exit 0 when user supplies Redis, otherwise explicitly unvalidated.
- Failure returns to: Files 1–4 for docs; Files 5–9 for contract/TCP; File 10 or owning earlier Step for process failure root cause.
- Completion criteria: affected module suite/TCP/static gates pass, all 23 requirements/75 tests are traced, no forbidden production scope changed, and live boundary is honestly reported.
- Rollback: docs/tests/fixtures can revert independently only if corresponding runtime Steps also roll back; runtime release rollback follows §9 below and never deletes Redis state broadly.
- Commit paths: egon-cola-components/egon-cola-component-access-guard-starter/README.md egon-cola-components/egon-cola-component-access-guard-starter/README.zh-CN.md egon-cola-components/egon-cola-component-rpc/README.md egon-cola-components/egon-cola-component-rpc/README.zh-CN.md egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract/src/main/java/top/egon/cola/component/rpc/test/contract/AsyncEchoRpc.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract/src/test/java/top/egon/cola/component/rpc/test/contract/EchoGeneratedContractTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract/src/test/java/top/egon/cola/component/rpc/test/fixture/consumer/AsyncEchoRpcTestClient.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract/src/test/java/top/egon/cola/component/rpc/test/fixture/directconsumer/DirectEchoRpcTestClient.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract/src/test/java/top/egon/cola/component/rpc/test/fixture/directconsumer/RpcDirectTestConsumerApplication.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract/src/test/java/top/egon/cola/component/rpc/test/fixture/provider/AsyncEchoRpcTestProvider.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract/src/test/java/top/egon/cola/component/rpc/test/mockgateway/RpcRuntimeGovernanceTcpTest.java egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract/src/test/java/top/egon/cola/component/rpc/test/process/RpcProcessIT.java
- Commit: `test(rpc): verify runtime governance compatibility`

## 8. Test, Validation, and Quality Gates

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| Baseline before each Step | repository root | `git status --short --branch && git rev-parse HEAD` | worktree/commit isolation | only known user-owned unrelated paths plus committed predecessors; no overlapping edit | stop execution and reconcile ownership | repository safety |
| RED Step 1 | repository root | focused selector for `GuardPlanValidatorTest,RateLimitAlgorithmStrategyFactoryTest,LocalRateLimitBackendTest` before production files | Guard Local | compile/behavior failure exactly for missing enum/factory/algorithms | Step 1 Files 1–3 fixtures if unrelated | `REQ-012`; module |
| GREEN Step 1 | repository root | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-access-guard-starter -am -Dtest=GuardPlanValidatorTest,RateLimitAlgorithmStrategyFactoryTest,LocalRateLimitBackendTest -Dsurefire.failIfNoSpecifiedTests=false test` | Guard Local | exit 0, token regression + leak/window/concurrency pass | Step 1 owning file | `REQ-012`; module |
| RED/GREEN Step 2 | repository root | Step 2 default command, then opt-in Redis command only when Docker is explicitly available | Guard Redisson | mock Lua/key gate always exit 0; live Redis all algorithms exit 0 or explicitly deferred | Step 2 key/backend | `REQ-012`,`REQ-013`; module/real disposable Redis |
| RED/GREEN Step 3 | repository root | Step 3 focused properties/LB selector | RPC contracts/LB | exit 0; every enum resolves, deterministic/statistical assertions pass | Step 3 policy or algorithm file | `REQ-001`,`REQ-008`,`REQ-009`,`REQ-020`; module |
| RED/GREEN Step 4 | repository root | Step 4 DDC directory/LB selector | endpoint projection | exit 0; both endpoint modes map 80/default100 and old query tests pass | Step 4 endpoint/adapter | `REQ-008`,`REQ-009`,`REQ-021`; module/mock DDC |
| RED/GREEN Step 5 | repository root | Step 5 pool/security selector | Channel ownership | exit 0; single creation/ref/in-flight/drain counts exact | Step 5 key/lease/pool | `REQ-011`,`REQ-017`; module/mock transport |
| RED/GREEN Step 6 | repository root | Step 6 resolver/strategy/manager/coordinator selector | discovery/fixed mode/lifecycle | exit 0; opposite Manager calls zero, revisions/expiry/ref counts/state order pass | Step 6 owning file | `REQ-001`–`REQ-006`,`REQ-011`,`REQ-023`; module/mock DDC |
| RED/GREEN Step 7 | repository root | Step 7 executor/status selector | logical invocation | exit 0; availability/business/deadline/cancel/null matrix passes | Step 7 plan/context/executor/mapper | `REQ-003`,`REQ-004`,`REQ-013`–`REQ-020`; module/mock gRPC |
| RED/GREEN Step 8 | repository root | Step 8 validator/scanner/server selector | typed async Provider | exit 0; exact generic types and observer terminal/cancel pass | Step 8 descriptor/binding/server | `REQ-014`,`REQ-015`,`REQ-018`,`REQ-021`; module |
| RED/GREEN Step 9 | repository root | Step 9 generic/cache/interceptor selector | generic API/security | exit 0; raw parity/invalid surface/cache bounds/metadata pass | Step 9 command/cache/invoker/interceptor | `REQ-005`,`REQ-009`,`REQ-015`–`REQ-017`,`REQ-020`,`REQ-021`; module |
| RED/GREEN Step 10 | repository root | Step 10 focused selector plus forbidden JDK/duplicate-factory `rg` | CGLIB/Spring graph | exit 0; one CGLIB runtime and legacy direct clients pass; forbidden search empty | Step 10 proxy/AutoConfig | `REQ-001`–`REQ-005`,`REQ-011`,`REQ-014`–`REQ-018`,`REQ-023`; module/context |
| RED/GREEN Step 11 | repository root | Step 11 Guard/lifecycle selector | Provider integration | exit 0; Guard-less/Guard AOP/mapping/state/heartbeat/weight pass | Step 11 optional config/mapper/lifecycle | `REQ-007`,`REQ-010`–`REQ-013`,`REQ-020`,`REQ-021`; component |
| Guard full regression | repository root | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-access-guard-starter -am test` | Access Guard module | exit 0, no admission/time-limit/storage regression | Steps 1–2 | Guard module, not production Redis |
| RPC core/adapters regression | repository root | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-rpc-starter,:egon-cola-component-rpc-ddc-adapter -am test` | RPC/adapter modules | exit 0; all tests pass | Steps 3–11 | RPC module/mock DDC |
| Real loopback TCP | repository root | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-rpc-test-contract -am -Dtest=EchoGeneratedContractTest,RpcTcpCallTest,RpcRuntimeGovernanceTcpTest -Dsurefire.failIfNoSpecifiedTests=false test` | generated Proto/TCP | exit 0; real Netty loopback, CGLIB, async/generic/shared Channel/no leaks | Step 12 or owning runtime Step | `REQ-014`–`REQ-018`,`REQ-021`; local TCP, not DDC |
| Combined affected reactor | repository root | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-access-guard-starter,:egon-cola-component-rpc-starter,:egon-cola-component-rpc-ddc-adapter,:egon-cola-component-rpc-test-contract -am test` | all affected modules | exit 0 and captured final reactor summary | owning Step from first failing test | all; static/module/TCP |
| Static scope/format/docs | repository root | `git diff --check`; Step 12 contradiction `rg`; `git diff --name-only <baseline>...HEAD` review | source/docs/scope | exit 0; no forbidden old wording; only inventory paths changed | owning Step or Plan correction | `REQ-021`,`REQ-022`; static |
| Live process IT | repository root/user environment | `DDC_TEST_REDIS_HOST=127.0.0.1 DDC_TEST_REDIS_PORT=6379 ./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-rpc-test-contract -am -Pddc-live-test -Dit.test=RpcProcessIT verify` | separate JVM/DDC/Redis/topology | user-run exit 0; no skipped profile; per-mode counters/reselection/drain pass | Step 12 harness or owning runtime Step | `REQ-003`–`REQ-007`,`REQ-010`,`REQ-011`,`REQ-019`,`REQ-020`; live boundary |
| Final Spec conformance | repository root | compare Primary §4/§14/§19 against commits/tests; run Plan strict validator | docs/trace | every requirement/TEST ID has implementation evidence or explicit live boundary | affected Step/Spec | all |

RED runs are recorded only while executing the Plan; a compile failure for a deliberately absent new type is acceptable only when it matches the Step's stated RED reason. A lost/terminated Maven handle, a skipped opt-in integration test, a static compile, or a mock snapshot is never reported as live DDC/Redis/gRPC process proof.

## 9. Migration, Compatibility, Rollout, and Rollback

### 9.1 Persistence and generated-code disposition

- Relational database/Flyway：N/A. Primary §11 proves no table/entity/DAO/schema change; no existing or new migration file may be touched.
- Protobuf/generated Java：wire schema unchanged. `echo_service.proto` and generated sources are not edited; `AsyncEchoRpc` is a Java-only contract bound to the existing unary descriptor.
- DDC Registry：no key/topic/API/reconcile scheduler change. Existing event+full-pull/lease behavior is reused.
- Access Guard Redis：no migration/backfill. Token Bucket keeps the exact existing HASH key; Leaky/Sliding create lazy suffixed keys and old/inactive state expires by existing idle TTL.
- Consumer/Provider runtime state is process memory only and disappears on restart.

### 9.2 Compatibility windows and rollout order

1. Merge/publish Access Guard algorithms and RPC Starter/DDC Adapter in one platform component version after Steps 1–12 gates pass. Guard default remains TOKEN_BUCKET, so merely upgrading does not create new Redis key types.
2. Upgrade Provider applications first with retries irrelevant and Guard disabled/Token Bucket unchanged. Verify Provider state reaches READY only after all DDC leases and heartbeat interval is lower than lease duration.
3. Providers opting into rate limiting explicitly add Access Guard Starter and annotate implementation methods. Verify Spring proxy/advisor is active and observed rejection is `UNAVAILABLE` with Provider stage; start with conservative limits.
4. Async Java Contract changes are deployed as a coordinated Contract+Provider+Consumer version because Java binary signatures change, although Proto does not.
5. Upgrade Consumers with `retries=0`/effective inherited Direct zero retries first. Verify CGLIB, discovery cache, weights and shared Channel counts before enabling attempt replay.
6. Enable retries per call site only after the business owner proves stable unique business key/upsert/overwrite behavior. Framework idempotent metadata is not a gate.
7. Enable WEIGHTED_RANDOM/SWRR only after instance `gateway.weight` distribution is verified; enable CONSISTENT_HASH only with a deterministic named resolver/explicit generic affinity key.
8. Enable Leaky/Sliding Guard rules after Local/Redis capacity tests. Monitor Guard rejection/backend latency plus RPC same-mode retry amplification.
9. Generic API consumers must deploy descriptor ownership/size validation and handle nullable FAIL_OPEN before use; no public endpoint should expose arbitrary generic invocation without a separate security design.

### 9.3 Rollback and forward-fix

- Consumer rollback to the prior Starter is wire-safe for blocking typed contracts. New async Java Contract artifacts must roll back together across Contract/Provider/Consumer.
- Disable per-reference retries (`retries=0`) before runtime rollback when duplicate execution is suspected. There is no cross-mode fallback flag or state to clean.
- If shared pool/CGLIB/lifecycle causes a runtime issue, roll back the application artifact/restart process; no persisted pool/cache state exists.
- Before rolling Guard artifact back, change LEAKY_BUCKET/SLIDING_WINDOW rules to TOKEN_BUCKET or disable them. Never broad-delete Redis; suffixed keys expire naturally and the legacy token key remains readable.
- `FAIL_OPEN=null` callers that fail null handling should first change strategy to FAIL_CLOSED or LOCAL_FALLBACK, then redeploy/forward-fix caller code.
- Provider drain failure rollback is artifact restart; DDC TTL removes abandoned leases if deregistration did not finish.
- Gateway/DDC/IdP/DB/frontend rollback is N/A because their production code/schema is unchanged.

### 9.4 Pre/post deployment evidence

Pre-deploy: exact artifact versions, no unsupported async mixed version, business duplicate-safety evidence for every retries>0 call site, Guard rule/storage/key scope review, resolver bean existence, Provider weight range, container/system termination grace greater than RPC drain timeouts.

Post-deploy: Provider/Consumer state transitions, DDC endpoint counts/revisions, Channel count/drain count, retry attempts by fixed mode, Guard decision/storage/backend failures, Direct pure-limit versus mixed error code, null fail-open handling, business duplicate audit. Production P99, Redis contention/eviction, Pub/Sub loss, HTTP/2 stream cap and Kubernetes/systemd signal grace remain user-operated runtime evidence.

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| `REQ-001` | Primary §4, §9.2.1–§9.2.2 | Steps 3,6,10 | annotations, policy/definition/resolver, BPP/proxy | definition/BPP/AutoConfig | both annotations resolve one common policy |
| `REQ-002` | Primary §4, §9.2.1 | Steps 6,10,12 | Gateway Strategy/Manager, BPP, Process IT/docs | `TEST-001`,`008`,`009`,`067`,`068` | Gateway demand/opposite Direct counter zero |
| `REQ-003` | Primary §4, §9.2.2 | Steps 6,7,10,12 | Direct Strategy/Manager, executor, BPP, Process IT | `TEST-004`–`008`,`066` | exact Provider mode and Gateway counter zero |
| `REQ-004` | Primary §4, §7.3.4 | Steps 6,7,10,12 | Strategy/LB/executor/process harness | `TEST-005`–`008`,`051`,`066`,`067` | distinct same-mode candidates then mode error |
| `REQ-005` | Primary §4, §7.3.1 | Steps 6,9,10,12 | Managers/strategies/generic cache/AutoConfig | `TEST-010`–`013`,`069`,`070` | one subscription/cache per query; no call-time pull |
| `REQ-006` | Primary §4, §7.3.4 | Steps 4,6,12 | DDC directories/managers/process | `TEST-013`–`015` | old revision/expired lease excluded |
| `REQ-007` | Primary §4, §10.3 | Steps 11,12 | Provider lifecycle/lease/docs/process | `TEST-031`–`035` | RPC-owned fixed-delay heartbeat/recovery |
| `REQ-008` | Primary §4, §7.3.1 | Steps 3,4,6,12 | LB factory/enum/endpoints/Managers/docs | `TEST-016`–`025`,`068` | five algorithms plus least-in-flight exhaustive |
| `REQ-009` | Primary §4, §9.2.8, §10 | Steps 3,4,7,9,11,12 | resolver/LB/DDC endpoints/generic/lease/docs | `TEST-017`,`019`–`023`,`074` | weight parity and stable redacted affinity |
| `REQ-010` | Primary §4, §10.6 | Steps 11,12 | Provider state/lifecycle/process/docs | `TEST-031`–`033` | READY only after Server+all leases |
| `REQ-011` | Primary §4, §10.6 | Steps 5,6,7,10,11,12 | pool/Consumer coordinator/executor/Provider lifecycle | `TEST-028`–`030`,`036`–`038`,`072` | gate closes, in-flight drains or forces once |
| `REQ-012` | Primary §4, §9.2.3, §11 | Steps 1,2,11,12 | Guard algorithms/backends, RPC optional integration/docs | `TEST-039`–`048`,`073`–`075` | three Local/Redisson algorithms and AOP method guard |
| `REQ-013` | Primary §4, §9.2.3 | Steps 2,7,11,12 | Redis backend/status/Guard mapper/component/process | `TEST-044`,`045`,`047`,`051`,`073`,`075` | UNAVAILABLE/provider/rate-limit and target not called |
| `REQ-014` | Primary §4, §9.2.4 | Steps 7,8,10,12 | executor/descriptors/validator/CGLIB/TCP | `TEST-057`,`065` + existing Echo | blocking typed compatibility |
| `REQ-015` | Primary §4, §9.2.5/§9.2.7 | Steps 7,8,9,10,12 | executor/server/generic/CGLIB/async fixtures | `TEST-047`,`053`,`058`–`063` | immediate cancellable single-terminal Stage |
| `REQ-016` | Primary §4, §9.2.6–§9.2.7 | Steps 9,10,12 | generic command/cache/invoker/interceptors/TCP | `TEST-062`–`064`,`069` | canonical raw unary parity/security/bounds |
| `REQ-017` | Primary §4, §7.3.1 | Steps 5,7,9,10,12 | pool/leases/executor/generic/TCP | `TEST-026`–`030`,`069` | one Channel per key, concurrent streams, clean drain |
| `REQ-018` | Primary §4, §13 | Steps 8,10,12 | descriptors/proxy factory/interceptor/TCP | `TEST-001`,`057`,`058`,`065` | CGLIB class and O(1) method plan |
| `REQ-019` | Primary §4, §7.3.3 | Steps 7,12 | executor test/docs/process | `TEST-049`,`055`,`066` | idempotent=false still retries when configured |
| `REQ-020` | Primary §4, §7.3.4 | Steps 3,7,9,11,12 | policy/executor/status/generic/Guard/docs | `TEST-050`–`056`,`060`,`063`,`066`,`067` | business terminal; availability-only fail strategy/null |
| `REQ-021` | Primary §4, §16 | Steps 4,8,9,11,12 | compatibility constructors/optional POM/test contract/static gate | `TEST-023`,`024`,`035`,`057`,`062`,`064` | no Proto/DDC/Gateway/DB production change |
| `REQ-022` | Primary §4, §14–§16 | Step 12 | four READMEs and final gates | `TEST-009`,`068`; combined reactor/static | bilingual docs and affected suite exit 0 |
| `REQ-023` | Primary §4, §10.6 | Steps 6,10,12 | Consumer state/coordinator/AutoConfig/process | `TEST-070`–`072` | infrastructure before gate; failure/drain cleanup |

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| `RISK-001` | Retry can duplicate committed non-idempotent business effects | Step 7 executor, Step 12 docs/process | Primary `DEC-010`, current unknown-result window | business owner | Accepted user risk; require unique business key/upsert proof before retries>0 |
| `RISK-002` | Shared pool/cancel/drain races can leak or double-close Channel | Steps 5–7 | current managers own independent channels; new ref/in-flight model | RPC implementer | Mitigated by barrier tests, terminal CAS and force-close count |
| `RISK-003` | CGLIB may not support unplanned AOT/native/module constraints | Step 10 proxy | Primary marks AOT/native as non-goal | deployment owner | Non-blocking for current Java 21 JVM; add separate Spec if native deployment is required |
| `RISK-004` | Sliding Window exact entries can consume heap/Redis memory | Steps 1–2 Guard | capacity×rule×key cardinality | Guard/operator | Mitigated by capacity 100000, maxEntries, idle TTL and capacity tests; production sizing required |
| `RISK-005` | Rate-limit-as-UNAVAILABLE may amplify a hot request across instances | Steps 7,11,12 | same-mode reselection is explicitly required | service/operator | Bound by retries/distinct candidates/deadline; monitor Guard/backend/attempt metrics |
| `RISK-006` | Gateway strips Provider error-type, so application code differs by mode | Steps 7,11 | current Gateway safe trailer reconstruction | platform owner | Closed by Primary contract: Direct pure limit=`RPC_RATE_LIMITED`, Gateway=`RPC_PROVIDER_UNAVAILABLE` |
| `RISK-007` | Generic caller can send schema-invalid bytes | Step 9/12 | raw API deliberately lacks Java Contract | generic caller owner | Accepted by design; canonical identity/size/security and runtime error tests required |
| `RISK-008` | Async Provider may ignore cancellation if Stage is not Future/context-aware | Step 8/12 | Java CompletionStage has no mandatory cancel | Provider owner | Cancel Future when supported; document gRPC Context observation and test both paths |
| `RISK-009` | Dirty worktree/concurrent changes may overlap future implementation | every Step | current staged Gateway deletion/untracked files | executing agent/user | Recheck before every Step; path-limited commits; stop on overlap |
| `RISK-010` | Review Spec is not Accepted | all implementation Steps | Primary metadata and user requested Plan only | user | Plan may be reviewed; execution waits for explicit approval/Spec acceptance |

No unresolved public-behavior decision blocks Plan review. Runtime capacity, Docker availability, live DDC/Redis topology and business duplicate-safety are verification/deployment responsibilities, not permission to infer different behavior.

## 12. Review and Acceptance

### 12.1 Plan completeness review

- Exactly one primary Spec is linked; its Review status/revision and all effective amendments/dependencies are recorded.
- All `REQ-001`–`REQ-023` appear in Step Requirements lines and the traceability matrix; all 75 Primary tests map to focused/component/TCP/process gates.
- Every affected file in §5 has one owning Step and one commit boundary. `GuardPlanValidator` and generic interceptor context additions are evidence-backed, semantic-preserving clarifications.
- RED tests precede production files in every behavior-changing Step; each Step defines exact cwd, command, failure return, completion, rollback and commit paths.
- No implementation source/test, service, browser, database, migration or external system was changed/run while writing this Plan. Only read-only source inspection, existing baseline Maven tests and documentation edits occurred.

### 12.2 Original-request fidelity

The Plan preserves all user corrections: mode is configured client-side and never switches; failed availability attempts reselect only same-mode instances; business-condition errors remain errors; Guard owns Provider limiting and maps rejection to UNAVAILABLE; blocking/CompletionStage/generic/multiplex/CGLIB are explicit; canonical generic identity is `fully.qualified.Service/Method`; retries never inspect idempotent; FAIL_OPEN returns null. Strategy, Factory, Adapter, Observer reuse and explicit State models are used only at real variation/ownership points.

### 12.3 Repository and proof boundary

Paths/symbols/build selectors were refreshed at `main@0aa6673d`. A baseline focused reactor command actually passed with exit 0 for existing `LocalRateLimitBackendTest`, `RpcConsumerInvocationHandlerTest` and `DdcRpcProviderDirectoryTest`. This proves the selector/toolchain baseline only. The Plan does not claim the future implementation, live Redis algorithms, live DDC events/leases, separate JVM topology, production HTTP/2 multiplex limits or business idempotency have passed.

### 12.4 Approval gate and final verdict

`PASS — Ready for user review`

Because the primary Spec remains `Review`, this Plan also remains `Review`. User approval must first mark/confirm the effective Spec and Plan for execution; only then may `$egon-coding-executing-plan` implement Step 1 through Step 12 one at a time with each defined commit and final Spec conformance audit.
