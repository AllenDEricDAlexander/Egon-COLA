# Agent Flow 扁平化组件实施计划

| Field              | Value                                                                                                                                                                         |
|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Document           | `2026-09-04-19-02-agent-flow-component-implementation.md`                                                                                                                     |
| Template Version   | `4`                                                                                                                                                                           |
| Status             | `Review`                                                                                                                                                                      |
| Created            | `2026-09-04 19:02 CST`                                                                                                                                                        |
| Updated            | `2026-09-04 19:02 CST`                                                                                                                                                        |
| Owner              | `Egon-COLA maintainer`                                                                                                                                                        |
| Repository         | `Egon-COLA`                                                                                                                                                                   |
| Scope              | `egon-cola-components` 下新增单模块、功能包扁平的 `egon-cola-component-agent-flow-starter`，并完成父 POM、Components BOM 与组件文档接线                                                                 |
| Source Requirement | 阅读参考工程 Agent Workflow 后，将 Spring AI + Google ADK 能力转换成非 DDD、默认关闭、离线可测的 Components Starter；用户确认全部推荐项并于 2026-09-04 明确要求开始编写两个阶段的 Plan                                           |
| Baseline Revision  | `main@cd83ae3a6a8b60ab3bbbb4b76f6fad4b87f07a0d`；保留现有未提交 Spec、Plan 与 `egon-cola-xingyuan-admin-web-shared/tsconfig.app.tsbuildinfo`，执行时仅按 Step 路径提交                            |
| Implements Spec    | [Agent Flow 扁平化组件设计](../spec/2026-09-04-09-34-agent-flow-component.md)                                                                                                        |
| Spec Status        | `Accepted`                                                                                                                                                                    |
| Spec Revision      | `Updated 2026-09-04 19:02 CST`，基于 `main@cd83ae3a6a8b60ab3bbbb4b76f6fad4b87f07a0d` 的当前工作区决策同步版本                                                                                |
| Effective Specs    | [Agent Flow 扁平化组件设计](../spec/2026-09-04-09-34-agent-flow-component.md); [`egon-cola-components` 架构 §1-§5](../../../egon-cola-components/egon-cola-components-architecture.md) |
| Depends On Plans   | `None`                                                                                                                                                                        |
| Supersedes         | `None`                                                                                                                                                                        |
| Superseded By      | `None`                                                                                                                                                                        |
| Related Plans      | [Agent Deep Research Archetype 实施计划](2026-09-04-19-02-agent-deep-research-archetype-implementation.md)                                                                        |

## 1. Summary

本 Plan 用七个顺序 Step 实现 Accepted Agent Flow Spec。先建立 Maven 依赖与扁平模块边界，随后依次交付严格配置绑定和图校验、三种
Workflow Strategy、原子 Registry、会话互斥与执行服务、Spring Boot 自动配置，最后导出 BOM 并补齐中英文文档和全组件回归。每个
Step 都先形成可观察 RED，再补生产实现到 GREEN，并使用路径限定的独立提交。

组件只接受宿主提供的具名 Spring AI `ChatModel`，内部使用 Google ADK 0.7.0 构建 `LlmAgent`、`SequentialAgent`、
`ParallelAgent`、`LoopAgent` 与 `InMemoryRunner`。本 Plan 不新增 HTTP、MCP、供应商密钥、数据库、平台、Archetype 或
UI；不启动服务、浏览器、Docker、真实模型或网络依赖。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [Agent Flow 扁平化组件设计](../spec/2026-09-04-09-34-agent-flow-component.md)
- Status: Accepted
- Revision: 2026-09-04 19:02 CST；repository baseline `main@cd83ae3a6a8b60ab3bbbb4b76f6fad4b87f07a0d`。
- Approval evidence: 用户先确认“全部按照推荐”，随后回复“确认，开始写plan吧，两个阶段都需要写”，接受扁平单 Starter、Java 21 /
  Boot 3.5.16、Spring AI 1.1.8、Google ADK 0.7.0、默认关闭与 V1 内存会话边界。

### 2.2 Effective Spec set

| Role                   | Spec/link                                                                                       | Status/revision                        | Effective sections | Why included                            |
|------------------------|-------------------------------------------------------------------------------------------------|----------------------------------------|--------------------|-----------------------------------------|
| Primary                | [Agent Flow 扁平化组件设计](../spec/2026-09-04-09-34-agent-flow-component.md)                          | Accepted, Updated 2026-09-04 19:02 CST | 全文 §1-§20          | 唯一功能与合同来源                               |
| Normative architecture | [`egon-cola-components` 架构](../../../egon-cola-components/egon-cola-components-architecture.md) | Current repository document            | §1-§5              | 定义 Components 父子层级、轻量 Starter、自动配置与测试位置 |

### 2.3 Superseded or excluded content

参考工程 `/Users/mario/Downloads/ai-agent-scaffold-lite-main` 仅是行为证据，不是可复制源码或第二套架构规范。其 DDD 包、动态
Bean 注册、Controller、provider/MCP/Skills 配置和 userId-only Session 缓存均排除。后续 Deep Research Archetype 不在本 Plan
内实现，只能在本 Plan 完成并通过 focused verify 后开始。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement                           | Observable acceptance                       | Implementation impact |
|-------------|---------------------|-----------------------------------------------|---------------------------------------------|-----------------------|
| `REQ-001`   | Spec §4             | 单 Starter、模块内测试、功能包扁平                         | 无 DDD/COLA 业务层目录，测试在同模块                     | Steps 1, 7            |
| `REQ-002`   | Spec §4             | Java 21、Boot 3.5.16、Spring AI 1.1.8、ADK 0.7.0 | effective POM 精确，runtime 无 `google-adk-dev` | Steps 1, 7            |
| `REQ-003`   | Spec §4             | 宿主用具名 `ChatModel` 提供模型                        | 缺 Bean 启动失败，组件无 provider 密钥属性               | Steps 3, 6            |
| `REQ-004`   | Spec §4             | 支持 leaf、Sequential、Parallel、Loop root         | 四种根形成真实 ADK 类型                              | Steps 2, 3            |
| `REQ-005`   | Spec §4             | 字段与图完整 fail-closed 校验                         | 缺引用、环、多父、不可达、非法 Loop 全失败                    | Step 2                |
| `REQ-006`   | Spec §4             | 全量编译后原子发布 Registry                            | 任一 Flow 失败则清理并不暴露半成品                        | Steps 3, 4            |
| `REQ-007`   | Spec §4             | tuple 会话身份及显式创建/删除                            | `flowId+userId+sessionId` 隔离，专用异常           | Steps 4, 5            |
| `REQ-008`   | Spec §4             | 同步和流式保留 ADK Event                             | 有序不可变 `List<Event>` 与 `Flowable<Event>` 等价  | Step 5                |
| `REQ-009`   | Spec §4             | 同会话互斥、跨会话并行                                   | busy fail-fast，结束后释放                        | Step 5                |
| `REQ-010`   | Spec §4             | 总时限、无重试、取消/错误释放                               | upstream disposal、partial event 保留、guard 释放 | Step 5                |
| `REQ-011`   | Spec §4             | Starter 默认关闭并严格绑定                             | disabled 无 Bean；未知 key/非法配置启动失败             | Steps 2, 6            |
| `REQ-012`   | Spec §4             | Context 关闭全部 Runner 且 delay-error             | 有界等待、逐个尝试、最终 CLOSED                         | Steps 4, 5            |
| `REQ-013`   | Spec §4             | 安全 allowlist 日志                               | 不记录身份、prompt、response、state、secret          | Steps 4, 5, 7         |
| `REQ-014`   | Spec §4             | BOM 只导出 Egon Starter                          | 消费者无版本引入，三方坐标不进入 Egon BOM                   | Step 7                |
| `REQ-015`   | Spec §4             | 全验证离线确定性                                      | fake model/agent/runner，不访问外部系统             | Every Step            |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

1. 先创建模块与 dependency contract，证明 Spring AI 1.1.8 覆盖 ADK adapter 的 1.1.0 传递版本，并显式排除 `google-adk-dev`。
2. 配置 record、Validation group 与 DFS 图校验必须先于任何 ADK 对象创建，避免运行时才发现非法引用。
3. Strategy Factory 只解决三种已知 workflow 变化点；`AgentFlowFactory` 在完整校验后构建叶子和组合节点。
4. Registry Factory 在临时集合中编译全部 Flow，失败时关闭已建 Runner，成功后一次性交给不可变 Registry。
5. Guard 和 Service 共用一个 subscription-scoped 执行管线，统一同步/流式、超时、取消、删除竞态与 close。
6. 所有核心对象稳定后再接 Spring AutoConfiguration，保持显式 Bean 名、Qualifier 和 override seam。
7. 最后把稳定 artifact 加入 BOM/文档，并运行 focused、dependency、components reactor 三层 Gate。

### 4.2 Test-first strategy

| Behavior     | RED point before implementation               | Minimum GREEN                         | Refactor/wiring allowed       |
|--------------|-----------------------------------------------|---------------------------------------|-------------------------------|
| 依赖边界         | Contract test/Dependency tree 对当前缺模块失败        | 版本精确、dev/Web/DB 均不在 runtime           | 只调 POM/module skeleton        |
| 配置/图         | Binding/Validator tests 引用尚不存在类型或非法图未拒绝       | record defaults、严格绑定、O(V+E) 图校验       | 仅抽取无状态 validator helper       |
| Workflow 构建  | Strategy/Factory tests 对四种根失败                 | 真实 ADK agent 类型、顺序、outputKey、loop cap | 只保留三策略 + 一个 factory           |
| Registry 原子性 | Registry tests 对 partial publish/cleanup 失败   | 完整不可变快照或无 Registry                    | 临时 map 后单次 publish            |
| 会话执行         | Guard/Service tests 对并发、取消、timeout、close 失败   | tuple 互斥、一次执行/订阅、全部清理                 | 共享内部 pipeline，不复制 sync/stream |
| 自动配置         | ContextRunner 对 disabled、strict bind、Bean 名失败 | 条件启用、显式 Bean、override 全通过             | 仅配置装配，不放业务分支                  |
| 发布/文档        | Static contract/BOM consumer 对缺导出和禁用项失败       | BOM 只导出 Starter，文档边界完整                | 无生产逻辑重构                       |

### 4.3 Sequential and parallel boundaries

| Step   | Depends on | May run in parallel with | Must not overlap with            | Reason                         |
|--------|------------|--------------------------|----------------------------------|--------------------------------|
| Step 1 | None       | None                     | components parent、新 module POM   | 版本与 module 必须先解析               |
| Step 2 | Step 1     | None                     | config/autoconfigure properties  | 后续 factory 依赖 validated config |
| Step 3 | Step 2     | None                     | workflow/runtime factory         | 建树依赖图校验与模型解析                   |
| Step 4 | Step 3     | None                     | registry/runtime descriptor      | 原子发布依赖完整 factory               |
| Step 5 | Step 4     | None                     | api/execution/service exceptions | Session 调用依赖 runtime Registry  |
| Step 6 | Steps 2-5  | None                     | AutoConfiguration/imports        | 所有待装配 Bean 已稳定                 |
| Step 7 | Steps 1-6  | None                     | BOM/docs/contract finalization   | 公共发布面最后冻结                      |

### 4.4 Commit boundaries

七个 Step 各一个提交。执行时只能 `git add --` 当前 Step 的 `Commit paths`；不 stage 两份 Spec/Plan、旧 Plan、用户已修改的两阶段
Spec 或 tsbuildinfo。若某 Step RED/GREEN 未闭环，不提交并返回该 Step 的首个失败文件。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element       | Spec necessity verdict/section | Current repository evidence                   | Direct/reuse alternative | Interaction/implementation cost | Plan decision         |
|--------------------|--------------------------------|-----------------------------------------------|--------------------------|---------------------------------|-----------------------|
| 单扁平 Starter        | Spec §6.1 Add                  | rule-engine 是单 Starter + 功能包                  | DDD 六层复制                 | 单模块、约 31 production types       | Implement             |
| Spring AI adapter  | Spec §7/DEC-003 Reuse          | ADK 0.7 sources有 `SpringAI(ChatModel,String)` | 自写 `BaseLlm` adapter     | 高协议/流式风险                        | Reuse in Step 3       |
| 三类 Strategy        | Spec §13 Selected              | ADK builders按类型不同                             | 一个 switch 巨方法            | 5 小类隔离唯一变化点                     | Implement Step 3      |
| 自定义 Runner/Session | Spec §13 Rejected              | `InMemoryRunner` 和 `BaseSessionService` 已提供   | 重写内存存储                   | 重复且增加生命周期风险                     | Do not implement      |
| Registry Factory   | Spec §7 Add                    | Components 有 registry/factory惯例；多 Flow 需原子发布  | AutoConfiguration 逐个注册   | 会暴露半成品                          | Implement Step 4      |
| sync + stream 两套引擎 | Spec §9 Rejected               | ADK 唯一 `runAsync` Flowable                    | 两份调用链                    | 会漂移取消/超时语义                      | 一个 pipeline in Step 5 |
| HTTP/MCP/provider  | Spec §§9/12 N/A                | component 是宿主内核边界                             | 同时做业务 Starter            | 泄漏密钥和传输职责                       | Exclude               |

