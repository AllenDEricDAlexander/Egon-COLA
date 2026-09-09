# Agent Deep Research Archetype 实施计划

| Field              | Value                                                                                                                                                                                                                                                                                           |
|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Document           | `2026-09-04-19-02-agent-deep-research-archetype-implementation.md`                                                                                                                                                                                                                              |
| Template Version   | `4`                                                                                                                                                                                                                                                                                             |
| Status             | `Review`                                                                                                                                                                                                                                                                                        |
| Created            | `2026-09-04 19:02 CST`                                                                                                                                                                                                                                                                          |
| Updated            | `2026-09-04 19:02 CST`                                                                                                                                                                                                                                                                          |
| Owner              | `Egon-COLA maintainer`                                                                                                                                                                                                                                                                          |
| Repository         | `Egon-COLA`                                                                                                                                                                                                                                                                                     |
| Scope              | `egon-cola-archetypes` 新增 Web non-open 六模块 `egon-cola-source-agent`、Agent definition/生成验证，并把两阶段发布合同从六个产品扩成七个；业务只实现 Deep Research                                                                                                                                                                |
| Source Requirement | Agent Flow脚手架完成后，在 archetypes 下新增采用 `egon-cola-archetype-web` 架构方式的 Agent Archetype Project，业务仅 Deep Research；用户确认推荐决策并要求两个阶段分别写 Plan                                                                                                                                                           |
| Baseline Revision  | `main@cd83ae3a6a8b60ab3bbbb4b76f6fad4b87f07a0d`；保留现有 dirty Spec/Plan/tsbuildinfo，禁止修改现有六个 family及任何 Flyway文件                                                                                                                                                                                    |
| Implements Spec    | [Agent Deep Research Archetype 设计](../spec/2026-09-04-16-32-agent-deep-research-archetype.md)                                                                                                                                                                                                   |
| Spec Status        | `Accepted`                                                                                                                                                                                                                                                                                      |
| Spec Revision      | `Updated 2026-09-04 19:02 CST`，基于 `main@cd83ae3a6a8b60ab3bbbb4b76f6fad4b87f07a0d` 的当前工作区决策同步版本                                                                                                                                                                                                  |
| Effective Specs    | [Agent Deep Research Archetype 设计](../spec/2026-09-04-16-32-agent-deep-research-archetype.md); [Agent Flow 扁平化组件设计](../spec/2026-09-04-09-34-agent-flow-component.md); [Archetype 两阶段生成、Spring 依赖治理与 Flyway 收敛设计](../spec/2026-09-03-11-04-archetype-generated-reactor-spring-flyway-design.md) |
| Depends On Plans   | [Agent Flow 扁平化组件实施计划](2026-09-04-19-02-agent-flow-component-implementation.md)                                                                                                                                                                                                                 |
| Supersedes         | `None`                                                                                                                                                                                                                                                                                          |
| Superseded By      | `None`                                                                                                                                                                                                                                                                                          |
| Related Plans      | `None`                                                                                                                                                                                                                                                                                          |

## 1. Summary

本 Plan 仅在前置 Agent Flow Plan完成并验证后执行，用八个顺序 Step交付第七个公开 Archetype family。先建立 Web non-open六模块
source reactor与架构/禁用面合同，再完成 Domain/Application用例、Infrastructure Agent Flow/MCP适配、Starter固定
Flow与离线配置，随后实现唯一 POST SSE Adapter/OpenAPI。source `clean verify`通过后才新增 definition和派生 `.generated`，最后把
release/static policy从六制品扩为七制品。

Deep Research固定为 `Planner -> Parallel(Evidence, Counterpoint, Freshness) -> Writer`；请求不能选择模型、工具、prompt或
flow。V1只保留 JVM临时 run/session/permit，不新增数据库、Flyway、缓存、消息、GraphQL、RPC、Facade模块或 UI。测试只使用 fake
ChatModel、fake ToolCallbacks、MockMvc、ApplicationContextRunner和脚本 fixture，不启动真实服务、模型、MCP、Docker、数据库或浏览器，也不执行真实
Maven Central发布。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [Agent Deep Research Archetype 设计](../spec/2026-09-04-16-32-agent-deep-research-archetype.md)
- Status: Accepted
- Revision: 2026-09-04 19:02 CST；repository baseline `main@cd83ae3a6a8b60ab3bbbb4b76f6fad4b87f07a0d`。
- Approval evidence: 用户要求新增 Agent Archetype且只实现 Deep Research，指定采用 `egon-cola-archetype-web`
  架构方式，并以“全部按照推荐”“确认，开始写plan吧”接受独立 family、六模块、单 POST SSE、API Key、MCP SSE search、进程内容量与无数据库边界。

### 2.2 Effective Spec set

| Role                 | Spec/link                                                                                       | Status/revision                        | Effective sections                                                                 | Why included                                         |
|----------------------|-------------------------------------------------------------------------------------------------|----------------------------------------|------------------------------------------------------------------------------------|------------------------------------------------------|
| Primary              | [Agent Deep Research Archetype 设计](../spec/2026-09-04-16-32-agent-deep-research-archetype.md)   | Accepted, Updated 2026-09-04 19:02 CST | 全文 §1-§20                                                                          | Agent family、业务/API/测试的唯一目标                          |
| Normative dependency | [Agent Flow 扁平化组件设计](../spec/2026-09-04-09-34-agent-flow-component.md)                          | Accepted, Updated 2026-09-04 19:02 CST | §1, §7-§10, §14-§16                                                                | 提供 component Java API、版本、会话/取消合同；本 Plan只消费不复制        |
| Amended base         | [Archetype 两阶段设计](../spec/2026-09-03-11-04-archetype-generated-reactor-spring-flyway-design.md) | Accepted                               | primary Spec `Amends`列出的 §1、§3.1、§4 REQ-001至011/020、§7-§9、§14-§16、§18-§20；其余作为不变约束 | source/definition/.generated、发布前 Gate和既有 Flyway不可变规则 |

### 2.3 Superseded or excluded content

本 Plan不修改 `egon-cola-source-web` 或其 definition，不复制 user/teaching/DB/MQ/GraphQL/Dubbo能力。旧六 family与 24个
Flyway路径是保护对象。`.generated/egon-cola-archetype-agent` 是脚本输出，不手工编辑、不提交。前置 Component Plan未完成时，所有
Step均不得开始。

## 3. Effective Requirements and Acceptance

Primary Spec 的 REQ ID 与两个 normative Specs 存在相同编号；下表以 `Agent`限定 primary语义。Steps中的裸 `REQ-NNN` 用于
validator trace，文字必须结合 source qualifier解释；依赖 Specs只作为前置/保留 Gate，不在本 Plan重复实现。

| Requirement       | Source Spec section | Effective statement                  | Observable acceptance                         | Implementation impact        |
|-------------------|---------------------|--------------------------------------|-----------------------------------------------|------------------------------|
| `Agent REQ-001`   | Primary §4          | 新增独立 Agent family                    | source/definition/generated identity独立，Web无变化 | Steps 1, 7, 8                |
| `Agent REQ-002`   | Primary §4          | 精确 Web non-open六模块                   | exact order/edges，无 facade                    | Steps 1, 6, 7                |
| `Agent REQ-003`   | Primary §4          | 业务仅 `research`                       | 无 user/teaching/CRUD/第二业务                     | Steps 1-7                    |
| `Agent REQ-004`   | Primary §4          | 只消费 Agent Flow + 1.1.8/0.7.0         | Infrastructure无 Runner/compiler复制             | Steps 1, 3, 4, 6             |
| `Agent REQ-005`   | Primary §4          | 固定 planner/parallel/writer           | exact names/output keys/order                 | Step 4                       |
| `Agent REQ-006`   | Primary §4          | 一个 MCP SSE search工具源                 | dev/prod fail closed，test fake                | Steps 3, 4                   |
| `Agent REQ-007`   | Primary §4          | 唯一 POST SSE Command                  | 无 session/status/history endpoint             | Step 5                       |
| `Agent REQ-008`   | Primary §4          | 校验 body/media/API key                | 400/401/406/415且零模型调用                         | Step 5                       |
| `Agent REQ-009`   | Primary §4          | 稳定 SSE framing/终态                    | id递增，唯一 completed/failed                      | Steps 2, 5                   |
| `Agent REQ-010`   | Primary §4          | 每 run独立 Session且全终态清理                | session/subscription/permit无泄漏                | Steps 2, 3, 5                |
| `Agent REQ-011`   | Primary §4          | 本地并发/总时长受限                           | 第五请求429，默认5分钟                                 | Steps 2, 5                   |
| `Agent REQ-012`   | Primary §4          | secret不进请求/源码/log/SSE/OpenAPI        | secret scan/log test为零                        | Steps 3-6                    |
| `Agent REQ-013`   | Primary §4          | 无 durable state/恢复承诺                 | 无 persistence依赖与接口                            | Steps 1, 6, 7                |
| `Agent REQ-014`   | Primary §4          | code-first OpenAPI匹配 runtime         | `/v3/api-docs`完整断言                            | Steps 5, 6                   |
| `Agent REQ-015`   | Primary §4          | source/definition/generated独立 verify | 无 sentinel，basic IT通过                         | Steps 6, 7                   |
| `Agent REQ-016`   | Primary §4          | 产品集合安全从六到七                           | generate/check/profile/release识别七个            | Steps 7, 8                   |
| `Agent REQ-017`   | Primary §4          | 旧六和全部 Flyway不变                       | path/hash/diff gate                           | Steps 7, 8                   |
| `Agent REQ-018`   | Primary §4          | 所有测试离线确定性                            | 无凭据/外部服务                                      | Every Step                   |
| `Reactor REQ-019` | Base §4             | Open family手工 SQL不纳入 Flyway          | 本 Plan零 migration变更                           | Steps 7, 8 preservation gate |
| `Reactor REQ-020` | Base §4             | 任一发布 Gate失败禁止 deploy                 | dry-run fail-before-deploy                    | Step 8                       |

Component Spec REQ-001至015通过 `Depends On Plans`完成证据消费：版本/API/session/event/取消/默认关闭/无外部系统等合同由
Step 1前置检查和 Steps 3-4 integration test验证，不在 Archetype内复制其实现。

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

1. 前置 Plan必须 Completed且 focused/dependency gates通过；随后创建 internal source root、六 module POM和架构/forbidden
   tests。
2. Domain先定义纯业务 records/enums/ports，Application用 Facade(Manage)+Observer+Bulkhead编排一次 run，不出现
   ADK/RxJava/HTTP。
3. Infrastructure Adapter实现 Domain Yuheng，消费 AgentFlowService并以四个 static MapStruct/BaseConverter隔离第三方/边界模型。
4. Starter/Infrastructure创建一个 MCP tool集合、一个具名 ChatModel与固定 flow配置；test profile全部 fake且 key parity。
5. Adapter最后接唯一 POST SSE、API key/trace/filter/error mapping，复用 application observer，不把 `SseEmitter`向内泄漏。
6. source project完成 OpenAPI、架构、dependency、secret/profile和 `clean verify`后冻结双语文档。
7. source稳定后新增独立 definition/basic IT，通过既有 generator生成 ignored Agent child并执行 generated verify。
8. 最后先让 release tests对旧“6”合同 RED，再把 policy/release scripts扩为七；只运行 `archetypes --dry-run`。

### 4.2 Test-first strategy

| Behavior              | RED point before implementation          | Minimum GREEN                                    | Refactor/wiring allowed                     |
|-----------------------|------------------------------------------|--------------------------------------------------|---------------------------------------------|
| 六模块/禁用面               | POM/static architecture tests对缺 source失败 | exact edges、研究单域、零 DB/GraphQL/RPC                | 只建 module skeleton                          |
| Domain/Application    | model/manage/capacity tests先失败           | normalized task、终态CAS、permit exactly once        | Facade+Observer+Bulkhead，不建 state hierarchy |
| Agent Flow yuheng    | yuheng/converter tests先失败               | one session/run、safe event allowlist、cleanup     | Adapter pattern only                        |
| MCP/model/fixed flow  | context/flow/profile tests先失败            | named Beans、nonempty tools、exact graph、fake test | Starter只装配，Infrastructure tool factory      |
| HTTP/SSE              | MockMvc/filter/converter tests先失败        | exact status/header/framing/terminal/cancel      | one Controller + filters/advice             |
| OpenAPI/source verify | OAS/architecture/static gates先失败         | API-GATE + clean verify                          | 文档只在行为稳定后写                                  |
| Archetype             | basic verifier先要求 Agent family           | six generated modules and reports                | reuse generic generator unchanged           |
| Seven release         | fixture先期待7而 script仍6                    | exact seven artifact shape, fail before deploy   | no new release path                         |

### 4.3 Sequential and parallel boundaries

| Step   | Depends on              | May run in parallel with | Must not overlap with              | Reason                                        |
|--------|-------------------------|--------------------------|------------------------------------|-----------------------------------------------|
| Step 1 | Component Plan complete | None                     | source root/POM/architecture tests | dependency topology baseline                  |
| Step 2 | Step 1                  | None                     | domain/application/model tests     | domain ports before adapter                   |
| Step 3 | Step 2                  | None                     | infrastructure yuheng/converters  | consumes stable ports/models                  |
| Step 4 | Step 3                  | None                     | tool/starter/config/flow tests     | model/tools must feed component config        |
| Step 5 | Steps 2-4               | None                     | adapter API/tests                  | HTTP maps stable application contract         |
| Step 6 | Steps 1-5               | None                     | OpenAPI/docs/full source verify    | freezes source behavior                       |
| Step 7 | Step 6                  | None                     | definition/generated               | generation only from verified source          |
| Step 8 | Step 7                  | None                     | release policy/scripts             | count changes after seventh artifact verifies |

### 4.4 Commit boundaries

八个 Step各一个语义提交。`.generated`只用于验证且永不 stage。Step 8只修改三个现有 scripts，不修改 workflows；现有
generator动态发现 definitions，除非 Step 7证明 generic bug，否则不得修改。所有 `git add --`限定当前 Step精确路径，旧六
definitions/source/Flyway和当前 dirty docs/tsbuildinfo保持未暂存。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element                | Spec necessity verdict/section | Current repository evidence              | Direct/reuse alternative | Interaction/implementation cost | Plan decision       |
|-----------------------------|--------------------------------|------------------------------------------|--------------------------|---------------------------------|---------------------|
| 独立 Agent family             | Primary §5 DEC-001             | definitions按 family动态发现                  | 修改 Web业务                 | 会污染现有用户                         | Implement Steps 1/7 |
| Web六模块                      | Primary DEC-002                | source-web exact modules/bytecode plugin | flat Starter或第七facade    | 偏离用户指定                          | Implement Step 1    |
| Domain Yuheng Adapter      | Primary §13                    | Web Infrastructure实现 Domain ports        | Manage直接调用 AgentFlow     | 泄漏第三方依赖                         | Implement Step 3    |
| Observer                    | Primary §13                    | SSE需跨层 progress/cancel                   | 返回 List或SseEmitter下沉     | 阻塞/层泄漏                          | Implement Steps 2/5 |
| Manage Facade + Bulkhead    | Primary §13                    | Web profile `*Manage`惯例                  | Controller直接拼装/semaphore | 重复业务控制                          | Implement Step 2    |
| provider Strategy           | Primary §13 rejected           | V1只有一个 model/MCP                         | 多 provider factory       | 无变化点                            | Do not implement    |
| Repository/state API        | Primary §§9/11 rejected        | no DB/durable state                      | status/history/resume    | 假承诺恢复                           | Do not implement    |
| new generator/release entry | Base Spec existing pipeline    | generic generator/release wrapper已存在     | Agent专用脚本                | 双发布路径                           | Reuse Steps 7/8     |

