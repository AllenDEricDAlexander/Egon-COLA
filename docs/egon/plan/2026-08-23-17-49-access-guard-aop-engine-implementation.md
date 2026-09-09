# Access Guard AOP 单入口、Engine 拆分与策略重构实施计划

| Field | Value |
| --- | --- |
| Document | `2026-08-23-17-49-access-guard-aop-engine-implementation.md` |
| Template Version | `2` |
| Status | `Review` |
| Created | `2026-08-23 17:49 CST` |
| Updated | `2026-08-23 17:49 CST` |
| Owner | `Egon-COLA xingyuan owner / User` |
| Repository | `Egon-COLA` |
| Scope | `egon-cola-component-bytecode 中 Access Guard Agent vertical slice；egon-cola-component-access-guard-starter 的 AOP-only API、typed policy、Engine split；两组件中英文 README；不改其他模块` |
| Source Requirement | `2026-08-23 用户决定删除 Guard Agent、只保留 AOP、拆分 Engine、重构策略并更新文档；随后显式调用 egon-coding-writing-plan` |
| Baseline Revision | `main@772df2b28b4abf29e7fffae6ea1b10fd616397b4；2026-08-23 17:49 CST dirty-worktree snapshot` |
| Implements Spec | [Access Guard AOP 单入口、Engine 拆分与策略管线重构规格](../spec/2026-08-23-16-56-access-guard-aop-engine-strategy-refactor.md) |
| Spec Status | `Review` |
| Spec Revision | `Updated 2026-08-23 16:56 CST；main@772df2b28b4abf29e7fffae6ea1b10fd616397b4；当前未提交 Review 文档` |
| Effective Specs | [主规格](../spec/2026-08-23-16-56-access-guard-aop-engine-strategy-refactor.md)；[Access Guard V2 整改设计](../../superpowers/specs/2026-07-29-access-guard-v2-remediation-design.md)；[Access Guard Component Design](../../superpowers/specs/2026-07-08-access-guard-component-design.md)；[Bytecode Enhancement Design](../../superpowers/specs/2026-07-15-bytecode-enhancement-design.md) |
| Depends On Plans | `None` |
| Supersedes | `None` |
| Superseded By | `None` |
| Related Plans | [历史 Access Guard V2 Remediation Plan](../../superpowers/plans/2026-07-29-access-guard-v2-remediation.md)；[历史 Bytecode Access Guard Agent Plan](../../superpowers/plans/2026-07-16-bytecode-access-guard-agent.md) |

## 1. Summary

本计划实现唯一主规格，共 5 个严格顺序的语义提交：Step 1 原子删除 Bytecode Access Guard Agent 的 bridge/core/runtime/agent/starter/test/dependency 切片并把 Bridge protocol 提升到 2.0；Step 2 删除 Guard 侧 AGENT/constructor 入口，只留下 AOP 自动入口与 programmatic Client；Step 3 把四项 admission policy 改为 `GuardPolicyType` 驱动的 typed Strategy；Step 4 以 `GuardAdmissionPipeline` 和 `GuardExecutionCoordinator` 拆分 `DefaultGuardEngine`；Step 5 同步四份中英文 README 和迁移说明。

每个行为 Step 先落 focused RED contract，再完成最小 GREEN、模块验证和 path-limited commit。最终证据包括 Guard 146-test 基线对应的全模块回归、Bytecode retained-capability focused tests、真实短进程 premain/Invoker `verify`、RPC Guard AOP 上下文测试、dependency tree、静态零引用与 `git diff --check`。本计划不实施代码、不启动服务、不执行数据库迁移；由于主 Spec 尚为 `Review`，计划仅进入 `Review`，不能直接视为获准实施。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path：[主规格](../spec/2026-08-23-16-56-access-guard-aop-engine-strategy-refactor.md)
- Status：`Review`
- Revision：`Updated 2026-08-23 16:56 CST`，baseline `main@772df2b28b4abf29e7fffae6ea1b10fd616397b4`
- Approval evidence：用户在收到 Review-ready Spec 后显式调用 `egon-coding-writing-plan`，授权编写一份 `Review` Plan；这不等于接受 Spec 或批准实施。

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [AOP-only Guard refactor](../spec/2026-08-23-16-56-access-guard-aop-engine-strategy-refactor.md) | Review；2026-08-23 16:56 CST | 全文，尤其 §4、§7–§10、§13–§16、§19–§20 | 唯一主目标，定义全部 `REQ-001`–`REQ-013`、target tree、internal contracts 与迁移 |
| Normative dependency | [Access Guard V2 整改设计](../../superpowers/specs/2026-07-29-access-guard-v2-remediation-design.md) | legacy 待审核；当前源码已落地其主体 | §9–§19 中除被主规格删除的 Agent/constructor 条款外：AOP、Client、plan/store/failure/time/reactive/observability | 主规格 `Depends On` 明确保留这些运行合同 |
| Amended predecessor | [Access Guard Component Design](../../superpowers/specs/2026-07-08-access-guard-component-design.md) | legacy implemented predecessor | §22：Strategy/Facade/Adapter 与“不引入宽泛 factory hierarchy”；被后续 V2 替换的旧 API/顺序均排除 | 证明 Spring auto-configuration 是 composition root，不新增 Abstract Factory |
| Partially replaced predecessor | [Bytecode Enhancement Design](../../superpowers/specs/2026-07-15-bytecode-enhancement-design.md) | legacy implemented predecessor | §5–§13、§15–§21、§23–§25 中 Executor/Observation/Method Extension、JDK-only bridge、premain、privacy、retained tests；排除 §14 Access Guard Agent 和相关顺序/验收 | 约束删除 Guard 时不得改变三项 retained capability、ClassLoader 与发布边界 |

### 2.3 Superseded or excluded content

- Bytecode Design §2.13/§2.14 中 Access Guard AOP/Agent 双引擎与 Method Extension→Access Guard→Observation 顺序、§14、§20.7、Stage 5、Guard acceptance 已由主规格删除；Method Extension→Observation→business 的 retained 路径继续有效。
- Access Guard V2 §9.1 constructor target、§17.1 AGENT、§17.3/§17.4、§21 Agent tests 和相关验收被主规格定向替换；§9–§19 的非 Agent 合同继续有效。
- Access Guard Component Design 的旧白名单优先顺序、旧注解和旧模块结构已被 V2/current source 替换；本计划只继承 §22 的模式边界。
- RPC runtime governance 仅为 related context，RPC 源码不进入 write scope；只运行现有 component tests。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| `REQ-001` | 主规格 §4 | 删除 Access Guard Agent 入口 | Guard 无 AGENT/integration/constructor entry；Bytecode 无 `ACCESS_GUARD` | Steps 1–2 删除 API、bridge、enhancer、adapter、tests |
| `REQ-002` | 主规格 §4 | AOP 是唯一自动拦截入口 | `@AccessGuard` target 仅 TYPE/METHOD；默认/显式 AOP 只有一个 Advisor | Step 2 annotation/config/validator tests |
| `REQ-003` | 主规格 §4 | 保留 programmatic 和 DISABLED | `AccessGuardClient` 可用；DISABLED 无 Advisor | Steps 2、4 保持 API 与 contract tests |
| `REQ-004` | 主规格 §4、§7.3 | Engine 拆为 Facade/admission/execution | `DefaultGuardEngine` 最终只组合两个 collaborator；阶段可独测 | Step 4 三个新 core type、Facade、wiring |
| `REQ-005` | 主规格 §4、§7.3.2 | 主策略类型化 | `GuardPolicy#type/evaluate` 无泛型 config；Engine/Pipeline 无 ID switch/unchecked cast | Step 3 typed contracts/implementations |
| `REQ-006` | 主规格 §4、§7.3.2 | 固定安全顺序与 typed bypass | exact DENY→ALLOW→PENALTY→RATE；deny 不可 bypass；缺失/重复/mismatch fail-fast | Steps 3–4 policy/pipeline tests |
| `REQ-007` | 主规格 §4、§7.3.3–§7.3.5 | Guard 行为保持 | AOP/programmatic/async/reactive outcome、failure、time、event terminality 与基线一致 | Steps 2–4 parity/full regression |
| `REQ-008` | 主规格 §4、§7.3.5、§8 | 删除 Bytecode Guard vertical slice | bridge records/methods、ASM package、runtime/starter adapter、fixtures/deps 均不存在 | Step 1 atomic cross-module commit |
| `REQ-009` | 主规格 §4、§7.3.5 | 保持其他 Bytecode 能力 | Executor/Observation/Method Extension unit、premain、Invoker 继续通过 | Step 1 retained tests + final verify |
| `REQ-010` | 主规格 §4、§16.4 | 更新四份 README | 不再给出 Guard Agent/constructor 用法；说明 AOP/Client/2.0 migration | Step 5 docs/static gate |
| `REQ-011` | 主规格 §4、§7.3.5、§16 | 删除能力 fail-fast，Bridge 2.0 | `engine=AGENT`/`features=access-guard` 失败；mixed major 失败 | Steps 1–2 protocol/config tests；Step 5 docs |
| `REQ-012` | 主规格 §4、§14 | 分层验证并标明证据边界 | focused/module/Invoker/dependency/static gates exit 0；外部 Redis/生产另验 | 所有 Steps + Chapter 8 |
| `REQ-013` | 主规格 §4、§8、§19 | 其他不动 | diff 仅含 inventory paths；RPC/Tianshu/DB/UI/并发 dirty files无改动 | 所有 path-limited commits + final scope audit |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

1. 先删 Bytecode Guard vertical slice。Guard starter 当前是 Bytecode starter 的 optional compile dependency；若先删除 Guard marker/enums，Bytecode adapter 将立即无法编译。Step 1 以一个不可再拆的跨模块提交同时删除所有 producer/consumer，并让 Bridge protocol 2.0 与 retained features 一起 GREEN。Step 1 结束时旧 Guard `engine=AGENT` 因没有 integration 明确 fail-fast，不会静默放行；该中间提交不得单独发布。
2. 再收缩 Guard AOP public/config surface。此时 Bytecode 已无 Guard consumer，可安全删除 `AccessGuardAgentIntegration`、AGENT/CONSTRUCTOR enums 和 constructor branches，保留 Client/DISABLED。
3. 在 Engine 仍为单类时完成 typed Strategy。这样 policy API、四个实现、state/bypass、Engine 调用点和 auto-configuration 在一个编译闭环内切换；`DefaultGuardEngine` 暂时仍拥有 admission loop，但不再有字符串 switch/cast。
4. 基于已类型化的策略拆出 `GuardAdmissionPipeline` 与 `GuardExecutionCoordinator`，最后将 Engine 收缩为两个依赖。该顺序避免新 Pipeline 一开始同时承担策略迁移和职责迁移两类风险。
5. 代码和测试稳定后更新四份 README，最后执行跨模块、Invoker、RPC context、dependency/static 和 scope gates。

无数据库、生成 IDL、前端、权限或持久化迁移。Bridge major 和 Maven semantic version是不同维度：本计划只改 Spec 明确的 `BridgeProtocol=2.0`，不修改仓库 `5.3.3`/BOM；仓库发布版本选择属于后续 release owner。

### 4.2 Test-first strategy

| Behavior | RED file/assertion | Expected RED reason | Minimum GREEN | Refactor/wiring allowed |
| --- | --- | --- | --- | --- |
| Bytecode Guard removal | `DispatcherRegistryTest`、`AgentConfigurationLoaderTest`、`BytecodeDependencyBoundaryTest` | protocol仍1、capability/method仍存在、feature仍可解析、POM/import仍含 Guard | atomic vertical-slice deletion + protocol 2 | 仅清理 retained constructors/imports，禁止改 retained semantics |
| AOP-only Guard surface | `AccessGuardApiTest`、`AccessGuardAutoConfigurationTest` | annotation仍含 CONSTRUCTOR；enum/class仍存在；AGENT 当前走 integration 错误而非 binder failure | 删除 Guard Agent/constructor API 和 validator branches | `GuardInvocation` 文案同步属于 `PLAN-CLAR-001` |
| Typed Strategy | policy tests、`AdmissionOrderContractTest`、Engine/entry contract fixtures | 新 enum/签名不存在，测试先发生 compile RED；旧 bypass 是 String | typed enum/interface/sets、四策略自取 config、monolith Engine typed loop | Engine ownership尚不拆分 |
| Engine split | 新 `GuardAdmissionPipelineTest`、`GuardExecutionCoordinatorTest`、Facade tests | 新 types/signatures不存在，compile RED | immutable handoff + two collaborators + auto-config Beans | 移动现有逻辑，不改变 outcome |
| README migration | static `rg` baseline | 当前四文档仍命中 Agent/constructor/config/version patterns | 双语文档移除旧用法并说明 AOP/Client/2.0 | documentation-only，test-first unit N/A |

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | None | all Bytecode bridge/core/runtime/agent/starter/test paths | vertical slice 与 protocol 必须原子编译，不能留 dormant selectable Guard |
| Step 2 | Step 1 committed | None | Guard API/autoconfigure/constructor paths | 只有 Bytecode consumer 删除后才能删 marker/enums |
| Step 3 | Step 2 committed | None | policy types、`DefaultGuardEngine`、CoreAutoConfiguration、entry fixtures | typed contract须一次切换所有 compile consumers |
| Step 4 | Step 3 committed | None | core Engine、CoreAutoConfiguration、Engine/entry tests | 依赖 Step 3 的 typed policy；与 Step 3 重叠文件按 symbols 分阶段 |
| Step 5 | Steps 1–4 committed | None | four README files | 文档必须描述最终代码，不能先写过渡状态 |

### 4.4 Commit boundaries