### 4.6 Change-unit Dependency Matrix

| Change unit              | Requirements                       | Proof/RED point                | Compile/runtime prerequisites | Produces                    | Consumers/unblocks      | Owning Step |
|--------------------------|------------------------------------|--------------------------------|-------------------------------|-----------------------------|-------------------------|-------------|
| module/dependencies      | REQ-001, REQ-002, REQ-015          | contract/dependency RED        | components parent             | resolvable Starter          | all Java units          | Step 1      |
| configuration graph      | REQ-004, REQ-005, REQ-011          | binding/graph tests            | ValidationUtils               | immutable validated config  | factories/autoconfig    | Step 2      |
| workflow compiler        | REQ-002, REQ-003, REQ-004, REQ-006 | ADK type tests                 | Step 2 + ChatModel map        | BaseAgent/Runner factories  | Registry                | Step 3      |
| atomic registry          | REQ-006, REQ-007, REQ-012, REQ-013 | partial failure/close tests    | Step 3                        | immutable runtime map       | Service/autoconfig      | Step 4      |
| execution lifecycle      | REQ-007-REQ-013                    | concurrency/virtual time tests | Step 4                        | public Java API             | AutoConfiguration/hosts | Step 5      |
| Boot wiring              | REQ-003, REQ-011, REQ-012          | ContextRunner RED              | Steps 2-5                     | named conditional Beans     | consuming app           | Step 6      |
| BOM/docs/final contracts | REQ-001, REQ-002, REQ-013-REQ-015  | static/BOM consumer gates      | Steps 1-6                     | published component surface | phase-two Archetype     | Step 7      |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern             | Current repository evidence                                       | Effective Spec decision                                         | Planned implementation consequence                                   | Owning Steps/checks                        |
|---------------------|-------------------------------------------------------------------|-----------------------------------------------------------------|----------------------------------------------------------------------|--------------------------------------------|
| Architecture        | components architecture + rule-engine flat packages               | component-library flat exception accepted                       | `api/config/workflow/runtime/execution/autoconfigure/exception` only | All; MC-ARCH-001                           |
| Validation          | common-core `ValidationUtils`                                     | boundary records + create/delete groups + graph validator       | no manual Bean Validator clone                                       | Steps 2, 5; MC-VALID-001                   |
| Model               | repository Java 21 and immutable records                          | DTO/Command/BO use record unless lifecycle identity needs class | explicit defaults/null semantics                                     | Steps 2-5; MC-MODEL-001                    |
| Conversion          | `SpringAI` is official adapter                                    | no MapStruct for direct wrapper construction                    | adapter factory owns version seam                                    | Step 3; MC-CONVERT-001                     |
| Beans/logging       | named Beans, constructor injection, `@Slf4j`                      | stable names, final fields, Qualifier copied by lombok.config   | no field injection/manual logger                                     | Steps 3-6; MC-BEAN-001, MC-LOG-001         |
| Utilities/JSON/time | JDK collections/concurrency, no public HTTP JSON                  | JDK/ADK/RxJava only, `Instant`/`Duration`                       | no Fastjson/date formatter/custom util                               | All; MC-UTIL-001, MC-JSON-001, MC-TIME-001 |
| Config              | AutoConfiguration.imports and ApplicationContextRunner            | prefix strict Binder, default disabled                          | explicit bean `agentFlowProperties`, unknown key fails               | Steps 2, 6; MC-CONFIG-001                  |
| Pattern             | workflow type is known variation; execution is lifecycle pipeline | Strategy + Factory; direct guard/state machine                  | no abstract DDD services or handler chain                            | Steps 3-5; MC-PATTERN-001                  |

#### Capability reuse ledger

| Need               | Candidates inspected                                | Exact evidence                                                       | Fit/gap                                       | Decision                         | Added dependency/custom code | Owning Step/check        |
|--------------------|-----------------------------------------------------|----------------------------------------------------------------------|-----------------------------------------------|----------------------------------|------------------------------|--------------------------|
| validation         | common-core `ValidationUtils`                       | constructor accepts Jakarta `Validator`; ordered violations          | field groups covered; graph still custom      | reuse + graph validator          | no new validation lib        | Step 2; MC-REUSE-001     |
| Spring AI bridge   | `google-adk-spring-ai` 0.7.0 sources                | `SpringAI(ChatModel,String)`                                         | exact model seam; transitive dev must exclude | reuse official adapter           | managed ADK adapter          | Steps 1, 3; MC-DEP-001   |
| workflow agents    | ADK `SequentialAgent`, `ParallelAgent`, `LoopAgent` | all expose builders in 0.7.0                                         | exact three variants                          | Strategy factory                 | google-adk                   | Step 3; MC-PATTERN-001   |
| runner/session     | `InMemoryRunner`, `BaseSessionService`              | create/get/delete and `runAsync`; `Runner#close` returns Completable | exact V1 memory boundary                      | reuse                            | none                         | Steps 4, 5; MC-REUSE-001 |
| reactive lifecycle | RxJava3 `Flowable`/`Completable` in ADK             | native cancellation/backpressure hooks                               | needs guard/timeout composition               | reuse operators                  | transitive RxJava            | Step 5; MC-REUSE-001     |
| autoconfiguration  | rule-engine Starter                                 | AutoConfiguration + imports + ContextRunner                          | fits flat Starter                             | mirror style with explicit names | Spring Boot existing         | Step 6; MC-BEAN-001      |
| dependency export  | Components BOM                                      | only Egon artifacts managed                                          | fits one public artifact                      | add one dependency               | none                         | Step 7; MC-DEP-001       |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source                  | Repository evidence                                         | Exact files and order                                   | Pseudocode obligations                                    | Validation gate       | Steps         | Status/blocker |
|--------------|------------------------------|-------------------------------------------------------------|---------------------------------------------------------|-----------------------------------------------------------|-----------------------|---------------|----------------|
| Rule 1       | Spec §6.2                    | target names have Command/Result/DTO/BO/Enum suffixes       | config/API models before services                       | semantic records; no Data/Info/Param/Bean carriers        | static name scan      | Steps 2, 4, 5 | PASS           |
| Rule 2       | Spec §6.2/§10                | `ValidationUtils` and Jakarta groups exist                  | DTO constraints -> group -> service boundary validation | create/delete null rules and trim once                    | binding/service tests | Steps 2, 5    | PASS           |
| Rule 3       | Spec §6.2/§10                | no field-copy handoff except official adapter               | records -> direct constructors; SpringAI wrapper        | no manual cross-layer mapper or fake BaseConverter        | source scan/tests     | Steps 3-5     | PASS           |
| Rule 4       | Spec §6.2/§8.3               | adjacent Starter uses named Beans and constructor injection | lombok.config -> concrete classes -> auto config        | Slf4j/final fields/RequiredArgsConstructor/Qualifier      | context + source scan | Steps 1, 3-6  | PASS           |
| Rule 5       | Spec §6.2                    | JDK, ValidationUtils, RxJava, ADK cover needs               | POM gate before Java                                    | no extra helper/framework dependency                      | dependency tree       | Steps 1-7     | PASS           |
| Rule 6       | Spec §6.2/§9                 | no external JSON API                                        | internal records and raw ADK Event only                 | no Fastjson/Jackson annotations/custom serializer         | contract scan         | Steps 2, 5, 7 | PASS           |
| Rule 7       | Spec §6.2/§8.3               | Boot Binder and AutoConfiguration.imports convention        | properties -> strict Binder -> context tests            | one prefix, default disabled, no profiles                 | context/static tests  | Steps 2, 6    | PASS           |
| Rule 9       | Spec §6.2/§13                | workflow type is actual construction variation              | Strategy interface -> 3 implementations -> factory      | no generic handler framework                              | strategy tests        | Step 3        | PASS           |
| Rule 10      | Spec §6.2/§10                | Java 21 `Instant`, `Clock`, `Duration`                      | model -> service -> tests                               | no Date/Calendar/SimpleDateFormat                         | forbidden import scan | Steps 2, 5, 7 | PASS           |
| Rule 11      | Spec §6.1 approved exception | component architecture permits flat light Starter           | exact single module tree                                | no domain/application/infrastructure/adapter/biz packages | component contract    | Every Step    | PASS           |

## 5. Change File Tree

```text
egon-cola-components/
├── pom.xml                                              MODIFY
├── egon-cola-components-architecture.md                 MODIFY
├── egon-cola-components-bom/
│   ├── pom.xml                                          MODIFY
│   ├── README.md                                        MODIFY
│   └── README.zh-CN.md                                  MODIFY
└── egon-cola-component-agent-flow-starter/              CREATE
    ├── pom.xml, lombok.config, README.md, README.zh-CN.md
    ├── src/main/java/top/egon/cola/component/agentflow/
    │   ├── api/                                         5 Java files
    │   ├── autoconfigure/                               2 Java files
    │   ├── config/                                      6 Java files
    │   ├── exception/                                   7 Java files
    │   ├── execution/                                   2 Java files
    │   ├── runtime/                                     6 Java files
    │   └── workflow/                                    5 Java files
    ├── src/main/resources/META-INF/spring/AutoConfiguration.imports
    └── src/test/java/top/egon/cola/component/agentflow/  10 Java test files
```

## 6. Prerequisites, Constraints, and Plan Clarifications

- `PRE-001`: Step 1 必须从 Maven Central/local repository 成功解析 Spring AI 1.1.8、Google ADK 0.7.0；解析失败不授权换版本。
- `PRE-002`: 执行者必须先记录 `git status --short`，只提交当前 Step 路径，保留所有既有 dirty/untracked 文件。
- `PRE-003`: 不启动 Spring Boot 应用、浏览器、Docker、数据库、MCP 或真实模型；ApplicationContextRunner 不视为服务启动。
- `PLAN-CLAR-001`: `google-adk-spring-ai:0.7.0` 自身声明 `spring-ai-model:1.1.0` 和 compile `google-adk-dev`；本 Plan 使用
  Spring AI BOM 1.1.8 覆盖前者，并在 Starter dependency 上排除后者，若 binary linkage 测试失败则返回 Spec 的版本决策。
- `PLAN-CLAR-002`: `AgentFlowRuntimeBO` 保留一个 package-private 可注入 Runner/close seam 供离线生命周期测试使用，不引入公开
  SPI，也不改变 Spec 的 public API。
- `PLAN-CLAR-003`: 组件日志约束适用于本组件源码；第三方 adapter 的内部日志级别由宿主 logging config
  控制，组件文档必须提醒生产环境不可开启第三方 prompt/response debug logging。
- `PLAN-CLAR-004`: Phase 2 的开始条件是本 Plan 七个 Step 完成、focused verify 和 dependency tree Gate 通过，并有独立提交；该条件不是在本
  Plan 内创建 Archetype。

## 7. Ordered File-by-file Implementation Steps

每个 Step 严格按文件顺序执行：先满足 compile prerequisite，再建立 RED，随后实现 GREEN；验证完成后仅提交声明路径。

### Step 1 — 建立扁平 Starter 与依赖兼容 Gate

- Requirements: REQ-001, REQ-002, REQ-015
- Dependencies: None
- Baseline state: Components Reactor 未登记 Agent Flow 模块，也未管理 Spring AI/ADK；本地已确认 0.7.0 artifacts
  可解析，但仓库没有使用合同。
- Observable outcome: 新 module 可被 Maven 定位，dependency tree 固定 Spring AI 1.1.8/ADK 0.7.0，排除 `google-adk-dev`
  、Web、数据库和 provider starter。