### 4.6 Change-unit Dependency Matrix

| Change unit            | Requirements                                | Proof/RED point           | Compile/runtime prerequisites | Produces                       | Consumers/unblocks     | Owning Step |
|------------------------|---------------------------------------------|---------------------------|-------------------------------|--------------------------------|------------------------|-------------|
| source topology        | REQ-001-REQ-004, REQ-013, REQ-018           | POM/architecture RED      | Component Plan complete       | six modules                    | all source code        | Step 1      |
| domain/application run | REQ-003, REQ-007, REQ-009-REQ-011, REQ-018  | model/manage tests        | Step 1                        | ports/Facade/Observer/Bulkhead | Infrastructure/Adapter | Step 2      |
| Agent Flow adapter     | REQ-004, REQ-006, REQ-009, REQ-010, REQ-012 | yuheng/converter RED     | Step 2 + component            | domain-safe event run          | Starter/Adapter        | Step 3      |
| host AI/config         | REQ-004-REQ-006, REQ-011, REQ-012, REQ-018  | context/flow/profile RED  | Step 3                        | model/tools/fixed flow         | HTTP use case          | Step 4      |
| SSE API                | REQ-007-REQ-012, REQ-014, REQ-018           | MockMvc/serialization RED | Steps 2-4                     | API-001                        | OAS/source verify      | Step 5      |
| source release quality | REQ-002-REQ-005, REQ-012-REQ-015, REQ-018   | OAS/static/docs RED       | Steps 1-5                     | verified source                | definition             | Step 6      |
| definition/generated   | REQ-001-REQ-003, REQ-015-REQ-019            | verifier RED              | Step 6                        | seventh generated artifact     | release shape          | Step 7      |
| seven-artifact release | REQ-016-REQ-020                             | release fixture RED       | Step 7                        | guarded dry-run                | release operator       | Step 8      |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern             | Current repository evidence                           | Effective Spec decision                               | Planned implementation consequence           | Owning Steps/checks                              |
|---------------------|-------------------------------------------------------|-------------------------------------------------------|----------------------------------------------|--------------------------------------------------|
| Architecture        | source-web六模块与 architecture plugin                    | exact Web non-open profile，业务仅 research               | domain-first edges、Adapter不依赖 Infrastructure | Steps 1/6/7; MC-ARCH-001                         |
| Validation/model    | ValidationUtils、Java 21 records                       | Request/Command/BO/Event/VO/Properties明确 null/default | HTTP + application/domain复验                  | Steps 2/5; MC-VALID-001, MC-MODEL-001            |
| Conversion          | common-core `BaseConverter` + MapStruct               | 四个真实边界 static mappers                                 | both directions，ERROR unmapped policy        | Steps 3/5; MC-CONVERT-001                        |
| Beans/logging       | Web具名 beans/constructor injection/Slf4j               | exact names、Qualifier、safe runId/trace/outcome logs   | no field injection/content/secrets           | Steps 2-5; MC-BEAN-001, MC-LOG-001               |
| Utilities/JSON/time | JDK/Spring/Jackson/MapStruct                          | no Utils/Fastjson；NON_NULL + UTC Instant/Duration     | no serializer hacks/legacy time              | Steps 2-6; MC-UTIL-001, MC-JSON-001, MC-TIME-001 |
| Config              | application dev/test/prod conventions                 | identical keys；secrets external；test fake             | typed properties/fail closed                 | Step 4; MC-CONFIG-001                            |
| Patterns            | Primary §13 selected Adapter/Observer/Facade/Bulkhead | each pattern owns one real complexity                 | no State/Strategy/Repository hierarchy       | Steps 2/3/5; MC-PATTERN-001                      |

#### Capability reuse ledger

| Need                | Candidates inspected                           | Exact evidence                      | Fit/gap                                  | Decision                 | Added dependency/custom code | Owning Step/check       |
|---------------------|------------------------------------------------|-------------------------------------|------------------------------------------|--------------------------|------------------------------|-------------------------|
| Flow/session/events | Agent Flow Component                           | accepted API and prerequisite Plan  | exact; implementation pending            | hard prerequisite/reuse  | one Egon Starter dep         | Steps 1/3; MC-REUSE-001 |
| SSE/validation      | Spring MVC + Jakarta                           | source-web adapter patterns         | SseEmitter lifecycle needs custom bridge | reuse MVC                | web/validation existing      | Step 5                  |
| OpenAPI             | springdoc BOM 2.8.17                           | source-web OpenAPI tests/config     | needs new API schema                     | reuse code-first         | existing starter             | Steps 5/6               |
| model               | Spring AI OpenAI-compatible ChatModel          | primary Spec decision               | host-owned Bean required                 | Starter config           | Spring AI OpenAI library     | Step 4                  |
| search tools        | Spring AI MCP support                          | primary Spec one SSE MCP source     | needs typed factory/lifecycle            | Infrastructure adapter   | one MCP client dependency    | Steps 3/4               |
| mapping             | MapStruct + BaseConverter                      | common-core and Web converter style | exact four boundaries                    | static mapper interfaces | existing processor           | Steps 3/5               |
| generation          | `generate_archetypes.sh` definitions discovery | current manifest pipeline           | no Agent definition yet                  | add definition only      | none                         | Step 7                  |
| publishing          | `maven-deploy.sh` + tests                      | current count hardcoded six         | count/policy list gap                    | modify existing scripts  | none                         | Step 8                  |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source         | Repository evidence                        | Exact files and order                          | Pseudocode obligations                             | Validation gate              | Steps       | Status/blocker |
|--------------|---------------------|--------------------------------------------|------------------------------------------------|----------------------------------------------------|------------------------------|-------------|----------------|
| Rule 1       | Primary §6.2/§10    | Web Request/Command/VO/Manage naming       | models before behaviors                        | exact semantic suffixes，无 Data/Info/Param/Bean     | source/verifier scan         | Steps 2-7   | PASS           |
| Rule 2       | Primary §6.2/§10    | Jakarta + ValidationUtils                  | Request -> Command -> BO                       | HTTP and inner boundary validation，normalize once  | tests 002/015                | Steps 2/5   | PASS           |
| Rule 3       | Primary §6.2/§10.4  | BaseConverter + MapStruct                  | source/target models then four converters      | static INSTANCE, exact generic, both directions    | mapping compile/tests        | Steps 3/5   | PASS           |
| Rule 4       | Primary §6.2/§8     | lombok.config/constructor pattern          | config first, concrete beans, wiring           | Slf4j/final/RequiredArgsConstructor/Qualifier/name | context/source scan          | Steps 1-5   | PASS           |
| Rule 5       | Primary §6.2/§6.1   | reuse ledger                               | POM contracts before code                      | JDK/Spring/Egon/approved AI only，无 Utils           | dependency/import gate       | Steps 1-8   | PASS           |
| Rule 6       | Primary §6.2/§9     | code-first JSON/SSE                        | Request/Event/Error then Controller/OAS        | Jackson-only, explicit null/enum/Instant/NON_NULL  | JSON/OAS tests               | Steps 5/6   | PASS           |
| Rule 7       | Primary §6.2/§8/§15 | profile files convention                   | properties then all profiles                   | identical key set，test fake，no literal secret/URL  | profile parity               | Steps 4/6   | PASS           |
| Rule 9       | Primary §6.2/§13    | long run + third-party boundary + capacity | Observer/Facade/Bulkhead before Adapter bridge | no speculative provider/state pattern              | lifecycle tests              | Steps 2/3/5 | PASS           |
| Rule 10      | Primary §6.2/§10    | UTC run/event/deadline                     | models/properties/converters                   | Instant/Duration/Clock only                        | serialization/import scan    | Steps 2-6   | PASS           |
| Rule 11      | Primary §6.1        | user chose Web non-open profile            | six module POMs then exact packages            | no flat/Traditional混用；no facade seventh module     | architecture/plugin/verifier | Every Step  | PASS           |

## 5. Change File Tree

```text
egon-cola-archetypes/source-projects/
├── pom.xml                                                   MODIFY
└── egon-cola-source-agent/                                   CREATE
    ├── pom.xml, README.md, README.zh-CN.md, lombok.config
    ├── egon-cola-source-agent-common/                        error enum + package docs
    ├── egon-cola-source-agent-domain/                        research models/ports/services + tests
    ├── egon-cola-source-agent-application/                   command/properties/manage/capacity + tests
    ├── egon-cola-source-agent-infrastructure/                AgentFlow yuheng/converter/MCP + tests
    ├── egon-cola-source-agent-adapter/                       filters/handler/SSE API/converters + tests
    └── egon-cola-source-agent-starter/                       application/config/profiles/flow + tests
egon-cola-archetypes/definitions/egon-cola-archetype-agent/   CREATE manifest/metadata/docs/basic IT
egon-cola-archetypes/.generated/egon-cola-archetype-agent/    GENERATED, ignored, never committed
scripts/maven-deploy.sh                                       MODIFY
scripts/test-archetype-release.sh                             MODIFY
scripts/test-spring-dependency-management.sh                  MODIFY
```

## 6. Prerequisites, Constraints, and Plan Clarifications

- `PRE-001`: [Agent Flow Plan](2026-09-04-19-02-agent-flow-component-implementation.md) 必须 Completed，七个 commits与
  focused/dependency gates均通过；只接受真实 public API，不允许本 Plan创建临时复制品。
- `PRE-002`: 执行前记录 source/definitions/`.generated`/Flyway inventory和旧六 hashes；`.generated`必须继续被 Git ignore且
  tracked count为零。
- `PRE-003`: 禁止启动业务服务、真实模型/MCP、数据库、Docker、浏览器或 Central publish；仅允许 Maven test context与
  `archetypes --dry-run`。
- `PLAN-CLAR-001`: source tree的每个有 Java文件的 main/test package均创建与 Web profile相同风格的 `package-info.java`；这是
  architecture/verifier文档约束，不新增行为或层。
- `PLAN-CLAR-002`: Primary target tree只列出 Starter四个核心测试名，但 §14明确要求
  Domain/Application/Infrastructure/Adapter tests；本 Plan新增语义测试类并由 basic verifier读取 surefire reports。
- `PLAN-CLAR-003`: 四个 MapStruct converter均为非 Spring static `INSTANCE`并实现 exact `BaseConverter<S,T>`；它们不占
  Bean name，具体行为 Bean仍遵循显式命名/Qualifier。
- `PLAN-CLAR-004`: MCP transport的具体 Spring AI client builder签名必须在 Step 3从锁定依赖源码确认；若 1.1.8没有同步 SSE
  client/ToolCallback能力，返回 primary Spec REQ-006，不换协议或新增自研 MCP client。
- `PLAN-CLAR-005`: `scripts/generate_archetypes.sh`预计无需修改，因为现状按 definitions动态发现；只有新增 Agent
  definition暴露通用缺陷且既有六 fixture证明修复无回归时，才返回 Spec/Plan补充，不在执行中顺手改。
- `PLAN-CLAR-006`: Base Spec同编号 REQ与 primary REQ语义不同；本 Plan以 source-qualified trace为准，Base REQ-019/020显式落在
  Steps 7/8的 preservation/publish Gate。

## 7. Ordered File-by-file Implementation Steps

所有 Step在前置 Plan完成后顺序执行；source先于 definition，definition先于 release count。每 Step先建立 RED、完成
GREEN和验证，再只提交声明路径。

### Step 1 — 建立 Web 六模块 Agent Source 与架构合同

- Requirements: REQ-001, REQ-002, REQ-003, REQ-004, REQ-013, REQ-018
- Dependencies: Agent Flow 扁平化组件实施计划 Completed；PRE-001
- Baseline state: source-projects reactor只有六个既有 source family，没有 `egon-cola-source-agent`目录或内部 GAV。
- Observable outcome: 新 source root精确包含 common/domain/application/infrastructure/adapter/starter，依赖方向匹配 Web
  non-open且只声明 Deep Research必要 dependencies。
- End state: module/POM/architecture基线可 verify；业务 classes尚未创建，后续只能落在 `research`包。
- Test-first gate: Required — 允许先建最小 Maven shell作为 Java test编译前置；`AgentArchitectureTest`随后对空依赖边/缺
  plugin约束形成 RED，再补齐六 child POM到 GREEN。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 4, Rule 5, Rule 11
- Ordered files:

#### File 1 —
`CREATE egon-cola-archetypes/source-projects/egon-cola-source-agent/{pom.xml,lombok.config,README.md,README.zh-CN.md}`

- Purpose: 建立 internal source root、六 module declaration、统一版本/BOM/plugin properties与初始范围文档。
- Symbols: GAV `top.egon.internal.archetype.source:egon-cola-source-agent:0.1.0-SNAPSHOT`; modules；Boot Parent
  3.5.16；Springdoc 2.8.17、Spring AI 1.1.8、Egon 5.3.3、Lombok 1.18.46、lombok-mapstruct-binding 0.2.0、MapStruct Plus 1.5.1
  properties。
- Repository evidence: source-web root直接继承 Boot Parent并列六模块；现有 source projects使用相同 internal
  group/version与 Lombok配置。
- Dependencies and consumers: 六 child POM继承；definition将此 root作为唯一业务 source；source-projects parent在 File 4登记。
- Why now: Java/test路径依赖 parent坐标与 modules，先建最小 root是编译前置。
- Contract/signature changes: 新 internal非发布 source GAV；README只说明 Deep Research、六层和 no durable state，不宣称实现完成。
- Input/output and state mapping: root properties/BOM流向 child；consumer artifactId/package由 definition阶段替换。
- Error and edge behavior: 不重复 import Boot BOM；无 provider endpoint/key默认值；source GAV不得进入 Central artifact
  allowlist。
- Standards impact: MC-ARCH-001、MC-DEP-001、MC-CONFIG-001、MC-SCOPE-001。
- Literal rule enforcement: Rule 4复制 Qualifier/Value；Rule 5只声明 approved BOM；Rule 11 exact six modules，无
  facade/flat混合。
- Implementation pseudocode:

```xml
inherit spring-boot-starter-parent 3.5.16 with empty relativePath and declare Java 21 plus internal source GAV
list common, domain, application, infrastructure, adapter and starter modules in that order
import Egon Components BOM 5.3.3, Spring AI BOM 1.1.8 and Springdoc BOM 2.8.17 without duplicating Boot dependency management
manage all six internal child artifacts at project.version plus Lombok 1.18.46 and MapStruct Plus 1.5.1 using the current Web source conventions
configure compiler annotation processors for Lombok, lombok-mapstruct-binding 0.2.0 and mapstruct-plus-processor 1.5.1 and mark the root as internal/non-Central source material
```

- Verification contribution: 提供 effective POM和 module reactor基础；business verify留后续 Step。
- After this file: root存在但 child POM/test缺失，reactor仍 RED。

#### File 2 —
`CREATE egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-{common,domain,application,infrastructure,adapter,starter}/pom.xml`

- Purpose: 创建六个可解析 child shell并声明精确依赖边。
- Symbols: six artifactIds；`domain -> common`; `application -> domain`;
  `infrastructure -> domain + agent-flow + spring-ai-mcp`;
  `adapter -> application + web/validation/springdoc/mapstruct`;
  `starter -> adapter + infrastructure + spring-ai-openai`。
- Repository evidence: source-web POMs使用相同 parent/模块边；primary Spec §8.3删除 Web的
  Facade/DB/MQ/GraphQL/Dubbo/Actuator/Cloud能力。