- 每个 Step 一个语义 commit，使用 `git add -- <Step exact paths>`；不得使用 `git add .`。
- Step 1 文件多但不可拆：bridge ABI、generated calls、runtime adapter 和 Agent feature 任一单独删除都会造成编译断裂或静默 bypass。
- `AccessGuardCoreAutoConfiguration.java` 在 Steps 2/3/4 分别拥有 Agent wiring、typed policy wiring、Engine collaborator wiring；`DefaultGuardEngine.java` 与两份 Engine contract tests在 Steps 3/4 先切 typed contract再迁移职责。每一步结束均可独立验证，不提交已知编译失败状态。
- 当前所有 identity/Yuheng/Tianshu/RBAC 等 dirty paths以及 `docs/egon/spec/2026-08-23-16-43-open-source-archetype-family.md`不进入任何 commit。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| Remove Bytecode Guard slice | 主规格 §7.0/§7.3.5：Remove | `BridgeCapability.ACCESS_GUARD`→ASM wrapper→combined dispatcher→runtime adapter | 仅删 enum 会留下 dead API/依赖；deprecated no-op会静默绕过 | 减少6 core类、4 bridge records、4 runtime/starter类和一套fixtures；Bridge major升级 | Implement Step 1 |
| `GuardAdmissionPipeline` | 主规格 §7.0：Add | `DefaultGuardEngine#evaluateAdmission` + policy/failure helpers | private-method rearrange保留多职责与cast问题 | one local call + one object；无网络/DB | Implement Step 4 |
| `GuardExecutionCoordinator` | 主规格 §7.0：Add | `DefaultGuardEngine#prepare/#executeWithOutcome` + rejection/time/finalizer | 下沉AOP会破坏Client/async parity | one local call；复用Prepared lifecycle | Implement Step 4 |
| `GuardAdmission` | 主规格 §7.0/§10：Add | mutable private `PlanCapture` | four裸参数易错配；再次resolve会产生snapshot撕裂 | one short-lived record/invocation | Implement Step 4 |
| `GuardPolicyType` | 主规格 §7.0/§7.3.2：Add | string IDs、`configFor/failurePoint` switch、unchecked cast | enum-name直接输出会破坏kebab-case stable IDs | one closed enum/EnumMap；无I/O | Implement Step 3 |
| Non-generic `GuardPolicy` | 主规格 §9 `INTERNAL-004/005`：Modify | four concrete policy classes + monolith Engine generic dispatch | 保留generic会把switch/cast搬到Pipeline | four local implementations变更；Store calls不增 | Implement Step 3 |
| Facade + composition root | 主规格 §13：Keep/Modify | `GuardEngine`、Spring `AccessGuardCoreAutoConfiguration` | Abstract Factory无产品族变化点 | 3 new internal core types；no dependency | Implement Steps 3–4；reject factory |
| README migration | 主规格 §16.4：Modify | four current docs advertise Agent/constructor and stale versions | 不更新会形成可操作的错误配置 | four docs only；no runtime state | Implement Step 5 |
| New API/table/cache/job/page/dependency | 主规格 non-goals | no approved target/evidence | existing AOP/Client/Store/AutoConfiguration sufficient | added surface would exceed scope | Do not implement |

没有 fetch-then-forward API、caller-supplied trusted identity、新 cache/table/page、duplicate mapper/model 或 speculative factory。审计没有发现需要返回 Spec 的架构缺陷。

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| Bytecode capability/ABI removal | `REQ-001`,`REQ-008`,`REQ-009`,`REQ-011`–`013` | protocol/feature/dependency tests | current bridge/core/agent/starter all present | protocol2 + 3 capabilities + no Guard deps | Safe Guard marker deletion | Step 1 |
| Guard AOP-only API/config | `REQ-001`–`003`,`REQ-007`,`REQ-011`–`013` | API/auto-config/startup tests | Step 1 removes external consumers | TYPE/METHOD annotation；AOP/DISABLED；no Agent marker | Typed policy work on final entry model | Step 2 |
| Typed admission Strategy | `REQ-005`–`007`,`REQ-012`,`REQ-013` | policy/ordering/entry compile RED | Step 2 final enums/kinds | typed IDs/config/bypass/local map；monolith typed Engine | Pipeline extraction | Step 3 |
| Engine two-stage split | `REQ-003`,`REQ-004`,`REQ-007`,`REQ-012`,`REQ-013` | new stage tests compile RED | Step 3 typed policies | `GuardAdmission`、Pipeline、Coordinator、thin Facade Beans | Final docs/release gates | Step 4 |
| Bilingual docs/migration | `REQ-010`–`013` | static doc matches | Steps 1–4 final behavior | four accurate README files | rollout/review | Step 5 |

## 5. Change File Tree

```text
egon-cola-components/
├── egon-cola-component-bytecode/
│   ├── README.md                                                   MODIFY S5
│   ├── README.zh-CN.md                                             MODIFY S5
│   ├── egon-cola-component-bytecode-bridge/src/{main,test}/java/... MODIFY/DELETE S1
│   ├── egon-cola-component-bytecode-core/src/{main,test}/java/...   MODIFY/DELETE S1
│   ├── egon-cola-component-bytecode-runtime/.../accessguard/        DELETE S1
│   ├── egon-cola-component-bytecode-agent/src/{main,test}/java/...  MODIFY S1
│   ├── egon-cola-component-bytecode-starter/
│   │   ├── pom.xml                                                  MODIFY S1
│   │   ├── .../BytecodeAutoConfiguration.java                      MODIFY S1
│   │   ├── .../methodextension/MethodMetadataResolver.java         MODIFY S1
│   │   ├── .../accessguard/                                        DELETE S1
│   │   └── .../AutoConfiguration.imports                           MODIFY S1
│   └── egon-cola-component-bytecode-test/
│       ├── pom.xml                                                  MODIFY S1
│       ├── src/test/.../AccessGuard*                                DELETE S1
│       └── src/it/access-guard-spring/                              DELETE S1
└── egon-cola-component-access-guard-starter/
    ├── README.md                                                    MODIFY S5
    ├── README.zh-CN.md                                              MODIFY S5
    └── src/
        ├── main/java/.../accessguard/
        │   ├── api/adapter/autoconfigure/execution/plan entry files MODIFY/DELETE S2
        │   ├── policy typed files                                   CREATE/MODIFY/DELETE S3
        │   └── core/{GuardAdmission,GuardAdmissionPipeline,
        │       GuardExecutionCoordinator}.java                      CREATE S4
        └── test/java/.../accessguard/                               CREATE/MODIFY S2-S4
```

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| MODIFY | `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/test/java/top/egon/cola/component/bytecode/bridge/DispatcherRegistryTest.java` | hardcoded protocol 1/2 | assert 2.0、three capabilities、removed dispatcher methods | Bridge RED/compat contract | 1 | `REQ-008`,`REQ-011`,`REQ-012` | focused Bridge test |
| MODIFY | `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/test/java/top/egon/cola/component/bytecode/agent/{AgentConfigurationLoaderTest.java,transform/CompositeBytecodeTransformerTest.java}` | accepts/accesses Access Guard feature | rejects removed feature；fatal test uses EXECUTOR | Agent RED + retained failure regression | 1 | `REQ-008`,`REQ-009`,`REQ-011` | focused Agent tests |
| MODIFY | `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/test/java/top/egon/cola/component/bytecode/starter/{BytecodeAutoConfigurationTest.java,BytecodeDependencyBoundaryTest.java}` | fatal fixture uses ACCESS_GUARD；POM expected optional Guard | retained fatal fixture；no Guard dep/import；direct dispatcher | Starter/dependency RED | 1 | `REQ-008`,`REQ-009`,`REQ-011` | focused Starter tests |
| MODIFY | `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/{BridgeProtocol.java,BridgeCapability.java,BytecodeRuntimeDispatcher.java,EgonPolicyBridge.java}` | protocol1 + Guard capability/methods | protocol2 + EXECUTOR/OBSERVATION/METHOD_EXTENSION only | retained bridge ABI | 1 | `REQ-008`,`REQ-009`,`REQ-011` | Bridge tests |
| DELETE | `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/{BridgeGuardedInvocation.java,BridgeConstructorInvocation.java,BridgeFailHint.java,ConstructorGuardDecision.java}` | Guard-only records | absent | remove public Guard bridge payloads | 1 | `REQ-001`,`REQ-008` | compile + zero search |
| MODIFY | `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/{ApplicationClassEnhancer.java,ClassEnhancementPlanner.java,DuplicateEnhancementDetector.java,EnhancementFeature.java,MethodEnhancementPlan.java}` | AccessGuard matcher/plan/wrapper branches | retained feature plan only | core enhancement composition | 1 | `REQ-008`,`REQ-009` | core tests |
| DELETE | `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/accessguard/{AccessGuardMatcher.java,AccessGuardMethodWrapper.java,AccessGuardPolicy.java,ConstructorGuardEnhancer.java,GovernanceAnnotationFilter.java,SyntheticBodyName.java}` | Guard ASM package | absent | remove Guard transformation | 1 | `REQ-001`,`REQ-008` | core compile/static |
| MODIFY | `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/main/java/top/egon/cola/component/bytecode/agent/{AgentConfiguration.java,BytecodeAgent.java}` | `accessGuardEnabled`/matcher wiring | only retained feature wiring | Agent config/runtime | 1 | `REQ-008`,`REQ-009`,`REQ-011` | agent tests |
| DELETE | `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-runtime/src/main/java/top/egon/cola/component/bytecode/runtime/accessguard/GuardedInvocationEvaluator.java` | Guard runtime SPI | absent | remove runtime seam | 1 | `REQ-008` | runtime compile |
| MODIFY | `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/{BytecodeAutoConfiguration.java,methodextension/MethodMetadataResolver.java}` | combined dispatcher/ObjectProvider；constructor resolver | direct dispatcher；method metadata only | retained Spring runtime wiring | 1 | `REQ-008`,`REQ-009` | starter tests |
| MODIFY | `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | imports Guard auto-config | retained imports only | registration boundary | 1 | `REQ-008`,`REQ-009` | boundary test/static |
| DELETE | `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/accessguard/{AccessGuardAgentAutoConfiguration.java,AccessGuardRuntimeAdapter.java,CombinedPolicyDispatcher.java}` | Guard starter adapter package | absent | remove Spring integration | 1 | `REQ-001`,`REQ-008` | starter compile |
| MODIFY | `egon-cola-components/egon-cola-component-bytecode/{egon-cola-component-bytecode-starter/pom.xml,egon-cola-component-bytecode-test/pom.xml}` | Access Guard dependencies | no Access Guard dependency | dependency hygiene | 1 | `REQ-008`,`REQ-011` | dependency tree/test |
| DELETE | `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/test/java/top/egon/cola/component/bytecode/core/enhance/accessguard/{AccessGuardMatcherTest.java,AccessGuardMethodWrapperTest.java,ConstructorGuardEnhancerTest.java}` | obsolete Guard core tests | absent | remove deleted capability tests | 1 | `REQ-008`,`REQ-009` | retained core tests |
| DELETE | `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/test/java/top/egon/cola/component/bytecode/starter/accessguard/{AccessGuardRuntimeAdapterTest.java,CombinedPolicyDispatcherTest.java}` | obsolete starter tests | absent | remove deleted adapter tests | 1 | `REQ-008` | retained starter tests |
| DELETE | `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/test/java/{sample/bytecode/agent/AccessGuardAgentFixture.java,top/egon/cola/component/bytecode/test/agent/AccessGuardAgentIntegrationTest.java}` | real premain Guard fixture | absent | remove Agent integration fixture | 1 | `REQ-008`,`REQ-009` | test module verify |
| DELETE | `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/access-guard-spring/{pom.xml,invoker.properties,verify.groovy,src/test/java/sample/accessguard/AccessGuardSpringAgentTest.java}` | Guard Invoker scenario | absent；17 retained scenarios | remove Spring Guard scenario | 1 | `REQ-008`,`REQ-009` | Invoker verify |
| MODIFY | `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/{api/AccessGuardApiTest.java,autoconfigure/AccessGuardAutoConfigurationTest.java,autoconfigure/AccessGuardStartupValidatorTest.java}` | constructor/Agent expectations | TYPE/METHOD；AOP/DISABLED；old AGENT binder failure | AOP-only RED/contract | 2 | `REQ-001`–`003`,`REQ-011` | focused Guard tests |
| MODIFY | `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/{api/AccessGuard.java,autoconfigure/AccessGuardEngine.java,core/GuardEntryType.java,core/GuardInvocationKind.java,core/GuardInvocation.java}` | constructor/AGENT values and wording | AOP/PROGRAMMATIC/METHOD/OPERATION only | public/internal entry model | 2 | `REQ-001`–`003`,`REQ-011` | API tests/compile |
| DELETE | `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/api/AccessGuardAgentIntegration.java` | Bytecode marker | absent | remove integration seam | 2 | `REQ-001` | zero search |
| MODIFY | `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/{adapter/aop/GuardBindingResolver.java,autoconfigure/AccessGuardStartupValidator.java,core/plan/GuardPlanValidator.java,execution/DefaultRejectionHandler.java,execution/MethodHandleFallbackHandler.java}` | constructor overload/branches | method/operation only | remove constructor runtime paths | 2 | `REQ-001`,`REQ-002`,`REQ-007` | focused/full Guard tests |
| MODIFY | `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardCoreAutoConfiguration.java` | Agent provider + String policy + monolith wiring | S2 no Agent；S3 typed policies；S4 collaborator Beans | composition root | 2,3,4 | `REQ-001`,`REQ-004`–`007` | auto-config/full Guard |
| MODIFY | `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/policy/{AdmissionOrderContractTest.java,allow/AllowListPolicyTest.java,deny/DenyListPolicyTest.java,penalty/PenaltyBoxPolicyTest.java}` | string/generic tests | typed order/config/bypass tests | Strategy RED/GREEN | 3 | `REQ-005`,`REQ-006`,`REQ-012` | policy tests |
| CREATE | `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/policy/ratelimit/RateLimitPolicyTest.java` | absent | backend request derived from own nested config | Rate policy contract | 3 | `REQ-005`,`REQ-007` | policy tests |
| MODIFY | `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/{core/DefaultGuardEngineTest.java,contract/AccessGuardEntryContractTest.java}` | String policy/monolith constructors | S3 typed fixtures；S4 stage/Facade fixtures | parity/engine contracts | 3,4 | `REQ-004`–`007` | core/contract tests |
| CREATE | `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/GuardPolicyType.java` | absent | four types + stable IDs/failure points/canonical order | typed identity | 3 | `REQ-005`,`REQ-006` | order tests |
| MODIFY | `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/{policy/GuardPolicy.java,policy/PolicyResult.java,policy/GuardContext.java,core/GuardExecutionState.java,core/plan/AdmissionConfig.java}` | generic/String bypass/PolicyConfig | non-generic + typed immutable sets | Strategy contracts/state | 3 | `REQ-005`–`007` | policy/core tests |
| MODIFY | `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/{allow/AllowListPolicy.java,deny/DenyListPolicy.java,penalty/PenaltyBoxPolicy.java,ratelimit/RateLimitPolicy.java}` | nested config argument + String id | `type()` + full AdmissionConfig evaluation | concrete strategies | 3 | `REQ-005`–`007` | four policy tests |
| DELETE | `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/{AdmissionPolicies.java,PolicyConfig.java}` | no-behavior factory/generic marker | absent | remove obsolete abstractions | 3 | `REQ-005`,`REQ-006` | compile/zero search |
| MODIFY | `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/DefaultGuardEngine.java` | string/generic monolith | S3 typed monolith；S4 two-collaborator Facade | migration then final Facade | 3,4 | `REQ-004`–`007` | core/entry tests |
| CREATE | `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/core/{GuardAdmissionPipelineTest.java,GuardExecutionCoordinatorTest.java}` | absent | independent admission/execution contracts | Engine split RED/GREEN | 4 | `REQ-004`,`REQ-006`,`REQ-007` | new core tests |
| CREATE | `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/{GuardAdmission.java,GuardAdmissionPipeline.java,GuardExecutionCoordinator.java}` | PlanCapture/monolith logic | immutable handoff + two collaborators | Engine split implementation | 4 | `REQ-004`–`007` | new core tests |
| MODIFY | `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardEngine.java` | stable methods, minimal docs | signatures unchanged；deferred/Facade contract clarified | stable SPI boundary | 4 | `REQ-003`,`REQ-004`,`REQ-007` | compile/parity |
| MODIFY | `egon-cola-components/egon-cola-component-access-guard-starter/{README.md,README.zh-CN.md}` | advertises Agent/constructor；5.3.2 | AOP/DISABLED + Client + split/typed design；5.3.3 | Guard docs/migration | 5 | `REQ-010`–`013` | static/doc review |
| MODIFY | `egon-cola-components/egon-cola-component-bytecode/{README.md,README.zh-CN.md}` | advertises Guard feature；5.2.3 | three retained features + protocol2 rollout；5.3.3 | Bytecode docs/migration | 5 | `REQ-009`–`013` | static/doc review |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- Prompt-provided main-agent `AGENTS.md` rules apply；仓库内 `rg --files -g AGENTS.md` 未发现额外文件。
- Branch `main`，commit `772df2b28b4abf29e7fffae6ea1b10fd616397b4`。
- 现有 identity/Yuheng/Tianshu/RBAC/scripts/两份 identity Spec/Plan 的 modified files，以及并发创建的 `docs/egon/spec/2026-08-23-16-43-open-source-archetype-family.md` 均属于用户/并发工作，必须保持未暂存、未提交、未修改。
- 主 Spec 与本 Plan 当前未跟踪；实施前应只 path-stage 它们的审批状态/关系提交，不能把其他 dirty files带入。
- 本任务没有 generated source、Flyway、数据库、前端或浏览器操作。

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| Java/Maven | `java -version` / `./mvnw -version`；root POM Java 21、Surefire 3.2.5 | JDK 21；repo Maven Wrapper | build/module only |
| Guard baseline | repo root：`./mvnw -B -ntp -pl :egon-cola-component-access-guard-starter test` | 2026-08-23 baseline：146 tests、0 failures、5 gated Redis skips | module；不证明真实 Redis |
| Bytecode focused baseline | repo root：artifact slice + `-Dtest=DispatcherRegistryTest,AgentConfigurationLoaderTest,CompositeBytecodeTransformerTest,BytecodeAutoConfigurationTest,BytecodeDependencyBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false test` | 15-module selected reactor exit 0；19 named tests pass | selected static/module |
| Known broad-reactor baseline | repo root：same Bytecode slice `-am test` without selectors | 当前在 upstream Guard tests 因 `NoProviderFoundException` 失败；不是本任务代码回归 | existing build-classpath gap；用分层 gates隔离并如实报告 |
| Invoker/premain | repo root：`./mvnw -B -ntp -pl :egon-cola-component-bytecode-test -am verify` | future execution；short-lived child JVM only | real test JVM，非业务服务 |
| External systems | no command in this Plan starts Redis/application | external Redis and target deployment supplied by owner | live proof remains external |

### 6.3 Immutable constraints and approved decisions

- `AccessGuardClient` 与 `GuardEngine.evaluate/prepare/execute` signatures不变；`DISABLED` 保留 Client。
- policy order固定 DENY_LIST→ALLOW_LIST→PENALTY_BOX→RATE_LIMIT；Deny不可 bypass。
- Store、Redis key/Lua、rule schema、failure policy、time limit、events/metrics和 rate-limit algorithm factory不改。
- Bytecode Executor/Observation/Method Extension 行为不改；只移除 Guard capability。
- Bridge ABI removal使用 protocol 2.0；同一进程 artifacts必须同版本；不保留 deprecated/no-op bridge。
- 不新增 Abstract Factory、plugin registry、Maven模块、依赖、DB migration、UI或 release-version change。
- 不启动项目；每 Step 一个 path-limited commit。

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| `PLAN-CLAR-001` | `GuardInvocation` 的 executable 校验保留，但错误文本从“methods and constructors”收缩为“methods” | `GuardInvocation.java:32-33`；final kind只有 METHOD/OPERATION | 只同步已决定的 enum surface，不改变合法/非法输入集合 | 若保留旧文本，功能仍工作但诊断违反 AOP-only 文档 |
| `PLAN-CLAR-002` | `DispatcherRegistryTest` 用 `BridgeProtocol.MAJOR` 表示有效版本、`MAJOR+1` 表示 mismatch，并单独断言 major=2 | 当前测试硬编码1为成功、2为失败；主规格 `DEC-009` | 仅消除测试对旧值的重复事实，协议行为不变 | 若 registry另有1.x兼容要求，需返回 Spec；当前无证据 |
| `PLAN-CLAR-003` | `AccessGuardEntryContractTest.LOCAL_FALLBACK` 改用 RateLimit backend failure + local RateLimit policy，不再手工给 DenyList 注入 local policy | auto-config local map仅 penalty/rate；主规格 §7.3.2要求精确两类；当前测试人为设置 DENY_LIST_STORE local fallback | 仍验证 AOP/programmatic 对 LOCAL_FALLBACK 的 outcome/store side-effect parity，且贴近生产 wiring | 若必须支持 Deny/Allow local fallback，需要修订 Spec和auto-config，不可在Plan内扩展 |
| `PLAN-CLAR-004` | 四份 README 的 Maven/JAR示例版本统一到当前 repository `5.3.3` | root/module POM=5.3.3；Guard docs=5.3.2、Bytecode docs=5.2.3 | 仅修正同文件内既有安装示例，不改artifact或发布流程 | 若文档要求使用占位符，应在实施前统一改为 `${project.version}` 表述 |

## 7. Ordered File-by-file Implementation Steps

> 每个 Step 必须先确认前一 Step commit 已存在；任何 RED 只能停留在本 Step 未提交工作区，Step commit 必须 GREEN。

### Step 1 — 原子删除 Bytecode Access Guard Agent 能力并升级 Bridge 2.0

- Requirements: `REQ-001`, `REQ-008`, `REQ-009`, `REQ-011`, `REQ-012`, `REQ-013`
- Dependencies: `None`
- Baseline state: Bridge 1.0 有四项 capability 和 Guard dispatcher methods；core/agent生成 Guard method/constructor calls；runtime/starter回调 GuardEngine；starter/test POM依赖 Guard；focused baseline 19 tests通过。
- Observable outcome: `features=access-guard` 被 parser 拒绝，Bridge只含三项 retained capabilities，Guard ABI/ASM/runtime/starter/dependency/fixtures全部消失，Executor/Observation/Method Extension tests保持。
- End state: Bytecode artifacts形成可编译 protocol2 coherent set；旧 Guard starter仍暂存 AGENT enum但因无 integration明确启动失败，Step 2 前不得发布该中间版本。
- Test-first gate: `Required — DispatcherRegistryTest 当前看到 major=1/Guard methods，AgentConfigurationLoaderTest 当前接受 access-guard，dependency/import tests 当前看到 Guard；focused RED 必须由这些已存在行为导致。`
- Ordered files:

#### File 1 — `MODIFY egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/test/java/top/egon/cola/component/bytecode/bridge/DispatcherRegistryTest.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/test/java/top/egon/cola/component/bytecode/agent/AgentConfigurationLoaderTest.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/test/java/top/egon/cola/component/bytecode/agent/transform/CompositeBytecodeTransformerTest.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/test/java/top/egon/cola/component/bytecode/starter/BytecodeAutoConfigurationTest.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/test/java/top/egon/cola/component/bytecode/starter/BytecodeDependencyBoundaryTest.java`

- Purpose: 先定义 protocol2、removed feature/API/dependency 与 retained failure behavior。
- Symbols: `rejectsMajorMismatchAndNegotiatesCapabilitiesAcrossMinorVersions`、new `rejectsRemovedAccessGuardFeature`、fatal transformer/agent test、`keepsOptionalIntegrationsOutOfTheDefaultDependencyGraph`。
- Repository evidence: 五个现有 JUnit 5 tests分别直接覆盖 registry、parser、transform failure、Spring runtime和POM文本边界。
- Dependencies and consumers: tests compile against Bridge/Agent/Starter；不依赖真实 Agent attach或Redis。
- Why now: 这是 Step 1 的可观察 RED；生产删除前先锁定 retained behavior和absence contract。
- Contract/signature changes: assert `BridgeProtocol.MAJOR==2`、capabilities exact set；reflection asserts dispatcher无 `invokeGuarded/guardConstructor`；`features=access-guard` throws；POM/import无 Guard；fatal fixtures使用EXECUTOR。
- Input/output and state mapping: config string→feature enum或IllegalArgumentException；Agent status retained capability→fatal startup；POM/import text→absence assertions。
- Error and edge behavior: minor版本仍接受；major+1拒绝；未知removed feature不忽略；fatal state仍阻止Spring context；没有Guard时默认dispatcher仍注册。
- Implementation pseudocode:

```java
assertThat(BridgeProtocol.MAJOR).isEqualTo(2)
assertThat(BridgeCapability.values()).containsExactly(EXECUTOR, OBSERVATION, METHOD_EXTENSION)
assertThat(methodNames(BytecodeRuntimeDispatcher.class)).doesNotContain("invokeGuarded", "guardConstructor")
assertThrows(IllegalArgumentException.class, () -> loader.load("enabled=true,features=access-guard,include=app.*"))
assertFalse(read(starterPomAndImports).contains("access-guard"))
```

- Verification contribution: RED/GREEN selectors覆盖 `TEST-026`,`028`,`029`,`031`；retained transformer failure防止误伤 `REQ-009`。
- After this file: 当前源码下至少 protocol/capability/parser/dependency assertions失败；失败原因是目标能力仍存在。

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/BridgeProtocol.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/BridgeCapability.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/BytecodeRuntimeDispatcher.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/EgonPolicyBridge.java`