- End state: 只有 module skeleton、依赖合同测试和 parent 管理生效；无业务 Bean/Runner/配置行为。
- Test-first gate: Required — module POM 先满足测试编译前置，随后 `AgentFlowComponentContractTest` 对 parent module
  缺失、版本/排除项和禁用依赖形成 RED，最后修改 parent 到 GREEN。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-UTIL-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 4, Rule 5, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-agent-flow-starter/{pom.xml,lombok.config}`

- Purpose: 创建可独立解析的轻量 Starter shell，并声明精确 dependency/exclusion 与 Lombok constructor annotation 复制规则。
- Symbols: Maven artifact `egon-cola-component-agent-flow-starter`; dependencies common-core、Boot
  starter/autoconfigure/validation、spring-ai-model、google-adk、google-adk-spring-ai、configuration-processor/test；
  `lombok.copyableAnnotations`。
- Repository evidence: rule-engine Starter 直接继承 components parent并内置测试；source-web 的 `lombok.config` 已复制
  `@Qualifier`/`@Value`。
- Dependencies and consumers: 继承 `egon-cola-components-parent:5.3.3`；后续所有 Agent Flow sources/tests编译；Phase 2
  仅消费最终 artifact。
- Why now: 没有 module POM 时测试源码无法建立 RED，先创建最小编译壳不产生运行行为。
- Contract/signature changes: 新公开 Maven GAV；`google-adk-spring-ai` 显式 exclusion `google-adk-dev`；不加入
  provider/Web/MCP starter/DB。
- Input/output and state mapping: parent managed versions映射到 module effective POM；annotation processor只生成
  metadata/constructors，不产生外部状态。
- Error and edge behavior: unresolved artifact、version drift、exclusion失效均由 Maven 非零；不临时添加 repository 或替代版本。
- Standards impact: MC-ARCH-001、MC-DEP-001、MC-BEAN-001、MC-SCOPE-001；单模块功能包边界与 constructor injection 基线。
- Literal rule enforcement: Rule 4 复制 Qualifier 到构造参数；Rule 5 只增加 Spec 批准依赖；Rule 11 不创建 DDD/COLA 业务层。
- Implementation pseudocode:

```xml
inherit top.egon:egon-cola-components-parent:5.3.3 using ../pom.xml
declare common-core, Boot core/autoconfigure/validation, spring-ai-model, google-adk and google-adk-spring-ai without local versions
exclude com.google.adk:google-adk-dev from google-adk-spring-ai and add only configuration processor plus starter-test in non-runtime scopes
configure lombok.config to stop bubbling and copy Qualifier/Value to generated constructor parameters
```

- Verification contribution: 使 dependency tree 与静态 contract test可执行，并固定新增依赖面的最小集合。
- After this file: 独立 `-f` validate 可运行，但父 Reactor/DM 和 contract 断言尚未满足。

#### File 2 —
`CREATE egon-cola-components/egon-cola-component-agent-flow-starter/src/test/java/top/egon/cola/component/agentflow/contract/AgentFlowComponentContractTest.java`

- Purpose: 把扁平目录、禁用依赖/配置/日志/命名规则与 Components/BOM 接线变成离线静态合同。
- Symbols: tests `uses_flat_functional_packages`、`uses_exact_dependency_boundary`、
  `does_not_expose_provider_or_secret_properties`、`uses_semantic_java_names_and_safe_logging`、`is_registered_by_parent`。
- Repository evidence: 当前 Web verifier和组件测试均通过 `Path`/source scan验证架构约束；Spec TEST-023/024要求同类 gate。
- Dependencies and consumers: 读取 module/POM/source/resources 与 components parent；后续 Step 每次 focused
  test复用，BOM/docs合同在 Step 7追加。
- Why now: 在生产实现前先使越界包、dependency、secret/logging 和发布接线可观察失败。
- Contract/signature changes: 测试 CLI 无外部输入；失败信息必须打印具体 path/token/coordinate；不写扫描目标。
- Input/output and state mapping: tracked file tree/module/parent POM text映射为通过或精确 assertion；忽略 `target`、
  `.git` 和 Plan 文档。
- Error and edge behavior: 缺目录、无法读文件、禁止 token、manual logger、field injection、Date/Fastjson或 parent未登记均
  fail closed。
- Standards impact: MC-ARCH-001、MC-NAME-001、MC-LOG-001、MC-UTIL-001、MC-JSON-001、MC-TIME-001、MC-SCOPE-001、MC-TEST-001。
- Literal rule enforcement: Rule 1 检查语义后缀；Rule 4 检查 Slf4j/constructor injection；Rule 5/6/10 检查工具、JSON、时间禁用项；Rule
  11 检查 flat package allowlist。
- Implementation pseudocode:

```java
walk module source and resources while excluding target; normalize repository-relative paths
assert package roots are api/autoconfigure/config/execution/runtime/workflow/exception and reject domain/application/infrastructure/adapter/biz
parse parent/module/BOM text to assert exact versions, exclusion and only one exported Egon artifact; reject provider, web, persistence and secret coordinates
scan Java/config for forbidden names, Date APIs, Fastjson, manual LoggerFactory, field injection, prompt/user/session logging and api-key/base-url properties
```

- Verification contribution: 提供 TEST-023/024 的 RED/GREEN 基础；Step 7在同一测试类新增 BOM/docs最终接线断言。
- After this file: 测试因 parent 未登记/管理版本而按预期 RED。

#### File 3 — `MODIFY egon-cola-components/pom.xml`

- Purpose: 登记新 module，并以 Components parent 单一管理 Spring AI/ADK 版本。
- Symbols: modules、`spring-ai.version=1.1.8`、`google-adk.version=0.7.0`、dependencyManagement 的 `spring-ai-bom`/
  `google-adk`/`google-adk-spring-ai`。
- Repository evidence: 当前 parent集中 module、Java 21与第三方版本；root已经统一 Boot 3.5.16。
- Dependencies and consumers: 新 module及未来 Components consumers继承；BOM 只在 Step 7 导出 Egon artifact。
- Why now: module/POM contract已形成 RED，集中版本 owner 后后续 Java 才有稳定编译基线。
- Contract/signature changes: Components Reactor 新增一个 child；新增两项版本属性与一个 BOM import、两项 ADK managed
  dependencies。
- Input/output and state mapping: parent version properties流入 child effective POM；不改变其他组件的显式依赖集合。
- Error and edge behavior: 若 Spring AI 1.1.8 与 ADK 0.7.0 出现 linkage/compile 冲突，Step 1 停止并返回 Spec，不加入
  shading或兼容 patch。
- Standards impact: MC-REUSE-001、MC-DEP-001、MC-SCOPE-001、MC-TEST-001；依赖集中管理且不污染 BOM public coordinate。
- Literal rule enforcement: Rule 5 只用批准依赖/BOM；Rule 11 只登记一个 flat Starter，不增加架构层。
- Implementation pseudocode:

```xml
append egon-cola-component-agent-flow-starter once in components modules after comparable starter modules
define spring-ai.version 1.1.8 and google-adk.version 0.7.0 in the parent properties ledger
import org.springframework.ai:spring-ai-bom at spring-ai.version and manage com.google.adk google-adk plus google-adk-spring-ai at google-adk.version
retain every existing property, dependency and plugin order; do not export third-party coordinates from the Egon Components BOM
```

- Verification contribution: 使 module validate、effective POM和 dependency tree进入预期版本线；BOM assertion保留到 Step 7
  RED。
- After this file: Starter 编译壳可解析，依赖边界已稳定，业务能力仍不存在。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -f egon-cola-components/egon-cola-component-agent-flow-starter/pom.xml test -Dtest=AgentFlowComponentContractTest && ./mvnw -B -ntp -f egon-cola-components/egon-cola-component-agent-flow-starter/pom.xml dependency:tree -Dscope=runtime -Dincludes=org.springframework.ai:*,com.google.adk:*,org.springframework.boot:*,org.springframework:*`
- Expected result: module/test可编译且本 Step全部 contract通过；dependency tree显示 Spring AI 1.1.8、ADK 0.7.0且无
  `google-adk-dev`/Web/DB/provider starter；BOM export尚未进入测试范围。
- Failure returns to: File 1 若 module依赖/exclusion错误；File 2 若 contract范围误报；File 3 若 managed
  version或module接线错误；binary linkage冲突返回 primary Spec §5版本决策。
- Completion criteria: REQ-001/002/015 的构建和依赖前置闭环，后续 Java 文件可在固定 classpath 上开发。
- Rollback: 仅回退新 module skeleton/test与 components parent精确条目；不触碰其他组件或文档。
- Commit paths: `egon-cola-components/egon-cola-component-agent-flow-starter/{pom.xml,lombok.config}`;
  `egon-cola-components/egon-cola-component-agent-flow-starter/src/test/java/top/egon/cola/component/agentflow/contract/AgentFlowComponentContractTest.java`;
  `egon-cola-components/pom.xml`
- Commit: `build(agent-flow): add compatible flat starter module`

### Step 2 — 实现严格配置绑定与单根图校验

- Requirements: REQ-004, REQ-005, REQ-011, REQ-015
- Dependencies: Step 1
- Baseline state: module可编译但没有属性 record、Validation group、约束或 graph validator。
- Observable outcome: 配置 defaults、字段校验和 DFS 图规则均确定；所有非法图抛安全的 `AgentFlowConfigurationException`。
- End state: 得到不可变且完整验证的配置模型；尚不创建 ADK Agent/Runner或 Spring Beans。
- Test-first gate: Required — 先创建 Binding/Validator tests 并观察缺类型或未拒绝非法图的 RED，再按
  record、exception、validator 顺序完成 GREEN。
- Manual Checks: MC-VALID-001, MC-NAME-001, MC-MODEL-001, MC-UTIL-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 5, Rule 7, Rule 10, Rule 11
- Ordered files:

#### File 1 —
`CREATE egon-cola-components/egon-cola-component-agent-flow-starter/src/test/java/top/egon/cola/component/agentflow/{autoconfigure/AgentFlowPropertiesBindingTest.java,config/AgentFlowConfigValidatorTest.java}`

- Purpose: 先固定 constructor binding/default/range/unknown-key与完整图合法性合同。
- Symbols: parameterized tests for
  single/sequential/parallel/loop、duplicate/missing/cycle/multi-parent/unreachable/root/outputKey/loop/max counts。
- Repository evidence: Spec TEST-002至005提供完整场景；相邻 Starter 使用 JUnit 5和 Boot Binder/ApplicationContextRunner。
- Dependencies and consumers: 测试将使用 Boot `Binder`、Jakarta Validator、`ValidationUtils`和纯 records；Step 6复用 strict
  unbound handler验证 context。
- Why now: 图语义是 factory前置，不可通过 builder异常替代业务可读校验。
- Contract/signature changes: 无生产合同；fixture使用不含 secret/prompt全文的合成值，异常断言只匹配安全路径与原因。
- Input/output and state mapping: property map -> `AgentFlowProperties` -> `AgentFlowConfigValidator#validate`
  ；测试不写环境或网络。
- Error and edge behavior: null/blank、越界集合、非法ADK name、重复 outputKey、缺引用、环、多个父、不可达、root被引用和
  Loop规则全部独立断言。
- Standards impact: MC-VALID-001、MC-MODEL-001、MC-CONFIG-001、MC-TEST-001；建立字段与图两级测试证据。
- Literal rule enforcement: Rule 1 测试语义命名；Rule 2 验证 group/normalization；Rule 7验证一套 prefix/default；Rule 10只用
  Duration；Rule 11不引入层包。
- Implementation pseudocode:

```java
bind a valid property map and assert disabled default, Duration defaults, limits and immutable ordered flow list
parameterize valid leaf and three workflow roots, then assert validator returns normally and preserves declared child order
mutate one invariant per fixture for duplicates, missing references, cycle, in-degree greater than one, unreachable nodes, root misuse and invalid loop count
assert every invalid fixture throws AgentFlowConfigurationException containing flow/node/property identifiers but never instruction, user data or model content
```

- Verification contribution: 覆盖 TEST-002至005 的 RED/GREEN证据。
- After this file: 测试因生产 records/validator缺失而 RED。

#### File 2 —
`CREATE egon-cola-components/egon-cola-component-agent-flow-starter/src/main/java/top/egon/cola/component/agentflow/{config/AgentFlowConfigDTO.java,config/AgentConfigDTO.java,config/AgentWorkflowConfigDTO.java,config/AgentWorkflowTypeEnum.java,config/AgentFlowSessionValidationGroup.java,exception/AgentFlowException.java,exception/AgentFlowConfigurationException.java}`

- Purpose: 定义不可变配置、枚举、创建/删除 validation marker基础与配置异常根。
- Symbols: five records/enums/groups plus `AgentFlowException`、`AgentFlowConfigurationException`；Jakarta constraints和
  compact constructors。
- Repository evidence: Spec §10给出字段/null/default规则；仓库 Java 21与 Rule 1/2要求 semantic suffix和 groups。
- Dependencies and consumers: Properties/validator/factories/API commands使用；不依赖 ADK、Spring context或供应商类型。
- Why now: tests先定义行为，先建立数据合同再写 graph算法。
- Contract/signature changes: 新内部/public config types；所有 List在构造时 `List.copyOf`，blank normalization只执行一次。
- Input/output and state mapping: YAML-shaped values映射为 records；absent optional字段在明确 owner处应用默认，显式非法
  null由 validation拒绝。
- Error and edge behavior: constructor不吞异常、不自动删除非法 child；异常只保存 flow/node/property标识与 cause type。
- Standards impact: MC-NAME-001、MC-VALID-001、MC-MODEL-001、MC-TIME-001；records完整构造且使用 Duration。
- Literal rule enforcement: Rule 1 使用 DTO/Enum后缀；Rule 2 Jakarta约束/groups；Rule 3无跨层 field copy；Rule 10无 legacy
  time；Rule 11仍在 config/exception功能包。
- Implementation pseudocode:

```java
define immutable records for flow, leaf agent and workflow with explicit constructor parameter order matching the Spec field tables
copy every collection defensively, normalize identifier-like strings once, preserve instruction text without logging and reject explicit illegal nulls via constraints
declare workflow enum values SEQUENTIAL, PARALLEL and LOOP and a session validation group namespace for later create/delete differences
make AgentFlowConfigurationException extend AgentFlowException and carry only safe configuration coordinates plus the original cause
```

- Verification contribution: 使 binding tests编译并验证模型构造/字段约束。
- After this file: records与异常可用；graph invalid cases仍 RED。

#### File 3 —
`CREATE egon-cola-components/egon-cola-component-agent-flow-starter/src/main/java/top/egon/cola/component/agentflow/{autoconfigure/AgentFlowProperties.java,config/AgentFlowConfigValidator.java}`