- Dependencies and consumers: module编译图；Starter architecture plugin在 verify检查 reactor；definition转换所有 POM。
- Why now: `AgentArchitectureTest`必须在真实 module classpath上运行，先创建最小可编译子模块。
- Contract/signature changes: 新六 internal artifacts；仅 Infrastructure消费 Agent Flow/MCP，只有 Starter消费 provider
  model。
- Input/output and state mapping: parent-managed internal artifact versions流入 child edges；无 runtime状态。
- Error and edge behavior: Adapter不得依赖 Infrastructure；Domain不得依赖 Spring/ADK/RxJava/HTTP；不存在
  JDBC/Flyway/Redis/GraphQL/RPC/MQ。
- Standards impact: MC-ARCH-001、MC-REUSE-001、MC-DEP-001、MC-SCOPE-001。
- Literal rule enforcement: Rule 5 使用批准 dependencies；Rule 11 落实单一 Web profile和 domain-first方向。
- Implementation pseudocode:

```xml
make common dependency-free except shared Egon core only when ResearchErrorCodeEnum contract requires it
make domain depend on common, application on domain, infrastructure on domain plus agent-flow Starter and Spring AI MCP client support
make adapter depend on application, spring-boot-starter-web, validation, springdoc webmvc api and MapStruct support without infrastructure
make starter depend on adapter and infrastructure plus Spring AI OpenAI model and test support; configure Boot plugin and Egon architecture reactor check
```

- Verification contribution: module graph可被 Java architecture test和 Maven dependency gate解析。
- After this file: reactor可编译空模块；architecture test尚未存在。

#### File 3 —
`CREATE egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/test/java/top/egon/cola/archetype/source/agent/architecture/{AgentArchitectureTest.java,package-info.java}`

- Purpose: 将 exact module order/edges、domain-first imports与禁止第七模块变成离线架构合同。
- Symbols: tests `uses_exact_web_non_open_modules`、`preserves_domain_first_dependencies`、
  `keeps_agent_technology_in_infrastructure_and_starter`、`has_no_facade_or_forbidden_integrations`。
- Repository evidence: source-web有 `WebArchitectureTest`和 starter architecture Maven plugin；definition
  verifier会再次验证生成结果。
- Dependencies and consumers: 扫描 sibling POM/source；Step 6 source contract补业务/命名/package-info检查，但不重复 module
  edge。
- Why now: 在生产 Java前锁住用户指定架构，避免后续为便利让 Adapter依赖 Infrastructure。
- Contract/signature changes: 无生产合同；测试错误打印具体 module/edge/import。
- Input/output and state mapping: reactor POM/source imports -> graph/allowlist assertions；不写文件。
- Error and edge behavior: module缺失/多余、错误依赖、Domain技术 import、禁止 coordinate全部 fail closed。
- Standards impact: MC-ARCH-001、MC-DEP-001、MC-SCOPE-001、MC-TEST-001。
- Literal rule enforcement: Rule 1检查后续 type后缀入口；Rule 5 dependency allowlist；Rule 11 exact Web profile，不接受
  hybrid。
- Implementation pseudocode:

```java
read the source root module list and assert exactly six modules in common/domain/application/infrastructure/adapter/starter order
parse each child POM into direct internal edges and assert domain-first graph, including no adapter-to-infrastructure edge and no facade child
scan current source imports so Domain rejects Spring, ADK, RxJava, HTTP and MCP while Application rejects component/ADK/MCP
reject JDBC, JPA, MyBatis, Flyway, Redis, MQ, Dubbo, GraphQL and source-web business coordinates across the new source root
```

- Verification contribution: TEST-018架构部分与 TEST-019 dependency禁用面。
- After this file: architecture test对正确 POM shell GREEN，并成为所有后续 Step回归 gate。

#### File 4 — `MODIFY egon-cola-archetypes/source-projects/pom.xml`

- Purpose: 将新 internal source root登记到正常 source reactor，位于六个既有 source之后。
- Symbols: modules entry `egon-cola-source-agent`。
- Repository evidence: 当前 parent同时包含 components/xingyuan/facades和六 source roots，使 source能解析仓库 artifacts。
- Dependencies and consumers: 完整 source reactor、release preflight；前置 Agent Flow module通过 included components
  reactor解析。
- Why now: 独立 source root通过结构测试后再进入共享 reactor。
- Contract/signature changes: internal modules从六增至七；public Archetype count仍到 Step 8才改变。
- Input/output and state mapping: reactor module entry -> Maven build order；不改变发布 artifacts。
- Error and edge behavior: 只追加一次，保持 existing module顺序/坐标；若 component artifact未解析则 PRE-001失败。
- Standards impact: MC-ARCH-001、MC-DEP-001、MC-SCOPE-001、MC-TEST-001。
- Literal rule enforcement: Rule 5 复用 current reactor；Rule 11 只增加 exact Web-profile source root。
- Implementation pseudocode:

```xml
append egon-cola-source-agent after the six existing source project modules
retain components, xingyuan and facade prerequisite entries and every existing source module unchanged
run targeted reactor validate and assert the new internal source artifact is not part of the public Central artifact inventory
```

- Verification contribution: 新 source成为标准验证输入，同时不改变 definitions/release集合。
- After this file: six-module Agent source skeleton由 normal source reactor管理。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/pom.xml -pl egon-cola-source-agent -am test -Dtest=AgentArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false`
- Expected result: target source与前置 component/prerequisites解析，AgentArchitectureTest通过，未运行服务/外部系统。
- Failure returns to: File 1若 parent/BOM错误；File 2若 module edge/dependency越界；File 3若架构断言错误；File 4若
  reactor顺序/解析错误。
- Completion criteria: Agent REQ-001至004/013/018的结构和 dependency前置闭环；Phase 2代码只能按六层继续。
- Rollback: 删除新 source root并移除 source-projects单一 module entry；旧六 source完全不动。
- Commit paths:
  `egon-cola-archetypes/source-projects/egon-cola-source-agent/{pom.xml,lombok.config,README.md,README.zh-CN.md}`;
  `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-{common,domain,application,infrastructure,adapter,starter}/pom.xml`;
  `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/test/java/top/egon/cola/archetype/source/agent/architecture/{AgentArchitectureTest.java,package-info.java}`;
  `egon-cola-archetypes/source-projects/pom.xml`
- Commit: `build(agent-archetype): add web-profile source reactor`

### Step 2 — 实现纯 Domain 与 Application 研究用例

- Requirements: REQ-003, REQ-007, REQ-009, REQ-010, REQ-011, REQ-012, REQ-013, REQ-018
- Dependencies: Step 1
- Baseline state: 六模块可解析但没有研究词汇、Domain ports、Observer、Manage或容量控制。
- Observable outcome: Domain表达 topic/task/event/run；Application通过 Manage Facade验证命令、获取 fair permit并把
  gateway事件传给 Observer，所有终态 exactly-once释放。
- End state: Domain/Application完全不依赖 ADK/RxJava/HTTP/MCP；Infrastructure可实现稳定 Yuheng。
- Test-first gate: Required — 先创建 Domain/Application tests覆盖 normalization、event invariants、capacity、sync
  failure、observer failure和 terminal/cancel race，再实现模型与行为到 GREEN。
- Manual Checks: MC-ARCH-001, MC-VALID-001, MC-NAME-001, MC-MODEL-001, MC-LOG-001, MC-PATTERN-001, MC-SCOPE-001,
  MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 4, Rule 5, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 —
`CREATE egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-{domain,application}/src/test/java/top/egon/cola/archetype/source/agent/{domain/research/DeepResearchDomainTest.java,application/research/DeepResearchManageImplTest.java,application/research/package-info.java,domain/research/package-info.java}`

- Purpose: 先固定 domain invariants、状态终止、capacity和 Manage清理次序。
- Symbols: topic/language/maxSources tests；event conditional fields；four-permit
  saturation；yuheng/observer/terminal/cancel races。
- Repository evidence: primary TEST-005/009至012；source-web application tests使用 fake ports和 JUnit 5。
- Dependencies and consumers: 纯 Java fakes，Application test实现 Domain Yuheng/Observer；不引用
  AgentFlow/SseEmitter/RxJava。
- Why now: 业务规则必须先于技术 adapter，且 cancellation ownership必须在跨层前定义。
- Contract/signature changes: 无生产合同；synthetic topic/keys，不含真实 secrets。
- Input/output and state mapping: raw command values -> normalized BO；yuheng callbacks -> validated Domain Event ->
  Observer；terminal -> lease release once。
- Error and edge behavior: blank/control/length/range、invalid terminal payload、fifth permit、sync start failure、observer
  exception、cancel/complete race全部断言。
- Standards impact: MC-VALID-001、MC-MODEL-001、MC-LOG-001、MC-PATTERN-001、MC-TEST-001。
- Literal rule enforcement: Rule 1 语义测试名；Rule 2 多层 validation；Rule 9 验证 Facade/Observer/Bulkhead职责；Rule 10 固定
  Clock/Instant/Duration；Rule 11 层内测试。
- Implementation pseudocode:

```java
parameterize topic trimming, control characters, length, language and maxSources to assert one normalized valid task or a typed validation failure
construct STARTED, PROGRESS, COMPLETED and FAILED events and reject every missing, conflicting or late terminal field combination
hold four fair capacity leases, assert the fifth fails without yuheng invocation, then release one and prove the next acquisition succeeds
script yuheng start, observer failure, cancel, complete and error races; assert one terminal winner and exactly one capacity release without technical-type leakage
```

- Verification contribution: TEST-005/009至012及 domain invariant RED/GREEN。
- After this file: tests RED，因为 Domain/Application production types不存在。

#### File 2 —
`CREATE egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-{common,domain}/src/main/java/top/egon/cola/archetype/source/agent/{common/error/ResearchErrorCodeEnum.java,common/package-info.java,domain/research/yuheng/DeepResearchAgentGateway.java,domain/research/model/DeepResearchTaskBO.java,domain/research/model/ResearchTopicBO.java,domain/research/model/DeepResearchEvent.java,domain/research/model/ResearchEventTypeEnum.java,domain/research/model/ResearchStageEnum.java,domain/research/model/ReportLanguageEnum.java,domain/research/service/DeepResearchEventObserverService.java,domain/research/service/DeepResearchRunService.java,domain/research/package-info.java}`

- Purpose: 定义纯研究词汇、Yuheng/Observer/Run ports和稳定错误码。
- Symbols: one error enum；three records + three enums；Yuheng start；Observer callback；Run cancel/identity。
- Repository evidence: primary §8/§9/§10要求 Domain零技术 import；Web profile Domain owns ports/models/services。
- Dependencies and consumers: Application编排，Infrastructure实现 Yuheng，Adapter实现 Observer bridge；只依赖 common/JDK。
- Why now: Tests先定义 invariants，先固化向内合同再实现外层 adapter。
- Contract/signature changes: 新 Domain/Application-facing Java APIs；无 Spring annotation或 reactive type。
- Input/output and state mapping: normalized topic/language/maxSources/trace -> task BO；safe progress/result/error ->
  Domain Event；run handle只暴露 cancel。
- Error and edge behavior: records compact constructors拒绝非法组合；error code/message为 allowlist；late terminal由
  Application owner丢弃。
- Standards impact: MC-NAME-001、MC-VALID-001、MC-MODEL-001、MC-JSON-001、MC-TIME-001。
- Literal rule enforcement: Rule 1用 BO/Event/Enum/Yuheng/Service；Rule 2 compact validation；Rule 3 records无
  Lombok数据组合；Rule 10 Instant；Rule 11纯 Domain。
- Implementation pseudocode:

```java
define bounded ResearchTopicBO normalization and DeepResearchTaskBO with server-owned runId, traceId, deadline, language and maxSources
define event/stage/language enums and one DeepResearchEvent record whose compact constructor enforces type-specific progress, report and safe failure fields
define DeepResearchAgentGateway.start(task, observer) returning DeepResearchRunService and observer callbacks that carry only Domain events
define DeepResearchRunService identity plus idempotent cancel contract and ResearchErrorCodeEnum values matching the approved HTTP/SSE error table
```

- Verification contribution: Domain tests编译并验证纯模型/ports；Application仍 RED。
- After this file: 内层词汇稳定，无 Spring/ADK/RxJava/MCP import。

#### File 3 —
`CREATE egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-application/src/main/java/top/egon/cola/archetype/source/agent/application/research/{command/StartDeepResearchCommand.java,config/DeepResearchRuntimeProperties.java,manage/DeepResearchManage.java,manage/impl/DeepResearchManageImpl.java,service/ResearchCapacityService.java,exception/DeepResearchApplicationException.java,package-info.java}`

- Purpose: 以 Manage Facade组合 validation、fair Bulkhead、run/terminal CAS和清理所有权。
- Symbols: `DeepResearchManage#startResearch`; command/properties；capacity acquire/lease；application
  exception；ManageImpl。
- Repository evidence: source-web使用 `*Manage`/Impl边界；primary INTERNAL-001明确一个 atomic use case和默认4/范围1-32。
- Dependencies and consumers: Adapter调用 Manage；Manage只依赖 Domain Yuheng/Observer和 ValidationUtils/Clock；不依赖
  Infrastructure。
- Why now: Domain ports稳定后实现唯一业务 orchestration owner，不让 Controller掌管 permit/session。
- Contract/signature changes: one application API；`DeepResearchRunService`包装 downstream cancel与 lease release但不公开
  semaphore。
- Input/output and state mapping: Command -> validated TaskBO with UUID/deadline -> capacity lease -> yuheng run ->
  wrapped idempotent run。
- Error and edge behavior: capacity fail在 gateway前；yuheng sync失败/observer failure/terminal/cancel均 CAS并 release
  once；不自动 retry。
- Standards impact: MC-VALID-001、MC-BEAN-001、MC-LOG-001、MC-PATTERN-001、MC-TIME-001。
- Literal rule enforcement: Rule 2 ValidationUtils复验；Rule 4 Slf4j/final/RequiredArgsConstructor/Qualifier/explicit
  bean name；Rule 9 Facade+Observer+Bulkhead；Rule 10 injected Clock；Rule 11 Application不知技术 adapter。
- Implementation pseudocode:

```java
validate StartDeepResearchCommand and DeepResearchRuntimeProperties, generate runId and deadline from injected Clock and create immutable DeepResearchTaskBO
acquire a fair ResearchCapacityService lease before calling the Domain Yuheng; on saturation throw RESEARCH_CAPACITY_EXHAUSTED without invoking downstream
wrap the caller observer with an atomic terminal flag so completed, failed, cancel and observer exception compete for one terminal outcome
start the yuheng, return a DeepResearchRunService whose cancel is idempotent, and release the capacity lease exactly once for every synchronous/asynchronous terminal path
log only runId, traceId, stage, outcome, duration and safe error code; never log topic, report, raw dependency error or secrets
```

- Verification contribution: Application TEST-005/006/009至012变 GREEN。
- After this file: 可用纯 ports驱动一次研究 run；技术 execution尚未实现。

- Validation working directory:
  `/Users/mario/SelfProject/Egon-COLA/egon-cola-archetypes/source-projects/egon-cola-source-agent`
- Verification command:
  `../../../mvnw -B -ntp test -pl egon-cola-source-agent-domain,egon-cola-source-agent-application -am -Dtest=DeepResearchDomainTest,DeepResearchManageImplTest -Dsurefire.failIfNoSpecifiedTests=false`