- Purpose: 发布 retained-only Bridge 2.0 Java ABI。
- Symbols: `BridgeProtocol.MAJOR/MINOR`、`BridgeCapability`、dispatcher defaults、`EgonPolicyBridge.evaluateMethodExtension`。
- Repository evidence: Bridge当前是JDK-only边界；registry按major exact check；PolicyBridge已有Method Extension和Guard分支。
- Dependencies and consumers: transformed retained classes、runtime dispatcher、Agent/starter/tests；不能引入非JDK依赖。
- Why now: tests已锁定新ABI；先移除Bridge consumers’目标 methods，再删payload records。
- Contract/signature changes: `MAJOR 1→2`,`MINOR=0`；enum删ACCESS_GUARD；dispatcher删两Guard methods；PolicyBridge只留Method Extension。
- Input/output and state mapping: retained method extension invocation→same dispatcher lookup/PROCEED fail-open；Guard invocation无映射或替代。
- Error and edge behavior: registry major mismatch继续IllegalArgumentException；Method Extension dispatcher null/throw继续PROCEED；不保留silent Guard no-op。
- Implementation pseudocode:

```java
final class BridgeProtocol { static final int MAJOR = 2; static final int MINOR = 0; }
enum BridgeCapability { EXECUTOR, OBSERVATION, METHOD_EXTENSION }
interface BytecodeRuntimeDispatcher { keep executor/observation/evaluateMethodExtension; remove both Guard methods; }
final class EgonPolicyBridge { keep evaluateMethodExtension unchanged; remove invokeGuarded and guardConstructor; }
```

- Verification contribution: makes Bridge protocol/reflection assertions GREEN and supplies compile target for retained modules。
- After this file: Bridge源码不再定义Guard ABI；core/runtime/starter旧consumers暂时compile-red，下一文件组移除。

#### File 3 — `DELETE egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/BridgeGuardedInvocation.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/BridgeConstructorInvocation.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/BridgeFailHint.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/ConstructorGuardDecision.java`

- Purpose: 删除 protocol1 Guard-only JDK payload types。
- Symbols: four records/enums listed in heading。
- Repository evidence: full-repo usage只在Guard bridge/core/runtime/starter/tests，均属于本Step；retained features不用。
- Dependencies and consumers: File 2已移除Bridge signatures；Files 4–8将删除其余imports/callers。
- Why now: ABI已移除，payload不再有合法consumer。
- Contract/signature changes: public classes完全删除，无deprecated alias。
- Input/output and state mapping: historical target/args/proceed/failHint/decision不再跨ClassLoader；AOP/Client不使用Bridge。
- Error and edge behavior: old binary caller通过protocol major mismatch在启动阶段暴露；不允许linkage后静默proceed。
- Implementation pseudocode:

```text
delete BridgeGuardedInvocation and BridgeConstructorInvocation sources
delete BridgeFailHint and ConstructorGuardDecision sources
run full rg for type names; every remaining hit must be another Step-1 deletion before commit
retain all Executor/Observation/MethodExtension bridge records unchanged
```

- Verification contribution: `TEST-028`,`032` absence/compile gates。
- After this file: Bridge module自身可编译；下游旧Guard callers待同Step清理。

#### File 4 — `MODIFY egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/ApplicationClassEnhancer.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/ClassEnhancementPlanner.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/DuplicateEnhancementDetector.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/EnhancementFeature.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/MethodEnhancementPlan.java`

- Purpose: 从 enhancement plan和rewrite pipeline删除Guard分支，保留三能力组合。
- Symbols: enhancer constructors/fields、`plan(...)` overload、feature enum、method record fields、duplicate detector method。
- Repository evidence: `ApplicationClassEnhancer`目前唯一组合四特性；Method Extension/Observation tests使用2/3参数constructors。
- Dependencies and consumers: Agent `BytecodeAgent`、retained core tests、BridgeCapability name mapping。
- Why now: 先移除accessguard package consumers，随后安全删除package。
- Contract/signature changes: enhancer最终最多3参数；planner最多4参数(loader,node,observation,methodExtension)；MethodPlan无AccessGuardPolicy；no ACCESS_GUARD feature/detector。
- Input/output and state mapping: class bytes→executor/observation/method-extension plans unchanged；features→BridgeCapability映射只处理retained values。
- Error and edge behavior: duplicate retained bridges仍return null；constructor Observation timing不改；Method Extension wrapper顺序不改。
- Implementation pseudocode:

```java
plan = planner.plan(loader, classNode, observationMatcher, methodExtensionMatcher)
if (executorEnabled && !containsExecutorBridge) rewriteExecutor()
if (observationMatcher != null && !containsObservationBridge) rewriteObservation(plan)
if (methodExtensionMatcher != null && !containsMethodExtensionBridge) rewriteMethodExtension(plan)
record MethodEnhancementPlan(..., ObservationPolicy observation, MethodExtensionPolicy extension)
```

- Verification contribution: retained core unit tests和 `TEST-027`,`030`。
- After this file: core common classes无AccessGuard imports/fields；accessguard package成为无consumer代码。

#### File 5 — `DELETE egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/accessguard/AccessGuardMatcher.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/accessguard/AccessGuardMethodWrapper.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/accessguard/AccessGuardPolicy.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/accessguard/ConstructorGuardEnhancer.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/accessguard/GovernanceAnnotationFilter.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/accessguard/SyntheticBodyName.java`

- Purpose: 删除Access Guard ASM matcher、method wrapper、constructor enhancer及私有helpers。
- Symbols: entire `core.enhance.accessguard` production package。
- Repository evidence: six files仅服务 `@AccessGuard` structural transformation；File 4已移除所有retained callers。
- Dependencies and consumers: old package dependedASM/Bridge Guard records；obsolete tests/fixtures在File 9删除。
- Why now: common planner/enhancer已不引用，删除不会影响retained transforms。
- Contract/signature changes: package完全不存在；无replacement matcher或generic policy hook。
- Input/output and state mapping: Guard annotation不再产生bytecode changes/metadata；AOP负责自动治理。
- Error and edge behavior: protected/private/static/constructor不再由Agent处理；config parser在File 6确保无法选择该能力。
- Implementation pseudocode:

```text
delete all six core.enhance.accessguard sources
search ApplicationClassEnhancer/ClassEnhancementPlanner for imports or ACCESS_GUARD branches
assert retained observation constructor and method-extension enhancer sources are untouched
allow no empty accessguard package or generated bridge-call signature
```

- Verification contribution: zero-reference gate、core compile、retained transformer tests。
- After this file: core无法生成Guard calls；Executor/Observation/Method Extension仍可生成。

#### File 6 — `MODIFY egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/main/java/top/egon/cola/component/bytecode/agent/AgentConfiguration.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/main/java/top/egon/cola/component/bytecode/agent/BytecodeAgent.java`

- Purpose: 删除Agent Guard feature accessor/matcher wiring，使removed feature在parse阶段失败。
- Symbols: `accessGuardEnabled()`、`BytecodeAgent.premain` enhancer construction。
- Repository evidence: loader通过`BridgeCapability.valueOf`解析；enum删除后`access-guard`自然IllegalArgumentException；premain当前创建4参数enhancer。
- Dependencies and consumers: AgentConfigurationLoader、startup reporter、CompositeTransformer、File4新constructor。
- Why now: core API已收缩，可以迁移Agent consumer并关闭transform入口。
- Contract/signature changes: `AgentConfiguration`只保留`methodExtensionEnabled`辅助；premain创建3参数enhancer。
- Input/output and state mapping: retained feature set→same matcher/enhancer；removed string→loader failure→startup failure summary。
- Error and edge behavior: unknown feature不可忽略；disabled Agent仍正常；failure policy仍记录fatal/degraded逻辑。
- Implementation pseudocode:

```java
boolean methodExtensionEnabled() = enabled && features.contains(METHOD_EXTENSION)
enhancer = new ApplicationClassEnhancer(executorEnabled, observationMatcher,
        configuration.methodExtensionEnabled() ? new MethodExtensionMatcher() : null)
configurationLoader.parseFeatures("access-guard") -> IllegalArgumentException from missing enum
keep premain catch/stateStore/failureSummary branches unchanged
```

- Verification contribution: Agent parser/transform retained tests，`TEST-026`,`030`。
- After this file: Agent不会选择/创建Guard matcher；focused Agent RED转GREEN。

#### File 7 — `MODIFY egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/BytecodeAutoConfiguration.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/methodextension/MethodMetadataResolver.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- Purpose: 删除runtime/starter Guard seam，直接注册retained dispatcher。
- Symbols: `bytecodeRuntimeDispatcher` Bean、MethodMetadataResolver constructor cache/method、AutoConfiguration imports。
- Repository evidence: current Bean仅在存在Guard evaluator时包CombinedPolicyDispatcher；constructor resolver只检查ACCESS_GUARD metadata。
- Dependencies and consumers: `DefaultBytecodeRuntimeDispatcher`、Method Extension auto-config、Spring imports；File8 POM随后删依赖。
- Why now: Bridge/core/Agent都已无Guard，runtime/starter可一次断开且不留下Bean/classpath入口。
- Contract/signature changes: dispatcher Bean不接收`ObjectProvider<GuardedInvocationEvaluator>`并总是返回default dispatcher；MethodMetadataResolver仅method；Guard auto-config import删除。
- Input/output and state mapping: executor/observation/method-extension配置→same default dispatcher capabilities；无Guard Bean/capability。
- Error and edge behavior: duplicate registration/fatal Agent/Method Extension capability validation保持；不存在“Guard class缺失时条件跳过”的隐式路径。
- Implementation pseudocode:

```java
@Bean BytecodeRuntimeDispatcher bytecodeRuntimeDispatcher(taskDecorator, observationRuntime,
        ObjectProvider<MethodExtensionInvocationEvaluator> evaluators, BytecodeProperties props) {
    return new DefaultBytecodeRuntimeDispatcher(taskDecorator, props.executor.enabled,
            observationRuntime, evaluators.getIfAvailable())
}
remove constructor ClassValue/resolveConstructor; remove Guard AutoConfiguration import
```

- Verification contribution: Starter auto-config、dependency/import boundary、retained Method Extension tests。
- After this file: runtime/starter wiring不再消费Guard evaluator；待File8删除实现类和SPI源码。

#### File 8 — `DELETE egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-runtime/src/main/java/top/egon/cola/component/bytecode/runtime/accessguard/GuardedInvocationEvaluator.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/accessguard/AccessGuardAgentAutoConfiguration.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/accessguard/AccessGuardRuntimeAdapter.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/accessguard/CombinedPolicyDispatcher.java`

- Purpose: 删除已从Spring wiring断开的Guard runtime SPI与starter adapter实现。
- Symbols: `GuardedInvocationEvaluator`、`AccessGuardAgentAutoConfiguration`、`AccessGuardRuntimeAdapter`、`CombinedPolicyDispatcher`。
- Repository evidence: 这些类型只服务Bridge Guard消息、Guard Agent auto-config和组合dispatcher；Files2–7完成后没有retained capability consumer。
- Dependencies and consumers: File7已先移除Bean参数/import；File9随后删除Maven依赖，File10删除专属测试。
- Why now: 先断开消费者再删实现，保证每个删除项都有明确的上游迁移点。
- Contract/signature changes: 删除四个内部runtime/starter类型；不提供deprecated shim，不改变default dispatcher的retained接口。
- Input/output and state mapping: Guard bridge request不再存在；executor/observation/method-extension请求仍由default dispatcher按原合同处理。
- Error and edge behavior: 不保留缺类条件分支；若仍有import，静态搜索和compile必须返回File7清理，而不是恢复这些类型。
- Implementation pseudocode:

```text
verify BytecodeAutoConfiguration and AutoConfiguration.imports no longer name Access Guard classes
delete runtime accessguard SPI and all three starter accessguard implementation classes
run rg across Bytecode production sources; any remaining import or type reference fails this file gate
```

- Verification contribution: Starter compile、auto-config测试、Guard零引用检查。
- After this file: Bytecode runtime/starter源码无Guard实现；File9可安全收缩依赖图。

#### File 9 — `MODIFY egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/pom.xml; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/pom.xml`

- Purpose: 删除Bytecode starter/test对Access Guard starter依赖。
- Symbols: two `<dependency>` blocks with `egon-cola-component-access-guard-starter`。
- Repository evidence: starter dependency为optional，test dependency为ordinary；File7/9后没有源码consumer。
- Dependencies and consumers: Maven reactor/dependency tree/test classpath；Method Extension dependency必须保留。
- Why now: 只有所有production consumers移除后才能安全收缩classpath。
- Contract/signature changes: exact dependency block deletion；不新增替代dependency，不改BOM。
- Input/output and state mapping: resolved graph不再带Guard/Jackson/AOP/Redisson transitives；retained starter dependencies不变。
- Error and edge behavior: dependency boundary test要求false；若compile出现Guard import，返回File7/9清理而非恢复dependency。
- Implementation pseudocode:

```xml
remove dependency top.egon:egon-cola-component-access-guard-starter from starter pom
remove dependency top.egon:egon-cola-component-access-guard-starter from bytecode-test pom
keep method-extension optional/runtime dependencies and all plugin executions unchanged
verify dependency:tree contains no matching Guard artifact
```

- Verification contribution: `TEST-031`和dependency tree。
- After this file: Bytecode Maven graph不依赖Guard；obsolete tests/fixtures需由File10删除以恢复test compile。

#### File 10 — `DELETE egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/test/java/top/egon/cola/component/bytecode/core/enhance/accessguard/; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/test/java/top/egon/cola/component/bytecode/starter/accessguard/; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/test/java/sample/bytecode/agent/AccessGuardAgentFixture.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/test/java/top/egon/cola/component/bytecode/test/agent/AccessGuardAgentIntegrationTest.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/access-guard-spring/`

- Purpose: 删除只证明已删除能力的3 core tests、2 starter tests、2 forked test files和4-file Invoker scenario。
- Symbols: `AccessGuardMatcherTest`、`AccessGuardMethodWrapperTest`、`ConstructorGuardEnhancerTest`、adapter/combined tests、fixtures/scenario。
- Repository evidence: exact directories/files均import deleted Guard/bridge/core/starter symbols；其他17 Invoker directories不含Guard。
- Dependencies and consumers: Maven Surefire/Failsafe/Invoker automatic discovery；POM File9已删Guard test dependency。
- Why now: production capability已完整删除；保留测试会testCompile失败且错误宣称能力。
- Contract/signature changes: no replacement Agent test；retained premain fixtures continue as `ExecutorAgentIntegrationTest`,`MethodObservationAgentIntegrationTest`,`MethodExtensionAgentIntegrationTest`。
- Input/output and state mapping: Invoker project count18→17；no Guard marker；other expected stdout markers保持。
- Error and edge behavior: 不删除Observation constructor tests；不修改architecture/agent/method-extension/observation scenario。
- Implementation pseudocode:

```text
delete exact AccessGuard core/starter test packages and two forked test Java files
delete entire src/it/access-guard-spring including pom, properties, verifier and Spring test
enumerate remaining src/it directories and assert count=17 with retained scenario names
run rg so no production/test file imports deleted Guard bridge/adapter/enhancer symbols
```

- Verification contribution: `TEST-027`–`032` retained/full Bytecode verify。
- After this file: all Step1 sources/tests compile against protocol2 without Guard；ready for GREEN gates。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl :egon-cola-component-bytecode-bridge,:egon-cola-component-bytecode-core,:egon-cola-component-bytecode-runtime,:egon-cola-component-bytecode-agent,:egon-cola-component-bytecode-starter -am -Dtest=DispatcherRegistryTest,AgentConfigurationLoaderTest,CompositeBytecodeTransformerTest,MethodExtensionEnhancerTest,MethodObservationEnhancerTest,BytecodeAutoConfigurationTest,BytecodeDependencyBoundaryTest,MethodExtensionAgentAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test`；随后 `./mvnw -B -ntp -pl :egon-cola-component-bytecode-starter dependency:tree -Dincludes=top.egon:egon-cola-component-access-guard-starter`
- Expected result: selected reactor exit 0；named retained/contract tests全过；dependency tree无Guard artifact；`rg`无Guard capability/bridge/core/runtime/starter/test source hits（README留待Step5）。
- Failure returns to: protocol/capability failure回Files2–3；transform/compile回Files4–6；Spring/runtime回Files7–8；dependency回File9；obsolete test/Invoker回File10。
- Completion criteria: Bridge2 + exact three capabilities；old feature fail-fast；Bytecode生产/test dependency无Guard；retained unit tests GREEN；无其他模块diff。
- Rollback: path-limited revert Step1 commit as one unit；不能只恢复protocol或单个Guard class。
- Commit paths: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/test/java/top/egon/cola/component/bytecode/bridge/DispatcherRegistryTest.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/test/java/top/egon/cola/component/bytecode/agent/AgentConfigurationLoaderTest.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/test/java/top/egon/cola/component/bytecode/agent/transform/CompositeBytecodeTransformerTest.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/test/java/top/egon/cola/component/bytecode/starter/BytecodeAutoConfigurationTest.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/test/java/top/egon/cola/component/bytecode/starter/BytecodeDependencyBoundaryTest.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/BridgeProtocol.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/BridgeCapability.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/BytecodeRuntimeDispatcher.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/EgonPolicyBridge.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/BridgeGuardedInvocation.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/BridgeConstructorInvocation.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/BridgeFailHint.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/ConstructorGuardDecision.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/main/java/top/egon/cola/component/bytecode/agent/AgentConfiguration.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/main/java/top/egon/cola/component/bytecode/agent/BytecodeAgent.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-runtime/src/main/java/top/egon/cola/component/bytecode/runtime/accessguard/GuardedInvocationEvaluator.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/pom.xml; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/pom.xml; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/test/java/top/egon/cola/component/bytecode/core/enhance/accessguard; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/test/java/top/egon/cola/component/bytecode/starter/accessguard; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/test/java/sample/bytecode/agent/AccessGuardAgentFixture.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/test/java/top/egon/cola/component/bytecode/test/agent/AccessGuardAgentIntegrationTest.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/access-guard-spring; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/ApplicationClassEnhancer.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/ClassEnhancementPlanner.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/DuplicateEnhancementDetector.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/EnhancementFeature.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/MethodEnhancementPlan.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/accessguard/AccessGuardMatcher.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/accessguard/AccessGuardMethodWrapper.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/accessguard/AccessGuardPolicy.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/accessguard/ConstructorGuardEnhancer.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/accessguard/GovernanceAnnotationFilter.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/accessguard/SyntheticBodyName.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/BytecodeAutoConfiguration.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/methodextension/MethodMetadataResolver.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-runtime/src/main/java/top/egon/cola/component/bytecode/runtime/accessguard/GuardedInvocationEvaluator.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/accessguard/AccessGuardAgentAutoConfiguration.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/accessguard/AccessGuardRuntimeAdapter.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/accessguard/CombinedPolicyDispatcher.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/test/java/top/egon/cola/component/bytecode/core/enhance/accessguard/; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/test/java/top/egon/cola/component/bytecode/starter/accessguard/; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/test/java/sample/bytecode/agent/AccessGuardAgentFixture.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/test/java/top/egon/cola/component/bytecode/test/agent/AccessGuardAgentIntegrationTest.java; egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/access-guard-spring/`
- Commit: `refactor(bytecode): remove access guard agent capability`

### Step 2 — 收缩 Access Guard 为 AOP-only 自动入口

- Requirements: `REQ-001`, `REQ-002`, `REQ-003`, `REQ-007`, `REQ-011`, `REQ-012`, `REQ-013`
- Dependencies: `Step 1 committed`
- Baseline state: Guard annotation含CONSTRUCTOR；engine/entry/kind含AGENT/CONSTRUCTOR；startup validator要求Bytecode marker并扫描constructor；Step1后AGENT配置会因找不到integration失败。
- Observable outcome: public annotation仅TYPE/METHOD；自动engine仅AOP/DISABLED；programmatic Client/OPERATION保留；旧AGENT值在Spring binding阶段失败；constructor专用运行分支消失。
- End state: Guard entry model是AOP+programmatic，Bytecode marker无任何consumer并已删除；rule/store/execution/observability semantics未改。
- Test-first gate: `Required — AccessGuardApiTest当前看到CONSTRUCTOR；reflection仍能加载Agent marker/enums；旧AGENT当前产生integration错误而非unknown-enum binding error。`
- Ordered files:

#### File 1 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/api/AccessGuardApiTest.java; egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardAutoConfigurationTest.java; egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardStartupValidatorTest.java`