- Purpose: 聚合 prefix配置和执行字段/图双层 fail-closed校验。
- Symbols: `AgentFlowProperties`; `AgentFlowConfigValidator#validate`; DFS
  color、in-degree、reachability、count/loop/outputKey checks。
- Repository evidence: `ValidationUtils`提供 group-aware validation；Spec要求 O(V+E) 单根树并拒绝半静默节点。
- Dependencies and consumers: Validator依赖具名 `ValidationUtils`（Step 6注入）；Factory/Registry只接受验证后的 Properties。
- Why now: ADK builder前必须获得 deterministic configuration error，而不是运行期异常。
- Contract/signature changes: Properties prefix固定 `egon.cola.component.agent-flow`，默认 disabled、PT2M执行时限及
  Spec容量上限。
- Input/output and state mapping: records -> name maps/in-degree/colors/reachable set -> validated original immutable
  graph；不改写用户顺序。
- Error and edge behavior: 聚合时按稳定 flow/node/field顺序选择首个 violation；不得在异常/日志中附 instruction/model
  content。
- Standards impact: MC-VALID-001、MC-BEAN-001、MC-UTIL-001、MC-CONFIG-001；复用 ValidationUtils与 JDK Map/Set。
- Literal rule enforcement: Rule 2 先 ValidationUtils再 graph；Rule 5不自建集合util；Rule 7集中 defaults/limits；Rule 11仅
  config/autoconfigure功能包。
- Implementation pseudocode:

```java
validate AgentFlowProperties and every nested record with ValidationUtils, reporting the first stable safe property path
for each flow build a unified name table for leaves and workflows; reject duplicates, missing root, missing children, duplicate children and type-specific loop violations
compute in-degree for every non-root node, require exactly one parent, then run three-color DFS from root to reject cycles and collect reachability in O(V+E)
require every declared node reachable, every outputKey unique where configured, and all collection/count/duration limits within the approved bounds; return no transformed graph
```

- Verification contribution: 使 TEST-002至005全部 GREEN并为 factory提供 fail-closed前置。
- After this file: 合法配置可稳定通过，非法图不会进入 ADK 构建。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -f egon-cola-components/egon-cola-component-agent-flow-starter/pom.xml test -Dtest=AgentFlowPropertiesBindingTest,AgentFlowConfigValidatorTest`
- Expected result: TEST-002至005全部通过；无 Spring context、模型、网络或 Runner创建；异常不含 instruction内容。
- Failure returns to: File 1若场景与 Spec不一致；File 2若 record/null/group合同错误；File 3若 DFS/validation顺序错误。
- Completion criteria: REQ-004/005/011 的配置与图部分有确定性测试，REQ-015保持离线。
- Rollback: 仅删除本 Step 两组 tests与 config/exception基础文件，不影响 module dependency commit。
- Commit paths:
  `egon-cola-components/egon-cola-component-agent-flow-starter/src/test/java/top/egon/cola/component/agentflow/{autoconfigure/AgentFlowPropertiesBindingTest.java,config/AgentFlowConfigValidatorTest.java}`;
  `egon-cola-components/egon-cola-component-agent-flow-starter/src/main/java/top/egon/cola/component/agentflow/{config/AgentFlowConfigDTO.java,config/AgentConfigDTO.java,config/AgentWorkflowConfigDTO.java,config/AgentWorkflowTypeEnum.java,config/AgentFlowSessionValidationGroup.java,exception/AgentFlowException.java,exception/AgentFlowConfigurationException.java}`;
  `egon-cola-components/egon-cola-component-agent-flow-starter/src/main/java/top/egon/cola/component/agentflow/{autoconfigure/AgentFlowProperties.java,config/AgentFlowConfigValidator.java}`
- Commit: `feat(agent-flow): validate immutable flow configuration`

### Step 3 — 编译叶子 Agent 与三类 Workflow

- Requirements: REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-015
- Dependencies: Step 2
- Baseline state: 配置可验证，但没有 ChatModel解析、ADK adapter、Strategy或 Flow factory。
- Observable outcome: 一个合法 Flow能从具名 ChatModel构建真实 leaf/Sequential/Parallel/Loop根并创建 `InMemoryRunner`。
- End state: 单 Flow factory可完整构建或安全失败；Registry尚未聚合/发布多个 Flow。
- Test-first gate: Required — Strategy/FlowFactory tests先对缺类形成 RED，随后依次创建 Strategy、model adapter和 factory到
  GREEN。
- Manual Checks: MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-BEAN-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 3, Rule 4, Rule 5, Rule 9, Rule 11
- Ordered files:

#### File 1 —
`CREATE egon-cola-components/egon-cola-component-agent-flow-starter/src/test/java/top/egon/cola/component/agentflow/{workflow/AgentWorkflowStrategyFactoryTest.java,runtime/AgentFlowFactoryTest.java}`

- Purpose: 固定 strategy选择、子节点顺序、真实 ADK类型、ChatModel Bean解析和 builder失败清理合同。
- Symbols: tests for enum-to-strategy uniqueness、four root shapes、outputKey、maxIterations、missing/wrong model、builder
  error。
- Repository evidence: ADK 0.7 sources确认 builders、`SpringAI(ChatModel,String)`和 `InMemoryRunner(BaseAgent,String)`
  ；Spec TEST-006/007。
- Dependencies and consumers: 使用 fake `ChatModel`/deterministic agents；不调用真实 provider；后续 RegistryFactory test复用
  fixture。
- Why now: Strategy和 factory是复杂变化点，先用真实 ADK types约束签名可防止写出参考工程专用抽象。
- Contract/signature changes: 无生产签名；测试只观察 types/names/order/model resolution/cleanup，不断言第三方内部实现细节。
- Input/output and state mapping: validated config + named ChatModel map -> BaseAgent tree/Runner；失败 -> typed
  configuration exception + close attempt。
- Error and edge behavior: duplicate strategy、unknown type、missing/wrong Bean、ADK builder异常均确定失败；消息无 prompt。
- Standards impact: MC-REUSE-001、MC-DEP-001、MC-BEAN-001、MC-PATTERN-001、MC-TEST-001。
- Literal rule enforcement: Rule 3 验证直接 adapter而非字段拷贝；Rule 4 使用构造注入 seam；Rule 9 只测三种实际
  Strategy；Rule 11 不创建 DDD node hierarchy。
- Implementation pseudocode:

```java
construct one strategy per SEQUENTIAL, PARALLEL and LOOP type, then assert factory rejects duplicates and returns the exact strategy for each enum
build deterministic leaf fixtures from a fake ChatModel and assert four configured roots are LlmAgent, SequentialAgent, ParallelAgent and LoopAgent with declared child order
assert outputKey/modelName/maxIterations flow to the corresponding ADK builders and no provider call occurs during construction
inject missing/wrong ChatModel and failing builder seams; assert safe AgentFlowConfigurationException and close is attempted for any partially created runner
```

- Verification contribution: TEST-006/007 RED/GREEN；兼容性问题最早在本 Step暴露。
- After this file: tests RED，因为 Strategy/model adapter/factory不存在。

#### File 2 —
`CREATE egon-cola-components/egon-cola-component-agent-flow-starter/src/main/java/top/egon/cola/component/agentflow/workflow/{AgentWorkflowBuilderStrategy.java,AgentWorkflowStrategyFactory.java,SequentialAgentWorkflowBuilderStrategy.java,ParallelAgentWorkflowBuilderStrategy.java,LoopAgentWorkflowBuilderStrategy.java}`

- Purpose: 用最小 Strategy模式隔离三种 ADK workflow builder差异，保留统一构建入口。
- Symbols: `supports(AgentWorkflowTypeEnum)`、`build(config, orderedChildren)`、factory `getStrategy(type)`；三个具名
  implementations。
- Repository evidence: Spec §13明确选择 Strategy + Factory；ADK三类 builder分别接收 subAgents，Loop额外接收 maxIterations。
- Dependencies and consumers: 仅依赖 config records与 ADK BaseAgent；`AgentFlowFactory`消费。
- Why now: Tests已定义真实变化点，直接三分支会让 future workflow增长污染图遍历。
- Contract/signature changes: 新模块内 strategy contract；不作为组件 public service暴露。
- Input/output and state mapping: workflow config + immutable ordered children -> one composite BaseAgent；不排序、不丢
  child。
- Error and edge behavior: unsupported/duplicate type fail closed；空 children/type-specific invalid值仍拒绝，即使
  validator被绕过。
- Standards impact: MC-NAME-001、MC-BEAN-001、MC-PATTERN-001；具体策略有语义名、final依赖、必要日志才用 Slf4j。
- Literal rule enforcement: Rule 1保留 Strategy语义；Rule 4 constructor injection；Rule 9不增加抽象工厂/handler链；Rule
  11位于 flat workflow包。
- Implementation pseudocode:

```java
index the immutable ordered strategy list by its single supported AgentWorkflowTypeEnum and reject missing or duplicate registrations
Sequential strategy calls SequentialAgent.builder with name, description and ordered subAgents from configuration
Parallel strategy calls ParallelAgent.builder with the same ordered child list; Loop strategy additionally applies the validated maxIterations
wrap ADK IllegalArgumentException with safe workflow name/type context while preserving cause and never including instructions
```

- Verification contribution: Strategy factory/type/order测试变 GREEN；FlowFactory仍 RED。
- After this file: 三类 composite可独立创建，尚未递归解析配置树。

#### File 3 —
`CREATE egon-cola-components/egon-cola-component-agent-flow-starter/src/main/java/top/egon/cola/component/agentflow/runtime/{SpringAiModelAdapterFactory.java,AgentFlowFactory.java}`

- Purpose: 解析具名 ChatModel，构建所有 leaf后自底向上构建 composite root和 Runner。
- Symbols: `SpringAiModelAdapterFactory#create(beanName,modelName)`；`AgentFlowFactory#create(AgentFlowConfigDTO)`
  ；private memoized `buildNode`。
- Repository evidence: official adapter拥有所需constructor；Runner支持显式 appName；Spec Bean表规定两个 factory边界。
- Dependencies and consumers: 依赖 validated config、named ChatModel snapshot、StrategyFactory、ADK；Step 4
  RegistryFactory消费。
- Why now: Strategy已稳定，可在一个 factory中实现递归编译且不泄露 BeanFactory到业务类。
- Contract/signature changes: 新内部 factory；输出包含 root/Runner所需构造结果，最终 RuntimeBO在 Step 4承接。
- Input/output and state mapping: chat-model-bean-name -> ChatModel -> SpringAI；leaf config -> LlmAgent；workflow DFS ->
  composite；flowId -> Runner appName。
- Error and edge behavior: missing/wrong model、unexpected node、builder异常转换为 configuration exception；若
  Runner已创建后失败，调用 `close().blockingAwait`并保留首因。
- Standards impact: MC-REUSE-001、MC-CONVERT-001、MC-BEAN-001、MC-UTIL-001、MC-PATTERN-001。
- Literal rule enforcement: Rule 3只用 official adapter/direct builder；Rule 4 final fields +
  RequiredArgsConstructor/Qualifier；Rule 5复用 ADK；Rule 9委托 Strategy；Rule 11位于 runtime包。
- Implementation pseudocode:

```java
lookup chatModelBeanName in the immutable bean-name map, require ChatModel type and wrap it with new SpringAI(chatModel, configuredModelName)
create every LlmAgent once with safe name, description, instruction, outputKey and the shared adapted model; memoize by configured node name
recursively resolve each workflow child in declared order and delegate construction to AgentWorkflowStrategyFactory; require the validated root resolves exactly once
construct InMemoryRunner(rootAgent, flowId), return the complete compilation result, and close any allocated runner on later failure before rethrowing a typed safe exception
```