- Expected result: Domain/Application tests通过；dependency/import scan无 ADK/RxJava/HTTP/MCP；capacity/terminal每条路径释放一次。
- Failure returns to: File 1若业务场景偏离 Spec；File 2若模型/ports泄漏技术类型；File 3若 permit/terminal/cancel时序错误。
- Completion criteria: Agent REQ-003/007/009至013/018的内层业务合同成立，Infrastructure获得稳定 port。
- Rollback: 回退本 Step tests与 common/domain/application sources；保留六模块 skeleton。
- Commit paths:
  `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-{domain,application}/src/test/java/top/egon/cola/archetype/source/agent/{domain/research/DeepResearchDomainTest.java,application/research/DeepResearchManageImplTest.java,application/research/package-info.java,domain/research/package-info.java}`;
  `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-{common,domain}/src/main/java/top/egon/cola/archetype/source/agent/{common/error/ResearchErrorCodeEnum.java,common/package-info.java,domain/research/yuheng/DeepResearchAgentGateway.java,domain/research/model/DeepResearchTaskBO.java,domain/research/model/ResearchTopicBO.java,domain/research/model/DeepResearchEvent.java,domain/research/model/ResearchEventTypeEnum.java,domain/research/model/ResearchStageEnum.java,domain/research/model/ReportLanguageEnum.java,domain/research/service/DeepResearchEventObserverService.java,domain/research/service/DeepResearchRunService.java,domain/research/package-info.java}`;
  `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-application/src/main/java/top/egon/cola/archetype/source/agent/application/research/{command/StartDeepResearchCommand.java,config/DeepResearchRuntimeProperties.java,manage/DeepResearchManage.java,manage/impl/DeepResearchManageImpl.java,service/ResearchCapacityService.java,exception/DeepResearchApplicationException.java,package-info.java}`
- Commit: `feat(agent-archetype): model deep research use case`

### Step 3 — 适配 Agent Flow 会话、事件与 MCP 工具边界

- Requirements: REQ-004, REQ-006, REQ-009, REQ-010, REQ-012, REQ-018
- Dependencies: Step 2；Agent Flow public API已实现
- Baseline state: Domain Gateway存在但没有 Infrastructure实现；没有 ADK Event安全投影、Session清理或 MCP
  properties/factory。
- Observable outcome: Infrastructure用 AgentFlowService创建/流式执行/删除独立 Session，转换 allowlist事件并在
  complete/error/cancel exactly-once清理；MCP factory拥有 typed配置与 close边界。
- End state: 技术 adapter可被 Starter注入，但尚未创建真实 ChatModel/MCP client或加载 fixed flow profiles。
- Test-first gate: Required — 先创建 Yuheng/EventConverter/MCP factory tests形成 RED，再实现 static converter、gateway与
  tool factory到 GREEN。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-SCOPE-001,
  MC-TEST-001
- Literal Rules: Rule 1, Rule 3, Rule 4, Rule 5, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 —
`CREATE egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-infrastructure/src/test/java/top/egon/cola/archetype/source/agent/infrastructure/research/{AgentFlowDeepResearchAgentGatewayTest.java,AgentFlowEventConverterTest.java,McpResearchToolFactoryTest.java,package-info.java}`

- Purpose: 固定 component调用次序、事件 redaction、cleanup exactly-once和 MCP fail-closed/close行为。
- Symbols: create-execute-delete；complete/error/cancel races；ADK metadata/tool args denylist；converter round
  trip；empty/missing MCP tools。
- Repository evidence: primary TEST-008至015；Agent Flow API固定 List/Event/Flowable与 session commands。
- Dependencies and consumers: mock/fake AgentFlowService和 fake ToolCallbacks/client seam；无 real endpoint/credential。
- Why now: 第三方边界最容易泄露 raw event/secret，必须先写 allowlist和 lifecycle测试。
- Contract/signature changes: 无生产合同；测试仅用 synthetic ADK Event builder与 fake safe text。
- Input/output and state mapping: task -> component session/execution command；ADK Event -> Domain
  Event；terminal/cancel -> deleteSession once；MCP client -> unique callbacks。
- Error and edge behavior: create失败不delete；subscribe后任意终态delete一次；converter忽略 raw metadata/tool args；missing
  endpoint/key/empty tools启动失败并 close partial client。
- Standards impact: MC-CONVERT-001、MC-LOG-001、MC-BEAN-001、MC-TEST-001。
- Literal rule enforcement: Rule 3双向 BaseConverter/MapStruct测试；Rule 4安全日志；Rule 5只用 component/Spring AI；Rule 9
  Adapter职责；Rule 11 Infrastructure唯一知道 ADK/MCP。
- Implementation pseudocode:

```java
script AgentFlowService createSession, executeStream and deleteSession and assert one unique session per run with exact flowId and server-owned user identity
emit ADK progress, final, error and metadata-rich events; assert converter maps only approved author/text/final fields and both BaseConverter directions compile
race complete, upstream error and cancel; assert subscription disposal, component session deletion and wrapped run terminal callback each happen exactly once
construct MCP client/tool fixtures for valid, missing endpoint/key, duplicate callback names, empty callbacks and close failure; assert fail closed and all resources attempted
```

- Verification contribution: TEST-008至015的 Infrastructure部分 RED/GREEN。
- After this file: tests RED，因为 converter/yuheng/tool files不存在。

#### File 2 —
`CREATE egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-infrastructure/src/main/java/top/egon/cola/archetype/source/agent/infrastructure/research/{converter/AgentFlowEventConverter.java,yuheng/AgentFlowDeepResearchAgentGateway.java,package-info.java}`

- Purpose: 用 Adapter模式隔离 AgentFlow/ADK/RxJava，并把 raw events投影为 Domain allowlist。
- Symbols: static `AgentFlowEventConverter.INSTANCE` implements `BaseConverter<Event,DeepResearchEvent>`；Yuheng
  implements Domain port；idempotent run handle。
- Repository evidence: primary §10.4明确 exact generic与双向 mapping；§13选择 Adapter；Component不负责业务 event映射。
- Dependencies and consumers: 注入 `agentFlowService`、flowId/Clock；Application只看 Domain port；Starter扫描/装配 concrete
  bean。
- Why now: tests已固定 mapping/cleanup，可实现唯一技术隔离层。
- Contract/signature changes: 新 Infrastructure Bean `agentFlowDeepResearchAgentGateway`；不向内暴露 Flowable/Event。
- Input/output and state mapping: TaskBO -> session/execution command/message；raw Event author/content/final/error ->
  stage/type/delta/report/code；Domain reverse -> minimal Event builder。
- Error and edge behavior: unknown author/metadata不透传；oversize report/safe text
  bounded；create/subscribe/error/cancel全部按 ownership cleanup；不自动 retry。
- Standards impact: MC-ARCH-001、MC-CONVERT-001、MC-LOG-001、MC-BEAN-001、MC-TIME-001、MC-PATTERN-001。
- Literal rule enforcement: Rule 3 `@Mapper(ERROR)` + INSTANCE + exact BaseConverter；Rule 4 yuheng
  Slf4j/final/RequiredArgsConstructor/Qualifier；Rule 9 Adapter；Rule 10 Clock/Instant；Rule 11技术只在 Infrastructure。
- Implementation pseudocode:

```java
map DeepResearchTaskBO to AgentFlowSessionCommand and AgentFlowExecutionCommand using the fixed flowId, runId-derived component userId and generated sessionId
subscribe once to AgentFlowService.executeStream and convert each Event through AgentFlowEventConverter.INSTANCE using an explicit author-to-stage allowlist
forward validated Domain events to the observer, select one terminal outcome with an atomic guard, dispose the subscription and call deleteSession exactly once
on cancel invoke the same cleanup path; wrap safe dependency failures without raw ADK metadata, tool arguments, user/session identifiers, topic or provider message
provide reverse converter mapping only a minimal synthetic ADK Event required by contract tests, ignoring every non-public metadata field with explicit MapStruct rules
```

- Verification contribution: TEST-008至013变 GREEN，证明 session隔离与 raw event redaction。
- After this file: Domain Gateway完成；MCP factory tests仍 RED。

#### File 3 —
`CREATE egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-infrastructure/src/main/java/top/egon/cola/archetype/source/agent/infrastructure/research/tool/{McpResearchToolFactory.java,McpResearchToolProperties.java,package-info.java}`

- Purpose: 封装一个同步 SSE MCP client、非空唯一 ToolCallbacks snapshot和有界关闭。
- Symbols: typed properties；factory `create`/`close`；package-private client adapter seam；bean output later named
  `deepResearchSearchTools`。
- Repository evidence: primary INTERNAL-003/REQ-006；Spring AI 1.1.8 MCP能力由 Step执行时源码校准；通用 Component不拥有
  MCP。
- Dependencies and consumers: Starter配置调用 factory并把 callbacks注入 ChatModel options；tests使用 fake client seam。
- Why now: Gateway完成后建立 host tool dependency边界，尚不在此文件创建 provider ChatModel。
- Contract/signature changes: 新 Infrastructure lifecycle factory；endpoint/key/timeout/tool allowlist由
  properties构造，不出现在 public request。
- Input/output and state mapping: external properties -> MCP transport/client -> sorted unique immutable ToolCallback
  list；close -> every resource attempted。
- Error and edge behavior: dev/prod missing endpoint/key、connect/init失败、zero/duplicate callbacks均启动失败并 close
  partial resources；异常/log不含 values。
- Standards impact: MC-DEP-001、MC-LOG-001、MC-BEAN-001、MC-CONFIG-001、MC-UTIL-001。
- Literal rule enforcement: Rule 1 Properties/Factory语义；Rule 4 Slf4j/constructor/explicit bean later；Rule 5使用 Spring
  AI MCP而非自研协议；Rule 11 Infrastructure ownership。
- Implementation pseudocode:

```java
validate McpResearchToolProperties for nonblank externally supplied endpoint and credential plus request timeout not exceeding total research timeout
build the Spring AI 1.1.8 synchronous SSE MCP client through one package-private adapter seam and initialize the available ToolCallbacks
reject an empty callback set or duplicate callback names, sort by name and publish an immutable snapshot without logging endpoint, key or tool arguments
if any initialization step fails close every allocated transport/client; expose idempotent close with delay-error semantics for ApplicationContext shutdown
```

- Verification contribution: TEST-015 MCP cases变 GREEN并提供 Step 4 named tool Bean原语。
- After this file: Infrastructure technical adapters均可由 fake环境验证，未连接真实 MCP。

- Validation working directory:
  `/Users/mario/SelfProject/Egon-COLA/egon-cola-archetypes/source-projects/egon-cola-source-agent`
- Verification command:
  `../../../mvnw -B -ntp test -pl egon-cola-source-agent-infrastructure -am -Dtest=AgentFlowDeepResearchAgentGatewayTest,AgentFlowEventConverterTest,McpResearchToolFactoryTest -Dsurefire.failIfNoSpecifiedTests=false`
- Expected result: yuheng/converter/tool tests全部通过；无网络请求；session/subscription/client清理 exactly once；raw
  metadata/secrets不进入 Domain/log。
- Failure returns to: File 1若 fake不匹配 public contracts；File 2若 session/Mapping/terminal语义错误；File 3若锁定 Spring
  AI MCP能力不足则返回 primary Spec REQ-006。
- Completion criteria: Agent REQ-004/006/009/010/012/018的 Infrastructure部分闭环，内层依赖方向不变。
- Rollback: 回退本 Step Infrastructure tests/sources；Domain/Application ports保留。
- Commit paths:
  `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-infrastructure/src/test/java/top/egon/cola/archetype/source/agent/infrastructure/research/{AgentFlowDeepResearchAgentGatewayTest.java,AgentFlowEventConverterTest.java,McpResearchToolFactoryTest.java,package-info.java}`;
  `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-infrastructure/src/main/java/top/egon/cola/archetype/source/agent/infrastructure/research/{converter/AgentFlowEventConverter.java,yuheng/AgentFlowDeepResearchAgentGateway.java,package-info.java}`;
  `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-infrastructure/src/main/java/top/egon/cola/archetype/source/agent/infrastructure/research/tool/{McpResearchToolFactory.java,McpResearchToolProperties.java,package-info.java}`
- Commit: `feat(agent-archetype): adapt agent flow and research tools`

### Step 4 — 装配具名 ChatModel、MCP Tools 与固定研究 Flow

- Requirements: REQ-004, REQ-005, REQ-006, REQ-010, REQ-011, REQ-012, REQ-018
- Dependencies: Step 3
- Baseline state: Infrastructure adapters可手工构造，但 Starter没有 Application、typed aggregate properties、provider model
  Bean、tool Bean、profile或 Agent Flow YAML。
- Observable outcome: dev/prod从环境获得 provider/MCP/API key并 fail closed；test用 fake model/tools；固定 Flow精确为
  Planner -> Parallel三分支 -> Writer。
- End state: Spring context能离线装配完整 Deep Research internal call path，HTTP Adapter尚未实现。
- Test-first gate: Required — 先创建 context/flow tests对缺 Bean、missing config、profile drift和 graph错误形成
  RED，再创建配置类与五份资源到 GREEN。
- Manual Checks: MC-ARCH-001, MC-DEP-001, MC-LOG-001, MC-BEAN-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001,
  MC-TEST-001
- Literal Rules: Rule 4, Rule 5, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 —
`CREATE egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/test/java/top/egon/cola/archetype/source/agent/starter/{DeepResearchApplicationTest.java,DeepResearchFlowTest.java,package-info.java}`

- Purpose: 固定 named Beans、profile key parity、missing dependency fail closed和真实 Agent Flow fixed graph。
- Symbols: context tests for fake test profile、missing model/MCP/tools/secrets；flow test for Planner/Parallel(
  Evidence/Counterpoint/Freshness)/Writer names/output keys/order/session isolation。
- Repository evidence: primary TEST-013至017；Agent Flow AutoConfiguration允许 named ChatModel并严格绑定 flow YAML。
- Dependencies and consumers: test configurations覆盖 external client/provider with
  fakes；ApplicationContext/SpringBootTest不开放端口。
- Why now: provider/tool/flow wiring是 source可运行前置，先确保 test profile不会误触外部网络。
- Contract/signature changes: 无生产合同；fixture key使用 synthetic值且不写入 main profiles。
- Input/output and state mapping: profile properties -> named model/tools/AgentFlow beans；fake model scripted outputs ->
  exact domain stages/final report。
- Error and edge behavior: missing/blank provider/MCP/API key、empty tools、wrong Bean name、invalid flow均 context
  failure并关闭 partial client/runtime。
- Standards impact: MC-BEAN-001、MC-CONFIG-001、MC-LOG-001、MC-PATTERN-001、MC-TEST-001。
- Literal rule enforcement: Rule 4逐名验证 Beans/Qualifier；Rule 7比较三 profile key集合；Rule 9验证 immutable fixed
  workflow；Rule 10 virtual Clock/Duration；Rule 11 Starter只装配。
- Implementation pseudocode:

```java
load the test profile with fake ChatModel and fake search ToolCallbacks and assert every approved bean name and qualifier resolves exactly once
remove model, MCP endpoint/key, API key or callbacks one condition at a time and assert the context fails before the Agent Flow registry is published
parse dev, test and prod YAML into flattened key sets and assert exact parity with no literal endpoint, credential or provider secret in tracked resources
execute the configured flow with deterministic model responses and assert Planner first, three named parallel branches, Writer last, unique output keys and isolated sessions
```

- Verification contribution: TEST-013至017的 RED/GREEN基础。
- After this file: tests RED，因为 Starter application/config/resources缺失。