- Purpose: 建立AOP-only annotation/enum/config/startup RED contract并移除constructor fixture。
- Symbols: `annotationsExposeOnlyApprovedTargets`、renamed `removedAgentEngineFailsBinding`、AOP method validation cases、reflection absence assertions。
- Repository evidence: three existing tests已经覆盖annotation target、AGENT startup和constructor/reactive/dedicated binding。
- Dependencies and consumers: ApplicationContextRunner、Guard API/autoconfiguration；不需Bytecode或Redis。
- Why now: public/config行为必须先于生产删除固定；constructor annotation test source也必须先移除以便后续编译。
- Contract/signature changes: AccessGuard target exact TYPE/METHOD；engine values AOP/DISABLED；entry AOP/PROGRAMMATIC；kind METHOD/OPERATION；marker class不可加载；AOP method/reactive/dedicated validation继续。
- Input/output and state mapping: property `engine=AGENT`→ConfigurationPropertiesBindException containing property/value；DISABLED→Client yes/Advisor no；method annotation→rule validation。
- Error and edge behavior: unknown old config不自动映射AOP；disabled component仍无beans；reactive adapter缺失和专用规则多策略错误保持。
- Implementation pseudocode:

```java
assertThat(target(AccessGuard.class)).containsExactlyInAnyOrder(TYPE, METHOD)
assertThat(AccessGuardEngine.values()).containsExactly(AOP, DISABLED)
assertThat(GuardEntryType.values()).containsExactly(AOP, PROGRAMMATIC)
assertThat(GuardInvocationKind.values()).containsExactly(METHOD, OPERATION)
contextRunner.withPropertyValues("...engine=AGENT").run(ctx -> assertBindingFailure(ctx, "engine", "AGENT"))
```

- Verification contribution: `TEST-014`,`021`–`024`及保留startup validation。
- After this file: 当前源码下target/enum/class/config assertions RED；测试本身不再声明constructor annotation。

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/api/AccessGuard.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardEngine.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardEntryType.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardInvocationKind.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardInvocation.java`

- Purpose: 收缩Guard Java入口类型并同步不可执行诊断。
- Symbols: `@Target`、three enums、GuardInvocation compact constructor。
- Repository evidence: exact types是Advisor/Client/Engine的共享入口模型；GuardInvocation只有OPERATION允许null executable。
- Dependencies and consumers: AOP invocation继续AOP/METHOD；Client继续PROGRAMMATIC/OPERATION；Step1已删AGENT consumer。
- Why now: tests锁定目标，先发布final enum/annotation shape供后续branches清理。
- Contract/signature changes: remove CONSTRUCTOR/AGENT constants；annotation target two values；GuardInvocation错误文本只提method。
- Input/output and state mapping: METHOD requires executable；OPERATION可null；arguments/attributes defensive copies不变。
- Error and edge behavior: null enum/continuation仍fail-fast；ruleId trim/validation不变；old source constructor annotation compile失败。
- Implementation pseudocode:

```java
@Target({ElementType.TYPE, ElementType.METHOD}) @interface AccessGuard { String value(); String key() default ""; }
enum AccessGuardEngine { AOP, DISABLED }
enum GuardEntryType { AOP, PROGRAMMATIC }
enum GuardInvocationKind { METHOD, OPERATION }
if (kind == METHOD && executable == null) throw new IllegalArgumentException("executable is required for methods")
```

- Verification contribution: API reflection/binding tests，`PLAN-CLAR-001`。
- After this file: entry type compile consumers必须清除AGENT/CONSTRUCTOR references；AOP/Client source仍匹配。

#### File 3 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/adapter/aop/GuardBindingResolver.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardStartupValidator.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardCoreAutoConfiguration.java`

- Purpose: 删除constructor binding/scan与Agent integration handshake。
- Symbols: `resolve(Constructor)`、validator `integrations/validateEngineIntegration/constructor loop`、CoreAutoConfiguration validator arguments。
- Repository evidence: resolver constructor overload只由validator/Bytecode adapter调用；Step1已删Bytecode caller；auto-config是唯一marker provider consumer。
- Dependencies and consumers: Spring method scan、plan/fallback/reactive/storage validation保持；AOP advisor仍用method resolver。
- Why now: final enums已收缩，清理所有startup/DI consumers后删除marker。
- Contract/signature changes: StartupValidator constructor不再接收Agent provider；`afterSingletonsInstantiated`不检查Agent；`validateGuardedType`只遍历methods。
- Input/output and state mapping: Bean types→method bindings→same plan execution validation；storage integration/reactive executor ObjectProviders保持。
- Error and edge behavior: HMAC、REDISSON、unknown rule、reactive/fallback/dedicated validation保持；不扫描constructor也不产生AOP constructor提示。
- Implementation pseudocode:

```java
afterSingletonsInstantiated() {
    validateHmacAndStorage(); resolveConfiguredRules();
    if (properties.engine != DISABLED) validateGuardedBeanMethodsOnly();
}
remove ObjectProvider<AccessGuardAgentIntegration> from fields/constructors/Bean factory
run rg and require the marker to have no remaining consumer before File4 deletes it
```

- Verification contribution: startup/auto-config tests、zero class search，`TEST-022/023/024`。
- After this file: Guard runtime wiring不再消费Agent integration；method startup validation保持。

#### File 4 — `DELETE egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/api/AccessGuardAgentIntegration.java`

- Purpose: 删除已无消费者的Guard Agent marker API。
- Symbols: `AccessGuardAgentIntegration`。
- Repository evidence: Step1删除唯一Bytecode实现，File3删除CoreAutoConfiguration与StartupValidator的provider/handshake。
- Dependencies and consumers: 删除前以`rg`确认Guard starter与全仓均无实现或注入点；AOP/Client不依赖该marker。
- Why now: 所有调用点先完成迁移，marker才能无兼容壳地删除。
- Contract/signature changes: public marker type直接删除；这是主Spec明确的不兼容能力删除，不提供deprecated窗口。
- Input/output and state mapping: 无运行时输入输出；启动时不再执行Agent integration存在性验证。
- Error and edge behavior: 若仍有源码/文档外consumer，compile或静态搜索失败并返回File3/Step1，不保留空接口掩盖问题。
- Implementation pseudocode:

```text
search repository production and test sources for AccessGuardAgentIntegration implementations and injection points
require zero remaining consumers after Step1 and File3 wiring changes
delete the marker source, then compile Guard and selected Bytecode modules to prove no hidden source dependency
```

- Verification contribution: Guard/Bytecode compile与Agent marker零引用检查。
- After this file: Guard starter不再暴露Agent integration API；File5清理constructor execution branches。

#### File 5 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/plan/GuardPlanValidator.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/DefaultRejectionHandler.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/MethodHandleFallbackHandler.java`

- Purpose: 删除constructor-only execution/rejection/fallback branches。
- Symbols: `validateExecution(Executable,...)` cast、handler kind switches。
- Repository evidence: GuardPlanValidator当前Constructor早退；两个handlers显式检查CONSTRUCTOR；AOP/Client只产生METHOD/OPERATION。
- Dependencies and consumers: FallbackMethodCache、Json parser、RejectionMode、existing handler tests。
- Why now: enum/scan已删除constructor路径，剩余branches无法编译或成为dead code。
- Contract/signature changes: validator直接要求/转换`Method` for execution validation；rejection/fallback只分METHOD/OPERATION；public method signatures保持。
- Input/output and state mapping: METHOD fallback/json/null映射不变；OPERATION fallback/null/json result contract不变；constructor无输入路径。
- Error and edge behavior: primitive RETURN_NULL、invalid fallback/json、handler failure仍原样；不扩大operation capabilities。
- Implementation pseudocode:

```java
Method method = (Method) Objects.requireNonNull(executable, "executable")
validate FALLBACK via fallbackCache.validateAndCache(method, name)
validate RETURN_JSON via jsonParser.parse(json, method.returnType)
in rejection/fallback handlers branch only OPERATION versus METHOD; remove CONSTRUCTOR errors
```

- Verification contribution: existing `GuardPlanValidatorTest`,`DefaultRejectionHandlerTest`,`MethodHandleFallbackHandlerTest` + full Guard module。
- After this file: production/test sources无AGENT/CONSTRUCTOR Guard entry references；Step2可GREEN。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl :egon-cola-component-access-guard-starter -Dtest=AccessGuardApiTest,AccessGuardAutoConfigurationTest,AccessGuardStartupValidatorTest,GuardPlanValidatorTest,DefaultRejectionHandlerTest,MethodHandleFallbackHandlerTest,DefaultAccessGuardClientTest,SpringAopAccessGuardAdvisorTest test`
- Expected result: named tests exit 0；old AGENT property fails binding with property/value；DISABLED Client保持；static search无Agent marker/enum/constructor binding symbols。
- Failure returns to: API/enum failure回File2；startup/bean failure回File3；marker残留回File4；execution regression回File5；programmatic/AOP regression若需改行为则返回Spec而非扩scope。
- Completion criteria: AOP唯一自动入口，Client/DISABLED通过，constructor source不可声明，Guard无Bytecode依赖/API，所有Step2 focused tests GREEN。
- Rollback: revert Step2 paths as a unit；若恢复AGENT必须同时回滚Step1完整Bytecode set和protocol。
- Commit paths: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/api/AccessGuardApiTest.java; egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardAutoConfigurationTest.java; egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardStartupValidatorTest.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/api/AccessGuard.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardEngine.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardEntryType.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardInvocationKind.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardInvocation.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/adapter/aop/GuardBindingResolver.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardStartupValidator.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardCoreAutoConfiguration.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/api/AccessGuardAgentIntegration.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/plan/GuardPlanValidator.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/DefaultRejectionHandler.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/MethodHandleFallbackHandler.java`
- Commit: `refactor(access-guard): keep aop as the only automatic engine`

### Step 3 — 类型化固定 admission Strategy

- Requirements: `REQ-005`, `REQ-006`, `REQ-007`, `REQ-012`, `REQ-013`
- Dependencies: `Step 2 committed`
- Baseline state: `GuardPolicy<C>`返回String id并接收nested config；Engine维护String order/config/failure switches和unchecked cast；bypass/state使用Set<String>；AdmissionPolicies只是List.of factory。
- Observable outcome: 四策略通过`GuardPolicyType`声明身份并自行从`AdmissionConfig`取配置；typed bypass/failure/local map生效；Engine不再含policy ID/config/failure switch或unchecked policy cast。
- End state: typed Strategy在现有monolith Engine内完整GREEN；职责拆分尚未发生，留给Step4。
- Test-first gate: `Required — tests先引用不存在的GuardPolicyType和新evaluate签名，预期testCompile RED；这是合同缺失而非fixture/environment failure。`
- Ordered files:

#### File 1 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/policy/AdmissionOrderContractTest.java; egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/policy/allow/AllowListPolicyTest.java; egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/policy/deny/DenyListPolicyTest.java; egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/policy/penalty/PenaltyBoxPolicyTest.java`

- Purpose: 定义typed metadata、full AdmissionConfig dispatch、typed bypass和前三项既有策略Store调用。
- Symbols: canonical order/ID/failure assertions；existing policies的`evaluate(context, admission)`与bypass断言。
- Repository evidence: AdmissionOrder及allow/deny/penalty已有独立测试；RateLimit当前只在Engine/entry tests间接覆盖，交由File2新增。
- Dependencies and consumers: policy classes、AdmissionConfig、fake Stores；无Spring/Redis。
- Why now: policy API变更先由compile RED固定，避免在Engine迁移中猜config mapping。
- Contract/signature changes: expected `type()`；evaluate receives complete AdmissionConfig；PolicyResult bypass typed set。
- Input/output and state mapping: each policy reads only own nested config；disabled→PASS；allow bypass→typed RATE/PENALTY；rate fields→RateLimitRequest unchanged。
- Error and edge behavior: DENY bypass构造失败；Store exception仍向上抛；retryAfter/remainingTokens不变。
- Implementation pseudocode:

```java
assertThat(GuardPolicyType.canonicalOrder()).containsExactly(DENY_LIST, ALLOW_LIST, PENALTY_BOX, RATE_LIMIT)
result = allowPolicy.evaluate(context, admissionWithAllow(BYPASS_RATE_LIMIT_AND_PENALTY))
assertThat(result.bypassedPolicies()).containsExactlyInAnyOrder(PENALTY_BOX, RATE_LIMIT)
assert deny/allow/penalty tests pass complete AdmissionConfig and observe only their own nested configuration
```

- Verification contribution: `TEST-018`–`020`与策略业务保持。
- After this file: 既有策略测试在current production下因缺少enum/签名而testCompile RED；File2补齐RateLimit独立合同。

#### File 2 — `CREATE egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/policy/ratelimit/RateLimitPolicyTest.java`

- Purpose: 为RateLimit Strategy补齐独立typed配置与Store请求合同。
- Symbols: `RateLimitPolicy.type/evaluate`、`RateLimitRequest`各字段、allow/reject result。
- Repository evidence: 当前RateLimit只由Engine/entry测试间接覆盖，缺少与其他三策略同层的unit test。
- Dependencies and consumers: `RateLimitPolicy`、`AdmissionConfig.RateLimitConfig`、fake `RateLimitStore`；不加载Spring或Redis。
- Why now: 新interface要求每个Strategy自行读取full AdmissionConfig，RateLimit请求字段最多，必须独立锁定映射。
- Contract/signature changes: test期望`type()==RATE_LIMIT`且`evaluate(context, admission)`；不引入新production API。
- Input/output and state mapping: rule/state/key与capacity/refill/requestedPermits逐项映射到Store；backend decision映射remainingTokens/retryAfter。
- Error and edge behavior: disabled不调用Store；Store异常向上抛；allow/reject都保留既有token/retry合同。
- Implementation pseudocode:

```java
policy = new RateLimitPolicy(capturingStore); assertThat(policy.type()).isEqualTo(RATE_LIMIT)
result = policy.evaluate(context, admissionWithRate(capacity, refillTokens, refillPeriod, requestedPermits))
assertCapturedRequestMatches(context, capacity, refillTokens, refillPeriod, requestedPermits); assertDecisionFields(result)
```

- Verification contribution: `TEST-020`的RateLimit Strategy direct contract。
- After this file: 四项Strategy均有direct typed RED合同；Files4–7完成production迁移后转GREEN。

#### File 3 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/core/DefaultGuardEngineTest.java; egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/contract/AccessGuardEntryContractTest.java`

- Purpose: 把Engine/entry fixtures切到typed list/map并保持outcome parity。
- Symbols: helper constructors、typed local map、`LOCAL_FALLBACK` scenario mapping。
- Repository evidence: 仅这两测试直接`new DefaultGuardEngine`；entry matrix覆盖8 scenarios和one final event。
- Dependencies and consumers: monolith Engine、four policies、fake stores/time limiter/event publisher。
- Why now: 生产constructor变更前先固定调用点；也落实`PLAN-CLAR-003`生产一致fixture。
- Contract/signature changes: lists/maps use `GuardPolicy`/`GuardPolicyType`；LOCAL_FALLBACK failure point改为RATE_LIMIT_BACKEND且local rate allows。
- Input/output and state mapping: same Scenario→same type/decision/resolution/businessCalls/events；stable outcome policy仍kebab-case String。
- Error and edge behavior: deny terminal、renderer failure、timeout fallback、fail-open/local-fallback均不执行业务或只按既有合同执行。
- Implementation pseudocode:

```java
policies = List.of(deny, allow, penalty, rate)
localPolicies = Map.of(PENALTY_BOX, localPenalty, RATE_LIMIT, localRate)
if (scenario == LOCAL_FALLBACK) primaryRateBackend throws StoreOperationException
failurePolicies.put(RATE_LIMIT_BACKEND, LOCAL_FALLBACK)
assert AOP result recursively equals programmatic result and finalEvents.size == 1
```

- Verification contribution: `TEST-003`–`006`,`010`,`015` parity while Engine still monolith。
- After this file: tests仍compile RED直到typed production files完成；expected outcomes未改变。

#### File 4 — `CREATE egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/GuardPolicyType.java`

- Purpose: 建立typed strategy identity、稳定ID、failure metadata与canonical order。
- Symbols: enum values、`id()`、`failurePoint()`、`canonicalOrder()`。
- Repository evidence: current String IDs散落Engine/PolicyResult/Context/State；FailurePoint switch在Engine。
- Dependencies and consumers: four policy implementations、Engine、tests；enum依赖existing `FailurePoint`。
- Why now: compile prerequisite，先定义contracts再改implementations/callers。
- Contract/signature changes: 新增closed enum，不开放plugin registration；outcome兼容字符串由后续消费者调用`type.id()`产生。
- Input/output and state mapping: enum ID exact `deny-list/allow-list/penalty-box/rate-limit`；四个FailurePoint mapping与canonical order exact。
- Error and edge behavior: enum无nullable lookup或宽松alias；未知外部字符串不得被静默映射为内置策略。
- Implementation pseudocode:

```java
enum GuardPolicyType { DENY_LIST("deny-list", DENY_LIST_STORE), ALLOW_LIST(...), PENALTY_BOX(...), RATE_LIMIT(...) }
static List<GuardPolicyType> canonicalOrder() = List.of(DENY_LIST, ALLOW_LIST, PENALTY_BOX, RATE_LIMIT)
constructor requires nonblank stable id and nonnull FailurePoint metadata for every constant
return the canonical list as an immutable value; never derive order from Spring bean ordering or EnumSet iteration
```

- Verification contribution: metadata/order tests与Engine deterministic order contract。
- After this file: strategy identity存在；File5迁移接口/bypass/state contracts。

#### File 5 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/GuardPolicy.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/PolicyResult.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/GuardContext.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardExecutionState.java`

- Purpose: 把Strategy API和bypass/execution state从String/generic改为`GuardPolicyType`。
- Symbols: non-generic `GuardPolicy`、`PolicyResult.bypassedPolicies`、`GuardContext`/`GuardExecutionState` typed sets。
- Repository evidence: current String IDs散落这些四个合同并驱动Engine switch；generic参数只为nested config cast服务。
- Dependencies and consumers: File4 enum、four policy implementations、Engine、tests；`GuardPolicy`依赖完整`AdmissionConfig`。
- Why now: identity先固定后再迁移共享接口，可让Files6–7按同一编译合同改造。
- Contract/signature changes: `String id()`→`GuardPolicyType type()`；`evaluate(GuardContext,AdmissionConfig)`；三个集合改为immutable `Set<GuardPolicyType>`。
- Input/output and state mapping: null bypass→empty immutable；merge产生新集合；外部Outcome仍在Engine用`type.id()`输出稳定字符串。
- Error and edge behavior: bypass `DENY_LIST`抛`IllegalArgumentException`；null type/result不允许；复制集合避免调用方后改。
- Implementation pseudocode:

```java
interface GuardPolicy { GuardPolicyType type(); PolicyResult evaluate(GuardContext context, AdmissionConfig admission); }
record PolicyResult(..., Set<GuardPolicyType> bypassedPolicies) { copy set; reject DENY_LIST bypass; }
GuardContext and GuardExecutionState accept immutable typed sets and merge by allocating a fresh EnumSet/copy
```

- Verification contribution: typed bypass与compile所有Strategy/Engine consumers。
- After this file: shared contracts已typed；Files6–7仍需迁移具体Strategy与Engine调用点。

#### File 6 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/plan/AdmissionConfig.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/allow/AllowListPolicy.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/deny/DenyListPolicy.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/penalty/PenaltyBoxPolicy.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/ratelimit/RateLimitPolicy.java`

- Purpose: 让每个Strategy拥有type和自身config选择，移除PolicyConfig marker。
- Symbols: four `type/evaluate` methods；AdmissionConfig nested records `implements PolicyConfig` removal。
- Repository evidence: each policy当前已有独立Store和enabled branching；只需改变config source，不改decision算法。
- Dependencies and consumers: Files4–5 interface/enum、existing Stores/RateLimitRequest；Engine File7。
- Why now: contracts已定义，先使implementations GREEN再迁移orchestrator。
- Contract/signature changes: concrete class implements non-generic GuardPolicy；`evaluate(ctx, admission)` internally selects `admission.denyList()`等。
- Input/output and state mapping: all config fields逐项原样传给Store/request；allow mode→typed bypass；rate decision→same retry/tokens。
- Error and edge behavior: disabled policy noStore call；StoreOperationException不捕获；null admission按standard NPE/argument contract。
- Implementation pseudocode:

```java
GuardPolicyType type() = RATE_LIMIT
PolicyResult evaluate(GuardContext ctx, AdmissionConfig admission) {
    RateLimitConfig config = admission.rateLimit(); if (!config.enabled()) return pass();
    decision = backend.acquire(new RateLimitRequest(ctx.ruleId(), ctx.stateVersion(), ctx.keyHash(), config...))
    return decision.allowed ? passWithTokens : reject(RATE_LIMITED, decision.retryAfter, decision.remainingTokens)
}
```

- Verification contribution: four strategy tests与existing Store contract separation。
- After this file: policies compile againstnew interface；Engine/auto-config仍需typed migration。

#### File 7 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/DefaultGuardEngine.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardCoreAutoConfiguration.java`

- Purpose: 在拆分类之前让现有Engine/Bean wiring完整消费typed Strategy。
- Symbols: policy/local fields与constructors、admission loop、store failure/penalty helpers、two qualified Beans。
- Repository evidence: Engine当前包含三处String dispatch helper和unchecked cast；auto-config用AdmissionPolicies与Map<String,...>。
- Dependencies and consumers: Files4–6 contracts/strategies、Engine/entry tests；Step4会再次修改ownership但不改变typed signatures。
- Why now: 所有策略实现已ready，orchestrator一次切换可恢复module compile。
- Contract/signature changes: `List<GuardPolicy>`和`Map<GuardPolicyType,GuardPolicy>`；canonical validation；auto-config `List.of`/typed Map；移除configFor/failurePoint/evaluatePolicy。
- Input/output and state mapping: policy.type→bypass/failure/local lookup；type.id→GuardOutcome/degradedPolicy；rate type比较替代String比较。
- Error and edge behavior: constructor拒绝null/duplicate/missing内置types及local key/type mismatch；FAIL_OPEN/LOCAL_FALLBACK/FAIL_CLOSED结果保持。
- Implementation pseudocode:

```java
policyMap = uniqueEnumMap(policies); require keys == Set.copyOf(GuardPolicyType.canonicalOrder())
for (type : canonicalOrder) { policy = policyMap.get(type); if (!state.bypassed.contains(type)) result = policy.evaluate(ctx, admission); }
on StoreOperationException use type.failurePoint and localPolicies.get(type)
outcome.policy = type.id(); if (type == RATE_LIMIT && decision == RATE_LIMITED) penaltyService.recordViolation(...)
autoConfig returns List.of(deny, allow, penalty, rate) and Map.of(PENALTY_BOX, localPenalty, RATE_LIMIT, localRate)
```

- Verification contribution: Engine/policy/entry/auto-config GREEN，zero switch/cast static audit。
- After this file: monolith Engine typed且module可编译；Step4仅迁移职责。

#### File 8 — `DELETE egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/AdmissionPolicies.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/PolicyConfig.java`

- Purpose: 删除已无consumer的List.of factory和generic marker。
- Symbols: two complete types。
- Repository evidence: File7 auto-config直接List.of；AdmissionConfig/File5 interface已不引用PolicyConfig。
- Dependencies and consumers: full `rg`必须无imports；tests已转typed。
- Why now: 所有compile consumers已迁移，安全删除避免保留双合同。
- Contract/signature changes: classes完全不存在；不加deprecated wrappers。
- Input/output and state mapping: object creation由Spring composition root直接完成；没有数据或runtime state变化。
- Error and edge behavior: 若搜索仍有consumer，返回对应File而非保留dead type；external source compatibility由README说明。
- Implementation pseudocode:

```text
delete AdmissionPolicies.java after auto-configuration and tests use explicit List.of
delete PolicyConfig.java after AdmissionConfig records and GuardPolicy no longer implement/reference it
run rg for both names and GuardPolicy<?>; require zero production/test hits
retain RateLimitAlgorithmStrategyFactory because it is a separate approved variation point
```

- Verification contribution: static zero-old-contract、module compile。
- After this file: Step3 target architecture complete，ready for focused/full GREEN。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl :egon-cola-component-access-guard-starter -Dtest=AdmissionOrderContractTest,AllowListPolicyTest,DenyListPolicyTest,PenaltyBoxPolicyTest,RateLimitPolicyTest,DefaultGuardEngineTest,AccessGuardEntryContractTest test`
- Expected result: all named tests exit 0；canonical metadata/four configs/typed bypass/8-scenario parity pass；`rg` confirms no `PolicyConfig`,`AdmissionPolicies`,`configFor`,`failurePoint(String)`,`@SuppressWarnings("unchecked")` in Engine path。
- Failure returns to: contract compile回File3；single-policy behavior回File4；Engine/order/failure parity回File5；stale symbol回File6。
- Completion criteria: typed Strategy完整且行为与基线一致；Engine仍monolith但无String/cast分派；Step3 files only。
- Rollback: revert Step3 commit；Step2 AOP-only不受影响。
- Commit paths: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/policy/AdmissionOrderContractTest.java; egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/policy/allow/AllowListPolicyTest.java; egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/policy/deny/DenyListPolicyTest.java; egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/policy/penalty/PenaltyBoxPolicyTest.java; egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/policy/ratelimit/RateLimitPolicyTest.java; egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/core/DefaultGuardEngineTest.java; egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/contract/AccessGuardEntryContractTest.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/GuardPolicyType.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/GuardPolicy.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/PolicyResult.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/GuardContext.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardExecutionState.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/plan/AdmissionConfig.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/allow/AllowListPolicy.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/deny/DenyListPolicy.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/penalty/PenaltyBoxPolicy.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/ratelimit/RateLimitPolicy.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/DefaultGuardEngine.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardCoreAutoConfiguration.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/AdmissionPolicies.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/PolicyConfig.java`
- Commit: `refactor(access-guard): type the admission policy pipeline`

### Step 4 — 拆分 Guard admission 与 execution 并收缩 Engine Facade

- Requirements: `REQ-003`, `REQ-004`, `REQ-007`, `REQ-012`, `REQ-013`
- Dependencies: `Step 3 committed`
- Baseline state: typed policy loop已GREEN但`DefaultGuardEngine`仍同时持有plan/key/policy/failure/penalty/time/rejection/event依赖和mutable PlanCapture。
- Observable outcome: admission、execution/finalization可独立构造测试；`GuardAdmission`使用同一plan snapshot投影；`DefaultGuardEngine`最终只依赖两个collaborator并委托三方法。
- End state: final Spec architecture与Spring Beans接线完成；AOP/Client/CompletionStage/Reactor shared `GuardEngine` contract不变。
- Test-first gate: `Required — 新GuardAdmissionPipelineTest/GuardExecutionCoordinatorTest先引用不存在类型，testCompile RED；DefaultGuardEngineTest先期望two-collaborator constructor而失败。`
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/core/GuardAdmissionPipelineTest.java; egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/core/GuardExecutionCoordinatorTest.java`

- Purpose: 分别锁定admission和execution阶段完整行为、failure与terminality。
- Symbols: `resolvesOneSnapshotAndUsesCanonicalPolicies`、plan/key failures、bypass、store policies、penalty；rejection modes、timeout/executor/business、safe defaults、event once。
- Repository evidence: 现有DefaultGuardEngineTest包含这些fixtures；new tests仍位于same `core` package并用AssertJ/JUnit5。
- Dependencies and consumers: new collaborator contracts、fake resolver/stores/time limiter/rejection publisher；无Spring。
- Why now: 先建立职责边界的RED，随后移动现有逻辑而非重写行为。
- Contract/signature changes: instantiate `GuardAdmissionPipeline`与`GuardExecutionCoordinator` exact constructors/methods from Spec。
- Input/output and state mapping: one invocation→one GuardAdmission(snapshot-derived outcome/execution/observability/start)→Prepared→result/outcome；same version/ticker。
- Error and edge behavior: plan/key/store failures、local fallback、penalty record failure、rejection renderer failure、terminal race；业务不在admission执行。
- Implementation pseudocode:

```java
admission = pipeline.evaluate(invocation); assertSameSnapshotFields(admission, planVersion, execution, observability)
assertPolicyCallsExactly(DENY_LIST, ALLOW_LIST, PENALTY_BOX, RATE_LIMIT); assertBusinessCalls(0)
prepared = coordinator.prepare(invocation, admission); result = coordinator.execute(prepared)
assertThat(result.outcome()).matchesExpectedDecisionResolution(); assertThat(events).singleElement()
```

- Verification contribution: Spec `TEST-001`–`009`,`016`,`017` direct ownership proof。
- After this file: testCompile RED due missing new classes/signatures；fixtures使用Step3 typed policy。

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/core/DefaultGuardEngineTest.java; egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/contract/AccessGuardEntryContractTest.java`

- Purpose: 把monolith tests重定位为thin Facade delegation与final entry parity。
- Symbols: `evaluateDelegatesAndFinishesAdmission`、`prepareDelegatesOnce`、`executeUnwrapsCoordinatorResult`；entry `engine()` fixture。
- Repository evidence: 同两文件是DefaultGuardEngine唯一直接test consumers；Step3已完成typed fixtures。
- Dependencies and consumers: new Pipeline/Coordinator；AOP advisor和DefaultAccessGuardClient不改。
- Why now: 新stage tests已定义内部行为，Engine test不再重复策略/execution矩阵。
- Contract/signature changes: `new DefaultGuardEngine(pipeline, coordinator)`；entry fixture显式构造两个collaborators。
- Input/output and state mapping: evaluate/prepare/execute每次只resolve一次admission；execute返回`GuardExecutionResult.value`；entry scenarios unchanged。
- Error and edge behavior: throwable透传、rejection business negative assertions、final event exactly one保持。
- Implementation pseudocode:

```java
engine = new DefaultGuardEngine(admissionPipeline, executionCoordinator)
assertThat(engine.evaluate(invocation)).isEqualTo(expectedAdmissionOutcome); verifySinglePipelineCall()
prepared = engine.prepare(invocation); verifyCoordinatorPrepare(invocation, admission)
assertThat(engine.execute(invocation)).isEqualTo(expectedResult.value()); assertEntryParityForAllScenarios()
```

- Verification contribution: `TEST-010`,`013`–`015`,`025`。
- After this file: tests表达final Facade/entry contracts；仍RED直到production/wiring完成。