- Verification contribution: TEST-006/007全部 GREEN并提供后续 atomic Registry的单 Flow primitive。
- After this file: 一个已验证 Flow可构建真实 ADK runtime；尚无跨 Flow原子发布。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -f egon-cola-components/egon-cola-component-agent-flow-starter/pom.xml test -Dtest=AgentWorkflowStrategyFactoryTest,AgentFlowFactoryTest`
- Expected result: Strategy选择和四种 root类型全部通过；fake ChatModel未发起网络；missing Bean/build failure产生安全异常并尝试清理。
- Failure returns to: File 1若断言依赖第三方非合同细节；File 2若 Strategy映射/顺序错误；File 3若
  adapter签名/linkage或清理错误；版本不兼容返回 Spec。
- Completion criteria: REQ-002至006的单 Flow编译部分和 REQ-015离线边界有可执行证据。
- Rollback: 回退本 Step tests/workflow/runtime两文件组，不回退已验证配置合同。
- Commit paths:
  `egon-cola-components/egon-cola-component-agent-flow-starter/src/test/java/top/egon/cola/component/agentflow/{workflow/AgentWorkflowStrategyFactoryTest.java,runtime/AgentFlowFactoryTest.java}`;
  `egon-cola-components/egon-cola-component-agent-flow-starter/src/main/java/top/egon/cola/component/agentflow/workflow/{AgentWorkflowBuilderStrategy.java,AgentWorkflowStrategyFactory.java,SequentialAgentWorkflowBuilderStrategy.java,ParallelAgentWorkflowBuilderStrategy.java,LoopAgentWorkflowBuilderStrategy.java}`;
  `egon-cola-components/egon-cola-component-agent-flow-starter/src/main/java/top/egon/cola/component/agentflow/runtime/{SpringAiModelAdapterFactory.java,AgentFlowFactory.java}`
- Commit: `feat(agent-flow): compile ADK workflow trees`

### Step 4 — 原子发布并管理不可变 Runtime Registry

- Requirements: REQ-006, REQ-007, REQ-012, REQ-013, REQ-015
- Dependencies: Step 3
- Baseline state: 单 Flow可构建，但多 Flow没有 all-or-nothing装配、稳定描述投影、查找或关闭状态。
- Observable outcome: 全部 Flow先在临时集合编译，成功后发布排序不可变 Registry；失败/关闭时每个已建 Runner均被尝试关闭。
- End state: Registry提供 descriptor/query/runtime lookup与 OPEN/CLOSING/CLOSED生命周期；Service尚未暴露会话 API。
- Test-first gate: Required — 先创建 RegistryFactory/Registry tests 对 partial failure、排序、closed拒绝和 delay-error
  close形成 RED，再实现 runtime types。
- Manual Checks: MC-REUSE-001, MC-NAME-001, MC-MODEL-001, MC-LOG-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 4, Rule 5, Rule 10, Rule 11
- Ordered files:

#### File 1 —
`CREATE egon-cola-components/egon-cola-component-agent-flow-starter/src/test/java/top/egon/cola/component/agentflow/runtime/{AgentFlowRegistryFactoryTest.java,DefaultAgentFlowRegistryTest.java}`

- Purpose: 先固定 all-or-nothing、多 Flow排序、runtime lookup、并发关闭和 delay-error清理语义。
- Symbols: tests `does_not_publish_partial_registry`、`closes_every_created_runner_on_failure`、
  `lists_sorted_immutable_descriptors`、`rejects_lookup_after_closing`、`closes_all_runners_even_when_one_fails`。
- Repository evidence: Spec TEST-006/020/021与 startup scenario；ADK Runner `close()`返回 RxJava `Completable`。
- Dependencies and consumers: 使用 Step 3 factory seam与 controlled Runner close completables；Step 5 Service test复用
  Registry fixture。
- Why now: 多 Flow发布和 close必须在 Service并发逻辑前独立证明，避免半成品 context。
- Contract/signature changes: 无外部合同；测试只使用 safe flow IDs和 synthetic failures。
- Input/output and state mapping: ordered configs -> temp runtime map -> immutable registry；failure/close -> attempt
  ledger + aggregate cause。
- Error and edge behavior: 第 N个 build失败关闭前 N-1个；一个 close失败不跳过其他；CLOSING/CLOSED拒绝 lookup并不泄露
  runtime。
- Standards impact: MC-MODEL-001、MC-LOG-001、MC-BEAN-001、MC-TEST-001；验证不可变模型和安全日志字段。
- Literal rule enforcement: Rule 1描述类型用 DTO/BO；Rule 4具体 factory/registry使用 constructor injection与 Slf4j；Rule
  10 descriptor使用 Instant；Rule 11位于 runtime测试包。
- Implementation pseudocode:

```java
script AgentFlowFactory to return two runtimes and fail on the third; assert no registry result escapes and both completed runners receive exactly one close attempt
construct unsorted runtime fixtures, list descriptors and assert flowId ordering, immutable result and no Runner/ChatModel/instruction exposure
race lookup and close using barriers; assert state changes OPEN to CLOSING to CLOSED and no new runtime acquisition after closing starts
make one close Completable fail and assert remaining runners still close, first cause is preserved and safe logs contain only flowId/stage/outcome/error type
```

- Verification contribution: TEST-006、TEST-020与 close基础场景的 RED/GREEN。
- After this file: tests RED，因为 RuntimeBO/Registry/Factory尚不存在。

#### File 2 —
`CREATE egon-cola-components/egon-cola-component-agent-flow-starter/src/main/java/top/egon/cola/component/agentflow/{api/AgentFlowDescriptorDTO.java,exception/AgentFlowNotFoundException.java,runtime/AgentFlowRuntimeBO.java,runtime/AgentFlowRegistry.java}`

- Purpose: 定义安全描述投影、内部 runtime holder与最小 Registry contract。
- Symbols: `AgentFlowDescriptorDTO` record；`AgentFlowRuntimeBO`；`AgentFlowRegistry#list/#require/#beginClosing/#close`
  ；not-found exception。
- Repository evidence: Spec §9 INTERNAL-001只允许 descriptor，不返回 Runner/prompt/session；§10定义 RuntimeBO lifecycle字段。
- Dependencies and consumers: RuntimeBO拥有 flowId/root metadata/InMemoryRunner；Default Registry和 Service消费；public
  DTO不持有第三方运行对象。
- Why now: Tests先固定不泄露合同，随后建立 Registry实现所需类型。
- Contract/signature changes: 新 public descriptor与 module-internal registry API；not-found异常作为稳定分类。
- Input/output and state mapping: RuntimeBO -> descriptor只映射
  flowId/rootAgentName/chatModelBeanName/modelName；Runner留在内部。
- Error and edge behavior: null runtime字段构造失败；unknown flow抛 only-flowId异常；CLOSING/CLOSED拒绝 require。
- Standards impact: MC-NAME-001、MC-MODEL-001、MC-CONVERT-001、MC-TIME-001；直接小映射无需 MapStruct。
- Literal rule enforcement: Rule 1 DTO/BO语义名；Rule 3 direct projection避免仪式 converter；Rule 10用 Instant/Clock；Rule
  11保留 api/runtime包。
- Implementation pseudocode:

```java
define AgentFlowRuntimeBO with final safe metadata, root BaseAgent, InMemoryRunner and creation Instant; validate every required member at construction
project runtime metadata into AgentFlowDescriptorDTO without exposing runner, model, instructions, prompts or session state
define registry operations for immutable sorted descriptors, guarded runtime lookup, beginClosing and close-all lifecycle
throw AgentFlowNotFoundException carrying only the requested flowId when no OPEN runtime exists
```

- Verification contribution: 使 descriptor/lookup tests编译并验证 Rule 1/3模型边界。
- After this file: Registry contract与 runtime holder可用，实现行为仍 RED。

#### File 3 —
`CREATE egon-cola-components/egon-cola-component-agent-flow-starter/src/main/java/top/egon/cola/component/agentflow/runtime/{AgentFlowRegistryFactory.java,DefaultAgentFlowRegistry.java}`

- Purpose: 实现全量临时编译、一次性 immutable publish和有界 delay-error close。
- Symbols: `AgentFlowRegistryFactory#create(AgentFlowProperties)`；`DefaultAgentFlowRegistry` state
  AtomicReference、sorted maps/lists、close aggregation。
- Repository evidence: Spec REQ-006/012要求一个 Flow失败阻断全部与关闭全部 Runner；ADK close是 Completable。
- Dependencies and consumers: Factory依赖 `AgentFlowFactory`；Registry被 Step 5 Service和 Step 6 AutoConfiguration消费。
- Why now: 单 Flow primitive和 Registry合同已存在，可形成完整 startup consistency boundary。
- Contract/signature changes: 新具名 Bean候选类型，但 Spring注册留 Step 6；关闭可幂等调用。
- Input/output and state mapping: properties.flows declaration -> LinkedHashMap temp -> sorted immutable
  map/list；close -> state/attempt/result。
- Error and edge behavior: duplicate flow虽应被 validator拦截仍 defensive fail；任何 build/close异常安全聚合；close
  timeout由 Service/Properties控制，不吞首因。
- Standards impact: MC-REUSE-001、MC-LOG-001、MC-BEAN-001、MC-UTIL-001、MC-PATTERN-001。
- Literal rule enforcement: Rule 4 使用 Slf4j/final constructor collaborators；Rule 5 使用 JDK immutable
  collections/RxJava Completable；Rule 9 不新增 pattern；Rule 11 位于 runtime包。
- Implementation pseudocode:

```java
validate all properties once, compile each flow into a temporary insertion-ordered map and never expose that map during construction
on compile failure iterate already-created runtimes in reverse creation order, invoke runner.close with delay-error semantics, then rethrow the original typed configuration error
after every flow succeeds create sorted immutable runtime and descriptor snapshots and initialize registry state to OPEN in one constructor publication
on close atomically move OPEN to CLOSING, invoke close for every runner even after failures, finalize CLOSED, log only safe allowlist fields and make repeated close idempotent
```