#### File 2 —
`CREATE egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/java/top/egon/cola/archetype/source/agent/starter/{DeepResearchApplication.java,config/DeepResearchAiConfiguration.java,config/DeepResearchConfigurationProperties.java,package-info.java}`

- Purpose: 创建 Boot入口、aggregate typed properties和具名 model/tools/clock/validation/Manage/Yuheng wiring。
- Symbols: `deepResearchChatModel`、`deepResearchSearchTools`、`deepResearchClock`、`deepResearchValidationUtils`
  、configuration-properties registration；Application scan boundary。
- Repository evidence: source-web Starter只装配 Adapter/Infrastructure并使用配置类；primary Bean表要求显式 names和
  constructor injection。
- Dependencies and consumers: 创建 Spring AI OpenAI-compatible ChatModel并引用 Infrastructure Mcp factory；Agent Flow通过
  configured bean name消费；Adapter在 Step 5消费 Manage。
- Why now: Core/Infrastructure均稳定，可在 host Starter拥有 provider和工具，而不污染 Component/Domain。
- Contract/signature changes: 新 internal runtime Beans；test profile通过 same-name fake Beans覆盖；不暴露 endpoint选择给
  request。
- Input/output and state mapping: typed external properties -> model builder/options + tools -> named ChatModel；runtime
  properties -> Manage/Capacity；component config独立由 YAML绑定。
- Error and edge behavior: dev/prod缺 secret/endpoint/model name或 tools为空启动失败；close context有界关闭
  MCP；日志只记录配置项名称而非值。
- Standards impact: MC-VALID-001、MC-LOG-001、MC-BEAN-001、MC-CONFIG-001、MC-UTIL-001。
- Literal rule enforcement: Rule 4所有具体 Bean显式名/Qualifier/constructor；Rule 5复用 Spring AI builders；Rule 7 typed
  properties；Rule 11 Starter无业务分支。
- Implementation pseudocode:

```java
bind one DeepResearchConfigurationProperties aggregate containing API, runtime, model and MCP nested records with Jakarta constraints and Duration bounds
create a named Clock and ValidationUtils, then create deepResearchSearchTools from McpResearchToolFactory with a test-profile same-name override seam
build the named OpenAI-compatible ChatModel from externally supplied base URL, API key and model name and attach the immutable search callbacks as default tools
wire capacity service, Domain Yuheng implementation and Manage implementation with explicit bean names and qualifiers; let Agent Flow resolve deepResearchChatModel by name
close the MCP factory on context shutdown and fail context creation before HTTP readiness for any invalid dependency configuration
```

- Verification contribution: named Bean/context failure tests变 GREEN；flow graph仍待 resources。
- After this file: Java wiring存在，但无 profile/flow values时 context按预期失败。

#### File 3 —
`CREATE egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/{application.yml,application-dev.yml,application-test.yml,application-prod.yml,agent/deep-research-flow.yml}`

- Purpose: 定义 profile-neutral key集合、test fake开关和 fixed Agent Flow tree/prompt ownership。
- Symbols: `agent.deep-research.*`; imports；`egon.cola.component.agent-flow.*`; four leaf agents + Sequential/Parallel
  nodes。
- Repository evidence: source-web使用 application + profile files；primary §8/§15列 exact resources与 key parity。
- Dependencies and consumers: DeepResearchConfigurationProperties、Spring AI model、MCP factory、Agent Flow strict
  Binder；test读取所有 profiles。
- Why now: Java properties已固定字段，可一次写入不漂移的环境配置与 flow contract。
- Contract/signature changes: 新 application configuration；dev/prod values只引用 environment placeholders且无可用
  default secret/URL；test用 local fake switch。
- Input/output and state mapping: env -> typed model/MCP/API/runtime values；flow YAML -> component tree；output keys在
  Writer instruction中引用 planner/three research outputs。
- Error and edge behavior: required env absent导致 binding/startup failure；unknown key由 component/host strict
  tests拒绝；不配置 retry。
- Standards impact: MC-CONFIG-001、MC-LOG-001、MC-JSON-001、MC-SCOPE-001。
- Literal rule enforcement: Rule 6配置不定义 public JSON；Rule 7四文件 key parity且值可不同；Rule 9固定 workflow；Rule 10
  ISO-8601 Duration；Rule 11 resources归 Starter。
- Implementation pseudocode:

```yaml
application.yml imports the active profile and agent/deep-research-flow.yml while declaring no credential or provider endpoint value
dev and prod bind every model, MCP, API-key, timeout, concurrency and docs key from environment placeholders without permissive secret defaults
test declares the identical flattened key set but selects fake model/tools and synthetic non-secret API key values with no external endpoint access
deep-research-flow enables Agent Flow and declares Sequential root [Planner, ParallelResearch, Writer], with ParallelResearch children [EvidenceResearcher, CounterpointResearcher, FreshnessResearcher]
assign unique output keys for Planner and all three branches, make Writer reference only those evidence keys, and keep model/tool/prompt selection server-owned
```

- Verification contribution: TEST-014/016 flow/profile gates变 GREEN；full context仍不开放网络。
- After this file: internal Deep Research flow可在 test profile离线执行。

- Validation working directory:
  `/Users/mario/SelfProject/Egon-COLA/egon-cola-archetypes/source-projects/egon-cola-source-agent`
- Verification command:
  `../../../mvnw -B -ntp test -pl egon-cola-source-agent-starter -am -Dtest=DeepResearchApplicationTest,DeepResearchFlowTest -Dsurefire.failIfNoSpecifiedTests=false`
- Expected result: context/fixed-flow tests通过；test profile零外部连接；dev/prod missing config cases fail closed；graph
  exact且 session隔离。
- Failure returns to: File 1若 test fixture绕过真实 Bean/config；File 2若 name/Qualifier/lifecycle错误；File 3若 key
  parity或 flow topology错误。
- Completion criteria: Agent REQ-004至006/010至012/018的 host装配闭环。
- Rollback: 回退本 Step Starter tests/Java/resources；Infrastructure adapters仍可单测。
- Commit paths:
  `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/test/java/top/egon/cola/archetype/source/agent/starter/{DeepResearchApplicationTest.java,DeepResearchFlowTest.java,package-info.java}`;
  `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/java/top/egon/cola/archetype/source/agent/starter/{DeepResearchApplication.java,config/DeepResearchAiConfiguration.java,config/DeepResearchConfigurationProperties.java,package-info.java}`;
  `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/{application.yml,application-dev.yml,application-test.yml,application-prod.yml,agent/deep-research-flow.yml}`
- Commit: `feat(agent-archetype): configure fixed research flow`

### Step 5 — 实现唯一 POST SSE API 与安全边界映射

- Requirements: REQ-007, REQ-008, REQ-009, REQ-010, REQ-011, REQ-012, REQ-014, REQ-018
- Dependencies: Steps 2-4
- Baseline state: 内部 use case/context可运行，但 Adapter没有 Request/VO/converters、API key/trace filters、Controller、SSE
  bridge或 error handler。
- Observable outcome: `POST /api/v1/deep-research/runs`按 exact media/auth/validation/capacity规则返回 SSE；pre-stream
  errors为 JSON，stream errors为唯一 failed event，断连取消 run。
- End state: runtime API-001完成且无第二 endpoint；OpenAPI文档生成配置/最终 gate留 Step 6。
- Test-first gate: Required — 先创建 MockMvc/filter/converter/SSE tests覆盖 TEST-001至012与 mapping round trips，再实现
  Adapter files到 GREEN。
- Manual Checks: MC-ARCH-001, MC-VALID-001, MC-NAME-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-JSON-001,
  MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 —
`CREATE egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-adapter/src/test/java/top/egon/cola/archetype/source/agent/adapter/{research/DeepResearchControllerTest.java,research/DeepResearchConverterTest.java,filter/ResearchApiKeyFilterTest.java,research/package-info.java,filter/package-info.java}`

- Purpose: 固定 request/media/auth、SSE id/event/data/terminal、pre-stream/stream error、disconnect与三 converter双向映射。
- Symbols: TEST-001至012；MockMvc async/SseEmitter callbacks；synthetic API keys；BaseConverter round trips；secret/log
  capture。
- Repository evidence: source-web Adapter tests用 standalone MockMvc；primary API-001表给出 status/header/schema和 SSE事件。
- Dependencies and consumers: fake DeepResearchManage/run/observer；不加载 Infrastructure或真实 model/MCP。
- Why now: 外部合同必须先于 Controller实现，尤其 HTTP 200后不能更改 status的错误分流。
- Contract/signature changes: 无生产合同；test JSON明确 absent与 explicit null差异、unknown字段和 enum coercion拒绝。
- Input/output and state mapping: headers/body -> Request -> Command -> fake events -> EventVO/SSE；exception -> error
  response/status/headers。
- Error and edge behavior: 400/401/406/415/429/500/503、stream failed、EOF、send failure/disconnect、terminal race和 late
  events全部断言。
- Standards impact: MC-VALID-001、MC-CONVERT-001、MC-LOG-001、MC-JSON-001、MC-TEST-001。
- Literal rule enforcement: Rule 2边界validation；Rule 3三 static converter round trips；Rule 6 Jackson/SSE精确；Rule 9
  observer bridge；Rule 10 millisecond UTC；Rule 11 Adapter不依赖 Infrastructure。
- Implementation pseudocode:

```java
send valid and invalid JSON with API key, trace, Accept and Content-Type combinations; assert exact status, headers, field paths and zero Manage calls on rejection
emit started, interleaved progress, completed and failed Domain events through a fake observer; assert SSE ids runId:sequence, event names, one-line JSON data and one terminal event
race emitter completion, timeout, error and client disconnect; assert the returned run cancel executes once and no late event is written
round-trip Request/Command, exception/error response and Domain Event/EventVO through the three BaseConverter interfaces and reject any unmapped or sensitive field
capture logs and JSON/OpenAPI fixtures to prove API key, topic, raw report, tool metadata and dependency messages never appear outside approved response fields
```

- Verification contribution: TEST-001至012全面 RED/GREEN目标。
- After this file: tests RED，因为 Adapter production files不存在。

#### File 2 —
`CREATE egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-adapter/src/main/java/top/egon/cola/archetype/source/agent/adapter/{config/DeepResearchApiProperties.java,filter/ResearchApiKeyFilter.java,filter/ResearchTraceFilter.java,handler/DeepResearchGlobalExceptionHandler.java,handler/DeepResearchErrorResponse.java,package-info.java}`

- Purpose: 实现 API配置、常量时间认证、trace规范化与 pre-stream安全错误映射。
- Symbols: named filters/advice；API properties；error response record；ordered filter registration。
- Repository evidence: source-web有 filter/global advice/trace惯例；primary定义 `X-Research-Api-Key`、`X-Trace-Id`和
  status表。
- Dependencies and consumers: Spring MVC filter chain；Controller/exception converter；Starter typed aggregate
  properties提供值。
- Why now: Controller前先确保未认证/非法请求不会进入付费 use case。
- Contract/signature changes: API key header/auth scheme、trace response header、JSON error schema；V1无 tenant/user。
- Input/output and state mapping: raw header -> constant-time byte comparison；trace absent/valid/invalid ->
  generated/accepted/400；typed exception -> status/header/error。
- Error and edge behavior: missing/wrong key均同一401 + `WWW-Authenticate`；不 trim key；invalid trace 400；429 Retry-After
  5；不回显 cause/secret。
- Standards impact: MC-VALID-001、MC-LOG-001、MC-BEAN-001、MC-JSON-001、MC-TIME-001。
- Literal rule enforcement: Rule 1 Properties/ErrorResponse语义；Rule 2 header validation；Rule 4
  Slf4j/final/RequiredArgsConstructor/named Bean；Rule 6 Jackson-only；Rule 10 Clock/Instant；Rule 11 Adapter ownership。
- Implementation pseudocode:

```java
bind DeepResearchApiProperties for header name, externally supplied secret, trace limits and Retry-After without exposing any value through accessors used by logging
order ResearchTraceFilter before ResearchApiKeyFilter, validate or generate traceId and place only traceId into request attributes, response headers and MDC lifecycle
compare presented and configured API-key bytes in constant time without trimming; return the same safe 401 body and WWW-Authenticate header for missing and wrong values
map validation, media, capacity, dependency and internal exceptions to exact JSON status/headers through one named RestControllerAdvice using injected Clock and safe codes
```

- Verification contribution: TEST-002至006 filter/pre-stream paths变 GREEN。
- After this file: 安全/错误边界可用，Controller/SSE/mappers仍 RED。

#### File 3 —
`CREATE egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-adapter/src/main/java/top/egon/cola/archetype/source/agent/adapter/research/{controller/DeepResearchController.java,converter/DeepResearchCommandConverter.java,converter/DeepResearchErrorConverter.java,converter/ResearchEventConverter.java,dto/StartDeepResearchRequest.java,vo/DeepResearchEventVO.java}`

- Purpose: 实现唯一 REST Command、三个 static MapStruct converters与 SSE observer/cancel bridge。
- Symbols: `DeepResearchController#startDeepResearch`; Request/VO records；converter `INSTANCE`s implementing exact
  BaseConverter generics。
- Repository evidence: primary §9.2规定 method/path/operationId/media/SSE events；§10.4规定 converters；Application
  Manage返回 cancellable run。
- Dependencies and consumers: Adapter只依赖 Application/Common；Spring MVC/Jackson/springdoc annotations；API client消费
  SSE。
- Why now: security/error boundary和 internal use case已完成，可接一个 direct Controller而无额外 command bus。
- Contract/signature changes: 新 `POST /api/v1/deep-research/runs`；consumes JSON/produces event-stream；无其他 endpoint。
- Input/output and state mapping: Request + trace -> Command；Domain Event -> VO + SSE id/name/data；application
  exception -> ErrorResponse；emitter callbacks -> run.cancel。
- Error and edge behavior: absent defaults与 explicit null规则；pre-stream throw交 Advice；started后任何 error映射唯一
  failed event；send failure/disconnect cancel；late event ignored。
- Standards impact: MC-NAME-001、MC-VALID-001、MC-CONVERT-001、MC-LOG-001、MC-JSON-001、MC-PATTERN-001、MC-TIME-001。
- Literal rule enforcement: Rule 1 Request/Command/VO/Converter；Rule 2 Jakarta constraints；Rule 3 `@Mapper(ERROR)` +
  BaseConverter both directions；Rule 4 Controller Slf4j/constructor injection；Rule 6 explicit JSON/SSE；Rule 9 Observer
  bridge；Rule 10 Instant；Rule 11 Adapter不知 Infrastructure。
- Implementation pseudocode:

```java
declare one POST mapping at /api/v1/deep-research/runs consuming application/json and producing text/event-stream with the approved validation and OpenAPI annotations
convert StartDeepResearchRequest plus trusted traceId to StartDeepResearchCommand through a static ERROR-policy BaseConverter, applying defaults only for absent optional fields
create SseEmitter with the configured total timeout, install completion/timeout/error callbacks before invoking Manage and retain the returned run for idempotent cancellation
adapt Domain events through ResearchEventConverter, emit monotonically increasing runId:sequence ids and exact research.* names, then complete after the single terminal event
map application exceptions through DeepResearchErrorConverter for pre-stream JSON; after stream start emit one safe failed VO and close without changing HTTP status
```

- Verification contribution: TEST-001至012全部 GREEN，API-001 runtime合同完成。
- After this file: 唯一外部 API可在 MockMvc/fake Manage下验证；OpenAPI总合同待 Step 6。

- Validation working directory:
  `/Users/mario/SelfProject/Egon-COLA/egon-cola-archetypes/source-projects/egon-cola-source-agent`