#### File 3 — `CREATE egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardAdmission.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardAdmissionPipeline.java`

- Purpose: 创建immutable handoff并迁移plan/key/policy/failure/penalty admission逻辑。
- Symbols: record fields；public `evaluate(GuardInvocation)`；constructor validation；private outcome/failure helpers。
- Repository evidence: 逻辑来自typed `DefaultGuardEngine#evaluateAdmission`和PlanCapture；package core与Spec target一致。
- Dependencies and consumers: GuardPlanResolver、GuardKeyResolver、typed policies/local map、FailurePolicyResolver、PenaltyService、ticker/storage/engine；Facade/Coordinator。
- Why now: stage tests锁定语义；先创建admission output供Coordinator消费。
- Contract/signature changes: `GuardAdmission(outcome,execution,observability,startedAtNanos)` nonnull fields；pipeline exact evaluate signature。
- Input/output and state mapping: plan resolve once；failure uses safe execution/default observability；policy outcome retains stable strings；no mutable plan crosses boundary。
- Error and edge behavior: null/duplicate/missing policies、local map exact PENALTY/RATE/type mismatch constructor fail；plan/key/store/failure outcomes完全按Spec。
- Implementation pseudocode:

```java
GuardAdmission evaluate(invocation) {
  started = ticker.getAsLong(); snapshot = resolveOrReturnConfigFailure(invocation.ruleId)
  if (!snapshot.plan.enabled) return admission(ALLOWED, snapshot.execution, snapshot.observability, started)
  key = keyResolver.resolve(...); state = initial(snapshot, context(key.hash))
  for (type : canonicalOrder) evaluateTypedPolicyOrResolveStoreFailure(state, type)
  return new GuardAdmission(finalAdmissionOutcome, snapshot.plan.execution, snapshot.plan.observability, started)
}
```

- Verification contribution: Pipeline tests、single-snapshot/typed order/Store failure assertions。
- After this file: admission tests可GREEN；Engine仍包含duplicate old逻辑直到File5，不提交中间状态。

#### File 4 — `CREATE egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardExecutionCoordinator.java`

- Purpose: 迁移prepare、sync execute、time/rejection/fallback和finalization逻辑。
- Symbols: public constructor、`prepare(invocation,admission)`、`execute(prepared)`、private resolve helpers。
- Repository evidence: current Engine `prepare/#executeWithOutcome/#resolveAndFinish/#executeOperation/#resolveRejection`和`PreparedGuardExecution`现成生命周期。
- Dependencies and consumers: TimeLimiter、RejectionHandler、ticker、GuardEventPublisher、PreparedGuardExecution；Facade/async adapters间接。
- Why now: GuardAdmission已定义同一snapshot投影，可以无plan resolver依赖构造Prepared。
- Contract/signature changes: new internal methods exact Spec；不修改PreparedGuardExecution public/package contract。
- Input/output and state mapping: admission execution/observability/start→callbacks/finalizer；prepared→value+elapsed final outcome；event once。
- Error and edge behavior: non-admitted resolve；time/executor/business decisions；renderer failure→FAILED/AccessGuardRejectedException；duplicate terminal由Prepared guard。
- Implementation pseudocode:

```java
PreparedGuardExecution prepare(inv, admission) = new PreparedGuardExecution(
  inv, admission.outcome, admission.execution, rejectionResolver(...), failureMapper(...),
  completionMapperWithElapsed(admission.startedAt), new GuardInvocationFinalizer(publisher, admission.observability))
execute(prepared) { stage admission; if (!admitted) resolveAndFinish; else run timeLimiter/continuation;
  catch typed timeout/executor/business and resolveFailure; always finish exactly once; }
```

- Verification contribution: Coordinator tests、existing async/reactive full regression。
- After this file: execution tests可GREEN；new collaborators都存在，Facade/wiring待Files5–6。

#### File 5 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/DefaultGuardEngine.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardEngine.java`

- Purpose: 删除monolith重复逻辑并完成two-collaborator Facade。
- Symbols: Default constructor/fields和三override；GuardEngine Javadoc/default prepare保持。
- Repository evidence: GuardEngine是Advisor/Client/async稳定SPI；Spec要求signatures不变；Files3–4已承接全部logic。
- Dependencies and consumers: `GuardAdmissionPipeline`、`GuardExecutionCoordinator`；AOP/Client/CompletionStage/Reactor不变。
- Why now: 两阶段实现与tests已存在，可安全删除旧fields/helpers/PlanCapture。
- Contract/signature changes: DefaultEngine唯一constructor `(GuardAdmissionPipeline,GuardExecutionCoordinator)`；evaluate/prepare/execute仅编排；`executeWithOutcome`从Engine移除。
- Input/output and state mapping: invocation→one admission；evaluate prepare+finish admission；prepare→Coordinator.prepare；execute→Coordinator.execute(prepare).value。
- Error and edge behavior: null delegated contracts；GuardEngine default prepare仍UnsupportedOperationException供custom old implementations；Throwable透明。
- Implementation pseudocode:

```java
GuardOutcome evaluate(inv) { prepared = prepare(inv); prepared.finish(prepared.admission()); return prepared.admission(); }
PreparedGuardExecution prepare(inv) { admission = admissionPipeline.evaluate(inv); return coordinator.prepare(inv, admission); }
Object execute(inv) throws Throwable { prepared = prepare(inv); return coordinator.execute(prepared).value(); }
remove every plan/key/policy/store/time/rejection/event field and PlanCapture from DefaultGuardEngine
```

- Verification contribution: Facade tests、entry parity、static dependency audit。
- After this file: final Engine只依赖两个classes；all internal logic owner唯一。

#### File 6 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardCoreAutoConfiguration.java`

- Purpose: 将typed strategies装配到Pipeline、execution dependencies装配到Coordinator，再创建thin Engine。
- Symbols: new `accessGuardAdmissionPipeline` Bean、`accessGuardExecutionCoordinator` Bean、simplified `accessGuardEngine` Bean。
- Repository evidence: current class已经是component composition root并对defaults使用ConditionalOnMissingBean；Step3已有typed qualified beans。
- Dependencies and consumers: all existing resolvers/stores/failure/time/rejection/publisher；Advisor/Client依赖GuardEngine。
- Why now: implementations都存在，最后接线避免提前Bean class-not-found。
- Contract/signature changes: two new internal Beans with `@ConditionalOnMissingBean`；Engine factory only acceptscollaborators；qualifiers保持。
- Input/output and state mapping: properties.storage/engine strings进入Pipeline outcome metadata；System.nanoTime同供Pipeline/Coordinator；publisher same provider。
- Error and edge behavior: missing/duplicate policy constructor fail-fast；custom GuardEngine仍back off default Engine；其他默认Beans行为不变。
- Implementation pseudocode:

```java
@Bean @ConditionalOnMissingBean GuardAdmissionPipeline accessGuardAdmissionPipeline(resolvers, typedPolicies,
  typedLocalPolicies, failureResolver, penaltyService, properties, publisher) { return new ... System::nanoTime ...; }
@Bean @ConditionalOnMissingBean GuardExecutionCoordinator accessGuardExecutionCoordinator(timeLimiter,
  rejectionHandler, publisher) { return new ... System::nanoTime ...; }
@Bean @ConditionalOnMissingBean(GuardEngine.class) DefaultGuardEngine accessGuardEngine(pipeline, coordinator) = new DefaultGuardEngine(...)
```

- Verification contribution: auto-config/entry/full Guard module，`TEST-014`,`025`。
- After this file: final production call chain fully wired；Step4 focused/full tests应GREEN。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl :egon-cola-component-access-guard-starter -Dtest=GuardAdmissionPipelineTest,GuardExecutionCoordinatorTest,DefaultGuardEngineTest,AccessGuardEntryContractTest,AccessGuardAutoConfigurationTest,CompletionStageGuardExecutorTest,ReactorGuardExecutorTest,GuardObservabilityTest test`；随后 `./mvnw -B -ntp -pl :egon-cola-component-access-guard-starter test`
- Expected result: focused tests exit 0；full module 0 failures，gated Redis tests可按既有profile skip并必须报告数量；DefaultGuardEngine只有两个fields/collaborators，无PlanCapture和policy switch/cast。
- Failure returns to: admission outcome/order回File3；execution/terminality回File4；Facade delegation回File5；Spring Bean/entry path回File6；若需改变rule/store/outcome则返回Spec。
- Completion criteria: new classes独立测试、AOP/programmatic/async/reactive parity、single final event、one snapshot、thin Facade均有证据。
- Rollback: revert Step4 commit；typed monolith Step3仍是可运行回滚点。
- Commit paths: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/core/GuardAdmissionPipelineTest.java; egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/core/GuardExecutionCoordinatorTest.java; egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/core/DefaultGuardEngineTest.java; egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/contract/AccessGuardEntryContractTest.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardAdmission.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardAdmissionPipeline.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardExecutionCoordinator.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/DefaultGuardEngine.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardEngine.java; egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardCoreAutoConfiguration.java`
- Commit: `refactor(access-guard): split admission and execution engine`

### Step 5 — 同步 Guard 与 Bytecode 双语文档和迁移边界

- Requirements: `REQ-010`, `REQ-011`, `REQ-012`, `REQ-013`
- Dependencies: `Steps 1–4 committed`
- Baseline state: Guard READMEs宣称AOP/Agent/constructor并含5.3.2；Bytecode READMEs列四features、Guard Agent section/依赖和5.2.3。
- Observable outcome: 四文档只描述AOP/DISABLED+programmatic Guard和三项Bytecode Agent能力，明确AOP盲区、旧配置fail-fast、Bridge2 coherent upgrade/rollback。
- End state: docs与final source/config/tests一致；retained Bytecode/Guard rule/store章节内容不作无关改写。
- Test-first gate: `Not applicable — documentation-only change；静态RED基线由rg已确认四文档命中engine=AGENT/features=access-guard/Access Guard Agent和旧version，生产行为已在前四Steps test-first完成。`
- Ordered files:

#### File 1 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/README.md`

- Purpose: 更新英文Guard安装、capability、engine、AOP边界、测试和migration。
- Symbols: intro/flow/capability/requirements/dependency；§6/§7/§23–§25/§31–§38 relevant sections。
- Repository evidence: current doc精确含Agent capability、constructor flow、engine=AGENT、Agent tests/checklist/examples和5.3.2。
- Dependencies and consumers: application developers/operators；must match Steps2–4 classes/config。
- Why now: final code完成后才能准确描述，不记录过渡状态。
- Contract/signature changes: engine values only AOP/DISABLED；TYPE/METHOD；programmatic Client replacement；Facade/Pipeline/Coordinator internal overview；protocol2 migration link。
- Input/output and state mapping: old config/source usage→compile/startup failure→AOP/Client migration；no runtime data。
- Error and edge behavior: private/static/constructor/self-invocation明确不自动治理；不承诺Client自动替换；rollback whole artifact set。
- Implementation pseudocode:

```text
remove every Guard Agent dependency/config/feature/constructor/static/synchronized usage block
rewrite execution-engine chapter to AOP default, DISABLED programmatic-only, and explicit proxy limitations
add migration matrix for engine=AGENT/features=access-guard/constructor annotations and coordinated protocol2 restart
update repository artifact examples and document target from 5.3.2 to 5.3.3
```