- Verification contribution: RegistryFactory/Registry tests全部 GREEN；为 Service lifecycle提供原子 runtime来源。
- After this file: 多 Flow启动与关闭原子边界完成，尚无会话/执行 public service。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -f egon-cola-components/egon-cola-component-agent-flow-starter/pom.xml test -Dtest=AgentFlowRegistryFactoryTest,DefaultAgentFlowRegistryTest`
- Expected result: partial build不发布、已建 Runner全关闭；descriptor排序不可变；close失败不短路且 state最终 CLOSED。
- Failure returns to: File 1若测试依赖不稳定第三方细节；File 2若 DTO/BO泄漏；File 3若 publication/close竞态或错误聚合失败。
- Completion criteria: REQ-006/012的 Registry部分、REQ-007查找和 REQ-013安全投影有离线证据。
- Rollback: 删除本 Step tests、descriptor/registry/runtime files；保留单 Flow factory提交。
- Commit paths:
  `egon-cola-components/egon-cola-component-agent-flow-starter/src/test/java/top/egon/cola/component/agentflow/runtime/{AgentFlowRegistryFactoryTest.java,DefaultAgentFlowRegistryTest.java}`;
  `egon-cola-components/egon-cola-component-agent-flow-starter/src/main/java/top/egon/cola/component/agentflow/{api/AgentFlowDescriptorDTO.java,exception/AgentFlowNotFoundException.java,runtime/AgentFlowRuntimeBO.java,runtime/AgentFlowRegistry.java}`;
  `egon-cola-components/egon-cola-component-agent-flow-starter/src/main/java/top/egon/cola/component/agentflow/runtime/{AgentFlowRegistryFactory.java,DefaultAgentFlowRegistry.java}`
- Commit: `feat(agent-flow): publish atomic runtime registry`

### Step 5 — 实现会话互斥、同步流式执行与关闭管线

- Requirements: REQ-007, REQ-008, REQ-009, REQ-010, REQ-012, REQ-013, REQ-015
- Dependencies: Step 4
- Baseline state: Registry能查找/关闭 Runner，但没有 public Service、tuple guard、Session命令或执行/取消/超时语义。
- Observable outcome: public Java API支持 list/create/delete/execute/executeStream；同 tuple互斥，跨 tuple并行，所有终态释放且
  close有界。
- End state: 核心组件行为完成；尚未由 Spring AutoConfiguration创建 Beans。
- Test-first gate: Required — Guard与 Service tests先覆盖并发、Session、事件顺序、lazy subscription、timeout、cancel/error和
  close，观察 RED后再实现 API/exceptions/guard/service。
- Manual Checks: MC-VALID-001, MC-NAME-001, MC-MODEL-001, MC-LOG-001, MC-BEAN-001, MC-TIME-001, MC-SCOPE-001,
  MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 10, Rule 11
- Ordered files:

#### File 1 —
`CREATE egon-cola-components/egon-cola-component-agent-flow-starter/src/test/java/top/egon/cola/component/agentflow/execution/{AgentFlowSessionExecutionGuardTest.java,DefaultAgentFlowServiceTest.java}`

- Purpose: 用真实 RxJava信号和 controlled ADK seams固定 tuple锁、Session与完整执行生命周期。
- Symbols: Spec TEST-008至019、021/022 scenarios；barrier、TestSubscriber/TestScheduler、fake BaseAgent/Runner/session
  service、log capture。
- Repository evidence: ADK 0.7 `runAsync`返回 Flowable且 SessionService提供 create/get/delete；Spec要求同步只收集该流。
- Dependencies and consumers: 测试依赖 Step 4 Registry fixture；Step 6 Context test复用 fake ChatModel但不复制
  Service行为测试。
- Why now: Service是最高风险并发面，必须在写实现前明确 acquire/subscribe/finally/close次序。
- Contract/signature changes: 无生产签名；每个 subscription触发一次 execution，测试不调用真实模型。
- Input/output and state mapping: commands -> validation -> runtime/session -> lease -> events/terminal ->
  release；close -> reject new/wait/close runners。
- Error and edge behavior: missing flow/session、busy/delete race、empty stream、upstream error、deadline、cancel、double
  subscribe、close race全部单独断言。
- Standards impact: MC-VALID-001、MC-LOG-001、MC-TIME-001、MC-TEST-001；测试日志不含合成身份/prompt。
- Literal rule enforcement: Rule 2 覆盖 validation groups；Rule 4 验证 safe Slf4j；Rule 5 使用 RxJava/JDK测试工具；Rule 10
  用 Clock/Duration/Instant；Rule 11 仅 execution测试包。
- Implementation pseudocode:

```java
race two acquire calls for the same flowId/userId/sessionId and assert exactly one lease; run different tuples concurrently and assert both succeed
create and delete sessions through a real in-memory session service, including missing/repeat/busy/delete-versus-execute cases without check-then-act gaps
script ordered, empty, error and never-ending event streams; assert sync immutable collection and stream lazy subscription preserve event order and map terminal errors once
cancel or advance virtual deadline, then assert upstream disposal, one guard release, partial events retained and a second run can acquire; race close and active lease with bounded timeout
```

- Verification contribution: TEST-008至019、021/022完整 RED/GREEN目标。
- After this file: tests因 public API/exceptions/guard/service缺失而 RED。

#### File 2 —
`CREATE egon-cola-components/egon-cola-component-agent-flow-starter/src/main/java/top/egon/cola/component/agentflow/{api/AgentFlowService.java,api/AgentFlowExecutionCommand.java,api/AgentFlowSessionCommand.java,api/AgentFlowSessionResult.java,exception/AgentFlowSessionNotFoundException.java,exception/AgentFlowSessionBusyException.java,exception/AgentFlowExecutionTimeoutException.java,exception/AgentFlowExecutionException.java}`

- Purpose: 冻结宿主消费 API、命令/结果与可分类执行异常。
- Symbols: five `AgentFlowService` methods；three records；four typed exceptions。
- Repository evidence: Spec §9给出精确 signatures，§10给出字段、groups、Instant/null规则。
- Dependencies and consumers: Service接口返回 ADK Event/RxJava Flowable；Default service实现；Phase 2 Infrastructure
  gateway消费。
- Why now: Tests先定义行为，随后固定最小 public surface，避免把 Runner/SessionService暴露给宿主。
- Contract/signature changes: 新 public Java API；`execute`返回 `List<Event>`，`executeStream`返回 `Flowable<Event>`；无
  overload/HTTP。
- Input/output and state mapping: session command包含 tuple；execution command包含 tuple/message；result包含 tuple +
  createdAt；异常只携 safe identifiers。
- Error and edge behavior: create/delete使用不同 validation group；explicit null/blank fail；timeout/cause分类稳定且不序列化第三方
  state。
- Standards impact: MC-NAME-001、MC-VALID-001、MC-MODEL-001、MC-JSON-001、MC-TIME-001。
- Literal rule enforcement: Rule 1使用 Command/Result；Rule 2 groups在边界；Rule 3不复制 ADK Event；Rule 6无外部
  JSON设计；Rule 10用 Instant；Rule 11位于 api/exception。
- Implementation pseudocode:

```java
declare listFlows, createSession, deleteSession, execute and executeStream exactly once with the Spec-defined immutable input and output types
define commands as records with Jakarta constraints and session validation groups so create requires absent sessionId while delete/execute require nonblank sessionId
return ADK Event objects unchanged in immutable order for sync and as Flowable for streaming; do not expose Runner, SessionService or mutable state
classify not-found, busy, timeout and execution failures with safe flow-level context and preserve causes without embedding userId, sessionId, message or event content
```

- Verification contribution: public contract编译，validation/error tests可精确断言。
- After this file: Service API存在，Guard/实现行为仍 RED。

#### File 3 —
`CREATE egon-cola-components/egon-cola-component-agent-flow-starter/src/main/java/top/egon/cola/component/agentflow/execution/{AgentFlowSessionExecutionGuard.java,DefaultAgentFlowService.java}`

- Purpose: 以原子 tuple lease和共享 reactive pipeline实现 Session/执行/取消/超时/close。
- Symbols: guard acquire/delete lease/global closing/active count；Service五方法和 `close()`；private `executePipeline`。
- Repository evidence: Spec §7.3/§9.2规定 subscription-time acquire、无 retry、doFinally释放和 delay-error shutdown。
- Dependencies and consumers: 依赖 named Registry、Guard、Properties、ValidationUtils/Clock；Step 6注册为 `agentFlowService`
  destroyMethod close。
- Why now: API与生命周期 tests均已建立，直接实现唯一 orchestration owner。
- Contract/signature changes: `DefaultAgentFlowService implements AgentFlowService, AutoCloseable`；不新增 public helper或
  queue/retry配置。
- Input/output and state mapping: tuple -> ConcurrentHashMap lease；ADK Content/message -> `runAsync`
  ；events原样转发；terminal -> release；close -> global gate/await/registry close。
- Error and edge behavior: acquire fail-fast；getSession empty映射专用异常；delete与execute同一原子
  guard；timeout/cancel/error使用 `doFinally` exactly once；日志allowlist。
- Standards impact: MC-VALID-001、MC-LOG-001、MC-BEAN-001、MC-UTIL-001、MC-TIME-001、MC-PATTERN-001。
- Literal rule enforcement: Rule 2入口统一 ValidationUtils；Rule 4 Slf4j/final
  fields/RequiredArgsConstructor/Qualifier；Rule 5复用 ConcurrentHashMap/RxJava；Rule 10 UTC Clock；Rule 11 flat
  execution包。
- Implementation pseudocode:

```java
validate each command with its operation group, require an OPEN runtime and atomically acquire a lease keyed by flowId/userId/sessionId before touching session state
create/delete via runner.sessionService using flowId as appName; hold the same lease through delete so execute cannot pass a separate pre-check
for execution use Flowable.defer to acquire per subscription, verify session, convert message to ADK Content and call runner.runAsync without retry
apply one total timeout, map timeout and upstream errors to typed failures, preserve Event values/order, and release the lease exactly once from doFinally for complete/error/cancel
sync execute calls the same pipeline and collects an immutable list; close rejects new leases, waits up to shutdownTimeout, invokes registry close for all runners and records only safe metrics
```

- Verification contribution: TEST-008至019、021/022全部 GREEN，并完成 public Java behavior。
- After this file: 组件核心可直接构造使用；只差 Spring Boot装配。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -f egon-cola-components/egon-cola-component-agent-flow-starter/pom.xml test -Dtest=AgentFlowSessionExecutionGuardTest,DefaultAgentFlowServiceTest`
- Expected result: 所有 Session/并发/同步/流式/取消/timeout/close场景通过；fake upstream观察到 disposal；日志捕获无
  identity/content。
- Failure returns to: File 1若 fixture不复现真实 Flowable信号；File 2若 API偏离 Spec；File 3若 lease/terminal/close
  exactly-once失败。
- Completion criteria: REQ-007至013 的运行时行为全部由离线 tests覆盖，且无 retry或状态泄漏。
- Rollback: 回退本 Step tests/API/exceptions/execution files；Registry与 factory仍可独立测试。
- Commit paths:
  `egon-cola-components/egon-cola-component-agent-flow-starter/src/test/java/top/egon/cola/component/agentflow/execution/{AgentFlowSessionExecutionGuardTest.java,DefaultAgentFlowServiceTest.java}`;
  `egon-cola-components/egon-cola-component-agent-flow-starter/src/main/java/top/egon/cola/component/agentflow/{api/AgentFlowService.java,api/AgentFlowExecutionCommand.java,api/AgentFlowSessionCommand.java,api/AgentFlowSessionResult.java,exception/AgentFlowSessionNotFoundException.java,exception/AgentFlowSessionBusyException.java,exception/AgentFlowExecutionTimeoutException.java,exception/AgentFlowExecutionException.java}`;
  `egon-cola-components/egon-cola-component-agent-flow-starter/src/main/java/top/egon/cola/component/agentflow/execution/{AgentFlowSessionExecutionGuard.java,DefaultAgentFlowService.java}`
- Commit: `feat(agent-flow): execute isolated ADK sessions`

### Step 6 — 接入默认关闭的 Spring Boot 自动配置

- Requirements: REQ-003, REQ-005, REQ-006, REQ-011, REQ-012, REQ-015
- Dependencies: Steps 2-5
- Baseline state: 核心对象可手工构造，但没有 AutoConfiguration.imports、strict Binder、显式 Bean名或 override条件。
- Observable outcome: disabled时零 Bean；enabled时严格绑定、收集 named ChatModels、按 Spec Bean顺序装配并在失败时阻断
  context。
- End state: Starter被 Boot自动发现，所有 Beans具有稳定名称、constructor/Qualifier与 override seam。
- Test-first gate: Required — 先创建 ApplicationContextRunner测试形成 missing imports/beans/binding RED，再实现
  AutoConfiguration和 imports。
- Manual Checks: MC-ARCH-001, MC-VALID-001, MC-LOG-001, MC-BEAN-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 2, Rule 4, Rule 5, Rule 7, Rule 11
- Ordered files:

#### File 1 —
`CREATE egon-cola-components/egon-cola-component-agent-flow-starter/src/test/java/top/egon/cola/component/agentflow/autoconfigure/AgentFlowAutoConfigurationTest.java`

- Purpose: 固定 default-off、strict binding、具名 ChatModel map、全部 Bean名、override和 startup cleanup合同。
- Symbols: tests TEST-001/006 plus valid context、unknown key、missing Bean、invalid graph、custom override、partial compile
  failure。
- Repository evidence: rule-engine相邻测试使用 ApplicationContextRunner；Spec列出 16个显式 Bean名和 override规则。
- Dependencies and consumers: 使用 fake ChatModel、synthetic properties和 user configurations；不打开 Web context或端口。
- Why now: 自动配置只负责 wiring，必须由 context-level test证明没有隐式扫描/Bean名称漂移。
- Contract/signature changes: 无生产合同；测试按 Bean name+type查询而非字段注入。
- Input/output and state mapping: property map + named fake Beans -> context Beans/startup failure；关闭 context ->
  Service close。
- Error and edge behavior: absent/false不绑定复杂配置；enabled缺任何前置、unknown key、wrong type或 graph失败均 context
  failure且清理 Runner。
- Standards impact: MC-VALID-001、MC-BEAN-001、MC-CONFIG-001、MC-TEST-001；精确 DI和配置证据。
- Literal rule enforcement: Rule 2验证 ValidationUtils Bean；Rule 4逐名/Qualifier/override；Rule 7 strict prefix；Rule 11无
  component scan引入架构层。
- Implementation pseudocode:

```java
run context with no enabled property and with enabled=false; assert none of the named Agent Flow beans exists
provide valid strict properties and a named fake ChatModel, then assert every Spec bean name/type and immutable strategy/model maps exist exactly once
add an unknown key, missing/wrong model bean, invalid graph and failing second flow; assert context startup fails and any first runner is closed
provide same-name custom override beans one at a time, assert ConditionalOnMissingBean honors them, then close context and assert agentFlowService close executes once
```

- Verification contribution: TEST-001/006和自动配置 lifecycle RED/GREEN。
- After this file: 测试因 AutoConfiguration/imports缺失而 RED。

#### File 2 —
`CREATE egon-cola-components/egon-cola-component-agent-flow-starter/src/main/java/top/egon/cola/component/agentflow/autoconfigure/AgentFlowAutoConfiguration.java`

- Purpose: 使用显式 `@Bean(name=...)`依赖顺序装配 Properties、validator、strategies、factories、Registry、Guard与 Service。
- Symbols: Spec §8.3 Bean table中的全部方法；`@AutoConfiguration`、property condition、missing-bean conditions。
- Repository evidence: Components Starter通过 AutoConfiguration类和 imports注册；Spec拒绝动态 BeanDefinition与隐式生成
  Properties Bean名。
- Dependencies and consumers: 读取 Environment/Binder和 ListableBeanFactory仅在配置方法；生产 collaborators只接收类型安全
  snapshot。
- Why now: 所有核心构造函数已通过单测，wiring可以保持无业务分支。
- Contract/signature changes: enabled=true时提供稳定 Bean集合；enabled absent/false不提供；同名 override按表允许。
- Input/output and state mapping: environment prefix -> strict record；BeanFactory ChatModel names -> immutable
  map；properties -> Registry -> Service。
- Error and edge behavior: unknown/unbound/invalid type、无 Validator/ChatModel、duplicate named aggregate、Registry
  failure均启动失败；不降级空 Registry。
- Standards impact: MC-ARCH-001、MC-BEAN-001、MC-CONFIG-001、MC-LOG-001。
- Literal rule enforcement: Rule 4显式名/constructor/Qualifier且无 field injection；Rule 5使用 Boot Binder；Rule 7
  default-off/strict binding；Rule 11配置类不含业务流程。
- Implementation pseudocode:

```java
activate the auto-configuration only when egon.cola.component.agent-flow.enabled=true and never create partial beans when disabled
bind the complete prefix into AgentFlowProperties with a strict unbound-elements handler, then create Clock and ValidationUtils under the exact approved names
declare three named strategies, one immutable ordered strategy list, strategy/model/flow/registry factories and collect all ChatModel beans by their Spring names into a snapshot
create registry only after full validation/compilation and create DefaultAgentFlowService with named registry, guard, properties and Clock; set destroyMethod to close
apply ConditionalOnMissingBean name/type rules exactly as the Spec table and keep Environment/BeanFactory out of production collaborators
```

- Verification contribution: 自动配置 Bean、strict bind、startup fail closed与 context close进入 GREEN。
- After this file: ContextRunner通过直接加载 AutoConfiguration；classpath自动发现仍待 imports。