- Verification command:
  `../../../mvnw -B -ntp test -pl egon-cola-source-agent-adapter -am -Dtest=DeepResearchControllerTest,DeepResearchConverterTest,ResearchApiKeyFilterTest -Dsurefire.failIfNoSpecifiedTests=false`
- Expected result: TEST-001至012通过；只有一个 endpoint；auth/media/validation在 Manage前拒绝；SSE terminal/cancel exactly
  once且零 secret/raw metadata泄露。
- Failure returns to: File 1若合同断言与 Spec API表不一致；File 2若 filter/status/header/trace错误；File 3若 mapping/SSE
  lifecycle或 converter反向构造失败。
- Completion criteria: Agent REQ-007至012/014/018的 runtime API部分闭环。
- Rollback: 回退本 Step Adapter tests/sources；internal use case仍可离线测试。
- Commit paths:
  `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-adapter/src/test/java/top/egon/cola/archetype/source/agent/adapter/{research/DeepResearchControllerTest.java,research/DeepResearchConverterTest.java,filter/ResearchApiKeyFilterTest.java,research/package-info.java,filter/package-info.java}`;
  `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-adapter/src/main/java/top/egon/cola/archetype/source/agent/adapter/{config/DeepResearchApiProperties.java,filter/ResearchApiKeyFilter.java,filter/ResearchTraceFilter.java,handler/DeepResearchGlobalExceptionHandler.java,handler/DeepResearchErrorResponse.java,package-info.java}`;
  `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-adapter/src/main/java/top/egon/cola/archetype/source/agent/adapter/research/{controller/DeepResearchController.java,converter/DeepResearchCommandConverter.java,converter/DeepResearchErrorConverter.java,converter/ResearchEventConverter.java,dto/StartDeepResearchRequest.java,vo/DeepResearchEventVO.java}`
- Commit: `feat(agent-archetype): expose deep research SSE command`

### Step 6 — 冻结 OpenAPI、包文档与 Source Verify

- Requirements: REQ-002, REQ-003, REQ-004, REQ-005, REQ-012, REQ-013, REQ-014, REQ-015, REQ-018
- Dependencies: Steps 1-5
- Baseline state: runtime API/fixed flow focused tests通过，但 `/v3/api-docs`、全 package-info/forbidden source
  contract和最终 README尚未闭环。
- Observable outcome: generated OpenAPI精确描述 API-001；每个 Java package有文档；source project `clean verify`
  证明架构、依赖、secret、配置与业务单域。
- End state: 正常 source project成为可生成的唯一业务事实源，可进入 definition阶段。
- Test-first gate: Required — 先创建 OpenAPI/SourceContract tests并观察缺 config/package docs/README contracts的
  RED，再补配置、package-info和最终文档到 GREEN。
- Manual Checks: MC-ARCH-001, MC-DEP-001, MC-NAME-001, MC-LOG-001, MC-JSON-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 4, Rule 5, Rule 6, Rule 7, Rule 10, Rule 11
- Ordered files:

#### File 1 —
`CREATE egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/test/java/top/egon/cola/archetype/source/agent/starter/{DeepResearchOpenApiTest.java,AgentSourceContractTest.java}`

- Purpose: 对 `/v3/api-docs`和完整 source tree建立最终合同。
- Symbols: API-GATE JSONPath assertions；one operationId；security
  scheme；status/headers/schemas/constraints；business/package/dependency/secret/time/logging denylist。
- Repository evidence: source-web有 OrganizationOpenApiTest；primary TEST-017至020要求 effective POM/source/OAS。
- Dependencies and consumers: test profile + MockMvc；source scanner读取 sibling modules；definition verifier复用相同期望而不复制
  Java logic。
- Why now: 所有 runtime types稳定后再测试生成文档，避免注解反向驱动业务设计。
- Contract/signature changes: 无 production API变化；测试把已接受 API表变为 drift gate。
- Input/output and state mapping: runtime annotations/records -> OpenAPI JSON；source/POM/resources -> allow/deny
  inventory。
- Error and edge behavior: duplicate operationId、缺 media/security/response/header、secret example、第二业务包、legacy
  Swagger/Fastjson/Date/DB依赖均 fail。
- Standards impact: MC-ARCH-001、MC-DEP-001、MC-NAME-001、MC-LOG-001、MC-JSON-001、MC-CONFIG-001、MC-TEST-001。
- Literal rule enforcement: Rule 1 naming scan；Rule 4 logging/DI scan；Rule 5 dependency scan；Rule 6 OAS/Jackson；Rule 7
  profile parity；Rule 10 time imports；Rule 11 module/package graph。
- Implementation pseudocode:

```java
request /v3/api-docs in the test profile and assert exactly POST /api/v1/deep-research/runs with operationId startDeepResearch
assert JSON request, text/event-stream success, API-key security, trace/header contracts, all error statuses/headers and Request/Event/Error schema constraints
walk all six modules to assert only research business roots, exact direct dependencies, no source sentinel, no DB/cache/MQ/RPC/GraphQL/Fastjson/legacy Swagger and no secret literals
require package-info.java in every populated Java package and assert all required test class names, profiles, fixed flow file and dual-language README sections exist
```

- Verification contribution: TEST-017至020 RED/GREEN和 definition verifier的 source基线。
- After this file: tests因 OpenAPI config/package docs/final docs缺失而 RED。

#### File 2 —
`CREATE egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/java/top/egon/cola/archetype/source/agent/starter/config/{DeepResearchOpenApiConfiguration.java,package-info.java}`

- Purpose: 注册 code-first OpenAPI info、`researchApiKey` security scheme与 docs production toggle。
- Symbols: named `deepResearchOpenApi`; API key header scheme；info/version；docs enabled property。
- Repository evidence: source-web `OrganizationSwaggerConfig`创建 OpenAPI Bean；primary §9.0禁止 Springfox和 prod
  docs默认开启。
- Dependencies and consumers: springdoc从 Controller/records/Bean生成 JSON；OpenApiTest消费。
- Why now: Controller annotations已稳定，只需最小 config补全全局 security/info。
- Contract/signature changes: dev/test可访问 docs；prod默认 disabled；不检查入 generated JSON。
- Input/output and state mapping: typed docs property -> springdoc exposure；Bean -> OpenAPI components/security scheme。
- Error and edge behavior: scheme name/header必须 exact；不得包含 key example、endpoint或 provider info。
- Standards impact: MC-BEAN-001、MC-JSON-001、MC-CONFIG-001、MC-SCOPE-001。
- Literal rule enforcement: Rule 4显式 Bean名/constructor；Rule 6 OAS3 only；Rule 7 same key in profiles；Rule 11
  Starter配置无业务分支。
- Implementation pseudocode:

```java
create one named OpenAPI bean with safe title/version and an apiKey security scheme named researchApiKey using header X-Research-Api-Key
let Controller and records remain the operation/schema source of truth; do not duplicate field schemas or add Springfox annotations
bind docs enabled under agent.deep-research.docs and keep prod false while dev/test contract profiles may enable generated docs
assert no example, description or extension contains an API key, MCP endpoint, model endpoint, topic or provider-specific credential
```

- Verification contribution: API-GATE test开始 GREEN。
- After this file: OpenAPI global contract存在；package docs/source README仍需完成。

#### File 3 —
`CREATE egon-cola-archetypes/source-projects/egon-cola-source-agent/{egon-cola-source-agent-domain/src/main/java/top/egon/cola/archetype/source/agent/domain/research/{yuheng,model,service}/package-info.java,egon-cola-source-agent-application/src/main/java/top/egon/cola/archetype/source/agent/application/research/{command,config,manage,manage/impl,service,exception}/package-info.java}`

- Purpose: 为 Domain/Application其余 populated Java packages补齐与 Web profile一致的包级职责文档。
- Symbols: Domain yuheng/model/service与 Application command/config/manage/manage.impl/service/exception package
  comments。
- Repository evidence: source-web每个 populated package使用 `package-info.java`；primary verifier要求 exact Web package
  documentation。
- Dependencies and consumers: Javadoc、SourceContract、generated verifier；不产生 runtime行为；Infrastructure/Adapter
  package docs由 File 4负责。
- Why now: 业务树已最终稳定，可一次按实际目录补齐，避免创建空/未来包。
- Contract/signature changes: documentation-only；不得增加 annotation依赖或 package层。
- Input/output and state mapping: actual populated directories -> exactly one package-info；包说明映射真实 layer
  responsibility。
- Error and edge behavior: 已在前序 Step创建的 package-info不覆盖；空目录不制造；package声明必须与目录/package
  substitution一致。
- Standards impact: MC-ARCH-001、MC-NAME-001、MC-SCOPE-001、MC-TEST-001。
- Literal rule enforcement: Rule 1 说明语义职责；Rule 11 文档证明 exact six-layer ownership，不新增结构。
- Implementation pseudocode:

```text
enumerate every directory under all six src/main/java and src/test/java roots that contains at least one Java file
for each directory missing package-info.java add the exact package declaration and a concise responsibility boundary matching adjacent Web source style
skip directories that already received package-info in earlier Steps and do not create empty packages, annotations or extra architecture layers
rerun AgentSourceContractTest and Javadoc compilation after package substitution assumptions are checked
```

- Verification contribution: SourceContract package completeness和 generated Javadoc前置变 GREEN。
- After this file: Domain/Application每个实际 Java package均有可生成文档的职责说明。

#### File 4 —
`CREATE egon-cola-archetypes/source-projects/egon-cola-source-agent/{egon-cola-source-agent-infrastructure/src/main/java/top/egon/cola/archetype/source/agent/infrastructure/research/{converter,yuheng}/package-info.java,egon-cola-source-agent-adapter/src/main/java/top/egon/cola/archetype/source/agent/adapter/{config,filter,handler,research,research/controller,research/converter,research/dto,research/vo}/package-info.java}`

- Purpose: 为 Infrastructure/Adapter其余 populated Java packages补齐技术隔离与 HTTP边界文档。
- Symbols: Infrastructure converter/yuheng；Adapter config/filter/handler/research/controller/converter/dto/vo package
  comments。
- Repository evidence: source-web同样为技术子包与 Adapter子包逐目录提供 package-info；primary verifier要求 package
  substitution后完整。
- Dependencies and consumers: Javadoc、AgentSourceContract、generated verifier；不改变 Bean扫描或 runtime行为。
- Why now: 技术/API目录已在 Steps 3/5稳定，可精确列举而不使用递归 wildcard创建未知包。
- Contract/signature changes: documentation-only；package声明必须与真实目录和生成后的 package替换一致。
- Input/output and state mapping: exact populated directory allowlist -> one package-info each；职责文本映射
  Infrastructure/Adapter边界。
- Error and edge behavior: 不覆盖已存在 root/tool/test package-info，不为 empty/future package建文件，不加入 annotation。
- Standards impact: MC-ARCH-001、MC-NAME-001、MC-SCOPE-001、MC-TEST-001。
- Literal rule enforcement: Rule 1 记录 converter/DTO/VO行为语义；Rule 11 证明 Infrastructure与 Adapter依赖边界且不混层。
- Implementation pseudocode:

```text
add package-info.java to the exact populated Infrastructure converter and yuheng packages and describe third-party isolation plus safe mapping ownership
add package-info.java to the exact populated Adapter config, filter, handler, research, controller, converter, dto and vo packages
use only matching package declarations and concise responsibility comments; add no annotations, component scan hints or new package directories
rerun source contract and Javadoc checks to prove the generated package substitution preserves every declaration
```

- Verification contribution: SourceContract完成所有技术/API package documentation assertions。
- After this file: 六模块所有 populated Java package均有精确职责说明。

#### File 5 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/{README.md,README.zh-CN.md}`

- Purpose: 将初始范围文档完善为双语生成项目使用/安全/运行边界合同。
- Symbols: setup/env表、curl SSE example、event/error表、architecture、testing、no durable state、Markdown safety、retry cost。
- Repository evidence: source projects和 public definitions维护双语 README；primary §16要求所有限制可见。
- Dependencies and consumers: source developers、generated consumers、basic verifier；示例严格匹配 API/config。
- Why now: code/OAS/config已稳定，文档可基于真实 symbols而非猜测。
- Contract/signature changes: 文档说明；不包含可用 URL/key或自动启动步骤。
- Input/output and state mapping: env variable names -> typed properties；request -> SSE sequence；failure -> client
  action。
- Error and edge behavior: 明示非幂等/重试新收费 run、重启丢失、断连无 resume、untrusted Markdown需
  sanitize、TLS/proxy由部署负责。
- Standards impact: MC-ARCH-001、MC-LOG-001、MC-JSON-001、MC-CONFIG-001、MC-SCOPE-001。
- Literal rule enforcement: Rule 6示例 exact JSON/SSE且无 secret；Rule 7完整 config key表；Rule 10 UTC/Duration；Rule
  11六模块说明。
- Implementation pseudocode:

```text
document the exact six-module dependency graph and state that research is the only business package with no database, status, history, resume, GraphQL, RPC or UI
list every required environment variable by name only, explain fake test profile and provide a placeholder curl request using the one POST SSE endpoint
describe event ordering, terminal/error/status headers, local capacity, total timeout, cancellation cleanup, non-idempotent retry cost and process-loss behavior
include source and generated verification commands, safe logging fields, external provider data boundary and untrusted Markdown sanitization guidance in both languages
```