- Verification contribution: English half of `TEST-032`/`REQ-010` static review。
- After this file: English Guard README不再提供可执行Agent instructions；existing rule/store/API sections保持。

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/README.zh-CN.md`

- Purpose: 与File1逐节同步中文Guard合同和迁移。
- Symbols: 对应能力、依赖、注解、完整配置、执行引擎、错误、测试、上线、迁移、示例章节。
- Repository evidence: 中文文档当前与英文平行并含AGENT/构造器/5.3.2内容。
- Dependencies and consumers: 中文业务/运维读者；术语必须使用实际Java symbols。
- Why now: 英文结构已定，逐节同步防止双语漂移。
- Contract/signature changes: 与File1相同，不新增中文独有行为。
- Input/output and state mapping: old配置/源码→相同fail-fast/migration结果；版本示例5.3.3。
- Error and edge behavior: 明确AOP代理限制、Client显式替代和constructor无自动等价路径。
- Implementation pseudocode:

```text
按英文最终章节逐项删除“Agent模式/构造器治理/features=access-guard”操作说明
保留Method/TYPE、AOP/DISABLED、程序化API、异步/响应式、Store/failure/outcome真实合同
同步添加Bridge 2.0整套artifact升级和完整回滚说明
逐项比对代码标识、配置键、命令和5.3.3版本，不做自由扩写
```

- Verification contribution: Chinese half static/doc parity。
- After this file: Guard bilingual docs语义一致。

#### File 3 — `MODIFY egon-cola-components/egon-cola-component-bytecode/README.md`

- Purpose: 更新英文Bytecode module/feature/Agent/protocol/dependency边界。
- Symbols: module table、runtime command/YAML feature list、deleted Access Guard Agent section、overlap order、status/protocol、compatibility。
- Repository evidence: current README listsoptional Guard integration、four features、full Guard Agent section and5.2.3 examples。
- Dependencies and consumers: Bytecode deployers；must preserve Method Extension/Observation/Executor/plugin docs。
- Why now: Step1 final retained feature set和protocol已验证。
- Contract/signature changes: feature exact executor/observation/method-extension；starter不依赖Guard；protocol2 coherent artifacts；no Guard integration section。
- Input/output and state mapping: old feature→parse failure；1.x/2.x mix→startup failure；retained args→same behavior。
- Error and edge behavior: Method Extension AGENT remains valid；Observation constructors remain valid；不要把所有Agent或constructor文档误删。
- Implementation pseudocode:

```text
remove access-guard from feature examples/YAML/module optional-integration description
delete only Access Guard Agent semantics and its cross-feature rejection paragraph
state retained order Method Extension -> Observation -> business body where both apply
document Bridge 2.0 same-version upgrade/rollback and update 5.2.3 artifact examples to 5.3.3
```

- Verification contribution: retained docs boundary + static removed-feature gate。
- After this file: English Bytecode README只宣称三项runtime capabilities并保留plugin docs。

#### File 4 — `MODIFY egon-cola-components/egon-cola-component-bytecode/README.zh-CN.md`

- Purpose: 同步中文Bytecode retained feature和protocol migration。
- Symbols: 与File3对应模块、安装、features、Method Extension、删Guard、指标状态、兼容章节。
- Repository evidence: 当前中文doc与英文平行且包含Guard Agent/5.2.3。
- Dependencies and consumers: 中文部署/开发读者；必须与File3和source enum一致。
- Why now: 英文final结构固定后逐段同步。
- Contract/signature changes: no Chinese-only behavior；三features/protocol2/dependency exact。
- Input/output and state mapping: 配置与升级/回滚结果同File3。
- Error and edge behavior: 保留Observation构造器与Method Extension Agent；删除只限Guard。
- Implementation pseudocode:

```text
按英文README逐节同步三项feature、无Guard依赖和protocol2说明
删除Access Guard Agent配置/语义/组合顺序/诊断，保留Method Extension Agent全文
保留Observation constructor timing、Executor、Maven plugin、privacy与Actuator合同
执行双语关键词和版本比对，任何单边段落差异返回File3或File4修复
```

- Verification contribution: bilingual parity、`REQ-009`–`013` docs gate。
- After this file: four docs complete，ready for final quality gates。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `! rg -n 'engine:\s*AGENT|features=[^[:space:]]*access-guard|Access Guard Agent|Agent mode.*Access Guard|Access Guard Agent 模式|构造器治理|Constructor Governance|5\.3\.2|5\.2\.3' egon-cola-components/egon-cola-component-access-guard-starter/README.md egon-cola-components/egon-cola-component-access-guard-starter/README.zh-CN.md egon-cola-components/egon-cola-component-bytecode/README.md egon-cola-components/egon-cola-component-bytecode/README.zh-CN.md`
- Expected result: command exit 0 with no matches；manual side-by-side review confirms retained Agent/Observation constructor docs still present and Guard AOP/Client migration complete。
- Failure returns to: Guard English/Chinese mismatch回Files1–2；Bytecode retained/Guard deletion mismatch回Files3–4；若docs需要未实现能力则返回对应code Step或Spec。
- Completion criteria: four READMEs无removed usage/stale versions，保留全部unchanged capability合同并包含2.0 rollout/rollback。
- Rollback: revert Step5 docs commit；代码不受影响，但未修复前不得发布。
- Commit paths: `egon-cola-components/egon-cola-component-access-guard-starter/README.md; egon-cola-components/egon-cola-component-access-guard-starter/README.zh-CN.md; egon-cola-components/egon-cola-component-bytecode/README.md; egon-cola-components/egon-cola-component-bytecode/README.zh-CN.md`
- Commit: `docs(guard): document aop-only guard migration`

## 8. Test, Validation, and Quality Gates

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| 1 RED Bytecode | repo root | Step1 focused selector command after File1/before production | protocol/parser/dependency | fails only because major/capability/method/dependency仍旧 | Step1 File1 assertions或Spec | `REQ-008`,`011`; module |
| 2 GREEN Bytecode | repo root | Step1 selected reactor command | bridge/core/runtime/agent/starter retained tests | exit0；all named tests pass | Step1 Files2–9 | `REQ-008`,`009`,`011`; selected reactor |
| 3 Bytecode dependency | repo root | `./mvnw -B -ntp -pl :egon-cola-component-bytecode-starter dependency:tree -Dincludes=top.egon:egon-cola-component-access-guard-starter` | resolved dependency graph | no matching artifact | Step1 File8 | `REQ-008`,`031`; static/build |
| 4 RED AOP surface | repo root | Step2 focused tests after File1 | annotation/enums/config | fails for current constructor/AGENT surface | Step2 File1 | `REQ-001`,`002`,`011`; module |
| 5 GREEN AOP surface | repo root | Step2 verification command | API/autoconfig/validator/handlers | exit0 | Step2 Files2–4 | `REQ-001`–`003`,`007`; module |
| 6 RED typed policy | repo root | Step3 focused tests after Files1–2 | new enum/signature | testCompile/focused RED for missing typed contract | Step3 Files1–2 | `REQ-005`,`006`; module |
| 7 GREEN typed policy | repo root | Step3 verification command | policies/typed monolith/entry | exit0；8 scenario parity | Step3 Files3–6 | `REQ-005`–`007`; module |
| 8 RED Engine split | repo root | Step4 focused tests after Files1–2 | new stages/Facade | testCompile RED for missing classes/constructor | Step4 Files1–2 | `REQ-004`,`007`; module |
| 9 GREEN Engine split | repo root | Step4 focused then full Guard commands | admission/execution/AOP/async/reactive | focused+module exit0；0 failures；skips reported | Step4 Files3–6 | `REQ-003`,`004`,`007`,`012`; module |
| 10 RPC context | repo root | `./mvnw -B -ntp -pl :egon-cola-component-rpc-starter -am -Dtest=RpcProviderAccessGuardComponentTest,RpcAccessGuardExceptionMapperTest -Dsurefire.failIfNoSpecifiedTests=false test` | unchanged RPC→Guard AOP/exception | exit0；RPC production diff empty | Steps2–4 if regression | `REQ-007`,`013`; cross-component static/process |
| 11 Bytecode real premain/Invoker | repo root | `./mvnw -B -ntp -pl :egon-cola-component-bytecode-test -am verify` | retained forked JVM + 17 Invoker projects | exit0；Executor/Observation/Method Extension markers present；no Guard scenario | Step1 | `REQ-009`,`012`; short-lived test JVM |
| 12 Docs/static | repo root | Step5 `rg` command plus source zero-reference searches | four docs + removed symbols | exit0/no banned matches；retained docs present | Steps1/2/5 | `REQ-001`,`008`–`013`; static |
| 13 Component reactor | repo root | `./mvnw -B -ntp -f egon-cola-components/pom.xml test` | component regression | exit0 if baseline validation-provider gap is resolved；otherwise record exact unchanged `NoProviderFoundException` separately and rely on successful focused gates | repository baseline owner or owning Step | `REQ-012`; broad reactor, not live topology |
| 14 Hygiene/scope | repo root | `git diff --check`；`git status --short`；path comparison against §5 inventory | whitespace + scope | no errors；only approved paths/commits；unrelated dirty files untouched | owning Step | `REQ-013`; static |
| 15 User-controlled runtime | target application | audit old configs/call sites；compile/start with coherent artifacts；invoke representative AOP/Client paths | production-like migration | no old AGENT configs；AOP/Client outcomes correct；retained Bytecode features active | application/release owner | `REQ-002`,`003`,`009`,`011`; external proof |

Final source zero-reference methods:

```bash
! rg -n 'ACCESS_GUARD|AccessGuardRuntimeAdapter|CombinedPolicyDispatcher|GuardedInvocationEvaluator|BridgeGuardedInvocation|BridgeConstructorInvocation|guardConstructor|invokeGuarded' \
  egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-{bridge,core,runtime,agent,starter,test} \
  --glob '!**/target/**'
! rg -n 'AccessGuardAgentIntegration|AccessGuardEngine\.AGENT|GuardEntryType\.AGENT|GuardInvocationKind\.CONSTRUCTOR|resolve\(Constructor' \
  egon-cola-components/egon-cola-component-access-guard-starter/src --glob '!**/target/**'
```

这些 future gates 不能证明真实 Redis、多JVM、生产代理拓扑、不可中断I/O取消或真实发布脚本；交付必须单列这些外部边界。

## 9. Migration, Compatibility, Rollout, and Rollback

### 9.1 Data/schema/generated contracts

N/A。主规格 §11明确无数据库/Flyway；Store key、Lua、TTL、rule schema和状态不变；没有IDL或generated output。不得修改任何 migration。

### 9.2 Source/config compatibility

| Old surface | New treatment | Required consumer action |
| --- | --- | --- |
| constructor `@AccessGuard` | source compile break | move governance to proxied method or explicit Client；no automatic equivalent |
| `AccessGuardEngine.AGENT` / `engine=AGENT` | enum/config break；binder fail-fast | remove property or set AOP；audit proxy boundary |
| `GuardEntryType.AGENT`/`GuardInvocationKind.CONSTRUCTOR`/marker | source/binary removal | external extenders migrate or remove integration |
| `GuardPolicy<C>`、String IDs/bypass | source break for extenders | implement `type()` + `evaluate(context,AdmissionConfig)` and typed sets |
| `features=access-guard` | Agent parser failure | delete feature；retain only executor/observation/method-extension |
| Bridge Guard records/methods | binary removal；protocol2 | upgrade all Bytecode artifacts together and restart |
| `GuardOutcome.policy` | compatible | no consumer change；stable kebab-case projection |

### 9.3 Rollout order

1. Before deployment, `rg` application source/config/deployment scripts for `engine=agent`、`features=access-guard`、constructor/private/static/self-invocation Guard and removed Java types。
2. Move public Spring Bean methods to default/explicit AOP；for non-proxy paths choose already-approved explicit `AccessGuardClient` or adjust Bean boundary。
3. Build/test one coherent artifact set；Bytecode agent/bridge/core/runtime/starter and Guard starter use same repository version，Bridge protocol reports2.0。
4. Restart the application/JVM；premain transformations cannot be changed in process。
5. Smoke AOP sync、CompletionStage/Reactor where used、programmatic Client、and retained Bytecode Executor/Observation/Method Extension。
6. Inspect existing Guard outcomes/events/metrics and Bytecode status endpoint without adding new signals。

### 9.4 Rollback

- Source rollback is commit-by-commit only before release：Step5 docs→Step4 split(to typed monolith)→Step3 typed policy→Step2 Guard surface。Step1/Step2跨组件必须成对回滚才能恢复Agent。
- Deployment rollback must restore the previous complete Guard starter + Bytecode agent/bridge/core/runtime/starter artifact set, restore old config, and restart JVM。不得混用protocol1/2。
- No DB/Redis data rollback is required；rule/state key formats不变。若新代码已写Store state，旧代码仍按原合同可读；本任务不新增state format。
- Maven semantic release version/BOM change不在本计划；release owner另行选择符合仓库策略的发布线。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| `REQ-001` | 主规格 §4/§7.3.5 | 1,2 | Bytecode Guard slice；Guard marker/enums/annotation | Gates1–5,12 | zero Agent/constructor symbols |
| `REQ-002` | 主规格 §4/§16 | 2,5 | Guard API/config/README | Gates4–5,9,12,15 | AOP only automatic entry |
| `REQ-003` | 主规格 §4/§9 | 2,4 | Client/Engine preserved；Facade/entry tests | Gates5,9,10,15 | DISABLED Client and entry parity |
| `REQ-004` | 主规格 §7.0/§7.3 | 4 | three new core types、Engine、auto-config、tests | Gates8–9 | independent stages + thin Facade |
| `REQ-005` | 主规格 §7.3.2/§9 | 3 | GuardPolicyType、GuardPolicy、four policies、Engine | Gates6–7 | no String switch/unchecked cast |
| `REQ-006` | 主规格 §7.3.2/§10 | 3,4 | typed sets/state、pipeline validation/tests | Gates6–9 | exact order/coverage/deny invariant |
| `REQ-007` | 主规格 §7.3.3–§7.3.5 | 2,3,4 | Guard execution/entry/state tests | Gates5,7,9,10 | baseline outcomes/terminality preserved |
| `REQ-008` | 主规格 §7.3.5/§8 | 1 | all Bytecode Guard production/test/POM paths | Gates1–3,11–12 | vertical slice/dependency absent |
| `REQ-009` | 主规格 §7.3.5/§14 | 1,5 | retained tests + Bytecode docs | Gates2,11–13 | three retained capabilities pass |
| `REQ-010` | 主规格 §16.4 | 5 | four README files | Gate12 | bilingual docs no removed usage |
| `REQ-011` | 主规格 §7.3.5/§16 | 1,2,5 | Bridge2、parsers/config tests、docs | Gates1–5,11–12,15 | old config/mixed artifacts fail-fast |
| `REQ-012` | 主规格 §14 | 1,2,3,4,5 | all tests/docs/gates | Gates1–15 | layered proof with explicit gaps |
| `REQ-013` | 主规格 §3.3/§8/§19 | 1,2,3,4,5 | only §5 inventory | Gates10,12–14 | unrelated dirty/RPC/Tianshu/DB/UI untouched |

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| `RISK-001` | External apps may depend on Agent blind-spot coverage | Step2/5 + rollout | primary `RISK-001`; repo cannot see external usages | application owner | Tracked：mandatory source/config audit and smoke |
| `RISK-002` | Cross-module Bytecode deletion can break retained transforms | Step1 core/bridge/agent/test | shared enhancer/bridge pipeline | Bytecode owner | Mitigated：one atomic commit + retained unit/premain/Invoker |
| `RISK-003` | Engine movement can duplicate/miss final event | Step4 core/tests | current monolith finalizer branches | Guard owner | Mitigated：stage tests + entry/async/reactive single-terminal assertions |
| `RISK-004` | Typed migration can alter stable policy strings/local fallback fixture | Step3 | String IDs and custom deny local fixture | Guard owner | Mitigated：stable ID tests + `PLAN-CLAR-003` |
| `RISK-005` | Protocol1/2 artifacts may be mixed | Step1/5/rollout | bridge registry exact major check | release owner | Tracked：coherent upgrade/restart/complete rollback only |
| `RISK-006` | Broad `-am test` baseline currently fails before Bytecode | Chapter8 Gate13 | 2026-08-23 `NoProviderFoundException` in upstream Guard reactor run；direct Guard + selected Bytecode pass | repository build owner | Mitigated for task：focused selectors and direct module gates；report broad result honestly |
| `DECISION-001` | Primary Spec remains Review | all | user requested Plan but did not state acceptance | User | Review required before execution |

不存在需要本计划自行决定的业务/API/schema/security重大 blocker；若用户改变programmatic保留、deprecated Agent窗口、第三方policy或Maven release version范围，必须先修订Spec。

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

- Agent入口：Steps1–2删除完整Bytecode/Guard切片，而非删除整个Bytecode Agent。
- 只保留AOP：Step2保留AOP自动入口、Client和DISABLED的已确认边界。
- Engine拆分：Step4精确实现two collaborators + immutable handoff + thin Facade。
- Strategy：Step3落实typed closed Strategy；没有Abstract Factory/plugin registry。
- 文档：Step5只修改四份目标README及必要版本/迁移内容。
- 别的不要动：所有commit/gate均path-limited；RPC仅测试，DB/UI/Tianshu/Yuheng/identity dirty paths不改。

### 12.2 Spec consistency

- 5 Steps逐项覆盖`REQ-001`–`REQ-013`，没有新增行为、dependency、module、schema、page或signal。
- `PLAN-CLAR-001`–`004`均是小型、可逆、源码证据支持的诊断/test fixture/docs一致性细节。
- simplicity audit证明两个collaborator、one record、one enum均有当前问题和direct alternative对比；没有fetch-then-forward或speculative abstraction。
- legacy Agent/constructor内容已按主Spec排除，retained Bytecode/Guard运行合同仍作为回归边界。

### 12.3 Repository executability

- 所有Create/Modify/Delete路径来自当前tree/`rg`证据；新路径符合现有`core/policy`与JUnit package风格。
- Step1跨模块原子，Steps2→3→4按compile dependency串行；shared files逐Step声明不同symbols。
- 每个Step都有RED reason、minimum GREEN、exact cwd/commands、failure return、rollback和commit paths。
- 当前direct Guard baseline 146 tests/5 skipped、selected Bytecode baseline 19 tests通过；broad reactor既有validation-provider gap已显式隔离，未伪称全绿。

### 12.4 Test and release completeness

- Unit/contract/auto-config/async/reactive、retained Agent real-premain/Invoker、RPC context、dependency/static/docs/hygiene均有future gates。
- Database/Flyway/frontend/generated contract为evidence-backed N/A。
- Rollout要求调用点审计、coherent artifacts、JVM restart和application smoke；rollback恢复完整protocol1 artifact set。
- 所有未来命令是执行说明，不是本Plan写作阶段已经完成的实现证明。

### 12.5 Final verdict

PASS — Ready for user review