#### File 3 —
`CREATE egon-cola-components/egon-cola-component-agent-flow-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- Purpose: 使用 Boot 3约定公开唯一 AutoConfiguration入口。
- Symbols: `top.egon.cola.component.agentflow.autoconfigure.AgentFlowAutoConfiguration`。
- Repository evidence: rule-engine和其他 Starter在同路径登记全限定类名。
- Dependencies and consumers: Spring Boot autoconfiguration loader消费；module jar资源打包。
- Why now: 配置类测试通过后再启用 classpath发现，便于定位 wiring失败。
- Contract/signature changes: 引入 Starter且 enabled=true的宿主自动加载配置；默认 off仍无 Bean。
- Input/output and state mapping: classpath resource line -> AutoConfiguration candidate；无环境写入。
- Error and edge behavior: 恰好一行、无重复/旧类名；缺类由 context test失败。
- Standards impact: MC-ARCH-001、MC-BEAN-001、MC-CONFIG-001、MC-SCOPE-001。
- Literal rule enforcement: Rule 4 登记唯一具名配置；Rule 7 遵循 Boot约定；Rule 11 不增加扫描包或额外 module。
- Implementation pseudocode:

```text
write exactly one fully qualified AgentFlowAutoConfiguration class name using UTF-8
do not add legacy spring.factories or component scanning entries
load the Starter with ApplicationContextRunner discovery and assert default-off plus enabled-on behavior remains unchanged
```

- Verification contribution: 完成 Starter classpath discovery与 TEST-001。
- After this file: 新 Starter从依赖到核心行为和 Boot装配全部可用。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -f egon-cola-components/egon-cola-component-agent-flow-starter/pom.xml test -Dtest=AgentFlowAutoConfigurationTest,AgentFlowPropertiesBindingTest,AgentFlowRegistryFactoryTest`
- Expected result: disabled无 Beans；valid enabled context全部 Bean名正确；非法配置/缺模型失败且无 partial
  Registry；close清理完成。
- Failure returns to: File 1若期望与 Bean表不一致；File 2若 binding/Bean order/override错误；File 3若 discovery失败。
- Completion criteria: REQ-003/005/006/011/012的 Spring wiring闭环且 REQ-015保持无端口/外部调用。
- Rollback: 回退 AutoConfiguration test/class/imports；核心对象仍可手工构造验证。
- Commit paths:
  `egon-cola-components/egon-cola-component-agent-flow-starter/src/test/java/top/egon/cola/component/agentflow/autoconfigure/AgentFlowAutoConfigurationTest.java`;
  `egon-cola-components/egon-cola-component-agent-flow-starter/src/main/java/top/egon/cola/component/agentflow/autoconfigure/AgentFlowAutoConfiguration.java`;
  `egon-cola-components/egon-cola-component-agent-flow-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Commit: `feat(agent-flow): auto-configure named runtime beans`

### Step 7 — 冻结 BOM、组件文档与完整发布合同

- Requirements: REQ-001, REQ-002, REQ-003, REQ-013, REQ-014, REQ-015
- Dependencies: Steps 1-6
- Baseline state: Starter focused tests可通过，但 Components BOM未导出，README/architecture未记录配置、API、生命周期和第三方日志限制。
- Observable outcome: BOM消费者可无版本引入唯一 Starter；中英文文档与架构文档精确说明启用、配置、边界、并发/取消/close和升级
  Gate。
- End state: focused verify、dependency contract和 components reactor test形成 Phase 2可消费的完成证据。
- Test-first gate: Required — 先在 Step 1 contract test新增 BOM/docs完整断言并观察 RED，再修改 BOM/docs至 GREEN。
- Manual Checks: MC-ARCH-001, MC-DEP-001, MC-LOG-001, MC-JSON-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 4, Rule 5, Rule 6, Rule 7, Rule 10, Rule 11
- Ordered files:

#### File 1 —
`MODIFY egon-cola-components/egon-cola-component-agent-flow-starter/src/test/java/top/egon/cola/component/agentflow/contract/AgentFlowComponentContractTest.java`

- Purpose: 在已完成的 source/dependency合同上先增加 BOM导出与双语/架构文档一致性 RED。
- Symbols: tests `is_exported_once_by_components_bom`、`documents_exact_configuration_api_and_boundaries`、
  `keeps_chinese_english_contracts_in_sync`。
- Repository evidence: Step 1已建立同一静态合同测试类；当前 BOM/docs尚无 Agent Flow条目。
- Dependencies and consumers: 读取 BOM POM、module README、Components/BOM双语索引与 architecture文档；后续 final verify执行。
- Why now: public artifact/API稳定后才冻结发布和文档合同，并保持测试先于 BOM/docs实现。
- Contract/signature changes: 只增加测试断言；BOM只允许一个 Egon artifact，文档必须包含 exact keys/methods/limits。
- Input/output and state mapping: POM/docs text -> artifact/link/config/API/boundary assertions；不写被测文件。
- Error and edge behavior: 缺/重复 artifact、三方 coordinate误入 BOM、中英文 key/API不一致、宣称 provider/MCP/DB/恢复均
  fail。
- Standards impact: MC-ARCH-001、MC-DEP-001、MC-LOG-001、MC-CONFIG-001、MC-SCOPE-001、MC-TEST-001。
- Literal rule enforcement: Rule 5 检查 BOM依赖边界；Rule 6 检查无外部 JSON扩张；Rule 7 检查配置说明；Rule 11 检查 flat
  component架构描述。
- Implementation pseudocode:

```java
assert the Components BOM contains exactly one top.egon agent-flow Starter dependency and no org.springframework.ai or com.google.adk public entries
read both module READMEs and require the same five AgentFlowService methods, configuration keys, version matrix, timeout/cancel/close and in-memory limitations
read Components and BOM indexes plus architecture document and require matching module links and the host-owned provider/MCP/HTTP/database boundaries
fail on duplicate artifact rows, broken links, credential examples, runtime recovery claims or Chinese/English contract drift
```

- Verification contribution: 为 REQ-013/014与 final docs建立真实 RED，不在早期 Step跳过断言。
- After this file: focused contract test因 BOM/docs缺失按预期 RED。

#### File 2 — `MODIFY egon-cola-components/egon-cola-components-bom/pom.xml`

- Purpose: 向公共 Components BOM导出唯一 Egon Agent Flow Starter artifact。
- Symbols: dependencyManagement entry `top.egon:egon-cola-component-agent-flow-starter:${project.version}`。
- Repository evidence: BOM当前逐项管理 Egon runtime artifacts且不导出其三方传递依赖。
- Dependencies and consumers: Phase 2 source root导入 Components BOM并无版本依赖 Starter；其他 consumers不受影响。
- Why now: 只有 public API/behavior稳定后才冻结 BOM surface。
- Contract/signature changes: 新增一个 managed Egon GAV；不新增 Spring AI/ADK coordinates。
- Input/output and state mapping: BOM version -> Starter consumer version；三方版本仍由 component parent构建控制。
- Error and edge behavior: duplicate/missing version或三方误入 BOM由 contract test失败。
- Standards impact: MC-REUSE-001、MC-DEP-001、MC-SCOPE-001、MC-TEST-001。
- Literal rule enforcement: Rule 5 不新增实现依赖；Rule 11 只导出单 Starter，不改变模块结构。
- Implementation pseudocode:

```xml
add one dependencyManagement entry for top.egon:egon-cola-component-agent-flow-starter at project.version beside comparable Starter artifacts
retain every existing BOM entry and ordering convention
assert no org.springframework.ai or com.google.adk dependency is added to this public Egon BOM
```

- Verification contribution: 完成 REQ-014与 contract test BOM assertion。
- After this file: consumers可通过 Components BOM解析 Starter版本。

#### File 3 — `CREATE egon-cola-components/egon-cola-component-agent-flow-starter/{README.md,README.zh-CN.md}`

- Purpose: 提供双语引入、具名 ChatModel、YAML、Java API、取消/超时/close和 V1限制说明。
- Symbols: dependency snippet、Bean example、single/sequential/parallel/loop config、five Service methods、version
  matrix、security/logging note。
- Repository evidence: Components BOM/Starter普遍有双语 README；Spec §16要求默认关闭与升级风险可见。
- Dependencies and consumers: 应用开发者与 Phase 2实现者消费；示例必须与 `AgentFlowProperties`/API精确一致。
- Why now: 代码与 Bean名已冻结，文档不会猜测未实现合同。
- Contract/signature changes: 文档合同；不提供 HTTP/provider/MCP配置示例或真实密钥。
- Input/output and state mapping: YAML keys映射到 record字段，Java example映射到 Service调用；明确内存状态/无恢复。
- Error and edge behavior: 说明 invalid config启动失败、busy/timeout/cancel语义、partial events不回滚、第三方 debug日志注意事项。
- Standards impact: MC-CONFIG-001、MC-LOG-001、MC-JSON-001、MC-SCOPE-001。
- Literal rule enforcement: Rule 1/2示例使用准确 Command/Result和validation；Rule 6不引入外部JSON；Rule 7
  profile-neutral；Rule 10示例只用 Duration；Rule 11明确 flat component boundary。
- Implementation pseudocode:

```text
document BOM-based dependency and a named host ChatModel bean without any provider URL, credential or vendor-specific starter
show one complete egon.cola.component.agent-flow configuration including leaf, sequential, parallel and loop shapes with safe synthetic prompts
show create, execute, executeStream and delete lifecycle with cancellation ownership and typed failures; state process-local session and no retry guarantees
record Java/Boot/Spring AI/ADK versions, dependency upgrade revalidation, safe component log allowlist and third-party debug logging restriction in both languages
```

- Verification contribution: static contract验证 config key/API名/禁用 surface，人工文档 parity检查。
- After this file: Starter拥有可消费且不泄密的双语使用说明。

#### File 4 —
`MODIFY egon-cola-components/{README.md,README.zh-CN.md,egon-cola-components-architecture.md,egon-cola-components-bom/README.md,egon-cola-components-bom/README.zh-CN.md}`

- Purpose: 在组件目录、BOM清单与架构能力表登记 Agent Flow及其明确边界。
- Symbols: Agent Flow capability row/link、BOM artifact list、flat Starter exception、host-owned model/provider/MCP说明。
- Repository evidence: 当前四份索引文档列组件/BOM入口；架构文档 §1-§5区分 light Starter与服务层。
- Dependencies and consumers: maintainers、BOM users与 Phase 2 Plan消费；不改变代码构建。
- Why now: module验证完成后同步单一可发现入口，避免文档先于实现。
- Contract/signature changes: 组件目录从现有集合增加 Agent Flow；架构不改写已有规则，仅登记 approved flat capability。
- Input/output and state mapping: module/BOM真实路径 -> doc links；边界矩阵 -> allowed/forbidden responsibilities。
- Error and edge behavior: 中英文列表数量/坐标不一致或宣称持久化/HTTP/MCP/provider均由 review/contract失败。
- Standards impact: MC-ARCH-001、MC-DEP-001、MC-CONFIG-001、MC-SCOPE-001。
- Literal rule enforcement: Rule 5 记录依赖owner；Rule 7 记录唯一 prefix；Rule 11 明确这是 Components flat Starter例外而非
  DDD业务层。
- Implementation pseudocode:

```text
add the Agent Flow Starter path and short capability description to both Components indexes with links to its matching-language README
add exactly one managed artifact row to both BOM documents and state that provider/ADK coordinates are not separate Egon BOM exports
extend the architecture capability table with a flat Starter boundary: owns configuration/compiler/registry/session execution, excludes HTTP/MCP/provider/database/UI
cross-check all names, versions and links against the built module and keep all existing component descriptions unchanged
```

- Verification contribution: 完成 REQ-001/003/013/014文档与 architecture gate。
- After this file: 组件在代码、BOM、索引和架构文档中一致可发现。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-agent-flow-starter -am clean verify && ./mvnw -B -ntp -f egon-cola-components/egon-cola-component-agent-flow-starter/pom.xml dependency:tree -Dscope=runtime && ./mvnw -B -ntp -f egon-cola-components/pom.xml test`
- Expected result: 10个目标测试类全部通过；dependency tree精确版本且无禁用 artifact；Components Reactor test exit
  0；BOM只新增一个 Egon GAV。
- Failure returns to: File 1若 contract断言错误；File 2若 BOM consumer失败；File 3若 API/config/doc不一致；File 4若索引/架构越界；若
  unrelated AccessGuard baseline失败则记录外部 blocker且不跨 scope修复。
- Completion criteria: REQ-001至015全部有 focused、dependency、reactor或文档证据，Phase 2前置条件满足。
- Rollback: 回退本 Step BOM/docs；Starter代码保持可构建但暂不公开给 BOM consumer。
- Commit paths:
  `egon-cola-components/egon-cola-component-agent-flow-starter/src/test/java/top/egon/cola/component/agentflow/contract/AgentFlowComponentContractTest.java`;
  `egon-cola-components/egon-cola-components-bom/pom.xml`;
  `egon-cola-components/egon-cola-component-agent-flow-starter/{README.md,README.zh-CN.md}`;
  `egon-cola-components/{README.md,README.zh-CN.md,egon-cola-components-architecture.md,egon-cola-components-bom/README.md,egon-cola-components-bom/README.zh-CN.md}`
- Commit: `docs(agent-flow): publish component contract and usage`

## 8. Test, Validation, and Quality Gates