- Verification contribution: README section/link/key assertions和人工 parity检查。
- After this file: verified source拥有完整 consumer/maintainer说明。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/pom.xml -pl egon-cola-source-agent -am clean verify && ./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-agent/pom.xml dependency:tree`
- Expected result: source Agent及 prerequisites BUILD SUCCESS；TEST-001至020和 architecture
  plugin通过；dependency/source/profile/secret gates无禁用项；不启动端口或外部服务。
- Failure returns to: File 1若 API/source期望不准确；File 2若 OAS/security/docs toggle错误；File 3若 Domain/Application
  package coverage错误；File 4若 Infrastructure/Adapter package coverage错误；File 5若双语合同漂移。
- Completion criteria: Agent REQ-002至005/012至015/018的 source-level证据闭环，可安全生成 Archetype。
- Rollback: 回退本 Step tests/OpenAPI/package docs/README修改；runtime代码仍保持 focused tests。
- Commit paths:
  `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/test/java/top/egon/cola/archetype/source/agent/starter/{DeepResearchOpenApiTest.java,AgentSourceContractTest.java}`;
  `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/java/top/egon/cola/archetype/source/agent/starter/config/{DeepResearchOpenApiConfiguration.java,package-info.java}`;
  `egon-cola-archetypes/source-projects/egon-cola-source-agent/{egon-cola-source-agent-domain/src/main/java/top/egon/cola/archetype/source/agent/domain/research/{yuheng,model,service}/package-info.java,egon-cola-source-agent-application/src/main/java/top/egon/cola/archetype/source/agent/application/research/{command,config,manage,manage/impl,service,exception}/package-info.java}`;
  `egon-cola-archetypes/source-projects/egon-cola-source-agent/{egon-cola-source-agent-infrastructure/src/main/java/top/egon/cola/archetype/source/agent/infrastructure/research/{converter,yuheng}/package-info.java,egon-cola-source-agent-adapter/src/main/java/top/egon/cola/archetype/source/agent/adapter/{config,filter,handler,research,research/controller,research/converter,research/dto,research/vo}/package-info.java}`;
  `egon-cola-archetypes/source-projects/egon-cola-source-agent/{README.md,README.zh-CN.md}`
- Commit: `test(agent-archetype): verify source API and architecture`

### Step 7 — 新增 Agent Definition 并验证派生 Archetype

- Requirements: REQ-001, REQ-002, REQ-003, REQ-015, REQ-016, REQ-017, REQ-018, REQ-019
- Dependencies: Step 6 source `clean verify`
- Baseline state: Agent source已验证，但 definitions仍只有既有六 family；`.generated`没有 Agent child且不可手工编辑。
- Observable outcome: 独立 definition声明 source/target/topology，basic IT生成 `deep-research-agent`并
  verify六模块、API/tests/docs/禁用面和无 sentinel。
- End state: `.generated`派生集合包含七个完整 child，旧六不变且 Git tracked generated count仍为零；release scripts尚按六计数并在
  Step 8修正。
- Test-first gate: Required — 先创建 Agent basic IT verifier/properties/goal，对缺 definition/metadata/generated child形成
  RED；再补 manifest/packaging/metadata/docs并运行 generic generator到 GREEN。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 5, Rule 6, Rule 7, Rule 11
- Ordered files:

#### File 1 —
`CREATE egon-cola-archetypes/definitions/egon-cola-archetype-agent/src/test/resources/projects/basic/{archetype.properties,goal.txt,verify.groovy}`

- Purpose: 先定义生成消费者的 semantic acceptance，而不是只检查文件存在。
- Symbols: fixture artifact `deep-research-agent`、package `it.pkg`、goal `verify`；Groovy assertions for
  modules/deps/source/tests/reports/OAS/config/docs/sentinels/forbidden paths。
- Repository evidence: existing Web definition basic IT读取生成源码/POM和 surefire reports；primary TEST-021要求同类完整
  verifier。
- Dependencies and consumers: maven-archetype-plugin integration-test消费；只读 generated fixture。
- Why now: definition合同必须从 consumer视角先 RED，避免仅能打包却不能生成可验证项目。
- Contract/signature changes: 新 Agent basic IT；无 runtime API变化。
- Input/output and state mapping: archetype properties -> generated artifact/package substitutions；generated
  tree/reports -> pass/fail。
- Error and edge behavior: 缺 module/file/report、sentinel残留、source internal GAV、wrong
  package、第二业务、DB/Flyway/GraphQL/RPC/secret、旧 Swagger均 fail并打印 path。
- Standards impact: MC-ARCH-001、MC-DEP-001、MC-NAME-001、MC-CONFIG-001、MC-TEST-001。
- Literal rule enforcement: Rule 1生成后命名扫描；Rule 5 dependency denylist；Rule 6 API/OAS/JSON契约；Rule 7 profile
  parity；Rule 11 exact Web six modules。
- Implementation pseudocode:

```groovy
assert generated root artifact deep-research-agent has exactly six ordered modules and only the approved direct dependency edges after package/artifact substitution
assert required Deep Research production types, test classes, profile/flow resources, package-info files, dual-language README and passing Surefire reports exist
parse generated OpenAPI test/source contracts and reject any second endpoint, second business root, source sentinel, internal source GAV or unchanged placeholder token
reject database, Flyway, Redis, MQ, Dubbo, GraphQL, facade module, Fastjson, legacy Swagger, Date APIs, literal credentials and provider endpoints across the generated project
```

- Verification contribution: TEST-021先形成可观察 RED，后验证真实 generated consumer。
- After this file: generic generator因 definition manifest/metadata缺失而 RED，旧六未改变。

#### File 2 —
`CREATE egon-cola-archetypes/definitions/egon-cola-archetype-agent/{archetype.properties,packaging-pom.xml,src/main/resources/META-INF/maven/archetype-metadata.xml,src/main/resources/META-INF/archetype-post-generate.groovy}`

- Purpose: 定义 source-to-public identity、六 module文件集、发布 POM与 wrapper executable后处理。
- Symbols: source `egon-cola-source-agent`; target `egon-cola-archetype-agent`; topology
  `common,domain,application,infrastructure,adapter,starter`; metadata modules/fileSets。
- Repository evidence: Web definition的 manifest/packaging/metadata/post-generate是 exact profile基线；generator按
  definition fields动态发现。
- Dependencies and consumers: `generate_archetypes.sh`读取；`.generated` packaging module消费；public release GAV
  `top.egon:egon-cola-archetype-agent:5.3.3`。
- Why now: basic verifier已先固定消费者要求，现补最小 definition使 generic pipeline可生成。
- Contract/signature changes: 第七个 public Archetype identity；packaging POM不依赖 organization/evaluation facades，因为
  Agent source无这些引用。
- Input/output and state mapping: internal source coordinates/package -> public target/resources tokenization；root
  version placeholder -> current release version。
- Error and edge behavior: source containment/topology mismatch由 generator失败；metadata漏 Java/resources/tests由 basic
  verifier失败；post-generate只设置 wrapper权限。
- Standards impact: MC-ARCH-001、MC-REUSE-001、MC-DEP-001、MC-SCOPE-001。
- Literal rule enforcement: Rule 5复用 archetype plugin/generator；Rule 7包含 profiles/resources；Rule 11 exact six module
  metadata。
- Implementation pseudocode:

```text
declare the exact sourceProject, internal GAV, sourcePackage, targetArtifactId and six-item expectedTopology in archetype.properties
copy the existing Web packaging contract but name the Agent artifact and omit facade test dependencies that the verified Agent source does not consume
declare root files and six metadata modules, filtering POM/README/Java/YAML while preserving wrapper/binary-safe files exactly like the Web definition
make post-generation logic only mark mvnw executable and avoid business rewrites, provider defaults, source edits or generated cleanup outside the new project
```

- Verification contribution: generator可以创建 seventh child并运行 basic IT。
- After this file: definition具备生成合同；架构/Javadoc文档仍缺使 verifier RED。

#### File 3 —
`CREATE egon-cola-archetypes/definitions/egon-cola-archetype-agent/{architecture-docs/agent-multi-module-architecture.md,src/main/javadoc/README.md}`

- Purpose: 提供 public Archetype六层/业务边界架构说明和 javadoc入口。
- Symbols: module dependency diagram、Deep Research call path、source/definition/generated ownership、no durable
  state/security limitations。
- Repository evidence: 每个 definition包含 architecture-docs和 javadoc；release要求 main/sources/javadoc artifacts。
- Dependencies and consumers: generated Archetype docs、Maven javadoc jar、basic verifier、consumers。
- Why now: definition identity和真实 source已稳定，文档可引用精确文件/模块。
- Contract/signature changes: documentation-only，不增加 runtime能力。
- Input/output and state mapping: verified source graph/API -> public architecture narrative；无状态转换。
- Error and edge behavior: 不宣称 production SLO、恢复、tenant/OAuth或事实准确性；不包含 secret/endpoint。
- Standards impact: MC-ARCH-001、MC-LOG-001、MC-CONFIG-001、MC-SCOPE-001。
- Literal rule enforcement: Rule 6 描述 exact SSE JSON边界；Rule 7 列环境 key owner；Rule 11 图示 exact six layers。
- Implementation pseudocode:

```text
document the six-module domain-first dependency graph and the API-to-Manage-to-Yuheng-to-AgentFlow call path with Adapter/Observer/Facade/Bulkhead ownership
explain source project as business truth, definition as packaging contract and ignored generated reactor as derived release material
state one POST SSE endpoint, fixed workflow, host-owned model/MCP configuration, process-local state and all excluded DB/GraphQL/RPC/UI capabilities
link Javadoc entry to the architecture and consumer README without embedding generated paths, credentials or environment-specific endpoints
```

- Verification contribution: basic IT architecture/Javadoc assertions与 release attachment前置。
- After this file: Agent definition完整且可生成/测试。

#### File 4 — `GENERATED egon-cola-archetypes/.generated/egon-cola-archetype-agent`

- Purpose: 由既有 atomic generator派生 public Maven Archetype child并验证 determinism；该目录不手工编辑/提交。
- Symbols: generated POM、archetype-resources、META-INF metadata、javadoc、basic IT、hash inventory。
- Repository evidence: base Spec已实现 ignored `.generated` reactor和 generate/check；Git tracked generated count应为零。
- Dependencies and consumers: generated profile、integration-test、Step 8 release shape；源仅来自 Step 6/definition Files
  1-3。
- Why now: verified source+definition齐备后才允许生成，避免手工修补派生输出。
- Contract/signature changes: 派生 reactor从六个 child变七个；不改变 Git tracked files。
- Input/output and state mapping: definition manifest + source tree + root version -> staged child -> atomic
  swap/hash；check重新生成比较。
- Error and edge behavior: 生成/IT失败保留旧完整 `.generated`；无 partial swap；任何手改由 check失败且必须回
  source/definition修复。
- Standards impact: MC-ARCH-001、MC-REUSE-001、MC-SCOPE-001、MC-TEST-001。
- Literal rule enforcement: Rule 5复用 generator/Maven；Rule 11 generated project精确六模块且无 hybrid。
- Implementation pseudocode:

```bash
run scripts/generate_archetypes.sh generate so the generic manifest discovery includes the new Agent definition with the six existing families
run scripts/check_archetypes.sh and require byte-for-byte deterministic regeneration plus a complete seven-child generated aggregator
run the Agent child integration-test so basic verify.groovy observes substituted six-module source and passing generated-project tests
assert git ls-files egon-cola-archetypes/.generated returns zero and never stage, patch or commit any generated file
```

- Verification contribution: TEST-021/022 generated evidence；`.generated`仍是 ephemeral验证输出。
- After this file: seventh public artifact可由 generated reactor验证，release scripts仍需七计数更新。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `bash scripts/test-generate-archetypes.sh && bash scripts/generate_archetypes.sh generate && bash scripts/check_archetypes.sh && ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -Pgenerated-archetypes -pl .generated/egon-cola-archetype-agent -am clean verify && test -z "$(git ls-files egon-cola-archetypes/.generated)"`
- Expected result: generator tests/生成/check/Agent basic IT全部通过；`.generated`恰七 child且零 tracked；旧六
  definition/source/Flyway tracked diff为空。
- Failure returns to: File 1若 consumer断言错误；File 2若 manifest/metadata/packaging错误；File 3若 docs缺失；File 4失败必须回
  source/definition，禁止手改 generated。
- Completion criteria: Agent REQ-001至003/015至019的 definition/generated部分闭环，public artifact ready for
  release-shape gate。
- Rollback: 删除新 Agent definition；重新运行 generate恢复六 child；`.generated`可整体重生但不通过 Git操作回滚。
- Commit paths:
  `egon-cola-archetypes/definitions/egon-cola-archetype-agent/src/test/resources/projects/basic/{archetype.properties,goal.txt,verify.groovy}`;
  `egon-cola-archetypes/definitions/egon-cola-archetype-agent/{archetype.properties,packaging-pom.xml,src/main/resources/META-INF/maven/archetype-metadata.xml,src/main/resources/META-INF/archetype-post-generate.groovy}`;
  `egon-cola-archetypes/definitions/egon-cola-archetype-agent/{architecture-docs/agent-multi-module-architecture.md,src/main/javadoc/README.md}`;
  `egon-cola-archetypes/.generated/egon-cola-archetype-agent` (generated verification only, never staged)
- Commit: `feat(agent-archetype): add generated archetype definition`

### Step 8 — 将依赖与发布门禁安全扩展为七制品

- Requirements: REQ-004, REQ-015, REQ-016, REQ-017, REQ-018, REQ-019, REQ-020
- Dependencies: Step 7 generated Agent `clean verify`
- Baseline state: definitions/generated已有七个产品，但 release fixture/script仍断言六；Spring dependency policy没有扫描
  source-agent。
- Observable outcome: policy扫描 Agent source版本/依赖；release fixture与 wrapper从 definitions推导并要求七个唯一
  main/sources/javadoc artifact，任一失败在 deploy前退出。
- End state: source -> definition -> generated -> seven-artifact dry-run完整；无 real Central publish，旧六和 Flyway不变。
- Test-first gate: Required — 先修改两个 test/policy scripts使其期待 Agent/七制品，观察现有 `maven-deploy.sh`六计数
  RED，再修改 wrapper到 GREEN。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 5, Rule 7, Rule 11
- Ordered files:

#### File 1 — `MODIFY scripts/{test-archetype-release.sh,test-spring-dependency-management.sh}`

- Purpose: 先把 seven-family release shape和 Agent source dependency policy写成失败合同。
- Symbols: expected definitions/artifacts `7`; Agent fixture/inventory；source-agent Boot/Springdoc/Spring AI/Components
  checks；old-six stability assertion。
- Repository evidence: current release test硬编码 expected=6并构建 six-family fixture；dependency policy枚举现有 source
  roots。
- Dependencies and consumers: CI/local preflight和 maven-deploy wrapper；读取 definitions/source POM，不发布。
- Why now: production release wrapper前必须有 RED证明，不直接把数字从6改7后自证。
- Contract/signature changes: 两个 script成功条件增加 Agent；所有失败仍 nonzero并在 deploy invocation前。
- Input/output and state mapping: definitions/POM/artifact fixture -> count/unique/attachment/policy
  assertions；临时目录自动清理。
- Error and edge behavior: missing/duplicate Agent、八或六 artifact、缺 sources/javadoc、source GAV误发布、wrong versions、旧六
  hash变化均失败。
- Standards impact: MC-REUSE-001、MC-DEP-001、MC-CONFIG-001、MC-SCOPE-001、MC-TEST-001。
- Literal rule enforcement: Rule 5 只用现有 Bash/Maven工具；Rule 7 静态检查 profile/版本owner；Rule 11 只验证
  family拓扑不改变代码层。
- Implementation pseudocode:

```bash
extend the release fixture with egon-cola-archetype-agent and require exactly seven unique definition-derived public targets
create main, sources and javadoc jar fixtures for all seven and assert missing, duplicate or extra artifacts fail before the mocked deploy marker
extend Spring dependency inventory to source-agent and assert Boot 3.5.16, Springdoc 2.8.17, Spring AI 1.1.8, ADK 0.7.0 and Components BOM ownership
snapshot the six existing definition/source/Flyway path hashes and assert the Agent-only change does not alter them
```

- Verification contribution: TEST-017/022/023先对旧 wrapper形成 RED并覆盖 preservation。
- After this file: dependency policy可 GREEN；release test因 wrapper仍要求6而 RED。

#### File 2 — `MODIFY scripts/maven-deploy.sh`

- Purpose: 把 release shape从六安全扩展为 definition-derived七制品，保留所有 preflight/opt-in规则。
- Symbols: artifact count/uniqueness assertion；Agent target inclusion；preflight顺序；`archetypes --dry-run`。
- Repository evidence: current script已有 source install -> generate/check -> profile verify -> release shape ->
  optional deploy；只缺第七 count。
- Dependencies and consumers: release operator/CI；消费 Steps 6/7 outputs和 File 1 tests。
- Why now: seventh artifact已经独立 verify，最后改变发布集合避免 partial/untested release path。
- Contract/signature changes: dry-run/`all --publish`可接受恰七 definitions；无新命令、无自动 publish。
- Input/output and state mapping: dynamic definition inventory -> expected public GAV set -> generated reactor
  attachments -> one root deployment；source GAV排除。
- Error and edge behavior: 任一 source/generate/check/IT/release-shape失败立即退出；`--publish`仍只允许 all；远端状态不自动
  retry。
- Standards impact: MC-REUSE-001、MC-DEP-001、MC-SCOPE-001、MC-TEST-001。
- Literal rule enforcement: Rule 5 复用现有 wrapper/pipeline且不新增工具；Rule 11 发布 family不改变 Web profile。
- Implementation pseudocode:

```bash
derive target artifactIds from validated definitions and require the unique set contains the six existing ids plus egon-cola-archetype-agent with total seven
preserve preflight order: source install, atomic generate, deterministic check, generated profile verify, release shape and only then optional one-root deploy
verify each public target contributes one main, sources and javadoc jar and reject every internal source GAV or generated aggregator artifact from the publish allowlist
keep archetypes --dry-run non-publishing and reject --publish outside all; on any gate failure exit before invoking the Central deploy command
```

- Verification contribution: TEST-023/024 release path变 GREEN并满足 Base REQ-020。
- After this file: seven-family release dry-run可验证，真实发布仍需用户另行授权。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `bash scripts/test-spring-dependency-management.sh && bash scripts/test-archetype-release.sh && bash scripts/test-generate-archetypes.sh && bash scripts/check_archetypes.sh && ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -Pgenerated-archetypes clean verify && bash scripts/maven-deploy.sh archetypes --dry-run`
- Expected result: policy/release/generator tests和 seven generated reactor通过；dry-run打印七 public artifacts且不调用
  Central；旧六/Flyway diff为空。
- Failure returns to: File 1若 fixture/policy inventory错误；File 2若 count/allowlist/preflight/publish guard错误；source或
  generated failure返回所属 Step，不能在 release脚本绕过。
- Completion criteria: Agent REQ-004/015至018与 Reactor REQ-019/020全部闭环；用户请求的第二阶段达到可评审实施终态。
- Rollback: 回退两个 test/policy scripts与 maven-deploy七计数；删除 Agent definition/source需按 Steps 7/1反向执行；不发布远端制品。
- Commit paths: `scripts/{test-archetype-release.sh,test-spring-dependency-management.sh}`; `scripts/maven-deploy.sh`
- Commit: `build(agent-archetype): release seven archetype families`

## 8. Test, Validation, and Quality Gates

| Gate/order            | Working directory       | Command or method                                | Scope                          | Expected result                          | Failure returns to            | Requirements/runtime boundary   |
|-----------------------|-------------------------|--------------------------------------------------|--------------------------------|------------------------------------------|-------------------------------|---------------------------------|
| 0 Prerequisite        | repository root         | Component Plan final focused/dependency commands | Agent Flow component           | Completed and exact public API available | prerequisite Plan             | Agent REQ-004; no fallback copy |
| 1 Topology            | source-agent            | architecture test                                | six POM/module edges           | exact Web non-open graph                 | Step 1                        | REQ-001至004/013                 |
| 2 Domain/Application  | source-agent            | two focused test classes                         | model/Manage/Bulkhead          | invariants and exactly-once release      | Step 2                        | REQ-003/007/009至013             |
| 3 Infrastructure      | source-agent            | three focused test classes                       | AgentFlow/MCP adapter          | session/tool cleanup and redaction       | Step 3                        | REQ-004/006/009/010/012         |
| 4 Host flow           | source-agent            | Application/Flow tests                           | Beans/profiles/fixed graph     | offline context and exact topology       | Step 4                        | REQ-004至006/010至012             |
| 5 API                 | source-agent            | Controller/Converter/Filter tests                | HTTP/SSE/security              | TEST-001至012 pass                        | Step 5                        | REQ-007至012/014                 |
| 6 OAS/source          | repository root         | source Agent clean verify                        | all six modules                | TEST-001至020 + architecture plugin       | Step 6                        | REQ-002至015/018                 |
| 7 Generator RED/GREEN | repository root         | generator tests/generate/check                   | seven definitions              | deterministic seven children             | Step 7                        | REQ-015至019                     |
| 8 Generated IT        | repository root         | generated profile Agent then full verify         | public Archetype               | basic verifier + all seven IT pass       | Step 7/owning source          | REQ-001至003/015至018             |
| 9 Release RED/GREEN   | repository root         | dependency/release script tests                  | policy/artifact shape          | exact seven, fail-before-deploy          | Step 8                        | REQ-016至020                     |
| 10 Dry-run            | repository root         | `scripts/maven-deploy.sh archetypes --dry-run`   | end-to-end static/build        | exit 0, no publish                       | owning Step                   | REQ-015至020; no Central         |
| 11 Live/manual        | user-controlled runtime | N/A in this Plan                                 | real provider/MCP/load/browser | not claimed                              | future operational validation | explicit authorization required |

Maven commands统一使用 repository wrapper与 `-B -ntp`。测试上下文不开放端口；MockMvc不是服务启动。任何 Gate失败禁止跳过，尤其不能通过手改
`.generated`、放宽 verifier或绕过 release shape获得 GREEN。

## 9. Migration, Compatibility, Rollout, and Rollback

数据库/数据迁移为 N/A：primary Spec §11明确新 family无 JDBC/Flyway/Redis；Base Spec既有 Flyway文件只做
hash/path保护，绝不修改。API为新增独立 family的 v1 contract，不影响现有 Web Archetype消费者。

Rollout顺序固定为：Component Plan完成 -> Agent source focused/clean verify -> Agent definition -> atomic
generate/check -> Agent basic IT -> seven full generated verify -> release tests -> non-publishing dry-run。真实
Central发布不在本 Plan授权范围；若未来获授权，仍只能在所有 Gates绿色后运行既有 `all --publish`入口。回滚优先按 Step提交逆序
path-limited revert并重新生成 ignored输出；已向 Central发布的版本不可删除/覆盖，只能新版本 forward-fix。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section            | Steps         | Files                           | Tests/gates          | Completion evidence                 |
|-------------|-----------------------------------|---------------|---------------------------------|----------------------|-------------------------------------|
| `REQ-001`   | Primary §4                        | 1, 7          | source/definition               | TEST-018/021         | independent family                  |
| `REQ-002`   | Primary §4                        | 1, 6, 7       | six POMs/architecture/verifier  | TEST-018/021         | exact Web profile                   |
| `REQ-003`   | Primary §4                        | 1-7           | research-only tree              | TEST-019/021         | one business root                   |
| `REQ-004`   | Primary §4 + Component dependency | 1, 3, 4, 6, 8 | POM/yuheng/config/policy       | TEST-013/014/017     | component reused, exact versions    |
| `REQ-005`   | Primary §4                        | 4, 6          | flow YAML/tests/docs            | TEST-014             | fixed graph/output keys             |
| `REQ-006`   | Primary §4                        | 3, 4          | MCP factory/profiles            | TEST-015/016         | one external tool source, test fake |
| `REQ-007`   | Primary §4                        | 2, 5          | Manage/Controller               | TEST-001             | one POST SSE                        |
| `REQ-008`   | Primary §4                        | 5             | filters/request/controller      | TEST-002至004         | 400/401/406/415 before Manage       |
| `REQ-009`   | Primary §4                        | 2, 3, 5       | events/yuheng/SSE              | TEST-007/009/012     | stable one-terminal sequence        |
| `REQ-010`   | Primary §4                        | 2-5           | run/yuheng/controller          | TEST-009至013         | one session and cleanup             |
| `REQ-011`   | Primary §4                        | 2, 4, 5       | capacity/properties/API         | TEST-005/011/015     | 429/timeout bounds                  |
| `REQ-012`   | Primary §4                        | 2-6           | logs/config/converters/OAS      | TEST-004/008/016/020 | no secret leakage                   |
| `REQ-013`   | Primary §4                        | 1, 2, 6, 7    | POM/tree/docs/verifier          | TEST-019/021         | no persistence/recovery claim       |
| `REQ-014`   | Primary §4                        | 5, 6          | annotations/OpenAPI config/test | TEST-020             | runtime/OAS parity                  |
| `REQ-015`   | Primary §4                        | 6-8           | source/definition/generated     | TEST-021/022/024     | clean verify at both stages         |
| `REQ-016`   | Primary §4/Base amendment         | 7, 8          | definition/release scripts      | TEST-022/023         | exact seven products                |
| `REQ-017`   | Primary §4/Base invariant         | 7, 8          | preservation tests              | TEST-019/022/023     | old six/Flyway unchanged            |
| `REQ-018`   | Primary §4                        | 1-8           | all tests/config/scripts        | TEST-001至024         | offline deterministic pipeline      |
| `REQ-019`   | Base §4                           | 7, 8          | verifier/preservation scripts   | Gate 7/9             | no Flyway/manual SQL changes        |
| `REQ-020`   | Base §4                           | 8             | release test/wrapper            | Gate 9/10            | every failure before deploy         |

## 11. Risks, Blockers, and User Decisions

| ID             | Risk or decision                                         | Impacted Steps/files | Evidence                            | Owner            | Status/action                                           |
|----------------|----------------------------------------------------------|----------------------|-------------------------------------|------------------|---------------------------------------------------------|
| `BLOCK-001`    | Agent Flow component尚未实现                                 | all Steps            | prerequisite Plan currently Review  | Implementer/User | Closed by gate — 本 Plan执行前必须先完成/验证/接受前置结果               |
| `RISK-001`     | Spring AI 1.1.8 MCP同步 SSE API可能与设计名称不同                   | Step 3 tool factory  | locked dependency尚未在 source使用       | Implementer      | Controlled — 源码/compile probe先行；能力缺失返回 Spec             |
| `RISK-002`     | POST + SseEmitter断连检测存在容器时序差异                            | Step 5               | MockMvc只能证明 callback ownership      | Host operator    | Accepted boundary — offline race tests；真实容器另验           |
| `RISK-003`     | OpenAI-compatible provider对 tool calling支持差异             | Step 4               | Spring AI abstraction不保证 provider功能 | Host operator    | Controlled — test fake；真实 provider为部署验收，不改源码合同          |
| `RISK-004`     | 全 source/dry-run可能遇到既有 AccessGuard validation provider失败 | Steps 6/8            | prior repository evidence           | Implementer      | Controlled —先复现并隔离，不跨 scope修复                           |
| `RISK-005`     | 七制品更新漏掉硬编码 inventory                                     | Step 8               | current two scripts已知显式 six         | Implementer      | Controlled — rg全仓 + fixture/shape gates；发现额外入口返回 Plan修订 |
| `DECISION-001` | 独立 Agent family + Web non-open六模块                        | source/definition    | user explicit                       | User             | Closed                                                  |
| `DECISION-002` | 业务仅 Deep Research、单 POST SSE、API Key、无 DB                | Steps 2-6            | user-approved recommendations       | User             | Closed                                                  |
| `DECISION-003` | fixed Flow、MCP SSE search、local capacity/no retry        | Steps 2-5            | user-approved recommendations       | User             | Closed                                                  |

没有未解决的 Spec或用户决策 blocker。`BLOCK-001`通过严格执行顺序关闭，不代表可并行开工；若前置 Plan失败，本 Plan保持未执行并返回前置
Spec。

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

八个 Step覆盖第二阶段独立 Agent Archetype、`egon-cola-archetype-web`六模块方式和唯一 Deep Research业务，并硬依赖第一阶段组件。没有把“参考
Web架构”扩成复制 Web的学生/组织/数据库/MQ/GraphQL/Dubbo功能。

### 12.2 Spec consistency

Plan保持 Accepted primary Spec的单 POST SSE、fixed flow、API key、MCP/model host配置、无 durable state与七制品合同。六项 Plan
Clarification只补执行必要的 package-info/tests、明确 converter Bean形式、锁定 MCP兼容 Gate、保护 generic generator和解决跨
Spec重复编号，不改变产品行为。Simplicity audit未发现 fetch-then-forward接口；一个 Manage方法直接拥有用例，未引入
status/history/session API。

### 12.3 Repository executability

现有 source-web POM、WebArchitectureTest、MapStruct/constructor模式、definition metadata/basic verifier、source-projects
reactor和三份 release/policy scripts均已复核。每 Step有真实路径、依赖顺序、RED/GREEN、cwd/command、failure return、rollback和
path-limited commit；`.generated`明确仅为 derived验证输出。

### 12.4 Test and release completeness

测试从模块图、Domain/Application、AgentFlow/MCP、fixed context、HTTP/SSE、OpenAPI/source verify、generated IT到 seven release
shape逐级升级。真实 provider/MCP、容器断连、生产性能和 Central publish均未伪装为已验证；数据库迁移明确 N/A且旧
Flyway只做不可变检查。

### 12.5 Blocking Manual Check

| Check ID         | Applicability | Status | Evidence                               | Finding                                               | Required action/exception |
|------------------|---------------|--------|----------------------------------------|-------------------------------------------------------|---------------------------|
| `MC-ARCH-001`    | `Applicable`  | `PASS` | §4.7、§5、Steps 1/6/7                    | exact Web non-open六模块，业务仅 research                    | None                      |
| `MC-REUSE-001`   | `Applicable`  | `PASS` | capability ledger、Steps 3/7/8          | 复用 Agent Flow/MVC/MapStruct/generator/release wrapper | None                      |
| `MC-DEP-001`     | `Applicable`  | `PASS` | Step 1 POM、Steps 6/8 dependency gates  | AI/MCP只在 owner层，无 DB/GraphQL/RPC                      | None                      |
| `MC-NAME-001`    | `Applicable`  | `PASS` | §5 inventory、SourceContract/verifier   | Request/Command/BO/Event/VO/Properties等语义后缀完整         | None                      |
| `MC-VALID-001`   | `Applicable`  | `PASS` | Steps 2/5、TEST-002/015                 | HTTP与 Application/Domain边界均验证                         | None                      |
| `MC-MODEL-001`   | `Applicable`  | `PASS` | Domain/API records、run lifecycle types | immutable records；行为对象完整构造                            | None                      |
| `MC-CONVERT-001` | `Applicable`  | `PASS` | Steps 3/5、四个 exact BaseConverter       | static MapStruct both directions，无绕过                  | None                      |
| `MC-LOG-001`     | `Applicable`  | `PASS` | REQ-012、yuheng/manage/filter tests    | 仅 runId/trace/stage/outcome/duration/code             | None                      |
| `MC-BEAN-001`    | `Applicable`  | `PASS` | Steps 2-5、lombok.config/context tests  | 具体 Bean显式名/Qualifier/constructor，converter非 Bean      | None                      |
| `MC-UTIL-001`    | `Applicable`  | `PASS` | capability ledger/POM/source denylist  | JDK/Spring/Egon/approved AI足够，无 Utils/Fastjson        | None                      |
| `MC-JSON-001`    | `Applicable`  | `PASS` | Step 5 converter/MockMvc、Step 6 OAS    | Jackson-only，absent/null/enum/NON_NULL/SSE明确          | None                      |
| `MC-TIME-001`    | `Applicable`  | `PASS` | model/properties/converter plans       | Instant/Duration/injected Clock，无 legacy time         | None                      |
| `MC-CONFIG-001`  | `Applicable`  | `PASS` | Step 4 profiles、TEST-016               | dev/test/prod key parity，test fake，secret external    | None                      |
| `MC-PATTERN-001` | `Applicable`  | `PASS` | §4.5、Steps 2/3/5                       | Adapter/Observer/Facade/Bulkhead各解决真实复杂点              | None                      |
| `MC-SCOPE-001`   | `Applicable`  | `PASS` | §5、commit paths、Steps 7/8保护 Gate       | 只新增 Agent family并改三 scripts，旧六/Flyway/dirty work不动    | None                      |
| `MC-TEST-001`    | `Applicable`  | `PASS` | §8 Gates 0-11、TEST-001至024             | 每项 requirement/standard有 future executable proof      | None                      |
| `MC-BLOCKER-001` | `Applicable`  | `PASS` | §11及前置 Gate                            | 用户决策已关闭；前置依赖由顺序 Gate关闭                                | None                      |

### 12.6 Final verdict

PASS — Ready for user review