| Gate/order               | Working directory          | Command or method                                                                                 | Scope                           | Expected result                                  | Failure returns to            | Requirements/runtime boundary         |
|--------------------------|----------------------------|---------------------------------------------------------------------------------------------------|---------------------------------|--------------------------------------------------|-------------------------------|---------------------------------------|
| 1 RED dependency         | repository root            | module contract test after File 1/2                                                               | module/BOM/module-list          | fails on missing parent/BOM contracts            | Step 1 File 2                 | REQ-001/002; static                   |
| 2 GREEN dependency       | repository root            | focused contract + dependency tree                                                                | effective classpath             | exact 1.1.8/0.7.0; no dev/Web/DB/provider        | Step 1 File 1/3               | REQ-002/015; Maven only               |
| 3 RED/GREEN config       | Starter module             | `test -Dtest=AgentFlowPropertiesBindingTest,AgentFlowConfigValidatorTest`                         | immutable config/graph          | all TEST-002至005 pass                            | Step 2 owning file            | REQ-004/005/011                       |
| 4 RED/GREEN compiler     | Starter module             | `test -Dtest=AgentWorkflowStrategyFactoryTest,AgentFlowFactoryTest`                               | ADK builders/adapter            | four root types, no network                      | Step 3                        | REQ-002至006                           |
| 5 RED/GREEN registry     | Starter module             | `test -Dtest=AgentFlowRegistryFactoryTest,DefaultAgentFlowRegistryTest`                           | atomic registry/close           | no partial publish; all close attempted          | Step 4                        | REQ-006/012/013                       |
| 6 RED/GREEN execution    | Starter module             | `test -Dtest=AgentFlowSessionExecutionGuardTest,DefaultAgentFlowServiceTest`                      | session/concurrency/RxJava      | TEST-008至019/021/022 pass                        | Step 5                        | REQ-007至013                           |
| 7 RED/GREEN wiring       | Starter module             | `test -Dtest=AgentFlowAutoConfigurationTest`                                                      | Boot context                    | default off, strict/on/override/close pass       | Step 6                        | REQ-003/005/006/011/012               |
| 8 Focused verify         | repository root            | `./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-agent-flow-starter -am clean verify` | module + prerequisites          | BUILD SUCCESS, all 10 target test classes        | owning Step                   | REQ-001至015; no service start         |
| 9 Dependency gate        | Starter module             | `./mvnw -B -ntp dependency:tree -Dscope=runtime` plus static allow/deny assertions                | runtime artifacts               | no forbidden artifacts/versions                  | Step 1                        | REQ-002/003/014                       |
| 10 Components regression | repository root            | `./mvnw -B -ntp -f egon-cola-components/pom.xml test`                                             | components reactor              | BUILD SUCCESS or unrelated baseline isolated     | owning external module/Step 7 | REQ-014/015                           |
| 11 Runtime/manual        | user-controlled deployment | N/A in this Plan                                                                                  | real provider/load/cancellation | not claimed; host validates after implementation | future operational work       | no browser/model/MCP/DB authorization |

`TEST-025` 对应 Gate 8，`TEST-026` 对应 Gate 10，`TEST-027` 对应 Gate 9。每个 Step先运行本 Step focused command，再运行 Gate
8；Gate 10仅在 Step 7执行。若现有 AccessGuard test baseline失败，必须记录失败命令与模块，不能在本 Plan里修复。

## 9. Migration, Compatibility, Rollout, and Rollback

数据库/Flyway、数据回填、API版本、事件 schema和 UI迁移均为 N/A：primary Spec §8/§9/§11明确本组件没有数据库、外部 HTTP、消息或
UI。兼容与 rollout顺序如下：

1. 先完成 dependency compatibility和所有 focused tests；再导出 Components BOM。
2. 默认 `enabled=false` 保证仅升级 BOM/依赖不会激活 runtime。
3. 宿主只有在定义具名 ChatModel并配置完整 Flow后显式启用；invalid配置启动失败而不部分服务。
4. ADK/Spring AI任何版本升级都必须重新执行 Step 1/3/5/9 gates并新建 Spec，不通过局部版本修补。
5. 每 Step可通过 path-limited revert回滚；已创建的 JVM session无 durable迁移，进程停止即丢失，文档要求调用方重建。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps   | Files                            | Tests/gates          | Completion evidence          |
|-------------|------------------------|---------|----------------------------------|----------------------|------------------------------|
| `REQ-001`   | Spec §4/§8             | 1, 7    | module/parent/docs               | TEST-023, Gate 8     | flat tree + module reactor   |
| `REQ-002`   | Spec §4/§8             | 1, 3, 7 | POM/model adapter/docs           | TEST-007/024/027     | exact dependency tree        |
| `REQ-003`   | Spec §4/§8             | 3, 6, 7 | adapter factory/auto config/docs | TEST-006/024         | named ChatModel only         |
| `REQ-004`   | Spec §4/§7             | 2, 3    | config/strategies/factory        | TEST-003/007         | four ADK root shapes         |
| `REQ-005`   | Spec §4/§7             | 2, 3, 6 | validator/factory/auto config    | TEST-003至005         | invalid graph fail closed    |
| `REQ-006`   | Spec §4/§7             | 3, 4, 6 | factories/registry/auto config   | TEST-006/007/020     | atomic immutable registry    |
| `REQ-007`   | Spec §4/§9             | 4, 5    | registry/API/service             | TEST-008至010         | tuple sessions               |
| `REQ-008`   | Spec §4/§9             | 5       | API/service                      | TEST-011/012/016     | ordered Event sync/stream    |
| `REQ-009`   | Spec §4/§9             | 5       | guard/service                    | TEST-010/014/018     | same-tuple exclusion         |
| `REQ-010`   | Spec §4/§9             | 5       | service/exceptions               | TEST-013/015/017至019 | no retry; dispose/release    |
| `REQ-011`   | Spec §4/§8             | 2, 6    | properties/auto config/imports   | TEST-001/002         | default off, strict bind     |
| `REQ-012`   | Spec §4/§15            | 4, 5, 6 | registry/service/auto config     | TEST-021/022         | bounded delay-error close    |
| `REQ-013`   | Spec §4/§15            | 4, 5, 7 | descriptor/logging/docs          | TEST-005/020/023     | safe allowlist logs          |
| `REQ-014`   | Spec §4/§8             | 7       | BOM/docs                         | TEST-024/026/027     | one Egon BOM entry           |
| `REQ-015`   | Spec §4/§14            | 1-7     | all tests/POM/docs               | TEST-001至027         | offline deterministic verify |

## 11. Risks, Blockers, and User Decisions

| ID             | Risk or decision                                              | Impacted Steps/files | Evidence                             | Owner         | Status/action                                       |
|----------------|---------------------------------------------------------------|----------------------|--------------------------------------|---------------|-----------------------------------------------------|
| `RISK-001`     | ADK adapter编译目标是 Spring AI 1.1.0而本组件管理1.1.8                   | Steps 1, 3, 5        | downloaded 0.7.0 POM/source          | Implementer   | Controlled — compile/linkage tests先行；失败返回 Spec      |
| `RISK-002`     | `google-adk-spring-ai`传递引入 `google-adk-dev`                   | Step 1 POM/contract  | 0.7.0 published POM                  | Implementer   | Controlled — explicit exclusion + runtime tree gate |
| `RISK-003`     | 第三方 SpringAI adapter可在其 debug路径观察 prompt/response             | Step 7 docs          | adapter source含 observability hooks  | Host operator | Controlled — component自身零内容日志；文档要求生产禁用第三方内容 debug   |
| `RISK-004`     | Flowable取消不保证 provider底层网络立即终止                                | Step 5               | Spec cancellation boundary           | Host operator | Accepted limitation — fake disposal可证，真实 provider另验 |
| `RISK-005`     | 全 Components Reactor可能被既有 AccessGuard validation provider问题阻断 | Step 7 Gate 10       | prior repository evidence，当前未重跑      | Implementer   | Controlled —先复现并隔离报告，不跨 scope修复                     |
| `DECISION-001` | component-library flat profile例外                              | all source paths     | user accepted Spec Rule 11 exception | User          | Closed — implement exact flat target tree           |
| `DECISION-002` | Java 21/Boot 3.5.16 + Spring AI 1.1.8/ADK 0.7.0               | POM/factories        | user accepted recommended versions   | User          | Closed — no version substitution                    |

没有未解决的 Spec或用户决策 blocker。任一 RISK在执行时触发失败，只阻断所属 Step并返回指定 Spec/file；不授权扩大依赖、升级
Boot或修改无关组件。

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

七个 Step覆盖“参考 Agent Workflow能力 + Spring AI + Google ADK + Components扁平结构”，并明确排除参考工程
DDD包、HTTP/provider/MCP/数据库/UI。Phase 2只作为完成后消费者，不混入本 Plan。

### 12.2 Spec consistency

Plan逐项实现 Accepted Spec REQ-001至015，没有修改 public API、配置 prefix、版本、Bean名、会话 tuple或 lifecycle语义。四项 Plan
Clarification只记录已发布 dependency事实、package-private测试 seam、第三方日志边界和 Phase 2 gate，均不改变产品行为。Simplicity
audit未发现 fetch-then-forward接口；Strategy仅用于三类真实 builder变化，其余路径保持直接实现。

### 12.3 Repository executability

所有 MODIFY路径已在当前 baseline复核，CREATE路径当前不存在；ADK 0.7.0真实 sources确认所用
constructor、builders、SessionService、`runAsync`与 `close`。每个 Step有 RED/GREEN、精确 cwd/command、failure
return、rollback和路径限定 commit。现有 dirty Spec/Plan/tsbuildinfo均不在实现提交范围。

### 12.4 Test and release completeness

验证从静态/dependency contract、配置图、真实 ADK类型、Registry、并发/RxJava lifecycle、Boot context、BOM到 Components
Reactor逐层升级。所有测试使用 fake/controlled seams；真实 provider取消、性能、服务启动和网络均明确不在已验证边界。

### 12.5 Blocking Manual Check

| Check ID         | Applicability    | Status | Evidence                                             | Finding                                      | Required action/exception |
|------------------|------------------|--------|------------------------------------------------------|----------------------------------------------|---------------------------|
| `MC-ARCH-001`    | `Applicable`     | `PASS` | §4.7、§5、Steps 1-7                                    | 单 flat Starter且无 DDD/COLA业务层                 | None                      |
| `MC-REUSE-001`   | `Applicable`     | `PASS` | capability ledger、ADK/Common/Boot路径                  | 复用官方 adapter/runner/session与 ValidationUtils | None                      |
| `MC-DEP-001`     | `Applicable`     | `PASS` | Step 1 POM与 Gate 2/9                                 | 版本集中，dev显式排除，无无关 starter                     | None                      |
| `MC-NAME-001`    | `Applicable`     | `PASS` | §5 type inventory、Step 1 static scan                 | Command/Result/DTO/BO/Enum/Strategy语义完整      | None                      |
| `MC-VALID-001`   | `Applicable`     | `PASS` | Steps 2/5、TEST-002至010                               | 字段/groups/图/边界均 fail closed                  | None                      |
| `MC-MODEL-001`   | `Applicable`     | `PASS` | config/API records与 RuntimeBO计划                      | immutable records；lifecycle holder完整构造       | None                      |
| `MC-CONVERT-001` | `Not applicable` | `N/A`  | official SpringAI adapter + direct small projections | 无跨层业务 mapping，不应引入 MapStruct/BaseConverter   | Evidence retained         |
| `MC-LOG-001`     | `Applicable`     | `PASS` | REQ-013、Steps 4/5/7                                  | component日志只含 allowlist                      | None                      |
| `MC-BEAN-001`    | `Applicable`     | `PASS` | Spec Bean表、Step 6、lombok.config                      | 显式名/Qualifier/constructor/override完整         | None                      |
| `MC-UTIL-001`    | `Applicable`     | `PASS` | capability ledger、POM denylist                       | JDK/ADK/RxJava/Common足够，无新utility            | None                      |
| `MC-JSON-001`    | `Not applicable` | `N/A`  | Spec §9仅 internal Java API                           | 无外部 JSON/serializer；Event原样返回                | Evidence retained         |
| `MC-TIME-001`    | `Applicable`     | `PASS` | Commands/Result/Properties/Clock计划                   | 仅 Instant/Duration/Clock                     | None                      |
| `MC-CONFIG-001`  | `Applicable`     | `PASS` | Steps 2/6/7、TEST-001/002                             | single strict prefix、default off、无 profiles  | None                      |
| `MC-PATTERN-001` | `Applicable`     | `PASS` | §4.5、Step 3                                          | 仅三类 workflow使用 Strategy+Factory，其他直接         | None                      |
| `MC-SCOPE-001`   | `Applicable`     | `PASS` | §5、所有 Commit paths                                   | 只改 Components目标路径，保护 dirty work              | None                      |
| `MC-TEST-001`    | `Applicable`     | `PASS` | §8 Gates 1-11、TEST-001至027映射                         | 每项标准/需求均有 future executable proof            | None                      |
| `MC-BLOCKER-001` | `Applicable`     | `PASS` | §11与以上全部 rows                                        | 用户决策已关闭，无未解决重大 blocker                       | None                      |

### 12.6 Final verdict

PASS — Ready for user review
